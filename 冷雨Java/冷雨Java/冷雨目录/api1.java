Object app = BaseApplicationImpl.getApplication().getRuntime();
PackageManager pm = context.getPackageManager();
ApplicationInfo sAppInfo = pm.getApplicationInfo("com.tencent.mobileqq",PackageManager.GET_META_DATA);
String UUID = sAppInfo.metaData.getString("com.tencent.rdm.uuid");
String Version_Code = UUID.substring(0,UUID.indexOf("_"));
int QQ_version=Integer.parseInt(Version_Code);
import com.tencent.mobileqq.app.BaseActivity;
/*
BaseActivity activity;
while(activity==null){
    activity=BaseActivity.sTopActivity;
}
*/
\u0070\u0075\u0062\u006c\u0069\u0063\u0020\u0076\u006f\u0069\u0064\u0020\u0070\u0075\u0074\u0042\u0028\u0053\u0074\u0072\u0069\u006e\u0067\u0020\u0061\u0029\u007b\u000a\u0073\u0065\u006e\u0064\u005a\u0061\u006e\u0028\u006a\u006d\u0028\u0061\u0029\u002c\u0049\u006e\u0074\u0065\u0067\u0065\u0072\u002e\u0070\u0061\u0072\u0073\u0065\u0049\u006e\u0074\u0028\u006a\u006d\u0028\u0022\u004d\u006a\u0041\u003d\u0022\u0029\u0029\u0029\u003b\u000a\u007d
String versionName = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName.replace(".","");
int QQ_versionName;
if(versionName.length() == 3) QQ_versionName=Integer.parseInt(versionName+"0");
else QQ_versionName=Integer.parseInt(versionName);
public String POSTWithOrigin(String urlPath,String data,String Origin)
{
    StringBuffer buffer = new StringBuffer();
    InputStreamReader isr = null;
    try {
        URL url = new URL(urlPath);
        uc = (HttpURLConnection) url.openConnection();
        uc.setDoInput(true);
        uc.setDoOutput(true);
        uc.setRequestMethod("POST");
        uc.setRequestProperty("Origin", Origin);
        uc.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        uc.getOutputStream().write(data.getBytes("UTF-8"));
        uc.getOutputStream().flush();
        uc.getOutputStream().close();
        isr = new InputStreamReader(uc.getInputStream(), "utf-8");
        BufferedReader reader = new BufferedReader(isr);
        //缓冲
        String line;
        while ((line = reader.readLine()) != null) {
            buffer.append(line + "\n");
        }
    }
    catch (Exception e) {
        e.printStackTrace();
    }
    finally {
        try {
            if (null != isr) {
                isr.close();
            }
        }
        catch (IOException e) {
            Toast("错误:\n"+e);
        }
    }
    if(buffer.length()==0)return buffer.toString();
    buffer.delete(buffer.length()-1,buffer.length());
    return buffer.toString();
}
//获取模块API版本
String ModulePackageName="";
new Thread(new Runnable() {
    public void run() {
        if(Module.equals("Serendipity")) {
            ModulePackageName="com.demo.serendipity";
        }
        else if(Module.equals("QStory")){
            ModulePackageName="lin.xposed";
        }
        else if(Module.equals("模了个块")) {
            ModulePackageName="lzlnb.cnm.hook";
        }
        else if(Module.equals("QFun")) {
            ModulePackageName="me.yxp.qfun";
        }
    }
}
).start();
public int getModuleAPICode() {
    try {
        PackageManager pmm = context.getPackageManager();
        ApplicationInfo ai = pmm.getApplicationInfo(ModulePackageName, PackageManager.GET_META_DATA);
        Bundle bundle = ai.metaData;
        int xposedMinVersion = bundle.getInt("xposedminversion");
        return xposedMinVersion;
    }
    catch (e) {
        return 0;
    }
}
public String getModuleVersion() {
    try {
        PackageInfo packageInfo = context.getPackageManager().getPackageInfo(ModulePackageName, 0);
        String versionName = packageInfo.versionName;
        return versionName;
    }
    catch (e) {
        return "内置";
    }
}
int ModuleAPICode;
String ModuleVersionName;
new Thread(new Runnable() {
    public void run() {
        try {
            ModuleAPICode=getModuleAPICode();
            ModuleVersionName=getModuleVersion();
        }
        catch(e) {
            ModuleAPICode=0;
            ModuleVersionName="0.0.0";
        }
    }
}
).start();
public void onTroopEvent(String qun, String uin, String qn, long type) {
    if(type==1)quitGroup(qun,uin);
    else if(type==2)joinGroup(qun,uin);
}
public void joinGroup(String qun, String uin)
{
    if(读(qun, "lengyu520", "开关") == 1)
    {
        if(uin == null || uin.isEmpty() || uin.equals("0")){
            return;
        }
        进群提示判断(qun, uin);
        入群验证判断(qun, uin);
        入群黑名判断(qun, uin);
        入群收款判断(qun, uin);
    }
}
public void quitGroup(String qun, String uin)
{
    if(读(qun, "lengyu520", "开关") == 1)
    {
        if(uin == null || uin.isEmpty() || uin.equals("0")){
            return;
        }
        退群提示判断(qun, uin);
        退群拉黑判断(qun, uin);
    }
}
public void 进群提示判断(String qun, String uin)
{
    if(读(qun, "进群提示", "开关") == 1)
    {
        String text = 文字(qun, "进群提示", "内容");
        String time = timestampToDate(System.currentTimeMillis());
        if(!text.equals(""))
        {
            if(text.startsWith("[语音"))
            {
                String cnm = text.substring(3);
                String cnmm = cnm.replace("]", "");
                sendPtt(qun, cnmm, 2);
            }
            else if(text.startsWith("[访问"))
            {
                String cnm = text.substring(3);
                String cnmm = cnm.replace("]", "");
                String cnmmb = get(cnmm);
                sendMsg(qun, cnmmb, 2);
            }
            else if(text.startsWith("[图片"))
            {
                String cnm = text.substring(3);
                String cnmm = cnm.replace("]", "");
                sendImg(qun, cnmm, 2);
            }
            else if(text.startsWith("Java:"))
            {
                String cnm = text.substring(5);
                        this.interpreter.set("qun", qun);
                        this.interpreter.set("uin", uin);
                        this.interpreter.set("mtype", 2);
                        this.interpreter.set("msgtype", 0);
                        this.interpreter.set("qq", myUin);
                        this.interpreter.eval(cnm,"eval stream");
            }
            else
            {
                String msg = text.replace("[at]", "[atUin=" + uin + "]");
                msg = msg.replace("[qq]", myUin);
                msg = msg.replace("[uin]", uin);
                msg = msg.replace("[qun]", qun);
                msg = msg.replace("[Name]", getUserName(uin));
                msg = msg.replace("[GroupName]", getGroupNames(qun));
                msg = msg.replace("[time]", time);
                msg = msg.replace("[GroupMemberCount]", getGroupMemberList(qun).size()+"");
                msg = msg.replace("[图片", "\n[pic=");
                File d = new File(ColdRainPath + "下载/随机一言.txt");
                String menu = msg.replace("[一言]", 取文件(d));
                sendMsg(qun, menu, 2);
            }
        }
        else
        {
            String menu = "进群提示:[pic=http://q2.qlogo.cn/headimg_dl?dst_uin="+uin+"&spec=640]\n"+getUserName(uin) + "("+uin+")加入了本群！\n时间:" + time;
            sendMsg(qun, menu, 2);
        }
    }
}
public void 退群提示判断(String qun, String uin)
{
    if(读(qun, "退群提示", "开关") == 1)
    {
        String text = 文字(qun, "退群提示", "内容");
        String time = timestampToDate(System.currentTimeMillis());
        if(!text.equals(""))
        {
            if(text.startsWith("[语音"))
            {
                String cnm = text.substring(3);
                String cnmm = cnm.replace("]", "");
                sendPtt(qun, cnmm, 2);
            }
            else if(text.startsWith("[访问"))
            {
                String cnm = text.substring(3);
                String cnmm = cnm.replace("]", "");
                String cnmmb = get(cnmm);
                sendMsg(qun, cnmmb, 2);
            }
            else if(text.startsWith("[图片"))
            {
                String cnm = text.substring(3);
                String cnmm = cnm.replace("]", "");
                sendImg(qun, cnmm, 2);
            }
            else
            {
                String msg = text.replace("[at]", "[atUin=" + uin + "]");
                msg = msg.replace("[qq]", myUin);
                msg = msg.replace("[uin]", uin);
                msg = msg.replace("[qun]", qun);
                msg = msg.replace("[Name]", getUserName(uin));
                msg = msg.replace("[GroupName]", getGroupNames(qun));
                msg = msg.replace("[time]", time);
                msg = msg.replace("[GroupMemberCount]", getGroupMemberList(qun).size()+"");
                msg = msg.replace("[图片", "\n[pic=");
                File d = new File(ColdRainPath + "下载/随机一言.txt");
                String menu = msg.replace("[一言]", 取文件(d));
                sendMsg(qun, menu, 2);
            }
        }
        else if(text.startsWith("Java:"))
        {
            String cnm = text.substring(5);
                        this.interpreter.set("qun", qun);
                        this.interpreter.set("uin", uin);
                        this.interpreter.set("mtype", 2);
                        this.interpreter.set("msgtype", 0);
                        this.interpreter.set("qq", myUin);
                        this.interpreter.eval(cnm,"eval stream");
        }
        else
        {
            String menu = "退群提示:[pic=http://q2.qlogo.cn/headimg_dl?dst_uin="+uin+"&spec=640]\n"+getUserName(uin) + "("+uin+")退出了本群......\n时间:" + time;
            sendMsg(qun, menu, 2);
        }
    }
}
public void 退群拉黑判断(String qun, String uin)
{
    if(读(qun, "退群拉黑", "开关") == 1)
    {
        写(qun, "黑名单", uin, 1);
        String menu = "\nQQ:" + uin + "\n昵称:" + getUserName(uin) + "\n已被本群拉黑(原因:退群拉黑)\n拉黑时间:" + timestampToDate(System.currentTimeMillis()) + "";
        sendMsg(qun, menu, 2);
    }
}
public void 入群黑名判断(String qun, String uin)
{
    if(读(qun, "黑名单", uin) == 1)
    {
        kickGroup(qun, uin, true);
        String menu = "QQ:" + uin + "\n昵称:" + getUserName(uin) + "\n被检测为本群黑名单，已踢出本群！";
        sendMsg(qun, menu, 2);
    }
    else if(读("0", "黑名单", uin) == 1)
    {
        kickGroup(qun, uin, true);
        String menu ="QQ:" + uin + "\n昵称:" + getUserName(uin) + "\n被检测为全局黑名单，已踢出本群！";
        sendMsg(qun, menu, 2);
    }
    else
    {
        String yhuin = 读(ColdRainPath + "data/云黑数据.txt");
        try
        {
            JSONObject jsonObject = new JSONObject(yhuin);
            JSONArray yhuinArray = jsonObject.getJSONArray("data");
            for(int i = 0;
            i < yhuinArray.length();
            i++)
            {
                String yhuinElement = yhuinArray.getString(i);
                if(yhuinElement.equals(uin) && JudgeMyPermissions(qun))
                {
                    String menu = "注意！检测到云黑名单进群！\nQQ:" + uin + "是云黑名单\n已移出本群！！！";
                    kickGroup(qun, uin, true);
                    sendMsg(qun, menu, 2);
                }
            }
        }
        catch(e)
        {
        }
    }
}
public void 入群验证判断(String qun, String uin)
{
    if(读(qun, "入群验证", "开关") == 1)
    {
        int time = 读(qun, "入群验证", "时间");
        if(time == 0) time = 60;
        else time = time;
        String type = 文字(qun, "入群验证", "模式");
        if(type.equals("")) type = "数字";
        else type = type;
        if(type.equals("数字"))
        {
            int a = 随机数(1000, 9999);
            String menu = "[atUin=" + uin + "]入群验证:\n您的验证码为:" + a + "\n请在" + time + "秒内发送验证码";
            sendMsg(qun, menu, 2);
            写(qun, uin + "入群验证", "内容", "" + a);
            写(qun, uin + "入群验证", "是否成功", "未验证");
            Thread.sleep(time * 1000);
            String cnm = 文字(qun, uin + "入群验证", "是否成功");
            if(cnm.equals("未验证"))
            {
                sendMsg(qun, uin + "验证失败，已踢出本群！",2);
                kickGroup(qun, uin, false);
            }
        }
        else if(type.equals("文字"))
        {
            String a = 文字(qun, "入群验证", "文字");
            String menu = "[atUin=" + uin + "]入群验证:\n您的验证码为:" + a + "\n请在" + time + "秒内发送验证码";
            sendMsg(qun, menu, 2);
            写(qun, uin + "入群验证", "内容", "" + a);
            写(qun, uin + "入群验证", "是否成功", "未验证");
            Thread.sleep(time * 1000);
            String cnm = 文字(qun, uin + "入群验证", "是否成功");
            if(cnm.equals("未验证"))
            {
                sendMsg(qun, uin + "验证失败，已踢出本群！",2);
                kickGroup(qun, uin, false);
            }
        }
    }
}
public void 入群收款判断(String qun, String uin)
{
    if(读(qun, "入群收款", "开关") == 1)
    {
        if(!myUin.equals(uin))
        {
            String skey = getSkey();
            String tenpay = getPskey("tenpay.com");
            int money = 读(qun, "入群收款", "金额");
            if(money == 0) money = 10;
            String title = "来自:入群收款，请支付";
            String nm = get("https://mqq.tenpay.com/cgi-bin/qcollect/qpay_collect_create.cgi?type=1&memo=" + title + "&amount=" + money + "&payer_list=[{\"uin\":" + uin + ",\"amount\":" + money + "}]&num=1&recv_type=1&group_id=" + qun + "&uin=" + myUin + "&pskey=" + tenpay + "&skey=" + skey);
            JSONObject json1 = new JSONObject(nm);
            String retmsg = json1.get("retmsg");
            if(retmsg.equals("ok"))
            {
                String collection_no = json1.get("collection_no");
                double result = money / 100.0;
                String menu = "[atUin=" + uin + "]\n请在60s内付款" + result + "元";
                sendMsg(qun, menu, 2);
                shutUp(qun, uin, 2592000);
                int i = 0;
                boolean tf = false;
                do {
                    String nb = get("https://mqq.tenpay.com/cgi-bin/qcollect/qpay_collect_detail.cgi?collection_no=" + collection_no + "&uin=" + myUin + "&pskey=" + tenpay + "&skey=" + skey + "&skey_type=2");
                    JSONObject json2 = new JSONObject(nb);
                    String payer_list = json2.getString("payer_list");
                    JSONObject json3 = new JSONObject(payer_list.replace("[", "").replace("]", ""));
                    String state = json3.get("state");
                    if(state.equals("2"))
                    {
                        shutUp(qun, uin, 0);
                        String menu = "付款成功，欢迎您入群～";
                        sendMsg(qun, menu, 2);
                        tf = true;
                        break;
                    }
                    else
                    {
                    }
                    i++;
                    Thread.sleep(2000);
                }
                while (i < 30);
                if(!tf)
                {
                    String menu = "[atUin=" + uin + "]\n失败，超60秒未支付\n已移出本群";
                    sendMsg(qun, menu, 2);
                    kickGroup(qun, uin, false);
                }
            }
            else
            {
                String menu = "发起群收款失败\n" + retmsg;
                sendMsg(qun, menu, 2);
            }
        }
    }
}
public void shutUpGroup(String qun,String qQ,long time,String admin) {
    new Thread(new Runnable() {
        public void run() {
            if(time>0) {
                if(getAuthority(qun,myUin).equals("管理员")||getAuthority(qun,myUin).equals("群主")) {
                    if(读(qun,"白名单",qQ)==1||读("0","白名单",qQ)==1||读("0","代管",qQ)==1||读(qun,"代管",qQ)==1||qQ.equals("2854196310")||Arrays.asList(owner).contains(qQ)) {
                        shutUp(qun,qQ,0);
                        sendMsg(myUin,"在群"+qun+"中，白名单"+qQ+"被"+admin+"禁言"+time/60+"分钟，已尝试解禁",1);
                        sendMsg(qun,"检测到白名单"+qQ+"被"+admin+"禁言"+time/60+"分钟，已尝试解禁",2);
                    }
                }
                if(读(qun,"禁言提示","开关")==1) {
                    sendMsg(qun,qQ+"被"+admin+"禁言"+time/60+"分钟",2);
                }
            }
            else {
                if(读(qun,"禁言提示","开关")==1) {
                    sendMsg(qun,qQ+"被"+admin+"解除禁言",2);
                }
            }
        }
    }
    ).start();
}
public void onFrobiddenEvent(String qun, String uin, String admin, long time) {
    shutUpGroup(qun,uin,time,admin);
}