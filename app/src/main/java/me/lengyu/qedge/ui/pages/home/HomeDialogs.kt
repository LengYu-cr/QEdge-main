package me.lengyu.qedge.ui.pages.home

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import me.lengyu.qedge.hook.item.LevelBoost
import me.lengyu.qedge.ui.core.theme.AccentGreen
import me.lengyu.qedge.ui.core.theme.QEdgeTheme
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

@Composable
fun HomeCommentInputDialog(
    show: Boolean,
    commentText: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    if (!show) return

    val colors = QEdgeTheme.colors
    var text by remember(commentText) { mutableStateOf(commentText) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(colors.cardBackground)
                .padding(20.dp)
        ) {
            Text(
                "自定义评论内容",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(16.dp))
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.textSecondary.copy(alpha = 0.08f))
                    .padding(14.dp)
            ) {
                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    textStyle = TextStyle(fontSize = 14.sp, color = colors.textPrimary),
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4,
                    decorationBox = { innerTextField ->
                        if (text.isEmpty()) {
                            Text("说点什么吧...", fontSize = 14.sp, color = colors.textSecondary)
                        }
                        innerTextField()
                    }
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onDismiss
                        )
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("取消", fontSize = 14.sp, color = colors.textSecondary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(AccentGreen)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onConfirm(text) }
                        )
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("确定", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun HomeMoodScheduleDialog(
    show: Boolean,
    initialTime: String,
    initialText: String,
    onDismiss: () -> Unit,
    onConfirm: (time: String, text: String) -> Unit
) {
    if (!show) return

    val colors = QEdgeTheme.colors
    var time by remember(initialTime) { mutableStateOf(initialTime) }
    var text by remember(initialText) { mutableStateOf(initialText) }
    val timeOk = LevelBoost.HH_MM_REGEX.matches(time.trim())
    val canConfirm = timeOk

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(colors.cardBackground)
                .padding(20.dp)
        ) {
            Text(
                "定时说说设置",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "仅需 HH:mm，无需日期，每天同一时间触发一次",
                fontSize = 12.sp,
                color = colors.textSecondary
            )

            Spacer(modifier = Modifier.height(14.dp))
            Text("发送时间 (HH:mm)", fontSize = 13.sp, color = colors.textPrimary)
            Spacer(modifier = Modifier.height(6.dp))
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.textSecondary.copy(alpha = 0.08f))
                    .padding(14.dp)
            ) {
                BasicTextField(
                    value = time,
                    onValueChange = { v ->
                        time = v.filter { it.isDigit() || it == ':' }.take(5)
                    },
                    textStyle = TextStyle(fontSize = 16.sp, color = colors.textPrimary),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (time.isEmpty()) {
                                Text("08:30", fontSize = 16.sp, color = colors.textSecondary)
                            } else {
                                innerTextField()
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                if (timeOk) "格式正确" else "格式错误",
                                fontSize = 12.sp,
                                color = if (timeOk) AccentGreen else Color(AndroidColor.parseColor("#FF5252"))
                            )
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            Text("说说内容", fontSize = 13.sp, color = colors.textPrimary)
            Spacer(modifier = Modifier.height(6.dp))
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.textSecondary.copy(alpha = 0.08f))
                    .padding(14.dp)
            ) {
                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    textStyle = TextStyle(fontSize = 14.sp, color = colors.textPrimary),
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 6,
                    decorationBox = { innerTextField ->
                        if (text.isEmpty()) {
                            Text("今天也要加油哦~", fontSize = 14.sp, color = colors.textSecondary)
                        } else {
                            innerTextField()
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onDismiss
                        )
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("取消", fontSize = 14.sp, color = colors.textSecondary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (canConfirm) AccentGreen else colors.textSecondary.copy(alpha = 0.3f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                if (!canConfirm) return@clickable
                                onConfirm(time.trim(), text.trim())
                            }
                        )
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "保存",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (canConfirm) Color.White else colors.textSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun HomeUpdateLogDialog(
    show: Boolean,
    onDismiss: () -> Unit
) {
    if (!show) return

    val colors = QEdgeTheme.colors
    var logText by remember { mutableStateOf("加载中...") }

    LaunchedEffect(Unit) {
        Thread {
            try {
                val url = URL("https://v.yuafeng.cn/QEdge/update/changelog.php")
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.requestMethod = "GET"

                val reader = BufferedReader(InputStreamReader(connection.inputStream, "UTF-8"))
                val response = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }
                reader.close()

                val json = JSONObject(response.toString())
                if (json.getInt("code") == 200) {
                    val data = json.getJSONObject("data")
                    val changelog = data.getJSONArray("changelog")
                    val sb = StringBuilder()
                    for (i in 0 until changelog.length()) {
                        val entry = changelog.getJSONObject(i)
                        sb.append("v${entry.getString("version")} (${entry.getString("date")})\n")
                        val items = entry.getJSONArray("items")
                        for (j in 0 until items.length()) {
                            sb.append("• ${items.getString(j)}\n")
                        }
                        if (i < changelog.length() - 1) {
                            sb.append("\n")
                        }
                    }
                    logText = sb.toString()
                } else {
                    logText = "获取失败"
                }
            } catch (e: Exception) {
                logText = "获取失败: ${e.message}"
            }
        }.start()
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .clip(RoundedCornerShape(20.dp))
                .background(colors.cardBackground)
                .padding(20.dp)
        ) {
            Text(
                "更新日志",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(12.dp))
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    logText,
                    fontSize = 14.sp,
                    color = colors.textSecondary,
                    lineHeight = 20.sp
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onDismiss
                        )
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("关闭", fontSize = 14.sp, color = colors.textPrimary)
                }
            }
        }
    }
}

/**
 * 图片外显设置弹窗：选择方式（随机文案 / HTTP接口），并编辑对应配置。
 */
@Composable
fun HomeImageSummaryDialog(
    show: Boolean,
    mode: String,
    tips: String,
    url: String,
    onDismiss: () -> Unit,
    onConfirm: (mode: String, tips: String, url: String) -> Unit
) {
    if (!show) return

    val colors = QEdgeTheme.colors
    var curMode by remember(mode) { mutableStateOf(mode) }
    var curTips by remember(tips) { mutableStateOf(tips) }
    var curUrl by remember(url) { mutableStateOf(url) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(colors.cardBackground)
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                "图片外显设置",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "发送图片时，将图片下方外显文本替换为以下内容",
                fontSize = 12.sp,
                color = colors.textSecondary
            )

            Spacer(modifier = Modifier.height(14.dp))
            Text("方式", fontSize = 13.sp, color = colors.textPrimary)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                ImageSummaryModeOption(
                    label = "随机文案",
                    selected = curMode != "http",
                    onClick = { curMode = "text" },
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(10.dp))
                ImageSummaryModeOption(
                    label = "接口返回",
                    selected = curMode == "http",
                    onClick = { curMode = "http" },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            if (curMode == "http") {
                Text("接口地址", fontSize = 13.sp, color = colors.textPrimary)
                Spacer(modifier = Modifier.height(6.dp))
                ImageSummaryField(
                    value = curUrl,
                    placeholder = "https://example.com/api/summary",
                    onValueChange = { curUrl = it }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "每张图片发送前都会请求该接口，直接以返回内容作为外显",
                    fontSize = 12.sp,
                    color = colors.textSecondary
                )
            } else {
                Text("文案列表", fontSize = 13.sp, color = colors.textPrimary)
                Spacer(modifier = Modifier.height(6.dp))
                ImageSummaryField(
                    value = curTips,
                    placeholder = "多条用逗号分隔，例如：奋斗每一天，加油，冲!",
                    multiLine = true,
                    onValueChange = { curTips = it }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "多条文案每次随机取一条，用逗号(,)分隔",
                    fontSize = 12.sp,
                    color = colors.textSecondary
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onDismiss
                        )
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("取消", fontSize = 14.sp, color = colors.textSecondary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(AccentGreen)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onConfirm(curMode, curTips.trim(), curUrl.trim()) }
                        )
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "保存",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun ImageSummaryModeOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = QEdgeTheme.colors
    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (selected) AccentGreen.copy(alpha = 0.15f)
                else colors.textSecondary.copy(alpha = 0.08f)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) AccentGreen else colors.textSecondary
        )
    }
}

@Composable
private fun ImageSummaryField(
    value: String,
    placeholder: String,
    multiLine: Boolean = false,
    onValueChange: (String) -> Unit
) {
    val colors = QEdgeTheme.colors
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.textSecondary.copy(alpha = 0.08f))
            .padding(14.dp)
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(fontSize = 14.sp, color = colors.textPrimary),
            modifier = Modifier.fillMaxWidth(),
            singleLine = !multiLine,
            maxLines = if (multiLine) 4 else 1,
            decorationBox = { innerTextField ->
                if (value.isEmpty()) {
                    Text(placeholder, fontSize = 14.sp, color = colors.textSecondary)
                }
                innerTextField()
            }
        )
    }
}
/**
 * 篡改发送图片比例设置弹窗：输入宽度、高度（整数）。
 */
@Composable
fun HomeImageRatioDialog(
    show: Boolean,
    width: String,
    height: String,
    onDismiss: () -> Unit,
    onConfirm: (width: Int, height: Int) -> Unit
) {
    if (!show) return

    val colors = QEdgeTheme.colors
    var curWidth by remember(width) { mutableStateOf(width) }
    var curHeight by remember(height) { mutableStateOf(height) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(colors.cardBackground)
                .padding(20.dp)
        ) {
            Text(
                "发送图片比例",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "对所有发送的图片强制设置像素宽高（整数）",
                fontSize = 12.sp,
                color = colors.textSecondary
            )

            Spacer(modifier = Modifier.height(14.dp))
            ImageRatioNumberField(
                label = "宽度 (px)",
                value = curWidth,
                placeholder = "例如 1080",
                onValueChange = { curWidth = it }
            )
            Spacer(modifier = Modifier.height(12.dp))
            ImageRatioNumberField(
                label = "高度 (px)",
                value = curHeight,
                placeholder = "例如 1440",
                onValueChange = { curHeight = it }
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "宽高必须为正整数，填 0 或留空则不生效",
                fontSize = 12.sp,
                color = colors.textSecondary
            )

            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onDismiss
                        )
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("取消", fontSize = 14.sp, color = colors.textSecondary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(AccentGreen)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                val w = curWidth.trim().toIntOrNull() ?: 0
                                val h = curHeight.trim().toIntOrNull() ?: 0
                                onConfirm(w, h)
                            }
                        )
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "保存",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun ImageRatioNumberField(
    label: String,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit
) {
    val colors = QEdgeTheme.colors
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, fontSize = 13.sp, color = colors.textPrimary)
        Spacer(modifier = Modifier.height(6.dp))
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(colors.textSecondary.copy(alpha = 0.08f))
                .padding(14.dp)
        ) {
            BasicTextField(
                value = value,
                onValueChange = { input ->
                    // 仅允许数字
                    onValueChange(input.filter { it.isDigit() })
                },
                textStyle = TextStyle(fontSize = 14.sp, color = colors.textPrimary),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                decorationBox = { innerTextField ->
                    if (value.isEmpty()) {
                        Text(placeholder, fontSize = 14.sp, color = colors.textSecondary)
                    }
                    innerTextField()
                }
            )
        }
    }
}

/**
 * 新建插件弹窗：选择语言(JS / Java)，填写脚本名、描述、作者、版本号。
 * 确认后回调 onConfirm(type, name, desc, author, version)，主脚本建空文件由用户自行编写。
 */
@Composable
fun HomeCreatePluginDialog(
    show: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (type: String, name: String, desc: String, author: String, version: String) -> Unit
) {
    if (!show) return

    val colors = QEdgeTheme.colors
    var isJs by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("") }
    var version by remember { mutableStateOf("1.0") }
    val canConfirm = name.trim().isNotEmpty()

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(20.dp))
                .background(colors.cardBackground)
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                "新建脚本",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text("脚本语言", fontSize = 13.sp, color = colors.textPrimary)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                CreatePluginTypeOption(
                    label = "Java",
                    selected = !isJs,
                    onClick = { isJs = false },
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(10.dp))
                CreatePluginTypeOption(
                    label = "JS",
                    selected = isJs,
                    onClick = { isJs = true },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            CreatePluginField(
                label = "脚本名",
                value = name,
                placeholder = "请输入脚本名",
                singleLine = true,
                onValueChange = { name = it }
            )

            Spacer(modifier = Modifier.height(14.dp))
            CreatePluginField(
                label = "描述",
                value = desc,
                placeholder = "简单介绍一下这个脚本",
                singleLine = false,
                onValueChange = { desc = it }
            )

            Spacer(modifier = Modifier.height(14.dp))
            CreatePluginField(
                label = "作者",
                value = author,
                placeholder = "作者名",
                singleLine = true,
                onValueChange = { author = it }
            )

            Spacer(modifier = Modifier.height(14.dp))
            CreatePluginField(
                label = "版本号",
                value = version,
                placeholder = "1.0",
                singleLine = true,
                onValueChange = { version = it }
            )

            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onDismiss
                        )
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("取消", fontSize = 14.sp, color = colors.textSecondary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (canConfirm) AccentGreen else colors.textSecondary.copy(alpha = 0.3f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                if (!canConfirm) return@clickable
                                onConfirm(
                                    if (isJs) "js" else "java",
                                    name.trim(),
                                    desc.trim(),
                                    author.trim(),
                                    version.trim()
                                )
                            }
                        )
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "创建",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (canConfirm) Color.White else colors.textSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun CreatePluginTypeOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = QEdgeTheme.colors
    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (selected) AccentGreen.copy(alpha = 0.15f)
                else colors.textSecondary.copy(alpha = 0.08f)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) AccentGreen else colors.textSecondary
        )
    }
}

@Composable
private fun CreatePluginField(
    label: String,
    value: String,
    placeholder: String,
    singleLine: Boolean,
    onValueChange: (String) -> Unit
) {
    val colors = QEdgeTheme.colors
    Text(label, fontSize = 13.sp, color = colors.textPrimary)
    Spacer(modifier = Modifier.height(6.dp))
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.textSecondary.copy(alpha = 0.08f))
            .padding(14.dp)
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(fontSize = 14.sp, color = colors.textPrimary),
            modifier = Modifier.fillMaxWidth(),
            singleLine = singleLine,
            maxLines = if (singleLine) 1 else 4,
            decorationBox = { innerTextField ->
                if (value.isEmpty()) {
                    Text(placeholder, fontSize = 14.sp, color = colors.textSecondary)
                }
                innerTextField()
            }
        )
    }
}