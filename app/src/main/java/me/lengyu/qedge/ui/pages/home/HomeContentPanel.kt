package me.lengyu.qedge.ui.pages.home

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import me.lengyu.qedge.R
import me.lengyu.qedge.ui.core.theme.QEdgeTheme
import java.time.LocalTime

@Composable
internal fun HomeContentPanel(
    versionName: String,
    updateStatus: HomeUpdateStatus,
    currentTime: LocalTime,
    onCheckUpdateClick: () -> Unit,
    onLaunchQQClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = QEdgeTheme.colors

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(colors.cardBackground)
    ) {
        val heroHeight = (maxHeight * if (maxHeight < 720.dp) 0.47f else 0.54f)
            .coerceIn(270.dp, 520.dp)
        val overviewMinHeight = (maxHeight - heroHeight).coerceAtLeast(0.dp)
        val compactWidth = maxWidth < 300.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            HomeHero(
                height = heroHeight,
                versionName = versionName
            )
            HomeOverview(
                updateStatus = updateStatus,
                currentTime = currentTime,
                compactWidth = compactWidth,
                minimumHeight = overviewMinHeight,
                onCheckUpdateClick = onCheckUpdateClick,
                onLaunchQQClick = onLaunchQQClick
            )
        }
    }
}

@Composable
private fun HomeHero(
    height: Dp,
    versionName: String
) {
    val colors = QEdgeTheme.colors
    val heroBrush = if (colors.isDark) {
        Brush.linearGradient(
            listOf(Color(0xFF29475D), Color(0xFF34364D), Color(0xFF4B354A))
        )
    } else {
        Brush.linearGradient(
            listOf(Color(0xFF8FD8F7), Color(0xFFDCE6F2), Color(0xFFF4DDEB))
        )
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .background(heroBrush)
    ) {
        val logoSize = minOf(maxWidth * 0.76f, maxHeight * 0.72f, 350.dp)

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 28.dp, end = 18.dp)
                .size(118.dp)
                .background(Color.White.copy(alpha = 0.14f), CircleShape)
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 22.dp, bottom = 70.dp)
                .size(76.dp)
                .background(Color.White.copy(alpha = 0.12f), CircleShape)
        )

        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 24.dp, top = 22.dp)
                .zIndex(1f),
            shape = RoundedCornerShape(50),
            color = Color.White.copy(alpha = if (colors.isDark) 0.13f else 0.56f)
        ) {
            Text(
                text = "QEdge · v$versionName",
                modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp),
                color = if (colors.isDark) Color.White else Color(0xFF44546A),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Image(
            painter = painterResource(R.drawable.logo),
            contentDescription = "QEdge",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .align(Alignment.Center)
                .size(logoSize)
                .shadow(18.dp, CircleShape)
                .clip(CircleShape)
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(124.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, colors.cardBackground)
                    )
                )
        )
    }
}

@Composable
private fun HomeOverview(
    updateStatus: HomeUpdateStatus,
    currentTime: LocalTime,
    compactWidth: Boolean,
    minimumHeight: Dp,
    onCheckUpdateClick: () -> Unit,
    onLaunchQQClick: () -> Unit
) {
    val colors = QEdgeTheme.colors
    val isCheckingUpdate = updateStatus == HomeUpdateStatus.CHECKING
    val greeting = when (currentTime.hour) {
        in 5..11 -> "早上好"
        in 12..17 -> "下午好"
        else -> "晚上好"
    }
    val statusText = when (updateStatus) {
        HomeUpdateStatus.IDLE -> "等待检查更新"
        HomeUpdateStatus.CHECKING -> "正在检查新版本"
        HomeUpdateStatus.LATEST -> "当前已是最新版本"
        HomeUpdateStatus.AVAILABLE -> "发现新版本，点击获取"
        HomeUpdateStatus.ERROR -> "检查失败，点击重试"
    }
    val statusIcon = when (updateStatus) {
        HomeUpdateStatus.LATEST -> R.drawable.ic_check_circle
        HomeUpdateStatus.AVAILABLE -> R.drawable.book
        else -> R.drawable.ic_refresh
    }
    val statusColor = when (updateStatus) {
        HomeUpdateStatus.LATEST -> colors.accentGreen
        HomeUpdateStatus.ERROR -> colors.accentRed
        else -> colors.accentBlue
    }
    val horizontalPadding = if (compactWidth) 18.dp else 24.dp
    val actionSize = if (compactWidth) 52.dp else 62.dp
    val actionSpacing = if (compactWidth) 8.dp else 12.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = minimumHeight)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = horizontalPadding,
                    end = horizontalPadding,
                    top = 6.dp,
                    bottom = 62.dp
                )
        ) {
            Text(
                text = greeting,
                color = colors.textSecondary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "须知少时凌云志，曾许人间第一流",
                color = colors.textPrimary,
                fontSize = if (compactWidth) 25.sp else 29.sp,
                lineHeight = if (compactWidth) 33.sp else 38.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                onClick = onCheckUpdateClick,
                enabled = !isCheckingUpdate,
                shape = RoundedCornerShape(50),
                color = colors.background
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isCheckingUpdate) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(15.dp),
                            color = statusColor,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            painter = painterResource(statusIcon),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = statusColor
                        )
                    }
                    Spacer(modifier = Modifier.width(7.dp))
                    Text(
                        text = statusText,
                        color = colors.textSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(actionSpacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = onCheckUpdateClick,
                    enabled = !isCheckingUpdate,
                    modifier = Modifier.size(actionSize),
                    shape = CircleShape,
                    color = colors.cardBackground,
                    contentColor = colors.textPrimary,
                    border = BorderStroke(1.dp, colors.textSecondary.copy(alpha = 0.18f)),
                    shadowElevation = 1.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isCheckingUpdate) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = colors.accentBlue,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                painter = painterResource(R.drawable.ic_refresh),
                                contentDescription = "检查更新",
                                modifier = Modifier.size(25.dp)
                            )
                        }
                    }
                }

                Surface(
                    onClick = onLaunchQQClick,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = actionSize),
                    shape = RoundedCornerShape(50),
                    color = colors.cardBackground,
                    contentColor = colors.textPrimary,
                    border = BorderStroke(1.dp, colors.textSecondary.copy(alpha = 0.18f)),
                    shadowElevation = 1.dp
                ) {
                    Box(
                        modifier = Modifier.padding(
                            horizontal = if (compactWidth) 12.dp else 20.dp,
                            vertical = if (compactWidth) 14.dp else 18.dp
                        ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "启动",
                            fontSize = if (compactWidth) 15.sp else 17.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

        }

        ThanksYumeBoxFooter(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 14.dp)
        )
    }
}

@Composable
private fun ThanksYumeBoxFooter(modifier: Modifier = Modifier) {
    val colors = QEdgeTheme.colors
    val gradient = Brush.horizontalGradient(
        listOf(
            colors.textSecondary.copy(alpha = 0.12f),
            colors.textSecondary.copy(alpha = if (colors.isDark) 0.58f else 0.48f),
            colors.textSecondary.copy(alpha = 0.12f)
        )
    )

    Text(
        text = "Thanks to YumeBox",
        modifier = modifier,
        style = TextStyle(
            brush = gradient,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.1.sp
        )
    )
}
