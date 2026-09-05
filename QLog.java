//
// Decompiled by Jadx - 1043ms
//
package com.tencent.qphone.base.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Environment;
import android.os.Looper;
import android.os.Process;
import android.text.TextUtils;
import android.util.Log;
import com.tencent.mars.xlog.Xlog;
import com.tencent.qphone.base.util.log.builder.QLogConfig;
import com.tencent.qphone.base.util.report.QLogReportManager;
import com.tencent.qphone.base.util.report.firebase.QLogFirebaseReportManager;
import com.tencent.thread.monitor.plugin.proxy.BaseThread;
import com.tencent.thread.monitor.plugin.proxy.ProxyExecutors;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

public class QLog {
    public static final String ANDROID_LOG_LEVEL_D = "D";
    public static final byte ANDROID_LOG_LEVEL_D_BYTE = 1;
    public static final String ANDROID_LOG_LEVEL_E = "E";
    public static final byte ANDROID_LOG_LEVEL_E_BYTE = 4;
    public static final String ANDROID_LOG_LEVEL_I = "I";
    public static final byte ANDROID_LOG_LEVEL_I_BYTE = 2;
    public static final String ANDROID_LOG_LEVEL_W = "W";
    public static final byte ANDROID_LOG_LEVEL_W_BYTE = 3;
    public static final int CLR = 2;
    public static final int DEV = 4;
    private static final String LOG_HOOK_PREFIX_TAG = "log_hook_pre_";
    private static final String LOG_TRUNCATED_NOTE = " 日志超1024个字符，后面不打印了！！！";
    private static final int MAX_LOG_LENGTH = 1024;
    public static final String MSF_IS_COLOR_LEVEL = "QLogConfig_B";
    public static final String TAG_REPORTLEVEL_COLORUSER = "W";
    public static final String TAG_REPORTLEVEL_DEVELOPER = "D";
    public static final String TAG_REPORTLEVEL_USER = "E";
    public static final int USR = 1;
    public static final String logLevelHead = "LOGLEVEL_";
    public static final String logLevelTime = "LOGLEVELTIME";
    private static Context sAppContext = null;
    private static boolean sHasStoragePermission = false;
    private static QLogItemManager sLogManager = null;
    private static OnPrintlnCallback sOnPrintlnCallback = null;
    private static final String tag = "QLog";
    private static final String manualLogLevelPath = Environment.getExternalStorageDirectory() + "/mqqLogLevel";
    private static final Charset logCharset = Charset.forName("UTF-8");
    public static int _DEFAULT_REPORTLOG_LEVEL = 4;
    private static int UIN_REPORTLOG_LEVEL = 4;
    private static boolean isDebug = false;
    private static boolean isGray = false;
    private static boolean isPublish = false;
    private static byte forceCallbackAndroidLogLevel = Byte.MAX_VALUE;
    public static boolean sLogcatHooked = false;
    private static String processName = "";
    private static String packageName = "";
    static final Set<String> colorTags = new HashSet();
    static long colorLogTime = 0;
    private static CopyOnWriteArrayList<ColorLevelChangeListener> colorLevelChangeListenerList = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<ILogCallback> sLogCallbackList = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<IAddLogCallback> sAddLogCallbackList = new CopyOnWriteArrayList<>();
    private static final String[] PERMS = {"android.permission.WRITE_EXTERNAL_STORAGE", "android.permission.READ_PHONE_STATE"};
    private static boolean useNewQLog = false;
    private static boolean useXlog = false;
    private static long mainThreadId = -1;
    private static Xlog xlog = null;
    private static ThreadFactory asyncThreadFactory = null;
    private static ExecutorService asyncSingleThreadExecutor = null;

    public interface ColorLevelChangeListener {
        void colorLevelChange(boolean z, int i);
    }

    public interface IAddLogCallback {
        String onAddLog(int i, String str, String str2);
    }

    public interface ILogCallback {
        void onWriteLog(String str, String str2);

        void onWriteLog(String str, byte[] bArr);
    }

    public interface OnPrintlnCallback {
        void onPrintln(int i, String str, String str2);

        void onPrintln(int i, String str, String str2, Throwable th);
    }

    private static void addLogItem(byte b, String str, int i, String str2, Throwable th) {
        long currentTimeMillis = System.currentTimeMillis();
        clearColorTags(currentTimeMillis);
        if (!isPublish || b >= forceCallbackAndroidLogLevel) {
            Iterator<IAddLogCallback> it = sAddLogCallbackList.iterator();
            while (it.hasNext()) {
                str2 = it.next().onAddLog(b, str, str2);
            }
        }
        String str3 = str2;
        if (useXlog) {
            printByXlog(b, str, str3, th);
            return;
        }
        QLogItemManager qLogItemManager = sLogManager;
        if (qLogItemManager != null) {
            qLogItemManager.addLog(b, currentTimeMillis, i, str, str3, th);
        }
    }

    public static int androidExtractLog(Date date, Date date2, String str, String str2) {
        Xlog xlog2 = xlog;
        if (xlog2 != null) {
            return xlog2.androidExtractLog(0L, date, date2, str, str2);
        }
        return -1;
    }

    public static String byteLevel2StringLevel(Byte b) {
        byte byteValue = b.byteValue();
        return byteValue != 2 ? byteValue != 3 ? byteValue != 4 ? "D" : "E" : "W" : ANDROID_LOG_LEVEL_I;
    }

    private static void clearColorTags(long j) {
        long j2 = colorLogTime;
        if (j2 == 0 || j - j2 <= 1800000) {
            return;
        }
        colorLogTime = 0L;
        colorTags.clear();
    }

    private static String concatLogMessages(Object[] objArr, int i) {
        if (objArr == null || objArr.length == 0) {
            return "";
        }
        if (i <= 0) {
            i = MAX_LOG_LENGTH;
        }
        StringBuilder sb = new StringBuilder(Math.min(objArr.length * 30, i));
        int length = objArr.length;
        int i2 = 0;
        while (true) {
            if (i2 >= length) {
                break;
            }
            Object obj = objArr[i2];
            if (obj != null) {
                String obj2 = obj.toString();
                int length2 = obj2.length();
                if (length2 > i) {
                    sb.append((CharSequence) obj2, 0, i);
                    break;
                }
                sb.append(obj2);
                i -= length2;
                if (i <= 0) {
                    break;
                }
            }
            i2++;
        }
        return sb.toString();
    }

    public static void d(String str, int i, String str2) {
        d(str, i, str2, (Throwable) null);
    }

    public static void dAsync(String str, int i, String str2) {
        ExecutorService executorService = asyncSingleThreadExecutor;
        if (executorService != null) {
            executorService.submit((Runnable) new i(str, i, str2));
        }
    }

    public static void deleteExpireLogFileActively() {
        QLogItemManager qLogItemManager = sLogManager;
        if (qLogItemManager != null) {
            qLogItemManager.deleteExpireLogFile();
        }
    }

    public static void e(String str, int i, String str2) {
        e(str, i, str2, (Throwable) null);
    }

    public static void eAsync(String str, int i, String str2) {
        ExecutorService executorService = asyncSingleThreadExecutor;
        if (executorService != null) {
            executorService.submit((Runnable) new j(str, i, str2));
        }
    }

    public static void flushLog() {
        QLogItemManager qLogItemManager = sLogManager;
        if (qLogItemManager != null) {
            qLogItemManager.flushLog();
        }
    }

    public static String getDeleteExpireLogRecordFilePath() {
        QLogItemManager qLogItemManager = sLogManager;
        return qLogItemManager != null ? qLogItemManager.getDeleteExpireLogRecordFilePath() : "";
    }

    public static String getLogExternalPath(Context context) {
        File externalFilesDir = context.getExternalFilesDir(null);
        return externalFilesDir != null ? externalFilesDir.getPath() : Environment.getExternalStorageDirectory().getPath();
    }

    @SuppressLint({"SimpleDateFormat"})
    public static SimpleDateFormat getLogFileFormatter() {
        QLogItemManager qLogItemManager = sLogManager;
        return qLogItemManager != null ? qLogItemManager.getLogFileFormatter() : new SimpleDateFormat("yy.MM.dd.HH");
    }

    public static String getLogPath() {
        QLogItemManager qLogItemManager = sLogManager;
        if (qLogItemManager != null) {
            String logPath = qLogItemManager.getLogPath();
            if (!TextUtils.isEmpty(logPath)) {
                return logPath;
            }
        }
        return getLogExternalPath(sAppContext) + LogWriterManager.QLOG_PARENT_DIRECTORY + packageName.replace(".", "/") + "/";
    }

    public static String getManualLogLevelPath() {
        return manualLogLevelPath;
    }

    public static List<File> getOutOfCurHourLogs() {
        try {
            return QLogHelper.getOutOfCurHourLogs();
        } catch (Exception e) {
            e("QLog.getOutOfCurHourLogs", 1, "getOutOfCurHourLogs error", e);
            return new ArrayList();
        }
    }

    public static String getPackageName() {
        return packageName;
    }

    public static String getProcessName() {
        return processName;
    }

    public static String getReportLevel(int i) {
        return i == 2 ? "W" : i == 4 ? "D" : "E";
    }

    public static String getStackTraceString(Throwable th) {
        return Log.getStackTraceString(th);
    }

    private static String getTag(String str) {
        if (!sLogcatHooked) {
            return str;
        }
        return LOG_HOOK_PREFIX_TAG + str;
    }

    public static int getUIN_REPORTLOG_LEVEL() {
        return UIN_REPORTLOG_LEVEL;
    }

    private static void handleAndroidRoomLogPrint(String str, String str2, String str3) {
        if (TextUtils.isEmpty(str2)) {
            return;
        }
        if (str2.length() <= MAX_LOG_LENGTH) {
            printAndroidLog(str, str2, str3);
            return;
        }
        if (!isPublish) {
            throw new IllegalArgumentException("Business log tag, over 1024 !!!");
        }
        printAndroidLog(str, str2.substring(0, MAX_LOG_LENGTH), str3);
        RuntimeException runtimeException = new RuntimeException("logTagOverLengthException");
        runtimeException.fillInStackTrace();
        HashMap hashMap = new HashMap();
        hashMap.put("androidLogLevel", str);
        hashMap.put("logTag", str2);
        hashMap.put("logMsg", str3);
        hashMap.put("stack", getStackTraceString(runtimeException));
        QLogReportManager.getInstance().sendToBeacon("log_tag_over_length_1024", hashMap);
    }

    public static void i(String str, int i, String str2) {
        i(str, i, str2, null);
    }

    public static void iAsync(String str, int i, String str2) {
        ExecutorService executorService = asyncSingleThreadExecutor;
        if (executorService != null) {
            executorService.submit((Runnable) new m(str, i, str2));
        }
    }

    public static void init(String str, String str2, String str3, long j) {
        init(str, str2, str3, j, false);
    }

    /* JADX WARN: Removed duplicated region for block: B:13:0x00aa  */
    /* JADX WARN: Removed duplicated region for block: B:17:0x00ad  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private static void initQlog(QLogConfig qLogConfig) {
        boolean z;
        boolean isDebug2;
        sAppContext = qLogConfig.getAppContext();
        processName = qLogConfig.getProcessName();
        packageName = qLogConfig.getPackageName();
        useNewQLog = qLogConfig.isUseNewQLog();
        useXlog = qLogConfig.isUseXlog();
        if (!TextUtils.isEmpty(qLogConfig.getLogPath())) {
            LogWriterManager.QLOG_PARENT_DIRECTORY = qLogConfig.getLogPath();
        }
        QLogItemManager qLogItemManager = new QLogItemManager(sAppContext, qLogConfig.getPackageName(), qLogConfig.getProcessName(), qLogConfig.getBuildNumber(), qLogConfig.isIs64Bit());
        sLogManager = qLogItemManager;
        qLogItemManager.init(qLogConfig.getDelayInit());
        if (!new File(getLogPath() + MSF_IS_COLOR_LEVEL).exists()) {
            if (!new File(getLogPath() + qLogConfig.getQLogConfigBeanSwitchKey()).exists() && qLogConfig.isGray()) {
                z = true;
                setFullEncryptedLogMode(z);
                isDebug2 = qLogConfig.isDebug();
                isDebug = isDebug2;
                if (!isDebug2) {
                    _DEFAULT_REPORTLOG_LEVEL = 4;
                } else if (qLogConfig.isPublish()) {
                    _DEFAULT_REPORTLOG_LEVEL = 1;
                } else {
                    _DEFAULT_REPORTLOG_LEVEL = 2;
                }
                int i = _DEFAULT_REPORTLOG_LEVEL;
                UIN_REPORTLOG_LEVEL = i;
                d(tag, 1, "[init] setDebugMode call. ", " UIN_REPORTLOG_LEVEL: ", Integer.valueOf(i), " _DEFAULT_REPORTLOG_LEVEL: ", Integer.valueOf(_DEFAULT_REPORTLOG_LEVEL), " debug: ", Boolean.valueOf(isDebug));
                isPublish = qLogConfig.isPublish();
                forceCallbackAndroidLogLevel = qLogConfig.getForceCallbackAndroidLogLevel();
                d(tag, 1, "[init] setPublishMode call. isPublish=" + isPublish + ", qLogConfig=" + qLogConfig.toString());
                QLogReportManager.getInstance().setQLogReport(qLogConfig.getQLogReport());
                QLogFirebaseReportManager.getInstance().setFirebaseLogReport(qLogConfig.getFirebaseLogReport());
            }
        }
        z = false;
        setFullEncryptedLogMode(z);
        isDebug2 = qLogConfig.isDebug();
        isDebug = isDebug2;
        if (!isDebug2) {
        }
        int i2 = _DEFAULT_REPORTLOG_LEVEL;
        UIN_REPORTLOG_LEVEL = i2;
        d(tag, 1, "[init] setDebugMode call. ", " UIN_REPORTLOG_LEVEL: ", Integer.valueOf(i2), " _DEFAULT_REPORTLOG_LEVEL: ", Integer.valueOf(_DEFAULT_REPORTLOG_LEVEL), " debug: ", Boolean.valueOf(isDebug));
        isPublish = qLogConfig.isPublish();
        forceCallbackAndroidLogLevel = qLogConfig.getForceCallbackAndroidLogLevel();
        d(tag, 1, "[init] setPublishMode call. isPublish=" + isPublish + ", qLogConfig=" + qLogConfig.toString());
        QLogReportManager.getInstance().setQLogReport(qLogConfig.getQLogReport());
        QLogFirebaseReportManager.getInstance().setFirebaseLogReport(qLogConfig.getFirebaseLogReport());
    }

    private static void initXlog(QLogConfig qLogConfig) {
        initXlogParams(qLogConfig);
        if (useXlog) {
            System.loadLibrary("c++_shared");
            System.loadLibrary("marsxlog");
            Xlog xlog2 = new Xlog();
            xlog = xlog2;
            xlog2.setMaxAliveTime(0L, 604800L);
            xlog.setConsoleLogOpen(0L, !isPublish);
            xlog.appenderOpen(0, 0, "", getLogPath(), "QQXlog_" + processName.replace(":", "_"), 0);
            mainThreadId = Looper.getMainLooper().getThread().getId();
        }
        d(tag, 1, "[init] setDebugMode call. ", " UIN_REPORTLOG_LEVEL: ", Integer.valueOf(UIN_REPORTLOG_LEVEL), " _DEFAULT_REPORTLOG_LEVEL: ", Integer.valueOf(_DEFAULT_REPORTLOG_LEVEL), " debug: ", Boolean.valueOf(isDebug));
        d(tag, 1, "[init] setPublishMode call. isPublish=" + isPublish + ", qLogConfig=" + qLogConfig.toString());
    }

    private static void initXlogParams(QLogConfig qLogConfig) {
        sAppContext = qLogConfig.getAppContext();
        processName = qLogConfig.getProcessName();
        packageName = qLogConfig.getPackageName();
        useNewQLog = qLogConfig.isUseNewQLog();
        useXlog = qLogConfig.isUseXlog();
        if (!TextUtils.isEmpty(qLogConfig.getLogPath())) {
            LogWriterManager.QLOG_PARENT_DIRECTORY = qLogConfig.getLogPath();
        }
        isDebug = qLogConfig.isDebug();
        isGray = qLogConfig.isGray();
        boolean isPublish2 = qLogConfig.isPublish();
        isPublish = isPublish2;
        if (isDebug) {
            _DEFAULT_REPORTLOG_LEVEL = 4;
        } else if (!isPublish2) {
            _DEFAULT_REPORTLOG_LEVEL = 2;
        } else if (isGray) {
            _DEFAULT_REPORTLOG_LEVEL = 2;
        } else {
            _DEFAULT_REPORTLOG_LEVEL = 1;
        }
        UIN_REPORTLOG_LEVEL = _DEFAULT_REPORTLOG_LEVEL;
        ThreadFactory threadFactory = new ThreadFactory() {
            private AtomicInteger threadNumber = new AtomicInteger(1);

            @Override
            public Thread newThread(Runnable runnable) {
                return new BaseThread(runnable, "QLog_async_single_thread_pool_" + this.threadNumber.getAndIncrement());
            }
        };
        asyncThreadFactory = threadFactory;
        asyncSingleThreadExecutor = ProxyExecutors.newSingleThreadExecutor(threadFactory);
        QLogReportManager.getInstance().setQLogReport(qLogConfig.getQLogReport());
        QLogFirebaseReportManager.getInstance().setFirebaseLogReport(qLogConfig.getFirebaseLogReport());
    }

    public static boolean isColorLevel() {
        return UIN_REPORTLOG_LEVEL > 1 || isEncrypted();
    }

    public static boolean isDebugVersion() {
        return isDebug;
    }

    public static boolean isDevelopLevel() {
        return UIN_REPORTLOG_LEVEL >= 4;
    }

    private static boolean isEncrypted() {
        QLogItemManager qLogItemManager = sLogManager;
        return qLogItemManager != null && qLogItemManager.isEncrypted();
    }

    public static boolean isExistSDCard() {
        try {
            return Environment.getExternalStorageState().equals("mounted");
        } catch (Exception unused) {
            return false;
        }
    }

    public static boolean isHasStoragePermission(Context context) {
        if (sHasStoragePermission) {
            return true;
        }
        if (context == null || context.checkSelfPermission(PERMS[0]) != 0) {
            return false;
        }
        sHasStoragePermission = true;
        return true;
    }

    private static boolean isThreadLocalRandomOneTenthRate() {
        return ThreadLocalRandom.current().nextInt(10) == 0;
    }

    public static boolean isUseNewQLog() {
        return useNewQLog;
    }

    public static boolean isUseXlog() {
        return useXlog;
    }

    private static void onPrintln(String str, String str2) {
        OnPrintlnCallback onPrintlnCallback = sOnPrintlnCallback;
        if (onPrintlnCallback != null) {
            onPrintlnCallback.onPrintln(3, str, str2);
        }
    }

    public static void p(String str, String str2) {
        Log.d(getTag(str), "[s]" + str2);
    }

    private static void printAndroidLog(String str, String str2, String str3) {
        str.getClass();
        char c = 65535;
        switch (str.hashCode()) {
            case 68:
                if (str.equals("D")) {
                    c = 0;
                    break;
                }
                break;
            case 69:
                if (str.equals("E")) {
                    c = 1;
                    break;
                }
                break;
            case 73:
                if (str.equals(ANDROID_LOG_LEVEL_I)) {
                    c = 2;
                    break;
                }
                break;
            case 87:
                if (str.equals("W")) {
                    c = 3;
                    break;
                }
                break;
        }
        switch (c) {
            case 0:
                Log.d(str2, str3);
                return;
            case USR:
                Log.e(str2, str3);
                return;
            case CLR:
                Log.i(str2, str3);
                return;
            case 3:
                Log.w(str2, str3);
                return;
            default:
                Log.d(str2, str3);
                return;
        }
    }

    private static void printByXlog(byte b, String str, String str2, Throwable th) {
        String str3;
        if (xlog != null) {
            if (str2.length() <= MAX_LOG_LENGTH || b >= 4) {
                str3 = str2;
            } else {
                str3 = str2.substring(0, MAX_LOG_LENGTH) + LOG_TRUNCATED_NOTE;
            }
            int myPid = Process.myPid();
            int myTid = Process.myTid();
            long id = Thread.currentThread().getId();
            if (b == 2) {
                long j = myTid;
                xlog.logI(str, myPid, j, id, str3);
                if (th != null) {
                    xlog.logI(str, myPid, j, id, Log.getStackTraceString(th));
                }
            } else if (b == 3) {
                long j2 = myTid;
                xlog.logW(str, myPid, j2, id, str3);
                if (th != null) {
                    xlog.logW(str, myPid, j2, id, Log.getStackTraceString(th));
                }
            } else if (b != 4) {
                long j3 = myTid;
                xlog.logD(str, myPid, j3, id, str3);
                if (th != null) {
                    xlog.logD(str, myPid, j3, id, Log.getStackTraceString(th));
                }
            } else {
                long j4 = myTid;
                xlog.logE(str, myPid, j4, id, str3);
                if (th != null) {
                    xlog.logE(str, myPid, j4, id, Log.getStackTraceString(th));
                }
            }
            QLogHelper.reportToGoogleFirebase(0L, b, str, str3, th);
        }
    }

    public static void removeAddLogCallback(IAddLogCallback iAddLogCallback) {
        sAddLogCallbackList.remove(iAddLogCallback);
    }

    @Deprecated
    public static void removeLogCallback(ILogCallback iLogCallback) {
        sLogCallbackList.remove(iLogCallback);
    }

    public static void setAddLogCallback(IAddLogCallback iAddLogCallback) {
        CopyOnWriteArrayList<IAddLogCallback> copyOnWriteArrayList = sAddLogCallbackList;
        if (copyOnWriteArrayList.contains(iAddLogCallback)) {
            return;
        }
        copyOnWriteArrayList.add(iAddLogCallback);
    }

    public static void setColorLevelChangeListener(ColorLevelChangeListener colorLevelChangeListener) {
        if (colorLevelChangeListenerList.contains(colorLevelChangeListener)) {
            return;
        }
        colorLevelChangeListenerList.add(colorLevelChangeListener);
    }

    public static void setFullEncryptedLogMode(boolean z) {
        QLogItemManager qLogItemManager = sLogManager;
        if (qLogItemManager != null) {
            qLogItemManager.setFullEncryptedLogMode(z);
        }
    }

    @Deprecated
    public static void setLogCallback(ILogCallback iLogCallback) {
        CopyOnWriteArrayList<ILogCallback> copyOnWriteArrayList = sLogCallbackList;
        if (copyOnWriteArrayList.contains(iLogCallback)) {
            return;
        }
        copyOnWriteArrayList.add(iLogCallback);
    }

    public static void setManualLogLevel(int i) {
        if (i < 1 || i > 4 || UIN_REPORTLOG_LEVEL == i) {
            return;
        }
        UIN_REPORTLOG_LEVEL = i;
        d(tag, 1, Thread.currentThread().getName() + "[level]  set log level manual, " + UIN_REPORTLOG_LEVEL, new RuntimeException());
    }

    public static void setOnPrintlnCallback(OnPrintlnCallback onPrintlnCallback) {
        sOnPrintlnCallback = onPrintlnCallback;
    }

    private static void setReportLogLevel(int i) {
        if (i < 1 || i > 4 || UIN_REPORTLOG_LEVEL == i) {
            if (isColorLevel()) {
                d(tag, 2, "setReportLogLevel illegal, logLevel=", Integer.valueOf(i));
                return;
            }
            return;
        }
        UIN_REPORTLOG_LEVEL = i;
        d(tag, 1, Thread.currentThread().getName() + " set log level " + i, new RuntimeException());
        try {
            if (!processName.contains(":") && Looper.getMainLooper() != Looper.myLooper()) {
                File file = new File(getLogPath() + MSF_IS_COLOR_LEVEL);
                if (i >= 2 && !file.exists()) {
                    file.createNewFile();
                } else {
                    if (i >= 2 || !file.exists()) {
                        return;
                    }
                    file.delete();
                }
            }
        } catch (Throwable th) {
            d(tag, 1, "create file fail, ", th);
        }
    }

    private static boolean setReportLogLevelByFile() {
        BufferedReader bufferedReader;
        Throwable th;
        if (!isExistSDCard()) {
            return false;
        }
        try {
            String str = manualLogLevelPath;
            File file = new File(str);
            if (file.exists() && file.isFile()) {
                bufferedReader = new BufferedReader(new FileReader(str));
                try {
                    String readLine = bufferedReader.readLine();
                    if (readLine == null) {
                        try {
                            bufferedReader.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        return false;
                    }
                    int parseInt = Integer.parseInt(readLine);
                    if (parseInt >= 1 && parseInt <= 4) {
                        UIN_REPORTLOG_LEVEL = parseInt;
                        d(tag, 1, Thread.currentThread().getName() + "[level] set log level manual, " + parseInt);
                        try {
                            bufferedReader.close();
                        } catch (IOException e2) {
                            e2.printStackTrace();
                        }
                        return true;
                    }
                    if (isColorLevel()) {
                        d(tag, 2, "setReportLogLevel illegal, logLevel=", Integer.valueOf(parseInt));
                    }
                    try {
                        bufferedReader.close();
                    } catch (IOException e3) {
                        e3.printStackTrace();
                    }
                    return false;
                } catch (Throwable th2) {
                    th = th2;
                    try {
                        e(tag, 1, "[level] set log, manual log level read fail. ", th);
                        return false;
                    } finally {
                        if (bufferedReader != null) {
                            try {
                                bufferedReader.close();
                            } catch (IOException e4) {
                                e4.printStackTrace();
                            }
                        }
                    }
                }
            }
            return false;
        } catch (Throwable th3) {
            bufferedReader = null;
            th = th3;
        }
    }

    public static void setUIN_REPORTLOG_LEVEL(int i) {
        if (setReportLogLevelByFile()) {
            return;
        }
        setReportLogLevel(i);
        if (colorLevelChangeListenerList.size() > 0) {
            Iterator<ColorLevelChangeListener> it = colorLevelChangeListenerList.iterator();
            while (it.hasNext()) {
                ColorLevelChangeListener next = it.next();
                boolean z = true;
                if (i <= 1) {
                    z = false;
                }
                next.colorLevelChange(z, i);
            }
        }
    }

    public static void useNewQLog() {
        useNewQLog = true;
    }

    public static void useOldQLog() {
        useNewQLog = false;
    }

    public static void w(String str, int i, String str2) {
        w(str, i, str2, null);
    }

    public static void wAsync(String str, int i, String str2) {
        ExecutorService executorService = asyncSingleThreadExecutor;
        if (executorService != null) {
            executorService.submit((Runnable) new o(str, i, str2));
        }
    }

    public static class LogFile extends File {
        public String stuffix;

        public LogFile(File file, String str) {
            super(file, str);
            this.stuffix = "";
        }

        public LogFile(String str) {
            super(str);
            this.stuffix = "";
        }
    }

    public static void d(String str, int i, Throwable th, Object... objArr) {
        if ((UIN_REPORTLOG_LEVEL >= i || colorTags.contains(str)) || isEncrypted()) {
            d(str, i, concatLogMessages(objArr, 1025), th);
        }
    }

    public static void e(String str, int i, Throwable th, Object... objArr) {
        boolean z = UIN_REPORTLOG_LEVEL >= i || colorTags.contains(str);
        if (z || isEncrypted()) {
            String concatLogMessages = concatLogMessages(objArr, 1025);
            if (!useXlog && z) {
                handleAndroidRoomLogPrint("E", getTag(str), concatLogMessages + getStackTraceString(th));
            }
            addLogItem((byte) 4, str, i, concatLogMessages, th);
        }
    }

    public static void i(String str, int i, String str2, Throwable th) {
        boolean z = UIN_REPORTLOG_LEVEL >= i || colorTags.contains(str);
        if (z || isEncrypted()) {
            if (str2 == null) {
                str2 = "";
            }
            if (!useXlog && z) {
                handleAndroidRoomLogPrint(ANDROID_LOG_LEVEL_I, getTag(str), str2 + getStackTraceString(th));
            }
            addLogItem((byte) 2, str, i, str2, th);
        }
    }

    public static void init(String str, String str2, String str3, long j, boolean z) {
        useNewQLog = z;
        processName = str2;
        packageName = str;
        QLogItemManager qLogItemManager = new QLogItemManager(sAppContext, str, str2, str3, true);
        sLogManager = qLogItemManager;
        qLogItemManager.init(j);
    }

    public static void w(String str, int i, String str2, Throwable th) {
        boolean z = UIN_REPORTLOG_LEVEL >= i || colorTags.contains(str);
        if (z || isEncrypted()) {
            if (str2 == null) {
                str2 = "";
            }
            if (!useXlog && z) {
                handleAndroidRoomLogPrint("W", getTag(str), str2 + getStackTraceString(th));
            }
            addLogItem((byte) 3, str, i, str2, th);
        }
    }

    public static void dAsync(String str, int i, String str2, Throwable th) {
        ExecutorService executorService = asyncSingleThreadExecutor;
        if (executorService != null) {
            executorService.submit((Runnable) new n(str, i, str2, th));
        }
    }

    public static void eAsync(String str, int i, String str2, Throwable th) {
        ExecutorService executorService = asyncSingleThreadExecutor;
        if (executorService != null) {
            executorService.submit((Runnable) new l(str, i, str2, th));
        }
    }

    public static void flushLog(boolean z) {
        QLogItemManager qLogItemManager = sLogManager;
        if (qLogItemManager != null) {
            qLogItemManager.flushLog(z);
        }
    }

    public static void iAsync(String str, int i, String str2, Throwable th) {
        ExecutorService executorService = asyncSingleThreadExecutor;
        if (executorService != null) {
            executorService.submit((Runnable) new k(str, i, str2, th));
        }
    }

    private static void onPrintln(String str, String str2, Throwable th) {
        OnPrintlnCallback onPrintlnCallback = sOnPrintlnCallback;
        if (onPrintlnCallback != null) {
            onPrintlnCallback.onPrintln(3, str, str2, th);
        }
    }

    public static void wAsync(String str, int i, String str2, Throwable th) {
        ExecutorService executorService = asyncSingleThreadExecutor;
        if (executorService != null) {
            executorService.submit((Runnable) new p(str, i, str2, th));
        }
    }

    public static void d(String str, int i, Object... objArr) {
        d(str, i, (Throwable) null, objArr);
    }

    public static void d(String str, int i, String str2, Throwable th) {
        boolean z = UIN_REPORTLOG_LEVEL >= i || colorTags.contains(str);
        if (z || isEncrypted()) {
            if (str2 == null) {
                str2 = "";
            }
            if (!useXlog && z) {
                String tag2 = getTag(str);
                handleAndroidRoomLogPrint("D", tag2, str2 + getStackTraceString(th));
                if (th == null) {
                    onPrintln(tag2, str2);
                } else {
                    onPrintln(tag2, str2, th);
                }
            }
            addLogItem((byte) 1, str, i, str2, th);
        }
    }

    public static void init(QLogConfig qLogConfig) {
        if (qLogConfig.isUseXlog()) {
            initXlog(qLogConfig);
        } else {
            initQlog(qLogConfig);
        }
    }

    public static void e(String str, int i, Object... objArr) {
        e(str, i, (Throwable) null, objArr);
    }

    public static void e(String str, int i, String str2, Throwable th) {
        boolean z = UIN_REPORTLOG_LEVEL >= i || colorTags.contains(str);
        if (z || isEncrypted()) {
            if (str2 == null) {
                str2 = "";
            }
            if (!useXlog && z) {
                handleAndroidRoomLogPrint("E", getTag(str), str2 + getStackTraceString(th));
            }
            addLogItem((byte) 4, str, i, str2, th);
        }
    }

    public static void d(String str, int i, byte[] bArr, Throwable th) {
        d(str, i, new String(bArr, logCharset), th);
    }
}
