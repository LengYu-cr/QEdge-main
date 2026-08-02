package me.lengyu.qedge.utils;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindClass;
import org.luckypray.dexkit.result.ClassData;
import org.luckypray.dexkit.wrap.DexClass;

import java.lang.reflect.Constructor;

public class DexKitHelper {

    private static DexKitBridge dexKitBridge;

    public static void init(DexKitBridge bridge) {
        dexKitBridge = bridge;
    }

    public static Class<?> requireClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Cannot find class: " + className, e);
        }
    }

    public static Class<?> findClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    public static Class<?> requireClassByDexKit(String key) {
        try {
            String descriptor = DexKitCache.getDescriptor(key);
            if (descriptor != null) {
                return new DexClass(descriptor).getInstance(ReflectUtils.hostClassLoader);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        throw new RuntimeException("Cannot find class: " + key);
    }

    public static <T> T newInstance(Class<?> clazz, Object... args) throws Exception {
        if (args == null || args.length == 0) {
            return (T) clazz.newInstance();
        }
        Class<?>[] paramTypes = new Class[args.length];
        for (int i = 0; i < args.length; i++) {
            paramTypes[i] = args[i].getClass();
        }
        Constructor<?> constructor = clazz.getDeclaredConstructor(paramTypes);
        constructor.setAccessible(true);
        return (T) constructor.newInstance(args);
    }
}
