package me.lengyu.qedge.hook.item

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.View
import android.widget.ImageView
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import me.lengyu.qedge.R
import me.lengyu.qedge.hook.annotation.HookItemAnnotation
import me.lengyu.qedge.hook.base.BaseSwitchHookItem
import me.lengyu.qedge.lifecycle.Parasitics
import me.lengyu.qedge.ui.components.dialogs.RepeatMsgAction
import me.lengyu.qedge.ui.components.dialogs.RepeatMsgActionDialog
import me.lengyu.qedge.ui.components.dialogs.RawTextDialog
import me.lengyu.qedge.utils.HookUtils
import me.lengyu.qedge.utils.LogUtils
import me.lengyu.qedge.utils.ModuleConfig
import me.lengyu.qedge.utils.ReflectUtils
import me.lengyu.qedge.utils.Toasts
import me.lengyu.qedge.plugin.bean.MsgData
import me.lengyu.qedge.common.ModuleScope
import me.lengyu.qedge.utils.qq.ExtraTool
import me.lengyu.qedge.utils.qq.MsgTool
import me.lengyu.qedge.utils.qq.QQCurrentEnv
import com.tencent.qqnt.kernel.nativeinterface.MsgElement
import com.tencent.qqnt.kernel.nativeinterface.MsgRecord
import de.robv.android.xposed.XposedBridge
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.ArrayList
/**
 * @Author 冷雨
 * @Description 消息复读
 */
@HookItemAnnotation(value = "消息复读", category = "item", tag = "消息复读", desc = "在消息旁显示复读按钮，点击快速复读消息")
class RepeatMsg : BaseSwitchHookItem() {

    companion object {
        private const val TAG = "RepeatMsg"
        private const val SP_KEY = "repeat_msg"
        private const val ICON_SP_KEY = "repeat_msg_icon"

        @Volatile
        private var cachedTargetMethod: Method? = null
        @Volatile
        private var hasSearched = false

        // 自定义图标按 路径+修改时间 缓存：换图时文件被原地覆盖（路径不变），靠 lastModified 失效
        @Volatile
        private var cachedIconKey: String? = null
        @Volatile
        private var cachedIconBitmap: Bitmap? = null

        /**
         * 读取用户在首页选择的自定义图标（路径存 ModuleConfig）。
         * 未设置、文件不存在或解码失败均返回 null，回退到默认图标。
         * 大图按 256px 采样解码，避免整张照片解进内存（按钮只有几十 dp）。
         */
        fun loadCustomIcon(): Bitmap? {
            val path = ModuleConfig.getString(ICON_SP_KEY, "")
            if (path.isEmpty()) {
                cachedIconKey = null
                cachedIconBitmap = null
                return null
            }

            val file = java.io.File(path)
            if (!file.exists()) return null
            val key = "$path:${file.lastModified()}"
            if (key == cachedIconKey) return cachedIconBitmap

            val bitmap = try {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(path, bounds)
                var sample = 1
                while (bounds.outWidth / sample > 256 || bounds.outHeight / sample > 256) {
                    sample *= 2
                }
                BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
            } catch (_: Throwable) {
                null
            }
            cachedIconKey = key
            cachedIconBitmap = bitmap
            return bitmap
        }

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
            HookUtils.hookAfter(targetMethod) { param ->
                onHandleIntent(param)
            }
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
                // 用户在首页选过图则用自定义图标，否则用默认图标
                val customIcon = loadCustomIcon()
                if (customIcon != null) {
                    setImageBitmap(customIcon)
                } else {
                    setImageResource(R.drawable.repeat)
                }
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

            // 复制路径：视频（异步获取在线播放地址）
            if (msgData.videoList.size > 0) {
                actions.add(
                    RepeatMsgAction("复制链接", "视频(获取在线播放地址)") {
                        try {
                            val richMediaService = QQCurrentEnv.getRichMediaService()
                            if (richMediaService == null) {
                                Toasts.showCustomToast("富媒体服务不可用")
                                return@RepeatMsgAction
                            }

                            val contact = msgData.contact ?: return@RepeatMsgAction
                            val videoElement = msgData.data.elements?.firstOrNull { it.videoElement != null }
                            val filterMsgElement = msgData.data.elements?.firstOrNull { it.filterMsgElement != null }   
                            if (videoElement == null && filterMsgElement == null) {
                                Toasts.showCustomToast("未找到视频元素")
                                return@RepeatMsgAction
                            }
                            val elementId = videoElement?.elementId ?: filterMsgElement?.elementId ?: return@RepeatMsgAction

                            // 关键：枚举/参数/回调类都必须用宿主 ClassLoader 的类。模块 stub 是
                            // compileOnly 不打包，运行时若用模块自己的接口实现类做 callback，宿主内核
                            // 的 JNI 环境无法识别该类，异步回调阶段会在 native 层直接崩溃（进程 abort，
                            // 无 Java 日志可捕获）。故 callback 用宿主接口的动态代理生成。
                            val hostCL = richMediaService.javaClass.classLoader
                            val codecClass = XposedHelpers.findClass(
                                "com.tencent.qqnt.kernel.nativeinterface.VideoCodecFormatType", hostCL
                            )
                            val rmParamsClass = XposedHelpers.findClass(
                                "com.tencent.qqnt.kernel.nativeinterface.RMReqExParams", hostCL
                            )
                            val callbackClass = XposedHelpers.findClass(
                                "com.tencent.qqnt.kernel.nativeinterface.IVideoPlayUrlCallback", hostCL
                            )
                            // 宿主的 VideoCodecFormatType.KCODECFORMATH264 枚举实例
                            val codecH264 = codecClass.enumConstants?.firstOrNull {
                                (it as? Enum<*>)?.name == "KCODECFORMATH264"
                            } ?: codecClass.enumConstants?.get(0)
                            // 用宿主类构造 RMReqExParams(downSourceType=1, triggerType=0)
                            val rMReqExParams = XposedHelpers.newInstance(
                                rmParamsClass,
                                arrayOf<Class<*>>(Integer.TYPE, Integer.TYPE),
                                1, 0
                            )

                            // 用宿主 ClassLoader + 宿主接口生成回调代理，代理类由宿主 CL 定义，
                            // 内核 JNI 可正常识别；结果对象是宿主 VideoPlayUrlResult，通过反射读字段。
                            val callback = Proxy.newProxyInstance(
                                hostCL, arrayOf(callbackClass)
                            ) { _, m, args ->
                                if (m.name == "onResult") {
                                    val code = (args?.getOrNull(0) as? Int) ?: -1
                                    val msg = args?.getOrNull(1) as? String
                                    val result = args?.getOrNull(2)
                                    hostActivity.runOnUiThread {
                                        runCatching {
                                            val url = if (code == 0) extractVideoUrl(result) else null
                                            if (!url.isNullOrEmpty()) {
                                                copyToClipboard(hostActivity, url)
                                                Toasts.showCustomToast("已复制视频地址到剪贴板")
                                            } else {
                                                Toasts.showCustomToast(msg ?: "获取视频地址失败")
                                            }
                                        }.onFailure { LogUtils.e(TAG, "video onResult error: ${it.message}") }
                                    }
                                }
                                // onResult 返回 void
                                null
                            }

                            val method = XposedHelpers.findMethodExact(
                                richMediaService.javaClass, "getVideoPlayUrlV2",
                                contact.javaClass, java.lang.Long.TYPE, java.lang.Long.TYPE,
                                codecClass, rmParamsClass, callbackClass
                            )
                            method.invoke(
                                richMediaService, contact, msgData.msgId, elementId,
                                codecH264, rMReqExParams, callback
                            )
                        } catch (e: Throwable) {
                            LogUtils.e(TAG, "video copy error: ${e.message}")
                            Toasts.showCustomToast("获取视频地址失败: ${e.message}")
                        }
                    }
                )
            }
            if (msgData.pttList.size > 0) {
                actions.add(
                    RepeatMsgAction("复制链接", "语音(获取在线播放地址)") {
                        val pttElement = msgData.data.elements
                            ?.firstOrNull { it.pttElement != null }?.pttElement
                        if (pttElement == null) {
                            Toasts.showCustomToast("未找到语音元素")
                            return@RepeatMsgAction
                        }
                        Toasts.showCustomToast("正在获取语音地址...")
                        // fetchPacket 会阻塞等待回包，必须放到 IO 线程执行
                        ModuleScope.launchIOJava(TAG) {
                            val url = runCatching {
                                // chatType==1 为好友(c2c)，其余按群处理
                                if (msgData.type == 1) {
                                    ExtraTool.getFriendPttUrl(
                                        msgData.peerUid,
                                        pttElement.md5HexStr,
                                        pttElement.fileUuid,
                                        pttElement.fileName,
                                        pttElement.filePath,
                                        msgData.time
                                    )
                                } else {
                                    ExtraTool.getGroupPttUrl(
                                        pttElement.md5HexStr,
                                        pttElement.fileUuid,
                                        pttElement.fileName,
                                        pttElement.fileSize,
                                        pttElement.filePath,
                                        msgData.time
                                    )
                                }
                            }.getOrNull()
                            hostActivity.runOnUiThread {
                                if (!url.isNullOrEmpty()) {
                                    copyToClipboard(hostActivity, url)
                                    Toasts.showCustomToast("已复制语音地址到剪贴板")
                                } else {
                                    Toasts.showCustomToast("获取语音地址失败")
                                }
                            }
                        }
                    }
                )
            }

            // 复制链接：文件消息(msgType==3)，通过 kernel 反查下载直链
            if (msgData.msgType == 3) {
                actions.add(
                    RepeatMsgAction("复制链接", "文件(获取下载链接)") {
                        try {
                            val fileElement = msgData.data.elements
                                ?.firstOrNull { it.fileElement != null }?.fileElement
                            // FileElement 是宿主类，field 已在 MsgData/MsgTool 使用过，可直接访问
                            val fileUuid = fileElement?.fileUuid
                            val fileName = fileElement?.fileName
                            if (fileUuid.isNullOrEmpty()) {
                                Toasts.showCustomToast("未找到文件标识(uuid)")
                                return@RepeatMsgAction
                            }
                            Toasts.showCustomToast("正在获取文件下载链接...")

                            val fileAssistantService = QQCurrentEnv.getFileAssistantService()
                            if (fileAssistantService == null) {
                                Toasts.showCustomToast("文件助手服务不可用")
                                return@RepeatMsgAction
                            }
                            // getFileAssistantService 返回的是 api 层包装类(com.tencent.qqnt.kernel.api.impl.hl)，
                            // 它继承自 BaseService，私有字段 service 持有真正的 native
                            // IKernelFileAssistantService(getFileAssistantModelByUUIDs 等方法在其上)，反射读取。
                            val nativeService = readFieldViaHierarchy(fileAssistantService, "service")
                            if (nativeService == null) {
                                Toasts.showCustomToast("未找到文件助手内核服务")
                                return@RepeatMsgAction
                            }

                            // 枚举/参数/回调类都必须用宿主 ClassLoader 的类。模块 stub 是
                            // compileOnly 不打包，运行时若用模块自己的接口实现类做 callback，宿主内核
                            // 的 JNI 环境无法识别该类，异步回调阶段会在 native 层直接崩溃。故 callback
                            // 用宿主接口的动态代理生成。
                            val hostCL = ReflectUtils.hostClassLoader
                            val faInfoCallbackClass = XposedHelpers.findClass(
                                "com.tencent.qqnt.kernel.nativeinterface.IGetFAInfoCallback", hostCL
                            )

                            val nativeServiceClass = nativeService.javaClass
                            // getFileAssistantModelByUUIDs(ArrayList<String>, IGetFAInfoCallback)
                            val method = nativeServiceClass.declaredMethods.firstOrNull { m ->
                                m.parameterCount == 2 &&
                                m.parameterTypes[0] == ArrayList::class.java &&
                                m.name.contains("getFileAssistantModelByUUIDs")
                            } ?: XposedHelpers.findMethodExact(
                                nativeServiceClass, "getFileAssistantModelByUUIDs",
                                ArrayList::class.java, faInfoCallbackClass
                            )
                            method.isAccessible = true

                            val uuidList = ArrayList<String>().apply { add(fileUuid) }
                            val callback = Proxy.newProxyInstance(
                                hostCL, arrayOf(faInfoCallbackClass)
                            ) { _, m, args ->
                                if (m.name == "onResult") {
                                    val code = (args?.getOrNull(0) as? Int) ?: -1
                                    val msg = args?.getOrNull(1) as? String
                                    val modelList = args?.getOrNull(2) as? List<*>
                                    LogUtils.i(TAG, "file onResult code=$code msg=$msg listSize=${modelList?.size}")
                                    hostActivity.runOnUiThread {
                                        runCatching {
                                            var diag = ""
                                            val url = if (code == 0 && modelList != null) {
                                                modelList.firstNotNullOfOrNull { model ->
                                                    // FileAssistantModel 是宿主类，不能 cast，反射读 extModel.fileUrl
                                                    val extModel = ReflectUtils.getFieldValue(model, "extModel")
                                                    val extModelNull = extModel == null
                                                    val fileUrl = if (extModelNull) null else ReflectUtils.getFieldValue(extModel, "fileUrl") as? String
                                                    diag += ";extNull=$extModelNull;fileUrl=[$fileUrl];remoteFileId=[${ReflectUtils.getFieldValue(model, "remoteFileId")}]"
                                                    fileUrl?.takeIf { it.isNotEmpty() }
                                                }
                                            } else {
                                                diag += ";code=$code"
                                                null
                                            }
                                            LogUtils.i(TAG, "file diag$diag")
                                            if (!url.isNullOrEmpty()) {
                                                copyToClipboard(hostActivity, url)
                                                Toasts.showCustomToast(if (fileName.isNullOrEmpty()) "已复制文件下载链接" else "已复制文件下载链接: $fileName")
                                            } else {
                                                Toasts.showCustomToast(msg ?: "获取文件下载链接失败$diag")
                                            }
                                        }.onFailure { LogUtils.e(TAG, "file onResult error: ${it.message}") }
                                    }
                                }
                                null
                            }

                            method.invoke(nativeService, uuidList, callback)
                        } catch (e: Throwable) {
                            LogUtils.e(TAG, "file copy error: ${e.message}")
                            Toasts.showCustomToast("获取文件下载链接失败: ${e.message}")
                        }
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

    /**
     * 从宿主 VideoPlayUrlResult 中反射取出视频在线地址。
     * result 是宿主 ClassLoader 的类，模块 stub 与之不是同一个 Class，不能直接 cast，
     * 故按字段名反射读取：优先 domainUrl，退回 v4IpUrl / v6IpUrl；再取列表首元素的 url 字段。
     * 字段名为 QQ 内核公开数据类的稳定字段，方法名混淆不影响字段访问。
     */
    private fun extractVideoUrl(result: Any?): String? {
        if (result == null) return null
        try {
            for (field in arrayOf("domainUrl", "v4IpUrl", "v6IpUrl")) {
                val list = ReflectUtils.getFieldValue(result, field) as? List<*> ?: continue
                val first = list.firstOrNull() ?: continue
                val url = ReflectUtils.getFieldValue(first, "url") as? String
                if (!url.isNullOrEmpty()) return url
            }
        } catch (e: Throwable) {
            LogUtils.e(TAG, "extractVideoUrl error: ${e.message}")
        }
        return null
    }




    private fun performRepeat(msgRecord: MsgRecord) {

        val msgType = msgRecord.msgType
        val subMsgType = msgRecord.subMsgType

        val elements = msgRecord.elements
        val refreshedElements = refreshMsgElements(msgType, elements)

        if (refreshedElements != null) {
            if (isNeedForward(msgType, subMsgType)) {
                forwardSend(msgRecord, refreshedElements)
            }else{
                directSend(msgRecord, refreshedElements)
            }
        }
    }

    private fun isNeedForward(msgType: Int, subMsgType: Int): Boolean {
        return msgType == 3 || msgType == 7 || msgType == 19 || msgType == 10 || subMsgType == 3 || subMsgType == 4096 || subMsgType == 2
    }

    private fun refreshMsgElements(msgType: Int, elements: ArrayList<MsgElement>?): ArrayList<MsgElement>? {
        if (elements.isNullOrEmpty()) return elements

        return when (msgType) {
            2 -> {
                elements.forEach { element ->
                    runCatching {
                        element.textElement?.let { 
                            if(it.atType == 1){
                            it.atType = 0 
                            }
                        }
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

    /**
     * 沿继承链向上查找并读取字段值(含父类私有字段)。
     * ReflectUtils.findField 不遍历父类，而 BaseService.service 等私有字段在父类上，
     * 故此处单独实现。
     */
    private fun readFieldViaHierarchy(obj: Any?, fieldName: String): Any? {
        if (obj == null) return null
        var clazz: Class<*>? = obj.javaClass
        while (clazz != null) {
            try {
                val field = clazz.getDeclaredField(fieldName)
                field.isAccessible = true
                return field.get(obj)
            } catch (e: NoSuchFieldException) {
                clazz = clazz.superclass
            } catch (e: Throwable) {
                LogUtils.e(TAG, "readFieldViaHierarchy($fieldName) error: ${e.message}")
                return null
            }
        }
        return null
    }

    private fun isDoubleClick(): Boolean {
        val now = System.currentTimeMillis()
        val time = now - lastClickTime
        lastClickTime = now
        return time < 500
    }
}