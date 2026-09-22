package me.lengyu.qedge.hook.item

import me.lengyu.qedge.hook.annotation.HookItemAnnotation
import me.lengyu.qedge.hook.base.BaseSwitchHookItem
import me.lengyu.qedge.utils.HookUtils
import me.lengyu.qedge.utils.LogUtils
import de.robv.android.xposed.XposedHelpers
import me.lengyu.qedge.utils.ModuleConfig
import me.lengyu.qedge.utils.ReflectUtils

/**
 * @Author 冷雨
 * @Description 去广告：拦截 QQ 主界面顶部横幅(LebaPluginBannerView)等广告数据源，
 * 通过清空广告数据使横幅不展示，避免因隐藏 View 造成的布局异常。
 */
@HookItemAnnotation(
    value = "去广告",
    category = "item",
    tag = "去广告",
    desc = "清理 QQ 主界面顶部横幅广告(banner)及 SSO 广告数据源，界面更干净"
)
object RemoveAds : BaseSwitchHookItem() {

    private const val TAG = "RemoveAds"
    private const val KEY_ENABLE = "remove_ads"

    // 主界面顶部横幅广告 ViewK（Kotlin 源码类名稳定）
    private const val LABA_BANNER_VIEW = "com.tencent.mobileqq.leba.banner.LebaPluginBannerView"

    // SSO 广告加载 Servlet：cmd = "QqAd.getAd"，是所有走 SSO 拉取的广告数据唯一出口
    private const val SSO_LOAD_AD_SERVLET = "com.tencent.ad.common.SSOLoadAdServlet"

    private fun isEnabled() = ModuleConfig.getBoolean(KEY_ENABLE, false)

    override fun onInit(): Boolean = true

    override fun onHook() {
        val classLoader = ReflectUtils.hostClassLoader ?: return
        hookLebaBanner(classLoader)
        hookSsoAdLoad(classLoader)
    }

    /** 主界面顶部横幅：数据为空时会走隐藏分支，安全不崩 */
    private fun hookLebaBanner(classLoader: ClassLoader) {
        try {
            val clazz = Class.forName(LABA_BANNER_VIEW, false, classLoader)
            val method = XposedHelpers.findMethodExact(clazz, "setData", List::class.java)
            HookUtils.hookReplace(method) { param ->
                if (!isEnabled()) return@hookReplace HookUtils.invokeOriginalMethod(param)
                // 传入空列表 -> setData 内部走隐藏分支
                param.args[0] = ArrayList<Any>()
                HookUtils.invokeOriginalMethod(param)
            }
            // LogUtils.i(TAG, "已 hook 主界面顶部横幅: $LABA_BANNER_VIEW")
        } catch (e: Throwable) {
            LogUtils.e(TAG, "hookLebaBanner error: ${e.message}")
        }
    }

    /**
     * SSO 广告加载出口（cmd=QqAd.getAd）：
     * hook SSOLoadAdServlet.onReceive，强制把 FromServiceMsg 置为失败(resultCode!=0)，
     * 使其走 onReceive 的失败分支 notifyObserver(false)，广告数据不再解析/下发，
     * 从而在数据源头拦截所有经 SSO 拉取的广告。方法名混淆，按方法名+参数个数匹配。
     */
    private fun hookSsoAdLoad(classLoader: ClassLoader) {
        try {
            val clazz = Class.forName(SSO_LOAD_AD_SERVLET, false, classLoader)
            val method = clazz.declaredMethods
                .filter { it.name == "onReceive" && it.parameterTypes.size == 2 }
                .firstOrNull()
                ?: return
            HookUtils.hookBefore(method) { param ->
                if (!isEnabled()) return@hookBefore
                val from = param.args?.getOrNull(1) ?: return@hookBefore
                try {
                    // 成功码为 0，置为 1 -> isSuccess()=false -> onReceive 走失败分支
                    XposedHelpers.callMethod(from, "setResultCode", 1)
                } catch (_: Throwable) {
                }
            }
        } catch (e: Throwable) {
            LogUtils.e(TAG, "hookSsoAdLoad error: ${e.message}")
        }
    }
}