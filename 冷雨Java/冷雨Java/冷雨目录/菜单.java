public void 菜单(Object Yu)
{
    Object data = getData();
    data.put(Yu,""+Module);
    String quntext = data.quntext;
    String qun = data.qun;
    String uin = data.uin;
    String qq=myUin;
    int mtype=data.mtype;
    int msgtype=data.msgtype;
    long msgid=data.msgid;
    String menu_name="";
    if(mtype==2)menu_name=文字(qun,"菜单配置","menu_name");
    else if(mtype==1)menu_name=文字("0","菜单配置","menu_name");
    if(menu_name.equals("")) menu_name="菜单";
    originMsg(data);
    if(判断群(qun,mtype)==1) {
        if(mtype==2) {
            //管家监听
            if(quntext.startsWith("Come on!@")) {
                try {
                    String at=data.atList.get(0);
                    if(uin.equals(qq)&&at.equals("2854196310")) {
                        recallMsg(qun,data.msgid,2);
                    }
                }
                catch(e) {
                }
            }
            if(uin.equals("2854196310")&&data.originMsg.msgType==11) {
                if(文字(qun,"管家触发","触发人").equals(qq)) {
                    String b=读(ColdRainPath+"data/"+qq+"管家问题.txt");
                    String pskey=getPskey("qun.qq.com");
                    JSONObject json = new JSONObject(quntext.replace("}\n","}"));
                    写(ColdRainPath+"data/"+qun+"管家token.txt",json.getJSONObject("meta").getJSONObject("metadata").getString("token"));
                    String bb = 触发问答(qun,qq,skey,pskey,b,json.getJSONObject("meta").getJSONObject("metadata").getString("token"));
                    if(!bb.equals("成功")) {
                        sendMsg(qun,""+bb,2);
                        Toasts(""+bb);
                    }
                    else {
                        recallMsg(qun,data.msgid,2);
                        String c = 删除问答(qun,qq,skey,pskey,"1");
                        c = 删除问答(qun,qq,skey,pskey,"2");
                        写(ColdRainPath+"data/"+qq+"管家问题.txt","");
                        写(qun,"管家触发","触发人","0");
                        return;
                    }
                }
            }
            if(uin.equals(myUin)&&quntext.contains("代发")) {
                for(String uu : data.atList) {
                    if(myBotQQ.equals(uu)) {
                        recallMsg(qun,data.msgid,2);
                    }
                }
            }
            
        }
        if(!quntext.equals("")&&!uin.equals("0")&&data.originMsg.emojiLikesList.isEmpty()) {
            if("开机".equals(quntext)||"复活".equals(quntext)) {
                if(qq.equals(uin)||读(qun,"代管",uin)==1||读("0","代管",uin)==1) {
                    if(读(qun,"lengyu520","开关")==1&&cdxz(qun,uin)) {
                        sendText(data,"本聊天重复开机");
                        写(qun,"lengyu520","开关",1);
                    }
                    else {
                        sendText(data,"本聊天已开机");
                        写(qun,"lengyu520","开关",1);
                    }
                }
            }
            if("关机".equals(quntext)||"趋势".equals(quntext)) {
                if(qq.equals(uin)||读(qun,"代管",uin)==1||读("0","代管",uin)==1) {
                    if(读(qun,"lengyu520","开关")==0) {
                        sendText(data,"本聊天重复关机");
                        写(qun,"lengyu520","开关",0);
                    }
                    else {
                        sendText(data,"本聊天已关机");
                        写(qun,"lengyu520","开关",0);
                    }
                }
            }
            
            if(读(qun,"lengyu520","开关")==1&&cdxz(qun,uin)) {
                if(quntext.equals(menu_name)) {
                    Iterator keys = json_java.keys();
                    int length = 0;
                    String result = "";
                    while (keys.hasNext()) {
                        String key = keys.next();
                        int value = json_java.get(key);
                        if(value==1) {
                            key = key.replace("拓展-","").replace(".java","");
                            if(length==0) {
                                length++;
                                result += "\n║"+key;
                            }
                            else if(length==1) {
                                length=0;
                                result += "  "+key+"║";
                            }
                        }
                    }
                    if(length==1)result = result+URLSafeUtil.decode("*S1*-_e8bC81BthTTfKT1bej1Q6");
                    if(result.isEmpty()) {
                    String menu = "";
                        if(Module.equals("Serendipity")) {
                             menu = "您当前未加载任何拓展脚本。检测到运行模块为Serendipity，则拓展脚本请在 聊天页面右下角加号(长按)->拓展脚本 处加载";
                        } else if(Module.equals("QStory")) {
                             menu = "您当前未加载任何拓展脚本。检测到运行模块为QStory，则拓展脚本请在 悬浮窗->拓展脚本 处加载";
                        } else if(Module.equals("QFun")) {
                             menu = "您当前未加载任何拓展脚本。检测到运行模块为QFun，则拓展脚本请在 悬浮窗->拓展脚本 处加载";
                        } else if(Module.equals("模了个块")) {
                             menu = "您当前未加载任何拓展脚本。检测到运行模块为模了个块，则拓展脚本请在 聊天页面右下角加号(点击)->脚本菜单->拓展脚本 处加载";
                        } else {
                             menu = "模块不正确导致的错误";
                        }
                        sendText(data,menu);
                    }
                    else {
                        String menu = URLSafeUtil.decode("*S1*F39ZF30rF3jY-N9YZdTOyD_VEyhz8QxsWIsKjX_VEbCcF3jYF30rF39G")+result+"\n"+URLSafeUtil.decode("*S1*F39zF30rF30L-Njk8G9xyU0M8Njnm0ej1-uj1-hj1HW");
                        sendText(data,menu);
                    }
                }
                if(data.originMsg.records.isEmpty()) {
                    if(quntext.startsWith("复读")) {
                        if(qq.equals(uin)||读("0","代管",uin)==1) {
                            sendMsg(qun,quntext.substring(2),mtype);
                        }
                    }
                }
                else {
                    if(quntext.startsWith("复读")&&data.originMsg.msgType==9) {
                        if(qq.equals(uin)||读("0","代管",uin)==1) {
                            String text=quntext.substring(2);
                            int num=0;
                            if(text.matches("[0-9]+")) num=Integer.parseInt(text);
                            else num=1;
                            for(int q=0;
                            q<num;
                            q++) {
                                ArrayList MsgList=new ArrayList();
                                for(MsgRecord msgRecord : data.originMsg.records) {
                                    for(MsgElement msgElement : msgRecord.elements) {
                                        //TextElement textElement=new TextElement("成功",0,0,0,"",(Integer) 0,(Long) 0,null,(Long) 0,(Integer) 0,"",(Integer) 0);
                                        //MsgElement msgElement=QRoute.api(IMsgUtilApi.class).createTextElement(textElement);
                                        MsgList.add(msgElement);
                                    }
                                }
                                sendMsg(data.originMsg.peerUid,MsgList,mtype);
                            }
                        }
                    }
                    if(quntext.equals("复制")&&data.originMsg.msgType==9) {
                        if(qq.equals(uin)||读("0","代管",uin)==1) {
                            String text = "";
                            for(MsgRecord msgRecord : data.originMsg.records) {
                                Object data2 = getData();
                                data2.put(msgRecord,"OriginMsg");
                                text += data2.quntext;
                            }
                            PastetoClipboard("复制",text);
                        }
                    }
                }
                if("查看变量".equals(quntext)) {
                    if(qq.equals(uin)||读(qun,"代管",uin)==1||读("0","代管",uin)==1) {
                        String menu="查看变量:\n1.[qq]==自己的QQ\n2.[uin]==触发者的QQ\n3.[qun]==触发者所在群号\n4.[at]==艾特触发者\n5.[Name]==触发者群昵称\n6.[GroupName]==触发者所在群名\n7.[time]==触发时间\n8.[一言]==随机一言\n\n9.回复语音变量:[语音+语音链接]\n10.回复图片变量:[图片+图片链接]\n11.回复访问网址:[访问+链接]\n12.回复拍一拍变量:[拍一拍]\n13.艾特回复GPT模式:[GPT]\n\n14.定时任务变量(目前没写):[群打卡],[全体禁言],[全体解禁],[日签打卡],[空间签到],[发布说说],[大会签到],[抽群字符]\n15.执行代码变量:Java:+java代码\n16.[GroupMemberCount]==群人数\n\nTips:1.\"[]\"必须带上，且符号不能错！\n    2.本变量仅用于艾特回复，问答功能";
                        sendMsg(qun,menu,mtype);
                    }
                }
                if(quntext.equals("加群")) {
                    if(qq.equals(uin)||读(qun,"代管",uin)==1||读("0","代管",uin)==1) {
                        sendMsg(qun,"冷雨Java官方群:\nQQ:\n新群(冷雨·循迹) 935100470\n加群地址:https://api.yuafeng.cn/addGroup/\n\nTelegram:\nhttps://t.me/LengYuJava\n\n冷雨主页:"+myWang,mtype);
                    }
                }
            }
            if(quntext.equals("更新日志")) {
                if(qq.equals(uin)||读(qun,"代管",uin)==1||读("0","代管",uin)==1) {
                    String nb=读(pluginPath+"更新日志.txt");
                    if(qq.equals(uin)) {
                        setTips("更新日志:",""+nb);
                    }
                    else {
                        sendText(data,nb);
                    }
                }
            }
            
            
            if(quntext.matches("切换线路\\d+")) {
                if(qq.equals(uin)||读("0","代管",uin)==1) {
                    // 提取数字
                    String numStr = quntext.replaceAll("[^0-9]", "");
                    int type = Integer.parseInt(numStr);
                    // 调用切换线路
                    boolean ok = changeNetworkLine(type);
                    // 回复结果
                    if(ok) {
                        sendText(data, "切换成功，当前线路：" + NETWORK_LINE);
                    } else {
                        sendText(data, "切换失败，该线路不可用");
                    }
                }
            }
            
            if(quntext.equals("切换线路")) {
                if(qq.equals(uin)||读("0","代管",uin)==1) {
                sendText(data, "指令: 切换路线1-4\n线路1 => 北京/广州 腾讯云\n线路2 => 香港 创创云\n线路3 => 十堰 创创云\n线路4 => 新加坡 腾讯云");
                }
            }
            

            
            if(quntext.equals("更新拓展") || quntext.equals("下载拓展")) {
                if(qq.equals(uin)||读("0","代管",uin)==1) {
                    Toast("开始 下载/更新 拓展脚本");
                    downloadAllJavas(qun,mtype);
                }
            }
            //实用功能，作者自用
            if(quntext.startsWith("发送文件/")) {
                if(qq.equals(uin)||读(qun,"代管",uin)==1||读("0","代管",uin)==1) {
                    String name=quntext.substring(4);
                    sendFile(qun,name,mtype);
                }
            }
            if(quntext.startsWith("发送语音http")||quntext.startsWith("发送语音/")) {
                if(qq.equals(uin)||读(qun,"代管",uin)==1||读("0","代管",uin)==1) {
                    String voice=quntext.substring(4);
                    if(voice.startsWith("http")) {
                        String name=fetchRedirectUrl(voice);
                        sendPtt(qun,name,mtype);
                    }
                    else if(voice.startsWith("/")) {
                        sendPtt(qun,voice,mtype);
                    }
                }
            }
            if(quntext.startsWith("发送卡片{")) {
                if(qq.equals(uin)||读(qun,"代管",uin)==1||读("0","代管",uin)==1) {
                    String name=quntext.substring(4);
                    try {
                        JSONObject json = new JSONObject(name);
                        String app=json.get("app");
                        if(app.equals("")) {
                            sendText(data,"卡片内容出错了");
                        }
                        else {
                            sendCard(qun,name,mtype);
                        }
                    }
                    catch(e) {
                        sendText(data,"卡片解析出错了");
                    }
                }
            }
            if(quntext.startsWith("发送图片http")||quntext.startsWith("发送图片/")) {
                if(qq.equals(uin)||读(qun,"代管",uin)==1||读("0","代管",uin)==1) {
                    String image=quntext.substring(4);
                    if(image.startsWith("http")) {
                        sendImg(qun,fetchRedirectUrl(image),mtype);
                    }
                    else if(image.startsWith("/")) {
                        sendImg(qun,image,mtype);
                    }
                }
            }
            if(quntext.startsWith("发送视频http")) {
                try {
                    if(qq.equals(uin)||读(qun,"代管",uin)==1||读("0","代管",uin)==1) {
                        sendVideo(mtype,qun,fetchRedirectUrl(quntext.substring(4)));
                    }
                }
                catch(e) {
                    sendText(data,"错误:"+e);
                }
            }
            if(quntext.startsWith("发送视频/")) {
                try {
                    if(qq.equals(uin)||读(qun,"代管",uin)==1||读("0","代管",uin)==1) {
                        sendVideo(qun,quntext.substring(4),mtype);
                    }
                }
                catch(e) {
                    sendText(data,"错误:"+e);
                }
            }
            if(quntext.equals("复制音乐ck")) {
                if(qq.equals(uin)) {
                    String cookie=文字("0","音乐ck","cookie");
                    if(cookie.equals("")||!isMusicLogin(cookie)) {
                        cookie=accountlogin();
                        写("0","音乐ck","cookie",cookie);
                    }
                    PastetoClipboard("cookie",cookie);
                }
            }
            if(quntext.startsWith("复制GTK")) {
                if(qq.equals(uin)) {
                    String gm=quntext.substring(5);
                    if(gm.equals("")) {
                        sendText(data,"不是，你怎么连地址都不输入！" );
                    }
                    else {
                        String pskey=getPskey(gm);
                        String GTK=GetGTK(pskey)+"";
                        PastetoClipboard("cookie",GTK);
                    }
                }
            }
            if(quntext.startsWith("复制Pskey")) {
                if(qq.equals(uin)) {
                    String gm=quntext.substring(7);
                    if(gm.equals("")) {
                        sendText(data,"不是，你怎么连地址都不输入！" );
                    }
                    else {
                        String pskey=getPskey(gm);
                        PastetoClipboard("cookie",pskey);
                    }
                }
            }
            if(quntext.equals("复制Bkn")) {
                if(qq.equals(uin)) {
                    String bkn=getBkn(skey)+"";
                    PastetoClipboard("cookie",bkn);
                }
            }
            if(quntext.startsWith("执行代码")) {
                if(qq.equals(uin)||读("0","代管",uin)==1) {
                    try {
                        String text=quntext.substring(4);
                        this.interpreter.set("data", data);
                        this.interpreter.set("msg", data.originMsg);
                        this.interpreter.set("qun", qun);
                        this.interpreter.set("uin", uin);
                        this.interpreter.set("mtype", mtype);
                        this.interpreter.set("msgtype", msgtype);
                        this.interpreter.set("qq", myUin);
                        this.interpreter.eval(text,"eval stream");
                        //sendText(data,"执行完毕");
                    }
                    catch(e) {
                        sendText(data,"出错啦"+e);
                    }
                }
            }
            if(quntext.equals("刷新音乐ck")) {
                if(qq.equals(uin)||读("0","代管",uin)==1) {
                    cookie=accountlogin();
                    写("0","音乐ck","cookie",cookie);
                    String menu="刷新音乐ck成功";
                    sendText(data,menu);
                }
            }
            if(quntext.equals("复制Skey")) {
                if(qq.equals(uin)) {
                    PastetoClipboard("cookie",getSkey());
                }
            }
            if(quntext.equals("赞助作者")) {
                if(qq.equals(uin)||读(qun,"代管",uin)==1||读("0","代管",uin)==1) {
                    sendMsg(qun,"您的赞助是我们团队的更新动力！[pic="+myWang+"zan/pic/qq.png][pic="+myWang+"zan/pic/wx.png]有好的意见和建议欢迎反馈",mtype);
                }
            }
            
            if(quntext.equals("开关设置")) {
                if(qq.equals(uin)) {
                    kgsz(qun,mtype);
                }
            }
        }
    }
}
public void Callback_OnRawMsg(Object Yu) {
    new Thread(new Runnable() {
        public void run() {
            Object data = getData();
            data.put(Yu,"OriginMsg");
            originMsg(data);
        }
    }
    ).start();
}
public void originMsg(Object data) {
    String quntext = data.quntext;
    String qun = data.qun;
    String uin = data.uin;
    String qq=myUin;
    int mtype=data.mtype;
    int msgtype=data.msgtype;
    new Thread(new Runnable() {
        public void run() {
            if(data.msgtype == 1 && !data.uin.equals(myUin) && data.mtype ==1&&data.originMsg.subMsgType == 1025)
            {
                if(读("0","屏蔽自动回复","开关")==1) {
                    ArrayList MsgidList=new ArrayList();
                    MsgidList.add(data.msgid);
                    setMsgRead(data.qun,data.mtype);
                    deleteTroopMsg(data.qun,MsgidList,data.mtype);
                }
            }
            
    if(msgtype == 8 &&uin != null && !uin.isEmpty() && !uin.equals(qq) && !uin.equals("2854196310") && !uin.equals("0"))
    {
        if(读(qun, "撤回灰字", "开关") == 1)
        {
            int a = 读(qun, "白名单",uin);
            int b = 读("0", "白名单",uin);
            int c = 读(qun, "代管",uin);
            int d = 读("0", "代管",uin);
            if(uin.equals(qq) || a==1 ||b==1||c==1||d==1)
            {}
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
                String menu = uin+"被撤回了一条消息，原因:开启了撤回灰字提示";
                sendText(data, menu);
            }
        }
    }
    
        }
    }
    ).start();
}