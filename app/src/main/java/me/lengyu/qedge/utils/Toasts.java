package me.lengyu.qedge.utils;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.tencent.util.QQToastUtil;

import me.lengyu.qedge.utils.LogUtils;
import me.lengyu.qedge.utils.QQCurrentEnv;

public class Toasts {

    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static void toast(String message) {
        mainHandler.post(() -> {
            Context context = getContext();
            if (context != null) {
                Toast.makeText(context, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    public static void qqToast(int icon, String message) {
        try {
            if (message != null) {
                QQToastUtil.showQQToastInUiThread(icon, message);
            }
        } catch (Throwable e) {
            LogUtils.e(e);
            toast(message);
        }
    }

    private static Context getContext() {
        Activity activity = QQCurrentEnv.getActivity();
        if (activity != null) {
            return activity;
        }
        Context hostContext = HostInfo.getHostContext();
        if (hostContext instanceof Activity) {
            return hostContext;
        }
        return hostContext;
    }

    public static void showToast(String message) {
        toast(message);
    }
}