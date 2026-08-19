package me.lengyu.qedge.ui.pages

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import me.lengyu.qedge.utils.qq.QQCurrentEnv
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ripple
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import me.lengyu.qedge.R
import me.lengyu.qedge.utils.HostInfo
import me.lengyu.qedge.utils.LogUtils
import me.lengyu.qedge.ui.components.atoms.QEdgeCard
import me.lengyu.qedge.ui.core.theme.Dimens
import me.lengyu.qedge.ui.core.theme.QEdgeTheme
import me.lengyu.qedge.ui.pages.file.AudioPlayerDialog
import me.lengyu.qedge.ui.pages.file.FileListPanel
import me.lengyu.qedge.ui.pages.file.FileManagerUtils
import me.lengyu.qedge.ui.pages.file.ImagePreviewDialog
import me.lengyu.qedge.ui.pages.file.TextEditorScreen
import me.lengyu.qedge.ui.pages.file.formatPath
import java.io.File

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileManagerScreen(
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = QEdgeTheme.colors
    val context = androidx.compose.ui.platform.LocalContext.current
    val defaultPath = QQCurrentEnv.getHostPath()

    var leftPath by remember { mutableStateOf(defaultPath) }
    var rightPath by remember { mutableStateOf(defaultPath) }
    var leftZipFile by remember { mutableStateOf<File?>(null) }
    var leftZipPath by remember { mutableStateOf("") }
    var rightZipFile by remember { mutableStateOf<File?>(null) }
    var rightZipPath by remember { mutableStateOf("") }
    var leftSelectedFiles by remember { mutableStateOf<Set<File>>(emptySet()) }
    var rightSelectedFiles by remember { mutableStateOf<Set<File>>(emptySet()) }
    var activePanel by remember { mutableStateOf(PanelSide.LEFT) }
    var currentPage by remember { mutableStateOf(FileManagerPage.BROWSER) }
    var editorFilePath by remember { mutableStateOf("") }
    var imagePreviewPath by remember { mutableStateOf<String?>(null) }
    var audioPlayerPath by remember { mutableStateOf<String?>(null) }
    var actionMenuFile by remember { mutableStateOf<File?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var deleteTargetFile by remember { mutableStateOf<File?>(null) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameTargetFile by remember { mutableStateOf<File?>(null) }
    var renameInput by remember { mutableStateOf("") }
    var showPropertiesDialog by remember { mutableStateOf(false) }
    var propertiesFile by remember { mutableStateOf<File?>(null) }
    var propertiesText by remember { mutableStateOf("") }
    var showInstallConfirm by remember { mutableStateOf(false) }
    var installTargetFile by remember { mutableStateOf<File?>(null) }
    var showNewDialog by remember { mutableStateOf(false) }
    var newItemName by remember { mutableStateOf("") }
    var newItemType by remember { mutableStateOf("folder") }
    var refreshKey by remember { mutableStateOf(0) }

    fun triggerRefresh() {
        refreshKey++
    }

    val currentPath by remember {
        derivedStateOf {
            if (activePanel == PanelSide.LEFT) leftPath else rightPath
        }
    }

    val currentZipFile by remember {
        derivedStateOf {
            if (activePanel == PanelSide.LEFT) leftZipFile else rightZipFile
        }
    }

    val currentZipInnerPath by remember {
        derivedStateOf {
            if (activePanel == PanelSide.LEFT) leftZipPath else rightZipPath
        }
    }

    val isZipMode by remember {
        derivedStateOf {
            currentZipFile != null
        }
    }

    fun navigateUp() {
        val parent = File(currentPath).parent
        if (parent != null && currentPath != "/" && currentPath != android.os.Environment.getExternalStorageDirectory().absolutePath) {
            if (activePanel == PanelSide.LEFT) {
                leftPath = parent
                leftSelectedFiles = emptySet()
            } else {
                rightPath = parent
                rightSelectedFiles = emptySet()
            }
        } else {
            onBackClick()
        }
    }

    fun handleFileClick(file: File) {
        val extension = FileManagerUtils.getFileExtension(file.name)
        when {
            FileManagerUtils.isArchiveFile(extension) -> {
                if (activePanel == PanelSide.LEFT) {
                    leftZipFile = file
                    leftZipPath = ""
                } else {
                    rightZipFile = file
                    rightZipPath = ""
                }
            }
            FileManagerUtils.isApkFile(extension) -> {
                installTargetFile = file
                showInstallConfirm = true
            }
            FileManagerUtils.isImageFile(extension) -> {
                imagePreviewPath = file.absolutePath
            }
            FileManagerUtils.isAudioFile(extension) -> {
                audioPlayerPath = file.absolutePath
            }
            FileManagerUtils.isTextFile(extension) -> {
                editorFilePath = file.absolutePath
                currentPage = FileManagerPage.EDITOR
            }
        }
    }

    fun handleZipFileClick(zipFile: File, entryPath: String) {
        val extension = FileManagerUtils.getFileExtension(entryPath)
        when {
            FileManagerUtils.isImageFile(extension) -> {
                val tempFile = File(context.cacheDir, "zip_preview_${System.currentTimeMillis()}_${File(entryPath).name}")
                Thread {
                    FileManagerUtils.extractZipEntry(zipFile, entryPath, tempFile)
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        imagePreviewPath = tempFile.absolutePath
                    }
                }.start()
            }
            FileManagerUtils.isAudioFile(extension) -> {
                val tempFile = File(context.cacheDir, "zip_preview_${System.currentTimeMillis()}_${File(entryPath).name}")
                Thread {
                    FileManagerUtils.extractZipEntry(zipFile, entryPath, tempFile)
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        audioPlayerPath = tempFile.absolutePath
                    }
                }.start()
            }
            FileManagerUtils.isTextFile(extension) -> {
                val tempFile = File(context.cacheDir, "zip_preview_${System.currentTimeMillis()}_${File(entryPath).name}")
                Thread {
                    FileManagerUtils.extractZipEntry(zipFile, entryPath, tempFile)
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        editorFilePath = tempFile.absolutePath
                        currentPage = FileManagerPage.EDITOR
                    }
                }.start()
            }
        }
    }

    fun handleRefresh() {
        triggerRefresh()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        when (currentPage) {
            FileManagerPage.BROWSER -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.statusBars)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        QEdgeCard(
                            modifier = Modifier.size(40.dp),
                            animateContentSize = false,
                            onClick = onBackClick
                        ) {
                            Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                                Text("←", fontSize = 22.sp, color = colors.textPrimary)
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        val titleText = if (isZipMode && currentZipFile != null) {
                                if (currentZipInnerPath.isEmpty()) {
                                    currentZipFile!!.name
                                } else {
                                    val displayPath = currentZipInnerPath.removeSuffix("/")
                                    val lastSlash = displayPath.lastIndexOf('/')
                                    if (lastSlash >= 0) displayPath.substring(lastSlash + 1) else displayPath
                                }
                            } else {
                                File(currentPath).name.ifEmpty { currentPath }
                            }
                            Text(
                                text = titleText,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.textPrimary,
                                maxLines = 1,
                                modifier = Modifier
                                    .weight(1f)
                                    .combinedClickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = { },
                                        onLongClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val fullPath = if (isZipMode && currentZipFile != null) {
                                                "${currentZipFile!!.absolutePath}/$currentZipInnerPath"
                                            } else {
                                                currentPath
                                            }
                                            val clip = ClipData.newPlainText("path", fullPath)
                                            clipboard.setPrimaryClip(clip)
                                        }
                                    )
                            )

                        Spacer(modifier = Modifier.width(8.dp))

                        QEdgeCard(
                            modifier = Modifier.size(40.dp),
                            animateContentSize = false,
                            onClick = {
                                newItemName = ""
                                newItemType = "folder"
                                showNewDialog = true
                            }
                        ) {
                            Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                                Icon(
                                    painterResource(R.drawable.add),
                                    "新建",
                                    Modifier.size(20.dp),
                                    colors.textPrimary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        QEdgeCard(
                            modifier = Modifier.size(40.dp),
                            animateContentSize = false,
                            onClick = { handleRefresh() }
                        ) {
                            Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                                Icon(
                                    painterResource(R.drawable.ic_refresh),
                                    "刷新",
                                    Modifier.size(20.dp),
                                    colors.textPrimary
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 12.dp)
                    ) {
                        FileListPanel(
                            path = leftPath,
                            refreshKey = refreshKey,
                            zipFile = leftZipFile,
                            zipInnerPath = leftZipPath,
                            onPathChange = {
                                activePanel = PanelSide.LEFT
                                leftPath = it
                                leftZipFile = null
                                leftZipPath = ""
                                leftSelectedFiles = emptySet()
                            },
                            onZipPathChange = {
                                activePanel = PanelSide.LEFT
                                leftZipPath = it
                            },
                            onZipFileClick = { zipFile, entryPath ->
                                handleZipFileClick(zipFile, entryPath)
                            },
                            selectedFiles = leftSelectedFiles,
                            onSelectedFilesChange = { leftSelectedFiles = it },
                            isActive = activePanel == PanelSide.LEFT,
                            onActive = { activePanel = PanelSide.LEFT },
                            onFileClick = { handleFileClick(it) },
                            onFileLongClick = { file ->
                                activePanel = PanelSide.LEFT
                                actionMenuFile = file
                            },
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        FileListPanel(
                            path = rightPath,
                            refreshKey = refreshKey,
                            zipFile = rightZipFile,
                            zipInnerPath = rightZipPath,
                            onPathChange = {
                                activePanel = PanelSide.RIGHT
                                rightPath = it
                                rightZipFile = null
                                rightZipPath = ""
                                rightSelectedFiles = emptySet()
                            },
                            onZipPathChange = {
                                activePanel = PanelSide.RIGHT
                                rightZipPath = it
                            },
                            onZipFileClick = { zipFile, entryPath ->
                                handleZipFileClick(zipFile, entryPath)
                            },
                            selectedFiles = rightSelectedFiles,
                            onSelectedFilesChange = { rightSelectedFiles = it },
                            isActive = activePanel == PanelSide.RIGHT,
                            onActive = { activePanel = PanelSide.RIGHT },
                            onFileClick = { handleFileClick(it) },
                            onFileLongClick = { file ->
                                activePanel = PanelSide.RIGHT
                                actionMenuFile = file
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (imagePreviewPath != null) {
                    ImagePreviewDialog(
                        filePath = imagePreviewPath!!,
                        onDismiss = { imagePreviewPath = null }
                    )
                }

                if (audioPlayerPath != null) {
                    AudioPlayerDialog(
                        filePath = audioPlayerPath!!,
                        onDismiss = { audioPlayerPath = null }
                    )
                }

                if (actionMenuFile != null) {
                    val file = actionMenuFile!!
                    val extension = FileManagerUtils.getFileExtension(file.name)
                    val isArchive = FileManagerUtils.isArchiveFile(extension)
                    val isApk = FileManagerUtils.isApkFile(extension)
                    val isAudio = FileManagerUtils.isAudioFile(extension)
                    val isImage = FileManagerUtils.isImageFile(extension)
                    val targetPanelPath = if (activePanel == PanelSide.LEFT) rightPath else leftPath
                    val targetPanelName = if (activePanel == PanelSide.LEFT) "右" else "左"

                    Dialog(onDismissRequest = { actionMenuFile = null }) {
                        val colors = QEdgeTheme.colors
                        Column(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(colors.cardBackground)
                                .padding(vertical = 8.dp)
                        ) {
                            Text(
                                text = file.name,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                                maxLines = 1
                            )

                            MenuItem("📋", "复制到$targetPanelName 侧") {
                                actionMenuFile = null
                                Thread {
                                    FileManagerUtils.copyFile(file, File(targetPanelPath, file.name))
                                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                                        triggerRefresh()
                                    }
                                }.start()
                            }
                            MenuItem("✂️", "移动到$targetPanelName 侧") {
                                actionMenuFile = null
                                Thread {
                                    FileManagerUtils.moveFile(file, File(targetPanelPath, file.name))
                                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                                        triggerRefresh()
                                    }
                                }.start()
                            }
                            MenuItem("🗑️", "删除") {
                                actionMenuFile = null
                                deleteTargetFile = file
                                showDeleteConfirm = true
                            }
                            MenuItem("✏️", "重命名") {
                                actionMenuFile = null
                                renameTargetFile = file
                                renameInput = file.name
                                showRenameDialog = true
                            }
                            if (isArchive) {
                                MenuItem("📦", "解压到$targetPanelName 侧") {
                                    actionMenuFile = null
                                    Thread {
                                        FileManagerUtils.unzip(file, File(targetPanelPath, file.nameWithoutExtension))
                                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                                            triggerRefresh()
                                        }
                                    }.start()
                                }
                            }
                            if (isApk) {
                                MenuItem("📱", "安装软件") {
                                    actionMenuFile = null
                                    installTargetFile = file
                                    showInstallConfirm = true
                                }
                            }
                            if (isAudio) {
                                MenuItem("🎵", "播放音频") {
                                    actionMenuFile = null
                                    audioPlayerPath = file.absolutePath
                                }
                            }
                            if (isImage) {
                                MenuItem("🖼️", "查看图片") {
                                    actionMenuFile = null
                                    imagePreviewPath = file.absolutePath
                                }
                            }
                            MenuItem("ℹ️", "属性") {
                                actionMenuFile = null
                                propertiesFile = file
                                Thread {
                                    val size = if (file.isDirectory) {
                                        FileManagerUtils.formatFileSize(FileManagerUtils.getDirectorySize(file))
                                    } else {
                                        FileManagerUtils.formatFileSize(file.length())
                                    }
                                    val type = if (file.isDirectory) "文件夹" else FileManagerUtils.getFileExtension(file.name).ifEmpty { "未知" }
                                    val info = "名称：${file.name}\n路径：${file.absolutePath}\n大小：$size\n类型：$type"
                                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                                        propertiesText = info
                                        showPropertiesDialog = true
                                    }
                                }.start()
                            }
                        }
                    }
                }

                if (showDeleteConfirm && deleteTargetFile != null) {
                    val file = deleteTargetFile!!
                    val colors = QEdgeTheme.colors
                    Dialog(onDismissRequest = { showDeleteConfirm = false }) {
                        Column(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(colors.cardBackground)
                                .padding(24.dp)
                        ) {
                            Text("确认删除", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("确定要删除「${file.name}」吗？", fontSize = 14.sp, color = colors.textSecondary)
                            Spacer(modifier = Modifier.height(20.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                androidx.compose.material3.TextButton(onClick = { showDeleteConfirm = false }) {
                                    Text("取消", color = colors.textSecondary)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                androidx.compose.material3.TextButton(onClick = {
                                    showDeleteConfirm = false
                                    val targetFile = deleteTargetFile
                                    deleteTargetFile = null
                                    Thread {
                                        FileManagerUtils.deleteFile(targetFile!!)
                                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                                            triggerRefresh()
                                        }
                                    }.start()
                                }) {
                                    Text("删除", color = androidx.compose.ui.graphics.Color(0xFFEF4444))
                                }
                            }
                        }
                    }
                }

                if (showRenameDialog && renameTargetFile != null) {
                    val file = renameTargetFile!!
                    val colors = QEdgeTheme.colors
                    Dialog(onDismissRequest = { showRenameDialog = false }) {
                        Column(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(colors.cardBackground)
                                .padding(24.dp)
                        ) {
                            Text("重命名", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedTextField(
                                value = renameInput,
                                onValueChange = { renameInput = it },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                androidx.compose.material3.TextButton(onClick = { showRenameDialog = false }) {
                                    Text("取消", color = colors.textSecondary)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                androidx.compose.material3.TextButton(onClick = {
                                    if (renameInput.isNotBlank()) {
                                        showRenameDialog = false
                                        val targetFile = renameTargetFile
                                        val newName = renameInput
                                        renameTargetFile = null
                                        Thread {
                                            FileManagerUtils.renameFile(targetFile!!, newName)
                                            android.os.Handler(android.os.Looper.getMainLooper()).post {
                                                triggerRefresh()
                                            }
                                        }.start()
                                    }
                                }) {
                                    Text("确定", color = colors.accentBlue)
                                }
                            }
                        }
                    }
                }

                if (showPropertiesDialog && propertiesFile != null) {
                    val colors = QEdgeTheme.colors
                    Dialog(onDismissRequest = { showPropertiesDialog = false }) {
                        Column(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(colors.cardBackground)
                                .padding(24.dp)
                        ) {
                            Text("文件属性", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = propertiesText,
                                fontSize = 14.sp,
                                color = colors.textSecondary,
                                lineHeight = 22.sp
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                androidx.compose.material3.TextButton(onClick = { showPropertiesDialog = false }) {
                                    Text("确定", color = colors.accentBlue)
                                }
                            }
                        }
                    }
                }

                if (showInstallConfirm && installTargetFile != null) {
                    val file = installTargetFile!!
                    val colors = QEdgeTheme.colors
                    Dialog(onDismissRequest = { showInstallConfirm = false }) {
                        Column(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(colors.cardBackground)
                                .padding(24.dp)
                        ) {
                            Text("安装应用", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("确定要安装「${file.name}」吗？", fontSize = 14.sp, color = colors.textSecondary)
                            Spacer(modifier = Modifier.height(20.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                androidx.compose.material3.TextButton(onClick = { showInstallConfirm = false }) {
                                    Text("取消", color = colors.textSecondary)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                androidx.compose.material3.TextButton(onClick = {
                                    showInstallConfirm = false
                                    val targetFile = installTargetFile
                                    installTargetFile = null
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW)
                                    val apkUri = androidx.core.content.FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        targetFile!!
                                    )
                                    intent.setDataAndType(apkUri, "application/vnd.android.package-archive")
                                    intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                                    context.startActivity(intent)
                                }) {
                                    Text("安装", color = colors.accentBlue)
                                }
                            }
                        }
                    }
                }

                if (showNewDialog) {
                    val colors = QEdgeTheme.colors
                    Dialog(onDismissRequest = { showNewDialog = false }) {
                        Column(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(colors.cardBackground)
                                .padding(24.dp)
                        ) {
                            Text("新建", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedTextField(
                                value = newItemName,
                                onValueChange = { newItemName = it },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("请输入名称") }
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (newItemType == "folder") colors.accentBlue.copy(alpha = 0.1f)
                                            else colors.cardBackground
                                        )
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = ripple(color = colors.ripple),
                                            onClick = { newItemType = "folder" }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "文件夹",
                                        color = if (newItemType == "folder") colors.accentBlue else colors.textPrimary,
                                        fontWeight = if (newItemType == "folder") FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (newItemType == "file") colors.accentBlue.copy(alpha = 0.1f)
                                            else colors.cardBackground
                                        )
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = ripple(color = colors.ripple),
                                            onClick = { newItemType = "file" }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "文件",
                                        color = if (newItemType == "file") colors.accentBlue else colors.textPrimary,
                                        fontWeight = if (newItemType == "file") FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                androidx.compose.material3.TextButton(onClick = { showNewDialog = false }) {
                                    Text("取消", color = colors.textSecondary)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                androidx.compose.material3.TextButton(onClick = {
                                    if (newItemName.isNotBlank()) {
                                        showNewDialog = false
                                        val targetPath = if (activePanel == PanelSide.LEFT) leftPath else rightPath
                                        val itemName = newItemName
                                        val isFolder = newItemType == "folder"
                                        newItemName = ""
                                        Thread {
                                            val targetFile = File(targetPath, itemName)
                                            if (isFolder) {
                                                targetFile.mkdirs()
                                            } else {
                                                targetFile.createNewFile()
                                            }
                                            android.os.Handler(android.os.Looper.getMainLooper()).post {
                                                triggerRefresh()
                                            }
                                        }.start()
                                    }
                                }) {
                                    Text("创建", color = colors.accentBlue)
                                }
                            }
                        }
                    }
                }
            }
            FileManagerPage.EDITOR -> {
                TextEditorScreen(
                    filePath = editorFilePath,
                    isDarkTheme = isDarkTheme,
                    onThemeToggle = onThemeToggle,
                    onBackClick = { currentPage = FileManagerPage.BROWSER }
                )
            }
        }
    }
}

@Composable
private fun TopBarActionButton(
    iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit
) {
    val colors = QEdgeTheme.colors
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(colors.cardBackground)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = colors.ripple),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painterResource(iconRes),
            contentDescription,
            Modifier.size(20.dp),
            colors.textPrimary
        )
    }
}

@Composable
private fun MenuItem(
    icon: String,
    text: String,
    onClick: () -> Unit
) {
    val colors = QEdgeTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = colors.ripple),
                onClick = onClick
            )
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, fontSize = 18.sp)
        Spacer(modifier = Modifier.width(14.dp))
        Text(text, fontSize = 15.sp, color = colors.textPrimary)
    }
}

@Composable
private fun BottomActionBar(
    activePanel: PanelSide,
    leftSelectedCount: Int,
    rightSelectedCount: Int,
    onCopyToRight: () -> Unit,
    onCopyToLeft: () -> Unit,
    onDelete: () -> Unit,
    onRename: () -> Unit,
    onCreateFolder: () -> Unit,
    onExtractZip: () -> Unit
) {
    val colors = QEdgeTheme.colors
    val sourceCount = if (activePanel == PanelSide.LEFT) leftSelectedCount else rightSelectedCount
    val canTransfer = sourceCount > 0

    QEdgeCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            BottomActionItem(
                icon = "📁",
                label = "新建",
                onClick = onCreateFolder
            )

            BottomActionItem(
                icon = "✏️",
                label = "重命名",
                onClick = onRename,
                enabled = sourceCount == 1
            )

            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (canTransfer) colors.accentBlue else colors.textSecondary.copy(alpha = 0.2f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = if (canTransfer) ripple(color = Color.White.copy(alpha = 0.3f)) else null,
                        onClick = {
                            if (canTransfer) {
                                if (activePanel == PanelSide.LEFT) {
                                    onCopyToRight()
                                } else {
                                    onCopyToLeft()
                                }
                            }
                        },
                        enabled = canTransfer
                    ),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (activePanel == PanelSide.LEFT) {
                        Icon(
                            painterResource(R.drawable.ic_chevron_right),
                            "复制到右侧",
                            Modifier.size(24.dp),
                            androidx.compose.ui.graphics.Color.White
                        )
                    } else {
                        Icon(
                            painterResource(R.drawable.ic_chevron_right),
                            "复制到左侧",
                            Modifier
                                .size(24.dp)
                                .rotate(180f),
                            androidx.compose.ui.graphics.Color.White
                        )
                    }
                }
            }

            BottomActionItem(
                icon = "🗑️",
                label = "删除",
                onClick = onDelete,
                enabled = sourceCount > 0
            )

            BottomActionItem(
                icon = "📦",
                label = "解压",
                onClick = onExtractZip,
                enabled = sourceCount == 1
            )
        }
    }
}

@Composable
private fun BottomActionItem(
    icon: String,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    val colors = QEdgeTheme.colors
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = if (enabled) ripple(color = colors.ripple) else null,
                onClick = { if (enabled) onClick() },
                enabled = enabled
            )
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Text(
            text = icon,
            fontSize = 20.sp,
            color = if (enabled) colors.textPrimary else colors.textSecondary.copy(alpha = 0.4f)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = if (enabled) colors.textSecondary else colors.textSecondary.copy(alpha = 0.4f)
        )
    }
}

private enum class PanelSide {
    LEFT,
    RIGHT
}

private enum class FileManagerPage {
    BROWSER,
    EDITOR
}

private fun copyFiles(files: Set<File>, targetPath: String) {
    val targetDir = File(targetPath)
    if (!targetDir.exists() || !targetDir.isDirectory) return

    Thread {
        files.forEach { file ->
            try {
                if (file.isDirectory) {
                    copyDirectory(file, File(targetDir, file.name))
                } else {
                    file.copyTo(File(targetDir, file.name), overwrite = true)
                }
            } catch (e: Exception) {
                LogUtils.e(e)
            }
        }
    }.start()
}

private fun copyDirectory(source: File, target: File) {
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
}

private fun deleteFiles(files: Set<File>) {
    Thread {
        files.forEach { file ->
            try {
                if (file.isDirectory) {
                    deleteDirectory(file)
                } else {
                    file.delete()
                }
            } catch (e: Exception) {
                LogUtils.e(e)
            }
        }
    }.start()
}

private fun deleteDirectory(dir: File) {
    dir.listFiles()?.forEach { file ->
        if (file.isDirectory) {
            deleteDirectory(file)
        } else {
            file.delete()
        }
    }
    dir.delete()
}
