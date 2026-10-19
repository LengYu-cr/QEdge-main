package me.lengyu.qedge.hook.item

import de.robv.android.xposed.XposedHelpers
import me.lengyu.qedge.hook.annotation.HookItemAnnotation
import me.lengyu.qedge.hook.base.BaseSwitchHookItem
import me.lengyu.qedge.utils.HookUtils
import me.lengyu.qedge.utils.HostInfo
import me.lengyu.qedge.utils.LogUtils
import me.lengyu.qedge.utils.ModuleConfig
import me.lengyu.qedge.utils.ReflectUtils
import java.lang.reflect.Field
import java.lang.reflect.Method

/**
 * @Author 冷雨
 * @Description 屏蔽虚拟形象：从表情面板的数据源里移除「虚拟形象」页签（EmotionPanelInfo.type == 22）。
 *
 * 表情面板的页签栏（HorizontalListViewEx）和 ViewPager 共用同一份 panelDataList，
 * 且页签栏按固定宽度 mTabWidth + 索引定位，单纯把 View 置 GONE 只会留下空位，
 * 因此必须在数据层把条目删掉，让页签和页面一起消失。
 */
@HookItemAnnotation(
    value = "屏蔽虚拟形象",
    category = "item",
    tag = "屏蔽虚拟形象",
    desc = "移除表情面板中的「虚拟形象」页签",
    // 表情面板只在宿主主进程渲染，子进程不需要加载 BasePanelView/EmoticonTabAdapter
    process = "Main"
)
object HideVirtualAvatar : BaseSwitchHookItem() {

    private const val TAG = "HideVirtualAvatar"
    private const val KEY_ENABLE = "hide_virtual_avatar"
    private const val PANEL_VIEW_CLASS = "com.tencent.mobileqq.emoticonview.BasePanelView"
    private const val TAB_ADAPTER_CLASS = "com.tencent.mobileqq.emoticonview.EmoticonTabAdapter"
    private const val AI_AVATAR_TAB_TYPE = 22

    /** 安装后首次用到时缓存 EmotionPanelInfo.type，回调里只做 Field.getInt，不再按名查找 */
    @Volatile
    private var typeField: Field? = null

    private fun isEnabled() = ModuleConfig.getBoolean(KEY_ENABLE, false)

    override fun onInit(): Boolean = HostInfo.isQQ

    override fun onHook() {
        if (!isEnabled()) return
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
                // setViewPagerAdapter 里先调它，之后才 fixSelectIndex / initTabView / setData，
                // 在这个时机删条目不会造成选中索引错位
                HookUtils.hookAfter(filterMethod) { param ->
                    if (!isEnabled()) return@hookAfter
                    removeAiAvatarTab(param.args?.getOrNull(0))
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
                            removeAiAvatarTab(XposedHelpers.callMethod(controller, "getPanelDataList"))
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
    }

    /**
     * 页签按钮由 EmoticonTabAdapter.mData 绘制，与 ViewPager 的 panelDataList 并非同一次构建，
     * 因此还要在 updateData 入口把虚拟形象条目过滤掉，否则页面没了按钮还在。
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
            if (item != null && tabType(item) == AI_AVATAR_TAB_TYPE) {
                hit = true
                break
            }
        }
        if (!hit) return null

        val result = ArrayList<Any>(data.size - 1)
        for (item in data) {
            if (item == null) continue
            if (tabType(item) == AI_AVATAR_TAB_TYPE) continue
            result.add(item)
        }
        return result
    }

    /** 首次调用时解析并缓存 type 字段；之后每次仅一次 Field.getInt */
    private fun tabType(item: Any): Int {
        var field = typeField
        if (field == null) {
            field = try {
                XposedHelpers.findFieldIfExists(item.javaClass, "type")
            } catch (e: Throwable) {
                null
            } ?: return -1
            typeField = field
        }
        return try {
            field.getInt(item)
        } catch (e: Throwable) {
            -1
        }
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

    private fun removeAiAvatarTab(data: Any?) {
        val list = data as? MutableList<*> ?: return
        for (index in list.indices.reversed()) {
            val info = list[index] ?: continue
            if (tabType(info) != AI_AVATAR_TAB_TYPE) continue
            try {
                list.removeAt(index)
            } catch (e: Throwable) {
                LogUtils.e(TAG, "remove tab failed: ${e.message}")
            }
        }
    }
}
