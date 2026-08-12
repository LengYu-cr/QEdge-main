package me.lengyu.qedge.hook.item

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import me.lengyu.qedge.hook.annotation.HookItemAnnotation
import me.lengyu.qedge.hook.base.BaseApiHookItem
import me.lengyu.qedge.hook.base.Listener
import me.lengyu.qedge.utils.HookUtils
import me.lengyu.qedge.utils.LogUtils
import me.lengyu.qedge.utils.ModuleConfig
import me.lengyu.qedge.utils.ReflectUtils
import me.lengyu.qedge.utils.dexkit.DexKitTask
import org.luckypray.dexkit.query.FindClass
import org.luckypray.dexkit.query.FindMethod
import org.luckypray.dexkit.query.base.BaseFinder
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStream
import java.lang.reflect.Field
import java.lang.reflect.Method

@HookItemAnnotation(value = "透明头像", category = "item")
object TransparentAvatar : BaseApiHookItem<TransparentAvatar.TransparentAvatarListener>(), DexKitTask {

    const val TAG = "TransparentAvatar"

    private const val KEY_F_METHOD = "f_method"
    private const val KEY_CROP_ACTIVITY = "crop_activity"

    @Volatile
    private var pendingSource: String? = null

    private fun isEnabled() = ModuleConfig.getBoolean("transparent_avatar", false)

    override fun getQueryMap(): Map<String, BaseFinder> = mapOf(
        KEY_F_METHOD to FindMethod().apply {
            searchPackages("com.tencent.mobileqq.util")
            matcher {
                usingStrings(
                    "image illegal, size must be square.",
                    "file not exist",
                    "network error"
                )
            }
        },
        KEY_CROP_ACTIVITY to FindClass().apply {
            searchPackages("com.tencent.mobileqq.activity.photo")
            matcher {
                usingStrings(
                    "PhotoConst.CLIP_WIDTH",
                    "PhotoConst.TARGET_PATH",
                    "PhotoConst.SINGLE_PHOTO_PATH"
                )
            }
        }
    )

    override fun loadHook() {
        if (!isEnabled()) return

        val classLoader = ReflectUtils.hostClassLoader ?: javaClass.classLoader

        val fMethod: Method
        val cropClass: Class<*>
        try {
            fMethod = requireMethod(KEY_F_METHOD)
            cropClass = requireClass(KEY_CROP_ACTIVITY)
        } catch (e: Throwable) {
            LogUtils.e("TransparentAvatar", "DexKit resolve error: ${e.message}")
            return
        }

        hookPhotoCropSource(cropClass)
        hookProfileCardUtilF(fMethod)
        hookBitmapCompress()
        hookBitmapFactoryDecodeFile(classLoader)
    }

    // ------- 递归搜字段：找存在的文件路径 / content/file URI -------
    @Suppress("DEPRECATION")
    private fun extractFilePathFromAny(root: Any?): String? {
        if (root == null) return null
        val visited = HashSet<Int>()
        var found: String? = null

        fun dfs(obj: Any?, depth: Int) {
            if (found != null || obj == null || depth > 5) return
            val id = System.identityHashCode(obj)
            if (id in visited) return
            visited += id

            when (obj) {
                is String -> {
                    if (obj.startsWith("/")) {
                        val f = File(obj)
                        if (f.exists() && f.isFile) found = obj
                    }
                }
                is File -> if (obj.exists()) found = obj.absolutePath
                is Uri -> {
                    val path = obj.path
                    if (path != null && path.startsWith("/")) {
                        val f = File(path)
                        if (f.exists() && f.isFile) found = path
                    }
                }
                is Intent -> {
                    obj.data?.let { dfs(it, depth + 1) }
                    obj.extras?.let { dfs(it, depth + 1) }
                }
                is android.os.Bundle -> {
                    for (k in obj.keySet()) dfs(obj.get(k), depth + 1)
                }
                is Map<*, *> -> {
                    for (v in obj.values) dfs(v, depth + 1)
                }
                is Iterable<*> -> {
                    for (v in obj) dfs(v, depth + 1)
                }
                is Array<*> -> {
                    if (!obj.isArrayOf<Byte>()) for (v in obj) dfs(v, depth + 1)
                }
                else -> {
                    var clz: Class<*>? = obj.javaClass
                    while (clz != null && clz.name != "java.lang.Object") {
                        for (f: Field in clz.declaredFields) {
                            try {
                                f.isAccessible = true
                                val fn = f.name
                                if (fn.startsWith("this$") || fn.startsWith("\$lambda") ||
                                    fn == "serialVersionUID") continue
                                val type = f.type
                                if (type.isPrimitive) continue
                                if (type.name.startsWith("java.lang.Class")) continue
                                if (type.name.startsWith("java.lang.reflect")) continue
                                val value = f.get(obj) ?: continue
                                dfs(value, depth + 1)
                                if (found != null) return
                            } catch (_: Throwable) {}
                        }
                        clz = clz.superclass
                    }
                }
            }
        }
        dfs(root, 0)
        return found
    }

    // ------- PhotoCropActivity：捕获用户选择的原图路径 -------
    private fun hookPhotoCropSource(cropClass: Class<*>) {
        try {
            cropClass.declaredMethods
                .filter { it.name == "doOnCreate" || it.name.contains("onCreate") }
                .forEach { method ->
                    method.isAccessible = true
                    HookUtils.hookAfter(method) { param ->
                        val activity = param.thisObject as? Activity ?: return@hookAfter
                        val fromIntent = extractFilePathFromAny(activity.intent)
                        val fromFields = extractFilePathFromAny(activity)
                        pendingSource = fromIntent ?: fromFields
                    }
                }

            // 内部 AsyncTask 完成后，从 task 字段再搜一次路径
            for (inner in cropClass.declaredClasses) {
                if (inner.simpleName.contains("Task") || inner.simpleName.length == 1) {
                    for (m in inner.declaredMethods) {
                        if (m.returnType == Bitmap::class.java || m.name == "doInBackground") {
                            m.isAccessible = true
                            HookUtils.hookAfter(m) { param ->
                                if (pendingSource == null) {
                                    pendingSource = extractFilePathFromAny(param.thisObject)
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Throwable) {
            LogUtils.e("TransparentAvatar", "hook crop source error: ${e.message}")
        }
    }

    // ------- decodeFile：在 PhotoCropActivity 调用栈中记录路径兜底 -------
    private fun hookBitmapFactoryDecodeFile(classLoader: ClassLoader) {
        try {
            listOf(
                BitmapFactory::class.java.getDeclaredMethod(
                    "decodeFile", String::class.java, BitmapFactory.Options::class.java
                ),
                BitmapFactory::class.java.getDeclaredMethod(
                    "decodeFile", String::class.java
                )
            ).forEach { method ->
                HookUtils.hookBefore(method) { param ->
                    if (pendingSource != null) return@hookBefore
                    val path = param.args.getOrNull(0) as? String ?: return@hookBefore
                    val file = File(path)
                    if (!file.isFile) return@hookBefore
                    val stack = Thread.currentThread().stackTrace
                    for (el in stack) {
                        val cn = el.className
                        if (cn.contains("ProfileCardUtil")) return@hookBefore
                        if (cn.contains("PhotoCropActivity")) {
                            pendingSource = path
                            break
                        }
                    }
                }
            }
        } catch (e: Throwable) {
            LogUtils.e("TransparentAvatar", "hook decodeFile error: ${e.message}")
        }
    }

    // ------- ProfileCardUtil.F()：原图覆盖 tmp + 绕过检查 -------
    private fun hookProfileCardUtilF(fMethod: Method) {
        try {
            val resultCodeField = fMethod.returnType.getDeclaredField("a").apply { isAccessible = true }
            val resultMsgField = fMethod.returnType.getDeclaredField("b").apply { isAccessible = true }

            HookUtils.hookBefore(fMethod) { param ->
                param.args[2] = false
                val source = pendingSource ?: return@hookBefore
                val target = param.args.getOrNull(0) as? String ?: return@hookBefore
                val src = File(source)
                if (!src.exists()) {
                    pendingSource = null
                    return@hookBefore
                }
                if (source == target) {
                    pendingSource = null
                    return@hookBefore
                }
                val dst = File(target)
                dst.parentFile?.takeIf { !it.exists() }?.mkdirs()
                try {
                    copyFile(src, dst)
                } catch (_: Throwable) {
                } finally {
                    pendingSource = null
                }
            }

            HookUtils.hookAfter(fMethod) { param ->
                val result = param.result ?: return@hookAfter
                val code = resultCodeField.getInt(result)
                if (code != 0) {
                    resultCodeField.setInt(result, 0)
                    resultMsgField.set(result, "ok")
                }
            }
        } catch (e: Throwable) {
            LogUtils.e("TransparentAvatar", "hook F method error: ${e.message}")
        }
    }

    // ------- Bitmap.compress：头像场景 JPEG -> PNG 保留透明通道 -------
    private fun hookBitmapCompress() {
        try {
            val method = Bitmap::class.java.getDeclaredMethod(
                "compress",
                Bitmap.CompressFormat::class.java,
                Int::class.javaPrimitiveType,
                OutputStream::class.java
            )
            HookUtils.hookBefore(method) { param ->
                val fmt = param.args[0] as? Bitmap.CompressFormat ?: return@hookBefore
                if (fmt != Bitmap.CompressFormat.JPEG) return@hookBefore
                val stack = Thread.currentThread().stackTrace
                for (el in stack) {
                    val cn = el.className
                    if (cn.contains("PhotoCropActivity") ||
                        cn.contains("ProfileCardUtil") ||
                        cn.contains("pic.compress") ||
                        cn.contains("zplan.avatar") ||
                        cn.contains("avatar.upload")) {
                        param.args[0] = Bitmap.CompressFormat.PNG
                        break
                    }
                }
            }
        } catch (e: Throwable) {
            LogUtils.e("TransparentAvatar", "hook compress error: ${e.message}")
        }
    }

    private fun copyFile(src: File, dst: File) {
        FileInputStream(src).channel.use { inCh ->
            FileOutputStream(dst).channel.use { outCh ->
                inCh.transferTo(0, inCh.size(), outCh)
            }
        }
    }

    interface TransparentAvatarListener : Listener
}
