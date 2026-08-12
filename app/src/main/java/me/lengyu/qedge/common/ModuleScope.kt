package me.lengyu.qedge.common

import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.lengyu.qedge.utils.LogUtils
import kotlin.coroutines.CoroutineContext

object ModuleScope : CoroutineScope {

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        LogUtils.e("ModuleScope", throwable)
    }

    override val coroutineContext: CoroutineContext =
        SupervisorJob() + Dispatchers.Default + exceptionHandler

    fun launchIO(tag: String = "IO", block: suspend CoroutineScope.() -> Unit) =
        launch(
            Dispatchers.IO + CoroutineExceptionHandler { _, e -> LogUtils.e(tag, e) },
            block = block
        )

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