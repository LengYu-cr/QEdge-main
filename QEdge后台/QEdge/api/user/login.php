<?php
/**
 * 用户登录API接口
 */

require_once __DIR__ . '/../../require.php';
require_once __DIR__ . '/../../function.php';

// 如果已登录，直接返回成功
if (isUserLoggedIn()) {
    jsonResponse(200, '已登录', ['redirect' => 'user/index.php']);
}

// 只接受POST请求
if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    jsonResponse(400, '不支持的请求方式');
}

try {
    $qq = isset($_POST['qq']) ? sanitizeInput($_POST['qq']) : '';
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

    // 验证QQ号
    if (!preg_match('/^[1-9][0-9]{4,10}$/', $qq)) {
        jsonResponse(400, '请输入正确的QQ号');
    }

    // 验证密码
    if (empty($password)) {
        jsonResponse(400, '请输入密码');
    }

    $pdo = getDBConnection();
    if (!$pdo) {
        jsonResponse(500, '数据库连接失败');
    }

    // 检查黑名单（包括未注册用户）
    $stmt = $pdo->prepare("SELECT qq FROM banned_users WHERE qq = ?");
    $stmt->execute([$qq]);
    $isBanned = $stmt->fetch();

    if ($isBanned) {
        jsonResponse(403, '您的账号已被拉黑，无法登录');
    }

    // 查询用户
    $stmt = $pdo->prepare("SELECT * FROM users WHERE qq = ?");
    $stmt->execute([$qq]);
    $user = $stmt->fetch();

    if (!$user) {
        jsonResponse(400, '用户不存在，请先使用模块登录');
    }

    // 验证密码
    if (!verifyPassword($password, $user['password'])) {
        jsonResponse(400, '密码错误');
    }

    // 登录成功：重新生成会话ID，删除旧会话文件（防会话固定）
    session_regenerate_id(true);
    $_SESSION['user_qq'] = $user['qq'];
    $_SESSION['user_nickname'] = $user['nickname'];
    $_SESSION['user_logged_in'] = true;
    $_SESSION['user_login_time'] = time();            // 记录登录时间，用于过期校验
    $_SESSION['user_last_active'] = time();           // 上次活跃时间，用于刷新 cookie 生命周期

    // 再手动刷新一次 cookie：明确告诉浏览器 session 要存 7 天（双保险，PHP 有时 session_start 后的 setcookie 比 INI 更可靠）
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

    // 更新最后登录信息
    updateUserLastLogin($pdo, $qq);

    jsonResponse(200, '登录成功', ['redirect' => 'user/index.php']);

} catch (Exception $e) {
    error_log("用户登录失败: " . $e->getMessage());
    jsonResponse(500, '登录失败，请稍后重试');
}
?>