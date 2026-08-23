package me.lengyu.qedge.coldrain.features;

import android.content.Context;

import com.tencent.qqnt.kernelpublic.nativeinterface.Contact;

import me.lengyu.qedge.coldrain.ColdRainCore;
import me.lengyu.qedge.coldrain.ColdRainFeature;
import me.lengyu.qedge.hook.api.OnTroopQuit;
import me.lengyu.qedge.plugin.bean.MsgData;
import me.lengyu.qedge.utils.LogUtils;
import me.lengyu.qedge.utils.qq.MsgTool;
/**
 * @Author 冷雨
 * @Description 退群欢迎系统处理
 */
public class WelcomeQuitFeature implements ColdRainFeature {

    private static boolean listenerRegistered = false;

    @Override
    public boolean shouldHandle(MsgData msgData) {
        String text = msgData.msg.trim();
        return text.equals("退群提示") || text.startsWith("设置退群提示");
    }

    @Override
    public void handle(MsgData msgData, ColdRainCore core) {
        if (!core.isAdminOrSelf(msgData)) return;
        String text = msgData.msg.trim();
        String groupUin = msgData.peerUin;
        if (text.equals("退群提示")) {
            boolean enabled = core.isGroupFeatureEnabled("feature_welcome_quit", groupUin);
            String msg = getQuitMsg(core, groupUin);
            StringBuilder sb = new StringBuilder();
            sb.append("退群提示设置：\n");
            sb.append("状态：").append(enabled ? "开启" : "关闭").append("\n");
            sb.append("提示语：").append(msg).append("\n\n");
            sb.append("发送【退群提示开/关】开启或关闭\n");
            sb.append("发送【设置退群提示 内容】设置提示语\n");
            sb.append("可用变量：{qq} 退群成员QQ\n");
            core.reply(msgData, sb.toString());
        } else if (text.startsWith("设置退群提示")) {
            String content = text.substring(6).trim();
            setQuitMsg(core, groupUin, content);
            core.reply(msgData, "退群提示语已设置为：" + content);
        }
    }

    public static void registerListeners(ColdRainCore core) {
        if (listenerRegistered) return;
        listenerRegistered = true;

        OnTroopQuit.registerListener(new OnTroopQuit.TroopQuitListener() {
            @Override
            public void onQuit(String troopUin, String memberUin) {
                try {
                    if (!core.isMasterEnabled()) return;
                    if (!core.isFeatureEnabled("feature_welcome_quit")) return;
                    if (!core.isGroupMasterEnabled(troopUin)) return;
                    if (!core.isGroupFeatureEnabled("feature_welcome_quit", troopUin)) return;

                    String quitMsg = getQuitMsg(core, troopUin);
                    quitMsg = quitMsg.replace("{qq}", memberUin);

                    core.reply(troopUin, 2, quitMsg);
                } catch (Throwable e) {
                    LogUtils.e(e);
                }
            }
        });
    }

    private static String getQuitMsg(ColdRainCore core, String groupUin) {
        String defaultMsg = core.getConfigString("welcome_quit_msg", "{qq} 已退出本群");
        return core.getConfigString("welcome_quit_msg_" + groupUin, defaultMsg);
    }

    private static void setQuitMsg(ColdRainCore core, String groupUin, String msg) {
        core.setConfigString("welcome_quit_msg_" + groupUin, msg);
    }
}
