<?php
/**
 * 管理员反馈处理接口
 * 权限：必须已登录管理员 + review_permission=1（与脚本审核同等级权限）
 * 操作：
 *   - 改状态（0待处理→1处理中，或2已解决/3已关闭）
 *   - 填写回复（非必填，但建议 2已解决/3已关闭 时必须填写）
 */

require_once __DIR__ . '/../../require.php';
require_once __DIR__ . '/../../function.php';

header('Content-Type: application/json');

if (!isAdminLoggedIn()) {
    jsonResponse(401, '请先登录管理员账号');
}

$adminQQ = getCurrentAdminQQ();
$pdo = getDBConnection();

if (!$pdo) {
    jsonResponse(500, '数据库连接失败');
}

// 权限：只有拥有审核权限的管理员可以处理反馈（和脚本审核同等级）
$stmt = $pdo->prepare("SELECT review_permission, nickname FROM users WHERE qq = ?");
$stmt->execute([$adminQQ]);
$admin = $stmt->fetch();
if (!$admin || $admin['review_permission'] != 1) {
    jsonResponse(403, '您没有处理反馈的权限');
}

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    jsonResponse(405, '请求方法不正确');
}

// 兼容 json 与 form 两种提交
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

$couldId = isset($_POST['could_id']) ? intval($_POST['could_id']) : 0;
$status  = isset($_POST['status'])   ? intval($_POST['status'])   : -1;
$reply   = isset($_POST['reply'])    ? rtrim(sanitizeInput($_POST['reply'])) : '';

if ($couldId <= 0)                     jsonResponse(400, '反馈编号不正确');
if (!in_array($status, [0, 1, 2, 3])) jsonResponse(400, '状态参数不正确');

// 状态=已解决(2)/已关闭(3) 建议填写回复内容（非强制，警告即可），这里不强制阻拦

try {
    $stmt = $pdo->prepare("SELECT * FROM feedbacks WHERE could_id = ? FOR UPDATE");
    $stmt->execute([$couldId]);
    $fb = $stmt->fetch();
    if (!$fb) jsonResponse(404, '反馈不存在');

    $adminNick = $admin['nickname'] ?? $adminQQ;
    $replyTime = date('Y-m-d H:i:s');

    $updateSql = "UPDATE feedbacks SET status = ?, reply_qq = ?, reply_nickname = ?, reply_time = ?";
    $params = [$status, $adminQQ, $adminNick, $replyTime];

    // 如果管理员填了回复内容，就更新 reply 字段；如果没填，保留原有 reply（避免之前的回复被清空）
    if (trim($reply) !== '') {
        $updateSql .= ", reply = ?";
        $params[] = $reply;
    }
    $updateSql .= " WHERE could_id = ?";
    $params[] = $couldId;

    $pdo->beginTransaction();
    $stmt = $pdo->prepare($updateSql);
    $stmt->execute($params);
    $pdo->commit();

    // 读取更新后的值返回给前端
    $stmt = $pdo->prepare("SELECT * FROM feedbacks WHERE could_id = ?");
    $stmt->execute([$couldId]);
    $after = $stmt->fetch();

    $statusMap = [0 => '待处理', 1 => '处理中', 2 => '已解决', 3 => '已关闭'];
    $statusText = isset($statusMap[$status]) ? $statusMap[$status] : '未知';
    $logLine = "管理员 {$adminQQ} 处理反馈 #{$couldId} → 状态改为{$statusText}";
    if (trim($reply) !== '') $logLine .= "，回复内容长度:" . mb_strlen($reply, 'UTF-8') . "字";
    logMessage($logLine, 'INFO');

    jsonResponse(200, '处理成功', [
        'could_id'       => (int)$after['could_id'],
        'status'         => (int)$after['status'],
        'status_text'    => $statusText,
        'reply'          => $after['reply'],
        'reply_qq'       => $after['reply_qq'],
        'reply_nickname' => $after['reply_nickname'],
        'reply_time'     => $after['reply_time']
    ]);

} catch (Exception $e) {
    if ($pdo && $pdo->inTransaction()) $pdo->rollBack();
    error_log("反馈处理失败: " . $e->getMessage());
    jsonResponse(500, '操作失败: ' . $e->getMessage());
}
