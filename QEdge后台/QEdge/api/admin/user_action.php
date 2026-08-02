<?php
/**
 * 管理员用户操作API接口
 */

require_once __DIR__ . '/../../require.php';
require_once __DIR__ . '/../../function.php';

// 检查管理员是否登录
if (!isAdminLoggedIn()) {
    jsonResponse(401, '请先登录');
}

// 只接受POST请求
if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    jsonResponse(400, '不支持的请求方式');
}

try {
    $action = isset($_POST['action']) ? sanitizeInput($_POST['action']) : '';
    $qq = isset($_POST['qq']) ? sanitizeInput($_POST['qq']) : '';
    $csrfToken = isset($_POST['csrf_token']) ? $_POST['csrf_token'] : '';

    // 验证CSRF Token
    if (!verifyCSRFToken($csrfToken)) {
        jsonResponse(400, '安全验证失败');
    }

    $pdo = getDBConnection();
    if (!$pdo) {
        jsonResponse(500, '数据库连接失败');
    }

    switch ($action) {
        case 'ban':
            // 获取昵称
            $nickname = getQQNickname($qq);

            $stmt = $pdo->prepare("SELECT id FROM users WHERE qq = ?");
            $stmt->execute([$qq]);
            $userExists = $stmt->fetch();

            if ($userExists) {
                $stmt = $pdo->prepare("UPDATE users SET is_banned = 1, nickname = COALESCE(?, nickname) WHERE qq = ?");
                $stmt->execute([$nickname ?: null, $qq]);
            }

            $stmt = $pdo->prepare("INSERT IGNORE INTO banned_users (qq, nickname) VALUES (?, ?)");
            $stmt->execute([$qq, $nickname ?: null]);

            logMessage("管理员拉黑用户 {$qq}" . ($nickname ? " (昵称: {$nickname})" : ""), 'INFO');
            jsonResponse(200, '用户已加入黑名单' . ($nickname ? " (昵称: {$nickname})" : ""));
            break;

        case 'unban':
            $stmt = $pdo->prepare("UPDATE users SET is_banned = 0 WHERE qq = ?");
            $stmt->execute([$qq]);

            $stmt = $pdo->prepare("DELETE FROM banned_users WHERE qq = ?");
            $stmt->execute([$qq]);

            logMessage("管理员解除拉黑用户 {$qq}", 'INFO');
            jsonResponse(200, '用户已从黑名单移除');
            break;

        case 'delete':
            $stmt = $pdo->prepare("DELETE FROM users WHERE qq = ?");
            $stmt->execute([$qq]);
            logMessage("管理员删除用户 {$qq}", 'INFO');
            jsonResponse(200, '用户已删除');
            break;

        case 'grant_upload':
            $stmt = $pdo->prepare("UPDATE users SET upload_permission = 1 WHERE qq = ?");
            $stmt->execute([$qq]);
            logMessage("管理员授予用户 {$qq} 上传权限", 'INFO');
            jsonResponse(200, '已授权上传权限');
            break;

        case 'revoke_upload':
            $stmt = $pdo->prepare("UPDATE users SET upload_permission = 0 WHERE qq = ?");
            $stmt->execute([$qq]);
            logMessage("管理员撤销用户 {$qq} 上传权限", 'INFO');
            jsonResponse(200, '已撤销上传权限');
            break;

        case 'grant_review':
            $stmt = $pdo->prepare("UPDATE users SET review_permission = 1 WHERE qq = ?");
            $stmt->execute([$qq]);
            logMessage("管理员授予用户 {$qq} 审核权限", 'INFO');
            jsonResponse(200, '已授权审核权限');
            break;

        case 'revoke_review':
            $stmt = $pdo->prepare("UPDATE users SET review_permission = 0 WHERE qq = ?");
            $stmt->execute([$qq]);
            logMessage("管理员撤销用户 {$qq} 审核权限", 'INFO');
            jsonResponse(200, '已撤销审核权限');
            break;

        case 'set_sponsor':
            $stmt = $pdo->prepare("UPDATE users SET is_sponsor = 1 WHERE qq = ?");
            $stmt->execute([$qq]);
            // 同步写入 sponsor_users 表（如不存在则 INSERT，保证两张表一致，避免后续"取消赞助"只清一边）
            $stmtCheck = $pdo->prepare("SELECT qq FROM sponsor_users WHERE qq = ?");
            $stmtCheck->execute([$qq]);
            if (!$stmtCheck->fetch()) {
                // 拿昵称兜底
                $nickname = getQQNickname($qq);
                if (!$nickname) {
                    $stmtUser = $pdo->prepare("SELECT nickname FROM users WHERE qq = ?");
                    $stmtUser->execute([$qq]);
                    $nickname = $stmtUser->fetchColumn() ?: null;
                }
                $stmtIns = $pdo->prepare("INSERT INTO sponsor_users (qq, nickname, amount, note) VALUES (?, ?, 0, NULL)");
                $stmtIns->execute([$qq, $nickname]);
            }
            logMessage("管理员设置用户 {$qq} 为赞助用户", 'INFO');
            jsonResponse(200, '已设置为赞助用户');
            break;

        case 'unset_sponsor':
            $stmt = $pdo->prepare("UPDATE users SET is_sponsor = 0 WHERE qq = ?");
            $stmt->execute([$qq]);
            // 同步删除 sponsor_users 表记录，避免"取消赞助后赞助管理页面还能看到"
            $stmtDel = $pdo->prepare("DELETE FROM sponsor_users WHERE qq = ?");
            $stmtDel->execute([$qq]);
            logMessage("管理员取消用户 {$qq} 赞助状态", 'INFO');
            jsonResponse(200, '已取消赞助状态');
            break;

        default:
            jsonResponse(400, '无效操作');
    }

} catch (Exception $e) {
    error_log("管理员操作失败: " . $e->getMessage());
    jsonResponse(500, '操作失败');
}
?>