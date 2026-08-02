<?php
/**
 * 预览zip文件内容API（仅限有审核权限的用户）
 */

require_once __DIR__ . '/../../require.php';
require_once __DIR__ . '/../../function.php';

header('Content-Type: application/json; charset=utf-8');

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
    $stmt = $pdo->prepare("SELECT plugin_name, file_path FROM plugins WHERE could_id = ?");
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

    if (!class_exists('ZipArchive')) {
        jsonResponse(500, '服务器不支持ZipArchive扩展');
    }

    $zip = new ZipArchive();
    $res = $zip->open($filePath, ZipArchive::RDONLY);
    if ($res !== true) {
        jsonResponse(500, '无法打开zip文件，错误码: ' . $res);
    }

    $files = [];
    for ($i = 0; $i < $zip->numFiles; $i++) {
        $stat = $zip->statIndex($i);
        $name = $stat['name'];
        $isDir = substr($name, -1) === '/';
        $files[] = [
            'name' => $isDir ? rtrim($name, '/') : $name,
            'size' => $stat['size'],
            'is_dir' => $isDir
        ];
    }
    $zip->close();

    jsonResponse(200, '获取成功', [
        'plugin_name' => $plugin['plugin_name'],
        'total_files' => count($files),
        'files' => $files
    ]);

} catch (Exception $e) {
    error_log("预览zip失败: " . $e->getMessage());
    jsonResponse(500, '预览失败: ' . $e->getMessage());
}
