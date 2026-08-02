<?php
require_once __DIR__ . '/../require.php';
require_once __DIR__ . '/../function.php';

$userQQ = getCurrentUserQQ();
$pdo = getDBConnection();
$dbError = '';

if (!$pdo) {
    $dbError = '数据库连接失败，请检查数据库配置';
    $userInfo = null;
} else {
    $userInfo = getUserInfo($pdo, $userQQ);
}

if (!$userInfo) {
    if (!$dbError) {
        header('Location: logout.php');
        exit;
    }
}

$pageTitle = '用户首页';
?>

<?php require_once __DIR__ . '/header.php'; ?>

<?php if ($dbError): ?>
    <div class="alert alert-danger"><?php echo htmlspecialchars($dbError); ?></div>
<?php endif; ?>

<div class="page-header">
    <h1 class="page-title">个人中心</h1>
    <p class="page-subtitle">管理您的账户信息</p>
</div>

<div class="card" style="margin-bottom: 16px;">
    <div class="card-body">
        <div style="display: flex; align-items: center; gap: 16px;">
            <img src="<?php echo QQ_AVATAR_URL . $userQQ; ?>&s=640" alt="头像" class="user-avatar" style="width: 64px; height: 64px;">
            <div style="flex: 1;">
                <h2 style="font-size: 18px; font-weight: 600; color: var(--color-text-primary); margin-bottom: 4px;"><?php echo htmlspecialchars($userInfo['nickname'] ?? $userQQ); ?></h2>
                <p style="font-size: 14px; color: var(--color-text-secondary);">QQ: <?php echo $userQQ; ?></p>
            </div>
        </div>
        <div style="display: flex; gap: 8px; margin-top: 16px; flex-wrap: wrap;">
            <?php if ($userInfo['upload_permission'] == 1): ?>
                <span class="badge badge-primary">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="width:12px;height:12px;">
                        <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
                        <polyline points="17 8 12 3 7 8"></polyline>
                        <line x1="12" y1="3" x2="12" y2="15"></line>
                    </svg>
                    上传权限
                </span>
            <?php endif; ?>
            <?php if ($userInfo['review_permission'] == 1): ?>
                <span class="badge badge-purple">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="width:12px;height:12px;">
                        <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
                        <polyline points="22 4 12 14.01 9 11.01"></polyline>
                    </svg>
                    审核权限
                </span>
            <?php endif; ?>
        </div>
    </div>
</div>

<div class="card" style="margin-bottom: 16px;">
    <div class="card-header">
        <h3 class="card-title">账户信息</h3>
    </div>
    <div class="card-body">
        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 12px 24px; font-size: 14px;">
            <div>
                <span style="color: var(--color-text-tertiary);">注册时间：</span>
                <span style="color: var(--color-text-primary);"><?php echo htmlspecialchars($userInfo['register_time']); ?></span>
            </div>
            <div>
                <span style="color: var(--color-text-tertiary);">最后登录：</span>
                <span style="color: var(--color-text-primary);"><?php echo htmlspecialchars($userInfo['last_login_time'] ?? '暂无'); ?></span>
            </div>
            <div>
                <span style="color: var(--color-text-tertiary);">模块版本：</span>
                <span style="color: var(--color-text-primary);"><?php echo htmlspecialchars($userInfo['module_version'] ?? '未知'); ?></span>
            </div>
            <div>
                <span style="color: var(--color-text-tertiary);">QQ版本：</span>
                <span style="color: var(--color-text-primary);"><?php echo htmlspecialchars($userInfo['qq_version'] ?? '未知'); ?></span>
            </div>
        </div>
        <div style="margin-top: 16px; padding-top: 16px; border-top: 1px solid var(--color-border-light);">
            <div style="font-size: 14px; color: var(--color-text-tertiary); margin-bottom: 8px;">个性签名</div>
            <div style="font-size: 14px; color: var(--color-text-primary); line-height: 1.6;">
                <?php echo htmlspecialchars($userInfo['signature'] ?? '暂无签名'); ?>
            </div>
        </div>
    </div>
</div>

<?php if ($userInfo['is_sponsor'] == 1): ?>
<div class="card" style="margin-bottom: 16px;">
    <div class="card-header">
        <h3 class="card-title">修改个性签名</h3>
    </div>
    <div class="card-body">
        <form id="signatureForm">
            <div class="form-group">
                <textarea id="signature" name="signature" class="form-textarea" placeholder="请输入您的个性签名（最多500字）" maxlength="500"><?php echo htmlspecialchars($userInfo['signature'] ?? ''); ?></textarea>
            </div>
            <button type="submit" class="btn btn-primary" id="saveSignatureBtn">保存签名</button>
        </form>
    </div>
</div>
<?php endif; ?>

<div class="card" style="margin-bottom: 16px;">
    <div class="card-header">
        <h3 class="card-title">修改昵称</h3>
    </div>
    <div class="card-body">
        <form id="nicknameForm">
            <div class="form-group">
                <label class="form-label">新昵称</label>
                <input type="text" id="nickname" name="nickname" class="form-input" placeholder="请输入新昵称（最多20个字符）" maxlength="20" value="<?php echo htmlspecialchars($userInfo['nickname'] ?? ''); ?>">
            </div>
            <button type="submit" class="btn btn-primary" id="saveNicknameBtn">保存昵称</button>
        </form>
    </div>
</div>

<div class="card">
    <div class="card-header">
        <h3 class="card-title">快捷操作</h3>
    </div>
    <div class="card-body">
        <div style="display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); gap: 12px;">
            <a href="../online_plugin/list.php" class="btn btn-secondary" style="justify-content: flex-start; text-decoration: none;">
                <span class="icon">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
                        <polyline points="7 10 12 15 17 10"></polyline>
                        <line x1="12" y1="15" x2="12" y2="3"></line>
                    </svg>
                </span>
                在线脚本
            </a>
            <?php if ($userInfo['review_permission'] == 1): ?>
            <a href="plugin_review.php" class="btn btn-secondary" style="justify-content: flex-start; text-decoration: none;">
                <span class="icon">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V9z"></path>
                        <polyline points="14 2 14 8 20 8"></polyline>
                    </svg>
                </span>
                脚本审核
            </a>
            <?php endif; ?>
            <a href="motify_password.php" class="btn btn-secondary" style="justify-content: flex-start; text-decoration: none;">
                <span class="icon">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect>
                        <path d="M7 11V7a5 5 0 0 1 10 0v4"></path>
                    </svg>
                </span>
                修改密码
            </a>
            <a href="feedback.php" class="btn btn-secondary" style="justify-content: flex-start; text-decoration: none;">
                <span class="icon">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path>
                    </svg>
                </span>
                反馈中心
            </a>
        </div>
    </div>
</div>

<script>
<?php if ($userInfo['is_sponsor'] == 1): ?>
document.getElementById('signatureForm').addEventListener('submit', function(e) {
    e.preventDefault();
    const signature = document.getElementById('signature').value;
    const btn = document.getElementById('saveSignatureBtn');
    btn.disabled = true;
    btn.textContent = '保存中...';
    fetch('../api/user/update_signature.php', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ signature: signature, csrf_token: '<?php echo generateCSRFToken(); ?>' })
    })
    .then(response => response.json())
    .then(data => {
        if (data.code === 200) {
            Toast.success('签名修改成功！');
            setTimeout(() => location.reload(), 1000);
        } else {
            Toast.error(data.message);
        }
    })
    .catch(() => {
        Toast.error('网络错误，请稍后重试');
    })
    .finally(() => {
        btn.disabled = false;
        btn.textContent = '保存签名';
    });
});
<?php endif; ?>

document.getElementById('nicknameForm').addEventListener('submit', function(e) {
    e.preventDefault();
    const nickname = document.getElementById('nickname').value.trim();
    const btn = document.getElementById('saveNicknameBtn');
    
    if (!nickname) {
        Toast.error('昵称不能为空');
        return;
    }
    
    btn.disabled = true;
    btn.textContent = '保存中...';
    
    fetch('../api/user/update_nickname.php', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ nickname: nickname, csrf_token: '<?php echo generateCSRFToken(); ?>' })
    })
    .then(response => response.json())
    .then(data => {
        if (data.code === 200) {
            Toast.success('昵称修改成功！');
            setTimeout(() => location.reload(), 1000);
        } else {
            Toast.error(data.message);
        }
    })
    .catch(() => {
        Toast.error('网络错误，请稍后重试');
    })
    .finally(() => {
        btn.disabled = false;
        btn.textContent = '保存昵称';
    });
});
</script>

<?php require_once __DIR__ . '/footer.php'; ?>
