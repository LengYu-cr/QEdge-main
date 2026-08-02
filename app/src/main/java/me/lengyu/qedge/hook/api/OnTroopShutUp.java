package me.lengyu.qedge.hook.api;

import me.lengyu.qedge.hook.annotation.HookItemAnnotation;
import me.lengyu.qedge.hook.base.BaseApiHookItem;
import me.lengyu.qedge.hook.base.Listener;
import me.lengyu.qedge.utils.LogUtils;
import me.lengyu.qedge.utils.qq.FriendTool;

import org.json.JSONObject;

@HookItemAnnotation(value = "监听群禁言", category = "api")
public class OnTroopShutUp extends BaseApiHookItem<OnTroopShutUp.TroopShutUpListener> {

    public static final OnTroopShutUp INSTANCE = new OnTroopShutUp();

    public OnTroopShutUp() {}

    @Override
    public void loadHook() {
        FromServiceMsgDispatcher.registerListener((serviceCmd, json, msg) -> {
            try {
                JSONObject msgHead = walkJson(json, "1", "2");
                if (msgHead == null) return;
                if (msgHead.optInt("1") != 732 || msgHead.optInt("2") != 12) {
                    return;
                }

                JSONObject groupInfo = walkJson(json, "1", "3", "2");
                if (groupInfo == null) return;

                String troopUin = getNumberString(groupInfo, "1");
                if (troopUin == null || troopUin.isEmpty()) return;

                String opUid = getUidString(groupInfo, "4");
                String opUin = FriendTool.getUinFromUid(opUid);

                JSONObject shutUpInfo = walkJson(groupInfo, "5", "3");
                if (shutUpInfo == null) return;

                String memberUid = getUidString(shutUpInfo, "1");
                String memberUin = FriendTool.getUinFromUid(memberUid);
                long time = shutUpInfo.optLong("2", 0L);

                notifyListeners(troopUin, memberUin, time, opUin);
            } catch (Throwable e) {
                LogUtils.e("OnTroopShutUp", "dispatch error: " + e.getMessage());
            }
        });
    }

    private JSONObject walkJson(JSONObject json, String... keys) {
        try {
            JSONObject current = json;
            for (int i = 0; i < keys.length - 1; i++) {
                current = current.getJSONObject(keys[i]);
            }
            return current.getJSONObject(keys[keys.length - 1]);
        } catch (Exception e) {
            return null;
        }
    }

    private String getNumberString(JSONObject obj, String key) {
        try {
            Object value = obj.get(key);
            if (value instanceof Long) {
                return String.valueOf((Long) value);
            } else if (value instanceof Integer) {
                return String.valueOf((Integer) value);
            } else if (value instanceof String) {
                return (String) value;
            }
        } catch (Exception e) {
        }
        return null;
    }

    private String getUidString(JSONObject obj, String key) {
        try {
            Object value = obj.get(key);
            if (value instanceof String) {
                String str = (String) value;
                if (str.startsWith("u_")) {
                    return str;
                }
                return str;
            } else if (value instanceof Number) {
                long num = ((Number) value).longValue();
                if (num > 0) {
                    return "u_" + num;
                }
            }
        } catch (Exception e) {
        }
        return "";
    }

    private void notifyListeners(String troopUin, String memberUin, long time, String opUin) {
        forEachChecked(listener -> listener.onShutUp(troopUin, memberUin, time, opUin));
    }

    public interface TroopShutUpListener extends Listener {
        void onShutUp(String troopUin, String memberUin, long time, String opUin);
    }

    public static void registerListener(TroopShutUpListener listener) {
        INSTANCE.addListener(listener);
    }

    public static void unregisterListener(TroopShutUpListener listener) {
        INSTANCE.removeListener(listener);
    }
}
