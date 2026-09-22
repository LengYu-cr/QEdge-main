package me.lengyu.qedge.utils

/**
 * @Author 冷雨
 * @Description 模块配置工具类
 *
 * 开关(boolean)与数据(string/int/long)分文件存储，避免单文件损坏时互相影响：
 *  - config.dat   开关配置
 *  - userdata.dat 数据配置
 */
object ModuleConfig {
    private const val SWITCH_CONFIG = "config"
    private const val DATA_CONFIG = "userdata"

    private val configDir: String
        get() = "${HostInfo.getModuleDataPath()}data/"

    // ---- 开关配置 ----

    @JvmStatic
    fun putBoolean(key: String, value: Boolean) {
        JsonConfigUtils.putBoolean(configDir, SWITCH_CONFIG, key, value)
    }

    @JvmStatic
    fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return JsonConfigUtils.getBoolean(configDir, SWITCH_CONFIG, key, defaultValue)
    }

    // ---- 数据配置 ----

    @JvmStatic
    fun putString(key: String, value: String) {
        JsonConfigUtils.putString(configDir, DATA_CONFIG, key, value)
    }

    @JvmStatic
    fun putInt(key: String, value: Int) {
        JsonConfigUtils.putInt(configDir, DATA_CONFIG, key, value)
    }

    @JvmStatic
    fun putLong(key: String, value: Long) {
        JsonConfigUtils.putLong(configDir, DATA_CONFIG, key, value)
    }

    @JvmStatic
    fun getString(key: String, defaultValue: String): String {
        return JsonConfigUtils.getString(configDir, DATA_CONFIG, key, defaultValue)
    }

    @JvmStatic
    fun getInt(key: String, defaultValue: Int): Int {
        return JsonConfigUtils.getInt(configDir, DATA_CONFIG, key, defaultValue)
    }

    @JvmStatic
    fun getLong(key: String, defaultValue: Long): Long {
        return JsonConfigUtils.getLong(configDir, DATA_CONFIG, key, defaultValue)
    }

    // ---- 通用：两个文件一起处理，保证语义正确 ----

    @JvmStatic
    fun contains(key: String): Boolean {
        return JsonConfigUtils.contains(configDir, SWITCH_CONFIG, key) ||
                JsonConfigUtils.contains(configDir, DATA_CONFIG, key)
    }

    @JvmStatic
    fun remove(key: String) {
        JsonConfigUtils.remove(configDir, SWITCH_CONFIG, key)
        JsonConfigUtils.remove(configDir, DATA_CONFIG, key)
    }
}