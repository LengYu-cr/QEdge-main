package com.liquidglass.java.miba;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.os.Build;
import android.os.SystemClock;
import android.view.View;

import java.util.ArrayList;

/**
 * Direct Java/View port of catalog/utils/InteractiveHighlight.kt.
 *
 * Default behavior intentionally follows the KT source:
 * - press progress: spring(0.5f, 300f, 0.001f)
 * - pointer position: spring(0.5f, 300f, Offset.VisibilityThreshold)
 * - press: progress -> 1 and position snapTo(down)
 * - drag: position snapTo(pointer)
 * - release/cancel: progress -> 0 and position -> startPosition
 * - draw only while raw progress > 0
 * - additive base white alpha 0.08 * progress
 * - additive radial white alpha 0.15 * progress
 * - fallback white alpha 0.25 * progress
 *
 * There is deliberately NO Java-only linear fade, release envelope, flash guard,
 * frozen spot, or separate visual-alpha state in the default path.
 */
public final class InteractiveHighlight implements Runnable {

    public interface PositionProvider {
        void getPosition(float width, float height, float pointerX, float pointerY, float[] outXY);
    }

    /** Java-only redraw hook; it does not modify the KT animation values. */
    public interface FrameListener {
        void onInteractiveHighlightFrame(InteractiveHighlight highlight);
    }

    public static final float KT_PRESS_DAMPING_RATIO = 0.5f;
    public static final float KT_PRESS_STIFFNESS = 300f;
    public static final float KT_PRESS_VISIBILITY_THRESHOLD = 0.001f;
    public static final float KT_POSITION_DAMPING_RATIO = 0.5f;
    public static final float KT_POSITION_STIFFNESS = 300f;
    /* Offset.VisibilityThreshold is represented per axis in this Java port. */
    public static final float KT_POSITION_VISIBILITY_THRESHOLD = 0.5f;
    public static final float KT_BASE_ALPHA = 0.08f;
    public static final float KT_SPOT_ALPHA = 0.15f;
    public static final float KT_FALLBACK_ALPHA = 0.25f;
    public static final float KT_RADIUS_MULTIPLIER = 1.5f;

    /** Legacy duration constants kept only for binary/source compatibility. */
    public static final long DEFAULT_FADE_IN_MS = 0L;
    public static final long DEFAULT_FADE_OUT_MS = 0L;

    private final View host;
    private PositionProvider positionProvider;
    private FrameListener frameListener;

    private final SpringFloat pressProgressAnimation =
            new SpringFloat(0f, KT_PRESS_DAMPING_RATIO, KT_PRESS_STIFFNESS,
                    KT_PRESS_VISIBILITY_THRESHOLD);
    private final SpringFloat positionXAnimation =
            new SpringFloat(0f, KT_POSITION_DAMPING_RATIO, KT_POSITION_STIFFNESS,
                    KT_POSITION_VISIBILITY_THRESHOLD);
    private final SpringFloat positionYAnimation =
            new SpringFloat(0f, KT_POSITION_DAMPING_RATIO, KT_POSITION_STIFFNESS,
                    KT_POSITION_VISIBILITY_THRESHOLD);

    private float startX;
    private float startY;
    private long lastFrame;
    private boolean posted;
    private Object shader;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float[] resolved = new float[2];
    private final ArrayList<View> invalidationTargets = new ArrayList<View>();

    /* Optional Java/Lua tuning. Defaults are exactly the KT constants above. */
    private float baseAlpha = KT_BASE_ALPHA;
    private float spotAlpha = KT_SPOT_ALPHA;
    private float fallbackAlpha = KT_FALLBACK_ALPHA;
    private float intensityScale = 1f;
    private float radiusMultiplier = KT_RADIUS_MULTIPLIER;
    private boolean enabled = true;

    /* Compatibility-only fields retained so older Lua/Java code still links.
     * They do NOT alter the exact-KT spring path. */
    private boolean compatibilityFlashProtection;
    private long compatibilityFadeInMs;
    private long compatibilityFadeOutMs;

    public InteractiveHighlight(View host) {
        this(host, null);
    }

    public InteractiveHighlight(View host, PositionProvider provider) {
        if (host == null) throw new IllegalArgumentException("host == null");
        this.host = host;
        this.positionProvider = provider;
    }

    public View getHostView() { return host; }

    public void setFrameListener(FrameListener listener) { frameListener = listener; }
    public FrameListener getFrameListener() { return frameListener; }

    /** Register Views that actually draw this shared highlight (BottomTabs uses this). */
    public void addInvalidationTarget(View view) {
        if (view == null || view == host || invalidationTargets.contains(view)) return;
        invalidationTargets.add(view);
        view.invalidate();
    }

    public void removeInvalidationTarget(View view) {
        if (view != null) invalidationTargets.remove(view);
    }

    public void clearInvalidationTargets() { invalidationTargets.clear(); }

    /** Equivalent to KT onDragStart. */
    public void press(float px, float py) {
        if (!enabled) return;
        startX = px;
        startY = py;
        pressProgressAnimation.animateTo(1f);
        positionXAnimation.snapTo(startX);
        positionYAnimation.snapTo(startY);
        notifyFrame();
        schedule();
    }

    /** Equivalent to KT onDrag { positionAnimation.snapTo(change.position) }. */
    public void move(float px, float py) {
        if (!enabled) return;
        positionXAnimation.snapTo(px);
        positionYAnimation.snapTo(py);
        notifyFrame();
        if (pressProgressAnimation.value != pressProgressAnimation.target) schedule();
    }

    /** Equivalent to both KT onDragEnd and onDragCancel. */
    public void release() {
        if (!enabled) return;
        pressProgressAnimation.animateTo(0f);
        positionXAnimation.animateTo(startX);
        positionYAnimation.animateTo(startY);
        notifyFrame();
        schedule();
    }

    public void releaseFromCancel() { release(); }

    /** Java utility only; the KT gesture path does not call this. */
    public void cancel() {
        pressProgressAnimation.snapTo(0f);
        positionXAnimation.snapTo(startX);
        positionYAnimation.snapTo(startY);
        posted = false;
        lastFrame = 0L;
        host.removeCallbacks(this);
        notifyFrame();
    }

    /** Raw Animatable-equivalent value. Deliberately not clamped to 0..1. */
    public float getPressProgress() { return pressProgressAnimation.value; }
    public float getRawPressProgress() { return pressProgressAnimation.value; }
    public float getHighlightAlphaProgress() { return pressProgressAnimation.value; }
    public float getTargetPressProgress() { return pressProgressAnimation.target; }
    public boolean isPressedTarget() { return pressProgressAnimation.target == 1f; }
    public boolean isReleasing() {
        return pressProgressAnimation.target == 0f &&
                (pressProgressAnimation.value != 0f || pressProgressAnimation.velocity != 0f);
    }

    public float getOffsetX() { return positionXAnimation.value - startX; }
    public float getOffsetY() { return positionYAnimation.value - startY; }
    public float getPointerX() { return positionXAnimation.value; }
    public float getPointerY() { return positionYAnimation.value; }
    public float getHighlightX() { return positionXAnimation.value; }
    public float getHighlightY() { return positionYAnimation.value; }
    public float getMotionX() { return positionXAnimation.value; }
    public float getMotionY() { return positionYAnimation.value; }
    public float getStartX() { return startX; }
    public float getStartY() { return startY; }
    public boolean isAnimating() { return posted; }

    public boolean isHighlightVisible() {
        return enabled && pressProgressAnimation.value > 0f;
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean value) {
        enabled = value;
        if (!value) cancel();
        else notifyFrame();
    }

    public PositionProvider getPositionProvider() { return positionProvider; }
    public void setPositionProvider(PositionProvider provider) {
        positionProvider = provider;
        notifyFrame();
    }

    public void setIntensityScale(float scale) {
        intensityScale = Math.max(0f, scale);
        notifyFrame();
    }
    public float getIntensityScale() { return intensityScale; }

    public void setBaseAlpha(float alpha) { baseAlpha = Math.max(0f, alpha); notifyFrame(); }
    public float getBaseAlpha() { return baseAlpha; }
    public void setSpotAlpha(float alpha) { spotAlpha = Math.max(0f, alpha); notifyFrame(); }
    public float getSpotAlpha() { return spotAlpha; }
    public void setFallbackAlpha(float alpha) { fallbackAlpha = Math.max(0f, alpha); notifyFrame(); }
    public float getFallbackAlpha() { return fallbackAlpha; }
    public void setRadiusMultiplier(float multiplier) { radiusMultiplier = Math.max(0f, multiplier); notifyFrame(); }
    public float getRadiusMultiplier() { return radiusMultiplier; }

    public void resetKtIntensityDefaults() {
        baseAlpha = KT_BASE_ALPHA;
        spotAlpha = KT_SPOT_ALPHA;
        fallbackAlpha = KT_FALLBACK_ALPHA;
        radiusMultiplier = KT_RADIUS_MULTIPLIER;
        intensityScale = 1f;
        notifyFrame();
    }

    /* ---- Compatibility methods from the previous Java/Lua port ----
     * Exact-KT mode does not have linear durations or a flash-protection switch.
     * Keeping these methods prevents old Lua bridges from crashing, but they do
     * not modify the animation. */
    public void setReleaseFlashProtection(boolean value) { compatibilityFlashProtection = value; }
    public boolean isReleaseFlashProtectionEnabled() { return compatibilityFlashProtection; }
    public void setFadeInDurationMs(long value) { compatibilityFadeInMs = Math.max(0L, value); }
    public long getFadeInDurationMs() { return compatibilityFadeInMs; }
    public void setFadeOutDurationMs(long value) { compatibilityFadeOutMs = Math.max(0L, value); }
    public long getFadeOutDurationMs() { return compatibilityFadeOutMs; }
    public void setFadeDurationsMs(long inMs, long outMs) {
        setFadeInDurationMs(inMs);
        setFadeOutDurationMs(outMs);
    }

    /** Exact port of InteractiveHighlight.modifier drawWithContent pre-content drawing. */
    public void draw(Canvas canvas, float width, float height) {
        if (!enabled || canvas == null) return;

        final float progress = pressProgressAnimation.value;
        if (progress <= 0f) return;

        final float intensity = intensityScale;
        if (intensity <= 0f) return;

        if (Platform.isRuntimeShaderSupported()) {
            paint.reset();
            paint.setAntiAlias(true);
            paint.setColor(Color.argb(toAlpha(baseAlpha * progress * intensity), 255, 255, 255));
            Api29.plus(paint);
            canvas.drawRect(0f, 0f, width, height, paint);

            if (shader == null) shader = Api33.createShader();

            float px = positionXAnimation.value;
            float py = positionYAnimation.value;
            if (positionProvider != null) {
                positionProvider.getPosition(width, height, px, py, resolved);
                px = resolved[0];
                py = resolved[1];
            }

            /* KT fastCoerceIn(0f, size.width/height). */
            px = Math.max(0f, Math.min(width, px));
            py = Math.max(0f, Math.min(height, py));

            /*
             * Compose/KT feeds a translucent color into ShaderBrush.  When the
             * same RuntimeShader is used directly as an android.graphics.Paint
             * shader, keeping RGB at 1 while alpha is 0.15 can make PLUS look
             * much hotter on some HWUI paths because AGSL output is expected to
             * be premultiplied.  Keep the exact KT visual strength (0.15*p), but
             * apply it as Paint alpha.  The shader itself receives opaque white,
             * therefore color*intensity is premultiplied by construction:
             * [i, i, i, i].  Paint alpha then produces
             * [i*a, i*a, i*a, i*a].
             *
             * This changes only the moving radial spot.  KT_BASE_ALPHA (0.08),
             * radius, position, PLUS blend mode and all spring values stay exact.
             */
            Api33.configure(shader, width, height,
                    Math.min(width, height) * radiusMultiplier,
                    px, py);

            paint.setColor(Color.WHITE);
            paint.setAlpha(toAlpha(spotAlpha * progress * intensity));
            Api33.applyShader(paint, shader);
            Api29.plus(paint);
            canvas.drawRect(0f, 0f, width, height, paint);
            paint.setShader(null);
            paint.setAlpha(255);
        } else {
            paint.reset();
            paint.setAntiAlias(true);
            paint.setColor(Color.argb(toAlpha(fallbackAlpha * progress * intensity), 255, 255, 255));
            if (Build.VERSION.SDK_INT >= 29) Api29.plus(paint);
            else paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.ADD));
            canvas.drawRect(0f, 0f, width, height, paint);
        }
    }

    private void notifyFrame() {
        if (frameListener != null) frameListener.onInteractiveHighlightFrame(this);
        host.invalidate();
        int i;
        for (i = 0; i < invalidationTargets.size(); i++) {
            View view = invalidationTargets.get(i);
            if (view != null) view.invalidate();
        }
    }

    private void schedule() {
        if (!enabled || posted) return;
        posted = true;
        host.postOnAnimation(this);
    }

    public void run() {
        posted = false;
        if (!enabled) return;

        long now = SystemClock.uptimeMillis();
        float dt = lastFrame == 0L ? 1f / 60f : (now - lastFrame) / 1000f;
        lastFrame = now;

        boolean active = false;
        active |= pressProgressAnimation.step(dt);
        active |= positionXAnimation.step(dt);
        active |= positionYAnimation.step(dt);

        notifyFrame();
        if (active) schedule();
        else lastFrame = 0L;
    }

    private static int toAlpha(float alpha) {
        float a = alpha;
        if (a < 0f) a = 0f;
        if (a > 1f) a = 1f;
        return Math.round(a * 255f);
    }

    private static final class Api29 {
        private Api29() {}
        static void plus(Paint paint) { paint.setBlendMode(android.graphics.BlendMode.PLUS); }
    }

    private static final class Api33 {
        private Api33() {}
        static Object createShader() {
            return new android.graphics.RuntimeShader(Shaders.INTERACTIVE_HIGHLIGHT);
        }
        static void applyShader(Paint paint, Object object) {
            paint.setShader((android.graphics.RuntimeShader) object);
        }
        static void configure(Object object, float width, float height,
                              float radius, float x, float y) {
            android.graphics.RuntimeShader s = (android.graphics.RuntimeShader) object;
            s.setFloatUniform("size", width, height);
            /* Opaque white makes shader output color*intensity premultiplied. */
            s.setColorUniform("color", Color.WHITE);
            s.setFloatUniform("radius", radius);
            s.setFloatUniform("position", x, y);
        }
    }
}
