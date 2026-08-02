package me.lengyu.qedge.utils;

public class ClassUtils {

    public static ClassLoader hostClassLoader;

    static {
        try {
            Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
            Object currentActivityThread = activityThreadClass.getMethod("currentActivityThread").invoke(null);
            if (currentActivityThread != null) {
                java.lang.reflect.Field mPackagesField = activityThreadClass.getDeclaredField("mPackages");
                mPackagesField.setAccessible(true);
                Object mPackages = mPackagesField.get(currentActivityThread);
                if (mPackages instanceof android.util.ArrayMap) {
                    android.util.ArrayMap<?, ?> packages = (android.util.ArrayMap<?, ?>) mPackages;
                    if (!packages.isEmpty()) {
                        Object packageInfo = packages.valueAt(0);
                        Class<?> loadedApkClass = Class.forName("android.app.LoadedApk");
                        java.lang.reflect.Field mClassLoaderField = loadedApkClass.getDeclaredField("mClassLoader");
                        mClassLoaderField.setAccessible(true);
                        hostClassLoader = (ClassLoader) mClassLoaderField.get(packageInfo);
                    }
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        if (hostClassLoader == null) {
            hostClassLoader = ClassUtils.class.getClassLoader();
        }
    }

    public static ClassLoader getHostClassLoader() {
        return hostClassLoader;
    }
}