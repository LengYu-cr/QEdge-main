package me.lengyu.qedge.ui.pages.home

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
import androidx.compose.runtime.compositionLocalOf
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
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
import me.lengyu.qedge.hook.annotation.HookItemAnnotation
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

/** 首页搜索关键词：空串表示未搜索，由 HomeSettingsPage 顶部的搜索框提供 */
internal val LocalHomeSearchQuery = compositionLocalOf { "" }

/** 当前卡片的可搜索文本（标题 + 副标题）：行过滤时该文本命中则整卡所有行都显示 */
internal val LocalHomeCardSearchText = compositionLocalOf { "" }

/** 关键词是否命中：未搜索时全部命中，搜索时忽略大小写做包含匹配 */
internal fun searchHit(query: String, vararg texts: String): Boolean {
    if (query.isBlank()) return true
    val q = query.trim()
    return texts.any { it.contains(q, ignoreCase = true) }
}

/** hook/item 下全部 hook 项类名：UI 进程没有 HookRegistry（仅宿主进程填充），改按类名反射读注解 */
private val hookItemClassNames: List<String> = listOf(
    "me.lengyu.qedge.hook.item.AntiPokeDelay",
    "me.lengyu.qedge.hook.item.AntiQfixPatch",
    "me.lengyu.qedge.hook.item.AntiReport",
    "me.lengyu.qedge.hook.item.AutoLikeBack",
    "me.lengyu.qedge.hook.item.BypassProfileBan",
    "me.lengyu.qedge.hook.item.CopyArkMessage",
    "me.lengyu.qedge.hook.item.DisableAIAvatar",
    "me.lengyu.qedge.hook.item.DisableSecCheck",
    "me.lengyu.qedge.hook.item.DisableWebSecurityCheck",
    "me.lengyu.qedge.hook.item.DownloadEmotion",
    "me.lengyu.qedge.hook.item.EmotionAiTag",
    "me.lengyu.qedge.hook.item.FlashPicBypass",
    "me.lengyu.qedge.hook.item.ForceFullScreenBtnShow",
    "me.lengyu.qedge.hook.item.ForceInputNoLimit",
    "me.lengyu.qedge.hook.item.ForceModuleToast",
    "me.lengyu.qedge.hook.item.ForceSpeaker",
    "me.lengyu.qedge.hook.item.ForceVip",
    "me.lengyu.qedge.hook.item.ImageRatioOverride",
    "me.lengyu.qedge.hook.item.ImageSummary",
    "me.lengyu.qedge.hook.item.KeepAliveHook",
    "me.lengyu.qedge.hook.item.LevelBoost",
    "me.lengyu.qedge.hook.item.LongClickSendCard",
    "me.lengyu.qedge.hook.item.PreventRecall",
    "me.lengyu.qedge.hook.item.QLogRedirect",
    "me.lengyu.qedge.hook.item.QZoneLikeTool",
    "me.lengyu.qedge.hook.item.RemoveAds",
    "me.lengyu.qedge.hook.item.RemoveLinkInfo",
    "me.lengyu.qedge.hook.item.RemoveQrCodeCheck",
    "me.lengyu.qedge.hook.item.RemoveRiskWebpageBlock",
    "me.lengyu.qedge.hook.item.RepeatMsg",
    "me.lengyu.qedge.hook.item.SkipScanWaitTime",
    "me.lengyu.qedge.hook.item.SplitScreenScan",
    "me.lengyu.qedge.hook.item.TimArkCardBypass",
    "me.lengyu.qedge.hook.item.TransparentAvatar",
    "me.lengyu.qedge.hook.item.VideoToBubble",
    "me.lengyu.qedge.hook.item.VoiceSpeed"
)

/** 注解检索文本 */
private class HookAnnotationText(val value: String, val tag: String, val desc: String)

/**
 * 注解索引：lazy 反射构建一次。
 * initialize=false 只读注解不跑类初始化；任何失败（类缺失等）静默降级，搜索退回静态表行为。
 */
private val hookAnnotationIndex: List<HookAnnotationText> by lazy {
    val loader = HookItemAnnotation::class.java.classLoader
    hookItemClassNames.mapNotNull { name ->
        try {
            val ann = Class.forName(name, false, loader)
                ?.getAnnotation(HookItemAnnotation::class.java) ?: return@mapNotNull null
            HookAnnotationText(ann.value, ann.tag, ann.desc)
        } catch (t: Throwable) {
            null
        }
    }
}

/**
 * 注解层命中：行标题与注解 value/tag 关联（双向包含，兼容 UI 行标题与注解 value 的措辞差异，
 * 如"语音强制免提" vs "强制免提"）或注解 desc 含行标题时，查询词命中注解文本也算命中。
 */
internal fun annotationSearchHit(query: String, rowTitle: String): Boolean {
    if (query.isBlank() || rowTitle.isBlank()) return false
    val q = query.trim()
    return hookAnnotationIndex.any { a ->
        val related = (a.value.isNotEmpty() &&
            (rowTitle.contains(a.value, true) || a.value.contains(rowTitle, true))) ||
            (a.tag.isNotEmpty() &&
                (rowTitle.contains(a.tag, true) || a.tag.contains(rowTitle, true))) ||
            a.desc.contains(rowTitle, true)
        related && (a.value + a.tag + a.desc).contains(q, ignoreCase = true)
    }
}

/** 搜索态下某张卡片是否显示：卡片标题/副标题、任一功能行标题，或行关联注解的 value/tag/desc 命中 */
internal fun isCardVisibleForSearch(query: String, key: String): Boolean {
    val rows = homeCardRowTitles(key)
    if (searchHit(query, homeCardSearchText(key), *rows.toTypedArray())) return true
    // 仅注解命中时放行整卡，卡内各行再按各自谓词过滤，只留真正命中的行
    return rows.any { annotationSearchHit(query, it) }
}

/** 搜索态下是否命中任何卡片：用于空结果提示 */
internal fun anyCardVisibleForSearch(query: String): Boolean =
    homeCardKeys().any { isCardVisibleForSearch(query, it) }

/** 搜索态下的行间距：不搜索时才占位，避免被过滤掉的行留下空档 */
@Composable
internal fun SettingGap(height: Int) {
    if (LocalHomeSearchQuery.current.isBlank()) {
        Spacer(modifier = Modifier.height(height.dp))
    }
}

/** 搜索态下给行自身补的垂直内边距：行间距收拢后靠它撑开，避免多行贴在一起 */
@Composable
internal fun Modifier.searchRowPadding(): Modifier {
    return if (LocalHomeSearchQuery.current.isNotBlank()) {
        this.padding(vertical = 6.dp)
    } else {
        this
    }
}

/** 搜索态下的分组分割线：不搜索时才显示，避免夹在被隐藏的行之间 */
@Composable
internal fun SettingGroupDivider() {
    if (LocalHomeSearchQuery.current.isBlank()) {
        HorizontalDivider(color = QEdgeTheme.colors.textSecondary.copy(0.08f))
    }
}

/** 首页搜索框：玻璃卡片 + 搜索图标 + 输入框，由 CollapsibleContent 从标题下方推出 */
@Composable
internal fun HomeSearchBar(query: String, onQueryChange: (String) -> Unit) {
    val colors = QEdgeTheme.colors
    QEdgeCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(44.dp),
        glass = true,
        animateContentSize = false
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painterResource(R.drawable.ic_search),
                null,
                Modifier.size(20.dp),
                colors.textSecondary
            )
            Spacer(modifier = Modifier.width(8.dp))
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(fontSize = 14.sp, color = colors.textPrimary),
                cursorBrush = SolidColor(colors.accentBlue),
                modifier = Modifier.weight(1f),
                decorationBox = { innerTextField ->
                    if (query.isEmpty()) {
                        Text("搜索功能…", fontSize = 14.sp, color = colors.textSecondary)
                    }
                    innerTextField()
                }
            )
        }
    }
}

/**
 * 可折叠内容区：用 expandVertically/shrinkVertically 做高度裁剪动画，
 * 配合淡入淡出。相比 animateContentSize 只测量一次目标尺寸，
 * 内容多时不会首帧卡顿，展开时有"自上而下循序展开"的观感。
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
internal fun CollapsibleContent(
    expanded: Boolean,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    AnimatedVisibility(
        visible = expanded,
        enter = fadeIn(animationSpec = tween(150)) + expandVertically(
            // 刚度高 → 起手就冲，快到终点才减速；阻尼 0.65 留一点过冲当回弹
            animationSpec = spring(dampingRatio = 0.65f, stiffness = 1300f),
            expandFrom = Alignment.Top
        ),
        exit = fadeOut(animationSpec = tween(110)) + shrinkVertically(
            // 收起要更干脆：高刚度 + 接近临界阻尼，快速收拢、末端减速
            animationSpec = spring(dampingRatio = 0.88f, stiffness = 1500f),
            shrinkTowards = Alignment.Top
        ),
        content = { Column(content = content) }
    )
}

/**
 * 手风琴卡片的可点击标题行：标题 + 副标题 + 右侧展开箭头。
 * 文案取自 HomeRowText 注解，title 为其中的常量 key，subtitle 传入时覆盖注解里的静态文案。
 * 点击切换卡片的展开/收起状态。
 */
@Composable
internal fun CardHeader(
    title: String,
    subtitle: String? = null,
    expanded: Boolean,
    onClick: () -> Unit
) {
    val colors = QEdgeTheme.colors
    val rowTitle = homeRowTitle(title)
    val rowSubtitle = homeRowSubtitle(title, subtitle)
    // 搜索态下卡片强制展开，标题行不再响应折叠
    val searching = LocalHomeSearchQuery.current.isNotBlank()
    // 复用所在卡片的按压源：按下标题行时整张卡片一起做按压回弹
    val fallbackSource = remember { MutableInteractionSource() }
    val pressSource = LocalCardPressSource.current ?: fallbackSource
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = pressSource,
                indication = null,
                enabled = !searching,
                onClick = onClick
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                rowTitle,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                rowSubtitle,
                fontSize = 13.sp,
                color = colors.textSecondary
            )
        }
        if (!searching) {
            // 展开箭头：展开时朝上，收起时朝下
            Text(
                text = if (expanded) "˄" else "˅",
                fontSize = 16.sp,
                color = colors.textSecondary.copy(alpha = 0.5f),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/** 开关设置行：文案取自 HomeRowText 注解，title 为常量 key，subtitle 传入时覆盖注解里的静态文案 */
@Composable
internal fun SettingSwitchItem(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onClick: (() -> Unit)? = null
) {
    val colors = QEdgeTheme.colors
    val rowTitle = homeRowTitle(title)
    val rowSubtitle = homeRowSubtitle(title, subtitle)
    // 搜索态：只渲染命中的行；卡片自身命中时整卡展开，全部行都显示
    if (!searchHit(LocalHomeSearchQuery.current, rowTitle, rowSubtitle, LocalHomeCardSearchText.current) &&
        !annotationSearchHit(LocalHomeSearchQuery.current, rowTitle)) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                ) else Modifier
            )
            .searchRowPadding(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                rowTitle,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                rowSubtitle,
                fontSize = 12.sp,
                color = colors.textSecondary
            )
        }
        QEdgeSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/** 纯点击设置行：文案取自 HomeRowText 注解，title 为常量 key，subtitle 传入时覆盖注解里的静态文案 */
@Composable
internal fun SettingClickItem(
    title: String,
    subtitle: String? = null,
    enabled: Boolean = true,
    subtitleColor: Color? = null,
    onClick: () -> Unit
) {
    val colors = QEdgeTheme.colors
    val rowTitle = homeRowTitle(title)
    val rowSubtitle = homeRowSubtitle(title, subtitle)
    // 搜索态：只渲染命中的行；卡片自身命中时整卡展开，全部行都显示
    if (!searchHit(LocalHomeSearchQuery.current, rowTitle, rowSubtitle, LocalHomeCardSearchText.current) &&
        !annotationSearchHit(LocalHomeSearchQuery.current, rowTitle)) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .searchRowPadding(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                rowTitle,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = if (enabled) colors.textPrimary else colors.textSecondary.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                rowSubtitle,
                fontSize = 12.sp,
                color = subtitleColor
                    ?: colors.textSecondary.copy(alpha = if (enabled) 1f else 0.6f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            "›",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = colors.textSecondary.copy(alpha = 0.5f)
        )
    }
}

@Composable
internal fun UpdateLogCard() {
    val colors = QEdgeTheme.colors
    var logText by remember { mutableStateOf("加载中...") }

    LaunchedEffect(Unit) {
        Thread {
            try {
                val url = URL("https://v.yuafeng.cn/QEdge/update/changelog.php")
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.requestMethod = "GET"

                val reader = java.io.BufferedReader(java.io.InputStreamReader(connection.inputStream, "UTF-8"))
                val response = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }
                reader.close()

                val json = org.json.JSONObject(response.toString())
                if (json.getInt("code") == 200) {
                    val data = json.getJSONObject("data")
                    val changelog = data.getJSONArray("changelog")
                    val sb = StringBuilder()
                    for (i in 0 until changelog.length()) {
                        val entry = changelog.getJSONObject(i)
                        sb.append("v${entry.getString("version")} (${entry.getString("date")})\n")
                        val items = entry.getJSONArray("items")
                        for (j in 0 until items.length()) {
                            sb.append("• ${items.getString(j)}\n")
                        }
                        if (i < changelog.length() - 1) {
                            sb.append("\n")
                        }
                    }
                    logText = sb.toString()
                } else {
                    logText = "获取失败"
                }
            } catch (e: Exception) {
                logText = "获取失败: ${e.message}"
            }
        }.start()
    }

    QEdgeCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        glass = true
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                "更新日志",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    logText,
                    fontSize = 14.sp,
                    color = colors.textSecondary,
                    lineHeight = 20.sp
                )
            }
        }
    }
}
