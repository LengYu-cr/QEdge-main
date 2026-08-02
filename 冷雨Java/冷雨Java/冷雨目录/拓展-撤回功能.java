public void 撤回功能(Object Yu) {
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
                if(读(qun,"lengyu520","开关")==1&&cdxz(qun,uin)&&判断群(qun,mtype)==1) {
                    int time = 读(qun, "撤回禁言", "时间");
                    if(time == 0) time = 5;
                    else time = time;
                    if("撤回功能".equals(quntext))
                    {
                        if(qq.equals(uin) || 读(qun, "代管", uin) == 1 || 读("0", "代管", uin) == 1)
                        {
                            if(读(qun, "撤回链接", "开关") == 1) lengyu = "开";
                            else lengyu = "关";
                            if(读(qun, "撤回卡片", "开关") == 1) lengyu1 = "开";
                            else lengyu1 = "关";
                            if(读(qun, "撤回语音", "开关") == 1) lengyu2 = "开";
                            else lengyu2 = "关";
                            if(读(qun, "撤回图片", "开关") == 1) lengyu3 = "开";
                            else lengyu3 = "关";
                            if(读("0", "撤回自身", "开关") == 1) lengyu4 = "开";
                            else lengyu4 = "关";
                            if(读(qun, "撤回红包", "开关") == 1) lengyu6 = "开";
                            else lengyu6 = "关";
                            if(读(qun, "撤回视频", "开关") == 1) lengyu7 = "开";
                            else lengyu7 = "关";
                            if(读(qun, "撤回灰字", "开关") == 1) lengyu8 = "开";
                            else lengyu8 = "关";
                            if(读(qun, "撤回数字", "开关") == 1) lengyu9 = "开";
                            else lengyu9 = "关";
                            int lengyu5 = 读("0", "撤回自身", "撤回间隔");
                            if(读(qun, "撤回禁言", "开关") == 1) lengyu10 = "开";
                            else lengyu10 = "关";
                            if(读(qun, "撤回踢出", "开关") == 1) lengyu11 = "开";
                            else lengyu11 = "关";
                            if(读(qun, "撤回Markdown", "开关") == 1) lengyu12 = "开";
                            else lengyu12 = "关";
                            String menu = "撤回功能:\n开启/关闭撤回链接\n开启/关闭撤回卡片\n开启/关闭撤回语音\n开启/关闭撤回红包\n开启/关闭撤回图片\n开启/关闭撤回灰字提示\n开启/关闭撤回Markdown\n开启/关闭撤回视频\n开启/关闭撤回数字\n开启/关闭撤回自身\n开启/关闭撤回禁言\n开启/关闭撤回踢出\n设置撤回禁言+时间(分)\n设置撤回间隔+时间(秒)\n查看撤回间隔\nTips:1.不撤回代管和白名单消息\n      2.撤回间隔是自身撤回的时间间隔\n\n撤回链接(" + lengyu + ")\n撤回卡片(" + lengyu1 + ")\n撤回语音(" + lengyu2 + ")\n撤回红包(" + lengyu6 + ")\n撤回视频(" + lengyu7 + ")\n撤回图片(" + lengyu3 + ")\n撤回灰字(" + lengyu8 + ")\n撤回数字(" + lengyu9 + ")\n撤回禁言(" + lengyu10 + ")\n撤回踢出(" + lengyu11 + ")\n撤回Markdown(" + lengyu12 + ")\n撤回自身(" + lengyu4 + ")\n撤回间隔(" + lengyu5 + "秒)";
                            sendText(data, menu);
                        }
                    }
                    if(quntext.equals("查看撤回间隔"))
                    {
                        if(qq.equals(uin) || 读("0", "代管", uin) == 1)
                        {
                            int 自身撤回 = 读("0", "撤回自身", "撤回间隔");
                            sendText(data, "当前撤回间隔为" + 自身撤回 + "秒");
                        }
                    }
                    if(quntext.equals("开启撤回自身"))
                    {
                        if(qq.equals(uin) || 读("0", "代管", uin) == 1)
                        {
                            写("0", "撤回自身", "开关", 1);
                            String menu = "已开启撤回自身";
                            sendText(data, menu);
                        }
                    }
                    if(读("0", "撤回自身", "开关") == 1)
                    {
                        if(msgtype == 1 || msgtype == 2 || msgtype == 4 || msgtype == 10)
                        {
                            int 自身撤回 = 读("0", "撤回自身", "撤回间隔");
                            int sj = 自身撤回 * 1000;
                            if(qq.equals(uin))
                            {
                                new Thread(new Runnable()
                                {
                                    public void run()
                                    {
                                        Thread.sleep(sj);
                                        recallMsg(qun,data.msgid,2);
                                        if(读(qun, "撤回禁言", "开关") == 1)
                                        {
                                            shutUp(qun, uin, time * 60);
                                        }
                                        if(读(qun, "撤回踢出", "开关") == 1)
                                        {
                                            kickGroup(qun, uin, false);
                                        }
                                    }
                                }
                                ).start();
                            }
                        }
                    }
                    if(quntext.matches("设置撤回间隔[0-9]+"))
                    {
                        if(qq.equals(uin) || 读("0", "代管", uin) == 1)
                        {
                            int one = Long.parseLong(quntext.substring(6));
                            写("0", "撤回自身", "撤回间隔", one);
                            sendText(data, "写入撤回间隔成功～");
                        }
                    }
                    if(quntext.equals("开启撤回自身"))
                    {
                        if(qq.equals(uin) || 读("0", "代管", uin) == 1)
                        {
                            写("0", "撤回自身", "开关", 1);
                            String menu = "全局已开启撤回自身";
                            sendText(data, menu);
                        }
                    }
                    if(quntext.equals("关闭撤回自身"))
                    {
                        if(qq.equals(uin) || 读("0", "代管", uin) == 1)
                        {
                            写("0", "撤回自身", "开关", 0);
                            String menu = "全局已关闭撤回自身";
                            sendText(data, menu);
                        }
                    }
                    if(quntext.equals("开启撤回图片"))
                    {
                        if(qq.equals(uin) || 读(qun, "代管", uin) == 1 || 读("0", "代管", uin) == 1)
                        {
                            写(qun, "撤回图片", "开关", 1);
                            String menu = "本聊天已开启撤回图片";
                            sendText(data, menu);
                        }
                    }
                    if(quntext.equals("关闭撤回图片"))
                    {
                        if(qq.equals(uin) || 读(qun, "代管", uin) == 1 || 读("0", "代管", uin) == 1)
                        {
                            写(qun, "撤回图片", "开关", 0);
                            String menu = "本聊天已关闭撤回图片";
                            sendText(data, menu);
                        }
                    }
                    if(quntext.equals("开启撤回数字"))
                    {
                        if(qq.equals(uin) || 读(qun, "代管", uin) == 1 || 读("0", "代管", uin) == 1)
                        {
                            写(qun, "撤回数字", "开关", 1);
                            String menu = "本聊天已开启撤回数字";
                            sendText(data, menu);
                        }
                    }
                    if(quntext.equals("关闭撤回数字"))
                    {
                        if(qq.equals(uin) || 读(qun, "代管", uin) == 1 || 读("0", "代管", uin) == 1)
                        {
                            写(qun, "撤回数字", "开关", 0);
                            String menu = "本聊天已关闭撤回数字";
                            sendText(data, menu);
                        }
                    }
                    if(quntext.equals("开启撤回禁言"))
                    {
                        if(qq.equals(uin) || 读(qun, "代管", uin) == 1 || 读("0", "代管", uin) == 1)
                        {
                            写(qun, "撤回禁言", "开关", 1);
                            String menu = "本聊天已开启撤回禁言";
                            sendText(data, menu);
                        }
                    }
                    if(quntext.equals("关闭撤回禁言"))
                    {
                        if(qq.equals(uin) || 读(qun, "代管", uin) == 1 || 读("0", "代管", uin) == 1)
                        {
                            写(qun, "撤回禁言", "开关", 0);
                            String menu = "本聊天已关闭撤回禁言";
                            sendText(data, menu);
                        }
                    }
                    if(quntext.equals("开启撤回踢出"))
                    {
                        if(qq.equals(uin) || 读(qun, "代管", uin) == 1 || 读("0", "代管", uin) == 1)
                        {
                            写(qun, "撤回踢出", "开关", 1);
                            String menu = "本聊天已开启撤回踢出";
                            sendText(data, menu);
                        }
                    }
                    if(quntext.equals("关闭撤回踢出"))
                    {
                        if(qq.equals(uin) || 读(qun, "代管", uin) == 1 || 读("0", "代管", uin) == 1)
                        {
                            写(qun, "撤回踢出", "开关", 0);
                            String menu = "本聊天已关闭撤回踢出";
                            sendText(data, menu);
                        }
                    }
                    if(quntext.matches("设置撤回禁言[0-9]+"))
                    {
                        if(qq.equals(uin) || 读(qun, "代管", uin) == 1 || 读("0", "代管", uin) == 1)
                        {
                            int one = Long.parseLong(quntext.substring(6));
                            写(qun, "撤回禁言", "时间", one);
                            sendText(data, "写入撤回禁言时间成功～");
                        }
                    }
                    if(quntext.equals("开启撤回Markdown"))
                    {
                        if(qq.equals(uin) || 读(qun, "代管", uin) == 1 || 读("0", "代管", uin) == 1)
                        {
                            写(qun, "撤回Markdown", "开关", 1);
                            String menu = "本聊天已开启撤回Markdown";
                            sendText(data, menu);
                        }
                    }
                    if(quntext.equals("关闭撤回Markdown"))
                    {
                        if(qq.equals(uin) || 读(qun, "代管", uin) == 1 || 读("0", "代管", uin) == 1)
                        {
                            写(qun, "撤回Markdown", "开关", 0);
                            String menu = "本聊天已关闭撤回Markdown";
                            sendText(data, menu);
                        }
                    }
                    if(quntext.equals("开启撤回红包"))
                    {
                        if(qq.equals(uin) || 读(qun, "代管", uin) == 1 || 读("0", "代管", uin) == 1)
                        {
                            写(qun, "撤回红包", "开关", 1);
                            String menu = "本聊天已开启撤回红包";
                            sendText(data, menu);
                        }
                    }
                    if(quntext.equals("关闭撤回红包"))
                    {
                        if(qq.equals(uin) || 读(qun, "代管", uin) == 1 || 读("0", "代管", uin) == 1)
                        {
                            写(qun, "撤回红包", "开关", 0);
                            String menu = "本聊天已关闭撤回红包";
                            sendText(data, menu);
                        }
                    }
                    if(quntext.equals("开启撤回视频"))
                    {
                        if(qq.equals(uin) || 读(qun, "代管", uin) == 1 || 读("0", "代管", uin) == 1)
                        {
                            写(qun, "撤回视频", "开关", 1);
                            String menu = "本聊天已开启撤回视频";
                            sendText(data, menu);
                        }
                    }
                    if(quntext.equals("关闭撤回视频"))
                    {
                        if(qq.equals(uin) || 读(qun, "代管", uin) == 1 || 读("0", "代管", uin) == 1)
                        {
                            写(qun, "撤回视频", "开关", 0);
                            String menu = "本聊天已关闭撤回视频";
                            sendText(data, menu);
                        }
                    }
                    if(quntext.equals("开启撤回卡片"))
                    {
                        if(qq.equals(uin) || 读(qun, "代管", uin) == 1 || 读("0", "代管", uin) == 1)
                        {
                            写(qun, "撤回卡片", "开关", 1);
                            String menu = "本聊天已开启撤回卡片";
                            sendText(data, menu);
                        }
                    }
                    if(quntext.equals("关闭撤回卡片"))
                    {
                        if(qq.equals(uin) || 读(qun, "代管", uin) == 1 || 读("0", "代管", uin) == 1)
                        {
                            写(qun, "撤回卡片", "开关", 0);
                            String menu = "本聊天已关闭撤回卡片";
                            sendText(data, menu);
                        }
                    }
                    if(quntext.equals("开启撤回语音"))
                    {
                        if(qq.equals(uin) || 读(qun, "代管", uin) == 1 || 读("0", "代管", uin) == 1)
                        {
                            写(qun, "撤回语音", "开关", 1);
                            String menu = "本聊天已开启撤回语音";
                            sendText(data, menu);
                        }
                    }
                    if(quntext.equals("关闭撤回语音"))
                    {
                        if(qq.equals(uin) || 读(qun, "代管", uin) == 1 || 读("0", "代管", uin) == 1)
                        {
                            写(qun, "撤回语音", "开关", 0);
                            String menu = "本聊天已关闭撤回语音";
                            sendText(data, menu);
                        }
                    }
                    if(quntext.equals("开启撤回灰字提示"))
                    {
                        if(qq.equals(uin) || 读(qun, "代管", uin) == 1 || 读("0", "代管", uin) == 1)
                        {
                            写(qun, "撤回灰字", "开关", 1);
                            String menu = "本聊天已开启撤回灰字提示";
                            sendText(data, menu);
                        }
                    }
                    if(quntext.equals("关闭撤回灰字提示"))
                    {
                        if(qq.equals(uin) || 读(qun, "代管", uin) == 1 || 读("0", "代管", uin) == 1)
                        {
                            写(qun, "撤回灰字", "开关", 0);
                            String menu = "本聊天已关闭撤回灰字提示";
                            sendText(data, menu);
                        }
                    }
                    if(quntext.equals("开启撤回链接"))
                    {
                        if(qq.equals(uin) || 读(qun, "代管", uin) == 1 || 读("0", "代管", uin) == 1)
                        {
                            写(qun, "撤回链接", "开关", 1);
                            String menu = "本聊天已开启撤回链接";
                            sendText(data, menu);
                        }
                    }
                    if(quntext.equals("关闭撤回链接"))
                    {
                        if(qq.equals(uin) || 读(qun, "代管", uin) == 1 || 读("0", "代管", uin) == 1)
                        {
                            写(qun, "撤回链接", "开关", 0);
                            String menu = "本聊天已关闭撤回链接";
                            sendText(data, menu);
                        }
                    }
                    if(msgtype == 2 && !uin.equals(qq) && !uin.equals("2854196310"))
                    {
                        if(读(qun, "撤回图片", "开关") == 1)
                        {
                            int a = 读(qun, "白名单",uin);
                            int b = 读("0", "白名单",uin);
                            int c = 读(qun, "代管",uin);
                            int d = 读("0", "代管",uin);
                            if(uin.equals(qq) || a==1 ||b==1||c==1||d==1)
                            {
                            }
                            else
                            {
                                recallMsg(qun,data.msgid,2);
                                if(读(qun, "撤回禁言", "开关") == 1)
                                {
                                    shutUp(qun, uin, time * 60);
                                }
                                if(读(qun, "撤回踢出", "开关") == 1)
                                {
                                    kickGroup(qun, uin, false);
                                }
                                String menu = uin + "被撤回了一条消息，原因:开启了撤回图片";
                                sendText(data, menu);
                            }
                        }
                    }
                    if(msgtype == 14 && !uin.equals(qq) && !uin.equals("2854196310"))
                    {
                        if(读(qun, "撤回Markdown", "开关") == 1)
                        {
                            int a = 读(qun, "白名单",uin);
                            int b = 读("0", "白名单",uin);
                            int c = 读(qun, "代管",uin);
                            int d = 读("0", "代管",uin);
                            if(uin.equals(qq) || a==1 ||b==1||c==1||d==1)
                            {
                            }
                            else
                            {
                                recallMsg(qun,data.msgid,2);
                                if(读(qun, "撤回禁言", "开关") == 1)
                                {
                                    shutUp(qun, uin, time * 60);
                                }
                                if(读(qun, "撤回踢出", "开关") == 1)
                                {
                                    kickGroup(qun, uin, false);
                                }
                                String menu = uin + "被撤回了一条消息，原因:开启了撤回Markdown";
                                sendText(data, menu);
                            }
                        }
                    }
                    if(msgtype == 9 && !uin.equals(qq) && !uin.equals("2854196310"))
                    {
                        if(读(qun, "撤回红包", "开关") == 1)
                        {
                            int a = 读(qun, "白名单",uin);
                            int b = 读("0", "白名单",uin);
                            int c = 读(qun, "代管",uin);
                            int d = 读("0", "代管",uin);
                            if(uin.equals(qq) || a==1 ||b==1||c==1||d==1)
                            {
                            }
                            else
                            {
                                recallMsg(qun,data.msgid,2);
                                if(读(qun, "撤回禁言", "开关") == 1)
                                {
                                    shutUp(qun, uin, time * 60);
                                }
                                if(读(qun, "撤回踢出", "开关") == 1)
                                {
                                    kickGroup(qun, uin, false);
                                }
                                String menu = uin + "被撤回了一条消息，原因:开启了撤回红包";
                                sendText(data, menu);
                            }
                        }
                    }
                    if(msgtype == 5 && !uin.equals(qq) && !uin.equals("2854196310"))
                    {
                        if(读(qun, "撤回视频", "开关") == 1)
                        {
                            int a = 读(qun, "白名单",uin);
                            int b = 读("0", "白名单",uin);
                            int c = 读(qun, "代管",uin);
                            int d = 读("0", "代管",uin);
                            if(uin.equals(qq) || a==1 ||b==1||c==1||d==1)
                            {
                            }
                            else
                            {
                                recallMsg(qun,data.msgid,2);
                                if(读(qun, "撤回禁言", "开关") == 1)
                                {
                                    shutUp(qun, uin, time * 60);
                                }
                                if(读(qun, "撤回踢出", "开关") == 1)
                                {
                                    kickGroup(qun, uin, false);
                                }
                                String menu = uin + "被撤回了一条消息，原因:开启了撤回视频";
                                sendText(data, menu);
                            }
                        }
                    }
                    if(msgtype == 1 && !uin.equals(qq) && !uin.equals("2854196310") && quntext.matches(".*\\d.*"))
                    {
                        if(读(qun, "撤回数字", "开关") == 1)
                        {
                            int a = 读(qun, "白名单",uin);
                            int b = 读("0", "白名单",uin);
                            int c = 读(qun, "代管",uin);
                            int d = 读("0", "代管",uin);
                            if(uin.equals(qq) || a==1 ||b==1||c==1||d==1)
                            {
                            }
                            else
                            {
                                recallMsg(qun,data.msgid,2);
                                if(读(qun, "撤回禁言", "开关") == 1)
                                {
                                    shutUp(qun, uin, time * 60);
                                }
                                if(读(qun, "撤回踢出", "开关") == 1)
                                {
                                    kickGroup(qun, uin, false);
                                }
                                String menu = uin + "被撤回了一条消息，原因:开启了撤回数字";
                                sendText(data, menu);
                            }
                        }
                    }
                    if(data.originMsg.msgType==11&& !uin.equals(qq) && !uin.equals("2854196310"))
                    {
                        if(读(qun, "撤回卡片", "开关") == 1)
                        {
                            int a = 读(qun, "白名单",uin);
                            int b = 读("0", "白名单",uin);
                            int c = 读(qun, "代管",uin);
                            int d = 读("0", "代管",uin);
                            if(uin.equals(qq) || a==1 ||b==1||c==1||d==1)
                            {
                            }
                            else
                            {
                                recallMsg(qun,data.msgid,2);
                                if(读(qun, "撤回禁言", "开关") == 1)
                                {
                                    shutUp(qun, uin, time * 60);
                                }
                                if(读(qun, "撤回踢出", "开关") == 1)
                                {
                                    kickGroup(qun, uin, false);
                                }
                                String menu = uin + "被撤回了一条消息，原因:开启了撤回卡片";
                                sendText(data, menu);
                            }
                        }
                    }
                    if(msgtype == 4 && !uin.equals(qq) && !uin.equals("2854196310"))
                    {
                        if(读(qun, "撤回语音", "开关") == 1)
                        {
                            int a = 读(qun, "白名单",uin);
                            int b = 读("0", "白名单",uin);
                            int c = 读(qun, "代管",uin);
                            int d = 读("0", "代管",uin);
                            if(uin.equals(qq) || a==1 ||b==1||c==1||d==1)
                            {
                            }
                            else
                            {
                                recallMsg(qun,data.msgid,2);
                                if(读(qun, "撤回禁言", "开关") == 1)
                                {
                                    shutUp(qun, uin, time * 60);
                                }
                                if(读(qun, "撤回踢出", "开关") == 1)
                                {
                                    kickGroup(qun, uin, false);
                                }
                                String menu = uin + "被撤回了一条消息，原因:开启了撤回语音";
                                sendText(data, menu);
                            }
                        }
                    }
                    if(msgtype == 1 && !uin.equals(qq) && !uin.equals("2854196310"))
                    {
                        if(读(qun, "撤回链接", "开关") == 1)
                        {
                            int a = 读(qun, "白名单",uin);
                            int b = 读("0", "白名单",uin);
                            int c = 读(qun, "代管",uin);
                            int d = 读("0", "代管",uin);
                            if(uin.equals(qq) || a==1 ||b==1||c==1||d==1)
                            {
                            }
                            else
                            {
                                String[] URLList= {
                                    ".top",".cn",".com",".online",".vin",".ski",".gay",".cc",".xyz",".icu",".info",".love",".cloud",".store",".net",".tech",".shop",".vip","site",".art",".fun",".website",".kim",".tv",".club",".pro",".plus",".team",".gg",".企业",".在线",".中国",".edu"
                                };
                                boolean tf=false;
                                for(String u: URLList)
                                {
                                    if(quntext.contains(u))
                                    {
                                        tf=true;
                                        break;
                                    }
                                }
                                if(tf) {
                                    recallMsg(qun,data.msgid,2);
                                    if(读(qun, "撤回禁言", "开关") == 1)
                                    {
                                        shutUp(qun, uin, time * 60);
                                    }
                                    if(读(qun, "撤回踢出", "开关") == 1)
                                    {
                                        kickGroup(qun, uin, false);
                                    }
                                    String menu = uin + "被撤回了一条消息，原因:开启了撤回链接";
                                    sendText(data, menu);
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    ).start();
}