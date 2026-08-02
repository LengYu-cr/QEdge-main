public void 签到系统(Object Yu) {
    new Thread(new Runnable() {
        public void run() {
            Object data = getData();
            data.put(Yu, "" + Module);
            String quntext = data.quntext;
            String qun = data.qun;
            String uin = data.uin;
            String qq = myUin;
            int mtype = data.mtype;
            long msgid = data.msgid;
            int msgtype = data.msgtype;
            // 开启/关闭签到系统
            if (quntext.equals("开启签到系统")) {
                if (qq.equals(uin) || 读(qun, "代管", uin) == 1 || 读("0", "代管", uin) == 1) {
                    写(qun, "签到系统", "开关", 1);
                    sendText(data, "群" + qun + "\n已开启签到系统");
                }
            }
            if (quntext.equals("关闭签到系统")) {
                if (qq.equals(uin) || 读(qun, "代管", uin) == 1 || 读("0", "代管", uin) == 1) {
                    写(qun, "签到系统", "开关", 0);
                    sendText(data, "群" + qun + "\n已关闭签到系统");
                }
            }
            if (读(qun, "签到系统", "开关") == 1) {
                // 签到系统菜单
                if (quntext.equals("签到系统")) {
                    String menu = "签到系统:\n开启/关闭签到系统\n签到(签到金币默认)\n设置签到金币自定义#min#max\n设置签到金币定值#数字\n设置签到金币随机(默认100-999)\n我的金币\n查询金币@QQ";
                    sendText(data, menu);
                }
                // 签到功能
                if ("签到".equals(quntext)) {
                    String time = 文字(qun, uin + "签到时间", "时间");
                    String time1 = 年月日();
                    String type = 文字(qun, "签到系统", "模式");
                    int num = 读(qun, "签到系统", uin + "签到次数");
                    String name = getUserName(uin);
                    if (!time1.equals(time)) {
                        if (type.equals("")) type = "随机";
                        else if (type.equals("自定义")) type = "自定义";
                        else if (type.equals("定值")) type = "定值";
                        else if (type.equals("随机")) type = "随机";
                        if (type.equals("随机")) {
                            int a = 随机数(100, 999);
                            int b = 读(qun, "金币" + uin, "数量");
                            int c = a + b;
                            写(qun, "金币" + uin, "数量", c);
                            写(qun, uin + "签到时间", "时间", time1);
                            写(qun, "签到系统", uin + "签到次数", num + 1);
                            String menu = "[atUin=" + uin + "]\n[pic=http://q2.qlogo.cn/headimg_dl?dst_uin=" + uin + "&spec=640]QQ:" + uin + "\n签到成功！\n昵称:" + name + "\n获得金币:" + a + "个\n\n当前金币:" + c + "个\n已签到" + (num + 1) + "天";
                            sendText(data, menu);
                        }
                        else if (type.equals("自定义")) {
                            int min = 读(qun, "自定义", "最小值");
                            int max = 读(qun, "自定义", "最大值");
                            int a = 随机数(min, max);
                            int b = 读(qun, "金币" + uin, "数量");
                            int c = a + b;
                            写(qun, "金币" + uin, "数量", c);
                            写(qun, uin + "签到时间", "时间", time1);
                            写(qun, "签到系统", uin + "签到次数", num + 1);
                            String menu = "[atUin=" + uin + "]\n[pic=http://q2.qlogo.cn/headimg_dl?dst_uin=" + uin + "&spec=640]QQ:" + uin + "\n签到成功！\n昵称:" + name + "\n获得金币:" + a + "个\n\n当前金币:" + c + "个\n已签到" + (num + 1) + "天";
                            sendText(data, menu);
                        }
                        else if (type.equals("定值")) {
                            int a = 读(qun, "自定义", "定值");
                            int b = 读(qun, "金币" + uin, "数量");
                            int c = a + b;
                            写(qun, "金币" + uin, "数量", c);
                            写(qun, uin + "签到时间", "时间", time1);
                            写(qun, "签到系统", uin + "签到次数", num + 1);
                            String menu = "[atUin=" + uin + "]\n[pic=http://q2.qlogo.cn/headimg_dl?dst_uin=" + uin + "&spec=640]QQ:" + uin + "\n签到成功！\n昵称:" + name + "\n获得金币:" + a + "个\n\n当前金币:" + c + "个\n已签到" + (num + 1) + "天";
                            sendText(data, menu);
                        }
                    }
                    else {
                        int b = 读(qun, "金币" + uin, "数量");
                        String menu = "[atUin=" + uin + "]\n[pic=http://q2.qlogo.cn/headimg_dl?dst_uin=" + uin + "&spec=640]QQ:" + uin + "\n昵称:" + name + "\n今日已签过到！！！\n\n当前金币:" + b + "个\n已签到" + num + "天";
                        sendText(data, menu);
                    }
                }
                // 我的金币
                if (quntext.equals("我的金币")) {
                    String name = getUserName(uin);
                    int b = 读(qun, "金币" + uin, "数量");
                    int c = 读(qun, "银行余额", uin);
                    sendText(data, "QQ:" + uin + "\n昵称:" + name + "\n当前金币:" + b + "\n银行余额:" + c);
                }
                // 查询他人金币
                if (quntext.startsWith("查询金币@")) {
                    if (data.atList.size() >= 1) {
                        String at = data.atList.get(0);
                        String name = getUserName(at);
                        int b = 读(qun, "金币" + at, "数量");
                        int c = 读(qun, "银行系统", at);
                        sendText(data, "QQ:" + at + "\n昵称:" + name + "\n当前金币:" + b + "\n银行余额:" + c);
                    }
                }
                // 设置签到模式
                if (quntext.startsWith("设置签到金币自定义#")) {
                    if (qq.equals(uin) || 读(qun, "代管", uin) == 1 || 读("0", "代管", uin) == 1) {
                        try {
                            String[] parts = quntext.split("#");
                            int min = Integer.parseInt(parts[1]);
                            int max = Integer.parseInt(parts[2]);
                            写(qun, "自定义", "最大值", max);
                            写(qun, "自定义", "最小值", min);
                            写(qun, "签到系统", "模式", "自定义");
                            sendText(data, "设置成功！\n当前签到金币:" + min + "～" + max);
                        }
                        catch (Exception e) {
                            sendText(data, "格式错误，正确格式：设置签到金币自定义#最小值#最大值");
                        }
                    }
                }
                if (quntext.startsWith("设置签到金币定值#")) {
                    if (qq.equals(uin) || 读(qun, "代管", uin) == 1 || 读("0", "代管", uin) == 1) {
                        try {
                            String[] parts = quntext.split("#");
                            int t = Integer.parseInt(parts[1]);
                            写(qun, "自定义", "定值", t);
                            写(qun, "签到系统", "模式", "定值");
                            sendText(data, "设置成功！\n当前签到金币:" + t);
                        }
                        catch (Exception e) {
                            sendText(data, "格式错误，正确格式：设置签到金币定值#数值");
                        }
                    }
                }
                if (quntext.equals("设置签到金币随机")) {
                    if (qq.equals(uin) || 读(qun, "代管", uin) == 1 || 读("0", "代管", uin) == 1) {
                        写(qun, "签到系统", "模式", "随机");
                        sendText(data, "设置成功！\n当前签到金币:100～999");
                    }
                }
            }
        }
    }
    ).start();
}