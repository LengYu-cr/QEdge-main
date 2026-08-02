<?php
/**
 * 管理员安全退出系统
 */

require_once __DIR__ . '/../require.php';
require_once __DIR__ . '/../function.php';

// 记录退出日志
if (isAdminLoggedIn()) {
    logMessage("管理员退出登录", 'INFO');
}

// 清除会话数据
$_SESSION = [];    // 先清空变量
session_unset();   // 释放 $_SESSION 中全部变量
session_destroy(); // 删除服务器端 session 文件

// 关键：手动删除浏览器端的会话 Cookie（session_destroy 不会删除客户端 cookie！）
if (ini_get('session.use_cookies')) {
    $cookieParams = session_get_cookie_params();
    setcookie(
        SESSION_NAME,
        '',
        time() - 42000,       // 过期时间设为过去
        $cookieParams['path'],
        $cookieParams['domain'],
        $cookieParams['secure'],
        $cookieParams['httponly']
    );
    // 再加一份通用 path=/ 的删除，防不同 php 版本 session_get_cookie_params 拿不到 path
    setcookie(SESSION_NAME, '', time() - 42000, '/');
}

// 重定向到登录页面
header('Location: login.php');
exit;
?>