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
import org.mozilla.javascript.BaseFunction
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject
import org.mozilla.javascript.Undefined
import android.os.Handler
import android.os.Looper
import java.io.File
import java.io.BufferedInputStream
import java.io.FileInputStream
import java.lang.reflect.Modifier
import java.util.zip.ZipInputStream
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
            injectClassImporter(ctx, sc)
            injectUiBridge(ctx, sc)

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

    /** 注入 loadJs/loadJsLib(绝对路径)，对齐 loadJava，把目标文件或打包(zip/jar)加载进同一作用域 */
    private fun injectLoaders(ctx: Context, sc: ScriptableObject) {
        putJava(ctx, sc, "__loader__", JsLoader(this))
        ctx.evaluateString(
            sc,
            "var loadJs=function(path){return __loader__.loadJs(path);};",
            "<loader>", 1, null
        )
        ctx.evaluateString(
            sc,
            "var loadJsLib=function(path){return __loader__.loadJsLib(path);};",
            "<loader>", 1, null
        )
    }

    /** 全局注入 importClass：支持 Java 类对象(Packages.xxx)或类名字符串，把短名绑定到当前脚本作用域 */
    private fun injectClassImporter(ctx: Context, sc: ScriptableObject) {
        val loader = ReflectUtils.hostClassLoader
        ScriptableObject.putProperty(sc, "importClass", object : BaseFunction() {
            override fun call(cx: Context, scope: Scriptable, thisObj: Scriptable, args: Array<out Any?>): Any? {
                if (args.isEmpty()) return Undefined.instance
                val cls = resolveJavaClass(args[0], cx, loader)
                if (cls == null) {
                    PluginError.callError(RuntimeException("importClass: 无法解析类 ${args[0]}"), info)
                    return Undefined.instance
                }
                ScriptableObject.putProperty(sc, cls.simpleName, Context.javaToJS(cls, sc))
                return Undefined.instance
            }
        })
    }

    /** 解析 importClass 参数：Java 类对象 或 类名字符串(支持嵌套类用 . 分隔，如 android.app.AlertDialog.Builder) */
    private fun resolveJavaClass(arg: Any?, cx: Context, loader: ClassLoader): Class<*>? {
        val fromObject = runCatching { Context.jsToJava(arg, Class::class.java) }.getOrNull()
        if (fromObject is Class<*>) return fromObject
        val name = (arg as? String) ?: return null
        fun tryLoad(cn: String): Class<*>? = runCatching { Class.forName(cn, false, loader) }.getOrNull()
        tryLoad(name)?.let { return it }
        var cn = name
        var idx = cn.lastIndexOf('.')
        while (idx > 0) {
            cn = cn.substring(0, idx) + '$' + cn.substring(idx + 1)
            tryLoad(cn)?.let { return it }
            idx = cn.lastIndexOf('.')
        }
        return null
    }

    /** 全局注入 runOnUiThread：把 JS 函数投递到主线程执行，便于直接弹窗/操作 UI */
    private fun injectUiBridge(ctx: Context, sc: ScriptableObject) {
        ScriptableObject.putProperty(sc, "runOnUiThread", object : BaseFunction() {
            override fun call(cx: Context, scope: Scriptable, thisObj: Scriptable, args: Array<out Any?>): Any? {
                if (args.isEmpty() || args[0] !is Function) return Undefined.instance
                runOnMainThread(args[0] as Function)
                return Undefined.instance
            }
        })
    }

    /** 把 JS 函数投递到主线程执行（主线程内临时进入 Rhino Context） */
    private fun runOnMainThread(fn: Function) {
        val sc = scope ?: return
        Handler(Looper.getMainLooper()).post {
            try {
                val ux = Context.enter()
                try {
                    ux.optimizationLevel = -1
                    fn.call(ux, sc, sc, emptyArray())
                } finally {
                    Context.exit()
                }
            } catch (t: Exception) {
                PluginError.callError(t, info)
            }
        }
    }

    /** 供 loadJs 调用：在当前作用域求值目标文件(必须已在脚本线程内) */
    internal fun loadJsFile(path: String) {
        val ctx = context ?: return
        val sc = scope ?: return
        val f = File(path)
        if (!f.exists()) throw IllegalStateException("loadJs: file not found: $path")
        evalFile(ctx, sc, f)
    }

    /**
     * 供 loadJsLib 调用：一次性加载打包(Zip/Jar)内所有 .js 到同一作用域。
     * 按包内路径字典序逐个求值，保证可预期的加载顺序；不递归目录层级。
     */
    internal fun loadJsLibFile(path: String) {
        val ctx = context ?: return
        val sc = scope ?: return
        val f = File(path)
        if (!f.exists()) throw IllegalStateException("loadJsLib: file not found: $path")
        val scripts = HashMap<String, String>()
        ZipInputStream(BufferedInputStream(FileInputStream(f))).use { zis ->
            while (true) {
                val entry = zis.nextEntry ?: break
                val name = entry.name
                if (!entry.isDirectory && name.endsWith(".js")) {
                    val code = zis.readBytes().toString(Charsets.UTF_8)
                    scripts[name] = code
                }
                zis.closeEntry()
            }
        }
        if (scripts.isEmpty()) throw IllegalStateException("loadJsLib: 包内没有 .js 文件: $path")
        scripts.toSortedMap().forEach { (name, code) ->
            ctx.evaluateString(sc, code, name, 1, null)
        }
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

        fun loadJsLib(path: String?) {
            if (path == null) return
            runtime.loadJsLibFile(path)
        }
    }
}
