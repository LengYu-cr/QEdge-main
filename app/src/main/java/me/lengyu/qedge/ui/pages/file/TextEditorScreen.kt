package me.lengyu.qedge.ui.pages.file

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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.ripple
import androidx.compose.material3.Icon
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.lengyu.qedge.R
import me.lengyu.qedge.ui.components.atoms.QEdgeCard
import me.lengyu.qedge.ui.components.molecules.QEdgeTopBar
import me.lengyu.qedge.ui.core.theme.Dimens
import me.lengyu.qedge.ui.core.theme.QEdgeTheme
import me.lengyu.qedge.utils.LogUtils
import java.io.File

@Composable
fun TextEditorScreen(
    filePath: String,
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = QEdgeTheme.colors
    val context = LocalContext.current
    var content by remember { mutableStateOf(TextFieldValue("")) }
    var isModified by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var fileName by remember { mutableStateOf("") }
    var fileExtension by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val scrollState = rememberScrollState()
    val hScrollState = rememberScrollState()

    LaunchedEffect(filePath) {
        val file = File(filePath)
        fileName = file.name
        fileExtension = file.extension.lowercase()
        Thread {
            try {
                val text = file.readText(Charsets.UTF_8)
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    content = TextFieldValue(text)
                    isLoading = false
                }
            } catch (e: Exception) {
                LogUtils.e(e)
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    content = TextFieldValue("读取失败: ${e.message}")
                    isLoading = false
                }
            }
        }.start()
    }

    fun saveFile() {
        Thread {
            try {
                File(filePath).writeText(content.text, Charsets.UTF_8)
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    isModified = false
                    android.widget.Toast.makeText(context, "${fileName} 已保存", android.widget.Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                LogUtils.e(e)
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    android.widget.Toast.makeText(context, "保存失败: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            QEdgeTopBar(
                title = fileName,
                showBackButton = true,
                onBackClick = onBackClick,
                isDarkTheme = isDarkTheme,
                showThemeButton = false,
                onThemeToggle = onThemeToggle,
                actions = {
                    if (isModified) {
                        Text(
                            text = "●",
                            fontSize = 14.sp,
                            color = colors.accentRed,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                    TopBarActionButton(
                        iconRes = R.drawable.ic_check_circle,
                        contentDescription = "保存",
                        onClick = { saveFile() }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            QEdgeCard(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "加载中...",
                            fontSize = 14.sp,
                            color = colors.textSecondary
                        )
                    }
                } else {
                    val lines = content.text.split("\n")
                    val lineHeight = 20.sp
                    val fontSize = 14.sp
                    val padding = 16.dp

                    Row(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .width(48.dp)
                                .fillMaxHeight()
                                .background(colors.cardBackground)
                                .verticalScroll(scrollState)
                        ) {
                            Text(
                                text = buildAnnotatedString {
                                    lines.forEachIndexed { index, _ ->
                                        withStyle(
                                            style = androidx.compose.ui.text.SpanStyle(
                                                color = colors.textSecondary.copy(alpha = 0.6f)
                                            )
                                        ) {
                                            append("${index + 1}")
                                        }
                                        if (index < lines.size - 1) {
                                            append("\n")
                                        }
                                    }
                                },
                                fontSize = fontSize,
                                lineHeight = lineHeight,
                                textAlign = TextAlign.End,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = padding, bottom = padding, end = 8.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .verticalScroll(scrollState)
                                .horizontalScroll(hScrollState)
                        ) {
                            Text(
                                text = highlightSyntax(
                                    text = content.text,
                                    extension = fileExtension,
                                    textColor = colors.textPrimary,
                                    commentColor = colors.textSecondary.copy(alpha = 0.7f),
                                    stringColor = Color(0xFF34C759),
                                    keywordColor = Color(0xFFAF52DE)
                                ),
                                fontSize = fontSize,
                                lineHeight = lineHeight,
                                softWrap = false,
                                modifier = Modifier
                                    .padding(padding)
                                    .width(10000.dp)
                            )

                            BasicTextField(
                                value = content,
                                onValueChange = {
                                    content = it
                                    isModified = true
                                },
                                modifier = Modifier
                                    .padding(padding)
                                    .width(10000.dp)
                                    .focusRequester(focusRequester),
                                textStyle = TextStyle(
                                    fontSize = fontSize,
                                    color = Color.Transparent,
                                    lineHeight = lineHeight
                                ),
                                singleLine = false,
                                cursorBrush = androidx.compose.ui.graphics.SolidColor(colors.textPrimary),
                                decorationBox = { innerTextField ->
                                    Box {
                                        innerTextField()
                                    }
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            EditorBottomBar(
                lineCount = content.text.split("\n").size,
                charCount = content.text.length,
                onSave = { saveFile() }
            )
        }
    }
}

@Composable
private fun TopBarActionButton(
    iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit
) {
    val colors = QEdgeTheme.colors
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(16.dp))
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
            Modifier.size(20.dp),
            colors.textPrimary
        )
    }
}

@Composable
private fun EditorBottomBar(
    lineCount: Int,
    charCount: Int,
    onSave: () -> Unit
) {
    val colors = QEdgeTheme.colors

    QEdgeCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "行: $lineCount",
                fontSize = 12.sp,
                color = colors.textSecondary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "字符: $charCount",
                fontSize = 12.sp,
                color = colors.textSecondary
            )
            Spacer(modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.accentBlue)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(color = Color.White.copy(alpha = 0.3f)),
                        onClick = onSave
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "保存",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
            }
        }
    }
}

private fun isCommentLine(line: String, extension: String): Boolean {
    val trimmed = line.trim()
    if (trimmed.isEmpty()) return false

    val singleLineComments = when (extension) {
        "java", "kt", "kts", "js", "ts", "c", "cpp", "h", "hpp", "cs", "go", "rust", "swift" -> listOf("//")
        "py", "sh", "bash", "zsh", "yaml", "yml", "toml", "ini", "conf", "cfg", "properties" -> listOf("#")
        "php" -> listOf("//", "#")
        "lua" -> listOf("--")
        "html", "xml" -> listOf("<!--")
        "css" -> listOf("/*")
        else -> listOf("//", "#", "--")
    }

    return singleLineComments.any { trimmed.startsWith(it) }
}

private fun getKeywords(extension: String): Set<String> {
    val baseKeywords = setOf(
        "if", "else", "elif", "elseif",
        "public", "private", "protected",
        "function", "fun", "def", "fn",
        "val", "var", "const", "let",
        "int", "long", "byte", "short", "float", "double", "char", "boolean", "bool", "string", "void",
        "class", "interface", "struct", "enum", "object", "trait",
        "return", "break", "continue", "for", "while", "do", "switch", "case", "when",
        "new", "this", "super", "null", "true", "false", "nil", "None",
        "import", "package", "include", "require", "use", "from", "as",
        "try", "catch", "finally", "throw", "throws", "exception",
        "static", "final", "abstract", "override", "open", "inner", "sealed", "data",
        "in", "is", "as", "typeof", "instanceof",
        "and", "or", "not", "&&", "||", "!"
    )

    val langKeywords = when (extension) {
        "java" -> setOf("abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const", "continue", "default", "do", "double", "else", "enum", "extends", "final", "finally", "float", "for", "goto", "if", "implements", "import", "instanceof", "int", "interface", "long", "native", "new", "package", "private", "protected", "public", "return", "short", "static", "strictfp", "super", "switch", "synchronized", "this", "throw", "throws", "transient", "try", "void", "volatile", "while")
        "kt", "kts" -> setOf("abstract", "annotation", "as", "break", "by", "catch", "class", "companion", "const", "constructor", "continue", "data", "do", "dynamic", "else", "enum", "external", "false", "final", "finally", "for", "fun", "if", "import", "in", "infix", "init", "inline", "inner", "interface", "internal", "is", "lateinit", "object", "open", "operator", "out", "override", "package", "private", "protected", "public", "return", "sealed", "super", "suspend", "tailrec", "this", "throw", "true", "try", "typealias", "val", "var", "vararg", "when", "where", "while")
        "py" -> setOf("False", "None", "True", "and", "as", "assert", "break", "class", "continue", "def", "del", "elif", "else", "except", "finally", "for", "from", "global", "if", "import", "in", "is", "lambda", "nonlocal", "not", "or", "pass", "raise", "return", "try", "while", "with", "yield")
        "js", "ts" -> setOf("break", "case", "catch", "class", "const", "continue", "debugger", "default", "delete", "do", "else", "export", "extends", "finally", "for", "function", "if", "import", "in", "instanceof", "let", "new", "return", "super", "switch", "this", "throw", "try", "typeof", "var", "void", "while", "with", "yield")
        "php" -> setOf("abstract", "and", "array", "as", "break", "callable", "case", "catch", "class", "clone", "const", "continue", "declare", "default", "die", "do", "echo", "else", "elseif", "empty", "enddeclare", "endfor", "endforeach", "endif", "endswitch", "endwhile", "eval", "exit", "extends", "final", "finally", "for", "foreach", "function", "global", "goto", "if", "implements", "include", "include_once", "instanceof", "insteadof", "interface", "isset", "list", "namespace", "new", "or", "print", "private", "protected", "public", "require", "require_once", "return", "static", "switch", "throw", "trait", "try", "unset", "use", "var", "while", "xor", "yield")
        "c", "cpp", "h", "hpp" -> setOf("auto", "break", "case", "char", "const", "continue", "default", "do", "double", "else", "enum", "extern", "float", "for", "goto", "if", "int", "long", "register", "return", "short", "signed", "sizeof", "static", "struct", "switch", "typedef", "union", "unsigned", "void", "volatile", "while", "class", "public", "private", "protected", "virtual", "inline", "template", "typename", "namespace", "new", "delete", "this", "try", "catch", "throw")
        "go" -> setOf("break", "case", "chan", "const", "continue", "default", "defer", "else", "fallthrough", "for", "func", "go", "goto", "if", "import", "interface", "map", "package", "range", "return", "select", "struct", "switch", "type", "var")
        "rust" -> setOf("as", "break", "const", "continue", "crate", "else", "enum", "extern", "false", "fn", "for", "if", "impl", "in", "let", "loop", "match", "mod", "move", "mut", "pub", "ref", "return", "self", "Self", "static", "struct", "super", "trait", "true", "type", "unsafe", "use", "where", "while")
        "lua" -> setOf("and", "break", "do", "else", "elseif", "end", "false", "for", "function", "goto", "if", "in", "local", "nil", "not", "or", "repeat", "return", "then", "true", "until", "while")
        "sh", "bash", "zsh" -> setOf("if", "then", "else", "elif", "fi", "for", "in", "do", "done", "while", "case", "esac", "function", "return", "break", "continue", "local", "export", "readonly", "declare", "typeset")
        else -> baseKeywords
    }

    return langKeywords
}

private fun highlightSyntax(
    text: String,
    extension: String,
    textColor: Color,
    commentColor: Color,
    stringColor: Color,
    keywordColor: Color
): androidx.compose.ui.text.AnnotatedString {
    val keywords = getKeywords(extension)
    val lines = text.split("\n")

    return buildAnnotatedString {
        lines.forEachIndexed { lineIndex, line ->
            val trimmed = line.trimStart()
            val commentPrefix = getCommentPrefix(trimmed, extension)

            if (commentPrefix != null) {
                val leadingSpaces = line.length - trimmed.length
                append(line.substring(0, leadingSpaces))
                withStyle(style = androidx.compose.ui.text.SpanStyle(color = commentColor)) {
                    append(line.substring(leadingSpaces))
                }
            } else {
                var i = 0
                val lineLen = line.length

                while (i < lineLen) {
                    val c = line[i]

                    if (c == '"' || c == '\'') {
                        val quote = c
                        val start = i
                        i++
                        while (i < lineLen && line[i] != quote) {
                            if (line[i] == '\\' && i + 1 < lineLen) {
                                i += 2
                            } else {
                                i++
                            }
                        }
                        if (i < lineLen) i++

                        withStyle(style = androidx.compose.ui.text.SpanStyle(color = stringColor)) {
                            append(line.substring(start, i.coerceAtMost(lineLen)))
                        }
                    } else if (c.isLetter() || c == '_') {
                        val start = i
                        while (i < lineLen && (line[i].isLetterOrDigit() || line[i] == '_')) {
                            i++
                        }
                        val word = line.substring(start, i)

                        if (keywords.contains(word)) {
                            withStyle(style = androidx.compose.ui.text.SpanStyle(color = keywordColor, fontWeight = FontWeight.Bold)) {
                                append(word)
                            }
                        } else {
                            withStyle(style = androidx.compose.ui.text.SpanStyle(color = textColor)) {
                                append(word)
                            }
                        }
                    } else if (c.isDigit()) {
                        val start = i
                        while (i < lineLen && (line[i].isDigit() || line[i] == '.' || line[i] == 'x' || line[i] == 'X' || line[i] in 'a'..'f' || line[i] in 'A'..'F' || line[i] == 'L' || line[i] == 'l' || line[i] == 'f' || line[i] == 'F' || line[i] == 'd' || line[i] == 'D')) {
                            i++
                        }
                        withStyle(style = androidx.compose.ui.text.SpanStyle(color = stringColor)) {
                            append(line.substring(start, i))
                        }
                    } else {
                        withStyle(style = androidx.compose.ui.text.SpanStyle(color = textColor)) {
                            append(c.toString())
                        }
                        i++
                    }
                }
            }

            if (lineIndex < lines.size - 1) {
                append("\n")
            }
        }
    }
}

private fun getCommentPrefix(line: String, extension: String): String? {
    if (line.isEmpty()) return null

    val commentPrefixes = when (extension) {
        "java", "kt", "kts", "js", "ts", "c", "cpp", "h", "hpp", "cs", "go", "rust", "swift" -> listOf("//")
        "py", "sh", "bash", "zsh", "yaml", "yml", "toml", "ini", "conf", "cfg", "properties" -> listOf("#")
        "php" -> listOf("//", "#")
        "lua" -> listOf("--")
        "html", "xml" -> listOf("<!--")
        "css" -> listOf("/*")
        else -> listOf("//", "#", "--")
    }

    return commentPrefixes.firstOrNull { line.startsWith(it) }
}


