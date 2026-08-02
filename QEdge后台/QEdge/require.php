<?php
/**
 * QEdge系统配置文件
 * 适配PHP 7.3
 */

// 错误报告设置
error_reporting(E_ALL);
ini_set('display_errors', 1);
ini_set('display_startup_errors', 1);
ini_set('log_errors', 1);
ini_set('error_log', __DIR__ . '/logs/error.log');

// ===== 全局字符集统一 UTF-8 =====
ini_set('default_charset', 'UTF-8');
if (function_exists('mb_internal_encoding')) mb_internal_encoding('UTF-8');
if (function_exists('mb_http_output'))       mb_http_output('UTF-8');
if (function_exists('mb_regex_encoding'))    mb_regex_encoding('UTF-8');
if (function_exists('iconv_set_encoding')) {
    @iconv_set_encoding('input_encoding',    'UTF-8');
    @iconv_set_encoding('output_encoding',   'UTF-8');
    @iconv_set_encoding('internal_encoding', 'UTF-8');
}
// 注意：PHP 7.x 之后 mb_http_input() 移除了第 2 个参数，只能查询无法设置
// 输入编码统一由 function.php 的 decodeUtf8Input() + forceUtf8() 兜底处理
if (function_exists('mb_http_input')) {
    // 仅读取不设置，避免 ArgumentCountError
    @mb_http_input('GPC');
}

// 时区设置
date_default_timezone_set('Asia/Shanghai');

// 数据库配置
define('DataBase_HOST', '127.0.0.1');
define('DataBase_USER', 'qedge');
define('DataBase_PASS', 'QEdge114514.');
define('DataBase_NAME', 'qedge');
define('DataBase_CHARSET', 'utf8mb4');

// 管理员账号配置（硬编码）
define('ADMIN_USERNAME', 'lengyu');
define('ADMIN_PASSWORD', 'QEdge114514.');
define('ADMIN_QQ', '1431136407');

// SMTP邮箱配置
define('SMTP_HOST', 'smtp.qq.com');
define('SMTP_PORT', 465);
define('SMTP_USER', '1431136407@qq.com');
define('SMTP_PASS', 'fgiimwilgpfkjjcg'); // 请在此处填写授权码
define('SMTP_FROM', '1431136407@qq.com');
define('SMTP_FROM_NAME', 'QEdge系统');

// 会话配置
define('SESSION_NAME', 'QEDGE_SESSION');
define('SESSION_LIFETIME', 86400 * 7); // 7天持久会话（原 24 小时太短）
define('SESSION_SAVE_PATH', __DIR__ . '/data/sessions'); // 本地私有目录，防系统 /tmp 定时清理

// 安全配置
define('CSRF_TOKEN_NAME', 'csrf_token');
define('PASSWORD_MIN_LENGTH', 6);
define('PASSWORD_MAX_LENGTH', 32);

// QQ头像API
define('QQ_AVATAR_URL', 'https://q1.qlogo.cn/g?b=qq&nk=');

// 系统信息
define('SYSTEM_NAME', 'QEdge');
define('SYSTEM_VERSION', '0.1.3');
const SYSTEM_UPDATE_LOG = [
    '修复 冷雨Java开关机',
    '新增 空间秒赞，秒评',
    '新增 视频转泡泡功能',
    '新增 闪照破解',
    '新增 半透明头像/名片/群头像/群名片等',
    '新增 表情长按下载'
];

// 创建数据库连接
function getDBConnection() {
    try {
        $dsn = "mysql:host=" . DataBase_HOST . ";dbname=" . DataBase_NAME . ";charset=" . DataBase_CHARSET;
        $options = [
            PDO::ATTR_ERRMODE            => PDO::ERRMODE_EXCEPTION,
            PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
            PDO::ATTR_EMULATE_PREPARES   => false,
            // PHP7.3 某些PDO/mysqlnd组合在部分环境下 DSN 内 charset 不生效，再强制SET NAMES兜底
            PDO::MYSQL_ATTR_INIT_COMMAND => "SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci",
        ];
        $pdo = new PDO($dsn, DataBase_USER, DataBase_PASS, $options);
        // 二次兜底：连接建立后再执行一次SET NAMES，防驱动层字符集协商失败
        $pdo->exec("SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci");
        $pdo->exec("SET character_set_client=utf8mb4, character_set_connection=utf8mb4, character_set_results=utf8mb4");
        return $pdo;
    } catch (PDOException $e) {
        error_log("数据库连接失败: " . $e->getMessage());
        return null;
    }
}

// ===== 启动会话（解决 Cookie 失效太快的核心修复） =====
// 1) 使用本地私有目录保存 session 文件，防系统 /tmp 被 tmpfiles.d/cron 定时清空
if (!is_dir(SESSION_SAVE_PATH)) {
    @mkdir(SESSION_SAVE_PATH, 0755, true);
    @file_put_contents(SESSION_SAVE_PATH . '/.htaccess', "Deny from all\n");
    @file_put_contents(SESSION_SAVE_PATH . '/index.html', '');
}
session_save_path(SESSION_SAVE_PATH);

// 2) 服务端 session 文件 GC 生命周期 = 客户端 Cookie 过期时间 = SESSION_LIFETIME（7 天）
ini_set('session.gc_maxlifetime', (string)SESSION_LIFETIME);
ini_set('session.gc_probability', 1);
ini_set('session.gc_divisor', 100);  // 1% 概率触发 GC，兼顾性能与清理

// 3) 关闭“会话 Cookie 仅在浏览器打开期间有效”的默认行为（0 = 关闭即失效）
ini_set('session.cookie_lifetime', (string)SESSION_LIFETIME);

// 4) 会话初始化：统一设置 cookie 全参数，避免跨目录/跨页面 cookie 丢失
if (session_status() == PHP_SESSION_NONE) {
    session_name(SESSION_NAME);
    if (PHP_VERSION_ID >= 70300) {
        // PHP 7.3+ 支持 session_set_cookie_params(array) 的 SameSite 参数
        $isHttps = (!empty($_SERVER['HTTPS']) && $_SERVER['HTTPS'] !== 'off')
                   || (isset($_SERVER['SERVER_PORT']) && $_SERVER['SERVER_PORT'] == 443);
        session_set_cookie_params([
            'lifetime' => SESSION_LIFETIME,
            'path'     => '/',              // 全站点路径，避免 admin/、user/、online_plugin/ 各自 cookie 不共享
            'domain'   => '',               // 空=自动匹配当前域名
            'secure'   => $isHttps,         // HTTPS 才发 secure
            'httponly' => true,             // 禁止 JS 读取（防 XSS 盗会话）
            'samesite' => 'Lax',            // 防止 CSRF，兼顾站内跳转携带 cookie
        ]);
    } else {
        session_set_cookie_params(SESSION_LIFETIME, '/', '', false, true);
    }
    session_start();
} else {
    // 已经启动的会话也强制重置 cookie lifetime（活跃用户不自动过期）
    if (ini_get('session.cookie_lifetime') != SESSION_LIFETIME) {
        $sessionName = session_name();
        $sessionId   = session_id();
        if ($sessionId) {
            setcookie($sessionName, $sessionId, time() + SESSION_LIFETIME, '/', '', false, true);
        }
    }
}
?>