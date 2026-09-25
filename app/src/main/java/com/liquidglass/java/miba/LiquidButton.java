package com.liquidglass.java.miba;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Port of catalog/components/LiquidButton.kt. */
public class LiquidButton extends LiquidGlassView {
    private static final int INVALID_POINTER_ID = -1;
    private final LinearLayout contentLayout;
    private final InteractiveHighlight interactiveHighlight;
    private boolean interactive = true;
    private boolean hasTint;
    private int tint;
    private boolean hasSurfaceColor;
    private int surfaceColor;
    private float downX;
    private float downY;
    private int activePointerId = INVALID_POINTER_ID;
    private ImageView generatedImageView;
    private TextView generatedTextView;
    private float imageSizeDp = 24f;
    private String imagePath;
    private String lastImageLoadError;

    /*
     * Java/Lua convenience using the repository's
     * AdaptiveLuminanceGlassContent.kt algorithm: 5x5 backdrop sample,
     * Rec.709 luminance, black/white target at 0.5, 1000 ms tween.
     */
    private boolean adaptiveContentColorEnabled = true;
    private int contentColor;
    private int contentColorStart;
    private int contentColorTarget;
    private float lastBackdropLuminance = -1f;
    private float luminanceThreshold = 0.5f;
    private long contentColorAnimationStart;
    private long contentColorAnimationDurationMs = 1000L;
    private long adaptiveSampleIntervalMs = 120L;
    private long lastAdaptiveSampleTime;
    private boolean adaptiveColorAnimating;
    private boolean adaptiveColorPosted;

    private final Runnable adaptiveColorRunnable = new Runnable() {
        public void run() {
            adaptiveColorPosted = false;
            if (!adaptiveContentColorEnabled || !isAttachedToWindow()) return;

            long now = SystemClock.uptimeMillis();
            if (lastAdaptiveSampleTime == 0L || now - lastAdaptiveSampleTime >= adaptiveSampleIntervalMs) {
                lastAdaptiveSampleTime = now;
                float luminance = ActivityBackdropManager.sampleAverageLuminance(LiquidButton.this, 5, 5);
                if (luminance >= 0f) {
                    lastBackdropLuminance = luminance;
                    int target = luminance > luminanceThreshold ? Color.BLACK : Color.WHITE;
                    if (target != contentColorTarget) {
                        contentColorStart = contentColor;
                        contentColorTarget = target;
                        contentColorAnimationStart = now;
                        adaptiveColorAnimating = contentColor != contentColorTarget;
                    }
                }
            }

            if (adaptiveColorAnimating) {
                float t = contentColorAnimationDurationMs <= 0L
                        ? 1f
                        : (now - contentColorAnimationStart) / (float) contentColorAnimationDurationMs;
                if (t >= 1f) {
                    t = 1f;
                    adaptiveColorAnimating = false;
                }
                contentColor = lerpColor(contentColorStart, contentColorTarget,
                        fastOutSlowIn(clamp01(t)));
                applyContentColorToButton();
            } else if (contentColor != contentColorTarget) {
                contentColor = contentColorTarget;
                applyContentColorToButton();
            }

            scheduleAdaptiveColor(adaptiveColorAnimating ? 16L : adaptiveSampleIntervalMs);
        }
    };

    public LiquidButton(Context context) { this(context, null); }
    public LiquidButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        int nightMode = getResources().getConfiguration().uiMode & 0x30;
        contentColor = nightMode == 0x20 ? Color.WHITE : Color.BLACK;
        contentColorStart = contentColor;
        contentColorTarget = contentColor;
        final float d = getResources().getDisplayMetrics().density;
        setShape(new GlassShape(Float.MAX_VALUE));
        setEffects(new Effects() {
            public void apply(BackdropEffectScope scope) {
                scope.vibrancy();
                scope.blur(2f * d);
                scope.lens(12f * d, 24f * d);
            }
        });

        contentLayout = new SpacedRow(context, dpInt(8));
        contentLayout.setOrientation(LinearLayout.HORIZONTAL);
        contentLayout.setGravity(Gravity.CENTER);
        contentLayout.setPadding(dpInt(16), 0, dpInt(16), 0);
        // Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally) from the KT Row.
        addView(contentLayout, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        interactiveHighlight = new InteractiveHighlight(this);
        interactiveHighlight.setFrameListener(new InteractiveHighlight.FrameListener() {
            public void onInteractiveHighlightFrame(InteractiveHighlight highlight) {
                updateInteractiveTransform();
            }
        });
        setOnDrawSurface(new DrawCallback() {
            private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            public void draw(Canvas canvas, LiquidGlassView view, Path path) {
                if (hasTint) {
                    paint.reset(); paint.setAntiAlias(true); paint.setColor(tint);
                    if (Build.VERSION.SDK_INT >= 29) Api29.hue(paint);
                    canvas.drawRect(0f, 0f, getWidth(), getHeight(), paint);
                    paint.reset(); paint.setAntiAlias(true);
                    paint.setColor(ColorUtils.withAlpha(tint, 0.75f));
                    canvas.drawRect(0f, 0f, getWidth(), getHeight(), paint);
                }
                if (hasSurfaceColor) {
                    paint.reset(); paint.setAntiAlias(true); paint.setColor(surfaceColor);
                    canvas.drawRect(0f, 0f, getWidth(), getHeight(), paint);
                }
            }
        });
        /*
         * InteractiveHighlight.modifier is after drawBackdrop() and before Row content.
         * onDrawContentBackground is the matching Java draw slot: glass/surface first,
         * then the interactive additive light, then the button children.
         */
        setOnDrawContentBackground(new DrawCallback() {
            public void draw(Canvas canvas, LiquidGlassView view, Path path) {
                if (interactive) interactiveHighlight.draw(canvas, getWidth(), getHeight());
            }
        });
        setClickable(true);
    }

    /** Alias kept for older port code; setBackdrop() is inherited and public too. */
    public void setBackdropSource(Backdrop backdrop) { setBackdrop(backdrop); }
    public LinearLayout getContentLayout() { return contentLayout; }
    public InteractiveHighlight getInteractiveHighlight() { return interactiveHighlight; }

    /** Lua-friendly equivalent of the Kotlin onClick lambda. */
    public void setOnClickAction(final Runnable action) {
        setOnClickListener(action == null ? null : new OnClickListener() {
            public void onClick(View v) { action.run(); }
        });
    }

    /** Add arbitrary button content, equivalent to RowScope content in the KT API. */
    public void addContentView(View view) {
        if (view != null) {
            contentLayout.addView(view);
            if (adaptiveContentColorEnabled) applyContentColorRecursive(view, contentColor);
        }
    }

    public void clearContent() {
        contentLayout.removeAllViews();
        generatedImageView = null;
        generatedTextView = null;
        imagePath = null;
        lastImageLoadError = null;
    }

    /** Generated label convenience. Unlike the older port, this preserves setImage(). */
    public void setText(CharSequence text) {
        TextView tv = ensureGeneratedTextView();
        tv.setText(text == null ? "" : text);
        tv.setTextColor(contentColor);
    }

    /** Lua-friendly file/content/asset image API. The image is adaptively tinted with the label. */
    public boolean setImage(String source) {
        if (source == null || source.trim().length() == 0) {
            lastImageLoadError = "empty image path";
            return false;
        }
        Bitmap bitmap = ImageSourceLoader.load(getContext(), source);
        if (bitmap == null) {
            lastImageLoadError = "could not decode: " + source;
            return false;
        }
        setImageBitmap(bitmap);
        imagePath = source;
        lastImageLoadError = null;
        return true;
    }

    public boolean setImageUri(String uri) { return setImage(uri); }

    public boolean setImageAsset(String assetPath) {
        if (assetPath == null || assetPath.length() == 0) {
            lastImageLoadError = "empty asset path";
            return false;
        }
        Bitmap bitmap = ImageSourceLoader.loadAsset(getContext(), assetPath);
        if (bitmap == null) {
            lastImageLoadError = "could not decode asset: " + assetPath;
            return false;
        }
        setImageBitmap(bitmap);
        imagePath = "asset://" + assetPath;
        lastImageLoadError = null;
        return true;
    }

    public void setImageBitmap(Bitmap bitmap) {
        ImageView image = ensureGeneratedImageView();
        image.setImageBitmap(bitmap);
        image.setVisibility(VISIBLE);
        image.setAlpha(1f);
        image.setColorFilter(contentColor, PorterDuff.Mode.SRC_IN);
        imagePath = null;
        lastImageLoadError = bitmap == null ? "bitmap == null" : null;
        requestLayout();
        invalidate();
    }

    public void setImageDrawable(Drawable drawable) {
        ImageView image = ensureGeneratedImageView();
        image.setImageDrawable(drawable);
        image.setVisibility(VISIBLE);
        image.setAlpha(1f);
        image.setColorFilter(contentColor, PorterDuff.Mode.SRC_IN);
        imagePath = null;
        lastImageLoadError = drawable == null ? "drawable == null" : null;
        requestLayout();
        invalidate();
    }

    public void setImageResource(int drawableResId) {
        ImageView image = ensureGeneratedImageView();
        image.setImageResource(drawableResId);
        image.setColorFilter(contentColor, PorterDuff.Mode.SRC_IN);
        imagePath = drawableResId == 0 ? null : "res://" + drawableResId;
        lastImageLoadError = drawableResId == 0 ? "resource id == 0" : null;
        requestLayout();
        invalidate();
    }

    /** Size is in dp, matching Lua layout conventions. */
    public void setImageSize(float sizeDp) {
        imageSizeDp = Math.max(0f, sizeDp);
        if (generatedImageView != null) {
            int size = dpInt(imageSizeDp);
            generatedImageView.setLayoutParams(new LinearLayout.LayoutParams(size, size));
        }
        requestLayout();
    }

    public void setImageSize(int sizeDp) { setImageSize((float) sizeDp); }
    public void setImageSizeDp(float sizeDp) { setImageSize(sizeDp); }
    public float getImageSize() { return imageSizeDp; }
    public float getImageSizeDp() { return imageSizeDp; }
    public String getImagePath() { return imagePath; }
    public String getLastImageLoadError() { return lastImageLoadError; }
    public ImageView getImageView() { return generatedImageView; }
    public boolean hasImage() { return generatedImageView != null && generatedImageView.getDrawable() != null; }

    public void clearImage() {
        if (generatedImageView != null) {
            contentLayout.removeView(generatedImageView);
            generatedImageView = null;
        }
        imagePath = null;
        lastImageLoadError = null;
        requestLayout();
        invalidate();
    }

    private ImageView ensureGeneratedImageView() {
        if (generatedImageView == null) {
            generatedImageView = new ImageView(getContext());
            generatedImageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            generatedImageView.setColorFilter(contentColor, PorterDuff.Mode.SRC_IN);
            int size = dpInt(imageSizeDp);
            contentLayout.addView(generatedImageView, 0, new LinearLayout.LayoutParams(size, size));
        }
        return generatedImageView;
    }

    private TextView ensureGeneratedTextView() {
        if (generatedTextView == null) {
            generatedTextView = new TextView(getContext());
            generatedTextView.setTextSize(16f);
            generatedTextView.setTextColor(contentColor);
            generatedTextView.setGravity(Gravity.CENTER);
            contentLayout.addView(generatedTextView, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
        return generatedTextView;
    }

    public void setInteractive(boolean value) {
        interactive = value;
        interactiveHighlight.setEnabled(value);
        if (!value) {
            setTranslationX(0f);
            setTranslationY(0f);
            setScaleX(1f);
            setScaleY(1f);
        }
        invalidate();
    }
    public boolean isInteractive() { return interactive; }
    public void setTint(int color) { tint = color; hasTint = true; invalidate(); }
    public void clearTint() { hasTint = false; invalidate(); }
    /** Kotlin LiquidButton(surfaceColor = ...). */
    @Override
    public void setSurfaceColor(int color) { setButtonSurfaceColor(color); }

    public void setButtonSurfaceColor(int color) { surfaceColor = color; hasSurfaceColor = true; invalidate(); }
    public int getButtonSurfaceColor() { return surfaceColor; }
    public boolean hasButtonSurfaceColor() { return hasSurfaceColor; }
    public void clearButtonSurfaceColor() { hasSurfaceColor = false; invalidate(); }
    public void clearSurfaceColor() { clearButtonSurfaceColor(); }

    public int getTint() { return tint; }
    public boolean hasTint() { return hasTint; }

    /** 1.0 is the exact KT InteractiveHighlight intensity. */
    public void setPressHighlightIntensity(float scale) { interactiveHighlight.setIntensityScale(scale); }
    public float getPressHighlightIntensity() { return interactiveHighlight.getIntensityScale(); }

    /** Moving radial spot only. 0.15f is the original KT value. */
    public void setPressHighlightSpotAlpha(float alpha) { interactiveHighlight.setSpotAlpha(alpha); }
    public float getPressHighlightSpotAlpha() { return interactiveHighlight.getSpotAlpha(); }

    /** Whole-control additive lift only. 0.08f is the original KT value. */
    public void setPressHighlightBaseAlpha(float alpha) { interactiveHighlight.setBaseAlpha(alpha); }
    public float getPressHighlightBaseAlpha() { return interactiveHighlight.getBaseAlpha(); }

    public void setReleaseFlashProtection(boolean enabled) {
        interactiveHighlight.setReleaseFlashProtection(enabled);
    }
    public boolean isReleaseFlashProtectionEnabled() {
        return interactiveHighlight.isReleaseFlashProtectionEnabled();
    }

    /** Compatibility-only: exact KT highlighter uses spring(0.5, 300, 0.001), not durations. */
    public void setPressHighlightFadeDurationsMs(long fadeInMs, long fadeOutMs) {
        interactiveHighlight.setFadeDurationsMs(fadeInMs, fadeOutMs);
    }
    public void setPressHighlightFadeInMs(long durationMs) { interactiveHighlight.setFadeInDurationMs(durationMs); }
    public long getPressHighlightFadeInMs() { return interactiveHighlight.getFadeInDurationMs(); }
    public void setPressHighlightFadeOutMs(long durationMs) { interactiveHighlight.setFadeOutDurationMs(durationMs); }
    public long getPressHighlightFadeOutMs() { return interactiveHighlight.getFadeOutDurationMs(); }

    public TextView getTextView() {
        if (generatedTextView != null) return generatedTextView;
        int i;
        for (i = 0; i < contentLayout.getChildCount(); i++) {
            View child = contentLayout.getChildAt(i);
            if (child instanceof TextView) return (TextView) child;
        }
        return null;
    }

    public CharSequence getText() {
        TextView tv = getTextView();
        return tv == null ? "" : tv.getText();
    }

    public void setTextColor(int color) { setContentColor(color); }

    /** Real-backdrop black/white foreground detection; enabled by default. */
    public void setAdaptiveContentColorEnabled(boolean enabled) {
        adaptiveContentColorEnabled = enabled;
        removeCallbacks(adaptiveColorRunnable);
        adaptiveColorPosted = false;
        if (enabled) {
            lastAdaptiveSampleTime = 0L;
            scheduleAdaptiveColor(0L);
        }
    }

    public boolean isAdaptiveContentColorEnabled() { return adaptiveContentColorEnabled; }

    /** Manual foreground color; disables adaptive detection. */
    public void setContentColor(int color) {
        setAdaptiveContentColorEnabled(false);
        contentColor = color;
        contentColorStart = color;
        contentColorTarget = color;
        adaptiveColorAnimating = false;
        applyContentColorToButton();
    }

    public int getContentColor() { return contentColor; }
    public float getLastBackdropLuminance() { return lastBackdropLuminance; }

    public void setLuminanceThreshold(float threshold) {
        luminanceThreshold = clamp01(threshold);
        lastAdaptiveSampleTime = 0L;
        if (adaptiveContentColorEnabled) scheduleAdaptiveColor(0L);
    }
    public float getLuminanceThreshold() { return luminanceThreshold; }

    public void setAdaptiveContentColorAnimationDurationMs(long durationMs) {
        contentColorAnimationDurationMs = Math.max(0L, durationMs);
    }
    public long getAdaptiveContentColorAnimationDurationMs() { return contentColorAnimationDurationMs; }

    public void setAdaptiveContentColorSampleIntervalMs(long intervalMs) {
        adaptiveSampleIntervalMs = Math.max(16L, intervalMs);
    }
    public long getAdaptiveContentColorSampleIntervalMs() { return adaptiveSampleIntervalMs; }

    public void refreshAdaptiveContentColor() {
        lastAdaptiveSampleTime = 0L;
        if (adaptiveContentColorEnabled) scheduleAdaptiveColor(0L);
    }

    public void setTextSizeSp(float sizeSp) {
        TextView tv = getTextView();
        if (tv == null) {
            setText("");
            tv = getTextView();
        }
        if (tv != null) tv.setTextSize(sizeSp);
    }

    /**
     * Lua/table configuration. This mirrors the KT function parameters and
     * exposes a few View-friendly content conveniences.
     *
     * Keys: text, interactive/isInteractive, tint, surfaceColor, backdrop,
     * pressHighlightIntensity, releaseFlashProtection, enabled, alpha,
     * textColor, textSize/textSizeSp.
     */
    public LiquidButton applyConfig(Object table) {
        if (table == null) return this;

        // Also accepts all inherited drawBackdrop parameters: shape/effects/highlight/shadow/innerShadow.
        super.applyConfig(table);

        if (table instanceof CharSequence) {
            setText((CharSequence) table);
            return this;
        }

        String text = LuaTableBridge.getString(table, new String[] {"text", "title", "label"}, null);
        if (text != null) setText(text);

        String image = LuaTableBridge.getString(table,
                new String[] {"image", "png", "icon", "imagePath", "iconPath", "path"}, null);
        if (image != null) setImage(image);
        String imageAsset = LuaTableBridge.getString(table,
                new String[] {"imageAsset", "iconAsset", "asset"}, null);
        if (imageAsset != null) setImageAsset(imageAsset);
        Object imageBitmap = LuaTableBridge.getAny(table, new String[] {"imageBitmap", "bitmap"});
        if (imageBitmap instanceof Bitmap) setImageBitmap((Bitmap) imageBitmap);
        Object imageDrawable = LuaTableBridge.getAny(table, new String[] {"imageDrawable", "drawable"});
        if (imageDrawable instanceof Drawable) setImageDrawable((Drawable) imageDrawable);
        Object imageRes = LuaTableBridge.getAny(table, new String[] {"imageRes", "imageResource", "iconRes"});
        if (imageRes != null) setImageResource(LuaTableBridge.getInt(
                table, new String[] {"imageRes", "imageResource", "iconRes"}, 0));
        if (LuaTableBridge.getAny(table,
                new String[] {"imageSize", "imageSizeDp", "iconSize", "iconSizeDp"}) != null) {
            setImageSize(LuaTableBridge.getFloat(table,
                    new String[] {"imageSize", "imageSizeDp", "iconSize", "iconSizeDp"}, imageSizeDp));
        }

        Object action = LuaTableBridge.getAny(table, new String[] {"onClick", "click", "action"});
        if (action instanceof Runnable) setOnClickAction((Runnable) action);

        Object backdropValue = LuaTableBridge.getAny(table, new String[] {"backdrop"});
        if (backdropValue instanceof Backdrop) setBackdrop((Backdrop) backdropValue);

        if (LuaTableBridge.getAny(table, new String[] {"interactive", "isInteractive"}) != null) {
            setInteractive(LuaTableBridge.getBoolean(
                    table, new String[] {"interactive", "isInteractive"}, isInteractive()));
        }

        if (LuaTableBridge.getAny(table, new String[] {"tint"}) != null) {
            setTint(LuaTableBridge.getInt(table, new String[] {"tint"}, getTint()));
        }

        if (LuaTableBridge.getAny(table, new String[] {"surfaceColor", "surface"}) != null) {
            setButtonSurfaceColor(LuaTableBridge.getInt(
                    table, new String[] {"surfaceColor", "surface"}, getButtonSurfaceColor()));
        }

        if (LuaTableBridge.getAny(table, new String[] {"pressHighlightIntensity", "highlightIntensity"}) != null) {
            setPressHighlightIntensity(LuaTableBridge.getFloat(
                    table, new String[] {"pressHighlightIntensity", "highlightIntensity"},
                    getPressHighlightIntensity()));
        }

        if (LuaTableBridge.getAny(table, new String[] {"pressHighlightSpotAlpha", "highlightSpotAlpha"}) != null) {
            setPressHighlightSpotAlpha(LuaTableBridge.getFloat(
                    table, new String[] {"pressHighlightSpotAlpha", "highlightSpotAlpha"},
                    getPressHighlightSpotAlpha()));
        }
        if (LuaTableBridge.getAny(table, new String[] {"pressHighlightBaseAlpha", "highlightBaseAlpha"}) != null) {
            setPressHighlightBaseAlpha(LuaTableBridge.getFloat(
                    table, new String[] {"pressHighlightBaseAlpha", "highlightBaseAlpha"},
                    getPressHighlightBaseAlpha()));
        }

        if (LuaTableBridge.getAny(table, new String[] {"releaseFlashProtection", "flashProtection"}) != null) {
            setReleaseFlashProtection(LuaTableBridge.getBoolean(
                    table, new String[] {"releaseFlashProtection", "flashProtection"}, true));
        }

        if (LuaTableBridge.getAny(table, new String[] {"highlightFadeInMs", "pressHighlightFadeInMs"}) != null) {
            setPressHighlightFadeInMs(LuaTableBridge.getInt(
                    table, new String[] {"highlightFadeInMs", "pressHighlightFadeInMs"},
                    (int) getPressHighlightFadeInMs()));
        }
        if (LuaTableBridge.getAny(table, new String[] {"highlightFadeOutMs", "pressHighlightFadeOutMs"}) != null) {
            setPressHighlightFadeOutMs(LuaTableBridge.getInt(
                    table, new String[] {"highlightFadeOutMs", "pressHighlightFadeOutMs"},
                    (int) getPressHighlightFadeOutMs()));
        }

        Object adaptiveColor = LuaTableBridge.getAny(table,
                new String[] {"adaptiveContentColor", "adaptiveForeground", "autoContentColor"});
        if (adaptiveColor != null) {
            setAdaptiveContentColorEnabled(LuaTableBridge.getBoolean(table,
                    new String[] {"adaptiveContentColor", "adaptiveForeground", "autoContentColor"},
                    isAdaptiveContentColorEnabled()));
        }
        if (LuaTableBridge.getAny(table, new String[] {"luminanceThreshold"}) != null) {
            setLuminanceThreshold(LuaTableBridge.getFloat(table,
                    new String[] {"luminanceThreshold"}, getLuminanceThreshold()));
        }
        if (LuaTableBridge.getAny(table,
                new String[] {"contentColorAnimationMs", "adaptiveColorAnimationMs"}) != null) {
            setAdaptiveContentColorAnimationDurationMs(LuaTableBridge.getInt(table,
                    new String[] {"contentColorAnimationMs", "adaptiveColorAnimationMs"},
                    (int) getAdaptiveContentColorAnimationDurationMs()));
        }

        if (LuaTableBridge.getAny(table,
                new String[] {"textColor", "contentColor", "foregroundColor"}) != null) {
            setContentColor(LuaTableBridge.getInt(table,
                    new String[] {"textColor", "contentColor", "foregroundColor"}, getContentColor()));
        }

        if (LuaTableBridge.getAny(table, new String[] {"textSize", "textSizeSp"}) != null) {
            setTextSizeSp(LuaTableBridge.getFloat(
                    table, new String[] {"textSize", "textSizeSp"}, 16f));
        }

        if (LuaTableBridge.getAny(table, new String[] {"enabled"}) != null) {
            setEnabled(LuaTableBridge.getBoolean(table, new String[] {"enabled"}, isEnabled()));
        }

        if (LuaTableBridge.getAny(table, new String[] {"alpha"}) != null) {
            setAlpha(LuaTableBridge.getFloat(table, new String[] {"alpha"}, getAlpha()));
        }

        return this;
    }

    private void normalizeExternalChildrenIntoContent() {
        /*
         * Lua loadlayout { LiquidButton; { ImageView; ... } } adds that ImageView
         * directly to this FrameLayout. KT LiquidButton's content is a RowScope, so
         * move external direct children into the Row once attached. This also makes
         * adaptive foreground tinting apply to those ImageViews.
         */
        for (int i = getChildCount() - 1; i >= 0; i--) {
            View child = getChildAt(i);
            if (child == contentLayout) continue;
            ViewGroup.LayoutParams old = child.getLayoutParams();
            int width = old == null ? ViewGroup.LayoutParams.WRAP_CONTENT : old.width;
            int height = old == null ? ViewGroup.LayoutParams.WRAP_CONTENT : old.height;
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(width, height);
            if (old instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams margins = (ViewGroup.MarginLayoutParams) old;
                lp.setMargins(margins.leftMargin, margins.topMargin, margins.rightMargin, margins.bottomMargin);
            }
            removeViewAt(i);
            contentLayout.addView(child, lp);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        normalizeExternalChildrenIntoContent();
        applyContentColorToButton();
        if (adaptiveContentColorEnabled) {
            lastAdaptiveSampleTime = 0L;
            scheduleAdaptiveColor(0L);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        removeCallbacks(adaptiveColorRunnable);
        adaptiveColorPosted = false;
        super.onDetachedFromWindow();
    }

    private void applyContentColorToButton() {
        applyContentColorRecursive(contentLayout, contentColor);
        contentLayout.invalidate();
        invalidate();
    }

    private static void applyContentColorRecursive(View view, int color) {
        if (view instanceof TextView) {
            ((TextView) view).setTextColor(color);
        }
        if (view instanceof ImageView) {
            ((ImageView) view).setColorFilter(color, PorterDuff.Mode.SRC_IN);
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            int count = group.getChildCount();
            for (int i = 0; i < count; i++) {
                applyContentColorRecursive(group.getChildAt(i), color);
            }
        }
    }

    private void scheduleAdaptiveColor(long delayMs) {
        if (!adaptiveContentColorEnabled || !isAttachedToWindow() || adaptiveColorPosted) return;
        adaptiveColorPosted = true;
        if (delayMs <= 0L) post(adaptiveColorRunnable);
        else postDelayed(adaptiveColorRunnable, delayMs);
    }

    private static float clamp01(float value) {
        return value < 0f ? 0f : (value > 1f ? 1f : value);
    }

    /** Compose FastOutSlowInEasing = cubic-bezier(0.4,0,0.2,1). */
    private static float fastOutSlowIn(float x) {
        x = clamp01(x);
        if (x == 0f || x == 1f) return x;
        float t = x;
        for (int i = 0; i < 8; i++) {
            float omt = 1f - t;
            float curveX = 3f * 0.4f * omt * omt * t
                    + 3f * 0.2f * omt * t * t + t * t * t;
            float derivative = 3f * 0.4f * (omt * omt - 2f * omt * t)
                    + 3f * 0.2f * (2f * omt * t - t * t) + 3f * t * t;
            float error = curveX - x;
            if (Math.abs(error) < 0.00001f || Math.abs(derivative) < 0.00001f) break;
            t = clamp01(t - error / derivative);
        }
        float omt = 1f - t;
        return 3f * omt * t * t + t * t * t;
    }

    private static int lerpColor(int from, int to, float t) {
        t = clamp01(t);
        int a = Math.round(Color.alpha(from) + (Color.alpha(to) - Color.alpha(from)) * t);
        int r = Math.round(Color.red(from) + (Color.red(to) - Color.red(from)) * t);
        int g = Math.round(Color.green(from) + (Color.green(to) - Color.green(from)) * t);
        int b = Math.round(Color.blue(from) + (Color.blue(to) - Color.blue(from)) * t);
        return Color.argb(a, r, g, b);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int desiredH = dpInt(48);
        int h = resolveSize(desiredH, heightMeasureSpec);
        int w = resolveSize(dpInt(120), widthMeasureSpec);
        setMeasuredDimension(w, h);
        contentLayout.measure(MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(h, MeasureSpec.EXACTLY));
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        contentLayout.layout(0, 0, getWidth(), getHeight());
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) return false;
        if (!interactive) return super.onTouchEvent(event);

        int action = event.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                int index = event.getActionIndex();
                activePointerId = event.getPointerId(index);
                downX = event.getX(index);
                downY = event.getY(index);
                interactiveHighlight.press(downX, downY);
                return true;
            }
            case MotionEvent.ACTION_POINTER_DOWN:
                /* inspectDragGestures keeps tracking the current pointer. */
                return true;

            case MotionEvent.ACTION_MOVE: {
                int index = event.findPointerIndex(activePointerId);
                if (index >= 0) {
                    interactiveHighlight.move(event.getX(index), event.getY(index));
                }
                return true;
            }

            case MotionEvent.ACTION_POINTER_UP: {
                int upIndex = event.getActionIndex();
                if (event.getPointerId(upIndex) == activePointerId) {
                    /*
                     * Exact DragGestureInspector.kt behavior: when the tracked
                     * pointer goes up while another pointer remains pressed,
                     * continue with that other pointer instead of ending. The
                     * highlight position is not snapped until that pointer moves.
                     */
                    int other = findOtherPointerIndex(event, upIndex);
                    if (other >= 0) {
                        activePointerId = event.getPointerId(other);
                    } else {
                        activePointerId = INVALID_POINTER_ID;
                        interactiveHighlight.release();
                    }
                }
                return true;
            }

            case MotionEvent.ACTION_UP:
                if (activePointerId != INVALID_POINTER_ID) {
                    activePointerId = INVALID_POINTER_ID;
                    interactiveHighlight.release();
                    performClick();
                }
                return true;

            case MotionEvent.ACTION_CANCEL:
                if (activePointerId != INVALID_POINTER_ID) {
                    activePointerId = INVALID_POINTER_ID;
                    interactiveHighlight.releaseFromCancel();
                }
                return true;
        }
        return true;
    }

    private static int findOtherPointerIndex(MotionEvent event, int excludedIndex) {
        int i;
        for (i = 0; i < event.getPointerCount(); i++) {
            if (i != excludedIndex) return i;
        }
        return -1;
    }

    @Override public boolean performClick() { super.performClick(); return true; }

    private void updateInteractiveTransform() {
        float width = Math.max(1f, getWidth());
        float height = Math.max(1f, getHeight());
        /* KT layerBlock reads InteractiveHighlight.pressProgress directly. */
        float p = interactiveHighlight.getPressProgress();
        float scale = lerp(1f, 1f + dpF(4f) / height, p);
        float maxOffset = Math.min(width, height);
        float ox = interactiveHighlight.getOffsetX();
        float oy = interactiveHighlight.getOffsetY();
        setTranslationX(maxOffset * tanh(0.05f * ox / maxOffset));
        setTranslationY(maxOffset * tanh(0.05f * oy / maxOffset));
        float maxDragScale = dpF(4f) / height;
        float angle = (float) Math.atan2(oy, ox);
        float sx = scale + maxDragScale * Math.abs((float) Math.cos(angle) * ox / Math.max(width, height)) * Math.min(width / height, 1f);
        float sy = scale + maxDragScale * Math.abs((float) Math.sin(angle) * oy / Math.max(width, height)) * Math.min(height / width, 1f);
        setScaleX(sx);
        setScaleY(sy);
        invalidate();
    }

    private static final class SpacedRow extends LinearLayout {
        private final int gap;
        SpacedRow(Context context, int gap) {
            super(context);
            this.gap = Math.max(0, gap);
        }

        @Override
        protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
            int count = getChildCount();
            int visible = 0;
            int total = 0;
            int i;
            for (i = 0; i < count; i++) {
                View child = getChildAt(i);
                if (child.getVisibility() == GONE) continue;
                LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) child.getLayoutParams();
                total += child.getMeasuredWidth() + lp.leftMargin + lp.rightMargin;
                visible++;
            }
            if (visible > 1) total += gap * (visible - 1);
            int availableLeft = getPaddingLeft();
            int availableRight = getWidth() - getPaddingRight();
            /*
             * KT uses Arrangement.spacedBy(..., Alignment.CenterHorizontally).
             * Alignment.CenterHorizontally still centers content when a caller forces
             * a button narrower than its 16dp + 16dp horizontal padding.  The old
             * Java port clamped the centering offset to >= 0, which pushed image-only
             * content to the right in small square buttons (for example 45dp).
             * Allow the negative compensation so the visual content remains centered
             * on the actual button bounds.
             */
            int x = availableLeft + (availableRight - availableLeft - total) / 2;
            int centerY = (getHeight() + getPaddingTop() - getPaddingBottom()) / 2;
            for (i = 0; i < count; i++) {
                View child = getChildAt(i);
                if (child.getVisibility() == GONE) continue;
                LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) child.getLayoutParams();
                x += lp.leftMargin;
                int cw = child.getMeasuredWidth();
                int ch = child.getMeasuredHeight();
                int y = centerY - ch / 2 + lp.topMargin - lp.bottomMargin;
                child.layout(x, y, x + cw, y + ch);
                x += cw + lp.rightMargin + gap;
            }
        }
    }

    private static final class Api29 {
        private Api29() {}
        static void hue(Paint paint) {
            paint.setBlendMode(android.graphics.BlendMode.HUE);
        }
    }

    private static float tanh(float x) {
        double e2 = Math.exp(2.0 * x);
        return (float) ((e2 - 1.0) / (e2 + 1.0));
    }
    private static float lerp(float a, float b, float t) { return a + (b - a) * t; }
    private int dpInt(float value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private float dpF(float value) { return value * getResources().getDisplayMetrics().density; }
}
