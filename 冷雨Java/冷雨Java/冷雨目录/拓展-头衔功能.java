public void 头衔功能(Object Yu) {
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
            if(读(qun,"lengyu520","开关")==1&&cdxz(qun,uin)&&判断群(qun,mtype)==1) {
                if(quntext.equals("开启自助头衔")) {
                    if(qq.equals(uin)||读(qun,"代管",uin)==1||读("0","代管",uin)==1) {
                        写(qun,"自助头衔","开关",1);
                        String menu="已开启本聊天自助头衔";
                        sendText(data,menu);
                    }
                }
                if(quntext.equals("关闭自助头衔")) {
                    if(qq.equals(uin)||读(qun,"代管",uin)==1||读("0","代管",uin)==1) {
                        写(qun,"自助头衔","开关",0);
                        String menu="已关闭本聊天自助头衔";
                        sendText(data,menu);
                    }
                }
                if(quntext.equals("头衔功能")) {
                    String lengyu = "关";
                    if(读(qun,"自助头衔","开关")==1) lengyu = "开";
                    String menu = "自助头衔:\n开启/关闭自助头衔("+lengyu+")\n我要头衔\n设置头衔金额+金额(分)\n上头衔+QQ 头衔\n上头衔@QQ 头衔\n添加头衔违禁词+内容\n删除头衔违禁词+内容\n查看头衔违禁词+内容\n头衔违禁词列表\n设置头衔违禁禁言+时间\n清空头衔违禁词\n群员头衔+头衔(一键全员)";
                    sendText(data,menu);
                }
                if(读(qun,"自助头衔","开关")==1) {
                    if(quntext.startsWith("我要头衔")||quntext.startsWith("我要头街")) {
                        if(读(qun,"自助头衔","开关")==1) {
                            int money=读(qun,"自助头衔","金额");
                            cb =quntext.substring(4);
                            cf =cb.replace("'","");
                            cg =cf.replace(".","");
                            ch =cg.replace("•","");
                            op =ch.replace(",","");
                            if(qq.equals(uin)||读(qun,"代管",uin)==1||读("0","代管",uin)==1||读(qun,"白名单",uin)==1||读("0","白名单",uin)==1) {
                                setMemberUniqueTitle(qun,uin,quntext.substring(4));
                                String menu="QQ:"+uin+"\n您的头衔已经ok了哦";
                                sendText(data,menu);
                            }
                            else {
                                boolean tf=false;
                                for(String u:列表2(qun,"头衔违禁词")) {
                                    if(op.contains(u)) {
                                        tf=true;
                                        break;
                                    }
                                }
                                if(tf) {
                                    sendText(data,uin+"触发头衔违禁词");
                                    recallMsg(qun,data.msgid,2);
                                    int jy=读(qun,"头衔违禁","禁言时间");
                                    shutUp(qun,uin,jy*60);
                                }
                                else {
                                    if(money>0) {
                                        String title="来自:我要头衔，请支付";
                                        String nm=get("https://mqq.tenpay.com/cgi-bin/qcollect/qpay_collect_create.cgi?type=1&memo="+title+"&amount="+money+"&payer_list=[{\"uin\":"+uin+",\"amount\":"+money+"}]&num=1&recv_type=1&group_id="+qun+"&uin="+qq+"&pskey="+tenpay+"&skey="+skey);
                                        JSONObject json1 = new JSONObject(nm);
                                        String retmsg=json1.get("retmsg");
                                        if(retmsg.equals("ok")) {
                                            String collection_no=json1.get("collection_no");
                                            double result = money / 100.0;
                                            sendText(data,"[atUin="+uin+"]\n请支付"+result+"元，即可获得头衔");
                                            int i=0;
                                            boolean tf=false;
                                            do {
                                                String nb=get("https://mqq.tenpay.com/cgi-bin/qcollect/qpay_collect_detail.cgi?collection_no="+collection_no+"&uin="+qq+"&pskey="+tenpay+"&skey="+skey+"&skey_type=2");
                                                JSONObject json2 = new JSONObject(nb);
                                                String payer_list=json2.getString("payer_list");
                                                JSONObject json3 = new JSONObject(payer_list.replace("[","").replace("]",""));
                                                String state=json3.get("state");
                                                if(state.equals("2")) {
                                                    String menu="付款成功，已处理...";
                                                    setMemberUniqueTitle(qun,uin,quntext.substring(4));
                                                    sendText(data,menu);
                                                    tf=true;
                                                    break;
                                                }
                                                else {
                                                }
                                                i++;
                                                Thread.sleep(1000);
                                            }
                                            while(i<60);
                                            if(!tf) {
                                                String menu="[atUin="+uin+"]\n设置头衔失败，超60秒未支付";
                                                sendText(data,menu);
                                                int jy=读(qun,"头衔违禁","禁言时间");
                                                shutUp(qun,uin,jy*60);
                                            }
                                        }
                                        else {
                                            String menu="发起群收款失败\n"+retmsg;
                                            sendText(data,menu);
                                        }
                                    }
                                    else {
                                        setMemberUniqueTitle(qun,uin,quntext.substring(4));
                                        String menu="QQ:"+uin+"\n您的头衔已经ok了哦";
                                        sendText(data,menu);
                                    }
                                }
                            }
                        }
                    }
                }
                if(quntext.matches("设置头衔金额[0-9]+")) {
                    if(qq.equals(uin)||读(qun,"代管",uin)==1||读("0","代管",uin)==1) {
                        int one=Long.parseLong(quntext.substring(6));
                        写(qun,"自助头衔","金额",one);
                        sendText(data,"写入自助头衔金额"+one+"分成功～");
                    }
                }
                if(quntext.matches("设置头衔违禁禁言[0-9]+")) {
                    if(qq.equals(uin)||读(qun,"代管",uin)==1||读("0","代管",uin)==1) {
                        int one=Long.parseLong(quntext.substring(8));
                        写(qun,"头衔违禁","禁言时间",one);
                        sendText(data,"写入头衔违禁时间成功～");
                    }
                }
                if(quntext.startsWith("添加头衔违禁词")) {
                    if(qq.equals(uin)||读(qun,"代管",uin)==1||读("0","代管",uin)==1) {
                        String one=quntext.substring(7);
                        if(读(qun,"头衔违禁词",one)==1) {
                            sendText(data,"已添加过该头衔违禁词～");
                        }
                        else {
                            写(qun,"头衔违禁词",one,1);
                            sendText(data,"写入头衔违禁词成功～");
                        }
                    }
                }
                if(quntext.startsWith("查看头衔违禁词")) {
                    if(qq.equals(uin)||读(qun,"代管",uin)==1||读("0","代管",uin)==1) {
                        String one=quntext.substring(5);
                        if(读(qun,"头衔违禁词",one)==1) {
                            sendText(data,"已有该违禁词～");
                        }
                        else {
                            sendText(data,"暂无该违禁词～");
                        }
                    }
                }
                if(quntext.startsWith("删除头衔违禁词")) {
                    if(qq.equals(uin)||读(qun,"代管",uin)==1||读("0","代管",uin)==1) {
                        String text=quntext.substring(7);
                        if(读(qun,"头衔违禁词",text)!=1) {
                            sendText(data,"该头衔违禁词不在本群头衔违禁词列表中哦～");
                        }
                        else {
                            清除(qun,"头衔违禁词",text);
                            sendText(data,"已删除该头衔违禁词～");
                        }
                    }
                }
                if(quntext.equals("头衔违禁词列表")) {
                    if(qq.equals(uin)||读("0","代管",uin)==1) {
                        String[] List=列表2(qun,"头衔违禁词");
                        String x="本群头衔违禁词列表有:";
                        String a=x;
                        long i=0;
                        for(String s:List) {
                            i++;
                            x=x+"\n"+i+"."+s;
                        }
                        if(x.equals(a)) x="目前还没有头衔违禁词哦～";
                        sendText(data,x);
                    }
                }
                if(quntext.equals("清空头衔违禁词")) {
                    if(qq.equals(uin)||读(qun,"代管",uin)==1||读("0","代管",uin)==1) {
                        删除(qun,"头衔违禁词");
                        sendText(data,"已清空本群头衔违禁词列表～");
                    }
                }
                if(quntext.startsWith("群员头衔")) {
                    if(qq.equals(uin)||读(qun,"代管",uin)==1||读("0","代管",uin)==1) {
                        String text=""+quntext.substring(4);
                        try {
                            if(Module.equals("Serendipity") || Module.equals("模了个块")) {
                                for(HashMap info:getGroupMemberList(qun)) {
                                    setMemberUniqueTitle(qun,info.get("uin"),text);
                                }
                                sendText(data,"已将所有群员设置头衔为"+text);
                            }
                            else if(Module.equals("QStory")) {
                                Object st=getGroupMemberList(qun);
                                for(Object b:st)
                                {
                                    String um=b.UserUin;
                                    setMemberUniqueTitle(qun,um,text);
                                }
                                sendText(data,"已将所有群员设置头衔为"+text);
                            }else if(Module.equals("QFun")) {
                                Object st=getGroupMemberList(qun);
                                for(Object b:st)
                                {
                                    String um=b.uin;
                                    setMemberUniqueTitle(qun,um,text);
                                }
                                sendText(data,"已将所有群员设置头衔为"+text);
                            }
                        }
                        catch(e) {
                            sendText(data,"出错啦"+e);
                        }
                    }
                }
                if(quntext.matches("上头衔[0-9]+ [\\s\\S]+")) {
                    if(qq.equals(uin)||读(qun,"代管",uin)==1||读("0","代管",uin)==1) {
                        String at=quntext.split("衔")[1];
                        String t=quntext.split(" ")[1];
                        String aite=at.replace(" "+t,"");
                        setMemberUniqueTitle(qun,aite,t);
                        String menu="ok";
                        sendText(data,menu);
                    }
                }
                if(quntext.startsWith("上头衔@")) {
                    if(qq.equals(uin)||读(qun,"代管",uin)==1||读("0","代管",uin)==1) {
                        String at=data.atList.get(0);
                        int str=quntext.lastIndexOf(" ")+1;
                        String text=quntext.substring(str);
                        setMemberUniqueTitle(qun,at,text);
                        String menu="ok";
                        sendText(data,menu);
                    }
                }
            }
        }
    }
    ).start();
}