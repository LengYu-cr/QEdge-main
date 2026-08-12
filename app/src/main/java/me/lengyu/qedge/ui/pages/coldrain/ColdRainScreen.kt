package me.lengyu.qedge.ui.pages.coldrain

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ripple
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.lengyu.qedge.R
import me.lengyu.qedge.ui.components.atoms.QEdgeCard
import me.lengyu.qedge.ui.components.atoms.QEdgeSwitch
import me.lengyu.qedge.ui.core.theme.AccentBlue
import me.lengyu.qedge.ui.core.theme.QEdgeTheme

@Composable
fun ColdRainScreen(
    modifier: Modifier = Modifier
) {
    val colors = QEdgeTheme.colors
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        ColdRainConfig.init(context)
    }

    var masterEnabled by remember { mutableStateOf(ColdRainConfig.isMasterEnabled) }
    var menuRestricted by remember { mutableStateOf(ColdRainConfig.menuRestricted) }
    var menuName by remember { mutableStateOf(ColdRainConfig.menuName) }
    var replyMode by remember { mutableStateOf(ColdRainConfig.replyMode) }
    var masterUin by remember { mutableStateOf(ColdRainConfig.masterUin) }
    var globalAdmins by remember { mutableStateOf(ColdRainConfig.globalAdmins) }
    var welcomeJoinMsg by remember { mutableStateOf(ColdRainConfig.welcomeJoinMsg) }
    var welcomeQuitMsg by remember { mutableStateOf(ColdRainConfig.welcomeQuitMsg) }
    var hourlyCustom by remember { mutableStateOf(ColdRainConfig.hourlyCustom) }
    val scrollState = rememberScrollState()

    var showMenuNameDialog by remember { mutableStateOf(false) }
    var showReplyModeDialog by remember { mutableStateOf(false) }
    var showMasterUinDialog by remember { mutableStateOf(false) }
    var showGlobalAdminsDialog by remember { mutableStateOf(false) }
    var showWelcomeJoinDialog by remember { mutableStateOf(false) }
    var showWelcomeQuitDialog by remember { mutableStateOf(false) }
    var showHourlyCustomDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QEdgeCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painterResource(R.drawable.logo),
                        "冷雨Java",
                        Modifier.size(48.dp),
                        tint = Color.Unspecified
                    )
                    Spacer(modifier = Modifier.width(16.dp))
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
                    QEdgeSwitch(
                        checked = masterEnabled,
                        onCheckedChange = {
                            masterEnabled = it
                            ColdRainConfig.setBoolean("master_enabled", it)
                        }
                    )
                }
            }

            QEdgeCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "基础配置",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    ConfigItemColdRain(
                        title = "菜单召唤词",
                        value = menuName,
                        onClick = { showMenuNameDialog = true }
                    )

                    ConfigItemColdRain(
                        title = "回复模式",
                        value = when (replyMode) {
                            "text" -> "文字模式"
                            "card" -> "卡片模式"
                            "image" -> "图片模式"
                            "forward" -> "转发模式"
                            "markdown" -> "MarkDown"
                            "guanjia" -> "管家模式"
                            else -> "文字模式"
                        },
                        onClick = { showReplyModeDialog = true }
                    )

                    SwitchItemColdRain(
                        title = "菜单限制",
                        description = "仅管理员可使用菜单",
                        checked = menuRestricted,
                        onCheckedChange = {
                            menuRestricted = it
                            ColdRainConfig.setBoolean("menu_restricted", it)
                        }
                    )

                    ConfigItemColdRain(
                        title = "主人QQ",
                        value = masterUin.ifEmpty { "未设置" },
                        onClick = { showMasterUinDialog = true }
                    )

                    ConfigItemColdRain(
                        title = "全局代管",
                        value = if (globalAdmins.isEmpty()) "未设置" else globalAdmins,
                        onClick = { showGlobalAdminsDialog = true }
                    )
                }
            }

            QEdgeCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "功能配置",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    ConfigItemColdRain(
                        title = "进群欢迎语",
                        value = if (welcomeJoinMsg.isEmpty()) "默认" else welcomeJoinMsg.take(20) + if (welcomeJoinMsg.length > 20) "..." else "",
                        onClick = { showWelcomeJoinDialog = true }
                    )

                    ConfigItemColdRain(
                        title = "退群提示语",
                        value = if (welcomeQuitMsg.isEmpty()) "默认" else welcomeQuitMsg.take(20) + if (welcomeQuitMsg.length > 20) "..." else "",
                        onClick = { showWelcomeQuitDialog = true }
                    )

                    ConfigItemColdRain(
                        title = "自定义报时内容",
                        value = if (hourlyCustom.isEmpty()) "未设置" else hourlyCustom.take(20) + if (hourlyCustom.length > 20) "..." else "",
                        onClick = { showHourlyCustomDialog = true }
                    )
                }
            }

            QEdgeCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "功能说明",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "• 全局开关控制脚本是否加载\n" +
                        "• 功能开关控制是否启用该功能\n" +
                        "• 群内发送「开机」或「关机」控制单群是否可用\n" +
                        "• 群内发送「开启XX」或「关闭XX」控制单群功能开关\n"+ 
                        "• 聊天页面长按右下角加号或右上角三条杠，即可打开冷雨Java菜单栏",
                        fontSize = 12.sp,
                        color = colors.textSecondary,
                        lineHeight = 18.sp
                    )
                }
            }

            ColdRainConfig.categories.forEach { category ->
                val features = ColdRainConfig.allFeatures.filter { it.category == category }
                if (features.isNotEmpty()) {
                    QEdgeCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                category,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            features.forEach { feature ->
                                FeatureSwitchItemColdRain(feature = feature)
                                if (feature != features.last()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
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

    if (showGlobalAdminsDialog) {
        var inputText by remember { mutableStateOf(globalAdmins) }
        AlertDialog(
            onDismissRequest = { showGlobalAdminsDialog = false },
            title = { Text("全局代管") },
            text = {
                Column {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        singleLine = true,
                        label = { Text("多个QQ用逗号分隔") }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "全局代管拥有所有群的管理权限",
                        fontSize = 12.sp,
                        color = colors.textSecondary
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    globalAdmins = inputText.trim()
                    ColdRainConfig.setString("global_admins", globalAdmins)
                    showGlobalAdminsDialog = false
                }) {
                    Text("确定", color = AccentBlue)
                }
            },
            dismissButton = {
                TextButton(onClick = { showGlobalAdminsDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (showWelcomeJoinDialog) {
        var inputText by remember { mutableStateOf(welcomeJoinMsg) }
        AlertDialog(
            onDismissRequest = { showWelcomeJoinDialog = false },
            title = { Text("进群欢迎语") },
            text = {
                Column {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        label = { Text("请输入欢迎语") }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "可用变量：{qq} 新成员QQ",
                        fontSize = 12.sp,
                        color = colors.textSecondary
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    welcomeJoinMsg = inputText.trim()
                    ColdRainConfig.setString("welcome_join_msg", welcomeJoinMsg)
                    showWelcomeJoinDialog = false
                }) {
                    Text("确定", color = AccentBlue)
                }
            },
            dismissButton = {
                TextButton(onClick = { showWelcomeJoinDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (showWelcomeQuitDialog) {
        var inputText by remember { mutableStateOf(welcomeQuitMsg) }
        AlertDialog(
            onDismissRequest = { showWelcomeQuitDialog = false },
            title = { Text("退群提示语") },
            text = {
                Column {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        label = { Text("请输入提示语") }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "可用变量：{qq} 退群成员QQ",
                        fontSize = 12.sp,
                        color = colors.textSecondary
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    welcomeQuitMsg = inputText.trim()
                    ColdRainConfig.setString("welcome_quit_msg", welcomeQuitMsg)
                    showWelcomeQuitDialog = false
                }) {
                    Text("确定", color = AccentBlue)
                }
            },
            dismissButton = {
                TextButton(onClick = { showWelcomeQuitDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (showHourlyCustomDialog) {
        var inputText by remember { mutableStateOf(hourlyCustom) }
        AlertDialog(
            onDismissRequest = { showHourlyCustomDialog = false },
            title = { Text("自定义报时内容") },
            text = {
                Column {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        label = { Text("请输入报时内容") }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "可用变量：{time} 当前时间",
                        fontSize = 12.sp,
                        color = colors.textSecondary
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    hourlyCustom = inputText.trim()
                    ColdRainConfig.setString("hourly_custom", hourlyCustom)
                    showHourlyCustomDialog = false
                }) {
                    Text("确定", color = AccentBlue)
                }
            },
            dismissButton = {
                TextButton(onClick = { showHourlyCustomDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun FeatureSwitchItemColdRain(
    feature: ColdRainConfig.FeatureItem
) {
    val colors = QEdgeTheme.colors
    var checked by remember { mutableStateOf(ColdRainConfig.isFeatureEnabled(feature.key)) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    if (checked) colors.accentBlue.copy(alpha = 0.1f)
                    else colors.cardBackground
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painterResource(R.drawable.ic_check_circle),
                feature.name,
                Modifier.size(20.dp),
                if (checked) colors.accentBlue else colors.textSecondary
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                feature.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textPrimary
            )
            Text(
                feature.description,
                fontSize = 12.sp,
                color = colors.textSecondary
            )
        }
        QEdgeSwitch(
            checked = checked,
            onCheckedChange = {
                checked = it
                ColdRainConfig.setFeatureEnabled(feature.key, it)
            }
        )
    }
}

@Composable
private fun ConfigItemColdRain(
    title: String,
    value: String,
    onClick: () -> Unit
) {
    val colors = QEdgeTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = colors.ripple),
                onClick = onClick
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textPrimary
            )
            Text(
                value,
                fontSize = 12.sp,
                color = colors.textSecondary
            )
        }
        Icon(
            painterResource(R.drawable.ic_chevron_right),
            "more",
            Modifier.size(16.dp),
            colors.textSecondary
        )
    }
}

@Composable
private fun SwitchItemColdRain(
    title: String,
    description: String = "",
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = QEdgeTheme.colors

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textPrimary
            )
            if (description.isNotEmpty()) {
                Text(
                    description,
                    fontSize = 12.sp,
                    color = colors.textSecondary
                )
            }
        }
        QEdgeSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
