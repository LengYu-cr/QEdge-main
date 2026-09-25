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
 * Direct View-system reconstruction of catalog/components/LiquidSlider.kt.
 *
 * KT visual geometry:
 * - outer BoxWithConstraints.fillMaxWidth(), contentAlignment = CenterStart
 * - track: full available width x 6dp
 * - glass thumb: 40dp x 24dp
 *
 * If a Lua layout gives the Slider a taller slot, the track and thumb are
 * centered vertically. The previous Java port placed the 24dp thumb at y=0,
 * which made its Portal/layer look much higher than the actual KT component.
 */
public class LiquidSlider extends ViewGroup implements LiquidGlassOverlay {
    private static final int INVALID_POINTER_ID = -1;

    public interface OnValueChangeListener {
        void onValueChange(LiquidSlider slider, float value);
    }

    private final TrackView track;
    private final LiquidGlassView thumb;
    private final LayerBackdrop trackBackdrop;

    private DampedDragAnimation animation;
    private float rangeStart = 0f;
    private float rangeEnd = 1f;
    private float visibilityThreshold = 0.001f;
    private float value;

    private boolean didDrag;
    private float lastRawX;
    private int activePointerId = INVALID_POINTER_ID;
    private OnValueChangeListener listener;

    private final boolean lightTheme;
    private int accentColor;
    private int trackColor;
    private float pressHighlightIntensity = 1f;

    private Backdrop baseBackdrop = EmptyBackdrop.INSTANCE;
    private boolean explicitBackdrop;
    private boolean autoBackdropEnabled = true;

    public LiquidSlider(Context context) {
        this(context, null);
    }

    public LiquidSlider(Context context, AttributeSet attrs) {
        super(context, attrs);
        setClipChildren(false);
        setClipToPadding(false);
        setWillNotDraw(true);

        lightTheme = !isDarkTheme(context);
        accentColor = lightTheme
                ? Color.rgb(0x00, 0x88, 0xFF)
                : Color.rgb(0x00, 0x91, 0xFF);
        trackColor = ColorUtils.withAlpha(
                lightTheme ? Color.rgb(0x78, 0x78, 0x78) : Color.rgb(0x78, 0x78, 0x80),
                lightTheme ? 0.20f : 0.36f);

        track = new TrackView(context);
        track.setClickable(true);
        addView(track);
        trackBackdrop = new LayerBackdrop(track);

        thumb = new LiquidGlassView(context);
        thumb.setShape(GlassShape.capsule());
        addView(thumb);

        rebuildAnimation(0f);
        configureKtThumb();

        thumb.setOnTouchListener(new OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                return handleThumbTouch(event);
            }
        });

        // LiquidSlider.kt detectTapGestures is attached to the 6dp track only.
        track.setOnTouchListener(new OnTouchListener() {
            private boolean down;

            public boolean onTouch(View v, MotionEvent event) {
                if (!isEnabled()) return false;
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        down = true;
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (!down) return false;
                        down = false;
                        float p = clamp(event.getX() / Math.max(1f, track.getWidth()), 0f, 1f);
                        if (getLayoutDirection() == LAYOUT_DIRECTION_RTL) p = 1f - p;
                        float target = rangeStart + (rangeEnd - rangeStart) * p;
                        value = clamp(target, rangeStart, rangeEnd);
                        animation.animateToValue(value);
                        notifyValue(value);
                        return true;
                    case MotionEvent.ACTION_CANCEL:
                        down = false;
                        return true;
                }
                return down;
            }
        });
    }

    /* ------------------------------ KT backdrop ------------------------------ */

    private void configureKtThumb() {
        final float d = density();
        thumb.setEffects(new LiquidGlassView.Effects() {
            public void apply(BackdropEffectScope scope) {
                float p = animation.getPressProgress();
                scope.blur(8f * d * (1f - p));
                scope.lens(10f * d * p, 14f * d * p, false, true);
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
                        float sx = lerp(2f / 3f, 1f, p);
                        float sy = lerp(0f, 1f, p);
                        canvas.scale(sx, sy, owner.getWidth() * 0.5f, owner.getHeight() * 0.5f);
                    }
                });
        thumb.setBackdrop(new CombinedBackdrop(baseBackdrop, transformedTrack));
    }

    /* ------------------------------- Value API ------------------------------- */

    public void setOnValueChangeListener(OnValueChangeListener value) {
        listener = value;
    }

    public OnValueChangeListener getOnValueChangeListener() {
        return listener;
    }

    public float getValue() {
        return value;
    }

    public void setValue(float newValue) {
        setValue(newValue, true);
    }

    public void setValue(float newValue, boolean animated) {
        value = clamp(newValue, rangeStart, rangeEnd);
        if (animated) {
            // KT snapshotFlow(value) -> dampedDragAnimation.updateValue(value)
            animation.updateValue(value);
        } else {
            animation.snapValue(value);
            applyAnimationState();
        }
    }

    public float getRangeStart() {
        return rangeStart;
    }

    public float getRangeEnd() {
        return rangeEnd;
    }

    public float getVisibilityThreshold() {
        return visibilityThreshold;
    }

    public void setValueRange(float start, float end) {
        setValueRange(start, end, visibilityThreshold);
    }

    public void setValueRange(float start, float end, float threshold) {
        if (end <= start) throw new IllegalArgumentException("end must be > start");
        rangeStart = start;
        rangeEnd = end;
        visibilityThreshold = threshold;
        value = clamp(value, start, end);
        rebuildAnimation(value);
        configureKtThumb();
        requestLayout();
        invalidate();
    }

    private void rebuildAnimation(float initial) {
        animation = new DampedDragAnimation(
                this,
                initial,
                rangeStart,
                rangeEnd,
                visibilityThreshold,
                1f,
                1.5f,
                new DampedDragAnimation.Listener() {
                    public void onAnimationFrame(DampedDragAnimation a) {
                        applyAnimationState();
                    }
                });
    }

    public DampedDragAnimation getDragAnimation() {
        return animation;
    }

    public DampedDragAnimation getDampedDragAnimation() {
        return animation;
    }

    public float getPressProgress() {
        return animation.getPressProgress();
    }

    public float getAnimatedValue() {
        return animation.getValue();
    }

    public float getAnimatedProgress() {
        return animation.getProgress();
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
        switch (event.getActionMasked()) {
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

                float delta = (rangeEnd - rangeStart) * (dx / Math.max(1f, getWidth()));
                if (getLayoutDirection() == LAYOUT_DIRECTION_RTL) delta = -delta;
                value = clamp(animation.getTargetValue() + delta, rangeStart, rangeEnd);
                animation.updateValue(value);
                notifyValue(value);
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
                        finishThumbGesture();
                    }
                }
                return true;
            }

            case MotionEvent.ACTION_UP:
                if (activePointerId != INVALID_POINTER_ID) {
                    activePointerId = INVALID_POINTER_ID;
                    finishThumbGesture();
                }
                return true;

            case MotionEvent.ACTION_CANCEL:
                if (activePointerId != INVALID_POINTER_ID) {
                    activePointerId = INVALID_POINTER_ID;
                    finishThumbGesture();
                }
                return true;
        }
        return true;
    }

    private void finishThumbGesture() {
        // LiquidSlider.kt only emits onValueChange on stop when there was a drag.
        if (didDrag) notifyValue(animation.getTargetValue());
        didDrag = false;
        animation.release();
    }

    private void notifyValue(float newValue) {
        if (listener != null) listener.onValueChange(this, newValue);
    }

    private void applyAnimationState() {
        float progress = animation.getProgress();
        // LiquidSlider.kt uses graphicsLayer { size.width } of the measured thumb.
        float thumbWidth = thumb.getWidth() > 0 ? thumb.getWidth() : dpF(40f);
        float raw = -thumbWidth * 0.5f + getWidth() * progress;
        float tx = clamp(
                raw,
                -thumbWidth * 0.25f,
                getWidth() - thumbWidth * 0.75f);
        if (getLayoutDirection() == LAYOUT_DIRECTION_RTL) tx = -tx;
        thumb.setTranslationX(tx);

        float velocity = animation.getVelocity() / 10f;
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
        int w = resolveSize(dp(280), widthMeasureSpec);
        int h = resolveSize(dp(24), heightMeasureSpec);
        setMeasuredDimension(w, h);

        track.measure(
                MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(dp(6), MeasureSpec.EXACTLY));
        thumb.measure(
                MeasureSpec.makeMeasureSpec(dp(40), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(dp(24), MeasureSpec.EXACTLY));
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int trackH = dp(6);
        int thumbW = dp(40);
        int thumbH = dp(24);
        int trackTop = (getHeight() - trackH) / 2;
        int thumbTop = (getHeight() - thumbH) / 2;

        track.layout(0, trackTop, getWidth(), trackTop + trackH);

        // Alignment.CenterStart: left in LTR, right in RTL.
        int thumbLeft = getLayoutDirection() == LAYOUT_DIRECTION_RTL
                ? getWidth() - thumbW
                : 0;
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
            float radius = getHeight() * 0.5f;
            paint.setColor(trackColor);
            canvas.drawRoundRect(0f, 0f, getWidth(), getHeight(), radius, radius, paint);

            // KT active track is a separate Capsule whose own measured width is
            // progress * fullWidth, so the moving end is rounded too.
            float p = clamp(animation.getProgress(), 0f, 1f);
            float fillWidth = getWidth() * p;
            if (fillWidth <= 0f) return;

            float fillRadius = Math.min(fillWidth, getHeight()) * 0.5f;
            paint.setColor(accentColor);
            if (getLayoutDirection() == LAYOUT_DIRECTION_RTL) {
                canvas.drawRoundRect(
                        getWidth() - fillWidth,
                        0f,
                        getWidth(),
                        getHeight(),
                        fillRadius,
                        fillRadius,
                        paint);
            } else {
                canvas.drawRoundRect(
                        0f,
                        0f,
                        fillWidth,
                        getHeight(),
                        fillRadius,
                        fillRadius,
                        paint);
            }
        }
    }

    /* ------------------------------- Lua API --------------------------------- */

    public LiquidSlider applyConfig(Object table) {
        if (table == null) return this;

        Object backdropValue = LuaTableBridge.getAny(table, new String[] {"backdrop"});
        if (backdropValue instanceof Backdrop) setBackdrop((Backdrop) backdropValue);

        if (LuaTableBridge.getAny(table, new String[] {"autoBackdrop", "autoBackdropEnabled"}) != null) {
            setAutoBackdropEnabled(LuaTableBridge.getBoolean(
                    table,
                    new String[] {"autoBackdrop", "autoBackdropEnabled"},
                    isAutoBackdropEnabled()));
        }

        Object valueListener = LuaTableBridge.getAny(table, new String[] {"onValueChange", "listener"});
        if (valueListener instanceof OnValueChangeListener) {
            setOnValueChangeListener((OnValueChangeListener) valueListener);
        }

        Object thumbConfig = LuaTableBridge.getAny(table, new String[] {"thumb", "thumbConfig"});
        if (thumbConfig != null) applyThumbConfig(thumbConfig);

        float start = LuaTableBridge.getFloat(
                table, new String[] {"rangeStart", "min", "minimum"}, rangeStart);
        float end = LuaTableBridge.getFloat(
                table, new String[] {"rangeEnd", "max", "maximum"}, rangeEnd);
        float threshold = LuaTableBridge.getFloat(
                table, new String[] {"visibilityThreshold", "threshold"}, visibilityThreshold);
        if (start != rangeStart || end != rangeEnd || threshold != visibilityThreshold) {
            setValueRange(start, end, threshold);
        }

        boolean animated = LuaTableBridge.getBoolean(table, new String[] {"animated", "animate"}, true);
        if (LuaTableBridge.getAny(table, new String[] {"value"}) != null) {
            setValue(LuaTableBridge.getFloat(table, new String[] {"value"}, value), animated);
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
