package me.lengyu.qedge.ui.core.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

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

@Composable
fun QEdgeTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
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