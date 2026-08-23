package me.lengyu.qedge.hook.api;

import com.tencent.qphone.base.remote.FromServiceMsg;

import me.lengyu.qedge.utils.HookUtils;
import me.lengyu.qedge.utils.LogUtils;
import me.lengyu.qedge.utils.ReflectUtils;
import me.lengyu.qedge.hook.annotation.HookItemAnnotation;
import me.lengyu.qedge.utils.json.ProtoData;

import org.json.JSONObject;

import java.lang.reflect.Method;
import java.util.concurrent.CopyOnWriteArrayList;
/**
 * @Author 冷雨
 * @Description 监听接收protoBuffer包体
 */
@HookItemAnnotation(value = "监听接收包体", category = "api")
public class FromServiceMsgDispatcher {

    private static final String TAG = "FromServiceMsgDispatcher";
    private static final String SERVICE_CMD = "trpc.msg.olpush.OlPushService.MsgPush";

    private static boolean hooked = false;
    private static final CopyOnWriteArrayList<DispatcherListener> listeners = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<FeedsListener> feedsListeners = new CopyOnWriteArrayList<>();

    public interface DispatcherListener {
        void onDispatch(String serviceCmd, JSONObject json, FromServiceMsg msg);
    }

    public interface FeedsListener {
        void onFeedsResponse(JSONObject json, FromServiceMsg msg);
    }

    public static void registerListener(DispatcherListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public static void unregisterListener(DispatcherListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    public static void registerFeedsListener(FeedsListener listener) {
        if (listener != null) {
            feedsListeners.add(listener);
        }
    }

    public static void unregisterFeedsListener(FeedsListener listener) {
        if (listener != null) {
            feedsListeners.remove(listener);
        }
    }

    public static void loadHook() {
        if (hooked) return;
        hooked = true;

        try {
            ClassLoader classLoader = ReflectUtils.hostClassLoader;
            if (classLoader == null) {
                classLoader = FromServiceMsgDispatcher.class.getClassLoader();
            }
            Class<?> msfServletClass = Class.forName("mqq.app.MSFServlet", false, classLoader);
            Method onReceiveMethod = msfServletClass.getDeclaredMethod("onReceive", FromServiceMsg.class);
            HookUtils.hookAfter(onReceiveMethod, param -> {
                try {
                    FromServiceMsg fromServiceMsg = (FromServiceMsg) param.args[0];
                    if (fromServiceMsg == null) return;

                    String cmd = fromServiceMsg.getServiceCmd();
                    if (cmd == null) return;

                    byte[] wupBuffer = fromServiceMsg.getWupBuffer();
                    if (wupBuffer == null || wupBuffer.length == 0) return;

                    JSONObject json = null;

                    // OlPush 消息（原有的 DispatcherListener）
                    if (SERVICE_CMD.equals(cmd) && !listeners.isEmpty()) {
                        if (json == null) {
                            ProtoData data = new ProtoData();
                            data.fromBytes(wupBuffer);
                            json = data.toJSON();
                        }
                        for (DispatcherListener listener : listeners) {
                            try {
                                listener.onDispatch(cmd, json, fromServiceMsg);
                            } catch (Throwable e) {
                                LogUtils.e(TAG, "dispatch error: " + e.getMessage());
                            }
                        }
                    }

                    // 好友说说列表响应（FeedsListener）
                    if ((cmd.contains("GetFriendFeeds") || cmd.contains("feeds_reader.FeedsReader"))
                        && !feedsListeners.isEmpty()) {
                        if (json == null) {
                            ProtoData data = new ProtoData();
                            data.fromBytes(wupBuffer);
                            json = data.toJSON();
                        }
                        for (FeedsListener listener : feedsListeners) {
                            try {
                                listener.onFeedsResponse(json, fromServiceMsg);
                            } catch (Throwable e) {
                                LogUtils.e(TAG, "feeds dispatch error: " + e.getMessage());
                            }
                        }
                    }
                } catch (Throwable e) {
                    LogUtils.e(TAG, "hook callback error: " + e.getMessage());
                }
            });
        } catch (Throwable e) {
            LogUtils.e(TAG, "loadHook error: " + e.getMessage());
        }
    }
}
