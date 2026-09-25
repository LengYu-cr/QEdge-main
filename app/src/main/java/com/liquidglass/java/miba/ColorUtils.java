package com.liquidglass.java.miba;

import android.graphics.Color;

public final class ColorUtils {
    private ColorUtils() {}

    public static int withAlpha(int color, float alpha) {
        float a = Math.max(0f, Math.min(1f, alpha));
        return Color.argb(Math.round(255f * a), Color.red(color), Color.green(color), Color.blue(color));
    }

    public static int argb(float alpha, int rgb) {
        return Color.argb(Math.round(Math.max(0f, Math.min(1f, alpha)) * 255f), Color.red(rgb), Color.green(rgb), Color.blue(rgb));
    }

    public static int lerp(int start, int stop, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int a = Math.round(Color.alpha(start) + (Color.alpha(stop) - Color.alpha(start)) * t);
        int r = Math.round(Color.red(start) + (Color.red(stop) - Color.red(start)) * t);
        int g = Math.round(Color.green(start) + (Color.green(stop) - Color.green(start)) * t);
        int b = Math.round(Color.blue(start) + (Color.blue(stop) - Color.blue(start)) * t);
        return Color.argb(a, r, g, b);
    }
}
