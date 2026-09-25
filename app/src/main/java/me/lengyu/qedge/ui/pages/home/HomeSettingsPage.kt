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

@Composable
internal fun HomePage(
    state: HomePageState,
    callbacks: HomePageCallbacks
) {
    val colors = QEdgeTheme.colors
    // 手风琴：当前展开的卡片 key，null 表示全部收起
    var expandedCard by remember { mutableStateOf<String?>(null) }
    val toggleCard: (String) -> Unit = { key ->
        expandedCard = if (expandedCard == key) null else key
    }

    // 用 LazyColumn 替代 Column(verticalScroll)：首帧只组合可见的卡片，
    // 屏幕外的卡片滚动到才构建，避免首次进入时一次性布局全部卡片导致的卡顿。
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = GlassBackdropHost.CONTENT_BOTTOM_DP.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "hangup") {
            HangupEntryCard()
        }

        item(key = "card_qzone") {
        val expanded = expandedCard == "card_qzone"
        QEdgeCard(modifier = Modifier.fillMaxWidth(), glass = true) {
            Column(modifier = Modifier.padding(20.dp)) {
                CardHeader(
                    title = "QQ空间",
                    subtitle = "自动点赞、自动评论",
                    expanded = expanded,
                    onClick = { toggleCard("card_qzone") }
                )

                CollapsibleContent(expanded) {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = colors.textSecondary.copy(0.08f))
                    Spacer(modifier = Modifier.height(16.dp))

                    SettingSwitchItem(
                        title = "空间秒赞",
                        subtitle = "收到好友动态自动点赞(确保在前台运行)",
                        checked = state.qzoneAutoLike,
                        onCheckedChange = callbacks.onLikeToggle
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    SettingSwitchItem(
                        title = "空间秒评",
                        subtitle = state.commentText,
                        checked = state.qzoneAutoComment,
                        onCheckedChange = callbacks.onCommentToggle,
                        onClick = callbacks.onCommentTextClick
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    SettingSwitchItem(
                        title = "定时发说说 +0.5天",
                        subtitle = run {
                            val preview = if (state.moodText.length > 18) state.moodText.take(18) + "…" else state.moodText
                            "${state.moodTime} · $preview"
                        },
                        checked = state.moodEnabled,
                        onCheckedChange = callbacks.onMoodToggle,
                        onClick = callbacks.onMoodConfigClick
                    )
                }
            }
        }
        }

        item(key = "card_chat") {
        val expanded = expandedCard == "card_chat"
        QEdgeCard(modifier = Modifier.fillMaxWidth(), glass = true) {
            Column(modifier = Modifier.padding(20.dp)) {
                CardHeader(
                    title = "聊天功能",
                    subtitle = "闪照破解、视频转泡泡等",
                    expanded = expanded,
                    onClick = { toggleCard("card_chat") }
                )

                CollapsibleContent(expanded) {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = colors.textSecondary.copy(0.08f))
                    Spacer(modifier = Modifier.height(16.dp))

                    SettingSwitchItem(
                        title = "闪照破解",
                        subtitle = "闪照直接查看，无需长按",
                        checked = state.flashPicBypass,
                        onCheckedChange = callbacks.onFlashPicToggle
                    )

                Spacer(modifier = Modifier.height(12.dp))

                // 存储路径固定不变，缓存避免每次重组都走一次 getExternalStorageDirectory
                val emotionSavePath = remember {
                    android.os.Environment.getExternalStorageDirectory().absolutePath + "/Download/QQ/QEdge/"
                }
                val copyCtx = androidx.compose.ui.platform.LocalContext.current

                SettingSwitchItem(
                    title = "表情/泡泡/视频/语音下载",
                    subtitle = "保存至 " + emotionSavePath + "，点击复制",
                    checked = state.downloadEmotion,
                    onCheckedChange = callbacks.onDownloadEmotionToggle,
                    onClick = {
                        try {
                            val cm =
                                copyCtx.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            cm.setPrimaryClip(android.content.ClipData.newPlainText("path", emotionSavePath))
                            android.widget.Toast.makeText(copyCtx, "已复制保存路径", android.widget.Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            LogUtils.e("HomeScreen", "copy save path error: ${e.message}")
                        }
                    }
                )

                SettingSwitchItem(
                    title = "屏蔽链接信息卡片",
                    subtitle = "收到链接时，自动屏蔽",
                    checked = state.removeLinkInfo,
                    onCheckedChange = callbacks.onRemoveLinkInfoToggle
                )

                if (HostInfo.isQQ) {
                    Spacer(modifier = Modifier.height(12.dp))

                    SettingSwitchItem(
                        title = "视频转泡泡消息",
                        subtitle = "发送视频时，自动替换为泡泡",
                        checked = state.videoToBubble,
                        onCheckedChange = callbacks.onVideoToBubbleToggle
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                SettingSwitchItem(
                    title = "取消拍一拍时间限制",
                    subtitle = "解除拍一拍时间限制",
                    checked = state.antiPokeDelay,
                    onCheckedChange = callbacks.onAntiPokeDelayToggle
                )

                Spacer(modifier = Modifier.height(12.dp))

                SettingSwitchItem(
                    title = "防撤回",
                    subtitle = "拦截QQ消息撤回，已撤回的消息依然可见",
                    checked = state.preventRecall,
                    onCheckedChange = callbacks.onPreventRecallToggle
                )

                Spacer(modifier = Modifier.height(12.dp))

                SettingSwitchItem(
                    title = "复制卡片消息",
                    subtitle = "在卡片上方显示长按复制按钮，长按复制JSON",
                    checked = state.copyArkMessage,
                    onCheckedChange = callbacks.onCopyArkMessageToggle
                )

                Spacer(modifier = Modifier.height(12.dp))

                SettingSwitchItem(
                    title = "长按发送发卡片",
                    subtitle = "长按发送按钮将输入框内JSON作为卡片消息发送",
                    checked = state.longClickSendCard,
                    onCheckedChange = callbacks.onLongClickSendCardToggle
                )

                Spacer(modifier = Modifier.height(12.dp))

                SettingSwitchItem(
                    title = "消息复读",
                    subtitle = "点击复读，长按可复制链接、查看原始消息",
                    checked = state.repeatMsg,
                    onCheckedChange = callbacks.onRepeatMsgToggle
                )

                Spacer(modifier = Modifier.height(12.dp))

                SettingSwitchItem(
                    title = "AI表情标签",
                    subtitle = "发送纯表情包时自动带上AI表情标签",
                    checked = state.emotionAiTag,
                    onCheckedChange = callbacks.onEmotionAiTagToggle
                )

                Spacer(modifier = Modifier.height(12.dp))

                SettingSwitchItem(
                    title = "篡改发送图片比例",
                    subtitle = run {
                        val w = state.imageRatioWidth.toIntOrNull() ?: 0
                        val h = state.imageRatioHeight.toIntOrNull() ?: 0
                        if (w > 0 && h > 0) "宽 ${w}px × 高 ${h}px，点击修改" else "未设置宽高，点击配置"
                    },
                    checked = state.imageRatioEnabled,
                    onCheckedChange = callbacks.onImageRatioToggle,
                    onClick = callbacks.onImageRatioConfigClick
                )

                Spacer(modifier = Modifier.height(12.dp))

                SettingSwitchItem(
                    title = "语音消息倍速播放",
                    subtitle = run {
                        val v = state.voiceSpeedValue.toFloatOrNull() ?: 1.5f
                        "播放倍速 $v x，点击修改"
                    },
                    checked = state.voiceSpeedEnabled,
                    onCheckedChange = callbacks.onVoiceSpeedToggle,
                    onClick = callbacks.onVoiceSpeedConfigClick
                )

                Spacer(modifier = Modifier.height(12.dp))

                SettingSwitchItem(
                    title = "语音强制免提",
                    subtitle = "语音消息强制扬声器播放，不走听筒",
                    checked = state.forceSpeakerEnabled,
                    onCheckedChange = callbacks.onForceSpeakerToggle
                )

                Spacer(modifier = Modifier.height(12.dp))

                SettingSwitchItem(
                    title = "图片外显自定义",
                    subtitle = run {
                        val modeDesc = if (state.imageSummaryMode == "http") {
                            if (state.imageSummaryFormat == "json") "接口返回(JSON)" else "接口返回(文本)"
                        } else "随机文案"
                        val preview = if (state.imageSummaryMode == "http") {
                            if (state.imageSummaryUrl.isNotEmpty()) state.imageSummaryUrl else "未设置接口"
                        } else {
                            if (state.imageSummaryTips.isNotEmpty()) state.imageSummaryTips.take(18) + "…" else "未设置文案"
                        }
                        "$modeDesc · $preview"
                    },
                    checked = state.imageSummaryEnabled,
                    onCheckedChange = callbacks.onImageSummaryToggle,
                    onClick = callbacks.onImageSummaryConfigClick
                )

                if (HostInfo.isTIM) {
                    Spacer(modifier = Modifier.height(12.dp))
                    SettingSwitchItem(
                        title = "TIM卡片阻断绕过",
                        subtitle = "解除低版本TIM对Ark卡片跳转的限制",
                        checked = state.timArkCardBypass,
                        onCheckedChange = callbacks.onTimArkCardBypassToggle
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = colors.textSecondary.copy(0.08f))
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    "聊天页脚本菜单入口",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    "长按聊天页对应按钮打开脚本菜单（重启QQ生效）",
                    fontSize = 12.sp,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(12.dp))

                val chunked = remember { ChatSettingLoader.ENTRY_OPTIONS.entries.toList().chunked(4) }
                for (row in chunked) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for ((key, label) in row) {
                            val selected = state.chatSettingEntry == key
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (selected) AccentBlue else colors.background
                                    )
                                    .clickable { callbacks.onChatSettingEntryChange(key) }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    label,
                                    fontSize = 13.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selected) Color.White else colors.textPrimary
                                )
                            }
                        }
                        // 补齐空位
                        repeat(4 - row.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = colors.textSecondary.copy(0.08f))
                Spacer(modifier = Modifier.height(16.dp))
                SettingSwitchItem(
                    title = "综合面板（表情/语音/视频）",
                    subtitle = "打开后长按聊天页对应按钮打开综合面板",
                    checked = state.mediaPanelEnabled,
                    onCheckedChange = callbacks.onMediaPanelToggle
                )
                Spacer(modifier = Modifier.height(12.dp))
                // 入口选择区：开关关闭时禁用（按钮状态与开关联动刷新）
                val mediaEnabled = state.mediaPanelEnabled
                Text(
                    "综合面板入口",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (mediaEnabled) colors.textPrimary else colors.textSecondary.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(2.dp))
                // 主动判断两个入口是否重合：重合则红字警告，不重合显示各自入口名
                val scriptEntryLabel = ChatSettingLoader.ENTRY_OPTIONS[state.chatSettingEntry] ?: state.chatSettingEntry
                val mediaEntryLabel = MediaPanelLoader.ENTRY_OPTIONS[state.mediaPanelEntry] ?: state.mediaPanelEntry
                val entryConflict = state.chatSettingEntry == state.mediaPanelEntry
                Text(
                    if (entryConflict) "脚本菜单与综合面板入口均为「$mediaEntryLabel」，长按会冲突，请错开"
                    else "脚本菜单「$scriptEntryLabel」、面板「$mediaEntryLabel」，长按互不冲突",
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = if (entryConflict) colors.accentRed.copy(alpha = if (mediaEnabled) 1f else 0.5f)
                    else colors.textSecondary.copy(alpha = if (mediaEnabled) 1f else 0.5f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(12.dp))
                val mediaChunked = remember { MediaPanelLoader.ENTRY_OPTIONS.entries.toList().chunked(4) }
                for (mrow in mediaChunked) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for ((key, label) in mrow) {
                            val selected = state.mediaPanelEntry == key
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (selected) AccentBlue
                                        else colors.background.copy(alpha = if (mediaEnabled) 1f else 0.5f)
                                    )
                                    .clickable(enabled = mediaEnabled) { callbacks.onMediaPanelEntryChange(key) }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    label,
                                    fontSize = 13.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selected) Color.White
                                    else colors.textPrimary.copy(alpha = if (mediaEnabled) 1f else 0.5f)
                                )
                            }
                        }
                        repeat(4 - mrow.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                }
            }
        }
        }

        item(key = "card_profile") {
        val expanded = expandedCard == "card_profile"
        QEdgeCard(modifier = Modifier.fillMaxWidth(), glass = true) {
            Column(modifier = Modifier.padding(20.dp)) {
                CardHeader(
                    title = "资料卡",
                    subtitle = "上传透明头像等，名片回赞",
                    expanded = expanded,
                    onClick = { toggleCard("card_profile") }
                )

                CollapsibleContent(expanded) {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = colors.textSecondary.copy(0.08f))
                    Spacer(modifier = Modifier.height(16.dp))

                    SettingSwitchItem(
                        title = "半透明头像上传",
                        subtitle = "可上传(群)头像、名片等，不用则关",
                        checked = state.transparentAvatar,
                        onCheckedChange = callbacks.onTransparentAvatarToggle
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    SettingSwitchItem(
                        title = "名片自动回赞",
                        subtitle = "收到名片点赞自动回赞",
                        checked = state.profileAutoLikeBack,
                        onCheckedChange = callbacks.onProfileAutoLikeBackToggle
                    )
                }
            }
        }
        }

        item(key = "card_level") {
        val expanded = expandedCard == "card_level"
        QEdgeCard(modifier = Modifier.fillMaxWidth(), glass = true) {
            Column(modifier = Modifier.padding(20.dp)) {
                CardHeader(
                    title = "等级加速",
                    subtitle = "00:00时自动空间打卡，qq日签打卡，大会员签到，自动加好友",
                    expanded = expanded,
                    onClick = { toggleCard("card_level") }
                )

                CollapsibleContent(expanded) {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = colors.textSecondary.copy(0.08f))
                    Spacer(modifier = Modifier.height(16.dp))

                    SettingSwitchItem(
                        title = "空间等级签到",
                        subtitle = "自动执行空间打卡 +0.5天",
                        checked = state.qzoneCheckinEnabled,
                        onCheckedChange = callbacks.onCheckinToggle
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    SettingSwitchItem(
                        title = "QQ 日签打卡",
                        subtitle = "自动执行日签打卡 +0.5天",
                        checked = state.dailySignEnabled,
                        onCheckedChange = callbacks.onDailySignToggle
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    SettingSwitchItem(
                        title = "大会员签到",
                        subtitle = "自动执行（无需开通大会员） +0.5天",
                        checked = state.bigVipCheckinEnabled,
                        onCheckedChange = callbacks.onBigVipCheckinToggle
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    SettingSwitchItem(
                        title = "自动加好友",
                        subtitle = "自动添加3个好友 +1.5天",
                        checked = state.levelBoostEnabled,
                        onCheckedChange = callbacks.onLevelBoostToggle
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    SettingSwitchItem(
                        title = "空间浏览",
                        subtitle = "浏览好友说说10条 +0.5天",
                        checked = state.spaceBrowseEnabled,
                        onCheckedChange = callbacks.onSpaceBrowseToggle
                    )
                }
            }
        }
        }

        item(key = "card_keepalive") {
        val expanded = expandedCard == "card_keepalive"
        QEdgeCard(modifier = Modifier.fillMaxWidth(), glass = true) {
            Column(modifier = Modifier.padding(20.dp)) {
                CardHeader(
                    title = "应用保活",
                    subtitle = "应用保活，保持进程可见，可能会高耗电",
                    expanded = expanded,
                    onClick = { toggleCard("card_keepalive") }
                )

                CollapsibleContent(expanded) {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = colors.textSecondary.copy(0.08f))
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("透明悬浮窗", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("1x1透明悬浮窗，保持进程可见", fontSize = 12.sp, color = colors.textSecondary, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        }
                        QEdgeSwitch(checked = state.keepAlivePixel, onCheckedChange = callbacks.onKeepAlivePixelToggle)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("前台通知", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("高优先级常驻通知，最高保活优先级", fontSize = 12.sp, color = colors.textSecondary, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        }
                        QEdgeSwitch(checked = state.keepAliveForeground, onCheckedChange = callbacks.onKeepAliveForegroundToggle)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("后台通知", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("低优先级通知，轻量保活", fontSize = 12.sp, color = colors.textSecondary, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        }
                        QEdgeSwitch(checked = state.keepAliveBackground, onCheckedChange = callbacks.onKeepAliveBackgroundToggle)
                    }
                }
            }
        }
        }

        item(key = "card_system") {
        val expanded = expandedCard == "card_system"
        QEdgeCard(modifier = Modifier.fillMaxWidth(), glass = true) {
            Column(modifier = Modifier.padding(20.dp)) {
                CardHeader(
                    title = "基础配置",
                    subtitle = "禁用QQ修复补丁等系统级功能",
                    expanded = expanded,
                    onClick = { toggleCard("card_system") }
                )

                CollapsibleContent(expanded) {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = colors.textSecondary.copy(0.08f))
                    Spacer(modifier = Modifier.height(16.dp))

                    SettingSwitchItem(
                        title = "禁用QQ修复补丁",
                        subtitle = "拦截并禁用QQ的修复补丁机制",
                        checked = state.antiQfixPatch,
                        onCheckedChange = callbacks.onAntiQfixPatchToggle
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    SettingSwitchItem(
                        title = "禁用QQ日志上报",
                        subtitle = "拦截SSO上报并禁用QQ日志",
                        checked = state.antiReport,
                        onCheckedChange = callbacks.onAntiReportToggle
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    SettingSwitchItem(
                        title = "解锁本地会员",
                        subtitle = "强制本地QQ超级会员/VIP/SVIP，目前可用于开启QQ自带的自动语音转文字、解除表情包收藏500的限制、解除语音发送时长限制、解除每日文件上传限制，其他的自己去测试。会员不会在主页显示。",
                        checked = state.forceVip,
                        onCheckedChange = callbacks.onForceVipToggle
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    SettingSwitchItem(
                        title = "屏蔽QQ秀/AI头像",
                        subtitle = "屏蔽QQ秀与AI头像相关显示",
                        checked = state.disableAIAvatar,
                        onCheckedChange = callbacks.onDisableAIAvatarToggle
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    SettingSwitchItem(
                        title = "解除风险网页拦截",
                        subtitle = "点击消息中链接时不再拦截风险网页",
                        checked = state.removeRiskWebpage,
                        onCheckedChange = callbacks.onRemoveRiskWebpageToggle
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    SettingSwitchItem(
                        title = "拦截网页安全检测",
                        subtitle = "阻止WebView截图上传识别，跳过网页安全OCR检测",
                        checked = state.disableWebSecurityCheck,
                        onCheckedChange = callbacks.onDisableWebSecurityCheckToggle
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    SettingSwitchItem(
                        title = "拦截安全校验",
                        subtitle = "屏蔽重打包检测、签名校验与APK版本读取",
                        checked = state.disableSecCheck,
                        onCheckedChange = callbacks.onDisableSecCheckToggle
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    SettingSwitchItem(
                        title = "解除扫码限制",
                        subtitle = "解除长按识别或从相册中扫描二维码时的风险检查",
                        checked = state.removeQrCodeCheck,
                        onCheckedChange = callbacks.onRemoveQrCodeCheckToggle
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    SettingSwitchItem(
                        title = "跳过扫码确认等待时间",
                        subtitle = "忽略倒计时，扫码确认按钮可直接点击确认登录",
                        checked = state.skipScanWaitTime,
                        onCheckedChange = callbacks.onSkipScanWaitTimeToggle
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    SettingSwitchItem(
                        title = "绕过资料卡封禁",
                        subtitle = "强制显示被封禁用户的 QQ 资料卡主页，绕过封禁拦截弹窗",
                        checked = state.bypassProfileBan,
                        onCheckedChange = callbacks.onBypassProfileBanToggle
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    SettingSwitchItem(
                        title = "去页面内横幅广告",
                        subtitle = "清理QQ主界面顶部横幅广告等广告数据源",
                        checked = state.removeAds,
                        onCheckedChange = callbacks.onRemoveAdsToggle
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    SettingSwitchItem(
                        title = "QLog日志重定向/拦截",
                        subtitle = when (state.qlogRedirectMode) {
                            QLogRedirect.MODE_MUTE -> "纯拦截模式：QQ日志被直接丢弃，不写入本地文件，点击选择模式"
                            QLogRedirect.MODE_REDIRECT -> "重定向模式：QQ日志写入 QEdge/log/QLog/，点击选择模式"
                            else -> "未开启拦截，QQ日志正常输出，点击选择模式"
                        },
                        checked = state.qlogRedirectMode != QLogRedirect.MODE_OFF,
                        onCheckedChange = callbacks.onQLogRedirectToggle,
                        onClick = callbacks.onQLogRedirectModeClick
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    SettingSwitchItem(
                        title = "强制模块Toast",
                        subtitle = "接管QQ原生Toast，改用模块样式弹出提示",
                        checked = state.forceModuleToast,
                        onCheckedChange = callbacks.onForceModuleToastToggle
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    SettingSwitchItem(
                        title = "自定义背景图",
                        subtitle = when {
                            state.bgImageEnabled && state.bgImageUri.isNotEmpty() ->
                                "已选图片作为首页背景，点击重新选择"
                            state.bgImageUri.isNotEmpty() ->
                                "未开启，当前使用默认明暗主题，点击选择图片"
                            else ->
                                "开启后跳转相册选图作为背景，未选图时使用默认明暗主题"
                        },
                        checked = state.bgImageEnabled && state.bgImageUri.isNotEmpty(),
                        onCheckedChange = callbacks.onBgImageToggle,
                        onClick = callbacks.onBgImagePickClick
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
        }
    }
}
