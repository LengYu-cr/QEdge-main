package me.lengyu.qedge.ui.widget.glass;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;

/**
 * 极光的绘制内核。
 *
 * 它被两处共用，这是整个真液态玻璃能成立的前提：
 * <ul>
 *   <li>{@link AuroraView#onDraw} —— 把极光画到屏幕上；</li>
 *   <li>玻璃卡片的 backdrop —— 把同一份极光画进玻璃的采样画布。</li>
 * </ul>
 * 两边用同一套光斑参数、同一个时间基准，所以玻璃折射出来的内容和它背后的背景
 * 严丝合缝，不会出现"玻璃里漂着另一片极光"的错位感。
 *
 * 之所以不让玻璃去采样整块屏幕（库的自动 backdrop 模式），是因为卡片的内容本身
 * 也被画在屏幕上：采样整屏 → 卡片会把自己的文字图标一起吸进去，边缘糊出一圈
 * 自己的重影。改成只采样极光这一层，采样源里没有任何前景内容，干净且开销小。
 */
public final class AuroraRenderer {

    /** 深空底：比纯黑多一点点蓝，叠上光斑后像夜空而不是"熄屏"。 */
    public static final int DARK_BASE = 0xFF05060D;

    private static final float TWO_PI = (float) (Math.PI * 2);

    private AuroraRenderer() {}

    /** 一块极光光斑。字段含义与 Compose 版一一对应。 */
    private static final class Blob {
        final int color;
        final float centerX;
        final float centerY;
        final float radius;
        final float driftX;
        final float driftY;
        final int periodMs;
        final float phase;

        Blob(int argb, float centerX, float centerY, float radius,
             float driftX, float driftY, int periodMs, float phase) {
            this.color = argb;
            this.centerX = centerX;
            this.centerY = centerY;
            this.radius = radius;
            this.driftX = driftX;
            this.driftY = driftY;
            this.periodMs = periodMs;
            this.phase = phase;
        }
    }

    /**
     * 暗色极光：青 / 紫 / 品红 / 深蓝四团大半径光晕。
     * 透明度压在 0.20 上下，四团叠加后才刚好有颜色，单团不过曝，
     * 保证黑色仍然是黑的、文字对比度不受影响。
     */
    private static final Blob[] DARK = {
            new Blob(withAlpha(0xFF19E3FF, 0.26f), 0.16f, 0.06f, 0.95f, 0.10f, 0.06f, 17000, 0.00f),
            new Blob(withAlpha(0xFF7B4DFF, 0.28f), 0.90f, 0.16f, 0.85f, 0.08f, 0.08f, 23000, 0.35f),
            new Blob(withAlpha(0xFFFF2E8A, 0.18f), 0.74f, 0.88f, 0.80f, 0.09f, 0.07f, 29000, 0.60f),
            new Blob(withAlpha(0xFF2F6BFF, 0.24f), 0.10f, 0.80f, 0.90f, 0.07f, 0.09f, 19000, 0.82f)
    };

    /**
     * 亮色极光：马卡龙浅晕（高明度、低饱和）。
     * 亮色下玻璃表面本来就接近纯白，这里只要一点点彩色，
     * 玻璃棱边和折射就能"吃到"颜色，同时不压低黑字的对比度。
     */
    private static final Blob[] LIGHT = {
            new Blob(withAlpha(0xFF9CC4FF, 0.55f), 0.16f, 0.06f, 0.95f, 0.10f, 0.06f, 17000, 0.00f),
            new Blob(withAlpha(0xFFFFB3D9, 0.45f), 0.90f, 0.16f, 0.85f, 0.08f, 0.08f, 23000, 0.35f),
            new Blob(withAlpha(0xFFB9A6FF, 0.42f), 0.74f, 0.88f, 0.80f, 0.09f, 0.07f, 29000, 0.60f),
            new Blob(withAlpha(0xFFA6EFD5, 0.45f), 0.10f, 0.80f, 0.90f, 0.07f, 0.09f, 19000, 0.82f)
    };

    private static final Paint PAINT = new Paint(Paint.ANTI_ALIAS_FLAG);

    /**
     * 把极光画进一块 {@code width × height} 的画布。
     *
     * @param baseColor     底色（暗色用 {@link #DARK_BASE}，亮色用页面底色）
     * @param vignetteColor 暗角色
     * @param timeMs        时间基准，背景层与玻璃采样层必须传同一个值
     */
    public static void draw(Canvas canvas, float width, float height,
                            int baseColor, int vignetteColor, boolean dark, long timeMs) {
        if (width <= 0f || height <= 0f) return;

        PAINT.setShader(null);
        PAINT.setColor(baseColor);
        canvas.drawRect(0f, 0f, width, height, PAINT);

        final float maxDim = Math.max(width, height);
        final Blob[] blobs = dark ? DARK : LIGHT;
        for (Blob blob : blobs) {
            // sin/cos 在 progress 0 与 1 处取值相同，周期首尾相接，循环点不会有跳变。
            final float progress = (timeMs % blob.periodMs) / (float) blob.periodMs;
            final float angle = (progress + blob.phase) * TWO_PI;
            final float cx = blob.centerX * width + (float) Math.sin(angle) * blob.driftX * width;
            final float cy = blob.centerY * height + (float) Math.cos(angle) * blob.driftY * height;
            final float radius = maxDim * blob.radius;
            if (radius <= 0f) continue;

            final float alpha = alphaOf(blob.color);
            PAINT.setShader(new RadialGradient(
                    cx, cy, radius,
                    new int[]{blob.color, withAlpha(blob.color, alpha * 0.5f), withAlpha(blob.color, 0f)},
                    new float[]{0f, 0.45f, 1f},
                    Shader.TileMode.CLAMP));
            canvas.drawCircle(cx, cy, radius, PAINT);
        }

        // 暗角：中心留亮、四周压暗。光斑叠在中心时最亮，边缘自然收进去，
        // 页面内容不会因为背景过亮而显得发灰。
        PAINT.setShader(new RadialGradient(
                width * 0.5f, height * 0.42f, maxDim * 0.78f,
                new int[]{0x00000000, 0x00000000, vignetteColor},
                new float[]{0f, 0.55f, 1f},
                Shader.TileMode.CLAMP));
        canvas.drawRect(0f, 0f, width, height, PAINT);

        PAINT.setShader(null);
    }

    public static float alphaOf(int color) {
        return ((color >>> 24) & 0xFF) / 255f;
    }

    public static int withAlpha(int color, float alpha) {
        if (alpha < 0f) alpha = 0f;
        if (alpha > 1f) alpha = 1f;
        return (color & 0x00FFFFFF) | (((int) (alpha * 255f + 0.5f)) << 24);
    }
}
