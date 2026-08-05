package me.lengyu.qedge.hook.item

import me.lengyu.qedge.hook.annotation.HookCategory
import me.lengyu.qedge.hook.annotation.HookItemAnnotation
import me.lengyu.qedge.hook.base.BaseApiHookItem
import me.lengyu.qedge.utils.HostInfo
import me.lengyu.qedge.utils.LogUtils
import me.lengyu.qedge.utils.ModuleConfig
import me.lengyu.qedge.utils.QQCurrentEnv
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.atomic.AtomicBoolean

/**
 * QZone 定时任务 + 等级加速类每日签到
 * 【等级加速·凌晨 00:00 触发】
 *  ① 空间等级签到（qzoneClockIn）
 *  ② QQ 日签打卡（dailySign，ti.qq.com）
 *  ③ 大会员签到（bigVipClockIn，QQ大会员中心）
 * 【定时说说·用户自定义 HH:mm 触发】
 *  ④ 用户自定义时间发一条说说（publishMood）
 *
 * 三重去重保证只执行一次：
 *  ① 仅主进程启动 Timer（HostInfo.processName == HostInfo.packageName）
 *  ② SP 跨进程幂等标记（key 按今天日期命名，boolean）
 *  ③ 标记改为"发布成功后才写"
 */
@HookItemAnnotation(value = "QZone 定时任务", category = "item")
object QZoneSchedule : BaseApiHookItem<QZoneSchedule.Listener>() {

    const val TAG = "QZoneSchedule"

    /** 空间等级签到开关 */
    const val SP_CHECKIN_ENABLED = "qzone_daily_checkin_enabled"
    /** QQ 日签打卡开关（等级加速） */
    const val SP_DAILY_SIGN_ENABLED = "qq_daily_sign_enabled"
    /** 大会员签到开关（等级加速） */
    const val SP_BIGVIP_CHECKIN_ENABLED = "qq_bigvip_checkin_enabled"
    /** 定时说说开关 */
    const val SP_MOOD_ENABLED = "qzone_schedule_mood_enabled"
    /** 定时说说发送时间 HH:mm SP key，默认 08:30 */
    const val SP_MOOD_TIME = "qzone_schedule_mood_time"
    /** 定时说说内容 SP key */
    const val SP_MOOD_TEXT = "qzone_schedule_mood_text"
    /** 所有等级加速类签到默认触发时间：凌晨 00:00 */
    const val CHECKIN_TRIGGER_TIME = "00:00"

    /** SP 幂等 key 前缀：某一天空间签到已完成 */
    private const val SP_PREFIX_CHECKIN_DONE = "qzone_schedule_checkin_done_"
    /** SP 幂等 key 前缀：某一天日签打卡已完成 */
    private const val SP_PREFIX_DAILY_SIGN_DONE = "qq_daily_sign_done_"
    /** SP 幂等 key 前缀：某一天大会员签到已完成 */
    private const val SP_PREFIX_BIGVIP_DONE = "qq_bigvip_checkin_done_"
    /** SP 幂等 key 前缀：某一天定时说说已发 */
    private const val SP_PREFIX_MOOD_DONE = "qzone_schedule_mood_done_"

    interface Listener : me.lengyu.qedge.hook.base.Listener {
        fun onScheduleMoodResult(result: Boolean, text: String, trigger: String)
        fun onScheduleCheckinResult(result: Boolean)
        fun onDailySignResult(result: Boolean, msg: String)
        fun onBigVipCheckinResult(result: Boolean)
    }

    private val started = AtomicBoolean(false)
    private var timer: Timer? = null

    /** 单条 HH:mm 格式校验正则（00-23 小时，00-59 分） */
    val HH_MM_REGEX = "^([01]\\d|2[0-3]):([0-5]\\d)$".toRegex()

    fun isCheckinEnabled(): Boolean = ModuleConfig.getBoolean(SP_CHECKIN_ENABLED, false)
    fun isDailySignEnabled(): Boolean = ModuleConfig.getBoolean(SP_DAILY_SIGN_ENABLED, false)
    fun isBigVipCheckinEnabled(): Boolean = ModuleConfig.getBoolean(SP_BIGVIP_CHECKIN_ENABLED, false)
    fun isMoodEnabled(): Boolean = ModuleConfig.getBoolean(SP_MOOD_ENABLED, false)
    fun getMoodTime(): String = ModuleConfig.getString(SP_MOOD_TIME, "08:30").trim()
        .takeIf { HH_MM_REGEX.matches(it) } ?: "08:30"
    fun getMoodText(): String = ModuleConfig.getString(SP_MOOD_TEXT, "").trim()
        .ifEmpty { "今天也要加油哦~" }

    /** SP 跨进程幂等：今天是否已做完某个任务 */
    private fun isDoneToday(prefix: String, today: String): Boolean =
        ModuleConfig.getBoolean("$prefix$today", false)

    /** SP 跨进程幂等：标记今天的任务已完成 */
    private fun markDoneToday(prefix: String, today: String) {
        runCatching {
            ModuleConfig.putBoolean("$prefix$today", true)
            val y = today.toIntOrNull() ?: return
            for (i in -7..-4) {
                val oldKey = "$prefix${y + i}"
                if (ModuleConfig.contains(oldKey)) {
                    ModuleConfig.remove(oldKey)
                }
            }
        }
    }

    private fun isMainProcess(): Boolean {
        return runCatching {
            val pn = HostInfo.packageName.trim().orEmpty()
            val proc = HostInfo.processName.trim().orEmpty()
            pn.isNotEmpty() && proc.isNotEmpty() && proc == pn
        }.getOrDefault(false)
    }

    override fun loadHook() {
        if (!isMainProcess()) return
        if (!started.compareAndSet(false, true)) return
        val t = Timer("QZone_Schedule", true)
        timer = t
        val delayMs = 5000L
        val periodMs = 60L * 1000L
        t.schedule(object : TimerTask() {
            override fun run() = tick()
        }, delayMs, periodMs)
    }

    fun shutdown() {
        timer?.cancel()
        timer = null
        started.set(false)
    }

    /** 命中判断：允许目标分钟或下一分钟（1 分钟抖动容错，SP 幂等保证不会重复） */
    private fun hitTime(hm: String, target: String): Boolean {
        if (hm == target) return true
        val hp = target.split(':')
        if (hp.size != 2) return false
        val h = hp[0].toIntOrNull() ?: return false
        val m = hp[1].toIntOrNull() ?: return false
        var h2 = h; var m2 = m + 1
        if (m2 >= 60) { m2 = 0; h2 = (h2 + 1) % 24 }
        val next = "%02d:%02d".format(h2, m2)
        return hm == next
    }

    private fun tick() {
        runCatching {
            val now = Date()
            val hm = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now)
            val today = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(now)
            val uin = QQCurrentEnv.getCurrentUin().orEmpty()

            // =============== 【等级加速：凌晨 00:00 三个任务按顺序串行执行】 ===============
            val triggerTime = hitTime(hm, CHECKIN_TRIGGER_TIME)
            if (triggerTime && uin.isNotEmpty()) {
                // ① 空间等级签到
                if (isCheckinEnabled() && !isDoneToday(SP_PREFIX_CHECKIN_DONE, today)) {
                    Thread {
                        runCatching {
                            val ok = QZoneLikeTool.qzoneClockIn()
                            if (ok) markDoneToday(SP_PREFIX_CHECKIN_DONE, today)
                            forEachChecked { it.onScheduleCheckinResult(ok) }
                        }.onFailure { LogUtils.e(TAG, "tick checkin error: ${it.message}") }
                    }.start()
                }
                // ② QQ 日签打卡
                if (isDailySignEnabled() && !isDoneToday(SP_PREFIX_DAILY_SIGN_DONE, today)) {
                    Thread {
                        runCatching {
                            val (ok, msg) = QZoneLikeTool.dailySign()
                            if (ok) markDoneToday(SP_PREFIX_DAILY_SIGN_DONE, today)
                            forEachChecked { it.onDailySignResult(ok, msg) }
                        }.onFailure { LogUtils.e(TAG, "tick dailySign error: ${it.message}") }
                    }.start()
                }
                // ③ 大会员签到
                if (isBigVipCheckinEnabled() && !isDoneToday(SP_PREFIX_BIGVIP_DONE, today)) {
                    Thread {
                        runCatching {
                            val ok = QZoneLikeTool.bigVipClockIn()
                            if (ok) markDoneToday(SP_PREFIX_BIGVIP_DONE, today)
                            forEachChecked { it.onBigVipCheckinResult(ok) }
                        }.onFailure { LogUtils.e(TAG, "tick bigVipCheckin error: ${it.message}") }
                    }.start()
                }
            }

            // =============== 【定时说说：用户自定义 HH:mm 触发】 ===============
            val tgt = getMoodTime()
            val text = getMoodText()
            val needMood = isMoodEnabled()
                && text.isNotEmpty()
                && hitTime(hm, tgt)
                && !isDoneToday(SP_PREFIX_MOOD_DONE, today)
                && uin.isNotEmpty()
            if (needMood) {
                Thread {
                    runCatching {
                        val ok = QZoneLikeTool.publishMood(text)
                        if (ok) markDoneToday(SP_PREFIX_MOOD_DONE, today)
                        forEachChecked { it.onScheduleMoodResult(ok, text, tgt) }
                    }.onFailure { LogUtils.e(TAG, "tick mood error: ${it.message}") }
                }.start()
            }
        }.onFailure { LogUtils.e(TAG, "tick error: ${it.message}") }
    }
}
