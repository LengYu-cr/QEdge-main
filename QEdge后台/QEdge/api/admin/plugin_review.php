<?php
/**
 * 插件审核API接口
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

$stmt = $pdo->prepare("SELECT review_permission FROM users WHERE qq = ?");
$stmt->execute([$adminQQ]);
$user = $stmt->fetch();

if ($user && $user['review_permission'] != 1) {
    jsonResponse(403, '您没有脚本审核权限');
}

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    jsonResponse(405, '请求方法不正确');
}

$csrfToken = isset($_POST['csrf_token']) ? $_POST['csrf_token'] : '';
if (!verifyCSRFToken($csrfToken)) {
    jsonResponse(400, '安全验证失败');
}

$action = isset($_POST['action']) ? sanitizeInput($_POST['action']) : '';

if (!in_array($action, ['approve', 'reject', 'takedown'])) {
    jsonResponse(400, '操作类型不正确');
}

$couldId = isset($_POST['could_id']) ? intval($_POST['could_id']) : 0;
if ($couldId <= 0) {
    jsonResponse(400, '插件ID不正确');
}

try {
    $stmt = $pdo->prepare("SELECT * FROM plugins WHERE could_id = ?");
    $stmt->execute([$couldId]);
    $plugin = $stmt->fetch();

    if (!$plugin) {
        jsonResponse(404, '插件不存在');
    }

    $uploadsDir = __DIR__ . '/../../uploads/plugins/';

    if ($action === 'approve') {
        if ($plugin['status'] == 1) {
            jsonResponse(400, '该脚本已经通过审核');
        }

        $pdo->beginTransaction();

        $stmt = $pdo->prepare("UPDATE plugins SET status = 1, reject_reason = NULL WHERE could_id = ?");
        $stmt->execute([$couldId]);

        $targetDir = $uploadsDir . 'approved/';
        if (!is_dir($targetDir)) {
            mkdir($targetDir, 0755, true);
        }

        $currentFilePath = $plugin['file_path'];
        $filename = basename($currentFilePath);
        $newPath = $targetDir . $filename;

        if (!file_exists($currentFilePath)) {
            $pendingPath = $uploadsDir . 'pending/' . $filename;
            if (file_exists($pendingPath)) {
                $currentFilePath = $pendingPath;
            } else {
                $rejectedPath = $uploadsDir . 'rejected/' . $filename;
                if (file_exists($rejectedPath)) {
                    $currentFilePath = $rejectedPath;
                } else {
                    $takedownPath = $uploadsDir . 'takedown/' . $filename;
                    if (file_exists($takedownPath)) {
                        $currentFilePath = $takedownPath;
                    } else {
                        throw new Exception('文件不存在');
                    }
                }
            }
        }

        if (file_exists($currentFilePath)) {
            if (rename($currentFilePath, $newPath)) {
                $stmt = $pdo->prepare("UPDATE plugins SET file_path = ? WHERE could_id = ?");
                $stmt->execute([$newPath, $couldId]);
            } else {
                throw new Exception('文件移动失败');
            }
        } else {
            throw new Exception('源文件不存在');
        }

        $pdo->commit();
        logMessage("管理员 {$adminQQ} 审核通过插件 {$plugin['plugin_name']} (could_id: {$couldId})", 'INFO');
        jsonResponse(200, '审核通过');

    } elseif ($action === 'reject') {
        if ($plugin['status'] == 2) {
            jsonResponse(400, '该脚本已被拒绝');
        }

        $rejectReason = isset($_POST['reason']) ? sanitizeInput($_POST['reason']) : '';
        if (empty($rejectReason)) {
            jsonResponse(400, '请输入拒绝原因');
        }

        $pdo->beginTransaction();

        $stmt = $pdo->prepare("UPDATE plugins SET status = 2, reject_reason = ? WHERE could_id = ?");
        $stmt->execute([$rejectReason, $couldId]);

        $targetDir = $uploadsDir . 'rejected/';
        if (!is_dir($targetDir)) {
            mkdir($targetDir, 0755, true);
        }

        $currentFilePath = $plugin['file_path'];
        $filename = basename($currentFilePath);
        $newPath = $targetDir . $filename;

        if (file_exists($currentFilePath)) {
            if (rename($currentFilePath, $newPath)) {
                $stmt = $pdo->prepare("UPDATE plugins SET file_path = ? WHERE could_id = ?");
                $stmt->execute([$newPath, $couldId]);
            } else {
                throw new Exception('文件移动失败');
            }
        }

        $pdo->commit();
        logMessage("管理员 {$adminQQ} 审核拒绝插件 {$plugin['plugin_name']} (could_id: {$couldId}) 原因: {$rejectReason}", 'INFO');
        jsonResponse(200, '已拒绝审核');

    } elseif ($action === 'takedown') {
        if ($plugin['status'] != 1) {
            jsonResponse(400, '只有已通过的脚本才能下架');
        }

        $pdo->beginTransaction();

        $stmt = $pdo->prepare("UPDATE plugins SET status = 3 WHERE could_id = ?");
        $stmt->execute([$couldId]);

        $targetDir = $uploadsDir . 'takedown/';
        if (!is_dir($targetDir)) {
            mkdir($targetDir, 0755, true);
        }

        $currentFilePath = $plugin['file_path'];
        $filename = basename($currentFilePath);
        $newPath = $targetDir . $filename;

        if (file_exists($currentFilePath)) {
            if (rename($currentFilePath, $newPath)) {
                $stmt = $pdo->prepare("UPDATE plugins SET file_path = ? WHERE could_id = ?");
                $stmt->execute([$newPath, $couldId]);
            } else {
                throw new Exception('文件移动失败');
            }
        }

        $pdo->commit();
        logMessage("管理员 {$adminQQ} 下架脚本 {$plugin['plugin_name']} (could_id: {$couldId})", 'INFO');
        jsonResponse(200, '已下架');
    }

} catch (Exception $e) {
    if ($pdo && $pdo->inTransaction()) {
        $pdo->rollBack();
    }
    error_log("插件审核失败: " . $e->getMessage());
    jsonResponse(500, '操作失败: ' . $e->getMessage());
}
