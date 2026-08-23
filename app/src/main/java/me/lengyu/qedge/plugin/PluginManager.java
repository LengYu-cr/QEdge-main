package me.lengyu.qedge.plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import me.lengyu.qedge.plugin.bean.PluginInfo;
import me.lengyu.qedge.utils.LogUtils;
import me.lengyu.qedge.utils.qq.QQCurrentEnv;
import me.lengyu.qedge.utils.ObjectStore;
import me.lengyu.qedge.utils.Toasts;



/**
 * @Author 冷雨
 * @Description 插件管理器
 */
public class PluginManager {
    public static final List<PluginInfo> plugins = new ArrayList<>();
    public static final List<String> autoLoadList = new ArrayList<>();
    
    // 保存正在运行的插件的Compiler实例，key为pluginId
    public static final Map<String, PluginCompiler> runningCompilers = new HashMap<>();

    private static File pluginDir;

    private static File getPluginDir() {
        pluginDir = new File(QQCurrentEnv.getCurrentDir(), "plugin");
        if (!pluginDir.exists()) {
            boolean created = pluginDir.mkdirs();
        }
        return pluginDir;
    }

    public static void loadAll() {
        plugins.clear();

        File[] dirs = getPluginDir().listFiles(File::isDirectory);
        if (dirs != null) {
            for (File dir : dirs) {
                PluginInfo info = PluginInfo.fromDir(dir);
                if (info != null) {
                    // 如果这个插件正在运行，恢复其状态和Compiler
                    PluginCompiler existingCompiler = runningCompilers.get(info.getId());
                    if (existingCompiler != null) {
                        info.setCompiler(existingCompiler);
                        info.setRunning(true);
                    }
                    plugins.add(info);
                }
            }
        }

        loadAutoLoadConfig();
    }

    private static void loadAutoLoadConfig() {
        List<String> savedList = ObjectStore.loadList("data", "AutoLoadList.json");
        if (savedList != null) {
            autoLoadList.clear();
            autoLoadList.addAll(savedList);
        }
    }

    private static void saveAutoLoadConfig() {
        ObjectStore.saveList("data", "AutoLoadList.json", autoLoadList);
    }

    public static boolean startPlugin(PluginInfo plugin) {
        try {
            plugin.getCompiler().start();
            // 保存运行中的Compiler
            runningCompilers.put(plugin.getId(), plugin.getCompiler());
            return true;
        } catch (Exception e) {
            // LogUtils.e("[DEBUG-PLUGIN]", "DP-003 ERROR: " + e.getMessage());
            // LogUtils.e("[DEBUG-PLUGIN]", e);
            Toasts.showCustomToast("QEdge：Java插件" + plugin.getName() + "加载异常：请检查日志");
            e.printStackTrace();
            PluginError.evalError(e, plugin);
            return false;
        }
    }

    public static void stopPlugin(PluginInfo plugin) {
        try {
            plugin.getCompiler().stop(true);
            // 移除运行状态
            runningCompilers.remove(plugin.getId());
        } catch (Exception e) {
            PluginError.evalError(e, plugin);
        }
    }

    public static boolean reloadPlugin(PluginInfo plugin) {
        stopPlugin(plugin);
        return startPlugin(plugin);
    }

    public static void deletePlugin(PluginInfo plugin) {
        stopPlugin(plugin);
        plugins.remove(plugin);
        autoLoadList.remove(plugin.getId());
        deleteFile(new File(plugin.getDirPath()));
        saveAutoLoadConfig();
    }

    private static void deleteFile(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    deleteFile(f);
                }
            }
        }
        file.delete();
    }

    public static void setAutoLoad(PluginInfo plugin, boolean isAuto) {
        if (isAuto) {
            if (!autoLoadList.contains(plugin.getId())) {
                autoLoadList.add(plugin.getId());
            }
        } else {
            autoLoadList.remove(plugin.getId());
        }
        saveAutoLoadConfig();
    }

    public static void startAutoLoadPlugins() {
        new Thread(() -> {
            for (PluginInfo plugin : plugins) {
                if (autoLoadList.contains(plugin.getId()) && !plugin.isRunning()) {
                    startPlugin(plugin);
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ignored) {}
                }
            }
        }, "Plugin-AutoLoad").start();
    }

    public static void stopAllPlugins() {
        for (PluginInfo plugin : plugins) {
            if (plugin.isRunning()) {
                stopPlugin(plugin);
            }
        }
    }

    public static void initAllPluginForCurrent() {
        stopAllPlugins();
        plugins.clear();
        loadAll();
        startAutoLoadPlugins();
    }

    public static boolean createExamplePlugin() {
        long timestamp = System.currentTimeMillis();
        String dirName = "示例脚本";
        File targetDir = new File(getPluginDir(), dirName);

        if (!targetDir.mkdirs()) {
            LogUtils.e("PluginManager", "Failed to create directory: " + targetDir.getAbsolutePath());
            return false;
        }

        try {
            File propFile = new File(targetDir, "info.prop");
            String propContent = "id=example_" + timestamp + "\n" +
                    "pluginName=示例脚本\n" +
                    "versionCode=1.0\n" +
                    "author=Developer\n";
            writeFile(propFile, propContent);

            File descFile = new File(targetDir, "desc.txt");
            writeFile(descFile, "这是一个自动生成的示例脚本");

            File mainFile = new File(targetDir, "main.java");
            String javaContent =
                    "log(\"脚本开始运行...\");\n" +
                    "qqToast(2, \"Hello World!\");\n" +
                    "\n" +
                    "addItem(\"测试菜单\", \"onTestClick\");\n" +
                    "\n" +
                    "void onTestClick(int chatType, String peerUin, String peerName) {\n" +
                    "    qqToast(2, \"点击了菜单\");\n" +
                    "}\n" +
                    "\n" +
                    "void unLoadPlugin() {\n" +
                    "    qqToast(0, \"脚本停止运行\");\n" +
                    "    log(\"脚本停止运行\");\n" +
                    "}\n";
            writeFile(mainFile, javaContent);

            PluginInfo info = PluginInfo.fromDir(targetDir);
            if (info != null) {
                plugins.add(info);
                return true;
            }
            LogUtils.e("PluginManager", "Failed to load plugin info from dir");
            return true;
        } catch (Exception e) {
            LogUtils.e("PluginManager", e);
            deleteFile(targetDir);
            return false;
        }
    }

    private static void writeFile(File file, String content) throws Exception {
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        java.io.BufferedWriter writer = new java.io.BufferedWriter(
                new java.io.OutputStreamWriter(new java.io.FileOutputStream(file), "UTF-8"));
        writer.write(content);
        writer.flush();
        writer.close();
    }

    public static void uploadPlugin() {
        try {
            android.app.Activity activity = QQCurrentEnv.getActivity();
            if (activity == null) {
                LogUtils.e("PluginManager", "Activity is null, cannot upload plugin");
                return;
            }

            android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_GET_CONTENT);
            intent.setType("application/octet-stream");
            intent.addCategory(android.content.Intent.CATEGORY_OPENABLE);

            activity.startActivityForResult(intent, 1001);
        } catch (Exception e) {
            LogUtils.e("PluginManager", e);
        }
    }
}