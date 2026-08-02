public void 群发功能(Object Yu) {
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
            if(判断群(qun,mtype)==1) {
                if(读("0","代管",uin)==1||qq.equals(uin)) {
                    if(quntext.equals("群发功能")) {
                        String menu="群发功能:\n开始群发群+内容\n开始群发好友+内容\n设置群发延迟+ms(默认5000)\n添加群发白名单+群号\n删除群发白名单+群号\n添加好友白名单+QQ\n删除好友白名单+QQ\n群发白名单\n好友白名单\n加白本群(群)\n加白此人(好友)\nTips:设置内容时可使用以下几种形式:\nCard: => 发送卡片\nImg: => 发送图片\nPtt: => 发送语音";
                        sendText(data,""+menu);
                    }
                    if(quntext.startsWith("开始群发群")) {
                        int ms=读("群发功能","间隔时间","ms");
                        if(ms==0) ms=5000;
                        else ms=ms;
                        String text=quntext.substring(5);
                        String send_type="文字";
                        if(text.startsWith("Card:")) {
                            send_type="卡片";
                            text=text.substring(5);
                        }
                        else if(text.startsWith("Img:")) {
                            send_type="图片";
                            text=text.substring(4);
                        }
                        else if(text.startsWith("Ptt:")) {
                            text=text.substring(4);
                            send_type="语音";
                        }
                        if(text.isEmpty()) {
                            sendText(data,"未输入内容");
                        }
                        else {
                            sendText(data,"正在群发群中...请耐心等待ing...");
                            if(Module.equals("Serendipity") || Module.equals("模了个块")) {
                                for(HashMap info:getGroupList()) {
                                    String qr=info.get("group");
                                    if(读("群发功能","白名单",qr)!=1) {
                                        sendQUN(qr,text,2,send_type);
                                        Thread.sleep(ms);
                                    }
                                }
                            }
                            else if(Module.equals("QStory")) {
                                Object st=getGroupList();
                                for(Object b:st)
                                {
                                    String qr=b.GroupUin;
                                    if(读("群发功能","白名单",qr)!=1) {
                                        sendQUN(qr,text,2,send_type);
                                        Thread.sleep(ms);
                                    }
                                }
                            }else if(Module.equals("QFun")) {
                                Object st=getGroupList();
                                for(Object b:st)
                                {
                                    String qr=b.group;
                                    if(读("群发功能","白名单",qr)!=1) {
                                        sendQUN(qr,text,2,send_type);
                                        Thread.sleep(ms);
                                    }
                                }
                            }
                            sendText(data,"群发群已完成");
                        }
                    }
                    if(quntext.startsWith("开始群发好友")) {
                        int ms=读("群发功能","间隔时间","ms");
                        if(ms==0) ms=5000;
                        else ms=ms;
                        String text=quntext.substring(6);
                        String send_type="文字";
                        if(text.startsWith("Card:")) {
                            send_type="卡片";
                            text=text.substring(5);
                        }
                        else if(text.startsWith("Img:")) {
                            send_type="图片";
                            text=text.substring(4);
                        }
                        else if(text.startsWith("Ptt:")) {
                            text=text.substring(4);
                            send_type="语音";
                        }
                        if(text.isEmpty()) {
                            sendText(data,"未输入内容");
                        }
                        else {
                            sendText(data,"正在群发好友中...请耐心等待ing...");
                            IFriendDataService Info = app.getRuntimeService(IFriendDataService.class);
                            String result="";
                            int i=0;
                            for(Friends list:Info.getAllFriends()) {
                                qr=list.uin;
                                if(读("群发功能","白名单2",qr)!=1) {
                                    sendQUN(qr,text,1,send_type);
                                    Thread.sleep(ms);
                                    i++;
                                }
                            }
                            sendText(data,"群发好友已完成\n共群发"+i+"个好友");
                        }
                    }
                    if(quntext.equals("群发白名单")) {
                        String[] List=列表("群发功能","白名单");
                        String x="群发白名单列表有:";
                        String a=x;
                        long i=0;
                        for(String s:List) {
                            i++;
                            x=x+"\n"+i+"."+s;
                        }
                        if(x.equals(a)) x="目前还没有群发白名单哦～";
                        sendText(data,x);
                    }
                    if(quntext.equals("好友白名单")) {
                        String[] List=列表("群发功能","白名单2");
                        String x="好友白名单列表有:";
                        String a=x;
                        long i=0;
                        for(String s:List) {
                            i++;
                            x=x+"\n"+i+"."+s;
                        }
                        if(x.equals(a)) x="目前还没有好友白名单哦～";
                        sendText(data,x);
                    }
                    if(quntext.startsWith("设置群发延迟")) {
                        int text=Long.parseLong(quntext.substring(6));
                        写("群发功能","间隔时间","ms",text);
                        sendText(data,"设置成功！\n当前群发延迟:"+quntext.substring(6)+"毫秒");
                    }
                    if(quntext.matches("添加群发白名单[0-9]+")) {
                        String at=quntext.substring(7);
                        if(读("群发功能","白名单",at)==1) {
                            sendText(data,"已添加过该群为群发白名单～");
                        }
                        else {
                            写("群发功能","白名单",at,1);
                            sendText(data,"写入群发白名单成功～");
                        }
                    }
                    if(quntext.matches("删除群发白名单[0-9]+")) {
                        String at=quntext.substring(7);
                        if(读("群发功能","白名单",at)==0) {
                            sendText(data,"该群不是群发白名单～");
                        }
                        else {
                            写("群发功能","白名单",at,0);
                            sendText(data,"已删除群发白名单～");
                        }
                    }
                    if(quntext.equals("加白本群")) {
                        if(mtype==2) {
                            if(读("群发功能","白名单",qun)==1) {
                                sendText(data,"已添加过该群为群发白名单～");
                            }
                            else {
                                写("群发功能","白名单",qun,1);
                                sendText(data,"写入群发白名单成功～");
                            }
                        }
                        else {
                            sendText(data,"非群页面");
                        }
                    }
                    if(quntext.equals("加白此人")) {
                        if(mtype==1) {
                            if(读("群发功能","白名单2",qun)==1) {
                                sendText(data,"已添加过该群为好友群发白名单～");
                            }
                            else {
                                写("群发功能","白名单2",qun,1);
                                sendText(data,"写入好友群发白名单成功～");
                            }
                        }
                        else {
                            sendText(data,"非好友页面");
                        }
                    }
                    if(quntext.matches("添加好友白名单[0-9]+")) {
                        String at=quntext.substring(7);
                        if(读("群发功能","白名单2",at)==1) {
                            sendText(data,"已添加过该好友为好友白名单～");
                        }
                        else {
                            写("群发功能","白名单2",at,1);
                            sendText(data,"写入群发好友白名单成功～");
                        }
                    }
                    if(quntext.matches("删除好友白名单[0-9]+")) {
                        String at=quntext.substring(7);
                        if(读("群发功能","白名单2",at)==0) {
                            sendText(data,"该好友不是好友白名单～");
                        }
                        else {
                            写("群发功能","白名单2",at,0);
                            sendText(data,"已删除群发好友白名单～");
                        }
                    }
                }
            }
        }
    }
    ).start();
}
public void sendQUN(String qun,String text,int mtype,String send_type) {
    if(send_type.equals("文字"))sendMsg(qun,text,mtype);
    else if(send_type.equals("卡片"))sendCard(qun,text,mtype);
    else if(send_type.equals("图片"))sendImg(qun,text,mtype);
    else if(send_type.equals("语音"))sendPtt(qun,text,mtype);
}