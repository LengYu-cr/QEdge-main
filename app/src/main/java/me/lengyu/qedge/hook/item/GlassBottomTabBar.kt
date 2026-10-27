package me.lengyu.qedge.hook.item

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewParent
import android.view.ViewTreeObserver
import android.view.WindowInsets
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.liquidglass.java.miba.LiquidBottomTab
import com.liquidglass.java.miba.LiquidBottomTabs
import com.liquidglass.java.miba.LiquidGlassApi
import de.robv.android.xposed.XposedHelpers
import me.lengyu.qedge.hook.annotation.HookItemAnnotation
import me.lengyu.qedge.hook.base.BaseSwitchHookItem
import me.lengyu.qedge.utils.HookUtils
import me.lengyu.qedge.utils.HostInfo
import me.lengyu.qedge.utils.LogUtils
import me.lengyu.qedge.utils.ModuleConfig
import me.lengyu.qedge.utils.ReflectUtils
import java.lang.ref.WeakReference
import java.lang.reflect.Method
import java.util.WeakHashMap

/**
 * @Author 冷雨
 * @Description 用液态玻璃底栏替换 QQ 主界面底部导航栏。
 *
 * 实现要点：
 * 1. 在 TabFrameLayout 的「底栏重建」方法（唯一的 void + int 签名）之后取到 QQTabLayout；
 * 2. 原生 QQTabLayout 与同父的 QQBlurViewWrapper 一并压成 GONE，并用全局布局监听持续压制，
 *    避免 QQ 在滑动时重新显示导致「残留一个框」；
 * 3. 把 com.liquidglass.java.miba 的 LiquidBottomTabs 直接 addView 到 QQTabLayout 的父容器
 *    （DragFrameLayout），由库自己负责玻璃托盘/指示球/背景采样；
 * 4. tab 的图标与文字全部从原生 tab item 中提取（图标优先取 TabDragAnimationView 的
 *    Drawable 字段，三级回退），不自行绘制图标；
 * 5. 点击走原生 tab item 的 performClick()，选中态由 FrameFragment.onTabChanged(String)
 *    驱动同步，避免直接 setCurrentTab 造成的页面错位。
 */
@HookItemAnnotation(
    value = "液态玻璃底栏",
    category = "item",
    tag = "液态玻璃底栏",
    desc = "用液态玻璃底栏替换 QQ 原生底部导航栏，图标取自 QQ 原生底栏",
    // 首页只在宿主主进程渲染
    process = "Main"
)
object GlassBottomTabBar : BaseSwitchHookItem() {

    private const val TAG = "GlassBottomTabBar"

    private const val KEY_ENABLE = "glass_bottom_tab_bar"

    /** 底栏布局根，类名未混淆 */
    private const val TAB_FRAME_LAYOUT = "com.tencent.mobileqq.tab.TabFrameLayout"

    /** 底栏本体，类名未混淆 */
    private const val QQ_TAB_LAYOUT = "com.tencent.mobileqq.widget.QQTabLayout"

    /** 底栏毛玻璃底板，类名未混淆 */
    private const val QQ_BLUR_VIEW_WRAPPER = "com.tencent.qui.quiblurview.QQBlurViewWrapper"

    /** 底栏单个 tab 的图标绘制 View（图标 Drawable 存在它的私有字段里） */
    private const val TAB_DRAG_ANIMATION_VIEW = "com.tencent.mobileqq.widget.TabDragAnimationView"

    /** 主页 Fragment，onTabChanged(String) 是频道切换回调 */
    private const val FRAME_FRAGMENT = "com.tencent.mobileqq.app.FrameFragment"

    /** QQ 所有 Activity 的基类，生命周期方法在这里 */
    private const val BASE_ACTIVITY = "com.tencent.mobileqq.app.BaseActivity"

    /** 主页右侧滑出的抽屉容器（抽屉面板在屏幕左侧） */
    private const val DRAWER_FRAME = "com.tencent.mobileqq.activity.recent.DrawerFrame"

    /** 抽屉面板本体，关闭时 GONE，展开时置为 VISIBLE 并从左侧滑入 */
    private const val SETTING_ME_PANEL = "QQSettingMe"

    /** 玻璃底栏的 view tag，用于重建时复用 */
    private const val VIEW_TAG = "QEdge_GlassBottomTabBar"

    /** 底栏尺寸配置项：模块首页「液态玻璃底栏」卡片里可调，默认值即原来的比例 */
    const val KEY_HEIGHT_DP = "glass_bar_height_dp"
    const val KEY_SIDE_MARGIN_DP = "glass_bar_side_margin_dp"
    const val DEFAULT_HEIGHT_DP = 58
    const val DEFAULT_SIDE_MARGIN_DP = 36

    /** 托盘高度与内部行高度之间固定的 8dp 内边距，调节高度时按比例保留 */
    private const val INNER_GAP_DP = 8f
    private const val MIN_HEIGHT_DP = 36
    private const val MAX_HEIGHT_DP = 88
    private const val MAX_SIDE_MARGIN_DP = 120
    private const val BOTTOM_GAP_DP = 8f
    private const val ICON_SIZE_DP = 22f
    private const val TEXT_SIZE_SP = 10f

    /** 整屏重录的节流间隔：背景已经模糊，30fps 肉眼几乎无差，重录次数砍半以上 */
    private const val CAPTURE_INTERVAL_MS = 32L

    /** 仅保留薄薄一层雾面：原来的 40% 白雾叠在 QQ 的浅灰列表上会整块发灰 */
    private const val CONTAINER_LIGHT = 0x0FFAFAFA.toInt()
    private const val CONTAINER_DARK = 0x24121212.toInt()
    private const val ACCENT_LIGHT = 0xFF007AFF.toInt()
    private const val ACCENT_DARK = 0xFF0091FF.toInt()
    private const val CONTENT_LIGHT = 0xFF1D1D1F.toInt()
    private const val CONTENT_DARK = 0xFFF5F5F7.toInt()

    /** 当前挂载的玻璃底栏 */
    private var barTabs: LiquidBottomTabs? = null

    /** 玻璃底栏所在的原生父容器，用于判断是否需要重新挂载 */
    private var barHost = WeakReference<ViewGroup>(null)

    /** 原生 tab item，索引与玻璃底栏的 tab 一一对应 */
    private var nativeItems: List<View> = emptyList()

    /** 已被压成 GONE 的原生视图，避免重复注册全局布局监听 */
    private val guardedViews = WeakHashMap<View, Boolean>()

    /** QQ 是否通过 FrameFragment.setTabBarHide 主动隐藏了底栏（进入其他页面时） */
    @Volatile
    private var tabBarHide = false

    /** 已注册可见性监听的宿主，避免重复注册 */
    private var visibilityWatcherHost = WeakReference<ViewGroup>(null)

    /** 已挂上全局布局监听的 ViewTreeObserver，换树时迁移 */
    private var watchedObserver = WeakReference<ViewTreeObserver>(null)

    private val attachListener = object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) {
            // 关掉库的逐帧重录，改为内容真正走一次 traversal 才采样、且最多 30fps
            LiquidGlassApi.setAutoBackdropCaptureInterval(v, CAPTURE_INTERVAL_MS)
            // 入 Portal 后 onAttachedToWindow 会把所有 tab 重置成统一色，这里补回选中态配色
            (v as? LiquidBottomTabs)?.let { applyTabColors(it) }
        }

        override fun onViewDetachedFromWindow(v: View) = Unit
    }

    /** 玻璃底栏替换的是 QQ 专属底栏，TIM 没有 */
    override fun onInit(): Boolean = HostInfo.isQQ

    private fun isEnabled() = ModuleConfig.getBoolean(KEY_ENABLE, false)

    override fun onHook() {
        // 开关只在启动装钩子时读一次：关闭时完全不介入，原生底栏保持原样
        if (!isEnabled()) return
        try {
            hookTabRebuild()
            hookTabChanged()
            hookTabBarVisibility()
            hookActivityResume()
            scheduleScan()
        } catch (e: Throwable) {
            LogUtils.e(TAG, "onHook: ${e.message}")
        }
    }

    // ───────────────────────── 钩子安装 ─────────────────────────

    /**
     * 两个入口合起来保证一定能挂上：
     * 1. TabFrameLayout.onFinishInflate 之后补一帧再挂，覆盖「底栏重新构建方法不触发」的情况；
     * 2. TabFrameLayout 中「返回 void + 单个 int 参数」的重建方法，切 tab / 重建时再同步一次。
     */
    private fun hookTabRebuild() {
        val clazz = findHostClass(TAB_FRAME_LAYOUT)
        if (clazz == null) {
            LogUtils.e(TAG, "$TAB_FRAME_LAYOUT not found")
            return
        }

        val inflateMethod = clazz.declaredMethods.firstOrNull {
            it.name == "onFinishInflate" && it.parameterTypes.isEmpty()
        }
        if (inflateMethod != null) {
            HookUtils.hookAfter(inflateMethod) { param ->
                val root = param.thisObject as? ViewGroup ?: return@hookAfter
                // 子 tab 由 QQ 在 inflate 之后才 add，延后两帧等它们就位
                root.post { root.post { safeInstall(root) } }
            }
        }

        val intType = Int::class.javaPrimitiveType ?: return
        val candidates = clazz.declaredMethods.filter { method ->
            method.returnType == Void.TYPE &&
                method.parameterTypes.size == 1 &&
                method.parameterTypes[0] == intType
        }
        val method = candidates.singleOrNull()
            ?: candidates.firstOrNull { it.name.contains("tab", ignoreCase = true) }
            ?: candidates.firstOrNull()
        if (method == null) {
            LogUtils.e(TAG, "TabFrameLayout 底栏重建方法未找到")
            return
        }
        HookUtils.hookAfter(method) { param ->
            val root = param.thisObject as? ViewGroup ?: return@hookAfter
            safeInstall(root)
        }
    }

    private fun safeInstall(root: ViewGroup) {
        try {
            install(root)
        } catch (e: Throwable) {
            LogUtils.e(TAG, "rebuild: ${e.message}")
        }
    }

    /** 频道切换回调，用来把原生选中态同步到玻璃底栏 */
    private fun hookTabChanged() {
        val clazz = findHostClass(FRAME_FRAGMENT)
        if (clazz == null) {
            LogUtils.e(TAG, "$FRAME_FRAGMENT not found")
            return
        }
        val stringType = String::class.java
        val method: Method? = clazz.declaredMethods.firstOrNull { candidate ->
            candidate.name == "onTabChanged" &&
                candidate.returnType == Void.TYPE &&
                candidate.parameterTypes.size == 1 &&
                candidate.parameterTypes[0] == stringType
        } ?: clazz.declaredMethods.firstOrNull { candidate ->
            candidate.returnType == Void.TYPE &&
                candidate.parameterTypes.size == 1 &&
                candidate.parameterTypes[0] == stringType &&
                candidate.name.contains("Tab", ignoreCase = true)
        }
        if (method == null) {
            LogUtils.e(TAG, "FrameFragment.onTabChanged 未找到")
            return
        }
        HookUtils.hookAfter(method) { param ->
            val tabs = barTabs ?: return@hookAfter
            val tag = param.args?.getOrNull(0) as? String
            val indexByTag = indexByTag(tag)
            tabs.post {
                try {
                    val target = if (indexByTag >= 0) indexByTag else selectedIndexIn(nativeItems) ?: return@post
                    if (target != tabs.selectedIndex) {
                        tabs.setSelectedIndex(target, true)
                        applyTabColors(tabs)
                    }
                } catch (e: Throwable) {
                    LogUtils.e(TAG, "sync: ${e.message}")
                }
            }
        }
    }

    // ───────────────────────── 挂载 ─────────────────────────

    /**
     * 液态玻璃控件会被库 portal 到 DecorView 的最顶层，不再受 DragFrameLayout 的
     * 显隐约束。因此必须自己跟随 QQ 的底栏显隐：
     * 1. FrameFragment.setTabBarHide(true) 是 QQ 隐藏底栏的总入口（进入其他页面时）；
     * 2. 宿主 DragFrameLayout 自身被 GONE（主页 Fragment 被 hide）时也要收起。
     */
    private fun hookTabBarVisibility() {
        val clazz = findHostClass(FRAME_FRAGMENT) ?: return
        val boolType = Boolean::class.javaPrimitiveType ?: return
        val method = clazz.declaredMethods.firstOrNull { candidate ->
            candidate.name == "setTabBarHide" &&
                candidate.returnType == Void.TYPE &&
                candidate.parameterTypes.size == 1 &&
                candidate.parameterTypes[0] == boolType
        }
        if (method == null) {
            LogUtils.e(TAG, "FrameFragment.setTabBarHide 未找到")
            return
        }
        HookUtils.hookAfter(method) { param ->
            val hide = param.args?.getOrNull(0) as? Boolean ?: return@hookAfter
            tabBarHide = hide
            applyBarVisibility()
        }
    }

    /** 宿主被挂载/卸载时立刻重新判断一次显隐（detach 后不会再收到全局布局回调） */
    private val hostAttachListener = object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) {
            watchTree(v as ViewGroup)
            applyBarVisibility()
        }

        override fun onViewDetachedFromWindow(v: View) = applyBarVisibility()
    }

    /** 注册一次全局布局监听，宿主不可见时把 portal 出去的玻璃栏一并收起 */
    private fun attachVisibilityWatcher(parent: ViewGroup) {
        val previous = visibilityWatcherHost.get()
        if (previous !== parent) {
            previous?.removeOnAttachStateChangeListener(hostAttachListener)
            visibilityWatcherHost = WeakReference(parent)
            parent.addOnAttachStateChangeListener(hostAttachListener)
        }
        watchTree(parent)
        applyBarVisibility()
    }

    /** ViewTreeObserver 会随 attach 合并/替换，换树时重新挂一次 */
    private fun watchTree(parent: ViewGroup) {
        val observer = parent.viewTreeObserver
        val previous = watchedObserver?.get()
        if (previous === observer) return
        if (previous != null && previous.isAlive) {
            previous.removeOnGlobalLayoutListener(layoutListener)
        }
        if (!observer.isAlive) return
        observer.addOnGlobalLayoutListener(layoutListener)
        watchedObserver = WeakReference(observer)
    }

    private val layoutListener = ViewTreeObserver.OnGlobalLayoutListener { applyBarVisibility() }

    private fun applyBarVisibility() {
        val tabs = barTabs ?: return
        val host = barHost.get()
        val shown = host != null && host.isShown && host.windowVisibility == View.VISIBLE &&
            !tabBarHide && !isDrawerOpen(host)
        // 必须用 INVISIBLE：GONE 会让 portal 占位视图测不到尺寸，库停止同步位置，
        // 再次显示时就会错位。INVISIBLE 只是不绘制，布局与 portal 同步都照常。
        val target = if (shown) View.VISIBLE else View.INVISIBLE
        if (tabs.visibility != target) tabs.visibility = target
    }

    /**
     * 抽屉（首页右滑拉出的设置页）是否展开。
     *
     * 它和底栏同在 DrawerFrame 下，但玻璃底栏已被库 portal 到 DecorView 顶层，
     * 抽屉盖上来时不会自动被遮住，必须自己收起；收起时也顺带避开了
     * 抽屉滑入过程中 portal 重算位置导致的底栏上浮。
     *
     * 判定依据：抽屉面板关闭时是 GONE，展开时才置为 VISIBLE。面板的显隐变化会触发
     * 一次 layout，正好由这里的全局布局监听捕获，无需额外 hook 抽屉的混淆方法。
     */
    private fun isDrawerOpen(host: ViewGroup?): Boolean {
        val drawer = findAncestorByTypeName(host ?: return false, DRAWER_FRAME) as? ViewGroup
            ?: return false
        for (i in 0 until drawer.childCount) {
            val child = drawer.getChildAt(i)
            if (!child.javaClass.name.contains(SETTING_ME_PANEL)) continue
            return child.visibility == View.VISIBLE
        }
        return false
    }

    /**
     * 模块注入晚于 TabFrameLayout 的创建，而且它是代码 new 出来的（不会回调 onFinishInflate），
     * 所以只能借 Activity 回到前台时在视图树里主动找它。
     */
    private fun hookActivityResume() {
        val clazz = findHostClass(BASE_ACTIVITY) ?: return
        val method = clazz.declaredMethods.firstOrNull {
            it.name == "doOnResume" && it.parameterTypes.isEmpty()
        } ?: clazz.declaredMethods.firstOrNull {
            it.name == "onResume" && it.parameterTypes.isEmpty()
        } ?: return
        HookUtils.hookAfter(method) { param ->
            val activity = param.thisObject as? Activity ?: return@hookAfter
            val decor = activity.window?.decorView as? ViewGroup ?: return@hookAfter
            val root = findViewByTypeName(decor, TAB_FRAME_LAYOUT) as? ViewGroup ?: return@hookAfter
            safeInstall(root)
        }
    }

    /** 主页要等启动流程走完才建好，分几次扫描，命中就挂 */
    private fun scheduleScan() {
        val handler = Handler(Looper.getMainLooper())
        for (delay in longArrayOf(800L, 2000L, 4000L, 8000L)) {
            handler.postDelayed({ scanCurrentActivities() }, delay)
        }
    }

    /** 从 ActivityThread 拿当前所有 Activity，找带 TabFrameLayout 的那个 */
    private fun scanCurrentActivities() {
        try {
            val threadClass = Class.forName("android.app.ActivityThread")
            val current = threadClass.getMethod("currentActivityThread").invoke(null) ?: return
            val field = threadClass.getDeclaredField("mActivities").apply { isAccessible = true }
            val records = field.get(current) as? Map<*, *> ?: return
            for (record in records.values) {
                if (record == null) continue
                val activityField = record.javaClass.getDeclaredField("activity").apply { isAccessible = true }
                val activity = activityField.get(record) as? Activity ?: continue
                if (activity.isFinishing) continue
                val decor = activity.window?.decorView as? ViewGroup ?: continue
                val root = findViewByTypeName(decor, TAB_FRAME_LAYOUT) as? ViewGroup ?: continue
                safeInstall(root)
            }
        } catch (e: Throwable) {
            LogUtils.e(TAG, "scan: ${e.message}")
        }
    }

    private fun install(root: ViewGroup) {
        val tabLayout = findViewByTypeName(root, QQ_TAB_LAYOUT) as? ViewGroup ?: run {
            LogUtils.e(TAG, "QQTabLayout not found")
            return
        }
        val parent = tabLayout.parent as? ViewGroup ?: run {
            LogUtils.e(TAG, "QQTabLayout 没有父容器")
            return
        }
        val items = collectTabItems(tabLayout)
        if (items.isEmpty()) {
            LogUtils.e(TAG, "未收集到原生 tab item")
            return
        }

        hideNative(tabLayout)
        hideBlurSiblings(parent, tabLayout)
        nativeItems = items

        val existing = barTabs
        if (existing != null && barHost.get() === parent) {
            refreshTabs(existing, items)
            return
        }

        // 换了宿主（Activity 重建 / 底栏重建出新容器）：先拆掉旧的
        existing?.let { old ->
            old.removeOnAttachStateChangeListener(attachListener)
            (old.parent as? ViewGroup)?.removeView(old)
            old.visibility = View.GONE
        }
        barTabs = null

        val context = root.context
        val heightDp = barHeightDp()
        val sideMarginDp = barSideMarginDp()
        val innerDp = (heightDp - INNER_GAP_DP).coerceAtLeast(30f)
        val tabs = LiquidBottomTabs(context)
        tabs.tag = VIEW_TAG
        tabs.setAdaptiveContentColorEnabled(false)
        tabs.setBarHeightDp(heightDp, innerDp)
        tabs.addOnAttachStateChangeListener(attachListener)
        tabs.setOnTabSelectedListener(object : LiquidBottomTabs.OnTabSelectedListener {
            override fun onTabSelected(tabs: LiquidBottomTabs, index: Int) {
                applyTabColors(tabs)
                val item = nativeItems.getOrNull(index) ?: return
                if (!item.performClick()) {
                    // tab item 没有点击监听时退回 QQTabLayout.setCurrentTab
                    invokeSetCurrentTab(tabLayout, index)
                }
            }
        })

        buildTabs(tabs, items)
        applyTabColors(tabs)

        val lp = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(context, heightDp)
        )
        lp.gravity = Gravity.BOTTOM
        lp.leftMargin = dp(context, sideMarginDp)
        lp.rightMargin = dp(context, sideMarginDp)
        lp.bottomMargin = dp(context, BOTTOM_GAP_DP) + navigationBarHeight(context)
        try {
            parent.addView(tabs, lp)
        } catch (e: Throwable) {
            parent.addView(
                tabs,
                ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(context, heightDp))
            )
        }

        barTabs = tabs
        barHost = WeakReference(parent)
        attachVisibilityWatcher(parent)
    }

    private fun refreshTabs(tabs: LiquidBottomTabs, items: List<View>) {
        val sameCount = tabs.tabCount == items.size
        val sameLabels = sameCount && items.indices.all { i ->
            tabs.getTabAt(i)?.text?.toString() == labelOf(items[i])
        }
        if (!sameCount || !sameLabels) {
            buildTabs(tabs, items)
            applyTabColors(tabs)
        }
        // 重复扫描时只校正显隐，避免每次 refreshCaptureTabs 触发重采样
        applyBarVisibility()
    }

    private fun buildTabs(tabs: LiquidBottomTabs, items: List<View>) {
        val fallback = tabs.selectedIndex
        tabs.clearTabs()
        items.forEach { item ->
            val tab = LiquidBottomTab(tabs.context)
            tab.textSizeSp = TEXT_SIZE_SP
            tab.iconSizeDp = ICON_SIZE_DP
            tab.setText(labelOf(item))
            iconOf(item)?.let { tab.setIconDrawable(it) }
            tabs.addTab(tab)
        }
        val initial = selectedIndexIn(items) ?: fallback
        tabs.setSelectedIndex(initial.coerceIn(0, (items.size - 1).coerceAtLeast(0)), false)
        applyBarTheme(tabs)
    }

    /** 托盘/重音/正文色只在底栏重建或主题变化时设置一次，避免每次选中都重建图层 */
    private fun applyBarTheme(tabs: LiquidBottomTabs) {
        val dark = isDarkTheme(tabs.context)
        tabs.setContainerColor(if (dark) CONTAINER_DARK else CONTAINER_LIGHT)
        // setContainerColor 内部会重建图层并把投影恢复成 Shadow.DEFAULT（24dp / 10% 黑），
        // 在浅色页面上就是托盘外一圈灰影，所以必须紧跟着再清一次
        tabs.panelView.clearShadow()
        tabs.setAccentColor(if (dark) ACCENT_DARK else ACCENT_LIGHT)
        tabs.setContentColor(if (dark) CONTENT_DARK else CONTENT_LIGHT)
    }

    /** 选中项用重音色，其余用正文色；同时把图标按同样的颜色着色以保证暗色可读 */
    private fun applyTabColors(tabs: LiquidBottomTabs) {
        val dark = isDarkTheme(tabs.context)
        val accent = if (dark) ACCENT_DARK else ACCENT_LIGHT
        val content = if (dark) CONTENT_DARK else CONTENT_LIGHT
        val selected = tabs.selectedIndex
        for (i in 0 until tabs.tabCount) {
            val tab = tabs.getTabAt(i) ?: continue
            tab.textSizeSp = TEXT_SIZE_SP
            tab.iconSizeDp = ICON_SIZE_DP
            tab.setGeneratedContentColor(if (i == selected) accent else content)
        }
        tabs.refreshCaptureTabs()
    }

    // ───────────────────────── 原生视图处理 ─────────────────────────

    /** 隐藏原生底栏，并注册一次全局布局监听持续压制 */
    private fun hideNative(view: View) {
        view.visibility = View.GONE
        if (guardedViews.containsKey(view)) return
        guardedViews[view] = true
        view.viewTreeObserver.addOnGlobalLayoutListener {
            if (view.visibility != View.GONE) view.visibility = View.GONE
        }
    }

    /**
     * 隐藏与原生底栏同父的装饰视图：
     * - QQBlurViewWrapper：底栏毛玻璃底板，不隐藏会留下一条底栏形状的框；
     * - 底栏上方的 1px 分隔线（android.view.View + 极扁高度 + 有背景）：不隐藏会在玻璃栏上方留一条细线；
     * - 类名带 Divider 的视图。
     */
    private fun hideBlurSiblings(parent: ViewGroup, tabLayout: View) {
        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i)
            if (child === tabLayout) continue
            val className = child.javaClass.name
            if (className.endsWith(QQ_BLUR_VIEW_WRAPPER) || className.contains("Divider")) {
                hideNative(child)
                continue
            }
            if (child.javaClass == View::class.java && isThinDivider(child)) {
                hideNative(child)
            }
        }
    }

    /** 判定底栏上方的 1px 分隔线：普通 View、固定且极扁的高度、带背景色 */
    private fun isThinDivider(view: View): Boolean {
        val lp = view.layoutParams ?: return false
        val height = lp.height
        if (height <= 0) return false
        if (height > dp(view.context, 2f)) return false
        return view.background != null
    }

    /** 收集原生 tab item：取包含最多 TabDragAnimationView 的子容器作为 strip，取其直接子项 */
    private fun collectTabItems(tabLayout: ViewGroup): List<View> {
        val strip = findTabStrip(tabLayout)
        if (strip != null) {
            val direct = ArrayList<View>()
            for (i in 0 until strip.childCount) {
                val child = strip.getChildAt(i)
                if (child === tabLayout) continue
                if (findTabDragView(child) != null || findTextView(child) != null) {
                    direct.add(child)
                }
            }
            if (direct.size >= 2) return direct
        }
        // 兜底：枚举所有 TabDragAnimationView，映射回 strip 的直接子项
        val drags = ArrayList<View>()
        collectDragViews(tabLayout, drags)
        val boundary = strip ?: tabLayout
        return drags.mapNotNull { tabItemOf(it, boundary) }.distinct()
    }

    /** strip = TabLayout 内部承载所有 TabView 的那个子容器（按 TabDragAnimationView 数量判定） */
    private fun findTabStrip(tabLayout: ViewGroup): ViewGroup? {
        var best: ViewGroup? = null
        var bestCount = 0
        for (i in 0 until tabLayout.childCount) {
            val child = tabLayout.getChildAt(i) as? ViewGroup ?: continue
            val count = countDragViews(child)
            if (count > bestCount) {
                bestCount = count
                best = child
            }
        }
        return best
    }

    private fun countDragViews(view: View): Int {
        var count = if (view.javaClass.name.endsWith(TAB_DRAG_ANIMATION_VIEW)) 1 else 0
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                count += countDragViews(view.getChildAt(i))
            }
        }
        return count
    }

    private fun collectDragViews(view: View, out: MutableList<View>) {
        if (view.javaClass.name.endsWith(TAB_DRAG_ANIMATION_VIEW)) {
            out.add(view)
            return
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                collectDragViews(view.getChildAt(i), out)
            }
        }
    }

    /** 从 dragView 向上找到 boundary 的直接子项，即一个完整的 tab item */
    private fun tabItemOf(view: View, boundary: ViewGroup): View? {
        var current: View = view
        while (true) {
            val parent = current.parent as? ViewGroup ?: return null
            if (parent === boundary) return current
            current = parent
        }
    }

    private fun selectedIndexIn(items: List<View>): Int? {
        val index = items.indexOfFirst { isTabSelected(it) }
        return if (index >= 0) index else null
    }

    private fun isTabSelected(item: View): Boolean {
        if (item.isSelected) return true
        return findTabDragView(item)?.isSelected == true
    }

    private fun indexByTag(tag: String?): Int {
        if (tag.isNullOrEmpty()) return -1
        return nativeItems.indexOfFirst { item ->
            (item.tag as? String) == tag || (findTabDragView(item)?.tag as? String) == tag
        }
    }

    /** tab item 没有点击监听时的兜底：反射调用 QQTabLayout.setCurrentTab(int) */
    private fun invokeSetCurrentTab(tabLayout: ViewGroup, index: Int) {
        try {
            val method = tabLayout.javaClass.methods.firstOrNull { candidate ->
                candidate.name == "setCurrentTab" &&
                    candidate.parameterTypes.size == 1 &&
                    candidate.parameterTypes[0] == Int::class.javaPrimitiveType
            } ?: return
            method.invoke(tabLayout, index)
        } catch (e: Throwable) {
            LogUtils.e(TAG, "setCurrentTab: ${e.message}")
        }
    }

    // ───────────────────────── 图标 / 文字提取 ─────────────────────────

    private fun labelOf(item: View): String {
        val text = findTextView(item)?.text?.toString()?.trim().orEmpty()
        if (text.isNotEmpty()) return text
        return item.contentDescription?.toString()?.trim().orEmpty()
    }

    /** 图标优先取 TabDragAnimationView 里的 Drawable 字段，取不到再退回 ImageView */
    private fun iconOf(item: View): Drawable? {
        val dragView = findTabDragView(item)
        if (dragView != null) {
            readDrawableField(dragView)?.let { return cloneDrawable(it) }
        }
        val imageView = findImageView(item)
        return imageView?.drawable?.let { cloneDrawable(it) }
    }

    /**
     * 读取 TabDragAnimationView 的图标 Drawable：
     * 先按已知的混淆字段名 f/h/i/q/r，再按字段类型兜底扫描。
     * 字段是 int 资源 id 时按资源加载。
     */
    private fun readDrawableField(view: View): Drawable? {
        for (name in arrayOf("f", "h", "i", "q", "r")) {
            try {
                val field = view.javaClass.getDeclaredField(name)
                field.isAccessible = true
                when (val value = field.get(view)) {
                    is Drawable -> if (isUsableIcon(value)) return value
                    is Int -> if (value != 0) {
                        runCatching { view.context.getDrawable(value) }
                            .getOrNull()
                            ?.let { if (isUsableIcon(it)) return it }
                    }
                }
            } catch (_: Throwable) {
                // 该字段不存在，继续下一个
            }
        }
        for (field in view.javaClass.declaredFields) {
            if (field.isSynthetic) continue
            if (!Drawable::class.java.isAssignableFrom(field.type)) continue
            try {
                field.isAccessible = true
                val value = field.get(view) as? Drawable ?: continue
                if (isUsableIcon(value)) return value
            } catch (_: Throwable) {
                // 忽略单个字段读取失败
            }
        }
        return null
    }

    private fun isUsableIcon(drawable: Drawable): Boolean = drawable !is ColorDrawable

    /** 复制一份 Drawable，避免与原 View 共享状态 */
    private fun cloneDrawable(drawable: Drawable): Drawable? {
        return try {
            (drawable.constantState?.newDrawable() ?: drawable).mutate()
        } catch (_: Throwable) {
            drawable
        }
    }

    private fun findTabDragView(view: View): View? {
        if (view.javaClass.name.endsWith(TAB_DRAG_ANIMATION_VIEW)) return view
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                findTabDragView(view.getChildAt(i))?.let { return it }
            }
        }
        return null
    }

    private fun findTextView(view: View): TextView? {
        if (view is TextView) {
            if (!view.text.isNullOrBlank()) return view
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                findTextView(view.getChildAt(i))?.let { return it }
            }
        }
        return null
    }

    private fun findImageView(view: View): ImageView? {
        if (view is ImageView) return view
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                findImageView(view.getChildAt(i))?.let { return it }
            }
        }
        return null
    }

    private fun findViewByTypeName(view: View, className: String): View? {
        if (view.javaClass.name == className) return view
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                findViewByTypeName(view.getChildAt(i), className)?.let { return it }
            }
        }
        return null
    }

    /** 沿父链向上找一个指定类名的祖先 */
    private fun findAncestorByTypeName(view: View, className: String): View? {
        var parent: ViewParent? = view.parent
        while (parent is View) {
            if (parent.javaClass.name == className) return parent
            parent = parent.parent
        }
        return null
    }

    // ───────────────────────── 工具方法 ─────────────────────────

    private fun findHostClass(name: String): Class<*>? =
        XposedHelpers.findClassIfExists(name, ReflectUtils.hostClassLoader)

    private fun dp(context: Context, value: Float): Int =
        Math.round(value * context.resources.displayMetrics.density)

    /** 底栏高度（dp），模块首页可调，默认 58 */
    private fun barHeightDp(): Float =
        ModuleConfig.getInt(KEY_HEIGHT_DP, DEFAULT_HEIGHT_DP)
            .coerceIn(MIN_HEIGHT_DP, MAX_HEIGHT_DP)
            .toFloat()

    /** 左右留白（dp），越小底栏越宽，默认 36 */
    private fun barSideMarginDp(): Float =
        ModuleConfig.getInt(KEY_SIDE_MARGIN_DP, DEFAULT_SIDE_MARGIN_DP)
            .coerceIn(0, MAX_SIDE_MARGIN_DP)
            .toFloat()

    private fun isDarkTheme(context: Context): Boolean {
        return try {
            val attrs = context.obtainStyledAttributes(intArrayOf(android.R.attr.colorBackground))
            val color = attrs.getColor(0, Color.WHITE)
            attrs.recycle()
            luminance(color) < 0.5f
        } catch (_: Throwable) {
            val mode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            mode == Configuration.UI_MODE_NIGHT_YES
        }
    }

    private fun luminance(color: Int): Float =
        (0.299f * Color.red(color) + 0.587f * Color.green(color) + 0.114f * Color.blue(color)) / 255f

    private fun navigationBarHeight(context: Context): Int {
        val activity = findActivity(context)
        if (activity != null && Build.VERSION.SDK_INT >= 23) {
            val insets = activity.window?.decorView?.rootWindowInsets
            if (insets != null) {
                return if (Build.VERSION.SDK_INT >= 30) {
                    insets.getInsets(WindowInsets.Type.navigationBars()).bottom
                } else {
                    insets.systemWindowInsetBottom
                }
            }
        }
        val resources = context.resources
        val id = resources.getIdentifier("navigation_bar_height", "dimen", "android")
        return if (id > 0) resources.getDimensionPixelSize(id) else 0
    }

    private fun findActivity(context: Context): Activity? {
        var current: Context? = context
        while (current is ContextWrapper) {
            if (current is Activity) return current
            current = current.baseContext
        }
        return current as? Activity
    }
}
