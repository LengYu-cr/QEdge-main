package com.liquidglass.java.miba;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

/**
 * Direct View-system reconstruction of catalog/components/LiquidToggle.kt.
 *
 * KT visual geometry:
 * - outer Box: caller modifier, contentAlignment = CenterStart
 * - track: 64dp x 28dp
 * - glass thumb: 40dp x 24dp
 * - thumb travel: 20dp with 2dp edge padding
 *
 * The outer Java View is allowed to be taller/wider when Lua supplies explicit
 * layout params, but the 64x28 / 40x24 visual layers stay CenterStart exactly
 * like the Compose Box. This is important for AutoPortal: a 50dp/64dp layout
 * slot must not move the actual glass layer to the top of that slot.
 */
public class LiquidToggle extends ViewGroup implements LiquidGlassOverlay {
    private static final int INVALID_POINTER_ID = -1;

    public interface OnSelectListener {
        void onSelect(LiquidToggle toggle, boolean selected);
    }

    private final TrackView track;
    private final LiquidGlassView thumb;
    private final LayerBackdrop trackBackdrop;
    private final DampedDragAnimation animation;

    private boolean selected;
    private boolean didDrag;
    private float lastRawX;
    private int activePointerId = INVALID_POINTER_ID;
    private OnSelectListener listener;

    private final boolean lightTheme;
    private int accentColor;
    private int trackColor;
    private float pressHighlightIntensity = 1f;

    private Backdrop baseBackdrop = EmptyBackdrop.INSTANCE;
    private boolean explicitBackdrop;
    private boolean autoBackdropEnabled = true;

    public LiquidToggle(Context context) {
        this(context, null);
    }

    public LiquidToggle(Context context, AttributeSet attrs) {
        super(context, attrs);
        setClipChildren(false);
        setClipToPadding(false);
        setWillNotDraw(true);

        lightTheme = !isDarkTheme(context);
        accentColor = lightTheme
                ? Color.rgb(0x34, 0xC7, 0x59)
                : Color.rgb(0x30, 0xD1, 0x58);
        trackColor = ColorUtils.withAlpha(
                lightTheme ? Color.rgb(0x78, 0x78, 0x78) : Color.rgb(0x78, 0x78, 0x80),
                lightTheme ? 0.20f : 0.36f);

        track = new TrackView(context);
        addView(track);
        trackBackdrop = new LayerBackdrop(track);

        thumb = new LiquidGlassView(context);
        thumb.setShape(GlassShape.capsule());
        addView(thumb);

        animation = new DampedDragAnimation(
                this,
                0f,
                0f,
                1f,
                0.001f,
                1f,
                1.5f,
                new DampedDragAnimation.Listener() {
                    public void onAnimationFrame(DampedDragAnimation a) {
                        applyAnimationState();
                    }
                });

        configureKtThumb();
        thumb.setOnTouchListener(new OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                return handleThumbTouch(event);
            }
        });
    }

    /* ------------------------------ KT backdrop ------------------------------ */

    private void configureKtThumb() {
        final float d = density();
        thumb.setEffects(new LiquidGlassView.Effects() {
            public void apply(BackdropEffectScope scope) {
                float p = animation.getPressProgress();
                // LiquidToggle.kt: blur(8dp * (1-p))
                scope.blur(8f * d * (1f - p));
                // lens(5dp*p, 10dp*p, chromaticAberration = true)
                scope.lens(5f * d * p, 10f * d * p, false, true);
            }
        });
        thumb.setOnDrawSurface(new LiquidGlassView.DrawCallback() {
            private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

            public void draw(Canvas canvas, LiquidGlassView view, Path path) {
                float p = animation.getPressProgress();
                paint.setColor(Color.argb(
                        Math.round(255f * clamp(1f - p, 0f, 1f)),
                        255, 255, 255));
                canvas.drawRect(0f, 0f, view.getWidth(), view.getHeight(), paint);
            }
        });
        updateThumbBackdrop();
        applyAnimationState();
    }

    private void updateThumbBackdrop() {
        Backdrop transformedTrack = new TransformedBackdrop(
                trackBackdrop,
                new TransformedBackdrop.Transform() {
                    public void beforeDraw(Canvas canvas, LiquidGlassView owner) {
                        float p = animation.getPressProgress();
                        float sx = lerp(2f / 3f, 0.75f, p);
                        float sy = lerp(0f, 0.75f, p);
                        // DrawScope.scale() pivots around the draw-scope center.
                        canvas.scale(sx, sy, owner.getWidth() * 0.5f, owner.getHeight() * 0.5f);
                    }
                });
        // Explicit internal CombinedBackdrop, same as rememberCombinedBackdrop(...).
        thumb.setBackdrop(new CombinedBackdrop(baseBackdrop, transformedTrack));
    }

    /* ------------------------------- State API ------------------------------- */

    public void setOnSelectListener(OnSelectListener value) {
        listener = value;
    }

    public OnSelectListener getOnSelectListener() {
        return listener;
    }

    public boolean isSelected() {
        return selected;
    }

    public void toggle() {
        setSelected(!selected);
    }

    public void setSelected(boolean value) {
        setSelected(value, true);
    }

    public void setSelected(boolean value, boolean animated) {
        selected = value;
        float target = value ? 1f : 0f;
        if (animated) {
            // KT selected snapshotFlow -> animateToValue(target)
            animation.animateToValue(target);
        } else {
            animation.snapValue(target);
            applyAnimationState();
        }
    }

    public float getAnimatedValue() {
        return animation.getValue();
    }

    public float getPressProgress() {
        return animation.getPressProgress();
    }

    public DampedDragAnimation getDragAnimation() {
        return animation;
    }

    public DampedDragAnimation getDampedDragAnimation() {
        return animation;
    }

    public View getTrackView() {
        return track;
    }

    public LiquidGlassView getThumbView() {
        return thumb;
    }

    public LayerBackdrop getTrackBackdrop() {
        return trackBackdrop;
    }

    public int getAccentColor() {
        return accentColor;
    }

    public void setAccentColor(int color) {
        accentColor = color;
        track.invalidate();
    }

    public int getTrackColor() {
        return trackColor;
    }

    public void setTrackColor(int color) {
        trackColor = color;
        track.invalidate();
    }

    public boolean isLightThemeDefaults() {
        return lightTheme;
    }

    public float getPressHighlightIntensity() {
        return pressHighlightIntensity;
    }

    /** Java/Lua extension; 1f is exact KT. */
    public void setPressHighlightIntensity(float value) {
        pressHighlightIntensity = Math.max(0f, value);
        applyAnimationState();
    }

    public void setThumbHighlight(Highlight value) {
        thumb.setHighlight(value);
    }

    public Highlight getThumbHighlight() {
        return thumb.getHighlight();
    }

    public void setThumbShadow(Shadow value) {
        thumb.setShadow(value);
    }

    public Shadow getThumbShadow() {
        return thumb.getShadow();
    }

    public void setThumbInnerShadow(InnerShadow value) {
        thumb.setInnerShadow(value);
    }

    public InnerShadow getThumbInnerShadow() {
        return thumb.getInnerShadow();
    }

    public void setThumbEffects(LiquidGlassView.Effects value) {
        thumb.setEffects(value);
    }

    public void applyThumbConfig(Object table) {
        thumb.applyConfig(table);
    }

    /* ----------------------------- Backdrop API ------------------------------ */

    public void setBackdrop(Backdrop backdrop) {
        baseBackdrop = backdrop == null ? EmptyBackdrop.INSTANCE : backdrop;
        explicitBackdrop = true;
        updateThumbBackdrop();
    }

    public Backdrop getBackdrop() {
        return baseBackdrop;
    }

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

    public boolean isAutoBackdropEnabled() {
        return autoBackdropEnabled;
    }

    private void resolveAutomaticBackdrop() {
        if (explicitBackdrop) return;
        Backdrop resolved = null;
        if (autoBackdropEnabled) {
            ViewParent p = getParent();
            while (p instanceof View) {
                if (p instanceof LiquidGlassBackdropLayout) {
                    resolved = ((LiquidGlassBackdropLayout) p).getBackdrop();
                    break;
                }
                p = p.getParent();
            }
            if (resolved == null && isAttachedToWindow()) {
                resolved = ActivityBackdropManager.getBackdrop(this);
            }
        }
        baseBackdrop = resolved == null ? EmptyBackdrop.INSTANCE : resolved;
        updateThumbBackdrop();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        resolveAutomaticBackdrop();
    }

    @Override
    public void draw(Canvas canvas) {
        if (ActivityBackdropManager.isCapturingBackdrop()) return;
        super.draw(canvas);
    }

    /* ------------------------------ KT gesture ------------------------------- */

    private boolean handleThumbTouch(MotionEvent event) {
        if (!isEnabled()) return false;
        int action = event.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                int index = event.getActionIndex();
                activePointerId = event.getPointerId(index);
                lastRawX = rawX(event, index);
                didDrag = false;
                animation.press();
                ViewParent parent = getParent();
                if (parent != null) parent.requestDisallowInterceptTouchEvent(true);
                return true;
            }
            case MotionEvent.ACTION_POINTER_DOWN:
                return true;

            case MotionEvent.ACTION_MOVE: {
                int index = event.findPointerIndex(activePointerId);
                if (index < 0) return true;
                float now = rawX(event, index);
                float dx = now - lastRawX;
                lastRawX = now;
                if (dx != 0f) didDrag = true;
                float delta = dx / dpF(20f);
                if (getLayoutDirection() == LAYOUT_DIRECTION_RTL) delta = -delta;
                animation.updateValue(clamp(animation.getTargetValue() + delta, 0f, 1f));
                return true;
            }

            case MotionEvent.ACTION_POINTER_UP: {
                int up = event.getActionIndex();
                if (event.getPointerId(up) == activePointerId) {
                    int other = findOtherPointer(event, up);
                    if (other >= 0) {
                        activePointerId = event.getPointerId(other);
                        lastRawX = rawX(event, other);
                    } else {
                        activePointerId = INVALID_POINTER_ID;
                        finishGesture();
                    }
                }
                return true;
            }

            case MotionEvent.ACTION_UP:
                if (activePointerId != INVALID_POINTER_ID) {
                    activePointerId = INVALID_POINTER_ID;
                    finishGesture();
                }
                return true;

            case MotionEvent.ACTION_CANCEL:
                if (activePointerId != INVALID_POINTER_ID) {
                    activePointerId = INVALID_POINTER_ID;
                    // inspectDragGestures onDragCancel invokes onDragStopped too.
                    finishGesture();
                }
                return true;
        }
        return true;
    }

    private void finishGesture() {
        boolean newSelected;
        if (didDrag) {
            newSelected = animation.getTargetValue() >= 0.5f;
        } else {
            newSelected = !selected;
        }
        selected = newSelected;
        animation.updateValue(newSelected ? 1f : 0f);
        if (listener != null) listener.onSelect(this, selected);
        didDrag = false;
        animation.release();
    }

    private void applyAnimationState() {
        float value = animation.getValue();
        float padding = dpF(2f);
        float tx = lerp(padding, padding + dpF(20f), value);
        if (getLayoutDirection() == LAYOUT_DIRECTION_RTL) tx = -tx;
        thumb.setTranslationX(tx);

        float velocity = animation.getVelocity() / 50f;
        float sx = animation.getScaleX();
        float sy = animation.getScaleY();
        sx /= 1f - clamp(velocity * 0.75f, -0.2f, 0.2f);
        sy *= 1f - clamp(velocity * 0.25f, -0.2f, 0.2f);
        thumb.setScaleX(sx);
        thumb.setScaleY(sy);

        float p = animation.getPressProgress();
        float hp = clamp(p * pressHighlightIntensity, 0f, 1f);
        Highlight ambient = Highlight.AMBIENT;
        thumb.setHighlight(new Highlight(
                ambient.widthDp / 1.5f,
                ambient.blurRadiusDp / 1.5f,
                hp,
                ambient.style));
        thumb.setShadow(new Shadow(
                4f,
                Color.argb(Math.round(255f * 0.05f), 0, 0, 0)));
        thumb.setInnerShadow(new InnerShadow(4f * p, p));

        track.invalidate();
        thumb.invalidate();
    }

    /* ------------------------------ KT layout -------------------------------- */

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = resolveSize(dp(64), widthMeasureSpec);
        int h = resolveSize(dp(28), heightMeasureSpec);
        setMeasuredDimension(w, h);

        track.measure(
                MeasureSpec.makeMeasureSpec(dp(64), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(dp(28), MeasureSpec.EXACTLY));
        thumb.measure(
                MeasureSpec.makeMeasureSpec(dp(40), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(dp(24), MeasureSpec.EXACTLY));
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int trackW = dp(64);
        int trackH = dp(28);
        int thumbW = dp(40);
        int thumbH = dp(24);

        boolean rtl = getLayoutDirection() == LAYOUT_DIRECTION_RTL;
        int trackLeft = rtl ? getWidth() - trackW : 0;
        int thumbLeft = rtl ? getWidth() - thumbW : 0;
        int trackTop = (getHeight() - trackH) / 2;
        int thumbTop = (getHeight() - thumbH) / 2;

        track.layout(trackLeft, trackTop, trackLeft + trackW, trackTop + trackH);
        thumb.layout(thumbLeft, thumbTop, thumbLeft + thumbW, thumbTop + thumbH);
        applyAnimationState();
    }

    private final class TrackView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        TrackView(Context context) {
            super(context);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            float fraction = animation.getValue();
            paint.setColor(ColorUtils.lerp(trackColor, accentColor, fraction));
            float radius = getHeight() * 0.5f;
            canvas.drawRoundRect(0f, 0f, getWidth(), getHeight(), radius, radius, paint);
        }
    }

    /* ------------------------------- Lua API --------------------------------- */

    public LiquidToggle applyConfig(Object table) {
        if (table == null) return this;

        Object backdropValue = LuaTableBridge.getAny(table, new String[] {"backdrop"});
        if (backdropValue instanceof Backdrop) setBackdrop((Backdrop) backdropValue);

        if (LuaTableBridge.getAny(table, new String[] {"autoBackdrop", "autoBackdropEnabled"}) != null) {
            setAutoBackdropEnabled(LuaTableBridge.getBoolean(
                    table,
                    new String[] {"autoBackdrop", "autoBackdropEnabled"},
                    isAutoBackdropEnabled()));
        }

        Object selectListener = LuaTableBridge.getAny(table, new String[] {"onSelect", "onSelected", "listener"});
        if (selectListener instanceof OnSelectListener) {
            setOnSelectListener((OnSelectListener) selectListener);
        }

        Object thumbConfig = LuaTableBridge.getAny(table, new String[] {"thumb", "thumbConfig"});
        if (thumbConfig != null) applyThumbConfig(thumbConfig);

        boolean animated = LuaTableBridge.getBoolean(table, new String[] {"animated", "animate"}, true);
        if (LuaTableBridge.getAny(table, new String[] {"selected", "checked", "value"}) != null) {
            setSelected(LuaTableBridge.getBoolean(
                    table,
                    new String[] {"selected", "checked", "value"},
                    selected), animated);
        }

        if (LuaTableBridge.getAny(table, new String[] {"accentColor", "accent"}) != null) {
            setAccentColor(LuaTableBridge.getInt(
                    table, new String[] {"accentColor", "accent"}, accentColor));
        }
        if (LuaTableBridge.getAny(table, new String[] {"trackColor", "track"}) != null) {
            setTrackColor(LuaTableBridge.getInt(
                    table, new String[] {"trackColor", "track"}, trackColor));
        }
        if (LuaTableBridge.getAny(table, new String[] {"pressHighlightIntensity", "highlightIntensity"}) != null) {
            setPressHighlightIntensity(LuaTableBridge.getFloat(
                    table,
                    new String[] {"pressHighlightIntensity", "highlightIntensity"},
                    pressHighlightIntensity));
        }
        if (LuaTableBridge.getAny(table, new String[] {"enabled"}) != null) {
            setEnabled(LuaTableBridge.getBoolean(table, new String[] {"enabled"}, isEnabled()));
        }
        if (LuaTableBridge.getAny(table, new String[] {"alpha"}) != null) {
            setAlpha(LuaTableBridge.getFloat(table, new String[] {"alpha"}, getAlpha()));
        }
        return this;
    }

    /* -------------------------------- Helpers -------------------------------- */

    private static int findOtherPointer(MotionEvent event, int excluded) {
        int i;
        for (i = 0; i < event.getPointerCount(); i++) {
            if (i != excluded) return i;
        }
        return -1;
    }

    private static float rawX(MotionEvent event, int pointerIndex) {
        if (pointerIndex < 0 || pointerIndex >= event.getPointerCount()) return event.getRawX();
        float windowOffset = event.getRawX() - event.getX(0);
        return event.getX(pointerIndex) + windowOffset;
    }

    private float density() {
        return getResources().getDisplayMetrics().density;
    }

    private int dp(int value) {
        return Math.round(value * density());
    }

    private float dpF(float value) {
        return value * density();
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }

    private static boolean isDarkTheme(Context context) {
        return (context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
    }

    @Override
    protected void onDetachedFromWindow() {
        ActivityBackdropManager.release(this);
        super.onDetachedFromWindow();
    }
}
