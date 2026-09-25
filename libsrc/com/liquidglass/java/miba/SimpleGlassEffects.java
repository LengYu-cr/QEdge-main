package com.liquidglass.java.miba;

/**
 * Mutable Lua-friendly effect chain.
 *
 * The execution order matches the common KT call order used by the catalog:
 * vibrancy -> colorControls -> opacity -> blur -> lens.
 */
public final class SimpleGlassEffects implements LiquidGlassView.Effects {
    private boolean vibrancyEnabled;
    private boolean colorControlsEnabled;
    private float brightness;
    private float contrast = 1f;
    private float saturation = 1f;
    private boolean opacityEnabled;
    private float opacity = 1f;
    private float blurRadiusPx;
    private float refractionHeightPx;
    private float refractionAmountPx;
    private boolean depthEffect;
    private boolean chromaticAberration;

    public SimpleGlassEffects() {}

    public SimpleGlassEffects(Object configTable) {
        applyConfig(configTable);
    }

    public void apply(BackdropEffectScope scope) {
        if (scope == null) return;
        if (vibrancyEnabled) scope.vibrancy();
        if (colorControlsEnabled) scope.colorControls(brightness, contrast, saturation);
        if (opacityEnabled) scope.opacity(opacity);
        if (blurRadiusPx > 0f) scope.blur(blurRadiusPx);
        if (refractionHeightPx > 0f && refractionAmountPx > 0f) {
            scope.lens(refractionHeightPx, refractionAmountPx, depthEffect, chromaticAberration);
        }
    }

    public boolean isVibrancyEnabled() { return vibrancyEnabled; }
    public SimpleGlassEffects setVibrancyEnabled(boolean enabled) {
        vibrancyEnabled = enabled;
        return this;
    }

    public boolean isColorControlsEnabled() { return colorControlsEnabled; }
    public SimpleGlassEffects setColorControlsEnabled(boolean enabled) {
        colorControlsEnabled = enabled;
        return this;
    }

    public float getBrightness() { return brightness; }
    public float getContrast() { return contrast; }
    public float getSaturation() { return saturation; }

    public SimpleGlassEffects setColorControls(float brightness, float contrast, float saturation) {
        this.brightness = brightness;
        this.contrast = contrast;
        this.saturation = saturation;
        colorControlsEnabled = !(brightness == 0f && contrast == 1f && saturation == 1f);
        return this;
    }

    public SimpleGlassEffects clearColorControls() {
        brightness = 0f;
        contrast = 1f;
        saturation = 1f;
        colorControlsEnabled = false;
        return this;
    }

    public boolean isOpacityEnabled() { return opacityEnabled; }
    public float getOpacity() { return opacity; }

    public SimpleGlassEffects setOpacity(float alpha) {
        opacity = alpha;
        opacityEnabled = true;
        return this;
    }

    public SimpleGlassEffects clearOpacity() {
        opacity = 1f;
        opacityEnabled = false;
        return this;
    }

    public float getBlurRadiusPx() { return blurRadiusPx; }

    public SimpleGlassEffects setBlurRadiusPx(float radiusPx) {
        blurRadiusPx = Math.max(0f, radiusPx);
        return this;
    }

    public SimpleGlassEffects clearBlur() {
        blurRadiusPx = 0f;
        return this;
    }

    public float getRefractionHeightPx() { return refractionHeightPx; }
    public float getRefractionAmountPx() { return refractionAmountPx; }
    public boolean isDepthEffect() { return depthEffect; }
    public boolean isChromaticAberration() { return chromaticAberration; }

    public SimpleGlassEffects setLens(float heightPx, float amountPx) {
        return setLens(heightPx, amountPx, false, false);
    }

    public SimpleGlassEffects setLens(float heightPx, float amountPx,
                                      boolean depthEffect, boolean chromaticAberration) {
        refractionHeightPx = Math.max(0f, heightPx);
        refractionAmountPx = Math.max(0f, amountPx);
        this.depthEffect = depthEffect;
        this.chromaticAberration = chromaticAberration;
        return this;
    }

    public SimpleGlassEffects setRefractionHeightPx(float value) {
        refractionHeightPx = Math.max(0f, value);
        return this;
    }

    public SimpleGlassEffects setRefractionAmountPx(float value) {
        refractionAmountPx = Math.max(0f, value);
        return this;
    }

    public SimpleGlassEffects setDepthEffect(boolean value) {
        depthEffect = value;
        return this;
    }

    public SimpleGlassEffects setChromaticAberration(boolean value) {
        chromaticAberration = value;
        return this;
    }

    public SimpleGlassEffects clearLens() {
        refractionHeightPx = 0f;
        refractionAmountPx = 0f;
        depthEffect = false;
        chromaticAberration = false;
        return this;
    }

    public SimpleGlassEffects copyFrom(SimpleGlassEffects other) {
        if (other == null) return clear();
        vibrancyEnabled = other.vibrancyEnabled;
        colorControlsEnabled = other.colorControlsEnabled;
        brightness = other.brightness;
        contrast = other.contrast;
        saturation = other.saturation;
        opacityEnabled = other.opacityEnabled;
        opacity = other.opacity;
        blurRadiusPx = other.blurRadiusPx;
        refractionHeightPx = other.refractionHeightPx;
        refractionAmountPx = other.refractionAmountPx;
        depthEffect = other.depthEffect;
        chromaticAberration = other.chromaticAberration;
        return this;
    }

    /**
     * Lua/table configuration. Pixel units are used because BackdropEffectScope
     * itself uses pixels, matching the KT effect extension functions after dp.toPx().
     *
     * Accepted keys:
     * vibrancy
     * brightness, contrast, saturation
     * opacity
     * blur / blurRadius / blurRadiusPx
     * refractionHeight / refractionHeightPx
     * refractionAmount / refractionAmountPx
     * depthEffect
     * chromaticAberration / dispersion
     */
    public SimpleGlassEffects applyConfig(Object table) {
        if (table == null) return this;

        if (LuaTableBridge.getAny(table, new String[] {"vibrancy", "vibrancyEnabled"}) != null) {
            setVibrancyEnabled(LuaTableBridge.getBoolean(
                    table, new String[] {"vibrancy", "vibrancyEnabled"}, vibrancyEnabled));
        }

        Object brightnessValue = LuaTableBridge.getAny(table, new String[] {"brightness"});
        Object contrastValue = LuaTableBridge.getAny(table, new String[] {"contrast"});
        Object saturationValue = LuaTableBridge.getAny(table, new String[] {"saturation"});
        if (brightnessValue != null || contrastValue != null || saturationValue != null) {
            setColorControls(
                    LuaTableBridge.getFloat(table, new String[] {"brightness"}, brightness),
                    LuaTableBridge.getFloat(table, new String[] {"contrast"}, contrast),
                    LuaTableBridge.getFloat(table, new String[] {"saturation"}, saturation)
            );
        }

        if (LuaTableBridge.getAny(table, new String[] {"opacity", "alpha"}) != null) {
            setOpacity(LuaTableBridge.getFloat(table, new String[] {"opacity", "alpha"}, opacity));
        }

        if (LuaTableBridge.getAny(table, new String[] {"blur", "blurRadius", "blurRadiusPx"}) != null) {
            setBlurRadiusPx(LuaTableBridge.getFloat(
                    table, new String[] {"blur", "blurRadius", "blurRadiusPx"}, blurRadiusPx));
        }

        Object h = LuaTableBridge.getAny(table,
                new String[] {"refractionHeight", "refractionHeightPx", "lensHeight"});
        Object a = LuaTableBridge.getAny(table,
                new String[] {"refractionAmount", "refractionAmountPx", "lensAmount"});
        Object d = LuaTableBridge.getAny(table, new String[] {"depthEffect", "depth"});
        Object c = LuaTableBridge.getAny(table,
                new String[] {"chromaticAberration", "dispersion", "chromatic"});

        if (h != null || a != null || d != null || c != null) {
            setLens(
                    LuaTableBridge.getFloat(table,
                            new String[] {"refractionHeight", "refractionHeightPx", "lensHeight"},
                            refractionHeightPx),
                    LuaTableBridge.getFloat(table,
                            new String[] {"refractionAmount", "refractionAmountPx", "lensAmount"},
                            refractionAmountPx),
                    LuaTableBridge.getBoolean(table,
                            new String[] {"depthEffect", "depth"}, depthEffect),
                    LuaTableBridge.getBoolean(table,
                            new String[] {"chromaticAberration", "dispersion", "chromatic"},
                            chromaticAberration)
            );
        }

        return this;
    }

    public SimpleGlassEffects clear() {
        vibrancyEnabled = false;
        clearColorControls();
        clearOpacity();
        clearBlur();
        clearLens();
        return this;
    }
}
