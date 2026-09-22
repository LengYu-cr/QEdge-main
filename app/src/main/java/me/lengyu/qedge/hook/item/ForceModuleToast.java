package me.lengyu.qedge.hook.item;

import android.text.TextUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import me.lengyu.qedge.hook.annotation.HookItemAnnotation;
import me.lengyu.qedge.hook.base.BaseApiHookItem;
import me.lengyu.qedge.utils.LogUtils;
import me.lengyu.qedge.utils.ModuleConfig;
import me.lengyu.qedge.utils.ReflectUtils;
import me.lengyu.qedge.utils.HookUtils;
import me.lengyu.qedge.utils.Toasts;

@HookItemAnnotation(value = "强制模块Toast", category = "item")
public class ForceModuleToast extends BaseApiHookItem {
    public static final ForceModuleToast INSTANCE = new ForceModuleToast();
    private static final String TAG = "ForceModuleToast";

    private static final String TARGET_CLASS = "com.tencent.mobileqq.widget.QQToast";

    private static boolean isEnabled() {
        return ModuleConfig.INSTANCE.getBoolean("force_module_toast", false);
    }

    @Override
    public void loadHook() {
        try {
            Class<?> cls = ReflectUtils.hostClassLoader.loadClass(TARGET_CLASS);

            // 找 message 字段（可能是 private CharSequence message）
            Field msgField = null;
            for (Field f : cls.getDeclaredFields()) {
                if (CharSequence.class.isAssignableFrom(f.getType())
                        && !f.isSynthetic()) {
                    msgField = f;
                    break;
                }
            }
            if (msgField == null) {
                LogUtils.e(TAG, "找不到 message 字段");
                return;
            }
            msgField.setAccessible(true);
            // LogUtils.d(TAG, "message 字段: " + msgField.getName());

            final Field finalMsgField = msgField;

            // 拦 show() 和 show(int)
            for (Method m : cls.getDeclaredMethods()) {
                if (!m.getName().equals("show")) continue;
                if (m.getParameterCount() > 1) continue;

                m.setAccessible(true);
                final int paramCount = m.getParameterCount();

                HookUtils.hookBefore(m, param -> {
                    if (!isEnabled()) return;

                    try {
                        Object self = param.thisObject;
                        Object msgObj = finalMsgField.get(self);
                        String msg = msgObj == null ? "" : msgObj.toString();

                        if (TextUtils.isEmpty(msg)) {
                            msg = "";
                        }

                        // LogUtils.d(TAG, "拦截 show(" + paramCount + "): " + msg);
                        Toasts.toast(msg);
                    } catch (Throwable t) {
                        LogUtils.e(TAG, "Toasts.toast 出错: " + t);
                    }

                    param.setResult(null);
                });
                // LogUtils.d(TAG, "hook show(" + paramCount + ") 成功");
            }

        } catch (Throwable t) {
            LogUtils.e(TAG, "loadHook: " + t);
        }
    }
}