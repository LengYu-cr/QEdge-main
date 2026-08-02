package me.lengyu.qedge.coldrain.features;

import me.lengyu.qedge.coldrain.ColdRainCore;
import me.lengyu.qedge.coldrain.ColdRainFeature;
import me.lengyu.qedge.plugin.bean.MsgData;
import me.lengyu.qedge.utils.qq.FriendTool;

public class LikeFeature implements ColdRainFeature {

    @Override
    public boolean shouldHandle(MsgData msgData) {
        String text = msgData.msg.trim();
        return text.equals("赞我点赞") ||
            text.equals("赞我") ||
            text.startsWith("点赞") ||
            text.startsWith("设置赞我回复") ||
            text.equals("查看赞我回复");
    }

    @Override
    public void handle(MsgData msgData, ColdRainCore core) {
        String text = msgData.msg.trim();

        if (!core.isAdminOrSelf(msgData) && !text.equals("赞我")) {
            return;
        }

        if (text.equals("赞我点赞")) {
            boolean enabled = core.isFeatureEnabled("feature_like");
            StringBuilder sb = new StringBuilder();
            sb.append("赞我点赞:\n");
            sb.append("赞我\n");
            sb.append("点赞@QQ/+QQ\n");
            sb.append("开启/关闭赞我点赞(").append(enabled ? "开" : "关").append(")\n");
            sb.append("设置赞我回复#成功回复#失败回复\n");
            sb.append("查看赞我回复\n");
            core.reply(msgData, sb.toString());
            return;
        }

        if (text.startsWith("设置赞我回复")) {
            String content = text.substring(6).trim();
            String[] parts = content.split("#");
            if (parts.length >= 2) {
                core.setConfigString("like_success", parts[0]);
                core.setConfigString("like_fail", parts[1]);
                core.reply(msgData, "设置成功");
            } else {
                core.reply(msgData, "格式错误，应为：设置赞我回复#成功回复#失败回复");
            }
            return;
        }

        if (text.equals("查看赞我回复")) {
            String success = core.getConfigString("like_success", "点赞成功啦~");
            String fail = core.getConfigString("like_fail", "点赞失败了QAQ");
            core.reply(msgData, "成功回复: " + success + "\n失败回复: " + fail);
            return;
        }

        if (text.equals("赞我")) {
            String uin = msgData.userUin;
            String successMsg = core.getConfigString("like_success", "点赞成功啦~");
            String failMsg = core.getConfigString("like_fail", "点赞失败了QAQ");
            try {
                FriendTool.sendZan(uin, 20);
                core.reply(msgData, successMsg.replace("{uin}", uin));
            } catch (Throwable e) {
                core.reply(msgData, failMsg.replace("{uin}", uin) + " - " + e.getMessage());
            }
            return;
        }

        if (text.startsWith("点赞")) {
            String uin = "";
            if (text.contains("@")) {
                if (msgData.atList.isEmpty()) {
                    core.reply(msgData, "请真实艾特对方，不支持多人");
                    return;
                }
                uin = msgData.atList.get(0);
            } else {
                uin = text.substring(2).trim();
                if (!FriendTool.isValidUin(uin)) {
                    return;
                }
            }
            if (!uin.isEmpty()) {
                try {
                    FriendTool.sendZan(uin, 20);
                    core.reply(msgData, "已点赞 " + uin);
                } catch (Throwable e) {
                    core.reply(msgData, "点赞失败: " + e.getMessage());
                }
            }
            return;
        }
    }
}
