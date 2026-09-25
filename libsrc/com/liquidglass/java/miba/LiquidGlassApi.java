package com.liquidglass.java.miba;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.view.View;

/**
 * Lua-friendly static entry points mirroring the public KT factory/extension API.
 * This class has no Kotlin/Compose dependency.
 */
public final class LiquidGlassApi {
    private LiquidGlassApi() {}

    public static Backdrop emptyBackdrop() { return EmptyBackdrop.INSTANCE; }
    public static LayerBackdrop layerBackdrop() { return new LayerBackdrop(); }
    public static LayerBackdrop layerBackdrop(View source) { return new LayerBackdrop(source); }
    public static CanvasBackdrop canvasBackdrop(CanvasBackdrop.Drawer drawer) { return new CanvasBackdrop(drawer); }
    public static CallbackBackdrop backdrop(Backdrop source, CallbackBackdrop.Drawer drawer) { return new CallbackBackdrop(source, drawer); }
    public static CombinedBackdrop combinedBackdrop(Backdrop first, Backdrop second) { return new CombinedBackdrop(first, second); }
    public static CombinedBackdrop combinedBackdrop(Backdrop first, Backdrop second, Backdrop third) { return new CombinedBackdrop(first, second, third); }
    public static CombinedBackdrop combinedBackdrop(Backdrop[] backdrops) { return new CombinedBackdrop(backdrops); }
    public static TransformedBackdrop transformedBackdrop(Backdrop source, TransformedBackdrop.Transform transform) { return new TransformedBackdrop(source, transform); }

    public static GlassShape capsule() { return GlassShape.capsule(); }
    public static GlassShape rounded(float radiusPx) { return new GlassShape(radiusPx); }
    public static GlassShape rounded(float topLeftPx, float topRightPx, float bottomRightPx, float bottomLeftPx) { return new GlassShape(topLeftPx, topRightPx, bottomRightPx, bottomLeftPx); }
    public static GlassShape shape(Object table) { return GlassShape.fromConfig(table); }

    public static Highlight highlight() { return new Highlight(); }
    public static Highlight highlight(float widthDp, float blurRadiusDp, float alpha, HighlightStyle style) { return new Highlight(widthDp, blurRadiusDp, alpha, style); }
    public static Highlight highlight(Object table) { return Highlight.fromConfig(table); }
    public static HighlightStyle highlightStyle(Object table) { return HighlightStyle.fromConfig(table); }
    public static Shadow shadow() { return new Shadow(); }
    public static Shadow shadow(float radiusDp, float offsetXDp, float offsetYDp, int color, float alpha) { return new Shadow(radiusDp, offsetXDp, offsetYDp, color, alpha); }
    public static Shadow shadow(Object table) { return Shadow.fromConfig(table); }
    public static InnerShadow innerShadow() { return new InnerShadow(); }
    public static InnerShadow innerShadow(float radiusDp, float offsetXDp, float offsetYDp, int color, float alpha) { return new InnerShadow(radiusDp, offsetXDp, offsetYDp, color, alpha); }
    public static InnerShadow innerShadow(Object table) { return InnerShadow.fromConfig(table); }
    public static SimpleGlassEffects effects() { return new SimpleGlassEffects(); }
    public static SimpleGlassEffects effects(Object table) { return new SimpleGlassEffects(table); }

    public static LiquidButton button(Context context) { return new LiquidButton(context); }
    public static LiquidButton button(Context context, Object config) { return new LiquidButton(context).applyConfig(config); }
    public static LiquidToggle toggle(Context context) { return new LiquidToggle(context); }
    public static LiquidToggle toggle(Context context, Object config) { return new LiquidToggle(context).applyConfig(config); }
    public static LiquidSlider slider(Context context) { return new LiquidSlider(context); }
    public static LiquidSlider slider(Context context, Object config) { return new LiquidSlider(context).applyConfig(config); }
    public static LiquidBottomTabs bottomTabs(Context context) { return new LiquidBottomTabs(context); }
    public static LiquidBottomTabs bottomTabs(Context context, Object config) { LiquidBottomTabs v = new LiquidBottomTabs(context); v.applyConfig(config); return v; }
    public static LiquidBottomTab bottomTab(Context context) { return new LiquidBottomTab(context); }
    public static LiquidBottomTab bottomTab(Context context, Object config) { return new LiquidBottomTab(context).applyConfig(config); }
    public static LiquidGlassView glass(Context context) { return new LiquidGlassView(context); }
    public static LiquidGlassView glass(Context context, Object config) { return new LiquidGlassView(context).applyConfig(config); }
    public static LiquidGlassBackdropLayout backdropLayout(Context context) { return new LiquidGlassBackdropLayout(context); }
    public static UISensor uiSensor(Context context) { return new UISensor(context); }
    public static ProgressConverter defaultProgressConverter() { return ProgressConverter.DEFAULT; }
    public static RuntimeShader runtimeShader(String shaderString) { return new RuntimeShader(shaderString); }
    public static RuntimeShaderCache runtimeShaderCache() { return new RuntimeShaderCache(); }
    public static SdfShader sdfShader(Bitmap bitmap) { return new SdfShader(bitmap); }
    public static SdfShader sdfShaderPng(String path) { return SdfShader.fromPng(path); }
    public static SdfShader sdfShaderBytes(byte[] bytes) { return SdfShader.fromBytes(bytes); }
    public static SdfShader sdfShaderAsset(Context context, String path) { return SdfShader.fromAsset(context, path); }
    public static SdfShader sdfShaderUri(Context context, String uri) { return SdfShader.fromUri(context, uri); }

    /** Shared automatic backdrop used by normal Activity/View layouts. */
    public static Backdrop autoBackdrop(View view) { return ActivityBackdropManager.getBackdrop(view); }
    public static void invalidateAutoBackdrop(View view) { ActivityBackdropManager.invalidate(view); }
    public static void setAutoBackdropContinuous(View view, boolean enabled) { ActivityBackdropManager.setContinuousCapture(view, enabled); }
    public static boolean isAutoBackdropContinuous(View view) { return ActivityBackdropManager.isContinuousCapture(view); }

    /** Dispatches a Lua/Map/table config to any supported public component. */
    public static Object configure(Object target, Object table) {
        if (target == null || table == null) return target;
        if (target instanceof LiquidButton) return ((LiquidButton) target).applyConfig(table);
        if (target instanceof LiquidToggle) return ((LiquidToggle) target).applyConfig(table);
        if (target instanceof LiquidSlider) return ((LiquidSlider) target).applyConfig(table);
        if (target instanceof LiquidBottomTabs) { ((LiquidBottomTabs) target).applyConfig(table); return target; }
        if (target instanceof LiquidBottomTab) return ((LiquidBottomTab) target).applyConfig(table);
        if (target instanceof LiquidGlassView) return ((LiquidGlassView) target).applyConfig(table);
        if (target instanceof SimpleGlassEffects) return ((SimpleGlassEffects) target).applyConfig(table);
        return target;
    }

    public static float dp(Context context, float valueDp) { return context == null ? valueDp : valueDp * context.getResources().getDisplayMetrics().density; }
    public static int argb(int alpha, int red, int green, int blue) { return Color.argb(alpha, red, green, blue); }
    public static int colorWithAlpha(int color, float alpha) { return ColorUtils.withAlpha(color, alpha); }
    public static boolean isRenderEffectSupported() { return Platform.isRenderEffectSupported(); }
    public static boolean isRuntimeShaderSupported() { return Platform.isRuntimeShaderSupported(); }
}
