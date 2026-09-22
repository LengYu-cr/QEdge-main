package me.lengyu.qedge.ui.services

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.io.FileReader
import java.io.BufferedReader
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import me.lengyu.qedge.utils.LogUtils
import me.lengyu.qedge.utils.ZipUtil
import me.lengyu.qedge.utils.qq.QQCurrentEnv

object OnlinePluginService {

    private const val BASE_URL = "https://v.yuafeng.cn/QEdge"
    private const val PLUGIN_LIST_URL = "$BASE_URL/online_plugin/list.php"
    private const val PLUGIN_DOWNLOAD_URL = "$BASE_URL/online_plugin/download.php"
    private const val PLUGIN_UPLOAD_URL = "$BASE_URL/online_plugin/index.php"

    fun fetchOnlinePlugins(search: String = "", callback: (List<OnlinePlugin>?, String?) -> Unit) {
        Thread {
            try {
                val urlString = if (search.isNotEmpty()) {
                    "$PLUGIN_LIST_URL?api=json&search=${URLEncoder.encode(search, "UTF-8")}"
                } else {
                    "$PLUGIN_LIST_URL?api=json"
                }

                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                connection.requestMethod = "GET"

                val reader = BufferedReader(InputStreamReader(connection.inputStream, "UTF-8"))
                val response = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }
                reader.close()

                val json = JSONObject(response.toString())
                if (json.getInt("code") == 200) {
                    val data = json.getJSONArray("data")
                    val plugins = mutableListOf<OnlinePlugin>()
                    for (i in 0 until data.length()) {
                        val item = data.getJSONObject(i)
                        plugins.add(
                            OnlinePlugin(
                                id = item.getInt("could_id"),
                                pluginId = item.getString("plugin_id"),
                                pluginName = item.getString("plugin_name"),
                                versionCode = item.getString("version_code"),
                                authorName = item.getString("author_name"),
                                uploadQq = item.getString("upload_qq"),
                                downloadCount = item.getInt("download_count"),
                                uploadTime = item.getString("upload_time")
                            )
                        )
                    }
                    callback(plugins, null)
                } else {
                    callback(null, json.getString("message"))
                }
            } catch (e: Exception) {
                callback(null, e.message)
            }
        }.start()
    }

    fun downloadPlugin(
        pluginId: String,
        pluginName: String,
        callback: (filePath: String?, error: String?, zipPath: String?, targetPath: String?) -> Unit
    ) {
        Thread {
            var tempFile: File? = null
            var targetDir: File? = null
            try {
                val url = URL("$PLUGIN_DOWNLOAD_URL?id=$pluginId")
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 30000
                connection.readTimeout = 30000
                connection.requestMethod = "GET"

                val inputStream = connection.inputStream
                val tempDirLocal = File(QQCurrentEnv.getCurrentDir(), "temp")
                if (!tempDirLocal.exists()) {
                    tempDirLocal.mkdirs()
                }
                val downloadedFile = File(tempDirLocal, "plugin_download_${pluginId}.zip")
                tempFile = downloadedFile
                if (downloadedFile.exists()) {
                    downloadedFile.delete()
                }

                downloadedFile.outputStream().use { outputStream ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                    }
                }
                inputStream.close()

                val pluginBaseDir = File(QQCurrentEnv.getCurrentDir(), "plugin")
                val extractTargetDir = pluginBaseDir.resolve(pluginName)
                targetDir = extractTargetDir
                val pluginDir = extractPluginZip(downloadedFile, extractTargetDir)
                downloadedFile.delete()
                callback(pluginDir.absolutePath, null, null, null)
            } catch (e: Exception) {
                LogUtils.e(e)
                callback(null, "解压失败: ${e.message}", tempFile?.absolutePath, targetDir?.absolutePath)
            }
        }.start()
    }

    private fun extractPluginZip(zipFile: File, targetDir: File): File {
        if (targetDir.exists() && !deleteDir(targetDir)) {
            throw IOException("无法删除已存在的目录: ${targetDir.absolutePath}")
        }
        if (!targetDir.mkdirs()) {
            throw IOException("无法创建目标目录: ${targetDir.absolutePath}")
        }

        // 直接解压到目标目录，不做根目录判断、不用临时目录，保证 info.prop 等文件不丢失
        ZipUtil.unzip(zipFile.absolutePath, targetDir.absolutePath)

        // 目标目录需真实存在
        if (!targetDir.isDirectory) {
            throw IOException("解压失败: 目标目录未生成 ${targetDir.absolutePath}")
        }

        // 校验解压结果：空目录说明 ZIP 无效，或下载到了错误内容（如服务器返回的 HTML/JSON 页面）
        val extractedFiles = targetDir.listFiles()
        if (extractedFiles == null || extractedFiles.isEmpty()) {
            throw IOException("解压失败: ZIP 为空或不是有效压缩包（可能下载到错误页面）")
        }
        return targetDir
    }

    private fun deleteDir(dir: File): Boolean {
        if (dir.isDirectory) {
            dir.listFiles()?.forEach { deleteDir(it) }
        }
        return dir.delete()
    }

    private fun readFileContent(file: File): String {
        return try {
            file.readText(StandardCharsets.UTF_8)
        } catch (e: IOException) {
            LogUtils.e(e)
            ""
        }
    }

    private fun readPluginDescription(pluginDir: File): String {
        val descriptionFile = pluginDir.resolve("desc.txt")
        return if (descriptionFile.exists()) {
            readFileContent(descriptionFile)
        } else {
            ""
        }
    }

    fun uploadPlugin(
        pluginId: String,
        pluginName: String,
        versionCode: String,
        authorName: String,
        pluginDir: File,
        callback: (Map<String, Any>?, String?) -> Unit
    ) {
        Thread {
            var zipFile: File? = null
            try {
                val tempDir = File(QQCurrentEnv.getCurrentDir(), "temp").apply {
                    if (!exists()) mkdirs()
                }
                zipFile = File(tempDir, "plugin_upload_${System.currentTimeMillis()}.zip")
                zipDirectory(pluginDir, zipFile)

                val boundary = "----WebKitFormBoundary${System.currentTimeMillis()}"
                val url = URL(PLUGIN_UPLOAD_URL)
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 30000
                connection.readTimeout = 30000
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary; charset=UTF-8")
                connection.doOutput = true

                val outputStream = DataOutputStream(BufferedOutputStream(connection.outputStream))

                writeFormField(outputStream, boundary, "qq", QQCurrentEnv.getCurrentUin() ?: "")

                val safeFileName = URLEncoder.encode(zipFile.name, "UTF-8")
                    .replace("+", "%20")
                outputStream.writeUtf8("--$boundary\r\n")
                outputStream.writeUtf8(
                    "Content-Disposition: form-data; name=\"pluginFile\"; filename=\"${zipFile.name}\"; filename*=UTF-8''$safeFileName\r\n"
                )
                outputStream.writeUtf8("Content-Type: application/zip\r\n")
                outputStream.writeUtf8("Content-Transfer-Encoding: binary\r\n")
                outputStream.writeUtf8("\r\n")

                FileInputStream(zipFile).use { fis ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (fis.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                    }
                }

                outputStream.writeUtf8("\r\n")
                outputStream.writeUtf8("--$boundary--\r\n")
                outputStream.flush()
                outputStream.close()

                val reader = BufferedReader(InputStreamReader(connection.inputStream, "UTF-8"))
                val response = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }
                reader.close()

                zipFile.delete()
                zipFile = null

                val json = JSONObject(response.toString())
                if (json.getInt("code") == 200) {
                    val data = mutableMapOf<String, Any>()
                    data["message"] = json.getString("message")
                    if (json.has("data")) {
                        val dataObj = json.getJSONObject("data")
                        val keys = dataObj.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            data[key] = dataObj.get(key)
                        }
                    }
                    callback(data, null)
                } else {
                    callback(null, json.getString("message"))
                }
            } catch (e: Exception) {
                zipFile?.delete()
                callback(null, e.message)
            }
        }.start()
    }

    private fun DataOutputStream.writeUtf8(str: String) {
        write(str.toByteArray(StandardCharsets.UTF_8))
    }

    private fun writeFormField(output: DataOutputStream, boundary: String, name: String, value: String) {
        output.writeUtf8("--$boundary\r\n")
        output.writeUtf8("Content-Disposition: form-data; name=\"$name\"\r\n")
        output.writeUtf8("Content-Type: text/plain; charset=UTF-8\r\n")
        output.writeUtf8("Content-Transfer-Encoding: 8bit\r\n")
        output.writeUtf8("\r\n")
        output.writeUtf8(value)
        output.writeUtf8("\r\n")
    }

    private fun zipDirectory(sourceDir: File, outputFile: File) {
        ZipOutputStream(BufferedOutputStream(FileOutputStream(outputFile)), StandardCharsets.UTF_8).use { zos ->
            zos.setLevel(java.util.zip.Deflater.BEST_SPEED)
            sourceDir.walk().forEach { file ->
                val relativePath = sourceDir.toPath().relativize(file.toPath()).toString()
                if (file.isDirectory) {
                    if (relativePath.isNotEmpty()) {
                        zos.putNextEntry(ZipEntry("$relativePath/"))
                        zos.closeEntry()
                    }
                } else {
                    zos.putNextEntry(ZipEntry(relativePath))
                    file.inputStream().use { it.copyTo(zos, 8192) }
                    zos.closeEntry()
                }
            }
            zos.finish()
        }
    }

    data class OnlinePlugin(
        val pluginId: String,
        val pluginName: String,
        val versionCode: String,
        val authorName: String,
        val uploadQq: String,
        val downloadCount: Int,
        val uploadTime: String,
        val id: Int
    )
}