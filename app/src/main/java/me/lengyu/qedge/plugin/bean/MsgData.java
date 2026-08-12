package me.lengyu.qedge.plugin.bean;

import com.tencent.qqnt.kernel.nativeinterface.MsgElement;
import com.tencent.qqnt.kernel.nativeinterface.MsgRecord;
import com.tencent.qqnt.kernelpublic.nativeinterface.Contact;
import me.lengyu.qedge.utils.qq.CookieTool;
import me.lengyu.qedge.utils.qq.FriendTool;
import me.lengyu.qedge.utils.QQCurrentEnv;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

public class MsgData {

    public final MsgRecord data;

    public final int type;
    public final int msgType;
    public final int subMsgType;
    public final int sendType;
    public final int sendStatus;
    public final boolean editable;
    public final String peerUin;
    public final String peerUid;
    public final String userUin;
    public final String userUid;
    public final String userName;
    public final String peerName;
    public final long time;
    public final long msgId;
    public final long msgSeq;
    public final long msgRandom;
    public final String guildId;
    public final String channelId;
    public final Contact contact;

    public String msg = "";
    public final ArrayList<String> atList = new ArrayList<>();
    public final HashMap<String, String> atMap = new HashMap<>();
    public final ArrayList<String> picList = new ArrayList<>();
    public final ArrayList<String> videoList = new ArrayList<>();
    public final ArrayList<String> pttList = new ArrayList<>();
    public String path = "";
    public String replySenderUin = "";
    public String replySenderUid = "";
    public long replyMsgId = 0;

    public MsgData(Object msgRecord) {
        this.data = (MsgRecord) msgRecord;

        this.type = data.chatType;
        this.msgType = data.msgType;
        this.subMsgType = data.subMsgType;
        this.sendType = data.sendType;
        this.sendStatus = data.sendStatus;
        this.editable = data.editable;
        this.peerUin = String.valueOf(data.peerUin);
        this.peerUid = data.peerUid != null ? data.peerUid : "";
        this.userUin = String.valueOf(data.senderUin);
        this.userUid = data.senderUid != null ? data.senderUid : "";
        this.userName = data.sendNickName != null ? data.sendNickName : "";
        this.peerName = data.peerName != null ? data.peerName : "";
        this.time = data.msgTime;
        this.msgId = data.msgId;
        this.msgSeq = data.msgSeq;
        this.msgRandom = data.msgRandom;
        this.guildId = data.guildId != null ? data.guildId : "";
        this.channelId = data.channelId != null ? data.channelId : "";
        this.contact = new Contact(data.chatType, data.peerUid, data.guildId);

        if (data.elements != null) {
            processElements(data.elements);
        }
    }

    public MsgData() {
        this.data = null;
        this.type = 0;
        this.msgType = 0;
        this.subMsgType = 0;
        this.sendType = 0;
        this.sendStatus = 0;
        this.editable = false;
        this.peerUin = "";
        this.peerUid = "";
        this.userUin = "";
        this.userUid = "";
        this.userName = "";
        this.peerName = "";
        this.time = 0;
        this.msgId = 0;
        this.msgSeq = 0;
        this.msgRandom = 0;
        this.guildId = "";
        this.channelId = "";
        this.contact = null;
        this.msg = "";
        this.atList.clear();
        this.atMap.clear();
        this.picList.clear();
        this.videoList.clear();
        this.pttList.clear();
        this.path = "";
    }

    public void processElements(ArrayList<MsgElement> elements) {
        StringBuilder msgBuilder = new StringBuilder();
        for (MsgElement element : elements) {
            int elementType = element.elementType;
            try {
                switch (elementType) {
                    case 1: {
                        com.tencent.qqnt.kernel.nativeinterface.TextElement textElement = element.textElement;
                        if (textElement != null) {
                            msgBuilder.append(textElement.content);
                            if (textElement.atType == 2) {
                                String atUin = "";
                                try {
                                    if (textElement.atNtUid != null && !textElement.atNtUid.isEmpty()) {
                                        atUin = FriendTool.getUinFromUid(textElement.atNtUid);
                                    }
                                    if (atUin.isEmpty()) {
                                        atUin = String.valueOf(textElement.atUid);
                                    }
                                } catch (Throwable e) {
                                    atUin = String.valueOf(textElement.atUid);
                                }
                                atList.add(atUin);
                                atMap.put(atUin, textElement.content);
                            }
                        }
                        break;
                    }
                    case 2: {
                        String rkey = type == 1 ? CookieTool.getFriendRKey() : CookieTool.getGroupRKey();
                        com.tencent.qqnt.kernel.nativeinterface.PicElement picElement = element.picElement;
                        if (picElement != null) {
                            String url = picElement.originImageUrl;
                            if (url != null && !url.isEmpty()) {
                                String fullUrl = "https://multimedia.nt.qq.com.cn" + url + (rkey != null ? rkey : "");
                                msgBuilder.append("[pic=").append(fullUrl).append("]");
                                picList.add(fullUrl);
                            }
                        }
                        break;
                    }
                    case 3: {
                        com.tencent.qqnt.kernel.nativeinterface.FileElement fileElement = element.fileElement;
                        if (fileElement != null && fileElement.filePath != null) {
                            path = fileElement.filePath;
                        }
                        break;
                    }
                    case 4: {
                        com.tencent.qqnt.kernel.nativeinterface.PttElement pttElement = element.pttElement;
                        if (pttElement != null && pttElement.fileName != null) {
                            String pttPath = pttElement.filePath;
                            if (pttPath != null && !pttPath.isEmpty() && new File(pttPath).exists()) {
                                path = pttPath;
                            } else {
                                path = getPttPath(pttElement.fileName);
                            }
                            if (path != null && !path.isEmpty()) {
                                pttList.add(path);
                            }
                        }
                        break;
                    }
                    case 5: {
                        com.tencent.qqnt.kernel.nativeinterface.VideoElement videoElement = element.videoElement;
                        if (videoElement != null && (videoElement.originVideoMd5 != null || videoElement.videoMd5 != null)) {
                            String videoPath = videoElement.filePath;
                            if (videoPath != null && !videoPath.isEmpty() && new File(videoPath).exists()) {
                                path = videoPath;
                            } else {
                                path = getVideoPath(videoElement.originVideoMd5 != null ? videoElement.originVideoMd5 : videoElement.videoMd5);
                            }
                            if (path != null && !path.isEmpty()) {
                                videoList.add(path);
                            }
                        }
                        break;
                    }
                    case 7: {
                        com.tencent.qqnt.kernel.nativeinterface.ReplyElement replyElement = element.replyElement;
                        if (replyElement != null) {
                            replyMsgId = replyElement.replayMsgId;
                            if (replyElement.senderUidStr != null && !replyElement.senderUidStr.isEmpty()) {
                                replySenderUid = replyElement.senderUidStr;
                                try {
                                    String uin = FriendTool.getUinFromUid(replyElement.senderUidStr);
                                    if (uin != null && !uin.isEmpty()) {
                                        replySenderUin = uin;
                                    }
                                } catch (Throwable ignored) {
                                    if (replyElement.senderUid != null) {
                                        replySenderUin = String.valueOf(replyElement.senderUid);
                                    }
                                }
                            } else if (replyElement.senderUid != null) {
                                replySenderUin = String.valueOf(replyElement.senderUid);
                            }
                        }
                        break;
                    }
                    case 10: {
                        com.tencent.qqnt.kernel.nativeinterface.ArkElement arkElement = element.arkElement;
                        if (arkElement != null && arkElement.bytesData != null) {
                            msgBuilder.append(arkElement.bytesData);
                        }
                        break;
                    }
                    case 49: {
                        // 富媒体 / 泡泡视频 (FilterMsgElement, busiType=1 且 subBusiType=1 就是泡泡视频,其他也是 Filter 视频,都加到 videoList 让它可保存)
                        com.tencent.qqnt.kernel.nativeinterface.FilterMsgElement filterMsgElement = element.filterMsgElement;
                        if (filterMsgElement != null) {
                            if (filterMsgElement.videoMd5 != null && !filterMsgElement.videoMd5.isEmpty()) {
                                String fp = filterMsgElement.filePath;
                                if (fp != null && !fp.isEmpty() && new File(fp).exists()) {
                                    path = fp;
                                } else {
                                    path = getVideoPath(filterMsgElement.videoMd5);
                                }
                                if (path != null && !path.isEmpty()) {
                                    videoList.add(path);
                                }
                            }
                        }
                        break;
                    }
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        msg = msgBuilder.toString();
    }
    
    public MsgData clone() {
        return new MsgData(this);
    }

    public String getVideoPath(String videoMd5) {
        String path = QQCurrentEnv.getHostPath() + "Tencent/MobileQQ/shortvideo/" + videoMd5 + "/" + videoMd5 + ".mp4";
        return path;
    }

    public String getPttPath(String path) {
        String pttPath = QQCurrentEnv.getHostPath() + "Tencent/MobileQQ/" + QQCurrentEnv.getCurrentUin() + "/ptt/" + path;
        return pttPath;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("MsgData{");
        sb.append("type=").append(type);
        sb.append(", msgType=").append(msgType);
        sb.append(", subMsgType=").append(subMsgType);
        sb.append(", sendType=").append(sendType);
        sb.append(", sendStatus=").append(sendStatus);
        sb.append(", editable=").append(editable);
        sb.append(", peerUin='").append(peerUin).append("'");
        sb.append(", peerUid='").append(peerUid).append("'");
        sb.append(", userUin='").append(userUin).append("'");
        sb.append(", userUid='").append(userUid).append("'");
        sb.append(", userName='").append(userName).append("'");
        sb.append(", peerName='").append(peerName).append("'");
        sb.append(", time=").append(time);
        sb.append(", msgId=").append(msgId);
        sb.append(", msgSeq=").append(msgSeq);
        sb.append(", msgRandom=").append(msgRandom);
        sb.append(", guildId='").append(guildId).append("'");
        sb.append(", channelId='").append(channelId).append("'");
        sb.append(", contact=").append(contact);
        sb.append(", msg='").append(msg).append("'");
        sb.append(", atList=").append(atList);
        sb.append(", atMap=").append(atMap);
        sb.append(", picList=").append(picList);
        sb.append(", videoList=").append(videoList);
        sb.append(", pttList=").append(pttList);
        sb.append(", path='").append(path).append("'");
        sb.append(", data='").append(data).append("'");
        sb.append("}");
        return sb.toString();
    }

}