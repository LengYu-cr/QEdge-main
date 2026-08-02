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

$pageTitle = '赞助作者';
$activeMenu = 'sponsor';
?>

<?php require_once __DIR__ . '/header.php'; ?>

<?php if ($dbError): ?>
    <div class="alert alert-danger"><?php echo htmlspecialchars($dbError); ?></div>
<?php endif; ?>

<div class="card" style="max-width: 720px;">
    <div class="card-header">
        <h3 class="card-title">赞助作者</h3>
        <p class="card-subtitle" style="margin-top:4px;">感谢您对 QEdge 项目的支持，每一笔赞助都是我们持续更新的动力</p>
    </div>
    <div class="card-body">
        <div style="display:flex; flex-direction:column; align-items:center; gap: 16px; padding: 20px 0;">
            <div style="
                width: 100%;
                max-width: 320px;
                aspect-ratio: 1 / 1;
                border-radius: 20px;
                overflow: hidden;
                box-shadow: 0 4px 24px rgba(100, 150, 200, 0.15);
                border: 1px solid var(--color-border);
                background: #fff;">
                <img
                    src="https://cdn.yuafeng.cn/ly/wx.png"
                    alt="微信收款码"
                    style="width:100%; height:100%; object-fit:cover; display:block;"
                    onerror="this.parentNode.innerHTML='<div style=\'padding:40px;text-align:center;color:var(--color-text-secondary);\'>收款码图片加载失败<br>请访问 <a href=\'https://cdn.yuafeng.cn/ly/wx.png\' style=\'color:var(--color-primary);\'>https://cdn.yuafeng.cn/ly/wx.png</a></div>'"
                >
            </div>
            <div style="text-align:center; line-height:1.8; color: var(--color-text-secondary); font-size: 14px;">
                <div>
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="14" height="14" style="vertical-align:-2px; color:#07c160;">
                        <path d="M9 11l3 3L22 4"></path>
                        <path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11"></path>
                    </svg>
                    &nbsp;微信扫码 / 长按识别 任意金额
                </div>
                <div style="margin-top:6px;">您的支持会出现在 <a href="../index.php#sponsor-wall" style="color:var(--color-primary);">首页赞助墙</a>（如需匿名请在付款时备注）</div>
            </div>
        </div>
    </div>
</div>

<?php require_once __DIR__ . '/footer.php'; ?>
