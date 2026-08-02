<?php
/**
 * 插件详情页
 */

require_once __DIR__ . '/../require.php';
require_once __DIR__ . '/../function.php';

$pdo = getDBConnection();
$dbError = '';

$couldId = isset($_GET['could_id']) ? intval($_GET['could_id']) : 0;

if ($couldId <= 0) {
    die('插件ID不正确');
}

if (!$pdo) {
    $dbError = '数据库连接失败，请检查数据库配置';
    $plugin = null;
} else {
    try {
        $stmt = $pdo->prepare("SELECT p.*, u.nickname as uploader_name FROM plugins p LEFT JOIN users u ON p.upload_qq = u.qq WHERE p.could_id = ?");
        $stmt->execute([$couldId]);
        $plugin = $stmt->fetch();

        if (!$plugin) {
            die('插件不存在');
        }
        // 关键：修复历史数据乱码，保证 plugin_name/author_name/description 全是中文
        $plugin = dbFixUtf8($plugin);
    } catch (Exception $e) {
        $dbError = '获取插件信息失败: ' . $e->getMessage();
        $plugin = null;
    }
}

if (!$plugin) {
    die('插件不存在或获取失败');
}

$statusMap = [
    0 => ['text' => '待审核', 'class' => 'warning'],
    1 => ['text' => '已通过', 'class' => 'success'],
    2 => ['text' => '已拒绝', 'class' => 'danger']
];
$statusInfo = $statusMap[$plugin['status']];

$versions = [];
try {
    $stmt = $pdo->prepare("SELECT could_id, version_code, status, upload_time FROM plugins WHERE plugin_id = ? ORDER BY could_id DESC");
    $stmt->execute([$plugin['plugin_id']]);
    $versions = $stmt->fetchAll();
    $versions = dbFixUtf8($versions);  // 版本列表也转码
} catch (Exception $e) {
}

$prevVersion = null;
$nextVersion = null;
if (count($versions) > 1) {
    $index = array_search($couldId, array_column($versions, 'could_id'));
    if ($index !== false) {
        if (isset($versions[$index + 1])) {
            $nextVersion = $versions[$index + 1]['could_id'];
        }
        if (isset($versions[$index - 1])) {
            $prevVersion = $versions[$index - 1]['could_id'];
        }
    }
}
?>

<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><?php echo htmlspecialchars($plugin['plugin_name']); ?> - 插件详情</title>
    <link rel="stylesheet" href="../assets/style.css">
</head>
<body>
    <nav class="navbar">
        <div class="navbar-inner">
            <a href="../index.php" class="navbar-brand">
                <span class="brand-icon">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="width:18px;height:18px;">
                        <path d="M12 2L2 7l10 5 10-5-10-5z"></path>
                        <path d="M2 17l10 5 10-5"></path>
                        <path d="M2 12l10 5 10-5"></path>
                    </svg>
                </span>
                QEdge
            </a>
            <div class="navbar-nav">
                <a href="../index.php">
                    <span class="icon">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                            <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"></path>
                            <polyline points="9 22 9 12 15 12 15 22"></polyline>
                        </svg>
                    </span>
                    <span>首页</span>
                </a>
                <a href="list.php" class="active">
                    <span class="icon">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
                            <polyline points="7 10 12 15 17 10"></polyline>
                            <line x1="12" y1="15" x2="12" y2="3"></line>
                        </svg>
                    </span>
                    <span>在线脚本</span>
                </a>
            </div>
        </div>
    </nav>

    <div class="container" style="max-width: 800px;">
        <?php if ($dbError): ?>
            <div class="alert alert-danger"><?php echo htmlspecialchars($dbError); ?></div>
        <?php endif; ?>

        <div class="card">
            <div class="card-header">
                <div style="display: flex; justify-content: space-between; align-items: flex-start; gap: 16px;">
                    <div style="display: flex; align-items: center; gap: 16px;">
                        <div style="width: 56px; height: 56px; background: var(--color-primary-light); color: var(--color-primary); border-radius: var(--radius-lg); display: flex; align-items: center; justify-content: center;">
                            <span class="icon icon-lg">
                                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                    <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V9z"></path>
                                    <polyline points="14 2 14 8 20 8"></polyline>
                                </svg>
                            </span>
                        </div>
                        <div>
                            <h1 style="font-size: 20px; font-weight: 600; color: var(--color-text-primary); margin-bottom: 4px;"><?php echo htmlspecialchars($plugin['plugin_name']); ?></h1>
                            <div style="font-size: 13px; color: var(--color-text-tertiary);">
                                插件ID: <?php echo htmlspecialchars($plugin['plugin_id']); ?>
                                · 版本: <span class="badge badge-primary" style="font-size: 11px; padding: 2px 8px;">v<?php echo htmlspecialchars($plugin['version_code']); ?></span>
                            </div>
                        </div>
                    </div>
                    <span class="badge badge-<?php echo $statusInfo['class']; ?>"><?php echo $statusInfo['text']; ?></span>
                </div>
            </div>

            <div class="card-body">
                <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 16px 32px; margin-bottom: 24px;">
                    <div>
                        <div style="font-size: 12px; color: var(--color-text-tertiary); text-transform: uppercase; letter-spacing: 0.5px; margin-bottom: 4px;">作者</div>
                        <div style="font-size: 15px; font-weight: 500; color: var(--color-text-primary);"><?php echo htmlspecialchars($plugin['author_name']); ?></div>
                    </div>
                    <div>
                        <div style="font-size: 12px; color: var(--color-text-tertiary); text-transform: uppercase; letter-spacing: 0.5px; margin-bottom: 4px;">上传者</div>
                        <div style="font-size: 15px; font-weight: 500; color: var(--color-text-primary);"><?php echo htmlspecialchars($plugin['uploader_name'] ?? $plugin['upload_qq']); ?></div>
                    </div>
                    <div>
                        <div style="font-size: 12px; color: var(--color-text-tertiary); text-transform: uppercase; letter-spacing: 0.5px; margin-bottom: 4px;">下载次数</div>
                        <div style="font-size: 15px; font-weight: 500; color: var(--color-text-primary);"><?php echo number_format($plugin['download_count']); ?></div>
                    </div>
                    <div>
                        <div style="font-size: 12px; color: var(--color-text-tertiary); text-transform: uppercase; letter-spacing: 0.5px; margin-bottom: 4px;">上传时间</div>
                        <div style="font-size: 15px; font-weight: 500; color: var(--color-text-primary);"><?php echo date('Y-m-d H:i', strtotime($plugin['upload_time'])); ?></div>
                    </div>
                </div>

                <?php if (!empty($plugin['description'])): ?>
                <div style="padding-top: 20px; border-top: 1px solid var(--color-border-light);">
                    <h3 style="font-size: 14px; color: var(--color-text-tertiary); font-weight: 500; margin-bottom: 12px;">插件描述</h3>
                    <p style="font-size: 14px; line-height: 1.7; color: var(--color-text-secondary);"><?php echo nl2br(htmlspecialchars($plugin['description'])); ?></p>
                </div>
                <?php endif; ?>
            </div>

            <div class="card-footer" style="display: flex; gap: 12px; flex-wrap: wrap;">
                <?php if ($plugin['status'] == 1): ?>
                    <a href="download.php?could_id=<?php echo $plugin['could_id']; ?>" class="btn btn-success">
                        <span class="icon">
                            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
                                <polyline points="7 10 12 15 17 10"></polyline>
                                <line x1="12" y1="15" x2="12" y2="3"></line>
                            </svg>
                        </span>
                        下载插件
                    </a>
                <?php endif; ?>
                <a href="list.php" class="btn btn-secondary">
                    <span class="icon">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                            <line x1="19" y1="12" x2="5" y2="12"></line>
                            <polyline points="12 19 5 12 12 5"></polyline>
                        </svg>
                    </span>
                    返回列表
                </a>
            </div>
        </div>

        <?php if (!empty($versions) && count($versions) > 1): ?>
        <div class="card" style="margin-top: 16px;">
            <div class="card-header">
                <h2 class="card-title">版本历史 (共 <?php echo count($versions); ?> 个版本)</h2>
            </div>
            <div class="card-body">
                <div style="display: flex; flex-wrap: wrap; gap: 8px;">
                    <?php foreach ($versions as $v): 
                        $statusBadgeClass = 'secondary';
                        if ($v['status'] == 1) $statusBadgeClass = 'success';
                        elseif ($v['status'] == 0) $statusBadgeClass = 'warning';
                        elseif ($v['status'] == 2) $statusBadgeClass = 'danger';
                        $isActive = ($v['could_id'] == $couldId);
                    ?>
                        <a href="details.php?could_id=<?php echo $v['could_id']; ?>" 
                           style="display: inline-flex; align-items: center; gap: 6px; padding: 6px 12px; border-radius: var(--radius-md); font-size: 13px; text-decoration: none; transition: var(--transition); <?php echo $isActive ? 'background: var(--color-primary); color: white;' : 'background: var(--color-bg-secondary); color: var(--color-text-secondary);'; ?>"
                           onmouseover="if(!<?php echo $isActive ? 'true' : 'false'; ?>){this.style.background='var(--color-bg-tertiary)';}"
                           onmouseout="if(!<?php echo $isActive ? 'true' : 'false'; ?>){this.style.background='var(--color-bg-secondary)';}">
                            <span style="width: 6px; height: 6px; border-radius: 50%; display: inline-block; <?php echo $isActive ? 'background: white;' : 'background: var(--color-text-tertiary);'; ?>"></span>
                            v<?php echo htmlspecialchars($v['version_code']); ?>
                            <?php if ($isActive): ?> 当前<?php endif; ?>
                        </a>
                    <?php endforeach; ?>
                </div>
            </div>
        </div>
        <?php endif; ?>

        <div style="text-align: center; padding: 24px 0; color: var(--color-text-tertiary); font-size: 12px;">
            插件版本记录ID: <?php echo $plugin['could_id']; ?> · 数据更新时间: <?php echo date('Y-m-d H:i:s'); ?>
        </div>
    </div>
</body>
</html>
