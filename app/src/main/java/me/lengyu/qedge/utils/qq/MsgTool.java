package me.lengyu.qedge.utils.qq;

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.tencent.qqnt.kernel.nativeinterface.MsgElement;
import com.tencent.qqnt.kernel.nativeinterface.PttElement;
import com.tencent.qqnt.kernel.nativeinterface.ArkElement;
import com.tencent.qqnt.kernelpublic.nativeinterface.Contact;
import com.tencent.qqnt.kernelpublic.nativeinterface.JsonGrayElement;
import com.tencent.qqnt.msg.api.impl.MsgUtilApiImpl;
import com.tencent.qqnt.kernel.nativeinterface.MarkdownElement;
import com.tencent.mobileqq.paiyipai.PaiYiPaiHandler;
import com.tencent.mobileqq.app.CardHandler;
import com.tencent.mobileqq.qroute.QRoute;
import com.tencent.mobileqq.ptt.impl.QQRecorderUtilsImpl;
import com.tencent.qqnt.kernel.nativeinterface.IForwardOperateCallback;
import me.lengyu.qedge.utils.HttpUtils;
import me.lengyu.qedge.utils.LogUtils;
import me.lengyu.qedge.utils.ReflectUtils;
import me.lengyu.qedge.utils.qq.QQCurrentEnv;
import kotlin.math.MathKt;

/**
 * @Author 冷雨
 * @Description 消息工具类
 */
public class MsgTool {

    public static MsgUtilApiImpl msgUtilApiImpl;

    public static MsgUtilApiImpl getMsgUtilApi() {
        if (msgUtilApiImpl == null) {
            try {
                msgUtilApiImpl = new MsgUtilApiImpl();
                } catch (Throwable e) {
                    LogUtils.e("MsgTool", "[getMsgUtilApi] reflection failed: " + e.getMessage());
                }
            }
        return msgUtilApiImpl;
    }

    public static Method sendPaiMethod;

    static {
        try {
            sendPaiMethod = ReflectUtils.findMethodOrNull(PaiYiPaiHandler.class, void.class,
                    new Class[]{String.class, String.class, int.class, int.class});
            if (sendPaiMethod == null) {
                sendPaiMethod = ReflectUtils.findMethod(PaiYiPaiHandler.class, void.class,
                        new Class[]{int.class, int.class, String.class, String.class});
            }
        } catch (Throwable e) {
            LogUtils.e(e);
        }
    }

    public static void sendMsg(String peerUin, String msg, int chatType) {
        sendMsg(makeContact(peerUin, chatType), msg);
    }

    public static void sendMsg(Contact contact, String msg) {
        try {
            ArrayList<MsgElement> elements = processMessageContent(contact, msg);
            sendMsgInternal(contact, elements);
        } catch (Throwable e) {
            LogUtils.e("MsgTool", "[sendMsg] error: " + e.getMessage());
        }
    }

    public static void sendMsg(Contact contact, ArrayList<MsgElement> elements) {
        sendMsgInternal(contact, elements);
    }

    public static void sendMsg(int chatType, String peerUin, ArrayList<MsgElement> elements) {
        sendMsgInternal(makeContact(peerUin, chatType), elements);
    }

    public static void sendMsgInternal(Contact contact, ArrayList<MsgElement> elements) {
        if (elements == null || elements.isEmpty()) {
            return;
        }
        try {
            Object msgService = getMsgServiceViaReflection();
            if (msgService == null) {
                throw new RuntimeException("未获取到msgService");
            }

            long msgId = generateMsgUniqueId(msgService, contact.chatType);
            java.util.HashMap<Integer, Object> attributeMap = new java.util.HashMap<>();
            
            Method sendMsgMethod = ReflectUtils.findMethod(msgService.getClass(), "sendMsg", 5);
            if (sendMsgMethod != null) {
                sendMsgMethod.invoke(msgService, msgId, contact, elements, attributeMap, null);
            } else {
                sendMsgMethod = ReflectUtils.findMethod(msgService.getClass(), "sendMsg", 3);
                if (sendMsgMethod != null) {
                    sendMsgMethod.invoke(msgService, contact, elements, null);
                } else {
                    LogUtils.e("MsgTool", "[sendMsgInternal] sendMsg method not found");
                }
            }
        } catch (Throwable e) {
            LogUtils.e("MsgTool", "[sendMsgInternal] error: " + e.getMessage());
        }
    }

    public static void forwardMsg(Contact contact, ArrayList<MsgElement> elements) {
        if (elements == null || elements.isEmpty()) {
            return;
        }
        try {
            Object msgService = getMsgServiceViaReflection();
            if (msgService == null) {
                throw new RuntimeException("未获取到msgService");
            }

            long msgId = generateMsgUniqueId(msgService, contact.chatType);

            java.util.ArrayList<Long> forwardIds = new java.util.ArrayList<Long>();
            forwardIds.add(msgId);

            java.util.ArrayList<Contact> contacts = new java.util.ArrayList<Contact>();
            contacts.add(contact);
            
            Method forwardMsgMethod = ReflectUtils.findMethod(msgService.getClass(), "forwardMsg", 5);
            if (forwardMsgMethod != null) {
                forwardMsgMethod.invoke(msgService, forwardIds, contact, contacts, elements, new IForwardOperateCallback() {
                    @Override
                    public void onResult(int i, String str, HashMap<Long, Integer> hashMap) {
                        LogUtils.d("MsgTool", "[forwardMsg] onResult: " + i + ", " + str + ", " + hashMap);
                    }
                });
            } else {
                LogUtils.e("MsgTool", "[forwardMsg] forwardMsg method not found");
            }
        } catch (Throwable e) {
            LogUtils.e("MsgTool", "[forwardMsg] error: " + e.getMessage());
        }
    }


     public static Object getMsgServiceViaReflection() {
        try {
            Object appInterface = QQCurrentEnv.getQQAppInterface();
            if (appInterface == null) {
                return null;
            }

            ClassLoader qqClassLoader = appInterface.getClass().getClassLoader();

            Class<?> qRouteClass = qqClassLoader.loadClass("com.tencent.mobileqq.qroute.QRoute");
            Method apiMethod = qRouteClass.getDeclaredMethod("api", Class.class);
            apiMethod.setAccessible(true);

            Class<?> iMsgServiceClass = qqClassLoader.loadClass("com.tencent.qqnt.msg.api.IMsgService");
            Object result = apiMethod.invoke(null, iMsgServiceClass);

            return result;
        } catch (Throwable e) {
            LogUtils.e("MsgTool", "[getMsgServiceViaReflection] error: " + e.getMessage());
            return null;
        }
    }

     public static long generateMsgUniqueId(Object msgService, int chatType) {
        try {
            Method generateMethod = ReflectUtils.findMethod(msgService.getClass(), "generateMsgUniqueId", 1);
            if (generateMethod != null) {
                Object result = generateMethod.invoke(msgService, chatType);
                if (result instanceof Long) {
                    return ((Long) result).longValue();
                }
            }
            return System.currentTimeMillis();
        } catch (Throwable e) {
            return System.currentTimeMillis();
        }
    }

    public static void sendPic(String peerUin, String path, int chatType) {
        sendMsgByType(peerUin, chatType, path, "pic");
    }

    public static void sendPic(Contact contact, String path) {
        sendMsgByType(contact, path, "pic");
    }

    public static void sendPtt(String peerUin, String path, int chatType) {
        sendMsgByType(peerUin, chatType, path, "ptt");
    }

    public static void sendPtt(Contact contact, String path) {
        sendMsgByType(contact, path, "ptt");
    }

    public static void sendCard(String peerUin, String data, int chatType) {
        sendMsgByType(peerUin, chatType, data, "ark");
    }

    public static void sendCard(Contact contact, String data) {
        sendMsgByType(contact, data, "ark");
    }

    public static void sendVideo(String peerUin, String path, int chatType) {
        sendMsgByType(peerUin, chatType, path, "video");
    }

    public static void sendVideo(Contact contact, String path) {
        sendMsgByType(contact, path, "video");
    }

    public static void sendFile(String peerUin, String path, int chatType) {
        sendMsgByType(peerUin, chatType, path, "file");
    }

    public static void sendFile(Contact contact, String path) {
        sendMsgByType(contact, path, "file");
    }

    public static void sendMarkDown(String peerUin, String text, int chatType) {
        sendMsgByType(peerUin, chatType, text, "md");
    }

    public static void sendMarkDown(Contact contact, String text) {
        sendMsgByType(contact, text, "md");
    }

    public static void sendReplyMsg(Contact contact, long replyMsgId, String msg) {
        try {
            MsgUtilApiImpl msgUtilApiImpl = getMsgUtilApi();
            if (msgUtilApiImpl == null) {
                throw new RuntimeException("未获取到msgUtilApi");
            }
            ArrayList<MsgElement> elements = new ArrayList<>();
            elements.add(msgUtilApiImpl.createReplyElement(replyMsgId));
            elements.addAll(processMessageContent(contact, msg));
            sendMsgInternal(contact, elements);
        } catch (Throwable e) {
            LogUtils.e(e);
        }
    }

    public static void sendReplyMsg(String peerUin, long replyMsgId, String msg, int chatType) {
        sendReplyMsg(makeContact(peerUin, chatType), replyMsgId, msg);
    }

     public static void sendMsgByType(String peerUin, int chatType, String value, String type) {
        sendMsgByType(makeContact(peerUin, chatType), value, type);
    }

     public static void sendMsgByType(Contact contact, String value, String type) {
        try {
            MsgUtilApiImpl msgUtilApiImpl = getMsgUtilApi();
            if (msgUtilApiImpl == null) {
                throw new RuntimeException("未获取到msgUtilApi");
            }

            MsgElement msgElement;
            switch (type) {
                case "pic":
                    String path = handlePicPath(value);
                    if (path == null){
                        LogUtils.e("MsgTool", "handlePicPath failed: " + value);
                        return;
                    }
                    File file = new File(path);
                    if (!file.exists()){
                        LogUtils.e("MsgTool", "file not exists: " + path);
                        return;
                    }
                    msgElement = msgUtilApiImpl.createPicElement(path, true, 0);
                    break;
                case "ptt":
                    String pttPath = handlePttPath(value);
                    if (pttPath == null){
                        LogUtils.e("MsgTool", "handlePttPath failed: " + value);
                        return;
                    }
                    File pttFile = new File(pttPath);
                    if (!pttFile.exists()){
                        LogUtils.e("MsgTool", "file not exists: " + pttPath);
                        return;
                    }
                    ArrayList<Byte> byList = new ArrayList<>();
                    byte[] b = {28, 26, 43, 29, 31, 61, 34, 49, 51, 56, 52, 74, 41, 62, 66, 46, 25, 57, 51, 70, 33, 45, 39, 27, 68, 58, 46, 59, 59, 63};
                    for (int i = 0; i < b.length; i++) {
                        byList.add(b[i]);
                    }
                    int duration = 1000;

                    if (isSilkFile(pttPath)) {
                        QQRecorderUtilsImpl qqRecorderUtilsImpl = new QQRecorderUtilsImpl();
                        duration = qqRecorderUtilsImpl.getFilePlayTime(pttPath);
                    } else {
                        android.media.MediaPlayer mp = new android.media.MediaPlayer();
                        try {
                            mp.setDataSource(pttPath);
                            mp.prepare();
                            duration = mp.getDuration() * 1000;

                        } catch (Throwable e) {
                            LogUtils.e("MsgTool", "MediaPlayer获取ptt时长失败: " + pttPath);
                            long fileSize = pttFile.length();
                            duration = (int) (fileSize / 1000) * 1000;
                            if (duration < 1000) duration = 1000;
                        } finally {
                            try { mp.release(); } catch (Throwable ignored) {}
                        }
                    }
                    
                    MsgElement msgElements = msgUtilApiImpl.createPttElement(pttPath, 0);   
                    PttElement pttElement = msgElements.getPttElement();
                    pttElement.waveAmplitudes = byList;
                    pttElement.duration = (int) MathKt.roundToInt(duration / 1000.0f);
                    msgElement = msgElements;
                    break;
                case "video":
                    String videoPath = handleVideoPath(value);
                    if (videoPath == null){
                        LogUtils.e("MsgTool", "handleVideoPath failed: " + value);
                        return;
                    }
                    File videoFile = new File(videoPath);
                    if (!videoFile.exists()){
                        LogUtils.e("MsgTool", "file not exists: " + videoPath);
                        return;
                    }
                    if (videoFile.length() >= 200L * 1024 * 1024) {
                        LogUtils.d("MsgTool", "视频体积" + (videoFile.length() / 1024 / 1024) + "MB>=200MB，转为文件发送");
                        msgElement = msgUtilApiImpl.createFileElement(videoPath);
                    } else {
                        msgElement = msgUtilApiImpl.createVideoElement(videoPath);
                    }
                    break;
                case "file":
                    String filePath = handleFilePath(value);
                    if (filePath == null){
                        LogUtils.e("MsgTool", "handleFilePath failed: " + value);
                        return;
                    }
                    File file2 = new File(filePath);
                    if (!file2.exists()){
                            LogUtils.e("MsgTool", "file not exists: " + filePath);
                        return;
                    }
                    msgElement = msgUtilApiImpl.createFileElement(filePath);    
                    break;
                case "ark":
                    ArkElement arkElement = new ArkElement();
                    arkElement.bytesData = value;
    
                    msgElement = new MsgElement();
                    msgElement.elementType = 10;
                    msgElement.arkElement = arkElement; 
                    break;
                case "md":
                    msgElement = new MsgElement();
                    MarkdownElement markDownElement = new MarkdownElement();
                    markDownElement.content = value;
                    msgElement.markdownElement = markDownElement;
                    msgElement.elementType = 14;
                    break;
                default:
                    msgElement = msgUtilApiImpl.createTextElement(value);
            }
            ArrayList<MsgElement> elements = new ArrayList<>();
            elements.add(msgElement);
            sendMsgInternal(contact, elements);
        } catch (Throwable e) {
            LogUtils.e(e);
        }
    }

     public static String handlePicPath(String path) {
        if (path.startsWith("http")) {
            String savePath = QQCurrentEnv.getCurrentDir() + "cache/images/" ;

            String picPath = HttpUtils.download(path, savePath);
            if (new File(picPath).exists()){
                return picPath;
            }
            LogUtils.e("MsgTool", new Throwable("下载图片失败: " + path));
            return null;
        }
        return new File(path).exists() ? path : null;
    }

     public static String handlePttPath(String path) {
        if (path.startsWith("http")) {
            String savePath = QQCurrentEnv.getCurrentDir() + "cache/ptt/" ;

            String pttPath = HttpUtils.download(path, savePath);
            if (new File(pttPath).exists()){
                return pttPath;
            }
            LogUtils.e("MsgTool", new Throwable("下载语音失败: " + path));
            return null;
        }
        return new File(path).exists() ? path : null;
    }

     public static String handleVideoPath(String path) {
        if (path.startsWith("http")) {
            String savePath = QQCurrentEnv.getCurrentDir() + "cache/video/" ;

            String videoPath = HttpUtils.download(path, savePath);
            if (new File(videoPath).exists()){
                return videoPath;
            }
            LogUtils.e("MsgTool", new Throwable("下载视频失败: " + path));
            return null;
        }
        return new File(path).exists() ? path : null;
    }

     public static String handleFilePath(String path) {
        if (path.startsWith("http")) {
            String savePath = QQCurrentEnv.getCurrentDir() + "cache/file/" ;

            String filePath = HttpUtils.download(path, savePath);
            if (new File(filePath).exists()){
                return filePath;
            }
            LogUtils.e("MsgTool", new Throwable("下载文件失败: " + path));
            return null;
        }
        return new File(path).exists() ? path : null;
    }

    public static ArrayList<MsgElement> processMessageContent(Contact contact, String msg) {
        ArrayList<MsgElement> elements = new ArrayList<>();
        try {
            MsgUtilApiImpl msgUtilApiImpl = getMsgUtilApi();
            if (msgUtilApiImpl == null) {
                throw new RuntimeException("未获取到msgUtilApi");
            }

            List<Pair<String, String>> parts = processMessageParts(msg);

            for (Pair<String, String> part : parts) {
                String type = part.first;
                String value = part.second;

                switch (type) {
                    case "text":
                        MsgElement textElement = msgUtilApiImpl.createTextElement(value);
                        elements.add(textElement);
                        break;
                    case "atUin":
                        int chatType = contact.chatType;
                        if (chatType == 2) {
                            int atType = value.equals("0") ? 1 : 2;
                            String uid = value.equals("0") ? "0" : FriendTool.getUidFromUin(value);
                            MsgElement atElement = msgUtilApiImpl.createAtTextElement("@全体成员", uid, atType);
                            elements.add(atElement);
                        }
                        break;
                    case "pic":
                        String picPath = handlePicPath(value);
                        if (picPath != null) {
                            MsgElement picElement = msgUtilApiImpl.createPicElement(picPath, true, 0);
                            elements.add(picElement);
                        } else {
                            elements.add(msgUtilApiImpl.createTextElement("[图片下载失败]"));
                        }
                        break;
                }
            }
        } catch (Throwable e) {
            LogUtils.e("MsgTool", "[processMessageContent] error: " + e.getMessage());
        }
        return elements;
    }

     public static List<Pair<String, String>> processMessageParts(String input) {
        List<Pair<String, String>> result = new ArrayList<>();
        int lastEnd = 0;

        Pattern pattern = Pattern.compile("\\[(atUin|pic)=([^]]*)]");
        Matcher matcher = pattern.matcher(input);

        while (matcher.find()) {
            if (matcher.start() > lastEnd) {
                result.add(new Pair<>("text", input.substring(lastEnd, matcher.start())));
            }
            String type = matcher.group(1);
            String value = matcher.group(2);
            result.add(new Pair<>(type, value));
            lastEnd = matcher.end();
        }

        if (lastEnd < input.length()) {
            result.add(new Pair<>("text", input.substring(lastEnd)));
        }

        return result;
    }

    public static void sendPai(String toUin, String peerUin, int chatType) {
        try {
            PaiYiPaiHandler handler = QQServiceHelper.getHandler(PaiYiPaiHandler.class);
            if (handler == null || sendPaiMethod == null) return;

            try {
                sendPaiMethod.invoke(handler, toUin, peerUin, chatType, 1);
            } catch (Throwable e) {
                sendPaiMethod.invoke(handler, chatType, 1, toUin, peerUin);
            }
        } catch (Throwable e) {
            LogUtils.e(e);
        }
    }

    public static void recallMsg(int chatType, String peerUin, long msgId) {
        recallMsg(makeContact(peerUin, chatType), msgId);
    }

    public static void recallMsg(Contact contact, long msgId) {
        try {
            Object service = getMsgServiceViaReflection();
            if (service != null) {
                ReflectUtils.callMethod(service, "recallMsg", contact, msgId, new com.tencent.qqnt.kernel.nativeinterface.IOperateCallback() {
                    @Override
                    public void onResult(int i, String str) {
                        if (i == 0) {
                             LogUtils.i("MsgTool", "recallMsg success: " + msgId);
                        } else {
                            LogUtils.e("MsgTool", "recallMsg failed: " + msgId + ", " + str);
                        }
                    }
                });
            }
        } catch (Throwable e) {
            LogUtils.e(e);
        }
    }

    public static void addLocalGrayTipMsg(Contact contact, String jsonStr, long busiId) {
        try {
            JsonGrayElement grayElement = new JsonGrayElement(busiId, jsonStr, "", false, null);

            Object service = QQCurrentEnv.getKernelMsgService();
            if (service != null) {
                ReflectUtils.callMethod(service, "addLocalJsonGrayTipMsg", contact, grayElement, true, true, null);
            }
        } catch (Throwable e) {
            LogUtils.e("NtGrayTip", e);
        }
    }

    public static Contact makeContact(String peerUin, int chatType) {
        try {
            String peerUid;
            if (chatType == 1 || chatType == 100) {
                if(FriendTool.isValidUin(peerUin)){
                    peerUid = FriendTool.getUidFromUin(peerUin);
                } else {
                    peerUid = peerUin;
                }
            } else if (chatType == 2) {
                peerUid = peerUin;
            } else {
                throw new IllegalStateException("不支持的聊天类型: " + chatType);
            }
            return new Contact(chatType, peerUid, "");
        } catch (Throwable e) {
            LogUtils.e("MsgTool", "[makeContact] error: " + e.getMessage());
            return null;
        }
    }

    public static void sendBubbleVideo(String peerUin, String videoPath, int chatType) {
        sendBubbleVideo(makeContact(peerUin, chatType), videoPath);
    }

    /**
     * 仅创建【泡泡视频 MsgElement】，不调用发送 —— 用于直接注入到原本就要发送的 elements list 中
     *  （elementType=49 + FilterMsgElement(busiType=1, subBusiType=1, fileFormat=2)）
     *  @return 成功返回 MsgElement，失败返回 null
     */
    public static MsgElement createBubbleVideoElement(String videoPath) {
        try {
            String realPath = handleVideoPath(videoPath);
            if (realPath == null) {
                LogUtils.e("MsgTool", "泡泡视频路径无效: " + videoPath);
                return null;
            }

            File videoFile = new File(realPath);
            if (!videoFile.exists()) {
                LogUtils.e("MsgTool", "泡泡视频文件不存在: " + videoPath);
                return null;
            }

            Object kernelService = QQCurrentEnv.getKernelMsgService();
            if (kernelService == null) {
                LogUtils.e("MsgTool", "泡泡视频: kernelService为null");
                return null;
            }

            String md5 = com.tencent.qqnt.kernel.nativeinterface.QQNTWrapperUtil.CppProxy.genFileMd5Hex(realPath);
            if (md5 == null || md5.isEmpty()) {
                LogUtils.e("MsgTool", "泡泡视频: 计算md5失败");
                return null;
            }
            String fileName = videoFile.getName();

            // 获取视频目标路径
            com.tencent.qqnt.kernel.nativeinterface.RichMediaFilePathInfo videoInfo =
                new com.tencent.qqnt.kernel.nativeinterface.RichMediaFilePathInfo(5, 2, md5, fileName, 1, 0, null, "", true);
            String destPath = (String) kernelService.getClass()
                .getMethod("getRichMediaFilePathForMobileQQSend", com.tencent.qqnt.kernel.nativeinterface.RichMediaFilePathInfo.class)
                .invoke(kernelService, videoInfo);

            if (destPath == null || destPath.isEmpty()) {
                LogUtils.e("MsgTool", "泡泡视频: 获取目标路径失败");
                return null;
            }

            if (!com.tencent.qqnt.kernel.nativeinterface.QQNTWrapperUtil.CppProxy.fileIsExist(destPath)) {
                if (!com.tencent.qqnt.kernel.nativeinterface.QQNTWrapperUtil.CppProxy.copyFile(realPath, destPath)) {
                    LogUtils.e("MsgTool", "泡泡视频: 复制文件失败");
                    return null;
                }
            }

            // 生成缩略图
            String thumbPath = null;
            com.tencent.qqnt.kernel.nativeinterface.RichMediaFilePathInfo thumbInfo =
                new com.tencent.qqnt.kernel.nativeinterface.RichMediaFilePathInfo(5, 1, md5, fileName, 2, 0, null, "", true);
            String thumbDestPath = (String) kernelService.getClass()
                .getMethod("getRichMediaFilePathForMobileQQSend", com.tencent.qqnt.kernel.nativeinterface.RichMediaFilePathInfo.class)
                .invoke(kernelService, thumbInfo);

            if (thumbDestPath != null && !thumbDestPath.isEmpty()) {
                if (com.tencent.qqnt.kernel.nativeinterface.QQNTWrapperUtil.CppProxy.fileIsExist(thumbDestPath)) {
                    thumbPath = thumbDestPath;
                } else {
                    android.media.MediaMetadataRetriever retriever = null;
                    java.io.FileOutputStream fos = null;
                    try {
                        retriever = new android.media.MediaMetadataRetriever();
                        retriever.setDataSource(realPath);
                        android.graphics.Bitmap frame = retriever.getFrameAtTime();
                        if (frame != null) {
                            fos = new java.io.FileOutputStream(thumbDestPath);
                            frame.compress(android.graphics.Bitmap.CompressFormat.JPEG, 60, fos);
                            fos.flush();
                            thumbPath = thumbDestPath;
                        }
                    } catch (Throwable e) {
                        LogUtils.e("MsgTool", "泡泡视频: 生成缩略图失败: " + e.getMessage());
                    } finally {
                        try { if (fos != null) fos.close(); } catch (Throwable ignored) {}
                        try { if (retriever != null) retriever.release(); } catch (Throwable ignored) {}
                    }
                }
            }

            int thumbWidth = 720, thumbHeight = 720;
            if (thumbPath != null) {
                android.graphics.BitmapFactory.Options opts = new android.graphics.BitmapFactory.Options();
                opts.inJustDecodeBounds = true;
                android.graphics.BitmapFactory.decodeFile(thumbPath, opts);
                if (opts.outWidth > 0) thumbWidth = opts.outWidth;
                if (opts.outHeight > 0) thumbHeight = opts.outHeight;
            }

            // 获取视频时长
            int fileTime = 91;
            android.media.MediaMetadataRetriever durationRetriever = null;
            try {
                durationRetriever = new android.media.MediaMetadataRetriever();
                durationRetriever.setDataSource(realPath);
                String durationStr = durationRetriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION);
                if (durationStr != null) {
                    fileTime = (int) (Long.parseLong(durationStr) / 1000);
                }
            } catch (Throwable e) {
                LogUtils.e("MsgTool", "泡泡视频: 获取时长失败，使用默认值");
            } finally {
                try { if (durationRetriever != null) durationRetriever.release(); } catch (Throwable ignored) {}
            }

            // 构建 FilterMsgElement
            com.tencent.qqnt.kernel.nativeinterface.FilterMsgElement filterMsgElement = new com.tencent.qqnt.kernel.nativeinterface.FilterMsgElement();
            filterMsgElement.filePath = destPath;
            filterMsgElement.videoMd5 = md5;
            filterMsgElement.fileSize = com.tencent.qqnt.kernel.nativeinterface.QQNTWrapperUtil.CppProxy.getFileSize(destPath);
            filterMsgElement.fileName = fileName;
            filterMsgElement.fileFormat = 2;
            filterMsgElement.busiType = 1;
            filterMsgElement.subBusiType = 1;
            filterMsgElement.fileTime = fileTime;
            filterMsgElement.thumbWidth = thumbWidth;
            filterMsgElement.thumbHeight = thumbHeight;
            filterMsgElement.original = true;
            filterMsgElement.videoFrom = 1;

            if (thumbPath != null) {
                filterMsgElement.thumbMd5 = com.tencent.qqnt.kernel.nativeinterface.QQNTWrapperUtil.CppProxy.genFileMd5Hex(thumbPath);
                filterMsgElement.thumbSize = (int) com.tencent.qqnt.kernel.nativeinterface.QQNTWrapperUtil.CppProxy.getFileSize(thumbPath);
                HashMap<Integer, String> thumbMap = new HashMap<>();
                thumbMap.put(0, thumbPath);
                filterMsgElement.thumbPath = thumbMap;
            }

            MsgElement msgElement = new MsgElement();
            msgElement.elementType = 49;
            msgElement.filterMsgElement = filterMsgElement;
            return msgElement;

        } catch (Throwable e) {
            LogUtils.e("MsgTool", "createBubbleVideoElement 失败: " + e.getMessage());
            return null;
        }
    }

    public static void sendBubbleVideo(Contact contact, String videoPath) {
        try {
            String localPath = handleVideoPath(videoPath);
            if (localPath == null) {
                LogUtils.e("MsgTool", "泡泡视频路径无效: " + videoPath);
                return;
            }
            File videoFile = new File(localPath);
            if (!videoFile.exists()) {
                LogUtils.e("MsgTool", "泡泡视频文件不存在: " + videoPath);
                return;
            }
            if (videoFile.length() >= 200L * 1024 * 1024) {
                LogUtils.d("MsgTool", "泡泡视频体积" + (videoFile.length() / 1024 / 1024) + "MB>=200MB，转为文件发送");
                sendFile(contact, localPath);
                return;
            }
            MsgElement bubbleElement = createBubbleVideoElement(localPath);
            if (bubbleElement == null) {
                return;
            }
            ArrayList<MsgElement> elements = new ArrayList<>();
            elements.add(bubbleElement);
            sendMsgInternal(contact, elements);
        } catch (Throwable e) {
            LogUtils.e("MsgTool", "泡泡视频发送失败: " + e.getMessage());
        }
    }

    public static class Pair<K, V> {
        public final K first;
        public final V second;
        public Pair(K first, V second) {
            this.first = first;
            this.second = second;
        }
    }

    /**
     * 判断是否为 silk 音频（不看后缀）：文件头 "#!SILK_V3"，
     * 兼容 QQ 导出的带 0x02 前缀的 10 字节头。
     */
    private static boolean isSilkFile(String path) {
        try (FileInputStream input = new FileInputStream(path)) {
            byte[] header = new byte[9];
            if (input.read(header) != 9) return false;
            String ascii = new String(header, StandardCharsets.US_ASCII);
            if ("#!SILK_V3".equals(ascii)) return true;
            return header[0] == 0x02 && "#!SILK_V3".equals(ascii.substring(1));
        } catch (Throwable e) {
            return path.toLowerCase().endsWith(".silk");
        }
    }
}
