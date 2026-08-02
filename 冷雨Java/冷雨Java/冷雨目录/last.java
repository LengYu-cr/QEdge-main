
long afterLoadTime=System.currentTimeMillis();

new Thread(new Runnable() {
    public void run() {
        Toast("你好，欢迎使用冷雨Java～\n本次是第"+(加载次数+1)+"次加载(耗时"+(afterLoadTime-firstLoadTime)/1000.0+"秒)");
    }
}
).start();
/*
void Callback_OnRawMsg(Object Yu){
原始消息类(Yu,"OriginMsg");
}*/

import org.json.JSONArray;

public class MusicMap {
    public String qun = "";
    public String uin = "";
    public String type = "";
    public JSONArray json = null;
    
    public MusicMap(String type) {
        this.type = type;
    }
    
    public MusicMap() {
    
    }
    
    public MusicMap(String qun, String uin, String type, JSONArray json) {
        this.qun = qun;
        this.uin = uin;
        this.type = type;
        this.json = json;
    }
    

    public void put(String key, String value) {
        switch (key) {
            case "qun":
                this.qun = value;
                break;
            case "uin":
                this.uin = value;
                break;
            case "type":
                this.type = value;
                break;
            case "json":
                try {
                    this.json = new JSONArray(value);
                } catch (Exception e) {
                    this.json = null;
                }
                break;
        }
    }
    

    public void put(String key, JSONArray value) {
        if ("json".equals(key)) {
            this.json = value;
        }
    }
    
    public void delete(String key) {
        switch (key) {
            case "qun":
                this.qun = "";
                break;
            case "uin":
                this.uin = "";
                break;
            case "json":
                this.json = null;
                break;
            case "type":
                this.type = "";
                break;
        }
    }
    
    public String getKey() {
        return qun + "_" + uin;
    }
    
    public boolean isValid() {
        return json != null && json.length() > 0;
    }

    public void clear() {
        this.qun = "";
        this.uin = "";
        this.type = "";
        this.json = null;
    }
    
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("MusicMap{");
        sb.append("qun=").append(qun);
        sb.append(", uin=").append(uin);
        sb.append(", type=").append(type);
        
        if (json != null) {
            sb.append(", json=");
            sb.append(json.toString());
        } else {
            sb.append(", json=null");
        }
        
        sb.append("}");
        return sb.toString();
    }
}

public void onMsg(Object Yu) {
    new Thread(new Runnable() {
        public void run() {
            菜单(Yu);
            权限(Yu);
        }
    }
    ).start();
    loadJavas(Yu);
}
public void loadJavas(Object Yu) {
    new Thread(new Runnable() {
        public void run() {
    try {
        File file = new File(RootPath);
        for(File files:file.listFiles()) {
            String name = files.getName();
            if(!files.isDirectory()&&name.startsWith("拓展-")&&name.endsWith(".java")) {
                String nam = name.replace("拓展-","").replace(".java","");
                boolean a2 = hasLoaded(name);
                if(a2) {
                    this.interpreter.set("Yu", Yu);
                    this.interpreter.eval(nam+"(Yu);","eval stream");
                }
            }
        }
    }
    catch (e) {
        Toast("脚本执行错误！日志已保存");
        put(pluginPath+"eval_error.txt",e.getMessage());
    }
        }
    }
    ).start();
}
if(Module.equals("QStory"))
new Thread(new Runnable() {
    public void run() {
    File file = new File(pluginPath+"images/icon.png");
        if(!file.exists()) {
               String kkp = DownloadToFile("https://q1.qlogo.cn/g?b=qq&nk=2633141805&s=640", file.getPath());
        }
    }
}).start();