package me.lengyu.qedge.utils.dexkit;

import java.io.File;

import me.lengyu.qedge.utils.HostInfo;
import me.lengyu.qedge.utils.LogUtils;

/**
 * DexKit 原生库加载管理器。
 *
 * <p>问题背景：Hook 运行在宿主的寄生 ClassLoader（如 {@code fpa.core.FPAClassLoader}）上下文中，
 * 该 ClassLoader 的 nativeLibraryDirectories 不含模块 APK 的 lib 目录，直接
 * {@code System.loadLibrary("dexkit")} 会抛 {@code UnsatisfiedLinkError: couldn't find "libdexkit.so"}。
 *
 * <p>本类提供全局幂等的加载入口：优先常规 {@code loadLibrary}，失败则回退到用模块 APK 的
 * native 库绝对路径 {@code System.load()}，绕开 ClassLoader 的库目录限制。加载结果缓存，
 * 全进程只真正加载一次。
 */
public final class DexKitManager {

    private static final String TAG = "DexKitManager";
    private static final String LIB_NAME = "dexkit";

    private static volatile boolean loaded = false;

    private DexKitManager() {
    }

    /**
     * 确保 libdexkit.so 已加载。幂等、线程安全。
     *
     * @return 是否加载成功；失败时调用方应放弃 DexKit 逻辑（回退到硬编码/扫描）。
     */
    public static boolean ensureLibrary() {
        if (loaded) {
            return true;
        }
        synchronized (DexKitManager.class) {
            if (loaded) {
                return true;
            }
            // 1) 常规方式：在模块自身上下文中通常可用
            try {
                System.loadLibrary(LIB_NAME);
                loaded = true;
                return true;
            } catch (Throwable e) {
                LogUtils.w(TAG, "loadLibrary failed, fallback to absolute path: " + e.getMessage());
            }

            // 2) 回退：用模块 APK 的 native 库目录绝对路径加载，绕开寄生 ClassLoader 限制
            String dir = HostInfo.moduleNativeLibraryDir;
            if (dir != null && !dir.isEmpty()) {
                File so = new File(dir, System.mapLibraryName(LIB_NAME));
                if (so.exists()) {
                    try {
                        System.load(so.getAbsolutePath());
                        loaded = true;
                        return true;
                    } catch (Throwable e) {
                        LogUtils.e(TAG, "System.load(" + so.getAbsolutePath() + ") failed: " + e.getMessage());
                    }
                } else {
                    LogUtils.e(TAG, "libdexkit.so not found at: " + so.getAbsolutePath());
                }
            } else {
                LogUtils.e(TAG, "moduleNativeLibraryDir is empty, cannot load libdexkit.so");
            }
            return false;
        }
    }

    public static boolean isLoaded() {
        return loaded;
    }
}
