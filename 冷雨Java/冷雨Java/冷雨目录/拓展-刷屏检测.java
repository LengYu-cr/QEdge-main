public void 刷屏检测(Object Yu) {
    new Thread(new Runnable() {
        public void run() {
            Object data = getData();
            data.put(Yu,Module);
            String quntext = data.quntext;
            String qun = data.qun;
            String uin = data.uin;
            String qq=myUin;
            int mtype=data.mtype;
            long msgid=data.msgid;
            int msgtype=data.msgtype;
            if(mtype==2) {
                if(读(qun,"lengyu520","开关")==1&&判断群(qun,mtype)==1) {
                    if("刷屏检测".equals(quntext)) {
                        if(qq.equals(uin)||读(qun,"代管",uin)==1||读("0","代管",uin)==1) {
                            if(读(qun, "刷屏检测", "开关") == 1) lengyu = "开";
                            else lengyu = "关";
                            sendText(data,"开启/关闭刷屏检测\n设置刷屏禁言+时间(秒)\n设置刷屏间隔+时间(秒)\n设置刷屏条数+数量\nTips:Q群管家，白名单，代管，自己不触发\n\n刷屏检测("+lengyu+")");
                        }
                    }
                    int first = 读("0", "代管", uin);
                    int second = 读(qun, "代管", uin);
                    int third = 读("0", "白名单", uin);
                    int fourth = 读(qun, "白名单", uin);
                    long 刷屏间隔 = 读(qun, "刷屏检测", "刷屏间隔");
                    long time1 = 读(qun, "刷屏检测", "" + uin + "发言时间");
                    long time2 = data.originMsg.msgTime;
                    int jy = 读(qun, "刷屏检测", "刷屏禁言");
                    if(quntext.equals("开启刷屏检测"))
                    {
                        if(qq.equals(uin) || 读(qun, "代管", uin) == 1 || 读("0", "代管", uin) == 1)
                        {
                            写(qun, "刷屏检测", "开关", 1);
                            String menu = "已开启刷屏检测";
                            sendText(data, menu);
                        }
                    }
                    if(quntext.matches("设置刷屏间隔[0-9]+"))
                    {
                        if(qq.equals(uin) || 读(qun, "代管", uin) == 1 || 读("0", "代管", uin) == 1)
                        {
                            int one = Long.parseLong(quntext.substring(6));
                            写(qun, "刷屏检测", "刷屏间隔", one);
                            sendText(data, "写入刷屏间隔成功～");
                        }
                    }
                    if(quntext.matches("设置刷屏禁言[0-9]+"))
                    {
                        if(qq.equals(uin) || 读(qun, "代管", uin) == 1 || 读("0", "代管", uin) == 1)
                        {
                            int one = Long.parseLong(quntext.substring(6));
                            写(qun, "刷屏检测", "刷屏禁言", one);
                            sendText(data, "写入刷屏禁言时间成功～");
                        }
                    }
                    if(quntext.equals("关闭刷屏检测"))
                    {
                        if(qq.equals(uin) || 读(qun, "代管", uin) == 1 || 读("0", "代管", uin) == 1)
                        {
                            写(qun, "刷屏检测", "开关", 0);
                            String menu = "已关闭刷屏检测";
                            sendText(data, menu);
                        }
                    }
                    if(quntext.matches("设置刷屏条数[0-9]+"))
                    {
                        if(qq.equals(uin) || 读(qun, "代管", uin) == 1 || 读("0", "代管", uin) == 1)
                        {
                            int one = Long.parseLong(quntext.substring(6));
                            写(qun, "刷屏检测", "刷屏条数", one);
                            sendText(data, "写入刷屏条数成功～");
                        }
                    }
                    //来自云上升
                    if(读(qun, "刷屏检测", "开关") == 1)
                    {
                        new Thread(new Runnable()
                        {
                            public void run()
                            {
                                int 刷屏时间 = 读(qun, "刷屏检测", "刷屏间隔");
                                int 刷屏条数 = 读(qun, "刷屏检测", "刷屏条数");
                                int 刷屏禁言 = 读(qun, "刷屏检测", "刷屏禁言");
                                int first = 读("0", "代管", uin);
                                int second = 读(qun, "代管", uin);
                                int third = 读("0", "白名单", uin);
                                int fourth = 读(qun, "白名单", uin);
                                if(刷屏时间 == 0) 刷屏时间 = 5;
                                if(刷屏条数 == 0) 刷屏条数 = 10;
                                if(刷屏禁言 == 0) 刷屏禁言 = 60;
                                boolean isFlood = isFloodDetected(uin, quntext, 刷屏时间, 刷屏条数);
                                //检测刷屏
                                if(!uin.equals("2854196310") && first != 1 && second != 1 && third != 1 && fourth != 1 && !qq.equals(uin) && !Arrays.asList(owner).contains(uin) && !uin.equals("0") && !quntext.equals(""))
                                {
                                    if(isFlood)
                                    {
                                        shutUp(qun, uin, 刷屏禁言);
                                        sendMsg(qun, "QQ:" + uin + "\n已被禁言" + 刷屏禁言 + "秒\n原因:" + 刷屏时间 + "秒发了" + 刷屏条数 + "条以上", 2);
                                        recallMsg(qun,data.msgid,2);
                                    }
                                }
                            }
                        }
                        ).start();
                    }
                }
            }
        }
    }
    ).start();
}
//云上升
HashMap lastMessageMap = new HashMap();
HashMap lastMessageTimeMap = new HashMap();
HashMap messageCountMap = new HashMap();
//存储每个用户最近一次发送的消息内容和时间戳
public boolean isFloodDetected(String user, String message, int timeThresholdInSeconds, int maxMessagesPerInterval)
{
    long currentTime = System.currentTimeMillis() / 1000;
    //如果用户之前发送过消息
    if(lastMessageTimeMap.containsKey(user))
    {
        long lastMessageTime = lastMessageTimeMap.get(user);
        //如果距离上次发送的时间小于阈值
        if(currentTime - lastMessageTime <= timeThresholdInSeconds)
        {
            //增加用户的消息数量计数
            int messageCount = messageCountMap.getOrDefault(user, 0) + 1;
            messageCountMap.put(user, messageCount);
            //如果消息数量超过阈值，则触发警报
            if(messageCount > maxMessagesPerInterval)
            {
                return true;
            }
        }
        else
        {
            //重置消息数量计数
            messageCountMap.put(user, 1);
        }
    }
    else
    {
        //初始化消息数量计数
        messageCountMap.put(user, 1);
    }
    //更新用户的最新消息内容和时间
    lastMessageMap.put(user, message);
    lastMessageTimeMap.put(user, currentTime);
    return false;
}