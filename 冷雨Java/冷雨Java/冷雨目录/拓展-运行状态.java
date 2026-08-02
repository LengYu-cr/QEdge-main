public static String getManufacturerInfo(Context context) {
    StringBuilder deviceInfo = new StringBuilder();
    // 获取制造商
    String manufacturer = Build.MANUFACTURER;
    deviceInfo.append(""+manufacturer );
    return deviceInfo.toString();
}
public static String getScreenInfo(Context context) {
    StringBuilder deviceInfo = new StringBuilder();
    // 获取设备的屏幕分辨率
    int screenWidth = context.getResources().getDisplayMetrics().widthPixels;
    int screenHeight = context.getResources().getDisplayMetrics().heightPixels;
    deviceInfo.append(""+screenWidth+"*"+screenHeight+"");
    return deviceInfo.toString();
}
public static String getCPUInfo(Context context) {
    StringBuilder deviceInfo = new StringBuilder();
    // CPU相关(系统架构)
    String Device = Build.CPU_ABI;
    deviceInfo.append(""+Device );
    return deviceInfo.toString();
}
public String[] getBatteryStatus()
{
    IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
    Intent batteryStatus = context.registerReceiver(null, ifilter);
    float level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
    float scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
    float vol = batteryStatus.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1);
    String[] info = new String[5];
    info[0] = (int)((level / scale) * 100) + "%";
    int pluged = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
    switch(pluged)
    {
        case BatteryManager.BATTERY_PLUGGED_AC:
        info[1] = "充电中";
        break;
        case BatteryManager.BATTERY_PLUGGED_USB:
        info[1] = "USB充电中";
        break;
        default:
        info[1] = "放电中";
        break;
    }
    info[2] = batteryStatus.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY);
    info[3] = (batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10) + "℃";
    info[4] = vol / 1000 + "V";
    return info;
}
public String[] getWifiStatus()
{
    String result = null;
    WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
    WifiInfo connectionInfo = wm.getConnectionInfo();
    int rssi = connectionInfo.getRssi();
    if(rssi > -60)
    {
        result = "(信号强)";
    }
    else if(rssi <= -60 && rssi > -80)
    {
        result = "(信号一般)";
    }
    else if(rssi <= -80 && rssi > -120)
    {
        result = "(信号弱)";
    }
    else
    {
        result = "("+getOperatorName()+"_"+getCurrentNetType()+")";
    }
    String[] info = new String[2];
    info[0] = rssi + "dbm";
    info[1] = result;
    return info;
}
public String getOperatorName()
{
    String provider = "unknown";
    try
    {
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if(TelephonyManager.SIM_STATE_READY == telephonyManager.getSimState())
        {
            String operator = telephonyManager.getSimOperator();
            if(operator != null)
            {
                if(operator.equals("46000") || operator.equals("46002") || operator.equals("46007"))
                {
                    provider = "移动";
                }
                else if(operator.equals("46001"))
                {
                    provider = "联通";
                }
                else if(operator.equals("46003"))
                {
                    provider = "电信";
                }
            }
        }
        return provider;
    }
    catch(Exception e)
    {
        return "未给权限";
        Toast(0,"错误" + e);
    }
}
/** * 得到当前的手机网络类型 * * @param context * @return */
public String getCurrentNetType()
{
    String type = "未知";
    try
    {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        if(info == null)
        {
            type = "未知";
        }
        else if(info.getType() == ConnectivityManager.TYPE_WIFI)
        {
            type = getWifiStatus()[1] + "\n信号强度:" + getWifiStatus()[0];
        }
        else if(info.getType() == ConnectivityManager.TYPE_MOBILE)
        {
            int subType = info.getSubtype();
            if(subType == TelephonyManager.NETWORK_TYPE_CDMA || subType == TelephonyManager.NETWORK_TYPE_GPRS || subType == TelephonyManager.NETWORK_TYPE_EDGE)
            {
                type = "2G";
            }
            else if(subType == TelephonyManager.NETWORK_TYPE_UMTS || subType == TelephonyManager.NETWORK_TYPE_HSDPA || subType == TelephonyManager.NETWORK_TYPE_EVDO_A || subType == TelephonyManager.NETWORK_TYPE_EVDO_0 || subType == TelephonyManager.NETWORK_TYPE_EVDO_B)
            {
                type = "3G";
            }
            else if(subType == TelephonyManager.NETWORK_TYPE_LTE)
            {
                type = "4G";
            }
            else type = "5G";
            type = ""+ type;
        }
        return type;
    }
    catch(Exception e)
    {
        return "未给权限";
        Toast( "错误" + e);
    }
}
public static String getHostInfo()
{
    try
    {
        return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName + "(" + context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode + ")";
    }
    catch(Throwable e)
    {
        Log.e("Utils", "Can not get PackageInfo!");
        throw new AssertionError("Can not get PackageInfo!");
    }
}
public static List getInstalledApplication(boolean needSysAPP)
{
    PackageManager packageManager = context.getPackageManager();
    Intent intent = new Intent(Intent.ACTION_MAIN);
    intent.addCategory(Intent.CATEGORY_LAUNCHER);
    List resolveInfos = packageManager.queryIntentActivities(intent, 0);
    if(!needSysAPP)
    {
        List resolveInfosWithoutSystem = new ArrayList();
        for(int i = 0;
        i < resolveInfos.size();
        i++)
        {
            ResolveInfo resolveInfo = resolveInfos.get(i);
            try
            {
                if(!isSysApp(resolveInfo.activityInfo.packageName))
                {
                    resolveInfosWithoutSystem.add(resolveInfo);
                }
            }
            catch(PackageManager.NameNotFoundException e)
            {
                Toast( "错误" + e);
            }
        }
        return resolveInfosWithoutSystem;
    }
    return resolveInfos;
}
public static boolean isSysApp(String packageName) throws PackageManager.NameNotFoundException
{
    PackageInfo packageInfo = context.getPackageManager().getPackageInfo(packageName, 0);
    return(packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 1;
}
public static String phoneModel()
{
    return Build.MODEL;
}
public static String phoneReleaseVersion()
{
    return Build.VERSION.RELEASE;
}
public static String phoneSdkVersion()
{
    return Build.VERSION.SDK;
}
public static String phoneplay()
{
    return Build.DISPLAY;
}
public String getAvailMemory()
{
    ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
    ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
    am.getMemoryInfo(mi);
    return formatSize(mi.availMem);
}
public static String getAvailableInternalMemorySize()
{
    File path = Environment.getDataDirectory();
    StatFs stat = new StatFs(path.getPath());
    long blockSize = stat.getBlockSize();
    long availableBlocks = stat.getAvailableBlocks();
    return Formatter.formatFileSize(context, availableBlocks * blockSize);
}
public static String getTotalInternalMemorySize()
{
    File path = Environment.getDataDirectory();
    StatFs stat = new StatFs(path.getPath());
    long blockSize = stat.getBlockSize();
    long totalBlocks = stat.getBlockCount();
    return Formatter.formatFileSize(context, totalBlocks * blockSize);
}
public static String formatSize(float size)
{
    String suffix = null;
    if(size >= 1024)
    {
        suffix = "KB";
        size /= 1024;
        if(size >= 1024)
        {
            suffix = "MB";
            size /= 1024;
        }
        if(size >= 1024)
        {
            suffix = "GB";
            size /= 1024;
        }
    }
    DecimalFormat format = new DecimalFormat(".00");
    String p = format.format(size);
    StringBuilder resultBuffer = new StringBuilder(p);
    if(suffix != null) resultBuffer.append(suffix);
    return resultBuffer.toString();
}
public static String formatTime(float time)
{
    String suffix = "豪秒";
    long seconds = (long)(time / 1000);
    String tr = seconds / 3600 + "时" + (seconds % 3600) / 60 + "分" + seconds % 3600 % 60 % 60 + "秒";
    tr = tr.replace("分0秒", "分");
    tr = tr.replace("时0分", "时");
    tr = tr.replace("0时", "");
    return tr;
}
import com.tencent.mobileqq.onlinestatus.api.IOnlineStatusService;
import mqq.app.AppRuntime.Status;
public String getOnlineStatus() {
    Status status=app.getRuntimeService(IOnlineStatusService.class). getOnlineStatus();
    String StatusString=status+"";
    if(StatusString.equals("null")) return "未知";
    else if(StatusString.equals("away")) return "离开";
    else if(StatusString.equals("offline")) return "离线";
    else if(StatusString.equals("invisiable")) return "隐身";
    else if(StatusString.equals("busy")) return "忙碌";
    else if(StatusString.equals("qme")) return "Q我吧";
    else if(StatusString.equals("dnd")) return "请勿打扰";
    else if(StatusString.equals("online")) return "在线";
    else if(StatusString.equals("receiveofflinemsg")) return "失联";
    else return StatusString;
}
public static String getBatteryHealth(Context context) {
    IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
    Intent batteryStatus = context.registerReceiver(null, ifilter);
    int health = batteryStatus.getIntExtra(BatteryManager.EXTRA_HEALTH, -1);
    String healthStatus = "";
    switch (health) {
        case BatteryManager.BATTERY_HEALTH_UNKNOWN:
        healthStatus = "未知";
        break;
        case BatteryManager.BATTERY_HEALTH_GOOD:
        healthStatus = "良好";
        break;
        case BatteryManager.BATTERY_HEALTH_OVERHEAT:
        healthStatus = "过热";
        break;
        case BatteryManager.BATTERY_HEALTH_DEAD:
        healthStatus = "损坏";
        break;
        case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE:
        healthStatus = "电压过高";
        break;
        case BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE:
        healthStatus = "未指定故障";
        break;
    }
    return healthStatus;
}
public String getIPAddress() {
    //获取本机ip
    try {
        InetAddress localHost = InetAddress.getLocalHost();
        String ipAddress = localHost.getHostAddress();
        return ipAddress;
    }
    catch (e) {
        return e+"";
    }
}
public int getCPURunningNum() {
    int cores = Runtime.getRuntime().availableProcessors();
    return cores;
}
import android.telephony.TelephonyManager;
public String getSimCardInfo() {
    //sim卡信息
    TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
    String operatorName = telephonyManager.getSimOperatorName();
    String operatorCode = telephonyManager.getSimOperator();
    String countryCode = telephonyManager.getSimCountryIso();
    return "Operator Name: " + operatorName+"\nOperator Code: " + operatorCode+"\nCountry Code: " + countryCode;
}
import android.app.ActivityManager.RunningAppProcessInfo;
public String RunningAppProcesses() {
    try {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List appProcesses = activityManager.getRunningAppProcesses();
        for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
            if (appProcess.processName.equals(QQ_PackageName)) {
                if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                    return "在前台";
                    // 应用处于前台
                }
                else {
                    return "在后台";
                }
            }
            else {
                return "可能在后台";
            }
        }
    }
    catch(e) {
        return "可能在后台";
    }
}
public String getXPName() {
    try {
        Object cl=this.getClass().getClassLoader();
        Class clazz=cl.loadClass("de.robv.android.xposed.XposedBridge");
        Field f=clazz.getField("TAG");
        f.setAccessible(true);
        String tag=(String)f.get(null);
        return tag;
    }
    catch(e) {
        return "LSPosed";
    }
}
public void 运行状态(Object Yu) {
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
            if(qq.equals(uin)||读("0","代管",uin)==1) {
                int type2=data.mtype;
                long time2 = data.originMsg.msgTime;
                SimpleDateFormat df = new SimpleDateFormat("MM月dd日 HH:mm:ss");
                if(quntext.equals("运行状态"))
                {
                    List apps = new ArrayList();
                    apps = getInstalledApplication(true);
                    Calendar calendar = Calendar.getInstance();
                    String time3 = df.format(calendar.getTime());
                    if(type2 == 2) GroupInfo = "群名:" + data.originMsg.peerName;
                    else if(type2 == 1) GroupInfo = "好友名:" + data.originMsg.sendNickName;
                    File folder = new File(pluginPath);
                    String formattedSize = getFormattedSize(folder);
                    String qqnt="";
                    String ath=LocalPath.substring(18);
                    if(ath.startsWith("999")) qqnt="Ⅱ·";
                    String doubleOpen="";
                    if(ath.startsWith("999")) doubleOpen="双开";
                    else if(ath.startsWith("10")) doubleOpen="非机主";
                    try {
                        String info = "运行状态:\nQQ:" + myUin + "\n昵称:" + getUserName(myUin) + "\n在线状态:" + getOnlineStatus() + "\n" + GroupInfo + "\n电池类型:" + getBatteryStatus()[2] + "\n电池温度:" + getBatteryStatus()[3] + "\n电池电压:" + getBatteryStatus()[4] + "\n电池电量:" + getBatteryStatus()[0] + "(" + getBatteryStatus()[1] + ")\n电池健康:" + getBatteryHealth(context) + "\n"+doubleOpen+"应用数量:" + apps.size() + "个("+qqnt+CurrentApp+RunningAppProcesses()+")\n剩余运存:" + getAvailMemory() + "\n剩余储存:" + getAvailableInternalMemorySize() + "/" + getTotalInternalMemorySize() + "\n"+CurrentApp+"版本:" + getHostInfo() + "\n运行时间:" + formatTime((float)(time2 * 1000 - firstLoadTime)) + "\n手机型号:" + getManufacturerInfo(context) + " " + phoneModel() + "\n信号强度:" + getWifiStatus()[0] + getWifiStatus()[1] + "\n屏幕分辨率:" + getScreenInfo(context) + "\nCPU架构:" + getCPUInfo(context) + "(运行:" + getCPURunningNum() + "个)\n运行脚本:" + readprop(pluginPath + "info.prop", "pluginName") + "(" + reallVersion + ")\n脚本ID:" + readprop(pluginPath + "info.prop", "id") + "\n脚本大小:" + formattedSize + "\n运行模块:"+Module+"_"+ModuleVersionName+"\n运行框架:"+getXPName()+"\nBuild ID:" + phoneplay() + "\n安卓版本:" + phoneReleaseVersion() + "(SDK:" + phoneSdkVersion() + ")\n当前时间:" + time3;
                        info = info.replace(" GB", "GB");
                        sendText(data,info);
                    }
                    catch(e) {
                        sendText(data,"运行状态:\n出错"+e);
                    }
                }
            }
        }
    }
    ).start();
}