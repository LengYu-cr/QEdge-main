package com.liquidglass.java.miba;

import android.graphics.Canvas;

public final class EmptyBackdrop implements Backdrop {
    public static final EmptyBackdrop INSTANCE = new EmptyBackdrop();
    private EmptyBackdrop() {}
    public static EmptyBackdrop getInstance() { return INSTANCE; }
    public boolean isCoordinatesDependent() { return false; }
    public void drawBackdrop(Canvas canvas, LiquidGlassView owner) {}
}
