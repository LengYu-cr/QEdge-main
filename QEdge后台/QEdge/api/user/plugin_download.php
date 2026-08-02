<?php
/**
 * 审核员下载脚本API（不限制审核状态）
 */

require_once __DIR__ . '/../../require.php';
require_once __DIR__ . '/../../function.php';

if (!isUserLoggedIn()) {
    jsonResponse(401, '请先登录');
}

$userQQ = getCurrentUserQQ();
$pdo = getDBConnection();

if (!$pdo) {
    jsonResponse(500, '数据库连接失败');
}

$stmt = $pdo->prepare("SELECT review_permission FROM users WHERE qq = ?");
$stmt->execute([$userQQ]);
$user = $stmt->fetch();

if (!$user || $user['review_permission'] != 1) {
    jsonResponse(403, '没有审核权限');
}

$couldId = isset($_GET['could_id']) ? intval($_GET['could_id']) : 0;
if ($couldId <= 0) {
    jsonResponse(400, '插件ID不正确');
}

try {
    $stmt = $pdo->prepare("SELECT plugin_name, version_code, file_path FROM plugins WHERE could_id = ?");
    $stmt->execute([$couldId]);
    $plugin = $stmt->fetch();

    if (!$plugin) {
        jsonResponse(404, '插件不存在');
    }

    $filePath = $plugin['file_path'];
    if (!file_exists($filePath)) {
        $projectRoot = realpath(__DIR__ . '/../../');
        $pendingPath = $projectRoot . '/uploads/plugins/pending/' . basename($filePath);
        if (file_exists($pendingPath)) {
            $filePath = $pendingPath;
        } else {
            jsonResponse(404, '文件不存在');
        }
    }

    $fileName = sanitizeInput($plugin['plugin_name']) . '_v' . sanitizeInput($plugin['version_code']) . '.zip';

    header('Content-Type: application/zip');
    header('Content-Disposition: attachment; filename="' . $fileName . '"');
    header('Content-Length: ' . filesize($filePath));
    header('Cache-Control: no-cache, no-store, must-revalidate');
    header('Pragma: no-cache');
    header('Expires: 0');

    readfile($filePath);

    logMessage("审核员 {$userQQ} 下载脚本 {$plugin['plugin_name']} (could_id: {$couldId})", 'INFO');
    exit;

} catch (Exception $e) {
    error_log("审核员下载失败: " . $e->getMessage());
    jsonResponse(500, '下载失败');
}
