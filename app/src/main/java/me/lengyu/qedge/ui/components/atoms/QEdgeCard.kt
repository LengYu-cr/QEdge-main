package me.lengyu.qedge.ui.components.atoms

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import me.lengyu.qedge.ui.core.theme.Dimens
import me.lengyu.qedge.ui.core.theme.QEdgeTheme

/**
 * 卡片内部点击区（手风琴标题行等）上报按压状态用的通道。
 * 卡片自己提供，内部可点击元素复用同一个 source，
 * 这样按下标题行时整张卡片会一起做按压回弹。
 */
internal val LocalCardPressSource = staticCompositionLocalOf<MutableInteractionSource?> { null }

@Composable
fun QEdgeCard(
    modifier: Modifier = Modifier,
    glass: Boolean = false,
    animateContentSize: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    if (glass) {
        QEdgeGlassCard(modifier, animateContentSize, onClick, content)
        return
    }

    val shape = RoundedCornerShape(Dimens.CardCornerRadius)
    val colors = QEdgeTheme.colors
    val dark = colors.isDark

    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = 0.3f, stiffness = 1000f),
        label = "cardPressScale"
    )
    val pressGlow by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = 900f),
        label = "cardPressGlow"
    )

    val surfaceBrush = if (dark) Brush.verticalGradient(
        0f to Color.White.copy(alpha = 0.125f),
        0.5f to Color.White.copy(alpha = 0.095f),
        1f to Color.White.copy(alpha = 0.075f)
    ) else Brush.verticalGradient(
        0f to Color.White.copy(alpha = 0.90f),
        0.5f to Color.White.copy(alpha = 0.82f),
        1f to Color.White.copy(alpha = 0.74f)
    )

    val rimBrush = if (dark) Brush.linearGradient(
        listOf(
            Color.White.copy(alpha = 0.32f),
            Color.White.copy(alpha = 0.08f),
            Color.White.copy(alpha = 0.03f)
        )
    ) else Brush.linearGradient(
        listOf(
            Color.White.copy(alpha = 1f),
            Color(0x0D000000),
            Color(0x17000000)
        )
    )

    val glowColors = if (dark) listOf(
        Color.White.copy(alpha = 0.22f),
        Color.White.copy(alpha = 0.06f),
        Color.Transparent
    ) else listOf(
        Color.Black.copy(alpha = 0.05f),
        Color.Black.copy(alpha = 0.015f),
        Color.Transparent
    )

    val glossAlpha = if (dark) 0.14f else 0.5f

    CompositionLocalProvider(LocalCardPressSource provides interactionSource) {
        Box(
            modifier = modifier
                .graphicsLayer {
                    scaleX = pressScale
                    scaleY = pressScale
                }
                .shadow(
                    elevation = if (dark) 0.dp else 5.dp,
                    shape = shape,
                    ambientColor = colors.textSecondary.copy(alpha = 0.10f),
                    spotColor = colors.textSecondary.copy(alpha = 0.10f)
                )
                .clip(shape)
                .background(brush = surfaceBrush, shape = shape)
                .drawBehind {
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = glossAlpha),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.2f, -size.height * 0.15f),
                            radius = size.maxDimension * 0.9f
                        )
                    )
                    if (pressGlow > 0.001f) {
                        drawRect(
                            brush = Brush.linearGradient(colors = glowColors),
                            alpha = pressGlow
                        )
                    }
                }
                .border(width = 1.dp, brush = rimBrush, shape = shape)
                .then(
                    if (onClick != null) Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = ripple(color = colors.ripple),
                        onClick = onClick
                    ) else Modifier
                )
                .then(
                    if (animateContentSize) Modifier
                        .heightIn(min = 40.dp)
                        .animateContentSize(
                            animationSpec = spring(
                                dampingRatio = 0.5f,
                                stiffness = 400f,
                                visibilityThreshold = IntSize(2, 2)
                            )
                        )
                    else Modifier
                ),
            content = content
        )
    }
}

/**
 * 液态玻璃卡片（纯 Compose 实现）：
 * 光从顶部进入、在玻璃下沿聚出亮光 —— 霜面下亮上暗 + 下沿棱边高光 +
 * 底部内辉光 + 下方软投影，受光方向对齐库的 45° 斜光与底部导航条。
 * 不走库的 Portal 采样（页内卡片会被搬出 Compose 盖在整页上，
 * 产生白坨与文字重影）。
 */
@Composable
private fun QEdgeGlassCard(
    modifier: Modifier,
    animateContentSize: Boolean,
    onClick: (() -> Unit)?,
    content: @Composable BoxScope.() -> Unit
) {
    val colors = QEdgeTheme.colors
    val dark = colors.isDark
    val shape = RoundedCornerShape(Dimens.CardCornerRadius)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = 0.3f, stiffness = 1000f),
        label = "glassCardPress"
    )

    /* 霜面：光在玻璃下沿聚拢 —— 底部更亮、顶部收敛 */
    val surface = if (dark) Brush.verticalGradient(
        listOf(Color.White.copy(alpha = 0.07f), Color.White.copy(alpha = 0.16f))
    ) else Brush.verticalGradient(
        listOf(Color.White.copy(alpha = 0.66f), Color.White.copy(alpha = 0.92f))
    )

    /* 棱边：下沿出射光最亮、上沿只留一道弱 specular（库 45° 斜光方向） */
    val rim = Brush.verticalGradient(
        listOf(
            Color.White.copy(alpha = if (dark) 0.12f else 0.30f),
            Color.White.copy(alpha = if (dark) 0.60f else 0.95f)
        )
    )

    /* 深色底衬：压住背景照片亮部，让文字“沉进”玻璃而不是浮在照片上（贴纸感）。
       无背景图时底衬叠在纯黑主题背景上不可见，普通暗色主题外观不变 */
    val underScrim = Brush.verticalGradient(
        listOf(
            Color.Black.copy(alpha = if (dark) 0.34f else 0f),
            Color.Black.copy(alpha = if (dark) 0.50f else 0f)
        )
    )

    CompositionLocalProvider(LocalCardPressSource provides interactionSource) {
        Box(
            modifier = modifier
                .shadow(
                    elevation = 11.dp,
                    shape = shape,
                    clip = false,
                    ambientColor = Color.Black.copy(alpha = if (dark) 0.42f else 0.16f),
                    spotColor = Color.Black.copy(alpha = if (dark) 0.42f else 0.16f)
                )
                .graphicsLayer {
                    scaleX = pressScale
                    scaleY = pressScale
                }
                .clip(shape)
                .background(underScrim)
                .background(surface)
                .border(width = 1.dp, brush = rim, shape = shape)
                .drawBehind {
                    /* 底部内辉光：光穿过玻璃从下沿射出的一条亮带 */
                    val h = 4.dp.toPx()
                    val glow = Color.White.copy(alpha = if (dark) 0.35f else 0.85f)
                    drawRect(
                        brush = Brush.verticalGradient(
                            0f to Color.Transparent,
                            ((size.height - h) / size.height).coerceIn(0f, 1f) to Color.Transparent,
                            1f to glow
                        )
                    )
                }
                .then(
                    if (onClick != null) Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = ripple(color = colors.ripple),
                        onClick = onClick
                    ) else Modifier
                )
                .then(
                    if (animateContentSize) Modifier
                        .heightIn(min = 40.dp)
                        .animateContentSize(
                            animationSpec = spring(
                                dampingRatio = 0.5f,
                                stiffness = 400f,
                                visibilityThreshold = IntSize(2, 2)
                            )
                        )
                    else Modifier
                )
        ) {
            Box(content = content)
        }
    }
}
