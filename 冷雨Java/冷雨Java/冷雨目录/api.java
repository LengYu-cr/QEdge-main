public int dpToPx(Context context, int dp) {
    float density = context.getResources().getDisplayMetrics().density;
    return Math.round(dp * density);
}
public void sendTroopMarkDown(String qun, String content, int mtype) {
    String qr="";
    if(mtype==1) qr=getUidFromUin(qun);
    else if(mtype==2) qr=qun;
    try {
        Contact contact=new Contact(mtype,qr,"");
        MarkdownElement markdownElement=new MarkdownElement();
        markdownElement.content=content;
        MsgElement msgElement= new MsgElement();
        msgElement.markdownElement=markdownElement;
        msgElement.elementType=14;
        ArrayList msgList=new ArrayList();
        msgList.add(msgElement);
        ((IMsgService) QRoute.api(IMsgService.class)).sendMsg(contact,msgList,null);
    }
    catch(e) {
        sendMsg(qun,""+e,mtype);
    }
}
public void sendMsg(qun,ArrayList msgList,mtype) {
    //发送内容
    Contact contact=new Contact(mtype,qun,"");
    ((IMsgService) QRoute.api(IMsgService.class)).sendMsg(contact,msgList,null);
}
public void sendTroopFace(String qun, int faceIndex, String stickerId, int mtype)
{
    //发送表情
    String qr="";
    if(mtype==1) qr=getUidFromUin(qun);
    else if(mtype==2) qr=qun;
    Contact contact = new Contact(mtype, qr, "");
    MsgElement msgElement = QRoute.api(IMsgUtilApi.class).createFaceElement(1, 2, "");
    String summary = "龙";
    FaceElement faceElement = msgElement.getFaceElement();
    faceElement.faceType = 3;
    faceElement.faceIndex = faceIndex;
    faceElement.stickerType = 3;
    faceElement.stickerId = stickerId;
    faceElement.faceText = summary;
    faceElement.surpriseId = "100";
    faceElement.packId = "40";
    faceElement.chainCount = 999;
    faceElement.randomType = 999;
    msgElement.elementType=6;
    ArrayList MsgElementList = new ArrayList();
    MsgElementList.add(msgElement);
    ((IMsgService) QRoute.api(IMsgService.class)).sendMsg(contact, MsgElementList, null);
}
public void sendTroopPoke(String qun, int pokeType, int vaspokeId, int mtype)
{
    //发送戳一戳
    String qr="";
    if(mtype==1) qr=getUidFromUin(qun);
    else if(mtype==2) qr=qun;
    Contact contact = new Contact(mtype, qr, "");
    String summary = "戳你吗";
    FaceElement faceElement = new FaceElement();
    faceElement.faceType = 5;
    faceElement.faceIndex = 0;
    faceElement.pokeType = pokeType;
    faceElement.oldVersionStr = "[戳一戳消息]请升级原神查看";
    faceElement.spokeSummary = summary;
    faceElement.faceText = summary;
    faceElement.setVaspokeName(summary);
    faceElement.vaspokeMinver = "7.2.0";
    faceElement.pokeFlag = 0;
    faceElement.pokeStrength = 3;
    faceElement.vaspokeId = vaspokeId;
    faceElement.doubleHit = 1;
    MsgElement msgElement = new MsgElement();
    msgElement.setFaceElement(faceElement);
    msgElement.elementType=6;
    ArrayList MsgElementList = new ArrayList();
    MsgElementList.add(msgElement);
    ((IMsgService) QRoute.api(IMsgService.class)).sendMsg(contact, MsgElementList, null);
}
public void sendTroopBullet(String qun, int faceType, int faceCount, int mtype)
{
    //发送弹
    String qr="";
    if(mtype==1) qr=getUidFromUin(qun);
    else if(mtype==2) qr=qun;
    Contact contact = new Contact(mtype, qr, "");
    String summary = "弹你吗";
    FaceBubbleElement faceElement = new FaceBubbleElement();
    faceElement.faceType = faceType;
    faceElement.faceCount = faceCount;
    faceElement.oldVersionStr = "[弹消息]请升级原神查看";
    faceElement.faceSummary = summary;
    faceElement.content = summary;
    faceElement.faceFlag = 0;
    MsgElement msgElement = new MsgElement();
    msgElement.faceBubbleElement=faceElement;
    msgElement.elementType=27;
    ArrayList MsgElementList = new ArrayList();
    MsgElementList.add(msgElement);
    ((IMsgService) QRoute.api(IMsgService.class)).sendMsg(contact, MsgElementList, null);
}
public void sendTroopBullet(String qun, int faceType, int faceCount, int faceId, int mtype)
{
    //发送黄色表情弹
    String qr="";
    if(mtype==1) qr=getUidFromUin(qun);
    else if(mtype==2) qr=qun;
    Contact contact = new Contact(mtype, qr, "");
    String summary = "弹你吗";
    FaceBubbleElement faceElement = new FaceBubbleElement();
    faceElement.faceType = faceType;
    faceElement.faceCount = faceCount;
    faceElement.oldVersionStr = "[弹消息]请升级原神查看";
    faceElement.faceSummary = summary;
    faceElement.content = summary;
    faceElement.faceFlag = 0;
    SmallYellowFaceInfo yellowFaceInfo = new SmallYellowFaceInfo(faceId,summary,summary,"");
    faceElement.yellowFaceInfo = yellowFaceInfo;
    MsgElement msgElement = new MsgElement();
    msgElement.faceBubbleElement=faceElement;
    msgElement.elementType=27;
    ArrayList MsgElementList = new ArrayList();
    MsgElementList.add(msgElement);
    ((IMsgService) QRoute.api(IMsgService.class)).sendMsg(contact, MsgElementList, null);
}
public void sendTroopPic(String qun, String text, int mtype)
{
    //发送图片
    try {
        String qr="";
        if(mtype==1) qr=getUidFromUin(qun);
        else if(mtype==2) qr=qun;
        File d = new File(ColdRainPath + "下载/随机一言.txt");
        String dd = 取文件(d);
        Contact contact = new Contact(mtype, qr, "");
        MsgElement msgElement = QRoute.api(IMsgUtilApi.class).createPicElement(text, true, 0);
        PicElement picElement = msgElement.getPicElement();
        picElement.summary = dd;
        ArrayList MsgElementList = new ArrayList();
        MsgElementList.add(msgElement);
        ((IMsgService) QRoute.api(IMsgService.class)).sendMsg(contact, MsgElementList, null);
    }
    catch(e) {
        sendPic(qun,text,mtype);
    }
}
public void sendFlashPic(String qun, String text, int mtype)
{
    //发送闪照
    String qr="";
    if(mtype==1) qr=getUidFromUin(qun);
    else if(mtype==2) qr=qun;
    Contact contact = new Contact(mtype, qr, "");
    MsgElement msgElement = QRoute.api(IMsgUtilApi.class).createPicElement(text, true, 0);
    PicElement picElement = msgElement.getPicElement();
    picElement.isFlashPic = true;
    ArrayList MsgElementList = new ArrayList();
    MsgElementList.add(msgElement);
    ((IMsgService) QRoute.api(IMsgService.class)).sendMsg(contact, MsgElementList, null);
}
public void deleteTroopMsg(String qun,ArrayList MsgidList,int mtype) {
    //删除消息
    String qr="";
    if(mtype==1) qr=getUidFromUin(qun);
    else if(mtype==2) qr=qun;
    Contact contact=new Contact(mtype,qr,"");
    ((IMsgService) QRoute.api(IMsgService.class)).setLocalMsgRead(contact,null);
    //已读
    ((IMsgService) QRoute.api(IMsgService.class)).deleteMsg(contact,MsgidList,null);
    //删除消息
}
public void setMsgRead(String qun,int mtype) {
    //设置已读消息
    String qr="";
    if(mtype==1) qr=getUidFromUin(qun);
    else if(mtype==2) qr=qun;
    Contact contact=new Contact(mtype,qr,"");
    QRoute.api(IMsgService.class).setLocalMsgRead(contact,null);
    //已读消息
}
public String getFilesType(String url)
{
    try
    {
        URLConnection connection = new URL(url).openConnection();
        String contentType = connection.getContentType();
        return contentType;
    }
    catch(e)
    {
        return "text";
    }
}
public String getUrlInfo(String url)
{
    try
    {
        URLConnection connection = new URL(url).openConnection();
        String contentType = connection.getContentType();
        long ContentLength = connection.getContentLengthLong();
        String redirectUrl = connection.getHeaderField("Location");
        int responseCode = ((HttpURLConnection) connection).getResponseCode();
        if(redirectUrl == null) redirectUrl = url;
        JSONObject json = new JSONObject();
        json.put("code", responseCode);
        json.put("type", contentType);
        json.put("length", ContentLength);
        json.put("redirectUrl", redirectUrl);
        return json.toString();
    }
    catch(e)
    {
        JSONObject json = new JSONObject();
        json.put("code", -1);
        json.put("msg", "出错啦！");
        return json.toString();
    }
}
public void sendImg(String qun, String url, int type)
{
    try
    {
        if(url.startsWith("http"))
        {
            String stype=读(ColdRainPath+"data/"+qun+"菜单模式.txt");
            if(stype.equals("官机")) {
                String url=httppost1("https://ly.yuafeng.cn/sender.php?image="+URL(url,1)+"&type=image&bot="+myBot+"&qun="+qun,"","");
                JSONObject json = new JSONObject(url);
                if(json.getBoolean("need_at")&&type==2) {
                    sendMsg(qun,"[atUin="+myBotQQ+"]代发"+qun,type);
                }
            }
            else {
                String jb = User(3);
                String file = ColdRainPath + "/图片/" + jb + ".png";
                String sp = DownloadToFile(url, file);
                if(sp.equals("成功"))
                {
                    sendTroopPic(qun, file, type);
                    sc(file);
                }
                else
                {
                    sendMsg(qun, "图片文件已失效或并非图片", type);
                }
            }
        }
        else if(url.startsWith("/"))
        {
            sendTroopPic(qun, url, type);
        }
    }
    catch(e)
    {
        Toast( "错误:\n" + e);
    }
}
public void sendVideo(int mtype,String qun, String url)
{
    try
    {
        if(url == null)
        {
            sendMsg(qun, "视频重定向失败,请检查API接口是否正常",mtype);
        }
        else if(url.startsWith("http"))
        {
            String stype=读(ColdRainPath+"data/"+qun+"菜单模式.txt");
            if(stype.equals("官机")) {
                String url=httppost1("https://ly.yuafeng.cn/sender.php?url="+URL(url,1)+"&type=video&bot="+myBot+"&qun="+qun,"","");
                JSONObject json = new JSONObject(url);
                if(json.getBoolean("need_at")&&mtype==2) {
                    sendMsg(qun,"[atUin="+myBotQQ+"]代发"+qun,mtype);
                }
            }
            else {
                String jb = "视频"+User(3);
                String file = ColdRainPath + "/视频/" + jb + ".mp4";
                String sp = DownloadToFile(url, file);
                if(sp.equals("成功"))
                {
                    sendVideo(qun, file, mtype);
                    sc(file);
                }
                else
                {
                    sendMsg(qun, "视频链接下载" + sp,mtype);
                }
            }
        }
    }
    catch(e)
    {
        Toast( "错误:\n" + e);
    }
}
import com.tencent.util.QQToastUtil;
public void QQToast(int i,String text) {
    QQToastUtil QToastUtil=new QQToastUtil();
    QToastUtil.showQQToastInUiThread(i,text);
}
public String User(int tt)
{
    String base = "wqetryuioplkjhgfdsazxcvbmnABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    StringBuffer sb = new StringBuffer();
    Random rd = new Random();
    for(int i = 0;
    i < tt;
    i++)
    {
        sb.append(base.charAt(rd.nextInt(base.length())));
    }
    return sb.toString();
}
public String DownloadToFile(String url, String filepath) throws Exception
{
    File file = new File(filepath);
    if(!file.getParentFile().exists())
    {
        file.getParentFile().mkdirs();
    }
    InputStream input = null;
    try
    {
        URL ur = new URL(url);
        HttpURLConnection urlConn = (HttpURLConnection) ur.openConnection();
        input = urlConn.getInputStream();
        byte[] bs = new byte[1024];
        int len;
        FileOutputStream out = new FileOutputStream(filepath, false);
        while((len = input.read(bs)) != -1)
        {
            out.write(bs, 0, len);
        }
        out.close();
        input.close();
    }
    catch(IOException e)
    {
        return "失败";
    }
    finally
    {
        try
        {
            input.close();
        }
        catch(IOException e)
        {
            e.printStackTrace();
            return "失败";
        }
    }
    return "成功";
}
public String get(String url)
{
    StringBuffer buffer = new StringBuffer();
    InputStreamReader isr = null;
    try
    {
        URL urlObj = new URL(url);
        URLConnection uc = urlObj.openConnection();
        uc.setConnectTimeout(20000);
        uc.setReadTimeout(20000);
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
        return "访问网页失败，原因:" + e;
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
            return "访问网页失败，原因:" + e;
        }
    }
    if(buffer.length() == 0) return "访问网页失败";
    buffer.delete(buffer.length() - 1, buffer.length());
    return buffer.toString();
}
public String httppost1(String urlPath, String cookie, String data)
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
        uc.setReadTimeout(2000000);
        uc.setRequestMethod("POST");
        uc.setRequestProperty("Content-Type", "application/json");
        uc.setRequestProperty("Cookie", cookie);
        uc.getOutputStream().write(data.getBytes("UTF-8"));
        uc.getOutputStream().flush();
        uc.getOutputStream().close();
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
            Toast( "错误:\n" + e);
        }
    }
    if(buffer.length() == 0) return buffer.toString();
    buffer.delete(buffer.length() - 1, buffer.length());
    return buffer.toString();
}
public String httpget(String url, String cookie)
{
    StringBuffer buffer = new StringBuffer();
    InputStreamReader isr = null;
    try
    {
        URL urlObj = new URL(url);
        URLConnection uc = urlObj.openConnection();
        uc.setRequestProperty("Cookie", cookie);
        uc.setRequestProperty("user-agent", "Mozilla/5.0 (Linux; Android 12; V2055A Build/SP1A.210812.003; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/89.0.4389.72 MQQBrowser/6.2 TBS/046209 Mobile Safari/537.36 V1_AND_SQ_8.9.5_3176_YYB_D A_8090500 QQ/8.9.5.8845 NetType/WIFI WebP/0.3.0 Pixel/1080 StatusBarHeight/85 SimpleUISwitch/0 QQTheme/1000 InMagicWin/0 StudyMode/0 CurrentMode/0 CurrentFontScale/0.87 GlobalDensityScale/0.90000004 AppId/537129734");
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
public void sendText(Object data,String menu) {
    new Thread(new Runnable() {
        public void run() {
            String qun=data.qun;
            String uin=data.uin;
            int mtype=data.mtype;
            if(menu==null||menu.isEmpty()) {
                Toast("发送消息为空");
            }
            else {
                menu=getStandardMsg(data,menu);
                String stype=读(ColdRainPath+"data/"+qun+"菜单模式.txt");
                if(stype.equals("")||stype.equals("文字")) {
                    sendMsg(data.qun,menu,data.mtype);
                }
                else if(stype.equals("图片")) {
                    if(menu.contains("[pic")) {
                        sendMsg(qun,menu,data.mtype);
                    }
                    else {
                        String pic=文字("0","图片模式","图片底图");
                        String textcolor=文字("0","图片模式","字体颜色");
                        String nm=MakeTextPhoto(menu,pic,textcolor,qun);
                        sendImg(qun,nm,data.mtype);
                        sc(nm);
                    }
                }
                else if(stype.equals("回复")) {
                    sendReply(data.ModuleMsg,menu);
                }
                else if(stype.equals("管家")) {
                    if(mtype==2) {
                        String pskey=getPskey("qun.qq.com");
                        String skey=getSkey();
                        String text="\n"+menu.replace("$","").replace("&lt;","<").replace("&gt;",">");
                        if(text.length()>20000||text.contains(".cn")||text.contains(".net")||text.contains(".vip")||text.contains(".com")||text.contains(".中国")||text.contains(".edu")||text.contains(".tv")||text.contains("[pic=")) {
                            sendMsg(qun,menu,data.mtype);
                        }
                        else {
                            String xxx=sendGuanjia(qun,myUin,skey,pskey,User(3),text.replaceAll("\\r\\n|\\n|\\r", "\\\\n").replace("\"","\\\""));
写(qun,"管家触发","触发人",myUin);
if(!xxx.equals("成功")){
sendMsg(qun,menu,data.mtype);
Toast("出现错误:"+xxx);
}
}
}
}else if(stype.equals("MarkDown")){
String[] texts=menu.split("\n");
String result="";
for(String info:texts){
if(info.contains("[pic")){
result+="\n[图片消息，点我查看] (mqqapi://openhalfscreenweb/?height=1920&url="+URL(info.replace("[pic=","").replace("]",""),1)+")";
                        }
                        else {
                            result+="\n["+info+"] (mqqapi://aio/inlinecmd?command="+URL(info,1)+"&enter=false&reply=false)";
                        }
                    }
                    sendTroopMarkDown(qun,result,mtype);
                }
                else if(stype.equals("卡片")) {
                    //String text6=文字("0","卡片外显","内容");
                    File d=new File(ColdRainPath+"下载/随机一言.txt");
                    String dd=取文件(d);
                    //if(text6.equals("")) text7=dd;else text7=text6;
                    //int ca=读("0","卡片模式","卡片");
                    String Card="{\"app\":\"com.tencent.bot.task.deblock\",\"desc\":\"\",\"bizsrc\":\"\",\"view\":\"index\",\"ver\":\"2.0.4.0\",\"prompt\":\"冷雨Java\",\"appID\":\"\",\"sourceName\":\"\",\"actionData\":\"\",\"actionData_A\":\"\",\"sourceUrl\":\"\",\"meta\":{\"detail\":{\"appID\":\"\",\"botName\":\"冷雨Java\",\"cmdTitle\":\""+dd+"\",\"content\":\""+menu.replace("\"","")+"\",\"guildID\":\"\",\"iconLeft\":[{\"num\":\"10\"}],\"iconRight\":[],\"receiverName\":\"\"}},\"config\":{\"autosize\":1,\"ctime\":1661659096,\"token\":\"卡片模式仅自己可见\"},\"text\":\"\",\"extraApps\":[],\"sourceAd\":\"\",\"extra\":\"\"}";
                    sendCard(qun,Card,mtype);
                }
                else if(stype.equals("官机")) {
                    if(mtype==2) {
                        String url=httppost1("https://ly.yuafeng.cn/sender.php?text="+URL(menu,1)+"&type=text&bot="+myBot+"&qun="+qun,"","");
                JSONObject json = new JSONObject(url);
                if(json.getBoolean("need_at")) {
                    sendMsg(qun,"[atUin="+myBotQQ+"]代发"+qun,mtype);
                }
                }
                }
                else if(stype.equals("转发")) {
                    //{ "zf": [ "%e4%bd%a0%e5%a5%bd","%e4%bd%a0%e5%a5%bd"  ], "ms": "你好啊", "yh": [ "繁华落幕", "25632286" ], "type": "text", "config":{ "source":"你干嘛呢", "waixian":"你好", "summary":"我不好", "qpid":2100348 } }
                    JSONObject json = new JSONObject();
                    JSONArray zf = new JSONArray();
                    JSONArray yh = new JSONArray();
                    zf.put(data.quntext);
                    zf.put(menu);
                    yh.put(data.uin);
                    yh.put(myUin);
                    json.put("zf",zf);
                    json.put("yh",yh);
                    json.put("ms","冷雨Java");
                    json.put("type","text");
                    JSONObject config = new JSONObject();
                    config.put("source","聊天记录");
                    config.put("waixian","[聊天记录]点击查看");
                    config.put("summary","冷雨Java");
                    config.put("qpid",2079307);
                    json.put("config",config);
                    String jsonString = httppost1("https://api.s01s.cn/API/lt_zf/","",json.toString());
                    if(jsonString.isEmpty()||jsonString.equals("访问网页失败")) {
                        Toast("请求转发聊天记录api出错");
                        sendMsg(qun,menu,mtype);
                    }
                    else {
                        sendCard(qun,jsonString,mtype);
                    }
                }
            }
        }
    }
    ).start();
}
public String getStandardMsg(Object data,String text) {
    if(!text.equals(""))
    {
        String msg = text.replace("[at]", "[atUin=" + data.uin + "]");
        msg = msg.replace("[qq]", myUin);
        msg = msg.replace("[uin]", data.uin);
        msg = msg.replace("[qun]", data.qun);
        if(msg.contains("[Name]"))msg = msg.replace("[Name]", getUserName(data.uin));
        if(msg.contains("[GroupName]"))msg = msg.replace("[GroupName]", getGroupNames(data.qun));
        if(msg.contains("[time]"))msg = msg.replace("[time]", timestampToDate(System.currentTimeMillis()));
        if(msg.contains("[GroupMemberCount]"))msg = msg.replace("[GroupMemberCount]", getGroupMemberList(data.qun).size()+"");
        if(msg.contains("[图片"))msg = msg.replace("[图片", "\n[pic=");
        if(msg.contains("[一言]")) {
            File d = new File(ColdRainPath + "下载/随机一言.txt");
            msg.replace("[一言]", 取文件(d));
        }
        return msg;
    }
    else {
        return "";
    }
}
public static String timestampToDate(long timestamp)
{
    String timestampStr = String.valueOf(timestamp);
    long timestampMs = timestamp;
    
    if (timestampStr.length() == 10) {
        timestampMs = timestamp * 1000;
    } else if (timestampStr.length() == 13) {
        timestampMs = timestamp;
    } else {
        throw new IllegalArgumentException("不支持的时间戳长度：" + timestampStr.length() + "位");
    }
    
    Date date = new Date(timestampMs);
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    String dateString = sdf.format(date);
    return dateString;
}
public static long dateToTimestamp(String date)
{
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    Date d = sdf.parse(date);
    long timestamp = d.getTime();
    return timestamp;
}
public int 判断群(String qun,int type)
{
    if(type==2) {
        String qunList = Arrays.asList(QUN) + "";
        String qqList = Arrays.asList(owner) + "";
        if(qqList.contains(myUin)||getAuthority(qun,myUin).equals("群主")||getAuthority(qun,myUin).equals("管理员"))
        {
            return 1;
        }
        else
        {
            if(qunList.contains(qun))
            {
                return 0;
            }
            else
            {
                return 1;
            }
        }
    }
    else if(type==1) {
        return 1;
    }
    else {
        return 0;
    }
}
import com.tencent.mobileqq.troop.api.ITroopInfoService;
public String getAuthority(String qun, String uin)
{
    Object app = BaseApplicationImpl.getApplication().getRuntime();
    ITroopInfoService TroopInfo = app.getRuntimeService(ITroopInfoService.class);
    Object info = TroopInfo.getTroopInfo(qun);
    if(info.isTroopOwner(uin)) return "群主";
    else if(info.isTroopAdmin(uin)) return "管理员";
    else return "群员";
}
public boolean JudgeMyPermissions(String qun)
{
    if(getAuthority(qun, myUin).equals("管理员") || getAuthority(qun, myUin).equals("群主"))
    {
        return true;
    }
    else
    {
        return false;
    }
}
public boolean getAuthority(String qun,String uin,String uin2) {
    String a=getAuthority(qun,uin);
    String b=getAuthority(qun,uin2);
    if(a.equals("群主")) {
        if(uin.equals(uin2)) {
            return false;
        }
        else {
            return true;
        }
    }
    else if(a.equals("管理员")) {
        if(b.equals("群主")||b.equals("管理员")) {
            return false;
        }
        else {
            return true;
        }
    }
    else {
        return false;
    }
}
public boolean getAuthority2(String qun,String uin,String uin2) {
    String a=getAuthority(qun,uin);
    String b=getAuthority(qun,uin2);
    if(uin.equals(uin2)) {
        return true;
    }
    else {
        if(a.equals("群主")) {
            if(uin.equals(uin2)) {
                return false;
            }
            else {
                return true;
            }
        }
        else if(a.equals("管理员")) {
            if(b.equals("群主")||b.equals("管理员")) {
                return false;
            }
            else {
                return true;
            }
        }
        else {
            return false;
        }
    }
}
public String encryptKaiser(String orignal, String str, int type)
{
    long key = Long.parseLong(str);
    char[] chars = orignal.toCharArray();
    StringBuilder sb = new StringBuilder();
    for(char aChar: chars)
    {
        long asciiCode = aChar;
        if(type == 1) asciiCode += key;
        if(type == 0) asciiCode -= key;
        char result = (char) asciiCode;
        sb.append(result);
    }
    return sb.toString();
}
public String jm(String text)
{
    try
    {
        byte[] decode = Base64.getDecoder().decode(text);
        String str = new String(decode);
        return str;
    }
    catch(Exception e)
    {
        return text;
    }
}
public String jam(String text)
{
    try
    {
        String data = text;
        byte[] bytes = data.getBytes();
        String encode = Base64.getEncoder().encodeToString(bytes);
        return encode;
    }
    catch(Exception e)
    {
        return text;
    }
}
public static String u解(String unicode)
{
    StringBuffer string = new StringBuffer();
    String[] hex = unicode.split("\\\\u");
    for(int i = 0;
    i < hex.length;
    i++)
    {
        try
        {
            if(hex[i].length() >= 4)
            {
                String chinese = hex[i].substring(0, 4);
                try
                {
                    int chr = Integer.parseInt(chinese, 16);
                    boolean isChinese = isChinese((char) chr);
                    string.append((char) chr);
                    String behindString = hex[i].substring(4);
                    string.append(behindString);
                }
                catch(NumberFormatException e1)
                {
                    string.append(hex[i]);
                }
            }
            else
            {
                string.append(hex[i]);
            }
        }
        catch(NumberFormatException e)
        {
            string.append(hex[i]);
        }
    }
    return string.toString();
}
public static String u加(String str)
{
    String r = "";
    for(int i = 0;
    i < str.length();
    i++)
    {
        int chr1 = (char) str.charAt(i);
        String x = "" + Integer.toHexString(chr1);
        if(x.length() == 1) r += "\\u000" + x;
        if(x.length() == 2) r += "\\u00" + x;
        if(x.length() == 3) r += "\\u0" + x;
        if(x.length() == 4) r += "\\u" + x;
    }
    return r;
}
public static boolean isChinese(char c)
{
    Character.UnicodeBlock ub = Character.UnicodeBlock.of(c);
    if(ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS || ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A || ub == Character.UnicodeBlock.GENERAL_PUNCTUATION || ub == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION || ub == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS)
    {
        return true;
    }
    return false;
}
public static int 随机数(int min, int max)
{
    Random random = new Random();
    return random.nextInt((max - min) + 1) + min;
}
String[] QUN= {
    "641486099","948759593","903976754","947217535","922192315","745669460","255413638","954720846","808445801","325031618"
};
// 这些群里不让开启本Java
addItem("开/关机", "lengyu520");
String RootPath2 = ColdRainPath + "头像/";
//无心
public boolean uploadAvatar(String path)
{
    ITransFileController control = BaseApplicationImpl.getApplication().getRuntime().getRuntimeService(ITransFileController.class);
    TransferRequest transferRequest = new TransferRequest();
    transferRequest.mIsUp = true;
    transferRequest.mLocalPath = path;
    transferRequest.mFileType = 22;
    boolean transferAsync=control.transferAsync(transferRequest);
    return transferAsync;
}
//自写
public boolean uploadCover(String path)
{
ITransFileController control = BaseApplicationImpl.getApplication().getRuntime().getRuntimeService(ITransFileController.class);
  TransferRequest transferRequest = new TransferRequest();
  transferRequest.mIsUp = true;
  transferRequest.mLocalPath = path;
  transferRequest.mFileType = 35;
  boolean transferAsync=control.transferAsync(transferRequest);
  return transferAsync;
}

import com.tencent.mobileqq.app.BaseActivity;
import com.tencent.mobileqq.troop.avatar.TroopPhotoController;

//上传群封面
import com.tencent.mobileqq.troop.avatar.TroopAvatarController;
import com.tencent.mobileqq.troop.avatar.api.ITroopPhotoUtilsApi;
import com.tencent.mobileqq.troop.activity.TroopAvatarWallEditActivity;

// 定义回调接口
public interface UploadCallback {
    void onResult(boolean success);
}

// 异步上传方法
public void UploadTroopAvatar(String qun, String filepath, Object callback) {
    if (BaseActivity.sTopActivity == null) {
        if (callback != null) callback.onResult(false);
        return;
    }
    
    BaseActivity.sTopActivity.runOnUiThread(new Runnable() {
        public void run() {
            boolean result = false;
            try {
                TroopAvatarWallEditActivity TroopAvatarActivity = new TroopAvatarWallEditActivity();
                
                Bundle bundle = new Bundle();
                bundle.putString("troopUin", qun);
                bundle.putInt("type", 1);
                
                TroopAvatarController troopAvatarController = new TroopAvatarController(
                    context, 
                    TroopAvatarActivity, 
                    app, 
                    bundle
                );
                
                String tt = QRoute.api(ITroopPhotoUtilsApi.class).getClipStr(0, 0, 0, 0);
                result = troopAvatarController.A(filepath, tt);
                
            } catch (Exception e) {
                result = false;
            } finally {
                if (callback != null) {
                    callback.onResult(result);
                }
            }
        }
    });
}


public void UploadTroopCover(String qun, String filepath, Object callback) {
    if (BaseActivity.sTopActivity == null) {
        if (callback != null) callback.onResult(false);
        return;
    }
    
    BaseActivity.sTopActivity.runOnUiThread(new Runnable() {
        public void run() {
            boolean result = false;
            try {
                TroopAvatarWallEditActivity TroopAvatarActivity = new TroopAvatarWallEditActivity();
                
                Bundle bundle = new Bundle();
                bundle.putString("troopUin", qun);
                bundle.putInt("type", 1);
                
                TroopPhotoController troopAvatarController = new TroopPhotoController(
                    context, 
                    TroopAvatarActivity, 
                    app, 
                    bundle
                );
                
                String tt = QRoute.api(ITroopPhotoUtilsApi.class).getClipStr(0, 0, 0, 0);
                result = troopAvatarController.A(filepath, tt);
                
            } catch (Exception e) {
                result = false;
            } finally {
                if (callback != null) {
                    callback.onResult(result);
                }
            }
        }
    });
}

public String findUrlbyDataMsg(Object Yu)
{
    Object data = getData();
    data.put(Yu,""+Module);
    int mtype = 0;
    if(data != null) {
        mtype = data.mtype;
    }

    if(Module.equals("模了个块")) {
        return data.quntext;
    }

    if(data == null || data.originMsg == null || data.originMsg.elements == null || data.originMsg.elements.size() < 1) {
        return "获取Url失败";
    }

    Object elem0 = data.originMsg.elements.get(0);
    if(elem0 == null) {
        return "获取Url失败";
    }

    String dataString = elem0.toString();
    String uid = "";
    if(elem0.picElement != null) {
        uid = elem0.picElement.fileUuid;
    }

    Matcher matcher = Pattern.compile("md5HexStr=(.*?),").matcher(dataString);
    Matcher matcher1 = Pattern.compile("originImageUrl=(.*?),").matcher(dataString);

    if(matcher.find() && matcher1.find())
    {
        String md5 = matcher.group(1).toUpperCase();
        String originImageUrl = matcher1.group(1);

        if(originImageUrl == null) {
            originImageUrl = "";
        }

        if(!originImageUrl.isEmpty() && !originImageUrl.equals("null"))
        {
            if(originImageUrl.startsWith("/download"))
            {
                String baseUrl = "https://gchat.qpic.cn";
                String rKey = "";
                if(originImageUrl.contains("appid=1406")) {
                    rKey = getGroupRKey();
                } else {
                    rKey = getFriendRKey();
                }
                originImageUrl = baseUrl + originImageUrl + rKey;

                if(originImageUrl.contains("null")) {
                    originImageUrl = "https://gchat.qpic.cn/gchatpic_new/0/0-0-" + md5 + "/0?term=2&is_origin=1";
                }
            }
            else {
                String baseUrl = "https://gchat.qpic.cn";
                originImageUrl = baseUrl + originImageUrl;
            }
        }
        else if(uid != null && uid.length() >= 64) {
            String baseUrl = "https://gchat.qpic.cn/download";
            String appid = "";

            if(mtype == 1) {
                appid = "?appid=1406";
            }
            else if(mtype == 2) {
                appid = "?appid=1407";
            }

            String rKey = "";
            if(appid.contains("appid=1406")) {
                rKey = getGroupRKey();
            } else {
                rKey = getFriendRKey();
            }

            originImageUrl = baseUrl + appid + "&fileid=" + uid + "&spec=0" + rKey;

            if(originImageUrl.contains("null")) {
                originImageUrl = "https://gchat.qpic.cn/gchatpic_new/0/0-0-" + md5 + "/0?term=2&is_origin=1";
            }
        }
        else
        {
            originImageUrl = "https://gchat.qpic.cn/gchatpic_new/0/0-0-" + md5 + "/0?term=2&is_origin=1";
        }
        return originImageUrl;
    }
    else
    {
        return "获取Url失败";
    }
}

public String getUserName(String uin)
{
    try
    {
        Object card = GetCard(uin);
        if(card == null||card.strNick==null)
        {
            return getUserNickName(uin);
        }
        else
        {
            return card.strNick;
        }
    }
    catch(e)
    {
        return getUserNickName(uin);
    }
}
public long GetGTK(String key)
{
    String pskey = key + "";
    int hash = 5381;
    for(int i = 0, len = pskey.length();
    i < len;
    i++)
    {
        hash = (hash + (hash << 5) + (int)(char) pskey.charAt(i)).intValue();
    }
    return hash & 0x7fffffff;
}
import com.tencent.mobileqq.profilecard.api.IProfileDataService;
import com.tencent.mobileqq.profilecard.api.IProfileProtocolService;
import com.tencent.mobileqq.data.Card;
public Object GetCard(String uin)
{
    IProfileDataService ProfileData = app.getRuntimeService(IProfileDataService.class);
    IProfileProtocolService ProtocolService = app.getRuntimeService(IProfileProtocolService.class);
    ProfileData.onCreate(app);
    Object card = ProfileData.getProfileCard(uin, false);
    if(card == null || card.iQQLevel == null)
    {
        Bundle bundle = new Bundle();
        bundle.putLong("selfUin", Long.parseLong(myUin));
        bundle.putLong("targetUin", Long.parseLong(uin));
        bundle.putInt("comeFromType", 12);
        ProtocolService.requestProfileCard(bundle);
        return null;
    }
    else return card;
}
public Object GetCard(String qun,String uin)
{
    IProfileDataService ProfileData = app.getRuntimeService(IProfileDataService.class);
    IProfileProtocolService ProtocolService = app.getRuntimeService(IProfileProtocolService.class);
    ProfileData.onCreate(app);
    Object card = ProfileData.getProfileCard(uin, false);
    if(card == null || card.iQQLevel == null)
    {
        Bundle bundle = new Bundle();
        bundle.putLong("selfUin", Long.parseLong(myUin));
        bundle.putLong("targetUin", Long.parseLong(uin));
        bundle.putLong("troopUin", Long.parseLong(qun));
        bundle.putInt("comeFromType", 5);
        ProtocolService.requestProfileCard(bundle);
        return null;
    }
    else return card;
}
public static void launchApp(Context context, String packageName) {
    PackageManager packageManager = context.getPackageManager();
    Intent launchIntent = packageManager.getLaunchIntentForPackage(packageName);
    if (launchIntent != null) {
        context.startActivity(launchIntent);
    }
}
public String JsonBeautify(String jsonString) {
    try {
        JSONObject json = new JSONObject(jsonString);
        String formattedJsonString = json.toString(4);
        return formattedJsonString;
    }
    catch(e) {
        return "错误"+e;
    }
}
import com.tencent.qphone.base.util.MD5;
public String toMD5(String str) {
    String md5 = MD5.toMD5(str);
    return md5;
}
import android.content.ComponentName;
public void startComponentName(String packageName,String className) {
    Intent intent = new Intent();
    intent.setAction(Intent.ACTION_MAIN);
    ComponentName componentName = new ComponentName(packageName, className);
    intent.setComponent(componentName);
    activity.startActivity(intent);
}
public long GetBkn(String skey)
{
   return GetGTK(skey);
}
public String httppost(String urlPath, String cookie, String data, String ContentType,String host)
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
        uc.setReadTimeout(2000000);
        uc.setRequestMethod("POST");
        uc.setRequestProperty("Host", host);
        uc.setRequestProperty("Content-Length",""+data.length());
        uc.setRequestProperty("Content-Type", ContentType);
        uc.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 5.2; en-US) AppleWebKit/537.36 (KHTML, like Gecko) Safari/537.36 VivoBrowser/14.5.12.0 Chrome/87.0.4280.141");
        uc.setRequestProperty("Cookie", cookie);
        uc.getOutputStream().write(data.getBytes("UTF-8"));
        uc.getOutputStream().flush();
        uc.getOutputStream().close();
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
            Toast( "错误:\n" + e);
        }
    }
    if(buffer.length() == 0) return buffer.toString();
    buffer.delete(buffer.length() - 1, buffer.length());
    return buffer.toString();
}
public void recallMsg(String qun,long msgid,int mtype) {
    //撤回消息
    Contact contact=new Contact(mtype,qun,"");
    ((IMsgService) QRoute.api(IMsgService.class)).recallMsg(contact,msgid,null);
    //撤回消息
}
public String httppost(String urlPath, String cookie, String data, String ContentType)
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
        uc.setReadTimeout(2000000);
        uc.setRequestMethod("POST");
        uc.setRequestProperty("Host", "web.qun.qq.com");
        uc.setRequestProperty("qname-service", "976321:131072");
        uc.setRequestProperty("Origin", "https://web.qun.qq.com");
        uc.setRequestProperty("Content-Type", ContentType);
        uc.setRequestProperty("qname-space", "Production");
        uc.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 5.2; en-US) AppleWebKit/537.36 (KHTML, like Gecko) Safari/537.36 VivoBrowser/14.5.12.0 Chrome/87.0.4280.141");
        uc.setRequestProperty("Cookie", cookie);
        uc.getOutputStream().write(data.getBytes("UTF-8"));
        uc.getOutputStream().flush();
        uc.getOutputStream().close();
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
            Toast( "错误:\n" + e);
        }
    }
    if(buffer.length() == 0) return buffer.toString();
    buffer.delete(buffer.length() - 1, buffer.length());
    return buffer.toString();
}
public static String 取文件(File f)
{
    try
    {
        String result = null;
        Random rand = new Random();
        int n = 0;
        for(Scanner sc = new Scanner(f);
        sc.hasNext();
        )
        {
            ++n;
            String line = sc.nextLine();
            //循环输出文件每行内容
            if(rand.nextInt(n) == 0) result = line;
        }
        return result;
    }
    catch(e)
    {
        return "取文件错误了";
    }
}
import java.io.FileReader;
public static String RandomLines(String filePath) {
    List lines = new ArrayList();
    try {
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        String line;
        while ((line = reader.readLine()) != null) {
            lines.add(line);
        }
    }
    catch (IOException e) {
        e.printStackTrace();
        return;
    }
    Random random = new Random();
    int randomIndex = random.nextInt(lines.size());
    String randomLine = lines.get(randomIndex);
    return randomLine;
}
public void setTips(String title, String message)
{
    Activity ThisActivity = getNowActivity();
    ThisActivity.runOnUiThread(new Runnable()
    {
        public void run()
        {
            TextView textView = new TextView(ThisActivity);
            textView.setText(message);
            textView.setTextSize(17);
            textView.setTextColor(Color.BLACK);
            textView.setTextIsSelectable(true);
            LinearLayout layout = new LinearLayout(ThisActivity);
            layout.setPadding(20, 20, 20, 20);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.addView(textView);
            new AlertDialog.Builder(ThisActivity, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT).setTitle(title).setView(layout).setNegativeButton("关闭", null).show();
        }
    }
    );
}
public void showContentDialog(String title, String content) {
    Activity ThisActivity = getNowActivity();
    ThisActivity.runOnUiThread(new Runnable() {
        public void run() {
            LinearLayout mainLayout = new LinearLayout(ThisActivity);
            mainLayout.setOrientation(LinearLayout.VERTICAL);
            mainLayout.setPadding(35, 30, 35, 25);
            mainLayout.setBackground(getShape("#FFFFFF", "#E3F2FD", 2, 25, 255, true));
            TextView titleView = new TextView(ThisActivity);
            titleView.setText(title);
            titleView.setTextColor(Color.parseColor("#1976D2"));
            titleView.setTextSize(18);
            titleView.setTypeface(null, Typeface.BOLD);
            titleView.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
            );
            titleParams.setMargins(0, 0, 0, 20);
            titleView.setLayoutParams(titleParams);
            LinearLayout contentContainer = new LinearLayout(ThisActivity);
            contentContainer.setOrientation(LinearLayout.VERTICAL);
            contentContainer.setPadding(20, 20, 20, 20);
            contentContainer.setBackground(getShape("#F8F9FA", "#E1F5FE", 1, 15, 255, false));
            LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dpToPx(ThisActivity, 400)
            );
            containerParams.setMargins(0, 0, 0, 25);
            contentContainer.setLayoutParams(containerParams);
            ScrollView scrollView = new ScrollView(ThisActivity);
            scrollView.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
            ));
            TextView contentView = new TextView(ThisActivity);
            contentView.setText(content);
            contentView.setTextColor(Color.parseColor("#424242"));
            contentView.setTextSize(14);
            contentView.setLineSpacing(1.4f, 1.4f);
            contentView.setPadding(10, 10, 10, 10);
            scrollView.addView(contentView);
            contentContainer.addView(scrollView);
            Button confirmBtn = new Button(ThisActivity);
            confirmBtn.setText("确定");
            confirmBtn.setTextColor(Color.parseColor("#FFFFFF"));
            confirmBtn.setBackground(getShape("#2196F3", "#1976D2", 0, 20, 255, false));
            confirmBtn.setPadding(60, 15, 60, 15);
            confirmBtn.setTextSize(16);
            confirmBtn.setTypeface(null, Typeface.BOLD);
            LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
            );
            btnParams.gravity = Gravity.CENTER_HORIZONTAL;
            confirmBtn.setLayoutParams(btnParams);
            mainLayout.addView(titleView);
            mainLayout.addView(contentContainer);
            mainLayout.addView(confirmBtn);
            final Dialog dialog = new Dialog(ThisActivity);
            dialog.setContentView(mainLayout);
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.setCancelable(true);
            WindowManager wm = (WindowManager) ThisActivity.getSystemService(Context.WINDOW_SERVICE);
            Display display = wm.getDefaultDisplay();
            int width = (int) (display.getWidth() * 0.85);
            int height = (int) (display.getHeight() * 0.63);
            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
            layoutParams.copyFrom(dialog.getWindow().getAttributes());
            layoutParams.width = width;
            layoutParams.height = height;
            dialog.getWindow().setAttributes(layoutParams);
            confirmBtn.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Toast("让你点了吗？");
                    dialog.dismiss();
                }
            }
            );
            dialog.show();
        }
    }
    );
}
import com.tencent.mobileqq.data.troop.TroopInfo;
public TroopInfo findTroopInfo(String qun) {
    Object app = BaseApplicationImpl.getApplication().getRuntime();
    ITroopInfoService Info = app.getRuntimeService(ITroopInfoService.class);
    return Info.findTroopInfo(""+qun);
}
public static String fetchRedirectUrl(String url)
{
    try
    {
        URL imageUrl = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) imageUrl.openConnection();
        connection.setRequestMethod("GET");
        connection.setFollowRedirects(true);
        String redirectUrl = connection.getHeaderField("Location");
        if(redirectUrl == null)
        {
            return connection.getURL().toString();
        }
        else
        {
            return redirectUrl;
        }
        connection.disconnect();
    }
    catch(Exception e)
    {
        return url;
    }
}
public String httppost2(String urlPath, String cookie, String data,String Referer)
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
        uc.setReadTimeout(2000000);
        uc.setRequestMethod("POST");
        uc.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        uc.setRequestProperty("Referer", Referer);
        uc.setRequestProperty("Cookie", cookie);
        uc.getOutputStream().write(data.getBytes("UTF-8"));
        uc.getOutputStream().flush();
        uc.getOutputStream().close();
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
            Toast( "错误:\n" + e);
        }
    }
    if(buffer.length() == 0) return buffer.toString();
    buffer.delete(buffer.length() - 1, buffer.length());
    return buffer.toString();
}
//QQ访问
import com.tencent.mobileqq.activity.QQBrowserActivity;
public qqGetUrl(String url,String title) {
    Intent intent = new Intent(getNowActivity(), QQBrowserActivity.class);
    intent.putExtra("url", url);
    intent.putExtra("finish_animation_out_to_right", true);
    intent.putExtra("is_wrap_content", true);
    intent.putExtra("hide_left_button", false);
    intent.putExtra("title", title);
    //设置标题
    getNowActivity().startActivity(intent);
}
public String MakeTextPhoto(String text,String pic,String textcolor,String qun)
{
    if(读("0","图片模式","mode")==0||读("0","图片模式","mode")==1) {
        text=text.replace("╭┅☆ 冷雨𝒥𝒶𝓋𝒶 ☆┅╮","       冷雨JAVA       \n");
        text=text.replace("╰┅━ 赞助作者 ━┅╯","        赞助作者        ");
        text=text.replace("║","  ");
        return MakeTextPhoto(text,pic,textcolor);
    }
    else if(读("0","图片模式","mode")==2) {
        text=text.replace("╭┅☆ 冷雨𝒥𝒶𝓋𝒶 ☆┅╮\n","");
        text=text.replace("\n╰┅━ 赞助作者 ━┅╯","");
        text=text.replace("║","    ");
        text=text.replace("+","[and]");
        text=text.replace("\n","↔");
        if(pic.isEmpty()) {
            pic=myWeb+"bizhi.php";
        }
        if(!pic.startsWith("http")) {
            pic=myWeb+"bizhi.php";
            Toast("传入的图片链接不对，未以http开头");
        }
        return myWeb+"ttf/gjtwhc.php?text="+text+"&image="+pic+"&fontsize=50";
    }
    else {
        return myWeb+"ttf/gjtwhc.php?text=不支持的图片模式，请重新切换&image="+pic+"&fontsize=50";
    }
}
public Bitmap getbitmap(String path) {
    if(path.startsWith("http")) {
        try {
            URL url1 = new URL(path);
            HttpURLConnection urlc = url1.openConnection();
            InputStream inst = urlc.getInputStream();
            Bitmap bmp = BitmapFactory.decodeStream(inst).copy(Bitmap.Config.ARGB_8888, true);
            return bmp;
        }
        catch(e) {
            Toast("错误:"+e+"");
            return Bitmap.createBitmap(800,800,Bitmap.Config.ARGB_8888);
        }
    }
    else {
        try {
            Bitmap bmp= BitmapFactory.decodeStream(new FileInputStream(path)).copy(Bitmap.Config.ARGB_8888, true);
            return bmp;
        }
        catch(e) {
            Toast("错误:"+e+"");
            return Bitmap.createBitmap(800,800,Bitmap.Config.ARGB_8888);
        }
    }
}
boolean auto=true;
//萌新
public String MakeTextPhoto(String text,String pic,String textcolor)
{
    String textface=ColdRainPath+"下载/字体.ttf";
    Object typeface;
    try {
        if(textFaceType==1) {
            typeface=Typeface.createFromFile(textface);
        }
        else {
            typeface=Typeface.DEFAULT_BOLD;
        }
    }
    catch(e) {
        typeface=Typeface.DEFAULT_BOLD;
    }
    text=text.replace("[]","");
    if(textcolor.equals("")) textcolor2="#FF000000";
    else textcolor2=textcolor;
    String[] word=text.split("\n");
    float textsize=40.0f;
    float padding=30.0f;
    Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
    paint.setTypeface(typeface);
    paint.setTextSize(textsize);
    Bitmap mybitmap;
    if(!pic.startsWith("http")&&!pic.startsWith("/")) mybitmap=getbitmap("https://tianquan.gtimg.cn/card/item/2034375/newPreview2.jpg");
    else mybitmap=getbitmap(pic);
    //图片链接
    float text_width=0;
    float average_width=0;
    float text_height=0;
    String newword="";
    for(String line:word)
    {
        average_width +=paint.measureText(line);
    }
    average_width=average_width/word.length;
    for(String line:word)
    {
        float width=paint.measureText(line);
        if(auto) {
            if(width-average_width>700) {
                int rr=Math.ceil(width/average_width);
                int cut=Math.ceil(line.length()/rr);
                line=splitString(line,cut);
                for(String newl:line.split("\n"))
                {
                    width=paint.measureText(newl);
                    if(text_width<width) text_width=width;
                }
            }
        }
        if(text_width<width) text_width=width;
        newword+=line+"\n";
    }
    word=newword.split("\n");
    int width=(int)(text_width + padding * 2f);
    int heigth=(int)((textsize+8) * word.length+ padding * 2f)-8;
    Bitmap original = Bitmap.createBitmap(width, heigth, Bitmap.Config.ARGB_8888);
    Canvas canvas=new Canvas(original);
    Matrix matrix = new Matrix();
    float i=(float)width/(float)mybitmap.getWidth();
    float b=(float)heigth/(float)mybitmap.getHeight();
    if(i>b) b=i;
    //if(i<b) b=i;
    matrix.postScale(b,b);
    //长和宽放大缩小的比例
    Bitmap resizeBmp = Bitmap.createBitmap(mybitmap,0,0,mybitmap.getWidth(),mybitmap.getHeight(),matrix,true);
    canvas.drawBitmap(resizeBmp, (original.getWidth()-resizeBmp.getWidth())/2, (original.getHeight()-resizeBmp.getHeight())/2, paint);
    canvas.drawColor(Color.parseColor("#6AFFFFFF"));
    //白色半透明遮罩
    paint.setColor(ParseColor(""+textcolor2,Color.BLACK));
    float yoffset=textsize+padding;
    for(String line:word)
    {
        canvas.drawText(line, padding, yoffset, paint);
        yoffset += textsize+8;
    }
    String path=RootPath+"/缓存/"+canvas+".jpg";
    File end = new File(path);
    if(!end.exists()) end.getParentFile().mkdirs();
    FileOutputStream out = new FileOutputStream(end);
    original.compress(Bitmap.CompressFormat.PNG, 100, out);
    out.close();
    return path;
}
private static String randomColor(int len) {
    try {
        StringBuffer result = new StringBuffer();
        for (int i = 0;
        i < len;
        i++) {
            result.append(Integer.toHexString(new Random().nextInt(16)));
        }
        return result.toString().toUpperCase();
    }
    catch (Exception e) {
        return "00CCCC";
    }
}
public static int getColor(String color) {
    switch(color) {
        case "红色":return Color.RED;
        case "黑色":return Color.BLACK;
        case "蓝色":return Color.BLUE;
        case "蓝绿":return Color.CYAN;
        case "白灰":return Color.LTGRAY;
        case "灰色":return Color.GRAY;
        case "绿色":return Color.GREEN;
        case "深灰":return Color.DKGRAY;
        case "洋红":return Color.MAGENTA;
        case "透明":return Color.TRANSPARENT;
        case "白色":return Color.WHITE;
        case "黄色":return Color.YELLOW;
        case "随机":return Color.parseColor("#"+randomColor(6));
        default:return Color.parseColor("#FF000000");
    }
}
public Object ParseColor(String color,Object normal) {
    Object parsecolor;
    try {
        if(color.contains("随机")) parsecolor=Color.parseColor(randomColor(6));
        else parsecolor=Color.parseColor(color);
    }
    catch(e) {
        parsecolor=normal;
    }
    return parsecolor;
}
public String splitString(String content, int len) {
    String tmp = "";
    if(len > 0) {
        if(content.length() > len) {
            int rows = Math.ceil(content.length() / len);
            for (int i = 0;
            i < rows;
            i++) {
                if(i == rows - 1) {
                    tmp += content.substring(i * len);
                }
                else {
                    tmp += content.substring(i * len, i * len + len) + "\n ";
                }
            }
        }
        else {
            tmp = content;
        }
    }
    return tmp;
}
public Bitmap bitmapurl(String url) {
    InputStream input = null;
    Bitmap original =null;
    try {
        File file=new File(url);
        if(file.exists()) original=BitmapFactory.decodeFile(url).copy(Bitmap.Config.ARGB_8888,true);
        else {
            URL urlsssss = new URL(url);
            HttpURLConnection urlConn = (HttpURLConnection) urlsssss.openConnection();
            urlConn.setConnectTimeout(10000);
            urlConn.setReadTimeout(10000);
            input = urlConn.getInputStream();
            original = BitmapFactory.decodeStream(input).copy(Bitmap.Config.ARGB_8888, true);
        }
    }
    catch (Exception e) {
    }
    return original;
}
public String getLocation(String downloadUrl)
{
    String na;
    URL url = new URL(downloadUrl);
    HttpURLConnection conn = null;
    try
    {
        conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        conn.setFollowRedirects(true);
        conn.getResponseCode();
        na = conn.getHeaderField("Location");
        if(na == null) na = conn.getURL().toString();
    }
    catch(IOException e)
    {
        na = "未知";
    }
    finally
    {
        conn.disconnect();
    }
    return na;
}
import android.content.ClipboardManager;
public void PastetoClipboard(String title,String text)
{
    ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
    ClipData clip = ClipData.newPlainText(title, text);
    clipboard.setPrimaryClip(clip);
}
import com.tencent.mobileqq.paiyipai.PaiYiPaiHandler;
public void sendPaiYiPai(String str,String str2,int i) {
    PaiYiPaiHandler inHandler = new PaiYiPaiHandler(app);
    Class clazz = PaiYiPaiHandler.class;
    Class[] ParamTYPE = new Class[] {
        String.class,String.class,int.class,int.class
    };
    Method[] methods = clazz.getMethods();
    for (Method m : methods) {
        if (m.getParameterCount() == ParamTYPE.length ) {
            Class[] params = m.getParameterTypes();
            boolean match = true;
            for (int i = 0;
            i < ParamTYPE.length;
            i++) {
                if (!params[i].equals(ParamTYPE[i])) {
                    match = false;
                    break;
                }
            }
            if (match) {
                m.setAccessible(true);
                m.invoke(inHandler,new Object[] {
                    str2,str,i,0
                }
                );
                break;
            }
        }
    }
}
public void sendPaiYiPai(String str,String str2,int type,int num) {
    for(int i=0;
    i<num;
    i++) {
        sendPaiYiPai(str,str2,type);
    }
}
import com.tencent.qqnt.troop.TroopOperationRepo;
public void ClearGroupChatRecords(String str) {
    //清空聊天记录
    if(QQ_versionName<=9095) {
        TroopOperationRepo inHandler=TroopOperationRepo.INSTANCE;
        //aa.deleteRecentContactsVer2(str);
        Class clazz = TroopOperationRepo.class;
        Method[] methods = clazz.getMethods();
        for (Method m : methods) {
            if(m.getName().equals("clearMsgRecord")) {
                m.setAccessible(true);
                m.invoke(inHandler,new Object[] {
                    str
                }
                );
                break;
            }
        }
    }
    else {
    }
}
/*
设置群消息提醒类型API
由卑微萌新(QQ779412117)开发，使用请保留版权
由冷雨修复并适配ntQQ
SetTroopMsgFilter(String qun,int type)
群号 提醒类型
1 取消免打扰(接收消息并提醒)
2 收进群助手且不提醒
3 屏蔽群消息
4 接收消息但不提醒
*/
public void SetTroopMsgFilter(String qun,int type)
{
    Object app = BaseApplicationImpl.getApplication().getRuntime();
    app.setTroopMsgFilterToServer(qun,type);
}
//检测是否为好友
import com.tencent.mobileqq.friend.api.IFriendDataService;
public boolean isFriend(String uin) {
    try {
        Object app = BaseApplicationImpl.getApplication().getRuntime();
        IFriendDataService Info = app.getRuntimeService(IFriendDataService.class);
        boolean m=Info.isFriend(uin);
        return m;
    }
    catch(e) {
        return isFriend2(uin);
    }
}
import com.tencent.qqnt.ntrelation.friendsinfo.api.IFriendsInfoService;
import com.tencent.relation.common.api.IRelationNTUinAndUidApi;
public static boolean isFriend2(String str) {
    return QRoute.api(IFriendsInfoService.class).isFriend(QRoute.api(IRelationNTUinAndUidApi.class).getUidFromUin(str), TAG);
}
public String 年月日()
{
    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
    Date date = new Date();
    String currentDate = df.format(date);
    return currentDate;
}
public String findPicUrl(PicElement picElement,int mtype)
{
    String dataString = picElement.toString();
    Matcher matcher = Pattern.compile("md5HexStr=(.*?),").matcher(dataString);
    Matcher matcher1 = Pattern.compile("originImageUrl=(.*?),").matcher(dataString);
    String uid=picElement.fileUuid;
    if(matcher.find() && matcher1.find())
    {
        String md5 = matcher.group(1).toUpperCase();
        String originImageUrl = matcher1.group(1);
        if(!originImageUrl.isEmpty()&&originImageUrl!=null&&!originImageUrl.equals("null"))
        {
            if(originImageUrl.startsWith("/download"))
            {
                String baseUrl = "https://gchat.qpic.cn";
                String rKey = originImageUrl.contains("appid=1406") ? getGroupRKey() : getFriendRKey();
                originImageUrl = baseUrl + originImageUrl + rKey;
                if(originImageUrl.contains("null")) {
                    originImageUrl = "https://gchat.qpic.cn/gchatpic_new/0/0-0-" + md5 + "/0?term=2&is_origin=1";
                }
            }
            else {
                String baseUrl = "https://gchat.qpic.cn";
                originImageUrl=baseUrl+originImageUrl;
            }
        }
        else if(uid.length()>=64) {
            String appid = "";
            String baseUrl = "https://gchat.qpic.cn/download";
            if(mtype==1) appid="?appid=1406";
            else if(mtype==2) appid="?appid=1407";
            String rKey = appid.contains("appid=1406") ? getGroupRKey() : getFriendRKey();
            originImageUrl = baseUrl + appid + "&fileid=" + uid + "&spec=0" + rKey;
            if(originImageUrl.contains("null")) {
                originImageUrl = "https://gchat.qpic.cn/gchatpic_new/0/0-0-" + md5 + "/0?term=2&is_origin=1";
            }
        }
        else
        {
            originImageUrl = "https://gchat.qpic.cn/gchatpic_new/0/0-0-" + md5 + "/0?term=2&is_origin=1";
        }
        return originImageUrl;
    }
    else
    {
        return "获取Url失败";
    }
}
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
public static String md5(String input) {
    try {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] messageBytes = input.getBytes("UTF-8");
        byte[] md5Bytes = md.digest(messageBytes);
        StringBuilder hexString = new StringBuilder();
        for (byte b : md5Bytes) {
            String hex = Integer.toHexString(0xFF & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
    catch (NoSuchAlgorithmException e) {
        throw new RuntimeException("MD5 算法不存在", e);
        return "";
    }
    catch (java.io.UnsupportedEncodingException e) {
        throw new RuntimeException("UTF-8 编码不支持", e);
        return "";
    }
}
public String getDayOfWeek() {
    Calendar calendar = Calendar.getInstance();
    int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
    String[] weekDays = {
        "星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六"
    };
    String today = weekDays[dayOfWeek - 1];
    return "" + today;
}
//云上升，利用加密来获取歌曲直链
import java.math.BigInteger;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.*;
public static final String iv = "0102030405060708";
String mima="91789428";
public static final String modulus = "00e0b509f6259df8642dbc35662901477df22677ec152b5ff68ace615bb7b725152b3ab17a876aea8a5aa76d2e417629ec4ee341f56135fccf695280104e0312ecbda92557c93870114af6c9d05c4f7f0c3685b7a46bee255932575cce10b424d813cfe4875d3e82047b97ddef52741d546b8e289dc6935b3ece0462db0a22b8e7";
public static final String pubKey="010001";
public static final String presetKey = "0CoJUm6Qyw8W8jud";
public static final String keys = "LengYu1234567890abcdfhijklmopqrstvwxyzABCDEFGHIJKMNOPQRSTUVWXZ";
public static String createSecretKey() {
    StringBuilder key = new StringBuilder();
    for (int i = 0;
    i < 16;
    i++) {
        double index = Math.floor(Math.random() * keys.length());
        key.append(keys.charAt((int) index));
    }
    return key.toString();
}
public static String zFill(String str) {
    StringBuilder strBuilder = new StringBuilder(str);
    while (strBuilder.length() < 256) {
        strBuilder.insert(0, "0");
    }
    str = strBuilder.toString();
    return str;
}
public static String rsaEncrypt(String text) {
    text = new StringBuffer(text).reverse().toString();
    BigInteger biText = new BigInteger(strToHex(text), 16);
    BigInteger biEx = new BigInteger(pubKey, 16);
    BigInteger biMod = new BigInteger(modulus, 16);
    BigInteger biRet = biText.modPow(biEx, biMod);
    return zFill(biRet.toString(16));
}
public static String strToHex(String s) {
    StringBuilder str = new StringBuilder();
    for (int i = 0;
    i < s.length();
    i++) {
        int ch = s.charAt(i);
        String s4 = Integer.toHexString(ch);
        str.append(s4);
    }
    return str.toString();
}
public String aesEncrypt( String content, String key,String iv) {
    String result = null;
    try {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        byte[] bytes;
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES"),
        new IvParameterSpec(iv.getBytes(StandardCharsets.UTF_8)));
        bytes = cipher.doFinal(content.getBytes(StandardCharsets.UTF_8));
        result = Base64.getEncoder().encodeToString(bytes);
    }
    catch (Exception e) {
        e.printStackTrace();
    }
    return result;
}
public static String[] weapiEncrypt(String content) {
    String[] result = new String[3];
    String key = presetKey;
    //createSecretKey();
    String encText = aesEncrypt(aesEncrypt(content, presetKey, iv), key, iv);
    String encSecKey = rsaEncrypt(key);
    result[0] = encText;
    result[1] = encSecKey;
    result[2] = key;
    return result;
}
import com.tencent.mobileqq.music.api.IQQPlayer;
import com.tencent.mobileqq.music.SongInfo;
public void playMusicByQQ(String song,String singer,String link,String music,String cover) {
    String key = Alpha("-88Z^DBZU]X");
    try {
        if(CurrentApp.equals("TIM")) {
            SongInfo info = new SongInfo();
            info.h = song;
            info.g = music;
            info.n = link;
            info.p = singer;
            info.s = 4;
            QRoute.api(IQQPlayer.class).startPlayMusic(context,key, new SongInfo[] {
                info
            }
            );
            /*
QQPlayerService.c1(new Intent(context, MusicPlayerActivity.class));
QQPlayerService.d1(103);
QQPlayerService.m1(context, "aio_ark.music.module",info);
*/
        }
        else if(CurrentApp.equals("QQ")) {
            SongInfo info = new SongInfo(1L,cover,music);
            info.e = singer;
            info.i = song;
            info.m = link;
            QRoute.api(IQQPlayer.class).startPlayMusic(context,key, new SongInfo[] {
                info
            }
            );
        }
    }
    catch(e) {
        Toast("错误"+e);
    }
}
import com.tencent.mobileqq.music.api.impl.QQPlayerImpl;
import com.tencent.mobileqq.musicgene.MusicPlayerActivity;
import com.tencent.mobileqq.music.QQPlayerService;
import com.tencent.mobileqq.ark.api.module.ArkAppMusicModule;
import android.content.Intent;
import com.tencent.mobileqq.music.api.impl.QQPlayerImpl;
/*
d=>1
e=>null
f=>0
g=>http://ws.stream.qqmusic.qq.com/C400002202B43Cq4V4.m4a?guid=7982463958&vkey=FD1D9DAA3DCA616EA9778730B3AD319D0B506A4BB5EFDD1984DB04F016097114F674987D5AFF68B1FC701CEB0FA8767C5C71B98740501FA4__v2b9abf47&uin=3170727851&fromtag=123032&API=ly.missqiu.icu
h=>晴天
i=>null
m=>null
n=>https://i.y.qq.com/v8/playsong.html?songmid=0039MnYb0qxYhV
o=>null
p=>周杰伦
q=>0
r=>0
s=>4
t=>false
v=>0
*/
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
public Object[] getPicUrlList(String text) {
    String regex = "\\[pic=([^]]+)\\]";
    Pattern pattern = Pattern.compile(regex);
    Matcher matcher = pattern.matcher(text);
    Object[] obj = new Object[2];
    List picUrls = new ArrayList();
    List remainingTextList = new ArrayList();
    int lastEnd = 0;
    while (matcher.find()) {
        String picUrl = matcher.group(1);
        picUrls.add(picUrl);
        String segment = text.substring(lastEnd, matcher.start());
        if (!segment.isEmpty()) {
            remainingTextList.add(segment);
        }
        lastEnd = matcher.end();
    }
    String finalSegment = text.substring(lastEnd);
    if (!finalSegment.isEmpty()) {
        remainingTextList.add(finalSegment);
    }
    obj[0] = remainingTextList;
    obj[1] = picUrls;
    return obj;
}
import com.tencent.qqnt.kernel.nativeinterface.GroupMsgMask;
import com.tencent.qqnt.troop.ITroopOperationRepoApi;
public void setGroupMsgMask(String qun, int type) {
    GroupMsgMask groupMsgMask;
    switch (type) {
        case 0:
        groupMsgMask = GroupMsgMask.NOTIFY;
        break;
        case 1:
        groupMsgMask = GroupMsgMask.RECEIVE;
        break;
        case 2:
        groupMsgMask = GroupMsgMask.ASSISTANT;
        break;
        case 3:
        try {
            groupMsgMask = GroupMsgMask.UNSPECIFIED;
        }
        catch (Exception e) {
            groupMsgMask = GroupMsgMask.SHIELD;
        }
        break;
        default:
        groupMsgMask = GroupMsgMask.NOTIFY;
        break;
    }
    QRoute.api(ITroopOperationRepoApi.class)
    .setGroupMsgMask(qun, groupMsgMask, getNowActivity(), null);
}
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
public String httpRequest(String url, String data, HashMap headerMap, String request_method) {
    StringBuffer buffer = new StringBuffer();
    InputStreamReader isr = null;
    OutputStream os = null;
    HttpURLConnection uc = null;
    try {
        URL requestUrl = new URL(url);
        uc = (HttpURLConnection) requestUrl.openConnection();
        uc.setDoInput(true);
        uc.setConnectTimeout(20000);
        uc.setReadTimeout(20000);
        uc.setRequestMethod(request_method.toUpperCase());
        if (headerMap != null && !headerMap.isEmpty()) {
            for (Map.Entry entry : headerMap.entrySet()) {
                uc.setRequestProperty(entry.getKey(), entry.getValue());
            }
        }
        if ("POST".equals(uc.getRequestMethod())) {
            uc.setDoOutput(true);
            os = uc.getOutputStream();
            os.write(data.getBytes("UTF-8"));
            os.flush();
        }
        isr = new InputStreamReader(uc.getInputStream(), "utf-8");
        BufferedReader reader = new BufferedReader(isr);
        String line;
        while ((line = reader.readLine()) != null) {
            buffer.append(line).append("\n");
        }
    }
    catch (Exception e) {
        e.printStackTrace();
    }
    finally {
        try {
            if (os != null) os.close();
            if (isr != null) isr.close();
            if (uc != null) uc.disconnect();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    if (buffer.length() > 0) {
        buffer.deleteCharAt(buffer.length() - 1);
    }
    return buffer.toString();
}

public String handleGetGTK(String key){
try{
     String kkk = get("https://api.yuafeng.cn/ly/java/getBkn.php?key="+key);
     JSONObject json = new JSONObject(kkk);
     if(json.getInt("code")==0){
          return json.optString("g_tk");
     }else{
         return "0";
     }
}catch(e){
    return "0";
}
}

public static String uploadFile(String urlStr, String formName, String filePath)
{
  long start = System.currentTimeMillis();
  File file = new File(filePath.replace("\\/", "/"));
  if(!file.exists() || !file.canRead())
  {
    return "10001";
  }
  long fileSize = file.length();
  if(fileSize > 20 * 1024 * 1024)
  {
    return "10002";
  }
  String baseResult = null;
  try
  {
    final String newLine = "\r\n";
    final String boundaryPrefix = "--";
    String BOUNDARY = "------" + System.currentTimeMillis(); // 模拟数据分隔线
    URL url = new URL(urlStr);
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.setRequestMethod("POST"); // 设置为POST请求
    conn.setDoOutput(true);
    conn.setDoInput(true);
    conn.setRequestProperty("Connection", "keep-alive");
    conn.setRequestProperty("Accept", "application/json, text/plain, */*");
    // conn.setRequestProperty("authorization", "Bearer " + 文字("0", "智谱Ai", "token"));
    conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + BOUNDARY);
    OutputStream out = conn.getOutputStream();
    StringBuilder sb = new StringBuilder();
    sb.append(boundaryPrefix);
    sb.append(BOUNDARY);
    sb.append(newLine);
    String filename=file.getName();
     filename = filePath.substring(filename.lastIndexOf('/')+1);
    sb.append("Content-Disposition: form-data; name=\"").append(formName).append("\";filename=\"").append(filename).append("\"").append(newLine);
    String extension = "zip";
    int i = filename.lastIndexOf('.');
    if(i > 0)
    {
      extension = filename.substring(i + 1);
    }
    sb.append("Content-Type: application/" + extension);
    sb.append(newLine);
    sb.append(newLine);
    out.write(sb.toString().getBytes()); // 将参数头的数据写入到输出流中
    DataInputStream in = new DataInputStream(new FileInputStream(file)); // 数据输入流,用于读取文件数据
    byte[] bufferOut = new byte[2 * 1024 * 1024]; // 2 M
    int bytes = 0;
    final int uploadAvailable = in .available();
    int curUploadSize = 0;
    StringBuilder prgBar = new StringBuilder();
    long duration = System.currentTimeMillis();
    while((bytes = in .read(bufferOut)) != -1)
    { // 每次读2M数据,并且将文件数据写入到输出流中
      out.write(bufferOut, 0, bytes);
      curUploadSize += bytes;
      if((System.currentTimeMillis() - duration) >= 100)
      {
        prgBar.append("=");
        String percent = String.format("%.2f", (curUploadSize / (float) uploadAvailable) * 100);
        String tail = "=>[" + percent + "]";
        System.out.println(prgBar.toString() + tail);
        duration = System.currentTimeMillis();
      }
    }
    System.out.println(prgBar.toString() + "==>[" + 100 + "]");
    System.out.println("send tcp packet data..");
    out.write(newLine.getBytes()); in .close();
    byte[] end_data = (newLine + boundaryPrefix + BOUNDARY + boundaryPrefix + newLine).getBytes();
    out.write(end_data);
    out.flush();
    out.close();
    StringBuilder builder = new StringBuilder();
    builder.append(conn.getResponseCode()) //<===注意，实际发送请求的代码段就在这里
      .append(" ").append(conn.getResponseMessage()).append("\n");
    Map map = conn.getHeaderFields();
    for(Map.Entry entry: map.entrySet())
    {
      if(entry.getKey() == null) continue;
      builder.append(entry.getKey()).append(": ");
      List headerValues = entry.getValue();
      Iterator it = headerValues.iterator();
      if(it.hasNext())
      {
        builder.append(it.next());
        while(it.hasNext())
        {
          builder.append(", ").append(it.next());
        }
      }
      builder.append("\n");
    }
    System.out.println(builder);
    //读取响应体
    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
    String line = null;
    StringBuilder retStr = new StringBuilder("");
    while((line = reader.readLine()) != null)
    {
      retStr.append(line);
    }
    baseResult = retStr.toString();
    conn.disconnect();
  }
  catch(Exception e)
  {
    baseResult = e.getMessage();
  }
  return baseResult;
}