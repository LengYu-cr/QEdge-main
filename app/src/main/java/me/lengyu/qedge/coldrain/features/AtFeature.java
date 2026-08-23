package me.lengyu.qedge.coldrain.features;

import me.lengyu.qedge.coldrain.ColdRainCore;
import me.lengyu.qedge.coldrain.ColdRainFeature;
import me.lengyu.qedge.common.ModuleScope;
import me.lengyu.qedge.plugin.bean.MsgData;
import me.lengyu.qedge.utils.LogUtils;
/**
 * @Author 冷雨
 * @Description 艾特处理
 */
public class AtFeature implements ColdRainFeature {

    @Override
    public boolean shouldHandle(MsgData msgData) {
        String text = msgData.msg.trim();
        // 指令类
        if (text.equals("艾特处理")) return true;
        if (text.equals("开启艾特回复")) return true;
        if (text.equals("关闭艾特回复")) return true;
        if (text.equals("开启艾特禁言")) return true;
        if (text.equals("关闭艾特禁言")) return true;
        if (text.equals("开启艾特提醒")) return true;
        if (text.equals("关闭艾特提醒")) return true;
        if (text.equals("开启艾特管家禁言")) return true;
        if (text.equals("关闭艾特管家禁言")) return true;
        if (text.startsWith("设置艾特回复")) return true;
        if (text.equals("查看艾特回复")) return true;
        if (text.matches("设置艾特禁言[0-9]+")) return true;
        if (text.equals("查看艾特禁言")) return true;
        if (text.matches("设置艾特管家禁言[0-9]+")) return true;
        if (text.equals("查看变量")) return true;
        
        // 被艾特时触发
        String qq = ColdRainCore.getInstance().getMyUin();
        if (msgData.atList != null && msgData.atList.contains(qq) && msgData.type == 2) {
            return true;
        }
        
        return false;
    }

    @Override
    public void handle(MsgData msgData, ColdRainCore core) {
        ModuleScope.launchIOJava("AtFeature", () -> {
                try {
                    String text = msgData.msg.trim();
                    String qun = msgData.peerUin;
                    String uin = msgData.userUin;
                    if (uin == null || uin.isEmpty()) uin = msgData.userUid;
                    String qq = core.getMyUin();

                    // 显示菜单
                    if (text.equals("艾特处理")) {
                        String replyStatus = "1".equals(core.getConfigString("global_艾特回复_开关", "0")) ? "开" : "关";
                        String remindStatus = "1".equals(core.getConfigString("global_艾特提醒_开关", "0")) ? "开" : "关";
                        String banStatus = "1".equals(core.getConfigString("global_艾特禁言_开关", "0")) ? "开" : "关";
                        String adminBanStatus = "1".equals(core.getConfigString("global_艾特管家禁言_开关", "0")) ? "开" : "关";
                        String menu = "艾特处理:\n" +
                                "开启/关闭艾特回复\n" +
                                "开启/关闭艾特禁言\n" +
                                "开启/关闭艾特提醒\n" +
                                "开启/关闭艾特管家禁言\n" +
                                "设置艾特回复+内容\n" +
                                "查看变量\n" +
                                "设置艾特禁言+时间(秒)\n" +
                                "查看艾特禁言\n" +
                                "设置艾特管家禁言+时间(秒)\n\n" +
                                "艾特回复(" + replyStatus + ")\n" +
                                "艾特提醒(" + remindStatus + ")\n" +
                                "艾特禁言(" + banStatus + ")\n" +
                                "艾特管家禁言(" + adminBanStatus + ")\n\n" +
                                "【提示】以上为全局功能，仅主人可操作";
                        core.reply(msgData, menu);
                        return;
                    }

                    // ========== 艾特回复设置 ==========
                    if (text.equals("开启艾特回复")) {
                        if (core.isAdminOrSelfForUin(qun, uin)) {
                            core.setConfigString("global_艾特回复_开关", "1");
                            core.reply(msgData, "开启艾特回复成功");
                        }
                        return;
                    }

                    if (text.equals("关闭艾特回复")) {
                        if (core.isAdminOrSelfForUin(qun, uin)) {
                            core.setConfigString("global_艾特回复_开关", "0");
                            core.reply(msgData, "关闭艾特回复成功");
                        }
                        return;
                    }

                    if (text.startsWith("设置艾特回复")) {
                        if (core.isAdminOrSelfForUin(qun, uin)) {
                            String content = text.substring(6);
                            core.setConfigString("global_艾特回复_内容", content);
                            core.reply(msgData, "设置成功，当前艾特回复:" + content);
                        }
                        return;
                    }

                    if (text.equals("查看艾特回复")) {
                        if (core.isAdminOrSelfForUin(qun, uin)) {
                            String content = core.getConfigString("global_艾特回复_内容", "");
                            core.reply(msgData, "当前艾特回复:" + content);
                        }
                        return;
                    }

                    // ========== 艾特禁言设置 ==========
                    if (text.equals("开启艾特禁言")) {
                        if (core.isAdminOrSelfForUin(qun, uin)) {
                            core.setConfigString("global_艾特禁言_开关", "1");
                            core.reply(msgData, "开启艾特禁言成功");
                        }
                        return;
                    }

                    if (text.equals("关闭艾特禁言")) {
                        if (core.isAdminOrSelfForUin(qun, uin)) {
                            core.setConfigString("global_艾特禁言_开关", "0");
                            core.reply(msgData, "关闭艾特禁言成功");
                        }
                        return;
                    }

                    if (text.matches("设置艾特禁言[0-9]+")) {
                        if (core.isAdminOrSelfForUin(qun, uin)) {
                            int time = Integer.parseInt(text.substring(6));
                            core.setConfigString("global_艾特禁言_时间", String.valueOf(time));
                            core.reply(msgData, "写入艾特禁言成功～");
                        }
                        return;
                    }

                    if (text.equals("查看艾特禁言")) {
                        if (core.isAdminOrSelfForUin(qun, uin)) {
                            int time = Integer.parseInt(core.getConfigString("global_艾特禁言_时间", "0"));
                            core.reply(msgData, "当前艾特禁言:" + time + "秒");
                        }
                        return;
                    }

                    // ========== 艾特提醒设置 ==========
                    if (text.equals("开启艾特提醒")) {
                        if (core.isAdminOrSelfForUin(qun, uin)) {
                            core.setConfigString("global_艾特提醒_开关", "1");
                            core.reply(msgData, "开启艾特提醒成功");
                        }
                        return;
                    }

                    if (text.equals("关闭艾特提醒")) {
                        if (core.isAdminOrSelfForUin(qun, uin)) {
                            core.setConfigString("global_艾特提醒_开关", "0");
                            core.reply(msgData, "关闭艾特提醒成功");
                        }
                        return;
                    }

                    // ========== 艾特管家禁言开关 ==========
                    if (text.equals("开启艾特管家禁言")) {
                        if (core.isAdminOrSelfForUin(qun, uin)) {
                            core.setConfigString("global_艾特管家禁言_开关", "1");
                            core.reply(msgData, "开启艾特管家禁言成功");
                        }
                        return;
                    }

                    if (text.equals("关闭艾特管家禁言")) {
                        if (core.isAdminOrSelfForUin(qun, uin)) {
                            core.setConfigString("global_艾特管家禁言_开关", "0");
                            core.reply(msgData, "关闭艾特管家禁言成功");
                        }
                        return;
                    }

                    // ========== 艾特管家禁言设置 ==========
                    if (text.matches("设置艾特管家禁言[0-9]+")) {
                        if (core.isAdminOrSelfForUin(qun, uin)) {
                            int time = Integer.parseInt(text.substring(8));
                            core.setConfigString("global_艾特管家禁言_时间", String.valueOf(time));
                            core.reply(msgData, "写入艾特管家禁言成功～");
                        }
                        return;
                    }

                    // ========== 查看变量 ==========
                    if (text.equals("查看变量")) {
                        if (core.isAdminOrSelfForUin(qun, uin)) {
                            String vars = "可用变量:\n" +
                                    "[at] → @发送者\n" +
                                    "[qq] → 机器人QQ\n" +
                                    "[uin] → 发送者QQ\n" +
                                    "[qun] → 群号\n" +
                                    "[Name] → 发送者昵称\n" +
                                    "[GroupName] → 群名称\n" +
                                    "[time] → 发送时间\n" +
                                    "[图片URL] → 图片\n" +
                                    "[GroupMemberCount] → 群成员数\n" +
                                    "[一言] → 随机一言";
                            core.reply(msgData, vars);
                        }
                        return;
                    }

                    // ========== 被艾特时的处理 ==========
                    // 被艾特触发的自动行为(回复/禁言/提醒/管家禁言)仅在已开机的群生效；
                    // 上方的管理员指令与「艾特处理」菜单不受此限制。
                    boolean groupEnabled = msgData.type != 2 || core.isGroupMasterEnabled(qun);
                    if (groupEnabled && msgData.atList != null && msgData.atList.contains(qq)) {
                        // 艾特回复
                        if ("1".equals(core.getConfigString("global_艾特回复_开关", "0"))) {
                            String replyContent = core.getConfigString("global_艾特回复_内容", "");
                            if (!replyContent.isEmpty()) {
                                String formattedReply = formatAnswer(replyContent, uin, qq, qun, msgData.userName);
                                core.reply(msgData, formattedReply);
                            }
                        }

                        // 艾特禁言
                        if ("1".equals(core.getConfigString("global_艾特禁言_开关", "0"))) {
                            int banTime = Integer.parseInt(core.getConfigString("global_艾特禁言_时间", "0"));
                            if (banTime > 0 && msgData.type == 2) {
                                try {
                                    me.lengyu.qedge.utils.qq.TroopTool.INSTANCE.shutUp(qun, uin, banTime * 1000L);
                                } catch (Throwable ignored) {}
                            }
                        }

                        // 艾特提醒
                        if ("1".equals(core.getConfigString("global_艾特提醒_开关", "0"))) {
                            String timeStr = timestampToDate(System.currentTimeMillis());
                            String notifyMsg = "主人，收到艾特啦！\n" +
                                    "QQ:" + uin + "\n" +
                                    "昵称:" + (msgData.userName != null ? msgData.userName : uin) + "\n" +
                                    "群号:" + qun + "\n" +
                                    "群名:" + (msgData.peerName != null ? msgData.peerName : "") + "\n" +
                                    "消息内容:" + text + "\n" +
                                    "时间:" + timeStr;
                            try {
                                core.reply(qq, 1, notifyMsg);
                            } catch (Throwable ignored) {}
                        }
                    }

                    // ========== 艾特管家禁言 ==========
                    if (groupEnabled && "1".equals(core.getConfigString("global_艾特管家禁言_开关", "0"))) {
                        if (uin.equals("2854196310") && text.endsWith("嗨~，我是Q群管家，可以发送入群欢迎和定时消息，暂时还不能和你对话哦。")) {
                            if (msgData.atList != null && !msgData.atList.isEmpty()) {
                                String atUin = msgData.atList.get(0);
                                int shutUpTime = Integer.parseInt(core.getConfigString("global_艾特管家禁言_时间", "0"));
                                if (shutUpTime > 0) {
                                    try {
                                        me.lengyu.qedge.utils.qq.TroopTool.INSTANCE.shutUp(qun, atUin, shutUpTime * 1000L);
                                        core.reply(msgData, atUin + "被禁言" + shutUpTime + "秒\n原因:被Q群管家艾特");
                                    } catch (Throwable ignored) {}
                                }
                            }
                        }
                    }

                } catch (Throwable e) {
                    LogUtils.e(e);
                }
        });
    }

    private String formatAnswer(String answer, String uin, String qq, String qun, String userName) {
        return answer.replace("[at]", "[atUin=" + uin + "]")
                .replace("[qq]", qq)
                .replace("[uin]", uin)
                .replace("[qun]", qun)
                .replace("[Name]", userName != null ? userName : uin)
                .replace("[图片", "\n[pic=");
    }

    private String timestampToDate(long timestamp) {
        java.util.Date date = new java.util.Date(timestamp);
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(date);
    }
}
