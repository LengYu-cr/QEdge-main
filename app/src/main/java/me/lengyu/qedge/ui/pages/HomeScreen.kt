package me.lengyu.qedge.ui.pages

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import me.lengyu.qedge.utils.HostInfo
import me.lengyu.qedge.utils.ModuleConfig
import me.lengyu.qedge.hook.item.QZoneSchedule
import me.lengyu.qedge.hook.item.KeepAliveHook
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.Box as ComposeBox

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
    var timArkCardBypass by remember { mutableStateOf(ModuleConfig.getBoolean("tim_ark_card_bypass", true)) }
    var profileAutoLikeBack by remember { mutableStateOf(ModuleConfig.getBoolean("profile_auto_like_back", false)) }
    var qzoneCheckinEnabled by remember { mutableStateOf(ModuleConfig.getBoolean(QZoneSchedule.SP_CHECKIN_ENABLED, false)) }
    var dailySignEnabled by remember { mutableStateOf(ModuleConfig.getBoolean(QZoneSchedule.SP_DAILY_SIGN_ENABLED, false)) }
    var bigVipCheckinEnabled by remember { mutableStateOf(ModuleConfig.getBoolean(QZoneSchedule.SP_BIGVIP_CHECKIN_ENABLED, false)) }
    var levelBoostEnabled by remember { mutableStateOf(ModuleConfig.getBoolean(QZoneSchedule.SP_LEVEL_BOOST_ENABLED, false)) }
    var spaceBrowseEnabled by remember { mutableStateOf(ModuleConfig.getBoolean(QZoneSchedule.SP_SPACE_BROWSE_ENABLED, false)) }
    var moodEnabled by remember { mutableStateOf(ModuleConfig.getBoolean(QZoneSchedule.SP_MOOD_ENABLED, false)) }
    var moodTime by remember { mutableStateOf(QZoneSchedule.getMoodTime()) }
    var moodText by remember { mutableStateOf(QZoneSchedule.getMoodText()) }
    var showCommentDialog by remember { mutableStateOf(false) }
    var showMoodConfigDialog by remember { mutableStateOf(false) }
    var showUpdateLogDialog by remember { mutableStateOf(false) }
    var keepAlivePixel by remember { mutableStateOf(ModuleConfig.getBoolean(KeepAliveHook.SP_PIXEL, false)) }
    var keepAliveForeground by remember { mutableStateOf(ModuleConfig.getBoolean(KeepAliveHook.SP_FOREGROUND, false)) }
    var keepAliveBackground by remember { mutableStateOf(ModuleConfig.getBoolean(KeepAliveHook.SP_BACKGROUND, false)) }

    fun loadOnlinePlugins() {
        isLoading = true
        errorMessage = ""
        Thread {
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
                                description = item.optString("description", "")
                            )
                        )
                    }
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        onlinePlugins = plugins
                    }
                } else {
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        errorMessage = json.getString("message")
                    }
                }
            } catch (e: Exception) {
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    errorMessage = e.message ?: "获取失败"
                }
            } finally {
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    isLoading = false
                }
            }
        }.start()
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
                    1 -> "Java脚本"
                    2 -> "冷雨Java"
                    else -> "文件管理"
                },
                showBackButton = true,
                onBackClick = onBackClick,
                isDarkTheme = isDarkTheme,
                onThemeToggle = onThemeToggle,
                showCreateButton = selectedTab == 1,
                onCreateClick = onCreateClick,
                showDocButton = selectedTab == 1,
                onDocClick = onDocClick,
                showUpdateLogButton = true,
                onUpdateLogClick = { showUpdateLogDialog = true },
                actions = {}
            )

            HomeTabBar(selectedTab, { newTab ->
                selectedTab = newTab
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
                            animationSpec = tween(250),
                            initialOffsetX = { it }
                        ) + fadeIn(animationSpec = tween(150)) togetherWith
                        slideOutHorizontally(
                            animationSpec = tween(250),
                            targetOffsetX = { -it / 3 }
                        ) + fadeOut(animationSpec = tween(150))
                    } else {
                        slideInHorizontally(
                            animationSpec = tween(250),
                            initialOffsetX = { -it }
                        ) + fadeIn(animationSpec = tween(150)) togetherWith
                        slideOutHorizontally(
                            animationSpec = tween(250),
                            targetOffsetX = { it / 3 }
                        ) + fadeOut(animationSpec = tween(150))
                    }
                },
                modifier = Modifier.weight(1f)
            ) { tab ->
                when (tab) {
                    0 -> HomePage(
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
                        qzoneCheckinEnabled = qzoneCheckinEnabled,
                        dailySignEnabled = dailySignEnabled,
                        bigVipCheckinEnabled = bigVipCheckinEnabled,
                        levelBoostEnabled = levelBoostEnabled,
                        spaceBrowseEnabled = spaceBrowseEnabled,
                        moodEnabled = moodEnabled,
                        moodTime = moodTime,
                        moodText = moodText,
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
                            Thread { ModuleConfig.putBoolean(QZoneSchedule.SP_CHECKIN_ENABLED, it) }.start()
                        },
                        onDailySignToggle = {
                            dailySignEnabled = it
                            Thread { ModuleConfig.putBoolean(QZoneSchedule.SP_DAILY_SIGN_ENABLED, it) }.start()
                        },
                        onBigVipCheckinToggle = {
                            bigVipCheckinEnabled = it
                            Thread { ModuleConfig.putBoolean(QZoneSchedule.SP_BIGVIP_CHECKIN_ENABLED, it) }.start()
                        },
                        onLevelBoostToggle = {
                            levelBoostEnabled = it
                            Thread { ModuleConfig.putBoolean(QZoneSchedule.SP_LEVEL_BOOST_ENABLED, it) }.start()
                        },
                        onSpaceBrowseToggle = {
                            spaceBrowseEnabled = it
                            Thread { ModuleConfig.putBoolean(QZoneSchedule.SP_SPACE_BROWSE_ENABLED, it) }.start()
                        },
                        onMoodToggle = {
                            moodEnabled = it
                            Thread { ModuleConfig.putBoolean(QZoneSchedule.SP_MOOD_ENABLED, it) }.start()
                        },
                        onMoodConfigClick = { showMoodConfigDialog = true },
                        keepAlivePixel = keepAlivePixel,
                        keepAliveForeground = keepAliveForeground,
                        keepAliveBackground = keepAliveBackground,
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
                        }
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

        CommentInputDialog(
            show = showCommentDialog,
            commentText = qzoneCommentText,
            onDismiss = { showCommentDialog = false },
            onConfirm = { text ->
                qzoneCommentText = text
                showCommentDialog = false
                Thread { ModuleConfig.putString("qzone_comment_text", text) }.start()
            }
        )

        MoodScheduleDialog(
            show = showMoodConfigDialog,
            initialTime = moodTime,
            initialText = moodText,
            onDismiss = { showMoodConfigDialog = false },
            onConfirm = { t, txt ->
                moodTime = t
                moodText = txt.ifEmpty { QZoneSchedule.getMoodText() }
                showMoodConfigDialog = false
                Thread {
                    ModuleConfig.putString(QZoneSchedule.SP_MOOD_TIME, t)
                    ModuleConfig.putString(QZoneSchedule.SP_MOOD_TEXT, txt)
                }.start()
            }
        )

        UpdateLogDialog(
            show = showUpdateLogDialog,
            onDismiss = { showUpdateLogDialog = false }
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
            TabItem("Java脚本", selectedTab == 1, { onTabSelected(1) })
            TabItem("冷雨Java", selectedTab == 2, { onTabSelected(2) })
            TabItem("文件管理", false, onFileManagerClick)
        }
    }
}

@Composable
private fun HomePage(
    qzoneAutoLike: Boolean,
    qzoneAutoComment: Boolean,
    commentText: String,
    flashPicBypass: Boolean,
    removeLinkInfo: Boolean,
    downloadEmotion: Boolean,
    transparentAvatar: Boolean,
    videoToBubble: Boolean,
    antiPokeDelay: Boolean,
    timArkCardBypass: Boolean,
    profileAutoLikeBack: Boolean,
    qzoneCheckinEnabled: Boolean,
    dailySignEnabled: Boolean,
    bigVipCheckinEnabled: Boolean,
    levelBoostEnabled: Boolean,
    spaceBrowseEnabled: Boolean,
    moodEnabled: Boolean,
    moodTime: String,
    moodText: String,
    onLikeToggle: (Boolean) -> Unit,
    onCommentToggle: (Boolean) -> Unit,
    onCommentTextClick: () -> Unit,
    onFlashPicToggle: (Boolean) -> Unit,
    onRemoveLinkInfoToggle: (Boolean) -> Unit,
    onDownloadEmotionToggle: (Boolean) -> Unit,
    onTransparentAvatarToggle: (Boolean) -> Unit,
    onVideoToBubbleToggle: (Boolean) -> Unit,
    onAntiPokeDelayToggle: (Boolean) -> Unit,
    onTimArkCardBypassToggle: (Boolean) -> Unit,
    onProfileAutoLikeBackToggle: (Boolean) -> Unit,
    onCheckinToggle: (Boolean) -> Unit,
    onDailySignToggle: (Boolean) -> Unit,
    onBigVipCheckinToggle: (Boolean) -> Unit,
    onLevelBoostToggle: (Boolean) -> Unit,
    onSpaceBrowseToggle: (Boolean) -> Unit,
    onMoodToggle: (Boolean) -> Unit,
    onMoodConfigClick: () -> Unit,
    keepAlivePixel: Boolean,
    keepAliveForeground: Boolean,
    keepAliveBackground: Boolean,
    onKeepAlivePixelToggle: (Boolean) -> Unit,
    onKeepAliveForegroundToggle: (Boolean) -> Unit,
    onKeepAliveBackgroundToggle: (Boolean) -> Unit
) {
    val colors = QEdgeTheme.colors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
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
                    checked = qzoneAutoLike,
                    onCheckedChange = onLikeToggle
                )

                Spacer(modifier = Modifier.height(12.dp))

                SettingSwitchItem(
                    title = "空间秒评",
                    subtitle = commentText,
                    checked = qzoneAutoComment,
                    onCheckedChange = onCommentToggle,
                    onClick = onCommentTextClick
                )

                Spacer(modifier = Modifier.height(12.dp))

                SettingSwitchItem(
                    title = "定时发说说 +0.5天",
                    subtitle = run {
                        val preview = if (moodText.length > 18) moodText.take(18) + "…" else moodText
                        "$moodTime · $preview"
                    },
                    checked = moodEnabled,
                    onCheckedChange = onMoodToggle,
                    onClick = onMoodConfigClick
                )

            }
        }

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
                    checked = flashPicBypass,
                    onCheckedChange = onFlashPicToggle
                )

                Spacer(modifier = Modifier.height(12.dp))

                SettingSwitchItem(
                    title = "表情/泡泡/视频/语音下载",
                    subtitle = "长按消息保存到相册",
                    checked = downloadEmotion,
                    onCheckedChange = onDownloadEmotionToggle
                )

                Spacer(modifier = Modifier.height(12.dp))

                SettingSwitchItem(
                    title = "屏蔽链接信息卡片",
                    subtitle = "收到链接时，自动屏蔽",
                    checked = removeLinkInfo,
                    onCheckedChange = onRemoveLinkInfoToggle
                )

                if (HostInfo.isQQ) {
                    Spacer(modifier = Modifier.height(12.dp))

                    SettingSwitchItem(
                        title = "视频转泡泡消息",
                        subtitle = "发送视频时，自动替换为泡泡",
                        checked = videoToBubble,
                        onCheckedChange = onVideoToBubbleToggle
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                SettingSwitchItem(
                    title = "取消拍一拍时间限制",
                    subtitle = "解除拍一拍时间限制",
                    checked = antiPokeDelay,
                    onCheckedChange = onAntiPokeDelayToggle
                )

                if (HostInfo.isTIM) {
                    Spacer(modifier = Modifier.height(12.dp))
                    SettingSwitchItem(
                        title = "TIM卡片阻断绕过",
                        subtitle = "解除低版本TIM对Ark卡片跳转的限制",
                        checked = timArkCardBypass,
                        onCheckedChange = onTimArkCardBypassToggle
                    )
                }
            }
        }

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
                    checked = transparentAvatar,
                    onCheckedChange = onTransparentAvatarToggle
                )

                Spacer(modifier = Modifier.height(12.dp))

                SettingSwitchItem(
                    title = "名片自动回赞",
                    subtitle = "收到名片点赞自动回赞",
                    checked = profileAutoLikeBack,
                    onCheckedChange = onProfileAutoLikeBackToggle
                )
            }
        }

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
                    checked = qzoneCheckinEnabled,
                    onCheckedChange = onCheckinToggle
                )

                Spacer(modifier = Modifier.height(12.dp))

                SettingSwitchItem(
                    title = "QQ 日签打卡",
                    subtitle = "自动执行日签打卡 +0.5天",
                    checked = dailySignEnabled,
                    onCheckedChange = onDailySignToggle
                )

                Spacer(modifier = Modifier.height(12.dp))

                SettingSwitchItem(
                    title = "大会员签到",
                    subtitle = "自动执行（无需开通大会员） +0.5天",
                    checked = bigVipCheckinEnabled,
                    onCheckedChange = onBigVipCheckinToggle
                )

                Spacer(modifier = Modifier.height(12.dp))

                SettingSwitchItem(
                    title = "自动加好友",
                    subtitle = "自动添加3个好友 +1.5天",
                    checked = levelBoostEnabled,
                    onCheckedChange = onLevelBoostToggle
                )

                Spacer(modifier = Modifier.height(12.dp))

                SettingSwitchItem(
                    title = "空间浏览",
                    subtitle = "浏览好友说说10条 +0.5天",
                    checked = spaceBrowseEnabled,
                    onCheckedChange = onSpaceBrowseToggle
                )
            }
        }

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
                    QEdgeSwitch(checked = keepAlivePixel, onCheckedChange = onKeepAlivePixelToggle)
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
                    QEdgeSwitch(checked = keepAliveForeground, onCheckedChange = onKeepAliveForegroundToggle)
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
                    QEdgeSwitch(checked = keepAliveBackground, onCheckedChange = onKeepAliveBackgroundToggle)
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
                color = colors.textSecondary,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
        QEdgeSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun CommentInputDialog(
    show: Boolean,
    commentText: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    if (!show) return

    val colors = QEdgeTheme.colors
    var text by remember(commentText) { mutableStateOf(commentText) }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(colors.cardBackground)
                .padding(20.dp)
        ) {
            Text(
                "自定义评论内容",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(16.dp))
            ComposeBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.textSecondary.copy(alpha = 0.08f))
                    .padding(14.dp)
            ) {
                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 14.sp,
                        color = colors.textPrimary
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4,
                    decorationBox = { innerTextField ->
                        if (text.isEmpty()) {
                            Text("说点什么吧...", fontSize = 14.sp, color = colors.textSecondary)
                        }
                        innerTextField()
                    }
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                ComposeBox(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onDismiss
                        )
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("取消", fontSize = 14.sp, color = colors.textSecondary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                ComposeBox(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(AccentGreen)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onConfirm(text) }
                        )
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("确定", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = androidx.compose.ui.graphics.Color.White)
                }
            }
        }
    }
}

@Composable
private fun MoodScheduleDialog(
    show: Boolean,
    initialTime: String,
    initialText: String,
    onDismiss: () -> Unit,
    onConfirm: (time: String, text: String) -> Unit
) {
    if (!show) return

    val colors = QEdgeTheme.colors
    var time by remember(initialTime) { mutableStateOf(initialTime) }
    var text by remember(initialText) { mutableStateOf(initialText) }
    val timeOk = QZoneSchedule.HH_MM_REGEX.matches(time.trim())
    val canConfirm = timeOk

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(colors.cardBackground)
                .padding(20.dp)
        ) {
            Text(
                "定时说说设置",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "仅需 HH:mm，无需日期，每天同一时间触发一次",
                fontSize = 12.sp,
                color = colors.textSecondary
            )

            Spacer(modifier = Modifier.height(14.dp))
            Text("发送时间 (HH:mm)", fontSize = 13.sp, color = colors.textPrimary)
            Spacer(modifier = Modifier.height(6.dp))
            ComposeBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.textSecondary.copy(alpha = 0.08f))
                    .padding(14.dp)
            ) {
                BasicTextField(
                    value = time,
                    onValueChange = { v ->
                        val t = v.filter { it.isDigit() || it == ':' }.take(5)
                        time = t
                    },
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 16.sp,
                        color = colors.textPrimary
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (time.isEmpty()) {
                                Text("08:30", fontSize = 16.sp, color = colors.textSecondary)
                            } else {
                                innerTextField()
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                if (timeOk) "格式正确" else "格式错误",
                                fontSize = 12.sp,
                                color = if (timeOk) AccentGreen else android.graphics.Color.parseColor("#FF5252").let { androidx.compose.ui.graphics.Color(it) }
                            )
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            Text("说说内容", fontSize = 13.sp, color = colors.textPrimary)
            Spacer(modifier = Modifier.height(6.dp))
            ComposeBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.textSecondary.copy(alpha = 0.08f))
                    .padding(14.dp)
            ) {
                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 14.sp,
                        color = colors.textPrimary
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 6,
                    decorationBox = { innerTextField ->
                        if (text.isEmpty()) {
                            Text("今天也要加油哦~", fontSize = 14.sp, color = colors.textSecondary)
                        } else {
                            innerTextField()
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                ComposeBox(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onDismiss
                        )
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("取消", fontSize = 14.sp, color = colors.textSecondary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                ComposeBox(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (canConfirm) AccentGreen else colors.textSecondary.copy(alpha = 0.3f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                if (!canConfirm) return@clickable
                                val t = time.trim()
                                val txt = text.trim()
                                onConfirm(t, txt)
                            }
                        )
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "保存",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (canConfirm) androidx.compose.ui.graphics.Color.White else colors.textSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun UpdateLogDialog(
    show: Boolean,
    onDismiss: () -> Unit
) {
    if (!show) return

    val colors = QEdgeTheme.colors
    var logText by remember { mutableStateOf("加载中...") }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        Thread {
            try {
                val url = java.net.URL("https://v.yuafeng.cn/QEdge/update/changelog.php")
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.requestMethod = "GET"

                val reader = java.io.BufferedReader(
                    java.io.InputStreamReader(connection.inputStream, "UTF-8")
                )
                val response = java.lang.StringBuilder()
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

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .clip(RoundedCornerShape(20.dp))
                .background(colors.cardBackground)
                .padding(20.dp)
        ) {
            Text(
                "更新日志",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(12.dp))
            ComposeBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    logText,
                    fontSize = 14.sp,
                    color = colors.textSecondary,
                    lineHeight = 20.sp
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                ComposeBox(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onDismiss
                        )
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("关闭", fontSize = 14.sp, color = colors.textPrimary)
                }
            }
        }
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
                        animationSpec = tween(220),
                        initialOffsetX = { it / 2 }
                    ) + fadeIn(animationSpec = tween(150)) togetherWith
                    slideOutHorizontally(
                        animationSpec = tween(220),
                        targetOffsetX = { -it / 2 }
                    ) + fadeOut(animationSpec = tween(150))
                } else {
                    slideInHorizontally(
                        animationSpec = tween(220),
                        initialOffsetX = { -it / 2 }
                    ) + fadeIn(animationSpec = tween(150)) togetherWith
                    slideOutHorizontally(
                        animationSpec = tween(220),
                        targetOffsetX = { it / 2 }
                    ) + fadeOut(animationSpec = tween(150))
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
                plugin.authorName
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
    onExpandToggle: () -> Unit
) {
    val colors = QEdgeTheme.colors

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
            Text(name, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
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
    val description: String = ""
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
    isRunning: Boolean,
    onRunToggle: (Boolean) -> Unit,
    onExpandToggle: () -> Unit
) {
    val colors = QEdgeTheme.colors

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
            Text(name, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
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
