package me.lengyu.qedge.ui.pages.file

import android.media.MediaPlayer
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.lengyu.qedge.R
import me.lengyu.qedge.ui.components.atoms.QEdgeCard
import me.lengyu.qedge.ui.components.molecules.QEdgeTopBar
import me.lengyu.qedge.ui.core.theme.Dimens
import me.lengyu.qedge.ui.core.theme.QEdgeTheme
import java.io.File
import java.util.concurrent.TimeUnit

@Composable
fun AudioPlayerScreen(
    filePath: String,
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = QEdgeTheme.colors
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableStateOf(0) }
    var duration by remember { mutableStateOf(0) }
    var fileName by remember { mutableStateOf("") }
    var isPrepared by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    val mediaPlayer = remember { MediaPlayer() }

    LaunchedEffect(filePath) {
        val file = File(filePath)
        fileName = file.name
        try {
            mediaPlayer.reset()
            mediaPlayer.setDataSource(filePath)
            mediaPlayer.prepareAsync()
            mediaPlayer.setOnPreparedListener {
                duration = it.duration
                isPrepared = true
            }
            mediaPlayer.setOnCompletionListener {
                isPlaying = false
                currentPosition = 0
            }
            mediaPlayer.setOnErrorListener { _, _, _ ->
                errorMessage = "播放失败"
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            errorMessage = "加载失败: ${e.message}"
        }
    }

    DisposableEffect(Unit) {
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val updateRunnable = object : Runnable {
            override fun run() {
                if (isPlaying && isPrepared) {
                    try {
                        currentPosition = mediaPlayer.currentPosition
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                handler.postDelayed(this, 500)
            }
        }
        handler.postDelayed(updateRunnable, 500)

        onDispose {
            handler.removeCallbacks(updateRunnable)
            try {
                mediaPlayer.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun togglePlay() {
        if (!isPrepared) return
        try {
            if (isPlaying) {
                mediaPlayer.pause()
            } else {
                mediaPlayer.start()
            }
            isPlaying = !isPlaying
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun seekTo(position: Int) {
        if (!isPrepared) return
        try {
            mediaPlayer.seekTo(position)
            currentPosition = position
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            QEdgeTopBar(
                title = "音乐播放",
                showBackButton = true,
                onBackClick = onBackClick,
                isDarkTheme = isDarkTheme,
                onThemeToggle = onThemeToggle,
                actions = {}
            )

            Spacer(modifier = Modifier.height(24.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                QEdgeCard(
                    modifier = Modifier
                        .size(200.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🎵",
                            fontSize = 80.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = fileName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "音频文件",
                    fontSize = 13.sp,
                    color = colors.textSecondary
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            QEdgeCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Slider(
                        value = if (duration > 0) currentPosition.toFloat() else 0f,
                        onValueChange = { seekTo(it.toInt()) },
                        valueRange = 0f..(if (duration > 0) duration.toFloat() else 100f),
                        colors = SliderDefaults.colors(
                            thumbColor = colors.accentBlue,
                            activeTrackColor = colors.accentBlue,
                            inactiveTrackColor = colors.textSecondary.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatTime(currentPosition),
                            fontSize = 12.sp,
                            color = colors.textSecondary
                        )
                        Text(
                            text = formatTime(duration),
                            fontSize = 12.sp,
                            color = colors.textSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PlayControlButton(
                    icon = "⏮️",
                    onClick = { seekTo((currentPosition - 10000).coerceAtLeast(0)) }
                )

                Spacer(modifier = Modifier.width(32.dp))

                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(colors.accentBlue)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = rememberRipple(color = Color.White.copy(alpha = 0.3f)),
                            onClick = { togglePlay() }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isPlaying) "⏸" else "▶",
                        fontSize = 32.sp,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(32.dp))

                PlayControlButton(
                    icon = "⏭️",
                    onClick = { seekTo((currentPosition + 10000).coerceAtMost(duration)) }
                )
            }

            if (errorMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = errorMessage,
                    fontSize = 13.sp,
                    color = colors.accentRed,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun PlayControlButton(
    icon: String,
    onClick: () -> Unit
) {
    val colors = QEdgeTheme.colors
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(colors.cardBackground)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(color = colors.ripple),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = icon,
            fontSize = 24.sp
        )
    }
}

private fun formatTime(ms: Int): String {
    if (ms <= 0) return "0:00"
    val minutes = TimeUnit.MILLISECONDS.toMinutes(ms.toLong())
    val seconds = TimeUnit.MILLISECONDS.toSeconds(ms.toLong()) % 60
    return String.format("%d:%02d", minutes, seconds)
}
