package me.lengyu.qedge.hook;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.Timer;
import java.util.TimerTask;

import com.tencent.common.app.AppInterface;
import com.tencent.common.app.BaseApplicationImpl;

import me.lengyu.qedge.ui.components.dialogs.UpdateDialog;
import me.lengyu.qedge.ui.components.dialogs.WelcomeDialog;
import me.lengyu.qedge.utils.HttpUtils;
import me.lengyu.qedge.utils.LogUtils;
import me.lengyu.qedge.utils.HostInfo;
import me.lengyu.qedge.utils.QQCurrentEnv;



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
                SharedPreferences prefs = getPreferences();
                if (prefs != null) {
                    currentUin = prefs.getString("current_uin", null);
                }
            }
            
            if (currentUin == null || currentUin.isEmpty()) {
                LogUtils.e("HeartbeatManager", "Uin is null or empty, skip heartbeat");
                return;
            }

            String qqVersion = HostInfo.versionName;
            String moduleVersion = HostInfo.moduleVersionName;

            JSONObject json = new JSONObject();
            json.put("qq", currentUin);
            json.put("qq_version", qqVersion);
            json.put("module_version", moduleVersion);
            json.put("nickname", QQCurrentEnv.getCurrentName());

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
                runOnMainThread(() -> {
                    Toast.makeText(
                            HostInfo.getHostContext(),
                            "您的账号已被拉黑，模块功能已禁用",
                            Toast.LENGTH_LONG
                    ).show();
                });
                disableAllHooks();
            } else if (code == 200) {
                isBanned = false;
                JSONObject data = json.optJSONObject("data");
                if (data != null) {
                    String initialPassword = data.optString("initial_password", null);
                    if (initialPassword != null && !initialPassword.isEmpty()) {
                        lastInitialPassword = initialPassword;
                        SharedPreferences prefs = getPreferences();
                        boolean hasShownWelcome = prefs.getBoolean("has_shown_welcome_" + currentUin, false);
                        
                        prefs.edit()
                                .putString("initial_password_" + currentUin, initialPassword)
                                .apply();
                                
                        if (!hasShownWelcome) {
                            showWelcomeDialog(initialPassword, currentUin);
                        }
                    }
                }
            } else if (code == 0) {
                isBanned = false;
                JSONObject data = json.optJSONObject("data");
                if (data != null) {
                    String version = data.optString("version", "");
                    String updateLog = data.optString("update", "");
                    String apkUrl = data.optString("apk", "");
                    
                    if (!version.isEmpty() && !apkUrl.isEmpty()) {
                        String ignoredVersion = getPreferences().getString("ignored_version", "");
                        if (!version.equals(ignoredVersion)) {
                            showUpdateDialog(version, updateLog, apkUrl);
                        }
                    }
                }
            }
        } catch (Exception e) {
            LogUtils.e("HeartbeatManager", "Parse response failed: " + e.getMessage());
        }
    }

    private void showUpdateDialog(String version, String updateLog, String apkUrl) {
        runOnMainThread(() -> {
            try {
                android.app.Activity activity = QQCurrentEnv.getActivity();
                if (activity != null) {
                    UpdateDialog dialog = new UpdateDialog(
                            activity,
                            version,
                            updateLog,
                            apkUrl,
                            () -> {
                                getPreferences().edit()
                                        .putString("ignored_version", version)
                                        .apply();
                            }
                    );
                    dialog.show();
                }
            } catch (Exception e) {
                LogUtils.e("HeartbeatManager", "Show update dialog failed: " + e.getMessage());
            }
        });
    }

    private void showWelcomeDialog(String initialPassword, String currentUin) {
        runOnMainThread(() -> {
            try {
                android.app.Activity activity = QQCurrentEnv.getActivity();
                if (activity != null) {
                    WelcomeDialog dialog = new WelcomeDialog(activity, currentUin, initialPassword);
                    dialog.show();

                    getPreferences().edit()
                            .putBoolean("has_shown_welcome_" + currentUin, true)
                            .apply();
                }
            } catch (Exception e) {
                LogUtils.e("HeartbeatManager", "Show welcome dialog failed: " + e.getMessage());
            }
        });
    }

    private void disableAllHooks() {
    }

    private String stringToHex(String input) {
        byte[] bytes = input.getBytes(StandardCharsets.UTF_8);
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            hexString.append(String.format("%02X", b));
        }
        return hexString.toString();
    }

    private SharedPreferences getPreferences() {
        Context context = HostInfo.getHostContext();
        if (context != null) {
            return context.getSharedPreferences("QEdge_Heartbeat", Context.MODE_PRIVATE);
        }
        return null;
    }

    private void runOnMainThread(Runnable runnable) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            runnable.run();
        } else {
            new Handler(Looper.getMainLooper()).post(runnable);
        }
    }
}
