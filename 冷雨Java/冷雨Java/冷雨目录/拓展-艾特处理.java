public void 艾特处理(Object Yu)
{
    new Thread(new Runnable() {
        public void run() {
            Object data = getData();
            data.put(Yu,""+Module);
            String quntext = data.quntext;
            String qun = data.qun;
            String uin = data.uin;
            String qq=myUin;
            int mtype=data.mtype;
            long msgid=data.msgid;
            int msgtype=data.msgtype;
            int ATF = 读("0", "艾特禁言", "时间");
            int AishutUp = 读(qun, "艾特管家禁言", "时间");
            if("艾特处理".equals(quntext)) {
                if(qq.equals(uin)||读("0","代管",uin)==1) {
                    if(读("0","艾特回复","开关")==1) lengyu="开";
                    else lengyu="关";
                    if(读("0","艾特提醒","开关")==1) lengyu1="开";
                    else lengyu1="关";
                    if(读("0","艾特禁言","开关")==1) lengyu2="开";
                    else lengyu2="关";
                    String menu="艾特处理:\n开启/关闭艾特回复\n开启/关闭艾特禁言\n开启/关闭艾特提醒\n设置艾特回复+内容\n查看变量\n设置艾特禁言+时间(秒)\n查看艾特禁言\n\n艾特回复("+lengyu+")\n艾特提醒("+lengyu1+")\n艾特禁言("+lengyu2+")";
                    sendText(data,menu);
                }
            }
            if(quntext.matches("设置艾特管家禁言[0-9]+"))
            {
                if(qq.equals(uin) || 读(qun, "代管", uin) == 1 || 读("0", "代管", uin) == 1)
                {
                    int one = Long.parseLong(quntext.substring(8));
                    写(qun, "艾特管家禁言", "时间", one);
                    sendText(data, "写入艾特管家禁言成功～");
                }
            }
            if("查看艾特禁言".equals(quntext))
            {
                if(qq.equals(uin) || 读("0", "代管", uin) == 1)
                {
                    int time = 读("0", "艾特禁言", "时间");
                    sendText(data, "当前艾特禁言:" + time + "秒");
                }
            }
            if(quntext.matches("设置艾特禁言[0-9]+"))
            {
                if(qq.equals(uin) || 读("0", "代管", uin) == 1)
                {
                    int one = Long.parseLong(quntext.substring(6));
                    写("0", "艾特禁言", "时间", one);
                    sendText(data, "写入艾特禁言成功～");
                }
            }
            if("开启艾特提醒".equals(quntext))
            {
                if(qq.equals(uin) || 读("0", "代管", uin) == 1)
                {
                    写("0", "艾特提醒", "开关", 1);
                    String menu = "开启艾特提醒成功";
                    sendText(data, menu);
                }
            }
            if("关闭艾特提醒".equals(quntext))
            {
                if(qq.equals(uin) || 读("0", "代管", uin) == 1)
                {
                    写("0", "艾特提醒", "开关", 0);
                    String menu = "关闭艾特提醒成功";
                    sendText(data, menu);
                }
            }
            if(quntext.startsWith("设置艾特回复"))
            {
                if(qq.equals(uin) || 读("0", "代管", uin) == 1)
                {
                    String text = quntext.substring(6);
                    写("0", "艾特回复", "内容", text);
                    String menu = "设置成功，当前艾特回复:" + text;
                    sendText(data, menu);
                }
            }
            if("开启艾特禁言".equals(quntext))
            {
                if(qq.equals(uin) || 读("0", "代管", uin) == 1)
                {
                    写("0", "艾特禁言", "开关", 1);
                    String menu = "开启艾特禁言成功";
                    sendText(data, menu);
                }
            }
            if("开启艾特回复".equals(quntext))
            {
                if(qq.equals(uin) || 读("0", "代管", uin) == 1)
                {
                    写("0", "艾特回复", "开关", 1);
                    String menu = "开启艾特回复成功";
                    sendText(data, menu);
                }
            }
            if("关闭艾特禁言".equals(quntext))
            {
                if(qq.equals(uin) || 读("0", "代管", uin) == 1)
                {
                    写("0", "艾特禁言", "开关", 0);
                    String menu = "关闭艾特禁言成功";
                    sendText(data, menu);
                }
            }
            if("关闭艾特回复".equals(quntext))
            {
                if(qq.equals(uin) || 读("0", "代管", uin) == 1)
                {
                    写("0", "艾特回复", "开关", 0);
                    String menu = "关闭艾特回复成功";
                    sendText(data, menu);
                }
            }
            if("查看艾特回复".equals(quntext))
            {
                if(qq.equals(uin) || 读("0", "代管", uin) == 1)
                {
                    String text = 文字("0", "艾特回复", "内容");
                    sendText(data, "当前艾特回复:" + text);
                }
            }
            if(data.atList.contains(qq) && !uin.equals("2854196310"))
            {
                if(读("0", "艾特回复", "开关") == 1)
                {
                    String text = 文字("0", "艾特回复", "内容");
                    if(!text.equals(""))
                    {
                        if(text.startsWith("[语音"))
                        {
                            String cnm = text.substring(3);
                            String cnmm = cnm.replace("]", "");
                            sendPtt(qun, cnmm, 2);
                        }
                        else if(text.startsWith("[访问"))
                        {
                            String cnm = text.substring(3);
                            String cnmm = cnm.replace("]", "");
                            String cnmmb = get(cnmm);
                            sendMsg(qun, cnmmb, 2);
                        }
                        else if(text.startsWith("[图片"))
                        {
                            String cnm = text.substring(3);
                            String cnmm = cnm.replace("]", "");
                            sendImg(qun, cnmm, 2);
                        }
                        else if(text.equals("[GPT]"))
                        {
                            sendGPT(Yu);
                        }
                        else if(text.equals("[拍一拍]"))
                        {
                            sendPaiYiPai(qun, uin, mtype);
                        }
                        else if(text.startsWith("Java:"))
                        {
                        String cnm = text.substring(5);
                        this.interpreter.set("data", data);
                        this.interpreter.set("msg", data.originMsg);
                        this.interpreter.set("qun", qun);
                        this.interpreter.set("uin", uin);
                        this.interpreter.set("mtype", mtype);
                        this.interpreter.set("msgtype", msgtype);
                        this.interpreter.set("qq", myUin);
                        this.interpreter.eval(cnm,"eval stream");
                        }
                        else
                        {
                            String msg = text.replace("[at]", "[atUin=" + uin + "]");
                            msg = msg.replace("[qq]", qq);
                            msg = msg.replace("[uin]", uin);
                            msg = msg.replace("[qun]", qun);
                            msg = msg.replace("[Name]", getUserName(uin));
                            msg = msg.replace("[GroupName]", data.originMsg.peerName);
                            msg = msg.replace("[time]", timestampToDate(data.originMsg.msgTime * 1000));
                            msg = msg.replace("[图片", "\n[pic=");
                            msg = msg.replace("[GroupMemberCount]", getGroupMemberList(qun).size()+"");
                            File d = new File(ColdRainPath + "下载/随机一言.txt");
                            String menu = msg.replace("[一言]", 取文件(d));
                            sendMsg(qun, menu, 2);
                        }
                    }
                }
                if(读("0", "艾特禁言", "开关") == 1)
                {
                    if(ATF != 0)
                    {
                        shutUp(qun, uin, ATF);
                    }
                }
                if(读("0", "艾特提醒", "开关") == 1)
                {
                    String time = timestampToDate(data.originMsg.msgTime * 1000);
                    sendMsg(myUin, "主人，收到艾特啦！\nQQ:" + uin + "\n昵称:" + getUserName(uin) + "\n群号:" + qun + "\n群名:" + data.originMsg.peerName + "\n消息内容:" + quntext + "\n时间:" + time, 1);
                    Toast("收到艾特啦！群:" + qun + "(" + data.originMsg.peerName + ")");
                }
            }
            if(读(qun, "艾特管家禁言", "开关") == 1)
            {
                if(读(qun, "lengyu520", "开关") == 1)
                {
                    if(uin.equals("2854196310") && quntext.endsWith("嗨~，我是Q群管家，可以发送入群欢迎和定时消息，暂时还不能和你对话哦。"))
                    {
                        if(data.atList.size() >= 1)
                        {
                            String at = data.atList.get(0);
                            if(!at.equals(qq) || 读("0", "代管", uin) == 0 || 读("" + qun, "代管", uin) == 0 || 读("0", "白名单", uin) == 0 || 读("" + qun, "白名单", uin) == 0)
                            {
                                shutUp(qun, at, AishutUp);
                                sendText(data, at + "被禁言" + AishutUp + "秒\n原因:被Q群管家艾特");
                            }
                        }
                    }
                }
            }
        }
    }
    ).start();
}