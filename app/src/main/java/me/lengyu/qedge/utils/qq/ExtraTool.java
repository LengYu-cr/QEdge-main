package me.lengyu.qedge.utils.qq;

import android.content.Context;
import android.os.Bundle;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;
import java.lang.reflect.Method;
import com.tencent.image.utils.PicFormatUtils;
import java.io.FileInputStream;
import com.tencent.mobileqq.transfile.api.ITransFileController;
import com.tencent.mobileqq.transfile.TransferRequest;
import mqq.app.AppRuntime;
import com.tencent.mobileqq.troop.avatar.TroopAvatarController;
import com.tencent.mobileqq.troop.avatar.TroopPhotoController;
import com.tencent.mobileqq.troop.avatar.api.ITroopPhotoUtilsApi;
import com.tencent.mobileqq.troop.activity.TroopAvatarWallEditActivity;
import com.tencent.mobileqq.app.BaseActivity;
import com.tencent.mobileqq.app.QQAppInterface;
import com.tencent.mobileqq.qroute.QRoute;
import com.tencent.mobileqq.mini.api.IMiniCallback;
import com.tencent.mobileqq.mini.share.MiniArkShareAsyncManager;
import com.tencent.mobileqq.videocodec.ffmpeg.FFmpeg;
import com.tencent.mobileqq.videocodec.ffmpeg.FFmpegCommandAlreadyRunningException;
import com.tencent.mobileqq.videocodec.ffmpeg.FFmpegExecuteResponseCallback;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import me.lengyu.qedge.utils.qq.QQCurrentEnv;
import me.lengyu.qedge.utils.HostInfo;
import me.lengyu.qedge.plugin.bean.JointGroup;
import me.lengyu.qedge.plugin.bean.BlackUser;
import me.lengyu.qedge.utils.proto.PacketHelper;
import me.lengyu.qedge.utils.proto.packetListener;
import me.lengyu.qedge.utils.Toasts;
import me.lengyu.qedge.utils.LogUtils;
import me.lengyu.qedge.common.ModuleScope;
import me.lengyu.qedge.utils.HttpUtils;
import me.lengyu.qedge.utils.json.ProtoData;
/**
 * @Author 冷雨
 * @Description 额外工具类
 */
public class ExtraTool {

    public interface UploadCallback {
        void onResult(boolean success);
    }

    public static boolean uploadAvatar(String path){
        AppRuntime appRuntime = (AppRuntime) QQCurrentEnv.getAppRuntime();
        if (appRuntime == null) return false;
        ITransFileController control = appRuntime.getRuntimeService(ITransFileController.class, "");
        TransferRequest transferRequest = new TransferRequest();
        transferRequest.mIsUp = true;
        transferRequest.mLocalPath = path;
        transferRequest.mFileType = 22;
        boolean transferAsync=control.transferAsync(transferRequest);
        return transferAsync;
    }

    public static boolean uploadCover(String path){
        AppRuntime appRuntime = (AppRuntime) QQCurrentEnv.getAppRuntime();
        if (appRuntime == null) return false;
        ITransFileController control = appRuntime.getRuntimeService(ITransFileController.class, "");
        TransferRequest transferRequest = new TransferRequest();
        transferRequest.mIsUp = true;
        transferRequest.mLocalPath = path;
        transferRequest.mFileType = 35;
        boolean transferAsync=control.transferAsync(transferRequest);
        return transferAsync;
    }

    public static void UploadTroopAvatar(String qun, String filepath, UploadCallback callback) {
        if (BaseActivity.sTopActivity == null) {
            if (callback != null) callback.onResult(false);
            return;
        }

        BaseActivity.sTopActivity.runOnUiThread(() -> {
            boolean result = false;
            try {
                TroopAvatarWallEditActivity troopAvatarActivity = new TroopAvatarWallEditActivity();

                Bundle bundle = new Bundle();
                bundle.putString("troopUin", qun);
                bundle.putInt("type", 1);

                QQAppInterface app = QQCurrentEnv.getQQAppInterface();
                Context context = HostInfo.getContext();

                TroopAvatarController troopAvatarController = new TroopAvatarController(
                        context,
                        troopAvatarActivity,
                        app,
                        bundle
                );

                String tt = QRoute.api(ITroopPhotoUtilsApi.class).getClipStr(0, 0, 0, 0);
                result = troopAvatarController.A(filepath, tt);

            } catch (Exception e) {
                result = false;
            } finally {
                if (callback != null) {
                    callback.onResult(result);
                }
            }
        });
    }

    public static void UploadTroopCover(String qun, String filepath, UploadCallback callback) {
        if (BaseActivity.sTopActivity == null) {
            if (callback != null) callback.onResult(false);
            return;
        }

        BaseActivity.sTopActivity.runOnUiThread(() -> {
            boolean result = false;
            try {
                TroopAvatarWallEditActivity troopAvatarActivity = new TroopAvatarWallEditActivity();

                Bundle bundle = new Bundle();
                bundle.putString("troopUin", qun);
                bundle.putInt("type", 1);

                QQAppInterface app = QQCurrentEnv.getQQAppInterface();
                Context context = HostInfo.getContext();

                TroopPhotoController troopPhotoController = new TroopPhotoController(
                        context,
                        troopAvatarActivity,
                        app,
                        bundle
                );

                String tt = QRoute.api(ITroopPhotoUtilsApi.class).getClipStr(0, 0, 0, 0);
                result = troopPhotoController.A(filepath, tt);

            } catch (Exception e) {
                result = false;
            } finally {
                if (callback != null) {
                    callback.onResult(result);
                }
            }
        });
    }

    public static boolean uploadTroopAvatar(String qun, String filepath) {
        if (BaseActivity.sTopActivity == null) {
            return false;
        }
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean result = new AtomicBoolean(false);
        UploadTroopAvatar(qun, filepath, success -> {
            result.set(success);
            latch.countDown();
        });
        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return result.get();
    }

    public static String uploadImage(String filepath) {
        
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean result = new AtomicBoolean(false);
        AtomicReference<String> imageUrl = new AtomicReference<>();
        MiniArkShareAsyncManager.performUploadArkShareImage(filepath, new IMiniCallback() {
            @Override
            public void onCallbackResult(boolean success, Bundle bundle) {
                if (bundle == null) {
                    result.set(false);
                    latch.countDown();
                    return;
                }
                imageUrl.set(bundle.getString("imageUrl"));
                result.set(success);
                latch.countDown();
            }
        });
        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (!result.get() || imageUrl.get() == null || imageUrl.get().isEmpty()) {
            return "";
        }
        return imageUrl.get();
    }

    public static boolean uploadTroopCover(String qun, String filepath) {
        if (BaseActivity.sTopActivity == null) {
            return false;
        }
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean result = new AtomicBoolean(false);
        UploadTroopCover(qun, filepath, success -> {
            result.set(success);
            latch.countDown();
        });
        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return result.get();
    }





    public static void changeMyName(String title) {
        try {
            JSONObject json = new JSONObject();
            json.put("1", 4394);
            json.put("2", 2);
            json.put("12", 0);
        
            JSONObject json2 = new JSONObject();
            json2.put("1", Long.parseLong(QQCurrentEnv.getCurrentUin()));
        
            JSONObject json3 = new JSONObject();
            json3.put("1", 20002);
            json3.put("2", title);
            json2.put("2", json3);
            json.put("4", json2);
        
            PacketHelper.sendPacket("OidbSvcTrpcTcp.0x112a_2", json, new packetListener() {
                @Override
                public void onResult(boolean success, JSONObject json) {
                    if (success) {
                        // Toasts.toast("昵称修改成功");
                    } else {
                        Toasts.toast("昵称修改失败");
                    }
                }
            }); 
        
        } catch (Exception e) {
            Toasts.toast("昵称修改失败(报错)");
            LogUtils.e("ExtraTool", "昵称修改失败: " + e.getMessage());
            // sendMsg(myUin, "ProtoData报错: " + e.getMessage(), 1);
        }
    }

    public static boolean sendPacket(String cmd, String json) {
        try {
            final CountDownLatch latch = new CountDownLatch(1);
            
            PacketHelper.sendPacket(cmd, new JSONObject(json), new packetListener() {
                @Override
                public void onResult(boolean success, JSONObject json) {
                    // 回调触发，释放锁
                    latch.countDown();
                }
            }); 
            
            // 阻塞等待，最多 30 秒
            latch.await(30, TimeUnit.SECONDS);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static String fetchPacket(String cmd, String json) {
        try {
            return fetchPacket(cmd, new JSONObject(json));
        } catch (Exception e) {
            try {
                JSONObject json1 = new JSONObject();
                json1.put("1", cmd);
                json1.put("2", e.getMessage());
                json1.put("12", 0);
                return json1.toString();
            } catch (Exception ex) {
                return "";
            }
        }
    }
    
    public static boolean sendPacket(String cmd, JSONObject json) {
        try {
            final CountDownLatch latch = new CountDownLatch(1);
            
            PacketHelper.sendPacket(cmd, json, new packetListener() {
                @Override
                public void onResult(boolean success, JSONObject json) {
                    // 回调触发，释放锁
                    latch.countDown();
                }
            }); 
            
            // 阻塞等待，最多 30 秒
            latch.await(30, TimeUnit.SECONDS);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static String fetchPacket(String cmd, JSONObject json) {
        final String[] resultHolder = {""};
        final Object lock = new Object();
        final boolean[] done = {false};
        
        ModuleScope.launchIOJava("ExtraTool", () -> {
            try {
                final CountDownLatch latch = new CountDownLatch(1);
                
                PacketHelper.sendPacket(cmd, json, new packetListener() {
                    @Override
                    public void onResult(boolean success, JSONObject jsonResult) {
                        try {
                            if (jsonResult != null) {
                                resultHolder[0] = jsonResult.toString();
                            }
                        } catch (Exception e) {
                            LogUtils.e(e);
                        } finally {
                            latch.countDown();
                        }
                    }
                });
                
                latch.await(30, TimeUnit.SECONDS);
            } catch (Exception e) {
                try {
                    JSONObject json1 = new JSONObject();
                    json1.put("1", cmd);
                    json1.put("2", e.getMessage());
                    json1.put("12", 0);
                    resultHolder[0] = json1.toString();
                } catch (Exception ex) {
                    resultHolder[0] = "";
                }
            } finally {
                synchronized (lock) {
                    done[0] = true;
                    lock.notify();
                }
            }
        });
        
        synchronized (lock) {
            while (!done[0]) {
                try {
                    lock.wait(30000);
                    break;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        
        return resultHolder[0];
    }

    public static void groupClockIn(String qun) {
        try {
            JSONObject packet = new JSONObject();
            packet.put("1", 3767);
            packet.put("2", 1);
            packet.put("3", 0);

            JSONObject inner4 = new JSONObject();
            JSONObject inner2 = new JSONObject();
            inner2.put("1", QQCurrentEnv.getCurrentUin());
            inner2.put("2", qun);
            inner2.put("3", HostInfo.versionName);
            inner4.put("2", inner2);
            packet.put("4", inner4);

            packet.put("6", "android " + HostInfo.versionName);
            PacketHelper.sendPacket("OidbSvc.0xb77_9", packet, new packetListener() {
                @Override
                public void onResult(boolean success, JSONObject json) {
                    if (success) {
                        // Toasts.toast("群打卡成功");
                    } else {
                        Toasts.toast("群打卡失败");
                    }
                }
            }); 
        
        } catch (Exception e) {
            Toasts.toast("群打卡失败(报错)");
            LogUtils.e("ExtraTool", "群打卡失败: " + e.getMessage());
        }
    }

    public static void setMemberTitle(String qun,String uin, String title) {
        try {
            JSONObject packet = new JSONObject();
            packet.put("1", 2300);
            packet.put("2", 2);

            JSONObject inner4 = new JSONObject();
            inner4.put("1", Long.parseLong(qun));

            JSONObject inner3 = new JSONObject();
            inner3.put("1", Long.parseLong(uin));
            inner3.put("5", title);
            inner3.put("6", 4294967295L);
            inner3.put("7", title);

            inner4.put("3", inner3);
            packet.put("4", inner4);

            PacketHelper.sendPacket("OidbSvc.0x8fc_2", packet, new packetListener() {
                @Override
                public void onResult(boolean success, JSONObject json) {
                    if (success) {
                        // Toasts.toast("设置群员头衔成功");
                    } else {
                        Toasts.toast("设置群员头衔失败");
                    }
                }
            }); 
        
        } catch (Exception e) {
            Toasts.toast("设置群员头衔失败(报错)");
            LogUtils.e("ExtraTool", "设置群员头衔失败: " + e.getMessage());
        }
    }

    public static void changeGroupName(String qun, String name) {
        try {
            JSONObject packet = new JSONObject();
            packet.put("1", 2202);
            packet.put("2", 0);

            JSONObject inner4 = new JSONObject();
            inner4.put("1", Long.parseLong(qun));

            JSONObject inner2 = new JSONObject();
            inner2.put("3", name);
            inner4.put("2", inner2);
            inner4.put("4", 0);

            packet.put("4", inner4);
            packet.put("12", 0);


            PacketHelper.sendPacket("OidbSvcTrpcTcp.0x89a_0", packet, new packetListener() {
                @Override
                public void onResult(boolean success, JSONObject json) {
                    if (success) {
                        // Toasts.toast("设置群名称成功");
                    } else {
                        Toasts.toast("设置群名称失败");
                    }
                }
            }); 
        
        } catch (Exception e) {
            Toasts.toast("设置群名称失败(报错)");
            LogUtils.e("ExtraTool", "设置群名称失败: " + e.getMessage());
        }
    }

    public static void sendPai(String qun, String uin) {
        try {
            JSONObject packet = new JSONObject();
            packet.put("1", 3795);
            packet.put("2", 1);
            packet.put("3", 0);

            JSONObject inner4 = new JSONObject();
            inner4.put("1", Long.parseLong(uin));
            inner4.put("2", Long.parseLong(qun));
            inner4.put("6", 0);

            packet.put("4", inner4);
            packet.put("6", "android " + HostInfo.versionName);

            PacketHelper.sendPacket("OidbSvc.0xed3", packet, new packetListener() {
                @Override
                public void onResult(boolean success, JSONObject json) {
                    if (success) {
                        // Toasts.toast("拍成功");
                    } else {
                        Toasts.toast("拍失败");
                    }
                }
            }); 
        
        } catch (Exception e) {
            Toasts.toast("拍失败(报错)");
            LogUtils.e("ExtraTool", "拍失败: " + e.getMessage());
        }
    }
    
    public static void setGroupShortcutBar(String name, String desc, String link, String qun, String cover) {
        try {
            JSONObject packet = new JSONObject();
            packet.put("1", 38667);
            packet.put("2", 0);
            packet.put("3", 0);

            JSONObject inner4 = new JSONObject();
            inner4.put("1", Long.parseLong(qun));

            JSONObject inner2 = new JSONObject();
            inner2.put("1", String.valueOf(1000000000 + new Random().nextInt(100000000)));
            inner2.put("3", 3);
            inner2.put("4", name);
            inner2.put("5", cover);
            inner2.put("6", desc);
            inner2.put("7", link);

            inner4.put("2", inner2);
            inner4.put("3", 1);

            packet.put("4", inner4);
            packet.put("6", "android " + HostInfo.versionName);

            PacketHelper.sendPacket("OidbSvcTrpcTcp.0x970b_0", packet, new packetListener() {
                @Override
                public void onResult(boolean success, JSONObject json) {
                    if (success) {
                        // Toasts.toast("设置群快捷栏显示成功");
                    } else {
                        Toasts.toast("设置群快捷栏显示失败");
                    }
                }
            });

        } catch (Exception e) {
            Toasts.toast("设置群快捷栏显示失败(报错)");
            LogUtils.e("ExtraTool", "设置群快捷栏显示失败: " + e.getMessage());
        }
    }

     public static void setGroupLocation(String qun, String location) {
        try {
            JSONObject packet = new JSONObject();
            packet.put("1", 2202);
            packet.put("2", 0);

            JSONObject inner4 = new JSONObject();
            inner4.put("1", Long.parseLong(qun));

            JSONObject inner2 = new JSONObject();
            JSONObject inner20 = new JSONObject();
            inner20.put("1", 10120);
            inner20.put("2", 0);
            inner20.put("3", 0);
            inner20.put("4", location);
            
            inner2.put("20", inner20);
            inner4.put("2", inner2);
            inner4.put("4", 0);

            packet.put("4", inner4);
            packet.put("12", 0);

            PacketHelper.sendPacket("OidbSvcTrpcTcp.0x89a_0", packet, new packetListener() {
                @Override
                public void onResult(boolean success, JSONObject json) {
                    if (success) {
                        // Toasts.toast("设置成功");
                    } else {
                        Toasts.toast("设置群地点失败");
                    }
                }
            });

        } catch (Exception e) {
            Toasts.toast("设置群地点失败(报错)");
            LogUtils.e("ExtraTool", "设置群地点失败: " + e.getMessage());
        }
    }

    public static void sendMusic(String platform, String uin, String title, String desc, String detailUrl, String audio, String img, int mtype) {
        try {
            String packageName = "";
            String appid = "";
            String token = "";

            if (platform.equals("qq")) {
                packageName = "com.tencent.qqmusic";
                appid = "100497308";
                token = "cbd27cd7c861227d013a25b2d10f0799";
            } else if (platform.equals("wy")) {
                packageName = "com.netease.cloudmusic";
                appid = "100495085";
                token = "da6b069da1e2982db3e386233f68d76d";
            } else if (platform.equals("kg")) {
                packageName = "com.kugou.android";
                appid = "205141";
                token = "fe4a24d80fcf253a00676a808f62c2c6";
            } else if (platform.equals("kw")) {
                packageName = "cn.kuwo.player";
                appid = "100243533";
                token = "a6b745bf24a2c277527716f6f36eb68d";
            } else if (platform.equals("bd")) {
                packageName = "cn.wenyu.bodian";
                appid = "101904722";
                token = "5380ca99d2bea3173b0b6e52cff44b45";
            } else if (platform.equals("mg")) {
                packageName = "cmccwm.mobilemusic";
                appid = "1101053067";
                token = "6cdc72a439cef99a3418d2a78aa28c73";
            } else if (platform.equals("QQ")) {
                packageName = "com.tencent.mobileqq";
                appid = "35055";
                token = "a6b745bf24a2c277527716f6f36eb68d";
            }

            if (audio == null || audio.isEmpty()) {
                audio = "https://cdn.yuafeng.cn/ly/playerror.mp3";
            }

            JSONObject json = new JSONObject();
            json.put("1", 2935);
            json.put("2", 9);
            json.put("6", "android " + HostInfo.versionName);

            JSONObject obj4 = new JSONObject();
            obj4.put("1", Long.parseLong(appid));
            obj4.put("2", 1);
            obj4.put("3", 4);
            obj4.put("20", 0);
            obj4.put("21", 0);
            obj4.put("11", Long.parseLong(uin));

            if (mtype == 2) {
                obj4.put("10", 3 - mtype);
            } else if (mtype == 1) {
                obj4.put("10", 1 - mtype);
            } else {
                obj4.put("10", mtype);
            }

            JSONObject obj5 = new JSONObject();
            obj5.put("1", 1);
            obj5.put("2", "0.0.0");
            obj5.put("3", packageName);
            obj5.put("4", token);
            obj4.put("5", obj5);

            JSONObject obj7 = new JSONObject();
            obj7.put("15", PacketHelper.rand(100, 99999));
            obj4.put("7", obj7);

            JSONObject obj12 = new JSONObject();
            obj12.put("16", audio);
            obj12.put("10", title);
            obj12.put("11", desc);
            obj12.put("13", detailUrl);
            obj12.put("14", img);
            obj4.put("12", obj12);

            obj4.put("22", new JSONObject());
            obj4.put("23", new JSONObject());
            json.put("4", obj4);

            PacketHelper.sendPacket("OidbSvc.0xb77_9", json, new packetListener() {
                @Override
                public void onResult(boolean success, JSONObject json) {
                    if (!success) {
                        Toasts.toast("发送音乐卡片失败");
                    }
                }
            });

        } catch (Exception e) {
            Toasts.toast("发送音乐卡片失败(报错)");
            LogUtils.e("ExtraTool", "发送音乐卡片失败: " + e.getMessage());
        }
    }

    /**
     * 发送小程序卡片 / Ark 卡片
     * 参数语义与用户提供的小程序卡片协议结构一一对应（OidbSvc.0xb77_9 通用）
     *
     * @param packageName 客户端包名 (obj4.5.3)，例如 com.tencent.mobileqq
     * @param appid       外置 AppID (obj4.1)，例如 35055
     * @param token       应用签名 token (obj4.5.4)
     * @param arkAppId    Ark 应用 ID (obj4.16.1)
     * @param miniAppPath 小程序/页面路径 (obj4.16.2)，可传空串
     * @param uin         目标 QQ 号或群号
     * @param appName     应用名称，同时填充到卡片显示名和 Ark 内部字段
     * @param desc        卡片提示文字，同时填充到 obj12 和 obj16
     * @param detailUrl   跳转链接 URL
     * @param img         预览图/封面图 URL
     * @param mtype       同 sendMusic 的 mtype：1=好友，2=群聊（内部自动转换为协议要求的 0/1）
     * @param arkJson     Ark 内部具体 JSON 数据 (obj4.16.10)，可为 null
     */
    public static void sendMiniApp(String packageName, String appid, String token,
                                   String arkAppId, String miniAppPath,
                                   String uin, String appName, String desc,
                                   String detailUrl, String img, int mtype,
                                   JSONObject arkJson) {
        try {
            if (packageName == null || packageName.isEmpty()) packageName = "com.tencent.mobileqq";
            if (appid == null || appid.isEmpty()) appid = "35055";
            if (token == null || token.isEmpty()) token = "a6b745bf24a2c277527716f6f36eb68d";
            if (arkAppId == null) arkAppId = "";
            if (miniAppPath == null) miniAppPath = "";
            if (appName == null) appName = "";
            if (desc == null) desc = "";
            if (detailUrl == null) detailUrl = "";
            if (img == null) img = "";

            JSONObject json = new JSONObject();
            json.put("1", 2935);
            json.put("2", 9);
            json.put("6", "android " + HostInfo.versionName);

            JSONObject obj4 = new JSONObject();
            obj4.put("1", Long.parseLong(appid));
            obj4.put("2", 1);
            obj4.put("3", 8);
            obj4.put("20", 0);
            obj4.put("21", 0);
            obj4.put("11", Long.parseLong(uin));

            // 保持与 sendMusic mtype 参数映射一致：mtype 1=好友 2=群聊  → 协议 10: 0=好友 1=群聊
            if (mtype == 2) {
                obj4.put("10", 3 - mtype);
            } else if (mtype == 1) {
                obj4.put("10", 1 - mtype);
            } else {
                obj4.put("10", mtype);
            }

            JSONObject obj5 = new JSONObject();
            obj5.put("1", 1);
            obj5.put("2", "0.0.0");
            obj5.put("3", packageName);
            obj5.put("4", token);
            obj4.put("5", obj5);

            JSONObject obj7 = new JSONObject();
            obj7.put("15", PacketHelper.rand(100, 99999));
            obj4.put("7", obj7);

            // obj12: 卡片显示内容
            JSONObject obj12 = new JSONObject();
            obj12.put("10", appName);
            obj12.put("11", desc);
            obj12.put("13", detailUrl);
            obj12.put("14", img);
            obj4.put("12", obj12);

            // obj16: Ark 内部数据（小程序卡片独有）
            JSONObject obj16 = new JSONObject();
            obj16.put("1", arkAppId);
            obj16.put("2", miniAppPath);
            obj16.put("3", detailUrl);
            obj16.put("5", appName);
            obj16.put("6", desc);
            if (arkJson != null) {
                obj16.put("10", arkJson.toString());
            } else {
                obj16.put("10", "");
            }
            obj4.put("16", obj16);

            obj4.put("22", new JSONObject());
            obj4.put("23", new JSONObject());
            json.put("4", obj4);
            LogUtils.d("ExtraTool", "发送小程序卡片: " + json.toString());
            PacketHelper.sendPacket("OidbSvc.0xb77_9", json, new packetListener() {
                @Override
                public void onResult(boolean success, JSONObject json) {
                    if (!success) {
                        Toasts.toast("发送小程序卡片失败");
                    }
                }
            });

        } catch (Exception e) {
            Toasts.toast("发送小程序卡片失败(报错)");
            LogUtils.e("ExtraTool", "发送小程序卡片失败: " + e.getMessage());
        }
    }

    /** 便捷入口：发送到群聊（mtype=2） */
    public static void sendTroopMiniApp(String packageName, String appid, String token,
                                         String arkAppId, String miniAppPath,
                                         String uin, String appName, String desc,
                                         String detailUrl, String img, JSONObject arkJson) {
        sendMiniApp(packageName, appid, token, arkAppId, miniAppPath, uin,
                appName, desc, detailUrl, img, 2, arkJson);
    }

    /** 便捷入口：发送到好友（mtype=1） */
    public static void sendFriendMiniApp(String packageName, String appid, String token,
                                          String arkAppId, String miniAppPath,
                                          String uin, String appName, String desc,
                                          String detailUrl, String img, JSONObject arkJson) {
        sendMiniApp(packageName, appid, token, arkAppId, miniAppPath, uin,
                appName, desc, detailUrl, img, 1, arkJson);
    }

    public static void qqsendTroopMusic(String qun, String title, String desc, String detailUrl, String audio, String img) {
        sendMusic("qq", qun, title, desc, detailUrl, audio, img, 2);
    }

    public static void qqsendFriendMusic(String uin, String title, String desc, String detailUrl, String audio, String img) {
        sendMusic("qq", uin, title, desc, detailUrl, audio, img, 1);
    }

    public static void wysendTroopMusic(String qun, String title, String desc, String detailUrl, String audio, String img) {
        sendMusic("wy", qun, title, desc, detailUrl, audio, img, 2);
    }

    public static void wysendFriendMusic(String uin, String title, String desc, String detailUrl, String audio, String img) {
        sendMusic("wy", uin, title, desc, detailUrl, audio, img, 1);
    }

    public static void kgsendTroopMusic(String qun, String title, String desc, String detailUrl, String audio, String img) {
        sendMusic("kg", qun, title, desc, detailUrl, audio, img, 2);
    }

    public static void kgsendFriendMusic(String uin, String title, String desc, String detailUrl, String audio, String img) {
        sendMusic("kg", uin, title, desc, detailUrl, audio, img, 1);
    }
    public static void kwsendTroopMusic(String qun, String title, String desc, String detailUrl, String audio, String img) {
        sendMusic("kw", qun, title, desc, detailUrl, audio, img, 2);
    }

    public static void kwsendFriendMusic(String uin, String title, String desc, String detailUrl, String audio, String img) {
        sendMusic("kw", uin, title, desc, detailUrl, audio, img, 1);
    }

    public static void bdsendTroopMusic(String qun, String title, String desc, String detailUrl, String audio, String img) {
        sendMusic("bd", qun, title, desc, detailUrl, audio, img, 2);
    }

    public static void bdsendFriendMusic(String uin, String title, String desc, String detailUrl, String audio, String img) {
        sendMusic("bd", uin, title, desc, detailUrl, audio, img, 1);
    }

    public static void mgsendTroopMusic(String qun, String title, String desc, String detailUrl, String audio, String img) {
        sendMusic("mg", qun, title, desc, detailUrl, audio, img, 2);
    }

    public static void mgsendFriendMusic(String uin, String title, String desc, String detailUrl, String audio, String img) {
        sendMusic("mg", uin, title, desc, detailUrl, audio, img, 1);
    }

    public static List<String> getAbnormalUserList(String qun) {
        List<String> userList = new ArrayList<>();
        try {
            JSONObject requestJson = new JSONObject();
            requestJson.put("1", 37312);
            requestJson.put("2", 0);
            requestJson.put("12", 0);

            JSONObject obj4 = new JSONObject();
            obj4.put("1", Long.parseLong(qun));
            requestJson.put("4", obj4);

            String responseStr = fetchPacket("OidbSvcTrpcTcp.0x91c0_0", requestJson);
            JSONObject response = new JSONObject(responseStr);
            if (response.has("4")) {
                JSONObject respObj4 = response.getJSONObject("4");
                if (respObj4.has("1")) {
                    Object arrObj = respObj4.get("1");

                    if (arrObj instanceof JSONArray) {
                        JSONArray usersArray = (JSONArray) arrObj;  
                        for (int i = 0; i < usersArray.length(); i++) {
                            Object userObj = usersArray.get(i);  
                            if (userObj instanceof JSONObject) {
                                JSONObject userJson = (JSONObject) userObj;
                                if (userJson.has("1")) {
                                    Object val = userJson.get("1");
                                    userList.add(String.valueOf(val));
                                }
                            } else if (userObj instanceof Long || userObj instanceof Integer) {
                                userList.add(String.valueOf(userObj));
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            LogUtils.e("ExtraTool", "getAbnormalUserList error: " + e.getMessage());
        }
        return userList;
    }

    public static void addFriend(String targetUin, String msg, String remark) {
        try {
            String myUin = QQCurrentEnv.getCurrentUin();
            JSONObject packet = new JSONObject();
            packet.put("1", 1986);
            packet.put("2", 5);

            JSONObject obj4 = new JSONObject();

            JSONObject obj32 = new JSONObject();
            JSONObject obj1 = new JSONObject();
            obj1.put("100", 0);
            obj1.put("101", 0);
            obj1.put("102", 0);
            obj32.put("1", obj1);
            obj4.put("32", obj32);

            obj4.put("1", Long.parseLong(myUin));
            obj4.put("2", Long.parseLong(targetUin));
            obj4.put("3", 1);
            obj4.put("4", 1);
            obj4.put("5", 0);
            obj4.put("7", msg);
            obj4.put("9", 1);
            obj4.put("11", 3041);
            obj4.put("12", 12);
            obj4.put("18", remark);
            obj4.put("20", 0);
            obj4.put("28", 1);
            obj4.put("29", 1);
            obj4.put("30", new JSONObject());
            obj4.put("31", "");

            packet.put("4", obj4);
            packet.put("12", 1);

            PacketHelper.sendPacket("OidbSvcTrpcTcp.0x7c2_5", packet, new packetListener() {
                @Override
                public void onResult(boolean success, JSONObject json) {
                    if (success) {
                        // Toasts.toast("好友请求已发送");
                    } else {
                        Toasts.toast("添加好友失败");
                    }
                }
            });

        } catch (Exception e) {
            Toasts.toast("添加好友失败(报错)");
            LogUtils.e("ExtraTool", "添加好友失败: " + e.getMessage());
        }
    }


    /*上传头像
     * @param url 头像url
     * @param isStatic 是否静态头像
     */
    public static void uploadAvatar(String url, boolean isStatic) {
        String type = "动态";
        if (isStatic) {
            type = "静态";
        }

        try {
        JSONObject json = new JSONObject();
        json.put("uin", QQCurrentEnv.getCurrentUin());
        json.put("img", url);
        json.put("type", type);

        JSONObject inner = new JSONObject();
        inner.put("1", Long.parseLong(QQCurrentEnv.getCurrentUin()));
        inner.put("2", 0);
        inner.put("3", 16);
        inner.put("4", 1);
        inner.put("6", 3);
        inner.put("7", 5);

        JSONObject root = new JSONObject();
        root.put("1281", inner);

        String packetResult = fetchPacket("HttpConn.0x6ff_501", root);

        ProtoData protoData = new ProtoData();
        protoData.fromJSON(new JSONObject(packetResult));
        byte[] sendData = protoData.toBytes();

        json.put("hex", protoData.bytesToHex(sendData));

            String postData = json.toString();
            String response = HttpUtils.post("https://api.s01s.cn/API/sctx/", postData, "application/json");

            // writeLog(response);

            if (response == null) {
                Toasts.toast("上传头像异常: 数据为null");
                return;
            }
            JSONObject jsons = new JSONObject(response);
            if (jsons.getInt("code") == 0) {
                Toasts.toast("上传头像成功: " + jsons.optString("message"));
            }
        } catch (Exception e) {
            Toasts.toast("上传头像失败: " + e.getMessage());
            LogUtils.e("ExtraTool", "上传头像失败: " + e.getMessage());
        }
    }

    /*获取群踢出并拉黑列表
     * @param groupId 群号
     * @return 踢黑列表
     */
    public static List<BlackUser> getGroupBlackList(String groupId) {
        List<BlackUser> list = new ArrayList<BlackUser>();

        try {
            JSONObject json = new JSONObject();
            json.put("1", 37453);
            json.put("2", 1);

            JSONObject inner = new JSONObject();
            inner.put("1", Long.parseLong(groupId));
            inner.put("2", 500);
            inner.put("3", 0);

            json.put("4", inner);
            json.put("12", 1);

            String response = fetchPacket("OidbSvcTrpcTcp.0x924d_1", json);
            if (response == null || response.isEmpty()) {
                Toasts.toast("获取踢黑失败: 响应为空");
                return list;
            }

            JSONObject result = new JSONObject(response);

            JSONObject data4 = result.optJSONObject("4");
            if (data4 == null) {
                Toasts.toast("获取踢黑失败: 没有4字段");
                return list;
            }

            // writeLog(data4.toString());

            Object listObj = data4.get("2");
            if (listObj == null) {
                Toasts.toast("踢黑列表为空");
                return list;
            }

            if (listObj instanceof JSONArray) {
                JSONArray arr = (JSONArray) listObj;
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.getJSONObject(i);
                    String uin = String.valueOf(obj.optLong("1"));
                    long time = obj.optLong("3");
                    String op = String.valueOf(obj.optLong("4"));

                    Object name = obj.opt("2");
                    String nick = "null";
                    if (name instanceof String) {
                        nick = String.valueOf(name);
                    }

                    BlackUser blackUser = new BlackUser(uin, nick, time, op);

                    list.add(blackUser);
                }
            } else if (listObj instanceof JSONObject) {
                JSONObject obj = (JSONObject) listObj;
                String uin = String.valueOf(obj.optLong("1"));
                Object name = obj.opt("2");
                String nick = "null";
                if (name instanceof String) {
                    nick = String.valueOf(name);
                }

                long time = obj.optLong("3");
                String op = String.valueOf(obj.optLong("4"));

                BlackUser blackUser = new BlackUser(uin, nick, time, op);

                list.add(blackUser);
            }

            // Toasts.toast("获取踢黑列表成功: " + list.size() + " 人");
            return list;

        } catch (Exception e) {
            Toasts.toast("获取踢黑列表异常: " + e.getMessage());
            LogUtils.e("ExtraTool", "获取踢黑列表异常: " + e.getMessage());
            return list;
        }
    }

    /* 获取共同群列表
     * @param uin 群号
     * @return 共同群列表
     */
    public static List<JointGroup> getJointGroupList(String uin) {
        List<JointGroup> list = new ArrayList<JointGroup>();

        try {

            JSONObject json = new JSONObject();
            json.put("1", 3316);
            json.put("2", 0);
            json.put("3", 0);

            JSONObject inner = new JSONObject();
            inner.put("1", Long.parseLong(QQCurrentEnv.getCurrentUin()));
            inner.put("2", Long.parseLong(uin));
            inner.put("4", 1);
            inner.put("6", 0);

            JSONArray array = new JSONArray();
            JSONObject item1 = new JSONObject();
            JSONObject item1_3 = new JSONObject();
            item1_3.put("1", Long.parseLong(QQCurrentEnv.getCurrentUin()));
            item1_3.put("2", Long.parseLong(uin));
            item1.put("3", item1_3);
            item1.put("5", 3436);
            array.put(item1);

            JSONObject item2 = new JSONObject();
            JSONObject item2_3 = new JSONObject();
            JSONObject item2_3_1 = new JSONObject();
            item2_3_1.put("1", uin);
            item2_3_1.put("2", 1);
            item2_3.put("1", item2_3_1);
            item2.put("3", item2_3);
            item2.put("5", 3460);
            array.put(item2);

            inner.put("5", array);

            json.put("4", inner);
            json.put("6", "android " + HostInfo.versionName);

            String response = fetchPacket("OidbSvc.0xcf4", json);
            if (response == null || response.isEmpty()) {
                Toasts.toast("获取共同群列表失败: 响应为空");
                return list;
            }
            JSONObject result = new JSONObject(response);

            JSONObject data4 = result.optJSONObject("4");
            if (data4 == null) {
                Toasts.toast("获取共同群列表失败: 没有4字段");
                return list;
            }

            // Toasts.toast(data4.toString());

            Object listObj = data4.getJSONObject("12").get("1");
            if (listObj == null) {
                Toasts.toast("共同群列表为空");
                return list;
            }

            if (listObj instanceof JSONArray) {
                JSONArray arr = (JSONArray) listObj;
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.getJSONObject(i);
                    String qun = String.valueOf(obj.optLong("1"));
                    long time = obj.optLong("4");
                    JointGroup jointGroup = new JointGroup(qun, time);

                    list.add(jointGroup);
                }
            } else if (listObj instanceof JSONObject) {
                JSONObject obj = (JSONObject) listObj;
                String qun = String.valueOf(obj.optLong("1"));
                long time = obj.optLong("4");
                JointGroup jointGroup = new JointGroup(qun, time);

                list.add(jointGroup);
            }

            // Toasts.toast("共同群列表成功: " + list.size() + " 人");
            return list;

        } catch (Exception e) {
            Toasts.toast("共同群列表异常: " + e.getMessage());
            LogUtils.e("ExtraTool", "共同群列表异常: " + e.getMessage());
            return list;
        }
    }

    /**
     * 获取群语音在线播放链接。
     * 通过 OidbSvcTrpcTcp.0x126e_200 (NTV2RichMediaReq) 向服务器申请语音下载地址，
     * 解析回包中的 域名 + fileid 路径 + rkey 拼成可直接访问的 https 链接。
     *
     * @param md5Hex   语音文件 MD5 的 HEX 字符串 (PttElement.md5HexStr)
     * @param uuid     语音文件 uuid (PttElement.fileUuid)
     * @param fileName 语音文件名 (PttElement.fileName)
     * @param fileSize 语音文件大小 (PttElement.fileSize)
     * @param filePath 语音本地缓存路径 (PttElement.filePath)，可为空；用于计算 sha1
     * @param msgTime  消息时间戳 (秒)
     * @return 完整在线链接，失败返回空串
     */
    public static String getGroupPttUrl(String md5Hex, String uuid, String fileName,
                                        long fileSize, String filePath, long msgTime) {
        return buildPttUrl(false, null, md5Hex, uuid, fileName, filePath, msgTime);
    }

    /**
     * 获取好友语音在线播放链接。
     * 通过 OidbSvcTrpcTcp.0x126d_200 (NTV2RichMediaReq) 向服务器申请语音下载地址。
     * 与群语音差异：命令号、requestId(4717)、common.1(19)、scene.200(1)、
     * scene 用 201(好友场景, 含对方 uid) 而非 202、fileType(4631)、fileInfo.8(2)。
     *
     * @param peerUid  对方的 uid (MsgData.peerUid，形如 u_xxx)
     * @param md5Hex   语音文件 MD5 的 HEX 字符串 (PttElement.md5HexStr)
     * @param uuid     语音文件 uuid (PttElement.fileUuid)
     * @param fileName 语音文件名 (PttElement.fileName)
     * @param filePath 语音本地缓存路径 (PttElement.filePath)，可为空；用于计算 sha1
     * @param msgTime  消息时间戳 (秒)
     * @return 完整在线链接，失败返回空串
     */
    public static String getFriendPttUrl(String peerUid, String md5Hex, String uuid,
                                         String fileName, String filePath, long msgTime) {
        return buildPttUrl(true, peerUid, md5Hex, uuid, fileName, filePath, msgTime);
    }

    /**
     * 群/好友语音在线链接的统一构造与请求逻辑。
     * 群与好友仅在少量字段上不同(见下方 isFriend 分支)，主体结构完全一致。
     */
    private static String buildPttUrl(boolean isFriend, String peerUid, String md5Hex,
                                      String uuid, String fileName, String filePath, long msgTime) {
        try {
            if (md5Hex == null) md5Hex = "";
            if (uuid == null) uuid = "";
            if (fileName == null) fileName = "";

            // sha1 字段：内核数据类无 sha1，若本地有缓存文件则实时计算，否则传空
            String sha1Hex = "";
            if (filePath != null && !filePath.isEmpty()) {
                String s = getFileSha1(filePath);
                if (s != null) sha1Hex = s;
            }

            // ---- 构造 fileInfo (4.3.1.1) ----
            JSONObject fileInfo = new JSONObject();
            fileInfo.put("1", isFriend ? 4631 : 6335); // fileType: 好友4631 / 群6335
            fileInfo.put("2", md5Hex);                  // md5 hex 字符串(直接填，抓包即此形态)
            fileInfo.put("3", sha1Hex);                 // sha1 hex 字符串(空亦可)
            fileInfo.put("4", fileName);
            JSONObject fileSub = new JSONObject();
            fileSub.put("1", 3);
            fileSub.put("2", 0);
            fileSub.put("3", 0);
            fileSub.put("4", 1);
            fileInfo.put("5", fileSub);
            fileInfo.put("6", 0);
            fileInfo.put("7", 0);
            fileInfo.put("8", isFriend ? 2 : 3);
            fileInfo.put("9", 0);

            // ---- 构造 node (4.3.1) ----
            JSONObject node = new JSONObject();
            node.put("1", fileInfo);
            node.put("2", uuid);
            node.put("3", 1);
            node.put("4", msgTime);
            node.put("5", 604800);
            node.put("6", 0);

            // ---- 构造 downloadReq (4.3) ----
            JSONObject downloadReq = new JSONObject();
            downloadReq.put("1", node);
            // download 扩展固定 blob (对应发包 4.3.2，直接搬运原始字符串)
            downloadReq.put("2", "\u0012\u0010\b\u0000\u0018\u0000(\u00002\b\b\u0000\u0012\u0000\u001a\u0000 \u0000\"\u0002\b\u0000");
            downloadReq.put("3", 0);

            // ---- 构造 reqHead (4.1) ----
            JSONObject common = new JSONObject();
            common.put("1", isFriend ? 19 : 3);
            common.put("2", 200);

            JSONObject scene = new JSONObject();
            scene.put("101", 1);
            scene.put("102", 3);
            scene.put("103", 0);
            scene.put("200", isFriend ? 1 : 2);
            if (isFriend) {
                // 好友场景 201: {1:2(uid类型), 2:对方uid}
                JSONObject peerScene = new JSONObject();
                peerScene.put("1", 2);
                peerScene.put("2", peerUid != null ? peerUid : "");
                scene.put("201", peerScene);
            } else {
                // 群场景 202: {1:当前登录uin}
                JSONObject sceneUin = new JSONObject();
                sceneUin.put("1", Long.parseLong(QQCurrentEnv.getCurrentUin()));
                scene.put("202", sceneUin);
            }

            JSONObject client = new JSONObject();
            client.put("1", 2);

            JSONObject reqHead = new JSONObject();
            reqHead.put("1", common);
            reqHead.put("2", scene);
            reqHead.put("3", client);

            // ---- 组装最外层 ----
            JSONObject body = new JSONObject();
            body.put("1", reqHead);
            body.put("3", downloadReq);

            JSONObject packet = new JSONObject();
            packet.put("1", isFriend ? 4717 : 4718);
            packet.put("2", 200);
            packet.put("4", body);
            packet.put("12", 1);

            String cmd = isFriend ? "OidbSvcTrpcTcp.0x126d_200" : "OidbSvcTrpcTcp.0x126e_200";
            String response = fetchPacket(cmd, packet);
            if (response == null || response.isEmpty()) {
                return "";
            }

            JSONObject result = new JSONObject(response);
            JSONObject r4 = result.optJSONObject("4");
            if (r4 == null) return "";
            JSONObject downloadRsp = r4.optJSONObject("3");
            if (downloadRsp == null) return "";

            String rkey = downloadRsp.optString("1", "");   // 已包含 &rkey=
            JSONObject info = downloadRsp.optJSONObject("3");
            if (info == null) return "";
            String domain = info.optString("1", "");
            String path = info.optString("2", "");
            if (domain.isEmpty() || path.isEmpty()) return "";

            return "https://" + domain + path + rkey;

        } catch (Exception e) {
            LogUtils.e("ExtraTool", "获取语音链接异常: " + e.getMessage());
            return "";
        }
    }

    /**
     * 提取视频中的音频（调用 QQ 内置 FFmpeg，输出 aac 编码的 .m4a 文件）
     *
     * @param context   上下文，一般传 HostInfo.getContext()
     * @param inputMp4  输入视频文件路径
     * @param outputM4a 输出音频文件路径，需以 .m4a 结尾
     */
    public static void extractAudio(Context context, String inputMp4, String outputM4a) {
        try {
            String[] cmd = new String[] {
                "-y",
                "-i", inputMp4,
                "-vn",
                "-acodec", "aac",
                "-b:a", "192k",
                "-ar", "44100",
                "-ac", "2",
                outputM4a
            };

            FFmpeg.getInstance(context).execute(cmd, new FFmpegExecuteResponseCallback() {
                @Override
                public void onStart() {
                    // LogUtils.i("ExtraTool", "提取音频开始: " + inputMp4);
                }

                @Override
                public void onSuccess(String msg) {
                    // LogUtils.i("ExtraTool", "提取音频成功: " + msg);
                }

                @Override
                public void onFailure(String msg) {
                    LogUtils.e("ExtraTool", "提取音频失败: " + msg);
                }

                @Override
                public void onProgress(String msg) {
                }

                @Override
                public void onFinish(boolean success) {
                    // LogUtils.i("ExtraTool", "提取音频结束: " + success);
                }
            });
        } catch (FFmpegCommandAlreadyRunningException e) {
            LogUtils.e("ExtraTool", "提取音频失败，已有命令在执行: " + e.getMessage());
        } catch (Throwable e) {
            LogUtils.e("ExtraTool", "提取音频异常: " + e.getMessage());
        }
    }

    /** 计算文件 SHA1 的 HEX 字符串(小写)，失败返回 null */
    private static String getFileSha1(String filePath) {
        try (java.io.FileInputStream fis = new java.io.FileInputStream(filePath)) {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-1");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = fis.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
            StringBuilder sb = new StringBuilder();
            for (byte b : digest.digest()) {
                sb.append(String.format("%02x", b & 0xFF));
            }
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }


    private static Method sPinyinMethod = null;
    private static boolean sScanned = false;

    private static Method findPinyinMethod() {
        if (sPinyinMethod != null) return sPinyinMethod;
        if (sScanned) return null;
        sScanned = true;
        try {
            Class<?> cls = Class.forName("com.tencent.mobileqq.utils.ChnToSpell");
            for (Method m : cls.getDeclaredMethods()) {
                Class<?>[] p = m.getParameterTypes();
                if (p.length == 2
                        && p[0] == String.class
                        && p[1] == int.class
                        && m.getReturnType() == String.class) {
                    m.setAccessible(true);
                    sPinyinMethod = m;
                    break;
                }
            }
        } catch (Throwable t) {
            LogUtils.e("PinyinUtils", "findPinyinMethod: " + t);
        }
        return sPinyinMethod;
    }

    /**
     * 汉字转拼音
     * "张三" → "zhangsan"
     * "张三" → "zs"
     *
     * @param text 原始文本
     * @param type 1=全拼  2=首字母
     */
    public static String getPinyin(String text, int type) {
        if (text == null || text.isEmpty()) return "";
        try {
            Method m = findPinyinMethod();
            if (m == null) return "";
            Object r = m.invoke(null, text, type);
            return r == null ? "" : r.toString();
        } catch (Throwable t) {
            LogUtils.e("PinyinUtils", "getPinyin: " + t);
            return "";
        }
    }

        
    /**
     * 按路径判断图片格式，返回 jpg/png/gif/bmp 等
     */
    public static String getImageType(String filePath) {
        if (filePath == null || filePath.isEmpty()) return null;
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(filePath);
            return PicFormatUtils.getTypeByStream(fis);
        } catch (Throwable t) {
            LogUtils.e("PicFormat", "getImageType: " + t);
            return null;
        } finally {
            if (fis != null) try { fis.close(); } catch (Throwable ignored) {}
        }
    }

}