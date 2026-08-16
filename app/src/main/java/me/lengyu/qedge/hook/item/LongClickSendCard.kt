package me.lengyu.qedge.hook.item

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import me.lengyu.qedge.hook.annotation.HookItemAnnotation
import me.lengyu.qedge.hook.base.BaseSwitchHookItem
import me.lengyu.qedge.plugin.view.ChatSettingLoader
import me.lengyu.qedge.utils.LogUtils
import me.lengyu.qedge.utils.ModuleConfig
import me.lengyu.qedge.utils.QQCurrentEnv
import me.lengyu.qedge.utils.ReflectUtils
import me.lengyu.qedge.utils.Toasts
import me.lengyu.qedge.utils.qq.MsgTool
import org.json.JSONObject

@HookItemAnnotation(value = "长按发送按钮发卡片", category = "item", tag = "长按发送按钮发卡片", desc = "长按发送按钮将输入框内的JSON作为卡片消息发送")
class LongClickSendCard : BaseSwitchHookItem() {

    companion object {
        private const val TAG = "LongClickSendCard"
        private const val SP_KEY = "long_click_send_card"
        private const val LONG_CLICK_TAG = "qedge_send_card_tag"

        fun isEnabled(): Boolean {
            return ModuleConfig.getBoolean(SP_KEY, false)
        }
    }

    private var hookDone = false

    override fun onInit(): Boolean = true

    override fun onHook() {
        if (hookDone) return
        hookSendButton()
        hookDone = true
    }

    private fun hookSendButton() {
        val hostCL = ReflectUtils.hostClassLoader ?: return

        // 策略1: Hook AIODelegate.show 监听聊天页面打开
        hookAIOShow(hostCL)

        // 策略2: Hook 各种输入区 ViewBinder 的 bindViewAndData 方法
        hookInputBinders(hostCL)
    }

    private fun hookAIOShow(cl: ClassLoader) {
        try {
            val aioDelegateClass = XposedHelpers.findClass("com.tencent.qqnt.aio.activity.AIODelegate", cl)
            XposedHelpers.findAndHookMethod(aioDelegateClass, "show", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    if (!isEnabled()) return
                    val activity = QQCurrentEnv.getActivity() ?: return
                    scanAndSetup(activity)
                }
            })
        } catch (e: Exception) {
            LogUtils.e(TAG, "hook AIODelegate.show failed: ${e.message}")
        }
    }

    private fun hookInputBinders(cl: ClassLoader) {
        val candidates = listOf(
            "com.tencent.mobileqq.aio.input.AIODefaultInputViewBinder",
            "com.tencent.mobileqq.aio.input.sendmsg.AIOSendMsgVBDelegate",
            "com.tencent.guild.aio.input.realinput.GuildAioDefaultInputViewBinder"
        )
        for (clsName in candidates) {
            try {
                val cls = XposedHelpers.findClassIfExists(clsName, cl) ?: continue
                for (m in cls.declaredMethods) {
                    if (m.name != "bindViewAndData") continue
                    XposedBridge.hookMethod(m, object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            if (!isEnabled()) return
                            try {
                                setupFromThisObject(param.thisObject)
                            } catch (e: Exception) {
                                LogUtils.e(TAG, "binder hook error: ${e.message}")
                            }
                        }
                    })
                }
            } catch (e: Exception) {
                LogUtils.e(TAG, "hook $clsName failed: ${e.message}")
            }
        }
    }

    private fun scanAndSetup(activity: Activity) {
        val decorView = activity.window.decorView as? ViewGroup ?: return
        // 多次重试，等待视图加载完成
        val delays = longArrayOf(300, 800, 1500, 2500)
        for (delay in delays) {
            decorView.postDelayed({
                try {
                    val root = activity.window?.decorView as? ViewGroup ?: return@postDelayed
                    val pair = findInputAndSendButton(root) ?: return@postDelayed
                    setupLongClick(pair.second, pair.first)
                } catch (_: Exception) {}
            }, delay)
        }
    }

    private fun setupFromThisObject(obj: Any) {
        var editText: EditText? = null
        var sendBtn: View? = null

        // 尝试固定字段路径 (e.e = EditText, e.l = send button)
        try {
            val e = ReflectUtils.getFieldValue(obj, "e")
            if (e != null) {
                editText = ReflectUtils.getFieldValue(e, "e") as? EditText
                // 尝试多个可能的按钮字段名
                for (fieldName in arrayOf("l", "f", "g", "h", "i", "j", "k", "m", "n")) {
                    val candidate = ReflectUtils.getFieldValue(e, fieldName) as? View
                    if (candidate != null && candidate !== editText && candidate.isClickable) {
                        sendBtn = candidate
                        break
                    }
                }
            }
        } catch (_: Exception) {}

        // 遍历字段找
        if (editText == null || sendBtn == null) {
            var cls: Class<*>? = obj.javaClass
            while (cls != null) {
                for (f in cls.declaredFields) {
                    if (f.type.isPrimitive) continue
                    f.isAccessible = true
                    val v = try { f.get(obj) } catch (_: Exception) { null } ?: continue
                    if (v is EditText && editText == null) editText = v
                    if (v is View && v !== editText && sendBtn == null && v.isClickable && v !is EditText) {
                        sendBtn = v
                    }
                }
                cls = cls.superclass
            }
        }

        // 从 root view 查找
        if (editText == null || sendBtn == null) {
            try {
                val root = ReflectUtils.callMethod(obj, "getRoot") as? ViewGroup
                    ?: ReflectUtils.callMethod(obj, "getView") as? ViewGroup
                    ?: ReflectUtils.callMethod(obj, "getHostView") as? ViewGroup
                if (root != null) {
                    val pair = findInputAndSendButton(root)
                    if (pair != null) {
                        editText = pair.first
                        sendBtn = pair.second
                    }
                }
            } catch (_: Exception) {}
        }

        if (editText != null && sendBtn != null) {
            setupLongClick(sendBtn!!, editText!!)
        }
    }

    private fun findInputAndSendButton(root: ViewGroup): Pair<EditText, View>? {
        // 找到所有可见的 EditText
        val editTexts = mutableListOf<EditText>()
        collectEditTexts(root, editTexts)

        // 优先选择在屏幕底部的 EditText（聊天输入框在底部）
        val editText = editTexts.firstOrNull() ?: return null
        val sendBtn = findSendButtonNearEditText(root, editText) ?: return null
        return Pair(editText, sendBtn)
    }

    private fun collectEditTexts(root: ViewGroup, out: MutableList<EditText>) {
        for (i in 0 until root.childCount) {
            val child = root.getChildAt(i)
            if (child is EditText && child.isShown && child.isEnabled && child.height > 0) {
                out.add(child)
            }
            if (child is ViewGroup) {
                collectEditTexts(child, out)
            }
        }
    }

    private fun findSendButtonNearEditText(root: ViewGroup, editText: EditText): View? {
        // 从EditText向上找最近的clickable非EditText View
        var parent = editText.parent as? ViewGroup ?: return null

        // 同级中找可点击View
        for (depth in 0 until 4) {
            for (i in 0 until parent.childCount) {
                val sibling = parent.getChildAt(i)
                if (sibling !== editText && sibling is View && sibling.isClickable
                    && sibling.isShown && sibling !is EditText) {
                    return sibling
                }
            }
            parent = parent.parent as? ViewGroup ?: return null
        }
        return null
    }

    private fun setupLongClick(sendBtn: View, editText: EditText) {
        if (sendBtn.getTag(LONG_CLICK_TAG.hashCode()) != null) return
        sendBtn.setTag(LONG_CLICK_TAG.hashCode(), true)

        sendBtn.setOnLongClickListener { _ ->
            try {
                val text = editText.text?.toString()?.trim() ?: ""
                if (text.isEmpty()) return@setOnLongClickListener false
                if (!isJson(text)) {
                    Toasts.showToast("卡片格式错误，请输入有效JSON")
                    return@setOnLongClickListener false
                }
                val contact = ChatSettingLoader.currentContact
                if (contact.peerUin.isEmpty() || contact.chatType == 0) {
                    Toasts.showToast("无法获取当前会话信息")
                    return@setOnLongClickListener false
                }
                MsgTool.sendCard(contact.peerUin, text, contact.chatType)
                editText.setText("")
                Toasts.showToast("卡片已发送")
                return@setOnLongClickListener true
            } catch (e: Exception) {
                LogUtils.e(TAG, "send card error: ${e.message}")
                Toasts.showToast("发送失败")
            }
            false
        }
    }

    private fun isJson(text: String): Boolean {
        return try {
            JSONObject(text.trim())
            true
        } catch (_: Exception) {
            false
        }
    }
}
