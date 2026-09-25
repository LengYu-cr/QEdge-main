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

@Composable
internal fun UserInfoCard(uin: String, avatarBitmap: android.graphics.Bitmap?) {
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
            .padding(horizontal = 16.dp, vertical = 8.dp),
        glass = true
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        /* 赞助用户：沿头像边缘勾勒渐变环，外圈两层低透明描边作光晕 */
                        .drawBehind {
                            if (!isSponsor) return@drawBehind
                            drawCircle(
                                color = Color(0x1AFF3D8B),
                                radius = 33.dp.toPx(),
                                style = Stroke(width = 2.5.dp.toPx())
                            )
                            drawCircle(
                                color = Color(0x0DFF8A00),
                                radius = 35.dp.toPx(),
                                style = Stroke(width = 2.dp.toPx())
                            )
                            drawCircle(
                                brush = Brush.linearGradient(
                                    listOf(Color(0xFFFF8A00), Color(0xFFFF3D8B), Color(0xFF8A5CFF))
                                ),
                                radius = 31.dp.toPx(),
                                style = Stroke(width = 2.dp.toPx())
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    /* 头像 */
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(30.dp))
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
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text("👤", fontSize = 26.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column(modifier = Modifier.weight(1f)) {
                    /* 昵称配色：点击切换。赞助用户多套线性渐变，普通用户径向渐变，最后一档为原始色 */
                    val nicknameBrushes: List<Brush?> = if (isSponsor) {
                        listOf(
                            /* 暖阳 */
                            Brush.linearGradient(
                                listOf(Color(0xFFFF8A00), Color(0xFFFF3D8B), Color(0xFF8A5CFF))
                            ),
                            /* 鎏金 */
                            Brush.linearGradient(
                                listOf(Color(0xFFFFC845), Color(0xFFFF9500), Color(0xFFFF5E00))
                            ),
                            /* 蜜桃紫 */
                            Brush.linearGradient(
                                listOf(Color(0xFFFF6EC4), Color(0xFFB06AB3), Color(0xFF7873F5))
                            ),
                            /* 霞红 */
                            Brush.linearGradient(
                                listOf(Color(0xFFF9D423), Color(0xFFF83600), Color(0xFFD62E5A))
                            ),
                            null
                        )
                    } else {
                        listOf(
                            /* 清透青蓝 */
                            Brush.radialGradient(
                                listOf(Color(0xFF22D3EE), Color(0xFF3B82F6), Color(0xFF6366F1))
                            ),
                            null
                        )
                    }
                    var nickColorStep by remember(uin) { mutableStateOf(0) }
                    val nicknameBrush = nicknameBrushes[nickColorStep % nicknameBrushes.size]
                    val nicknameText = if (nickname.isNotEmpty()) nickname else "未登录"
                    Text(
                        text = buildAnnotatedString {
                            val brush = nicknameBrush
                            if (brush == null) {
                                append(nicknameText)
                            } else {
                                withStyle(SpanStyle(brush = brush)) { append(nicknameText) }
                            }
                        },
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (nicknameBrush == null) colors.textPrimary else Color.Unspecified,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            nickColorStep = (nickColorStep + 1) % nicknameBrushes.size
                        }
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
internal fun UserInfoRow(label: String, value: String) {
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
internal fun HangupEntryCard() {
    val colors = QEdgeTheme.colors
    val context = androidx.compose.ui.platform.LocalContext.current

    QEdgeCard(modifier = Modifier.fillMaxWidth(), glass = true) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    try {
                        val intent = android.content.Intent(
                            android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse("https://v.yuafeng.cn/QEdge/user/hangup.php")
                        ).apply { addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK) }
                        context.startActivity(intent)
                    } catch (_: Throwable) {
                        android.widget.Toast.makeText(context, "无法打开浏览器", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "电脑代挂",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "登录后刷在线时长，约 2 小时自动下线，仅对赞助20元以上用户生效",
                        fontSize = 13.sp,
                        color = colors.textSecondary
                    )
                }
                Text(
                    text = "›",
                    fontSize = 22.sp,
                    color = colors.textSecondary
                )
            }
        }
    }
}

@Composable
internal fun SponsorCard() {
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
            .padding(horizontal = 16.dp, vertical = 8.dp),
        glass = true
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
