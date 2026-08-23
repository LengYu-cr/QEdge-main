package me.lengyu.qedge.hook.item

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.View
import android.widget.ImageView
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import me.lengyu.qedge.R
import me.lengyu.qedge.hook.annotation.HookItemAnnotation
import me.lengyu.qedge.hook.base.BaseSwitchHookItem
import me.lengyu.qedge.lifecycle.Parasitics
import me.lengyu.qedge.ui.components.dialogs.RepeatMsgAction
import me.lengyu.qedge.ui.components.dialogs.RepeatMsgActionDialog
import me.lengyu.qedge.ui.components.dialogs.RawTextDialog
import me.lengyu.qedge.utils.LogUtils
import me.lengyu.qedge.utils.ModuleConfig
import me.lengyu.qedge.utils.ReflectUtils
import me.lengyu.qedge.utils.Toasts
import me.lengyu.qedge.plugin.bean.MsgData
import me.lengyu.qedge.utils.qq.MsgTool
import me.lengyu.qedge.utils.qq.QQCurrentEnv
import com.tencent.qqnt.kernel.nativeinterface.MsgElement
import com.tencent.qqnt.kernel.nativeinterface.MsgRecord
import java.lang.reflect.Method
/**
 * @Author 冷雨
 * @Description 消息复读
 */
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
                    LogUtils.e(TAG, "repeatMsgComponent method not found")
                }
                method
            } catch (e: Exception) {
                LogUtils.e(TAG, "getTargetMethod repeatMsgComponent failed: ${e.message}")
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
            val msgRecord = ReflectUtils.callMethod(aioMsgItem, "getMsgRecord") as? MsgRecord ?: return

            repeatView.apply {
                visibility = View.VISIBLE
                setImageResource(R.drawable.repeat)
                setOnClickListener {
                    // LogUtils.d(TAG, "repeatView clicked")
                    if (isDoubleClick()) return@setOnClickListener
                    performRepeat(msgRecord)
                }

                setOnLongClickListener {
                    showActionDialog(msgRecord, repeatView.context)
                    true
                }
            }
        } catch (e: Exception) {
            LogUtils.e(TAG, "onHandleIntent: ${e.message}")
        }
    }

    /**
     * 长按复读按钮后，弹出 Compose 操作列表弹窗。
     * 操作项按消息内容动态显示：图片→复制链接，视频/语音→复制路径，
     * 始终提供：复制文本、原始消息(MsgRecord)、重构消息(MsgData)。
     */
    private fun showActionDialog(msgRecord: MsgRecord, viewContext: Context) {
        try {
            val elements = msgRecord.elements
            if (elements.isNullOrEmpty()) {
                LogUtils.e(TAG, "showActionDialog: elements is null")
                return
            }

            // 优先使用真实宿主 Activity，避免 view.context 已销毁导致 BadTokenException
            val hostActivity = QQCurrentEnv.getActivity() ?: (viewContext as? Activity)
            if (hostActivity == null || hostActivity.isFinishing || hostActivity.isDestroyed) {
                LogUtils.e(TAG, "showActionDialog: host activity invalid")
                Toasts.showCustomToast("当前页面状态异常，无法打开菜单")
                return
            }
            Parasitics.ensureInitialized(hostActivity)
            runCatching { Parasitics.injectModuleResources(hostActivity.resources) }

            val msgData = MsgData(msgRecord)
            val actions = mutableListOf<RepeatMsgAction>()

            // 复制链接：仅图片消息
            if (msgData.picList.size > 0) {
                actions.add(
                    RepeatMsgAction("复制链接", "共 ${msgData.picList.size} 张图片") {
                        copyToClipboard(hostActivity, msgData.picList.joinToString("\n"))
                        Toasts.showCustomToast("已复制图片链接到剪贴板")
                    }
                )
            }

            // 复制路径：视频 / 语音
            if (msgData.videoList.size > 0) {
                actions.add(
                    RepeatMsgAction("复制路径", "视频(需先预览视频才会缓存)") {
                        copyToClipboard(hostActivity, msgData.videoList.joinToString("\n"))
                        Toasts.showCustomToast("已复制视频路径到剪贴板")
                    }
                )
            }
            if (msgData.pttList.size > 0) {
                actions.add(
                    RepeatMsgAction("复制路径", "语音(需先播放语音才会缓存)") {
                        copyToClipboard(hostActivity, msgData.pttList.joinToString("\n"))
                        Toasts.showCustomToast("已复制语音路径到剪贴板")
                    }
                )
            }

            // 复制文本：始终提供
            actions.add(
                RepeatMsgAction("复制文本") {
                    copyToClipboard(hostActivity, msgData.msg)
                    Toasts.showCustomToast("已复制消息到剪贴板")
                }
            )

            // 原始消息：完整展示 MsgRecord 原始内容（不格式化、不省略，可选中复制）
            actions.add(
                RepeatMsgAction("原始消息", "查看 MsgRecord 原始内容") {
                    RawTextDialog(hostActivity, "原始消息 (MsgRecord)", msgRecord.toString()).show()
                }
            )

            // 重构消息：完整展示 MsgData 原始内容（不格式化、不省略，可选中复制）
            actions.add(
                RepeatMsgAction("重构消息", "查看 MsgData 原始内容") {
                    RawTextDialog(hostActivity, "重构消息 (MsgData)", msgData.toString()).show()
                }
            )

            RepeatMsgActionDialog(hostActivity, "消息操作", actions).show()
        } catch (e: Exception) {
            LogUtils.e(TAG, "showActionDialog failed: ${e.message}")
            Toasts.showCustomToast("打开菜单失败: ${e.message}")
        }
    }

    private fun copyToClipboard(context: Context, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        if (clipboard != null) {
            val clip = ClipData.newPlainText("repeat_msg", text)
            clipboard.setPrimaryClip(clip)
        }
    }




    private fun performRepeat(msgRecord: MsgRecord) {

        val msgType = msgRecord.msgType

        val elements = msgRecord.elements
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

    private fun directSend(msgRecord: MsgRecord, elements: ArrayList<MsgElement>) {
        val peerUid = msgRecord.peerUid ?: return
        val contact = MsgTool.makeContact(peerUid, msgRecord.chatType)
        MsgTool.sendMsgInternal(contact, elements)
    }

    private fun forwardSend(msgRecord: MsgRecord, elements: ArrayList<MsgElement>) {
        val peerUid = msgRecord.peerUid ?: return
        val contact = MsgTool.makeContact(peerUid, msgRecord.chatType)
        MsgTool.forwardMsg(contact, elements)
    }

    private fun isDoubleClick(): Boolean {
        val now = System.currentTimeMillis()
        val time = now - lastClickTime
        lastClickTime = now
        return time < 500
    }
}