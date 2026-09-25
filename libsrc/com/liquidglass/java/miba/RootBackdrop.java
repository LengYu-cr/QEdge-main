package com.liquidglass.java.miba;

import android.graphics.Canvas;

public final class RootBackdrop implements Backdrop {
    private final LiquidGlassBackdropLayout root;
    RootBackdrop(LiquidGlassBackdropLayout root) { this.root = root; }
    public boolean isCoordinatesDependent() { return true; }
    public void drawBackdrop(Canvas canvas, LiquidGlassView owner) {
        root.drawRecordedBackdrop(canvas, owner);
    }
}
