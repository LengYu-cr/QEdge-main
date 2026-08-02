package me.lengyu.qedge.ui.components.dialogs

import android.content.Context
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.lengyu.qedge.R
import me.lengyu.qedge.ui.core.compatibility.XposedComposeDialog
import me.lengyu.qedge.ui.core.theme.QEdgeTheme

class WelcomeDialog(
    context: Context,
    private val currentUin: String,
    private val initialPassword: String
) : XposedComposeDialog(context) {

    override fun configureWindow() {
        super.configureWindow()
        window?.apply {
            setGravity(Gravity.CENTER)
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
            setDimAmount(0.6f)
        }
    }

    @Composable
    override fun DialogContent() {
        QEdgeTheme {
            WelcomeContent(
                visible = isVisible,
                currentUin = currentUin,
                initialPassword = initialPassword,
                onDismiss = ::dismissWithAnimation
            )
        }
    }
}

@Composable
private fun WelcomeContent(
    visible: Boolean,
    currentUin: String,
    initialPassword: String,
    onDismiss: () -> Unit
) {
    val colors = QEdgeTheme.colors

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(200)),
        exit = fadeOut(tween(200))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = scaleIn(tween(250, delayMillis = 50), initialScale = 0.92f) + fadeIn(tween(200)),
                exit = scaleOut(tween(150), targetScale = 0.92f) + fadeOut(tween(150))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(colors.cardBackground)
                        .padding(vertical = 24.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {}
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_launcher),
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier.size(48.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "欢迎使用 QEdge",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "后台地址：https://v.yuafeng.cn/QEdge/user/",
                            fontSize = 13.sp,
                            color = colors.textSecondary,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "账号：",
                                fontSize = 14.sp,
                                color = colors.textSecondary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = currentUin,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.textPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                painter = painterResource(R.drawable.ic_copy),
                                contentDescription = null,
                                tint = colors.accentBlue,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = {
                                            copyToClipboard(currentUin)
                                        }
                                    )
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "初始密码：",
                                fontSize = 14.sp,
                                color = colors.textSecondary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = initialPassword,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.textPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                painter = painterResource(R.drawable.ic_copy),
                                contentDescription = null,
                                tint = colors.accentBlue,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = {
                                            copyToClipboard(initialPassword)
                                        }
                                    )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "请妥善保管您的初始密码，并尽快登录后台修改！",
                        fontSize = 12.sp,
                        color = colors.textSecondary.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(colors.accentBlue)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onDismiss
                            )
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "我知道了",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

private fun copyToClipboard(text: String) {
    val context = me.lengyu.qedge.utils.HostInfo.getHostContext() ?: return
    val clipboard = ContextCompat.getSystemService(
        context,
        android.content.ClipboardManager::class.java
    )
    val clip = android.content.ClipData.newPlainText("QEdge", text)
    clipboard?.setPrimaryClip(clip)
    Toast.makeText(context, "已复制", Toast.LENGTH_SHORT).show()
}