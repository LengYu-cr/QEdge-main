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
        List<String> savedList = ObjectStore.loadList("data", "AutoLoadList.dat");
        if (savedList == null) {
            // 兼容旧文件：.json 已有数据则迁移到 .dat
            List<String> legacy = ObjectStore.loadList("data", "AutoLoadList.json");
            if (legacy != null) {
                ObjectStore.saveList("data", "AutoLoadList.dat", legacy);
                savedList = legacy;
            }
        }
        if (savedList != null) {
            autoLoadList.clear();
            autoLoadList.addAll(savedList);
        }
    }

    private static void saveAutoLoadConfig() {
        ObjectStore.saveList("data", "AutoLoadList.dat", autoLoadList);
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
            Toasts.showCustomToast("QEdge：Java插件[" + plugin.getName() + "]加载异常：请检查日志");
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
        return createExamplePlugin(PluginInfo.TYPE_JAVA);
    }

    /**
     * 按弹窗填写的信息新建插件。
     * type 为 "java"(BeanShell) 或 "js"(Rhino)，主脚本文件建空文件由用户自行编写。
     * 目录名直接用脚本名(去非法字符)，唯一性由 id 保证：已存在同名目录或同 id 插件则拒绝创建。
     */
    public static boolean createPlugin(String type, String name, String desc, String author, String version) {
        boolean isJs = "js".equalsIgnoreCase(type) || "javascript".equalsIgnoreCase(type);
        long timestamp = System.currentTimeMillis();

        String safeName = (name == null || name.trim().isEmpty())
                ? (isJs ? "JS脚本" : "脚本") : name.trim();
        String safeAuthor = (author == null || author.trim().isEmpty()) ? "Developer" : author.trim();
        String safeVersion = (version == null || version.trim().isEmpty()) ? "1.0" : version.trim();
        String safeDesc = (desc == null) ? "" : desc.trim();

        // id 作为唯一标识；目录名直接用脚本名，不加时间戳
        String id = (isJs ? "js_" : "plugin_") + timestamp;
        if (isIdExists(id)) {
            LogUtils.e("PluginManager", "Plugin id already exists: " + id);
            Toasts.showCustomToast("已存在相同 id 的脚本");
            return false;
        }

        String dirName = sanitizeFileName(safeName);
        File targetDir = new File(getPluginDir(), dirName);
        if (targetDir.exists()) {
            LogUtils.e("PluginManager", "Plugin dir already exists: " + targetDir.getAbsolutePath());
            Toasts.showCustomToast("已存在同名脚本，请换个名字");
            return false;
        }

        if (!targetDir.mkdirs()) {
            LogUtils.e("PluginManager", "Failed to create directory: " + targetDir.getAbsolutePath());
            return false;
        }

        try {
            File propFile = new File(targetDir, "info.prop");
            String propContent = "id=" + id + "\n" +
                    "pluginName=" + safeName + "\n" +
                    "versionCode=" + safeVersion + "\n" +
                    "author=" + safeAuthor + "\n" +
                    "type=" + (isJs ? PluginInfo.TYPE_JS : PluginInfo.TYPE_JAVA) + "\n";
            writeFile(propFile, propContent);

            writeFile(new File(targetDir, "desc.txt"), safeDesc);

            // 主脚本建空文件，交由用户自行编写
            writeFile(new File(targetDir, isJs ? "main.js" : "main.java"), "");

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

    /** 判断当前插件列表里是否已存在同 id 插件 */
    private static boolean isIdExists(String id) {
        for (PluginInfo p : plugins) {
            if (id.equals(p.getId())) return true;
        }
        return false;
    }

    /** 过滤文件名里的非法字符，避免建目录失败 */
    private static String sanitizeFileName(String name) {
        String cleaned = name.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
        return cleaned.isEmpty() ? "plugin" : cleaned;
    }

    /** 新建示例插件。type 为 "java"(BeanShell) 或 "js"(Rhino) */
    public static boolean createExamplePlugin(String type) {
        boolean isJs = "js".equalsIgnoreCase(type) || "javascript".equalsIgnoreCase(type);
        long timestamp = System.currentTimeMillis();
        String dirName = isJs ? "JS示例脚本_" + timestamp : "示例脚本_" + timestamp;
        File targetDir = new File(getPluginDir(), dirName);

        if (!targetDir.mkdirs()) {
            LogUtils.e("PluginManager", "Failed to create directory: " + targetDir.getAbsolutePath());
            return false;
        }

        try {
            File propFile = new File(targetDir, "info.prop");
            String propContent = "id=" + (isJs ? "jsexample_" : "example_") + timestamp + "\n" +
                    "pluginName=" + (isJs ? "JS示例脚本" : "示例脚本") + "\n" +
                    "versionCode=1.0\n" +
                    "author=Developer\n" +
                    "type=" + (isJs ? PluginInfo.TYPE_JS : PluginInfo.TYPE_JAVA) + "\n";
            writeFile(propFile, propContent);

            File descFile = new File(targetDir, "desc.txt");
            writeFile(descFile, isJs ? "这是一个自动生成的 JS 示例脚本" : "这是一个自动生成的示例脚本");

            if (isJs) {
                File mainFile = new File(targetDir, "main.js");
                String jsContent =
                        "console.log(\"JS 脚本开始运行...\");\n" +
                        "qqToast(2, \"Hello JS!\");\n" +
                        "\n" +
                        "// 注册消息菜单，点击后回调 onTestClick\n" +
                        "addItem(\"测试菜单\", \"onTestClick\");\n" +
                        "\n" +
                        "// 收到消息回调(与 Java 插件一致)\n" +
                        "function onMsg(msg) {\n" +
                        "    console.log(\"收到消息: \" + msg);\n" +
                        "}\n" +
                        "\n" +
                        "function onTestClick(chatType, peerUin, peerName) {\n" +
                        "    qqToast(2, \"点击了菜单\");\n" +
                        "}\n" +
                        "\n" +
                        "function unLoadPlugin() {\n" +
                        "    qqToast(0, \"脚本停止运行\");\n" +
                        "    console.log(\"脚本停止运行\");\n" +
                        "}\n";
                writeFile(mainFile, jsContent);
            } else {
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
            }

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