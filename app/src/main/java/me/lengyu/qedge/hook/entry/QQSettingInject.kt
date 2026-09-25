package me.lengyu.qedge.hook.entry

import android.app.Activity
import android.content.Context
import android.content.Intent
import me.lengyu.qedge.R
import me.lengyu.qedge.activity.SettingActivity
import me.lengyu.qedge.hook.annotation.HookItemAnnotation
import me.lengyu.qedge.hook.base.BaseApiHookItem
import me.lengyu.qedge.hook.base.Listener
import me.lengyu.qedge.lifecycle.Parasitics
import me.lengyu.qedge.utils.HostInfo
import me.lengyu.qedge.utils.LogUtils
import me.lengyu.qedge.utils.dexkit.DexKitTask
import me.lengyu.qedge.utils.hook.hookAfter
import me.lengyu.qedge.utils.reflect.ClassUtils
import me.lengyu.qedge.utils.reflect.clazz
import me.lengyu.qedge.utils.reflect.findMethod
import me.lengyu.qedge.utils.reflect.toClass
import org.luckypray.dexkit.query.FindClass
import org.luckypray.dexkit.query.base.BaseFinder
import java.lang.reflect.Proxy
/**
 * @Author 冷雨
 * @Description QQ设置入口
 */
@HookItemAnnotation(category = "entry", process = "All", value = "QQ设置入口")
object QQSettingInject : BaseApiHookItem<Listener>(), DexKitTask {

    private const val TOP_TITLE = "QEdge"
    private const val BOTTOM_TITLE = "送君茉莉，愿君莫离。"
    private const val MODULE_ORDER = 6

    @Suppress("UNCHECKED_CAST")
    override fun loadHook() {

        val providerClass =
            if (HostInfo.isQQ && HostInfo.versionCode >= 12288) requireClass("provider")
            else "com.tencent.mobileqq.setting.main.NewSettingConfigProvider".clazz
                ?: "com.tencent.mobileqq.setting.main.MainSettingConfigProvider".clazz

        if (providerClass == null) throw ClassNotFoundException("SettingConfigProvider")

        val simpleItemProcessorClass = runCatching {
            requireClass("simpleItemProcessor")
        }.getOrNull()

        val sectionClass = runCatching {
            requireClass("settingSection")
        }.getOrNull()

        providerClass.findMethod {
            returnType = list
            paramTypes(context)
        }.hookAfter(this) { param ->
            val context = param.args[0] as Context
            val result = param.result as MutableList<Any>

            val secClass = sectionClass ?: if (result.isNotEmpty()) result[0].javaClass else null
            val procClass = simpleItemProcessorClass ?: findSimpleItemProcessorFromList(result)

            if (procClass != null && secClass != null) {
                val clickLambda = makeClickLambda(context, SettingActivity::class.java)
                val processor = createProcessor(procClass, context, clickLambda)
                val newSection = createSection(secClass, processor)
                result.add(1, newSection)
            }
        }
    }

    private fun findSimpleItemProcessorFromList(sections: List<Any>): Class<*>? {
        for (section in sections) {
            val result = runCatching {
                val listMethod = section.javaClass.declaredMethods.firstOrNull {
                    it.returnType == List::class.java && it.parameterTypes.isEmpty()
                }
                listMethod?.isAccessible = true
                val list = listMethod?.invoke(section) as? List<*>
                if (list != null) {
                    for (item in list) {
                        if (item != null) {
                            val cls = item.javaClass
                            if (isSimpleItemProcessor(cls)) {
                                return@runCatching cls
                            }
                            val superCls = cls.superclass
                            if (superCls != null && isSimpleItemProcessor(superCls)) {
                                val found = findConcreteItemProcessorClass(superCls)
                                if (found != null) {
                                    return@runCatching found
                                }
                            }
                        }
                    }
                }
                null
            }
            if (result.isSuccess && result.getOrNull() != null) {
                return result.getOrThrow()
            }
        }
        return null
    }

    private fun isSimpleItemProcessor(clazz: Class<*>): Boolean {
        val result = runCatching {
            val pkg = clazz.`package`?.name
            if (pkg == null || !pkg.startsWith("com.tencent.mobileqq.setting.processor")) {
                false
            } else {
                clazz.declaredConstructors.any { cons ->
                    val params = cons.parameterTypes
                    params.size >= 3 &&
                        params[0] == Context::class.java &&
                        params[1] == Int::class.java &&
                        CharSequence::class.java.isAssignableFrom(params[2])
                }
            }
        }
        return result.getOrDefault(false)
    }

    private fun findConcreteItemProcessorClass(baseClass: Class<*>): Class<*>? {
        val result = runCatching {
            val pkg = baseClass.`package`?.name
            if (pkg == null) {
                null
            } else {
                val loader = ClassUtils.hostClassLoader
                var found: Class<*>? = null
                val simpleNames = listOf("i", "h", "g", "f", "e", "d", "j", "k")
                for (name in simpleNames) {
                    runCatching {
                        val cls = Class.forName("$pkg.$name", false, loader)
                        if (baseClass.isAssignableFrom(cls) && isSimpleItemProcessor(cls)) {
                            found = cls
                        }
                    }
                    if (found != null) break
                }
                found
            }
        }
        return result.getOrNull()
    }

    private fun createProcessor(
        processorClass: Class<*>,
        context: Context,
        clickLambda: Any
    ): Any {
        val constructor = processorClass.declaredConstructors.firstOrNull { cons ->
            val params = cons.parameterTypes
            params.size >= 3 &&
                params[0] == Context::class.java &&
                params[1] == Int::class.java &&
                CharSequence::class.java.isAssignableFrom(params[2])
        } ?: throw NoSuchMethodException("SimpleItemProcessor constructor not found")

        constructor.isAccessible = true

        val params = constructor.parameterTypes
        val args = mutableListOf<Any?>()
        args.add(context)
        args.add(MODULE_ORDER)
        args.add("QEdge")

        var intCount = 1
        for (i in 3 until params.size) {
            when {
                params[i] == Int::class.java -> {
                    if (intCount == 1) {
                        args.add(R.mipmap.ic_launcher)
                        intCount++
                    } else {
                        args.add(0)
                    }
                }
                params[i] == String::class.java -> args.add("")
                else -> args.add(null)
            }
        }

        val instance = constructor.newInstance(*args.toTypedArray())

        runCatching {
            var set = false
            val methods = processorClass.methods + processorClass.declaredMethods
            for (m in methods) {
                if (m.parameterTypes.size == 1 &&
                    m.parameterTypes[0].name == "kotlin.jvm.functions.Function0"
                ) {
                    m.isAccessible = true
                    m.invoke(instance, clickLambda)
                    set = true
                    break
                }
            }
            if (!set) {
                for (m in methods) {
                    if (m.parameterTypes.size == 1 &&
                        m.parameterTypes[0].name == "kotlin.jvm.functions.Function1"
                    ) {
                        m.isAccessible = true
                        val function1 = makeFunction1Lambda(context, SettingActivity::class.java)
                        m.invoke(instance, function1)
                        break
                    }
                }
            }
        }.onFailure {
            LogUtils.e("QQSettingInject", "set click listener failed: ${it.message}")
        }

        return instance
    }

    private fun createSection(sectionClass: Class<*>, processor: Any): Any {
        val constructor = sectionClass.declaredConstructors.firstOrNull { cons ->
            cons.parameterTypes.isNotEmpty() && cons.parameterTypes[0] == List::class.java
        } ?: throw NoSuchMethodException("Section constructor not found")

        constructor.isAccessible = true

        val params = constructor.parameterTypes
        val args = mutableListOf<Any?>()
        args.add(listOf(processor))

        var charSeqIndex = 0
        for (i in 1 until params.size) {
            when {
                CharSequence::class.java.isAssignableFrom(params[i]) -> {
                    if (charSeqIndex == 0) {
                        args.add(TOP_TITLE)
                    } else {
                        args.add(BOTTOM_TITLE)
                    }
                    charSeqIndex++
                }
                params[i] == Int::class.java -> args.add(0)
                else -> args.add(null)
            }
        }

        return constructor.newInstance(*args.toTypedArray())
    }

    private fun makeClickLambda(context: Context, activityClass: Class<*>): Any {
        val function0Class = "kotlin.jvm.functions.Function0".toClass
        val unitClass = "kotlin.Unit".toClass
        val unitInstance = unitClass.getField("INSTANCE").get(null)

        return Proxy.newProxyInstance(
            ClassUtils.hostClassLoader,
            arrayOf(function0Class)
        ) { _, method, _ ->
            if (method.name == "invoke") {
                runCatching {
                    val activity = context as Activity
                    Parasitics.ensureInitialized(activity)
                    Parasitics.injectModuleResources(activity.resources)
                    val intent = Intent(activity, activityClass)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    intent.addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                    activity.startActivity(intent)
                }.onFailure {
                    LogUtils.e("QQSettingInject", it)
                }
            }
            unitInstance
        }
    }

    private fun makeFunction1Lambda(context: Context, activityClass: Class<*>): Any {
        val function1Class = "kotlin.jvm.functions.Function1".toClass
        val unitClass = "kotlin.Unit".toClass
        val unitInstance = unitClass.getField("INSTANCE").get(null)

        return Proxy.newProxyInstance(
            ClassUtils.hostClassLoader,
            arrayOf(function1Class)
        ) { _, method, _ ->
            if (method.name == "invoke") {
                runCatching {
                    val activity = context as Activity
                    Parasitics.ensureInitialized(activity)
                    Parasitics.injectModuleResources(activity.resources)
                    val intent = Intent(activity, activityClass)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    intent.addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                    activity.startActivity(intent)
                }.onFailure {
                    LogUtils.e("QQSettingInject", it)
                }
            }
            unitInstance
        }
    }

    override fun getQueryMap(): Map<String, BaseFinder> = mapOf(
        "provider" to FindClass().apply {
            searchPackages("com.tencent.mobileqq.setting.main")
            matcher {
                superClass("com.tencent.mobileqq.setting.processor.SettingConfigProvider")
            }
        },
        "simpleItemProcessor" to FindClass().apply {
            searchPackages("com.tencent.mobileqq.setting.processor")
            matcher {
                usingStrings("SimpleItemProcessor")
            }
        },
        "settingSection" to FindClass().apply {
            searchPackages("com.tencent.mobileqq.setting.processor")
            matcher {
                usingStrings("itemProcessors")
            }
        }
    )

}
