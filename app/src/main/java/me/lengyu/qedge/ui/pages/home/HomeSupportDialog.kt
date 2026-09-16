package me.lengyu.qedge.ui.pages.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import me.lengyu.qedge.R
import me.lengyu.qedge.ui.core.theme.QEdgeTheme

@Composable
internal fun HomeSupportDialog(onDismiss: () -> Unit) {
    val colors = QEdgeTheme.colors

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 620.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(colors.cardBackground)
                .padding(22.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "适配列表",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(18.dp))

            SupportItem(
                name = "QQ",
                version = "9.1.78-9.3.55",
                features = listOf("Java脚本", "文件管理器", "冷雨Java嵌入版", "好玩的模块功能")
            )
            SupportDivider()
            SupportItem(
                name = "TIM",
                version = "不知道-不知道（大概最新版吧）",
                features = listOf("Java脚本", "文件管理器", "冷雨Java嵌入版", "好玩的模块功能")
            )
            SupportDivider()
            SupportItem(
                name = "KK键盘",
                version = "全版本",
                features = listOf(
                    "本地会员全版本通杀",
                    "本地去广告全版本通杀",
                    "阻止闪退全版本通杀",
                    "透明头像上传"
                )
            )
            SupportDivider()
            SupportItem(
                name = "酷狗大字版",
                version = "全版本",
                features = listOf(
                    "本地开屏广告全版本通杀（免root）",
                    "透明头像上传"
                )
            )
            SupportDivider()
            SupportItem(
                name = "酷狗概念版",
                version = "全版本",
                features = listOf("本地开屏广告全版本通杀（需要root）")
            )
            SupportDivider()
            SupportItem(
                name = "傲软抠图",
                version = "全版本",
                features = listOf("本地会员全版本通杀（需要root）")
            )
            SupportDivider()
            SupportItem(
                name = "无痛单词",
                version = "全版本",
                features = listOf("本地会员全版本通杀")
            )
            SupportDivider()
            SupportItem(
                name = "设备信息X",
                version = "全版本",
                features = listOf("本地会员全版本通杀(需要root)")
            )
            SupportDivider()
            SupportItem(
                name = "一个木函",
                version = "全版本",
                features = listOf("本地会员全版本通杀")
            )


            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("关闭", color = colors.accentBlue)
                }
            }
        }
    }
}

@Composable
private fun SupportDivider() {
    Spacer(modifier = Modifier.height(18.dp))
}

@Composable
private fun SupportItem(
    name: String,
    version: String,
    features: List<String>
) {
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
                    tint = colors.accentGreen
                )
                Spacer(modifier = Modifier.width(7.dp))
                Text(
                    text = feature,
                    fontSize = 13.sp,
                    color = colors.textSecondary
                )
            }
        }
    }
}
