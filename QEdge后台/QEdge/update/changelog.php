<?php
/**
 * 更新日志接口（JSON格式）
 */

require_once __DIR__ . '/../require.php';

header('Content-Type: application/json; charset=utf-8');

$latestVersion = SYSTEM_VERSION;

$changelog = [
    [
        'version' => $latestVersion,
        'date' => date('Y-m-d'),
        'items' => SYSTEM_UPDATE_LOG
    ]
];

echo json_encode([
    'code' => 200,
    'message' => 'success',
    'data' => [
        'latest_version' => $latestVersion,
        'changelog' => $changelog
    ]
], JSON_UNESCAPED_UNICODE);
?>
