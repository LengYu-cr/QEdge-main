package me.lengyu.qedge.hook.item

import android.media.MediaScannerConnection
import android.os.Environment
import me.lengyu.qedge.hook.annotation.HookItemAnnotation
import me.lengyu.qedge.hook.api.OnMenuBuild
import me.lengyu.qedge.hook.base.BaseApiHookItem
import me.lengyu.qedge.hook.base.Listener
import me.lengyu.qedge.plugin.bean.MsgData
import me.lengyu.qedge.utils.HttpUtils
import me.lengyu.qedge.utils.LogUtils
import me.lengyu.qedge.utils.ModuleConfig
import me.lengyu.qedge.utils.QQCurrentEnv
import me.lengyu.qedge.utils.Toasts
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Locale
import kotlin.concurrent.thread

@HookItemAnnotation(value = "图片视频语音下载", category = "item")
object DownloadEmotion : BaseApiHookItem<DownloadEmotion.DownloadEmotionListener>() {

    private const val TAG = "DownloadEmotion"
    private const val MENU_KEY = "[QEdge],DownloadEmotion,保存,,2,7,32,6"

    private fun isEnabled(): Boolean {
        return ModuleConfig.getBoolean("download_emotion", false)
    }

    override fun loadHook() {
        try {
            OnMenuBuild.addMenuListener(
                menuKey = MENU_KEY,
                enabled = { isEnabled() }
            ) { msgData, _ ->
                val hasPic = msgData.picList.isNotEmpty()
                val hasVideo = msgData.videoList.isNotEmpty()
                val hasPtt = msgData.pttList.isNotEmpty()
                if (!hasPic && !hasVideo && !hasPtt) return@addMenuListener
                downloadMedia(msgData)
            }
            // LogUtils.d(TAG, "loadHook success")
        } catch (e: Throwable) {
            LogUtils.e(TAG, "loadHook error: ${e.message}")
        }
    }

    private fun downloadMedia(msgData: MsgData) {
        val picList = msgData.picList
        val videoList = msgData.videoList
        val pttList = msgData.pttList

        thread {
            try {
                // 统一存 /storage/emulated/0/Download/QEdge/{Pictures,Videos,PTT}/ (QQ 对 Tencent 目录有完整权限, 绕过 Scoped Storage)
                val rootDir = File(QQCurrentEnv.getLocalPath() + "Download/QQ/QEdge")
                val picSaveDir = File(rootDir, "Pictures").apply { if (!exists()) mkdirs() }
                val videoSaveDir = File(rootDir, "Videos").apply { if (!exists()) mkdirs() }
                val pttSaveDir = File(rootDir, "Ptts").apply { if (!exists()) mkdirs() }

                val downloadedFiles = mutableListOf<File>()

                for ((index, picUrl) in picList.withIndex()) {
                    if (picUrl.isEmpty()) continue
                    runCatching {
                        val ext = getImageExt(picUrl)
                        val fileName = "pic_${System.currentTimeMillis()}_${index}$ext"
                        val file = File(picSaveDir, fileName)
                        if (HttpUtils.downloadSync(picUrl, file.absolutePath)) {
                            downloadedFiles.add(file)
                        }
                    }
                }

                for ((index, videoPath) in videoList.withIndex()) {
                    if (videoPath.isEmpty()) continue
                    runCatching {
                        val src = File(videoPath)
                        if (!src.exists() || !src.isFile){
                            Toasts.toast("视频文件不存在,请先预览视频")
                            return@runCatching
                        }
                        val ext = getVideoExt(videoPath)
                        val fileName = "video_${System.currentTimeMillis()}_${index}$ext"
                        val dst = File(videoSaveDir, fileName)
                        if (copyFile(src, dst)) {
                            downloadedFiles.add(dst)
                        }
                    }
                }

                for ((index, pttPath) in pttList.withIndex()) {
                    if (pttPath.isEmpty()) continue
                    runCatching {
                        val src = File(pttPath)
                        if (!src.exists() || !src.isFile) {
                            Toasts.toast("语音文件不存在,请先预览语音")
                            return@runCatching
                        }
                        val ext = getPttsExt(pttPath)
                        val fileName = "ptt_${System.currentTimeMillis()}_${index}$ext"
                        val dst = File(pttSaveDir, fileName)
                        if (copyFile(src, dst)) {
                            downloadedFiles.add(dst)
                        }
                    }
                }

                if (downloadedFiles.isNotEmpty()) {
                    val activity = QQCurrentEnv.getActivity()
                    if (activity != null) {
                        val paths = downloadedFiles.map { it.absolutePath }.toTypedArray()
                        MediaScannerConnection.scanFile(activity, paths, null) { _, _ -> }
                    }
                    val sb = StringBuilder("已保存到/Tencent/QEdge/")
                    val types = mutableListOf<String>()
                    if (pttList.isNotEmpty()) types.add("PTT(语音)")
                    if (videoList.isNotEmpty()) types.add("Videos(视频)")
                    if (picList.isNotEmpty()) types.add("Pictures(图片)")
                    if (types.isNotEmpty()) sb.append(types.joinToString("+"))
                    Toasts.toast(sb.toString())
                } else {
                    Toasts.toast("下载失败")
                }
            } catch (e: Throwable) {
                Toasts.toast("下载失败: ${e.message}")
            }
        }
    }

    private fun copyFile(src: File, dst: File): Boolean {
        return runCatching {
            FileInputStream(src).use { ins ->
                FileOutputStream(dst).use { out ->
                    val buf = ByteArray(8192)
                    var len: Int
                    while (true) {
                        len = ins.read(buf)
                        if (len <= 0) break
                        out.write(buf, 0, len)
                    }
                    out.flush()
                }
            }
            dst.exists() && dst.length() == src.length()
        }.getOrDefault(false)
    }

    private fun getImageExt(url: String): String {
        val lowerUrl = url.lowercase(Locale.getDefault())
        return when {
            lowerUrl.contains(".gif") -> ".gif"
            lowerUrl.contains(".png") -> ".png"
            lowerUrl.contains(".webp") -> ".webp"
            lowerUrl.contains(".jpg") || lowerUrl.contains(".jpeg") -> ".jpg"
            else -> ".jpg"
        }
    }

    private fun getVideoExt(path: String): String {
        val lower = path.lowercase(Locale.getDefault())
        return when {
            lower.endsWith(".mp4") -> ".mp4"
            lower.endsWith(".mov") -> ".mov"
            lower.endsWith(".avi") -> ".avi"
            lower.endsWith(".mkv") -> ".mkv"
            lower.endsWith(".3gp") -> ".3gp"
            lower.endsWith(".flv") -> ".flv"
            lower.endsWith(".webm") -> ".webm"
            lower.endsWith(".m4v") -> ".m4v"
            else -> ".mp4"
        }
    }

    private fun getPttsExt(path: String): String {
        val lower = path.lowercase(Locale.getDefault())
        return when {
            lower.endsWith(".amr") -> ".amr"
            lower.endsWith(".m4a") -> ".m4a"
            lower.endsWith(".wav") -> ".wav"
            else -> ".amr"
        }
    }

    interface DownloadEmotionListener : Listener
}
