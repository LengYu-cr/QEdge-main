<?php
/**
 * 插件元数据编辑接口
 * 管理员在后台编辑不合规范的脚本信息：名称、作者、版本号、脚本ID、介绍文案
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

// 权限：只有拥有审核权限的管理员才能编辑（与 approve/reject/takedown 同等级）
$stmt = $pdo->prepare("SELECT review_permission FROM users WHERE qq = ?");
$stmt->execute([$adminQQ]);
$user = $stmt->fetch();
if (!$user || $user['review_permission'] != 1) {
    jsonResponse(403, '您没有脚本审核/编辑权限');
}

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    jsonResponse(405, '请求方法不正确');
}

$csrfToken = isset($_POST['csrf_token']) ? $_POST['csrf_token'] : '';
if (!verifyCSRFToken($csrfToken)) {
    jsonResponse(400, '安全验证失败');
}

$couldId = isset($_POST['could_id']) ? intval($_POST['could_id']) : 0;
if ($couldId <= 0) {
    jsonResponse(400, '插件ID不正确');
}

// 读取并过 sanitize（sanitize 内部会 forceUtf8 + htmlspecialchars，保证 UTF-8 入库且防注入）
$pluginName  = isset($_POST['plugin_name'])  ? trim(sanitizeInput($_POST['plugin_name']))  : '';
$pluginId    = isset($_POST['plugin_id'])    ? trim(sanitizeInput($_POST['plugin_id']))    : '';
$versionCode = isset($_POST['version_code']) ? trim(sanitizeInput($_POST['version_code'])) : '';
$authorName  = isset($_POST['author_name'])  ? trim(sanitizeInput($_POST['author_name']))  : '';
$description = isset($_POST['description'])  ? rtrim(sanitizeInput($_POST['description'])) : '';

// 必填字段校验（与上传时从 info.prop 读取的校验一致）
if ($pluginName === '')  jsonResponse(400, '脚本名称不能为空');
if ($pluginId === '')    jsonResponse(400, '脚本ID不能为空');
if ($versionCode === '') jsonResponse(400, '版本号不能为空');
if ($authorName === '')  jsonResponse(400, '作者名称不能为空');
if ($description === '') $description = '该作者很懒，什么也没留下';

// 长度兜底（防止写入超长文本导致字段截断）
if (mb_strlen($pluginName,  'UTF-8') > 100) jsonResponse(400, '脚本名称不能超过100字');
if (mb_strlen($pluginId,    'UTF-8') > 80)  jsonResponse(400, '脚本ID不能超过80字');
if (mb_strlen($versionCode, 'UTF-8') > 50)  jsonResponse(400, '版本号不能超过50字');
if (mb_strlen($authorName,  'UTF-8') > 50)  jsonResponse(400, '作者名称不能超过50字');
if (mb_strlen($description, 'UTF-8') > 2000) jsonResponse(400, '介绍不能超过2000字');

try {
    $stmt = $pdo->prepare("SELECT could_id, plugin_name, plugin_id, version_code, author_name, description FROM plugins WHERE could_id = ?");
    $stmt->execute([$couldId]);
    $old = $stmt->fetch();
    if (!$old) jsonResponse(404, '插件不存在');

    $stmt = $pdo->prepare("UPDATE plugins
        SET plugin_name = ?, plugin_id = ?, version_code = ?, author_name = ?, description = ?
        WHERE could_id = ?");
    $stmt->execute([$pluginName, $pluginId, $versionCode, $authorName, $description, $couldId]);

    // 记录操作日志（包含变更前后的差异摘要，方便回溯管理员修改记录）
    $changes = [];
    if ($old['plugin_name']  !== $pluginName)  $changes[] = "name[{$old['plugin_name']}→{$pluginName}]";
    if ($old['plugin_id']    !== $pluginId)    $changes[] = "id[{$old['plugin_id']}→{$pluginId}]";
    if ($old['version_code'] !== $versionCode) $changes[] = "ver[{$old['version_code']}→{$versionCode}]";
    if ($old['author_name']  !== $authorName)  $changes[] = "author[{$old['author_name']}→{$authorName}]";
    $oldDesc = !empty($old['description']) ? $old['description'] : '';
    if ($oldDesc !== $description)            $changes[] = "desc[" . mb_strlen($oldDesc, 'UTF-8') . "B→" . mb_strlen($description, 'UTF-8') . "B]";
    $changeStr = $changes ? implode('; ', $changes) : '无字段变化';

    logMessage("管理员 {$adminQQ} 编辑插件 {$pluginName} (could_id: {$couldId}) —— {$changeStr}", 'INFO');

    jsonResponse(200, '修改成功', [
        'could_id'     => $couldId,
        'plugin_name'  => $pluginName,
        'plugin_id'    => $pluginId,
        'version_code' => $versionCode,
        'author_name'  => $authorName,
        'description'  => $description
    ]);

} catch (Exception $e) {
    error_log("插件编辑失败: " . $e->getMessage());
    jsonResponse(500, '操作失败: ' . $e->getMessage());
}
