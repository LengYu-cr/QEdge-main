package me.lengyu.qedge.hook.item;

import android.os.Message;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import me.lengyu.qedge.hook.annotation.HookItemAnnotation;
import me.lengyu.qedge.hook.base.BaseApiHookItem;
import me.lengyu.qedge.utils.LogUtils;
import me.lengyu.qedge.utils.ModuleConfig;
import me.lengyu.qedge.utils.ReflectUtils;
import me.lengyu.qedge.utils.HookUtils;

/**
 * @Author 冷雨
 * @Description 拦截 QQ WebView 的网页安全 OCR 检测（WebSecurityCheck），阻止其截图并上传识别。
 */
@HookItemAnnotation(value = "拦截网页安全检测", category = "item")
public class DisableWebSecurityCheck extends BaseApiHookItem {
    public static final DisableWebSecurityCheck INSTANCE = new DisableWebSecurityCheck();
    private static final String TAG = "DisableWebSecurityCheck";

    private static final String TARGET_CLASS = "com.tencent.mobileqq.webview.WebSecurityCheck";

    private static boolean isEnabled() {
        return ModuleConfig.INSTANCE.getBoolean("disable_web_security_check", false);
    }

    @Override
    public void loadHook() {
        try {
            Class<?> checkClass = ReflectUtils.hostClassLoader.loadClass(TARGET_CLASS);

            // 主拦截：handleMessage 直接返回 true
            Method handleMessage = checkClass.getDeclaredMethod("handleMessage", Message.class);
            handleMessage.setAccessible(true);
            HookUtils.hookBefore(handleMessage, param -> {
                if (!isEnabled()) return;
                param.setResult(true);   // 跳过整个检测逻辑
            });

            // LogUtils.d(TAG, "hook handleMessage success");

        } catch (Exception e) {
            LogUtils.e(TAG, "loadHook: " + e.getMessage());
        }
    }
}