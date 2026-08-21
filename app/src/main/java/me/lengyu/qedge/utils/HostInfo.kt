package me.lengyu.qedge.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.Environment

@Suppress("DEPRECATION")
@SuppressLint("StaticFieldLeak")
object HostInfo {
    const val PACKAGE_NAME_QQ = "com.tencent.mobileqq"
    const val PACKAGE_NAME_TIM = "com.tencent.tim"

    private var _hostContext: Context? = null
    private var _moduleDataPath: String = ""

    @JvmField
    var packageName: String = ""

    @JvmField
    var moduleVersionName: String = ""

    @JvmField
    var moduleVersionCode: Long = 0

    @JvmField
    var processName: String = ""

    var hostName: String = "Host App"
        private set

    val isQQ: Boolean
        get() = packageName == PACKAGE_NAME_QQ
    val isTIM: Boolean
        get() = packageName == PACKAGE_NAME_TIM

    val isInHostProcess: Boolean
        get() = isQQ || isTIM

    @JvmField
    var versionCode: Long = 0
    @JvmField
    var versionName: String = ""

    /** 模块自身 APK 的 native 库目录，用于用绝对路径加载 so（寄生 ClassLoader 下无法 loadLibrary）。 */
    @JvmField
    var moduleNativeLibraryDir: String = ""

    @JvmStatic
    fun init(context: Context) {
        _hostContext = context

        runCatching {
            val pm = context.packageManager
            val info = pm.getPackageInfo(context.packageName, 0)
            versionCode =
                if (Build.VERSION.SDK_INT > 28)
                    info.longVersionCode
                else
                    info.versionCode.toLong()
            versionName = info.versionName.orEmpty()
            val appInfo = pm.getApplicationInfo(packageName, 0)
            hostName = pm.getApplicationLabel(appInfo).toString()

            val moduleInfo = pm.getPackageInfo("me.lengyu.qedge", 0)
            moduleVersionCode =
                if (Build.VERSION.SDK_INT > 28)
                    moduleInfo.longVersionCode
                else
                    moduleInfo.versionCode.toLong()
            moduleVersionName = moduleInfo.versionName.orEmpty()

            // 记录模块 APK 的 native 库目录，供 DexKitManager 用绝对路径加载 libdexkit.so
            moduleNativeLibraryDir =
                pm.getApplicationInfo("me.lengyu.qedge", 0).nativeLibraryDir.orEmpty()
        }

        val externalDir = context.getExternalFilesDir(null)?.parentFile
        _moduleDataPath = externalDir?.let { "${it.absolutePath}/QEdge/" }
            ?: "${Environment.getExternalStorageDirectory().absolutePath}/Android/data/${context.packageName}/QEdge/"
        
        //android.util.Log.i("QEdge", "HostInfo.init: moduleDataPath = $_moduleDataPath")
    }

    @JvmStatic
    fun getHostContext(): Context? = _hostContext

    @JvmStatic
    fun getContext(): Context? = _hostContext

    @JvmStatic
    fun setHostContext(context: Context) {
        _hostContext = context
    }

    @JvmStatic
    fun getModuleDataPath(): String = _moduleDataPath

    /**
     * 判断系统是否处于暗色模式（DarkModeManager.k() 同款逻辑）
     * Android 10 (API 29) 及以上才支持暗色模式
     */
    @JvmStatic
    fun isSystemDarkModeSupported(): Boolean = Build.VERSION.SDK_INT >= 29

    /**
     * 判断系统当前是否为暗色模式（DarkModeManager.l() 同款逻辑）
     * 对应 (uiMode & UI_MODE_NIGHT_MASK) == UI_MODE_NIGHT_YES
     */
    @JvmStatic
    fun isSystemDarkMode(): Boolean {
        val context = _hostContext ?: return false
        val uiMode = context.resources.configuration.uiMode
        return (uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    }

    /**
     * 判断 QQ 当前主题是否为夜间模式（DarkModeManager.l() 同款逻辑）
     * 通过反射调用 ThemeUtil.isNowThemeIsNight()
     */
    @JvmStatic
    fun isHostNightTheme(): Boolean {
        return try {
            val themeUtil = Class.forName("com.tencent.mobileqq.vas.theme.api.ThemeUtil")
            val method = themeUtil.getDeclaredMethod("isNowThemeIsNight",
                Class.forName("mqq.app.AppRuntime"),
                java.lang.Boolean.TYPE,
                String::class.java
            )
            method.invoke(null, null, false, null) as Boolean
        } catch (_: Throwable) {
            false
        }
    }

    /**
     * 综合判断当前是否为暗色主题
     * 优先跟随QQ主题，ThemeUtil调用失败时回退到系统暗色模式
     */
    @JvmStatic
    fun isDarkTheme(): Boolean {
        if (!isSystemDarkModeSupported()) return false
        return try {
            val themeUtil = Class.forName("com.tencent.mobileqq.vas.theme.api.ThemeUtil")
            val method = themeUtil.getDeclaredMethod("isNowThemeIsNight",
                Class.forName("mqq.app.AppRuntime"),
                java.lang.Boolean.TYPE,
                String::class.java
            )
            method.invoke(null, null, false, null) as Boolean
        } catch (_: Throwable) {
            isSystemDarkMode()
        }
    }
}