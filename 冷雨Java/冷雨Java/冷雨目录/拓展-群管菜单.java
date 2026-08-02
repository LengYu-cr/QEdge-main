public void 群管菜单(Object Yu) {
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
                if(读("0","代管",uin)==1||读(qun,"代管",uin)==1||qq.equals(uin)) {
                    if(mtype==2) {
                        if(data.originMsg.msgType==9) {
                            if(quntext.equals("/ban")||quntext.equals("ban")) {
                                String u=data.originMsg.records.get(0).senderUin+"";
                                if(getAuthority(qun,myUin,u)) {
                                    if(读("0","代管",u)==1||读(qun,"代管",u)==1) {
                                        sendMsg(qun,"gun,代管你也"+quntext.replace("/","")+"？",2);
                                    }
                                    else if(qq.equals(u)) {
                                        sendMsg(qun,"gun,自己你也"+quntext.replace("/","")+"？",2);
                                    }
                                    else if(读("0","白名单",u)==1||读(qun,"白名单",u)==1) {
                                        sendMsg(qun,"gun,白名单你也"+quntext.replace("/","")+"？",2);
                                    }
                                    else {
                                        kickGroup(qun,u,true);
                                        sendMsg(qun,"ok,将不会再收到该用户入群申请",2);
                                    }
                                }
                                else {
                                    sendMsg(qun,"gun,权限不足",2);
                                }
                            }
                            if(quntext.equals("/kick")||quntext.equals("kick")) {
                                String u=data.originMsg.records.get(0).senderUin+"";
                                if(getAuthority(qun,myUin,u)) {
                                    if(读("0","代管",u)==1||读(qun,"代管",u)==1) {
                                        sendMsg(qun,"gun,代管你也"+quntext.replace("/","")+"？",2);
                                    }
                                    else if(qq.equals(u)) {
                                        sendMsg(qun,"gun,自己你也"+quntext.replace("/","")+"？",2);
                                    }
                                    else if(读("0","白名单",u)==1||读(qun,"白名单",u)==1) {
                                        sendMsg(qun,"gun,白名单你也"+quntext.replace("/","")+"？",2);
                                    }
                                    else {
                                        kickGroup(qun,u,false);
                                        sendMsg(qun,"ok",2);
                                    }
                                }
                                else {
                                    sendMsg(qun,"gun,权限不足",2);
                                }
                            }
                            if(quntext.equals("/禁言")||quntext.equals("/禁")||quntext.equals("/闭嘴")||quntext.equals("禁言")||quntext.equals("禁")||quntext.equals("闭嘴")) {
                                String u=data.originMsg.records.get(0).senderUin+"";
                                if(getAuthority(qun,myUin,u)) {
                                    if(读("0","代管",u)==1||读(qun,"代管",u)==1) {
                                        sendMsg(qun,"gun,代管你也"+quntext.replace("/","")+"？",2);
                                    }
                                    else if(qq.equals(u)) {
                                        sendMsg(qun,"gun,自己你也"+quntext.replace("/","")+"？",2);
                                    }
                                    else if(读("0","白名单",u)==1||读(qun,"白名单",u)==1) {
                                        sendMsg(qun,"gun,白名单你也"+quntext.replace("/","")+"？",2);
                                    }
                                    else {
                                        shutUp(qun,u,60*60);
                                        sendMsg(qun,"ok,已把Ta关进小黑屋1小时",2);
                                    }
                                }
                                else {
                                    sendMsg(qun,"gun,权限不足",2);
                                }
                            }
                            if(quntext.equals("/解禁")||quntext.equals("/解")||quntext.equals("/说话")||quntext.equals("说话")||quntext.equals("解禁")||quntext.equals("解")) {
                                String u=data.originMsg.records.get(0).senderUin+"";
                                if(getAuthority(qun,myUin,u)) {
                                    shutUp(qun,u,0);
                                    sendMsg(qun,"ok",2);
                                }
                                else {
                                    sendMsg(qun,"gun,权限不足",2);
                                }
                            }
                            if(quntext.equals("/撤回")||quntext.equals("撤回")) {
                                String u=data.originMsg.records.get(0).senderUin+"";
                                if(getAuthority2(qun,myUin,u)) {
                                //Toast("111");
                                    long ii=data.originMsg.msgId;
                                    for(MsgElement msgElement : data.originMsg.elements) {
                                        if(msgElement.replyElement!=null) {
                                            long i=msgElement.replyElement.replayMsgId;
                                            recallTroopMsg(data.originMsg.peerUid,i,mtype);
                                        }
                                    }
                                    if(qq.equals(uin)) {
                                        recallTroopMsg(data.originMsg.peerUid,ii,mtype);
                                    }
                                }
                                else {
                                    sendMsg(qun,"gun,权限不足",2);
                                }
                            }
                        }
                        if(quntext.equals("群管菜单")) {
                            String reply="\n[回复]";
                            String menu="群管菜单:"+
                            reply+"/ban 踢黑"+
                            reply+"/kick 踢"+
                            reply+"/闭嘴,/禁言,/禁 禁言一小时"+
                            reply+"/解禁,/说话,/解 解禁该成员"+
                            reply+"/撤回 撤回某人一条消息\n"+
                            "踢@QQ/+QQ\n"+
                            "踢黑@QQ/+QQ\n"+
                            "禁言@QQ/+QQ 时间\n"+
                            "全体禁言/解禁\n"+
                            "全禁/全解\n"+
                            "上管/下管@QQ\n"+
                            "解禁/解@QQ/+QQ\n"+
                            "禁言列表"+
                            "\nTip:\"/\"号可加可不加，功能仅代管/自身可用";
                            sendText(data,menu);
                        }
                        if(quntext.startsWith("禁言@")) {
                            for(String at : data.atList) {
                                int str=quntext.lastIndexOf(" ")+1;
                                long t=Long.parseLong(quntext.substring(str));
                                int time=t*60;
                                if(time>2592000) time = 2592000;
                                shutUp(qun,at,time);
                            }
                            String menu="ok";
                            sendText(data,menu);
                        }
                        if(quntext.matches("禁言[0-9]+ [0-9]+")) {
                            String at=quntext.split("言")[1];
                            long t=Long.parseLong(quntext.split(" ")[1]);
                            String aite=at.replace(" "+t,"");
                            int time=t*60;
                            if(time>2592000) time = 2592000;
                            shutUp(qun,aite,time);
                            String menu="ok";
                            sendText(data,menu);
                        }
                        if(quntext.startsWith("踢@")) {
                            for(String at : data.atList) {
                                kickGroup(qun,at,false);
                            }
                            String menu="ok";
                            sendText(data,menu);
                        }
                        if(quntext.startsWith("踢黑@")) {
                            for(String at : data.atList) {
                                kickGroup(qun,at,true);
                            }
                            String menu="ok,将不会再收到该用户入群申请";
                            sendText(data,menu);
                        }
                        if(quntext.matches("踢[0-9]+")) {
                            String at=quntext.substring(1);
                            kickGroup(qun,at,false);
                            String menu="ok";
                            sendText(data,menu);
                        }
                        if(quntext.matches("踢黑[0-9]+")) {
                            String at=quntext.substring(1);
                            kickGroup(qun,at,true);
                            String menu="ok,将不会再收到该用户入群申请";
                            sendText(data,menu);
                        }
                        if(quntext.matches("解禁[0-9]+")||quntext.matches("解[0-9]+")) {
                            String at=quntext.replace("解","").replace("禁","");
                            shutUp(qun,at,0);
                            String menu="ok";
                        }
                        if(quntext.startsWith("解禁@")||quntext.startsWith("解@")) {
                            for(String at : data.atList) {
                                shutUp(qun,at,0);
                            }
                            String menu="ok";
                            sendText(data,menu);
                        }
                        if(quntext.equals("禁言列表")) {
                            String result="";
                            int i=1;
                            if(Module.equals("Serendipity") || Module.equals("模了个块")) {
                                for(HashMap info:getProhibitList(qun)) {
                                    String um=info.get("user");
                                    String nm=info.get("userName");
                                    String time=timestampToDate(info.get("endTime"));
                                    result+=i+"、"+nm+"("+um+")\n结束时间:"+time+"\n";
                                    i++;
                                }
                                if(result.equals("\n")||result.equals("")) result="暂无禁言列表\n";
                                String menu=result+"Tips:发送『一键解禁』可一键解除禁言\n    发送『#解禁+序号』可选择解禁\n    发送『踢出禁言列表』可一键踢出\n";
                                sendText(data,menu);
                            }
                            else if(Module.equals("QStory")) {
                                Object st=getForbiddenList(qun);
                                for(Object b:st)
                                {
                                    String um=b.UserUin;
                                    String nm=b.UserName;
                                    String time=timestampToDate(b.Endtime);
                                    result+=i+"、"+nm+"("+um+")\n结束时间:"+time+"\n";
                                    i++;
                                }
                                if(result.equals("\n")||result.equals("")) result="暂无禁言列表\n";
                                String menu=result+"Tips:发送『一键解禁』可一键解除禁言\n    发送『#解禁+序号』可选择解禁\n    发送『踢出禁言列表』可一键踢出\n";
                                sendText(data,menu);
                            }else if(Module.equals("QFun")) {
                                Object st=getProhibitList(qun);
                                for(Object b:st)
                                {
                                    String um=b.user;
                                    String nm=b.userName;
                                    String time=timestampToDate(b.endTime);
                                    result+=i+"、"+nm+"("+um+")\n结束时间:"+time+"\n";
                                    i++;
                                }
                                if(result.equals("\n")||result.equals("")) result="暂无禁言列表\n";
                                String menu=result+"Tips:发送『一键解禁』可一键解除禁言\n    发送『#解禁+序号』可选择解禁\n    发送『踢出禁言列表』可一键踢出\n";
                                sendText(data,menu);
                            }
                        }
                        if(quntext.equals("踢出禁言列表")) {
                            int p=0;
                            if(Module.equals("Serendipity") || Module.equals("模了个块")) {
                                for(HashMap info:getProhibitList(qun)) {
                                    String um=info.get("user");
                                    p++;
                                    kickGroup(qun,um,false);
                                }
                            }
                            else if(Module.equals("QStory")) {
                                Object st=getForbiddenList(qun);
                                for(Object b:st)
                                {
                                    String um=b.UserUin;
                                    p++;
                                    kickGroup(qun,um,false);
                                }
                            }else if(Module.equals("QFun")) {
                                Object st=getProhibitList(qun);
                                for(Object b:st)
                                {
                                    String um=b.user;
                                    p++;
                                    kickGroup(qun,um,false);
                                }
                            }
                            sendText(data,"共踢出"+p+"个被禁言成员");
                        }
                        if(quntext.equals("一键解禁")) {
                            if(Module.equals("Serendipity") || Module.equals("模了个块")) {
                                try {
                                    for(HashMap info:getProhibitList(qun))
                                    shutUp(qun,info.get("user"),0);
                                    sendText(data,"已一键解禁所有被禁群员");
                                }
                                catch(e) {
                                    for(HashMap into:getGroupMemberList(qun))
                                    shutUp(qun,into.get("uin"),0);
                                    sendText(data,"已一键解禁所有被禁群员");
                                }
                            }
                            else if(Module.equals("QStory")) {
                                try {
                                    Object st=getForbiddenList(qun);
                                    for(Object b:st)
                                    {
                                        String um=b.UserUin;
                                        shutUp(qun,um,0);
                                    }
                                    sendText(data,"已一键解禁所有被禁群员");
                                }
                                catch(e) {
                                    Object st=getGroupMemberList(qun);
                                    for(Object b:st)
                                    {
                                        String um=b.UserUin;
                                        shutUp(qun,um,0);
                                    }
                                    sendText(data,"已一键解禁所有被禁群员");
                                }
                            }else if(Module.equals("QFun")) {
                                try {
                                    Object st=getProhibitList(qun);
                                    for(Object b:st)
                                    {
                                        String um=b.user;
                                        shutUp(qun,um,0);
                                    }
                                    sendText(data,"已一键解禁所有被禁群员");
                                }
                                catch(e) {
                                    Object st=getGroupMemberList(qun);
                                    for(Object b:st)
                                    {
                                        String um=b.uin;
                                        shutUp(qun,um,0);
                                    }
                                    sendText(data,"已一键解禁所有被禁群员");
                                }
                            }
                        }
                        if(quntext.matches("#解禁[0-9]+")) {
                            int at=Integer.parseInt(quntext.split("禁")[1]);
                            int p=1;
                            if(Module.equals("Serendipity") || Module.equals("模了个块")) {
                                Object list=getProhibitList(qun);
                                if(list.size()<at) {
                                    sendText(data,"序号错误");
                                }
                                else {
                                    for(HashMap info:list) {
                                        String um=info.get("user");
                                        if(at==p) {
                                            shutUp(qun,um,0);
                                            sendText(data,"已解禁"+um);
                                        }
                                        p++;
                                    }
                                }
                            }
                            else if(Module.equals("QStory")) {
                                Object st=getForbiddenList(qun);
                                if(st.size()<at) {
                                    sendText(data,"序号错误");
                                }
                                else {
                                    for(Object b:st)
                                    {
                                        String um=b.UserUin;
                                        if(at==p) {
                                            shutUp(qun,um,0);
                                            sendText(data,"已解禁"+um);
                                        }
                                        p++;
                                    }
                                }
                            }else if(Module.equals("QFun")) {
                                Object st=getProhibitList(qun);
                                if(st.size()<at) {
                                    sendText(data,"序号错误");
                                }
                                else {
                                    for(Object b:st)
                                    {
                                        String um=b.user;
                                        if(at==p) {
                                            shutUp(qun,um,0);
                                            sendText(data,"已解禁"+um);
                                        }
                                        p++;
                                    }
                                }
                            }
                        }
                        if(quntext.equals("假全禁")) {
                            if(qq.equals(uin)||读(qun,"代管",uin)==1||读("0","代管",uin)==1) {
                                if(getAuthority(qun,qq).equals("管理员")||getAuthority(qun,qq).equals("群主")) {
                                    shutUpAllFalse(qun);
                                    String menu="群"+qun+"\n开启假全禁成功！";
                                    sendText(data,menu);
                                }
                                else {
                                    String menu="群"+qun+"\n假全禁失败\n"+qq+"没有本群管理权限";
                                    sendText(data,menu);
                                }
                            }
                        }
                        if(quntext.equals("全体禁言")||quntext.equals("全禁")) {
                            if(qq.equals(uin)||读(qun,"代管",uin)==1||读("0","代管",uin)==1) {
                                if(getAuthority(qun,qq).equals("管理员")||getAuthority(qun,qq).equals("群主")) {
                                    shutUpAll(qun,true);
                                    String menu="群"+qun+"\n开启全体禁言成功！";
                                    sendText(data,menu);
                                }
                                else {
                                    String menu="群"+qun+"\n全体禁言失败\n"+qq+"没有本群管理权限";
                                    sendText(data,menu);
                                }
                            }
                        }
                        if(quntext.equals("全体解禁")||quntext.equals("全解")) {
                            if(qq.equals(uin)||读(qun,"代管",uin)==1||读("0","代管",uin)==1) {
                                if(getAuthority(qun,qq).equals("管理员")||getAuthority(qun,qq).equals("群主")) {
                                    shutUpAll(qun,false);
                                    String menu="群"+qun+"\n关闭全体解禁成功！";
                                    sendText(data,menu);
                                }
                                else {
                                    String menu="群"+qun+"\n全体解禁失败\n"+qq+"没有本群管理权限";
                                    sendText(data,menu);
                                }
                            }
                        }
                        if(quntext.startsWith("上管@")) {
                            if(qq.equals(uin)||读(qun,"代管",uin)==1||读("0","代管",uin)==1) {
                                String aa="";
                                for(String at : data.atList) {
                                    aa+=at+"上管:"+setAdmin(qun,at,1);
                                }
                                sendText(data,""+aa);
                            }
                        }
                        if(quntext.startsWith("下管@")) {
                            if(qq.equals(uin)||读(qun,"代管",uin)==1||读("0","代管",uin)==1) {
                                String aa="";
                                for(String at : data.atList) {
                                    aa+=at+"下管:"+setAdmin(qun,at,1);
                                }
                                sendText(data,""+aa);
                            }
                        }
                    }
                }
            }
        }
    }
    ).start();
}