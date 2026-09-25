package com.liquidglass.java.miba;

import android.graphics.Color;

/** Immutable data-equivalent port of shadow/InnerShadow.kt. Values are dp. */
public final class InnerShadow {
    public final float radiusDp;
    public final float offsetXDp;
    public final float offsetYDp;
    public final int color;
    public final float alpha;
    public final GlassBlendMode blendMode;

    public InnerShadow() {
        this(24f, 0f, 24f,
                Color.argb(Math.round(255f * 0.15f), 0, 0, 0),
                1f, GlassBlendMode.SRC_OVER);
    }

    public InnerShadow(float radiusDp) {
        this(radiusDp, 0f, radiusDp,
                Color.argb(Math.round(255f * 0.15f), 0, 0, 0),
                1f, GlassBlendMode.SRC_OVER);
    }

    /** Convenience used by original component ports: radius + animated alpha. */
    public InnerShadow(float radiusDp, float alpha) {
        this(radiusDp, 0f, radiusDp,
                Color.argb(Math.round(255f * 0.15f), 0, 0, 0),
                alpha, GlassBlendMode.SRC_OVER);
    }

    public InnerShadow(float radiusDp, float offsetXDp, float offsetYDp) {
        this(radiusDp, offsetXDp, offsetYDp,
                Color.argb(Math.round(255f * 0.15f), 0, 0, 0),
                1f, GlassBlendMode.SRC_OVER);
    }

    public InnerShadow(float radiusDp, float offsetXDp, float offsetYDp, int color) {
        this(radiusDp, offsetXDp, offsetYDp, color, 1f, GlassBlendMode.SRC_OVER);
    }

    public InnerShadow(float radiusDp, float offsetXDp, float offsetYDp,
                       int color, float alpha) {
        this(radiusDp, offsetXDp, offsetYDp, color, alpha, GlassBlendMode.SRC_OVER);
    }

    public InnerShadow(float radiusDp, float offsetXDp, float offsetYDp,
                       int color, float alpha, GlassBlendMode blendMode) {
        this.radiusDp = radiusDp;
        this.offsetXDp = offsetXDp;
        this.offsetYDp = offsetYDp;
        this.color = color;
        this.alpha = alpha;
        this.blendMode = blendMode == null ? GlassBlendMode.SRC_OVER : blendMode;
    }

    public float getRadiusDp() { return radiusDp; }
    public float getOffsetXDp() { return offsetXDp; }
    public float getOffsetYDp() { return offsetYDp; }
    public int getColor() { return color; }
    public float getAlpha() { return alpha; }
    public GlassBlendMode getBlendMode() { return blendMode; }

    public InnerShadow copy() {
        return new InnerShadow(radiusDp, offsetXDp, offsetYDp, color, alpha, blendMode);
    }

    public InnerShadow copy(float radiusDp, float offsetXDp, float offsetYDp,
                            int color, float alpha, GlassBlendMode blendMode) {
        return new InnerShadow(radiusDp, offsetXDp, offsetYDp, color, alpha, blendMode);
    }

    public float component1() { return radiusDp; }
    public float component2() { return offsetXDp; }
    public float component3() { return offsetYDp; }
    public int component4() { return color; }
    public float component5() { return alpha; }
    public GlassBlendMode component6() { return blendMode; }

    public InnerShadow withAlpha(float value) {
        return new InnerShadow(radiusDp, offsetXDp, offsetYDp, color, value, blendMode);
    }

    public InnerShadow withRadius(float value) {
        return new InnerShadow(value, offsetXDp, offsetYDp, color, alpha, blendMode);
    }

    public InnerShadow withOffset(float xDp, float yDp) {
        return new InnerShadow(radiusDp, xDp, yDp, color, alpha, blendMode);
    }

    public InnerShadow withColor(int value) {
        return new InnerShadow(radiusDp, offsetXDp, offsetYDp, value, alpha, blendMode);
    }

    public InnerShadow withBlendMode(GlassBlendMode value) {
        return new InnerShadow(radiusDp, offsetXDp, offsetYDp, color, alpha, value);
    }

    /** Port of com.kyant.backdrop.shadow.lerp(start, stop, fraction). */
    public static InnerShadow lerp(InnerShadow start, InnerShadow stop, float fraction) {
        if (start == null || stop == null) {
            throw new IllegalArgumentException("start/stop must not be null");
        }
        return new InnerShadow(
                lerpFloat(start.radiusDp, stop.radiusDp, fraction),
                lerpFloat(start.offsetXDp, stop.offsetXDp, fraction),
                lerpFloat(start.offsetYDp, stop.offsetYDp, fraction),
                ColorUtils.lerp(start.color, stop.color, fraction),
                lerpFloat(start.alpha, stop.alpha, fraction),
                fraction < 0.5f ? start.blendMode : stop.blendMode
        );
    }

    public static InnerShadow fromConfig(Object table) {
        if (table == null) return null;
        if (table instanceof InnerShadow) return (InnerShadow) table;
        if (table instanceof Boolean && !((Boolean) table).booleanValue()) return null;

        float radius = LuaTableBridge.getFloat(
                table, new String[] {"radius", "radiusDp"}, 24f);
        float ox = LuaTableBridge.getFloat(
                table, new String[] {"offsetX", "offsetXDp", "x"}, 0f);
        float oy = LuaTableBridge.getFloat(
                table, new String[] {"offsetY", "offsetYDp", "y"}, radius);
        int color = LuaTableBridge.getInt(
                table, new String[] {"color"},
                Color.argb(Math.round(255f * 0.15f), 0, 0, 0));
        float alpha = LuaTableBridge.getFloat(
                table, new String[] {"alpha"}, 1f);
        String blend = LuaTableBridge.getString(
                table, new String[] {"blendMode", "blend"}, "srcOver");
        return new InnerShadow(
                radius, ox, oy, color, alpha,
                HighlightStyle.parseBlendMode(blend, GlassBlendMode.SRC_OVER));
    }

    private static float lerpFloat(float a, float b, float t) {
        return a + (b - a) * t;
    }

    public static final InnerShadow DEFAULT = new InnerShadow();

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof InnerShadow)) return false;
        InnerShadow s = (InnerShadow) other;
        return Float.floatToIntBits(radiusDp) == Float.floatToIntBits(s.radiusDp)
                && Float.floatToIntBits(offsetXDp) == Float.floatToIntBits(s.offsetXDp)
                && Float.floatToIntBits(offsetYDp) == Float.floatToIntBits(s.offsetYDp)
                && color == s.color
                && Float.floatToIntBits(alpha) == Float.floatToIntBits(s.alpha)
                && blendMode == s.blendMode;
    }

    @Override
    public int hashCode() {
        int result = Float.floatToIntBits(radiusDp);
        result = 31 * result + Float.floatToIntBits(offsetXDp);
        result = 31 * result + Float.floatToIntBits(offsetYDp);
        result = 31 * result + color;
        result = 31 * result + Float.floatToIntBits(alpha);
        result = 31 * result + blendMode.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "InnerShadow(radiusDp=" + radiusDp
                + ", offsetXDp=" + offsetXDp
                + ", offsetYDp=" + offsetYDp
                + ", color=" + color
                + ", alpha=" + alpha
                + ", blendMode=" + blendMode + ")";
    }
}
