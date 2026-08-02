<?php
require_once __DIR__ . '/../require.php';
require_once __DIR__ . '/../function.php';

if (!isAdminLoggedIn()) {
    header('Location: login.php');
    exit;
}

$pdo = getDBConnection();
$dbError = '';
$sponsorUsers = [];

if (!$pdo) {
    $dbError = '数据库连接失败，请检查数据库配置';
} else {
    $search = isset($_GET['search']) ? trim($_GET['search']) : '';

    $params = [];
    $conditions = [];

    if (!empty($search)) {
        $conditions['users'] = "(u.qq LIKE ? OR u.nickname LIKE ?)";
        $conditions['sponsors'] = "(su.qq LIKE ? OR su.nickname LIKE ?)";
        $params = ['%' . $search . '%', '%' . $search . '%', '%' . $search . '%', '%' . $search . '%'];
    }

    // users 表：只取 is_sponsor=1 但不在 sponsor_users 表里的老数据（兼容早期历史数据），
    // 已经在 sponsor_users 里的一律以 sponsor_users 为准（有金额/备注字段，展示信息更完整）
    $usersSql = "SELECT u.qq, u.nickname, u.register_time, u.last_login_time, u.upload_permission, 0 as amount, NULL as note, 'registered' as user_type FROM users u WHERE u.is_sponsor = 1 AND u.qq NOT IN (SELECT qq FROM sponsor_users WHERE qq IS NOT NULL)";
    if (!empty($conditions['users'])) {
        $usersSql .= " AND " . $conditions['users'];
    }

    // sponsor_users 表：所有赞助用户（含未注册）。通过子查询关联 users 表拿 last_login_time / upload_permission，
    // 并根据是否存在 users 记录自动判断 user_type = registered / unregistered，展示信息比之前更全
    $sponsorsSql = "SELECT su.qq, su.nickname, su.created_at as register_time,
                            (SELECT u.last_login_time FROM users u WHERE u.qq = su.qq LIMIT 1) as last_login_time,
                            (SELECT u.upload_permission FROM users u WHERE u.qq = su.qq LIMIT 1) as upload_permission,
                            su.amount, su.note,
                            CASE WHEN EXISTS (SELECT 1 FROM users u WHERE u.qq = su.qq) THEN 'registered' ELSE 'unregistered' END as user_type
                     FROM sponsor_users su";
    if (!empty($conditions['sponsors'])) {
        $sponsorsSql .= " WHERE " . $conditions['sponsors'];
    }

    $sql = "SELECT * FROM (" . $usersSql . " UNION " . $sponsorsSql . ") AS t ORDER BY register_time DESC";
    $stmt = $pdo->prepare($sql);
    $stmt->execute($params);
    $sponsorUsers = $stmt->fetchAll();

    $totalAmount = $pdo->query("SELECT COALESCE(SUM(amount), 0) FROM sponsor_users")->fetchColumn();
}

$csrfToken = generateCSRFToken();
$pageTitle = '赞助管理';
$activeMenu = 'sponsor';
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
    <h1 class="page-title">赞助用户管理</h1>
    <p class="page-subtitle">共 <?php echo count($sponsorUsers); ?> 位，总金额：¥<?php echo number_format($totalAmount ?? 0, 2); ?></p>
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
        <h2 class="card-title">添加赞助用户</h2>
    </div>
    <div class="card-body">
        <div class="form-row">
            <div class="form-group" style="margin-bottom: 0;">
                <input type="text" id="qq" class="form-input" placeholder="请输入QQ号" required>
            </div>
            <div class="form-group" style="margin-bottom: 0;">
                <input type="number" id="amount" class="form-input" placeholder="赞助金额（元）" min="0" step="0.01" value="0">
            </div>
            <div class="form-group" style="margin-bottom: 0;">
                <input type="text" id="note" class="form-input" placeholder="备注（可选）" maxlength="255">
            </div>
        </div>
        <div style="margin-top: 12px; display: flex; justify-content: space-between; align-items: center;">
            <div style="font-size: 13px; color: var(--color-text-secondary);">
                输入QQ号、赞助金额和备注（可选）后点击添加，未注册用户也可以添加。
            </div>
            <button class="btn btn-primary" onclick="addSponsor()">添加赞助</button>
        </div>
    </div>
</div>

<div class="card">
    <div class="card-header">
        <h2 class="card-title">赞助用户列表</h2>
    </div>
    <div class="card-body" style="padding: 0;">
        <?php if (empty($sponsorUsers)): ?>
            <div class="empty-state">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" class="empty-state-icon">
                    <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"></polygon>
                </svg>
                <h3>暂无赞助用户</h3>
                <p>还没有赞助用户记录</p>
            </div>
        <?php else: ?>
            <div style="padding: 16px;">
                <div class="user-grid">
                    <?php foreach ($sponsorUsers as $user): ?>
                    <div class="user-card">
                        <div class="user-card-header">
                            <img src="<?php echo QQ_AVATAR_URL . $user['qq']; ?>&s=640" alt="头像" class="user-avatar">
                            <div class="user-info">
                                <h3><?php echo htmlspecialchars($user['nickname'] ?? $user['qq']); ?></h3>
                                <p>QQ: <?php echo $user['qq']; ?></p>
                            </div>
                        </div>

                        <div class="user-card-badges">
                            <span class="badge badge-purple">赞助用户</span>
                            <?php if ($user['user_type'] == 'unregistered'): ?>
                                <span class="badge badge-warning">未注册</span>
                            <?php endif; ?>
                        </div>

                        <div class="user-card-details">
                            <?php if ($user['amount'] > 0): ?>
                                <div><strong>赞助金额：</strong><span style="color: var(--color-success); font-weight: 600;">¥<?php echo number_format($user['amount'], 2); ?></span></div>
                            <?php endif; ?>
                            <?php if (!empty($user['note'])): ?>
                                <div><strong>备注：</strong><?php echo htmlspecialchars($user['note']); ?></div>
                            <?php endif; ?>
                            <div><strong>注册时间：</strong><?php echo $user['register_time']; ?></div>
                            <div><strong>最后登录：</strong><?php echo $user['last_login_time'] ?? '暂无'; ?></div>
                            <?php if ($user['upload_permission'] == 1): ?>
                                <div><strong>上传权限：</strong>已授权</div>
                            <?php endif; ?>
                        </div>

                        <div class="user-card-actions">
                            <button class="btn btn-danger btn-sm" onclick="removeSponsor('<?php echo $user['qq']; ?>')">取消赞助</button>
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

    function addSponsor() {
        const qq = document.getElementById('qq').value;
        const amount = document.getElementById('amount').value;
        const note = document.getElementById('note').value;

        if (!qq) {
            Toast.error('请输入QQ号');
            return;
        }

        Loading.show();
        fetch('../api/admin/sponsor_action.php', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded'
            },
            body: `action=add&qq=${qq}&amount=${amount || 0}&note=${encodeURIComponent(note)}&csrf_token=${csrfToken}`
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
    }

    function removeSponsor(qq) {
        Modal.confirm('确认取消该用户的赞助状态？', function() {
            Loading.show();
            fetch('../api/admin/sponsor_action.php', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded'
                },
                body: `action=remove&qq=${qq}&csrf_token=${csrfToken}`
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
