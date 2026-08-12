package me.lengyu.qedge.hook.aoruan;

import java.lang.reflect.Method;
import java.util.List;
import me.lengyu.qedge.hook.annotation.HookItemAnnotation;
import me.lengyu.qedge.hook.base.BaseApiHookItem;
import me.lengyu.qedge.utils.ReflectUtils;
import me.lengyu.qedge.utils.LogUtils;
import me.lengyu.qedge.utils.HookUtils;
import me.lengyu.qedge.utils.HostInfo;
import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindClass;
import org.luckypray.dexkit.query.matchers.ClassMatcher;
import org.luckypray.dexkit.result.ClassData;
import org.luckypray.dexkit.wrap.DexClass;

public class AoRuanHook {

    private static boolean initialized = false;

    public static void loadHook() {
        if (initialized) return;
        initialized = true;

        hookVip();

    }

    public static void hookVip() {
        try {
            ClassLoader classLoader = ReflectUtils.hostClassLoader;
            if (classLoader == null) {
                LogUtils.e("hookVip", "getClassLoader error");
                return;
            }
            Class<?> vipClass = classLoader.loadClass("com.backgrounderaser.baselib.account.VipManager");

            Method vipMethod = null;
            Method vipValidMethod = null;
            Method expireMethod = null;
            Method balanceMethod = null;
            Method dateMethod = null;
            Method[] methods = vipClass.getDeclaredMethods();
            for (Method method : methods) {
                if (method.getName().equals("isVip")) {
                    Class<?>[] paramTypes = method.getParameterTypes();
                    if (paramTypes.length == 0) {
                        vipMethod = method;
                    }
                }
                if (method.getName().equals("isVipValid")) {
                    Class<?>[] paramTypes = method.getParameterTypes();
                    if (paramTypes.length == 0) {
                        vipValidMethod = method;
                    }
                }
                if (method.getName().equals("isExpire")) {
                    Class<?>[] paramTypes = method.getParameterTypes();
                    if (paramTypes.length == 0) {
                        expireMethod = method;
                    }
                }
                if (method.getName().equals("isVipValidOrBalance")) {
                    Class<?>[] paramTypes = method.getParameterTypes();
                    if (paramTypes.length == 0) {
                        balanceMethod = method;
                    }
                }

                if (method.getName().equals("getDeadlineDate")) {
                    Class<?>[] paramTypes = method.getParameterTypes();
                    if (paramTypes.length == 0) {
                        dateMethod = method;
                    }
                }
            }
            // LogUtils.d("hookVip", "isVip method: " + vipMethod);
            
            if (vipMethod != null) {
                HookUtils.hookBefore(vipMethod, param -> {
                    try {
                        param.setResult(true);
                    } catch (Throwable e) {
                        LogUtils.e("hookVip", "isVip callback error: " + e.getMessage());
                    }
                });
            } else {
                LogUtils.e("hookVip", "isVip method not found");
            }

            if (vipValidMethod != null) {
                HookUtils.hookBefore(vipValidMethod, param -> {
                    try {
                        param.setResult(true);
                    } catch (Throwable e) {
                        LogUtils.e("hookVip", "isVipValid callback error: " + e.getMessage());
                    }
                });
            } else {
                LogUtils.e("hookVip", "isVipValid method not found");
            }

            if (expireMethod != null) {
                HookUtils.hookBefore(expireMethod, param -> {
                    try {
                        param.setResult(true);
                    } catch (Throwable e) {
                        LogUtils.e("hookVip", "isExpire callback error: " + e.getMessage());
                    }
                });
            } else {
                LogUtils.e("hookVip", "isExpire method not found");
            }
            
            if (balanceMethod != null) {
                HookUtils.hookBefore(balanceMethod, param -> {
                    try {
                        param.setResult(true);
                    } catch (Throwable e) {
                        LogUtils.e("hookVip", "isVipValidOrBalance callback error: " + e.getMessage());
                    }
                });
            } else {
                LogUtils.e("hookVip", "isVipValidOrBalance method not found");
            }

            if (dateMethod != null) {
                HookUtils.hookBefore(dateMethod, param -> {
                    try {
                        param.setResult("2099.12.31");
                    } catch (Throwable e) {
                        LogUtils.e("hookVip", "getDeadlineDate callback error: " + e.getMessage());
                    }
                });
            } else {
                LogUtils.e("hookVip", "getDeadlineDate method not found");
            }

        } catch (Throwable e) {
            LogUtils.e("hookVip", "loadHook error: " + e.getMessage());
            LogUtils.e(e);
        }
    }

}
