public class PetUser {
    public String uid;
    public String petName;
    public String originalName;
    public int level;
    public int power;
    public PetUser(String uid, String petName, String originalName, int level, int power) {
        this.uid = uid;
        this.petName = petName;
        this.originalName = originalName;
        this.level = level;
        this.power = power;
    }
}
public void 宠物系统(Object Yu) {
    new Thread(new Runnable() {
        public void run() {
            Object data = getData();
            data.put(Yu, "" + Module);
            String quntext = data.quntext;
            String qun = data.qun;
            String uin = data.uin;
            String qq = myUin;
            if (quntext.equals("开启宠物系统")) {
                if (qq.equals(uin) || 读(qun, "代管", uin) == 1 || 读("0", "代管", uin) == 1) {
                    写(qun, "宠物系统", "开关", 1);
                    sendText(data, "本聊天已开启宠物系统");
                }
            }
            if (quntext.equals("关闭宠物系统")) {
                if (qq.equals(uin) || 读(qun, "代管", uin) == 1 || 读("0", "代管", uin) == 1) {
                    写(qun, "宠物系统", "开关", 0);
                    sendText(data, "本聊天已关闭宠物系统");
                }
            }

            if(读(qun, "宠物系统", "开关") == 1) {
            
                            if (quntext.equals("宠物系统") || quntext.equals("宠物菜单")) {
                    String menu = "🐾 宠物系统 🐾\n" +
                    "【基础命令】\n" +
                    "领取宠物 - 领取初始宠物\n" +
                    "我的宠物 - 查看宠物信息\n" +
                    "宠物改名 - 给宠物改名\n" +
                    "【养成命令】\n" +
                    "喂养 - 喂养宠物增加经验\n" +
                    "训练 - 训练宠物提升属性\n" +
                    "升级 - 宠物升级\n" +
                    "进化 - 宠物进化\n" +
                    "【互动命令】\n" +
                    "宠物对战@QQ - 与其他宠物对战\n" +
                    "宠物探险 - 每日探险\n" +
                    "宠物打工 - 宠物打工赚金币\n" +
                    "【系统命令】\n" +
                    "宠物排行 - 查看宠物排行榜\n" +
                    "宠物图鉴 - 查看所有宠物\n" +
                    "放生宠物 - 放生当前宠物\n" +
                    "购买宠物 - 购买稀有宠物";
                    sendText(data, menu);
            }
                if (quntext.equals("领取宠物")) {
                    String petInfo = 文字(qun, "宠物信息", uin);
                    if (!petInfo.equals("")) {
                        sendText(data, "您已经拥有宠物了！\n发送『我的宠物』查看");
                        return;
                    }
                    String[] petNames = {
                        "小灵猫", "火云狐", "水灵龟", "金翅鸟", "木灵猴",
                        "雷光鼠", "冰晶兔", "岩石熊", "风影狼", "光翼蝶"
                    };
                    String[] petTypes = {
                        "普通", "火系", "水系", "金系", "木系",
                        "雷系", "冰系", "土系", "风系", "光系"
                    };
                    int index = 随机数(0, petNames.length - 1);
                    String petName = petNames[index];
                    String petType = petTypes[index];
                    int health = 随机数(80, 120);
                    int attack = 随机数(10, 20);
                    int defense = 随机数(5, 15);
                    int speed = 随机数(8, 18);
                    int talent = 随机数(60, 100);
                    String initialPet = petName + "|" + petType + "|1|0/100|" +
                    health + "|" + attack + "|" + defense + "|" +
                    speed + "|" + talent + "|100|" + petName;
                    写(qun, "宠物信息", uin, initialPet);
                    String userName = getUserName(uin);
                    sendText(data, "🎉 恭喜" + userName + "获得初始宠物！\n" +
                    "宠物名称：" + petName + "\n" +
                    "宠物属性：" + petType + "\n" +
                    "资质：" + talent + "\n" +
                    "等级：1级\n" +
                    "发送『我的宠物』查看详细信息");
                }
                if (quntext.equals("我的宠物")) {
                    String petInfo = 文字(qun, "宠物信息", uin);
                    if (petInfo.equals("")) {
                        sendText(data, "您还没有宠物，发送『领取宠物』领取一只");
                        return;
                    }
                    String[] parts = petInfo.split("\\|");
                    String customName = parts[parts.length - 1];
                    String originalName = parts[0];
                    String petType = parts[1];
                    int level = Integer.parseInt(parts[2]);
                    String expStr = parts[3];
                    int health = Integer.parseInt(parts[4]);
                    int attack = Integer.parseInt(parts[5]);
                    int defense = Integer.parseInt(parts[6]);
                    int speed = Integer.parseInt(parts[7]);
                    int talent = Integer.parseInt(parts[8]);
                    int happiness = Integer.parseInt(parts[9]);
                    String[] expParts = expStr.split("/");
                    int currentExp = Integer.parseInt(expParts[0]);
                    int maxExp = Integer.parseInt(expParts[1]);
                    int power = (health / 10) + attack * 2 + defense + speed + (talent / 10);
                    StringBuilder info = new StringBuilder();
                    info.append("🐾 ").append(customName).append("（").append(originalName).append("）\n");
                    info.append("属性：").append(petType).append(" | 等级：").append(level).append("级\n");
                    info.append("经验：").append(currentExp).append("/").append(maxExp).append("\n");
                    info.append("生命：").append(health).append(" | 攻击：").append(attack).append("\n");
                    info.append("防御：").append(defense).append(" | 速度：").append(speed).append("\n");
                    info.append("资质：").append(talent).append("/100\n");
                    info.append("心情：").append(getHappinessEmoji(happiness)).append(" ").append(happiness).append("/100\n");
                    info.append("战力：").append(power).append("\n");
                    if (currentExp >= maxExp) {
                        info.append("✨ 可升级！发送『升级』进行升级\n");
                    }
                    if (level >= 10 && level % 10 == 0) {
                        info.append("🌟 可进化！发送『进化』进行进化\n");
                    }
                    String lastFeed = 文字(qun, "今日喂养", uin);
                    String today = 年月日();
                    if (!today.equals(lastFeed)) {
                        info.append("🍖 可喂养（今日剩余：3次）\n");
                    }
                    else {
                        int feedCount = 读(qun, "今日喂养次数", uin);
                        info.append("🍖 今日已喂养").append(feedCount).append("次/3次\n");
                    }
                    String lastTrain = 文字(qun, "今日训练", uin);
                    if (!today.equals(lastTrain)) {
                        info.append("💪 可训练（今日剩余：2次）\n");
                    }
                    else {
                        int trainCount = 读(qun, "今日训练次数", uin);
                        info.append("💪 今日已训练").append(trainCount).append("次/2次\n");
                    }
                    sendText(data, info.toString());
                }
                if (quntext.startsWith("宠物改名 ")) {
                    String petInfo = 文字(qun, "宠物信息", uin);
                    if (petInfo.equals("")) {
                        sendText(data, "您还没有宠物");
                        return;
                    }
                    String newName = quntext.substring(5);
                    if (newName.length() < 2 || newName.length() > 6) {
                        sendText(data, "宠物名称需为2-6个字");
                        return;
                    }
                    long userGold = 读(qun, "金币" + uin, "数量");
                    int renameCost = 500;
                    if (userGold < renameCost) {
                        sendText(data, "金币不足，改名需要500金币");
                        return;
                    }
                    写(qun, "金币" + uin, "数量", userGold - renameCost);
                    String[] parts = petInfo.split("\\|");
                    parts[parts.length - 1] = newName;
                    StringBuilder newPetInfo = new StringBuilder();
                    for (int i = 0;
                    i < parts.length;
                    i++) {
                        newPetInfo.append(parts[i]);
                        if (i < parts.length - 1) newPetInfo.append("|");
                    }
                    写(qun, "宠物信息", uin, newPetInfo.toString());
                    sendText(data, "宠物改名成功！\n新名称：" + newName + "\n消耗金币：" + renameCost);
                }
                if (quntext.equals("喂养")) {
                    String petInfo = 文字(qun, "宠物信息", uin);
                    if (petInfo.equals("")) {
                        sendText(data, "您还没有宠物");
                        return;
                    }
                    String lastFeedDate = 文字(qun, "今日喂养", uin);
                    String today = 年月日();
                    int feedCount = 0;
                    if (today.equals(lastFeedDate)) {
                        long feedCountLong = 读(qun, "今日喂养次数", uin);
                        feedCount = (int)feedCountLong;
                        if (feedCount < 0) feedCount = 0;
                        if (feedCount >= 3) {
                            sendText(data, "今日喂养次数已达上限（3次），请明天再来");
                            return;
                        }
                    }
                    else {
                        写(qun, "今日喂养", uin, today);
                        写(qun, "今日喂养次数", uin, 0);
                        feedCount = 0;
                    }
                    long userGold = 读(qun, "金币" + uin, "数量");
                    int feedCost = 100;
                    if (userGold < feedCost) {
                        sendText(data, "金币不足，喂养需要100金币");
                        return;
                    }
                    String[] parts = petInfo.split("\\|");
                    String expStr = parts[3];
                    int happiness = Integer.parseInt(parts[9]);
                    String[] expParts = expStr.split("/");
                    int currentExp = Integer.parseInt(expParts[0]);
                    int maxExp = Integer.parseInt(expParts[1]);
                    int expGain = 随机数(10, 30);
                    int happinessGain = 随机数(5, 15);
                    currentExp = Math.min(maxExp, currentExp + expGain);
                    happiness = Math.min(100, happiness + happinessGain);
                    parts[3] = currentExp + "/" + maxExp;
                    parts[9] = String.valueOf(happiness);
                    StringBuilder newPetInfo = new StringBuilder();
                    for (int i = 0;
                    i < parts.length;
                    i++) {
                        newPetInfo.append(parts[i]);
                        if (i < parts.length - 1) newPetInfo.append("|");
                    }
                    写(qun, "宠物信息", uin, newPetInfo.toString());
                    feedCount++;
                    写(qun, "今日喂养次数", uin, feedCount);
                    写(qun, "金币" + uin, "数量", userGold - feedCost);
                    sendText(data, "喂养成功！🍖\n" +
                    "宠物经验 +" + expGain + "\n" +
                    "宠物心情 +" + happinessGain + "\n" +
                    "消耗金币：" + feedCost + "\n" +
                    "今日喂养：" + feedCount + "/3次");
                }
                if (quntext.equals("训练")) {
                    String petInfo = 文字(qun, "宠物信息", uin);
                    if (petInfo.equals("")) {
                        sendText(data, "您还没有宠物");
                        return;
                    }
                    String lastTrainDate = 文字(qun, "今日训练", uin);
                    String today = 年月日();
                    int trainCount = 0;
                    if (today.equals(lastTrainDate)) {
                        long trainCountLong = 读(qun, "今日训练次数", uin);
                        trainCount = (int)trainCountLong;
                        if (trainCount < 0) trainCount = 0;
                        if (trainCount >= 2) {
                            sendText(data, "今日训练次数已达上限（2次），请明天再来");
                            return;
                        }
                    }
                    else {
                        写(qun, "今日训练", uin, today);
                        写(qun, "今日训练次数", uin, 0);
                        trainCount = 0;
                    }
                    long userGold = 读(qun, "金币" + uin, "数量");
                    int trainCost = 300;
                    if (userGold < trainCost) {
                        sendText(data, "金币不足，训练需要300金币");
                        return;
                    }
                    String[] parts = petInfo.split("\\|");
                    int health = Integer.parseInt(parts[4]);
                    int attack = Integer.parseInt(parts[5]);
                    int defense = Integer.parseInt(parts[6]);
                    int speed = Integer.parseInt(parts[7]);
                    int happiness = Integer.parseInt(parts[9]);
                    int trainType = 随机数(1, 4);
                    int attributeGain = 随机数(2, 5);
                    int happinessLoss = 随机数(5, 10);
                    String attributeName = "";
                    switch (trainType) {
                        case 1:
                        health += attributeGain;
                        attributeName = "生命值";
                        break;
                        case 2:
                        attack += attributeGain;
                        attributeName = "攻击力";
                        break;
                        case 3:
                        defense += attributeGain;
                        attributeName = "防御力";
                        break;
                        case 4:
                        speed += attributeGain;
                        attributeName = "速度";
                        break;
                    }
                    happiness = Math.max(0, happiness - happinessLoss);
                    parts[4] = String.valueOf(health);
                    parts[5] = String.valueOf(attack);
                    parts[6] = String.valueOf(defense);
                    parts[7] = String.valueOf(speed);
                    parts[9] = String.valueOf(happiness);
                    StringBuilder newPetInfo = new StringBuilder();
                    for (int i = 0;
                    i < parts.length;
                    i++) {
                        newPetInfo.append(parts[i]);
                        if (i < parts.length - 1) newPetInfo.append("|");
                    }
                    写(qun, "宠物信息", uin, newPetInfo.toString());
                    trainCount++;
                    写(qun, "今日训练次数", uin, trainCount);
                    写(qun, "金币" + uin, "数量", userGold - trainCost);
                    String petName = parts[parts.length - 1];
                    sendText(data, "训练成功！💪\n" +
                    petName + "的" + attributeName + " +" + attributeGain + "\n" +
                    "宠物心情 -" + happinessLoss + "\n" +
                    "消耗金币：" + trainCost + "\n" +
                    "今日训练：" + trainCount + "/2次");
                }
                if (quntext.equals("升级")) {
                    String petInfo = 文字(qun, "宠物信息", uin);
                    if (petInfo.equals("")) {
                        sendText(data, "您还没有宠物");
                        return;
                    }
                    String[] parts = petInfo.split("\\|");
                    int level = Integer.parseInt(parts[2]);
                    String expStr = parts[3];
                    int talent = Integer.parseInt(parts[8]);
                    String[] expParts = expStr.split("/");
                    int currentExp = Integer.parseInt(expParts[0]);
                    int maxExp = Integer.parseInt(expParts[1]);
                    if (currentExp < maxExp) {
                        sendText(data, "经验不足，无法升级\n需要经验：" + maxExp + "\n当前经验：" + currentExp);
                        return;
                    }
                    long userGold = 读(qun, "金币" + uin, "数量");
                    int upgradeCost = level * 200;
                    if (userGold < upgradeCost) {
                        sendText(data, "金币不足，升级需要" + upgradeCost + "金币");
                        return;
                    }
                    level++;
                    currentExp = currentExp - maxExp;
                    maxExp = level * 100;
                    int healthGain = 10 + talent / 10;
                    int attackGain = 2 + talent / 20;
                    int defenseGain = 1 + talent / 25;
                    int speedGain = 1 + talent / 30;
                    int health = Integer.parseInt(parts[4]) + healthGain;
                    int attack = Integer.parseInt(parts[5]) + attackGain;
                    int defense = Integer.parseInt(parts[6]) + defenseGain;
                    int speed = Integer.parseInt(parts[7]) + speedGain;
                    parts[2] = String.valueOf(level);
                    parts[3] = currentExp + "/" + maxExp;
                    parts[4] = String.valueOf(health);
                    parts[5] = String.valueOf(attack);
                    parts[6] = String.valueOf(defense);
                    parts[7] = String.valueOf(speed);
                    StringBuilder newPetInfo = new StringBuilder();
                    for (int i = 0;
                    i < parts.length;
                    i++) {
                        newPetInfo.append(parts[i]);
                        if (i < parts.length - 1) newPetInfo.append("|");
                    }
                    写(qun, "宠物信息", uin, newPetInfo.toString());
                    写(qun, "金币" + uin, "数量", userGold - upgradeCost);
                    String petName = parts[parts.length - 1];
                    sendText(data, "宠物升级成功！✨\n" +
                    petName + " 升到 " + level + "级\n" +
                    "生命 +" + healthGain + "，攻击 +" + attackGain + "\n" +
                    "防御 +" + defenseGain + "，速度 +" + speedGain + "\n" +
                    "消耗金币：" + upgradeCost);
                }
                if (quntext.equals("进化")) {
                    String petInfo = 文字(qun, "宠物信息", uin);
                    if (petInfo.equals("")) {
                        sendText(data, "您还没有宠物");
                        return;
                    }
                    String[] parts = petInfo.split("\\|");
                    int level = Integer.parseInt(parts[2]);
                    String petType = parts[1];
                    String originalName = parts[0];
                    if (level < 10 || level % 10 != 0) {
                        sendText(data, "宠物需要10级、20级、30级...才能进化\n当前等级：" + level);
                        return;
                    }
                    long userGold = 读(qun, "金币" + uin, "数量");
                    int evolutionCost = level * 1000;
                    if (userGold < evolutionCost) {
                        sendText(data, "金币不足，进化需要" + evolutionCost + "金币");
                        return;
                    }
                    int successRate = 70 + level;
                    if (successRate > 95) successRate = 95;
                    boolean success = 随机数(1, 100) <= successRate;
                    if (success) {
                        String evolvedName = getEvolvedName(originalName, petType);
                        if (evolvedName.equals(originalName)) {
                            sendText(data, "您的宠物已是最终形态，无法继续进化");
                            return;
                        }
                        int healthBonus = 随机数(30, 60);
                        int attackBonus = 随机数(5, 12);
                        int defenseBonus = 随机数(3, 8);
                        int speedBonus = 随机数(2, 6);
                        int talentBonus = 随机数(1, 5);
                        int health = Integer.parseInt(parts[4]) + healthBonus;
                        int attack = Integer.parseInt(parts[5]) + attackBonus;
                        int defense = Integer.parseInt(parts[6]) + defenseBonus;
                        int speed = Integer.parseInt(parts[7]) + speedBonus;
                        int talent = Integer.parseInt(parts[8]) + talentBonus;
                        if (talent > 100) talent = 100;
                        parts[0] = evolvedName;
                        parts[4] = String.valueOf(health);
                        parts[5] = String.valueOf(attack);
                        parts[6] = String.valueOf(defense);
                        parts[7] = String.valueOf(speed);
                        parts[8] = String.valueOf(talent);
                        StringBuilder newPetInfo = new StringBuilder();
                        for (int i = 0;
                        i < parts.length;
                        i++) {
                            newPetInfo.append(parts[i]);
                            if (i < parts.length - 1) newPetInfo.append("|");
                        }
                        写(qun, "宠物信息", uin, newPetInfo.toString());
                        写(qun, "金币" + uin, "数量", userGold - evolutionCost);
                        String customName = parts[parts.length - 1];
                        sendText(data, "宠物进化成功！🌟\n" +
                        customName + " 进化为 " + evolvedName + "\n" +
                        "生命 +" + healthBonus + "，攻击 +" + attackBonus + "\n" +
                        "防御 +" + defenseBonus + "，速度 +" + speedBonus + "\n" +
                        "资质 +" + talentBonus + "\n" +
                        "消耗金币：" + evolutionCost);
                    }
                    else {
                        int penalty = evolutionCost / 2;
                        写(qun, "金币" + uin, "数量", userGold - penalty);
                        sendText(data, "进化失败！💥\n" +
                        "进化能量失控，损失" + penalty + "金币\n" +
                        "宠物暂时无法继续进化");
                    }
                }
                if (quntext.startsWith("宠物对战@")) {
                    if (data.atList.size() >= 1) {
                        String at = data.atList.get(0);
                        petBattle(data, qun, uin, at);
                    }
                }
                if (quntext.equals("宠物探险")) {
                    String petInfo = 文字(qun, "宠物信息", uin);
                    if (petInfo.equals("")) {
                        sendText(data, "您还没有宠物");
                        return;
                    }
                    String lastAdventure = 文字(qun, "今日探险", uin);
                    String today = 年月日();
                    if (today.equals(lastAdventure)) {
                        sendText(data, "今日探险次数已用完，请明天再来");
                        return;
                    }
                    String[] parts = petInfo.split("\\|");
                    int level = Integer.parseInt(parts[2]);
                    int happiness = Integer.parseInt(parts[9]);
                    if (happiness < 30) {
                        sendText(data, "宠物心情太差（" + happiness + "/100），无法探险\n请先喂养宠物提升心情");
                        return;
                    }
                    int eventType = 随机数(1, 5);
                    StringBuilder result = new StringBuilder("宠物探险结果：\n");
                    int goldReward = 0;
                    int expReward = 0;
                    int happinessChange = 0;
                    switch (eventType) {
                        case 1:
                        goldReward = 随机数(200, 800);
                        expReward = 随机数(20, 60);
                        happinessChange = 随机数(5, 15);
                        写(qun, "金币" + uin, "数量", 读(qun, "金币" + uin, "数量") + goldReward);
                        String expStr = parts[3];
                        String[] expParts = expStr.split("/");
                        int currentExp = Integer.parseInt(expParts[0]);
                        int maxExp = Integer.parseInt(expParts[1]);
                        currentExp = Math.min(maxExp, currentExp + expReward);
                        parts[3] = currentExp + "/" + maxExp;
                        happiness = Math.min(100, happiness + happinessChange);
                        parts[9] = String.valueOf(happiness);
                        result.append("发现隐藏宝藏！💰\n");
                        result.append("获得金币：").append(goldReward).append("\n");
                        result.append("宠物经验：+").append(expReward).append("\n");
                        result.append("宠物心情：+").append(happinessChange);
                        break;
                        case 2:
                        goldReward = 随机数(100, 400);
                        expReward = 随机数(10, 30);
                        happinessChange = 随机数(0, 10);
                        写(qun, "金币" + uin, "数量", 读(qun, "金币" + uin, "数量") + goldReward);
                        expStr = parts[3];
                        expParts = expStr.split("/");
                        currentExp = Integer.parseInt(expParts[0]);
                        maxExp = Integer.parseInt(expParts[1]);
                        currentExp = Math.min(maxExp, currentExp + expReward);
                        parts[3] = currentExp + "/" + maxExp;
                        happiness = Math.min(100, happiness + happinessChange);
                        parts[9] = String.valueOf(happiness);
                        result.append("平安归来，小有收获\n");
                        result.append("获得金币：").append(goldReward).append("\n");
                        result.append("宠物经验：+").append(expReward).append("\n");
                        if (happinessChange > 0) {
                            result.append("宠物心情：+").append(happinessChange);
                        }
                        break;
                        case 3:
                        int damage = 随机数(10, 40);
                        happinessChange = -随机数(10, 25);
                        int health = Integer.parseInt(parts[4]);
                        health = Math.max(1, health - damage);
                        parts[4] = String.valueOf(health);
                        happiness = Math.max(0, happiness + happinessChange);
                        parts[9] = String.valueOf(happiness);
                        result.append("遭遇野生宠物攻击！\n");
                        result.append("宠物受伤，生命 -").append(damage).append("\n");
                        result.append("宠物心情：").append(happinessChange);
                        break;
                        case 4:
                        goldReward = 随机数(500, 1500);
                        expReward = 随机数(50, 100);
                        int itemChance = 随机数(1, 10);
                        if (itemChance == 1) {
                            String[] items = {
                                "宠物饼干", "成长药剂", "快乐糖果", "进化石"
                            };
                            String item = items[随机数(0, items.length - 1)];
                            写(qun, "宠物物品", uin + item, 读(qun, "宠物物品", uin + item) + 1);
                            result.append("获得特殊物品：").append(item).append("\n");
                        }
                        写(qun, "金币" + uin, "数量", 读(qun, "金币" + uin, "数量") + goldReward);
                        expStr = parts[3];
                        expParts = expStr.split("/");
                        currentExp = Integer.parseInt(expParts[0]);
                        maxExp = Integer.parseInt(expParts[1]);
                        currentExp = Math.min(maxExp, currentExp + expReward);
                        parts[3] = currentExp + "/" + maxExp;
                        result.append("发现神秘奇遇！✨\n");
                        result.append("获得金币：").append(goldReward).append("\n");
                        result.append("宠物经验：+").append(expReward);
                        break;
                        case 5:
                        result.append("平静的一天，平安归来");
                        break;
                    }
                    StringBuilder newPetInfo = new StringBuilder();
                    for (int i = 0;
                    i < parts.length;
                    i++) {
                        newPetInfo.append(parts[i]);
                        if (i < parts.length - 1) newPetInfo.append("|");
                    }
                    写(qun, "宠物信息", uin, newPetInfo.toString());
                    写(qun, "今日探险", uin, today);
                    sendText(data, result.toString());
                }
                if (quntext.equals("宠物打工")) {
                    String petInfo = 文字(qun, "宠物信息", uin);
                    if (petInfo.equals("")) {
                        sendText(data, "您还没有宠物");
                        return;
                    }
                    String lastWork = 文字(qun, "今日打工", uin);
                    String today = 年月日();
                    int workCount = 0;
                    if (today.equals(lastWork)) {
                        long workCountLong = 读(qun, "今日打工次数", uin);
                        workCount = (int)workCountLong;
                        if (workCount < 0) workCount = 0;
                        if (workCount >= 2) {
                            sendText(data, "今日打工次数已达上限（2次），请明天再来");
                            return;
                        }
                    }
                    else {
                        写(qun, "今日打工", uin, today);
                        写(qun, "今日打工次数", uin, 0);
                        workCount = 0;
                    }
                    String[] parts = petInfo.split("\\|");
                    int level = Integer.parseInt(parts[2]);
                    int happiness = Integer.parseInt(parts[9]);
                    if (happiness < 20) {
                        sendText(data, "宠物心情太差（" + happiness + "/100），无法打工\n请先喂养宠物提升心情");
                        return;
                    }
                    int baseReward = 200;
                    int levelBonus = level * 10;
                    int goldReward = baseReward + levelBonus;
                    int happinessLoss = 随机数(10, 25);
                    happiness = Math.max(0, happiness - happinessLoss);
                    parts[9] = String.valueOf(happiness);
                    StringBuilder newPetInfo = new StringBuilder();
                    for (int i = 0;
                    i < parts.length;
                    i++) {
                        newPetInfo.append(parts[i]);
                        if (i < parts.length - 1) newPetInfo.append("|");
                    }
                    写(qun, "宠物信息", uin, newPetInfo.toString());
                    写(qun, "金币" + uin, "数量", 读(qun, "金币" + uin, "数量") + goldReward);
                    workCount++;
                    写(qun, "今日打工次数", uin, workCount);
                    String petName = parts[parts.length - 1];
                    sendText(data, petName + "打工归来！💰\n" +
                    "获得金币：" + goldReward + "\n" +
                    "宠物心情：-" + happinessLoss + "\n" +
                    "今日打工：" + workCount + "/2次");
                }
                if (quntext.equals("宠物排行")) {
                    String[] users = 列表2(qun, "宠物信息");
                    if (users == null || users.length == 0) {
                        sendText(data, "暂无宠物数据");
                        return;
                    }
                    PetUser[] petUsers = new PetUser[users.length];
                    int count = 0;
                    for (String userUid : users) {
                        String petInfo = 文字(qun, "宠物信息", userUid);
                        if (!petInfo.equals("")) {
                            String[] parts = petInfo.split("\\|");
                            int level = Integer.parseInt(parts[2]);
                            int health = Integer.parseInt(parts[4]);
                            int attack = Integer.parseInt(parts[5]);
                            int defense = Integer.parseInt(parts[6]);
                            int speed = Integer.parseInt(parts[7]);
                            int talent = Integer.parseInt(parts[8]);
                            int power = (health / 10) + attack * 2 + defense + speed + (talent / 10);
                            String petName = parts[parts.length - 1];
                            String originalName = parts[0];
                            petUsers[count] = new PetUser(userUid, petName, originalName, level, power);
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
                            if (comparePetUser(petUsers[j], petUsers[j + 1]) < 0) {
                                PetUser temp = petUsers[j];
                                petUsers[j] = petUsers[j + 1];
                                petUsers[j + 1] = temp;
                            }
                        }
                    }
                    StringBuilder result = new StringBuilder("🐾 宠物排行榜 🐾\n");
                    int displayCount = Math.min(10, count);
                    for (int i = 0;
                    i < displayCount;
                    i++) {
                        String userName = getUserName(petUsers[i].uid);
                        result.append(i + 1).append("、").append(userName).append("\n");
                        result.append("   ").append(petUsers[i].petName).append("（").append(petUsers[i].originalName).append("）\n");
                        result.append("   等级：").append(petUsers[i].level).append("级 | 战力：").append(petUsers[i].power).append("\n");
                    }
                    sendText(data, result.toString());
                }
                if (quntext.equals("宠物图鉴")) {
                    String[] allPets = {
                        "小灵猫→灵猫王→九命猫妖",
                        "火云狐→炎狐→九尾天狐",
                        "水灵龟→水龟王→玄武",
                        "金翅鸟→金鹏→大鹏金翅",
                        "木灵猴→灵猴王→齐天大圣",
                        "雷光鼠→雷鼠王→雷神鼠",
                        "冰晶兔→冰兔王→玉兔",
                        "岩石熊→岩熊王→大地熊",
                        "风影狼→风狼王→啸月天狼",
                        "光翼蝶→光蝶王→光明女神蝶"
                    };
                    StringBuilder result = new StringBuilder("📚 宠物图鉴 📚\n");
                    result.append("（宠物进化路线）\n\n");
                    for (int i = 0;
                    i < allPets.length;
                    i++) {
                        String[] stages = allPets[i].split("→");
                        result.append(i + 1).append("、").append(stages[0]);
                        if (stages.length > 1) {
                            result.append(" → ").append(stages[1]);
                        }
                        if (stages.length > 2) {
                            result.append(" → ").append(stages[2]);
                        }
                        result.append("\n");
                    }
                    result.append("\n发送『领取宠物』随机获得一种初始宠物");
                    sendText(data, result.toString());
                }
                if (quntext.equals("放生宠物")) {
                    String petInfo = 文字(qun, "宠物信息", uin);
                    if (petInfo.equals("")) {
                        sendText(data, "您还没有宠物");
                        return;
                    }
                    sendText(data, "⚠️ 确定要放生宠物吗？\n" +
                    "放生后宠物将消失，不可恢复！\n" +
                    "发送『确认放生』确认操作");
                }
                else if (quntext.equals("确认放生")) {
                    String petInfo = 文字(qun, "宠物信息", uin);
                    if (petInfo.equals("")) {
                        sendText(data, "操作已过期");
                        return;
                    }
                    String[] parts = petInfo.split("\\|");
                    int level = Integer.parseInt(parts[2]);
                    String petName = parts[parts.length - 1];
                    int compensation = level * 100;
                    写(qun, "金币" + uin, "数量", 读(qun, "金币" + uin, "数量") + compensation);
                    写(qun, "宠物信息", uin, "");
                    写(qun, "今日喂养", uin, "");
                    写(qun, "今日训练", uin, "");
                    写(qun, "今日探险", uin, "");
                    写(qun, "今日打工", uin, "");
                    sendText(data, "已放生宠物：" + petName + "\n" +
                    "获得补偿金币：" + compensation + "\n" +
                    "您可以重新领取新的宠物");
                }
                if (quntext.equals("购买宠物")) {
                    long userGold = 读(qun, "金币" + uin, "数量");
                    int cost = 10000;
                    if (userGold < cost) {
                        sendText(data, "金币不足，稀有宠物需要" + cost + "金币");
                        return;
                    }
                    String petInfo = 文字(qun, "宠物信息", uin);
                    if (!petInfo.equals("")) {
                        sendText(data, "您已拥有宠物，请先放生现有宠物");
                        return;
                    }
                    String[] rarePets = {
                        "火焰狮", "雷霆虎", "寒冰龙", "大地象", "天空鹰"
                    };
                    String[] rareTypes = {
                        "火系", "雷系", "冰系", "土系", "风系"
                    };
                    int index = 随机数(0, rarePets.length - 1);
                    String petName = rarePets[index];
                    String petType = rareTypes[index];
                    int health = 随机数(150, 200);
                    int attack = 随机数(25, 35);
                    int defense = 随机数(15, 25);
                    int speed = 随机数(20, 30);
                    int talent = 随机数(85, 100);
                    String rarePetInfo = petName + "|" + petType + "|1|0/100|" +
                    health + "|" + attack + "|" + defense + "|" +
                    speed + "|" + talent + "|100|" + petName;
                    写(qun, "宠物信息", uin, rarePetInfo);
                    写(qun, "金币" + uin, "数量", userGold - cost);
                    String userName = getUserName(uin);
                    sendText(data, "🎁 恭喜" + userName + "获得稀有宠物！\n" +
                    "宠物名称：" + petName + "（稀有）\n" +
                    "宠物属性：" + petType + "\n" +
                    "资质：" + talent + "（极品）\n" +
                    "等级：1级\n" +
                    "消耗金币：" + cost);
                }
            }
        }
    }
    ).start();
}
public void petBattle(Object data, String qun, String uin, String at) {
    String myPetInfo = 文字(qun, "宠物信息", uin);
    String targetPetInfo = 文字(qun, "宠物信息", at);
    if (myPetInfo.equals("")) {
        sendText(data, "您还没有宠物");
        return;
    }
    if (targetPetInfo.equals("")) {
        sendText(data, "对方还没有宠物");
        return;
    }
    if (uin.equals(at)) {
        sendText(data, "不能和自己的宠物对战");
        return;
    }
    String[] myParts = myPetInfo.split("\\|");
    String[] targetParts = targetPetInfo.split("\\|");
    String myPetName = myParts[myParts.length - 1];
    String targetPetName = targetParts[targetParts.length - 1];
    String myPetType = myParts[1];
    String targetPetType = targetParts[1];
    int myLevel = Integer.parseInt(myParts[2]);
    int targetLevel = Integer.parseInt(targetParts[2]);
    int myHealth = Integer.parseInt(myParts[4]);
    int myAttack = Integer.parseInt(myParts[5]);
    int myDefense = Integer.parseInt(myParts[6]);
    int mySpeed = Integer.parseInt(myParts[7]);
    int myTalent = Integer.parseInt(myParts[8]);
    int targetHealth = Integer.parseInt(targetParts[4]);
    int targetAttack = Integer.parseInt(targetParts[5]);
    int targetDefense = Integer.parseInt(targetParts[6]);
    int targetSpeed = Integer.parseInt(targetParts[7]);
    int targetTalent = Integer.parseInt(targetParts[8]);
    int myPower = (myHealth / 10) + myAttack * 2 + myDefense + mySpeed + (myTalent / 10);
    int targetPower = (targetHealth / 10) + targetAttack * 2 + targetDefense + targetSpeed + (targetTalent / 10);
    int typeBonus = calculateTypeBonus(myPetType, targetPetType);
    int baseWinRate = 50;
    int powerDiff = myPower - targetPower;
    baseWinRate += powerDiff / 20;
    baseWinRate += typeBonus;
    baseWinRate += (myLevel - targetLevel) * 2;
    baseWinRate += 随机数(-20, 20);
    if (baseWinRate < 10) baseWinRate = 10;
    if (baseWinRate > 90) baseWinRate = 90;
    boolean win = 随机数(1, 100) <= baseWinRate;
    String myName = getUserName(uin);
    String targetName = getUserName(at);
    StringBuilder result = new StringBuilder();
    result.append("⚔️ 宠物对战 ⚔️\n");
    result.append(myName).append("的").append(myPetName).append("（Lv").append(myLevel).append("）\n");
    result.append("VS\n");
    result.append(targetName).append("的").append(targetPetName).append("（Lv").append(targetLevel).append("）\n\n");
    result.append("战力对比：").append(myPower).append(" vs ").append(targetPower).append("\n");
    result.append("属性克制：");
    if (typeBonus > 0) result.append("有利");
    else if (typeBonus < 0) result.append("不利");
    else result.append("平衡");
    result.append("（").append(typeBonus).append("%）\n\n");
    if (win) {
        result.append("🏆 胜利方：").append(myName).append("的").append(myPetName).append("\n");
        int expReward = targetLevel * 5;
        int goldReward = targetLevel * 20;
        String myExpStr = myParts[3];
        String[] myExpParts = myExpStr.split("/");
        int myCurrentExp = Integer.parseInt(myExpParts[0]);
        int myMaxExp = Integer.parseInt(myExpParts[1]);
        myCurrentExp = Math.min(myMaxExp, myCurrentExp + expReward);
        myParts[3] = myCurrentExp + "/" + myMaxExp;
        long currentGold = 读(qun, "金币" + uin, "数量");
        写(qun, "金币" + uin, "数量", currentGold + goldReward);
        StringBuilder newMyPetInfo = new StringBuilder();
        for (int i = 0;
        i < myParts.length;
        i++) {
            newMyPetInfo.append(myParts[i]);
            if (i < myParts.length - 1) newMyPetInfo.append("|");
        }
        写(qun, "宠物信息", uin, newMyPetInfo.toString());
        result.append("获得经验：").append(expReward).append("\n");
        result.append("获得金币：").append(goldReward);
    }
    else {
        result.append("🏆 胜利方：").append(targetName).append("的").append(targetPetName).append("\n");
        int expLoss = myLevel * 2;
        String myExpStr = myParts[3];
        String[] myExpParts = myExpStr.split("/");
        int myCurrentExp = Integer.parseInt(myExpParts[0]);
        myCurrentExp = Math.max(0, myCurrentExp - expLoss);
        myParts[3] = myCurrentExp + "/" + myExpParts[1];
        StringBuilder newMyPetInfo = new StringBuilder();
        for (int i = 0;
        i < myParts.length;
        i++) {
            newMyPetInfo.append(myParts[i]);
            if (i < myParts.length - 1) newMyPetInfo.append("|");
        }
        写(qun, "宠物信息", uin, newMyPetInfo.toString());
        result.append("您的宠物损失经验：").append(expLoss);
    }
    sendText(data, result.toString());
}
public int calculateTypeBonus(String myType, String targetType) {
    if (myType.contains("火") && targetType.contains("金")) return 10;
    if (myType.contains("火") && targetType.contains("木")) return 10;
    if (myType.contains("水") && targetType.contains("火")) return 10;
    if (myType.contains("水") && targetType.contains("土")) return 10;
    if (myType.contains("木") && targetType.contains("土")) return 10;
    if (myType.contains("木") && targetType.contains("水")) return 10;
    if (myType.contains("金") && targetType.contains("木")) return 10;
    if (myType.contains("金") && targetType.contains("土")) return 10;
    if (myType.contains("土") && targetType.contains("水")) return 10;
    if (myType.contains("土") && targetType.contains("火")) return 10;
    if (myType.contains("火") && targetType.contains("水")) return -10;
    if (myType.contains("水") && targetType.contains("木")) return -10;
    if (myType.contains("木") && targetType.contains("金")) return -10;
    if (myType.contains("金") && targetType.contains("火")) return -10;
    if (myType.contains("土") && targetType.contains("木")) return -10;
    if (myType.contains("雷") && targetType.contains("水")) return 15;
    if (myType.contains("雷") && targetType.contains("风")) return 15;
    if (myType.contains("冰") && targetType.contains("火")) return 15;
    if (myType.contains("冰") && targetType.contains("风")) return 15;
    if (myType.contains("风") && targetType.contains("土")) return 15;
    return 0;
}
public String getEvolvedName(String originalName, String petType) {
    if (originalName.equals("小灵猫")) return "灵猫王";
    if (originalName.equals("灵猫王")) return "九命猫妖";
    if (originalName.equals("火云狐")) return "炎狐";
    if (originalName.equals("炎狐")) return "九尾天狐";
    if (originalName.equals("水灵龟")) return "水龟王";
    if (originalName.equals("水龟王")) return "玄武";
    if (originalName.equals("金翅鸟")) return "金鹏";
    if (originalName.equals("金鹏")) return "大鹏金翅";
    if (originalName.equals("木灵猴")) return "灵猴王";
    if (originalName.equals("灵猴王")) return "齐天大圣";
    if (originalName.equals("雷光鼠")) return "雷鼠王";
    if (originalName.equals("雷鼠王")) return "雷神鼠";
    if (originalName.equals("冰晶兔")) return "冰兔王";
    if (originalName.equals("冰兔王")) return "玉兔";
    if (originalName.equals("岩石熊")) return "岩熊王";
    if (originalName.equals("岩熊王")) return "大地熊";
    if (originalName.equals("风影狼")) return "风狼王";
    if (originalName.equals("风狼王")) return "啸月天狼";
    if (originalName.equals("光翼蝶")) return "光蝶王";
    if (originalName.equals("光蝶王")) return "光明女神蝶";
    if (originalName.equals("火焰狮")) return "烈火狮王";
    if (originalName.equals("雷霆虎")) return "雷神虎";
    if (originalName.equals("寒冰龙")) return "冰霜巨龙";
    if (originalName.equals("大地象")) return "泰坦巨象";
    if (originalName.equals("天空鹰")) return "苍穹神鹰";
    if (originalName.equals("烈火狮王")) return "烈焰麒麟";
    if (originalName.equals("雷神虎")) return "雷霆圣虎";
    if (originalName.equals("冰霜巨龙")) return "冰霜龙王";
    if (originalName.equals("泰坦巨象")) return "大地之神";
    if (originalName.equals("苍穹神鹰")) return "天空之神";
    return originalName;
}
public String getHappinessEmoji(int happiness) {
    if (happiness >= 80) return "😄";
    else if (happiness >= 60) return "😊";
    else if (happiness >= 40) return "😐";
    else if (happiness >= 20) return "😔";
    else return "😭";
}
public int comparePetUser(PetUser a, PetUser b) {
    if (a.level != b.level) {
        return b.level - a.level;
    }
    return b.power - a.power;
}