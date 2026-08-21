package me.lengyu.qedge.hook.item

import android.view.View
import android.widget.ImageView
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import me.lengyu.qedge.R
import me.lengyu.qedge.hook.annotation.HookItemAnnotation
import me.lengyu.qedge.hook.base.BaseSwitchHookItem
import me.lengyu.qedge.utils.LogUtils
import me.lengyu.qedge.utils.ModuleConfig
import me.lengyu.qedge.utils.ReflectUtils
import me.lengyu.qedge.utils.Toasts
import me.lengyu.qedge.utils.qq.MsgTool
import me.lengyu.qedge.utils.qq.QQCurrentEnv
import com.tencent.qqnt.kernel.nativeinterface.MsgElement
import java.lang.reflect.Method

@HookItemAnnotation(value = "消息复读", category = "item", tag = "消息复读", desc = "在消息旁显示复读按钮，点击快速复读消息")
class RepeatMsg : BaseSwitchHookItem() {

    companion object {
        private const val TAG = "RepeatMsg"
        private const val SP_KEY = "repeat_msg"

        @Volatile
        private var cachedTargetMethod: Method? = null
        @Volatile
        private var hasSearched = false

        fun isEnabled(): Boolean {
            return ModuleConfig.getBoolean(SP_KEY, false)
        }

        @Synchronized
        private fun getTargetMethod(hostCL: ClassLoader): Method? {
            cachedTargetMethod?.let { return it }
            if (hasSearched) return null
            hasSearched = true

            return try {
                val componentClass = XposedHelpers.findClass(
                    "com.tencent.mobileqq.aio.msglist.holder.component.msgfollow.AIOMsgFollowComponent",
                    hostCL
                )
                val method = componentClass.declaredMethods.firstOrNull { m ->
                    m.parameterCount == 3 &&
                    m.returnType == Void.TYPE &&
                    m.parameterTypes[0] == Int::class.java &&
                    List::class.java.isAssignableFrom(m.parameterTypes[2])
                }?.also { it.isAccessible = true }

                cachedTargetMethod = method
                if (method == null) {
                    LogUtils.e(TAG, "L1 method not found")
                }
                method
            } catch (e: Exception) {
                LogUtils.e(TAG, "getTargetMethod failed: ${e.message}")
                null
            }
        }
    }

    private var lastClickTime = 0L

    override fun onInit(): Boolean {
        return true
    }

    override fun onHook() {
        val hostCL = ReflectUtils.hostClassLoader ?: return
        try {
            val targetMethod = getTargetMethod(hostCL)
            if (targetMethod == null) {
                LogUtils.e(TAG, "repeatMsg method not found")
                return
            }
            XposedBridge.hookMethod(targetMethod, object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    onHandleIntent(param)
                }
            })
        } catch (e: Exception) {
            LogUtils.e(TAG, "onHook failed: ${e.message}")
        }
    }

    private fun onHandleIntent(param: XC_MethodHook.MethodHookParam) {
        try {
            if (!isEnabled()) return

            val lazyField = param.thisObject.javaClass.declaredFields.find {
                it.type.name == "kotlin.Lazy"
            } ?: return
            lazyField.isAccessible = true
            val lazy = lazyField.get(param.thisObject) ?: return

            val repeatView = ReflectUtils.callMethod(lazy, "getValue") as? ImageView ?: return

            if (repeatView.context.javaClass.name.contains("MultiForwardActivity")) return

            val aioMsgItem = param.args.getOrNull(1) ?: return
            val msgRecord = ReflectUtils.callMethod(aioMsgItem, "getMsgRecord") ?: return

            repeatView.apply {
                visibility = View.VISIBLE
                setImageResource(R.drawable.repeat)
                setOnClickListener {
                    // LogUtils.d(TAG, "repeatView clicked")
                    if (isDoubleClick()) return@setOnClickListener
                    performRepeat(msgRecord)
                }
            }
        } catch (e: Exception) {
            LogUtils.e(TAG, "onHandleIntent: ${e.message}")
        }
    }

    private fun performRepeat(msgRecord: Any) {
        
        val msgType = ReflectUtils.getFieldValue(msgRecord, "msgType") as? Int
        if (msgType == null) {
            LogUtils.e(TAG, "performRepeat: msgType is null")
            return
        }

        val elements = ReflectUtils.getFieldValue(msgRecord, "elements") as? ArrayList<MsgElement>
        val refreshedElements = refreshMsgElements(msgType, elements)

        if (refreshedElements != null) {
            if (isNeedForward(msgType)) {
                forwardSend(msgRecord, refreshedElements)
            }else{
                directSend(msgRecord, refreshedElements)
            }
        }
    }

    private fun isNeedForward(msgType: Int): Boolean {
        return msgType == 7 || msgType == 19 || msgType == 10
    }

    private fun refreshMsgElements(msgType: Int, elements: ArrayList<MsgElement>?): ArrayList<MsgElement>? {
        if (elements.isNullOrEmpty()) return elements

        return when (msgType) {
            2 -> {
                elements.forEach { element ->
                    runCatching {
                        element.textElement?.let { it.atType = 0 }
                    }
                }
                elements
            }

            else -> elements
        }
    }

    private fun directSend(msgRecord: Any, elements: ArrayList<MsgElement>) {
        val chatType = ReflectUtils.getFieldValue(msgRecord, "chatType") as? Int ?: return
        val peerUid = ReflectUtils.getFieldValue(msgRecord, "peerUid") as? String ?: return
        val contact = MsgTool.makeContact(peerUid, chatType)
        MsgTool.sendMsgInternal(contact, elements)
    }

    private fun forwardSend(msgRecord: Any, elements: ArrayList<MsgElement>) {
        val chatType = ReflectUtils.getFieldValue(msgRecord, "chatType") as? Int ?: return
        val peerUid = ReflectUtils.getFieldValue(msgRecord, "peerUid") as? String ?: return
        val contact = MsgTool.makeContact(peerUid, chatType)
        MsgTool.forwardMsg(contact, elements)
    }

    private fun isDoubleClick(): Boolean {
        val now = System.currentTimeMillis()
        val time = now - lastClickTime
        lastClickTime = now
        return time < 500
    }
}