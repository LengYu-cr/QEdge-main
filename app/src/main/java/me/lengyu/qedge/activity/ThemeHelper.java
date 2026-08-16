package me.lengyu.qedge.activity;

import android.app.UiModeManager;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;

import androidx.core.content.ContextCompat;

public class ThemeHelper {

    public static void applyTheme(Context context) {
        // 默认跟随系统
    }

    public static boolean isNightMode(Context context) {
        int currentNightMode = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return currentNightMode == Configuration.UI_MODE_NIGHT_YES;
    }

    public static void setNightMode(boolean isDark) {
        // 主题设置已通过 ModuleConfig 持久化
    }

    public static void setNightModeForApp(Context context, boolean isDark) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            UiModeManager uiModeManager = ContextCompat.getSystemService(context, UiModeManager.class);
            if (uiModeManager != null) {
                uiModeManager.setApplicationNightMode(isDark
                        ? UiModeManager.MODE_NIGHT_YES
                        : UiModeManager.MODE_NIGHT_NO);
            }
        }
    }
}