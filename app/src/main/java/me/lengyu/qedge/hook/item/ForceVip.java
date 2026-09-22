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
 * @Description 解锁本地QQ超级会员/VIP/SVIP，目前可用于开启QQ自带的自动语音转文字、解除表情包收藏500的限制、解除语音发送时长限制，其他的自己去测试。会员不会在主页显示。    
 */
@HookItemAnnotation(value = "解锁本地QQ超级会员", category = "item")
public class ForceVip extends BaseApiHookItem {
    public static final ForceVip INSTANCE = new ForceVip();
    private static final String TAG = "ForceVip";

    private static boolean isEnabled() {
        return ModuleConfig.INSTANCE.getBoolean("force_vip", false);
    }

    @Override
    public void loadHook() {
        try {
            Class<?> vipManagerClass = ReflectUtils.hostClassLoader.loadClass(
                "com.tencent.mobileqq.vip.VipStatusManagerImpl"
            );
            
            // 所有判断方法都返回 true
            String[] methods = {"isVip", "isSVip", "isSuperQQ", "isBigClub", "isStar"};
            
            for (String methodName : methods) {
                Method method = findMethod(vipManagerClass, methodName);
                if (method != null) {
                    method.setAccessible(true);
                    HookUtils.hookBefore(method, param -> {
                        if (!isEnabled()) return;
                        param.setResult(true);
                    });
                }
            }
            
            // 拦截 getPrivilegeFlags，返回所有权限标志
            Method getFlags = findMethod(vipManagerClass, "getPrivilegeFlags");
            if (getFlags != null) {
                getFlags.setAccessible(true);
                HookUtils.hookBefore(getFlags, param -> {
                    if (!isEnabled()) return;
                    // 返回所有权限: VIP(2) + SVIP(4) + SuperQQ(1) + BigClub(8) + Star(16) = 31
                    param.setResult(31);
                });
            }
                        
        } catch (Exception e) {
            LogUtils.e(TAG, "loadHook: " + e.getMessage());
        }
    }

    private Method findMethod(Class<?> clazz, String name) {
        for (Method m : clazz.getDeclaredMethods()) {
            if (m.getName().equals(name)) {
                return m;
            }
        }
        return null;
    }
}