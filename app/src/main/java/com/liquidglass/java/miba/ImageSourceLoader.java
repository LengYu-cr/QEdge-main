package com.liquidglass.java.miba;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/** Dependency-free raster image loader shared by Lua-facing glass controls. */
final class ImageSourceLoader {
    private ImageSourceLoader() {}

    static Bitmap load(Context context, String source) {
        if (context == null || source == null) return null;
        String value = source.trim();
        if (value.length() == 0) return null;

        InputStream in = null;
        try {
            if (value.startsWith("content://") || value.startsWith("android.resource://")) {
                in = context.getContentResolver().openInputStream(Uri.parse(value));
                return in == null ? null : BitmapFactory.decodeStream(in);
            }

            if (value.startsWith("asset://")) {
                in = context.getAssets().open(value.substring("asset://".length()));
                return BitmapFactory.decodeStream(in);
            }
            if (value.startsWith("assets://")) {
                in = context.getAssets().open(value.substring("assets://".length()));
                return BitmapFactory.decodeStream(in);
            }

            String path = value;
            if (value.startsWith("file://")) {
                String parsed = Uri.parse(value).getPath();
                if (parsed != null) path = parsed;
            }

            Bitmap direct = BitmapFactory.decodeFile(path);
            if (direct != null) return direct;

            File file = new File(path);
            if (file.isFile()) {
                in = new FileInputStream(file);
                Bitmap streamed = BitmapFactory.decodeStream(in);
                if (streamed != null) return streamed;
            }

            /* Lua projects often pass an asset-like relative path. */
            if (!new File(path).isAbsolute()) {
                try {
                    in = context.getAssets().open(path);
                    return BitmapFactory.decodeStream(in);
                } catch (IOException ignored) {
                    return null;
                }
            }
        } catch (Throwable ignored) {
            return null;
        } finally {
            if (in != null) {
                try { in.close(); } catch (IOException ignored) {}
            }
        }
        return null;
    }

    static Bitmap loadAsset(Context context, String assetPath) {
        if (assetPath == null) return null;
        return load(context, "asset://" + assetPath);
    }
}
