<?php
/**
 * 更新用户签名接口
 */

require_once __DIR__ . '/../../require.php';
require_once __DIR__ . '/../../function.php';

// 检查用户是否登录
if (!isUserLoggedIn()) {
    jsonResponse(401, '请先登录');
}

$userQQ = getCurrentUserQQ();
$pdo = getDBConnection();

// 检查用户是否是赞助用户
$stmt = $pdo->prepare("SELECT is_sponsor FROM users WHERE qq = ?");
$stmt->execute([$userQQ]);
$user = $stmt->fetch();

if (!$user || $user['is_sponsor'] != 1) {
    jsonResponse(403, '只有赞助用户才能修改个性签名');
}

// 处理POST请求
if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    jsonResponse(400, '不支持的请求方式');
}

try {
    // 获取JSON数据
    $input = file_get_contents('php://input');
    $data = json_decode($input, true);

    if ($data === null) {
        jsonResponse(400, '数据格式错误');
    }

    $signature = isset($data['signature']) ? sanitizeInput($data['signature']) : '';
    $csrfToken = isset($data['csrf_token']) ? $data['csrf_token'] : '';

    // 验证CSRF Token
    if (!verifyCSRFToken($csrfToken)) {
        jsonResponse(400, '安全验证失败');
    }

    // 验证签名长度（最多500字）
    if (strlen($signature) > 500) {
        jsonResponse(400, '签名长度不能超过500字');
    }

    // 更新签名
    $stmt = $pdo->prepare("UPDATE users SET signature = ? WHERE qq = ?");
    $stmt->execute([$signature, $userQQ]);

    logMessage("赞助用户 {$userQQ} 更新签名", 'INFO');

    jsonResponse(200, '签名修改成功', [
        'signature' => $signature
    ]);

} catch (Exception $e) {
    error_log("更新签名失败: " . $e->getMessage());
    logMessage("更新签名失败: " . $e->getMessage(), 'ERROR');
    jsonResponse(500, '修改失败，请稍后重试');
}
?>