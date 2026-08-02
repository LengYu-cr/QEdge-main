public void 检测更新(Object Yu) {
    Object data = getData();
    data.put(Yu,Module);
    String quntext = data.quntext;
    String qun = data.qun;
    String uin = data.uin;
    String qq=myUin;
    int mtype=data.mtype;
    long msgid=data.msgid;
    int msgtype=data.msgtype;
    if(quntext.equals("检测更新")) {
        if(判断群(qun,mtype)==1) {
            if(读("0","代管",uin)==1||读(qun,"代管",uin)==1||qq.equals(uin)) {
                new Thread(new Runnable() {
                    public void run() {
                        //检测更新的，不用管
                        long oldV=Long.parseLong(Version.replace(".",""));
                        String Vnew=get(myWang+"update/myJava.php?version="+oldV+"&uin="+myUin);
                        if(!Vnew.equals("访问网页失败")) {
                            try {
                                JSONObject json_update=new JSONObject(Vnew);
                                Vnew=json_update.optString("version");
                                long newV=Long.parseLong(Vnew.replace(".",""));
                                if(json_update.getInt("code") == 0) {
                                    if(newV!=oldV) {
                                        String url=json_update.optString("path");
                                        sendText(data,"检测到新版本！\n冷雨Java"+Vnew+"\n内存较大，请耐心等待，自动更新");
                                        //sendMsg(myUin,"发现新版本:\n冷雨Java"+Vnew+"\n请耐心等待",1);
                                        DownloadToFile(""+url,ColdRainPath+"/下载/冷雨Java"+Vnew+".zip");
                                        Unzip(ColdRainPath+"/下载/冷雨Java"+Vnew+".zip",""+QQPath);
                                        sc(ColdRainPath+"/下载/冷雨Java"+Vnew+".zip");
                                        Toast("更新冷雨Java"+Vnew+"成功");
                                        //String nb=读(pluginPath+"更新日志.txt");
                                        //setTips("更新日志:",""+nb+"");
                                        sendText(data,"更新冷雨Java成功！\n版本:"+Vnew+"\n请重新加载本Java");
                                    }
                                    else {
                                        sendText(data,"暂无更新，当前已是最新版本");
                                    }
                                }
                                else {
                                    sendText(data,json_update.getString("msg"));
                                }
                            }
                            catch(e) {
                                sendText(data,"检测更新失败，请到官方群反馈:\n" + e.getMessage());
                            }
                        }
                        else {
                            sendText(data,"检测更新失败，请到官方群反馈");
                            Toast("检测更新失败，请稍后重试");
                        }
                    }
                }
                ).start();
            }
        }
    }
}