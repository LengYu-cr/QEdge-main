package me.lengyu.qedge.coldrain.features;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.lengyu.qedge.coldrain.ColdRainCore;
import me.lengyu.qedge.coldrain.ColdRainFeature;
import me.lengyu.qedge.common.ModuleScope;
import me.lengyu.qedge.plugin.bean.ForbidInfo;
import me.lengyu.qedge.plugin.bean.MsgData;
import me.lengyu.qedge.utils.qq.TroopTool;

public class GroupManagerFeature implements ColdRainFeature {

    @Override
    public boolean shouldHandle(MsgData msgData) {
        if (msgData.type != 2) return false;
        String text = msgData.msg.trim();
        if (text.startsWith("/")) {
            return text.equals("/ban") ||
                   text.equals("/禁言") ||
                   text.equals("/unban") ||
                   text.equals("/解禁") ||
                   text.equals("/kick") ||
                   text.equals("/踢") ||
                   text.equals("/kickban") ||
                   text.equals("/踢黑");
        }
        return text.equals("群管菜单") ||
               text.startsWith("禁言") ||
               text.startsWith("解禁") ||
               text.startsWith("解@") ||
               text.startsWith("踢") ||
               text.startsWith("踢黑") ||
               text.equals("全体禁言") ||
               text.equals("全禁") ||
               text.equals("全体解禁") ||
               text.equals("全解") ||
               text.startsWith("上管@") ||
               text.startsWith("下管@") ||
               text.equals("禁言列表") ||
               text.equals("一键解禁") ||
               text.equals("踢出禁言列表") ||
               text.matches("#解禁[0-9]+");
    }

    @Override
    public void handle(MsgData msgData, ColdRainCore core) {
        if (!core.isAdminOrSelf(msgData)) return;

        ModuleScope.launchIOJava("GroupManagerFeature", () -> {
                try {
                    handleGroupCommand(msgData, core);
                } catch (Throwable e) {
                    core.reply(msgData, "群管操作失败: " + e.getMessage());
                }
        });
    }

    private void handleGroupCommand(MsgData msgData, ColdRainCore core) {
        String text = msgData.msg.trim();
        String troopUin = msgData.peerUin;

        if (text.startsWith("/")) {
            handleReplyCommand(msgData, core, troopUin);
            return;
        }

        if (text.equals("群管菜单")) {
            showMenu(msgData, core);
            return;
        }

        if (text.equals("全体禁言") || text.equals("全禁")) {
            TroopTool.INSTANCE.shutUpAll(troopUin, true);
            core.reply(msgData, "ok,已开启全体禁言");
            return;
        }

        if (text.equals("全体解禁") || text.equals("全解")) {
            TroopTool.INSTANCE.shutUpAll(troopUin, false);
            core.reply(msgData, "ok,已关闭全体禁言");
            return;
        }

        if (text.equals("禁言列表")) {
            showForbidList(msgData, core, troopUin);
            return;
        }

        if (text.equals("一键解禁")) {
            List<ForbidInfo> list = TroopTool.INSTANCE.getForbidInfo(troopUin);
            for (ForbidInfo info : list) {
                TroopTool.INSTANCE.shutUp(troopUin, info.uin, 0);
            }
            core.reply(msgData, "已一键解禁所有被禁群员");
            return;
        }

        if (text.equals("踢出禁言列表")) {
            List<ForbidInfo> list = TroopTool.INSTANCE.getForbidInfo(troopUin);
            for (ForbidInfo info : list) {
                TroopTool.INSTANCE.kickGroup(troopUin, info.uin, false);
            }
            core.reply(msgData, "共踢出" + list.size() + "个被禁言成员");
            return;
        }

        if (text.matches("#解禁[0-9]+")) {
            int index = Integer.parseInt(text.substring(3));
            List<ForbidInfo> list = TroopTool.INSTANCE.getForbidInfo(troopUin);
            if (index < 1 || index > list.size()) {
                core.reply(msgData, "序号错误");
                return;
            }
            ForbidInfo info = list.get(index - 1);
            TroopTool.INSTANCE.shutUp(troopUin, info.uin, 0);
            core.reply(msgData, "已解禁" + info.uin);
            return;
        }

        if (text.startsWith("禁言@")) {
            if (msgData.atList != null && !msgData.atList.isEmpty()) {
                long time = parseTimeFromText(text);
                for (String uin : msgData.atList) {
                    if (!canOperate(core, troopUin, uin, msgData)) continue;
                    TroopTool.INSTANCE.shutUp(troopUin, uin, time);
                }
                core.reply(msgData, "ok");
            }
            return;
        }

        Pattern p = Pattern.compile("^禁言([0-9]+)\\s+([0-9]+)");
        Matcher m = p.matcher(text);
        if (m.find()) {
            String uin = m.group(1);
            long minutes = Long.parseLong(m.group(2));
            long time = minutes * 60;
            if (time > 2592000) time = 2592000;
            if (canOperate(core, troopUin, uin, msgData)) {
                TroopTool.INSTANCE.shutUp(troopUin, uin, time);
                core.reply(msgData, "ok");
            }
            return;
        }

        if (text.startsWith("解禁@") || text.startsWith("解@")) {
            if (msgData.atList != null && !msgData.atList.isEmpty()) {
                for (String uin : msgData.atList) {
                    TroopTool.INSTANCE.shutUp(troopUin, uin, 0);
                }
                core.reply(msgData, "ok");
            }
            return;
        }

        Pattern p2 = Pattern.compile("^解禁([0-9]+)");
        Matcher m2 = p2.matcher(text);
        if (m2.find()) {
            String uin = m2.group(1);
            TroopTool.INSTANCE.shutUp(troopUin, uin, 0);
            core.reply(msgData, "ok");
            return;
        }

        if (text.startsWith("踢@")) {
            if (msgData.atList != null && !msgData.atList.isEmpty()) {
                for (String uin : msgData.atList) {
                    if (!canOperate(core, troopUin, uin, msgData)) continue;
                    TroopTool.INSTANCE.kickGroup(troopUin, uin, false);
                }
                core.reply(msgData, "ok");
            }
            return;
        }

        if (text.startsWith("踢黑@")) {
            if (msgData.atList != null && !msgData.atList.isEmpty()) {
                for (String uin : msgData.atList) {
                    if (!canOperate(core, troopUin, uin, msgData)) continue;
                    TroopTool.INSTANCE.kickGroup(troopUin, uin, true);
                }
                core.reply(msgData, "ok,将不会再收到该用户入群申请");
            }
            return;
        }

        Pattern p3 = Pattern.compile("^踢([0-9]+)");
        Matcher m3 = p3.matcher(text);
        if (m3.find()) {
            String uin = m3.group(1);
            if (canOperate(core, troopUin, uin, msgData)) {
                TroopTool.INSTANCE.kickGroup(troopUin, uin, false);
                core.reply(msgData, "ok");
            }
            return;
        }

        Pattern p4 = Pattern.compile("^踢黑([0-9]+)");
        Matcher m4 = p4.matcher(text);
        if (m4.find()) {
            String uin = m4.group(1);
            if (canOperate(core, troopUin, uin, msgData)) {
                TroopTool.INSTANCE.kickGroup(troopUin, uin, true);
                core.reply(msgData, "ok,将不会再收到该用户入群申请");
            }
            return;
        }

        if (text.startsWith("上管@")) {
            if (msgData.atList != null && !msgData.atList.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (String uin : msgData.atList) {
                    TroopTool.INSTANCE.setGroupAdmin(troopUin, uin, true);
                    sb.append(uin).append("上管:成功\n");
                }
                core.reply(msgData, sb.toString().trim());
            }
            return;
        }

        if (text.startsWith("下管@")) {
            if (msgData.atList != null && !msgData.atList.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (String uin : msgData.atList) {
                    TroopTool.INSTANCE.setGroupAdmin(troopUin, uin, false);
                    sb.append(uin).append("下管:成功\n");
                }
                core.reply(msgData, sb.toString().trim());
            }
            return;
        }
    }

    private long parseTimeFromText(String text) {
        try {
            int spaceIdx = text.lastIndexOf(" ");
            if (spaceIdx > 0) {
                String timeStr = text.substring(spaceIdx + 1).trim();
                long minutes = Long.parseLong(timeStr);
                long time = minutes * 60;
                if (time > 2592000) time = 2592000;
                return time;
            }
        } catch (Exception ignored) {}
        return 3600;
    }

    private boolean canOperate(ColdRainCore core, String troopUin, String targetUin, MsgData msgData) {
        if (core.isAdminOrSelfForUin(troopUin, targetUin)) {
            core.reply(msgData, "对方为代管/自身,无法操作");
            return false;
        }
        if (!BlackWhiteListFeature.canOperate(core, troopUin, targetUin)) {
            core.reply(msgData, "对方在白名单中,无法操作");
            return false;
        }
        return true;
    }

    private void handleReplyCommand(MsgData msgData, ColdRainCore core, String troopUin) {
        String text = msgData.msg.trim();
        int msgType = msgData.msgType;
        if (msgType != 9) {
            return;
        }
        String targetUin = String.valueOf(msgData.data.records.get(0).senderUin);
        if (targetUin == null || targetUin.isEmpty()) {
            core.reply(msgData, "获取被回复者信息失败");
            return;
        }
        if (!canOperate(core, troopUin, targetUin, msgData)) {
            return;
        }
        if (text.equals("/ban") || text.equals("/踢黑")) {
            TroopTool.INSTANCE.kickGroup(troopUin, targetUin, true);
            core.reply(msgData, "ok,已踢黑");
        } else if (text.equals("/解") || text.equals("/解禁")) {
            TroopTool.INSTANCE.shutUp(troopUin, targetUin, 0L);
            core.reply(msgData, "ok,已解禁");
        } else if (text.equals("/kick") || text.equals("/踢")) {
            TroopTool.INSTANCE.kickGroup(troopUin, targetUin, false);
            core.reply(msgData, "ok,已踢出");
        } else if (text.equals("/禁言") || text.equals("/神权")) {
            TroopTool.INSTANCE.shutUp(troopUin, targetUin, 2592000);
            core.reply(msgData, "ok,已禁言");
        }
    }

    private void showMenu(MsgData msgData, ColdRainCore core) {
        StringBuilder sb = new StringBuilder();
        sb.append("群管菜单:\n");
        sb.append("/ban 踢黑\n");
        sb.append("/kick 踢\n");
        sb.append("/禁言 /禁 禁言一小时\n");
        sb.append("/解禁 /解 解禁该成员\n");
        sb.append("踢@QQ/+QQ\n");
        sb.append("踢黑@QQ/+QQ\n");
        sb.append("禁言@QQ 时间(分)\n");
        sb.append("全体禁言/解禁\n");
        sb.append("全禁/全解\n");
        sb.append("上管/下管@QQ\n");
        sb.append("禁言列表\n");
        sb.append("Tip:功能仅代管/自身可用");
        core.reply(msgData, sb.toString());
    }

    private void showForbidList(MsgData msgData, ColdRainCore core, String troopUin) {
        List<ForbidInfo> list = TroopTool.INSTANCE.getForbidInfo(troopUin);
        StringBuilder sb = new StringBuilder();
        if (list.isEmpty()) {
            sb.append("暂无禁言列表\n");
        } else {
            int i = 1;
            for (ForbidInfo info : list) {
                long hours = info.duration / 3600;
                long mins = (info.duration % 3600) / 60;
                String timeStr;
                if (hours > 0) {
                    timeStr = hours + "小时" + mins + "分";
                } else {
                    timeStr = mins + "分钟";
                }
                sb.append(i).append("、").append(info.name)
                  .append("(").append(info.uin).append(")\n")
                  .append("剩余:").append(timeStr).append("\n");
                i++;
            }
        }
        sb.append("Tips:发送『一键解禁』可一键解除禁言\n");
        sb.append("    发送『#解禁+序号』可选择解禁\n");
        sb.append("    发送『踢出禁言列表』可一键踢出");
        core.reply(msgData, sb.toString());
    }
}
