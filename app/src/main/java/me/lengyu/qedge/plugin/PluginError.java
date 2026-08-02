package me.lengyu.qedge.plugin;

import me.lengyu.qedge.plugin.bean.PluginInfo;
import me.lengyu.qedge.utils.LogUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;

public class PluginError {

    public static void evalError(Exception e, PluginInfo info) {
        String error = formatError("插件加载异常", e);
        // LogUtils.e("PluginError", error);
        saveErrorLog(info, error);
    }

    public static void callError(Exception e, PluginInfo info) {
        String error = formatError("插件运行异常", e);
        // LogUtils.e("PluginError", error);
        saveErrorLog(info, error);
    }

    public static void findError(NoSuchMethodException e, PluginInfo info, String methodName) {
        String error = "未找到方法: " + methodName + "\n" +
                "请检查方法签名是否正确（参数个数和类型）";
        // LogUtils.e("PluginError", error);
        saveErrorLog(info, error);
    }

    private static String formatError(String title, Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        // e.printStackTrace(pw);
        return title + ": " + e.getMessage() + "\n" + sw.toString();
    }

    private static void saveErrorLog(PluginInfo info, String error) {
        try {
            File logFile = new File(info.getDirPath(), "error.log");
            try (FileWriter writer = new FileWriter(logFile, true)) {
                writer.write("========================================\n");
                writer.write("Time: " + java.text.SimpleDateFormat.getInstance().format(new java.util.Date()) + "\n");
                writer.write(error);
                writer.write("\n");
            }
        } catch (Exception e) {
            LogUtils.e("PluginError", "Failed to save error log: " + e.getMessage());
        }
    }
}