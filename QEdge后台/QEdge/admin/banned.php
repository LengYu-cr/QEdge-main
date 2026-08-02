<?php
require_once __DIR__ . '/../require.php';
require_once __DIR__ . '/../function.php';

if (!isAdminLoggedIn()) {
    header('Location: login.php');
    exit;
}

$pdo = getDBConnection();
$dbError = '';
$users = [];
$total = 0;

if (!$pdo) {
    $dbError = '数据库连接失败，请检查数据库配置';
} else {
    $search = isset($_GET['search']) ? trim($_GET['search']) : '';

    $params = [];
    $conditions = [];

    if (!empty($search)) {
        $conditions['users'] = "(u.qq LIKE ? OR u.nickname LIKE ?)";
        $conditions['banned'] = "(bu.qq LIKE ? OR bu.nickname LIKE ?)";
        $params = ['%' . $search . '%', '%' . $search . '%', '%' . $search . '%', '%' . $search . '%'];
    }

    $usersSql = "SELECT u.qq, u.nickname, u.register_time, u.last_login_time, u.module_version, u.qq_version, 'registered' as user_type FROM users u WHERE u.is_banned = 1";
    if (!empty($conditions['users'])) {
        $usersSql .= " AND " . $conditions['users'];
    }

    $bannedSql = "SELECT bu.qq, bu.nickname, bu.created_at as register_time, NULL as last_login_time, NULL as module_version, NULL as qq_version, 'unregistered' as user_type FROM banned_users bu";
    if (!empty($conditions['banned'])) {
        $bannedSql .= " WHERE " . $conditions['banned'];
    }

    $sql = "SELECT * FROM (" . $usersSql . " UNION " . $bannedSql . ") AS t ORDER BY register_time DESC";
    $stmt = $pdo->prepare($sql);
    $stmt->execute($params);
    $users = $stmt->fetchAll();
    $total = count($users);
}

$csrfToken = generateCSRFToken();
$pageTitle = '黑名单';
$activeMenu = 'banned';
require_once __DIR__ . '/header.php';
?>

<?php if ($dbError): ?>
    <div class="alert alert-danger">
        <span class="icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="12" cy="12" r="10"></circle>
                <line x1="12" y1="8" x2="12" y2="12"></line>
                <line x1="12" y1="16" x2="12.01" y2="16"></line>
            </svg>
        </span>
        <span><?php echo htmlspecialchars($dbError); ?></span>
    </div>
<?php endif; ?>

<div class="page-header">
    <h1 class="page-title">黑名单管理</h1>
    <p class="page-subtitle">共 <?php echo $total; ?> 位黑名单用户</p>
</div>

<div class="search-bar">
    <form method="GET" action="">
        <input type="text" name="search" value="<?php echo htmlspecialchars($search ?? ''); ?>" class="form-input" placeholder="搜索QQ号或昵称...">
        <button type="submit" class="btn btn-primary">
            <span class="icon">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <circle cx="11" cy="11" r="8"></circle>
                    <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
                </svg>
            </span>
            搜索
        </button>
        <?php if (!empty($search)): ?>
            <a href="?" class="btn btn-secondary">清除</a>
        <?php endif; ?>
    </form>
</div>

<div class="card" style="margin-bottom: 20px;">
    <div class="card-header">
        <h2 class="card-title">添加黑名单</h2>
    </div>
    <div class="card-body">
        <div style="display: flex; gap: 12px; align-items: flex-end;">
            <div style="flex: 1;">
                <label class="form-label">用户QQ</label>
                <input type="text" id="banQQ" class="form-input" placeholder="请输入要拉黑的用户QQ" maxlength="20">
            </div>
            <button class="btn btn-danger" onclick="banUser()">加入黑名单</button>
        </div>
    </div>
</div>

<div class="card">
    <div class="card-header">
        <h2 class="card-title">黑名单列表</h2>
    </div>
    <div class="card-body" style="padding: 0;">
        <?php if ($total == 0): ?>
            <div class="empty-state">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" class="empty-state-icon">
                    <circle cx="12" cy="12" r="10"></circle>
                    <line x1="4.93" y1="4.93" x2="19.07" y2="19.07"></line>
                </svg>
                <h3>暂无黑名单用户</h3>
                <p>所有用户状态正常</p>
            </div>
        <?php else: ?>
            <div style="padding: 16px;">
                <div class="user-grid">
                    <?php foreach ($users as $user): ?>
                    <div class="user-card">
                        <div class="user-card-header">
                            <img src="<?php echo QQ_AVATAR_URL . $user['qq']; ?>&s=640" alt="头像" class="user-avatar">
                            <div class="user-info">
                                <h3><?php echo htmlspecialchars($user['nickname'] ?? $user['qq']); ?></h3>
                                <p>QQ: <?php echo $user['qq']; ?></p>
                            </div>
                        </div>

                        <div class="user-card-badges">
                            <span class="badge badge-danger">已拉黑</span>
                            <?php if ($user['user_type'] == 'unregistered'): ?>
                                <span class="badge badge-warning">未注册</span>
                            <?php endif; ?>
                        </div>

                        <div class="user-card-details">
                            <div><strong>注册时间：</strong><?php echo $user['register_time']; ?></div>
                            <div><strong>最后登录：</strong><?php echo $user['last_login_time'] ?? '暂无'; ?></div>
                            <div><strong>模块版本：</strong><?php echo $user['module_version'] ?? '未知'; ?></div>
                            <div><strong>QQ版本：</strong><?php echo $user['qq_version'] ?? '未知'; ?></div>
                        </div>

                        <div class="user-card-actions">
                            <button class="btn btn-success btn-sm" onclick="unbanUser('<?php echo $user['qq']; ?>')">解除拉黑</button>
                        </div>
                    </div>
                    <?php endforeach; ?>
                </div>
            </div>
        <?php endif; ?>
    </div>
</div>

<script>
    const csrfToken = '<?php echo $csrfToken; ?>';

    function banUser() {
        const qq = document.getElementById('banQQ').value.trim();

        if (!qq) {
            Toast.error('请输入用户QQ');
            return;
        }

        if (!/^\d+$/.test(qq)) {
            Toast.error('QQ号码格式不正确');
            return;
        }

        Loading.show();
        fetch('../api/admin/user_action.php', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded'
            },
            body: `qq=${qq}&action=ban&csrf_token=${csrfToken}`
        })
        .then(response => response.json())
        .then(data => {
            Loading.hide();
            if (data.code === 200) {
                Toast.success(data.message);
                document.getElementById('banQQ').value = '';
                setTimeout(() => location.reload(), 1000);
            } else {
                Toast.error(data.message);
            }
        })
        .catch(error => {
            Loading.hide();
            Toast.error('操作失败，请稍后重试');
        });
    }

    function unbanUser(qq) {
        Modal.confirm('确认解除此用户的拉黑状态？', function() {
            Loading.show();
            fetch('../api/admin/user_action.php', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded'
                },
                body: `qq=${qq}&action=unban&csrf_token=${csrfToken}`
            })
            .then(response => response.json())
            .then(data => {
                Loading.hide();
                if (data.code === 200) {
                    Toast.success(data.message);
                    setTimeout(() => location.reload(), 1000);
                } else {
                    Toast.error(data.message);
                }
            })
            .catch(error => {
                Loading.hide();
                Toast.error('操作失败，请稍后重试');
            });
        });
    }
</script>

<?php require_once __DIR__ . '/footer.php'; ?>
