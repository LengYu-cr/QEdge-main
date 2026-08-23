package me.lengyu.qedge.coldrain.features;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.lengyu.qedge.coldrain.ColdRainCore;
import me.lengyu.qedge.coldrain.ColdRainFeature;
import me.lengyu.qedge.common.ModuleScope;
import me.lengyu.qedge.hook.api.OnTroopJoin;
import me.lengyu.qedge.hook.api.OnTroopShutUp;
import me.lengyu.qedge.plugin.bean.MsgData;
import me.lengyu.qedge.utils.qq.TroopTool;
/**
 * @Author 冷雨
 * @Description 黑白名单处理
 */
public class BlackWhiteListFeature implements ColdRainFeature {

    @Override
    public boolean shouldHandle(MsgData msgData) {
        String text = msgData.msg.trim();
        return text.startsWith("拉黑") ||
               text.startsWith("白名单") ||
               text.startsWith("黑名单") ||
               text.startsWith("解除白名单") ||
               text.startsWith("解除黑名单") ||
               text.equals("黑白名单");
    }

    @Override
    public void handle(MsgData msgData, ColdRainCore core) {
        if (!core.isAdminOrSelf(msgData)) return;

        ModuleScope.launchIOJava("BlackWhiteListFeature", () -> {
                try {
                    handleCommand(msgData, core);
                } catch (Throwable e) {
                    core.reply(msgData, "黑白名单操作失败: " + e.getMessage());
                }
        });
    }

    private void handleCommand(MsgData msgData, ColdRainCore core) {
        String text = msgData.msg.trim();
        String troopUin = msgData.peerUin;

        if (text.equals("黑白名单")) {
            showMenu(msgData, core);
            return;
        }

        if (text.equals("黑名单")) {
            showBlacklist(msgData, core, troopUin);
            return;
        }

        if (text.equals("白名单")) {
            showWhitelist(msgData, core, troopUin);
            return;
        }

        Pattern globalBlack = Pattern.compile("拉黑([0-9]+)");
        Matcher m = globalBlack.matcher(text);
        if (m.find()) {
            String uin = m.group(1);
            addToGlobalBlacklist(core, uin);
            core.reply(msgData, "已将 " + uin + " 添加到全局黑名单");
            return;
        }

        Pattern groupBlack = Pattern.compile("拉黑([0-9]+)本群");
        m = groupBlack.matcher(text);
        if (m.find()) {
            String uin = m.group(1);
            addToGroupBlacklist(core, troopUin, uin);
            core.reply(msgData, "已将 " + uin + " 添加到本群黑名单");
            return;
        }

        Pattern globalWhite = Pattern.compile("白名单([0-9]+)");
        m = globalWhite.matcher(text);
        if (m.find()) {
            String uin = m.group(1);
            addToGlobalWhitelist(core, uin);
            core.reply(msgData, "已将 " + uin + " 添加到全局白名单");
            return;
        }

        Pattern groupWhite = Pattern.compile("白名单([0-9]+)本群");
        m = groupWhite.matcher(text);
        if (m.find()) {
            String uin = m.group(1);
            addToGroupWhitelist(core, troopUin, uin);
            core.reply(msgData, "已将 " + uin + " 添加到本群白名单");
            return;
        }

        Pattern removeGlobalBlack = Pattern.compile("解除黑名单([0-9]+)");
        m = removeGlobalBlack.matcher(text);
        if (m.find()) {
            String uin = m.group(1);
            removeFromGlobalBlacklist(core, uin);
            core.reply(msgData, "已将 " + uin + " 从全局黑名单移除");
            return;
        }

        Pattern removeGroupBlack = Pattern.compile("解除黑名单([0-9]+)本群");
        m = removeGroupBlack.matcher(text);
        if (m.find()) {
            String uin = m.group(1);
            removeFromGroupBlacklist(core, troopUin, uin);
            core.reply(msgData, "已将 " + uin + " 从本群黑名单移除");
            return;
        }

        Pattern removeGlobalWhite = Pattern.compile("解除白名单([0-9]+)");
        m = removeGlobalWhite.matcher(text);
        if (m.find()) {
            String uin = m.group(1);
            removeFromGlobalWhitelist(core, uin);
            core.reply(msgData, "已将 " + uin + " 从全局白名单移除");
            return;
        }

        Pattern removeGroupWhite = Pattern.compile("解除白名单([0-9]+)本群");
        m = removeGroupWhite.matcher(text);
        if (m.find()) {
            String uin = m.group(1);
            removeFromGroupWhitelist(core, troopUin, uin);
            core.reply(msgData, "已将 " + uin + " 从本群白名单移除");
            return;
        }

        if (text.startsWith("拉黑@")) {
            if (msgData.atList != null && !msgData.atList.isEmpty()) {
                for (String uin : msgData.atList) {
                    addToGlobalBlacklist(core, uin);
                }
                core.reply(msgData, "已将选中成员添加到全局黑名单");
            }
            return;
        }

        if (text.startsWith("拉黑@本群")) {
            if (msgData.atList != null && !msgData.atList.isEmpty()) {
                for (String uin : msgData.atList) {
                    addToGroupBlacklist(core, troopUin, uin);
                }
                core.reply(msgData, "已将选中成员添加到本群黑名单");
            }
            return;
        }

        if (text.startsWith("白名单@")) {
            if (msgData.atList != null && !msgData.atList.isEmpty()) {
                for (String uin : msgData.atList) {
                    addToGlobalWhitelist(core, uin);
                }
                core.reply(msgData, "已将选中成员添加到全局白名单");
            }
            return;
        }

        if (text.startsWith("白名单@本群")) {
            if (msgData.atList != null && !msgData.atList.isEmpty()) {
                for (String uin : msgData.atList) {
                    addToGroupWhitelist(core, troopUin, uin);
                }
                core.reply(msgData, "已将选中成员添加到本群白名单");
            }
            return;
        }
    }

    private void showMenu(MsgData msgData, ColdRainCore core) {
        StringBuilder sb = new StringBuilder();
        sb.append("黑白名单菜单:\n");
        sb.append("黑名单 → 查看黑名单列表\n");
        sb.append("白名单 → 查看白名单列表\n");
        sb.append("拉黑QQ → 添加到全局黑名单\n");
        sb.append("拉黑QQ本群 → 添加到本群黑名单\n");
        sb.append("拉黑@ → 批量添加到全局黑名单\n");
        sb.append("拉黑@本群 → 批量添加到本群黑名单\n");
        sb.append("白名单QQ → 添加到全局白名单\n");
        sb.append("白名单QQ本群 → 添加到本群白名单\n");
        sb.append("白名单@ → 批量添加到全局白名单\n");
        sb.append("白名单@本群 → 批量添加到本群白名单\n");
        sb.append("解除黑名单QQ → 从全局移除\n");
        sb.append("解除黑名单QQ本群 → 从本群移除\n");
        sb.append("解除白名单QQ → 从全局移除\n");
        sb.append("解除白名单QQ本群 → 从本群移除\n");
        sb.append("Tip:功能仅代管/自身可用");
        core.reply(msgData, sb.toString());
    }

    private void showBlacklist(MsgData msgData, ColdRainCore core, String troopUin) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== 黑名单列表 ===\n");

        List<String> globalList = getGlobalBlacklist(core);
        if (!globalList.isEmpty()) {
            sb.append("全局黑名单(").append(globalList.size()).append("):\n");
            for (int i = 0; i < globalList.size(); i++) {
                sb.append("  ").append(i + 1).append(". ").append(globalList.get(i)).append("\n");
            }
        }

        List<String> groupList = getGroupBlacklist(core, troopUin);
        if (!groupList.isEmpty()) {
            sb.append("本群黑名单(").append(groupList.size()).append("):\n");
            for (int i = 0; i < groupList.size(); i++) {
                sb.append("  ").append(i + 1).append(". ").append(groupList.get(i)).append("\n");
            }
        }

        sb.append("操作:\n");
        sb.append("拉黑QQ → 添加到全局黑名单\n");
        sb.append("拉黑QQ本群 → 添加到本群黑名单\n");
        sb.append("解除黑名单QQ → 从全局移除\n");
        sb.append("解除黑名单QQ本群 → 从本群移除");

        core.reply(msgData, sb.toString());
    }

    private void showWhitelist(MsgData msgData, ColdRainCore core, String troopUin) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== 白名单列表 ===\n");

        List<String> globalList = getGlobalWhitelist(core);
        if (!globalList.isEmpty()) {
            sb.append("全局白名单(").append(globalList.size()).append("):\n");
            for (int i = 0; i < globalList.size(); i++) {
                sb.append("  ").append(i + 1).append(". ").append(globalList.get(i)).append("\n");
            }
        }

        List<String> groupList = getGroupWhitelist(core, troopUin);
        if (!groupList.isEmpty()) {
            sb.append("本群白名单(").append(groupList.size()).append("):\n");
            for (int i = 0; i < groupList.size(); i++) {
                sb.append("  ").append(i + 1).append(". ").append(groupList.get(i)).append("\n");
            }
        }

        sb.append("操作:\n");
        sb.append("白名单QQ → 添加到全局白名单\n");
        sb.append("白名单QQ本群 → 添加到本群白名单\n");
        sb.append("解除白名单QQ → 从全局移除\n");
        sb.append("解除白名单QQ本群 → 从本群移除");

        core.reply(msgData, sb.toString());
    }

    private List<String> getList(ColdRainCore core, String key) {
        List<String> result = new ArrayList<>();
        String json = core.getConfigString(key, "[]");
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                result.add(arr.getString(i));
            }
        } catch (JSONException ignored) {
        }
        return result;
    }

    private void saveList(ColdRainCore core, String key, List<String> list) {
        JSONArray arr = new JSONArray();
        for (String item : list) {
            arr.put(item);
        }
        core.setConfigString(key, arr.toString());
    }

    public boolean isInGlobalBlacklist(ColdRainCore core, String uin) {
        return getList(core, "global_blacklist").contains(uin);
    }

    public boolean isInGroupBlacklist(ColdRainCore core, String groupUin, String uin) {
        return getList(core, "group_blacklist_" + groupUin).contains(uin);
    }

    public boolean isInBlacklist(ColdRainCore core, String groupUin, String uin) {
        return isInGlobalBlacklist(core, uin) || isInGroupBlacklist(core, groupUin, uin);
    }

    public boolean isInGlobalWhitelist(ColdRainCore core, String uin) {
        return getList(core, "global_whitelist").contains(uin);
    }

    public boolean isInGroupWhitelist(ColdRainCore core, String groupUin, String uin) {
        return getList(core, "group_whitelist_" + groupUin).contains(uin);
    }

    public boolean isInWhitelist(ColdRainCore core, String groupUin, String uin) {
        return isInGlobalWhitelist(core, uin) || isInGroupWhitelist(core, groupUin, uin);
    }

    public void addToGlobalBlacklist(ColdRainCore core, String uin) {
        List<String> list = getList(core, "global_blacklist");
        if (!list.contains(uin)) {
            list.add(uin);
            saveList(core, "global_blacklist", list);
        }
    }

    public void removeFromGlobalBlacklist(ColdRainCore core, String uin) {
        List<String> list = getList(core, "global_blacklist");
        list.remove(uin);
        saveList(core, "global_blacklist", list);
    }

    public void addToGroupBlacklist(ColdRainCore core, String groupUin, String uin) {
        List<String> list = getList(core, "group_blacklist_" + groupUin);
        if (!list.contains(uin)) {
            list.add(uin);
            saveList(core, "group_blacklist_" + groupUin, list);
        }
    }

    public void removeFromGroupBlacklist(ColdRainCore core, String groupUin, String uin) {
        List<String> list = getList(core, "group_blacklist_" + groupUin);
        list.remove(uin);
        saveList(core, "group_blacklist_" + groupUin, list);
    }

    public void addToGlobalWhitelist(ColdRainCore core, String uin) {
        List<String> list = getList(core, "global_whitelist");
        if (!list.contains(uin)) {
            list.add(uin);
            saveList(core, "global_whitelist", list);
        }
    }

    public void removeFromGlobalWhitelist(ColdRainCore core, String uin) {
        List<String> list = getList(core, "global_whitelist");
        list.remove(uin);
        saveList(core, "global_whitelist", list);
    }

    public void addToGroupWhitelist(ColdRainCore core, String groupUin, String uin) {
        List<String> list = getList(core, "group_whitelist_" + groupUin);
        if (!list.contains(uin)) {
            list.add(uin);
            saveList(core, "group_whitelist_" + groupUin, list);
        }
    }

    public void removeFromGroupWhitelist(ColdRainCore core, String groupUin, String uin) {
        List<String> list = getList(core, "group_whitelist_" + groupUin);
        list.remove(uin);
        saveList(core, "group_whitelist_" + groupUin, list);
    }

    public List<String> getGlobalBlacklist(ColdRainCore core) {
        return getList(core, "global_blacklist");
    }

    public List<String> getGroupBlacklist(ColdRainCore core, String groupUin) {
        return getList(core, "group_blacklist_" + groupUin);
    }

    public List<String> getGlobalWhitelist(ColdRainCore core) {
        return getList(core, "global_whitelist");
    }

    public List<String> getGroupWhitelist(ColdRainCore core, String groupUin) {
        return getList(core, "group_whitelist_" + groupUin);
    }

    public static void registerListeners(ColdRainCore core) {
        OnTroopJoin.registerListener(new OnTroopJoin.TroopJoinListener() {
            @Override
            public void onJoin(String troopUin, String memberUin) {
                if (!core.isMasterEnabled()) return;
                if (!core.isGroupMasterEnabled(troopUin)) return;
                if (!core.isGroupFeatureEnabled("feature_black_white_list", troopUin)) return;
                List<String> globalList = getListStatic(core, "global_blacklist");
                List<String> groupList = getListStatic(core, "group_blacklist_" + troopUin);
                if (globalList.contains(memberUin) || groupList.contains(memberUin)) {
                    try {
                        TroopTool.INSTANCE.kickGroup(troopUin, memberUin, true);
                    } catch (Throwable e) {
                    }
                }
            }
        });

        OnTroopShutUp.registerListener(new OnTroopShutUp.TroopShutUpListener() {
            @Override
            public void onShutUp(String troopUin, String memberUin, long time, String opUin) {
                if (!core.isMasterEnabled()) return;
                if (!core.isGroupMasterEnabled(troopUin)) return;
                if (!core.isGroupFeatureEnabled("feature_black_white_list", troopUin)) return;
                if (time > 0) {
                    List<String> globalList = getListStatic(core, "global_whitelist");
                    List<String> groupList = getListStatic(core, "group_whitelist_" + troopUin);
                    if (globalList.contains(memberUin) || groupList.contains(memberUin)) {
                        try {
                            TroopTool.INSTANCE.shutUp(troopUin, memberUin, 0);
                        } catch (Throwable e) {
                        }
                    }
                }
            }
        });
    }

    private static List<String> getListStatic(ColdRainCore core, String key) {
        List<String> result = new ArrayList<>();
        String json = core.getConfigString(key, "[]");
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                result.add(arr.getString(i));
            }
        } catch (JSONException ignored) {
        }
        return result;
    }

    public static boolean canOperate(ColdRainCore core, String troopUin, String targetUin) {
        List<String> globalList = getListStatic(core, "global_whitelist");
        List<String> groupList = getListStatic(core, "group_whitelist_" + troopUin);
        if (globalList.contains(targetUin) || groupList.contains(targetUin)) {
            return false;
        }
        return true;
    }
}
