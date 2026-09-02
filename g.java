//
// Decompiled by Jadx - 883ms
//
package zl;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Insets;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Process;
import android.os.StatFs;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Size;
import android.view.Display;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.WindowMetrics;
import android.view.inputmethod.InputMethodManager;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.c;
import androidx.core.text.util.b;
import androidx.core.view.l;
import coil.decode.t;
import com.badlogic.gdx.scenes.scene2d.ui.i;
import g1.f;
import im.weshine.foundation.base.log.TraceLog;
import im.weshine.foundation.base.utils.RomUtils;
import io.sentry.instrumentation.file.SentryFileInputStream;
import io.sentry.instrumentation.file.SentryFileOutputStream;
import io.sentry.instrumentation.file.SentryFileReader;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import kotlin.collections.r;
import kotlin.text.o;
import m4.a;

public abstract class g {
    public static final char[] a = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
    public static volatile boolean b;
    public static String c;
    public static Boolean d;
    public static Boolean e;
    public static Integer f;
    public static Boolean g;
    public static Boolean h;
    public static Boolean i;

    public static String A(File file) {
        int i2;
        FileInputStream fileInputStream = null;
        try {
            try {
                MessageDigest messageDigest = MessageDigest.getInstance("MD5");
                fileInputStream = SentryFileInputStream.Factory.create(new FileInputStream(file), file);
                byte[] bArr = new byte[1024];
                while (true) {
                    int read = fileInputStream.read(bArr);
                    if (read <= 0) {
                        break;
                    }
                    messageDigest.update(bArr, 0, read);
                }
                byte[] digest = messageDigest.digest();
                StringBuilder sb = new StringBuilder(digest.length * 2);
                for (i2 = 0; i2 < digest.length; i2++) {
                    char[] cArr = a;
                    sb.append(cArr[(digest[i2] & 240) >>> 4]);
                    sb.append(cArr[digest[i2] & 15]);
                }
                String sb2 = sb.toString();
                try {
                    fileInputStream.close();
                } catch (Throwable unused) {
                }
                return sb2;
            } catch (IOException e2) {
                e2.printStackTrace();
                if (fileInputStream == null) {
                    return "";
                }
                try {
                    fileInputStream.close();
                } catch (Throwable unused2) {
                    return "";
                }
            } catch (NoSuchAlgorithmException e3) {
                e3.printStackTrace();
                if (fileInputStream == null) {
                    return "";
                }
                fileInputStream.close();
            }
        } catch (Throwable th) {
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (Throwable unused3) {
                }
            }
            throw th;
        }
    }

    public static long B(File file) {
        long length;
        long j = 0;
        try {
            File[] listFiles = file.listFiles();
            for (int i2 = 0; i2 < listFiles.length; i2++) {
                if (listFiles[i2].isDirectory()) {
                    length = B(listFiles[i2]);
                } else if (!listFiles[i2].getName().endsWith("classes.dex")) {
                    length = listFiles[i2].length();
                }
                j += length;
            }
            return j;
        } catch (Exception e2) {
            e2.printStackTrace();
            return j;
        }
    }

    public static int C(int i2) {
        float[] fArr = new float[3];
        Color.colorToHSV(i2, fArr);
        float f2 = fArr[0] + 0.5f;
        fArr[0] = f2;
        if (f2 >= 360.0f) {
            fArr[0] = f2 - 360.0f;
        }
        float f3 = fArr[2] + 0.5f;
        fArr[2] = f3;
        if (f3 > 1.0f) {
            fArr[2] = f3 - 1.0f;
        }
        return Color.HSVToColor(fArr);
    }

    public static String D(Context context, String str) {
        StringBuilder sb = new StringBuilder();
        BufferedReader bufferedReader = null;
        try {
            try {
                try {
                    BufferedReader bufferedReader2 = new BufferedReader(new InputStreamReader(context.getAssets().open(str)));
                    while (true) {
                        try {
                            String readLine = bufferedReader2.readLine();
                            if (readLine == null) {
                                break;
                            }
                            sb.append(readLine);
                        } catch (IOException e2) {
                            e = e2;
                            bufferedReader = bufferedReader2;
                            e.printStackTrace();
                            if (bufferedReader != null) {
                                bufferedReader.close();
                            }
                            return sb.toString();
                        } catch (Throwable th) {
                            th = th;
                            bufferedReader = bufferedReader2;
                            if (bufferedReader != null) {
                                try {
                                    bufferedReader.close();
                                } catch (IOException e3) {
                                    e3.printStackTrace();
                                }
                            }
                            throw th;
                        }
                    }
                    bufferedReader2.close();
                } catch (Throwable th2) {
                    th = th2;
                }
            } catch (IOException e4) {
                e = e4;
            }
        } catch (IOException e5) {
            e5.printStackTrace();
        }
        return sb.toString();
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Removed duplicated region for block: B:40:0x0078 A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /* JADX WARN: Type inference failed for: r0v2 */
    /* JADX WARN: Type inference failed for: r0v4, types: [java.io.FileInputStream] */
    /* JADX WARN: Type inference failed for: r0v9 */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public static String E(String str) {
        ?? r0;
        Throwable th;
        FileInputStream fileInputStream;
        File file = new File(str);
        if (file.exists() && !file.isDirectory()) {
            try {
                try {
                    MessageDigest messageDigest = MessageDigest.getInstance("MD5");
                    fileInputStream = SentryFileInputStream.Factory.create(new FileInputStream(file), file);
                    try {
                        byte[] bArr = new byte[2048];
                        System.currentTimeMillis();
                        while (true) {
                            int read = fileInputStream.read(bArr);
                            if (read != -1) {
                                messageDigest.update(bArr, 0, read);
                            } else {
                                String c2 = c(messageDigest.digest());
                                i.n("FileUtils", "get md5 for file :" + c2);
                                try {
                                    fileInputStream.close();
                                    return c2;
                                } catch (Exception e2) {
                                    e2.printStackTrace();
                                    return c2;
                                }
                            }
                        }
                    } catch (Exception e3) {
                        e = e3;
                        e.printStackTrace();
                        if (fileInputStream != null) {
                            try {
                                fileInputStream.close();
                            } catch (Exception e4) {
                                e4.printStackTrace();
                            }
                        }
                        return null;
                    }
                } catch (Throwable th2) {
                    th = th2;
                    r0 = file;
                    if (r0 != 0) {
                        try {
                            r0.close();
                        } catch (Exception e5) {
                            e5.printStackTrace();
                        }
                    }
                    throw th;
                }
            } catch (Exception e6) {
                e = e6;
                fileInputStream = null;
            } catch (Throwable th3) {
                r0 = 0;
                th = th3;
                if (r0 != 0) {
                }
                throw th;
            }
        } else {
            return "";
        }
    }

    public static int F(int i2) {
        float[] fArr = new float[3];
        Color.colorToHSV(i2, fArr);
        if (fArr[2] > 0.5f) {
            return Integer.MIN_VALUE;
        }
        return -2130706433;
    }

    public static PackageInfo G(Context context, File file) {
        PackageManager packageManager;
        if (context != null && file.exists() && (packageManager = context.getPackageManager()) != null) {
            return packageManager.getPackageArchiveInfo(file.getAbsolutePath(), 1);
        }
        return null;
    }

    public static RomUtils.RomType H() {
        RomUtils.RomType romType = RomUtils.RomType.COMMOM;
        if (f()) {
            return RomUtils.RomType.MIUI;
        }
        String str = Build.MANUFACTURER;
        if (str.contains("HUAWEI")) {
            return RomUtils.RomType.HUAWEI;
        }
        if (str.toLowerCase().contains("oppo")) {
            return RomUtils.RomType.OPPO;
        }
        if (e()) {
            return RomUtils.RomType.MEIZU;
        }
        if (!str.contains("QiKU") && !str.contains("360")) {
            return romType;
        }
        return RomUtils.RomType.QIHU360;
    }

    public static long I() {
        File rootDirectory;
        try {
            if ("mounted".equals(Environment.getExternalStorageState())) {
                rootDirectory = Environment.getExternalStorageDirectory();
            } else {
                rootDirectory = Environment.getRootDirectory();
            }
            StatFs statFs = new StatFs(rootDirectory.getAbsolutePath());
            return statFs.getAvailableBlocksLong() * statFs.getBlockSizeLong();
        } catch (IllegalArgumentException unused) {
            return 0L;
        }
    }

    /* JADX WARN: Removed duplicated region for block: B:29:0x0048 A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public static String J(String str) {
        BufferedReader bufferedReader;
        BufferedReader bufferedReader2 = null;
        try {
            bufferedReader = new BufferedReader(new InputStreamReader(Runtime.getRuntime().exec("getprop ".concat(str)).getInputStream()), 4096);
            try {
                try {
                    String readLine = bufferedReader.readLine();
                    bufferedReader.close();
                    try {
                        bufferedReader.close();
                        return readLine;
                    } catch (IOException e2) {
                        e2.printStackTrace();
                        return readLine;
                    }
                } catch (IOException e3) {
                    e = e3;
                    e.printStackTrace();
                    if (bufferedReader != null) {
                        try {
                            bufferedReader.close();
                        } catch (IOException e4) {
                            e4.printStackTrace();
                        }
                    }
                    return null;
                }
            } catch (Throwable th) {
                th = th;
                bufferedReader2 = bufferedReader;
                if (bufferedReader2 != null) {
                    try {
                        bufferedReader2.close();
                    } catch (IOException e5) {
                        e5.printStackTrace();
                    }
                }
                throw th;
            }
        } catch (IOException e6) {
            e = e6;
            bufferedReader = null;
        } catch (Throwable th2) {
            th = th2;
            if (bufferedReader2 != null) {
            }
            throw th;
        }
    }

    public static long K() {
        long currentTimeMillis = System.currentTimeMillis();
        return currentTimeMillis - (currentTimeMillis % 0x05265c00);
    }

    public static long L() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(11, 0);
        calendar.set(12, 0);
        calendar.set(13, 0);
        calendar.set(14, 0);
        return calendar.getTime().getTime();
    }

    public static void M(View view) {
        ((InputMethodManager) view.getContext().getSystemService("input_method")).hideSoftInputFromWindow(view.getWindowToken(), 2);
    }

    public static boolean N(int i2, long j, long j2) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(j);
        calendar.set(11, 0);
        calendar.set(12, 0);
        calendar.set(13, 0);
        calendar.set(14, 0);
        Calendar calendar2 = Calendar.getInstance();
        calendar2.setTimeInMillis(j2);
        calendar2.set(11, 0);
        calendar2.set(12, 0);
        calendar2.set(13, 0);
        calendar2.set(14, 0);
        if (Math.abs(calendar.getTimeInMillis() - calendar2.getTimeInMillis()) / 0x05265c00 <= i2) {
            return false;
        }
        return true;
    }

    /* JADX WARN: Code restructure failed: missing block: B:68:0x00c5, code lost:
    
        if (r5 == null) goto L44;
     */
    /* JADX WARN: Code restructure failed: missing block: B:69:0x00c8, code lost:
    
        r0 = false;
     */
    /* JADX WARN: Removed duplicated region for block: B:48:0x0108  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public static boolean O(Context context) {
        boolean z;
        boolean z2;
        context.getClass();
        Boolean bool = i;
        if (bool != null) {
            return bool.booleanValue();
        }
        String str = Build.TAGS;
        boolean z3 = true;
        if (str == null || !o.J(str, "test-keys", false)) {
            String[] strArr = {"/system/app/Superuser.apk", "/sbin/su", "/system/bin/su", "/system/xbin/su", "/data/local/xbin/su", "/data/local/bin/su", "/system/sd/xbin/su", "/system/bin/failsafe/su", "/data/local/su", "/su/bin/su", "/su/bin", "/system/xbin/daemonsu"};
            int i2 = 0;
            while (true) {
                if (i2 < 12) {
                    String str2 = strArr[i2];
                    try {
                    } catch (RuntimeException unused) {
                        TraceLog.e("RootChecker", "Error when trying to check if root file " + str2 + " exists");
                    }
                    if (new File(str2).exists()) {
                        break;
                    }
                    i2++;
                } else {
                    Process process = null;
                    try {
                        try {
                            process = Runtime.getRuntime().exec(new String[]{"/system/xbin/which", "su"});
                            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
                            try {
                                if (bufferedReader.readLine() != null) {
                                    z = true;
                                } else {
                                    z = false;
                                }
                                bufferedReader.close();
                                process.destroy();
                            } finally {
                            }
                        } finally {
                            if (0 != 0) {
                                process.destroy();
                            }
                        }
                    } catch (IOException unused2) {
                        TraceLog.e("RootChecker", "SU isn't found on this Device.");
                    } catch (Throwable th) {
                        TraceLog.e("RootChecker", "Error when trying to check if SU exists. msg: " + th.getMessage());
                    }
                    if (!z) {
                        Context applicationContext = context.getApplicationContext();
                        applicationContext.getClass();
                        PackageManager packageManager = applicationContext.getPackageManager();
                        if (packageManager != null) {
                            String[] strArr2 = {"com.devadvance.rootcloak", "com.devadvance.rootcloakplus", "com.koushikdutta.superuser", "com.thirdparty.superuser", "eu.chainfire.supersu", "com.noshufou.android.su"};
                            for (int i3 = 0; i3 < 6; i3++) {
                                String str3 = strArr2[i3];
                                try {
                                    if (Build.VERSION.SDK_INT >= 33) {
                                        b.q(packageManager, str3, b.f());
                                    } else {
                                        packageManager.getPackageInfo(str3, 0);
                                    }
                                    z2 = true;
                                    if (!z2) {
                                        z3 = false;
                                    }
                                } catch (PackageManager.NameNotFoundException unused3) {
                                }
                            }
                        }
                        z2 = false;
                        if (!z2) {
                        }
                    }
                }
            }
        }
        Boolean valueOf = Boolean.valueOf(z3);
        i = valueOf;
        TraceLog.f(2, "RootChecker", "isRooted: " + valueOf);
        Boolean bool2 = i;
        bool2.getClass();
        return bool2.booleanValue();
    }

    public static boolean P(String str) {
        FileInputStream fileInputStream = null;
        try {
            try {
                fileInputStream = SentryFileInputStream.Factory.create(new FileInputStream(str), str);
                byte[] bArr = new byte[3];
                fileInputStream.read(bArr);
                return "GIF".equalsIgnoreCase(new String(bArr));
            } catch (IOException e2) {
                e2.printStackTrace();
                g(fileInputStream);
                return false;
            }
        } finally {
            g(fileInputStream);
        }
    }

    public static boolean Q(long j, long j2) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(j);
        Calendar calendar2 = Calendar.getInstance();
        calendar2.setTimeInMillis(j2);
        if (calendar.get(1) == calendar2.get(1) && calendar.get(6) == calendar2.get(6)) {
            return true;
        }
        return false;
    }

    public static boolean R(long j) {
        long L = L();
        long j2 = 0x05265c00 + L;
        if (j >= L && j < j2) {
            return true;
        }
        return false;
    }

    public static boolean S(long j) {
        if (j == 0) {
            return false;
        }
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date date = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return simpleDateFormat.format(new Date(j)).equals(simpleDateFormat.format(calendar.getTime()));
    }

    public static int T(int i2, int i3) {
        float red = Color.red(i2);
        float green = Color.green(i2);
        float blue = Color.blue(i2);
        float alpha = Color.alpha(i3) / 255.0f;
        float f2 = 1.0f - alpha;
        return Color.rgb((int) ((Color.red(i3) * alpha) + (red * f2)), (int) ((Color.green(i3) * alpha) + (green * f2)), (int) ((Color.blue(i3) * alpha) + (blue * f2)));
    }

    public static RuntimeException U(StackTraceElement[] stackTraceElementArr, Exception exc) {
        if (stackTraceElementArr == null) {
            RuntimeException runtimeException = new RuntimeException(exc);
            runtimeException.setStackTrace(exc.getStackTrace());
            return runtimeException;
        }
        RuntimeException runtimeException2 = new RuntimeException(exc.getMessage());
        StackTraceElement[] stackTraceElementArr2 = {new StackTraceElement("", "", "", 0)};
        StackTraceElement[] stackTrace = exc.getStackTrace();
        Object[] objArr = (Object[]) Array.newInstance(stackTraceElementArr.getClass().getComponentType(), stackTraceElementArr.length + 1 + stackTrace.length);
        System.arraycopy(stackTraceElementArr, 0, objArr, 0, stackTraceElementArr.length);
        System.arraycopy(stackTraceElementArr2, 0, objArr, stackTraceElementArr.length, 1);
        System.arraycopy(stackTrace, 0, objArr, stackTraceElementArr.length + 1, stackTrace.length);
        runtimeException2.setStackTrace((StackTraceElement[]) objArr);
        return runtimeException2;
    }

    public static String V(long j) {
        StringBuilder sb = new StringBuilder();
        long j2 = j / 60;
        long j3 = j % 60;
        sb.append(j2 / 10);
        sb.append(j2 % 10);
        sb.append(":");
        sb.append(j3 / 10);
        sb.append(j3 % 10);
        return sb.toString();
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Removed duplicated region for block: B:30:0x0042 A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /* JADX WARN: Type inference failed for: r1v0 */
    /* JADX WARN: Type inference failed for: r1v1, types: [java.io.FileInputStream] */
    /* JADX WARN: Type inference failed for: r1v2 */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public static String W(File file) {
        FileInputStream fileInputStream;
        ?? r1 = 0;
        if (file.exists()) {
            byte[] bArr = new byte[(int) file.length()];
            try {
                try {
                    fileInputStream = SentryFileInputStream.Factory.create(new FileInputStream(file), file);
                    try {
                        fileInputStream.read(bArr);
                        try {
                            fileInputStream.close();
                        } catch (IOException e2) {
                            e2.printStackTrace();
                        }
                        return new String(bArr);
                    } catch (IOException e3) {
                        e = e3;
                        e.printStackTrace();
                        if (fileInputStream != null) {
                            try {
                                fileInputStream.close();
                            } catch (IOException e4) {
                                e4.printStackTrace();
                            }
                        }
                        return null;
                    }
                } catch (Throwable th) {
                    th = th;
                    r1 = file;
                    if (r1 != 0) {
                        try {
                            r1.close();
                        } catch (IOException e5) {
                            e5.printStackTrace();
                        }
                    }
                    throw th;
                }
            } catch (IOException e6) {
                e = e6;
                fileInputStream = null;
            } catch (Throwable th2) {
                th = th2;
                if (r1 != 0) {
                }
                throw th;
            }
        }
        return null;
    }

    public static Bitmap X(Bitmap bitmap, int i2, float f2) {
        boolean z;
        if (bitmap == null) {
            return bitmap;
        }
        Matrix matrix = new Matrix();
        boolean z2 = true;
        if (i2 != 0) {
            matrix.setRotate(i2);
            z = true;
        } else {
            z = false;
        }
        i.g("BitmapUtils", "maxSideLen:" + f2 + "  b.getWidth():" + bitmap.getWidth() + " b.getHeight():" + bitmap.getHeight());
        float min = Math.min(f2 / ((float) bitmap.getWidth()), f2 / ((float) bitmap.getHeight()));
        StringBuilder sb = new StringBuilder("scale:");
        sb.append(min);
        i.g("BitmapUtils", sb.toString());
        if (min > 0.0f && min < 0.99d) {
            matrix.postScale(min, min);
        } else {
            z2 = z;
        }
        if (z2) {
            try {
                try {
                    Bitmap createBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                    if (createBitmap == null || bitmap == createBitmap) {
                        return bitmap;
                    }
                    bitmap.recycle();
                    return createBitmap;
                } catch (OutOfMemoryError unused) {
                    return bitmap;
                }
            } catch (OutOfMemoryError unused2) {
            }
        }
        return bitmap;
    }

    public static boolean Y(Bitmap bitmap, File file) {
        Bitmap.CompressFormat compressFormat = Bitmap.CompressFormat.JPEG;
        if (bitmap != null) {
            if (file.isFile()) {
                file.delete();
            }
            FileOutputStream fileOutputStream = null;
            try {
                try {
                    fileOutputStream = SentryFileOutputStream.Factory.create(new FileOutputStream(file), file);
                    boolean compress = bitmap.compress(compressFormat, 80, fileOutputStream);
                    if (fileOutputStream != null) {
                        try {
                            return compress;
                        } catch (IOException e2) {
                        }
                    }
                    return compress;
                } finally {
                    if (fileOutputStream != null) {
                        try {
                            fileOutputStream.close();
                        } catch (IOException e22) {
                            e22.printStackTrace();
                        }
                    }
                }
            } catch (FileNotFoundException e3) {
                i.C("BitmapUtils", e3.toString());
                if (fileOutputStream != null) {
                    try {
                        fileOutputStream.close();
                        return false;
                    } catch (IOException e4) {
                        e4.printStackTrace();
                        return false;
                    }
                }
                return false;
            }
        }
        return false;
    }

    /* JADX WARN: Removed duplicated region for block: B:10:0x0032  */
    /* JADX WARN: Removed duplicated region for block: B:43:0x00fd A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:7:0x002a  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public static void Z(Uri uri, ContentResolver contentResolver, File file) {
        Cursor cursor;
        String str;
        String[] strArr = {"_data"};
        int i2 = 0;
        Bitmap bitmap = null;
        try {
            cursor = contentResolver.query(uri, strArr, null, null, null);
        } catch (Throwable unused) {
            cursor = null;
        }
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                str = cursor.getString(cursor.getColumnIndex(strArr[0]));
                if (cursor != null) {
                    cursor.close();
                }
                if (str == null) {
                    File file2 = new File(str);
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inJustDecodeBounds = true;
                    try {
                        BitmapFactory.decodeFile(file2.getPath(), options);
                    } catch (Throwable th) {
                        i.i("BitmapUtils", th);
                    }
                    i.B("BitmapUtils", "src width: " + options.outWidth + ", height: " + options.outHeight);
                    options.inJustDecodeBounds = false;
                    options.inSampleSize = Math.max(options.outWidth / 800, options.outHeight / 800);
                    i.B("BitmapUtils", "inSampleSize: " + options.inSampleSize);
                    try {
                        bitmap = BitmapFactory.decodeFile(file2.getPath(), options);
                    } catch (Throwable th2) {
                        i.i("BitmapUtils", th2);
                    }
                    if (bitmap == null) {
                        i.C("BitmapUtils", "decodeFile failed");
                        return;
                    }
                    try {
                        int attributeInt = new ExifInterface(file2.getPath()).getAttributeInt("Orientation", -1);
                        if (attributeInt != -1) {
                            if (attributeInt != 3) {
                                if (attributeInt != 6) {
                                    if (attributeInt == 8) {
                                        i2 = 270;
                                    }
                                } else {
                                    i2 = 90;
                                }
                            } else {
                                i2 = 180;
                            }
                        }
                    } catch (Exception unused2) {
                    }
                    Bitmap X = X(bitmap, i2, 800);
                    Bitmap.CompressFormat compressFormat = Bitmap.CompressFormat.JPEG;
                    Y(X, file);
                    i.B("BitmapUtils", "dst width: " + X.getWidth() + ", height: " + X.getHeight() + ", size: " + file.length());
                    X.recycle();
                    return;
                }
                try {
                    FileDescriptor fileDescriptor = contentResolver.openFileDescriptor(uri, "r").getFileDescriptor();
                    float f2 = 800;
                    if (fileDescriptor != null) {
                        BitmapFactory.Options options2 = new BitmapFactory.Options();
                        options2.inJustDecodeBounds = true;
                        BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options2);
                        options2.inJustDecodeBounds = false;
                        options2.inPreferredConfig = Bitmap.Config.RGB_565;
                        options2.inSampleSize = (int) Math.max(options2.outWidth / f2, options2.outHeight / f2);
                        Bitmap decodeFileDescriptor = BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options2);
                        if (decodeFileDescriptor != null) {
                            bitmap = X(decodeFileDescriptor, 0, f2);
                        }
                    }
                    Bitmap.CompressFormat compressFormat2 = Bitmap.CompressFormat.JPEG;
                    Y(bitmap, file);
                    return;
                } catch (Throwable unused3) {
                    return;
                }
            }
        }
        str = null;
        if (cursor != null) {
        }
        if (str == null) {
        }
    }

    public static int a(int i2, int i3) {
        return (i2 & 16777215) | (i3 << 24);
    }

    public static void a0(Bitmap bitmap, String str) {
        File file = new File(str);
        try {
            file.createNewFile();
            try {
                FileOutputStream create = SentryFileOutputStream.Factory.create(new FileOutputStream(file), file);
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, create);
                try {
                    create.flush();
                    try {
                        create.close();
                    } catch (IOException e2) {
                        e2.printStackTrace();
                        i.i("BitmapUtils", e2);
                        throw e2;
                    }
                } catch (IOException e3) {
                    e3.printStackTrace();
                    i.i("BitmapUtils", e3);
                    throw e3;
                }
            } catch (FileNotFoundException e4) {
                e4.printStackTrace();
                i.i("BitmapUtils", e4);
                throw e4;
            }
        } catch (IOException e5) {
            e5.printStackTrace();
            i.i("BitmapUtils", e5);
            throw e5;
        }
    }

    /* JADX WARN: Removed duplicated region for block: B:35:0x0067 A[Catch: IOException -> 0x0063, TRY_LEAVE, TryCatch #2 {IOException -> 0x0063, blocks: (B:42:0x005f, B:35:0x0067), top: B:41:0x005f }] */
    /* JADX WARN: Removed duplicated region for block: B:41:0x005f A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public static void b(String str, String str2) {
        FileOutputStream fileOutputStream;
        File file;
        ByteArrayInputStream byteArrayInputStream;
        ByteArrayInputStream byteArrayInputStream2 = null;
        r0 = null;
        FileOutputStream fileOutputStream2 = null;
        byteArrayInputStream2 = null;
        try {
            try {
                file = new File(str2);
                if (!file.exists()) {
                    file.createNewFile();
                }
                byteArrayInputStream = new ByteArrayInputStream(Base64.decode(str, 0));
            } catch (IOException e2) {
                e2.printStackTrace();
                return;
            }
        } catch (IOException e3) {
            e = e3;
            fileOutputStream = null;
        } catch (Throwable th) {
            th = th;
            fileOutputStream = null;
        }
        try {
            byte[] bArr = new byte[4096];
            fileOutputStream2 = SentryFileOutputStream.Factory.create(new FileOutputStream(file), file);
            while (true) {
                int read = byteArrayInputStream.read(bArr);
                if (read == -1) {
                    break;
                } else {
                    fileOutputStream2.write(bArr, 0, read);
                }
            }
            byteArrayInputStream.close();
            if (fileOutputStream2 != null) {
                fileOutputStream2.close();
            }
        } catch (IOException e4) {
            e = e4;
            fileOutputStream = fileOutputStream2;
            byteArrayInputStream2 = byteArrayInputStream;
            try {
                e.printStackTrace();
                if (byteArrayInputStream2 != null) {
                    byteArrayInputStream2.close();
                }
                if (fileOutputStream != null) {
                    fileOutputStream.close();
                }
            } catch (Throwable th2) {
                th = th2;
                if (byteArrayInputStream2 != null) {
                    try {
                        byteArrayInputStream2.close();
                    } catch (IOException e5) {
                        e5.printStackTrace();
                        throw th;
                    }
                }
                if (fileOutputStream != null) {
                    fileOutputStream.close();
                }
                throw th;
            }
        } catch (Throwable th3) {
            th = th3;
            fileOutputStream = fileOutputStream2;
            byteArrayInputStream2 = byteArrayInputStream;
            if (byteArrayInputStream2 != null) {
            }
            if (fileOutputStream != null) {
            }
            throw th;
        }
    }

    public static void b0(View view) {
        view.setFocusable(true);
        view.setFocusableInTouchMode(true);
        view.requestFocus();
        ((InputMethodManager) view.getContext().getSystemService("input_method")).showSoftInput(view, 1);
    }

    public static String c(byte[] bArr) {
        char[] cArr = new char[32];
        int i2 = 0;
        for (int i3 = 0; i3 < 16; i3++) {
            byte b2 = bArr[i3];
            int i4 = i2 + 1;
            char[] cArr2 = a;
            cArr[i2] = cArr2[(b2 >>> 4) & 15];
            i2 += 2;
            cArr[i4] = cArr2[b2 & 15];
        }
        return new String(cArr);
    }

    public static boolean c0(Context context, String str) {
        Intent intent = new Intent("android.intent.action.VIEW", Uri.parse("market://details?id=" + str));
        intent.addFlags(0x10000000);
        try {
            context.startActivity(intent);
            return true;
        } catch (Exception e2) {
            e2.printStackTrace();
            return false;
        }
    }

    public static boolean d() {
        return TextUtils.equals(Build.MANUFACTURER.toLowerCase(Locale.getDefault()), "honor");
    }

    public static boolean e() {
        boolean z;
        if (e == null) {
            String J = J("ro.build.display.id");
            if (TextUtils.isEmpty(J)) {
                e = Boolean.FALSE;
            } else {
                if (!J.contains("flyme") && !J.toLowerCase().contains("flyme")) {
                    z = false;
                } else {
                    z = true;
                }
                e = Boolean.valueOf(z);
            }
        }
        return e.booleanValue();
    }

    public static boolean f() {
        if (d == null) {
            d = Boolean.valueOf(!TextUtils.isEmpty(J("ro.miui.ui.version.name")));
        }
        return d.booleanValue();
    }

    public static void g(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e2) {
                e2.printStackTrace();
            }
        }
    }

    public static void h(Context context) {
        try {
            Intent intent = new Intent(Settings.class.getDeclaredField("ACTION_MANAGE_OVERLAY_PERMISSION").get(null).toString());
            intent.setFlags(0x10000000);
            intent.setData(Uri.parse("package:" + context.getPackageName()));
            context.startActivity(intent);
        } catch (Exception e2) {
            e2.printStackTrace();
        }
    }

    public static void i(AppCompatActivity appCompatActivity) {
        try {
            Method declaredMethod = Activity.class.getDeclaredMethod("getActivityOptions", null);
            declaredMethod.setAccessible(true);
            Object invoke = declaredMethod.invoke(appCompatActivity, null);
            Class<?> cls = null;
            for (Class<?> cls2 : Activity.class.getDeclaredClasses()) {
                if (cls2.getSimpleName().contains("TranslucentConversionListener")) {
                    cls = cls2;
                }
            }
            Method declaredMethod2 = Activity.class.getDeclaredMethod("convertToTranslucent", cls, ActivityOptions.class);
            declaredMethod2.setAccessible(true);
            declaredMethod2.invoke(appCompatActivity, null, invoke);
        } catch (Throwable unused) {
        }
    }

    /* JADX WARN: Can't wrap try/catch for region: R(10:3|(2:4|5)|(4:(10:10|11|(1:13)(1:49)|14|15|16|17|18|19|(2:20|(1:22)(3:23|24|25)))|18|19|(3:20|(0)(0)|22))|50|11|(0)(0)|14|15|16|17) */
    /* JADX WARN: Code restructure failed: missing block: B:42:0x008f, code lost:
    
        r6 = move-exception;
     */
    /* JADX WARN: Code restructure failed: missing block: B:43:0x0090, code lost:
    
        r6 = r5;
        r5 = r6;
        r7 = null;
     */
    /* JADX WARN: Code restructure failed: missing block: B:44:0x0089, code lost:
    
        r6 = move-exception;
     */
    /* JADX WARN: Code restructure failed: missing block: B:45:0x008a, code lost:
    
        r6 = r5;
        r5 = r6;
        r7 = null;
     */
    /* JADX WARN: Code restructure failed: missing block: B:46:0x0086, code lost:
    
        r6 = th;
     */
    /* JADX WARN: Code restructure failed: missing block: B:47:0x0087, code lost:
    
        r7 = null;
     */
    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Removed duplicated region for block: B:13:0x0048  */
    /* JADX WARN: Removed duplicated region for block: B:22:0x006c A[Catch: all -> 0x0071, IOException -> 0x0075, FileNotFoundException -> 0x007a, LOOP:0: B:20:0x0065->B:22:0x006c, LOOP_END, TRY_LEAVE, TryCatch #5 {FileNotFoundException -> 0x007a, IOException -> 0x0075, all -> 0x0071, blocks: (B:19:0x0063, B:20:0x0065, B:22:0x006c), top: B:18:0x0063 }] */
    /* JADX WARN: Removed duplicated region for block: B:23:0x007f A[SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:49:0x0049 A[Catch: all -> 0x0030, IOException -> 0x0034, FileNotFoundException -> 0x0039, TryCatch #6 {FileNotFoundException -> 0x0039, IOException -> 0x0034, all -> 0x0030, blocks: (B:5:0x0007, B:7:0x000d, B:10:0x001c, B:11:0x0042, B:14:0x004f, B:49:0x0049, B:50:0x003e), top: B:4:0x0007 }] */
    /* JADX WARN: Type inference failed for: r6v0, types: [java.io.File] */
    /* JADX WARN: Type inference failed for: r6v21 */
    /* JADX WARN: Type inference failed for: r6v22 */
    /* JADX WARN: Type inference failed for: r6v3 */
    /* JADX WARN: Type inference failed for: r6v5, types: [java.io.Closeable] */
    /* JADX WARN: Type inference failed for: r7v0, types: [java.lang.CharSequence, java.lang.String] */
    /* JADX WARN: Type inference failed for: r7v22 */
    /* JADX WARN: Type inference failed for: r7v23 */
    /* JADX WARN: Type inference failed for: r7v4 */
    /* JADX WARN: Type inference failed for: r7v7, types: [java.io.Closeable] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public static File j(File file, File file2, String str) {
        FileOutputStream fileOutputStream;
        FileInputStream fileInputStream;
        FileOutputStream fileOutputStream2;
        FileInputStream fileInputStream2;
        FileOutputStream fileOutputStream3;
        String name;
        FileInputStream create;
        FileOutputStream fileOutputStream4;
        byte[] bArr;
        int read;
        if (!file.isDirectory()) {
            FileInputStream fileInputStream3 = null;
            try {
                try {
                } catch (Throwable th) {
                    th = th;
                    fileInputStream3 = file2;
                    fileOutputStream = str;
                }
            } catch (FileNotFoundException e2) {
                e = e2;
                fileInputStream2 = null;
                fileOutputStream3 = null;
            } catch (IOException e3) {
                e = e3;
                fileInputStream = null;
                fileOutputStream2 = null;
            } catch (Throwable th2) {
                th = th2;
                fileOutputStream = null;
            }
            try {
                if (!TextUtils.isEmpty(str) && !file.getName().toLowerCase().endsWith(str)) {
                    name = file.getName() + ((String) str);
                    File file3 = !file2.isFile() ? file2 : new File((File) file2, name);
                    create = SentryFileInputStream.Factory.create(new FileInputStream(file), file);
                    fileOutputStream4 = SentryFileOutputStream.Factory.create(new FileOutputStream(file3), file3);
                    bArr = new byte[8192];
                    while (true) {
                        read = create.read(bArr);
                        if (read == -1) {
                            fileOutputStream4.write(bArr, 0, read);
                        } else {
                            g(create);
                            g(fileOutputStream4);
                            return file3;
                        }
                    }
                }
                bArr = new byte[8192];
                while (true) {
                    read = create.read(bArr);
                    if (read == -1) {
                    }
                    fileOutputStream4.write(bArr, 0, read);
                }
            } catch (FileNotFoundException e4) {
                fileInputStream2 = create;
                e = e4;
                fileOutputStream3 = fileOutputStream4;
                e.printStackTrace();
                file2 = fileInputStream2;
                str = fileOutputStream3;
                g(file2);
                g(str);
                return null;
            } catch (IOException e5) {
                fileInputStream = create;
                e = e5;
                fileOutputStream2 = fileOutputStream4;
                e.printStackTrace();
                file2 = fileInputStream;
                str = fileOutputStream2;
                g(file2);
                g(str);
                return null;
            } catch (Throwable th3) {
                th = th3;
                fileInputStream3 = create;
                th = th;
                fileOutputStream = fileOutputStream4;
                g(fileInputStream3);
                g(fileOutputStream);
                throw th;
            }
            name = file.getName();
            if (!file2.isFile()) {
            }
            create = SentryFileInputStream.Factory.create(new FileInputStream(file), file);
            fileOutputStream4 = SentryFileOutputStream.Factory.create(new FileOutputStream(file3), file3);
        } else {
            a.l("src must be file!");
            return null;
        }
    }

    /* JADX WARN: Code restructure failed: missing block: B:16:0x002f, code lost:
    
        if (r0 == null) goto L31;
     */
    /* JADX WARN: Code restructure failed: missing block: B:17:0x0028, code lost:
    
        r0.close();
     */
    /* JADX WARN: Code restructure failed: missing block: B:20:0x0026, code lost:
    
        if (r0 == null) goto L31;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public static void k(Bitmap bitmap, File file) {
        FileOutputStream fileOutputStream = null;
        try {
            try {
                try {
                    fileOutputStream = SentryFileOutputStream.Factory.create(new FileOutputStream(file), file);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
                    fileOutputStream.flush();
                    fileOutputStream.close();
                } catch (IOException e2) {
                    e2.printStackTrace();
                }
            } catch (FileNotFoundException e3) {
                e3.printStackTrace();
            } catch (IOException e4) {
                e4.printStackTrace();
            }
        } catch (Throwable th) {
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (IOException e5) {
                    e5.printStackTrace();
                }
            }
            throw th;
        }
    }

    public static void l(File file) {
        File parentFile = file.getParentFile();
        if (!parentFile.exists()) {
            parentFile.mkdirs();
        }
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException unused) {
            }
        }
    }

    public static byte[] m(String str, String str2, byte[] bArr) {
        SecretKeySpec secretKeySpec = new SecretKeySpec(str.getBytes(), "AES");
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(2, secretKeySpec, new IvParameterSpec(str2.getBytes()));
        return cipher.doFinal(Base64.decode(bArr, 2));
    }

    public static boolean n(File file) {
        if (file.isDirectory()) {
            String[] list = file.list();
            if (list == null) {
                return true;
            }
            for (String str : list) {
                if (!n(new File(file, str))) {
                    return false;
                }
            }
        }
        return file.delete();
    }

    public static boolean o(File file) {
        if (file != null && file.isDirectory()) {
            String[] list = file.list();
            if (list != null) {
                for (String str : list) {
                    if (!o(new File(file, str))) {
                        return false;
                    }
                }
            }
        } else if (file != null && file.getName().endsWith("classes.dex")) {
            return true;
        }
        if (file == null || file.delete()) {
            return true;
        }
        return false;
    }

    public static boolean p(File file) {
        if (file.exists()) {
            if (file.isDirectory()) {
                File[] listFiles = file.listFiles();
                if (listFiles == null) {
                    return file.delete();
                }
                for (File file2 : listFiles) {
                    if (!p(file2)) {
                        return false;
                    }
                }
                return file.delete();
            }
            return file.delete();
        }
        return true;
    }

    public static String q(String str) {
        try {
            Charset charset = StandardCharsets.UTF_8;
            SecretKeySpec secretKeySpec = new SecretKeySpec("123456ABCD!@#$%^".getBytes(charset), "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(1, secretKeySpec, new IvParameterSpec("123456ABCD!@#$%^".getBytes(charset)));
            return new String(Base64.encode(cipher.doFinal(str.getBytes(charset)), 0), charset).replaceAll("[\\s*\t\n\r]", "");
        } catch (Exception unused) {
            return null;
        }
    }

    public static String r(String str) {
        Charset charset = StandardCharsets.UTF_8;
        SecretKeySpec secretKeySpec = new SecretKeySpec("WESHINEABC!@#$%^".getBytes(charset), "AES");
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(1, secretKeySpec, new IvParameterSpec("WESHINEABC!@#$%^".getBytes(charset)));
        return new String(Base64.encode(cipher.doFinal(str.getBytes(charset)), 0), charset).replaceAll("[\\s*\t\n\r]", "");
    }

    public static String s(String str) {
        SecretKeySpec secretKeySpec = new SecretKeySpec("123456ABCD!@#$%^".getBytes(), "AES");
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(1, secretKeySpec, new IvParameterSpec("123456ABCD!@#$%^".getBytes()));
            return Base64.encodeToString(cipher.doFinal(str.getBytes(StandardCharsets.UTF_8)), 2);
        } catch (Exception e2) {
            e2.printStackTrace();
            return null;
        }
    }

    public static final Size t(Context context) {
        int i2;
        int i3;
        context.getClass();
        Object systemService = context.getSystemService("window");
        systemService.getClass();
        WindowManager windowManager = (WindowManager) systemService;
        if (Build.VERSION.SDK_INT >= 30) {
            WindowMetrics v = androidx.media3.exoplayer.source.mediaparser.a.v(windowManager);
            v.getClass();
            WindowInsets d2 = org.reactivestreams.a.d(v);
            d2.getClass();
            Insets e2 = l.e(d2, l.t() | l.D());
            e2.getClass();
            int b2 = android.support.v4.media.session.a.b(e2) + android.support.v4.media.session.a.u(e2);
            int w = android.support.v4.media.session.a.w(e2) + android.support.v4.media.session.a.n(e2);
            Rect n = androidx.media3.exoplayer.source.mediaparser.a.n(v);
            n.getClass();
            i2 = n.width() - b2;
            i3 = n.height() - w;
        } else {
            Point point = new Point();
            Display defaultDisplay = windowManager.getDefaultDisplay();
            if (defaultDisplay != null) {
                defaultDisplay.getSize(point);
            }
            i2 = point.x;
            i3 = point.y;
        }
        return new Size(i2, i3);
    }

    public static Signature u(Context context) {
        Signature[] signatureArr;
        context.getClass();
        int i2 = Build.VERSION.SDK_INT;
        if (i2 >= 28) {
            if (i2 >= 33) {
                signatureArr = t.z(android.support.v4.media.session.b.h(b.d(context.getPackageManager(), context.getPackageName(), b.v())));
            } else {
                signatureArr = t.z(android.support.v4.media.session.b.h(context.getPackageManager().getPackageInfo(context.getPackageName(), 0x08000000)));
            }
            signatureArr.getClass();
        } else {
            signatureArr = context.getPackageManager().getPackageInfo(context.getPackageName(), 64).signatures;
            signatureArr.getClass();
        }
        return (Signature) r.K(signatureArr);
    }

    public static String v(byte[] bArr) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            messageDigest.update(bArr, 0, bArr.length);
            return c(messageDigest.digest());
        } catch (Exception e2) {
            e2.printStackTrace();
            return null;
        }
    }

    public static String w(Context context, String str) {
        File cacheDir = context.getCacheDir();
        if (cacheDir == null && Environment.getExternalStorageState().equals("mounted")) {
            cacheDir = context.getExternalCacheDir();
        }
        if (cacheDir == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(cacheDir.getAbsolutePath());
        String str2 = File.separator;
        File file = new File(c.p(sb, str2, str, str2));
        if (!file.exists()) {
            file.mkdirs();
        }
        return file.getAbsolutePath();
    }

    public static int x(float f2, int i2) {
        return (Math.min(255, Math.max(0, (int) (f2 * 255.0f))) << 24) + (i2 & 16777215);
    }

    /* JADX WARN: Code restructure failed: missing block: B:36:0x0094, code lost:
    
        if (r0 == null) goto L37;
     */
    /* JADX WARN: Removed duplicated region for block: B:19:0x004e  */
    /* JADX WARN: Removed duplicated region for block: B:21:0x0054  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public static String y() {
        String str;
        String str2;
        BufferedReader bufferedReader;
        Object invoke;
        if (!TextUtils.isEmpty(c)) {
            return c;
        }
        String str3 = null;
        if (Build.VERSION.SDK_INT >= 28) {
            str = f.j();
        } else {
            str = null;
        }
        c = str;
        if (!TextUtils.isEmpty(str)) {
            String str4 = c;
            str4.getClass();
            return str4;
        }
        try {
            Method declaredMethod = Class.forName("android.app.ActivityThread").getDeclaredMethod("currentProcessName", null);
            declaredMethod.setAccessible(true);
            invoke = declaredMethod.invoke(null, null);
        } catch (Throwable th) {
            th.printStackTrace();
        }
        if (invoke instanceof String) {
            str2 = (String) invoke;
            c = str2;
            if (TextUtils.isEmpty(str2)) {
                String str5 = c;
                str5.getClass();
                return str5;
            }
            try {
                bufferedReader = new BufferedReader(new SentryFileReader(new File("/proc/" + Process.myPid() + "/cmdline")));
                try {
                    String readLine = bufferedReader.readLine();
                    readLine.getClass();
                    str3 = o.v0(readLine).toString();
                } catch (Throwable th2) {
                    th = th2;
                    try {
                        th.printStackTrace();
                    } catch (Throwable th3) {
                        if (bufferedReader != null) {
                            try {
                                bufferedReader.close();
                            } catch (Exception unused) {
                            }
                        }
                        throw th3;
                    }
                }
            } catch (Throwable th4) {
                th = th4;
                bufferedReader = null;
            }
            try {
                bufferedReader.close();
            } catch (Exception unused2) {
                c = str3;
                return str3;
            }
        }
        str2 = null;
        c = str2;
        if (TextUtils.isEmpty(str2)) {
        }
    }

    public static String z(String str) {
        if (l.e(str)) {
            return str;
        }
        int lastIndexOf = str.lastIndexOf(".");
        int lastIndexOf2 = str.lastIndexOf(File.separator);
        if (lastIndexOf == -1 || lastIndexOf2 >= lastIndexOf) {
            return "";
        }
        return str.substring(lastIndexOf + 1);
    }
}
