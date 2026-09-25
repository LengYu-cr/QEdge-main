package com.liquidglass.java.miba;

import android.graphics.Canvas;
import android.graphics.RecordingCanvas;
import android.graphics.RenderNode;
import android.os.Build;
import android.view.View;

/** View-system version of the Kotlin LayerBackdrop. */
public final class LayerBackdrop implements Backdrop {
    private View source;
    private Object node;
    private View coordinateAnchor;

    public LayerBackdrop() {}
    public LayerBackdrop(View source) { this.source = source; }

    public void setSource(View source) {
        this.source = source;
        this.coordinateAnchor = null;
    }
    public View getSource() { return source; }
    public boolean hasExportedNode() { return node != null && source == null; }

    void setExportedNode(Object node, View coordinateAnchor) {
        this.node = node;
        this.coordinateAnchor = coordinateAnchor;
        this.source = null;
    }

    public boolean isCoordinatesDependent() { return true; }

    public void drawBackdrop(Canvas canvas, LiquidGlassView owner) {
        Object drawNode = node;
        View anchor = coordinateAnchor;
        if (source != null) anchor = source;
        if (anchor == null) return;

        int[] a = new int[2];
        int[] b = new int[2];
        owner.getLocationInWindow(a);
        anchor.getLocationInWindow(b);

        int save = canvas.save();
        owner.concatInverseLayerTransform(canvas);
        canvas.translate(b[0] - a[0], b[1] - a[1]);

        if (Platform.isRenderNodeSupported() && canvas.isHardwareAccelerated()) {
            if (source != null) {
                drawNode = Api29.recordSource(source, drawNode);
                node = drawNode;
            }
            if (drawNode != null) Api29.draw(canvas, drawNode);
        } else if (source != null) {
            // Pre-29 fallback. The original Android library supports older API
            // levels through Compose's private layer implementation; a direct
            // View draw is the public-API equivalent when RenderNode is absent.
            source.draw(canvas);
        }
        canvas.restoreToCount(save);
    }

    private static final class Api29 {
        private Api29() {}
        static Object recordSource(View source, Object old) {
            int w = source.getWidth();
            int h = source.getHeight();
            if (w <= 0 || h <= 0) return old;
            RenderNode n = old instanceof RenderNode ? (RenderNode) old : new RenderNode("LayerBackdrop");
            n.setPosition(0, 0, w, h);
            RecordingCanvas c = n.beginRecording(w, h);
            source.draw(c);
            n.endRecording();
            return n;
        }
        static void draw(Canvas canvas, Object node) {
            canvas.drawRenderNode((RenderNode) node);
        }
    }
}
