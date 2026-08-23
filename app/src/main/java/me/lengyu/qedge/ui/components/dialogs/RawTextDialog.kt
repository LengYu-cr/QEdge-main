package me.lengyu.qedge.ui.components.dialogs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.Gravity
import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.lengyu.qedge.ui.core.compatibility.XposedComposeDialog
import me.lengyu.qedge.ui.core.theme.QEdgeTheme

/**
 * 原始文本查看弹窗：完整展示原始内容，不做任何格式化或省略。
 * 内容区域支持竖向 + 横向滚动，并包裹在 SelectionContainer 中，
 * 用户可自由长按选中任意片段进行复制；底部提供"复制全部"按钮。
 *
 * 背景不使用暗色遮罩，且不覆盖到系统状态栏区域。
 */
class RawTextDialog(
    context: Context,
    private val title: String,
    private val content: String
) : XposedComposeDialog(context) {

    override fun configureWindow() {
        super.configureWindow()
        window?.apply {
            setGravity(Gravity.CENTER)
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
            // 不使用暗色遮罩背景（状态栏透明由基类统一处理）
            setDimAmount(0f)
        }
    }

    @Composable
    override fun DialogContent() {
        QEdgeTheme {
            RawTextContent(
                visible = isVisible,
                title = title,
                content = content,
                onCopyAll = { copyToClipboard(context, content) },
                onDismiss = ::dismissWithAnimation
            )
        }
    }

    private fun copyToClipboard(ctx: Context, text: String) {
        val clipboard = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText("raw_text", text))
    }
}

@Composable
private fun RawTextContent(
    visible: Boolean,
    title: String,
    content: String,
    onCopyAll: () -> Unit,
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
                .statusBarsPadding()
                .navigationBarsPadding()
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
                        .fillMaxWidth(0.9f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(colors.cardBackground)
                        .padding(vertical = 22.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {}
                ) {
                    Text(
                        text = title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 22.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // 内容：竖向滚动 + 自动换行 + 可选中复制
                    SelectionContainer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 80.dp, max = 460.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 22.dp)
                    ) {
                        Text(
                            text = content,
                            fontSize = 13.sp,
                            color = colors.textPrimary,
                            lineHeight = 19.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 22.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(colors.background)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = onDismiss
                                )
                                .padding(vertical = 13.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "关闭",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.textSecondary
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(colors.accentBlue)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = onCopyAll
                                )
                                .padding(vertical = 13.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "复制全部",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
