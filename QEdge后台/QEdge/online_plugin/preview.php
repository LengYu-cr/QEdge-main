<?php
require_once __DIR__ . '/../require.php';
require_once __DIR__ . '/../function.php';

$couldId = isset($_GET['could_id']) ? intval($_GET['could_id']) : 0;
$filePath = isset($_GET['file']) ? $_GET['file'] : '';

if ($couldId <= 0 || empty($filePath)) {
    die('参数不正确');
}

$allowedExts = ['java', 'js', 'json', 'prop', 'txt'];
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
        .code-pre {
            margin: 0;
        }
        .code-pre code.hljs {
            padding: 0;
            font-size: 13px;
            line-height: 1.6;
            font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
            background: #fff;
        }
        /* highlight.js 行号插件的表格样式 */
        .hljs-ln { width: 100%; }
        .hljs-ln td { padding: 0; vertical-align: top; }
        .hljs-ln-numbers {
            text-align: right;
            color: #b0b0b0;
            background: #fafafa;
            border-right: 1px solid #e8e8e8;
            user-select: none;
            -webkit-user-select: none;
            width: 1%;
            white-space: nowrap;
        }
        .hljs-ln-n { padding: 0 12px 0 14px; display: block; }
        .hljs-ln-code {
            padding: 0 14px;
            white-space: pre-wrap;
            word-break: break-all;
        }
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
<?php
// highlight.js 语言映射：prop/txt 不做语法高亮（plaintext）
$langMap = ['java' => 'java', 'js' => 'javascript', 'json' => 'json', 'prop' => 'properties', 'txt' => 'plaintext'];
$hljsLang = isset($langMap[$ext]) ? $langMap[$ext] : 'plaintext';
?>
        <pre class="code-pre"><code class="language-<?php echo $hljsLang; ?>"><?php echo htmlspecialchars($content, ENT_QUOTES, 'UTF-8'); ?></code></pre>
    </div>

    <!-- highlight.js：成熟的语法高亮库，自动识别 Java/JS/JSON 等，避免手写正则的边界问题 -->
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/styles/github.min.css">
    <script src="https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/highlight.min.js"></script>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/highlightjs-line-numbers.js/2.8.0/highlightjs-line-numbers.min.js"></script>
    <script>
        document.addEventListener('DOMContentLoaded', function () {
            document.querySelectorAll('pre code').forEach(function (block) {
                hljs.highlightElement(block);
                if (window.hljs && hljs.lineNumbersBlock) {
                    hljs.lineNumbersBlock(block);
                }
            });
        });
    </script>
</body>
</html>

