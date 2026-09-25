package com.liquidglass.java.miba;

import android.graphics.Color;

/** Immutable data-equivalent port of shadow/Shadow.kt. Values are dp. */
public final class Shadow {
    public final float radiusDp;
    public final float offsetXDp;
    public final float offsetYDp;
    public final int color;
    public final float alpha;
    public final GlassBlendMode blendMode;

    public Shadow() {
        this(24f, 0f, 4f,
                Color.argb(Math.round(255f * 0.1f), 0, 0, 0),
                1f, GlassBlendMode.SRC_OVER);
    }

    public Shadow(float radiusDp) {
        this(radiusDp, 0f, radiusDp / 6f,
                Color.argb(Math.round(255f * 0.1f), 0, 0, 0),
                1f, GlassBlendMode.SRC_OVER);
    }

    public Shadow(float radiusDp, int color) {
        this(radiusDp, 0f, radiusDp / 6f, color, 1f, GlassBlendMode.SRC_OVER);
    }

    public Shadow(float radiusDp, float offsetXDp, float offsetYDp) {
        this(radiusDp, offsetXDp, offsetYDp,
                Color.argb(Math.round(255f * 0.1f), 0, 0, 0),
                1f, GlassBlendMode.SRC_OVER);
    }

    public Shadow(float radiusDp, float offsetXDp, float offsetYDp, int color) {
        this(radiusDp, offsetXDp, offsetYDp, color, 1f, GlassBlendMode.SRC_OVER);
    }

    public Shadow(float radiusDp, float offsetXDp, float offsetYDp, int color, float alpha) {
        this(radiusDp, offsetXDp, offsetYDp, color, alpha, GlassBlendMode.SRC_OVER);
    }

    public Shadow(float radiusDp, float offsetXDp, float offsetYDp,
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

    public Shadow copy() {
        return new Shadow(radiusDp, offsetXDp, offsetYDp, color, alpha, blendMode);
    }

    public Shadow copy(float radiusDp, float offsetXDp, float offsetYDp,
                       int color, float alpha, GlassBlendMode blendMode) {
        return new Shadow(radiusDp, offsetXDp, offsetYDp, color, alpha, blendMode);
    }

    public float component1() { return radiusDp; }
    public float component2() { return offsetXDp; }
    public float component3() { return offsetYDp; }
    public int component4() { return color; }
    public float component5() { return alpha; }
    public GlassBlendMode component6() { return blendMode; }

    public Shadow withAlpha(float value) {
        return new Shadow(radiusDp, offsetXDp, offsetYDp, color, value, blendMode);
    }

    public Shadow withRadius(float value) {
        return new Shadow(value, offsetXDp, offsetYDp, color, alpha, blendMode);
    }

    public Shadow withOffset(float xDp, float yDp) {
        return new Shadow(radiusDp, xDp, yDp, color, alpha, blendMode);
    }

    public Shadow withColor(int value) {
        return new Shadow(radiusDp, offsetXDp, offsetYDp, value, alpha, blendMode);
    }

    public Shadow withBlendMode(GlassBlendMode value) {
        return new Shadow(radiusDp, offsetXDp, offsetYDp, color, alpha, value);
    }

    public static Shadow fromConfig(Object table) {
        if (table == null) return null;
        if (table instanceof Shadow) return (Shadow) table;
        if (table instanceof Boolean && !((Boolean) table).booleanValue()) return null;

        float radius = LuaTableBridge.getFloat(
                table, new String[] {"radius", "radiusDp"}, 24f);
        float ox = LuaTableBridge.getFloat(
                table, new String[] {"offsetX", "offsetXDp", "x"}, 0f);
        float oy = LuaTableBridge.getFloat(
                table, new String[] {"offsetY", "offsetYDp", "y"}, radius / 6f);
        int color = LuaTableBridge.getInt(
                table, new String[] {"color"},
                Color.argb(Math.round(255f * 0.1f), 0, 0, 0));
        float alpha = LuaTableBridge.getFloat(
                table, new String[] {"alpha"}, 1f);
        String blend = LuaTableBridge.getString(
                table, new String[] {"blendMode", "blend"}, "srcOver");
        return new Shadow(
                radius, ox, oy, color, alpha,
                HighlightStyle.parseBlendMode(blend, GlassBlendMode.SRC_OVER));
    }

    public static final Shadow DEFAULT = new Shadow();

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof Shadow)) return false;
        Shadow s = (Shadow) other;
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
        return "Shadow(radiusDp=" + radiusDp
                + ", offsetXDp=" + offsetXDp
                + ", offsetYDp=" + offsetYDp
                + ", color=" + color
                + ", alpha=" + alpha
                + ", blendMode=" + blendMode + ")";
    }
}
