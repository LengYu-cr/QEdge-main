package me.lengyu.qedge.utils.reflect

import me.lengyu.qedge.utils.HostInfo

object ClassUtils {
    private var _hostClassLoader: ClassLoader? = null

    var hostClassLoader: ClassLoader
        get() = _hostClassLoader ?: throw IllegalStateException("HostClassLoader not initialized")
        set(value) {
            _hostClassLoader = value
        }

    val moduleClassLoader: ClassLoader by lazy { this::class.java.classLoader!! }

    fun loadClassOrNull(className: String): Class<*>? {
        return runCatching { hostClassLoader.loadClass(className) }.getOrNull()
    }

    fun loadClass(className: String): Class<*> {
        return hostClassLoader.loadClass(className)
    }
}

val String.clazz: Class<*>?
    get() = ClassUtils.loadClassOrNull(this)

val String.toClass: Class<*>
    get() = ClassUtils.loadClass(this)