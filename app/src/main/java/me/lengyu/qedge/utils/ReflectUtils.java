package me.lengyu.qedge.utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import me.lengyu.qedge.utils.reflect.ClassUtils;
import me.lengyu.qedge.utils.reflect.ReflectExtensionsKt;

/**
 * @Author 冷雨
 * @Description Java反射工具类
 * 保留原有静态方法签名，内部统一委托到 utils/reflect（Kotlin）：缓存、父类链回退、参数兼容匹配都由那边提供。
 */
public class ReflectUtils {

    private static final String TAG = "ReflectUtils";

    // 宿主 ClassLoader 的唯一存储点：主线程初始化后由多个 Hook 线程读取，用 volatile 保证可见性
    public static volatile ClassLoader hostClassLoader;

    public static void initClassLoader(ClassLoader loader) {
        if (loader != null) {
            injectClassLoader(loader);
            hostClassLoader = loader;
        }
    }

    /**
     * 只切换宿主类查找入口，不改写模块自身 ClassLoader 的 parent。
     *
     * <p>QQ 9.3.70 起用 Tinker 热更补丁运行，真正在跑的宿主类挂在补丁 ClassLoader 上，
     * 必须用它去查找宿主类，否则会挂到基础包里的旧版同名类上（表现为 hook 安装成功但永不触发）。
     * 但补丁 loader 是 DelegateLastClassLoader，启动期正并发加载同一批类，
     * 一旦把它设成模块 loader 的 parent 就会互相等待死锁（QQ 卡启动页），
     * 因此补丁 loader 只能走这里，绝不能走 {@link #injectClassLoader}。
     */
    public static void setHostClassLoader(ClassLoader loader) {
        if (loader == null) {
            return;
        }
        HybridClassLoader.setHostClassLoader(loader);
        hostClassLoader = loader;
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
        } catch (Exception e) {
            LogUtils.e(TAG, "injectClassLoader error: " + e.getMessage());
        }
    }

    // ---- 类加载 ----

    /** 加载宿主类，失败抛 ClassNotFoundException */
    public static Class<?> findClass(String className) throws ClassNotFoundException {
        ClassLoader loader = hostClassLoader;
        if (loader == null) {
            throw new ClassNotFoundException("hostClassLoader not initialized: " + className);
        }
        return loader.loadClass(className);
    }

    /** 加载宿主类，失败返回 null */
    public static Class<?> findClassIfExists(String className) {
        return findClassIfExists(className, hostClassLoader);
    }

    /** 用指定 ClassLoader 加载，失败返回 null */
    public static Class<?> findClassIfExists(String className, ClassLoader classLoader) {
        if (className == null || classLoader == null) {
            return null;
        }
        try {
            return classLoader.loadClass(className);
        } catch (Throwable e) {
            return null;
        }
    }

    // ---- 方法查找 ----

    public static Method findMethod(Class<?> clazz, String methodName) {
        if (clazz == null || methodName == null) return null;
        return ClassUtils.findMethodOrNull(clazz, methodName);
    }

    public static Method findMethod(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        if (clazz == null || methodName == null) return null;
        return ClassUtils.findMethodOrNull(clazz, methodName, parameterTypes);
    }

    public static Method findMethod(Class<?> clazz, String methodName, int paramCount) {
        if (clazz == null || methodName == null) return null;
        return ClassUtils.findMethodOrNull(clazz, methodName, paramCount);
    }

    public static Method findMethod(Class<?> clazz, Class<?> returnType, Class<?>... parameterTypes) {
        if (clazz == null) return null;
        return ClassUtils.findMethodOrNull(clazz, returnType, parameterTypes);
    }

    public static Method findMethodOrNull(Class<?> clazz, Class<?> returnType, Class<?>... parameterTypes) {
        return findMethod(clazz, returnType, parameterTypes);
    }

    public static Constructor<?> findConstructor(Class<?> clazz, Class<?>... parameterTypes) {
        if (clazz == null) return null;
        // utils/reflect 未提供构造器检索的 DSL，这里保留原实现（含参数兼容匹配）
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
            LogUtils.e(TAG, "findConstructor error: " + e.getMessage());
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

    // ---- 字段查找 ----

    public static Field findField(Class<?> clazz, String fieldName) {
        if (clazz == null || fieldName == null) return null;
        return ClassUtils.findFieldOrNull(clazz, fieldName);
    }

    public static Object getFieldValue(Object obj, String fieldName) {
        if (obj == null || fieldName == null) return null;
        try {
            Field field = findField(obj.getClass(), fieldName);
            if (field != null) {
                return field.get(obj);
            }
        } catch (Throwable e) {
            LogUtils.e(TAG, "getFieldValue error: " + e.getMessage());
        }
        return null;
    }

    public static void setFieldValue(Object obj, String fieldName, Object value) {
        if (obj == null || fieldName == null) return;
        try {
            Field field = findField(obj.getClass(), fieldName);
            if (field != null) {
                field.set(obj, value);
            }
        } catch (Throwable e) {
            LogUtils.e(TAG, "setFieldValue error: " + e.getMessage());
        }
    }

    // ---- 方法调用 ----

    public static Object callMethod(Object obj, String methodName, Object... args) {
        if (obj == null || methodName == null) return null;
        Method method = ClassUtils.findMethodOrNull(obj.getClass(), methodName, toArgTypes(args));
        if (method == null) {
            return null;
        }
        try {
            return invoke(method, obj, args);
        } catch (Throwable e) {
            LogUtils.e(TAG, "callMethod " + methodName + " error: " + e.getMessage());
            return null;
        }
    }

    /** 历史签名：不带实参，仅能调用无参方法；新代码请用 callMethod(Object, String, Object...) */
    public static Object callMethod(Object obj, Class<?> clazz, String methodName, Class<?>[] paramTypes) {
        try {
            Method method = findMethod(clazz, methodName, paramTypes);
            if (method != null) {
                return method.invoke(obj, (Object[]) null);
            }
        } catch (Throwable e) {
            LogUtils.e(TAG, "callMethod error: " + e.getMessage());
        }
        return null;
    }

    /** 调用被 Hook 前的原始方法 */
    public static Object callOriginal(Object obj, String methodName, Object... args) {
        if (obj == null || methodName == null) return null;
        try {
            Class<?>[] argTypes = new Class[args.length];
            for (int i = 0; i < args.length; i++) {
                argTypes[i] = args[i] == null ? null : args[i].getClass();
            }
            Method method = ClassUtils.findMethodOrNull(obj.getClass(), methodName, argTypes);
            if (method == null) {
                return null;
            }
            return ReflectExtensionsKt.callOriginal(method, obj, args);
        } catch (Throwable e) {
            LogUtils.e(TAG, "callOriginal error: " + e.getMessage());
            return null;
        }
    }

    public static Object callStaticMethod(Class<?> clazz, String methodName, Object... args) {
        if (clazz == null || methodName == null) return null;
        Method method = ClassUtils.findMethodOrNull(clazz, methodName, toArgTypes(args));
        if (method == null) {
            method = ClassUtils.findMethodOrNull(clazz, methodName, args == null ? 0 : args.length);
        }
        if (method == null) {
            return null;
        }
        try {
            return invoke(method, null, args);
        } catch (Throwable e) {
            LogUtils.e(TAG, "callStaticMethod " + methodName + " error: " + e.getMessage());
            return null;
        }
    }

    /** 实参类型表：null 实参留给 DSL 按“非基本类型”匹配 */
    private static Class<?>[] toArgTypes(Object[] args) {
        if (args == null) {
            return new Class[0];
        }
        Class<?>[] types = new Class[args.length];
        for (int i = 0; i < args.length; i++) {
            types[i] = args[i] == null ? null : args[i].getClass();
        }
        return types;
    }

    /** 常规 invoke；被 Hook 的目标方法 invoke 失败时回退到未 Hook 的原始实现 */
    private static Object invoke(Method method, Object obj, Object[] args) throws Throwable {
        try {
            return method.invoke(obj, args);
        } catch (Exception e) {
            try {
                return ReflectExtensionsKt.callOriginal(method, obj, args);
            } catch (Exception ignored) {
                throw e;
            }
        }
    }

    public static Object newInstance(Class<?> clazz, Object... args) throws Exception {
        if (clazz == null) return null;
        return ReflectExtensionsKt.newInstanceWithArgs(clazz, args);
    }
}
