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
    val repeatMsgIcon: String,
    val emotionAiTag: Boolean,
    val imageRatioEnabled: Boolean,
    val imageRatioWidth: String,
    val imageRatioHeight: String,
    val voiceSpeedEnabled: Boolean,
    val voiceSpeedValue: String,
    val forceSpeakerEnabled: Boolean,
    val qlogRedirectMode: String,
    val imageSummaryEnabled: Boolean,
    val imageSummaryMode: String,
    val imageSummaryTips: String,
    val imageSummaryUrl: String,
    val imageSummaryFormat: String,
    val imageSummaryField: String,
    val antiQfixPatch: Boolean,
    val antiReport: Boolean,
    val forceVip: Boolean,
    val disableAIAvatar: Boolean,
    val removeRiskWebpage: Boolean,
    val disableWebSecurityCheck: Boolean,
    val disableSecCheck: Boolean,
    val removeQrCodeCheck: Boolean,
    val skipScanWaitTime: Boolean,
    val bypassProfileBan: Boolean,
    val removeAds: Boolean,
    val forceModuleToast: Boolean,
    val forceInputNoLimit: Boolean,
    val forceFullScreenBtnShow: Boolean,
    val bgImageEnabled: Boolean,
    val bgImageUri: String,
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
    val chatSettingEntry: String,
    val mediaPanelEnabled: Boolean,
    val mediaPanelEntry: String
)

data class HomePageCallbacks(
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
    val onRepeatMsgIconClick: () -> Unit,
    val onEmotionAiTagToggle: (Boolean) -> Unit,
    val onImageRatioToggle: (Boolean) -> Unit,
    val onImageRatioConfigClick: () -> Unit,
    val onVoiceSpeedToggle: (Boolean) -> Unit,
    val onVoiceSpeedConfigClick: () -> Unit,
    val onForceSpeakerToggle: (Boolean) -> Unit,
    val onQLogRedirectToggle: (Boolean) -> Unit,
    val onQLogRedirectModeClick: () -> Unit,
    val onImageSummaryToggle: (Boolean) -> Unit,
    val onImageSummaryConfigClick: () -> Unit,
    val onAntiQfixPatchToggle: (Boolean) -> Unit,
    val onAntiReportToggle: (Boolean) -> Unit,
    val onForceVipToggle: (Boolean) -> Unit,
    val onDisableAIAvatarToggle: (Boolean) -> Unit,
    val onRemoveRiskWebpageToggle: (Boolean) -> Unit,
    val onDisableWebSecurityCheckToggle: (Boolean) -> Unit,
    val onDisableSecCheckToggle: (Boolean) -> Unit,
    val onRemoveQrCodeCheckToggle: (Boolean) -> Unit,
    val onSkipScanWaitTimeToggle: (Boolean) -> Unit,
    val onBypassProfileBanToggle: (Boolean) -> Unit,
    val onRemoveAdsToggle: (Boolean) -> Unit,
    val onForceModuleToastToggle: (Boolean) -> Unit,
    val onForceInputNoLimitToggle: (Boolean) -> Unit,
    val onForceFullScreenBtnShowToggle: (Boolean) -> Unit,
    val onBgImageToggle: (Boolean) -> Unit,
    val onBgImagePickClick: () -> Unit,
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
    val onChatSettingEntryChange: (String) -> Unit,
    val onChatSettingEntryClick: () -> Unit,
    val onMediaPanelToggle: (Boolean) -> Unit,
    val onMediaPanelEntryChange: (String) -> Unit,
    val onMediaPanelEntryClick: () -> Unit
)
