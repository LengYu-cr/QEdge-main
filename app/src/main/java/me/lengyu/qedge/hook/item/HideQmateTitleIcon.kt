package me.lengyu.qedge.hook.item

import android.widget.ImageView
import de.robv.android.xposed.XposedHelpers
import me.lengyu.qedge.hook.annotation.HookItemAnnotation
import me.lengyu.qedge.hook.base.BaseSwitchHookItem
import me.lengyu.qedge.utils.HookUtils
import me.lengyu.qedge.utils.HostInfo
import me.lengyu.qedge.utils.LogUtils
import me.lengyu.qedge.utils.ModuleConfig
import me.lengyu.qedge.utils.ReflectUtils
import me.lengyu.qedge.utils.dexkit.DexKitTask
import org.luckypray.dexkit.query.FindClass
import org.luckypray.dexkit.query.base.BaseFinder

/**
 * @Author 冷雨
 * @Description 隐藏好友单聊标题栏右侧的「合养秋秋人 / 搭子」入口图标（资源 0x7f0a72b1）。
 *
 * 该图标由 QmateEntryIconManager 驱动：
 * - showQmateIcon(Z) 发送 type="4" 的 UpdateRight3IvEvent，并附带 buildViewInitCallback$1；
 * - AIOTitleVB 处理该事件时，即使 visiable=false，只要 clickAction 非空仍会执行它；
 * - 回调内部调用 applyEntryIconResultToImageView(ImageView, entry) 直接 setImageDrawable + setVisibility(VISIBLE)。
 * 因此仅修改 Right3IvData 的 visiable 无效，必须拦截 QmateEntryIconManager 自身。
 *
 * Right3IvData 的类名和方法名都是混淆的（如 com.tencent.mobileqq.aio.title.am），
 * 这里统一用 DexKit 按稳定字符串定位类，再用反射签名匹配混淆方法，避免版本升级后失效。
 */
@HookItemAnnotation(
    value = "隐藏合养秋秋人入口",
    category = "item",
    tag = "隐藏合养秋秋人入口",
    desc = "隐藏好友单聊标题栏右侧的「合养秋秋人」图标",
    // 标题栏只在宿主主进程渲染，子进程装了钩子也永远不会触发
    process = "Main"
)
object HideQmateTitleIcon : BaseSwitchHookItem(), DexKitTask {

    private const val TAG = "HideQmateTitleIcon"
    private const val KEY_ENABLE = "hide_qmate_title_icon"
    private const val KEY_QMATE_MANAGER = "qmate_manager"
    private const val KEY_RIGHT3_DATA = "right3_iv_data"

    private const val QMATE_PACKAGE = "com.tencent.mobileqq.qmate"
    private const val RIGHT3_PACKAGE = "com.tencent.mobileqq.aio.title"

    /** QmateEntryIconManager 类名未混淆，DexKit 未命中时用它兜底 */
    private const val QMATE_MANAGER_CLASS = "com.tencent.mobileqq.qmate.aio.QmateEntryIconManager"
    private const val QMATE_TYPE = "4"

    /** 合养秋秋人入口从 QQ 9.3.30 开始才有，低版本不安装钩子。 */
    private const val MIN_MAJOR = 9
    private const val MIN_MINOR = 3
    private const val MIN_PATCH = 70

    private fun isEnabled() = ModuleConfig.getBoolean(KEY_ENABLE, false)

    /**
     * 开关只在启动装钩子时快照一次（UI 也提示「重启 QQ 后生效」）。
     * Right3IvData 构造是高频路径，回调里读配置会做多次 stat，必须避免。
     */
    @Volatile
    private var enabled = false

    /** 当前宿主是否为 QQ 9.3.30 及以上（UI 侧也复用该判断）。 */
    fun isSupported(): Boolean {
        if (!HostInfo.isQQ) return false
        val parts = HostInfo.versionName.split(".")
        val major = parts.getOrNull(0)?.toIntOrNull() ?: return false
        val minor = parts.getOrNull(1)?.toIntOrNull() ?: return false
        val patch = parts.getOrNull(2)?.filter { it.isDigit() }?.toIntOrNull() ?: return false
        return when {
            major != MIN_MAJOR -> major > MIN_MAJOR
            minor != MIN_MINOR -> minor > MIN_MINOR
            else -> patch >= MIN_PATCH
        }
    }

    /** 低版本没有该功能，不执行 DexKit 查询，避免缓存里留下无法解析的 key。 */
    override fun isApplicable(): Boolean = isSupported()

    override fun getQueryMap(): Map<String, BaseFinder> = mapOf(
        // 类名本身未混淆，用 showQmateIcon 的日志字符串精确命中
        KEY_QMATE_MANAGER to FindClass().apply {
            searchPackages(QMATE_PACKAGE)
            matcher {
                usingStrings(
                    "showQmateIcon: peer is not friend, hide entry",
                    "showQmateIcon: Qmate switch off, override to LocalIcon(qui_qmate) peerUin="
                )
            }
        },
        // Right3IvData 是 Kotlin data class，toString 里的字符串可作为稳定锚点
        KEY_RIGHT3_DATA to FindClass().apply {
            searchPackages(RIGHT3_PACKAGE)
            matcher {
                usingStrings(
                    "Right3IvData(showRedDot=",
                    ", visiable=",
                    ", isSelectedMode="
                )
            }
        }
    )

    override fun onInit(): Boolean {
        if (!isSupported()) {
            LogUtils.i(TAG, "QQ ${HostInfo.versionName} 低于 9.3.30，跳过")
            return false
        }
        return true
    }

    override fun onHook() {
        enabled = isEnabled()
        if (!enabled) return
        hookQmateEntryIconManager()
        hookRight3IvData()
    }

    /**
     * 从源头拦截 Qmate 入口：不发送事件、不操作 right3Iv。
     * 类内方法名被混淆，按签名唯一定位：
     * - showQmateIcon：(Z)V，类中唯一；
     * - applyEntryIconResultToImageView：(ImageView, entry)V，类中唯一。
     */
    private fun hookQmateEntryIconManager() {
        try {
            val clazz = resolveQmateManager() ?: run {
                LogUtils.e(TAG, "QmateEntryIconManager not found")
                return
            }
            val booleanType = Boolean::class.javaPrimitiveType
            val showMethods = clazz.declaredMethods.filter {
                !it.isSynthetic && !it.isBridge &&
                    it.returnType == Void.TYPE &&
                    it.parameterTypes.size == 1 &&
                    it.parameterTypes[0] == booleanType
            }
            val applyMethods = clazz.declaredMethods.filter {
                !it.isSynthetic && !it.isBridge &&
                    it.returnType == Void.TYPE &&
                    it.parameterTypes.size == 2 &&
                    it.parameterTypes[0] == ImageView::class.java
            }
            if (showMethods.isEmpty() && applyMethods.isEmpty()) {
                LogUtils.e(TAG, "QmateEntryIconManager 方法签名未匹配")
                return
            }
            showMethods.forEach { method ->
                HookUtils.hookBefore(method) { param ->
                    if (!enabled) return@hookBefore
                    param.result = null
                }
            }
            applyMethods.forEach { method ->
                HookUtils.hookBefore(method) { param ->
                    if (!enabled) return@hookBefore
                    param.result = null
                }
            }
        } catch (e: Throwable) {
            LogUtils.e(TAG, "hookQmateEntryIconManager: ${e.message}")
        }
    }

    private fun resolveQmateManager(): Class<*>? {
        return try {
            requireClass(KEY_QMATE_MANAGER)
        } catch (e: Throwable) {
            LogUtils.e(TAG, "DexKit 未定位 QmateEntryIconManager：${e.message}，回退类名")
            XposedHelpers.findClassIfExists(QMATE_MANAGER_CLASS, ReflectUtils.hostClassLoader)
        }
    }

    /**
     * 兜底：把 type="4" 的 Right3IvData 置为不可见。
     */
    private fun hookRight3IvData() {
        try {
            val clazz = requireClass(KEY_RIGHT3_DATA)
            // (showRedDot, visiable, src, type, isSelectedMode, clickAction)
            HookUtils.hookAllConstructors(
                clazz,
                { param ->
                    if (!enabled) return@hookAllConstructors
                    val args = param.args ?: return@hookAllConstructors
                    if (args.size < 4) return@hookAllConstructors
                    if (args[3] != QMATE_TYPE) return@hookAllConstructors
                    args[1] = false
                },
                null
            )
        } catch (e: Throwable) {
            LogUtils.e(TAG, "hookRight3IvData: ${e.message}")
        }
    }
}
