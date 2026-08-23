package me.lengyu.qedge.coldrain.features;

import java.util.ArrayList;
import java.util.List;

import me.lengyu.qedge.coldrain.ColdRainCore;
import me.lengyu.qedge.coldrain.ColdRainFeature;
import me.lengyu.qedge.plugin.bean.MsgData;
import me.lengyu.qedge.utils.qq.ExtraTool;
import me.lengyu.qedge.utils.qq.FriendTool;
import me.lengyu.qedge.utils.qq.TroopTool;

/**
 * @Author 冷雨
 * @Description 头衔功能处理
 */
public class TitleFeature implements ColdRainFeature {

    @Override
    public boolean shouldHandle(MsgData msgData) {
        if (msgData.type != 2) return false;
        String text = msgData.msg.trim();
        return text.equals("头衔功能") ||
            text.startsWith("我要头衔") ||
            text.startsWith("我要头街") ||
            text.matches("设置头衔金额[0-9]+") ||
            text.startsWith("上头衔") ||
            text.startsWith("添加头衔违禁词") ||
            text.startsWith("删除头衔违禁词") ||
            text.startsWith("查看头衔违禁词") ||
            text.equals("头衔违禁词列表") ||
            text.startsWith("设置头衔违禁禁言") ||
            text.equals("清空头衔违禁词") ||
            text.startsWith("群员头衔");
    }

    @Override
    public void handle(MsgData msgData, ColdRainCore core) {
        String text = msgData.msg.trim();
        String groupUin = msgData.peerUin;

        if (!core.isAdminOrSelf(msgData) && !text.startsWith("我要头衔") && !text.startsWith("我要头街")) {
            return;
        }

        if (text.equals("头衔功能")) {
            boolean enabled = core.isGroupFeatureEnabled("feature_title", groupUin);
            int money = core.getConfigInt("title_money_" + groupUin, 0);
            StringBuilder sb = new StringBuilder();
            sb.append("自助头衔:\n");
            sb.append("开启/关闭头衔功能(").append(enabled ? "开" : "关").append(")\n");
            sb.append("我要头衔+内容\n");
            sb.append("设置头衔金额+金额(分)\n");
            sb.append("上头衔+QQ 头衔\n");
            sb.append("上头衔@QQ 头衔\n");
            sb.append("添加头衔违禁词+内容\n");
            sb.append("删除头衔违禁词+内容\n");
            sb.append("查看头衔违禁词+内容\n");
            sb.append("头衔违禁词列表\n");
            sb.append("设置头衔违禁禁言+时间\n");
            sb.append("清空头衔违禁词\n");
            sb.append("群员头衔+头衔(一键全员)\n\n");
            sb.append("头衔金额(分):").append(money);
            core.reply(msgData, sb.toString());
            return;
        }

        if (text.matches("设置头衔金额[0-9]+")) {
            int money = Integer.parseInt(text.substring(6).trim());
            core.setConfigInt("title_money_" + groupUin, money);
            core.reply(msgData, "写入自助头衔金额" + money + "分成功～");
            return;
        }

        if (text.startsWith("我要头衔") || text.startsWith("我要头街")) {
            if (!core.isGroupFeatureEnabled("feature_title", groupUin)) {
                core.reply(msgData, "本群未开启头衔功能");
                return;
            }
            String title = text.substring(4).trim();
            if (title.isEmpty()) {
                core.reply(msgData, "请输入头衔内容");
                return;
            }
            List<String> banWords = getTitleBanWords(core, groupUin);
            for (String word : banWords) {
                if (title.contains(word)) {
                    int jy = core.getConfigInt("title_ban_time_" + groupUin, 0);
                    if (jy > 0) {
                        try {
                            TroopTool.INSTANCE.shutUp(groupUin, msgData.userUin, jy * 60L);
                        } catch (Throwable ignored) {}
                    }
                    core.reply(msgData, "触发头衔违禁词");
                    return;
                }
            }
            try {
                ExtraTool.setMemberTitle(groupUin, msgData.userUin, title);
                core.reply(msgData, "QQ:" + msgData.userUin + "\n您的头衔已经ok了哦");
            } catch (Throwable e) {
                core.reply(msgData, "设置头衔失败: " + e.getMessage());
            }
            return;
        }

        if (text.startsWith("上头衔")) {
            String uin = "";
            String title = "";
            if (text.contains("@")) {
                String[] parts = text.split(" ");
                if (parts.length >= 2) {
                       if (msgData.atList.isEmpty()) {
                           core.reply(msgData, "请真实艾特对方，不支持多人");
                           return;
                       }
                    uin = msgData.atList.get(0);
                    title = parts[1];
                }
            } else if (text.matches("上头衔[0-9]+ [\\s\\S]+")) {
                String rest = text.substring(3);
                int idx = rest.indexOf(" ");
                if (idx > 0) {
                    uin = rest.substring(0, idx);
                    if(!FriendTool.isValidUin(uin)){
                        core.reply(msgData, "请输入正确的QQ号");
                        return;
                    }
                    title = rest.substring(idx + 1);
                }
            }
            if (!uin.isEmpty() && !title.isEmpty()) {
                try {
                    ExtraTool.setMemberTitle(groupUin, uin, title);
                    core.reply(msgData, "设置成功");
                } catch (Throwable e) {
                    core.reply(msgData, "设置失败: " + e.getMessage());
                }
            }
            return;
        }

        if (text.startsWith("添加头衔违禁词")) {
            String word = text.substring(7).trim();
            if (word.isEmpty()) {
                core.reply(msgData, "请输入违禁词内容");
                return;
            }
            addTitleBanWord(core, groupUin, word);
            core.reply(msgData, "写入头衔违禁词成功～");
            return;
        }

        if (text.startsWith("删除头衔违禁词")) {
            String word = text.substring(7).trim();
            if (removeTitleBanWord(core, groupUin, word)) {
                core.reply(msgData, "已删除该头衔违禁词～");
            } else {
                core.reply(msgData, "该头衔违禁词不在本群头衔违禁词列表中哦～");
            }
            return;
        }

        if (text.startsWith("查看头衔违禁词")) {
            String word = text.substring(7).trim();
            if (isTitleBanWord(core, groupUin, word)) {
                core.reply(msgData, word + " 是头衔违禁词");
            } else {
                core.reply(msgData, word + " 不是头衔违禁词");
            }
            return;
        }

        if (text.equals("头衔违禁词列表")) {
            List<String> words = getTitleBanWords(core, groupUin);
            StringBuilder sb = new StringBuilder();
            sb.append("本群头衔违禁词列表有:");
            if (words.isEmpty()) {
                sb.append("\n目前还没有头衔违禁词哦～");
            } else {
                int i = 1;
                for (String w : words) {
                    sb.append("\n").append(i).append("、").append(w);
                    i++;
                }
            }
            core.reply(msgData, sb.toString());
            return;
        }

        if (text.equals("清空头衔违禁词")) {
            core.setConfigString("title_ban_words_" + groupUin, "");
            core.reply(msgData, "已清空本群头衔违禁词列表～");
            return;
        }

        if (text.startsWith("设置头衔违禁禁言")) {
            try {
                int minutes = Integer.parseInt(text.substring(8).trim());
                core.setConfigInt("title_ban_time_" + groupUin, minutes);
                core.reply(msgData, "写入头衔违禁时间成功～");
            } catch (Exception e) {
                core.reply(msgData, "设置失败");
            }
            return;
        }

        if (text.startsWith("群员头衔")) {
            String title = text.substring(4).trim();
            if (title.isEmpty()) {
                core.reply(msgData, "请输入头衔内容");
                return;
            }
            core.reply(msgData, "一键全员头衔功能需要遍历群成员列表，暂未实现");
            return;
        }
    }

    private List<String> getTitleBanWords(ColdRainCore core, String groupUin) {
        List<String> words = new ArrayList<>();
        String str = core.getConfigString("title_ban_words_" + groupUin, "");
        if (!str.isEmpty()) {
            for (String w : str.split(",")) {
                if (!w.trim().isEmpty()) words.add(w.trim());
            }
        }
        return words;
    }

    private void addTitleBanWord(ColdRainCore core, String groupUin, String word) {
        List<String> words = getTitleBanWords(core, groupUin);
        for (String w : words) {
            if (w.equals(word)) return;
        }
        words.add(word);
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (sb.length() > 0) sb.append(",");
            sb.append(w);
        }
        core.setConfigString("title_ban_words_" + groupUin, sb.toString());
    }

    private boolean removeTitleBanWord(ColdRainCore core, String groupUin, String word) {
        List<String> words = getTitleBanWords(core, groupUin);
        boolean found = false;
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (!w.equals(word)) {
                if (sb.length() > 0) sb.append(",");
                sb.append(w);
            } else {
                found = true;
            }
        }
        if (found) {
            core.setConfigString("title_ban_words_" + groupUin, sb.toString());
        }
        return found;
    }

    private boolean isTitleBanWord(ColdRainCore core, String groupUin, String word) {
        List<String> words = getTitleBanWords(core, groupUin);
        for (String w : words) {
            if (w.equals(word)) return true;
        }
        return false;
    }
}
