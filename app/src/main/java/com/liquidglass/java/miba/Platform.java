package com.liquidglass.java.miba;

import android.os.Build;

public final class Platform {
    private Platform() {}

    public static boolean isRenderNodeSupported() {
        return Build.VERSION.SDK_INT >= 29;
    }

    public static boolean isRenderEffectSupported() {
        return Build.VERSION.SDK_INT >= 31;
    }

    public static boolean isRuntimeShaderSupported() {
        return Build.VERSION.SDK_INT >= 33;
    }
}
