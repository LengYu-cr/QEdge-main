package me.lengyu.qedge.coldrain.features;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import me.lengyu.qedge.coldrain.ColdRainCore;
import me.lengyu.qedge.coldrain.ColdRainFeature;
import me.lengyu.qedge.plugin.bean.MsgData;
import me.lengyu.qedge.utils.qq.MsgTool;

public class HourlyChimeFeature implements ColdRainFeature {

    private static final String MY_WEB = "https://api.yuafeng.cn/API/ly/";
    private static Timer timer;
    private static boolean timerStarted = false;

    @Override
    public boolean shouldHandle(MsgData msgData) {
        String text = msgData.msg.trim();
        return text.equals("整点报时") ||
            text.equals("报时测试") ||
            (text.startsWith("切换") && text.endsWith("报时")) ||
            text.startsWith("设置报时内容");
    }

    @Override
    public void handle(MsgData msgData, ColdRainCore core) {
        String text = msgData.msg.trim();
        String groupUin = msgData.peerUin;

        if (text.equals("整点报时")) {
            boolean enabled = core.isGroupFeatureEnabled("feature_hourly", groupUin);
            String mode = core.getConfigString("hourly_mode_" + groupUin, "文字");
            StringBuilder sb = new StringBuilder();
            sb.append("整点报时:\n");
            sb.append("开启/关闭整点报时\n");
            sb.append("切换语音/文字/图片/自定义报时(默认文字)\n");
            sb.append("设置报时内容+内容(可用变量)\n");
            sb.append("报时测试 查看变量\n\n");

            sb.append("整点报时(").append(enabled ? "开" : "关").append(")\n");
            sb.append("报时方式(").append(mode).append(")");
            core.reply(msgData, sb.toString());
            return;
        }

        if (text.equals("报时测试")) {
            sendBaoShi(groupUin, msgData.type, core);
            return;
        }

        if (text.startsWith("切换") && text.endsWith("报时")) {
            String mode = text.replace("切换", "").replace("报时", "");
            if (mode.equals("文字") || mode.equals("语音") || mode.equals("图片") || mode.equals("自定义")) {
                core.setConfigString("hourly_mode_" + groupUin, mode);
                core.reply(msgData, "已切换为" + mode + "报时");
                sendBaoShi(groupUin, msgData.type, core);
            }
            return;
        }

        if (text.startsWith("设置报时内容")) {
            String content = text.substring(6).trim();
            core.setConfigString("hourly_custom_" + groupUin, content);
            core.reply(msgData, "设置自定义报时成功");
            sendBaoShi(groupUin, msgData.type, core);
            return;
        }
    }

    public static void startTimer(ColdRainCore core) {
        if (timerStarted) return;
        timerStarted = true;
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    String time = new SimpleDateFormat("mm:ss", Locale.getDefault()).format(new Date());
                    if (time.startsWith("00:00") || time.equals("00:01")) {
                        checkAndSendAll(core);
                    }
                } catch (Throwable ignored) {}
            }
        }, 1000, 60000);
    }

    private static void checkAndSendAll(ColdRainCore core) {
        if (!core.isFeatureEnabled("feature_hourly")) return;
        for (String key : core.getAllConfigKeys()) {
            if (key.startsWith("group_feature_hourly_") && core.getConfigBoolean(key, false)) {
                String groupUin = key.substring("group_feature_hourly_".length());
                sendBaoShi(groupUin, 2, core);
            }
        }
    }

    private static void sendBaoShi(String peerUin, int type, ColdRainCore core) {
        String mode = core.getConfigString("hourly_mode_" + peerUin, "文字");
        String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        String menu = "整点报时系统提醒您:\n现在是" + time;

        try {
            String content = menu;
            switch (mode) {
                case "文字":
                    content = menu;
                    break;
                case "语音":
                    String voiceUrl = MY_WEB + "baoshi.php";
                    content = menu;
                    break;
                case "图片":
                    String imgUrl = MY_WEB + "time.php";
                    content = menu + "\n[pic=" + imgUrl + "]";
                    break;
                case "自定义":
                    String custom = core.getConfigString("hourly_custom_" + peerUin, "");
                    if (!custom.isEmpty()) {
                        content = custom.replace("{time}", time);
                    } else {
                        content = menu;
                    }
                    break;
            }
            core.reply(peerUin, type, content);
        } catch (Throwable ignored) {}
    }
}
