package me.lengyu.qedge.hook;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.Timer;
import java.util.TimerTask;

import com.tencent.common.app.AppInterface;
import com.tencent.common.app.BaseApplicationImpl;

import me.lengyu.qedge.hook.base.HookRegistry;
import me.lengyu.qedge.ui.components.dialogs.WelcomeDialog;
import me.lengyu.qedge.utils.HttpUtils;
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
    private static boolean isBanned = false;

    private Timer heartbeatTimer;
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

    public void startHeartbeat() {
        stopHeartbeat();

        heartbeatTimer = new Timer("QEdge_Heartbeat");
        heartbeatTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                sendHeartbeat();
            }
        }, 0, HEARTBEAT_INTERVAL);
    }

    public void stopHeartbeat() {
        if (heartbeatTimer != null) {
            heartbeatTimer.cancel();
            heartbeatTimer = null;
        }
    }

    private void sendHeartbeat() {
        try {
            String currentUin = null;
            
            try {
                AppInterface app = (AppInterface) BaseApplicationImpl.getApplication().peekAppRuntime();
                if (app != null) {
                    currentUin = app.getCurrentAccountUin();
                }
            } catch (Throwable e) {
                LogUtils.e("HeartbeatManager", "Failed to get uin from BaseApplicationImpl: " + e.getMessage());
            }
            
            if (currentUin == null || currentUin.isEmpty()) {
                currentUin = ModuleConfig.INSTANCE.getString("heartbeat_current_uin", null);
            }
            
            if (currentUin == null || currentUin.isEmpty()) {
                LogUtils.e("HeartbeatManager", "Uin is null or empty, skip heartbeat");
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

            String response = HttpUtils.post(API_URL, hexData);
            if (response != null) {
                parseResponse(response, currentUin);
            }
        } catch (Exception e) {
            LogUtils.e("HeartbeatManager", "Heartbeat failed: " + e.getMessage());
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
                        lastInitialPassword = initialPassword;
                        boolean hasShownWelcome = ModuleConfig.INSTANCE.getBoolean("has_shown_welcome_" + currentUin, false);
                        
                        ModuleConfig.INSTANCE.putString("initial_password_" + currentUin, initialPassword);
                                
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
