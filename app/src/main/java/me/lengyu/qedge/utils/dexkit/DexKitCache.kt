package me.lengyu.qedge.utils.dexkit

import me.lengyu.qedge.hook.item.QZoneLikeTool
import me.lengyu.qedge.utils.HostInfo
import me.lengyu.qedge.utils.LogUtils
import me.lengyu.qedge.utils.qq.TroopTool
import me.lengyu.qedge.utils.reflect.TAG
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

    /**
     * 宿主 APK 指纹在缓存文件中的保留 key，不参与查找结果读写。
     * 同一 QQ 版本可能存在不同安装包（官方包 / 壳包），versionCode 无法区分，
     * 指纹不一致说明 APK 换了，旧结果（尤其是壳包扫出来的全空串）必须作废。
     */
    private const val FINGERPRINT_KEY = "__host_apk__"

    var cacheMap = mutableMapOf<String, String>()

    private val cacheFile by lazy {
        File(
            "${HostInfo.getModuleDataPath()}dexkit",
            "CacheMap_${HostInfo.versionCode}_${me.lengyu.qedge.BuildConfig.VERSION_CODE}"
        )
    }

    /** 宿主 APK 路径 + 大小 + 修改时间，安装/更新/换包都会变化 */
    private fun hostFingerprint(): String {
        val apk = HostInfo.getHostContext()?.applicationInfo?.sourceDir ?: return ""
        val file = File(apk)
        return "${file.absolutePath}:${file.length()}:${file.lastModified()}"
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
            // 取不到宿主 APK 信息时不做指纹校验，避免误删有效缓存
            val fingerprint = hostFingerprint()
            if (fingerprint.isNotEmpty() && jsonObject.optString(FINGERPRINT_KEY) != fingerprint) {
                LogUtils.i("DexKitCache", "host apk changed, drop stale cache")
                clearAll()
                return@runCatching false
            }
            val keys = jsonObject.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                if (key == FINGERPRINT_KEY) continue
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
            jsonObject.put(FINGERPRINT_KEY, hostFingerprint())
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

    /**
     * 本宿主需要查找的全部缓存 key，与 DexKitFinder.startFind 的任务集合完全一致：
     * HookRegistry 里的 DexKitTask + TroopTool + QZoneLikeTool，均按 isApplicable() 过滤。
     * key 的拼接方式也必须与 startFind 一致（tag 取类简单名）。
     */
    @JvmStatic
    fun expectedKeys(): List<String> {
        val tasks = mutableListOf<DexKitTask>()
        runCatching {
            Class.forName("me.lengyu.qedge.hook.base.HookRegistry")
                .getMethod("getHookItems")
                .invoke(null) as List<*>
        }.onFailure {
            LogUtils.e("DexKitCache", "read HookRegistry failed: ${it.message}")
        }.getOrNull()?.let { tasks += it.filterIsInstance<DexKitTask>() }

        tasks += TroopTool
        tasks += QZoneLikeTool

        return tasks.filter { it.isApplicable() }
            .flatMap { task -> task.getQueryMap().keys.map { "${task.TAG}->$it" } }
    }

    /**
     * 清空内存与磁盘缓存（下次查找会全量重扫）。
     * 缓存里没有的 key 与查过但标记为 NOT_FOUND（空串）的 key，都算缺失。
     */
    @JvmStatic
    fun clearAll() {
        cacheMap.clear()
        runCatching { cacheFile.delete() }
            .onFailure { LogUtils.e("DexKitCache", "clearAll failed: ${it.message}") }
    }

    @JvmStatic
    fun validateAllTasks(): Boolean = expectedKeys().all { cacheMap.containsKey(it) }
}
