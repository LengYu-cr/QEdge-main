package me.lengyu.qedge.hook.item

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import de.robv.android.xposed.XposedHelpers
import me.lengyu.qedge.hook.annotation.HookItemAnnotation
import me.lengyu.qedge.hook.base.BaseSwitchHookItem
import me.lengyu.qedge.utils.HookUtils
import me.lengyu.qedge.utils.LogUtils
import me.lengyu.qedge.utils.ModuleConfig
import me.lengyu.qedge.utils.ReflectUtils
import me.lengyu.qedge.utils.Toasts
/**
 * @Author 冷雨
 * @Description 复制卡片消息
 */
@HookItemAnnotation(value = "复制卡片消息", category = "item", tag = "复制卡片消息", desc = "卡片消息上方显示按钮，长按可复制卡片JSON数据")
class CopyArkMessage : BaseSwitchHookItem() {

    companion object {
        private const val TAG = "CopyArkMessage"
        private const val SP_KEY = "copy_ark_message"
        private const val COPY_BUTTON_ID = 0x293284

        fun isEnabled(): Boolean {
            return ModuleConfig.getBoolean(SP_KEY, false)
        }
    }

    private var aioViewHookDone = false

    override fun onInit(): Boolean = true

    override fun onHook() {
        hookAIOMsgUpdate()
    }

    private fun hookAIOMsgUpdate() {
        if (aioViewHookDone) return
        val hostCL = ReflectUtils.hostClassLoader ?: return
        try {
            val vbClass = XposedHelpers.findClass(
                "com.tencent.mobileqq.aio.msglist.holder.AIOBubbleMsgItemVB",
                hostCL
            )
            val mviUiStateClass = XposedHelpers.findClass(
                "com.tencent.mvi.base.mvi.MviUIState",
                hostCL
            )
            HookUtils.hookClassMethod(
                vbClass, "handleUIState", arrayOf(mviUiStateClass),
                null
            ) { param -> onMsgItemUpdate(param) }
            aioViewHookDone = true
        } catch (e: Exception) {
            LogUtils.e(TAG, "hookAIOMsgUpdate failed: ${e.message}")
        }
    }

    private fun onMsgItemUpdate(param: de.robv.android.xposed.XC_MethodHook.MethodHookParam) {
        try {
            if (!isEnabled()) return

            val thisObject = param.thisObject

            // 获取消息视图
            val itemView = getVBView(thisObject) ?: return
            val rootView = itemView as? ViewGroup ?: return

            // 获取 AIOMsgItem
            val aioMsgItem = findFirstFieldOfType(
                thisObject,
                "com.tencent.mobileqq.aio.msg.AIOMsgItem"
            ) ?: return

            // 获取 msgRecord
            val msgRecord = ReflectUtils.callMethod(aioMsgItem, "getMsgRecord") ?: return

            // 获取 elements 列表
            @Suppress("UNCHECKED_CAST")
            val elements = ReflectUtils.getFieldValue(msgRecord, "elements") as? List<Any> ?: return

            // 查找 ArkElement
            var cardData: String? = null
            for (element in elements) {
                val arkElement = try {
                    ReflectUtils.callMethod(element, "getArkElement")
                } catch (_: Exception) { null }
                if (arkElement != null) {
                    cardData = try {
                        ReflectUtils.getFieldValue(arkElement, "bytesData") as? String
                    } catch (_: Exception) { null }
                    if (cardData != null) break
                }
            }

            val existingCopyBtn = rootView.findViewById<View?>(COPY_BUTTON_ID)
            if (cardData == null) {
                if (existingCopyBtn != null) rootView.removeView(existingCopyBtn)
                return
            }

            if (existingCopyBtn != null) return

            addCopyButton(rootView, cardData)
        } catch (e: Exception) {
            LogUtils.e(TAG, "onMsgItemUpdate: ${e.message}")
        }
    }

    private fun getVBView(thisObject: Any): View? {
        try {
            val field = thisObject.javaClass.getDeclaredField("e")
            field.isAccessible = true
            val view = field.get(thisObject) as? View
            if (view != null) return view
        } catch (_: Exception) {}
        try {
            return ReflectUtils.callMethod(thisObject, "getHostView") as? View
        } catch (_: Exception) {}
        return null
    }

    private fun addCopyButton(rootView: ViewGroup, cardData: String) {
        val context = rootView.context
        val textView = TextView(context).apply {
            text = "长按复制卡片"
            textSize = 13f
            setTextColor(0xFF69F0AE.toInt())
            gravity = Gravity.CENTER_HORIZONTAL
            id = COPY_BUTTON_ID
            setOnLongClickListener {
                copyToClipboard(context, cardData)
                Toasts.showToast("已复制卡片JSON数据")
                true
            }
        }
        rootView.addView(textView, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ))
    }

    private fun copyToClipboard(context: Context, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        if (clipboard != null) {
            val clip = ClipData.newPlainText("ark_card", text)
            clipboard.setPrimaryClip(clip)
        }
    }

    private fun findFirstFieldOfType(obj: Any, typeName: String): Any? {
        try {
            val hostCL = ReflectUtils.hostClassLoader ?: return null
            val targetType = XposedHelpers.findClass(typeName, hostCL)
            var clazz: Class<*>? = obj.javaClass
            while (clazz != null) {
                for (field in clazz.declaredFields) {
                    if (targetType.isAssignableFrom(field.type)) {
                        field.isAccessible = true
                        return field.get(obj)
                    }
                }
                clazz = clazz.superclass
            }
        } catch (_: Exception) {}
        return null
    }
}
