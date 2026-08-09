package me.lengyu.qedge.ui.pages.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import me.lengyu.qedge.ui.core.theme.QEdgeTheme
import java.time.LocalTime

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
    content: @Composable () -> Unit
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

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(railBrush)
    ) {
        val railWidth = (maxWidth * 0.18f).coerceIn(68.dp, 86.dp)

        Row(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            HomeSideRail(
                currentTime = currentTime,
                contentColor = railContentColor,
                actions = actions,
                modifier = Modifier
                    .width(railWidth)
                    .fillMaxHeight()
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(top = 8.dp, end = 8.dp, bottom = 8.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = 36.dp,
                            topEnd = 18.dp,
                            bottomStart = 36.dp,
                            bottomEnd = 18.dp
                        )
                    )
                    .background(colors.cardBackground)
            ) {
                content()
            }
        }
    }
}

@Composable
private fun HomeSideRail(
    currentTime: LocalTime,
    contentColor: Color,
    actions: List<HomeRailAction>,
    modifier: Modifier = Modifier
) {
    val batteryState = rememberHomeBatteryState()

    Column(
        modifier = modifier.padding(horizontal = 8.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = currentTime.hour.toString().padStart(2, '0'),
            color = contentColor,
            fontSize = 42.sp,
            lineHeight = 42.sp,
            fontWeight = FontWeight.Light
        )
        Text(
            text = "—",
            color = contentColor.copy(alpha = 0.72f),
            fontSize = 25.sp,
            lineHeight = 25.sp,
            fontWeight = FontWeight.Light
        )
        Text(
            text = currentTime.minute.toString().padStart(2, '0'),
            color = contentColor,
            fontSize = 42.sp,
            lineHeight = 42.sp,
            fontWeight = FontWeight.Light
        )

        Spacer(modifier = Modifier.height(16.dp))

        HomeBatteryIndicator(
            batteryState = batteryState,
            contentColor = contentColor
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.Bottom)
        ) {
            items(actions, key = { it.label }) { action ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (action.showDividerBefore) {
                        Box(
                            modifier = Modifier
                                .padding(bottom = 10.dp)
                                .width(30.dp)
                                .height(1.dp)
                                .background(contentColor.copy(alpha = 0.28f))
                        )
                    }
                    RailActionButton(action, contentColor)
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
                .width(48.dp)
                .height(22.dp)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(44.dp)
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
private fun RailActionButton(action: HomeRailAction, contentColor: Color) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(
                if (action.emphasized) contentColor.copy(alpha = 0.14f)
                else contentColor.copy(alpha = 0.04f)
            )
            .clickable(
                role = Role.Button,
                onClick = action.onClick
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
