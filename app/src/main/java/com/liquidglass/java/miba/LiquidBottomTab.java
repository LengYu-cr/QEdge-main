package com.liquidglass.java.miba;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Java/View port of catalog/components/LiquidBottomTab.kt.
 *
 * Original KT semantics:
 * - Column
 * - clip(Capsule())
 * - fillMaxHeight().weight(1f)
 * - vertical Arrangement.spacedBy(2.dp, CenterVertically)
 * - horizontal Alignment.CenterHorizontally
 * - the parent supplies the animated LocalLiquidBottomTabScale.
 *
 * Java/Lua keeps normal addView(...) content support and adds icon/text helpers.
 */
public class LiquidBottomTab extends LinearLayout {
    private final Path capsulePath = new Path();
    private ImageView iconView;
    private TextView labelView;
    private float iconSizeDp = 28f; // BottomTabsContent.kt uses 28.dp.
    private float textSizeSp = 12f; // BottomTabsContent.kt uses 12.sp.
    private Integer generatedContentColor;
    private String iconPath;
    private String lastIconLoadError;

    public LiquidBottomTab(Context context) { this(context, null); }

    public LiquidBottomTab(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOrientation(VERTICAL);
        setGravity(Gravity.CENTER);
        setClickable(true);
        setClipChildren(false);
        setClipToPadding(false);
        setWillNotDraw(false);
    }

    /** LocalLiquidBottomTabScale equivalent. */
    public void setLiquidScale(float scale) {
        setScaleX(scale);
        setScaleY(scale);
    }

    public float getLiquidScale() { return getScaleX(); }

    public void setOnClickAction(final Runnable action) {
        setOnClickListener(action == null ? null : new OnClickListener() {
            public void onClick(View v) { action.run(); }
        });
    }

    /** Equivalent of arbitrary ColumnScope content. */
    public void addContentView(View view) {
        if (view != null) addView(view);
    }

    public void clearContent() {
        removeAllViews();
        iconView = null;
        labelView = null;
        iconPath = null;
        lastIconLoadError = null;
    }

    public void setText(CharSequence text) {
        if (text == null) {
            if (labelView != null) labelView.setText("");
            return;
        }
        ensureLabelView().setText(text);
        requestLayout();
        invalidate();
    }

    public CharSequence getText() { return labelView == null ? "" : labelView.getText(); }
    public TextView getLabelView() { return labelView; }

    public void setTextColor(int color) {
        ensureLabelView().setTextColor(color);
        generatedContentColor = Integer.valueOf(color);
        invalidate();
    }

    public int getTextColor() {
        return labelView == null ? Color.BLACK : labelView.getCurrentTextColor();
    }

    /** Demo-equivalent icon ColorFilter.tint + TextStyle color. */
    public void setGeneratedContentColor(int color) {
        generatedContentColor = Integer.valueOf(color);
        if (labelView != null) labelView.setTextColor(color);
        if (iconView != null) iconView.setColorFilter(color, PorterDuff.Mode.SRC_IN);
        invalidate();
    }

    public int getGeneratedContentColor() {
        return generatedContentColor == null ? getTextColor() : generatedContentColor.intValue();
    }

    public boolean hasGeneratedContentColor() { return generatedContentColor != null; }

    public void clearGeneratedContentColor() {
        generatedContentColor = null;
        if (iconView != null) iconView.clearColorFilter();
        invalidate();
    }

    public void setTextSizeSp(float sizeSp) {
        textSizeSp = Math.max(0f, sizeSp);
        if (labelView != null) labelView.setTextSize(textSizeSp);
        requestLayout();
    }

    public float getTextSizeSp() { return textSizeSp; }

    public ImageView getIconView() { return iconView; }

    public void setIconSizeDp(float sizeDp) {
        iconSizeDp = Math.max(0f, sizeDp);
        if (iconView != null) {
            int size = dp(iconSizeDp);
            iconView.setLayoutParams(new LinearLayout.LayoutParams(size, size));
        }
        requestLayout();
    }

    public float getIconSizeDp() { return iconSizeDp; }
    public String getIconPath() { return iconPath; }

    public void setIconBitmap(Bitmap bitmap) {
        ImageView v = ensureIconView();
        v.setImageBitmap(bitmap);
        v.setVisibility(VISIBLE);
        if (generatedContentColor != null) v.setColorFilter(generatedContentColor.intValue(), PorterDuff.Mode.SRC_IN);
        else v.clearColorFilter();
        iconPath = null;
        lastIconLoadError = bitmap == null ? "bitmap == null" : null;
        requestLayout();
        invalidate();
    }

    public void setIconDrawable(Drawable drawable) {
        ImageView v = ensureIconView();
        v.setImageDrawable(drawable);
        v.setVisibility(VISIBLE);
        if (generatedContentColor != null) v.setColorFilter(generatedContentColor.intValue(), PorterDuff.Mode.SRC_IN);
        else v.clearColorFilter();
        iconPath = null;
        lastIconLoadError = drawable == null ? "drawable == null" : null;
        requestLayout();
        invalidate();
    }

    public void setIconResource(int drawableResId) {
        ImageView v = ensureIconView();
        v.setImageResource(drawableResId);
        if (generatedContentColor != null) v.setColorFilter(generatedContentColor.intValue(), PorterDuff.Mode.SRC_IN);
        else v.clearColorFilter();
        iconPath = drawableResId == 0 ? null : "res://" + drawableResId;
        lastIconLoadError = drawableResId == 0 ? "resource id == 0" : null;
        requestLayout();
        invalidate();
    }

    public boolean setIconPng(String filePath) {
        if (filePath == null || filePath.trim().length() == 0) {
            lastIconLoadError = "empty image path";
            return false;
        }
        Bitmap bitmap = ImageSourceLoader.load(getContext(), filePath);
        if (bitmap == null) {
            lastIconLoadError = "could not decode: " + filePath;
            return false;
        }
        setIconBitmap(bitmap);
        iconPath = filePath;
        lastIconLoadError = null;
        return true;
    }

    public boolean setIconPngFile(File file) {
        if (file == null) {
            lastIconLoadError = "file == null";
            return false;
        }
        return setIconPng(file.getAbsolutePath());
    }

    public boolean setIconUri(String uriString) {
        if (uriString == null || uriString.length() == 0) {
            lastIconLoadError = "empty uri";
            return false;
        }
        InputStream in = null;
        try {
            in = getContext().getContentResolver().openInputStream(Uri.parse(uriString));
            Bitmap bitmap = in == null ? null : BitmapFactory.decodeStream(in);
            if (bitmap == null) {
                lastIconLoadError = "could not decode uri: " + uriString;
                return false;
            }
            setIconBitmap(bitmap);
            iconPath = uriString;
            lastIconLoadError = null;
            return true;
        } catch (Throwable e) {
            lastIconLoadError = String.valueOf(e);
            return false;
        } finally {
            if (in != null) try { in.close(); } catch (IOException ignored) {}
        }
    }

    public boolean setIconBytes(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            lastIconLoadError = "empty bytes";
            return false;
        }
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        if (bitmap == null) {
            lastIconLoadError = "could not decode bytes";
            return false;
        }
        setIconBitmap(bitmap);
        iconPath = "bytes://" + bytes.length;
        lastIconLoadError = null;
        return true;
    }

    public boolean setIconAsset(String assetPath) {
        if (assetPath == null || assetPath.length() == 0) {
            lastIconLoadError = "empty asset path";
            return false;
        }
        Bitmap bitmap = ImageSourceLoader.loadAsset(getContext(), assetPath);
        if (bitmap == null) {
            lastIconLoadError = "could not decode asset: " + assetPath;
            return false;
        }
        setIconBitmap(bitmap);
        iconPath = "asset://" + assetPath;
        lastIconLoadError = null;
        return true;
    }

    public boolean setIconObject(Object value) {
        if (value == null) {
            clearIcon();
            return true;
        }
        if (value instanceof Bitmap) { setIconBitmap((Bitmap) value); return true; }
        if (value instanceof Drawable) { setIconDrawable((Drawable) value); return true; }
        if (value instanceof byte[]) return setIconBytes((byte[]) value);
        if (value instanceof File) return setIconPngFile((File) value);
        if (value instanceof Number) { setIconResource(((Number) value).intValue()); return true; }
        return setIconPng(String.valueOf(value));
    }

    public boolean hasIcon() { return iconView != null && iconView.getDrawable() != null; }
    public String getLastIconLoadError() { return lastIconLoadError; }

    public void clearIcon() {
        if (iconView != null) iconView.setImageDrawable(null);
        iconPath = null;
        lastIconLoadError = null;
        invalidate();
    }

    /** Removes only helper-created icon and label. */
    public void clearGeneratedContent() {
        if (iconView != null && iconView.getParent() == this) removeView(iconView);
        if (labelView != null && labelView.getParent() == this) removeView(labelView);
        iconView = null;
        labelView = null;
        iconPath = null;
        lastIconLoadError = null;
    }

    /**
     * Legacy compatibility helper from Fix15. Fix22 no longer uses cloned hidden
     * tab Views; LiquidBottomTabs performs the KT second content render pass through
     * CaptureMirrorRow so both passes share the same visual source.
     */
    LiquidBottomTab makeCaptureClone() {
        LiquidBottomTab target = new LiquidBottomTab(getContext());
        target.setIconSizeDp(iconSizeDp);
        target.setTextSizeSp(textSizeSp);
        if (iconView != null && iconView.getDrawable() != null) {
            Drawable source = iconView.getDrawable();
            Drawable copy = source;
            try {
                Drawable.ConstantState state = source.getConstantState();
                if (state != null) copy = state.newDrawable(getResources()).mutate();
            } catch (Throwable ignored) {}
            target.setIconDrawable(copy);
        }
        if (labelView != null) target.setText(labelView.getText());
        target.setEnabled(isEnabled());
        target.setContentDescription(getContentDescription());
        target.setAlpha(1f);
        // The whole hidden Row receives accent ColorFilter, so leave clone untinted.
        target.clearGeneratedContentColor();
        return target;
    }

    /** Backward-compatible helper retained for older code. */
    void copyGeneratedContentTo(LiquidBottomTab target) {
        if (target == null) return;
        LiquidBottomTab clone = makeCaptureClone();
        target.clearGeneratedContent();
        target.setIconSizeDp(clone.getIconSizeDp());
        target.setTextSizeSp(clone.getTextSizeSp());
        if (clone.getIconView() != null && clone.getIconView().getDrawable() != null) {
            target.setIconDrawable(clone.getIconView().getDrawable());
        }
        if (clone.getLabelView() != null) target.setText(clone.getText());
        target.clearGeneratedContentColor();
    }

    public LiquidBottomTab applyConfig(Object table) {
        if (table == null) return this;
        if (table instanceof CharSequence) {
            String value = String.valueOf(table);
            if (looksLikeImagePath(value)) setIconPng(value); else setText(value);
            return this;
        }

        String text = LuaTableBridge.getString(table, new String[] {"text", "title", "label", "name"}, null);
        if (text != null) setText(text);

        Object bitmap = LuaTableBridge.getAny(table, new String[] {"bitmap", "iconBitmap"});
        Object drawable = LuaTableBridge.getAny(table, new String[] {"drawable", "iconDrawable"});
        Object bytes = LuaTableBridge.getAny(table, new String[] {"iconBytes", "bytes"});
        Object res = LuaTableBridge.getAny(table, new String[] {"iconRes", "iconResource", "resId"});
        String asset = LuaTableBridge.getString(table, new String[] {"asset", "assetPath"}, null);
        String uri = LuaTableBridge.getString(table, new String[] {"uri", "contentUri"}, null);
        String png = LuaTableBridge.getString(table, new String[] {"png", "iconPath", "image", "path", "file"}, null);
        Object generic = LuaTableBridge.getAny(table, new String[] {"icon"});

        if (bitmap instanceof Bitmap) setIconBitmap((Bitmap) bitmap);
        else if (drawable instanceof Drawable) setIconDrawable((Drawable) drawable);
        else if (bytes instanceof byte[]) setIconBytes((byte[]) bytes);
        else if (res != null) setIconResource(LuaTableBridge.getInt(table, new String[] {"iconRes", "iconResource", "resId"}, 0));
        else if (asset != null) setIconAsset(asset);
        else if (uri != null) setIconUri(uri);
        else if (png != null) setIconPng(png);
        else if (generic != null) setIconObject(generic);

        if (LuaTableBridge.getAny(table, new String[] {"iconSize", "iconSizeDp", "size"}) != null)
            setIconSizeDp(LuaTableBridge.getFloat(table, new String[] {"iconSize", "iconSizeDp", "size"}, iconSizeDp));
        if (LuaTableBridge.getAny(table, new String[] {"textSize", "textSizeSp", "labelSize"}) != null)
            setTextSizeSp(LuaTableBridge.getFloat(table, new String[] {"textSize", "textSizeSp", "labelSize"}, textSizeSp));
        if (LuaTableBridge.getAny(table, new String[] {"contentColor", "foregroundColor", "iconTextColor", "textColor", "labelColor"}) != null)
            setGeneratedContentColor(LuaTableBridge.getInt(table,
                    new String[] {"contentColor", "foregroundColor", "iconTextColor", "textColor", "labelColor"}, getGeneratedContentColor()));
        if (LuaTableBridge.getAny(table, new String[] {"enabled"}) != null)
            setEnabled(LuaTableBridge.getBoolean(table, new String[] {"enabled"}, isEnabled()));
        if (LuaTableBridge.getAny(table, new String[] {"alpha"}) != null)
            setAlpha(LuaTableBridge.getFloat(table, new String[] {"alpha"}, getAlpha()));
        String description = LuaTableBridge.getString(table, new String[] {"contentDescription", "description"}, null);
        if (description != null) setContentDescription(description);
        return this;
    }

    private ImageView ensureIconView() {
        if (iconView == null) {
            iconView = new ImageView(getContext());
            iconView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            int size = dp(iconSizeDp);
            addView(iconView, 0, new LinearLayout.LayoutParams(size, size));
            if (generatedContentColor != null) iconView.setColorFilter(generatedContentColor.intValue(), PorterDuff.Mode.SRC_IN);
        }
        return iconView;
    }

    private TextView ensureLabelView() {
        if (labelView == null) {
            labelView = new TextView(getContext());
            labelView.setGravity(Gravity.CENTER);
            labelView.setIncludeFontPadding(false);
            labelView.setTextSize(textSizeSp);
            labelView.setTextColor(generatedContentColor == null ? Color.BLACK : generatedContentColor.intValue());
            addView(labelView, new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        }
        return labelView;
    }

    /** Arrangement.spacedBy(2.dp, Alignment.CenterVertically). */
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int count = getChildCount();
        if (count == 0) return;
        int gap = dp(2f);
        int totalHeight = 0;
        int visibleCount = 0;
        int i;
        for (i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == GONE) continue;
            totalHeight += child.getMeasuredHeight();
            visibleCount++;
        }
        if (visibleCount > 1) totalHeight += gap * (visibleCount - 1);
        int y = (getHeight() - totalHeight) / 2;
        for (i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == GONE) continue;
            int cw = child.getMeasuredWidth();
            int ch = child.getMeasuredHeight();
            int x = (getWidth() - cw) / 2;
            child.layout(x, y, x + cw, y + ch);
            y += ch + gap;
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        capsulePath.reset();
        float radius = getHeight() * 0.5f;
        capsulePath.addRoundRect(0f, 0f, getWidth(), getHeight(), radius, radius, Path.Direction.CW);
        int save = canvas.save();
        canvas.clipPath(capsulePath);
        super.dispatchDraw(canvas);
        canvas.restoreToCount(save);
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static boolean looksLikeImagePath(String value) {
        if (value == null) return false;
        String s = value.toLowerCase();
        return s.endsWith(".png") || s.endsWith(".webp") || s.endsWith(".jpg") || s.endsWith(".jpeg")
                || s.startsWith("/") || s.startsWith("file://") || s.startsWith("content://");
    }
}
