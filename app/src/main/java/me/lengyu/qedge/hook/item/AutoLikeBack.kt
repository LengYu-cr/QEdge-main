package me.lengyu.qedge.hook.item

import me.lengyu.qedge.hook.annotation.HookItemAnnotation
import me.lengyu.qedge.hook.api.FromServiceMsgDispatcher
import me.lengyu.qedge.hook.base.BaseApiHookItem
import me.lengyu.qedge.utils.LogUtils
import me.lengyu.qedge.utils.ModuleConfig
import me.lengyu.qedge.utils.qq.FriendTool
import org.json.JSONObject
import java.util.Collections
import java.util.LinkedHashMap

@HookItemAnnotation(value = "名片自动回赞", category = "item")
object AutoLikeBack : BaseApiHookItem<AutoLikeBack.Listener>() {

    const val TAG = "AutoLikeBack"
    const val SP_KEY = "profile_auto_like_back"

    /** 已处理的 msgUid，避免重复推送导致多次回赞。O(1) 查询，上限 500 LRU 自动淘汰 */
    private val processedMsgUid = Collections.synchronizedSet(
        Collections.newSetFromMap(
            object : LinkedHashMap<Long, Boolean>(128, 0.75f, true) {
                override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Long, Boolean>): Boolean = size > 500
            }
        )
    )

    interface Listener : me.lengyu.qedge.hook.base.Listener {
        fun onAutoLikeBack(fromUin: Long, fromNick: String, result: Boolean)
    }

    fun isEnabled(): Boolean =
        runCatching { ModuleConfig.getBoolean(SP_KEY, false) }.getOrDefault(false)

    override fun loadHook() {
        try {
            FromServiceMsgDispatcher.loadHook()
            FromServiceMsgDispatcher.registerListener { _, json, _ ->
                runCatching {
                    if (!isEnabled()) return@registerListener
                    handleOlPush(json)
                }.onFailure {
                    LogUtils.e(TAG, "dispatcher error: ${it.message}")
                }
            }
            // LogUtils.d(TAG, "loadHook success → 监听 OlPush type=203 名片被赞推送，回赞走 FriendTool.sendZan()")
        } catch (e: Throwable) {
            LogUtils.e(TAG, "loadHook error: ${e.message}")
        }
    }

    private fun handleOlPush(json: JSONObject) {
        // 先取 msgUid 去重（2.32），同一个推送不处理两次
        val msgUid = runCatching { json.optJSONObject("2")?.optLong("32", 0L) ?: 0L }.getOrDefault(0L)
        if (msgUid != 0L) {
            if (!processedMsgUid.add(msgUid)) return // O(1)
        }

        // 路径：1 → 3 → 2 → 1
        val msg1 = json.optJSONObject("1") ?: return
        val msg3 = msg1.optJSONObject("3") ?: return
        val msg32 = msg3.optJSONObject("2") ?: return
        val firstSub = msg32.optJSONObject("1") ?: return

        // type 必须是 203（名片被赞推送）
        val type = firstSub.optInt("2", -1)
        if (type != 203) return

        // 业务数据 203 → 14 → 3
        val biz = firstSub.optJSONObject("203") ?: return
        val obj14 = biz.optJSONObject("14") ?: return
        val inner3 = obj14.optJSONObject("3") ?: return

        // 点赞者 UIN（14.3.3）和昵称（14.3.5）
        val fromUin = inner3.optLong("3", 0L)
        val fromNick = inner3.optString("5", "未知用户")

        // 点赞次数：从文字 14.3.1（例："赞了我的资料卡10次"）里正则匹配数字，取不到默认 1，上限 20 避免被刷
        val tipText = inner3.optString("1", "")
        val likeTimes = """(\d+)""".toRegex()
            .find(tipText)
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()
            ?.coerceIn(1, 20) ?: 1

        if (fromUin <= 0L) return

        // 异步回赞，避免阻塞推送分发线程
        Thread({
            try {
                // 轻微延迟，模拟手动操作，避免风控
                Thread.sleep(600 + (Math.random() * 400).toLong())
                // 对方赞了几次就回几次，走 NT 原生接口
                FriendTool.sendZan(fromUin.toString(), likeTimes)
                val ok = true
                // LogUtils.d(TAG, "收到来自【$fromNick($fromUin)】的名片赞 ×$likeTimes → 回赞 $likeTimes 次")
                for (listener in getListenerSet()) {
                    runCatching { listener.onAutoLikeBack(fromUin, fromNick, ok) }
                }
            } catch (t: Throwable) {
                LogUtils.e(TAG, "autoLike thread error: ${t.message}")
            }
        }, "AutoLikeBack-$fromUin").start()
    }
}
