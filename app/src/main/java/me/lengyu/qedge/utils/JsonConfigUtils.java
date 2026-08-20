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

import me.lengyu.qedge.utils.LogUtils;

public class JsonConfigUtils {

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
        JSONObject json = loadConfig(absoluteDir, configName);
        return json.optString(key, defaultValue);
    }

    public static int getInt(String absoluteDir, String configName, String key, int defaultValue) {
        JSONObject json = loadConfig(absoluteDir, configName);
        return json.optInt(key, defaultValue);
    }

    public static boolean getBoolean(String absoluteDir, String configName, String key, boolean defaultValue) {
        JSONObject json = loadConfig(absoluteDir, configName);
        return json.optBoolean(key, defaultValue);
    }

    public static long getLong(String absoluteDir, String configName, String key, long defaultValue) {
        JSONObject json = loadConfig(absoluteDir, configName);
        return json.optLong(key, defaultValue);
    }

    public static double getDouble(String absoluteDir, String configName, String key, double defaultValue) {
        JSONObject json = loadConfig(absoluteDir, configName);
        return json.optDouble(key, defaultValue);
    }

    public static void remove(String absoluteDir, String configName, String key) {
        JSONObject json = loadConfig(absoluteDir, configName);
        json.remove(key);
        saveConfig(absoluteDir, configName, json);
    }

    public static boolean contains(String absoluteDir, String configName, String key) {
        JSONObject json = loadConfig(absoluteDir, configName);
        return json.has(key);
    }

    public static void clear(String absoluteDir, String configName) {
        saveConfig(absoluteDir, configName, new JSONObject());
    }

    public static java.util.Map<String, Object> getConfigMap(String absoluteDir, String configName) {
        JSONObject json = loadConfig(absoluteDir, configName);
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        try {
            java.util.Iterator<String> keys = json.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                map.put(key, json.get(key));
            }
        } catch (JSONException e) {
            LogUtils.e(e);
        }
        return map;
    }

    public static String getConfigPath(String absoluteDir, String configName) {
        return getConfigFile(absoluteDir, configName).getAbsolutePath();
    }

    private static void putValue(String absoluteDir, String configName, String key, Object value) {
        JSONObject json = loadConfig(absoluteDir, configName);
        try {
            json.put(key, value);
            saveConfig(absoluteDir, configName, json);
        } catch (JSONException e) {
            LogUtils.e(e);
        }
    }

    private static JSONObject loadConfig(String absoluteDir, String configName) {
        File configFile = getConfigFile(absoluteDir, configName);
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

    /** 原子写入：先写临时文件，再 rename，避免进程被杀导致文件变空 */
    private static void saveConfig(String absoluteDir, String configName, JSONObject json) {
        File configFile = getConfigFile(absoluteDir, configName);
        saveConfigInternal(configFile, json);
        // 写入成功后更新备份
        File backupFile = new File(configFile.getParentFile(), configName + ".json.bak");
        try {
            String content = json.toString(4);
            try (FileOutputStream fos = new FileOutputStream(backupFile);
                 OutputStreamWriter writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
                writer.write(content);
            }
        } catch (IOException | JSONException e) {
            LogUtils.e(e);
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
                fos.getFD().sync();  // 确保数据落盘
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

    private static File getConfigFile(String absoluteDir, String configName) {
        File dir = new File(absoluteDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return new File(dir, configName + ".json");
    }
}