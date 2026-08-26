<?php
/**
 * QEdge 系统主页 · 模块介绍
 * 复用后台统一设计令牌（assets/style.css），纯白布局 + 卡片堆叠 + CSS 入场动画。
 */

require_once __DIR__ . '/require.php';

// 模块功能卡片：图标(svg path) / 主题色变量前缀 / 标题 / 描述
$features = [
    [
        'accent' => 'primary',
        'icon'   => '<circle cx="12" cy="7" r="4"></circle><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>',
        'title'  => '用户体系',
        'desc'   => '注册、登录、资料管理一应俱全，赞助用户可自定义个性签名等专属特权。',
    ],
    [
        'accent' => 'success',
        'icon'   => '<path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path><polyline points="7 10 12 15 17 10"></polyline><line x1="12" y1="15" x2="12" y2="3"></line>',
        'title'  => '在线脚本',
        'desc'   => '一键上传、下载与管理脚本，支持 Java / JS 双引擎，自动统计下载次数。',
    ],
    [
        'accent' => 'warning',
        'icon'   => '<path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"></path>',
        'title'  => '安全可靠',
        'desc'   => '预处理 SQL 防注入、图形验证码防爆破，多重机制守护系统稳定运行。',
    ],
    [
        'accent' => 'purple',
        'icon'   => '<line x1="18" y1="20" x2="18" y2="10"></line><line x1="12" y1="20" x2="12" y2="4"></line><line x1="6" y1="20" x2="6" y2="14"></line>',
        'title'  => '数据统计',
        'desc'   => '实时汇总注册量、在线人数、脚本下载等关键指标，运营数据一目了然。',
    ],
    [
        'accent' => 'primary',
        'icon'   => '<rect x="5" y="2" width="14" height="20" rx="2" ry="2"></rect><line x1="12" y1="18" x2="12.01" y2="18"></line>',
        'title'  => '双端适配',
        'desc'   => '手机与电脑完美兼容，纯白画布搭配简洁排版，界面优雅清爽。',
    ],
    [
        'accent' => 'danger',
        'icon'   => '<polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"></polygon>',
        'title'  => '心跳机制',
        'desc'   => '客户端心跳实时上报在线状态，自动记录 QQ 与模块版本等运行信息。',
    ],
];
?>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>QEdge - QQ 模块系统</title>
    <link rel="stylesheet" href="assets/style.css">
    <style>
        /* ===== 首页专属：入场动画 + Hero，仅补充 style.css 未覆盖的部分 ===== */
        @keyframes qe-rise {
            from { opacity: 0; transform: translateY(16px); }
            to   { opacity: 1; transform: translateY(0); }
        }
        .qe-rise { opacity: 0; animation: qe-rise .55s cubic-bezier(.22,.61,.36,1) forwards; }

        .qe-hero { text-align: center; padding: 64px 0 40px; }
        .qe-logo {
            width: 68px; height: 68px; margin: 0 auto 20px;
            display: flex; align-items: center; justify-content: center;
            color: #fff; border-radius: var(--radius-xl);
            background: linear-gradient(135deg, var(--color-primary), #60a5fa);
            box-shadow: var(--shadow-lg);
        }
        .qe-hero h1 { font-size: 34px; font-weight: 700; color: var(--color-text-primary); letter-spacing: -.5px; }
        .qe-hero p  { font-size: 16px; color: var(--color-text-secondary); margin: 10px 0 18px; }
        .qe-cta { display: flex; justify-content: center; gap: 12px; margin-top: 28px; flex-wrap: wrap; }

        .qe-grid {
            display: grid; gap: 16px; margin: 8px 0 48px;
            grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
        }
        /* 卡片堆叠：hover 轻微上浮，配合入场动画 */
        .qe-feat {
            background: var(--color-bg); border: 1px solid var(--color-border);
            border-radius: var(--radius-lg); padding: 24px; box-shadow: var(--shadow-sm);
            transition: transform .25s ease, box-shadow .25s ease, border-color .25s ease;
        }
        .qe-feat:hover {
            transform: translateY(-4px);
            box-shadow: var(--shadow-lg);
            border-color: var(--color-border-light);
        }
        .qe-feat-ic {
            width: 44px; height: 44px; margin-bottom: 16px;
            display: flex; align-items: center; justify-content: center;
            border-radius: var(--radius-md);
        }
        .qe-feat h3 { font-size: 16px; font-weight: 600; color: var(--color-text-primary); margin-bottom: 8px; }
        .qe-feat p  { font-size: 14px; color: var(--color-text-secondary); line-height: 1.7; }

        .qe-footer { text-align: center; padding: 8px 0 40px; color: var(--color-text-tertiary); font-size: 13px; }
        .qe-footer p + p { margin-top: 6px; }

        /* 图标配色（对应 --color-*-light / --color-*） */
        .ic-primary { background: var(--color-primary-light); color: var(--color-primary); }
        .ic-success { background: var(--color-success-light); color: var(--color-success); }
        .ic-warning { background: var(--color-warning-light); color: var(--color-warning); }
        .ic-purple  { background: var(--color-purple-light);  color: var(--color-purple); }
        .ic-danger  { background: var(--color-danger-light);  color: var(--color-danger); }

        @media (max-width: 600px) {
            .qe-hero { padding: 40px 0 28px; }
            .qe-hero h1 { font-size: 28px; }
        }
        @media (prefers-reduced-motion: reduce) {
            .qe-rise { animation: none; opacity: 1; }
            .qe-feat { transition: none; }
        }
    </style>
</head>
<body>
    <nav class="navbar">
        <div class="navbar-inner">
            <a href="index.php" class="navbar-brand">
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
                <a href="online_plugin/list.php">在线脚本</a>
                <a href="user/login.php">用户中心</a>
                <a href="admin/login.php">管理后台</a>
            </div>
        </div>
    </nav>

    <div class="container">
        <section class="qe-hero qe-rise">
            <div class="qe-logo">
                <span class="icon icon-lg">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <path d="M12 2L2 7l10 5 10-5-10-5z"></path>
                        <path d="M2 17l10 5 10-5"></path>
                        <path d="M2 12l10 5 10-5"></path>
                    </svg>
                </span>
            </div>
            <h1>QEdge</h1>
            <p>基于 Xposed 的 QQ 扩展模块系统</p>
            <span class="badge badge-primary">v<?php echo SYSTEM_VERSION; ?></span>
            <div class="qe-cta">
                <a href="user/login.php" class="btn btn-primary btn-lg">进入用户中心</a>
                <a href="online_plugin/list.php" class="btn btn-secondary btn-lg">浏览在线脚本</a>
            </div>
        </section>

        <div class="qe-grid">
            <?php foreach ($features as $i => $f): ?>
            <div class="qe-feat qe-rise" style="animation-delay: <?php echo 0.08 + $i * 0.07; ?>s;">
                <div class="qe-feat-ic ic-<?php echo $f['accent']; ?>">
                    <span class="icon">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><?php echo $f['icon']; ?></svg>
                    </span>
                </div>
                <h3><?php echo htmlspecialchars($f['title']); ?></h3>
                <p><?php echo htmlspecialchars($f['desc']); ?></p>
            </div>
            <?php endforeach; ?>
        </div>

        <footer class="qe-footer qe-rise" style="animation-delay: .6s;">
            <p>&copy; <?php echo date('Y'); ?> QEdge System. All rights reserved.</p>
            <p>作者：lengyu</p>
        </footer>
    </div>
</body>
</html>
