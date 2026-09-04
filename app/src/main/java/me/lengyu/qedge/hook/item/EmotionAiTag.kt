package me.lengyu.qedge.hook.item

import me.lengyu.qedge.hook.annotation.HookItemAnnotation
import me.lengyu.qedge.hook.api.OnSendMsg
import me.lengyu.qedge.hook.base.BaseSwitchHookItem
import me.lengyu.qedge.utils.LogUtils
import me.lengyu.qedge.utils.ModuleConfig
import me.lengyu.qedge.utils.ReflectUtils
/**
 * @Author 冷雨
 * @Description 发送表情包时带上"AI表情"标签：单图纯表情包(picType=1000)改为 2000 + picSubType=14
 */
@HookItemAnnotation(value = "把表情包发送带上AI表情标签", category = "item", tag = "AI表情标签", desc = "发送纯表情包时自动带上AI表情标签")
object EmotionAiTag : BaseSwitchHookItem() {

    private const val TAG = "EmotionAiTag"
    private const val KEY_ENABLE = "ai_emotion_tag"

    private fun isEnabled() = ModuleConfig.getBoolean(KEY_ENABLE, false)

    override fun onInit(): Boolean = true

    override fun onHook() {
        OnSendMsg.registerListener { _, elements ->
            if (!isEnabled()) return@registerListener
            applyAiTag(elements)
        }
    }

    private fun applyAiTag(elements: ArrayList<*>?) {
        // 仅当整条发送消息只有一个元素（纯图片/表情包，无文本等其他内容）时处理
        if (elements == null || elements.size != 1) return
        try {
            val element = elements[0] ?: return
            // 存在文本等内容则跳过
            var hasOtherContent = false
            val picElement = ReflectUtils.getFieldValue(element, "picElement")
            val textElement = ReflectUtils.getFieldValue(element, "textElement")
            if (picElement == null) return
            if (textElement != null) hasOtherContent = true
            if (hasOtherContent) return

            val picSubType = ReflectUtils.getFieldValue(picElement, "picSubType") as? Int ?: return
            if (picSubType != 1) return

            ReflectUtils.setFieldValue(picElement, "picSubType", 14)
            // LogUtils.i(TAG, "已为表情包设置AI表情标签")
        } catch (e: Throwable) {
            LogUtils.e(TAG, "applyAiTag error: ${e.message}")
        }
    }
}