package me.lengyu.qedge.hook.item

import me.lengyu.qedge.hook.annotation.HookItemAnnotation
import me.lengyu.qedge.hook.base.BaseSwitchHookItem
import me.lengyu.qedge.utils.HookUtils
import me.lengyu.qedge.utils.HostInfo
import me.lengyu.qedge.utils.LogUtils
import me.lengyu.qedge.utils.ModuleConfig
import me.lengyu.qedge.utils.ReflectUtils
import me.lengyu.qedge.utils.dexkit.DexKitTask
import org.luckypray.dexkit.query.FindMethod
import org.luckypray.dexkit.query.base.BaseFinder
import org.luckypray.dexkit.query.matchers.MethodMatcher
import java.lang.reflect.Method

/**
 * @Author 冷雨
 * @Description 隐藏聊天页里「对方更换了气泡/背景等装扮」的豆腐块提示。
 *
 * 装扮提示是一条 TofuMsgItem，由 AIO 消息列表的过滤器
 * com.tencent.mobileqq.aio.msglist.msgprocessor.g#a(List) 决定去留。
 * 这里不还原对方的气泡/字体，只在过滤方法返回后从结果列表里移除装扮豆腐块：
 * - 实测装扮提示为 beancurstyle = 7（MultiImageBeancurdViewNew）、busiid = 17，
 *   跳转链接指向 vip.qq.com 的 aioDressPage；
 * - beancurstyle = 8 是 DressShowBeancurdView（装扮展示位）；
 * - 服务端若调整 busiid，再按卡片文案里的装扮关键词兜底。
 *
 * 过滤方法的类名/方法名都被混淆，用 DexKit 按「包名 + 入参/返回都是 List +
 * 方法体内调用了 IAIOTofuApi.isTheTofuNeedFiltered」唯一定位，不硬编码混淆名。
 * TofuMsgItem 的样式/busiid 访问器同样在运行时按签名解析：
 * - 样式：无参、返回 int、非 getViewType；
 * - busiid：无参、返回 long。
 */
@HookItemAnnotation(
    value = "隐藏好友装扮提示",
    category = "item",
    tag = "隐藏好友装扮提示",
    desc = "聊天页不再显示对方更换气泡/背景等装扮的豆腐块提示",
    process = "Main"
)
object HideDressBeancurd : BaseSwitchHookItem(), DexKitTask {

    private const val TAG = "HideDressBeancurd"

    private const val CONFIG_KEY = "hide_dress_beancurd"

    private const val KEY_TOFU_FILTER_METHOD = "tofu_filter_method"

    /** 过滤器所在包（AIO 消息列表处理器，包名未被混淆） */
    private const val MSG_PROCESSOR_PACKAGE = "com.tencent.mobileqq.aio.msglist.msgprocessor"

    /** 过滤方法体内调用的稳定接口，用来唯一定位混淆方法 */
    private const val TOFU_API = "com.tencent.qqnt.aio.adapter.api.IAIOTofuApi"
    private const val TOFU_API_METHOD = "isTheTofuNeedFiltered"

    /** 豆腐块消息载体，类名未被混淆 */
    private const val TOFU_MSG_ITEM_CLASS = "com.tencent.mobileqq.aio.msg.TofuMsgItem"

    /** 豆腐块文本内容类，类名未被混淆 */
    private const val TOFU_CONTENT_CLASS = "com.tencent.qqnt.kernel.nativeinterface.TofuContent"

    private const val VIEW_TYPE_METHOD = "getViewType"

    /** 装扮展示位（DressShowBeancurdView）的样式值，与 AIOTofuContentComponent 的 switch 一致 */
    private const val DRESS_SHOW_STYLE = 8

    /** 好友装扮提示的 busiid，服务端下发的协议字段（实测为 17） */
    private const val DRESS_BUSI_ID = 17L

    /** 装扮提示卡片文案特征，服务端措辞可能变化，命中任意一个即当作装扮 */
    private val DRESS_KEYWORDS = arrayOf("装扮", "气泡", "字体", "背景")

    private fun isEnabled() = ModuleConfig.getBoolean(CONFIG_KEY, false)

    override fun isApplicable(): Boolean = HostInfo.isQQ

    override fun getQueryMap(): Map<String, BaseFinder> = mapOf(
        KEY_TOFU_FILTER_METHOD to FindMethod().apply {
            searchPackages(MSG_PROCESSOR_PACKAGE)
            matcher {
                returnType("java.util.List")
                paramTypes("java.util.List")
                // 全包内唯一调用 isTheTofuNeedFiltered 的方法就是豆腐块过滤器
                addInvoke(MethodMatcher().apply {
                    declaredClass(TOFU_API)
                    name(TOFU_API_METHOD)
                })
            }
        }
    )

    override fun onInit(): Boolean = HostInfo.isQQ

    /**
     * 钩子无条件安装，开关在回调里判断，改动即时生效。
     */
    override fun onHook() {
        val filterMethod: Method = try {
            requireMethod(KEY_TOFU_FILTER_METHOD)
        } catch (e: Throwable) {
            LogUtils.e(TAG, "DexKit resolve error: ${e.message}")
            return
        }

        val tofuClass = ReflectUtils.findClassIfExists(TOFU_MSG_ITEM_CLASS)
        if (tofuClass == null) {
            LogUtils.e(TAG, "$TOFU_MSG_ITEM_CLASS not found")
            return
        }

        // 样式：无参 int；busiid：无参 long。两者都排除 getViewType（常量 viewType）
        val styleMethod = findUniqueMethod(tofuClass, Int::class.javaPrimitiveType, VIEW_TYPE_METHOD)
        val busiIdMethod = findUniqueMethod(tofuClass, Long::class.javaPrimitiveType, null)
        if (styleMethod == null || busiIdMethod == null) {
            LogUtils.e(
                TAG,
                "accessor not found: style=$styleMethod busiid=$busiIdMethod"
            )
            return
        }
        styleMethod.isAccessible = true
        busiIdMethod.isAccessible = true

        val textGetters = tofuClass.declaredMethods.filter {
            isNoArgGetter(it) && it.returnType == String::class.java
        }.onEach { it.isAccessible = true }

        val contentGetter = tofuClass.declaredMethods.firstOrNull {
            isNoArgGetter(it) && it.returnType.name == TOFU_CONTENT_CLASS
        }?.also { it.isAccessible = true }

        val listGetters = tofuClass.declaredMethods.filter {
            isNoArgGetter(it) && List::class.java.isAssignableFrom(it.returnType)
        }.onEach { it.isAccessible = true }

        // TofuMsgItem$b 里是 extBufForUI 解析出的扩展信息
        val extGetters = tofuClass.declaredMethods.filter {
            isNoArgGetter(it) && it.returnType.name.startsWith("$TOFU_MSG_ITEM_CLASS\$")
        }.onEach { it.isAccessible = true }

        HookUtils.hookAfter(filterMethod) { param ->
            val enabled = try {
                isEnabled()
            } catch (e: Throwable) {
                LogUtils.e(TAG, "read config failed: ${e.message}")
                false
            }
            if (!enabled) return@hookAfter

            // 原方法返回的是 toMutableList() 生成的 ArrayList，可安全原地删除
            val list = param.result as? MutableList<*> ?: return@hookAfter

            try {
                val iterator = list.iterator()
                while (iterator.hasNext()) {
                    val item: Any = iterator.next() ?: continue
                    if (!tofuClass.isInstance(item)) continue

                    val style = invokeInt(styleMethod, item)
                    val busiId = invokeLong(busiIdMethod, item)
                    val isDress = style == DRESS_SHOW_STYLE ||
                        busiId == DRESS_BUSI_ID ||
                        containsDressKeyword(
                            collectText(
                                item,
                                textGetters,
                                contentGetter,
                                listGetters,
                                extGetters
                            )
                        )
                    if (isDress) {
                        iterator.remove()
                    }
                }
            } catch (e: Throwable) {
                LogUtils.e(TAG, e)
            }
        }
    }

    private fun collectText(
        item: Any,
        textGetters: List<Method>,
        contentGetter: Method?,
        listGetters: List<Method>,
        extGetters: List<Method>
    ): String = buildString {
        for (getter in textGetters) {
            val value = invokeString(getter, item)
            if (!value.isNullOrEmpty()) append(value).append('\n')
        }
        contentGetter?.let { getter ->
            val value = invokeString(getter, item)
            if (!value.isNullOrEmpty()) append(value).append('\n')
        }
        for (getter in listGetters) {
            val value = invokeString(getter, item)
            if (!value.isNullOrEmpty()) append(value).append('\n')
        }
        for (getter in extGetters) {
            val value = invokeString(getter, item)
            if (!value.isNullOrEmpty()) append(value).append('\n')
        }
    }

    private fun containsDressKeyword(text: String): Boolean =
        DRESS_KEYWORDS.any { text.contains(it) }

    private fun invokeInt(method: Method, target: Any): Int = try {
        method.invoke(target) as? Int ?: -1
    } catch (e: Throwable) {
        -1
    }

    private fun invokeLong(method: Method, target: Any): Long = try {
        method.invoke(target) as? Long ?: -1L
    } catch (e: Throwable) {
        -1L
    }

    private fun invokeString(method: Method, target: Any): String? = try {
        method.invoke(target)?.toString()
    } catch (e: Throwable) {
        null
    }

    private fun isNoArgGetter(method: Method): Boolean =
        !method.isSynthetic && !method.isBridge &&
            method.parameterTypes.isEmpty() &&
            method.name != "toString" && method.name != "hashCode"

    /**
     * 按返回类型查找唯一的无参访问器；命中多个说明版本结构变化，放弃安装以避免误判。
     */
    private fun findUniqueMethod(
        clazz: Class<*>,
        returnType: Class<*>?,
        excludeName: String?
    ): Method? {
        val candidates = clazz.declaredMethods.filter { method ->
            !method.isSynthetic && !method.isBridge &&
                method.parameterTypes.isEmpty() &&
                method.returnType == returnType &&
                (excludeName == null || method.name != excludeName)
        }
        if (candidates.size != 1) {
            LogUtils.e(TAG, "unexpected accessor candidates($returnType): ${candidates.map { it.name }}")
            return null
        }
        return candidates.first()
    }
}
