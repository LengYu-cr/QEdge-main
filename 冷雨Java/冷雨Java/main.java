String myBot = "102615873";//官机appid
String myBotQQ = "3889527271";//官机QQ
//我的官机
//官方群1:   903976754
//官方群2:   774224871
//官方群3:   641486099
//作者QQ1431136407(QQ2: 3635522745)
//发送『加群』即可加入官方群
//没加密多少
//可以二改，→出问题找我你没(   )

// 冷雨个人主页 https://v.yuafeng.cn/
// 枫林api https://api.yuafeng.cn/
// 枫雨api https://api-v2.yuafeng.cn/


//哎呦我去，你看集贸啊
//给你一拳

//You came into my life,just like another season.
//你来到我的生命中，就像是另一个新的季节。
long firstLoadTime = System.currentTimeMillis();
import java.lang.reflect.Method;
import java.lang.reflect.*;
import android.content.Context;
import android.os.Environment;
String QQ_PackageName = context.getPackageName();
String versionName=context.getPackageManager().getPackageInfo(QQ_PackageName, 0).versionName.replace(".","");
int QQ_versionName;
String CurrentApp="";
if(versionName.length()==3) QQ_versionName=Integer.parseInt(versionName+"0");
else QQ_versionName=Integer.parseInt(versionName);
if(QQ_PackageName.equals("com.tencent.mobileqq")) CurrentApp="QQ";
else if(QQ_PackageName.equals("com.tencent.tim")) CurrentApp="TIM";
public String judge() {
    Object MClassLoader;
    Class Clazz = this.getClass();
    if(Clazz != null)
    {
        MClassLoader = Clazz.getClassLoader();
    }
    String path = MClassLoader.toString();
    if(path.contains("lzlnb.cnm.hook"))
    {
        return "模了个块";
    }
    else if(path.contains("com.demo.serendipity"))
    {
        return "Serendipity";
    }
    else if(path.contains("lin.xposed"))
    {
        return "QStory";
    }
    else if(path.contains("me.yxp.qfun"))
    {
        return "QFun";
    }
    else
    {
        return "未知模块";
    }
}
/**
 * 判断是否为有效的UIN（纯数字，大于10000）
 */
public boolean isValidUin(String uin) {
    if (uin == null || uin.isEmpty()) {
        return false;
    }
    if (!uin.matches("\\d+")) {
        return false;
    }
    try {
        long uinLong = Long.parseLong(uin);
        return uinLong > 10000;
    } catch (NumberFormatException e) {
        return false;
    }
}

/**
 * 判断是否为有效的UID（以"u_"开头）
 */
public boolean isValidUid(String uid) {
    return uid != null && uid.startsWith("u_") && uid.length() > 2;
}

/**
 * 根据UIN获取UID
 */
public String getUidFromUin(String uin) {
    if (!isValidUin(uin)) {
        return uin;
    }
    try {
        String uidFromUin = ((IRelationNTUinAndUidApi) QRoute.api(IRelationNTUinAndUidApi.class)).getUidFromUin(uin);
        return uidFromUin != null ? uidFromUin : uin;
    } catch (Exception e) {
        return uin;
    }
}

/**
 * 根据UID获取UIN
 */
public String getUinFromUid(String uid) {
    if (!isValidUid(uid)) {
        return uid;
    }
    try {
        String UinFromUid = ((IRelationNTUinAndUidApi) QRoute.api(IRelationNTUinAndUidApi.class)).getUinFromUid(uid);
        return UinFromUid != null ? UinFromUid : uid;
    } catch (Exception e) {
        return uid;
    }
}

/**
 * 获取好友UID（通过UIN）
 */
public String getFriendUidFromUin(String uin) {
    if (!isValidUin(uin)) {
        return uin;
    }
    try {
        String uidFromUin = ((IRelationNTUinAndUidApi) QRoute.api(IRelationNTUinAndUidApi.class)).getFriendUidFromUin(uin);
        return uidFromUin != null ? uidFromUin : uin;
    } catch (Exception e) {
        return uin;
    }
}

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
public List mapToList(Map atList) {
    List atUserList = new ArrayList();
    if (atList == null || atList.isEmpty()) {
        return atUserList;
    }
    for (Object entryObj : atList.entrySet()) {
        Map.Entry entry = (Map.Entry) entryObj;
        Object key = entry.getKey();
        // 将long类型转为String
        if (key instanceof Long) {
            atUserList.add(String.valueOf(key));
        } else if (key instanceof String) {
            atUserList.add(key);
        }
    }
    return atUserList;
}

public class Data {
    public String quntext;
    public String uin;
    public String qun;
    public int mtype;
    public int msgtype;
    public long msgid;
    public long time;
    public Object originMsg;
    public Object ModuleMsg;
    public List atList;
    public String username;
    public void put(Object msg,String type) {
        this.ModuleMsg=msg;
        try {
            if(type.equals("Serendipity")) {
                this.quntext=msg.msg;
                this.uin=msg.user;
                this.qun=msg.peerUid;
                this.mtype=msg.type;
                this.msgtype=msg.msgType;
                this.msgid=msg.data.msgId;
                this.originMsg=msg.data;
                this.atList=msg.atList;
                this.time=msg.data.msgTime;
                if(msg.type==1) {
                    this.username=msg.data.sendNickName;
                }
                else if(msg.type==2) {
                    this.username=msg.data.sendMemberName;
                }
                else if(msg.type==8) {
                    this.username="我的电脑";
                    this.qun=msg.data.peerUid+"";
                }
                else if(msg.type==103) {
                    this.username="QQ安全中心";
                    this.qun=msg.data.peerUid+"";
                }
            }
            else if(type.equals("QFun")) {
                this.quntext=msg.msg;
                this.uin=msg.userUin;
                this.qun=msg.peerUin;
                this.mtype=msg.type;
                this.msgtype=msg.data.elements.get(0).elementType;
                this.msgid=msg.data.msgId;
                this.originMsg=msg.data;
                this.atList=msg.atList;
                this.time=msg.data.msgTime;
                if(msg.type==1) {
                    this.username=msg.data.sendNickName;
                }
                else if(msg.type==2) {
                    this.username=msg.data.sendMemberName;
                }
                else if(msg.type==8) {
                    this.username="我的电脑";
                    this.qun=msg.data.peerUid+"";
                }
                else if(msg.type==103) {
                    this.username="QQ安全中心";
                    this.qun=msg.data.peerUid+"";
                }
            }
            else if(type.equals("QStory")) {
                this.quntext=msg.MessageContent;
                this.uin=msg.UserUin;
                this.mtype=msg.msg.chatType;
                if(msg.msg.chatType==2) {
                    this.qun=msg.GroupUin;
                    this.username=msg.msg.sendMemberName;
                }
                else if(msg.msg.chatType==1) {
                    this.qun=msg.msg.peerUin+"";
                    this.username=msg.msg.sendNickName;
                }
                else if(msg.msg.chatType==8) {
                    this.qun=msg.msg.peerUin+"";
                    this.username="我的电脑";
                }
                else if(msg.msg.chatType==103) {
                    this.username="QQ安全中心";
                    this.qun=msg.msg.peerUid+"";
                }
                this.msgtype=msg.msg.elements.get(0).elementType;
                this.msgid=msg.msg.msgId;
                this.originMsg=msg.msg;
                this.time=msg.msg.msgTime;
                this.atList=msg.mAtList;
            }
            else if(type.equals("模了个块")) {
                this.quntext=msg.Content;
                this.uin=msg.msg.senderUin.toString();
                this.mtype=msg.msg.chatType;
                if(msg.msg.chatType==2) {
                    this.qun=msg.msg.peerUin.toString();
                    this.username=msg.msg.sendMemberName;
                }
                else if(msg.msg.chatType==1) {
                    this.qun=msg.msg.senderUin.toString();
                    this.username=msg.msg.sendNickName;
                }
                else if(msg.msg.chatType==8) {
                    this.qun=msg.msg.peerUin+"";
                    this.username="我的电脑";
                }
                else if(msg.msg.chatType==103) {
                    this.username="QQ安全中心";
                    this.qun=msg.msg.peerUid+"";
                }
                this.msgtype=msg.msg.elements.get(0).elementType;
                this.msgid=msg.msg.msgId;
                this.originMsg=msg.msg;
                this.time=msg.msg.msgTime;
                this.atList=mapToList(msg.AtList);
            }
            else if(type.equals("OriginMsg")) {
                String text="";
                if(msg==null) {
                    this.quntext = "";
                    this.uin = "";
                    this.qun = "";
                    this.mtype=0;
                    this.msgtype=0;
                    this.originMsg=null;
                    this.atList=new ArrayList();
                    this.msgid=0;
                    this.username="";
                    this.time=0;
                }
                else {
                    for(Object msgElement:msg.elements) {
                        if(msgElement.textElement!=null) {
                            text+=msgElement.textElement.content;
                        }
                        else if(msgElement.picElement!=null) {
                            text+=""+msgElement.picElement.summary;
                        }
                        else if(msgElement.pttElement!=null) {
                            text=""+msgElement.pttElement.md5HexStr;
                        }
                        else if(msgElement.fileElement!=null) {
                            text=""+msgElement.fileElement.fileMd5;
                        }
                        else if(msgElement.arkElement!=null) {
                            text=""+msgElement.arkElement.bytesData;
                        }
                        else if(msgElement.videoElement!=null) {
                            text=""+msgElement.videoElement.videoMd5;
                        }
                        else if(msgElement.replyElement!=null) {
                            text+=""+msgElement.replyElement.sourceMsgText+"\n";
                        }
                        else if(msgElement.avRecordElement!=null) {
                            text=""+msgElement.avRecordElement.text;
                        }
                        else if(msgElement.walletElement!=null) {
                            text=""+msgElement.walletElement.receiver.title;
                        }
                        else if(msgElement.faceElement!=null) {
                            text+=""+msgElement.faceElement.faceText;
                        }
                        else if(msgElement.faceBubbleElement!=null) {
                            text=""+msgElement.faceBubbleElement.faceSummary+" "+msgElement.faceBubbleElement.content;
                        }
                        else {
                            text="";
                        }
                    }
                    this.quntext=text;
                    this.uin=getUinFromUid(""+msg.senderUid);
                    this.mtype=msg.chatType;
                    if(msg.chatType==2) {
                        this.qun=msg.peerUid+"";
                        this.username=msg.sendMemberName;
                    }
                    else if(msg.chatType==1) {
                        this.qun=msg.peerUin+"";
                        this.username=msg.sendNickName;
                    }
                    else if(msg.chatType==8) {
                        this.qun=msg.peerUin+"";
                        this.username="我的电脑";
                    }
                    else if(msg.type==103) {
                        this.username="QQ安全中心";
                        this.qun=msg.peerUid+"";
                    }
                    this.msgtype=msg.elements.get(0).elementType;
                    this.msgid=msg.msgId;
                    this.originMsg=msg;
                    this.time=msg.msgTime;
                    this.atList=new ArrayList();
                }
            }
        }
        catch(e) {
            this.quntext = "";
            this.uin = "";
            this.qun = "";
            this.mtype=0;
            this.msgtype=0;
            this.originMsg=null;
            this.atList=new ArrayList();
            this.msgid=0;
            this.username="";
            this.time=0;
        }
    }
    public Data() {
        this.quntext = "";
        this.uin = "";
        this.qun = "";
        this.mtype=0;
        this.msgtype=0;
        this.originMsg=null;
        this.atList=new ArrayList();
        this.msgid=0;
        this.username="";
        this.time=0;
    }

    public String toString() {
        StringBuilder DataElement = new StringBuilder();
        DataElement.append("JavaMsgRecords{quntext=");
        DataElement.append(quntext);
        DataElement.append(",uin=");
        DataElement.append(uin);
        DataElement.append(",qun=");
        DataElement.append(qun);
        DataElement.append(",mtype=");
        DataElement.append(mtype);
        DataElement.append(",msgtype=");
        DataElement.append(msgtype);
        DataElement.append(",originMsg=");
        List originMsgList= new ArrayList();
        originMsgList.add(originMsg);
        DataElement.append(originMsgList);
        DataElement.append(",atList=");
        DataElement.append(atList);
        DataElement.append(",msgid=");
        DataElement.append(msgid);
        DataElement.append(",username=");
        DataElement.append(username);
        DataElement.append(",time=");
        DataElement.append(time);
        DataElement.append("}");
        return DataElement.toString();
    }
}

// Object DATAMSG = new Data();

public Object getData(){
    return new Data();
}


String Module=judge();
if(Module.equals("QStory")) {
    load(appPath+"/配置.java");
    load(appPath+"/冷雨目录/import.java");
    load(appPath+"/冷雨目录/api2.java");
    load(appPath+"/冷雨目录/QStory.java");
}
else if(Module.equals("Serendipity")) {
    loadJava(pluginPath+"配置.java");
    loadJava(pluginPath+"冷雨目录/import.java");
    loadJava(pluginPath+"冷雨目录/api2.java");
    loadJava(pluginPath+"冷雨目录/Serendipity.java");
}
else if(Module.equals("QFun")) {
    loadJava(pluginPath+"/配置.java");
    loadJava(pluginPath+"/冷雨目录/import.java");
    loadJava(pluginPath+"/冷雨目录/api2.java");
    loadJava(pluginPath+"/冷雨目录/QFun.java");
}
else if(Module.equals("模了个块")) {
    String pluginPath = getScriptPath();
    load(pluginPath+"配置.java");
    load(pluginPath+"冷雨目录/import.java");
    load(pluginPath+"冷雨目录/api2.java");
    load(pluginPath+"冷雨目录/模了个块.java");
}else {
    Toast("不支持的运行模块");
}