package com.liquidglass.java.miba;

import android.graphics.Canvas;

public final class CombinedBackdrop implements Backdrop {
    private final Backdrop[] backdrops;
    public CombinedBackdrop(Backdrop... backdrops) {
        this.backdrops = backdrops == null ? new Backdrop[0] : backdrops.clone();
    }
    public int size() { return backdrops.length; }
    public Backdrop get(int index) { return index < 0 || index >= backdrops.length ? null : backdrops[index]; }
    public Backdrop[] toArray() { return backdrops.clone(); }
    public boolean isCoordinatesDependent() {
        int i;
        for (i = 0; i < backdrops.length; i++) {
            if (backdrops[i] != null && backdrops[i].isCoordinatesDependent()) return true;
        }
        return false;
    }
    public void drawBackdrop(Canvas canvas, LiquidGlassView owner) {
        int i;
        for (i = 0; i < backdrops.length; i++) {
            if (backdrops[i] != null) backdrops[i].drawBackdrop(canvas, owner);
        }
    }
}
