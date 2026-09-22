package me.lengyu.qedge.plugin.view

import android.app.Activity
import android.content.Context
import android.view.View
import com.tencent.qqnt.aio.activity.AIODelegate
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import me.lengyu.qedge.lifecycle.Parasitics
import me.lengyu.qedge.ui.core.compatibility.QEdgeBottomDialog
import me.lengyu.qedge.ui.pages.media.MediaPanelContent
import me.lengyu.qedge.utils.HttpUtils
import me.lengyu.qedge.utils.LogUtils
import me.lengyu.qedge.utils.ModuleConfig
import me.lengyu.qedge.utils.Toasts
import me.lengyu.qedge.utils.qq.ExtraTool
import me.lengyu.qedge.utils.qq.FriendTool
import me.lengyu.qedge.utils.qq.QQCurrentEnv
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.URLEncoder
import java.security.MessageDigest
import java.util.regex.Pattern

/**
 * 表情/语音/视频综合面板入口加载器。
 *
 * 与 [ChatSettingLoader] 共用「长按聊天页图标」的注入思路，但使用独立的配置项与入口点，
 * 避免与脚本菜单入口冲突：
 *  - 开关：media_panel_enabled
 *  - 入口：media_panel_entry（默认 album，与脚本菜单默认 more_features 不同）
 *  - 默认栏：media_panel_default_tab（img / voice / video）
 */
object MediaPanelLoader {

    const val KEY_ENABLED = "media_panel_enabled"
    const val KEY_ENTRY = "media_panel_entry"
    const val KEY_DEFAULT_TAB = "media_panel_default_tab"

    // 可选入口匹配点（与 ChatSettingLoader 同源标签，触发点需在设置里与脚本菜单错开）
    val ENTRY_OPTIONS = mapOf(
        "more_features" to "更多功能",
        "chat_settings" to "聊天设置",
        "bubble" to "泡泡",
        "emoji" to "表情",
        "camera" to "拍照",
        "album" to "相册",
        "voice" to "语音"
    )

    const val API_BASE = "https://v.yuafeng.cn/QEdge/api/media/"

    data class PanelContact(
        val chatType: Int = 0,
        val peerUin: String = "",
        val peerName: String = ""
    )

    data class MediaCollection(
        val name: String,
        val cover: String,
        val count: Int,
        val tags: List<String> = emptyList()
    )

    data class MediaItem(val url: String, val id: Int = 0, val uin: String = "", val createTime: String = "")

    /** 上传结果：成功入库数与失败数（失败 = 上传图床失败 + 服务器拒绝 + 入库异常） */
    data class UploadResult(val success: Int, val failed: Int)

    private var hooked = false
    private var aioDelegate: AIODelegate? = null

    val currentContact: PanelContact
        get() = aioDelegate?.let { parseContact(it.aioContact.toString()) } ?: PanelContact()

    @JvmStatic
    fun loadHook() {
        if (hooked) return
        hooked = true

        try {
            XposedBridge.hookAllMethods(
                android.widget.ImageView::class.java,
                "onAttachedToWindow",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        try {
                            if (!ModuleConfig.getBoolean(KEY_ENABLED, false)) return
                            val entryMode = ModuleConfig.getString(KEY_ENTRY, "album")
                            val targetText = ENTRY_OPTIONS[entryMode] ?: "相册"

                            val view = param.thisObject as android.widget.ImageView
                            val desc = view.contentDescription
                            if (desc != null && desc.toString().contains(targetText)) {
                                view.post {
                                    try {
                                        view.setOnLongClickListener {
                                            showMediaPanel(view)
                                            true
                                        }
                                    } catch (e: Throwable) {
                                        LogUtils.e("MediaPanelLoader", "setOnLongClickListener failed: " + e.message)
                                    }
                                }
                            }
                        } catch (e: Throwable) {
                            LogUtils.e("MediaPanelLoader", "afterHookedMethod failed: " + e.message)
                        }
                    }
                }
            )

            XposedBridge.hookAllMethods(
                AIODelegate::class.java,
                "show",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        try {
                            aioDelegate = param.thisObject as AIODelegate
                        } catch (e: Throwable) {
                            LogUtils.e("MediaPanelLoader", "hook AIODelegate.show failed: " + e.message)
                        }
                    }
                }
            )

            XposedBridge.hookAllMethods(
                AIODelegate::class.java,
                "hide",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        aioDelegate = null
                    }
                }
            )
        } catch (e: Throwable) {
            LogUtils.e("MediaPanelLoader", "loadHook failed: " + e.message)
        }
    }

    private fun parseContact(input: String): PanelContact {
        return try {
            val regex = Pattern.compile("(\\w+)=([^,)]*)")
            val matcher = regex.matcher(input)
            val map = mutableMapOf<String, String>()
            while (matcher.find()) {
                val key = matcher.group(1) ?: continue
                val value = matcher.group(2)?.trim('\'') ?: ""
                map[key] = value
            }

            val chatType = try {
                map["chatType"]?.toInt() ?: 0
            } catch (e: NumberFormatException) {
                0
            }
            val peerUid = map["peerUid"] ?: ""
            val peerName = map["nick"] ?: ""

            val peerUin = if (chatType == 2) {
                peerUid
            } else {
                try {
                    FriendTool.getUinFromUid(peerUid).ifEmpty { "" }
                } catch (e: Throwable) {
                    ""
                }
            }
            PanelContact(chatType, peerUin, peerName)
        } catch (e: Throwable) {
            PanelContact()
        }
    }

    private fun showMediaPanel(view: View) {
        val hostActivity = QQCurrentEnv.getActivity() ?: (view.context as? Activity)
        if (hostActivity == null || hostActivity.isFinishing || hostActivity.isDestroyed) {
            LogUtils.e("MediaPanelLoader", "skip showMediaPanel: host activity invalid")
            Toasts.toast("当前页面状态异常")
            return
        }
        Parasitics.ensureInitialized(hostActivity)
        runCatching { Parasitics.injectModuleResources(hostActivity.resources) }

        val contact = currentContact
        if (contact.peerUin.isEmpty()) {
            Toasts.toast("获取聊天信息失败")
            return
        }

        try {
            QEdgeBottomDialog(hostActivity) { dismiss ->
                MediaPanelContent(contact, dismiss)
            }.show()
        } catch (e: Throwable) {
            LogUtils.e("MediaPanelLoader", "showMediaPanel failed: " + e.message)
            Toasts.toast("打开面板失败: " + e.message)
        }
    }

    /**
     * 媒体接口封装（全部为阻塞调用，需在 IO 线程执行）
     */
    object MediaApi {

        fun fetchCollections(type: String, limit: Int = 500): List<MediaCollection> {
            val url = API_BASE + "list.php?type=" + type + "&limit=" + limit
            val body = HttpUtils.get(url) ?: return emptyList()
            return parseCollections(body)
        }

        fun searchCollections(type: String, keyword: String): List<MediaCollection> {
            val url = API_BASE + "search.php?type=" + type + "&keyword=" + URLEncoder.encode(keyword, "UTF-8")
            val body = HttpUtils.get(url) ?: return emptyList()
            return parseCollections(body)
        }

        private fun parseCollections(body: String): List<MediaCollection> {
            return try {
                val root = JSONObject(body)
                if (root.optInt("code") != 200) return emptyList()
                val arr = root.optJSONObject("data")?.optJSONArray("collections") ?: return emptyList()
                val list = mutableListOf<MediaCollection>()
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    val tagsArr = o.optJSONArray("tags")
                    val tags = mutableListOf<String>()
                    if (tagsArr != null) {
                        for (j in 0 until tagsArr.length()) {
                            val t = tagsArr.optString(j)
                            if (t.isNotEmpty() && t !in tags) tags.add(t)
                        }
                    }
                    list.add(
                        MediaCollection(
                            o.optString("name"),
                            o.optString("cover"),
                            o.optInt("count"),
                            tags
                        )
                    )
                }
                list.filter { it.name.isNotEmpty() && it.cover.isNotEmpty() }
            } catch (e: Throwable) {
                LogUtils.e("MediaApi", "parseCollections failed: " + e.message)
                emptyList()
            }
        }

        fun fetchCollectionItems(name: String, type: String): List<MediaItem> {
            val url = API_BASE + "collection.php?name=" + URLEncoder.encode(name, "UTF-8") + "&type=" + type
            val body = HttpUtils.get(url) ?: return emptyList()
            return try {
                val root = JSONObject(body)
                if (root.optInt("code") != 200) return emptyList()
                val arr = root.optJSONObject("data")?.optJSONArray("items") ?: return emptyList()
                val list = mutableListOf<MediaItem>()
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    list.add(
                        MediaItem(
                            o.optString("url"),
                            o.optInt("id"),
                            o.optString("uin"),
                            o.optString("create_time")
                        )
                    )
                }
                list.filter { it.url.isNotEmpty() }
            } catch (e: Throwable) {
                LogUtils.e("MediaApi", "fetchCollectionItems parse failed: " + e.message)
                emptyList()
            }
        }

        /**
         * 上传一批本地图片到服务器。
         * 先通过 ExtraTool.uploadImage 将每张本地图上传到腾讯图床拿链接，
         * 再统一 POST 到 upload.php（collection 为合集名，tag/url 均为集合）。
         *
         * @return UploadResult(success=成功入库数, failed=失败数)。
         *         失败数 = 图床拿不到链接 + 服务器拒绝(域名校验) + 入库异常。
         */
        fun uploadImages(
            collection: String,
            paths: List<String>,
            tags: List<String>,
            type: String,
            uin: String
        ): UploadResult {
            val total = paths.size
            if (collection.isEmpty() || total == 0) return UploadResult(0, total)

            val urls = mutableListOf<String>()
            for (p in paths) {
                val s = ExtraTool.uploadImage(p)
                if (s.isNotEmpty()) urls.add(s)
            }
            if (urls.isEmpty()) {
                LogUtils.e("MediaApi", "uploadImages: 全部图片均未能获取到链接")
                return UploadResult(0, total)
            }

            return try {
                val payload = JSONObject()
                payload.put("uin", uin)
                payload.put("type", type)
                payload.put("collection", collection)
                payload.put("tag", JSONArray(tags))
                payload.put("url", JSONArray(urls))
                val body = HttpUtils.post(API_BASE + "upload.php", payload.toString(), "application/json")
                    ?: return UploadResult(0, total)
                val root = JSONObject(body)
                if (root.optInt("code") != 200) {
                    LogUtils.e("MediaApi", "uploadImages failed: " + root.optString("message"))
                    return UploadResult(0, total)
                }
                val data = root.optJSONObject("data")
                val inserted = data?.optInt("inserted") ?: urls.size
                val success = inserted.coerceAtMost(total)
                UploadResult(success, (total - success).coerceAtLeast(0))
            } catch (e: Throwable) {
                LogUtils.e("MediaApi", "uploadImages failed: " + e.message)
                UploadResult(0, total)
            }
        }
    }

    /**
     * 网络图片缓存：下载到本地缓存目录并返回路径，供缩略图/详情复用。
     */
    object MediaImageCache {

        fun dir(): String {
            val d = QQCurrentEnv.getCurrentDir() + "media/cache/"
            File(d).let { if (!it.exists()) it.mkdirs() }
            return d
        }

        fun galleryDir(): String {
            // 与 DownloadEmotion 图片保存目录保持一致：/storage/emulated/0/Download/QQ/QEdge/Pictures/
            val d = QQCurrentEnv.getLocalPath() + "Download/QQ/QEdge/Pictures/"
            File(d).let { if (!it.exists()) it.mkdirs() }
            return d
        }

        private fun md5(s: String): String {
            return try {
                val digest = MessageDigest.getInstance("MD5")
                val bytes = digest.digest(s.toByteArray(Charsets.UTF_8))
                val sb = StringBuilder()
                for (b in bytes) sb.append(String.format("%02x", b))
                sb.toString()
            } catch (e: Throwable) {
                s.hashCode().toString()
            }
        }

        /** 下载/读取网络图到缓存，成功返回本地路径，失败返回 null */
        fun load(url: String): String? {
            if (url.isEmpty()) return null
            val f = File(dir() + md5(url) + ".img")
            if (f.exists() && f.length() > 0) return f.absolutePath
            val ok = HttpUtils.downloadSync(url, f.absolutePath)
            return if (ok && f.exists() && f.length() > 0) f.absolutePath else null
        }

        /** 保存网络图到本地图库（下载按钮），返回保存后的路径 */
        fun saveToGallery(url: String, context: Context): String? {
            val path = load(url) ?: return null
            return try {
                val src = File(path)
                val ext = guessExt(url)
                val name = "img_" + System.currentTimeMillis() + ext
                val dst = File(galleryDir(), name)
                src.copyTo(dst, overwrite = true)
                dst.absolutePath
            } catch (e: Throwable) {
                LogUtils.e("MediaImageCache", "saveToGallery failed: " + e.message)
                null
            }
        }

        /**
         * 删除本地图库图片（健壮处理）：
         * 文件已不存在视为成功；直接 delete 失败时回退到 MediaStore 按路径删除，
         * 覆盖部分机型 Scoped Storage 下 File.delete() 失效的情况。
         */
        fun deleteFromGallery(path: String, context: Context): Boolean {
            val f = File(path)
            if (!f.exists()) {
                LogUtils.e("MediaImageCache", "deleteFromGallery: file not exists, treat as deleted: $path")
                return true
            }
            if (f.delete()) return true

            LogUtils.e("MediaImageCache", "deleteFromGallery: File.delete() false, try MediaStore: $path")
            return try {
                val resolver = context.contentResolver
                val collection = android.provider.MediaStore.Images.Media
                    .getContentUri(android.provider.MediaStore.VOLUME_EXTERNAL)
                val proj = arrayOf(android.provider.MediaStore.Images.Media._ID)
                val sel = android.provider.MediaStore.Images.Media.DATA + "=?"
                var ok = false
                resolver.query(collection, proj, sel, arrayOf(path), null)?.use { c ->
                    if (c.moveToFirst()) {
                        val id = c.getLong(0)
                        val itemUri = android.content.ContentUris.withAppendedId(collection, id)
                        ok = resolver.delete(itemUri, null, null) > 0
                    }
                }
                if (!ok) LogUtils.e("MediaImageCache", "deleteFromGallery: MediaStore delete also failed: $path")
                ok
            } catch (e: Throwable) {
                LogUtils.e("MediaImageCache", "deleteFromGallery MediaStore fallback error: " + e.message)
                false
            }
        }

        private fun guessExt(url: String): String {
            val clean = url.substringBefore('?')
            val name = clean.substringAfterLast('/', "")
            val idx = name.lastIndexOf('.')
            if (idx > 0 && idx < name.length - 1) {
                val ext = name.substring(idx).lowercase()
                if (ext in listOf(".png", ".jpg", ".jpeg", ".gif", ".webp", ".bmp")) return ext
            }
            return ".jpg"
        }
    }

    /**
     * 语音/视频本地保存目录（与图片的 Pictures 目录并列，无需封面，仅本地文件列表）。
     */
    object MediaFileCache {
        fun dirFor(type: String): String {
            // 与 DownloadEmotion 保存目录保持一致：语音 Ptts、视频 Videos
            val sub = if (type == "voice") "Ptts" else "Videos"
            val d = QQCurrentEnv.getLocalPath() + "Download/QQ/QEdge/" + sub + "/"
            File(d).let { if (!it.exists()) it.mkdirs() }
            return d
        }

        fun listFiles(type: String): List<File> {
            return try {
                val dir = File(dirFor(type))
                (dir.listFiles() ?: emptyArray())
                    .filter { it.isFile }
                    .sortedByDescending { it.lastModified() }
            } catch (e: Throwable) {
                emptyList()
            }
        }
    }
}