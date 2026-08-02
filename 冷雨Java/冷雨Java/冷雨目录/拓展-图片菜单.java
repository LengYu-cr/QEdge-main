public void 图片菜单(Object Yu) {
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
                if(quntext.equals("开启图片菜单")) {
                    if(qq.equals(uin)||读(qun,"代管",uin)==1||读("0","代管",uin)==1) {
                        写(qun,"图片菜单","开关",1);
                        String menu="已开启本聊天图片菜单";
                        sendText(data,menu);
                    }
                }
                if(quntext.equals("关闭图片菜单")) {
                    if(qq.equals(uin)||读(qun,"代管",uin)==1||读("0","代管",uin)==1) {
                        写(qun,"图片菜单","开关",0);
                        String menu="已关闭本聊天图片菜单";
                        sendText(data,menu);
                    }
                }
                if(quntext.equals("图片菜单")) {
                    if(读(qun,"图片菜单","开关")==1) {
                        String menu = "图片菜单:\n随机腹肌 幼态动漫\n高清壁纸 七濑胡桃\n随机龙图 坤坤表情\n原神系列 动漫综合\n少女写真 风景系列\n物语系列 猫娘系列\n动漫人物 二次元图\n舔狗日记 电脑壁纸\n帅哥图片 随机柴郡\n每日早报 随机咖波\n抹茶旦旦 一二布布\n可爱表情 永雏塔菲\n美女图集 美女壁纸\n随机腿图 随机妹子\n可爱龙图 \n每日60s 妹子JK doro\n卖家秀 kemomimi miku\nQQ壁纸 bing 米哈游\n随机JK 小豆泥 COS\n小狐狸 猫羽雫 fufu\n度娘搜图+内容\n搜索表情+内容";
                        sendText(data,menu);
                    }
                    else {
                        sendText(data,"本聊天未开启图片菜单");
                    }
                }
                if(读(qun,"图片菜单","开关")==1) {
                    if(quntext.equals("可爱龙图")) {
                        String tt=get("https://api.yuafeng.cn/API/ly/long.php?type=text");
                        sendImg(qun,tt,mtype);
                    }
                    if (quntext.equals("COS")) {
                        String tt=get("https://api.yuafeng.cn/API/ly/mys.php");
                        if (tt.equals("访问网页失败")) {
                            sendText( data, "请求服务器出错：服务器未响应");
                            return;
                        }
                        try {
                            JSONObject json = new JSONObject(tt);
                            if (json.getInt("code") == 0) {
                                String channel = json.getString("channel");
                                String last_id = json.getString("last_id");
                                //下一页
                                json = json.getJSONObject("data");
                                String type = json.getString("type");
                                if (type.equals("video")) {
                                    String subject = json.getString("subject");
                                    String content = json.getString("content");
                                    String cover = json.getString("cover");
                                    JSONArray array = json.getJSONArray("video").getJSONObject(0).getJSONArray("resolutions");
                                    String video = array.getJSONObject(array.length()-1).getString("url");
                                    //2K视频(最高品)
                                    String tag = array.getJSONObject(array.length()-1).getString("definition");
                                    String jump = json.getString("url");
                                    long created_time = json.getLong("created_time");
                                    String time = timestampToDate(created_time*1000);
                                    String coser = json.getJSONObject("user").getString("name");
                                    String gender = json.getJSONObject("user").getString("gender");
                                    sendText( data, "[pic=" + cover + "]" + coser + "(" + gender + ")\n" + subject + "\n" + content + "\n来源：" + jump + "\n来自于" + channel + "\n" + tag + "视频正在发送中......");
                                    sendVideo( mtype, qun, video);
                                }
                                else if (type.equals("image")) {
                                    String subject = json.getString("subject");
                                    String content = json.getString("content");
                                    String cover = json.getString("cover");
                                    String result = "图集来咯！";
                                    JSONArray array = json.getJSONArray("image");
                                    for (int x = 0;
                                    x < array.length();
                                    x ++) {
                                        String a = array.getString(x);
                                        result += "[pic=" + a + "]";
                                    }
                                    String jump = json.getString("url");
                                    long created_time = json.getLong("created_time");
                                    String time = timestampToDate(created_time*1000);
                                    String coser = json.getJSONObject("user").getString("name");
                                    String gender = json.getJSONObject("user").getString("gender");
                                    sendText( data, "[pic=" + cover + "]" + coser + "(" + gender + ")\n" + subject + "\n" + content + "\n来源：" + jump + "\n来自于" + channel + "\n图集正在发送中......");
                                    sendMsg( qun, result, mtype);
                                }
                                else {
                                    sendText( data, "返回类型出错：返回类型不是image或video");
                                }
                            }
                            else {
                                sendText( data, "请求服务器出错：" + json.getString("msg"));
                            }
                        }
                        catch(e) {
                            sendText( data, "JSON解析出错：" + e.getMessage() );
                        }
                    }
                    if(quntext.equals("每日早报")) {
                        sendImg(qun,myApi+"API/60sn/",mtype);
                    }
                    if(quntext.equals("每日60s")) {
                        sendImg(qun,myApi+"API/60s/",mtype);
                    }
                    if(quntext.equals("随机腹肌")) {
                        sendImg(qun,"https://free.wqwlkj.cn/wqwlapi/sgfj.php?type=image",mtype);
                    }
                    if(quntext.equals("幼态动漫")) {
                        sendImg(qun,"https://api.s01s.cn/API/ytdm/",mtype);
                    }
                    if(quntext.equals("猫羽雫")) {
                        //sendImg(qun,"http://shanhe.kim/api/tu/mao.php",mtype);
                        sendImg(qun,myApi+"API/ly/tu.php?type=myn",mtype);
                    }
                    if(quntext.equals("随机JK")) {
                        sendImg(qun,"http://shanhe.kim/api/tu/jk.php",mtype);
                    }
                    if(quntext.equals("bing")) {
                        sendImg(qun,"https://imgapi.cn/bing.php?rand=true",mtype);
                    }
                    if(quntext.equals("高清壁纸")) {
                        sendImg(qun,"https://api.suyanw.cn/api/scenery?return=",mtype);
                    }
                    if(quntext.startsWith("度娘搜图")) {
                        String msg=quntext.substring(4);
                        String result="";
                        String url="https://m.baidu.com/sf/vsearch?pd=image_content&word="+msg+"&tn=vsearch&atn=page&sa=vs_img_indexhot&fr=index";
                        String url=get(url);
                        int index = url.lastIndexOf("type=\"application/json\">{\"data\":");
                        String text = url.substring(index + 24);
                        int rd = text.indexOf("}}</script>");
                        String re = text.substring(0,rd+2);
                        JSONObject json=new JSONObject(re);
                        String data1=json.getString("data");
                        if(!data1.contains("\"rsdata\":[]")) {
                            JSONObject json1=new JSONObject(data1);
                            String rsInfo=json1.getString("rsInfo");
                            JSONObject json2=new JSONObject(rsInfo);
                            JSONArray rsdata=json2.getJSONArray("rsdata");
                            for(int q=0;
                            q<rsdata.length();
                            q++) {
                                JSONObject List=rsdata.get(q);
                                String relateSearchQuery=List.get("relateSearchQuery");
                                String rs_thumburl=List.get("rs_thumburl");
                                if(!rs_thumburl.equals("")) {
                                    result+=relateSearchQuery+"\n[pic="+rs_thumburl+"]";
                                }
                            }
                            sendMsg(qun,"图片来咯～\n"+result,mtype);
                        }
                        else if(data1.contains("\"images\":[{\"")) {
                            JSONObject json1=new JSONObject(data1);
                            JSONArray rsdata=json1.getJSONArray("images");
                            for(int q=0;
                            q<rsdata.length();
                            q++) {
                                JSONObject List=rsdata.get(q);
                                String relateSearchQuery=List.get("fromPageSummaryOrig");
                                String rs_thumburl=List.get("thumburl");
                                String fromUrlHost=List.get("fromUrlHost");
                                if(!rs_thumburl.equals("")) {
                                    result+=relateSearchQuery+"\n[pic="+rs_thumburl+"]来源:"+fromUrlHost+"\n";
                                }
                            }
                            sendMsg(qun,"图片来咯～\n"+result,mtype);
                        }
                    }
                    if(quntext.startsWith("搜索表情")) {
                        try {
                            String msg=quntext.substring(4);
                            String result="";
                            String url="https://h5api.sginput.qq.com/wxbq/search?key="+msg+"&page=1&num=20";
                            String url=get(url);
                            JSONObject json=new JSONObject(url);
                            String returnmsg=json.get("msg");
                            if(returnmsg.equals("succ")) {
                                JSONArray datamsg=json.getJSONArray("data");
                                //int a=随机数(0,datamsg.length());
                                for(int q=0;
                                q<datamsg.length();
                                q++) {
                                    JSONObject List=datamsg.get(q);
                                    String indexUrl=List.get("indexUrl");
                                    result+="图片来咯～[pic="+indexUrl+"]";
                                }
                                sendImg(qun,result,mtype);
                            }
                            else {
                                群聊发送(Yu,"表情包搜索失败，请稍后重试！");
                            }
                        }
                        catch(e) {
                            群聊发送(Yu,"表情包搜索失败，请稍后重试");
                        }
                    }
                    if(quntext.equals("动漫人物")) {
                        sendImg(qun,"http://shanhe.kim/api/tu/anime.php",mtype);
                    }
                    if(quntext.equals("舔狗日记")) {
                        sendImg(qun,"https://free.wqwlkj.cn/wqwlapi/tgbj.php",mtype);
                    }
                    if(quntext.equals("二次元图")) {
                        sendImg(qun,"https://imgapi.xl0408.top/index.php",mtype);
                    }
                    if(quntext.equals("少女写真")) {
                        int a=0;
                        int b=6;
                        int c=随机数(a,b);
                        sendImg(qun,"https://api.r10086.com/%E6%A8%B1%E9%81%93%E9%9A%8F%E6%9C%BA%E5%9B%BE%E7%89%87api%E6%8E%A5%E5%8F%A3.php?%E5%9B%BE%E7%89%87%E7%B3%BB%E5%88%97=%E5%B0%91%E5%A5%B3%E5%86%99%E7%9C%9F"+c,mtype);
                    }
                    if(quntext.equals("物语系列")) {
                        int a=0;
                        int b=2;
                        int c=随机数(a,b);
                        sendImg(qun,"https://api.r10086.com/%E6%A8%B1%E9%81%93%E9%9A%8F%E6%9C%BA%E5%9B%BE%E7%89%87api%E6%8E%A5%E5%8F%A3.php?%E5%9B%BE%E7%89%87%E7%B3%BB%E5%88%97=%E7%89%A9%E8%AF%AD%E7%B3%BB%E5%88%97"+c,mtype);
                    }
                    if(quntext.equals("猫娘系列")) {
                        sendImg(qun,"https://api.r10086.com/%E6%A8%B1%E9%81%93%E9%9A%8F%E6%9C%BA%E5%9B%BE%E7%89%87api%E6%8E%A5%E5%8F%A3.php?%E5%9B%BE%E7%89%87%E7%B3%BB%E5%88%97=%E7%8C%AB%E5%A8%981",mtype);
                    }
                    if(quntext.equals("风景系列")) {
                        int a=0;
                        int b=10;
                        int c=随机数(a,b);
                        sendImg(qun,"https://api.r10086.com/%E6%A8%B1%E9%81%93%E9%9A%8F%E6%9C%BA%E5%9B%BE%E7%89%87api%E6%8E%A5%E5%8F%A3.php?%E5%9B%BE%E7%89%87%E7%B3%BB%E5%88%97=%E9%A3%8E%E6%99%AF%E7%B3%BB%E5%88%97"+c,mtype);
                    }
                    if(quntext.equals("动漫综合")) {
                        int a=0;
                        int b=18;
                        int c=随机数(a,b);
                        sendImg(qun,"https://api.r10086.com/%E6%A8%B1%E9%81%93%E9%9A%8F%E6%9C%BA%E5%9B%BE%E7%89%87api%E6%8E%A5%E5%8F%A3.php?%E5%9B%BE%E7%89%87%E7%B3%BB%E5%88%97=%E5%8A%A8%E6%BC%AB%E7%BB%BC%E5%90%88"+c,mtype);
                    }
                    if(quntext.equals("原神系列")) {
                        sendImg(qun,"https://api.r10086.com/%E6%A8%B1%E9%81%93%E9%9A%8F%E6%9C%BA%E5%9B%BE%E7%89%87api%E6%8E%A5%E5%8F%A3.php?%E8%87%AA%E9%80%82%E5%BA%94%E5%9B%BE%E7%89%87%E7%B3%BB%E5%88%97=%E5%8E%9F%E7%A5%9E",mtype);
                    }
                    if(quntext.equals("帅哥图片")) {
                        String tt=fetchRedirectUrl("https://api.lolimi.cn/API/boy/");
                        sendImg(qun,tt,mtype);
                    }
                    if(quntext.equals("随机柴郡")) {
                        //String tt=fetchRedirectUrl("https://api.lolimi.cn/API/chaiq/c.php");
                        String tt=myApi+"API/ly/tu.php?type=mm";
                        sendImg(qun,tt,mtype);
                    }
                    if(quntext.equals("坤坤表情")) {
                        String tt=get("http://api.tangdouz.com/zzz/kk.php");
                        sendImg(qun,tt,mtype);
                    }
                    if(quntext.equals("随机龙图")) {
                        //String tt=fetchRedirectUrl("https://api.lolimi.cn/API/longt/l.php");
                        String tt=myApi+"API/ly/tu.php?type=long";
                        sendImg(qun,tt,mtype);
                    }
                    if(quntext.equals("小狐狸")) {
                        String tt=fetchRedirectUrl("https://t.alcy.cc/xhl");
                        sendImg(qun,tt,mtype);
                    }
                    if(quntext.equals("七濑胡桃")) {
                        String tt=fetchRedirectUrl("https://t.alcy.cc/lai");
                        sendImg(qun,tt,mtype);
                    }
                    if(quntext.equals("小豆泥")) {
                        String tt=fetchRedirectUrl("http://api.treason.cn/API/v1/xdn/api.php");
                        sendImg(qun,tt,mtype);
                    }
                    if(quntext.equals("QQ壁纸")) {
                        String tt=fetchRedirectUrl(myWeb+"bizhi.php");
                        sendImg(qun,tt,mtype);
                    }
                    if(quntext.equals("电脑壁纸")) {
                        String tt=fetchRedirectUrl(myApi+"API/dnbz/api.php");
                        sendImg(qun,tt,mtype);
                    }
                    if(quntext.equals("随机咖波")) {
                        String tt=myApi+"API/ly/tu.php?type=mmc";
                        sendImg(qun,tt,mtype);
                    }
                    if(quntext.equals("抹茶旦旦")) {
                        String tt=myApi+"API/ly/tu.php?type=mcdd";
                        sendImg(qun,tt,mtype);
                    }
                    if(quntext.equals("doro")) {
                        String tt=myApi+"API/ly/tu.php?type=doro";
                        sendImg(qun,tt,mtype);
                    }
                    if(quntext.equals("miku")) {
                        String tt=myApi+"API/ly/tu.php?type=miku";
                        sendImg(qun,tt,mtype);
                    }
                    if(quntext.equals("kemomimi")) {
                        String tt=myApi+"API/ly/tu.php?type=kemomimi";
                        sendImg(qun,tt,mtype);
                    }
                    if(quntext.equals("fufu")) {
                        String tt=myApi+"API/ly/tu.php?type=fufu";
                        sendImg(qun,tt,mtype);
                    }
                    if(quntext.equals("一二布布")) {
                        String tt=myApi+"API/ly/tu.php?type=yebb";
                        sendImg(qun,tt,mtype);
                    }
                    if(quntext.equals("永雏塔菲")) {
                        String tt=myApi+"API/ly/tu.php?type=yctf";
                        sendImg(qun,tt,mtype);
                    }
                    if(quntext.equals("可爱表情")) {
                        String tt=myApi+"API/ly/tu.php?type=ka";
                        sendImg(qun,tt,mtype);
                    }
                    if(quntext.equals("米哈游")) {
                        String tt=myApi+"API/ly/tu.php?type=mhy";
                        sendImg(qun,tt,mtype);
                    }
                    if(quntext.equals("随机妹子")) {
                        String tt=get(myApi+"API/ly/tui.php?type=text");
                        sendImg(qun,tt,mtype);
                    }
                    if(quntext.equals("妹子JK")) {
                        String tt=get(myApi+"API/ly/jk.php?type=text");
                        sendImg(qun,tt,mtype);
                    }
                    if(quntext.equals("随机腿图")) {
                        String tt=get(myApi+"API/ly/meitui.php?type=text");
                        sendImg(qun,tt,mtype);
                    }
                    if(quntext.equals("卖家秀")) {
                        String tt=get(myApi+"API/ly/mjx.php?type=text");
                        sendImg(qun,tt,mtype);
                    }
                    if(quntext.equals("美女壁纸")) {
                        String tt=get("http://ly.missqiu.icu/pa/xjj2.php?type=text");
                        sendImg(qun,tt,mtype);
                    }
                    if(quntext.equals("美女图集")) {
                        String tt=get("http://ly.missqiu.icu/pa/xjj.php");
                        JSONObject json = new JSONObject(tt);
                        int code=json.getInt("code");
                        if(code==0) {
                            if(tt.contains("images")) {
                                JSONObject json1=json.getJSONObject("data");
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
                            else {
                                sendText(data,"返回内容并非图集");
                            }
                        }
                        else {
                            sendText(data,"接口出错了");
                        }
                    }
                    //输入type参数:long(龙图),mcdd(抹茶旦旦,小鳄鱼),mm(柴郡猫),myn(猫羽雫),yebb(一二布布),doro(doro),mmc(猫猫虫,咖波),miku(miku),kemomimi(kemomimi),fufu(fufu初音未来)
                }
            }
        }
    }
    ).start();
}