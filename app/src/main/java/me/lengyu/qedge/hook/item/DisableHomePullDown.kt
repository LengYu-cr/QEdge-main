package me.lengyu.qedge.hook.item

import de.robv.android.xposed.XposedHelpers
import me.lengyu.qedge.hook.annotation.HookItemAnnotation
import me.lengyu.qedge.hook.base.BaseSwitchHookItem
import me.lengyu.qedge.utils.HookUtils
import me.lengyu.qedge.utils.HostInfo
import me.lengyu.qedge.utils.LogUtils
import me.lengyu.qedge.utils.ModuleConfig
import me.lengyu.qedge.utils.ReflectUtils
import java.lang.reflect.Method

/**
 * @Author 冷雨
 * @Description 关掉首页消息列表的「长距离下拉呼出小程序面板」，保留短距离下拉刷新。
 *
 * 真实链路：
 * 1. 消息列表的 SmartRefreshLayout 使用 TwoLevelHeader（QQ 定制版）作为刷新头，
 *    下拉时核心回调 TwoLevelHeader.onMoving(boolean, float percent, int, int, int)。
 *    当 percent 达到二楼阈值（1.9f）时，头部把状态切到 ReleaseToTwoLevel，
 *    松手后进入 TwoLevel，再由 Conversation 的刷新监听回调
 *    MiniAppPullInterface.onHeaderShowProgress(float) 驱动小程序面板滑入，
 *    最终 MiniAppExpDesktopPartHost 打开小程序桌面。
 * 2. 因此这里做两件事：
 *    - 把 onMoving 的 percent 钳在 1.0f 以内（普通刷新阈值），状态机永远不会
 *      进入 ReleaseToTwoLevel / TwoLevel，长拉只会变成普通刷新；
 *    - 把 onHeaderShowProgress 的进度钳成 0，即使面板容器被置为可见，
 *      小程序内容也不会滑入。
 *
 * 类名均为未混淆的稳定类名；混淆的方法按「返回 void + 参数签名」在运行时动态定位，
 * 不硬编码混淆方法名。
 */
@HookItemAnnotation(
    value = "禁用首页下拉小程序",
    category = "item",
    tag = "禁用首页下拉小程序",
    desc = "保留短距离下拉刷新，长距离下拉不再呼出小程序面板",
    // 首页只在宿主主进程渲染
    process = "Main"
)
object DisableHomePullDown : BaseSwitchHookItem() {

    private const val TAG = "DisableHomePullDown"
    private const val KEY_ENABLE = "disable_home_pull_down"

    /** 消息列表刷新头，类名未混淆 */
    private const val TWO_LEVEL_HEADER_CLASS =
        "com.qqnt.widget.smartrefreshlayout.header.TwoLevelHeader"

    /** 首页小程序桌面宿主，类名未混淆 */
    private const val MINI_APP_DESKTOP_HOST_CLASS =
        "com.tencent.mobileqq.mini.entry.expDesktop.part.MiniAppExpDesktopPartHost"

    /**
     * 普通刷新的百分比阈值。TwoLevelHeader 内部的二楼阈值为 1.9f（mTwoLevelThreshold），
     * 钳在 1.0f 可保证状态机永远不满足进入二楼的条件。
     */
    private const val MAX_REFRESH_PERCENT = 1.0f

    private fun isEnabled() = ModuleConfig.getBoolean(KEY_ENABLE, false)

    /** 开关只在启动装钩子时快照一次，回调里不再读配置 */
    @Volatile
    private var enabled = false

    /** 下拉小程序面板是 QQ 专属，TIM 没有该面板 */
    override fun onInit(): Boolean = HostInfo.isQQ

    override fun onHook() {
        enabled = isEnabled()
        if (!enabled) return
        try {
            hookTwoLevelHeader()
            hookMiniAppDesktopProgress()
        } catch (e: Throwable) {
            LogUtils.e(TAG, "onHook: ${e.message}")
        }
    }

    /**
     * 定位 TwoLevelHeader.onMoving(ZFIII)V，把 percent 钳在普通刷新阈值内。
     */
    private fun hookTwoLevelHeader() {
        val headerClass = XposedHelpers.findClassIfExists(
            TWO_LEVEL_HEADER_CLASS, ReflectUtils.hostClassLoader
        )
        if (headerClass == null) {
            LogUtils.e(TAG, "TwoLevelHeader not found")
            return
        }
        val onMoving = findOnMovingMethod(headerClass)
        if (onMoving == null) {
            LogUtils.e(TAG, "TwoLevelHeader.onMoving 未找到")
            return
        }
        HookUtils.hookBefore(onMoving) { param ->
            if (!enabled) return@hookBefore
            val args = param.args ?: return@hookBefore
            val percent = args.getOrNull(1) as? Float ?: return@hookBefore
            if (percent > MAX_REFRESH_PERCENT) args[1] = MAX_REFRESH_PERCENT
        }
    }

    /**
     * 定位 MiniAppExpDesktopPartHost.onHeaderShowProgress(F)V，把面板进度钳成 0。
     */
    private fun hookMiniAppDesktopProgress() {
        val hostClass = XposedHelpers.findClassIfExists(
            MINI_APP_DESKTOP_HOST_CLASS, ReflectUtils.hostClassLoader
        )
        if (hostClass == null) {
            LogUtils.e(TAG, "MiniAppExpDesktopPartHost not found")
            return
        }
        val showProgress = findShowProgressMethod(hostClass)
        if (showProgress == null) {
            LogUtils.e(TAG, "onHeaderShowProgress 未找到")
            return
        }
        HookUtils.hookBefore(showProgress) { param ->
            if (!enabled) return@hookBefore
            param.args?.set(0, 0f)
        }
    }

    /**
     * 定位 onMoving：类中唯一的「返回 void、参数 (boolean, float, int, int, int)」方法。
     */
    private fun findOnMovingMethod(headerClass: Class<*>): Method? {
        val booleanType = Boolean::class.javaPrimitiveType ?: return null
        val floatType = Float::class.javaPrimitiveType ?: return null
        val intType = Int::class.javaPrimitiveType ?: return null
        return headerClass.declaredMethods.firstOrNull { method ->
            method.returnType == Void.TYPE &&
                method.parameterTypes.size == 5 &&
                method.parameterTypes[0] == booleanType &&
                method.parameterTypes[1] == floatType &&
                method.parameterTypes[2] == intType &&
                method.parameterTypes[3] == intType &&
                method.parameterTypes[4] == intType
        }
    }

    /**
     * 定位 onHeaderShowProgress：优先按方法名，方法名被混淆时退回
     * 「返回 void、参数 (float)」的唯一方法。
     */
    private fun findShowProgressMethod(hostClass: Class<*>): Method? {
        val floatType = Float::class.javaPrimitiveType ?: return null
        val candidates = hostClass.declaredMethods.filter { method ->
            method.returnType == Void.TYPE &&
                method.parameterTypes.size == 1 &&
                method.parameterTypes[0] == floatType
        }
        return candidates.firstOrNull { it.name == "onHeaderShowProgress" }
            ?: candidates.singleOrNull()
    }
}
