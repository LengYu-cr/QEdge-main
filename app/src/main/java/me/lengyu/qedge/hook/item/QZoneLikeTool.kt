package me.lengyu.qedge.hook.item

import me.lengyu.qedge.utils.LogUtils
import me.lengyu.qedge.utils.qq.QQCurrentEnv
import me.lengyu.qedge.utils.dexkit.DexKitTask
import me.lengyu.qedge.utils.qq.CookieTool
import me.lengyu.qedge.hook.annotation.HookItemAnnotation
import me.lengyu.qedge.hook.annotation.HookCategory
import org.luckypray.dexkit.query.base.BaseFinder
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
@HookItemAnnotation(value = "QZone点赞工具", category = "item")
object QZoneLikeTool : DexKitTask {

    const val TAG = "QZoneLikeTool"

    fun hookLike() {
    }

    /**
     * 点赞
     * @param likeKey 动态的likeKey，如 http://user.qzone.qq.com/{uin}/mood/{cellid},
     * @param likeType 点赞类型，默认1
     * @return 是否成功
     */
    fun doLike(likeKey: String, likeType: Int = 1): Boolean {
        return runCatching {
            val uin = QQCurrentEnv.getCurrentUin() ?: return@runCatching false
            val cookieUin = QQCurrentEnv.getCookieUin()
            val skey = CookieTool.getSkey() ?: return@runCatching false
            val pSkey = CookieTool.getPskey("qzone.qq.com") ?: return@runCatching false
            val gtk = CookieTool.getBkn(pSkey)

            val encodedKey = URLEncoder.encode(likeKey, "UTF-8")
            val urlStr = "https://h5.qzone.qq.com/proxy/domain/w.qzone.qq.com/cgi-bin/likes/internal_dolike_app?g_tk=$gtk"
            val postData = "opuin=$uin&unikey=$encodedKey&curkey=$encodedKey&appid=311&opr_type=like&format=purejson"

            val cookie = "uin=$cookieUin; skey=$skey; p_uin=$cookieUin; p_skey=$pSkey"

            val result = httpPostForm(urlStr, postData, cookie)
            LogUtils.d(TAG, "doLike result: $result")
            result.contains("\"ret\":0") || result.contains("\"code\":0")
        }.onFailure {
            LogUtils.e(TAG, "doLike error: ${it.message}")
        }.getOrDefault(false)
    }

    /**
     * 通过uin和cellid点赞
     */
    fun doLikeByUinAndCellid(uin: String, cellid: String, likeType: Int = 1): Boolean {
        val cleanCellid = cellid.trimEnd(',')
        val likeKey = "http://user.qzone.qq.com/$uin/mood/$cleanCellid"
        return doLike(likeKey, likeType)
    }

    /**
     * 发评论
     * @param ownUin 对方QQ号（说说主人）
     * @param cellid 说说ID
     * @param content 评论内容
     * @param isPrivate 是否私密评论
     * @return 是否成功
     */
    fun doComment(ownUin: String, cellid: String, content: String, isPrivate: Boolean = false): Boolean {
        return runCatching {
            val myUin = QQCurrentEnv.getCurrentUin() ?: return@runCatching false
            val cookieUin = QQCurrentEnv.getCookieUin() ?: return@runCatching false
            val skey = CookieTool.getSkey() ?: return@runCatching false
            val pSkey = CookieTool.getPskey("qzone.qq.com") ?: return@runCatching false
            val gtk = CookieTool.getBkn(pSkey)

            val cleanCellid = cellid.trimEnd(',')
            val urlStr = "https://h5.qzone.qq.com/webapp/json/qzoneOperation/addComment?g_tk=$gtk"
            val cookie = "uin=$cookieUin; skey=$skey; p_uin=$cookieUin; p_skey=$pSkey"

            val jsonBody = "{" +
                "\"appid\":311," +
                "\"uin\":$myUin," +
                "\"ownuin\":\"$ownUin\"," +
                "\"srcId\":\"$cleanCellid\"," +
                "\"content\":\"${escapeJson(content)}\"," +
                "\"isPrivateComment\":${if (isPrivate) 1 else 0}," +
                "\"busi_param\":{}," +
                "\"bypass_param\":{}" +
                "}"

            val result = httpPostJson(urlStr, jsonBody, cookie)
            LogUtils.d(TAG, "doComment result: $result")
            result.contains("\"ret\":0") || result.contains("\"code\":0")
        }.onFailure {
            LogUtils.e(TAG, "doComment error: ${it.message}")
        }.getOrDefault(false)
    }

    private fun escapeJson(str: String): String {
        return str.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    private fun httpPostForm(urlStr: String, data: String, cookie: String): String {
        return httpPost(urlStr, data, "application/x-www-form-urlencoded; charset=UTF-8", cookie)
    }

    private fun httpPostJson(urlStr: String, json: String, cookie: String): String {
        return httpPost(urlStr, json, "application/json; charset=UTF-8", cookie)
    }

    private fun httpPost(urlStr: String, data: String, contentType: String, cookie: String): String {
        var conn: HttpURLConnection? = null
        var reader: BufferedReader? = null
        return try {
            val url = URL(urlStr)
            conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            conn.doOutput = true
            conn.doInput = true
            conn.setRequestProperty("Content-Type", contentType)
            conn.setRequestProperty("Cookie", cookie)
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 12) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/89.0.4389.72 MQQBrowser/6.2 Mobile Safari/537.36")
            conn.setRequestProperty("Referer", "https://h5.qzone.qq.com/")

            val out = DataOutputStream(conn.outputStream)
            out.write(data.toByteArray(Charsets.UTF_8))
            out.flush()
            out.close()

            reader = BufferedReader(InputStreamReader(conn.inputStream, "UTF-8"))
            val sb = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                sb.append(line)
            }
            sb.toString()
        } catch (e: Exception) {
            LogUtils.e(TAG, "httpPost error: ${e.message}")
            ""
        } finally {
            try { reader?.close() } catch (_: Exception) {}
            conn?.disconnect()
        }
    }

    /**
     * 定时发说说
     * 接口来源：taotao.qzone.qq.com emotion_cgi_publish_v6（QQ空间 PC/H5 发表纯文字说说）
     * @param msg 说说内容，中文会自动 UTF-8 编码
     * @return 是否成功
     */
    fun publishMood(msg: String): Boolean {
        return runCatching {
            val uin = QQCurrentEnv.getCurrentUin() ?: return@runCatching false
            val skey = CookieTool.getSkey() ?: return@runCatching false
            val pSkey = CookieTool.getPskey("qzone.qq.com") ?: return@runCatching false
            val bkn = CookieTool.getBkn(skey)
            val cookieUin = QQCurrentEnv.getCookieUin() ?: return@runCatching false

            val urlStr = "https://user.qzone.qq.com/proxy/domain/taotao.qzone.qq.com/cgi-bin/emotion_cgi_publish_v6?g_tk=$bkn"
            val encodedMsg = URLEncoder.encode(msg, "UTF-8")
            val referer = URLEncoder.encode("https://user.qzone.qq.com/$uin/infocenter?via=toolbar", "UTF-8")

            val postData = "syn_tweet_verson=1" +
                "&paramstr=1" +
                "&pic_template=&richtype=&richval=&special_url=&subrichtype=" +
                "&con=qm$encodedMsg" +
                "&feedversion=1&ver=1&ugc_right=1&to_sign=1" +
                "&hostuin=$uin" +
                "&code_version=1&format=fs" +
                "&qzreferrer=$referer"

            val cookie = "uin=$cookieUin; skey=$skey; p_uin=$cookieUin; p_skey=$pSkey"
            val result = httpPostForm(urlStr, postData, cookie)
            // LogUtils.d(TAG, "publishMood result=${if (result.length > 300) result.substring(0, 300) else result}")
            result.contains("\"code\":0,") || result.contains("\"ret\":0")
        }.onFailure {
            LogUtils.e(TAG, "publishMood error: ${it.message}")
        }.getOrDefault(false)
    }

    /**
     * 空间等级签到（等级加速类）
     * 接口来源：h5.qzone.qq.com publishDiyMood/publishmood（小程序入口的每日签到模板）
     * @param content 签到自定义文字，默认 "123" 可改
     * @return 是否成功
     */
    fun qzoneClockIn(content: String = "QEdge每日空间签到"): Boolean {
        return runCatching {
            val cookieUin = QQCurrentEnv.getCookieUin() ?: return@runCatching false
            val uin = QQCurrentEnv.getCurrentUin() ?: return@runCatching false
            val skey = CookieTool.getSkey() ?: return@runCatching false
            val pSkey = CookieTool.getPskey("qzone.qq.com") ?: return@runCatching false
            val gtk = CookieTool.getBkn(pSkey)
            
            val t = System.currentTimeMillis()
            val checkinfall = """{"uuid":"$t","id":"union_${t}_$uin","desc":"","cover_url":"","cover_width":0,"cover_height":0,"location":"","material_id":"13||1102","material_cate_1":"13","material_cate_2":"","from":"miniprogram","adId":"randomsignin"}"""
            val eventTags = "${uin}_QEdge_每日签到"
            val richvalList = "aurl=https%3A%2F%2Fsfile.chatglm.cn%2Fchatglm4%2F0696bf9d-97f7-4cd3-85ee-176f4123ead3.jpg" +
                "&murl=https%3A%2F%2Fsfile.chatglm.cn%2Fchatglm4%2F0696bf9d-97f7-4cd3-85ee-176f4123ead3.jpg" +
                "&m_width=2916&m_length=2187" +
                "&burl=https%3A%2F%2Fsfile.chatglm.cn%2Fchatglm4%2F0696bf9d-97f7-4cd3-85ee-176f4123ead3.jpg" +
                "&b_width=2916&b_length=2187" +
                "&pic_type=1000&templateId=1&who=2"

            val safeContent = content.replace("\\", "\\\\").replace("\"", "\\\"")
            val safeCheckinfall = checkinfall.replace("\\", "\\\\").replace("\"", "\\\"")
            val safeEventTags = eventTags.replace("\\", "\\\\").replace("\"", "\\\"")
            val safeRichval = richvalList.replace("\\", "\\\\").replace("\"", "\\\"")

            val data1 = """{"uin":$uin,"content":"$safeContent","extend_info":{"checkinfall":"$safeCheckinfall","comm_self_define_tail_id":"5"},"format":"json","frames":10,"inCharset":"utf-8","issynctoweibo":"0","isWinPhone":"3","outCharset":"utf-8","richtype":"1","richval":"$safeRichval","right_info":{"ugc_right":1},"source":{"subtype":"33"},"stored_extend_info":{"is_diy":"1","event_tags":"$safeEventTags","pic_jump_type":"0","sign_content":"","use_useragent_tail":"1"},"lbsinfo":{"lbs_nm":"","lbs_type":1,"lbs_x":"","lbs_y":""}}"""

            val urlStr = "https://h5.qzone.qq.com/webapp/json/publishDiyMood/publishmood?g_tk=$gtk"
            val cookie = "uin=$cookieUin; skey=$skey; p_uin=$cookieUin; p_skey=$pSkey"

            val result = httpPostJson(urlStr, data1, cookie)
            // LogUtils.d(TAG, "qzoneClockIn result=${if (result.length > 300) result.substring(0, 300) else result}")
            result.contains("data is ok!") || result.contains("\"ret\":0") || result.contains("\"code\":0")
        }.onFailure {
            LogUtils.e(TAG, "qzoneClockIn error: ${it.message}")
        }.getOrDefault(false)
    }

    /**
     * QQ 日签打卡（等级加速类，凌晨执行）
     * 接口来源：ti.qq.com hybrid-h5 /daily_attendance/SignIn
     * @return Pair(是否成功, 返回消息/原因)，成功=true 或 已签过=true（retCode=1 也要写幂等）
     */
    fun dailySign(): Pair<Boolean, String> {
        return runCatching {
            val cookieUin = QQCurrentEnv.getCookieUin() ?: return@runCatching false to "当前QQ未登录"
            val uin = QQCurrentEnv.getCurrentUin() ?: return@runCatching false to "当前QQ未登录"
            val skey = CookieTool.getSkey() ?: return@runCatching false to "未获取到 skey"
            val tiPskey = CookieTool.getPskey("ti.qq.com") ?: return@runCatching false to "未获取到 ti.qq.com pskey"
            val cookie = "uin=$cookieUin; skey=$skey; p_uin=$cookieUin; p_skey=$tiPskey"


            val urlStr = "https://ti.qq.com/hybrid-h5/api/json/daily_attendance/SignIn"
            // mpExtend.tianshuAdsReq：app/os/version/imei 基本固定即可（广告请求元数据，QQ版本填 8.8.93 兼容）
            val adsReq = """{"app":"QQ","os":"Android","version":"8.8.93","imei":""}""".replace("\"", "\\\"")
            val body = """{"uin":"$uin","type":1,"sId":"","qua":"V1_AND_SQ_8.8.93_2862_HDBM_T","mpExtend":{"tianshuAdsReq":"$adsReq"}}"""

            val conn: HttpURLConnection?
            var reader: BufferedReader? = null
            val raw = try {
                val url = URL(urlStr)
                conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.connectTimeout = 10000
                conn.readTimeout = 10000
                conn.doOutput = true
                conn.doInput = true
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                conn.setRequestProperty("Cookie", cookie)
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 12) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/89.0.4389.72 MQQBrowser/6.2 Mobile Safari/537.36")
                conn.setRequestProperty("Referer", "https://ti.qq.com/")

                val out = DataOutputStream(conn.outputStream)
                out.write(body.toByteArray(Charsets.UTF_8))
                out.flush(); out.close()

                reader = BufferedReader(InputStreamReader(conn.inputStream, "UTF-8"))
                val sb = StringBuilder(); var line: String?
                while (reader.readLine().also { line = it } != null) sb.append(line)
                sb.toString()
            } catch (e: Exception) {
                LogUtils.e(TAG, "dailySign http error: ${e.message}")
                return@runCatching false to "网络异常: ${e.message}"
            } finally {
                try { reader?.close() } catch (_: Exception) {}
            }
            // LogUtils.d(TAG, "dailySign result=${if (raw.length > 300) raw.substring(0, 300) else raw}")

            val root = org.json.JSONObject(raw)
            val data = root.optJSONObject("data") ?: return@runCatching false to "接口返回异常: ${raw.take(100)}"
            val retCode = data.optString("retCode", "-1").trim()
            when (retCode) {
                "0" -> {
                    val signOutLook = data.optJSONObject("signInOutLook")
                    val title = signOutLook?.optString("title", "").orEmpty()
                    val desc = signOutLook?.optString("desc", "").orEmpty()
                    true to "日签打卡成功\n今日主题: $title\n$desc"
                }
                "1" -> {
                    // retCode=1 是今日已打卡，也算"今天任务完成"，返回 true 好让调度器写幂等标记
                    true to "今日已打过日签卡"
                }
                else -> false to "日签失败 retCode=$retCode"
            }
        }.onFailure {
            LogUtils.e(TAG, "dailySign error: ${it.message}")
        }.getOrDefault(false to "日签异常")
    }

    /**
     * 大会员签到（等级加速类，凌晨执行）
     * 接口来源：h5.qzone.qq.com /vip/fcg-bin/v2/fcg_vip_task_checkin op=CheckIn appid=qq_big_vip
     * @return 是否成功
     */
    fun bigVipClockIn(): Boolean {
        return runCatching {
            val uin = QQCurrentEnv.getCurrentUin() ?: return@runCatching false
            val skey = CookieTool.getSkey() ?: return@runCatching false
            val qzone = CookieTool.getPskey("qzone.qq.com") ?: return@runCatching false
            // h5.qzone.qq.com VIP 接口的 g_tk 不是用 skey，而是用 qzone.qq.com 域 p_skey 算（否则 -10000 g_tk invalid）
            val bkn = CookieTool.getBkn(qzone)
            val cookieUin = "o$uin"
            val cookie = "uin=$cookieUin; skey=$skey; p_uin=$cookieUin; p_skey=$qzone"

            val urlStr = "https://h5.qzone.qq.com/p/vip/fcg-bin/v2/fcg_vip_task_checkin?g_tk=$bkn"
            val postData = "appid=qq_big_vip&op=CheckIn&format=json&inCharset=utf-8&outCharset=utf-8"
            val result = httpPostForm(urlStr, postData, cookie)
            // LogUtils.d(TAG, "bigVipClockIn result=${if (result.length > 300) result.substring(0, 300) else result}")
            // -1 / code=0 / msg=成功 / ret=0 任一命中算成功
            result.contains("\"ret\":0") || result.contains("\"code\":0")
                || result.contains("\"msg\":\"成功\"") || result.contains("\"msg\":\"已签到\"")
                || result.contains("CheckIn成功") || result.contains("success")
        }.onFailure {
            LogUtils.e(TAG, "bigVipClockIn error: ${it.message}")
        }.getOrDefault(false)
    }

    override fun getQueryMap(): Map<String, BaseFinder> = emptyMap()
}
