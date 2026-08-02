package me.lengyu.qedge.ui.components.dialogs

import android.content.Context
import android.view.Gravity
import android.view.ViewGroup
import android.view.Window
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.lengyu.qedge.R
import me.lengyu.qedge.ui.core.compatibility.XposedComposeDialog
import me.lengyu.qedge.ui.core.theme.AccentBlue
import me.lengyu.qedge.ui.core.theme.QEdgeTheme

data class PluginMenuItem(
    val key: String,
    val name: String,
    val description: String = "",
    val icon: String = "default",
    val action: () -> Unit
)

class PluginMenuDialog(
    context: Context,
    private val menuItems: List<PluginMenuItem>,
    private val title: String = "脚本菜单"
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
            PluginMenuContent(
                visible = isVisible,
                title = title,
                menuItems = menuItems,
                onDismiss = ::dismissWithAnimation
            )
        }
    }
}

@Composable
private fun PluginMenuContent(
    visible: Boolean,
    title: String,
    menuItems: List<PluginMenuItem>,
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
                        .padding(vertical = 20.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {}
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        items(menuItems) { item ->
                            PluginMenuItemCard(item, onDismiss)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(colors.background)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onDismiss
                            )
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "取消",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.textSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PluginMenuItemCard(item: PluginMenuItem, onDismiss: () -> Unit) {
    val colors = QEdgeTheme.colors

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.background)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    item.action()
                    onDismiss()
                }
            )
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(AccentBlue.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = getIconPainter(item.icon),
                    contentDescription = null,
                    tint = AccentBlue,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textPrimary
                )

                if (item.description.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.description,
                        fontSize = 12.sp,
                        color = colors.textSecondary,
                        maxLines = 1
                    )
                }
            }

            Icon(
                painter = painterResource(R.drawable.ic_more_horiz),
                contentDescription = null,
                tint = colors.textSecondary.copy(alpha = 0.4f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun getIconPainter(iconName: String): Painter {
    return when (iconName) {
        "send" -> painterResource(R.drawable.ic_send)
        "recall" -> painterResource(R.drawable.ic_reply)
        "translate" -> painterResource(R.drawable.ic_translate)
        "copy" -> painterResource(R.drawable.ic_copy)
        "forward" -> painterResource(R.drawable.card)
        "delete" -> painterResource(R.drawable.ic_delete)
        "star" -> painterResource(R.drawable.ic_star)
        "share" -> painterResource(R.drawable.ic_share)
        else -> painterResource(R.drawable.ic_logo_qq)
    }
}
