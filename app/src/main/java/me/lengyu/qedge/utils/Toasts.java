package me.lengyu.qedge.utils;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.tencent.util.QQToastUtil;

import me.lengyu.qedge.utils.LogUtils;
import me.lengyu.qedge.utils.qq.QQCurrentEnv;
import me.lengyu.qedge.utils.HostInfo;

/**
 * @Author 冷雨
 * @Description 自定义Toast工具类
 */
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

    public static void showCustomToast(String message) {
        mainHandler.post(() -> {
            Context context = getContext();
            if (context == null) return;

            boolean isDark = HostInfo.isDarkTheme();

            GradientDrawable bg = new GradientDrawable();
            bg.setColor(isDark
                    ? Color.argb(180, 40, 40, 42)
                    : Color.argb(180, 255, 255, 255));
            bg.setCornerRadius(24f);

            TextView textView = new TextView(context);
            textView.setText(message);
            textView.setTextColor(isDark ? Color.WHITE : Color.BLACK);
            textView.setTextSize(14f);
            textView.setPadding(40, 24, 40, 24);
            textView.setBackground(bg);
            textView.setGravity(Gravity.CENTER);

            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            params.gravity = Gravity.CENTER;
            textView.setLayoutParams(params);

            FrameLayout container = new FrameLayout(context);
            container.addView(textView);

            Toast toast = new Toast(context);
            toast.setDuration(Toast.LENGTH_LONG);
            toast.setView(container);
            toast.show();
        });
    }
}