package me.lengyu.qedge.ui.components.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.lengyu.qedge.ui.core.theme.QEdgeTheme

@Composable
fun CenterDialogContainerNoButton(title: String, content: @Composable () -> Unit) {
    val colors = QEdgeTheme.colors
    
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(colors.cardBackground)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary,
            modifier = Modifier.fillMaxWidth()
        )
        content()
    }
}