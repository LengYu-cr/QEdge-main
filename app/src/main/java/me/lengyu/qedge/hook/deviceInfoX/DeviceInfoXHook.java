package me.lengyu.qedge.hook.deviceInfoX;

import android.content.SharedPreferences;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import me.lengyu.qedge.utils.HostInfo;
import me.lengyu.qedge.utils.HookUtils;
import me.lengyu.qedge.utils.LogUtils;
import me.lengyu.qedge.utils.ReflectUtils;
import me.lengyu.qedge.utils.dexkit.DexKitManager;
import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;

/**
 * 设备信息X (com.liuzh.deviceinfo) 专业版/会员 解锁
 *
 * 逆向结论 (来自 QStory/main.java)：
 *   会员状态只有一个源头 —— hu3.b()
 *     hu3 单例 (hu3.d)，b() 返回 boolean = 是否专业版
 *     - 已登录且 Vip.available → true
 *     - 否则回退 SharedPreferences("com.liuzh.deviceinfo_pro_pref").is_pro_user
 *   全部会员分支(50+处)、广告加载、升级提示、功能限制弹窗均派生自 hu3.b()
 *
 * DexKit 定位策略 (不硬编码混淆类名/方法名)：
 *   1. hu3.b(): 全应用唯一使用字符串 "is_pro_user" 的 boolean 方法
 *   2. nw5 (Vip模型): 同时拥有 boolean 字段(available) + long 字段(vip_expire)，
 *      且有无参 boolean 方法(a) 和 无参 long 方法(b) 的类
 *   3. SharedPreferences: 写入 is_pro_user = true (兜底，其他进程读到也为已激活)
 */
public class DeviceInfoXHook {

    private static final String TAG = "DeviceInfoXHook";
    private static boolean initialized = false;

    public static void loadHook() {
        if (initialized) return;
        initialized = true;

        try {
            String sourceDir = HostInfo.getHostContext().getApplicationInfo().sourceDir;
            if (sourceDir == null) {
                LogUtils.e(TAG, "sourceDir is null");
                return;
            }

            if (!DexKitManager.ensureLibrary()) {
                LogUtils.e(TAG, "DexKit library load failed");
                return;
            }

            DexKitBridge bridge = DexKitBridge.create(sourceDir);
            if (bridge == null) {
                LogUtils.e(TAG, "DexKitBridge.create returned null");
                return;
            }

            try {
                // 1. 写入宿主会员开关 (兜底)
                writeProPref();

                // 2. Hook 会员判定 hu3.b() -> true (核心，一次性解锁全部功能+去广告)
                hookProCheck(bridge);

                // 3. Hook Vip 数据模型 nw5 (UI 一致性: 会员图标/到期时间显示)
                hookVipModel(bridge);
            } finally {
                bridge.close();
            }

        } catch (Throwable e) {
            LogUtils.e(TAG, "loadHook error: " + e.getMessage());
            LogUtils.e(e);
        }
    }

    /**
     * 写入宿主 SharedPreferences，is_pro_user = true
     * 好处: 其他未注入的进程/子模块读取该配置时同样得到"已激活"
     */
    private static void writeProPref() {
        try {
            SharedPreferences sp = HostInfo.getHostContext()
                .getSharedPreferences("com.liuzh.deviceinfo_pro_pref", 0);
            sp.edit().putBoolean("is_pro_user", true).commit();
        } catch (Throwable t) {
            LogUtils.e(TAG, "writeProPref failed: " + t);
        }
    }

    /**
     * 定位 hu3.b(): 全应用唯一使用 "is_pro_user" 字符串的 boolean 返回值方法
     */
    private static void hookProCheck(DexKitBridge bridge) {
        try {
            FindMethod findMethod = new FindMethod();
            findMethod.matcher(new MethodMatcher()
                .usingStrings("is_pro_user")
            );

            List<MethodData> methods = bridge.findMethod(findMethod);
            if (methods.isEmpty()) {
                LogUtils.e(TAG, "hookProCheck: 未找到使用 is_pro_user 的方法");
                return;
            }

            for (MethodData methodData : methods) {
                Method method = methodData.getMethodInstance(ReflectUtils.hostClassLoader);
                if (method == null) continue;
                // 只 hook 返回 boolean 的方法 (hu3.b() 返回 boolean)
                if (method.getReturnType() != boolean.class
                        && method.getReturnType() != Boolean.class) continue;

                HookUtils.hookBefore(method, param -> {
                    param.setResult(true);
                });
            }
        } catch (Throwable e) {
            LogUtils.e(TAG, "hookProCheck error: " + e.getMessage());
        }
    }

    /**
     * 定位 nw5 (Vip 数据模型):
     *   特征: 同时有 boolean 字段(available) + long 字段(vip_expire)
     *         且有无参 boolean 方法(a) 和 无参 long 方法(b)
     */
    private static void hookVipModel(DexKitBridge bridge) {
        try {
            // 找出所有无参返回 boolean 的方法
            FindMethod findBool = new FindMethod();
            findBool.matcher(new MethodMatcher()
                .returnType("boolean")
                .paramTypes(new String[0])
            );
            List<MethodData> boolMethods = bridge.findMethod(findBool);

            // 找出所有无参返回 long 的方法
            FindMethod findLong = new FindMethod();
            findLong.matcher(new MethodMatcher()
                .returnType("long")
                .paramTypes(new String[0])
            );
            List<MethodData> longMethods = bridge.findMethod(findLong);

            // 按类名取交集: 同时拥有这两种无参方法的类
            Set<String> boolClasses = new HashSet<>();
            for (MethodData md : boolMethods) {
                boolClasses.add(md.getClassName());
            }
            Set<String> longClasses = new HashSet<>();
            for (MethodData md : longMethods) {
                longClasses.add(md.getClassName());
            }

            List<String> candidates = new ArrayList<>();
            for (String cls : boolClasses) {
                if (longClasses.contains(cls)) {
                    candidates.add(cls);
                }
            }

            if (candidates.isEmpty()) {
                LogUtils.e(TAG, "hookVipModel: 未找到 Vip 模型候选类");
                return;
            }

            // 反射验证: 必须同时有 boolean 字段和 long 字段
            ClassLoader cl = ReflectUtils.hostClassLoader;
            Class<?> vipClass = null;
            for (String className : candidates) {
                try {
                    Class<?> clazz = cl.loadClass(className);
                    boolean hasBoolField = false;
                    boolean hasLongField = false;
                    for (Field f : clazz.getDeclaredFields()) {
                        if (f.getType() == boolean.class) hasBoolField = true;
                        if (f.getType() == long.class) hasLongField = true;
                    }
                    if (hasBoolField && hasLongField) {
                        vipClass = clazz;
                        break;
                    }
                } catch (Throwable ignored) {}
            }

            if (vipClass == null) {
                LogUtils.e(TAG, "hookVipModel: 未找到含 boolean+long 字段的 Vip 模型");
                return;
            }

            // 定位 a() -> boolean (available) 和 b() -> long (vip_expire)
            Method methodA = null;
            Method methodB = null;
            for (Method m : vipClass.getDeclaredMethods()) {
                if (m.getParameterTypes().length != 0) continue;
                if (m.getReturnType() == boolean.class) methodA = m;
                if (m.getReturnType() == long.class) methodB = m;
            }

            if (methodA != null) {
                HookUtils.hookBefore(methodA, param -> {
                    param.setResult(true);
                });
                
            }
            if (methodB != null) {
                final long expire = 4102444800000L; // 2100-01-01 00:00:00 UTC
                HookUtils.hookBefore(methodB, param -> {
                    param.setResult(expire);
                });
                
            }

        } catch (Throwable e) {
            LogUtils.e(TAG, "hookVipModel error: " + e.getMessage());
        }
    }
}
