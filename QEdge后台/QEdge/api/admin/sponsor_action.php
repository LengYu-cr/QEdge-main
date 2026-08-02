<?php
/**
 * 管理员赞助用户操作API接口
 */

require_once __DIR__ . '/../../require.php';
require_once __DIR__ . '/../../function.php';

// 检查管理员是否登录
if (!isAdminLoggedIn()) {
    jsonResponse(401, '请先登录');
}

// 只接受POST请求
if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    jsonResponse(400, '不支持的请求方式');
}

try {
    $action = isset($_POST['action']) ? sanitizeInput($_POST['action']) : '';
    $qq = isset($_POST['qq']) ? sanitizeInput($_POST['qq']) : '';
    $csrfToken = isset($_POST['csrf_token']) ? $_POST['csrf_token'] : '';

    // 验证CSRF Token
    if (!verifyCSRFToken($csrfToken)) {
        jsonResponse(400, '安全验证失败');
    }

    $pdo = getDBConnection();
    if (!$pdo) {
        jsonResponse(500, '数据库连接失败');
    }

    if ($action === 'add') {
        // 验证QQ号
        if (!preg_match('/^[1-9][0-9]{4,10}$/', $qq)) {
            jsonResponse(400, '请输入正确的QQ号');
        }

        // 获取金额和备注
        $amount = isset($_POST['amount']) ? floatval($_POST['amount']) : 0;
        $note = isset($_POST['note']) ? sanitizeInput($_POST['note']) : '';

        // 验证金额
        if ($amount < 0 || $amount > 99999999.99) {
            jsonResponse(400, '赞助金额不合法');
        }

        // 获取昵称
        $nickname = getQQNickname($qq);

        // 检查用户是否存在
        $stmt = $pdo->prepare("SELECT * FROM users WHERE qq = ?");
        $stmt->execute([$qq]);
        $user = $stmt->fetch();

        // 检查是否已经是赞助用户
        $stmt = $pdo->prepare("SELECT qq FROM sponsor_users WHERE qq = ?");
        $stmt->execute([$qq]);
        $isSponsor = $stmt->fetch();

        if ($isSponsor) {
            jsonResponse(400, '该用户已是赞助用户');
        }

        // 如果用户已注册，更新users表
        if ($user) {
            $stmt = $pdo->prepare("UPDATE users SET is_sponsor = 1, nickname = COALESCE(?, nickname) WHERE qq = ?");
            $stmt->execute([$nickname ?: null, $qq]);
        }

        // 添加到赞助用户表
        $stmt = $pdo->prepare("INSERT INTO sponsor_users (qq, nickname, amount, note) VALUES (?, ?, ?, ?)");
        $stmt->execute([$qq, $nickname ?: null, $amount, $note ?: null]);

        logMessage("管理员手动添加赞助用户 {$qq}" . ($nickname ? " (昵称: {$nickname})" : "") . " 金额: {$amount}" . ($note ? " 备注: {$note}" : ""), 'INFO');
        jsonResponse(200, '已添加为赞助用户' . ($nickname ? " (昵称: {$nickname})" : "") . " 金额: {$amount}元");

    } elseif ($action === 'remove') {
        // 如果用户已注册，更新users表
        $stmt = $pdo->prepare("UPDATE users SET is_sponsor = 0 WHERE qq = ?");
        $stmt->execute([$qq]);

        // 从赞助用户表移除
        $stmt = $pdo->prepare("DELETE FROM sponsor_users WHERE qq = ?");
        $stmt->execute([$qq]);

        logMessage("管理员取消赞助用户 {$qq}", 'INFO');
        jsonResponse(200, '已取消赞助状态');

    } else {
        jsonResponse(400, '无效操作');
    }

} catch (Exception $e) {
    error_log("赞助操作失败: " . $e->getMessage());
    jsonResponse(500, '操作失败');
}
?>