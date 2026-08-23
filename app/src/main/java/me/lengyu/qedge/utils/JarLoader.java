package me.lengyu.qedge.utils;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import dalvik.system.DexClassLoader;
import dalvik.system.PathClassLoader;
import me.lengyu.qedge.utils.LogUtils;

/**
 * @Author 冷雨
 * @Description Jar加载工具类
 */
public class JarLoader {

    public static ClassLoader loadJar(String path) throws IOException {
        File jarFile = new File(path);
        if (!jarFile.exists()) {
            throw new IOException("Jar文件不存在: " + path);
        }

        File optimizedDirectory = new File(HostInfo.getContext().getCacheDir(), "dex_opt");
        if (!optimizedDirectory.exists()) {
            optimizedDirectory.mkdirs();
        }

        DexClassLoader dexClassLoader = new DexClassLoader(
                path,
                optimizedDirectory.getAbsolutePath(),
                null,
                PathClassLoader.getSystemClassLoader()
        );

        return dexClassLoader;
    }

    public static ClassLoader loadJarFromAssets(Context context, String assetPath) throws IOException {
        File jarFile = new File(context.getCacheDir(), "plugin_" + assetPath.hashCode() + ".jar");
        if (!jarFile.exists()) {
            try (InputStream is = context.getAssets().open(assetPath);
                 FileOutputStream fos = new FileOutputStream(jarFile)) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, len);
                }
            }
        }
        return loadJar(jarFile.getAbsolutePath());
    }

    public static Class<?> loadClassFromJar(String jarPath, String className) throws IOException, ClassNotFoundException {
        ClassLoader classLoader = loadJar(jarPath);
        return classLoader.loadClass(className);
    }

    public static void loadJarToSystem(String jarPath) throws IOException {
        ClassLoader classLoader = loadJar(jarPath);
        PathClassLoader systemClassLoader = (PathClassLoader) PathClassLoader.getSystemClassLoader();
        try {
            Method addDexPathMethod = PathClassLoader.class.getDeclaredMethod("addDexPath", String.class);
            addDexPathMethod.setAccessible(true);
            addDexPathMethod.invoke(systemClassLoader, jarPath);
        } catch (Exception e) {
            LogUtils.e(e);
        }
    }

    public static boolean isValidJar(String path) {
        try {
            JarFile jarFile = new JarFile(path);
            Enumeration<JarEntry> entries = jarFile.entries();
            boolean hasClasses = false;
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.getName().endsWith(".class")) {
                    hasClasses = true;
                    break;
                }
            }
            jarFile.close();
            return hasClasses;
        } catch (IOException e) {
            return false;
        }
    }
}