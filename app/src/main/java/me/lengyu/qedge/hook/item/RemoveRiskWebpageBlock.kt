package me.lengyu.qedge.hook.item

import com.tencent.biz.pubaccount.CustomWebView
import com.tencent.smtt.sdk.WebView
import me.lengyu.qedge.hook.annotation.HookItemAnnotation
import me.lengyu.qedge.hook.base.BaseSwitchHookItem
import me.lengyu.qedge.utils.HookUtils
import me.lengyu.qedge.utils.LogUtils
import me.lengyu.qedge.utils.ModuleConfig
/**
 * @Author 冷雨
 * @Description 解除风险网页拦截：点消息链接时不再被 c.pc.qq.com 风险页拦截
 */
@HookItemAnnotation(
    value = "解除风险网页拦截",
    category = "item",
    tag = "解除风险网页拦截",
    desc = "点击消息中链接时不再拦截风险网页"
)
object RemoveRiskWebpageBlock : BaseSwitchHookItem() {

    private const val TAG = "RemoveRiskWebpageBlock"
    private const val KEY_ENABLE = "remove_risk_webpage"
    private const val BLOCK_URL = "c.pc.qq.com"

    private var targetUrl: String? = null
    private var ready = false

    override fun onInit(): Boolean = true

    override fun onHook() {
        hookLoadUrl(CustomWebView::class.java)
        hookLoadUrl(WebView::class.java)
    }

    private fun hookLoadUrl(clazz: Class<*>) {
        try {
            val method = clazz.getDeclaredMethod("loadUrl", String::class.java)
            HookUtils.hookReplace(method) { param ->
                if (!ModuleConfig.getBoolean(KEY_ENABLE, false)) {
                    return@hookReplace HookUtils.invokeOriginalMethod(param)
                }
                val url = param.args[0] as String
                if (url.contains(BLOCK_URL)) {
                    val real = targetUrl
                    if (!real.isNullOrEmpty()) {
                        param.args[0] = real
                        if (!ready) {
                            ready = true
                        }
                        return@hookReplace HookUtils.invokeOriginalMethod(param)
                    }
                    return@hookReplace HookUtils.invokeOriginalMethod(param)
                } else {
                    targetUrl = url
                    return@hookReplace HookUtils.invokeOriginalMethod(param)
                }
            }
        } catch (e: Throwable) {
            LogUtils.e(TAG, "hook error: ${e.message}")
        }
    }
}