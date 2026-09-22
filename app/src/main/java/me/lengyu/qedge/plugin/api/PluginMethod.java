package me.lengyu.qedge.plugin.api;

import android.app.Activity;
import android.content.Intent;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;
import me.lengyu.qedge.lifecycle.DynamicActivityRegistry;
import me.lengyu.qedge.plugin.PluginCompiler;
import me.lengyu.qedge.plugin.bean.ForbidInfo;
import me.lengyu.qedge.plugin.bean.FriendInfo;
import me.lengyu.qedge.plugin.bean.GroupInfo;
import me.lengyu.qedge.plugin.bean.MemberInfo;
import me.lengyu.qedge.utils.HttpUtils;
import me.lengyu.qedge.utils.JsonConfigUtils;
import me.lengyu.qedge.utils.JarLoader;
import me.lengyu.qedge.utils.LogUtils;
import me.lengyu.qedge.utils.Toasts;
import me.lengyu.qedge.utils.HostInfo;
import me.lengyu.qedge.utils.qq.CookieTool;
import me.lengyu.qedge.utils.qq.FriendTool;
import me.lengyu.qedge.utils.qq.MsgTool;
import me.lengyu.qedge.utils.qq.QQCurrentEnv;
import me.lengyu.qedge.utils.qq.TroopTool;
import me.lengyu.qedge.utils.qq.ExtraTool;
import me.lengyu.qedge.utils.json.ProtoData;
import me.lengyu.qedge.plugin.bean.BlackUser;
import me.lengyu.qedge.plugin.bean.JointGroup;
/**
 * @Author 冷雨
 * @Description Java插件方法类
 */
public class PluginMethod {
    private final PluginCompiler compiler;

    public PluginMethod(PluginCompiler compiler) {
        this.compiler = compiler;
    }

    private String getConfigPath() {
        return compiler.info.getDirPath() + "/config/";
    }

    public void log(String fileName, String msg) {
        runWithErrorHandle(() -> {
            File dir = new File(compiler.info.getDirPath());
            if (!dir.exists()) dir.mkdirs();
            File logFile = new File(dir, fileName != null ? fileName : "log.txt");
            try {
                java.io.FileWriter writer = new java.io.FileWriter(logFile, true);
                writer.write(msg + "\n");
                writer.close();
            } catch (java.io.IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void log(String msg) {
        log("log.txt", msg);
    }

    public Activity getNowActivity() {
        return (Activity) runWithErrorHandle(() -> {
            return QQCurrentEnv.getActivity();
        });
    }

    public void toast(Object msg) {
        Toasts.toast(msg != null ? msg.toString() : "null");
    }

    public void toastLong(Object msg) {
        Toasts.toast(msg != null ? msg.toString() : "null");
    }

    public void qqToast(int icon, Object msg) {
        Toasts.qqToast(icon, msg != null ? msg.toString() : "null");
    }

    public void addItem(String name, String callback) {
        compiler.getMenuItems().put(name, callback);
    }

    public void addMenuItem(String name, String callback) {
        addMenuItem(name, callback, null);
    }

    public void addMenuItem(String name, String callback, int[] msgTypes) {
        String types = "";
        if (msgTypes != null) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < msgTypes.length; i++) {
                if (i > 0) sb.append(",");
                sb.append(msgTypes[i]);
            }
            types = sb.toString();
        }
        String item = "[QEdge]," + compiler.info.getId() + "," + name + "," + callback + "," + types;
        compiler.addMenuItem(item);
    }

    public void loadJava(String path) {
        runWithErrorHandle(() -> {
            try {
                compiler.getInterpreter().source(path);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void loadJar(String path) {
        runWithErrorHandle(() -> {
            try {
                java.lang.ClassLoader loader = JarLoader.loadJar(path);
                compiler.getLoader().addClassLoader(loader);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void loadDex(String path) {
        loadJar(path);
    }

    public void registerActivity(Class<?> activityClass) {
        runWithErrorHandle(() -> {
            if (!Activity.class.isAssignableFrom(activityClass)) {
                throw new RuntimeException("注册类必须是Activity的实现类");
            }
            DynamicActivityRegistry.register(activityClass);
            compiler.registerActivity(activityClass.getName());
        });
    }

    public String getPluginPath() {
        return compiler.info.getDirPath();
    }

    public String getPluginId() {
        return compiler.info.getId();
    }

    public String getCurrentUin() {
        return runWithErrorHandle(QQCurrentEnv::getCurrentUin);
    }

    public String getCurrentName() {
        return runWithErrorHandle(QQCurrentEnv::getCurrentName);
    }

    public String getCurrentUid() {
        return runWithErrorHandle(QQCurrentEnv::getCurrentUid);
    }

    public List<FriendInfo> getAllFriend() {
        return runWithErrorHandle(FriendTool::getAllFriend);
    }

    public boolean isFriend(String uin) {
        return runWithErrorHandle(() -> FriendTool.isFriend(uin));
    }

    public void sendZan(String uin, int num) {
        runWithErrorHandle(() -> FriendTool.sendZan(uin, num));
    }

    public List<GroupInfo> getGroupList() {
        return runWithErrorHandle(() -> TroopTool.INSTANCE.getGroupList());
    }

    public Object getGroupInfo(String group) {
        return runWithErrorHandle(() -> TroopTool.INSTANCE.getGroupInfo(group));
    }

    public void shutUpAll(String group, boolean enable) {
        runWithErrorHandle(() -> TroopTool.INSTANCE.shutUpAll(group, enable));
    }

    public void shutUp(String group, String uin, long time) {
        runWithErrorHandle(() -> TroopTool.INSTANCE.shutUp(group, uin, time));
    }

    public void setGroupAdmin(String group, String uin, boolean enable) {
        runWithErrorHandle(() -> TroopTool.INSTANCE.setGroupAdmin(group, uin, enable));
    }

    public void setMemberTitle(String group, String uin, String title) {
        runWithErrorHandle(() -> ExtraTool.setMemberTitle(group, uin, title));
    }

    public void changeGroupName(String group, String name) {
        runWithErrorHandle(() -> ExtraTool.changeGroupName(group, name));
    }

    public void kickGroup(String group, String uin, boolean block) {
        runWithErrorHandle(() -> TroopTool.INSTANCE.kickGroup(group, uin, block));
    }

    public void clockIn(String group) {
        runWithErrorHandle(() -> ExtraTool.groupClockIn(group));
    }

    public List<MemberInfo> getGroupMemberList(String group) {
        return runWithErrorHandle(() -> TroopTool.INSTANCE.getGroupMemberList(group));
    }

    public List<ForbidInfo> getForbidInfo(String group) {
        return runWithErrorHandle(() -> TroopTool.INSTANCE.getForbidInfo(group));
    }

    public void sendMsg(String peerUin, String msg, int chatType) {
        runWithErrorHandle(() -> MsgTool.sendMsg(peerUin, msg, chatType));
    }

    public void sendPic(String peerUin, String path, int chatType) {
        runWithErrorHandle(() -> MsgTool.sendPic(peerUin, path, chatType));
    }

    public void sendPtt(String peerUin, String path, int chatType) {
        runWithErrorHandle(() -> MsgTool.sendPtt(peerUin, path, chatType));
    }

    public void sendCard(String peerUin, String data, int chatType) {
        runWithErrorHandle(() -> MsgTool.sendCard(peerUin, data, chatType));
    }

    public void sendVideo(String peerUin, String path, int chatType) {
        runWithErrorHandle(() -> MsgTool.sendVideo(peerUin, path, chatType));
    }

    public void sendFile(String peerUin, String path, int chatType) {
        runWithErrorHandle(() -> MsgTool.sendFile(peerUin, path, chatType));
    }

    public void sendPai(String toUin, String peerUin, int chatType) {
        runWithErrorHandle(() -> ExtraTool.sendPai(peerUin, toUin));
    }

    public void sendPai(String peerUin, String toUin) {
        runWithErrorHandle(() -> ExtraTool.sendPai(peerUin, toUin));
    }

    public void recallMsg(int chatType, String peerUin, long msgId) {
        runWithErrorHandle(() -> MsgTool.recallMsg(chatType, peerUin, msgId));
    }

    public String getSkey() {
        return runWithErrorHandle(CookieTool::getSkey);
    }

    public String getPskey(String url) {
        return runWithErrorHandle(() -> CookieTool.getPskey(url));
    }

    public String getRealSkey() {
        return runWithErrorHandle(CookieTool::getRealSkey);
    }

    public String getStweb() {
        return runWithErrorHandle(CookieTool::getStweb);
    }

    public String getPt4Token(String url) {
        return runWithErrorHandle(() -> CookieTool.getPt4Token(url));
    }

    public String getGTK(String url) {
        return runWithErrorHandle(() -> CookieTool.getGTK(url));
    }

    public long getBkn(String key) {
        return runWithErrorHandle(() -> CookieTool.getBkn(key));
    }

    public String getGroupRKey() {
        return runWithErrorHandle(CookieTool::getGroupRKey);
    }

    public String getFriendRKey() {
        return runWithErrorHandle(CookieTool::getFriendRKey);
    }

    public String getGtk(String skey) {
        return runWithErrorHandle(() -> String.valueOf(CookieTool.getGtk(skey)));
    }

    public void putString(String configName, String key, String value) {
        runWithErrorHandle(() -> JsonConfigUtils.putString(getConfigPath(), configName, key, value));
    }

    public String getString(String configName, String key, String defaultValue) {
        return runWithErrorHandle(() -> JsonConfigUtils.getString(getConfigPath(), configName, key, defaultValue));
    }

    public void putInt(String configName, String key, int value) {
        runWithErrorHandle(() -> JsonConfigUtils.putInt(getConfigPath(), configName, key, value));
    }

    public int getInt(String configName, String key, int defaultValue) {
        return runWithErrorHandle(() -> JsonConfigUtils.getInt(getConfigPath(), configName, key, defaultValue));
    }

    public void putBoolean(String configName, String key, boolean value) {
        runWithErrorHandle(() -> JsonConfigUtils.putBoolean(getConfigPath(), configName, key, value));
    }

    public boolean getBoolean(String configName, String key, boolean defaultValue) {
        return runWithErrorHandle(() -> JsonConfigUtils.getBoolean(getConfigPath(), configName, key, defaultValue));
    }

    public void putLong(String configName, String key, long value) {
        runWithErrorHandle(() -> JsonConfigUtils.putLong(getConfigPath(), configName, key, value));
    }

    public long getLong(String configName, String key, long defaultValue) {
        return runWithErrorHandle(() -> JsonConfigUtils.getLong(getConfigPath(), configName, key, defaultValue));
    }

    public void putDouble(String configName, String key, double value) {
        runWithErrorHandle(() -> JsonConfigUtils.putDouble(getConfigPath(), configName, key, value));
    }

    public double getDouble(String configName, String key, double defaultValue) {
        return runWithErrorHandle(() -> JsonConfigUtils.getDouble(getConfigPath(), configName, key, defaultValue));
    }

    public void remove(String configName, String key) {
        runWithErrorHandle(() -> JsonConfigUtils.remove(getConfigPath(), configName, key));
    }

    public boolean contains(String configName, String key) {
        return runWithErrorHandle(() -> JsonConfigUtils.contains(getConfigPath(), configName, key));
    }

    public void clear(String configName) {
        runWithErrorHandle(() -> JsonConfigUtils.clear(getConfigPath(), configName));
    }

    public Map<String, Object> getConfigMap(String configName) {
        return runWithErrorHandle(() -> JsonConfigUtils.getConfigMap(getConfigPath(), configName));
    }

    public String getConfigPath(String configName) {
        return runWithErrorHandle(() -> JsonConfigUtils.getConfigPath(getConfigPath(), configName));
    }

    public void startQQActivity(String activityClass) {
        runWithErrorHandle(() -> {
            try {
                Activity activity = getNowActivity();
                if (activity != null) {
                    Class<?> cls = Class.forName(activityClass);
                    Intent intent = new Intent(activity, cls);
                    activity.startActivity(intent);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void startQQActivity(String activityClass, Map<String, String> extras) {
        runWithErrorHandle(() -> {
            try {
                Activity activity = getNowActivity();
                if (activity != null) {
                    Class<?> cls = Class.forName(activityClass);
                    Intent intent = new Intent(activity, cls);
                    if (extras != null) {
                        for (Map.Entry<String, String> entry : extras.entrySet()) {
                            intent.putExtra(entry.getKey(), entry.getValue());
                        }
                    }
                    activity.startActivity(intent);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    public String httpGet(String url) {
        return runWithErrorHandle(() -> {
            java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newSingleThreadExecutor();
            java.util.concurrent.Future<String> future = executor.submit(() -> HttpUtils.get(url));
            try {
                return future.get();
            } catch (java.util.concurrent.ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                executor.shutdown();
            }
        });
    }

    public String httpGet(String url, int connectTimeout, int readTimeout) {
        return runWithErrorHandle(() -> {
            java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newSingleThreadExecutor();
            java.util.concurrent.Future<String> future = executor.submit(() -> HttpUtils.get(url, connectTimeout, readTimeout));
            try {
                return future.get();
            } catch (java.util.concurrent.ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                executor.shutdown();
            }
        });
    }

    public String httpPost(String url, String body) {
        return runWithErrorHandle(() -> {
            java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newSingleThreadExecutor();
            java.util.concurrent.Future<String> future = executor.submit(() -> HttpUtils.post(url, body));
            try {
                return future.get();
            } catch (java.util.concurrent.ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                executor.shutdown();
            }
        });
    }

    public String httpPost(String url, String body, String contentType) {
        return runWithErrorHandle(() -> {
            java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newSingleThreadExecutor();
            java.util.concurrent.Future<String> future = executor.submit(() -> HttpUtils.post(url, body, contentType));
            try {
                return future.get();
            } catch (java.util.concurrent.ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                executor.shutdown();
            }
        });
    }

    public boolean downloadFile(String url, String savePath) {
        return runWithErrorHandle(() -> {
            java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newSingleThreadExecutor();
            java.util.concurrent.Future<Boolean> future = executor.submit(() -> HttpUtils.downloadSync(url, savePath));
            try {
                return future.get();
            } catch (java.util.concurrent.ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                executor.shutdown();
            }
        });
    }

    public String formatTime(long timestamp) {
        return runWithErrorHandle(() -> {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            return sdf.format(new java.util.Date(timestamp));
        });
    }

    public long getCurrentTimeMillis() {
        return System.currentTimeMillis();
    }

    public String getDeviceId() {
        return runWithErrorHandle(() -> {
            android.content.Context ctx = me.lengyu.qedge.utils.HostInfo.getContext();
            return android.provider.Settings.Secure.getString(ctx.getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
        });
    }

    public String getVersionName() {
        return runWithErrorHandle(() -> {
            try {
                android.content.Context ctx = me.lengyu.qedge.utils.HostInfo.getContext();
                return ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0).versionName;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    public int getVersionCode() {
        return runWithErrorHandle(() -> {
            try {
                android.content.Context ctx = me.lengyu.qedge.utils.HostInfo.getContext();
                return ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0).versionCode;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void runOnUiThread(Runnable runnable) {
        runWithErrorHandle(() -> {
            Activity activity = getNowActivity();
            if (activity != null) {
                activity.runOnUiThread(runnable);
            }
        });
    }

    public void sleep(long millis) {
        runWithErrorHandle(() -> {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    private void runWithErrorHandle(Runnable runnable) {
        try {
            runnable.run();
        } catch (Throwable e) {
            LogUtils.e("PluginMethod", e);
            throw e;
        }
    }

    private <T> T runWithErrorHandle(java.util.function.Supplier<T> supplier) {
        try {
            return supplier.get();
        } catch (Throwable e) {
            LogUtils.e("PluginMethod", e);
            throw e;
        }
    }

    public boolean uploadAvatar(String path) {
        return runWithErrorHandle(() -> ExtraTool.uploadAvatar(path));
    }

    public boolean uploadCover(String path) {
        return runWithErrorHandle(() -> ExtraTool.uploadCover(path));
    }

    public void changeMyName(String title) {
        runWithErrorHandle(() -> ExtraTool.changeMyName(title));
    }

    public boolean sendPacket(String cmd, String json) {
        return runWithErrorHandle(() -> ExtraTool.sendPacket(cmd, json));
    }

    public String fetchPacket(String cmd, String json) {
        return runWithErrorHandle(() -> ExtraTool.fetchPacket(cmd, json));
    }

    public boolean sendPacket(String cmd, JSONObject json) {
        return runWithErrorHandle(() -> ExtraTool.sendPacket(cmd, json));
    }

    public String fetchPacket(String cmd, JSONObject json) {
        return runWithErrorHandle(() -> ExtraTool.fetchPacket(cmd, json));
    }

    public void setGroupShortcutBar(String qun, String desc, String link, String name, String cover) {
        runWithErrorHandle(() -> ExtraTool.setGroupShortcutBar(name, desc, link, qun, cover));
    }

    public void setGroupLocation(String qun, String location) {
        runWithErrorHandle(() -> ExtraTool.setGroupLocation(qun, location));
    }

    public void sendMusic(String platform, String uin, String title, String desc, String detailUrl, String audio, String img, int mtype) {
        runWithErrorHandle(() -> ExtraTool.sendMusic(platform, uin, title, desc, detailUrl, audio, img, mtype));
    }

    public List<String> getAbnormalUserList(String qun) {
        return runWithErrorHandle(() -> ExtraTool.getAbnormalUserList(qun));
    }

    public String getUidFromUin(String uin) {
        return runWithErrorHandle(() -> FriendTool.getUidFromUin(uin));
    }

    public String getUinFromUid(String uid) {
        return runWithErrorHandle(() -> FriendTool.getUinFromUid(uid));
    }

    public boolean isValidUin(String uin) {
        return runWithErrorHandle(() -> FriendTool.isValidUin(uin));
    }

    public boolean isValidUid(String uid) {
        return runWithErrorHandle(() -> FriendTool.isValidUid(uid));
    }

    public boolean isValidQQ(String qq) {
        return runWithErrorHandle(() -> FriendTool.isValidUin(qq));
    }

    public Object callMethod(Object obj, String methodName, Object... args) {
        return runWithErrorHandle(() -> me.lengyu.qedge.utils.ReflectUtils.callMethod(obj, methodName, args));
    }

    public Object callStaticMethod(String className, String methodName, Object... args) {
        return runWithErrorHandle(() -> {
            try {
                Class<?> cls = Class.forName(className);
                return me.lengyu.qedge.utils.ReflectUtils.callStaticMethod(cls, methodName, args);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public Object getFieldValue(Object obj, String fieldName) {
        return runWithErrorHandle(() -> me.lengyu.qedge.utils.ReflectUtils.getFieldValue(obj, fieldName));
    }

    public void setFieldValue(Object obj, String fieldName, Object value) {
        runWithErrorHandle(() -> me.lengyu.qedge.utils.ReflectUtils.setFieldValue(obj, fieldName, value));
    }

    public Object newInstance(String className, Object... args) {
        return runWithErrorHandle(() -> {
            try {
                Class<?> cls = Class.forName(className);
                return me.lengyu.qedge.utils.ReflectUtils.newInstance(cls, args);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    public Object findClass(String className) {
        return runWithErrorHandle(() -> {
            try {
                return Class.forName(className);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public Object findMethod(Object obj, String methodName, int paramCount) {
        return runWithErrorHandle(() -> {
            Class<?> cls = obj instanceof Class ? (Class<?>) obj : obj.getClass();
            return me.lengyu.qedge.utils.ReflectUtils.findMethod(cls, methodName, paramCount);
        });
    }

    public Object findMethod(Object obj, String methodName, Class<?>... parameterTypes) {
        return runWithErrorHandle(() -> {
            Class<?> cls = obj instanceof Class ? (Class<?>) obj : obj.getClass();
            return me.lengyu.qedge.utils.ReflectUtils.findMethod(cls, methodName, parameterTypes);
        });
    }

    public Object findField(Object obj, String fieldName) {
        return runWithErrorHandle(() -> {
            Class<?> cls = obj instanceof Class ? (Class<?>) obj : obj.getClass();
            return me.lengyu.qedge.utils.ReflectUtils.findField(cls, fieldName);
        });
    }

    public void sendPrivateMsg(String uin, String msg) {
        runWithErrorHandle(() -> MsgTool.sendMsg(uin, msg, 1));
    }

    public void sendGroupMsg(String qun, String msg) {
        runWithErrorHandle(() -> MsgTool.sendMsg(qun, msg, 2));
    }

    public void sendPrivatePic(String uin, String path) {
        runWithErrorHandle(() -> MsgTool.sendPic(uin, path, 1));
    }

    public void sendGroupPic(String qun, String path) {
        runWithErrorHandle(() -> MsgTool.sendPic(qun, path, 2));
    }

    public void sendPrivatePtt(String uin, String path) {
        runWithErrorHandle(() -> MsgTool.sendPtt(uin, path, 1));
    }

    public void sendGroupPtt(String qun, String path) {
        runWithErrorHandle(() -> MsgTool.sendPtt(qun, path, 2));
    }

    public void sendPrivateCard(String uin, String data) {
        runWithErrorHandle(() -> MsgTool.sendCard(uin, data, 1));
    }

    public void sendGroupCard(String qun, String data) {
        runWithErrorHandle(() -> MsgTool.sendCard(qun, data, 2));
    }

    public void sendPrivateVideo(String uin, String path) {
        runWithErrorHandle(() -> MsgTool.sendVideo(uin, path, 1));
    }

    public void sendGroupVideo(String qun, String path) {
        runWithErrorHandle(() -> MsgTool.sendVideo(qun, path, 2));
    }

    public void sendPrivateFile(String uin, String path) {
        runWithErrorHandle(() -> MsgTool.sendFile(uin, path, 1));
    }

    public void sendGroupFile(String qun, String path) {
        runWithErrorHandle(() -> MsgTool.sendFile(qun, path, 2));
    }

    public void recallPrivateMsg(String uin, long msgId) {
        runWithErrorHandle(() -> MsgTool.recallMsg(1, uin, msgId));
    }

    public void recallGroupMsg(String qun, long msgId) {
        runWithErrorHandle(() -> MsgTool.recallMsg(2, qun, msgId));
    }

    public void addLocalGrayTipMsg(String peerUin, String jsonStr, long busiId, int chatType) {
        runWithErrorHandle(() -> MsgTool.addLocalGrayTipMsg(MsgTool.makeContact(peerUin, chatType), jsonStr, busiId));
    }

    public void sendReplyMsg(String peerUin, long replyMsgId, String msg, int chatType) {
        runWithErrorHandle(() -> MsgTool.sendReplyMsg(peerUin, replyMsgId, msg, chatType));
    }

    public void sendPrivateReplyMsg(String uin, long replyMsgId, String msg) {
        runWithErrorHandle(() -> MsgTool.sendReplyMsg(uin, replyMsgId, msg, 1));
    }

    public void sendGroupReplyMsg(String qun, long replyMsgId, String msg) {
        runWithErrorHandle(() -> MsgTool.sendReplyMsg(qun, replyMsgId, msg, 2));
    }

    public boolean uploadTroopAvatar(String qun, String filepath) {
        return runWithErrorHandle(() -> ExtraTool.uploadTroopAvatar(qun, filepath));
    }

    public boolean uploadTroopCover(String qun, String filepath) {
        return runWithErrorHandle(() -> ExtraTool.uploadTroopCover(qun, filepath));
    }

    public void qqsendTroopMusic(String qun, String title, String desc, String detailUrl, String audio, String img) {
        runWithErrorHandle(() -> ExtraTool.qqsendTroopMusic(qun, title, desc, detailUrl, audio, img));
    }

    public void qqsendFriendMusic(String uin, String title, String desc, String detailUrl, String audio, String img) {
        runWithErrorHandle(() -> ExtraTool.qqsendFriendMusic(uin, title, desc, detailUrl, audio, img));
    }

    public void wysendTroopMusic(String qun, String title, String desc, String detailUrl, String audio, String img) {
        runWithErrorHandle(() -> ExtraTool.wysendTroopMusic(qun, title, desc, detailUrl, audio, img));
    }

    public void wysendFriendMusic(String uin, String title, String desc, String detailUrl, String audio, String img) {
        runWithErrorHandle(() -> ExtraTool.wysendFriendMusic(uin, title, desc, detailUrl, audio, img));
    }

    public void kgsendTroopMusic(String qun, String title, String desc, String detailUrl, String audio, String img) {
        runWithErrorHandle(() -> ExtraTool.kgsendTroopMusic(qun, title, desc, detailUrl, audio, img));
    }

    public void kgsendFriendMusic(String uin, String title, String desc, String detailUrl, String audio, String img) {
        runWithErrorHandle(() -> ExtraTool.kgsendFriendMusic(uin, title, desc, detailUrl, audio, img));
    }

    public void kwsendTroopMusic(String qun, String title, String desc, String detailUrl, String audio, String img) {
        runWithErrorHandle(() -> ExtraTool.kwsendTroopMusic(qun, title, desc, detailUrl, audio, img));
    }

    public void kwsendFriendMusic(String uin, String title, String desc, String detailUrl, String audio, String img) {
        runWithErrorHandle(() -> ExtraTool.kwsendFriendMusic(uin, title, desc, detailUrl, audio, img));
    }

    public void bdsendTroopMusic(String qun, String title, String desc, String detailUrl, String audio, String img) {
        runWithErrorHandle(() -> ExtraTool.bdsendTroopMusic(qun, title, desc, detailUrl, audio, img));
    }

    public void bdsendFriendMusic(String uin, String title, String desc, String detailUrl, String audio, String img) {
        runWithErrorHandle(() -> ExtraTool.bdsendFriendMusic(uin, title, desc, detailUrl, audio, img));
    }

    public void mgsendTroopMusic(String qun, String title, String desc, String detailUrl, String audio, String img) {
        runWithErrorHandle(() -> ExtraTool.mgsendTroopMusic(qun, title, desc, detailUrl, audio, img));
    }

    public void mgsendFriendMusic(String uin, String title, String desc, String detailUrl, String audio, String img) {
        runWithErrorHandle(() -> ExtraTool.mgsendFriendMusic(uin, title, desc, detailUrl, audio, img));
    }

    public Object hookAfter(Object member, Object callback) {
        return runWithErrorHandle(() -> {
            if (member instanceof java.lang.reflect.Member) {
                return me.lengyu.qedge.utils.hook.HookExtensionsKt.hookAfter(
                        (java.lang.reflect.Member) member,
                        null,
                        (kotlin.jvm.functions.Function1<de.robv.android.xposed.XC_MethodHook.MethodHookParam, kotlin.Unit>) callback
                );
            }
            return null;
        });
    }

    public Object hookBefore(Object member, Object callback) {
        return runWithErrorHandle(() -> {
            if (member instanceof java.lang.reflect.Member) {
                return me.lengyu.qedge.utils.hook.HookExtensionsKt.hookBefore(
                        (java.lang.reflect.Member) member,
                        null,
                        (kotlin.jvm.functions.Function1<de.robv.android.xposed.XC_MethodHook.MethodHookParam, kotlin.Unit>) callback
                );
            }
            return null;
        });
    }

    public boolean deleteFile(String path) {
        return runWithErrorHandle(() -> {
            File file = new File(path);
            return file.delete();
        });
    }

    public boolean existsFile(String path) {
        return runWithErrorHandle(() -> new File(path).exists());
    }

    public boolean mkdirs(String path) {
        return runWithErrorHandle(() -> new File(path).mkdirs());
    }

    public String getFileName(String path) {
        return runWithErrorHandle(() -> new File(path).getName());
    }

    public long getFileSize(String path) {
        return runWithErrorHandle(() -> new File(path).length());
    }

    public long getFileLastModified(String path) {
        return runWithErrorHandle(() -> new File(path).lastModified());
    }

    public String getParentPath(String path) {
        return runWithErrorHandle(() -> {
            File file = new File(path);
            File parent = file.getParentFile();
            return parent != null ? parent.getAbsolutePath() : null;
        });
    }

    public List<String> listFiles(String dirPath) {
        return runWithErrorHandle(() -> {
            File dir = new File(dirPath);
            String[] files = dir.list();
            return files != null ? java.util.Arrays.asList(files) : new ArrayList<>();
        });
    }

    public String readFile(String path) {
        return runWithErrorHandle(() -> {
            File file = new File(path);
            try {
                java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(file));
                try {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line).append("\n");
                    }
                    return sb.toString();
                } finally {
                    reader.close();
                }
            } catch (java.io.IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void writeFile(String path, String content) {
        runWithErrorHandle(() -> {
            File file = new File(path);
            try {
                java.io.BufferedWriter writer = new java.io.BufferedWriter(new java.io.FileWriter(file));
                try {
                    writer.write(content);
                } finally {
                    writer.close();
                }
            } catch (java.io.IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void appendFile(String path, String content) {
        runWithErrorHandle(() -> {
            File file = new File(path);
            try {
                java.io.BufferedWriter writer = new java.io.BufferedWriter(new java.io.FileWriter(file, true));
                try {
                    writer.write(content);
                } finally {
                    writer.close();
                }
            } catch (java.io.IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public String urlEncode(String str) {
        return runWithErrorHandle(() -> {
            try {
                return java.net.URLEncoder.encode(str, "UTF-8");
            } catch (java.io.UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public String urlDecode(String str) {
        return runWithErrorHandle(() -> {
            try {
                return java.net.URLDecoder.decode(str, "UTF-8");
            } catch (java.io.UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public String base64Encode(String str) {
        return runWithErrorHandle(() -> android.util.Base64.encodeToString(str.getBytes(), android.util.Base64.DEFAULT));
    }

    public String base64Decode(String str) {
        return runWithErrorHandle(() -> new String(android.util.Base64.decode(str, android.util.Base64.DEFAULT)));
    }

    public JSONObject pbDecode(byte[] data) {
        try {
            ProtoData protoData = new ProtoData();
            protoData.fromBytes(data);
            return protoData.toJSON();
        } catch (Exception e) {
            LogUtils.e("PluginMethod", e);
            throw new RuntimeException(e);
        }
    }

    public byte[] pbEncode(JSONObject json) {
        try {
            ProtoData protoData = new ProtoData();
            protoData.fromJSON(json);
            return protoData.toBytes();
        } catch (Exception e) {
            LogUtils.e("PluginMethod", e);
            throw new RuntimeException(e);
        }
    }

    public String pbToHex(byte[] data) {
        return runWithErrorHandle(() -> {
            StringBuilder sb = new StringBuilder();
            for (byte b : data) {
                sb.append(String.format("%02X", b & 0xFF));
            }
            return sb.toString();
        });
    }

    public byte[] pbFromHex(String hex) {
        return runWithErrorHandle(() -> {
            int len = hex.length();
            byte[] data = new byte[len / 2];
            for (int i = 0; i < len; i += 2) {
                data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                        + Character.digit(hex.charAt(i + 1), 16));
            }
            return data;
        });
    }

    public String generateRandomString(int length) {
        return runWithErrorHandle(() -> {
            String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
            java.util.Random random = new java.util.Random();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < length; i++) {
                sb.append(chars.charAt(random.nextInt(chars.length())));
            }
            return sb.toString();
        });
    }

    public int generateRandomInt(int min, int max) {
        return runWithErrorHandle(() -> {
            java.util.Random random = new java.util.Random();
            return random.nextInt(max - min + 1) + min;
        });
    }

    public String toJson(Object obj) {
        return runWithErrorHandle(() -> {
            if (obj instanceof JSONObject) {
                return obj.toString();
            }
            if (obj instanceof Map) {
                return new JSONObject((Map) obj).toString();
            }
            return obj != null ? obj.toString() : "null";
        });
    }

    public JSONObject parseJson(String jsonStr) {
        return runWithErrorHandle(() -> {
            try {
                return new JSONObject(jsonStr);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public JSONArray parseJsonArray(String jsonStr) {
        return runWithErrorHandle(() -> {
            try {
                return new JSONArray(jsonStr);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public boolean isMainThread() {
        return runWithErrorHandle(() -> android.os.Looper.getMainLooper() == android.os.Looper.myLooper());
    }

    public void runOnBackgroundThread(Runnable runnable) {
        runWithErrorHandle(() -> new Thread(runnable).start());
    }

    public String getPluginName() {
        return compiler.info.getName();
    }

    public boolean isPluginEnabled() {
        return runWithErrorHandle(() -> compiler.info.isRunning());
    }

    public void setPluginEnabled(boolean enabled) {
        runWithErrorHandle(() -> compiler.info.setRunning(enabled));
    }

    public long getCurrentTimeSeconds() {
        return System.currentTimeMillis() / 1000;
    }

    public String formatTime(long timestamp, String format) {
        return runWithErrorHandle(() -> {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(format);
            return sdf.format(new java.util.Date(timestamp));
        });
    }

    public String readprop(String file, String name) {
        return runWithErrorHandle(() -> {
            String text = readFile(file);
            java.util.Properties props = new java.util.Properties();
            try {
                props.load(new java.io.StringReader(text));
            } catch (java.io.IOException e) {
                throw new RuntimeException(e);
            }
            return props.getProperty(name);
        });
    }

    public void addFriend(String targetUin, String msg, String remark) {
        runWithErrorHandle(() -> ExtraTool.addFriend(targetUin, msg, remark));
    }

    public void uploadAvatar(String url, boolean isStatic) {
        runWithErrorHandle(() -> ExtraTool.uploadAvatar(url, isStatic));
    }

    public List<BlackUser> getGroupBlackList(String qun) {
        return runWithErrorHandle(() -> ExtraTool.getGroupBlackList(qun));
    }

    public List<JointGroup> getJointGroupList(String uin) {
        return runWithErrorHandle(() -> ExtraTool.getJointGroupList(uin));
    }

    public void sendBubbleVideo(String targetUin, String videoPath, int chatType) {
        if(HostInfo.INSTANCE.isQQ()){
        runWithErrorHandle(() -> MsgTool.sendBubbleVideo(targetUin, videoPath, chatType));
        } else {
            Toasts.toast("不支持TIM，无法发送泡泡消息");
        }
    }

    public String uploadImage(String imagePath) {
        File file = new File(imagePath);
        if(!file.exists()){
            Toasts.toast("上传失败，图片不存在：" + imagePath);
            return "";
        }
        return runWithErrorHandle(() -> ExtraTool.uploadImage(imagePath));
    }

    public void sendMiniApp(String packageName, String appid, String token,
                                   String arkAppId, String miniAppPath,
                                   String uin, String appName, String desc,
                                   String detailUrl, String img, int mtype,
                                   JSONObject arkJson) {
        runWithErrorHandle(() -> ExtraTool.sendMiniApp(packageName, appid, token,
                arkAppId, miniAppPath,
                uin, appName, desc,
                detailUrl, img, mtype,
                arkJson));
    }

    public void extractAudio(String videoPath, String audioPath) {
        runWithErrorHandle(() -> ExtraTool.extractAudio(HostInfo.getHostContext(), videoPath, audioPath));
    }

    public String getPinyin(String text, int type) {
        return runWithErrorHandle(() -> ExtraTool.getPinyin(text, type));
    }

    public String getImageType(String filePath) {
        return runWithErrorHandle(() -> ExtraTool.getImageType(filePath));
    }
}