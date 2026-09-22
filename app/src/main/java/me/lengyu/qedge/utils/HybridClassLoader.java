package me.lengyu.qedge.utils;

import android.content.Context;

/**
 * @Author 冷雨
 * @Description 混合类加载器
 */
public class HybridClassLoader extends ClassLoader {

    public static final HybridClassLoader INSTANCE = new HybridClassLoader();
    private static final ClassLoader sBootClassLoader = Context.class.getClassLoader();
    private static ClassLoader sLoaderParentClassLoader;
    private static ClassLoader sHostClassLoader;

    private HybridClassLoader() {
        super(sBootClassLoader);
    }

    public static void setLoaderParentClassLoader(ClassLoader loaderClassLoader) {
        if (loaderClassLoader == HybridClassLoader.class.getClassLoader()) {
            sLoaderParentClassLoader = null;
        } else {
            sLoaderParentClassLoader = loaderClassLoader;
        }
    }

    public static void setHostClassLoader(ClassLoader hostClassLoader) {
        sHostClassLoader = hostClassLoader;
    }

    public static boolean isHostClass(String name) {
        return name.startsWith("com.tencent.")
                || name.startsWith("com.qq.")
                || name.startsWith("mqq.")
                || name.startsWith("NS_")
                || name.startsWith("oicq.")
                || name.startsWith("QQService.")
                || name.startsWith("tencent.")
                || name.startsWith("cooperation.")
                || name.startsWith("dov.")
                || name.startsWith("com.tenpay");
    }

    public static boolean isConflictingClass(String name) {
        return name.startsWith("androidx.") || name.startsWith("android.support.")
                || name.startsWith("kotlin.") || name.startsWith("kotlinx.")
                || name.startsWith("com.tencent.mmkv.")
                || name.startsWith("com.android.tools.r8.")
                || name.startsWith("com.google.android.")
                || name.startsWith("com.google.gson.")
                || name.startsWith("com.google.common.")
                || name.startsWith("com.google.protobuf.")
                || name.startsWith("com.microsoft.appcenter.")
                || name.startsWith("org.intellij.lang.annotations.")
                || name.startsWith("org.jetbrains.annotations.")
                || name.startsWith("com.bumptech.glide.")
                || name.startsWith("com.google.errorprone.annotations.")
                || name.startsWith("org.jf.dexlib2.")
                || name.startsWith("org.jf.util.")
                || name.startsWith("javax.annotation.")
                || name.startsWith("_COROUTINE.");
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        try {
            return sBootClassLoader.loadClass(name);
        } catch (ClassNotFoundException ignored) {
        }

        if (sLoaderParentClassLoader != null && name.startsWith("me.lengyu.qedge.loader.")) {
            return sLoaderParentClassLoader.loadClass(name);
        }

        if (isConflictingClass(name)) {
            throw new ClassNotFoundException(name);
        }

        if (sLoaderParentClassLoader != null) {
            try {
                return sLoaderParentClassLoader.loadClass(name);
            } catch (ClassNotFoundException ignored) {
            }
        }

        if (sHostClassLoader != null && isHostClass(name)) {
            try {
                return sHostClassLoader.loadClass(name);
            } catch (ClassNotFoundException ignored) {
            }
        }

        throw new ClassNotFoundException(name);
    }
}
