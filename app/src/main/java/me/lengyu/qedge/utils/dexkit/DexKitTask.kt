package me.lengyu.qedge.utils.dexkit

import me.lengyu.qedge.utils.reflect.*
import org.luckypray.dexkit.query.base.BaseQuery
import java.lang.reflect.Method

interface DexKitTask {

    fun getQueryMap(): Map<String, BaseQuery>

    fun requireClass(name: String): Class<*> {
        return DexKitCache.getClass("${TAG}->$name")
    }

    fun requireMethod(name: String): Method {
        return DexKitCache.getMethod("${TAG}->$name")
    }
}