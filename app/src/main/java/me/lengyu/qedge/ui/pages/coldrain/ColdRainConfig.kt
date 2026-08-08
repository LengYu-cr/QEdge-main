package me.lengyu.qedge.ui.pages.coldrain

import android.content.Context
import java.io.File
import me.lengyu.qedge.utils.QQCurrentEnv

object ColdRainConfig {
    private const val TAG = "ColdRainConfig"

    private var isInitialized = false

    fun getLocalPath(): String {
        return QQCurrentEnv.getLocalPath()
    }

    fun init(context: Context): Boolean {
        if (isInitialized) {
            return true
        }
        try {
            me.lengyu.qedge.coldrain.ColdRainCore.getInstance().init(context)
            isInitialized = true
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    fun getBoolean(key: String, default: Boolean = false): Boolean {
        return me.lengyu.qedge.coldrain.ColdRainCore.getInstance().getConfigBoolean(key, default)
    }

    fun setBoolean(key: String, value: Boolean) {
        me.lengyu.qedge.coldrain.ColdRainCore.getInstance().setConfigBoolean(key, value)
    }

    fun getString(key: String, default: String = ""): String {
        return me.lengyu.qedge.coldrain.ColdRainCore.getInstance().getConfigString(key, default)
    }

    fun setString(key: String, value: String) {
        me.lengyu.qedge.coldrain.ColdRainCore.getInstance().setConfigString(key, value)
    }

    fun getInt(key: String, default: Int = 0): Int {
        return me.lengyu.qedge.coldrain.ColdRainCore.getInstance().getConfigInt(key, default)
    }

    fun setInt(key: String, value: Int) {
        me.lengyu.qedge.coldrain.ColdRainCore.getInstance().setConfigInt(key, value)
    }

    val isMasterEnabled: Boolean
        get() = getBoolean("master_enabled", false)

    val menuName: String
        get() = getString("menu_name", "菜单")

    val replyMode: String
        get() = getString("reply_mode", "text")

    val menuRestricted: Boolean
        get() = getBoolean("menu_restricted", false)

    val masterUin: String
        get() = getString("master_uin", "")

    val globalAdmins: String
        get() = getString("global_admins", "")

    val welcomeJoinMsg: String
        get() = getString("welcome_join_msg", "欢迎 {qq} 加入本群！")

    val welcomeQuitMsg: String
        get() = getString("welcome_quit_msg", "{qq} 已退出本群")

    val hourlyCustom: String
        get() = getString("hourly_custom", "")

    data class FeatureItem(
        val key: String,
        val name: String,
        val description: String,
        val category: String,
        val defaultEnabled: Boolean = true
    )

    val allFeatures = listOf(
        FeatureItem("feature_group_manager", "群管菜单", "踢人、禁言、群管理功能", "群管理"),
        FeatureItem("feature_welcome_join", "进群欢迎", "新成员进群自动欢迎", "群管理"),
        FeatureItem("feature_welcome_quit", "退群提示", "成员退群自动提示", "群管理"),
        FeatureItem("feature_ban", "违禁系统", "违禁词检测、提醒", "安全"),
        FeatureItem("feature_black_white_list", "黑白名单", "黑白名单管理、自动踢人", "安全"),
        FeatureItem("feature_signin", "签到系统", "每日签到、积分统计", "娱乐"),
        FeatureItem("feature_question", "问答功能", "自定义问答触发", "娱乐"),
        FeatureItem("feature_query", "查询系统", "账号信息等查询", "工具"),
        FeatureItem("feature_weather", "天气系统", "天气预报查询", "工具"),
        FeatureItem("feature_hourly", "整点报时", "群内整点报时", "工具"),
        FeatureItem("feature_video_parse", "视频解析", "短视频平台无水印解析", "工具"),
        FeatureItem("feature_image_menu", "图片菜单", "随机图片、表情、壁纸", "娱乐"),
        FeatureItem("feature_video_menu", "视频菜单", "抖音快手B站搜索、随机视频", "娱乐"),
        FeatureItem("feature_music", "音乐菜单", "音乐搜索和点歌功能", "娱乐"),
        FeatureItem("feature_image", "图片功能", "头像处理：阴影头像、羽化头像等", "工具"),
        FeatureItem("feature_avatar_menu", "头像菜单", "上传头像、上传封面", "工具"),
        FeatureItem("feature_autoadmin", "自助上管", "自助申请管理员", "高级"),
        FeatureItem("feature_title", "头衔功能", "专属头衔管理", "高级"),
        FeatureItem("feature_like", "赞我点赞", "互赞、名片赞", "高级"),
        FeatureItem("feature_at", "艾特处理", "艾特回复、禁言、提醒", "高级")
    )

    fun isFeatureEnabled(key: String): Boolean {
        val feature = allFeatures.find { it.key == key }
        return getBoolean(key, feature?.defaultEnabled ?: false)
    }

    fun setFeatureEnabled(key: String, enabled: Boolean) {
        setBoolean(key, enabled)
    }

    val categories = listOf(
        "群管理", "娱乐", "经济", "安全", "工具", "高级"
    )

    private val personalFeatures = setOf(
        "feature_status",
        "feature_group_manager"
    )

    fun isPersonalFeature(key: String): Boolean {
        return personalFeatures.contains(key)
    }

    fun getConfigDir(): File? {
        return File(getLocalPath(), "Android/media/me.lengyu.qedge/冷雨Java")
    }

    fun isInitialized(): Boolean {
        return isInitialized
    }
}
