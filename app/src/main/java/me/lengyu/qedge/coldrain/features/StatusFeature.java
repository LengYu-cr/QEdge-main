package me.lengyu.qedge.coldrain.features;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.text.format.Formatter;

import java.io.File;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import me.lengyu.qedge.coldrain.ColdRainCore;
import me.lengyu.qedge.coldrain.ColdRainFeature;
import me.lengyu.qedge.common.ModuleScope;
import me.lengyu.qedge.plugin.bean.MsgData;

/**
 * @Author 冷雨
 * @Description 运行状态处理
 */
public class StatusFeature implements ColdRainFeature {

    private static long firstLoadTime = System.currentTimeMillis();

    @Override
    public boolean shouldHandle(MsgData msgData) {
        String text = msgData.msg.trim();
        return text.equals("运行状态");
    }

    @Override
    public void handle(MsgData msgData, ColdRainCore core) {
        if (!core.isAdminOrSelf(msgData)) return;
        
        ModuleScope.launchIOJava("StatusFeature", () -> {
                try {
                    String info = buildStatusInfo(msgData, core);
                    core.reply(msgData, info);
                } catch (Throwable e) {
                    core.reply(msgData, "运行状态:\n出错" + e.getMessage());
                }
        });
    }

    private String buildStatusInfo(MsgData msgData, ColdRainCore core) {
        Context context = core.getContext();
        StringBuilder sb = new StringBuilder();
        sb.append("运行状态:\n");

        sb.append("QQ:").append(core.getMyUin()).append("\n");
        sb.append("在线状态:").append(getOnlineStatus()).append("\n");

        if (msgData.type == 2) {
            sb.append("群号:").append(msgData.peerUin).append("\n");
        } else if (msgData.type == 1) {
            sb.append("好友:").append(msgData.peerUin).append("\n");
        }

        String[] battery = getBatteryStatus(context);
        sb.append("电池类型:").append(battery[2]).append("\n");
        sb.append("电池温度:").append(battery[3]).append("\n");
        sb.append("电池电压:").append(battery[4]).append("\n");
        sb.append("电池电量:").append(battery[0]).append("(").append(battery[1]).append(")\n");
        sb.append("电池健康:").append(getBatteryHealth(context)).append("\n");

        sb.append("剩余运存:").append(getAvailMemory(context)).append("\n");
        sb.append("剩余储存:").append(getAvailableInternalMemorySize(context))
          .append("/").append(getTotalInternalMemorySize(context)).append("\n");

        sb.append("QQ版本:").append(getQQVersion(context)).append("\n");
        sb.append("运行时间:").append(getRunTime()).append("\n");

        sb.append("手机型号:").append(Build.MANUFACTURER).append(" ").append(Build.MODEL).append("\n");
        sb.append("屏幕分辨率:").append(getScreenInfo(context)).append("\n");
        sb.append("CPU架构:").append(Build.SUPPORTED_ABIS.length > 0 ? Build.SUPPORTED_ABIS[0] : "未知").append("(运行:").append(Runtime.getRuntime().availableProcessors()).append("个)\n");

        sb.append("模块版本:").append(getModuleVersion(context)).append("\n");
        sb.append("安卓版本:").append(Build.VERSION.RELEASE).append("(SDK:").append(Build.VERSION.SDK_INT).append(")\n");

        SimpleDateFormat df = new SimpleDateFormat("MM月dd日 HH:mm:ss");
        sb.append("当前时间:").append(df.format(new Date()));

        return sb.toString();
    }

    private String getOnlineStatus() {
        return "未知";
    }

    private String[] getBatteryStatus(Context context) {
        String[] info = new String[5];
        try {
            IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatus = context.registerReceiver(null, ifilter);
            if (batteryStatus == null) {
                info[0] = "未知"; info[1] = "未知"; info[2] = "未知"; info[3] = "未知"; info[4] = "未知";
                return info;
            }
            float level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            float scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            float vol = batteryStatus.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1);
            info[0] = (int)((level / scale) * 100) + "%";
            int pluged = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
            switch (pluged) {
                case BatteryManager.BATTERY_PLUGGED_AC:
                    info[1] = "充电中"; break;
                case BatteryManager.BATTERY_PLUGGED_USB:
                    info[1] = "USB充电中"; break;
                default:
                    info[1] = "放电中"; break;
            }
            info[2] = batteryStatus.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY);
            info[3] = (batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10) + "℃";
            info[4] = vol / 1000 + "V";
        } catch (Throwable e) {
            info[0] = "未知"; info[1] = "未知"; info[2] = "未知"; info[3] = "未知"; info[4] = "未知";
        }
        return info;
    }

    private String getBatteryHealth(Context context) {
        try {
            IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatus = context.registerReceiver(null, ifilter);
            if (batteryStatus == null) return "未知";
            int health = batteryStatus.getIntExtra(BatteryManager.EXTRA_HEALTH, -1);
            switch (health) {
                case BatteryManager.BATTERY_HEALTH_GOOD: return "良好";
                case BatteryManager.BATTERY_HEALTH_OVERHEAT: return "过热";
                case BatteryManager.BATTERY_HEALTH_DEAD: return "损坏";
                case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE: return "电压过高";
                default: return "未知";
            }
        } catch (Throwable e) {
            return "未知";
        }
    }

    private String getAvailMemory(Context context) {
        try {
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
            am.getMemoryInfo(mi);
            return formatSize(mi.availMem);
        } catch (Throwable e) {
            return "未知";
        }
    }

    private String getAvailableInternalMemorySize(Context context) {
        try {
            File path = Environment.getDataDirectory();
            StatFs stat = new StatFs(path.getPath());
            long blockSize = stat.getBlockSizeLong();
            long availableBlocks = stat.getAvailableBlocksLong();
            return Formatter.formatFileSize(context, availableBlocks * blockSize);
        } catch (Throwable e) {
            return "未知";
        }
    }

    private String getTotalInternalMemorySize(Context context) {
        try {
            File path = Environment.getDataDirectory();
            StatFs stat = new StatFs(path.getPath());
            long blockSize = stat.getBlockSizeLong();
            long totalBlocks = stat.getBlockCountLong();
            return Formatter.formatFileSize(context, totalBlocks * blockSize);
        } catch (Throwable e) {
            return "未知";
        }
    }

    private static String formatSize(long size) {
        String suffix = "B";
        float s = size;
        if (s >= 1024) { suffix = "KB"; s /= 1024; }
        if (s >= 1024) { suffix = "MB"; s /= 1024; }
        if (s >= 1024) { suffix = "GB"; s /= 1024; }
        DecimalFormat format = new DecimalFormat(".00");
        return format.format(s) + suffix;
    }

    private String getRunTime() {
        long ms = System.currentTimeMillis() - firstLoadTime;
        long seconds = ms / 1000;
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        StringBuilder sb = new StringBuilder();
        if (hours > 0) sb.append(hours).append("时");
        if (minutes > 0 || hours > 0) sb.append(minutes).append("分");
        sb.append(secs).append("秒");
        return sb.toString();
    }

    private String getQQVersion(Context context) {
        try {
            return context.getPackageManager()
                .getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (Throwable e) {
            return "未知";
        }
    }

    private String getScreenInfo(Context context) {
        try {
            int w = context.getResources().getDisplayMetrics().widthPixels;
            int h = context.getResources().getDisplayMetrics().heightPixels;
            return w + "*" + h;
        } catch (Throwable e) {
            return "未知";
        }
    }

    private String getModuleVersion(Context context) {
        try {
            return context.getPackageManager()
                .getPackageInfo("me.lengyu.qedge", 0).versionName;
        } catch (Throwable e) {
            return "未知";
        }
    }
}
