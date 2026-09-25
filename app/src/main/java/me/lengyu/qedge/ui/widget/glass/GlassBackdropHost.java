package me.lengyu.qedge.ui.widget.glass;

import android.app.Activity;
import android.graphics.Insets;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.FrameLayout;

import com.liquidglass.java.miba.LiquidBottomTab;
import com.liquidglass.java.miba.LiquidBottomTabs;
import com.liquidglass.java.miba.LiquidGlassApi;

/**
 * 模块页底部导航条的宿主。
 *
 * 直接使用液态玻璃库自带的成品组件 {@link LiquidBottomTabs}：它内部已经是
 * 玻璃托盘(panel) + 玻璃球(indicator) + 采样层(captureGlass) + 拖拽动画
 * (DampedDragAnimation) + 按压光斑(InteractiveHighlight) 的完整结构，
 * 数值全部沿用库自身的默认配方，本类不再自行拼参数。
 *
 * 采样走库的自动模式(ActivityBackdropManager)：整个 DecorView 每帧只录制一次到
 * 一份共享 RenderNode，所有玻璃层共用，玻璃自身不需要接管重绘时机。
 * {@link LiquidBottomTabs} 同时是 LiquidGlassOverlay，库会自动把它搬进挂
 * 在 DecorView 上的 PortalHost，contentRoot 中只留下一个保持原位置的占位 View。
 *
 * 注意：tabs 的 LayoutParams 在入 Portal 时会被占位 View 接管，之后再改它的
 * margin 不会生效。因此高度/边距/安全区一律作用在 holder 上（padding + 子 View 的
 * margin），靠占位 View 的位置带动玻璃跟随。
 */
public final class GlassBackdropHost {

    public interface OnSelect {
        void run(int index);
    }

    /** 导航条上的 4 个导航键。 */
    public static final String[] TAB_LABELS = {"模块首页", "拓展脚本", "冷雨Java", "文件管理"};

    /** "文件管理"是跳页动作，不是可停留的选中态：点完让玻璃球弹回原位置。 */
    private static final int ACTION_TAB_INDEX = 3;
    private static final long ACTION_REBOUND_DELAY_MS = 240L;

    /** 导航条高度（dp），与 LiquidBottomTabs 的 64dp 托盘一致。 */
    public static final float NAV_HEIGHT_DP = 64f;

    /** 导航条左右外边距（dp）。 */
    private static final float NAV_SIDE_MARGIN_DP = 12f;

    /** 导航条与系统导航栏之间的距离（dp）。 */
    private static final float NAV_BOTTOM_GAP_DP = 10f;

    /** 页面内容底部需要预留的高度（dp）：导航条 + 与系统栏的间距 + 呼吸间距。 */
    public static final float CONTENT_BOTTOM_DP = NAV_HEIGHT_DP + NAV_BOTTOM_GAP_DP + 12f;

    /**
     * 两次重新采样之间至少间隔的毫秒数。
     *
     * 采样一次 = 把整个 DecorView 的显示列表重录一遍，工作量与绘制一帧内容相当。
     * 库默认每个 vsync 都重录一次，滑动时等于把整屏画两遍；
     * 这里放到约 30fps：背景在玻璃里已经是模糊过的，慢一帧肉眼基本看不出，
     * 但重录次数直接砍半以上。
     */
    private static final long CAPTURE_INTERVAL_MS = 32L;

    private static final int CONTAINER_LIGHT = 0x66FAFAFA;
    private static final int CONTAINER_DARK = 0x66121212;
    private static final int ACCENT_LIGHT = 0xFF007AFF;
    private static final int ACCENT_DARK = 0xFF0091FF;
    private static final int CONTENT_LIGHT = 0xFF1D1D1F;
    private static final int CONTENT_DARK = 0xFFF5F5F7;

    private static GlassBackdropHost sInstance;

    private final FrameLayout holder;
    private final LiquidBottomTabs tabs;
    private final Runnable revertAction;

    private OnSelect onSelect;
    private int contentColor = CONTENT_LIGHT;
    private int accentColor = ACCENT_LIGHT;
    private boolean dark;
    private int lastStableIndex;

    private final View.OnAttachStateChangeListener attachListener =
            new View.OnAttachStateChangeListener() {
                @Override public void onViewAttachedToWindow(View v) {
                    // 关掉库的"逐帧重录整屏 + Choreographer 帧泵"模式，
                    // 改成"内容真正走一次 traversal 才采样、且最多 30fps"。
                    // 静止时零开销；滑动时重录次数减半以上，这正是卡帧的来源。
                    LiquidGlassApi.setAutoBackdropCaptureInterval(v, CAPTURE_INTERVAL_MS);
                    // 入 Portal 后 onAttachedToWindow 会把所有 tab 重置成统一色，
                    // 这里补回选中项的主题色。
                    applyTabColors();
                }

                @Override public void onViewDetachedFromWindow(View v) {
                }
            };

    private GlassBackdropHost(Activity activity, ViewGroup contentRoot, boolean dark) {
        holder = new FrameLayout(activity);
        holder.setClipChildren(false);
        holder.setClipToPadding(false);

        tabs = new LiquidBottomTabs(activity);
        // QEdge 的主题由模块自己决定，不用系统夜间模式，也不用自适应取色
        // （自适应会在每帧把选中项的重音色覆盖掉）。
        tabs.setAdaptiveContentColorEnabled(false);
        for (String label : TAB_LABELS) {
            LiquidBottomTab tab = new LiquidBottomTab(activity);
            tab.setText(label);
            tabs.addTab(tab);
        }
        revertAction = new Runnable() {
            @Override public void run() {
                if (tabs.getSelectedIndex() == ACTION_TAB_INDEX) {
                    tabs.setSelectedIndex(lastStableIndex, true);
                    applyTabColors();
                }
            }
        };
        tabs.setOnTabSelectedListener(new LiquidBottomTabs.OnTabSelectedListener() {
            @Override public void onTabSelected(LiquidBottomTabs view, int index) {
                if (index != ACTION_TAB_INDEX) lastStableIndex = index;
                applyTabColors();
                if (index == ACTION_TAB_INDEX) {
                    view.postDelayed(revertAction, ACTION_REBOUND_DELAY_MS);
                }
                if (onSelect != null) onSelect.run(index);
            }
        });
        tabs.addOnAttachStateChangeListener(attachListener);

        applyTheme(dark);

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Math.round(dp(NAV_HEIGHT_DP)));
        lp.gravity = Gravity.BOTTOM;
        lp.leftMargin = Math.round(dp(NAV_SIDE_MARGIN_DP));
        lp.rightMargin = Math.round(dp(NAV_SIDE_MARGIN_DP));
        lp.bottomMargin = Math.round(dp(NAV_BOTTOM_GAP_DP));
        holder.addView(tabs, lp);
        tabs.setVisibility(View.GONE);
        tabs.setSelectedIndex(0, false);

        contentRoot.addView(holder, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        holder.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                int bottom, left, right;
                if (Build.VERSION.SDK_INT >= 30) {
                    Insets b = insets.getInsets(
                            WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                    bottom = b.bottom;
                    left = b.left;
                    right = b.right;
                } else {
                    bottom = insets.getSystemWindowInsetBottom();
                    left = insets.getSystemWindowInsetLeft();
                    right = insets.getSystemWindowInsetRight();
                }
                v.setPadding(left, 0, right, bottom);
                return insets;
            }
        });
        holder.requestApplyInsets();
    }

    // ───────────────────────── 生命周期 ─────────────────────────

    /** 把导航条挂到 Activity 的 content 上，需在 setContent 之后调用。 */
    public static void attach(Activity activity, ViewGroup contentRoot, boolean dark) {
        detach();
        if (contentRoot == null) return;
        sInstance = new GlassBackdropHost(activity, contentRoot, dark);
    }

    public static void detach() {
        if (sInstance == null) return;
        GlassBackdropHost host = sInstance;
        sInstance = null;
        host.dispose();
    }

    public static GlassBackdropHost get() {
        return sInstance;
    }

    private void dispose() {
        tabs.removeOnAttachStateChangeListener(attachListener);
        tabs.setVisibility(View.GONE);
        ViewGroup parent = (ViewGroup) holder.getParent();
        if (parent != null) parent.removeView(holder);
    }

    // ───────────────────────── 对外接口 ─────────────────────────

    public void setOnSelect(OnSelect l) {
        onSelect = l;
    }

    public void setVisible(boolean visible) {
        int target = visible ? View.VISIBLE : View.GONE;
        if (tabs.getVisibility() != target) tabs.setVisibility(target);
    }

    public void setDark(boolean value) {
        if (dark == value) return;
        applyTheme(value);
    }

    public void setSelected(int index, boolean animate) {
        if (index >= 0 && index < ACTION_TAB_INDEX) lastStableIndex = index;
        tabs.setSelectedIndex(index, animate);
        applyTabColors();
    }

    /** 当前选中的导航键下标。 */
    public int selected() {
        return tabs.getSelectedIndex();
    }

    // ───────────────────────── 内部 ─────────────────────────

    private void applyTheme(boolean value) {
        dark = value;
        contentColor = dark ? CONTENT_DARK : CONTENT_LIGHT;
        accentColor = dark ? ACCENT_DARK : ACCENT_LIGHT;
        tabs.setContainerColor(dark ? CONTAINER_DARK : CONTAINER_LIGHT);
        tabs.setAccentColor(accentColor);
        tabs.setContentColor(contentColor);
        applyTabColors();
    }

    /** 选中项用重音色，其余用正文色。 */
    private void applyTabColors() {
        int selected = tabs.getSelectedIndex();
        for (int i = 0; i < tabs.getTabCount(); i++) {
            LiquidBottomTab tab = tabs.getTabAt(i);
            if (tab != null) {
                tab.setGeneratedContentColor(i == selected ? accentColor : contentColor);
            }
        }
        tabs.refreshCaptureTabs();
    }

    private float dp(float value) {
        return value * holder.getResources().getDisplayMetrics().density;
    }
}