package me.lengyu.qedge.hook.item;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import com.tencent.biz.qui.quibutton.QUIButton;

import java.lang.reflect.Field;

import me.lengyu.qedge.hook.annotation.HookItemAnnotation;
import me.lengyu.qedge.hook.base.BaseSwitchHookItem;
import me.lengyu.qedge.utils.HookUtils;
import me.lengyu.qedge.utils.LogUtils;
import me.lengyu.qedge.utils.ModuleConfig;
import me.lengyu.qedge.utils.ReflectUtils;

/**
 * @Author 冷雨
 * @Description 跳过扫码确认等待时间：hook QRLoginAuthActivity.doOnCreate，
 * 找到确认按钮(QUIButton)并立即启用 + 重置类型，忽略倒计时，可直接点击确认。
 */
@HookItemAnnotation(value = "跳过扫码确认等待时间", category = "item", tag = "去扫码等待", desc = "可忽略倒计时，直接点击确认即可")
public class SkipScanWaitTime extends BaseSwitchHookItem {

    public static final SkipScanWaitTime INSTANCE = new SkipScanWaitTime();

    private static final String TAG = "SkipScanWaitTime";
    private static final String KEY_ENABLE = "skip_scan_wait_time";
    private static final String ACTIVITY_CLASS = "com.tencent.biz.qrcode.activity.QRLoginAuthActivity";

    private static boolean isEnabled() {
        return ModuleConfig.INSTANCE.getBoolean(KEY_ENABLE, false);
    }

    @Override
    public void onHook() {
        try {
            ClassLoader classLoader = ReflectUtils.hostClassLoader != null
                ? ReflectUtils.hostClassLoader
                : SkipScanWaitTime.class.getClassLoader();

            Class<?> activityClass = Class.forName(ACTIVITY_CLASS, false, classLoader);

            HookUtils.hookClassMethod(
                activityClass,
                "doOnCreate",
                new Class[]{Bundle.class},
                null,
                param -> {
                    if (!isEnabled()) return;
                    try {
                        handle(param.thisObject);
                    } catch (Throwable t) {
                        LogUtils.e(TAG, "doOnCreate after error: " + t.getMessage());
                    }
                }
            );
        } catch (Throwable e) {
            LogUtils.e(TAG, "onHook error: " + e.getMessage());
        }
    }

    private void handle(Object activity) {
        if (activity == null) return;
        try {
            QUIButton confirmButton = findButtonByType(activity);
            if (confirmButton == null) {
                LogUtils.w(TAG, "未找到确认按钮(QUIButton)");
                return;
            }
            // 延迟 100ms 等按钮初始化完成后再启用
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                confirmButton.setEnabled(true);
                confirmButton.setType(0);
                confirmButton.setText("确认登录");
            }, 100);
        } catch (Throwable t) {
            LogUtils.e(TAG, "handle error: " + t.getMessage());
        }
    }

    /** 遍历 Activity 所有字段，取类型为 QUIButton 的字段值 */
    private QUIButton findButtonByType(Object activity) throws IllegalAccessException {
        for (Class<?> c = activity.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field field : c.getDeclaredFields()) {
                if (QUIButton.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    Object value = field.get(activity);
                    if (value instanceof QUIButton) {
                        return (QUIButton) value;
                    }
                }
            }
        }
        return null;
    }
}