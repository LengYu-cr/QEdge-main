public void 赞我点赞(Object Yu) {
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
                if(quntext.equals("开启赞我点赞")) {
                    if(qq.equals(uin)||读("0","代管",uin)==1) {
                        写("0","赞我点赞","开关",1);
                        String menu="已开启全局发\"赞我\"点赞";
                        sendText(data,menu);
                    }
                }
                if(quntext.equals("关闭赞我点赞")) {
                    if(qq.equals(uin)||读("0","代管",uin)==1) {
                        写("0","赞我点赞","开关",0);
                        String menu="已关闭全局发\"赞我\"点赞";
                        sendText(data,menu);
                    }
                }
                if(quntext.equals("赞我点赞")) {
                    if(qq.equals(uin)||读("0","代管",uin)==1) {
                        String Leng="关";
                        if(读("0","赞我点赞","开关")==1) Leng="开";
                        String menu = "赞我点赞:\n赞我\n点赞@QQ/+QQ\n开启/关闭赞我点赞("+Leng+")\n设置赞我回复#成功回复#失败回复\n查看赞我回复\n(赞我回复支持变量)\n查看变量\n";
                        sendText(data,menu);
                    }
                }
                if(quntext.matches("设置赞我回复#[\\s\\S]+#[\\s\\S]+")) {
                    if(qq.equals(uin)||读("0","代管",uin)==1) {
                        String one=quntext.split("#")[1];
                        String two=quntext.split("#")[2];
                        写("0","赞我点赞","成功回复",one);
                        写("0","赞我点赞","失败回复",two);
                        sendText(data,"设置成功～");
                    }
                }
                if(quntext.equals("查看赞我回复")) {
                    if(qq.equals(uin)||读("0","代管",uin)==1) {
                        String one=文字("0","赞我点赞","成功回复");
                        String two=文字("0","赞我点赞","失败回复");
                        if(one.isEmpty())one="QQ:"+uin+"\n已为你点赞20次";
                        if(two.isEmpty())two="QQ:"+uin+"\n抱歉，今天已经为你点过赞了";
                        sendText(data,"赞我成功回复为:\n"+one+"\n\n赞我失败回复为:\n"+two);
                    }
                }
                if(quntext.startsWith("点赞@")&&mtype==2&&data.atList.size()>0) {
                    if(qq.equals(uin)||读("0","代管",uin)==1) {
                        String result="";
                        String result2="";
                        for(String uu:data.atList) {
                            if(uu.equals(qq)) {
                                result2+="\n\n"+getUserName(uu)+"("+uu+")点赞失败，检测到为主体账号";
                            }
                            else {
                                sendZan(uu,20);
                                result+="\n"+getUserName(uu)+"("+uu+")";
                            }
                        }
                        sendText(data,"已为以下群友点赞:"+result+result2);
                    }
                }
                if(读("0","赞我点赞","开关")==1) {
                    if(quntext.equals("赞我")) {
                        if(qq.equals(uin)) {
                            sendText(data,"无法给自己点赞哦～");
                        }
                        else {
                            String date=年月日();
                            String one=文字("0","赞我点赞","成功回复");
                            String two=文字("0","赞我点赞","失败回复");
                            if(one.isEmpty())one="QQ:"+uin+"\n已为你点赞20次";
                            if(two.isEmpty())two="QQ:"+uin+"\n抱歉，今天已经为你点过赞了";
                            String date2=文字("0","赞我点赞",uin);
                            if(date.equals(date2)) {
                                sendText(data,two);
                            }
                            else {
                                sendZan(uin,20);
                                sendText(data,one);
                                写("0","赞我点赞",uin,date);
                            }
                        }
                    }
                }
            }
        }
    }
    ).start();
}