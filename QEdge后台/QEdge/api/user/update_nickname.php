<?php
/**
 * 更新用户昵称接口
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

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    jsonResponse(400, '不支持的请求方式');
}

try {
    $input = file_get_contents('php://input');
    $data = json_decode($input, true);

    if ($data === null) {
        jsonResponse(400, '数据格式错误');
    }

    $nickname = isset($data['nickname']) ? trim($data['nickname']) : '';
    $csrfToken = isset($data['csrf_token']) ? $data['csrf_token'] : '';

    if (!verifyCSRFToken($csrfToken)) {
        jsonResponse(400, '安全验证失败');
    }

    if (empty($nickname)) {
        jsonResponse(400, '昵称不能为空');
    }

    if (mb_strlen($nickname) > 20) {
        jsonResponse(400, '昵称长度不能超过20个字符');
    }

    $stmt = $pdo->prepare("UPDATE users SET nickname = ? WHERE qq = ?");
    $stmt->execute([$nickname, $userQQ]);

    logMessage("用户 {$userQQ} 修改昵称为: {$nickname}", 'INFO');

    jsonResponse(200, '昵称修改成功', [
        'nickname' => $nickname
    ]);

} catch (Exception $e) {
    error_log("更新昵称失败: " . $e->getMessage());
    logMessage("更新昵称失败: " . $e->getMessage(), 'ERROR');
    jsonResponse(500, '修改失败，请稍后重试');
}
?>
