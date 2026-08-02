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
$totalPages = 0;

if (!$pdo) {
    $dbError = '数据库连接失败，请检查数据库配置';
} else {
    $search = isset($_GET['search']) ? trim($_GET['search']) : '';
    $page = isset($_GET['page']) ? intval($_GET['page']) : 1;
    $perPage = 20;
    $offset = ($page - 1) * $perPage;

    $where = '';
    $params = [];
    if (!empty($search)) {
        $where = "WHERE qq LIKE ? OR nickname LIKE ?";
        $params = ['%' . $search . '%', '%' . $search . '%'];
    }

    $countSql = "SELECT COUNT(*) FROM users " . $where;
    $stmt = $pdo->prepare($countSql);
    $stmt->execute($params);
    $total = $stmt->fetchColumn();
    $totalPages = ceil($total / $perPage);

    $sql = "SELECT * FROM users " . $where . " ORDER BY register_time DESC LIMIT ? OFFSET ?";
    $stmt = $pdo->prepare($sql);
    $stmt->execute(array_merge($params, [$perPage, $offset]));
    $users = $stmt->fetchAll();
}

$csrfToken = generateCSRFToken();
$pageTitle = '用户管理';
$activeMenu = 'users';
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
    <h1 class="page-title">用户管理</h1>
    <p class="page-subtitle">共 <?php echo $total; ?> 位用户</p>
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

<?php if (empty($users) && !$dbError): ?>
    <div class="card">
        <div class="card-body">
            <div class="empty-state">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" class="empty-state-icon">
                    <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
                    <circle cx="9" cy="7" r="4"></circle>
                    <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
                    <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
                </svg>
                <h3>暂无用户</h3>
                <p><?php echo !empty($search) ? '没有找到匹配的用户' : '还没有用户注册'; ?></p>
            </div>
        </div>
    </div>
<?php else: ?>
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
                <?php if ($user['is_sponsor'] == 1): ?>
                    <span class="badge badge-purple">赞助用户</span>
                <?php endif; ?>
                <?php if ($user['is_banned'] == 1): ?>
                    <span class="badge badge-danger">已拉黑</span>
                <?php endif; ?>
                <?php if ($user['upload_permission'] == 1): ?>
                    <span class="badge badge-primary">上传权限</span>
                <?php endif; ?>
                <?php if ($user['review_permission'] == 1): ?>
                    <span class="badge badge-success">审核权限</span>
                <?php endif; ?>
            </div>

            <div class="user-card-details">
                <div><strong>注册时间：</strong><?php echo $user['register_time']; ?></div>
                <div><strong>最后登录：</strong><?php echo $user['last_login_time'] ?? '暂无'; ?></div>
                <div><strong>模块版本：</strong><?php echo $user['module_version'] ?? '未知'; ?></div>
                <div><strong>QQ版本：</strong><?php echo $user['qq_version'] ?? '未知'; ?></div>
            </div>

            <div class="user-card-actions">
                <?php if ($user['is_banned'] == 0): ?>
                    <button class="btn btn-warning btn-sm" onclick="userAction('<?php echo $user['qq']; ?>', 'ban')">拉黑</button>
                <?php else: ?>
                    <button class="btn btn-success btn-sm" onclick="userAction('<?php echo $user['qq']; ?>', 'unban')">解除拉黑</button>
                <?php endif; ?>

                <?php if ($user['upload_permission'] == 0): ?>
                    <button class="btn btn-primary btn-sm" onclick="userAction('<?php echo $user['qq']; ?>', 'grant_upload')">授权上传</button>
                <?php else: ?>
                    <button class="btn btn-secondary btn-sm" onclick="userAction('<?php echo $user['qq']; ?>', 'revoke_upload')">撤销上传</button>
                <?php endif; ?>

                <?php if ($user['review_permission'] == 0): ?>
                    <button class="btn btn-success btn-sm" onclick="userAction('<?php echo $user['qq']; ?>', 'grant_review')">授权审核</button>
                <?php else: ?>
                    <button class="btn btn-secondary btn-sm" onclick="userAction('<?php echo $user['qq']; ?>', 'revoke_review')">撤销审核</button>
                <?php endif; ?>

                <?php if ($user['is_sponsor'] == 0): ?>
                    <button class="btn btn-purple btn-sm" onclick="userAction('<?php echo $user['qq']; ?>', 'set_sponsor')">设为赞助</button>
                <?php else: ?>
                    <button class="btn btn-secondary btn-sm" onclick="userAction('<?php echo $user['qq']; ?>', 'unset_sponsor')">取消赞助</button>
                <?php endif; ?>

                <button class="btn btn-danger btn-sm" onclick="confirmDelete('<?php echo $user['qq']; ?>')">删除</button>
            </div>
        </div>
        <?php endforeach; ?>
    </div>

    <?php if ($totalPages > 1): ?>
    <div class="pagination">
        <?php if ($page > 1): ?>
            <a href="?page=<?php echo $page - 1; ?><?php echo !empty($search) ? '&search=' . urlencode($search) : ''; ?>">上一页</a>
        <?php endif; ?>

        <?php
        $start = max(1, $page - 2);
        $end = min($totalPages, $page + 2);
        for ($i = $start; $i <= $end; $i++):
        ?>
            <?php if ($i == $page): ?>
                <span class="active"><?php echo $i; ?></span>
            <?php else: ?>
                <a href="?page=<?php echo $i; ?><?php echo !empty($search) ? '&search=' . urlencode($search) : ''; ?>"><?php echo $i; ?></a>
            <?php endif; ?>
        <?php endfor; ?>

        <?php if ($page < $totalPages): ?>
            <a href="?page=<?php echo $page + 1; ?><?php echo !empty($search) ? '&search=' . urlencode($search) : ''; ?>">下一页</a>
        <?php endif; ?>
    </div>
    <?php endif; ?>
<?php endif; ?>

<script>
    const csrfToken = '<?php echo $csrfToken; ?>';

    function userAction(qq, action) {
        Modal.confirm('确认执行此操作？', function() {
            Loading.show();
            fetch('../api/admin/user_action.php', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded'
                },
                body: `qq=${qq}&action=${action}&csrf_token=${csrfToken}`
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
            .catch(() => {
                Loading.hide();
                Toast.error('操作失败，请稍后重试');
            });
        });
    }

    function confirmDelete(qq) {
        Modal.confirm('确认删除此用户？此操作不可恢复！', function() {
            Loading.show();
            fetch('../api/admin/user_action.php', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded'
                },
                body: `qq=${qq}&action=delete&csrf_token=${csrfToken}`
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
            .catch(() => {
                Loading.hide();
                Toast.error('操作失败，请稍后重试');
            });
        });
    }
</script>

<?php require_once __DIR__ . '/footer.php'; ?>
