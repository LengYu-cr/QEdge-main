<?php
/**
 * QEdge系统安装脚本
 * 仅适配PHP 7.3
 */

require_once __DIR__ . '/require.php';

// 检查是否已安装
$lockFile = __DIR__ . '/install.lock';
if (file_exists($lockFile)) {
    die('系统已安装！如需重新安装，请删除install.lock文件。');
}

// 检查GD扩展
if (!extension_loaded('gd')) {
    die('<div style="font-family: -apple-system, BlinkMacSystemFont, sans-serif; padding: 20px; background: #fff; border-radius: 10px; text-align: center;">
        <h2 style="color: #c62828;">安装失败</h2>
        <p style="color: #666; margin-top: 10px;">系统需要GD扩展来生成图形验证码。</p>
        <p style="color: #999; font-size: 14px; margin-top: 5px;">请在php.ini中启用GD扩展后重试。</p>
    </div>');
}

$message = '';
$error = '';

// 处理安装请求
if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    try {
        // 连接数据库（不指定数据库名）
        $dsn = "mysql:host=" . DataBase_HOST . ";charset=" . DataBase_CHARSET;
        $pdo = new PDO($dsn, DataBase_USER, DataBase_PASS, [
            PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION
        ]);

        // 创建数据库
        $pdo->exec("CREATE DATABASE IF NOT EXISTS `" . DataBase_NAME . "` CHARACTER SET " . DataBase_CHARSET . " COLLATE " . DataBase_CHARSET . "_unicode_ci");
        $pdo->exec("USE `" . DataBase_NAME . "`");

        // 设置SQL模式以兼容MySQL 5.5
        $pdo->exec("SET sql_mode = ''");

        // 创建用户表
        $sql = "CREATE TABLE IF NOT EXISTS `users` (
            `id` INT(11) UNSIGNED NOT NULL AUTO_INCREMENT,
            `qq` VARCHAR(20) NOT NULL UNIQUE COMMENT 'QQ号',
            `nickname` VARCHAR(100) DEFAULT NULL COMMENT '昵称',
            `password` VARCHAR(255) NOT NULL COMMENT '登录密码',
            `is_sponsor` TINYINT(1) DEFAULT 0 COMMENT '是否赞助用户 0否1是',
            `is_banned` TINYINT(1) DEFAULT 0 COMMENT '是否拉黑 0否1是',
            `signature` VARCHAR(500) DEFAULT NULL COMMENT '个性签名',
            `module_version` VARCHAR(50) DEFAULT NULL COMMENT '模块版本号',
            `qq_version` VARCHAR(50) DEFAULT NULL COMMENT 'QQ版本号',
            `upload_permission` TINYINT(1) DEFAULT 0 COMMENT '上传脚本权限 0否1是',
            `review_permission` TINYINT(1) DEFAULT 0 COMMENT '脚本审核权限 0否1是',
            `register_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
            `last_login_time` DATETIME DEFAULT NULL COMMENT '最后登录时间',
            `last_login_ip` VARCHAR(45) DEFAULT NULL COMMENT '最后登录IP',
            PRIMARY KEY (`id`),
            UNIQUE KEY `idx_qq` (`qq`),
            KEY `idx_sponsor` (`is_sponsor`),
            KEY `idx_banned` (`is_banned`),
            KEY `idx_register_time` (`register_time`)
        ) ENGINE=InnoDB DEFAULT CHARSET=" . DataBase_CHARSET . " COMMENT='用户表'";
        $pdo->exec($sql);

        // 创建在线插件表
        $sql = "CREATE TABLE IF NOT EXISTS `plugins` (
            `id` INT(11) UNSIGNED NOT NULL AUTO_INCREMENT,
            `plugin_id` VARCHAR(100) NOT NULL UNIQUE COMMENT '插件ID',
            `plugin_name` VARCHAR(200) NOT NULL COMMENT '插件名称',
            `version_code` VARCHAR(50) NOT NULL COMMENT '版本号',
            `author_name` VARCHAR(100) NOT NULL COMMENT '作者名称',
            `upload_qq` VARCHAR(20) NOT NULL COMMENT '上传者QQ',
            `file_path` VARCHAR(500) NOT NULL COMMENT '文件路径',
            `download_count` INT(11) UNSIGNED DEFAULT 0 COMMENT '下载次数',
            `upload_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
            `update_time` DATETIME DEFAULT NULL COMMENT '更新时间',
            `status` TINYINT(1) DEFAULT 0 COMMENT '状态 0待审核 1已通过 2已拒绝',
            `reject_reason` VARCHAR(500) DEFAULT NULL COMMENT '拒绝原因',
            `description` VARCHAR(1000) DEFAULT NULL COMMENT '脚本简介',
            `review_time` DATETIME DEFAULT NULL COMMENT '审核时间',
            `reviewer_qq` VARCHAR(20) DEFAULT NULL COMMENT '审核管理员QQ',
            PRIMARY KEY (`id`),
            UNIQUE KEY `idx_plugin_id` (`plugin_id`),
            KEY `idx_upload_qq` (`upload_qq`),
            KEY `idx_upload_time` (`upload_time`),
            KEY `idx_status` (`status`)
        ) ENGINE=InnoDB DEFAULT CHARSET=" . DataBase_CHARSET . " COMMENT='在线插件表'";
        $pdo->exec($sql);

        // 创建验证码表
        $sql = "CREATE TABLE IF NOT EXISTS `email_codes` (
            `id` INT(11) UNSIGNED NOT NULL AUTO_INCREMENT,
            `email` VARCHAR(100) NOT NULL COMMENT '邮箱',
            `code` VARCHAR(10) NOT NULL COMMENT '验证码',
            `type` VARCHAR(20) NOT NULL COMMENT '类型',
            `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
            `expires_at` DATETIME NOT NULL COMMENT '过期时间',
            `used` TINYINT(1) DEFAULT 0 COMMENT '是否已使用',
            PRIMARY KEY (`id`),
            KEY `idx_email` (`email`),
            KEY `idx_code` (`code`),
            KEY `idx_expires` (`expires_at`)
        ) ENGINE=InnoDB DEFAULT CHARSET=" . DataBase_CHARSET . " COMMENT='邮箱验证码表'";
        $pdo->exec($sql);

        // 创建用户活跃统计表
        $sql = "CREATE TABLE IF NOT EXISTS `user_stats` (
            `id` INT(11) UNSIGNED NOT NULL AUTO_INCREMENT,
            `stat_date` DATE NOT NULL UNIQUE COMMENT '统计日期',
            `new_users` INT(11) UNSIGNED DEFAULT 0 COMMENT '新增用户数',
            `active_users` INT(11) UNSIGNED DEFAULT 0 COMMENT '活跃用户数',
            `online_plugins` INT(11) UNSIGNED DEFAULT 0 COMMENT '在线脚本数',
            PRIMARY KEY (`id`),
            UNIQUE KEY `idx_stat_date` (`stat_date`)
        ) ENGINE=InnoDB DEFAULT CHARSET=" . DataBase_CHARSET . " COMMENT='用户活跃统计表'";
        $pdo->exec($sql);

        // 创建会话表
        $sql = "CREATE TABLE IF NOT EXISTS `sessions` (
            `id` INT(11) UNSIGNED NOT NULL AUTO_INCREMENT,
            `session_id` VARCHAR(191) NOT NULL UNIQUE COMMENT '会话ID',
            `user_qq` VARCHAR(20) NOT NULL COMMENT '用户QQ',
            `user_type` VARCHAR(20) NOT NULL COMMENT '用户类型 user/admin',
            `ip_address` VARCHAR(45) DEFAULT NULL COMMENT 'IP地址',
            `user_agent` VARCHAR(500) DEFAULT NULL COMMENT '用户代理',
            `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
            `last_activity` DATETIME DEFAULT NULL COMMENT '最后活动时间',
            PRIMARY KEY (`id`),
            KEY `idx_session_id` (`session_id`),
            KEY `idx_user_qq` (`user_qq`),
            KEY `idx_last_activity` (`last_activity`)
        ) ENGINE=InnoDB DEFAULT CHARSET=" . DataBase_CHARSET . " COMMENT='会话表'";
        $pdo->exec($sql);

        // 创建黑名单表（支持未注册用户）
        $sql = "CREATE TABLE IF NOT EXISTS `banned_users` (
            `id` INT(11) UNSIGNED NOT NULL AUTO_INCREMENT,
            `qq` VARCHAR(20) NOT NULL UNIQUE COMMENT 'QQ号',
            `nickname` VARCHAR(100) DEFAULT NULL COMMENT '昵称（如有）',
            `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '拉黑时间',
            PRIMARY KEY (`id`),
            UNIQUE KEY `idx_qq` (`qq`),
            KEY `idx_created_at` (`created_at`)
        ) ENGINE=InnoDB DEFAULT CHARSET=" . DataBase_CHARSET . " COMMENT='黑名单表'";
        $pdo->exec($sql);

        // 创建赞助用户表（支持未注册用户）
        $sql = "CREATE TABLE IF NOT EXISTS `sponsor_users` (
            `id` INT(11) UNSIGNED NOT NULL AUTO_INCREMENT,
            `qq` VARCHAR(20) NOT NULL UNIQUE COMMENT 'QQ号',
            `nickname` VARCHAR(100) DEFAULT NULL COMMENT '昵称（如有）',
            `amount` DECIMAL(10,2) DEFAULT 0.00 COMMENT '赞助金额',
            `note` VARCHAR(255) DEFAULT NULL COMMENT '备注',
            `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '添加时间',
            PRIMARY KEY (`id`),
            UNIQUE KEY `idx_qq` (`qq`),
            KEY `idx_created_at` (`created_at`)
        ) ENGINE=InnoDB DEFAULT CHARSET=" . DataBase_CHARSET . " COMMENT='赞助用户表'";
        $pdo->exec($sql);

        // 创建管理员账号
        $adminPassword = password_hash(ADMIN_PASSWORD, PASSWORD_DEFAULT);
        $stmt = $pdo->prepare("INSERT IGNORE INTO `users` (`qq`, `nickname`, `password`, `is_sponsor`, `upload_permission`, `review_permission`) VALUES (?, ?, ?, 1, 1, 1)");
        $stmt->execute([ADMIN_QQ, ADMIN_USERNAME, $adminPassword]);

        // 创建日志目录
        $logsDir = __DIR__ . '/logs';
        if (!is_dir($logsDir)) {
            mkdir($logsDir, 0755, true);
        }

        // 创建上传目录
        $uploadsDir = __DIR__ . '/uploads/plugins';
        if (!is_dir($uploadsDir)) {
            mkdir($uploadsDir, 0755, true);
        }

        // 创建安装锁文件
        file_put_contents($lockFile, date('Y-m-d H:i:s'));

        $message = '安装成功！数据库表已创建。';
    } catch (Exception $e) {
        $error = '安装失败: ' . $e->getMessage();
    }
}
?>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>QEdge系统安装</title>
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }
        body {
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
            background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
            min-height: 100vh;
            display: flex;
            align-items: center;
            justify-content: center;
            padding: 20px;
        }
        .container {
            background: white;
            border-radius: 20px;
            box-shadow: 0 10px 40px rgba(0, 0, 0, 0.1);
            padding: 40px;
            max-width: 600px;
            width: 100%;
            animation: fadeIn 0.5s ease;
        }
        @keyframes fadeIn {
            from {
                opacity: 0;
                transform: translateY(-20px);
            }
            to {
                opacity: 1;
                transform: translateY(0);
            }
        }
        .title {
            color: #333;
            font-size: 28px;
            margin-bottom: 10px;
            text-align: center;
        }
        .subtitle {
            color: #666;
            font-size: 14px;
            text-align: center;
            margin-bottom: 30px;
        }
        .message {
            padding: 15px;
            border-radius: 10px;
            margin-bottom: 20px;
            animation: slideIn 0.3s ease;
        }
        @keyframes slideIn {
            from {
                opacity: 0;
                transform: translateX(-10px);
            }
            to {
                opacity: 1;
                transform: translateX(0);
            }
        }
        .success {
            background: #d4edda;
            color: #155724;
            border: 1px solid #c3e6cb;
        }
        .error {
            background: #f8d7da;
            color: #721c24;
            border: 1px solid #f5c6cb;
        }
        .info-box {
            background: #e3f2fd;
            border-left: 4px solid #2196F3;
            padding: 15px;
            border-radius: 5px;
            margin-bottom: 20px;
        }
        .info-box h3 {
            color: #1976D2;
            margin-bottom: 10px;
            font-size: 16px;
        }
        .info-box p {
            color: #555;
            font-size: 14px;
            line-height: 1.6;
        }
        .btn {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            border: none;
            padding: 15px 40px;
            font-size: 16px;
            border-radius: 25px;
            cursor: pointer;
            width: 100%;
            transition: all 0.3s ease;
            box-shadow: 0 5px 15px rgba(102, 126, 234, 0.4);
        }
        .btn:hover {
            transform: translateY(-2px);
            box-shadow: 0 7px 20px rgba(102, 126, 234, 0.6);
        }
        .btn:active {
            transform: translateY(0);
        }
        .requirements {
            margin-top: 20px;
            padding: 15px;
            background: #f8f9fa;
            border-radius: 10px;
        }
        .requirements h3 {
            color: #333;
            margin-bottom: 10px;
            font-size: 16px;
        }
        .requirements ul {
            list-style: none;
        }
        .requirements li {
            color: #666;
            font-size: 14px;
            padding: 5px 0;
        }
        .requirements li:before {
            content: "✓ ";
            color: #4CAF50;
            font-weight: bold;
        }
    </style>
</head>
<body>
    <div class="container">
        <h1 class="title">QEdge 系统安装</h1>
        <p class="subtitle">版本 <?php echo SYSTEM_VERSION; ?></p>

        <?php if ($message): ?>
            <div class="message success">
                <?php echo htmlspecialchars($message); ?>
            </div>
        <?php endif; ?>

        <?php if ($error): ?>
            <div class="message error">
                <?php echo htmlspecialchars($error); ?>
            </div>
        <?php endif; ?>

        <div class="info-box">
            <h3>安装说明</h3>
            <p>点击下方按钮开始安装。安装程序将自动创建所需的数据库表和目录结构。</p>
            <p style="margin-top: 10px;"><strong>管理员账号：</strong><?php echo ADMIN_USERNAME; ?></p>
        </div>

        <div class="requirements">
            <h3>系统要求</h3>
            <ul>
                <li>PHP 7.3 或更高版本</li>
                <li>MySQL 5.7 或更高版本</li>
                <li>PDO PHP 扩展</li>
                <li>Session PHP 扩展</li>
                <li>写入权限（用于创建目录和日志）</li>
            </ul>
        </div>

        <?php if (!$message): ?>
            <form method="POST" style="margin-top: 20px;">
                <button type="submit" class="btn">开始安装</button>
            </form>
        <?php else: ?>
            <a href="index.php" class="btn" style="display: block; text-align: center; text-decoration: none; margin-top: 20px;">访问首页</a>
        <?php endif; ?>
    </div>
</body>
</html>