<?php
require_once __DIR__ . '/../require.php';
require_once __DIR__ . '/../function.php';

$couldId = isset($_GET['could_id']) ? intval($_GET['could_id']) : 0;
$filePath = isset($_GET['file']) ? $_GET['file'] : '';

if ($couldId <= 0 || empty($filePath)) {
    die('参数不正确');
}

$allowedExts = ['java', 'json', 'prop', 'txt'];
$ext = strtolower(pathinfo($filePath, PATHINFO_EXTENSION));
if (!in_array($ext, $allowedExts)) {
    die('不支持预览该文件类型');
}

$pdo = getDBConnection();
if (!$pdo) {
    die('数据库连接失败');
}

$stmt = $pdo->prepare("SELECT plugin_name, file_path FROM plugins WHERE could_id = ?");
$stmt->execute([$couldId]);
$plugin = $stmt->fetch();

if (!$plugin) {
    die('插件不存在');
}

$zipPath = $plugin['file_path'];
if (!file_exists($zipPath)) {
    $projectRoot = realpath(__DIR__ . '/../');
    $pendingPath = $projectRoot . '/uploads/plugins/pending/' . basename($zipPath);
    if (file_exists($pendingPath)) {
        $zipPath = $pendingPath;
    } else {
        die('文件不存在');
    }
}

$content = false;
if (class_exists('ZipArchive')) {
    $zip = new ZipArchive();
    if ($zip->open($zipPath, ZipArchive::RDONLY) === true) {
        $content = $zip->getFromName($filePath);
        $zip->close();
    }
}

if ($content === false) {
    die('无法读取文件内容');
}

$lines = explode("\n", $content);
$totalLines = count($lines);
$displayName = basename($filePath);
$pluginName = htmlspecialchars($plugin['plugin_name']);
?>

<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><?php echo htmlspecialchars($displayName); ?> - 文件预览</title>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body {
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
            background: #f5f5f5;
            color: #333;
        }
        .preview-header {
            background: #fff;
            border-bottom: 1px solid #e8e8e8;
            padding: 14px 20px;
            display: flex;
            align-items: center;
            justify-content: space-between;
            flex-wrap: wrap;
            gap: 10px;
        }
        .preview-header-left {
            display: flex;
            align-items: center;
            gap: 12px;
            min-width: 0;
        }
        .preview-header-left h1 {
            font-size: 15px;
            font-weight: 600;
            color: #1a1a1a;
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
        }
        .preview-meta {
            font-size: 12px;
            color: #999;
            white-space: nowrap;
        }
        .preview-back {
            padding: 6px 16px;
            border: 1px solid #d9d9d9;
            border-radius: 4px;
            background: #fff;
            color: #333;
            font-size: 13px;
            cursor: pointer;
            text-decoration: none;
            white-space: nowrap;
        }
        .preview-back:hover {
            border-color: #1a73e8;
            color: #1a73e8;
        }
        .code-container {
            margin: 0;
            background: #fff;
            border: 1px solid #e8e8e8;
            border-radius: 0;
            overflow: auto;
            max-height: calc(100vh - 57px);
        }
        .code-table {
            border-collapse: collapse;
            width: 100%;
            table-layout: fixed;
        }
        .code-table td {
            vertical-align: top;
            padding: 0;
            line-height: 1.6;
            font-size: 13px;
            font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
            white-space: pre-wrap;
            word-break: break-all;
        }
        .line-num {
            width: 55px;
            min-width: 55px;
            max-width: 55px;
            text-align: right;
            padding: 0 10px 0 0 !important;
            color: #b0b0b0;
            background: #fafafa;
            border-right: 1px solid #e8e8e8;
            user-select: none;
            -webkit-user-select: none;
        }
        .line-content {
            padding: 0 12px !important;
            background: #fff;
        }
        .code-table tr:hover .line-num {
            background: #f0f0f0;
        }
        .code-table tr:hover .line-content {
            background: #fafafa;
        }

        /* Java syntax highlighting */
        .hl-keyword { color: #0033b3; font-weight: bold; }
        .hl-string { color: #067d17; }
        .hl-comment { color: #8c8c8c; font-style: italic; }
        .hl-number { color: #1750eb; }
        .hl-annotation { color: #7f0055; font-weight: bold; }
        .hl-type { color: #0057ae; }
        .hl-constant { color: #ff0000; }

        /* JSON syntax highlighting */
        .hl-json-key { color: #0451a5; }
        .hl-json-string { color: #a31515; }
        .hl-json-number { color: #098658; }
        .hl-json-bool { color: #0000ff; }
        .hl-json-null { color: #0000ff; }
        .hl-json-brace { color: #333; }

        /* Prop syntax highlighting */
        .hl-prop-key { color: #0451a5; }
        .hl-prop-sep { color: #333; }
        .hl-prop-value { color: #a31515; }
        .hl-prop-comment { color: #8c8c8c; font-style: italic; }
    </style>
</head>
<body>
    <div class="preview-header">
        <div class="preview-header-left">
            <h1><?php echo htmlspecialchars($displayName); ?></h1>
            <span class="preview-meta"><?php echo $totalLines; ?> 行</span>
            <span class="preview-meta"><?php echo strtoupper($ext); ?></span>
            <span class="preview-meta"><?php echo $pluginName; ?></span>
        </div>
        <a href="javascript:history.back()" class="preview-back">返回</a>
    </div>
    <div class="code-container">
        <table class="code-table">
            <tbody>
<?php
$needsHighlight = in_array($ext, ['java', 'json']);

foreach ($lines as $i => $line) {
    $lineNum = $i + 1;
    $escaped = htmlspecialchars($line, ENT_QUOTES, 'UTF-8');

    if ($needsHighlight) {
        if ($ext === 'java') {
            $escaped = highlightJava($escaped);
        } elseif ($ext === 'json') {
            $escaped = highlightJson($escaped);
        }
    } elseif ($ext === 'prop') {
        $escaped = highlightProp($escaped);
    }

    echo '<tr><td class="line-num">' . $lineNum . '</td><td class="line-content">' . ($escaped ?: ' ') . '</td></tr>' . "\n";
}
?>
            </tbody>
        </table>
    </div>
</body>
</html>

<?php
function highlightJava($line) {
    // Comments
    if (preg_match('/^(\s*)(\/\/.*)$/', $line, $m)) {
        return $m[1] . '<span class="hl-comment">' . $m[2] . '</span>';
    }

    $patterns = [
        // Annotations
        '/(@\w+)/' => '<span class="hl-annotation">$1</span>',
        // Strings (double and single quoted)
        '/(&quot;[^&]*?&quot;)/' => '<span class="hl-string">$1</span>',
        // Keywords
        '/\b(abstract|assert|boolean|break|byte|case|catch|char|class|continue|default|do|double|else|enum|extends|final|finally|float|for|if|implements|import|instanceof|int|interface|long|native|new|null|package|private|protected|public|return|short|static|strictfp|super|switch|synchronized|this|throw|throws|transient|try|void|volatile|while|true|false)\b/' => '<span class="hl-keyword">$1</span>',
        // Types
        '/\b(String|Integer|Long|Double|Float|Boolean|Object|List|Map|Set|ArrayList|HashMap|HashSet|Exception|Throwable|Override|Deprecated|SuppressWarnings)\b/' => '<span class="hl-type">$1</span>',
        // Numbers
        '/\b(\d+[lL]?[fFdD]?)\b/' => '<span class="hl-number">$1</span>',
    ];

    foreach ($patterns as $pattern => $replacement) {
        $line = preg_replace($pattern, $replacement, $line);
    }

    return $line;
}

function highlightJson($line) {
    // JSON key-value pattern: "key": value
    $line = preg_replace('/(&quot;)(.*?)(&quot;)\s*:/', '<span class="hl-json-key">$1$2$3</span>:', $line);
    // String values (not already highlighted as keys)
    $line = preg_replace('/:\s*(&quot;)(.*?)(&quot;)/', ': <span class="hl-json-string">$1$2$3</span>', $line);
    // Standalone strings in arrays
    $line = preg_replace('/(?<=\[|,\s)(&quot;)(.*?)(&quot;)/', '<span class="hl-json-string">$1$2$3</span>', $line);
    // Numbers
    $line = preg_replace('/:\s*(\d+\.?\d*)\b/', ': <span class="hl-json-number">$1</span>', $line);
    // Booleans and null
    $line = preg_replace('/:\s*(true|false|null)\b/', ': <span class="hl-json-bool">$1</span>', $line);
    return $line;
}

function highlightProp($line) {
    $trimmed = ltrim($line);
    if (strpos($trimmed, '#') === 0 || strpos($trimmed, '!') === 0) {
        return '<span class="hl-prop-comment">' . $line . '</span>';
    }
    $eqPos = strpos($line, '=');
    if ($eqPos !== false) {
        $key = substr($line, 0, $eqPos);
        $val = substr($line, $eqPos + 1);
        return '<span class="hl-prop-key">' . htmlspecialchars($key, ENT_QUOTES, 'UTF-8') . '</span><span class="hl-prop-sep">=</span><span class="hl-prop-value">' . htmlspecialchars($val, ENT_QUOTES, 'UTF-8') . '</span>';
    }
    return $line;
}
?>
