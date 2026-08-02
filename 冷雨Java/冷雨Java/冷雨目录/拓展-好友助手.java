public void 好友助手(Object Yu) {
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
            if(mtype==1) {
                if(!quntext.equals("")&&!qq.equals(uin)) {
                    if(读("0","自动回复","开关")==1) {
                        String text=文字("0","自动回复","内容");
                        if(!text.equals("")) {
                            if(text.startsWith("[语音")) {
                                String cnm=text.substring(3);
                                String cnmm=cnm.replace("]","");
                                sendPtt(qun,cnmm,1);
                            }
                            else if(text.startsWith("[访问")) {
                                String cnm=text.substring(3);
                                String cnmm=cnm.replace("]","");
                                String cnmmb=get(cnmm);
                                sendMsg(qun,cnmmb,1);
                            }
                            else if(text.startsWith("[图片")) {
                                String cnm=text.substring(3);
                                String cnmm=cnm.replace("]","");
                                sendImg(qun,cnmm,1);
                            }
                            else if(text.equals("[拍一拍]")) {
                                sendPaiYiPai(qun,uin,mtype);
                            }
                            else if(text.equals("[GPT]")) {
                                sendGPT(data);
                            }
                            else if(text.startsWith("Java:")) {
                                String cnm = text.substring(5);
                                this.interpreter.eval("String qq=\""+myUin+"\";String uin=\""+uin+"\";String qun=\""+qun+"\";"+cnm,"eval stream");
                            }
                            else {
                                String msg=text.replace("[at]","[艾特消息]");
                                msg=msg.replace("[qq]",qq);
                                msg=msg.replace("[uin]",uin);
                                msg=msg.replace("[qun]",qun);
                                msg=msg.replace("[Name]",getUserName(uin));
                                msg=msg.replace("[GroupName]",data.originMsg.sendNickName);
                                msg=msg.replace("[time]",timestampToDate(data.originMsg.msgTime*1000));
                                msg=msg.replace("[图片","\n[pic=");
                                msg = msg.replace("[GroupMemberCount]", 2+"");
                                String menu=msg;
                                sendMsg(qun,menu,1);
                            }
                        }
                    }
                }
                if(msgtype==2&&!qq.equals(uin)) {
                    if(读("0","闪照破解","开关")==1) {
                        try {
                            Object dataMsg=data.originMsg;
                            String url="";
                            for(MsgElement msgElement:dataMsg.elements) {
                                if(msgElement.picElement!=null) {
                                    String a=msgElement.picElement.toString();
                                    //String md5HexStr=msgElement.picElement.md5HexStr.toUpperCase();
                                    //Toast(a);
                                    if(a.contains("isFlashPic=true,"))
                                    url=findPicUrl(msgElement.picElement,mtype);
                                }
                            }
                            if(!url.equals(""))sendMsg(myUin,"收到了闪照！\n好友："+qun+"\n昵称："+data.originMsg.sendNickName+"\n[pic="+url+"]",1);
                        }
                        catch(e) {
                        }
                    }
                }
                String quntextUpper=quntext.toUpperCase();
                if(quntextUpper.startsWith("备注")||quntextUpper.startsWith("BZ")) {
                    if(读("0","自动备注","开关")==1) {
                        if(qq.equals(uin)) {
                            setFriendRemark(qun,quntext.substring(2));
                            sendText(data,"已为ta备注成功");
                        }
                        else {
                            setFriendRemark(uin,quntext.substring(2));
                            sendText(data,"已为您备注成功");
                        }
                    }
                }
                if(读("0","代管",uin)==1||qq.equals(uin)) {
                    if(quntext.equals("好友助手")) {
                        sendText(data,"好友助手:\n开启/关闭闪照破解\n开启/关闭自动回复\n开启/关闭屏蔽自动回复\n设置自动回复+内容\n开启/关闭自动备注\nBZ/bz/备注+内容(自动备注)\n检查被封好友\n一键为好友点赞\n设置昵称+内容\nTip:自动回复支持变量=>查看变量");
                    }
                    if(quntext.equals("开启闪照破解")) {
                        String menu="开启成功";
                        写("0","闪照破解","开关",1);
                        sendText(data,menu);
                    }
                    if(quntext.equals("关闭闪照破解")) {
                        String menu="关闭成功";
                        写("0","闪照破解","开关",0);
                        sendText(data,menu);
                    }
                    if(quntext.equals("开启自动备注")) {
                        String menu="开启成功";
                        写("0","自动备注","开关",1);
                        sendText(data,menu);
                    }
                    if(quntext.equals("关闭自动备注")) {
                        String menu="关闭成功";
                        写("0","自动备注","开关",0);
                        sendText(data,menu);
                    }
                    if(quntext.equals("检查被封好友")) {
                        if(qq.equals(uin)||读("0","代管",uin)==1) {
                            try {
                                IFriendDataService iFriendDataService = app.getRuntimeService(IFriendDataService.class);
                                int i=0;
                                int q=0;
                                String result="";
                                ProfileData.onCreate(app);
                                sendText(data,"正在执行中...请耐心等待...");
                                for(Friends friends:iFriendDataService.getAllFriends()) {
                                    Object card = ProfileData.getProfileCard(friends.uin, true);
                                    if(card==null||!card.isForbidAccount) {
                                        Bundle bundle = new Bundle();
                                        bundle.putLong("selfUin", Long.parseLong(myUin));
                                        bundle.putLong("targetUin", Long.parseLong(friends.uin));
                                        bundle.putInt("comeFromType", 12);
                                        ProtocolService.requestProfileCard(bundle);
                                        card = ProfileData.getProfileCard(friends.uin, true);
                                    }
                                    if(card!=null) {
                                        if(card.isForbidAccount) {
                                            result+=(i+1)+"、"+getUserName(friends.uin)+"("+friends.uin+")\n";
                                            i++;
                                        }
                                    }
                                    else {
                                        q++;
                                    }
                                }
                                String nm=MakeTextPhoto("共"+i+"位好友被封禁(其中"+q+"人未检测成功)\n被封好友分别是：\n"+result,"","");
                                sendImg(qun,nm,1);
                                sc(nm);
                            }
                            catch(e) {
                                sendText(data,"失败"+e);
                            }
                        }
                    }
                    if(quntext.equals("一键为好友点赞")) {
                        try {
                            IFriendDataService iFriendDataService = app.getRuntimeService(IFriendDataService.class);
                            int i=1;
                            for(Friends friends:iFriendDataService.getAllFriends()) {
                                i++;
                                sendZan(friends.uin+"",10);
                            }
                            sendText(data,"已为"+i+"位好友点赞10次");
                        }
                        catch(e) {
                            sendText(data,"点赞失败"+e);
                        }
                    }
                    if(quntext.equals("开启屏蔽自动回复")) {
                        String menu="开启成功";
                        写("0","屏蔽自动回复","开关",1);
                        sendText(data,menu);
                    }
                    if(quntext.equals("关闭屏蔽自动回复")) {
                        String menu="关闭成功";
                        写("0","屏蔽自动回复","开关",0);
                        sendText(data,menu);
                    }
                    if(quntext.equals("开启自动回复")) {
                        String menu="开启成功";
                        写("0","自动回复","开关",1);
                        sendText(data,menu);
                    }
                    if(quntext.equals("关闭自动回复")) {
                        String menu="关闭成功";
                        写("0","自动回复","开关",0);
                        sendText(data,menu);
                    }
                    if(quntext.startsWith("设置自动回复")) {
                        String menu="设置成功";
                        写("0","自动回复","内容",quntext.substring(6));
                        sendText(data,menu);
                    }
                    if(quntext.startsWith("设置昵称")) {
                        changeMyName(quntext.substring(4),new protoListener() {
                        
                        public boolean onResponse(){
                            return true;
                        }
            
                        public void onSuccess(String cmd, JSONObject json) {
                        Toast("昵称修改成功");
                        sendText(data,"昵称修改成功");
                        }
            
                        public void onFailure(String cmd, String error) {
                        Toast("昵称修改失败: " + error);
                        sendText(data, "昵称修改失败: " + error);
                        }
                        });
                    }
                }
            }
        }
    }
    ).start();
}
import com.tencent.mobileqq.activity.AutoRemarkActivity;
import com.tencent.mobileqq.app.FriendListHandler;
import com.tencent.mobileqq.app.BusinessHandlerFactory;
public void setFriendRemark(String uin,String text) {
    //修改备注
    AutoRemarkActivity acc;
    if(acc==null) {
        BaseActivity.sTopActivity.runOnUiThread(new Runnable() {
            public void run() {
                acc=new AutoRemarkActivity();
            }
        }
        );
    }
    while(acc==null) {
    }
    //acc.f=uin;
    FriendListHandler l=(FriendListHandler) app.getBusinessHandler(BusinessHandlerFactory.FRIENDLIST_HANDLER);
    l.setFriendComment(uin,text, false);
}