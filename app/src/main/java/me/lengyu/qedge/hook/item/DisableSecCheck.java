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
 * @Description 拦截 QQ 安全校验（SecUtil）：重打包检测、签名校验、APK 版本读取。
 */
@HookItemAnnotation(value = "拦截安全校验", category = "item")
public class DisableSecCheck extends BaseApiHookItem {
    public static final DisableSecCheck INSTANCE = new DisableSecCheck();
    private static final String TAG = "DisableSecCheck";

    private static final String TARGET_CLASS = "com.tencent.mobileqq.utils.SecUtil";

    private static boolean isEnabled() {
        return ModuleConfig.INSTANCE.getBoolean("disable_sec_check", false);
    }

    @Override
    public void loadHook() {
        try {
            Class<?> cls = ReflectUtils.hostClassLoader.loadClass(TARGET_CLASS);

            // 1. 重打包检测 → 永远返回 false
            Method checkRepack = findMethod(cls, "check0DayRepack", 1);
            if (checkRepack != null) {
                checkRepack.setAccessible(true);
                HookUtils.hookBefore(checkRepack, param -> {
                    if (!isEnabled()) return;
                    param.setResult(false);
                });
                // LogUtils.d(TAG, "hook check0DayRepack success");
            }

            // 2. 拿签名 → 返回空字节（让调用方拿不到真签名）
            Method getSign = findMethod(cls, "getSign", 1);
            if (getSign != null) {
                getSign.setAccessible(true);
                HookUtils.hookBefore(getSign, param -> {
                    if (!isEnabled()) return;
                    param.setResult(new byte[0]);
                });
                // LogUtils.d(TAG, "hook getSign success");
            }

            // 3. 签名 MD5 → 返回空串
            Method getSigHash = findMethod(cls, "getSignatureHash", 1);
            if (getSigHash != null) {
                getSigHash.setAccessible(true);
                HookUtils.hookBefore(getSigHash, param -> {
                    if (!isEnabled()) return;
                    param.setResult("");
                });
                // LogUtils.d(TAG, "hook getSignatureHash success");
            }

            // 4. APK 版本读取 → 返回空串
            Method getVer = findMethod(cls, "getPackageVersion", 1);
            if (getVer != null) {
                getVer.setAccessible(true);
                HookUtils.hookBefore(getVer, param -> {
                    if (!isEnabled()) return;
                    param.setResult("");
                });
                // LogUtils.d(TAG, "hook getPackageVersion success");
            }

            // 5. MD5 计算 → 返回空串（三个重载都拦）
            for (Method m : cls.getDeclaredMethods()) {
                if ("getFileMd5".equals(m.getName())) {
                    m.setAccessible(true);
                    HookUtils.hookBefore(m, param -> {
                        if (!isEnabled()) return;
                        param.setResult("");
                    });
                    // LogUtils.d(TAG, "hook getFileMd5(" + m.getParameterCount() + " args) success");
                }
            }

        } catch (Exception e) {
            LogUtils.e(TAG, "loadHook: " + e.getMessage());
        }
    }

    /**
     * 按名字 + 参数个数找方法
     */
    private Method findMethod(Class<?> clazz, String name, int paramCount) {
        for (Method m : clazz.getDeclaredMethods()) {
            if (m.getName().equals(name) && m.getParameterCount() == paramCount) {
                return m;
            }
        }
        return null;
    }
}