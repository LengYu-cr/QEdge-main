<?php
/**
 * 插件删除API接口
 */

require_once __DIR__ . '/../../require.php';
require_once __DIR__ . '/../../function.php';

header('Content-Type: application/json');

// 检查管理员登录状态
if (!isAdminLoggedIn()) {
    jsonResponse(401, '请先登录管理员账号');
}

$adminQQ = getCurrentAdminQQ();
$pdo = getDBConnection();

if (!$pdo) {
    jsonResponse(500, '数据库连接失败');
}

// 检查审核权限（只有有审核权限的管理员才能删除）
$stmt = $pdo->prepare("SELECT review_permission FROM users WHERE qq = ?");
$stmt->execute([$adminQQ]);
$user = $stmt->fetch();

if ($user && $user['review_permission'] != 1) {
    jsonResponse(403, '您没有删除插件的权限');
}

// 检查请求方法
if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    jsonResponse(405, '请求方法不正确');
}

// 验证CSRF Token
$csrfToken = isset($_POST['csrf_token']) ? $_POST['csrf_token'] : '';
if (!verifyCSRFToken($csrfToken)) {
    jsonResponse(400, '安全验证失败');
}

// 获取插件 could_id
$couldId = isset($_POST['could_id']) ? intval($_POST['could_id']) : 0;
if ($couldId <= 0) {
    jsonResponse(400, '插件ID不正确');
}

try {
    // 获取插件信息
    $stmt = $pdo->prepare("SELECT * FROM plugins WHERE could_id = ?");
    $stmt->execute([$couldId]);
    $plugin = $stmt->fetch();

    if (!$plugin) {
        jsonResponse(404, '插件不存在');
    }

    // 开始事务
    $pdo->beginTransaction();

    // 删除文件（如果存在）
    $filePath = $plugin['file_path'];
    if (file_exists($filePath)) {
        if (unlink($filePath)) {
            error_log("删除文件成功: " . $filePath);
        } else {
            error_log("删除文件失败: " . $filePath);
        }
    } else {
        error_log("文件不存在，跳过删除: " . $filePath);
    }

    // 从数据库中删除记录
    $stmt = $pdo->prepare("DELETE FROM plugins WHERE could_id = ?");
    $stmt->execute([$couldId]);

    $pdo->commit();

    logMessage("管理员 {$adminQQ} 删除了插件 {$plugin['plugin_name']} (plugin_id: {$plugin['plugin_id']}, could_id: {$couldId})", 'INFO');
    jsonResponse(200, '插件已删除成功');

} catch (Exception $e) {
    if ($pdo && $pdo->inTransaction()) {
        $pdo->rollBack();
    }
    error_log("插件删除失败: " . $e->getMessage());
    jsonResponse(500, '删除失败: ' . $e->getMessage());
}
?>