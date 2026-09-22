package me.lengyu.qedge.hook.ifly;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import me.lengyu.qedge.utils.HookUtils;
import me.lengyu.qedge.utils.HostInfo;
import me.lengyu.qedge.utils.LogUtils;
import me.lengyu.qedge.utils.ReflectUtils;
import me.lengyu.qedge.utils.Toasts;

/**
 * @Author 冷雨
 * @Description 讯飞输入法 破解会员（Vip）
 * <p>
 * 会员链路：VipAidlUtils -> IVipCoreService$Wrapper -> VipCoreServiceBinder -> 服务实现
 * 统一在实体 VipInfo 与客户端两个门面上取值，实体存在时看 VipInfo，实体为 null 时由门面伪造。
 */
public class IFlyHook {

    private static final String CLS_VIP_INFO = "com.iflytek.inputmethod.vip.core.entity.VipInfo";
    private static final String CLS_VIP_LEVEL = "com.iflytek.inputmethod.vip.core.entity.VipLevel";
    private static final String CLS_VIP_WRAPPER = "com.iflytek.inputmethod.vip.core.IVipCoreService$Wrapper";
    private static final String CLS_VIP_AIDL_UTILS = "com.iflytek.inputmethod.vip.VipAidlUtils";
    private static final String CLS_ACCOUNT_HELPER = "com.iflytek.inputmethod.depend.account.helper.AccountInfoHelper";
    private static final String CLS_SIGN_CHECK = "com.iflytek.inputmethod.common.util.IFlyImeSignCheck";

    /** 会员种类：主会员、恋爱会员、友帮会员 */
    private static final String VIP_RESOURCE = "vip_resource";
    private static final String[] VIP_CODES = {VIP_RESOURCE, "vip_lianai", "vip_youbang"};

    /** 伪造的会员有效期：一年 */
    private static final long VIP_DURATION = 365L * 24 * 60 * 60 * 1000;

    /** 账号信息由宿主账号服务异步下发，冷启动取不到，等待后再判定，避免误判为未登录 */
    private static final int LOGIN_CHECK_TIMES = 10;
    private static final long LOGIN_CHECK_INTERVAL = 500;

    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static boolean initialized = false;

    public static void loadHook() {
        if (initialized) return;
        initialized = true;

        // 诊断采集：定位改包/注入后宿主环境里到底被改了什么
        collectHostEnv();

        // 插件可能早于账号态就绪就被加载，校验放行必须立即执行，不能等登录判定
        hookSignCheck();

        // 必须在主线程判定：宿主账号单例由宿主在主线程创建，此处若抢先构造会干扰宿主账号初始化
        mainHandler.post(() -> checkLoginAndLoad(0));
    }

    private static void checkLoginAndLoad(int attempt) {
        if (isHostLoggedIn()) {
            loadVipHook();
            return;
        }
        if (attempt >= LOGIN_CHECK_TIMES) {
            Toasts.toast("请登录后重启应用以加载QEdge");
            return;
        }
        mainHandler.postDelayed(() -> checkLoginAndLoad(attempt + 1), LOGIN_CHECK_INTERVAL);
    }

    private static void loadVipHook() {
        hookVipInfo();
        hookVipCoreService();
        hookVipAidlUtils();
    }

    private static void collectHostEnv() {
        try {
            Context context = HostInfo.getContext();
            if (context == null) {
                LogUtils.e("IFlyHook", "collectHostEnv: context is null");
                return;
            }
        } catch (Throwable e) {
            LogUtils.e("IFlyHook", "collectHostEnv error: " + e.getMessage());
        }
    }

    /**
     * 诊断：native 库加载结果。中文引擎在 so 里，英文不需要，
     * 若 loadLibrary 有失败项即可直接对上症状
     */
    private static void hookNativeLoad() {
        try {
            Class<?> systemClass = Class.forName("java.lang.System");
            HookUtils.hookAfter(systemClass.getDeclaredMethod("loadLibrary", String.class), param -> {
                if (param.hasThrowable()) {
                    LogUtils.e("IFlyHook", "loadLibrary FAILED: " + param.args[0] + " -> " + param.getThrowable());
                }
            });
            HookUtils.hookAfter(systemClass.getDeclaredMethod("load", String.class), param -> {
                if (param.hasThrowable()) {
                    LogUtils.e("IFlyHook", "load FAILED: " + param.args[0] + " -> " + param.getThrowable());
                }
            });
        } catch (Throwable e) {
            LogUtils.e("IFlyHook", "hookNativeLoad error: " + e.getMessage());
        }
    }

    /**
     * 插件包签名校验：宿主改包（换签名）后，插件包内 META-INF 证书与新签名不一致，
     * check 返回 false 会导致本地插件（中文引擎所在）被拒绝加载，表现为中文打不出、英文正常
     */
    private static void hookSignCheck() {
        try {
            ClassLoader classLoader = ReflectUtils.hostClassLoader;
            if (classLoader == null) {
                LogUtils.e("hookSignCheck", "getClassLoader error");
                return;
            }
            Class<?> signCheckClass = classLoader.loadClass(CLS_SIGN_CHECK);
            Method checkMethod = findMethod(signCheckClass, "check", 1);
            if (checkMethod != null) {
                HookUtils.hookBefore(checkMethod, param -> {
                    param.setResult(true);
                });
            }
            hookResult(signCheckClass, "compairPublicKeys", 2, true);
        } catch (Throwable e) {
            LogUtils.e("hookSignCheck", "loadHook error: " + e.getMessage());
        }
    }

    /**
     * 宿主账号是否已登录：会员数据全部挂在账号下，未登录时 hook 无意义
     */
    private static boolean isHostLoggedIn() {
        try {
            ClassLoader classLoader = ReflectUtils.hostClassLoader;
            if (classLoader == null) return false;

            Class<?> helperClass = classLoader.loadClass(CLS_ACCOUNT_HELPER);
            Object companion = helperClass.getField("Companion").get(null);
            Object helper = companion.getClass().getMethod("getInstance").invoke(companion);
            if (helper == null) return false;

            Method isLogin = findMethod(helperClass, "isLogin", 0);
            if (isLogin != null) {
                Object result = isLogin.invoke(helper);
                if (result instanceof Boolean) return (Boolean) result;
            }

            Field userInfo = helperClass.getDeclaredField("userInfo");
            userInfo.setAccessible(true);
            return userInfo.get(helper) != null;
        } catch (Throwable e) {
            LogUtils.e("IFlyHook", "isHostLoggedIn error: " + e.getMessage());
            return false;
        }
    }

    /**
     * 实体层：任何来源的 VipInfo（本地库、网络、Binder 反序列化）都在这里兜底
     */
    private static void hookVipInfo() {
        try {
            ClassLoader classLoader = ReflectUtils.hostClassLoader;
            if (classLoader == null) {
                LogUtils.e("hookVipInfo", "getClassLoader error");
                return;
            }
            Class<?> vipInfoClass = classLoader.loadClass(CLS_VIP_INFO);

            hookResult(vipInfoClass, "isExpired", 0, false);
            hookResult(vipInfoClass, "isNotExpired", 0, true);
            hookResult(vipInfoClass, "isNearExpire", 1, false);
            hookResult(vipInfoClass, "getExpire", 0, false);
            hookResult(vipInfoClass, "getExpireDate", 0, fakeExpireDate());

            Method expireTimeMethod = findMethod(vipInfoClass, "getVipExpireTime", 0);
            if (expireTimeMethod != null) {
                HookUtils.hookBefore(expireTimeMethod, param -> {
                    try {
                        param.setResult(System.currentTimeMillis() + VIP_DURATION);
                    } catch (Throwable e) {
                        LogUtils.e("hookVipInfo", "getVipExpireTime callback error: " + e.getMessage());
                    }
                });
            }
        } catch (Throwable e) {
            LogUtils.e("hookVipInfo", "loadHook error: " + e.getMessage());
            LogUtils.e(e);
        }
    }

    /**
     * 服务门面层：VipAidlUtils 与宿主各进程拿到的包装类，实体为 null 时在这里伪造
     */
    private static void hookVipCoreService() {
        try {
            ClassLoader classLoader = ReflectUtils.hostClassLoader;
            if (classLoader == null) {
                LogUtils.e("hookVipCoreService", "getClassLoader error");
                return;
            }
            Class<?> wrapperClass = classLoader.loadClass(CLS_VIP_WRAPPER);
            hookQueryVipInfoLocal(wrapperClass);
            hookResult(wrapperClass, "hasValidVipInfoLocal", 1, true);
            hookAllVipInfo(wrapperClass);
        } catch (Throwable e) {
            LogUtils.e("hookVipCoreService", "loadHook error: " + e.getMessage());
            LogUtils.e(e);
        }
    }

    private static void hookVipAidlUtils() {
        try {
            ClassLoader classLoader = ReflectUtils.hostClassLoader;
            if (classLoader == null) {
                LogUtils.e("hookVipAidlUtils", "getClassLoader error");
                return;
            }
            Class<?> aidlUtilsClass = classLoader.loadClass(CLS_VIP_AIDL_UTILS);
            hookQueryVipInfoLocal(aidlUtilsClass);
            hookResult(aidlUtilsClass, "hasValidVipInfoLocal", 1, true);
        } catch (Throwable e) {
            LogUtils.e("hookVipAidlUtils", "loadHook error: " + e.getMessage());
            LogUtils.e(e);
        }
    }

    private static void hookQueryVipInfoLocal(Class<?> clazz) {
        Method method = findMethod(clazz, "queryVipInfoLocal", 1);
        if (method == null) return;
        HookUtils.hookBefore(method, param -> {
            try {
                String vipCode = param.args[0] instanceof String ? (String) param.args[0] : VIP_RESOURCE;
                Object vipInfo = createVipInfo(vipCode);
                if (vipInfo != null) param.setResult(vipInfo);
            } catch (Throwable e) {
                LogUtils.e("hookQueryVipInfoLocal", "callback error: " + e.getMessage());
            }
        });
    }

    private static void hookAllVipInfo(Class<?> clazz) {
        Method method = findMethod(clazz, "getAllVipInfo", 0);
        if (method == null) return;
        HookUtils.hookBefore(method, param -> {
            try {
                List<Object> vipInfoList = new ArrayList<>();
                for (String vipCode : VIP_CODES) {
                    Object vipInfo = createVipInfo(vipCode);
                    if (vipInfo != null) vipInfoList.add(vipInfo);
                }
                param.setResult(vipInfoList);
            } catch (Throwable e) {
                LogUtils.e("hookAllVipInfo", "callback error: " + e.getMessage());
            }
        });
    }

    /**
     * 构造伪造的 VipInfo：
     * VipInfo(vipCode, vipType, name, extras, expireDate, expire, autoRenewal, level, rights)
     */
    private static Object createVipInfo(String vipCode) {
        try {
            ClassLoader classLoader = ReflectUtils.hostClassLoader;
            Class<?> vipInfoClass = classLoader.loadClass(CLS_VIP_INFO);
            Class<?> vipLevelClass = classLoader.loadClass(CLS_VIP_LEVEL);
            Constructor<?> constructor = vipInfoClass.getConstructor(String.class, String.class,
                    String.class, String.class, String.class, boolean.class, boolean.class,
                    vipLevelClass, List.class);
            return constructor.newInstance(vipCode, vipCode, getVipName(vipCode), null,
                    fakeExpireDate(), false, false, null, null);
        } catch (Throwable e) {
            LogUtils.e("createVipInfo", "error: " + e.getMessage());
            return null;
        }
    }

    private static String getVipName(String vipCode) {
        if (VIP_RESOURCE.equals(vipCode)) return "超级会员";
        if ("vip_lianai".equals(vipCode)) return "恋爱会员";
        if ("vip_youbang".equals(vipCode)) return "友帮会员";
        return "会员";
    }

    private static String fakeExpireDate() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(new Date(System.currentTimeMillis() + VIP_DURATION));
    }

    private static void hookResult(Class<?> clazz, String methodName, int paramCount, Object result) {
        Method method = findMethod(clazz, methodName, paramCount);
        if (method == null) return;
        HookUtils.hookBefore(method, param -> {
            try {
                param.setResult(result);
            } catch (Throwable e) {
                LogUtils.e("IFlyHook", methodName + " callback error: " + e.getMessage());
            }
        });
    }

    private static Method findMethod(Class<?> clazz, String methodName, int paramCount) {
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.getName().equals(methodName) && method.getParameterTypes().length == paramCount) {
                return method;
            }
        }
        LogUtils.e("IFlyHook", methodName + " method not found");
        return null;
    }
}