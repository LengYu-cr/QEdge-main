package me.lengyu.qedge.hook.item

import me.lengyu.qedge.hook.annotation.HookItemAnnotation
import me.lengyu.qedge.hook.base.BaseApiHookItem
import me.lengyu.qedge.hook.base.Listener
import me.lengyu.qedge.utils.HookUtils
import me.lengyu.qedge.utils.LogUtils
import me.lengyu.qedge.utils.ModuleConfig
import me.lengyu.qedge.utils.ReflectUtils
import me.lengyu.qedge.utils.dexkit.DexKitTask
import org.luckypray.dexkit.query.FindMethod
import org.luckypray.dexkit.query.base.BaseFinder
import java.lang.reflect.Method
import java.lang.reflect.Modifier

/**
 * @Author 冷雨
 * @Description 分屏允许扫码。
 *
 * 扫描入口（消息列表"+"面板的扫一扫）打开前会先调用一个静态方法判断是否处于分屏：
 * 非分屏返回 true 放行；分屏则弹 Toast 拦截并返回 false。该判定方法的类名/方法名均被混淆
 * （反编译原型为 com.tencent.mobileqq.activity.recent.q#g(QBaseActivity)），因此不硬编码：
 *   1. DexKit 用扫描路由字符串 "/qrscan/scanner" + k 的特征签名定位到发起扫码的类（q）；
 *   2. 反射在该类中取唯一的静态 boolean 单参方法，即分屏判定方法 g；
 *   3. hook 后强制返回 true，使分屏/小窗下也能打开扫一扫。
 *
 * 另外扫一扫 Activity（com.tencent.mobileqq.olympic.activity.QQScanActivity，Manifest 声明类名稳定）
 * 使用的 isInMultiWindow() 继承自父类，分屏下会走到另一套逻辑导致扫不出，因此沿继承链定位到
 * 真正声明该方法的类一并 hook，强制对扫一扫 Activity 返回 false。
 *
 * 钩子无条件安装，开关在回调里每次读取，改动即时生效。
 */
@HookItemAnnotation(value = "分屏允许扫码", category = "item")
object SplitScreenScan : BaseApiHookItem<Listener>(), DexKitTask {

    const val TAG = "SplitScreenScan"

    private const val KEY_SCAN_LAUNCH_METHOD = "scan_launch_method"

    /** 目标所在包（QQ 包名稳定，用于缩小 DexKit 检索范围） */
    private const val QQ_PACKAGE = "com.tencent.mobileqq.activity.recent"



    /** 判定方法唯一参数类型：QQ 的 QBaseActivity（类名未被混淆） */
    private const val Q_BASE_ACTIVITY = "com.tencent.mobileqq.app.QBaseActivity"

    /** 扫码路由（QQ 源码中 k() 里的字面量，稳定） */
    private const val SCAN_ROUTE = "/qrscan/scanner"

    /** 扫一扫 Activity（Manifest 声明，类名未被混淆） */
    private const val SCAN_ACTIVITY = "com.tencent.mobileqq.olympic.activity.QQScanActivity"

    private fun isEnabled() = ModuleConfig.getBoolean("split_screen_scan", false)

    override fun getQueryMap(): Map<String, BaseFinder> = mapOf(
        KEY_SCAN_LAUNCH_METHOD to FindMethod().apply {
            searchPackages(QQ_PACKAGE)
            matcher {
                // k(QBaseActivity, long, long, int) 中调起了扫一扫路由
                usingStrings(SCAN_ROUTE)
                returnType("void")
                paramTypes(Q_BASE_ACTIVITY, "long", "long", "int")
            }
        }
    )

    override fun loadHook() {
        hookScanLaunchCheck()
        hookScanActivityMultiWindow()
    }

    /** 发起扫码前的分屏判定方法（q#g）→ 强制返回 true */
    private fun hookScanLaunchCheck() {
        val launchMethod: Method = try {
            requireMethod(KEY_SCAN_LAUNCH_METHOD)
        } catch (e: Throwable) {
            LogUtils.e(TAG, "DexKit resolve error: ${e.message}")
            return
        }

        val checkMethod = findSplitScreenCheckMethod(launchMethod.declaringClass) ?: return

        HookUtils.hookBefore(checkMethod) { param ->
            if (!isEnabled()) return@hookBefore
            param.setResult(true)
        }
    }

    /** 扫一扫 Activity 继承来的 isInMultiWindow() → 对扫一扫 Activity 强制返回 false */
    private fun hookScanActivityMultiWindow() {
        try {
            val classLoader = ReflectUtils.hostClassLoader ?: javaClass.classLoader
            val scanActivity = classLoader.loadClass(SCAN_ACTIVITY)
            // 该方法继承自父类，需沿继承链向上找到真正声明它的类
            val method = findMethodUpwards(scanActivity, "isInMultiWindow", 0)
            if (method == null) {
                LogUtils.e(TAG, "no isInMultiWindow() in ${scanActivity.name} hierarchy")
                return
            }
            method.isAccessible = true
            // LogUtils.i(TAG, "isInMultiWindow declared in ${method.declaringClass.name}")
            HookUtils.hookBefore(method) { param ->
                if (!isEnabled()) return@hookBefore
                // 声明类可能是 QQ 的公共父类，必须限定只对扫一扫 Activity 生效
                val self = param.thisObject
                if (self == null || !scanActivity.isInstance(self)) return@hookBefore
                param.setResult(false)
            }
        } catch (e: Throwable) {
            LogUtils.e(TAG, "hook QQScanActivity.isInMultiWindow failed: ${e.message}")
        }
    }

    /** 沿继承链向上查找声明指定签名方法的类（含本类，最先命中即声明类） */
    private fun findMethodUpwards(clazz: Class<*>, name: String, paramCount: Int): Method? {
        var current: Class<*>? = clazz
        while (current != null) {
            current.declaredMethods.firstOrNull {
                it.name == name && it.parameterCount == paramCount
            }?.let { return it }
            current = current.superclass
        }
        return null
    }

    /**
     * 在发起扫码的类中反射取唯一的「静态 + boolean + 单参(QBaseActivity)」方法（即 g）。
     * 出现 0 个或多个候选时放弃，避免误伤同类其它方法。
     */
    private fun findSplitScreenCheckMethod(clazz: Class<*>): Method? {
        val candidates = clazz.declaredMethods.filter { m ->
            Modifier.isStatic(m.modifiers) &&
                !m.isSynthetic &&
                m.returnType == Boolean::class.javaPrimitiveType &&
                m.parameterCount == 1 &&
                m.parameterTypes[0].name == Q_BASE_ACTIVITY
        }
        if (candidates.size != 1) {
            LogUtils.e(TAG, "expect 1 candidate in ${clazz.name}, but got ${candidates.size}")
            return null
        }
        candidates[0].isAccessible = true
        return candidates[0]
    }
}
