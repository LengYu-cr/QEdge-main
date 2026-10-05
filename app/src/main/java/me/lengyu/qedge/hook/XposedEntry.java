package me.lengyu.qedge.hook;

import android.content.Context;
import android.os.Build;
import dalvik.system.BaseDexClassLoader;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import me.lengyu.qedge.BuildConfig;
import me.lengyu.qedge.activity.SettingActivity;
import me.lengyu.qedge.common.ModuleScope;
import me.lengyu.qedge.lifecycle.DynamicActivityRegistry;
import me.lengyu.qedge.lifecycle.Parasitics;
import me.lengyu.qedge.utils.HostInfo;
import me.lengyu.qedge.utils.Toasts;
import me.lengyu.qedge.utils.ReflectUtils;
import me.lengyu.qedge.utils.dexkit.DexKitCache;
import me.lengyu.qedge.utils.dexkit.DexKitFinder;
import me.lengyu.qedge.utils.hook.HookStatusImpl;
import me.lengyu.qedge.hook.kk.KKHook;
import me.lengyu.qedge.hook.ifly.IFlyHook;
import me.lengyu.qedge.hook.kugou.KuGouHook;
import me.lengyu.qedge.hook.aoruan.AoRuanHook;
import me.lengyu.qedge.hook.deviceInfoX.DeviceInfoXHook;
import me.lengyu.qedge.hook.painlessword.PainlessWordHook;
import me.lengyu.qedge.hook.woodenletter.WoodenLetterHook;

/**
 * @Author 冷雨
 * @Description Xposed 入口类
 */
public class XposedEntry implements IXposedHookLoadPackage, IXposedHookZygoteInit {
    private static final AtomicBoolean initialized = new AtomicBoolean(false);
    private static final AtomicBoolean hasCapturedTinker = new AtomicBoolean(false);
    private static String modulePath = null;
    private static final String[] supportedPackages = {"com.tencent.mobileqq", "com.tencent.tim", "im.weshine.keyboard", "com.iflytek.inputmethod", "com.kugou.android", "com.apowersoft.backgrounderaser", "com.liuzh.deviceinfo", "tech.xiangzi.painless", "com.One.WoodenLetter"};

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
            ReflectUtils.initClassLoader(lpparam.classLoader);
            Parasitics.INSTANCE.setModulePath(modulePath);

            if (lpparam.packageName.equals("com.tencent.mobileqq") || lpparam.packageName.equals("com.tencent.tim")) {
                DynamicActivityRegistry dynamicActivityRegistry = DynamicActivityRegistry.INSTANCE;
                DynamicActivityRegistry.register(SettingActivity.class);
                hookBaseApplicationOnCreate(lpparam.classLoader);
            } else if (lpparam.packageName.equals("im.weshine.keyboard")) {
                hookKK(lpparam.classLoader);
            } else if (lpparam.packageName.equals("com.iflytek.inputmethod")) {
                hookIFly(lpparam.classLoader);
            } else if (lpparam.packageName.equals("com.kugou.android.elder")) {
                hookKuGouElder(lpparam.classLoader);
            } else if (lpparam.packageName.equals("com.kugou.android.lite")) {
                hookKuGouLite(lpparam.classLoader);
            } else if (lpparam.packageName.equals("com.apowersoft.backgrounderaser")) {
                hookAoRuan(lpparam.classLoader);
            } else if (lpparam.packageName.equals("com.liuzh.deviceinfo")) {
                hookDeviceInfoX(lpparam.classLoader);
            } else if (lpparam.packageName.equals("tech.xiangzi.painless")) {
                hookPainless(lpparam.classLoader);
            } else if (lpparam.packageName.equals("com.One.WoodenLetter")) {
                hookWoodenLetter(lpparam.classLoader);
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
        } else if (pkg.equals("com.iflytek.inputmethod")) {
            appClass = "com.iflytek.inputmethod.FlyApp";
        } else if (pkg.equals("com.kugou.android.elder")) {
            appClass = "com.kugou.common.app.KGTinkerApplication";
        } else if (pkg.equals("com.kugou.android.lite")) {
            appClass = "com.kugou.android.app.KGApplication";
        } else if (pkg.equals("com.apowersoft.backgrounderaser")) {
            appClass = "com.backgrounderaser.baselib.init.GlobalApplication";
        } else if (pkg.equals("com.liuzh.deviceinfo")) {
            appClass = "com.liuzh.deviceinfo.DeviceInfoApp";
        } else if (pkg.equals("tech.xiangzi.painless")) {
            appClass = "android.app.Application";
        } else if (pkg.equals("com.One.WoodenLetter")) {
            appClass = "android.app.Application";
        } else {
            return;
        }
        try {
            XposedHelpers.findAndHookMethod(appClass, lpparam.classLoader, "onCreate", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    try {
                        if (param.thisObject instanceof Context) {
                            Toasts.toast("QEdge 不支持 32 位系统版本");
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
            Method attach = classLoader
                    .loadClass("com.tencent.common.app.QFixApplicationImplProxy")
                    .getDeclaredMethod("attachBaseContext", Context.class);
            attach.setAccessible(true);
            hookQFixAttach(attach);
        } catch (Throwable e) {
            XposedBridge.log("[QEdge] 未找到 QFixApplicationImplProxy.attachBaseContext，直接启动: " + e.getMessage());
            doRealStartup(classLoader);
        }
    }

    /**
     * QQ 启用了热更补丁（QFix/Tinker），真正的宿主类挂在补丁自己的 ClassLoader 上，
     * 而不是 lpparam.classLoader。若直接拿 lpparam.classLoader 去 hook，会挂到宿主里的同名类上，
     * 表现就是"hook 安装成功但永远不触发"。这里在 attachBaseContext 执行期间拦截所有
     * BaseDexClassLoader 构造，捕获补丁 loader，附件结束后立即卸载临时 hook。
     */
    private void hookQFixAttach(final Method attach) {
        final List<XC_MethodHook.Unhook> constructorUnhooks = new ArrayList<>();

        XposedBridge.hookMethod(attach, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                for (Constructor<?> constructor : BaseDexClassLoader.class.getDeclaredConstructors()) {
                    try {
                        XC_MethodHook.Unhook unhook = XposedBridge.hookMethod(constructor, new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(MethodHookParam param) {
                                ClassLoader loader = (ClassLoader) param.thisObject;
                                String loaderStr = loader.toString();
                                if (loaderStr.contains(BuildConfig.APPLICATION_ID)) {
                                    return;
                                }
                                if ((loaderStr.contains("com.tencent.")
                                        || loaderStr.contains("TinkerClassLoader")
                                        || loaderStr.contains("DelegateLastClassLoader"))
                                        && hasCapturedTinker.compareAndSet(false, true)) {
                                    doRealStartup(loader);
                                }
                            }
                        });
                        if (unhook != null) {
                            constructorUnhooks.add(unhook);
                        }
                    } catch (Throwable ignored) {
                    }
                }
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                for (XC_MethodHook.Unhook unhook : constructorUnhooks) {
                    unhook.unhook();
                }
                constructorUnhooks.clear();

                if (!hasCapturedTinker.get()) {
                    Context context = (Context) param.args[0];
                    XposedBridge.log("[QEdge] 未捕获热更 ClassLoader，回退 context.classLoader");
                    doRealStartup(context.getClassLoader());
                }
            }
        });
    }

    /** 拿到真实宿主 ClassLoader 后只执行一次：挂 Application.onCreate */
    private synchronized void doRealStartup(final ClassLoader realClassLoader) {
        if (!initialized.compareAndSet(false, true)) {
            return;
        }
        // 两个需求必须分开满足：
        //  - 宿主类查找要用补丁 loader，否则会挂到基础包里的旧版同名类上（hook 装了不触发）；
        //  - 但绝不能改写模块自身 ClassLoader 的 parent（initClassLoader 会做这件事），
        //    补丁 loader 是 DelegateLastClassLoader，启动期正并发加载同一批类，会死锁（QQ 卡启动页）。
        // 所以这里只切换查找入口，不动 parent 链。
        ReflectUtils.setHostClassLoader(realClassLoader);
        hookApplicationOnCreate();
    }

    /**
     * QQ 9.3.70 起 Application 改由 QFix 代理创建，会先后实例化
     * QFixApplicationImplProxy / QFixApplicationImpl / BaseApplicationImpl，
     * 且都不重写 onCreate，只能从 android.app.Application 兜底拦截后沿继承链确认身份。
     */
    private void hookApplicationOnCreate() {
        try {
            XposedHelpers.findAndHookMethod(
                    "android.app.Application",
                    ReflectUtils.hostClassLoader,
                    "onCreate",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            Object app = param.thisObject;
                            if (!(app instanceof Context)) {
                                return;
                            }
                            if (!isBaseApplicationImpl(app)) {
                                return;
                            }
                            XposedBridge.log("[QEdge] 命中 QQ Application: " + app.getClass().getName());
                            try {
                                Context hostContext = (Context) app;
                                HostInfo hostInfo = HostInfo.INSTANCE;
                                HostInfo.init(hostContext);
                                Parasitics.initForStubActivity(hostContext);
                            } catch (Throwable e) {
                                XposedBridge.log("[QEdge] 初始化失败: " + e.getMessage());
                                XposedBridge.log(e);
                            }
                            loadHooksOffMainThread();
                        }
                    });
        } catch (Throwable e) {
            XposedBridge.log("[QEdge] Hook Application.onCreate 失败: " + e.getMessage());
        }
    }

    /** 沿继承链判断是否 QQ 的 BaseApplicationImpl（不能在编译期引用该类） */
    private static boolean isBaseApplicationImpl(Object app) {
        for (Class<?> c = app.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            if ("com.tencent.common.app.BaseApplicationImpl".equals(c.getName())) {
                return true;
            }
        }
        return false;
    }
    /**
     * 缓存读盘、反射校验、Hook 注册与 DexKit 查找都不是首帧必需，整体移到后台调度器执行，
     * 主线程只保留 initialized 抢占与 HostInfo/Parasitics 这类必须尽早生效的初始化。
     * 例外：首次安装/升级后没有缓存时，必须先在主线程同步挂上 SplashActivity 的查找 hook
     * （DexKitFinder.doFind），否则查找流程永远不会被触发，Hook 也就永远不会加载。
     */
    private void loadHooksOffMainThread() {
        if (!DexKitCache.hasCacheFile()) {
            DexKitFinder.doFind();
            return;
        }
        ModuleScope.launchHookJava("HookInit", () -> {
            try {
                // 必须先注册：validateAllTasks() 靠 HookRegistry 收集 DexKitTask 列表，
                // 未注册时列表为空、校验恒为 true，缓存缺条目也不会被发现，会直接 hook 失败。
                // loadHook() 内部会再注册一次，HookRegistry 按类去重，无副作用。
                MainHook.registerHookItems();
                if (DexKitCache.initCache() && DexKitCache.validateAllTasks()) {
                    MainHook.loadHook();
                    XposedBridge.log("[QEdge] Hook 初始化成功");
                } else {
                    // 缓存损坏或缺条目：退回重新查找
                    ModuleScope.postToMain(DexKitFinder::doFind);
                    XposedBridge.log("[QEdge] Hook 初始化失败，退回重新查找");
                }
            } catch (Throwable e) {
                XposedBridge.log("[QEdge] Hook 初始化失败: " + e.getMessage());
                XposedBridge.log(e);
            }
        });
    }

    private void hookKuGouElder(final ClassLoader classLoader) {
        try {
            XposedHelpers.findAndHookMethod("com.kugou.common.app.KGTinkerApplication", classLoader, "onCreate", new Object[]{new XC_MethodHook() {
                protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) {
                    try {
                        Context hostContext = (Context) param.thisObject;
                        HostInfo hostInfo = HostInfo.INSTANCE;
                        HostInfo.init(hostContext);
                        // Hook 注册与 DexKit 扫描较重，交给后台调度器，不占用宿主主线程
                        ModuleScope.launchHookJava("KuGouHook", () -> KuGouHook.loadHook("com.kugou.android.elder"));
                        Toasts.toast("QEdge 注入成功");
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
                        ModuleScope.launchHookJava("KuGouHook", () -> KuGouHook.loadHook("com.kugou.android.lite"));
                        Toasts.toast("QEdge 注入成功");
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
                            // Hook 注册与 DexKit 扫描较重，交给后台调度器，不占用宿主主线程
                            ModuleScope.launchHookJava("KKHook", () -> KKHook.loadHook());
                            Toasts.toast("QEdge 注入成功");
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

     private void hookIFly(final ClassLoader classLoader) {
        try {
            XposedHelpers.findAndHookMethod("com.iflytek.inputmethod.FlyApp", classLoader, "onCreate", new Object[]{new XC_MethodHook() {
                protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) {
                    if (XposedEntry.initialized.compareAndSet(false, true)) {
                        try {
                            Context hostContext = (Context) param.thisObject;
                            HostInfo hostInfo = HostInfo.INSTANCE;
                            HostInfo.init(hostContext);
                            ModuleScope.launchHookJava("IFlyHook", () -> IFlyHook.loadHook());
                            Toasts.toast("QEdge 注入成功");
                        } catch (Throwable e) {
                            XposedBridge.log("[QEdge] 讯飞输入法延迟初始化失败: " + e.getMessage());
                            XposedBridge.log(e);
                        }
                    }
                }
            }});
        } catch (Throwable e) {
            XposedBridge.log("[QEdge] Hook FlyApp.onCreate 失败: " + e.getMessage());
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
                            ModuleScope.launchHookJava("AoRuanHook", () -> AoRuanHook.loadHook());
                            Toasts.toast("QEdge 注入成功");
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

    private void hookDeviceInfoX(final ClassLoader classLoader) {
        try {
            XposedHelpers.findAndHookMethod("com.liuzh.deviceinfo.DeviceInfoApp", classLoader, "onCreate", new Object[]{new XC_MethodHook() {
                protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) {
                    if (XposedEntry.initialized.compareAndSet(false, true)) {
                        try {
                            Context hostContext = (Context) param.thisObject;
                            HostInfo hostInfo = HostInfo.INSTANCE;
                            HostInfo.init(hostContext);
                            Parasitics.initForStubActivity(hostContext);
                            ModuleScope.launchHookJava("DeviceInfoXHook", () -> DeviceInfoXHook.loadHook());
                            Toasts.toast("QEdge 注入成功");
                        } catch (Throwable e) {
                            XposedBridge.log("[QEdge] 设备信息X延迟初始化失败: " + e.getMessage());
                            XposedBridge.log(e);
                        }
                    }
                }
            }});
        } catch (Throwable e) {
            XposedBridge.log("[QEdge] Hook DeviceInfoApp.onCreate 失败: " + e.getMessage());
        }
    }

    private void hookPainless(final ClassLoader classLoader) {
        try {
            XposedHelpers.findAndHookMethod("android.app.Application", classLoader, "onCreate", new Object[]{new XC_MethodHook() {
                protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) {
                    if (XposedEntry.initialized.compareAndSet(false, true)) {
                        try {
                            Context hostContext = (Context) param.thisObject;
                            HostInfo hostInfo = HostInfo.INSTANCE;
                            HostInfo.init(hostContext);
                            Toasts.toast("QEdge 注入成功");
                            ModuleScope.launchHookJava("PainlessWordHook", () -> PainlessWordHook.loadHook());
                        } catch (Throwable e) {
                            XposedBridge.log("[QEdge] 无痛单词延迟初始化失败: " + e.getMessage());
                            XposedBridge.log(e);
                        }
                    }
                }
            }});
        } catch (Throwable e) {
            XposedBridge.log("[QEdge] Hook Application.onCreate 失败: " + e.getMessage());
        }
    }

    private void hookWoodenLetter(final ClassLoader classLoader) {
        try {
            XposedHelpers.findAndHookMethod("android.app.Application", classLoader, "onCreate", new Object[]{new XC_MethodHook() {
                protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) {
                    if (XposedEntry.initialized.compareAndSet(false, true)) {
                        try {
                            Context hostContext = (Context) param.thisObject;
                            HostInfo hostInfo = HostInfo.INSTANCE;
                            HostInfo.init(hostContext);
                            Toasts.toast("QEdge 注入成功");
                            ModuleScope.launchHookJava("WoodenLetterHook", () -> WoodenLetterHook.loadHook());
                        } catch (Throwable e) {
                            XposedBridge.log("[QEdge] 木函延迟初始化失败: " + e.getMessage());
                            XposedBridge.log(e);
                        }
                    }
                }
            }});
        } catch (Throwable e) {
            XposedBridge.log("[QEdge] Hook Application.onCreate 失败: " + e.getMessage());
        }
    }

}