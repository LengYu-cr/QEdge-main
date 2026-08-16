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
import androidx.compose.ui.text.style.TextAlign
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
    onMenuClick: () -> Unit,
    onCheckUpdateClick: () -> Unit,
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
        val compactWidth = maxWidth < 300.dp

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            HomeHero(
                height = heroHeight,
                versionName = versionName,
                updateStatus = updateStatus,
                onMenuClick = onMenuClick,
                onCheckUpdateClick = onCheckUpdateClick
            )
            HomeOverview(
                updateStatus = updateStatus,
                currentTime = currentTime,
                compactWidth = compactWidth,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun HomeHero(
    height: Dp,
    versionName: String,
    updateStatus: HomeUpdateStatus,
    onMenuClick: () -> Unit,
    onCheckUpdateClick: () -> Unit
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
    val isCheckingUpdate = updateStatus == HomeUpdateStatus.CHECKING

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .background(heroBrush)
    ) {
        // 汉堡菜单按钮
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(start = 16.dp, top = 16.dp)
                .size(40.dp)
                .zIndex(2f)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.12f))
                .clickable(onClick = onMenuClick),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "☰",
                color = if (colors.isDark) Color.White else Color(0xFF44546A),
                fontSize = 20.sp,
                fontWeight = FontWeight.Light
            )
        }

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

        // 右上角：版本号 + 检测更新按钮
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 16.dp, end = 16.dp)
                .zIndex(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 版本号
            Surface(
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

            // 检测更新按钮
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.12f))
                    .clickable(
                        enabled = !isCheckingUpdate,
                        onClick = onCheckUpdateClick
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isCheckingUpdate) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = if (colors.isDark) Color.White else Color(0xFF44546A),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        painter = painterResource(R.drawable.ic_refresh),
                        contentDescription = "检查更新",
                        modifier = Modifier.size(16.dp),
                        tint = if (colors.isDark) Color.White else Color(0xFF44546A)
                    )
                }
            }
        }

        // Spacer(modifier = Modifier.height(40.dp))

        Image(
            painter = painterResource(R.drawable.logo),
            contentDescription = "QEdge",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = 50.dp)
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
    modifier: Modifier = Modifier
) {
    val colors = QEdgeTheme.colors
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
    val statusColor = when (updateStatus) {
        HomeUpdateStatus.LATEST -> colors.accentGreen
        HomeUpdateStatus.ERROR -> colors.accentRed
        else -> colors.accentBlue
    }
    val horizontalPadding = if (compactWidth) 18.dp else 24.dp

    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(horizontal = horizontalPadding)
                .padding(bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))

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
                fontSize = if (compactWidth) 22.sp else 25.sp,
                lineHeight = if (compactWidth) 30.sp else 33.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.weight(1f))

            // 状态文字
            Text(
                text = statusText,
                color = statusColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            // 英文文案
            SparkFooter()
        }
    }
}

@Composable
private fun SparkFooter(modifier: Modifier = Modifier) {
    val colors = QEdgeTheme.colors
    val gradient = Brush.horizontalGradient(
        listOf(
            colors.textSecondary.copy(alpha = 0.12f),
            colors.textSecondary.copy(alpha = if (colors.isDark) 0.58f else 0.48f),
            colors.textSecondary.copy(alpha = 0.12f)
        )
    )

    Text(
        text = "A tiny spark can start a prairie fire",
        modifier = modifier,
        style = TextStyle(
            brush = gradient,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.6.sp
        )
    )
}