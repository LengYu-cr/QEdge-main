package me.lengyu.qedge.coldrain;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

import com.tencent.qqnt.kernel.nativeinterface.MsgElement;
import com.tencent.qqnt.kernelpublic.nativeinterface.Contact;

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
import me.lengyu.qedge.utils.QQCurrentEnv;
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
        String basePath = Environment.getExternalStorageDirectory().getPath() 
            + "/Android/media/" + context.getPackageName() + "/冷雨Java";
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

            if (text.equals("开机")) {
                if (!isAdminOrSelf(msgData)) {
                    return;
                }
                if (msgData.type == 2) {
                    String groupUin = msgData.peerUin;
                    setBoolean("group_master_enabled_" + groupUin, true);
                    reply(msgData, "本群已开机！\n发送【" + getMenuName() + "】查看菜单");
                } else {
                    if (!isMasterEnabled()) {
                        setBoolean("master_enabled", true);
                        reply(msgData, "冷雨Java 已启动！\n发送【" + getMenuName() + "】查看菜单");
                    } else {
                        reply(msgData, "冷雨Java 已经在运行中啦~");
                    }
                }
                return;
            }

            if (text.equals("关机")) {
                if (!isAdminOrSelf(msgData)) {
                    return;
                }
                if (msgData.type == 2) {
                    String groupUin = msgData.peerUin;
                    setBoolean("group_master_enabled_" + groupUin, false);
                    reply(msgData, "本群已关机");
                } else {
                    setBoolean("master_enabled", false);
                    reply(msgData, "冷雨Java 已关闭");
                }
                return;
            }

            if (msgData.type == 2 && isAdminOrSelf(msgData)) {
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
                boolean isGroupOn = msgData.type == 2 ? getBoolean("group_master_enabled_" + msgData.peerUin, false) : true;
                
                if (!isMasterOn || !isGroupOn) {
                    if (!isAdminOrSelf(msgData)) {
                        return;
                    }
                    if (!isMasterOn) {
                        return;
                    }
                    if (msgData.type == 2 && !isGroupOn) {
                        reply(msgData, "本群未开机");
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

        // 菜单命令不受功能开关和群功能开关限制
        if (isMenuCommand(msgData.msg)) {
            if (msgData.type == 2 && msgData.peerUin != null && !msgData.peerUin.isEmpty()) {
                return getBoolean("group_master_enabled_" + msgData.peerUin, false);
            }
            return true;
        }
        
        if (!isFeatureEnabled(featureKey) && !featureKey.equals("feature_status")) {
            return false;
        }
        if (isPersonalFeature(featureKey)) {
            return true;
        }
        if (msgData.type == 2 && msgData.peerUin != null && !msgData.peerUin.isEmpty()) {
            if (!getBoolean("group_master_enabled_" + msgData.peerUin, false)) {
                return false;
            }
            return isGroupFeatureEnabled(featureKey, msgData.peerUin);
        }
        return true;
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
        String groupUin = msgData.peerUin;
        setGroupFeatureEnabled(featureKey, groupUin, enable);
        String featureName = "";
        for (String[] mapping : FEATURE_NAME_MAP) {
            if (mapping[1].equals(featureKey)) {
                featureName = mapping[0];
                break;
            }
        }
        reply(msgData, (enable ? "已开启" : "已关闭") + "本群" + featureName);
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
            return me.lengyu.qedge.utils.QQCurrentEnv.getCurrentUin();
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
        }
    }

    private void sendGuanjiaReply(String qun, String text, int mtype) {
        try {
            if (mtype != 2) {
                MsgTool.sendMsg(qun, text, mtype);
                return;
            }
            String pskey = CookieTool.getPskey("qun.qq.com");
            String skey = CookieTool.getSkey();
            if (pskey == null || skey == null) {
                MsgTool.sendMsg(qun, text, mtype);
                return;
            }
            String myUin = QQCurrentEnv.getCurrentUin();
            String content = "\n" + text.replace("$", "").replace("&lt;", "<").replace("&gt;", ">");
            if (content.length() > 20000 || content.contains(".cn") || content.contains(".net") ||
                content.contains(".vip") || content.contains(".com") || content.contains(".中国") ||
                content.contains(".edu") || content.contains(".tv") || content.contains("[pic=")) {
                MsgTool.sendMsg(qun, text, mtype);
                return;
            }
            String url = "https://qun.qq.com/cgi-bin/qun_mgr/send_group_msg";
            String data = "groupid=" + qun + "&msgtype=0&msg=" +
                java.net.URLEncoder.encode(content.replaceAll("\\r\\n|\\n|\\r", "\\\\n").replace("\"", "\\\""), "UTF-8");
            String cookie = "p_uin=o0" + myUin + ";skey=" + skey + ";p_skey=" + pskey;
            String result = HttpUtils.postWithCookie(url, data, cookie);
            if (result == null || !result.contains("ok")) {
                MsgTool.sendMsg(qun, text, mtype);
            }
        } catch (Throwable e) {
            LogUtils.e(TAG, "sendGuanjiaReply error: " + e.getMessage());
            MsgTool.sendMsg(qun, text, mtype);
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
