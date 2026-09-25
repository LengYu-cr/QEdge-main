package com.liquidglass.java.miba;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Shader;
import android.net.Uri;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/** Direct Android Java port of catalog/utils/SdfShader.kt + SdfShader.android.kt. */
public final class SdfShader {
    private static final String SOURCE =
            "uniform shader content;\n" +
            "uniform shader sdfTex;\n" +
            "\n" +
            "uniform float2 size;\n" +
            "uniform float2 sdfTexSize;\n" +
            "uniform float refractionHeight;\n" +
            "uniform float lightAngle;\n" +
            "\n" +
            "float circleMap(float x) {\n" +
            "    return 1.0 - sqrt(1.0 - x * x);\n" +
            "}\n" +
            "\n" +
            "half4 main(float2 coord) {\n" +
            "    half2 p = coord / size * sdfTexSize;\n" +
            "    if (p.x < 0.0 || p.y < 0.0 || p.x >= sdfTexSize.x || p.y >= sdfTexSize.y) {\n" +
            "        return half4(0.0);\n" +
            "    }\n" +
            "    half4 v = sdfTex.eval(p);\n" +
            "    float sd = v.r * 2.0 - 1.0;\n" +
            "    v.a = smoothstep(0.5, 1.0, v.a);\n" +
            "    if (v.a <= 0.0) {\n" +
            "        return half4(0.0);\n" +
            "    }\n" +
            "    if (v.a < 1.0) {\n" +
            "        sd = 0.0;\n" +
            "    }\n" +
            "    float2 normal = normalize(v.gb * 2.0 - 1.0);\n" +
            "    \n" +
            "    float intensity = circleMap(1.0 - min(1.0, -sd * 1.5));\n" +
            "    float2 refractedCoord = coord - intensity * refractionHeight * normal;\n" +
            "\n" +
            "    half4 color = content.eval(refractedCoord) * v.a;\n" +
            "    float2 lightDir = float2(cos(lightAngle * 3.1415926 / 180.0), sin(lightAngle * 3.1415926 / 180.0));\n" +
            "    float bevelIntensity = clamp(dot(normal, lightDir), 0.0, 1.0);\n" +
            "    color.rgb *= 1.0 + 0.5 * intensity * bevelIntensity;\n" +
            "    bevelIntensity = clamp(dot(normal, -lightDir), 0.0, 1.0);\n" +
            "    color.rgb *= 1.0 + 0.5 * bevelIntensity * min(1.0, smoothstep(1.0, 0.0, abs(intensity - 0.25) * 6.0));\n" +
            "    return color;\n" +
            "}";

    private final Bitmap bitmap;
    private final Object bitmapShader;

    public SdfShader(Bitmap bitmap) {
        if (bitmap == null) throw new IllegalArgumentException("bitmap == null");
        this.bitmap = bitmap;
        this.bitmapShader = new BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
    }

    public Bitmap getBitmap() { return bitmap; }
    public int getWidth() { return bitmap.getWidth(); }
    public int getHeight() { return bitmap.getHeight(); }

    /** Exact KT defaults: refractionHeight = 48.dp, lightAngle = 45 degrees. */
    public BackdropEffectScope apply(BackdropEffectScope scope) {
        return apply(scope, 48f * (scope == null ? 1f : scope.getDensity()), 45f);
    }

    public BackdropEffectScope apply(final BackdropEffectScope scope, final float refractionHeightPx) {
        return apply(scope, refractionHeightPx, 45f);
    }

    public BackdropEffectScope apply(final BackdropEffectScope scope,
                                     final float refractionHeightPx,
                                     final float lightAngle) {
        if (scope == null || !Platform.isRuntimeShaderSupported()) return scope;
        scope.runtimeShaderEffect("SdfShader", SOURCE, "content", new BackdropEffectScope.RuntimeShaderBlock() {
            public void apply(RuntimeShader shader) {
                shader.setInputBuffer("sdfTex", bitmapShader);
                shader.setFloatUniform("size", scope.getWidth(), scope.getHeight());
                shader.setFloatUniform("sdfTexSize", getWidth(), getHeight());
                shader.setFloatUniform("refractionHeight", refractionHeightPx);
                shader.setFloatUniform("lightAngle", lightAngle);
            }
        });
        return scope;
    }

    public static SdfShader fromPng(String path) {
        if (path == null || path.length() == 0) return null;
        Bitmap bitmap = BitmapFactory.decodeFile(path.startsWith("file://") ? Uri.parse(path).getPath() : path);
        return bitmap == null ? null : new SdfShader(bitmap);
    }

    public static SdfShader fromPng(File file) {
        return file == null ? null : fromPng(file.getAbsolutePath());
    }

    public static SdfShader fromBytes(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return null;
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        return bitmap == null ? null : new SdfShader(bitmap);
    }

    public static SdfShader fromAsset(Context context, String assetPath) {
        if (context == null || assetPath == null || assetPath.length() == 0) return null;
        InputStream in = null;
        try {
            in = context.getAssets().open(assetPath);
            Bitmap bitmap = BitmapFactory.decodeStream(in);
            return bitmap == null ? null : new SdfShader(bitmap);
        } catch (IOException ignored) {
            return null;
        } finally {
            if (in != null) try { in.close(); } catch (IOException ignored) { }
        }
    }

    public static SdfShader fromUri(Context context, String uri) {
        if (context == null || uri == null || uri.length() == 0) return null;
        InputStream in = null;
        try {
            in = context.getContentResolver().openInputStream(Uri.parse(uri));
            Bitmap bitmap = in == null ? null : BitmapFactory.decodeStream(in);
            return bitmap == null ? null : new SdfShader(bitmap);
        } catch (Throwable ignored) {
            return null;
        } finally {
            if (in != null) try { in.close(); } catch (IOException ignored) { }
        }
    }

    public static String getShaderSource() { return SOURCE; }
}
