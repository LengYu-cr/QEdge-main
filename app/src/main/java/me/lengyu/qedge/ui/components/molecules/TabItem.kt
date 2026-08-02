package me.lengyu.qedge.ui.components.molecules

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import me.lengyu.qedge.ui.core.theme.QEdgeTheme

@Composable
fun TabItem(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = QEdgeTheme.colors

    Text(
        text = text,
        fontSize = 14.sp,
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
        color = if (isSelected) colors.accentGreen else colors.textSecondary,
        modifier = modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        )
    )
}