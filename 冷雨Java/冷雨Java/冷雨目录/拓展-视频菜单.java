List captionList=new ArrayList();
List authorList=new ArrayList();
List durationList=new ArrayList();
List likeCountList=new ArrayList();
List viewCountList=new ArrayList();
List photoUrlList=new ArrayList();
List coverUrlList=new ArrayList();
List coverList=new ArrayList();
List uriList=new ArrayList();
List titleList=new ArrayList();
List author2List=new ArrayList();
List imgList=new ArrayList();
List videoList=new ArrayList();
List descList=new ArrayList();
List artistList=new ArrayList();
public void 视频菜单(Object Yu) {
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
                if(quntext.equals("开启视频菜单")) {
                    if(qq.equals(uin)||读(qun,"代管",uin)==1||读("0","代管",uin)==1) {
                        写(qun,"视频菜单","开关",1);
                        String menu="已开启本聊天视频菜单";
                        sendText(data,menu);
                    }
                }
                if(quntext.equals("关闭视频菜单")) {
                    if(qq.equals(uin)||读(qun,"代管",uin)==1||读("0","代管",uin)==1) {
                        写(qun,"视频菜单","开关",0);
                        String menu="已关闭本聊天视频菜单";
                        sendText(data,menu);
                    }
                }
                if(quntext.equals("视频菜单")) {
                    if(读(qun,"视频菜单","开关")==1) {
                        String menu = "视频菜单:\n纯情女高 蛇姐系列\n狱卒系列 动漫系列\n帅哥系列 网抑系列\n漫画芋系 冷雨推荐\n治愈系列 随机快手\n小姐姐系 等待添加\n快手搜索+内容\n哔哩哔哩+内容\n抖音搜索+内容";
                        sendText(data,menu);
                    }
                    else {
                        sendText(data,"本聊天未开启视频菜单");
                    }
                }
                if(读(qun,"视频菜单","开关")==1) {
                    if(quntext.startsWith("抖音搜索")) {
                        String url=get(myWeb+"dySearch.php?text="+quntext.substring(4));
                        JSONObject json=new JSONObject(url);
                        if(json.getInt("code")==0) {
                            JSONArray jsona=json.getJSONArray("data");
                            String result="";
                            artistList.clear();
                            //durationList.clear();
                            descList.clear();
                            videoList.clear();
                            //viewCountList.clear();
                            imgList.clear();
                            //photoUrlList.clear();
                            for(int i=0;
                            i<jsona.length();
                            i++) {
                                JSONObject j=jsona.getJSONObject(i);
                                String author=j.getJSONObject("author").getString("author");
                                String caption=j.getString("desc");
                                String video=j.getString("video");
                                String cover=j.getString("cover");
                                artistList.add(author);
                                descList.add(caption);
                                imgList.add(cover);
                                videoList.add(video);
                                result+=(i+1)+"、"+caption+"--"+author+"\n";
                            }
                            sendText(data,result+"发送序号选择");
                        }
                        else {
                            sendText(data,json.getString("msg"));
                        }
                    }
                    if(quntext.startsWith("快手搜索")) {
                        String url=get(myWeb+"ksSearch.php?text="+quntext.substring(4));
                        JSONObject json=new JSONObject(url);
                        if(json.getInt("code")==0) {
                            JSONArray jsona=json.getJSONArray("data");
                            String result="";
                            authorList.clear();
                            durationList.clear();
                            captionList.clear();
                            likeCountList.clear();
                            viewCountList.clear();
                            coverUrlList.clear();
                            photoUrlList.clear();
                            for(int i=0;
                            i<jsona.length();
                            i++) {
                                JSONObject j=jsona.getJSONObject(i);
                                String author=j.getJSONObject("author").getString("name");
                                JSONObject jj=j.getJSONObject("photo");
                                long duration=jj.getLong("duration");
                                String caption=jj.getString("caption");
                                String likeCount=jj.getString("likeCount");
                                String viewCount=jj.optString("viewCount");
                                String coverUrl=jj.getString("coverUrl");
                                String photoUrl=jj.getString("photoUrl");
                                authorList.add(author);
                                durationList.add(duration);
                                captionList.add(caption);
                                likeCountList.add(likeCount);
                                viewCountList.add(viewCount);
                                coverUrlList.add(coverUrl);
                                photoUrlList.add(photoUrl);
                                result+=(i+1)+"、"+caption+"--"+author+"\n";
                            }
                            sendText(data,result+"发送序号选择");
                        }
                        else {
                            sendText(data,json.getString("msg"));
                        }
                    }
                    if(quntext.matches("[0-9]+")&&quntext.length()<3&&Long.parseLong(quntext)>0&&Long.parseLong(quntext)<descList.size()+1) {
                        int i=Long.parseLong(quntext)-1;
                        sendText(data,"[pic="+imgList.get(i)+"]描述:"+descList.get(i)+"\n作者:"+artistList.get(i));
                        sendVideo(mtype,qun,videoList.get(i));
                        artistList.clear();
                        //durationList.clear();
                        descList.clear();
                        videoList.clear();
                        //viewCountList.clear();
                        imgList.clear();
                        //photoUrlList.clear();
                    }
                    if(quntext.equals("随机快手")) {
                        String url=get(myWeb+"ksRandom.php");
                        JSONObject json=new JSONObject(url);
                        if(json.getInt("code")==0) {
                            JSONArray jsona=json.getJSONArray("data");
                            String result="";
                            authorList.clear();
                            durationList.clear();
                            captionList.clear();
                            likeCountList.clear();
                            viewCountList.clear();
                            coverUrlList.clear();
                            photoUrlList.clear();
                            for(int i=0;
                            i<jsona.length();
                            i++) {
                                JSONObject j=jsona.getJSONObject(i);
                                String author=j.getJSONObject("author").getString("name");
                                JSONObject jj=j.getJSONObject("photo");
                                long duration=jj.getLong("duration");
                                String caption=jj.getString("caption");
                                String likeCount=jj.getString("likeCount");
                                String viewCount=jj.optString("viewCount");
                                String coverUrl=jj.getString("coverUrl");
                                String photoUrl=jj.getString("photoUrl");
                                authorList.add(author);
                                durationList.add(duration);
                                captionList.add(caption);
                                likeCountList.add(likeCount);
                                viewCountList.add(viewCount);
                                coverUrlList.add(coverUrl);
                                photoUrlList.add(photoUrl);
                                result+=(i+1)+"、"+caption+"--"+author+"\n";
                            }
                            sendText(data,result+"发送序号选择");
                        }
                        else {
                            sendText(data,json.getString("msg"));
                        }
                    }
                    if(quntext.matches("[0-9]+")&&quntext.length()<3&&Long.parseLong(quntext)>0&&Long.parseLong(quntext)<captionList.size()+1) {
                        int i=Long.parseLong(quntext)-1;
                        sendText(data,"[pic="+coverUrlList.get(i)+"]描述:"+captionList.get(i)+"\n作者:"+authorList.get(i)+"\n时长:"+durationList.get(i)/1000+"秒\n点赞量:"+likeCountList.get(i)+"\n播放量:"+viewCountList.get(i));
                        sendVideo(mtype,qun,photoUrlList.get(i));
                        authorList.clear();
                        durationList.clear();
                        captionList.clear();
                        likeCountList.clear();
                        viewCountList.clear();
                        coverUrlList.clear();
                        photoUrlList.clear();
                    }
                    if(quntext.startsWith("哔哩哔哩")) {
                        String msg=quntext.substring(4);
                        String result="";
                        String url=get("http://app.bilibili.com/x/v2/search?appkey=1d8b6e7d45233436&build=560161&duration=0&keyword="+msg+"&mobi_app=android&platform=android&pn=1&ps=10&ts=1534807273&sign=58fec668fa2fb65a5149d04aeca15cbb");
                        JSONObject json=new JSONObject(url);
                        String code=json.optString("code");
                        if(code.equals("0")) {
                            coverList.clear();
                            titleList.clear();
                            uriList.clear();
                            author2List.clear();
                            String data1=json.getString("data");
                            JSONObject json1=new JSONObject(data1);
                            String items=json1.getString("items");
                            JSONObject json2=new JSONObject(items);
                            JSONArray archive=json2.getJSONArray("archive");
                            for(int q=0;
                            q<archive.length();
                            q++) {
                                JSONObject List=archive.get(q);
                                String title=List.get("title");
                                //标题
                                String cover=List.get("cover");
                                //图片
                                String author=List.get("author");
                                //作者
                                String view_content=List.get("view_content");
                                //播放次数
                                String share=List.getString("share");
                                JSONObject json3=new JSONObject(share);
                                String video=json3.getString("video");
                                JSONObject json4=new JSONObject(video);
                                String uri=json4.getString("short_link");
                                //链接
                                result+=(q+1)+"、"+title+"——"+author+"("+view_content+")\n";
                                coverList.add(cover);
                                titleList.add(title);
                                uriList.add(uri);
                                author2List.add(author);
                            }
                            sendText(data,""+result);
                        }
                        else {
                            sendText(data,"未搜索到");
                        }
                    }
                    if(quntext.matches("[0-9]+")&&quntext.length()<3&&Long.parseLong(quntext)>0&&Long.parseLong(quntext)<titleList.size()+1) {
                        try {
                            int i=Long.parseLong(quntext)-1;
                            String uri=uriList.get(i);
                            //String url=get(uri);
                            int index = uri.lastIndexOf("https://b23.tv/");
                            String id = uri.substring(index + "https://b23.tv/".length());
                            //String id=findIDByUrl(msgs);
                            String url=get("https://api.bilibili.com/x/web-interface/view/detail?aid=&bvid="+id);
                            JSONObject json=new JSONObject(url);
                            if(json.getInt("code")==0) {
                                json=json.getJSONObject("data");
                                JSONObject json2=json.getJSONObject("View");
                                JSONObject json3=json2.getJSONObject("stat");
                                JSONObject json4=json2.getJSONObject("owner");
                                sendText(data,json2.getString("title")+"[pic="+json2.getString("pic")+"]\n作者:"+json4.getString("name")+"\n播放量:"+json3.optString("view")+"\n点赞量:"+json3.optString("like")+"\n正在输出视频:320p(流畅)\n时长:"+json2.optString("duration")+"秒");
                                url=get("https://api.bilibili.com/x/player/playurl?cid="+json2.optString("cid")+"&avid=&bvid="+id+"&otype=json&platform=html5&type=mp4&html5=1");
                                json=new JSONObject(url);
                                //删除(qun,"哔哩哔哩");
                                
                                coverList.clear();
                                titleList.clear();
                                uriList.clear();
                                author2List.clear();
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
                    //qqsendTroopNews(qun,title,title+"\n"+author,readyVideoUrl,cover);
                    if(quntext.equals("小姐姐系")) {
                        String url=myWeb+"xjj.php";
                        sendVideo(mtype,qun,url);
                    }
                    if(quntext.equals("纯情女高")) {
                        String url=myWeb+"cqng.php";
                        sendVideo(mtype,qun,url);
                    }
                    if(quntext.equals("蛇姐系列")) {
                        String url=myWeb+"sjxl.php";
                        sendVideo(mtype,qun,url);
                    }
                    if(quntext.equals("狱卒系列")) {
                        String url=myWeb+"yzxl.php";
                        sendVideo(mtype,qun,url);
                    }
                    if(quntext.equals("动漫系列")) {
                        String url=myWeb+"dmxl.php";
                        sendVideo(mtype,qun,url);
                    }
                    if(quntext.equals("帅哥系列")) {
                        String url=myWeb+"sgxl.php";
                        sendVideo(mtype,qun,url);
                    }
                    if(quntext.equals("网抑系列")) {
                        String url=myWeb+"emo.php";
                        sendVideo(mtype,qun,url);
                    }
                    if(quntext.equals("漫画芋系")) {
                        String url=myWeb+"mhy.php";
                        sendVideo(mtype,qun,url);
                    }
                    if(quntext.equals("治愈系列")) {
                        String url=myWeb+"zyxl.php";
                        sendVideo(mtype,qun,url);
                    }
                    if(quntext.equals("冷雨推荐")) {
                        String url=myWeb+"sp.php";
                        sendVideo(mtype,qun,url);
                    }
                }
            }
        }
    }
    ).start();
}