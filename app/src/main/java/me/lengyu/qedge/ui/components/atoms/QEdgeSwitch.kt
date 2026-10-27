package me.lengyu.qedge.ui.components.atoms

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import me.lengyu.qedge.ui.core.theme.QEdgeTheme

@Composable
fun QEdgeSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val colors = QEdgeTheme.colors
    val trackColor by animateColorAsState(
        targetValue = if (checked) colors.switchTrackOn else colors.switchTrackOff,
        animationSpec = tween(durationMillis = 250),
        label = "trackColor"
    )
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 22.dp else 0.dp,
        animationSpec = spring(dampingRatio = 1f, stiffness = 700f),
        label = "thumbOffset"
    )
    /* 滑块启动瞬间横向拉伸、到位后回弹 —— 挤压拉伸的形变感 */
    val thumbStretch = remember { Animatable(1f) }
    var initialized by remember { mutableStateOf(false) }
    LaunchedEffect(checked) {
        if (!initialized) {
            initialized = true
            return@LaunchedEffect
        }
        thumbStretch.animateTo(1.16f, animationSpec = tween(durationMillis = 110))
        thumbStretch.animateTo(1f, animationSpec = spring(dampingRatio = 0.42f, stiffness = 520f))
    }

    Box(
        modifier = modifier
            .width(52.dp)
            .height(30.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(trackColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                onClick = { onCheckedChange(!checked) }
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .padding(start = 3.dp)
                .offset(x = thumbOffset)
                .size(24.dp)
                .graphicsLayer { scaleX = thumbStretch.value }
                .shadow(elevation = if (checked) 3.dp else 2.dp, shape = CircleShape, clip = false)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}