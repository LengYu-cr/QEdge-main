package com.liquidglass.java.miba;

import android.os.SystemClock;
import android.view.View;

/** Behavioral port of catalog/utils/DampedDragAnimation.kt. */
public final class DampedDragAnimation implements Runnable {
    public interface Listener { void onAnimationFrame(DampedDragAnimation animation); }

    private final View host;
    private final float initialValue;
    private final float rangeStart;
    private final float rangeEnd;
    private final float visibilityThreshold;
    private final float initialScale;
    private final Listener listener;

    private final SpringFloat valueSpring;
    private final SpringFloat velocitySpring;
    /* Exact KT pressProgressAnimationSpec = spring(1f, 1000f, 0.001f). */
    private final SpringFloat pressProgressSpring;
    private final SpringFloat scaleXSpring;
    private final SpringFloat scaleYSpring;

    private long lastFrame;
    private boolean posted;
    private float lastObservedValue;
    private long lastObservedTime;
    private boolean releasePending;
    private boolean releaseWaitOneFrame;
    private boolean trackValueVelocity;
    private final float pressedScale;

    /* Compatibility-only values retained for Lua/Java callers from earlier ports.
     * They no longer control physical press motion; white highlight timing lives
     * in InteractiveHighlight. */
    private long compatibilityFadeInMs = InteractiveHighlight.DEFAULT_FADE_IN_MS;
    private long compatibilityFadeOutMs = InteractiveHighlight.DEFAULT_FADE_OUT_MS;

    public DampedDragAnimation(View host, float initialValue, float rangeStart, float rangeEnd,
                               float visibilityThreshold, float initialScale, float pressedScale,
                               Listener listener) {
        this.host = host;
        this.initialValue = initialValue;
        this.rangeStart = rangeStart;
        this.rangeEnd = rangeEnd;
        this.visibilityThreshold = visibilityThreshold;
        this.initialScale = initialScale;
        this.listener = listener;
        this.pressedScale = pressedScale;
        valueSpring = new SpringFloat(initialValue, 1f, 1000f, visibilityThreshold);
        velocitySpring = new SpringFloat(0f, 0.5f, 300f, visibilityThreshold * 10f);
        pressProgressSpring = new SpringFloat(0f, 1f, 1000f, 0.001f);
        scaleXSpring = new SpringFloat(initialScale, 0.6f, 250f, 0.001f);
        scaleYSpring = new SpringFloat(initialScale, 0.7f, 250f, 0.001f);
        lastObservedValue = initialValue;
        lastObservedTime = SystemClock.uptimeMillis();
    }

    public float getInitialValue() { return initialValue; }
    public float getRangeStart() { return rangeStart; }
    public float getRangeEnd() { return rangeEnd; }
    public float getVisibilityThreshold() { return visibilityThreshold; }
    public float getInitialScale() { return initialScale; }
    public float getPressedScale() { return pressedScale; }
    public boolean isAnimating() { return posted || releasePending; }
    public float getValue() { return valueSpring.value; }
    public float getTargetValue() { return valueSpring.target; }
    public float getProgress() {
        float d = rangeEnd - rangeStart;
        return d == 0f ? 0f : (valueSpring.value - rangeStart) / d;
    }
    public float getPressProgress() { return clamp01(pressProgressSpring.value); }
    public float getScaleX() { return scaleXSpring.value; }
    public float getScaleY() { return scaleYSpring.value; }
    public float getVelocity() { return velocitySpring.value; }

    public void setPressFadeInDurationMs(long value) { compatibilityFadeInMs = Math.max(0L, value); }
    public long getPressFadeInDurationMs() { return compatibilityFadeInMs; }
    public void setPressFadeOutDurationMs(long value) { compatibilityFadeOutMs = Math.max(0L, value); }
    public long getPressFadeOutDurationMs() { return compatibilityFadeOutMs; }
    public void setPressFadeDurationsMs(long fadeInMs, long fadeOutMs) {
        setPressFadeInDurationMs(fadeInMs);
        setPressFadeOutDurationMs(fadeOutMs);
    }

    public void press() {
        releasePending = false;
        releaseWaitOneFrame = false;
        lastObservedValue = valueSpring.value;
        lastObservedTime = SystemClock.uptimeMillis();
        pressProgressSpring.animateTo(1f);
        scaleXSpring.animateTo(pressedScale);
        scaleYSpring.animateTo(pressedScale);
        schedule();
    }

    public void release() {
        releasePending = true;
        releaseWaitOneFrame = true;
        schedule();
    }

    public void updateValue(float value) {
        trackValueVelocity = true;
        valueSpring.animateTo(clamp(value));
        schedule();
    }

    public void snapValue(float value) {
        trackValueVelocity = true;
        valueSpring.snapTo(clamp(value));
        updateMeasuredVelocity();
        notifyFrame();
    }

    public void animateToValue(float value) {
        press();
        settleToValue(value);
        release();
        schedule();
    }

    public void settleToValue(float value) {
        trackValueVelocity = false;
        valueSpring.animateTo(clamp(value));
        if (velocitySpring.value != 0f || velocitySpring.target != 0f) {
            velocitySpring.animateTo(0f);
        }
        schedule();
    }

    public void releasePressNow() {
        releasePending = false;
        releaseWaitOneFrame = false;
        pressProgressSpring.animateTo(0f);
        scaleXSpring.animateTo(initialScale);
        scaleYSpring.animateTo(initialScale);
        schedule();
    }

    private float clamp(float value) { return Math.max(rangeStart, Math.min(rangeEnd, value)); }

    private void schedule() {
        if (posted) return;
        posted = true;
        host.postOnAnimation(this);
    }

    public void run() {
        posted = false;
        long now = SystemClock.uptimeMillis();
        float dt = lastFrame == 0L ? 1f / 60f : (now - lastFrame) / 1000f;
        lastFrame = now;

        boolean active = false;
        active |= valueSpring.step(dt);
        if (trackValueVelocity) updateMeasuredVelocity();
        active |= velocitySpring.step(dt);

        if (releasePending) {
            if (releaseWaitOneFrame) {
                releaseWaitOneFrame = false;
            } else {
                float threshold = Math.abs(rangeEnd - rangeStart) * 0.025f;
                if (valueSpring.value == valueSpring.target
                        || Math.abs(valueSpring.value - valueSpring.target) < threshold) {
                    pressProgressSpring.animateTo(0f);
                    scaleXSpring.animateTo(initialScale);
                    scaleYSpring.animateTo(initialScale);
                    releasePending = false;
                }
            }
        }

        active |= pressProgressSpring.step(dt);
        active |= scaleXSpring.step(dt);
        active |= scaleYSpring.step(dt);
        if (releasePending) active = true;

        notifyFrame();
        if (active) schedule(); else lastFrame = 0L;
    }

    private void updateMeasuredVelocity() {
        long now = SystemClock.uptimeMillis();
        long delta = now - lastObservedTime;
        if (delta > 0L) {
            float range = rangeEnd - rangeStart;
            if (range != 0f) {
                float perSecond = (valueSpring.value - lastObservedValue) / (delta / 1000f) / range;
                velocitySpring.animateTo(perSecond);
            }
            lastObservedValue = valueSpring.value;
            lastObservedTime = now;
        }
    }

    private void notifyFrame() {
        if (listener != null) listener.onAnimationFrame(this);
        host.invalidate();
    }

    private static float clamp01(float value) { return Math.max(0f, Math.min(1f, value)); }
}
