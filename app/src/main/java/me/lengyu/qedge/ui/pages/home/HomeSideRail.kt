package me.lengyu.qedge.ui.pages.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import me.lengyu.qedge.ui.core.theme.QEdgeTheme
import java.time.LocalTime
import kotlin.math.roundToInt

internal data class HomeRailAction(
    val iconRes: Int,
    val label: String,
    val emphasized: Boolean = false,
    val showDividerBefore: Boolean = false,
    val tint: Color? = null,
    val onClick: () -> Unit
)

@Composable
internal fun HomeScaffold(
    currentTime: LocalTime,
    actions: List<HomeRailAction>,
    modifier: Modifier = Modifier,
    content: @Composable (ToggleDrawer: () -> Unit) -> Unit
) {
    val colors = QEdgeTheme.colors

    val railBrush = if (colors.isDark) {
        Brush.verticalGradient(
            listOf(Color(0xFF203348), Color(0xFF25283A), Color(0xFF171D2B))
        )
    } else {
        Brush.verticalGradient(
            listOf(Color(0xFFA9D7EB), Color(0xFFD8DCE8), Color(0xFFBBC1D3))
        )
    }
    val railContentColor = if (colors.isDark) Color.White else Color(0xFF29445A)

    var drawerOpen by remember { mutableStateOf(false) }

    val drawerWidthDp = 200.dp

    // 抽屉偏移动画
    val drawerOffset by animateDpAsState(
        targetValue = if (drawerOpen) 0.dp else -drawerWidthDp,
        animationSpec = tween(durationMillis = 380, easing = FastOutSlowInEasing),
        label = "drawerOffset"
    )

    // 内容区偏移动画（抽屉打开时内容右移一点）
    val contentOffset by animateDpAsState(
        targetValue = if (drawerOpen) drawerWidthDp * 0.18f else 0.dp,
        animationSpec = tween(durationMillis = 380, easing = FastOutSlowInEasing),
        label = "contentOffset"
    )

    // 遮罩透明度
    val scrimAlpha by animateFloatAsState(
        targetValue = if (drawerOpen) 0.42f else 0f,
        animationSpec = tween(durationMillis = 380, easing = FastOutSlowInEasing),
        label = "scrimAlpha"
    )

    // 内容区圆角动画（打开时带圆角）
    val contentCornerRadius by animateDpAsState(
        targetValue = if (drawerOpen) 28.dp else 0.dp,
        animationSpec = tween(durationMillis = 380, easing = FastOutSlowInEasing),
        label = "contentRadius"
    )

    // 内容区缩放动画
    val contentScale by animateFloatAsState(
        targetValue = if (drawerOpen) 0.94f else 1f,
        animationSpec = tween(durationMillis = 380, easing = FastOutSlowInEasing),
        label = "contentScale"
    )

    val toggleDrawer: () -> Unit = { drawerOpen = !drawerOpen }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.cardBackground)
    ) {
        // ========== 主内容区 ==========
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(contentOffset.roundToPx(), 0) }
                .scale(contentScale)
                .shadow(if (drawerOpen) 24.dp else 0.dp, RoundedCornerShape(contentCornerRadius))
                .clip(RoundedCornerShape(contentCornerRadius))
                .background(colors.cardBackground)
        ) {
            content(toggleDrawer)
        }

        // ========== 遮罩层 ==========
        if (scrimAlpha > 0.001f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = scrimAlpha))
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null,
                        onClick = { drawerOpen = false }
                    )
            )
        }

        // ========== 侧滑抽屉 ==========
        Box(
            modifier = Modifier
                .width(drawerWidthDp)
                .fillMaxHeight()
                .offset { IntOffset(drawerOffset.roundToPx(), 0) }
                .background(railBrush)
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            HomeSideRail(
                currentTime = currentTime,
                contentColor = railContentColor,
                actions = actions,
                onActionClick = { drawerOpen = false },
                modifier = Modifier.fillMaxSize()
            )
        }

        // ========== 左边缘滑动手势打开抽屉 ==========
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(12.dp)
                .align(Alignment.CenterStart)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = { drawerOpen = true },
                        onHorizontalDrag = { _, dragAmount ->
                            if (dragAmount > 30f) drawerOpen = true
                        }
                    )
                }
        )
    }
}

@Composable
private fun HomeSideRail(
    currentTime: LocalTime,
    contentColor: Color,
    actions: List<HomeRailAction>,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val batteryState = rememberHomeBatteryState()

    Column(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 文案
        Text(
            text = "赠君茉莉，",
            color = contentColor,
            fontSize = 16.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.Light
        )
        Text(
            text = "愿君莫离。",
            color = contentColor.copy(alpha = 0.78f),
            fontSize = 16.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.Light
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 电池
        HomeBatteryIndicator(
            batteryState = batteryState,
            contentColor = contentColor
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 分隔线
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(1.dp)
                .background(contentColor.copy(alpha = 0.22f))
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 操作按钮
        LazyColumn(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.Bottom)
        ) {
            items(actions, key = { it.label }) { action ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (action.showDividerBefore) {
                        Box(
                            modifier = Modifier
                                .padding(bottom = 14.dp)
                                .width(36.dp)
                                .height(1.dp)
                                .background(contentColor.copy(alpha = 0.22f))
                        )
                    }
                    RailActionButton(
                        action = action,
                        contentColor = contentColor,
                        onClick = {
                            action.onClick()
                            onActionClick()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeBatteryIndicator(
    batteryState: HomeBatteryState,
    contentColor: Color
) {
    val fraction = (batteryState.level ?: 0) / 100f
    val fillColor = when {
        batteryState.isCharging -> Color(0xFF34C759)
        (batteryState.level ?: 100) <= 20 -> Color(0xFFFF5A52)
        else -> contentColor.copy(alpha = 0.84f)
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .width(52.dp)
                .height(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(48.dp)
                    .fillMaxHeight()
                    .border(
                        width = 1.4.dp,
                        color = contentColor.copy(alpha = 0.72f),
                        shape = RoundedCornerShape(6.dp)
                    )
                    .padding(3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction)
                        .clip(RoundedCornerShape(3.dp))
                        .background(fillColor)
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(3.dp)
                    .height(9.dp)
                    .clip(RoundedCornerShape(topEnd = 2.dp, bottomEnd = 2.dp))
                    .background(contentColor.copy(alpha = 0.62f))
            )

            if (batteryState.isCharging) {
                Text(
                    text = "⚡",
                    modifier = Modifier.align(Alignment.Center),
                    color = contentColor,
                    fontSize = 9.sp,
                    lineHeight = 9.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text = batteryState.level?.let { "$it%" } ?: "--%",
            color = contentColor.copy(alpha = 0.78f),
            fontSize = 11.sp,
            lineHeight = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun RailActionButton(
    action: HomeRailAction,
    contentColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(
                if (action.emphasized) contentColor.copy(alpha = 0.16f)
                else contentColor.copy(alpha = 0.05f)
            )
            .clickable(
                role = Role.Button,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(action.iconRes),
            contentDescription = action.label,
            modifier = Modifier.size(24.dp),
            tint = action.tint ?: contentColor
        )
    }
}

@Composable
internal fun rememberHomeTime(): LocalTime {
    var currentTime by remember { mutableStateOf(LocalTime.now()) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = LocalTime.now()
            val untilNextMinute = 60_000L - (System.currentTimeMillis() % 60_000L) + 50L
            delay(untilNextMinute)
        }
    }

    return currentTime
}