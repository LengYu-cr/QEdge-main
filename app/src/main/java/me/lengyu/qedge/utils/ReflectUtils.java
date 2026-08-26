package me.lengyu.qedge.utils;

import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.util.concurrent.ConcurrentHashMap;
import me.lengyu.qedge.utils.LogUtils;
import me.lengyu.qedge.utils.HybridClassLoader;
/**
 * @Author 冷雨
 * @Description Java反射工具类
 */
public class ReflectUtils {

    private static final String TAG = "ReflectUtils";
    public static ClassLoader hostClassLoader;

    // ---- 反射结果缓存：避免消息/图片等热路径每次都遍历方法表、重复抛 NoSuchFieldException ----
    // key 用 "类名#成员名(#参数个数)"，value 命中缓存的 Method/Field；查不到用 NOT_FOUND 哨兵占位，避免反复失败查找。
    private static final ConcurrentHashMap<String, Method> METHOD_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Field> FIELD_CACHE = new ConcurrentHashMap<>();

    private static final Method NOT_FOUND_METHOD;
    private static final Field NOT_FOUND_FIELD;
    static {
        Method m = null;
        Field f = null;
        try {
            m = ReflectUtils.class.getDeclaredMethod("initClassLoader", ClassLoader.class);
            f = ReflectUtils.class.getDeclaredField("hostClassLoader");
        } catch (Throwable ignored) {
        }
        NOT_FOUND_METHOD = m;
        NOT_FOUND_FIELD = f;
    }

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
        if (clazz == null) return null;
        String key = clazz.getName() + "#" + methodName;
        Method cached = METHOD_CACHE.get(key);
        if (cached != null) {
            return cached == NOT_FOUND_METHOD ? null : cached;
        }
        Method found = null;
        try {
            for (Method method : clazz.getDeclaredMethods()) {
                if (method.getName().equals(methodName)) {
                    method.setAccessible(true);
                    found = method;
                    break;
                }
            }
            if (found == null) {
                for (Method method : clazz.getMethods()) {
                    if (method.getName().equals(methodName)) {
                        method.setAccessible(true);
                        found = method;
                        break;
                    }
                }
            }
        } catch (Throwable e) {
            LogUtils.e(e);
        }
        METHOD_CACHE.put(key, found != null ? found : NOT_FOUND_METHOD);
        return found;
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
        if (clazz == null) return null;
        String key = clazz.getName() + "#" + methodName + "#" + paramCount;
        Method cached = METHOD_CACHE.get(key);
        if (cached != null) {
            return cached == NOT_FOUND_METHOD ? null : cached;
        }
        Method found = null;
        try {
            for (Method method : clazz.getDeclaredMethods()) {
                if (method.getName().equals(methodName) && method.getParameterCount() == paramCount) {
                    method.setAccessible(true);
                    found = method;
                    break;
                }
            }
            if (found == null) {
                for (Method method : clazz.getMethods()) {
                    if (method.getName().equals(methodName) && method.getParameterCount() == paramCount) {
                        method.setAccessible(true);
                        found = method;
                        break;
                    }
                }
            }
        } catch (Throwable e) {
            LogUtils.e(e);
        }
        METHOD_CACHE.put(key, found != null ? found : NOT_FOUND_METHOD);
        return found;
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
            Log.e(TAG, "findMethod by return type error: " + e.getMessage());
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
        if (clazz == null) return null;
        String key = clazz.getName() + "#" + fieldName;
        Field cached = FIELD_CACHE.get(key);
        if (cached != null) {
            return cached == NOT_FOUND_FIELD ? null : cached;
        }
        Field found = null;
        try {
            found = clazz.getDeclaredField(fieldName);
            found.setAccessible(true);
        } catch (NoSuchFieldException e) {
            try {
                found = clazz.getField(fieldName);
                found.setAccessible(true);
            } catch (NoSuchFieldException ex) {
                LogUtils.e(ex);
            }
        }
        FIELD_CACHE.put(key, found != null ? found : NOT_FOUND_FIELD);
        return found;
    }

    public static Object getFieldValue(Object obj, String fieldName) {
        try {
            Field field = findField(obj.getClass(), fieldName);
            if (field != null) {
                return field.get(obj);
            }
        } catch (Throwable e) {
            Log.e(TAG, "getFieldValue error: " + e.getMessage());
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
            Log.e(TAG, "setFieldValue error: " + e.getMessage());
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
            Log.e(TAG, "callMethod error: " + e.getMessage());
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
            Log.e(TAG, "callMethod error: " + e.getMessage());
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
            Log.e(TAG, "callStaticMethod error: " + e.getMessage());
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