package com.tencent.qqnt.msg.api.impl;

import com.tencent.qqnt.kernel.nativeinterface.MsgElement;
import com.tencent.qqnt.kernel.nativeinterface.MsgRecord;
import com.tencent.qqnt.kernel.nativeinterface.TextElement;
import com.tencent.qqnt.msg.api.IMsgUtilApi;
import kotlin.jvm.functions.Function1;

import java.util.ArrayList;
import kotlin.Pair;

public class MsgUtilApiImpl {
    
    public MsgUtilApiImpl() {
    }
    
    
    public MsgElement createTextElement(String content) {
        throw new RuntimeException("Not yet implemented");
    }

    public MsgElement createTextElement(TextElement textElement) {
        throw new RuntimeException("Not yet implemented");
    }

    public MsgElement createAtTextElement(String name, String uid, int type) {
        throw new RuntimeException("Not yet implemented");
    }

    public MsgElement createFaceElement(int faceType, int index, String desc) {
        throw new RuntimeException("Not yet implemented");
    }

    public MsgElement createFaceElement(int faceType, int index, String packId, int imageType, String desc) {
        throw new RuntimeException("Not yet implemented");
    }


    public MsgElement createFileElement(String path) {
        throw new RuntimeException("Not yet implemented");
    }

            
    public MsgElement createFileElement(String path, int type) {
        throw new RuntimeException("Not yet implemented");
    }
            
    public MsgElement createPicElement(String origPath, boolean quality, int subType, String textSummary,
                                       float maxPicSize, Object picExtBizInfo, boolean isFlashPic,
                                       Function1<?, ?> extPicElement) {
        throw new RuntimeException("Not yet implemented");
    }
            
    public MsgElement createPicElement(String origPath, boolean quality, int subType) {
        throw new RuntimeException("Not yet implemented");
    }
            
    public MsgElement createPicElementForGuild(String origPath, boolean quality, int subType) {
        throw new RuntimeException("Not yet implemented");
    }
            
    public MsgElement createVideoElement(String origPath) {
        throw new RuntimeException("Not yet implemented");
    }
            
    public MsgElement createVideoElement(String origPath, int subBusiType, boolean isClip, String thumbPath) {
        throw new RuntimeException("Not yet implemented");
    }
            
    public MsgElement createPttElement(String origPath, int duration, ArrayList<Byte> audioData) {
        throw new RuntimeException("Not yet implemented");
    }
            
    public MsgElement createPttElement(String origPath, int duration) {
        throw new RuntimeException("Not yet implemented");
    }
            
    public MsgElement createReplyElement(long msgId) {
        throw new RuntimeException("Not yet implemented");
    }
            
    public MsgElement createReplyElement(long msgId, String senderUidStr, String sourceMsgText) {
        throw new RuntimeException("Not yet implemented");
    }
            
    public Pair<Integer, Integer> scaleSize(int width, int height, float maxPicSize) {
        return new Pair<>(0, 0);
    }
            
    public String getElementContent(MsgElement element) {
        return "";
    }
            
    public MsgElement createGiphyElement(String id, int width, int height, boolean isClip) {
        throw new RuntimeException("Not yet implemented");
    }
            
    public MsgElement createArkElement(Object arkMsgModel) {
        throw new RuntimeException("Not yet implemented");
    }
            
    public Pair<Integer, Integer> getPicSizeByPath(String path) {
        return new Pair<>(0, 0);
    }
            
    public boolean isTextElem(MsgElement elem) {
        return false;
    }
            
    public boolean isPictureElem(MsgElement elem) {
        return false;
    }
            
    public boolean isVideoElem(MsgElement elem) {
        return false;
    }
            
    public boolean isArkElem(MsgElement elem) {
        return false;
    }
            
    public String getElementSummary(MsgRecord mrecord) {
        return "";
    }

            
    public String msgTypeToString(int msgType) {
        return "unknown";
    }

            
    public String msgTypeToString(MsgRecord record) {
        return "unknown";
    }
}