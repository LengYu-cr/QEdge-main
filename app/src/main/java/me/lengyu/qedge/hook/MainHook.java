package me.lengyu.qedge.hook;

import android.content.Context;
import me.lengyu.qedge.common.ModuleScope;
import me.lengyu.qedge.hook.api.FromServiceMsgDispatcher;
import me.lengyu.qedge.hook.item.DownloadEmotion;
import me.lengyu.qedge.hook.item.FlashPicBypass;
import me.lengyu.qedge.hook.item.TransparentAvatar;
import me.lengyu.qedge.hook.item.VideoToBubble;
import me.lengyu.qedge.hook.item.AntiPokeDelay;
import me.lengyu.qedge.hook.item.AutoLikeBack;
import me.lengyu.qedge.hook.item.KeepAliveHook;
import me.lengyu.qedge.hook.item.LevelBoost;
import me.lengyu.qedge.hook.item.TimArkCardBypass;
import me.lengyu.qedge.hook.api.OnMenuBuild;
import me.lengyu.qedge.hook.api.OnPaiYiPai;
import me.lengyu.qedge.hook.api.OnQZonePush;
import me.lengyu.qedge.hook.api.OnReceiveMsg;
import me.lengyu.qedge.hook.api.OnSendMsg;
import me.lengyu.qedge.hook.api.OnTroopJoin;
import me.lengyu.qedge.hook.api.OnTroopQuit;
import me.lengyu.qedge.hook.api.OnTroopShutUp;
import me.lengyu.qedge.hook.api.OnGetRKey;
import me.lengyu.qedge.hook.entry.QQPlusInject;
import me.lengyu.qedge.hook.base.BaseApiHookItem;
import me.lengyu.qedge.hook.base.BaseClickableHookItem;
import me.lengyu.qedge.hook.base.BaseSwitchHookItem;
import me.lengyu.qedge.hook.base.HookRegistry;
import me.lengyu.qedge.plugin.bean.PluginInfo;
import me.lengyu.qedge.plugin.PluginManager;
import me.lengyu.qedge.plugin.view.ChatSettingLoader;
import me.lengyu.qedge.hook.HeartbeatManager;
import me.lengyu.qedge.utils.HostInfo;
import me.lengyu.qedge.utils.HookUtils;
import me.lengyu.qedge.utils.LogUtils;
import me.lengyu.qedge.utils.ModuleConfig;
import me.lengyu.qedge.utils.qq.QQCurrentEnv;
import me.lengyu.qedge.utils.ReflectUtils;
import me.lengyu.qedge.hook.entry.QQSettingInject;
import me.lengyu.qedge.coldrain.ColdRainCore;
import me.lengyu.qedge.hook.item.RemoveLinkInfo;
import me.lengyu.qedge.hook.item.PreventRecall;
import me.lengyu.qedge.hook.item.CopyArkMessage;
import me.lengyu.qedge.hook.item.LongClickSendCard;

import java.util.List;

public class MainHook {

    private static boolean initialized = false;

    public static void registerHookItems() {
        HookRegistry.register(OnGetRKey.INSTANCE);
        HookRegistry.register(OnReceiveMsg.INSTANCE);
        HookRegistry.register(OnSendMsg.INSTANCE);
        HookRegistry.register(OnTroopJoin.INSTANCE);
        HookRegistry.register(OnTroopQuit.INSTANCE);
        HookRegistry.register(OnTroopShutUp.INSTANCE);
        HookRegistry.register(OnPaiYiPai.INSTANCE);
        HookRegistry.register(OnMenuBuild.INSTANCE);
        HookRegistry.register(OnQZonePush.INSTANCE);
        HookRegistry.register(FlashPicBypass.INSTANCE);
        HookRegistry.register(DownloadEmotion.INSTANCE);
        HookRegistry.register(AntiPokeDelay.INSTANCE);
        HookRegistry.register(TransparentAvatar.INSTANCE);
        HookRegistry.register(VideoToBubble.INSTANCE);
        HookRegistry.register(TimArkCardBypass.INSTANCE);
        HookRegistry.register(AutoLikeBack.INSTANCE);
        HookRegistry.register(KeepAliveHook.INSTANCE);
        HookRegistry.register(LevelBoost.INSTANCE);
        HookRegistry.register(QQPlusInject.INSTANCE);
        HookRegistry.register(RemoveLinkInfo.INSTANCE);
        HookRegistry.register(QQSettingInject.INSTANCE);
        HookRegistry.register(new PreventRecall());
        HookRegistry.register(new CopyArkMessage());
        HookRegistry.register(new LongClickSendCard());
    }

    private static long lastPluginLoadTime = 0;
    private static final long PLUGIN_LOAD_INTERVAL = 10000;

    public static void loadHook() {
        if (initialized) return;
        initialized = true;

        try {
            if (HeartbeatManager.isBanned()) {
                LogUtils.e("MainHook", "Account is banned, skip loading hooks");
                return;
            }
        } catch (Throwable e) {
            LogUtils.e("MainHook", "Check ban status failed: " + e.getMessage());
        }

        registerHookItems();
        FromServiceMsgDispatcher.loadHook();
        loadApiHook();
        initSwitchHookItem();
        hookAccountChange();

        try {
            ChatSettingLoader.loadHook();
        } catch (Throwable e) {
            LogUtils.e("MainHook", "Failed to load ChatSettingLoader: " + e.getMessage());
        }

        ModuleScope.launchDelayedIO("Plugin-AutoLoad", 2000, () -> {
            try {
                loadPluginsIfNeeded();
            } catch (Throwable e) {
                LogUtils.e("MainHook", "Failed to load plugins: " + e.getMessage());
            }
            try {
                ColdRainCore.getInstance().init(HostInfo.getHostContext());
            } catch (Throwable e) {
                LogUtils.e("MainHook", "Failed to init ColdRainCore: " + e.getMessage());
            }
        });

    }

    private static void loadPluginsIfNeeded() {
        long now = System.currentTimeMillis();
        if (now - lastPluginLoadTime < PLUGIN_LOAD_INTERVAL) {
            return;
        }
        lastPluginLoadTime = now;
        PluginManager.initAllPluginForCurrent();
    }

    private static void loadApiHook() {
        List<BaseApiHookItem> apiHookItemList = HookRegistry.getHookItemsByClass(BaseApiHookItem.class);
        for (BaseApiHookItem item : apiHookItemList) {
            try {
                if (item.isInTargetProcess()) {
                    item.loadHook();
                }
            } catch (Throwable t) {
                LogUtils.e(item.getClass().getSimpleName(), t);
            }
        }
    }

    private static void initSwitchHookItem() {
        List<BaseSwitchHookItem> switchHookItemList = HookRegistry.getHookItemsByClass(BaseSwitchHookItem.class);
        for (BaseSwitchHookItem item : switchHookItemList) {
            item.init();
        }
    }

    private static void hookAccountChange() {
        try {
            Class<?> qqAppInterface = ReflectUtils.hostClassLoader != null 
                ? Class.forName("com.tencent.mobileqq.app.QQAppInterface", false, ReflectUtils.hostClassLoader)
                : Class.forName("com.tencent.mobileqq.app.QQAppInterface");
            
            java.lang.reflect.Method targetMethod = null;
            String[] methodNames = {"onCreateQQMessageFacade", "initQQMessageFacade", "createQQMessageFacade", "onCreate"};
            for (String methodName : methodNames) {
                targetMethod = ReflectUtils.findMethod(qqAppInterface, methodName);
                if (targetMethod != null) {
                    break;
                }
            }
            if (targetMethod == null) {
                LogUtils.w("hookAccountChange", "Specific method not found, trying constructor hook");
                HookUtils.hookAfter(qqAppInterface.getDeclaredConstructors()[0], (param) -> {
                    ModuleScope.launchDelayedIO("AccountChange", 3000, () -> onAccountChanged());
                });
                return;
            }
            HookUtils.hookAfter(targetMethod, (param) -> {
                ModuleScope.launchIOJava("AccountChange", () -> onAccountChanged());
            });
        } catch (Throwable e) {
            LogUtils.e("hookAccountChange", e);
        }
    }

    private static void onAccountChanged() {
        try {
            QQCurrentEnv.reset();
            String currentUin = QQCurrentEnv.getCurrentUin();
            if (currentUin != null && !currentUin.isEmpty()) {
                ModuleConfig.INSTANCE.putString("currentUin", currentUin);
                ModuleConfig.INSTANCE.putString("heartbeat_current_uin", currentUin);
            } else {
                LogUtils.w("onAccountChanged", "currentUin is null or empty, skip saving to config.");
            }
            processDataForCurrent("init");
            loadPluginsIfNeeded();

            try {
                HeartbeatManager.getInstance().startHeartbeat();
            } catch (Throwable e) {
                LogUtils.e("onAccountChanged", "Failed to restart heartbeat: " + e.getMessage());
            }
        } catch (Throwable e) {
            LogUtils.e("onAccountChanged", e);
        }
    }

    public static void processDataForCurrent(String tag) {
        List<BaseClickableHookItem> clickableHookItemList = HookRegistry.getHookItemsByClass(BaseClickableHookItem.class);
        for (BaseClickableHookItem item : clickableHookItemList) {
            if (item.isAvailable()) {
                if ("init".equals(tag)) {
                    try {
                        java.lang.reflect.Method method = BaseClickableHookItem.class.getDeclaredMethod("initData");
                        method.setAccessible(true);
                        method.invoke(item);
                    } catch (Throwable e) {
                        LogUtils.e(e);
                    }
                } else if ("save".equals(tag)) {
                    try {
                        java.lang.reflect.Method method = BaseClickableHookItem.class.getDeclaredMethod("saveData");
                        method.setAccessible(true);
                        method.invoke(item);
                    } catch (Throwable e) {
                        LogUtils.e(e);
                    }
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static List<BaseSwitchHookItem> getSwitchHookItems() {
        return (List<BaseSwitchHookItem>) (List<?>) HookRegistry.getHookItemsByClass(BaseSwitchHookItem.class);
    }

    @SuppressWarnings("unchecked")
    public static List<BaseClickableHookItem> getClickableHookItems() {
        return (List<BaseClickableHookItem>) (List<?>) HookRegistry.getHookItemsByClass(BaseClickableHookItem.class);
    }

    @androidx.annotation.NonNull
    public static java.util.List<me.lengyu.qedge.ui.pages.PluginData> getPluginList() {
        me.lengyu.qedge.plugin.PluginManager.loadAll();
        
        java.util.List<me.lengyu.qedge.ui.pages.PluginData> list = new java.util.ArrayList<>();
        try {
            for (PluginInfo plugin : me.lengyu.qedge.plugin.PluginManager.plugins) {
                list.add(new me.lengyu.qedge.ui.pages.PluginData(
                    plugin.getId(),
                    plugin.getName(),
                    plugin.getVersion(),
                    plugin.getAuthor(),
                    plugin.getDesc(),
                    plugin.isRunning(),
                    me.lengyu.qedge.plugin.PluginManager.autoLoadList.contains(plugin.getId()),
                    plugin.getDirPath()
                ));
            }
        } catch (Throwable e) {
            LogUtils.e("MainHook", e);
        }
        return list;
    }

    public static void setPluginRunning(String pluginId, boolean running) {
        try {
            for (PluginInfo plugin : me.lengyu.qedge.plugin.PluginManager.plugins) {
                if (plugin.getId().equals(pluginId)) {
                    if (running) {
                        me.lengyu.qedge.plugin.PluginManager.startPlugin(plugin);
                    } else {
                        me.lengyu.qedge.plugin.PluginManager.stopPlugin(plugin);
                    }
                    break;
                }
            }
        } catch (Throwable e) {
            LogUtils.e("[DEBUG-PLUGIN]", "DP-002 ERROR: " + e.getMessage());
            LogUtils.e("[DEBUG-PLUGIN]", e);
            LogUtils.e("MainHook", e);
        }
    }

    public static void setPluginAutoLoad(String pluginId, boolean autoLoad) {
        try {
            for (PluginInfo plugin : me.lengyu.qedge.plugin.PluginManager.plugins) {
                if (plugin.getId().equals(pluginId)) {
                    me.lengyu.qedge.plugin.PluginManager.setAutoLoad(plugin, autoLoad);
                    break;
                }
            }
        } catch (Throwable e) {
            LogUtils.e("MainHook", e);
        }
    }

    public static void deletePlugin(String pluginId) {
        try {
            for (PluginInfo plugin : me.lengyu.qedge.plugin.PluginManager.plugins) {
                if (plugin.getId().equals(pluginId)) {
                    me.lengyu.qedge.plugin.PluginManager.deletePlugin(plugin);
                    break;
                }
            }
        } catch (Throwable e) {
            LogUtils.e("MainHook", e);
        }
    }

    public static void reloadPlugin(String pluginId) {
        try {
            for (PluginInfo plugin : me.lengyu.qedge.plugin.PluginManager.plugins) {
                if (plugin.getId().equals(pluginId)) {
                    me.lengyu.qedge.plugin.PluginManager.reloadPlugin(plugin);
                    break;
                }
            }
        } catch (Throwable e) {
            LogUtils.e("MainHook", e);
        }
    }

    public static void createPlugin() {
        try {
            me.lengyu.qedge.plugin.PluginManager.createExamplePlugin();
        } catch (Throwable e) {
            LogUtils.e("MainHook", e);
        }
    }
}
