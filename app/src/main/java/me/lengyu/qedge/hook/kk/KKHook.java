package me.lengyu.qedge.hook.kk;

import java.lang.reflect.Method;
import java.util.List;
import me.lengyu.qedge.hook.annotation.HookItemAnnotation;
import me.lengyu.qedge.hook.base.BaseApiHookItem;
import me.lengyu.qedge.utils.ReflectUtils;
import me.lengyu.qedge.utils.LogUtils;
import me.lengyu.qedge.utils.HookUtils;
import me.lengyu.qedge.utils.HostInfo;
import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindClass;
import org.luckypray.dexkit.query.matchers.ClassMatcher;
import org.luckypray.dexkit.result.ClassData;
import org.luckypray.dexkit.wrap.DexClass;

public class KKHook {

    private static boolean initialized = false;

    public static void loadHook() {
        if (initialized) return;
        initialized = true;

        hookVip();
        hookVipInfo();
        hookExit();
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
            e.printStackTrace();
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
            e.printStackTrace();
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
            e.printStackTrace();
        }
    }

    private static void hookUncaughtException() {
        try {
            final Thread.UncaughtExceptionHandler defaultHandler =
                Thread.getDefaultUncaughtExceptionHandler();
            Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
                LogUtils.e("hookExit", "Uncaught exception in " + thread.getName() +
                    ": " + throwable.getMessage());
                throwable.printStackTrace();
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
}
