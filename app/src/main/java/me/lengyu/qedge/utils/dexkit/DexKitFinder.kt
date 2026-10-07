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

            val bridge = DexKitBridge.create(sourceDir)

            bridge.use { b ->
                tasks.forEach { task ->
                    runCatching {
                        task.getQueryMap().forEach { (name, query) ->
                            when (query) {
                                is FindClass -> {
                                    val classes = b.findClass(query)
                                    classes.singleOrNull()?.let {
                                        val tip = "${task.TAG}->$name"
                                        progressText = tip
                                        DexKitCache.cacheMap[tip] = it.descriptor
                                    } ?: run {
                                        if (classes.isNotEmpty()) {
                                            val first = classes.first()
                                            val tip = "${task.TAG}->$name"
                                            progressText = tip
                                            DexKitCache.cacheMap[tip] = first.descriptor
                                        } else {
                                            LogUtils.e(TAG, "No class found for: ${task.TAG}->$name")
                                            // 留痕：否则 validateAllTasks 会认为缓存不完整，每次都重扫
                                            DexKitCache.putUnresolved("${task.TAG}->$name")
                                        }
                                    }
                                }

                                is FindMethod -> {
                                    val methods = b.findMethod(query)
                                    methods.singleOrNull()?.let {
                                        val tip = "${task.TAG}->$name"
                                        progressText = tip
                                        DexKitCache.cacheMap[tip] = it.descriptor
                                    } ?: run {
                                        if (methods.isNotEmpty()) {
                                            val first = methods.first()
                                            val tip = "${task.TAG}->$name"
                                            progressText = tip
                                            DexKitCache.cacheMap[tip] = first.descriptor
                                        } else {
                                            LogUtils.e(TAG, "No method found for: ${task.TAG}->$name")
                                            // 留痕：否则 validateAllTasks 会认为缓存不完整，每次都重扫
                                            DexKitCache.putUnresolved("${task.TAG}->$name")
                                        }
                                    }
                                }
                            }
                        }
                    }.onFailure {
                        LogUtils.e(task.TAG, it)
                        // 查询抛异常同样要留痕，否则校验会一直判定缓存不完整而重复全量扫描
                        task.getQueryMap().keys.forEach { DexKitCache.putUnresolved("${task.TAG}->$it") }
                    }
                }
            }
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
