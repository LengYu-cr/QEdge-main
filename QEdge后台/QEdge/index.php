<?php
/**
 * QEdge系统主页
 * 模块介绍页面
 */

require_once __DIR__ . '/require.php';
?>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>QEdge - QQ模块系统</title>
    <link rel="stylesheet" href="assets/style.css">
</head>
<body>
    <div class="container">
        <div style="text-align: center; padding: 48px 0 32px;">
            <div style="width: 64px; height: 64px; background: var(--color-primary); border-radius: var(--radius-lg); display: flex; align-items: center; justify-content: center; color: white; margin: 0 auto 16px;">
                <span class="icon icon-lg">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <path d="M12 2L2 7l10 5 10-5-10-5z"></path>
                        <path d="M2 17l10 5 10-5"></path>
                        <path d="M2 12l10 5 10-5"></path>
                    </svg>
                </span>
            </div>
            <h1 style="font-size: 32px; font-weight: 700; color: var(--color-text-primary); margin-bottom: 8px;">QEdge</h1>
            <p style="font-size: 16px; color: var(--color-text-secondary); margin-bottom: 16px;">强大的QQ模块管理系统</p>
            <span class="badge badge-primary">v<?php echo SYSTEM_VERSION; ?></span>
        </div>

        <div style="display: flex; justify-content: center; gap: 12px; margin-bottom: 40px; flex-wrap: wrap;">
            <a href="user/login.php" class="btn btn-primary btn-lg">用户登录</a>
            <a href="admin/login.php" class="btn btn-secondary btn-lg">管理后台</a>
            <a href="online_plugin/list.php" class="btn btn-secondary btn-lg">在线脚本</a>
        </div>

        <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(300px, 1fr)); gap: 16px; margin-bottom: 40px;">
            <div class="card">
                <div class="card-body">
                    <div style="width: 40px; height: 40px; background: var(--color-primary-light); color: var(--color-primary); border-radius: var(--radius-md); display: flex; align-items: center; justify-content: center; margin-bottom: 16px;">
                        <span class="icon">
                            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                                <circle cx="12" cy="7" r="4"></circle>
                            </svg>
                        </span>
                    </div>
                    <h3 style="font-size: 16px; font-weight: 600; color: var(--color-text-primary); margin-bottom: 8px;">用户管理</h3>
                    <p style="font-size: 14px; color: var(--color-text-secondary); line-height: 1.6;">完善的用户系统，支持用户注册、登录、个人信息管理等功能。赞助用户享有修改个性签名等特权。</p>
                </div>
            </div>

            <div class="card">
                <div class="card-body">
                    <div style="width: 40px; height: 40px; background: var(--color-success-light); color: var(--color-success); border-radius: var(--radius-md); display: flex; align-items: center; justify-content: center; margin-bottom: 16px;">
                        <span class="icon">
                            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
                                <polyline points="7 10 12 15 17 10"></polyline>
                                <line x1="12" y1="15" x2="12" y2="3"></line>
                            </svg>
                        </span>
                    </div>
                    <h3 style="font-size: 16px; font-weight: 600; color: var(--color-text-primary); margin-bottom: 8px;">在线脚本</h3>
                    <p style="font-size: 14px; color: var(--color-text-secondary); line-height: 1.6;">支持脚本上传、下载和管理。自动统计下载次数，方便用户分享和使用优质脚本。</p>
                </div>
            </div>

            <div class="card">
                <div class="card-body">
                    <div style="width: 40px; height: 40px; background: var(--color-warning-light); color: var(--color-warning); border-radius: var(--radius-md); display: flex; align-items: center; justify-content: center; margin-bottom: 16px;">
                        <span class="icon">
                            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"></path>
                            </svg>
                        </span>
                    </div>
                    <h3 style="font-size: 16px; font-weight: 600; color: var(--color-text-primary); margin-bottom: 8px;">安全可靠</h3>
                    <p style="font-size: 14px; color: var(--color-text-secondary); line-height: 1.6;">采用预处理SQL语句防止SQL注入，图形验证码防止暴力破解，全面的安全机制保障系统稳定运行。</p>
                </div>
            </div>

            <div class="card">
                <div class="card-body">
                    <div style="width: 40px; height: 40px; background: var(--color-purple-light); color: var(--color-purple); border-radius: var(--radius-md); display: flex; align-items: center; justify-content: center; margin-bottom: 16px;">
                        <span class="icon">
                            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                <line x1="18" y1="20" x2="18" y2="10"></line>
                                <line x1="12" y1="20" x2="12" y2="4"></line>
                                <line x1="6" y1="20" x2="6" y2="14"></line>
                            </svg>
                        </span>
                    </div>
                    <h3 style="font-size: 16px; font-weight: 600; color: var(--color-text-primary); margin-bottom: 8px;">数据统计</h3>
                    <p style="font-size: 14px; color: var(--color-text-secondary); line-height: 1.6;">实时统计用户活跃数据，包括今日注册数、在线用户数、脚本下载次数等关键指标。</p>
                </div>
            </div>

            <div class="card">
                <div class="card-body">
                    <div style="width: 40px; height: 40px; background: var(--color-primary-light); color: var(--color-primary); border-radius: var(--radius-md); display: flex; align-items: center; justify-content: center; margin-bottom: 16px;">
                        <span class="icon">
                            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                <rect x="5" y="2" width="14" height="20" rx="2" ry="2"></rect>
                                <line x1="12" y1="18" x2="12.01" y2="18"></line>
                            </svg>
                        </span>
                    </div>
                    <h3 style="font-size: 16px; font-weight: 600; color: var(--color-text-primary); margin-bottom: 8px;">响应式设计</h3>
                    <p style="font-size: 14px; color: var(--color-text-secondary); line-height: 1.6;">完美适配手机和电脑双端，采用纯白画布加简洁设计，优雅的用户界面。</p>
                </div>
            </div>

            <div class="card">
                <div class="card-body">
                    <div style="width: 40px; height: 40px; background: var(--color-danger-light); color: var(--color-danger); border-radius: var(--radius-md); display: flex; align-items: center; justify-content: center; margin-bottom: 16px;">
                        <span class="icon">
                            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                <polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"></polygon>
                            </svg>
                        </span>
                    </div>
                    <h3 style="font-size: 16px; font-weight: 600; color: var(--color-text-primary); margin-bottom: 8px;">心跳机制</h3>
                    <p style="font-size: 14px; color: var(--color-text-secondary); line-height: 1.6;">支持客户端心跳数据上报，实时获取用户在线状态，自动记录用户QQ版本和模块版本等信息。</p>
                </div>
            </div>
        </div>

        <div class="card" style="margin-bottom: 40px;">
            <div class="card-header">
                <h2 class="card-title" style="text-align: center;">系统特性</h2>
            </div>
            <div class="card-body">
                <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 12px;">
                    <div style="padding: 16px; background: var(--color-bg-secondary); border-radius: var(--radius-md); text-align: center;">
                        <div style="font-size: 18px; font-weight: 700; color: var(--color-text-primary); margin-bottom: 4px;">PHP 7.3</div>
                        <div style="font-size: 13px; color: var(--color-text-secondary);">运行环境</div>
                    </div>
                    <div style="padding: 16px; background: var(--color-bg-secondary); border-radius: var(--radius-md); text-align: center;">
                        <div style="font-size: 18px; font-weight: 700; color: var(--color-text-primary); margin-bottom: 4px;">MySQL</div>
                        <div style="font-size: 13px; color: var(--color-text-secondary);">数据库</div>
                    </div>
                    <div style="padding: 16px; background: var(--color-bg-secondary); border-radius: var(--radius-md); text-align: center;">
                        <div style="font-size: 18px; font-weight: 700; color: var(--color-text-primary); margin-bottom: 4px;">PDO</div>
                        <div style="font-size: 13px; color: var(--color-text-secondary);">数据库驱动</div>
                    </div>
                    <div style="padding: 16px; background: var(--color-bg-secondary); border-radius: var(--radius-md); text-align: center;">
                        <div style="font-size: 18px; font-weight: 700; color: var(--color-text-primary); margin-bottom: 4px;">安全</div>
                        <div style="font-size: 13px; color: var(--color-text-secondary);">防注入机制</div>
                    </div>
                </div>
            </div>
        </div>

        <div style="text-align: center; padding: 24px 0; color: var(--color-text-tertiary); font-size: 14px;">
            <p>&copy; <?php echo date('Y'); ?> QEdge System. All rights reserved.</p>
            <p style="margin-top: 8px;">作者：lengyu</p>
        </div>
    </div>
</body>
</html>
