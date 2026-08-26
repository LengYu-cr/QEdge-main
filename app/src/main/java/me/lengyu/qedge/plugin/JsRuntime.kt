package me.lengyu.qedge.plugin

import me.lengyu.qedge.plugin.api.PluginMethod
import me.lengyu.qedge.plugin.bean.PluginInfo
import me.lengyu.qedge.utils.HostInfo
import me.lengyu.qedge.utils.LogUtils
import me.lengyu.qedge.utils.ReflectUtils
import me.lengyu.qedge.utils.Toasts
import me.lengyu.qedge.utils.qq.QQCurrentEnv
import org.mozilla.javascript.Context
import org.mozilla.javascript.Function
import org.mozilla.javascript.NativeJavaObject
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject
import org.mozilla.javascript.Undefined
import java.io.File
import java.lang.reflect.Modifier
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory

/**
 * @Author 冷雨
 * @Description Rhino JS 脚本运行时。
 *
 * 与 Java(BeanShell) 插件对齐：
 *  - PluginMethod 的全部 public 方法提升为 JS 全局裸调函数(无需 qe. 前缀)。
 *  - loadJs(绝对路径) 对齐 loadJava，可加载其它 JS 文件到同一作用域。
 *  - onMsg / unLoadPlugin 等约定回调，通过在脚本里定义同名顶层函数即可被宿主回调。
 *
 * 线程模型：Rhino 的 Context/Scriptable 不能跨线程共享。每个 JS 插件独占一个单线程
 * Executor，start、所有回调、loadJs、stop 全部提交到该线程串行执行，天然线程安全。
 */
class JsRuntime(private val info: PluginInfo, private val api: PluginMethod) {

    private val executor = Executors.newSingleThreadExecutor(ThreadFactory { r ->
        Thread(r, "JsPlugin-" + info.id).apply { isDaemon = true }
    })

    /** 绑定到专用线程的 Rhino Context，仅在 executor 线程内访问 */
    @Volatile
    private var context: Context? = null

    /** 全局作用域(顶层)，脚本定义的函数都挂在这里 */
    @Volatile
    private var scope: ScriptableObject? = null

    @Volatile
    private var started = false

    /**
     * 加载并执行入口脚本 main.js。同步等待执行完成，异常向上抛出，
     * 与 PluginCompiler 对 Java 插件的处理一致(便于 startPlugin 捕获后提示)。
     */
    fun start() {
        val entry = File(info.dirPath, "main.js")
        if (!entry.exists()) {
            throw IllegalStateException("main.js not found")
        }
        submitBlocking {
            val ctx = Context.enter()
            // Android(Dalvik/ART)无法运行 Rhino 生成的字节码，必须解释执行
            ctx.optimizationLevel = -1
            ctx.languageVersion = Context.VERSION_ES6
            context = ctx

            val sc = ctx.initStandardObjects()
            scope = sc

            injectEnvVars(ctx, sc)
            injectApiMethods(ctx, sc)
            injectConsole(ctx, sc)
            injectLoaders(ctx, sc)

            evalFile(ctx, sc, entry)
            started = true
        }
    }

    fun destroy() {
        // 先在脚本线程内执行卸载回调，再拆运行时
        if (started) {
            runCatching { callFunctionSync("unLoadPlugin") }
                .onFailure { PluginError.callError(RuntimeException(it), info) }
        }
        started = false
        runCatching {
            submitBlocking {
                scope = null
                if (context != null) {
                    Context.exit()
                    context = null
                }
            }
        }
        executor.shutdownNow()
    }

    /** 脚本里是否定义了名为 name 的顶层函数 */
    fun hasFunction(name: String): Boolean {
        val sc = scope ?: return false
        val v = ScriptableObject.getProperty(sc, name)
        return v is Function
    }

    /** 在脚本线程串行异步调用某函数(fire-and-forget)，用于 onMsg/菜单等不需要返回值的回调 */
    fun callFunctionAsync(name: String, vararg args: Any?) {
        if (!started) return
        executor.execute {
            runCatching { invokeInScope(name, args) }
                .onFailure { PluginError.callError(RuntimeException(it), info) }
        }
    }

    /** 同步调用某函数并返回结果(转成 Java 对象)，用于 getMsg/getSummary 等需要返回值的回调 */
    fun callFunctionSync(name: String, vararg args: Any?): Any? {
        if (!started && name != "unLoadPlugin") return null
        return submitBlocking { invokeInScope(name, args) }
    }

    // ---- 内部实现 ----

    private fun invokeInScope(name: String, args: Array<out Any?>): Any? {
        val ctx = context ?: return null
        val sc = scope ?: return null
        val fnObj = ScriptableObject.getProperty(sc, name)
        if (fnObj !is Function) return null
        val jsArgs = args.map { Context.javaToJS(it, sc) }.toTypedArray()
        val result = fnObj.call(ctx, sc, sc, jsArgs)
        return jsToJava(result)
    }

    private fun jsToJava(value: Any?): Any? {
        return when (value) {
            null, Undefined.instance -> null
            is NativeJavaObject -> value.unwrap()
            is CharSequence -> value.toString()
            else -> value
        }
    }

    private fun injectEnvVars(ctx: Context, sc: ScriptableObject) {
        putJava(ctx, sc, "context", HostInfo.getHostContext())
        putJava(ctx, sc, "myUin", QQCurrentEnv.getCurrentUin())
        putJava(ctx, sc, "classLoader", ReflectUtils.hostClassLoader)
        putJava(ctx, sc, "pluginPath", info.dirPath)
        putJava(ctx, sc, "pluginId", info.id)
    }

    /**
     * 把 PluginMethod 的每个 public 方法提升为 JS 全局裸调函数。
     * 做法：把 api 实例包成隐藏的 __api__(NativeJavaObject，自动处理重载分派)，
     * 再为每个方法名生成一个转发到 __api__ 的全局 JS 函数。
     */
    private fun injectApiMethods(ctx: Context, sc: ScriptableObject) {
        val apiJs = Context.javaToJS(api, sc)
        ScriptableObject.putProperty(sc, "__api__", apiJs)

        val names = LinkedHashSet<String>()
        for (m in PluginMethod::class.java.declaredMethods) {
            if (Modifier.isPublic(m.modifiers) && !m.name.contains("$")) {
                names.add(m.name)
            }
        }
        val sb = StringBuilder()
        for (n in names) {
            // 用 apply 转发全部实参，让 Rhino 的 NativeJavaMethod 按参数选重载
            sb.append("var ").append(n).append("=function(){return __api__.")
                .append(n).append(".apply(__api__,arguments);};\n")
        }
        ctx.evaluateString(sc, sb.toString(), "<api-bridge>", 1, null)
    }

    /** 注入 console.log/console.error，走宿主日志 + Toast(与 Java 插件的调试习惯一致) */
    private fun injectConsole(ctx: Context, sc: ScriptableObject) {
        putJava(ctx, sc, "__console__", JsConsole(info))
        ctx.evaluateString(
            sc,
            "var console={log:function(){__console__.log(Array.prototype.slice.call(arguments).join(' '));}," +
                "error:function(){__console__.error(Array.prototype.slice.call(arguments).join(' '));}," +
                "warn:function(){__console__.log(Array.prototype.slice.call(arguments).join(' '));}," +
                "info:function(){__console__.log(Array.prototype.slice.call(arguments).join(' '));}};",
            "<console>", 1, null
        )
    }

    /** 注入 loadJs(绝对路径)，对齐 loadJava，把目标文件加载进同一作用域 */
    private fun injectLoaders(ctx: Context, sc: ScriptableObject) {
        putJava(ctx, sc, "__loader__", JsLoader(this))
        ctx.evaluateString(
            sc,
            "var loadJs=function(path){return __loader__.loadJs(path);};",
            "<loader>", 1, null
        )
    }

    /** 供 loadJs 调用：在当前作用域求值目标文件(必须已在脚本线程内) */
    internal fun loadJsFile(path: String) {
        val ctx = context ?: return
        val sc = scope ?: return
        val f = File(path)
        if (!f.exists()) throw IllegalStateException("loadJs: file not found: $path")
        evalFile(ctx, sc, f)
    }

    private fun evalFile(ctx: Context, sc: ScriptableObject, file: File) {
        val code = file.readText(Charsets.UTF_8)
        ctx.evaluateString(sc, code, file.name, 1, null)
    }

    private fun putJava(ctx: Context, sc: ScriptableObject, name: String, value: Any?) {
        ScriptableObject.putProperty(sc, name, Context.javaToJS(value, sc))
    }

    /** 提交任务到脚本线程并阻塞等待结果，异常原样抛出到调用方 */
    private fun <T> submitBlocking(block: () -> T): T {
        val future = executor.submit<T> { block() }
        try {
            return future.get()
        } catch (e: java.util.concurrent.ExecutionException) {
            throw e.cause ?: e
        }
    }

    /** console 桥接对象，作为 NativeJavaObject 注入 */
    class JsConsole(private val info: PluginInfo) {
        fun log(msg: String?) {
            val text = msg ?: "null"
            // logcat 保留(tag [QEdge]:JsPlugin-<id>)，同时写到插件自己目录的 log.txt，与 Java 插件 log() 一致
            LogUtils.i("JsPlugin-" + info.id, text)
            writeToPluginLog(text)
        }

        fun error(msg: String?) {
            val text = msg ?: "null"
            LogUtils.e("JsPlugin-" + info.id, text)
            writeToPluginLog(text)
            Toasts.showCustomToast(text)
        }

        /** 追加写到「插件目录/log.txt」，方便直接在脚本文件夹里查看 */
        private fun writeToPluginLog(msg: String) {
            try {
                val dir = File(info.dirPath)
                if (!dir.exists()) dir.mkdirs()
                java.io.FileWriter(File(dir, "log.txt"), true).use { it.write(msg + "\n") }
            } catch (_: Throwable) {
            }
        }
    }

    /** loadJs 桥接对象 */
    class JsLoader(private val runtime: JsRuntime) {
        fun loadJs(path: String?) {
            if (path == null) return
            runtime.loadJsFile(path)
        }
    }
}
