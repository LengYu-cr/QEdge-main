package me.lengyu.qedge.common

import android.os.Looper
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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
}