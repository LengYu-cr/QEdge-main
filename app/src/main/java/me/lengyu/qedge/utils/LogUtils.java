package me.lengyu.qedge.utils;

import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * 日志工具。
 *
 * <p>性能：调用方（主线程 / Hook 线程）只把日志行放入队列后立即返回，由后台单线程
 * {@code QEdge-Log} 批量写入文件，避免原先"每条日志都 open→write→close + 全局锁"的
 * 同步文件 IO 在启动热路径上造成卡顿（也会干扰耗时测量）。logcat 输出保持同步不变。
 */
public class LogUtils {

    private static final String TAG = "[QEdge]";
    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private static final SimpleDateFormat TIME_FMT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());

    /** 待写入的日志行队列；容量上限防止极端情况下内存膨胀。 */
    private static final BlockingQueue<String> QUEUE = new LinkedBlockingQueue<>(4096);

    static {
        Thread worker = new Thread(LogUtils::drainLoop, "QEdge-Log");
        worker.setDaemon(true);
        worker.start();
    }

    private static void drainLoop() {
        List<String> batch = new ArrayList<>();
        while (true) {
            try {
                // 阻塞等第一条，拿到后把队列里已堆积的一起取出，合并成一次文件写入
                String first = QUEUE.take();
                batch.add(first);
                QUEUE.drainTo(batch);
                flushBatch(batch);
                batch.clear();
                // 轻微聚合窗口：让短时间内的后续日志能并入下一批
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Throwable ignored) {
                batch.clear();
            }
        }
    }

    private static void flushBatch(List<String> batch) {
        if (batch.isEmpty()) return;
        try {
            synchronized (DATE_FMT) {
                File file = getLogFile();
                try (FileWriter writer = new FileWriter(file, true)) {
                    for (String line : batch) {
                        writer.write(line);
                    }
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private static File getLogFile() {
        File dir = new File(HostInfo.getModuleDataPath(), "log");
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, DATE_FMT.format(new Date()) + ".log");
    }

    /** 把日志行入队；队列满时丢弃，绝不阻塞调用线程。 */
    private static void enqueue(String line) {
        QUEUE.offer(line);
    }

    private static void writeToFile(String level, String tag, String message) {
        enqueue(TIME_FMT.format(new Date()) + " " + level + " " + tag + ": " + message + "\n");
    }

    private static void writeThrowable(String tag, Throwable throwable) {
        StringWriter sw = new StringWriter();
        throwable.printStackTrace(new PrintWriter(sw));
        enqueue(TIME_FMT.format(new Date()) + " E " + tag + ": " + throwable.getMessage() + "\n" + sw + "\n");
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
