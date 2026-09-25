package com.liquidglass.java.miba;

import android.graphics.Color;

/**
 * Immutable data-equivalent port of highlight/HighlightStyle.kt.
 *
 * createShader(...) is the Java/View equivalent of the public KT
 * DrawScope.createShader(shape, runtimeShaderCache) method.
 */
public abstract class HighlightStyle {
    public final int color;
    public final GlassBlendMode blendMode;

    protected HighlightStyle(int color, GlassBlendMode blendMode) {
        this.color = color;
        this.blendMode = blendMode == null ? GlassBlendMode.SRC_OVER : blendMode;
    }

    public int getColor() { return color; }
    public GlassBlendMode getBlendMode() { return blendMode; }

    public RuntimeShader createShader(float widthPx, float heightPx, GlassShape shape) {
        return createShader(widthPx, heightPx, shape, new RuntimeShaderCache());
    }

    public RuntimeShader createShader(float widthPx, float heightPx,
                                      GlassShape shape, RuntimeShaderCache cache) {
        if (!Platform.isRuntimeShaderSupported()) return null;
        if (this instanceof Plain) return null;
        if (cache == null) cache = new RuntimeShaderCache();
        if (shape == null) shape = GlassShape.capsule();

        RuntimeShader shader;
        if (this instanceof Ambient) {
            shader = cache.obtainRuntimeShader("Ambient", Shaders.AMBIENT_HIGHLIGHT);
            shader.setFloatUniform("size", widthPx, heightPx);
            shader.setFloatUniform("cornerRadii", shape.getCornerRadii(widthPx, heightPx));
            shader.setFloatUniform("angle", 45f * (float) (Math.PI / 180.0));
            shader.setFloatUniform("falloff", 1f);
            return shader;
        }

        Default d = (Default) this;
        shader = cache.obtainRuntimeShader("Default", Shaders.DEFAULT_HIGHLIGHT);
        shader.setFloatUniform("size", widthPx, heightPx);
        shader.setFloatUniform("cornerRadii", shape.getCornerRadii(widthPx, heightPx));
        shader.setColorUniform(
                "color",
                Color.argb(255, Color.red(d.color), Color.green(d.color), Color.blue(d.color)));
        shader.setFloatUniform("angle", d.angle * (float) (Math.PI / 180.0));
        shader.setFloatUniform("falloff", d.falloff);
        return shader;
    }

    public static final class Plain extends HighlightStyle {
        public Plain() {
            this(Color.argb(Math.round(255f * 0.38f), 255, 255, 255), GlassBlendMode.PLUS);
        }

        public Plain(int color) {
            this(color, GlassBlendMode.PLUS);
        }

        public Plain(int color, GlassBlendMode blendMode) {
            super(color, blendMode);
        }

        public Plain copy() {
            return new Plain(color, blendMode);
        }

        public Plain copy(int color, GlassBlendMode blendMode) {
            return new Plain(color, blendMode);
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof Plain)) return false;
            Plain p = (Plain) other;
            return color == p.color && blendMode == p.blendMode;
        }

        @Override
        public int hashCode() {
            return 31 * color + blendMode.hashCode();
        }

        @Override
        public String toString() {
            return "HighlightStyle.Plain(color=" + color + ", blendMode=" + blendMode + ")";
        }
    }

    public static final class Default extends HighlightStyle {
        public final float angle;
        public final float falloff;

        public Default() {
            this(Color.argb(Math.round(255f * 0.5f), 255, 255, 255),
                    GlassBlendMode.PLUS, 45f, 1f);
        }

        public Default(float angle, float falloff) {
            this(Color.argb(Math.round(255f * 0.5f), 255, 255, 255),
                    GlassBlendMode.PLUS, angle, falloff);
        }

        public Default(int color, GlassBlendMode blendMode) {
            this(color, blendMode, 45f, 1f);
        }

        public Default(int color, GlassBlendMode blendMode, float angle, float falloff) {
            super(color, blendMode);
            this.angle = angle;
            this.falloff = falloff;
        }

        public float getAngle() { return angle; }
        public float getFalloff() { return falloff; }

        public Default copy() {
            return new Default(color, blendMode, angle, falloff);
        }

        public Default copy(int color, GlassBlendMode blendMode, float angle, float falloff) {
            return new Default(color, blendMode, angle, falloff);
        }

        public Default withColor(int value) {
            return new Default(value, blendMode, angle, falloff);
        }

        public Default withBlendMode(GlassBlendMode value) {
            return new Default(color, value, angle, falloff);
        }

        public Default withAngle(float value) {
            return new Default(color, blendMode, value, falloff);
        }

        public Default withFalloff(float value) {
            return new Default(color, blendMode, angle, value);
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof Default)) return false;
            Default d = (Default) other;
            return color == d.color
                    && blendMode == d.blendMode
                    && Float.floatToIntBits(angle) == Float.floatToIntBits(d.angle)
                    && Float.floatToIntBits(falloff) == Float.floatToIntBits(d.falloff);
        }

        @Override
        public int hashCode() {
            int result = 31 * color + blendMode.hashCode();
            result = 31 * result + Float.floatToIntBits(angle);
            result = 31 * result + Float.floatToIntBits(falloff);
            return result;
        }

        @Override
        public String toString() {
            return "HighlightStyle.Default(color=" + color
                    + ", blendMode=" + blendMode
                    + ", angle=" + angle
                    + ", falloff=" + falloff + ")";
        }
    }

    public static final class Ambient extends HighlightStyle {
        public final float intensity;

        public Ambient() {
            this(0.38f);
        }

        public Ambient(float intensity) {
            super(Color.argb(Math.round(255f * clamp01(intensity)), 255, 255, 255),
                    GlassBlendMode.SRC_OVER);
            this.intensity = intensity;
        }

        public float getIntensity() { return intensity; }

        public Ambient copy() {
            return new Ambient(intensity);
        }

        public Ambient copy(float intensity) {
            return new Ambient(intensity);
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof Ambient)) return false;
            Ambient a = (Ambient) other;
            return Float.floatToIntBits(intensity) == Float.floatToIntBits(a.intensity);
        }

        @Override
        public int hashCode() {
            return Float.floatToIntBits(intensity);
        }

        @Override
        public String toString() {
            return "HighlightStyle.Ambient(intensity=" + intensity + ")";
        }
    }

    public static final Default DEFAULT = new Default();
    public static final Ambient AMBIENT = new Ambient();
    public static final Plain PLAIN = new Plain();

    public static Plain plain() { return new Plain(); }
    public static Default defaultStyle() { return new Default(); }
    public static Ambient ambient() { return new Ambient(); }

    /**
     * Lua/table parser:
     * "default", "ambient", "plain"
     * or {type="default", color=..., angle=45, falloff=1}
     */
    public static HighlightStyle fromConfig(Object value) {
        if (value == null) return DEFAULT;
        if (value instanceof HighlightStyle) return (HighlightStyle) value;

        if (value instanceof CharSequence) {
            String name = String.valueOf(value);
            if ("plain".equalsIgnoreCase(name)) return PLAIN;
            if ("ambient".equalsIgnoreCase(name)) return AMBIENT;
            return DEFAULT;
        }

        String type = LuaTableBridge.getString(
                value, new String[] {"type", "name", "style"}, "default");

        if ("plain".equalsIgnoreCase(type)) {
            int color = LuaTableBridge.getInt(
                    value, new String[] {"color"},
                    Color.argb(Math.round(255f * 0.38f), 255, 255, 255));
            GlassBlendMode mode = parseBlendMode(
                    LuaTableBridge.getString(value, new String[] {"blendMode", "blend"}, "plus"),
                    GlassBlendMode.PLUS);
            return new Plain(color, mode);
        }

        if ("ambient".equalsIgnoreCase(type)) {
            return new Ambient(LuaTableBridge.getFloat(
                    value, new String[] {"intensity", "alpha"}, 0.38f));
        }

        int color = LuaTableBridge.getInt(
                value, new String[] {"color"},
                Color.argb(Math.round(255f * 0.5f), 255, 255, 255));
        GlassBlendMode mode = parseBlendMode(
                LuaTableBridge.getString(value, new String[] {"blendMode", "blend"}, "plus"),
                GlassBlendMode.PLUS);
        float angle = LuaTableBridge.getFloat(
                value, new String[] {"angle"}, 45f);
        float falloff = LuaTableBridge.getFloat(
                value, new String[] {"falloff"}, 1f);
        return new Default(color, mode, angle, falloff);
    }

    public static GlassBlendMode parseBlendMode(String name, GlassBlendMode fallback) {
        if (name == null) return fallback;
        if ("plus".equalsIgnoreCase(name)
                || "add".equalsIgnoreCase(name)
                || "additive".equalsIgnoreCase(name)) {
            return GlassBlendMode.PLUS;
        }
        if ("srcOver".equalsIgnoreCase(name)
                || "src_over".equalsIgnoreCase(name)
                || "sourceOver".equalsIgnoreCase(name)
                || "normal".equalsIgnoreCase(name)) {
            return GlassBlendMode.SRC_OVER;
        }
        return fallback;
    }

    private static float clamp01(float value) {
        return Math.max(0f, Math.min(1f, value));
    }
}
