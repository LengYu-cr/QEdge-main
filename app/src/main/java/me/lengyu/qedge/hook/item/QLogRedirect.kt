package me.lengyu.qedge.hook.item

import de.robv.android.xposed.XposedHelpers
import me.lengyu.qedge.hook.annotation.HookItemAnnotation
import me.lengyu.qedge.hook.base.BaseSwitchHookItem
import me.lengyu.qedge.utils.HostInfo
import me.lengyu.qedge.utils.HookUtils
import me.lengyu.qedge.utils.LogUtils
import me.lengyu.qedge.utils.ModuleConfig
import me.lengyu.qedge.utils.ReflectUtils
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue

/**
 * @Author 冷雨
 * @Description QLog 日志处理工具：hook com.tencent.qphone.base.util.QLog 的日志汇总点，
 * 支持三种模式：
 *  - off      关闭：完全放行原日志，不影响 QQ；
 *  - mute     拦截：丢弃 QQ 所有日志(logcat/beacon/文件)，不写入任何本地文件；
 *  - redirect 重定向：拦截并丢弃原日志，写入 QEdge/log/QLog/yyyy-MM-dd_HH.log。
 */
@HookItemAnnotation(
    value = "QLog日志重定向/拦截",
    category = "item",
    tag = "QLog重定向",
    desc = "hook QLog，拦截/重定向腾讯QQ宿主日志，不保留原日志"
)
object QLogRedirect : BaseSwitchHookItem() {

    private const val TAG = "QLogRedirect"

    private const val KEY_MODE = "qlog_redirect_mode"

    /** 模式常量 */
    const val MODE_OFF = "off"
    const val MODE_MUTE = "mute"
    const val MODE_REDIRECT = "redirect"

    /** 目标宿主类(QQ 9.3.25)，方法名稳定。 */
    private const val QLOG_CLASS = "com.tencent.qphone.base.util.QLog"

    private val DATE_FMT = SimpleDateFormat("yyyy-MM-dd_HH", Locale.getDefault())
    private val TIME_FMT = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

    /** 后台写日志队列，避免 Hook 线程/主线程同步文件 IO。 */
    private val queue: BlockingQueue<String> = LinkedBlockingQueue(8192)
    private var writerThread: Thread? = null

    private fun getMode(): String = ModuleConfig.getString(KEY_MODE, MODE_OFF)

    private fun isActive() = getMode() != MODE_OFF

    override fun onInit(): Boolean = true

    override fun onHook() {
        val classLoader = ReflectUtils.hostClassLoader ?: return
        val clazz = XposedHelpers.findClass(QLOG_CLASS, classLoader)

        startWriter()

        // addLogItem：d/e/i/w 最终落点，替换为写入本目录并丢弃原逻辑(不 invoke 原方法)。
        try {
            val addLogItem = XposedHelpers.findMethodExact(
                clazz, "addLogItem",
                Byte::class.javaPrimitiveType, String::class.java,
                Int::class.javaPrimitiveType, String::class.java, Throwable::class.java
            )
            HookUtils.hookReplace(addLogItem) { param ->
                val mode = getMode()
                if (mode == MODE_OFF) {
                    HookUtils.invokeOriginalMethod(param)
                    return@hookReplace null
                }
                // 拦截：丢弃原日志(不 invoke 原方法)。redirect 模式额外写入本地文件。
                if (mode == MODE_REDIRECT) {
                    try {
                        val levelByte = (param.args[0] as? Number)?.toByte() ?: byteArrayOf(1)[0]
                        val tag = param.args[1] as? String ?: ""
                        val msg = param.args[3] as? String ?: ""
                        val th = param.args[4] as? Throwable
                        writeLog(levelByte, tag, msg, th)
                    } catch (t: Throwable) {
                        LogUtils.e(TAG, "addLogItem write error: ${t.message}")
                    }
                }
                null
            }
        } catch (e: Throwable) {
            LogUtils.e(TAG, "hook addLogItem error: ${e.message}")
        }

        // handleAndroidRoomLogPrint：logcat 输出 + beacon 上报入口，一并拦截丢弃。
        try {
            val printMethod = XposedHelpers.findMethodExact(
                clazz, "handleAndroidRoomLogPrint",
                String::class.java, String::class.java, String::class.java
            )
            HookUtils.hookReplace(printMethod) { param ->
                // off 时放行原 logcat/beacon，拦截/重定向时丢弃
                if (getMode() == MODE_OFF) HookUtils.invokeOriginalMethod(param)
                null
            }
        } catch (e: Throwable) {
            LogUtils.e(TAG, "hook handleAndroidRoomLogPrint error: ${e.message}")
        }
    }

    private fun startWriter() {
        synchronized(this) {
            if (writerThread != null) return
            val t = Thread {
                while (true) {
                    try {
                        val first = queue.take()
                        val list: MutableList<String> = mutableListOf(first)
                        queue.drainTo(list)
                        flush(list)
                    } catch (e: InterruptedException) {
                        Thread.currentThread().interrupt()
                        return@Thread
                    } catch (_: Throwable) {
                        // drain 后写入异常仅记录一次，避免影响下次
                    }
                }
            }
            t.isDaemon = true
            t.name = "QEdge-QLogRedirect"
            writerThread = t
            t.start()
        }
    }

    private fun levelName(levelByte: Byte): String = when (levelByte.toInt()) {
        2 -> "I"
        3 -> "W"
        4 -> "E"
        else -> "D"
    }

    private fun writeLog(levelByte: Byte, tag: String, msg: String, th: Throwable?) {
        val line = buildString {
            append(TIME_FMT.format(Date()))
            append(" ")
            append(levelName(levelByte))
    append(" [")
            append(tag)
            append("] ")
            append(msg)
            if (th != null) {
                append("\n")
                val sw = StringWriter()
                th.printStackTrace(PrintWriter(sw))
                append(sw)
            }
            append("\n")
        }
        queue.offer(line)  // 队列满时丢弃，绝不阻塞调用线程
    }

    private fun flush(list: List<String>) {
        if (list.isEmpty()) return
        val file = logFile()
        try {
            FileWriter(file, true).use { w ->
                for (line in list) w.write(line)
            }
        } catch (t: Throwable) {
            LogUtils.e(TAG, "flush error: ${t.message}")
        }
    }

    @Synchronized
    private fun logFile(): File {
        val dir = File(HostInfo.getModuleDataPath(), "log/QLog")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, DATE_FMT.format(Date()) + ".log")
    }
}