package me.lengyu.qedge.utils.dexkit

import me.lengyu.qedge.utils.HostInfo
import me.lengyu.qedge.utils.LogUtils
import org.json.JSONObject
import org.luckypray.dexkit.wrap.DexClass
import org.luckypray.dexkit.wrap.DexMethod
import java.io.File
import java.lang.reflect.Method

object DexKitCache {

    /**
     * 缓存值：该 key 本次查找已尝试但无法解析。
     * 用于把「没查过」和「查过但没结果」区分开：否则这类 key 会让 validateAllTasks
     * 每次都判定缓存不完整，导致每个进程每次启动都触发一次全量重扫。
     */
    private const val NOT_FOUND = ""

    var cacheMap = mutableMapOf<String, String>()

    private val cacheFile by lazy {
        File(
            "${HostInfo.getModuleDataPath()}dexkit",
            "CacheMap_${HostInfo.versionCode}_${me.lengyu.qedge.BuildConfig.VERSION_CODE}"
        )
    }

    @JvmStatic
    fun put(key: String, descriptor: String) {
        cacheMap[key] = descriptor
    }

    /** 标记该 key 已查找但无法解析（仅在尚无结果时写入），供 validateAllTasks 判定缓存完整性 */
    @JvmStatic
    fun putUnresolved(key: String) {
        if (!cacheMap.containsKey(key)) cacheMap[key] = NOT_FOUND
    }

    @JvmStatic
    fun getDescriptor(key: String): String? {
        return cacheMap[key]?.takeIf { it.isNotEmpty() }
    }

    @JvmStatic
    fun contains(key: String): Boolean {
        return cacheMap.containsKey(key)
    }

    @JvmStatic
    fun clear() {
        cacheMap.clear()
    }

    @JvmStatic
    fun getClass(key: String): Class<*> {
        return getDescriptor(key)?.let {
            DexClass(it).getInstance(me.lengyu.qedge.utils.ReflectUtils.hostClassLoader)
        } ?: throw ClassNotFoundException(key)
    }

    @JvmStatic
    fun getMethod(key: String): Method {
        return getDescriptor(key)?.let {
            DexMethod(it).getMethodInstance(me.lengyu.qedge.utils.ReflectUtils.hostClassLoader)
        } ?: throw NoSuchMethodException(key)
    }

    /**
     * 缓存文件是否已存在。仅一次 stat，供冷启动分支判断使用：
     * 无缓存时必须在主线程赶在 SplashActivity 创建前挂上查找 hook，不能延后到后台线程再判断。
     */
    @JvmStatic
    fun hasCacheFile(): Boolean = cacheFile.exists()

    @JvmStatic
    fun initCache(): Boolean {
        if (!cacheFile.exists()) return false
        return runCatching {
            val content = cacheFile.readText()
            val jsonObject = JSONObject(content)
            val keys = jsonObject.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                cacheMap[key] = jsonObject.getString(key)
            }
            // LogUtils.d("DexKitCache", "Loaded ${cacheMap.size} entries from cache")
            true
        }.onFailure {
            LogUtils.e("DexKitCache", "initCache failed: ${it.message}")
        }.getOrDefault(false)
    }

    @JvmStatic
    fun saveCache(): Boolean {
        return runCatching {
            val parentDir = cacheFile.parentFile
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs()
            }
            val jsonObject = JSONObject()
            cacheMap.forEach { (key, value) ->
                jsonObject.put(key, value)
            }
            val content = jsonObject.toString(4)
            // 原子写：先写临时文件再 rename。QQ 多进程都会执行 find 并写同一缓存文件，
            // 直接 writeText 会在进程间产生竞态，可能把半写/中间态覆写进磁盘，
            // 导致下次启动 initCache 读到残缺结果（"有时方法不足，删缓存又完整"）。
            // renameTo 在同一目录下是原子的，读取方只会看到完整文件或旧完整文件。
            val tmp = File(parentDir, cacheFile.name + ".tmp")
            tmp.writeText(content)
            if (!tmp.renameTo(cacheFile)) {
                // rename 失败（极少见）回退直接写，保证缓存仍能落地
                cacheFile.writeText(content)
                tmp.delete()
            }
            true
        }.onFailure {
            LogUtils.e("DexKitCache", "saveCache failed: ${it.message}")
        }.getOrDefault(false)
    }

    @JvmStatic
    fun validateAllTasks(): Boolean {
        val hookItems = runCatching {
            Class.forName("me.lengyu.qedge.hook.base.HookRegistry")
                .getMethod("getHookItems")
                .invoke(null) as List<*>
        }.getOrDefault(emptyList<Any>())

        for (item in hookItems) {
            // 与 DexKitFinder.startFind 保持一致：不适用的任务（TIM/QQ 专属）本来就不查，
            // 不能要求它的 key 存在于缓存，否则校验会恒为 false
            if (item is DexKitTask && item.isApplicable()) {
                val tagField = runCatching {
                    item.javaClass.getDeclaredField("TAG").apply { isAccessible = true }
                }.getOrNull() ?: continue
                val tag = tagField.get(item) as? String ?: continue
                for (key in item.getQueryMap().keys) {
                    val cacheKey = "$tag->$key"
                    if (!cacheMap.containsKey(cacheKey)) {
                        return false
                    }
                }
            }
        }
        return true
    }
}
