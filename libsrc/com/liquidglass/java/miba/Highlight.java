package com.liquidglass.java.miba;

/** Immutable data-equivalent port of highlight/Highlight.kt. Values are dp. */
public final class Highlight {
    public final float widthDp;
    public final float blurRadiusDp;
    public final float alpha;
    public final HighlightStyle style;

    public Highlight() {
        this(0.5f, 0.25f, 1f, HighlightStyle.DEFAULT);
    }

    /** Kotlin default-argument equivalent: Highlight(width = ...). */
    public Highlight(float widthDp) {
        this(widthDp, widthDp / 2f, 1f, HighlightStyle.DEFAULT);
    }

    public Highlight(float widthDp, float blurRadiusDp) {
        this(widthDp, blurRadiusDp, 1f, HighlightStyle.DEFAULT);
    }

    public Highlight(float widthDp, float blurRadiusDp, float alpha) {
        this(widthDp, blurRadiusDp, alpha, HighlightStyle.DEFAULT);
    }

    public Highlight(float widthDp, float blurRadiusDp, float alpha, HighlightStyle style) {
        this.widthDp = widthDp;
        this.blurRadiusDp = blurRadiusDp;
        this.alpha = alpha;
        this.style = style == null ? HighlightStyle.DEFAULT : style;
    }

    public float getWidthDp() { return widthDp; }
    public float getBlurRadiusDp() { return blurRadiusDp; }
    public float getAlpha() { return alpha; }
    public HighlightStyle getStyle() { return style; }

    public Highlight copy() {
        return new Highlight(widthDp, blurRadiusDp, alpha, style);
    }

    public Highlight copy(float widthDp, float blurRadiusDp, float alpha, HighlightStyle style) {
        return new Highlight(widthDp, blurRadiusDp, alpha, style);
    }

    public float component1() { return widthDp; }
    public float component2() { return blurRadiusDp; }
    public float component3() { return alpha; }
    public HighlightStyle component4() { return style; }

    public Highlight withAlpha(float value) {
        return new Highlight(widthDp, blurRadiusDp, value, style);
    }

    public Highlight withWidth(float width) {
        return new Highlight(width, blurRadiusDp, alpha, style);
    }

    public Highlight withBlurRadius(float blur) {
        return new Highlight(widthDp, blur, alpha, style);
    }

    public Highlight withStyle(HighlightStyle value) {
        return new Highlight(widthDp, blurRadiusDp, alpha, value);
    }

    public Highlight withWidthAndBlur(float width, float blur) {
        return new Highlight(width, blur, alpha, style);
    }

    public static Highlight fromConfig(Object table) {
        if (table == null) return null;
        if (table instanceof Highlight) return (Highlight) table;
        if (table instanceof Boolean && !((Boolean) table).booleanValue()) return null;

        float width = LuaTableBridge.getFloat(
                table, new String[] {"width", "widthDp"}, 0.5f);
        float blur = LuaTableBridge.getFloat(
                table, new String[] {"blurRadius", "blurRadiusDp", "blur"}, width / 2f);
        float alpha = LuaTableBridge.getFloat(
                table, new String[] {"alpha"}, 1f);
        Object styleValue = LuaTableBridge.getAny(table, new String[] {"style", "highlightStyle"});
        HighlightStyle style = styleValue == null
                ? HighlightStyle.DEFAULT
                : HighlightStyle.fromConfig(styleValue);
        return new Highlight(width, blur, alpha, style);
    }

    public static final Highlight DEFAULT = new Highlight();
    public static final Highlight AMBIENT =
            new Highlight(0.5f, 0.25f, 1f, HighlightStyle.AMBIENT);
    public static final Highlight PLAIN =
            new Highlight(0.5f, 0.25f, 1f, HighlightStyle.PLAIN);

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof Highlight)) return false;
        Highlight h = (Highlight) other;
        return Float.floatToIntBits(widthDp) == Float.floatToIntBits(h.widthDp)
                && Float.floatToIntBits(blurRadiusDp) == Float.floatToIntBits(h.blurRadiusDp)
                && Float.floatToIntBits(alpha) == Float.floatToIntBits(h.alpha)
                && style.equals(h.style);
    }

    @Override
    public int hashCode() {
        int result = Float.floatToIntBits(widthDp);
        result = 31 * result + Float.floatToIntBits(blurRadiusDp);
        result = 31 * result + Float.floatToIntBits(alpha);
        result = 31 * result + style.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "Highlight(widthDp=" + widthDp
                + ", blurRadiusDp=" + blurRadiusDp
                + ", alpha=" + alpha
                + ", style=" + style + ")";
    }
}
