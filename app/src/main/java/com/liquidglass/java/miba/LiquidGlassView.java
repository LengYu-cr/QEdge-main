package com.liquidglass.java.miba;

import android.content.Context;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RenderEffect;
import android.graphics.RenderNode;
import android.graphics.RuntimeShader;
import android.graphics.Shader;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import java.util.HashMap;
import java.util.Map;

/**
 * View-system port of drawBackdrop(). Drawing order follows the Kotlin modifier chain:
 * outer shadow -> clipped (behind/backdrop/surface/content/front) -> highlight -> inner shadow.
 */
public class LiquidGlassView extends FrameLayout implements LiquidGlassOverlay {

    public interface Effects { void apply(BackdropEffectScope scope); }
    public interface DrawCallback { void draw(Canvas canvas, LiquidGlassView view, Path shapePath); }
    public interface BackdropDrawInterceptor {
        void draw(Canvas canvas, LiquidGlassView view, Runnable drawBackdrop);
    }

    private Backdrop backdrop = EmptyBackdrop.INSTANCE;
    private boolean explicitBackdrop;
    private boolean autoBackdropEnabled = true;
    private int convenienceSurfaceColor = Color.TRANSPARENT;
    private boolean hasConvenienceSurfaceColor;
    private GlassShape shape = new GlassShape(Float.MAX_VALUE);
    private Effects effects;
    private SimpleGlassEffects simpleEffects;

    private Highlight highlight = Highlight.DEFAULT;
    private Shadow shadow = Shadow.DEFAULT;
    private InnerShadow innerShadow;

    private DrawCallback onDrawBehind;
    private DrawCallback onDrawSurface;
    private DrawCallback onDrawContentBackground;
    /*
     * Dynamic overlay is drawn directly on the real Canvas, outside the clipped
     * content RenderNode. It is intended for per-frame press highlights whose
     * alpha must remain visible during release even when HWUI reuses child
     * display lists.
     */
    private DrawCallback onDrawDynamicOverlay;
    private DrawCallback onDrawFront;
    private BackdropDrawInterceptor onDrawBackdrop;

    private LayerBackdrop exportedBackdrop;

    private final BackdropEffectScope effectScope = new BackdropEffectScope();
    private final Map<String, Object> highlightShaderCache = new HashMap<String, Object>();

    private Object contentNode;
    private Object effectNode;
    private Object shadowNode;
    private Object highlightNode;
    private Object innerShadowNode;
    private Object exportedNode;

    private final Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint highlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint innerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint clearPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint layerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public LiquidGlassView(Context context) { super(context); init(); }
    public LiquidGlassView(Context context, AttributeSet attrs) { super(context, attrs); init(); }
    public LiquidGlassView(Context context, AttributeSet attrs, int defStyleAttr) { super(context, attrs, defStyleAttr); init(); }

    private void init() {
        setClipChildren(false);
        setClipToPadding(false);
        setWillNotDraw(true);
        clearPaint.setColor(Color.TRANSPARENT);
        clearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
    }

    /**
     * Explicitly sets the backdrop and disables automatic backdrop replacement
     * until clearExplicitBackdrop()/useAutoBackdrop() is called.
     */
    public void setBackdrop(Backdrop backdrop) {
        explicitBackdrop = true;
        this.backdrop = backdrop == null ? EmptyBackdrop.INSTANCE : backdrop;
        invalidate();
    }

    /** Internal/automatic assignment which does not become an explicit override. */
    void setResolvedBackdrop(Backdrop backdrop) {
        this.backdrop = backdrop == null ? EmptyBackdrop.INSTANCE : backdrop;
        invalidate();
    }

    public Backdrop getBackdrop() { return backdrop; }
    public void setBackdropSource(Backdrop backdrop) { setBackdrop(backdrop); }
    public Backdrop getBackdropSource() { return getBackdrop(); }

    /** Return to the Compose-like automatic backdrop behavior. */
    public void clearExplicitBackdrop() {
        explicitBackdrop = false;
        resolveAutomaticBackdrop();
    }

    public boolean hasExplicitBackdrop() { return explicitBackdrop; }

    public void setAutoBackdropEnabled(boolean enabled) {
        autoBackdropEnabled = enabled;
        if (enabled && !explicitBackdrop) resolveAutomaticBackdrop();
        else if (!enabled && !explicitBackdrop) setResolvedBackdrop(EmptyBackdrop.INSTANCE);
    }

    public boolean isAutoBackdropEnabled() { return autoBackdropEnabled; }

    /** Lua-friendly name: enable automatic backdrop and clear any explicit override. */
    public void useAutoBackdrop() {
        autoBackdropEnabled = true;
        explicitBackdrop = false;
        resolveAutomaticBackdrop();
    }

    private void resolveAutomaticBackdrop() {
        if (!autoBackdropEnabled || explicitBackdrop) return;
        Backdrop resolved = null;
        android.view.ViewParent parent = getParent();
        while (parent instanceof android.view.View) {
            if (parent instanceof LiquidGlassBackdropLayout) {
                resolved = ((LiquidGlassBackdropLayout) parent).getBackdrop();
                break;
            }
            parent = parent.getParent();
        }
        if (resolved == null && isAttachedToWindow()) {
            resolved = ActivityBackdropManager.getBackdrop(this);
        }
        setResolvedBackdrop(resolved == null ? EmptyBackdrop.INSTANCE : resolved);
    }

    public void setShape(GlassShape shape) {
        this.shape = shape == null ? new GlassShape(Float.MAX_VALUE) : shape;
        invalidate();
    }
    public GlassShape getShape() { return shape; }
    public void setCornerRadiusPx(float radiusPx) { setShape(new GlassShape(radiusPx)); }
    public void setCornerRadiiPx(float topLeftPx, float topRightPx, float bottomRightPx, float bottomLeftPx) {
        setShape(new GlassShape(topLeftPx, topRightPx, bottomRightPx, bottomLeftPx));
    }
    public void setCornerRadiusDp(float radiusDp) { setCornerRadiusPx(dp(radiusDp)); }
    public void setCornerRadiiDp(float topLeftDp, float topRightDp, float bottomRightDp, float bottomLeftDp) {
        setCornerRadiiPx(dp(topLeftDp), dp(topRightDp), dp(bottomRightDp), dp(bottomLeftDp));
    }
    public float[] getCornerRadiiPx() { return shape.getCornerRadii(getWidth(), getHeight()); }

    public void setEffects(Effects effects) { this.effects = effects; invalidate(); }
    public Effects getEffects() { return effects; }
    public void clearEffects() { setEffects(null); }

    /**
     * Lua-friendly mutable effect chain. Calling this makes the returned object
     * the active Effects implementation; mutations take effect after invalidate().
     */
    public SimpleGlassEffects useSimpleEffects() {
        if (simpleEffects == null) simpleEffects = new SimpleGlassEffects();
        if (effects != simpleEffects) setEffects(simpleEffects);
        return simpleEffects;
    }

    public SimpleGlassEffects getSimpleEffects() { return simpleEffects; }
    public void setSimpleEffects(SimpleGlassEffects value) {
        simpleEffects = value;
        setEffects(value);
    }

    public void setVibrancyEnabled(boolean enabled) {
        useSimpleEffects().setVibrancyEnabled(enabled);
        invalidate();
    }
    public boolean isVibrancyEnabled() { return simpleEffects != null && simpleEffects.isVibrancyEnabled(); }
    public void setBlurRadiusPx(float radiusPx) {
        useSimpleEffects().setBlurRadiusPx(radiusPx);
        invalidate();
    }
    public void setBlurRadiusDp(float radiusDp) { setBlurRadiusPx(dp(radiusDp)); }
    public float getBlurRadiusPx() { return simpleEffects == null ? 0f : simpleEffects.getBlurRadiusPx(); }
    public void clearBlur() { if (simpleEffects != null) { simpleEffects.clearBlur(); invalidate(); } }
    public void setLensPx(float heightPx, float amountPx) {
        useSimpleEffects().setLens(heightPx, amountPx);
        invalidate();
    }
    public void setLensPx(float heightPx, float amountPx, boolean depthEffect, boolean chromaticAberration) {
        useSimpleEffects().setLens(heightPx, amountPx, depthEffect, chromaticAberration);
        invalidate();
    }
    public void setLensDp(float heightDp, float amountDp) { setLensPx(dp(heightDp), dp(amountDp)); }
    public void setLensDp(float heightDp, float amountDp, boolean depthEffect, boolean chromaticAberration) {
        setLensPx(dp(heightDp), dp(amountDp), depthEffect, chromaticAberration);
    }
    public float getRefractionHeightPx() { return simpleEffects == null ? 0f : simpleEffects.getRefractionHeightPx(); }
    public float getRefractionAmountPx() { return simpleEffects == null ? 0f : simpleEffects.getRefractionAmountPx(); }
    public boolean isDepthEffectEnabled() { return simpleEffects != null && simpleEffects.isDepthEffect(); }
    public boolean isChromaticAberrationEnabled() { return simpleEffects != null && simpleEffects.isChromaticAberration(); }
    public void clearLens() { if (simpleEffects != null) { simpleEffects.clearLens(); invalidate(); } }
    public void setColorControls(float brightness, float contrast, float saturation) {
        useSimpleEffects().setColorControls(brightness, contrast, saturation);
        invalidate();
    }
    public void clearColorControls() { if (simpleEffects != null) { simpleEffects.clearColorControls(); invalidate(); } }
    public float getBrightness() { return simpleEffects == null ? 0f : simpleEffects.getBrightness(); }
    public float getContrast() { return simpleEffects == null ? 1f : simpleEffects.getContrast(); }
    public float getSaturation() { return simpleEffects == null ? 1f : simpleEffects.getSaturation(); }
    public void setBackdropOpacity(float alpha) {
        useSimpleEffects().setOpacity(alpha);
        invalidate();
    }
    public float getBackdropOpacity() { return simpleEffects == null ? 1f : simpleEffects.getOpacity(); }
    public void clearBackdropOpacity() { if (simpleEffects != null) { simpleEffects.clearOpacity(); invalidate(); } }
    public void applyEffectsConfig(Object table) { useSimpleEffects().applyConfig(table); invalidate(); }
    public void setHighlight(Highlight highlight) { this.highlight = highlight; invalidate(); }
    public Highlight getHighlight() { return highlight; }
    public void setHighlight(float widthDp, float blurRadiusDp, float alpha, HighlightStyle style) {
        setHighlight(new Highlight(widthDp, blurRadiusDp, alpha, style));
    }
    public void clearHighlight() { setHighlight(null); }

    public void setShadow(Shadow shadow) { this.shadow = shadow; invalidate(); }
    public Shadow getShadow() { return shadow; }
    public void setShadow(float radiusDp, float offsetXDp, float offsetYDp, int color, float alpha) {
        setShadow(new Shadow(radiusDp, offsetXDp, offsetYDp, color, alpha));
    }
    public void clearShadow() { setShadow(null); }

    public void setInnerShadow(InnerShadow innerShadow) { this.innerShadow = innerShadow; invalidate(); }
    public InnerShadow getInnerShadow() { return innerShadow; }
    public void setInnerShadow(float radiusDp, float offsetXDp, float offsetYDp, int color, float alpha) {
        setInnerShadow(new InnerShadow(radiusDp, offsetXDp, offsetYDp, color, alpha));
    }
    public void clearInnerShadow() { setInnerShadow(null); }

    public void setOnDrawBehind(DrawCallback callback) { onDrawBehind = callback; invalidate(); }
    public DrawCallback getOnDrawBehind() { return onDrawBehind; }
    public void setOnDrawSurface(DrawCallback callback) { onDrawSurface = callback; invalidate(); }
    public DrawCallback getOnDrawSurface() { return onDrawSurface; }
    /** Draws after surface but before child content; used by InteractiveHighlight.kt. */
    public void setOnDrawContentBackground(DrawCallback callback) { onDrawContentBackground = callback; invalidate(); }
    public DrawCallback getOnDrawContentBackground() { return onDrawContentBackground; }
    public void setOnDrawDynamicOverlay(DrawCallback callback) { onDrawDynamicOverlay = callback; invalidate(); }
    public DrawCallback getOnDrawDynamicOverlay() { return onDrawDynamicOverlay; }
    public void setOnDrawFront(DrawCallback callback) { onDrawFront = callback; invalidate(); }
    public DrawCallback getOnDrawFront() { return onDrawFront; }
    public void setOnDrawBackdrop(BackdropDrawInterceptor interceptor) { onDrawBackdrop = interceptor; invalidate(); }
    public BackdropDrawInterceptor getOnDrawBackdrop() { return onDrawBackdrop; }

    public void setExportedBackdrop(LayerBackdrop backdrop) {
        exportedBackdrop = backdrop;
        invalidate();
    }
    public LayerBackdrop getExportedBackdrop() { return exportedBackdrop; }

    /** Convenience only; Kotlin itself uses onDrawSurface. */
    public void setSurfaceColor(final int color) {
        convenienceSurfaceColor = color;
        hasConvenienceSurfaceColor = Color.alpha(color) != 0;
        if (!hasConvenienceSurfaceColor) {
            setOnDrawSurface(null);
            return;
        }
        setOnDrawSurface(new DrawCallback() {
            private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            public void draw(Canvas canvas, LiquidGlassView view, Path shapePath) {
                paint.setColor(color);
                canvas.drawRect(0f, 0f, view.getWidth(), view.getHeight(), paint);
            }
        });
    }
    public int getSurfaceColor() { return convenienceSurfaceColor; }
    public boolean hasSurfaceColor() { return hasConvenienceSurfaceColor; }
    public void clearSurfaceColor() { setSurfaceColor(Color.TRANSPARENT); }

    /** Lua/table configuration for the public drawBackdrop-style parameters. */
    public LiquidGlassView applyConfig(Object table) {
        if (table == null) return this;

        Object backdropValue = LuaTableBridge.getAny(table, new String[] {"backdrop", "backdropSource"});
        if (backdropValue instanceof Backdrop) setBackdrop((Backdrop) backdropValue);

        Object shapeValue = LuaTableBridge.getAny(table, new String[] {"shape"});
        if (shapeValue != null) setShape(GlassShape.fromConfig(shapeValue));

        Object radiusPx = LuaTableBridge.getAny(table, new String[] {"cornerRadiusPx", "radiusPx"});
        Object radiusDp = LuaTableBridge.getAny(table, new String[] {"cornerRadiusDp", "radiusDp"});
        if (radiusPx != null) setCornerRadiusPx(LuaTableBridge.getFloat(table, new String[] {"cornerRadiusPx", "radiusPx"}, 0f));
        else if (radiusDp != null) setCornerRadiusDp(LuaTableBridge.getFloat(table, new String[] {"cornerRadiusDp", "radiusDp"}, 0f));

        Object surface = LuaTableBridge.getAny(table, new String[] {"surfaceColor", "surface"});
        if (surface != null) setSurfaceColor(LuaTableBridge.getInt(table, new String[] {"surfaceColor", "surface"}, Color.TRANSPARENT));

        Object effectTable = LuaTableBridge.getAny(table, new String[] {"effects", "effect"});
        if (effectTable != null) applyEffectsConfig(effectTable);
        else {
            Object effectProbe = LuaTableBridge.getAny(table, new String[] {
                    "vibrancy", "blur", "blurRadius", "blurRadiusPx", "brightness", "contrast", "saturation",
                    "opacity", "refractionHeight", "refractionAmount", "lensHeight", "lensAmount",
                    "depthEffect", "chromaticAberration"
            });
            if (effectProbe != null) applyEffectsConfig(table);
        }

        Object highlightValue = LuaTableBridge.getAny(table, new String[] {"highlight"});
        if (highlightValue instanceof Highlight) setHighlight((Highlight) highlightValue);
        else if (highlightValue instanceof Boolean && !((Boolean) highlightValue).booleanValue()) clearHighlight();
        else if (highlightValue != null) setHighlight(Highlight.fromConfig(highlightValue));

        Object shadowValue = LuaTableBridge.getAny(table, new String[] {"shadow"});
        if (shadowValue instanceof Shadow) setShadow((Shadow) shadowValue);
        else if (shadowValue instanceof Boolean && !((Boolean) shadowValue).booleanValue()) clearShadow();
        else if (shadowValue != null) setShadow(Shadow.fromConfig(shadowValue));

        Object innerValue = LuaTableBridge.getAny(table, new String[] {"innerShadow"});
        if (innerValue instanceof InnerShadow) setInnerShadow((InnerShadow) innerValue);
        else if (innerValue instanceof Boolean && !((Boolean) innerValue).booleanValue()) clearInnerShadow();
        else if (innerValue != null) setInnerShadow(InnerShadow.fromConfig(innerValue));

        if (LuaTableBridge.getAny(table, new String[] {"enabled"}) != null)
            setEnabled(LuaTableBridge.getBoolean(table, new String[] {"enabled"}, isEnabled()));
        if (LuaTableBridge.getAny(table, new String[] {"alpha"}) != null)
            setAlpha(LuaTableBridge.getFloat(table, new String[] {"alpha"}, getAlpha()));
        if (LuaTableBridge.getAny(table, new String[] {"autoBackdrop", "autoBackdropEnabled"}) != null)
            setAutoBackdropEnabled(LuaTableBridge.getBoolean(table, new String[] {"autoBackdrop", "autoBackdropEnabled"}, isAutoBackdropEnabled()));

        invalidate();
        return this;
    }

    float density() { return getResources().getDisplayMetrics().density; }
    float dp(float value) { return value * density(); }
    public float getDensityValue() { return density(); }
    public float dpToPx(float valueDp) { return dp(valueDp); }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        /*
         * Auto-backdrop mode no longer requires LiquidGlassBackdropLayout.
         * Ordinary ViewGroups default to clipping child overflow, which would
         * cut the KT-style outer shadow, blur/lens padding and pressed scaling.
         * Mirror the old backdrop container behaviour automatically.
         */
        AncestorClipManager.attach(this);
        resolveAutomaticBackdrop();
    }

    /**
     * Automatic root capture must not include the glass itself. Returning here
     * suppresses the whole LiquidGlassView subtree only for the manager's
     * offscreen capture pass; normal screen drawing is unchanged.
     */
    @Override
    public void draw(Canvas canvas) {
        if (ActivityBackdropManager.isCapturingBackdrop()) return;
        super.draw(canvas);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) {
            super.dispatchDraw(canvas);
            return;
        }

        Path path = shape.createPath(w, h);

        // ShadowModifier.kt draws before drawContent().
        drawOuterShadow(canvas, path);

        // DrawBackdropNode places its content in an Offscreen graphics layer clipped
        // to the requested shape. Keep that isolation here too, because surface
        // blend modes (for example LiquidButton's Hue tint) must blend against the
        // sampled glass content, not arbitrary pixels already in the parent canvas.
        drawClippedGlassContent(canvas, path);

        /*
         * Keep animated press-light out of contentNode. On some HWUI builds the
         * node can be reused after ACTION_UP, making an alpha animation appear
         * to cut directly to zero. Drawing this layer on the real Canvas makes
         * every postOnAnimation frame observable.
         */
        if (onDrawDynamicOverlay != null) {
            int dynamicSave = canvas.save();
            canvas.clipPath(path);
            onDrawDynamicOverlay.draw(canvas, this, path);
            canvas.restoreToCount(dynamicSave);
        }

        // HighlightModifier.kt draws after drawContent().
        drawHighlight(canvas, path);
        // InnerShadowModifier.kt is the outer modifier and therefore draws last.
        drawInnerShadow(canvas, path);

        updateExportedBackdrop(path);
    }


    private void drawClippedGlassContent(Canvas canvas, Path path) {
        /*
         * The final visible outline must remain the real GlassShape. Fix12's
         * DST_IN mask can fail on some HWUI/RenderNode paths and leave a
         * rectangular offscreen layer. Keep only the padding produced by the KT
         * BackdropEffectScope effect chain in the effect RenderNode, and clip the
         * visible control to the real GlassShape.
         */
        if (Platform.isRenderNodeSupported() && canvas.isHardwareAccelerated()) {
            contentNode = Api29.obtain(contentNode, "LiquidGlassClippedContent",
                    getWidth(), getHeight(), true, null);
            /*
             * Compose places DrawBackdropNode with placeWithLayer { clip = true; shape = ... }.
             * Clip the RenderNode at composition time instead of clipping its RecordingCanvas.
             * This preserves the rounded outline and avoids the thin uncovered AA fringe that can
             * expose the raw backdrop at the very edge on some HWUI devices.
             */
            Api29.setClipShape(contentNode, shape, path, getWidth(), getHeight());
            Canvas rc = Api29.begin(contentNode, getWidth(), getHeight());
            drawGlassContentBody(rc, path);
            Api29.end(contentNode);
            Api29.draw(canvas, contentNode);
        } else {
            int layer = canvas.saveLayer(0f, 0f, getWidth(), getHeight(), null);
            int clipped = canvas.save();
            canvas.clipPath(path);
            drawGlassContentBody(canvas, path);
            canvas.restoreToCount(clipped);
            canvas.restoreToCount(layer);
        }
    }

    private void drawGlassContentBody(Canvas canvas, Path path) {
        if (onDrawBehind != null) onDrawBehind.draw(canvas, this, path);
        drawBackdropLayer(canvas);
        if (onDrawSurface != null) onDrawSurface.draw(canvas, this, path);
        if (onDrawContentBackground != null) onDrawContentBackground.draw(canvas, this, path);
        super.dispatchDraw(canvas);
        if (onDrawFront != null) onDrawFront.draw(canvas, this, path);
    }

    private void drawBackdropLayer(final Canvas canvas) {
        final int w = getWidth();
        final int h = getHeight();
        effectScope.begin(w, h, shape, density(), getResources().getConfiguration().fontScale, getLayoutDirection());
        if (effects != null) effects.apply(effectScope);

        if (!Platform.isRenderNodeSupported() || !canvas.isHardwareAccelerated()) {
            final Canvas fallbackCanvas = canvas;
            final Runnable fallbackDraw = new Runnable() {
                public void run() { backdrop.drawBackdrop(fallbackCanvas, LiquidGlassView.this); }
            };
            if (onDrawBackdrop != null) onDrawBackdrop.draw(canvas, this, fallbackDraw);
            else fallbackDraw.run();
            return;
        }

        float renderPadding = effectScope.getRenderPadding();
        int paddingInt = (int) renderPadding; // Kotlin layer topLeft/size use padding.toInt().
        int nodeW = Math.max(1, w + paddingInt * 2);
        int nodeH = Math.max(1, h + paddingInt * 2);
        effectNode = Api29.obtain(effectNode, "LiquidGlassBackdropEffect", nodeW, nodeH, false, null);
        if (Platform.isRenderEffectSupported()) {
            Api31.setRenderEffect(effectNode, effectScope.getRenderEffectObject());
        }

        Canvas recording = Api29.begin(effectNode, nodeW, nodeH);
        // KT recordBackdropBlock translates by the Float padding, while layer size/topLeft use Int padding.
        if (renderPadding != 0f) recording.translate(renderPadding, renderPadding);
        final Canvas target = recording;
        final Runnable draw = new Runnable() {
            public void run() { backdrop.drawBackdrop(target, LiquidGlassView.this); }
        };
        if (onDrawBackdrop != null) onDrawBackdrop.draw(recording, this, draw);
        else draw.run();
        Api29.end(effectNode);

        int save = canvas.save();
        if (paddingInt != 0) canvas.translate(-paddingInt, -paddingInt);
        Api29.draw(canvas, effectNode);
        canvas.restoreToCount(save);
    }

    private void drawOuterShadow(Canvas canvas, Path path) {
        Shadow s = shadow;
        if (s == null) return;
        float d = density();
        float radius = s.radiusDp * d;
        float ox = s.offsetXDp * d;
        float oy = s.offsetYDp * d;

        shadowPaint.reset();
        shadowPaint.setAntiAlias(true);
        shadowPaint.setColor(s.color);
        shadowPaint.setStyle(Paint.Style.FILL);
        shadowPaint.setMaskFilter(radius > 0f ? new BlurMaskFilter(radius, BlurMaskFilter.Blur.NORMAL) : null);

        if (Platform.isRenderNodeSupported() && canvas.isHardwareAccelerated()) {
            int sw = Math.max(1, (int) Math.ceil(getWidth() + radius * 4f + ox));
            int sh = Math.max(1, (int) Math.ceil(getHeight() + radius * 4f + oy));
            configureLayerPaint(s.blendMode);
            shadowNode = Api29.obtain(shadowNode, "LiquidGlassOuterShadow", sw, sh, true, layerPaint);
            Api29.setAlpha(shadowNode, s.alpha);
            Canvas rc = Api29.begin(shadowNode, sw, sh);
            rc.translate(radius * 2f + ox, radius * 2f + oy);
            rc.drawPath(path, shadowPaint);
            rc.translate(-ox, -oy);
            rc.drawPath(path, clearPaint);
            rc.translate(ox, oy);
            Api29.end(shadowNode);
            int save = canvas.save();
            canvas.translate(-radius * 2f, -radius * 2f);
            Api29.draw(canvas, shadowNode);
            canvas.restoreToCount(save);
        } else {
            // GraphicsLayer.alpha in ShadowModifier.kt multiplies the shadow as a whole.
            shadowPaint.setAlpha(Math.round(shadowPaint.getAlpha() * s.alpha));
            int saveLayer = canvas.saveLayer(null, null);
            int save = canvas.save();
            canvas.translate(ox, oy);
            canvas.drawPath(path, shadowPaint);
            canvas.restoreToCount(save);
            canvas.drawPath(path, clearPaint);
            canvas.restoreToCount(saveLayer);
        }
    }

    private void drawHighlight(Canvas canvas, Path path) {
        Highlight h = highlight;
        if (h == null || h.widthDp <= 0f) return;
        float d = density();
        float widthPx = Math.min(h.widthDp * d, Math.min(getWidth(), getHeight()) * 0.5f);
        float blurPx = h.blurRadiusDp * d;

        highlightPaint.reset();
        highlightPaint.setAntiAlias(true);
        highlightPaint.setStyle(Paint.Style.STROKE);
        highlightPaint.setStrokeWidth((float) Math.ceil(widthPx) * 2f);
        highlightPaint.setColor(h.style.color);
        highlightPaint.setMaskFilter(blurPx > 0f ? new BlurMaskFilter(blurPx, BlurMaskFilter.Blur.NORMAL) : null);
        if (Platform.isRuntimeShaderSupported()) Api33.configureHighlightShader(highlightPaint, highlightShaderCache, h.style, shape, getWidth(), getHeight());
        else highlightPaint.setShader(null);

        if (Platform.isRenderNodeSupported() && canvas.isHardwareAccelerated()) {
            configureLayerPaint(h.style.blendMode);
            int sw = getWidth() + 2;
            int sh = getHeight() + 2;
            highlightNode = Api29.obtain(highlightNode, "LiquidGlassHighlight", sw, sh, true, layerPaint);
            Api29.setAlpha(highlightNode, h.alpha);
            Canvas rc = Api29.begin(highlightNode, sw, sh);
            rc.translate(1f, 1f);
            int save = rc.save();
            rc.clipPath(path);
            rc.drawPath(path, highlightPaint);
            rc.restoreToCount(save);
            Api29.end(highlightNode);
            save = canvas.save();
            canvas.translate(-1f, -1f);
            Api29.draw(canvas, highlightNode);
            canvas.restoreToCount(save);
        } else {
            int save = canvas.save();
            canvas.clipPath(path);
            highlightPaint.setAlpha(Math.round(highlightPaint.getAlpha() * h.alpha));
            canvas.drawPath(path, highlightPaint);
            canvas.restoreToCount(save);
        }
    }

    private void drawInnerShadow(Canvas canvas, Path path) {
        InnerShadow s = innerShadow;
        if (s == null || !Platform.isRenderEffectSupported() || !canvas.isHardwareAccelerated()) return;

        float d = density();
        float radius = s.radiusDp * d;
        float ox = s.offsetXDp * d;
        float oy = s.offsetYDp * d;

        innerPaint.reset();
        innerPaint.setAntiAlias(true);
        innerPaint.setColor(s.color);
        innerPaint.setStyle(Paint.Style.FILL);

        configureLayerPaint(s.blendMode);
        innerShadowNode = Api29.obtain(innerShadowNode, "LiquidGlassInnerShadow", getWidth(), getHeight(), true, layerPaint);
        Api29.setAlpha(innerShadowNode, s.alpha);
        Api31.setBlurDecal(innerShadowNode, radius);

        Canvas rc = Api29.begin(innerShadowNode, getWidth(), getHeight());
        int clip = rc.save();
        rc.clipPath(path);
        rc.drawPath(path, innerPaint);
        rc.translate(ox, oy);
        rc.drawPath(path, clearPaint);
        rc.translate(-ox, -oy);
        rc.restoreToCount(clip);
        Api29.end(innerShadowNode);

        int save = canvas.save();
        canvas.clipPath(path);
        Api29.draw(canvas, innerShadowNode);
        canvas.restoreToCount(save);
    }

    private void updateExportedBackdrop(Path path) {
        if (exportedBackdrop == null || !Platform.isRenderNodeSupported() || getWidth() <= 0 || getHeight() <= 0) return;
        exportedNode = Api29.obtain(exportedNode, "LiquidGlassExportedBackdrop", getWidth(), getHeight(), true, null);
        Canvas rc = Api29.begin(exportedNode, getWidth(), getHeight());
        if (onDrawBehind != null) onDrawBehind.draw(rc, this, path);
        drawBackdropLayerIntoRecording(rc);
        if (onDrawSurface != null) onDrawSurface.draw(rc, this, path);
        if (onDrawFront != null) onDrawFront.draw(rc, this, path);
        Api29.end(exportedNode);
        exportedBackdrop.setExportedNode(exportedNode, this);
    }

    /** Same as drawBackdropLayer but records the already built effect node; avoids recording child content. */
    private void drawBackdropLayerIntoRecording(Canvas canvas) {
        if (effectNode == null || !Platform.isRenderNodeSupported()) return;
        int p = (int) effectScope.getRenderPadding();
        int save = canvas.save();
        if (p != 0) canvas.translate(-p, -p);
        Api29.draw(canvas, effectNode);
        canvas.restoreToCount(save);
    }

    private void configureLayerPaint(GlassBlendMode mode) {
        layerPaint.reset();
        layerPaint.setAntiAlias(true);
        if (Platform.isRenderNodeSupported()) Api29.setBlend(layerPaint, mode);
        else if (mode == GlassBlendMode.PLUS) layerPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.ADD));
    }

    /** Port of InverseLayerScope.inverseTransformAtTopLeft for rotationZ/scaleX/scaleY. */
    void concatInverseLayerTransform(Canvas canvas) {
        float sx = getScaleX();
        float sy = getScaleY();
        float rz = getRotation();
        if (rz == 0f) {
            if (sx != 0f && sy != 0f) canvas.scale(1f / sx, 1f / sy, 0f, 0f);
            return;
        }
        double rad = rz * (Math.PI / 180.0);
        float sn = (float) Math.sin(rad);
        float cs = (float) Math.cos(rad);
        float a00 = cs * sx;
        float a01 = sn * sy;
        float a10 = -sn * sx;
        float a11 = cs * sy;
        float det = a00 * a11 - a01 * a10;
        if (det == 0f) return;
        float inv = 1f / det;
        Matrix m = new Matrix();
        m.setValues(new float[] {
                a11 * inv, -a01 * inv, 0f,
                -a10 * inv, a00 * inv, 0f,
                0f, 0f, 1f
        });
        canvas.concat(m);
    }

    @Override
    protected void onDetachedFromWindow() {
        ActivityBackdropManager.release(this);
        AncestorClipManager.detach(this);
        super.onDetachedFromWindow();
        effectScope.clearRuntimeShaderCache();
        highlightShaderCache.clear();
        contentNode = null;
        effectNode = null;
        shadowNode = null;
        highlightNode = null;
        innerShadowNode = null;
        exportedNode = null;
    }

    private static final class Api29 {
        private Api29() {}

        static Object obtain(Object old, String name, int width, int height, boolean compositing, Paint compositingPaint) {
            RenderNode n = old instanceof RenderNode ? (RenderNode) old : new RenderNode(name);
            n.setPosition(0, 0, Math.max(1, width), Math.max(1, height));
            n.setUseCompositingLayer(compositing, compositingPaint);
            return n;
        }

        static void setClipShape(Object node, GlassShape shape, Path path, int width, int height) {
            RenderNode n = (RenderNode) node;
            Outline outline = new Outline();
            float[] radii = shape == null
                    ? new float[] {0f, 0f, 0f, 0f}
                    : shape.getCornerRadii(width, height);
            boolean uniform = Math.abs(radii[0] - radii[1]) < 0.001f
                    && Math.abs(radii[0] - radii[2]) < 0.001f
                    && Math.abs(radii[0] - radii[3]) < 0.001f;
            if (uniform) {
                outline.setRoundRect(0, 0, Math.max(1, width), Math.max(1, height), radii[0]);
            } else {
                /* Rounded-rect paths are convex; this API exists on all RenderNode-supported levels. */
                outline.setConvexPath(path);
            }
            n.setOutline(outline);
            n.setClipToOutline(true);
        }

        static Canvas begin(Object node, int width, int height) {
            return ((RenderNode) node).beginRecording(Math.max(1, width), Math.max(1, height));
        }

        static void end(Object node) { ((RenderNode) node).endRecording(); }
        static void draw(Canvas canvas, Object node) { canvas.drawRenderNode((RenderNode) node); }
        static void setAlpha(Object node, float alpha) { ((RenderNode) node).setAlpha(alpha); }

        static void setBlend(Paint paint, GlassBlendMode mode) {
            if (Build.VERSION.SDK_INT >= 29) {
                paint.setBlendMode(mode == GlassBlendMode.PLUS ? android.graphics.BlendMode.PLUS : android.graphics.BlendMode.SRC_OVER);
            }
        }
    }

    private static final class Api31 {
        private Api31() {}
        static void setRenderEffect(Object node, Object effect) {
            ((RenderNode) node).setRenderEffect((RenderEffect) effect);
        }
        static void setBlurDecal(Object node, float radius) {
            if (radius > 0f) {
                ((RenderNode) node).setRenderEffect(RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.DECAL));
            } else {
                ((RenderNode) node).setRenderEffect(null);
            }
        }
    }

    private static final class Api33 {
        private Api33() {}

        static void configureHighlightShader(Paint paint, Map<String, Object> cache,
                                             HighlightStyle style, GlassShape shape,
                                             float width, float height) {
            if (style instanceof HighlightStyle.Plain) {
                paint.setShader(null);
                return;
            }
            String key;
            String source;
            if (style instanceof HighlightStyle.Ambient) {
                key = "Ambient";
                source = Shaders.AMBIENT_HIGHLIGHT;
            } else {
                key = "Default";
                source = Shaders.DEFAULT_HIGHLIGHT;
            }
            RuntimeShader shader = (RuntimeShader) cache.get(key);
            if (shader == null) {
                shader = new RuntimeShader(source);
                cache.put(key, shader);
            }
            shader.setFloatUniform("size", width, height);
            shader.setFloatUniform("cornerRadii", shape.getCornerRadii(width, height));
            if (style instanceof HighlightStyle.Default) {
                HighlightStyle.Default s = (HighlightStyle.Default) style;
                // The Kotlin code uses color.copy(alpha = 1f): preserve RGB and force alpha 255.
                shader.setColorUniform("color", Color.argb(255, Color.red(s.color), Color.green(s.color), Color.blue(s.color)));
                shader.setFloatUniform("angle", s.angle * (float) (Math.PI / 180.0));
                shader.setFloatUniform("falloff", s.falloff);
            } else {
                shader.setFloatUniform("angle", 45f * (float) (Math.PI / 180.0));
                shader.setFloatUniform("falloff", 1f);
            }
            paint.setShader(shader);
        }
    }
}
