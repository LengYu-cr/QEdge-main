package me.lengyu.qedge.ui.components.dialogs

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import me.lengyu.qedge.R
import me.lengyu.qedge.ui.core.compatibility.XposedComposeDialog
import me.lengyu.qedge.ui.core.theme.QEdgeTheme
import me.lengyu.qedge.ui.core.theme.AccentBlue
import me.lengyu.qedge.ui.core.theme.LightTextSecondary
import me.lengyu.qedge.common.ModuleScope
import me.lengyu.qedge.utils.LogUtils

class UpdateDialog(
    context: Context,
    private val version: String,
    private val updateLog: String,
    private val downloadUrl: String,
    private val onIgnore: Runnable = Runnable {}
) : XposedComposeDialog(context) {

    override fun configureWindow() {
        super.configureWindow()
        window?.apply {
            setGravity(Gravity.CENTER)
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
            setDimAmount(0.35f)
        }
    }

    @Composable
    override fun DialogContent() {
        QEdgeTheme {
            UpdateContent(
                visible = isVisible,
                version = version,
                updateLog = updateLog,
                downloadUrl = downloadUrl,
                onDismiss = ::dismissWithAnimation,
                onIgnore = {
                    onIgnore.run()
                    dismissWithAnimation()
                }
            )
        }
    }
}

@Composable
private fun UpdateContent(
    visible: Boolean,
    version: String,
    updateLog: String,
    downloadUrl: String,
    onDismiss: () -> Unit,
    onIgnore: () -> Unit
) {
    val colors = QEdgeTheme.colors
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(0) }
    var downloadCompleted by remember { mutableStateOf(false) }
    var showBrowserButton by remember { mutableStateOf(false) }
    var downloadError by remember { mutableStateOf<String?>(null) }

    // 5秒仍卡在0%时显示浏览器下载按钮
    LaunchedEffect(isDownloading, downloadProgress) {
        if (isDownloading && downloadProgress == 0 && !showBrowserButton) {
            kotlinx.coroutines.delay(5000)
            if (downloadProgress == 0) {
                showBrowserButton = true
            }
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = scaleIn(tween(250, delayMillis = 50), initialScale = 0.92f) + fadeIn(tween(200)),
        exit = scaleOut(tween(150), targetScale = 0.92f) + fadeOut(tween(150))
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(colors.cardBackground)
                .padding(vertical = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_launcher),
                            contentDescription = null,
                            tint = androidx.compose.ui.graphics.Color.Unspecified,
                            modifier = Modifier.size(48.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "有新版本发布！",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "版本 $version",
                            fontSize = 14.sp,
                            color = AccentBlue
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.background)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "更新内容",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.textPrimary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = updateLog,
                            fontSize = 13.sp,
                            color = colors.textSecondary,
                            lineHeight = 20.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    AnimatedVisibility(
                        visible = isDownloading,
                        enter = fadeIn(tween(200)),
                        exit = fadeOut(tween(200))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(LightTextSecondary.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(downloadProgress / 100f)
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(AccentBlue)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "下载中 ${downloadProgress}%",
                                fontSize = 12.sp,
                                color = colors.textSecondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 下载出错或卡在0%时显示错误信息和浏览器下载按钮
                    AnimatedVisibility(
                        visible = showBrowserButton || downloadError != null,
                        enter = fadeIn(tween(200)),
                        exit = fadeOut(tween(200))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (downloadError != null) {
                                Text(
                                    text = downloadError!!,
                                    fontSize = 12.sp,
                                    color = Color(0xFFFF6B6B),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                            val context = LocalContext.current
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(AccentBlue)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = {
                                            try {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl))
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                LogUtils.e(e)
                                            }
                                        }
                                    )
                                    .padding(vertical = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "在浏览器中下载",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(colors.background)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = onIgnore
                                )
                                .padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "忽略",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.textPrimary
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(AccentBlue)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = {
                                        if (!isDownloading && !downloadCompleted) {
                                            isDownloading = true
                                            downloadProgress = 0
                                            showBrowserButton = false
                                            downloadError = null
                                        }
                                    }
                                )
                                .padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = when {
                                    downloadCompleted -> "下载完成"
                                    isDownloading -> "下载中"
                                    else -> "立即更新"
                                },
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                        }
                    }

                    if (isDownloading) {
                        LaunchedEffect(Unit) {
                            downloadApk(downloadUrl,
                                onProgress = { progress ->
                                    downloadProgress = progress
                                    if (progress >= 100) {
                                        downloadCompleted = true
                                        isDownloading = false
                                    }
                                },
                                onError = { error ->
                                    downloadError = error
                                    isDownloading = false
                                }
                            )
                        }
                    }
                }
            }
    }

private fun downloadApk(url: String, onProgress: (Int) -> Unit, onError: (String) -> Unit) {
    ModuleScope.launchIOJava("UpdateDialog") {
        try {
            val context = me.lengyu.qedge.utils.HostInfo.getHostContext() ?: run {
                onError("无法获取应用上下文")
                return@launchIOJava
            }
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

            val cleanUrl = url.trim().removeSurrounding("`", "`").removeSurrounding("\"", "\"")

            val request = DownloadManager.Request(Uri.parse(cleanUrl))
                .setTitle("QEdge 更新")
                .setDescription("正在下载 QEdge 最新版本")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "QEdge.apk")
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

            val downloadId = downloadManager.enqueue(request)

            var lastProgress = 0
            while (true) {
                val query = DownloadManager.Query().setFilterById(downloadId)
                val cursor = downloadManager.query(query)
                if (cursor.moveToFirst()) {
                    val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                    val bytesDownloaded = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                    val bytesTotal = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))

                    if (status == DownloadManager.STATUS_SUCCESSFUL) {
                        onProgress(100)
                        val uri = downloadManager.getUriForDownloadedFile(downloadId)
                        if (uri != null) {
                            openApk(context, uri)
                        }
                        cursor.close()
                        break
                    } else if (status == DownloadManager.STATUS_FAILED) {
                        val reason = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
                        onError("下载失败 (错误码: $reason)")
                        cursor.close()
                        break
                    } else if (bytesTotal > 0) {
                        val progress = (bytesDownloaded * 100 / bytesTotal).toInt()
                        if (progress != lastProgress) {
                            lastProgress = progress
                            onProgress(progress)
                        }
                    }
                }
                cursor.close()
                Thread.sleep(500)
            }
        } catch (e: Exception) {
            LogUtils.e(e)
            onError(e.message ?: "下载异常")
        }
    }
}

private fun openApk(context: Context, uri: Uri) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/vnd.android.package-archive")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "无法打开安装包", Toast.LENGTH_SHORT).show()
    }
}