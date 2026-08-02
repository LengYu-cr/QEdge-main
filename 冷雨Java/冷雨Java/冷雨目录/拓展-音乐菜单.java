HashMap search_music_list = new HashMap();
public void 音乐菜单(Object Yu) {
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
                if(quntext.equals("开启音乐菜单")) {
                    if(qq.equals(uin)||读(qun,"代管",uin)==1||读("0","代管",uin)==1) {
                        写(qun,"音乐菜单","开关",1);
                        String menu="已开启本聊天音乐菜单";
                        sendText(data,menu);
                    }
                }
                if(quntext.equals("关闭音乐菜单")) {
                    if(qq.equals(uin)||读(qun,"代管",uin)==1||读("0","代管",uin)==1) {
                        写(qun,"音乐菜单","开关",0);
                        String menu="已关闭本聊天音乐菜单";
                        sendText(data,menu);
                    }
                }
                if(quntext.equals("音乐菜单")) {
                    if(读(qun,"音乐菜单","开关")==1) {
                        String menu = "音乐菜单:\n随机音乐 随机网易\n随机唱鸭 逆天语音\n坤坤音乐 取消点歌\n点歌+歌曲\nQQ点歌+歌曲(vip)\n网易点歌+歌曲(vip)\n酷狗点歌+歌曲(vip)\n酷我点歌+歌曲(vip)\n汽水点歌+歌曲(vip)\n切换卡片/语音/链接/文件/下载/空间/播放点歌";
                        sendText(data,menu);
                    }
                    else {
                        sendText(data,"本聊天未开启音乐菜单");
                    }
                }
                if(读(qun,"音乐菜单","开关")==1) {
                    if(quntext.equals("逆天语音")) {
                        String name=fetchRedirectUrl(myWeb+"sjyy.php");
                        sendPtt(qun,name,mtype);
                    }
                    if(quntext.equals("坤坤音乐")) {
                        String name=myWeb+"kun.php";
                        sendPtt(qun,name,mtype);
                    }
                    if(quntext.equals("随机音乐")) {
                        String yyms=读(ColdRainPath+"data/"+qun+"点歌模式.txt");
                        if(yyms.equals("")) yyms="卡片";
                        String url = get(myWeb+"randomMusic.php?mode=Share&id=6283364726&type=中品质");
                        if(url.equals("访问网页失败")) {
                            sendText(data,url);
                        }
                        else {
                            JSONObject jsons = new JSONObject(url);
                            if(jsons.getInt("code")==0) {
                                jsons = jsons.getJSONObject("data");
                                String music = jsons.getString("music");
                                String album_name = jsons.getString("album_name");
                                String song = jsons.getString("song");
                                String singer = jsons.getString("singer");
                                String cover = jsons.getString("cover");
                                String link="https://i.y.qq.com/v8/playsong.html?songmid="+jsons.get("mid");
                                sendMusic(qun,song,singer,link,music,cover,"QQ",yyms,mtype);
                            }
                            else {
                                sendText(data,jsons.optString("msg"));
                            }
                        }
                    }
                    if("随机网易".equals(quntext)) {
                        try {
                            String yyms=读(ColdRainPath+"data/"+qun+"点歌模式.txt");
                            if(yyms.equals("")) yyms="卡片";
                            String url=get(myWeb+"randomNetease.php?select=随机");
                            JSONObject json=new JSONObject(url);
                            String data1=json.getString("data");
                            JSONObject json1=new JSONObject(data1);
                            String name=json1.getString("song");
                            String music=json1.getString("music");
                            String picurl=json1.getString("cover");
                            String artistsname=json1.getString("singer");
                            sendMusic(qun,name,artistsname,"https://y.music.163.com/m/song?id="+json1.getString("id"),music,picurl,"网易",yyms,mtype);
                        }
                        catch(e) {
                            sendText(data,"获取失败"+e);
                        }
                    }
                    if(quntext.equals("随机唱鸭")) {
                        String result="";
                        String f=RandomLines(ColdRainPath+"下载/唱鸭.txt");
                        String yyms=读(ColdRainPath+"data/"+qun+"点歌模式.txt");
                        if(yyms.equals("")) yyms="卡片";
                        String http="https://m.api.singduck.cn/user-piece/"+f+"?userId=2001391065";
                        String url=get(http);
                        int index = url.lastIndexOf("\"__NEXT_DATA__\" type=\"application/json\" crossorigin=\"anonymous\">");
                        String text = url.substring(index + 64);
                        int rd = text.indexOf("}<");
                        String re = text.substring(0,rd+1);
                        JSONObject json=new JSONObject(re);
                        String props=json.getString("props");
                        JSONObject json1=new JSONObject(props);
                        String pageProps=json1.getString("pageProps");
                        JSONObject json2=new JSONObject(pageProps);
                        String clip=json2.getString("clip");
                        JSONObject List=new JSONObject(clip);
                        String artist=List.optString("singerName").replace("[","").replace("]","").replace("\"","").replace(","," and ");
                        String songName=List.optString("songName");
                        String audioUrl=List.optString("audioSrc");
                        String lyrics=List.optString("lyrics").replace("[","").replace("]","").replace("\"","").replace(",","\n");
                        sendText(data,"歌名:"+songName+"\n歌手:"+artist+"\n"+lyrics);
                        sendMusic(qun,songName+"",""+artist,""+http,audioUrl,"http://q2.qlogo.cn/headimg_dl?dst_uin="+uin+"&spec=640","哈哈哈！你妈死啦",yyms,mtype);
                    }
                    if(quntext.startsWith("点歌")) {
                        String yyms=读(ColdRainPath+"data/"+qun+"点歌模式.txt");
                        if(yyms.equals("")) yyms="卡片";
                        String gm=quntext.substring(2);
                        if(gm.equals("")) {
                            sendText(data,"[atUin="+uin+"]\n不是，你怎么连歌名都不输入！");
                        }
                        else {
                            String cookie="uin=o0"+qq+"; skey="+skey+"; p_uin=o0"+qq+"; p_skey="+getPskey("y.qq.com");
                            String url=httppost1("https://u.y.qq.com/cgi-bin/musicu.fcg?_webcgikey=DoSearchForQQMusicDesktop",cookie,"{\"comm\":{\"format\":\"json\",\"inCharset\":\"utf-8\",\"outCharset\":\"utf-8\",\"notice\":0,\"platform\":\"h5\",\"needNewCode\":1,\"ct\":23,\"cv\":0},\"req_0\":{\"method\":\"DoSearchForQQMusicDesktop\",\"module\":\"music.search.SearchCgiService\",\"param\":{\"remoteplace\":\"txt.mqq.all\",\"search_type\":0,\"query\":\""+gm+"\",\"page_num\":1,\"num_per_page\":20}}}");
                            JSONObject json = new JSONObject(url);
                            String code=json.optString("code");
                            if(code.equals("0")) {
                                String result="";
                                String req_0=json.getString("req_0");
                                JSONObject json1 = new JSONObject(req_0);
                                String data1=json1.getString("data");
                                JSONObject json2 = new JSONObject(data1);
                                String body=json2.getString("body");
                                JSONObject json3 = new JSONObject(body);
                                String song=json3.getString("song");
                                JSONObject json4 = new JSONObject(song);
                                String list=json4.getString("list");
                                if(list.length()<5) {
                                    sendText(data,"[atUin="+uin+"]\n歌曲未搜索到");
                                }
                                else {
                                    JSONArray infos=json4.getJSONArray("list");
                                    for(int q=0;
                                    q<1;
                                    q++) {
                                        JSONObject List=infos.get(q);
                                        String mid=List.get("mid");
                                        String album=List.getString("album");
                                        JSONObject json9=new JSONObject(album);
                                        String albumId=json9.get("pmid");
                                        //专辑
                                        String coverUrl="//y.gtimg.cn/music/photo_new/T002R500x500M000"+albumId+".jpg";
                                        //获取图片链接
                                        String albumId_name=json9.get("name");
                                        //专辑名
                                        String file=List.getString("file");
                                        JSONObject json10=new JSONObject(file);
                                        String media_mid=json10.get("media_mid");
                                        String mv=List.getString("mv");
                                        JSONObject json11=new JSONObject(mv);
                                        String vid=json11.optString("vid");
                                        String name=List.get("name");
                                        JSONArray singer=List.getJSONArray("singer");
                                        for(int i=0;
                                        i<singer.length();
                                        i++) {
                                            JSONObject List2=singer.get(i);
                                            String name1=List2.get("name");
                                            result+=name1+" ";
                                        }
                                        String re3=getMusicUrl(mid,media_mid);
                                        if(re3!=null&&re3.startsWith("http")) {
                                            sendMusic(qun,""+name,""+result+"·"+albumId_name,"https://i.y.qq.com/v8/playsong.html?songmid="+mid+"&senderName=ColdRain",re3,"http:"+coverUrl,"QQ",""+yyms,mtype);
                                        }
                                        else {
                                            if(vid.equals("")) {
                                                sendText(data,"失败，可能是歌曲收费\n『"+name+"——"+result+"』直链：\nhttps://i.y.qq.com/v8/playsong.html?songmid="+mid+"&senderName=ColdRain\n正在获取1分钟试听链接......");
                                                String url2=httppost1("https://u.y.qq.com/cgi-bin/musicu.fcg",""+cookie,"{\"comm\":{\"uin\":\""+qq+"\",\"authst\":\"\",\"mina\":1,\"appid\":1109523715,\"ct\":29},\"urlReq0\":{\"module\":\"vkey.GetVkeyServer\",\"method\":\"CgiGetVkey\",\"param\":{\"guid\":\"7982463958\",\"songmid\":[\""+mid+"\"],\"songtype\":[0],\"filename\":[\"RS02"+media_mid+".mp3\"],\"uin\":\""+qq+"\",\"loginflag\":1,\"platform\":\"23\",\"h5to\":\"speed\"}}}");
                                                if(url2.contains("\"purl\":\"")) {
                                                    int index3 = url2.lastIndexOf("\"purl\":\"");
                                                    String text3 = url2.substring(index3 + 8);
                                                    int rd3 = text3.indexOf("\",\"errtype\":\"");
                                                    String re3 = text3.substring(0,rd3);
                                                    String music="http://sjy.stream.qqmusic.qq.com/"+u解(re3);
                                                    sendMusic(qun,""+name,""+result+"·"+albumId_name,"https://i.y.qq.com/v8/playsong.html?songmid="+mid+"&senderName=ColdRain",music,"http:"+coverUrl,"QQ",""+yyms,mtype);
                                                }
                                                else {
                                                    sendText(data,"1分钟试听链接获取失败！");
                                                }
                                            }
                                            else {
                                                sendText(data,"失败，可能是歌曲收费\n『"+name+"——"+result+"』直链：\nhttps://i.y.qq.com/v8/playsong.html?songmid="+mid+"&senderName=ColdRain\n正在获取MV链接......");
                                                String url3=httpget("https://u6.y.qq.com/cgi-bin/musicu.fcg?g_tk="+GetGTK(skey)+"&uin="+qq+"&ct=23&cv=0&format=json&callback=&data=%7B%22comm%22%3A%7B%22ct%22%3A23%2C%22cv%22%3A0%2C%22format%22%3A%22json%22%2C%22platform%22%3A%22h5%22%2C%22uin%22%3A%22"+qq+"%22%2C%22g_tk%22%3A"+GetGTK(skey)+"%7D%2C%22getMVInfo%22%3A%7B%22module%22%3A%22video.VideoDataServer%22%2C%22method%22%3A%22get_video_info_batch%22%2C%22param%22%3A%7B%22vidlist%22%3A%5B%22"+vid+"%22%5D%2C%22required%22%3A%5B%22vid%22%2C%22sid%22%2C%22gmid%22%2C%22type%22%2C%22name%22%2C%22cover_pic%22%2C%22video_switch%22%2C%22msg%22%2C%22new_switch_str%22%2C%22video_pay%22%2C%22aspect_state%22%5D%2C%22from%22%3A%22h5.https%3A%2F%2Fi.y.qq.com%2Fv8%2Fplaysong.html%22%2C%22qimei36%22%3A%22%22%7D%7D%2C%22getMVUrl%22%3A%7B%22module%22%3A%22music.stream.MvUrlProxy%22%2C%22method%22%3A%22GetMvUrls%22%2C%22param%22%3A%7B%22vids%22%3A%5B%22"+vid+"%22%5D%2C%22from%22%3A%22h5.https%3A%2F%2Fi.y.qq.com%2Fv8%2Fplaysong.html%22%2C%22request_type%22%3A10003%2C%22format%22%3A264%2C%22addrtype%22%3A3%7D%7D%7D&_="+System.currentTimeMillis()+"&platform=h5",cookie);
                                                JSONObject json = new JSONObject(url3);
                                                String code=json.optString("code");
                                                if(code.equals("0")) {
                                                    String getMVUrl=json.getString("getMVUrl");
                                                    JSONObject json12 = new JSONObject(getMVUrl);
                                                    String data2=json12.getString("data");
                                                    JSONObject json13 = new JSONObject(data2);
                                                    String vidd=json13.getString(vid);
                                                    JSONObject json14 = new JSONObject(vidd);
                                                    String mp4=json14.getString("mp4");
                                                    int index = mp4.lastIndexOf("\"],\"freeflow_url\":[\"");
                                                    String text = mp4.substring(index+20);
                                                    int rd = text.indexOf("mp4\"],\"comm_url\":");
                                                    String re = text.substring(0,rd+3).replace("\\/","/");
                                                    sendMusic(qun,""+name,""+result+"·"+albumId_name,"https://i.y.qq.com/v8/playsong.html?songmid="+mid+"&senderName=ColdRain",re,"http:"+coverUrl,"QQ",""+yyms,mtype);
                                                }
                                                else {
                                                    sendText(data,"获取MV链接失败");
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if(quntext.startsWith("汽水点歌")) {
                    
                    if(search_music_list.containsKey(qun + "_" + uin)){
                        search_music_list.remove(qun + "_" + uin);
                    }
                    MusicMap music_map = new MusicMap();
                    music_map.put("qun", qun);
                    music_map.put("uin", uin);
                    music_map.put("type", "汽水");
                        String text=quntext.substring(4);
                        String url=get(myWeb+"qsmusic.php?msg="+URL(text,1)+"&num=15");
                        if(url.equals("访问网页失败")) {
                            sendText(data,url);
                        }
                        else {
                            String result="";
                            JSONObject json = new JSONObject(url);
                            int code=json.getInt("code");
                            if(code==0) {
                                JSONArray word_list=json.getJSONArray("data");
                                for(int i=0;
                                i<word_list.length();
                                i++) {
                                    JSONObject item=word_list.get(i);
                                    String songname=item.get("song");
                                    String name=item.get("singer");
                                    result+=(i+1)+"、"+songname+"——"+name+"\n";
                                    music_map.put("json", word_list);
                                }
                                search_music_list.put(qun + "_" + uin, music_map);
                                sendText(data,result+"请发送序号选择\n不想点歌请发\"取消点歌\"");
                            }
                            else {
                                sendText(data,json.get("msg"));
                            }
                        }
                    }
                    if(quntext.startsWith("QQ点歌")) {
                    if(search_music_list.containsKey(qun + "_" + uin)){
                        search_music_list.remove(qun + "_" + uin);
                    }
                    
                    MusicMap music_map = new MusicMap();
                    music_map.put("qun", qun);
                    music_map.put("uin", uin);
                    music_map.put("type", "QQ");
                    
                        String text=quntext.substring(4);
                        String url=get(myWeb+"qqqmusic.php?msg="+URL(text,1)+"&adaptive=1&num=15");
                        if(url.equals("访问网页失败")) {
                            sendText(data,url);
                        }
                        else {
                            String result="";
                            JSONObject json = new JSONObject(url);
                            int code=json.getInt("code");
                            if(code==0) {
                                JSONArray word_list=json.getJSONArray("data");
                                for(int i=0;
                                i<word_list.length();
                                i++) {
                                    JSONObject item=word_list.get(i);
                                    String songname=item.get("title");
                                    String name=item.get("singer");
                                    result+=(i+1)+"、"+songname+"——"+name+"\n";
                                    music_map.put("json", word_list);
                                }
                                search_music_list.put(qun + "_" + uin, music_map);
                                sendText(data,result+"请发送序号选择\n不想点歌请发\"取消点歌\"");
                            }
                            else {
                                sendText(data,json.get("msg"));
                            }
                        }
                    }
                    if(quntext.startsWith("网易点歌")) {
                    if(search_music_list.containsKey(qun + "_" + uin)){
                        search_music_list.remove(qun + "_" + uin);
                    }
                    MusicMap music_map = new MusicMap();
                    music_map.put("qun", qun);
                    music_map.put("uin", uin);
                    music_map.put("type", "网易");
                        String text=quntext.substring(4);
                        String url=get(myWeb+"wymusic.php?msg="+URL(text,1)+"&adaptive=1&num=15");
                        if(url.equals("访问网页失败")) {
                            sendText(data,url);
                        }
                        else {
                            String result="";
                            JSONObject json = new JSONObject(url);
                            int code=json.getInt("code");
                            if(code==0) {
                                JSONArray word_list=json.getJSONArray("data");
                                for(int i=0;
                                i<word_list.length();
                                i++) {
                                    JSONObject item=word_list.get(i);
                                    String songname=item.get("song");
                                    String name=item.get("singer");
                                    result+=(i+1)+"、"+songname+"——"+name+"\n";
                                    music_map.put("json", word_list);
                                }
                                search_music_list.put(qun + "_" + uin, music_map);
                                sendText(data,result+"请发送序号选择\n不想点歌请发\"取消点歌\"");
                            }
                            else {
                                sendText(data,json.get("msg"));
                            }
                        }
                    }
                    if(quntext.startsWith("酷狗点歌")) {
                    if(search_music_list.containsKey(qun + "_" + uin)){
                        search_music_list.remove(qun + "_" + uin);
                    }
                    MusicMap music_map = new MusicMap();
                    music_map.put("qun", qun);
                    music_map.put("uin", uin);
                    music_map.put("type", "酷狗");
                        String text=quntext.substring(4);
                        String url=get(myWeb+"kgmusic.php?msg="+URL(text,1)+"&adaptive=1&num=15");
                        if(url.equals("访问网页失败")) {
                            sendText(data,url);
                        }
                        else {
                            String result="";
                            JSONObject json = new JSONObject(url);
                            int code=json.getInt("code");
                            if(code==0) {
                                JSONArray word_list=json.getJSONArray("data");
                                for(int i=0;
                                i<word_list.length();
                                i++) {
                                    JSONObject item=word_list.get(i);
                                    String songname=item.get("song");
                                    String name=item.get("singer");
                                    result+=(i+1)+"、"+songname+"——"+name+"\n";
                                    music_map.put("json", word_list);
                                }
                                search_music_list.put(qun + "_" + uin, music_map);
                                sendText(data,result+"请发送序号选择\n不想点歌请发\"取消点歌\"");
                            }
                            else {
                                sendText(data,json.get("msg"));
                            }
                        }
                    }
                    if(quntext.startsWith("酷我点歌")) {
                    if(search_music_list.containsKey(qun + "_" + uin)){
                        search_music_list.remove(qun + "_" + uin);
                    }
                    MusicMap music_map = new MusicMap();
                    music_map.put("qun", qun);
                    music_map.put("uin", uin);
                    music_map.put("type", "酷我");
                        String text=quntext.substring(4);
                        String url=get("https://search.kuwo.cn/r.s?client=kt&all="+URL(text,1)+"&pn=0&rn=20&uid=794762570&ver=kwplayer_ar_9.2.2.1&vipver=1&show_copyright_off=1&newver=1&ft=music&cluster=0&strategy=2012&encoding=utf8&rformat=json&vermerge=1&mobi=1&issubtitle=1&_=1657713784395");
                        if(url.equals("访问网页失败")) {
                            sendText(data,url);
                        }
                        else {
                            String result="";
                            JSONObject json = new JSONObject(url);
                            String code=json.optString("HIT_BUT_OFFLINE");
                            if(code.equals("0")) {
                                JSONArray word_list=json.getJSONArray("abslist");
                                for(int i=0;
                                i<word_list.length();
                                i++) {
                                    JSONObject item=word_list.get(i);
                                    String songname=item.get("NAME");
                                    String name=item.get("ARTIST");
                                    result+=(i+1)+"、"+songname+"——"+name+"\n";
                                    music_map.put("json", word_list);
                                }
                                search_music_list.put(qun + "_" + uin, music_map);
                                sendText(data,result+"请发送序号选择\n不想点歌请发\"取消点歌\"");
                            }
                            else {
                                sendText(data,"搜索不到，请重试");
                            }
                        }
                    }
                    if(quntext.matches("[0-9]+")&&quntext.length()<=2) {
                        String yyms=读(ColdRainPath+"data/"+qun+"点歌模式.txt");
                        if(yyms.equals("")) yyms="卡片";
                        int index = Integer.parseInt(quntext);
                        boolean tf = isCurrent(search_music_list, qun + "_" + uin);
                        if(index>0 && tf) {
                        MusicMap music_map = search_music_list.get(qun + "_" + uin);
                            if(music_map.type.equals("QQ") && index<=music_map.json.length()) {
                                //QQ音乐
                                index =index-1;
                                String mid = music_map.json.getJSONObject(index).optString("mid");

                                String url=get(myWeb+"qqmusicu.php?mid="+mid+"&adaptive=1&type=中品质&sign=" + md5(SECRET + mid));
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
                                        String song = json.getString("title");
                                        String singer = json.getString("singer");
                                        String cover = json.getString("cover");
                                        String link="https://i.y.qq.com/v8/playsong.html?songmid="+json.get("mid");
                                        sendMusic(qun,song,singer,link,music,cover,"QQ",yyms,mtype);
                                    }
                                    else {
                                        sendText(data,json.get("msg"));
                                    }
                                }
                            }
                            else if(music_map.type.equals("网易") && index<=music_map.json.length()) {
                                //网易音乐
                                index =index-1;
                                String id = music_map.json.getJSONObject(index).optString("id");
                                String jsonString="{\"ids\":\"["+id+"]\",\"br\":320000,\"csrf_token\":\"\"}";
                                String[] r2=weapiEncrypt(jsonString);
                                String url=get(myWeb+"wyjx.php?id="+id+"&params="+java.net.URLEncoder.encode(r2[0])+"&encSecKey="+r2[1]);
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
                                        String cover = json.getString("cover");
                                        String link="https://y.music.163.com/m/song?id="+json.get("id");
                                        sendMusic(qun,song,singer,link,music,cover,"网易",yyms,mtype);
                                    }
                                    else {
                                        sendText(data,json.get("msg"));
                                    }
                                }
                            }
                            else if(music_map.type.equals("酷狗") && index<=music_map.json.length()) {
                                //酷狗音乐
                                index =index-1;
                                String hash = music_map.json.getJSONObject(index).optString("hash");

                                String url=get(myWeb+"kgmusic.php?hash="+hash);
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
                                        String cover = json.getString("cover");
                                        String link = json.getString("link");
                                        sendMusic(qun,song,singer,link,music,cover,"酷狗",yyms,mtype);
                                    }
                                    else {
                                        sendText(data,json.get("msg"));
                                    }
                                }
                            }
                            else if(music_map.type.equals("酷我") && index<=music_map.json.length()) {
                                //酷我音乐
                                index =index-1;
                                String id = music_map.json.getJSONObject(index).optString("DC_TARGETID");

                                String url=get(myWeb+"kwmusic.php?type=320kmp3&id="+id);
                                if(url.equals("访问网页失败")) {
                                    sendText(data,url);
                                }
                                else {
                                    String result="";
                                    JSONObject json = new JSONObject(url);
                                    int code=json.getInt("code");
                                    if(code==0) {
                                        json = json.getJSONObject("data");
                                        String music = json.getString("music").replace("$","=");
                                        String album_name = json.getString("album_name");
                                        String song = json.getString("song");
                                        String singer = json.getString("singer");
                                        String cover = json.getString("cover");
                                        String link = "http://m.kuwo.cn/newh5app/play_detail/"+id;
                                        sendMusic(qun,song,singer,link,music,cover,"波点",yyms,mtype);
                                    }
                                    else {
                                        sendText(data,json.get("msg"));
                                    }
                                }
                            }
                            else if(music_map.type.equals("汽水") && index<=music_map.json.length()) {
                                //汽水音乐
                                index =index-1;
                                String url = music_map.json.getJSONObject(index).toString();

                                if(url.equals("")) {
                                    sendText(data,"汽水点歌失败\n温馨提示:汽水点歌成功后的卡片将用波点发出");
                                }
                                else {
                                    String result="";
                                    JSONObject json = new JSONObject(url);
                                    String music = json.getString("music");
                                    String album_name = json.getInt("duration") + "秒";
                                    String song = json.getString("song");
                                    String singer = json.getString("singer");
                                    String cover = json.getString("cover");
                                    //String link = "https://music.douyin.com/qishui/share/track?track_id=" + json.optString("id") + "&from_item_id=" + json.optString("id") + "&sec_sharer_id=MS4wLjABAAAAulOSUaKs7pgGZ7wUasne9Xf5J3_Lrug0YztGw9Fc42z0Fgg8FaNCHQ4I6jkSx8Yp&use_micro=0&enter_from=feed_music_card&zlink_id=J8yXw&target_app=douyin&hybrid_sdk_version=bullet&auto_play_bgm=1&utm_source=copy&utm_campaign=client_share&utm_medium=android&app=aweme";
                                    sendMusic(qun,song,singer,music,music,cover,"波点",yyms,mtype);
                                }
                            }
                        }
                    }
                    if(quntext.equals("切换链接点歌")) {
                        if(qq.equals(uin)||读(qun,"代管",uin)==1||读("0","代管",uin)==1) {
                            写(ColdRainPath+"data/"+qun+"点歌模式.txt","链接");
                            String menu="已切换为链接点歌";
                            sendText(data,menu);
                        }
                    }
                    if(quntext.equals("切换文件点歌")) {
                        if(qq.equals(uin)||读(qun,"代管",uin)==1||读("0","代管",uin)==1) {
                            写(ColdRainPath+"data/"+qun+"点歌模式.txt","文件");
                            String menu="已切换为文件点歌";
                            sendText(data,menu);
                        }
                    }
                    if(quntext.equals("切换下载点歌")) {
                        if(qq.equals(uin)||读(qun,"代管",uin)==1||读("0","代管",uin)==1) {
                            写(ColdRainPath+"data/"+qun+"点歌模式.txt","下载");
                            String menu="已切换为下载点歌";
                            sendText(data,menu);
                        }
                    }
                    if(quntext.equals("切换卡片点歌")) {
                        if(qq.equals(uin)||读(qun,"代管",uin)==1||读("0","代管",uin)==1) {
                            写(ColdRainPath+"data/"+qun+"点歌模式.txt","卡片");
                            String menu="已切换为卡片点歌";
                            sendText(data,menu);
                        }
                    }
                    if(quntext.equals("取消点歌")) {
                        search_music_list.remove(qun + "_" + uin);
                        String menu="已清除您的所有点歌缓存数据";
                        sendText(data,menu);
                    }
                    if(quntext.equals("切换语音点歌")) {
                        if(qq.equals(uin)||读(qun,"代管",uin)==1||读("0","代管",uin)==1) {
                            写(ColdRainPath+"data/"+qun+"点歌模式.txt","语音");
                            String menu="已切换为语音点歌";
                            sendText(data,menu);
                        }
                    }
                    if(quntext.equals("切换空间点歌")) {
                        if(qq.equals(uin)||读(qun,"代管",uin)==1||读("0","代管",uin)==1) {
                            写(ColdRainPath+"data/"+qun+"点歌模式.txt","空间");
                            String menu="已切换为空间点歌";
                            sendText(data,menu);
                        }
                    }
                    if(quntext.equals("切换播放点歌")) {
                        if(qq.equals(uin)||读(qun,"代管",uin)==1||读("0","代管",uin)==1) {
                            写(ColdRainPath+"data/"+qun+"点歌模式.txt","播放");
                            String menu="已切换为播放点歌";
                            sendText(data,menu);
                        }
                    }
                }
            }
        }
    }
    ).start();
}
public String getMusicUrl(String mid,String media_mid) {
    try {
        String Quality=文字("0","音乐质量","质量");
        String cookie=文字("0","音乐ck","cookie");
        if(cookie.equals("")||!isMusicLogin(cookie)) {
            cookie=accountlogin();
            写("0","音乐ck","cookie",cookie);
        }
        get(myWeb+"qmusic.php?uin="+myUin+"&Cookie=uin="+cookie);
        if(Quality.equals("m4a")||Quality.equals("")) {
            String url2=httpget("https://i.y.qq.com/v8/playsong.html?platform=11&appshare=android_qq&appversion=12080008&hosteuin=&songmid="+mid+"&type=0&appsongtype=1&_wv=1&source=qq&ADTAG=qfshare",cookie);
            int index2 = url2.lastIndexOf("window.__ssrFirstPageData__ =");
            String text2 = url2.substring(index2 + 29);
            int rd2 = text2.indexOf("}</");
            String re2 = text2.substring(0,rd2+1);
            JSONObject json3=new JSONObject(re2);
            String songList2=json3.getString("songList");
            if(songList2.contains("\"url\":\"http")) {
                int index3 = songList2.lastIndexOf(",\"url\":\"");
                String text3 = songList2.substring(index3 + 8);
                int rd3 = text3.indexOf("\",\"ppurl\":\"");
                String re3 = text3.substring(0,rd3);
                return re3.replace("\\","");
            }
            else {
                String url2=httppost1("https://u.y.qq.com/cgi-bin/musicu.fcg",cookie,"{\"comm\":{\"uin\":\""+myUin+"\",\"authst\":\"\",\"mina\":1,\"appid\":1109523715,\"ct\":29},\"urlReq0\":{\"module\":\"vkey.GetVkeyServer\",\"method\":\"CgiGetVkey\",\"param\":{\"guid\":\"7982463958\",\"songmid\":[\""+mid+"\"],\"songtype\":[0],\"filename\":[\"C400"+media_mid+".m4a\"],\"uin\":\""+myUin+"\",\"loginflag\":1,\"platform\":\"23\"}}}");
                if(url2.contains("\"purl\":\"")) {
                    int index3 = url2.lastIndexOf("\"purl\":\"");
                    String text3 = url2.substring(index3 + 8);
                    int rd3 = text3.indexOf("\",\"errtype\":\"");
                    String re3 = text3.substring(0,rd3);
                    if(!re3.equals("")) {
                        return u解(re3);
                    }
                    else {
                        return null;
                    }
                }
            }
        }
        else if(Quality.equals("mp3")) {
            String url2=httppost1("https://u.y.qq.com/cgi-bin/musicu.fcg",cookie,"{\"comm\":{\"uin\":\""+myUin+"\",\"authst\":\"\",\"mina\":1,\"appid\":1109523715,\"ct\":29},\"urlReq0\":{\"module\":\"vkey.GetVkeyServer\",\"method\":\"CgiGetVkey\",\"param\":{\"guid\":\"7982463958\",\"songmid\":[\""+mid+"\"],\"songtype\":[0],\"filename\":[\"M800"+media_mid+".mp3\"],\"uin\":\""+myUin+"\",\"loginflag\":1,\"platform\":\"23\"}}}");
            if(url2.contains("\"purl\":\"")) {
                int index3 = url2.lastIndexOf("\"purl\":\"");
                String text3 = url2.substring(index3 + 8);
                int rd3 = text3.indexOf("\",\"errtype\":\"");
                String re3 = text3.substring(0,rd3);
                if(!re3.equals("")) {
                    return u解(re3);
                }
                else {
                    return null;
                }
            }
        }
        else if(Quality.equals("flac")) {
            String url2=httppost1("https://u.y.qq.com/cgi-bin/musicu.fcg",cookie,"{\"comm\":{\"uin\":\""+myUin+"\",\"authst\":\"\",\"mina\":1,\"appid\":1109523715,\"ct\":29},\"urlReq0\":{\"module\":\"vkey.GetVkeyServer\",\"method\":\"CgiGetVkey\",\"param\":{\"guid\":\"7982463958\",\"songmid\":[\""+mid+"\"],\"songtype\":[0],\"filename\":[\"F000"+media_mid+".flac\"],\"uin\":\""+myUin+"\",\"loginflag\":1,\"platform\":\"23\"}}}");
            if(url2.contains("\"purl\":\"")) {
                int index3 = url2.lastIndexOf("\"purl\":\"");
                String text3 = url2.substring(index3 + 8);
                int rd3 = text3.indexOf("\",\"errtype\":\"");
                String re3 = text3.substring(0,rd3);
                if(!re3.equals("")) {
                    return u解(re3);
                }
                else {
                    String url=getMusicUrlbyMid(mid);
                    if(url.equals("")||url==null) {
                        return null;
                    }
                    else {
                        return url;
                    }
                }
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
/*
new Thread(new Runnable() {
    public void run() {
        String cookie=文字("0","音乐ck","cookie");
        if(cookie.equals("")||!isMusicLogin(cookie)) {
            cookie=accountlogin();
            写("0","音乐ck","cookie",cookie);
        }
        get(myWeb+"qmusic.php?uin="+myUin+"&Cookie="+cookie);
    }
}
).start();
*/

public boolean isCurrent(HashMap map, String key) {
    if (map == null) {
        return false;
    }
    
    if (map.isEmpty()) {
        return false;
    }
    
    if(map.containsKey(key)) {
        if(map.get(key) == null) {
             return false;
        } else {
             return true;
        }
    }
    
    return false;
}