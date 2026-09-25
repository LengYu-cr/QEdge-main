package com.liquidglass.java.miba;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.RecordingCanvas;
import android.graphics.RenderNode;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

/**
 * Root recorder replacing Compose Modifier.layerBackdrop(rememberLayerBackdrop()).
 * Put the scene/background as normal children and liquid-glass components as overlay children.
 */
public class LiquidGlassBackdropLayout extends FrameLayout {
    private Object backdropNode;
    private final RootBackdrop backdrop = new RootBackdrop(this);

    public LiquidGlassBackdropLayout(Context context) { super(context); init(); }
    public LiquidGlassBackdropLayout(Context context, AttributeSet attrs) { super(context, attrs); init(); }
    public LiquidGlassBackdropLayout(Context context, AttributeSet attrs, int defStyleAttr) { super(context, attrs, defStyleAttr); init(); }

    private void init() {
        setClipChildren(false);
        setClipToPadding(false);
    }

    public RootBackdrop getBackdrop() { return backdrop; }

    /** Lua-friendly helpers; normal addView() remains available too. */
    public void addBackdropView(View view) {
        if (view != null) addView(view);
    }

    public void addBackdropView(View view, LayoutParams params) {
        if (view != null) addView(view, params);
    }

    public void addOverlayView(View view) {
        if (view != null) addView(view);
    }

    public void addOverlayView(View view, LayoutParams params) {
        if (view != null) addView(view, params);
    }

    public void invalidateBackdrop() {
        backdropNode = null;
        invalidate();
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        if (!Platform.isRenderNodeSupported() || !canvas.isHardwareAccelerated()) {
            super.dispatchDraw(canvas);
            return;
        }
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        backdropNode = Api29.record(this, backdropNode, w, h);
        Api29.draw(canvas, backdropNode);

        long time = getDrawingTime();
        int i;
        for (i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == VISIBLE && child instanceof LiquidGlassOverlay) {
                drawChild(canvas, child, time);
            }
        }
    }

    void drawRecordedBackdrop(Canvas canvas, LiquidGlassView owner) {
        int[] ownerLoc = new int[2];
        int[] rootLoc = new int[2];
        owner.getLocationInWindow(ownerLoc);
        getLocationInWindow(rootLoc);

        int save = canvas.save();
        owner.concatInverseLayerTransform(canvas);
        canvas.translate(rootLoc[0] - ownerLoc[0], rootLoc[1] - ownerLoc[1]);
        if (Platform.isRenderNodeSupported() && backdropNode != null && canvas.isHardwareAccelerated()) {
            Api29.draw(canvas, backdropNode);
        } else {
            long time = getDrawingTime();
            int i;
            for (i = 0; i < getChildCount(); i++) {
                View child = getChildAt(i);
                if (child.getVisibility() != VISIBLE) continue;
                if (child instanceof LiquidGlassOverlay) continue;
                drawChild(canvas, child, time);
            }
        }
        canvas.restoreToCount(save);
    }

    private static final class Api29 {
        private Api29() {}

        static Object record(LiquidGlassBackdropLayout root, Object old, int width, int height) {
            RenderNode node = old instanceof RenderNode ? (RenderNode) old : new RenderNode("LiquidGlassRootBackdrop");
            node.setPosition(0, 0, width, height);
            RecordingCanvas recording = node.beginRecording(width, height);
            long time = root.getDrawingTime();
            int i;
            for (i = 0; i < root.getChildCount(); i++) {
                View child = root.getChildAt(i);
                if (child.getVisibility() != VISIBLE) continue;
                if (child instanceof LiquidGlassOverlay) continue;
                root.drawChild(recording, child, time);
            }
            node.endRecording();
            return node;
        }

        static void draw(Canvas canvas, Object node) {
            canvas.drawRenderNode((RenderNode) node);
        }
    }
}
