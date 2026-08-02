// 炸弹用户类
public class BombUser {
    String uid;
    int bombs;
    int level;
    public BombUser(String uid, int bombs, int level) {
        this.uid = uid;
        this.bombs = bombs;
        this.level = level;
    }
}
public void 炸弹系统(Object Yu) {
    new Thread(new Runnable() {
        public void run() {
            Object data = getData();
            data.put(Yu, "" + Module);
            String quntext = data.quntext;
            String qun = data.qun;
            String uin = data.uin;
            String qq = myUin;
            // 开启/关闭炸弹系统
            if (quntext.equals("开启炸弹系统")) {
                if (qq.equals(uin) || 读(qun, "代管", uin) == 1 || 读("0", "代管", uin) == 1) {
                    写(qun, "炸弹系统", "开关", 1);
                    sendText(data, "群" + qun + "\n已开启炸弹系统");
                }
            }
            if (quntext.equals("关闭炸弹系统")) {
                if (qq.equals(uin) || 读(qun, "代管", uin) == 1 || 读("0", "代管", uin) == 1) {
                    写(qun, "炸弹系统", "开关", 0);
                    sendText(data, "群" + qun + "\n已关闭炸弹系统");
                }
            }
            if (读(qun, "炸弹系统", "开关") == 1) {
            
                if (quntext.equals("炸弹系统")) {
                String menu = "炸弹系统:\n开启/关闭炸弹系统\n抽炸弹\n埋炸弹\n清空炸弹\n扔炸弹@QQ\n购买炸弹+数量(100*1)\n扣除炸弹+QQ/@QQ 数量\n充值金币@QQ/+QQ 数量\n我的炸弹\n升级炸弹(消耗金币)";
                sendText(data, menu);
                }
                // 我的炸弹
                if (quntext.equals("我的炸弹")) {
                    String name = getUserName(uin);
                    int b = 读(qun, uin + "炸弹系统", "数量");
                    int level = 读(qun, uin + "炸弹等级", "等级");
                    if (level == 0) level = 1;
                    sendText(data, "QQ:" + uin + "\n昵称:" + name + "\n当前炸弹:" + b + "\n炸弹等级:" + level + "级");
                }
                // 抽炸弹
                if ("抽炸弹".equals(quntext)) {
                    String time = 文字(qun, uin + "抽炸弹", "时间");
                    int b = 读(qun, uin + "炸弹系统", "数量");
                    String time1 = 年月日();
                    if (!time1.equals(time)) {
                        int a = 随机数(1, 9);
                        // 根据炸弹等级增加抽取数量
                        int level = 读(qun, uin + "炸弹等级", "等级");
                        if (level > 1) {
                            a += (level - 1) * 2;
                            // 每级增加2个
                        }
                        写(qun, uin + "抽炸弹", "时间", time1);
                        写(qun, uin + "炸弹系统", "数量", a + b);
                        sendText(data, "抽到" + a + "个炸弹\n当前炸弹:" + (a + b));
                    }
                    else {
                        sendText(data, "今日已抽过炸弹！\n当前炸弹:" + b + "个");
                    }
                }
                // 埋炸弹
                if (quntext.equals("埋炸弹")) {
                    int a = 读(qun, "埋炸弹", "次数");
                    int b = 读(qun, uin + "炸弹系统", "数量");
                    写(qun, "埋炸弹", "开关", 0);
                    if (a > 1) {
                        sendText(data, "群里已经有人埋下了炸弹啦");
                    }
                    else {
                        if (b >= 1) {
                            int level = 读(qun, uin + "炸弹等级", "等级");
                            if (level == 0) level = 1;
                            int bombCount = 随机数(1, 15 + level * 5);
                            // 等级越高，持续时间越长
                            sendText(data, "偷偷埋下了一个" + level + "级炸弹\n嘿嘿 谁能踩到呢");
                            写(qun, uin + "炸弹系统", "数量", b - 1);
                            写(qun, "埋炸弹", "次数", bombCount);
                            写(qun, "埋炸弹", "开关", 1);
                            写(qun, "埋炸弹", "埋弹者", uin);
                        }
                        else {
                            sendText(data, "不是你有炸弹吗你就埋");
                        }
                    }
                }
                // 踩炸弹检测
                if (quntext.length() > 0 && 读(qun, "炸弹系统", "开关") == 1) {
                    int a = 读(qun, "埋炸弹", "次数");
                    if (读(qun, "埋炸弹", "开关") == 1) {
                        if (qq.equals(uin) || 读(qun, "代管", uin) == 1 || 读("0", "代管", uin) == 1) return;
                        String bomber = 文字(qun, "埋炸弹", "埋弹者");
                        int level = 读(qun, bomber + "炸弹等级", "等级");
                        if (level == 0) level = 1;
                        if (a == 1) {
                            int c = 随机数(1, 5 + level * 2);
                            // 等级越高，禁言时间越长
                            shutUp(qun, uin, c * 60);
                            sendText(data, "踩中了" + bomber + "埋下的" + level + "级炸弹\n晕倒了" + c + "分钟 嘿嘿");
                            写(qun, "埋炸弹", "开关", 0);
                            // 埋弹者获得奖励
                            int reward = 随机数(1, 3);
                            int bomberBombs = 读(qun, bomber + "炸弹系统", "数量");
                            //写(qun, bomber + "炸弹系统", "数量", bomberBombs + reward);
                            //sendText(data, "埋弹者" + bomber + "获得" + reward + "个炸弹奖励");
                        }
                        else if (a > 1) {
                            int b = a - 1;
                            if(b == 10 || b == 5) {
                                写(qun, "埋炸弹", "次数", b);
                                sendText(data, "离爆炸还有" + b + "步，小心！");
                            }
                        }
                    }
                }
                // 清空炸弹
                if (quntext.equals("清空炸弹")) {
                    if (qq.equals(uin) || 读(qun, "代管", uin) == 1 || 读("0", "代管", uin) == 1) {
                        int a = 读(qun, "埋炸弹", "次数");
                        if (a > 1) {
                            sendText(data, "群:" + qun + "\n埋下的炸弹已清空");
                            写(qun, "埋炸弹", "次数", 0);
                            写(qun, "埋炸弹", "开关", 0);
                        }
                        else {
                            sendText(data, "群:" + qun + "\n当前群没有人埋炸弹");
                        }
                    }
                }
                // 扔炸弹@QQ
                if (quntext.startsWith("扔炸弹@")) {
                    if (data.atList.size() >= 1) {
                        String at = data.atList.get(0);
                        throwBomb(data, qun, uin, at);
                    }
                }
                // 购买炸弹
                if (quntext.matches("购买炸弹[0-9]+")) {
                    String name = getUserName(uin);
                    int a = Integer.parseInt(quntext.substring(4));
                    int b = a * 100;
                    int c = 读(qun, "金币" + uin, "数量");
                    if (c >= b) {
                        int d = c - b;
                        int e = 读(qun, uin + "炸弹系统", "数量") + a;
                        写(qun, uin + "炸弹系统", "数量", e);
                        写(qun, "金币" + uin, "数量", d);
                        sendText(data, "购买" + a + "个炸弹成功！\n扣除金币" + b + "个\n当前金币:" + d + "个");
                    }
                    else {
                        sendText(data, "金币不足！\n需要金币:" + b + "个\n当前金币:" + c + "个");
                    }
                }
                // 升级炸弹
                if (quntext.equals("升级炸弹")) {
                    int currentLevel = 读(qun, uin + "炸弹等级", "等级");
                    if (currentLevel == 0) currentLevel = 1;
                    if (currentLevel >= 10) {
                        sendText(data, "已达最大等级(10级)");
                        return;
                    }
                    int cost = currentLevel * 500;
                    int coins = 读(qun, "金币" + uin, "数量");
                    if (coins >= cost) {
                        写(qun, "金币" + uin, "数量", coins - cost);
                        写(qun, uin + "炸弹等级", "等级", currentLevel + 1);
                        sendText(data, "炸弹升级成功！\n当前等级:" + (currentLevel + 1) + "级\n消耗金币:" + cost);
                    }
                    else {
                        sendText(data, "金币不足！\n升级需要:" + cost + "金币\n当前金币:" + coins);
                    }
                }
                // 管理员功能 - 扣除炸弹
                if (quntext.matches("扣除炸弹[0-9]+ [0-9]+")) {
                    if (qq.equals(uin) || 读(qun, "代管", uin) == 1 || 读("0", "代管", uin) == 1) {
                        String text = quntext.replace("扣除炸弹", "");
                        String[] parts = text.split(" ");
                        String at = parts[0];
                        int t = Integer.parseInt(parts[1]);
                        int e = 读(qun, at + "炸弹系统", "数量");
                        if (e < t) {
                            sendText(data, at + "没那么多炸弹\n当前炸弹:" + e + "个");
                        }
                        else {
                            e = e - t;
                            写(qun, at + "炸弹系统", "数量", e);
                            sendText(data, at + "的炸弹被扣掉了" + t + "个\n当前炸弹:" + e + "个");
                        }
                    }
                }
                if (quntext.startsWith("扣除炸弹@")) {
                    if (qq.equals(uin) || 读(qun, "代管", uin) == 1 || 读("0", "代管", uin) == 1) {
                        if (data.atList.size() >= 1) {
                            String at = data.atList.get(0);
                            int str=quntext.lastIndexOf(" ")+1;
                            int t=Integer.parseInt(quntext.substring(str));
                            int e = 读(qun, at + "炸弹系统", "数量");
                            if (e < t) {
                                sendText(data, at + "没那么多炸弹\n当前炸弹:" + e + "个");
                            }
                            else {
                                e = e - t;
                                写(qun, at + "炸弹系统", "数量", e);
                                sendText(data, at + "的炸弹被扣掉了" + t + "个\n当前炸弹:" + e + "个");
                            }
                        }
                    }
                }
                // 管理员功能 - 充值金币
                if (quntext.startsWith("充值金币@")) {
                    if (qq.equals(uin) || 读(qun, "代管", uin) == 1 || 读("0", "代管", uin) == 1) {
                        if (data.atList.size() >= 1) {
                            String at = data.atList.get(0);
                            int str=quntext.lastIndexOf(" ")+1;
                            int t=Integer.parseInt(quntext.substring(str));
                            int c = 读(qun, "金币" + at, "数量");
                            int d = c + t;
                            写(qun, "金币" + at, "数量", d);
                            String name = getUserName(at);
                            String myname = getUserName(uin);
                            sendText(data, myname + "为" + name + "成功充值" + t + "个金币\n当前金币:" + d + "个");
                        }
                    }
                }
                // 新增功能：炸弹交易
                if (quntext.startsWith("交易炸弹@")) {
                    try {
                        if (data.atList.size() < 1) {
                            sendText(data, "请@交易对象");
                            return;
                        }
                        String at = data.atList.get(0);
                        if (at.equals(uin)) {
                            sendText(data, "不能和自己交易");
                            return;
                        }
                        String[] parts = quntext.split(" ");
                        if (parts.length < 2) {
                            sendText(data, "格式：交易炸弹@QQ 数量");
                            return;
                        }
                        int tradeAmount = Integer.parseInt(parts[1]);
                        if (tradeAmount <= 0) {
                            sendText(data, "交易数量必须大于0");
                            return;
                        }
                        int myBombs = 读(qun, uin + "炸弹系统", "数量");
                        if (myBombs < tradeAmount) {
                            sendText(data, "炸弹不足，当前拥有:" + myBombs + "个");
                            return;
                        }
                        // 交易确认
                        String tradeKey = "炸弹交易确认" + System.currentTimeMillis();
                        写(qun, "临时交易", tradeKey, uin + "," + at + "," + tradeAmount);
                        String myName = getUserName(uin);
                        String targetName = getUserName(at);
                        sendText(data, myName + "向" + targetName + "发起交易\n交易数量:" + tradeAmount + "个炸弹\n\n" + targetName + "请发送『确认交易 " + tradeKey + "』来确认交易");
                    }
                    catch (Exception e) {
                        sendText(data, "格式错误，正确格式：交易炸弹@QQ 数量");
                    }
                }
                // 确认交易
                if (quntext.startsWith("确认交易 ")) {
                    try {
                        String tradeKey = quntext.substring(5);
                        String tradeInfo = 文字(qun, "临时交易", tradeKey);
                        if (tradeInfo.equals("")) {
                            sendText(data, "交易不存在或已过期");
                            return;
                        }
                        String[] info = tradeInfo.split(",");
                        if (info.length < 3) {
                            sendText(data, "交易信息错误");
                            return;
                        }
                        String fromUid = info[0];
                        String toUid = info[1];
                        int tradeAmount = Integer.parseInt(info[2]);
                        if (!toUid.equals(uin)) {
                            sendText(data, "这不是给你的交易");
                            return;
                        }
                        int fromBombs = 读(qun, fromUid + "炸弹系统", "数量");
                        if (fromBombs < tradeAmount) {
                            sendText(data, "对方炸弹已不足，交易取消");
                            写(qun, "临时交易", tradeKey, "");
                            return;
                        }
                        int toBombs = 读(qun, toUid + "炸弹系统", "数量");
                        // 执行交易
                        写(qun, fromUid + "炸弹系统", "数量", fromBombs - tradeAmount);
                        写(qun, toUid + "炸弹系统", "数量", toBombs + tradeAmount);
                        String fromName = getUserName(fromUid);
                        String toName = getUserName(toUid);
                        sendText(data, "交易成功！\n" + fromName + "→" + toName + "\n交易数量:" + tradeAmount + "个炸弹");
                        // 清理交易记录
                        写(qun, "临时交易", tradeKey, "");
                    }
                    catch (Exception e) {
                        sendText(data, "交易确认失败");
                    }
                }
            }
        }
    }
    ).start();
}
// 扔炸弹处理函数
public void throwBomb(Object data, String qun, String uin, String at) {
    String name = getUserName(at);
    int b = 读(qun, uin + "炸弹系统", "数量");
    int level = 读(qun, uin + "炸弹等级", "等级");
    if (level == 0) level = 1;
    if (at.equals(uin)) {
        sendText(data, name + "，不是哥们，你自己扔自己吗？\n你的炸弹我都懒得扣，扔别人去吧！\n当前炸弹:" + b + "个");
        return;
    }
    if (at.equals("2854196310")) {
        sendText(data, "不是哥们，你连Q群管家都不放过吗？\n饶了它吧！\n当前炸弹:" + b + "个");
        return;
    }
    String myname = getUserName(uin);
    int d = 随机数(0, 225);
    if (b >= 1) {
        // 等级影响成功率
        int successRate = 100 + (level - 1) * 10;
        if (d < successRate) {
            int c = 随机数(1, 5 + level);
            shutUp(qun, at, c * 60);
            写(qun, uin + "炸弹系统", "数量", b - 1);
            sendText(data, name + "被" + myname + "的" + level + "级炸弹砸中！\n治疗" + c + "分钟\n当前炸弹:" + (b - 1) + "个");
        }
        else if (d < 150) {
            int c = 随机数(1, 3);
            shutUp(qun, uin, c * 60);
            写(qun, uin + "炸弹系统", "数量", b - 1);
            sendText(data, myname + "不幸被自己扔的炸弹砸中！🤡\n治疗" + c + "分钟\n当前炸弹:" + (b - 1) + "个");
        }
        else if (d < 200) {
            写(qun, uin + "炸弹系统", "数量", b - 1);
            sendText(data, myname + "，你的炸弹没扔中对方！\n当前炸弹:" + (b - 1) + "个");
        }
        else if (d <= 225) {
            int c = 随机数(1, 3);
            写(qun, uin + "炸弹系统", "数量", b - 1);
            sendText(data, myname + "，你的炸弹扔中了全体成员，全禁" + c + "分钟！\n当前炸弹:" + (b - 1) + "个");
            shutUpAll(qun, true);
            // 使用传统方式创建线程
            new Thread(new Runnable() {
                public void run() {
                    try {
                        Thread.sleep(c * 60 * 1000);
                        shutUpAll(qun, false);
                        sendText(data, "全禁时间到！");
                    }
                    catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            ).start();
        }
    }
    else {
        sendText(data, myname + "，你有炸弹吗你就扔！");
    }
}
// 炸弹用户比较函数
public int compareBombUser(BombUser a, BombUser b) {
    if (a.level != b.level) {
        return b.level - a.level;
    }
    return b.bombs - a.bombs;
}