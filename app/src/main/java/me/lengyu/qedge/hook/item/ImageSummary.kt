package me.lengyu.qedge.hook.item

import me.lengyu.qedge.hook.annotation.HookItemAnnotation
import me.lengyu.qedge.hook.api.OnSendMsg
import me.lengyu.qedge.hook.base.BaseSwitchHookItem
import me.lengyu.qedge.utils.HttpUtils
import me.lengyu.qedge.utils.LogUtils
import me.lengyu.qedge.utils.ModuleConfig
import me.lengyu.qedge.utils.ReflectUtils
/**
 * @Author 冷雨
 * @Description 图片外显自定义（模块级）：发送图片时将外显摘要改为随机文案或 HTTP 接口返回内容
 */
@HookItemAnnotation(value = "图片外显", category = "item", tag = "图片外显", desc = "发送图片时自定义外显摘要：随机文案或接口返回")
object ImageSummary : BaseSwitchHookItem() {

    private const val TAG = "ImageSummary"

    private const val KEY_ENABLE = "image_summary"
    private const val KEY_MODE = "image_summary_mode"   // text / http
    private const val KEY_TIPS = "image_summary_tips"   // 多条按逗号分隔
    private const val KEY_URL = "image_summary_url"     // HTTP 接口地址

    /** 模块该功能是否开启（供 PluginCallback 判断脚本是否让位） */
    @JvmStatic
    fun isModuleEnabled(): Boolean {
        return ModuleConfig.getBoolean(KEY_ENABLE, false)
    }

    override fun onInit(): Boolean = true

    override fun onHook() {
        // 无论开关状态都注册监听，运行时以 isModuleEnabled() 为准，便于切换
        OnSendMsg.registerListener { _, elements ->
            if (!isModuleEnabled()) return@registerListener
            applySummary(elements)
        }
    }

    private fun applySummary(elements: ArrayList<*>?) {
        if (elements.isNullOrEmpty()) return
        try {
            for (element in elements) {
                if (element == null) continue
                val picElement = ReflectUtils.getFieldValue(element, "picElement") ?: continue
                var summary: String? = when (ModuleConfig.getString(KEY_MODE, "")) {
                    "http" -> fetchFromHttp()
                    else -> pickRandomTip()
                }
                if (summary.isNullOrEmpty()) continue
                summary = summary.trim()
                // 去掉首尾引号，避免接口返回带引号的 JSON 字符串直接作为外显
                if (summary.length >= 2 &&
                    ((summary.startsWith("\"") && summary.endsWith("\"")) ||
                        (summary.startsWith("'") && summary.endsWith("'")))
                ) {
                    summary = summary.substring(1, summary.length - 1)
                }
                ReflectUtils.setFieldValue(picElement, "summary", summary)
            }
        } catch (e: Throwable) {
            LogUtils.e(TAG, "applySummary error: ${e.message}")
        }
    }

    /** 多条文案轮询：逗号分割后随机取一条 */
    private fun pickRandomTip(): String? {
        val tips = ModuleConfig.getString(KEY_TIPS, "")
        val list = tips.split(',', '，', '、').map { it.trim() }.filter { it.isNotEmpty() }
        if (list.isEmpty()) return null
        return list.random()
    }

    /** HTTP 接口：把返回内容作为外显 */
    private fun fetchFromHttp(): String? {
        val url = ModuleConfig.getString(KEY_URL, "").trim()
        if (url.isEmpty()) return null
        return try {
            HttpUtils.get(url, 5000, 5000)
        } catch (e: Throwable) {
            LogUtils.e(TAG, "fetchFromHttp error: ${e.message}")
            null
        }
    }
}