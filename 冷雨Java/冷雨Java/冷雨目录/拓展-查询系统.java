public void 查询系统(Object Yu) {
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
                if(quntext.equals("开启查询系统")) {
                    if(qq.equals(uin)||读(qun,"代管",uin)==1||读("0","代管",uin)==1) {
                        写(qun,"查询系统","开关",1);
                        String menu="已开启本聊天查询系统";
                        sendText(data,menu);
                    }
                }
                if(quntext.equals("关闭查询系统")) {
                    if(qq.equals(uin)||读(qun,"代管",uin)==1||读("0","代管",uin)==1) {
                        写(qun,"查询系统","开关",0);
                        String menu="已关闭本聊天查询系统";
                        sendText(data,menu);
                    }
                }
                if(quntext.equals("查询系统")) {
                    if(读(qun,"查询系统","开关")==1) {
                        String menu = "查询系统:\n查Q音@QQ/+QQ\n查访客/任务@QQ/+QQ\n查达人/等级@QQ/+QQ\n查QID/信息@QQ/+QQ\n查询信息@QQ/+QQ";
                        sendText(data,menu);
                    }
                    else {
                        sendText(data,"本聊天未开启查询系统");
                    }
                }
                if(读(qun,"查询系统","开关")==1) {
                    /*
if(quntext.startsWith("查战力#")){
String one=quntext.split("#")[1];
String two=quntext.split("#")[2];
two.replace("微信","wx");
two.replace("QQ","qq");
two.replace("扣扣","qq");
if(two.equals("wx")||two.equals("qq")){
String url=get(myWeb+"wz_zhanli.php?select=getHeroInfo&name="+one+"&platform="+two);
JSONObject json = new JSONObject(url);
if(json.getInt("code")==0){
json = json.getJSONObject("data");
String text = "[pic=" + json.getString("photo") + "]" +
             "英雄:" + json.getString("name") + "(" + json.getString("alias") + ")" +
              "\nIP:" + " " + json.getString("city") + " " + json.getString("area") +
              "\n国标:" + json.getString("guobiao") +
              "\n更新时间:" + json.getString("updatetime");
sendText(data,text);
}else{
sendText(data,json.getString("msg"));
}
}else{
sendText(data,"区不对，请输入qq(QQ区),wx(微信区)");
}
}
*/
                    if(quntext.matches("查Q音[0-9]+")) {
                        String at = quntext.substring(3);
                        String text = get(myWeb+"music_cha.php?uin="+at);
                        if( text.isEmpty() || text.equals("访问网页失败") ) {
                            sendText(data , "接口出错！");
                            return;
                        }
                        JSONObject json = new JSONObject(text);
                        if(json.getInt("code")==0) {
                            json = json.getJSONObject("data");
                            text = "[pic="+json.getString("avatar")+"]"+
                            "\nQQ:"+json.getString("uin")+
                            "\n昵称:"+json.getString("name")+
                            "\n性别:"+json.getString("gender")+
                            "\n星座:"+json.getString("constellation")+
                            "\n粉丝:"+json.getLong("fans_num")+
                            "\n关注:"+json.getLong("follow_num")+
                            "\n访客:"+json.getLong("visitor")+
                            "\n地址:"+json.getString("ip")+
                            "\n头像修改:"+timestampToDate(json.getLong("avatar_modify_time")*1000);
                            String msg = get(myWeb+"qq_music.php?uin="+at+"&select=%E5%A4%9A%E9%80%89");
                            if( !msg.isEmpty() && !msg.equals("访问网页失败") ) {
                                json = new JSONObject(msg);
                                if(json.getInt("code") == 0) {
                                    JSONArray array = json.getJSONArray("music_list");
                                    if( array.length() >= 1 ) {
                                        text += "\n收藏歌单列表:";
                                    }
                                    for(int i = 0 ;
                                    i < array.length() ;
                                    i++ ) {
                                        JSONObject jSONObject = array.getJSONObject(i);
                                        text += "\n歌单:"+jSONObject.getString("title")+
                                        "\n详情:"+jSONObject.getString("info")+
                                        "\n歌单id:"+jSONObject.getLong("id");
                                    }
                                }
                            }
                            sendText(data,""+text);
                        }
                        else {
                            sendText(data,json.getString("msg"));
                        }
                    }
                    if(quntext.startsWith("查Q音@")) {
                        String at=data.atList.get(0);
                        String text = get(myWeb+"music_cha.php?uin="+at);
                        if( text.isEmpty() || text.equals("访问网页失败") ) {
                            sendText(data , "接口出错！");
                            return;
                        }
                        JSONObject json = new JSONObject(text);
                        if(json.getInt("code")==0) {
                            json = json.getJSONObject("data");
                            text = "[pic="+json.getString("avatar")+"]"+
                            "\nQQ:"+json.getString("uin")+
                            "\n昵称:"+json.getString("name")+
                            "\n性别:"+json.getString("gender")+
                            "\n星座:"+json.getString("constellation")+
                            "\n粉丝:"+json.getLong("fans_num")+
                            "\n关注:"+json.getLong("follow_num")+
                            "\n访客:"+json.getLong("visitor")+
                            "\n地址:"+json.getString("ip")+
                            "\n头像修改:"+timestampToDate(json.getLong("avatar_modify_time")*1000);
                            String msg = get(myWeb+"qq_music.php?uin="+at+"&select=%E5%A4%9A%E9%80%89");
                            if( !msg.isEmpty() && !msg.equals("访问网页失败") ) {
                                json = new JSONObject(msg);
                                if(json.getInt("code") == 0) {
                                    JSONArray array = json.getJSONArray("music_list");
                                    if( array.length() >= 1 ) {
                                        text += "\n收藏歌单列表:";
                                    }
                                    for(int i = 0 ;
                                    i < array.length() ;
                                    i++ ) {
                                        JSONObject jSONObject = array.getJSONObject(i);
                                        text += "\n歌单:"+jSONObject.getString("title")+
                                        "\n详情:"+jSONObject.getString("info")+
                                        "\n歌单id:"+jSONObject.getLong("id");
                                    }
                                }
                            }
                            sendText(data,""+text);
                        }
                        else {
                            sendText(data,json.getString("msg"));
                        }
                    }
                    if(quntext.matches("查QID[0-9]+")) {
                        String at=quntext.substring(4);
                        String text=getQID(at);
                        sendText(data,""+text);
                    }
                    if(quntext.startsWith("查QID@")) {
                        String at=data.atList.get(0);
                        String text=getQID(at);
                        sendText(data,""+text);
                    }
                    if(quntext.matches("查达人[0-9]+")) {
                        String at=quntext.substring(3);
                        String text=getDarenDays(at);
                        sendText(data,""+text);
                    }
                    if(quntext.startsWith("查达人@")) {
                        String at=data.atList.get(0);
                        String text=getDarenDays(at);
                        sendText(data,""+text);
                    }
                    if(quntext.startsWith("查等级@")) {
                        String at=data.atList.get(0);
                        String menu=getVipInfo1(at);
                        sendText(data,menu);
                    }
                    if(quntext.matches("查等级[0-9]+")) {
                        String at=quntext.substring(3);
                        String menu=getVipInfo1(at);
                        sendText(data,menu);
                    }
                    /*
                    if(quntext.startsWith("查注册@")) {
                        String at=data.atList.get(0);
                        searchUserRegTime(at, new protoListener() {
                            public boolean onResponse() {
                                return true;
                            }
    
                            public void onSuccess(String cmd, JSONObject json) {
                                try {
                                    long regTime = json.getLong("regTime");
                                    String uin = json.getString("uin");
                                    sendText(data, "QQ: " + uin + "\n注册时间: " + timestampToDate(regTime));
                                } catch (Exception e) {
                                    sendText(data, "解析失败: " + e.getMessage());
                                }
                            }
    
                            public void onFailure(String cmd, String error) {
                                sendText(data, "获取失败: " + error);
                            }
                        });
                    }
                    if(quntext.matches("查注册[0-9]+")) {
                        String at=quntext.substring(3);
                        searchUserRegTime(at, new protoListener() {
                            public boolean onResponse() {
                                return true;
                            }
    
                            public void onSuccess(String cmd, JSONObject json) {
                                try {
                                    long regTime = json.getLong("regTime");
                                    String uin = json.getString("uin");
                                    sendText(data, "QQ: " + uin + "\n注册时间: " + timestampToDate(regTime));
                                } catch (Exception e) {
                                    sendText(data, "解析失败: " + e.getMessage());
                                }
                            }
    
                            public void onFailure(String cmd, String error) {
                                sendText(data, "获取失败: " + error);
                            }
                        });
                    }
                    */
                    if(quntext.matches("查询信息[0-9]+")) {
                        String at=quntext.substring(4);
                        long gtk=GetGTK(qzone);
                        String cookie="p_uin=o0"+qq+";skey="+skey+";p_skey="+qzone;
                        String url="https://r.qzone.qq.com/cgi-bin/user/cgi_personal_card?uin="+at+"&remark=0&g_tk="+gtk;
                        String nm=httpget(url,cookie);
                        String nv=nm.replace("_Callback(","");
                        String nan=nv.replace(");","");
                        String boy=nan.replace("\n","");
                        JSONObject json1 = new JSONObject(boy);
                        String UIN=json1.optString("uin");
                        //QQ
                        String intimacyScore=json1.optString("intimacyScore");
                        //亲密度
                        String qzonee=json1.optString("qzone").replace("1","有").replace("1","无");
                        //空间是否有权访问
                        String realname=json1.get("realname");
                        //自己给别人的备注
                        String nickname=json1.get("nickname");
                        //昵称
                        long logolabel=Long.parseLong(json1.get("logolabel"));
                        //标识时间(大概是)
                        String qqvip=json1.optString("qqvip");
                        //SVIP等级
                        String greenvip=json1.optString("greenvip");
                        //绿钻等级
                        String gender=json1.optString("gender").replace("1","男").replace("2","女");
                        //性别
                        String isFriend=json1.optString("isFriend").replace("1","是").replace("0","否");
                        //是否为好友
                        String commfrd=json1.optString("commfrd");
                        //共同好友
                        String isSpecialCare=json1.optString("isSpecialCare").replace("1","是");
                        //是否为特别关心
                        String biaoshi=timestampToDate(logolabel*1000);
                        String menu="QQ:"+UIN+"\n昵称:"+nickname+"\n性别:"+gender+"\n备注:"+realname+"\n亲密度:"+intimacyScore+"\n空间有无访问权限:"+qzonee+"\n是否为特别关心:"+isSpecialCare+"\n是否为好友:"+isFriend+"\n标识时间:"+biaoshi+"\n共同好友数量:"+commfrd+"\nSVIP等级:"+qqvip+"\n绿钻等级:"+greenvip;
                        sendText(data,menu+"\n"+getUserInfo2(at));
                    }
                    if(quntext.startsWith("查询信息@")) {
                        String at=data.atList.get(0);
                        long gtk=GetGTK(qzone);
                        String cookie="p_uin=o0"+qq+";skey="+skey+";p_skey="+qzone;
                        String url="https://r.qzone.qq.com/cgi-bin/user/cgi_personal_card?uin="+at+"&remark=0&g_tk="+gtk;
                        String nm=httpget(url,cookie);
                        String nv=nm.replace("_Callback(","");
                        String nan=nv.replace(");","");
                        String boy=nan.replace("\n","");
                        JSONObject json1 = new JSONObject(boy);
                        String UIN=json1.optString("uin");
                        //QQ
                        String intimacyScore=json1.optString("intimacyScore");
                        //亲密度
                        String qzonee=json1.optString("qzone").replace("1","有").replace("1","无");
                        //空间是否有权访问
                        String realname=json1.get("realname");
                        //自己给别人的备注
                        String nickname=json1.get("nickname");
                        //昵称
                        long logolabel=Long.parseLong(json1.get("logolabel"));
                        //标识时间(大概是)
                        String qqvip=json1.optString("qqvip");
                        //SVIP等级
                        String greenvip=json1.optString("greenvip");
                        //绿钻等级
                        String gender=json1.optString("gender").replace("1","男").replace("2","女");
                        //性别
                        String isFriend=json1.optString("isFriend").replace("1","是").replace("0","否");
                        //是否为好友
                        String commfrd=json1.optString("commfrd");
                        //共同好友
                        String isSpecialCare=json1.optString("isSpecialCare").replace("1","是");
                        //是否为特别关心
                        String biaoshi=timestampToDate(logolabel*1000);
                        String menu="QQ:"+UIN+"\n昵称:"+nickname+"\n性别:"+gender+"\n备注:"+realname+"\n亲密度:"+intimacyScore+"\n空间有无访问权限:"+qzonee+"\n是否为特别关心:"+isSpecialCare+"\n是否为好友:"+isFriend+"\n标识时间:"+biaoshi+"\n共同好友数量:"+commfrd+"\nSVIP等级:"+qqvip+"\n绿钻等级:"+greenvip;
                        sendText(data,menu+"\n"+getUserInfo2(at));
                    }
                    if(quntext.startsWith("查访客@")) {
                        String at=data.atList.get(0);
                        String menu=getQzoneVisiters(at);
                        sendText(data,menu);
                    }
                    if(quntext.matches("查访客[0-9]+")) {
                        String at=quntext.substring(3);
                        String menu=getQzoneVisiters(at);
                        sendText(data,menu);
                    }
                    if(quntext.startsWith("查任务@")) {
                        String at=data.atList.get(0);
                        String menu=getVipInfo1(at)+"\n"+getVipInfo3(at);
                        sendText(data,menu);
                    }
                    if(quntext.matches("查任务[0-9]+")) {
                        String at=quntext.substring(3);
                        String menu=getVipInfo1(at)+"\n"+getVipInfo3(at);
                        sendText(data,menu);
                    }
                    if(quntext.startsWith("查信息@")) {
                        String at=data.atList.get(0);
                        String menu=getUserInfo2(at);
                        sendText(data,menu);
                    }
                    if(quntext.matches("查信息[0-9]+")) {
                        String at=quntext.substring(3);
                        String menu=getUserInfo2(at);
                        sendText(data,menu);
                    }
                }
            }
        }
    }
    ).start();
}
public String getDarenDays(String uin)
{
    try
    {
        String url = "https://cgi.vip.qq.com/card/getExpertInfo?ps_tk=" + GetGTK(vippskey) + "&fuin=" + uin + "&g_tk=" + GetGTK(vippskey);
        String cookie = "uin=o" + myUin + "; skey=" + getSkey() + "; p_uin=o" + myUin + "; p_skey=" + getPskey("vip.qq.com");
        String post = cookieGetWithRefer(url, cookie, "https://club.vip.qq.com/");
        JSONObject json = new JSONObject(post);
        int code = json.getInt("ret");
        if(code == 0)
        {
            String data1 = json.getString("data");
            JSONObject json1 = new JSONObject(data1);
            String g = json1.getString("g");
            int rd = g.indexOf(",");
            String gg = g.substring(rd + 1);
            int rr = gg.indexOf(",");
            String ggg = gg.substring(rr + 1);
            String gggg = gg.replace(ggg, "");
            return "QQ:" + uin + "\n达人天数:" + gggg.replace(",", "");
        }
        else
        {
            return "查询失败";
        }
    }
    catch(e)
    {
        return "设置失败:" + e;
    }
}
public String getQID(String uin)
{
    try
    {
        String url = "https://club.vip.qq.com/api/trpc/qid_server/GetQid?g_tk=" + GetGTK(vippskey);
        String dataJson = "{\"uin\":" + uin + "}";
        String cookie = "uin=o" + myUin + "; skey=" + getSkey() + "; p_uin=o" + myUin + "; p_skey=" + getPskey("vip.qq.com");
        String post = httppost5(url, cookie, dataJson);
        JSONObject json = new JSONObject(post);
        int code = json.getInt("code");
        if(code == 0)
        {
            String data1 = json.getString("data");
            JSONObject json1 = new JSONObject(data1);
            String qid = json1.get("qid");
            return "QQ:" + uin + "\nQID:" + qid;
        }
        else
        {
            return "查询失败";
        }
    }
    catch(e)
    {
        return "查询失败:" + e;
    }
}
public String httppost5(String urlPath, String cookie, String data)
{
    StringBuffer buffer = new StringBuffer();
    InputStreamReader isr = null;
    try
    {
        URL url = new URL(urlPath);
        uc = (HttpURLConnection) url.openConnection();
        uc.setDoInput(true);
        uc.setDoOutput(true);
        uc.setConnectTimeout(2000000);
        // 设置连接主机超时（单位：毫秒）
        uc.setReadTimeout(2000000);
        // 设置从主机读取数据超时（单位：毫秒）
        uc.setRequestMethod("POST");
        uc.setRequestProperty("Host", "club.vip.qq.com");
        uc.setRequestProperty("Connection", "keep-alive");
        uc.setRequestProperty("Content-Length", "18");
        uc.setRequestProperty("Accept", "application/json, text/plain,*/*");
        uc.setRequestProperty("qname-service", "976321:131072");
        uc.setRequestProperty("qname-space", "Production");
        uc.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 13; V2166BA Build/TP1A.220624.014; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/109.0.5414.86 MQQBrowser/6.2 TBS/046715 Mobile Safari/537.36 V1_AND_SQ_8.9.83_4680_YYB_D QQ/8.9.83.12605 NetType/WIFI WebP/0.3.0 AppId/537178657 Pixel/1080 StatusBarHeight/100 SimpleUISwitch/0 QQTheme/1000 StudyMode/0 CurrentMode/0 CurrentFontScale/1.0 GlobalDensityScale/0.90000004 AllowLandscape/false InMagicWin/0");
        uc.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
        uc.setRequestProperty("Origin", "https://qun.qq.com");
        uc.setRequestProperty("Sec-Fetch-Site", "same-origin");
        uc.setRequestProperty("Sec-Fetch-Mode", "cors");
        uc.setRequestProperty("Sec-Fetch-Dest", "empty");
        uc.setRequestProperty("Referer", "https://club.vip.qq.com/qid/card?_wv=16777216&_proxy=0&src=icon&from=icon&trace_detail=base64-eyJhcHBpZCI6Im91dHNpZGUiLCJwYWdlX2lkIjoiNzgifQ%3D%3D&_proxyByURL=1");
        uc.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9,en-US;q=0.8,en;q=0.7");
        uc.setRequestProperty("Cookie", "" + cookie);
        uc.getOutputStream().write(data.getBytes("UTF-8"));
        uc.getOutputStream().flush();
        uc.getOutputStream().close();
        isr = new InputStreamReader(uc.getInputStream(), "utf-8");
        BufferedReader reader = new BufferedReader(isr);
        //缓冲
        String line;
        while((line = reader.readLine()) != null)
        {
            buffer.append(line + "\n");
        }
    }
    catch(Exception e)
    {
        e.printStackTrace();
    }
    finally
    {
        try
        {
            if(null != isr)
            {
                isr.close();
            }
        }
        catch(IOException e)
        {
            Toast( "错误:\n" + e);
        }
    }
    if(buffer.length() == 0) return buffer.toString();
    buffer.delete(buffer.length() - 1, buffer.length());
    return buffer.toString();
}
public String cookieGetWithRefer(String url, String cookie, String refer)
{
    StringBuffer buffer = new StringBuffer();
    InputStreamReader isr = null;
    try
    {
        URL urlObj = new URL(url);
        URLConnection uc = urlObj.openConnection();
        uc.setRequestProperty("Cookie", "" + cookie);
        uc.setRequestProperty("Referer", "" + refer);
        uc.setConnectTimeout(10000);
        uc.setReadTimeout(10000);
        isr = new InputStreamReader(uc.getInputStream(), "utf-8");
        BufferedReader reader = new BufferedReader(isr);
        String line;
        while((line = reader.readLine()) != null)
        {
            buffer.append(line + "\n");
        }
    }
    catch(Exception e)
    {
        e.printStackTrace();
    }
    finally
    {
        try
        {
            if(null != isr)
            {
                isr.close();
            }
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
    }
    if(buffer.length() == 0) return buffer.toString();
    buffer.delete(buffer.length() - 1, buffer.length());
    return buffer.toString();
}
public String getQzoneVisiters(String uin)
{
    try
    {
        String pskey = getPskey("qzone.qq.com");
        String cookie = "uin=o" + myUin + "; skey=" + skey + "; p_uin=o" + myUin + "; p_skey=" + pskey;
        String url = httpget("https://h5.qzone.qq.com/webapp/json/vpageCover_v2/getMainPage?g_tk=" + GetGTK(pskey) + "&uin=" + uin + "&visituin=" + myUin + "&force=1&format=json", cookie);
        JSONObject json = new JSONObject(url);
        if(json.getInt("ret") == 0)
        {
            String msg = json.get("msg");
            if(!msg.equals("")) msg = msg + "\n";
            else msg = msg;
            JSONObject profile = json.getJSONObject("data").getJSONObject("profile");
            String nickname = profile.optString("nickname");
            //昵称
            String qzonename = profile.optString("qzonename");
            //空间名称
            String gender = profile.optString("gender").replace("1", "男").replace("2", "女").replace("0", "未知");
            //性别
            String astro = profile.optString("astro");
            //星座
            String viplevel = profile.optString("viplevel");
            //会员等级
            String vipscore = profile.optString("vipscore");
            //会员积分
            String vipspeed = profile.optString("vipspeed");
            //会员成长
            String space_desc = profile.optString("space_desc");
            //空间说明
            String vip_keepdays = profile.optString("vip_keepdays");
            //会员持续(天)
            JSONObject count = json.getJSONObject("data").getJSONObject("count");
            String pic_allnum = count.optString("pic_allnum");
            //相册数量
            String shuoshuo_allnum = count.optString("shuoshuo_allnum");
            //说说数量
            JSONObject visit = json.getJSONObject("data").getJSONObject("visit");
            String todaynum = visit.optString("todaynum");
            String totalnum = visit.optString("totalnum");
            return msg + "空间:" + qzonename + "\n今日访客:" + todaynum + "人次" + "\n" + "总访客量:" + totalnum + "人次";
        }
        else
        {
            return "获取失败";
        }
    }
    catch(e)
    {
        return "获取失败" + e;
    }
}
public String getVipInfo1(String uin)
{
    try
    {
        String pskey = getPskey("vip.qq.com");
        String cookie = "uin=o" + myUin + "; skey=" + skey + "; p_uin=o" + myUin + "; p_skey=" + pskey;
        String url = httpget("https://club.vip.qq.com/api/vip/getQQLevelInfo?g_tk=" + GetGTK(pskey) + "&requestBody={\"sClientIp\":\"\",\"sSessionKey\":\"" + skey + "\",\"iKeyType\":1,\"iAppId\":0,\"iUin\":\"" + uin + "\"}", cookie);
        JSONObject json = new JSONObject(url);
        if(json.getInt("ret") == 0)
        {
            String data2 = json.getString("data");
            JSONObject data3 = new JSONObject(data2);
            String mRes = data3.getString("mRes");
            JSONObject data1 = new JSONObject(mRes);
            String sNickName = data1.get("sNickName");
            String iPCQQOnlineTime = data1.get("iPCQQOnlineTime");
            String iPCQQOnline = data1.get("iPCQQOnline");
            String iMaxLvlTotalDays = data1.get("iMaxLvlTotalDays");
            String iNextLevelDay = data1.get("iNextLevelDay");
            String iBigClubGrowth = data1.get("iBigClubGrowth");
            String iQQLevel = data1.get("iQQLevel");
            String iMobileQQOnline = data1.get("iMobileQQOnline");
            String iMobileQQOnlineTime = data1.get("iMobileQQOnlineTime");
            String iTotalActiveDay = data1.get("iTotalActiveDay");
            return "QQ:" + uin + "\n昵称:" + sNickName + "\nQQ等级:" + iQQLevel + "\n最大日可活跃天数:" + iMaxLvlTotalDays + "\n升下一级需:" + iNextLevelDay + "天\n大会员成长值:" + iBigClubGrowth + "\n手机在线:" + iMobileQQOnline.replace("0", "否").replace("1", "是") + "(共" + iMobileQQOnlineTime + "小时)\n电脑在线:" + iPCQQOnline.replace("0", "否").replace("1", "是") + "(共" + iPCQQOnlineTime + "小时)\n总活跃天数:" + iTotalActiveDay;
        }
        else
        {
            return "";
        }
    }
    catch(e)
    {
        return "" + e;
    }
}
public String getVipInfo3(String uin)
{
    try
    {
        String pskey = getPskey("vip.qq.com");
        String cookie = "uin=o" + myUin + "; skey=" + skey + "; p_uin=o" + myUin + "; p_skey=" + pskey;
        String url = httppost1("https://h5.vip.qq.com/proxy/domain/club.vip.qq.com/api/aggregation?from=hippy-vipAggregation&g_tk=" + GetGTK(pskey), cookie, "{\"commonInfo__getQQLevelInfo\":{\"args\":[\"" + uin + "\"],\"needCtx\":false}}");
        JSONObject json = new JSONObject(url);
        if(json.getInt("code") == 0)
        {
            String data1 = json.getString("data");
            JSONObject json1 = new JSONObject(data1);
            String json2 = json1.getString("commonInfo__getQQLevelInfo");
            JSONObject data3 = new JSONObject(json2);
            String iAddFriend = data3.get("iAddFriend").replace("0", "未完成").replace("1", "已完成");
            //新增好友
            String iDailySign = data3.get("iDailySign").replace("0", "未完成").replace("1", "已完成");
            //今日打卡
            String WeishiVideoview = data3.get("WeishiVideoview").replace("0", "未完成").replace("1", "已完成");
            //微视加速
            String iVipSpeedRate = data3.get("iVipSpeedRate");
            //会员成长速率
            String iMobileGameOnline = data3.get("iMobileGameOnline").replace("0", "未完成").replace("1", "已完成");
            //手游在线
            String iTotalDays = data3.get("iTotalDays");
            //今日成长
            String iPCSafeOnline = data3.get("iPCSafeOnline").replace("0", "未完成").replace("1", "已完成");
            //电脑管家在线
            String iContinueLogin = data3.get("iContinueLogin").replace("0", "未完成").replace("1", "已完成");
            //持续登陆
            String QzoneVisitor = data3.get("QzoneVisitor");
            //空间访客
            String iBigClubGrowth = data3.get("iBigClubGrowth");
            //大会员成长值
            String iBigClubLevel = data3.get("iBigClubLevel");
            //大会员等级
            String iVip = data3.get("iVip").replace("0", "否").replace("1", "是");
            //是否为VIP
            String iSVip = data3.get("iSVip").replace("0", "否").replace("1", "是");
            //是否为SVIP
            String iYearVip = data3.get("iYearVip").replace("0", "否").replace("1", "是");
            //是否为年VIP
            String iBigClubVipFlag = data3.get("iBigClubVipFlag").replace("0", "否").replace("1", "是");
            //是否有大会员
            return "新增好友:" + iAddFriend + "\n日签打卡:" + iDailySign + "\n微视加速:" + WeishiVideoview + "\n手游在线:" + iMobileGameOnline + "\n电脑管家在线:" + iPCSafeOnline + "\n连续登录:" + iContinueLogin + "\n会员速度:" + iVipSpeedRate + "\n今日成长:" + iTotalDays + "\n空间访客:" + QzoneVisitor + "\n是否有会员:" + iVip + "\n是否有年会" + iYearVip + "\n是否有超会:" + iSVip + "\n是否有大会:" + iBigClubVipFlag + "\n大会成长值:" + iBigClubGrowth + "\n大会等级:" + iBigClubLevel;
        }
    }
    catch(e)
    {
        return "" + e;
    }
}
public String getUserInfo2(String uin)
{
    try
    {
        String pskey = getPskey("qzone.qq.com");
        String cookie = "uin=o" + myUin + "; skey=" + skey + "; p_uin=o" + myUin + "; p_skey=" + pskey;
        String url = httpget("https://h5.qzone.qq.com/webapp/json/vpageCover_v2/getMainPage?g_tk=" + GetGTK(pskey) + "&uin=" + uin + "&visituin=" + myUin + "&force=1&format=json", cookie);
        JSONObject json = new JSONObject(url);
        if(json.getInt("ret") == 0)
        {
            String msg = json.get("msg");
            if(!msg.equals("")) msg = msg + "\n";
            else msg = msg;
            JSONObject profile = json.getJSONObject("data").getJSONObject("profile");
            String nickname = profile.optString("nickname");
            //昵称
            String qzonename = profile.optString("qzonename");
            //空间名称
            String gender = profile.optString("gender").replace("1", "男").replace("2", "女").replace("0", "未知");
            //性别
            String astro = profile.optString("astro");
            //星座
            String viplevel = profile.optString("viplevel");
            //会员等级
            String vipscore = profile.optString("vipscore");
            //会员积分
            String vipspeed = profile.optString("vipspeed");
            //会员成长
            String space_desc = profile.optString("space_desc");
            //空间说明
            String vip_keepdays = profile.optString("vip_keepdays");
            //会员持续(天)
            JSONObject count = json.getJSONObject("data").getJSONObject("count");
            String pic_allnum = count.optString("pic_allnum");
            //相册数量
            String shuoshuo_allnum = count.optString("shuoshuo_allnum");
            //说说数量
            JSONObject visit = json.getJSONObject("data").getJSONObject("visit");
            String todaynum = visit.optString("todaynum");
            String totalnum = visit.optString("totalnum");
            return msg + "空间:" + qzonename + "\n" + "星座:" + astro + "\n" + "空间说明:" + space_desc + "\n" + "会员持续:" + vip_keepdays + "天" + "\n" + "相册数量:" + pic_allnum + "\n" + "说说数量:" + shuoshuo_allnum + "\n" + "今日访客:" + todaynum + "人次" + "\n" + "总访客量:" + totalnum + "人次";
        }
        else
        {
            return "获取失败";
        }
    }
    catch(e)
    {
        return "获取失败" + e;
    }
}