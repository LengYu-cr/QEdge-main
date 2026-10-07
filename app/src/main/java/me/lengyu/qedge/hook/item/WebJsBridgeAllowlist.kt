package me.lengyu.qedge.hook.item

import me.lengyu.qedge.hook.annotation.HookItemAnnotation
import me.lengyu.qedge.hook.base.BaseSwitchHookItem
import me.lengyu.qedge.utils.HookUtils
import me.lengyu.qedge.utils.LogUtils
import me.lengyu.qedge.utils.ModuleConfig
import me.lengyu.qedge.utils.ReflectUtils
import java.lang.reflect.Modifier

/**
 * @Author 冷雨
 * @Description QQ 内置浏览器 JS 接口放行：用户自定义域名集合，允许这些域名通过 JS 调用 QQ 的 mqq 及内部接口
 */
@HookItemAnnotation(
    value = "浏览器JS接口放行",
    category = "item",
    tag = "浏览器JS接口放行",
    desc = "允许自定义域名使用JS调用QQ的mqq和内部接口"
)
object WebJsBridgeAllowlist : BaseSwitchHookItem() {

    private const val TAG = "WebJsBridgeAllowlist"
    private const val KEY_ENABLE = "web_js_allowlist_enable"
    private const val KEY_RULES = "web_js_allowlist_rules"
    private const val TARGET_CLASS = "com.tencent.biz.AuthorizeConfig"

    override fun onInit(): Boolean = true

    override fun onHook() {
        try {
            val clazz = ReflectUtils.findClassIfExists(TARGET_CLASS) ?: run {
                LogUtils.e(TAG, "未找到 $TARGET_CLASS")
                return
            }
            // AuthorizeConfig 下有两个按域名放行的闸口，均不硬编码方法名、按参数签名匹配：
            //  1) 命令闸口 (String pageUrl, String cmd, boolean def) -> boolean
            //     管 JS Bridge 命令（mqq.invoke / jsbridge:// 派发前调用）
            //  2) scheme 闸口 (String pageUrl, String scheme) -> boolean（非静态）
            //     管自定义 scheme 跳转：WebViewPluginEngine.C 会在此处询问是否放行，
            //     通过后才会把 mqqapi:// / mqqopensdkapi:// 交给插件（JumpActivity）跳转。
            //     静态的 C(String,String)Z 是域名 pattern 匹配器，必须排除，否则会破坏匹配逻辑。
            val commandGate = clazz.declaredMethods.filter { m ->
                m.returnType == java.lang.Boolean.TYPE &&
                    m.parameterTypes.size == 3 &&
                    m.parameterTypes[0] == String::class.java &&
                    m.parameterTypes[1] == String::class.java &&
                    m.parameterTypes[2] == java.lang.Boolean.TYPE
            }
            val schemeGate = clazz.declaredMethods.filter { m ->
                m.returnType == java.lang.Boolean.TYPE &&
                    !Modifier.isStatic(m.modifiers) &&
                    m.parameterTypes.size == 2 &&
                    m.parameterTypes[0] == String::class.java &&
                    m.parameterTypes[1] == String::class.java
            }
            val targets = (commandGate + schemeGate).distinct()
            if (targets.isEmpty()) {
                LogUtils.e(TAG, "未匹配到可用的域名闸口方法")
                return
            }
            targets.forEach { method ->
                method.isAccessible = true
                HookUtils.hookReplace(method) { param ->
                    // 两个闸口的第一个参数都是页面(当前) URL
                    val domain = param.args?.getOrNull(0) as? String
                    if (isAllowlisted(domain)) {
                        LogUtils.d(TAG, "放行 " + method.name + " args=" + safeArgs(param.args))
                        true
                    } else {
                        val result = HookUtils.invokeOriginalMethod(param)
                        // 未命中规则也记录，便于按其他页面的日志推断参数格式
                        LogUtils.d(TAG, "原逻辑 " + method.name + " args=" + safeArgs(param.args) + " result=" + result)
                        result
                    }
                }
            }
        } catch (e: Throwable) {
            LogUtils.e(TAG, "onHook failed: $e")
        }
    }

    private fun safeArgs(args: Array<Any?>?): String =
        args?.joinToString(", ") { it?.toString() ?: "null" } ?: "null"

    /** 第一个参数是域名；开关未开、无规则或域名未命中时返回 false，交还原逻辑 */
    private fun isAllowlisted(domain: String?): Boolean {
        if (domain.isNullOrBlank()) return false
        if (!ModuleConfig.getBoolean(KEY_ENABLE, false)) return false
        val rules = parseRules(ModuleConfig.getString(KEY_RULES, ""))
        if (rules.isEmpty()) return false
        return matchRules(domain, rules)
    }

    private fun parseRules(raw: String): List<String> =
        raw.split(',')
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }

    private fun matchRules(value: String, rules: List<String>): Boolean {
        val v = value.trim().lowercase()
        if (v.isEmpty()) return false
        // 参数可能是完整 URL，先取 host 再比较
        val host = if (v.contains("://")) runCatching { java.net.URI(v).host }.getOrNull() ?: v else v
        return rules.any { r -> host == r || host.endsWith(".$r") }
    }
}
