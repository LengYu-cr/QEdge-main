<?php
/**
 * 心跳接收模块
 * 接收POST提交过来的hex数据，解析成json
 */

require_once __DIR__ . '/../require.php';
require_once __DIR__ . '/../function.php';

// GET请求不返回内容
if ($_SERVER['REQUEST_METHOD'] === 'GET') {
    exit;
}

// 只接受POST请求
if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    jsonResponse(400, '不支持的请求方式');
}

try {
    // 获取POST数据
    $hexData = file_get_contents('php://input');

    if (empty($hexData)) {
        jsonResponse(400, '未收到数据');
    }

    // 将hex数据解码成字符串
    $jsonData = hex2bin($hexData);

    if ($jsonData === false) {
        jsonResponse(400, '数据解码失败');
    }

    // 解析JSON
    $data = json_decode($jsonData, true);

    if ($data === null || !isset($data['qq'])) {
        jsonResponse(400, '数据格式错误');
    }

    // 提取用户信息
    $qq = sanitizeInput($data['qq']);
    $nickname = isset($data['nickname']) ? sanitizeInput($data['nickname']) : null;
    $qqVersion = isset($data['qq_version']) ? sanitizeInput($data['qq_version']) : null;
    $moduleVersion = isset($data['module_version']) ? sanitizeInput($data['module_version']) : null;

    // 验证QQ号
    if (!preg_match('/^[1-9][0-9]{4,10}$/', $qq)) {
        jsonResponse(400, 'QQ号格式错误');
    }

    // 连接数据库
    $pdo = getDBConnection();
    if (!$pdo) {
        jsonResponse(500, '数据库连接失败');
    }

    // 检查黑名单（包括未注册用户）
    $stmt = $pdo->prepare("SELECT qq FROM banned_users WHERE qq = ?");
    $stmt->execute([$qq]);
    $isBanned = $stmt->fetch();

    if ($isBanned) {
        jsonResponse(403, '您的账号已被拉黑');
    }

    // 检查用户是否存在
    $stmt = $pdo->prepare("SELECT * FROM users WHERE qq = ?");
    $stmt->execute([$qq]);
    $user = $stmt->fetch();

    $isNewUser = !$user;
    $initialPassword = '';

    if ($isNewUser) {
        // 检查是否是赞助用户
        $stmt = $pdo->prepare("SELECT qq FROM sponsor_users WHERE qq = ?");
        $stmt->execute([$qq]);
        $isSponsor = $stmt->fetch();

        // 首次登录（注册），生成随机密码
        $initialPassword = generateRandomPassword(12);
        $hashedPassword = encryptPassword($initialPassword);

        // 插入新用户（如果是赞助用户，自动设置赞助状态）
        $stmt = $pdo->prepare("INSERT INTO users (qq, nickname, password, qq_version, module_version, is_sponsor, last_login_time, last_login_ip) VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
        $stmt->execute([
            $qq,
            $nickname,
            $hashedPassword,
            $qqVersion,
            $moduleVersion,
            $isSponsor ? 1 : 0,
            date('Y-m-d H:i:s'),
            getClientIP()
        ]);

        logMessage("新用户注册: QQ={$qq}, 昵称={$nickname}, 赞助状态=" . ($isSponsor ? '是' : '否'), 'INFO');
    } else {
        // 检查是否被拉黑
        if ($user['is_banned'] == 1) {
            jsonResponse(403, '您的账号已被拉黑');
        }

        // 更新用户信息
        // 昵称策略：数据库已有昵称则不更新（防止模块提交错误的QQ号作为昵称）
        // 只有数据库昵称为空时才用心跳提交的昵称
        $finalNickname = !empty($user['nickname']) ? $user['nickname'] : $nickname;

        $stmt = $pdo->prepare("UPDATE users SET nickname = ?, qq_version = ?, module_version = ?, last_login_time = ?, last_login_ip = ? WHERE qq = ?");
        $stmt->execute([
            $finalNickname,
            $qqVersion,
            $moduleVersion,
            date('Y-m-d H:i:s'),
            getClientIP(),
            $qq
        ]);
    }

    // 返回响应
    if ($isNewUser) {
        // 首次登录返回初始密码
        jsonResponse(200, '注册成功', [
            'qq' => $qq,
            'nickname' => $nickname,
            'initial_password' => $initialPassword,
            'message' => '欢迎使用QEdge系统，这是您的初始密码，请妥善保管并尽快修改'
        ]);
    } else {
    
        $latestVersion = (int) str_replace(".", "",SYSTEM_VERSION);
        $currentVersion = (int) str_replace(".", "",$moduleVersion);
        
        if($latestVersion > $currentVersion){
            jsonResponse(0, '有新版本发布！', [
                "apk" => "https://cdn.yuafeng.cn/ly/QEdge_" . SYSTEM_VERSION . ".apk",
               "version" => SYSTEM_VERSION,
               "update" => file_get_contents("https://v.yuafeng.cn/QEdge/update/index.php")
            ]);
        }
        
        // 二次登录不返回密码
        jsonResponse(200, '登录成功', [
            'qq' => $qq,
            'nickname' => $finalNickname,
            'is_sponsor' => $user['is_sponsor'] == 1,
            'upload_permission' => $user['upload_permission'] == 1
        ]);
    }

} catch (Exception $e) {
    error_log("心跳处理失败: " . $e->getMessage());
    logMessage("心跳处理失败: " . $e->getMessage(), 'ERROR');
    jsonResponse(500, '处理失败，请稍后重试');
}
?>