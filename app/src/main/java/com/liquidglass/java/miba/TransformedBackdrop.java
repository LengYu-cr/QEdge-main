package com.liquidglass.java.miba;

import android.graphics.Canvas;

/** Equivalent to rememberBackdrop(backdrop) { drawBackdrop -> transform { drawBackdrop() } }. */
public final class TransformedBackdrop implements Backdrop {
    public interface Transform { void beforeDraw(Canvas canvas, LiquidGlassView owner); }
    private final Backdrop source;
    private final Transform transform;
    public TransformedBackdrop(Backdrop source, Transform transform) {
        this.source = source;
        this.transform = transform;
    }
    public Backdrop getSource() { return source; }
    public Transform getTransform() { return transform; }
    public boolean isCoordinatesDependent() { return source != null && source.isCoordinatesDependent(); }
    public void drawBackdrop(Canvas canvas, LiquidGlassView owner) {
        if (source == null) return;
        int save = canvas.save();
        if (transform != null) transform.beforeDraw(canvas, owner);
        source.drawBackdrop(canvas, owner);
        canvas.restoreToCount(save);
    }
}
