package com.liquidglass.java.miba;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import java.util.List;

/**
 * Clean Java/View re-port of the original catalog/components/LiquidBottomTabs.kt.
 *
 * This class intentionally mirrors the KT composition as three independent layers:
 * 1) visible 64dp glass panel + visible tab Row;
 * 2) hidden 56dp glass/capture Row used only as tabsBackdrop (accent-tinted);
 * 3) 56dp moving indicator using CombinedBackdrop(baseBackdrop, tabsBackdrop).
 *
 * KT invokes the same content lambda twice. The Java version therefore performs a
 * second render pass into the hidden tabsBackdrop layer, but deliberately does not
 * keep a second TextView/ImageView hierarchy. This avoids independent display-list
 * caches becoming visually out of phase and causing release ghosting.
 */
public class LiquidBottomTabs extends ViewGroup implements LiquidGlassOverlay, Runnable {
    private static final int INVALID_POINTER_ID = -1;

    public interface OnTabSelectedListener {
        void onTabSelected(LiquidBottomTabs tabs, int index);
    }

    private final LiquidGlassView panel;
    private final LinearLayout visibleRow;
    private final LiquidGlassView captureGlass;
    private final CaptureMirrorRow captureRow;
    private final LayerBackdrop tabsBackdrop;
    private final LiquidGlassView indicator;
    private final InteractiveHighlight interactiveHighlight;

    private DampedDragAnimation dragAnimation;
    private Backdrop baseBackdrop = EmptyBackdrop.INSTANCE;
    private boolean explicitBackdrop;
    private boolean autoBackdropEnabled = true;
    private OnTabSelectedListener selectedListener;

    private int selectedIndex;
    private float rawDragOffsetPx;
    private final SpringFloat panelOffsetSpring = new SpringFloat(0f, 1f, 300f, 0.5f);
    private boolean panelOffsetPosted;
    private long lastPanelOffsetFrame;

    private boolean dragging;
    private int activePointerId = INVALID_POINTER_ID;
    private float lastRawX;
    private float gestureDownRawX;
    private float gestureDownRawY;
    private boolean gestureMoved;
    private final int touchSlop;

    private final boolean lightTheme;
    private int accentColor;
    private int containerColor;
    private float pressHighlightIntensity = 1f;

    // Java/Lua extension based on AdaptiveLuminanceGlassContent.kt. The original
    // LiquidBottomTabs.kt itself uses system theme black/white content.
    private boolean adaptiveContentColorEnabled = true;
    private int contentColor;
    private int contentColorFrom;
    private int contentColorTarget;
    private float lastBackdropLuminance = -1f;
    private float luminanceThreshold = 0.5f;
    private long contentColorAnimationMs = 1000L;
    private long contentColorAnimationStart;
    private long adaptiveSampleIntervalMs = 180L;
    private long lastAdaptiveSampleTime;
    private boolean adaptiveAnimating;
    private boolean adaptivePosted;

    private final Runnable adaptiveRunnable = new Runnable() {
        public void run() {
            adaptivePosted = false;
            if (!isAttachedToWindow() || !adaptiveContentColorEnabled) return;

            long now = SystemClock.uptimeMillis();
            if (lastAdaptiveSampleTime == 0L || now - lastAdaptiveSampleTime >= adaptiveSampleIntervalMs) {
                lastAdaptiveSampleTime = now;
                float lum = ActivityBackdropManager.sampleAverageLuminance(LiquidBottomTabs.this, 5, 5);
                if (lum >= 0f) {
                    lastBackdropLuminance = lum;
                    int target = lum > luminanceThreshold ? Color.BLACK : Color.WHITE;
                    if (target != contentColorTarget) {
                        contentColorFrom = contentColor;
                        contentColorTarget = target;
                        contentColorAnimationStart = now;
                        adaptiveAnimating = contentColor != target;
                    }
                }
            }

            if (adaptiveAnimating) {
                float t = contentColorAnimationMs <= 0L ? 1f
                        : (now - contentColorAnimationStart) / (float) contentColorAnimationMs;
                if (t >= 1f) {
                    t = 1f;
                    adaptiveAnimating = false;
                }
                contentColor = lerpColor(contentColorFrom, contentColorTarget,
                        fastOutSlowIn(clamp(t, 0f, 1f)));
                applyVisibleContentColor();
            }

            scheduleAdaptive(adaptiveAnimating ? 16L : adaptiveSampleIntervalMs);
        }
    };

    public LiquidBottomTabs(Context context) { this(context, null); }

    public LiquidBottomTabs(Context context, AttributeSet attrs) {
        super(context, attrs);
        setClipChildren(false);
        setClipToPadding(false);
        setWillNotDraw(true);
        touchSlop = android.view.ViewConfiguration.get(context).getScaledTouchSlop();

        lightTheme = !isDarkTheme(context);
        accentColor = lightTheme ? Color.rgb(0x00, 0x88, 0xFF) : Color.rgb(0x00, 0x91, 0xFF);
        containerColor = ColorUtils.withAlpha(
                lightTheme ? Color.rgb(0xFA, 0xFA, 0xFA) : Color.rgb(0x12, 0x12, 0x12), 0.4f);
        contentColor = lightTheme ? Color.BLACK : Color.WHITE;
        contentColorFrom = contentColor;
        contentColorTarget = contentColor;

        panel = new LiquidGlassView(context);
        panel.setShape(new GlassShape(Float.MAX_VALUE));
        panel.setClipChildren(false);
        panel.setClipToPadding(false);
        addView(panel);

        visibleRow = new LinearLayout(context);
        visibleRow.setOrientation(LinearLayout.HORIZONTAL);
        visibleRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
        visibleRow.setClipChildren(false);
        visibleRow.setClipToPadding(false);
        panel.addView(visibleRow);

        captureGlass = new CaptureOnlyLiquidGlassView(context);
        captureGlass.setShape(new GlassShape(Float.MAX_VALUE));
        captureGlass.setClipChildren(false);
        captureGlass.setClipToPadding(false);
        addView(captureGlass);

        // The KT content lambda is rendered a second time into the hidden tabsBackdrop row.
        // In View-land we mirror the already-laid-out visible tab visuals into this
        // capture-only view instead of maintaining a second independently rasterized
        // TextView/ImageView tree. That preserves the second render pass while avoiding
        // stale display-list/text ghosting between two separate child hierarchies.
        captureRow = new CaptureMirrorRow(context, visibleRow);
        captureRow.setAccentColor(accentColor);
        captureGlass.addView(captureRow);
        tabsBackdrop = new LayerBackdrop(captureGlass);

        indicator = new LiquidGlassView(context);
        indicator.setShape(new GlassShape(Float.MAX_VALUE));
        indicator.setClipChildren(false);
        indicator.setClipToPadding(false);
        addView(indicator);

        rebuildDragAnimation(0f);

        interactiveHighlight = new InteractiveHighlight(this, new InteractiveHighlight.PositionProvider() {
            public void getPosition(float width, float height, float pointerX, float pointerY, float[] outXY) {
                float tabWidth = getTabWidth();
                float panelOffset = getPanelOffsetPx();
                float value = dragAnimation == null ? 0f : dragAnimation.getValue();
                if (getLayoutDirection() == LAYOUT_DIRECTION_RTL) {
                    outXY[0] = width - (value + 0.5f) * tabWidth + panelOffset;
                } else {
                    outXY[0] = (value + 0.5f) * tabWidth + panelOffset;
                }
                outXY[1] = height * 0.5f;
            }
        });
        interactiveHighlight.addInvalidationTarget(panel);
        interactiveHighlight.addInvalidationTarget(captureGlass);

        configureKtLayers();

        indicator.setOnTouchListener(new OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                return handleIndicatorTouch(event);
            }
        });
    }

    private void rebuildDragAnimation(float initialValue) {
        int count = Math.max(1, getTabCount());
        float initial = clamp(initialValue, 0f, count - 1f);
        dragAnimation = new DampedDragAnimation(
                this,
                initial,
                0f,
                count - 1f,
                0.001f,
                1f,
                78f / 56f,
                new DampedDragAnimation.Listener() {
                    public void onAnimationFrame(DampedDragAnimation animation) {
                        applyKtAnimationState();
                    }
                }
        );
    }

    /** Recreates the exact three drawBackdrop parameter sets from LiquidBottomTabs.kt. */
    private void configureKtLayers() {
        final float d = density();

        // Visible 64dp panel:
        // vibrancy(); blur(8dp); lens(24dp,24dp); surface containerColor.
        panel.setEffects(new LiquidGlassView.Effects() {
            public void apply(BackdropEffectScope scope) {
                scope.vibrancy();
                scope.blur(8f * d);
                scope.lens(24f * d, 24f * d);
            }
        });
        panel.setOnDrawSurface(new SolidSurface(containerColor));
        panel.setOnDrawContentBackground(new LiquidGlassView.DrawCallback() {
            public void draw(Canvas canvas, LiquidGlassView view, Path path) {
                interactiveHighlight.draw(canvas, view.getWidth(), view.getHeight());
            }
        });
        panel.setHighlight(Highlight.DEFAULT);
        panel.setShadow(Shadow.DEFAULT);
        panel.clearInnerShadow();

        // Hidden 56dp tabsBackdrop row:
        // vibrancy(); blur(8dp); lens(24dp*p,24dp*p); Highlight.Default(alpha=p).
        captureGlass.setEffects(new LiquidGlassView.Effects() {
            public void apply(BackdropEffectScope scope) {
                float p = ktPressProgress();
                scope.vibrancy();
                scope.blur(8f * d);
                scope.lens(24f * d * p, 24f * d * p);
            }
        });
        captureGlass.setOnDrawSurface(new SolidSurface(containerColor));
        captureGlass.setOnDrawContentBackground(new LiquidGlassView.DrawCallback() {
            public void draw(Canvas canvas, LiquidGlassView view, Path path) {
                interactiveHighlight.draw(canvas, view.getWidth(), view.getHeight());
            }
        });
        captureGlass.setShadow(Shadow.DEFAULT);
        captureGlass.clearInnerShadow();
        captureRow.setAccentColor(accentColor);

        // Moving 56dp indicator; backdrop assigned in updateBackdrops().
        indicator.setEffects(new LiquidGlassView.Effects() {
            public void apply(BackdropEffectScope scope) {
                float p = ktPressProgress();
                scope.lens(10f * d * p, 14f * d * p, false, true);
            }
        });
        indicator.setOnDrawSurface(new LiquidGlassView.DrawCallback() {
            private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            public void draw(Canvas canvas, LiquidGlassView view, Path path) {
                float p = ktPressProgress();
                int edge = lightTheme ? Color.BLACK : Color.WHITE;
                paint.setColor(ColorUtils.withAlpha(edge, 0.1f * (1f - p)));
                canvas.drawRect(0f, 0f, view.getWidth(), view.getHeight(), paint);
                paint.setColor(ColorUtils.withAlpha(Color.BLACK, 0.03f * p));
                canvas.drawRect(0f, 0f, view.getWidth(), view.getHeight(), paint);
            }
        });
        updateBackdrops();
        applyKtAnimationState();
    }

    private float ktPressProgress() {
        return dragAnimation == null ? 0f : dragAnimation.getPressProgress();
    }

    private void applyKtAnimationState() {
        if (dragAnimation == null) return;
        float p = ktPressProgress();
        float panelOffset = getPanelOffsetPx();

        // Visible panel layerBlock: lerp(1, 1 + 16dp / width, p).
        float panelScale = 1f;
        if (panel.getWidth() > 0) panelScale = lerp(1f, 1f + dpF(16f) / panel.getWidth(), p);
        panel.setScaleX(panelScale);
        panel.setScaleY(panelScale);
        panel.setTranslationX(panelOffset);

        // Hidden capture row is translated by panelOffset and its second content
        // render pass scales 1 -> 1.2 through LocalLiquidBottomTabScale.
        captureGlass.setTranslationX(panelOffset);
        captureRow.setCaptureScale(lerp(1f, 1.2f, p));

        // Dynamic hidden-row highlight exactly follows p.
        float highlightAlpha = clamp(p * pressHighlightIntensity, 0f, 1f);
        captureGlass.setHighlight(Highlight.DEFAULT.withAlpha(highlightAlpha));

        // Indicator layerBlock: DampedDragAnimation scale + velocity deformation.
        float velocity = dragAnimation.getVelocity() / 10f;
        float sx = dragAnimation.getScaleX();
        float sy = dragAnimation.getScaleY();
        sx /= 1f - clamp(velocity * 0.75f, -0.2f, 0.2f);
        sy *= 1f - clamp(velocity * 0.25f, -0.2f, 0.2f);
        indicator.setScaleX(sx);
        indicator.setScaleY(sy);

        indicator.setHighlight(Highlight.DEFAULT.withAlpha(highlightAlpha));
        indicator.setShadow(Shadow.DEFAULT.withAlpha(p));
        indicator.setInnerShadow(new InnerShadow(8f * p, p));

        float tabWidth = getTabWidth();
        float tx;
        if (getLayoutDirection() == LAYOUT_DIRECTION_RTL) {
            tx = -dragAnimation.getValue() * tabWidth + panelOffset;
        } else {
            tx = dragAnimation.getValue() * tabWidth + panelOffset;
        }
        indicator.setTranslationX(tx);

        panel.invalidate();
        captureGlass.invalidate();
        captureRow.invalidate();
        indicator.invalidate();
        invalidate();
    }

    /* ----------------------------- Tabs data/API ----------------------------- */

    public void addTab(final LiquidBottomTab tab) {
        if (tab == null) return;
        visibleRow.addView(tab, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
        tab.setGeneratedContentColor(contentColor);

        captureRow.invalidate();

        rebindTabClicks();
        int keep = Math.min(selectedIndex, Math.max(0, getTabCount() - 1));
        selectedIndex = keep;
        rebuildDragAnimation(keep);
        configureKtLayers();
        requestLayout();
    }

    public LiquidBottomTab addTabPng(String pngPath, String text) {
        LiquidBottomTab tab = new LiquidBottomTab(getContext());
        if (pngPath != null) tab.setIconPng(pngPath);
        if (text != null) tab.setText(text);
        addTab(tab);
        return tab;
    }

    public LiquidBottomTab addTabPng(String pngPath) { return addTabPng(pngPath, null); }

    public LiquidBottomTab addTabAsset(String assetPath, String text) {
        LiquidBottomTab tab = new LiquidBottomTab(getContext());
        if (assetPath != null) tab.setIconAsset(assetPath);
        if (text != null) tab.setText(text);
        addTab(tab);
        return tab;
    }

    public LiquidBottomTab addTabUri(String uri, String text) {
        LiquidBottomTab tab = new LiquidBottomTab(getContext());
        if (uri != null) tab.setIconUri(uri);
        if (text != null) tab.setText(text);
        addTab(tab);
        return tab;
    }

    public LiquidBottomTab addTabResource(int drawableResId, String text) {
        LiquidBottomTab tab = new LiquidBottomTab(getContext());
        tab.setIconResource(drawableResId);
        if (text != null) tab.setText(text);
        addTab(tab);
        return tab;
    }

    public LiquidBottomTab addTabBytes(byte[] bytes, String text) {
        LiquidBottomTab tab = new LiquidBottomTab(getContext());
        tab.setIconBytes(bytes);
        if (text != null) tab.setText(text);
        addTab(tab);
        return tab;
    }

    public LiquidBottomTab addTabBitmap(Bitmap bitmap, String text) {
        LiquidBottomTab tab = new LiquidBottomTab(getContext());
        tab.setIconBitmap(bitmap);
        if (text != null) tab.setText(text);
        addTab(tab);
        return tab;
    }

    public LiquidBottomTab addTabDrawable(Drawable drawable, String text) {
        LiquidBottomTab tab = new LiquidBottomTab(getContext());
        tab.setIconDrawable(drawable);
        if (text != null) tab.setText(text);
        addTab(tab);
        return tab;
    }

    public LiquidBottomTab addTab(Object config) {
        if (config instanceof LiquidBottomTab) {
            addTab((LiquidBottomTab) config);
            return (LiquidBottomTab) config;
        }
        LiquidBottomTab tab = new LiquidBottomTab(getContext());
        tab.applyConfig(config);
        addTab(tab);
        return tab;
    }

    public LiquidBottomTab getTabAt(int index) {
        if (index < 0 || index >= visibleRow.getChildCount()) return null;
        View v = visibleRow.getChildAt(index);
        return v instanceof LiquidBottomTab ? (LiquidBottomTab) v : null;
    }

    public int getTabCount() { return visibleRow.getChildCount(); }
    public int getSelectedIndex() { return selectedIndex; }
    public int getSelectedLuaIndex() { return selectedIndex + 1; }
    public LiquidBottomTab getLuaTabAt(int oneBasedIndex) { return getTabAt(oneBasedIndex - 1); }

    public void removeTabAt(int index) {
        if (index < 0 || index >= getTabCount()) return;
        visibleRow.removeViewAt(index);
        captureRow.invalidate();
        selectedIndex = Math.min(selectedIndex, Math.max(0, getTabCount() - 1));
        rebindTabClicks();
        rebuildDragAnimation(selectedIndex);
        configureKtLayers();
        requestLayout();
    }

    public void clearTabs() {
        visibleRow.removeAllViews();
        captureRow.invalidate();
        selectedIndex = 0;
        rawDragOffsetPx = 0f;
        panelOffsetSpring.snapTo(0f);
        rebuildDragAnimation(0f);
        configureKtLayers();
        requestLayout();
        invalidate();
    }

    public void setTabs(Object table) {
        List<Object> items = LuaTableBridge.toList(table);
        clearTabs();
        int i;
        for (i = 0; i < items.size(); i++) {
            Object item = items.get(i);
            if (item == null) continue;
            addTab(item);
        }
        if (getTabCount() > 0) setSelectedIndex(Math.min(selectedIndex, getTabCount() - 1), false);
    }

    public boolean setTabPng(int index, String pngPath) {
        LiquidBottomTab tab = getTabAt(index);
        if (tab == null) return false;
        boolean ok = tab.setIconPng(pngPath);
        if (ok) syncCaptureTab(index);
        return ok;
    }

    public boolean setTabAsset(int index, String assetPath) {
        LiquidBottomTab tab = getTabAt(index);
        if (tab == null) return false;
        boolean ok = tab.setIconAsset(assetPath);
        if (ok) syncCaptureTab(index);
        return ok;
    }

    public boolean setTabUri(int index, String uri) {
        LiquidBottomTab tab = getTabAt(index);
        if (tab == null) return false;
        boolean ok = tab.setIconUri(uri);
        if (ok) syncCaptureTab(index);
        return ok;
    }

    public boolean setTabBytes(int index, byte[] bytes) {
        LiquidBottomTab tab = getTabAt(index);
        if (tab == null) return false;
        boolean ok = tab.setIconBytes(bytes);
        if (ok) syncCaptureTab(index);
        return ok;
    }

    public void setTabBitmap(int index, Bitmap bitmap) {
        LiquidBottomTab tab = getTabAt(index);
        if (tab != null) { tab.setIconBitmap(bitmap); syncCaptureTab(index); }
    }

    public void setTabDrawable(int index, Drawable drawable) {
        LiquidBottomTab tab = getTabAt(index);
        if (tab != null) { tab.setIconDrawable(drawable); syncCaptureTab(index); }
    }

    public void setTabResource(int index, int drawableResId) {
        LiquidBottomTab tab = getTabAt(index);
        if (tab != null) { tab.setIconResource(drawableResId); syncCaptureTab(index); }
    }

    public void setTabConfig(int index, Object config) {
        LiquidBottomTab tab = getTabAt(index);
        if (tab != null) { tab.applyConfig(config); syncCaptureTab(index); }
    }

    public void setTabIcon(int index, Object icon) {
        LiquidBottomTab tab = getTabAt(index);
        if (tab != null) { tab.setIconObject(icon); syncCaptureTab(index); }
    }

    public void setTabText(int index, CharSequence text) {
        LiquidBottomTab tab = getTabAt(index);
        if (tab != null) { tab.setText(text); syncCaptureTab(index); }
    }

    public String getTabIconLoadError(int index) {
        LiquidBottomTab tab = getTabAt(index);
        return tab == null ? "tab not found" : tab.getLastIconLoadError();
    }

    public boolean setLuaTabPng(int oneBasedIndex, String pngPath) { return setTabPng(oneBasedIndex - 1, pngPath); }
    public String getLuaTabIconLoadError(int oneBasedIndex) { return getTabIconLoadError(oneBasedIndex - 1); }
    public boolean setLuaTabAsset(int oneBasedIndex, String assetPath) { return setTabAsset(oneBasedIndex - 1, assetPath); }
    public boolean setLuaTabUri(int oneBasedIndex, String uri) { return setTabUri(oneBasedIndex - 1, uri); }
    public boolean setLuaTabBytes(int oneBasedIndex, byte[] bytes) { return setTabBytes(oneBasedIndex - 1, bytes); }
    public void setLuaTabResource(int oneBasedIndex, int resId) { setTabResource(oneBasedIndex - 1, resId); }
    public void setLuaTabBitmap(int oneBasedIndex, Bitmap bitmap) { setTabBitmap(oneBasedIndex - 1, bitmap); }
    public void setLuaTabDrawable(int oneBasedIndex, Drawable drawable) { setTabDrawable(oneBasedIndex - 1, drawable); }
    public void setLuaTabConfig(int oneBasedIndex, Object config) { setTabConfig(oneBasedIndex - 1, config); }
    public void setLuaTabIcon(int oneBasedIndex, Object icon) { setTabIcon(oneBasedIndex - 1, icon); }
    public void setLuaTabText(int oneBasedIndex, CharSequence text) { setTabText(oneBasedIndex - 1, text); }

    private void syncCaptureTab(int index) {
        // The hidden KT row is now a second render pass of the visible tab visuals.
        // No cloned TextView/ImageView hierarchy exists to become stale.
        captureRow.invalidate();
        captureGlass.invalidate();
    }

    public void refreshCaptureTabs() {
        captureRow.invalidate();
        captureGlass.invalidate();
    }

    private void rebindTabClicks() {
        int i;
        for (i = 0; i < visibleRow.getChildCount(); i++) {
            final int index = i;
            View child = visibleRow.getChildAt(i);
            child.setOnClickListener(new OnClickListener() {
                public void onClick(View v) { selectFromUser(index); }
            });
        }
    }

    /* --------------------------- Selection / gesture -------------------------- */

    public void setOnTabSelectedListener(OnTabSelectedListener listener) { selectedListener = listener; }
    public OnTabSelectedListener getOnTabSelectedListener() { return selectedListener; }

    public void setSelectedIndex(int index) { setSelectedIndex(index, true); }

    public void setSelectedIndex(int index, boolean animated) {
        int count = getTabCount();
        if (count <= 0) { selectedIndex = 0; return; }
        int value = Math.max(0, Math.min(count - 1, index));
        selectedIndex = value;
        if (animated) {
            dragAnimation.animateToValue(value);
        } else {
            rebuildDragAnimation(value);
            configureKtLayers();
        }
        invalidate();
    }

    public void setSelectedLuaIndex(int oneBasedIndex) { setSelectedIndex(oneBasedIndex - 1, true); }
    public void setSelectedLuaIndex(int oneBasedIndex, boolean animated) { setSelectedIndex(oneBasedIndex - 1, animated); }

    private void selectFromUser(int index) {
        setSelectedIndex(index, true);
        if (selectedListener != null) selectedListener.onTabSelected(this, selectedIndex);
    }

    private boolean handleIndicatorTouch(MotionEvent event) {
        if (!isEnabled() || getTabCount() <= 0) return false;
        int action = event.getActionMasked();

        if (action == MotionEvent.ACTION_DOWN) {
            int ai = event.getActionIndex();
            activePointerId = event.getPointerId(ai);
            lastRawX = rawX(event, ai);
            gestureDownRawX = event.getRawX();
            gestureDownRawY = event.getRawY();
            gestureMoved = false;
            dragging = true;
            interactiveHighlight.press(event.getX(ai), event.getY(ai));
            dragAnimation.press();
            panelOffsetSpring.snapTo(rawDragOffsetPx);
            if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
            return true;
        }

        if (action == MotionEvent.ACTION_MOVE) {
            if (!dragging || activePointerId == INVALID_POINTER_ID) return true;
            int pi = event.findPointerIndex(activePointerId);
            if (pi < 0) return true;
            float rx = rawX(event, pi);
            float dx = rx - lastRawX;
            lastRawX = rx;
            float dyFromDown = event.getRawY() - gestureDownRawY;
            float dxFromDown = event.getRawX() - gestureDownRawX;
            if (dxFromDown * dxFromDown + dyFromDown * dyFromDown > touchSlop * touchSlop) {
                gestureMoved = true;
            }
            float direction = getLayoutDirection() == LAYOUT_DIRECTION_RTL ? -1f : 1f;
            float target = dragAnimation.getTargetValue() + dx / getTabWidth() * direction;
            dragAnimation.updateValue(clamp(target, 0f, getTabCount() - 1f));
            rawDragOffsetPx += dx;
            panelOffsetSpring.snapTo(rawDragOffsetPx);
            interactiveHighlight.move(event.getX(pi), event.getY(pi));
            applyKtAnimationState();
            return true;
        }

        if (action == MotionEvent.ACTION_POINTER_UP) {
            int upIndex = event.getActionIndex();
            if (event.getPointerId(upIndex) == activePointerId) {
                int other = findOtherPointer(event, upIndex);
                if (other >= 0) {
                    // DragGestureInspector.kt hands the gesture to another pressed pointer.
                    activePointerId = event.getPointerId(other);
                    lastRawX = rawX(event, other);
                } else {
                    finishIndicatorGesture();
                }
            }
            return true;
        }

        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            if (dragging || activePointerId != INVALID_POINTER_ID) finishIndicatorGesture();
            return true;
        }

        return true;
    }

    private void finishIndicatorGesture() {
        boolean wasTap = !gestureMoved;
        dragging = false;
        activePointerId = INVALID_POINTER_ID;
        interactiveHighlight.release();

        int target = Math.round(dragAnimation.getTargetValue());
        target = Math.max(0, Math.min(getTabCount() - 1, target));
        selectedIndex = target;

        // onDragStopped() calls animateToValue(target); inspectDragGestures then release().
        dragAnimation.animateToValue(target);

        panelOffsetSpring.animateTo(0f);
        schedulePanelOffset();
        if (selectedListener != null) selectedListener.onTabSelected(this, target);
        if (wasTap) performClick();
    }

    private static int findOtherPointer(MotionEvent event, int excluded) {
        int i;
        for (i = 0; i < event.getPointerCount(); i++) if (i != excluded) return i;
        return -1;
    }

    private static float rawX(MotionEvent event, int pointerIndex) {
        if (pointerIndex < 0 || pointerIndex >= event.getPointerCount()) return event.getRawX();
        float windowOffset = event.getRawX() - event.getX(0);
        return event.getX(pointerIndex) + windowOffset;
    }

    private float getPanelOffsetPx() {
        if (getWidth() <= 0) return 0f;
        float fraction = clamp(rawDragOffsetPx / getWidth(), -1f, 1f);
        float sign = fraction < 0f ? -1f : (fraction > 0f ? 1f : 0f);
        return dpF(4f) * sign * easeOut(Math.abs(fraction));
    }

    private void schedulePanelOffset() {
        if (panelOffsetPosted) return;
        panelOffsetPosted = true;
        postOnAnimation(this);
    }

    public void run() {
        panelOffsetPosted = false;
        long now = SystemClock.uptimeMillis();
        float dt = lastPanelOffsetFrame == 0L ? 1f / 60f : (now - lastPanelOffsetFrame) / 1000f;
        lastPanelOffsetFrame = now;
        boolean active = panelOffsetSpring.step(dt);
        rawDragOffsetPx = panelOffsetSpring.value;
        applyKtAnimationState();
        if (active) schedulePanelOffset(); else lastPanelOffsetFrame = 0L;
    }

    /* ------------------------------- Backdrop -------------------------------- */

    public void setBackdrop(Backdrop backdrop) {
        baseBackdrop = backdrop == null ? EmptyBackdrop.INSTANCE : backdrop;
        explicitBackdrop = true;
        updateBackdrops();
    }

    public Backdrop getBackdrop() { return baseBackdrop; }

    public void clearExplicitBackdrop() {
        explicitBackdrop = false;
        resolveAutomaticBackdrop();
    }

    public void useAutoBackdrop() {
        autoBackdropEnabled = true;
        explicitBackdrop = false;
        resolveAutomaticBackdrop();
    }

    public void setAutoBackdropEnabled(boolean enabled) {
        autoBackdropEnabled = enabled;
        if (!explicitBackdrop) resolveAutomaticBackdrop();
    }

    public boolean isAutoBackdropEnabled() { return autoBackdropEnabled; }

    private void resolveAutomaticBackdrop() {
        if (explicitBackdrop) return;
        Backdrop resolved = null;
        if (autoBackdropEnabled) {
            android.view.ViewParent p = getParent();
            while (p instanceof View) {
                if (p instanceof LiquidGlassBackdropLayout) {
                    resolved = ((LiquidGlassBackdropLayout) p).getBackdrop();
                    break;
                }
                p = p.getParent();
            }
            if (resolved == null && isAttachedToWindow()) resolved = ActivityBackdropManager.getBackdrop(this);
        }
        baseBackdrop = resolved == null ? EmptyBackdrop.INSTANCE : resolved;
        updateBackdrops();
    }

    private void updateBackdrops() {
        // Internal layers have explicit backdrops, exactly like the three Compose
        // drawBackdrop() calls. Prevent LiquidGlassView auto-resolution from
        // replacing the indicator's CombinedBackdrop on attach.
        panel.setBackdrop(baseBackdrop);
        captureGlass.setBackdrop(baseBackdrop);
        indicator.setBackdrop(new CombinedBackdrop(baseBackdrop, tabsBackdrop));
    }

    /* ------------------------- Adaptive visible content ----------------------- */

    public void setAdaptiveContentColorEnabled(boolean enabled) {
        adaptiveContentColorEnabled = enabled;
        removeCallbacks(adaptiveRunnable);
        adaptivePosted = false;
        if (enabled) {
            lastAdaptiveSampleTime = 0L;
            scheduleAdaptive(0L);
        }
    }

    public boolean isAdaptiveContentColorEnabled() { return adaptiveContentColorEnabled; }

    public void setContentColor(int color) {
        setAdaptiveContentColorEnabled(false);
        contentColor = color;
        contentColorFrom = color;
        contentColorTarget = color;
        adaptiveAnimating = false;
        applyVisibleContentColor();
    }

    public int getContentColor() { return contentColor; }
    public float getLastBackdropLuminance() { return lastBackdropLuminance; }

    public void setLuminanceThreshold(float value) {
        luminanceThreshold = clamp(value, 0f, 1f);
        refreshAdaptiveContentColor();
    }
    public float getLuminanceThreshold() { return luminanceThreshold; }

    public void setAdaptiveContentColorAnimationDurationMs(long value) { contentColorAnimationMs = Math.max(0L, value); }
    public long getAdaptiveContentColorAnimationDurationMs() { return contentColorAnimationMs; }
    public void setAdaptiveContentColorSampleIntervalMs(long value) { adaptiveSampleIntervalMs = Math.max(16L, value); }
    public long getAdaptiveContentColorSampleIntervalMs() { return adaptiveSampleIntervalMs; }

    public void refreshAdaptiveContentColor() {
        lastAdaptiveSampleTime = 0L;
        if (adaptiveContentColorEnabled) scheduleAdaptive(0L);
    }

    private void applyVisibleContentColor() {
        int i;
        for (i = 0; i < visibleRow.getChildCount(); i++) {
            View v = visibleRow.getChildAt(i);
            if (v instanceof LiquidBottomTab) ((LiquidBottomTab) v).setGeneratedContentColor(contentColor);
        }
        visibleRow.invalidate();
        panel.invalidate();
    }

    private void scheduleAdaptive(long delayMs) {
        if (!adaptiveContentColorEnabled || !isAttachedToWindow() || adaptivePosted) return;
        adaptivePosted = true;
        if (delayMs <= 0L) post(adaptiveRunnable); else postDelayed(adaptiveRunnable, delayMs);
    }

    /* ----------------------------- Public tuning ------------------------------ */

    public int getAccentColor() { return accentColor; }
    public void setAccentColor(int color) {
        accentColor = color;
        captureRow.setAccentColor(color);
        captureRow.invalidate();
        captureGlass.invalidate();
    }

    public int getContainerColor() { return containerColor; }
    public void setContainerColor(int color) {
        containerColor = color;
        configureKtLayers();
    }

    public boolean isLightThemeDefaults() { return lightTheme; }

    public LiquidGlassView getPanelView() { return panel; }
    public LiquidGlassView getIndicatorView() { return indicator; }
    public LinearLayout getTabsRow() { return visibleRow; }
    public LiquidGlassView getTabsCaptureView() { return captureGlass; }
    public LayerBackdrop getTabsBackdrop() { return tabsBackdrop; }
    public InteractiveHighlight getInteractiveHighlight() { return interactiveHighlight; }
    public DampedDragAnimation getDragAnimation() { return dragAnimation; }
    public DampedDragAnimation getDampedDragAnimation() { return dragAnimation; }
    public float getIndicatorValue() { return dragAnimation == null ? 0f : dragAnimation.getValue(); }
    public float getDragOffset() { return rawDragOffsetPx; }
    public boolean isDragging() { return dragging; }

    // Compatibility API retained from earlier Java/Lua builds. Exact KT spring path
    // ignores linear fade durations and flash protection.
    public void setReleaseFlashProtection(boolean enabled) { interactiveHighlight.setReleaseFlashProtection(enabled); }
    public boolean isReleaseFlashProtectionEnabled() { return interactiveHighlight.isReleaseFlashProtectionEnabled(); }
    public void setPressHighlightFadeDurationsMs(long inMs, long outMs) { interactiveHighlight.setFadeDurationsMs(inMs, outMs); }
    public void setPressHighlightFadeInMs(long ms) { interactiveHighlight.setFadeInDurationMs(ms); }
    public long getPressHighlightFadeInMs() { return interactiveHighlight.getFadeInDurationMs(); }
    public void setPressHighlightFadeOutMs(long ms) { interactiveHighlight.setFadeOutDurationMs(ms); }
    public long getPressHighlightFadeOutMs() { return interactiveHighlight.getFadeOutDurationMs(); }
    public void setPressHighlightIntensity(float value) {
        pressHighlightIntensity = Math.max(0f, value);
        interactiveHighlight.setIntensityScale(pressHighlightIntensity);
        applyKtAnimationState();
    }
    public float getPressHighlightIntensity() { return pressHighlightIntensity; }
    public void setPressHighlightSpotAlpha(float alpha) { interactiveHighlight.setSpotAlpha(alpha); }
    public float getPressHighlightSpotAlpha() { return interactiveHighlight.getSpotAlpha(); }
    public void setPressHighlightBaseAlpha(float alpha) { interactiveHighlight.setBaseAlpha(alpha); }
    public float getPressHighlightBaseAlpha() { return interactiveHighlight.getBaseAlpha(); }

    /** Lua-table convenience without changing the original KT defaults. */
    public void applyConfig(Object table) {
        if (table == null) return;
        Object tabs = LuaTableBridge.getAny(table, new String[] {"tabs", "items", "data"});
        if (tabs != null) setTabs(tabs);

        Object backdropValue = LuaTableBridge.getAny(table, new String[] {"backdrop"});
        if (backdropValue instanceof Backdrop) setBackdrop((Backdrop) backdropValue);
        if (LuaTableBridge.getAny(table, new String[] {"autoBackdrop", "autoBackdropEnabled"}) != null)
            setAutoBackdropEnabled(LuaTableBridge.getBoolean(table,
                    new String[] {"autoBackdrop", "autoBackdropEnabled"}, autoBackdropEnabled));

        if (LuaTableBridge.getAny(table, new String[] {"accentColor", "accent"}) != null)
            setAccentColor(LuaTableBridge.getInt(table, new String[] {"accentColor", "accent"}, accentColor));
        if (LuaTableBridge.getAny(table, new String[] {"containerColor", "container"}) != null)
            setContainerColor(LuaTableBridge.getInt(table, new String[] {"containerColor", "container"}, containerColor));

        Object adaptive = LuaTableBridge.getAny(table, new String[] {"adaptiveContentColor", "autoContentColor", "autoForegroundColor"});
        if (adaptive != null) setAdaptiveContentColorEnabled(LuaTableBridge.getBoolean(table,
                new String[] {"adaptiveContentColor", "autoContentColor", "autoForegroundColor"}, adaptiveContentColorEnabled));
        Object manual = LuaTableBridge.getAny(table, new String[] {"contentColor", "foregroundColor", "tabContentColor"});
        if (manual != null) setContentColor(LuaTableBridge.getInt(table,
                new String[] {"contentColor", "foregroundColor", "tabContentColor"}, contentColor));
        if (LuaTableBridge.getAny(table, new String[] {"luminanceThreshold"}) != null)
            setLuminanceThreshold(LuaTableBridge.getFloat(table, new String[] {"luminanceThreshold"}, luminanceThreshold));
        if (LuaTableBridge.getAny(table, new String[] {"contentColorAnimationMs", "adaptiveColorAnimationMs"}) != null)
            setAdaptiveContentColorAnimationDurationMs(LuaTableBridge.getInt(table,
                    new String[] {"contentColorAnimationMs", "adaptiveColorAnimationMs"}, (int) contentColorAnimationMs));

        Object luaIndex = LuaTableBridge.getAny(table, new String[] {"selectedLuaIndex", "luaIndex"});
        if (luaIndex != null) setSelectedLuaIndex(LuaTableBridge.getInt(table,
                new String[] {"selectedLuaIndex", "luaIndex"}, selectedIndex + 1), false);
        else if (LuaTableBridge.getAny(table, new String[] {"selectedIndex", "selectedTabIndex", "index"}) != null)
            setSelectedIndex(LuaTableBridge.getInt(table,
                    new String[] {"selectedIndex", "selectedTabIndex", "index"}, selectedIndex), false);

        if (LuaTableBridge.getAny(table, new String[] {"pressHighlightIntensity", "highlightIntensity"}) != null)
            setPressHighlightIntensity(LuaTableBridge.getFloat(table,
                    new String[] {"pressHighlightIntensity", "highlightIntensity"}, pressHighlightIntensity));
        if (LuaTableBridge.getAny(table, new String[] {"pressHighlightSpotAlpha", "highlightSpotAlpha"}) != null)
            setPressHighlightSpotAlpha(LuaTableBridge.getFloat(table,
                    new String[] {"pressHighlightSpotAlpha", "highlightSpotAlpha"}, getPressHighlightSpotAlpha()));
        if (LuaTableBridge.getAny(table, new String[] {"pressHighlightBaseAlpha", "highlightBaseAlpha"}) != null)
            setPressHighlightBaseAlpha(LuaTableBridge.getFloat(table,
                    new String[] {"pressHighlightBaseAlpha", "highlightBaseAlpha"}, getPressHighlightBaseAlpha()));
    }

    /* ------------------------------ View layout ------------------------------- */

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        AncestorClipManager.attach(this);
        resolveAutomaticBackdrop();
        applyVisibleContentColor();
        if (adaptiveContentColorEnabled) {
            lastAdaptiveSampleTime = 0L;
            scheduleAdaptive(0L);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        ActivityBackdropManager.release(this);
        removeCallbacks(adaptiveRunnable);
        adaptivePosted = false;
        removeCallbacks(this);
        panelOffsetPosted = false;
        AncestorClipManager.detach(this);
        super.onDetachedFromWindow();
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    @Override
    public void draw(Canvas canvas) {
        if (ActivityBackdropManager.isCapturingBackdrop()) return;
        super.draw(canvas);
    }

    /** The hidden captureGlass stays attached/laid out but is not painted to screen. */
    @Override
    protected void dispatchDraw(Canvas canvas) {
        long time = getDrawingTime();
        drawChild(canvas, panel, time);
        drawChild(canvas, indicator, time);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int desiredW = dp(320f);
        int desiredH = dp(64f);
        int w = resolveSize(desiredW, widthMeasureSpec);
        int h = resolveSize(desiredH, heightMeasureSpec);
        setMeasuredDimension(w, h);

        int panelH = dp(64f);
        int innerH = dp(56f);
        int rowW = Math.max(0, w - dp(8f));
        int indicatorW = Math.max(1, Math.round((w - dpF(8f)) / Math.max(1, getTabCount())));

        panel.measure(MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(panelH, MeasureSpec.EXACTLY));
        visibleRow.measure(MeasureSpec.makeMeasureSpec(rowW, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(innerH, MeasureSpec.EXACTLY));
        captureGlass.measure(MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(innerH, MeasureSpec.EXACTLY));
        captureRow.measure(MeasureSpec.makeMeasureSpec(rowW, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(innerH, MeasureSpec.EXACTLY));
        indicator.measure(MeasureSpec.makeMeasureSpec(indicatorW, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(innerH, MeasureSpec.EXACTLY));
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int panelH = dp(64f);
        int innerH = dp(56f);
        int top = Math.max(0, (getHeight() - panelH) / 2);
        int inset = dp(4f);

        panel.layout(0, top, getWidth(), top + panelH);
        visibleRow.layout(inset, inset, getWidth() - inset, panelH - inset);

        // Source view starts at the same visual y as the KT hidden 56dp Row.
        captureGlass.layout(0, top + inset, getWidth(), top + inset + innerH);
        captureRow.layout(inset, 0, getWidth() - inset, innerH);

        float tabWidth = getTabWidth();
        int left = getLayoutDirection() == LAYOUT_DIRECTION_RTL
                ? Math.round(getWidth() - inset - tabWidth)
                : inset;
        indicator.layout(left, top + inset, left + Math.round(tabWidth), top + inset + innerH);
        applyKtAnimationState();
    }

    private float getTabWidth() {
        return Math.max(1f, (getWidth() - dpF(8f)) / Math.max(1, getTabCount()));
    }

    /* -------------------------------- Helpers -------------------------------- */

    /**
     * Hidden tabsBackdrop source. It must participate in drawing/layout, but it
     * must NEVER participate in touch dispatch. In the Compose source the
     * alpha(0) duplicate content carries the same clickable lambdas; in the
     * Java re-port the cloned views have no user click action, so allowing this
     * hidden layer to receive touch would swallow clicks intended for the
     * visible tabs.
     */
    private static final class CaptureOnlyLiquidGlassView extends LiquidGlassView {
        CaptureOnlyLiquidGlassView(Context context) {
            super(context);
            setClickable(false);
            setFocusable(false);
        }

        @Override
        public boolean dispatchTouchEvent(MotionEvent event) {
            return false;
        }
    }

    /**
     * Capture-only equivalent of the KT alpha(0).layerBackdrop(tabsBackdrop) Row.
     * It re-renders the visible tabs in their local positions, applies the KT
     * LocalLiquidBottomTabScale to each tab, then applies ColorFilter.tint(accent).
     * The view has no touch participation and is never directly dispatched onscreen.
     */
    private static final class CaptureMirrorRow extends View {
        private final LinearLayout sourceRow;
        private final Paint layerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private int accentColor = Color.WHITE;
        private float captureScale = 1f;

        CaptureMirrorRow(Context context, LinearLayout source) {
            super(context);
            sourceRow = source;
            setClickable(false);
            setFocusable(false);
            setWillNotDraw(false);
        }

        void setAccentColor(int color) {
            if (accentColor == color) return;
            accentColor = color;
            invalidate();
        }

        void setCaptureScale(float scale) {
            if (captureScale == scale) return;
            captureScale = scale;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (sourceRow == null) return;

            layerPaint.setColorFilter(new PorterDuffColorFilter(accentColor, PorterDuff.Mode.SRC_IN));
            int layer = canvas.saveLayer(0f, 0f, getWidth(), getHeight(), layerPaint);

            int count = sourceRow.getChildCount();
            int i;
            for (i = 0; i < count; i++) {
                View child = sourceRow.getChildAt(i);
                if (child == null || child.getVisibility() != VISIBLE || child.getWidth() <= 0 || child.getHeight() <= 0) {
                    continue;
                }
                int save = canvas.save();
                float cx = child.getLeft() + child.getWidth() * 0.5f;
                float cy = child.getTop() + child.getHeight() * 0.5f;
                canvas.scale(captureScale, captureScale, cx, cy);
                canvas.translate(child.getLeft(), child.getTop());
                child.draw(canvas);
                canvas.restoreToCount(save);
            }

            canvas.restoreToCount(layer);
            layerPaint.setColorFilter(null);
        }
    }

    private static final class SolidSurface implements LiquidGlassView.DrawCallback {
        private final int color;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        SolidSurface(int color) { this.color = color; }
        public void draw(Canvas canvas, LiquidGlassView view, Path path) {
            paint.setColor(color);
            canvas.drawRect(0f, 0f, view.getWidth(), view.getHeight(), paint);
        }
    }

    private static boolean isDarkTheme(Context context) {
        return (context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
    }

    private float density() { return getResources().getDisplayMetrics().density; }
    private int dp(float value) { return Math.round(value * density()); }
    private float dpF(float value) { return value * density(); }
    private static float lerp(float a, float b, float t) { return a + (b - a) * t; }
    private static float clamp(float v, float min, float max) { return Math.max(min, Math.min(max, v)); }

    private static float easeOut(float x) {
        x = clamp(x, 0f, 1f);
        if (x == 0f || x == 1f) return x;
        float t = x;
        int i;
        for (i = 0; i < 8; i++) {
            float omt = 1f - t;
            float curveX = 3f * 0.58f * omt * t * t + t * t * t;
            float derivative = 6f * 0.58f * omt * t - 3f * 0.58f * t * t + 3f * t * t;
            float error = curveX - x;
            if (Math.abs(error) < 0.00001f || Math.abs(derivative) < 0.00001f) break;
            t = clamp(t - error / derivative, 0f, 1f);
        }
        float omt = 1f - t;
        return 3f * omt * t * t + t * t * t;
    }

    private static float fastOutSlowIn(float x) {
        x = clamp(x, 0f, 1f);
        if (x == 0f || x == 1f) return x;
        float t = x;
        int i;
        for (i = 0; i < 8; i++) {
            float omt = 1f - t;
            float curveX = 3f * 0.4f * omt * omt * t + 3f * 0.2f * omt * t * t + t * t * t;
            float derivative = 3f * 0.4f * (omt * omt - 2f * omt * t)
                    + 3f * 0.2f * (2f * omt * t - t * t) + 3f * t * t;
            float error = curveX - x;
            if (Math.abs(error) < 0.00001f || Math.abs(derivative) < 0.00001f) break;
            t = clamp(t - error / derivative, 0f, 1f);
        }
        float omt = 1f - t;
        return 3f * omt * t * t + t * t * t;
    }

    private static int lerpColor(int from, int to, float t) {
        t = clamp(t, 0f, 1f);
        int a = Math.round(Color.alpha(from) + (Color.alpha(to) - Color.alpha(from)) * t);
        int r = Math.round(Color.red(from) + (Color.red(to) - Color.red(from)) * t);
        int g = Math.round(Color.green(from) + (Color.green(to) - Color.green(from)) * t);
        int b = Math.round(Color.blue(from) + (Color.blue(to) - Color.blue(from)) * t);
        return Color.argb(a, r, g, b);
    }
}
