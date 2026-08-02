<?php
require_once __DIR__ . '/../require.php';
require_once __DIR__ . '/../function.php';

if (isAdminLoggedIn()) {
    header('Location: dashboard.php');
    exit;
}

$csrfToken = generateCSRFToken();
?>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>管理员登录 - QEdge管理后台</title>
    <link rel="stylesheet" href="../assets/style.css">
</head>
<body class="auth-page">
    <div class="auth-card">
        <div class="auth-header">
            <div class="auth-logo">
                <span class="icon icon-lg">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"></path>
                    </svg>
                </span>
            </div>
            <h1 class="auth-title">QEdge 管理后台</h1>
            <p class="auth-subtitle">请登录您的管理员账号</p>
        </div>

        <div id="message" style="display: none;"></div>

        <form id="loginForm" class="auth-form">
            <input type="hidden" name="csrf_token" value="<?php echo $csrfToken; ?>">

            <div class="form-group">
                <label class="form-label" for="username">用户名</label>
                <input type="text" id="username" name="username" class="form-input" placeholder="请输入管理员用户名" required autofocus>
            </div>

            <div class="form-group">
                <label class="form-label" for="password">密码</label>
                <input type="password" id="password" name="password" class="form-input" placeholder="请输入密码" required>
            </div>

            <div class="form-group">
                <label class="form-label" for="captcha">验证码</label>
                <div class="captcha-group">
                    <input type="text" id="captcha" name="captcha" class="form-input" placeholder="请输入验证码" required>
                    <img src="../api/captcha.php" alt="验证码" class="captcha-img" id="captchaImg" onclick="this.src='../api/captcha.php?t=' + Math.random()" title="点击刷新">
                </div>
            </div>

            <button type="submit" class="btn btn-primary btn-block btn-lg" id="submitBtn">登 录</button>
        </form>

        <div class="auth-note">
            <strong>注意：</strong>这是管理员登录页面，仅限管理员使用。所有登录行为都会被记录。
        </div>

        <div class="auth-footer">
            <a href="../index.php">&larr; 返回首页</a>
        </div>
    </div>

    <script src="../assets/common.js"></script>
    <script>
        document.getElementById('loginForm').addEventListener('submit', function(e) {
            e.preventDefault();

            const submitBtn = document.getElementById('submitBtn');
            const messageDiv = document.getElementById('message');

            submitBtn.disabled = true;
            submitBtn.textContent = '登录中...';

            const formData = new FormData(this);

            fetch('../api/admin/login.php', {
                method: 'POST',
                body: formData
            })
            .then(response => response.json())
            .then(data => {
                if (data.code === 200) {
                    messageDiv.className = 'alert alert-success';
                    messageDiv.innerHTML = '<span class="icon"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path><polyline points="22 4 12 14.01 9 11.01"></polyline></svg></span><span>' + data.message + '</span>';
                    messageDiv.style.display = 'flex';

                    setTimeout(() => {
                        window.location.href = 'dashboard.php';
                    }, 1000);
                } else {
                    messageDiv.className = 'alert alert-danger';
                    messageDiv.innerHTML = '<span class="icon"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="8" x2="12" y2="12"></line><line x1="12" y1="16" x2="12.01" y2="16"></line></svg></span><span>' + data.message + '</span>';
                    messageDiv.style.display = 'flex';
                    submitBtn.disabled = false;
                    submitBtn.textContent = '登 录';

                    document.getElementById('captchaImg').src = '../api/captcha.php?t=' + Math.random();
                }
            })
            .catch(error => {
                messageDiv.className = 'alert alert-danger';
                messageDiv.innerHTML = '<span class="icon"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="8" x2="12" y2="12"></line><line x1="12" y1="16" x2="12.01" y2="16"></line></svg></span><span>网络错误，请稍后重试</span>';
                messageDiv.style.display = 'flex';
                submitBtn.disabled = false;
                submitBtn.textContent = '登 录';
            });
        });
    </script>
</body>
</html>
