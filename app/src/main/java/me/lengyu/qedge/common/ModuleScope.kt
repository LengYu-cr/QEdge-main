package me.lengyu.qedge.common

import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.lengyu.qedge.utils.LogUtils
import kotlin.coroutines.CoroutineContext
/**
 * @Author 冷雨
 * @Description 模块作用域
 */
object ModuleScope : CoroutineScope {

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        LogUtils.e("ModuleScope", throwable)
    }

    override val coroutineContext: CoroutineContext =
        SupervisorJob() + Dispatchers.Default + exceptionHandler

    /**
     * 受限并发调度器：Hook 加载/初始化专用，最多 2 个并发。
     * 冷启动时会一次性提交 40+ 个 hook 初始化任务，若直接丢给共享的 Dispatchers.IO
     * （默认 64 线程），会与宿主主线程抢 CPU 并放大类加载/GC 压力导致掉帧。
     * 限流后串行/低并发执行，避免线程池爆炸。
     */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val hookDispatcher: CoroutineDispatcher = Dispatchers.IO.limitedParallelism(2)

    fun launchIO(tag: String = "IO", block: suspend CoroutineScope.() -> Unit) =
        launch(
            Dispatchers.IO + CoroutineExceptionHandler { _, e -> LogUtils.e(tag, e) },
            block = block
        )

    /**
     * 在受限并发调度器上执行 Hook 加载任务。
     */
    fun launchHook(tag: String = "Hook", block: suspend CoroutineScope.() -> Unit) =
        launch(
            hookDispatcher + CoroutineExceptionHandler { _, e -> LogUtils.e(tag, e) },
            block = block
        )

    /**
     * Java-friendly: 在受限并发调度器上延迟执行 Hook 加载任务。
     */
    @JvmStatic
    fun launchDelayedHook(tag: String, delayMs: Long, block: Runnable) = launchHook(tag) {
        if (delayMs > 0) delay(delayMs)
        block.run()
    }

    /**
     * Java-friendly: run blocking code on IO thread without manual Thread creation.
     * All uncaught exceptions are logged via LogUtils, never leaked to framework.
     */
    @JvmStatic
    fun launchIOJava(tag: String, block: Runnable) = launchIO(tag) { block.run() }

    /**
     * Java-friendly: run blocking code on IO thread after a delay.
     * Prefer this over Thread.sleep() + new Thread().
     */
    @JvmStatic
    fun launchDelayedIO(tag: String, delayMs: Long, block: Runnable) = launchIO(tag) {
        delay(delayMs)
        block.run()
    }

    /**
     * Java-friendly: post to main thread.
     */
    @JvmStatic
    fun postToMain(block: Runnable) {
        Handler(Looper.getMainLooper()).post(block)
    }
}