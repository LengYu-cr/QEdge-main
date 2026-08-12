package me.lengyu.qedge.hook;

import android.content.Context;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import me.lengyu.qedge.activity.SettingActivity;
import me.lengyu.qedge.lifecycle.DynamicActivityRegistry;
import me.lengyu.qedge.lifecycle.Parasitics;
import me.lengyu.qedge.utils.HostInfo;
import me.lengyu.qedge.utils.LogUtils;
import me.lengyu.qedge.utils.ReflectUtils;
import me.lengyu.qedge.utils.reflect.ClassUtils;
import me.lengyu.qedge.utils.dexkit.DexKitCache;
import me.lengyu.qedge.utils.dexkit.DexKitFinder;
import me.lengyu.qedge.utils.hook.HookStatusImpl;
import me.lengyu.qedge.hook.kk.KKHook;
import me.lengyu.qedge.hook.kugou.KuGouHook;
import me.lengyu.qedge.hook.aoruan.AoRuanHook;


public class XposedEntry implements IXposedHookLoadPackage, IXposedHookZygoteInit {

    /**
     * 每个宿主 App 独立初始化标志，避免 KK 初始化后 QQ 被跳过。
     * Key = 宿主包名，如 "com.tencent.mobileqq"、"im.weshine.keyboard"
     */
    private static final ConcurrentHashMap<String, AtomicBoolean> appInitialized = new ConcurrentHashMap<>();
    private static String modulePath = null;
    private static final String[] supportedPackages = {
        "com.tencent.mobileqq", "com.tencent.tim",
        "im.weshine.keyboard", "com.kugou.android", "com.apowersoft.backgrounderaser"
    };

    // ──────────────────────── Zygote 阶段 ────────────────────────

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

    // ──────────────────────── 包名分发 ────────────────────────

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

        try {
            if (modulePath == null) {
                modulePath = getModulePathFromClassLoader();
            }

            if (modulePath == null) {
                LogUtils.e("XposedEntry", "模块路径为null，资源注入将失败！");
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
                hookThirdPartyApp(lpparam, "im.weshine.foundation.base.delegate.BaseApplication",
                        KKHook::loadHook, "QEdge 注入成功");
            } else if (lpparam.packageName.equals("com.kugou.android.elder")) {
                hookThirdPartyApp(lpparam, "com.kugou.common.app.KGTinkerApplication",
                        () -> KuGouHook.loadHook("com.kugou.android.elder"),
                        "[QEdge] 酷狗大字版注入成功");
            } else if (lpparam.packageName.equals("com.kugou.android.lite")) {
                hookThirdPartyApp(lpparam, "com.kugou.android.app.KGApplication",
                        () -> KuGouHook.loadHook("com.kugou.android.lite"),
                        "[QEdge] 酷狗概念版注入成功");
            } else if (lpparam.packageName.equals("com.apowersoft.backgrounderaser")) {
                hookThirdPartyApp(lpparam, "com.backgrounderaser.baselib.init.GlobalApplication",
                        AoRuanHook::loadHook, "QEdge 注入成功");
            }
        } catch (Throwable e) {
            LogUtils.e("XposedEntry", "Hook load failed: " + e.getMessage());
            LogUtils.e("XposedEntry", e);
        }
    }

    // ──────────────────────── 通用第三方 App Hook 方法 ────────────────────────

    /**
     * 统一的第三方 App Hook 入口，消除 hookKK / hookKuGouElder / hookKuGouLite / hookAoRuan 的重复代码。
     *
     * @param lpparam        LoadPackageParam（用于获取 classLoader 和 packageName）
     * @param appClassName   宿主 Application 全限定类名
     * @param loadHookAction 初始化成功后的回调（如 KKHook::loadHook）
     * @param toastMsg       注入成功 Toast 文本
     */
    private void hookThirdPartyApp(XC_LoadPackage.LoadPackageParam lpparam,
                                   String appClassName,
                                   Runnable loadHookAction,
                                   String toastMsg) {
        try {
            XposedHelpers.findAndHookMethod(appClassName, lpparam.classLoader, "onCreate",
                new XC_MethodHook() {
                    protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) {
                        AtomicBoolean flag = appInitialized.computeIfAbsent(
                                lpparam.packageName, k -> new AtomicBoolean(false));
                        if (!flag.compareAndSet(false, true)) {
                            return;
                        }
                        try {
                            Context hostContext = (Context) param.thisObject;
                            HostInfo.INSTANCE.init(hostContext);
                            Parasitics.initForStubActivity(hostContext);
                            loadHookAction.run();
                            android.widget.Toast.makeText(hostContext, toastMsg,
                                    android.widget.Toast.LENGTH_LONG).show();
                        } catch (Throwable e) {
                            LogUtils.e("XposedEntry", lpparam.packageName + " 注入失败: " + e.getMessage());
                            LogUtils.e("XposedEntry", e);
                        }
                    }
                });
        } catch (Throwable e) {
            LogUtils.e("XposedEntry", "Hook " + appClassName + ".onCreate 失败: " + e.getMessage());
        }
    }

    // ──────────────────────── 模块路径查找 ────────────────────────

    private String getModulePathFromClassLoader() {
        Object pathList;
        Object[] dexElements;
        try {
            ClassLoader moduleClassLoader = XposedEntry.class.getClassLoader();
            if (moduleClassLoader != null
                    && (pathList = ReflectUtils.getFieldValue(moduleClassLoader, "pathList")) != null
                    && (dexElements = (Object[]) ReflectUtils.getFieldValue(pathList, "dexElements")) != null
                    && dexElements.length > 0) {
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
            LogUtils.e("XposedEntry", "获取模块路径失败: " + e.getMessage());
            return null;
        }
    }

    // ──────────────────────── QQ/TIM 主入口 ────────────────────────

    private void hookBaseApplicationOnCreate(final ClassLoader classLoader) {
        try {
            XposedHelpers.findAndHookMethod("com.tencent.common.app.BaseApplicationImpl",
                classLoader, "onCreate", new XC_MethodHook() {
                    protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) {
                        AtomicBoolean flag = appInitialized.computeIfAbsent(
                                HostInfo.packageName, k -> new AtomicBoolean(false));
                        if (!flag.compareAndSet(false, true)) {
                            return;
                        }
                        try {
                            Context hostContext = (Context) param.thisObject;
                            HostInfo.INSTANCE.init(hostContext);
                            Parasitics.initForStubActivity(hostContext);

                            boolean cacheValid = DexKitCache.initCache();
                            if (cacheValid && DexKitCache.validateAllTasks()) {
                                MainHook.loadHook();
                            } else {
                                DexKitFinder.doFind();
                            }
                        } catch (Throwable e) {
                            LogUtils.e("XposedEntry", "延迟初始化失败: " + e.getMessage());
                            LogUtils.e("XposedEntry", e);
                        }
                    }
                });
        } catch (Throwable e) {
            LogUtils.e("XposedEntry", "Hook BaseApplicationImpl.onCreate 失败: " + e.getMessage());
        }
    }
}