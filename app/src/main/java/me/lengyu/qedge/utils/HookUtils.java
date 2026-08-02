package me.lengyu.qedge.utils;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

import java.lang.reflect.Member;
import java.lang.reflect.Method;

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