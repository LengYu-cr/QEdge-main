package me.lengyu.qedge.coldrain.features;

import me.lengyu.qedge.common.ModuleScope;
import me.lengyu.qedge.coldrain.ColdRainCore;
import me.lengyu.qedge.coldrain.ColdRainFeature;
import me.lengyu.qedge.plugin.bean.MsgData;
import me.lengyu.qedge.utils.HttpUtils;
import me.lengyu.qedge.utils.qq.CookieTool;
import me.lengyu.qedge.utils.qq.TroopTool;

import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.concurrent.ConcurrentHashMap;

public class AutoAdminFeature implements ColdRainFeature {

    private static final ConcurrentHashMap<String, PendingPayment> pendingPayments = new ConcurrentHashMap<>();

    private static class PendingPayment {
        final String collectionNo;
        final String groupUin;
        final String userUin;
        volatile boolean cancelled = false;

        PendingPayment(String collectionNo, String groupUin, String userUin) {
            this.collectionNo = collectionNo;
            this.groupUin = groupUin;
            this.userUin = userUin;
        }
    }

    private static String pendingKey(String groupUin, String userUin) {
        return groupUin + "_" + userUin;
    }

    @Override
    public boolean shouldHandle(MsgData msgData) {
        if (msgData.type != 2) return false;
        String text = msgData.msg.trim();
        return text.equals("自助上管") ||
            text.equals("我要管理") ||
            text.equals("取消上管") ||
            text.equals("取消支付") ||
            text.matches("设置上管金额[0-9]+");
    }

    @Override
    public void handle(MsgData msgData, ColdRainCore core) {
        String text = msgData.msg.trim();
        String groupUin = msgData.peerUin;
        String userUin = msgData.userUin;

        if (!core.isAdminOrSelf(msgData) && !text.equals("我要管理") && !text.equals("取消支付") && !text.equals("取消上管")) {
            return;
        }

        if (text.equals("自助上管")) {
            boolean enabled = core.isGroupFeatureEnabled("feature_autoadmin", groupUin);
            int money = core.getConfigInt("autoadmin_money_" + groupUin, 0);
            StringBuilder sb = new StringBuilder();
            sb.append("自助上管:\n");
            sb.append("开启/关闭自助上管\n");
            sb.append("我要管理\n");
            sb.append("取消上管/取消支付\n");
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

        if (text.equals("取消上管") || text.equals("取消支付")) {
            handleCancelAdmin(msgData, core, groupUin, userUin);
            return;
        }

        if (text.equals("我要管理")) {
            handleAutoAdmin(msgData, core, groupUin, userUin);
            return;
        }
    }

    private void handleCancelAdmin(MsgData msgData, ColdRainCore core, String groupUin, String userUin) {
        // 先查发起人自己的待支付账单
        String key = pendingKey(groupUin, userUin);
        PendingPayment pending = pendingPayments.get(key);

        if (pending == null && core.isAdminOrSelf(msgData)) {
            // 管理员可取消本群任意待支付账单
            for (PendingPayment p : pendingPayments.values()) {
                if (p.groupUin.equals(groupUin)) {
                    pending = p;
                    break;
                }
            }
        }

        if (pending == null) {
            core.reply(msgData, "没有进行中的上管");
            return;
        }

        // 权限校验：仅账单发起人、主人或机器人自身可取消
        if (!pending.userUin.equals(userUin) && !core.isAdminOrSelf(msgData)) {
            // core.reply(msgData, "无权取消他人的上管");
            return;
        }

        try {
            String skey = CookieTool.getSkey();
            String tenpayPskey = CookieTool.getPskey("tenpay.com");
            String myUin = core.getMyUin();
            if (skey == null || skey.isEmpty() || tenpayPskey == null || tenpayPskey.isEmpty()) {
                core.reply(msgData, "获取凭证失败，无法取消");
                return;
            }

            String closeUrl = "https://mqq.tenpay.com/cgi-bin/qcollect/qpay_collect_close.cgi" +
                "?collection_no=" + URLEncoder.encode(pending.collectionNo, "UTF-8") +
                "&uin=" + myUin +
                "&pskey=" + tenpayPskey +
                "&skey=" + skey +
                "&skey_type=2";

            String closeResult = HttpUtils.get(closeUrl);
            pending.cancelled = true;
            pendingPayments.remove(pendingKey(pending.groupUin, pending.userUin));

            if (closeResult != null && !closeResult.isEmpty()) {
                JSONObject json = new JSONObject(closeResult);
                String retmsg = json.optString("retmsg", "");
                if (retmsg.equals("ok")) {
                    core.reply(msgData, "已取消上管");
                } else {
                    core.reply(msgData, "取消上管失败: " + retmsg);
                }
            } else {
                core.reply(msgData, "取消上管失败：网络错误");
            }
        } catch (Throwable e) {
            pending.cancelled = true;
            pendingPayments.remove(pendingKey(pending.groupUin, pending.userUin));
            core.reply(msgData, "取消上管异常: " + e.getMessage());
        }
    }

    private void handleAutoAdmin(MsgData msgData, ColdRainCore core, String groupUin, String userUin) {
        if (!core.isGroupFeatureEnabled("feature_autoadmin", groupUin)) {
            core.reply(msgData, "本群未开启自助上管");
            return;
        }

        String myUin = core.getMyUin();
        if (userUin.equals(myUin)) {
            core.reply(msgData, "不是，你自己问自己要管理员？？？");
            return;
        }

        // 检查是否已有进行中的上管
        String key = pendingKey(groupUin, userUin);
        if (pendingPayments.containsKey(key)) {
            core.reply(msgData, "你已有进行中的上管，请先取消");
            return;
        }

        int money = core.getConfigInt("autoadmin_money_" + groupUin, 0);
        if (money == 0) money = 10;

        final int finalMoney = money;
        final String finalMyUin = myUin;

        // 支付流程在新线程中执行，轮询最多60秒，避免阻塞消息处理
        ModuleScope.launchIOJava("AutoAdmin", () -> {
            try {
                String skey = CookieTool.getSkey();
                String tenpayPskey = CookieTool.getPskey("tenpay.com");
                if (skey == null || skey.isEmpty() || tenpayPskey == null || tenpayPskey.isEmpty()) {
                    core.reply(msgData, "获取凭证失败，请稍后再试");
                    return;
                }

                // 发起群收款
                String title = "来自:我要管理，请支付";
                String payerList = "[{\"uin\":" + userUin + ",\"amount\":" + finalMoney + "}]";
                String createUrl = "https://mqq.tenpay.com/cgi-bin/qcollect/qpay_collect_create.cgi" +
                    "?type=1" +
                    "&memo=" + URLEncoder.encode(title, "UTF-8") +
                    "&amount=" + finalMoney +
                    "&payer_list=" + URLEncoder.encode(payerList, "UTF-8") +
                    "&num=1" +
                    "&recv_type=1" +
                    "&group_id=" + groupUin +
                    "&uin=" + finalMyUin +
                    "&pskey=" + tenpayPskey +
                    "&skey=" + skey;

                String createResult = HttpUtils.get(createUrl);
                if (createResult == null || createResult.isEmpty()) {
                    core.reply(msgData, "发起群收款失败：网络错误");
                    return;
                }

                JSONObject json1 = new JSONObject(createResult);
                String retmsg = json1.optString("retmsg", "");
                if (!retmsg.equals("ok")) {
                    core.reply(msgData, "发起群收款失败\n" + retmsg);
                    return;
                }

                String collectionNo = json1.optString("collection_no", "");
                if (collectionNo.isEmpty()) {
                    core.reply(msgData, "发起群收款失败：未获取到收款单号");
                    return;
                }

                // 注册待支付账单，供取消上管使用
                PendingPayment pending = new PendingPayment(collectionNo, groupUin, userUin);
                pendingPayments.put(key, pending);

                double yuanAmount = finalMoney / 100.0;
                core.reply(msgData, "[atUin=" + userUin + "]\n请支付" + yuanAmount + "元，即可获得管理员\n发送\"取消上管\"可取消");

                // 轮询检查支付状态，最多60秒
                boolean paid = false;
                boolean expired = false;
                for (int i = 0; i < 60; i++) {
                    if (pending.cancelled) break;
                    try {
                        String detailUrl = "https://mqq.tenpay.com/cgi-bin/qcollect/qpay_collect_detail.cgi" +
                            "?collection_no=" + URLEncoder.encode(collectionNo, "UTF-8") +
                            "&uin=" + finalMyUin +
                            "&pskey=" + tenpayPskey +
                            "&skey=" + skey +
                            "&skey_type=2";

                        String detailResult = HttpUtils.get(detailUrl);
                        if (detailResult != null && !detailResult.isEmpty()) {
                            JSONObject json2 = new JSONObject(detailResult);

                            // 顶层 state: 3=过期, 4=已取消
                            String topState = json2.optString("state", "");
                            if (topState.equals("3") || topState.equals("4")) {
                                expired = true;
                                break;
                            }

                            // payer_list state: 2=已支付
                            String payerListStr = json2.optString("payer_list", "");
                            if (payerListStr.startsWith("[")) {
                                payerListStr = payerListStr.substring(1);
                            }
                            if (payerListStr.endsWith("]")) {
                                payerListStr = payerListStr.substring(0, payerListStr.length() - 1);
                            }
                            JSONObject json3 = new JSONObject(payerListStr);
                            String payerState = json3.optString("state", "");
                            if (payerState.equals("2")) {
                                TroopTool.INSTANCE.setGroupAdmin(groupUin, userUin, true);
                                core.reply(msgData, "付款成功，已处理...");
                                paid = true;
                                break;
                            }
                        }
                    } catch (Throwable ignored) {}
                    Thread.sleep(1000);
                }

                pendingPayments.remove(key);

                if (!paid && !pending.cancelled) {
                    if (expired) {
                        core.reply(msgData, "[atUin=" + userUin + "]\n账单已过期，上管失败");
                    } else {
                        core.reply(msgData, "[atUin=" + userUin + "]\n上管失败，超60秒未支付");
                    }
                }
            } catch (Throwable e) {
                pendingPayments.remove(key);
                core.reply(msgData, "自助上管异常: " + e.getMessage());
            }
        });
    }
}
