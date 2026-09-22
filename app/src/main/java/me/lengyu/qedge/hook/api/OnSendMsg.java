package me.lengyu.qedge.hook.api;

import java.util.ArrayList;
import java.lang.reflect.Method;

import me.lengyu.qedge.hook.annotation.HookItemAnnotation;
import me.lengyu.qedge.hook.base.BaseApiHookItem;
import me.lengyu.qedge.hook.base.Listener;
import me.lengyu.qedge.utils.LogUtils;
import me.lengyu.qedge.utils.HookUtils;

import com.tencent.mobileqq.qroute.QRoute;
import com.tencent.qqnt.kernelpublic.nativeinterface.Contact;
import com.tencent.qqnt.kernel.nativeinterface.MsgElement;
/**
 * @Author 冷雨
 * @Description 监听发送消息
 */
@HookItemAnnotation(value = "监听发送消息", category = "api")
public class OnSendMsg extends BaseApiHookItem<OnSendMsg.SendMsgListener> {

    public static final OnSendMsg INSTANCE = new OnSendMsg();

    public OnSendMsg() {}

    @Override
    public void loadHook() {
        try {
            ClassLoader classLoader = QRoute.class.getClassLoader();
            if (classLoader == null) {
                classLoader = getClass().getClassLoader();
            }

            Class<?> cppProxyClass = null;
            try {
                cppProxyClass = classLoader.loadClass("com.tencent.qqnt.kernel.nativeinterface.IKernelMsgService$CppProxy");
            } catch (ClassNotFoundException e) {
                try {
                    cppProxyClass = classLoader.loadClass("com.tencent.qqnt.kernel.nativeinterface.IKernelMsgService.CppProxy");
                } catch (ClassNotFoundException ex) {
                    LogUtils.e("OnSendMsg", "Cannot find IKernelMsgService CppProxy class");
                    return;
                }
            }

            Method sendMsgMethod = null;
            Method[] methods = cppProxyClass.getDeclaredMethods();
            for (Method method : methods) {
                if (method.getName().equals("sendMsg")) {
                    Class<?>[] paramTypes = method.getParameterTypes();
                    if (paramTypes.length == 5) {
                        sendMsgMethod = method;
                        break;
                    }
                }
            }

            if (sendMsgMethod != null) {
                HookUtils.hookBefore(sendMsgMethod, param -> {
                    try {
                        if (param.args.length >= 3) {
                            Object args1 = param.args[1];
                            Object args2 = param.args[2];

                            if (args1 instanceof Contact && args2 instanceof ArrayList) {
                                Contact contact = (Contact) args1;
                                ArrayList<?> elements = (ArrayList<?>) args2;
                                notifyListeners(contact, elements);
                            }
                        }
                    } catch (Throwable e) {
                        LogUtils.e("OnSendMsg", "sendMsg callback error: " + e.getMessage());
                    }
                });
            } else {
                LogUtils.e("OnSendMsg", "sendMsg method not found");
            }

        } catch (Throwable e) {
            LogUtils.e("OnSendMsg", "loadHook error: " + e.getMessage());
            LogUtils.e(e);
        }
    }

    private void notifyListeners(Contact contact, ArrayList<?> elements) {
        forEachChecked(listener -> listener.onSend(contact, elements));
    }

    public interface SendMsgListener extends Listener {
        void onSend(Contact contact, ArrayList<?> elements);
    }

    public static void registerListener(SendMsgListener listener) {
        INSTANCE.addListener(listener);
    }

    public static void unregisterListener(SendMsgListener listener) {
        INSTANCE.removeListener(listener);
    }
}