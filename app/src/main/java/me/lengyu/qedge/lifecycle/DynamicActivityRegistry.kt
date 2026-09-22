package me.lengyu.qedge.lifecycle

import android.app.Activity
import java.util.concurrent.ConcurrentHashMap
/**
 * @Author 冷雨
 * @Description 动态活动注册类
 */
object DynamicActivityRegistry {
    
    private val registry = ConcurrentHashMap<String, Class<out Activity>>()

    @Suppress("UNCHECKED_CAST")
    @JvmStatic
    fun register(clazz: Class<*>) {
        registry[clazz.name] = clazz as Class<out Activity>
    }

    @JvmStatic
    fun unregister(className: String) {
        registry.remove(className)
    }

    @JvmStatic
    fun contains(className: String): Boolean {
        return registry.containsKey(className)
    }

    @JvmStatic
    fun getActivityClass(className: String): Class<out Activity>? {
        return registry[className]
    }
}