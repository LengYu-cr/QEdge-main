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
    if (!$user || $user['review_permission'] != 1) {
        die('您没有处理反馈的权限');
    }
}

$statusFilter = isset($_GET['status']) ? intval($_GET['status']) : -1;
if (!in_array($statusFilter, [-1, 0, 1, 2, 3])) {
    $statusFilter = -1;
}

$feedbacks = [];
if ($pdo) {
    try {
        if ($statusFilter === -1) {
            $sql = "SELECT f.could_id, f.qq, f.user_nickname, f.title, f.content, f.status, f.reply, f.reply_qq, f.reply_nickname, f.reply_time, f.create_time, f.attachment,
                           u.nickname as db_nickname
                    FROM feedbacks f LEFT JOIN users u ON f.qq COLLATE utf8mb4_unicode_ci = u.qq COLLATE utf8mb4_unicode_ci ORDER BY f.create_time DESC LIMIT 500";
            $stmt = $pdo->query($sql);
        } else {
            $sql = "SELECT f.could_id, f.qq, f.user_nickname, f.title, f.content, f.status, f.reply, f.reply_qq, f.reply_nickname, f.reply_time, f.create_time, f.attachment,
                           u.nickname as db_nickname
                    FROM feedbacks f LEFT JOIN users u ON f.qq COLLATE utf8mb4_unicode_ci = u.qq COLLATE utf8mb4_unicode_ci
                    WHERE f.status = ? ORDER BY f.create_time DESC LIMIT 500";
            $stmt = $pdo->prepare($sql);
            $stmt->execute([$statusFilter]);
        }
        $feedbacks = dbFixUtf8($stmt->fetchAll());
    } catch (Exception $e) {
        $dbError = '获取反馈列表失败: ' . $e->getMessage();
        error_log("管理员获取反馈列表失败: " . $e->getMessage());
    }
}

$stats = ['total' => 0, 'pending' => 0, 'processing' => 0, 'solved' => 0, 'closed' => 0];
if ($pdo) {
    try {
        $cnt = function($sql) use ($pdo) {
            $st = $pdo->query($sql);
            return (int)$st->fetchColumn();
        };
        $stats['total']      = $cnt("SELECT COUNT(*) FROM feedbacks");
        $stats['pending']    = $cnt("SELECT COUNT(*) FROM feedbacks WHERE status = 0");
        $stats['processing'] = $cnt("SELECT COUNT(*) FROM feedbacks WHERE status = 1");
        $stats['solved']     = $cnt("SELECT COUNT(*) FROM feedbacks WHERE status = 2");
        $stats['closed']     = $cnt("SELECT COUNT(*) FROM feedbacks WHERE status = 3");
    } catch (Exception $e) {
        $stats = ['total' => 0, 'pending' => 0, 'processing' => 0, 'solved' => 0, 'closed' => 0];
    }
}

$csrfToken = generateCSRFToken();
$pageTitle = '反馈管理';
$activeMenu = 'feedback';
require_once __DIR__ . '/header.php';
?>

<?php if ($dbError): ?>
    <div class="alert alert-danger"><?php echo htmlspecialchars($dbError); ?></div>
<?php endif; ?>

<div class="page-header">
    <h1 class="page-title">反馈管理</h1>
    <p class="page-subtitle">共 <?php echo $stats['total']; ?> 条用户反馈</p>
</div>

<div class="stats-grid">
    <div class="stat-card">
        <div class="stat-card-header">
            <span class="stat-label">总计</span>
            <div class="stat-icon primary">
                <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path><polyline points="14 2 14 8 20 8"></polyline>
                </svg>
            </div>
        </div>
        <div class="stat-value"><?php echo $stats['total']; ?></div>
        <div class="stat-label">反馈总数</div>
    </div>
    <div class="stat-card">
        <div class="stat-card-header">
            <span class="stat-label">待处理</span>
            <div class="stat-icon warning">
                <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <circle cx="12" cy="12" r="10"></circle><line x1="12" y1="8" x2="12" y2="12"></line><line x1="12" y1="16" x2="12.01" y2="16"></line>
                </svg>
            </div>
        </div>
        <div class="stat-value" style="color: var(--color-warning);"><?php echo $stats['pending']; ?></div>
        <div class="stat-label">待处理</div>
    </div>
    <div class="stat-card">
        <div class="stat-card-header">
            <span class="stat-label">处理中</span>
            <div class="stat-icon primary">
                <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <circle cx="12" cy="12" r="10"></circle><polyline points="12 6 12 12 16 14"></polyline>
                </svg>
            </div>
        </div>
        <div class="stat-value" style="color: var(--color-primary);"><?php echo $stats['processing']; ?></div>
        <div class="stat-label">处理中</div>
    </div>
    <div class="stat-card">
        <div class="stat-card-header">
            <span class="stat-label">已解决</span>
            <div class="stat-icon success">
                <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path><polyline points="22 4 12 14.01 9 11.01"></polyline>
                </svg>
            </div>
        </div>
        <div class="stat-value" style="color: var(--color-success);"><?php echo $stats['solved']; ?></div>
        <div class="stat-label">已解决</div>
    </div>
    <div class="stat-card">
        <div class="stat-card-header">
            <span class="stat-label">已关闭</span>
            <div class="stat-icon" style="background: #f5f5f5; color: #595959;">
                <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect><path d="M7 11V7a5 5 0 0 1 10 0v4"></path>
                </svg>
            </div>
        </div>
        <div class="stat-value" style="color: #595959;"><?php echo $stats['closed']; ?></div>
        <div class="stat-label">已关闭</div>
    </div>
</div>

<div class="filter-bar">
    <button class="filter-btn <?php echo $statusFilter === -1 ? 'active' : ''; ?>" onclick="location.href='feedback.php?status=-1'">全部</button>
    <button class="filter-btn <?php echo $statusFilter === 0 ? 'active' : ''; ?>" onclick="location.href='feedback.php?status=0'">待处理</button>
    <button class="filter-btn <?php echo $statusFilter === 1 ? 'active' : ''; ?>" onclick="location.href='feedback.php?status=1'">处理中</button>
    <button class="filter-btn <?php echo $statusFilter === 2 ? 'active' : ''; ?>" onclick="location.href='feedback.php?status=2'">已解决</button>
    <button class="filter-btn <?php echo $statusFilter === 3 ? 'active' : ''; ?>" onclick="location.href='feedback.php?status=3'">已关闭</button>
</div>

<!-- Reply Modal -->
<div class="modal-overlay" id="replyModalOverlay" style="display:none;">
    <div class="modal" style="max-width: 680px;">
        <div class="modal-header">
            <h3 class="modal-title" id="replyModalTitle">处理反馈</h3>
            <button class="modal-close" onclick="closeReplyModal()" aria-label="关闭">
                <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line>
                </svg>
            </button>
        </div>
        <div class="modal-body" style="padding: 18px 20px 8px;">
            <input type="hidden" id="reply_could_id" value="">
            <div style="margin-bottom: 14px;">
                <div style="font-size: 13px; color: #595959; margin-bottom: 6px;">反馈标题</div>
                <div id="reply_title" style="font-weight:550; color:#1a2b3c; padding: 8px 12px; background:#f8fcff; border-radius: 8px; border-left: 3px solid #dce8f5; line-height: 1.5;"></div>
            </div>
            <div style="display:grid; grid-template-columns: 1fr 1fr; gap: 14px; margin-bottom: 14px;">
                <div>
                    <div style="font-size: 13px; color: #595959; margin-bottom: 6px;">反馈用户</div>
                    <div id="reply_user" style="padding: 8px 12px; background:#fafafa; border-radius: 6px; font-size:13px;"></div>
                </div>
                <div>
                    <div style="font-size: 13px; color: #595959; margin-bottom: 6px;">提交时间</div>
                    <div id="reply_time" style="padding: 8px 12px; background:#fafafa; border-radius: 6px; font-size:13px;"></div>
                </div>
            </div>
            <div style="margin-bottom: 14px;">
                <div style="font-size: 13px; color: #595959; margin-bottom: 6px;">反馈内容</div>
                <div id="reply_content" style="padding: 10px 14px; background: #f8fcff; border: 1px solid #e2edf9; border-radius: 8px; line-height: 1.7; white-space: pre-wrap; word-break: break-word; color: #1f3347; max-height: 220px; overflow-y: auto;"></div>
            </div>
            <?php if (!empty($fb['reply'])): ?>
            <!-- 动态渲染 -->
            <?php endif; ?>
            <div id="reply_existing_block" style="margin-bottom: 14px; display:none;">
                <div style="font-size: 13px; color: #595959; margin-bottom: 6px;">历史回复</div>
                <div id="reply_existing" style="padding: 10px 14px; background: #f0f9ff; border: 1px solid #bae0ff; border-radius: 8px;"></div>
            </div>
            <div style="margin-bottom: 14px;">
                <label class="form-label" style="display:block; margin-bottom: 6px; font-size: 13px; color: #595959;">处理状态</label>
                <select id="reply_status" class="form-input" style="width:100%; padding: 9px 12px;">
                    <option value="0">待处理</option>
                    <option value="1">处理中</option>
                    <option value="2">已解决</option>
                    <option value="3">已关闭</option>
                </select>
            </div>
            <div>
                <label class="form-label" style="display:block; margin-bottom: 6px; font-size: 13px; color: #595959;">回复内容 <small style="color:#999;">（建议解决或关闭时建议填写，用户可在反馈中心看到）</small></label>
                <textarea id="reply_body" class="form-textarea" maxlength="2000" placeholder="请输入处理回复内容..." style="width: 100%; min-height: 120px; resize: vertical; line-height: 1.65;"></textarea>
                <div style="text-align:right; font-size: 12px; color:#999; margin-top: 4px;"><span id="reply_count">0</span>/2000</div>
            </div>
        </div>
        <div class="modal-footer">
            <button class="btn btn-secondary" onclick="closeReplyModal()">取消</button>
            <button class="btn btn-primary" onclick="submitReply()">提交处理</button>
        </div>
    </div>
</div>

<?php if (empty($feedbacks)): ?>
    <div class="card">
        <div class="card-body">
            <div class="empty-state">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" class="empty-state-icon">
                    <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path>
                </svg>
                <h3>暂无反馈记录</h3>
                <p>当前筛选条件下没有反馈</p>
            </div>
        </div>
    </div>
<?php else: ?>
    <?php
    $statusMap = [0 => ['status-pending', '待处理'], 1 => ['status-processing', '处理中'], 2 => ['status-approved', '已解决'], 3 => ['status-takedown', '已关闭']];
    $enc = function($s) { return htmlspecialchars((string)$s, ENT_QUOTES, 'UTF-8'); };
    foreach ($feedbacks as $fb):
        $s = isset($statusMap[$fb['status']]) ? $statusMap[$fb['status']] : $statusMap[0];
        $statusClass = $s[0];
        $statusText = $s[1];
        $nick = !empty($fb['db_nickname']) ? $fb['db_nickname'] : ($fb['user_nickname'] ?? $fb['qq']);
        $hasReply = !empty($fb['reply']) && trim((string)$fb['reply']) !== '';
        $contentPreview = $enc($fb['content']);
        if (mb_strlen($fb['content'], 'UTF-8') > 140) {
            $contentPreview = $enc(mb_substr($fb['content'], 0, 140, 'UTF-8')) . '...';
        }
    ?>
        <div class="plugin-review-card"
             data-could_id="<?php echo $fb['could_id']; ?>"
             data-title="<?php echo $enc($fb['title']); ?>"
             data-content="<?php echo $enc($fb['content']); ?>"
             data-qq="<?php echo $enc($fb['qq']); ?>"
             data-nickname="<?php echo $enc($nick); ?>"
             data-create_time="<?php echo $fb['create_time']; ?>"
             data-status="<?php echo (int)$fb['status']; ?>"
             data-reply="<?php echo $hasReply ? $enc($fb['reply']) : ''; ?>"
             data-reply_nickname="<?php echo $hasReply ? $enc($fb['reply_nickname'] ?? ($fb['reply_qq'] ?? '管理员')) : ''; ?>"
             data-reply_time="<?php echo $hasReply ? $enc($fb['reply_time'] ?? '') : ''; ?>">
            <div class="plugin-review-header">
                <div class="plugin-review-title"><?php echo $enc($fb['title']); ?></div>
                <span class="plugin-review-id">#<?php echo $fb['could_id']; ?></span>
            </div>
            <div style="font-size:13px;color:#666;line-height:1.65;margin-bottom:10px;padding:8px 12px;background:#fafafa;border-radius:4px;border-left:3px solid #d9d9d9;"><?php echo $contentPreview; ?></div>
            <?php if ($hasReply): ?>
                <div style="margin-bottom: 10px; padding: 8px 12px; background:#f0f9ff; border-radius:4px; border-left: 3px solid #91d5ff; color:#0958b8; font-size: 13px; line-height:1.6;">
                    <strong><?php echo $enc($fb['reply_nickname'] ?? ($fb['reply_qq'] ?? '管理员')); ?></strong> · <?php echo $enc($fb['reply_time'] ?? ''); ?><br>
                    <?php
                    $rp = $enc($fb['reply']);
                    if (mb_strlen($fb['reply'], 'UTF-8') > 100) $rp = $enc(mb_substr($fb['reply'], 0, 100, 'UTF-8')) . '...';
                    echo $rp;
                    ?>
                </div>
            <?php endif; ?>
            <div class="plugin-review-info">
                <div><strong>反馈用户：</strong><?php echo $enc($nick); ?> (QQ: <?php echo $enc($fb['qq']); ?>)</div>
                <div><strong>提交时间：</strong><?php echo $fb['create_time']; ?></div>
                <?php if ($hasReply): ?>
                    <div><strong>回复时间：</strong><?php echo $enc($fb['reply_time'] ?? ''); ?></div>
                <?php endif; ?>
            </div>
            <div class="plugin-review-footer">
                <div><span class="status-badge <?php echo $statusClass; ?>"><?php echo $statusText; ?></span></div>
                <div class="plugin-review-actions">
                    <button class="btn btn-primary btn-sm" onclick="openReplyModal(<?php echo $fb['could_id']; ?>)">查看 / 回复</button>
                </div>
            </div>
        </div>
    <?php endforeach; ?>
<?php endif; ?>

<script>
    var fbAdminCsrf = '<?php echo $csrfToken; ?>';

    function openReplyModal(couldId) {
        var card = document.querySelector('.plugin-review-card[data-could_id="' + couldId + '"]');
        if (!card) return;
        var ds = card.dataset;
        document.getElementById('reply_could_id').value = String(couldId);
        document.getElementById('reply_title').textContent = ds.title || '';
        document.getElementById('reply_user').textContent  = (ds.nickname || ds.qq || '') + ' (QQ: ' + (ds.qq || '') + ')';
        document.getElementById('reply_time').textContent  = ds.create_time || '';
        document.getElementById('reply_content').textContent = ds.content || '';
        document.getElementById('reply_status').value = ds.status || '0';
        document.getElementById('reply_body').value = '';
        var exBlock = document.getElementById('reply_existing_block');
        if (ds.reply && ds.reply !== '') {
            exBlock.style.display = 'block';
            document.getElementById('reply_existing').innerHTML =
                '<div style="font-size:12px;color:#096dd9;margin-bottom:4px;">' +
                (ds.reply_nickname || '管理员') + ' · ' + (ds.reply_time || '') + '</div>' +
                '<div style="line-height:1.7;white-space:pre-wrap;word-break:break-word;color:#1e2b3c;">' + escapeHtml(ds.reply) + '</div>';
        } else {
            exBlock.style.display = 'none';
            document.getElementById('reply_existing').innerHTML = '';
        }
        updateReplyCount();
        document.getElementById('replyModalOverlay').style.display = 'flex';
    }

    function closeReplyModal() {
        document.getElementById('replyModalOverlay').style.display = 'none';
    }

    function updateReplyCount() {
        var v = document.getElementById('reply_body');
        var c = document.getElementById('reply_count');
        if (v && c) c.textContent = v.value.length;
    }
    document.getElementById('reply_body').addEventListener('input', updateReplyCount);

    function submitReply() {
        var couldId = parseInt(document.getElementById('reply_could_id').value, 10);
        var status  = parseInt(document.getElementById('reply_status').value, 10);
        var reply   = document.getElementById('reply_body').value;
        if (!couldId || couldId <= 0) { Toast.error('反馈编号异常'); return; }
        if (isNaN(status) || status < 0 || status > 3) { Toast.error('状态不正确'); return; }

        Loading.show();
        var body = 'could_id=' + couldId + '&status=' + status + '&csrf_token=' + fbAdminCsrf;
        if (reply.trim() !== '') body += '&reply=' + encodeURIComponent(reply);

        fetch('../api/admin/feedback_reply.php', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: body
        })
        .then(function(r){ return r.json(); })
        .then(function(data) {
            Loading.hide();
            if (data.code === 200) {
                Toast.success(data.message || '处理成功');
                closeReplyModal();
                setTimeout(function(){ location.reload(); }, 800);
            } else {
                Toast.error(data.message || '处理失败');
            }
        })
        .catch(function() { Loading.hide(); Toast.error('网络错误，请稍后重试'); });
    }

    function escapeHtml(str) {
        var d = document.createElement('div');
        d.appendChild(document.createTextNode(str == null ? '' : String(str)));
        return d.innerHTML;
    }

    document.getElementById('replyModalOverlay').addEventListener('click', function(e) {
        if (e.target === this) closeReplyModal();
    });
    document.addEventListener('keydown', function(e) {
        if (e.key === 'Escape') closeReplyModal();
    });
</script>

<?php require_once __DIR__ . '/footer.php'; ?>
