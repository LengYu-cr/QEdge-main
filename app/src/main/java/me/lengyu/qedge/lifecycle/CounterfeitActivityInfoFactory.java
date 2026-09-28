package me.lengyu.qedge.lifecycle;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Build;

import me.lengyu.qedge.utils.HostInfo;
import me.lengyu.qedge.utils.LogUtils;
/**
 * @Author 冷雨
 * @Description 伪活动信息工厂类
 */
public class CounterfeitActivityInfoFactory {

    private static final String TAG = "CounterfeitActivityInfo";

    private static final String[] CANDIDATES = new String[]{
            "com.tencent.mobileqq.activity.QQSettingSettingActivity",
            "com.tencent.mobileqq.activity.QPublicFragmentActivity"};

    /**
     * 伪造宿主 ActivityInfo。
     * 本方法运行在 IPackageManager 代理内部，任何异常穿透都会直接崩宿主，
     * 因此失败一律返回 null（由代理侧回退真实结果），绝不抛出。
     */
    public static ActivityInfo makeProxyActivityInfo(String className, long flags) {
        try {
            Context ctx = HostInfo.getHostContext();
            if (ctx == null) {
                LogUtils.e(TAG, "host context is null");
                return null;
            }
            Class.forName(className);

            PackageManager pm = ctx.getPackageManager();
            if (pm == null) {
                LogUtils.e(TAG, "package manager is null");
                return null;
            }

            for (String activityName : CANDIDATES) {
                try {
                    ActivityInfo proto = pm.getActivityInfo(
                            new ComponentName(ctx.getPackageName(), activityName), (int) flags);
                    return initCommon(proto, className);
                } catch (PackageManager.NameNotFoundException ignored) {
                }
            }
            LogUtils.e(TAG, "no candidate activity found, are we in the host?");
            return null;
        } catch (Throwable e) {
            LogUtils.e(TAG, "makeProxyActivityInfo error: " + e.getMessage());
            return null;
        }
    }

    private static ActivityInfo initCommon(ActivityInfo ai, String name) {
        ai.targetActivity = null;
        ai.taskAffinity = null;
        ai.descriptionRes = 0;
        ai.name = name;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ai.splitName = null;
        }
        ai.configChanges |= ActivityInfo.CONFIG_UI_MODE;
        return ai;
    }
}