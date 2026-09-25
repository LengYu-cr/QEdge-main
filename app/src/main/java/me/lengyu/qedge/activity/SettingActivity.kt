package me.lengyu.qedge.activity

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import me.lengyu.qedge.ui.pages.PluginData
import me.lengyu.qedge.ui.pages.HomeScreen
import me.lengyu.qedge.ui.pages.FileManagerScreen
import me.lengyu.qedge.ui.pages.coldrain.ColdRainScreen
import me.lengyu.qedge.ui.pages.coldrain.ColdRainConfig
import me.lengyu.qedge.ui.core.compatibility.QEdgeCenterDialog
import me.lengyu.qedge.ui.core.theme.QEdgeTheme
import me.lengyu.qedge.ui.services.OnlinePluginService
import me.lengyu.qedge.ui.widget.glass.GlassBackdropHost
import me.lengyu.qedge.utils.ModuleConfig
import me.lengyu.qedge.utils.LogUtils
import me.lengyu.qedge.utils.HostInfo
import java.io.File
import java.lang.reflect.Method
/**
 * @Author 冷雨
 * @Description 设置活动
 */
class SettingActivity : ComponentActivity() {

    private var isDarkTheme = false
    private var mainHookClass: Class<*>? = null
    private var pluginList: MutableList<PluginData>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        @Suppress("DEPRECATION")
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        WindowCompat.setDecorFitsSystemWindows(window, false)
        @Suppress("DEPRECATION")
        window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        setupTheme()
        // 自定义背景图生效：窗口底色提前切黑，别让主题底色在首帧前闪出来；
        // 同时后台预热解码，尽量赶在第一帧组合前把图备好
        val bgImageUri = ModuleConfig.getString("bg_image_uri", "")
        if (ModuleConfig.getBoolean("bg_image_enabled", false) && bgImageUri.isNotEmpty()) {
            window.setBackgroundDrawable(
                android.graphics.drawable.ColorDrawable(
                    me.lengyu.qedge.ui.core.theme.DarkBackground.toArgb()
                )
            )
            Thread {
                me.lengyu.qedge.ui.pages.BgImageCache.load(applicationContext, bgImageUri)
            }.start()
        }
        try {
            mainHookClass = Class.forName("me.lengyu.qedge.hook.MainHook")
        } catch (e: ClassNotFoundException) {
            LogUtils.e(e)
        }
        
        // 这里不再启动心跳：心跳由宿主进程固定调度，重复启动会立刻补发一次
        // 同步请求，进入设置页时会卡一下

        setupUI()
        attachGlassNav()
        showPendingUpdateDialog()
    }

    /**
     * 检查心跳落盘的更新数据，有更新则弹更新日志弹窗。
     * 心跳只写数据不弹窗，只有进入设置页才提示。
     */
    private fun showPendingUpdateDialog() {
        try {
            if (!me.lengyu.qedge.hook.UserData.hasUpdateInfo()) return
            val version = me.lengyu.qedge.hook.UserData.getUpdateVersion()
            me.lengyu.qedge.ui.components.dialogs.UpdateDialog(
                this,
                version,
                me.lengyu.qedge.hook.UserData.getUpdateLog(),
                me.lengyu.qedge.hook.UserData.getUpdateApk(),
                Runnable {
                    ModuleConfig.putString("ignored_version", version)
                    me.lengyu.qedge.hook.UserData.clearUpdateInfo()
                }
            ).show()
        } catch (e: Throwable) {
            LogUtils.e(e)
        }
    }

    /**
     * 把液态玻璃导航条挂到 Activity 的 content 上。
     * 必须在 setContent 之后调用：此时 content 的第一个子 View 才是 Compose 内容。
     */
    private fun attachGlassNav() {
        val contentRoot = findViewById<android.view.ViewGroup>(android.R.id.content)
        GlassBackdropHost.attach(this, contentRoot, isDarkTheme)
    }

    private fun setupTheme() {
        val userTheme = ModuleConfig.getInt("theme", -1)
        if (userTheme == -1) {
            isDarkTheme = (resources.configuration.uiMode and
                    android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                    android.content.res.Configuration.UI_MODE_NIGHT_YES
        } else {
            isDarkTheme = userTheme == 1
        }
    }

    private fun saveTheme(dark: Boolean) {
        ModuleConfig.putInt("theme", if (dark) 1 else 0)
    }
    
    private fun saveCurrentPage(page: String) {
        ModuleConfig.putString("current_page", page)
    }
    
    private fun getSavedPage(): String {
        return ModuleConfig.getString("current_page", "plugin")
    }

    private fun callMainHookMethod(methodName: String, vararg args: Any?): Any? {
        return try {
            val paramTypes = args.map { arg ->
                when (arg) {
                    is Boolean -> Boolean::class.javaPrimitiveType ?: Boolean::class.java
                    is Int -> Int::class.javaPrimitiveType ?: Int::class.java
                    is Long -> Long::class.javaPrimitiveType ?: Long::class.java
                    is Float -> Float::class.javaPrimitiveType ?: Float::class.java
                    is Double -> Double::class.javaPrimitiveType ?: Double::class.java
                    null -> Void::class.java
                    else -> arg.javaClass
                }
            }.toTypedArray()
            val method = mainHookClass?.getMethod(methodName, *paramTypes)
            val result = method?.invoke(null, *args)
            result
        } catch (e: Exception) {
            LogUtils.e("[DEBUG-PLUGIN]DP-001 ERROR: ${e.message}", e)
            LogUtils.e(e)
            null
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun getPluginList(): List<PluginData> {
        return try {
            callMainHookMethod("getPluginList") as? List<PluginData> ?: emptyList()
        } catch (e: Exception) {
            LogUtils.e(e)
            emptyList()
        }
    }

    private fun setPluginRunning(id: String, running: Boolean) {
        callMainHookMethod("setPluginRunning", id, running)
    }

    private fun setPluginAutoLoad(id: String, autoLoad: Boolean) {
        callMainHookMethod("setPluginAutoLoad", id, autoLoad)
    }

    private fun deletePlugin(id: String) {
        callMainHookMethod("deletePlugin", id)
    }

    private fun reloadPlugin(id: String) {
        callMainHookMethod("reloadPlugin", id)
    }

    private fun createPlugin() {
        callMainHookMethod("createPlugin")
    }

    private fun createPlugin(type: String, name: String, desc: String, author: String, version: String) {
        callMainHookMethod("createPlugin", type, name, desc, author, version)
    }

    private fun downloadPlugin(plugin: me.lengyu.qedge.ui.pages.home.OnlinePluginItem) {
        Thread {
            try {
                OnlinePluginService.downloadPlugin(
                    plugin.id.toString(),
                    plugin.pluginName
                ) { filePath, error, zipPath, targetPath ->
                    runOnUiThread {
                        if (error != null) {
                            if (zipPath != null && targetPath != null) {
                                showExtractErrorDialog(zipPath, targetPath)
                            } else {
                                android.widget.Toast.makeText(this, "下载失败: $error", android.widget.Toast.LENGTH_SHORT).show()
                            }
                            return@runOnUiThread
                        }

                        filePath?.let {
                            android.widget.Toast.makeText(this, "下载成功", android.widget.Toast.LENGTH_SHORT).show()
                            pluginList?.clear()
                            pluginList?.addAll(getPluginList())
                        }
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    android.widget.Toast.makeText(this, "下载失败: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun showExtractErrorDialog(zipPath: String, targetPath: String) {
        QEdgeCenterDialog(this) { onDismiss ->
            val colors = QEdgeTheme.colors
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(colors.cardBackground)
                    .padding(20.dp)
            ) {
                Text(
                    "解压插件失败",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "请手动将 ZIP 解压到目标目录（点击路径可复制）：",
                    fontSize = 14.sp,
                    color = colors.textSecondary
                )
                Spacer(modifier = Modifier.height(16.dp))

                Text("ZIP 文件路径", fontSize = 12.sp, color = colors.textSecondary)
                Text(
                    zipPath,
                    fontSize = 13.sp,
                    color = colors.textPrimary,
                    modifier = Modifier.clickable { copyToClipboard(zipPath) }
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text("目标目录", fontSize = 12.sp, color = colors.textSecondary)
                Text(
                    targetPath,
                    fontSize = 13.sp,
                    color = colors.textPrimary,
                    modifier = Modifier.clickable { copyToClipboard(targetPath) }
                )

                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("关闭", color = colors.textPrimary)
                    }
                }
            }
        }.show()
    }

    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("path", text)
        clipboard.setPrimaryClip(clip)
        android.widget.Toast.makeText(this, "已复制", android.widget.Toast.LENGTH_SHORT).show()
    }

    private fun deleteDir(dir: File): Boolean {
        if (dir.isDirectory) {
            val files = dir.listFiles()
            if (files != null) {
                for (file in files) {
                    deleteDir(file)
                }
            }
        }
        return dir.delete()
    }

    private fun uploadPlugin(plugin: me.lengyu.qedge.ui.pages.PluginData) {
        Thread {
            try {
                val pluginDir = File(plugin.dirPath)
                
                if (!pluginDir.exists() || !pluginDir.isDirectory) {
                    runOnUiThread {
                        android.widget.Toast.makeText(this, "上传的插件目录不存在", android.widget.Toast.LENGTH_SHORT).show()
                    }
                    return@Thread
                }

                val propFile = File(pluginDir, "info.prop")
                val props = java.util.Properties()
                if (propFile.exists()) {
                    java.io.InputStreamReader(
                        java.io.FileInputStream(propFile),
                        java.nio.charset.StandardCharsets.UTF_8
                    ).use { reader ->
                        props.load(reader)
                    }
                }

                val pluginName = props.getProperty("pluginName", plugin.name)
                val versionCode = props.getProperty("versionCode", plugin.version)
                val authorName = props.getProperty("author", plugin.author)
                val pluginId = props.getProperty("id", plugin.id)

                OnlinePluginService.uploadPlugin(
                    pluginId = pluginId,
                    pluginName = pluginName,
                    versionCode = versionCode,
                    authorName = authorName,
                    pluginDir = pluginDir
                ) { result, error ->
                    runOnUiThread {
                        if (error != null) {
                            android.widget.Toast.makeText(this, "上传失败: $error", android.widget.Toast.LENGTH_SHORT).show()
                        } else {
                            android.widget.Toast.makeText(this, "上传成功，等待审核中", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    android.widget.Toast.makeText(this, "上传失败: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun processDataForCurrent(action: String) {
        callMainHookMethod("processDataForCurrent", action)
    }

    @OptIn(ExperimentalAnimationApi::class)
    private fun setupUI() {
        setContent {
            val list = remember { mutableStateListOf<PluginData>() }
            list.addAll(getPluginList())
            this@SettingActivity.pluginList = list

            val initialPage = intent.getStringExtra("page") ?: getSavedPage()
            var currentPage by remember { mutableStateOf(initialPage) }

            // 液态玻璃导航条只属于模块首页，其它页面隐藏
            LaunchedEffect(currentPage) {
                GlassBackdropHost.get()?.setVisible(currentPage == "plugin")
            }

            fun refreshPlugins() {
                list.clear()
                list.addAll(getPluginList())
            }

            QEdgeTheme(darkTheme = isDarkTheme) {
                AnimatedContent(
                    targetState = currentPage,
                    transitionSpec = {
                        val forward = targetState != "plugin" && initialState == "plugin"
                        if (forward) {
                            slideInHorizontally(
                                animationSpec = tween(300, easing = androidx.compose.animation.core.Easing { x ->
                                    x * x * (3f - 2f * x)
                                }),
                                initialOffsetX = { it }
                            ) + fadeIn(animationSpec = tween(200)) togetherWith
                            slideOutHorizontally(
                                animationSpec = tween(300, easing = androidx.compose.animation.core.Easing { x ->
                                    x * x * (3f - 2f * x)
                                }),
                                targetOffsetX = { -it / 3 }
                            ) + fadeOut(animationSpec = tween(200))
                        } else {
                            slideInHorizontally(
                                animationSpec = tween(300, easing = androidx.compose.animation.core.Easing { x ->
                                    x * x * (3f - 2f * x)
                                }),
                                initialOffsetX = { -it }
                            ) + fadeIn(animationSpec = tween(200)) togetherWith
                            slideOutHorizontally(
                                animationSpec = tween(300, easing = androidx.compose.animation.core.Easing { x ->
                                    x * x * (3f - 2f * x)
                                }),
                                targetOffsetX = { it / 3 }
                            ) + fadeOut(animationSpec = tween(200))
                        }
                    },
                    contentAlignment = androidx.compose.ui.Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    when (page) {
                        "file_manager" -> FileManagerScreen(
                            isDarkTheme = isDarkTheme,
                            onThemeToggle = {
                                isDarkTheme = !isDarkTheme
                                saveTheme(isDarkTheme)
                                saveCurrentPage(currentPage)
                                recreate()
                            },
                            onBackClick = { saveCurrentPage("plugin"); currentPage = "plugin" }
                        )
                        "cold_rain" -> ColdRainScreen()
                        else -> HomeScreen(
                            plugins = list,
                            isDarkTheme = isDarkTheme,
                            onThemeToggle = {
                                isDarkTheme = !isDarkTheme
                                saveTheme(isDarkTheme)
                                saveCurrentPage(currentPage)
                                recreate()
                            },
                            onBackClick = { finish() },
                            onRunToggle = { id: String, running: Boolean ->
                                setPluginRunning(id, running)
                                refreshPlugins()
                            },
                            onAutoLoadToggle = { id: String, autoLoad: Boolean ->
                                setPluginAutoLoad(id, autoLoad)
                                refreshPlugins()
                            },
                            onDelete = { id: String ->
                                deletePlugin(id)
                                refreshPlugins()
                            },
                            onReload = { id: String ->
                                reloadPlugin(id)
                                refreshPlugins()
                            },
                            onCreateClick = {
                                // 打开新建弹窗由 HomeScreen 内部处理，这里无需操作
                            },
                            onCreatePlugin = { type, name, desc, author, version ->
                                createPlugin(type, name, desc, author, version)
                                refreshPlugins()
                            },
                            onUploadClick = { plugin ->
                                uploadPlugin(plugin)
                            },
                            onDownloadClick = { plugin ->
                                downloadPlugin(plugin)
                            },
                            onDocClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://v.yuafeng.cn/QEdge/PluginMethod/"))
                                startActivity(intent)
                            },
                            onFileManagerClick = {
                                saveCurrentPage("file_manager")
                                currentPage = "file_manager"
                            },
                            onColdRainClick = {
                                ColdRainConfig.init(this@SettingActivity)
                                saveCurrentPage("cold_rain")
                                currentPage = "cold_rain"
                            }
                        )
                    }
                }
            }
        }
    }

    

    override fun onDestroy() {
        processDataForCurrent("save")
        GlassBackdropHost.detach()
        super.onDestroy()
    }
}
