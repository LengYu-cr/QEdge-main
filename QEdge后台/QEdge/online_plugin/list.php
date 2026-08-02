<?php
require_once __DIR__ . '/../require.php';
require_once __DIR__ . '/../function.php';

$pdo = getDBConnection();
$dbError = '';

if (!$pdo) {
    $dbError = '数据库连接失败，请检查数据库配置';
}

$search = isset($_GET['search']) ? trim($_GET['search']) : '';

if (isset($_GET['api']) && $_GET['api'] === 'json') {
    if (!$pdo) {
        jsonResponse(500, '数据库连接失败');
    }
    try {
        if (!empty($search)) {
            $stmt = $pdo->prepare("SELECT could_id, plugin_id, plugin_name, version_code, author_name, upload_qq, download_count, upload_time, description FROM plugins WHERE status = 1 AND (plugin_name LIKE ? OR author_name LIKE ? OR upload_qq LIKE ?) ORDER BY upload_time DESC");
            $stmt->execute(["%$search%", "%$search%", "%$search%"]);
        } else {
            $stmt = $pdo->query("SELECT could_id, plugin_id, plugin_name, version_code, author_name, upload_qq, download_count, upload_time, description FROM plugins WHERE status = 1 ORDER BY upload_time DESC");
        }
        $plugins = $stmt->fetchAll();
        // 关键：历史数据可能存了乱码，必须过一次dbFixUtf8再json_encode输出给Android端，
        // 否则模块端删除插件弹窗、列表显示全是乱码（典型如"ç¤ºä¾èæ¬"）
        $plugins = dbFixUtf8($plugins);
        
        $index = 1;
        foreach ($plugins as &$plugin) {
            $plugin['n'] = $index++;
        }
        
        jsonResponse(200, '获取成功', $plugins);
    } catch (Exception $e) {
        error_log("获取脚本列表失败: " . $e->getMessage());
        jsonResponse(500, '获取失败: ' . $e->getMessage());
    }
}

$plugins = [];
if ($pdo) {
    try {
        if (!empty($search)) {
            $stmt = $pdo->prepare("SELECT could_id, plugin_id, plugin_name, version_code, author_name, upload_qq, download_count, upload_time, description FROM plugins WHERE status = 1 AND (plugin_name LIKE ? OR author_name LIKE ? OR upload_qq LIKE ?) ORDER BY upload_time DESC");
            $stmt->execute(["%$search%", "%$search%", "%$search%"]);
        } else {
            $stmt = $pdo->query("SELECT could_id, plugin_id, plugin_name, version_code, author_name, upload_qq, download_count, upload_time, description FROM plugins WHERE status = 1 ORDER BY upload_time DESC");
        }
        $plugins = $stmt->fetchAll();
        $plugins = dbFixUtf8($plugins);  // Web端列表展示也转码
        
        $index = 1;
        foreach ($plugins as &$plugin) {
            $plugin['n'] = $index++;
        }
    } catch (Exception $e) {
        $dbError = '获取脚本列表失败: ' . $e->getMessage();
        error_log("获取脚本列表失败: " . $e->getMessage());
    }
}
?>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>在线脚本列表 - QEdge</title>
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
                <a href="../user/login.php">
                    <span class="icon">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                            <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                            <circle cx="12" cy="7" r="4"></circle>
                        </svg>
                    </span>
                    <span>用户中心</span>
                </a>
            </div>
        </div>
    </nav>

    <div class="container">
        <div class="page-header">
            <h1 class="page-title">在线脚本列表</h1>
            <p class="page-subtitle">共 <?php echo count($plugins); ?> 个脚本</p>
        </div>

        <?php if ($dbError): ?>
            <div class="alert alert-danger"><?php echo htmlspecialchars($dbError); ?></div>
        <?php endif; ?>

        <div class="search-bar">
            <input type="text" id="searchInput" class="form-input" placeholder="搜索脚本名、作者或QQ..." value="<?php echo htmlspecialchars($search); ?>">
            <button class="btn btn-primary" onclick="doSearch()">
                <span class="icon">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <circle cx="11" cy="11" r="8"></circle>
                        <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
                    </svg>
                </span>
                搜索
            </button>
            <?php if (!empty($search)): ?>
                <button class="btn btn-secondary" onclick="clearSearch()">清除</button>
            <?php endif; ?>
        </div>

        <?php if (empty($plugins)): ?>
            <div class="empty-state">
                <div class="empty-state-icon">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V9z"></path>
                        <polyline points="14 2 14 8 20 8"></polyline>
                    </svg>
                </div>
                <?php if (!empty($search)): ?>
                    <h3>未找到匹配的脚本</h3>
                    <p>请尝试其他关键词</p>
                <?php else: ?>
                    <h3>暂无在线脚本</h3>
                <?php endif; ?>
            </div>
        <?php else: ?>
            <div style="display: grid; grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); gap: 16px;">
                <?php foreach ($plugins as $plugin): ?>
                <div class="card">
                    <div class="card-header">
                        <div style="display: flex; align-items: center; gap: 12px;">
                            <div style="width: 44px; height: 44px; background: var(--color-primary-light); color: var(--color-primary); border-radius: var(--radius-md); display: flex; align-items: center; justify-content: center;">
                                <span class="icon">
                                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                        <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V9z"></path>
                                        <polyline points="14 2 14 8 20 8"></polyline>
                                    </svg>
                                </span>
                            </div>
                            <div style="flex: 1; min-width: 0;">
                                <div class="card-title" style="font-size: 15px;"><?php echo htmlspecialchars($plugin['plugin_name']); ?></div>
                                <div style="font-size: 12px; color: var(--color-text-tertiary);">ID: <?php echo htmlspecialchars($plugin['plugin_id']); ?></div>
                            </div>
                        </div>
                    </div>
                    <div class="card-body">
                        <div style="display: flex; gap: 8px; margin-bottom: 16px; flex-wrap: wrap;">
                            <span class="badge badge-primary">v<?php echo htmlspecialchars($plugin['version_code']); ?></span>
                            <span class="badge badge-success">
                                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="width:12px;height:12px;">
                                    <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
                                    <polyline points="7 10 12 15 17 10"></polyline>
                                    <line x1="12" y1="15" x2="12" y2="3"></line>
                                </svg>
                                <?php echo $plugin['download_count']; ?> 次下载
                            </span>
                        </div>
                        <div style="font-size: 13px; color: var(--color-text-secondary); line-height: 1.6; margin-bottom: 10px; padding: 8px 12px; background: var(--color-bg-secondary); border-radius: var(--radius-sm);"><?php echo htmlspecialchars(!empty($plugin['description']) ? $plugin['description'] : '该作者很懒，什么也没留下'); ?></div>
                        <div style="font-size: 13px; color: var(--color-text-secondary); line-height: 1.8;">
                            <div><strong style="color: var(--color-text-primary); font-weight: 500;">作者：</strong><?php echo htmlspecialchars($plugin['author_name']); ?></div>
                            <div><strong style="color: var(--color-text-primary); font-weight: 500;">上传者：</strong><?php echo $plugin['upload_qq']; ?></div>
                            <div><strong style="color: var(--color-text-primary); font-weight: 500;">上传时间：</strong><?php echo $plugin['upload_time']; ?></div>
                        </div>
                    </div>
                    <div class="card-footer">
                        <a href="download.php?id=<?php echo $plugin['could_id']; ?>" class="btn btn-primary btn-sm" style="width: 100%; text-decoration: none;">
                            <span class="icon">
                                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                    <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
                                    <polyline points="7 10 12 15 17 10"></polyline>
                                    <line x1="12" y1="15" x2="12" y2="3"></line>
                                </svg>
                            </span>
                            下载脚本
                        </a>
                    </div>
                </div>
                <?php endforeach; ?>
            </div>
        <?php endif; ?>
    </div>

    <script src="../assets/common.js"></script>
    <script>
        function doSearch() {
            var keyword = document.getElementById('searchInput').value.trim();
            if (keyword) {
                window.location.href = 'list.php?search=' + encodeURIComponent(keyword);
            } else {
                window.location.href = 'list.php';
            }
        }

        function clearSearch() {
            window.location.href = 'list.php';
        }

        document.getElementById('searchInput').addEventListener('keypress', function(e) {
            if (e.key === 'Enter') {
                doSearch();
            }
        });
    </script>
</body>
</html>
