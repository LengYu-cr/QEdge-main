package me.lengyu.qedge.utils.qq;

import java.lang.reflect.Method;
import me.lengyu.qedge.utils.LogUtils;
import me.lengyu.qedge.utils.ReflectUtils;
import me.lengyu.qedge.utils.qq.QQCurrentEnv;
/**
 * @Author 冷雨
 * @Description Cookie 工具类
 */
public class CookieTool {

    private static Object getTicketManager() {
        try {
            Object appRuntime = QQCurrentEnv.getAppRuntime();
            if (appRuntime != null) {
                try {
                    Method getManagerMethod = ReflectUtils.findMethod(appRuntime.getClass(), "getManager", int.class);
                    if (getManagerMethod != null) {
                        Object manager = getManagerMethod.invoke(appRuntime, 2);
                        if (manager != null) return manager;
                    }
                } catch (Throwable ignored) {}

                Class<?> ticketManagerClass = Class.forName("mqq.manager.TicketManager");
                Object service = QQServiceHelper.getApi(ticketManagerClass);
                if (service != null) return service;

                Method getRuntimeServiceMethod = ReflectUtils.findMethod(appRuntime.getClass(), "getRuntimeService", Class.class, String.class);
                if (getRuntimeServiceMethod != null) {
                    return getRuntimeServiceMethod.invoke(appRuntime, ticketManagerClass, "");
                }
            }
        } catch (Throwable e) {
            LogUtils.e(e);
        }
        return null;
    }

    public static String getRealSkey() {
        try {
            Object manager = getTicketManager();
            if (manager != null) {
                Object result = ReflectUtils.callMethod(manager, "getRealSkey", QQCurrentEnv.getCurrentUin());
                return result != null ? result.toString() : null;
            }
        } catch (Throwable e) {
            LogUtils.e(e);
        }
        return null;
    }

    public static String getSkey() {
        try {
            Object manager = getTicketManager();
            if (manager != null) {
                Object result = ReflectUtils.callMethod(manager, "getSkey", QQCurrentEnv.getCurrentUin());
                return result != null ? result.toString() : null;
            }
        } catch (Throwable e) {
            LogUtils.e(e);
        }
        return null;
    }

    public static String getStweb() {
        try {
            Object manager = getTicketManager();
            if (manager != null) {
                Object result = ReflectUtils.callMethod(manager, "getStweb", QQCurrentEnv.getCurrentUin());
                return result != null ? result.toString() : null;
            }
        } catch (Throwable e) {
            LogUtils.e(e);
        }
        return null;
    }

    public static String getPt4Token(String url) {
        try {
            Object manager = getTicketManager();
            if (manager != null) {
                Object result = ReflectUtils.callMethod(manager, "getPt4Token", QQCurrentEnv.getCurrentUin(), url);
                return result != null ? result.toString() : null;
            }
        } catch (Throwable e) {
            LogUtils.e(e);
        }
        return null;
    }

    public static String getPskey(String url) {
        try {
            Object manager = getTicketManager();
            if (manager != null) {
                Object result = ReflectUtils.callMethod(manager, "getPskey", QQCurrentEnv.getCurrentUin(), url);
                return result != null ? result.toString() : null;
            }
        } catch (Throwable e) {
            LogUtils.e(e);
        }
        return null;
    }

    public static String getFriendRKey() {
        return me.lengyu.qedge.hook.api.OnGetRKey.friendRkey;
    }

    public static String getGroupRKey() {
        return me.lengyu.qedge.hook.api.OnGetRKey.groupRkey;
    }

    public static String getGTK(String url) {
        String pskey = getPskey(url);
        if (pskey != null) {
            long bkn = getBkn(pskey);
            return String.valueOf(bkn);
        }
        return null;
    }

    public static long getBkn(String key) {
        long hash = 5381;
        for (int i = 0; i < key.length(); i++) {
            hash += (hash << 5) + key.charAt(i);
        }
        return hash & 0x7FFFFFFF;
    }

    public static long getGtk(String key) {
        return getBkn(key);
    }

    public static String getP_skey() {
        return getPskey("qun.qq.com");
    }

    public static String getCookie(String key) {
        if ("skey".equalsIgnoreCase(key)) {
            return getSkey();
        } else if ("p_skey".equalsIgnoreCase(key) || "pskey".equalsIgnoreCase(key)) {
            return getP_skey();
        } else if ("real_skey".equalsIgnoreCase(key)) {
            return getRealSkey();
        } else if ("stweb".equalsIgnoreCase(key)) {
            return getStweb();
        }
        return null;
    }

    public static String getGtk() {
        String skey = getSkey();
        if (skey != null) {
            return String.valueOf(getBkn(skey));
        }
        return null;
    }
}