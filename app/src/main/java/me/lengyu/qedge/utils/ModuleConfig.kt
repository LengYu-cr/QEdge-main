package me.lengyu.qedge.utils

import org.json.JSONObject

object ModuleConfig {
    private const val CONFIG_NAME = "config"

    private val configDir: String
        get() = "${HostInfo.getModuleDataPath()}data/"

    private fun getConfig(): JSONObject {
        return try {
            JsonConfigUtils.getConfigMap(configDir, CONFIG_NAME).let { map ->
                JSONObject(map)
            }
        } catch (e: Exception) {
            JSONObject()
        }
    }

    fun putBoolean(key: String, value: Boolean) {
        JsonConfigUtils.putBoolean(configDir, CONFIG_NAME, key, value)
    }

    fun putString(key: String, value: String) {
        JsonConfigUtils.putString(configDir, CONFIG_NAME, key, value)
    }

    fun putInt(key: String, value: Int) {
        JsonConfigUtils.putInt(configDir, CONFIG_NAME, key, value)
    }

    fun putLong(key: String, value: Long) {
        JsonConfigUtils.putLong(configDir, CONFIG_NAME, key, value)
    }

    fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return JsonConfigUtils.getBoolean(configDir, CONFIG_NAME, key, defaultValue)
    }

    fun getString(key: String, defaultValue: String): String {
        return JsonConfigUtils.getString(configDir, CONFIG_NAME, key, defaultValue)
    }

    fun getInt(key: String, defaultValue: Int): Int {
        return JsonConfigUtils.getInt(configDir, CONFIG_NAME, key, defaultValue)
    }

    fun getLong(key: String, defaultValue: Long): Long {
        return JsonConfigUtils.getLong(configDir, CONFIG_NAME, key, defaultValue)
    }

    fun contains(key: String): Boolean {
        return JsonConfigUtils.contains(configDir, CONFIG_NAME, key)
    }

    fun remove(key: String) {
        JsonConfigUtils.remove(configDir, CONFIG_NAME, key)
    }
}
