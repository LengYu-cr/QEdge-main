package com.liquidglass.java.miba;

import android.graphics.Canvas;

/** Java counterpart of com.kyant.backdrop.Backdrop. */
public interface Backdrop {
    boolean isCoordinatesDependent();
    void drawBackdrop(Canvas canvas, LiquidGlassView owner);
}
