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
import me.lengyu.qedge.utils.reflect.ClassUtils
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
        private const val AIO_MSG_ITEM_CLASS = "com.tencent.mobileqq.aio.msg.AIOMsgItem"

        /** 按 VB 的 Class 缓存 AIOMsgItem 字段（视图复用同一 Class，命中后 O(1)） */
        private val aioMsgItemFieldCache = java.util.concurrent.ConcurrentHashMap<Class<*>, Any>()
        private val FIELD_NOT_FOUND = Any()

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
            val aioMsgItem = findAioMsgItem(thisObject) ?: return

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

            // 用 View tag 记录按钮，避免每次 findViewById 走一遍子树
            val existingCopyBtn = rootView.getTag(COPY_BUTTON_ID) as? View
            if (cardData == null) {
                if (existingCopyBtn != null) {
                    rootView.removeView(existingCopyBtn)
                    rootView.setTag(COPY_BUTTON_ID, null)
                }
                return
            }

            if (existingCopyBtn != null) return

            addCopyButton(rootView, cardData)
        } catch (e: Exception) {
            LogUtils.e(TAG, "onMsgItemUpdate: ${e.message}")
        }
    }

    private fun getVBView(thisObject: Any): View? {
        // 字段查找走 ClassUtils 的缓存，命中后不再反射扫描
        try {
            val field = ClassUtils.findFieldOrNull(thisObject.javaClass, "e")
            if (field != null) {
                val view = field.get(thisObject) as? View
                if (view != null) return view
            }
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
        rootView.setTag(COPY_BUTTON_ID, textView)
    }

    private fun copyToClipboard(context: Context, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        if (clipboard != null) {
            val clip = ClipData.newPlainText("ark_card", text)
            clipboard.setPrimaryClip(clip)
        }
    }

    /** 按 Class 缓存 AIOMsgItem 字段，避免每次气泡更新都全字段扫描 */
    private fun findAioMsgItem(obj: Any): Any? {
        val clazz = obj.javaClass
        val cached = aioMsgItemFieldCache[clazz]
        val field: java.lang.reflect.Field = when {
            cached === FIELD_NOT_FOUND -> return null
            cached != null -> cached as java.lang.reflect.Field
            else -> {
                val found = findFirstFieldOfType(clazz, AIO_MSG_ITEM_CLASS)
                aioMsgItemFieldCache.putIfAbsent(clazz, found ?: FIELD_NOT_FOUND)
                found
            }
        } ?: return null
        return try {
            field.get(obj)
        } catch (_: Exception) {
            null
        }
    }

    private fun findFirstFieldOfType(clazz: Class<*>, typeName: String): java.lang.reflect.Field? {
        try {
            val hostCL = ReflectUtils.hostClassLoader ?: return null
            val targetType = XposedHelpers.findClass(typeName, hostCL)
            var current: Class<*>? = clazz
            while (current != null) {
                for (field in current.declaredFields) {
                    if (targetType.isAssignableFrom(field.type)) {
                        field.isAccessible = true
                        return field
                    }
                }
                current = current.superclass
            }
        } catch (_: Exception) {}
        return null
    }
}
