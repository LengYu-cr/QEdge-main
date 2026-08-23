package me.lengyu.qedge.hook.item

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import me.lengyu.qedge.hook.annotation.HookCategory
import me.lengyu.qedge.hook.annotation.HookItemAnnotation
import me.lengyu.qedge.hook.base.BaseSwitchHookItem
import me.lengyu.qedge.utils.HostInfo
import me.lengyu.qedge.utils.LogUtils
import me.lengyu.qedge.utils.ModuleConfig
/**
 * @Author 冷雨
 * @Description QQ进程保活：像素悬浮窗/前台通知/后台通知
 */
@HookItemAnnotation(
    value = "保活机制",
    desc = "QQ进程保活：像素悬浮窗/前台通知/后台通知",
    category = HookCategory.OTHER
)
object KeepAliveHook : BaseSwitchHookItem() {

    private const val TAG = "KeepAliveHook"
    private const val CHANNEL_FG = "qedge_keepalive_fg"
    private const val CHANNEL_BG = "qedge_keepalive_bg"
    private const val NOTIF_ID_FG = 0x6901
    private const val NOTIF_ID_BG = 0x6902
    private const val NOTIF_RESHOW_INTERVAL = 3000L

    const val SP_PIXEL = "keep_alive_pixel"
    const val SP_FOREGROUND = "keep_alive_foreground"
    const val SP_BACKGROUND = "keep_alive_background"

    private var windowManager: WindowManager? = null
    private var pixelView: View? = null
    private var notificationManager: NotificationManager? = null
    private var channelCreated = false

    private val mainHandler = Handler(Looper.getMainLooper())
    private var notifReshowRunning = false

    private val notifReshowRunnable = object : Runnable {
        override fun run() {
            try {
                val ctx = HostInfo.getHostContext() ?: return
                ensureChannels()
                if (ModuleConfig.getBoolean(SP_FOREGROUND, false)) {
                    notificationManager?.notify(NOTIF_ID_FG, buildNotification(ctx, CHANNEL_FG, "QQ", "QQ正在后台运行", true))
                }
                if (ModuleConfig.getBoolean(SP_BACKGROUND, false)) {
                    notificationManager?.notify(NOTIF_ID_BG, buildNotification(ctx, CHANNEL_BG, "QEdge", "保活服务运行中", false))
                }
            } catch (e: Throwable) {
                LogUtils.e(TAG, "notif reshow: ${e.message}")
            }
            if (notifReshowRunning) {
                mainHandler.postDelayed(this, NOTIF_RESHOW_INTERVAL)
            }
        }
    }

    override fun onInit(): Boolean = HostInfo.isQQ || HostInfo.isTIM

    override fun onHook() {
        refresh()
    }

    fun refresh() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { refresh() }
            return
        }
        try {
            val ctx = HostInfo.getHostContext() ?: return
            if (windowManager == null) {
                windowManager = ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            }
            if (notificationManager == null) {
                notificationManager = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            }

            if (ModuleConfig.getBoolean(SP_PIXEL, false)) startPixelWindow() else stopPixelWindow()

            val fgEnabled = ModuleConfig.getBoolean(SP_FOREGROUND, false)
            val bgEnabled = ModuleConfig.getBoolean(SP_BACKGROUND, false)

            if (fgEnabled) startForegroundNotification() else stopForegroundNotification()
            if (bgEnabled) startBackgroundNotification() else stopBackgroundNotification()

            // QQ 主界面 onResume 会 cancelAll，需要定时补显
            if (fgEnabled || bgEnabled) {
                startNotifReshow()
            } else {
                stopNotifReshow()
            }
        } catch (e: Throwable) {
            LogUtils.e(TAG, "refresh error: ${e.message}")
        }
    }

    private fun startNotifReshow() {
        if (notifReshowRunning) return
        notifReshowRunning = true
        mainHandler.postDelayed(notifReshowRunnable, NOTIF_RESHOW_INTERVAL)
    }

    private fun stopNotifReshow() {
        notifReshowRunning = false
        mainHandler.removeCallbacks(notifReshowRunnable)
    }

    private fun ensureChannels() {
        if (channelCreated) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager?.createNotificationChannel(
                NotificationChannel(CHANNEL_FG, "QEdge保活(前台)", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "前台保活通知"
                    setShowBadge(false)
                }
            )
            notificationManager?.createNotificationChannel(
                NotificationChannel(CHANNEL_BG, "QEdge保活(后台)", NotificationManager.IMPORTANCE_MIN).apply {
                    description = "后台保活通知"
                    setShowBadge(false)
                }
            )
        }
        channelCreated = true
    }

    private fun startPixelWindow() {
        if (pixelView != null) return
        try {
            val ctx = HostInfo.getHostContext() ?: return
            val view = View(ctx).apply { setBackgroundColor(Color.TRANSPARENT) }
            val params = WindowManager.LayoutParams(
                1, 1,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                else
                    @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.END
                x = 0
                y = 0
            }
            windowManager?.addView(view, params)
            pixelView = view
        } catch (e: Throwable) {
            LogUtils.e(TAG, "startPixelWindow: ${e.message}")
        }
    }

    private fun stopPixelWindow() {
        pixelView?.let { v ->
            try { windowManager?.removeView(v) } catch (_: Throwable) {}
        }
        pixelView = null
    }

    private fun buildNotification(ctx: Context, channelId: String, title: String, text: String, ongoing: Boolean): Notification {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            Notification.Builder(ctx, channelId)
        else
            @Suppress("DEPRECATION") Notification.Builder(ctx)

        val iconRes = ctx.applicationInfo?.icon ?: android.R.drawable.ic_dialog_info
        builder.setSmallIcon(iconRes)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(ongoing)
            .setShowWhen(false)
            .setContentIntent(android.app.PendingIntent.getActivity(
                ctx, 0,
                ctx.packageManager.getLaunchIntentForPackage(ctx.packageName) ?: android.content.Intent(),
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) android.app.PendingIntent.FLAG_IMMUTABLE else 0
            ))
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            @Suppress("DEPRECATION")
            builder.setPriority(if (ongoing) Notification.PRIORITY_HIGH else Notification.PRIORITY_LOW)
        }
        return builder.build()
    }

    private fun startForegroundNotification() {
        try {
            val ctx = HostInfo.getHostContext() ?: return
            ensureChannels()
            notificationManager?.notify(NOTIF_ID_FG, buildNotification(ctx, CHANNEL_FG, "QQ", "QQ正在后台运行", true))
        } catch (e: Throwable) {
            LogUtils.e(TAG, "startForegroundNotification: ${e.message}")
        }
    }

    private fun stopForegroundNotification() {
        try { notificationManager?.cancel(NOTIF_ID_FG) } catch (_: Throwable) {}
    }

    private fun startBackgroundNotification() {
        try {
            val ctx = HostInfo.getHostContext() ?: return
            ensureChannels()
            notificationManager?.notify(NOTIF_ID_BG, buildNotification(ctx, CHANNEL_BG, "QEdge", "保活服务运行中", false))
        } catch (e: Throwable) {
            LogUtils.e(TAG, "startBackgroundNotification: ${e.message}")
        }
    }

    private fun stopBackgroundNotification() {
        try { notificationManager?.cancel(NOTIF_ID_BG) } catch (_: Throwable) {}
    }
}
