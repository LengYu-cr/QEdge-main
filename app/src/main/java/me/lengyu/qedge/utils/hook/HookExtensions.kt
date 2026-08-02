package me.lengyu.qedge.utils.hook

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import me.lengyu.qedge.hook.base.BaseHookItem
import java.lang.reflect.Member

fun Member.hookAfter(
    owner: BaseHookItem? = null,
    block: (XC_MethodHook.MethodHookParam) -> Unit
): XC_MethodHook.Unhook {
    return XposedBridge.hookMethod(this, object : XC_MethodHook() {
        override fun afterHookedMethod(param: MethodHookParam) {
            block(param)
        }
    })
}

fun Member.hookBefore(
    owner: BaseHookItem? = null,
    block: (XC_MethodHook.MethodHookParam) -> Unit
): XC_MethodHook.Unhook {
    return XposedBridge.hookMethod(this, object : XC_MethodHook() {
        override fun beforeHookedMethod(param: MethodHookParam) {
            block(param)
        }
    })
}

fun Member.hookReplace(
    owner: BaseHookItem? = null,
    block: (XC_MethodHook.MethodHookParam) -> Any?
): XC_MethodHook.Unhook {
    return XposedBridge.hookMethod(this, object : XC_MethodReplacement() {
        override fun replaceHookedMethod(param: XC_MethodHook.MethodHookParam): Any? {
            return block(param)
        }
    })
}

fun XC_MethodHook.MethodHookParam.invokeOriginal(): Any? {
    return XposedBridge.invokeOriginalMethod(method, thisObject, args)
}