package me.lengyu.qedge.hook.item

import me.lengyu.qedge.hook.annotation.HookItemAnnotation
import me.lengyu.qedge.hook.base.BaseApiHookItem
import me.lengyu.qedge.hook.base.Listener
import me.lengyu.qedge.utils.HookUtils
import me.lengyu.qedge.utils.HostInfo
import me.lengyu.qedge.utils.LogUtils
import me.lengyu.qedge.utils.ModuleConfig
import me.lengyu.qedge.utils.dexkit.DexKitTask
import org.luckypray.dexkit.query.FindMethod
import org.luckypray.dexkit.query.base.BaseFinder
import org.luckypray.dexkit.query.matchers.MethodMatcher
import java.lang.reflect.Method

/**
 * @Author 冷雨
 * @Description 开启QQ原生下载表情包通道
 *
 * 表情包图片（PicElement）在进入下载链路前会先经过 com.tencent.qqnt.msg 包下的一个判定方法
 * （反编译原型为混淆名 M），特征：
 *   1. 声明类在 com.tencent.qqnt.msg 包下；
 *   2. 唯一参数 = com.tencent.qqnt.kernel.nativeinterface.PicElement；
 *   3. 返回 = boolean；
 *   4. 方法体内调用了 IZplanOffLineApi.isZPlanMsgOffLine。
 * 该方法返回 false 时原生下载通道被拦截，hook 后强制返回 true 即可放行。
 *
 * 类名/方法名都被混淆，不硬编码：DexKit 按「包名 + 参数类型 + 返回类型 + 方法体调用」四重条件
 * 定位唯一的 (PicElement)→boolean 方法，结果由 DexKitCache 缓存（key 见 [KEY_PIC_DOWNLOAD_METHOD]）；
 * 缓存未命中时 requireMethod 抛异常，此处只记录错误日志，不影响其它功能。
 *
 * 钩子无条件安装，开关在回调里每次读取，改动即时生效，无需重启。
 */
@HookItemAnnotation(value = "原生表情包下载", category = "item")
object NativeEmotionDownload : BaseApiHookItem<Listener>(), DexKitTask {

    const val TAG = "NativeEmotionDownload"

    private const val CONFIG_KEY = "native_emotion_download"

    private const val KEY_PIC_DOWNLOAD_METHOD = "pic_download_method"

    /** 目标方法所在包（QQ NT 消息模块，包名未被混淆，用于缩小 DexKit 检索范围） */
    private const val MSG_PACKAGE = "com.tencent.qqnt.msg"

    /** 判定方法唯一参数类型：QQ 内核的图片元素（类名未被混淆） */
    private const val PIC_ELEMENT = "com.tencent.qqnt.kernel.nativeinterface.PicElement"

    /** 方法体内调用的离线判定接口与其中的方法名（QQ 内部 API，未被混淆） */
    private const val ZPLAN_API = "com.tencent.qqnt.msg.api.IZplanOffLineApi"
    private const val ZPLAN_API_METHOD = "isZPlanMsgOffLine"

    private fun isEnabled() = ModuleConfig.getBoolean(CONFIG_KEY, false)

    /** 表情包下载通道是 QQ 原生功能，TIM 不适用 */
    override fun isApplicable(): Boolean = HostInfo.isQQ

    override fun getQueryMap(): Map<String, BaseFinder> = mapOf(
        KEY_PIC_DOWNLOAD_METHOD to FindMethod().apply {
            searchPackages(MSG_PACKAGE)
            matcher {
                returnType("boolean")
                paramTypes(PIC_ELEMENT)
                // 同类下存在多个 (PicElement)→boolean 方法，靠方法体调用了
                // IZplanOffLineApi.isZPlanMsgOffLine 这一特征唯一定位到目标方法
                addInvoke(MethodMatcher().apply {
                    declaredClass(ZPLAN_API)
                    name(ZPLAN_API_METHOD)
                })
            }
        }
    )

    override fun loadHook() {
        if (!HostInfo.isQQ) return

        val picDownloadMethod: Method = try {
            requireMethod(KEY_PIC_DOWNLOAD_METHOD)
        } catch (e: Throwable) {
            LogUtils.e(TAG, "DexKit resolve error: ${e.message}")
            return
        }

        HookUtils.hookAfter(picDownloadMethod) { param ->
            if (!isEnabled()) return@hookAfter
            // 只把判定结果拉成 true，保留原方法自身的副作用
            if (param.result != true) param.result = true
        }
    }
}
