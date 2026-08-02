<?php
/**
 * QEdge公用函数库
 * 适配PHP 7.3
 */

require_once 'require.php';

/**
 * 强制把任意编码的字符串转成标准 UTF-8（PHP7.3 兼容兜底）
 * 
 * 关键修复：增加"UTF-8 字节被 PHP 当成 ISO-8859-1 逐字节映射存为字符串"的检测与还原。
 * 典型现象：客户端明明发的是正确 UTF-8 中文，PHP 接收后 $_POST 里变成
 * "ç¤ºä¾èæ¬"（每个中文字节被当成 ISO-8859-1 的一个字符），这种字符串的每个
 * 字符 codepoint 都 < 256，但把这些 codepoint 按字节拼起来又是合法的 UTF-8
 * 多字节中文。检测到这种情况就做一次 ISO-8859-1 → 字节还原。
 * 
 * 自动识别 GBK/GB2312/BIG5/CP936/ISO-8859-1 等常见输入编码。
 * @param mixed $data
 * @return mixed
 */
function forceUtf8($data) {
    if (is_array($data)) {
        return array_map('forceUtf8', $data);
    }
    if ($data === null || is_bool($data) || is_numeric($data)) {
        return $data;
    }
    if (!is_string($data)) {
        return $data;
    }
    if ($data === '') {
        return '';
    }
    $hasMb = function_exists('mb_check_encoding');

    // 客户端直接发送 UTF-8 中文，PHP 7.3 + _charset_=UTF-8 声明后
    // $_POST 接收的就是正确的 UTF-8 字节串，因此先判断：合法 UTF-8 直接返回
    if ($hasMb) {
        if (@mb_check_encoding($data, 'UTF-8')) {
            return $data;
        }
    } else {
        if (preg_match('//u', $data)) {
            return $data;
        }
    }

    // 兜底：只有当字符串不是合法 UTF-8（例如 GBK 表单提交等历史情况），才尝试转码
    $candidates = ['GBK', 'GB2312', 'CP936', 'BIG5', 'ISO-8859-1'];
    $hasIconv = function_exists('iconv');
    $converted = '';
    if ($hasMb && function_exists('mb_convert_encoding')) {
        foreach ($candidates as $src) {
            $try = @mb_convert_encoding($data, 'UTF-8', $src);
            if (@mb_check_encoding($try, 'UTF-8')) {
                $converted = $try;
                break;
            }
        }
    }
    if ($converted === '' && $hasIconv) {
        foreach ($candidates as $src) {
            $try = @iconv($src, 'UTF-8//IGNORE', $data);
            if ($try !== false && $try !== '') {
                $converted = $try;
                break;
            }
        }
    }
    return ($converted !== '') ? $converted : $data;
}

/**
 * 批量把 $_POST / $_GET / $_REQUEST / $_FILES['name'] 全部转成 UTF-8
 * （客户端现在直接传中文UTF-8，不再做URLEncode，因此这里也不再rawurldecode，直接forceUtf8兜底）
 */
function decodeUtf8Input() {
    $decodedLists = [];
    foreach (['_POST', '_GET'] as $which) {
        if (!isset($GLOBALS[$which]) || !is_array($GLOBALS[$which])) continue;
        foreach ($GLOBALS[$which] as $k => &$v) {
            if (is_string($v)) {
                // 兜底强制转码 UTF-8（识别"UTF-8字节被当成ISO-8859-1读入"的典型乱码并还原）
                $v = forceUtf8($v);
            } else if (is_array($v)) {
                $v = forceUtf8($v);
            }
        }
        unset($v);
        $decodedLists[] = $which;
    }
    // 上传文件名也修正
    if (isset($_FILES) && is_array($_FILES)) {
        foreach ($_FILES as $f => $inf) {
            if (!is_array($inf)) continue;
            if (isset($inf['name']) && is_string($inf['name'])) {
                $_FILES[$f]['name'] = forceUtf8($inf['name']);
            }
        }
        $decodedLists[] = '_FILES[name]';
    }
    return $decodedLists;
}
// 函数库加载完就立即执行一次，保证后续代码取到的都是 UTF-8
decodeUtf8Input();

/**
 * 从数据库取出的字段也要统一转码（解决历史数据里已经存了乱码的情况）
 * 对查询结果的每一行每个字符串字段过一次 forceUtf8，保证 json_encode 输出给客户端的都是中文
 */
function dbFixUtf8($data) {
    if (!is_array($data)) {
        return is_string($data) ? forceUtf8($data) : $data;
    }
    $out = [];
    foreach ($data as $k => $v) {
        if (is_array($v)) {
            $out[$k] = dbFixUtf8($v);
        } elseif (is_string($v)) {
            $out[$k] = forceUtf8($v);
        } else {
            $out[$k] = $v;
        }
    }
    return $out;
}

/**
 * 输入数据转义
 * @param string $data 待转义数据
 * @return string 转义后数据
 */
function sanitizeInput($data) {
    // 强制 UTF-8 兜底（防止 decodeUtf8Input 之后还有其他入口传入非 UTF-8）
    $data = forceUtf8($data);
    $data = trim($data);
    $data = stripslashes($data);
    // 确认使用 UTF-8 模式做 htmlspecialchars
    $data = htmlspecialchars($data, ENT_QUOTES | ENT_SUBSTITUTE, 'UTF-8', false);
    return $data;
}

/**
 * 生成CSRF Token
 * @return string CSRF Token
 */
function generateCSRFToken() {
    if (!isset($_SESSION['csrf_token'])) {
        $_SESSION['csrf_token'] = bin2hex(random_bytes(32));
    }
    return $_SESSION['csrf_token'];
}

/**
 * 验证CSRF Token
 * @param string $token 待验证的token
 * @return bool 验证结果
 */
function verifyCSRFToken($token) {
    if (!isset($_SESSION['csrf_token']) || empty($token)) {
        return false;
    }
    return hash_equals($_SESSION['csrf_token'], $token);
}

/**
 * 生成随机密码
 * @param int $length 密码长度
 * @return string 随机密码
 */
function generateRandomPassword($length = 12) {
    $chars = 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!@#$%^&*';
    $password = '';
    for ($i = 0; $i < $length; $i++) {
        $password .= $chars[random_int(0, strlen($chars) - 1)];
    }
    return $password;
}

/**
 * 密码加密
 * @param string $password 原始密码
 * @return string 加密后的密码
 */
function encryptPassword($password) {
    return password_hash($password, PASSWORD_DEFAULT);
}

/**
 * 验证密码
 * @param string $password 原始密码
 * @param string $hash 加密后的密码
 * @return bool 验证结果
 */
function verifyPassword($password, $hash) {
    return password_verify($password, $hash);
}

/**
 * 发送验证码邮件
 * @param string $email 收件人邮箱
 * @param string $code 验证码
 * @return array ['success' => bool, 'message' => string]
 */
function sendVerificationCodeEmail($email, $code) {
    //require_once "../functions.php";
    $subject = 'QEdge系统 - 密码重置验证码';
    
    // 邮件内容
    $content = "QEdge系统密码重置验证码\n\n";
    $content .= "您的验证码是：{$code}\n";
    $content .= "验证码有效期为10分钟，请尽快使用。\n\n";
    $content .= "如果这不是您本人的操作，请忽略此邮件。\n";
    $content .= "此邮件由系统自动发送，请勿回复。\n";
    $content .= "© " . date('Y') . " QEdge System\n";
    $content .= "\n";
    $content .= "---\n";
    $content .= "此邮件由系统自动发送，请勿回复。";
    
    return sendEmail($email, $subject, $content);
}

// 发送邮件函数
function sendEmail($to, $subject, $content) {
    // 直接使用 socket 发送 SMTP 邮件（最稳定的方式）
    return sendEmailBySMTP(SMTP_HOST, SMTP_PORT, SMTP_USER, SMTP_PASS, SMTP_FROM, $to, $subject, $content);
}

// 使用原生 socket 发送邮件
function sendEmailBySMTP($host, $port, $user, $pass, $from, $to, $subject, $content) {
    $errno = 0;
    $errstr = '';
    
    // SSL连接（QQ邮箱465端口）
    if ($port == 465) {
        $context = stream_context_create([
            'ssl' => [
                'verify_peer' => false,
                'verify_peer_name' => false
            ]
        ]);
        $socket = @stream_socket_client(
            'ssl://' . $host . ':' . $port,
            $errno,
            $errstr,
            30,
            STREAM_CLIENT_CONNECT,
            $context
        );
    } else {
        $socket = @stream_socket_client(
            $host . ':' . $port,
            $errno,
            $errstr,
            30
        );
    }
    
    if (!$socket) {
        return ['success' => false, 'message' => "连接SMTP失败: $errstr ($errno)"];
    }
    
    stream_set_timeout($socket, 30);
    
    // 读取欢迎消息
    $response = fgets($socket, 1024);
    if (substr($response, 0, 3) != '220') {
        fclose($socket);
        return ['success' => false, 'message' => 'SMTP欢迎消息错误: ' . trim($response)];
    }
    
    // EHLO
    fwrite($socket, "EHLO localhost\r\n");
    while ($line = fgets($socket, 1024)) {
        if (substr($line, 3, 1) == ' ') break;
    }
    
    // AUTH LOGIN
    fwrite($socket, "AUTH LOGIN\r\n");
    $response = fgets($socket, 1024);
    if (substr($response, 0, 3) != '334') {
        fclose($socket);
        return ['success' => false, 'message' => 'AUTH失败: ' . trim($response)];
    }
    
    // 发送用户名
    fwrite($socket, base64_encode($user) . "\r\n");
    $response = fgets($socket, 1024);
    if (substr($response, 0, 3) != '334') {
        fclose($socket);
        return ['success' => false, 'message' => '用户名错误: ' . trim($response)];
    }
    
    // 发送密码
    fwrite($socket, base64_encode($pass) . "\r\n");
    $response = fgets($socket, 1024);
    if (substr($response, 0, 3) != '235') {
        fclose($socket);
        return ['success' => false, 'message' => '认证失败: ' . trim($response)];
    }
    
    // MAIL FROM
    fwrite($socket, "MAIL FROM:<" . $from . ">\r\n");
    $response = fgets($socket, 1024);
    
    // RCPT TO
    fwrite($socket, "RCPT TO:<" . $to . ">\r\n");
    $response = fgets($socket, 1024);
    
    // DATA
    fwrite($socket, "DATA\r\n");
    $response = fgets($socket, 1024);
    
    // 邮件内容
    $boundary = md5(uniqid(time()));
    $headers = "From: ". SMTP_FROM_NAME ." <" . $from . ">\r\n";
    $headers .= "To: " . $to . "\r\n";
    $headers .= "Subject: =?UTF-8?B?" . base64_encode($subject) . "?=\r\n";
    $headers .= "MIME-Version: 1.0\r\n";
    $headers .= "Content-Type: text/plain; charset=UTF-8\r\n";
    $headers .= "Content-Transfer-Encoding: base64\r\n";
    $headers .= "\r\n";
    
    $body = base64_encode($content);
    
    fwrite($socket, $headers . $body . "\r\n.\r\n");
    $response = fgets($socket, 1024);
    
    // QUIT
    fwrite($socket, "QUIT\r\n");
    fclose($socket);
    
    if (substr($response, 0, 3) == '250') {
        return ['success' => true, 'message' => '发送成功'];
    } else {
        return ['success' => false, 'message' => '发送失败: ' . trim($response)];
    }
}


/**
 * 生成图形验证码
 * @return array ['image' => 图片资源, 'code' => 验证码]
 */
function generateCaptcha() {
    $code = '';
    $chars = '0123456789';
    for ($i = 0; $i < 4; $i++) {
        $code .= $chars[random_int(0, strlen($chars) - 1)];
    }

    // 存储验证码到session
    $_SESSION['captcha_code'] = $code;
    $_SESSION['captcha_time'] = time();

    // 创建图片
    $width = 120;
    $height = 40;
    $image = imagecreatetruecolor($width, $height);

    // 设置颜色
    $bgColor = imagecolorallocate($image, 255, 255, 255);
    $textColor = imagecolorallocate($image, 100, 100, 100);
    $lineColor = imagecolorallocate($image, 200, 200, 200);

    // 填充背景
    imagefilledrectangle($image, 0, 0, $width, $height, $bgColor);

    // 添加干扰线
    for ($i = 0; $i < 5; $i++) {
        imageline($image, random_int(0, $width), random_int(0, $height), random_int(0, $width), random_int(0, $height), $lineColor);
    }

    // 添加干扰点
    for ($i = 0; $i < 50; $i++) {
        imagesetpixel($image, random_int(0, $width), random_int(0, $height), $lineColor);
    }

    // 添加文字
    $fontSize = 5;
    $x = 10;
    for ($i = 0; $i < strlen($code); $i++) {
        $textColorRandom = imagecolorallocate($image, random_int(50, 100), random_int(50, 100), random_int(50, 100));
        imagestring($image, $fontSize, $x + $i * 25, random_int(10, 20), $code[$i], $textColorRandom);
    }

    return ['image' => $image, 'code' => $code];
}

/**
 * 输出图形验证码
 */
function outputCaptcha() {
    $captcha = generateCaptcha();
    header('Content-type: image/png');
    imagepng($captcha['image']);
    imagedestroy($captcha['image']);
    exit;
}

/**
 * 验证图形验证码
 * @param string $code 用户输入的验证码
 * @return bool 验证结果
 */
function verifyCaptcha($code) {
    if (!isset($_SESSION['captcha_code']) || !isset($_SESSION['captcha_time'])) {
        return false;
    }

    // 验证码有效期5分钟
    if (time() - $_SESSION['captcha_time'] > 300) {
        unset($_SESSION['captcha_code']);
        unset($_SESSION['captcha_time']);
        return false;
    }

    $result = (strtoupper($code) === strtoupper($_SESSION['captcha_code']));

    // 验证后清除验证码
    unset($_SESSION['captcha_code']);
    unset($_SESSION['captcha_time']);

    return $result;
}

/**
 * 生成验证码
 * @param int $length 验证码长度
 * @return string 验证码
 */
function generateVerificationCode($length = 6) {
    $chars = '0123456789';
    $code = '';
    for ($i = 0; $i < $length; $i++) {
        $code .= $chars[random_int(0, strlen($chars) - 1)];
    }
    return $code;
}

/**
 * 返回JSON响应
 * @param int $code 状态码
 * @param string $message 消息
 * @param mixed $data 数据
 */
function jsonResponse($code, $message, $data = null) {
    header('Content-Type: application/json; charset=utf-8');
    $response = [
        'code' => $code,
        'message' => $message,
        'timestamp' => time()
    ];
    if ($data !== null) {
        $response['data'] = $data;
    }
    echo json_encode($response, JSON_UNESCAPED_UNICODE);
    exit;
}

/**
 * 刷新会话 Cookie 生命周期（滑动窗口机制：最后一次操作 + SESSION_LIFETIME）
 * 每次访问需要登录的接口/页面调用，保证活跃用户持续在线
 */
function refreshSessionLifetime() {
    if (session_status() !== PHP_SESSION_ACTIVE) return;
    $sessionId = session_id();
    if (!$sessionId || headers_sent()) return;

    $now = time();
    // 限流：每 10 分钟最多刷一次 Cookie，避免每个请求都发 Set-Cookie 头
    $lastRefresh = isset($_SESSION['_cookie_last_refresh']) ? (int)$_SESSION['_cookie_last_refresh'] : 0;
    if ($now - $lastRefresh < 600) return; // 小于 10 分钟不刷

    $isHttps = (!empty($_SERVER['HTTPS']) && $_SERVER['HTTPS'] !== 'off')
               || (isset($_SERVER['SERVER_PORT']) && $_SERVER['SERVER_PORT'] == 443);

    setcookie(
        SESSION_NAME,
        $sessionId,
        $now + SESSION_LIFETIME,
        '/',
        '',
        $isHttps,
        true   // httponly
    );
    $_SESSION['_cookie_last_refresh'] = $now;
}

/**
 * 检查用户是否登录（包含过期校验 + 自动续期）
 * @return bool 登录状态
 */
function isUserLoggedIn() {
    if (!isset($_SESSION['user_qq']) || empty($_SESSION['user_qq'])) return false;

    // 过期校验：最后活跃时间超过 SESSION_LIFETIME => 强制掉线
    $lastActive = isset($_SESSION['user_last_active']) ? (int)$_SESSION['user_last_active'] : 0;
    if ($lastActive > 0 && (time() - $lastActive) > SESSION_LIFETIME) {
        $_SESSION = [];
        @session_regenerate_id(true);
        return false;
    }

    // 滑动窗口：刷新最后活跃时间 + Cookie 过期时间
    $_SESSION['user_last_active'] = time();
    refreshSessionLifetime();
    return true;
}

/**
 * 检查管理员是否登录（包含过期校验 + 自动续期）
 * @return bool 登录状态
 */
function isAdminLoggedIn() {
    if (!isset($_SESSION['admin_logged_in']) || $_SESSION['admin_logged_in'] !== true) return false;

    // 过期校验：最后活跃时间超过 SESSION_LIFETIME => 强制掉线
    $lastActive = isset($_SESSION['admin_last_active']) ? (int)$_SESSION['admin_last_active'] : 0;
    if ($lastActive > 0 && (time() - $lastActive) > SESSION_LIFETIME) {
        $_SESSION = [];
        @session_regenerate_id(true);
        return false;
    }

    // 滑动窗口：刷新最后活跃时间 + Cookie 过期时间
    $_SESSION['admin_last_active'] = time();
    refreshSessionLifetime();
    return true;
}

/**
 * 获取当前登录管理员QQ
 * @return string 管理员QQ
 */
function getCurrentAdminQQ() {
    return ADMIN_QQ;
}

/**
 * 获取当前登录用户QQ
 * @return string|null 用户QQ
 */
function getCurrentUserQQ() {
    return isset($_SESSION['user_qq']) ? $_SESSION['user_qq'] : null;
}

/**
 * 获取用户信息
 * @param PDO $pdo 数据库连接
 * @param string $qq 用户QQ
 * @return array|null 用户信息
 */
function getUserInfo($pdo, $qq) {
    $stmt = $pdo->prepare("SELECT * FROM users WHERE qq = ?");
    $stmt->execute([$qq]);
    return $stmt->fetch();
}

/**
 * 检查用户是否被拉黑
 * @param PDO $pdo 数据库连接
 * @param string $qq 用户QQ
 * @return bool 是否被拉黑
 */
function isUserBanned($pdo, $qq) {
    $stmt = $pdo->prepare("SELECT is_banned FROM users WHERE qq = ?");
    $stmt->execute([$qq]);
    $result = $stmt->fetch();
    return $result && $result['is_banned'] == 1;
}

/**
 * 更新用户最后登录信息
 * @param PDO $pdo 数据库连接
 * @param string $qq 用户QQ
 */
function updateUserLastLogin($pdo, $qq) {
    $ip = $_SERVER['REMOTE_ADDR'];
    $time = date('Y-m-d H:i:s');
    $stmt = $pdo->prepare("UPDATE users SET last_login_time = ?, last_login_ip = ? WHERE qq = ?");
    $stmt->execute([$time, $ip, $qq]);
}

/**
 * 获取客户端IP
 * @return string IP地址
 */
function getClientIP() {
    if (!empty($_SERVER['HTTP_CLIENT_IP'])) {
        $ip = $_SERVER['HTTP_CLIENT_IP'];
    } elseif (!empty($_SERVER['HTTP_X_FORWARDED_FOR'])) {
        $ip = $_SERVER['HTTP_X_FORWARDED_FOR'];
    } else {
        $ip = $_SERVER['REMOTE_ADDR'];
    }
    return filter_var($ip, FILTER_VALIDATE_IP) ? $ip : '0.0.0.0';
}

/**
 * 生成唯一ID
 * @return string 唯一ID
 */
function generateUniqueID() {
    return uniqid(bin2hex(random_bytes(8)), true);
}

/**
 * 格式化时间
 * @param string $datetime 时间字符串
 * @return string 格式化后的时间
 */
function formatTime($datetime) {
    $timestamp = strtotime($datetime);
    $now = time();
    $diff = $now - $timestamp;

    if ($diff < 60) {
        return '刚刚';
    } elseif ($diff < 3600) {
        return floor($diff / 60) . '分钟前';
    } elseif ($diff < 86400) {
        return floor($diff / 3600) . '小时前';
    } elseif ($diff < 604800) {
        return floor($diff / 86400) . '天前';
    } else {
        return date('Y-m-d', $timestamp);
    }
}

/**
 * 分页函数
 * @param int $total 总记录数
 * @param int $page 当前页
 * @param int $perPage 每页记录数
 * @return array 分页信息
 */
function paginate($total, $page = 1, $perPage = 20) {
    $totalPages = ceil($total / $perPage);
    $page = max(1, min($page, $totalPages));
    $offset = ($page - 1) * $perPage;

    return [
        'total' => $total,
        'per_page' => $perPage,
        'current_page' => $page,
        'total_pages' => $totalPages,
        'offset' => $offset,
        'has_more' => $page < $totalPages
    ];
}

/**
 * 安全的文件上传
 * @param array $file $_FILES数组元素
 * @param string $destination 目标目录
 * @param array $allowedTypes 允许的文件类型
 * @return array 上传结果
 */
function uploadFile($file, $destination, $allowedTypes = ['zip']) {
    if (!isset($file['error']) || is_array($file['error'])) {
        return ['success' => false, 'message' => '无效的文件参数'];
    }

    switch ($file['error']) {
        case UPLOAD_ERR_OK:
            break;
        case UPLOAD_ERR_NO_FILE:
            return ['success' => false, 'message' => '没有文件被上传'];
        case UPLOAD_ERR_INI_SIZE:
        case UPLOAD_ERR_FORM_SIZE:
            return ['success' => false, 'message' => '文件大小超出限制'];
        default:
            return ['success' => false, 'message' => '未知错误'];
    }

    if ($file['size'] > 10485760) { // 10MB
        return ['success' => false, 'message' => '文件大小不能超过10MB'];
    }

    $ext = strtolower(pathinfo($file['name'], PATHINFO_EXTENSION));
    if (!in_array($ext, $allowedTypes)) {
        return ['success' => false, 'message' => '文件类型不允许'];
    }

    $filename = generateUniqueID() . '.' . $ext;
    $filepath = rtrim($destination, '/') . '/' . $filename;

    if (!move_uploaded_file($file['tmp_name'], $filepath)) {
        return ['success' => false, 'message' => '文件保存失败'];
    }

    return [
        'success' => true,
        'message' => '文件上传成功',
        'filename' => $filename,
        'filepath' => $filepath
    ];
}

/**
 * 记录日志
 * @param string $message 日志消息
 * @param string $level 日志级别
 */
function logMessage($message, $level = 'INFO') {
    $logFile = __DIR__ . '/logs/' . date('Y-m-d') . '.log';
    $logDir = dirname($logFile);
    if (!is_dir($logDir)) {
        mkdir($logDir, 0755, true);
    }
    $logMessage = '[' . date('Y-m-d H:i:s') . '] [' . $level . '] ' . $message . PHP_EOL;
    file_put_contents($logFile, $logMessage, FILE_APPEND);
}

/**
 * 通过QQ号获取昵称
 * @param string $qq QQ号
 * @return string|null 昵称，获取失败返回null
 */
function getQQNickname($qq) {
    if (empty($qq) || !preg_match('/^[1-9][0-9]{4,10}$/', $qq)) {
        return null;
    }

    $url = 'https://users.qzone.qq.com/fcg-bin/cgi_get_portrait.fcg?uins=' . $qq;
    $response = '';

    // 优先使用curl
    if (function_exists('curl_init')) {
        $ch = curl_init();
        curl_setopt_array($ch, [
            CURLOPT_URL => $url,
            CURLOPT_RETURNTRANSFER => true,
            CURLOPT_SSL_VERIFYPEER => false,
            CURLOPT_SSL_VERIFYHOST => false,
            CURLOPT_ENCODING => '',
            CURLOPT_MAXREDIRS => 10,
            CURLOPT_TIMEOUT => 10,
            CURLOPT_HTTP_VERSION => CURL_HTTP_VERSION_1_1,
            CURLOPT_CUSTOMREQUEST => 'GET',
            CURLOPT_HTTPHEADER => [
                'User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36'
            ],
        ]);
        $response = curl_exec($ch);
        curl_close($ch);
    } elseif (ini_get('allow_url_fopen')) {
        // 备用：使用file_get_contents
        $context = stream_context_create([
            'http' => [
                'timeout' => 10,
                'header' => "User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36\r\n"
            ]
        ]);
        $response = @file_get_contents($url, false, $context);
    }

    if (empty($response) || strlen($response) <= 17) {
        return null;
    }

    $encode = mb_detect_encoding($response, ['ASCII', 'UTF-8', 'GB2312', 'GBK', 'BIG5']);
    $response = mb_convert_encoding($response, 'UTF-8', $encode);

    $jsonp = substr($response, 17, -1);
    if (empty($jsonp)) {
        return null;
    }

    $data = json_decode($jsonp, true);
    if (!isset($data[$qq]) || !isset($data[$qq][6])) {
        return null;
    }

    $nickname = trim($data[$qq][6]);
    if (empty($nickname) || $nickname == '.' || $nickname == '暂无') {
        return null;
    }

    return $nickname;
}
?>