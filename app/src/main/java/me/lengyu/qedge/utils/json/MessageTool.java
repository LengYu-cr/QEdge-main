package me.lengyu.qedge.utils.json;

import org.json.JSONObject;
import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindClass;
import org.luckypray.dexkit.query.matchers.ClassMatcher;
import org.luckypray.dexkit.result.ClassData;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

import me.lengyu.qedge.utils.LogUtils;

public class MessageTool {

    private static Class<?> messageClass;
    private static Class<?> fieldDescriptorClass;
    
    private static Method methodGetValues;
    private static Method methodReadValue;
    private static Method methodPutValue;

    private static boolean inited = false;

    public static boolean isInited() {
        return inited;
    }

    public static void init(String sourceDir, ClassLoader classLoader) {
        if (inited) return;
        try {
            DexKitBridge bridge = DexKitBridge.create(sourceDir);
            if (bridge == null) return;

            try {
                FindClass messageQuery = new FindClass();
                messageQuery.searchPackages("pbandk");
                ClassData messageData = bridge.findClass(messageQuery).singleOrNull();
                
                if (messageData != null) {
                    messageClass = messageData.getInstance(classLoader);
                }

                FindClass fieldQuery = new FindClass();
                fieldQuery.searchPackages("pbandk");
                fieldQuery.excludePackages("pbandk.internal");
                ClassData fieldData = bridge.findClass(fieldQuery).singleOrNull();

                if (fieldData != null) {
                    fieldDescriptorClass = fieldData.getInstance(classLoader);
                }

                if (messageClass != null) {
                    for (Method m : messageClass.getDeclaredMethods()) {
                        if (m.getParameterCount() == 0 && Map.class.isAssignableFrom(m.getReturnType())) {
                            methodGetValues = m;
                            methodGetValues.setAccessible(true);
                            break;
                        }
                    }
                    for (Method m : messageClass.getDeclaredMethods()) {
                        if (m.getParameterCount() == 2 && m.getParameterTypes()[0] == int.class && m.getParameterTypes()[1] == Object.class) {
                            methodPutValue = m;
                            methodPutValue.setAccessible(true);
                            break;
                        }
                    }
                }

                if (fieldDescriptorClass != null) {
                    for (Method m : fieldDescriptorClass.getDeclaredMethods()) {
                        if (m.getParameterCount() == 0) {
                            methodReadValue = m;
                            methodReadValue.setAccessible(true);
                            break;
                        }
                    }
                }
            } finally {
                bridge.close();
            }
            inited = true;
        } catch (Throwable e) {
            LogUtils.e(e);
        }
    }

    /**
     * 核心读取逻辑
     * @param message 消息对象
     * @param tag 字段号
     * @return 值对象
     */
    public static Object getValue(Object message, int tag) {
        try {
            if (methodGetValues == null || methodReadValue == null) return null;
            
            // 调用 message.getValues()
            Map<Integer, Object> valueMap = (Map<Integer, Object>) methodGetValues.invoke(message);
            if (valueMap == null) return null;

            Object descriptor = valueMap.get(tag);
            if (descriptor == null) return null;

            // 调用 descriptor.readValue()
            return methodReadValue.invoke(descriptor);
            
        } catch (Exception e) {
            LogUtils.e(e);
            return null;
        }
    }

    /**
     * 链式读取，模拟 walk
     * 例如：getProtoValue(obj, 3, 2, 1)
     */
    public static Object getProtoValue(Object message, int... tags) {
        Object current = message;
        for (int tag : tags) {
            if (current == null) return null;
            current = getValue(current, tag);
        }
        return current;
    }
    
    public static boolean isMessage(Object obj) {
        return messageClass != null && messageClass.isInstance(obj);
    }
}

