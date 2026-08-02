<?php
require_once __DIR__ . '/../require.php';
require_once __DIR__ . '/../function.php';

$userQQ = getCurrentUserQQ();
$pdo = getDBConnection();
$userInfo = getUserInfo($pdo, $userQQ);

if (!$userInfo) {
    header('Location: logout.php');
    exit;
}

$csrfToken = generateCSRFToken();
$pageTitle = '修改密码';
?>

<?php require_once __DIR__ . '/header.php'; ?>

<div class="page-header">
    <h1 class="page-title">修改密码</h1>
    <p class="page-subtitle">更新您的账户密码</p>
</div>

<div class="card" style="max-width: 500px;">
    <div class="card-body">
        <form id="passwordForm">
            <input type="hidden" name="csrf_token" value="<?php echo $csrfToken; ?>">

            <div class="form-group">
                <label class="form-label" for="new_password">新密码</label>
                <input type="password" id="new_password" name="new_password" class="form-input" placeholder="请输入新密码（<?php echo PASSWORD_MIN_LENGTH; ?>-<?php echo PASSWORD_MAX_LENGTH; ?>位）" required>
            </div>

            <div class="form-group">
                <label class="form-label" for="confirm_password">确认密码</label>
                <input type="password" id="confirm_password" name="confirm_password" class="form-input" placeholder="请再次输入新密码" required>
            </div>

            <div class="form-group">
                <button type="submit" id="submitBtn" class="btn btn-primary btn-block">修改密码</button>
            </div>
        </form>

        <div class="alert alert-primary" style="margin-top: 16px; margin-bottom: 0;">
            <span class="icon">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <circle cx="12" cy="12" r="10"></circle>
                    <line x1="12" y1="16" x2="12" y2="12"></line>
                    <line x1="12" y1="8" x2="12.01" y2="8"></line>
                </svg>
            </span>
            <span>修改密码后，请使用新密码重新登录系统。</span>
        </div>
    </div>
</div>

<script>
    document.getElementById('passwordForm').addEventListener('submit', function(e) {
        e.preventDefault();
        const submitBtn = document.getElementById('submitBtn');
        submitBtn.disabled = true;
        submitBtn.textContent = '修改中...';
        const formData = new FormData(this);
        fetch('../api/user/change_password.php', { method: 'POST', body: formData })
        .then(response => response.json())
        .then(data => {
            if (data.code === 200) {
                Toast.success(data.message);
                setTimeout(() => window.location.href = 'logout.php', 2000);
            } else {
                Toast.error(data.message);
                submitBtn.disabled = false;
                submitBtn.textContent = '修改密码';
            }
        })
        .catch(() => {
            Toast.error('网络错误，请稍后重试');
            submitBtn.disabled = false;
            submitBtn.textContent = '修改密码';
        });
    });
</script>

<?php require_once __DIR__ . '/footer.php'; ?>
