package me.lengyu.qedge.hook;

import org.json.JSONObject;

import me.lengyu.qedge.utils.ModuleConfig;

/**
 * @Author 冷雨
 * @Description 心跳下发的用户数据存取入口。
 * 底层走 ModuleConfig（JSON 存储），按 uin 隔离，支持跨进程读取：
 * 宿主进程收到心跳后写入，模块 UI 进程可直接读取展示。
 */
public class UserData {

    private static final String K_NICKNAME = "user_nickname_";
    private static final String K_SIGNATURE = "user_signature_";
    private static final String K_REGISTER_TIME = "user_register_time_";
    private static final String K_QQ_VERSION = "user_qq_version_";
    private static final String K_MODULE_VERSION = "user_module_version_";
    private static final String K_IS_SPONSOR = "user_is_sponsor_";
    private static final String K_SPONSOR_AMOUNT = "user_sponsor_amount_"; // 单位：分
    private static final String K_UPLOAD_PERMISSION = "user_upload_permission_";
    private static final String K_REVIEW_PERMISSION = "user_review_permission_";

    private UserData() {
    }

    /** 用心跳返回的 data 更新本地用户数据（仅在字段存在时写入，避免空值覆盖） */
    public static void update(JSONObject data, String currentUin) {
        if (data == null || currentUin == null || currentUin.isEmpty()) return;
        if (data.has("nickname") && !data.isNull("nickname")) {
            ModuleConfig.INSTANCE.putString(K_NICKNAME + currentUin, data.optString("nickname", ""));
        }
        if (data.has("signature") && !data.isNull("signature")) {
            ModuleConfig.INSTANCE.putString(K_SIGNATURE + currentUin, data.optString("signature", ""));
        }
        if (data.has("register_time") && !data.isNull("register_time")) {
            ModuleConfig.INSTANCE.putString(K_REGISTER_TIME + currentUin, data.optString("register_time", ""));
        }
        if (data.has("is_sponsor")) {
            ModuleConfig.INSTANCE.putBoolean(K_IS_SPONSOR + currentUin, data.optBoolean("is_sponsor", false));
        }
        if (data.has("sponsor_amount")) {
            double amount = data.optDouble("sponsor_amount", 0);
            ModuleConfig.INSTANCE.putLong(K_SPONSOR_AMOUNT + currentUin, Math.round(amount * 100));
        }
        if (data.has("upload_permission")) {
            ModuleConfig.INSTANCE.putBoolean(K_UPLOAD_PERMISSION + currentUin, data.optBoolean("upload_permission", false));
        }
        if (data.has("review_permission")) {
            ModuleConfig.INSTANCE.putBoolean(K_REVIEW_PERMISSION + currentUin, data.optBoolean("review_permission", false));
        }
    }

    /** 记录本地环境信息（模块版本、QQ 版本、昵称兜底），由心跳发送时写入 */
    public static void updateEnv(String currentUin, String moduleVersion, String qqVersion, String nickname) {
        if (currentUin == null || currentUin.isEmpty()) return;
        if (moduleVersion != null) ModuleConfig.INSTANCE.putString(K_MODULE_VERSION + currentUin, moduleVersion);
        if (qqVersion != null) ModuleConfig.INSTANCE.putString(K_QQ_VERSION + currentUin, qqVersion);
        // 昵称兜底：仅当本地还没有昵称时用心跳上报的昵称，避免覆盖服务器下发值
        if (nickname != null && !nickname.isEmpty()
                && ModuleConfig.INSTANCE.getString(K_NICKNAME + currentUin, "").isEmpty()) {
            ModuleConfig.INSTANCE.putString(K_NICKNAME + currentUin, nickname);
        }
    }

    public static String getNickname(String uin) {
        return ModuleConfig.INSTANCE.getString(K_NICKNAME + uin, "");
    }

    public static String getSignature(String uin) {
        return ModuleConfig.INSTANCE.getString(K_SIGNATURE + uin, "");
    }

    public static String getRegisterTime(String uin) {
        return ModuleConfig.INSTANCE.getString(K_REGISTER_TIME + uin, "");
    }

    public static String getModuleVersion(String uin) {
        return ModuleConfig.INSTANCE.getString(K_MODULE_VERSION + uin, "");
    }

    public static String getQqVersion(String uin) {
        return ModuleConfig.INSTANCE.getString(K_QQ_VERSION + uin, "");
    }

    public static boolean isSponsor(String uin) {
        return ModuleConfig.INSTANCE.getBoolean(K_IS_SPONSOR + uin, false);
    }

    /** 赞助金额，单位：分（0 表示没有/非赞助） */
    public static long getSponsorAmountCents(String uin) {
        return ModuleConfig.INSTANCE.getLong(K_SPONSOR_AMOUNT + uin, 0);
    }

    public static boolean hasUploadPermission(String uin) {
        return ModuleConfig.INSTANCE.getBoolean(K_UPLOAD_PERMISSION + uin, false);
    }

    public static boolean hasReviewPermission(String uin) {
        return ModuleConfig.INSTANCE.getBoolean(K_REVIEW_PERMISSION + uin, false);
    }
}
