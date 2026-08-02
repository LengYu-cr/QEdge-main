package me.lengyu.qedge.coldrain.features;

import java.util.ArrayList;
import java.util.List;

import me.lengyu.qedge.coldrain.ColdRainCore;
import me.lengyu.qedge.coldrain.ColdRainFeature;
import me.lengyu.qedge.plugin.bean.MsgData;

public class BanDetectionFeature implements ColdRainFeature {

    @Override
    public boolean shouldHandle(MsgData msgData) {
        if (msgData.msg == null || msgData.msg.isEmpty()) return false;
        String text = msgData.msg.trim();
        if (msgData.type == 2) {
            return text.equals("违禁系统") ||
                   text.startsWith("添加违禁词") ||
                   text.startsWith("删除违禁词") ||
                   text.equals("违禁词列表") ||
                   text.equals("清空违禁词") ||
                   text.startsWith("设置违禁禁言");
        }
        return false;
    }

    @Override
    public void handle(MsgData msgData, ColdRainCore core) {
        String text = msgData.msg;
        String groupUin = msgData.peerUin;

        if (msgData.type == 2 && core.isAdminOrSelf(msgData)) {
            if (text.trim().equals("违禁系统")) {
                showMenu(msgData, core);
                return;
            }
            if (text.trim().startsWith("添加违禁词")) {
                addBanWord(msgData, core, groupUin);
                return;
            }
            if (text.trim().startsWith("删除违禁词")) {
                removeBanWord(msgData, core, groupUin);
                return;
            }
            if (text.trim().equals("违禁词列表")) {
                showBanList(msgData, core, groupUin);
                return;
            }
            if (text.trim().equals("清空违禁词")) {
                clearBanWords(msgData, core, groupUin);
                return;
            }
            if (text.trim().startsWith("设置违禁禁言")) {
                setBanTime(msgData, core, groupUin);
                return;
            }
        }

        List<String> banWords = getBanWords(core, groupUin);
        if (core.isAdminOrSelf(msgData)) return;

        for (String word : banWords) {
            if (text.toLowerCase().contains(word.toLowerCase())) {
                String reply = core.getConfigString("ban_reply_" + groupUin, "检测到违禁词，请文明发言！");
                int banTime = core.getConfigInt("ban_shutup_time_" + groupUin, 0);
                if (banTime > 0 && msgData.type == 2) {
                    try {
                        me.lengyu.qedge.utils.qq.TroopTool.INSTANCE.shutUp(
                            msgData.peerUin, msgData.userUin, banTime * 60L
                        );
                    } catch (Throwable ignored) {}
                }
                core.reply(msgData, "@" + msgData.userName + " " + reply);
                break;
            }
        }
    }

    private List<String> getBanWords(ColdRainCore core, String groupUin) {
        List<String> banWords = new ArrayList<>();
        String customBanWords = core.getConfigString("ban_words_" + groupUin, "");
        if (!customBanWords.isEmpty()) {
            String[] custom = customBanWords.split(",");
            for (String word : custom) {
                if (!word.trim().isEmpty()) {
                    banWords.add(word.trim());
                }
            }
        }
        return banWords;
    }

    private void showMenu(MsgData msgData, ColdRainCore core) {
        StringBuilder sb = new StringBuilder();
        sb.append("违禁系统:\n");
        sb.append("添加违禁词+内容\n");
        sb.append("删除违禁词+内容\n");
        sb.append("违禁词列表\n");
        sb.append("清空违禁词\n");
        sb.append("设置违禁禁言+时间(分)\n");
        sb.append("Tip:仅代管/自身可管理");
        core.reply(msgData, sb.toString());
    }

    private void addBanWord(MsgData msgData, ColdRainCore core, String groupUin) {
        String word = msgData.msg.trim().substring(5).trim();
        if (word.isEmpty()) {
            core.reply(msgData, "请输入违禁词内容");
            return;
        }
        List<String> banWords = getBanWords(core, groupUin);
        for (String w : banWords) {
            if (w.equalsIgnoreCase(word)) {
                core.reply(msgData, "该违禁词已存在");
                return;
            }
        }
        String custom = core.getConfigString("ban_words_" + groupUin, "");
        if (custom.isEmpty()) {
            custom = word;
        } else {
            custom = custom + "," + word;
        }
        core.setConfigString("ban_words_" + groupUin, custom);
        core.reply(msgData, "已添加违禁词: " + word);
    }

    private void removeBanWord(MsgData msgData, ColdRainCore core, String groupUin) {
        String word = msgData.msg.trim().substring(5).trim();
        if (word.isEmpty()) {
            core.reply(msgData, "请输入违禁词内容");
            return;
        }
        String custom = core.getConfigString("ban_words_" + groupUin, "");
        if (custom.isEmpty()) {
            core.reply(msgData, "违禁词列表为空");
            return;
        }
        String[] words = custom.split(",");
        StringBuilder newWords = new StringBuilder();
        boolean found = false;
        for (String w : words) {
            if (!w.trim().equalsIgnoreCase(word)) {
                if (newWords.length() > 0) newWords.append(",");
                newWords.append(w.trim());
            } else {
                found = true;
            }
        }
        if (!found) {
            core.reply(msgData, "未找到该违禁词");
            return;
        }
        core.setConfigString("ban_words_" + groupUin, newWords.toString());
        core.reply(msgData, "已删除违禁词: " + word);
    }

    private void showBanList(MsgData msgData, ColdRainCore core, String groupUin) {
        List<String> banWords = getBanWords(core, groupUin);
        StringBuilder sb = new StringBuilder();
        sb.append("违禁词列表（共").append(banWords.size()).append("个）:\n");
        int i = 1;
        for (String w : banWords) {
            sb.append(i).append("、").append(w).append("\n");
            i++;
        }
        core.reply(msgData, sb.toString().trim());
    }

    private void clearBanWords(MsgData msgData, ColdRainCore core, String groupUin) {
        core.setConfigString("ban_words_" + groupUin, "");
        core.reply(msgData, "已清空所有违禁词");
    }

    private void setBanTime(MsgData msgData, ColdRainCore core, String groupUin) {
        String text = msgData.msg.trim();
        try {
            int minutes = Integer.parseInt(text.substring(6).trim());
            core.setConfigInt("ban_shutup_time_" + groupUin, minutes);
            core.reply(msgData, "已设置违禁禁言时间: " + minutes + "分钟");
        } catch (Exception e) {
            core.reply(msgData, "设置失败，请输入正确的分钟数");
        }
    }
}
