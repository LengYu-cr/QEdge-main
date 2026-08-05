package me.lengyu.qedge.ui.pages.home

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
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import me.lengyu.qedge.R
import me.lengyu.qedge.ui.components.atoms.QEdgeCard
import me.lengyu.qedge.ui.core.theme.AccentGreen
import me.lengyu.qedge.ui.core.theme.AccentBlue
import me.lengyu.qedge.ui.core.theme.QEdgeTheme

@Composable
fun MainScreen(
    versionName: String,
    versionCode: Int,
    isLatestVersion: Boolean,
    isCheckingUpdate: Boolean,
    onQQGroupClick: () -> Unit,
    onTGClick: () -> Unit,
    onUserBackendClick: () -> Unit,
    onQFunClick: () -> Unit,
    onUpdateLogClick: () -> Unit,
    onCheckUpdateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = QEdgeTheme.colors
    var showSupportDialog by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
                .windowInsetsPadding(WindowInsets.statusBars)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "QEdge",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text("$versionName($versionCode)", fontSize = 14.sp, color = colors.textSecondary)
            }
        }

        QEdgeCard(
            modifier = Modifier.fillMaxWidth(),
            animateContentSize = false,
            onClick = onCheckUpdateClick
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painterResource(if (isLatestVersion) R.drawable.ic_check_circle else R.drawable.book),
                    null,
                    Modifier.size(36.dp),
                    if (isLatestVersion) AccentGreen else AccentBlue
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (isCheckingUpdate) "正在检查更新..."
                        else if (isLatestVersion) "已是最新版本"
                        else "发现新版本",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        if (isCheckingUpdate) "请稍候"
                        else "当前版本 v$versionName($versionCode)",
                        fontSize = 13.sp,
                        color = colors.textSecondary
                    )
                }
                Icon(
                    painterResource(R.drawable.ic_chevron_right),
                    null,
                    Modifier.size(20.dp),
                    colors.textSecondary.copy(alpha = 0.4f)
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            LinkCard(
                R.drawable.ic_logo_qq,
                "QQ交流群",
                "群号: 935100470",
                onQQGroupClick
            )
            LinkCard(
                R.drawable.ic_logo_telegram,
                "Telegram",
                "https://t.me/+St91SS8CLNUyMDY1",
                onTGClick
            )
            LinkCard(
                R.drawable.ic_launcher,
                "用户后台",
                "https://v.yuafeng.cn/QEdge/user/index.php",
                onUserBackendClick
            )
            LinkCard(
                R.drawable.book,
                "更新日志",
                "查看版本更新记录",
                onUpdateLogClick,
                iconTint = AccentBlue
            )
            LinkCard(
                R.drawable.category,
                "适配列表",
                "查看已适配的软件和功能",
                { showSupportDialog = true },
                iconTint = AccentGreen
            )
            LinkCard(
                R.drawable.qfun,
                "QFun 开源致谢",
                "https://github.com/oneQAQone/QFun",
                onQFunClick
            )
        }

        Spacer(modifier = Modifier.height(40.dp))
        }

        if (showSupportDialog) {
            Dialog(onDismissRequest = { showSupportDialog = false }) {
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.cardBackground)
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        "适配列表",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    SupportItem(
                        name = "QQ",
                        version = "9.1.78-9.3.30",
                        features = listOf(
                            "Java脚本",
                            "文件管理器",
                            "冷雨Java嵌入版",
                            "好玩的模块功能"
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    SupportItem(
                        name = "TIM",
                        version = "不知道-4.1.0",
                        features = listOf(
                            "Java脚本",
                            "文件管理器",
                            "冷雨Java嵌入版",
                            "不好玩的模块功能"
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    SupportItem(
                        name = "KK键盘",
                        version = "不知道-4.0.9",
                        features = listOf(
                            "本地会员全版本通杀",
                            "本地去广告全版本通杀",
                            "阻止闪退全版本通杀"
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    SupportItem(
                        name = "酷狗大字版",
                        version = "全版本",
                        features = listOf(
                            "本地开屏广告全版本通杀（免root）"
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    SupportItem(
                        name = "酷狗概念版",
                        version = "全版本",
                        features = listOf(
                            "本地开屏广告全版本通杀（需要root）"
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    SupportItem(
                        name = "傲软抠图",
                        version = "全版本",
                        features = listOf(
                            "本地会员全版本通杀（需要root）"
                        )
                    )



                    Spacer(modifier = Modifier.height(20.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        androidx.compose.material3.TextButton(onClick = { showSupportDialog = false }) {
                            Text("关闭", color = colors.accentBlue)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LinkCard(iconRes: Int, title: String, subtitle: String, onClick: () -> Unit, iconTint: Color = Color.Unspecified) {
    val colors = QEdgeTheme.colors

    QEdgeCard(modifier = Modifier.fillMaxWidth(), animateContentSize = false, onClick = onClick) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(painterResource(iconRes), title, Modifier.size(36.dp), tint = iconTint)
            Spacer(modifier = Modifier.width(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(subtitle, fontSize = 13.sp, color = colors.textSecondary)
            }
        }
    }
}

@Composable
private fun SupportItem(name: String, version: String, features: List<String>) {
    val colors = QEdgeTheme.colors

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = version,
                fontSize = 12.sp,
                color = colors.textSecondary
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        features.forEach { feature ->
            Row(
                modifier = Modifier.padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_check_circle),
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = AccentGreen
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = feature,
                    fontSize = 13.sp,
                    color = colors.textSecondary
                )
            }
        }
    }
}