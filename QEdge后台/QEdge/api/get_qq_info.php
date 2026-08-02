<?php
/**
 * 获取QQ用户信息接口
 * 通过QQ空间API获取用户昵称和头像
 */

function echo_json($data, $options = JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES) {
    header('Content-Type: application/json; charset=utf-8');
    header('Cache-Control: no-cache, no-store, must-revalidate');
    header('Pragma: no-cache');
    header('Expires: 0');
    echo json_encode($data, $options);
    exit;
}

$uin = $_GET['uin'] ?? $_GET['qq'] ?? '';

// 判断是否有传入QQ号
if (empty($uin)) {
    echo_json(['code' => -3, 'msg' => '缺少QQ号参数']);
}

// 判断QQ号格式合法性
if (!preg_match('/^[1-9][0-9]{4,10}$/', $uin)) {
    echo_json(['code' => -2, 'msg' => 'QQ号格式不合法']);
}

$userInfo = getQQInfo($uin);

// 输出结果
if (empty($userInfo)) {
    echo_json(['code' => -1, 'msg' => '未获取到用户信息，可能该QQ号未开通空间、不存在或者昵称含有不规则字符']);
} else {
    echo_json(['code' => 0, 'msg' => 'success', 'data' => $userInfo]);
}

function getQQInfo($qq) {
    $curl = curl_init();

    curl_setopt_array($curl, [
        CURLOPT_URL => 'https://users.qzone.qq.com/fcg-bin/cgi_get_portrait.fcg?uins=' . $qq,
        CURLOPT_RETURNTRANSFER => true,
        CURLOPT_SSL_VERIFYPEER => false,
        CURLOPT_SSL_VERIFYHOST => false,
        CURLOPT_ENCODING => '',
        CURLOPT_MAXREDIRS => 10,
        CURLOPT_TIMEOUT => 30,
        CURLOPT_HTTP_VERSION => CURL_HTTP_VERSION_1_1,
        CURLOPT_CUSTOMREQUEST => 'GET',
        CURLOPT_POSTFIELDS => '------WebKitFormBoundaryYTwvlk5brGmyD3Mn',
        CURLOPT_HTTPHEADER => [
            'Content-Type: multipart/form-data; boundary=---012345678912345678912312',
        ],
    ]);

    $response = curl_exec($curl);

    // 判断curl是否请求成功
    if (curl_errno($curl)) {
        curl_close($curl);
        return [];
    }
    curl_close($curl);

    // 判断响应是否为空
    if (empty($response)) {
        return [];
    }

    $encode = mb_detect_encoding($response, ['ASCII', 'UTF-8', 'GB2312', 'GBK', 'BIG5']);
    $response = mb_convert_encoding($response, 'UTF-8', $encode);

    // 判断响应长度是否足够
    if (strlen($response) <= 17) {
        return [];
    }

    $jsonp = substr($response, 17, -1);

    // 判断jsonp是否为空
    if (empty($jsonp)) {
        return [];
    }

    $data = json_decode($jsonp, true);

    // 判断json解析是否成功
    if (json_last_error() !== JSON_ERROR_NONE) {
        return [];
    }

    // 判断QQ号对应的数据是否存在
    if (!isset($data[$qq])) {
        return [];
    }

    $userData = $data[$qq];

    // 获取头像URL（索引0）
    $avatar = isset($userData[0]) ? $userData[0] : '';

    // 获取昵称（索引6）
    $nickname = isset($userData[6]) ? $userData[6] : '';
    $nickname = trim($nickname);

    // 判断昵称是否为空或无效
    if (empty($nickname) || $nickname == '.' || $nickname == '暂无') {
        $nickname = '';
    }

    // 如果昵称和头像都为空，返回空
    if (empty($nickname) && empty($avatar)) {
        return [];
    }

    // 处理头像URL
    if (!empty($avatar)) {
        $avatar = str_replace('/100', '/640', $avatar);
    }

    return [
        'qq' => $qq,
        'nickname' => $nickname,
        'qzone_avatar' => $avatar,
        'qq_avatar' => 'https://q1.qlogo.cn/g?b=qq&nk=' . $qq . '&s=640'
    ];
}
