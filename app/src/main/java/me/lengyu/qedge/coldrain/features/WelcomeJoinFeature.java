package me.lengyu.qedge.coldrain.features;

import android.content.Context;

import com.tencent.qqnt.kernelpublic.nativeinterface.Contact;

import me.lengyu.qedge.coldrain.ColdRainCore;
import me.lengyu.qedge.coldrain.ColdRainFeature;
import me.lengyu.qedge.hook.api.OnTroopJoin;
import me.lengyu.qedge.plugin.bean.JoinData;
import me.lengyu.qedge.plugin.bean.MsgData;
import me.lengyu.qedge.utils.LogUtils;
import me.lengyu.qedge.utils.qq.MsgTool;
/**
 * @Author 冷雨
 * @Description 进群欢迎系统处理
 */
public class WelcomeJoinFeature implements ColdRainFeature {

    private static boolean listenerRegistered = false;

    @Override
    public boolean shouldHandle(MsgData msgData) {
        String text = msgData.msg.trim();
        return text.equals("进群欢迎") || 
               text.startsWith("设置进群欢迎") ||
               text.equals("提示系统");
    }

    @Override
    public void handle(MsgData msgData, ColdRainCore core) {
        if (!core.isAdminOrSelf(msgData)) return;
        String text = msgData.msg.trim();
        String groupUin = msgData.peerUin;
        
        if (text.equals("提示系统")) {
            boolean joinEnabled = core.isGroupFeatureEnabled("feature_welcome_join", groupUin);
            boolean quitEnabled = core.isGroupFeatureEnabled("feature_welcome_quit", groupUin);
            StringBuilder sb = new StringBuilder();
            sb.append("提示系统:\n");
            sb.append("进群欢迎(状态:").append(joinEnabled ? "开" : "关").append(")\n");
            sb.append("退群提示(状态:").append(quitEnabled ? "开" : "关").append(")\n\n");
            sb.append("发送【进群欢迎】设置进群欢迎\n");
            sb.append("发送【退群提示】设置退群提示");
            core.reply(msgData, sb.toString());
            return;
        }
        
        if (text.equals("进群欢迎")) {
            boolean enabled = core.isGroupFeatureEnabled("feature_welcome_join", groupUin);
            String msg = getWelcomeMsg(core, groupUin);
            StringBuilder sb = new StringBuilder();
            sb.append("进群欢迎设置：\n");
            sb.append("状态：").append(enabled ? "开启" : "关闭").append("\n");
            sb.append("欢迎语：").append(msg).append("\n\n");
            sb.append("发送【进群欢迎开/关】开启或关闭\n");
            sb.append("发送【设置进群欢迎 内容】设置欢迎语\n");
            sb.append("可用变量：{qq} 新成员QQ\n");
            core.reply(msgData, sb.toString());
        } else if (text.startsWith("设置进群欢迎")) {
            String content = text.substring(6).trim();
            setWelcomeMsg(core, groupUin, content);
            core.reply(msgData, "进群欢迎语已设置为：" + content);
        }
    }

    public static void registerListeners(ColdRainCore core) {
        if (listenerRegistered) return;
        listenerRegistered = true;

        OnTroopJoin.registerListener(new OnTroopJoin.TroopJoinListener() {
            @Override
            public void onJoin(JoinData data) {
                try {
                    String troopUin = data.getTroopUin();
                    String memberUin = data.getMemberUin();
                    if (!core.isMasterEnabled()) return;
                    if (!core.isFeatureEnabled("feature_welcome_join")) return;
                    if (!core.isGroupMasterEnabled(troopUin)) return;
                    if (!core.isGroupFeatureEnabled("feature_welcome_join", troopUin)) return;

                    String welcomeMsg = getWelcomeMsg(core, troopUin);
                    welcomeMsg = welcomeMsg.replace("{qq}", memberUin);

                    core.reply(troopUin, 2, welcomeMsg);
                } catch (Throwable e) {
                    LogUtils.e(e);
                }
            }
        });
    }

    private static String getWelcomeMsg(ColdRainCore core, String groupUin) {
        String defaultMsg = core.getConfigString("welcome_join_msg", "欢迎 {qq} 加入本群！");
        return core.getConfigString("welcome_join_msg_" + groupUin, defaultMsg);
    }

    private static void setWelcomeMsg(ColdRainCore core, String groupUin, String msg) {
        core.setConfigString("welcome_join_msg_" + groupUin, msg);
    }
}
