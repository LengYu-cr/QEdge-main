package me.lengyu.qedge.hook.api;

import me.lengyu.qedge.hook.annotation.HookItemAnnotation;
import me.lengyu.qedge.hook.base.BaseApiHookItem;
import me.lengyu.qedge.hook.base.Listener;
import me.lengyu.qedge.utils.LogUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.regex.Pattern;

@HookItemAnnotation(value = "监听拍一拍", category = "api")
public class OnPaiYiPai extends BaseApiHookItem<OnPaiYiPai.PaiYiPaiListener> {

    public static final OnPaiYiPai INSTANCE = new OnPaiYiPai();
    private static final Pattern QQ_REGEX = Pattern.compile("[1-9]\\d{4,12}");

    public OnPaiYiPai() {}

    @Override
    public void loadHook() {
        FromServiceMsgDispatcher.registerListener((serviceCmd, json, msg) -> {
            try {
                JSONObject msgHead = walkJson(json, "1", "2");
                if (msgHead == null) return;

                String peerUin = getNumberString(json, "1", "1", "1");

                int cmd1 = msgHead.optInt("1", 0);
                int cmd2 = msgHead.optInt("2", 0);

                if (walkJson(json, "1", "3") == null) return;

                int chatType;
                String fromUin;
                String toUin;

                if (cmd1 == 732 && cmd2 == 20) {
                    chatType = 2;
                    String content = getString(json, "1", "3", "2");
                    if (content == null) return;
                    fromUin = extractQQ(content, "1");
                    toUin = extractQQ(content, "2");
                } else if (cmd1 == 528 && cmd2 == 290) {
                    chatType = 1;
                    fromUin = peerUin;
                    JSONArray dataArray = getArray(json, "1", "3", "2", "7");
                    toUin = extractToUinFromArray(dataArray);
                    if (toUin == null) return;
                } else {
                    return;
                }

                if (!isValidQQ(fromUin)) return;

                notifyListeners(peerUin, chatType, fromUin);
            } catch (Throwable e) {
                LogUtils.e("OnPaiYiPai", "dispatch error: " + e.getMessage());
            }
        });
    }

    private JSONObject walkJson(JSONObject json, String... keys) {
        try {
            JSONObject current = json;
            for (String key : keys) {
                current = current.getJSONObject(key);
            }
            return current;
        } catch (Exception e) {
            return null;
        }
    }

    private String getNumberString(JSONObject json, String... keys) {
        try {
            JSONObject current = json;
            for (int i = 0; i < keys.length - 1; i++) {
                current = current.getJSONObject(keys[i]);
            }
            Object value = current.get(keys[keys.length - 1]);
            if (value instanceof Long) return String.valueOf((Long) value);
            if (value instanceof Integer) return String.valueOf((Integer) value);
            if (value instanceof String) return (String) value;
        } catch (Exception e) {
        }
        return null;
    }

    private String getString(JSONObject json, String... keys) {
        try {
            JSONObject current = json;
            for (int i = 0; i < keys.length - 1; i++) {
                current = current.getJSONObject(keys[i]);
            }
            return current.getString(keys[keys.length - 1]);
        } catch (Exception e) {
            return null;
        }
    }

    private JSONArray getArray(JSONObject json, String... keys) {
        try {
            JSONObject current = json;
            for (int i = 0; i < keys.length - 1; i++) {
                current = current.getJSONObject(keys[i]);
            }
            return current.getJSONArray(keys[keys.length - 1]);
        } catch (Exception e) {
            return null;
        }
    }

    private String extractQQ(String target, String type) {
        String key = "uin_str" + type;
        int startIndex = target.indexOf(key);
        if (startIndex == -1) return "";
        startIndex += key.length();

        String remaining = target.substring(startIndex);
        int digitStart = -1;
        for (int i = 0; i < remaining.length(); i++) {
            if (Character.isDigit(remaining.charAt(i))) {
                digitStart = i;
                break;
            }
        }
        if (digitStart == -1) return "";

        int realStart = startIndex + digitStart;
        int realEnd = target.indexOf(':', realStart);
        if (realEnd == -1) realEnd = target.length();
        return target.substring(realStart, realEnd);
    }

    private String extractToUinFromArray(JSONArray array) {
        if (array == null) return null;
        try {
            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.optJSONObject(i);
                if (item == null) continue;
                if ("uin_str2".equals(item.optString("1", null))) {
                    return item.optString("2", null);
                }
            }
        } catch (Exception e) {
        }
        return null;
    }

    private boolean isValidQQ(String input) {
        return input != null && QQ_REGEX.matcher(input).matches();
    }

    private void notifyListeners(String peerUin, int chatType, String fromUin) {
        forEachChecked(listener -> listener.onPai(peerUin, chatType, fromUin));
    }

    public interface PaiYiPaiListener extends Listener {
        void onPai(String peerUin, int chatType, String fromUin);
    }

    public static void registerListener(PaiYiPaiListener listener) {
        INSTANCE.addListener(listener);
    }

    public static void unregisterListener(PaiYiPaiListener listener) {
        INSTANCE.removeListener(listener);
    }
}
