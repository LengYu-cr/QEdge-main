package me.lengyu.qedge.utils;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * @Author 冷雨
 * @Description JSON 配置读写工具类
 *
 * <p>性能优化：每个配置文件在进程内维护一份内存缓存，读操作命中缓存后仅做一次
 * {@code lastModified()} 判断（O(1) stat），不再每次全量读盘 + 解析；写操作立即更新
 * 缓存并把落盘合并到后台单线程去抖执行，去掉了每次写调用里的 {@code fsync} 阻塞。
 *
 * <p>跨进程一致性：模块 UI 进程写、QQ/TIM Hook 进程读。读时通过文件 {@code lastModified()}
 * 变化检测其他进程的写入，变化则重新加载一次，保证 Hook 侧能读到 UI 侧的最新开关值。
 */
public class JsonConfigUtils {

    /** 每个配置文件对应一个缓存条目，key 为配置文件绝对路径。 */
    private static final ConcurrentHashMap<String, ConfigEntry> CACHE = new ConcurrentHashMap<>();

    /** 后台落盘线程：单线程，去抖合并写入。 */
    private static final ScheduledExecutorService FLUSH_EXECUTOR =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "QEdge-ConfigFlush");
                t.setDaemon(true);
                return t;
            });

    /** 写入去抖延迟：短时间内的多次写只触发一次落盘。 */
    private static final long FLUSH_DELAY_MS = 300;

    private static final class ConfigEntry {
        final File configFile;
        /** 内存中的最新配置，所有读写都以它为准。 */
        JSONObject data;
        /** 上次从磁盘加载/写入后记录的文件修改时间，用于检测其他进程的写入。 */
        long fileStamp;
        /** 待落盘标记，配合去抖任务合并写入。 */
        boolean dirty;
        ScheduledFuture<?> pendingFlush;

        ConfigEntry(File configFile) {
            this.configFile = configFile;
        }
    }

    public static void putString(String absoluteDir, String configName, String key, String value) {
        putValue(absoluteDir, configName, key, value);
    }

    public static void putInt(String absoluteDir, String configName, String key, int value) {
        putValue(absoluteDir, configName, key, value);
    }

    public static void putBoolean(String absoluteDir, String configName, String key, boolean value) {
        putValue(absoluteDir, configName, key, value);
    }

    public static void putLong(String absoluteDir, String configName, String key, long value) {
        putValue(absoluteDir, configName, key, value);
    }

    public static void putDouble(String absoluteDir, String configName, String key, double value) {
        putValue(absoluteDir, configName, key, value);
    }

    public static String getString(String absoluteDir, String configName, String key, String defaultValue) {
        ConfigEntry entry = obtainEntry(absoluteDir, configName);
        synchronized (entry) {
            return entry.data.optString(key, defaultValue);
        }
    }

    public static int getInt(String absoluteDir, String configName, String key, int defaultValue) {
        ConfigEntry entry = obtainEntry(absoluteDir, configName);
        synchronized (entry) {
            return entry.data.optInt(key, defaultValue);
        }
    }

    public static boolean getBoolean(String absoluteDir, String configName, String key, boolean defaultValue) {
        ConfigEntry entry = obtainEntry(absoluteDir, configName);
        synchronized (entry) {
            return entry.data.optBoolean(key, defaultValue);
        }
    }

    public static long getLong(String absoluteDir, String configName, String key, long defaultValue) {
        ConfigEntry entry = obtainEntry(absoluteDir, configName);
        synchronized (entry) {
            return entry.data.optLong(key, defaultValue);
        }
    }

    public static double getDouble(String absoluteDir, String configName, String key, double defaultValue) {
        ConfigEntry entry = obtainEntry(absoluteDir, configName);
        synchronized (entry) {
            return entry.data.optDouble(key, defaultValue);
        }
    }

    public static void remove(String absoluteDir, String configName, String key) {
        ConfigEntry entry = obtainEntry(absoluteDir, configName);
        synchronized (entry) {
            entry.data.remove(key);
            scheduleFlush(entry);
        }
    }

    public static boolean contains(String absoluteDir, String configName, String key) {
        ConfigEntry entry = obtainEntry(absoluteDir, configName);
        synchronized (entry) {
            return entry.data.has(key);
        }
    }

    public static void clear(String absoluteDir, String configName) {
        ConfigEntry entry = obtainEntry(absoluteDir, configName);
        synchronized (entry) {
            entry.data = new JSONObject();
            scheduleFlush(entry);
        }
    }

    public static java.util.Map<String, Object> getConfigMap(String absoluteDir, String configName) {
        ConfigEntry entry = obtainEntry(absoluteDir, configName);
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        synchronized (entry) {
            try {
                java.util.Iterator<String> keys = entry.data.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    map.put(key, entry.data.get(key));
                }
            } catch (JSONException e) {
                LogUtils.e(e);
            }
        }
        return map;
    }

    public static String getConfigPath(String absoluteDir, String configName) {
        return getConfigFile(absoluteDir, configName).getAbsolutePath();
    }

    private static void putValue(String absoluteDir, String configName, String key, Object value) {
        ConfigEntry entry = obtainEntry(absoluteDir, configName);
        synchronized (entry) {
            try {
                entry.data.put(key, value);
                scheduleFlush(entry);
            } catch (JSONException e) {
                LogUtils.e(e);
            }
        }
    }

    /**
     * 取得配置缓存条目：首次访问从磁盘加载一次；后续访问仅在文件被其他进程改动
     * （{@code lastModified()} 变化）时重新加载，命中缓存则直接返回内存对象。
     */
    private static ConfigEntry obtainEntry(String absoluteDir, String configName) {
        File configFile = getConfigFile(absoluteDir, configName);
        ConfigEntry entry = CACHE.computeIfAbsent(configFile.getAbsolutePath(),
                p -> new ConfigEntry(configFile));
        synchronized (entry) {
            long currentStamp = configFile.exists() ? configFile.lastModified() : 0L;
            // data 未初始化，或磁盘被其他进程改动过 -> 重新加载。
            // dirty 时说明本进程有尚未落盘的更改，以内存为准，不覆盖。
            if (entry.data == null || (!entry.dirty && currentStamp != entry.fileStamp)) {
                entry.data = loadConfig(entry.configFile, configName);
                entry.fileStamp = currentStamp;
            }
        }
        return entry;
    }

    /** 调度一次去抖落盘；调用方需持有 entry 锁。 */
    private static void scheduleFlush(ConfigEntry entry) {
        entry.dirty = true;
        if (entry.pendingFlush != null) {
            entry.pendingFlush.cancel(false);
        }
        entry.pendingFlush = FLUSH_EXECUTOR.schedule(
                () -> flush(entry), FLUSH_DELAY_MS, TimeUnit.MILLISECONDS);
    }

    /** 后台线程执行：把内存快照写入磁盘。 */
    private static void flush(ConfigEntry entry) {
        JSONObject snapshot;
        synchronized (entry) {
            if (!entry.dirty || entry.data == null) {
                return;
            }
            snapshot = new JSONObject();
            try {
                java.util.Iterator<String> keys = entry.data.keys();
                while (keys.hasNext()) {
                    String k = keys.next();
                    snapshot.put(k, entry.data.get(k));
                }
            } catch (JSONException e) {
                LogUtils.e(e);
                return;
            }
            entry.dirty = false;
        }

        saveConfigInternal(entry.configFile, snapshot);
        // 写备份
        File backupFile = new File(entry.configFile.getParentFile(),
                stripExtension(entry.configFile.getName()) + ".json.bak");
        try {
            String content = snapshot.toString(4);
            try (FileOutputStream fos = new FileOutputStream(backupFile);
                 OutputStreamWriter writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
                writer.write(content);
            }
        } catch (IOException | JSONException e) {
            LogUtils.e(e);
        }

        // 落盘后刷新时间戳，避免把自己的写入误判为"其他进程改动"而重复加载。
        synchronized (entry) {
            if (!entry.dirty) {
                entry.fileStamp = entry.configFile.exists() ? entry.configFile.lastModified() : 0L;
            }
        }
    }

    private static JSONObject loadConfig(File configFile, String configName) {
        if (!configFile.exists()) {
            return new JSONObject();
        }

        String content = readFileContent(configFile);
        if (content == null || content.trim().isEmpty()) {
            // 文件为空或损坏，尝试从备份恢复
            LogUtils.e("JsonConfigUtils", "config file empty/corrupt: " + configFile.getAbsolutePath());
            File backupFile = new File(configFile.getParentFile(), configName + ".json.bak");
            if (backupFile.exists()) {
                String backupContent = readFileContent(backupFile);
                if (backupContent != null && !backupContent.trim().isEmpty()) {
                    try {
                        JSONObject recovered = new JSONObject(new JSONTokener(backupContent));
                        // 恢复成功，把备份写回主文件
                        saveConfigInternal(configFile, recovered);
                        LogUtils.e("JsonConfigUtils", "recovered from backup: " + backupFile.getAbsolutePath());
                        return recovered;
                    } catch (JSONException e) {
                        LogUtils.e(e);
                    }
                }
            }
            return new JSONObject();
        }

        try {
            return new JSONObject(new JSONTokener(content));
        } catch (JSONException e) {
            LogUtils.e(e);
            // 解析失败，尝试从备份恢复
            File backupFile = new File(configFile.getParentFile(), configName + ".json.bak");
            if (backupFile.exists()) {
                String backupContent = readFileContent(backupFile);
                if (backupContent != null && !backupContent.trim().isEmpty()) {
                    try {
                        JSONObject recovered = new JSONObject(new JSONTokener(backupContent));
                        saveConfigInternal(configFile, recovered);
                        LogUtils.e("JsonConfigUtils", "recovered from backup after parse error");
                        return recovered;
                    } catch (JSONException ex) {
                        LogUtils.e(ex);
                    }
                }
            }
            return new JSONObject();
        }
    }

    /** 读取文件全部内容，失败返回 null */
    private static String readFileContent(File file) {
        StringBuilder contentBuilder = new StringBuilder();
        try (FileInputStream fis = new FileInputStream(file);
             InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
             BufferedReader reader = new BufferedReader(isr)) {
            String line;
            while ((line = reader.readLine()) != null) {
                contentBuilder.append(line);
            }
            return contentBuilder.toString();
        } catch (IOException e) {
            LogUtils.e(e);
            return null;
        }
    }

    /** 直接写入指定文件（原子操作） */
    private static void saveConfigInternal(File configFile, JSONObject json) {
        File tempFile = new File(configFile.getParentFile(), configFile.getName() + ".tmp");
        try {
            String content = json.toString(4);
            try (FileOutputStream fos = new FileOutputStream(tempFile);
                 OutputStreamWriter writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
                writer.write(content);
                writer.flush();
                fos.getFD().sync();  // 后台线程执行，确保数据落盘
            }
            // 原子替换：先删目标文件避免 rename 失败
            configFile.delete();
            if (!tempFile.renameTo(configFile)) {
                // rename 仍失败时回退到直接覆盖
                LogUtils.e("JsonConfigUtils", "rename failed, fallback to direct write");
                configFile.delete();
                try (FileOutputStream fos = new FileOutputStream(configFile);
                     OutputStreamWriter writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
                    writer.write(content);
                }
            }
        } catch (IOException | JSONException e) {
            LogUtils.e(e);
            tempFile.delete();
        }
    }

    private static String stripExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
    }

    private static File getConfigFile(String absoluteDir, String configName) {
        File dir = new File(absoluteDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return new File(dir, configName + ".json");
    }
}
