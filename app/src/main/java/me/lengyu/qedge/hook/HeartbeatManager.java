package me.lengyu.qedge.hook;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Timer;
import java.util.TimerTask;

import com.tencent.common.app.AppInterface;
import com.tencent.common.app.BaseApplicationImpl;

import me.lengyu.qedge.hook.base.HookRegistry;
import me.lengyu.qedge.ui.components.dialogs.WelcomeDialog;
import me.lengyu.qedge.utils.LogUtils;
import me.lengyu.qedge.utils.ModuleConfig;
import me.lengyu.qedge.utils.HostInfo;
import me.lengyu.qedge.utils.Toasts;
import me.lengyu.qedge.utils.qq.QQCurrentEnv;


/**
 * @Author 冷雨
 * @Description 心跳管理类
 */
public class HeartbeatManager {

    private static final String API_URL = "https://v.yuafeng.cn/QEdge/heartbeat/index.php";
    private static final long HEARTBEAT_INTERVAL = 600000;

    private static HeartbeatManager instance;
    private static volatile boolean isBanned = false;

    private volatile Timer heartbeatTimer;
    private String lastInitialPassword;

    private HeartbeatManager() {
    }

    public static synchronized HeartbeatManager getInstance() {
        if (instance == null) {
            instance = new HeartbeatManager();
        }
        return instance;
    }

    public static boolean isBanned() {
        return isBanned;
    }

    public synchronized void startHeartbeat() {
        stopHeartbeat();

        // 启动留痕：此前心跳从启动到发送全程无日志，无法判断上游是否调用过来，这里补一条生命周期日志
        LogUtils.i("HeartbeatManager", "startHeartbeat: 心跳已启动"
                + " | qq=" + HostInfo.versionName
                + " | module=" + HostInfo.moduleVersionName
                + " | interval=" + HEARTBEAT_INTERVAL + "ms");

        heartbeatTimer = new Timer("QEdge_Heartbeat");
        heartbeatTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                sendHeartbeat();
            }
        }, 0, HEARTBEAT_INTERVAL);
    }

    public synchronized void stopHeartbeat() {
        if (heartbeatTimer != null) {
            heartbeatTimer.cancel();
            heartbeatTimer = null;
        }
    }

    private void sendHeartbeat() {
        long start = System.currentTimeMillis();
        String currentUin = null;
        String uinSource = "none";

        try {
            try {
                AppInterface app = (AppInterface) BaseApplicationImpl.getApplication().peekAppRuntime();
                if (app != null) {
                    currentUin = app.getCurrentAccountUin();
                    if (currentUin != null && !currentUin.isEmpty()) {
                        uinSource = "host";
                    }
                }
            } catch (Throwable e) {
                LogUtils.e("HeartbeatManager", "Failed to get uin from BaseApplicationImpl: " + e.getMessage());
            }

            if (currentUin == null || currentUin.isEmpty()) {
                currentUin = ModuleConfig.INSTANCE.getString("heartbeat_current_uin", null);
                if (currentUin != null && !currentUin.isEmpty()) {
                    uinSource = "config";
                }
            }

            if (currentUin == null || currentUin.isEmpty()) {
                // 取不到 UIN 时首页用户卡片就没有可用的 key，必然显示"未登录"，这里把获取过程完整打出来
                LogUtils.e("HeartbeatManager", "心跳失败：未取到当前 UIN，跳过本次心跳"
                        + " | uin来源=" + uinSource
                        + " | qq=" + HostInfo.versionName
                        + " | module=" + HostInfo.moduleVersionName
                        + " | url=" + API_URL);
                return;
            }

            String qqVersion = HostInfo.versionName;
            String moduleVersion = HostInfo.moduleVersionName;
            String nickname = QQCurrentEnv.getCurrentName();

            // 记录本地环境信息（跨进程可读，供模块首页用户卡片展示）
            UserData.updateEnv(currentUin, moduleVersion, qqVersion, nickname);

            JSONObject json = new JSONObject();
            json.put("qq", currentUin);
            json.put("qq_version", qqVersion);
            json.put("module_version", moduleVersion);
            json.put("nickname", nickname);

            String hexData = stringToHex(json.toString());

            sendHeartbeatRequest(API_URL, hexData, currentUin, uinSource, start);
        } catch (Exception e) {
            // 构造请求体等环节异常：同样带上完整上下文与堆栈，方便定位
            LogUtils.e("HeartbeatManager", "心跳失败：构造请求异常"
                    + " | uin=" + currentUin + "(" + uinSource + ")"
                    + " | qq=" + HostInfo.versionName
                    + " | module=" + HostInfo.moduleVersionName
                    + " | url=" + API_URL
                    + " | 耗时=" + (System.currentTimeMillis() - start) + "ms"
                    + " | 原因=" + e.getClass().getSimpleName() + ": " + e.getMessage());
            LogUtils.e("HeartbeatManager", e);
        }
    }

    /**
     * 心跳 POST 请求。成功（HTTP 200 且响应非空）不打任何日志，直接交给 parseResponse；
     * 失败（非 200 / 空响应 / 网络异常）输出带完整上下文的详细日志。
     *
     * <p>这里不用 HttpUtils.post 是因为它把 IOException 与状态码全部吞掉只剩 null，
     * 无法区分超时 / DNS 失败 / 非 200，定位不了"部分用户首页显示未登录"。
     */
    private void sendHeartbeatRequest(String urlStr, String body, String uin, String uinSource, long start) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlStr);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(30000);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            connection.setDoOutput(true);

            try (OutputStream os = connection.getOutputStream()) {
                os.write(body.getBytes("UTF-8"));
                os.flush();
            }

            int code = connection.getResponseCode();
            long cost = System.currentTimeMillis() - start;

            if (code != HttpURLConnection.HTTP_OK) {
                LogUtils.e("HeartbeatManager", "心跳失败：HTTP " + code
                        + " | uin=" + uin + "(" + uinSource + ")"
                        + " | qq=" + HostInfo.versionName
                        + " | module=" + HostInfo.moduleVersionName
                        + " | url=" + urlStr
                        + " | 耗时=" + cost + "ms"
                        + " | 返回=" + readQuietly(connection.getErrorStream()));
                return;
            }

            String response = readQuietly(connection.getInputStream());
            if (response == null || response.isEmpty()) {
                LogUtils.e("HeartbeatManager", "心跳失败：响应为空"
                        + " | uin=" + uin + "(" + uinSource + ")"
                        + " | qq=" + HostInfo.versionName
                        + " | module=" + HostInfo.moduleVersionName
                        + " | url=" + urlStr
                        + " | 耗时=" + cost + "ms");
                return;
            }

            // 请求成功：不输出日志
            parseResponse(response, uin);
        } catch (IOException e) {
            LogUtils.e("HeartbeatManager", "心跳失败：网络异常"
                    + " | uin=" + uin + "(" + uinSource + ")"
                    + " | qq=" + HostInfo.versionName
                    + " | module=" + HostInfo.moduleVersionName
                    + " | url=" + urlStr
                    + " | 耗时=" + (System.currentTimeMillis() - start) + "ms"
                    + " | 原因=" + e.getClass().getSimpleName() + ": " + e.getMessage());
            LogUtils.e("HeartbeatManager", e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /** 读取流内容，出错或流为 null 时返回 null，绝不抛异常影响心跳主流程。 */
    private String readQuietly(InputStream is) {
        if (is == null) return null;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        } catch (Exception ignored) {
            return null;
        }
    }

    private void parseResponse(String response, String currentUin) {
        try {
            JSONObject json = new JSONObject(response);
            int code = json.optInt("code", 0);

            if (code == 403) {
                isBanned = true;
                LogUtils.e("HeartbeatManager", "Account banned!");
                stopHeartbeat();
                HookRegistry.disableAllHooks();
                JSONObject data = json.optJSONObject("data");
                String reason = (data != null) ? data.optString("reason", "") : "";
                String message = "您的账号已被拉黑" + (reason.isEmpty() ? "" : "，原因：" + reason);
                Toasts.toast(message);
            } else if (code == 200) {
                isBanned = false;
                JSONObject data = json.optJSONObject("data");
                if (data != null) {
                    String initialPassword = data.optString("initial_password", null);
                    if (initialPassword != null && !initialPassword.isEmpty()) {
                        // 口令只留在内存中，直接传给欢迎弹窗，不落盘（外部存储 JSON 为明文）
                        lastInitialPassword = initialPassword;
                        boolean hasShownWelcome = ModuleConfig.INSTANCE.getBoolean("has_shown_welcome_" + currentUin, false);

                        if (!hasShownWelcome) {
                            showWelcomeDialog(initialPassword, currentUin);
                        }
                    }
                    // 心跳下发的用户数据存入内存（随进程存活，用时直接取，不落盘）
                    UserData.update(data, currentUin);
                }
            } else if (code == 0) {
                isBanned = false;
                JSONObject data = json.optJSONObject("data");
                if (data != null) {
                    String version = data.optString("version", "");
                    String updateLog = data.optString("update", "");
                    String apkUrl = data.optString("apk", "");
                    
                    if (!version.isEmpty() && !apkUrl.isEmpty()) {
                        String ignoredVersion = ModuleConfig.INSTANCE.getString("ignored_version", "");
                        // 已经装上这个版本了就不再提示：只按"服务端版本 > 本机安装版本"判定
                        if (isNewerVersion(version, HostInfo.moduleVersionName) && !version.equals(ignoredVersion)) {
                            // 只落盘更新数据，进入设置页时再弹更新日志
                            UserData.setUpdateInfo(version, updateLog, apkUrl);
                            // 温柔提醒，不打断使用
                            Toasts.toast("发现新版本 " + version + "，进入设置页可查看更新日志");
                        }
                    }
                }
            } else {
                // HTTP 请求本身是成功的，但服务端返回了未处理的业务码：用户数据不会被写入，
                // 首页用户卡片拿不到昵称就会显示"未登录"，需要日志留痕才能定位
                LogUtils.e("HeartbeatManager", "心跳异常：服务端返回未处理的 code=" + code
                        + " | uin=" + currentUin
                        + " | 原始响应=" + response);
            }
        } catch (Exception e) {
            LogUtils.e("HeartbeatManager", "Parse response failed: " + e.getMessage());
        }
    }

    /**
     * 服务端版本是否比本机安装的模块版本新。
     * 只按分段数字比较（1.0.10 > 1.0.9），任一版本号解析不出数字时保守返回 false，
     * 宁可少提示一次，也不要把"已经装上的版本"再报一遍更新。
     */
    public static boolean isNewerVersion(String remoteVersion, String installedVersion) {
        int[] remote = parseVersion(remoteVersion);
        int[] installed = parseVersion(installedVersion);
        if (remote.length == 0 || installed.length == 0) return false;

        int length = Math.max(remote.length, installed.length);
        for (int i = 0; i < length; i++) {
            int r = i < remote.length ? remote[i] : 0;
            int c = i < installed.length ? installed[i] : 0;
            if (r != c) return r > c;
        }
        return false;
    }

    /** 把 "0.2.8" / "v1.0.10-beta" 解析成 {0,2,8} / {1,0,10}，非数字段记 0。 */
    private static int[] parseVersion(String version) {
        if (version == null) return new int[0];
        String trimmed = version.trim();
        if (trimmed.isEmpty()) return new int[0];

        String[] parts = trimmed.split("\\.");
        int[] numbers = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            StringBuilder digits = new StringBuilder();
            for (int j = 0; j < parts[i].length(); j++) {
                char ch = parts[i].charAt(j);
                if (ch >= '0' && ch <= '9') digits.append(ch);
                else if (digits.length() > 0) break;
            }
            try {
                numbers[i] = digits.length() == 0 ? 0 : Integer.parseInt(digits.toString());
            } catch (NumberFormatException e) {
                numbers[i] = 0;
            }
        }
        return numbers;
    }

    private void showWelcomeDialog(String initialPassword, String currentUin) {
        runOnMainThread(() -> {
            try {
                android.app.Activity activity = QQCurrentEnv.getActivity();
                if (activity != null) {
                    WelcomeDialog dialog = new WelcomeDialog(activity, currentUin, initialPassword);
                    dialog.show();

                    ModuleConfig.INSTANCE.putBoolean("has_shown_welcome_" + currentUin, true);
                }
            } catch (Exception e) {
                LogUtils.e("HeartbeatManager", "Show welcome dialog failed: " + e.getMessage());
            }
        });
    }

    private String stringToHex(String input) {
        byte[] bytes = input.getBytes(StandardCharsets.UTF_8);
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            hexString.append(String.format("%02X", b));
        }
        return hexString.toString();
    }

    private void runOnMainThread(Runnable runnable) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            runnable.run();
        } else {
            new Handler(Looper.getMainLooper()).post(runnable);
        }
    }
}
