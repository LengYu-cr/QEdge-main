public void 菜单配置(Object Yu) {
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
                if(qq.equals(uin)||读(qun,"代管",uin)==1||读("0","代管",uin)==1) {
                    if(quntext.equals("开启菜单限制")) {
                        if(qq.equals(uin)||读(qun,"代管",uin)==1||读("0","代管",uin)==1) {
                            写(qun,"菜单限制","开关",1);
                            String menu="已开启菜单限制";
                            sendText(data,menu);
                        }
                    }
                    if(quntext.equals("关闭菜单限制")) {
                        if(qq.equals(uin)||读(qun,"代管",uin)==1||读("0","代管",uin)==1) {
                            写(qun,"菜单限制","开关",0);
                            String menu="已关闭菜单限制";
                            sendText(data,menu);
                        }
                    }
                    if(quntext.startsWith("设置菜单召唤")) {
                        if(qq.equals(uin)||读(qun,"代管",uin)==1||读("0","代管",uin)==1) {
                            String text=quntext.substring(6);
                            if(text.equals("")) msg="菜单";
                            else msg=text;
                            if(mtype==2)写(qun,"菜单配置","menu_name",text);
                            else if(mtype==1)写("0","菜单配置","menu_name",text);
                            sendText(data,"设置成功！\n发送\""+msg+"\"即可呼出菜单");
                        }
                    }
                    if(quntext.equals("菜单配置")) {
                        if(qq.equals(uin)||读(qun,"代管",uin)==1||读("0","代管",uin)==1) {
                            String menu_name;
                            if(mtype==2)menu_name=文字(qun,"菜单配置","menu_name");
                            else if(mtype==1)menu_name=文字("0","菜单配置","menu_name");
                            if(menu_name.equals("")) menu_name="菜单";
                            else menu_name=menu_name;
                            sendText(data,"开启/关闭菜单限制\n设置菜单召唤+菜单名\n当前菜单召唤:"+menu_name+"\n切换文字/卡片/图片/管家/回复/MarkDown/官机/转发模式\n设置图片底图+链接/路径\n设置图片模式+数字(1,2)\n设置字体颜色+颜色编码(图片模式字体)");
                        }
                    }
                    if(quntext.equals("切换转发模式")) {
                        if(qq.equals(uin)||读(qun,"代管",uin)==1||读("0","代管",uin)==1) {
                            写(ColdRainPath+"data/"+qun+"菜单模式.txt","转发");
                            String menu="已切换为转发模式";
                            sendText(data,menu);
                        }
                    }
                    if(quntext.equals("切换文字模式")) {
                        if(qq.equals(uin)||读(qun,"代管",uin)==1||读("0","代管",uin)==1) {
                            写(ColdRainPath+"data/"+qun+"菜单模式.txt","文字");
                            String menu="已切换为文字模式";
                            sendText(data,menu);
                        }
                    }
                    if(quntext.equals("切换管家模式")) {
                        if(mtype==2) {
                            if(qq.equals(uin)||读(qun,"代管",uin)==1||读("0","代管",uin)==1) {
                                写(ColdRainPath+"data/"+qun+"菜单模式.txt","管家");
                                String menu="已切换为管家模式";
                                sendText(data,menu);
                            }
                        }
                        else {
                            String menu="不支持的聊天页面";
                            sendText(data,menu);
                        }
                    }
                    if(quntext.equals("切换图片模式")) {
                        if(qq.equals(uin)||读(qun,"代管",uin)==1||读("0","代管",uin)==1) {
                            写(ColdRainPath+"data/"+qun+"菜单模式.txt","图片");
                            String menu="已切换为图片模式";
                            sendText(data,menu);
                        }
                    }
                    if(quntext.equals("切换官机模式")) {
                        if(qq.equals(uin)||读(qun,"代管",uin)==1||读("0","代管",uin)==1) {
                            写(ColdRainPath+"data/"+qun+"菜单模式.txt","官机");
                            String menu="已切换为官机模式";
                            sendText(data,menu);
                        }
                    }
                    if(quntext.equals("切换MarkDown模式")) {
                        if(qq.equals(uin)||读(qun,"代管",uin)==1||读("0","代管",uin)==1) {
                            写(ColdRainPath+"data/"+qun+"菜单模式.txt","MarkDown");
                            String menu="已切换为MarkDown模式";
                            sendText(data,menu);
                        }
                    }
                    if(quntext.equals("切换卡片模式")) {
                        if(qq.equals(uin)||读(qun,"代管",uin)==1||读("0","代管",uin)==1) {
                            写(ColdRainPath+"data/"+qun+"菜单模式.txt","卡片");
                            String menu="已切换为卡片模式";
                            sendText(data,menu);
                        }
                    }
                    if(quntext.equals("切换回复模式")) {
                        if(qq.equals(uin)||读(qun,"代管",uin)==1||读("0","代管",uin)==1) {
                            写(ColdRainPath+"data/"+qun+"菜单模式.txt","回复");
                            String menu="已切换为回复模式";
                            sendText(data,menu);
                        }
                    }
                    if(quntext.startsWith("设置图片底图")) {
                        if(qq.equals(uin)||读("0","代管",uin)==1) {
                            String text=quntext.substring(6);
                            写("0","图片模式","图片底图",text);
                            sendText(data,"设置成功，图片底图错误可能报错");
                        }
                    }
                    if(quntext.matches("设置图片模式[0-9]+")) {
                        if(qq.equals(uin)||读("0","代管",uin)==1) {
                            int text=Integer.parseInt(quntext.substring(6));
                            if(text==1||text==2) {
                                写("0","图片模式","mode",text);
                                sendText(data,"设置成功，当前图片模式为"+text);
                            }
                            else {
                                sendText(data,"设置失败，当前图片模式仅支持1/2");
                            }
                        }
                    }
                    if(quntext.startsWith("设置字体颜色")) {
                        if(qq.equals(uin)||读("0","代管",uin)==1) {
                            String text=quntext.substring(6);
                            写("0","图片模式","字体颜色",text);
                            sendText(data,"设置成功，字体颜色格式错误可能报错\nTip:如果配置.java中textFaceType为1，那么将不启用");
                        }
                    }
                }
            }
        }
    }
    ).start();
}