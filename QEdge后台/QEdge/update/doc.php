<?php
require_once __DIR__ . '/../require.php';
?>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>更新日志 · QEdge</title>
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }

        body {
            background: #ffffff;
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
            min-height: 100vh;
            display: flex;
            justify-content: center;
            align-items: center;
            padding: 2rem 1.5rem;
            margin: 0;
            color: #1e1e1e;
            line-height: 1.5;
        }

        .log-container {
            max-width: 720px;
            width: 100%;
            background: #ffffff;
            box-shadow: 0 8px 24px rgba(0, 0, 0, 0.03), 0 2px 6px rgba(0, 0, 0, 0.02);
            border-radius: 28px;
            padding: 2.5rem 2rem;
            transition: all 0.2s ease;
            border: 1px solid #f0f0f0;
        }

        /* 纯白标题 — 仅字重区分 */
        .log-header {
            margin-bottom: 2.4rem;
            border-bottom: 1px solid #eaeaea;
            padding-bottom: 1.2rem;
        }

        .log-header h1 {
            font-size: 1.8rem;
            font-weight: 450;
            letter-spacing: -0.02em;
            color: #111;
            margin: 0 0 0.2rem 0;
        }

        .log-header .sub {
            font-size: 0.9rem;
            color: #777;
            font-weight: 350;
            letter-spacing: 0.02em;
        }

        /* 卡片列表 — 纯白底，极简分隔 */
        .entry-list {
            display: flex;
            flex-direction: column;
            gap: 2rem;
        }

        .entry-card {
            background: #ffffff;
            border-radius: 18px;
            padding: 0.2rem 0 0.6rem 0;
            transition: all 0.2s;
            border-bottom: 1px solid #f0f0f0;
        }

        .entry-card:last-of-type {
            border-bottom: none;
            padding-bottom: 0;
        }

        /* 版本 + 日期行 — 清晰干净 */
        .entry-meta {
            display: flex;
            flex-wrap: wrap;
            align-items: baseline;
            gap: 0.75rem 1.2rem;
            margin-bottom: 0.9rem;
        }

        .entry-version {
            font-size: 1.25rem;
            font-weight: 460;
            color: #111;
            letter-spacing: -0.01em;
            background: #f8f8f8;
            padding: 0.1rem 0.8rem;
            border-radius: 40px;
            display: inline-block;
            border: 1px solid #ececec;
        }

        .entry-date {
            font-size: 0.95rem;
            color: #6b6b6b;
            font-weight: 350;
            letter-spacing: 0.02em;
        }

        /* 更新内容列表 — 无圆点，用细线 + 缩进 */
        .items-list {
            list-style: none;
            padding: 0;
            margin: 0;
        }

        .items-list li {
            position: relative;
            padding: 0.45rem 0 0.45rem 1.5rem;
            font-size: 0.98rem;
            font-weight: 350;
            color: #1f1f1f;
            line-height: 1.6;
            border-bottom: 1px solid #f4f4f4;
        }

        .items-list li:last-child {
            border-bottom: none;
        }

        /* 细线装饰 — 纯粹，无圆点 */
        .items-list li::before {
            content: "—";
            position: absolute;
            left: 0;
            color: #b0b0b0;
            font-weight: 300;
            font-size: 0.9rem;
        }

        /* 动态入场 — 逐项滑入 */
        .entry-card {
            opacity: 0;
            transform: translateY(12px);
            animation: slideUp 0.45s cubic-bezier(0.08, 0.65, 0.3, 1) forwards;
        }

        .entry-card:nth-child(1) { animation-delay: 0.04s; }
        .entry-card:nth-child(2) { animation-delay: 0.12s; }
        .entry-card:nth-child(3) { animation-delay: 0.20s; }
        .entry-card:nth-child(4) { animation-delay: 0.28s; }
        .entry-card:nth-child(5) { animation-delay: 0.36s; }
        .entry-card:nth-child(6) { animation-delay: 0.44s; }

        @keyframes slideUp {
            0% {
                opacity: 0;
                transform: translateY(16px);
            }
            100% {
                opacity: 1;
                transform: translateY(0);
            }
        }

        /* 空状态或无更新 — 占位干净 */
        .empty-state {
            color: #888;
            font-weight: 350;
            padding: 1.2rem 0 0.8rem 0;
            border-bottom: 1px solid #f0f0f0;
        }

        /* 微响应 */
        @media (max-width: 480px) {
            .log-container {
                padding: 1.8rem 1.2rem;
                border-radius: 20px;
            }
            .log-header h1 {
                font-size: 1.5rem;
            }
            .entry-version {
                font-size: 1.1rem;
                padding: 0.05rem 0.7rem;
            }
            .items-list li {
                font-size: 0.92rem;
                padding-left: 1.3rem;
            }
        }

        /* 纯白，无任何彩色干扰 */
        a, a:visited, a:hover {
            color: inherit;
            text-decoration: none;
        }
        ::selection {
            background: #eaeaea;
            color: #000;
        }
    </style>
</head>
<body>
    <div class="log-container">

        <div class="log-header">
            <h1>更新日志</h1>
        </div>

        <div class="entry-list">
            <?php
            // 若未定义，给予后备，避免报错
            $version = defined('SYSTEM_VERSION') ? SYSTEM_VERSION : 'v1.0.0';
            $changelog = [];

            // 构造日志条目
            if (defined('SYSTEM_UPDATE_LOG') && is_array(SYSTEM_UPDATE_LOG)) {
                $changelog[] = [
                    'version' => $version,
                    'date' => date('Y-m-d'),
                    'items' => SYSTEM_UPDATE_LOG
                ];
            } else {
                // 无数据时显示占位 (但依然保持动态UI)
                $changelog[] = [
                    'version' => $version,
                    'date' => date('Y-m-d'),
                    'items' => ['暂无更新记录', '请检查 SYSTEM_UPDATE_LOG']
                ];
            }

            // 遍历输出卡片
            foreach ($changelog as $entry) :
                // 安全获取
                $ver = isset($entry['version']) ? $entry['version'] : '—';
                $date = isset($entry['date']) ? $entry['date'] : date('Y-m-d');
                $items = isset($entry['items']) && is_array($entry['items']) ? $entry['items'] : [];
            ?>
                <div class="entry-card">
                    <div class="entry-meta">
                        <span class="entry-version"><?php echo htmlspecialchars($ver, ENT_QUOTES, 'UTF-8'); ?></span>
                        <span class="entry-date"><?php echo htmlspecialchars($date, ENT_QUOTES, 'UTF-8'); ?></span>
                    </div>

                    <?php if (!empty($items)) : ?>
                        <ul class="items-list">
                            <?php foreach ($items as $item) : ?>
                                <li><?php echo htmlspecialchars($item, ENT_QUOTES, 'UTF-8'); ?></li>
                            <?php endforeach; ?>
                        </ul>
                    <?php else : ?>
                        <div class="empty-state">— 暂无更新内容</div>
                    <?php endif; ?>
                </div>
            <?php endforeach; ?>
        </div>
        <div style="margin-top: 2.4rem; border-top: 1px solid #f0f0f0; padding-top: 1.2rem; font-size: 0.75rem; color: #b5b5b5; letter-spacing: 0.02rem; text-align: right;">
            <span>系统版本 · <?php echo htmlspecialchars($version, ENT_QUOTES, 'UTF-8'); ?></span>
        </div>
    </div>
</body>
</html>