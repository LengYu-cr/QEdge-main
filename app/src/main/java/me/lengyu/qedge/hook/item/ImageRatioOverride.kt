package me.lengyu.qedge.hook.item

import me.lengyu.qedge.hook.annotation.HookItemAnnotation
import me.lengyu.qedge.hook.api.OnSendMsg
import me.lengyu.qedge.hook.base.BaseSwitchHookItem
import me.lengyu.qedge.utils.LogUtils
import me.lengyu.qedge.utils.ModuleConfig
import me.lengyu.qedge.utils.ReflectUtils
/**
 * @Author 冷雨
 * @Description 篡改发送图片比例：对所有发送的图片元素(picElement)强制设置 picWidth/picHeight
 */
@HookItemAnnotation(value = "篡改发送图片比例", category = "item", tag = "篡改发送图片比例", desc = "所有发送图片按自定义宽高(整数)重设比例")
object ImageRatioOverride : BaseSwitchHookItem() {

    private const val TAG = "ImageRatioOverride"

    private const val KEY_ENABLE = "image_ratio"
    private const val KEY_WIDTH = "image_ratio_width"
    private const val KEY_HEIGHT = "image_ratio_height"

    private fun isEnabled() = ModuleConfig.getBoolean(KEY_ENABLE, false)

    override fun onInit(): Boolean = true

    override fun onHook() {
        OnSendMsg.registerListener { _, elements ->
            if (!isEnabled()) return@registerListener
            applyRatio(elements)
        }
    }

    private fun applyRatio(elements: ArrayList<*>?) {
        if (elements.isNullOrEmpty()) return
        val width = ModuleConfig.getInt(KEY_WIDTH, 0)
        val height = ModuleConfig.getInt(KEY_HEIGHT, 0)
        if (width <= 0 || height <= 0) return
        try {
            for (element in elements) {
                if (element == null) continue
                val picElement = ReflectUtils.getFieldValue(element, "picElement") ?: continue
                ReflectUtils.setFieldValue(picElement, "picWidth", width)
                ReflectUtils.setFieldValue(picElement, "picHeight", height)
            }
        } catch (e: Throwable) {
            LogUtils.e(TAG, "applyRatio error: ${e.message}")
        }
    }
}