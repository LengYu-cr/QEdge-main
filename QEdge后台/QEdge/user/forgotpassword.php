<?php
require_once __DIR__ . '/../require.php';
require_once __DIR__ . '/../function.php';

$csrfToken = generateCSRFToken();
?>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>忘记密码 - QEdge</title>
    <link rel="stylesheet" href="../assets/style.css">
</head>
<body class="auth-page">
    <div class="auth-box">
        <div class="auth-header">
            <div class="auth-logo">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="width:24px;height:24px;">
                    <rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect>
                    <path d="M7 11V7a5 5 0 0 1 10 0v4"></path>
                </svg>
            </div>
            <h1 class="auth-title">忘记密码</h1>
            <p class="auth-subtitle">通过邮箱重置您的密码</p>
        </div>

        <div id="step1">
            <form id="sendCodeForm">
                <input type="hidden" name="csrf_token" value="<?php echo $csrfToken; ?>">
                <input type="hidden" name="action" value="send_code">

                <div class="form-group">
                    <label class="form-label" for="qq">QQ号</label>
                    <input type="text" id="qq" name="qq" class="form-input" placeholder="请输入您的QQ号" required>
                </div>

                <div class="form-group">
                    <label class="form-label" for="captcha">图形验证码</label>
                    <div class="captcha-group">
                        <input type="text" id="captcha" name="captcha" class="form-input" placeholder="请输入验证码" required>
                        <img src="../api/captcha.php" alt="验证码" id="captchaImg" class="captcha-img" onclick="this.src='../api/captcha.php?t=' + Math.random()">
                    </div>
                </div>

                <div class="form-group">
                    <button type="submit" class="btn btn-primary btn-block btn-lg" id="sendCodeBtn">发送验证码</button>
                </div>
            </form>
        </div>

        <div id="step2" style="display: none;">
            <form id="resetPasswordForm">
                <input type="hidden" name="csrf_token" value="<?php echo $csrfToken; ?>">
                <input type="hidden" name="action" value="reset_password">
                <input type="hidden" id="qq_reset" name="qq" value="">

                <div class="form-group">
                    <label class="form-label">QQ号</label>
                    <div id="qq_display" class="info-item"></div>
                </div>

                <div class="form-group">
                    <label class="form-label">邮箱</label>
                    <div id="email_display" class="info-item"></div>
                </div>

                <div class="form-group">
                    <label class="form-label" for="email_code">邮箱验证码</label>
                    <input type="text" id="email_code" name="email_code" class="form-input" placeholder="请输入收到的验证码" required>
                    <div class="form-hint">验证码已发送至您的邮箱</div>
                </div>

                <div class="form-group">
                    <label class="form-label" for="new_password">新密码</label>
                    <input type="password" id="new_password" name="new_password" class="form-input" placeholder="请输入新密码（<?php echo PASSWORD_MIN_LENGTH; ?>-<?php echo PASSWORD_MAX_LENGTH; ?>位）" required>
                </div>

                <div class="form-group">
                    <label class="form-label" for="confirm_password">确认密码</label>
                    <input type="password" id="confirm_password" name="confirm_password" class="form-input" placeholder="请再次输入新密码" required>
                </div>

                <div class="form-group">
                    <button type="submit" class="btn btn-primary btn-block btn-lg" id="resetPasswordBtn">重置密码</button>
                </div>
            </form>
        </div>

        <div class="auth-links">
            <a href="login.php">返回登录</a>
            <a href="../index.php">返回首页</a>
        </div>
    </div>

    <script src="../assets/common.js"></script>
    <script>
        document.getElementById('sendCodeForm').addEventListener('submit', function(e) {
            e.preventDefault();
            const sendCodeBtn = document.getElementById('sendCodeBtn');
            sendCodeBtn.disabled = true;
            sendCodeBtn.textContent = '发送中...';
            const formData = new FormData(this);
            fetch('../api/user/forgot_password.php', { method: 'POST', body: formData })
            .then(response => response.json())
            .then(data => {
                if (data.code === 200) {
                    Toast.success(data.message);
                    document.getElementById('step1').style.display = 'none';
                    document.getElementById('step2').style.display = 'block';
                    const qq = document.getElementById('qq').value;
                    document.getElementById('qq_reset').value = qq;
                    document.getElementById('qq_display').textContent = qq;
                    if (data.data && data.data.email) {
                        document.getElementById('email_display').textContent = data.data.email;
                    } else {
                        document.getElementById('email_display').textContent = '邮箱信息已发送';
                    }
                } else {
                    Toast.error(data.message);
                    sendCodeBtn.disabled = false;
                    sendCodeBtn.textContent = '发送验证码';
                    document.getElementById('captchaImg').src = '../api/captcha.php?t=' + Math.random();
                }
            })
            .catch(() => {
                Toast.error('网络错误，请稍后重试');
                sendCodeBtn.disabled = false;
                sendCodeBtn.textContent = '发送验证码';
            });
        });

        document.getElementById('resetPasswordForm').addEventListener('submit', function(e) {
            e.preventDefault();
            const resetPasswordBtn = document.getElementById('resetPasswordBtn');
            resetPasswordBtn.disabled = true;
            resetPasswordBtn.textContent = '重置中...';
            const formData = new FormData(this);
            fetch('../api/user/forgot_password.php', { method: 'POST', body: formData })
            .then(response => response.json())
            .then(data => {
                if (data.code === 200) {
                    Toast.success(data.message);
                    setTimeout(() => window.location.href = 'login.php', 2000);
                } else {
                    Toast.error(data.message);
                    resetPasswordBtn.disabled = false;
                    resetPasswordBtn.textContent = '重置密码';
                }
            })
            .catch(() => {
                Toast.error('网络错误，请稍后重试');
                resetPasswordBtn.disabled = false;
                resetPasswordBtn.textContent = '重置密码';
            });
        });
    </script>
</body>
</html>
