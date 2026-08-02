package me.lengyu.qedge.hook.item

import me.lengyu.qedge.utils.LogUtils
import me.lengyu.qedge.utils.QQCurrentEnv
import me.lengyu.qedge.utils.dexkit.DexKitTask
import me.lengyu.qedge.utils.qq.CookieTool
import me.lengyu.qedge.hook.annotation.HookItemAnnotation
import me.lengyu.qedge.hook.annotation.HookCategory
import org.luckypray.dexkit.query.base.BaseQuery
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
            // LogUtils.e(TAG, "doLike result: $result")
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
            val cookieUin = QQCurrentEnv.getCookieUin()
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
            // LogUtils.e(TAG, "doComment result: $result")
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

    override fun getQueryMap(): Map<String, BaseQuery> = emptyMap()
}
