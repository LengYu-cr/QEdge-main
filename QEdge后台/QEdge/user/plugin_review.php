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

if ($userInfo['review_permission'] != 1) {
    die('您没有审核权限');
}

$statusFilter = isset($_GET['status']) ? intval($_GET['status']) : 0;
if (!in_array($statusFilter, [-1, 0, 1, 2, 3])) {
    $statusFilter = 0;
}

$plugins = [];
if ($pdo) {
    try {
        if ($statusFilter === -1) {
            $stmt = $pdo->query("SELECT p.could_id, p.plugin_id, p.plugin_name, p.version_code, p.author_name, p.upload_qq, p.file_path, p.download_count, p.status, p.upload_time, p.reject_reason, p.description, u.nickname as uploader_name FROM plugins p LEFT JOIN users u ON p.upload_qq = u.qq ORDER BY p.upload_time DESC");
        } else {
            $stmt = $pdo->prepare("SELECT p.could_id, p.plugin_id, p.plugin_name, p.version_code, p.author_name, p.upload_qq, p.file_path, p.download_count, p.status, p.upload_time, p.reject_reason, p.description, u.nickname as uploader_name FROM plugins p LEFT JOIN users u ON p.upload_qq = u.qq WHERE p.status = ? ORDER BY p.upload_time DESC");
            $stmt->execute([$statusFilter]);
        }
        $plugins = $stmt->fetchAll();
    } catch (Exception $e) {
        $dbError = '获取插件列表失败: ' . $e->getMessage();
        error_log("获取插件列表失败: " . $e->getMessage());
    }
}

$stats = [];
if ($pdo) {
    try {
        $stats['pending'] = $pdo->query("SELECT COUNT(*) FROM plugins WHERE status = 0")->fetchColumn();
        $stats['approved'] = $pdo->query("SELECT COUNT(*) FROM plugins WHERE status = 1")->fetchColumn();
        $stats['rejected'] = $pdo->query("SELECT COUNT(*) FROM plugins WHERE status = 2")->fetchColumn();
        $stats['takedown'] = $pdo->query("SELECT COUNT(*) FROM plugins WHERE status = 3")->fetchColumn();
        $stats['total'] = $pdo->query("SELECT COUNT(*) FROM plugins")->fetchColumn();
    } catch (Exception $e) {
        $stats = ['pending' => 0, 'approved' => 0, 'rejected' => 0, 'takedown' => 0, 'total' => 0];
    }
}

$csrfToken = generateCSRFToken();
$pageTitle = '脚本审核';
?>

<?php require_once __DIR__ . '/header.php'; ?>

<style>
    .review-container { max-width: 1200px; margin: 0 auto; padding: 15px; }
    .review-stats { display: grid; grid-template-columns: repeat(auto-fit, minmax(100px, 1fr)); gap: 12px; margin: 15px 0; }
    .review-stat-card { padding: 15px 10px; text-align: center; background: #fff; border-radius: 8px; border: 1px solid #e8ecf1; border-top: 3px solid #ccc; }
    .review-stat-card .number { font-size: 24px; font-weight: bold; }
    .review-stat-card .label { font-size: 13px; color: #666; margin-top: 4px; }
    .review-stat-card.pending { border-top-color: #d4880f; }
    .review-stat-card.approved { border-top-color: #389e0d; }
    .review-stat-card.rejected { border-top-color: #cf1322; }
    .review-stat-card.takedown { border-top-color: #8c8c8c; }
    .review-filter { display: flex; flex-wrap: wrap; gap: 8px; margin: 15px 0; }
    .review-filter-btn { padding: 8px 18px; border: 1px solid #d9d9d9; background: #fff; border-radius: 4px; cursor: pointer; font-size: 14px; transition: all 0.2s; color: #333; }
    .review-filter-btn.active { background: #1a73e8; color: #fff; border-color: #1a73e8; }
    .review-filter-btn:hover:not(.active) { border-color: #1a73e8; color: #1a73e8; }
    .review-card { background: #fff; border-radius: 8px; border: 1px solid #e8ecf1; margin-bottom: 14px; padding: 18px; transition: box-shadow 0.2s; }
    .review-card:hover { box-shadow: 0 2px 12px rgba(0,0,0,0.06); }
    .review-header { display: flex; flex-wrap: wrap; justify-content: space-between; align-items: center; gap: 8px; margin-bottom: 10px; }
    .review-title { font-size: 17px; font-weight: 600; color: #1a1a1a; word-break: break-word; }
    .review-id { font-size: 12px; color: #999; background: #f5f5f5; padding: 2px 10px; border-radius: 3px; white-space: nowrap; }
    .review-body { margin-bottom: 10px; }
    .review-desc { font-size: 13px; color: #666; line-height: 1.6; margin-bottom: 10px; padding: 8px 12px; background: #fafafa; border-radius: 4px; border-left: 3px solid #d9d9d9; word-break: break-all; overflow-wrap: break-word; white-space: pre-wrap; }
    .review-info { display: grid; grid-template-columns: repeat(auto-fit, minmax(150px, 1fr)); gap: 6px 20px; font-size: 13px; color: #555; }
    .review-info > div { padding: 3px 0; }
    .review-info strong { color: #333; }
    .review-footer { display: flex; flex-wrap: wrap; justify-content: space-between; align-items: center; gap: 10px; padding-top: 12px; border-top: 1px solid #f0f0f0; }
    .review-status-badge { display: inline-block; padding: 3px 12px; border-radius: 3px; font-size: 12px; font-weight: 500; white-space: nowrap; }
    .review-status-badge.pending { background: #fff7e6; color: #d4880f; border: 1px solid #ffe58f; }
    .review-status-badge.approved { background: #f6ffed; color: #389e0d; border: 1px solid #b7eb8f; }
    .review-status-badge.rejected { background: #fff1f0; color: #cf1322; border: 1px solid #ffa39e; }
    .review-status-badge.takedown { background: #f5f5f5; color: #595959; border: 1px solid #d9d9d9; }
    .review-actions { display: flex; flex-wrap: wrap; gap: 6px; }
    .review-btn { padding: 5px 14px; border: 1px solid #d9d9d9; border-radius: 4px; font-size: 13px; cursor: pointer; transition: all 0.2s; white-space: nowrap; min-height: 32px; background: #fff; color: #333; }
    .review-btn:active { transform: scale(0.97); }
    .review-btn-preview { border-color: #1a73e8; color: #1a73e8; }
    .review-btn-preview:hover { background: #e8f0fe; }
    .review-btn-approve { border-color: #389e0d; color: #389e0d; }
    .review-btn-approve:hover { background: #f6ffed; }
    .review-btn-reject { border-color: #cf1322; color: #cf1322; }
    .review-btn-reject:hover { background: #fff1f0; }
    .review-btn-download { border-color: #722ed1; color: #722ed1; }
    .review-btn-download:hover { background: #f9f0ff; }
    .review-btn-takedown { border-color: #8c8c8c; color: #595959; }
    .review-btn-takedown:hover { background: #f5f5f5; }
    .review-empty { text-align: center; padding: 40px 20px; color: #999; font-size: 15px; }
    .review-error { background: #fff1f0; color: #cf1322; padding: 12px 16px; border-radius: 4px; margin-bottom: 16px; border: 1px solid #ffa39e; }
    .review-modal-overlay { display: none; position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.45); z-index: 1000; justify-content: center; align-items: center; padding: 20px; }
    .review-modal-overlay.show { display: flex; }
    .review-modal { background: #fff; border-radius: 8px; padding: 24px; max-width: 600px; width: 100%; max-height: 85vh; overflow-y: auto; }
    .review-modal h3 { margin-bottom: 16px; font-size: 17px; color: #1a1a1a; font-weight: 600; }
    .review-modal textarea { width: 100%; min-height: 120px; padding: 10px; border: 1px solid #d9d9d9; border-radius: 4px; font-size: 14px; resize: vertical; font-family: inherit; }
    .review-modal textarea:focus { outline: none; border-color: #1a73e8; box-shadow: 0 0 0 2px rgba(26,115,232,0.15); }
    .review-modal-actions { display: flex; gap: 10px; margin-top: 16px; justify-content: flex-end; }
    .review-modal-btn { padding: 7px 22px; border: 1px solid #d9d9d9; border-radius: 4px; font-size: 14px; cursor: pointer; transition: all 0.2s; background: #fff; color: #333; }
    .review-modal-btn-primary { background: #1a73e8; color: #fff; border-color: #1a73e8; }
    .review-modal-btn-primary:hover { background: #1557b0; }
    .review-modal-btn-secondary:hover { border-color: #1a73e8; color: #1a73e8; }
    .reject-reason-box { margin-top: 8px; padding: 8px 12px; background: #fff1f0; border-radius: 4px; color: #cf1322; font-size: 13px; border: 1px solid #ffa39e; }
    .zip-tree { font-family: 'Consolas', 'Monaco', monospace; font-size: 13px; line-height: 1.8; background: #fafafa; border: 1px solid #e8e8e8; border-radius: 4px; padding: 14px; max-height: 400px; overflow-y: auto; }
    .zip-tree .file-item { display: flex; align-items: center; gap: 6px; padding: 1px 0; }
    .zip-tree .file-icon { flex-shrink: 0; width: 16px; text-align: center; font-size: 12px; }
    .zip-tree .file-name { color: #333; word-break: break-all; }
    .zip-tree .file-name.previewable { color: #1a73e8; cursor: pointer; text-decoration: underline; }
    .zip-tree .file-size { color: #999; font-size: 12px; margin-left: auto; white-space: nowrap; }
    .zip-tree .dir-icon { color: #d4880f; }
    .zip-tree .file-icon-doc { color: #1a73e8; }
    .zip-tree .indent { display: inline-block; }
    .zip-loading { text-align: center; padding: 30px; color: #999; }
    @media (max-width: 768px) {
        .review-info { grid-template-columns: 1fr; gap: 3px; }
        .review-footer { flex-direction: column; align-items: stretch; }
        .review-footer > div:first-child { text-align: center; }
        .review-actions { justify-content: center; }
        .review-btn { flex: 1; text-align: center; padding: 7px 10px; }
        .review-filter-btn { flex: 1; text-align: center; padding: 6px 10px; }
        .review-modal { padding: 16px; margin: 10px; }
        .review-modal-actions { flex-direction: column; }
        .review-modal-btn { width: 100%; text-align: center; padding: 9px; }
    }
</style>

<div class="review-container">
    <?php if ($dbError): ?>
        <div class="review-error"><?php echo htmlspecialchars($dbError); ?></div>
    <?php endif; ?>

    <div class="card">
        <div class="user-header">
            <h3>脚本审核</h3>
            <span style="font-size: 14px; color: #999;">共 <?php echo $stats['total']; ?> 个脚本</span>
        </div>
        <div class="review-stats">
            <div class="review-stat-card pending">
                <div class="number" style="color: #d4880f;"><?php echo $stats['pending']; ?></div>
                <div class="label">待审核</div>
            </div>
            <div class="review-stat-card approved">
                <div class="number" style="color: #389e0d;"><?php echo $stats['approved']; ?></div>
                <div class="label">已通过</div>
            </div>
            <div class="review-stat-card rejected">
                <div class="number" style="color: #cf1322;"><?php echo $stats['rejected']; ?></div>
                <div class="label">已拒绝</div>
            </div>
            <div class="review-stat-card takedown">
                <div class="number" style="color: #595959;"><?php echo $stats['takedown']; ?></div>
                <div class="label">已下架</div>
            </div>
        </div>
        <div class="review-filter">
            <button class="review-filter-btn <?php echo $statusFilter === 0 ? 'active' : ''; ?>" onclick="location.href='plugin_review.php?status=0'">待审核</button>
            <button class="review-filter-btn <?php echo $statusFilter === 1 ? 'active' : ''; ?>" onclick="location.href='plugin_review.php?status=1'">已通过</button>
            <button class="review-filter-btn <?php echo $statusFilter === 2 ? 'active' : ''; ?>" onclick="location.href='plugin_review.php?status=2'">已拒绝</button>
            <button class="review-filter-btn <?php echo $statusFilter === 3 ? 'active' : ''; ?>" onclick="location.href='plugin_review.php?status=3'">已下架</button>
            <button class="review-filter-btn <?php echo $statusFilter === -1 ? 'active' : ''; ?>" onclick="location.href='plugin_review.php?status=-1'">全部</button>
        </div>
    </div>

    <div class="plugin-list">
        <?php if (empty($plugins)): ?>
            <div class="review-empty">暂无相关脚本</div>
        <?php else: ?>
            <?php foreach ($plugins as $plugin):
                $statusMap = [0 => ['pending', '待审核'], 1 => ['approved', '已通过'], 2 => ['rejected', '已拒绝'], 3 => ['takedown', '已下架']];
                $s = isset($statusMap[$plugin['status']]) ? $statusMap[$plugin['status']] : ['pending', '未知'];
                $statusClass = $s[0]; $statusText = $s[1];
                $desc = !empty($plugin['description']) ? $plugin['description'] : '该作者很懒，什么也没留下';
            ?>
                <div class="review-card">
                    <div class="review-header">
                        <div class="review-title"><?php echo htmlspecialchars($plugin['plugin_name']); ?></div>
                        <span class="review-id"><?php echo htmlspecialchars($plugin['plugin_id']); ?></span>
                    </div>
                    <div class="review-body">
                        <div class="review-desc"><?php echo htmlspecialchars($desc); ?></div>
                        <div class="review-info">
                            <div><strong>版本：</strong><?php echo htmlspecialchars($plugin['version_code']); ?></div>
                            <div><strong>作者：</strong><?php echo htmlspecialchars($plugin['author_name']); ?></div>
                            <div><strong>上传者：</strong><?php echo htmlspecialchars($plugin['uploader_name'] ?? $plugin['upload_qq']); ?></div>
                            <div><strong>上传时间：</strong><?php echo $plugin['upload_time']; ?></div>
                            <div><strong>下载次数：</strong><?php echo $plugin['download_count']; ?></div>
                        </div>
                        <?php if ($plugin['status'] == 2 && !empty($plugin['reject_reason'])): ?>
                            <div class="reject-reason-box">
                                <strong>拒绝原因：</strong><?php echo htmlspecialchars($plugin['reject_reason']); ?>
                            </div>
                        <?php endif; ?>
                    </div>
                    <div class="review-footer">
                        <div>
                            <span class="review-status-badge <?php echo $statusClass; ?>"><?php echo $statusText; ?></span>
                        </div>
                        <div class="review-actions">
                            <button class="review-btn review-btn-preview" onclick="previewZip(<?php echo $plugin['could_id']; ?>, '<?php echo htmlspecialchars(addslashes($plugin['plugin_name'])); ?>')">预览</button>
                            <button class="review-btn review-btn-download" onclick="downloadPlugin(<?php echo $plugin['could_id']; ?>)">下载</button>
                            <?php if ($plugin['status'] == 0): ?>
                                <button class="review-btn review-btn-approve" onclick="approvePlugin(<?php echo $plugin['could_id']; ?>)">通过</button>
                                <button class="review-btn review-btn-reject" onclick="rejectPlugin(<?php echo $plugin['could_id']; ?>)">拒绝</button>
                            <?php endif; ?>
                            <?php if ($plugin['status'] == 2 || $plugin['status'] == 3): ?>
                                <button class="review-btn review-btn-approve" onclick="approvePlugin(<?php echo $plugin['could_id']; ?>)">同意</button>
                            <?php endif; ?>
                            <?php if ($plugin['status'] == 1): ?>
                                <button class="review-btn review-btn-takedown" onclick="takedownPlugin(<?php echo $plugin['could_id']; ?>)">下架</button>
                            <?php endif; ?>
                        </div>
                    </div>
                </div>
            <?php endforeach; ?>
        <?php endif; ?>
    </div>
</div>

<!-- Reject Modal -->
<div class="review-modal-overlay" id="rejectModal">
    <div class="review-modal">
        <h3>拒绝原因</h3>
        <textarea id="rejectReason" placeholder="请输入拒绝原因（必填，最多500字）" maxlength="500"></textarea>
        <div class="review-modal-actions">
            <button class="review-modal-btn" onclick="closeRejectModal()">取消</button>
            <button class="review-modal-btn review-modal-btn-primary" onclick="confirmReject()">确认拒绝</button>
        </div>
    </div>
</div>

<!-- Zip Preview Modal -->
<div class="review-modal-overlay" id="previewModal">
    <div class="review-modal" style="max-width: 650px;">
        <h3 id="previewTitle">文件预览</h3>
        <div id="previewContent" class="zip-loading">加载中...</div>
        <div class="review-modal-actions">
            <button class="review-modal-btn" onclick="closePreviewModal()">关闭</button>
        </div>
    </div>
</div>

<input type="hidden" id="csrf_token" value="<?php echo $csrfToken; ?>">

<script src="../assets/common.js"></script>
<script>
    var currentRejectId = null;
    var currentCouldId = null;
    var previewExts = ['java', 'js', 'json', 'prop', 'txt'];

    function downloadPlugin(couldId) {
        window.open('../api/user/plugin_download.php?could_id=' + couldId, '_blank');
    }

    function approvePlugin(id) {
        if (typeof Modal !== 'undefined' && Modal.confirm) {
            Modal.confirm('确认通过该脚本？', function() { submitAction(id, 'approve'); });
        } else {
            if (confirm('确认通过该脚本？')) submitAction(id, 'approve');
        }
    }

    function takedownPlugin(id) {
        if (typeof Modal !== 'undefined' && Modal.confirm) {
            Modal.confirm('确认下架该脚本？下架后用户将无法下载。', function() { submitAction(id, 'takedown'); });
        } else {
            if (confirm('确认下架该脚本？下架后用户将无法下载。')) submitAction(id, 'takedown');
        }
    }

    function submitAction(id, action, extra) {
        showLoading();
        var body = 'action=' + action + '&could_id=' + id + '&csrf_token=' + document.getElementById('csrf_token').value;
        if (extra) body += '&' + extra;
        fetch('../api/user/plugin_review.php', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: body
        })
        .then(function(r) { return r.json(); })
        .then(function(data) {
            hideLoading();
            if (data.code === 200) {
                showToast(data.message, 'success');
                setTimeout(function() { location.reload(); }, 1000);
            } else {
                showToast(data.message, 'error');
            }
        })
        .catch(function() {
            hideLoading();
            showToast('操作失败，请稍后重试', 'error');
        });
    }

    function rejectPlugin(id) {
        currentRejectId = id;
        document.getElementById('rejectReason').value = '';
        document.getElementById('rejectModal').classList.add('show');
    }

    function closeRejectModal() {
        document.getElementById('rejectModal').classList.remove('show');
        currentRejectId = null;
    }

    function confirmReject() {
        var reason = document.getElementById('rejectReason').value.trim();
        if (!reason) { showToast('请输入拒绝原因', 'error'); return; }
        submitAction(currentRejectId, 'reject', 'reason=' + encodeURIComponent(reason));
        closeRejectModal();
    }

    function previewZip(couldId, pluginName) {
        currentCouldId = couldId;
        document.getElementById('previewTitle').textContent = pluginName + ' - 文件预览';
        document.getElementById('previewContent').innerHTML = '<div class="zip-loading">加载中...</div>';
        document.getElementById('previewModal').classList.add('show');

        fetch('../api/user/zip_preview.php?could_id=' + couldId)
        .then(function(r) { return r.json(); })
        .then(function(data) {
            if (data.code !== 200) {
                document.getElementById('previewContent').innerHTML = '<div class="zip-loading">' + (data.message || '预览失败') + '</div>';
                return;
            }
            var files = data.data.files;
            if (!files || files.length === 0) {
                document.getElementById('previewContent').innerHTML = '<div class="zip-loading">压缩包为空</div>';
                return;
            }
            var html = '<div class="zip-tree">';
            html += '<div style="margin-bottom:8px;color:#999;font-size:12px;">共 ' + data.data.total_files + ' 个条目</div>';
            for (var i = 0; i < files.length; i++) {
                var f = files[i];
                var parts = f.name.split('/');
                var indent = Math.max(0, parts.length - 1);
                var displayName = parts[parts.length - 1] || parts[parts.length - 2] || f.name;
                var pad = (indent * 20) + 'px';
                var ext = displayName.split('.').pop().toLowerCase();
                var canPreview = !f.is_dir && previewExts.indexOf(ext) >= 0;

                if (f.is_dir) {
                    html += '<div class="file-item"><span class="indent" style="width:' + pad + '"></span><span class="file-icon dir-icon">D</span><span class="file-name" style="font-weight:500;">' + escapeHtml(displayName) + '/</span></div>';
                } else {
                    var sizeStr = formatSize(f.size);
                    if (canPreview) {
                        html += '<div class="file-item"><span class="indent" style="width:' + pad + '"></span><span class="file-icon file-icon-doc">F</span><span class="file-name previewable" onclick="openFilePreview(' + currentCouldId + ', \'' + escapeAttr(f.name) + '\')">' + escapeHtml(displayName) + '</span><span class="file-size">' + sizeStr + '</span></div>';
                    } else {
                        html += '<div class="file-item"><span class="indent" style="width:' + pad + '"></span><span class="file-icon file-icon-doc">F</span><span class="file-name">' + escapeHtml(displayName) + '</span><span class="file-size">' + sizeStr + '</span></div>';
                    }
                }
            }
            html += '</div>';
            document.getElementById('previewContent').innerHTML = html;
        })
        .catch(function() {
            document.getElementById('previewContent').innerHTML = '<div class="zip-loading">请求失败</div>';
        });
    }

    function openFilePreview(couldId, fileName) {
        window.open('../online_plugin/preview.php?could_id=' + couldId + '&file=' + encodeURIComponent(fileName), '_blank');
    }

    function closePreviewModal() {
        document.getElementById('previewModal').classList.remove('show');
    }

    function formatSize(bytes) {
        if (bytes === 0) return '0 B';
        var k = 1024, sizes = ['B', 'KB', 'MB', 'GB'];
        var i = Math.floor(Math.log(bytes) / Math.log(k));
        return (bytes / Math.pow(k, i)).toFixed(1) + ' ' + sizes[i];
    }

    function escapeHtml(str) {
        var div = document.createElement('div');
        div.appendChild(document.createTextNode(str));
        return div.innerHTML;
    }

    function escapeAttr(str) {
        return str.replace(/\\/g, '/').replace(/'/g, "\\'").replace(/"/g, '&quot;');
    }

    function showToast(message, type) {
        if (typeof Toast !== 'undefined' && Toast[type]) { Toast[type](message); return; }
        var bgMap = { success: '#389e0d', error: '#cf1322', info: '#1a73e8' };
        var toast = document.createElement('div');
        toast.textContent = message;
        toast.style.cssText = 'position:fixed;top:20px;left:50%;transform:translateX(-50%);padding:10px 24px;background:' + (bgMap[type] || '#333') + ';color:#fff;border-radius:4px;font-size:14px;z-index:9999;box-shadow:0 2px 8px rgba(0,0,0,0.15);';
        document.body.appendChild(toast);
        setTimeout(function() { toast.style.opacity = '0'; toast.style.transition = 'opacity 0.3s'; setTimeout(function() { toast.remove(); }, 300); }, 2500);
    }

    function showLoading() { if (typeof Loading !== 'undefined' && Loading.show) Loading.show(); }
    function hideLoading() { if (typeof Loading !== 'undefined' && Loading.hide) Loading.hide(); }

    document.getElementById('rejectModal').addEventListener('click', function(e) { if (e.target === this) closeRejectModal(); });
    document.getElementById('previewModal').addEventListener('click', function(e) { if (e.target === this) closePreviewModal(); });
    document.addEventListener('keydown', function(e) { if (e.key === 'Escape') { closeRejectModal(); closePreviewModal(); } });
</script>

<?php require_once __DIR__ . '/footer.php'; ?>
