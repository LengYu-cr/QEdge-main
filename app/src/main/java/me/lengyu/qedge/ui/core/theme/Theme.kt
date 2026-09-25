package me.lengyu.qedge.ui.core.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import me.lengyu.qedge.utils.HostInfo
import me.lengyu.qedge.utils.ModuleConfig

private val LightColorScheme = lightColorScheme(
    primary = AccentBlue, onPrimary = Color.White,
    secondary = AccentGreen, onSecondary = Color.White,
    background = LightBackground, onBackground = LightTextPrimary,
    surface = LightCardBackground, onSurface = LightTextPrimary,
    surfaceVariant = LightCardBackground, onSurfaceVariant = LightTextSecondary,
    error = AccentRed, onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = AccentBlue, onPrimary = Color.White,
    secondary = AccentGreenDark, onSecondary = Color.White,
    background = DarkBackground, onBackground = DarkTextPrimary,
    surface = DarkCardBackground, onSurface = DarkTextPrimary,
    surfaceVariant = DarkCardBackground, onSurfaceVariant = DarkTextSecondary,
    error = AccentRed, onError = Color.White
)

@Stable
data class QEdgeColors(
    val background: Color,
    val cardBackground: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val ripple: Color,
    val switchTrackOn: Color,
    val switchTrackOff: Color,
    val accentGreen: Color,
    val accentBlue: Color,
    val accentRed: Color,
    val isDark: Boolean
)

val LocalQEdgeColors = staticCompositionLocalOf {
    QEdgeColors(
        LightBackground,
        LightCardBackground,
        LightTextPrimary,
        LightTextSecondary,
        LightRipple,
        SwitchTrackOn,
        SwitchTrackOff,
        AccentGreen,
        AccentBlue,
        AccentRed,
        false
    )
}

/** 背景图模式固定配色：有背景图时不再跟随明暗主题，统一走暗色玻璃风格 */
val ForcedDarkColors = QEdgeColors(
    background = DarkBackground,
    cardBackground = DarkCardBackground,
    textPrimary = DarkTextPrimary,
    // 背景图上 0xFF86868B 的灰字对比度不足看不清，提亮到 0xFFC7C7CC（仅背景图模式）
    textSecondary = Color(0xFFC7C7CC),
    ripple = DarkRipple,
    switchTrackOn = SwitchTrackOnDark,
    // 背景图模式：关闭态轨道用半透明，透出背景图，避免在照片上糊一块黑色
    switchTrackOff = Color.White.copy(alpha = 0.28f),
    accentGreen = AccentGreenDark,
    accentBlue = AccentBlue,
    accentRed = AccentRed,
    isDark = true
)

/** 背景图模式：开关打开且已选图（与 HomeScreen 的判定保持一致） */
fun isBgImageActive(): Boolean =
    ModuleConfig.getBoolean("bg_image_enabled", false) &&
        ModuleConfig.getString("bg_image_uri", "").isNotEmpty()

/**
 * 模块主题解析，与模块首页保持一致：
 * theme = -1 跟随系统/宿主夜间模式，0 强制亮色，1 强制暗色。
 *
 * 宿主进程内的弹窗（脚本菜单、媒体面板等）必须走这里，不能直接用 [HostInfo.isDarkTheme]，
 * 否则模块设为暗色而 QQ 处于亮色时会渲染成亮色背景。
 *
 * 注意：背景图模式不在这里强制暗色——页面的暗色由 HomeScreen 的 ForcedDarkColors 单独处理，
 * 弹窗应跟随用户主题配置（默认亮色），否则开启背景图后所有弹窗都会变成黑色。
 */
fun resolveDarkTheme(): Boolean = when {
    ModuleConfig.getInt("theme", -1) == 1 -> true
    ModuleConfig.getInt("theme", -1) == 0 -> false
    else -> HostInfo.isDarkTheme()
}

@Composable
fun QEdgeTheme(darkTheme: Boolean = resolveDarkTheme(), content: @Composable () -> Unit) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    @Composable
    fun animateAppColor(target: Color) = animateColorAsState(target, tween(300), label = "").value

    val qedgeColors = QEdgeColors(
        background = animateAppColor(if (darkTheme) DarkBackground else LightBackground),
        cardBackground = animateAppColor(if (darkTheme) DarkCardBackground else LightCardBackground),
        textPrimary = animateAppColor(if (darkTheme) DarkTextPrimary else LightTextPrimary),
        textSecondary = animateAppColor(if (darkTheme) DarkTextSecondary else LightTextSecondary),
        ripple = if (darkTheme) DarkRipple else LightRipple,
        switchTrackOn = if (darkTheme) SwitchTrackOnDark else SwitchTrackOn,
        switchTrackOff = if (darkTheme) SwitchTrackOffDark else SwitchTrackOff,
        accentGreen = if (darkTheme) AccentGreenDark else AccentGreen,
        accentBlue = AccentBlue,
        accentRed = AccentRed,
        isDark = darkTheme
    )

    CompositionLocalProvider(LocalQEdgeColors provides qedgeColors) {
        MaterialTheme(colorScheme, content = content)
    }
}

object QEdgeTheme {
    val colors: QEdgeColors @Composable get() = LocalQEdgeColors.current
}