package me.lengyu.qedge.plugin;

import bsh.BshMethod;
import bsh.Interpreter;
import bsh.NameSpace;
import me.lengyu.qedge.hook.api.OnPaiYiPai;
import me.lengyu.qedge.hook.api.OnReceiveMsg;
import me.lengyu.qedge.hook.api.OnSendMsg;
import me.lengyu.qedge.hook.api.OnTroopJoin;
import me.lengyu.qedge.hook.api.OnTroopQuit;
import me.lengyu.qedge.hook.api.OnTroopShutUp;
import me.lengyu.qedge.plugin.bean.MsgData;
import me.lengyu.qedge.plugin.bean.PluginInfo;

import java.util.Arrays;

/**
 * @Author 冷雨
 * @Description 插件回调
 */
public class PluginCallback {
    private final PluginCompiler compiler;
    private final PluginInfo info;

    private OnReceiveMsg.ReceiveMsgListener receiveMsgListener;
    private OnSendMsg.SendMsgListener sendMsgListener;
    private OnTroopJoin.TroopJoinListener troopJoinListener;
    private OnTroopQuit.TroopQuitListener troopQuitListener;
    private OnTroopShutUp.TroopShutUpListener troopShutUpListener;
    private OnPaiYiPai.PaiYiPaiListener paiYiPaiListener;

    public PluginCallback(PluginCompiler compiler) {
        this.compiler = compiler;
        this.info = compiler.info;
        initListeners();
    }

    private void initListeners() {
        receiveMsgListener = msgRecord -> runOnBackground(
                "onMsg",
                new Class[]{Object.class},
                new Object[]{new MsgData(msgRecord)}
        );

        troopJoinListener = (troopUin, memberUin) -> runOnBackground(
                "joinGroup",
                new Class[]{String.class, String.class},
                new Object[]{troopUin, memberUin}
        );

        troopQuitListener = (troopUin, memberUin) -> runOnBackground(
                "quitGroup",
                new Class[]{String.class, String.class},
                new Object[]{troopUin, memberUin}
        );

        troopShutUpListener = (troopUin, memberUin, time, opUin) -> runOnBackground(
                "shutUpGroup",
                new Class[]{String.class, String.class, Long.class, String.class},
                new Object[]{troopUin, memberUin, time, opUin}
        );

        paiYiPaiListener = (peerUin, chatType, fromUin) -> runOnBackground(
                "onPaiYiPai",
                new Class[]{String.class, int.class, String.class},
                new Object[]{peerUin, chatType, fromUin}
        );

        sendMsgListener = (contact, elements) -> {
            if (info.isJs()) {
                handleSendMsgJs(elements);
                return;
            }
            Interpreter interpreter = info.getCompiler().getInterpreter();
            if (interpreter == null) return;
            NameSpace nameSpace = interpreter.getNameSpace();
            if (nameSpace == null) return;

            String[] methodNames = nameSpace.getMethodNames();
            if (methodNames != null && (Arrays.asList(methodNames).contains("getMsg") || Arrays.asList(methodNames).contains("getSummary"))) {
                try {
                    if (elements instanceof Iterable) {
                        for (Object element : (Iterable<?>) elements) {
                            Object textElement = me.lengyu.qedge.utils.ReflectUtils.getFieldValue(element, "textElement");
                            if (textElement != null) {
                                String content = (String) me.lengyu.qedge.utils.ReflectUtils.getFieldValue(textElement, "content");
                                BshMethod method = nameSpace.getMethod("getMsg", new Class[]{String.class});
                                if (method != null) {
                                    Object result = method.invoke(new Object[]{content}, interpreter);
                                    if (result instanceof String) {
                                        me.lengyu.qedge.utils.ReflectUtils.setFieldValue(textElement, "content", result);
                                    }
                                }
                            }

                            Object picElement = me.lengyu.qedge.utils.ReflectUtils.getFieldValue(element, "picElement");
                            if (picElement != null) {
                                String summary = (String) me.lengyu.qedge.utils.ReflectUtils.getFieldValue(picElement, "summary");
                                BshMethod method = nameSpace.getMethod("getSummary", new Class[]{String.class});
                                if (method != null) {
                                    if (summary == null) summary = "";
                                    Object result = method.invoke(new Object[]{summary}, interpreter);
                                    if (result instanceof String) {
                                        me.lengyu.qedge.utils.ReflectUtils.setFieldValue(picElement, "summary", result);
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    PluginError.callError(e, info);
                }
            }
        };
    }

    /** JS 插件的发送消息拦截：调用脚本里定义的 getMsg/getSummary 修改文本/图片摘要 */
    private void handleSendMsgJs(Object elements) {
        JsRuntime rt = info.getCompiler().getJsRuntime();
        if (rt == null) return;
        boolean hasGetMsg = rt.hasFunction("getMsg");
        boolean hasGetSummary = rt.hasFunction("getSummary");
        if (!hasGetMsg && !hasGetSummary) return;
        try {
            if (elements instanceof Iterable) {
                for (Object element : (Iterable<?>) elements) {
                    if (hasGetMsg) {
                        Object textElement = me.lengyu.qedge.utils.ReflectUtils.getFieldValue(element, "textElement");
                        if (textElement != null) {
                            String content = (String) me.lengyu.qedge.utils.ReflectUtils.getFieldValue(textElement, "content");
                            Object result = rt.callFunctionSync("getMsg", content);
                            if (result instanceof String) {
                                me.lengyu.qedge.utils.ReflectUtils.setFieldValue(textElement, "content", result);
                            }
                        }
                    }
                    if (hasGetSummary) {
                        Object picElement = me.lengyu.qedge.utils.ReflectUtils.getFieldValue(element, "picElement");
                        if (picElement != null) {
                            String summary = (String) me.lengyu.qedge.utils.ReflectUtils.getFieldValue(picElement, "summary");
                            if (summary == null) summary = "";
                            Object result = rt.callFunctionSync("getSummary", summary);
                            if (result instanceof String) {
                                me.lengyu.qedge.utils.ReflectUtils.setFieldValue(picElement, "summary", result);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            PluginError.callError(e, info);
        }
    }

    public void unLoadPlugin() {
        invokeMethodExists("unLoadPlugin", new Class[]{}, new Object[]{});
    }

    public void chatInterface(int chatType, String peerUin, String peerName) {
        runOnBackground(
                "chatInterface",
                new Class[]{int.class, String.class, String.class},
                new Object[]{chatType, peerUin, peerName}
        );
    }

    public void invokeMsgMenuItem(String methodName, MsgData msgData) {
        runOnBackground(methodName, new Class[]{Object.class}, new Object[]{msgData});
    }

    public void invokeMenuItem(String methodName, int chatType, String peerUin, String peerName) {
        runOnBackground(
                methodName,
                new Class[]{int.class, String.class, String.class},
                new Object[]{chatType, peerUin, peerName}
        );
    }

    private void runOnBackground(String methodName, Class<?>[] paramTypes, Object[] args) {
        if (info.isJs()) {
            // JS 插件走自己的专用单线程 Executor，不能再包 new Thread(跨线程碰 Rhino scope 会崩)
            JsRuntime rt = info.getCompiler().getJsRuntime();
            if (rt != null) {
                rt.callFunctionAsync(methodName, args);
            }
            return;
        }
        new Thread(() -> invokeMethodExists(methodName, paramTypes, args), "Plugin-" + info.getId()).start();
    }

    private void invokeMethodExists(String methodName, Class<?>[] paramTypes, Object[] args) {
        try {
            if (info.isJs()) {
                JsRuntime rt = info.getCompiler().getJsRuntime();
                if (rt != null && rt.hasFunction(methodName)) {
                    rt.callFunctionSync(methodName, args);
                }
                return;
            }
            Interpreter interpreter = info.getCompiler().getInterpreter();
            if (interpreter == null || interpreter.getNameSpace() == null) return;

            NameSpace nameSpace = interpreter.getNameSpace();
            String[] methodNames = nameSpace.getMethodNames();
            if (methodNames != null && Arrays.asList(methodNames).contains(methodName)) {
                BshMethod method = nameSpace.getMethod(methodName, paramTypes);
                if (method != null) {
                    method.invoke(args, interpreter);
                }
            }
        } catch (Exception e) {
            PluginError.callError(e, info);
        }
    }

    public OnReceiveMsg.ReceiveMsgListener getReceiveMsgListener() { return receiveMsgListener; }
    public OnSendMsg.SendMsgListener getSendMsgListener() { return sendMsgListener; }
    public OnTroopJoin.TroopJoinListener getTroopJoinListener() { return troopJoinListener; }
    public OnTroopQuit.TroopQuitListener getTroopQuitListener() { return troopQuitListener; }
    public OnTroopShutUp.TroopShutUpListener getTroopShutUpListener() { return troopShutUpListener; }
    public OnPaiYiPai.PaiYiPaiListener getPaiYiPaiListener() { return paiYiPaiListener; }
}