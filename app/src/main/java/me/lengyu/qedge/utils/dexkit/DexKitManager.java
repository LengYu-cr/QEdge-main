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
 * <p>本类提供全局幂等的加载入口：优先用模块 APK 的 native 库绝对路径 {@code System.load()}，
 * 失败再退化到常规 {@code loadLibrary}。加载结果缓存，全进程只真正加载一次。
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
            // 1) 优先：用模块 APK 的 native 库目录绝对路径加载。
            // 寄生 ClassLoader（FPAClassLoader）的 nativeLibraryDirectories 指向
            // "base.apk!/lib/<abi>"，so 被压缩进 APK 后该路径无法再直接映射，loadLibrary 必然失败；
            // 而 ApplicationInfo.nativeLibraryDir 指向安装时解压出来的真实目录，稳定可用。
            String dir = HostInfo.moduleNativeLibraryDir;
            if (dir != null && !dir.isEmpty()) {
                File so = new File(dir, System.mapLibraryName(LIB_NAME));
                if (so.exists()) {
                    try {
                        System.load(so.getAbsolutePath());
                        loaded = true;
                        return true;
                    } catch (Throwable e) {
                        LogUtils.w(TAG, "System.load(" + so.getAbsolutePath() + ") failed: " + e.getMessage());
                    }
                }
            }

            // 2) 兜底：常规 loadLibrary，正常进程上下文下可用
            try {
                System.loadLibrary(LIB_NAME);
                loaded = true;
                return true;
            } catch (Throwable e) {
                LogUtils.e(TAG, "loadLibrary failed" + (dir == null || dir.isEmpty()
                        ? ", moduleNativeLibraryDir is empty" : ", dir=" + dir) + ": " + e.getMessage());
            }
            return false;
        }
    }

    public static boolean isLoaded() {
        return loaded;
    }
}
