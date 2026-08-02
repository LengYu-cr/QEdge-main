package me.lengyu.qedge.utils;

public class ModulePathHolder {
    private static String modulePath;

    public static void setModulePath(String path) {
        modulePath = path;
    }

    public static String getModulePath() {
        return modulePath;
    }
}
