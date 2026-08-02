<?php
/**
 * 忘记密码API接口
 */

require_once __DIR__ . '/../../require.php';
require_once __DIR__ . '/../../function.php';

// 只接受POST请求
if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    jsonResponse(400, '不支持的请求方式');
}

try {
    $action = isset($_POST['action']) ? sanitizeInput($_POST['action']) : '';
    $csrfToken = isset($_POST['csrf_token']) ? $_POST['csrf_token'] : '';

    // 验证CSRF Token
    if (!verifyCSRFToken($csrfToken)) {
        jsonResponse(400, '安全验证失败，请刷新页面重试');
    }

    $pdo = getDBConnection();
    if (!$pdo) {
        jsonResponse(500, '数据库连接失败');
    }

    if ($action === 'send_code') {
        // 发送验证码
        $qq = isset($_POST['qq']) ? sanitizeInput($_POST['qq']) : '';
        $email = $qq . "@qq.com";
        $captcha = isset($_POST['captcha']) ? sanitizeInput($_POST['captcha']) : '';

        // 验证图形验证码
        if (!verifyCaptcha($captcha)) {
            jsonResponse(400, '验证码错误');
        }

        // 验证QQ号
        if (!preg_match('/^[1-9][0-9]{4,10}$/', $qq)) {
            jsonResponse(400, '请输入正确的QQ号');
        }

        // 验证邮箱格式
        if (!filter_var($email, FILTER_VALIDATE_EMAIL)) {
            jsonResponse(400, '请输入正确的邮箱地址');
        }

        // 查询用户
        $stmt = $pdo->prepare("SELECT * FROM users WHERE qq = ?");
        $stmt->execute([$qq]);
        $user = $stmt->fetch();

        if (!$user) {
            jsonResponse(400, '用户不存在');
        }

        // 生成验证码
        $code = generateVerificationCode(6);

        // 保存验证码到数据库
        $stmt = $pdo->prepare("INSERT INTO email_codes (email, code, type, expires_at) VALUES (?, ?, 'reset_password', ?)");
        $expiresAt = date('Y-m-d H:i:s', strtotime('+10 minutes'));
        $stmt->execute([$email, $code, $expiresAt]);

        // 发送验证码邮件
        if (sendVerificationCodeEmail($email, $code)) {
            jsonResponse(200, '验证码已发送至您的邮箱->'.$email);
        } else {
            jsonResponse(500, '验证码发送失败，请稍后重试');
        }

    } elseif ($action === 'reset_password') {
        // 重置密码
        $qq = isset($_POST['qq']) ? sanitizeInput($_POST['qq']) : '';
        $email = $qq . "@qq.com";
        $emailCode = isset($_POST['email_code']) ? sanitizeInput($_POST['email_code']) : '';
        $newPassword = isset($_POST['new_password']) ? $_POST['new_password'] : '';
        $confirmPassword = isset($_POST['confirm_password']) ? $_POST['confirm_password'] : '';

        // 验证QQ号
        if (!preg_match('/^[1-9][0-9]{4,10}$/', $qq)) {
            jsonResponse(400, '请输入正确的QQ号');
        }

        // 验证邮箱格式
        if (!filter_var($email, FILTER_VALIDATE_EMAIL)) {
            jsonResponse(400, '请输入正确的邮箱地址');
        }

        // 验证邮箱验证码
        if (empty($emailCode)) {
            jsonResponse(400, '请输入验证码');
        }

        // 验证密码长度
        if (strlen($newPassword) < PASSWORD_MIN_LENGTH || strlen($newPassword) > PASSWORD_MAX_LENGTH) {
            jsonResponse(400, '密码长度应在' . PASSWORD_MIN_LENGTH . '-' . PASSWORD_MAX_LENGTH . '位之间');
        }

        // 验证两次密码是否一致
        if ($newPassword !== $confirmPassword) {
            jsonResponse(400, '两次输入的密码不一致');
        }

        // 验证邮箱验证码
        $stmt = $pdo->prepare("SELECT * FROM email_codes WHERE email = ? AND code = ? AND type = 'reset_password' AND used = 0 AND expires_at > ? ORDER BY created_at DESC LIMIT 1");
        $stmt->execute([$email, $emailCode, date('Y-m-d H:i:s')]);
        $codeRecord = $stmt->fetch();

        if (!$codeRecord) {
            jsonResponse(400, '验证码错误或已过期');
        }

        // 标记验证码已使用
        $stmt = $pdo->prepare("UPDATE email_codes SET used = 1 WHERE id = ?");
        $stmt->execute([$codeRecord['id']]);

        // 加密新密码
        $hashedPassword = encryptPassword($newPassword);

        // 更新密码
        $stmt = $pdo->prepare("UPDATE users SET password = ? WHERE qq = ?");
        $stmt->execute([$hashedPassword, $qq]);

        jsonResponse(200, '密码重置成功', ['redirect' => 'user/login.php']);

    } else {
        jsonResponse(400, '无效操作');
    }

} catch (Exception $e) {
    error_log("忘记密码操作失败: " . $e->getMessage());
    jsonResponse(500, '操作失败，请稍后重试');
}
?>