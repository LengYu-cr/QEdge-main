package me.lengyu.qedge.hook.item

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
 * @Description 禁用聊天页（AIO）左滑手势，避免进入群聊的「群应用」或私聊的「聊天关系」页面。
 *
 * 真实链路：AIO 页面由 com.tencent.aio.frame.drawer.DrawerFrameViewGroup 承载，
 * 左滑时 onInterceptTouchEvent / onTouchEvent 都会调用内部的滑动判定方法
 * m(F, String)Z（参数为 xDetail = currentX - lastX，负值即手指向左滑动），
 * 判定拦截后滚动抽屉露出右帧 DrawerChildFrame，右帧内容由
 * TroopAppNewDrawer / TroopAppShortcutDrawer / TroopAiSummaryDrawer（群）
 * 与 FriendIntimateInfoDrawer（私聊）提供。
 *
 * 因此在这里直接让左滑判定返回 false 即可禁用左滑；右滑关闭抽屉、
 * 消息列表纵向滚动和点击均不受影响。
 *
 * DrawerFrameViewGroup 类名未混淆，混淆方法名按「参数 (float, String)、返回 boolean」
 * 在运行时动态定位，不硬编码。
 */
@HookItemAnnotation(
    value = "禁用聊天页左滑",
    category = "item",
    tag = "禁用聊天页左滑",
    desc = "聊天页左滑不再进入群应用或聊天关系页面",
    // 聊天页只在宿主主进程渲染
    process = "Main"
)
object DisableAioLeftSwipe : BaseSwitchHookItem() {

    private const val TAG = "DisableAioLeftSwipe"
    private const val KEY_ENABLE = "disable_aio_left_swipe"
    private const val DRAWER_VIEW_GROUP_CLASS = "com.tencent.aio.frame.drawer.DrawerFrameViewGroup"

    private fun isEnabled() = ModuleConfig.getBoolean(KEY_ENABLE, false)

    /** 开关只在启动装钩子时快照一次，回调里不再读配置 */
    @Volatile
    private var enabled = false

    override fun onHook() {
        enabled = isEnabled()
        if (!enabled) return
        try {
            val drawerClass = XposedHelpers.findClassIfExists(
                DRAWER_VIEW_GROUP_CLASS, ReflectUtils.hostClassLoader
            )
            if (drawerClass == null) {
                LogUtils.e(TAG, "DrawerFrameViewGroup not found")
                return
            }
            val swipeMethod = findSwipeMethod(drawerClass)
            if (swipeMethod == null) {
                LogUtils.e(TAG, "DrawerFrameViewGroup 滑动判定方法未找到")
                return
            }
            HookUtils.hookBefore(swipeMethod) { param ->
                if (!enabled) return@hookBefore
                val xDetail = param.args?.getOrNull(0) as? Float ?: return@hookBefore
                // xDetail < 0 表示手指向左滑动（露出右帧）
                if (xDetail < 0f) param.result = false
            }
        } catch (e: Throwable) {
            LogUtils.e(TAG, "onHook: ${e.message}")
        }
    }

    /**
     * 定位 m(F, String)Z：类中唯一的「参数 (float, String)、返回 boolean」方法。
     */
    private fun findSwipeMethod(drawerClass: Class<*>): Method? {
        val floatType = Float::class.javaPrimitiveType ?: return null
        val booleanType = Boolean::class.javaPrimitiveType ?: return null
        return drawerClass.declaredMethods.firstOrNull { method ->
            method.returnType == booleanType &&
                method.parameterTypes.size == 2 &&
                method.parameterTypes[0] == floatType &&
                method.parameterTypes[1] == String::class.java
        }
    }
}
