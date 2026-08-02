public void 表情功能(Object msg)
{
    new Thread(new Runnable() {
        public void run() {
            Object data = getData();
            data.put(msg,""+Module);
            String quntext = data.quntext;
            String qun = data.qun;
            String uin = data.uin;
            String qq=myUin;
            int mtype=data.mtype;
            long msgid=data.msgid;
            if(判断群(qun,mtype)==1) {
                if(qq.equals(uin)||读(qun,"代管",uin)==1||读("0","代管",uin)==1) {
                    if(quntext.equals("表情功能")) {
                        sendText(data,"点赞 心碎 碎屏\n手雷 勾引 结印\n迎春 音响 比心\n敲门  戳一戳  放大招\n六六六 抓一下 玫瑰花\n让你皮 召唤术 宝贝球\n弹表情#FaceId#数量\n平底锅+数量\n略略略+数量\n榴莲/钞票+数量\n猪头/便便+数量\n炸弹/爱心+数量\n哈哈/亲亲+数量\n赞/药丸+数量\nTips:1.戳表情中部分svip不支持私聊\n   2.弹表情中部分id不支持\n   3.弹表情支持数量为负数");
                    }
                    if(quntext.matches("弹表情#[0-9]+#[-+]?[0-9]+")) {
                        int id=Integer.parseInt(quntext.split("#")[1]);
                        int num=Integer.parseInt(quntext.split("#")[2]);
                        if(id>0) {
                            if(id<=12) {
                                sendTroopBullet(qun,id,num,mtype);
                            }
                            else {
                                sendTroopBullet(qun,13,num,id,mtype);
                            }
                        }
                        else {
                            sendText(data,"有这个id吗？你就发");
                        }
                    }
                    if(quntext.matches("榴莲[-+]?[0-9]+")) {
                        int num=Integer.parseInt(quntext.substring(2));
                        sendTroopBullet(qun,9,num,mtype);
                    }
                    if(quntext.matches("平底锅[-+]?[0-9]+")) {
                        int num=Integer.parseInt(quntext.substring(3));
                        sendTroopBullet(qun,11,num,mtype);
                    }
                    if(quntext.matches("钞票[-+]?[0-9]+")) {
                        int num=Integer.parseInt(quntext.substring(2));
                        sendTroopBullet(qun,12,num,mtype);
                    }
                    if(quntext.matches("略略略[-+]?[0-9]+")) {
                        int num=Integer.parseInt(quntext.substring(3));
                        sendTroopBullet(qun,10,num,mtype);
                    }
                    if(quntext.matches("猪头[-+]?[0-9]+")) {
                        int num=Integer.parseInt(quntext.substring(2));
                        sendTroopBullet(qun,4,num,mtype);
                    }
                    if(quntext.matches("便便[-+]?[0-9]+")) {
                        int num=Integer.parseInt(quntext.substring(2));
                        sendTroopBullet(qun,6,num,mtype);
                    }
                    if(quntext.matches("炸弹[-+]?[0-9]+")) {
                        int num=Integer.parseInt(quntext.substring(2));
                        sendTroopBullet(qun,5,num,mtype);
                    }
                    if(quntext.matches("爱心[-+]?[0-9]+")) {
                        int num=Integer.parseInt(quntext.substring(2));
                        sendTroopBullet(qun,2,num,mtype);
                    }
                    if(quntext.matches("哈哈[-+]?[0-9]+")) {
                        int num=Integer.parseInt(quntext.substring(2));
                        sendTroopBullet(qun,3,num,mtype);
                    }
                    if(quntext.matches("亲亲[-+]?[0-9]+")) {
                        int num=Integer.parseInt(quntext.substring(2));
                        sendTroopBullet(qun,7,num,mtype);
                    }
                    if(quntext.matches("赞[-+]?[0-9]+")) {
                        int num=Integer.parseInt(quntext.substring(1));
                        sendTroopBullet(qun,1,num,mtype);
                    }
                    if(quntext.matches("药丸[-+]?[0-9]+")) {
                        int num=Integer.parseInt(quntext.substring(2));
                        sendTroopBullet(qun,8,num,mtype);
                    }
                    if(quntext.equals("戳一戳")) {
                        sendTroopPoke(qun,1,0,mtype);
                    }
                    if(quntext.equals("比心")) {
                        sendTroopPoke(qun,2,0,mtype);
                    }
                    if(quntext.equals("点赞")) {
                        sendTroopPoke(qun,3,0,mtype);
                    }
                    if(quntext.equals("心碎")) {
                        sendTroopPoke(qun,4,0,mtype);
                    }
                    if(quntext.equals("六六六")) {
                        sendTroopPoke(qun,5,0,mtype);
                    }
                    if(quntext.equals("放大招")) {
                        sendTroopPoke(qun,6,0,mtype);
                    }
                    if(quntext.equals("敲门")) {
                        sendTroopPoke(qun,126,2000,mtype);
                    }
                    if(quntext.equals("抓一下")) {
                        sendTroopPoke(qun,126,2001,mtype);
                    }
                    if(quntext.equals("碎屏")) {
                        sendTroopPoke(qun,126,2002,mtype);
                    }
                    if(quntext.equals("手雷")) {
                        sendTroopPoke(qun,126,2004,mtype);
                    }
                    if(quntext.equals("勾引")) {
                        sendTroopPoke(qun,126,2003,mtype);
                    }
                    if(quntext.equals("结印")) {
                        sendTroopPoke(qun,126,2005,mtype);
                    }
                    if(quntext.equals("玫瑰花")) {
                        sendTroopPoke(qun,126,2007,mtype);
                    }
                    if(quntext.equals("迎春")) {
                        sendTroopPoke(qun,126,2008,mtype);
                    }
                    if(quntext.equals("让你皮")) {
                        sendTroopPoke(qun,126,2009,mtype);
                    }
                    if(quntext.equals("音响")) {
                        sendTroopPoke(qun,126,2010,mtype);
                    }
                    if(quntext.equals("召唤术")) {
                        sendTroopPoke(qun,126,2006,mtype);
                    }
                    if(quntext.equals("宝贝球")) {
                        sendTroopPoke(qun,126,2011,mtype);
                    }
                }
            }
        }
    }
    ).start();
}