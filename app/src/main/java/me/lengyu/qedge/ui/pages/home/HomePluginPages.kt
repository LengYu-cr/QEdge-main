package me.lengyu.qedge.ui.pages.home

import me.lengyu.qedge.ui.pages.PluginData

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.lengyu.qedge.common.ModuleScope
import me.lengyu.qedge.ui.components.atoms.ActionButton
import me.lengyu.qedge.ui.components.atoms.LocalCardPressSource
import me.lengyu.qedge.ui.components.atoms.QEdgeCard
import me.lengyu.qedge.ui.components.atoms.QEdgeSwitch
import me.lengyu.qedge.ui.components.molecules.AnimatedListItem
import me.lengyu.qedge.ui.components.molecules.EmptyStateView
import me.lengyu.qedge.ui.components.molecules.QEdgeTopBar
import me.lengyu.qedge.R
import me.lengyu.qedge.ui.components.molecules.TabItem
import me.lengyu.qedge.ui.pages.coldrain.ColdRainScreen
import me.lengyu.qedge.ui.core.theme.AccentBlue
import me.lengyu.qedge.ui.core.theme.AccentGreen
import me.lengyu.qedge.ui.core.theme.Dimens
import me.lengyu.qedge.ui.core.theme.QEdgeTheme
import me.lengyu.qedge.ui.widget.glass.GlassBackdropHost
import me.lengyu.qedge.ui.pages.home.HomeCommentInputDialog
import me.lengyu.qedge.ui.pages.home.HomeCreatePluginDialog
import me.lengyu.qedge.ui.pages.home.HomeImageSummaryDialog
import me.lengyu.qedge.ui.pages.home.HomeImageRatioDialog
import me.lengyu.qedge.ui.pages.home.HomeVoiceSpeedDialog
import me.lengyu.qedge.ui.pages.home.HomeQLogRedirectDialog
import me.lengyu.qedge.ui.pages.home.HomeMoodScheduleDialog
import me.lengyu.qedge.utils.HostInfo
import me.lengyu.qedge.utils.LogUtils
import me.lengyu.qedge.utils.ModuleConfig
import me.lengyu.qedge.plugin.view.ChatSettingLoader
import me.lengyu.qedge.plugin.view.MediaPanelLoader
import me.lengyu.qedge.hook.item.LevelBoost
import me.lengyu.qedge.hook.item.KeepAliveHook
import me.lengyu.qedge.hook.item.QLogRedirect
import me.lengyu.qedge.hook.UserData
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.Box as ComposeBox
import androidx.compose.foundation.Image
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.material3.CircularProgressIndicator
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

@OptIn(ExperimentalAnimationApi::class)
@Composable
internal fun JavaPluginsPage(
    plugins: List<PluginData>,
    onlinePlugins: List<OnlinePluginItem>,
    isLoading: Boolean,
    searchQuery: String,
    errorMessage: String,
    onRunToggle: (String, Boolean) -> Unit,
    onAutoLoadToggle: (String, Boolean) -> Unit,
    onDelete: (String) -> Unit,
    onReload: (String) -> Unit,
    onUploadClick: (PluginData) -> Unit,
    onSearchChange: (String) -> Unit,
    onRefresh: () -> Unit,
    onDownloadClick: (OnlinePluginItem) -> Unit
) {
    val colors = QEdgeTheme.colors
    var subTab by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = subTab,
            modifier = Modifier.weight(1f),
            transitionSpec = {
                val forward = targetState > initialState
                if (forward) {
                    slideInHorizontally(
                        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow),
                        initialOffsetX = { it / 2 }
                    ) + fadeIn(animationSpec = tween(150)) togetherWith
                    slideOutHorizontally(
                        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow),
                        targetOffsetX = { -it / 2 }
                    ) + fadeOut(animationSpec = tween(120))
                } else {
                    slideInHorizontally(
                        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow),
                        initialOffsetX = { -it / 2 }
                    ) + fadeIn(animationSpec = tween(150)) togetherWith
                    slideOutHorizontally(
                        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow),
                        targetOffsetX = { it / 2 }
                    ) + fadeOut(animationSpec = tween(120))
                }
            }
        ) { tab ->
            when (tab) {
                0 -> LocalPluginPage(
                    plugins,
                    onRunToggle,
                    onAutoLoadToggle,
                    onDelete,
                    onReload,
                    onUploadClick
                )
                1 -> OnlinePluginPage(
                    onlinePlugins,
                    isLoading,
                    searchQuery,
                    errorMessage,
                    onSearchChange,
                    onRefresh,
                    onDownloadClick,
                )
                else -> EmptyStateView(message = "")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        SubTabBar(
            selectedTab = subTab,
            onTabSelected = { subTab = it },
            localCount = plugins.size,
            onlineCount = onlinePlugins.size
        )

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
internal fun SubTabBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    localCount: Int,
    onlineCount: Int
) {
    val colors = QEdgeTheme.colors

    ComposeBox(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (colors.isDark) androidx.compose.ui.graphics.Color.White.copy(alpha = 0.05f)
                else androidx.compose.ui.graphics.Color.White.copy(alpha = 0.6f)
            )
            .padding(4.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            SubTabItem(
                text = "本地脚本",
                count = localCount,
                isSelected = selectedTab == 0,
                onClick = { onTabSelected(0) },
                modifier = Modifier.weight(1f)
            )
            SubTabItem(
                text = "在线脚本",
                count = onlineCount,
                isSelected = selectedTab == 1,
                onClick = { onTabSelected(1) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
internal fun SubTabItem(
    text: String,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = QEdgeTheme.colors

    ComposeBox(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isSelected) {
                    if (colors.isDark) androidx.compose.ui.graphics.Color.White.copy(alpha = 0.12f)
                    else androidx.compose.ui.graphics.Color.White.copy(alpha = 0.9f)
                } else {
                    androidx.compose.ui.graphics.Color.Transparent
                }
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = text,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) colors.textPrimary else colors.textSecondary
            )
            Spacer(modifier = Modifier.width(6.dp))
            ComposeBox(
                modifier = Modifier
                    .size(20.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isSelected) AccentGreen
                        else colors.textSecondary.copy(alpha = 0.12f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = count.toString(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) androidx.compose.ui.graphics.Color.White else colors.textSecondary
                )
            }
        }
    }
}

@Composable
internal fun LocalPluginPage(
    plugins: List<PluginData>,
    onRunToggle: (String, Boolean) -> Unit,
    onAutoLoadToggle: (String, Boolean) -> Unit,
    onDelete: (String) -> Unit,
    onReload: (String) -> Unit,
    onUpload: (PluginData) -> Unit
) {
    if (plugins.isEmpty()) {
        EmptyStateView(message = "暂无本地脚本")
    } else {
        LazyColumn(
            state = rememberLazyListState(),
            modifier = Modifier.fillMaxHeight(),
            contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, GlassBackdropHost.CONTENT_BOTTOM_DP.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(plugins, key = { _, plugin -> plugin.id }) { index, plugin ->
                AnimatedListItem(index) {
                    LocalPluginCard(
                        plugin = plugin,
                        onRunToggle = { onRunToggle(plugin.id, it) },
                        onAutoLoadToggle = { onAutoLoadToggle(plugin.id, it) },
                        onDelete = { onDelete(plugin.id) },
                        onReload = { onReload(plugin.id) },
                        onUpload = { onUpload(plugin) }
                    )
                }
            }
        }
    }
}

@Composable
internal fun OnlinePluginPage(
    plugins: List<OnlinePluginItem>,
    isLoading: Boolean,
    searchQuery: String,
    errorMessage: String,
    onSearchChange: (String) -> Unit,
    onRefresh: () -> Unit,
    onDownloadClick: (OnlinePluginItem) -> Unit,
) {
    val colors = QEdgeTheme.colors

    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            ComposeBox(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.cardBackground)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        androidx.compose.ui.res.painterResource(R.drawable.ic_search),
                        null,
                        Modifier.size(20.dp),
                        colors.textSecondary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = onSearchChange,
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, color = colors.textPrimary),
                        modifier = Modifier.weight(1f),
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text("搜索脚本...", fontSize = 14.sp, color = colors.textSecondary)
                            }
                            innerTextField()
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            QEdgeCard(modifier = Modifier.size(44.dp), animateContentSize = false, onClick = onRefresh) {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        androidx.compose.ui.res.painterResource(R.drawable.ic_refresh),
                        null,
                        Modifier.size(20.dp),
                        colors.textPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (isLoading) {
            EmptyStateView(message = "加载中...")
        } else if (errorMessage.isNotEmpty()) {
            EmptyStateView(message = errorMessage)
        } else if (plugins.isEmpty()) {
            EmptyStateView(message = "暂无在线脚本")
        } else {
            LazyColumn(
                state = rememberLazyListState(),
                modifier = Modifier.fillMaxHeight(),
                contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, GlassBackdropHost.CONTENT_BOTTOM_DP.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(plugins, key = { _, plugin -> plugin.id }) { index, plugin ->
                    AnimatedListItem(index) {
                        OnlinePluginCard(plugin = plugin, onDownload = { onDownloadClick(plugin) })
                    }
                }
            }
        }
    }
}

@Composable
internal fun OnlinePluginCard(plugin: OnlinePluginItem, onDownload: () -> Unit) {
    val colors = QEdgeTheme.colors
    var isExpanded by remember { mutableStateOf(false) }

    QEdgeCard(modifier = Modifier.fillMaxWidth(), glass = true) {
        Column(modifier = Modifier.padding(Dimens.PaddingMedium)) {
            OnlinePluginCardHeader(
                plugin.pluginName,
                plugin.versionCode,
                plugin.authorName,
                plugin.type
            ) { isExpanded = !isExpanded }

            CollapsibleContent(isExpanded) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = colors.textSecondary.copy(0.1f))
                Spacer(modifier = Modifier.height(12.dp))
                OnlinePluginCardDetails(
                    plugin.description,
                    plugin.uploadQq,
                    plugin.downloadCount,
                    plugin.uploadTime
                )
                Spacer(modifier = Modifier.height(Dimens.PaddingMedium))
                OnlinePluginCardActions(onDownload)
            }
        }
    }
}

@Composable
internal fun OnlinePluginCardHeader(
    name: String,
    version: String,
    author: String,
    type: String,
    onExpandToggle: () -> Unit
) {
    val colors = QEdgeTheme.colors
    val isJs = type == "js"
    val typeLabel = if (isJs) "JS" else "Java"
    val typeColor = if (isJs) Color(0xFFB8860B) else colors.accentBlue

    // 复用所在卡片的按压源：按下标题行时整张卡片一起做按压回弹
    val fallbackSource = remember { MutableInteractionSource() }
    val pressSource = LocalCardPressSource.current ?: fallbackSource

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = pressSource,
                indication = null,
                onClick = onExpandToggle
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(name, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .background(typeColor.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 1.dp)
                ) {
                    Text(typeLabel, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = typeColor)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "V$version • $author",
                fontSize = 12.sp,
                color = colors.textSecondary
            )
        }
        Icon(
            androidx.compose.ui.res.painterResource(R.drawable.ic_more_horiz),
            null,
            Modifier.size(20.dp),
            colors.textSecondary.copy(alpha = 0.4f)
        )
    }
}

@Composable
internal fun OnlinePluginCardDetails(
    description: String,
    uploadQq: String,
    downloadCount: Int,
    uploadTime: String
) {
    val colors = QEdgeTheme.colors

    Text(
        description.ifEmpty { "该作者很懒，什么也没留下" },
        fontSize = 13.sp,
        color = colors.textSecondary,
        lineHeight = 18.sp
    )
    Spacer(modifier = Modifier.height(Dimens.PaddingSmall))
    Text("上传者QQ: $uploadQq", fontSize = 13.sp, color = colors.textSecondary)
    Spacer(modifier = Modifier.height(Dimens.PaddingSmall))
    Text("下载量: $downloadCount", fontSize = 13.sp, color = colors.textSecondary)
    Spacer(modifier = Modifier.height(Dimens.PaddingSmall))
    Text(
        "上传时间: $uploadTime",
        fontSize = 13.sp,
        color = colors.textSecondary,
        lineHeight = 18.sp
    )
}

@Composable
internal fun OnlinePluginCardActions(onDownload: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        ActionButton("下载", onDownload, style = me.lengyu.qedge.ui.components.atoms.ActionButtonStyle.Success)
    }
}

data class OnlinePluginItem(
    val pluginId: String,
    val pluginName: String,
    val versionCode: String,
    val authorName: String,
    val uploadQq: String,
    val downloadCount: Int,
    val uploadTime: String,
    val id: Int,
    val description: String = "",
    val type: String = "java"
)

@Composable
internal fun LocalPluginCard(
    plugin: PluginData,
    onRunToggle: (Boolean) -> Unit,
    onAutoLoadToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onReload: () -> Unit,
    onUpload: () -> Unit
) {
    val colors = QEdgeTheme.colors
    var isExpanded by remember { mutableStateOf(false) }

    QEdgeCard(modifier = Modifier.fillMaxWidth(), glass = true) {
        Column(modifier = Modifier.padding(Dimens.PaddingMedium)) {
            PluginCardHeader(
                plugin.name,
                plugin.version,
                plugin.type,
                plugin.isRunning,
                onRunToggle
            ) { isExpanded = !isExpanded }

            CollapsibleContent(isExpanded) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = colors.textSecondary.copy(0.1f))
                Spacer(modifier = Modifier.height(12.dp))
                PluginCardDetails(
                    plugin.author,
                    plugin.description,
                    plugin.isAutoLoad,
                    onAutoLoadToggle
                )
                Spacer(modifier = Modifier.height(Dimens.PaddingMedium))
                PluginCardActions(onDelete, onReload, onUpload)
            }
        }
    }
}

@Composable
internal fun PluginCardHeader(
    name: String,
    version: String,
    type: String,
    isRunning: Boolean,
    onRunToggle: (Boolean) -> Unit,
    onExpandToggle: () -> Unit
) {
    val colors = QEdgeTheme.colors
    val isJs = type == "js"
    val typeLabel = if (isJs) "JS" else "Java"
    val typeColor = if (isJs) Color(0xFFB8860B) else colors.accentBlue

    // 复用所在卡片的按压源：按下标题行时整张卡片一起做按压回弹
    val fallbackSource = remember { MutableInteractionSource() }
    val pressSource = LocalCardPressSource.current ?: fallbackSource

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = pressSource,
                indication = null,
                onClick = onExpandToggle
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(name, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .background(typeColor.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 1.dp)
                ) {
                    Text(typeLabel, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = typeColor)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "V$version • ${if (isRunning) "运行中" else "未运行"}",
                fontSize = 12.sp,
                color = colors.textSecondary
            )
        }
        QEdgeSwitch(isRunning, onRunToggle)
    }
}

@Composable
internal fun PluginCardDetails(
    author: String,
    description: String,
    isAutoLoad: Boolean,
    onAutoLoadToggle: (Boolean) -> Unit
) {
    val colors = QEdgeTheme.colors

    Text("作者: $author", fontSize = 13.sp, color = colors.textSecondary)
    Spacer(modifier = Modifier.height(Dimens.PaddingSmall))
    Text(
        description.ifEmpty { "暂无描述" },
        fontSize = 13.sp,
        color = colors.textSecondary,
        lineHeight = 18.sp
    )
    Spacer(modifier = Modifier.height(12.dp))

    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            "QQ启动时自动加载",
            fontSize = 14.sp,
            color = colors.textPrimary,
            modifier = Modifier.weight(1f)
        )
        QEdgeSwitch(isAutoLoad, onAutoLoadToggle)
    }
}

@Composable
internal fun PluginCardActions(onDelete: () -> Unit, onReload: () -> Unit, onUpload: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        ActionButton("删除", onDelete, style = me.lengyu.qedge.ui.components.atoms.ActionButtonStyle.Danger)
        Spacer(modifier = Modifier.width(Dimens.PaddingSmall))
        ActionButton("重载", onReload, style = me.lengyu.qedge.ui.components.atoms.ActionButtonStyle.Primary)
        Spacer(modifier = Modifier.width(Dimens.PaddingSmall))
        ActionButton("上传", onUpload, style = me.lengyu.qedge.ui.components.atoms.ActionButtonStyle.Success)
    }
}
