<?php
require_once __DIR__ . '/../require.php';

$version = SYSTEM_VERSION;

$changelog = [
    [
        'version' => $version,
        'date' => date('Y-m-d'),
        'items' => SYSTEM_UPDATE_LOG
    ]
];

// 逐条输出为文本
foreach ($changelog as $entry) {
    echo "版本: " . $entry['version'] . "\n";
    echo "日期: " . $entry['date'] . "\n";
    echo "更新内容:\n";
    foreach ($entry['items'] as $item) {
        echo "- " . $item . "\n";
    }
    echo "\n";
}
?>