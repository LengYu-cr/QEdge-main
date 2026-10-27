package me.lengyu.qedge.utils.dexkit

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import dalvik.system.BaseDexClassLoader
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodHook.MethodHookParam
import de.robv.android.xposed.XposedHelpers
import me.lengyu.qedge.common.ModuleScope
import me.lengyu.qedge.hook.MainHook
import me.lengyu.qedge.hook.base.HookRegistry
import me.lengyu.qedge.ui.components.dialogs.CenterDialogContainerNoButton
import me.lengyu.qedge.ui.core.compatibility.XposedComposeDialog
import me.lengyu.qedge.ui.core.theme.QEdgeTheme
import me.lengyu.qedge.utils.HostInfo
import me.lengyu.qedge.utils.LogUtils
import me.lengyu.qedge.utils.ReflectUtils
import me.lengyu.qedge.utils.Toasts
import me.lengyu.qedge.utils.reflect.*
import me.lengyu.qedge.utils.hook.hookAfter
import me.lengyu.qedge.utils.qq.TroopTool
import me.lengyu.qedge.hook.item.QZoneLikeTool
import org.luckypray.dexkit.DexKitBridge
import org.luckypray.dexkit.query.FindClass
import org.luckypray.dexkit.query.FindMethod
import org.luckypray.dexkit.query.base.BaseFinder

object DexKitFinder {

    private const val SPLASH_ACTIVITY = "com.tencent.mobileqq.activity.SplashActivity"

    private var progressText by mutableStateOf("QEdge准备开始查找...")
    private var isFindComplete by mutableStateOf(false)
    private var splashHookFired = false
    private var dialogRef: XposedComposeDialog? = null

    /** 重新查找完成后的回调（首页「模块配置」用它刷新缓存状态），仅 refind 期间有值 */
    private var onFindFinished: (() -> Unit)? = null

    /**
     * 清除当前 DexKit 缓存并立即重新查找方法（首页「模块配置」按钮）。
     * 查找期间复用启动时的进度对话框，完成后在主线程回调 [onFinished]。
     * 已有查找在进行时返回 false（此时不会回调），避免调用方一直等待。
     */
    @JvmStatic
    fun refind(context: Context, onFinished: () -> Unit): Boolean {
        if (dialogRef?.isShowing == true) {
            Toasts.toast("正在查找方法，请稍候")
            return false
        }
        onFindFinished = onFinished
        isFindComplete = false
        progressText = "QEdge准备开始查找..."
        DexKitCache.clearAll()
        showFindDialogInternal(context)
        return true
    }

    @JvmStatic
    fun doFind() {
        // DexKit 必须优先于 hook：
        //  - registerHookItems() 只把 hook 项注册进 HookRegistry（供下方 startFind 收集全部 DexKitTask），
        //    但不执行任何 onHook；
        //  - 查找完成后（方法已就绪）再由 startFind 末尾调用 MainHook.loadHook() 真正执行 onHook。
        // 避免"方法还没找到就直接 hook"导致的功能缺失/时序错乱。
        me.lengyu.qedge.hook.MainHook.registerHookItems()
        showFindDialog()
    }

    @Suppress("DEPRECATION")
    private fun showFindDialog() {
        // QQ 的 Activity 基类走 doOnCreate 模板方法；9.3.70 起可能不再经过它，
        // 因此额外兜底 Activity 生命周期 onCreate，两者取先触发者。
        hookSplashMethod("doOnCreate")
        hookSplashMethod("onCreate")
    }

    private fun hookSplashMethod(name: String) {
        try {
            // 必须用宿主查找 loader（补丁 loader）解析，不能用 SplashActivity::class.java：
            // 后者走模块 loader 的 parent 链，解析到的是基础包里那个根本没在跑的旧版类，
            // 结果就是 hook 安装成功但永不触发（QQ 9.3.70 的启动页在 Tinker 补丁里）。
            val clazz = ReflectUtils.findClassIfExists(SPLASH_ACTIVITY)
                ?: throw ClassNotFoundException(SPLASH_ACTIVITY)
            XposedHelpers.findAndHookMethod(
                clazz, name, Bundle::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        if (splashHookFired) {
                            return
                        }
                        splashHookFired = true
                        showFindDialogInternal(param.thisObject as Context)
                    }
                }
            )
        } catch (e: Throwable) {
            LogUtils.e("SplashActivity.$name hook 失败: " + e)
        }
    }

    private fun showFindDialogInternal(context: Context) {
        dialogRef = object : XposedComposeDialog(context) {
                    @Composable
                    override fun DialogContent() {
                        QEdgeTheme {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CenterDialogContainerNoButton(
                                    title = if (isFindComplete) "QEdge查找完成" else "QEdge查找方法中"
                                ) {
                                    val colors = QEdgeTheme.colors
                                    Text(
                                        text = progressText,
                                        fontSize = 15.sp,
                                        color = colors.textSecondary,
                                        lineHeight = 22.sp,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }.apply {
                    setCanceledOnTouchOutside(false)
                    setCancelable(false)
                    show()
                }

        startFind()
    }

    private fun startFind() {
        ModuleScope.launchIO(TAG) {
            try {
                // 在后台线程加载 dexkit so，避免阻塞宿主主线程启动。
                // 通过 DexKitManager 兜底：寄生 ClassLoader 下 loadLibrary 会失败，需绝对路径加载。
                if (!DexKitManager.ensureLibrary()) {
                    LogUtils.e(TAG, "dexkit lib unavailable, abort find")
                    progressText = "libdexkit.so 加载失败"
                    abortFind()
                    return@launchIO
                }

                val tasks = HookRegistry.getHookItems().filterIsInstance<DexKitTask>().toMutableList().apply {
                    add(TroopTool)
                    add(QZoneLikeTool)
                }.filter { it.isApplicable() }

                val sourceDir = HostInfo.getHostContext()?.applicationInfo?.sourceDir
                if (sourceDir == null) {
                    LogUtils.e(TAG, "sourceDir is null")
                    progressText = "宿主 sourceDir 为空，查找失败"
                    abortFind()
                    return@launchIO
                }

                // 第一源：宿主 APK 本身。正常安装包一次即可全部命中。
                progressText = "正在读取宿主 APK..."
                scanSource("host apk", tasks) { DexKitBridge.create(sourceDir) }

                // 第二源：运行时已加载的 dex。壳包的外壳 APK 只有几 KB，
                // 真实代码在运行时加载的 dex 里，只能从 ClassLoader 读到。
                if (hasUnresolved(tasks)) {
                    scanRuntimeDex(tasks)
                }

                // 所有来源都扫完再统一留痕，避免单源扫不到就写入空串把缓存锁死
                markUnresolved(tasks)
                progressText = "查找完成，正在初始化..."
                DexKitCache.saveCache()
                isFindComplete = true
                val finished = onFindFinished
                onFindFinished = null
                Handler(Looper.getMainLooper()).post {
                    dialogRef?.dismiss()
                    finished?.invoke()
                }
                // 缓存已就绪，Hook 注册与安装交给后台调度器，不占用主线程
                ModuleScope.launchHookJava("HookInit") {
                    MainHook.loadHook()
                }
            } catch (e: Throwable) {
                // 查找任意环节抛异常都必须收尾，否则进度弹窗常驻、「重建缓存」永转圈、Hook 永不安装
                LogUtils.e(TAG, e)
                progressText = "查找失败，请重试"
                abortFind()
            }
        }
    }

    /** 打开一个来源并扫描未命中的 key；单个来源失败不影响后续来源 */
    private fun scanSource(label: String, tasks: List<DexKitTask>, create: () -> DexKitBridge) {
        runCatching { create().use { scanBridge(it, tasks) } }
            .onFailure { LogUtils.e(TAG, "$label scan failed: $it") }
    }

    /** 只扫描尚未解析的 key；多个来源依次兜底时避免重复查询 */
    private fun scanBridge(bridge: DexKitBridge, tasks: List<DexKitTask>) {
        tasks.forEach { task ->
            runCatching {
                task.getQueryMap().forEach query@{ (name, query) ->
                    val key = "${task.TAG}->$name"
                    if (DexKitCache.contains(key)) return@query
                    queryOne(bridge, key, query)
                }
            }.onFailure { LogUtils.e(task.TAG, it) }
        }
    }

    private fun queryOne(bridge: DexKitBridge, key: String, query: BaseFinder) {
        when (query) {
            is FindClass -> {
                val descriptor = bridge.findClass(query).firstOrNull()?.descriptor
                if (descriptor != null) {
                    progressText = key
                    DexKitCache.put(key, descriptor)
                } else {
                    LogUtils.e(TAG, "No class found for: $key")
                }
            }

            is FindMethod -> {
                val descriptor = bridge.findMethod(query).firstOrNull()?.descriptor
                if (descriptor != null) {
                    progressText = key
                    DexKitCache.put(key, descriptor)
                } else {
                    LogUtils.e(TAG, "No method found for: $key")
                }
            }
        }
    }

    /**
     * 沿宿主 ClassLoader 的 parent 链逐个读取已加载的 dex（含内存 dex）。
     * 壳包的真实代码可能挂在当前 loader 的 pathList 上，也可能在其父 loader 上，两条都要覆盖。
     * 仅在第一个来源未全部命中时才执行，正常安装包不会走到这里。
     */
    private fun scanRuntimeDex(tasks: List<DexKitTask>) {
        progressText = "正在读取运行时 dex..."
        var current: ClassLoader? = ReflectUtils.hostClassLoader
        var depth = 0
        while (current != null && depth < 8 && hasUnresolved(tasks)) {
            val loader = current
            if (loader is BaseDexClassLoader) {
                scanSource("runtime dex #$depth", tasks) { DexKitBridge.create(loader, true) }
            }
            current = loader.parent
            depth++
        }
    }

    private fun hasUnresolved(tasks: List<DexKitTask>): Boolean = tasks.any { task ->
        runCatching {
            task.getQueryMap().keys.any { !DexKitCache.contains("${task.TAG}->$it") }
        }.getOrDefault(false)
    }

    /** 所有来源扫完后统一留痕：查过但没命中，避免 validateAllTasks 每次都判缓存不完整 */
    private fun markUnresolved(tasks: List<DexKitTask>) {
        tasks.forEach { task ->
            runCatching {
                task.getQueryMap().keys.forEach { DexKitCache.putUnresolved("${task.TAG}->$it") }
            }.onFailure { LogUtils.e(task.TAG, it) }
        }
    }

    /** 查找异常结束：关掉进度弹窗并在主线程回调，避免「重新查找」按钮永远转圈 */
    private fun abortFind() {
        val finished = onFindFinished
        onFindFinished = null
        Handler(Looper.getMainLooper()).post {
            dialogRef?.dismiss()
            finished?.invoke()
        }
    }
}
