package me.lengyu.qedge.hook.api;

import java.util.ArrayList;
import java.util.List;
import java.lang.reflect.Modifier;

import me.lengyu.qedge.hook.annotation.HookItemAnnotation;
import me.lengyu.qedge.hook.base.BaseApiHookItem;
import me.lengyu.qedge.hook.base.Listener;
import me.lengyu.qedge.utils.HookUtils;
import me.lengyu.qedge.utils.LogUtils;
import me.lengyu.qedge.utils.ReflectUtils;
import me.lengyu.qedge.utils.dexkit.DexKitCache;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindClass;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.query.matchers.ClassMatcher;
import org.luckypray.dexkit.query.matchers.MethodsMatcher;
import org.luckypray.dexkit.result.ClassData;
import org.luckypray.dexkit.wrap.DexClass;

import android.content.Context;
import me.lengyu.qedge.utils.HostInfo;

/**
 * @Author 冷雨
 * @Description 监听接收消息
 */
@HookItemAnnotation(value = "监听接收消息", category = "api")
public class OnReceiveMsg extends BaseApiHookItem<OnReceiveMsg.ReceiveMsgListener> {

    public static final OnReceiveMsg INSTANCE = new OnReceiveMsg();
    private static final String CACHE_KEY = "OnReceiveMsg->msgService";

    public OnReceiveMsg() {}

    @Override
    public void loadHook() {
        try {
            ClassLoader classLoader = ReflectUtils.hostClassLoader;
            if (classLoader == null) {
                classLoader = getClass().getClassLoader();
            }

            Class<?> msgServiceClass = findMsgServiceClass(classLoader);
            if (msgServiceClass == null) {
                LogUtils.e("OnReceiveMsg", "Cannot find msgService class");
                return;
            }

            try {
                java.lang.reflect.Method onRecvMsgMethod = ReflectUtils.findMethod(msgServiceClass, "onRecvMsg");
                if (onRecvMsgMethod != null) {
                    HookUtils.hookAfter(onRecvMsgMethod, param -> {
                        try {
                            Object args0 = param.args[0];
                            if (args0 instanceof ArrayList) {
                                ArrayList<?> msgRecords = (ArrayList<?>) args0;
                                if (!msgRecords.isEmpty()) {
                                    Object msgRecord = msgRecords.get(0);
                                    notifyListeners(msgRecord);
                                }
                            }
                        } catch (Throwable e) {
                            LogUtils.e("OnReceiveMsg", "onRecvMsg callback error: " + e.getMessage());
                        }
                    });
                } else {
                    LogUtils.e("OnReceiveMsg", "onRecvMsg method not found");
                }
            } catch (Throwable e) {
                LogUtils.e("OnReceiveMsg", "Failed to hook onRecvMsg: " + e.getMessage());
            }

            try {
                java.lang.reflect.Method onAddSendMsgMethod = ReflectUtils.findMethod(msgServiceClass, "onAddSendMsg");
                if (onAddSendMsgMethod != null) {
                    HookUtils.hookAfter(onAddSendMsgMethod, param -> {
                        try {
                            Object msgRecord = param.args[0];
                            notifyListeners(msgRecord);
                        } catch (Throwable e) {
                            LogUtils.e("OnReceiveMsg", "onAddSendMsg callback error: " + e.getMessage());
                        }
                    });
                } else {
                    LogUtils.e("OnReceiveMsg", "onAddSendMsg method not found");
                }
            } catch (Throwable e) {
                LogUtils.e("OnReceiveMsg", "Failed to hook onAddSendMsg: " + e.getMessage());
            }

        } catch (Throwable e) {
            LogUtils.e("OnReceiveMsg", "loadHook error: " + e.getMessage());
            LogUtils.e(e);
        }
    }

    private Class<?> findMsgServiceClass(ClassLoader classLoader) {
        try {
            String cachedDescriptor = DexKitCache.getDescriptor(CACHE_KEY);
            if (cachedDescriptor != null) {
                try {
                    Class<?> clazz = new DexClass(cachedDescriptor).getInstance(classLoader);
                    if (hasMethod(clazz, "onRecvMsg") && hasMethod(clazz, "onAddSendMsg")) {
                        return clazz;
                    }
                } catch (Throwable e) {
                    LogUtils.w("OnReceiveMsg", "Cached class invalid, re-finding: " + e.getMessage());
                }
            }
        } catch (Throwable e) {
            LogUtils.w("OnReceiveMsg", "Failed to read DexKit cache: " + e.getMessage());
        }

        Class<?> result = findMsgServiceByDexKit(classLoader);
        if (result != null) {
            return result;
        }

        result = findMsgServiceByHardcode(classLoader);
        if (result != null) {
            return result;
        }

        return findMsgServiceByScan(classLoader);
    }

    private Class<?> findMsgServiceByDexKit(ClassLoader classLoader) {
        try {
            Context hostContext = HostInfo.getHostContext();
            if (hostContext == null) {
                LogUtils.w("OnReceiveMsg", "Host context not available, skip DexKit");
                return null;
            }

            String sourceDir = hostContext.getApplicationInfo().sourceDir;
            if (sourceDir == null) {
                LogUtils.w("OnReceiveMsg", "Source dir not available, skip DexKit");
                return null;
            }

            if (!me.lengyu.qedge.utils.dexkit.DexKitManager.ensureLibrary()) {
                LogUtils.w("OnReceiveMsg", "dexkit lib unavailable, skip DexKit");
                return null;
            }

            try (DexKitBridge bridge = DexKitBridge.create(sourceDir)) {
                if (bridge == null) {
                    LogUtils.e("OnReceiveMsg", "Failed to create DexKitBridge");
                    return null;
                }

                FindClass query = new FindClass();
                query.searchPackages("com.tencent.qqnt.msg");
                query.excludePackages("com.tencent.qqnt.msg.migration");
                query.matcher(new ClassMatcher()
                    .modifiers(Modifier.FINAL)
                    .methods(new MethodsMatcher()
                        .add(new MethodMatcher().name("onRecvMsg"))
                        .add(new MethodMatcher().name("onAddSendMsg"))
                    )
                );

                List<ClassData> result = bridge.findClass(query);
                if (!result.isEmpty()) {
                    ClassData classData = result.get(0);
                    String descriptor = classData.getDescriptor();
                    DexKitCache.put(CACHE_KEY, descriptor);
                    // 落盘，避免每次启动都重新全量扫描（此前每次耗时 5~6 秒）
                    DexKitCache.saveCache();

                    Class<?> clazz = new DexClass(descriptor).getInstance(classLoader);
                    return clazz;
                }
            }
        } catch (Throwable e) {
            LogUtils.e("OnReceiveMsg", "DexKit search failed: " + e.getMessage());
            LogUtils.e(e);
        }
        return null;
    }

    private Class<?> findMsgServiceByHardcode(ClassLoader classLoader) {
        String[] possibleClasses = {
            "com.tencent.qqnt.msg.MsgService",
            "com.tencent.qqnt.msg.service.MsgServiceImpl",
            "com.tencent.qqnt.msg.service.MsgService",
            "com.tencent.mobileqq.msg.service.MsgServiceImpl",
            "com.tencent.mobileqq.msg.service.MsgService",
            "com.tencent.qqnt.msg.core.MsgService",
            "com.tencent.qqnt.msg.core.MsgServiceImpl"
        };

        for (String className : possibleClasses) {
            try {
                Class<?> clazz = classLoader.loadClass(className);
                if (hasMethod(clazz, "onRecvMsg") && hasMethod(clazz, "onAddSendMsg")) {
                    return clazz;
                }
            } catch (ClassNotFoundException ignored) {
            }
        }
        return null;
    }

    private Class<?> findMsgServiceByScan(ClassLoader classLoader) {
        try {
            java.lang.reflect.Field packagesField = ClassLoader.class.getDeclaredField("packages");
            packagesField.setAccessible(true);
            Object packages = packagesField.get(classLoader);
            if (packages instanceof java.util.Map) {
                java.util.Map<?, ?> pkgMap = (java.util.Map<?, ?>) packages;
                for (Object key : pkgMap.keySet()) {
                    String pkgName = key.toString();
                    if (pkgName.contains("com.tencent.qqnt.msg") && !pkgName.contains("migration")) {
                        try {
                            java.lang.reflect.Method definedClassesMethod = classLoader.getClass().getDeclaredMethod("getDefinedClasses");
                            definedClassesMethod.setAccessible(true);
                            java.util.Set<Class<?>> definedClasses = (java.util.Set<Class<?>>) definedClassesMethod.invoke(classLoader);
                            for (Class<?> clazz : definedClasses) {
                                if (clazz.getName().startsWith(pkgName) &&
                                    Modifier.isFinal(clazz.getModifiers()) &&
                                    hasMethod(clazz, "onRecvMsg") &&
                                hasMethod(clazz, "onAddSendMsg")){
                                    return clazz;
                                }
                            }
                        } catch (Throwable ignored) {
                        }
                    }
                }
            }
        } catch (Throwable e) {
            LogUtils.e("OnReceiveMsg", "scanForMsgService error: " + e.getMessage());
        }
        return null;
    }

    private boolean hasMethod(Class<?> clazz, String methodName) {
        try {
            java.lang.reflect.Method[] methods = clazz.getDeclaredMethods();
            for (java.lang.reflect.Method method : methods) {
                if (method.getName().equals(methodName)) {
                    return true;
                }
            }
            if (clazz.getSuperclass() != null) {
                return hasMethod(clazz.getSuperclass(), methodName);
            }
        } catch (Throwable e) {
        }
        return false;
    }

    private void notifyListeners(Object msgRecord) {
        forEachChecked(listener -> listener.onReceive(msgRecord));
    }

    public interface ReceiveMsgListener extends Listener {
        void onReceive(Object msgRecord);
    }

    public static void registerListener(ReceiveMsgListener listener) {
        INSTANCE.addListener(listener);
    }

    public static void unregisterListener(ReceiveMsgListener listener) {
        INSTANCE.removeListener(listener);
    }
}
