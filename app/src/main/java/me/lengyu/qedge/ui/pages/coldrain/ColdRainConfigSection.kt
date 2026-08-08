package me.lengyu.qedge.ui.pages.coldrain

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ripple
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import android.content.Context
import androidx.compose.ui.platform.LocalContext
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
import me.lengyu.qedge.ui.core.theme.AccentBlue
import me.lengyu.qedge.ui.core.theme.QEdgeTheme

@Composable
fun ColdRainConfigSection(
    onFullScreenClick: () -> Unit
) {
    val colors = QEdgeTheme.colors
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        ColdRainConfig.init(context)
    }

    var masterEnabled by remember { mutableStateOf(ColdRainConfig.isMasterEnabled) }
    var menuName by remember { mutableStateOf(ColdRainConfig.menuName) }
    var replyMode by remember { mutableStateOf(ColdRainConfig.replyMode) }
    var masterUin by remember { mutableStateOf(ColdRainConfig.masterUin) }

    var showMenuNameDialog by remember { mutableStateOf(false) }
    var showReplyModeDialog by remember { mutableStateOf(false) }
    var showMasterUinDialog by remember { mutableStateOf(false) }

    QEdgeCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onFullScreenClick),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(AccentBlue),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painterResource(R.drawable.ic_launcher),
                        "冷雨Java",
                        Modifier.size(28.dp),
                        Color.White
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "冷雨Java",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        if (masterEnabled) "运行中" else "已关闭",
                        fontSize = 13.sp,
                        color = if (masterEnabled) AccentBlue else colors.textSecondary
                    )
                }
                Switch(
                    checked = masterEnabled,
                    onCheckedChange = {
                        masterEnabled = it
                        ColdRainConfig.setBoolean("master_enabled", it)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = AccentBlue,
                        checkedTrackColor = AccentBlue.copy(alpha = 0.3f)
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MiniConfigItem(
                    title = "菜单词",
                    value = menuName,
                    onClick = { showMenuNameDialog = true }
                )
                MiniConfigItem(
                    title = "回复模式",
                    value = when (replyMode) {
                        "text" -> "文字"
                        "card" -> "卡片"
                        "image" -> "图片"
                        "forward" -> "转发"
                        "markdown" -> "MD"
                        "guanjia" -> "管家"
                        else -> "文字"
                    },
                    onClick = { showReplyModeDialog = true }
                )
                MiniConfigItem(
                    title = "主人QQ",
                    value = masterUin.ifEmpty { "-" },
                    onClick = { showMasterUinDialog = true }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "点击进入详细设置",
                fontSize = 12.sp,
                color = colors.textSecondary,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }

    if (showMenuNameDialog) {
        var inputText by remember { mutableStateOf(menuName) }
        AlertDialog(
            onDismissRequest = { showMenuNameDialog = false },
            title = { Text("菜单召唤词") },
            text = {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    singleLine = true,
                    label = { Text("请输入召唤词") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (inputText.isNotBlank()) {
                        menuName = inputText.trim()
                        ColdRainConfig.setString("menu_name", menuName)
                    }
                    showMenuNameDialog = false
                }) {
                    Text("确定", color = AccentBlue)
                }
            },
            dismissButton = {
                TextButton(onClick = { showMenuNameDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (showReplyModeDialog) {
        val modes = listOf(
            "text" to "文字模式",
            "card" to "卡片模式",
            "image" to "图片模式",
            "forward" to "转发模式",
            "markdown" to "MarkDown",
            "guanjia" to "管家模式"
        )
        AlertDialog(
            onDismissRequest = { showReplyModeDialog = false },
            title = { Text("回复模式") },
            text = {
                Column {
                    modes.forEach { (key, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    replyMode = key
                                    ColdRainConfig.setString("reply_mode", key)
                                    showReplyModeDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                label,
                                fontSize = 15.sp,
                                color = if (replyMode == key) AccentBlue else colors.textPrimary
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            if (replyMode == key) {
                                Icon(
                                    painterResource(R.drawable.ic_check_circle),
                                    "selected",
                                    Modifier.size(20.dp),
                                    AccentBlue
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showReplyModeDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (showMasterUinDialog) {
        var inputText by remember { mutableStateOf(masterUin) }
        AlertDialog(
            onDismissRequest = { showMasterUinDialog = false },
            title = { Text("主人QQ") },
            text = {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    singleLine = true,
                    label = { Text("请输入主人QQ号") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    masterUin = inputText.trim()
                    ColdRainConfig.setString("master_uin", masterUin)
                    showMasterUinDialog = false
                }) {
                    Text("确定", color = AccentBlue)
                }
            },
            dismissButton = {
                TextButton(onClick = { showMasterUinDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun MiniConfigItem(
    title: String,
    value: String,
    onClick: () -> Unit
) {
    val colors = QEdgeTheme.colors

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(colors.cardBackground)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = colors.ripple),
                onClick = onClick
            )
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            title,
            fontSize = 12.sp,
            color = colors.textSecondary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = colors.textPrimary
        )
    }
}
