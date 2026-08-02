<?php
/**
 * 读取zip内单个文件内容API（审核权限）
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
$fileName = isset($_GET['file']) ? $_GET['file'] : '';

if ($couldId <= 0 || empty($fileName)) {
    jsonResponse(400, '参数不正确');
}

$allowedExts = ['java', 'json', 'prop', 'txt'];
$ext = strtolower(pathinfo($fileName, PATHINFO_EXTENSION));
if (!in_array($ext, $allowedExts)) {
    jsonResponse(400, '不支持预览该文件类型');
}

try {
    $stmt = $pdo->prepare("SELECT file_path FROM plugins WHERE could_id = ?");
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

    $zip = new ZipArchive();
    $res = $zip->open($filePath, ZipArchive::RDONLY);
    if ($res !== true) {
        jsonResponse(500, '无法打开zip文件');
    }

    $content = $zip->getFromName($fileName);
    $zip->close();

    if ($content === false) {
        jsonResponse(404, '文件在压缩包中不存在');
    }

    $content = mb_convert_encoding($content, 'UTF-8', 'UTF-8');

    jsonResponse(200, '获取成功', [
        'file_name' => $fileName,
        'file_ext' => $ext,
        'content' => $content
    ]);

} catch (Exception $e) {
    error_log("读取zip文件内容失败: " . $e->getMessage());
    jsonResponse(500, '读取失败');
}
