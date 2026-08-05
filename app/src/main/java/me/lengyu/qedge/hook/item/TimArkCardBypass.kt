package me.lengyu.qedge.hook.item

import me.lengyu.qedge.hook.annotation.HookItemAnnotation
import me.lengyu.qedge.hook.base.BaseApiHookItem
import me.lengyu.qedge.hook.base.Listener
import me.lengyu.qedge.utils.HookUtils
import me.lengyu.qedge.utils.HostInfo
import me.lengyu.qedge.utils.LogUtils
import me.lengyu.qedge.utils.ModuleConfig
import me.lengyu.qedge.utils.ReflectUtils
import me.lengyu.qedge.utils.dexkit.DexKitTask
import me.lengyu.qedge.utils.reflect.findMethod
import me.lengyu.qedge.utils.reflect.findMethodOrNull
import org.luckypray.dexkit.DexKitBridge
import org.luckypray.dexkit.query.FindClass
import org.luckypray.dexkit.query.base.BaseFinder
import dalvik.system.DexFile
import java.lang.reflect.Method

@HookItemAnnotation(value = "TIM卡片阻断绕过", category = "item")
object TimArkCardBypass : BaseApiHookItem<TimArkCardBypass.TimArkCardBypassListener>(), DexKitTask {

    const val TAG = "TimArkCardBypass"
    private const val CONFIG_KEY = "tim_ark_card_bypass"
    private const val KEY_ARK_CONFIG_MODEL = "ArkConfigModel"
    private const val ARK_PACKAGE_PREFIX = "com.tencent.mobileqq.aio.msglist.holder.component.ark"
    private const val STRING_WHITELIST_UPDATE = "updateTimArkClickWhiteList"

    @JvmStatic
    fun isEnabled(): Boolean {
        if (!HostInfo.isTIM) return false
        return ModuleConfig.getBoolean(CONFIG_KEY, true)
    }

    override fun getQueryMap(): Map<String, BaseFinder> = mapOf(
        KEY_ARK_CONFIG_MODEL to FindClass().apply {
            searchPackages(ARK_PACKAGE_PREFIX)
            matcher {
                usingStrings(
                    "tim_ark_msg_valid_config",
                    STRING_WHITELIST_UPDATE,
                    "ArkConfigModel"
                )
            }
        }
    )

    /**
     * 三级查找 ArkConfigModel 类:
     * 1. DexKitCache (DexKitFinder 启动时提前预扫并存入的缓存,最快)
     * 2. DexKitBridge.create(sourceDir) 实时扫 APK dex (可靠,跨版本通用)
     * 3. ClassLoader.dexElements 反射扫 ark 包 (兜底,兼容一切版本)
     */
    private fun findArkConfigModelClass(): Class<*>? {
        val classLoader = try {
            Thread.currentThread().contextClassLoader ?: TimArkCardBypass.javaClass.classLoader
        } catch (t: Throwable) {
            LogUtils.e(TAG, "findArkConfigModelClass: getClassLoader error ${t.message}")
            return null
        }
        if (classLoader == null) return null

        // 1. DexKit 缓存优先: 与 DexKitTask.requireClass 机制保持一致
        try {
            val cached = runCatching { requireClass(KEY_ARK_CONFIG_MODEL) }.getOrNull()
            if (cached != null) {
                LogUtils.d(TAG, "findArkConfigModelClass: DexKitCache 命中 -> ${cached.name}")
                return cached
            }
        } catch (_: Throwable) { }

        // 2. 实时 DexKit 扫 APK: 直接用字符串 updateTimArkClickWhiteList 定位类
        try {
            val sourceDir = HostInfo.getHostContext()?.applicationInfo?.sourceDir
            if (sourceDir != null) {
                try { System.loadLibrary("dexkit") } catch (_: Throwable) { }
                val bridge = DexKitBridge.create(sourceDir)
                    try {
                        val hits = bridge.findClass(FindClass().apply {
                            searchPackages(ARK_PACKAGE_PREFIX)
                            matcher { usingStrings(STRING_WHITELIST_UPDATE) }
                        })
                        for (hit in hits) {
                            val clazz = runCatching { Class.forName(hit.name, false, classLoader) }.getOrNull()
                            if (clazz != null) {
                                // LogUtils.d(TAG, "findArkConfigModelClass: DexKit 实时扫命中 -> ${clazz.name}")
                                return clazz
                            }
                        }
                    } finally {
                        runCatching { bridge.close() }
                    }
            }
        } catch (t: Throwable) {
            LogUtils.e(TAG, "findArkConfigModelClass: DexKit 实时扫异常: ${t.message}")
        }

        // 3. 反射兜底: 扫 ark 包下所有类,找到含 String,String→boolean 方法的类即可(优先带 updateTimArkClickWhiteList 字符串特征或方法数量匹配)
        val allClasses = scanArkPackageClassesByReflection(classLoader)
        var best: Class<*>? = null
        var bestScore = 0
        for (clazz in allClasses) {
            val m = clazz.findMethodOrNull {
                returnType = boolean
                paramTypes(String::class.java, String::class.java)
            }
            if (m != null) {
                // 类里命中 (String,String)Z 的方法; 进一步看类的 declaredMethods 里 boolean 方法数多不多,多的更像是 ArkConfigModel 白名单判断器
                val booleanCount = clazz.declaredMethods.count { it.returnType == Boolean::class.javaPrimitiveType }
                val score = 100 + booleanCount
                if (score > bestScore) {
                    bestScore = score
                    best = clazz
                }
            }
        }
        if (best != null) {
            // LogUtils.d(TAG, "findArkConfigModelClass: 反射兜底命中 -> ${best.name}, score=$bestScore")
            return best
        }
        LogUtils.e(TAG, "findArkConfigModelClass: 三级查找全部失败,无法定位 ArkConfigModel")
        return null
    }

    /**
     * 通过反射遍历 ClassLoader.dexElements,枚举 ark 包下所有类(兜底用).
     */
    @Suppress("DEPRECATION")
    private fun scanArkPackageClassesByReflection(classLoader: ClassLoader): List<Class<*>> {
        val result = mutableListOf<Class<*>>()
        val seen = hashSetOf<String>()
        val visited = hashSetOf<ClassLoader>()
        var current: ClassLoader? = classLoader
        while (current != null && !visited.contains(current)) {
            visited.add(current)
            val pathList = runCatching { ReflectUtils.getFieldValue(current, "pathList") }.getOrNull()
            if (pathList != null) {
                val dexElements = runCatching { ReflectUtils.getFieldValue(pathList, "dexElements") as? Array<*> }.getOrNull()
                if (dexElements != null) {
                    var i = 0
                    while (i < dexElements.size) {
                        val element = dexElements[i]
                        i++
                        if (element == null) continue
                        var dexFile: DexFile? = runCatching { ReflectUtils.getFieldValue(element, "dexFile") as? DexFile }.getOrNull()
                        if (dexFile == null) {
                            val path = runCatching { ReflectUtils.getFieldValue(element, "path") as? String }.getOrNull()
                            if (path != null && (path.endsWith(".dex") || path.endsWith(".apk") || path.endsWith(".jar"))) {
                                dexFile = runCatching { DexFile(path) }.getOrNull()
                            }
                        }
                        if (dexFile == null) continue
                        runCatching {
                            val entries = dexFile.entries()
                            while (entries.hasMoreElements()) {
                                val cn = entries.nextElement() ?: continue
                                if (!cn.startsWith(ARK_PACKAGE_PREFIX)) continue
                                if (cn.contains("\$Proxy") || cn.contains("$$") || seen.contains(cn)) continue
                                seen.add(cn)
                                val c = runCatching { Class.forName(cn, false, current) }.getOrNull()
                                if (c != null) result.add(c)
                            }
                        }
                    }
                }
            }
            current = current.parent
        }
        return result
    }

    override fun loadHook() {
        try {
            if (!HostInfo.isTIM) return
            if (!isEnabled()) return

            val arkConfigModelClass = findArkConfigModelClass()
                ?: run {
                    LogUtils.e(TAG, "loadHook 失败: 无法定位 ArkConfigModel 类")
                    return
                }

            // 精确匹配 QFun BypassArkLimit 里同样的方法签名: boolean method(String,String)
            val checkMethod: Method? = arkConfigModelClass.findMethodOrNull {
                returnType = boolean
                paramTypes(String::class.java, String::class.java)
            }
            if (checkMethod == null) {
                LogUtils.e(TAG, "loadHook 失败: 在 ${arkConfigModelClass.name} 中未找到 (String,String)→boolean 白名单检查方法")
                return
            }
            checkMethod.isAccessible = true

            // HookAfter 原判断完成后强制 true,保持 QFun 一致的语义:仅修改白名单判断结果.
            HookUtils.hookAfter(checkMethod) { param ->
                if (!isEnabled()) return@hookAfter
                val ret = param.result
                if (ret != true) param.result = true
            }

            // LogUtils.d(TAG, "loadHook success → 精准定位白名单判断方法 ${arkConfigModelClass.simpleName}.${checkMethod.name}(String,String)Z,强制返回 true 绕过 TIM 卡片阻断")
        } catch (e: Throwable) {
            LogUtils.e(TAG, "loadHook error: ${e.message}")
        }
    }

    interface TimArkCardBypassListener : Listener
}
