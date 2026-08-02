package me.lengyu.qedge.hook.api;

import android.util.Base64;

import com.tencent.qphone.base.remote.FromServiceMsg;

import me.lengyu.qedge.hook.annotation.HookItemAnnotation;
import me.lengyu.qedge.hook.base.BaseApiHookItem;
import me.lengyu.qedge.hook.base.Listener;
import me.lengyu.qedge.utils.HookUtils;
import me.lengyu.qedge.utils.LogUtils;
import me.lengyu.qedge.utils.ModuleConfig;
import me.lengyu.qedge.utils.ReflectUtils;
import me.lengyu.qedge.utils.json.ProtoData;
import me.lengyu.qedge.hook.item.QZoneLikeTool;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@HookItemAnnotation(value = "拦截QQ空间推送", category = "api")
public class OnQZonePush extends BaseApiHookItem<OnQZonePush.QZonePushListener> {

    public static final OnQZonePush INSTANCE = new OnQZonePush();

    private static final String TAG = "OnQZonePush";

    @Override
    public void loadHook() {
        try {
            Class<?> apiImplClass = null;
            try {
                apiImplClass = Class.forName("com.tencent.qzonehub.api.impl.QZonePushApiImpl");
            } catch (ClassNotFoundException e) {
                LogUtils.e(TAG, "QZonePushApiImpl class not found: " + e.getMessage());
                return;
            }

            for (Method m : apiImplClass.getDeclaredMethods()) {
                if ("onHandlePushMsg".equals(m.getName())) {
                    HookUtils.hookBefore(m, param -> {
                        try {
                            Object baseQQAppInterface = param.args[0];
                            Long pushId = (Long) param.args[1];
                            byte[] data = (byte[]) param.args[2];

                            if (data != null && data.length > 0) {
                                try {
                                    ProtoData proto = new ProtoData();
                                    proto.fromBytes(data);
                                } catch (Throwable ignored) {
                                }
                                try {
                                    String str = new String(data, "UTF-8");
                                } catch (Throwable ignored) {}
                            }
                        } catch (Throwable e) {
                            LogUtils.e(TAG, "onHandlePushMsg hook error: " + e.getMessage());
                        }
                    });
                }

                if ("handleSilentPush".equals(m.getName())) {
                    HookUtils.hookBefore(m, param -> {
                        try {
                            String uin = (String) param.args[0];
                            @SuppressWarnings("unchecked")
                            Map<String, String> map = (Map<String, String>) param.args[1];

                        } catch (Throwable e) {
                            LogUtils.e(TAG, "handleSilentPush hook error: " + e.getMessage());
                        }
                    });
                }
            }
        } catch (Throwable e) {
            LogUtils.e(TAG, "loadHook error: " + e.getMessage());
        }

        try {
            Class<?> messageMicroClass = Class.forName("com.tencent.mobileqq.pb.MessageMicro");
            for (Method m : messageMicroClass.getDeclaredMethods()) {
                if ("mergeFrom".equals(m.getName()) && m.getParameterTypes().length == 1 && m.getParameterTypes()[0] == byte[].class) {
                    HookUtils.hookAfter(m, param -> {
                        try {
                            Object thisObj = param.thisObject;
                            String className = thisObj.getClass().getName();
                            
                            if (className.contains("QZMomentReader$StGetFeedListRsp") || className.contains("StGetFeedListRsp")) {
                                try {
                                    java.lang.reflect.Field vecFeedField = findField(thisObj.getClass(), "vecFeed");
                                    if (vecFeedField != null) {
                                        vecFeedField.setAccessible(true);
                                        Object vecFeed = vecFeedField.get(thisObj);
                                        if (vecFeed != null) {
                                            Method getMethod = vecFeed.getClass().getMethod("get", int.class);
                                            Method sizeMethod = vecFeed.getClass().getMethod("size");
                                            int size = (int) sizeMethod.invoke(vecFeed);
                                            for (int i = 0; i < size; i++) {
                                                Object feed = getMethod.invoke(vecFeed, i);
                                            }
                                        }
                                    }
                                } catch (Throwable e) {
                                    LogUtils.e(TAG, "parse feed list error: " + e.getMessage());
                                }
                            }
                        } catch (Throwable e) {
                            LogUtils.e(TAG, "mergeFrom hook error: " + e.getMessage());
                        }
                    });
                    break;
                }
            }
        } catch (Throwable e) {
            LogUtils.e(TAG, "hook MessageMicro.mergeFrom error: " + e.getMessage());
        }

        try {
            ClassLoader classLoader = ReflectUtils.hostClassLoader;
            if (classLoader == null) {
                classLoader = OnQZonePush.class.getClassLoader();
            }
            Class<?> msfServletClass = Class.forName("mqq.app.MSFServlet", false, classLoader);
            Method onReceiveMethod = msfServletClass.getDeclaredMethod("onReceive", FromServiceMsg.class);
            HookUtils.hookAfter(onReceiveMethod, param -> {
                try {
                    FromServiceMsg fromServiceMsg = (FromServiceMsg) param.args[0];
                    if (fromServiceMsg == null) return;

                    String cmd = fromServiceMsg.getServiceCmd();
                    if (cmd == null) return;

                    if (cmd.contains("GetFriendFeeds") || cmd.contains("feeds_reader.FeedsReader")) {
                        try {
                            byte[] wupBuffer = fromServiceMsg.getWupBuffer();
                            if (wupBuffer != null && wupBuffer.length > 0) {
                                ProtoData data = new ProtoData();
                                data.fromBytes(wupBuffer);
                                JSONObject json = data.toJSON();
                                printFeedsSummary(json);
                            }
                        } catch (Throwable e2) {
                        }
                    }
                } catch (Throwable e) {
                    LogUtils.e(TAG, "MSF onReceive hook error: " + e.getMessage());
                }
            });
        } catch (Throwable e) {
            LogUtils.e(TAG, "hook MSFServlet.onReceive error: " + e.getMessage());
        }

        try {
            FromServiceMsgDispatcher.loadHook();
            FromServiceMsgDispatcher.registerListener((cmd, json, msg) -> {
                try {
                    parseOlPushMsg(json);
                } catch (Throwable e) {
                    LogUtils.e(TAG, "parse olpush error: " + e.getMessage());
                }
            });
        } catch (Throwable e) {
            LogUtils.e(TAG, "register FromServiceMsgDispatcher error: " + e.getMessage());
        }
    }

    private void parseOlPushMsg(JSONObject json) {
        // LogUtils.d(TAG, "qzone parse olpush msg: " + json.toString());
        try {
            JSONObject msg1 = json.optJSONObject("1");
            if (msg1 == null) return;
            
            JSONObject senderInfo = msg1.optJSONObject("1");
            long senderUin = 0;
            String senderNick = "";
            if (senderInfo != null) {
                senderUin = senderInfo.optLong("1", 0);
                senderNick = senderInfo.optString("2", "");
            }
            
            long receiverUin = 0;
            if (senderInfo != null) {
                receiverUin = senderInfo.optLong("5", 0);
            }
            
            JSONObject msg3 = msg1.optJSONObject("3");
            if (msg3 == null) return;
            
            JSONObject msg3_2 = msg3.optJSONObject("2");
            if (msg3_2 == null) return;
            
            JSONObject feedInfo = msg3_2.optJSONObject("5");
            if (feedInfo == null) return;
            
            int feedType = feedInfo.optInt("1", 0);
            long feedUin = feedInfo.optLong("2", 0);
            String feedKey = feedInfo.optString("3", "");
            int feedStatus = feedInfo.optInt("4", 0);
            String tips = feedInfo.optString("5", "");
            
            String content = "";
            boolean hasContent = false;
            JSONObject contentObj = feedInfo.optJSONObject("6");
            if (contentObj != null) {
                Object content1 = contentObj.opt("1");
                if (content1 instanceof String) {
                    content = (String) content1;
                    hasContent = content.length() > 0;
                } else if (content1 instanceof JSONObject) {
                    hasContent = true;
                    content = content1.toString();
                }
            }
            
            boolean isDelete = false;
            if (contentObj != null) {
                Object content1 = contentObj.opt("1");
                if (content1 instanceof String && ((String) content1).length() == 0) {
                    isDelete = true;
                }
            }
            
            String logoUrl = feedInfo.optString("8", "");
            String jumpSchema = feedInfo.optString("9", "");
            String cellid = "";
            String uinFromSchema = "";
            if (jumpSchema.length() > 0) {
                try {
                    JSONObject jumpJson = new JSONObject(jumpSchema);
                    String schema = jumpJson.optString("jump_schema", "");
                    if (schema.contains("schema=")) {
                        String base64Part = schema.substring(schema.indexOf("schema=") + 7);
                        byte[] decoded = Base64.decode(base64Part, Base64.URL_SAFE);
                        String decodedStr = new String(decoded, "UTF-8");
                        
                        if (decodedStr.contains("uin=")) {
                            int uinStart = decodedStr.indexOf("uin=") + 4;
                            int uinEnd = decodedStr.indexOf("&", uinStart);
                            if (uinEnd == -1) uinEnd = decodedStr.length();
                            uinFromSchema = decodedStr.substring(uinStart, uinEnd);
                        }
                        if (decodedStr.contains("cellid=")) {
                            int cellidStart = decodedStr.indexOf("cellid=") + 7;
                            int cellidEnd = decodedStr.indexOf("&", cellidStart);
                            if (cellidEnd == -1) cellidEnd = decodedStr.length();
                            cellid = decodedStr.substring(cellidStart, cellidEnd);
                        }
                    }
                } catch (Throwable ignored) {
                }
            }
            
            long finalUin = feedUin != 0 ? feedUin : (senderUin != 0 ? senderUin : (uinFromSchema.length() > 0 ? Long.parseLong(uinFromSchema) : 0));
            
            JSONArray actionList = feedInfo.optJSONArray("10");
            
            String action = isDelete ? "delete" : "post";

            if ("post".equals(action) && cellid.length() > 0 && finalUin != 0) {
                final String uinStr = String.valueOf(finalUin);
                final String finalCellid = cellid;
                final boolean autoLike = ModuleConfig.INSTANCE.getBoolean("qzone_auto_like", false);
                final boolean autoComment = ModuleConfig.INSTANCE.getBoolean("qzone_auto_comment", false);
                final String commentText = ModuleConfig.INSTANCE.getString("qzone_comment_text", "我来暖说说啦！");

                if (autoLike || autoComment) {
                    new Thread(() -> {
                        try {
                            Thread.sleep(1000);
                            if (autoLike) {
                                boolean likeResult = QZoneLikeTool.INSTANCE.doLikeByUinAndCellid(uinStr, finalCellid, 1);
                                // LogUtils.e(TAG, "auto like result: " + likeResult + ", uin=" + uinStr + ", cellid=" + finalCellid);
                            }
                            if (autoComment && commentText.length() > 0) {
                                boolean commentResult = QZoneLikeTool.INSTANCE.doComment(uinStr, finalCellid, commentText, false);
                                // LogUtils.e(TAG, "auto comment result: " + commentResult + ", uin=" + uinStr + ", cellid=" + finalCellid);
                            }
                        } catch (Throwable e) {
                            LogUtils.e(TAG, "auto action error: " + e.getMessage());
                        }
                    }).start();
                }
            }

            for (QZonePushListener listener : getListenerSet()) {
                try {
                    JSONObject result = new JSONObject();
                    result.put("action", action);
                    result.put("uin", finalUin);
                    result.put("nick", senderNick);
                    result.put("tips", tips);
                    result.put("content", content);
                    result.put("cellid", cellid);
                    result.put("feedKey", feedKey);
                    result.put("hasContent", hasContent);
                    result.put("isDelete", isDelete);
                    result.put("feedType", feedType);
                    result.put("feedStatus", feedStatus);
                    result.put("logoUrl", logoUrl);
                    result.put("receiverUin", receiverUin);
                    if (actionList != null) {
                        result.put("actionList", actionList);
                    }
                    result.put("raw", feedInfo);
                    result.put("rawFull", json);
                    listener.onPush(result);
                } catch (Throwable e) {
                    LogUtils.e(TAG, "listener error: " + e.getMessage());
                }
            }
        } catch (Throwable e) {
            LogUtils.e(TAG, "parseOlPushMsg error: " + e.getMessage());
        }
    }
    
    private static java.lang.reflect.Field findField(Class<?> clazz, String fieldName) {
        while (clazz != null && clazz != Object.class) {
            try {
                return clazz.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        return null;
    }

    private static Object getFieldValue(Class<?> clazz, Object obj, String fieldName) throws IllegalAccessException {
        java.lang.reflect.Field field = findField(clazz, fieldName);
        if (field != null) {
            field.setAccessible(true);
            return field.get(obj);
        }
        return null;
    }

    private static void printFeedsSummary(JSONObject json) {
        try {
            List<String> rootKeys = new ArrayList<>();
            Iterator<String> it = json.keys();
            while (it.hasNext()) rootKeys.add(it.next());
            
            Object feedsObj = null;
            for (String key : rootKeys) {
                Object val = json.get(key);
                if (val instanceof JSONArray) {
                    JSONArray arr = (JSONArray) val;
                    if (arr.length() > 0 && arr.get(0) instanceof JSONObject) {
                        JSONObject firstItem = arr.getJSONObject(0);
                        if (firstItem.has("id") || firstItem.has("content") || firstItem.has("poster") || firstItem.has("1") || firstItem.has("2")) {
                            feedsObj = arr;
                            break;
                        }
                    }
                }
            }
            
            if (feedsObj == null) {
                printJsonStructure(json, 0, 2);
                return;
            }
            
            JSONArray feeds = (JSONArray) feedsObj;
            int count = Math.min(feeds.length(), 3);
            
            for (int i = 0; i < count; i++) {
                JSONObject feed = feeds.getJSONObject(i);
                printJsonStructure(feed, 1, 3);
            }
        } catch (Throwable e) {
            LogUtils.e(TAG, "printFeedsSummary error: " + e.getMessage());
        }
    }
    
    private static void printJsonStructure(JSONObject obj, int indent, int maxDepth) {
        try {
            if (maxDepth <= 0) return;
            String prefix = "";
            for (int i = 0; i < indent; i++) prefix += "  ";
            
            Iterator<String> it = obj.keys();
            while (it.hasNext()) {
                String key = it.next();
                Object val = obj.get(key);
                if (val instanceof JSONObject) {
                    List<String> subKeys = new ArrayList<>();
                    Iterator<String> subIt = ((JSONObject) val).keys();
                    while (subIt.hasNext()) subKeys.add(subIt.next());
                    if (maxDepth > 1) {
                        printJsonStructure((JSONObject) val, indent + 1, maxDepth - 1);
                    }
                } else if (val instanceof JSONArray) {
                    JSONArray arr = (JSONArray) val;
                    if (arr.length() > 0 && maxDepth > 1) {
                        Object first = arr.get(0);
                        if (first instanceof JSONObject) {
                            List<String> subKeys = new ArrayList<>();
                            Iterator<String> subIt = ((JSONObject) first).keys();
                            while (subIt.hasNext()) subKeys.add(subIt.next());
                            if (maxDepth > 2) {
                                printJsonStructure((JSONObject) first, indent + 2, maxDepth - 2);
                            }
                        }
                    }
                } else {
                    String strVal = String.valueOf(val);
                    if (strVal.length() > 100) strVal = strVal.substring(0, 100) + "...";
                }
            }
        } catch (Throwable e) {
            LogUtils.e(TAG, "printJsonStructure error: " + e.getMessage());
        }
    }

    private static String bytesToHex(byte[] bytes) {
        if (bytes == null) return "null";
        StringBuilder sb = new StringBuilder();
        int len = Math.min(bytes.length, 200);
        for (int i = 0; i < len; i++) {
            sb.append(String.format("%02x", bytes[i] & 0xff));
        }
        if (bytes.length > len) {
            sb.append("...(").append(bytes.length).append(" bytes total)");
        }
        return sb.toString();
    }

    public interface QZonePushListener extends Listener {
        void onPush(Object push);
    }
}
