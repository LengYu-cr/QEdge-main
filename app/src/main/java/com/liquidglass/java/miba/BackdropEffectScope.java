package com.liquidglass.java.miba;

import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.RenderEffect;
import android.graphics.Shader;


/** Direct port of BackdropEffectScope + effects/*.kt. */
public final class BackdropEffectScope {
    /** Java replacement for RuntimeShader.() -> Unit. */
    public interface RuntimeShaderBlock { void apply(RuntimeShader shader); }
    private float width;
    private float height;
    private float density = 1f;
    private float fontScale = 1f;
    private int layoutDirection;
    private float padding;
    private GlassShape shape;
    private Object renderEffect;
    private final RuntimeShaderCache runtimeShaderCache = new RuntimeShaderCache();

    void begin(float width, float height, GlassShape shape) {
        begin(width, height, shape, 1f, 1f, 0);
    }

    void begin(float width, float height, GlassShape shape, float density, float fontScale, int layoutDirection) {
        this.width = width;
        this.height = height;
        this.shape = shape;
        this.density = density;
        this.fontScale = fontScale;
        this.layoutDirection = layoutDirection;
        this.padding = 0f;
        this.renderEffect = null;
    }

    public float getWidth() { return width; }
    public float getHeight() { return height; }
    public float[] getSize() { return new float[] { width, height }; }
    public float getDensity() { return density; }
    public float getFontScale() { return fontScale; }
    public int getLayoutDirection() { return layoutDirection; }
    public float getPadding() { return padding; }
    /** Recording padding with the exact Kotlin BackdropEffectScope semantics. */
    public float getRenderPadding() { return padding; }
    public void setPadding(float value) { padding = Math.max(0f, value); }
    public GlassShape getShape() { return shape; }
    public Object getRenderEffect() { return renderEffect; }
    public RuntimeShaderCache getRuntimeShaderCache() { return runtimeShaderCache; }
    public float dpToPx(float valueDp) { return valueDp * density; }
    public float spToPx(float valueSp) { return valueSp * density * fontScale; }
    public void setRenderEffect(Object effect) { renderEffect = effect; }
    Object getRenderEffectObject() { return renderEffect; }

    /** Equivalent to BackdropEffectScope.effect(RenderEffect). Pass a framework RenderEffect on API 31+. */
    public BackdropEffectScope effect(Object effect) {
        if (!Platform.isRenderEffectSupported() || effect == null) return this;
        renderEffect = Api31.chain(renderEffect, effect);
        return this;
    }

    public BackdropEffectScope blur(float radiusPx) {
        return blur(radiusPx, Shader.TileMode.CLAMP);
    }

    public BackdropEffectScope blur(float radiusPx, Shader.TileMode edgeTreatment) {
        if (!Platform.isRenderEffectSupported()) return this;
        if (radiusPx <= 0f) return this;
        if (edgeTreatment == null) edgeTreatment = Shader.TileMode.CLAMP;

        // Exact Blur.kt padding rule.
        if (edgeTreatment != Shader.TileMode.CLAMP || renderEffect != null) {
            if (radiusPx > padding) padding = radiusPx;
        }
        renderEffect = Api31.blur(renderEffect, radiusPx, edgeTreatment);
        return this;
    }

    public BackdropEffectScope colorFilter(ColorFilter colorFilter) {
        if (!Platform.isRenderEffectSupported() || colorFilter == null) return this;
        renderEffect = Api31.colorFilter(renderEffect, colorFilter);
        return this;
    }

    public BackdropEffectScope opacity(float alpha) {
        ColorMatrix matrix = new ColorMatrix(new float[] {
                1f, 0f, 0f, 0f, 0f,
                0f, 1f, 0f, 0f, 0f,
                0f, 0f, 1f, 0f, 0f,
                0f, 0f, 0f, alpha, 0f
        });
        return colorFilter(new ColorMatrixColorFilter(matrix));
    }

    public BackdropEffectScope colorControls() {
        return colorControls(0f, 1f, 1f);
    }

    public BackdropEffectScope colorControls(float brightness) {
        return colorControls(brightness, 1f, 1f);
    }

    public BackdropEffectScope colorControls(float brightness, float contrast) {
        return colorControls(brightness, contrast, 1f);
    }

    public BackdropEffectScope colorControls(float brightness, float contrast, float saturation) {
        if (brightness == 0f && contrast == 1f && saturation == 1f) return this;
        return colorFilter(colorControlsColorFilter(brightness, contrast, saturation));
    }

    public BackdropEffectScope vibrancy() {
        return colorFilter(colorControlsColorFilter(0f, 1f, 1.5f));
    }

    public BackdropEffectScope lens(float refractionHeightPx, float refractionAmountPx) {
        return lens(refractionHeightPx, refractionAmountPx, false, false);
    }

    public BackdropEffectScope lens(float refractionHeightPx, float refractionAmountPx,
                                    boolean depthEffect) {
        return lens(refractionHeightPx, refractionAmountPx, depthEffect, false);
    }

    public BackdropEffectScope lens(float refractionHeightPx, float refractionAmountPx,
                                    boolean depthEffect, boolean chromaticAberration) {
        if (!Platform.isRuntimeShaderSupported()) return this;
        if (refractionHeightPx <= 0f || refractionAmountPx <= 0f) return this;

        if (padding > 0f) padding = Math.max(0f, padding - refractionHeightPx);
        float[] radii = shape == null
                ? new float[] {0f, 0f, 0f, 0f}
                : shape.getCornerRadii(width, height);

        Object lens = Api33.lens(runtimeShaderCache, width, height, -padding, radii,
                refractionHeightPx, -refractionAmountPx, depthEffect, chromaticAberration);
        renderEffect = Api31.chain(renderEffect, lens);
        return this;
    }

    /** Direct port of effects/runtimeShaderEffect(). */
    public BackdropEffectScope runtimeShaderEffect(String key, String shaderString,
                                                    String uniformShaderName) {
        return runtimeShaderEffect(key, shaderString, uniformShaderName, null);
    }

    public BackdropEffectScope runtimeShaderEffect(String key, String shaderString,
                                                    String uniformShaderName, RuntimeShaderBlock block) {
        if (!Platform.isRuntimeShaderSupported()) return this;
        RuntimeShader shader = runtimeShaderCache.obtainRuntimeShader(key, shaderString);
        if (block != null) block.apply(shader);
        Object effect = Api33.runtimeShaderEffect(shader, uniformShaderName);
        renderEffect = Api31.chain(renderEffect, effect);
        return this;
    }

    public RuntimeShader obtainRuntimeShader(String key, String shaderString) {
        if (!Platform.isRuntimeShaderSupported()) {
            throw new UnsupportedOperationException("RuntimeShader requires Android 13 / API 33+");
        }
        return runtimeShaderCache.obtainRuntimeShader(key, shaderString);
    }

    public void clearRuntimeShaderCache() {
        runtimeShaderCache.clear();
    }

    /** Clear the current chain while retaining cached RuntimeShaders. */
    public BackdropEffectScope reset() {
        padding = 0f;
        renderEffect = null;
        return this;
    }

    private static ColorFilter colorControlsColorFilter(float brightness, float contrast, float saturation) {
        float invSat = 1f - saturation;
        float r = 0.213f * invSat;
        float g = 0.715f * invSat;
        float b = 0.072f * invSat;

        float c = contrast;
        float t = (0.5f - c * 0.5f + brightness) * 255f;
        float s = saturation;

        float cr = c * r;
        float cg = c * g;
        float cb = c * b;
        float cs = c * s;

        return new ColorMatrixColorFilter(new ColorMatrix(new float[] {
                cr + cs, cg, cb, 0f, t,
                cr, cg + cs, cb, 0f, t,
                cr, cg, cb + cs, 0f, t,
                0f, 0f, 0f, 1f, 0f
        }));
    }

    private static final class Api31 {
        private Api31() {}

        static Object chain(Object current, Object other) {
            if (other == null) return current;
            if (current != null) {
                return RenderEffect.createChainEffect((RenderEffect) other, (RenderEffect) current);
            }
            return other;
        }

        static Object blur(Object current, float radius, Shader.TileMode tileMode) {
            if (current != null) {
                return RenderEffect.createBlurEffect(radius, radius, (RenderEffect) current, tileMode);
            }
            return RenderEffect.createBlurEffect(radius, radius, tileMode);
        }

        static Object colorFilter(Object current, ColorFilter filter) {
            if (current != null) {
                return RenderEffect.createColorFilterEffect(filter, (RenderEffect) current);
            }
            return RenderEffect.createColorFilterEffect(filter);
        }
    }

    private static final class Api33 {
        private Api33() {}

        static Object lens(RuntimeShaderCache cache,
                           float width, float height, float offset,
                           float[] radii, float refractionHeight,
                           float refractionAmount, boolean depthEffect,
                           boolean chromaticAberration) {
            String key = chromaticAberration ? "RefractionWithDispersion" : "Refraction";
            RuntimeShader shader = cache.obtainRuntimeShader(
                    key,
                    chromaticAberration
                            ? Shaders.ROUNDED_RECT_REFRACTION_WITH_DISPERSION
                            : Shaders.ROUNDED_RECT_REFRACTION
            );
            shader.setFloatUniform("size", width, height);
            shader.setFloatUniform("offset", offset, offset);
            shader.setFloatUniform("cornerRadii", radii);
            shader.setFloatUniform("refractionHeight", refractionHeight);
            shader.setFloatUniform("refractionAmount", refractionAmount);
            shader.setFloatUniform("depthEffect", depthEffect ? 1f : 0f);
            if (chromaticAberration) shader.setFloatUniform("chromaticAberration", 1f);
            return RenderEffect.createRuntimeShaderEffect(
                    (android.graphics.RuntimeShader) shader.frameworkShader(), "content");
        }

        static Object runtimeShaderEffect(RuntimeShader shader, String uniformShaderName) {
            return RenderEffect.createRuntimeShaderEffect(
                    (android.graphics.RuntimeShader) shader.frameworkShader(), uniformShaderName);
        }
    }
}
