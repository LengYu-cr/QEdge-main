public void 提示系统(Object Yu) {
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
                if(qq.equals(uin)||读(qun,"代管",uin)==1||读("0","代管",uin)==1) {
                    if(quntext.equals("提示系统")) {
                        if(读(qun,"进群提示","开关")==1) lengyu="开";
                        else lengyu="关";
                        if(读(qun,"退群提示","开关")==1) lengyu1="开";
                        else lengyu1="关";
                        if(读(qun,"入群验证","开关")==1) lengyu2="开";
                        else lengyu2="关";
                        if(读(qun,"退群拉黑","开关")==1) lengyu3="开";
                        else lengyu3="关";
                        if(读(qun,"禁言提示","开关")==1) lengyu4="开";
                        else lengyu4="关";
                        int time=读(qun,"入群验证","时间");
                        if(time==0) time=60;
                        int money=读(qun,"入群收款","金额");
                        if(money==0) money=10;
                        String menu="提示系统:\n开启/关闭进群提示\n设置进群提示+内容\n开启/关闭退群提示\n设置退群提示+内容\n开启/关闭禁言提示\n开启/关闭入群验证\n开启/关闭退群拉黑\n设置入群验证数字/文字\n设置验证文字+内容\n设置验证时间+时间(秒)\n查看变量\nTip:验证时间默认60秒\n开启/关闭入群收款\n设置入群收款金额+金额(1=1分)\n\n进群提示("+lengyu+")\n退群提示("+lengyu1+")\n入群验证("+lengyu2+")\n退群拉黑("+lengyu3+")\n禁言提示("+lengyu4+")\n验证时间("+time+"秒)\n收款金额("+money+"分)";
                        sendText(data,menu);
                    }
                    if(quntext.equals("设置入群验证数字")) {
                        写(qun,"入群验证","模式","数字");
                        String menu="群"+qun+"\n已切换入群验证为数字";
                        sendText(data,menu);
                    }
                    if(quntext.equals("设置入群验证文字")) {
                        写(qun,"入群验证","模式","文字");
                        String menu="群"+qun+"\n已切换入群验证为文字";
                        sendText(data,menu);
                    }
                    if(quntext.startsWith("设置验证文字")) {
                        写(qun,"入群验证","文字",""+quntext.substring(6));
                        String menu="群"+qun+"\n已设置入群验证为"+quntext.substring(6);
                        sendText(data,menu);
                    }
                    if(quntext.matches("设置验证时间[0-9]+")) {
                        写(qun,"入群验证","时间",Integer.parseInt(quntext.substring(6)));
                        String menu="群"+qun+"\n已设置入群时间为"+quntext.substring(6)+"秒";
                        sendText(data,menu);
                    }
                    if(quntext.equals("开启禁言提示")) {
                        写(qun,"禁言提示","开关",1);
                        String menu="群"+qun+"\n已开启禁言提示";
                        sendText(data,menu);
                    }
                    if(quntext.equals("关闭禁言提示")) {
                        写(qun,"禁言提示","开关",0);
                        String menu="群"+qun+"\n已关闭禁言提示";
                        sendText(data,menu);
                    }
                    if(quntext.equals("开启进群提示")) {
                        写(qun,"进群提示","开关",1);
                        String menu="群"+qun+"\n已开启进群提示";
                        sendText(data,menu);
                    }
                    if(quntext.equals("关闭进群提示")) {
                        写(qun,"进群提示","开关",0);
                        String menu="群"+qun+"\n已关闭进群提示";
                        sendText(data,menu);
                    }
                    if(quntext.equals("开启入群收款")) {
                        写(qun,"入群收款","开关",1);
                        String menu="群"+qun+"\n已开启入群收款";
                        sendText(data,menu);
                    }
                    if(quntext.equals("关闭入群收款")) {
                        写(qun,"入群收款","开关",0);
                        String menu="群"+qun+"\n已关闭入群收款";
                        sendText(data,menu);
                    }
                    if(quntext.startsWith("设置退群提示")) {
                        String text=quntext.substring(6);
                        写(qun,"退群提示","内容",text);
                        sendText(data,"设置成功");
                    }
                    if(quntext.startsWith("设置进群提示")) {
                        String text=quntext.substring(6);
                        写(qun,"进群提示","内容",text);
                        sendText(data,"设置成功");
                    }
                    if(quntext.matches("设置入群收款金额[0-9]+")) {
                        String text=quntext.substring(8);
                        写(qun,"入群收款","金额",Integer.parseInt(text));
                        sendText(data,"设置成功");
                    }
                    if(quntext.equals("开启退群提示")) {
                        写(qun,"退群提示","开关",1);
                        String menu="群"+qun+"\n已开启退群提示";
                        sendText(data,menu);
                    }
                    if(quntext.equals("关闭退群提示")) {
                        写(qun,"退群提示","开关",0);
                        String menu="群"+qun+"\n已关闭退群提示";
                        sendText(data,menu);
                    }
                    if(quntext.equals("开启退群拉黑")) {
                        写(qun,"退群拉黑","开关",1);
                        String menu="群"+qun+"\n已开启退群拉黑";
                        sendText(data,menu);
                    }
                    if(quntext.equals("关闭退群拉黑")) {
                        写(qun,"退群拉黑","开关",0);
                        String menu="群"+qun+"\n已关闭退群拉黑";
                        sendText(data,menu);
                    }
                    if(quntext.equals("开启入群验证")) {
                        写(qun,"入群验证","开关",1);
                        String menu="群"+qun+"\n已开启入群验证";
                        sendText(data,menu);
                    }
                    if(quntext.equals("关闭入群验证")) {
                        写(qun,"入群验证","开关",0);
                        String menu="群"+qun+"\n已关闭入群验证";
                        sendText(data,menu);
                    }
                }
                try {
                    if(读(qun,"入群验证","开关")==1) {
                        String msg=文字(qun,uin+"入群验证","是否成功");
                        if(msg.equals("未验证")) {
                            String text=文字(qun,uin+"入群验证","内容");
                            if(quntext.contains(text)) {
                                写(qun,uin+"入群验证","是否成功","已验证");
                                sendText(data,"[atUin="+uin+"]验证成功！\n欢迎您的加入～");
                            }
                            else {
                                sendText(data,"[atUin="+uin+"]验证失败，请重试\n您的验证码为:"+text);
                            }
                        }
                    }
                }
                catch(e) {
                }
            }
        }
    }
    ).start();
}