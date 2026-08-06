package me.lengyu.qedge.utils.dexkit

import me.lengyu.qedge.utils.reflect.*
import org.luckypray.dexkit.query.base.BaseFinder
import java.lang.reflect.Method

interface DexKitTask {

    fun getQueryMap(): Map<String, BaseFinder>

    /** 当前宿主是否需要执行此查找任务，默认 true，TIM/QQ 专属功能可覆写 */
    fun isApplicable(): Boolean = true

    fun requireClass(name: String): Class<*> {
        return DexKitCache.getClass("${TAG}->$name")
    }

    fun requireMethod(name: String): Method {
        return DexKitCache.getMethod("${TAG}->$name")
    }
}