<?php
require_once __DIR__ . '/../require.php';
require_once __DIR__ . '/../function.php';

if (!isAdminLoggedIn()) {
    header('Location: login.php');
    exit;
}

$pdo = getDBConnection();
$dbError = '';
$stats = [
    'totalUsers' => 0,
    'activeUsers' => 0,
    'sponsorUsers' => 0,
    'bannedUsers' => 0,
    'onlinePlugins' => 0,
    'pendingPlugins' => 0,
    'todayNew' => 0
];

if (!$pdo) {
    $dbError = '数据库连接失败，请检查数据库配置';
} else {
    $stats['totalUsers'] = $pdo->query("SELECT COUNT(*) FROM users")->fetchColumn();
    // 赞助用户统计：两张表合并后去重（QQ号唯一），避免遗漏未注册的赞助用户
    $stats['sponsorUsers'] = $pdo->query("SELECT COUNT(DISTINCT qq) FROM (
        SELECT qq FROM users WHERE is_sponsor = 1 AND qq IS NOT NULL
        UNION
        SELECT qq FROM sponsor_users WHERE qq IS NOT NULL
    ) AS t")->fetchColumn();
    $stats['bannedUsers'] = $pdo->query("SELECT COUNT(*) FROM users WHERE is_banned = 1")->fetchColumn();
    $stats['onlinePlugins'] = $pdo->query("SELECT COUNT(*) FROM plugins WHERE status = 1")->fetchColumn();
    $stats['pendingPlugins'] = $pdo->query("SELECT COUNT(*) FROM plugins WHERE status = 0")->fetchColumn();
    $stats['todayNew'] = $pdo->query("SELECT COUNT(*) FROM users WHERE DATE(register_time) = CURDATE()")->fetchColumn();
    
    $activeTime = date('Y-m-d H:i:s', time() - 600);
    $stats['activeUsers'] = $pdo->query("SELECT COUNT(*) FROM users WHERE last_login_time >= '$activeTime'")->fetchColumn();
}

$pageTitle = '数据统计';
$activeMenu = 'dashboard';
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
    <h1 class="page-title">数据统计</h1>
    <p class="page-subtitle">系统整体运行状态概览</p>
</div>

<div class="stats-grid">
    <div class="stat-card">
        <div class="stat-card-header">
            <span class="stat-label">总用户数</span>
            <div class="stat-icon primary">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="20" height="20">
                    <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
                    <circle cx="9" cy="7" r="4"></circle>
                    <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
                    <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
                </svg>
            </div>
        </div>
        <div class="stat-value"><?php echo $stats['totalUsers']; ?></div>
        <div class="stat-label">今日新增 <?php echo $stats['todayNew']; ?> 人</div>
    </div>

    <div class="stat-card">
        <div class="stat-card-header">
            <span class="stat-label">活跃用户</span>
            <div class="stat-icon success">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="20" height="20">
                    <polyline points="22 12 18 12 15 21 9 3 6 12 2 12"></polyline>
                </svg>
            </div>
        </div>
        <div class="stat-value"><?php echo $stats['activeUsers']; ?></div>
        <div class="stat-label">近10分钟在线</div>
    </div>

    <div class="stat-card">
        <div class="stat-card-header">
            <span class="stat-label">赞助用户</span>
            <div class="stat-icon purple">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="20" height="20">
                    <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"></polygon>
                </svg>
            </div>
        </div>
        <div class="stat-value"><?php echo $stats['sponsorUsers']; ?></div>
        <div class="stat-label">感谢支持</div>
    </div>

    <div class="stat-card">
        <div class="stat-card-header">
            <span class="stat-label">封禁用户</span>
            <div class="stat-icon danger">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="20" height="20">
                    <circle cx="12" cy="12" r="10"></circle>
                    <line x1="4.93" y1="4.93" x2="19.07" y2="19.07"></line>
                </svg>
            </div>
        </div>
        <div class="stat-value"><?php echo $stats['bannedUsers']; ?></div>
        <div class="stat-label">黑名单用户</div>
    </div>

    <div class="stat-card">
        <div class="stat-card-header">
            <span class="stat-label">在线脚本</span>
            <div class="stat-icon primary">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="20" height="20">
                    <path d="M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.77-3.77a6 6 0 0 1-7.94 7.94l-6.91 6.91a2.12 2.12 0 0 1-3-3l6.91-6.91a6 6 0 0 1 7.94-7.94l-3.76 3.76z"></path>
                </svg>
            </div>
        </div>
        <div class="stat-value"><?php echo $stats['onlinePlugins']; ?></div>
        <div class="stat-label">已上线脚本</div>
    </div>

    <div class="stat-card">
        <div class="stat-card-header">
            <span class="stat-label">待审核</span>
            <div class="stat-icon warning">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="20" height="20">
                    <circle cx="12" cy="12" r="10"></circle>
                    <line x1="12" y1="8" x2="12" y2="12"></line>
                    <line x1="12" y1="16" x2="12.01" y2="16"></line>
                </svg>
            </div>
        </div>
        <div class="stat-value"><?php echo $stats['pendingPlugins']; ?></div>
        <div class="stat-label">待审核脚本</div>
    </div>
</div>

<div class="card">
    <div class="card-header">
        <h2 class="card-title">系统信息</h2>
    </div>
    <div class="card-body">
        <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 16px;">
            <div>
                <div style="font-size: 13px; color: var(--color-text-secondary); margin-bottom: 4px;">系统版本</div>
                <div style="font-size: 15px; font-weight: 500;"><?php echo SYSTEM_VERSION; ?></div>
            </div>
            <div>
                <div style="font-size: 13px; color: var(--color-text-secondary); margin-bottom: 4px;">PHP 版本</div>
                <div style="font-size: 15px; font-weight: 500;"><?php echo phpversion(); ?></div>
            </div>
            <div>
                <div style="font-size: 13px; color: var(--color-text-secondary); margin-bottom: 4px;">操作系统</div>
                <div style="font-size: 15px; font-weight: 500;"><?php echo PHP_OS; ?></div>
            </div>
            <div>
                <div style="font-size: 13px; color: var(--color-text-secondary); margin-bottom: 4px;">服务器时间</div>
                <div style="font-size: 15px; font-weight: 500;"><?php echo date('Y-m-d H:i:s'); ?></div>
            </div>
            <div>
                <div style="font-size: 13px; color: var(--color-text-secondary); margin-bottom: 4px;">数据库状态</div>
                <div style="font-size: 15px; font-weight: 500; color: <?php echo $pdo ? 'var(--color-success)' : 'var(--color-danger)'; ?>;"><?php echo $pdo ? '正常连接' : '连接失败'; ?></div>
            </div>
            <div>
                <div style="font-size: 13px; color: var(--color-text-secondary); margin-bottom: 4px;">当前管理员</div>
                <div style="font-size: 15px; font-weight: 500;"><?php echo htmlspecialchars($_SESSION['admin_username'] ?? '未知'); ?></div>
            </div>
        </div>
    </div>
</div>

<?php require_once __DIR__ . '/footer.php'; ?>
