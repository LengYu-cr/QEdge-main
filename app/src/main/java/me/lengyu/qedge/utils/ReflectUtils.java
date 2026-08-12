package me.lengyu.qedge.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import me.lengyu.qedge.utils.LogUtils;
import me.lengyu.qedge.utils.HybridClassLoader;

public class ReflectUtils {

    public static ClassLoader hostClassLoader;

    public static void initClassLoader(ClassLoader loader) {
        if (loader != null) {
            injectClassLoader(loader);
               hostClassLoader = loader;
               return;
        }
        
    }

    public static void injectClassLoader(ClassLoader hostClassLoader) {
        HybridClassLoader.setHostClassLoader(hostClassLoader);
        HybridClassLoader loader = HybridClassLoader.INSTANCE;
        ClassLoader self = ReflectUtils.class.getClassLoader();
        assert self != null;
        ClassLoader parent = self.getParent();
        HybridClassLoader.setLoaderParentClassLoader(parent);
        try {
            Field fParent = ClassLoader.class.getDeclaredField("parent");
            fParent.setAccessible(true);
            fParent.set(self, loader);
        } catch (Exception ignored) {
            LogUtils.e("injectClassLoader: failed", ignored.getMessage());
        }
    }

    public static Method findMethod(Class<?> clazz, String methodName) {
        try {
            for (Method method : clazz.getDeclaredMethods()) {
                if (method.getName().equals(methodName)) {
                    method.setAccessible(true);
                    return method;
                }
            }
            for (Method method : clazz.getMethods()) {
                if (method.getName().equals(methodName)) {
                    method.setAccessible(true);
                    return method;
                }
            }
        } catch (Throwable e) {
            LogUtils.e(e);
        }
        return null;
    }

    public static Method findMethod(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        try {
            Method method = clazz.getDeclaredMethod(methodName, parameterTypes);
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException e) {
            try {
                Method method = clazz.getMethod(methodName, parameterTypes);
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException ex) {
                LogUtils.e(ex);
            }
        }
        return null;
    }

    public static Method findMethod(Class<?> clazz, String methodName, int paramCount) {
        try {
            for (Method method : clazz.getDeclaredMethods()) {
                if (method.getName().equals(methodName) && method.getParameterCount() == paramCount) {
                    method.setAccessible(true);
                    return method;
                }
            }
            for (Method method : clazz.getMethods()) {
                if (method.getName().equals(methodName) && method.getParameterCount() == paramCount) {
                    method.setAccessible(true);
                    return method;
                }
            }
        } catch (Throwable e) {
            LogUtils.e(e);
        }
        return null;
    }

    public static Method findMethod(Class<?> clazz, Class<?> returnType, Class<?>... parameterTypes) {
        try {
            for (Method method : clazz.getDeclaredMethods()) {
                if (isTypeCompatible(method.getReturnType(), returnType)) {
                    Class<?>[] types = method.getParameterTypes();
                    if (types.length == parameterTypes.length) {
                        boolean match = true;
                        for (int i = 0; i < types.length; i++) {
                            if (!isTypeCompatible(types[i], parameterTypes[i])) {
                                match = false;
                                break;
                            }
                        }
                        if (match) {
                            method.setAccessible(true);
                            return method;
                        }
                    }
                }
            }
            for (Method method : clazz.getMethods()) {
                if (isTypeCompatible(method.getReturnType(), returnType)) {
                    Class<?>[] types = method.getParameterTypes();
                    if (types.length == parameterTypes.length) {
                        boolean match = true;
                        for (int i = 0; i < types.length; i++) {
                            if (!isTypeCompatible(types[i], parameterTypes[i])) {
                                match = false;
                                break;
                            }
                        }
                        if (match) {
                            method.setAccessible(true);
                            return method;
                        }
                    }
                }
            }
        } catch (Throwable e) {
            LogUtils.e(e);
        }
        return null;
    }

    public static Constructor<?> findConstructor(Class<?> clazz, Class<?>... parameterTypes) {
        try {
            for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
                Class<?>[] types = constructor.getParameterTypes();
                if (types.length == parameterTypes.length) {
                    boolean match = true;
                    for (int i = 0; i < types.length; i++) {
                        if (!isTypeCompatible(types[i], parameterTypes[i])) {
                            match = false;
                            break;
                        }
                    }
                    if (match) {
                        constructor.setAccessible(true);
                        return constructor;
                    }
                }
            }
            for (Constructor<?> constructor : clazz.getConstructors()) {
                Class<?>[] types = constructor.getParameterTypes();
                if (types.length == parameterTypes.length) {
                    boolean match = true;
                    for (int i = 0; i < types.length; i++) {
                        if (!isTypeCompatible(types[i], parameterTypes[i])) {
                            match = false;
                            break;
                        }
                    }
                    if (match) {
                        constructor.setAccessible(true);
                        return constructor;
                    }
                }
            }
        } catch (Throwable e) {
            LogUtils.e(e);
        }
        return null;
    }

    private static boolean isTypeCompatible(Class<?> actualType, Class<?> searchType) {
        if (actualType == searchType) return true;
        if (searchType == null) return !actualType.isPrimitive();
        if (searchType.isAssignableFrom(actualType)) return true;
        if (searchType.isPrimitive()) {
            if (searchType == Boolean.TYPE && actualType == Boolean.class) return true;
            if (searchType == Integer.TYPE && actualType == Integer.class) return true;
            if (searchType == Long.TYPE && actualType == Long.class) return true;
            if (searchType == Byte.TYPE && actualType == Byte.class) return true;
            if (searchType == Short.TYPE && actualType == Short.class) return true;
            if (searchType == Character.TYPE && actualType == Character.class) return true;
            if (searchType == Float.TYPE && actualType == Float.class) return true;
            if (searchType == Double.TYPE && actualType == Double.class) return true;
        }
        return false;
    }

    public static Method findMethodOrNull(Class<?> clazz, Class<?> returnType, Class<?>... parameterTypes) {
        return findMethod(clazz, returnType, parameterTypes);
    }

    public static Field findField(Class<?> clazz, String fieldName) {
        try {
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException e) {
            try {
                Field field = clazz.getField(fieldName);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ex) {
                LogUtils.e(ex);
            }
        }
        return null;
    }

    public static Object getFieldValue(Object obj, String fieldName) {
        try {
            Field field = findField(obj.getClass(), fieldName);
            if (field != null) {
                return field.get(obj);
            }
        } catch (Throwable e) {
            LogUtils.e(e);
        }
        return null;
    }

    public static void setFieldValue(Object obj, String fieldName, Object value) {
        try {
            Field field = findField(obj.getClass(), fieldName);
            if (field != null) {
                field.set(obj, value);
            }
        } catch (Throwable e) {
            LogUtils.e(e);
        }
    }

    public static Object callMethod(Object obj, String methodName, Object... args) {
        try {
            Class<?>[] paramTypes = new Class[args.length];
            for (int i = 0; i < args.length; i++) {
                paramTypes[i] = args[i].getClass();
            }
            Method method = findMethod(obj.getClass(), methodName, paramTypes);
            if (method != null) {
                return method.invoke(obj, args);
            }
        } catch (Throwable e) {
            LogUtils.e(e);
        }
        return null;
    }

    public static Object callMethod(Object obj, Class<?> clazz, String methodName, Class<?>[] paramTypes) {
        try {
            Method method = findMethod(clazz, methodName, paramTypes);
            if (method != null) {
                return method.invoke(obj, (Object[]) null);
            }
        } catch (Throwable e) {
            LogUtils.e(e);
        }
        return null;
    }

    public static Object callOriginal(Object obj, String methodName, Object... args) {
        return callMethod(obj, methodName, args);
    }

    public static Object callStaticMethod(Class<?> clazz, String methodName, Object... args) {
        try {
            Class<?>[] paramTypes = new Class[args.length];
            for (int i = 0; i < args.length; i++) {
                if (args[i] != null) {
                    paramTypes[i] = args[i].getClass();
                } else {
                    paramTypes[i] = Object.class;
                }
            }
            Method method = findMethod(clazz, methodName, paramTypes);
            if (method != null) {
                return method.invoke(null, args);
            }
            // 尝试不指定参数类型
            method = findMethod(clazz, methodName, args.length);
            if (method != null) {
                return method.invoke(null, args);
            }
        } catch (Throwable e) {
            LogUtils.e(e);
        }
        return null;
    }

    public static Object newInstance(Class<?> clazz, Object... args) throws Exception {
        if (args == null || args.length == 0) {
            return clazz.newInstance();
        }
        Class<?>[] paramTypes = new Class[args.length];
        for (int i = 0; i < args.length; i++) {
            paramTypes[i] = args[i].getClass();
        }
        java.lang.reflect.Constructor<?> constructor = clazz.getDeclaredConstructor(paramTypes);
        constructor.setAccessible(true);
        return constructor.newInstance(args);
    }
}