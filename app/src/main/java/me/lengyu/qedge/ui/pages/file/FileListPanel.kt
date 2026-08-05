package me.lengyu.qedge.ui.pages.file

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ripple
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.lengyu.qedge.R
import me.lengyu.qedge.ui.components.atoms.QEdgeCard
import me.lengyu.qedge.ui.components.molecules.EmptyStateView
import me.lengyu.qedge.ui.core.theme.QEdgeTheme
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FileListPanel(
    path: String,
    refreshKey: Int = 0,
    zipFile: File? = null,
    zipInnerPath: String = "",
    onPathChange: (String) -> Unit,
    onZipPathChange: (String) -> Unit = {},
    onZipFileClick: (File, String) -> Unit = { _, _ -> },
    selectedFiles: Set<File>,
    onSelectedFilesChange: (Set<File>) -> Unit,
    isActive: Boolean,
    onActive: () -> Unit,
    onFileClick: (File) -> Unit,
    onFileLongClick: (File) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = QEdgeTheme.colors
    var fileList by remember { mutableStateOf<List<FileItem>>(emptyList()) }
    var zipItemList by remember { mutableStateOf<List<FileManagerUtils.ZipItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val listState = rememberLazyListState()
    val isZipMode = zipFile != null

    LaunchedEffect(path, refreshKey) {
        if (!isZipMode) {
            isLoading = true
            loadFiles(path) { files ->
                fileList = files
                isLoading = false
            }
        }
    }

    LaunchedEffect(zipFile, zipInnerPath, refreshKey) {
        if (isZipMode) {
            isLoading = true
            zipItemList = FileManagerUtils.listZipEntries(zipFile, zipInnerPath)
            isLoading = false
        }
    }

    QEdgeCard(
        modifier = modifier
            .fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
                .background(if (isActive) colors.accentBlue.copy(alpha = 0.05f) else androidx.compose.ui.graphics.Color.Transparent)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onActive
                )
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
            ) {
                if (isLoading) {
                    EmptyStateView(message = "加载中...")
                } else if (isZipMode) {
                    val hasParent = zipInnerPath.isNotEmpty()
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        item("..") {
                            FileListItem(
                                fileItem = FileItem(
                                    name = "..",
                                    path = "",
                                    isDirectory = true,
                                    size = "",
                                    modifiedTime = "",
                                    extension = ""
                                ),
                                isSelected = false,
                                isParent = true,
                                onClick = {
                                    onActive()
                                    if (hasParent) {
                                        val lastSlash = zipInnerPath.dropLast(1).lastIndexOf('/')
                                        val parentPath = if (lastSlash >= 0) {
                                            zipInnerPath.substring(0, lastSlash + 1)
                                        } else {
                                            ""
                                        }
                                        onZipPathChange(parentPath)
                                    } else {
                                        onPathChange(path)
                                    }
                                },
                                onLongClick = { }
                            )
                        }
                        if (zipItemList.isEmpty()) {
                            item("empty") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 48.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "空文件夹",
                                        fontSize = 14.sp,
                                        color = colors.textSecondary
                                    )
                                }
                            }
                        } else {
                            items(zipItemList, key = { it.path }) { zipItem ->
                                val item = FileItem(
                                    name = zipItem.name,
                                    path = zipItem.path,
                                    isDirectory = zipItem.isDirectory,
                                    size = FileManagerUtils.formatFileSize(zipItem.size),
                                    modifiedTime = "",
                                    extension = FileManagerUtils.getFileExtension(zipItem.name)
                                )
                                FileListItem(
                                    fileItem = item,
                                    isSelected = false,
                                    isParent = false,
                                    onClick = {
                                        onActive()
                                        if (zipItem.isDirectory) {
                                            onZipPathChange(zipItem.path)
                                        } else {
                                            onZipFileClick(zipFile, zipItem.path)
                                        }
                                    },
                                    onLongClick = { }
                                )
                            }
                        }
                    }
                } else {
                    val parentFile = File(path).parentFile
                    val hasParent = parentFile != null && path != "/" && path != "/storage/emulated/0"
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (hasParent) {
                            item("..") {
                                FileListItem(
                                    fileItem = FileItem(
                                        name = "..",
                                        path = parentFile.absolutePath,
                                        isDirectory = true,
                                        size = "",
                                        modifiedTime = "",
                                        extension = ""
                                    ),
                                    isSelected = false,
                                    isParent = true,
                                    onClick = {
                                        onActive()
                                        onPathChange(parentFile.absolutePath)
                                        onSelectedFilesChange(emptySet())
                                    },
                                    onLongClick = { }
                                )
                            }
                        }
                        if (fileList.isEmpty()) {
                            item("empty") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 48.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "空文件夹",
                                        fontSize = 14.sp,
                                        color = colors.textSecondary
                                    )
                                }
                            }
                        } else {
                            items(fileList, key = { it.path }) { fileItem ->
                                FileListItem(
                                    fileItem = fileItem,
                                    isSelected = selectedFiles.contains(File(fileItem.path)),
                                    isParent = false,
                                    onClick = {
                                        onActive()
                                        if (selectedFiles.isNotEmpty()) {
                                            toggleSelection(fileItem, selectedFiles, onSelectedFilesChange)
                                        } else {
                                            if (fileItem.isDirectory) {
                                                onPathChange(fileItem.path)
                                                onSelectedFilesChange(emptySet())
                                            } else {
                                                onFileClick(File(fileItem.path))
                                            }
                                        }
                                    },
                                    onLongClick = {
                                        onActive()
                                        onFileLongClick(File(fileItem.path))
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileListItem(
    fileItem: FileItem,
    isSelected: Boolean,
    isParent: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val colors = QEdgeTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) colors.accentBlue.copy(alpha = 0.15f) else androidx.compose.ui.graphics.Color.Transparent)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = colors.ripple),
                onClick = onClick,
                onLongClick = { if (!isParent) onLongClick() }
            )
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    if (isParent) colors.accentBlue.copy(alpha = 0.5f)
                    else colors.accentBlue
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(getFileIcon(fileItem.isDirectory, fileItem.extension)),
                contentDescription = fileItem.name,
                modifier = Modifier.size(22.dp),
                tint = androidx.compose.ui.graphics.Color.White
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = fileItem.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = if (isParent) colors.textSecondary else colors.textPrimary,
                maxLines = 1
            )
            if (!isParent) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${fileItem.size} • ${fileItem.modifiedTime}",
                    fontSize = 11.sp,
                    color = colors.textSecondary,
                    maxLines = 1
                )
            }
        }
    }
}

data class FileItem(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: String,
    val modifiedTime: String,
    val extension: String
)

private fun loadFiles(path: String, callback: (List<FileItem>) -> Unit) {
    Thread {
        try {
            val dir = File(path)
            if (!dir.exists() || !dir.isDirectory) {
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    callback(emptyList())
                }
                return@Thread
            }

            val files = dir.listFiles()?.toList() ?: emptyList()
            val fileItems = files
                .sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
                .map { file ->
                    FileItem(
                        name = file.name,
                        path = file.absolutePath,
                        isDirectory = file.isDirectory,
                        size = if (file.isDirectory) {
                            val count = file.list()?.size ?: 0
                            "$count 项"
                        } else {
                            formatFileSize(file.length())
                        },
                        modifiedTime = formatDate(file.lastModified()),
                        extension = getFileExtension(file.name)
                    )
                }

            android.os.Handler(android.os.Looper.getMainLooper()).post {
                callback(fileItems)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                callback(emptyList())
            }
        }
    }.start()
}

private fun toggleSelection(
    fileItem: FileItem,
    selectedFiles: Set<File>,
    onSelectedFilesChange: (Set<File>) -> Unit
) {
    val file = File(fileItem.path)
    val newSelection = selectedFiles.toMutableSet()
    if (newSelection.contains(file)) {
        newSelection.remove(file)
    } else {
        newSelection.add(file)
    }
    onSelectedFilesChange(newSelection)
}

fun formatPath(path: String, maxLength: Int = 50): String {
    val cleanPath = path.trimEnd('/')
    if (cleanPath.length <= maxLength) return cleanPath
    val segments = cleanPath.split("/").filter { it.isNotEmpty() }
    if (segments.size <= 2) return cleanPath
    var result = cleanPath
    for (i in 0 until segments.size - 2) {
        val remaining = segments.subList(i + 1, segments.size).joinToString("/")
        val candidate = ".../$remaining"
        if (candidate.length <= maxLength) {
            result = candidate
            break
        }
    }
    return result
}

private fun formatFileSize(size: Long): String {
    return when {
        size < 1024 -> "$size B"
        size < 1024 * 1024 -> "${size / 1024} KB"
        size < 1024 * 1024 * 1024 -> "${size / (1024 * 1024)} MB"
        else -> "${size / (1024 * 1024 * 1024)} GB"
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun getFileExtension(name: String): String {
    val dotIndex = name.lastIndexOf('.')
    return if (dotIndex >= 0 && dotIndex < name.length - 1) {
        name.substring(dotIndex + 1).lowercase()
    } else {
        ""
    }
}

private fun getFileIcon(isDirectory: Boolean, extension: String): Int {
    if (isDirectory) return R.drawable.folder
    return when (extension) {
        "txt", "log", "md", "json", "js", "css", "c", "cpp", "h", "hpp", "sh", "bat", "ini", "conf", "cfg", "yml", "yaml", "toml", "properties" -> R.drawable.txt
        "xml" -> R.drawable.xml
        "html" -> R.drawable.html
        "java" -> R.drawable.java
        "kt", "kts" -> R.drawable.kotlin
        "py" -> R.drawable.python
        "php" -> R.drawable.php
        "lua" -> R.drawable.lua
        "jar" -> R.drawable.jar
        "apk" -> R.drawable.apk
        "zip", "rar", "7z", "tar", "gz" -> R.drawable.zip
        "jpg", "jpeg", "png", "gif", "bmp", "webp" -> R.drawable.picture
        "mp3", "wav", "ogg", "m4a", "flac", "aac", "wma", "amr" -> R.drawable.music
        else -> R.drawable.file_default
    }
}

private fun getFileIconBg(@Suppress("UNUSED_PARAMETER") extension: String): androidx.compose.ui.graphics.Color {
    return androidx.compose.ui.graphics.Color(0xFFF0F0F0).copy(alpha = 0.0f)
}
