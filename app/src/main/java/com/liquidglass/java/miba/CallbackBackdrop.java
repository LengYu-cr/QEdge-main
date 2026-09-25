package com.liquidglass.java.miba;

import android.graphics.Canvas;

/**
 * Java equivalent of rememberBackdrop(backdrop) { drawBackdrop -> ... }.
 * The callback decides when/if to invoke drawSource.run().
 */
public final class CallbackBackdrop implements Backdrop {
    public interface Drawer {
        void draw(Canvas canvas, LiquidGlassView owner, Runnable drawSource);
    }

    private final Backdrop source;
    private final Drawer drawer;

    public CallbackBackdrop(Backdrop source, Drawer drawer) {
        this.source = source == null ? EmptyBackdrop.INSTANCE : source;
        this.drawer = drawer;
    }

    public Backdrop getSource() { return source; }
    public Drawer getDrawer() { return drawer; }

    public boolean isCoordinatesDependent() {
        return source.isCoordinatesDependent();
    }

    public void drawBackdrop(final Canvas canvas, final LiquidGlassView owner) {
        final Runnable drawSource = new Runnable() {
            public void run() { source.drawBackdrop(canvas, owner); }
        };
        if (drawer != null) drawer.draw(canvas, owner, drawSource);
        else drawSource.run();
    }
}
