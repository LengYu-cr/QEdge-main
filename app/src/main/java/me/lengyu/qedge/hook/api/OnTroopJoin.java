package me.lengyu.qedge.hook.api;

import me.lengyu.qedge.hook.annotation.HookItemAnnotation;
import me.lengyu.qedge.hook.base.BaseApiHookItem;
import me.lengyu.qedge.hook.base.Listener;
import me.lengyu.qedge.utils.HookUtils;
import me.lengyu.qedge.utils.LogUtils;
import me.lengyu.qedge.utils.ReflectUtils;
import me.lengyu.qedge.utils.HostInfo;
import me.lengyu.qedge.utils.json.ProtoData;
import me.lengyu.qedge.utils.qq.FriendTool;

import org.json.JSONObject;

import java.util.ArrayList;

@HookItemAnnotation(value = "监听群成员加入", category = "api")
public class OnTroopJoin extends BaseApiHookItem<OnTroopJoin.TroopJoinListener> {

    public static final OnTroopJoin INSTANCE = new OnTroopJoin();

    public OnTroopJoin() {}

    @Override
    public void loadHook() {
        try {
            if ("com.tencent.mobileqq".equals(HostInfo.packageName)) {
                Class<?> processorClass = Class.forName("com.tencent.qqnt.push.processor.TroopMemberAddPushProcessor");
                java.lang.reflect.Method[] methods = processorClass.getDeclaredMethods();
                java.lang.reflect.Method targetMethod = null;
                for (java.lang.reflect.Method method : methods) {
                    if (method.getParameterTypes().length == 1 && method.getParameterTypes()[0].equals(ArrayList.class)) {
                        targetMethod = method;
                        break;
                    }
                }
                if (targetMethod != null) {
                    HookUtils.hookAfter(targetMethod, param -> {
                        try {
                            Object args0 = param.args[0];
                            if (args0 instanceof ArrayList) {
                                ArrayList<?> byteList = (ArrayList<?>) args0;
                                byte[] bytes = new byte[byteList.size()];
                                for (int i = 0; i < byteList.size(); i++) {
                                    bytes[i] = (Byte) byteList.get(i);
                                }
                                ProtoData protoData = new ProtoData();
                                protoData.fromBytes(bytes);
                                JSONObject json = protoData.toJSON();
                                // LogUtils.d("OnTroopJoin", "json: " + json.toString());
                                String troopUin = walkJson(json, "3", "2", "1");
                                String memberUid = walkJson(json, "3", "2", "3");
                                
                                if (memberUid != null && !memberUid.isEmpty()) {
                                    String memberUin = FriendTool.getUinFromUid(memberUid);
                                    notifyListeners(troopUin, memberUin);
                                }
                            }
                        } catch (Throwable e) {
                            LogUtils.e("OnTroopJoin", "callback error: " + e.getMessage());
                            LogUtils.e(e);
                        }
                    });
                } else {
                    LogUtils.e("OnTroopJoin", "method not found");
                }
            } else if ("com.tencent.tim".equals(HostInfo.packageName)) {
                Class<?> handlerClass = Class.forName("com.tencent.mobileqq.troop.onlinepush.api.impl.TroopOnlinePushHandler");
                java.lang.reflect.Method handleJoinMethod = ReflectUtils.findMethod(handlerClass, "handleJoin", String.class, String.class, String.class);
                if (handleJoinMethod != null) {
                    HookUtils.hookAfter(handleJoinMethod, param -> {
                        try {
                            String troopUin = (String) param.args[0];
                            String memberUin = (String) param.args[1];
                            notifyListeners(troopUin, memberUin);
                        } catch (Throwable e) {
                            LogUtils.e("OnTroopJoin", "handleJoin callback error: " + e.getMessage());
                            LogUtils.e(e);
                        }
                    });
                } else {
                    LogUtils.e("OnTroopJoin", "handleJoin method not found");
                }
            }
        } catch (Throwable e) {
            LogUtils.e("OnTroopJoin", "loadHook error: " + e.getMessage());
            LogUtils.e(e);
        }
    }

    private String walkJson(JSONObject json, String... keys) {
        try {
            JSONObject current = json;
            // LogUtils.d("OnTroopJoin", "walkJson: " + json.toString());
            for (int i = 0; i < keys.length - 1; i++) {
                current = current.getJSONObject(keys[i]);
            }
            return current.getString(keys[keys.length - 1]);
        } catch (Exception e) {
            return null;
        }
    }

    private void notifyListeners(String troopUin, String memberUin) {
        forEachChecked(listener -> listener.onJoin(troopUin, memberUin));
    }

    public interface TroopJoinListener extends Listener {
        void onJoin(String troopUin, String memberUin);
    }

    public static void registerListener(TroopJoinListener listener) {
        INSTANCE.addListener(listener);
    }

    public static void unregisterListener(TroopJoinListener listener) {
        INSTANCE.removeListener(listener);
    }
}
