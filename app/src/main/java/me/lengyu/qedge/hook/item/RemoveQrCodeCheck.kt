package me.lengyu.qedge.hook.item

import android.os.Bundle
import de.robv.android.xposed.XposedHelpers
import me.lengyu.qedge.hook.annotation.HookItemAnnotation
import me.lengyu.qedge.hook.base.BaseSwitchHookItem
import me.lengyu.qedge.utils.HookUtils
import me.lengyu.qedge.utils.LogUtils
import me.lengyu.qedge.utils.ModuleConfig
import me.lengyu.qedge.utils.ReflectUtils
import java.lang.reflect.Method

/**
 * @Author 冷雨
 * @Description 解除扫码限制：hook QrAgentLoginManager 的扫码风险检查方法，
 * 把首个 boolean 参数改写为 false，解除长按识别/相册扫码时的风险校验。
 * 方法名混淆，按返回类型 void + 参数组合匹配（包名与结构稳定，无需 dexkit）。
 */
@HookItemAnnotation(value = "解除扫码限制", category = "item", tag = "去扫码限制", desc = "解除长按识别或从相册中扫描二维码时的风险检查")
object RemoveQrCodeCheck : BaseSwitchHookItem() {

    private const val TAG = "RemoveQrCodeCheck"
    private const val KEY_ENABLE = "remove_qrcode_check"
    private const val MANAGER_CLASS = "com.tencent.open.agent.QrAgentLoginManager"

    private var check: Method? = null

    private fun isEnabled() = ModuleConfig.getBoolean(KEY_ENABLE, false)

    override fun onInit(): Boolean = true

    override fun onHook() {
        val classLoader = ReflectUtils.hostClassLoader ?: return
        val manager = XposedHelpers.findClass(MANAGER_CLASS, classLoader)

        // 找扫码风险检查方法：优先 (boolean,String,Bundle)，兼容旧版带 manager 前置参数的 (QrAgentLoginManager,boolean,String,Bundle)
        check = ReflectUtils.findMethodOrNull(manager, Void.TYPE, Boolean::class.javaPrimitiveType, String::class.java, Bundle::class.java)
            ?: ReflectUtils.findMethodOrNull(manager, Void.TYPE, manager, Boolean::class.javaPrimitiveType, String::class.java, Bundle::class.java)
            ?: run {
                LogUtils.w(TAG, "未找到扫码风险检查方法")
                null
            }

        val target = check ?: return
        HookUtils.hookReplace(target) { param ->
            if (!isEnabled()) return@hookReplace HookUtils.invokeOriginalMethod(param)
            // 把首个 boolean 参数强制改为 false，解除风险校验。
            // 方法可能是 (boolean,String,Bundle) 或带前置 manager 的 (QrAgentLoginManager,boolean,String,Bundle)，
            // boolean 不一定是 args[0]，需遍历找到第一个 Boolean 参数再改写。
            val a = param.args
            if (a != null) {
                for (i in a.indices) {
                    if (a[i] is Boolean) {
                        a[i] = false
                        break
                    }
                }
            }
            HookUtils.invokeOriginalMethod(param)
        }
    }
}