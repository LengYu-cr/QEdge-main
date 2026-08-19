package me.lengyu.qedge.coldrain;

import android.content.Context;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

import com.tencent.qqnt.kernel.nativeinterface.MsgElement;
import com.tencent.qqnt.kernelpublic.nativeinterface.Contact;
import com.tencent.mobileqq.data.troop.TroopInfo;
import me.lengyu.qedge.coldrain.features.BanDetectionFeature;
import me.lengyu.qedge.coldrain.features.GroupManagerFeature;
import me.lengyu.qedge.coldrain.features.MenuFeature;
import me.lengyu.qedge.coldrain.features.QueryFeature;
import me.lengyu.qedge.coldrain.features.QuestionFeature;
import me.lengyu.qedge.coldrain.features.SignInFeature;
import me.lengyu.qedge.coldrain.features.StatusFeature;
import me.lengyu.qedge.coldrain.features.WelcomeJoinFeature;
import me.lengyu.qedge.coldrain.features.WelcomeQuitFeature;
import me.lengyu.qedge.coldrain.features.VideoParseFeature;
import me.lengyu.qedge.coldrain.features.ImageMenuFeature;
import me.lengyu.qedge.coldrain.features.VideoMenuFeature;
import me.lengyu.qedge.coldrain.features.MusicMenuFeature;
import me.lengyu.qedge.coldrain.features.ImageFeature;
import me.lengyu.qedge.coldrain.features.WeatherFeature;
import me.lengyu.qedge.coldrain.features.HourlyChimeFeature;
import me.lengyu.qedge.coldrain.features.TitleFeature;
import me.lengyu.qedge.coldrain.features.LikeFeature;
import me.lengyu.qedge.coldrain.features.AutoAdminFeature;
import me.lengyu.qedge.coldrain.features.AtFeature;
import me.lengyu.qedge.coldrain.features.AvatarMenuFeature;
import me.lengyu.qedge.coldrain.features.BlackWhiteListFeature;
import me.lengyu.qedge.hook.api.OnReceiveMsg;
import me.lengyu.qedge.plugin.bean.MemberInfo;
import me.lengyu.qedge.plugin.bean.MsgData;
import me.lengyu.qedge.utils.HttpUtils;
import me.lengyu.qedge.utils.LogUtils;
import me.lengyu.qedge.utils.Toasts;
import me.lengyu.qedge.utils.qq.QQCurrentEnv;
import me.lengyu.qedge.utils.ReflectUtils;
import me.lengyu.qedge.utils.qq.CookieTool;
import me.lengyu.qedge.utils.qq.MsgTool;

public class ColdRainCore {
    private static final String TAG = "ColdRainCore";
    private static final String CONFIG_FILE_NAME = "config.json";
    private static volatile ColdRainCore instance;
    private Context context;
    private JSONObject configData;
    private File configFile;
    private boolean initialized = false;
    private long lastFileModified = 0;

    private final Map<String, ColdRainFeature> features = new HashMap<>();

    private static final String[] PERSONAL_FEATURES = {
        "feature_status",
        "feature_group_manager",
        "feature_at"
    };

    /** 仅群聊可用的功能，好友聊天不适用 */
    private static final String[] GROUP_ONLY_FEATURES = {
        "feature_welcome_join",
        "feature_welcome_quit",
        "feature_hourly",
        "feature_black_white_list",
        "feature_ban",
        "feature_autoadmin",
        "feature_at",
        "feature_title",
        "feature_signin",
        "feature_question",
        "feature_group_manager"
    };

    public static ColdRainCore getInstance() {
        if (instance == null) {
            synchronized (ColdRainCore.class) {
                if (instance == null) {
                    instance = new ColdRainCore();
                }
            }
        }
        return instance;
    }

    private ColdRainCore() {}

    public void init(Context ctx) {
        if (initialized) return;
        this.context = ctx.getApplicationContext();
        
        initConfigFile();
        loadConfig();
        initialized = true;

        registerFeatures();
        initDefaultConfig();
        setupMessageListener();
        WelcomeJoinFeature.registerListeners(this);
        WelcomeQuitFeature.registerListeners(this);
        BlackWhiteListFeature.registerListeners(this);
        HourlyChimeFeature.startTimer(this);
    }

    private File dataDir;

    private void initConfigFile() {
        String basePath = QQCurrentEnv.getLocalPath()
            + "Android/media/" + context.getPackageName() + "/冷雨Java";
        File baseDir = new File(basePath);
        if (!baseDir.exists()) {
            baseDir.mkdirs();
        }
        configFile = new File(baseDir, CONFIG_FILE_NAME);
        
        // 数据目录
        dataDir = new File(baseDir, "data");
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
    }

    private void loadConfig() {
        configData = new JSONObject();
        if (configFile.exists()) {
            try {
                FileReader reader = new FileReader(configFile);
                StringBuilder sb = new StringBuilder();
                char[] buffer = new char[1024];
                int len;
                while ((len = reader.read(buffer)) != -1) {
                    sb.append(buffer, 0, len);
                }
                reader.close();
                configData = new JSONObject(sb.toString());
                lastFileModified = configFile.lastModified();
            } catch (Exception e) {
                LogUtils.e(TAG, "loadConfig error: " + e.getMessage());
                configData = new JSONObject();
            }
        }
    }

    public void reloadConfig() {
        loadConfig();
    }

    private void saveConfig() {
        try {
            FileWriter writer = new FileWriter(configFile);
            writer.write(configData.toString(4));
            writer.close();
        } catch (Exception e) {
            LogUtils.e(TAG, "saveConfig error: " + e.getMessage());
        }
    }

    private boolean getBoolean(String key, boolean defaultValue) {
        if (configData == null) {
            return defaultValue;
        }
        checkAndReloadIfModified();
        if (configData.has(key)) {
            return configData.optBoolean(key, defaultValue);
        }
        return defaultValue;
    }

    private void setBoolean(String key, boolean value) {
        try {
            configData.put(key, value);
            saveConfig();
        } catch (Exception e) {
            LogUtils.e(TAG, "setBoolean error: " + e.getMessage());
        }
    }

    private String getString(String key, String defaultValue) {
        if (configData == null) {
            return defaultValue;
        }
        checkAndReloadIfModified();
        if (configData.has(key)) {
            return configData.optString(key, defaultValue);
        }
        return defaultValue;
    }

    private void setString(String key, String value) {
        try {
            configData.put(key, value);
            saveConfig();
        } catch (Exception e) {
            LogUtils.e(TAG, "setString error: " + e.getMessage());
        }
    }

    public void setConfigString(String key, String value) {
        setString(key, value);
    }

    public String getConfigString(String key, String defaultValue) {
        return getString(key, defaultValue);
    }

    public void setConfigBoolean(String key, boolean value) {
        setBoolean(key, value);
    }

    public boolean getConfigBoolean(String key, boolean defaultValue) {
        return getBoolean(key, defaultValue);
    }

    public void setConfigInt(String key, int value) {
        try {
            configData.put(key, value);
            saveConfig();
        } catch (Exception e) {
            LogUtils.e(TAG, "setConfigInt error: " + e.getMessage());
        }
    }

    public int getConfigInt(String key, int defaultValue) {
        if (configData.has(key)) {
            return configData.optInt(key, defaultValue);
        }
        return defaultValue;
    }

    // ========== 数据文件操作 ==========
    public JSONObject getDataFile(String fileName) {
        File file = new File(dataDir, fileName);
        if (!file.exists()) {
            return new JSONObject();
        }
        try {
            FileReader reader = new FileReader(file);
            StringBuilder sb = new StringBuilder();
            char[] buffer = new char[1024];
            int len;
            while ((len = reader.read(buffer)) != -1) {
                sb.append(buffer, 0, len);
            }
            reader.close();
            return new JSONObject(sb.toString());
        } catch (Exception e) {
            LogUtils.e(TAG, "getDataFile error: " + e.getMessage());
            return new JSONObject();
        }
    }

    public void saveDataFile(String fileName, JSONObject data) {
        try {
            File file = new File(dataDir, fileName);
            FileWriter writer = new FileWriter(file);
            writer.write(data.toString(4));
            writer.close();
        } catch (Exception e) {
            LogUtils.e(TAG, "saveDataFile error: " + e.getMessage());
        }
    }

    public java.util.Set<String> getAllConfigKeys() {
        java.util.Set<String> keys = new java.util.HashSet<>();
        java.util.Iterator<String> it = configData.keys();
        while (it.hasNext()) {
            keys.add(it.next());
        }
        return keys;
    }

    public void removeConfig(String key) {
        configData.remove(key);
        saveConfig();
    }

    private void initDefaultConfig() {
        if (!configData.has("master_enabled")) {
            setBoolean("master_enabled", false);
        }
        if (!configData.has("menu_name")) {
            setString("menu_name", "菜单");
        }
        for (String featureKey : features.keySet()) {
            if (!configData.has(featureKey)) {
                setBoolean(featureKey, true);
            }
        }
    }

    private void registerFeatures() {
        features.put("feature_status", new StatusFeature());
        features.put("feature_question", new QuestionFeature());
        features.put("feature_signin", new SignInFeature());
        features.put("feature_group_manager", new GroupManagerFeature());
        features.put("feature_black_white_list", new BlackWhiteListFeature());
        features.put("feature_welcome_join", new WelcomeJoinFeature());
        features.put("feature_welcome_quit", new WelcomeQuitFeature());
        features.put("feature_ban", new BanDetectionFeature());
        features.put("feature_query", new QueryFeature());
        features.put("feature_video_parse", new VideoParseFeature());
        features.put("feature_image_menu", new ImageMenuFeature());
        features.put("feature_video_menu", new VideoMenuFeature());
        features.put("feature_music", new MusicMenuFeature());
        features.put("feature_image", new ImageFeature());
        features.put("feature_weather", new WeatherFeature());
        features.put("feature_hourly", new HourlyChimeFeature());
        features.put("feature_title", new TitleFeature());
        features.put("feature_like", new LikeFeature());
        features.put("feature_autoadmin", new AutoAdminFeature());
        features.put("feature_avatar_menu", new AvatarMenuFeature());
        features.put("feature_at", new AtFeature());
    }

    private void setupMessageListener() {
        OnReceiveMsg.registerListener(new OnReceiveMsg.ReceiveMsgListener() {
            @Override
            public void onReceive(Object msgRecord) {
                try {
                    boolean isMasterOn = isMasterEnabled();
                    if (!isMasterOn) {
                        return;
                    }
                    handleMessage(msgRecord);
                } catch (Throwable e) {
                    LogUtils.e(TAG, "handleMessage error: " + e.getMessage());
                }
            }
        });
    }

    private void handleMessage(Object msgRecord) {
        try {
            if (!initialized) {
                return;
            }
            
            MsgData msgData = new MsgData(msgRecord);

            String text = msgData.msg.trim();
            if (text.isEmpty()) return;

            // Q群管家 token 捕获：监听 Q群管家(2854196310)发送的 JSON 卡片
            if (msgData.type == 2 && "2854196310".equals(msgData.userUin) &&
                msgData.msgType == 11 && text.contains("\"token\"")) {
                handleGuanjiaTokenMessage(msgData);
                return;
            }

            if (text.equals("开机")) {
                if (!isAdminOrSelf(msgData)) {
                    return;
                }
                String effectiveKey = (msgData.type == 2) ? msgData.peerUin : "friend_global";
                setBoolean("group_master_enabled_" + effectiveKey, true);
                reply(msgData, (msgData.type == 2 ? "本群已开机" : "好友聊天已开机") + "！\n发送【" + getMenuName() + "】查看菜单");
                return;
            }

            if (text.equals("关机")) {
                if (!isAdminOrSelf(msgData)) {
                    return;
                }
                String effectiveKey = (msgData.type == 2) ? msgData.peerUin : "friend_global";
                setBoolean("group_master_enabled_" + effectiveKey, false);
                reply(msgData, msgData.type == 2 ? "本群已关机" : "好友聊天已关机");
                return;
            }

            if (isAdminOrSelf(msgData)) {
                String groupFeature = matchGroupFeatureCommand(text);
                if (groupFeature != null) {
                    handleGroupFeatureToggle(msgData, groupFeature, text.startsWith("开启"));
                    return;
                }
            }

            String menuName = getMenuName();
            if (text.equals(menuName)) {
                if (isMenuRestricted() && !isAdminOrSelf(msgData)) {
                    return;
                }
                boolean isMasterOn = isMasterEnabled();
                String effectiveKey = (msgData.type == 2) ? msgData.peerUin : "friend_global";
                boolean isGroupOn = getBoolean("group_master_enabled_" + effectiveKey, false);

                if (!isMasterOn || !isGroupOn) {
                    if (!isAdminOrSelf(msgData)) {
                        return;
                    }
                    if (!isMasterOn) {
                        return;
                    }
                    if (!isGroupOn) {
                        reply(msgData, msgData.type == 2 ? "本群未开机" : "好友聊天未开机");
                        return;
                    }
                }
                new MenuFeature().handle(msgData, this);
                return;
            }

            for (Map.Entry<String, ColdRainFeature> entry : features.entrySet()) {
                try {
                    String featureKey = entry.getKey();
                    ColdRainFeature feature = entry.getValue();

                    if (!feature.shouldHandle(msgData)) continue;
                    if (!canTriggerFeature(featureKey, msgData)) continue;

                    feature.handle(msgData, this);
                    break;
                } catch (Throwable e) {
                    LogUtils.e(TAG, "Feature handle error: " + e.getMessage());
                }
            }
        } catch (Throwable e) {
            LogUtils.e(TAG, "handleMessage error: " + e.getMessage());
        }
    }

    private boolean canTriggerFeature(String featureKey, MsgData msgData) {
        if (!isMasterEnabled()) {
            return false;
        }

        // 菜单限制：只有主人/管理员/自己可以触发
        if (isMenuRestricted() && !isAdminOrSelf(msgData)) {
            return false;
        }

        // 群聊专属功能在好友聊天中不可用
        if (msgData.type != 2 && isGroupOnlyFeature(featureKey)) {
            return false;
        }

        // 好友聊天使用全局key，群聊使用群号
        String effectiveKey = (msgData.type == 2 && msgData.peerUin != null && !msgData.peerUin.isEmpty())
            ? msgData.peerUin : "friend_global";

        // 菜单命令不受功能开关和群功能开关限制
        if (isMenuCommand(msgData.msg)) {
            return getBoolean("group_master_enabled_" + effectiveKey, false);
        }

        if (!isFeatureEnabled(featureKey) && !featureKey.equals("feature_status")) {
            return false;
        }
        if (isPersonalFeature(featureKey)) {
            return true;
        }
        if (!getBoolean("group_master_enabled_" + effectiveKey, false)) {
            return false;
        }
        return isGroupFeatureEnabled(featureKey, effectiveKey);
    }
    
    private boolean isMenuCommand(String text) {
        if (text == null) return false;
        text = text.trim();
        return text.equals("赞我点赞") ||
            text.equals("图片菜单") ||
            text.equals("查询系统") ||
            text.equals("音乐菜单") ||
            text.equals("签到系统") ||
            text.equals("问答功能") ||
            text.equals("视频解析") ||
            text.equals("解析菜单") ||
            text.equals("天气菜单") ||
            text.equals("头衔功能") ||
            text.equals("自助上管") ||
            text.equals("整点报时") ||
            text.equals("艾特处理") ||
            text.equals("群管菜单") ||
            text.equals("提示系统") ||
            text.equals("违禁系统") ||
            text.equals("冷雨菜单") ||
            text.equals("菜单");
    }

    public boolean isPersonalFeature(String featureKey) {
        for (String f : PERSONAL_FEATURES) {
            if (f.equals(featureKey)) return true;
        }
        return false;
    }

    public boolean isGroupOnlyFeature(String featureKey) {
        for (String f : GROUP_ONLY_FEATURES) {
            if (f.equals(featureKey)) return true;
        }
        return false;
    }

    public boolean isGroupFeatureEnabled(String featureKey, String groupUin) {
        return getBoolean("group_" + featureKey + "_" + groupUin, false);
    }

    public void setGroupFeatureEnabled(String featureKey, String groupUin, boolean enabled) {
        setBoolean("group_" + featureKey + "_" + groupUin, enabled);
    }

    public boolean isGroupMasterEnabled(String groupUin) {
        return getBoolean("group_master_enabled_" + groupUin, false);
    }

    public void setGroupMasterEnabled(String groupUin, boolean enabled) {
        setBoolean("group_master_enabled_" + groupUin, enabled);
    }

    private static final String[][] FEATURE_NAME_MAP = {
        {"进群欢迎", "feature_welcome_join"},
        {"退群提示", "feature_welcome_quit"},
        {"违禁系统", "feature_ban"},
        {"黑白名单", "feature_black_white_list"},
        {"签到系统", "feature_signin"},
        {"问答功能", "feature_question"},
        {"查询系统", "feature_query"},
        {"天气系统", "feature_weather"},
        {"整点报时", "feature_hourly"},
        {"视频解析", "feature_video_parse"},
        {"图片菜单", "feature_image_menu"},
        {"视频菜单", "feature_video_menu"},
        {"音乐菜单", "feature_music"},
        {"图片功能", "feature_image"},
        {"自助上管", "feature_autoadmin"},
        {"头衔功能", "feature_title"},
        {"赞我点赞", "feature_like"}
    };

    private String matchGroupFeatureCommand(String text) {
        for (String[] mapping : FEATURE_NAME_MAP) {
            String name = mapping[0];
            if (text.equals("开启" + name) || text.equals("关闭" + name)) {
                return mapping[1];
            }
        }
        return null;
    }

    private void handleGroupFeatureToggle(MsgData msgData, String featureKey, boolean enable) {
        if (msgData.type != 2 && isGroupOnlyFeature(featureKey)) {
            if (isMenuCommand(msgData.msg)) {
                reply(msgData, "此功能仅支持群聊");
            }
            return;
        }
        String effectiveKey = (msgData.type == 2) ? msgData.peerUin : "friend_global";
        setGroupFeatureEnabled(featureKey, effectiveKey, enable);
        String featureName = "";
        for (String[] mapping : FEATURE_NAME_MAP) {
            if (mapping[1].equals(featureKey)) {
                featureName = mapping[0];
                break;
            }
        }
        reply(msgData, (enable ? "已开启" : "已关闭") + (msgData.type == 2 ? "本群" : "好友") + featureName);
    }

    public boolean isInitialized() {
        return initialized;
    }

    public boolean isMasterEnabled() {
        checkAndReloadIfModified();
        return getBoolean("master_enabled", false);
    }

    public void setMasterEnabled(boolean enabled) {
        setBoolean("master_enabled", enabled);
    }

    private void checkAndReloadIfModified() {
        if (configFile == null) {
            return;
        }
        if (configFile.exists() && configFile.lastModified() > lastFileModified) {
            loadConfig();
        }
    }

    public boolean isFeatureEnabled(String key) {
        checkAndReloadIfModified();
        return getBoolean(key, true);
    }

    public String getMenuName() {
        checkAndReloadIfModified();
        return getString("menu_name", "菜单");
    }

    public String getReplyMode() {
        checkAndReloadIfModified();
        return getString("reply_mode", "text");
    }

    public Context getContext() {
        return context;
    }

    public String getMyUin() {
        try {
            return QQCurrentEnv.getCurrentUin();
        } catch (Throwable e) {
            return "";
        }
    }

    public boolean isMenuRestricted() {
        return getBoolean("menu_restricted", false);
    }

    public String getMasterUin() {
        return getString("master_uin", "");
    }

    public String getGlobalAdmins() {
        return getString("global_admins", "");
    }

    public String getGroupAdmins(String groupUin) {
        return getString("group_admins_" + groupUin, "");
    }

    public boolean isAdminOrSelf(MsgData msgData) {
        if (msgData.sendType == 1) return true;

        String uin = msgData.userUin;
        if (uin == null || uin.isEmpty()) {
            uin = msgData.userUid;
        }
        if (uin == null || uin.isEmpty()) return false;

        String myUin = getMyUin();
        if (!myUin.isEmpty() && uin.equals(myUin)) return true;

        String masterUin = getMasterUin();
        if (!masterUin.isEmpty() && uin.equals(masterUin)) return true;

        String globalAdmins = getGlobalAdmins();
        if (!globalAdmins.isEmpty()) {
            for (String admin : globalAdmins.split(",")) {
                if (admin.trim().equals(uin)) return true;
            }
        }

        if (msgData.type == 2 && msgData.peerUin != null && !msgData.peerUin.isEmpty()) {
            String groupAdmins = getGroupAdmins(msgData.peerUin);
            if (!groupAdmins.isEmpty()) {
                for (String admin : groupAdmins.split(",")) {
                    if (admin.trim().equals(uin)) return true;
                }
            }
        }

        return false;
    }

    public boolean isAdminOrSelfForUin(String groupUin, String uin) {
        if (uin == null || uin.isEmpty()) return false;

        String myUin = getMyUin();
        if (!myUin.isEmpty() && uin.equals(myUin)) return true;

        String masterUin = getMasterUin();
        if (!masterUin.isEmpty() && uin.equals(masterUin)) return true;

        String globalAdmins = getGlobalAdmins();
        if (!globalAdmins.isEmpty()) {
            for (String admin : globalAdmins.split(",")) {
                if (admin.trim().equals(uin)) return true;
            }
        }

        if (groupUin != null && !groupUin.isEmpty()) {
            String groupAdmins = getGroupAdmins(groupUin);
            if (!groupAdmins.isEmpty()) {
                for (String admin : groupAdmins.split(",")) {
                    if (admin.trim().equals(uin)) return true;
                }
            }
        }

        return false;
    }

    public void reply(MsgData msgData, String text) {
        try {
            if (msgData.contact == null) return;

            String mode = getString("reply_mode", "text");
            String qun = msgData.peerUin;
            int mtype = msgData.type;

            text = getStandardMsg(msgData, text);

            switch (mode) {
                case "text":
                case "文字":
                    MsgTool.sendMsg(msgData.contact, text);
                    break;
                case "card":
                case "卡片":
                    sendCardReply(qun, text, mtype);
                    break;
                case "image":
                case "图片":
                    sendImageReply(qun, text, mtype);
                    break;
                case "forward":
                case "转发":
                    sendForwardReply(msgData, text);
                    break;
                case "markdown":
                case "MarkDown":
                    sendMarkdownReply(qun, text, mtype);
                    break;
                case "reply":
                case "回复":
                    if (msgData.msgId > 0) {
                        MsgTool.sendReplyMsg(msgData.contact, msgData.msgId, text);
                    } else {
                        MsgTool.sendMsg(msgData.contact, text);
                    }
                    break;
                case "guanjia":
                case "管家":
                    sendGuanjiaReply(qun, text, mtype);
                    break;
                default:
                    MsgTool.sendMsg(msgData.contact, text);
            }
        } catch (Throwable e) {
            LogUtils.e(TAG, "reply error: " + e.getMessage());
        }
    }

    public void reply(String peerUin, int type, String text) {
        try {
            String mode = getString("reply_mode", "text");
            Contact contact = new Contact(type, peerUin, "");

            switch (mode) {
                case "text":
                case "文字":
                    MsgTool.sendMsg(contact, text);
                    break;
                case "card":
                case "卡片":
                    sendCardReply(peerUin, text, type);
                    break;
                case "image":
                case "图片":
                    sendImageReply(peerUin, text, type);
                    break;
                case "markdown":
                case "MarkDown":
                    sendMarkdownReply(peerUin, text, type);
                    break;
                case "guanjia":
                case "管家":
                    sendGuanjiaReply(peerUin, text, type);
                    break;
                default:
                    MsgTool.sendMsg(contact, text);
            }
        } catch (Throwable e) {
            LogUtils.e(TAG, "reply error: " + e.getMessage());
        }
    }

    private String getStandardMsg(MsgData data, String text) {
        if (text == null || text.isEmpty()) return "";
        String msg = text.replace("[at]", "[atUin=" + data.userUin + "]");
        msg = msg.replace("[qq]", QQCurrentEnv.getCurrentUin());
        msg = msg.replace("[uin]", data.userUin);
        msg = msg.replace("[qun]", data.peerUin);
        if (msg.contains("[time]")) {
            msg = msg.replace("[time]", new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date()));
        }
        if (msg.contains("[图片")) {
            msg = msg.replace("[图片", "\n[pic=");
        }
        if (msg.contains("[Name]")) {
            msg = msg.replace("[Name]", getUserName(data.userUin, data.peerUin));
        }
        return msg;
    }

    private String getUserName(String uin, String qun) {
        try {
            if (qun != null && !qun.isEmpty()) {
                try {
                    MemberInfo memberInfo = me.lengyu.qedge.utils.qq.TroopTool.INSTANCE.getMemberInfo(qun, uin);
                    if (memberInfo != null && memberInfo.uinName != null && !memberInfo.uinName.isEmpty()) {
                        return memberInfo.uinName;
                    }
                } catch (Throwable ignored) {}
            }
            try {
                String uid = me.lengyu.qedge.utils.qq.FriendTool.getUidFromUin(uin);
                com.tencent.qqnt.ntrelation.friendsinfo.api.IFriendsInfoService service =
                    me.lengyu.qedge.utils.qq.QQServiceHelper.getApi(com.tencent.qqnt.ntrelation.friendsinfo.api.IFriendsInfoService.class);
                if (service != null) {
                    String nick = service.getNickWithUid(uid, "");
                    if (nick != null && !nick.isEmpty()) {
                        return nick;
                    }
                }
            } catch (Throwable ignored) {}
        } catch (Throwable ignored) {}
        return uin;
    }

    private void sendCardReply(String qun, String text, int mtype) {
        try {
            String dd = "冷雨Java";
            String card = "{\"app\":\"com.tencent.bot.task.deblock\",\"desc\":\"\",\"bizsrc\":\"\",\"view\":\"index\",\"ver\":\"2.0.4.0\",\"prompt\":\"冷雨Java\",\"appID\":\"\",\"sourceName\":\"\",\"actionData\":\"\",\"actionData_A\":\"\",\"sourceUrl\":\"\",\"meta\":{\"detail\":{\"appID\":\"\",\"botName\":\"冷雨Java\",\"cmdTitle\":\"" + dd + "\",\"content\":\"" + text.replace("\"", "") + "\",\"guildID\":\"\",\"iconLeft\":[{\"num\":\"10\"}],\"iconRight\":[],\"receiverName\":\"\"}},\"config\":{\"autosize\":1,\"ctime\":1661659096,\"token\":\"卡片模式仅自己可见\"},\"text\":\"\",\"extraApps\":[],\"sourceAd\":\"\",\"extra\":\"\"}";
            MsgTool.sendCard(qun, card, mtype);
        } catch (Throwable e) {
            LogUtils.e(TAG, "sendCardReply error: " + e.getMessage());
            MsgTool.sendMsg(qun, text, mtype);
            Toasts.showToast("发送卡片失败");
        }
    }

    private void sendImageReply(String qun, String text, int mtype) {
        try {
            if (text.contains("[pic=")) {
                MsgTool.sendMsg(qun, text, mtype);
                return;
            }
            String pic = getString("image_mode_bg", "");
            String textcolor = getString("image_mode_color", "#000000");
            text = text.replace("𝒥𝒶𝓋𝒶", "JAVA");
            text = text.replace("╰┅━ 赞助作者 ━┅╯", "赞助作者");
            text = text.replace("║", "");
            text = text.replace("+", "[and]");
            text = text.replace("\n", "↔");
            if (pic.isEmpty()) {
                pic = "https://api.yuafeng.cn/API/ly/bizhi.php";
            }
            if (!pic.startsWith("http")) {
                pic = "https://api.yuafeng.cn/API/ly/bizhi.php";
            }
            String imgUrl = "https://api.yuafeng.cn/API/ly/ttf/gjtwhc.php?text=" +
                java.net.URLEncoder.encode(text, "UTF-8") +
                "&image=" + java.net.URLEncoder.encode(pic, "UTF-8") +
                "&fontsize=50";
            MsgTool.sendPic(qun, imgUrl, mtype);
        } catch (Throwable e) {
            LogUtils.e(TAG, "sendImageReply error: " + e.getMessage());
            MsgTool.sendMsg(qun, text, mtype);
            Toasts.showToast("发送图片失败");
        }
    }

    private void sendForwardReply(MsgData msgData, String text) {
        try {
            String qun = msgData.peerUin;
            int mtype = msgData.type;
            String myUin = QQCurrentEnv.getCurrentUin();

            org.json.JSONObject json = new org.json.JSONObject();
            org.json.JSONArray zf = new org.json.JSONArray();
            org.json.JSONArray yh = new org.json.JSONArray();
            zf.put(msgData.msg);
            zf.put(text);
            yh.put(msgData.userUin);
            yh.put(myUin);
            json.put("zf", zf);
            json.put("yh", yh);
            json.put("ms", "冷雨Java");
            json.put("type", "text");
            org.json.JSONObject config = new org.json.JSONObject();
            config.put("source", "聊天记录");
            config.put("waixian", "[聊天记录]点击查看");
            config.put("summary", "冷雨Java");
            config.put("qpid", 2079307);
            json.put("config", config);

            String result = HttpUtils.post("https://api.s01s.cn/API/lt_zf/", json.toString());
            if (result == null || result.isEmpty() || result.equals("访问网页失败")) {
                MsgTool.sendMsg(qun, text, mtype);
            } else {
                MsgTool.sendCard(qun, result, mtype);
            }
        } catch (Throwable e) {
            LogUtils.e(TAG, "sendForwardReply error: " + e.getMessage());
            MsgTool.sendMsg(msgData.peerUin, text, msgData.type);
            Toasts.showToast("发送聊天记录失败");
        }
    }

    private void sendMarkdownReply(String qun, String text, int mtype) {
        try {
            String[] texts = text.split("\n");
            StringBuilder result = new StringBuilder();
            for (String info : texts) {
                if (info.contains("[pic=")) {
                    String picUrl = info.replace("[pic=", "").replace("]", "");
                    result.append("\n[图片消息，点我查看] (mqqapi://openhalfscreenweb/?height=1920&url=" +
                        java.net.URLEncoder.encode(picUrl, "UTF-8") + ")");
                } else {
                    result.append("\n[" + info + "] (mqqapi://aio/inlinecmd?command=" +
                        java.net.URLEncoder.encode(info, "UTF-8") + "&enter=false&reply=false)");
                }
            }
            sendMarkDown(qun, result.toString(), mtype);
        } catch (Throwable e) {
            LogUtils.e(TAG, "sendMarkdownReply error: " + e.getMessage());
            MsgTool.sendMsg(qun, text, mtype);
            Toasts.showToast("发送Markdown失败");
        }
    }

    private void sendGuanjiaReply(String qun, String text, int mtype) {
        try {
            if (mtype != 2) {
                MsgTool.sendMsg(qun, text, mtype);
                return;
            }

            String myUin = QQCurrentEnv.getCurrentUin();

            // 检查机器人是否是群主或管理员
            TroopInfo troopInfo = me.lengyu.qedge.utils.qq.TroopTool.INSTANCE.getGroupInfo(qun);
            if (troopInfo == null) {
                MsgTool.sendMsg(qun, text, mtype);
                return;
            }
            boolean isOwner = troopInfo.isTroopOwner(myUin);
            boolean isAdmin = troopInfo.isTroopAdmin(myUin);
            if (!isOwner && !isAdmin) {
                MsgTool.sendMsg(qun, text, mtype);
                return;
            }

            String pskey = CookieTool.getPskey("qun.qq.com");
            String skey = CookieTool.getSkey();
            if (pskey == null || skey == null) {
                MsgTool.sendMsg(qun, text, mtype);
                return;
            }
            long bkn = CookieTool.getBkn(skey);
            String cookie = "p_uin=" + QQCurrentEnv.getCookieUin() + ";uin=" + QQCurrentEnv.getCookieUin() +
                ";skey=" + skey + ";p_skey=" + pskey;

            // 获取已保存的 token
            String token = getString("guanjia_token_" + qun, "");

            // 生成随机短字符串作为问题和关键词，answer 为实际要发送的内容
            String randomQ = generateRandomQuestion();

            // 添加问答
            String addResult = guanjiaAddQna(qun, bkn, cookie, randomQ, text);
            if (addResult.startsWith("添加失败") || addResult.equals("你不是管理") || addResult.equals("访问频率过快，稍后再试")) {
                MsgTool.sendMsg(qun, text, mtype);
                return;
            }

            if (token.isEmpty()) {
                // 无 token：艾特 Q群管家获取 token
                MsgTool.sendMsg(qun, "Come on![atUin=2854196310]", mtype);
                setString("guanjia_trigger_question_" + qun, randomQ);
                setString("guanjia_trigger_uin_" + qun, myUin);
            } else {
                // 有 token：触发问答
                String triggerResult = guanjiaTriggerQna(qun, bkn, cookie, randomQ, token);
                if (triggerResult.equals("会话过期")) {
                    // token 过期，重新艾特 Q群管家
                    MsgTool.sendMsg(qun, "Come on![atUin=2854196310]", mtype);
                    setString("guanjia_trigger_question_" + qun, randomQ);
                    setString("guanjia_trigger_uin_" + qun, myUin);
                } else if (triggerResult.equals("成功")) {
                    guanjiaDeleteQna(qun, bkn, cookie, "1");
                    guanjiaDeleteQna(qun, bkn, cookie, "2");
                } else {
                    MsgTool.sendMsg(qun, text, mtype);
                }
            }
        } catch (Throwable e) {
            LogUtils.e(TAG, "sendGuanjiaReply error: " + e.getMessage());
            MsgTool.sendMsg(qun, text, mtype);
            Toasts.showToast("发送管家问答失败");
        }
    }

    private void handleGuanjiaTokenMessage(MsgData msgData) {
        try {
            String qun = msgData.peerUin;
            String triggerUin = getString("guanjia_trigger_uin_" + qun, "");
            String triggerQuestion = getString("guanjia_trigger_question_" + qun, "");
            if (triggerUin.isEmpty() || triggerQuestion.isEmpty()) return;

            // 解析 JSON 卡片提取 token
            String jsonStr = msgData.msg.replace("}\n", "}");
            JSONObject json = new JSONObject(jsonStr);
            String token = json.getJSONObject("meta").getJSONObject("metadata").getString("token");
            setString("guanjia_token_" + qun, token);

            // 用新 token 触发问答
            String pskey = CookieTool.getPskey("qun.qq.com");
            String skey = CookieTool.getSkey();
            if (pskey == null || skey == null) return;
            long bkn = CookieTool.getBkn(skey);
            String cookie = "p_uin=" + QQCurrentEnv.getCookieUin() + ";uin=" + QQCurrentEnv.getCookieUin() +
                ";skey=" + skey + ";p_skey=" + pskey;

            String result = guanjiaTriggerQna(qun, bkn, cookie, triggerQuestion, token);
            if (result.equals("成功")) {
                // 撤回 Q群管家的卡片消息
                if (msgData.msgId > 0) {
                    MsgTool.recallMsg(2, qun, msgData.msgId);
                }
                guanjiaDeleteQna(qun, bkn, cookie, "1");
                guanjiaDeleteQna(qun, bkn, cookie, "2");
            } else {
                MsgTool.sendMsg(qun, result, 2);
                Toasts.showToast("触发管家问答失败：" + result);
            }

            // 清理触发状态
            setString("guanjia_trigger_uin_" + qun, "");
            setString("guanjia_trigger_question_" + qun, "");
        } catch (Throwable e) {
            LogUtils.e(TAG, "handleGuanjiaTokenMessage error: " + e.getMessage());
            Toasts.showToast("处理管家问答失败");
        }
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }

    private String generateRandomQuestion() {
        String chars = "abcdefghijklmnopqrstuvwxyz0123456789";
        java.util.Random rnd = new java.util.Random();
        StringBuilder sb = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private String guanjiaAddQna(String qun, long bkn, String cookie, String question, String answer) {
        try {
            String url = "https://web.qun.qq.com/qunrobot/proxy/domain/app.qun.qq.com/cgi-bin/guanjia_robot/qna_setting/set_qna?bkn=" + bkn;
            String q = escapeJson(question);
            String a = escapeJson("\n" + answer);
            String body = "{\"bkn\":" + bkn + ",\"group_id\":" + qun +
                ",\"qna_item\":{\"slot\":0,\"question\":\"" + q + "\",\"answer\":\"" + a +
                "\",\"keyword\":[\"" + q + "\"]}}";
            HashMap<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "application/json");
            headers.put("Cookie", cookie);
            headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            headers.put("qname-service", "976321:131072");
            headers.put("qname-space", "Production");
            String result = HttpUtils.post(url, body, headers);
            if (result == null) return "添加失败";
            JSONObject json = new JSONObject(result);
            int retcode = json.optInt("retcode", -1);
            if (retcode == 0) return "成功";
            if (retcode == 100106) return "添加失败，问答已存在";
            if (retcode == 100302) return "你不是管理";
            if (retcode == 1009) return "访问频率过快，稍后再试";
            return "添加失败，原因:" + json.optString("msg", "未知");
        } catch (Throwable e) {
            return "添加失败，原因:" + e.getMessage();
        }
    }

    private String guanjiaDeleteQna(String qun, long bkn, String cookie, String slot) {
        try {
            String url = "https://web.qun.qq.com/qunrobot/proxy/domain/app.qun.qq.com/cgi-bin/guanjia_robot/qna_setting/set_qna?bkn=" + bkn;
            String body = "{\"bkn\":" + bkn + ",\"group_id\":" + qun +
                ",\"qna_item\":{\"slot\":" + slot + ",\"question\":\"\",\"answer\":\"\",\"keyword\":[]}}";
            HashMap<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "application/json");
            headers.put("Cookie", cookie);
            headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            headers.put("qname-service", "976321:131072");
            headers.put("qname-space", "Production");
            String result = HttpUtils.post(url, body, headers);
            if (result == null) return "删除失败";
            JSONObject json = new JSONObject(result);
            int retcode = json.optInt("retcode", -1);
            if (retcode == 0) return "成功";
            return "删除失败";
        } catch (Throwable e) {
            return "删除失败";
        }
    }

    private String guanjiaTriggerQna(String qun, long bkn, String cookie, String question, String token) {
        try {
            String url = "https://app.qun.qq.com/cgi-bin/guanjia_robot/qna_callback/get_answer?bkn=" + bkn;
            String q = escapeJson(question);
            String t = escapeJson(token);
            String body = "{\"anonymous\":1,\"question\":\"" + q + "\",\"token\":\"" + t + "\"}";
            HashMap<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "application/json");
            headers.put("Cookie", cookie);
            headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            headers.put("qname-service", "976321:131072");
            headers.put("qname-space", "Production");
            String result = HttpUtils.post(url, body, headers);
            if (result == null) return "触发失败";
            JSONObject json = new JSONObject(result);
            int ec = json.optInt("ec", -1);
            if (ec == 0) return "成功";
            if (ec == 70000) return "会话过期";
            if (ec == 70003) return "访问频率过快，稍后再试";
            return "触发失败，原因:" + json.optString("em", "未知");
        } catch (Throwable e) {
            return "触发失败，原因:" + e.getMessage();
        }
    }

    private void sendMarkDown(String qun, String text, int mtype) {
        try {
            MsgTool.sendMarkDown(qun, text, mtype);
        } catch (Throwable e) {
            LogUtils.e(TAG, "sendMarkDown error: " + e.getMessage());
            MsgTool.sendMsg(qun, text, mtype);
        }
    }

}
