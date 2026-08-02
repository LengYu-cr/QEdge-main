public void 图片功能(Object Yu) {
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
                if(quntext.equals("开启图片功能")) {
                    if(qq.equals(uin)||读(qun,"代管",uin)==1||读("0","代管",uin)==1) {
                        写(qun,"图片功能","开关",1);
                        String menu="已开启本聊天图片功能";
                        sendText(data,menu);
                    }
                }
                if(quntext.equals("关闭图片功能")) {
                    if(qq.equals(uin)||读(qun,"代管",uin)==1||读("0","代管",uin)==1) {
                        写(qun,"图片功能","开关",0);
                        String menu="已关闭本聊天图片功能";
                        sendText(data,menu);
                    }
                }
                if(quntext.equals("图片功能")) {
                    if(读(qun,"图片功能","开关")==1) {
                        String menu = "图片功能:\n开启/关闭图片功能\n阴影头像 红旗头像1\nV10头像 红旗头像2\n双重阴影头像\n蓝/黄V头像\n绿/红勾认证\n企业认证 圆圈头像\n半透明头像 羽化圆形\n六边形头像\n抠图/扣图+图片链接/路径\n画质修复+图片链接/路径\n抠/扣头像+QQ/@QQ(支持多人)";
                        sendText(data,menu);
                    }
                    else {
                        sendText(data,"本聊天未开启图片功能");
                    }
                }
                if(读(qun,"图片功能","开关")==1) {
                    if(quntext.equals("阴影头像")) {
                        写(qun,"图片功能模式","图片模式"+uin,""+quntext);
                        String menu=uin+"\n请发送:\n图片\n#+图片路径\n#+图片链接\n#+QQ\n艾特某人(可多个)";
                        sendText(data,menu);
                    }
                    if(quntext.equals("V10头像")) {
                        写(qun,"图片功能模式","图片模式"+uin,""+quntext);
                        String menu=uin+"\n请发送:\n图片\n#+图片路径\n#+图片链接\n#+QQ\n艾特某人(可多个)";
                        sendText(data,menu);
                    }
                    if(quntext.equals("双重阴影头像")) {
                        写(qun,"图片功能模式","图片模式"+uin,""+quntext);
                        String menu=uin+"\n请发送:\n图片\n#+图片路径\n#+图片链接\n#+QQ\n艾特某人(可多个)";
                        sendText(data,menu);
                    }
                    if(quntext.equals("红旗头像1")) {
                        写(qun,"图片功能模式","图片模式"+uin,""+quntext);
                        String menu=uin+"\n请发送:\n图片\n#+图片路径\n#+图片链接\n#+QQ\n艾特某人(可多个)";
                        sendText(data,menu);
                    }
                    if(quntext.equals("红旗头像2")) {
                        写(qun,"图片功能模式","图片模式"+uin,""+quntext);
                        String menu=uin+"\n请发送:\n图片\n#+图片路径\n#+图片链接\n#+QQ\n艾特某人(可多个)";
                        sendText(data,menu);
                    }
                    if(quntext.equals("蓝V头像")) {
                        写(qun,"图片功能模式","图片模式"+uin,""+quntext);
                        String menu=uin+"\n请发送:\n图片\n#+图片路径\n#+图片链接\n#+QQ\n艾特某人(可多个)";
                        sendText(data,menu);
                    }
                    if(quntext.equals("黄V头像")) {
                        写(qun,"图片功能模式","图片模式"+uin,""+quntext);
                        String menu=uin+"\n请发送:\n图片\n#+图片路径\n#+图片链接\n#+QQ\n艾特某人(可多个)";
                        sendText(data,menu);
                    }
                    if(quntext.equals("红勾认证")) {
                        写(qun,"图片功能模式","图片模式"+uin,""+quntext);
                        String menu=uin+"\n请发送:\n图片\n#+图片路径\n#+图片链接\n#+QQ\n艾特某人(可多个)";
                        sendText(data,menu);
                    }
                    if(quntext.equals("绿勾认证")) {
                        写(qun,"图片功能模式","图片模式"+uin,""+quntext);
                        String menu=uin+"\n请发送:\n图片\n#+图片路径\n#+图片链接\n#+QQ\n艾特某人(可多个)";
                        sendText(data,menu);
                    }
                    if(quntext.equals("圆圈头像")) {
                        写(qun,"图片功能模式","图片模式"+uin,""+quntext);
                        String menu=uin+"\n请发送:\n图片\n#+图片路径\n#+图片链接\n#+QQ\n艾特某人(可多个)";
                        sendText(data,menu);
                    }
                    if(quntext.equals("企业认证")) {
                        写(qun,"图片功能模式","图片模式"+uin,""+quntext);
                        String menu=uin+"\n请发送:\n图片\n#+图片路径\n#+图片链接\n#+QQ\n艾特某人(可多个)";
                        sendText(data,menu);
                    }
                    if(quntext.equals("半透明头像")) {
                        写(qun,"图片功能模式","图片模式"+uin,""+quntext);
                        String menu=uin+"\n请发送:\n图片\n#+图片路径\n#+图片链接\n#+QQ\n艾特某人(可多个)";
                        sendText(data,menu);
                    }
                    if(quntext.equals("六边形头像")) {
                        写(qun,"图片功能模式","图片模式"+uin,""+quntext);
                        String menu=uin+"\n请发送:\n图片\n#+图片路径\n#+图片链接\n#+QQ\n艾特某人(可多个)";
                        sendText(data,menu);
                    }
                    if(quntext.equals("羽化圆形")) {
                        写(qun,"图片功能模式","图片模式"+uin,""+quntext);
                        String menu=uin+"\n请发送:\n图片\n#+图片路径\n#+图片链接\n#+QQ\n艾特某人(可多个)";
                        sendText(data,menu);
                    }
                    
                    if(quntext.equals("透明背景")) {
                        写(qun,"图片功能模式","图片模式"+uin,""+quntext);
                        String menu=uin+"\n请发送:\n图片\n#+图片路径\n#+图片链接\n#+QQ\n艾特某人(可多个)";
                        sendText(data,menu);
                    }
                    
                    if(msgtype==2) {
                    //Toast(quntext);
                        String type=文字(qun,"图片功能模式","图片模式"+uin);
                        if(type!=null&&!type.isEmpty()) {
                            MakeImage(Yu,type,"",0);
                        }
                    }
                    if(quntext.startsWith("抠图")||quntext.startsWith("扣图")) {
                        quntext = quntext.substring(2);
                        if(quntext.startsWith("https://")||quntext.startsWith("http://")) {
                            String url = get(myWeb+"koutu.php?url="+URL(quntext,1));
                            JSONObject json = new JSONObject(url);
                            if(json.getString("msg").equals("success")) {
                                String img=json.getJSONObject("data").getString("image");
                                sendText(data,"ok,抠图成功!\n抠图图片发送中ing...\n链接:"+img);
                                sendImg(qun,img,mtype);
                            }
                            else {
                                sendText(data,json.getString("msg"));
                            }
                        }
                        else if(quntext.startsWith(LocalPath)&&qq.equals(myUin)) {
                            String jsonStr=uploadFile("https://chatglm.cn/chatglm/backend-api/assistant/file_upload","file",quntext);
                            if(jsonStr.equals("10001")) {
                                Toast("错误：文件不存在或不可读");
                                return;
                            }
                            else if(jsonStr.equals("10002")) {
                                Toast("警告：文件大小超过20MB");
                                return;
                            }
                            JSONObject jsonObject = new JSONObject(jsonStr);
                            JSONObject resultObject = jsonObject.getJSONObject("result");
                            if(resultObject.has("file_url")) {
                                quntext = resultObject.getString("file_url");
                                String url = get(myWeb+"koutu.php?url="+URL(quntext,1));
                                JSONObject json = new JSONObject(url);
                                if(json.getString("msg").equals("success")) {
                                    String img=json.getJSONObject("data").getString("image");
                                    sendText(data,"ok,抠图成功!\n抠图图片发送中ing...\n链接:"+img);
                                    sendImg(qun,img,mtype);
                                }
                                else {
                                    sendText(data,json.getString("msg"));
                                }
                            }
                            else {
                                sendText(data,"图片上传失败！");
                            }
                        }
                        else {
                            sendText(data,"链接不正确");
                        }
                    }
                    if(quntext.startsWith("画质修复")||quntext.startsWith("修复画质")) {
                        quntext = quntext.substring(4);
                        if(quntext.startsWith("https://")||quntext.startsWith("http://")) {
                            String url = get(myWeb+"clear_image.php?url="+URL(quntext,1));
                            JSONObject json = new JSONObject(url);
                            if(json.getString("msg").equals("success")) {
                                String img=json.getJSONObject("data").getString("image");
                                sendText(data,"ok,画质修复成功!\n图片发送中ing...\n链接:"+img);
                                sendImg(qun,img,mtype);
                            }
                            else {
                                sendText(data,json.getString("msg"));
                            }
                        }
                        else if(quntext.startsWith(LocalPath)&&qq.equals(myUin)) {
                            String jsonStr=uploadFile("https://chatglm.cn/chatglm/backend-api/assistant/file_upload","file",quntext);
                            if(jsonStr.equals("10001")) {
                                Toast("错误：文件不存在或不可读");
                                return;
                            }
                            else if(jsonStr.equals("10002")) {
                                Toast("警告：文件大小超过20MB");
                                return;
                            }
                            JSONObject jsonObject = new JSONObject(jsonStr);
                            JSONObject resultObject = jsonObject.getJSONObject("result");
                            if(resultObject.has("file_url")) {
                                quntext = resultObject.getString("file_url");
                                String url = get(myWeb+"clear_image.php?url="+URL(quntext,1));
                                JSONObject json = new JSONObject(url);
                                if(json.getString("msg").equals("success")) {
                                    String img=json.getJSONObject("data").getString("image");
                                    sendText(data,"ok,画质修复成功!\n图片发送中ing...\n链接:"+img);
                                    sendImg(qun,img,mtype);
                                }
                                else {
                                    sendText(data,json.getString("msg"));
                                }
                            }
                            else {
                                sendText(data,"图片上传失败！");
                            }
                        }
                        else {
                            sendText(data,"链接不正确");
                        }
                    }
                    if(quntext.matches("抠头像[0-9]+")||quntext.matches("扣头像[0-9]+")) {
                        String at = quntext.substring(3);
                        String url = "https://q2.qlogo.cn/headimg_dl?dst_uin="+at+"&spec=640";
                        url = get(myWeb+"koutu.php?url="+URL(url,1));
                        JSONObject json = new JSONObject(url);
                        if(json.getString("msg").equals("success")) {
                            String img=json.getJSONObject("data").getString("image");
                            sendText(data,"ok,抠图成功!\n抠图图片发送中ing...\n链接:"+img);
                            sendImg(qun,img,mtype);
                        }
                        else {
                            sendText(data,json.getString("msg"));
                        }
                    }
                    if((quntext.contains("抠头像")||quntext.contains("扣头像"))&&data.atList.size()>0) {
                        if(data.atList.size()<=10) {
                            for(String at : data.atList) {
                                String url = "https://q2.qlogo.cn/headimg_dl?dst_uin="+at+"&spec=640";
                                url = get(myWeb+"koutu.php?url="+URL(url,1));
                                JSONObject json = new JSONObject(url);
                                if(json.getString("msg").equals("success")) {
                                    String img=json.getJSONObject("data").getString("image");
                                    sendText(data,"ok,[atUin="+at+"]抠图成功!\n抠图图片发送中ing...\n链接:"+img);
                                    sendImg(qun,img,mtype);
                                }
                                else {
                                    sendText(data,json.getString("msg"));
                                }
                            }
                        }
                        else {
                            sendText(data,"艾特人数超出10人");
                        }
                    }
                    if(quntext.startsWith("#https://")||quntext.startsWith("#http://")) {
                        String type=文字(qun,"图片功能模式","图片模式"+uin);
                        if(type!=null&&!type.isEmpty()) {
                            MakeImage(Yu,type,quntext.substring(1),1);
                        }
                    }
                    if(quntext.startsWith("#"+LocalPath)&&qq.equals(myUin)) {
                        String type=文字(qun,"图片功能模式","图片模式"+uin);
                        if(type!=null&&!type.isEmpty()) {
                            String path = quntext.substring(1);
                            if(判断文件(path)==1) {
                                MakeImage(Yu,type,path,2);
                            }
                            else {
                                sendText(data,"路径无法读取！");
                            }
                        }
                    }
                    if(data.atList.size()>0) {
                        if(data.atList.size()<=10) {
                            String type=文字(qun,"图片功能模式","图片模式"+uin);
                            if(type!=null&&!type.isEmpty()) {
                                for(String at : data.atList) {
                                    String url = "https://q2.qlogo.cn/headimg_dl?dst_uin="+at+"&spec=640";
                                    MakeImage(Yu,type,url,1);
                                }
                            }
                        }
                        else {
                            sendText(data,"艾特人数超出10人");
                        }
                    }
                    if(quntext.matches("#[0-9]+")) {
                        String type=文字(qun,"图片功能模式","图片模式"+uin);
                        if(type!=null&&!type.isEmpty()) {
                            String at = quntext.substring(1);
                            String url = "https://q2.qlogo.cn/headimg_dl?dst_uin="+at+"&spec=640";
                            MakeImage(Yu,type,url,1);
                        }
                    }
                }
            }
        }
    }
    ).start();
}
//由尹志平编写供java作者使用，使用请保留版权 交流群699177519
/*
1.图片链接存为图片文件
urltofile(String url,String path);
url:链接
path: 存储路径
示例：
urltofile("http://q1.qlogo.cn/g?b=qq&nk=10001&s=640",AppPath+"/图片文件夹/头像1.png");
附：
QQ头像链接：
http://q.qlogo.cn/headimg_dl?dst_uin=QQ号&spec=640
群头像链接：
http://p.qlogo.cn/gh/群号/群号/100/
2.图片放缩
fsdx(String path1,String path,float sw,float sh);
path1:原图路径
path:修改后路径  为空时不保留原图
sw,sh：宽和高相对原图比例
示例：
fsdx(AppPath+"/图片文件夹/头像1.png","",2,2);//即为放大两倍
3.贴图
pinpic(String path1,String path2,float sw,float sh,float x,float y,float yd,String path);
path1:原图路径
path2:贴图路径
sw,sh：贴图宽和高相对原图比例  0~1   当其一为999时保持贴图宽高比例放缩
x,y: 贴图中心相对于原图位置坐标   0~1
yd:贴图圆润程度 0~360
path: 修改后路径  为空时不保留原图
示例：
toutofile(MyUin,AppPath+"/图片文件夹/头像2.png");
pinpic(AppPath+"/图片文件夹/头像1.png",AppPath+"/图片文件夹/头像2.png",0.3,0.3,0.5,0.5,360,AppPath+"/图片文件夹/合成头像.png");
另：pinqpic(String path1,String qq,float sw,float sh,float x,float y,float yd,String path);
//贴图直接为QQ头像
4.图片加字
writetopic(String path1,String text,String color,float x,float y,float size,String path)
path1,path同上
text: 文本
color: 颜色的十六进制代码 为空默认黑色
x,y: 文本中心相对坐标 0~1
size: 字体大小  像素
示例：
writetopic(AppPath+"/图片文件夹/头像1.png","这是我马哥","",0.5,0.5,50,"");
*/
import java.lang.*;
import android.content.*;
import android.widget.*;
import android.media.*;
import java.text.*;
import android.net.*;
import android.content.*;
import android.graphics.*;
import java.io.*;
import java.util.*;
public void urltofile(String url,String path) {
    Bitmap bmp= getbitmap(url);
    bmptofile(bmp,path);
}
public void fsdx(String path1,String path,Object a,Object b) {
    Bitmap bm1= getbitmap(path1);
    Matrix ma = new Matrix();
    float w=a;
    float h=b;
    ma.postScale(w,h);
    Bitmap bmp= Bitmap.createBitmap(bm1,0,0,bm1.getWidth(),bm1.getHeight(),ma,true);
    if("".equals(path)||path==null) {
        bmptofile(bmp,path1);
    }
    else {
        bmptofile(bmp,path);
    }
}
public void pinpic(String path1,String path2,Object sw,Object sh,Object x,Object y,Object yd,String path) {
    Bitmap bm1=getbitmap(path1);
    Bitmap bm2 =getbitmap(path2);
    Matrix ma = new Matrix();
    float a=(float)sw*((float)bm1.getWidth()/(float)bm2.getWidth());
    float b=(float)sh*((float)bm1.getHeight()/(float)bm2.getHeight());
    if(sw>998||sh>998) {
        float i=Math.min(a,b);
        ma.postScale(i,i);
    }
    else {
        ma.postScale(a,b);
    }
    Bitmap zmp = Bitmap.createBitmap(bm2,0,0,bm2.getWidth(),bm2.getHeight(),ma,true);
    float x1=x*bm1.getWidth()-0.5*zmp.getWidth();
    float y1=y*bm1.getHeight()-0.5*zmp.getHeight();
    float ds=yd;
    Bitmap smp=getroundbmp(zmp,ds);
    Canvas cas = new Canvas(bm1);
    cas.drawBitmap(smp,x1,y1,null);
    if("".equals(path)||path==null) {
        bmptofile(bm1,path1);
    }
    else {
        bmptofile(bm1,path);
    }
}
public void writetopic(String path1,String text,String color,Object x,Object y,Object size,String path) {
    Bitmap bmp=getbitmap(path1);
    Canvas cas = new Canvas(bmp);
    Paint pt = new Paint();
    //pt.setFakeBoldText(true);
    pt.setTextSize(size);
    //pt.setTypeface(Typeface.SANS_SERIF);
    pt.setTypeface(Typeface.MONOSPACE);
    if(!"".equals(color)&&!color==null)
    pt.setColor(Color.parseColor(color));
    float x1=x*bmp.getWidth()-0.5*text.length()*size;
    float y1=y*bmp.getHeight()+0.5*size;
    cas.drawText(text,x1,y1,pt);
    if("".equals(path)||path==null) {
        bmptofile(bmp,path1);
    }
    else {
        bmptofile(bmp,path);
    }
}
public void bmptofile(Bitmap bmp,String path) {
    File f= new File(path);
    if(f.exists()) {
        f.delete();
    }
    if(!f.exists()) {
        f.getParentFile().mkdirs();
    }
    FileOutputStream fs = new FileOutputStream(path);
    bmp.compress(Bitmap.CompressFormat.PNG,100,fs);
    fs.flush();
}
public void pinqpic(String path1,String uin,Object sw,Object sh,Object x,Object y,Object yd,String path) {
    Bitmap bm1=getbitmap(path1);
    Bitmap bm2 =getbitmap("http://q1.qlogo.cn/g?b=qq&nk="+uin+"&s=640");
    Matrix ma = new Matrix();
    float a=(float)sw*((float)bm1.getWidth()/(float)bm2.getWidth());
    float b=(float)sh*((float)bm1.getHeight()/(float)bm2.getHeight());
    if(sw>998||sh>998) {
        float i=Math.min(a,b);
        ma.postScale(i,i);
    }
    else {
        ma.postScale(a,b);
    }
    Bitmap zmp = Bitmap.createBitmap(bm2,0,0,bm2.getWidth(),bm2.getHeight(),ma,true);
    float x1=x*bm1.getWidth()-0.5*zmp.getWidth();
    float y1=y*bm1.getHeight()-0.5*zmp.getHeight();
    float ds=yd;
    Bitmap smp=getroundbmp(zmp,ds);
    Canvas cas = new Canvas(bm1);
    cas.drawBitmap(smp,x1,y1,null);
    if("".equals(path)||path==null) {
        bmptofile(bm1,path1);
    }
    else {
        bmptofile(bm1,path);
    }
}
public static Bitmap getroundbmp(Bitmap bitmap,float roundPx) {
    Bitmap bmp = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(bmp);
    Paint paint = new Paint();
    Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
    RectF rectF = new RectF(rect);
    paint.setAntiAlias(true);
    canvas.drawRoundRect(rectF, roundPx, roundPx, paint);
    paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
    canvas.drawBitmap(bitmap, rect, rect,paint);
    return bmp;
}
public static void delAllFile(String path) {
    boolean flag = false;
    File file = new File(path);
    if (!file.exists()) {
        return;
    }
    if (!file.isDirectory()) {
        return;
    }
    String[] tempList = file.list();
    File temp = null;
    for (int i = 0;
    i < tempList.length;
    i++) {
        if (path.endsWith(File.separator)) {
            temp = new File(path + tempList[i]);
        }
        else {
            temp = new File(path + File.separator + tempList[i]);
        }
        if (temp.isFile()) {
            temp.delete();
        }
        if (temp.isDirectory()) {
            delAllFile(path + "/" + tempList[i]);
            //先删除文件夹里面的文件
            flag = true;
        }
    }
    return;
}
public int 判断文件(String files) {
    try {
        File file = new File(files);
        long totalBytes = file.length();
        if(totalBytes==0||!file.exists()) {
            return 0;
        }
        else {
            return 1;
        }
    }
    catch(e) {
        return 0;
    }
}
public void 下载图片功能(String qun) {
    new Thread(new Runnable() {
        public void run() {
            try {
                DownloadToFile("https://gitee.com/ColdRainJava/update/raw/master/图片.zip",ColdRainPath+"下载/图片.zip");
                Unzip(ColdRainPath+"下载/图片.zip",ColdRainPath+"图片/");
                Toast("下载图片缓存成功！");
            }
            catch(e) {
                Toast("哈哈～图床也是跑路了呢！『图片功能』已关闭");
                写(qun,"图片功能","开关",0);
            }
        }
    }
    ).start();
}
public void MakeImage(Object Yu,String type,String img_path,int path_type) {
    new Thread(new Runnable() {
        public void run() {
            try {
                Object data = getData();
                data.put(Yu,Module);
                String quntext = data.quntext;
                String qun = data.qun;
                String uin = data.uin;
                String qq=myUin;
                int mtype=data.mtype;
                long msgid=data.msgid;
                int msgtype=data.msgtype;
                if(!type.equals("")) {
                    删除(qun,"图片功能模式");
                    if(判断文件(ColdRainPath+"图片/企业")==0||判断文件(ColdRainPath+"图片/全透明")==0||判断文件(ColdRainPath+"图片/圆圈")==0||判断文件(ColdRainPath+"图片/绿勾")==0||判断文件(ColdRainPath+"图片/红勾")==0||判断文件(ColdRainPath+"图片/黄V")==0||判断文件(ColdRainPath+"图片/蓝V")==0||判断文件(ColdRainPath+"图片/红旗1")==0||判断文件(ColdRainPath+"图片/V10")==0||判断文件(ColdRainPath+"图片/红旗2")==0||判断文件(ColdRainPath+"图片/阴影头像")==0||判断文件(ColdRainPath+"图片/双重阴影头像")==0||判断文件(ColdRainPath+"图片/六边形")==0) 下载图片功能(qun);
                    String 图片底=ColdRainPath+"图片/"+System.currentTimeMillis()+".png";
                    String 图片=ColdRainPath+"图片/"+System.currentTimeMillis()+"1.png";
                    if(path_type==0) {
                        if(Module.equals("模了个块")){
                            String url=quntext;
                            urltofile(url,图片底);
                        }else{
                            String url=findUrlbyDataMsg(Yu);
                            urltofile(url,图片底);
                        }
                    }
                    else if(path_type==1) {
                        String url=img_path;
                        urltofile(url,图片底);
                    }
                    else if(path_type==2) {
                        图片底=img_path;
                    }
                    if(type.equals("阴影头像")) {
                        pinpic(ColdRainPath+"图片/阴影头像",图片底,0.61,0.61,0.542,0.448,210,图片);
                        sendImg(qun,图片,mtype);
                    }
                    else if(type.equals("V10头像")) {
                        pinpic(图片底,ColdRainPath+"图片/V10",999,0.65,0.6,0.6,90,图片);
                        sendImg(qun,图片,mtype);
                    }
                    else if(type.equals("双重阴影头像")) {
                        pinpic(ColdRainPath+"图片/双重阴影头像",图片底,0.52,0.52,0.540,0.448,75,图片);
                        sendImg(qun,图片,mtype);
                    }
                    else if(type.equals("红旗头像1")) {
                        pinpic(图片底,ColdRainPath+"图片/红旗1",999,1,0.5,0.5,0,图片);
                        sendImg(qun,图片,mtype);
                    }
                    else if(type.equals("红旗头像2")) {
                        pinpic(图片底,ColdRainPath+"图片/红旗2",999,1,0.5,0.5,0,图片);
                        sendImg(qun,图片,mtype);
                    }
                    else if(type.equals("蓝V头像")) {
                        pinpic(图片底,ColdRainPath+"图片/蓝V",999,0.65,0.6,0.6,90,图片);
                        sendImg(qun,图片,mtype);
                    }
                    else if(type.equals("黄V头像")) {
                        pinpic(图片底,ColdRainPath+"图片/黄V",999,0.65,0.6,0.6,90,图片);
                        sendImg(qun,图片,mtype);
                    }
                    else if(type.equals("红勾认证")) {
                        pinpic(图片底,ColdRainPath+"图片/红勾",999,0.65,0.6,0.6,90,图片);
                        sendImg(qun,图片,mtype);
                    }
                    else if(type.equals("绿勾认证")) {
                        pinpic(图片底,ColdRainPath+"图片/绿勾",999,0.65,0.6,0.6,90,图片);
                        sendImg(qun,图片,mtype);
                    }
                    else if(type.equals("圆圈头像")) {
                        pinpic(ColdRainPath+"图片/圆圈",图片底,999,0.85,0.5,0.5,360,图片);
                        sendImg(qun,图片,mtype);
                    }
                    else if(type.equals("企业认证")) {
                        pinpic(图片底,ColdRainPath+"图片/企业",999,0.65,0.6,0.6,90,图片);
                        sendImg(qun,图片,mtype);
                    }
                    else if(type.equals("半透明头像")) {
                        pinpic(ColdRainPath+"图片/全透明",图片底,999,0.65,0.5,0.5,0,图片);
                        sendImg(qun,图片,mtype);
                    }
                    else if(type.equals("六边形头像")) {
                        pinpic(ColdRainPath+"图片/六边形",图片底,0.355,0.355,0.4989,0.499,360,图片);
                        sendImg(qun,图片,mtype);
                    }
                    else if(type.equals("羽化圆形")) {
                        circleCutWithShadowAndBlur(图片底, 图片);
                        sendImg(qun,图片,mtype);
                    }
                    else if(type.equals("透明背景")) {
                        addShadowToImages(图片底, 图片);
                        sendImg(qun,图片,mtype);
                    }
                }
                else {
                    sendText(data,"错误的图片功能设置");
                }
            }
            catch(e) {
                sendText(data,"图片功能错误:"+e);
            }
        }
    }
    ).start();
}

import android.graphics.*;
import java.io.*;

public Bitmap circleCutWithShadowAndBlur(Bitmap source) {
    try {
        // 裁剪正方形
        int w = source.getWidth();
        int h = source.getHeight();
        Bitmap squareBmp;
        if (w == h) {
            squareBmp = source;
        } else {
            int size = Math.min(w, h);
            int x = (w - size) / 2;
            int y = (h - size) / 2;
            squareBmp = Bitmap.createBitmap(source, x, y, size, size);
        }
        
        int size = squareBmp.getWidth();
        int padding = 40;
        int canvasSize = size + padding * 2;
        
        // 创建透明画布
        Bitmap result = Bitmap.createBitmap(canvasSize, canvasSize, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);
        
        float cx = canvasSize / 2.0f;
        float cy = canvasSize / 2.0f;
        float radius = size / 2.0f;
        
        // 创建圆形渐变遮罩
        Bitmap maskBitmap = Bitmap.createBitmap(canvasSize, canvasSize, Bitmap.Config.ARGB_8888);
        Canvas maskCanvas = new Canvas(maskBitmap);
        
        RadialGradient maskGradient = new RadialGradient(
            cx, cy, radius,
            new int[]{
                Color.argb(255, 0, 0, 0),
                Color.argb(255, 0, 0, 0),
                Color.argb(128, 0, 0, 0),
                Color.argb(0, 0, 0, 0)
            },
            new float[]{0.0f, 0.7f, 0.9f, 1.0f},
            Shader.TileMode.CLAMP
        );
        
        Paint maskPaint = new Paint();
        maskPaint.setShader(maskGradient);
        maskCanvas.drawRect(0, 0, canvasSize, canvasSize, maskPaint);
        
        // 先画原图
        canvas.drawBitmap(squareBmp, cx - radius, cy - radius, null);
        
        // 用遮罩把边缘变透明
        Paint alphaPaint = new Paint();
        alphaPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
        canvas.drawBitmap(maskBitmap, 0, 0, alphaPaint);
        
        maskBitmap.recycle();
        
        if (squareBmp != source) {
            squareBmp.recycle();
        }
        
        return result;
        
    } catch (Exception e) {
        return source;
    }
}
public void circleCutWithShadowAndBlur(String infile, String outfile) {
    try {
        Bitmap input = BitmapFactory.decodeFile(infile);
        Bitmap output = circleCutWithShadowAndBlur(input);
        
        FileOutputStream fos = new FileOutputStream(outfile);
        output.compress(Bitmap.CompressFormat.PNG, 100, fos);
        fos.flush();
        fos.close();
        
        input.recycle();
        output.recycle();
        
        //Toast("circleCutWithShadowAndBlur处理完成");
    } catch (Exception e) {
        Toast("处理失败：" + e.toString());
    }
}


public Bitmap addShadowToImage(Bitmap source) {
    try {
        int srcW = source.getWidth();
        int srcH = source.getHeight();

        // 1. 画布保持原图大小
        int canvasW = srcW;
        int canvasH = srcH;

        // 2. 原图占画布宽度的 3/4，高度按原图比例缩放
        float targetW = canvasW * 0.6f;
        float ratio = (float) srcH / srcW;          // 原图高宽比
        float targetH = targetW * ratio;

        // 3. 缩放原图（保持比例，不拉伸变形）
        Bitmap scaledBmp = Bitmap.createScaledBitmap(source, (int) targetW, (int) targetH, true);

        // 4. 创建透明画布
        Bitmap result = Bitmap.createBitmap(canvasW, canvasH, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);

        // 5. 居中放置（垂直方向也居中）
        float imgX = (canvasW - targetW) / 2.0f;
        float imgY = (canvasH - targetH) / 2.0f;

        // 6. 阴影参数
        float shadowOffsetX = 8.0f;
        float shadowOffsetY = 12.0f;
        float corner = 12.0f;

// 第一层阴影（最大范围，最模糊）
Paint shadow1 = new Paint();
shadow1.setColor(Color.argb(60, 0, 0, 0));
shadow1.setAntiAlias(true);
shadow1.setMaskFilter(new BlurMaskFilter(50.0f, BlurMaskFilter.Blur.NORMAL));
RectF rect1 = new RectF(
    imgX + 25.0f, imgY + 35.0f,
    imgX + 25.0f + targetW, imgY + 35.0f + targetH
);
canvas.drawRoundRect(rect1, corner, corner, shadow1);

// 第二层阴影（中等范围）
Paint shadow2 = new Paint();
shadow2.setColor(Color.argb(80, 0, 0, 0));
shadow2.setAntiAlias(true);
shadow2.setMaskFilter(new BlurMaskFilter(30.0f, BlurMaskFilter.Blur.NORMAL));
RectF rect2 = new RectF(
    imgX + 15.0f, imgY + 22.0f,
    imgX + 15.0f + targetW, imgY + 22.0f + targetH
);
canvas.drawRoundRect(rect2, corner, corner, shadow2);

// 第三层阴影（较深）
Paint shadow3 = new Paint();
shadow3.setColor(Color.argb(100, 0, 0, 0));
shadow3.setAntiAlias(true);
shadow3.setMaskFilter(new BlurMaskFilter(15.0f, BlurMaskFilter.Blur.NORMAL));
RectF rect3 = new RectF(
    imgX + 8.0f, imgY + 12.0f,
    imgX + 8.0f + targetW, imgY + 12.0f + targetH
);
canvas.drawRoundRect(rect3, corner, corner, shadow3);

// 第四层阴影（紧贴图片）
Paint shadow4 = new Paint();
shadow4.setColor(Color.argb(60, 0, 0, 0));
shadow4.setAntiAlias(true);
shadow4.setMaskFilter(new BlurMaskFilter(5.0f, BlurMaskFilter.Blur.NORMAL));
RectF rect4 = new RectF(
    imgX + 3.0f, imgY + 5.0f,
    imgX + 3.0f + targetW, imgY + 5.0f + targetH
);
canvas.drawRoundRect(rect4, corner, corner, shadow4);
        // 7. 贴上缩放后的图片
        canvas.drawBitmap(scaledBmp, imgX, imgY, null);

        if (scaledBmp != source) {
            scaledBmp.recycle();
        }
        return result;

    } catch (Exception e) {
        return source;
    }
}
public void addShadowToImages(String path1, String path2){
    Bitmap input = BitmapFactory.decodeFile(path1);
    Bitmap output = addShadowToImage(input);

    FileOutputStream fos = new FileOutputStream(path2);
    output.compress(Bitmap.CompressFormat.PNG, 100, fos);
    fos.flush();
    fos.close();
}