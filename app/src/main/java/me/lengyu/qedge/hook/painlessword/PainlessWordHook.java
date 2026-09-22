package me.lengyu.qedge.hook.painlessword;

import java.lang.reflect.Method;
import me.lengyu.qedge.utils.HookUtils;
import me.lengyu.qedge.utils.LogUtils;
import me.lengyu.qedge.utils.ReflectUtils;

/**
 * 无痛单词 (tech.xiangzi.painless) 专业版解锁
 *
 * 目标类: tech.xiangzi.painless.data.remote.model.UserBean
 *   1. isPro() → boolean: 是否专业版（所有会员判断走这里）
 *   2. getPro() → int: 会员类型值
 *   3. getProExpire() → Long: 会员到期时间（返回远期时间戳）
 *   4. getProType() → UserType: 会员类型枚举（返回 PRO_FOREVER）
 *
 * 关联枚举: tech.xiangzi.painless.data.UserType
 *   PRO_FOREVER.getValue() = 永久会员对应的 int 值（运行时反射获取，不写死）
 */
public class PainlessWordHook {

    private static final String TAG = "PainlessWordHook";
    private static final String USER_BEAN_CLASS = "tech.xiangzi.painless.data.remote.model.UserBean";
    private static final String USER_TYPE_CLASS = "tech.xiangzi.painless.data.UserType";
    private static boolean initialized = false;

    public static void loadHook() {
        if (initialized) return;
        initialized = true;

        try {
            ClassLoader cl = ReflectUtils.hostClassLoader;
            if (cl == null) {
                LogUtils.e(TAG, "hostClassLoader is null");
                return;
            }

            // 1. 运行时解析 UserType.PRO_FOREVER 枚举常量及其 int 值
            Object proForeverEnum = resolveProForeverEnum(cl);
            int proForeverValue = -1;
            if (proForeverEnum != null) {
                try {
                    Method getValue = proForeverEnum.getClass().getMethod("getValue");
                    proForeverValue = (int) getValue.invoke(proForeverEnum);
                } catch (Throwable t) {
                    LogUtils.e(TAG, "getValue() invoke failed: " + t.getMessage());
                }
            }

            // 2. 加载 UserBean
            Class<?> userBeanClass;
            try {
                userBeanClass = cl.loadClass(USER_BEAN_CLASS);
            } catch (ClassNotFoundException e) {
                LogUtils.e(TAG, "UserBean 未找到（可能被混淆）: " + e.getMessage());
                return;
            }

            // 3. 定位四个目标方法：无参、返回值匹配
            Method isProMethod = null;
            Method getProMethod = null;
            Method getProExpireMethod = null;
            Method getProTypeMethod = null;
            for (Method m : userBeanClass.getDeclaredMethods()) {
                if (m.getParameterTypes().length != 0) continue;
                if (m.getName().equals("isPro") && m.getReturnType() == boolean.class) {
                    isProMethod = m;
                }
                if (m.getName().equals("getPro") && m.getReturnType() == int.class) {
                    getProMethod = m;
                }
                if (m.getName().equals("getProExpire") && m.getReturnType() == Long.class) {
                    getProExpireMethod = m;
                }
                if (m.getName().equals("getProType")) {
                    getProTypeMethod = m;
                }
            }

            // 4. Hook isPro() → true（核心：所有"是否会员"判断走这里）
            if (isProMethod != null) {
                isProMethod.setAccessible(true);
                HookUtils.hookBefore(isProMethod, param -> param.setResult(true));
            } else {
                LogUtils.e(TAG, "isPro() method not found");
            }

            // 5. Hook getPro() → PRO_FOREVER 的真实 int 值
            if (getProMethod != null && proForeverValue != -1) {
                getProMethod.setAccessible(true);
                final int fv = proForeverValue;
                HookUtils.hookBefore(getProMethod, param -> param.setResult(fv));
            } else {
                LogUtils.e(TAG, "getPro() not hooked (method="
                    + (getProMethod != null) + ", value=" + proForeverValue + ")");
            }

            // 6. Hook getProExpire() → 远期时间戳 (2100-01-01)
            if (getProExpireMethod != null) {
                getProExpireMethod.setAccessible(true);
                HookUtils.hookBefore(getProExpireMethod, param -> param.setResult(4102444800000L));
            } else {
                LogUtils.e(TAG, "getProExpire() method not found");
            }

            // 7. Hook getProType() → UserType.PRO_FOREVER 枚举常量
            if (getProTypeMethod != null && proForeverEnum != null) {
                final Object foreverEnum = proForeverEnum;
                getProTypeMethod.setAccessible(true);
                HookUtils.hookBefore(getProTypeMethod, param -> param.setResult(foreverEnum));
            } else {
                LogUtils.e(TAG, "getProType() not hooked (method="
                    + (getProTypeMethod != null) + ", enum=" + (proForeverEnum != null) + ")");
            }

        } catch (Throwable e) {
            LogUtils.e(TAG, "loadHook error: " + e.getMessage());
            LogUtils.e(e);
        }
    }

    /**
     * 运行时反射获取 UserType.PRO_FOREVER 枚举常量对象
     * 不写死 int 值，避免枚举值在不同版本间变化
     */
    private static Object resolveProForeverEnum(ClassLoader cl) {
        try {
            Class<?> userTypeClass = cl.loadClass(USER_TYPE_CLASS);
            for (Object enumConstant : userTypeClass.getEnumConstants()) {
                if ("PRO_FOREVER".equals(((Enum<?>) enumConstant).name())) {
                    return enumConstant;
                }
            }
            LogUtils.e(TAG, "PRO_FOREVER enum constant not found");
        } catch (Throwable t) {
            LogUtils.e(TAG, "resolveProForeverEnum failed: " + t.getMessage());
        }
        return null;
    }
}
