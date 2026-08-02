public void 艾特作图(Object Yu) {
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
                if(quntext.equals("开启艾特作图")) {
                    if(qq.equals(uin)||读(qun,"代管",uin)==1||读("0","代管",uin)==1) {
                        写(qun,"艾特作图","开关",1);
                        String menu="已开启本聊天艾特作图";
                        sendText(data,menu);
                    }
                }
                if(quntext.equals("关闭艾特作图")) {
                    if(qq.equals(uin)||读(qun,"代管",uin)==1||读("0","代管",uin)==1) {
                        写(qun,"艾特作图","开关",0);
                        String menu="已关闭本聊天艾特作图";
                        sendText(data,menu);
                    }
                }
                if(quntext.equals("艾特作图")) {
                    if(读(qun,"艾特作图","开关")==1) {
                        if(读(qun,"反弹作图","开关")==1)lengyu="开";
                        else lengyu="关";
                        String atPicTips = "艾特作图:\n";
                        if(!"".equals(at_pic_array)) {
                            JSONObject jsonObj = new JSONObject(at_pic_array);
                            String[] keys = jsonObj.keySet().toArray(new String[0]);
                            int total = keys.length;
                            int totalPage = total / 50;
                            if(total % 50 != 0) totalPage++;
                            for(int i=0;
                            i<totalPage;
                            i++) {
                                atPicTips += "艾特作图" + (i+1);
                                if((i+1)%2 == 0) {
                                    atPicTips += "\n";
                                }
                                else if(i != totalPage-1) {
                                    atPicTips += " ";
                                }
                            }
                        }
                        else {
                            atPicTips += "艾特作图1 艾特作图2\n艾特作图3 艾特作图4\n";
                        }
                        atPicTips += "开启/关闭艾特作图\n开启/关闭反弹作图(当被对象为机器人时自动替换成触发人QQ)\n\n反弹作图("+lengyu+")";
                        sendText(data, atPicTips);
                    }
                    else {
                        sendText(data,"本聊天未开启艾特作图");
                    }
                }
                if(读(qun,"艾特作图","开关")==1) {
                    if(读(qun,"艾特作图","开关")==1) {
                        if(data.atList.size()>=1&&quntext.matches("[\\s\\S]+@[\\s\\S]+")) {
                            String at=data.atList.get(0);
                            if(at.equals(myUin)&&读(qun,"反弹作图","开关")==1) at=uin;
                            try {
                                String tex=quntext.split("@")[1];
                                String text=quntext.replace("@"+tex,"");
                                JSONObject a=new JSONObject(at_pic_array);
                                String URL=a.optString(text);
                                if(!URL.equals("")) {
                                    String url=URL.replace("[QQ]",at);
                                    url=url.replace("[User]",uin);
                                    if(url.contains("[Name]")) {
                                        url=url.replace("[Name]",getUserName(at));
                                    }
                                    sendImg(qun,url,mtype);
                                }
                            }
                            catch(e) {
                            }
                        }
                        if(quntext.matches("[\\s\\S]+我")) {
                            try {
                                String text=quntext.replace("我","");
                                JSONObject a=new JSONObject(at_pic_array);
                                String URL=a.optString(text);
                                if(!URL.equals("")) {
                                    String url=URL.replace("[QQ]",uin).replace("[Name]",getUserName(uin));
                                    sendImg(qun,url,mtype);
                                }
                            }
                            catch(e) {
                            }
                        }
                        if(quntext.matches("[\\s\\S]+#[0-9]+")) {
                            String at=quntext.split("#")[1];
                            if(at.equals(myUin)&&读(qun,"反弹作图","开关")==1) at=uin;
                            String text=quntext.replace(at,"").replace(" ","").replace("#","");
                            try {
                                JSONObject a=new JSONObject(at_pic_array);
                                String URL=a.get(""+text);
                                String url=URL.replace("[QQ]",at).replace("[Name]",getUserName(at));
                                sendImg(qun,url,mtype);
                            }
                            catch(e) {
                            }
                        }
                    }
                    if(quntext.matches("艾特作图[0-9]+")) {
                        int num;
                        try {
                            num = Integer.parseInt(quntext.substring(4));
                        }
                        catch (NumberFormatException e) {
                            num = 1;
                        }
                        int 开关 = 读(qun,"艾特作图","开关");
                        if(开关 == 0) {
                            sendText(data,"本群艾特作图未开启");
                        }
                        else {
                            String xx = "";
                            if(!"".equals(at_pic_array)) {
                                JSONObject jsonObj = new JSONObject(at_pic_array);
                                String[] keys = jsonObj.keySet().toArray(new String[0]);
                                int total = keys.length;
                                int start = 50 * (num - 1);
                                int end = 50 * num;
                                if(end > total) {
                                    end = total;
                                }
                                int totalPage;
                                if(total % 50 == 0) {
                                    totalPage = total / 50;
                                }
                                else {
                                    totalPage = total / 50 + 1;
                                }
                                if(start >= total) {
                                    xx = "此页为空，请输入正确页数！";
                                }
                                else {
                                    int idx = 1;
                                    for(int i = start;
                                    i < end;
                                    i++) {
                                        String keyword = keys[i];
                                        xx += (50*(num-1)+idx) + "、" + keyword + "@QQ/#+QQ/我" + "\n";
                                        idx++;
                                    }
                                    xx += "第" + num + "页，共" + totalPage + "页";
                                }
                            }
                            else {
                                xx = "作图列表未加载完成，请稍后再试！";
                            }
                            sendText(data, xx);
                        }
                    }
                    if(quntext.equals("开启反弹作图")) {
                        if(qq.equals(uin)||读(qun,"代管",uin)==1||读("0","代管",uin)==1) {
                            写(qun,"反弹作图","开关",1);
                            String menu="已开启本聊天反弹作图";
                            sendText(data,menu);
                        }
                    }
                    if(quntext.equals("关闭反弹作图")) {
                        if(qq.equals(uin)||读(qun,"代管",uin)==1||读("0","代管",uin)==1) {
                            写(qun,"反弹作图","开关",0);
                            String menu="已关闭本聊天反弹作图";
                            sendText(data,menu);
                        }
                    }
                }
            }
        }
    }
    ).start();
}
String at_pic_array = "";
new Thread(new Runnable() {
    public void run() {
        String at_pic = get(myWang + "java/?action=at_pic");
        if(!at_pic.equals("")&&!at_pic.equals("访问网页失败")) {
            JSONObject json = new JSONObject(at_pic);
            at_pic_array = json.getJSONObject("data").toString();
        }
        else {
            Toast("艾特作图缓存获取失败");
        }
    }
}
).start();