package me.lengyu.qedge.hook.kk;

import android.content.ContentResolver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.List;
import me.lengyu.qedge.hook.annotation.HookItemAnnotation;
import me.lengyu.qedge.hook.base.BaseApiHookItem;
import me.lengyu.qedge.utils.ReflectUtils;
import me.lengyu.qedge.utils.LogUtils;
import me.lengyu.qedge.utils.HookUtils;
import me.lengyu.qedge.utils.HostInfo;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindClass;
import org.luckypray.dexkit.query.matchers.ClassMatcher;
import org.luckypray.dexkit.result.ClassData;
import org.luckypray.dexkit.wrap.DexClass;
/**
 * @Author 冷雨
 * @Description KK键盘 去除广告，破解Vip，阻止应用闪退
 */
public class KKHook {

    private static boolean initialized = false;

    public static void loadHook() {
        if (initialized) return;
        initialized = true;

        hookVip();
        hookVipInfo();
        hookExit();
        hookAvatar();
    }

    public static void hookVip() {
        try {
            ClassLoader classLoader = ReflectUtils.hostClassLoader;
            if (classLoader == null) {
                LogUtils.e("hookVip", "getClassLoader error");
                return;
            }
            Class<?> vipClass = classLoader.loadClass("im.weshine.business.provider.UserPreference");

            Method vipMethod = null;
            Method[] methods = vipClass.getDeclaredMethods();
            for (Method method : methods) {
                if (method.getName().equals("isVip")) {
                    Class<?>[] paramTypes = method.getParameterTypes();
                    if (paramTypes.length == 0) {
                        vipMethod = method;
                        break;
                    }
                }
            }
            if (vipMethod != null) {
                HookUtils.hookBefore(vipMethod, param -> {
                    try {
                        param.setResult(true);
                    } catch (Throwable e) {
                        LogUtils.e("hookVip", "isVip callback error: " + e.getMessage());
                    }
                });
            } else {
                LogUtils.e("hookVip", "isVip method not found");
            }

        } catch (Throwable e) {
            LogUtils.e("hookVip", "loadHook error: " + e.getMessage());
            LogUtils.e(e);
        }
    }

    public static void hookVipInfo() {
        try {
            ClassLoader classLoader = ReflectUtils.hostClassLoader;
            if (classLoader == null) {
                LogUtils.e("hookVipInfo", "getClassLoader error");
                return;
            }
            Class<?> vipClass = classLoader.loadClass("im.weshine.business.database.model.VipInfo");

            Method vipMethod = null;
            Method adMethod = null;
            Method[] methods = vipClass.getDeclaredMethods();
            for (Method method : methods) {
                if (method.getName().equals("getUserType")) {
                    Class<?>[] paramTypes = method.getParameterTypes();
                    if (paramTypes.length == 0) {
                        vipMethod = method;
                    }
                }
                if (method.getName().equals("isAdFree")) {
                    Class<?>[] paramTypes = method.getParameterTypes();
                    if (paramTypes.length == 0) {
                        adMethod = method;
                        break;
                    }
                }
            }
            if (vipMethod != null) {
                HookUtils.hookBefore(vipMethod, param -> {
                    try {
                        param.setResult(5);
                    } catch (Throwable e) {
                        LogUtils.e("hookVipInfo", "getUserType callback error: " + e.getMessage());
                    }
                });
            } else {
                LogUtils.e("hookVipInfo", "getUserType method not found");
            }

            if (adMethod != null) {
                HookUtils.hookBefore(adMethod, param -> {
                    try {
                        param.setResult(true);
                    } catch (Throwable e) {
                        LogUtils.e("hookVipInfo", "isAdFree callback error: " + e.getMessage());
                    }
                });
            } else {
                LogUtils.e("hookVipInfo", "isAdFree method not found");
            }
        } catch (Throwable e) {
            LogUtils.e("hookVipInfo", "loadHook error: " + e.getMessage());
            LogUtils.e(e);
        }
    }

    public static void hookExit() {
        try {
            hookUncaughtException();
            hookSystemExit();
            hookProcessKill();
            hookActivityManagerKill();

            String sourceDir = HostInfo.getHostContext().getApplicationInfo().sourceDir;
            if (sourceDir == null) {
                LogUtils.e("hookExit", "sourceDir is null");
                return;
            }

            System.loadLibrary("dexkit");
            DexKitBridge bridge = DexKitBridge.create(sourceDir);
            if (bridge == null) {
                LogUtils.e("hookExit", "DexKitBridge.create returned null");
                return;
            }

            try {
                hookMethodByString(bridge, "do process kill");
                hookMethodByString(bridge, "kill system ");
                hookMethodByString(bridge, "shut down process");
            } finally {
                bridge.close();
            }

        } catch (Throwable e) {
            LogUtils.e("hookExit", "loadHook error: " + e.getMessage());
            LogUtils.e(e);
        }
    }

    private static void hookUncaughtException() {
        try {
            final Thread.UncaughtExceptionHandler defaultHandler =
                Thread.getDefaultUncaughtExceptionHandler();
            Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
                LogUtils.e("hookExit", "Uncaught exception in " + thread.getName() +
                    ": " + throwable.getMessage());
                LogUtils.e(throwable);
            });
        } catch (Throwable e) {
            LogUtils.e("hookExit", "hookUncaughtException error: " + e.getMessage());
        }
    }

    private static void hookSystemExit() {
        try {
            Method exitMethod = System.class.getDeclaredMethod("exit", int.class);
            HookUtils.hookBefore(exitMethod, param -> {
                try {
                    param.setResult(null);
                } catch (Throwable e) {
                    LogUtils.e("hookExit", "System.exit hook error: " + e.getMessage());
                }
            });

            Method haltMethod = Runtime.class.getDeclaredMethod("halt", int.class);
            HookUtils.hookBefore(haltMethod, param -> {
                try {
                    param.setResult(null);
                } catch (Throwable e) {
                    LogUtils.e("hookExit", "Runtime.halt hook error: " + e.getMessage());
                }
            });
        } catch (Throwable e) {
            LogUtils.e("hookExit", "hookSystemExit error: " + e.getMessage());
        }
    }

    private static void hookProcessKill() {
        try {
            Class<?> processClass = Class.forName("android.os.Process");
            Method killProcessMethod = processClass.getDeclaredMethod("killProcess", int.class);
            HookUtils.hookBefore(killProcessMethod, param -> {
                try {
                    int pid = (int) param.args[0];
                    int myPid = android.os.Process.myPid();
                    if (pid == myPid) {
                        param.setResult(null);
                    }
                } catch (Throwable e) {
                    LogUtils.e("hookExit", "Process.killProcess hook error: " + e.getMessage());
                }
            });

            Method killProcessQuietlyMethod = null;
            try {
                killProcessQuietlyMethod = processClass.getDeclaredMethod("killProcessQuietly", int.class);
            } catch (NoSuchMethodException ignored) {}
            if (killProcessQuietlyMethod != null) {
                HookUtils.hookBefore(killProcessQuietlyMethod, param -> {
                    try {
                        int pid = (int) param.args[0];
                        int myPid = android.os.Process.myPid();
                        if (pid == myPid) {
                            param.setResult(null);
                        }
                    } catch (Throwable e) {
                        LogUtils.e("hookExit", "Process.killProcessQuietly hook error: " + e.getMessage());
                    }
                });
            }
        } catch (Throwable e) {
            LogUtils.e("hookExit", "hookProcessKill error: " + e.getMessage());
        }
    }

    private static void hookActivityManagerKill() {
        try {
            Class<?> amClass = Class.forName("android.app.ActivityManager");
            String packageName = HostInfo.getHostContext().getPackageName();

            try {
                Method forceStopMethod = amClass.getDeclaredMethod("forceStopPackage", String.class);
                HookUtils.hookBefore(forceStopMethod, param -> {
                    try {
                        String pkg = (String) param.args[0];
                        if (packageName.equals(pkg)) {
                            param.setResult(null);
                        }
                    } catch (Throwable e) {
                        LogUtils.e("hookExit", "forceStopPackage hook error: " + e.getMessage());
                    }
                });
            } catch (NoSuchMethodException ignored) {}

            try {
                Method killBackgroundMethod = amClass.getDeclaredMethod(
                    "killBackgroundProcesses", String.class);
                HookUtils.hookBefore(killBackgroundMethod, param -> {
                    try {
                        String pkg = (String) param.args[0];
                        if (packageName.equals(pkg)) {
                            param.setResult(null);
                        }
                    } catch (Throwable e) {
                        LogUtils.e("hookExit", "killBackgroundProcesses hook error: " + e.getMessage());
                    }
                });
            } catch (NoSuchMethodException ignored) {}
        } catch (Throwable e) {
            LogUtils.e("hookExit", "hookActivityManagerKill error: " + e.getMessage());
        }
    }

    private static void hookMethodByString(DexKitBridge bridge, String keyword) {
        try {
            FindClass findClass = new FindClass();
            findClass.matcher(new ClassMatcher()
                .usingStrings(keyword)
            );

            List<ClassData> classes = bridge.findClass(findClass);
            if (classes.isEmpty()) {
                LogUtils.e("hookExit", "No class found for keyword: " + keyword);
                return;
            }

            ClassData classData = classes.get(0);

            Class<?> exitClass = new DexClass(classData.getDescriptor())
                .getInstance(ReflectUtils.hostClassLoader);

            Method[] methods = exitClass.getDeclaredMethods();
            for (Method method : methods) {
                if (method.getReturnType() != void.class) continue;
                if (!java.lang.reflect.Modifier.isStatic(method.getModifiers())) continue;

                Class<?>[] paramTypes = method.getParameterTypes();
                if (paramTypes.length == 0 ||
                    (paramTypes.length == 1 && paramTypes[0] == int.class)) {
                    HookUtils.hookBefore(method, param -> {
                        try {
                            param.setResult(null);
                        } catch (Throwable e) {
                            LogUtils.e("hookExit", "callback error: " + e.getMessage());
                        }
                    });
                }
            }
        } catch (Throwable e) {
            LogUtils.e("hookExit", "hookMethodByString error for '" + keyword + "': " + e.getMessage());
        }
    }

    /**
     * 头像上传无损压缩：用 DexKit 定位 BitmapUtils(zl.g) 的 Z(Uri,ContentResolver,File)
     * 整体替换其内部逻辑，去掉 800px 降采样 / RGB_565 / JPEG 有损编码，
     * 改为按原始分辨率解码后用 PNG(100) 无损写入，直接跳过原方法。
     */
    private static void hookAvatar() {
        try {
            String sourceDir = HostInfo.getHostContext().getApplicationInfo().sourceDir;
            System.loadLibrary("dexkit");
            DexKitBridge bridge = DexKitBridge.create(sourceDir);
            if (bridge == null) {
                LogUtils.e("hookAvatar", "DexKitBridge.create returned null");
                return;
            }

            try {
                FindClass findClass = new FindClass();
                findClass.matcher(new ClassMatcher()
                    .usingStrings("BitmapUtils")
                );
                List<ClassData> classes = bridge.findClass(findClass);
                if (classes.isEmpty()) {
                    LogUtils.e("hookAvatar", "BitmapUtils class not found");
                    return;
                }

                Class<?> bitmapUtils = null;
                for (ClassData cd : classes) {
                    // 优先取简单类名为 g（对应反编译出的 zl.g）
                    String desc = cd.getDescriptor();
                    String simple = desc.endsWith(";")
                        ? desc.substring(desc.lastIndexOf('/') + 1, desc.length() - 1)
                        : desc;
                    if ("g".equals(simple)) {
                        bitmapUtils = new DexClass(desc).getInstance(ReflectUtils.hostClassLoader);
                        break;
                    }
                }
                if (bitmapUtils == null && !classes.isEmpty()) {
                    bitmapUtils = new DexClass(classes.get(0).getDescriptor())
                        .getInstance(ReflectUtils.hostClassLoader);
                }
                if (bitmapUtils == null) {
                    LogUtils.e("hookAvatar", "load BitmapUtils class failed");
                    return;
                }

                Method zMethod = null;
                for (Method method : bitmapUtils.getDeclaredMethods()) {
                    if ("Z".equals(method.getName()) && method.getParameterCount() == 3) {
                        zMethod = method;
                        break;
                    }
                }
                if (zMethod == null) {
                    LogUtils.e("hookAvatar", "Z(Uri,ContentResolver,File) not found");
                    return;
                }
                zMethod.setAccessible(true);
                final Method finalZ = zMethod;

                // g.Z 是 void 方法，hookBefore + setResult 无法阻止原方法体执行，
                // 必须用 XC_MethodReplacement 完整替换
                XposedBridge.hookMethod(zMethod, new XC_MethodReplacement() {
                    @Override
                    protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                        // 无损写入成功 -> 返回 null，完全替换原方法（不执行 800px+JPEG）
                        // 写入失败 -> 调用原始方法放行
                        if (losslessPngUpload(param)) {
                            return null;
                        }
                        return XposedBridge.invokeOriginalMethod(finalZ, param.thisObject, param.args);
                    }
                });
                LogUtils.i("hookAvatar", "hooked avatar upload -> lossless PNG");
            } finally {
                bridge.close();
            }
        } catch (Throwable e) {
            LogUtils.e("hookAvatar", "hookAvatar error: " + e.getMessage());
            LogUtils.e(e);
        }
    }

    /** 按原分辨率解码后 PNG 无损写入目标文件；true=已替换，false=交给原方法 */
    private static boolean losslessPngUpload(XC_MethodHook.MethodHookParam param) {
        try {
            Uri uri = (Uri) param.args[0];
            ContentResolver contentResolver = (ContentResolver) param.args[1];
            File out = (File) param.args[2];
            if (uri == null || contentResolver == null || out == null) {
                return false; // 参数缺失，走原逻辑
            }

            Bitmap bitmap = decodeOriginal(uri, contentResolver);
            if (bitmap == null) {
                LogUtils.e("hookAvatar", "decode original failed, fallback to original logic");
                return false; // 解码失败，放行原方法
            }

            // 按原始分辨率无损写入 PNG
            if (out.isFile()) {
                out.delete();
            }
            try (FileOutputStream fos = new FileOutputStream(out)) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                fos.flush();
            } finally {
                bitmap.recycle();
            }
            return true;
        } catch (Throwable e) {
            LogUtils.e("hookAvatar", "losslessPngUpload error: " + e.getMessage());
            LogUtils.e(e);
            return false;
        }
    }

    /** 按原分辨率解码，parse EXIF 旋转，保持 ARGB_8888（不降采样、不用 RGB_565） */
    private static Bitmap decodeOriginal(Uri uri, ContentResolver contentResolver) {
        String path = null;
        try {
            Cursor cursor = contentResolver.query(uri, new String[]{"_data"}, null, null, null);
            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        int idx = cursor.getColumnIndex("_data");
                        if (idx >= 0) {
                            path = cursor.getString(idx);
                        }
                    }
                } finally {
                    cursor.close();
                }
            }
        } catch (Throwable ignored) {
        }

        Bitmap bitmap = null;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        if (path != null) {
            try {
                bitmap = BitmapFactory.decodeFile(path, options);
            } catch (Throwable ignored) {
            }
        }
        if (bitmap == null) {
            try (InputStream is = contentResolver.openInputStream(uri)) {
                bitmap = BitmapFactory.decodeStream(is, null, options);
            } catch (Throwable ignored) {
            }
        }
        if (bitmap == null) {
            return null;
        }

        int degree = 0;
        if (path != null) {
            try {
                int orientation = new ExifInterface(path)
                    .getAttributeInt(ExifInterface.TAG_ORIENTATION, -1);
                if (orientation == 3) {
                    degree = 180;
                } else if (orientation == 6) {
                    degree = 90;
                } else if (orientation == 8) {
                    degree = 270;
                }
            } catch (Throwable ignored) {
            }
        }
        if (degree != 0) {
            Matrix matrix = new Matrix();
            matrix.setRotate(degree);
            Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0,
                bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            if (rotated != bitmap) {
                bitmap.recycle();
                bitmap = rotated;
            }
        }
        return bitmap;
    }
}
