loadJava(RootPath+"proto/PacketHelper.java");

// 平台配置类
class MusicPlatform {
    String packageName;
    String appId;
    String token;
    
    MusicPlatform(String packageName, String appId, String token) {
        this.packageName = packageName;
        this.appId = appId;
        this.token = token;
    }
}

// 音乐发送接口
interface MusicSender {
    void sendTroop(String qun, String title, String desc, String url, String audio, String img);
    void sendFriend(String qun, String title, String desc, String url, String audio, String img);
}

// 缓存配置信息
private static String cachedQuality = null;
private static long qualityCacheTime = 0;

private String getCachedQuality() {
    long currentTime = System.currentTimeMillis();
    if(cachedQuality == null || (currentTime - qualityCacheTime) > 300000) { // 5分钟缓存
        cachedQuality = 文字("0", "音乐质量", "质量");
        if(cachedQuality.equals("")) cachedQuality = "m4a";
        qualityCacheTime = currentTime;
    }
    return cachedQuality;
}

// 初始化发送器映射
private static HashMap senderMap = null;

private static void initSenderMap() {
    if(senderMap == null) {
        senderMap = new HashMap();
        
        Object wySender = new MusicSender() {
            public void sendTroop(String qun, String title, String desc, String url, String audio, String img) {
                sendMusic("wy", qun, "", 1, title, desc, url, audio, img, 2);
            }
            public void sendFriend(String qun, String title, String desc, String url, String audio, String img) {
                sendMusic("wy", qun, "", 0, title, desc, url, audio, img, 1);
            }
        };
        senderMap.put("网易", wySender);
        
        Object qqSender = new MusicSender() {
            public void sendTroop(String qun, String title, String desc, String url, String audio, String img) {
                sendMusic("qq", qun, "", 1, title, desc, url, audio, img, 2);
            }
            public void sendFriend(String qun, String title, String desc, String url, String audio, String img) {
                sendMusic("qq", qun, "", 0, title, desc, url, audio, img, 1);
            }
        };
        senderMap.put("QQ", qqSender);
        
        Object kgSender = new MusicSender() {
            public void sendTroop(String qun, String title, String desc, String url, String audio, String img) {
                sendMusic("kg", qun, "", 1, title, desc, url, audio, img, 2);
            }
            public void sendFriend(String qun, String title, String desc, String url, String audio, String img) {
                sendMusic("kg", qun, "", 0, title, desc, url, audio, img, 1);
            }
        };
        senderMap.put("酷狗", kgSender);
        
        Object mgSender = new MusicSender() {
            public void sendTroop(String qun, String title, String desc, String url, String audio, String img) {
                sendMusic("mg", qun, "", 1, title, desc, url, audio, img, 2);
            }
            public void sendFriend(String qun, String title, String desc, String url, String audio, String img) {
                sendMusic("mg", qun, "", 0, title, desc, url, audio, img, 1);
            }
        };
        senderMap.put("咪咕", mgSender);
        
        Object kgmvSender = new MusicSender() {
            public void sendTroop(String qun, String title, String desc, String url, String audio, String img) {
                sendMusic("kg", qun, "", 1, title, desc, url, audio, img, 2);
            }
            public void sendFriend(String qun, String title, String desc, String url, String audio, String img) {
                sendMusic("kg", qun, "", 0, title, desc, url, audio, img, 1);
            }
        };
        senderMap.put("酷狗MV", kgmvSender);
        
        Object kwSender = new MusicSender() {
            public void sendTroop(String qun, String title, String desc, String url, String audio, String img) {
                sendMusic("kw", qun, "", 1, title, desc, url, audio, img, 2);
            }
            public void sendFriend(String qun, String title, String desc, String url, String audio, String img) {
                sendMusic("kw", qun, "", 0, title, desc, url, audio, img, 1);
            }
        };
        senderMap.put("酷我", kwSender);
        
        Object bdSender = new MusicSender() {
            public void sendTroop(String qun, String title, String desc, String url, String audio, String img) {
                sendMusic("bd", qun, "", 1, title, desc, url, audio, img, 2);
            }
            public void sendFriend(String qun, String title, String desc, String url, String audio, String img) {
                sendMusic("bd", qun, "", 0, title, desc, url, audio, img, 1);
            }
        };
        senderMap.put("波点", bdSender);
    }
}

public void sendMusic(String platform, String uin1, String uin2, int uintype, String title, String desc, String detail_url, String audio, String img, int mtype) {
    String package_name = "";
    String appid = "";
    String token = "";
    
    if(platform.equals("qq")) {
        package_name = "com.tencent.qqmusic";
        appid = "100497308";
        token = "cbd27cd7c861227d013a25b2d10f0799";
    } else if(platform.equals("wy")) {
        package_name = "com.netease.cloudmusic";
        appid = "100495085";
        token = "da6b069da1e2982db3e386233f68d76d";
    } else if(platform.equals("kg")) {
        package_name = "com.kugou.android";
        appid = "205141";
        token = "fe4a24d80fcf253a00676a808f62c2c6";
    } else if(platform.equals("kw")) {
        package_name = "cn.kuwo.player";
        appid = "100243533";
        token = "a6b745bf24a2c277527716f6f36eb68d";
    } else if(platform.equals("bd")) {
        package_name = "cn.wenyu.bodian";
        appid = "101904722";
        token = "5380ca99d2bea3173b0b6e52cff44b45";
    } else if(platform.equals("mg")) {
        package_name = "cmccwm.mobilemusic";
        appid = "1101053067";
        token = "6cdc72a439cef99a3418d2a78aa28c73";
    } else if(platform.equals("QQ")) {
        package_name = "com.tencent.mobileqq";
        appid = "35055";
        token = "a6b745bf24a2c277527716f6f36eb68d";
    }
    
    makeMusicCard(title, desc, detail_url, img, audio, uin1, mtype, Long.parseLong(appid), package_name, token);
}

public void qqsendTroopMusic(String qun, String title, String desc, String detail_url, String audio, String img) {
    sendMusic("qq", qun, "", 1, title, desc, detail_url, audio, img, 2);
}

public void qqsendFriendMusic(String qun, String title, String desc, String detail_url, String audio, String img) {
    sendMusic("qq", qun, "", 0, title, desc, detail_url, audio, img, 1);
}

public void wysendTroopMusic(String qun, String title, String desc, String detail_url, String audio, String img) {
    sendMusic("wy", qun, "", 1, title, desc, detail_url, audio, img, 2);
}

public void wysendFriendMusic(String qun, String title, String desc, String detail_url, String audio, String img) {
    sendMusic("wy", qun, "", 0, title, desc, detail_url, audio, img, 1);
}

public void kgsendTroopMusic(String qun, String title, String desc, String detail_url, String audio, String img) {
    sendMusic("kg", qun, "", 1, title, desc, detail_url, audio, img, 2);
}

public void kgsendFriendMusic(String qun, String title, String desc, String detail_url, String audio, String img) {
    sendMusic("kg", qun, "", 0, title, desc, detail_url, audio, img, 1);
}

public void kwsendTroopMusic(String qun, String title, String desc, String detail_url, String audio, String img) {
    sendMusic("kw", qun, "", 1, title, desc, detail_url, audio, img, 2);
}

public void kwsendFriendMusic(String qun, String title, String desc, String detail_url, String audio, String img) {
    sendMusic("kw", qun, "", 0, title, desc, detail_url, audio, img, 1);
}

public void bdsendTroopMusic(String qun, String title, String desc, String detail_url, String audio, String img) {
    sendMusic("bd", qun, "", 1, title, desc, detail_url, audio, img, 2);
}

public void bdsendFriendMusic(String qun, String title, String desc, String detail_url, String audio, String img) {
    sendMusic("bd", qun, "", 0, title, desc, detail_url, audio, img, 1);
}

public void mgsendTroopMusic(String qun, String title, String desc, String detail_url, String audio, String img) {
    sendMusic("mg", qun, "", 1, title, desc, detail_url, audio, img, 2);
}

public void mgsendFriendMusic(String qun, String title, String desc, String detail_url, String audio, String img) {
    sendMusic("mg", qun, "", 0, title, desc, detail_url, audio, img, 1);
}

// 异步文件下载回调接口
interface DownloadCallback {
    void onComplete(boolean success);
}

// 异步下载文件
private void downloadFileAsync(String url, String filePath, DownloadCallback callback) {
    new Thread(new Runnable() {
        public void run() {
            try {
                DownloadToFile(url, filePath);
                callback.onComplete(true);
            } catch(Exception e) {
                callback.onComplete(false);
            }
        }
    }).start();
}

// 清理临时文件
private void cleanupFile(String filePath) {
    new Thread(new Runnable() {
        public void run() {
            try {
                Thread.sleep(6000);
                sc(filePath);
            } catch(Exception e) {
            }
        }
    }).start();
}

private void handleError(String qun, String errorMsg, int mtype) {
    sendMsg(qun, "操作失败: " + errorMsg, mtype);
}

// 同步发送音乐
private void sendMusicSync(String qun, String song, String singer, String url, String music, String cover, String app, String type, int mtype) {
    String Quality = getCachedQuality();
    String processedMusic = music.replace("&API=ly.aa.cab", "");
    
    try {
        if(type.equals("卡片")) {
            initSenderMap();
            Object sender = senderMap.get(app);
            if(sender != null) {
                if(mtype == 2) {
                    sender.sendTroop(qun, song, singer, url, processedMusic, cover);
                } else if(mtype == 1) {
                    sender.sendFriend(qun, song, singer, url, processedMusic, cover);
                }
            } else {
                // 默认使用QQ
                if(mtype == 2) {
                    qqsendTroopMusic(qun, song, singer, url, processedMusic, cover);
                } else if(mtype == 1) {
                    qqsendFriendMusic(qun, song, singer, url, processedMusic, cover);
                }
            }
        } else if(type.equals("语音")) {
            sendPtt(qun, processedMusic, mtype);
        } else if(type.equals("链接")) {
            sendMsg(qun, "[pic=" + cover + "]\n" + song + "——" + singer + "\n" + processedMusic.replace("\\/", "/"), mtype);
        } else if(type.equals("下载")) {
            String filePath = ColdRainPath + "下载/" + song + "." + Quality;
            downloadFileAsync(processedMusic, filePath, new DownloadCallback() {
                public void onComplete(boolean success) {
                    if(success) {
                        sendMsg(qun, "[pic=" + cover + "]\n" + song + "——" + singer + "\n\n已下载到" + filePath, mtype);
                    } else {
                        handleError(qun, "下载失败", mtype);
                    }
                }
            });
        } else if(type.equals("空间")) {
            shareToQzone(qun, song, singer, processedMusic, cover, mtype);
        } else if(type.equals("播放")) {
            Toast("正在启动QQ/TIM自带播放器");
            playMusicByQQ(song, singer, url, processedMusic, cover);
        }
    } catch(Exception e) {
        handleError(qun, "音乐发送失败: " + e.getMessage(), mtype);
    }
}

// 分享到QQ空间
private void shareToQzone(String qun, String song, String singer, String music, String cover, int mtype) {
    new Thread(new Runnable() {
        public void run() {
            try {
                String url2 = httppost2("https://sns.qzone.qq.com/cgi-bin/qzshare/cgi_qzshareadd_url?g_tk="+GetGTK(getPskey("qzone.qq.com"))+"&qzonetoken=","uin=o"+myUin+";p_uin=o"+myUin+";skey="+getSkey()+";p_skey="+getPskey("qzone.qq.com"),"where=0&entryuin="+myUin+"&spaceuin="+myUin+"&title="+song+"&summary="+singer+"&token="+GetGTK(getPskey("qzone.qq.com"))+"&sendparam=&description="+song+"——"+singer+"&type=4&url="+URL(music,1)+"&site=&to=&share2weibo=0&fupdate=1&notice=1&pics="+cover,"https://sns.qzone.qq.com/cgi-bin/qzshare/cgi_qzshare_onekey?desc=%E2%99%AA%E6%88%91%E6%AD%A3%E5%9C%A8%E6%94%B6%E5%90%AC%E3%80%8A%E6%BA%AF%20(Reverse)feat.%20%E9%A9%AC%E5%90%9F%E5%90%9F%E3%80%8B%EF%BC%88%E6%9D%A5%E8%87%AA%40QQ%E9%9F%B3%E4%B9%90%EF%BC%89&url=https%3A%2F%2Fi.y.qq.com%2Fv8%2Fplaysong.html%3Fsongid%3D214920523%23webchat_redirect&desc=%E2%99%AA%E6%88%91%E6%AD%A3%E5%9C%A8%E6%94%B6%E5%90%AC%E3%80%8A%E6%BA%AF%20(Reverse)feat.%20%E9%A9%AC%E5%90%9F%E5%90%9F%E3%80%8B%EF%BC%88%E6%9D%A5%E8%87%AA%40QQ%E9%9F%B3%E4%B9%90%EF%BC%89&summary=CORSAK%E8%83%A1%E6%A2%A6%E5%91%A8%7C%E9%A9%AC%E5%90%9F%E5%90%9F&title=%E5%88%86%E4%BA%AB%E6%AD%8C%E6%9B%B2%E3%80%8A%E6%BA%AF%20(Reverse)feat.%20%E9%A9%AC%E5%90%9F%E5%90%9F%E3%80%8B&pics=%2F%2Fy.qq.com%2Fmusic%2Fphoto_new%2FT002R300x300M000002yOiJ347ly2o_1.jpg%3Fmax_age%3D2592000");
                if(url2.contains("\"code\":0,")) {
                    sendMsg(qun,"已发送到空间",mtype);
                } else {
                    sendMsg(qun,"发送失败，请手动发送\nhttps://sns.qzone.qq.com/cgi-bin/qzshare/cgi_qzshare_onekey?where=0&entryuin="+myUin+"&spaceuin="+myUin+"&title="+song+"&summary="+singer+"&token=&sendparam=&description="+song+"——"+singer+"&type=4&url="+URL(music,1)+"&site=&to=&share2weibo=0&fupdate=1&notice=1&pics="+cover,mtype);
                }
            } catch(Exception e) {
                handleError(qun, "分享到空间失败: " + e.getMessage(), mtype);
            }
        }
    }).start();
}

public void sendMusic(String qun, String song, String singer, String url, String music, String cover, String app, String type, int mtype) {
    String Quality = getCachedQuality();
    String processedMusic = music.replace("&API=ly.aa.cab", "");
    
    if(type.equals("文件")) {
        String filePath = ColdRainPath + "下载/" + song + "." + Quality;
        downloadFileAsync(processedMusic, filePath, new DownloadCallback() {
            public void onComplete(boolean success) {
                if(success) {
                    sendFile(qun, filePath, mtype);
                    cleanupFile(filePath);
                } else {
                    handleError(qun, "下载失败", mtype);
                }
            }
        });
    } else {
        sendMusicSync(qun, song, singer, url, processedMusic, cover, app, type, mtype);
    }
}
