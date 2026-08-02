<?php
require_once __DIR__ . '/../require.php';
require_once __DIR__ . '/../function.php';

if (isUserLoggedIn()) {
    header('Location: index.php');
    exit;
}

$csrfToken = generateCSRFToken();
?>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>用户登录 - QEdge</title>
    <link rel="stylesheet" href="../assets/style.css">
</head>
<body class="auth-page">
    <div class="auth-box">
        <div class="auth-header">
            <div class="auth-logo">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="width:24px;height:24px;">
                    <path d="M12 2L2 7l10 5 10-5-10-5z"></path>
                    <path d="M2 17l10 5 10-5"></path>
                    <path d="M2 12l10 5 10-5"></path>
                </svg>
            </div>
            <h1 class="auth-title">用户登录</h1>
            <p class="auth-subtitle">欢迎使用 QEdge 系统</p>
        </div>

        <form id="loginForm">
            <input type="hidden" name="csrf_token" value="<?php echo $csrfToken; ?>">

            <div class="form-group">
                <label class="form-label" for="qq">QQ号</label>
                <input type="text" id="qq" name="qq" class="form-input" placeholder="请输入您的QQ号" required>
            </div>

            <div class="form-group">
                <label class="form-label" for="password">密码</label>
                <input type="password" id="password" name="password" class="form-input" placeholder="请输入密码" required>
            </div>

            <div class="form-group">
                <label class="form-label" for="captcha">验证码</label>
                <div class="captcha-group">
                    <input type="text" id="captcha" name="captcha" class="form-input" placeholder="请输入验证码" required>
                    <img src="../api/captcha.php" alt="验证码" id="captchaImg" class="captcha-img" onclick="this.src='../api/captcha.php?t=' + Math.random()">
                </div>
            </div>

            <div class="form-group">
                <button type="submit" class="btn btn-primary btn-block btn-lg" id="submitBtn">登录</button>
            </div>
        </form>

        <div class="auth-links">
            <a href="forgotpassword.php">忘记密码？</a>
            <a href="../index.php">返回首页</a>
        </div>

        <div class="auth-footer">
            <a href="../admin/login.php">管理员登录</a>
        </div>
    </div>

    <script src="../assets/common.js"></script>
    <script>
        document.getElementById('loginForm').addEventListener('submit', function(e) {
            e.preventDefault();
            const submitBtn = document.getElementById('submitBtn');
            submitBtn.disabled = true;
            submitBtn.textContent = '登录中...';
            const formData = new FormData(this);
            fetch('../api/user/login.php', { method: 'POST', body: formData })
            .then(response => response.json())
            .then(data => {
                if (data.code === 200) {
                    Toast.success(data.message);
                    setTimeout(() => window.location.href = 'index.php', 1000);
                } else {
                    Toast.error(data.message);
                    submitBtn.disabled = false;
                    submitBtn.textContent = '登录';
                    document.getElementById('captchaImg').src = '../api/captcha.php?t=' + Math.random();
                }
            })
            .catch(() => {
                Toast.error('网络错误，请稍后重试');
                submitBtn.disabled = false;
                submitBtn.textContent = '登录';
            });
        });
    </script>
</body>
</html>
