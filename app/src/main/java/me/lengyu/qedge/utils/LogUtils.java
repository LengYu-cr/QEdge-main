package me.lengyu.qedge.utils;

import android.util.Log;
import de.robv.android.xposed.XposedBridge;

public class LogUtils {

    private static final String TAG = "[QEdge]";

    public static void d(String message) {
        Log.d(TAG, message);
        XposedBridge.log(TAG + " DEBUG: " + message);
    }

    public static void d(String tag, String message) {
        Log.d(TAG + ":" + tag, message);
        XposedBridge.log(TAG + ":" + tag + " DEBUG: " + message);
    }

    public static void i(String message) {
        Log.i(TAG, message);
        XposedBridge.log(TAG + " INFO: " + message);
    }

    public static void i(String tag, String message) {
        Log.i(TAG + ":" + tag, message);
        XposedBridge.log(TAG + ":" + tag + " INFO: " + message);
    }

    public static void w(String message) {
        Log.w(TAG, message);
        XposedBridge.log(TAG + " WARN: " + message);
    }

    public static void w(String tag, String message) {
        Log.w(TAG + ":" + tag, message);
        XposedBridge.log(TAG + ":" + tag + " WARN: " + message);
    }

    public static void e(String message) {
        Log.e(TAG, message);
        XposedBridge.log(TAG + " ERROR: " + message);
    }

    public static void e(String tag, String message) {
        Log.e(TAG + ":" + tag, message);
        XposedBridge.log(TAG + ":" + tag + " ERROR: " + message);
    }

    public static void e(Throwable throwable) {
        Log.e(TAG, "", throwable);
        XposedBridge.log(throwable);
    }

    public static void e(String tag, Throwable throwable) {
        Log.e(TAG + ":" + tag, "", throwable);
        XposedBridge.log(TAG + ":" + tag + " ERROR: " + throwable.getMessage());
        XposedBridge.log(throwable);
    }
}