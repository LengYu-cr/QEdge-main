// 银行系统
public void 银行系统(Object Yu) {
    new Thread(new Runnable() {
        public void run() {
            Object data = getData();
            data.put(Yu, "" + Module);
            String quntext = data.quntext;
            String qun = data.qun;
            String uin = data.uin;
            String qq = myUin;
            // 开启/关闭银行系统
            if (quntext.equals("开启银行系统")) {
                if (qq.equals(uin) || 读(qun, "代管", uin) == 1 || 读("0", "代管", uin) == 1) {
                    写(qun, "银行系统", "开关", 1);
                    sendText(data, "群" + qun + "\n已开启银行系统");
                }
            }
            if (quntext.equals("关闭银行系统")) {
                if (qq.equals(uin) || 读(qun, "代管", uin) == 1 || 读("0", "代管", uin) == 1) {
                    写(qun, "银行系统", "开关", 0);
                    sendText(data, "群" + qun + "\n已关闭银行系统");
                }
            }
            if (读(qun, "银行系统", "开关") == 1) {
                // 银行系统菜单
                if (quntext.equals("银行系统")) {
                    String lengyu = 读(qun, "银行系统", "开关") == 1 ? "开" : "关";
                    String menu = "银行系统:\n开启/关闭银行系统\n存款/取款+金额\n打劫+QQ/@QQ\n抢银行\n我的银行\n查询余额\n存款排行榜\n转账@QQ 金额\n贷款+金额\n还款+金额\n查看贷款\n\n银行系统(" + lengyu + ")";
                    sendText(data, menu);
                }
                // 存款功能
                if (quntext.matches("存款[0-9]+")) {
                    long amount = Long.parseLong(quntext.substring(2));
                    if (amount <= 0) {
                        sendText(data, "存款金额必须大于0");
                        return;
                    }
                    long cash = 读(qun, "金币" + uin, "数量");
                    if (cash < amount) {
                        sendText(data, "金币不足\n当前金币:" + cash);
                        return;
                    }
                    long bankBalance = 读(qun, "银行余额", uin);
                    long totalDeposit = 读(qun, "累计存款", uin);
                    // 更新数据
                    写(qun, "金币" + uin, "数量", cash - amount);
                    写(qun, "银行余额", uin, bankBalance + amount);
                    写(qun, "累计存款", uin, totalDeposit + amount);
                    sendText(data, "存款成功\n存入金额:" + amount + "\n当前金币:" + (cash - amount) + "\n银行余额:" + (bankBalance + amount));
                }
                // 取款功能
                if (quntext.matches("取款[0-9]+")) {
                    long amount = Long.parseLong(quntext.substring(2));
                    if (amount <= 0) {
                        sendText(data, "取款金额必须大于0");
                        return;
                    }
                    long bankBalance = 读(qun, "银行余额", uin);
                    if (bankBalance < amount) {
                        sendText(data, "余额不足\n当前余额:" + bankBalance);
                        return;
                    }
                    long cash = 读(qun, "金币" + uin, "数量");
                    // 更新数据
                    写(qun, "金币" + uin, "数量", cash + amount);
                    写(qun, "银行余额", uin, bankBalance - amount);
                    sendText(data, "取款成功\n取出金额:" + amount + "\n当前金币:" + (cash + amount) + "\n银行余额:" + (bankBalance - amount));
                }
                // 查询余额
                if (quntext.equals("查询余额") || quntext.equals("我的银行")) {
                    String name = getUserName(uin);
                    long cash = 读(qun, "金币" + uin, "数量");
                    long bankBalance = 读(qun, "银行余额", uin);
                    long totalDeposit = 读(qun, "累计存款", uin);
                    long loan = 读(qun, "贷款金额", uin);
                    String info = "QQ:" + uin + "\n昵称:" + name +
                    "\n当前金币:" + cash +
                    "\n银行余额:" + bankBalance +
                    "\n累计存款:" + totalDeposit;
                    if (loan > 0) {
                        info += "\n贷款金额:" + loan;
                    }
                    sendText(data, info);
                }
                // 存款排行榜
                if (quntext.equals("存款排行榜")) {
                    String[] users = 列表2(qun, "累计存款");
                    if (users == null || users.length == 0) {
                        sendText(data, "暂无存款记录");
                        return;
                    }
                    BankUser[] bankUsers = new BankUser[users.length];
                    for (int i = 0;
                    i < users.length;
                    i++) {
                        long deposit = 读(qun, "累计存款", users[i]);
                        bankUsers[i] = new BankUser(users[i], deposit);
                    }
                    // 手动排序（冒泡排序）
                    for (int i = 0;
                    i < bankUsers.length - 1;
                    i++) {
                        for (int j = 0;
                        j < bankUsers.length - 1 - i;
                        j++) {
                            if (bankUsers[j].deposit < bankUsers[j + 1].deposit) {
                                BankUser temp = bankUsers[j];
                                bankUsers[j] = bankUsers[j + 1];
                                bankUsers[j + 1] = temp;
                            }
                        }
                    }
                    StringBuilder result = new StringBuilder("累计存款排行榜:\n");
                    int count = Math.min(10, bankUsers.length);
                    for (int i = 0;
                    i < count;
                    i++) {
                        String name = getUserName(bankUsers[i].uid);
                        result.append(i + 1).append("、").append(name).append("(").append(bankUsers[i].uid).append("): ").append(bankUsers[i].deposit).append("金币\n");
                    }
                    sendText(data, result.toString());
                }
                // 转账功能
                if (quntext.startsWith("转账@")) {
                    try {
                        if (data.atList.size() < 1) {
                            sendText(data, "请@转账对象");
                            return;
                        }
                        String at = data.atList.get(0);
                        String[] parts = quntext.split(" ");
                        if (parts.length < 2) {
                            sendText(data, "格式错误，正确格式：转账@QQ 金额");
                            return;
                        }
                        long amount = Long.parseLong(parts[1]);
                        if (amount <= 0) {
                            sendText(data, "转账金额必须大于0");
                            return;
                        }
                        if (at.equals(uin)) {
                            sendText(data, "不能转账给自己");
                            return;
                        }
                        long myBalance = 读(qun, "银行余额", uin);
                        if (myBalance < amount) {
                            sendText(data, "余额不足\n当前余额:" + myBalance);
                            return;
                        }
                        long targetBalance = 读(qun, "银行余额", at);
                        // 转账
                        写(qun, "银行余额", uin, myBalance - amount);
                        写(qun, "银行余额", at, targetBalance + amount);
                        // 累计存款也相应调整（转出的算作存款减少，转入的算作存款增加）
                        写(qun, "累计存款", at, 读(qun, "累计存款", at) + amount);
                        String myName = getUserName(uin);
                        String targetName = getUserName(at);
                        sendText(data, myName + "转账成功\n转账金额:" + amount + "\n收款方:" + targetName + "\n您的余额:" + (myBalance - amount));
                    }
                    catch (Exception e) {
                        sendText(data, "格式错误，正确格式：转账@QQ 金额");
                    }
                }
                // 打劫功能
                if (quntext.matches("打劫[0-9]+")) {
                    String at = quntext.substring(2);
                    processRobbery(data, qun, uin, at);
                }
                if (quntext.startsWith("打劫@")) {
                    if (data.atList.size() >= 1) {
                        String at = data.atList.get(0);
                        processRobbery(data, qun, uin, at);
                    }
                }
                // 抢银行功能
                if (quntext.equals("抢银行")) {
                    String lastRobberyTime = 文字(qun, "银行系统", uin + "抢劫时间");
                    if (!lastRobberyTime.matches("[0-9]+")) lastRobberyTime = "0";
                    long lastTime = Long.parseLong(lastRobberyTime);
                    long currentTime = System.currentTimeMillis();
                    long timeDiff = currentTime - lastTime;
                    // 1小时冷却时间（3600000毫秒）
                    if (timeDiff >= 3600000) {
                        if (随机数(1, 3) == 2) {
                            long bankBalance = 读(qun, "银行余额", uin);
                            long robberyAmount = 随机数(2500, 5000);
                            // 抢劫成功，钱直接存入银行
                            写(qun, "银行余额", uin, bankBalance + robberyAmount);
                            写(qun, "累计存款", uin, 读(qun, "累计存款", uin) + robberyAmount);
                            写(qun, "银行系统", uin + "抢劫时间", "" + currentTime);
                            sendText(data, "抢劫银行成功，获得金币" + robberyAmount + "个\n已存入银行余额");
                        }
                        else {
                            写(qun, "银行系统", uin + "抢劫时间", "" + currentTime);
                            sendText(data, "抢劫银行失败，被抓，等待1小时");
                        }
                    }
                    else {
                        long waitTime = (3600000 - timeDiff) / 1000;
                        sendText(data, "休息下吧，请等待" + waitTime + "秒后再抢劫");
                    }
                }
                // 贷款功能（新功能）
                if (quntext.matches("贷款[0-9]+")) {
                    long loanAmount = Long.parseLong(quntext.substring(2));
                    if (loanAmount <= 0) {
                        sendText(data, "贷款金额必须大于0");
                        return;
                    }
                    long existingLoan = 读(qun, "贷款金额", uin);
                    if (existingLoan > 0) {
                        sendText(data, "您已有贷款，请先还清贷款");
                        return;
                    }
                    // 检查贷款限额（最多可贷银行余额的2倍）
                    long bankBalance = 读(qun, "银行余额", uin);
                    long maxLoan = bankBalance * 2;
                    if (loanAmount > maxLoan) {
                        sendText(data, "贷款金额超过限额\n最大可贷:" + maxLoan + "\n当前银行余额:" + bankBalance + "\nTips: 由于征信问题，只能贷款自己银行余额数的两倍");
                        return;
                    }
                    // 发放贷款
                    写(qun, "贷款金额", uin, loanAmount);
                    写(qun, "金币" + uin, "数量", 读(qun, "金币" + uin, "数量") + loanAmount);
                    写(qun, "贷款时间", uin, "" + System.currentTimeMillis());
                    sendText(data, "贷款成功\n贷款金额:" + loanAmount + "\n已发放到金币\n请在3天内还款");
                }
                // 还款功能
                if (quntext.matches("还款[0-9]+")) {
                    long repayAmount = Long.parseLong(quntext.substring(2));
                    long loanAmount = 读(qun, "贷款金额", uin);
                    if (loanAmount <= 0) {
                        sendText(data, "您没有贷款需要还");
                        return;
                    }
                    if (repayAmount <= 0) {
                        sendText(data, "还款金额必须大于0");
                        return;
                    }
                    long cash = 读(qun, "金币" + uin, "数量");
                    if (cash < repayAmount) {
                        sendText(data, "金币不足\n当前金币:" + cash + "\n贷款金额:" + loanAmount);
                        return;
                    }
                    long actualRepay = repayAmount;
                    if (repayAmount > loanAmount) {
                        actualRepay = loanAmount;
                    }
                    // 还款
                    写(qun, "金币" + uin, "数量", cash - actualRepay);
                    写(qun, "贷款金额", uin, loanAmount - actualRepay);
                    String result = "还款成功\n还款金额:" + actualRepay + "\n剩余贷款:" + (loanAmount - actualRepay) + "\n当前金币:" + (cash - actualRepay);
                    // 检查是否逾期
                    String loanTimeStr = 文字(qun, "贷款时间", uin);
                    if (!loanTimeStr.equals("")) {
                        long loanTime = Long.parseLong(loanTimeStr);
                        long currentTime = System.currentTimeMillis();
                        long days = (currentTime - loanTime) / (1000 * 60 * 60 * 24);
                        if (days > 3) {
                            long overdueFee = (long)(actualRepay * 0.1);
                            // 10%滞纳金
                            result += "\n\n⚠️已逾期" + (days - 3) + "天\n滞纳金:" + overdueFee + "金币";
                        }
                    }
                    sendText(data, result);
                }
                // 查看贷款
                if (quntext.equals("查看贷款")) {
                    long loanAmount = 读(qun, "贷款金额", uin);
                    if (loanAmount <= 0) {
                        sendText(data, "您目前没有贷款\nTips: 由于征信问题，只能贷款自己银行余额数的两倍");
                        return;
                    }
                    String loanTimeStr = 文字(qun, "贷款时间", uin);
                    if (loanTimeStr.equals("")) {
                        sendText(data, "贷款金额:" + loanAmount + "\n请尽快还款");
                        return;
                    }
                    long loanTime = Long.parseLong(loanTimeStr);
                    long currentTime = System.currentTimeMillis();
                    long days = (currentTime - loanTime) / (1000 * 60 * 60 * 24);
                    String info = "贷款金额:" + loanAmount + "\n贷款天数:" + days + "天";
                    if (days > 3) {
                        long overdueDays = days - 3;
                        long overdueFee = (long)(loanAmount * 0.1 * overdueDays);
                        // 每天10%滞纳金
                        info += "\n\n⚠️已逾期" + overdueDays + "天\n需缴纳滞纳金:" + overdueFee + "金币";
                    }
                    sendText(data, info);
                }
            }
        }
    }
    ).start();
}
// 银行用户类
class BankUser {
    String uid;
    long deposit;
    public BankUser(String uid, long deposit) {
        this.uid = uid;
        this.deposit = deposit;
    }
}
// 打劫处理函数
private void processRobbery(Object data, String qun, String uin, String at) {
    String lastRobberyTime = 文字(qun, "银行系统", uin + "打劫时间");
    if (!lastRobberyTime.matches("[0-9]+")) lastRobberyTime = "0";
    long lastTime = Long.parseLong(lastRobberyTime);
    long currentTime = System.currentTimeMillis();
    long timeDiff = currentTime - lastTime;
    // 2分钟冷却时间（120000毫秒）
    if (timeDiff >= 120000) {
        long targetBalance = 读(qun, "银行余额", at);
        long robberyAmount = 随机数(1000, 3000);
        if (targetBalance < robberyAmount) {
            sendText(data, "你换个人打劫吧，他穷的苦茶子都不剩了");
            return;
        }
        long myBalance = 读(qun, "银行余额", uin);
        // 打劫成功，钱直接存入银行
        写(qun, "银行余额", uin, myBalance + robberyAmount);
        写(qun, "累计存款", uin, 读(qun, "累计存款", uin) + robberyAmount);
        写(qun, "银行余额", at, targetBalance - robberyAmount);
        写(qun, "银行系统", uin + "打劫时间", "" + currentTime);
        sendText(data, "打劫成功，获得金币" + robberyAmount + "个\n已存入银行余额");
    }
    else {
        long waitTime = (120000 - timeDiff) / 1000;
        sendText(data, "休息下吧，请等待" + waitTime + "秒后再打劫");
    }
}