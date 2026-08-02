public void 头像菜单(Object Yu) {
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
                if(quntext.equals("上传头像")) {
                    if(qq.equals(uin)||读("0","代管",uin)==1) {
                        写(qun,"上传头像","开关",1);
                        Thread.sleep(800);
                        String menu=qq+"请发送头像(图片)";
                        sendText(data,menu);
                    }
                }
                if(msgtype==2) {
                    if(qq.equals(uin)||读("0","代管",uin)==1) {
                        if(读(qun,"上传头像","开关")==1) {
                            try {
                                String url=findUrlbyDataMsg(Yu);
                                long times=System.currentTimeMillis();
                                String filepath=RootPath2+times+".png";
                                DownloadToFile(url,filepath);
                                Thread.sleep(1000);
                                uploadAvatar(filepath);
                                写(qun,"上传头像","开关",0);
                                String menu=qq+"头像上成功";
                                sendText(data,menu);
                            }
                            catch(e) {
                                sendText(data,""+e);
                                写(qun,"上传头像","开关",0);
                            }
                        }
                    }
                }
                if(quntext.equals("上传封面")) {
                    if(qq.equals(uin)||读("0","代管",uin)==1) {
                        写(qun,"上传封面","开关",1);
                        String menu=qq+"请发送封面(图片)";
                        sendText(data,menu);
                    }
                }
                if(msgtype==2) {
                    if(qq.equals(uin)||读("0","代管",uin)==1) {
                        if(读(qun,"上传封面","开关")==1) {
                            try {
                                String url=findUrlbyDataMsg(Yu);
                                long times=System.currentTimeMillis();
                                String filepath=RootPath2+times+".png";
                                DownloadToFile(url,filepath);
                                Thread.sleep(1000);
                                uploadCover(filepath);
                                写(qun,"上传封面","开关",0);
                                String menu=qq+"封面上成功";
                                sendText(data,menu);
                            }
                            catch(e) {
                                sendText(data,""+e);
                                写(qun,"上传封面","开关",0);
                            }
                        }
                    }
                }
                if(quntext.equals("上传群头像")) {
                    if(qq.equals(uin)||读("0","代管",uin)==1) {
                        写(qun,"上传群头像","开关",1);
                        String menu=qq+"请发送群头像(图片)";
                        sendText(data,menu);
                    }
                }
                if(msgtype==2) {
                    if(读(qun,"上传群头像","开关")==1) {
                        if(qq.equals(uin)||读("0","代管",uin)==1) {
                            String 链接=findUrlbyDataMsg(Yu);
                            DownloadToFile(链接,ColdRainPath+"头像/"+qun+".png");
                            Thread.sleep(1000);
                            写(qun,"上传群头像","开关",0);
                            String menu = "";
                            UploadTroopAvatar(qun, ColdRainPath + "头像/" + qun + ".png", new Object() {
                                public void onResult(boolean success) {
                                    if (success) {
                                        menu = qun + "头像上传成功";
                                    } else {
                                        menu = qun + "头像上传失败";
                                    }
                                    sendText(data,menu);
                                }
                            });
                        }
                    }
                }
                if(quntext.startsWith("设置群头像http")) {
                    if(qq.equals(uin)||读("0","代管",uin)==1) {
                        String Url=quntext.substring(5);
                        if(Url.equals("")) {
                            String menu="图片链接为空，请发送『设置群头像+图片链接』";
                            sendText(data,menu);
                            return;
                        }
                        else {
                            DownloadToFile(Url,ColdRainPath+"/头像/"+qun+".png");
                            String menu = "";
                            Thread.sleep(1000);
                            UploadTroopAvatar(qun, ColdRainPath + "头像/" + qun + ".png", new Object() {
                                public void onResult(boolean success) {
                                    if (success) {
                                        menu = qun + "头像上传成功";
                                    } else {
                                        menu = qun + "头像上传失败";
                                    }
                                    sendText(data,menu);
                                }
                            });
                        }
                    }
                }
                if(quntext.startsWith("设置群头像/")) {
                    if(qq.equals(uin)||读("0","代管",uin)==1) {
                        String Url=quntext.substring(5);
                        if(Url.equals("")) {
                            String menu="图片链接为空，请发送『设置群头像+图片路径』";
                            sendText(data,menu);
                            return;
                        }
                        else {
                        String menu = "";
                            UploadTroopAvatar(qun, ColdRainPath + "头像/" + qun + ".png", new Object() {
                                public void onResult(boolean success) {
                                    if (success) {
                                        menu = qun + "头像上传成功";
                                    } else {
                                        menu = qun + "头像上传失败";
                                    }
                                    sendText(data,menu);
                                }
                            });
                        }
                    }
                }
                if(quntext.equals("上传群封面")) {
                    if(qq.equals(uin)||读("0","代管",uin)==1) {
                        写(qun,"上传群封面","开关",1);
                        String menu=qq+"请发送群封面(图片)";
                        sendText(data,menu);
                    }
                }
                if(msgtype==2) {
                    if(读(qun,"上传群封面","开关")==1) {
                        if(qq.equals(uin)||读("0","代管",uin)==1) {
                            String 链接=findUrlbyDataMsg(Yu);
                            DownloadToFile(链接,ColdRainPath+"封面/"+qun+".png");
                            Thread.sleep(1000);
                            写(qun,"上传群封面","开关",0);
                            String menu = "";
                            UploadTroopCover(qun, ColdRainPath + "封面/" + qun + ".png", new Object() {
                                public void onResult(boolean success) {
                                    if (success) {
                                        menu = qun + "群封面上传成功";
                                    } else {
                                        menu = qun + "群封面上传失败";
                                    }
                                    sendText(data,menu);
                                }
                            });
                        }
                    }
                }
                if(quntext.startsWith("设置群封面http")) {
                    if(qq.equals(uin)||读("0","代管",uin)==1) {
                        String Url=quntext.substring(5);
                        if(Url.equals("")) {
                            String menu="图片链接为空，请发送『设置群封面+图片链接』";
                            sendText(data,menu);
                            return;
                        }
                        else {
                            DownloadToFile(Url,ColdRainPath+"/封面/"+qun+".png");
                            String menu = "";
                            Thread.sleep(1000);
                            UploadTroopCover(qun, ColdRainPath + "封面/" + qun + ".png", new Object() {
                                public void onResult(boolean success) {
                                    if (success) {
                                        menu = qun + "群封面上传成功";
                                    } else {
                                        menu = qun + "群封面上传失败";
                                    }
                                    sendText(data,menu);
                                }
                            });
                        }
                    }
                }
                if(quntext.startsWith("设置群封面/")) {
                    if(qq.equals(uin)||读("0","代管",uin)==1) {
                        String Url=quntext.substring(5);
                        if(Url.equals("")) {
                            String menu="图片链接为空，请发送『设置群封面+图片路径』";
                            sendText(data,menu);
                            return;
                        }
                        else {
                        String menu = "";
                            UploadTroopCover(qun, ColdRainPath + "封面/" + qun + ".png", new Object() {
                                public void onResult(boolean success) {
                                    if (success) {
                                        menu = qun + "群封面上传成功";
                                    } else {
                                        menu = qun + "群封面上传失败";
                                    }
                                    sendText(data,menu);
                                }
                            });
                        }
                    }
                }
                if(quntext.equals("头像菜单")) {
                    if(qq.equals(uin)||读("0","代管",uin)==1) {
                        String menu="头像菜单:\n设置头像+链接/路径\n克隆头像@QQ/+QQ\n上传头像\n上传群头像\n设置群头像+链接/路径\n克隆群头像+群号/@QQ\n上传群封面\n设置群封面+图片\n上传封面\n设置封面+链接/路径";
                        sendText(data,menu);
                    }
                }
                if(quntext.startsWith("克隆头像@")) {
                    if(qq.equals(uin)||读("0","代管",uin)==1) {
                        for(String tex: data.atList) {
                            String url="http://q2.qlogo.cn/headimg_dl?dst_uin="+tex+"&spec=640";
                            long times=System.currentTimeMillis();
                            String filepath=RootPath2+times+".png";
                            DownloadToFile(url,filepath);
                            if(uploadAvatar(filepath)) {
                                String menu=tex+"的头像\n已克隆过来啦～";
                                sendText(data,menu);
                            }
                            else {
                                String menu="头像克隆失败";
                                sendText(data,menu);
                            }
                        }
                    }
                }
                if(quntext.matches("克隆头像[0-9]+")) {
                    if(qq.equals(uin)||读("0","代管",uin)==1) {
                        String tex=quntext.substring(4);
                        String url="http://q2.qlogo.cn/headimg_dl?dst_uin="+tex+"&spec=640";
                        long times=System.currentTimeMillis();
                        String filepath=RootPath2+times+".png";
                        DownloadToFile(url,filepath);
                        if(uploadAvatar(filepath)) {
                            String menu=tex+"的头像\n已克隆过来啦～";
                            sendText(data,menu);
                        }
                        else {
                            String menu="头像克隆失败";
                            sendText(data,menu);
                        }
                    }
                }
                if(quntext.startsWith("克隆群头像@")) {
                    if(qq.equals(uin)||读("0","代管",uin)==1) {
                        for(String tex: data.atList) {
                            链接="http://q2.qlogo.cn/headimg_dl?dst_uin="+tex+"&spec=640";
                            String filepath=RootPath2+getTime()+".png";
                            DownloadToFile(链接,ColdRainPath+"/头像/"+qun+".png");
                            Thread.sleep(1000);
                            String menu = "";
                            UploadTroopAvatar(qun, ColdRainPath + "头像/" + qun + ".png", new Object() {
                                public void onResult(boolean success) {
                                    if (success) {
                                        menu = qun + "头像上传成功";
                                    } else {
                                        menu = qun + "头像上传失败";
                                    }
                                    sendText(data,menu);
                                }
                            });
                        }
                    }
                }
                if(quntext.matches("克隆群头像[0-9]+")) {
                    if(qq.equals(uin)||读("0","代管",uin)==1) {
                        String url="http://p.qlogo.cn/gh/"+quntext.substring(5)+"/"+quntext.substring(5)+"/640";
                        DownloadToFile(url,ColdRainPath+"/头像/"+qun+".png");
                        Thread.sleep(1000);
                        String menu = "";
                        UploadTroopAvatar(qun, ColdRainPath + "头像/" + qun + ".png", new Object() {
                                public void onResult(boolean success) {
                                    if (success) {
                                        menu = qun + "头像上传成功";
                                    } else {
                                        menu = qun + "头像上传失败";
                                    }
                                    sendText(data,menu);
                                }
                            });
                    }
                }
                if(quntext.startsWith("设置头像/")) {
                    if(qq.equals(uin)||读("0","代管",uin)==1) {
                        String Url=quntext.substring(4);
                        if(uploadAvatar(Url)) {
                            String menu=qq+"头像上传成功";
                            sendText(data,menu);
                        }
                        else {
                            String menu=qq+"头像上传失败";
                            sendText(data,menu);
                        }
                    }
                }
                if(quntext.startsWith("设置封面/")) {
                    if(qq.equals(uin)||读("0","代管",uin)==1) {
                        String Url=quntext.substring(5);
                        if(uploadCover(Url)) {
                            String menu=qq+"封面上传成功";
                            sendText(data,menu);
                        }
                        else {
                            String menu=qq+"封面上传失败";
                            sendText(data,menu);
                        }
                    }
                }
                if(quntext.startsWith("设置头像http")) {
                    if(qq.equals(uin)||读("0","代管",uin)==1) {
                        String Url=quntext.substring(4);
                        long times=System.currentTimeMillis();
                        String filepath=RootPath2+times+".png";
                        DownloadToFile(Url,filepath);
                        Thread.sleep(1000);
                        if(uploadAvatar(filepath)) {
                            String menu=qq+"头像上传成功";
                            sendText(data,menu);
                        }
                        else {
                            String menu=qq+"头像上传失败";
                            sendText(data,menu);
                        }
                    }
                }
                if(quntext.startsWith("设置封面http")) {
                    if(qq.equals(uin)||读("0","代管",uin)==1) {
                        String Url=quntext.substring(4);
                        long times=System.currentTimeMillis();
                        String filepath=RootPath2+times+".png";
                        DownloadToFile(Url,filepath);
                        Thread.sleep(1000);
                        if(uploadCover(filepath)) {
                            String menu=qq+"封面上传成功";
                            sendText(data,menu);
                        }
                        else {
                            String menu=qq+"封面上传失败";
                            sendText(data,menu);
                        }
                    }
                }
                if(quntext.startsWith("上传GIF头像/")) {
                    if(qq.equals(uin)||读("0","代管",uin)==1) {
                        String Url=quntext.substring(7);
                        uploadAvatar(Url);
                        Thread.sleep(1000);
                        String menu=qq+"头像上成功";
                        sendText(data,menu);
                    }
                }
            }
        }
    }
    ).start();
}