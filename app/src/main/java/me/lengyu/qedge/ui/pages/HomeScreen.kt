package me.lengyu.qedge.ui.pages

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import me.lengyu.qedge.ui.pages.home.HomeCommentInputDialog
import me.lengyu.qedge.ui.pages.home.HomeCreatePluginDialog
import me.lengyu.qedge.ui.pages.home.HomeMoodScheduleDialog
import me.lengyu.qedge.utils.HostInfo
import me.lengyu.qedge.utils.ModuleConfig
import me.lengyu.qedge.plugin.view.ChatSettingLoader
import me.lengyu.qedge.hook.item.LevelBoost
import me.lengyu.qedge.hook.item.KeepAliveHook
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
fun HomeScreen(
    plugins: List<PluginData>,
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit,
    onBackClick: () -> Unit,
    onRunToggle: (String, Boolean) -> Unit,
    onAutoLoadToggle: (String, Boolean) -> Unit,
    onDelete: (String) -> Unit,
    onReload: (String) -> Unit,
    onCreateClick: () -> Unit,
    onCreatePlugin: (type: String, name: String, desc: String, author: String, version: String) -> Unit = { _, _, _, _, _ -> },
    onUploadClick: (PluginData) -> Unit,
    onDownloadClick: (OnlinePluginItem) -> Unit,
    onDocClick: () -> Unit,
    onFileManagerClick: () -> Unit,
    onColdRainClick: () -> Unit
) {
    val colors = QEdgeTheme.colors
    var selectedTab by remember { mutableIntStateOf(0) }

    var onlinePlugins by remember { mutableStateOf<List<OnlinePluginItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    var qzoneAutoLike by remember { mutableStateOf(ModuleConfig.getBoolean("qzone_auto_like", false)) }
    var qzoneAutoComment by remember { mutableStateOf(ModuleConfig.getBoolean("qzone_auto_comment", false)) }
    var qzoneCommentText by remember { mutableStateOf(ModuleConfig.getString("qzone_comment_text", "我来暖说说啦！")) }
    var flashPicBypass by remember { mutableStateOf(ModuleConfig.getBoolean("flash_pic_bypass", false)) }
    var removeLinkInfo by remember { mutableStateOf(ModuleConfig.getBoolean("remove_linkinfo", false)) }
    var downloadEmotion by remember { mutableStateOf(ModuleConfig.getBoolean("download_emotion", false)) }
    var transparentAvatar by remember { mutableStateOf(ModuleConfig.getBoolean("transparent_avatar", false)) }
    var videoToBubble by remember { mutableStateOf(ModuleConfig.getBoolean("video_to_bubble", false)) }
    var antiPokeDelay by remember { mutableStateOf(ModuleConfig.getBoolean("anti_poke_delay", false)) }
    var preventRecall by remember { mutableStateOf(ModuleConfig.getBoolean("prevent_recall", false)) }
    var copyArkMessage by remember { mutableStateOf(ModuleConfig.getBoolean("copy_ark_message", false)) }
    var longClickSendCard by remember { mutableStateOf(ModuleConfig.getBoolean("long_click_send_card", false)) }
    var repeatMsg by remember { mutableStateOf(ModuleConfig.getBoolean("repeat_msg", false)) }
    var antiQfixPatch by remember { mutableStateOf(ModuleConfig.getBoolean("anti_qfix_patch", false)) }
    var antiReport by remember { mutableStateOf(ModuleConfig.getBoolean("anti_report", false)) }
    var forceVip by remember { mutableStateOf(ModuleConfig.getBoolean("force_vip", false)) }
    var disableAIAvatar by remember { mutableStateOf(ModuleConfig.getBoolean("disable_ai_avatar", false)) }
    var timArkCardBypass by remember { mutableStateOf(ModuleConfig.getBoolean("tim_ark_card_bypass", true)) }
    var profileAutoLikeBack by remember { mutableStateOf(ModuleConfig.getBoolean("profile_auto_like_back", false)) }
    var qzoneCheckinEnabled by remember { mutableStateOf(ModuleConfig.getBoolean(LevelBoost.SP_CHECKIN_ENABLED, false)) }
    var dailySignEnabled by remember { mutableStateOf(ModuleConfig.getBoolean(LevelBoost.SP_DAILY_SIGN_ENABLED, false)) }
    var bigVipCheckinEnabled by remember { mutableStateOf(ModuleConfig.getBoolean(LevelBoost.SP_BIGVIP_CHECKIN_ENABLED, false)) }
    var levelBoostEnabled by remember { mutableStateOf(ModuleConfig.getBoolean(LevelBoost.SP_LEVEL_BOOST_ENABLED, false)) }
    var spaceBrowseEnabled by remember { mutableStateOf(ModuleConfig.getBoolean(LevelBoost.SP_SPACE_BROWSE_ENABLED, false)) }
    var moodEnabled by remember { mutableStateOf(ModuleConfig.getBoolean(LevelBoost.SP_MOOD_ENABLED, false)) }
    var moodTime by remember { mutableStateOf(LevelBoost.getMoodTime()) }
    var moodText by remember { mutableStateOf(LevelBoost.getMoodText()) }
    var showCommentDialog by remember { mutableStateOf(false) }
    var showMoodConfigDialog by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var keepAlivePixel by remember { mutableStateOf(ModuleConfig.getBoolean(KeepAliveHook.SP_PIXEL, false)) }
    var keepAliveForeground by remember { mutableStateOf(ModuleConfig.getBoolean(KeepAliveHook.SP_FOREGROUND, false)) }
    var keepAliveBackground by remember { mutableStateOf(ModuleConfig.getBoolean(KeepAliveHook.SP_BACKGROUND, false)) }
    var chatSettingEntry by remember { mutableStateOf(ModuleConfig.getString("chat_setting_entry", "more_features")) }

    // 顶栏下推面板：0=无 1=用户信息 2=更新日志 3=赞助（互斥，点同一按钮收起）
    val currentUin = remember { ModuleConfig.getString("heartbeat_current_uin", "") }
    var expandedPanel by remember { mutableIntStateOf(0) }
    var avatarBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(currentUin) {
        if (currentUin.isNotEmpty()) {
            withContext(Dispatchers.IO) {
                try {
                    val url = URL("https://q.qlogo.cn/g?b=qq&nk=$currentUin&s=100")
                    val connection = url.openConnection()
                    connection.connectTimeout = 10000
                    connection.readTimeout = 10000
                    avatarBitmap = BitmapFactory.decodeStream(connection.getInputStream())
                } catch (_: Throwable) {
                }
            }
        }
    }

    fun loadOnlinePlugins() {
        isLoading = true
        errorMessage = ""
        ModuleScope.launchIOJava("OnlinePlugin") {
            try {
                val urlString = if (searchQuery.isNotEmpty()) {
                    "https://v.yuafeng.cn/QEdge/online_plugin/list.php?api=json&search=${java.net.URLEncoder.encode(searchQuery, "UTF-8")}"
                } else {
                    "https://v.yuafeng.cn/QEdge/online_plugin/list.php?api=json"
                }

                val url = java.net.URL(urlString)
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                connection.requestMethod = "GET"

                val reader = java.io.BufferedReader(java.io.InputStreamReader(connection.inputStream, "UTF-8"))
                val response = java.lang.StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }
                reader.close()

                val json = org.json.JSONObject(response.toString())
                if (json.getInt("code") == 200) {
                    val data = json.getJSONArray("data")
                    val plugins = mutableListOf<OnlinePluginItem>()
                    for (i in 0 until data.length()) {
                        val item = data.getJSONObject(i)
                        plugins.add(
                            OnlinePluginItem(
                                pluginId = item.getString("plugin_id"),
                                pluginName = item.getString("plugin_name"),
                                versionCode = item.getString("version_code"),
                                authorName = item.getString("author_name"),
                                uploadQq = item.getString("upload_qq"),
                                downloadCount = item.getInt("download_count"),
                                uploadTime = item.getString("upload_time"),
                                id = item.getInt("could_id"),
                                description = item.optString("description", ""),
                                type = if (item.optString("type", "java") == "js") "js" else "java"
                            )
                        )
                    }
                    ModuleScope.postToMain {
                        onlinePlugins = plugins
                    }
                } else {
                    ModuleScope.postToMain {
                        errorMessage = json.getString("message")
                    }
                }
            } catch (e: Exception) {
                ModuleScope.postToMain {
                    errorMessage = e.message ?: "获取失败"
                }
            } finally {
                ModuleScope.postToMain {
                    isLoading = false
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            QEdgeTopBar(
                title = when (selectedTab) {
                    0 -> "模块首页"
                    1 -> "拓展脚本"
                    2 -> "冷雨Java"
                    else -> "文件管理"
                },
                showBackButton = true,
                onBackClick = onBackClick,
                isDarkTheme = isDarkTheme,
                onThemeToggle = onThemeToggle,
                showCreateButton = selectedTab == 1,
                onCreateClick = { showCreateDialog = true },
                showDocButton = selectedTab == 1,
                onDocClick = onDocClick,
                showUpdateLogButton = true,
                onUpdateLogClick = { expandedPanel = if (expandedPanel == 2) 0 else 2 },
                showAvatarButton = selectedTab == 0,
                avatarBitmap = avatarBitmap,
                onAvatarClick = { expandedPanel = if (expandedPanel == 1) 0 else 1 },
                showSponsorButton = true,
                onSponsorClick = { expandedPanel = if (expandedPanel == 3) 0 else 3 },
                actions = {}
            )

            AnimatedVisibility(visible = expandedPanel == 1 && selectedTab == 0) {
                UserInfoCard(
                    uin = currentUin,
                    avatarBitmap = avatarBitmap
                )
            }

            AnimatedVisibility(visible = expandedPanel == 2) {
                UpdateLogCard()
            }

            AnimatedVisibility(visible = expandedPanel == 3) {
                SponsorCard()
            }

            HomeTabBar(selectedTab, { newTab ->
                selectedTab = newTab
                expandedPanel = 0
                if (newTab == 1 && onlinePlugins.isEmpty()) {
                    loadOnlinePlugins()
                }
            }, onFileManagerClick, onColdRainClick)

            Spacer(modifier = Modifier.height(12.dp))

            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    val forward = targetState > initialState
                    if (forward) {
                        slideInHorizontally(
                            animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow),
                            initialOffsetX = { it }
                        ) + fadeIn(animationSpec = tween(150)) togetherWith
                        slideOutHorizontally(
                            animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow),
                            targetOffsetX = { -it / 3 }
                        ) + fadeOut(animationSpec = tween(120))
                    } else {
                        slideInHorizontally(
                            animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow),
                            initialOffsetX = { -it }
                        ) + fadeIn(animationSpec = tween(150)) togetherWith
                        slideOutHorizontally(
                            animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow),
                            targetOffsetX = { it / 3 }
                        ) + fadeOut(animationSpec = tween(120))
                    }
                },
                modifier = Modifier.weight(1f)
            ) { tab ->
                when (tab) {
                    0 -> HomePage(
                        state = HomePageState(
                            qzoneAutoLike = qzoneAutoLike,
                            qzoneAutoComment = qzoneAutoComment,
                            commentText = qzoneCommentText,
                            flashPicBypass = flashPicBypass,
                            removeLinkInfo = removeLinkInfo,
                            downloadEmotion = downloadEmotion,
                            transparentAvatar = transparentAvatar,
                            videoToBubble = videoToBubble,
                            antiPokeDelay = antiPokeDelay,
                            timArkCardBypass = timArkCardBypass,
                            profileAutoLikeBack = profileAutoLikeBack,
                            preventRecall = preventRecall,
                            copyArkMessage = copyArkMessage,
                            longClickSendCard = longClickSendCard,
                            repeatMsg = repeatMsg,
                            antiQfixPatch = antiQfixPatch,
                            antiReport = antiReport,
                            forceVip = forceVip,
                            disableAIAvatar = disableAIAvatar,
                            qzoneCheckinEnabled = qzoneCheckinEnabled,
                            dailySignEnabled = dailySignEnabled,
                            bigVipCheckinEnabled = bigVipCheckinEnabled,
                            levelBoostEnabled = levelBoostEnabled,
                            spaceBrowseEnabled = spaceBrowseEnabled,
                            moodEnabled = moodEnabled,
                            moodTime = moodTime,
                            moodText = moodText,
                            keepAlivePixel = keepAlivePixel,
                            keepAliveForeground = keepAliveForeground,
                            keepAliveBackground = keepAliveBackground,
                            chatSettingEntry = chatSettingEntry
                        ),
                        callbacks = HomePageCallbacks(
                            onLikeToggle = {
                                qzoneAutoLike = it
                                Thread { ModuleConfig.putBoolean("qzone_auto_like", it) }.start()
                            },
                            onCommentToggle = {
                                qzoneAutoComment = it
                                Thread { ModuleConfig.putBoolean("qzone_auto_comment", it) }.start()
                            },
                            onCommentTextClick = { showCommentDialog = true },
                            onFlashPicToggle = {
                                flashPicBypass = it
                                Thread { ModuleConfig.putBoolean("flash_pic_bypass", it) }.start()
                            },
                            onRemoveLinkInfoToggle = {
                                removeLinkInfo = it
                                Thread { ModuleConfig.putBoolean("remove_linkinfo", it) }.start()
                            },
                            onDownloadEmotionToggle = {
                                downloadEmotion = it
                                Thread { ModuleConfig.putBoolean("download_emotion", it) }.start()
                            },
                            onTransparentAvatarToggle = {
                                transparentAvatar = it
                                Thread { ModuleConfig.putBoolean("transparent_avatar", it) }.start()
                            },
                            onVideoToBubbleToggle = {
                                videoToBubble = it
                                Thread { ModuleConfig.putBoolean("video_to_bubble", it) }.start()
                            },
                            onAntiPokeDelayToggle = {
                                antiPokeDelay = it
                                Thread { ModuleConfig.putBoolean("anti_poke_delay", it) }.start()
                            },
                            onPreventRecallToggle = {
                                preventRecall = it
                                Thread { ModuleConfig.putBoolean("prevent_recall", it) }.start()
                            },
                            onCopyArkMessageToggle = {
                                copyArkMessage = it
                                Thread { ModuleConfig.putBoolean("copy_ark_message", it) }.start()
                            },
                            onLongClickSendCardToggle = {
                                longClickSendCard = it
                                Thread { ModuleConfig.putBoolean("long_click_send_card", it) }.start()
                            },
                            onRepeatMsgToggle = {
                                repeatMsg = it
                                Thread { ModuleConfig.putBoolean("repeat_msg", it) }.start()
                            },
                            onAntiQfixPatchToggle = {
                                antiQfixPatch = it
                                Thread { ModuleConfig.putBoolean("anti_qfix_patch", it) }.start()
                            },
                            onAntiReportToggle = {
                                antiReport = it
                                Thread { ModuleConfig.putBoolean("anti_report", it) }.start()
                            },
                            onForceVipToggle = {
                                forceVip = it
                                Thread { ModuleConfig.putBoolean("force_vip", it) }.start()
                            },
                            onDisableAIAvatarToggle = {
                                disableAIAvatar = it
                                Thread { ModuleConfig.putBoolean("disable_ai_avatar", it) }.start()
                            },
                            onTimArkCardBypassToggle = {
                                timArkCardBypass = it
                                Thread { ModuleConfig.putBoolean("tim_ark_card_bypass", it) }.start()
                            },
                            onProfileAutoLikeBackToggle = {
                                profileAutoLikeBack = it
                                Thread { ModuleConfig.putBoolean("profile_auto_like_back", it) }.start()
                            },
                            onCheckinToggle = {
                                qzoneCheckinEnabled = it
                                Thread { ModuleConfig.putBoolean(LevelBoost.SP_CHECKIN_ENABLED, it) }.start()
                            },
                            onDailySignToggle = {
                                dailySignEnabled = it
                                Thread { ModuleConfig.putBoolean(LevelBoost.SP_DAILY_SIGN_ENABLED, it) }.start()
                            },
                            onBigVipCheckinToggle = {
                                bigVipCheckinEnabled = it
                                Thread { ModuleConfig.putBoolean(LevelBoost.SP_BIGVIP_CHECKIN_ENABLED, it) }.start()
                            },
                            onLevelBoostToggle = {
                                levelBoostEnabled = it
                                Thread { ModuleConfig.putBoolean(LevelBoost.SP_LEVEL_BOOST_ENABLED, it) }.start()
                            },
                            onSpaceBrowseToggle = {
                                spaceBrowseEnabled = it
                                Thread { ModuleConfig.putBoolean(LevelBoost.SP_SPACE_BROWSE_ENABLED, it) }.start()
                            },
                            onMoodToggle = {
                                moodEnabled = it
                                Thread { ModuleConfig.putBoolean(LevelBoost.SP_MOOD_ENABLED, it) }.start()
                            },
                            onMoodConfigClick = { showMoodConfigDialog = true },
                            onKeepAlivePixelToggle = {
                                keepAlivePixel = it
                                Thread {
                                    ModuleConfig.putBoolean(KeepAliveHook.SP_PIXEL, it)
                                    KeepAliveHook.refresh()
                                }.start()
                            },
                            onKeepAliveForegroundToggle = {
                                keepAliveForeground = it
                                Thread {
                                    ModuleConfig.putBoolean(KeepAliveHook.SP_FOREGROUND, it)
                                    KeepAliveHook.refresh()
                                }.start()
                            },
                            onKeepAliveBackgroundToggle = {
                                keepAliveBackground = it
                                Thread {
                                    ModuleConfig.putBoolean(KeepAliveHook.SP_BACKGROUND, it)
                                    KeepAliveHook.refresh()
                                }.start()
                            },
                            onChatSettingEntryChange = { newValue ->
                                chatSettingEntry = newValue
                                Thread { ModuleConfig.putString("chat_setting_entry", newValue) }.start()
                            }
                        )
                    )
                    1 -> JavaPluginsPage(
                        plugins = plugins,
                        onlinePlugins = onlinePlugins,
                        isLoading = isLoading,
                        searchQuery = searchQuery,
                        errorMessage = errorMessage,
                        onRunToggle = onRunToggle,
                        onAutoLoadToggle = onAutoLoadToggle,
                        onDelete = onDelete,
                        onReload = onReload,
                        onUploadClick = onUploadClick,
                        onSearchChange = { searchQuery = it; loadOnlinePlugins() },
                        onRefresh = { loadOnlinePlugins() },
                        onDownloadClick = onDownloadClick
                    )
                    2 -> ColdRainScreen()
                    else -> EmptyStateView(message = "文件管理")
                }
            }
        }

        HomeCommentInputDialog(
            show = showCommentDialog,
            commentText = qzoneCommentText,
            onDismiss = { showCommentDialog = false },
            onConfirm = { text ->
                qzoneCommentText = text
                showCommentDialog = false
                Thread { ModuleConfig.putString("qzone_comment_text", text) }.start()
            }
        )

        HomeMoodScheduleDialog(
            show = showMoodConfigDialog,
            initialTime = moodTime,
            initialText = moodText,
            onDismiss = { showMoodConfigDialog = false },
            onConfirm = { t, txt ->
                moodTime = t
                moodText = txt.ifEmpty { LevelBoost.getMoodText() }
                showMoodConfigDialog = false
                Thread {
                    ModuleConfig.putString(LevelBoost.SP_MOOD_TIME, t)
                    ModuleConfig.putString(LevelBoost.SP_MOOD_TEXT, txt)
                }.start()
            }
        )

        HomeCreatePluginDialog(
            show = showCreateDialog,
            onDismiss = { showCreateDialog = false },
            onConfirm = { type, name, desc, author, version ->
                onCreatePlugin(type, name, desc, author, version)
                showCreateDialog = false
            }
        )

    }
}

@Composable
private fun UserInfoCard(uin: String, avatarBitmap: android.graphics.Bitmap?) {
    val colors = QEdgeTheme.colors
    val context = androidx.compose.ui.platform.LocalContext.current
    val nickname = remember(uin) { UserData.getNickname(uin) }
    val signature = remember(uin) { UserData.getSignature(uin) }
    val registerTime = remember(uin) { UserData.getRegisterTime(uin) }
    val moduleVersion = remember(uin) { UserData.getModuleVersion(uin) }
    val qqVersion = remember(uin) { UserData.getQqVersion(uin) }
    val isSponsor = remember(uin) { UserData.isSponsor(uin) }
    val sponsorAmountCents = remember(uin) { UserData.getSponsorAmountCents(uin) }
    val canUpload = remember(uin) { UserData.hasUploadPermission(uin) }
    val canReview = remember(uin) { UserData.hasReviewPermission(uin) }

    QEdgeCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(colors.cardBackground)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            try {
                                val intent = android.content.Intent(
                                    android.content.Intent.ACTION_VIEW,
                                    android.net.Uri.parse("https://v.yuafeng.cn/QEdge/user/")
                                ).apply { addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK) }
                                context.startActivity(intent)
                            } catch (_: Throwable) {
                                android.widget.Toast.makeText(context, "无法打开浏览器", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (avatarBitmap != null) {
                        Image(
                            bitmap = avatarBitmap.asImageBitmap(),
                            contentDescription = "用户头像",
                            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(28.dp))
                        )
                    } else {
                        Text("👤", fontSize = 24.sp)
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (nickname.isNotEmpty()) nickname else "未登录",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (uin.isNotEmpty()) "QQ: $uin" else "QQ: --",
                        fontSize = 13.sp,
                        color = colors.textSecondary
                    )
                }
            }

            // 权限/身份标签
            val tags = buildList {
                if (isSponsor) add("赞助用户") else add("普通用户")
                if (canUpload) add("上传权限")
                if (canReview) add("审核权限")
            }
            if (tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    tags.forEach { tag ->
                        Box(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.accentBlue.copy(alpha = 0.12f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(tag, fontSize = 12.sp, color = colors.accentBlue)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = colors.textSecondary.copy(alpha = 0.12f))
            Spacer(modifier = Modifier.height(12.dp))

            UserInfoRow("个性签名", if (signature.isNotEmpty()) signature else "暂无")
            UserInfoRow("注册时间", if (registerTime.isNotEmpty()) registerTime else "--")
            UserInfoRow("模块版本", if (moduleVersion.isNotEmpty()) moduleVersion else "--")
            UserInfoRow("QQ版本", if (qqVersion.isNotEmpty()) qqVersion else "--")
            if (isSponsor && sponsorAmountCents > 0) {
                UserInfoRow(
                    "赞助金额",
                    String.format(java.util.Locale.CHINA, "¥%.2f", sponsorAmountCents / 100.0)
                )
            }
        }
    }
}

@Composable
private fun UserInfoRow(label: String, value: String) {
    val colors = QEdgeTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = colors.textSecondary,
            modifier = Modifier.width(72.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            fontSize = 13.sp,
            color = colors.textPrimary,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun HomeTabBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    onFileManagerClick: () -> Unit,
    onColdRainClick: () -> Unit
) {
    QEdgeCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TabItem("模块首页", selectedTab == 0, { onTabSelected(0) })
            TabItem("拓展脚本", selectedTab == 1, { onTabSelected(1) })
            TabItem("冷雨Java", selectedTab == 2, { onTabSelected(2) })
            TabItem("文件管理", false, onFileManagerClick)
        }
    }
}

data class HomePageState(
    val qzoneAutoLike: Boolean,
    val qzoneAutoComment: Boolean,
    val commentText: String,
    val flashPicBypass: Boolean,
    val removeLinkInfo: Boolean,
    val downloadEmotion: Boolean,
    val transparentAvatar: Boolean,
    val videoToBubble: Boolean,
    val antiPokeDelay: Boolean,
    val timArkCardBypass: Boolean,
    val profileAutoLikeBack: Boolean,
    val preventRecall: Boolean,
    val copyArkMessage: Boolean,
    val longClickSendCard: Boolean,
    val repeatMsg: Boolean,
    val antiQfixPatch: Boolean,
    val antiReport: Boolean,
    val forceVip: Boolean,
    val disableAIAvatar: Boolean,
    val qzoneCheckinEnabled: Boolean,
    val dailySignEnabled: Boolean,
    val bigVipCheckinEnabled: Boolean,
    val levelBoostEnabled: Boolean,
    val spaceBrowseEnabled: Boolean,
    val moodEnabled: Boolean,
    val moodTime: String,
    val moodText: String,
    val keepAlivePixel: Boolean,
    val keepAliveForeground: Boolean,
    val keepAliveBackground: Boolean,
    val chatSettingEntry: String
)

class HomePageCallbacks(
    val onLikeToggle: (Boolean) -> Unit,
    val onCommentToggle: (Boolean) -> Unit,
    val onCommentTextClick: () -> Unit,
    val onFlashPicToggle: (Boolean) -> Unit,
    val onRemoveLinkInfoToggle: (Boolean) -> Unit,
    val onDownloadEmotionToggle: (Boolean) -> Unit,
    val onTransparentAvatarToggle: (Boolean) -> Unit,
    val onVideoToBubbleToggle: (Boolean) -> Unit,
    val onAntiPokeDelayToggle: (Boolean) -> Unit,
    val onTimArkCardBypassToggle: (Boolean) -> Unit,
    val onProfileAutoLikeBackToggle: (Boolean) -> Unit,
    val onPreventRecallToggle: (Boolean) -> Unit,
    val onCopyArkMessageToggle: (Boolean) -> Unit,
    val onLongClickSendCardToggle: (Boolean) -> Unit,
    val onRepeatMsgToggle: (Boolean) -> Unit,
    val onAntiQfixPatchToggle: (Boolean) -> Unit,
    val onAntiReportToggle: (Boolean) -> Unit,
    val onForceVipToggle: (Boolean) -> Unit,
    val onDisableAIAvatarToggle: (Boolean) -> Unit,
    val onCheckinToggle: (Boolean) -> Unit,
    val onDailySignToggle: (Boolean) -> Unit,
    val onBigVipCheckinToggle: (Boolean) -> Unit,
    val onLevelBoostToggle: (Boolean) -> Unit,
    val onSpaceBrowseToggle: (Boolean) -> Unit,
    val onMoodToggle: (Boolean) -> Unit,
    val onMoodConfigClick: () -> Unit,
    val onKeepAlivePixelToggle: (Boolean) -> Unit,
    val onKeepAliveForegroundToggle: (Boolean) -> Unit,
    val onKeepAliveBackgroundToggle: (Boolean) -> Unit,
    val onChatSettingEntryChange: (String) -> Unit
)

@Composable
private fun HomePage(
    state: HomePageState,
    callbacks: HomePageCallbacks
) {
    val colors = QEdgeTheme.colors

    // 用 LazyColumn 替代 Column(verticalScroll)：首帧只组合可见的卡片，
    // 屏幕外的卡片滚动到才构建，避免首次进入时一次性布局全部卡片导致的卡顿。
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "card_qzone") {
        QEdgeCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    "QQ空间",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "自动点赞、自动评论",
                    fontSize = 13.sp,
                    color = colors.textSecondary
                )

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

        item(key = "card_chat") {
        QEdgeCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    "聊天功能",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "闪照破解、视频转泡泡等",
                    fontSize = 13.sp,
                    color = colors.textSecondary
                )

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

                SettingSwitchItem(
                    title = "表情/泡泡/视频/语音下载",
                    subtitle = "长按消息保存到相册",
                    checked = state.downloadEmotion,
                    onCheckedChange = callbacks.onDownloadEmotionToggle
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

                val options = ChatSettingLoader.ENTRY_OPTIONS.entries.toList()
                val chunked = options.chunked(4)
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
            }
        }
        }

        item(key = "card_profile") {
        QEdgeCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    "资料卡",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "上传透明头像等，名片回赞",
                    fontSize = 13.sp,
                    color = colors.textSecondary
                )

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

        item(key = "card_level") {
        QEdgeCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    "等级加速",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "00:00时自动空间打卡，qq日签打卡，大会员签到，自动加好友",
                    fontSize = 13.sp,
                    color = colors.textSecondary
                )

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

        item(key = "card_keepalive") {
        QEdgeCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    "应用保活",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "应用保活，保持进程可见，可能会高耗电",
                    fontSize = 13.sp,
                    color = colors.textSecondary
                )

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

        item(key = "card_system") {
        QEdgeCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    "基础配置",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "禁用QQ修复补丁等系统级功能",
                    fontSize = 13.sp,
                    color = colors.textSecondary
                )

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
            }
        }
        }
    }
}

@Composable
private fun SettingSwitchItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onClick: (() -> Unit)? = null
) {
    val colors = QEdgeTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                ) else Modifier
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                subtitle,
                fontSize = 12.sp,
                color = colors.textSecondary
            )
        }
        QEdgeSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun JavaPluginsPage(
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
private fun SubTabBar(
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
private fun SubTabItem(
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
private fun LocalPluginPage(
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
            contentPadding = PaddingValues(16.dp, 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(plugins, key = { it.id }) { plugin ->
                AnimatedListItem(plugins.indexOf(plugin)) {
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
private fun OnlinePluginPage(
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
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(plugins, key = { it.id }) { plugin ->
                    AnimatedListItem(plugins.indexOf(plugin)) {
                        OnlinePluginCard(plugin = plugin, onDownload = { onDownloadClick(plugin) })
                    }
                }
            }
        }
    }
}

@Composable
private fun OnlinePluginCard(plugin: OnlinePluginItem, onDownload: () -> Unit) {
    val colors = QEdgeTheme.colors
    var isExpanded by remember { mutableStateOf(false) }

    QEdgeCard(modifier = Modifier.fillMaxWidth(), animateContentSize = true) {
        Column(modifier = Modifier.padding(Dimens.PaddingMedium)) {
            OnlinePluginCardHeader(
                plugin.pluginName,
                plugin.versionCode,
                plugin.authorName,
                plugin.type
            ) { isExpanded = !isExpanded }

            if (isExpanded) {
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
private fun OnlinePluginCardHeader(
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

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
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
private fun OnlinePluginCardDetails(
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
private fun OnlinePluginCardActions(onDownload: () -> Unit) {
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
private fun LocalPluginCard(
    plugin: PluginData,
    onRunToggle: (Boolean) -> Unit,
    onAutoLoadToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onReload: () -> Unit,
    onUpload: () -> Unit
) {
    val colors = QEdgeTheme.colors
    var isExpanded by remember { mutableStateOf(false) }

    QEdgeCard(modifier = Modifier.fillMaxWidth(), animateContentSize = true) {
        Column(modifier = Modifier.padding(Dimens.PaddingMedium)) {
            PluginCardHeader(
                plugin.name,
                plugin.version,
                plugin.type,
                plugin.isRunning,
                onRunToggle
            ) { isExpanded = !isExpanded }

            if (isExpanded) {
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
private fun PluginCardHeader(
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

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
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
private fun PluginCardDetails(
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
private fun PluginCardActions(onDelete: () -> Unit, onReload: () -> Unit, onUpload: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        ActionButton("删除", onDelete, style = me.lengyu.qedge.ui.components.atoms.ActionButtonStyle.Danger)
        Spacer(modifier = Modifier.width(Dimens.PaddingSmall))
        ActionButton("重载", onReload, style = me.lengyu.qedge.ui.components.atoms.ActionButtonStyle.Primary)
        Spacer(modifier = Modifier.width(Dimens.PaddingSmall))
        ActionButton("上传", onUpload, style = me.lengyu.qedge.ui.components.atoms.ActionButtonStyle.Success)
    }
}

@Composable
private fun UpdateLogCard() {
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
            .padding(horizontal = 16.dp, vertical = 8.dp)
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

@Composable
private fun SponsorCard() {
    val colors = QEdgeTheme.colors
    var bitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val url = URL("https://cdn.yuafeng.cn/ly/wx.png")
                val connection = url.openConnection()
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                bitmap = BitmapFactory.decodeStream(connection.getInputStream())
            } catch (_: Throwable) {
            } finally {
                isLoading = false
            }
        }
    }

    QEdgeCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "赞助作者",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "感谢你的支持！",
                fontSize = 13.sp,
                color = colors.textSecondary
            )
            Spacer(modifier = Modifier.height(16.dp))
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = AccentBlue
                )
            } else if (bitmap != null) {
                Image(
                    bitmap = bitmap!!.asImageBitmap(),
                    contentDescription = "微信赞赏码",
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .clip(RoundedCornerShape(12.dp))
                )
            } else {
                Text(
                    "加载失败",
                    fontSize = 14.sp,
                    color = colors.textSecondary
                )
            }
        }
    }
}
