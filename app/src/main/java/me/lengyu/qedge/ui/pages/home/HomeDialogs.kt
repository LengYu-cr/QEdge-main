package me.lengyu.qedge.ui.pages.home

import android.graphics.Color as AndroidColor
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import me.lengyu.qedge.hook.item.QLogRedirect
import me.lengyu.qedge.ui.core.theme.AccentBlue
import me.lengyu.qedge.ui.core.theme.AccentGreen
import me.lengyu.qedge.ui.core.theme.QEdgeTheme

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

/**
 * 图片外显设置弹窗：选择方式（随机文案 / HTTP接口），并编辑对应配置。
 * 接口方式下可选择返回格式（纯文本 / JSON），JSON 时填写解析字段路径。
 */
@Composable
fun HomeImageSummaryDialog(
    show: Boolean,
    mode: String,
    tips: String,
    url: String,
    format: String,
    field: String,
    onDismiss: () -> Unit,
    onConfirm: (mode: String, tips: String, url: String, format: String, field: String) -> Unit
) {
    if (!show) return

    val colors = QEdgeTheme.colors
    var curMode by remember(mode) { mutableStateOf(mode) }
    var curTips by remember(tips) { mutableStateOf(tips) }
    var curUrl by remember(url) { mutableStateOf(url) }
    var curFormat by remember(format) { mutableStateOf(format.ifEmpty { "text" }) }
    var curField by remember(field) { mutableStateOf(field) }

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

                Spacer(modifier = Modifier.height(14.dp))
                Text("返回格式", fontSize = 13.sp, color = colors.textPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    ImageSummaryModeOption(
                        label = "纯文本",
                        selected = curFormat != "json",
                        onClick = { curFormat = "text" },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    ImageSummaryModeOption(
                        label = "JSON",
                        selected = curFormat == "json",
                        onClick = { curFormat = "json" },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                if (curFormat == "json") {
                    Text("解析字段", fontSize = 13.sp, color = colors.textPrimary)
                    Spacer(modifier = Modifier.height(6.dp))
                    ImageSummaryField(
                        value = curField,
                        placeholder = "例如 data.msg 或 msg",
                        onValueChange = { curField = it }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "按 . 逐级取值，支持数组下标，例如 data.list[0].msg",
                        fontSize = 12.sp,
                        color = colors.textSecondary
                    )
                } else {
                    Text(
                        "每张图片发送前都会请求该接口，直接以返回内容作为外显",
                        fontSize = 12.sp,
                        color = colors.textSecondary
                    )
                }
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
                            onClick = { onConfirm(curMode, curTips.trim(), curUrl.trim(), curFormat, curField.trim()) }
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
fun HomeVoiceSpeedDialog(
    show: Boolean,
    current: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    if (!show) return

    val colors = QEdgeTheme.colors
    val options = listOf("0.5", "1.0", "1.25", "1.5", "2.0")
    var selected by remember(current) { mutableStateOf(current) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(colors.cardBackground)
                .padding(20.dp)
        ) {
            Text(
                "语音消息倍速",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "选择固定播放倍速，开启开关后语音/AMR 消息按此倍速播放",
                fontSize = 12.sp,
                color = colors.textSecondary
            )

            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                options.forEach { opt ->
                    val isSelected = if (selected.startsWith(opt)) true else selected == opt
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) AccentGreen else colors.cardBackground)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { selected = opt }
                            )
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "${opt}×",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isSelected) Color.White else colors.textPrimary
                        )
                    }
                }
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
                            onClick = { onConfirm(selected) }
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

/**
 * QLog 日志处理模式选择弹窗：关闭 / 拦截 / 重定向 三选一。
 */
@Composable
fun HomeQLogRedirectDialog(
    show: Boolean,
    current: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    if (!show) return

    val colors = QEdgeTheme.colors
    var selected by remember(current) { mutableStateOf(current.ifEmpty { QLogRedirect.MODE_OFF }) }

    data class ModeItem(val value: String, val title: String, val desc: String)

    val modes = listOf(
        ModeItem(QLogRedirect.MODE_OFF, "关闭", "QQ日志正常输出，不做任何拦截"),
        ModeItem(QLogRedirect.MODE_REDIRECT, "重定向", "拦截并写入 QEdge/log/QLog/，丢弃原日志"),
        ModeItem(QLogRedirect.MODE_MUTE, "纯拦截", "直接丢弃QQ日志，不写入任何本地文件")
    )

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(colors.cardBackground)
                .padding(20.dp)
        ) {
            Text(
                "QLog日志处理",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "选择QQ宿主日志的处理模式，点击选项即时生效",
                fontSize = 12.sp,
                color = colors.textSecondary
            )

            Spacer(modifier = Modifier.height(16.dp))
            modes.forEach { item ->
                val isSelected = selected == item.value
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) AccentGreen.copy(alpha = 0.12f) else colors.cardBackground.copy(alpha = 0.5f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                selected = item.value
                                onConfirm(item.value)
                                onDismiss()
                            }
                        )
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(if (isSelected) AccentGreen else colors.textSecondary.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                androidx.compose.foundation.layout.Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(androidx.compose.foundation.shape.CircleShape)
                                        .background(Color.White)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            item.title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.textPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        item.desc,
                        fontSize = 12.sp,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(start = 30.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss
                    )
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("取消", fontSize = 14.sp, color = colors.textSecondary)
            }
        }
    }
}

/**
 * 入口选择弹窗：纵向单选列表，点击选项即时生效并关闭（脚本菜单入口、综合面板入口共用）。
 */
@Composable
fun HomeEntrySelectDialog(
    show: Boolean,
    title: String,
    subtitle: String,
    options: Map<String, String>,
    current: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    if (!show) return

    val colors = QEdgeTheme.colors
    var selected by remember(current) { mutableStateOf(current) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(colors.cardBackground)
                .padding(20.dp)
        ) {
            Text(
                title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                subtitle,
                fontSize = 12.sp,
                color = colors.textSecondary
            )

            Spacer(modifier = Modifier.height(16.dp))
            options.forEach { (key, label) ->
                val isSelected = selected == key
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) AccentBlue.copy(alpha = 0.15f)
                            else colors.textSecondary.copy(alpha = 0.08f)
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                selected = key
                                onConfirm(key)
                                onDismiss()
                            }
                        )
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(
                                    if (isSelected) AccentBlue
                                    else colors.textSecondary.copy(alpha = 0.3f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(androidx.compose.foundation.shape.CircleShape)
                                        .background(Color.White)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            label,
                            fontSize = 15.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) colors.textPrimary else colors.textSecondary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss
                    )
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("取消", fontSize = 14.sp, color = colors.textSecondary)
            }
        }
    }
}