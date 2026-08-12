package me.lengyu.qedge.hook.item

import me.lengyu.qedge.hook.annotation.HookCategory
import me.lengyu.qedge.hook.annotation.HookItemAnnotation
import me.lengyu.qedge.hook.base.BaseApiHookItem
import me.lengyu.qedge.utils.HostInfo
import me.lengyu.qedge.utils.HttpUtils
import me.lengyu.qedge.utils.LogUtils
import me.lengyu.qedge.utils.ModuleConfig
import me.lengyu.qedge.utils.QQCurrentEnv
import me.lengyu.qedge.utils.json.ProtoData
import me.lengyu.qedge.utils.proto.PacketHelper
import me.lengyu.qedge.utils.proto.packetListener
import me.lengyu.qedge.utils.qq.CookieTool
import me.lengyu.qedge.utils.qq.ExtraTool
import me.lengyu.qedge.utils.qq.FriendTool
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.atomic.AtomicBoolean
import java.util.regex.Pattern

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
    /** 等级加速·加好友开关 */
    const val SP_LEVEL_BOOST_ENABLED = "level_boost_enabled"
    /** 等级加速·空间浏览开关 */
    const val SP_SPACE_BROWSE_ENABLED = "space_browse_enabled"
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
    /** SP 幂等 key 前缀：某一天等级加速加好友已完成 */
    private const val SP_PREFIX_LEVEL_BOOST_DONE = "level_boost_done_"
    /** SP 幂等 key 前缀：某一天空间浏览已完成 */
    private const val SP_PREFIX_SPACE_BROWSE_DONE = "space_browse_done_"
    /** SP 记录上次等级加速开关状态，用于检测关→开变化 */
    private const val SP_LEVEL_BOOST_LAST_SWITCH = "level_boost_last_switch"
    /** SP 记录上次空间浏览开关状态，用于检测关→开变化 */
    private const val SP_SPACE_BROWSE_LAST_SWITCH = "space_browse_last_switch"
    /** SP 记录上次空间签到开关状态，用于检测关→开变化 */
    private const val SP_CHECKIN_LAST_SWITCH = "qzone_checkin_last_switch"
    /** SP 记录上次日签打卡开关状态，用于检测关→开变化 */
    private const val SP_DAILY_SIGN_LAST_SWITCH = "qq_daily_sign_last_switch"
    /** SP 记录上次大会员签到开关状态，用于检测关→开变化 */
    private const val SP_BIGVIP_LAST_SWITCH = "qq_bigvip_last_switch"

    /** 等级加速目标机器人 QQ 列表 */
    private val BOT_UINS = arrayOf(
        "66600000", "2854213893", "2854205672", "2854196306",
        "3889697341", "3889699650", "3889233115", "3889239358",
        "3889006667", "3889005595", "3889047008", "4014803937",
        "4015811614", "4018195174", "3889559736", "3889045760"
    )

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
    fun isLevelBoostEnabled(): Boolean = ModuleConfig.getBoolean(SP_LEVEL_BOOST_ENABLED, false)
    fun isSpaceBrowseEnabled(): Boolean = ModuleConfig.getBoolean(SP_SPACE_BROWSE_ENABLED, false)
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
        // 启动时补执行：等级加速若开启且今日未执行（覆盖0点手机不在线场景）
        Thread {
            runCatching {
                val today = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
                // 加好友
                val on = isLevelBoostEnabled()
                ModuleConfig.putBoolean(SP_LEVEL_BOOST_LAST_SWITCH, on)
                if (on && !isDoneToday(SP_PREFIX_LEVEL_BOOST_DONE, today)) {
                    runLevelBoost(today)
                }
                // 空间浏览
                val sbOn = isSpaceBrowseEnabled()
                ModuleConfig.putBoolean(SP_SPACE_BROWSE_LAST_SWITCH, sbOn)
                if (sbOn && !isDoneToday(SP_PREFIX_SPACE_BROWSE_DONE, today)) {
                    runSpaceBrowse(today)
                }
                // 空间签到
                val ckOn = isCheckinEnabled()
                ModuleConfig.putBoolean(SP_CHECKIN_LAST_SWITCH, ckOn)
                if (ckOn && !isDoneToday(SP_PREFIX_CHECKIN_DONE, today)) {
                    runCheckin(today)
                }
                // 日签打卡
                val dsOn = isDailySignEnabled()
                ModuleConfig.putBoolean(SP_DAILY_SIGN_LAST_SWITCH, dsOn)
                if (dsOn && !isDoneToday(SP_PREFIX_DAILY_SIGN_DONE, today)) {
                    runDailySign(today)
                }
                // 大会员签到
                val bvOn = isBigVipCheckinEnabled()
                ModuleConfig.putBoolean(SP_BIGVIP_LAST_SWITCH, bvOn)
                if (bvOn && !isDoneToday(SP_PREFIX_BIGVIP_DONE, today)) {
                    runBigVipCheckin(today)
                }
            }.onFailure { LogUtils.e(TAG, "boot level boost error: ${it.message}") }
        }.start()
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

            // =============== 【等级加速·加好友：开关关→开时立即执行一次（非0点开启场景）】 ===============
            if (uin.isNotEmpty()) {
                val levelBoostOn = isLevelBoostEnabled()
                val lastSwitch = ModuleConfig.getBoolean(SP_LEVEL_BOOST_LAST_SWITCH, false)
                if (levelBoostOn && !lastSwitch && !isDoneToday(SP_PREFIX_LEVEL_BOOST_DONE, today)) {
                    runLevelBoost(today)
                }
                if (levelBoostOn != lastSwitch) {
                    ModuleConfig.putBoolean(SP_LEVEL_BOOST_LAST_SWITCH, levelBoostOn)
                }
            }

            // =============== 【等级加速·空间浏览：开关关→开时立即执行一次（非0点开启场景）】 ===============
            if (uin.isNotEmpty()) {
                val sbOn = isSpaceBrowseEnabled()
                val lastSwitchSb = ModuleConfig.getBoolean(SP_SPACE_BROWSE_LAST_SWITCH, false)
                if (sbOn && !lastSwitchSb && !isDoneToday(SP_PREFIX_SPACE_BROWSE_DONE, today)) {
                    runSpaceBrowse(today)
                }
                if (sbOn != lastSwitchSb) {
                    ModuleConfig.putBoolean(SP_SPACE_BROWSE_LAST_SWITCH, sbOn)
                }
            }

            // =============== 【等级加速·空间签到：开关关→开时立即执行一次】 ===============
            if (uin.isNotEmpty()) {
                val on = isCheckinEnabled()
                val last = ModuleConfig.getBoolean(SP_CHECKIN_LAST_SWITCH, false)
                if (on && !last && !isDoneToday(SP_PREFIX_CHECKIN_DONE, today)) {
                    runCheckin(today)
                }
                if (on != last) {
                    ModuleConfig.putBoolean(SP_CHECKIN_LAST_SWITCH, on)
                }
            }

            // =============== 【等级加速·日签打卡：开关关→开时立即执行一次】 ===============
            if (uin.isNotEmpty()) {
                val on = isDailySignEnabled()
                val last = ModuleConfig.getBoolean(SP_DAILY_SIGN_LAST_SWITCH, false)
                if (on && !last && !isDoneToday(SP_PREFIX_DAILY_SIGN_DONE, today)) {
                    runDailySign(today)
                }
                if (on != last) {
                    ModuleConfig.putBoolean(SP_DAILY_SIGN_LAST_SWITCH, on)
                }
            }

            // =============== 【等级加速·大会员签到：开关关→开时立即执行一次】 ===============
            if (uin.isNotEmpty()) {
                val on = isBigVipCheckinEnabled()
                val last = ModuleConfig.getBoolean(SP_BIGVIP_LAST_SWITCH, false)
                if (on && !last && !isDoneToday(SP_PREFIX_BIGVIP_DONE, today)) {
                    runBigVipCheckin(today)
                }
                if (on != last) {
                    ModuleConfig.putBoolean(SP_BIGVIP_LAST_SWITCH, on)
                }
            }

            // =============== 【等级加速：凌晨 00:00 所有任务触发】 ===============
            val triggerTime = hitTime(hm, CHECKIN_TRIGGER_TIME)
            if (triggerTime && uin.isNotEmpty()) {
                // ① 空间等级签到
                if (isCheckinEnabled() && !isDoneToday(SP_PREFIX_CHECKIN_DONE, today)) {
                    runCheckin(today)
                }
                // ② QQ 日签打卡
                if (isDailySignEnabled() && !isDoneToday(SP_PREFIX_DAILY_SIGN_DONE, today)) {
                    runDailySign(today)
                }
                // ③ 大会员签到
                if (isBigVipCheckinEnabled() && !isDoneToday(SP_PREFIX_BIGVIP_DONE, today)) {
                    runBigVipCheckin(today)
                }
                // ④ 等级加速·加好友
                if (isLevelBoostEnabled() && !isDoneToday(SP_PREFIX_LEVEL_BOOST_DONE, today)) {
                    runLevelBoost(today)
                }
                // ⑤ 等级加速·空间浏览
                if (isSpaceBrowseEnabled() && !isDoneToday(SP_PREFIX_SPACE_BROWSE_DONE, today)) {
                    runSpaceBrowse(today)
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

    // ====================================== 等级加速·签到类 ======================================

    private val checkinRunning = AtomicBoolean(false)
    private val dailySignRunning = AtomicBoolean(false)
    private val bigVipRunning = AtomicBoolean(false)

    /** 等级加速·空间签到 */
    private fun runCheckin(today: String) {
        if (!checkinRunning.compareAndSet(false, true)) return
        Thread {
            runCatching {
                val ok = QZoneLikeTool.qzoneClockIn()
                if (ok) markDoneToday(SP_PREFIX_CHECKIN_DONE, today)
                forEachChecked { it.onScheduleCheckinResult(ok) }
            }.onFailure { LogUtils.e(TAG, "runCheckin error: ${it.message}") }
            checkinRunning.set(false)
        }.start()
    }

    /** 等级加速·QQ日签打卡 */
    private fun runDailySign(today: String) {
        if (!dailySignRunning.compareAndSet(false, true)) return
        Thread {
            runCatching {
                val (ok, msg) = QZoneLikeTool.dailySign()
                if (ok) markDoneToday(SP_PREFIX_DAILY_SIGN_DONE, today)
                forEachChecked { it.onDailySignResult(ok, msg) }
            }.onFailure { LogUtils.e(TAG, "runDailySign error: ${it.message}") }
            dailySignRunning.set(false)
        }.start()
    }

    /** 等级加速·大会员签到 */
    private fun runBigVipCheckin(today: String) {
        if (!bigVipRunning.compareAndSet(false, true)) return
        Thread {
            runCatching {
                val ok = QZoneLikeTool.bigVipClockIn()
                if (ok) markDoneToday(SP_PREFIX_BIGVIP_DONE, today)
                forEachChecked { it.onBigVipCheckinResult(ok) }
            }.onFailure { LogUtils.e(TAG, "runBigVipCheckin error: ${it.message}") }
            bigVipRunning.set(false)
        }.start()
    }

    // ====================================== 等级加速·加好友 ======================================

    private val levelBoostRunning = java.util.concurrent.atomic.AtomicBoolean(false)

    /** 等级加速·自动加机器人好友：已是好友则跳过，不是好友才加，每次间隔数秒 */
    private fun runLevelBoost(today: String) {
        if (!levelBoostRunning.compareAndSet(false, true)) return
        Thread {
            runCatching {
                var addedCount = 0
                for (botUin in BOT_UINS) {
                    try {
                        if (FriendTool.isFriend(botUin)) {
                            // LogUtils.d(TAG, "level boost: $botUin already friend, skip")
                            continue
                        }
                        ExtraTool.addFriend(botUin, "", "")
                        addedCount++
                        Thread.sleep(5000)
                    } catch (e: Throwable) {
                        LogUtils.e(TAG, "level boost add $botUin error: ${e.message}")
                    }
                }
                // 不管加了多少个都标记完成，避免反复触发
                markDoneToday(SP_PREFIX_LEVEL_BOOST_DONE, today)
                LogUtils.d(TAG, "level boost done: added $addedCount friends")
            }.onFailure { LogUtils.e(TAG, "runLevelBoost error: ${it.message}") }
            levelBoostRunning.set(false)
        }.start()
    }

    // ====================================== 空间浏览 ======================================

    private val spaceBrowseRunning = AtomicBoolean(false)
    private val MOOD_URL_PATTERN: Pattern =
        Pattern.compile("https?:\\\\?/\\\\?/user\\.qzone\\.qq\\.com\\\\?/\\d+\\\\?/mood\\\\?/[A-Za-z0-9]+")

    /** 等级加速·空间浏览：请求好友说说列表 → 提取15条mood链接 → 带cookie访问 */
    private fun runSpaceBrowse(today: String) {
        if (!spaceBrowseRunning.compareAndSet(false, true)) return
        Thread {
            runCatching {
                val uin = QQCurrentEnv.getCurrentUin()
                if (uin.isNullOrEmpty()) {
                    LogUtils.e(TAG, "space browse: uin empty")
                    return@Thread
                }
                // 构造 GetFriendFeeds 请求
                val req = JSONObject()
                req.put("1", 20)
                req.put("2", 0)
                val req2 = JSONObject()
                req2.put("1", uin)
                req.put("3", req2)

                var links: List<String>? = null
                val cmd = "QzoneV4Service.trpc.qzone.feeds_reader.FeedsReader.GetFriendFeeds"

                val latch = java.util.concurrent.CountDownLatch(1)
                PacketHelper.sendPacket(cmd, req, object : packetListener {
                    override fun onResult(success: Boolean, json: JSONObject) {
                        try {
                            if (success) {
                                links = extractMoodLinks(json).take(15)
                                // LogUtils.i(TAG, "space browse: got ${links!!.size} mood links")
                            } else {
                                LogUtils.e(TAG, "space browse: sendPacket failed: ${json.optString("2")}")
                            }
                        } finally {
                            latch.countDown()
                        }
                    }
                })
                runCatching { latch.await(15, java.util.concurrent.TimeUnit.SECONDS) }

                val urls = links
                if (urls.isNullOrEmpty()) {
                    LogUtils.e(TAG, "space browse: no mood links extracted, mark done anyway")
                    markDoneToday(SP_PREFIX_SPACE_BROWSE_DONE, today)
                    return@Thread
                }

                val p_skey = CookieTool.getCookie("p_skey").orEmpty()
                val skey = CookieTool.getCookie("skey").orEmpty()
                val uin10 = QQCurrentEnv.getCookieUin()
                val cookie = buildString {
                    append("uin=$uin10; ")
                    if (p_skey.isNotEmpty()) append("p_skey=$p_skey; ")
                    if (skey.isNotEmpty()) append("skey=$skey; ")
                }
                var successCount = 0
                for (url in urls) {
                    runCatching {
                        val headers = HashMap<String, String>()
                        headers["Cookie"] = cookie
                        headers["User-Agent"] = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
                        headers["Referer"] = "https://user.qzone.qq.com/"
                        HttpUtils.get(url, headers, 10000, 15000)
                        successCount++
                    }.onFailure { LogUtils.e(TAG, "space browse visit $url error: ${it.message}") }
                    try { Thread.sleep(800) } catch (_: Throwable) {}
                }
                markDoneToday(SP_PREFIX_SPACE_BROWSE_DONE, today)
                // LogUtils.i(TAG, "space browse done: visited $successCount/${urls.size}")
            }.onFailure { LogUtils.e(TAG, "runSpaceBrowse error: ${it.message}") }
            spaceBrowseRunning.set(false)
        }.start()
    }

    /** 从JSONObject的全文字符串中提取 mood 链接，去重保持顺序 */
    private fun extractMoodLinks(json: JSONObject): List<String> {
        val result = ArrayList<String>()
        val seen = HashSet<String>()
        try {
            val matcher = MOOD_URL_PATTERN.matcher(json.toString())
            while (matcher.find()) {
                val url = matcher.group().replace("\\/", "/")
                if (seen.add(url)) result.add(url)
            }
        } catch (t: Throwable) {
            LogUtils.e(TAG, "extractMoodLinks error: ${t.message}")
        }
        return result
    }
}
