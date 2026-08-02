String[] owner;

// 作者权限,操作开关机而已
new Thread(new Runnable() {
    public void run() {
        try {
            JSONObject ArraysObject = new JSONObject(get(myWeb + "User/Owner.json"));
            JSONArray uinArray = ArraysObject.getJSONArray("data");
            owner = new String[uinArray.length()];
            for (int i = 0; i < uinArray.length(); i++) {
                String uinElement = uinArray.getString(i);
                owner[i] = uinElement;
            }
        } catch (Exception e) {
            try {
                String ownerString = "{\"data\":[\"1431136407\",\"3635522745\",\"2633141805\"]}";
                JSONObject ArraysObject = new JSONObject(ownerString);
                JSONArray uinArray = ArraysObject.getJSONArray("data");
                owner = new String[uinArray.length()];
                for (int i = 0; i < uinArray.length(); i++) {
                    String uinElement = uinArray.getString(i);
                    owner[i] = uinElement;
                }
            } catch (Exception ee) {
            }
        }
    }
}).start();

// 检测更新
new Thread(new Runnable() {
    public void run() {
        try {
            long oldV = Long.parseLong(Version.replace(".", ""));
            String Vnew = get(NETWORK_LINE + "plugin/myJava.php?version=" + oldV + "&uin=" + myUin);
            if (!Vnew.equals("访问网页失败")) {
                JSONObject json_update = new JSONObject(Vnew);
                if (json_update.getInt("code") == 0 || json_update.getInt("code") == 1) {
                    Vnew = json_update.optString("version");
                    long newV = Long.parseLong(Vnew.replace(".", ""));
                    if (newV != oldV) {
                        String url = json_update.optString("path");
                        Toast("检测到新版本！\n冷雨Java" + Vnew + "\n请耐心等待，自动更新");
                        sendMsg(myUin, "发现新版本:\n冷雨Java" + Vnew + "\n请耐心等待更新", 1);
                        DownloadToFile(url, ColdRainPath + "/下载/冷雨Java" + Vnew + ".zip");
                        Unzip(ColdRainPath + "/下载/冷雨Java" + Vnew + ".zip", "" + QQPath);
                        sc(ColdRainPath + "/下载/冷雨Java" + Vnew + ".zip");
                        Toast("更新冷雨Java" + Vnew + "成功");
                        String nb = 读(pluginPath + "更新日志.txt");
                        showContentDialog("冷雨Java更新日志:", nb);
                        downloadAllJavas(myUin, 1);
                        if (json_update.getInt("code") == 1) {
                            showContentDialog("冷雨Java公告:", json_update.getString("msg"));
                        }
                    }
                } else if (json_update.getInt("code") == -1) {
                    String msg = json_update.getString("msg");
                    if (!文字("冷雨Java", "通知", "已读").equals(md5(msg))) {
                        showContentDialog("冷雨Java通知:", msg);
                        写("冷雨Java", "通知", "已读", md5(msg));
                    }
                } else {
                    String msg = json_update.getString("msg");
                    try {
                        if (!文字("冷雨Java", "通知", "已读").equals(md5(msg))) {
                            showContentDialog("冷雨Java通知:", msg);
                            写("冷雨Java", "通知", "已读", md5(msg));
                        }
                    } catch (Exception ee) {
                    }
                }
            } else {
                sendMsg(myUin, "冷雨Java检测更新失败，请到官方群反馈或者[切换路线]\n指令: 线路1 => 顶端云(https://cloud.dinduan.com/aff/MNAXRDDW)", 1);
                Toast("冷雨Java检测更新失败，请稍后重试");
            }
        } catch (Exception e) {
            sendMsg(myUin, "冷雨Java检测更新失败，请到官方群反馈或者[切换路线]\n指令: 线路1 => 顶端云(https://cloud.dinduan.com/aff/MNAXRDDW)", 1);
        }
    }
}).start();

// 心跳上报
import android.os.Handler;
import android.os.Looper;

Handler heart_beat = new Handler(Looper.getMainLooper());

// 心跳任务
final Runnable heartTask = new Runnable() {
    public void run() {
        new Thread(new Runnable() {
            public void run() {
                try {
                    JSONObject data = new JSONObject();
                    data.put("uin", myUin);
                    data.put("name", getUserName(myUin));
                    data.put("package_name", QQ_PackageName);
                    data.put("qq_version", context.getPackageManager().getPackageInfo(QQ_PackageName, 0).versionName);
                    data.put("module", Module);

                    String jsonStr = data.toString();
                    byte[] bytes = jsonStr.getBytes("UTF-8");
                    StringBuilder hex = new StringBuilder();
                    for (byte b : bytes) {
                        hex.append(String.format("%02x", b));
                    }

                    String url = NETWORK_LINE + "plugin/";
                    String u = httppost1(url, "", hex.toString());

                    JSONObject json = new JSONObject(u);

                    if (json.getInt("code") == 1 && json.getString("msg").equals("first")) {
                        heart_beat.post(new Runnable() {
                            public void run() {
                                Toast("检测到为首次将您的数据录入系统，请保管好密码");
                                sendMsg(myUin, "您的冷雨Java后台:\nhttps://v.yuafeng.cn/plugin/user/\n账号:" + myUin + "\n密码:" + json.getJSONObject("data").getString("password"), 1);
                            }
                        });
                    } else if (json.getInt("code") == -2) {
                        heart_beat.post(new Runnable() {
                            public void run() {
                                Toast(json.getString("msg"));
                                sendMsg(myUin, "你已被冷雨Java云黑\n" + json.getString("msg") + "\n接下来3s内QQ将会闪退", 1);
                            }
                        });
                        Thread.sleep(3000);
                        System.exit(1);
                    }
                } catch (Exception e) {}

                heart_beat.postDelayed(heartTask, 30 * 1000);
            }
        }).start();
    }
};

heart_beat.postDelayed(heartTask, 100);

// 权限指令
public void 权限(Object Yu) {
    try {
        Object data = getData();
        data.put(Yu, "" + Module);
        String quntext = data.quntext;
        String qun = data.qun;
        String uin = data.uin;
        String qq = myUin;
        int mtype = data.mtype;
        long msgid = data.msgid;
        int msgtype = data.msgtype;

        if (quntext.equals("一键关机")) {
            if (读(qun, "lengyu520", "开关") == 1) {
                boolean isAllow = false;
                if (owner != null) {
                    isAllow = Arrays.asList(owner).contains(uin);
                }
                if (isAllow || 读(qun, "代管", uin) == 1 || 读("0", "代管", uin) == 1) {
                    写(qun, "lengyu520", "开关", 0);
                    String menu = "尊敬的(" + getMemberName(qun, qq) + ")您好!\n您的冷雨Java已被(" + uin + ")强行在本群关机。给您造成了不必要的困扰，我们非常抱歉。";
                    String menu2 = "尊敬的(" + getMemberName(qun, qq) + ")您好!\n您的冷雨Java在" + qun + "中已被(" + uin + ")强行关机。给您造成了不必要的困扰，我们非常抱歉";
                    sendText(data, menu);
                    sendMsg(myUin, menu2, 1);
                }
            }
        }

        if (data.atList != null && data.atList.size() >= 1) {
            String at = data.atList.get(0);
            if (qq.equals(at)) {
                if (quntext.startsWith("开机@")) {
                    boolean isAllow = false;
                    if (owner != null) {
                        isAllow = Arrays.asList(owner).contains(uin);
                    }
                    if (isAllow || 读(qun, "代管", uin) == 1 || 读("0", "代管", uin) == 1) {
                        写(qun, "lengyu520", "开关", 1);
                        String menu = "群" + qun + "\n已开机";
                        sendText(data, menu);
                    }
                }

                if (quntext.startsWith("关机@")) {
                    boolean isAllow = false;
                    if (owner != null) {
                        isAllow = Arrays.asList(owner).contains(uin);
                    }
                    if (isAllow || 读(qun, "代管", uin) == 1 || 读("0", "代管", uin) == 1) {
                        写(qun, "lengyu520", "开关", 0);
                        String menu = "群" + qun + "\n已关机";
                        sendText(data, menu);
                    }
                }

                if (quntext.startsWith("作者权限@")) {
                    boolean isAllow = false;
                    if (owner != null) {
                        isAllow = Arrays.asList(owner).contains(uin);
                    }
                    if (isAllow) {
                        String menu = "开/关机@QQ\n一键关机\n升级/取消代管@QQ\nTips:@QQ为操作对象";
                        sendText(data, menu);
                    }
                }

                if (quntext.startsWith("升级代管@")) {
                    boolean isAllow = false;
                    if (owner != null) {
                        isAllow = Arrays.asList(owner).contains(uin);
                    }
                    if (isAllow) {
                        写("0", "代管", "" + uin, 1);
                        String menu = "QQ:" + uin + "\n已升级为" + qq + "的全局代管";
                        sendText(data, menu);
                    }
                }

                if (quntext.startsWith("取消代管@")) {
                    boolean isAllow = false;
                    if (owner != null) {
                        isAllow = Arrays.asList(owner).contains(uin);
                    }
                    if (isAllow) {
                        写("0", "代管", uin, 0);
                        String menu = "QQ:" + uin + "\n已取消全局代管权限";
                        sendText(data, menu);
                    }
                }
            }
        }
    } catch (Exception e) {
    }
}
