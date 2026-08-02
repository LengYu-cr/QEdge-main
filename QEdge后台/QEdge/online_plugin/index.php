<?php
/**
 * 在线插件上传
 * POST请求为上传脚本（只允许.zip文件）
 *
 * 关键设计：所有脚本元数据（pluginName/pluginID/versionCode/authorName/description）
 * 全部由服务端直接解压读取 ZIP 内部的 info.prop 和 desc.txt，
 * 完全不信任客户端提交的任何文本字段，从根本上解决字符集协商、
 * 客户端篡改字段等所有问题。
 * 客户端仅需要提交：qq（用于权限校验） + pluginFile（zip文件二进制）
 */

// ===== 入口强制UTF-8（PHP7.3 FastCGI/FPM模式下必须声明）=====
if (!headers_sent()) {
    header('Content-Type: text/html; charset=UTF-8');
}
ini_set('default_charset', 'UTF-8');
if (function_exists('mb_internal_encoding')) mb_internal_encoding('UTF-8');

require_once __DIR__ . '/../require.php';
require_once __DIR__ . '/../function.php';

// 入口再执行一次：防止require顺序问题导致 POST 仍有乱码（双重保险，幂等）
decodeUtf8Input();

/**
 * 从 ZIP 内部读取指定文件的内容（UTF-8），失败返回 null
 * @param ZipArchive $zip   已打开的 ZIP 句柄
 * @param string     $name  ZIP 内的相对路径（info.prop / desc.txt）
 * @return string|null      文件内容（UTF-8 字符串），读取失败返回 null
 */
function qedge_read_zip_entry_utf8($zip, $name) {
    $content = $zip->getFromName($name);
    if ($content === false || $content === '') {
        // 尝试兼容 Windows 打包的 ZIP（反斜杠路径）或带前导 ./ 的路径
        foreach ([$name, "./$name", "\\$name", str_replace('/', '\\', $name)] as $alt) {
            $content = $zip->getFromName($alt);
            if ($content !== false && $content !== '') break;
        }
    }
    if ($content === false || $content === '') return null;

    // 读取出来的是原始字节，判断编码并强制转成 UTF-8
    $hasMb = function_exists('mb_check_encoding');
    $isUtf8 = $hasMb ? @mb_check_encoding($content, 'UTF-8') : (bool)preg_match('//u', $content);
    if ($isUtf8) return $content;

    // 非 UTF-8（多半是 Windows GBK/CP936 打包的文件），兜底转码
    if (function_exists('mb_convert_encoding')) {
        foreach (['GBK', 'CP936', 'GB2312', 'BIG5'] as $src) {
            $try = @mb_convert_encoding($content, 'UTF-8', $src);
            if ($hasMb ? @mb_check_encoding($try, 'UTF-8') : (bool)preg_match('//u', $try)) {
                return $try;
            }
        }
    }
    if (function_exists('iconv')) {
        foreach (['GBK', 'CP936', 'GB2312', 'BIG5'] as $src) {
            $try = @iconv($src, 'UTF-8//IGNORE', $content);
            if ($try !== false && $try !== '') return $try;
        }
    }
    // 实在识别不了就原样返回（至少不丢字节）
    return $content;
}

/**
 * 解析 Properties 文件内容（key=value，每行一条；支持 # 开头注释）
 * 与 Java java.util.Properties 基本格式兼容
 * @param string $content info.prop 的文件内容（UTF-8 字符串）
 * @return array  [key => value] 关联数组
 */
function qedge_parse_properties($content) {
    $props = [];
    $lines = preg_split('/\r\n|\r|\n/', $content);
    foreach ($lines as $line) {
        $line = trim($line);
        if ($line === '' || strpos($line, '#') === 0 || strpos($line, '!') === 0) continue;
        $eqPos = strpos($line, '=');
        if ($eqPos === false) $eqPos = strpos($line, ':');
        if ($eqPos === false) {
            // 没有分隔符就整条当 key，value 空
            $props[trim($line)] = '';
            continue;
        }
        $key   = trim(substr($line, 0, $eqPos));
        $value = trim(substr($line, $eqPos + 1));
        // 去掉首尾引号（如果用户写了 "xxx"）
        if (strlen($value) >= 2 &&
            ($value[0] === '"' && $value[strlen($value)-1] === '"' ||
             $value[0] === "'" && $value[strlen($value)-1] === "'")) {
            $value = substr($value, 1, -1);
        }
        // Properties 的 \uXXXX 转义还原（兼容极少数人按 Java Properties 规范写的中文转义）
        $value = preg_replace_callback('/\\\\u([0-9a-fA-F]{4})/', function ($m) {
            return function_exists('mb_convert_encoding')
                ? mb_convert_encoding(pack('H*', $m[1]), 'UTF-8', 'UTF-16BE')
                : html_entity_decode('&#x' . $m[1] . ';', ENT_QUOTES, 'UTF-8');
        }, $value);
        $props[$key] = $value;
    }
    return $props;
}

$pdo = getDBConnection();

if (!$pdo) {
    if ($_SERVER['REQUEST_METHOD'] === 'POST') {
        jsonResponse(500, '数据库连接失败');
    }
    $dbError = '数据库连接失败，请检查数据库配置';
}

// 获取上传数据（只信任 qq 做权限校验，其他字段一律从 ZIP 内部读）
$userQQ = isset($_POST['qq']) ? sanitizeInput($_POST['qq']) : '';
if (!$userQQ) {
    $userQQ = isset($_GET['qq']) ? sanitizeInput($_GET['qq']) : '';
}

// 检查用户是否有上传权限
$hasPermission = false;
if ($pdo) {
    $stmt = $pdo->prepare("SELECT upload_permission FROM users WHERE qq = ?");
    $stmt->execute([$userQQ]);
    $user = $stmt->fetch();
    $hasPermission = $user && $user['upload_permission'] == 1;
}

if (!$hasPermission) {
    if ($_SERVER['REQUEST_METHOD'] === 'POST') {
        jsonResponse(403, '您没有上传脚本的权限');
    }
}

// 处理上传请求
if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    try {
        // 1) 仅校验 zip 文件本身，其他字段一概不看（不信任客户端）
        if (!isset($_FILES['pluginFile']) || $_FILES['pluginFile']['error'] !== UPLOAD_ERR_OK) {
            jsonResponse(400, '请上传插件文件');
        }
        $fileExt = strtolower(pathinfo($_FILES['pluginFile']['name'], PATHINFO_EXTENSION));
        if ($fileExt !== 'zip') {
            jsonResponse(400, '只允许上传.zip文件');
        }
        if ($_FILES['pluginFile']['size'] > 10485760) { // 10MB
            jsonResponse(400, '文件大小不能超过10MB');
        }
        if (empty($userQQ)) {
            jsonResponse(400, '无法识别当前登录QQ，请重试');
        }

        // 2) 保存 zip 到待审核目录
        $uploadDir = __DIR__ . '/../uploads/plugins/pending/';
        if (!is_dir($uploadDir)) {
            mkdir($uploadDir, 0755, true);
        }
        $fileName = generateUniqueID() . '.zip';
        $filePath = $uploadDir . $fileName;

        if (!move_uploaded_file($_FILES['pluginFile']['tmp_name'], $filePath)) {
            jsonResponse(500, '文件上传失败');
        }

        // 3) 打开 zip，读取 info.prop + desc.txt（核心：服务端直接读 ZIP，不信任客户端提交的文本）
        if (!class_exists('ZipArchive')) {
            @unlink($filePath);
            jsonResponse(500, '服务器未启用 ZipArchive 扩展，请联系管理员');
        }
        $zip = new ZipArchive();
        $zipOpen = $zip->open($filePath);
        if ($zipOpen !== true) {
            @unlink($filePath);
            jsonResponse(400, 'zip 文件损坏或无法读取（错误码：' . $zipOpen . '）');
        }

        $infoContent = qedge_read_zip_entry_utf8($zip, 'info.prop');
        $descContent = qedge_read_zip_entry_utf8($zip, 'desc.txt');
        $zip->close();

        if ($infoContent === null || trim($infoContent) === '') {
            @unlink($filePath);
            jsonResponse(400, 'zip 包内缺少 info.prop 文件，请按照规范打包插件');
        }

        $props = qedge_parse_properties($infoContent);

        // 从解析后的 properties 中提取必填字段（兼容大小写/别名）
        $findProp = function ($names) use ($props) {
            foreach ($names as $n) {
                if (isset($props[$n]) && trim($props[$n]) !== '') return trim($props[$n]);
            }
            return '';
        };

        $pluginName  = $findProp(['pluginName', 'plugin_name', 'name']);
        $pluginID    = $findProp(['id', 'pluginID', 'plugin_id', 'pluginId']);
        $versionCode = $findProp(['versionCode', 'version_code', 'version', 'versionName']);
        $authorName  = $findProp(['author', 'authorName', 'author_name']);

        if ($pluginName === '') {
            @unlink($filePath);
            jsonResponse(400, 'info.prop 中未找到 pluginName 字段');
        }
        if ($pluginID === '') {
            @unlink($filePath);
            jsonResponse(400, 'info.prop 中未找到 id/pluginID 字段');
        }
        if ($versionCode === '') {
            @unlink($filePath);
            jsonResponse(400, 'info.prop 中未找到 versionCode/version 字段');
        }
        if ($authorName === '') {
            @unlink($filePath);
            jsonResponse(400, 'info.prop 中未找到 author 字段');
        }

        // description：优先 desc.txt 内容，兜底 info.prop 里的 description，再兜底默认文案
        $description = '';
        if ($descContent !== null && trim($descContent) !== '') {
            $description = trim($descContent);
        } else {
            $description = $findProp(['description', 'desc', 'pluginDescription']);
        }
        if ($description === '') {
            $description = '该作者很懒，什么也没留下';
        }

        // 所有字段过 sanitize（里面会过一次 forceUtf8 保证 UTF-8 + htmlspecialchars 转义防注入）
        $pluginName  = sanitizeInput($pluginName);
        $pluginID    = sanitizeInput($pluginID);
        $versionCode = sanitizeInput($versionCode);
        $authorName  = sanitizeInput($authorName);
        $description = sanitizeInput($description);
        $userQQ      = sanitizeInput($userQQ);

        // 4) 冲突处理 + 写入数据库（status=0 待审核）
        // 规则：
        // - could_id 自增主键是全局唯一标识（列表、详情、下载都按 could_id 定位）
        // - 同一个 plugin_id 允许存在多个版本（不同 version_code）→ 正常 INSERT
        // - 同一个 plugin_id + 同一个 version_code → 无论作者是谁，都直接驳回（防止版本号语义混乱）
        // - UNIQUE KEY idx_plugin_version(plugin_id, version_code) 继续保留在 DB 层做并发兜底
        $dupCheck = $pdo->prepare("SELECT could_id, upload_qq, plugin_name FROM plugins WHERE plugin_id = ? AND version_code = ? LIMIT 1");
        $dupCheck->execute([$pluginID, $versionCode]);
        $dup = $dupCheck->fetch(PDO::FETCH_ASSOC);

        if ($dup) {
            // 同 ID + 同版本 → 驳回，新文件也删掉
            @unlink($filePath);
            $dupName = htmlspecialchars($dup['plugin_name'] ?? '未知脚本');
            logMessage("用户 {$userQQ} 上传插件被驳回：同ID同版本已存在 plugin_id={$pluginID} version={$versionCode} existing_could_id={$dup['could_id']}", 'WARN');
            jsonResponse(400, "脚本ID【{$pluginID}】的版本号【{$versionCode}】已存在（当前占用：{$dupName}），请修改 info.prop 中的版本号后重新上传");
        }

        // 没有重复 → 正常 INSERT，could_id 自增主键独立区分每条记录
        $stmt = $pdo->prepare("INSERT INTO plugins (plugin_id, plugin_name, version_code, author_name, upload_qq, file_path, download_count, status, description) VALUES (?, ?, ?, ?, ?, ?, 0, 0, ?)");
        $stmt->execute([
            $pluginID,
            $pluginName,
            $versionCode,
            $authorName,
            $userQQ,
            $filePath,
            $description
        ]);
        $couldId = $pdo->lastInsertId();
        logMessage("用户 {$userQQ} 上传插件 {$pluginName} (ID: {$pluginID}, Could ID: {$couldId}) [服务端从ZIP内info.prop读取]", 'INFO');
        jsonResponse(200, '插件上传成功，等待审核', [
            'could_id'     => $couldId,
            'plugin_id'    => $pluginID,
            'plugin_name'  => $pluginName,
            'version_code' => $versionCode,
            'author_name'  => $authorName,
            'description'  => $description
        ]);

    } catch (Exception $e) {
        if (isset($filePath) && file_exists($filePath)) {
            @unlink($filePath);
        }
        error_log("插件上传失败: " . $e->getMessage());
        logMessage("插件上传失败: " . $e->getMessage(), 'ERROR');
        jsonResponse(500, '上传失败：' . $e->getMessage());
    }
}

?>
