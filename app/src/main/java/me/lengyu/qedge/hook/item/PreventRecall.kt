package me.lengyu.qedge.hook.item

import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import me.lengyu.qedge.hook.annotation.HookItemAnnotation
import me.lengyu.qedge.hook.base.BaseSwitchHookItem
import me.lengyu.qedge.utils.HookUtils
import me.lengyu.qedge.utils.LogUtils
import me.lengyu.qedge.utils.ModuleConfig
import me.lengyu.qedge.utils.ReflectUtils
import me.lengyu.qedge.utils.qq.QQCurrentEnv
import me.lengyu.qedge.utils.qq.FriendTool
import java.io.ByteArrayOutputStream
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

/**
 * @Author 冷雨
 * @Description 防撤回 — 基于 QStory PreventRetractingMessageCore 逻辑
 *
 * 核心思路：
 * 1. Hook onMsfPush 拦截撤回推送
 * 2. InfoSyncPush：移除 syncInfoBody 中的撤回消息，内核收不到撤回通知
 * 3. MsgPush：将撤回操作中的 msgSeq 改为非法值 1，撤回失效
 */
@HookItemAnnotation(value = "防撤回", category = "item", tag = "防撤回", desc = "拦截QQ消息撤回，已撤回的消息依然可见")
class PreventRecall : BaseSwitchHookItem() {

    companion object {
        private const val TAG = "PreventRecall"
        private const val SP_KEY = "prevent_recall"
        private const val RECALL_PROMPT_VIEW_ID = 0x298382

        private fun isEnabled(): Boolean {
            return ModuleConfig.getBoolean(SP_KEY, false)
        }
    }

    // ==================== 撤回元数据存储（用于UI提示） ====================

    private data class RecallMeta(
        val operatorUid: String,
        val chatType: Int,  // 1 = C2C, 2 = Group
        val msgSeq: Int,
        val groupUin: String? = null
    ) {
        val key: String get() = if (chatType == 2 && groupUin != null) "${groupUin}_$msgSeq" else "${operatorUid}_$msgSeq"
    }

    /** 内存索引：key = peerUid/groupUin_msgSeq → RecallMeta */
    private val recallMetaMap = ConcurrentHashMap<String, RecallMeta>()

    // ==================== 实时视图刷新 ====================

    private val mainHandler = Handler(Looper.getMainLooper())
    private var currentAIOAdapter: WeakReference<Any>? = null
    private val recallExecutor = Executors.newSingleThreadExecutor { r ->
        Thread(r, "PreventRecall").apply { isDaemon = true }
    }

    override fun onInit(): Boolean {
        return true
    }

    override fun onHook() {
        val hostCL = ReflectUtils.hostClassLoader
        if (hostCL == null) {
            LogUtils.e(TAG, "onHook: hostClassLoader is null, skip hook")
            return
        }
        try {
            val className = "com.tencent.qqnt.kernel.nativeinterface.IQQNTWrapperSession\$CppProxy"
            val sessionClass = XposedHelpers.findClass(className, hostCL)

            val pushExtraInfoClass = Class.forName(
                "com.tencent.qqnt.kernel.nativeinterface.PushExtraInfo",
                false,
                hostCL
            )

            HookUtils.hookClassMethod(
                sessionClass, "onMsfPush",
                arrayOf(String::class.java, ByteArray::class.java, pushExtraInfoClass),
                { param ->
                    val cmd = param.args[0] as? String ?: return@hookClassMethod
                    try {
                        if (!isEnabled()) return@hookClassMethod
                        val buffer = param.args[1] as? ByteArray ?: return@hookClassMethod
                        when (cmd) {
                            "trpc.msg.register_proxy.RegisterProxy.InfoSyncPush" ->
                                handleInfoSyncPush(buffer, param)
                            "trpc.msg.olpush.OlPushService.MsgPush" ->
                                handleMsgPush(buffer, param)
                        }
                    } catch (e: Exception) {
                        LogUtils.e(TAG, "beforeHookedMethod error: ${e.message}")
                        LogUtils.e(TAG, e)
                    }
                },
                null
            )
        } catch (e: Exception) {
            LogUtils.e(TAG, "Hook failed: ${e.message}")
        }

        // Hook AIO 消息视图更新，用于添加「已撤回」提示
        hookAIOMsgUpdate()
    }

    // ==================== InfoSyncPush 处理 ====================

    /**
     * 处理 InfoSyncPush：移除 syncInfoBody 中的撤回消息条目
     * 使用原始字节操作（与 QStory 一致），避免 ProtoData 重编码改变字段顺序
     *
     * 纯字节数组扫描(无 IO/阻塞)，直接在推送线程内联执行同步拦截。
     * 此前是提交到单线程池再 future.get(500ms) 等待，多个推送包会串行排队并阻塞推送线程，
     * 内联后既保持同步拦截语义，又消除了排队与最多 500ms 的阻塞。
     */
    private fun handleInfoSyncPush(buffer: ByteArray, param: XC_MethodHook.MethodHookParam) {
        val newBuffer = try {
            val syncInfoBodyStart = findSyncInfoBody(buffer) ?: return
            val syncInfoBodyEnd = findFieldEnd(buffer, syncInfoBodyStart) ?: return
            val tagKey = readVarInt(buffer, syncInfoBodyStart) ?: return
            val payloadStart = skipVarInt(buffer, tagKey.nextIndex) ?: return
            val payloadBytes = buffer.copyOfRange(payloadStart, syncInfoBodyEnd)
            val newPayload = rewriteSyncInfoBody(payloadBytes) ?: return
            val output = ByteArrayOutputStream(buffer.size + newPayload.size - payloadBytes.size)
            output.write(buffer, 0, payloadStart)
            writeVarInt(output, newPayload.size)
            output.write(newPayload)
            output.write(buffer, syncInfoBodyEnd, buffer.size - syncInfoBodyEnd)
            output.toByteArray()
        } catch (e: Exception) {
            LogUtils.e(TAG, "handleInfoSyncPush error: ${e.message}")
            null
        }
        if (newBuffer != null) {
            param.args[1] = newBuffer
        }
    }

    /**
     * 重写 syncInfoBody 的每个条目，移除撤回消息
     * 返回 null 表示没有修改
     */
    private fun rewriteSyncInfoBody(bytes: ByteArray): ByteArray? {
        val output = ByteArrayOutputStream(bytes.size)
        var index = 0
        var changed = false

        while (index < bytes.size) {
            val fieldStart = index
            val key = readVarInt(bytes, index) ?: return null
            index = key.nextIndex
            val fieldNumber = key.value ushr 3
            val wireType = key.value and 0x07

            val fieldEnd = when (wireType) {
                0 -> readVarInt(bytes, index)?.nextIndex ?: return null
                1 -> (index + 8).coerceAtMost(bytes.size)
                2 -> {
                    val length = readVarInt(bytes, index) ?: return null
                    (length.nextIndex + length.value).coerceAtMost(bytes.size)
                }
                5 -> (index + 4).coerceAtMost(bytes.size)
                else -> return null
            }

            // field 8, wire type 2 是消息体，检查是否为撤回消息
            if (fieldNumber == 8 && wireType == 2) {
                val length = readVarInt(bytes, index) ?: return null
                val msgStart = length.nextIndex
                val msgEnd = msgStart + length.value
                val msgBytes = bytes.copyOfRange(msgStart, msgEnd)

                if (isRecallMessage(msgBytes)) {
                    changed = true
                    // 跳过这条撤回消息，不写入 output
                } else {
                    output.write(bytes, fieldStart, fieldEnd - fieldStart)
                }
            } else {
                output.write(bytes, fieldStart, fieldEnd - fieldStart)
            }
            index = fieldEnd
        }

        return if (changed) output.toByteArray() else null
    }

    /**
     * 判断消息体是否为撤回消息
     * msgType=528 + msgSubType=138 → 私聊撤回
     * msgType=732 + msgSubType=17 → 群聊撤回
     */
    private fun isRecallMessage(bytes: ByteArray): Boolean {
        var index = 0
        var msgType = 0
        var msgSubType = 0

        while (index < bytes.size) {
            val key = readVarInt(bytes, index) ?: break
            index = key.nextIndex
            val fieldNumber = key.value ushr 3
            val wireType = key.value and 0x07

            when (wireType) {
                0 -> {
                    val value = readVarInt(bytes, index) ?: break
                    index = value.nextIndex
                    // field 2 = messageContentInfo, field 1 = msgType, field 2 = msgSubType
                    // 但 messageContentInfo 是嵌套消息，需要递归解析
                }
                2 -> {
                    val length = readVarInt(bytes, index) ?: break
                    val subStart = length.nextIndex
                    val subEnd = subStart + length.value
                    index = subEnd

                    if (fieldNumber == 2) {
                        // messageContentInfo 嵌套消息
                        val subBytes = bytes.copyOfRange(subStart, subEnd)
                        val (t, s) = parseMsgTypeAndSubType(subBytes)
                        msgType = t
                        msgSubType = s
                    }
                }
                else -> {
                    val skipEnd = when (wireType) {
                        1 -> (index + 8).coerceAtMost(bytes.size)
                        5 -> (index + 4).coerceAtMost(bytes.size)
                        else -> break
                    }
                    index = skipEnd
                }
            }
        }

        return (msgType == 528 && msgSubType == 138) || (msgType == 732 && msgSubType == 17)
    }

    /**
     * 从 messageContentInfo 子消息中解析 msgType 和 msgSubType
     */
    private fun parseMsgTypeAndSubType(bytes: ByteArray): Pair<Int, Int> {
        var msgType = 0
        var msgSubType = 0
        var index = 0

        while (index < bytes.size) {
            val key = readVarInt(bytes, index) ?: break
            index = key.nextIndex
            val fieldNumber = key.value ushr 3
            val wireType = key.value and 0x07

            when {
                wireType == 0 -> {
                    val value = readVarInt(bytes, index) ?: break
                    index = value.nextIndex
                    when (fieldNumber) {
                        1 -> msgType = value.value
                        2 -> msgSubType = value.value
                    }
                }
                wireType == 2 -> {
                    val length = readVarInt(bytes, index) ?: break
                    index = length.nextIndex + length.value
                }
                else -> {
                    val skipEnd = when (wireType) {
                        1 -> (index + 8).coerceAtMost(bytes.size)
                        5 -> (index + 4).coerceAtMost(bytes.size)
                        else -> break
                    }
                    index = skipEnd
                }
            }
        }

        return Pair(msgType, msgSubType)
    }

    /**
     * 在 buffer 中定位 syncInfoBody 字段的起始位置
     * 结构：InfoSyncPush(1: syncRecallContent(1: syncInfoBody))
     * 返回 syncInfoBody 的 tag 位置
     */
    private fun findSyncInfoBody(buffer: ByteArray): Int? {
        var index = 0
        // 跳过 InfoSyncPush.syncRecallContent (field 1, wire type 2)
        while (index < buffer.size) {
            val key = readVarInt(buffer, index) ?: return null
            val fn = key.value ushr 3
            val wt = key.value and 0x07
            index = key.nextIndex

            if (fn == 1 && wt == 2) {
                // 进入 syncRecallContent
                val length = readVarInt(buffer, index) ?: return null
                index = length.nextIndex
                // 在 syncRecallContent 内找 syncInfoBody (field 1, wire type 2)
                val subKey = readVarInt(buffer, index) ?: return null
                val subFn = subKey.value ushr 3
                val subWt = subKey.value and 0x07
                if (subFn == 1 && subWt == 2) {
                    return index // 返回 tag 的起始位置（readVarInt 调用前的 index）
                }
                return null
            }

            // 跳过其他字段
            val skipEnd = when (wt) {
                0 -> readVarInt(buffer, index)?.nextIndex ?: return null
                1 -> (index + 8).coerceAtMost(buffer.size)
                2 -> {
                    val length = readVarInt(buffer, index) ?: return null
                    length.nextIndex + length.value
                }
                5 -> (index + 4).coerceAtMost(buffer.size)
                else -> return null
            }
            index = skipEnd
        }
        return null
    }

    /**
     * 找到字段的结束位置（跳过整个字段包括其 payload）
     */
    private fun findFieldEnd(buffer: ByteArray, tagIndex: Int): Int? {
        val key = readVarInt(buffer, tagIndex) ?: return null
        val wireType = key.value and 0x07
        var index = key.nextIndex

        return when (wireType) {
            0 -> readVarInt(buffer, index)?.nextIndex
            1 -> (index + 8).coerceAtMost(buffer.size)
            2 -> {
                val length = readVarInt(buffer, index) ?: return null
                length.nextIndex + length.value
            }
            5 -> (index + 4).coerceAtMost(buffer.size)
            else -> null
        }
    }

    // ==================== MsgPush 处理 ====================

    private data class RewriteResult(
        val buffer: ByteArray?,
        val recallMeta: RecallMeta?
    )

    /**
     * 处理 MsgPush：将撤回操作中的 msgSeq 改为非法值 1，使撤回失效
     *
     * 重写逻辑是纯字节扫描(无 IO/阻塞)，直接在推送线程内联同步执行拦截，
     * 消除了原先单线程池排队 + future.get(500ms) 的阻塞。后续 AIO 视图刷新仍走异步。
     */
    private fun handleMsgPush(buffer: ByteArray, param: XC_MethodHook.MethodHookParam) {
        val result = try {
            rewriteMsgPush(buffer)
        } catch (e: Exception) {
            LogUtils.e(TAG, "handleMsgPush error: ${e.message}")
            null
        } ?: return

        if (result.buffer != null) {
            param.args[1] = result.buffer
        }
        if (result.recallMeta != null) {
            recallMetaMap[result.recallMeta.key] = result.recallMeta
            mainHandler.postDelayed({
                if (currentAIOAdapter?.get() == null) {
                    // adapter 未缓存，等待视图绑定后再刷新
                    recallExecutor.execute {
                        try { Thread.sleep(150) } catch (_: Exception) {}
                        mainHandler.post { triggerAIORefresh() }
                    }
                } else {
                    triggerAIORefresh()
                }
            }, 200)
        }
    }

    /**
     * 重写 MsgPush 中的撤回操作
     * 返回 null 表示不是撤回消息，无需处理
     */
    private fun rewriteMsgPush(buffer: ByteArray): RewriteResult? {
        var index = 0
        var msgType = 0
        var subSeq = 0
        var operationInfoFieldStart = -1
        var operationInfoPayloadStart = -1
        var operationInfoPayloadEnd = -1

        // 第一遍扫描：找到 msgType, subSeq 和 operationInfo 的位置
        while (index < buffer.size) {
            val key = readVarInt(buffer, index) ?: break
            val fieldNumber = key.value ushr 3
            val wireType = key.value and 0x07
            index = key.nextIndex

            when (wireType) {
                0 -> {
                    val value = readVarInt(buffer, index) ?: break
                    index = value.nextIndex
                }
                2 -> {
                    val length = readVarInt(buffer, index) ?: break
                    val subStart = length.nextIndex
                    val subEnd = subStart + length.value
                    index = subEnd

                    if (fieldNumber == 1) {
                        // qqMessage 嵌套消息
                        val subBytes = buffer.copyOfRange(subStart, subEnd)
                        val (t, s, opStart, opPayloadStart, opPayloadEnd) = parseMsgPushQQMessage(subBytes)
                        msgType = t
                        subSeq = s
                        if (opStart >= 0) {
                            operationInfoFieldStart = subStart + opStart
                            operationInfoPayloadStart = subStart + opPayloadStart
                            operationInfoPayloadEnd = subStart + opPayloadEnd
                        }
                    }
                }
                else -> {
                    val skipEnd = when (wireType) {
                        1 -> (index + 8).coerceAtMost(buffer.size)
                        5 -> (index + 4).coerceAtMost(buffer.size)
                        else -> break
                    }
                    index = skipEnd
                }
            }
        }

        if (operationInfoFieldStart < 0) return null

        val isRecall = msgType == 528 && subSeq == 138 || msgType == 732 && subSeq == 17
        if (!isRecall) return null

        // 提取原始撤回元数据（在修改前）
        val recallMeta = extractRecallMeta(buffer, operationInfoPayloadStart, operationInfoPayloadEnd, msgType)

        // 第二遍：修改 operationInfo
        val newBuffer = when {
            msgType == 528 && subSeq == 138 -> {
                rewriteC2CRecallOpInfo(buffer, operationInfoPayloadStart, operationInfoPayloadEnd)
            }
            msgType == 732 && subSeq == 17 -> {
                if (operationInfoPayloadEnd - operationInfoPayloadStart > 7) {
                    rewriteGroupRecallOpInfo(buffer, operationInfoPayloadStart + 7, operationInfoPayloadEnd)
                } else null
            }
            else -> null
        }

        return RewriteResult(newBuffer, recallMeta)
    }

    /**
     * 从 qqMessage 子消息中解析 msgType, subSeq 和 operationInfo 位置
     */
    private fun parseMsgPushQQMessage(bytes: ByteArray): Quintuple {
        var msgType = 0
        var subSeq = 0
        var opFieldStart = -1
        var opPayloadStart = -1
        var opPayloadEnd = -1
        var index = 0

        while (index < bytes.size) {
            val key = readVarInt(bytes, index) ?: break
            val fieldNumber = key.value ushr 3
            val wireType = key.value and 0x07
            index = key.nextIndex

            when (wireType) {
                0 -> {
                    val value = readVarInt(bytes, index) ?: break
                    index = value.nextIndex
                }
                2 -> {
                    val length = readVarInt(bytes, index) ?: break
                    val subStart = length.nextIndex
                    val subEnd = subStart + length.value

                    when (fieldNumber) {
                        2 -> {
                            // messageContentInfo
                            val subBytes = bytes.copyOfRange(subStart, subEnd)
                            val (t, s) = parseMsgTypeAndSubType(subBytes)
                            msgType = t
                            subSeq = s
                        }
                        3 -> {
                            // messageBody → 找 operationInfo
                            val subBytes = bytes.copyOfRange(subStart, subEnd)
                            var si = 0
                            while (si < subBytes.size) {
                                val sk = readVarInt(subBytes, si) ?: break
                                val sfn = sk.value ushr 3
                                val swt = sk.value and 0x07
                                si = sk.nextIndex
                                if (sfn == 2 && swt == 2) {  // operationInfo is field 2 of MessageBody
                                    val sl = readVarInt(subBytes, si) ?: break
                                    opFieldStart = subStart + (sk.nextIndex - 1)
                                    opPayloadStart = subStart + sl.nextIndex
                                    opPayloadEnd = subStart + sl.nextIndex + sl.value
                                    break
                                }
                                when (swt) {
                                    0 -> { val sv = readVarInt(subBytes, si) ?: break; si = sv.nextIndex }
                                    1 -> si = (si + 8).coerceAtMost(subBytes.size)
                                    2 -> {
                                        val sl = readVarInt(subBytes, si) ?: break
                                        si = sl.nextIndex + sl.value
                                    }
                                    5 -> si = (si + 4).coerceAtMost(subBytes.size)
                                    else -> break
                                }
                            }
                        }
                    }
                    index = subEnd
                }
                else -> {
                    val skipEnd = when (wireType) {
                        1 -> (index + 8).coerceAtMost(bytes.size)
                        5 -> (index + 4).coerceAtMost(bytes.size)
                        else -> break
                    }
                    index = skipEnd
                }
            }
        }

        return Quintuple(msgType, subSeq, opFieldStart, opPayloadStart, opPayloadEnd)
    }

    private data class Quintuple(
        val msgType: Int,
        val subSeq: Int,
        val opFieldStart: Int,
        val opPayloadStart: Int,
        val opPayloadEnd: Int
    )

    // ==================== 撤回元数据提取 ====================

    /**
     * 从 operationInfo 字节中提取撤回元数据
     */
    private fun extractRecallMeta(
        buffer: ByteArray,
        payloadStart: Int,
        payloadEnd: Int,
        msgType: Int
    ): RecallMeta? {
        val payload = buffer.copyOfRange(payloadStart, payloadEnd)
        return when (msgType) {
            528 -> extractC2CRecallMeta(payload)
            732 -> {
                if (payload.size > 7) {
                    extractGroupRecallMeta(payload.copyOfRange(7, payload.size))
                } else null
            }
            else -> null
        }
    }

    /**
     * 从 C2CRecallOperationInfo 中提取撤回元数据
     * 结构：C2CRecallOperationInfo(1: info(1: operatorUid, 2: peerUid, 20: msgSeq, ...))
     * 
     * 注意：peerUid 是撤回操作中的聊天对方，当对方撤回时 peerUid 是当前用户的 uid
     * 需要判断哪个 uid 才是真正的聊天对方（peer），与 MsgRecord.getPeerUin() 保持一致
     */
    private fun extractC2CRecallMeta(bytes: ByteArray): RecallMeta? {
        // 定位 info 子消息 (field 1, wire 2)
        val inner = extractLengthDelimited(bytes, 1) ?: return null

        var operatorUid = ""
        var peerUid = ""
        var msgSeq = 0
        var index = 0
        while (index < inner.size) {
            val key = readVarInt(inner, index) ?: break
            val fn = key.value ushr 3
            val wt = key.value and 0x07
            index = key.nextIndex
            when {
                fn == 1 && wt == 2 -> {
                    val l = readVarInt(inner, index) ?: break
                    operatorUid = String(inner, l.nextIndex, l.value, Charsets.UTF_8)
                    index = l.nextIndex + l.value
                }
                fn == 2 && wt == 2 -> {
                    val l = readVarInt(inner, index) ?: break
                    peerUid = String(inner, l.nextIndex, l.value, Charsets.UTF_8)
                    index = l.nextIndex + l.value
                }
                fn == 20 && wt == 0 -> {
                    val v = readVarInt(inner, index) ?: break
                    msgSeq = v.value
                    index = v.nextIndex
                }
                else -> {
                    index = when (wt) {
                        0 -> readVarInt(inner, index)?.nextIndex ?: break
                        1 -> (index + 8).coerceAtMost(inner.size)
                        2 -> {
                            val l = readVarInt(inner, index) ?: break
                            l.nextIndex + l.value
                        }
                        5 -> (index + 4).coerceAtMost(inner.size)
                        else -> break
                    }
                }
            }
        }
        if (peerUid.isEmpty()) return null

        val currentUin = QQCurrentEnv.getCurrentUin()
        val peerUin = FriendTool.getUinFromUid(peerUid)
        val operatorUin = if (operatorUid.isNotEmpty()) FriendTool.getUinFromUid(operatorUid) else ""

        // 如果 peerUid 是当前用户，说明是对方撤回的，用 operatorUid 作为 peer
        // 否则 peerUid 就是对方
        val keyUin = if (peerUin == currentUin) operatorUin else peerUin

        return RecallMeta(keyUin, 1, msgSeq)
    }

    /**
     * 从 GroupRecallOperationInfo 中提取撤回元数据（不含前7字节头部）
     * 结构：GroupRecallOperationInfo(4: peerId, 11: info(1: operatorUid, 3: msgInfo(1: msgSeq)))
     */
    private fun extractGroupRecallMeta(bytes: ByteArray): RecallMeta? {
        var groupUin: String? = null
        var operatorUid: String? = null
        var msgSeq = 0
        var index = 0
        while (index < bytes.size) {
            val key = readVarInt(bytes, index) ?: break
            val fn = key.value ushr 3
            val wt = key.value and 0x07
            index = key.nextIndex
            when {
                fn == 4 && wt == 0 -> {
                    val value = readVarInt(bytes, index) ?: break
                    groupUin = value.value.toString()
                    index = value.nextIndex
                }
                fn == 11 && wt == 2 -> {
                    val length = readVarInt(bytes, index) ?: break
                    val subStart = length.nextIndex
                    val subEnd = subStart + length.value
                    val subBytes = bytes.copyOfRange(subStart, subEnd)
                    var si = 0
                    while (si < subBytes.size) {
                        val sk = readVarInt(subBytes, si) ?: break
                        val sfn = sk.value ushr 3
                        val swt = sk.value and 0x07
                        si = sk.nextIndex
                        when {
                            sfn == 1 && swt == 2 -> {
                                val sl = readVarInt(subBytes, si) ?: break
                                operatorUid = String(subBytes, sl.nextIndex, sl.value, Charsets.UTF_8)
                                si = sl.nextIndex + sl.value
                            }
                            sfn == 3 && swt == 2 -> {
                                // msgInfo 嵌套消息
                                val sl = readVarInt(subBytes, si) ?: break
                                val ms = sl.nextIndex
                                val me = ms + sl.value
                                val msgInfoBytes = subBytes.copyOfRange(ms, me)
                                var mi = 0
                                while (mi < msgInfoBytes.size) {
                                    val mk = readVarInt(msgInfoBytes, mi) ?: break
                                    val mfn = mk.value ushr 3
                                    val mwt = mk.value and 0x07
                                    mi = mk.nextIndex
                                    if (mfn == 1 && mwt == 0) {
                                        val mv = readVarInt(msgInfoBytes, mi) ?: break
                                        msgSeq = mv.value
                                        mi = mv.nextIndex
                                    } else {
                                        mi = skipProtoField(msgInfoBytes, mi, mwt) ?: break
                                    }
                                }
                                si = me
                            }
                            else -> {
                                si = skipProtoField(subBytes, si, swt) ?: break
                            }
                        }
                    }
                    index = subEnd
                }
                else -> {
                    index = skipProtoField(bytes, index, wt) ?: break
                }
            }
        }
        if (groupUin == null || operatorUid == null) return null
        return RecallMeta(operatorUid, 2, msgSeq, groupUin)
    }

    /**
     * 跳过 protobuf 字段，返回下一个字段的起始位置
     */
    private fun skipProtoField(bytes: ByteArray, index: Int, wireType: Int): Int? {
        return when (wireType) {
            0 -> skipVarInt(bytes, index)
            1 -> (index + 8).coerceAtMost(bytes.size)
            2 -> {
                val length = readVarInt(bytes, index) ?: return null
                length.nextIndex + length.value
            }
            5 -> (index + 4).coerceAtMost(bytes.size)
            else -> null
        }
    }

    /**
     * 私聊撤回：将 C2CRecallOperationInfo.info.msgSeq 设为 1
     */
    private fun rewriteC2CRecallOpInfo(
        buffer: ByteArray,
        payloadStart: Int,
        payloadEnd: Int
    ): ByteArray? {
        val payload = buffer.copyOfRange(payloadStart, payloadEnd)
        val newPayload = rewriteC2CRecallMsgSeq(payload) ?: return null

        val output = ByteArrayOutputStream(buffer.size)
        output.write(buffer, 0, payloadStart)
        output.write(newPayload)
        output.write(buffer, payloadEnd, buffer.size - payloadEnd)
        return output.toByteArray()
    }

    /**
     * 修改 C2CRecallOperationInfo 中的 msgSeq 为 1
     * 结构：C2CRecallOperationInfo(1: operatorUid, 2: peerUid, 20: msgSeq, ...)
     */
    private fun rewriteC2CRecallMsgSeq(bytes: ByteArray): ByteArray? {
        var index = 0
        while (index < bytes.size) {
            val key = readVarInt(bytes, index) ?: break
            val fn = key.value ushr 3
            val wt = key.value and 0x07
            val tagStart = index
            index = key.nextIndex

            if (fn == 1 && wt == 2) {
                // info 嵌套消息 (field 1)，进入内部改 msgSeq (field 20)
                val length = readVarInt(bytes, index) ?: break
                val subStart = length.nextIndex
                val subEnd = subStart + length.value
                val subBytes = bytes.copyOfRange(subStart, subEnd)

                val newSubBytes = rewriteMsgSeqField(subBytes, 20) ?: return null

                val output = ByteArrayOutputStream(bytes.size)
                output.write(bytes, 0, tagStart)
                writeVarInt(output, (fn shl 3) or 2)
                writeVarInt(output, newSubBytes.size)
                output.write(newSubBytes)
                output.write(bytes, subEnd, bytes.size - subEnd)
                return output.toByteArray()
            }
            index = when (wt) {
                0 -> readVarInt(bytes, index)?.nextIndex ?: break
                1 -> (index + 8).coerceAtMost(bytes.size)
                2 -> {
                    val l = readVarInt(bytes, index) ?: break
                    l.nextIndex + l.value
                }
                5 -> (index + 4).coerceAtMost(bytes.size)
                else -> break
            }
        }
        return null
    }

    /**
     * 读取指定字段 (wire type 2) 的 length-delimited 内容字节
     */
    private fun extractLengthDelimited(bytes: ByteArray, targetField: Int): ByteArray? {
        var index = 0
        while (index < bytes.size) {
            val key = readVarInt(bytes, index) ?: break
            val fn = key.value ushr 3
            val wt = key.value and 0x07
            index = key.nextIndex

            if (fn == targetField && wt == 2) {
                val length = readVarInt(bytes, index) ?: break
                val start = length.nextIndex
                val end = start + length.value
                if (end > bytes.size) return null
                return bytes.copyOfRange(start, end)
            }
            index = when (wt) {
                0 -> readVarInt(bytes, index)?.nextIndex ?: break
                1 -> (index + 8).coerceAtMost(bytes.size)
                2 -> {
                    val l = readVarInt(bytes, index) ?: break
                    l.nextIndex + l.value
                }
                5 -> (index + 4).coerceAtMost(bytes.size)
                else -> break
            }
        }
        return null
    }

    /**
     * 群聊撤回：将 GroupRecallOperationInfo.info.msgInfo.msgSeq 设为 1
     */
    private fun rewriteGroupRecallOpInfo(
        buffer: ByteArray,
        payloadStart: Int,
        payloadEnd: Int
    ): ByteArray? {
        val payload = buffer.copyOfRange(payloadStart, payloadEnd)
        val newPayload = rewriteGroupRecallMsgSeq(payload) ?: return null

        val output = ByteArrayOutputStream(buffer.size)
        output.write(buffer, 0, payloadStart)
        output.write(newPayload)
        output.write(buffer, payloadEnd, buffer.size - payloadEnd)
        return output.toByteArray()
    }

    /**
     * 修改 GroupRecallOperationInfo 中的 msgSeq 为 1
     * 结构：GroupRecallOperationInfo(4: peerId, 11: info(1: operatorUid, 3: msgInfo(1: msgSeq)), 37: msgSeq)
     */
    private fun rewriteGroupRecallMsgSeq(bytes: ByteArray): ByteArray? {
        var index = 0
        while (index < bytes.size) {
            val key = readVarInt(bytes, index) ?: break
            val fn = key.value ushr 3
            val wt = key.value and 0x07
            val tagStart = index
            index = key.nextIndex

            if (fn == 11 && wt == 2) {
                // info 嵌套消息 (field 11)，进入内部找 msgInfo (field 3)
                val length = readVarInt(bytes, index) ?: break
                val subStart = length.nextIndex
                val subEnd = subStart + length.value
                val subBytes = bytes.copyOfRange(subStart, subEnd)

                val newSubBytes = rewriteMsgInfoSeq(subBytes) ?: return null

                val output = ByteArrayOutputStream(bytes.size)
                output.write(bytes, 0, tagStart)
                writeVarInt(output, (fn shl 3) or 2)
                writeVarInt(output, newSubBytes.size)
                output.write(newSubBytes)
                output.write(bytes, subEnd, bytes.size - subEnd)
                return output.toByteArray()
            }
            index = when (wt) {
                0 -> readVarInt(bytes, index)?.nextIndex ?: break
                1 -> (index + 8).coerceAtMost(bytes.size)
                2 -> {
                    val l = readVarInt(bytes, index) ?: break
                    l.nextIndex + l.value
                }
                5 -> (index + 4).coerceAtMost(bytes.size)
                else -> break
            }
        }
        return null
    }

    /**
     * 在 info 消息中找 msgInfo (field 3)，然后找 msgSeq (field 1) 并设为 1
     */
    private fun rewriteMsgInfoSeq(bytes: ByteArray): ByteArray? {
        var index = 0
        while (index < bytes.size) {
            val key = readVarInt(bytes, index) ?: break
            val fn = key.value ushr 3
            val wt = key.value and 0x07
            val tagStart = index
            index = key.nextIndex

            if (fn == 3 && wt == 2) {
                // msgInfo 嵌套消息 (field 3)
                val length = readVarInt(bytes, index) ?: break
                val subStart = length.nextIndex
                val subEnd = subStart + length.value
                val subBytes = bytes.copyOfRange(subStart, subEnd)

                val newSubBytes = rewriteMsgSeqField(subBytes, 1) ?: return null

                val output = ByteArrayOutputStream(bytes.size)
                output.write(bytes, 0, tagStart)
                writeVarInt(output, (fn shl 3) or 2)
                writeVarInt(output, newSubBytes.size)
                output.write(newSubBytes)
                output.write(bytes, subEnd, bytes.size - subEnd)
                return output.toByteArray()
            }
            index = when (wt) {
                0 -> readVarInt(bytes, index)?.nextIndex ?: break
                1 -> (index + 8).coerceAtMost(bytes.size)
                2 -> {
                    val l = readVarInt(bytes, index) ?: break
                    l.nextIndex + l.value
                }
                5 -> (index + 4).coerceAtMost(bytes.size)
                else -> break
            }
        }
        return null
    }

    /**
     * 将消息中的 msgSeq 字段（field 1, wire type 0）设为 1
     */
    private fun rewriteMsgSeqField(bytes: ByteArray, targetField: Int): ByteArray? {
        var index = 0
        while (index < bytes.size) {
            val key = readVarInt(bytes, index) ?: break
            val fn = key.value ushr 3
            val wt = key.value and 0x07
            index = key.nextIndex

            if (fn == targetField && wt == 0) {
                // 找到 msgSeq 字段，将其值改为 1
                val oldValue = readVarInt(bytes, index) ?: return null
                val output = ByteArrayOutputStream(bytes.size)
                output.write(bytes, 0, index)
                writeVarInt(output, 1) // 新的 msgSeq = 1
                output.write(bytes, oldValue.nextIndex, bytes.size - oldValue.nextIndex)
                return output.toByteArray()
            }

            index = when (wt) {
                0 -> readVarInt(bytes, index)?.nextIndex ?: break
                1 -> (index + 8).coerceAtMost(bytes.size)
                2 -> {
                    val l = readVarInt(bytes, index) ?: break
                    l.nextIndex + l.value
                }
                5 -> (index + 4).coerceAtMost(bytes.size)
                else -> break
            }
        }
        return null
    }

    // ==================== AIO 消息视图 Hook（已撤回提示） ====================

    private var aioViewHookDone = false

    /**
     * Hook AIO 消息视图更新，用于在撤回消息上添加「已撤回」提示标签
     */
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
            ) { param -> onAIOMsgUpdate(param) }
            aioViewHookDone = true
        } catch (e: Exception) {
            LogUtils.e(TAG, "hookAIOMsgUpdate failed: ${e.message}")
        }
    }

    /**
     * AIO 消息视图更新回调：检查是否为已撤回消息，添加提示标签
     */
    private fun onAIOMsgUpdate(param: XC_MethodHook.MethodHookParam) {
        try {
            if (!isEnabled()) return
            if (recallMetaMap.isEmpty()) return

            val thisObject = param.thisObject

            // 获取消息视图：优先用 e 字段（实际内容视图），备选 getHostView()
            val itemView = getVBView(thisObject) ?: return
            val rootView = itemView as? ViewGroup ?: return

            // 仅在 adapter 未缓存时尝试捕获（避免每次消息更新都遍历视图树）
            if (currentAIOAdapter?.get() == null) {
                captureAIOAdapter(rootView)
            }

            // 获取 AIOMsgItem 字段
            val aioMsgItem = findFirstFieldOfType(
                thisObject,
                "com.tencent.mobileqq.aio.msg.AIOMsgItem"
            )
            if (aioMsgItem == null) {
                return
            }

            // 获取 msgRecord
            val msgRecord = ReflectUtils.callMethod(aioMsgItem, "getMsgRecord") ?: return

            // 获取 peerUid 和 msgSeq
            val peerUinLong = ReflectUtils.callMethod(msgRecord, "getPeerUin") as? Long
            val msgSeqLong = ReflectUtils.callMethod(msgRecord, "getMsgSeq") as? Long
            if (peerUinLong == null || msgSeqLong == null) return
            val peerUin = peerUinLong.toString()
            val msgSeq = msgSeqLong.toInt()

            val lookupKey = "${peerUin}_$msgSeq"
            val meta = recallMetaMap[lookupKey] ?: return

            // 防止重复添加
            if (rootView.findViewById<View?>(RECALL_PROMPT_VIEW_ID) != null) return

            // 添加「已撤回」提示标签
            addRecallPromptView(rootView)
        } catch (e: Exception) {
            LogUtils.e(TAG, "onAIOMsgUpdate: error - ${e.message}")
        }
    }

    /**
     * 从 AIOBubbleMsgItemVB 获取视图，优先使用 e 字段
     */
    private fun getVBView(thisObject: Any): View? {
        // 策略1：反射获取 e 字段（private final android.view.View）
        try {
            val field = thisObject.javaClass.getDeclaredField("e")
            field.isAccessible = true
            val view = field.get(thisObject) as? View
            if (view != null) return view
        } catch (_: Exception) {}

        // 策略2：调用 getHostView()
        try {
            return ReflectUtils.callMethod(thisObject, "getHostView") as? View
        } catch (_: Exception) {}

        return null
    }

    /**
     * 捕获当前 AIO 的 RecyclerView Adapter 引用
     */
    private fun captureAIOAdapter(view: View) {
        try {
            val hadAdapter = currentAIOAdapter?.get() != null

            // 策略1：从当前 view 向上遍历父链
            if (findRecyclerViewInParentChain(view)) {
                if (!hadAdapter) triggerAIORefresh()
                return
            }

            // 策略2：view 可能还没 attach，尝试从 Activity 的 decorView 中查找
            val activity = tryGetActivity(view)
            if (activity != null) {
                try {
                    val window = ReflectUtils.callMethod(activity, "getWindow")
                    if (window != null) {
                        val decorView = ReflectUtils.callMethod(window, "getDecorView") as? ViewGroup
                        if (decorView != null && findRecyclerViewInTree(decorView, 0)) {
                            if (!hadAdapter) triggerAIORefresh()
                            return
                        }
                    }
                } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            LogUtils.e(TAG, "captureAIOAdapter: error - ${e.message}")
        }
    }

    /**
     * 从 view 向上遍历父链查找 RecyclerView
     */
    private fun findRecyclerViewInParentChain(view: View): Boolean {
        var parent: Any? = view.parent
        var depth = 0
        while (parent != null && depth < 50) {
            val name = parent.javaClass.name
            if (name.contains("RecyclerView")) {
                val adapter = ReflectUtils.callMethod(parent, "getAdapter")
                if (adapter != null) {
                    currentAIOAdapter = WeakReference(adapter)
                    return true
                }
            }
            parent = (parent as? ViewGroup)?.parent
            depth++
        }
        return false
    }

    /**
     * 在 ViewGroup 树中递归查找 RecyclerView
     */
    private fun findRecyclerViewInTree(root: ViewGroup, depth: Int): Boolean {
        if (depth > 30) return false
        for (i in 0 until root.childCount) {
            val child = root.getChildAt(i) ?: continue
            val name = child.javaClass.name
            if (name.contains("RecyclerView")) {
                val adapter = ReflectUtils.callMethod(child, "getAdapter")
                if (adapter != null) {
                    currentAIOAdapter = WeakReference(adapter)
                    return true
                }
            }
            if (child is ViewGroup) {
                if (findRecyclerViewInTree(child, depth + 1)) return true
            }
        }
        return false
    }

    /**
     * 尝试从 view 获取所属的 Activity
     */
    private fun tryGetActivity(view: View): Any? {
        try {
            val visited = HashSet<Any>()
            var ctx: Any? = view.context
            while (ctx != null && visited.add(ctx)) {
                val name = ctx.javaClass.name
                if (name.contains("Activity")) {
                    return ctx
                }
                ctx = try {
                    ReflectUtils.callMethod(ctx, "getBaseContext")
                } catch (_: Exception) { null }
            }
        } catch (_: Exception) {}
        return null
    }

    /**
     * 主动触发 AIO 视图刷新，实现实时显示「已撤回」提示
     */
    private fun triggerAIORefresh() {
        try {
            val adapter = currentAIOAdapter?.get()
            if (adapter == null) {
                return
            }
            ReflectUtils.callMethod(adapter, "notifyDataSetChanged")
        } catch (_: Exception) {}
    }

    /**
     * 在消息根视图中添加「已撤回」提示标签
     */
    private fun addRecallPromptView(rootView: ViewGroup) {
        val context = rootView.context
        val textView = TextView(context).apply {
            text = "消息已撤回"
            textSize = 14f
            setTextColor(0xFF12B7F5.toInt()) // QQ蓝
            gravity = Gravity.CENTER
            id = RECALL_PROMPT_VIEW_ID
            isClickable = false
        }
        rootView.addView(textView, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ))
    }

    /**
     * 在对象中查找第一个指定类型的字段值（含父类）
     */
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
        } catch (e: Exception) {
            // ignore
        }
        return null
    }

    // ==================== VarInt 工具 ====================

    data class ProtoVarInt(val value: Int, val nextIndex: Int)

    private fun readVarInt(bytes: ByteArray, startIndex: Int): ProtoVarInt? {
        var result = 0
        var shift = 0
        var index = startIndex
        while (index < bytes.size && shift < Int.SIZE_BITS) {
            val byte = bytes[index].toInt() and 0xFF
            result = result or ((byte and 0x7F) shl shift)
            index++
            if ((byte and 0x80) == 0) {
                return ProtoVarInt(result, index)
            }
            shift += 7
        }
        return null
    }

    private fun skipVarInt(bytes: ByteArray, startIndex: Int): Int? {
        var index = startIndex
        while (index < bytes.size) {
            if ((bytes[index].toInt() and 0x80) == 0) {
                return index + 1
            }
            index++
        }
        return null
    }

    private fun writeVarInt(output: ByteArrayOutputStream, value: Int) {
        var v = value
        while (v and 0xFFFFFF80.toInt() != 0) {
            output.write((v and 0x7F) or 0x80)
            v = v ushr 7
        }
        output.write(v and 0x7F)
    }

    // ==================== 调试工具 ====================

    private fun ByteArray.toHex(): String {
        return joinToString("") { String.format("%02X", it) }
    }

    private fun dumpProtoFields(bytes: ByteArray): String {
        val sb = StringBuilder()
        var index = 0
        while (index < bytes.size) {
            val key = readVarInt(bytes, index) ?: break
            val fn = key.value ushr 3
            val wt = key.value and 0x07
            index = key.nextIndex
            sb.append("f$fn(w$wt)")
            when (wt) {
                0 -> {
                    val v = readVarInt(bytes, index)
                    if (v != null) {
                        sb.append("=${v.value}")
                        index = v.nextIndex
                    } else {
                        // 大整数溢出，跳过该字段
                        index = skipVarInt(bytes, index) ?: break
                    }
                }
                1 -> {
                    if (index + 8 <= bytes.size) {
                        sb.append("=64bit")
                        index += 8
                    } else break
                }
                2 -> {
                    val l = readVarInt(bytes, index) ?: break
                    sb.append("=${l.value}B")
                    index = l.nextIndex + l.value
                }
                5 -> {
                    if (index + 4 <= bytes.size) {
                        sb.append("=32bit")
                        index += 4
                    } else break
                }
                else -> break
            }
            if (index < bytes.size) sb.append(", ")
        }
        return sb.toString()
    }
}