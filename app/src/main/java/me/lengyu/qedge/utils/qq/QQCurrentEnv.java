package me.lengyu.qedge.utils.qq;

import android.app.Activity;
import android.content.Context;
import android.os.Environment;
import com.tencent.common.app.AppInterface;
import com.tencent.common.app.BaseApplicationImpl;
import com.tencent.qqnt.kernel.api.IKernelService;
import com.tencent.qqnt.kernel.api.impl.KernelServiceImpl;
import com.tencent.mobileqq.app.QQAppInterface;
import java.util.Map;
import java.io.File;
import java.lang.reflect.Method;
import me.lengyu.qedge.utils.HostInfo;
import me.lengyu.qedge.utils.ReflectUtils;
import me.lengyu.qedge.utils.LogUtils;
import mqq.app.MobileQQ;
import com.tencent.qqnt.kernel.api.impl.MsgService;
import com.tencent.qqnt.kernel.api.impl.KernelServiceImpl;

   
/**
 * @Author 冷雨
 * @Description QQ 当前环境工具类
 */
public class QQCurrentEnv {
    public static String currentDir;
    public static String currentUin;
    public static String currentUid;
    public static String currentNickname;

    public static MsgService kernelMsgService;
    public static Object richMediaService;
    public static QQAppInterface qqAppInterface;

    public static QQAppInterface getQQAppInterface() {
        if (qqAppInterface != null) {
            return qqAppInterface;
        }
        try {
            MobileQQ mobileQQ = MobileQQ.getMobileQQ();
            if (mobileQQ != null) {
                Object appRuntime = mobileQQ.peekAppRuntime();
                if (appRuntime == null) {
                    appRuntime = mobileQQ.waitAppRuntime();
                }
                if (appRuntime != null) {
                    if (appRuntime instanceof QQAppInterface) {
                        qqAppInterface = (QQAppInterface) appRuntime;
                        return qqAppInterface;
                    }
                }
            }
        } catch (Throwable e) {
            LogUtils.e("QEdge", "getQQAppInterface: " + e.getMessage());
        }
        try {
            Object app = BaseApplicationImpl.getApplication().peekAppRuntime();
            if (app != null) {
                if (app instanceof QQAppInterface) {
                    qqAppInterface = (QQAppInterface) app;
                    return qqAppInterface;
                }
            }
        } catch (Throwable e) {
            LogUtils.e("QEdge", "getQQAppInterface via BaseApplicationImpl: " + e.getMessage());
        }
        return null;
    }

    public static Object getAppRuntime() {
        try {
            MobileQQ mobileQQ = MobileQQ.getMobileQQ();
            if (mobileQQ != null) {
                Object appRuntime = mobileQQ.peekAppRuntime();
                if (appRuntime == null) {
                    appRuntime = mobileQQ.waitAppRuntime();
                }
                if (appRuntime != null) {
                    return appRuntime;
                }
            }
        } catch (Throwable e) {
            LogUtils.e("QEdge", "getAppRuntime: " + e.getMessage());
        }
        try {
            return BaseApplicationImpl.getApplication().peekAppRuntime();
        } catch (Throwable e) {
            LogUtils.e("QEdge", "getAppRuntime via BaseApplicationImpl: " + e.getMessage());
        }
        return null;
    }

    public static Object getRichMediaService() {
        if (richMediaService != null) {
            return richMediaService;
        }
        try {
            QQAppInterface appInterface = getQQAppInterface();
            if (appInterface == null) {
                return null;
            }
            // 从运行时取 QQ 已创建的内核服务，不是 new
            Object kernelService = appInterface.getRuntimeService(IKernelService.class, "");
            if (kernelService == null) {
                return null;
            }
            // getRichMediaService 方法名各版本稳定(IKernelService 接口方法),但运行时真实返回
            // 类型是混淆类 com.tencent.qqnt.kernel.api.ai,stub 声明为 Object,直接调用会因返回
            // 类型签名不匹配报 No virtual method,故按稳定方法名反射调用以规避返回类型变动
            Method method = ReflectUtils.findMethod(kernelService.getClass(), "getRichMediaService");
            if (method == null) {
                LogUtils.e("QQCurrentEnv", "[getRichMediaService] method not found");
                return null;
            }
            richMediaService = method.invoke(kernelService);

            return richMediaService;
        } catch (Throwable e) {
            LogUtils.e("QQCurrentEnv", "[getRichMediaService] error: " + e.getMessage());
            return null;
        }
    }

    public static MsgService getKernelMsgService() {
        if (kernelMsgService != null) {
            return kernelMsgService;
        }
        try {
            QQAppInterface appInterface = getQQAppInterface();
            if (appInterface == null) {
                return null;
            }
            // 从运行时取 QQ 已创建的内核服务，不是 new
            KernelServiceImpl kernelService = (KernelServiceImpl)
            appInterface.getRuntimeService(IKernelService.class, "");
            kernelMsgService = kernelService.getMsgService(); 

            return kernelMsgService;
        } catch (Throwable e) {
            LogUtils.e("QQCurrentEnv", "[getKernelMsgService] error: " + e.getMessage());
            return null;
        }
    }


    public static Activity getActivity() {
        try {
            Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
            Object activityThread = activityThreadClass.getMethod("currentActivityThread", new Class[0]).invoke(null, new Object[0]);
            Object activities = ReflectUtils.getFieldValue(activityThread, "mActivities");
            if (activities instanceof Map) {
                for (Object record : ((Map) activities).values()) {
                    if (record != null) {
                        Boolean paused = (Boolean) ReflectUtils.getFieldValue(record, "paused");
                        if (paused == null || !paused) {
                            Activity activity = (Activity) ReflectUtils.getFieldValue(record, "activity");
                            if (activity != null) {
                                return activity;
                            }
                        }
                    }
                }
            }
        } catch (Throwable e) {
            LogUtils.e("QEdge", "getActivity: " + e.getMessage());
        }
        return null;
    }

    

    public static String getCurrentUin() {
        if (currentUin != null) {
            return currentUin;
        }
        try {
            Object app = getAppRuntime();
            if (app != null) {
                Object uinObj = ReflectUtils.callMethod(app, "getCurrentAccountUin");
                if (uinObj != null) {
                    currentUin = uinObj.toString();
                }
                if (currentUin == null || currentUin.isEmpty()) {
                    uinObj = ReflectUtils.callMethod(app, "getAccount");
                    if (uinObj != null) {
                        currentUin = uinObj.toString();
                    }
                }
                if (currentUin == null || currentUin.isEmpty()) {
                    uinObj = ReflectUtils.callMethod(app, "getAccountUin");
                    if (uinObj != null) {
                        currentUin = uinObj.toString();
                    }
                }
                if (currentUin == null || currentUin.isEmpty()) {
                    uinObj = ReflectUtils.getFieldValue(app, "currentUin");
                    if (uinObj != null) {
                        currentUin = uinObj.toString();
                    }
                }
            }
            if (currentUin == null || currentUin.isEmpty()) {
                currentUin = "";
            }
        } catch (Throwable e) {
            LogUtils.e("QEdge", "getCurrentUin: " + e.getMessage());
        }
        return currentUin;
    }

    public static String getCookieUin() {
        String uin = getCurrentUin();
        if (uin == null || uin.isEmpty()) {
            return "o0000000000";
        }

        if (uin.length() >= 10) {
            return "o" + uin;
        }

        return "o" + String.format("%010d", Long.parseLong(uin));
    }

    public static String getCurrentUid() {
        if (currentUid != null) {
            return currentUid;
        }
        try {
            Object app = getAppRuntime();
            if (app != null) {
                Object uidObj = ReflectUtils.callMethod(app, "getCurrentUid");
                if (uidObj != null) {
                    currentUid = uidObj.toString();
                    return currentUid;
                }
                uidObj = ReflectUtils.getFieldValue(app, "currentUid");
                if (uidObj != null) {
                    currentUid = uidObj.toString();
                    return currentUid;
                }
            }
        } catch (Throwable e) {
            LogUtils.e("QEdge", "getCurrentUid: " + e.getMessage());
        }
        return currentUid;
    }

    public static String getCurrentName() {
        if (currentNickname != null) {
            return currentNickname;
        }
        try {
            Object app = getAppRuntime();
            if (app != null) {
                Object nameObj = ReflectUtils.callMethod(app, "getCurrentNickname");
                if (nameObj != null) {
                    currentNickname = nameObj.toString();
                    return currentNickname;
                }
            }
        } catch (Throwable e) {
            LogUtils.e("QEdge", "getCurrentName: " + e.getMessage());
        }
        return currentNickname;
    }

    public static void reset() {
        currentUin = null;
        currentUid = null;
        currentNickname = null;
        qqAppInterface = null;
        kernelMsgService = null;
    }

    public static String getCurrentDir() {
        if (currentDir != null) {
            return currentDir;
        }
        currentDir = HostInfo.getModuleDataPath();
        File dir = new File(currentDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return currentDir;
    }

    public static Object getGlobalPreference() {
        try {
            QQAppInterface appInterface = getQQAppInterface();
            if (appInterface != null) {
                return appInterface.getGlobalPreference();
            }
            return null;
        } catch (Throwable e) {
            LogUtils.e("QEdge", "getGlobalPreference: " + e.getMessage());
            return null;
        }
    }

    public static String getLocalPath() {
        return Environment.getExternalStorageDirectory().getPath()+"/";
    }

    public static String getHostPath() {
        Context context = HostInfo.getContext();
        if (context == null) {
            return getLocalPath() + "Android/data/com.tencent.mobileqq/";
        }
        return getLocalPath() + "Android/data/" + context.getPackageName() + "/";
    }
}