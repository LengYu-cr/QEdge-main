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

$statusFilter = isset($_GET['status']) ? intval($_GET['status']) : -1;
if (!in_array($statusFilter, [-1, 0, 1, 2, 3])) {
    $statusFilter = -1;
}

$feedbacks = [];
if ($pdo) {
    try {
        if ($statusFilter === -1) {
            $stmt = $pdo->prepare("SELECT could_id, title, content, status, reply, reply_qq, reply_nickname, reply_time, create_time
                                    FROM feedbacks WHERE qq COLLATE utf8mb4_unicode_ci = ? ORDER BY create_time DESC");
            $stmt->execute([$userQQ]);
        } else {
            $stmt = $pdo->prepare("SELECT could_id, title, content, status, reply, reply_qq, reply_nickname, reply_time, create_time
                                    FROM feedbacks WHERE qq COLLATE utf8mb4_unicode_ci = ? AND status = ? ORDER BY create_time DESC");
            $stmt->execute([$userQQ, $statusFilter]);
        }
        $feedbacks = dbFixUtf8($stmt->fetchAll());
    } catch (Exception $e) {
        $dbError = '获取反馈列表失败: ' . $e->getMessage();
        error_log("获取用户反馈列表失败: " . $e->getMessage());
    }
}

$stats = ['total' => 0, 'pending' => 0, 'processing' => 0, 'solved' => 0, 'closed' => 0];
if ($pdo) {
    try {
        // 使用 fetchColumn 写法，COLLATE 显式统一，避免 1267 Illegal mix of collations
        $cnt = function($sql) use ($pdo, $userQQ) {
            $st = $pdo->prepare($sql);
            $st->execute([$userQQ]);
            return (int)$st->fetchColumn();
        };
        $stats['total']      = $cnt("SELECT COUNT(*) FROM feedbacks WHERE qq COLLATE utf8mb4_unicode_ci = ?");
        $stats['pending']    = $cnt("SELECT COUNT(*) FROM feedbacks WHERE qq COLLATE utf8mb4_unicode_ci = ? AND status = 0");
        $stats['processing'] = $cnt("SELECT COUNT(*) FROM feedbacks WHERE qq COLLATE utf8mb4_unicode_ci = ? AND status = 1");
        $stats['solved']     = $cnt("SELECT COUNT(*) FROM feedbacks WHERE qq COLLATE utf8mb4_unicode_ci = ? AND status = 2");
        $stats['closed']     = $cnt("SELECT COUNT(*) FROM feedbacks WHERE qq COLLATE utf8mb4_unicode_ci = ? AND status = 3");
    } catch (Exception $e) {
        $stats = ['total' => 0, 'pending' => 0, 'processing' => 0, 'solved' => 0, 'closed' => 0];
    }
}

$csrfToken = generateCSRFToken();
$pageTitle = '反馈中心';
?>

<?php require_once __DIR__ . '/header.php'; ?>

<style>
    .fb-wrapper { max-width: 1080px; margin: 0 auto; }
    .fb-stat-grid { display:grid; grid-template-columns: repeat(5, 1fr); gap: 12px; margin: 14px 0 18px; }
    .fb-stat { padding:14px 10px; text-align:center; background:#fff; border-radius:12px; border:1px solid #e6eef9; border-top: 3px solid #ccc; }
    .fb-stat .num { font-size: 22px; font-weight: 550; color: #1c3450; line-height: 1.2; }
    .fb-stat .lbl { font-size: 12px; color: #3f5b77; margin-top: 4px; }
    .fb-stat.s-pending    { border-top-color: #faad14; } .fb-stat.s-pending .num    { color: #d4880f; }
    .fb-stat.s-processing { border-top-color: #1890ff; } .fb-stat.s-processing .num { color: #096dd9; }
    .fb-stat.s-solved     { border-top-color: #52c41a; } .fb-stat.s-solved .num     { color: #389e0d; }
    .fb-stat.s-closed     { border-top-color: #8c8c8c; } .fb-stat.s-closed .num     { color: #595959; }
    .fb-stat.s-total      { border-top-color: #597ef7; } .fb-stat.s-total .num      { color: #2f54eb; }
    .fb-filter { display:flex; flex-wrap:wrap; gap: 8px; margin: 6px 0 14px; }
    .fb-filter-btn { padding: 7px 18px; border: 1px solid #d9e8f7; background: #fff; border-radius: 40px; cursor:pointer; font-size:13px; color: #3f5b77; transition: all .2s; }
    .fb-filter-btn.active { background: #1890ff; border-color: #1890ff; color: #fff; }
    .fb-filter-btn:hover:not(.active) { border-color: #9bbde0; color: #1e2b3c; }
    .fb-card { background: #fff; border-radius: 16px; border: 1px solid #e6eef9; padding: 16px 18px; margin-bottom: 12px; transition: all .2s; cursor: pointer; }
    .fb-card:hover { border-color: #b8d0e8; box-shadow: 0 2px 10px rgba(190,212,235,.12); }
    .fb-card-head { display:flex; flex-wrap:wrap; justify-content:space-between; align-items:center; gap: 8px; margin-bottom: 8px; }
    .fb-card-title { font-size: 15px; font-weight: 550; color: #1c3450; word-break: break-word; line-height: 1.4; flex:1; min-width: 0; padding-right: 10px; }
    .fb-status { display:inline-block; padding: 2px 12px; border-radius: 30px; font-size: 12px; font-weight: 500; white-space: nowrap; border: 1px solid transparent; }
    .fb-status.pending    { background:#fff7e6; color:#d4880f; border-color:#ffe58f; }
    .fb-status.processing { background:#e6f7ff; color:#096dd9; border-color:#91d5ff; }
    .fb-status.solved     { background:#f6ffed; color:#389e0d; border-color:#b7eb8f; }
    .fb-status.closed     { background:#f5f5f5; color:#595959; border-color:#d9d9d9; }
    .fb-card-meta { display:flex; flex-wrap:wrap; gap: 14px; font-size: 12px; color: #6b8aa9; margin-bottom: 8px; }
    .fb-card-preview { font-size: 13px; color: #3f5b77; line-height: 1.65; background:#f8fcff; border-radius: 8px; padding: 8px 12px; border-left: 3px solid #dce8f5;
                       display:-webkit-box; -webkit-line-clamp:2; -webkit-box-orient:vertical; overflow:hidden; }
    .fb-reply-badge { margin-top: 10px; padding: 8px 12px; border-radius: 8px; background: #f0f5ff; border: 1px solid #d6e4ff; font-size: 12px; color: #2f54eb; }
    .fb-form-grid { display:grid; gap: 12px; }
    .fb-form-label { display:block; font-size:13px; color:#3f5b77; margin-bottom: 6px; }
    .fb-form-title { width:100%; padding: 10px 12px; border:1px solid #d9e8f7; border-radius: 8px; font-size: 14px; box-sizing: border-box; color:#1e2b3c; background:#fff; }
    .fb-form-title:focus, .fb-form-content:focus { outline:none; border-color:#9bbde0; box-shadow: 0 0 0 3px rgba(155,189,224,.15); }
    .fb-form-content { width:100%; min-height: 140px; padding: 10px 12px; border:1px solid #d9e8f7; border-radius: 8px; font-size:14px; box-sizing: border-box; resize: vertical; line-height: 1.65; color:#1e2b3c; background:#fff; font-family: inherit; }
    .fb-count { text-align:right; font-size: 12px; color:#6b8aa9; margin-top: 4px; }
    .fb-empty { text-align:center; padding: 40px 20px; color:#6b8aa9; font-size: 14px; }
    .fb-modal-overlay { display:none; position:fixed; inset:0; background: rgba(10, 30, 60, 0.45); z-index: 1000; justify-content:center; align-items:flex-start; padding: 24px 20px; overflow-y:auto; }
    .fb-modal-overlay.show { display:flex; }
    .fb-modal { background:#fff; border-radius: 16px; width:100%; max-width: 640px; border: 1px solid #e6eef9; box-shadow: 0 8px 30px rgba(155,189,224,.2); }
    .fb-modal-head { padding: 16px 20px; border-bottom: 1px solid #eef4fb; display:flex; justify-content:space-between; align-items:center; gap: 10px; }
    .fb-modal-head h3 { margin:0; font-size: 16px; color: #1c3450; font-weight: 550; line-height: 1.4; flex:1; word-break: break-word; }
    .fb-modal-close { border:0; background: transparent; cursor:pointer; padding: 6px; border-radius: 8px; color:#6b8aa9; display:flex; align-items:center; }
    .fb-modal-close:hover { background: #f0f6fe; color: #1e2b3c; }
    .fb-modal-body { padding: 18px 20px; font-size: 14px; line-height: 1.7; color: #1f3347; }
    .fb-meta-row { display:flex; flex-wrap:wrap; gap: 16px; font-size:12px; color:#6b8aa9; margin-bottom: 14px; padding-bottom: 12px; border-bottom: 1px dashed #e2edf9; }
    .fb-detail-content { padding: 12px 14px; background:#f8fcff; border-radius: 10px; border-left: 3px solid #dce8f5; white-space: pre-wrap; word-break: break-word; }
    .fb-detail-reply { margin-top: 18px; padding: 12px 14px; background: #f0f9ff; border-radius: 10px; border: 1px solid #bae0ff; }
    .fb-detail-reply .h { font-size: 13px; color: #096dd9; margin-bottom: 6px; font-weight: 550; }
    .fb-detail-reply .t { font-size: 12px; color: #6b8aa9; margin-bottom: 6px; }
    .fb-detail-reply .c { white-space: pre-wrap; word-break: break-word; color:#1e2b3c; line-height:1.7; font-size: 14px; }
    .fb-modal-foot { padding: 12px 20px 18px; border-top: 1px solid #eef4fb; text-align:right; }
    @media (max-width: 680px) {
        .fb-stat-grid { grid-template-columns: repeat(2, 1fr); }
        .fb-filter-btn { flex: 1; text-align: center; padding: 7px 10px; }
        .fb-modal { border-radius: 14px; }
    }
    @media (max-width: 420px) {
        .fb-stat-grid { grid-template-columns: 1fr 1fr; gap: 8px; }
        .fb-card { padding: 12px 14px; border-radius: 14px; }
    }
</style>

<?php if ($dbError): ?>
    <div class="alert alert-danger"><?php echo htmlspecialchars($dbError); ?></div>
<?php endif; ?>

<div class="fb-wrapper">

    <!-- 提交新反馈 -->
    <div class="card" style="margin-bottom: 18px;">
        <div class="card-header">
            <h3 class="card-title">提交新反馈</h3>
            <small style="color:#6b8aa9;">遇到任何问题都可以在这里反馈，我们会尽快处理回复</small>
        </div>
        <div class="card-body">
            <form id="fbForm" class="fb-form-grid">
                <div>
                    <label class="fb-form-label">标题 <span style="color:#ff4d4f;">*</span> <span style="color:#6b8aa9;font-weight:normal;">（一句话描述问题，≤ 200 字）</span></label>
                    <input type="text" id="fbTitle" class="fb-form-title" maxlength="200" placeholder="例：脚本下载后无法加载 / 在线脚本列表打开失败 / 建议增加xxx功能">
                </div>
                <div>
                    <label class="fb-form-label">详细内容 <span style="color:#ff4d4f;">*</span> <span style="color:#6b8aa9;font-weight:normal;">（≤ 3000 字）</span></label>
                    <textarea id="fbContent" class="fb-form-content" maxlength="3000" placeholder="请详细描述遇到的问题：&#10;1) 操作步骤（如：进入用户中心-点击xxx）&#10;2) 预期结果 / 实际结果&#10;3) 其他补充信息（QQ版本、模块版本、机型等）"></textarea>
                    <div class="fb-count"><span id="fbCount">0</span>/3000</div>
                </div>
                <div>
                    <button type="submit" class="btn btn-primary" id="fbSubmit">提交反馈</button>
                </div>
            </form>
        </div>
    </div>

    <!-- 我的反馈统计 -->
    <div class="card" style="margin-bottom: 18px;">
        <div class="card-header">
            <h3 class="card-title">我的反馈</h3>
            <small style="color:#6b8aa9;">共 <?php echo $stats['total']; ?> 条记录</small>
        </div>
        <div class="card-body">
            <div class="fb-stat-grid">
                <div class="fb-stat s-total">      <div class="num"><?php echo $stats['total']; ?></div>      <div class="lbl">全部</div></div>
                <div class="fb-stat s-pending">    <div class="num"><?php echo $stats['pending']; ?></div>    <div class="lbl">待处理</div></div>
                <div class="fb-stat s-processing"> <div class="num"><?php echo $stats['processing']; ?></div> <div class="lbl">处理中</div></div>
                <div class="fb-stat s-solved">     <div class="num"><?php echo $stats['solved']; ?></div>     <div class="lbl">已解决</div></div>
                <div class="fb-stat s-closed">     <div class="num"><?php echo $stats['closed']; ?></div>     <div class="lbl">已关闭</div></div>
            </div>
            <div class="fb-filter">
                <button class="fb-filter-btn <?php echo $statusFilter === -1 ? 'active' : ''; ?>" onclick="location.href='feedback.php?status=-1'">全部</button>
                <button class="fb-filter-btn <?php echo $statusFilter === 0  ? 'active' : ''; ?>" onclick="location.href='feedback.php?status=0'">待处理</button>
                <button class="fb-filter-btn <?php echo $statusFilter === 1  ? 'active' : ''; ?>" onclick="location.href='feedback.php?status=1'">处理中</button>
                <button class="fb-filter-btn <?php echo $statusFilter === 2  ? 'active' : ''; ?>" onclick="location.href='feedback.php?status=2'">已解决</button>
                <button class="fb-filter-btn <?php echo $statusFilter === 3  ? 'active' : ''; ?>" onclick="location.href='feedback.php?status=3'">已关闭</button>
            </div>

            <?php if (empty($feedbacks)): ?>
                <div class="fb-empty">当前筛选条件下没有反馈记录</div>
            <?php else: ?>
                <?php
                $statusMap = [0 => ['pending', '待处理'], 1 => ['processing', '处理中'], 2 => ['solved', '已解决'], 3 => ['closed', '已关闭']];
                $enc = function($s) { return htmlspecialchars((string)$s, ENT_QUOTES, 'UTF-8'); };
                foreach ($feedbacks as $fb):
                    $s = isset($statusMap[$fb['status']]) ? $statusMap[$fb['status']] : $statusMap[0];
                    $statusClass = $s[0]; $statusText = $s[1];
                    $hasReply = !empty($fb['reply']) && trim((string)$fb['reply']) !== '';
                ?>
                    <div class="fb-card" onclick='openFbDetail(<?php echo $fb["could_id"]; ?>)'
                         data-could_id="<?php echo $fb['could_id']; ?>"
                         data-title="<?php echo $enc($fb['title']); ?>"
                         data-content="<?php echo $enc($fb['content']); ?>"
                         data-status_class="<?php echo $statusClass; ?>"
                         data-status_text="<?php echo $statusText; ?>"
                         data-create_time="<?php echo $fb['create_time']; ?>"
                         data-reply="<?php echo $hasReply ? $enc($fb['reply']) : ''; ?>"
                         data-reply_nickname="<?php echo $hasReply ? $enc($fb['reply_nickname'] ?? ($fb['reply_qq'] ?? '管理员')) : ''; ?>"
                         data-reply_time="<?php echo $hasReply ? $enc($fb['reply_time'] ?? '') : ''; ?>">
                        <div class="fb-card-head">
                            <div class="fb-card-title"><?php echo $enc($fb['title']); ?></div>
                            <span class="fb-status <?php echo $statusClass; ?>"><?php echo $statusText; ?></span>
                        </div>
                        <div class="fb-card-meta">
                            <span>提交时间：<?php echo $fb['create_time']; ?></span>
                            <span>编号：#<?php echo $fb['could_id']; ?></span>
                            <?php if ($hasReply): ?>
                                <span style="color:#2f54eb;">已回复 · <?php echo $enc($fb['reply_time'] ?? ''); ?></span>
                            <?php endif; ?>
                        </div>
                        <div class="fb-card-preview"><?php echo $enc($fb['content']); ?></div>
                        <?php if ($hasReply): ?>
                            <div class="fb-reply-badge">
                                已有处理回复（<?php echo $enc($fb['reply_nickname'] ?? ($fb['reply_qq'] ?? '管理员')); ?> · <?php echo $enc($fb['reply_time'] ?? ''); ?>），点击卡片查看详情
                            </div>
                        <?php endif; ?>
                    </div>
                <?php endforeach; ?>
            <?php endif; ?>
        </div>
    </div>
</div>

<!-- 反馈详情弹窗 -->
<div class="fb-modal-overlay" id="fbDetailOverlay">
    <div class="fb-modal">
        <div class="fb-modal-head">
            <h3 id="fbDetailTitle">反馈详情</h3>
            <button class="fb-modal-close" onclick="closeFbDetail()" aria-label="关闭">
                <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line>
                </svg>
            </button>
        </div>
        <div class="fb-modal-body">
            <div class="fb-meta-row">
                <span id="fbDetailStatus"></span>
                <span>编号：<b id="fbDetailId"></b></span>
                <span>提交时间：<b id="fbDetailTime"></b></span>
            </div>
            <div style="font-size:12px; color:#6b8aa9; margin-bottom: 6px;">反馈内容</div>
            <div class="fb-detail-content" id="fbDetailContent"></div>
            <div class="fb-detail-reply" id="fbDetailReplyBlock" style="display:none;">
                <div class="h"><span id="fbDetailReplyWho">管理员</span> 的处理回复</div>
                <div class="t" id="fbDetailReplyTime"></div>
                <div class="c" id="fbDetailReplyBody"></div>
            </div>
        </div>
        <div class="fb-modal-foot">
            <button class="btn btn-secondary" onclick="closeFbDetail()">关闭</button>
        </div>
    </div>
</div>

<script>
    var fbCsrf = '<?php echo $csrfToken; ?>';
    var fbStatusMap = {
        pending:    {cls: 'fb-status pending',    text: '待处理'},
        processing: {cls: 'fb-status processing', text: '处理中'},
        solved:     {cls: 'fb-status solved',     text: '已解决'},
        closed:     {cls: 'fb-status closed',     text: '已关闭'}
    };

    // 字数统计
    var contentEl = document.getElementById('fbContent');
    var countEl = document.getElementById('fbCount');
    if (contentEl && countEl) {
        var up = function() { countEl.textContent = contentEl.value.length; };
        contentEl.addEventListener('input', up); up();
    }

    // 提交反馈
    document.getElementById('fbForm').addEventListener('submit', function(e) {
        e.preventDefault();
        var title   = document.getElementById('fbTitle').value.trim();
        var content = document.getElementById('fbContent').value;
        var btn     = document.getElementById('fbSubmit');

        if (!title)   { if (typeof Toast!=='undefined') Toast.error('请填写标题');   else alert('请填写标题');   document.getElementById('fbTitle').focus();   return; }
        if (!content) { if (typeof Toast!=='undefined') Toast.error('请填写详细内容'); else alert('请填写详细内容'); document.getElementById('fbContent').focus(); return; }

        if (typeof Loading!=='undefined') Loading.show();
        btn.disabled = true; btn.textContent = '提交中...';

        fetch('../api/user/feedback_submit.php', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ title: title, content: content, csrf_token: fbCsrf })
        })
        .then(function(r){ return r.json(); })
        .then(function(data) {
            if (typeof Loading!=='undefined') Loading.hide();
            btn.disabled = false; btn.textContent = '提交反馈';
            if (data.code === 200) {
                if (typeof Toast!=='undefined') Toast.success(data.message || '提交成功');
                setTimeout(function(){ location.reload(); }, 900);
            } else {
                if (typeof Toast!=='undefined') Toast.error(data.message || '提交失败');
                else alert(data.message || '提交失败');
            }
        })
        .catch(function() {
            if (typeof Loading!=='undefined') Loading.hide();
            btn.disabled = false; btn.textContent = '提交反馈';
            if (typeof Toast!=='undefined') Toast.error('网络错误，请稍后重试');
            else alert('网络错误，请稍后重试');
        });
    });

    // 打开详情
    function openFbDetail(couldId) {
        var card = document.querySelector('.fb-card[data-could_id="' + couldId + '"]');
        if (!card) return;
        var ds = card.dataset;

        document.getElementById('fbDetailTitle').textContent = ds.title || '反馈详情';
        var statusInfo = fbStatusMap[ds.status_class] || fbStatusMap.pending;
        document.getElementById('fbDetailStatus').innerHTML = '<span class="' + statusInfo.cls + '">' + statusInfo.text + '</span>';
        document.getElementById('fbDetailId').textContent   = '#' + couldId;
        document.getElementById('fbDetailTime').textContent = ds.create_time || '';
        document.getElementById('fbDetailContent').textContent = ds.content || '';

        var replyBlock = document.getElementById('fbDetailReplyBlock');
        if (ds.reply && ds.reply !== '') {
            replyBlock.style.display = 'block';
            document.getElementById('fbDetailReplyWho').textContent  = ds.reply_nickname || '管理员';
            document.getElementById('fbDetailReplyTime').textContent = ds.reply_time ? ('回复时间：' + ds.reply_time) : '';
            document.getElementById('fbDetailReplyBody').textContent = ds.reply;
        } else {
            replyBlock.style.display = 'none';
        }
        document.getElementById('fbDetailOverlay').classList.add('show');
    }

    function closeFbDetail() {
        document.getElementById('fbDetailOverlay').classList.remove('show');
    }

    document.getElementById('fbDetailOverlay').addEventListener('click', function(e) {
        if (e.target === this) closeFbDetail();
    });
    document.addEventListener('keydown', function(e) {
        if (e.key === 'Escape') closeFbDetail();
    });
</script>

<?php require_once __DIR__ . '/footer.php'; ?>
