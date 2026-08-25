package me.lengyu.qedge.hook.api;

import me.lengyu.qedge.hook.annotation.HookItemAnnotation;
import me.lengyu.qedge.hook.base.BaseApiHookItem;
import me.lengyu.qedge.hook.base.Listener;
import me.lengyu.qedge.utils.HookUtils;
import me.lengyu.qedge.utils.LogUtils;
import me.lengyu.qedge.utils.ReflectUtils;
import me.lengyu.qedge.utils.json.ProtoData;
import com.tencent.qphone.base.remote.FromServiceMsg;
import org.json.JSONArray;
import org.json.JSONObject;
/**
 * @Author 冷雨
 * @Description 监听RKey
 */
@HookItemAnnotation(value = "监听RKey", category = "api")
public class OnGetRKey extends BaseApiHookItem<Listener> {

    public static final OnGetRKey INSTANCE = new OnGetRKey();
    private static final String SERVICE_CMD = "OidbSvcTrpcTcp.0x9067_202";

    public static String friendRkey = "";
    public static String groupRkey = "";

    public OnGetRKey() {}

    @Override
    public void loadHook() {
        try {
            java.lang.reflect.Method getWupBufferMethod = FromServiceMsg.class.getDeclaredMethod(
                "getWupBuffer"
            );

            HookUtils.hookAfter(getWupBufferMethod, param -> {
                try {
                    
                    String cmd = null;
                    FromServiceMsg fromServiceMsg = (FromServiceMsg) param.thisObject;
                    if (fromServiceMsg != null) {
                        try {
                            cmd = (String) fromServiceMsg.getServiceCmd();
                        } catch (Throwable e) {
                            LogUtils.e("OnGetRKey", "getServiceCmd error: " + e.getMessage());
                            LogUtils.e(e);
                            return;
                        }
                    }

                    if (!SERVICE_CMD.equals(cmd)) return;

                    byte[] wupBuffer = (byte[]) param.getResult();
                    if (wupBuffer == null) {
                        LogUtils.e("OnGetRKey", "wupBuffer is null");
                        return;
                    }
                    ProtoData protoData = new ProtoData();
                    protoData.fromBytes(wupBuffer);
                    JSONObject json = protoData.toJSON();
                    if (json != null) {
                        parseRKey(json);
                    }
                } catch (Throwable e) {
                    LogUtils.e("OnGetRKey", "hook error: " + e.getMessage());
                    LogUtils.e(e);
                }
            });
        } catch (Throwable e) {
            LogUtils.e("OnGetRKey", "loadHook error: " + e.getMessage());
            LogUtils.e(e);
        }
    }

    private void parseRKey(JSONObject json) {
        try {
            if (!json.has("4")) {
                LogUtils.e("OnGetRKey", "json doesn't have key '4'");
                return;
            }

            JSONObject obj4 = json.getJSONObject("4");
            if (!obj4.has("4")) {
                LogUtils.e("OnGetRKey", "obj4 doesn't have key '4'");
                return;
            }

            JSONObject obj4_4 = obj4.getJSONObject("4");
            if (!obj4_4.has("1")) {
                LogUtils.e("OnGetRKey", "obj4_4 doesn't have key '1'");
                return;
            }

            Object arrObj = obj4_4.get("1");
            if (!(arrObj instanceof JSONArray)) {
                LogUtils.e("OnGetRKey", "arrObj is not JSONArray");
                return;
            }

            JSONArray rkeyArray = (JSONArray) arrObj;
            if (rkeyArray.length() > 0) {
                JSONObject firstItem = rkeyArray.getJSONObject(0);
                friendRkey = firstItem.optString("1", "");
            }

            if (rkeyArray.length() > 1) {
                JSONObject secondItem = rkeyArray.getJSONObject(1);
                groupRkey = secondItem.optString("1", "");
            }
        } catch (Throwable e) {
            LogUtils.e("OnGetRKey", "parseRKey error: " + e.getMessage());
            LogUtils.e(e);
        }
    }
}