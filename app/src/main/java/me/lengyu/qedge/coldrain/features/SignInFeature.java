package me.lengyu.qedge.coldrain.features;

import me.lengyu.qedge.coldrain.ColdRainCore;
import me.lengyu.qedge.coldrain.ColdRainFeature;
import me.lengyu.qedge.common.ModuleScope;
import me.lengyu.qedge.plugin.bean.MsgData;
import me.lengyu.qedge.utils.LogUtils;

/**
 * @Author 冷雨
 * @Description 签到系统处理
 */
public class SignInFeature implements ColdRainFeature {
    @Override
    public boolean shouldHandle(MsgData msgData) {
        String text = msgData.msg.trim();
        return text.equals("签到") ||
               text.equals("签到系统") ||
               text.equals("开启签到系统") ||
               text.equals("关闭签到系统") ||
               text.equals("我的金币") ||
               text.equals("设置签到金币随机") ||
               text.startsWith("设置签到金币自定义#") ||
               text.startsWith("设置签到金币定值#") ||
               text.startsWith("查询金币@");
    }

    @Override
    public void handle(MsgData msgData, ColdRainCore core) {
        ModuleScope.launchIOJava("SignInFeature", () -> {
                try {
                    String quntext = msgData.msg.trim();
                    String qun = msgData.peerUin;
                    String uin = msgData.userUin;
                    if (uin == null || uin.isEmpty()) uin = msgData.userUid;
                    String qq = core.getMyUin();

                    if (quntext.equals("开启签到系统")) {
                        if (core.isAdminOrSelfForUin(qun, uin)) {
                            core.setGroupFeatureEnabled("feature_signin", qun, true);
                            core.reply(msgData, "群" + qun + "\n已开启签到系统");
                        }
                        return;
                    }

                    if (quntext.equals("关闭签到系统")) {
                        if (core.isAdminOrSelfForUin(qun, uin)) {
                            core.setGroupFeatureEnabled("feature_signin", qun, false);
                            core.reply(msgData, "群" + qun + "\n已关闭签到系统");
                        }
                        return;
                    }

                    if (quntext.equals("签到系统")) {
                        boolean enabled = core.isGroupFeatureEnabled("feature_signin", qun);
                        StringBuilder sb = new StringBuilder();
                        sb.append("签到系统:\n");
                        sb.append("开启/关闭签到系统\n");
                        sb.append("签到(签到金币默认)\n");
                        sb.append("设置签到金币自定义#min#max\n");
                        sb.append("设置签到金币定值#数字\n");
                        sb.append("设置签到金币随机(默认100-999)\n");
                        sb.append("我的金币\n");
                        sb.append("查询金币@QQ\n\n");
                        sb.append("状态(").append(enabled ? "开" : "关").append(")");
                        core.reply(msgData, sb.toString());
                        return;
                    }

                    if (!core.isGroupFeatureEnabled("feature_signin", qun)) {
                        return;
                    }

                    if (quntext.equals("签到")) {
                        String time = core.getConfigString(qun + "_" + uin + "签到时间_时间", "");
                        String time1 = getTodayDate();
                        String type = core.getConfigString(qun + "_签到系统_模式", "");
                        int num = Integer.parseInt(core.getConfigString(qun + "_签到系统_" + uin + "签到次数", "0"));
                        String name = msgData.userName;
                    if (name == null || name.isEmpty()) name = uin;

                        if (!time1.equals(time)) {
                            if (type.isEmpty()) type = "随机";

                            int a = 0;
                            if (type.equals("随机")) {
                                a = (int) (Math.random() * 899) + 100;
                            } else if (type.equals("自定义")) {
                                int min = Integer.parseInt(core.getConfigString(qun + "_自定义_最小值", "100"));
                                int max = Integer.parseInt(core.getConfigString(qun + "_自定义_最大值", "999"));
                                a = (int) (Math.random() * (max - min + 1)) + min;
                            } else if (type.equals("定值")) {
                                a = Integer.parseInt(core.getConfigString(qun + "_自定义_定值", "100"));
                            }

                            int b = Integer.parseInt(core.getConfigString(qun + "_金币" + uin + "_数量", "0"));
                            int c = a + b;

                            core.setConfigString(qun + "_金币" + uin + "_数量", String.valueOf(c));
                            core.setConfigString(qun + "_" + uin + "签到时间_时间", time1);
                            core.setConfigString(qun + "_签到系统_" + uin + "签到次数", String.valueOf(num + 1));

                            String menu = "[at]\n[pic=http://q2.qlogo.cn/headimg_dl?dst_uin=" + uin + "&spec=640]QQ:" + uin + "\n签到成功！\n昵称:" + name + "\n获得金币:" + a + "个\n\n当前金币:" + c + "个\n已签到" + (num + 1) + "天";
                            core.reply(msgData, menu);
                        } else {
                            int b = Integer.parseInt(core.getConfigString(qun + "_金币" + uin + "_数量", "0"));
                            String menu = "[at]\n[pic=http://q2.qlogo.cn/headimg_dl?dst_uin=" + uin + "&spec=640]QQ:" + uin + "\n昵称:" + name + "\n今日已签过到！！！\n\n当前金币:" + b + "个\n已签到" + num + "天";
                            core.reply(msgData, menu);
                        }
                        return;
                    }

                    if (quntext.equals("我的金币")) {
                        String name = msgData.userName;
                    if (name == null || name.isEmpty()) name = uin;
                        int b = Integer.parseInt(core.getConfigString(qun + "_金币" + uin + "_数量", "0"));
                        int c = Integer.parseInt(core.getConfigString(qun + "_银行余额_" + uin, "0"));
                        core.reply(msgData, "QQ:" + uin + "\n昵称:" + name + "\n当前金币:" + b + "\n银行余额:" + c);
                        return;
                    }

                    if (quntext.startsWith("查询金币@")) {
                        if (msgData.atList != null && !msgData.atList.isEmpty()) {
                            String at = msgData.atList.get(0);
                            String name = at;
                            int b = Integer.parseInt(core.getConfigString(qun + "_金币" + at + "_数量", "0"));
                            int c = Integer.parseInt(core.getConfigString(qun + "_银行系统_" + at, "0"));
                            core.reply(msgData, "QQ:" + at + "\n昵称:" + name + "\n当前金币:" + b + "\n银行余额:" + c);
                        }
                        return;
                    }

                    if (quntext.startsWith("设置签到金币自定义#")) {
                        if (core.isAdminOrSelfForUin(qun, uin)) {
                            try {
                                String[] parts = quntext.split("#");
                                int min = Integer.parseInt(parts[1]);
                                int max = Integer.parseInt(parts[2]);
                                core.setConfigString(qun + "_自定义_最大值", String.valueOf(max));
                                core.setConfigString(qun + "_自定义_最小值", String.valueOf(min));
                                core.setConfigString(qun + "_签到系统_模式", "自定义");
                                core.reply(msgData, "设置成功！\n当前签到金币:" + min + "～" + max);
                            } catch (Exception e) {
                                core.reply(msgData, "格式错误，正确格式：设置签到金币自定义#最小值#最大值");
                            }
                        }
                        return;
                    }

                    if (quntext.startsWith("设置签到金币定值#")) {
                        if (core.isAdminOrSelfForUin(qun, uin)) {
                            try {
                                String[] parts = quntext.split("#");
                                int t = Integer.parseInt(parts[1]);
                                core.setConfigString(qun + "_自定义_定值", String.valueOf(t));
                                core.setConfigString(qun + "_签到系统_模式", "定值");
                                core.reply(msgData, "设置成功！\n当前签到金币:" + t);
                            } catch (Exception e) {
                                core.reply(msgData, "格式错误，正确格式：设置签到金币定值#数值");
                            }
                        }
                        return;
                    }

                    if (quntext.equals("设置签到金币随机")) {
                        if (core.isAdminOrSelfForUin(qun, uin)) {
                            core.setConfigString(qun + "_签到系统_模式", "随机");
                            core.reply(msgData, "设置成功！\n当前签到金币:100～999");
                        }
                        return;
                    }
                } catch (Throwable e) {
                    LogUtils.e(e);
                }
        });
    }

    private String getTodayDate() {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        return cal.get(java.util.Calendar.YEAR) + "-" +
               (cal.get(java.util.Calendar.MONTH) + 1) + "-" +
               cal.get(java.util.Calendar.DAY_OF_MONTH);
    }
}
