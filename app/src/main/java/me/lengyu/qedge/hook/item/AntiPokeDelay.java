package me.lengyu.qedge.hook.item;

import java.lang.reflect.Method;

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
import com.tencent.mobileqq.paiyipai.PaiYiPaiHandler;
/**
 * @Author 冷雨
 * @Description 取消拍一拍时间限制
 */
@HookItemAnnotation(value = "取消拍一拍时间限制", category = "item")
public class AntiPokeDelay extends BaseApiHookItem {
    public static final AntiPokeDelay INSTANCE = new AntiPokeDelay();
    private static final String TAG = "AntiPokeDelay";

    private static boolean isEnabled() {
        return ModuleConfig.INSTANCE.getBoolean("anti_poke_delay", false);
    }

    @Override
    public void loadHook() {
        
        Method targetMethod = null;
        
        try {
            Method[] methods = PaiYiPaiHandler.class.getDeclaredMethods();
            for (Method method : methods) {
                if (method.getReturnType().equals(boolean.class)) {
                    method.setAccessible(true);
                    // LogUtils.d(TAG, "loadHook: found method " + method);
                    targetMethod = method;
                    break;
                }
            }
        } catch (Exception e) {
            LogUtils.e(TAG, "loadHook: findMethod failed" + e.getMessage());
            return;
        }

        if (targetMethod != null) {
            HookUtils.hookBefore(targetMethod, param -> {
                if (!isEnabled()) {
                    return;
                }
                param.setResult(true);
            });
        } else {
            LogUtils.e(TAG, "loadHook: targetMethod is null, hook failed");
        }
    }
}
