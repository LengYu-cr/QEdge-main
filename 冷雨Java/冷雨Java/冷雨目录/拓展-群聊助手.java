public void 群聊助手(Object Yu) {
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
                if(判断群(qun,mtype)==1) {
                    if(读("0","代管",uin)==1||读(qun,"代管",uin)==1||qq.equals(uin)) {
                        if(quntext.equals("群聊助手")) {
                            sendText(data,"群聊助手:\n邀请管家 移除管家\n本群信息 群内排行\n本群星级 查询龙王\n抽群字符 管家发送\n查本群好友 群列表\n开/关撤回链接\n开/关撤回二维码\n开/关禁止发口令\n查看入群欢迎\n自助问答列表\n荣誉列表@QQ\n查共同群+QQ\n查共同成员+qun\n设置群地点+内容\n设置入群欢迎+内容\n一键已读所有群\n一键群打卡\n一键群免打扰/通知/小助手/屏蔽\n查看本群异常用户");
                        }
                        if("查本群好友".equals(quntext)) {
                            int i=0;
                            String result="";
                            if(Module.equals("Serendipity") || Module.equals("模了个块")) {
                                for(HashMap info:getGroupMemberList(qun)) {
                                    String qr=info.get("uin");
                                    IFriendDataService Info = app.getRuntimeService(IFriendDataService.class);
                                    boolean m=Info.isFriend(qr);
                                    if(m) {
                                        result+=(i+1)+"、"+qr+"("+getUserName(qr)+")\n";
                                        i++;
                                    }
                                }
                            }
                            else if(Module.equals("QStory")) {
                                Object st=getGroupMemberList(qun);
                                for(Object b:st)
                                {
                                    String qr=b.UserUin;
                                    IFriendDataService Info = app.getRuntimeService(IFriendDataService.class);
                                    boolean m=Info.isFriend(qr);
                                    if(m) {
                                        result+=(i+1)+"、"+qr+"("+getUserName(qr)+")\n";
                                        i++;
                                    }
                                }
                            }else if(Module.equals("QFun")) {
                                Object st=getGroupMemberList(qun);
                                for(Object b:st)
                                {
                                    String qr=b.uin;
                                    IFriendDataService Info = app.getRuntimeService(IFriendDataService.class);
                                    boolean m=Info.isFriend(qr);
                                    if(m) {
                                        result+=(i+1)+"、"+qr+"("+getUserName(qr)+")\n";
                                        i++;
                                    }
                                }
                            }
                            sendText(data,"查询到本群好友如下:\n\n"+result);
                        }
                        if(quntext.matches("查共同成员[0-9]+")) {
                            String qr=quntext.substring(5);
                            String result="";
                            sendText(data,"查询中，请稍等...");
                            if(Module.equals("Serendipity") || Module.equals("模了个块")) {
                                List list=new ArrayList();
                                List list1=new ArrayList();
                                for(HashMap into:getGroupMemberList(qun)) {
                                    list.add(into.get("uin"));
                                }
                                for(HashMap into:getGroupMemberList(qr)) {
                                    list1.add(into.get("uin"));
                                }
                                list1.retainAll(list);
                                int i=0;
                                for(String uins:list1) {
                                    result+=(i+1)+"、"+getUserName(uins)+"("+uins+")\n";
                                    i++;
                                }
                            }
                            else if(Module.equals("QStory")) {
                                List list=new ArrayList();
                                List list1=new ArrayList();
                                Object list2=getGroupMemberList(qun);
                                Object list3=getGroupMemberList(qr);
                                for(Object b:list2) {
                                    list.add(b.UserUin);
                                }
                                for(Object b:list3) {
                                    list1.add(b.UserUin);
                                }
                                list1.retainAll(list);
                                int i=0;
                                for(String uins:list1) {
                                    result+=(i+1)+"、"+getUserName(uins)+"("+uins+")\n";
                                    i++;
                                }
                            }else if(Module.equals("QFun")) {
                                List list=new ArrayList();
                                List list1=new ArrayList();
                                Object list2=getGroupMemberList(qun);
                                Object list3=getGroupMemberList(qr);
                                for(Object b:list2) {
                                    list.add(b.uin);
                                }
                                for(Object b:list3) {
                                    list1.add(b.uin);
                                }
                                list1.retainAll(list);
                                int i=0;
                                for(String uins:list1) {
                                    result+=(i+1)+"、"+getUserName(uins)+"("+uins+")\n";
                                    i++;
                                }
                            }
                            sendText(data,"群"+qun+"与"+qr+"的共同好友如下:\n\n"+result);
                        }
                        if(quntext.matches("查共同群[0-9]+")) {
                            String qr=quntext.substring(4);
                            int i=0;
                            String result="";
                            sendText(data,"查询中，请稍等...");
                            if(Module.equals("Serendipity") || Module.equals("模了个块")) {
                                for(HashMap info:getGroupList()) {
                                    String Qun=info.get("group");
                                    for(HashMap infos:getGroupMemberList(Qun)) {
                                        String qrr=infos.get("uin");
                                        if(qrr.equals(qr)) {
                                            result+=(i+1)+"、"+Qun+"("+getGroupNames(Qun)+")\n";
                                            i++;
                                        }
                                    }
                                }
                            }
                            else if(Module.equals("QStory")) {
                                Object st1=getGroupList();
                                for(Object c:st1)
                                {
                                    String Qun=c.GroupUin;
                                    Object st=getGroupMemberList(Qun);
                                    for(Object b:st)
                                    {
                                        String qrr=b.UserUin;
                                        if(qrr.equals(qr)) {
                                            result+=(i+1)+"、"+Qun+"("+getGroupNames(Qun)+")\n";
                                            i++;
                                        }
                                    }
                                }
                            }else if(Module.equals("QFun")) {
                                Object st1=getGroupList();
                                for(Object c:st1)
                                {
                                    String Qun=c.group;
                                    Object st=getGroupMemberList(Qun);
                                    for(Object b:st)
                                    {
                                        String qrr=b.uin;
                                        if(qrr.equals(qr)) {
                                            result+=(i+1)+"、"+Qun+"("+getGroupNames(Qun)+")\n";
                                            i++;
                                        }
                                    }
                                }
                            }
                            sendText(data,"你与"+qr+"的共同群如下:\n\n"+result);
                        }
                        if(quntext.startsWith("荣誉列表@")) {
                            String result="";
                            String at=data.atList.get(0);
                            String cookie="uin=o"+qq+"; skey="+skey+"; p_uin=o"+qq+"; p_skey="+qunpskey;
                            String url=httpget("https://qun.qq.com/interactive/userhonor?gc="+qun+"&uin="+at+"&_wv=3&&_wwv=128",cookie);
                            int index = url.lastIndexOf("window.__INITIAL_STATE__=");
                            String text = url.substring(index + 25);
                            int rd = text.indexOf("};(");
                            String re = text.substring(0,rd+1);
                            JSONObject json = new JSONObject(re);
                            JSONArray medalList=json.getJSONArray("medalList");
                            for(int i=0;
                            i<medalList.length();
                            i++) {
                                JSONObject List=medalList.get(i);
                                String name=List.get("name");
                                String medal_desc=List.get("medal_desc");
                                String rule_desc=List.get("rule_desc");
                                result+="荣誉类型:"+name+"\n头衔类型:"+rule_desc+"\n头衔描述:"+medal_desc+"\n";
                            }
                            sendText(data,result);
                        }
                        /*
if(quntext.equals("一键清空群聊天记录")){
if(qq.equals(uin)||读("0","代管",uin)==1){
if(Module.equals("Serendipity") || Module.equals("模了个块")){
    for(HashMap info:getGroupList())
                ClearGroupChatRecords(info.get("group"));
sendText(data,"一键清空群聊天记录成功");
}else if(Module.equals("QStory")){
Object st=getGroupList();
for(Object b:st)
{
    String um=b.GroupUin;
        ClearGroupChatRecords(um);
}
sendText(data,"一键清空群聊天记录成功");
}
}
}
if(quntext.equals("清空本群聊天记录")){
if(qq.equals(uin)){
ClearGroupChatRecords(qun);
sendText(data,"清空本群聊天记录成功！");
}
}
if(quntext.equals("解散本群")){
if(qq.equals(uin)){
if(getAuthority(qun,qq).equals("群主")){
disbandGroup(qun,true);
sendText(data,"解散本群成功！");
}else{
String menu="QQ:"+uin+"\n我还不是群主";
sendText(data,menu);
}
}
}
if(quntext.equals("屏蔽本群消息")){
SetTroopMsgFilter(qun,3);
sendText(data,"设置成功！");
}
if(quntext.equals("取消本群免打扰")){
SetTroopMsgFilter(qun,1);
sendText(data,"设置成功！");
}
if(quntext.equals("设置本群免打扰")){
SetTroopMsgFilter(qun,4);
sendText(data,"设置成功！");
}
*/
                        if(quntext.equals("开撤回链接")) {
                            String menu=setQGroupManager(qun,1,1);
                            sendText(data,""+menu);
                        }
                        if(quntext.equals("关撤回链接")) {
                            String menu=setQGroupManager(qun,1,0);
                            sendText(data,""+menu);
                        }
                        if(quntext.equals("开撤回二维码")) {
                            String menu=setQGroupManager(qun,2,1);
                            sendText(data,""+menu);
                        }
                        if(quntext.equals("关撤回二维码")) {
                            String menu=setQGroupManager(qun,2,0);
                            sendText(data,""+menu);
                        }
                        if(quntext.equals("开禁止发口令")) {
                            String menu=setQGroupManager(qun,3,1);
                            sendText(data,""+menu);
                        }
                        if(quntext.equals("关禁止发口令")) {
                            String menu=setQGroupManager(qun,3,0);
                            sendText(data,""+menu);
                        }
                        if(quntext.equals("邀请管家")) {
                            String pskey=getPskey("qun.qq.com");
                            String menu=邀请移除管家(qun,qq,skey,pskey,1);
                            String menu="邀请管家:"+menu;
                            sendText(data,menu);
                        }
                        if(quntext.equals("移除管家")) {
                            String pskey=getPskey("qun.qq.com");
                            String menu=邀请移除管家(qun,qq,skey,pskey,0);
                            String menu="移除管家:"+menu;
                            String stype=读(ColdRainPath+"data/"+qun+"菜单模式.txt");
                            if(stype.equals("管家")) {
                                写(ColdRainPath+"data/"+qun+"菜单模式.txt","文字");
                                menu=menu+"\n已切换为文字模式";
                            }
                            sendText(data,menu);
                        }
                        if(quntext.startsWith("艾特管家")) {
                            if(!quntext.substring(4).equals("")) {
                                sendMsg(qun,"[atUin=2854196310]"+quntext.substring(4),2);
                            }
                            else {
                                sendMsg(qun,"[atUin=2854196310]滚出来",2);
                            }
                        }
                        if(quntext.equals("群内排行")) {
                            String result="";
                            String cookie="uin=o"+myUin+"; p_uin=o"+myUin+"; p_skey="+qunpskey+"; skey="+skey;
                            String url=httpget("https://qun.qq.com/active/rank/index?gc="+qun+"&_wwv=128",cookie);
                            int index = url.lastIndexOf("window.__INITIAL_STATE__=");
                            String text = url.substring(index + 25);
                            int rd = text.indexOf("}<");
                            String re = text.substring(0,rd+1);
                            JSONObject json = new JSONObject(re);
                            JSONArray rankCardList=json.getJSONArray("rankCardList");
                            for(int i=0;
                            i<rankCardList.length();
                            i++) {
                                JSONObject List=rankCardList.get(i);
                                String userInfo=List.getString("userInfo");
                                String name1=List.get("name");
                                JSONObject json1 = new JSONObject(userInfo);
                                JSONArray users=json1.getJSONArray("users");
                                for(int q=0;
                                q<users.length();
                                q++) {
                                    JSONObject List1=users.get(q);
                                    String QQ=List1.get("uin");
                                    String score=List1.get("score");
                                    String name=List1.get("name");
                                    result+=name1+"\n第"+(q+1)+"名:\nQQ:"+QQ+"\n昵称:"+name+"\n积分:"+score+"\n\n";
                                }
                            }
                            sendText(data,""+result);
                        }
                        if(quntext.equals("本群星级")) {
                            String menu=getGroupLevel(qun);
                            sendText(data,menu);
                        }
                        if(quntext.startsWith("管家发送")) {
                            if(getAuthority(qun,qq).equals("管理员")||getAuthority(qun,qq).equals("群主")) {
                                String text=quntext.substring(4);
                                String pskey=getPskey("qun.qq.com");
                                String xxx=sendGuanjia(qun,qq,skey,pskey,User(3),text.replaceAll("\\r\\n|\\n|\\r", "\\\\n"));
                            }
                            else {
                                String menu="发送失败\n"+qq+"没有本群管理权限";
                                sendText(data,menu);
                            }
                        }
                        if(quntext.equals("查询龙王")) {
                            //陌然
                            skey=getSkey();
                            pskey=getPskey("qun.qq.com");
                            String result2="";
                            String cookie="p_skey="+pskey+"; uin=o"+qq+"; skey="+skey+"; p_uin=o"+qq;
                            String result=httpget("https://qun.qq.com/interactive/honorlist?gc="+qun+"&type=1&_wv=3&_wwv=129",cookie);
                            int index = result.lastIndexOf("window.__INITIAL_STATE__=");
                            String text = result.substring(index + 25);
                            int rd = text.indexOf("};");
                            String re = text.substring(0,rd+1);
                            JSONObject json=new JSONObject(re);
                            if(json.getString("currentTalkative").equals("null"))
                            {
                                sendText(data,"该群还没有龙王");
                                return;
                            }
                            if(json.getString("currentTalkative").equals("{}"))
                            {
                                sendText(data,"pskey错误");
                                return;
                            }
                            String nick= json.getJSONObject("currentTalkative").getString("nick");
                            String day= json.getJSONObject("currentTalkative").getString("day_count");
                            JSONArray j=json.getJSONArray("talkativeList");
                            if(j.length()>5) q =5;
                            if(j.length()<5) q =j.length();
                            for(int i=0;
                            i<q;
                            i++) {
                                JSONObject y=j.get(i);
                                String name=y.get("name");
                                String qq=y.getString("uin");
                                String desc=y.getString("desc");
                                result2+="\n名:"+name+"\nQQ:"+qq+"\n共"+desc;
                            }
                            String menu="龙王:"+nick+"\n连续"+day+"天龙王\n\n历史获得成员"+result2+"\n只显示前五位(共"+j.length()+"位)";
                            sendText(data,menu);
                        }
                        if(quntext.equals("本群信息")) {
                            String result="";
                            String result1="";
                            String result2="";
                            String result3="";
                            String result4="";
                            TroopInfo info=findTroopInfo(qun);
                            String result5="进群问题:"+info.joinTroopQuestion+"\n进群答案:"+info.joinTroopAnswer;
                            String cookie="uin=o"+qq+"; skey="+skey+"; p_uin=o"+qq+"; p_skey="+qunpskey;
                            String url=httppost1("https://qun.qq.com/m/qun/activedata/active.html?_wv=3&_wwv=128&gc="+qun+"&src=2",cookie,"");
                            int index = url.lastIndexOf("window.__INITIAL_STATE__=");
                            String text = url.substring(index + 25);
                            int rd = text.indexOf("}<");
                            String re = text.substring(0,rd+1);
                            JSONObject json = new JSONObject(re);
                            String groupInfo=json.getString("groupInfo");
                            String msgInfo=json.getString("msgInfo");
                            String activeData=json.getString("activeData");
                            String memberData=json.getString("memberData");
                            String joinData=json.getString("joinData");
                            String exitData=json.getString("exitData");
                            String applyData=json.getString("applyData");
                            if(!groupInfo.equals("{}")&&!msgInfo.equals("{}")&&!activeData.equals("{}")&&!memberData.equals("{}")&&!joinData.equals("{}")&&!exitData.equals("{}")&&!applyData.equals("{}")) {
                                JSONObject json1 = new JSONObject(groupInfo);
                                //群信息
                                String createDate=json1.get("createDate");
                                //创建时间
                                String groupName=json1.get("groupName");
                                //群名
                                String groupMember=json1.optString("groupMember");
                                //群人数
                                JSONObject json2 = new JSONObject(msgInfo);
                                //发言信息
                                String total=json2.optString("total");
                                //今日发言总数
                                JSONArray dataList=json2.getJSONArray("dataList");
                                for(int i=0;
                                i<dataList.length();
                                i++) {
                                    JSONObject List=dataList.get(i);
                                    String date=List.get("date");
                                    String number=List.optString("number");
                                    result+="日期:"+date+"\n发言数据:"+number+"\n";
                                }
                                JSONObject json3 = new JSONObject(activeData);
                                //活跃数据
                                String ratio=json3.optString("ratio");
                                //今日活跃比率
                                String activeData1=json3.optString("activeData");
                                //今日人数
                                JSONArray dataList1=json3.getJSONArray("dataList");
                                for(int i=0;
                                i<dataList1.length();
                                i++) {
                                    JSONObject List1=dataList1.get(i);
                                    String date1=List1.get("date");
                                    String number1=List1.optString("number");
                                    result1+="日期:"+date1+"\n活跃人数:"+number1;
                                }
                                JSONObject json4 = new JSONObject(memberData);
                                //群成员数据
                                JSONArray dataList2=json4.getJSONArray("dataList");
                                for(int i=0;
                                i<dataList2.length();
                                i++) {
                                    JSONObject List2=dataList2.get(i);
                                    String date2=List2.get("date");
                                    String number2=List2.optString("number");
                                    result2+="日期:"+date2+"\n群人数:"+number2+"\n";
                                }
                                JSONObject json5 = new JSONObject(joinData);
                                //加群数据
                                JSONArray dataList3=json5.getJSONArray("dataList");
                                for(int i=0;
                                i<dataList3.length();
                                i++) {
                                    JSONObject List3=dataList3.get(i);
                                    String date3=List3.get("date");
                                    String number3=List3.optString("number");
                                    result3+="日期:"+date3+"\n加群人数:"+number3+"\n";
                                }
                                JSONObject json6 = new JSONObject(exitData);
                                //退群数据
                                JSONArray dataList4=json6.getJSONArray("dataList");
                                for(int i=0;
                                i<dataList4.length();
                                i++) {
                                    JSONObject List4=dataList4.get(i);
                                    String date4=List4.get("date");
                                    String number4=List4.optString("number");
                                    result4+="日期:"+date4+"\n退群人数:"+number4+"\n";
                                }
                                sendText(data,"群号:"+qun+"\n群名:"+groupName+"\n群人数:"+groupMember+"\n创建时间:"+createDate+"\n"+result5+"\n今日发言:"+total+"\n活跃比率:"+ratio+"\n\n发言数据:\n"+result+"\n活跃数据:\n"+result2+"\n进群数据:\n"+result3+"\n退群数据:\n"+result4);
                            }
                            else {
                                String result6="创建时间:"+timestampToDate(info.troopCreateTime*1000)+"\n群名:"+info.troopname+"\n群主:"+info.troopowneruin+"\n最大群人数:"+info.wMemberMax+"\n当前群人数:"+info.wMemberNum;
                                sendText(data,"抱歉，我还不是管理员，只能获取以下信息:\n\n"+result5+"\n"+result6);
                            }
                        }
                        /*
if(quntext.equals("开启本群匿名")){
String text=setGroupAnonymous(qun,1);
sendText(data,text);
}
if(quntext.equals("关闭本群匿名")){
String text=setGroupAnonymous(qun,0);
sendText(data,text);
}
*/
                        if(quntext.equals("查看入群欢迎")) {
                            String WelcomeMsg=getWelcomeMsg(qun);
                            sendText(data,WelcomeMsg);
                        }
                        if(quntext.startsWith("设置入群欢迎")) {
                            String WelcomeMsg=setWelcomeMsg(qun,quntext.substring(6));
                            sendText(data,WelcomeMsg);
                        }
                        
                        if (quntext.equals("查看本群异常用户")) {
                            if (qq.equals(uin) || 读(qun, "代管", uin) == 1 || 读("0", "代管", uin) == 1) {
                                getAbnormalUsersInfo(qun, new protoListener() {
                                    
                                    public boolean onResponse() {
                                        return true;
                                    }
            
                                    public void onSuccess(String cmd, JSONObject json) {
                                        try {
                                            String info = json.getString("info");
                                            sendText(data, info);
                                        } catch (Exception e) {
                                            sendText(data, "解析结果失败: " + e.getMessage());
                                        }
                                    }
            
                                    public void onFailure(String cmd, String error) {
                                        sendText(data, "获取失败: " + error);
                                    }
                                });
                            }
                        }
                        
                        if(quntext.startsWith("设置群地点")) {
                            if(JudgeMyPermissions(qun)){
                            setGroupLocation(qun,quntext.substring(5));
                            sendText(data,"设置成功");
                            }else{
                            sendText(data,"无权限");
                            }
                        }
                        
                        if(quntext.equals("自助问答列表")) {
                            String WelcomeMsg=getQuestionsList(qun);
                            sendText(data,""+WelcomeMsg);
                        }
                        if(quntext.equals("抽群字符")) {
                            String text=getLuckyDrawLottery(qun);
                            sendText(data,text);
                        }
                        /*
if(quntext.equals("今日群打卡列表")){
String result="";
String time=年月日();
String dataJson="{\"dayYmd\":\""+time.replace("-","")+"\",\"uid\":\""+qq+"\",\"groupId\":\""+qun+"\"}";
String cookie="uin=o"+qq+"; skey="+skey+"; p_uin=o"+qq+"; p_skey="+qunpskey;
String url=httppost1("https://qun.qq.com/v2/signin/trpc/GetGTKSignInRecord?g_tk="+GetGTKGTK(qunpskey),cookie,dataJson);
JSONObject json = new JSONObject(url);
String funcCode=json.optString("funcCode");
if(funcCode.equals("0")){
String response=json.getString("response");
JSONObject json1 = new JSONObject(response);
String daySigned=json1.getString("daySigned");
JSONObject json2 = new JSONObject(daySigned);
String dayTotalSignedUid=json2.optString("dayTotalSignedUid");//打卡总人数
String daySignedPage=json2.getString("daySignedPage");
JSONObject json3 = new JSONObject(daySignedPage);
JSONArray infos=json3.getJSONArray("infos");
        for(int q=0;q<infos.length();q++){
        JSONObject List=infos.get(q);
        String uid=List.get("uid");
        String uidGroupNick=List.get("uidGroupNick");
        long signedTimeStamp=Long.parseLong(List.get("signedTimeStamp"));
        String signedTime=timestampToDate(signedTimeStamp*1000);
        result+="("+(q+1)+")、QQ:"+uid+"\n昵称:"+uidGroupNick+"\n打卡时间:"+signedTime+"\n";}
sendText(data,result+"今日打卡总数:"+dayTotalSignedUid+"人\n");
}else{
sendText(data,"失败，请稍后重试");
}
}
*/
                        if(quntext.equals("群列表")) {
                            if(qq.equals(uin)||读("0","代管",uin)==1) {
                                String result="";
                                if(Module.equals("Serendipity") || Module.equals("模了个块")) {
                                    for(HashMap info:getGroupList()) {
                                        String um=info.get("group");
                                        String nm=info.get("groupName");
                                        result+=nm+"("+um+")\n";
                                    }
                                    String menu=result;
                                    sendText(data,menu);
                                }
                                else if(Module.equals("QStory")) {
                                    Object st=getGroupList();
                                    for(Object b:st)
                                    {
                                        String um=b.GroupUin;
                                        String nm=b.GroupName;
                                        result+=nm+"("+um+")\n";
                                    }
                                    String menu=result;
                                    sendText(data,menu);
                                }else if(Module.equals("QFun")) {
                                    Object st=getGroupList();
                                    for(Object b:st)
                                    {
                                        String um=b.group;
                                        String nm=b.groupName;
                                        result+=nm+"("+um+")\n";
                                    }
                                    String menu=result;
                                    sendText(data,menu);
                                }
                            }
                        }
                        if(quntext.equals("一键群打卡")) {
                            if(qq.equals(uin)||读("0","代管",uin)==1) {
                                if(Module.equals("Serendipity") || Module.equals("模了个块")) {
                                    for(HashMap info:getGroupList())
                                    groupClockIn(info.get("group"),qq);
                                    sendText(data,"一键打卡成功");
                                }
                                else if(Module.equals("QStory")) {
                                    Object st=getGroupList();
                                    for(Object b:st)
                                    {
                                        String um=b.GroupUin;
                                        groupClockIn(um,qq);
                                    }
                                    sendText(data,"一键打卡成功");
                                }else if(Module.equals("QFun")) {
                                    Object st=getGroupList();
                                    for(Object b:st)
                                    {
                                        String um=b.group;
                                        groupClockIn(um,qq);
                                    }
                                    sendText(data,"一键打卡成功");
                                }
                            }
                        }
                        if(quntext.equals("一键已读所有群")) {
                            if(qq.equals(uin)||读("0","代管",uin)==1) {
                                if(Module.equals("Serendipity") || Module.equals("模了个块")) {
                                    for(HashMap info:getGroupList())
                                    setMsgRead(info.get("group"),2);
                                    sendText(data,"已一键已读所有群");
                                }
                                else if(Module.equals("QStory")) {
                                    Object st=getGroupList();
                                    for(Object b:st)
                                    {
                                        String um=b.GroupUin;
                                        setMsgRead(um,2);
                                    }
                                    sendText(data,"已一键已读所有群");
                                }else if(Module.equals("QFun")) {
                                    Object st=getGroupList();
                                    for(Object b:st)
                                    {
                                        String um=b.group;
                                        setMsgRead(um,2);
                                    }
                                    sendText(data,"已一键已读所有群");
                                }
                            }
                        }
                        if (quntext.matches("一键群(免打扰|通知|屏蔽|小助手)")) {
                            if (qq.equals(uin) || 读("0", "代管", uin) == 1) {
                                String text = quntext.substring(3);
                                int type = 0;
                                switch (text) {
                                    case "免打扰":
                                    type = 1;
                                    break;
                                    case "通知":
                                    type = 0;
                                    break;
                                    case "小助手":
                                    type = 2;
                                    break;
                                    case "屏蔽":
                                    type = 3;
                                    break;
                                }
                                if (Module.equals("Serendipity") || Module.equals("模了个块")) {
                                    for (HashMap info : getGroupList()) {
                                        setGroupMsgMask(info.get("group"), type);
                                    }
                                    sendText(data, "一键群" + text + "成功");
                                }
                                else if (Module.equals("QStory")) {
                                    Object st = getGroupList();
                                    for (Object b : st) {
                                        String um = b.GroupUin;
                                        setGroupMsgMask(um, type);
                                    }
                                    sendText(data, "一键群" + text + "成功");
                                }else if (Module.equals("QFun")) {
                                    Object st = getGroupList();
                                    for (Object b : st) {
                                        String um = b.group;
                                        setGroupMsgMask(um, type);
                                    }
                                    sendText(data, "一键群" + text + "成功");
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
public String getLuckyDrawLottery(String qun)
{
    String dataJson = "{\"group_code\":" + qun + "}";
    String url = "https://qun.qq.com/v2/luckyword/proxy/domain/qun.qq.com/cgi-bin/group_lucky_word/draw_lottery?bkn=" + GetGTK(skey);
    String cookie = "uin=o" + myUin + "; skey=" + getSkey() + "; p_uin=o" + myUin + "; p_skey=" + getPskey("qun.qq.com");
    String post = httppost4(url, dataJson, cookie);
    JSONObject json = new JSONObject(post);
    int retcode = json.getInt("retcode");
    if(retcode == 0)
    {
        String data1 = json.getString("data");
        if(data1.equals("{}"))
        {
            return "恭喜你，抽到了空气！";
        }
        else
        {
            JSONObject json1 = new JSONObject(data1);
            String word_info1 = json1.getString("word_info");
            JSONObject json2 = new JSONObject(word_info1);
            String word_info2 = json2.getString("word_info");
            JSONObject json3 = new JSONObject(word_info2);
            String wording = json3.getString("wording");
            //字符名
            String word_desc = json3.getString("word_desc");
            //描述
            return "字符:" + wording + "\n寓意:" + word_desc;
        }
    }
    else if(retcode == 11004)
    {
        return "超出今日抽字符限制";
    }
    else if(retcode == 11001)
    {
        return "本群幸运字符已被关闭";
    }
    else
    {
        String msg = json.get("msg");
        return msg;
    }
}
public String httppost4(String urlPath, String data, String cookie)
{
    StringBuffer buffer = new StringBuffer();
    InputStreamReader isr = null;
    try
    {
        URL url = new URL(urlPath);
        uc = (HttpURLConnection) url.openConnection();
        uc.setDoInput(true);
        uc.setDoOutput(true);
        uc.setConnectTimeout(2000000);
        // 设置连接主机超时（单位：毫秒）
        uc.setReadTimeout(2000000);
        // 设置从主机读取数据超时（单位：毫秒）
        uc.setRequestMethod("POST");
        uc.setRequestProperty("Host", "qun.qq.com");
        uc.setRequestProperty("Connection", "keep-alive");
        uc.setRequestProperty("Content-Length", "24");
        uc.setRequestProperty("Accept", "application/json, text/plain,*/*");
        uc.setRequestProperty("qname-service", "976321:131072");
        uc.setRequestProperty("qname-space", "Production");
        uc.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 13; V2166BA Build/TP1A.220624.014; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/109.0.5414.86 MQQBrowser/6.2 TBS/046715 Mobile Safari/537.36 V1_AND_SQ_8.9.83_4680_YYB_D QQ/8.9.83.12605 NetType/WIFI WebP/0.3.0 AppId/537178657 Pixel/1080 StatusBarHeight/100 SimpleUISwitch/0 QQTheme/1000 StudyMode/0 CurrentMode/0 CurrentFontScale/1.0 GlobalDensityScale/0.90000004 AllowLandscape/false InMagicWin/0");
        uc.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
        uc.setRequestProperty("Origin", "https://qun.qq.com");
        uc.setRequestProperty("Sec-Fetch-Site", "same-origin");
        uc.setRequestProperty("Sec-Fetch-Mode", "cors");
        uc.setRequestProperty("Sec-Fetch-Dest", "empty");
        uc.setRequestProperty("Referer", "https://qun.qq.com/v2/luckyword/index?qunid=641486099&_wv=67108865&_nav_txtclr=FFFFFF&_wvSb=0&source=enter");
        uc.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9,en-US;q=0.8,en;q=0.7");
        uc.setRequestProperty("Cookie", "" + cookie);
        uc.getOutputStream().write(data.getBytes("UTF-8"));
        uc.getOutputStream().flush();
        uc.getOutputStream().close();
        isr = new InputStreamReader(uc.getInputStream(), "utf-8");
        BufferedReader reader = new BufferedReader(isr);
        //缓冲
        String line;
        while((line = reader.readLine()) != null)
        {
            buffer.append(line + "\n");
        }
    }
    catch(Exception e)
    {
        e.printStackTrace();
    }
    finally
    {
        try
        {
            if(null != isr)
            {
                isr.close();
            }
        }
        catch(IOException e)
        {
            Toast("错误:\n" + e);
        }
    }
    if(buffer.length() == 0) return buffer.toString();
    buffer.delete(buffer.length() - 1, buffer.length());
    return buffer.toString();
}
public String qunGET(String url, String cookie)
{
    StringBuffer buffer = new StringBuffer();
    InputStreamReader isr = null;
    try
    {
        URL urlObj = new URL(url);
        URLConnection uc = urlObj.openConnection();
        uc.setRequestProperty("Cookie", "" + cookie);
        uc.setRequestProperty("Host", "web.qun.qq.com");
        uc.setRequestProperty("qname-space", "Production");
        uc.setRequestProperty("qname-service", "1434753:65536");
        uc.setConnectTimeout(10000);
        uc.setReadTimeout(10000);
        isr = new InputStreamReader(uc.getInputStream(), "utf-8");
        BufferedReader reader = new BufferedReader(isr);
        String line;
        while((line = reader.readLine()) != null)
        {
            buffer.append(line + "\n");
        }
    }
    catch(Exception e)
    {
        e.printStackTrace();
    }
    finally
    {
        try
        {
            if(null != isr)
            {
                isr.close();
            }
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
    }
    if(buffer.length() == 0) return buffer.toString();
    buffer.delete(buffer.length() - 1, buffer.length());
    return buffer.toString();
}
public String setWelcomeMsg(String qun, String text)
{
    String pskey = getPskey("qun.qq.com");
    String cookie = "uin=o0" + myUin + "; skey=" + skey + "; p_uin=o0" + myUin + "; p_skey=" + pskey;
    String url = qunGET("https://web.qun.qq.com/qunrobot/proxy/domain/qun.qq.com/cgi-bin/qunrobots/welcome_set?bkn=" + GetGTK(skey) + "&gc=" + qun + "&welcome_msg=" + text, cookie);
    try
    {
        JSONObject json = new JSONObject(url);
        String retcode = json.optString("retcode");
        if(retcode.equals("0")) xx = "设置成功！";
        else if(retcode.equals("251006")) xx = "设置失败，不是群" + qun + "的群主或管理员";
        else if(retcode.equals("100000")) xx = "设置失败，请检查cookie是否失效";
        else xx = "设置失败" + json.get("msg");
        return xx;
    }
    catch(e)
    {
        return "设置失败" + e;
    }
}
public String getWelcomeMsg(String qun)
{
    try {
        String cookie = "uin=o" + myUin + "; p_uin=o" + myUin + "; p_skey=" + qunpskey + "; skey=" + skey;
        String url = httpget("https://web.qun.qq.com/qunrobot/newbird?gc=" + qun + "&r_uin=2854196310&f_id=40&_wv=2&_wwv=128", cookie);
        int index = url.lastIndexOf("window.__INITIAL_STATE__=");
        String text = url.substring(index + 25);
        int rd = text.indexOf("}}<");
        String re = text.substring(0, rd + 2);
        JSONObject json = new JSONObject(re);
        String info = json.getString("info");
        JSONObject json1 = new JSONObject(info);
        String data1 = json1.getString("data");
        JSONObject json2 = new JSONObject(data1);
        String welcome_data = json2.getString("welcome_data");
        JSONObject json3 = new JSONObject(welcome_data);
        String welcome_msg = json3.getString("welcome_msg");
        String pic_url = json3.getString("pic_url");
        String set_uin = json2.getJSONObject("last_modify").getString("uin");
        String set_name = json2.getJSONObject("last_modify").getString("nick");
        if(welcome_msg.equals(""))
        {
            return "群" + qun + "暂未设置入群欢迎";
        }
        else
        {
            if(pic_url.startsWith("https://")||pic_url.startsWith("http://")) {
                pic_url="[pic="+pic_url+"]";
            }
            return "当前入群欢迎:" + welcome_msg+pic_url+"\n设置人:"+set_uin+"("+set_name+")";
        }
    }
    catch(e) {
        return "出错了"+e;
    }
}
public String setGroupAnonymous(String qun, int type)
{
    String cookie = "uin=" + myUin + "; skey=" + skey + "; p_uin=" + myUin + "; p_skey=" + qqweb;
    String url = httpget("https://qqweb.qq.com/c/anonymoustalk/set_anony_switch?value=" + type + "&group_code=" + qun + "&src=qinfo_v3&bkn=" + GetGTK(skey), cookie);
    JSONObject json = new JSONObject(url);
    String code = json.optString("cgicode");
    if(code.equals("0")) return "设置成功";
    else if(code.equals("100021")) return "Bkn错误";
    else if(code.equals("100000")) return "cookie失效";
}
public String setQGroupManager(String qun, int type, int switches)
{
    //设置Q群管家，type=1时是撤回链接，2时是撤回二维码，3时是禁止发口令红包；switches=1时为开启=0时为关闭
    String cookie = "uin=o" + myUin + "; p_uin=o" + myUin + "; p_skey=" + qunpskey + "; skey=" + skey;
    String menu = httppost("https://web.qun.qq.com/qunrobot/proxy/domain/app.qun.qq.com/cgi-bin/guanjia_robot/chat_manage/set_switch?bkn=" + getBkn(skey), "" + cookie, "word_type=" + type + "&switch_status=" + switches + "&group_code=" + qun + "&bkn=" + getBkn(skey), "application/x-www-form-urlencoded");
    JSONObject json = new JSONObject(menu);
    int retcode = json.getInt("retcode");
    if(retcode==0)
    {
        return "设置成功";
    }
    else if(retcode==21010)
    {
        return "Q群管家不是管理员";
    }
    else if(retcode == 11002)
    {
        return "你不是管理";
    }
    else if(retcode == 21000) {
        return "Q群管家不存在";
    }
    else
    {
        return "设置失败" + json.getString("retmsg");
    }
}
public String webGET(String url, String Cookie)
{
    StringBuffer buffer = new StringBuffer();
    InputStreamReader isr = null;
    try
    {
        URL urlObj = new URL(url);
        URLConnection uc = urlObj.openConnection();
        uc.setRequestProperty("Cookie", Cookie);
        uc.setRequestProperty("Host", "qqweb.qq.com");
        uc.setRequestProperty("Referer", "https://qqweb.qq.com/m/business/qunlevel/index.html?gc=464695369&from=0&_wv=1027");
        uc.setRequestProperty("User-agent", "Mozilla/5.0 (Linux; Android 11; Redmi K30 Build/RKQ1.200826.002; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/77.0.3865.120 MQQBrowser/6.2 TBS/045710 Mobile Safari/537.36 V1_AND_SQ_8.8.5_1858_YYB_D A_8080500 QQ/8.8.5.5570 NetType/WIFI WebP/0.3.0 Pixel/1080 StatusBarHeight/96 SimpleUISwitch/0 QQTheme/1046369 InMagicWin/0 StudyMode/0");
        uc.setConnectTimeout(10000);
        uc.setReadTimeout(10000);
        isr = new InputStreamReader(uc.getInputStream(), "utf-8");
        BufferedReader reader = new BufferedReader(isr);
        String line;
        while((line = reader.readLine()) != null)
        {
            buffer.append(line + "\n");
        }
    }
    catch(Exception e)
    {
        e.printStackTrace();
    }
    finally
    {
        try
        {
            if(null != isr)
            {
                isr.close();
            }
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
    }
    if(buffer.length() == 0) return buffer.toString();
    buffer.delete(buffer.length() - 1, buffer.length());
    return buffer.toString();
}
public String getGroupLevel(String qun)
{
    try
    {
        String cookie = "uin=" + myUin + "; skey=" + skey + "; p_uin=" + myUin + "; p_skey=" + qqweb;
        String url = webGET("https://qqweb.qq.com/c/activedata/get_credit_level_info?bkn=" + GetGTK(skey) + "&uin=" + myUin + "&gc=" + qun, cookie);
        JSONObject json1 = new JSONObject(url);
        String info = json1.getString("info");
        JSONObject json = new JSONObject(info);
        String group_name = json.optString("group_name").replace("$", "").replace("&lt;", "<").replace("&gt;", ">");
        String group_owner = json.optString("group_owner");
        String uiGroupLevel = json.optString("uiGroupLevel");
        if(uiGroupLevel.equals("5")) uiGroupLevel = "⭐⭐⭐⭐⭐";
        if(uiGroupLevel.equals("4")) uiGroupLevel = "⭐⭐⭐⭐";
        if(uiGroupLevel.equals("3")) uiGroupLevel = "⭐⭐⭐";
        if(uiGroupLevel.equals("2")) uiGroupLevel = "⭐⭐";
        if(uiGroupLevel.equals("1")) uiGroupLevel = "⭐";
        return "群名:" + group_name + "\n群主:" + group_owner + "\n群星:" + uiGroupLevel;
    }
    catch(e)
    {
        return "";
    }
}