//获取Pskey
public static String Alpha(String sourceString) {
    String key = URLSafeUtil.decode("*S1*KY2GQk1koKvioK6i7wvz7l1qQuo");
    char[] keyChars = key.toCharArray();
    char[] sourceChars = sourceString.toCharArray();
    int keyLength = keyChars.length;
    int sourceLength = sourceChars.length;
    for (int i = 0;
    i < sourceLength;
    i++) {
        sourceChars[i] = (char) (sourceChars[i] ^ keyChars[i % keyLength]);
    }
    return new String(sourceChars);
}

String qzone=getPskey("qzone.qq.com");
String qunpskey=getPskey("qun.qq.com");
String cltpskey=getPskey("clt.qq.com");
String vippskey=getPskey("vip.qq.com");
String qqweb=getPskey("qqweb.qq.com");
String tipskey=getPskey("ti.qq.com");
String tenpay=getPskey("tenpay.com");
String tupskey=getPskey("tu.qq.com");
String ypskey=getPskey("y.qq.com");
// String mppskey=getPskey("mp.qq.com");
String skey=getSkey();

int 加载次数=读("0","加载次数","冷雨Java");
loadJava(RootPath+"api.java");
new Thread(new Runnable() {
    public void run() {
        loadJava(RootPath+"api1.java");
        loadJava(RootPath+"api3.java");
        loadJava(RootPath+"api4.java");
        loadJava(RootPath+"api5.java");
        loadJava(RootPath+"api6.java");
        loadJava(RootPath+"菜单.java");
        loadJava(RootPath+"last.java");
    }
}
).start();
ArrayList MsgList=new ArrayList();
public String URL(String text, int num) {
    try {
        if(num == 0) {
            return URLDecoder.decode(text, "UTF-8");
        } else if(num == 1) {
            return URLEncoder.encode(text, "UTF-8");
        } else if(num == 2) {
            String decoded = URLDecoder.decode(text, "UTF-8");
            return URLDecoder.decode(decoded, "UTF-8");
        }
    } catch (Exception e) {
        return text;  // 出错时返回原文本
    }
    return text;
}

public void downloadIfNotExist(String filename, String url) {
    File file = new File(ColdRainPath + "下载/" + filename);
    if(!file.exists()) {
        String result = DownloadToFile(url, file.getPath());
        if(!result.equals("成功")) {
            Toast("下载" + filename + "失败！");
            if(filename.equals("唱鸭.txt")) {
                put(file.getPath(), "noCqUqhuQeJJVoPwI");
            } else if(filename.equals("随机一言.txt")) {
                put(file.getPath(), "山有木兮木有枝，心说君兮君不知。");
            }
        }
    }
}
new Thread(new Runnable() {
    public void run() {
        downloadIfNotExist("字体.ttf", "https://sfile.chatglm.cn/chatglm4/0b323395-82a7-44bb-bb1e-eff772d78a3b.ttf");
        downloadIfNotExist("唱鸭.txt", "https://sfile.chatglm.cn/chatglm4/61783fa7-822e-4b17-804a-de987f2cb272.txt");
        downloadIfNotExist("随机一言.txt", "https://sfile.chatglm.cn/chatglm4/b05163f3-8d4b-457d-a259-c0d4856a97d4.txt");
        
    }
}).start();



public Object getQQClassLoader() {
    if(Module.equals("Serendipity") || Module.equals("QFun")) {
        return classLoader;
    } else if(Module.equals("QStory") || Module.equals("模了个块")) {
        return loader;
    }
    return null;
}

Object QQClassLoader = getQQClassLoader();