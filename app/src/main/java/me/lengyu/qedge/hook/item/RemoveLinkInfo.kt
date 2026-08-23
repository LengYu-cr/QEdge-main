package me.lengyu.qedge.hook.item

import me.lengyu.qedge.hook.annotation.HookItemAnnotation
import me.lengyu.qedge.hook.base.BaseApiHookItem
import me.lengyu.qedge.hook.base.Listener
import me.lengyu.qedge.utils.HookUtils
import me.lengyu.qedge.utils.LogUtils
import me.lengyu.qedge.utils.ModuleConfig
import me.lengyu.qedge.utils.ReflectUtils
import me.lengyu.qedge.utils.dexkit.DexKitTask
import org.luckypray.dexkit.query.FindMethod
import org.luckypray.dexkit.query.base.BaseFinder
import java.lang.reflect.Method
/**
 * @Author 冷雨
 * @Description 移除链接信息
 */
@HookItemAnnotation(value = "移除链接信息", category = "item")
object RemoveLinkInfo : BaseApiHookItem<Listener>(), DexKitTask {

    const val TAG = "RemoveLinkInfo"

    private const val KEY_EXT_TEXTVIEW_METHOD = "e_method"

    private fun isEnabled() = ModuleConfig.getBoolean("remove_linkinfo", false)

    override fun getQueryMap(): Map<String, BaseFinder> = mapOf(
        KEY_EXT_TEXTVIEW_METHOD to FindMethod().apply {
            searchPackages("com.tencent.mobileqq.aio.msglist.holder.component.text")
            matcher {
                usingStrings("extTextView")
            }
        }
    )

    override fun loadHook() {
        if (!isEnabled()) return

        val extTextViewMethod: Method = try {
            requireMethod(KEY_EXT_TEXTVIEW_METHOD)
        } catch (e: Throwable) {
            LogUtils.e(TAG, "DexKit resolve error: ${e.message}")
            return
        }

        HookUtils.hookBefore(extTextViewMethod) {
            it.args[2] = null
        }
    }
}
