package me.lengyu.qedge.utils

import android.annotation.SuppressLint
import android.content.Context
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
}