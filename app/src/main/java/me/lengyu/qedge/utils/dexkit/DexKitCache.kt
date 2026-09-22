package me.lengyu.qedge.utils.dexkit

import me.lengyu.qedge.utils.HostInfo
import me.lengyu.qedge.utils.LogUtils
import org.json.JSONObject
import org.luckypray.dexkit.wrap.DexClass
import org.luckypray.dexkit.wrap.DexMethod
import java.io.File
import java.lang.reflect.Method

object DexKitCache {

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

    @JvmStatic
    fun getDescriptor(key: String): String? {
        return cacheMap[key]
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
        return cacheMap[key]?.let {
            DexClass(it).getInstance(me.lengyu.qedge.utils.ReflectUtils.hostClassLoader)
        } ?: throw ClassNotFoundException(key)
    }

    @JvmStatic
    fun getMethod(key: String): Method {
        return cacheMap[key]?.let {
            DexMethod(it).getMethodInstance(me.lengyu.qedge.utils.ReflectUtils.hostClassLoader)
        } ?: throw NoSuchMethodException(key)
    }

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
            if (item is DexKitTask) {
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
