package me.lengyu.qedge.utils.reflect

import java.lang.reflect.Field
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap
import me.lengyu.qedge.utils.HostInfo
import me.lengyu.qedge.utils.ReflectUtils

/**
 * @Author 冷雨
 * @Description 类工具类
 */
object ClassUtils {

    /**
     * 宿主 ClassLoader 的唯一存储点是 ReflectUtils.hostClassLoader：
     * 该字段被大量旧代码直接读取，注入 HybridClassLoader 的逻辑也在那边。
     * 这里只做读写转发，避免两处各存一份导致取值不一致。
     */
    var hostClassLoader: ClassLoader
        get() = ReflectUtils.hostClassLoader ?: throw IllegalStateException("HostClassLoader not initialized")
        set(value) {
            ReflectUtils.initClassLoader(value)
        }

    val moduleClassLoader: ClassLoader by lazy { this::class.java.classLoader!! }

    fun loadClassOrNull(className: String): Class<*>? {
        return runCatching { hostClassLoader.loadClass(className) }.getOrNull()
    }

    fun loadClass(className: String): Class<*> {
        return hostClassLoader.loadClass(className)
    }

    // ---- 给 Java 侧（ReflectUtils 门面）用的无 lambda 重载 ----
    // Kotlin 的 findMethodOrNull{} / findFieldOrNull{} 要求带接收者的 lambda，Java 调用很难看，
    // 这里提供纯参数版本，并沿父类链回退（原 ReflectUtils 只扫本类 declared，找不到父类私有成员）。

    /** 缓存结果上限，超过后整体清空，避免长期运行时无界增长 */
    private const val MAX_CACHE_ENTRIES = 4096

    /** 「查过但没找到」的哨兵，避免每次重新走父类链 */
    private val NOT_FOUND = Any()

    /**
     * 最终查找结果缓存：key 含 Class 与查找条件。
     * 高频回调（每个气泡的 handleUIState）此前每次都要重建父类链集合、再逐类扫 declaredMethods，
     * 这里命中后只剩一次哈希查表，O(1)。
     */
    private val methodCache = ConcurrentHashMap<MethodKey, Any>()
    private val fieldCache = ConcurrentHashMap<FieldKey, Any>()

    @JvmStatic
    fun findMethodOrNull(clazz: Class<*>?, methodName: String?): Method? {
        return cachedMethod(clazz, methodName, null, null, -1) { c ->
            searchUpwards(c) { it.findMethodOrNull { name = methodName } }
        }
    }

    @JvmStatic
    fun findMethodOrNull(clazz: Class<*>?, methodName: String?, types: Array<Class<*>?>?): Method? {
        return cachedMethod(clazz, methodName, types, null, -1) { c ->
            searchUpwards(c) { it.findMethodOrNull { name = methodName; paramTypes = types } }
        }
    }

    @JvmStatic
    fun findMethodOrNull(clazz: Class<*>?, methodName: String?, count: Int): Method? {
        return cachedMethod(clazz, methodName, null, null, count) { c ->
            searchUpwards(c) { it.findMethodOrNull { name = methodName; paramCount = count } }
        }
    }

    @JvmStatic
    fun findMethodOrNull(clazz: Class<*>?, type: Class<*>?, types: Array<Class<*>?>?): Method? {
        return cachedMethod(clazz, null, types, type, -1) { c ->
            searchUpwards(c) { it.findMethodOrNull { returnType = type; paramTypes = types } }
        }
    }

    @JvmStatic
    fun findFieldOrNull(clazz: Class<*>?, fieldName: String?): Field? {
        if (clazz == null || fieldName.isNullOrEmpty()) return null
        val key = FieldKey(clazz, fieldName)
        val cached = fieldCache[key]
        if (cached != null) return if (cached === NOT_FOUND) null else cached as Field

        val found = searchUpwards(clazz) { it.findFieldOrNull { name = fieldName } }
        if (fieldCache.size >= MAX_CACHE_ENTRIES) fieldCache.clear()
        fieldCache.putIfAbsent(key, found ?: NOT_FOUND)
        return found
    }

    private inline fun cachedMethod(
        clazz: Class<*>?,
        name: String?,
        types: Array<Class<*>?>?,
        returnType: Class<*>?,
        paramCount: Int,
        find: (Class<*>) -> Method?
    ): Method? {
        if (clazz == null) return null
        val key = MethodKey(clazz, name, types, returnType, paramCount)
        val cached = methodCache[key]
        if (cached != null) return if (cached === NOT_FOUND) null else cached as Method

        val found = find(clazz)
        if (methodCache.size >= MAX_CACHE_ENTRIES) methodCache.clear()
        methodCache.putIfAbsent(key, found ?: NOT_FOUND)
        return found
    }

    /** 从 clazz 起沿父类链、父接口链逐层查找，找到即返回；都没有则返回 null */
    private inline fun <T> searchUpwards(clazz: Class<*>?, find: (Class<*>) -> T?): T? {
        for (current in hierarchyOf(clazz)) {
            val found = find(current)
            if (found != null) return found
        }
        return null
    }

    /** 自身 → 父类链 → 接口链（接口没有父类，靠 interfaces 继续向上） */
    private fun hierarchyOf(clazz: Class<*>?): List<Class<*>> {
        if (clazz == null) return emptyList()
        val result = ArrayList<Class<*>>()
        val visited = HashSet<Class<*>>()
        fun visit(current: Class<*>?) {
            if (current == null || current == Any::class.java || !visited.add(current)) return
            result.add(current)
            visit(current.superclass)
            current.interfaces.forEach { visit(it) }
        }
        visit(clazz)
        return result
    }

    /** 方法查找缓存键：哈希在构造时算好，查表只剩一次比较 */
    private class MethodKey(
        private val clazz: Class<*>,
        private val name: String?,
        private val types: Array<Class<*>?>?,
        private val returnType: Class<*>?,
        private val paramCount: Int
    ) {
        private val hash: Int = run {
            var h = clazz.hashCode()
            h = 31 * h + (name?.hashCode() ?: 0)
            h = 31 * h + (returnType?.hashCode() ?: 0)
            h = 31 * h + paramCount
            val t = types
            if (t != null) {
                for (type in t) h = 31 * h + (type?.hashCode() ?: 0)
            }
            h
        }

        override fun hashCode(): Int = hash

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is MethodKey) return false
            return hash == other.hash &&
                    clazz === other.clazz &&
                    name == other.name &&
                    returnType === other.returnType &&
                    paramCount == other.paramCount &&
                    types.contentEquals(other.types)
        }
    }

    private class FieldKey(
        private val clazz: Class<*>,
        private val name: String
    ) {
        private val hash = 31 * clazz.hashCode() + name.hashCode()

        override fun hashCode(): Int = hash

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is FieldKey) return false
            return clazz === other.clazz && name == other.name
        }
    }
}

val String.clazz: Class<*>?
    get() = ClassUtils.loadClassOrNull(this)

val String.toClass: Class<*>
    get() = ClassUtils.loadClass(this)
