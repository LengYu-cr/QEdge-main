import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.text.TextUtils;

import com.tencent.qqnt.kernel.nativeinterface.MsgElement;
import com.tencent.qqnt.kernel.nativeinterface.QQNTWrapperUtil;
import com.tencent.qqnt.kernel.nativeinterface.RichMediaFilePathInfo;
import com.tencent.qqnt.kernel.nativeinterface.FilterMsgElement;
import me.lengyu.qedge.utils.QQCurrentEnv;
import me.lengyu.qedge.utils.qq.MsgTool;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

// ========== 工具方法 ==========
String getFileMd5(String path) {
    try {
        return QQNTWrapperUtil.CppProxy.genFileMd5Hex(path);
    } catch (Exception e) {
        log("getFileMd5 failed: " + path + e.getMessage());
        return "";
    }
}

long getFileSize(String path) {
    try {
        return QQNTWrapperUtil.CppProxy.getFileSize(path);
    } catch (Exception e) {
        log("getFileSize failed: " + path + e.getMessage());
        return 0;
    }
}

boolean fileExists(String path) {
    try {
        return QQNTWrapperUtil.CppProxy.fileIsExist(path);
    } catch (Exception e) {
        log("fileExists failed: " + path + e.getMessage());
        return false;
    }
}

boolean copyFile(String src, String dest) {
    try {
        boolean result = QQNTWrapperUtil.CppProxy.copyFile(src, dest);
        log("copyFile: " + src + " -> " + dest + ", result=" + result);
        return result;
    } catch (Exception e) {
        log("copyFile failed" + e.getMessage());
        return false;
    }
}

// ========== 生成缩略图 ==========
String generateThumbnail(String videoPath, Object kernelService, String md5, String fileName) {
    RichMediaFilePathInfo thumbInfo = new RichMediaFilePathInfo(
        5, 1, md5, fileName, 2, 0, null, "", true
    );
    String thumbPath = (String) kernelService.getClass()
        .getMethod("getRichMediaFilePathForMobileQQSend", RichMediaFilePathInfo.class)
        .invoke(kernelService, thumbInfo);
    
    if (TextUtils.isEmpty(thumbPath)) {
        log("get thumb path failed");
        return null;
    }
    
    if (fileExists(thumbPath)) {
        log("thumb already exists: " + thumbPath);
        return thumbPath;
    }
    
    MediaMetadataRetriever retriever = null;
    FileOutputStream fos = null;
    try {
        retriever = new MediaMetadataRetriever();
        retriever.setDataSource(videoPath);
        Bitmap frame = retriever.getFrameAtTime();
        if (frame == null) {
            log("getFrameAtTime return null");
            return null;
        }
        
        fos = new FileOutputStream(thumbPath);
        frame.compress(Bitmap.CompressFormat.JPEG, 60, fos);
        fos.flush();
        
        log("generate thumbnail success");
        return thumbPath;
        
    } catch (Exception e) {
        log("generate thumbnail exception" + e.getMessage());
        return null;
    } finally {
        try { if (fos != null) fos.close(); } catch (IOException e) { log("close fos failed" + e.getMessage()); }
        try { if (retriever != null) retriever.release(); } catch (Exception e) { log("release retriever failed" + e.getMessage()); }
    }
}

// ========== 主入口 ==========
MsgElement createFilterMsgElement(String videoPath) {
    log("createFilterMsgElement: " + videoPath);
    
    if (TextUtils.isEmpty(videoPath) || !fileExists(videoPath)) {
        log("video file not exists: " + videoPath);
        return null;
    }
    
    Object kernelService = QQCurrentEnv.getKernelMsgService();
    if (kernelService == null) {
        log("kernel service is null");
        return null;
    }
    
    String md5 = getFileMd5(videoPath);
    if (TextUtils.isEmpty(md5)) {
        log("calculate md5 failed");
        return null;
    }
    String fileName = new File(videoPath).getName();
    
    RichMediaFilePathInfo videoInfo = new RichMediaFilePathInfo(
        5, 2, md5, fileName, 1, 0, null, "", true
    );
    String destPath = (String) kernelService.getClass()
        .getMethod("getRichMediaFilePathForMobileQQSend", RichMediaFilePathInfo.class)
        .invoke(kernelService, videoInfo);
    
    if (TextUtils.isEmpty(destPath)) {
        log("get video dest path failed");
        return null;
    }
    log("video dest path: " + destPath);
    
    if (!fileExists(destPath)) {
        if (!copyFile(videoPath, destPath)) {
            log("copy video failed");
            return null;
        }
    }
    
    String thumbPath = generateThumbnail(videoPath, kernelService, md5, fileName);
    if (TextUtils.isEmpty(thumbPath)) {
        log("generate thumbnail failed");
        return null;
    }
    log("thumb path: " + thumbPath);
    
    BitmapFactory.Options options = new BitmapFactory.Options();
    options.inJustDecodeBounds = true;
    BitmapFactory.decodeFile(thumbPath, options);
    log("thumb size: " + options.outWidth + "x" + options.outHeight);
    // FilterMsgElement{filterId=,filePath=/storage/emulated/0/Android/data/com.tencent.mobileqq/files/ae/camera/capture/1785041943823/1785041950932.mp4,fileName=7c5293c2aea1658c0e3083eb69f6762d.mp4,videoMd5=7c5293c2aea1658c0e3083eb69f6762d,thumbMd5=43ba6b8aedcb8679df3745869bc76ec8,fileTime=1,thumbSize=35225,fileFormat=2,fileSize=431765,thumbWidth=720,thumbHeight=720,busiType=1,subBusiType=1,thumbPath={0=/storage/emulated/0/Android/data/com.tencent.mobileqq/cache/ae/camera/filter_video_cache/export/4a3c4b1a-89e1-4439-a1c6-6724f7b59c16-output_cover.png},transferStatus=0,progress=0,invalidState=0,fileUuid=,fileSubId=,fileBizId=null,originVideoMd5=,importRichMediaContext=null,sourceVideoCodecFormat=KCODECFORMATH264,storeID=0,original=true,videoFrom=1,isInApplicationDataPath=null,},
    FilterMsgElement filterMsgElement = new FilterMsgElement();
    filterMsgElement.filePath = destPath;
    filterMsgElement.videoMd5 = md5;
    filterMsgElement.fileSize = getFileSize(destPath);
    filterMsgElement.fileName = fileName;
    filterMsgElement.fileFormat = 2;
    filterMsgElement.busiType = 1;
    filterMsgElement.subBusiType = 1;
    filterMsgElement.fileTime = 91;
    filterMsgElement.thumbWidth = options.outWidth;
    filterMsgElement.thumbHeight = options.outHeight;
    filterMsgElement.thumbMd5 = getFileMd5(thumbPath);
    filterMsgElement.thumbSize = (int) getFileSize(thumbPath);
    HashMap<Integer, String> thumbMap = new HashMap<>();
    thumbMap.put(0, thumbPath);
    filterMsgElement.thumbPath = thumbMap;
    
    MsgElement msgElement = new MsgElement();
    msgElement.elementType = 49;
    msgElement.filterMsgElement = filterMsgElement;
    
    log("createFilterMsgElement success");
    return msgElement;
}

// ========== 使用示例 ==========
String videoPath = pluginPath + "/e.mp4";
MsgElement result = createFilterMsgElement(videoPath);
if (result != null) {
    log("视频 MsgElement 创建成功！" + result.toString());
    ArrayList elemt = new ArrayList();
    elemt.add(result);
    MsgTool.sendMsg(2, "935100470", elemt);
} else {
    log("视频 MsgElement 创建失败！");
}