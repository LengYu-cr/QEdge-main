package com.liquidglass.java.miba;

import android.graphics.Canvas;

public final class CanvasBackdrop implements Backdrop {
    public interface Drawer { void draw(Canvas canvas, LiquidGlassView owner); }
    private final Drawer drawer;
    public CanvasBackdrop(Drawer drawer) { this.drawer = drawer; }
    public Drawer getDrawer() { return drawer; }
    public boolean isCoordinatesDependent() { return false; }
    public void drawBackdrop(Canvas canvas, LiquidGlassView owner) {
        if (drawer != null) drawer.draw(canvas, owner);
    }
}
