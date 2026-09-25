package com.liquidglass.java.miba;

import android.graphics.Path;
import android.graphics.RectF;

/** Four-corner rounded rectangle used by the original lens SDF. Values are pixels. */
public final class GlassShape {
    private final float topLeft;
    private final float topRight;
    private final float bottomRight;
    private final float bottomLeft;

    public GlassShape(float radiusPx) {
        this(radiusPx, radiusPx, radiusPx, radiusPx);
    }

    public GlassShape(float topLeftPx, float topRightPx, float bottomRightPx, float bottomLeftPx) {
        topLeft = Math.max(0f, topLeftPx);
        topRight = Math.max(0f, topRightPx);
        bottomRight = Math.max(0f, bottomRightPx);
        bottomLeft = Math.max(0f, bottomLeftPx);
    }

    public static GlassShape capsule(float widthPx, float heightPx) {
        return new GlassShape(Math.min(widthPx, heightPx) * 0.5f);
    }

    /** Capsule independent of measured size; Float.MAX_VALUE is clamped at draw time. */
    public static GlassShape capsule() { return new GlassShape(Float.MAX_VALUE); }

    public float getTopLeftPx() { return topLeft; }
    public float getTopRightPx() { return topRight; }
    public float getBottomRightPx() { return bottomRight; }
    public float getBottomLeftPx() { return bottomLeft; }

    public GlassShape copy() { return new GlassShape(topLeft, topRight, bottomRight, bottomLeft); }

    public GlassShape withRadiusPx(float radiusPx) { return new GlassShape(radiusPx); }

    public static GlassShape fromConfig(Object table) {
        if (table == null) return capsule();
        if (table instanceof GlassShape) return (GlassShape) table;
        if (table instanceof Number) return new GlassShape(((Number) table).floatValue());

        Object capsuleValue = LuaTableBridge.getAny(table, new String[] {"capsule", "isCapsule"});
        if (capsuleValue != null && LuaTableBridge.getBoolean(table, new String[] {"capsule", "isCapsule"}, false)) {
            return capsule();
        }

        float radius = LuaTableBridge.getFloat(table,
                new String[] {"radius", "radiusPx", "cornerRadius", "cornerRadiusPx"}, Float.NaN);
        if (!Float.isNaN(radius)) return new GlassShape(radius);

        float tl = LuaTableBridge.getFloat(table, new String[] {"topLeft", "topLeftPx", "topLeftRadius"}, 0f);
        float tr = LuaTableBridge.getFloat(table, new String[] {"topRight", "topRightPx", "topRightRadius"}, tl);
        float br = LuaTableBridge.getFloat(table, new String[] {"bottomRight", "bottomRightPx", "bottomRightRadius"}, tl);
        float bl = LuaTableBridge.getFloat(table, new String[] {"bottomLeft", "bottomLeftPx", "bottomLeftRadius"}, tl);
        return new GlassShape(tl, tr, br, bl);
    }

    public float[] getCornerRadii(float width, float height) {
        float max = Math.max(0f, Math.min(width, height) * 0.5f);
        return new float[] {
                Math.min(topLeft, max),
                Math.min(topRight, max),
                Math.min(bottomRight, max),
                Math.min(bottomLeft, max)
        };
    }

    public float component1() { return topLeft; }
    public float component2() { return topRight; }
    public float component3() { return bottomRight; }
    public float component4() { return bottomLeft; }

    public Path createPath(float width, float height) {
        float[] r = getCornerRadii(width, height);
        Path path = new Path();
        RectF rect = new RectF(0f, 0f, width, height);
        path.addRoundRect(rect, new float[] {
                r[0], r[0], r[1], r[1], r[2], r[2], r[3], r[3]
        }, Path.Direction.CW);
        return path;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof GlassShape)) return false;
        GlassShape other = (GlassShape) object;
        return Float.compare(topLeft, other.topLeft) == 0
                && Float.compare(topRight, other.topRight) == 0
                && Float.compare(bottomRight, other.bottomRight) == 0
                && Float.compare(bottomLeft, other.bottomLeft) == 0;
    }

    @Override
    public int hashCode() {
        int result = Float.floatToIntBits(topLeft);
        result = 31 * result + Float.floatToIntBits(topRight);
        result = 31 * result + Float.floatToIntBits(bottomRight);
        result = 31 * result + Float.floatToIntBits(bottomLeft);
        return result;
    }

    @Override
    public String toString() {
        return "GlassShape(topLeft=" + topLeft + ", topRight=" + topRight
                + ", bottomRight=" + bottomRight + ", bottomLeft=" + bottomLeft + ")";
    }
}
