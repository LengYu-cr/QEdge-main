package me.lengyu.qedge.ui.pages.file

import android.content.Context
import android.content.SharedPreferences
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import me.lengyu.qedge.utils.HostInfo
import me.lengyu.qedge.utils.QQCurrentEnv

object FileManagerUtils {

    private const val PREFS_NAME = "file_manager_prefs"
    private const val KEY_HOME_PATH = "home_path"
    private val DEFAULT_QQ_PATH = QQCurrentEnv.getLocalPath() + "Android/data/" + HostInfo.packageName + "/"

    fun getHomePath(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_HOME_PATH, DEFAULT_QQ_PATH) ?: DEFAULT_QQ_PATH
    }

    fun setHomePath(context: Context, path: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_HOME_PATH, path).apply()
    }

    fun getDefaultQQPath(): String = DEFAULT_QQ_PATH

    fun formatFileSize(size: Long): String {
        return when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> "${size / 1024} KB"
            size < 1024 * 1024 * 1024 -> String.format("%.1f MB", size / (1024.0 * 1024.0))
            else -> String.format("%.2f GB", size / (1024.0 * 1024.0 * 1024.0))
        }
    }

    fun getFileExtension(name: String): String {
        val dotIndex = name.lastIndexOf('.')
        return if (dotIndex >= 0 && dotIndex < name.length - 1) {
            name.substring(dotIndex + 1).lowercase()
        } else {
            ""
        }
    }

    fun isTextFile(extension: String): Boolean {
        val textExtensions = setOf(
            "txt", "log", "xml", "json", "md", "java", "kt", "py", "js",
            "html", "css", "php", "c", "cpp", "h", "hpp", "sh", "bat",
            "ini", "conf", "cfg", "yml", "yaml", "toml", "properties"
        )
        return extension in textExtensions
    }

    fun isImageFile(extension: String): Boolean {
        val imageExtensions = setOf("jpg", "jpeg", "png", "gif", "bmp", "webp", "svg", "ico")
        return extension in imageExtensions
    }

    fun isAudioFile(extension: String): Boolean {
        val audioExtensions = setOf("mp3", "wav", "ogg", "m4a", "flac", "aac", "wma", "amr")
        return extension in audioExtensions
    }

    fun isVideoFile(extension: String): Boolean {
        val videoExtensions = setOf("mp4", "avi", "mkv", "mov", "3gp", "wmv", "flv", "webm")
        return extension in videoExtensions
    }

    fun isArchiveFile(extension: String): Boolean {
        val archiveExtensions = setOf("zip", "rar", "7z", "tar", "gz", "bz2", "xz")
        return extension in archiveExtensions
    }

    fun copyFile(source: File, target: File): Boolean {
        return try {
            if (source.isDirectory) {
                copyDirectory(source, target)
            } else {
                source.copyTo(target, overwrite = true)
                true
            }
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }

    fun copyDirectory(source: File, target: File): Boolean {
        return try {
            if (!target.exists()) {
                target.mkdirs()
            }
            source.listFiles()?.forEach { file ->
                val targetFile = File(target, file.name)
                if (file.isDirectory) {
                    copyDirectory(file, targetFile)
                } else {
                    file.copyTo(targetFile, overwrite = true)
                }
            }
            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }

    fun moveFile(source: File, target: File): Boolean {
        return try {
            if (source.isDirectory) {
                if (copyDirectory(source, target)) {
                    deleteDirectory(source)
                    true
                } else {
                    false
                }
            } else {
                val result = source.renameTo(target)
                if (!result) {
                    source.copyTo(target, overwrite = true)
                    source.delete()
                }
                true
            }
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }

    fun deleteFile(file: File): Boolean {
        return try {
            if (file.isDirectory) {
                deleteDirectory(file)
            } else {
                file.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun deleteDirectory(dir: File): Boolean {
        return try {
            dir.listFiles()?.forEach { file ->
                if (file.isDirectory) {
                    deleteDirectory(file)
                } else {
                    file.delete()
                }
            }
            dir.delete()
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun renameFile(file: File, newName: String): Boolean {
        return try {
            val parent = file.parentFile
            if (parent != null) {
                val newFile = File(parent, newName)
                file.renameTo(newFile)
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun createDirectory(parentPath: String, name: String): Boolean {
        return try {
            val dir = File(parentPath, name)
            if (!dir.exists()) {
                dir.mkdirs()
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun createFile(parentPath: String, name: String): Boolean {
        return try {
            val file = File(parentPath, name)
            if (!file.exists()) {
                file.createNewFile()
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun unzip(zipFile: File, targetDir: File, onProgress: ((Int, Int) -> Unit)? = null): Boolean {
        return try {
            if (!targetDir.exists()) {
                targetDir.mkdirs()
            }

            val zipInputStream = ZipInputStream(FileInputStream(zipFile))
            val entries = mutableListOf<ZipEntry>()
            var entry: ZipEntry?

            while (zipInputStream.nextEntry.also { entry = it } != null) {
                entry?.let { entries.add(it) }
            }
            zipInputStream.close()

            val totalEntries = entries.size
            var currentEntry = 0

            val newZipInputStream = ZipInputStream(FileInputStream(zipFile))
            var newEntry: ZipEntry?

            while (newZipInputStream.nextEntry.also { newEntry = it } != null) {
                newEntry?.let { zipEntry ->
                    val newFile = File(targetDir, zipEntry.name)

                    if (zipEntry.isDirectory) {
                        newFile.mkdirs()
                    } else {
                        newFile.parentFile?.mkdirs()
                        val outputStream = FileOutputStream(newFile)
                        val buffer = ByteArray(4096)
                        var len: Int

                        while (newZipInputStream.read(buffer).also { len = it } > 0) {
                            outputStream.write(buffer, 0, len)
                        }

                        outputStream.close()
                    }

                    currentEntry++
                    onProgress?.invoke(currentEntry, totalEntries)
                }
            }

            newZipInputStream.close()
            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }

    fun readTextFile(file: File): String {
        return try {
            file.readText(Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    fun writeTextFile(file: File, content: String): Boolean {
        return try {
            file.writeText(content, Charsets.UTF_8)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun getDirectorySize(dir: File): Long {
        var size = 0L
        dir.listFiles()?.forEach { file ->
            size += if (file.isDirectory) {
                getDirectorySize(file)
            } else {
                file.length()
            }
        }
        return size
    }

    fun listFilesSorted(dir: File): List<File> {
        return dir.listFiles()?.toList()
            ?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
            ?: emptyList()
    }

    data class ZipItem(
        val name: String,
        val path: String,
        val isDirectory: Boolean,
        val size: Long,
        val compressedSize: Long
    )

    fun listZipEntries(zipFile: File, zipPath: String = ""): List<ZipItem> {
        return try {
            val zipInputStream = ZipInputStream(FileInputStream(zipFile))
            val entries = mutableListOf<ZipEntry>()
            var entry: ZipEntry?

            while (zipInputStream.nextEntry.also { entry = it } != null) {
                entry?.let { entries.add(it) }
            }
            zipInputStream.close()

            val normalizedPath = if (zipPath.isEmpty()) "" else {
                if (zipPath.endsWith("/")) zipPath else "$zipPath/"
            }

            val result = mutableListOf<ZipItem>()
            val addedNames = mutableSetOf<String>()

            for (zipEntry in entries) {
                val entryName = zipEntry.name
                if (normalizedPath.isNotEmpty() && !entryName.startsWith(normalizedPath)) continue

                val relativePath = entryName.removePrefix(normalizedPath)
                if (relativePath.isEmpty()) continue

                val slashIndex = relativePath.indexOf('/')
                val isDirectChild = slashIndex < 0 || slashIndex == relativePath.length - 1
                val itemName = if (slashIndex >= 0) relativePath.substring(0, slashIndex) else relativePath

                if (isDirectChild && itemName.isNotEmpty() && !addedNames.contains(itemName)) {
                    addedNames.add(itemName)
                    val isDir = zipEntry.isDirectory || slashIndex >= 0
                    result.add(
                        ZipItem(
                            name = itemName,
                            path = normalizedPath + itemName + if (isDir) "/" else "",
                            isDirectory = isDir,
                            size = zipEntry.size,
                            compressedSize = zipEntry.compressedSize
                        )
                    )
                }
            }

            result.sortWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
            result
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun extractZipEntry(zipFile: File, entryPath: String, targetFile: File): Boolean {
        return try {
            val zipInputStream = ZipInputStream(FileInputStream(zipFile))
            var entry: ZipEntry?
            var found = false

            while (zipInputStream.nextEntry.also { entry = it } != null) {
                if (entry?.name == entryPath) {
                    found = true
                    targetFile.parentFile?.mkdirs()
                    val outputStream = FileOutputStream(targetFile)
                    val buffer = ByteArray(4096)
                    var len: Int
                    while (zipInputStream.read(buffer).also { len = it } > 0) {
                        outputStream.write(buffer, 0, len)
                    }
                    outputStream.close()
                    break
                }
            }

            zipInputStream.close()
            found
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun isApkFile(extension: String): Boolean {
        return extension == "apk"
    }
}
