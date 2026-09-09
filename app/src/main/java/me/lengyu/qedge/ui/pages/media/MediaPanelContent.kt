package me.lengyu.qedge.ui.pages.media

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.media.MediaPlayer
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.widget.ImageView
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.lengyu.qedge.plugin.view.MediaPanelLoader
import me.lengyu.qedge.R
import me.lengyu.qedge.ui.core.theme.QEdgeTheme
import me.lengyu.qedge.utils.LogUtils
import me.lengyu.qedge.utils.ModuleConfig
import me.lengyu.qedge.utils.Toasts
import me.lengyu.qedge.utils.qq.MsgTool
import me.lengyu.qedge.utils.qq.QQCurrentEnv
import java.io.File

/**
 * 表情/语音/视频综合面板内容。
 * 表情栏：本地图库 + 服务器热门合集；语音/视频预留。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaPanelContent(contact: MediaPanelLoader.PanelContact, onDismiss: () -> Unit) {
    val colors = QEdgeTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val defaultTabKey = remember { ModuleConfig.getString(MediaPanelLoader.KEY_DEFAULT_TAB, "img") }
    var tab by remember {
        mutableIntStateOf(
            when (defaultTabKey) {
                "voice" -> 1
                "video" -> 2
                else -> 0
            }
        )
    }

    // 表情栏数据
    var collections by remember { mutableStateOf<List<MediaPanelLoader.MediaCollection>>(emptyList()) }
    var collectionsLoaded by remember { mutableStateOf(false) }
    var galleryFiles by remember { mutableStateOf<List<File>>(emptyList()) }

    // 导航：0=主界面 1=本地图库 2=某合集详情
    var nav by remember { mutableIntStateOf(0) }
    var currentCollection by remember { mutableStateOf<MediaPanelLoader.MediaCollection?>(null) }
    var collectionItems by remember { mutableStateOf<List<MediaPanelLoader.MediaItem>>(emptyList()) }

    // 详情弹窗
    var detail by remember { mutableStateOf<DetailTarget?>(null) }

    // 语音/视频详情弹窗
    var audioDetail by remember { mutableStateOf<DetailTarget?>(null) }
    var audioDetailType by remember { mutableStateOf("voice") }

    // 搜索（结果直接展示在面板内，不再弹窗）
    var searchMode by remember { mutableStateOf(false) }
    var searchType by remember { mutableStateOf("img") }

    fun sendImage(target: String) {
        scope.launch(Dispatchers.IO) {
            try {
                MsgTool.sendPic(contact.peerUin, target, contact.chatType)
                Toasts.toast("已发送")
            } catch (e: Throwable) {
                LogUtils.e("MediaPanelContent", "sendImage failed: " + e.message)
                Toasts.toast("发送失败: " + e.message)
            }
        }
    }

    fun sendVoice(target: String) {
        scope.launch(Dispatchers.IO) {
            try {
                MsgTool.sendPtt(contact.peerUin, target, contact.chatType)
                Toasts.toast("已发送")
            } catch (e: Throwable) {
                LogUtils.e("MediaPanelContent", "sendVoice failed: " + e.message)
                Toasts.toast("发送失败: " + e.message)
            }
        }
    }

    fun sendVideo(target: String) {
        scope.launch(Dispatchers.IO) {
            try {
                MsgTool.sendVideo(contact.peerUin, target, contact.chatType)
                Toasts.toast("已发送")
            } catch (e: Throwable) {
                LogUtils.e("MediaPanelContent", "sendVideo failed: " + e.message)
                Toasts.toast("发送失败: " + e.message)
            }
        }
    }

    fun copyText(text: String) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        cm?.setPrimaryClip(ClipData.newPlainText("media", text))
        Toasts.toast("已复制")
    }

    LaunchedEffect(tab) {
        if (tab == 0 && !collectionsLoaded) {
            collectionsLoaded = true
            collections = withContext(Dispatchers.IO) {
                MediaPanelLoader.MediaApi.fetchCollections("img", 200)
            }
        }
    }

    LaunchedEffect(nav) {
        if (nav == 1) {
            galleryFiles = withContext(Dispatchers.IO) { listGalleryFiles() }
        } else if (nav == 2 && currentCollection != null) {
            val c = currentCollection
            if (c != null) {
                collectionItems = withContext(Dispatchers.IO) {
                    MediaPanelLoader.MediaApi.fetchCollectionItems(c.name, "img")
                }
            }
        }
    }

    Column(
        Modifier
            .fillMaxWidth()
            .fillMaxSize()
            .background(colors.background)
            .padding(top = 12.dp)
    ) {
        // 顶部拖拽条
        Box(
            Modifier
                .align(Alignment.CenterHorizontally)
                .size(width = 40.dp, height = 4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(colors.textSecondary.copy(0.3f))
        )
        Spacer(Modifier.height(12.dp))

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "综合面板",
                Modifier.weight(1f),
                colors.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            SearchIconButton {
                searchType = when (tab) { 0 -> "img"; 1 -> "voice"; else -> "video" }
                searchMode = true
            }
        }
        Spacer(Modifier.height(12.dp))

        if (searchMode) {
            SearchPage(
                type = searchType,
                onOpenDetail = { url -> detail = DetailTarget.Network(url) },
                onOpenAudio = { t -> audioDetail = t; audioDetailType = searchType },
                onSendTarget = { url ->
                    when (searchType) {
                        "voice" -> sendVoice(url)
                        "video" -> sendVideo(url)
                        else -> sendImage(url)
                    }
                },
                onBack = { searchMode = false }
            )
        } else {
            // 三栏切换
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TabButton("表情", tab == 0) { tab = 0 }
                TabButton("语音", tab == 1) { tab = 1 }
                TabButton("视频", tab == 2) { tab = 2 }
            }
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = colors.textSecondary.copy(0.08f))

            when (tab) {
                0 -> EmojiPanel(
                    nav = nav,
                    collections = collections,
                    galleryFiles = galleryFiles,
                    collection = currentCollection,
                    collectionItems = collectionItems,
                    onOpenLocal = { nav = 1 },
                    onOpenCollection = { c -> currentCollection = c; nav = 2 },
                    onBack = { nav = 0; currentCollection = null },
                    onTapItem = { detail = DetailTarget.Network(it) },
                    onTapLocal = { detail = DetailTarget.Local(it) },
                    onLongSend = { sendImage(it) },
                    onSearch = { searchType = "img"; searchMode = true }
                )
                1 -> AudioPanel("voice", ::sendVoice, onOpenDetail = { t -> audioDetail = t; audioDetailType = "voice" }, onSearch = { searchType = "voice"; searchMode = true })
                2 -> AudioPanel("video", ::sendVideo, onOpenDetail = { t -> audioDetail = t; audioDetailType = "video" }, onSearch = { searchType = "video"; searchMode = true })
            }
        }
    }

    detail?.let { target ->
        MediaDetailDialog(
            target = target,
            onCopy = { copyText(it) },
            onDownload = { url ->
                scope.launch(Dispatchers.IO) {
                    val saved = MediaPanelLoader.MediaImageCache.saveToGallery(url, context)
                    if (saved != null) Toasts.toast("已保存到本地图库") else Toasts.toast("保存失败")
                }
            },
            onSend = { sendImage(it) },
            onDelete = {
                val f = (target as? DetailTarget.Local)?.file
                if (f != null) {
                    scope.launch(Dispatchers.IO) {
                        val ok = MediaPanelLoader.MediaImageCache.deleteFromGallery(f.absolutePath, context)
                        withContext(Dispatchers.Main) {
                            if (ok) {
                                galleryFiles = galleryFiles.filterNot { it.absolutePath == f.absolutePath }
                                Toasts.toast("已删除")
                            } else {
                                Toasts.toast("删除失败")
                            }
                            detail = null
                        }
                    }
                }
            },
            onDismiss = { detail = null }
        )
    }

    audioDetail?.let { target ->
        AudioDetailDialog(
            type = audioDetailType,
            target = target,
            onCopy = { copyText(it) },
            onSend = {
                when (audioDetailType) {
                    "voice" -> sendVoice(it)
                    "video" -> sendVideo(it)
                    else -> sendImage(it)
                }
            },
            onDismiss = { audioDetail = null }
        )
    }
}

@Composable
private fun RowScope.TabButton(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = QEdgeTheme.colors
    Box(
        Modifier
            .weight(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) colors.accentBlue else colors.cardBackground)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) androidx.compose.ui.graphics.Color.White else colors.textPrimary
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EmojiPanel(
    nav: Int,
    collections: List<MediaPanelLoader.MediaCollection>,
    galleryFiles: List<File>,
    collection: MediaPanelLoader.MediaCollection?,
    collectionItems: List<MediaPanelLoader.MediaItem>,
    onOpenLocal: () -> Unit,
    onOpenCollection: (MediaPanelLoader.MediaCollection) -> Unit,
    onBack: () -> Unit,
    onTapItem: (String) -> Unit,
    onTapLocal: (File) -> Unit,
    onLongSend: (String) -> Unit,
    onSearch: () -> Unit
) {
    val colors = QEdgeTheme.colors
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // 本地图库上传相关状态
    var selecting by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(setOf<String>()) }
    var showTagDialog by remember { mutableStateOf(false) }
    var uploading by remember { mutableStateOf(false) }

    // 网络图集多选下载状态
    var netSelecting by remember { mutableStateOf(false) }
    var netSelected by remember { mutableStateOf(setOf<String>()) }
    var netDownloading by remember { mutableStateOf(false) }

    fun toggleSelect(path: String) {
        selected = if (path in selected) selected - path else selected + path
    }

    fun toggleNetSelect(url: String) {
        netSelected = if (url in netSelected) netSelected - url else netSelected + url
    }

    fun doNetDownload(context: Context) {
        if (netSelected.isEmpty() || netDownloading) return
        val urls = netSelected.toList()
        netDownloading = true
        scope.launch(Dispatchers.IO) {
            var count = 0
            for (url in urls) {
                val saved = MediaPanelLoader.MediaImageCache.saveToGallery(url, context)
                if (saved != null) count++
            }
            withContext(Dispatchers.Main) {
                netDownloading = false
                netSelecting = false
                netSelected = emptySet()
                Toasts.toast(if (count > 0) "已保存 $count 张到本地图库" else "保存失败")
            }
        }
    }

    fun doUpload(collection: String, tags: List<String>) {
        if (selected.isEmpty() || collection.isEmpty() || uploading) return
        val paths = selected.toList()
        val uin = QQCurrentEnv.getCurrentUin()
        uploading = true
        scope.launch(Dispatchers.IO) {
            val result = try {
                MediaPanelLoader.MediaApi.uploadImages(collection, paths, tags, "img", uin)
            } catch (e: Throwable) {
                LogUtils.e("MediaPanelContent", "upload failed: " + e.message)
                MediaPanelLoader.UploadResult(0, paths.size)
            }
            withContext(Dispatchers.Main) {
                uploading = false
                showTagDialog = false
                if (result.success > 0) {
                    val msg = if (result.failed > 0) {
                        "上传成功 ${result.success} 张，失败 ${result.failed} 张"
                    } else {
                        "上传成功 ${result.success} 张"
                    }
                    Toasts.toast(msg)
                    selecting = false
                    selected = emptySet()
                } else {
                    Toasts.toast("上传失败，共 ${result.failed} 张")
                }
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        when (nav) {
            1 -> {
                // 本地图库
                Column(Modifier.fillMaxSize()) {
                    PanelHeader(
                        "本地图库",
                        {
                            selecting = false
                            selected = emptySet()
                            showTagDialog = false
                            onBack()
                        },
                        onSearch
                    )
                    if (!selecting) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, bottom = 6.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.accentBlue)
                                .combinedClickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { selecting = true }
                                )
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "上传图片",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = androidx.compose.ui.graphics.Color.White
                            )
                        }
                    }
                    if (galleryFiles.isEmpty()) {
                        EmptyHint(
                            if (selecting) "本地图库为空"
                            else "本地图库为空，可在网络图库详情中下载保存"
                        )
                    } else {
                        LazyColumn(
                            Modifier.weight(1f).padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(galleryFiles.chunked(5)) { row ->
                                MediaThumbRow(
                                    row.map { MediaCell(it.absolutePath, it.absolutePath, null) },
                                    onTap = { cell ->
                                        if (selecting) toggleSelect(cell.key)
                                        else onTapLocal(File(cell.key))
                                    },
                                    onLong = { cell ->
                                        if (!selecting) onLongSend(cell.key)
                                    },
                                    isMarked = { cell -> cell.key in selected }
                                )
                            }
                        }
                    }
                    if (selecting) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            DetailButton("取消", colors.textSecondary, Modifier.weight(1f)) {
                                selecting = false
                                selected = emptySet()
                            }
                            DetailButton(
                                if (uploading) "上传中…" else "上传 (${selected.size})",
                                colors.accentBlue,
                                Modifier.weight(1f)
                            ) {
                                if (!uploading) {
                                    if (selected.isEmpty()) {
                                        Toasts.toast("请先选择要上传的图片")
                                    } else {
                                        showTagDialog = true
                                    }
                                }
                            }
                        }
                    }
                }
            }
        2 -> {
            // 合集详情
            val name = collection?.name ?: ""
            Column(Modifier.fillMaxSize()) {
                PanelHeader(
                    name,
                    onBack,
                    onSearch
                )
                if (!netSelecting) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 6.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.accentGreen)
                            .combinedClickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { netSelecting = true }
                            )
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "下载图片",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = androidx.compose.ui.graphics.Color.White
                        )
                    }
                }
                if (collectionItems.isEmpty()) {
                    EmptyHint("该合集暂无资源")
                } else {
                    LazyColumn(
                        Modifier.weight(1f).padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(collectionItems.chunked(5)) { row ->
                            MediaThumbRow(
                                row.map { MediaCell(it.url, it.url, null) },
                                onTap = { cell ->
                                    if (netSelecting) toggleNetSelect(cell.key)
                                    else onTapItem(cell.key)
                                },
                                onLong = { cell ->
                                    if (!netSelecting) onLongSend(cell.key)
                                },
                                isMarked = { cell -> cell.key in netSelected }
                            )
                        }
                    }
                }
                if (netSelecting) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        DetailButton("取消", colors.textSecondary, Modifier.weight(1f)) {
                            netSelecting = false
                            netSelected = emptySet()
                        }
                        DetailButton(
                            if (netDownloading) "下载中…" else "下载 (${netSelected.size})",
                            colors.accentGreen,
                            Modifier.weight(1f)
                        ) {
                            if (!netDownloading) {
                                if (netSelected.isEmpty()) {
                                    Toasts.toast("请先选择要下载的图片")
                                } else {
                                    doNetDownload(context)
                                }
                            }
                        }
                    }
                }
            }
        }
        else -> {
            // 主界面：本地图库 + 热门合集
            LazyColumn(
                Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(colors.cardBackground)
                            .combinedClickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onOpenLocal
                            )
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("本地图库", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                        Spacer(Modifier.weight(1f))
                        Text("›", fontSize = 18.sp, color = colors.textSecondary)
                    }
                }

                item {
                    Text(
                        "热门合集",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.textPrimary
                    )
                }

                if (collections.isEmpty()) {
                    item { EmptyHint("暂无服务器合集") }
                } else {
                    items(collections.chunked(5)) { row ->
                        MediaThumbRow(
                            row.map {
                                MediaCell(
                                    it.name,
                                    it.cover,
                                    it.name,
                                    it.tags.joinToString(", ").takeIf { s -> s.isNotEmpty() }
                                )
                            },
                            onTap = { c -> collections.firstOrNull { it.name == c.key }?.let(onOpenCollection) },
                            onLong = {}
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                }
            }
        }
        }
        if (showTagDialog) {
            UploadTagDialog(
                count = selected.size,
                onConfirm = { c, t -> doUpload(c, t) },
                onDismiss = { showTagDialog = false }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MediaThumbRow(
    cells: List<MediaCell>,
    onTap: (MediaCell) -> Unit,
    onLong: (MediaCell) -> Unit,
    isMarked: (MediaCell) -> Boolean = { false }
) {
    val colors = QEdgeTheme.colors
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        cells.forEach { cell ->
            Column(
                Modifier.weight(1f)
            ) {
                Box(
                    Modifier
                        .aspectRatio(1f)
                        .let {
                            if (isMarked(cell)) {
                                it.border(2.dp, colors.accentBlue, RoundedCornerShape(10.dp))
                            } else {
                                it
                            }
                        }
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.cardBackground)
                        .combinedClickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onTap(cell) },
                            onLongClick = { onLong(cell) }
                        )
                ) {
                    AnimatedImage(cell.image, Modifier.fillMaxSize(), ContentScale.Crop)
                    if (isMarked(cell)) {
                        Box(
                            Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .size(20.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(colors.accentBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "✓",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = androidx.compose.ui.graphics.Color.White
                            )
                        }
                    }
                }
                if (cell.label != null) {
                    Text(
                        cell.label,
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        color = colors.textPrimary,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (cell.subLabel != null) {
                    Text(
                        cell.subLabel,
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp),
                        color = colors.textSecondary,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        repeat(5 - cells.size) { Spacer(Modifier.weight(1f)) }
    }
}

@Composable
private fun PanelHeader(title: String, onBack: () -> Unit, onSearch: (() -> Unit)? = null) {
    val colors = QEdgeTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(colors.cardBackground)
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onBack
                )
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text("返回", fontSize = 13.sp, color = colors.accentBlue)
        }
        Spacer(Modifier.padding(start = 12.dp))
        Text(
            title,
            Modifier.weight(1f),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (onSearch != null) {
            SearchIconButton(onClick = onSearch)
        }
    }
}

@Composable
private fun EmptyHint(text: String) {
    val colors = QEdgeTheme.colors
    Box(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        Alignment.Center
    ) {
        Text(text, fontSize = 14.sp, color = colors.textSecondary)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SearchIconButton(onClick: () -> Unit) {
    val colors = QEdgeTheme.colors
    Box(
        Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(colors.cardBackground)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painterResource(R.drawable.ic_search),
            "搜索",
            Modifier.size(20.dp),
            colors.textPrimary
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SearchPage(
    type: String,
    onOpenDetail: (String) -> Unit,
    onOpenAudio: (DetailTarget) -> Unit,
    onSendTarget: (String) -> Unit,
    onBack: () -> Unit
) {
    val colors = QEdgeTheme.colors
    val scope = rememberCoroutineScope()
    var query by remember(type) { mutableStateOf("") }
    var results by remember(type) { mutableStateOf<List<MediaPanelLoader.MediaCollection>>(emptyList()) }
    var searched by remember(type) { mutableStateOf(false) }
    var viewCollection by remember(type) { mutableStateOf<MediaPanelLoader.MediaCollection?>(null) }
    var viewItems by remember(type) { mutableStateOf<List<MediaPanelLoader.MediaItem>>(emptyList()) }

    fun doSearch() {
        val kw = query.trim()
        if (kw.isEmpty()) {
            Toasts.toast("请输入搜索关键词")
            return
        }
        scope.launch(Dispatchers.IO) {
            val r = MediaPanelLoader.MediaApi.searchCollections(type, kw)
            withContext(Dispatchers.Main) {
                results = r
                searched = true
            }
        }
    }

    fun openCollection(c: MediaPanelLoader.MediaCollection) {
        viewCollection = c
        viewItems = emptyList()
        scope.launch(Dispatchers.IO) {
            val items = MediaPanelLoader.MediaApi.fetchCollectionItems(c.name, type)
            withContext(Dispatchers.Main) { viewItems = items }
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.cardBackground)
                    .combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { if (viewCollection != null) viewCollection = null else onBack() }
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(if (viewCollection != null) "返回" else "关闭", fontSize = 13.sp, color = colors.accentBlue)
            }
            Spacer(Modifier.padding(start = 12.dp))
            Text(
                viewCollection?.name ?: "搜索合集",
                Modifier.weight(1f),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (viewCollection == null) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.cardBackground)
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        textStyle = TextStyle(fontSize = 14.sp, color = colors.textPrimary),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        decorationBox = { inner ->
                            if (query.isEmpty()) {
                                Text("输入关键词", fontSize = 14.sp, color = colors.textSecondary)
                            }
                            inner()
                        }
                    )
                }
                Spacer(Modifier.padding(start = 8.dp))
                Box(
                    Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.accentBlue)
                        .combinedClickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { doSearch() }
                        )
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text("搜索", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color.White)
                }
            }
            Spacer(Modifier.height(12.dp))

            if (!searched) {
                EmptyHint("输入关键词搜索合集")
            } else if (results.isEmpty()) {
                EmptyHint("未找到相关合集")
            } else {
                LazyColumn(
                    Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(results) { c ->
                        CollectionRow(c) { openCollection(c) }
                    }
                }
            }
        } else {
            if (viewItems.isEmpty()) {
                EmptyHint("该合集暂无资源")
            } else if (type == "img") {
                LazyColumn(
                    Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(viewItems.chunked(5)) { row ->
                        MediaThumbRow(
                            row.map { MediaCell(it.url, it.url, null) },
                            onTap = { cell -> onOpenDetail(cell.key) },
                            onLong = { cell -> onSendTarget(cell.key) }
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                }
            } else {
                LazyColumn(
                    Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(viewItems) { it ->
                        AudioRow(
                            it.url.substringAfterLast('/'),
                            onClick = { onOpenAudio(DetailTarget.Network(it.url)) },
                            onLongClick = { onSendTarget(it.url) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AudioPanel(type: String, onSend: (String) -> Unit, onOpenDetail: (DetailTarget) -> Unit, onSearch: () -> Unit) {
    val colors = QEdgeTheme.colors
    val scope = rememberCoroutineScope()

    var collections by remember(type) { mutableStateOf<List<MediaPanelLoader.MediaCollection>>(emptyList()) }
    var loaded by remember(type) { mutableStateOf(false) }
    var localFiles by remember(type) { mutableStateOf<List<File>>(emptyList()) }
    var nav by remember(type) { mutableIntStateOf(0) }
    var current by remember(type) { mutableStateOf<MediaPanelLoader.MediaCollection?>(null) }
    var items by remember(type) { mutableStateOf<List<MediaPanelLoader.MediaItem>>(emptyList()) }

    // 本地文件多选上传状态
    var selecting by remember(type) { mutableStateOf(false) }
    var selected by remember(type) { mutableStateOf(setOf<String>()) }
    var showTagDialog by remember(type) { mutableStateOf(false) }
    var uploading by remember(type) { mutableStateOf(false) }

    fun toggleSelect(path: String) {
        selected = if (path in selected) selected - path else selected + path
    }

    fun doUpload(collection: String, tags: List<String>) {
        if (selected.isEmpty() || collection.isEmpty() || uploading) return
        val paths = selected.toList()
        val uin = QQCurrentEnv.getCurrentUin()
        uploading = true
        scope.launch(Dispatchers.IO) {
            val result = try {
                MediaPanelLoader.MediaApi.uploadImages(collection, paths, tags, type, uin)
            } catch (e: Throwable) {
                LogUtils.e("MediaPanelContent", "upload $type failed: " + e.message)
                MediaPanelLoader.UploadResult(0, paths.size)
            }
            withContext(Dispatchers.Main) {
                uploading = false
                showTagDialog = false
                if (result.success > 0) {
                    val msg = if (result.failed > 0) {
                        "上传成功 ${result.success} 个，失败 ${result.failed} 个"
                    } else {
                        "上传成功 ${result.success} 个"
                    }
                    Toasts.toast(msg)
                    selecting = false
                    selected = emptySet()
                } else {
                    Toasts.toast("上传失败，共 ${result.failed} 个")
                }
            }
        }
    }

    LaunchedEffect(type) {
        if (!loaded) {
            loaded = true
            collections = withContext(Dispatchers.IO) { MediaPanelLoader.MediaApi.fetchCollections(type, 200) }
            localFiles = withContext(Dispatchers.IO) { MediaPanelLoader.MediaFileCache.listFiles(type) }
        }
    }

    LaunchedEffect(type, nav, current) {
        if (nav == 1 && current != null) {
            val c = current
            if (c != null) {
                items = withContext(Dispatchers.IO) {
                    MediaPanelLoader.MediaApi.fetchCollectionItems(c.name, type)
                }
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        when (nav) {
            0 -> {
                Column(Modifier.fillMaxSize()) {
                    if (!selecting) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.accentBlue)
                                .combinedClickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { selecting = true }
                                )
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                if (type == "voice") "上传语音" else "上传视频",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = androidx.compose.ui.graphics.Color.White
                            )
                        }
                    }
                    LazyColumn(
                        Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item { SectionTitle(if (type == "voice") "本地语音" else "本地视频") }
                        if (localFiles.isEmpty()) {
                            item { EmptyHint("暂无本地资源") }
                        } else {
                            items(localFiles) { f ->
                                AudioRow(
                                    f.name,
                                    onClick = {
                                        if (selecting) toggleSelect(f.absolutePath)
                                        else onOpenDetail(DetailTarget.Local(f))
                                    },
                                    onLongClick = { if (!selecting) onSend(f.absolutePath) },
                                    marked = f.absolutePath in selected
                                )
                            }
                        }

                        item {
                            Spacer(Modifier.height(8.dp))
                            SectionTitle("服务器合集")
                        }
                        if (collections.isEmpty()) {
                            item { EmptyHint("暂无服务器合集") }
                        } else {
                            items(collections) { c ->
                                CollectionRow(c) { current = c; nav = 1 }
                            }
                        }
                    }
                    if (selecting) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            DetailButton("取消", colors.textSecondary, Modifier.weight(1f)) {
                                selecting = false
                                selected = emptySet()
                            }
                            DetailButton(
                                if (uploading) "上传中…" else "上传 (${selected.size})",
                                colors.accentBlue,
                                Modifier.weight(1f)
                            ) {
                                if (!uploading) {
                                    if (selected.isEmpty()) {
                                        Toasts.toast("请先选择要上传的文件")
                                    } else {
                                        showTagDialog = true
                                    }
                                }
                            }
                        }
                    }
                }
            }
            1 -> {
                val name = current?.name ?: ""
                Column(Modifier.fillMaxSize()) {
                    PanelHeader(name, onBack = { nav = 0; current = null }, onSearch = onSearch)
                    if (items.isEmpty()) {
                        EmptyHint("该合集暂无资源")
                    } else {
                        LazyColumn(
                            Modifier.weight(1f).padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(items) { it ->
                                AudioRow(
                                    it.url.substringAfterLast('/'),
                                    onClick = { onOpenDetail(DetailTarget.Network(it.url)) },
                                    onLongClick = { onSend(it.url) }
                                )
                            }
                        }
                    }
                }
            }
        }
        if (showTagDialog) {
            UploadTagDialog(
                count = selected.size,
                onConfirm = { c, t -> doUpload(c, t) },
                onDismiss = { showTagDialog = false }
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    val colors = QEdgeTheme.colors
    Text(text, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AudioRow(
    label: String,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    marked: Boolean = false
) {
    val colors = QEdgeTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.cardBackground)
            .let { if (marked) it.border(2.dp, colors.accentBlue, RoundedCornerShape(12.dp)) else it }
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            Modifier.weight(1f),
            fontSize = 14.sp,
            color = colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            if (marked) "✓" else "›",
            fontSize = if (marked) 16.sp else 18.sp,
            fontWeight = if (marked) FontWeight.Bold else FontWeight.Normal,
            color = colors.accentBlue
        )
    }
}

@Composable
private fun CollectionRow(c: MediaPanelLoader.MediaCollection, onClick: () -> Unit) {
    val colors = QEdgeTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.cardBackground)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            c.name,
            Modifier.weight(1f),
            fontSize = 14.sp,
            color = colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text("${c.count} 条", fontSize = 12.sp, color = colors.textSecondary)
    }
}

// ---------- 详情弹窗 ----------

sealed class DetailTarget {
    data class Local(val file: File) : DetailTarget()
    data class Network(val url: String) : DetailTarget()
}

data class MediaCell(val key: String, val image: String, val label: String?, val subLabel: String? = null)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MediaDetailDialog(
    target: DetailTarget,
    onCopy: (String) -> Unit,
    onDownload: (String) -> Unit,
    onSend: (String) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = QEdgeTheme.colors

    Box(
        Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.4f))
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.Center
    ) {
        // 拦截点击避免冒泡关闭
        Column(
            Modifier
                .fillMaxWidth(0.85f)
                .clip(RoundedCornerShape(20.dp))
                .background(colors.background)
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
                .padding(20.dp)
        ) {
            val isNetwork = target is DetailTarget.Network
            val source = when (target) {
                is DetailTarget.Local -> target.file.absolutePath
                is DetailTarget.Network -> target.url
            }
            val sizeStr = rememberDetailSize(target)

            // 预览图
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.cardBackground),
                contentAlignment = Alignment.Center
            ) {
                AnimatedImage(source, Modifier.fillMaxSize(), ContentScale.Fit)
            }

            Spacer(Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("大小", fontSize = 13.sp, color = colors.textSecondary)
                Spacer(Modifier.padding(start = 12.dp))
                Text(sizeStr, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (isNetwork) "链接" else "路径", fontSize = 13.sp, color = colors.textSecondary)
                Spacer(Modifier.padding(start = 12.dp))
                Text(
                    source,
                    Modifier
                        .weight(1f)
                        .combinedClickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onCopy(source) }
                        ),
                    fontSize = 12.sp,
                    color = colors.accentBlue,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(4.dp))
            Text("点击链接可复制", fontSize = 10.sp, color = colors.textSecondary)

            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (isNetwork) {
                    DetailButton("下载", colors.accentGreen, Modifier.weight(1f)) { onDownload(source) }
                } else {
                    DetailButton("删除", colors.accentRed, Modifier.weight(1f), onClick = onDelete)
                }
                DetailButton("发送", colors.accentBlue, Modifier.weight(1f)) { onSend(source) }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AudioDetailDialog(
    type: String,
    target: DetailTarget,
    onCopy: (String) -> Unit,
    onSend: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = QEdgeTheme.colors

    val isNetwork = target is DetailTarget.Network
    val source = when (target) {
        is DetailTarget.Local -> target.file.absolutePath
        is DetailTarget.Network -> target.url
    }
    val fileName = when (target) {
        is DetailTarget.Local -> target.file.name
        is DetailTarget.Network -> target.url.substringAfterLast('/')
    }
    val sizeStr = when (target) {
        is DetailTarget.Local -> formatSize(target.file.length())
        is DetailTarget.Network -> "在线资源"
    }

    // 语音播放器（视频不播放，无解码库）
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    var playing by remember { mutableStateOf(false) }

    fun releasePlayer() {
        player?.let { p ->
            try { if (p.isPlaying) p.stop() } catch (e: Throwable) {}
            try { p.release() } catch (e: Throwable) {}
        }
        player = null
        playing = false
    }

    DisposableEffect(Unit) {
        onDispose { releasePlayer() }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.4f))
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier
                .fillMaxWidth(0.85f)
                .clip(RoundedCornerShape(20.dp))
                .background(colors.background)
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
                .padding(20.dp)
        ) {
            Text(
                fileName,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("大小", fontSize = 13.sp, color = colors.textSecondary)
                Spacer(Modifier.padding(start = 12.dp))
                Text(sizeStr, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (isNetwork) "链接" else "路径", fontSize = 13.sp, color = colors.textSecondary)
                Spacer(Modifier.padding(start = 12.dp))
                Text(
                    source,
                    Modifier
                        .weight(1f)
                        .combinedClickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onCopy(source) }
                        ),
                    fontSize = 12.sp,
                    color = colors.accentBlue,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(4.dp))
            Text("点击链接可复制", fontSize = 10.sp, color = colors.textSecondary)

            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (type == "voice") {
                    DetailButton(if (playing) "暂停" else "播放", colors.accentGreen, Modifier.weight(1f)) {
                        if (playing) {
                            try { player?.pause() } catch (e: Throwable) {}
                            playing = false
                        } else {
                            val p = MediaPlayer()
                            try {
                                p.setDataSource(source)
                                p.setOnPreparedListener { mp -> mp.start() }
                                p.setOnCompletionListener { mp ->
                                    try { mp.release() } catch (e: Throwable) {}
                                    player = null
                                    playing = false
                                }
                                p.setOnErrorListener { mp, _, _ ->
                                    try { mp.release() } catch (e: Throwable) {}
                                    player = null
                                    playing = false
                                    Toasts.toast("播放失败")
                                    true
                                }
                                p.prepareAsync()
                                player = p
                                playing = true
                            } catch (e: Throwable) {
                                try { p.release() } catch (e: Throwable) {}
                                Toasts.toast("播放失败: " + e.message)
                            }
                        }
                    }
                }
                DetailButton("发送", colors.accentBlue, Modifier.weight(1f)) { onSend(source) }
            }
        }
    }
}

@Composable
private fun DetailButton(text: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color.White)
    }
}

@Composable
private fun rememberDetailSize(target: DetailTarget): String {
    var result by remember(target) { mutableStateOf("计算中…") }
    LaunchedEffect(target) {
        result = withContext(Dispatchers.IO) {
            try {
                val path = when (target) {
                    is DetailTarget.Local -> target.file.absolutePath
                    is DetailTarget.Network -> MediaPanelLoader.MediaImageCache.load(target.url)
                }
                if (path == null || path.isEmpty()) "未知" else formatSize(File(path).length())
            } catch (e: Throwable) {
                "未知"
            }
        }
    }
    return result
}

private fun listGalleryFiles(): List<File> {
    return try {
        val dir = File(MediaPanelLoader.MediaImageCache.galleryDir())
        (dir.listFiles() ?: emptyArray())
            .filter { it.isFile }
            .sortedByDescending { it.lastModified() }
    } catch (e: Throwable) {
        emptyList()
    }
}

private fun formatSize(bytes: Long): String {
    if (bytes <= 0) return "未知"
    return when {
        bytes >= 1024 * 1024 -> String.format("%.2f MB", bytes / 1024.0 / 1024.0)
        bytes >= 1024 -> String.format("%.2f KB", bytes / 1024.0)
        else -> "$bytes B"
    }
}

@Composable
private fun AnimatedImage(target: String?, modifier: Modifier = Modifier, contentScale: ContentScale = ContentScale.Crop) {
    if (target.isNullOrEmpty()) return
    // 统一解析为本地文件路径（网络图先走缓存下载）
    var localPath by remember(target) { mutableStateOf<String?>(null) }
    LaunchedEffect(target) {
        localPath = withContext(Dispatchers.IO) {
            try {
                if (target.startsWith("http")) MediaPanelLoader.MediaImageCache.load(target) else target
            } catch (e: Throwable) {
                LogUtils.e("MediaPanelContent", "load failed: " + e.message)
                null
            }
        }
    }
    val path = localPath
    if (path.isNullOrEmpty()) return

    if (isGif(path)) {
        val drawable = remember(path) { decodeAnimated(path) }
        if (drawable != null) {
            AndroidView(
                modifier = modifier,
                factory = { context ->
                    ImageView(context).apply {
                        scaleType = when (contentScale) {
                            ContentScale.FillBounds -> ImageView.ScaleType.FIT_XY
                            ContentScale.Fit -> ImageView.ScaleType.FIT_CENTER
                            else -> ImageView.ScaleType.CENTER_CROP
                        }
                        setImageDrawable(drawable)
                    }
                }
            )
        }
    } else {
        val bmp = rememberBitmap(target)
        if (bmp != null) {
            Image(bmp, null, modifier, contentScale = contentScale)
        }
    }
}

private fun decodeAnimated(path: String): android.graphics.drawable.Drawable? {
    return try {
        val source = ImageDecoder.createSource(File(path))
        val drawable = ImageDecoder.decodeDrawable(source)
        if (drawable is AnimatedImageDrawable) {
            drawable.start()
        }
        drawable
    } catch (e: Throwable) {
        LogUtils.e("MediaPanelContent", "gif decode failed: " + e.message)
        null
    }
}

/** 按文件头魔数判断是否 GIF（缓存文件统一 .img 后缀，不能靠扩展名判断） */
private fun isGif(path: String): Boolean {
    return try {
        File(path).inputStream().use { input ->
            val head = ByteArray(3)
            val n = input.read(head)
            n == 3 && head[0] == 'G'.code.toByte() && head[1] == 'I'.code.toByte() && head[2] == 'F'.code.toByte()
        }
    } catch (e: Throwable) {
        false
    }
}

@Composable
private fun rememberBitmap(target: String?): ImageBitmap? {
    if (target.isNullOrEmpty()) return null
    var bitmap by remember(target) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(target) {
        bitmap = withContext(Dispatchers.IO) {
            try {
                val path = if (target.startsWith("http")) {
                    MediaPanelLoader.MediaImageCache.load(target)
                } else {
                    target
                }
                if (path.isNullOrEmpty()) null
                else decodeSampledBitmap(path, 400, 400)?.asImageBitmap()
            } catch (e: Throwable) {
                LogUtils.e("MediaPanelContent", "decode failed: " + e.message)
                null
            }
        }
    }
    return bitmap
}

private fun decodeSampledBitmap(path: String, reqW: Int, reqH: Int): android.graphics.Bitmap? {
    return try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        var sample = 1
        while (bounds.outWidth / sample > reqW * 2 || bounds.outHeight / sample > reqH * 2) {
            sample *= 2
        }
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        BitmapFactory.decodeFile(path, opts)
    } catch (e: Throwable) {
        null
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun UploadTagDialog(
    count: Int,
    onConfirm: (String, List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = QEdgeTheme.colors
    var collectionText by remember { mutableStateOf("") }
    var tagText by remember { mutableStateOf("") }

    Box(
        Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.4f))
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier
                .fillMaxWidth(0.85f)
                .clip(RoundedCornerShape(20.dp))
                .background(colors.background)
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
                .padding(20.dp)
        ) {
            Text("填写合集与标签", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
            Spacer(Modifier.height(6.dp))
            Text(
                "合集名必填（如：月薪喵）；标签可选，逗号分隔，用于搜索",
                fontSize = 12.sp,
                color = colors.textSecondary
            )
            Spacer(Modifier.height(14.dp))

            Text("合集名称", fontSize = 13.sp, color = colors.textSecondary)
            Spacer(Modifier.height(6.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.cardBackground)
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                BasicTextField(
                    value = collectionText,
                    onValueChange = { collectionText = it },
                    textStyle = TextStyle(fontSize = 14.sp, color = colors.textPrimary),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    decorationBox = { inner ->
                        if (collectionText.isEmpty()) {
                            Text("合集名称", fontSize = 14.sp, color = colors.textSecondary)
                        }
                        inner()
                    }
                )
            }

            Spacer(Modifier.height(12.dp))
            Text("标签（可选）", fontSize = 13.sp, color = colors.textSecondary)
            Spacer(Modifier.height(6.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.cardBackground)
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                BasicTextField(
                    value = tagText,
                    onValueChange = { tagText = it },
                    textStyle = TextStyle(fontSize = 14.sp, color = colors.textPrimary),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    decorationBox = { inner ->
                        if (tagText.isEmpty()) {
                            Text("标签，英文逗号分隔", fontSize = 14.sp, color = colors.textSecondary)
                        }
                        inner()
                    }
                )
            }

            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DetailButton("取消", colors.textSecondary, Modifier.weight(1f)) { onDismiss() }
                DetailButton("确定上传 ($count)", colors.accentBlue, Modifier.weight(1f)) {
                    val name = collectionText.trim()
                    if (name.isEmpty()) {
                        Toasts.toast("请填写合集名称")
                    } else {
                        val tags = tagText
                            .split(',', '，', '、', ';', '；', ' ')
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                        onConfirm(name, tags)
                    }
                }
            }
        }
    }
}