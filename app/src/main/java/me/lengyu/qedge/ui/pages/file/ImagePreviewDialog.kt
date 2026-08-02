package me.lengyu.qedge.ui.pages.file

import android.graphics.BitmapFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import me.lengyu.qedge.R
import me.lengyu.qedge.ui.components.atoms.QEdgeCard
import me.lengyu.qedge.ui.core.theme.QEdgeTheme
import java.io.File

@Composable
fun ImagePreviewDialog(
    filePath: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = QEdgeTheme.colors
    var bitmap by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var fileName by remember { mutableStateOf("") }

    LaunchedEffect(filePath) {
        val file = File(filePath)
        fileName = file.name
        Thread {
            try {
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeFile(filePath, options)

                val sampleSize = calculateInSampleSize(options, 1920, 1920)
                options.inJustDecodeBounds = false
                options.inSampleSize = sampleSize

                val bmp = BitmapFactory.decodeFile(filePath, options)
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    bitmap = bmp.asImageBitmap()
                    isLoading = false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    errorMessage = "加载失败: ${e.message}"
                    isLoading = false
                }
            }
        }.start()
    }

    fun resetZoom() {
        scale = 1f
        offsetX = 0f
        offsetY = 0f
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(colors.cardBackground.copy(alpha = 0.5f))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = rememberRipple(color = colors.ripple),
                                onClick = onDismiss
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_chevron_right),
                            contentDescription = "返回",
                            modifier = Modifier
                                .size(24.dp)
                                .rotate(180f),
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = fileName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.weight(1f),
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(colors.cardBackground.copy(alpha = 0.5f))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = rememberRipple(color = colors.ripple),
                                onClick = { resetZoom() }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_refresh),
                            contentDescription = "重置",
                            modifier = Modifier.size(20.dp),
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        Text(
                            text = "加载中...",
                            fontSize = 14.sp,
                            color = colors.textSecondary
                        )
                    } else if (errorMessage.isNotEmpty()) {
                        Text(
                            text = errorMessage,
                            fontSize = 14.sp,
                            color = colors.accentRed
                        )
                    } else if (bitmap != null) {
                        androidx.compose.foundation.Image(
                            bitmap = bitmap!!,
                            contentDescription = fileName,
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    detectTransformGestures { _, pan, zoom, _ ->
                                        scale = (scale * zoom).coerceIn(0.5f, 5f)
                                        if (scale > 1f) {
                                            offsetX += pan.x
                                            offsetY += pan.y
                                        } else {
                                            offsetX = 0f
                                            offsetY = 0f
                                        }
                                    }
                                }
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale,
                                    translationX = offsetX,
                                    translationY = offsetY
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                ImageBottomBar(
                    scale = scale,
                    onZoomIn = { scale = (scale * 1.5f).coerceIn(0.5f, 5f) },
                    onZoomOut = { scale = (scale / 1.5f).coerceIn(0.5f, 5f) },
                    onReset = { resetZoom() }
                )
            }
        }
    }
}

@Composable
private fun ImageBottomBar(
    scale: Float,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onReset: () -> Unit
) {
    val colors = QEdgeTheme.colors

    QEdgeCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            BottomBarButton(
                iconRes = R.drawable.depress,
                label = "缩小",
                onClick = onZoomOut
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${String.format("%.1f", scale)}x",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "缩放",
                    fontSize = 11.sp,
                    color = colors.textSecondary
                )
            }

            BottomBarButton(
                iconRes = R.drawable.compress,
                label = "放大",
                onClick = onZoomIn
            )

            BottomBarButton(
                iconRes = R.drawable.ic_refresh,
                label = "重置",
                onClick = onReset
            )
        }
    }
}

@Composable
private fun BottomBarButton(
    iconRes: Int,
    label: String,
    onClick: () -> Unit
) {
    val colors = QEdgeTheme.colors
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(color = colors.ripple),
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = label,
            modifier = Modifier.size(22.dp),
            tint = colors.textPrimary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = colors.textSecondary
        )
    }
}

private fun calculateInSampleSize(
    options: android.graphics.BitmapFactory.Options,
    reqWidth: Int,
    reqHeight: Int
): Int {
    val height = options.outHeight
    val width = options.outWidth
    var inSampleSize = 1

    if (height > reqHeight || width > reqWidth) {
        val halfHeight = height / 2
        val halfWidth = width / 2

        while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
            inSampleSize *= 2
        }
    }

    return inSampleSize
}
