public void 解析菜单(Object Yu) {
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
                if(quntext.equals("开启视频解析")) {
                    if(qq.equals(uin)||读(qun,"代管",uin)==1||读("0","代管",uin)==1) {
                        写(qun,"视频解析","开关",1);
                        String menu="已开启本聊天视频解析";
                        sendText(data,menu);
                    }
                }
                if(quntext.equals("关闭视频解析")) {
                    if(qq.equals(uin)||读(qun,"代管",uin)==1||读("0","代管",uin)==1) {
                        写(qun,"视频解析","开关",0);
                        String menu="已关闭本聊天视频解析";
                        sendText(data,menu);
                    }
                }
                if(quntext.equals("开启音乐解析")) {
                    if(qq.equals(uin)||读(qun,"代管",uin)==1||读("0","代管",uin)==1) {
                        写(qun,"音乐解析","开关",1);
                        String menu="已开启本聊天音乐解析";
                        sendText(data,menu);
                    }
                }
                if(quntext.equals("关闭音乐解析")) {
                    if(qq.equals(uin)||读(qun,"代管",uin)==1||读("0","代管",uin)==1) {
                        写(qun,"音乐解析","开关",0);
                        String menu="已关闭本聊天音乐解析";
                        sendText(data,menu);
                    }
                }
                if(quntext.equals("解析菜单")) {
                    String lengyu="";
                    String wenqing="";
                    if(读(qun,"视频解析","开关")==1) lengyu="开";
                    else lengyu="关";
                    if(读(qun,"音乐解析","开关")==1) wenqing="开";
                    else wenqing="关";
                    String menu = "解析菜单:\n开启/关闭视频解析\n开启/关闭音乐解析\nTips:1.开启后直接发链接(可加文字)\n仅支持快手，抖音，小红书，哔哩哔哩，皮皮虾，西瓜视频的视频/图集解析\n2.支持QQ小世界解析(转发卡片)\n3.支持微信公众号/QQ频道图集/视频解析\n4.支持QQ/网易/酷我/波点/酷狗/汽水解析\n\n视频解析("+lengyu+")\n音乐解析("+wenqing+")";
                    sendText(data,menu);
                }
                if(读(qun,"视频解析","开关")==1) {
                    if(quntext.contains("https://b23.tv/")) {
                        String msgs=fetchRedirectUrl(findRealUrl(quntext));
                        try {
                            //String id=findIDByUrl(msgs);
                            String url=get(myApi+"API/ly/bilibili_jx.php?url="+msgs);
                            JSONObject json = new JSONObject(url);
                            String msg=json.getString("msg");
                            if(msg.equals("获取成功")) {
                                JSONObject json1 = json.getJSONObject("data");
                                String cover=json1.getString("cover");
                                String video=json1.getString("video");
                                String title=json1.getString("title");
                                String duration=json1.getJSONObject("origin").getString("duration_format");
                                String author=json.getJSONObject("author").getString("name");
                                String desc=json1.getString("desc");
                                String publish_time=json1.getString("publish_time");
                                sendText(data,"[pic="+cover+"]\n标题:"+title+"\n作者:"+author+"\n时长:"+duration+"\n发布时间:"+publish_time+"\n视频发送中...");
                                sendVideo(mtype,qun,video);
                            }
                            else {
                                sendText(data,"出现错误:"+json.getString("msg"));
                            }
                        }
                        catch(e) {
                            sendText(data,"出现错误:"+e);
                        }
                    }
                    if(quntext.contains("http://xhslink.com/")) {
                        String sl=findRealUrl(quntext);
                        String url=myApi+"/API/spjx/api.php?url="+sl;
                        String sj=get(url);
                        JSONObject json = new JSONObject(sj);
                        String msg=json.getString("msg");
                        if(msg.equals("获取成功")) {
                            String data1=json.getString("data");
                            if(!sj.contains("images")) {
                                JSONObject json1 = new JSONObject(data1);
                                String cover=json1.getString("cover");
                                String video=json1.getString("url");
                                String title=json1.getString("title");
                                sendText(data,"[pic="+cover+"]\n"+title);
                                sendVideo(mtype,qun,video);
                            }
                            else if(sj.contains("images")) {
                                JSONObject json1 = new JSONObject(data1);
                                String cover=json1.getString("cover");
                                JSONArray ttp=json1.getJSONArray("images");
                                String title=json1.getString("title");
                                String tp2="";
                                for (int i = 0;
                                i < ttp.length();
                                i++) {
                                    String tp1 = ttp.getString(i);
                                    tp2 +="[pic="+tp1+"]";
                                }
                                sendMsg(qun,"图片来咯～"+tp2+"标题:"+title,mtype);
                            }
                        }
                        else {
                            try {
                                String url=get(text);
                                int index = url.lastIndexOf("<a href=\"");
                                String text2 = url.substring(index + 9);
                                int rd = text2.indexOf("\">");
                                String re = text2.substring(0,rd);
                                String url2=get(re);
                                int index = url2.lastIndexOf("LAUNCHER_SSR_STORE_PAGE_DATA\":");
                                String text3 = url2.substring(index + 30);
                                int rdd = text3.indexOf("}</script>");
                                String ree = text3.substring(0,rdd);
                                JSONObject json = new JSONObject(ree);
                                String noteData=json.getString("noteData");
                                JSONObject json1 = new JSONObject(noteData);
                                String title=json1.getString("title");
                                //标题
                                String cover=json1.getString("cover");
                                JSONObject json2 = new JSONObject(cover);
                                String coverurl="http:"+u解(json2.getString("url"));
                                //封面链接
                                String generatedTitle=json1.getString("generatedTitle");
                                //标题1
                                if(noteData.contains("\"imageList\":[{\"url\":")) {
                                    String result="";
                                    JSONArray imageList=json1.getJSONArray("imageList");
                                    for(int q=0;
                                    q<imageList.length();
                                    q++) {
                                        JSONObject List=imageList.getJSONObject(q);
                                        String indexUrl=List.getString("url");
                                        result+="[pic=http:"+indexUrl+"]\n";
                                    }
                                    sendText(data,"标题:"+title+"\n"+result);
                                }
                                else {
                                    String video=json1.getString("video");
                                    JSONObject json3 = new JSONObject(video);
                                    String videourl=json3.getString("url");
                                    //视频链接
                                    String generatedText=json3.getString("generatedText");
                                    //标题2
                                    sendText(data,"标题:"+title+"\n标题2:"+generatedTitle+"\n标题3:"+generatedText+"[pic="+coverurl+"]");
                                    sendVideo(mtype,qun,videourl);
                                }
                            }
                            catch(e) {
                                sendText(data,"出现错误:"+e);
                            }
                        }
                    }
                    if(quntext.contains("https://v.douyin.com/")) {
                        String sl=findRealUrl(quntext);
                        //2.89 复制打开抖音，看看【人猿泰山的作品】青春没有售价 精灵坠崖我一跃而下# 蹦极 # 佩妮... https://v.douyin.com/YLwM0A5dEnw/ EuS:/ 05/24 k@C.UY
                        String url=myApi+"/API/ly/dyjx.php?url="+sl;
                        String sj=get(url);
                        JSONObject json = new JSONObject(sj);
                        String msg=json.getString("msg");
                        if(msg.equals("获取成功")) {
                            String type=json.getString("type");
                            String data1=json.getString("data");
                            if(type.equals("视频")) {
                                JSONObject json1 = new JSONObject(data1);
                                String cover=json1.getString("cover");
                                String video=json1.getString("url");
                                String title=json1.getString("desc");
                                JSONObject json2=json1.getJSONObject("count");
                                long like=json2.getLong("like");
                                long comment=json2.getLong("comment");
                                long share=json2.getLong("share");
                                long collect=json2.getLong("collect");
                                sendText(data,"[pic="+cover+"]\n标题:"+title+"\n作者:"+json1.getJSONObject("author").getString("name")+"\n喜欢数:"+like+"\n评论数:"+comment+"\n分享数:"+share+"\n收藏数:"+collect+"\n视频发送中...");
                                sendVideo(mtype,qun,video);
                            }
                            else if(type.equals("图集")) {
                                JSONObject json1 = new JSONObject(data1);
                                String cover=json1.getString("cover");
                                JSONArray ttp=json1.getJSONArray("images");
                                String title=json1.getString("desc");
                                JSONObject json2=json1.getJSONObject("count");
                                long like=json2.getLong("like");
                                long comment=json2.getLong("comment");
                                long share=json2.getLong("share");
                                long collect=json2.getLong("collect");
                                sendText(data,"[pic="+cover+"]\n标题:"+title+"\n作者:"+json1.getJSONObject("author").getString("name")+"\n喜欢数:"+like+"\n评论数:"+comment+"\n分享数:"+share+"\n收藏数:"+collect+"\n图集发送中...");
                                String tp2="";
                                for (int i = 0;
                                i < ttp.length();
                                i++) {
                                    String tp1 = ttp.getString(i);
                                    tp2+="[pic="+u解(tp1)+"]";
                                }
                                sendMsg(qun,"图片来咯～"+tp2+"共"+ttp.length()+"张图",mtype);
                            }
                            else if(type.equals("视频集合")) {
                                JSONObject json1 = new JSONObject(data1);
                                String cover=json1.getString("cover");
                                String title=json1.getString("desc");
                                JSONObject json2=json1.getJSONObject("count");
                                long like=json2.getLong("like");
                                long comment=json2.getLong("comment");
                                long share=json2.getLong("share");
                                long collect=json2.getLong("collect");
                                JSONArray ttp=json1.getJSONArray("videos");
                                sendText(data,"[pic="+cover+"]\n标题:"+title+"\n作者:"+json1.getJSONObject("author").getString("name")+"\n喜欢数:"+like+"\n评论数:"+comment+"\n分享数:"+share+"\n收藏数:"+collect+"\n视频集合发送中...(共"+ttp.length()+"个视频)");
                                for (int i = 0;
                                i < ttp.length();
                                i++) {
                                    String tp1 = ttp.getString(i);
                                    sendVideo(mtype,qun,u解(tp1));
                                }
                            }
                        }
                        else {
                            sendText(data,sj);
                        }
                    }
                    if(data.originMsg.msgType==11) {
                        try {
                            JSONObject Json = new JSONObject(quntext);
                            String AppArkName =Json.getString("app");
                            if(AppArkName.equals("com.tencent.miniapp_01")) {
                                String appid=Json.getJSONObject("meta").getJSONObject("detail_1").getString("appid");
                                if(appid.equals("1108735743")) {
                                    String qqdocurl=Json.getJSONObject("meta").getJSONObject("detail_1").getString("qqdocurl");
                                    String url=myApi+"/API/spjx/api.php?url="+qqdocurl;
                                    String sj=get(url);
                                    JSONObject json = new JSONObject(sj);
                                    String msg=json.getString("msg");
                                    if(msg.equals("获取成功")) {
                                        String data1=json.getString("data");
                                        if(!sj.contains("images")) {
                                            JSONObject json1 = new JSONObject(data1);
                                            String cover=json1.getString("cover");
                                            String video=json1.getString("url");
                                            String title=json1.getString("title");
                                            sendText(data,"[pic="+cover+"]\n"+title);
                                            sendVideo(mtype,qun,video);
                                        }
                                        else if(sj.contains("images")) {
                                            JSONObject json1 = new JSONObject(data1);
                                            String cover=json1.getString("cover");
                                            JSONArray ttp=json1.getJSONArray("images");
                                            String title=json1.getString("title");
                                            String tp2="";
                                            for (int i = 0;
                                            i < ttp.length();
                                            i++) {
                                                String tp1 = ttp.getString(i);
                                                tp2 +="[pic="+tp1+"]";
                                            }
                                            sendMsg(qun,"图片来咯～"+tp2+"标题:"+title,mtype);
                                        }
                                    }
                                    else {
                                        sendText(data,sj);
                                    }
                                }
                                else if(appid.equals("1109937557")) {
                                    String msgs=fetchRedirectUrl(Json.getJSONObject("meta").getJSONObject("detail_1").getString("qqdocurl"));
                                    try {
                                        String id=findIDByUrl(msgs);
                                        String url=get("https://api.bilibili.com/x/web-interface/view/detail?aid=&bvid="+id);
                                        JSONObject json=new JSONObject(url);
                                        if(json.getInt("code")==0) {
                                            json=json.getJSONObject("data");
                                            JSONObject json2=json.getJSONObject("View");
                                            JSONObject json3=json2.getJSONObject("stat");
                                            JSONObject json4=json2.getJSONObject("owner");
                                            sendText(data,json2.getString("title")+"[pic="+json2.getString("pic")+"]\n作者:"+json4.getString("name")+"\n播放量:"+json3.optString("view")+"\n点赞量:"+json3.optString("like")+"\n正在输出视频:360p(流畅)\n时长:"+json2.optString("duration")+"秒");
                                            url=get("https://api.bilibili.com/x/player/playurl?cid="+json2.optString("cid")+"&avid=&bvid="+id+"&otype=json&platform=html5&type=mp4&html5=1");
                                            json=new JSONObject(url);
                                            if(json.getInt("code")==0) {
                                                sendVideo(mtype,qun,json.getJSONObject("data").getJSONArray("durl").getJSONObject(0).getString("url"));
                                            }
                                            else {
                                                sendText(data,"出现错误:"+json.getString("message"));
                                            }
                                        }
                                        else {
                                            sendText(data,"出现错误:"+json.getString("message"));
                                        }
                                    }
                                    catch(e) {
                                        sendText(data,"出现错误:"+e);
                                    }
                                }
                            }
                        }
                        catch(e) {
                        }
                    }
                    //{"ver":"1.0.0.19","prompt":"[QQ小程序]快手极速版","config":{"type":"normal","width":0,"height":0,"forward":1,"autoSize":0,"ctime":1712197603,"token":"56913d035bc5cad1d3c2fa4d6fe38b71"},"needShareCallBack":false,"app":"com.tencent.miniapp_01","view":"view_8C8E89B49BE609866298ADDFF2DBABA4","meta":{"detail_1":{"appid":"1108735743","appType":0,"title":"快手","desc":"快手极速版","icon":"https:\/\/open.gtimg.cn\/open\/app_icon\/07\/80\/53\/32\/1107805332_100_m.png?t=1711611908","preview":"ali2.a.yximgs.com\/bs2\/shareImage\/MINI_PROGRAM_PHOTO_SHARE_COVER_1_3xsjyaadk8gu7qq_1711283986531.jpg","url":"m.q.qq.com\/a\/s\/2412a6a8c16b63c09d95f19a45e4f104","scene":1036,"host":{"uin":3470558502,"nick":"Yu."},"shareTemplateId":"8C8E89B49BE609866298ADDFF2DBABA4","shareTemplateData":{},"qqdocurl":"https:\/\/v.kuaishou.com\/wnyxSs","showLittleTail":"","gamePoints":"","gamePointsUrl":""}}}
                    if(quntext.contains("https://v.kuaishou.com/")) {
                        String sl=findRealUrl(quntext);
                        String url=myApi+"/API/ly/ksjx.php?url="+sl;
                        String sj=get(url);
                        JSONObject json = new JSONObject(sj);
                        String msg=json.getString("msg");
                        if(msg.equals("获取成功")) {
                            String type=json.getString("type");
                            String data1=json.getString("data");
                            if(type.equals("视频")) {
                                JSONObject json1 = new JSONObject(data1);
                                String cover=json1.getString("cover");
                                String video=json1.getString("url");
                                String title=json1.getString("desc");
                                JSONObject json2=json1.getJSONObject("count");
                                long like=json2.getLong("like");
                                long comment=json2.getLong("comment");
                                long share=json2.getLong("share");
                                long collect=json2.getLong("collect");
                                sendText(data,"[pic="+cover+"]\n标题:"+title+"\n作者:"+json1.getJSONObject("author").getString("name")+"\n喜欢数:"+like+"\n评论数:"+comment+"\n分享数:"+share+"\n收藏数:"+collect+"\n视频发送中...");
                                sendVideo(mtype,qun,video);
                            }
                            else if(type.equals("图集")) {
                                JSONObject json1 = new JSONObject(data1);
                                String cover=json1.getString("cover");
                                JSONArray ttp=json1.getJSONArray("images");
                                String title=json1.getString("desc");
                                JSONObject json2=json1.getJSONObject("count");
                                long like=json2.getLong("like");
                                long comment=json2.getLong("comment");
                                long share=json2.getLong("share");
                                long collect=json2.getLong("collect");
                                sendText(data,"[pic="+cover+"]\n标题:"+title+"\n作者:"+json1.getJSONObject("author").getString("name")+"\n喜欢数:"+like+"\n评论数:"+comment+"\n分享数:"+share+"\n收藏数:"+collect+"\n图集发送中...");
                                String tp2="";
                                for (int i = 0;
                                i < ttp.length();
                                i++) {
                                    String tp1 = ttp.getString(i);
                                    tp2+="[pic="+u解(tp1)+"]";
                                }
                                sendMsg(qun,"图片来咯～"+tp2+"共"+ttp.length()+"张图",mtype);
                            }
                        }
                        else {
                            sendText(data,sj);
                        }
                    }
                    if(quntext.contains("https://h5.pipix.com/s/")) {
                        String sl=findRealUrl(quntext);
                        String url=myApi+"/API/spjx/api.php?url="+sl;
                        String sj=get(url);
                        JSONObject json = new JSONObject(sj);
                        String msg=json.getString("msg");
                        if(msg.equals("获取成功")) {
                            String data1=json.getString("data");
                            if(!sj.contains("images")) {
                                JSONObject json1 = new JSONObject(data1);
                                String cover=json1.getString("cover");
                                String video=json1.getString("url");
                                String title=json1.getString("title");
                                sendText(data,"[pic="+cover+"]\n"+title);
                                sendVideo(mtype,qun,video);
                            }
                            else if(sj.contains("images")) {
                                JSONObject json1 = new JSONObject(data1);
                                String cover=json1.getString("cover");
                                JSONArray ttp=json1.getJSONArray("images");
                                String title=json1.getString("title");
                                String tp2="";
                                for (int i = 0;
                                i < ttp.length();
                                i++) {
                                    String tp1 = ttp.getString(i);
                                    tp2 +="[pic="+tp1+"]";
                                }
                                sendMsg(qun,"图片来咯～"+tp2+"标题:"+title,mtype);
                            }
                        }
                        else {
                            sendText(data,sj);
                        }
                    }
                    if(quntext.contains("https://pd.qq.com/s/")) {
                        String sl=findRealUrl(quntext);
                        String url=myWeb+"pdjx.php?url="+sl;
                        String sj=get(url);
                        JSONObject json = new JSONObject(sj);
                        String msg=json.getString("msg");
                        if(msg.equals("获取成功")) {
                            String data1=json.getString("data");
                            String type=json.getString("type");
                            if(type.equals("视频")) {
                                JSONObject json1 = new JSONObject(data1);
                                String cover=json1.getString("cover");
                                String video=json1.getString("video");
                                String title=json1.getString("desc");
                                JSONObject json2=json1.getJSONObject("count");
                                long like=json2.getLong("like");
                                long comment=json2.getLong("comment");
                                long share=json2.getLong("share");
                                long view=json2.getLong("view");
                                sendText(data,"[pic="+cover+"]\n标题:"+title+"\n作者:"+json1.getJSONObject("author").getString("name")+"\n喜欢数:"+like+"\n评论数:"+comment+"\n分享数:"+share+"\n浏览数:"+view+"\n视频发送中...");
                                sendVideo(mtype,qun,video);
                            }
                            else if(type.equals("图集")) {
                                JSONObject json1 = new JSONObject(data1);
                                String cover=json1.getString("cover");
                                JSONArray ttp=json1.getJSONArray("images");
                                String title=json1.getString("desc");
                                JSONObject json2=json1.getJSONObject("count");
                                long like=json2.getLong("like");
                                long comment=json2.getLong("comment");
                                long share=json2.getLong("share");
                                long view=json2.getLong("view");
                                sendText(data,"[pic="+cover+"]\n标题:"+title+"\n作者:"+json1.getJSONObject("author").getString("name")+"\n喜欢数:"+like+"\n评论数:"+comment+"\n分享数:"+share+"\n浏览数:"+view+"\n图集发送中...");
                                String tp2="";
                                for (int i = 0;
                                i < ttp.length();
                                i++) {
                                    String tp1 = ttp.getString(i);
                                    tp2+="[pic="+u解(tp1)+"]";
                                }
                                sendMsg(qun,"图片来咯～"+tp2+"共"+ttp.length()+"张图",mtype);
                            }
                        }
                        else {
                            sendText(data,sj);
                        }
                    }
                    if(quntext.contains("https://mp.weixin.qq.com/s/")) {
                        String sl=findRealUrl(quntext);
                        String url=myWeb+"wxjx.php?url="+sl;
                        String sj=get(url);
                        JSONObject json = new JSONObject(sj);
                        String msg=json.getString("msg");
                        if(msg.equals("获取成功")) {
                            String data1=json.getString("data");
                            String type=json.getString("type");
                            if(type.equals("视频")) {
                                JSONObject json1 = new JSONObject(data1);
                                JSONArray ttp=json1.getJSONArray("video");
                                String title=json1.getString("title");
                                String desc=json1.getString("desc");
                                sendText(data,"标题:"+title+"\n文章内容:\n"+desc);
                                if(ttp.length()>1||!ttp.getString(0).contains(".mp4")) {
                                    sendText(data,"视频列表:\n"+getStringbyArray(ttp));
                                }
                                else {
                                    sendVideo(mtype,qun,ttp.getString(0));
                                }
                            }
                            else if(type.equals("图集")) {
                                JSONObject json1 = new JSONObject(data1);
                                JSONArray ttp=json1.getJSONArray("images");
                                String title=json1.getString("title");
                                String desc=json1.getString("desc");
                                String tp2="";
                                for (int i = 0;
                                i < ttp.length();
                                i++) {
                                    String tp1 = ttp.getString(i);
                                    tp2 +="[pic="+tp1+"]";
                                }
                                sendMsg(qun,"图片来咯～"+tp2+"标题:"+title+"\n文章内容:\n"+desc,mtype);
                            }
                            else if(type.equals("图集and视频")) {
                                JSONObject json1 = new JSONObject(data1);
                                JSONArray ttp=json1.getJSONArray("images");
                                String title=json1.getString("title");
                                String desc=json1.getString("desc");
                                String tp2="";
                                for (int i = 0;
                                i < ttp.length();
                                i++) {
                                    String tp1 = ttp.getString(i);
                                    tp2 +="[pic="+tp1+"]";
                                }
                                sendMsg(qun,"图片来咯～"+tp2+"标题:"+title+"\n文章内容:\n"+desc,mtype);
                                JSONArray ttp2=json1.getJSONArray("video");
                                if(ttp2.length()>1||!ttp2.getString(0).contains(".mp4")) {
                                    sendText(data,"视频列表:\n"+getStringbyArray(ttp2));
                                }
                                else {
                                    sendVideo(mtype,qun,ttp2.getString(0));
                                }
                            }
                        }
                        else {
                            sendText(data,sj);
                        }
                    }
                }
                if(读(qun,"音乐解析","开关")==1) {
                    if((quntext.contains("i.y.qq.com/v8/playsong.html")||quntext.contains("2.y.qq.com/n3/other/pages/playsong/index.html"))&&msgtype==1) {
                        String yyms=读(ColdRainPath+"data/"+qun+"点歌模式.txt");
                        if(yyms.equals("")) yyms="卡片";
                        String mid=findmid(quntext);
                        String url=get(myWeb+"qqmusic.php?mid="+mid+"&adaptive=1&type=");
                        JSONObject json = new JSONObject(url);
                        int code=json.getInt("code");
                        if(code==0) {
                            json = json.getJSONObject("data");
                            String music = json.getString("music");
                            String album_name = json.getString("album_name");
                            String song = json.getString("song");
                            String singer = json.getString("singer");
                            String cover = json.getString("cover");
                            String link="https://i2.y.qq.com/n3/other/pages/playsong/index.html?songmid="+json.get("mid");
                            sendText(data,"QQ音乐直链解析成功:\n『"+song+"——"+singer+"』\n"+music);
                            sendMusic(qun,song,singer,link,music,cover,"QQ",yyms,mtype);
                        }
                        else {
                            sendText(data,json.get("msg"));
                        }
                    }
                    if(quntext.contains("y.music.163.com/m/song")&&msgtype==1) {
                        String yyms=读(ColdRainPath+"data/"+qun+"点歌模式.txt");
                        if(yyms.equals("")) yyms="卡片";
                        String id=findid(quntext);
                        String url=get(myWeb+"wyjx.php?id="+id);
                        if(url.equals("访问网页失败")) {
                            sendText(data,url);
                        }
                        else {
                            String result="";
                            JSONObject json = new JSONObject(url);
                            int code=json.getInt("code");
                            if(code==0) {
                                json = json.getJSONObject("data");
                                String music = json.getString("music");
                                String album_name = json.getString("album_name");
                                String song = json.getString("song");
                                String singer = json.getString("singer");
                                sendText(data,"网易云音乐直链解析成功:\n『"+song+"——"+singer+"』\n"+music);
                                String cover = json.getString("cover");
                                String link="https://y.music.163.com/m/song?id="+json.get("id");
                                sendMusic(qun,song,singer,link,music,cover,"网易",yyms,mtype);
                            }
                            else {
                                sendText(data,json.get("msg"));
                            }
                        }
                    }
                    if(quntext.contains("163cn.tv/")&&msgtype==1) {
                        quntext = fetchRedirectUrl(matcherLinks(quntext));
                        String yyms=读(ColdRainPath+"data/"+qun+"点歌模式.txt");
                        if(yyms.equals("")) yyms="卡片";
                        String id=findid(quntext);
                        String url=get(myWeb+"wyjx.php?id="+id);
                        if(url.equals("访问网页失败")) {
                            sendText(data,url);
                        }
                        else {
                            String result="";
                            JSONObject json = new JSONObject(url);
                            int code=json.getInt("code");
                            if(code==0) {
                                json = json.getJSONObject("data");
                                String music = json.getString("music");
                                String album_name = json.getString("album_name");
                                String song = json.getString("song");
                                String singer = json.getString("singer");
                                sendText(data,"网易云音乐直链解析成功:\n『"+song+"——"+singer+"』\n"+music);
                                String cover = json.getString("cover");
                                String link="https://y.music.163.com/m/song?id="+json.get("id");
                                sendMusic(qun,song,singer,link,music,cover,"网易",yyms,mtype);
                            }
                            else {
                                sendText(data,json.get("msg"));
                            }
                        }
                    }
                    if(quntext.contains("c6.y.qq.com/base/fcgi-bin/u?__=")&&msgtype==1) {
                        quntext = fetchRedirectUrl(matcherLinks(quntext));
                        String yyms=读(ColdRainPath+"data/"+qun+"点歌模式.txt");
                        if(yyms.equals("")) yyms="卡片";
                        String mid=findmid(quntext);
                        String url=get(myWeb+"qqmusic.php?mid="+mid+"&adaptive=1&type=");
                        JSONObject json = new JSONObject(url);
                        int code=json.getInt("code");
                        if(code==0) {
                            json = json.getJSONObject("data");
                            String music = json.getString("music");
                            String album_name = json.getString("album_name");
                            String song = json.getString("song");
                            String singer = json.getString("singer");
                            String cover = json.getString("cover");
                            String link="https://i.y.qq.com/v8/playsong.html?songmid="+json.get("mid");
                            sendText(data,"QQ音乐直链解析成功:\n『"+song+"——"+singer+"』\n"+music);
                            sendMusic(qun,song,singer,link,music,cover,"QQ",yyms,mtype);
                        }
                        else {
                            sendText(data,json.get("msg"));
                        }
                    }
                    if((quntext.contains("m.kuwo.cn/yinyue/")||quntext.contains("m.kuwo.cn/h5app/single/")||quntext.contains("m.kuwo.cn/newh5app/play_detail/"))&&msgtype==1) {
                        quntext = fetchRedirectUrl(matcherLinks(quntext));
                        String yyms=读(ColdRainPath+"data/"+qun+"点歌模式.txt");
                        if(yyms.equals("")) yyms="卡片";
                        String mid=findkwid(quntext);
                        String url=get(myWeb+"kwmusic.php?id="+mid+"&adaptive=1&type=");
                        JSONObject json = new JSONObject(url);
                        int code=json.getInt("code");
                        if(code==0) {
                            json = json.getJSONObject("data");
                            String music = json.getString("music");
                            String album_name = json.getString("album_name");
                            String song = json.getString("song");
                            String singer = json.getString("singer");
                            String cover = json.getString("cover");
                            String link="http://m.kuwo.cn/newh5app/play_detail/"+json.getString("id")+"?f=arphone&t=usercopy&isstar=0&loginuid=/V+ecziU4HXp6BTRJar/4w==";
                            sendText(data,"酷我音乐直链解析成功:\n『"+song+"——"+singer+"』\n"+music);
                            sendMusic(qun,song,singer,link,music,cover,"波点",yyms,mtype);
                        }
                        else {
                            sendText(data,json.get("msg"));
                        }
                    }
                    if(quntext.contains("h5app.kuwo.cn/m/bodian/playMusic.html")&&msgtype==1) {
                        quntext = fetchRedirectUrl(matcherLinks(quntext));
                        String yyms=读(ColdRainPath+"data/"+qun+"点歌模式.txt");
                        if(yyms.equals("")) yyms="卡片";
                        String mid=findbdid(quntext);
                        String url=get(myWeb+"bdmusic.php?id="+mid+"&adaptive=1&type=");
                        JSONObject json = new JSONObject(url);
                        int code=json.getInt("code");
                        if(code==0) {
                            json = json.getJSONObject("data");
                            String music = json.getString("music");
                            String album_name = json.getString("album_name");
                            String song = json.getString("song");
                            String singer = json.getString("singer");
                            String cover = json.getString("cover");
                            String link="https://h5app.kuwo.cn/m/bodian/playMusic.html?uid=-1&musicId="+json.getString("id");
                            sendText(data,"波点音乐直链解析成功:\n『"+song+"——"+singer+"』\n"+music);
                            sendMusic(qun,song,singer,link,music,cover,"波点",yyms,mtype);
                        }
                        else {
                            sendText(data,json.get("msg"));
                        }
                    }
                    if((quntext.contains("https://t1.kugou.com/")||quntext.contains("kugou.com/song.html?id="))&&msgtype==1) {
                        quntext = fetchRedirectUrl(matcherLinks(quntext));
                        String yyms=读(ColdRainPath+"data/"+qun+"点歌模式.txt");
                        if(yyms.equals("")) yyms="卡片";
                        String mid=findhash(quntext);
                        String url=get(myWeb+"kgmusic.php?hash="+mid+"&adaptive=1&type=");
                        JSONObject json = new JSONObject(url);
                        int code=json.getInt("code");
                        if(code==0) {
                            json = json.getJSONObject("data");
                            String music = json.getString("music");
                            String album_name = json.getString("album_name");
                            String song = json.getString("song");
                            String singer = json.getString("singer");
                            String cover = json.getString("cover");
                            String link = json.getString("link");
                            sendText(data,"酷狗音乐直链解析成功:\n『"+song+"——"+singer+"』\n"+music);
                            sendMusic(qun,song,singer,link,music,cover,"酷狗",yyms,mtype);
                        }
                        else {
                            sendText(data,json.get("msg"));
                        }
                    }
                    if(quntext.contains("qishui.douyin.com/s/")&&msgtype==1) {
                        quntext = fetchRedirectUrl(matcherLinks(quntext));
                        String yyms=读(ColdRainPath+"data/"+qun+"点歌模式.txt");
                        if(yyms.equals("")) yyms="卡片";
                        String mid=findid(quntext);
                        String url=get("https://beta-luna.douyin.com/luna/h5/seo_track?track_id="+mid+"&device_platform=web");
                        JSONObject json = new JSONObject(url);
                        String code=json.getJSONObject("status_info").optString("status_msg");
                        if(code.equals("")) {
                            JSONObject json2 = json.getJSONObject("seo_track").getJSONObject("track");
                            String json3s = json.getJSONObject("track_player").getString("video_model");
                            JSONObject json3 = new JSONObject(json3s);
                            String music = json3.getJSONArray("video_list").getJSONObject(0).getString("main_url");
                            String album_name = json2.getJSONObject("album").getString("name");
                            String song = json2.getString("name");
                            String singer = json2.getJSONArray("artists").getJSONObject(0).optString("name");
                            if(singer==null||singer.isEmpty()) {
                                singer = json2.getJSONArray("artists").getJSONObject(0).getJSONObject("user_info").getString("nickname");
                            }
                            String cover = "https://p3-luna.douyinpic.com/img/"+json2.getJSONObject("album").getJSONObject("url_cover").getString("uri")+"~c5_375x375.jpg";
                            String link = "https://music.douyin.com/qishui/share/track?track_id="+id+"&hybrid_sdk_version=bullet&auto_play_bgm=1";
                            if(yyms.equals("卡片")) {
                                sendText(data,"[pic="+cover+"]汽水音乐直链解析成功:\n『"+song+"——"+singer+"』\n由于汽水音乐未接入腾讯appid，所以用文字代替卡片发送\n"+music);
                            }
                            else {
                                sendMusic(qun,song,singer,link,music,cover,"波点",yyms,mtype);
                            }
                        }
                        else {
                            String url=get(quntext);
                            int index = url.lastIndexOf("_ROUTER_DATA = ");
                            String text = url.substring(index + "_ROUTER_DATA = ".length());
                            int rd = text.indexOf("};");
                            String re = text.substring(0,rd+1);
                            JSONObject json = new JSONObject(re);
                            json = json.getJSONObject("loaderData").getJSONObject("ugc_video_page").getJSONObject("videoOptions");
                            if(json.getInt("status_code")==0) {
                                String video = json.getString("url");
                                String song = json.getString("videoName");
                                String singer = json.getString("artistName");
                                String cover = json.getString("coverURL");
                                String duration =json.optString("duration");
                                sendText(data,"[pic="+cover+"]汽水视频直链解析成功！\n标题:"+song+"\n作者"+singer+"\n时长:"+duration+"秒");
                                sendVideo(mtype,qun,video);
                            }
                            else {
                                sendText(data, "解析汽水失败");
                            }
                        }
                    }
                }
            }
        }
    }
    ).start();
}
public String findQQByWeZone(String url)
{
    Matcher matcher = Pattern.compile("&xsj_author_uin=(.*?)&").matcher(url.replace("\\/", "/"));
    if(matcher.find())
    {
        String QQ = matcher.group(1);
        return "" + QQ;
    }
    else
    {
        return "获取QQ失败";
    }
}
public String findSender(String url)
{
    Matcher matcher = Pattern.compile("&xsj_from_uin=(.*?)&").matcher(url.replace("\\/", "/"));
    if(matcher.find())
    {
        String QQ = matcher.group(1);
        return "" + QQ;
    }
    else
    {
        return "获取QQ失败";
    }
}
public long findCreateTime(String url)
{
    Matcher matcher = Pattern.compile("&createtime=(.*?)&").matcher(url.replace("\\/", "/"));
    if(matcher.find())
    {
        String createtime = matcher.group(1);
        return Long.parseLong(createtime);
    }
    else
    {
        return 0;
    }
}
//https://www.bilibili.com/video/BV1HrzZYBE5t?-Arouter=story&buvid=XU1A58E1355F7BF7CAAB69EE7E55CD84EC80E&from_spmid=tm.recommend.0.0&is_story_h5=true&mid=Y9XdNuBcw3ZT2SZups1UMn8FTQ%2FSZMtL1rElX6M3iMo%3D&p=1&plat_id=163&share_from=ugc&share_medium=android&share_plat=android&share_session_id=284dbd93-8f15-4eb1-b620-9e0c29020ec1&share_source=QQ&share_tag=s_i&spmid=main.ugc-video-detail-vertical.0.0&timestamp=1737731784&unique_k=b8ymk1l&up_id=3546655032871394
public String findIDByUrl(String url)
{
    Matcher matcher = Pattern.compile("bilibili.com/video/(.*?)\\?").matcher(url.replace("\\/", "/"));
    if(matcher.find())
    {
        String ID = matcher.group(1);
        return ID+"";
    }
    else
    {
        return "获取ID失败";
    }
}
//mqqapi://qcircle/opendetail?cover_pic_url=https%253A%252F%252Fworldtj.photo.store.qq.com%252Fpsc%253F%252Fworld%252F0e7PjWG9ceFfrsX9JhSrpzPhLUS84mPRFkCVW25wACeeNCSM44KaFhDwgynW%252AlnepteSfT9FWLup1XJc%252AVecQ3FTMx%252AGt8cNIWNrBUA0U88%2521%252Fb%2526bo%253DOASgBTgEoAURHyg%2521%2526ek%253D1%2526tl%253D1%2526tm%253D1745110744%2526vuin%253Dc8a6fdec37f83e00%2526wm_text%253DQOKApg%2521%2521%2526h5%253D1440%2526w5%253D1080&createtime=1678096730&euid=0x01_213d173e9604c1fdaa465b7b55787245&feedid=B_h5ab905649c6f0800e3oPdPARc38U0X5c&feedtype=0&from=6&from_euid=0x01_56a8ffc7a7e6409f30a9931fcdb16eba&getfeedlist=1&is_feed_detail=1&is_middle_page=1&issinglefeed=1&pageid=69&play_url=http%253A%252F%252Fcirclevideo2.photo.qq.com%252F50040_0b53kj4tgmyfnaaivaiadjsh2uwdgmdakrca.f102100.mp4%253Fcvkey%253DYrbPP2TIL5wv4djEp52DOuVWqBWvqZvJMmFzwtuBIdQ9xfDsXTQEFHKgWJ8gqQeEFc4KXDnDU%25252Fp3N9FsCjIBtyWQZrrFz7dacndZf7gAjoAa176nkNT0Q4fEF3%25252F5i7TR%25252FDSnFXur7H5e%25252BOlQkPs5JA%25253D%25253D%2526dis_k%253D9f9a030c1a99f130ba4ffe17af6149b7%2526dis_t%253D1745110744%2526os%253D1%2526traceid%253D1431136407_0420085903527_60751%2526vuin%253Dc8a6fdec37f83e00&sharecategory=2&shareentrance=1&shareuin=1431136407&showhomebtn=1&sourcetype=15&timestamp=1745110744&transdata=%257B%2522feedid%2522%253A%2522B_h5ab905649c6f0800e3oPdPARc38U0X5c%2522%252C%2522uid%2522%253A%25220x01_213d173e9604c1fdaa465b7b55787245%2522%252C%2522ctime%2522%253A1678096730%252C%2522sharedid%2522%253A%25220x01_56a8ffc7a7e6409f30a9931fcdb16eba%2522%252C%2522recomContentID%2522%253A0%252C%2522fromtagname%2522%253A%2522%2522%252C%2522subpagetype%2522%253A0%257D&type=3&uin=863795028&v_height=1280&v_width=720&xsj_author_uin=863795028&xsj_custom_pgid=pg_xsj_share_mid_page&xsj_feed_id=B_h5ab905649c6f0800e3oPdPARc38U0X5c&xsj_from_uin=1431136407&xsj_main_entrance=qq_aio&xsj_sub_entrance=feed_details_and_rec&secretid=v2&sign=NpwseMnEZFMivTUEdUtgIA==
public String findRealUrl(String url) {
    try {
        String[] urls=url.split(" ");
        url="";
        for(String re:urls) {
            if(re.contains("v.douyin.com")||re.contains("v.kuaishou.com")||re.contains("b23.tv")||re.contains("xhslink.com")||re.contains("h5.pipix.com")||re.contains("mp.weixin.qq.com")||re.contains("pd.qq.com")) {
                re=re.substring(re.indexOf("http"));
                url=re;
                break;
            }
        }
        if(url.isEmpty()) {
            return null;
        }
        else {
            return matcherLinks(url);
        }
    }
    catch(e) {
        return null;
    }
}
public String getStringbyArray(JSONArray ttp) {
    String tp2="";
    for (int i = 0;
    i < ttp.length();
    i++) {
        String tp1 = ttp.getString(i);
        tp2+=tp1+"\n";
    }
    return tp2;
}
public String findmid(String url) {
    try {
        Pattern pattern = Pattern.compile("songmid=([^&]+)");
        Matcher matcher = pattern.matcher(url);
        if (matcher.find()) {
            String songmid = matcher.group(1);
            return songmid;
        }
        else {
            return null;
        }
    }
    catch(e) {
        return null;
    }
}
public String findid(String url) {
    try {
        Pattern pattern = Pattern.compile("id=([^&]+)");
        Matcher matcher = pattern.matcher(url);
        if (matcher.find()) {
            String songmid = matcher.group(1);
            return songmid;
        }
        else {
            return null;
        }
    }
    catch(e) {
        return null;
    }
}
import java.util.regex.*;
public String matcherLinks(String text) {
    try {
        String regex = "https?://(www\\.)?[-a-zA-Z0-9@:%._+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_+.~#?&/=]*)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group() : null;
    }
    catch(e) {
        return null;
    }
}
public String findkwid(String url) {
    try {
        Pattern pattern = Pattern.compile("m.kuwo.cn/(h5app/single/|newh5app/play_detail/|yinyue/)([^&]+)");
        //后面文字并不影响
        Matcher matcher = pattern.matcher(url);
        if (matcher.find()) {
            String songmid = matcher.group(2);
            return songmid;
        }
        else {
            return null;
        }
    }
    catch(e) {
        return null;
    }
}
public String findbdid(String url) {
    try {
        Pattern pattern = Pattern.compile("musicId=([^&]+)");
        //后面文字并不影响
        Matcher matcher = pattern.matcher(url);
        if (matcher.find()) {
            String songmid = matcher.group(1);
            return songmid;
        }
        else {
            return null;
        }
    }
    catch(e) {
        return null;
    }
}
public String findhash(String url) {
    try {
        Pattern pattern = Pattern.compile("hash=([^&]+)");
        //后面文字并不影响
        Matcher matcher = pattern.matcher(url);
        if (matcher.find()) {
            String songmid = matcher.group(1);
            return songmid;
        }
        else {
            url=get(url);
            Pattern pattern2 = Pattern.compile("\"hash\":\"([a-f0-9]{32})\",");
            Matcher matcher2 = pattern2.matcher(url);
            if (matcher2.find()) {
                String songmid = matcher2.group(1);
                return songmid;
            }
            else {
                return null;
            }
        }
    }
    catch(e) {
        return null;
    }
}
public String findqsid(String url) {
    try {
        Pattern pattern = Pattern.compile("track_id=([^&]+)");
        //后面文字并不影响
        Matcher matcher = pattern.matcher(url);
        if (matcher.find()) {
            String songmid = matcher.group(1);
            return songmid;
        }
        else {
            return null;
        }
    }
    catch(e) {
        return null;
    }
}