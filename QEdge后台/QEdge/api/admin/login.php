<?php
/**
 * 管理员登录API接口
 */

require_once __DIR__ . '/../../require.php';
require_once __DIR__ . '/../../function.php';

// 如果已登录，直接返回成功
if (isAdminLoggedIn()) {
    jsonResponse(200, '已登录', ['redirect' => 'admin/index.php']);
}

// 只接受POST请求
if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    jsonResponse(400, '不支持的请求方式');
}

try {
    $username = isset($_POST['username']) ? sanitizeInput($_POST['username']) : '';
    $password = isset($_POST['password']) ? $_POST['password'] : '';
    $captcha = isset($_POST['captcha']) ? sanitizeInput($_POST['captcha']) : '';
    $csrfToken = isset($_POST['csrf_token']) ? $_POST['csrf_token'] : '';

    // 验证CSRF Token
    if (!verifyCSRFToken($csrfToken)) {
        jsonResponse(400, '安全验证失败，请刷新页面重试');
    }

    // 验证图形验证码
    if (!verifyCaptcha($captcha)) {
        jsonResponse(400, '验证码错误');
    }

    // 验证用户名和密码（硬编码管理员账号）
    if ($username !== ADMIN_USERNAME || $password !== ADMIN_PASSWORD) {
        jsonResponse(400, '用户名或密码错误');
    }

    // 登录成功：重新生成会话ID，删除旧会话文件（防会话固定）
    session_regenerate_id(true);
    $_SESSION['admin_logged_in'] = true;
    $_SESSION['admin_username'] = ADMIN_USERNAME;
    $_SESSION['admin_login_time'] = time();      // 记录登录时间，用于过期校验
    $_SESSION['admin_last_active'] = time();     // 上次活跃时间，用于刷新 cookie 生命周期

    // 再手动刷新一次 cookie：明确告诉浏览器 session 要存 7 天（双保险）
    if (!headers_sent()) {
        setcookie(
            SESSION_NAME,
            session_id(),
            time() + SESSION_LIFETIME,
            '/',
            '',
            (!empty($_SERVER['HTTPS']) && $_SERVER['HTTPS'] !== 'off'),
            true    // httponly
        );
    }

    logMessage("管理员 {$username} 登录成功", 'INFO');

    jsonResponse(200, '登录成功', ['redirect' => 'admin/index.php']);

} catch (Exception $e) {
    error_log("管理员登录失败: " . $e->getMessage());
    jsonResponse(500, '登录失败，请稍后重试');
}
?>