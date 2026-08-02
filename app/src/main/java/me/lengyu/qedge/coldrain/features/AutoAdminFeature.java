package me.lengyu.qedge.coldrain.features;

import me.lengyu.qedge.coldrain.ColdRainCore;
import me.lengyu.qedge.coldrain.ColdRainFeature;
import me.lengyu.qedge.plugin.bean.MsgData;
import me.lengyu.qedge.utils.qq.TroopTool;

public class AutoAdminFeature implements ColdRainFeature {

    @Override
    public boolean shouldHandle(MsgData msgData) {
        if (msgData.type != 2) return false;
        String text = msgData.msg.trim();
        return text.equals("自助上管") ||
            text.equals("我要管理") ||
            text.matches("设置上管金额[0-9]+");
    }

    @Override
    public void handle(MsgData msgData, ColdRainCore core) {
        String text = msgData.msg.trim();
        String groupUin = msgData.peerUin;

        if (!core.isAdminOrSelf(msgData) && !text.equals("我要管理")) {
            return;
        }

        if (text.equals("自助上管")) {
            boolean enabled = core.isGroupFeatureEnabled("feature_autoadmin", groupUin);
            int money = core.getConfigInt("autoadmin_money_" + groupUin, 0);
            StringBuilder sb = new StringBuilder();
            sb.append("自助上管:\n");
            sb.append("开启/关闭自助上管\n");
            sb.append("我要管理\n");
            sb.append("设置上管金额+金额(分)\n\n");
            sb.append("自助上管(").append(enabled ? "开" : "关").append(")\n");
            sb.append("上管金额(分):").append(money);
            core.reply(msgData, sb.toString());
            return;
        }

        if (text.matches("设置上管金额[0-9]+")) {
            int money = Integer.parseInt(text.substring(6).trim());
            core.setConfigInt("autoadmin_money_" + groupUin, money);
            core.reply(msgData, "写入上管金额" + money + "分成功～");
            return;
        }

        if (text.equals("我要管理")) {
            if (!core.isGroupFeatureEnabled("feature_autoadmin", groupUin)) {
                core.reply(msgData, "本群未开启自助上管");
                return;
            }
            try {
                TroopTool.INSTANCE.setGroupAdmin(groupUin, msgData.userUin, true);
                core.reply(msgData, "已将你设置为管理员");
            } catch (Throwable e) {
                core.reply(msgData, "设置管理员失败: " + e.getMessage());
            }
            return;
        }
    }
}
