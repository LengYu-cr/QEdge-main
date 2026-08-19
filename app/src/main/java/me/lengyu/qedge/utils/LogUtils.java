package me.lengyu.qedge.utils;

import android.util.Log;

import me.lengyu.qedge.utils.qq.QQCurrentEnv;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class LogUtils {

    private static final String TAG = "[QEdge]";
    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private static final SimpleDateFormat TIME_FMT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());
    private static final Object LOCK = new Object();

    private static File getLogFile() {
        File dir = new File(QQCurrentEnv.getCurrentDir(), "log");
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, DATE_FMT.format(new Date()) + ".log");
    }

    private static void writeToFile(String level, String tag, String message) {
        try {
            String line = TIME_FMT.format(new Date()) + " " + level + " " + tag + ": " + message + "\n";
            synchronized (LOCK) {
                FileWriter writer = new FileWriter(getLogFile(), true);
                writer.write(line);
                writer.close();
            }
        } catch (Throwable ignored) {
        }
    }

    private static void writeThrowable(String tag, Throwable throwable) {
        try {
            StringWriter sw = new StringWriter();
            throwable.printStackTrace(new PrintWriter(sw));
            String line = TIME_FMT.format(new Date()) + " E " + tag + ": " + throwable.getMessage() + "\n" + sw + "\n";
            synchronized (LOCK) {
                FileWriter writer = new FileWriter(getLogFile(), true);
                writer.write(line);
                writer.close();
            }
        } catch (Throwable ignored) {
        }
    }

    public static void d(String message) {
        Log.d(TAG, message);
        writeToFile("D", TAG, message);
    }

    public static void d(String tag, String message) {
        Log.d(TAG + ":" + tag, message);
        writeToFile("D", TAG + ":" + tag, message);
    }

    public static void i(String message) {
        Log.i(TAG, message);
        writeToFile("I", TAG, message);
    }

    public static void i(String tag, String message) {
        Log.i(TAG + ":" + tag, message);
        writeToFile("I", TAG + ":" + tag, message);
    }

    public static void w(String message) {
        Log.w(TAG, message);
        writeToFile("W", TAG, message);
    }

    public static void w(String tag, String message) {
        Log.w(TAG + ":" + tag, message);
        writeToFile("W", TAG + ":" + tag, message);
    }

    public static void e(String message) {
        Log.e(TAG, message);
        writeToFile("E", TAG, message);
    }

    public static void e(String tag, String message) {
        Log.e(TAG + ":" + tag, message);
        writeToFile("E", TAG + ":" + tag, message);
    }

    public static void e(Throwable throwable) {
        Log.e(TAG, "", throwable);
        writeThrowable(TAG, throwable);
    }

    public static void e(String tag, Throwable throwable) {
        Log.e(TAG + ":" + tag, "", throwable);
        writeThrowable(TAG + ":" + tag, throwable);
    }
}
