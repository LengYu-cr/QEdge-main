package me.lengyu.qedge.utils;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

import java.lang.reflect.Member;

/**
 * @Author 冷雨
 * @Description XposedHook工具类
 */
public class HookUtils {

    private static ClassLoader hookClassLoader;

    public static void setHookClassLoader(ClassLoader classLoader) {
        hookClassLoader = classLoader;
    }

    public static ClassLoader getHookClassLoader() {
        return hookClassLoader;
    }

    public interface HookCallback {
        void onHook(XC_MethodHook.MethodHookParam param);
    }

    public static void hookAfter(Member member, HookCallback callback) {
        XposedBridge.hookMethod(member, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                callback.onHook(param);
            }
        });
    }

    public static void hookBefore(Member member, HookCallback callback) {
        XposedBridge.hookMethod(member, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                callback.onHook(param);
            }
        });
    }

    /**
     * 方法完全替换（XC_MethodReplacement）
     */
    public interface ReplaceCallback {
        Object replaceHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable;
    }
    public static void hookReplace(Member member, ReplaceCallback callback) {
        XposedBridge.hookMethod(member, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                return callback.replaceHookedMethod(param);
            }
        });
    }

    /**
     * 调用原方法（用于方法替换场景下需要执行原逻辑时）
     * 对应 Kotlin HookExtensions.kt 的 invokeOriginal 扩展
     */
    public static Object invokeOriginalMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
        return XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args);
    }

    /**
     * 按类名+方法名+参数类型 hook（支持 before/after），对应 XposedHelpers.findAndHookMethod
     */
    public static void hookClassMethod(Class<?> clazz, String methodName, Class<?>[] paramTypes, HookCallback beforeCallback, HookCallback afterCallback) {
        XC_MethodHook hook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (beforeCallback != null) beforeCallback.onHook(param);
            }
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (afterCallback != null) afterCallback.onHook(param);
            }
        };
        if (paramTypes != null && paramTypes.length > 0) {
            Object[] args = new Object[paramTypes.length + 1];
            System.arraycopy(paramTypes, 0, args, 0, paramTypes.length);
            args[paramTypes.length] = hook;
            XposedHelpers.findAndHookMethod(clazz, methodName, args);
        } else {
            XposedHelpers.findAndHookMethod(clazz, methodName, hook);
        }
    }

    public static void hookAllMethods(Class<?> clazz, String methodName, HookCallback afterCallback) {
        XposedHelpers.findAndHookMethod(clazz, methodName, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                afterCallback.onHook(param);
            }
        });
    }

    public static void hookAllMethods(Class<?> clazz, String methodName, HookCallback beforeCallback, HookCallback afterCallback) {
        XposedHelpers.findAndHookMethod(clazz, methodName, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (beforeCallback != null) {
                    beforeCallback.onHook(param);
                }
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (afterCallback != null) {
                    afterCallback.onHook(param);
                }
            }
        });
    }
}