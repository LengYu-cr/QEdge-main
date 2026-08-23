package me.lengyu.qedge.ui.components.dialogs

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.lengyu.qedge.ui.core.compatibility.XposedComposeDialog
import me.lengyu.qedge.ui.core.theme.QEdgeTheme

/**
 * 一个操作项：显示标题、可选副标题，点击回调。
 */
data class RepeatMsgAction(
    val title: String,
    val subtitle: String? = null,
    val onClick: () -> Unit
)

/**
 * 消息复读长按后弹出的操作列表弹窗（Compose，运行在宿主 QQ 进程）。
 * 通过传入的 actions 动态渲染，调用方按 picList/videoList/pttList 决定包含哪些项。
 *
 * 注意：操作项的 onClick 会在本弹窗完全关闭之后再执行（通过 pendingAction 延后），
 * 避免"旧弹窗还未 dismiss 就 show 新弹窗"造成的 window token 冲突导致闪退。
 */
class RepeatMsgActionDialog(
    context: Context,
    private val title: String,
    private val actions: List<RepeatMsgAction>
) : XposedComposeDialog(context) {

    private val handler = android.os.Handler(context.mainLooper)

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

    /**
     * 关闭本弹窗，并在其完全 dismiss 之后再执行操作。
     * dismissWithAnimation 有约 200ms 的关闭动画，这里延迟 260ms 再执行，
     * 确保旧弹窗 window 已分离，避免 show 新弹窗时的 token 冲突闪退。
     */
    private fun dismissThen(action: () -> Unit) {
        dismissWithAnimation()
        handler.postDelayed({ runCatching { action() } }, 260)
    }

    @Composable
    override fun DialogContent() {
        QEdgeTheme {
            RepeatMsgActionContent(
                visible = isVisible,
                title = title,
                actions = actions,
                onDismiss = ::dismissWithAnimation,
                onActionClick = { action -> dismissThen(action) }
            )
        }
    }
}

@Composable
private fun RepeatMsgActionContent(
    visible: Boolean,
    title: String,
    actions: List<RepeatMsgAction>,
    onDismiss: () -> Unit,
    onActionClick: (() -> Unit) -> Unit
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
                        .fillMaxWidth(0.86f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(colors.cardBackground)
                        .padding(vertical = 20.dp)
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
                            .padding(horizontal = 24.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 360.dp)
                    ) {
                        itemsIndexed(actions) { index, action ->
                            if (index > 0) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 20.dp),
                                    thickness = 0.5.dp,
                                    color = colors.textSecondary.copy(alpha = 0.15f)
                                )
                            }
                            ActionRow(
                                action = action,
                                titleColor = colors.textPrimary,
                                subtitleColor = colors.textSecondary,
                                onClick = {
                                    // 关闭当前弹窗后再执行操作，避免弹窗叠弹窗导致闪退
                                    onActionClick(action.onClick)
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(colors.accentBlue)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onDismiss
                            )
                            .padding(vertical = 13.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "取消",
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

@Composable
private fun ActionRow(
    action: RepeatMsgAction,
    titleColor: Color,
    subtitleColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = action.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = titleColor
            )
            if (!action.subtitle.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = action.subtitle,
                    fontSize = 12.sp,
                    color = subtitleColor
                )
            }
        }
    }
}
