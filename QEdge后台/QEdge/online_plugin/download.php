<?php
require_once __DIR__ . '/../require.php';
require_once __DIR__ . '/../function.php';

// 获取 could_id（自增主键）
$couldId = isset($_GET['id']) ? intval($_GET['id']) : 0;

if ($couldId <= 0) {
    jsonResponse(400, '请提供有效的插件ID');
}

try {
    $pdo = getDBConnection();
    if (!$pdo) {
        jsonResponse(500, '数据库连接失败');
    }

    // 使用 could_id 查询插件（status = 1 表示已审核通过）
    $stmt = $pdo->prepare("SELECT * FROM plugins WHERE could_id = ? AND status = 1");
    $stmt->execute([$couldId]);
    $plugin = $stmt->fetch();

    if (!$plugin) {
        jsonResponse(404, '插件不存在或已被禁用');
    }

    // 更新下载次数
    $stmt = $pdo->prepare("UPDATE plugins SET download_count = download_count + 1 WHERE could_id = ?");
    $stmt->execute([$couldId]);
    // exit($plugin['file_path']);
    // 检查文件是否存在
    if (!file_exists($plugin['file_path'])) {
        jsonResponse(404, '文件不存在');
    }

    // 生成下载文件名：插件名_版本号.zip
    $fileName = sanitizeInput($plugin['plugin_name']) . '_v' . sanitizeInput($plugin['version_code']) . '.zip';

    // 设置下载头
    header('Content-Type: application/zip');
    header('Content-Disposition: attachment; filename="' . $fileName . '"');
    header('Content-Length: ' . filesize($plugin['file_path']));
    header('Cache-Control: no-cache, no-store, must-revalidate');
    header('Pragma: no-cache');
    header('Expires: 0');

    // 输出文件

    readfile($plugin['file_path']);

    // 记录下载日志
    logMessage("插件 {$plugin['plugin_name']} (plugin_id: {$plugin['plugin_id']}, could_id: {$couldId}, 版本: {$plugin['version_code']}) 被下载", 'INFO');

    exit;

} catch (Exception $e) {
    error_log("下载失败: " . $e->getMessage());
    logMessage("下载失败: " . $e->getMessage(), 'ERROR');
    jsonResponse(500, '下载失败，请稍后重试');
}
?>