package me.lengyu.qedge.hook.item

import de.robv.android.xposed.XposedHelpers
import me.lengyu.qedge.hook.annotation.HookItemAnnotation
import me.lengyu.qedge.hook.base.BaseSwitchHookItem
import me.lengyu.qedge.utils.HookUtils
import me.lengyu.qedge.utils.LogUtils
import me.lengyu.qedge.utils.ModuleConfig
import me.lengyu.qedge.utils.ReflectUtils
/**
 * @Author 冷雨
 * @Description 语音消息倍速播放：hook 底层播放器 setPlaySpeed，强制固定倍速（默认 1.5x）
 */
@HookItemAnnotation(value = "语音消息倍速播放", category = "item", tag = "语音倍速", desc = "hook 播放器 setPlaySpeed，强制语音/AMR按配置倍速播放")
object VoiceSpeed : BaseSwitchHookItem() {

    private const val TAG = "VoiceSpeed"
    private const val KEY_ENABLE = "voice_speed_enable"
    private const val KEY_VALUE = "voice_speed_value"

    // QQ 语音播放器接口 com.tencent.mobileqq.qqaudio.audioplayer.j 有两个实现：silk 与 amr
    private const val SILK_CLASS = "com.tencent.mobileqq.qqaudio.audioplayer.SilkPlayer"
    private const val AMR_CLASS = "com.tencent.mobileqq.qqaudio.audioplayer.AmrPlayer"

    private fun isEnabled() = ModuleConfig.getBoolean(KEY_ENABLE, false)

    /** 读取配置倍速，非法时回退默认值 */
    fun currentSpeed(): Float {
        val raw = ModuleConfig.getString(KEY_VALUE, "1.5")
        val v = raw.toFloatOrNull()
        return if (v != null && v > 0f && v <= 3f) v else 1.5f
    }

    override fun onInit(): Boolean = true

    override fun onHook() {
        val classLoader = ReflectUtils.hostClassLoader ?: return
        hookSetPlaySpeed(classLoader, SILK_CLASS)
        hookSetPlaySpeed(classLoader, AMR_CLASS)
    }

    private fun hookSetPlaySpeed(classLoader: ClassLoader, className: String) {
        try {
            val clazz = XposedHelpers.findClass(className, classLoader)
            val method = XposedHelpers.findMethodExact(clazz, "setPlaySpeed", java.lang.Float.TYPE)
            HookUtils.hookReplace(method) { param ->
                if (!isEnabled()) return@hookReplace HookUtils.invokeOriginalMethod(param)
                // 强制改写为配置倍速
                param.args[0] = currentSpeed()
                HookUtils.invokeOriginalMethod(param)
            }
            // LogUtils.i(TAG, "已 hook 语音倍速: $className")
        } catch (e: Throwable) {
            LogUtils.e(TAG, "hookSetPlaySpeed($className) error: ${e.message}")
        }
    }
}