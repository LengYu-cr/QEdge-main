package me.lengyu.qedge.ui.pages.home

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

internal data class HomeBatteryState(
    val level: Int? = null,
    val isCharging: Boolean = false
)

@Composable
internal fun rememberHomeBatteryState(): HomeBatteryState {
    val context = LocalContext.current.applicationContext
    var batteryState by remember { mutableStateOf(HomeBatteryState()) }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                intent?.toHomeBatteryState()?.let { batteryState = it }
            }
        }
        val stickyIntent = ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            ContextCompat.RECEIVER_EXPORTED
        )
        stickyIntent?.toHomeBatteryState()?.let { batteryState = it }

        onDispose {
            runCatching { context.unregisterReceiver(receiver) }
        }
    }

    return batteryState
}

private fun Intent.toHomeBatteryState(): HomeBatteryState {
    val rawLevel = getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
    val scale = getIntExtra(BatteryManager.EXTRA_SCALE, 100)
    val level = if (rawLevel >= 0 && scale > 0) {
        ((rawLevel * 100f) / scale).toInt().coerceIn(0, 100)
    } else {
        null
    }
    val status = getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)

    return HomeBatteryState(
        level = level,
        isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
    )
}
