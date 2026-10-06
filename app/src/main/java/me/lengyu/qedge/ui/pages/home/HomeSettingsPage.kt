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
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.lengyu.qedge.common.ModuleScope
import me.lengyu.qedge.ui.components.atoms.ActionButton
import me.lengyu.qedge.ui.components.atoms.QEdgeCard
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
import me.lengyu.qedge.utils.qq.QQCurrentEnv
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
    // 搜索态：非空即按关键词过滤卡片与功能行，卡片强制展开
    val searchQuery = LocalHomeSearchQuery.current
    val searching = searchQuery.isNotBlank()
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
        if (searching && !anyCardVisibleForSearch(searchQuery)) {
            item(key = "empty_search") {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("没有找到相关功能", fontSize = 15.sp, color = colors.textSecondary)
                }
            }
        }

        item(key = "hangup") {
            if (!isCardVisibleForSearch(searchQuery, "hangup")) return@item
            HangupEntryCard()
        }

        item(key = "card_qzone") {
        if (!isCardVisibleForSearch(searchQuery, "card_qzone")) return@item
        val expanded = searching || expandedCard == "card_qzone"
        CompositionLocalProvider(LocalHomeCardSearchText provides homeCardSearchText("card_qzone")) {
        QEdgeCard(modifier = Modifier.fillMaxWidth(), glass = true) {
            Column(modifier = Modifier.padding(20.dp)) {
                CardHeader(
                    title = HomeRowText.CARD_QZONE,
                    expanded = expanded,
                    onClick = { toggleCard("card_qzone") }
                )

                CollapsibleContent(expanded) {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = colors.textSecondary.copy(0.08f))
                    Spacer(modifier = Modifier.height(16.dp))

                    SettingSwitchItem(
                        title = HomeRowText.QZONE_AUTO_LIKE,
                        checked = state.qzoneAutoLike,
                        onCheckedChange = callbacks.onLikeToggle
                    )

                    SettingGap(12)

                    SettingSwitchItem(
                        title = HomeRowText.QZONE_AUTO_COMMENT,
                        subtitle = state.commentText,
                        checked = state.qzoneAutoComment,
                        onCheckedChange = callbacks.onCommentToggle,
                        onClick = callbacks.onCommentTextClick
                    )

                    SettingGap(12)

                    SettingSwitchItem(
                        title = HomeRowText.MOOD_SCHEDULE,
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
        }

        item(key = "card_chat") {
        if (!isCardVisibleForSearch(searchQuery, "card_chat")) return@item
        val expanded = searching || expandedCard == "card_chat"
        CompositionLocalProvider(LocalHomeCardSearchText provides homeCardSearchText("card_chat")) {
        QEdgeCard(modifier = Modifier.fillMaxWidth(), glass = true) {
            Column(modifier = Modifier.padding(20.dp)) {
                CardHeader(
                    title = HomeRowText.CARD_CHAT,
                    expanded = expanded,
                    onClick = { toggleCard("card_chat") }
                )

                CollapsibleContent(expanded) {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = colors.textSecondary.copy(0.08f))
                    Spacer(modifier = Modifier.height(16.dp))

                    SettingSwitchItem(
                        title = HomeRowText.FLASH_PIC_BYPASS,
                        checked = state.flashPicBypass,
                        onCheckedChange = callbacks.onFlashPicToggle
                    )

                SettingGap(12)

                // 与 DownloadEmotion / 媒体面板保持同源，避免硬编码路径漂移
                val emotionSavePath = remember { QQCurrentEnv.getMediaPath() + "QEdge/" }
                val copyCtx = androidx.compose.ui.platform.LocalContext.current

                SettingSwitchItem(
                    title = HomeRowText.EMOTION_DOWNLOAD,
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
                    title = HomeRowText.REMOVE_LINK_INFO,
                    checked = state.removeLinkInfo,
                    onCheckedChange = callbacks.onRemoveLinkInfoToggle
                )

                if (HostInfo.isQQ) {
                    SettingGap(12)

                    SettingSwitchItem(
                        title = HomeRowText.VIDEO_TO_BUBBLE,
                        checked = state.videoToBubble,
                        onCheckedChange = callbacks.onVideoToBubbleToggle
                    )
                }

                SettingGap(12)

                SettingSwitchItem(
                    title = HomeRowText.ANTI_POKE_DELAY,
                    checked = state.antiPokeDelay,
                    onCheckedChange = callbacks.onAntiPokeDelayToggle
                )

                SettingGap(12)

                SettingSwitchItem(
                    title = HomeRowText.PREVENT_RECALL,
                    checked = state.preventRecall,
                    onCheckedChange = callbacks.onPreventRecallToggle
                )

                SettingGap(12)

                SettingSwitchItem(
                    title = HomeRowText.COPY_ARK_MESSAGE,
                    checked = state.copyArkMessage,
                    onCheckedChange = callbacks.onCopyArkMessageToggle
                )

                SettingGap(12)

                SettingSwitchItem(
                    title = HomeRowText.LONG_CLICK_SEND_CARD,
                    checked = state.longClickSendCard,
                    onCheckedChange = callbacks.onLongClickSendCardToggle
                )

                SettingGap(12)

                SettingSwitchItem(
                    title = HomeRowText.REPEAT_MSG,
                    subtitle = if (state.repeatMsgIcon.isNotEmpty())
                        "已自定义按钮图标，点击更换（打开后不选图则恢复默认）"
                    else
                        "点击复读，长按可复制链接、查看原始消息 · 点击此行自定义按钮图标",
                    checked = state.repeatMsg,
                    onCheckedChange = callbacks.onRepeatMsgToggle,
                    onClick = callbacks.onRepeatMsgIconClick
                )

                SettingGap(12)

                SettingSwitchItem(
                    title = HomeRowText.EMOTION_AI_TAG,
                    checked = state.emotionAiTag,
                    onCheckedChange = callbacks.onEmotionAiTagToggle
                )

                SettingGap(12)

                SettingSwitchItem(
                    title = HomeRowText.IMAGE_RATIO,
                    subtitle = run {
                        val w = state.imageRatioWidth.toIntOrNull() ?: 0
                        val h = state.imageRatioHeight.toIntOrNull() ?: 0
                        if (w > 0 && h > 0) "宽 ${w}px × 高 ${h}px，点击修改" else "未设置宽高，点击配置"
                    },
                    checked = state.imageRatioEnabled,
                    onCheckedChange = callbacks.onImageRatioToggle,
                    onClick = callbacks.onImageRatioConfigClick
                )

                SettingGap(12)

                SettingSwitchItem(
                    title = HomeRowText.VOICE_SPEED,
                    subtitle = run {
                        val v = state.voiceSpeedValue.toFloatOrNull() ?: 1.5f
                        "播放倍速 $v x，点击修改"
                    },
                    checked = state.voiceSpeedEnabled,
                    onCheckedChange = callbacks.onVoiceSpeedToggle,
                    onClick = callbacks.onVoiceSpeedConfigClick
                )

                SettingGap(12)

                SettingSwitchItem(
                    title = HomeRowText.FORCE_SPEAKER,
                    checked = state.forceSpeakerEnabled,
                    onCheckedChange = callbacks.onForceSpeakerToggle
                )

                SettingGap(12)

                SettingSwitchItem(
                    title = HomeRowText.IMAGE_SUMMARY,
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
                    SettingGap(12)
                    SettingSwitchItem(
                        title = HomeRowText.TIM_ARK_CARD_BYPASS,
                        checked = state.timArkCardBypass,
                        onCheckedChange = callbacks.onTimArkCardBypassToggle
                    )
                }

                SettingGap(12)
                SettingGroupDivider()
                SettingGap(12)

                val scriptEntryLabel = ChatSettingLoader.ENTRY_OPTIONS[state.chatSettingEntry] ?: state.chatSettingEntry
                SettingClickItem(
                    title = HomeRowText.CHAT_SCRIPT_ENTRY,
                    subtitle = "当前「$scriptEntryLabel」· 长按聊天页对应按钮打开脚本菜单（重启QQ生效），点击更改",
                    onClick = callbacks.onChatSettingEntryClick
                )

                SettingGap(20)
                SettingGroupDivider()
                SettingGap(16)
                SettingSwitchItem(
                    title = HomeRowText.MEDIA_PANEL,
                    checked = state.mediaPanelEnabled,
                    onCheckedChange = callbacks.onMediaPanelToggle
                )
                SettingGap(12)
                // 入口选择行：开关关闭时禁用（点击弹纵向单选弹窗，两入口重合时红字警告）
                val mediaEnabled = state.mediaPanelEnabled
                val mediaEntryLabel = MediaPanelLoader.ENTRY_OPTIONS[state.mediaPanelEntry] ?: state.mediaPanelEntry
                val entryConflict = state.chatSettingEntry == state.mediaPanelEntry
                SettingClickItem(
                    title = HomeRowText.MEDIA_PANEL_ENTRY,
                    subtitle = if (entryConflict)
                        "脚本菜单与综合面板入口均为「$mediaEntryLabel」，长按会冲突，请错开 · 点击更改"
                    else
                        "当前「$mediaEntryLabel」· 点击更改",
                    enabled = mediaEnabled,
                    subtitleColor = if (entryConflict)
                        colors.accentRed.copy(alpha = if (mediaEnabled) 1f else 0.5f)
                    else null,
                    onClick = callbacks.onMediaPanelEntryClick
                )

                SettingGap(4)

                SettingSwitchItem(
                    title = HomeRowText.FORCE_INPUT_NO_LIMIT,
                    checked = state.forceInputNoLimit,
                    onCheckedChange = callbacks.onForceInputNoLimitToggle
                )

                SettingGap(4)

                SettingSwitchItem(
                    title = HomeRowText.FORCE_FULLSCREEN_BTN,
                    checked = state.forceFullScreenBtnShow,
                    onCheckedChange = callbacks.onForceFullScreenBtnShowToggle
                )
                }
            }
        }
        }
        }

        item(key = "card_profile") {
        if (!isCardVisibleForSearch(searchQuery, "card_profile")) return@item
        val expanded = searching || expandedCard == "card_profile"
        CompositionLocalProvider(LocalHomeCardSearchText provides homeCardSearchText("card_profile")) {
        QEdgeCard(modifier = Modifier.fillMaxWidth(), glass = true) {
            Column(modifier = Modifier.padding(20.dp)) {
                CardHeader(
                    title = HomeRowText.CARD_PROFILE,
                    expanded = expanded,
                    onClick = { toggleCard("card_profile") }
                )

                CollapsibleContent(expanded) {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = colors.textSecondary.copy(0.08f))
                    Spacer(modifier = Modifier.height(16.dp))

                    SettingSwitchItem(
                        title = HomeRowText.TRANSPARENT_AVATAR,
                        checked = state.transparentAvatar,
                        onCheckedChange = callbacks.onTransparentAvatarToggle
                    )

                    SettingGap(12)

                    SettingSwitchItem(
                        title = HomeRowText.PROFILE_AUTO_LIKE_BACK,
                        checked = state.profileAutoLikeBack,
                        onCheckedChange = callbacks.onProfileAutoLikeBackToggle
                    )
                }
            }
        }
        }
        }

        item(key = "card_level") {
        if (!isCardVisibleForSearch(searchQuery, "card_level")) return@item
        val expanded = searching || expandedCard == "card_level"
        CompositionLocalProvider(LocalHomeCardSearchText provides homeCardSearchText("card_level")) {
        QEdgeCard(modifier = Modifier.fillMaxWidth(), glass = true) {
            Column(modifier = Modifier.padding(20.dp)) {
                CardHeader(
                    title = HomeRowText.CARD_LEVEL,
                    expanded = expanded,
                    onClick = { toggleCard("card_level") }
                )

                CollapsibleContent(expanded) {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = colors.textSecondary.copy(0.08f))
                    Spacer(modifier = Modifier.height(16.dp))

                    SettingSwitchItem(
                        title = HomeRowText.QZONE_CHECKIN,
                        checked = state.qzoneCheckinEnabled,
                        onCheckedChange = callbacks.onCheckinToggle
                    )

                    SettingGap(12)

                    SettingSwitchItem(
                        title = HomeRowText.DAILY_SIGN,
                        checked = state.dailySignEnabled,
                        onCheckedChange = callbacks.onDailySignToggle
                    )

                    SettingGap(12)

                    SettingSwitchItem(
                        title = HomeRowText.BIG_VIP_CHECKIN,
                        checked = state.bigVipCheckinEnabled,
                        onCheckedChange = callbacks.onBigVipCheckinToggle
                    )

                    SettingGap(12)

                    SettingSwitchItem(
                        title = HomeRowText.AUTO_ADD_FRIEND,
                        checked = state.levelBoostEnabled,
                        onCheckedChange = callbacks.onLevelBoostToggle
                    )

                    SettingGap(12)

                    SettingSwitchItem(
                        title = HomeRowText.SPACE_BROWSE,
                        checked = state.spaceBrowseEnabled,
                        onCheckedChange = callbacks.onSpaceBrowseToggle
                    )
                }
            }
        }
        }
        }

        item(key = "card_keepalive") {
        if (!isCardVisibleForSearch(searchQuery, "card_keepalive")) return@item
        val expanded = searching || expandedCard == "card_keepalive"
        CompositionLocalProvider(LocalHomeCardSearchText provides homeCardSearchText("card_keepalive")) {
        QEdgeCard(modifier = Modifier.fillMaxWidth(), glass = true) {
            Column(modifier = Modifier.padding(20.dp)) {
                CardHeader(
                    title = HomeRowText.CARD_KEEPALIVE,
                    expanded = expanded,
                    onClick = { toggleCard("card_keepalive") }
                )

                CollapsibleContent(expanded) {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = colors.textSecondary.copy(0.08f))
                    Spacer(modifier = Modifier.height(16.dp))

                    SettingSwitchItem(
                        title = HomeRowText.KEEP_ALIVE_PIXEL,
                        checked = state.keepAlivePixel,
                        onCheckedChange = callbacks.onKeepAlivePixelToggle
                    )

                    SettingGap(12)

                    SettingSwitchItem(
                        title = HomeRowText.KEEP_ALIVE_FOREGROUND,
                        checked = state.keepAliveForeground,
                        onCheckedChange = callbacks.onKeepAliveForegroundToggle
                    )

                    SettingGap(12)

                    SettingSwitchItem(
                        title = HomeRowText.KEEP_ALIVE_BACKGROUND,
                        checked = state.keepAliveBackground,
                        onCheckedChange = callbacks.onKeepAliveBackgroundToggle
                    )
                }
            }
        }
        }
        }

        item(key = "card_system") {
        if (!isCardVisibleForSearch(searchQuery, "card_system")) return@item
        val expanded = searching || expandedCard == "card_system"
        CompositionLocalProvider(LocalHomeCardSearchText provides homeCardSearchText("card_system")) {
        QEdgeCard(modifier = Modifier.fillMaxWidth(), glass = true) {
            Column(modifier = Modifier.padding(20.dp)) {
                CardHeader(
                    title = HomeRowText.CARD_SYSTEM,
                    expanded = expanded,
                    onClick = { toggleCard("card_system") }
                )

                CollapsibleContent(expanded) {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = colors.textSecondary.copy(0.08f))
                    Spacer(modifier = Modifier.height(16.dp))

                    SettingSwitchItem(
                        title = HomeRowText.ANTI_QFIX_PATCH,
                        checked = state.antiQfixPatch,
                        onCheckedChange = callbacks.onAntiQfixPatchToggle
                    )

                    SettingGap(4)

                    SettingSwitchItem(
                        title = HomeRowText.ANTI_REPORT,
                        checked = state.antiReport,
                        onCheckedChange = callbacks.onAntiReportToggle
                    )

                    SettingGap(4)

                    SettingSwitchItem(
                        title = HomeRowText.FORCE_VIP,
                        checked = state.forceVip,
                        onCheckedChange = callbacks.onForceVipToggle
                    )

                    SettingGap(4)

                    SettingSwitchItem(
                        title = HomeRowText.DISABLE_AI_AVATAR,
                        checked = state.disableAIAvatar,
                        onCheckedChange = callbacks.onDisableAIAvatarToggle
                    )

                    SettingGap(4)

                    SettingSwitchItem(
                        title = HomeRowText.REMOVE_RISK_WEBPAGE,
                        checked = state.removeRiskWebpage,
                        onCheckedChange = callbacks.onRemoveRiskWebpageToggle
                    )

                    SettingGap(4)

                    SettingSwitchItem(
                        title = HomeRowText.DISABLE_WEB_SECURITY_CHECK,
                        checked = state.disableWebSecurityCheck,
                        onCheckedChange = callbacks.onDisableWebSecurityCheckToggle
                    )

                    SettingGap(4)

                    SettingSwitchItem(
                        title = HomeRowText.WEB_JS_ALLOWLIST,
                        subtitle = run {
                            val count = state.webJsAllowlistRules.split(',').count { it.isNotBlank() }
                            if (count > 0) "已放行 $count 个域名，点击修改" else "放行后可让该域名获得*.qq.com的JSBridge权限,可以调用QQ内部接口。未配置域名，点击添加"
                        },
                        checked = state.webJsAllowlistEnabled,
                        onCheckedChange = callbacks.onWebJsAllowlistToggle,
                        onClick = callbacks.onWebJsAllowlistConfigClick
                    )

                    SettingGap(4)

                    SettingSwitchItem(
                        title = HomeRowText.DISABLE_SEC_CHECK,
                        checked = state.disableSecCheck,
                        onCheckedChange = callbacks.onDisableSecCheckToggle
                    )

                    SettingGap(4)

                    SettingSwitchItem(
                        title = HomeRowText.REMOVE_QRCODE_CHECK,
                        checked = state.removeQrCodeCheck,
                        onCheckedChange = callbacks.onRemoveQrCodeCheckToggle
                    )

                    SettingGap(4)

                    SettingSwitchItem(
                        title = HomeRowText.SKIP_SCAN_WAIT,
                        checked = state.skipScanWaitTime,
                        onCheckedChange = callbacks.onSkipScanWaitTimeToggle
                    )

                    SettingGap(4)

                    SettingSwitchItem(
                        title = HomeRowText.SPLIT_SCREEN_SCAN,
                        checked = state.splitScreenScan,
                        onCheckedChange = callbacks.onSplitScreenScanToggle
                    )

                    SettingGap(4)

                    SettingSwitchItem(
                        title = HomeRowText.BYPASS_PROFILE_BAN,
                        checked = state.bypassProfileBan,
                        onCheckedChange = callbacks.onBypassProfileBanToggle
                    )

                    SettingGap(4)

                    SettingSwitchItem(
                        title = HomeRowText.REMOVE_ADS,
                        checked = state.removeAds,
                        onCheckedChange = callbacks.onRemoveAdsToggle
                    )

                    SettingGap(4)

                    SettingSwitchItem(
                        title = HomeRowText.QLOG_REDIRECT,
                        subtitle = when (state.qlogRedirectMode) {
                            QLogRedirect.MODE_MUTE -> "纯拦截模式：QQ日志被直接丢弃，不写入本地文件，点击选择模式"
                            QLogRedirect.MODE_REDIRECT -> "重定向模式：QQ日志写入 QEdge/log/QLog/，点击选择模式"
                            else -> "未开启拦截，QQ日志正常输出，点击选择模式"
                        },
                        checked = state.qlogRedirectMode != QLogRedirect.MODE_OFF,
                        onCheckedChange = callbacks.onQLogRedirectToggle,
                        onClick = callbacks.onQLogRedirectModeClick
                    )

                    SettingGap(4)

                    SettingSwitchItem(
                        title = HomeRowText.FORCE_MODULE_TOAST,
                        checked = state.forceModuleToast,
                        onCheckedChange = callbacks.onForceModuleToastToggle
                    )

                    SettingGap(4)

                    SettingSwitchItem(
                        title = HomeRowText.BG_IMAGE,
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

                    SettingGap(4)
                }
            }
        }
        }
        }
    }
}
