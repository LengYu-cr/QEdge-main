String mQQ = myUin;
//管家发送来自陌然
public String sendGuanjia(String qun, String qq, String skey, String pskey, String rr, String xx)
{
    String WelcomeMsg = getQuestionsList(qun);
    if(WelcomeMsg.equals("群" + qun + "暂未设置自助问答")) c = "";
    else c = 删除问答(qun, qq, skey, pskey, "1");
    String token = 读(ColdRainPath + "data/" + qun + "管家token.txt");
    if((token + "").equals(""))
    {
        String a1 = 添加问答(qun, qq, skey, pskey, rr, xx);
        if((a1 + "").equals("你不是管理"))
        {
            return "非管理无法操作";
        }
        if((a1 + "").equals("访问频率过快，稍后再试"))
        {
            return "兄弟，车速太快了！！！";
        }
        sendMsg(qun, "Come on![atUin=2854196310]", 2);
        写(ColdRainPath + "data/" + qq + "管家问题.txt", rr);
        return "成功";
    }
    String a = 添加问答(qun, qq, skey, pskey, rr, xx);
    if((a + "").startsWith("添加失败"))
    {
        return a;
    }
    if((a + "").equals("访问频率过快，稍后再试"))
    {
        return "兄弟，车速太快了！！！";
    }
    if((a + "").equals("你不是管理"))
    {
        return "非管理无法操作";
    }
    String b = 触发问答(qun, qq, skey, pskey, rr, token);
    if((b + "").equals("访问频率过快，稍后再试"))
    {
        return "兄弟，车速太快了！！！";
    }
    if((b + "").equals("会话过期"))
    {
        sendMsg(qun, "Come on![atUin=2854196310]", 2);
        写(ColdRainPath + "data/" + qq + "管家问题.txt", rr);
        return "成功";
    }
    String c = 删除问答(qun, qq, skey, pskey, "1");
    String c = 删除问答(qun, qq, skey, pskey, "2");
    return "成功";
}
public String 邀请移除管家(String qun, String qq, String skey, String pskey, int type)
{
    try
    {
        if(type == 0) aa = "robots_close";
        else if(type == 1) aa = "robots_set";
        String cookie = "p_uin=o0" + qq + ";uin=o0" + qq + ";skey=" + skey + ";p_skey=" + pskey;
        String put = "gc=" + qun + "&robot_uin=2854196310";
        JSONObject json = new JSONObject(httppost("https://web.qun.qq.com/qunrobot/proxy/domain/qun.qq.com/cgi-bin/qunapp/" + aa + "?bkn=" + GetGTK(skey), cookie, put, "application/x-www-form-urlencoded"));
        int retcode = json.get("retcode");
        String retmsg = json.get("msg");
        if(retcode == 0) return "成功";
        else if(retcode == 10013) return "你不是管理";
        else if(retcode == 10025) return "Q群管家已经添加了！";
        else if(retcode == 10010) return "Q群管家已经移除了！";
        else return "添加失败，原因:\n" + msg;
    }
    catch(Exception e)
    {
        return "添加失败，原因:\n" + e;
    }
}
public String 管家设置(String qun, String qq, String skey, String pskey, String type, String status)
{
    try
    {
        String cookie = "p_uin=o0" + qq + ";uin=o0" + qq + ";skey=" + skey + ";p_skey=" + pskey;
        String put = "word_type=" + type + "&switch_status=" + status + "&group_code=" + qun + "&bkn=" + GetGTK(skey);
        JSONObject json = new JSONObject(httppost("https://web.qun.qq.com/qunrobot/proxy/domain/app.qun.qq.com/cgi-bin/guanjia_robot/chat_manage/set_switch?bkn=" + GetGTK(skey), cookie, put, "application/x-www-form-urlencoded"));
        int retcode = json.get("retcode");
        String retmsg = json.get("retmsg");
        if(retcode == 0) return "成功";
        else if(retcode == 11002) return "你不是管理";
        else if(retcode == 21000) return "Q群管家不存在";
        else return "添加失败，原因:\n" + retmsg;
    }
    catch(Exception e)
    {
        return "添加失败，原因:\n" + e;
    }
}
public String 触发问答(String qun, String qq, String skey, String pskey, String question, String token)
{
    try
    {
        String cookie = "p_uin=o0" + qq + ";uin=o0" + qq + ";skey=" + skey + ";p_skey=" + pskey;
        String put = "{ \"anonymous\": 1, \"question\": \"" + question + "\", \"token\": \"" + token + "\" }";
        JSONObject json = new JSONObject(httppost1("https://app.qun.qq.com/cgi-bin/guanjia_robot/qna_callback/get_answer?bkn=" + GetGTK(skey), cookie, put));
        int ec = json.get("ec");
        String em = json.get("em");
        if(ec == 0) return "成功";
        else if(ec == 70000) return "会话过期";
        else if(ec == 70003) return "访问频率过快，稍后再试";
        else return "触发失败，原因:\n" + em;
    }
    catch(Exception e)
    {
        return "触发失败，原因:\n" + e;
    }
}
public String 添加问答(String qun, String qq, String skey, String pskey, String question, String answer)
{
    try
    {
        String cookie = "p_uin=o0" + qq + ";uin=o0" + qq + ";skey=" + skey + ";p_skey=" + pskey;
        String put = "{\"bkn\":" + GetGTK(skey) + ",\"group_id\":" + qun + ",\"qna_item\":{\"slot\":0,\"question\":\"" + question + "\",\"answer\":\"" + answer + "\",\"keyword\":[\"" + question + "\"]}}";
        JSONObject json = new JSONObject(httppost("https://web.qun.qq.com/qunrobot/proxy/domain/app.qun.qq.com/cgi-bin/guanjia_robot/qna_setting/set_qna?bkn=" + GetGTK(skey), cookie, put, "application/json"));
        int retcode = json.get("retcode");
        String msg = json.get("msg");
        if(retcode == 0) return "成功";
        else if(retcode == 100106) return "失败，原因:\n问答已存在";
        else if(retcode == 100302) return "你不是管理";
        else if(retcode == 1009) return "访问频率过快，稍后再试";
        else return "添加失败，原因:\n" + msg;
    }
    catch(Exception e)
    {
        return "添加失败，原因:\n" + e;
    }
}
public String 删除问答(String qun, String qq, String skey, String pskey, String id)
{
    try
    {
        String cookie = "p_uin=o0" + qq + ";uin=o0" + qq + ";skey=" + skey + ";p_skey=" + pskey;
        String put = "{\"bkn\":" + GetGTK(skey) + ",\"group_id\":" + qun + ",\"qna_item\":{\"slot\":" + id + ",\"question\":\"\",\"answer\":\"\",\"keyword\":[]}}";
        JSONObject json = new JSONObject(httppost("https://web.qun.qq.com/qunrobot/proxy/domain/app.qun.qq.com/cgi-bin/guanjia_robot/qna_setting/set_qna?bkn=" + GetGTK(skey), cookie, put, "application/json"));
        int retcode = json.get("retcode");
        String msg = json.get("msg");
        if(retcode == 0) return "成功";
        else if(retcode == 100405) return "失败，原因:\n问答不存在";
        else if(retcode == 100302) return "你不是管理";
        else return "删除失败，原因:\n" + msg;
    }
    catch(Exception e)
    {
        return "删除失败，原因:\n" + e;
    }
}
public String getUserNickName(String uin)
{
    try {
        String qzone = getPskey("qzone.qq.com");
        long gtk = GetGTK(qzone);
        String cookie = "p_uin=o0" + myUin + ";skey=" + skey + ";p_skey=" + qzone;
        String url = "https://r.qzone.qq.com/cgi-bin/user/cgi_personal_card?uin=" + uin + "&remark=0&g_tk=" + gtk;
        String nm = httpget(url, cookie);
        String nv = nm.replace("_Callback(", "");
        String nan = nv.replace(");", "");
        String boy = nan.replaceAll("\n", "");
        JSONObject json1 = new JSONObject(boy);
        String nickname = json1.get("nickname");
        return nickname;
    }
    catch(e) {
        return uin;
    }
}
public String setAdmin(String qun, String uin, int type)
{
    try
    {
        String cookie = "p_uin=o" + myUin + ";uin=o" + myUin + ";skey=" + skey + ";p_skey=" + qunpskey;
        String put = "gc=" + qun + "&ul=" + uin + "&op=" + type + "&bkn=" + GetGTK(skey);
        JSONObject json = new JSONObject(httppost("https://qun.qq.com/cgi-bin/qun_mgr/set_group_admin?bkn=" + GetGTK(skey) + "&ts=" + System.currentTimeMillis(), cookie, put, "application/x-www-form-urlencoded"));
        int ec = json.get("ec");
        String em = json.get("em");
        if(ec == 0) return "设置成功";
        else if(ec == 13) return "设置失败，管理位置已满";
        else return "设置失败，原因:\n" + em;
    }
    catch(Exception e)
    {
        return "设置失败，原因:\n" + e;
    }
}
public String getAiMsg(String text)
{
    String dataJson = "{\"prompt\":\"" + u加(text) + "\",\"parentMessageId\":\"\"}";
    String url = jm("aHR0cHM6Ly9hcGkuamVldmVzLmFpL2dlbmVyYXRlL3Y0L2NoYXQ=");
    String result = httppost1(url, "", dataJson);
    if(result.equals("访问网页失败"))
    {
        return "出现错误:" + result;
    }
    else
    {
        if(result.contains("finalText"))
        {
            int index = result.lastIndexOf("finalText\":\"");
            String msg = result.substring(index + 12);
            int rd = msg.indexOf("\"}");
            String re = msg.substring(0, rd);
            return re.replace("\\n","\n");
        }
        else
        {
            return "出现未知错误(可能是请求次数过多)";
        }
    }
}
public void sendGPT(Object Yu)
{
    Object data = getData();
    data.put(Yu,""+Module);
    String quntext = "";
    String qun = data.qun;
    String uin = data.uin;
    String qq=myUin;
    int mtype=data.mtype;
    long msgid=data.msgid;
    for(Object msg:data.originMsg.elements) {
        if(msg.textElement!=null) {
            if(msg.textElement.atType==0) {
                quntext+=msg.textElement.content;
            }
        }
        else if(msg.picElement!=null) {
            quntext+="[图片]"+msg.picElement.summary;
        }
    }
    if(quntext.equals("")) quntext="你好呀";
    String menu = getAiMsg(quntext);
    sendText(data, menu);
}
public String getGroupOwner(String qun)
{
    if(Module.equals("Serendipity") || Module.equals("模了个块")) {
        for(HashMap info: getGroupList())
        {
            String ownernm = info.get("groupOwner");
            String qr = info.get("group");
            if(qr.equals(qun))
            {
                String list = ownernm + "";
                return list;
                break;
            }
        }
    }
    else if(Module.equals("QStory")) {
        Object st=getGroupList();
        for(Object b:st)
        {
            String qr=b.GroupUin;
            String ownernm = b.GroupOwner;
            if(qr.equals(qun))
            {
                String list = ownernm + "";
                return list;
                break;
            }
        }
    }else if(Module.equals("QFun")) {
        Object st=getGroupList();
        for(Object b:st)
        {
            String qr=b.group;
            String ownernm = b.groupOwner;
            if(qr.equals(qun))
            {
                String list = ownernm + "";
                return list;
                break;
            }
        }
    }
}
public String getGroupNames(String qun)
{
    TroopInfo info=findTroopInfo(qun);
    return info.troopname;
}
public String getQuestionsList(String qun)
{
    String result = "";
    String cookie = "uin=o" + myUin + "; p_uin=o" + myUin + "; p_skey=" + qunpskey + "; skey=" + skey;
    String url = httpget("https://web.qun.qq.com/qunrobot/questionsetting?gc=" + qun + "&uin=2854196310&f_id=100", cookie);
    int index = url.lastIndexOf("window.__INITIAL_STATE__=");
    String text = url.substring(index + 25);
    int rd = text.indexOf("}<");
    String re = text.substring(0, rd + 2);
    if(re.contains("\"questions\":[],"))
    {
        return "群" + qun + "暂未设置自助问答";
    }
    else
    {
        JSONObject json = new JSONObject(re);
        JSONArray questions = json.getJSONArray("questions");
        for(int q = 0;
        q < questions.length();
        q++)
        {
            JSONObject List = questions.get(q);
            String question = List.get("question");
            String answer = List.get("answer");
            String edit_uin = List.get("edit_uin");
            result += "问题:" + question + "\n回答:" + answer + "\n添加人:" + edit_uin + "\n";
        }
        return "Q群管家自助问答列表:\n\n" + result + "";
    }
}
public boolean isMusicLogin(String cookie) {
    String url=httpget("https://c.y.qq.com/portalcgi/fcgi-bin/music_mini_portal/fcg_vip_score_rank_new.fcg?format=json&outCharset=utf-8&cmd=1",cookie);
    JSONObject json=new JSONObject(url);
    int code=json.getInt("code");
    String message=json.get("message");
    if(code==0&&message.equals("")) {
        return true;
    }
    else {
        return false;
    }
}
String SECRET = "lengyu520";
public String GetUinClient()
{
    String client;
    TicketManager manager = app.getManager(2);
    client=manager.getStweb(myUin);
    if(client==null||client.equals("")) return null;
    else return client;
}
public String accountlogin() {
    try {
        String client=GetUinClient();
        if(client==null||client.equals("null")||client.equals("")) {
        }
        String url=getLocation("https://ssl.ptlogin2.qq.com/jump?u1=https%3A%2F%2Fconnect.qq.com&daid=383&pt_openlogin_data=s_url%3Dhttps%253A%252F%252Fconnect.qq.com%26refer_cgi%3Dauthorize%26response_type%3Dcode%26client_id%3D100497308%26redirect_uri%3Dhttps%253A%252F%252Fy.qq.com%252Fportal%252Fwx_redirect.html&keyindex=19&clientuin="+myUin+"&clientkey="+client);
        url=url.substring(url.indexOf("=")+1);
        String dataJson="{\"comm\":{\"platform\":\"yqq\",\"ct\":24,\"cv\":0},\"req\":{\"module\":\"QQConnectLogin.LoginServer\",\"method\":\"QQLogin\",\"param\":{\"code\":\""+url+"\"}}}";
        String result=httppost1("https://u.y.qq.com/cgi-bin/musicu.fcg","",dataJson);
        JSONObject json=new JSONObject(result);
        json=json.getJSONObject("req").getJSONObject("data");
        String uin=json.optString("str_musicid");
        String musickey=json.optString("musickey");
        return "uin="+uin+";qm_keyst="+musickey+";qqmusic_key="+musickey;
    }
    catch(e)
    {
        return "uin="+myUin+";qm_keyst=null;";
    }
    return "uin="+myUin+";qm_keyst=null;";
}