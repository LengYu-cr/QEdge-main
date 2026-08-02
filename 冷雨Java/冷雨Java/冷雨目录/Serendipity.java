public String getLocalPath() {
    return Environment.getExternalStorageDirectory().getPath()+"/";
}
public void sendReply(Object data,String menu) {
    sendReplyMsg(data.peerUid,data.msgId,menu,data.type);
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
import android.app.Activity;
Activity activity=getNowActivity();
String LocalPath=getLocalPath();
String RootPath = pluginPath+"冷雨目录/".replace("//","");
String QQPath=LocalPath+"Android/data/"+QQ_PackageName+"/Serendipity/plugin/";
String SerendipityPath=LocalPath+"Android/data/"+QQ_PackageName+"/Serendipity/";
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
public void lengyu520(int type, String qun, String Name)
{
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
public void wyzz(int type,String qun,String Name) {
    payList();
}
public void music(int type,String qun,String Name) {
    searchMusic(qun,type);
}
public void tzjb(int type,String qun,String Name) {
    本地脚本();
}
public void kgsz(int type,String qun,String Name) {
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
    Activity ThisActivity = getNowActivity();
    ThisActivity.runOnUiThread(new Runnable() {
        public void run() {
            try {
                Dialog dialog = new Dialog(ThisActivity);
                ScrollView mScrollView = new ScrollView(ThisActivity);
                LinearLayout l2 = new LinearLayout(ThisActivity);
                l2.setOrientation(LinearLayout.VERTICAL);
                final LinearLayout l5 = new LinearLayout(ThisActivity);
                l5.setOrientation(LinearLayout.VERTICAL);
                ProgressBar yq = new ProgressBar(ThisActivity);
                
                l2.setBackground(getShape("#FFFFFF", "#E3F2FD", 0, 20, 255, true));
                mScrollView.addView(l5);
                mScrollView.setPadding(10, 10, 10, 10);
                
                TextView tt = new TextView(ThisActivity);
                tt.setText("⚙️ 功能开关设置");
                tt.setTextColor(Color.parseColor("#7B1FA2"));
                tt.setGravity(Gravity.CENTER_HORIZONTAL);
                tt.setTextSize(22);
                tt.setTypeface(null, Typeface.BOLD);
                
                final TextView statsText = new TextView(ThisActivity);
                statsText.setText("正在加载功能列表...");
                statsText.setTextColor(Color.parseColor("#666666"));
                statsText.setGravity(Gravity.CENTER_HORIZONTAL);
                statsText.setTextSize(12);
                statsText.setPadding(0, 0, 0, 10);
                
                LinearLayout quickActionLayout = new LinearLayout(ThisActivity);
                quickActionLayout.setOrientation(LinearLayout.HORIZONTAL);
                quickActionLayout.setGravity(Gravity.CENTER_HORIZONTAL);
                quickActionLayout.setPadding(0, 10, 0, 10);
                Button loadAllBtn = new Button(ThisActivity);
                loadAllBtn.setText("🔓 一键开启全部");
                loadAllBtn.setTextColor(Color.parseColor("#FFFFFF"));
                loadAllBtn.setBackground(getShape("#4CAF50", "#388E3C", 0, 15, 255, false));
                loadAllBtn.setPadding(20, 10, 20, 10);
                loadAllBtn.setTextSize(14);
                Button autoAllBtn = new Button(ThisActivity);
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
                l2.addView(tt);
                l2.addView(statsText);
                l2.addView(quickActionLayout);
                l2.addView(mScrollView);
                l5.addView(yq);
                
                final List switchList = new ArrayList();
                new Thread(new Runnable() {
                    public void run() {
                        getFunctionList();
                        final int functionCount = functionList.size();
                        ThisActivity.runOnUiThread(new Runnable() {
                            public void run() {
                                yq.setVisibility(View.GONE);
                                statsText.setText("共 " + functionCount + " 个功能(可群内发送\"开启/关闭+功能名\")");
                            }
                        }
                        );
                        
                        for(int i = 0;
                        i < functionCount;
                        i++) {
                            final int k = i;
                            
                            try {
                                Thread.sleep(50);
                                
                            }
                            catch (Exception e) {
                            }
                            ThisActivity.runOnUiThread(new Runnable() {
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
                                        if (funcName.equals("触发回复")) {
                                            isEnabled = currentState == 0;
                                        }
                                        else {
                                            if (currentState == 1) {
                                                isEnabled = true;
                                            }
                                            else if (currentState == -1) {
                                                isEnabled = defaultValue.equals("1");
                                            }
                                            else {
                                                isEnabled = false;
                                            }
                                        }
                                        
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
                                        
                                        switchList.add(switchBtn);
                                        
                                        switchBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                                            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                                                try {
                                                    if (funcName.equals("触发回复")) {
                                                        写(storageKey, funcKey, "开关", isChecked ? 0 : 1);
                                                    }
                                                    else {
                                                        写(storageKey, funcKey, "开关", isChecked ? 1 : 0);
                                                    }
                                                    
                                                    if (isChecked) {
                                                        switchBtn.setText("开启");
                                                        switchBtn.setTextColor(Color.parseColor("#4CAF50"));
                                                    }
                                                    else {
                                                        switchBtn.setText("关闭");
                                                        switchBtn.setTextColor(Color.parseColor("#9E9E9E"));
                                                    }
                                                    Toast((isChecked ? "已开启: " : "已关闭: ") + funcName);
                                                }
                                                catch (Exception e) {
                                                    Toast("操作失败: " + e.getMessage());
                                                }
                                            }
                                        }
                                        );
                                        titleRow.addView(nameText);
                                        titleRow.addView(switchBtn);
                                        
                                        TextView descText = new TextView(ThisActivity);
                                        descText.setText("📝 " + funcDesc);
                                        descText.setTextColor(Color.parseColor("#666666"));
                                        descText.setTextSize(12);
                                        descText.setLineSpacing(1.2f, 1.2f);
                                        descText.setPadding(0, 8, 0, 0);
                                        funcCard.addView(titleRow);
                                        funcCard.addView(descText);
                                        l5.addView(funcCard);
                                    }
                                    catch (Exception e) {
                                        Toast("添加功能卡片失败: " + e.getMessage());
                                    }
                                }
                            }
                            );
                        }
                    }
                }
                ).start();
                
                loadAllBtn.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        new Thread(new Runnable() {
                            public void run() {
                                try {
                                    int successCount = 0;
                                    for (int i = 0;
                                    i < functionList.size();
                                    i++) {
                                        List func = (List) functionList.get(i);
                                        String storageKey = getScopes((String) func.get(3), qun);
                                        if (func.get(0).equals("触发回复")) {
                                            写(storageKey, (String) func.get(1), "开关", 0);
                                        }
                                        else {
                                            写(storageKey, (String) func.get(1), "开关", 1);
                                        }
                                        successCount++;
                                    }
                                    final int count = successCount;
                                    ThisActivity.runOnUiThread(new Runnable() {
                                        public void run() {
                                            
                                            for(int i = 0;
                                            i < switchList.size();
                                            i++) {
                                                Switch switchBtn = (Switch) switchList.get(i);
                                                if (switchBtn != null) {
                                                    switchBtn.setChecked(true);
                                                    switchBtn.setText("开启");
                                                    switchBtn.setTextColor(Color.parseColor("#4CAF50"));
                                                }
                                            }
                                            Toast("已开启 " + count + " 个功能");
                                        }
                                    }
                                    );
                                }
                                catch (Exception e) {
                                    Toast("开启失败: " + e.getMessage());
                                }
                            }
                        }
                        ).start();
                    }
                }
                );
                
                autoAllBtn.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        new Thread(new Runnable() {
                            public void run() {
                                try {
                                    int successCount = 0;
                                    for (int i = 0;
                                    i < functionList.size();
                                    i++) {
                                        List func = (List) functionList.get(i);
                                        String storageKey = getScopes((String) func.get(3), qun);
                                        if (func.get(0).equals("触发回复")) {
                                            写(storageKey, (String) func.get(1), "开关", 1);
                                        }
                                        else {
                                            写(storageKey, (String) func.get(1), "开关", 0);
                                        }
                                        successCount++;
                                    }
                                    final int count = successCount;
                                    ThisActivity.runOnUiThread(new Runnable() {
                                        public void run() {
                                            
                                            for(int i = 0;
                                            i < switchList.size();
                                            i++) {
                                                Switch switchBtn = (Switch) switchList.get(i);
                                                if (switchBtn != null) {
                                                    switchBtn.setChecked(false);
                                                    switchBtn.setText("关闭");
                                                    switchBtn.setTextColor(Color.parseColor("#9E9E9E"));
                                                }
                                            }
                                            Toast("已关闭 " + count + " 个功能");
                                        }
                                    }
                                    );
                                }
                                catch (Exception e) {
                                    Toast("关闭失败: " + e.getMessage());
                                }
                            }
                        }
                        ).start();
                    }
                }
                );
                
                Button closeBtn = new Button(ThisActivity);
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
                closeParams.setMargins(0, 15, 0, 0);
                closeBtn.setLayoutParams(closeParams);
                closeBtn.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        dialog.dismiss();
                    }
                }
                );
                l2.addView(closeBtn);
                dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                dialog.setContentView(l2);
                dialog.setCancelable(true);
                
                WindowManager wm = (WindowManager) ThisActivity.getSystemService(Context.WINDOW_SERVICE);
                int height = wm.getDefaultDisplay().getHeight();
                int width = wm.getDefaultDisplay().getWidth();
                height = (int)(height * 0.85);
                width = (int)(width * 0.92);
                WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
                layoutParams.copyFrom(dialog.getWindow().getAttributes());
                layoutParams.width = width;
                layoutParams.height = height;
                dialog.getWindow().setAttributes(layoutParams);
                dialog.show();
            }
            catch (Exception e) {
                Toast("对话框创建失败: " + e.getMessage());
            }
        }
    }
    );
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