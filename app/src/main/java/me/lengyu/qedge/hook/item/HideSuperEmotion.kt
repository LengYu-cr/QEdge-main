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
 * @Description 移除超级表情：从表情面板的数据源里移除「超级表情」页签（EmotionPanelInfo.type == 19）。
 *
 * 该页签位于「收藏」左侧，与虚拟形象（type == 22，位于收藏右侧）同在表情面板的页签栏中。
 * 页签栏按固定宽度 mTabWidth + 索引定位，单纯把 View 置 GONE 只会留下空位，
 * 因此必须在数据层把条目删掉，让页签和页面一起消失。
 */
@HookItemAnnotation(
    value = "移除超级表情",
    category = "item",
    tag = "移除超级表情",
    desc = "移除表情面板中的「超级表情」页签",
    // 表情面板只在宿主主进程渲染，子进程不需要加载 BasePanelView/EmoticonTabAdapter
    process = "Main"
)
object HideSuperEmotion : BaseSwitchHookItem() {

    private const val TAG = "HideSuperEmotion"
    private const val KEY_ENABLE = "hide_super_emotion"
    private const val PANEL_VIEW_CLASS = "com.tencent.mobileqq.emoticonview.BasePanelView"
    private const val TAB_ADAPTER_CLASS = "com.tencent.mobileqq.emoticonview.EmoticonTabAdapter"
    private const val VIEW_PAGER_ADAPTER_CLASS =
        "com.tencent.mobileqq.emoticonview.EmotionPanelViewPagerAdapter"
    private const val SUPER_EMOTION_TAB_TYPE = 19

    /** androidx.viewpager.widget.PagerAdapter.POSITION_NONE */
    private const val POSITION_NONE = -2

    /**
     * 本轮 setViewPagerAdapter 是否真的删掉了条目。
     * 删掉后 ViewPager 的页面索引会整体前移，但超级表情页（Z-plan/动画贴纸分支）
     * 返回的外层 View 没有 setTag，getItemPosition 会走默认 POSITION_UNCHANGED，
     * 页面不会重建，于是旧超级表情页停在收藏页签的位置上。
     */
    @Volatile
    private var needRebuildPages = false

    private fun isEnabled() = ModuleConfig.getBoolean(KEY_ENABLE, false)

    override fun onInit(): Boolean = true

    override fun onHook() {
        try {
            val panelViewClass = XposedHelpers.findClassIfExists(
                PANEL_VIEW_CLASS, ReflectUtils.hostClassLoader
            )
            if (panelViewClass == null) {
                LogUtils.e(TAG, "BasePanelView not found")
                return
            }

            val filterMethod = findFilterMethod(panelViewClass)
            if (filterMethod != null) {
                // 每次打开面板都会走这里，先重置标记，只有真的删了条目才需要重建页面
                HookUtils.hookBefore(filterMethod) { _ -> needRebuildPages = false }
                // setViewPagerAdapter 里先调它，之后才 fixSelectIndex / initTabView / setData，
                // 在这个时机删条目不会造成选中索引错位
                HookUtils.hookAfter(filterMethod) { param ->
                    if (!isEnabled()) return@hookAfter
                    if (removeSuperEmotionTab(param.args?.getOrNull(0))) {
                        needRebuildPages = true
                    }
                }
            } else {
                // 老版本没有 filterPanelDataListInLiveChannel，退回到页签初始化前删除
                HookUtils.hookAllMethods(
                    panelViewClass,
                    "initTabView",
                    { param ->
                        if (!isEnabled()) return@hookAllMethods
                        try {
                            val controller = XposedHelpers.getObjectField(
                                param.thisObject, "mPanelController"
                            )
                            if (removeSuperEmotionTab(XposedHelpers.callMethod(controller, "getPanelDataList"))) {
                                needRebuildPages = true
                            }
                        } catch (e: Throwable) {
                            LogUtils.e(TAG, "initTabView: ${e.message}")
                        }
                    },
                    null
                )
            }
        } catch (e: Throwable) {
            LogUtils.e(TAG, "onHook: ${e.message}")
        }
        hookTabAdapter()
        hookViewPagerAdapter()
    }

    /**
     * 删除条目后，ViewPager 需要按新列表重建页面。
     *
     * 超级表情页走 Z-plan / 动画贴纸分支，instantiateItem 返回的外层 View 没有 setTag，
     * getItemPosition 会落到 PagerAdapter 默认实现（POSITION_UNCHANGED），页面被留在原索引上；
     * 索引前移后这个旧页面正好顶到「收藏」的位置，收藏表情因此发不出去。
     * 只有在确实删过条目的那一轮才返回 POSITION_NONE，平时保留 ViewPager 的页面复用。
     */
    private fun hookViewPagerAdapter() {
        try {
            val adapterClass = XposedHelpers.findClassIfExists(
                VIEW_PAGER_ADAPTER_CLASS, ReflectUtils.hostClassLoader
            )
            if (adapterClass == null) {
                LogUtils.e(TAG, "EmotionPanelViewPagerAdapter not found")
                return
            }
            HookUtils.hookAllMethods(
                adapterClass,
                "getItemPosition",
                { param ->
                    if (needRebuildPages && isEnabled()) {
                        param.result = POSITION_NONE
                    }
                },
                null
            )
        } catch (e: Throwable) {
            LogUtils.e(TAG, "hookViewPagerAdapter: ${e.message}")
        }
    }

    /**
     * 页签按钮由 EmoticonTabAdapter.mData 绘制，与 ViewPager 的 panelDataList 并非同一次构建，
     * 因此还要在 updateData 入口把超级表情条目过滤掉，否则页面没了按钮还在。
     */
    private fun hookTabAdapter() {
        try {
            val adapterClass = XposedHelpers.findClassIfExists(
                TAB_ADAPTER_CLASS, ReflectUtils.hostClassLoader
            )
            if (adapterClass == null) {
                LogUtils.e(TAG, "EmoticonTabAdapter not found")
                return
            }
            HookUtils.hookAllMethods(
                adapterClass,
                "updateData",
                { param ->
                    if (!isEnabled()) return@hookAllMethods
                    val data = param.args?.getOrNull(0) as? List<*> ?: return@hookAllMethods
                    val filtered = filterTabItems(data) ?: return@hookAllMethods
                    param.args[0] = filtered
                },
                null
            )
        } catch (e: Throwable) {
            LogUtils.e(TAG, "hookTabAdapter: ${e.message}")
        }
    }

    private fun filterTabItems(data: List<*>): List<Any>? {
        // 先扫一遍判断是否存在目标页签，未命中直接返回，避免每次 updateData 都白拷贝一个列表
        var hit = false
        for (item in data) {
            if (item != null && tabType(item) == SUPER_EMOTION_TAB_TYPE) {
                hit = true
                break
            }
        }
        if (!hit) return null

        val result = ArrayList<Any>(data.size - 1)
        for (item in data) {
            if (item == null) continue
            if (tabType(item) == SUPER_EMOTION_TAB_TYPE) continue
            result.add(item)
        }
        return result
    }

    /**
     * 这里会同时收到 EmotionPanelInfo 和 EmoticonTabItem 两种对象，
     * 不能共用一个缓存 Field，否则拿到的 Field 声明类不匹配，getInt 会抛异常被吞成 -1。
     */
    private fun tabType(item: Any): Int = try {
        XposedHelpers.getIntField(item, "type")
    } catch (e: Throwable) {
        -1
    }

    private fun findFilterMethod(clazz: Class<*>): Method? {
        return try {
            clazz.getDeclaredMethod("filterPanelDataListInLiveChannel", List::class.java).apply {
                isAccessible = true
            }
        } catch (e: Throwable) {
            null
        }
    }

    private fun removeSuperEmotionTab(data: Any?): Boolean {
        val list = data as? MutableList<*> ?: return false
        var removed = false
        for (index in list.indices.reversed()) {
            val info = list[index] ?: continue
            if (tabType(info) != SUPER_EMOTION_TAB_TYPE) continue
            try {
                list.removeAt(index)
                removed = true
            } catch (e: Throwable) {
                LogUtils.e(TAG, "remove tab failed: ${e.message}")
            }
        }
        return removed
    }
}
