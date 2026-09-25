package me.lengyu.qedge.ui.pages

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
import androidx.compose.foundation.layout.navigationBars
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
import androidx.compose.runtime.DisposableEffect
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
import me.lengyu.qedge.ui.pages.coldrain.ColdRainScreen
import me.lengyu.qedge.ui.core.theme.AccentBlue
import me.lengyu.qedge.ui.core.theme.AccentGreen
import me.lengyu.qedge.ui.core.theme.Dimens
import me.lengyu.qedge.ui.core.theme.QEdgeTheme
import me.lengyu.qedge.ui.core.theme.ForcedDarkColors
import me.lengyu.qedge.ui.core.theme.LocalQEdgeColors
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

import me.lengyu.qedge.ui.pages.home.HomePage
import me.lengyu.qedge.ui.pages.home.HomePageCallbacks


import me.lengyu.qedge.ui.pages.home.HomePageState
import me.lengyu.qedge.ui.pages.home.JavaPluginsPage
import me.lengyu.qedge.ui.pages.home.OnlinePluginItem
import me.lengyu.qedge.ui.pages.home.SponsorCard
import me.lengyu.qedge.ui.pages.home.UpdateLogCard
import me.lengyu.qedge.ui.pages.home.UserInfoCard
import me.lengyu.qedge.ui.widget.glass.GlassBackdropHost

/**
 * 自定义背景图解码缓存。
 * 之前解码放在 LaunchedEffect 里（首帧之后才在 IO 线程跑完），
 * 于是首次进入总是先看到主题底色、解码完才切到背景图。
 * 现在首帧组合时同步 load（命中缓存则零成本），背景图第一帧就在。
 */
internal object BgImageCache {
    @Volatile
    private var cachedUri: String = ""
    @Volatile
    private var cachedBitmap: android.graphics.Bitmap? = null

    /** 背景文件内容被覆盖（换图固定写同一路径）时调用，否则 load 会按路径命中旧图 */
    fun invalidate() {
        cachedUri = ""
        cachedBitmap = null
    }

    fun load(context: android.content.Context, imageUri: String): android.graphics.Bitmap? {
        if (imageUri.isEmpty()) return null
        if (imageUri == cachedUri) return cachedBitmap
        val decoded = try {
            val openStream = {
                if (imageUri.startsWith("content://")) {
                    context.contentResolver.openInputStream(android.net.Uri.parse(imageUri))
                } else {
                    java.io.FileInputStream(java.io.File(imageUri))
                }
            }
            // 两遍解码：先测边界再按需采样，避免大图 OOM
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            openStream()?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (bounds.outWidth / sample > 2048 || bounds.outHeight / sample > 2048) sample *= 2
            openStream()?.use {
                BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
            }
        } catch (_: Throwable) {
            null
        }
        cachedUri = imageUri
        cachedBitmap = decoded
        return decoded
    }
}

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
    var imageSummaryEnabled by remember { mutableStateOf(ModuleConfig.getBoolean("image_summary", false)) }
    var imageSummaryMode by remember { mutableStateOf(ModuleConfig.getString("image_summary_mode", "text")) }
    var imageSummaryTips by remember { mutableStateOf(ModuleConfig.getString("image_summary_tips", "")) }
    var imageSummaryUrl by remember { mutableStateOf(ModuleConfig.getString("image_summary_url", "")) }
    var imageSummaryFormat by remember { mutableStateOf(ModuleConfig.getString("image_summary_format", "text")) }
    var imageSummaryField by remember { mutableStateOf(ModuleConfig.getString("image_summary_field", "")) }
    var emotionAiTag by remember { mutableStateOf(ModuleConfig.getBoolean("ai_emotion_tag", false)) }
    var imageRatioEnabled by remember { mutableStateOf(ModuleConfig.getBoolean("image_ratio", false)) }
    var imageRatioWidth by remember { mutableStateOf(ModuleConfig.getInt("image_ratio_width", 0).toString()) }
    var imageRatioHeight by remember { mutableStateOf(ModuleConfig.getInt("image_ratio_height", 0).toString()) }
    var showImageRatioDialog by remember { mutableStateOf(false) }
    var voiceSpeedEnabled by remember { mutableStateOf(ModuleConfig.getBoolean("voice_speed_enable", false)) }
    var voiceSpeedValue by remember { mutableStateOf(ModuleConfig.getString("voice_speed_value", "1.5")) }
    var showVoiceSpeedDialog by remember { mutableStateOf(false) }
    var forceSpeakerEnabled by remember { mutableStateOf(ModuleConfig.getBoolean("force_speaker", false)) }
    var qlogRedirectMode by remember { mutableStateOf(ModuleConfig.getString("qlog_redirect_mode", QLogRedirect.MODE_OFF)) }
    var showQLogRedirectDialog by remember { mutableStateOf(false) }
    var antiQfixPatch by remember { mutableStateOf(ModuleConfig.getBoolean("anti_qfix_patch", false)) }
    var antiReport by remember { mutableStateOf(ModuleConfig.getBoolean("anti_report", false)) }
    var forceVip by remember { mutableStateOf(ModuleConfig.getBoolean("force_vip", false)) }
    var disableAIAvatar by remember { mutableStateOf(ModuleConfig.getBoolean("disable_ai_avatar", false)) }
    var removeRiskWebpage by remember { mutableStateOf(ModuleConfig.getBoolean("remove_risk_webpage", false)) }
    var disableWebSecurityCheck by remember { mutableStateOf(ModuleConfig.getBoolean("disable_web_security_check", false)) }
    var disableSecCheck by remember { mutableStateOf(ModuleConfig.getBoolean("disable_sec_check", false)) }
    var removeQrCodeCheck by remember { mutableStateOf(ModuleConfig.getBoolean("remove_qrcode_check", false)) }
    var skipScanWaitTime by remember { mutableStateOf(ModuleConfig.getBoolean("skip_scan_wait_time", false)) }
    var bypassProfileBan by remember { mutableStateOf(ModuleConfig.getBoolean("bypass_profile_ban", false)) }
    var removeAds by remember { mutableStateOf(ModuleConfig.getBoolean("remove_ads", false)) }
    var forceModuleToast by remember { mutableStateOf(ModuleConfig.getBoolean("force_module_toast", false)) }
    var bgImageEnabled by remember { mutableStateOf(ModuleConfig.getBoolean("bg_image_enabled", false)) }
    var bgImageUri by remember { mutableStateOf(ModuleConfig.getString("bg_image_uri", "")) }
    // 换图固定覆盖同一文件、uri 不变，靠版本号驱动 remember 重算并使缓存失效
    var bgImageVersion by remember { mutableIntStateOf(0) }
    val localContext = androidx.compose.ui.platform.LocalContext.current
    val bgImagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            // 相册授权是临时的，拷贝到模块数据目录后永久保存
            Thread {
                try {
                    val file = java.io.File("${HostInfo.getModuleDataPath()}data", "bg_image.jpg")
                    file.parentFile?.mkdirs()
                    localContext.contentResolver.openInputStream(uri)?.use { input ->
                        file.outputStream().use { output -> input.copyTo(output) }
                    }
                    ModuleConfig.putBoolean("bg_image_enabled", true)
                    ModuleConfig.putString("bg_image_uri", file.absolutePath)
                    // State 必须在主线程更新，且需使缓存失效 + 版本递增才能刷新显示
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        BgImageCache.invalidate()
                        bgImageEnabled = true
                        bgImageUri = file.absolutePath
                        bgImageVersion++
                    }
                } catch (_: Throwable) {
                }
            }.start()
        }
    }
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
    var showImageSummaryDialog by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var keepAlivePixel by remember { mutableStateOf(ModuleConfig.getBoolean(KeepAliveHook.SP_PIXEL, false)) }
    var keepAliveForeground by remember { mutableStateOf(ModuleConfig.getBoolean(KeepAliveHook.SP_FOREGROUND, false)) }
    var keepAliveBackground by remember { mutableStateOf(ModuleConfig.getBoolean(KeepAliveHook.SP_BACKGROUND, false)) }
    var chatSettingEntry by remember { mutableStateOf(ModuleConfig.getString("chat_setting_entry", "more_features")) }
    var mediaPanelEnabled by remember { mutableStateOf(ModuleConfig.getBoolean(MediaPanelLoader.KEY_ENABLED, false)) }
    var mediaPanelEntry by remember { mutableStateOf(ModuleConfig.getString(MediaPanelLoader.KEY_ENTRY, "album")) }

    // 顶栏下推面板：0=无 1=用户信息 2=更新日志 3=赞助（互斥，点同一按钮收起）
    val currentUin = remember { ModuleConfig.getString("heartbeat_current_uin", "") }
    var expandedPanel by remember { mutableIntStateOf(0) }
    var avatarBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    // 背景图同步解码 + 缓存：首帧直接出图，不再“先主题底色、后背景图”闪一下
    val bgImageBitmap = remember(bgImageEnabled, bgImageUri, bgImageVersion) {
        if (bgImageEnabled && bgImageUri.isNotEmpty()) {
            BgImageCache.load(localContext, bgImageUri)
        } else {
            null
        }
    }
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

    // ── 原生液态玻璃导航条的状态桥接 ──
    // 导航条是 Compose 之外的兄弟 View，选中态与点击回调都靠这里同步。
    DisposableEffect(Unit) {
        val host = GlassBackdropHost.get()
        host?.setOnSelect { index ->
            if (index == 3) {
                onFileManagerClick()
            } else {
                selectedTab = index
                expandedPanel = 0
                if (index == 1 && onlinePlugins.isEmpty()) {
                    loadOnlinePlugins()
                }
            }
        }
        onDispose {
            GlassBackdropHost.get()?.setOnSelect(null)
            GlassBackdropHost.get()?.setVisible(false)
        }
    }

    LaunchedEffect(selectedTab) {
        GlassBackdropHost.get()?.setSelected(selectedTab, true)
    }

    // 有背景图时不分亮暗：整页（含弹窗）固定走暗色玻璃风格；无背景图则跟随当前主题
    val bgImageActive = bgImageEnabled && bgImageUri.isNotEmpty()
    val effectiveColors = if (bgImageActive) ForcedDarkColors else colors

    // 底部导航条是 Compose 外的独立 View，配色跟随页面有效配色：
    // 有背景图固定暗色，否则随明暗主题切换（之前只在 attach 时设置过一次）
    LaunchedEffect(effectiveColors.isDark) {
        GlassBackdropHost.get()?.setDark(effectiveColors.isDark)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(effectiveColors.background)
    ) {
        CompositionLocalProvider(LocalQEdgeColors provides effectiveColors) {
        // 作用域内直接捕获 colors 的代码（底部渐隐遮罩等）同步走有效配色
        val colors = effectiveColors
        // 自定义背景图：仅在开关打开且已选图时生效，否则回落默认明暗主题背景
        bgImageBitmap?.let { bmp ->
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
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
                showThemeButton = !bgImageActive,
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
                // 底部不再整体留白：内容一直排到系统导航栏上沿，
                // 玻璃导航条悬浮在内容之上，内容滚过去时被下面的渐隐层淡出。
                modifier = Modifier
                    .weight(1f)
                    .windowInsetsPadding(WindowInsets.navigationBars)
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
                            emotionAiTag = emotionAiTag,
                            imageRatioEnabled = imageRatioEnabled,
                            imageRatioWidth = imageRatioWidth,
                            imageRatioHeight = imageRatioHeight,
                            voiceSpeedEnabled = voiceSpeedEnabled,
                            voiceSpeedValue = voiceSpeedValue,
                            forceSpeakerEnabled = forceSpeakerEnabled,
                            qlogRedirectMode = qlogRedirectMode,
                            imageSummaryEnabled = imageSummaryEnabled,
                            imageSummaryMode = imageSummaryMode,
                            imageSummaryTips = imageSummaryTips,
                            imageSummaryUrl = imageSummaryUrl,
                            imageSummaryFormat = imageSummaryFormat,
                            imageSummaryField = imageSummaryField,
                            antiQfixPatch = antiQfixPatch,
                            antiReport = antiReport,
                            forceVip = forceVip,
                            disableAIAvatar = disableAIAvatar,
                            removeRiskWebpage = removeRiskWebpage,
                            disableWebSecurityCheck = disableWebSecurityCheck,
                            disableSecCheck = disableSecCheck,
                            removeQrCodeCheck = removeQrCodeCheck,
                            skipScanWaitTime = skipScanWaitTime,
                            bypassProfileBan = bypassProfileBan,
                            removeAds = removeAds,
                            forceModuleToast = forceModuleToast,
                            bgImageEnabled = bgImageEnabled,
                            bgImageUri = bgImageUri,
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
                            chatSettingEntry = chatSettingEntry,
                            mediaPanelEnabled = mediaPanelEnabled,
                            mediaPanelEntry = mediaPanelEntry
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
                            onEmotionAiTagToggle = {
                                emotionAiTag = it
                                Thread { ModuleConfig.putBoolean("ai_emotion_tag", it) }.start()
                            },
                            onImageRatioToggle = {
                                imageRatioEnabled = it
                                Thread { ModuleConfig.putBoolean("image_ratio", it) }.start()
                            },
                            onImageRatioConfigClick = { showImageRatioDialog = true },
                            onVoiceSpeedToggle = {
                                voiceSpeedEnabled = it
                                Thread { ModuleConfig.putBoolean("voice_speed_enable", it) }.start()
                            },
                            onVoiceSpeedConfigClick = { showVoiceSpeedDialog = true },
                            onForceSpeakerToggle = {
                                forceSpeakerEnabled = it
                                Thread { ModuleConfig.putBoolean("force_speaker", it) }.start()
                            },
                            onQLogRedirectToggle = {
                                qlogRedirectMode =
                                    if (it) (if (qlogRedirectMode == QLogRedirect.MODE_OFF) QLogRedirect.MODE_REDIRECT else qlogRedirectMode)
                                    else QLogRedirect.MODE_OFF
                                val finalMode = qlogRedirectMode
                                Thread {
                                    ModuleConfig.putString("qlog_redirect_mode", finalMode)
                                    QLogRedirect.invalidateModeCache()
                                }.start()
                            },
                            onQLogRedirectModeClick = { showQLogRedirectDialog = true },
                            onImageSummaryToggle = {
                                imageSummaryEnabled = it
                                Thread { ModuleConfig.putBoolean("image_summary", it) }.start()
                            },
                            onImageSummaryConfigClick = { showImageSummaryDialog = true },
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
                            onRemoveRiskWebpageToggle = {
                                removeRiskWebpage = it
                                Thread { ModuleConfig.putBoolean("remove_risk_webpage", it) }.start()
                            },
                            onDisableWebSecurityCheckToggle = {
                                disableWebSecurityCheck = it
                                Thread { ModuleConfig.putBoolean("disable_web_security_check", it) }.start()
                            },
                            onDisableSecCheckToggle = {
                                disableSecCheck = it
                                Thread { ModuleConfig.putBoolean("disable_sec_check", it) }.start()
                            },
                            onRemoveQrCodeCheckToggle = {
                                removeQrCodeCheck = it
                                Thread { ModuleConfig.putBoolean("remove_qrcode_check", it) }.start()
                            },
                            onSkipScanWaitTimeToggle = {
                                skipScanWaitTime = it
                                Thread { ModuleConfig.putBoolean("skip_scan_wait_time", it) }.start()
                            },
                            onBypassProfileBanToggle = {
                                bypassProfileBan = it
                                Thread { ModuleConfig.putBoolean("bypass_profile_ban", it) }.start()
                            },
                            onRemoveAdsToggle = {
                                removeAds = it
                                Thread { ModuleConfig.putBoolean("remove_ads", it) }.start()
                            },
                            onForceModuleToastToggle = {
                                forceModuleToast = it
                                Thread { ModuleConfig.putBoolean("force_module_toast", it) }.start()
                            },
                            onBgImageToggle = { enable ->
                                if (enable) {
                                    if (bgImageUri.isNotEmpty()) {
                                        // 已选过图：直接恢复上一次设定的背景，不再拉起相册
                                        bgImageEnabled = true
                                        Thread {
                                            ModuleConfig.putBoolean("bg_image_enabled", true)
                                        }.start()
                                    } else {
                                        bgImagePicker.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                                } else {
                                    bgImageEnabled = false
                                    Thread { ModuleConfig.putBoolean("bg_image_enabled", false) }.start()
                                }
                            },
                            onBgImagePickClick = {
                                bgImagePicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
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
                            },
                            onMediaPanelToggle = { enabled ->
                                mediaPanelEnabled = enabled
                                if (enabled) {
                                    // 打开综合面板时联动打开「图片视频语音下载」，方便保存到本地图库
                                    downloadEmotion = true
                                    Thread { ModuleConfig.putBoolean("download_emotion", true) }.start()
                                }
                                Thread { ModuleConfig.putBoolean(MediaPanelLoader.KEY_ENABLED, enabled) }.start()
                            },
                            onMediaPanelEntryChange = { newValue ->
                                mediaPanelEntry = newValue
                                Thread { ModuleConfig.putString("media_panel_entry", newValue) }.start()
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

        // 底部渐隐：盖住内容与玻璃导航条之间那条硬边。
        // 用背景色做三段渐变（透明度 0 → 0.72 → 1），既不会插出灰雾，
        // 又能让滚到下面的卡片自然淡出，玻璃采样到的边缘也就连续了。
        // 背景图模式跳过：此时渐变色是强制暗底色，会在导航条后面压出一条黑带。
        if (!bgImageActive) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(GlassBackdropHost.CONTENT_BOTTOM_DP.dp + 24.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                colors.background.copy(alpha = 0f),
                                colors.background.copy(alpha = 0.72f),
                                colors.background
                            )
                        )
                    )
            )
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

        HomeImageRatioDialog(
            show = showImageRatioDialog,
            width = imageRatioWidth,
            height = imageRatioHeight,
            onDismiss = { showImageRatioDialog = false },
            onConfirm = { w, h ->
                imageRatioWidth = w.toString()
                imageRatioHeight = h.toString()
                showImageRatioDialog = false
                Thread {
                    ModuleConfig.putInt("image_ratio_width", w)
                    ModuleConfig.putInt("image_ratio_height", h)
                }.start()
            }
        )

        HomeVoiceSpeedDialog(
            show = showVoiceSpeedDialog,
            current = voiceSpeedValue,
            onDismiss = { showVoiceSpeedDialog = false },
            onConfirm = { v ->
                voiceSpeedValue = v
                showVoiceSpeedDialog = false
                Thread { ModuleConfig.putString("voice_speed_value", v) }.start()
            }
        )

        HomeQLogRedirectDialog(
            show = showQLogRedirectDialog,
            current = qlogRedirectMode,
            onDismiss = { showQLogRedirectDialog = false },
            onConfirm = { m ->
                qlogRedirectMode = m
                val finalMode = m
                Thread {
                    ModuleConfig.putString("qlog_redirect_mode", finalMode)
                    QLogRedirect.invalidateModeCache()
                }.start()
            }
        )

        HomeImageSummaryDialog(
            show = showImageSummaryDialog,
            mode = imageSummaryMode,
            tips = imageSummaryTips,
            url = imageSummaryUrl,
            format = imageSummaryFormat,
            field = imageSummaryField,
            onDismiss = { showImageSummaryDialog = false },
            onConfirm = { m, tips, url, format, field ->
                imageSummaryMode = m
                imageSummaryTips = tips
                imageSummaryUrl = url
                imageSummaryFormat = format
                imageSummaryField = field
                showImageSummaryDialog = false
                Thread {
                    ModuleConfig.putString("image_summary_mode", m)
                    ModuleConfig.putString("image_summary_tips", tips)
                    ModuleConfig.putString("image_summary_url", url)
                    ModuleConfig.putString("image_summary_format", format)
                    ModuleConfig.putString("image_summary_field", field)
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
}
