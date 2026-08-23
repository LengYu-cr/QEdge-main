package me.lengyu.qedge.hook.item

import com.tencent.qqnt.kernel.nativeinterface.MsgElement
import com.tencent.qqnt.kernelpublic.nativeinterface.Contact
import me.lengyu.qedge.hook.annotation.HookItemAnnotation
import me.lengyu.qedge.hook.api.OnSendMsg
import me.lengyu.qedge.hook.base.BaseApiHookItem
import me.lengyu.qedge.hook.base.Listener
import me.lengyu.qedge.utils.HostInfo
import me.lengyu.qedge.utils.LogUtils
import me.lengyu.qedge.utils.ModuleConfig
import me.lengyu.qedge.utils.qq.MsgTool
import java.util.ArrayList
/**
 * @Author 冷雨
 * @Description 视频转泡泡消息
 */
@HookItemAnnotation(value = "视频转泡泡消息", category = "item")
object VideoToBubble : BaseApiHookItem<VideoToBubble.VideoToBubbleListener>() {

    private const val TAG = "VideoToBubble"
    private const val CONFIG_KEY = "video_to_bubble"

    @JvmStatic
    fun isEnabled(): Boolean {
        if (!HostInfo.isQQ) return false
        return ModuleConfig.getBoolean(CONFIG_KEY, false)
    }

    override fun loadHook() {
        try {
            if (HostInfo.isTIM) {
                // LogUtils.e(TAG, "loadHook SKIP: 宿主=${HostInfo.packageName} (TIM),不支持泡泡视频,本 Hook 不注册")
                return
            }
            OnSendMsg.registerListener(object : OnSendMsg.SendMsgListener {
                override fun onSend(contact: Contact?, elementsRaw: ArrayList<*>?) {
                    runCatching {
                        if (!isEnabled()) return@runCatching
                        if (contact == null || elementsRaw == null || elementsRaw.isEmpty()) return@runCatching

                        val elements = ArrayList<MsgElement>()
                        for (item in elementsRaw) {
                            if (item is MsgElement) {
                                elements.add(item)
                            }
                        }

                        val (videoMsgEl: MsgElement, videoPath: String) = findVideoElementAndPath(elements)
                            ?: return@runCatching
                        
                        val videoElement = videoMsgEl.videoElement ?: return@runCatching

                        val removed = elements.remove(videoMsgEl)
                        
                        // if (!removed) {
                        //     LogUtils.e(TAG, "[FAIL] 从 elements 删除原 videoMsgEl 失败 (list size=${elements.size})")
                        //     return@runCatching
                        // }

                        val bubbleEl = MsgTool.createBubbleVideoElement(videoPath)
                            bubbleEl.videoElement = videoElement
                        if (bubbleEl == null) {
                            LogUtils.e(TAG, "[FAIL] createBubbleVideoElement 返回 null,放弃替换,原消息正常发送")
                            return@runCatching
                        }

                        elements.add(0, bubbleEl)
                        
                        elementsRaw.clear()
                        @Suppress("UNCHECKED_CAST")
                        val targetList = elementsRaw as ArrayList<MsgElement>
                        targetList.addAll(elements)
                    }.onFailure { e ->
                        LogUtils.e(TAG, "[ERROR] onSend callback error: ${e.message}")
                    }
                }
            })
        } catch (e: Throwable) {
            LogUtils.e(TAG, "loadHook error: ${e.message}")
        }
    }

    private fun findVideoElementAndPath(elements: List<MsgElement>): Pair<MsgElement, String>? {
        for (el in elements) {
            runCatching {
                val videoEl = el.javaClass.getField("videoElement").get(el) ?: return@runCatching
                val fp = videoEl.javaClass.getField("filePath").get(videoEl) as? String
                if (!fp.isNullOrBlank()) return el to fp
            }
        }
        return null
    }

    interface VideoToBubbleListener : Listener
}
