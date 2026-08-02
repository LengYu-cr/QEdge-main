public void 整点报时(Object Yu) {
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
            if(读(qun,"lengyu520","开关")==1&&判断群(qun,mtype)==1) {
                if(quntext.equals("开启整点报时")) {
                    if(qq.equals(uin)||读(qun,"代管",uin)==1||读("0","代管",uin)==1) {
                        写(qun,"整点报时","开关",mtype);
                        String menu="已开启本聊天整点报时";
                        sendText(data,menu);
                    }
                }
                if(quntext.equals("关闭整点报时")) {
                    if(qq.equals(uin)||读(qun,"代管",uin)==1||读("0","代管",uin)==1) {
                        写(qun,"整点报时","开关",0);
                        String menu="已关闭本聊天整点报时";
                        sendText(data,menu);
                    }
                }
                if(quntext.equals("整点报时")) {
                    if(qq.equals(uin)||读("0","代管",uin)==1||读(qun,"代管",uin)==1) {
                        String lengyu="关";
                        if(读(qun,"整点报时","开关")==1||读(qun,"整点报时","开关")==2) lengyu="开";
                        String menu="整点报时:\n开启/关闭整点报时\n切换语音/文字/图片/自定义报时(默认文字)\n设置报时内容+内容(可用变量)\n报时测试 查看变量\n\n整点报时("+lengyu+")";
                        sendText(data,menu);
                    }
                }
                if(quntext.equals("报时测试")) {
                    if(qq.equals(uin)||读(qun,"代管",uin)==1||读("0","代管",uin)==1) {
                        sendBaoShi(qun,mtype);
                    }
                }
                if(quntext.startsWith("切换")&&quntext.endsWith("报时")) {
                    if(qq.equals(uin)||读(qun,"代管",uin)==1||读("0","代管",uin)==1) {
                        String text=quntext.replace("切换","").replace("报时","");
                        if(text.equals("文字")||text.equals("语音")||text.equals("图片")||text.equals("自定义")) {
                            写(qun,"整点报时","报时方式",text);
                            String menu="已切换为"+text+"报时";
                            sendText(data,menu);
                            sendBaoShi(qun,mtype);
                        }
                    }
                }
                if(quntext.startsWith("设置报时内容")) {
                    if(qq.equals(uin)||读(qun,"代管",uin)==1||读("0","代管",uin)==1) {
                        String text=quntext.substring(6);
                        写(qun,"整点报时","自定义报时",text);
                        String menu="设置自定义报时成功";
                        sendText(data,menu);
                        sendBaoShi(qun,mtype);
                    }
                }
            }
        }
    }
    ).start();
}
new Thread(new Runnable()
{
    public void run()
    {
        while(true)
        {
            String time = new Date().toString();
            if(time.contains(":00:00"))
            {
                new Thread(new Runnable() {
                    public void run() {
                        GroupChimeOnTheHour();
                    }
                }
                ).start();
                new Thread(new Runnable() {
                    public void run() {
                        FriendChimeOnTheHour();
                    }
                }
                ).start();
                Thread.sleep(1000);
            }
        }
    }
}
).start();
public void GroupChimeOnTheHour()
{
    if(Module.equals("Serendipity") || Module.equals("模了个块")) {
        for(HashMap info: getGroupList())
        {
            String um = info.get("group");
            if(读(um, "lengyu520", "开关") == 1)
            {
                if(读(um, "整点报时", "开关") == 2||读(um, "整点报时", "开关") == 1)
                {
                    sendBaoShi(um,2);
                }
            }
        }
    }
    else if(Module.equals("QStory")) {
        Object st=getGroupList();
        for(Object b:st)
        {
            String um=b.GroupUin;
            if(读(um, "lengyu520", "开关") == 1)
            {
                if(读(um, "整点报时", "开关") == 2||读(um, "整点报时", "开关") == 1)
                {
                    sendBaoShi(um,2);
                }
            }
        }
    }else if(Module.equals("QFun")) {
        Object st=getGroupList();
        for(Object b:st)
        {
            String um=b.group;
            if(读(um, "lengyu520", "开关") == 1)
            {
                if(读(um, "整点报时", "开关") == 2||读(um, "整点报时", "开关") == 1)
                {
                    sendBaoShi(um,2);
                }
            }
        }
    }
}
public void FriendChimeOnTheHour()
{
    IFriendDataService iFriendDataService = app.getRuntimeService(IFriendDataService.class);
    for(Friends friends:iFriendDataService.getAllFriends()) {
        String um = friends.uin;
        if(读(um, "lengyu520", "开关") == 1)
        {
            if(读(um, "整点报时", "开关") == 2||读(um, "整点报时", "开关") == 1)
            {
                sendBaoShi(um,1);
            }
        }
    }
}
public void sendBaoShi(String qun,int mtype)
{
    if(判断群(qun,2) == 1)
    {
        String type = 文字(qun, "整点报时", "报时方式");
        String time = timestampToDate(System.currentTimeMillis());
        String menu = "整点报时系统提醒您:\n现在是" + time;
        if(type.equals("") || type.equals("文字"))
        {
            File d = new File(ColdRainPath + "下载/随机一言.txt");
            sendMsg(qun, menu+"\n"+取文件(d), mtype);
        }
        else if(type.equals("语音"))
        {
            try {
                sendPtt(qun, myWeb+"baoshi.php", mtype);
                //https://xiaoapi.cn/API/data/baoshi/1.mp3
            }
            catch(e) {
                sendPtt(qun, "http://api.lolimi.cn/API/BaoShi/mp3/"+时()+".mp3", mtype);
            }
        }
        else if(type.equals("图片"))
        {
            sendImg(qun, myWeb+"time.php", mtype);
        }
        else if(type.equals("自定义"))
        {
            String text = 文字(qun, "整点报时", "自定义报时");
            if(!text.equals(""))
            {
                if(text.startsWith("[语音"))
                {
                    String cnm = text.substring(3);
                    String cnmm = cnm.replace("]", "");
                    sendPtt(qun, cnmm, mtype);
                }
                else if(text.startsWith("[访问"))
                {
                    String cnm = text.substring(3);
                    String cnmm = cnm.replace("]", "");
                    String cnmmb = get(cnmm);
                    sendMsg(qun, cnmmb, mtype);
                }
                else if(text.startsWith("[图片"))
                {
                    String cnm = text.substring(3);
                    String cnmm = cnm.replace("]", "");
                    sendImg(qun, cnmm, mtype);
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
                    String msg = text.replace("[qq]", myUin);
                    msg = msg.replace("[uin]", myUin);
                    msg = msg.replace("[qun]", qun);
                    msg = msg.replace("[Name]", getUserName(myUin));
                    msg = msg.replace("[time]", time);
                    if(mtype==2) {
                        msg = msg.replace("[GroupMemberCount]", getGroupMemberList(qun).size()+"");
                        msg = msg.replace("[GroupName]", getGroupNames(qun));
                        msg = msg.replace("[at]", "[atUin=" + myUin + "]");
                    }
                    else if(mtype==1) {
                        msg = msg.replace("[GroupName]", getUserName(qun));
                    }
                    msg = msg.replace("[图片", "\n[pic=");
                    File d = new File(ColdRainPath + "下载/随机一言.txt");
                    String menu = msg.replace("[一言]", 取文件(d));
                    sendMsg(qun, menu, mtype);
                }
            }
        }
    }
}