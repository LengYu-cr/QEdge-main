package me.lengyu.qedge.utils.reflect

import java.lang.reflect.Field
import java.lang.reflect.Method
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

    @JvmStatic
    fun findMethodOrNull(clazz: Class<*>?, methodName: String?): Method? {
        return searchUpwards(clazz) { it.findMethodOrNull { name = methodName } }
    }

    @JvmStatic
    fun findMethodOrNull(clazz: Class<*>?, methodName: String?, types: Array<Class<*>?>?): Method? {
        return searchUpwards(clazz) { it.findMethodOrNull { name = methodName; paramTypes = types } }
    }

    @JvmStatic
    fun findMethodOrNull(clazz: Class<*>?, methodName: String?, count: Int): Method? {
        return searchUpwards(clazz) { it.findMethodOrNull { name = methodName; paramCount = count } }
    }

    @JvmStatic
    fun findMethodOrNull(clazz: Class<*>?, type: Class<*>?, types: Array<Class<*>?>?): Method? {
        return searchUpwards(clazz) { it.findMethodOrNull { returnType = type; paramTypes = types } }
    }

    @JvmStatic
    fun findFieldOrNull(clazz: Class<*>?, fieldName: String?): Field? {
        if (fieldName.isNullOrEmpty()) return null
        return searchUpwards(clazz) { it.findFieldOrNull { name = fieldName } }
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
}

val String.clazz: Class<*>?
    get() = ClassUtils.loadClassOrNull(this)

val String.toClass: Class<*>
    get() = ClassUtils.loadClass(this)