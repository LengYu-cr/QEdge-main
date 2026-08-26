<?php
require_once __DIR__ . '/../require.php';
require_once __DIR__ . '/../function.php';

$pdo = getDBConnection();
$dbError = '';

if (!isAdminLoggedIn()) {
    header('Location: login.php');
    exit;
}

$adminQQ = getCurrentAdminQQ();

if ($pdo) {
    $stmt = $pdo->prepare("SELECT review_permission FROM users WHERE qq = ?");
    $stmt->execute([$adminQQ]);
    $user = $stmt->fetch();
    if ($user && $user['review_permission'] != 1) {
        die('您没有脚本审核权限');
    }
}

$statusFilter = isset($_GET['status']) ? intval($_GET['status']) : -1;
if (!in_array($statusFilter, [-1, 0, 1, 2, 3])) {
    $statusFilter = -1;
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
        // 关键：解决历史数据乱码。取出后立即dbFixUtf8，还原所有字符串字段的乱码（plugin_name/author_name/description等）
        $plugins = dbFixUtf8($plugins);
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
$activeMenu = 'plugins';
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
    <h1 class="page-title">脚本审核管理</h1>
    <p class="page-subtitle">共 <?php echo $stats['total']; ?> 个脚本</p>
</div>

<div class="stats-grid">
    <div class="stat-card">
        <div class="stat-card-header">
            <span class="stat-label">总计</span>
            <div class="stat-icon primary">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="20" height="20">
                    <path d="M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.77-3.77a6 6 0 0 1-7.94 7.94l-6.91 6.91a2.12 2.12 0 0 1-3-3l6.91-6.91a6 6 0 0 1 7.94-7.94l-3.76 3.76z"></path>
                </svg>
            </div>
        </div>
        <div class="stat-value"><?php echo $stats['total']; ?></div>
        <div class="stat-label">脚本总数</div>
    </div>
    <div class="stat-card">
        <div class="stat-card-header">
            <span class="stat-label">待审核</span>
            <div class="stat-icon warning">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="20" height="20">
                    <circle cx="12" cy="12" r="10"></circle><line x1="12" y1="8" x2="12" y2="12"></line><line x1="12" y1="16" x2="12.01" y2="16"></line>
                </svg>
            </div>
        </div>
        <div class="stat-value" style="color: var(--color-warning);"><?php echo $stats['pending']; ?></div>
        <div class="stat-label">待审核脚本</div>
    </div>
    <div class="stat-card">
        <div class="stat-card-header">
            <span class="stat-label">已通过</span>
            <div class="stat-icon success">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="20" height="20">
                    <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path><polyline points="22 4 12 14.01 9 11.01"></polyline>
                </svg>
            </div>
        </div>
        <div class="stat-value" style="color: var(--color-success);"><?php echo $stats['approved']; ?></div>
        <div class="stat-label">已通过审核</div>
    </div>
    <div class="stat-card">
        <div class="stat-card-header">
            <span class="stat-label">已拒绝</span>
            <div class="stat-icon danger">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="20" height="20">
                    <circle cx="12" cy="12" r="10"></circle><line x1="15" y1="9" x2="9" y2="15"></line><line x1="9" y1="9" x2="15" y2="15"></line>
                </svg>
            </div>
        </div>
        <div class="stat-value" style="color: var(--color-danger);"><?php echo $stats['rejected']; ?></div>
        <div class="stat-label">已拒绝脚本</div>
    </div>
    <div class="stat-card">
        <div class="stat-card-header">
            <span class="stat-label">已下架</span>
            <div class="stat-icon" style="background: #f5f5f5; color: #595959;">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="20" height="20">
                    <path d="M13 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V9z"></path><polyline points="13 2 13 9 20 9"></polyline>
                </svg>
            </div>
        </div>
        <div class="stat-value" style="color: #595959;"><?php echo $stats['takedown']; ?></div>
        <div class="stat-label">已下架脚本</div>
    </div>
</div>

<div class="filter-bar">
    <button class="filter-btn <?php echo $statusFilter === -1 ? 'active' : ''; ?>" onclick="location.href='plugins.php?status=-1'">全部</button>
    <button class="filter-btn <?php echo $statusFilter === 0 ? 'active' : ''; ?>" onclick="location.href='plugins.php?status=0'">待审核</button>
    <button class="filter-btn <?php echo $statusFilter === 1 ? 'active' : ''; ?>" onclick="location.href='plugins.php?status=1'">已通过</button>
    <button class="filter-btn <?php echo $statusFilter === 2 ? 'active' : ''; ?>" onclick="location.href='plugins.php?status=2'">已拒绝</button>
    <button class="filter-btn <?php echo $statusFilter === 3 ? 'active' : ''; ?>" onclick="location.href='plugins.php?status=3'">已下架</button>
</div>

<!-- Zip Preview Modal -->
<div class="modal-overlay" id="previewModalOverlay" style="display:none;">
    <div class="modal" style="max-width: 650px;">
        <div class="modal-header">
            <h3 class="modal-title" id="previewTitle">文件预览</h3>
            <button class="modal-close" onclick="closePreviewModal()">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="18" height="18">
                    <line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line>
                </svg>
            </button>
        </div>
        <div class="modal-body" id="previewContent" style="padding:16px;">
            <div style="text-align:center;padding:30px;color:#999;">加载中...</div>
        </div>
    </div>
</div>

<!-- Plugin Edit Modal -->
<div class="modal-overlay" id="editModalOverlay" style="display:none;">
    <div class="modal" style="max-width: 560px;">
        <div class="modal-header">
            <h3 class="modal-title" id="editModalTitle">编辑脚本信息</h3>
            <button class="modal-close" onclick="closeEditModal()">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="18" height="18">
                    <line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line>
                </svg>
            </button>
        </div>
        <div class="modal-body" style="padding: 18px 20px 8px;">
            <input type="hidden" id="edit_could_id" value="">
            <div class="form-group" style="margin-bottom:14px;">
                <label class="form-label" style="display:block;margin-bottom:6px;font-size:13px;color:#595959;">脚本名称 <span style="color:#ff4d4f;">*</span></label>
                <input type="text" id="edit_plugin_name" class="form-input" placeholder="例：示例脚本" maxlength="100" style="width:100%;">
            </div>
            <div class="form-row" style="display:grid;grid-template-columns:1fr 1fr;gap:14px;margin-bottom:14px;">
                <div class="form-group">
                    <label class="form-label" style="display:block;margin-bottom:6px;font-size:13px;color:#595959;">脚本ID <span style="color:#ff4d4f;">*</span></label>
                    <input type="text" id="edit_plugin_id" class="form-input" placeholder="例：example_xxx" maxlength="80" style="width:100%;">
                </div>
                <div class="form-group">
                    <label class="form-label" style="display:block;margin-bottom:6px;font-size:13px;color:#595959;">版本号 <span style="color:#ff4d4f;">*</span></label>
                    <input type="text" id="edit_version_code" class="form-input" placeholder="例：1.0" maxlength="50" style="width:100%;">
                </div>
            </div>
            <div class="form-group" style="margin-bottom:14px;">
                <label class="form-label" style="display:block;margin-bottom:6px;font-size:13px;color:#595959;">作者 <span style="color:#ff4d4f;">*</span></label>
                <input type="text" id="edit_author_name" class="form-input" placeholder="例：作者名" maxlength="50" style="width:100%;">
            </div>
            <div class="form-group" style="margin-bottom:12px;">
                <label class="form-label" style="display:block;margin-bottom:6px;font-size:13px;color:#595959;">介绍 <small style="color:#999;">（留空会用默认文案）</small></label>
                <textarea id="edit_description" class="form-textarea" placeholder="请输入脚本介绍" maxlength="2000"
                          style="width:100%;min-height:120px;resize:vertical;line-height:1.6;"></textarea>
                <div style="text-align:right;font-size:12px;color:#999;margin-top:4px;"><span id="edit_desc_count">0</span>/2000</div>
            </div>
        </div>
        <div class="modal-footer" style="border-top:1px solid #f0f0f0;padding: 12px 20px;">
            <button class="btn btn-secondary" onclick="closeEditModal()">取消</button>
            <button class="btn btn-primary" onclick="submitEdit()">保存修改</button>
        </div>
    </div>
</div>

<?php if (empty($plugins)): ?>
    <div class="card">
        <div class="card-body">
            <div class="empty-state">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" class="empty-state-icon">
                    <path d="M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.77-3.77a6 6 0 0 1-7.94 7.94l-6.91 6.91a2.12 2.12 0 0 1-3-3l6.91-6.91a6 6 0 0 1 7.94-7.94l-3.76 3.76z"></path>
                </svg>
                <h3>暂无相关插件</h3>
                <p>当前筛选条件下没有插件</p>
            </div>
        </div>
    </div>
<?php else: ?>
    <?php foreach ($plugins as $plugin):
        $statusMap = [0 => ['status-pending', '待审核'], 1 => ['status-approved', '已通过'], 2 => ['status-rejected', '已拒绝'], 3 => ['status-takedown', '已下架']];
        $s = isset($statusMap[$plugin['status']]) ? $statusMap[$plugin['status']] : ['status-pending', '未知'];
        $statusClass = $s[0]; $statusText = $s[1];
        $desc = !empty($plugin['description']) ? $plugin['description'] : '该作者很懒，什么也没留下';
        // HTML 属性中存原始字段值供编辑弹窗使用（经过 htmlspecialchars 防注入）
        $enc = function($s) { return htmlspecialchars((string)$s, ENT_QUOTES, 'UTF-8'); };
    ?>
        <div class="plugin-review-card"
             data-could_id="<?php echo $plugin['could_id']; ?>"
             data-plugin_name="<?php echo $enc($plugin['plugin_name']); ?>"
             data-plugin_id="<?php echo $enc($plugin['plugin_id']); ?>"
             data-version_code="<?php echo $enc($plugin['version_code']); ?>"
             data-author_name="<?php echo $enc($plugin['author_name']); ?>"
             data-description="<?php echo $enc(!empty($plugin['description']) ? $plugin['description'] : ''); ?>">
            <div class="plugin-review-header">
                <div class="plugin-review-title"><?php echo htmlspecialchars($plugin['plugin_name']); ?></div>
                <span class="plugin-review-id">ID: <?php echo htmlspecialchars($plugin['plugin_id']); ?> (Could: <?php echo $plugin['could_id']; ?>)</span>
            </div>
            <div style="font-size:13px;color:#666;line-height:1.6;margin-bottom:10px;padding:8px 12px;background:#fafafa;border-radius:4px;border-left:3px solid #d9d9d9;word-break:break-all;overflow-wrap:break-word;white-space:pre-wrap;"><?php echo htmlspecialchars($desc); ?></div>
            <div class="plugin-review-info">
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
            <div class="plugin-review-footer">
                <div>
                    <span class="status-badge <?php echo $statusClass; ?>"><?php echo $statusText; ?></span>
                </div>
                <div class="plugin-review-actions">
                    <button class="btn btn-sm" style="border-color:#597ef7;color:#597ef7;background:#f0f5ff;" onclick="openEditModal(<?php echo $plugin['could_id']; ?>, this)">编辑</button>
                    <button class="btn btn-secondary btn-sm" onclick="previewZip(<?php echo $plugin['could_id']; ?>, '<?php echo htmlspecialchars(addslashes($plugin['plugin_name'])); ?>')">预览</button>
                    <button class="btn btn-secondary btn-sm" onclick="downloadPlugin(<?php echo $plugin['could_id']; ?>)">下载</button>
                    <?php if ($plugin['status'] == 0): ?>
                        <button class="btn btn-success btn-sm" onclick="approvePlugin(<?php echo $plugin['could_id']; ?>)">通过</button>
                        <button class="btn btn-danger btn-sm" onclick="rejectPlugin(<?php echo $plugin['could_id']; ?>)">拒绝</button>
                    <?php endif; ?>
                    <?php if ($plugin['status'] == 2 || $plugin['status'] == 3): ?>
                        <button class="btn btn-success btn-sm" onclick="approvePlugin(<?php echo $plugin['could_id']; ?>)">同意</button>
                    <?php endif; ?>
                    <?php if ($plugin['status'] == 1): ?>
                        <button class="btn btn-sm" style="border-color:#8c8c8c;color:#595959;" onclick="takedownPlugin(<?php echo $plugin['could_id']; ?>)">下架</button>
                    <?php endif; ?>
                    <button class="btn btn-danger btn-sm" onclick="deletePlugin(<?php echo $plugin['could_id']; ?>, '<?php echo htmlspecialchars($plugin['plugin_name']); ?>')">删除</button>
                </div>
            </div>
        </div>
    <?php endforeach; ?>
<?php endif; ?>

<script>
    var csrfToken = '<?php echo $csrfToken; ?>';
    var currentRejectId = null;
    var previewExts = ['java', 'js', 'json', 'prop', 'txt'];

    function approvePlugin(id) {
        Modal.confirm('确认通过该脚本？', function() { submitAction(id, 'approve'); });
    }

    function takedownPlugin(id) {
        Modal.confirm('确认下架该脚本？下架后用户将无法下载。', function() { submitAction(id, 'takedown'); });
    }

    function submitAction(id, action, extra) {
        Loading.show();
        var body = 'action=' + action + '&could_id=' + id + '&csrf_token=' + csrfToken;
        if (extra) body += '&' + extra;
        fetch('../api/admin/plugin_review.php', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: body
        })
        .then(function(r) { return r.json(); })
        .then(function(data) {
            Loading.hide();
            if (data.code === 200) {
                Toast.success(data.message);
                setTimeout(function() { location.reload(); }, 1000);
            } else {
                Toast.error(data.message);
            }
        })
        .catch(function() {
            Loading.hide();
            Toast.error('操作失败，请稍后重试');
        });
    }

    function rejectPlugin(id) {
        currentRejectId = id;
        showRejectModal();
    }

    function showRejectModal() {
        var overlay = document.createElement('div');
        overlay.className = 'modal-overlay';
        overlay.id = 'rejectModalOverlay';
        overlay.innerHTML = '<div class="modal"><div class="modal-header"><h3 class="modal-title">拒绝原因</h3><button class="modal-close" onclick="closeRejectModal()"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="18" height="18"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg></button></div><div class="modal-body"><textarea id="rejectReason" class="form-textarea" placeholder="请输入拒绝原因（必填）" maxlength="500" style="width:100%;"></textarea></div><div class="modal-footer"><button class="btn btn-secondary" onclick="closeRejectModal()">取消</button><button class="btn btn-primary" onclick="confirmReject()">确认拒绝</button></div></div>';
        document.body.appendChild(overlay);
        overlay.addEventListener('click', function(e) { if (e.target === overlay) closeRejectModal(); });
        var handleEsc = function(e) { if (e.key === 'Escape') { closeRejectModal(); document.removeEventListener('keydown', handleEsc); } };
        document.addEventListener('keydown', handleEsc);
    }

    function closeRejectModal() {
        var overlay = document.getElementById('rejectModalOverlay');
        if (overlay) overlay.remove();
        currentRejectId = null;
    }

    function confirmReject() {
        var reason = document.getElementById('rejectReason').value.trim();
        if (!reason) { Toast.error('请输入拒绝原因'); return; }
        submitAction(currentRejectId, 'reject', 'reason=' + encodeURIComponent(reason));
        closeRejectModal();
    }

    function previewZip(couldId, pluginName) {
        document.getElementById('previewTitle').textContent = pluginName + ' - 文件预览';
        document.getElementById('previewContent').innerHTML = '<div style="text-align:center;padding:30px;color:#999;">加载中...</div>';
        document.getElementById('previewModalOverlay').style.display = 'flex';

        fetch('../api/user/zip_preview.php?could_id=' + couldId)
        .then(function(r) { return r.json(); })
        .then(function(data) {
            if (data.code !== 200) {
                document.getElementById('previewContent').innerHTML = '<div style="text-align:center;padding:30px;color:#999;">' + (data.message || '预览失败') + '</div>';
                return;
            }
            var files = data.data.files;
            if (!files || files.length === 0) {
                document.getElementById('previewContent').innerHTML = '<div style="text-align:center;padding:30px;color:#999;">压缩包为空</div>';
                return;
            }
            var style = '<style>.zip-tree{font-family:Consolas,Monaco,monospace;font-size:13px;line-height:1.8;background:#fafafa;border:1px solid #e8e8e8;border-radius:4px;padding:14px;max-height:400px;overflow-y:auto}.zip-tree .file-item{display:flex;align-items:center;gap:6px;padding:1px 0}.zip-tree .file-icon{flex-shrink:0;width:16px;text-align:center;font-size:12px}.zip-tree .file-name{color:#333;word-break:break-all}.zip-tree .file-name.previewable{color:#1a73e8;cursor:pointer;text-decoration:underline}.zip-tree .file-size{color:#999;font-size:12px;margin-left:auto;white-space:nowrap}.zip-tree .dir-icon{color:#d4880f}.zip-tree .file-icon-doc{color:#1a73e8}.zip-tree .indent{display:inline-block}</style>';
            var html = style + '<div class="zip-tree">';
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
                        html += '<div class="file-item"><span class="indent" style="width:' + pad + '"></span><span class="file-icon file-icon-doc">F</span><span class="file-name previewable" onclick="openFilePreview(' + couldId + ', \'' + escapeAttr(f.name) + '\')">' + escapeHtml(displayName) + '</span><span class="file-size">' + sizeStr + '</span></div>';
                    } else {
                        html += '<div class="file-item"><span class="indent" style="width:' + pad + '"></span><span class="file-icon file-icon-doc">F</span><span class="file-name">' + escapeHtml(displayName) + '</span><span class="file-size">' + sizeStr + '</span></div>';
                    }
                }
            }
            html += '</div>';
            document.getElementById('previewContent').innerHTML = html;
        })
        .catch(function() {
            document.getElementById('previewContent').innerHTML = '<div style="text-align:center;padding:30px;color:#999;">请求失败</div>';
        });
    }

    function openFilePreview(couldId, fileName) {
        window.open('../online_plugin/preview.php?could_id=' + couldId + '&file=' + encodeURIComponent(fileName), '_blank');
    }

    function closePreviewModal() {
        document.getElementById('previewModalOverlay').style.display = 'none';
    }

    function downloadPlugin(couldId) {
        window.open('../api/user/plugin_download.php?could_id=' + couldId, '_blank');
    }

    function deletePlugin(id, name) {
        Modal.confirm('确定要删除插件 "' + name + '" 吗？此操作不可恢复！', function() { submitDelete(id); });
    }

    function submitDelete(id) {
        Loading.show();
        fetch('../api/admin/plugin_delete.php', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: 'could_id=' + id + '&csrf_token=' + csrfToken
        })
        .then(function(r) { return r.json(); })
        .then(function(data) {
            Loading.hide();
            if (data.code === 200) { Toast.success(data.message); setTimeout(function() { location.reload(); }, 1000); }
            else { Toast.error(data.message); }
        })
        .catch(function() { Loading.hide(); Toast.error('删除失败，请稍后重试'); });
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

    // ================ 插件编辑相关 ================
    function openEditModal(couldId, btnEl) {
        var card = btnEl ? btnEl.closest('.plugin-review-card')
                         : document.querySelector('.plugin-review-card[data-could_id="' + couldId + '"]');
        if (!card) { Toast.error('找不到插件信息'); return; }
        var ds = card.dataset;
        document.getElementById('edit_could_id').value      = couldId;
        document.getElementById('edit_plugin_name').value  = ds.plugin_name  || '';
        document.getElementById('edit_plugin_id').value    = ds.plugin_id    || '';
        document.getElementById('edit_version_code').value = ds.version_code || '';
        document.getElementById('edit_author_name').value  = ds.author_name  || '';
        var descEl = document.getElementById('edit_description');
        descEl.value = ds.description || '';
        updateDescCount();
        document.getElementById('editModalTitle').textContent = '编辑脚本信息 - ' + (ds.plugin_name || '');
        document.getElementById('editModalOverlay').style.display = 'flex';
    }

    function closeEditModal() {
        document.getElementById('editModalOverlay').style.display = 'none';
    }

    function updateDescCount() {
        var el = document.getElementById('edit_description');
        var cntEl = document.getElementById('edit_desc_count');
        if (el && cntEl) cntEl.textContent = el.value.length;
    }

    function submitEdit() {
        var couldId     = parseInt(document.getElementById('edit_could_id').value, 10);
        var pluginName  = document.getElementById('edit_plugin_name').value.trim();
        var pluginId    = document.getElementById('edit_plugin_id').value.trim();
        var versionCode = document.getElementById('edit_version_code').value.trim();
        var authorName  = document.getElementById('edit_author_name').value.trim();
        var description = document.getElementById('edit_description').value; // 不要trim，允许介绍首尾换行/空格

        if (!couldId || couldId <= 0)  { Toast.error('插件ID异常'); return; }
        if (!pluginName)               { Toast.error('脚本名称不能为空'); document.getElementById('edit_plugin_name').focus(); return; }
        if (!pluginId)                 { Toast.error('脚本ID不能为空');   document.getElementById('edit_plugin_id').focus(); return; }
        if (!versionCode)              { Toast.error('版本号不能为空');    document.getElementById('edit_version_code').focus(); return; }
        if (!authorName)               { Toast.error('作者名称不能为空');  document.getElementById('edit_author_name').focus(); return; }

        Loading.show();
        var body = 'could_id=' + couldId
                 + '&csrf_token=' + csrfToken
                 + '&plugin_name='  + encodeURIComponent(pluginName)
                 + '&plugin_id='    + encodeURIComponent(pluginId)
                 + '&version_code=' + encodeURIComponent(versionCode)
                 + '&author_name='  + encodeURIComponent(authorName)
                 + '&description='  + encodeURIComponent(description);

        fetch('../api/admin/plugin_edit.php', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: body
        })
        .then(function(r) { return r.json(); })
        .then(function(data) {
            Loading.hide();
            if (data.code === 200) {
                Toast.success(data.message || '修改成功');
                closeEditModal();
                setTimeout(function() { location.reload(); }, 900);
            } else {
                Toast.error(data.message || '修改失败');
            }
        })
        .catch(function() { Loading.hide(); Toast.error('操作失败，请稍后重试'); });
    }

    // 监听：介绍textarea字数统计 + ESC关闭编辑弹窗
    document.addEventListener('DOMContentLoaded', function() {
        var descEl = document.getElementById('edit_description');
        if (descEl) descEl.addEventListener('input', updateDescCount);
        document.addEventListener('keydown', function(e) {
            if (e.key === 'Escape' &&
                document.getElementById('editModalOverlay').style.display === 'flex') {
                closeEditModal();
            }
        });
    });

    document.getElementById('previewModalOverlay').addEventListener('click', function(e) { if (e.target === this) closePreviewModal(); });
    document.getElementById('editModalOverlay').addEventListener('click', function(e)    { if (e.target === this) closeEditModal(); });
</script>

<?php require_once __DIR__ . '/footer.php'; ?>
