package me.lengyu.qedge.ui.components.molecules

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.lengyu.qedge.R
import me.lengyu.qedge.ui.components.atoms.QEdgeCard
import me.lengyu.qedge.ui.core.theme.QEdgeTheme

@Composable
fun QEdgeTopBar(
    title: String,
    modifier: Modifier = Modifier,
    showBackButton: Boolean = false,
    onBackClick: () -> Unit = {},
    isDarkTheme: Boolean = false,
    showThemeButton: Boolean = true,
    onThemeToggle: () -> Unit = {},
    showCreateButton: Boolean = false,
    onCreateClick: () -> Unit = {},
    showDocButton: Boolean = false,
    onDocClick: () -> Unit = {},
    showUpdateLogButton: Boolean = false,
    onUpdateLogClick: () -> Unit = {},
    actions: @Composable () -> Unit = {}
) {
    val colors = QEdgeTheme.colors
    val rotation by animateFloatAsState(
        targetValue = if (isDarkTheme) 180f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "themeRotation"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showBackButton) {
            QEdgeCard(
                modifier = Modifier.size(40.dp),
                animateContentSize = false,
                onClick = onBackClick
            ) {
                Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                    Icon(
                        painterResource(R.drawable.ic_chevron_right),
                        "返回",
                        Modifier.size(24.dp).rotate(180f),
                        colors.textPrimary
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
        }

        Text(
            text = title,
            fontSize = if (showBackButton) 20.sp else 28.sp,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary,
            modifier = Modifier.weight(1f)
        )

        actions()

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (showCreateButton) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.accentGreen)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(color = colors.ripple),
                            onClick = onCreateClick
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("+", fontSize = 22.sp, color = androidx.compose.ui.graphics.Color.White)
                }
                Spacer(modifier = Modifier.width(10.dp))
            }

            if (showDocButton) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.cardBackground)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(color = colors.ripple),
                            onClick = onDocClick
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("📖", fontSize = 18.sp)
                }
                Spacer(modifier = Modifier.width(10.dp))
            }

            if (showUpdateLogButton) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.cardBackground)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(color = colors.ripple),
                            onClick = onUpdateLogClick
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("📝", fontSize = 18.sp)
                }
                Spacer(modifier = Modifier.width(10.dp))
            }

            if (showThemeButton) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.cardBackground)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(color = colors.ripple),
                            onClick = onThemeToggle
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painterResource(if (isDarkTheme) R.drawable.ic_sun else R.drawable.ic_moon),
                        "Toggle theme",
                        Modifier
                            .size(24.dp)
                            .rotate(rotation),
                        colors.textPrimary
                    )
                }
            }
        }
    }
}

@Composable
fun TopBarIconButton(iconRes: Int, contentDescription: String, onClick: () -> Unit) {
    val colors = QEdgeTheme.colors
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(colors.cardBackground)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = colors.ripple),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painterResource(iconRes),
            contentDescription,
            Modifier.size(24.dp),
            colors.textPrimary
        )
    }
}