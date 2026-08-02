public void 自助上管(Object Yu) {
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
            if(mtype==2&&读(qun,"lengyu520","开关")==1&&cdxz(qun,uin)&&判断群(qun,mtype)==1) {
                if(quntext.equals("开启自助上管")) {
                    if(qq.equals(uin)||读(qun,"代管",uin)==1||读("0","代管",uin)==1) {
                        写(qun,"自助上管","开关",1);
                        String menu="已开启本聊天自助上管";
                        sendText(data,menu);
                    }
                }
                if(quntext.equals("关闭自助上管")) {
                    if(qq.equals(uin)||读(qun,"代管",uin)==1||读("0","代管",uin)==1) {
                        写(qun,"自助上管","开关",0);
                        String menu="已关闭本聊天自助上管";
                        sendText(data,menu);
                    }
                }
                if(quntext.equals("自助上管")) {
                    if(读(qun,"自助上管","开关")==1) {
                        int money=读(qun,"自助上管","上管金额");
                        String menu = "自助上管:\n开启/关闭自助上管\n我要管理\n设置上管金额+金额(分)\n\n自助上管金额(分):("+money+")\n";
                        sendText(data,menu);
                    }
                    else {
                        sendText(data,"本聊天未开启自助上管");
                    }
                }
                if(读(qun,"自助上管","开关")==1) {
                    if(quntext.equals("我要管理")) {
                        if(!qq.equals(uin)) {
                            int money=读(qun,"自助上管","上管金额");
                            if(money==0) money=10;
                            String title="来自:我要管理，请支付";
                            String nm=get("https://mqq.tenpay.com/cgi-bin/qcollect/qpay_collect_create.cgi?type=1&memo="+title+"&amount="+money+"&payer_list=[{\"uin\":"+uin+",\"amount\":"+money+"}]&num=1&recv_type=1&group_id="+qun+"&uin="+qq+"&pskey="+tenpay+"&skey="+skey);
                            JSONObject json1 = new JSONObject(nm);
                            String retmsg=json1.get("retmsg");
                            if(retmsg.equals("ok")) {
                                String collection_no=json1.get("collection_no");
                                double result = money / 100.0;
                                sendText(data,"[atUin="+uin+"]\n请支付"+result+"元，即可获得管理员");
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
                                        String a=setAdmin(qun,uin,1);
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
                                    String menu="[atUin="+uin+"]\n上管失败，超60秒未支付";
                                    sendText(data,menu);
                                }
                            }
                            else {
                                String menu="发起群收款失败\n"+retmsg;
                                sendText(data,menu);
                            }
                        }
                        else {
                            sendText(data,"不是，你自己问自己要管理员？？？");
                        }
                    }
                    if(quntext.matches("设置上管金额[0-9]+")) {
                        if(qq.equals(uin)||读(qun,"代管",uin)==1||读("0","代管",uin)==1) {
                            int money=Long.parseLong(quntext.substring(6));
                            写(qun,"自助上管","上管金额",money);
                            String menu="设置成功，单个管理金额"+money+"分";
                            sendText(data,menu);
                        }
                    }
                }
            }
        }
    }
    ).start();
}