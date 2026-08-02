<?php
/**
 * 模块更新检测接口
 * 传入当前版本号，返回是否有新版本
 */

require_once __DIR__ . '/../require.php';

header('Content-Type: application/json; charset=utf-8');

$currentVersion = isset($_GET['version']) ? trim($_GET['version']) : '';
$versionCode = isset($_GET['version_code']) ? intval($_GET['version_code']) : 0;

if (empty($currentVersion) && $versionCode <= 0) {
    echo json_encode([
        'code' => 400,
        'message' => '参数错误'
    ], JSON_UNESCAPED_UNICODE);
    exit;
}

$latestVersion = SYSTEM_VERSION;
$latestVersionCode = intval(str_replace('.', '', $latestVersion));

$hasUpdate = false;
$updateLog = '';
$downloadUrl = '';

$changelog = [
    [
        'version' => $latestVersion,
        'date' => date('Y-m-d'),
        'items' => SYSTEM_UPDATE_LOG
    ]
];

if ($versionCode > 0) {
    $hasUpdate = $latestVersionCode > $versionCode;
} else {
    $hasUpdate = version_compare($latestVersion, $currentVersion, '>');
}

if (!empty($changelog)) {
    $latestEntry = $changelog[0];
    $updateLog = "v{$latestEntry['version']} ({$latestEntry['date']})\n";
    foreach ($latestEntry['items'] as $index => $item) {
        $updateLog .= "• " . $item;
        if ($index < count($latestEntry['items']) - 1) {
            $updateLog .= "\n";
        }
    }
}

echo json_encode([
    'code' => 200,
    'message' => 'success',
    'data' => [
        'has_update' => $hasUpdate,
        'latest_version' => $latestVersion,
        'latest_version_code' => $latestVersionCode,
        'update_log' => $updateLog,
        'changelog' => $changelog,
        'download_url' => 'https://cdn.yuafeng.cn/ly/QEdge_' . $latestVersion . '.apk'
    ]
], JSON_UNESCAPED_UNICODE);
?>
