package me.lengyu.qedge.lifecycle

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Instrumentation
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.AssetManager
import android.content.res.Resources
import android.content.res.loader.ResourcesLoader
import android.content.res.loader.ResourcesProvider
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.os.PersistableBundle
import de.robv.android.xposed.XposedBridge
import me.lengyu.qedge.BuildConfig
import me.lengyu.qedge.R
import me.lengyu.qedge.activity.SettingActivity
import me.lengyu.qedge.utils.HostInfo
import me.lengyu.qedge.utils.reflect.ClassUtils
import me.lengyu.qedge.utils.reflect.callMethod
import me.lengyu.qedge.utils.reflect.callStaticMethod
import me.lengyu.qedge.utils.reflect.findMethod
import me.lengyu.qedge.utils.reflect.getObject
import me.lengyu.qedge.utils.reflect.getObjectByTypeOrNull
import me.lengyu.qedge.utils.reflect.getStaticObject
import me.lengyu.qedge.utils.reflect.setObject
import me.lengyu.qedge.utils.reflect.toClass
import java.io.File
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Proxy
/**
 * @Author 冷雨
 * @Description 活动寄生类
 */
@SuppressLint("DiscouragedPrivateApi", "PrivateApi")
@Suppress("DEPRECATION")
object Parasitics {
    private const val STUB_DEFAULT_ACTIVITY =
        "com.tencent.mobileqq.activity.photo.CameraPreviewActivity"
    private const val ACTIVITY_PROXY_INTENT = "ACTIVITY_PROXY_INTENT"

    private val moduleLoader by lazy { ClassUtils.moduleClassLoader }
    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }

    private object ModulePathHolder {
        @JvmStatic
        var modulePath: String? = null
    }

    private object ResourcesLoaderHolderApi30 {
        var sResourcesLoader: ResourcesLoader? = null
    }

    private var initialized = false

    private fun isTargetActivity(className: String?): Boolean {
        if (className == null) return false
        if (DynamicActivityRegistry.contains(className)) return true
        if (!className.startsWith(BuildConfig.APPLICATION_ID)) return false

        return runCatching {
            val targetClass = moduleLoader.loadClass(className)
            SettingActivity::class.java.isAssignableFrom(targetClass)
        }.getOrElse { false }
    }

    fun setModulePath(path: String?) {
        ModulePathHolder.modulePath = path
    }

    private fun getModulePath(): String? = ModulePathHolder.modulePath
    @JvmStatic
    fun initForStubActivity(ctx: Context) {
        runCatching {
            val activityThreadClass = "android.app.ActivityThread".toClass
            val currentActivityThread =
                activityThreadClass.callStaticMethod("currentActivityThread") ?: return

            val instrumentation =
                currentActivityThread.getObject("mInstrumentation") as Instrumentation
            if (instrumentation !is ProxyInstrumentation) {
                currentActivityThread.setObject(
                    "mInstrumentation",
                    ProxyInstrumentation(instrumentation)
                )
            }

            val mH = currentActivityThread.getObject("mH") as Handler
            val originalCallback = mH.getObjectByTypeOrNull<Handler.Callback>(Handler::class.java)

            mH.setObject("mCallback", Handler.Callback { msg ->
                val handledObj = when (msg.what) {
                    100 -> msg.obj
                    159 -> msg.obj
                    else -> null
                }
                handledObj?.let { handleLaunchMessage(it, msg.what) }
                originalCallback?.handleMessage(msg) ?: false
            }, Handler::class.java)

            hookIActivityManager()
            hookIPackageManager(ctx, currentActivityThread)
            
            initialized = true
        }
    }

    fun ensureInitialized(ctx: Context) {
        if (!initialized) {
            initForStubActivity(ctx)
        } else {
            runCatching {
                val activityThreadClass = "android.app.ActivityThread".toClass
                val currentActivityThread =
                    activityThreadClass.callStaticMethod("currentActivityThread") ?: return
                
                val instrumentation =
                    currentActivityThread.getObject("mInstrumentation") as Instrumentation
                if (instrumentation !is ProxyInstrumentation) {
                    currentActivityThread.setObject(
                        "mInstrumentation",
                        ProxyInstrumentation(instrumentation)
                    )
                }
                
                hookIActivityManager()
                hookIPackageManager(ctx, currentActivityThread)
            }
        }
    }

    private fun hookIActivityManager() {
        val singletonInstance = resolveActivityManagerSingleton() ?: return
        val mInstance = singletonInstance.getObject("mInstance", "android.util.Singleton".toClass)

        val iamInterface = if (Build.VERSION.SDK_INT >= 29) {
            "android.app.IActivityTaskManager".toClass
        } else {
            "android.app.IActivityManager".toClass
        }

        val proxy = Proxy.newProxyInstance(moduleLoader, arrayOf(iamInterface)) { _, method, args ->
            if ("asBinder" == method.name) {
                return@newProxyInstance (mInstance as android.os.IInterface).asBinder()
            }
            
            if ("startActivity" == method.name) {
                args?.indexOfFirst { it is Intent }?.takeIf { it != -1 }?.let { index ->
                    val raw = args[index] as Intent
                    val component = raw.component
                    if (component != null && HostInfo.packageName == component.packageName &&
                        isTargetActivity(component.className)
                    ) {
                        args[index] = Intent().apply {
                            setClassName(component.packageName, STUB_DEFAULT_ACTIVITY)
                            putExtra(ACTIVITY_PROXY_INTENT, raw)
                            flags = raw.flags
                        }
                    }
                }
            }
            invokeOriginal(mInstance, method, args)
        }

        singletonInstance.setObject("mInstance", proxy, "android.util.Singleton".toClass)
    }

    private fun hookIPackageManager(ctx: Context, currentActivityThread: Any) {
        val sPackageManager = currentActivityThread.getObject("sPackageManager")
        val ipmInterface = "android.content.pm.IPackageManager".toClass

        val proxy = Proxy.newProxyInstance(
            ipmInterface.classLoader,
            arrayOf(ipmInterface)
        ) { _, method, args ->
            if ("getActivityInfo" == method.name && args?.isNotEmpty() == true) {
                val component = args[0] as? ComponentName
                if (component != null && HostInfo.packageName == component.packageName &&
                    isTargetActivity(component.className)
                ) {
                    val flags = (args[1] as? Number)?.toLong() ?: 0L
                    return@newProxyInstance CounterfeitActivityInfoFactory.makeProxyActivityInfo(
                        component.className,
                        flags
                    )
                }
            }
            invokeOriginal(sPackageManager, method, args)
        }

        currentActivityThread.setObject("sPackageManager", proxy)
        ctx.packageManager.setObject("mPM", proxy)
    }

    private fun invokeOriginal(target: Any, method: Method, args: Array<Any?>?): Any? {
        return try {
            method.invoke(target, *args.orEmpty())
        } catch (e: InvocationTargetException) {
            throw e.targetException
        }
    }

    private fun resolveActivityManagerSingleton(): Any? {
        return runCatching {
            val atmClass = "android.app.ActivityTaskManager".toClass
            val singleton = atmClass.getStaticObject("IActivityTaskManagerSingleton")
            "android.util.Singleton".toClass.findMethod { name = "get" }.invoke(singleton)
            singleton
        }.recoverCatching {
            "android.app.ActivityManager".toClass.getStaticObject("IActivityManagerSingleton")
        }.recoverCatching {
            "android.app.ActivityManagerNative".toClass.getStaticObject("gDefault")
        }.getOrNull()
    }

    private fun handleLaunchMessage(obj: Any, what: Int) {
        if (what == 100) {
            val intent = obj.getObject("intent") as? Intent ?: return
            unwrapIntent(intent)?.let { obj.setObject("intent", it) }
        } else if (what == 159) {
            val callbacks =
                runCatching { obj.callMethod("getCallbacks") as? List<*> }.getOrNull() ?: return
            callbacks.forEach { item ->
                if (item != null && item.javaClass.name.contains("LaunchActivityItem")) {
                    val intent = item.getObject("mIntent") as? Intent ?: return
                    val original = unwrapIntent(intent) ?: return
                    
                    item.setObject("mIntent", original)

                    if (Build.VERSION.SDK_INT >= 31) {
                        fixActivityClientRecordForApi31(obj, original)
                    }
                }
            }
        }
    }

    private fun fixActivityClientRecordForApi31(transaction: Any, originalIntent: Intent) {
        runCatching {
            val token = transaction.callMethod("getActivityToken") as? IBinder ?: return
            val activityThread =
                "android.app.ActivityThread".toClass.callStaticMethod("currentActivityThread")
            val acr = activityThread?.callMethod("getLaunchingActivity", token) ?: return
            acr.setObject("intent", originalIntent)
        }
    }

    private fun unwrapIntent(intent: Intent): Intent? {
        return runCatching {
            val clone = intent.clone() as Intent
            clone.extras?.classLoader = ClassUtils.hostClassLoader
            if (clone.hasExtra(ACTIVITY_PROXY_INTENT)) {
                clone.getParcelableExtra<Intent>(ACTIVITY_PROXY_INTENT)?.apply {
                    extras?.classLoader = moduleLoader
                }
            } else {
                null
            }
        }.getOrNull()
    }

    fun injectModuleResources(res: Resources?) {
        if (res == null || runCatching { res.getString(R.string.app_name) }.isSuccess) return
        val path = getModulePath() ?: return

        if (Build.VERSION.SDK_INT >= 30) {
            val loader = ResourcesLoaderHolderApi30.sResourcesLoader ?: run {
                runCatching {
                    val pfd =
                        ParcelFileDescriptor.open(File(path), ParcelFileDescriptor.MODE_READ_ONLY)
                    val provider = ResourcesProvider.loadFromApk(pfd)
                    ResourcesLoader().apply { addProvider(provider) }
                }.getOrNull()?.also { ResourcesLoaderHolderApi30.sResourcesLoader = it }
            }

            val task = Runnable {
                try {
                    loader?.let { res.addLoaders(it) }
                } catch (e: IllegalArgumentException) {
                    if (e.message?.contains("Cannot modify resource loaders") == true) injectResourcesBelowApi30(
                        res,
                        path
                    )
                }
            }
            if (Looper.myLooper() == Looper.getMainLooper()) task.run() else mainHandler.post(task)
        } else {
            injectResourcesBelowApi30(res, path)
        }
    }

    private fun injectResourcesBelowApi30(res: Resources, path: String) {
        runCatching {
            AssetManager::class.java.findMethod {
                name = "addAssetPath"; paramTypes(String::class.java)
            }
                .invoke(res.assets, path)
        }
    }

    private class ProxyInstrumentation(private val mBase: Instrumentation) : Instrumentation() {
        override fun newActivity(cl: ClassLoader, className: String, intent: Intent): Activity {
            if (DynamicActivityRegistry.contains(className)) {
                DynamicActivityRegistry.getActivityClass(className)?.let { return it.newInstance() }
            }
            return try {
                mBase.newActivity(cl, className, intent)
            } catch (e: ClassNotFoundException) {
                Parasitics::class.java.classLoader!!.loadClass(className).newInstance() as Activity
            }
        }

        override fun callActivityOnCreate(activity: Activity, icicle: Bundle?) {
            injectModuleResources(activity.resources)
            if (icicle != null && isTargetActivity(activity.javaClass.name)) icicle.classLoader =
                moduleLoader
            mBase.callActivityOnCreate(activity, icicle)
        }

        override fun callActivityOnCreate(
            activity: Activity,
            icicle: Bundle?,
            persistentState: PersistableBundle?
        ) {
            injectModuleResources(activity.resources)
            if (icicle != null && isTargetActivity(activity.javaClass.name)) icicle.classLoader =
                moduleLoader
            mBase.callActivityOnCreate(activity, icicle, persistentState)
        }
    }
}
