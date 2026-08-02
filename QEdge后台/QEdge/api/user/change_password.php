<?php
/**
 * 修改密码API接口
 */

require_once __DIR__ . '/../../require.php';
require_once __DIR__ . '/../../function.php';

// 检查用户是否登录
if (!isUserLoggedIn()) {
    jsonResponse(401, '请先登录');
}

// 只接受POST请求
if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    jsonResponse(400, '不支持的请求方式');
}

try {
    $userQQ = getCurrentUserQQ();
    $newPassword = isset($_POST['new_password']) ? $_POST['new_password'] : '';
    $confirmPassword = isset($_POST['confirm_password']) ? $_POST['confirm_password'] : '';
    $csrfToken = isset($_POST['csrf_token']) ? $_POST['csrf_token'] : '';

    // 验证CSRF Token
    if (!verifyCSRFToken($csrfToken)) {
        jsonResponse(400, '安全验证失败，请刷新页面重试');
    }

    // 验证密码长度
    if (strlen($newPassword) < PASSWORD_MIN_LENGTH || strlen($newPassword) > PASSWORD_MAX_LENGTH) {
        jsonResponse(400, '密码长度应在' . PASSWORD_MIN_LENGTH . '-' . PASSWORD_MAX_LENGTH . '位之间');
    }

    // 验证两次密码是否一致
    if ($newPassword !== $confirmPassword) {
        jsonResponse(400, '两次输入的密码不一致');
    }

    $pdo = getDBConnection();
    if (!$pdo) {
        jsonResponse(500, '数据库连接失败');
    }

    // 加密新密码
    $hashedPassword = encryptPassword($newPassword);

    // 更新密码
    $stmt = $pdo->prepare("UPDATE users SET password = ? WHERE qq = ?");
    $stmt->execute([$hashedPassword, $userQQ]);

    logMessage("用户 {$userQQ} 修改密码", 'INFO');

    jsonResponse(200, '密码修改成功', ['redirect' => 'user/logout.php']);

} catch (Exception $e) {
    error_log("修改密码失败: " . $e->getMessage());
    jsonResponse(500, '密码修改失败，请稍后重试');
}
?>