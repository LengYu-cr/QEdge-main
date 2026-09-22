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
    private static final CopyOnWriteArrayList<TroopJoinListener> joinListeners = new CopyOnWriteArrayList<>();

    public interface DispatcherListener {
        void onDispatch(String serviceCmd, JSONObject json, FromServiceMsg msg);
    }

    public interface FeedsListener {
        void onFeedsResponse(JSONObject json, FromServiceMsg msg);
    }

    /** 群成员加入监听(基于 MSFServlet 拦截，只上报进群) */
    public interface TroopJoinListener {
        void onTroopJoin(String troopUin, String memberUid, String adminUid, int joinType);
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

    public static void registerJoinListener(TroopJoinListener listener) {
        if (listener != null) {
            joinListeners.add(listener);
        }
    }

    public static void unregisterJoinListener(TroopJoinListener listener) {
        if (listener != null) {
            joinListeners.remove(listener);
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

                    // OlPush 消息（原有的 DispatcherListener + 群成员加入 TroopJoinListener）
                    if (SERVICE_CMD.equals(cmd) && (!listeners.isEmpty() || !joinListeners.isEmpty())) {
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

                        // 群成员加入：ontype=33 且 type=131(进群)才上报，退群(130)忽略
                        if (!joinListeners.isEmpty()) {
                            try {
                                JSONObject top1 = json.optJSONObject("1");
                                if (top1 != null) {
                                    JSONObject msgType = top1.optJSONObject("2");
                                    int ontype = msgType != null ? msgType.optInt("1", 0) : 0;
                                    if (ontype == 33) {
                                        JSONObject json3 = top1.optJSONObject("3");
                                        if (json3 != null) {
                                            JSONObject joinInfo = json3.optJSONObject("2");
                                            if (joinInfo != null && joinInfo.has("3") && joinInfo.opt("5") instanceof String) {
                                                int type = joinInfo.optInt("4", 0);
                                                // 130=主动加入群, 131=邀请进群，两者都是入群事件
                                                if (type == 130 || type == 131) {
                                                    String troopUin = String.valueOf(joinInfo.optLong("1"));
                                                    String memberUid = joinInfo.optString("3");
                                                    String adminUid = joinInfo.optString("5");
                                                    for (TroopJoinListener l : joinListeners) {
                                                        try {
                                                            l.onTroopJoin(troopUin, memberUid, adminUid, type);
                                                        } catch (Throwable e) {
                                                            LogUtils.e(TAG, "join dispatch error: " + e.getMessage());
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            } catch (Throwable e) {
                                LogUtils.e(TAG, "join parse error: " + e.getMessage());
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
