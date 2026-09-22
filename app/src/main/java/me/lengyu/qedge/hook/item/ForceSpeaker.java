package me.lengyu.qedge.hook.item;

import android.app.Activity;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.view.View;
import android.widget.LinearLayout;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import me.lengyu.qedge.hook.annotation.HookItemAnnotation;
import me.lengyu.qedge.hook.base.BaseSwitchHookItem;
import me.lengyu.qedge.utils.HostInfo;
import me.lengyu.qedge.utils.LogUtils;
import me.lengyu.qedge.utils.ModuleConfig;
import me.lengyu.qedge.utils.HookUtils;
import me.lengyu.qedge.utils.ReflectUtils;

/**
 * @Author 冷雨
 * @Description 语音消息强制免提，不走听筒
 */
@HookItemAnnotation(value = "强制免提", category = "item", tag = "语音免提", desc = "语音消息强制扬声器播放，不走听筒")
public class ForceSpeaker extends BaseSwitchHookItem {
    public static final ForceSpeaker INSTANCE = new ForceSpeaker();
    private static final String TAG = "ForceSpeaker";
    private static final String CLS_BASE =
        "com.tencent.mobileqq.qqaudio.audioplayer.AudioPlayerBase";

    // 媒体通道属性：强制 AudioTrack 走媒体流，避免通话通道的带通滤波导致音质差
    private static final AudioAttributes MEDIA_ATTRIBUTES =
        new AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build();

    private static boolean isEnabled() {
        return ModuleConfig.INSTANCE.getBoolean("force_speaker", false);
    }

    @Override
    protected boolean onInit() {
        return true;
    }

    @Override
    protected void onHook() {
        hookE();
        hookSpeaker();
        hookMode();
        hookTrack();
        hookVolume();
        hookStereoVolume();
        hookMediaPlayer();
        hookStreamVolume();
        hookBotton();
        hookAIOAudioBtnVB();
    }

    private void hookE() {
        try {
            Class<?> cls = XposedHelpers.findClass(CLS_BASE, ReflectUtils.hostClassLoader);
            // AudioPlayerBase 中"开始播放前准备"的方法：void 无参且声明抛 IOException，
            // 混淆后方法名不固定，全部匹配后逐个 hook，播放开始时强制切到扬声器
            int count = 0;
            for (Method m : cls.getDeclaredMethods()) {
                Class<?>[] p = m.getParameterTypes();
                Class<?>[] ex = m.getExceptionTypes();
                if (p.length != 0 || m.getReturnType() != void.class
                        || ex.length != 1 || ex[0] != IOException.class) {
                    continue;
                }
                m.setAccessible(true);
                XposedBridge.hookMethod(m, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (!isEnabled()) return;
                        try {
                            AudioManager am = getAudioManager();
                            if (am != null) {
                                am.setMode(AudioManager.MODE_NORMAL);
                                am.setSpeakerphoneOn(true);
                            } else {
                                LogUtils.e(TAG, "getAudioManager returned null");
                            }
                            // 逆向结论：听筒场景 g$a.b = STREAM_VOICE_CALL(0)，prepare 时
                            // player.f(g$a.b) 已把 streamType 写死为 VOICE_CALL。这里兜底把
                            // 播放器 streamType 强制为 MUSIC，音量键才可控（构造器 hook 之外的保险）
                            forcePlayerStreamMusic(param.thisObject);
                        } catch (Throwable t) {
                            LogUtils.e(TAG, "forceSpeaker error: " + t);
                        }
                    }
                });
                count++;
            }
        } catch (Throwable t) {
            LogUtils.e(TAG, "hookE failed: " + t);
        }
    }

    private void hookSpeaker() {
        try {
            HookUtils.hookBefore(
                XposedHelpers.findMethodExact(
                    AudioManager.class, "setSpeakerphoneOn", boolean.class),
                param -> {
                    if (!isEnabled()) return;
                    param.args[0] = true;
                });
        } catch (Throwable t) {
            LogUtils.e(TAG, "hookSpeaker failed: " + t);
        }
    }

    // 阻止 QQ 切回听筒模式（MODE_IN_COMMUNICATION / MODE_IN_CALL）
    private void hookMode() {
        try {
            HookUtils.hookBefore(
                XposedHelpers.findMethodExact(
                    AudioManager.class, "setMode", int.class),
                param -> {
                    if (!isEnabled()) return;
                    int m = (Integer) param.args[0];
                    if (m == AudioManager.MODE_IN_COMMUNICATION || m == AudioManager.MODE_IN_CALL) {
                        LogUtils.i(TAG, "setMode blocked, keep NORMAL (was " + m + ")");
                        param.args[0] = AudioManager.MODE_NORMAL;
                    }
                });
        } catch (Throwable t) {
            LogUtils.e(TAG, "hookMode failed: " + t);
        }
    }

    private void hookTrack() {
        try {
            boolean any = false;
            for (Constructor<?> c : AudioTrack.class.getDeclaredConstructors()) {
                Class<?>[] p = c.getParameterTypes();
                boolean allInt = true;
                for (Class<?> pt : p) {
                    if (pt != int.class) { allInt = false; break; }
                }
                if (p.length == 6 && allInt) {
                    // (streamType, sampleRate, channelConfig, audioFormat, bufferSize, mode)
                    c.setAccessible(true);
                    XposedBridge.hookMethod(c, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if (!isEnabled()) return;
                            param.args[0] = AudioManager.STREAM_MUSIC;
                        }
                    });
                    any = true;
                } else if (p.length == 7 && allInt) {
                    // (streamType, sampleRate, channelConfig, audioFormat, bufferSize, mode, sessionId)
                    c.setAccessible(true);
                    XposedBridge.hookMethod(c, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if (!isEnabled()) return;
                            param.args[0] = AudioManager.STREAM_MUSIC;
                        }
                    });
                    any = true;
                } else if (p.length >= 3 && p[0] == AudioAttributes.class) {
                    // (AudioAttributes, AudioFormat, bufferSize, mode[, sessionId])
                    c.setAccessible(true);
                    XposedBridge.hookMethod(c, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if (!isEnabled()) return;
                            param.args[0] = MEDIA_ATTRIBUTES;
                        }
                    });
                    any = true;
                }
            }
            if (!any) {
                LogUtils.e(TAG, "hookTrack: no AudioTrack constructor matched");
            }
        } catch (Throwable t) {
            LogUtils.e(TAG, "hookTrack failed: " + t);
        }

        // 兜底：播放器实例可能已复用（构造器不会再次触发），
        // 在 play() 时检查流类型，若仍是通话通道则尝试切到媒体通道（旧系统存在 setStreamType）
        try {
            Method play = AudioTrack.class.getDeclaredMethod("play");
            play.setAccessible(true);
            XposedBridge.hookMethod(play, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (!isEnabled()) return;
                    int st = -1;
                    try {
                        Method getStream = AudioTrack.class.getMethod("getStreamType");
                        st = (Integer) getStream.invoke(param.thisObject);
                    } catch (Throwable t) {
                        LogUtils.e(TAG, "getStreamType unavailable: " + t.getMessage());
                    }
                    if (st != -1 && st != AudioManager.STREAM_MUSIC) {
                        LogUtils.i(TAG, "AudioTrack.play() streamType=" + st + ", forcing MUSIC");
                        try {
                            AudioTrack.class.getMethod("setStreamType", int.class)
                                .invoke(param.thisObject, AudioManager.STREAM_MUSIC);
                        } catch (Throwable t) {
                            LogUtils.e(TAG, "setStreamType on play unavailable: " + t.getMessage());
                        }
                    }
                }
            });
        } catch (Throwable t) {
            LogUtils.e(TAG, "hook AudioTrack.play failed: " + t);
        }
    }

    // 兜底：把播放器（j 接口实现，如 SilkPlayer/AmrPlayer）的 streamType 强制为 MUSIC。
    // 逆向确认 SilkPlayer 的 streamType 存于字段 d（f(I) 写入），AmrPlayer 的 f(I) 直接
    // 调 MediaPlayer.setAudioStreamType。
    // 字段名 h、方法名 f 均为 QQ 混淆名，必须反射特征匹配：
    //   - 字段：遍历含父类的字段，运行时判断字段值是否实现 j 接口（真正的播放器）。
    //     注意不能只看字段类型，audioplayer 包内还有 k 等监听器接口（如
    //     AIOPttAudioPlayerManager 实现 k），按类型前缀匹配会误判成播放器。
    //   - 方法：枚举 j 接口 (I)V 签名方法（setStreamType）动态调用。
    private void forcePlayerStreamMusic(Object playerBase) {
        try {
            if (playerBase == null) return;
            Class<?> ifaceJ = XposedHelpers.findClass(
                "com.tencent.mobileqq.qqaudio.audioplayer.j", ReflectUtils.hostClassLoader);
            // 1) 找播放器对象：字段值必须真正实现 j 接口
            Object player = null;
            for (Class<?> c = playerBase.getClass();
                    c != null && c != Object.class; c = c.getSuperclass()) {
                for (java.lang.reflect.Field fld : c.getDeclaredFields()) {
                    if (java.lang.reflect.Modifier.isStatic(fld.getModifiers())) continue;
                    try {
                        fld.setAccessible(true);
                        Object val = fld.get(playerBase);
                        if (val != null && ifaceJ.isInstance(val)) {
                            player = val;
                            break;
                        }
                    } catch (Throwable ignored) { }
                }
                if (player != null) break;
            }
            if (player == null) {
                // 未持有 j 播放器字段（如新版 AIOPttAudioPlayerManager 内部自行管理），
                // 交给 hookMediaPlayer / hookTrack 兜底，无需日志
                return;
            }
            // 2) 通过 j 接口枚举 (I)V 方法（setStreamType），不硬编码方法名
            for (Method m : ifaceJ.getDeclaredMethods()) {
                if (m.getParameterCount() == 1
                        && m.getParameterTypes()[0] == int.class
                        && m.getReturnType() == void.class) {
                    m.setAccessible(true);
                    m.invoke(player, AudioManager.STREAM_MUSIC);
                    LogUtils.i(TAG, "player streamType forced to MUSIC via " + m.getName());
                    return;
                }
            }
            LogUtils.w(TAG, "no (I)V method found on j interface");
        } catch (Throwable t) {
            LogUtils.e(TAG, "forcePlayerStreamMusic failed: " + t);
        }
    }

    // 限制 QQ 对播放通道的异常增益（声音特别大时多为 setVolume > 1.0 导致，且会绕过媒体音量）。
    // Android 高版本 setVolume 为 hidden API，findMethodExact 会抛 NoSuchMethodError，
    // 改用 hookAllMethods 遍历全部重载（float / float,float）
    private void hookVolume() {
        try {
            XposedBridge.hookAllMethods(AudioTrack.class, "setVolume",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        if (!isEnabled()) return;
                        for (int i = 0; i < param.args.length; i++) {
                            if (param.args[i] instanceof Float) {
                                float v = (Float) param.args[i];
                                if (v > 1.0f) {
                                    LogUtils.i(TAG, "setVolume clamped arg" + i + "=" + v + " -> 1.0");
                                    param.args[i] = 1.0f;
                                }
                            }
                        }
                    }
                });
        } catch (Throwable t) {
            LogUtils.e(TAG, "hookVolume failed: " + t);
        }
    }

    // SilkPlayer 播放前调用 AudioTrack.setStereoVolume(o, q) 设置增益，
    // 默认 1.0，但游戏中心等场景可能被调大导致声音异常放大且绕过媒体音量
    private void hookStereoVolume() {
        try {
            XposedBridge.hookAllMethods(AudioTrack.class, "setStereoVolume",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        if (!isEnabled()) return;
                        for (int i = 0; i < param.args.length; i++) {
                            if (param.args[i] instanceof Float) {
                                float v = (Float) param.args[i];
                                if (v > 1.0f) {
                                    LogUtils.i(TAG, "setStereoVolume clamped arg" + i + "=" + v + " -> 1.0");
                                    param.args[i] = 1.0f;
                                }
                            }
                        }
                    }
                });
        } catch (Throwable t) {
            LogUtils.e(TAG, "hookStereoVolume failed: " + t);
        }
    }

    // AMR 等语音走 MediaPlayer（AmrPlayer.f 里调 MediaPlayer.setAudioStreamType），
    // 听筒场景会传入 VOICE_CALL(0)：NORMAL 模式下该流音量不可控且带通信增益。
    // 直接强制媒体流，音量键（QBaseActivity 已绑 MUSIC）即可控制
    private void hookMediaPlayer() {
        try {
            XposedBridge.hookMethod(
                MediaPlayer.class.getMethod("setAudioStreamType", int.class),
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        if (!isEnabled()) return;
                        int st = (Integer) param.args[0];
                        if (st != AudioManager.STREAM_MUSIC) {
                            LogUtils.i(TAG, "MediaPlayer.setAudioStreamType(" + st + ") -> MUSIC");
                            param.args[0] = AudioManager.STREAM_MUSIC;
                        }
                    }
                });
        } catch (Throwable t) {
            LogUtils.e(TAG, "hookMediaPlayer setAudioStreamType failed: " + t);
        }
        try {
            XposedBridge.hookAllMethods(MediaPlayer.class, "setVolume",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        if (!isEnabled()) return;
                        for (int i = 0; i < param.args.length; i++) {
                            if (param.args[i] instanceof Float) {
                                float v = (Float) param.args[i];
                                if (v > 1.0f) {
                                    LogUtils.i(TAG, "MediaPlayer.setVolume clamped arg" + i + "=" + v + " -> 1.0");
                                    param.args[i] = 1.0f;
                                }
                            }
                        }
                    }
                });
        } catch (Throwable t) {
            LogUtils.e(TAG, "hookMediaPlayer setVolume failed: " + t);
        }
    }

    // QQ 在通话逻辑下可能操作 VOICE_CALL 音量，NORMAL 模式下无效；重定向到 MUSIC 使音量键生效
    private void hookStreamVolume() {
        try {
            XposedBridge.hookMethod(
                AudioManager.class.getMethod("setStreamVolume", int.class, int.class, int.class),
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        if (!isEnabled()) return;
                        int st = (Integer) param.args[0];
                        if (st == AudioManager.STREAM_VOICE_CALL) {
                            LogUtils.i(TAG, "setStreamVolume(VOICE_CALL=" + param.args[1] + ") -> MUSIC");
                            param.args[0] = AudioManager.STREAM_MUSIC;
                        }
                    }
                });
        } catch (Throwable t) {
            LogUtils.e(TAG, "hook setStreamVolume failed: " + t);
        }
        try {
            XposedBridge.hookMethod(
                AudioManager.class.getMethod("adjustStreamVolume", int.class, int.class, int.class),
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        if (!isEnabled()) return;
                        int st = (Integer) param.args[0];
                        if (st == AudioManager.STREAM_VOICE_CALL) {
                            LogUtils.i(TAG, "adjustStreamVolume(VOICE_CALL, dir=" + param.args[1] + ") -> MUSIC");
                            param.args[0] = AudioManager.STREAM_MUSIC;
                        }
                    }
                });
        } catch (Throwable t) {
            LogUtils.e(TAG, "hook adjustStreamVolume failed: " + t);
        }
        // 音量键真实入口：adjustSuggestedStreamVolume，按当前 Activity 的
        // setVolumeControlStream 决定调整哪个流。若被绑到 VOICE_CALL 则强制 MUSIC
        try {
            XposedBridge.hookAllMethods(AudioManager.class, "adjustSuggestedStreamVolume",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        if (!isEnabled()) return;
                        if (param.args.length < 2 || !(param.args[1] instanceof Integer)) return;
                        int st = (Integer) param.args[1];
                        if (st == AudioManager.STREAM_VOICE_CALL) {
                            LogUtils.i(TAG, "adjustSuggestedStreamVolume(VOICE_CALL) -> MUSIC");
                            param.args[1] = AudioManager.STREAM_MUSIC;
                        }
                    }
                });
        } catch (Throwable t) {
            LogUtils.e(TAG, "hook adjustSuggestedStreamVolume failed: " + t);
        }

        // QQ 语音界面常 setVolumeControlStream(VOICE_CALL) 把音量键绑定到通话音量，
        // 在 NORMAL 模式下无效导致"音量键无法控制"，重定向到 MUSIC
        try {
            XposedBridge.hookAllMethods(Activity.class, "setVolumeControlStream",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        if (!isEnabled()) return;
                        int st = (Integer) param.args[0];
                        if (st == AudioManager.STREAM_VOICE_CALL) {
                            LogUtils.i(TAG, "setVolumeControlStream(VOICE_CALL) -> MUSIC");
                            param.args[0] = AudioManager.STREAM_MUSIC;
                        }
                    }
                });
        } catch (Throwable t) {
            LogUtils.e(TAG, "hook setVolumeControlStream failed: " + t);
        }
    }

    private AudioManager getAudioManager() {
        try {
            Context ctx = HostInfo.getContext();
            if (ctx != null) {
                return (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
            }
        } catch (Throwable ignored) {
        }
        try {
            Context app = android.app.AndroidAppHelper.currentApplication();
            if (app != null) {
                return (AudioManager) app.getSystemService(Context.AUDIO_SERVICE);
            }
        } catch (Throwable ignored) {
        }
        try {
            Class<?> cls = XposedHelpers.findClass(
                "com.tencent.qphone.base.util.BaseApplication", ReflectUtils.hostClassLoader);
            Context ctx = (Context) XposedHelpers.callStaticMethod(cls, "getContext");
            if (ctx != null) {
                return (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    
    // 拦截 AIOAudioBtnVB.handleUIState：不显示听筒/扬声器切换按钮。
    // 注意不能用 cls.getMethod("handleUIState", MviUIState.class)：模块里引用的
    // MviUIState 是 stub 类，与宿主 QQ 进程的 MviUIState 不是同一个 Class 对象，
    // getMethod 会抛 NoSuchMethodException 导致拦截静默失效。
    // 改为签名特征匹配：单参数 MviUIState（按类名判断）的 void 方法，且不硬编码方法名抗混淆
    public void hookBotton() {
        try {
            Class<?> cls = XposedHelpers.findClass(
                "com.tencent.mobileqq.aio.reserve1.audio.AIOAudioBtnVB", ReflectUtils.hostClassLoader);
            if (cls == null) return;
            boolean any = false;
            for (Method m : cls.getDeclaredMethods()) {
                Class<?>[] p = m.getParameterTypes();
                if (p.length != 1 || m.getReturnType() != void.class) continue;
                if (!"com.tencent.mvi.base.mvi.MviUIState".equals(p[0].getName())) continue;
                m.setAccessible(true);
                XposedBridge.hookMethod(m, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        if (!isEnabled()) return;
                        param.setResult(null);
                    }
                });
                any = true;
            }
            if (!any) {
                LogUtils.e(TAG, "hookBotton: no handleUIState method matched");
            }
        } catch (Throwable t) {
            LogUtils.e(TAG, "hookBotton failed: " + t);
        }
    }

    // 拦截 AIOAudioBtnVB 实例创建：创建后立即隐藏听筒/扬声器切换按钮容器。
    // 逆向确认：字段 e 是 Lazy<LinearLayout>（45dp，含 ImageView 图标 + TextView 文字），
    // handleUIState 里 setVisibility(0) 显示。字段名 e/f/h 均为混淆名，
    // 用特征匹配：遍历本类 kotlin.Lazy 字段，getValue() 结果为首个 LinearLayout 的即切换容器。
    // 作为 handleUIState 拦截之外的保险——handleUIState 方法名混淆后 hook 会失效，
    // 这里不依赖任何方法名，创建实例时即可保证切换按钮默认隐藏
    private void hookAIOAudioBtnVB() {
        try {
            Class<?> cls = XposedHelpers.findClass(
                "com.tencent.mobileqq.aio.reserve1.audio.AIOAudioBtnVB", ReflectUtils.hostClassLoader);
            for (Constructor<?> c : cls.getDeclaredConstructors()) {
                c.setAccessible(true);
                XposedBridge.hookMethod(c, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (!isEnabled()) return;
                        try {
                            for (Field fld : cls.getDeclaredFields()) {
                                if (Modifier.isStatic(fld.getModifiers())) continue;
                                if (!"kotlin.Lazy".equals(fld.getType().getName())) continue;
                                fld.setAccessible(true);
                                Object lazy = fld.get(param.thisObject);
                                if (lazy == null) continue;
                                // kotlin.Lazy.getValue() 为接口方法，名字稳定
                                Object v = XposedHelpers.callMethod(lazy, "getValue");
                                if (v instanceof LinearLayout) {
                                    ((LinearLayout) v).setVisibility(View.GONE);
                                    return;
                                }
                            }
                        } catch (Throwable ignored) { }
                    }
                });
            }
        } catch (Throwable t) {
            LogUtils.e(TAG, "hookAIOAudioBtnVB failed: " + t);
        }
    }
}
