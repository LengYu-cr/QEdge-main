Activity activity=getActivity();
public void loadJava(String str) {
    load(str);
}
public String readprop(String file, String name2)
{
    String PropFile = file;
    String text = 读(PropFile);
    Properties props = new Properties();
    props.load(new StringReader(text));
    String name = props.getProperty(name2);
    return name;
}
public void sendCard(String str,String str2,int type) {
    if(type==1)sendCard("",str,str2);
    else if(type==2)sendCard(str,"",str2);
}
public void sendMsg(String str,String str2,int type) {
    String str2=str2.replace("atUin=","AtQQ=").replace("pic=","PicUrl=");
    if(type==1)sendMsg("",str,str2);
    else if(type==2)sendMsg(str,"",str2);
}
public void sendPtt(String str,String str2,int type) {
    if(type==1)sendVoice("",str,str2);
    else if(type==2)sendVoice(str,"",str2);
}
public void sendVideo(String str,String str2,int type) {
    if(type==1)sendVideo("",str,str2);
    else if(type==2)sendVideo(str,"",str2);
}
public void sendPic(String str,String str2,int type) {
    if(type==1)sendPic("",str,str2);
    else if(type==2)sendPic(str,"",str2);
}
public Toast(String str) {
    toast(str);
}
public void sendFile(String qun,String text,int mtype) {
    //发送文件
    String qr="";
    if(mtype==1) qr=getUidFromUin(qun);
    else if(mtype==2) qr=qun;
    Contact contact = new Contact(mtype, qr, "");
    MsgElement msgElement=QRoute.api(IMsgUtilApi.class).createFileElement(text);
    ArrayList msgList=new ArrayList();
    msgList.add(msgElement);
    //IOperateCallback iOperateCallback=new IOperateCallback();
    ((IMsgService) QRoute.api(IMsgService.class)).sendMsg(contact,msgList,null);
}
public void recallMsg(int mtype,String qun,List list) {
    //撤回消息
    String qr="";
    if(mtype==1) qr=getUidFromUin(qun);
    else if(mtype==2) qr=qun;
    for(long msgid:list) {
        Contact contact = new Contact(mtype, qr, "");
        ((IMsgService) QRoute.api(IMsgService.class)).recallMsg(contact,msgid,null);
        //撤回消息
    }
}
public void sendReply(Object data,String menu) {
    sendReply(data.GroupUin,data,menu);
}
public Activity getNowActivity() {
    return getActivity();
}
public void shutUp(String str,String str2,int i) {
    forbidden(str,str2,i);
}
import Java.lang.*;
import com.tencent.mobileqq.app.CardHandler;
import com.tencent.common.app.BaseApplicationImpl;
public void sendZan(String targetUin,int num) {
    Object app=BaseApplicationImpl.getApplication().getRuntime();
    int type = 10;
    if(isFriend(targetUin))type=1;
    CardHandler handler= new CardHandler(app);
    byte[] bArr = new byte[10];
    bArr[0] = (byte) 12;
    bArr[1] = (byte) 24;
    bArr[2] = (byte) 0;
    bArr[3] = (byte) 1;
    bArr[4] = (byte) 6;
    bArr[5] = (byte) 1;
    bArr[6] = (byte) 49;
    bArr[7] = (byte) 22;
    bArr[8] = (byte) 1;
    bArr[9] = (byte) 49;
    //handler.I3(Long.parseLong(myUin),Long.parseLong("3560868373"),bArr,1,10,0);
    Class clazz = CardHandler.class;
    Class[] ParamTYPE = new Class[] {
        Long.TYPE,Long.TYPE,byte[].class,Integer.TYPE,Integer.TYPE,Integer.TYPE
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
                // 调用匹配的方法
                m.setAccessible(true);
                m.invoke(handler,new Object[] {
                    Long.parseLong(myUin),Long.parseLong(targetUin),bArr,type,num,0
                }
                );
                break;
                // 找到匹配的方法后，跳出循环
            }
        }
    }
}
public void setGroupMemberTitle(String str,String str2,String str3) {
    setTitle(str,str2,str3);
}
public void kickGroup(String str,String str2,boolean z) {
    kick(str,str2,z);
}
public void shutUpAll(String qun,boolean z) {
    if(z)forbidden(qun,"",1);
    else forbidden(qun,"",0);
}
public void shutUpAllFalse(String qun) {
    forbidden(qun,"",2);
}
import com.tencent.mobileqq.troop.clockin.handler.TroopClockInHandler;
public void groupClockIn(String qun,String uin) {
    TroopClockInHandler inHandler;
    try {
        inHandler = new TroopClockInHandler(app);
    }
    catch(e) {
        inHandler = new TroopClockInHandler();
    }
    Method method;
    Class clazz = TroopClockInHandler.class;
    Class[] ParamTYPE = new Class[] {
        String.class,String.class,int.class,boolean.class
    };
    Method[] methods = clazz.getDeclaredMethods();
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
                // 调用匹配的方法
                m.setAccessible(true);
                m.invoke(inHandler,new Object[] {
                    qun,uin,0,true
                }
                );
                break;
                // 找到匹配的方法后，跳出循环
            }
        }
    }
}
public long getBkn(String sskey)
{
    int hash = 5381;
    for(int i = 0, len = sskey.length();
    i < len;
    i++) hash += (hash << 5) + (int)(char) sskey.charAt(i);
    return hash & 2147483647;
}

String NETWORK_LINE = "https://v.yuafeng.cn/";
public boolean changeNetworkLine(int type){
    String NetworkLine = "https://v.yuafeng.cn/";// 默认
    if(type == 0||type == 1){
         NetworkLine = "https://v.yuafeng.cn/";
    }
    
    else if(type == 2){
         NetworkLine = "https://api.yuafeng.cn/ly/";
    }
    
    String result = get(NetworkLine+"plugin/isReachable/");
    
    if(result.equals("true")){
         写("0","冷雨Java","线路",NetworkLine);
         NETWORK_LINE = NetworkLine;
         return true;
    }
    
     return false;
    
}

public void getNetworkLine(){
    String NetworkLine = 文字("0","冷雨Java","线路");
    if(NetworkLine == null || NetworkLine.equals("")){
        NetworkLine = "https://v.yuafeng.cn/";// 默认
    }
    NETWORK_LINE = NetworkLine;
    
}

String pluginPath=appPath+"/";
public String getLocalPath() {
    return Environment.getExternalStorageDirectory().getPath()+"/";
}
String LocalPath=getLocalPath();
String RootPath = pluginPath+"冷雨目录/".replace("//","");
String QQPath=LocalPath+"Android/data/"+QQ_PackageName+"/QStory/Plugin/";
String ColdRainPath=LocalPath+"Android/media/"+QQ_PackageName+"/冷雨Java/";
//冷雨Java数据储存目录
String Version="";
//当前版本
String reallVersion="";
reallVersion=readprop(pluginPath+"info.prop","versionName");
Version=readprop(pluginPath+"info.prop","versionName").substring(0,5);
getNetworkLine();
new Thread(new Runnable() {
    public void run() {
        loadJava(RootPath+"first.java");
    }
}
).start();
//int pluginSdk=83;替换为 ModuleAPICode
public void lengyu520(String qun)
{
    int type=getChatType();
    if(读(qun, "lengyu520", "开关") == 1)
    {
        写(qun, "lengyu520", "开关", 0);
        Toast("本聊天冷雨Java已关机～");
    }
    else
    {
        写(qun, "lengyu520", "开关", 1);
        Toast("本聊天冷雨Java已开机～");
    }
}
public void wyzz(String qun) {
    payList();
}
public void music(String qun) {
    searchMusic(qun,getChatType());
}
public void tzjb(String qun) {
    本地脚本();
}

public void kgsz(String qun) {
    int type=getChatType();
    kgsz(qun,type);
}


List functionList = new ArrayList();
public void getFunctionList() {
    functionList.clear();
    try {
        String jsonStr = 读(RootPath + "Cache/functions.json");
        JSONObject jsonObject = new JSONObject(jsonStr);
        JSONArray functionsArray = jsonObject.getJSONArray("functions");
        if(functionsArray.length() <= 0){
            getFunctionList2();
            return;
        }
        for (int i = 0;
        i < functionsArray.length();
        i++) {
            JSONObject funcObj = functionsArray.getJSONObject(i);
            List func = new ArrayList();
            func.add(funcObj.getString("name"));
            func.add(funcObj.getString("key"));
            func.add(funcObj.getString("desc"));
            func.add(funcObj.getString("scope"));
            func.add(funcObj.getString("default"));
            functionList.add(func);
        }
    
    }catch (Exception e) {
        Toast("读取功能列表失败: " + e.getMessage() + "\n开始尝试第二次");
        getFunctionList2();
    }
    
}

public void getFunctionList2() {
new Thread(new Runnable() {
    public void run() {
    functionList.clear();
    try {
       String function_list = get(myWang + "java/?action=function_list");
        if(!function_list.equals("")&&!function_list.equals("访问网页失败")){
        put(RootPath + "Cache/functions.json", function_list);
        JSONObject jsonObject = new JSONObject(function_list);
        JSONArray functionsArray = jsonObject.getJSONArray("functions");
        for (int i = 0;
        i < functionsArray.length();
        i++) {
            JSONObject funcObj = functionsArray.getJSONObject(i);
            List func = new ArrayList();
            func.add(funcObj.getString("name"));
            func.add(funcObj.getString("key"));
            func.add(funcObj.getString("desc"));
            func.add(funcObj.getString("scope"));
            func.add(funcObj.getString("default"));
            functionList.add(func);
        }
       }else{
           Toast("艾特作图缓存获取失败");
       }
    }catch (Exception e) {
        Toast("请求网络缓存: 读取功能列表失败: " + e.getMessage());
    }
    }
}).start();
}
public void kgsz(String qun, int mtype) {
    final Activity ThisActivity = getNowActivity();
    ThisActivity.runOnUiThread(new Runnable() {
        public void run() {
            try {
                // 1. 先创建整体布局
                final LinearLayout l2 = new LinearLayout(ThisActivity);
                l2.setOrientation(LinearLayout.VERTICAL);
                l2.setBackground(getShape("#FFFFFF", "#E3F2FD", 0, 20, 255, true));

                // 标题
                TextView tt = new TextView(ThisActivity);
                tt.setText("⚙️ 功能开关设置");
                tt.setTextColor(Color.parseColor("#7B1FA2"));
                tt.setGravity(Gravity.CENTER_HORIZONTAL);
                tt.setTextSize(22);
                tt.setTypeface(null, Typeface.BOLD);
                tt.setPadding(0, 20, 0, 10);
                l2.addView(tt);

                // 状态文本
                final TextView statsText = new TextView(ThisActivity);
                statsText.setText("正在加载功能列表...");
                statsText.setTextColor(Color.parseColor("#666666"));
                statsText.setGravity(Gravity.CENTER_HORIZONTAL);
                statsText.setTextSize(12);
                statsText.setPadding(0, 0, 0, 10);
                l2.addView(statsText);

                // 一键按钮行
                LinearLayout quickActionLayout = new LinearLayout(ThisActivity);
                quickActionLayout.setOrientation(LinearLayout.HORIZONTAL);
                quickActionLayout.setGravity(Gravity.CENTER_HORIZONTAL);
                quickActionLayout.setPadding(10, 0, 10, 10);

                final Button loadAllBtn = new Button(ThisActivity);
                loadAllBtn.setText("🔓 一键开启全部");
                loadAllBtn.setTextColor(Color.parseColor("#FFFFFF"));
                loadAllBtn.setBackground(getShape("#4CAF50", "#388E3C", 0, 15, 255, false));
                loadAllBtn.setPadding(20, 10, 20, 10);
                loadAllBtn.setTextSize(14);

                final Button autoAllBtn = new Button(ThisActivity);
                autoAllBtn.setText("🔒 一键关闭全部");
                autoAllBtn.setTextColor(Color.parseColor("#FFFFFF"));
                autoAllBtn.setBackground(getShape("#F44336", "#D32F2F", 0, 15, 255, false));
                autoAllBtn.setPadding(20, 10, 20, 10);
                autoAllBtn.setTextSize(14);

                LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f
                );
                btnParams.setMargins(5, 0, 5, 0);
                loadAllBtn.setLayoutParams(btnParams);
                autoAllBtn.setLayoutParams(btnParams);
                quickActionLayout.addView(loadAllBtn);
                quickActionLayout.addView(autoAllBtn);
                l2.addView(quickActionLayout);

                // 滚动容器
                final ScrollView mScrollView = new ScrollView(ThisActivity);
                mScrollView.setPadding(10, 10, 10, 10);
                final LinearLayout l5 = new LinearLayout(ThisActivity);
                l5.setOrientation(LinearLayout.VERTICAL);
                mScrollView.addView(l5);
                l2.addView(mScrollView);

                // 加载圈
                final ProgressBar yq = new ProgressBar(ThisActivity);
                l5.addView(yq);

                // 关闭按钮
                final Button closeBtn = new Button(ThisActivity);
                closeBtn.setText("关闭");
                closeBtn.setTextColor(Color.parseColor("#666666"));
                closeBtn.setBackground(getShape("#F5F5F5", "#E0E0E0", 1, 15, 255, false));
                closeBtn.setPadding(40, 12, 40, 12);
                closeBtn.setTextSize(14);
                LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                );
                closeParams.gravity = Gravity.CENTER_HORIZONTAL;
                closeParams.setMargins(0, 15, 0, 15);
                closeBtn.setLayoutParams(closeParams);
                l2.addView(closeBtn);

                // 2. 创建并显示Dialog
                final Dialog dialog = new Dialog(ThisActivity);
                dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                dialog.setContentView(l2);
                dialog.setCancelable(true);

                WindowManager wm = (WindowManager) ThisActivity.getSystemService(Context.WINDOW_SERVICE);
                int height = (int) (wm.getDefaultDisplay().getHeight() * 0.85);
                int width = (int) (wm.getDefaultDisplay().getWidth() * 0.92);
                WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
                layoutParams.copyFrom(dialog.getWindow().getAttributes());
                layoutParams.width = width;
                layoutParams.height = height;
                dialog.getWindow().setAttributes(layoutParams);
                dialog.show();

                // 关闭按钮点击
                closeBtn.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        dialog.dismiss();
                    }
                });

                // 3. 子线程加载数据并逐个添加View
                final List switchList = new ArrayList();
                new Thread(new Runnable() {
                    public void run() {
                        getFunctionList();
                        final int functionCount = functionList.size();

                        // 更新状态文本
                        l5.post(new Runnable() {
                            public void run() {
                                yq.setVisibility(View.GONE);
                                statsText.setText("共 " + functionCount + " 个功能(可群内发送\"开启/关闭+功能名\")");
                            }
                        });

                        // 逐个添加功能卡片
                        for (int i = 0; i < functionCount; i++) {
                            final int k = i;
                            l5.post(new Runnable() {
                                public void run() {
                                    try {
                                        List func = (List) functionList.get(k);
                                        final String funcName = (String) func.get(0);
                                        final String funcKey = (String) func.get(1);
                                        final String funcDesc = (String) func.get(2);
                                        final String scope = (String) func.get(3);
                                        final String defaultValue = (String) func.get(4);
                                        final String storageKey = getScopes(scope, qun);

                                        int currentState = 读(storageKey, funcKey, "开关");
                                        boolean isEnabled = false;
                                        if (currentState == 1) {
                                            isEnabled = true;
                                        } else if (currentState == -1) {
                                            isEnabled = defaultValue.equals("1");
                                        } else {
                                            isEnabled = false;
                                        }

                                        // 创建功能卡片
                                        LinearLayout funcCard = new LinearLayout(ThisActivity);
                                        funcCard.setOrientation(LinearLayout.VERTICAL);
                                        funcCard.setPadding(15, 12, 15, 12);
                                        funcCard.setBackground(getShape("#FFFFFF", "#EDE7F6", 1, 12, 255, false));
                                        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                                            LinearLayout.LayoutParams.MATCH_PARENT,
                                            LinearLayout.LayoutParams.WRAP_CONTENT
                                        );
                                        cardParams.setMargins(0, 0, 0, 8);
                                        funcCard.setLayoutParams(cardParams);

                                        // 标题行
                                        LinearLayout titleRow = new LinearLayout(ThisActivity);
                                        titleRow.setOrientation(LinearLayout.HORIZONTAL);
                                        titleRow.setGravity(Gravity.CENTER_VERTICAL);

                                        TextView nameText = new TextView(ThisActivity);
                                        nameText.setText(funcName);
                                        nameText.setTextColor(Color.parseColor("#7B1FA2"));
                                        nameText.setTextSize(16);
                                        nameText.setTypeface(null, Typeface.BOLD);
                                        nameText.setLayoutParams(new LinearLayout.LayoutParams(
                                            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f
                                        ));

                                        final Switch switchBtn = new Switch(ThisActivity);
                                        switchBtn.setChecked(isEnabled);
                                        switchBtn.setText(isEnabled ? "开启" : "关闭");
                                        switchBtn.setTextSize(12);
                                        switchBtn.setTextColor(isEnabled ? Color.parseColor("#4CAF50") : Color.parseColor("#9E9E9E"));
                                        switchBtn.setPadding(10, 0, 0, 0);

                                        // 用Tag绑定数据
                                        Object[] tagData = new Object[3];
                                        tagData[0] = storageKey;
                                        tagData[1] = funcKey;
                                        tagData[2] = funcName;
                                        switchBtn.setTag(tagData);
                                        switchList.add(switchBtn);

                                        switchBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                                            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                                                try {
                                                    Object[] data = (Object[]) buttonView.getTag();
                                                    String sk = (String) data[0];
                                                    String fk = (String) data[1];
                                                    String fn = (String) data[2];

                                                    写(sk, fk, "开关", isChecked ? 1 : 0);

                                                    if (isChecked) {
                                                        switchBtn.setText("开启");
                                                        switchBtn.setTextColor(Color.parseColor("#4CAF50"));
                                                    } else {
                                                        switchBtn.setText("关闭");
                                                        switchBtn.setTextColor(Color.parseColor("#9E9E9E"));
                                                    }
                                                    Toast((isChecked ? "已开启: " : "已关闭: ") + fn);
                                                } catch (Exception e) {
                                                    Toast("操作失败: " + e.getMessage());
                                                }
                                            }
                                        });

                                        titleRow.addView(nameText);
                                        titleRow.addView(switchBtn);

                                        // 描述文本
                                        TextView descText = new TextView(ThisActivity);
                                        descText.setText("📝 " + funcDesc);
                                        descText.setTextColor(Color.parseColor("#666666"));
                                        descText.setTextSize(12);
                                        descText.setLineSpacing(1.2f, 1.2f);
                                        descText.setPadding(0, 8, 0, 0);

                                        funcCard.addView(titleRow);
                                        funcCard.addView(descText);
                                        l5.addView(funcCard);

                                    } catch (Exception e) {
                                        Toast("添加功能卡片失败: " + e.getMessage());
                                    }
                                }
                            });

                            // 控制显示速度
                            try {
                                Thread.sleep(120);
                            } catch (Exception e) {}
                        }

                        // 一键开启全部
                        loadAllBtn.setOnClickListener(new View.OnClickListener() {
                            public void onClick(View v) {
                                new Thread(new Runnable() {
                                    public void run() {
                                        try {
                                            int successCount = 0;
                                            for (int i = 0; i < functionList.size(); i++) {
                                                List func = (List) functionList.get(i);
                                                String storageKey = getScopes((String) func.get(3), qun);
                                                写(storageKey, (String) func.get(1), "开关", 1);
                                                successCount++;
                                            }
                                            final int count = successCount;
                                            ThisActivity.runOnUiThread(new Runnable() {
                                                public void run() {
                                                    for (int i = 0; i < switchList.size(); i++) {
                                                        Switch switchBtn = (Switch) switchList.get(i);
                                                        if (switchBtn != null) {
                                                            switchBtn.setChecked(true);
                                                            switchBtn.setText("开启");
                                                            switchBtn.setTextColor(Color.parseColor("#4CAF50"));
                                                        }
                                                    }
                                                    Toast("已开启 " + count + " 个功能");
                                                }
                                            });
                                        } catch (Exception e) {
                                            Toast("开启失败: " + e.getMessage());
                                        }
                                    }
                                }).start();
                            }
                        });

                        // 一键关闭全部
                        autoAllBtn.setOnClickListener(new View.OnClickListener() {
                            public void onClick(View v) {
                                new Thread(new Runnable() {
                                    public void run() {
                                        try {
                                            int successCount = 0;
                                            for (int i = 0; i < functionList.size(); i++) {
                                                List func = (List) functionList.get(i);
                                                String storageKey = getScopes((String) func.get(3), qun);
                                                写(storageKey, (String) func.get(1), "开关", 0);
                                                successCount++;
                                            }
                                            final int count = successCount;
                                            ThisActivity.runOnUiThread(new Runnable() {
                                                public void run() {
                                                    for (int i = 0; i < switchList.size(); i++) {
                                                        Switch switchBtn = (Switch) switchList.get(i);
                                                        if (switchBtn != null) {
                                                            switchBtn.setChecked(false);
                                                            switchBtn.setText("关闭");
                                                            switchBtn.setTextColor(Color.parseColor("#9E9E9E"));
                                                        }
                                                    }
                                                    Toast("已关闭 " + count + " 个功能");
                                                }
                                            });
                                        } catch (Exception e) {
                                            Toast("关闭失败: " + e.getMessage());
                                        }
                                    }
                                }).start();
                            }
                        });
                    }
                }).start();

            } catch (Exception e) {
                Toast("对话框创建失败: " + e.getMessage());
            }
        }
    });
}

public String getScopes(String scope, String qun) {
    if(scope.equals("global")) {
        return "0";
    }
    if(scope.equals("冷雨Java")) {
        return "冷雨Java";
    }
    return qun;
}

addItem("开关设置","kgsz");