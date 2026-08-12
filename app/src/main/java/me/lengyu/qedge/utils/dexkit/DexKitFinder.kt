package me.lengyu.qedge.utils.dexkit

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.tencent.mobileqq.activity.SplashActivity
import me.lengyu.qedge.common.ModuleScope
import me.lengyu.qedge.hook.MainHook
import me.lengyu.qedge.hook.base.HookRegistry
import me.lengyu.qedge.ui.components.dialogs.CenterDialogContainerNoButton
import me.lengyu.qedge.ui.core.compatibility.XposedComposeDialog
import me.lengyu.qedge.ui.core.theme.QEdgeTheme
import me.lengyu.qedge.utils.HostInfo
import me.lengyu.qedge.utils.LogUtils
import me.lengyu.qedge.utils.reflect.*
import me.lengyu.qedge.utils.hook.hookAfter
import me.lengyu.qedge.utils.qq.TroopTool
import me.lengyu.qedge.hook.item.QZoneLikeTool
import org.luckypray.dexkit.DexKitBridge
import org.luckypray.dexkit.query.FindClass
import org.luckypray.dexkit.query.FindMethod
import org.luckypray.dexkit.query.base.BaseFinder

object DexKitFinder {

    private var progressText by mutableStateOf("QEdge准备开始查找...")
    private var isFindComplete by mutableStateOf(false)
    private var dialogRef: XposedComposeDialog? = null

    @JvmStatic
    fun doFind() {
        System.loadLibrary("dexkit")
        me.lengyu.qedge.hook.MainHook.registerHookItems()
        showFindDialog()
    }

    @Suppress("DEPRECATION")
    private fun showFindDialog() {
        SplashActivity::class.java
            .getDeclaredMethod("doOnCreate", Bundle::class.java)
            .hookAfter {
                val context = it.thisObject as Context

                dialogRef = object : XposedComposeDialog(context) {
                    override fun configureWindow() {
                        super.configureWindow()
                        window?.apply {
                            clearFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                            setDimAmount(0f)
                        }
                    }

                    @Composable
                    override fun DialogContent() {
                        QEdgeTheme {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0x80000000)),
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
    }

    private fun startFind() {
        ModuleScope.launchIO(TAG) {
            val tasks = HookRegistry.getHookItems().filterIsInstance<DexKitTask>().toMutableList().apply {
                add(TroopTool)
                add(QZoneLikeTool)
            }.filter { it.isApplicable() }

            val sourceDir = HostInfo.getHostContext()?.applicationInfo?.sourceDir
            if (sourceDir == null) {
                LogUtils.e(TAG, "sourceDir is null")
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
                                        }
                                    }
                                }
                            }
                        }
                    }.onFailure { LogUtils.e(task.TAG, it) }
                }
            }
            progressText = "查找完成，正在初始化..."
            DexKitCache.saveCache()
            isFindComplete = true
            Handler(Looper.getMainLooper()).post {
                dialogRef?.dismiss()
                MainHook.loadHook()
            }
        }
    }
}
