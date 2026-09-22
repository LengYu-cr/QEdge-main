package me.lengyu.qedge.utils;

/**
 * @Author 冷雨
 * @Description 模块路径工具类
 */
public class ModulePathHolder {
    private static String modulePath;

    public static void setModulePath(String path) {
        modulePath = path;
    }

    public static String getModulePath() {
        return modulePath;
    }
}
