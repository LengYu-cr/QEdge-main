<?php
/**
 * 用户反馈提交接口
 * 校验：已登录 + CSRF + 标题内容必填 + 长度限制
 * 写入：feedbacks 表（默认 status=0 待处理）
 */

require_once __DIR__ . '/../../require.php';
require_once __DIR__ . '/../../function.php';

header('Content-Type: application/json');

if (!isUserLoggedIn()) {
    jsonResponse(401, '请先登录');
}

$userQQ = getCurrentUserQQ();
$pdo = getDBConnection();

if (!$pdo) {
    jsonResponse(500, '数据库连接失败');
}

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    jsonResponse(405, '请求方法不正确');
}

// CSRF 校验（兼容 application/x-www-form-urlencoded 与 application/json 两种提交）
$rawBody = file_get_contents('php://input');
$isJson = isset($_SERVER['CONTENT_TYPE']) && stripos($_SERVER['CONTENT_TYPE'], 'application/json') !== false;
if ($isJson && $rawBody !== '') {
    $json = json_decode($rawBody, true);
    if (is_array($json)) $_POST = array_merge($_POST, $json);
}

$csrfToken = isset($_POST['csrf_token']) ? $_POST['csrf_token'] : '';
if (!verifyCSRFToken($csrfToken)) {
    jsonResponse(400, '安全验证失败');
}

$title   = isset($_POST['title'])   ? trim(sanitizeInput($_POST['title']))   : '';
$content = isset($_POST['content']) ? rtrim(sanitizeInput($_POST['content'])) : '';

if ($title === '')   jsonResponse(400, '请填写反馈标题');
if ($content === '') jsonResponse(400, '请填写反馈内容');

if (mb_strlen($title,   'UTF-8') > 200) jsonResponse(400, '标题不能超过 200 字');
if (mb_strlen($content, 'UTF-8') > 3000) jsonResponse(400, '内容不能超过 3000 字');

try {
    $userInfo = getUserInfo($pdo, $userQQ);
    $nickname = $userInfo ? ($userInfo['nickname'] ?? null) : null;

    $stmt = $pdo->prepare("INSERT INTO feedbacks (qq, user_nickname, title, content, status)
                           VALUES (?, ?, ?, ?, 0)");
    $stmt->execute([$userQQ, $nickname, $title, $content]);

    $couldId = $pdo->lastInsertId();

    logMessage("用户 {$userQQ} 提交反馈 (could_id: {$couldId}) 标题: {$title}", 'INFO');

    jsonResponse(200, '反馈提交成功，我们会尽快处理', [
        'could_id'    => (int)$couldId,
        'title'       => $title,
        'create_time' => date('Y-m-d H:i:s')
    ]);

} catch (Exception $e) {
    error_log("用户反馈提交失败: " . $e->getMessage());
    jsonResponse(500, '提交失败：' . $e->getMessage());
}
