public class ImmortalUser {
    String uid;
    String realm;
    int power;
    public ImmortalUser(String uid, String realm, int power) {
        this.uid = uid;
        this.realm = realm;
        this.power = power;
    }
}
public void 修仙系统(Object Yu) {
    new Thread(new Runnable() {
        public void run() {
            Object data = getData();
            data.put(Yu, "" + Module);
            String quntext = data.quntext;
            String qun = data.qun;
            String uin = data.uin;
            String qq = myUin;
            if (quntext.equals("开启修仙系统")) {
                if (qq.equals(uin) || 读(qun, "代管", uin) == 1 || 读("0", "代管", uin) == 1) {
                    写(qun, "修仙系统", "开关", 1);
                    sendText(data, "本聊天已开启修仙系统");
                }
            }
            if (quntext.equals("关闭修仙系统")) {
                if (qq.equals(uin) || 读(qun, "代管", uin) == 1 || 读("0", "代管", uin) == 1) {
                    写(qun, "修仙系统", "开关", 0);
                    sendText(data, "本聊天已关闭修仙系统");
                }
            }
            
            if(读(qun,"修仙系统","开关") == 1) {
                if (quntext.equals("修仙系统") || quntext.equals("修仙菜单")) {
                    if(读(qun,"修仙系统","开关") == 1) {
                        String menu = "修仙系统:\n" +
                        "【基础命令】\n" +
                        "修仙入门 - 开始修仙之路\n" +
                        "我的修仙 - 查看修仙信息\n" +
                        "修炼 - 每日修炼\n" +
                        "突破 - 突破境界\n" +
                        "【比试系统】\n" +
                        "切磋@QQ - 友好比试\n" +
                        "挑战@QQ - 生死决斗\n" +
                        "【宗门系统】\n" +
                        "创建宗门 - 消耗10万金币\n" +
                        "加入宗门 - 加入现有宗门\n" +
                        "我的宗门 - 查看宗门信息\n" +
                        "【功能系统】\n" +
                        "修仙排行 - 查看排行榜\n" +
                        "机缘 - 随机机缘事件\n" +
                        "炼丹 - 炼制丹药\n" +
                   "秘境 - 探索秘境";
                        sendText(data, menu);
                   }
                }
                if (quntext.equals("修仙入门")) {
                    if (文字(qun, "修仙信息", uin).equals("")) {
                        String[] linggenTypes = {
                            "金灵根", "木灵根", "水灵根", "火灵根", "土灵根"
                        };
                        String linggen = linggenTypes[随机数(0, linggenTypes.length - 1)];
                        int linggenPurity = 随机数(50, 80);
                        String xiuXianInfo = "境界:炼气期1层|修为:0/100|灵根:" + linggen + "|纯度:" + linggenPurity + "|战力:100|宗门:无";
                        写(qun, "修仙信息", uin, xiuXianInfo);
                        sendText(data, "恭喜道友踏上修仙之路！\n" +
                        "灵根资质：" + linggen + "（纯度" + linggenPurity + "%）\n" +
                        "初始境界：炼气期1层\n" +
                        "请发送『修炼』开始修行");
                    }
                    else {
                        sendText(data, "您已经踏入仙途，无需重复入门");
                    }
                }
                if (quntext.equals("我的修仙")) {
                    String info = 文字(qun, "修仙信息", uin);
                    if (info.equals("")) {
                        sendText(data, "您还未开始修仙，请发送『修仙入门』");
                        return;
                    }
                    String[] parts = info.split("\\|");
                    String name = getUserName(uin);
                    StringBuilder result = new StringBuilder();
                    result.append("道号：").append(name).append("\n");
                    for (String part : parts) {
                        result.append(part).append("\n");
                    }
                    String lastPractice = 文字(qun, "今日修炼", uin);
                    String today = 年月日();
                    if (today.equals(lastPractice)) {
                        result.append("\n❌ 今日已修炼");
                    }
                    else {
                        result.append("\n✅ 可修炼");
                    }
                    String canBreakthrough = checkBreakthrough(data, qun, uin);
                    result.append("\n").append(canBreakthrough);
                    sendText(data, result.toString());
                }
                if (quntext.equals("修炼")) {
                    String info = 文字(qun, "修仙信息", uin);
                    if (info.equals("")) {
                        sendText(data, "您还未开始修仙，请发送『修仙入门』");
                        return;
                    }
                    String lastPractice = 文字(qun, "今日修炼", uin);
                    String today = 年月日();
                    if (today.equals(lastPractice)) {
                        sendText(data, "今日已修炼，请明日再来");
                        return;
                    }
                    String[] parts = info.split("\\|");
                    String realm = parts[0].split(":")[1];
                    String cultivationStr = parts[1].split(":")[1];
                    String linggenInfo = parts[2];
                    int linggenPurity = Integer.parseInt(parts[3].split(":")[1]);
                    String[] cultivationParts = cultivationStr.split("/");
                    int currentCultivation = Integer.parseInt(cultivationParts[0]);
                    int maxCultivation = Integer.parseInt(cultivationParts[1]);
                    int baseGain = 10;
                    int purityBonus = linggenPurity / 10;
                    int randomBonus = 随机数(0, 10);
                    int totalGain = baseGain + purityBonus + randomBonus;
                    currentCultivation += totalGain;
                    if (currentCultivation >= maxCultivation) {
                        currentCultivation = maxCultivation;
                    }
                    String newCultivation = currentCultivation + "/" + maxCultivation;
                    parts[1] = "修为:" + newCultivation;
                    int power = 100 + currentCultivation / 10;
                    parts[4] = "战力:" + power;
                    StringBuilder newInfo = new StringBuilder();
                    for (int i = 0;
                    i < parts.length;
                    i++) {
                        newInfo.append(parts[i]);
                        if (i < parts.length - 1) newInfo.append("|");
                    }
                    写(qun, "修仙信息", uin, newInfo.toString());
                    写(qun, "今日修炼", uin, today);
                    sendText(data, "修炼成功！\n" +
                    "获得修为：" + totalGain + "点\n" +
                    "当前修为：" + currentCultivation + "/" + maxCultivation + "\n" +
                    "灵根加成：" + purityBonus + "点\n" +
                    "今日修炼已完成");
                }
                if (quntext.equals("突破")) {
                    String info = 文字(qun, "修仙信息", uin);
                    if (info.equals("")) {
                        sendText(data, "您还未开始修仙，请发送『修仙入门』");
                        return;
                    }
                    String[] parts = info.split("\\|");
                    String realmFull = parts[0].split(":")[1];
                    String cultivationStr = parts[1].split(":")[1];
                    String linggenInfo = parts[2];
                    int linggenPurity = Integer.parseInt(parts[3].split(":")[1]);
                    String[] cultivationParts = cultivationStr.split("/");
                    int currentCultivation = Integer.parseInt(cultivationParts[0]);
                    int maxCultivation = Integer.parseInt(cultivationParts[1]);
                    if (currentCultivation < maxCultivation) {
                        sendText(data, "修为不足，无法突破\n需要修为：" + maxCultivation + "\n当前修为：" + currentCultivation);
                        return;
                    }
                    String[] realmParts = realmFull.split("期");
                    String realmName = realmParts[0];
                    int realmLevel = Integer.parseInt(realmParts[1].replace("层", ""));
                    int baseCost = 1000;
                    int levelMultiplier = realmLevel;
                    if (realmName.equals("筑基")) levelMultiplier += 10;
                    else if (realmName.equals("金丹")) levelMultiplier += 20;
                    else if (realmName.equals("元婴")) levelMultiplier += 30;
                    else if (realmName.equals("化神")) levelMultiplier += 40;
                    else if (realmName.equals("炼虚")) levelMultiplier += 50;
                    else if (realmName.equals("合体")) levelMultiplier += 60;
                    else if (realmName.equals("大乘")) levelMultiplier += 70;
                    else if (realmName.equals("渡劫")) levelMultiplier += 80;
                    int goldNeeded = baseCost * levelMultiplier;
                    long userGold = 读(qun, "金币" + uin, "数量");
                    if (userGold < goldNeeded) {
                        sendText(data, "金币不足，无法突破\n需要金币：" + goldNeeded + "\n当前金币：" + userGold);
                        return;
                    }
                    int successRate = 70;
                    successRate += (linggenPurity - 50) / 5;
                    if (realmLevel == 10) {
                        successRate -= 30;
                    }
                    boolean success = 随机数(1, 100) <= successRate;
                    if (success) {
                        写(qun, "金币" + uin, "数量", userGold - goldNeeded);
                        int newLevel = realmLevel + 1;
                        String newRealmFull = realmName + "期" + newLevel + "层";
                        if (realmLevel == 10) {
                            String[] realmOrder = {
                                "炼气", "筑基", "金丹", "元婴", "化神", "炼虚", "合体", "大乘", "渡劫"
                            };
                            for (int i = 0;
                            i < realmOrder.length - 1;
                            i++) {
                                if (realmOrder[i].equals(realmName)) {
                                    realmName = realmOrder[i + 1];
                                    newRealmFull = realmName + "期1层";
                                    break;
                                }
                            }
                        }
                        int newMaxCultivation = 100 + (getRealmIndex(realmName) * 100) + (newLevel * 50);
                        parts[0] = "境界:" + newRealmFull;
                        parts[1] = "修为:0/" + newMaxCultivation;
                        int newPower = 100 + getRealmIndex(realmName) * 500 + newLevel * 50;
                        parts[4] = "战力:" + newPower;
                        StringBuilder newInfo = new StringBuilder();
                        for (int i = 0;
                        i < parts.length;
                        i++) {
                            newInfo.append(parts[i]);
                            if (i < parts.length - 1) newInfo.append("|");
                        }
                        写(qun, "修仙信息", uin, newInfo.toString());
                        String title = getRealmTitle(realmName);
                        sendText(data, "突破成功！🎉\n" +
                        "新境界：" + newRealmFull + "\n" +
                        "称号：" + title + "\n" +
                        "消耗金币：" + goldNeeded + "\n" +
                        "当前金币：" + (userGold - goldNeeded) + "\n" +
                        "修为已重置，请继续修炼");
                    }
                    else {
                        int lostGold = goldNeeded / 2;
                        int lostCultivation = maxCultivation / 2;
                        if (lostGold > 0) {
                            写(qun, "金币" + uin, "数量", userGold - lostGold);
                        }
                        currentCultivation = Math.max(0, currentCultivation - lostCultivation);
                        parts[1] = "修为:" + currentCultivation + "/" + maxCultivation;
                        StringBuilder newInfo = new StringBuilder();
                        for (int i = 0;
                        i < parts.length;
                        i++) {
                            newInfo.append(parts[i]);
                            if (i < parts.length - 1) newInfo.append("|");
                        }
                        写(qun, "修仙信息", uin, newInfo.toString());
                        sendText(data, "突破失败！💥\n" +
                        "心魔入侵，突破受阻\n" +
                        "损失金币：" + lostGold + "\n" +
                        "损失修为：" + lostCultivation + "\n" +
                        "当前修为：" + currentCultivation + "/" + maxCultivation);
                    }
                }
                if (quntext.startsWith("切磋@")) {
                    if (data.atList.size() >= 1) {
                        String at = data.atList.get(0);
                        duelBattle(data, qun, uin, at, false);
                    }
                }
                if (quntext.startsWith("挑战@")) {
                    if (data.atList.size() >= 1) {
                        String at = data.atList.get(0);
                        duelBattle(data, qun, uin, at, true);
                    }
                }
                if (quntext.equals("创建宗门")) {
                    String info = 文字(qun, "修仙信息", uin);
                    if (info.equals("")) {
                        sendText(data, "您还未开始修仙，请发送『修仙入门』");
                        return;
                    }
                    String[] parts = info.split("\\|");
                    String currentClan = parts[5].split(":")[1];
                    if (!currentClan.equals("无")) {
                        sendText(data, "您已加入宗门：" + currentClan + "，请先退出宗门");
                        return;
                    }
                    long userGold = 读(qun, "金币" + uin, "数量");
                    int createCost = 100000;
                    if (userGold < createCost) {
                        sendText(data, "金币不足，创建宗门需要10万金币\n当前金币：" + userGold);
                        return;
                    }
                    sendText(data, "请发送『宗门名称 XXX』来创建宗门\n例如：宗门名称 天火宗");
                }
                else if (quntext.startsWith("宗门名称 ")) {
                    String clanName = quntext.substring(5);
                    if (clanName.length() < 2 || clanName.length() > 6) {
                        sendText(data, "宗门名称需为2-6个字");
                        return;
                    }
                    long userGold = 读(qun, "金币" + uin, "数量");
                    int createCost = 100000;
                    if (userGold < createCost) {
                        sendText(data, "金币不足");
                        return;
                    }
                    String existingClan = 文字(qun, "宗门列表", clanName);
                    if (!existingClan.equals("")) {
                        sendText(data, "宗门名称已存在，请换一个");
                        return;
                    }
                    写(qun, "金币" + uin, "数量", userGold - createCost);
                    写(qun, "宗门列表", clanName, uin + "|宗主|" + 年月日());
                    写(qun, "宗门成员", clanName + uin, "宗主");
                    String info = 文字(qun, "修仙信息", uin);
                    String[] parts = info.split("\\|");
                    parts[5] = "宗门:" + clanName;
                    StringBuilder newInfo = new StringBuilder();
                    for (int i = 0;
                    i < parts.length;
                    i++) {
                        newInfo.append(parts[i]);
                        if (i < parts.length - 1) newInfo.append("|");
                    }
                    写(qun, "修仙信息", uin, newInfo.toString());
                    sendText(data, "宗门创建成功！\n" +
                    "宗门名称：" + clanName + "\n" +
                    "宗主：" + getUserName(uin) + "\n" +
                    "消耗金币：" + createCost + "\n" +
                    "发送『宗门邀请@QQ』邀请成员加入");
                }
                if (quntext.equals("加入宗门")) {
                    String info = 文字(qun, "修仙信息", uin);
                    if (info.equals("")) {
                        sendText(data, "您还未开始修仙");
                        return;
                    }
                    String[] parts = info.split("\\|");
                    String currentClan = parts[5].split(":")[1];
                    if (!currentClan.equals("无")) {
                        sendText(data, "您已加入宗门：" + currentClan);
                        return;
                    }
                    String[] clans = 列表2(qun, "宗门列表");
                    if (clans == null || clans.length == 0) {
                        sendText(data, "暂无宗门可加入");
                        return;
                    }
                    StringBuilder clanList = new StringBuilder("现有宗门：\n");
                    for (int i = 0;
                    i < Math.min(10, clans.length);
                    i++) {
                        String clanInfo = 文字(qun, "宗门列表", clans[i]);
                        String[] clanParts = clanInfo.split("\\|");
                        String leader = clanParts[0];
                        String leaderName = getUserName(leader);
                        clanList.append(i + 1).append("、").append(clans[i])
                        .append("（宗主：").append(leaderName).append("）\n");
                    }
                    clanList.append("\n请发送『申请加入 宗门名称』申请加入");
                    sendText(data, clanList.toString());
                }
                else if (quntext.startsWith("申请加入 ")) {
                    String clanName = quntext.substring(5);
                    String clanInfo = 文字(qun, "宗门列表", clanName);
                    if (clanInfo.equals("")) {
                        sendText(data, "宗门不存在");
                        return;
                    }
                    String[] clanParts = clanInfo.split("\\|");
                    String leader = clanParts[0];
                    String applyKey = "宗门申请" + clanName + uin;
                    写(qun, "宗门申请", applyKey, uin + "|" + 年月日时分());
                    String userName = getUserName(uin);
                    sendText(data, "已向" + clanName + "发送加入申请\n等待宗主审核");
                    String leaderNotice = userName + "（" + uin + "）申请加入宗门\n" +
                    "同意请发送『同意申请 " + uin + "』\n" +
                    "拒绝请发送『拒绝申请 " + uin + "』";
                    sendText(data, leaderNotice);
                }
                if (quntext.equals("我的宗门")) {
                    String info = 文字(qun, "修仙信息", uin);
                    if (info.equals("")) {
                        sendText(data, "您还未开始修仙");
                        return;
                    }
                    String[] parts = info.split("\\|");
                    String clanName = parts[5].split(":")[1];
                    if (clanName.equals("无")) {
                        sendText(data, "您尚未加入任何宗门");
                        return;
                    }
                    String clanInfo = 文字(qun, "宗门列表", clanName);
                    if (clanInfo.equals("")) {
                        sendText(data, "宗门信息异常");
                        return;
                    }
                    String[] clanParts = clanInfo.split("\\|");
                    String leader = clanParts[0];
                    String position = clanParts[1];
                    String createDate = clanParts[2];
                    String[] members = 列表2(qun, "宗门成员");
                    int memberCount = 0;
                    StringBuilder memberList = new StringBuilder();
                    for (String memberKey : members) {
                        if (memberKey.startsWith(clanName)) {
                            memberCount++;
                            String memberUid = memberKey.replace(clanName, "");
                            String memberPos = 文字(qun, "宗门成员", memberKey);
                            String memberName = getUserName(memberUid);
                            if (memberUid.equals(leader)) {
                                memberList.append("👑 ").append(memberName).append("（宗主）\n");
                            }
                            else if (memberPos.equals("长老")) {
                                memberList.append("⭐ ").append(memberName).append("（长老）\n");
                            }
                            else if (memberPos.equals("护法")) {
                                memberList.append("🔰 ").append(memberName).append("（护法）\n");
                            }
                            else {
                                memberList.append("👤 ").append(memberName).append("（弟子）\n");
                            }
                        }
                    }
                    String leaderName = getUserName(leader);
                    sendText(data, "宗门：" + clanName + "\n" +
                    "宗主：" + leaderName + "\n" +
                    "创建时间：" + createDate + "\n" +
                    "成员数量：" + memberCount + "人\n\n" +
                    "成员列表：\n" + memberList.toString());
                }
                if (quntext.equals("修仙排行")) {
                    String[] users = 列表2(qun, "修仙信息");
                    if (users == null || users.length == 0) {
                        sendText(data, "暂无修仙数据");
                        return;
                    }
                    ImmortalUser[] immortalUsers = new ImmortalUser[users.length];
                    int count = 0;
                    for (String userUid : users) {
                        String info = 文字(qun, "修仙信息", userUid);
                        if (!info.equals("")) {
                            String[] parts = info.split("\\|");
                            String realmFull = parts[0].split(":")[1];
                            String powerStr = parts[4].split(":")[1];
                            int power = Integer.parseInt(powerStr);
                            immortalUsers[count] = new ImmortalUser(userUid, realmFull, power);
                            count++;
                        }
                    }
                    if (count == 0) {
                        sendText(data, "暂无有效数据");
                        return;
                    }
                    for (int i = 0;
                    i < count - 1;
                    i++) {
                        for (int j = 0;
                        j < count - 1 - i;
                        j++) {
                            if (compareImmortalUser(immortalUsers[j], immortalUsers[j + 1]) < 0) {
                                ImmortalUser temp = immortalUsers[j];
                                immortalUsers[j] = immortalUsers[j + 1];
                                immortalUsers[j + 1] = temp;
                            }
                        }
                    }
                    StringBuilder result = new StringBuilder("修仙排行榜🏆\n");
                    int displayCount = Math.min(10, count);
                    for (int i = 0;
                    i < displayCount;
                    i++) {
                        String userName = getUserName(immortalUsers[i].uid);
                        result.append(i + 1).append("、").append(userName)
                        .append(" - ").append(immortalUsers[i].realm)
                        .append("（战力：").append(immortalUsers[i].power).append("）\n");
                    }
                    sendText(data, result.toString());
                }
                if (quntext.equals("机缘")) {
                    String info = 文字(qun, "修仙信息", uin);
                    if (info.equals("")) {
                        sendText(data, "您还未开始修仙");
                        return;
                    }
                    String lastChance = 文字(qun, "今日机缘", uin);
                    String today = 年月日();
                    if (today.equals(lastChance)) {
                        sendText(data, "今日机缘已用尽，请明日再来");
                        return;
                    }
                    int chanceType = 随机数(1, 5);
                    String result = "";
                    int reward = 0;
                    switch (chanceType) {
                        case 1:
                        reward = 随机数(100, 500);
                        写(qun, "金币" + uin, "数量", 读(qun, "金币" + uin, "数量") + reward);
                        result = "探索中发现小型灵石矿\n获得金币：" + reward;
                        break;
                        case 2:
                        String[] parts = info.split("\\|");
                        String cultivationStr = parts[1].split(":")[1];
                        String[] cultivationParts = cultivationStr.split("/");
                        int currentCultivation = Integer.parseInt(cultivationParts[0]);
                        int maxCultivation = Integer.parseInt(cultivationParts[1]);
                        int cultivationGain = 随机数(20, 50);
                        currentCultivation = Math.min(maxCultivation, currentCultivation + cultivationGain);
                        parts[1] = "修为:" + currentCultivation + "/" + maxCultivation;
                        StringBuilder newInfo = new StringBuilder();
                        for (int i = 0;
                        i < parts.length;
                        i++) {
                            newInfo.append(parts[i]);
                            if (i < parts.length - 1) newInfo.append("|");
                        }
                        写(qun, "修仙信息", uin, newInfo.toString());
                        result = "遇到前辈指点\n修为增加：" + cultivationGain + "点";
                        break;
                        case 3:
                        int goldReward = 随机数(200, 800);
                        int itemChance = 随机数(1, 3);
                        写(qun, "金币" + uin, "数量", 读(qun, "金币" + uin, "数量") + goldReward);
                        if (itemChance == 1) {
                            String[] danYao = {
                                "聚气丹", "筑基丹", "疗伤丹", "破境丹"
                            };
                            String item = danYao[随机数(0, danYao.length - 1)];
                            写(qun, "修仙物品", uin + item, "1");
                            result = "探索秘境获得宝物\n金币：" + goldReward + "\n获得：" + item;
                        }
                        else {
                            result = "探索秘境获得宝物\n获得金币：" + goldReward;
                        }
                        break;
                        case 4:
                        int damage = 随机数(10, 30);
                        String[] parts2 = info.split("\\|");
                        String cultivationStr2 = parts2[1].split(":")[1];
                        String[] cultivationParts2 = cultivationStr2.split("/");
                        int currentCultivation2 = Integer.parseInt(cultivationParts2[0]);
                        currentCultivation2 = Math.max(0, currentCultivation2 - damage);
                        parts2[1] = "修为:" + currentCultivation2 + "/" + cultivationParts2[1];
                        StringBuilder newInfo2 = new StringBuilder();
                        for (int i = 0;
                        i < parts2.length;
                        i++) {
                            newInfo2.append(parts2[i]);
                            if (i < parts2.length - 1) newInfo2.append("|");
                        }
                        写(qun, "修仙信息", uin, newInfo2.toString());
                        result = "遭遇妖兽袭击\n损失修为：" + damage + "点";
                        break;
                        case 5:
                        result = "游历四方，平静度过一日\n修为略有精进";
                        break;
                    }
                    写(qun, "今日机缘", uin, today);
                    sendText(data, result);
                }
                if (quntext.equals("炼丹")) {
                    String info = 文字(qun, "修仙信息", uin);
                    if (info.equals("")) {
                        sendText(data, "您还未开始修仙");
                        return;
                    }
                    String[] parts = info.split("\\|");
                    String realmFull = parts[0].split(":")[1];
                    String realmName = realmFull.split("期")[0];
                    if (getRealmIndex(realmName) < 1) {
                        sendText(data, "炼丹需要筑基期以上境界\n当前境界：" + realmFull);
                        return;
                    }
                    String lastAlchemy = 文字(qun, "今日炼丹", uin);
                    String today = 年月日();
                    if (today.equals(lastAlchemy)) {
                        sendText(data, "今日炼丹次数已用完");
                        return;
                    }
                    int cost = 500;
                    long userGold = 读(qun, "金币" + uin, "数量");
                    if (userGold < cost) {
                        sendText(data, "金币不足，炼丹需要500金币");
                        return;
                    }
                    写(qun, "金币" + uin, "数量", userGold - cost);
                    int successChance = 60 + getRealmIndex(realmName) * 5;
                    boolean success = 随机数(1, 100) <= successChance;
                    if (success) {
                        int quality = 随机数(1, 3);
                        String danYao = "";
                        String effect = "";
                        switch (quality) {
                            case 1:
                            danYao = "下品聚气丹";
                            effect = "使用后获得30点修为";
                            break;
                            case 2:
                            danYao = "中品聚气丹";
                            effect = "使用后获得60点修为";
                            break;
                            case 3:
                            danYao = "上品聚气丹";
                            effect = "使用后获得100点修为";
                            break;
                        }
                        String itemKey = uin + danYao;
                        int currentCount = 读(qun, "修仙物品", itemKey);
                        写(qun, "修仙物品", itemKey, currentCount + 1);
                        sendText(data, "炼丹成功！🎉\n" +
                        "获得：" + danYao + "\n" +
                        "品质：" + getQualityText(quality) + "\n" +
                        "效果：" + effect + "\n" +
                        "消耗金币：" + cost);
                    }
                    else {
                        sendText(data, "炼丹失败！💥\n" +
                        "丹炉爆炸，材料尽毁\n" +
                        "消耗金币：" + cost);
                    }
                    写(qun, "今日炼丹", uin, today);
                }
                if (quntext.equals("使用丹药")) {
                    String[] items = 列表2(qun, "修仙物品");
                    StringBuilder itemList = new StringBuilder("拥有的丹药：\n");
                    boolean hasItems = false;
                    for (String itemKey : items) {
                        if (itemKey.startsWith(uin) && itemKey.contains("丹")) {
                            hasItems = true;
                            String itemName = itemKey.replace(uin, "");
                            int count = 读(qun, "修仙物品", itemKey);
                            itemList.append(itemName).append(" x").append(count).append("\n");
                        }
                    }
                    if (!hasItems) {
                        sendText(data, "您没有丹药");
                        return;
                    }
                    itemList.append("\n发送『服用 丹药名称』使用丹药");
                    sendText(data, itemList.toString());
                }
                else if (quntext.startsWith("服用 ")) {
                    String danYao = quntext.substring(3);
                    String itemKey = uin + danYao;
                    long countLong = 读(qun, "修仙物品", itemKey);
                    int count = (int)countLong;
                    if (count <= 0) {
                        sendText(data, "没有该丹药或数量不足");
                        return;
                    }
                    String info = 文字(qun, "修仙信息", uin);
                    String[] parts = info.split("\\|");
                    String cultivationStr = parts[1].split(":")[1];
                    String[] cultivationParts = cultivationStr.split("/");
                    int currentCultivation = Integer.parseInt(cultivationParts[0]);
                    int maxCultivation = Integer.parseInt(cultivationParts[1]);
                    int effect = 0;
                    if (danYao.contains("下品")) effect = 30;
                    else if (danYao.contains("中品")) effect = 60;
                    else if (danYao.contains("上品")) effect = 100;
                    else effect = 20;
                    currentCultivation = Math.min(maxCultivation, currentCultivation + effect);
                    写(qun, "修仙物品", itemKey, count - 1);
                    parts[1] = "修为:" + currentCultivation + "/" + maxCultivation;
                    StringBuilder newInfo = new StringBuilder();
                    for (int i = 0;
                    i < parts.length;
                    i++) {
                        newInfo.append(parts[i]);
                        if (i < parts.length - 1) newInfo.append("|");
                    }
                    写(qun, "修仙信息", uin, newInfo.toString());
                    sendText(data, "服用" + danYao + "成功\n" +
                    "修为增加：" + effect + "点\n" +
                    "当前修为：" + currentCultivation + "/" + maxCultivation);
                }
                if (quntext.equals("秘境")) {
                    String info = 文字(qun, "修仙信息", uin);
                    if (info.equals("")) {
                        sendText(data, "您还未开始修仙");
                        return;
                    }
                    String lastExplore = 文字(qun, "今日秘境", uin);
                    String today = 年月日();
                    if (today.equals(lastExplore)) {
                        sendText(data, "今日秘境探索次数已用完");
                        return;
                    }
                    String[] secretRealms = {
                        "古修士洞府", "妖兽山脉", "灵草园", "上古遗迹", "迷雾森林"
                    };
                    String realm = secretRealms[随机数(0, secretRealms.length - 1)];
                    int resultType = 随机数(1, 4);
                    StringBuilder result = new StringBuilder("探索" + realm + "\n");
                    switch (resultType) {
                        case 1:
                        int goldReward = 随机数(300, 1000);
                        写(qun, "金币" + uin, "数量", 读(qun, "金币" + uin, "数量") + goldReward);
                        result.append("发现隐藏宝物\n获得金币：").append(goldReward);
                        break;
                        case 2:
                        String[] parts = info.split("\\|");
                        String cultivationStr = parts[1].split(":")[1];
                        String[] cultivationParts = cultivationStr.split("/");
                        int currentCultivation = Integer.parseInt(cultivationParts[0]);
                        int maxCultivation = Integer.parseInt(cultivationParts[1]);
                        int cultivationGain = 随机数(50, 150);
                        currentCultivation = Math.min(maxCultivation, currentCultivation + cultivationGain);
                        parts[1] = "修为:" + currentCultivation + "/" + maxCultivation;
                        StringBuilder newInfo = new StringBuilder();
                        for (int i = 0;
                        i < parts.length;
                        i++) {
                            newInfo.append(parts[i]);
                            if (i < parts.length - 1) newInfo.append("|");
                        }
                        写(qun, "修仙信息", uin, newInfo.toString());
                        result.append("感悟天地法则\n修为增加：").append(cultivationGain).append("点");
                        break;
                        case 3:
                        int damage = 随机数(20, 60);
                        String[] parts2 = info.split("\\|");
                        String cultivationStr2 = parts2[1].split(":")[1];
                        String[] cultivationParts2 = cultivationStr2.split("/");
                        int currentCultivation2 = Integer.parseInt(cultivationParts2[0]);
                        currentCultivation2 = Math.max(0, currentCultivation2 - damage);
                        parts2[1] = "修为:" + currentCultivation2 + "/" + cultivationParts2[1];
                        StringBuilder newInfo2 = new StringBuilder();
                        for (int i = 0;
                        i < parts2.length;
                        i++) {
                            newInfo2.append(parts2[i]);
                            if (i < parts2.length - 1) newInfo2.append("|");
                        }
                        写(qun, "修仙信息", uin, newInfo2.toString());
                        result.append("遭遇强大妖兽\n损失修为：").append(damage).append("点");
                        break;
                        case 4:
                        result.append("平安归来，略有收获");
                        break;
                    }
                    写(qun, "今日秘境", uin, today);
                    sendText(data, result.toString());
                }
                if (quntext.equals("重置修仙")) {
                    if (qq.equals(uin) || 读(qun, "代管", uin) == 1 || 读("0", "代管", uin) == 1) {
                        写(qun, "修仙信息", uin, "");
                        sendText(data, "已重置" + getUserName(uin) + "的修仙数据");
                    }
                }
            }
        }
    }
    ).start();
}
public void duelBattle(Object data, String qun, String uin, String at, boolean deadly) {
    String myInfo = 文字(qun, "修仙信息", uin);
    String targetInfo = 文字(qun, "修仙信息", at);
    if (myInfo.equals("")) {
        sendText(data, "您还未开始修仙");
        return;
    }
    if (targetInfo.equals("")) {
        sendText(data, "对方还未开始修仙");
        return;
    }
    if (uin.equals(at)) {
        sendText(data, "不能和自己比试");
        return;
    }
    String[] myParts = myInfo.split("\\|");
    String[] targetParts = targetInfo.split("\\|");
    String myRealmFull = myParts[0].split(":")[1];
    String targetRealmFull = targetParts[0].split(":")[1];
    String myLinggen = myParts[2].split(":")[1];
    String targetLinggen = targetParts[2].split(":")[1];
    int myPower = Integer.parseInt(myParts[4].split(":")[1]);
    int targetPower = Integer.parseInt(targetParts[4].split(":")[1]);
    int myLinggenPurity = Integer.parseInt(myParts[3].split(":")[1]);
    int targetLinggenPurity = Integer.parseInt(targetParts[3].split(":")[1]);
    if (!deadly) {
        String myRealm = myRealmFull.split("期")[0];
        String targetRealm = targetRealmFull.split("期")[0];
        int myRealmIndex = getRealmIndex(myRealm);
        int targetRealmIndex = getRealmIndex(targetRealm);
        if (Math.abs(myRealmIndex - targetRealmIndex) > 1) {
            sendText(data, "切磋只能和相差1个大境界以内的道友进行");
            return;
        }
    }
    int baseWinRate = 50;
    int powerDiff = myPower - targetPower;
    baseWinRate += powerDiff / 20;
    int linggenBonus = calculateLinggenBonus(myLinggen, targetLinggen);
    baseWinRate += linggenBonus;
    int purityBonus = (myLinggenPurity - targetLinggenPurity) / 10;
    baseWinRate += purityBonus;
    baseWinRate += 随机数(-15, 15);
    if (baseWinRate < 10) baseWinRate = 10;
    if (baseWinRate > 90) baseWinRate = 90;
    boolean win = 随机数(1, 100) <= baseWinRate;
    String myName = getUserName(uin);
    String targetName = getUserName(at);
    StringBuilder result = new StringBuilder();
    if (deadly) {
        result.append("⚔️ 生死决斗 ⚔️\n");
    }
    else {
        result.append("🤝 切磋比试 🤝\n");
    }
    result.append(myName).append("（").append(myRealmFull).append("）\n");
    result.append("VS\n");
    result.append(targetName).append("（").append(targetRealmFull).append("）\n\n");
    result.append("战力对比：").append(myPower).append(" vs ").append(targetPower).append("\n");
    result.append("灵根克制：");
    if (linggenBonus > 0) result.append("有利");
    else if (linggenBonus < 0) result.append("不利");
    else result.append("平衡");
    result.append("（").append(linggenBonus).append("%）\n\n");
    if (win) {
        result.append("🏆 胜者：").append(myName).append("\n");
        if (deadly) {
            int goldReward = 随机数(100, 500);
            long targetGold = 读(qun, "金币" + at, "数量");
            int actualReward = Math.min(goldReward, (int)targetGold / 2);
            if (actualReward > 0) {
                写(qun, "金币" + uin, "数量", 读(qun, "金币" + uin, "数量") + actualReward);
                写(qun, "金币" + at, "数量", targetGold - actualReward);
                result.append("掠夺金币：").append(actualReward).append("\n");
            }
            String[] cultivationParts = targetParts[1].split(":")[1].split("/");
            int targetCultivation = Integer.parseInt(cultivationParts[0]);
            int damage = 随机数(20, 80);
            targetCultivation = Math.max(0, targetCultivation - damage);
            targetParts[1] = "修为:" + targetCultivation + "/" + cultivationParts[1];
            StringBuilder newTargetInfo = new StringBuilder();
            for (int i = 0;
            i < targetParts.length;
            i++) {
                newTargetInfo.append(targetParts[i]);
                if (i < targetParts.length - 1) newTargetInfo.append("|");
            }
            写(qun, "修仙信息", at, newTargetInfo.toString());
            result.append("对方修为受损：").append(damage).append("点");
        }
        else {
            int goldReward = 随机数(50, 200);
            写(qun, "金币" + uin, "数量", 读(qun, "金币" + uin, "数量") + goldReward);
            result.append("获得金币：").append(goldReward);
        }
    }
    else {
        result.append("🏆 胜者：").append(targetName).append("\n");
        if (deadly) {
            int goldLoss = 随机数(100, 300);
            long myGold = 读(qun, "金币" + uin, "数量");
            int actualLoss = Math.min(goldLoss, (int)myGold / 2);
            if (actualLoss > 0) {
                写(qun, "金币" + uin, "数量", myGold - actualLoss);
                写(qun, "金币" + at, "数量", 读(qun, "金币" + at, "数量") + actualLoss);
                result.append("损失金币：").append(actualLoss).append("\n");
            }
            String[] cultivationParts = myParts[1].split(":")[1].split("/");
            int myCultivation = Integer.parseInt(cultivationParts[0]);
            int damage = 随机数(30, 100);
            myCultivation = Math.max(0, myCultivation - damage);
            myParts[1] = "修为:" + myCultivation + "/" + cultivationParts[1];
            StringBuilder newMyInfo = new StringBuilder();
            for (int i = 0;
            i < myParts.length;
            i++) {
                newMyInfo.append(myParts[i]);
                if (i < myParts.length - 1) newMyInfo.append("|");
            }
            写(qun, "修仙信息", uin, newMyInfo.toString());
            result.append("修为受损：").append(damage).append("点");
        }
        else {
            result.append("胜败乃兵家常事");
        }
    }
    sendText(data, result.toString());
}
public String checkBreakthrough(Object data, String qun, String uin) {
    String info = 文字(qun, "修仙信息", uin);
    if (info.equals("")) return "";
    String[] parts = info.split("\\|");
    String cultivationStr = parts[1].split(":")[1];
    String[] cultivationParts = cultivationStr.split("/");
    int currentCultivation = Integer.parseInt(cultivationParts[0]);
    int maxCultivation = Integer.parseInt(cultivationParts[1]);
    if (currentCultivation >= maxCultivation) {
        String realmFull = parts[0].split(":")[1];
        String[] realmParts = realmFull.split("期");
        String realmName = realmParts[0];
        int realmLevel = Integer.parseInt(realmParts[1].replace("层", ""));
        int baseCost = 1000;
        int levelMultiplier = realmLevel;
        if (realmName.equals("筑基")) levelMultiplier += 10;
        else if (realmName.equals("金丹")) levelMultiplier += 20;
        else if (realmName.equals("元婴")) levelMultiplier += 30;
        else if (realmName.equals("化神")) levelMultiplier += 40;
        else if (realmName.equals("炼虚")) levelMultiplier += 50;
        else if (realmName.equals("合体")) levelMultiplier += 60;
        else if (realmName.equals("大乘")) levelMultiplier += 70;
        else if (realmName.equals("渡劫")) levelMultiplier += 80;
        int goldNeeded = baseCost * levelMultiplier;
        long userGold = 读(qun, "金币" + uin, "数量");
        return "✅ 可突破（需要金币：" + goldNeeded + "，当前金币：" + userGold + "）";
    }
    else {
        return "❌ 修为不足（" + currentCultivation + "/" + maxCultivation + "）";
    }
}
public int calculateLinggenBonus(String myLinggen, String targetLinggen) {
    if (myLinggen.contains("金") && targetLinggen.contains("木")) return 10;
    if (myLinggen.contains("木") && targetLinggen.contains("土")) return 10;
    if (myLinggen.contains("土") && targetLinggen.contains("水")) return 10;
    if (myLinggen.contains("水") && targetLinggen.contains("火")) return 10;
    if (myLinggen.contains("火") && targetLinggen.contains("金")) return 10;
    if (myLinggen.contains("木") && targetLinggen.contains("金")) return -10;
    if (myLinggen.contains("土") && targetLinggen.contains("木")) return -10;
    if (myLinggen.contains("水") && targetLinggen.contains("土")) return -10;
    if (myLinggen.contains("火") && targetLinggen.contains("水")) return -10;
    if (myLinggen.contains("金") && targetLinggen.contains("火")) return -10;
    return 0;
}
public int getRealmIndex(String realmName) {
    switch (realmName) {
        case "炼气": return 0;
        case "筑基": return 1;
        case "金丹": return 2;
        case "元婴": return 3;
        case "化神": return 4;
        case "炼虚": return 5;
        case "合体": return 6;
        case "大乘": return 7;
        case "渡劫": return 8;
        default: return 0;
    }
}
public String getRealmTitle(String realmName) {
    switch (realmName) {
        case "炼气": return "炼气修士";
        case "筑基": return "筑基真人";
        case "金丹": return "金丹老祖";
        case "元婴": return "元婴老怪";
        case "化神": return "化神尊者";
        case "炼虚": return "炼虚上人";
        case "合体": return "合体大能";
        case "大乘": return "大乘圣者";
        case "渡劫": return "渡劫真仙";
        default: return "凡人";
    }
}
public String getQualityText(int quality) {
    switch (quality) {
        case 1: return "下品";
        case 2: return "中品";
        case 3: return "上品";
        default: return "普通";
    }
}
public int compareImmortalUser(ImmortalUser a, ImmortalUser b) {
    String aRealm = a.realm.split("期")[0];
    String bRealm = b.realm.split("期")[0];
    int aRealmIndex = getRealmIndex(aRealm);
    int bRealmIndex = getRealmIndex(bRealm);
    if (aRealmIndex != bRealmIndex) {
        return bRealmIndex - aRealmIndex;
    }
    int aLevel = Integer.parseInt(a.realm.split("期")[1].replace("层", ""));
    int bLevel = Integer.parseInt(b.realm.split("期")[1].replace("层", ""));
    if (aLevel != bLevel) {
        return bLevel - aLevel;
    }
    return b.power - a.power;
}