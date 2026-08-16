package me.lengyu.qedge.plugin;

import java.io.File;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import bsh.Interpreter;
import me.lengyu.qedge.hook.MainHook;
import me.lengyu.qedge.hook.api.OnMenuBuild;
import me.lengyu.qedge.hook.api.OnPaiYiPai;
import me.lengyu.qedge.hook.api.OnReceiveMsg;
import me.lengyu.qedge.hook.api.OnSendMsg;
import me.lengyu.qedge.hook.api.OnTroopJoin;
import me.lengyu.qedge.hook.api.OnTroopQuit;
import me.lengyu.qedge.hook.api.OnTroopShutUp;
import me.lengyu.qedge.lifecycle.DynamicActivityRegistry;
import me.lengyu.qedge.plugin.api.PluginMethod;
import me.lengyu.qedge.plugin.bean.MsgData;
import me.lengyu.qedge.plugin.bean.PluginInfo;
import me.lengyu.qedge.utils.HostInfo;
import me.lengyu.qedge.utils.LogUtils;
import me.lengyu.qedge.utils.QQCurrentEnv;
import me.lengyu.qedge.utils.ReflectUtils;

public class PluginCompiler {
    public final PluginInfo info;
    private Interpreter interpreter;
    private final Map<String, String> menuItems = new LinkedHashMap<>();
    private final List<String> msgMenuItem = new ArrayList<>();
    private final List<String> registedActivitys = new ArrayList<>();
    private final PluginMethod api;
    private final PluginCallback callback;
    private final FixClassLoader loader;

    private OnReceiveMsg.ReceiveMsgListener receiveMsgListener;
    private OnSendMsg.SendMsgListener sendMsgListener;
    private OnTroopJoin.TroopJoinListener troopJoinListener;
    private OnTroopQuit.TroopQuitListener troopQuitListener;
    private OnTroopShutUp.TroopShutUpListener troopShutUpListener;
    private OnPaiYiPai.PaiYiPaiListener paiYiPaiListener;

    public PluginCompiler(PluginInfo info) {
        this.info = info;
        this.api = new PluginMethod(this);
        this.callback = new PluginCallback(this);
        this.loader = new FixClassLoader();
    }

    public synchronized void start() {

        if (info.isRunning()) {
            stop(false);
        }
        info.updateFromDisk();

        File scriptFile = new File(info.getDirPath(), "main.java");
        if (!scriptFile.exists()) {
            LogUtils.e("[DEBUG-PLUGIN]", "DP-004 ERROR: main.java not found!");
            throw new IllegalStateException("main.java not found");
        }

        try {
            interpreter = new Interpreter();
            Object hostContext = HostInfo.getHostContext();
            String currentUin = QQCurrentEnv.getCurrentUin();
            Object classLoader = me.lengyu.qedge.utils.ReflectUtils.hostClassLoader;

            interpreter.set("context", hostContext);
            interpreter.set("myUin", currentUin);
            interpreter.set("classLoader", classLoader);
            interpreter.set("pluginPath", info.getDirPath());
            interpreter.set("pluginId", info.getId());
            interpreter.setClassLoader(loader);
            registerApiMethods(interpreter);

            info.setRunning(true);

            try {
                interpreter.source(scriptFile.getAbsolutePath());
            } catch (bsh.EvalError e) {
                LogUtils.e("[DEBUG-PLUGIN]", "DP-004 ERROR: BeanShell EvalError");
                LogUtils.e("[DEBUG-PLUGIN]", e);
                throw new RuntimeException(e);
            }

            registerCallbacks();

        } catch (Throwable e) {
            LogUtils.e("[DEBUG-PLUGIN]", "DP-004 FATAL ERROR: " + e.getMessage());
            LogUtils.e("[DEBUG-PLUGIN]", e);
            stop(false);
            throw new RuntimeException(e);
        }
    }

    public synchronized void stop(boolean invokeCallback) {
        if (!info.isRunning()) return;

        if (invokeCallback) {
            try {
                callback.unLoadPlugin();
            } catch (Exception e) {
                PluginError.evalError(e, info);
            }
        }

        try {
            for (String activityName : registedActivitys) {
                DynamicActivityRegistry.unregister(activityName);
            }
            removeCallbacks();
            if (interpreter != null && interpreter.getNameSpace() != null) {
                interpreter.getNameSpace().clear();
            }
            menuItems.clear();
            msgMenuItem.clear();
            registedActivitys.clear();
        } catch (Exception e) {
            PluginError.callError(e, info);
        } finally {
            info.setRunning(false);
        }
    }

    private void registerCallbacks() {
        receiveMsgListener = callback.getReceiveMsgListener();
        sendMsgListener = callback.getSendMsgListener();
        troopJoinListener = callback.getTroopJoinListener();
        troopQuitListener = callback.getTroopQuitListener();
        troopShutUpListener = callback.getTroopShutUpListener();
        paiYiPaiListener = callback.getPaiYiPaiListener();

        try {
            OnReceiveMsg.registerListener(receiveMsgListener);
            OnSendMsg.registerListener(sendMsgListener);
            OnTroopJoin.registerListener(troopJoinListener);
            OnTroopQuit.registerListener(troopQuitListener);
            OnTroopShutUp.registerListener(troopShutUpListener);
            OnPaiYiPai.registerListener(paiYiPaiListener);
        } catch (Throwable e) {
            LogUtils.e(e);
        }

        for (String item : msgMenuItem) {
            String[] args = item.split(",");
            if (args.length >= 4) {
                String finalMethodName = args[3];
                OnMenuBuild.addMenuListener(item, (me.lengyu.qedge.plugin.bean.MsgData msgData, String menuKey) -> {
                    if (menuKey.equals(item)) {
                        callback.invokeMsgMenuItem(finalMethodName, msgData);
                    }
                    return null;
                });
            }
        }
    }

     private void removeCallbacks() {
        try {
            OnReceiveMsg.unregisterListener(receiveMsgListener);
            OnSendMsg.unregisterListener(sendMsgListener);
            OnTroopJoin.unregisterListener(troopJoinListener);
            OnTroopQuit.unregisterListener(troopQuitListener);
            OnTroopShutUp.unregisterListener(troopShutUpListener);
            OnPaiYiPai.unregisterListener(paiYiPaiListener);
        } catch (Throwable e) {
            LogUtils.e(e);
        }
        OnMenuBuild.removeMenuListenersForItem(item -> {
            String[] args = item.split(",");
            return args.length >= 4 && msgMenuItem.contains(item);
        });
    }


    public Interpreter getInterpreter() {
        return interpreter;
    }

    public Map<String, String> getMenuItems() {
        return menuItems;
    }

    public List<String> getMsgMenuItem() {
        return msgMenuItem;
    }

    public List<String> getRegistedActivitys() {
        return registedActivitys;
    }

    public FixClassLoader getLoader() {
        return loader;
    }

    public PluginCallback getCallback() {
        return callback;
    }

    public void addMenuItem(String item) {
        msgMenuItem.add(item);
    }

    public void registerActivity(String activityClassName) {
        registedActivitys.add(activityClassName);
    }

    public Object getQqAppInterface() {
        return me.lengyu.qedge.utils.QQCurrentEnv.getQQAppInterface();
    }

    private void registerApiMethods(bsh.Interpreter interpreter) {
        try {
            bsh.NameSpace nameSpace = interpreter.getNameSpace();
            int registeredCount = 0;
            for (java.lang.reflect.Method method : PluginMethod.class.getDeclaredMethods()) {
                if (java.lang.reflect.Modifier.isPublic(method.getModifiers())
                        && !method.getName().contains("$")) {
                    nameSpace.setMethod(new bsh.BshMethod(method, api));
                    registeredCount++;
                }
            }
        } catch (Exception e) {
            LogUtils.e("[DEBUG-PLUGIN]", "DP-005 ERROR: registerApiMethods failed: " + e.getMessage());
            LogUtils.e("[DEBUG-PLUGIN]", e);
            LogUtils.e("PluginCompiler", "registerApiMethods failed: " + e.getMessage());
        }
    }
}