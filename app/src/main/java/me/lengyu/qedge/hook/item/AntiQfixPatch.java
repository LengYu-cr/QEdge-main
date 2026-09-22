package me.lengyu.qedge.hook.item;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import me.lengyu.qedge.hook.annotation.HookItemAnnotation;
import me.lengyu.qedge.hook.base.BaseApiHookItem;
import me.lengyu.qedge.hook.base.Listener;
import me.lengyu.qedge.utils.LogUtils;
import me.lengyu.qedge.utils.ModuleConfig;
import me.lengyu.qedge.utils.ReflectUtils;
import me.lengyu.qedge.utils.HookUtils;

/**
 * @Author 冷雨
 * @Description 禁用QQ修复补丁
 */
@HookItemAnnotation(value = "禁用QQ修复补丁", category = "item")
public class AntiQfixPatch extends BaseApiHookItem {
    public static final AntiQfixPatch INSTANCE = new AntiQfixPatch();
    private static final String TAG = "AntiQfixPatch";

    private static boolean isEnabled() {
        return ModuleConfig.INSTANCE.getBoolean("anti_qfix_patch", false);
    }

    @Override
    public void loadHook() {
        // 收集所有 getRedirector 重载(1参数、2参数)，全部禁用
        List<Method> targetMethods = new ArrayList<>();
        try {
            Method[] methods = ReflectUtils.hostClassLoader.loadClass("com.tencent.mobileqq.qfix.redirect.PatchRedirectCenter").getDeclaredMethods();
            for (Method method : methods) {
                if (method.getName().equals("getRedirector")) {
                    method.setAccessible(true);
                    targetMethods.add(method);
                }
            }
        } catch (Exception e) {
            LogUtils.e(TAG, "loadHook: findMethod failed" + e.getMessage());
            return;
        }

        if (targetMethods.isEmpty()) {
            LogUtils.e(TAG, "loadHook: 未找到 getRedirector 方法, hook failed");
            return;
        }

        for (Method targetMethod : targetMethods) {
            HookUtils.hookBefore(targetMethod, param -> {
                if (!isEnabled()) {
                    return;
                }
                param.setResult(null);
            });
        }
        // LogUtils.d(TAG, "loadHook: 已禁用 " + targetMethods.size() + " 个 getRedirector 方法");
    }
}
