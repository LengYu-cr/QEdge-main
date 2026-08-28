package me.lengyu.qedge.hook;

import android.content.Context;
import android.os.Build;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import java.util.concurrent.atomic.AtomicBoolean;
import me.lengyu.qedge.activity.SettingActivity;
import me.lengyu.qedge.lifecycle.DynamicActivityRegistry;
import me.lengyu.qedge.lifecycle.Parasitics;
import me.lengyu.qedge.utils.HostInfo;
import me.lengyu.qedge.utils.ReflectUtils;
import me.lengyu.qedge.utils.reflect.ClassUtils;
import me.lengyu.qedge.utils.dexkit.DexKitCache;
import me.lengyu.qedge.utils.dexkit.DexKitFinder;
import me.lengyu.qedge.utils.hook.HookStatusImpl;
import me.lengyu.qedge.hook.kk.KKHook;
import me.lengyu.qedge.hook.kugou.KuGouHook;
import me.lengyu.qedge.hook.aoruan.AoRuanHook;

/**
 * @Author 冷雨
 * @Description Xposed 入口类
 */
public class XposedEntry implements IXposedHookLoadPackage, IXposedHookZygoteInit {
    private static final AtomicBoolean initialized = new AtomicBoolean(false);
    private static String modulePath = null;
    private static final String[] supportedPackages = {"com.tencent.mobileqq", "com.tencent.tim", "im.weshine.keyboard", "com.kugou.android", "com.apowersoft.backgrounderaser"};

    public void initZygote(IXposedHookZygoteInit.StartupParam startupParam) throws Throwable {
        modulePath = startupParam.modulePath;
        initHookStatus();
    }

    private void initHookStatus() throws Throwable {
        HookStatusImpl.sZygoteHookMode = true;
        boolean dexObfsEnabled = !"de.robv.android.xposed.XposedBridge".equals(XposedBridge.class.getName());
        String hookProvider = null;
        if (dexObfsEnabled) {
            HookStatusImpl.sIsLsposedDexObfsEnabled = true;
            hookProvider = "LSPosed";
        } else {
            String bridgeTag = null;
            try {
                bridgeTag = (String) XposedBridge.class.getDeclaredField("TAG").get(null);
            } catch (ReflectiveOperationException ignored) {
            }
            if (bridgeTag != null) {
                if (bridgeTag.startsWith("LSPosed")) {
                    hookProvider = "LSPosed";
                } else if (bridgeTag.startsWith("EdXposed")) {
                    hookProvider = "EdXposed";
                } else if (bridgeTag.startsWith("PineXposed")) {
                    hookProvider = "Dreamland";
                }
            }
        }
        if (hookProvider != null) {
            HookStatusImpl.sZygoteHookProvider = hookProvider;
        }
    }

    private boolean isNameSupported(String packageName) {
        for (String supportedPackage : supportedPackages) {
            if (packageName.startsWith(supportedPackage)) {
                return true;
            }
        }
        return false;
    }

    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!isNameSupported(lpparam.packageName)) {
            return;
        }

        // 仅打包 arm64(v8a)：若为 32 位设备，不加载模块并提示系统版本不支持
        if (is32BitDevice()) {
            reject32BitDevice(lpparam);
            return;
        }

        try {
            if (modulePath == null) {
                modulePath = getModulePathFromClassLoader();
            }

            if (modulePath == null) {
                XposedBridge.log("[QEdge] ERROR: 模块路径为null，资源注入将失败！");
            }

            HostInfo.packageName = lpparam.packageName;
            HostInfo.processName = lpparam.processName;
            ClassUtils.INSTANCE.setHostClassLoader(lpparam.classLoader);
            ReflectUtils.initClassLoader(lpparam.classLoader);
            Parasitics.INSTANCE.setModulePath(modulePath);

            if (lpparam.packageName.equals("com.tencent.mobileqq") || lpparam.packageName.equals("com.tencent.tim")) {
                DynamicActivityRegistry dynamicActivityRegistry = DynamicActivityRegistry.INSTANCE;
                DynamicActivityRegistry.register(SettingActivity.class);
                hookBaseApplicationOnCreate(lpparam.classLoader);
            } else if (lpparam.packageName.equals("im.weshine.keyboard")) {
                hookKK(lpparam.classLoader);
            } else if (lpparam.packageName.equals("com.kugou.android.elder")) {
                hookKuGouElder(lpparam.classLoader);
            } else if (lpparam.packageName.equals("com.kugou.android.lite")) {
                hookKuGouLite(lpparam.classLoader);
            } else if (lpparam.packageName.equals("com.apowersoft.backgrounderaser")) {
                hookAoRuan(lpparam.classLoader);
            }
        } catch (Throwable e) {
            XposedBridge.log("[QEdge] Hook load failed: " + e.getMessage());
            XposedBridge.log(e);
        }
    }

    private String getModulePathFromClassLoader() {
        Object pathList;
        Object[] dexElements;
        try {
            ClassLoader moduleClassLoader = XposedEntry.class.getClassLoader();
            if (moduleClassLoader != null && (pathList = ReflectUtils.getFieldValue(moduleClassLoader, "pathList")) != null && (dexElements = (Object[]) ReflectUtils.getFieldValue(pathList, "dexElements")) != null && dexElements.length > 0) {
                for (Object dexElement : dexElements) {
                    String path = (String) ReflectUtils.getFieldValue(dexElement, "path");
                    if (path != null && path.contains("me.lengyu.qedge") && path.endsWith(".apk")) {
                        return path;
                    }
                }
                return null;
            }
            return null;
        } catch (Throwable e) {
            XposedBridge.log("[QEdge] 获取模块路径失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 仅打包 arm64 后，若设备为纯 32 位则判定为不支持
     */
    private boolean is32BitDevice() {
        return Build.SUPPORTED_64_BIT_ABIS == null || Build.SUPPORTED_64_BIT_ABIS.length == 0;
    }

    /**
     * 32 位设备：不加载任何模块，仅 hook 宿主 Application.onCreate 提示系统版本不支持
     */
    private void reject32BitDevice(XC_LoadPackage.LoadPackageParam lpparam) {
        final String appClass;
        String pkg = lpparam.packageName;
        if (pkg.equals("com.tencent.mobileqq") || pkg.equals("com.tencent.tim")) {
            appClass = "com.tencent.common.app.BaseApplicationImpl";
        } else if (pkg.equals("im.weshine.keyboard")) {
            appClass = "im.weshine.foundation.base.delegate.BaseApplication";
        } else if (pkg.equals("com.kugou.android.elder")) {
            appClass = "com.kugou.common.app.KGTinkerApplication";
        } else if (pkg.equals("com.kugou.android.lite")) {
            appClass = "com.kugou.android.app.KGApplication";
        } else if (pkg.equals("com.apowersoft.backgrounderaser")) {
            appClass = "com.backgrounderaser.baselib.init.GlobalApplication";
        } else {
            return;
        }
        try {
            XposedHelpers.findAndHookMethod(appClass, lpparam.classLoader, "onCreate", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    try {
                        if (param.thisObject instanceof Context) {
                            android.widget.Toast.makeText((Context) param.thisObject,
                                    "QEdge 不支持 32 位系统版本", android.widget.Toast.LENGTH_LONG).show();
                        }
                    } catch (Throwable ignored) {
                    }
                }
            });
        } catch (Throwable e) {
            XposedBridge.log("[QEdge] 32 位设备拦截提示失败: " + e.getMessage());
        }
    }

    private void hookBaseApplicationOnCreate(final ClassLoader classLoader) {
        try {
            XposedHelpers.findAndHookMethod("com.tencent.common.app.BaseApplicationImpl", classLoader, "onCreate", new Object[]{new XC_MethodHook() {
                protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) {
                    if (XposedEntry.initialized.compareAndSet(false, true)) {
                        try {
                            Context hostContext = (Context) param.thisObject;
                            HostInfo hostInfo = HostInfo.INSTANCE;
                            HostInfo.init(hostContext);
                            Parasitics.initForStubActivity(hostContext);

                            boolean cacheValid = DexKitCache.initCache();
                            if (cacheValid && DexKitCache.validateAllTasks()) {
                                MainHook.loadHook();
                            } else {
                                DexKitFinder.doFind();
                            }
                        } catch (Throwable e) {
                            XposedBridge.log("[QEdge] 延迟初始化失败: " + e.getMessage());
                            XposedBridge.log(e);
                        }
                    }
                }
            }});
        } catch (Throwable e) {
            XposedBridge.log("[QEdge] Hook BaseApplicationImpl.onCreate 失败: " + e.getMessage());
        }
    }

    private void hookKuGouElder(final ClassLoader classLoader) {
        try {
            XposedHelpers.findAndHookMethod("com.kugou.common.app.KGTinkerApplication", classLoader, "onCreate", new Object[]{new XC_MethodHook() {
                protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) {
                    try {
                        Context hostContext = (Context) param.thisObject;
                        HostInfo hostInfo = HostInfo.INSTANCE;
                        HostInfo.init(hostContext);
                        KuGouHook.loadHook("com.kugou.android.elder");
                        android.widget.Toast.makeText(hostContext, "[QEdge] 酷狗大字版注入成功", android.widget.Toast.LENGTH_LONG).show();
                    } catch (Throwable e) {
                        XposedBridge.log("[QEdge] 酷狗大字版 Hook 失败: " + e.getMessage());
                        XposedBridge.log(e);
                    }
                }
            }});
        } catch (Throwable e) {
            XposedBridge.log("[QEdge] Hook KugouApplication.onCreate 失败: " + e.getMessage());
        }
    }

    private void hookKuGouLite(final ClassLoader classLoader) {
        try {
            XposedHelpers.findAndHookMethod("com.kugou.android.app.KGApplication", classLoader, "onCreate", new Object[]{new XC_MethodHook() {
                protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) {
                    try {
                        Context hostContext = (Context) param.thisObject;
                        HostInfo hostInfo = HostInfo.INSTANCE;
                        HostInfo.init(hostContext);
                        KuGouHook.loadHook("com.kugou.android.lite");
                        android.widget.Toast.makeText(hostContext, "[QEdge] 酷狗概念版注入成功", android.widget.Toast.LENGTH_LONG).show();
                    } catch (Throwable e) {
                        XposedBridge.log("[QEdge] 酷狗概念版 Hook 失败: " + e.getMessage());
                        XposedBridge.log(e);
                    }
                }
            }});
        } catch (Throwable e) {
            XposedBridge.log("[QEdge] Hook KugouApplication.onCreate 失败: " + e.getMessage());
        }
    }

    private void hookKK(final ClassLoader classLoader) {
        try {
            XposedHelpers.findAndHookMethod("im.weshine.foundation.base.delegate.BaseApplication", classLoader, "onCreate", new Object[]{new XC_MethodHook() {
                protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) {
                    if (XposedEntry.initialized.compareAndSet(false, true)) {
                        try {
                            Context hostContext = (Context) param.thisObject;
                            HostInfo hostInfo = HostInfo.INSTANCE;
                            HostInfo.init(hostContext);
                            Parasitics.initForStubActivity(hostContext);
                            KKHook.loadHook();
                            android.widget.Toast.makeText(hostContext, "QEdge 注入成功", android.widget.Toast.LENGTH_LONG).show();
                        } catch (Throwable e) {
                            XposedBridge.log("[QEdge] 延迟初始化失败: " + e.getMessage());
                            XposedBridge.log(e);
                        }
                    }
                }
            }});
        } catch (Throwable e) {
            XposedBridge.log("[QEdge] Hook BaseApplication.onCreate 失败: " + e.getMessage());
        }
    }

     private void hookAoRuan(final ClassLoader classLoader) {
        try {
            XposedHelpers.findAndHookMethod("com.backgrounderaser.baselib.init.GlobalApplication", classLoader, "onCreate", new Object[]{new XC_MethodHook() {
                protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) {
                    if (XposedEntry.initialized.compareAndSet(false, true)) {
                        try {
                            Context hostContext = (Context) param.thisObject;
                            HostInfo hostInfo = HostInfo.INSTANCE;
                            HostInfo.init(hostContext);
                            Parasitics.initForStubActivity(hostContext);
                            AoRuanHook.loadHook();
                            android.widget.Toast.makeText(hostContext, "QEdge 注入成功", android.widget.Toast.LENGTH_LONG).show();
                        } catch (Throwable e) {
                            XposedBridge.log("[QEdge] 延迟初始化失败: " + e.getMessage());
                            XposedBridge.log(e);
                        }
                    }
                }
            }});
        } catch (Throwable e) {
            XposedBridge.log("[QEdge] Hook BaseApplication.onCreate 失败: " + e.getMessage());
        }
    }

}