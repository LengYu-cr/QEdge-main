package me.lengyu.qedge.hook.item;

import java.lang.reflect.Method;

import me.lengyu.qedge.hook.annotation.HookItemAnnotation;
import me.lengyu.qedge.hook.base.BaseApiHookItem;
import me.lengyu.qedge.utils.LogUtils;
import me.lengyu.qedge.utils.ModuleConfig;
import me.lengyu.qedge.utils.ReflectUtils;
import me.lengyu.qedge.utils.HookUtils;

/**
 * @Author 冷雨
 * @Description 屏蔽QQ秀/AI头像
 */
@HookItemAnnotation(value = "屏蔽QQ秀/AI头像", category = "item")
public class DisableAIAvatar extends BaseApiHookItem {
    public static final DisableAIAvatar INSTANCE = new DisableAIAvatar();
    private static final String TAG = "DisableAIAvatar";

    private static boolean isEnabled() {
        return ModuleConfig.INSTANCE.getBoolean("disable_ai_avatar", false);
    }

    @Override
    public void loadHook() {
        try {
            Class<?> implClass = ReflectUtils.hostClassLoader.loadClass(
                "com.tencent.mobileqq.ai.avatar.api.impl.AIAvatarSwitchApiImpl"
            );
            
            // 拦截所有返回 boolean 的方法
            Method[] methods = implClass.getDeclaredMethods();
            for (Method m : methods) {
                Class<?> returnType = m.getReturnType();
                if (returnType == boolean.class || returnType == Boolean.class) {
                    m.setAccessible(true);
                    HookUtils.hookBefore(m, param -> {
                        if (!isEnabled()) return;
                        param.setResult(false);
                    });
                }
            }
                        
        } catch (ClassNotFoundException e) {
            LogUtils.e(TAG, "类未找到: " + e.getMessage());
        } catch (Exception e) {
            LogUtils.e(TAG, "loadHook: " + e.getMessage());
        }
    }
}