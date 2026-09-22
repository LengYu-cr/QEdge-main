package me.lengyu.qedge.hook.item;

import java.lang.reflect.Method;

import de.robv.android.xposed.XposedHelpers;
import me.lengyu.qedge.hook.annotation.HookItemAnnotation;
import me.lengyu.qedge.hook.base.BaseApiHookItem;
import me.lengyu.qedge.hook.base.Listener;
import me.lengyu.qedge.utils.HookUtils;
import me.lengyu.qedge.utils.LogUtils;
import me.lengyu.qedge.utils.ModuleConfig;
import me.lengyu.qedge.utils.ReflectUtils;
/**
 * @Author 冷雨
 * @Description 闪照破解
 */
@HookItemAnnotation(value = "闪照破解", category = "item")
public class FlashPicBypass extends BaseApiHookItem<FlashPicBypass.FlashPicListener> {

    public static final FlashPicBypass INSTANCE = new FlashPicBypass();
    private static final String TAG = "FlashPicBypass";

    private static boolean isEnabled() {
        return ModuleConfig.INSTANCE.getBoolean("flash_pic_bypass", false);
    }

    @Override
    public void loadHook() {
        try {
            final ClassLoader classLoader = ReflectUtils.hostClassLoader != null
                ? ReflectUtils.hostClassLoader
                : FlashPicBypass.class.getClassLoader();

            Class<?> msgRecordClass = Class.forName(
                "com.tencent.qqnt.kernel.nativeinterface.MsgRecord",
                false,
                classLoader
            );

            // hook MsgRecord 所有 public 方法，before 阶段改 subMsgType
            Method[] methods = msgRecordClass.getDeclaredMethods();
            int count = 0;
            for (Method method : methods) {
                if (method.getName().equals("getClass")) continue;
                if (method.getName().equals("hashCode")) continue;
                if (method.getName().equals("equals")) continue;
                if (method.getName().equals("notify")) continue;
                if (method.getName().equals("notifyAll")) continue;
                if (method.getName().equals("wait")) continue;

                HookUtils.hookBefore(method, param -> {
                    if (!isEnabled()) return;
                    try {
                        int subMsgType = XposedHelpers.getIntField(param.thisObject, "subMsgType");
                        if (subMsgType == 8194) {
                            XposedHelpers.setIntField(param.thisObject, "subMsgType", subMsgType & ~8192);
                        }
                    } catch (Throwable ignored) {
                    }
                });
                count++;
            }

            // LogUtils.d(TAG, "hook MsgRecord success, count=" + count);
        } catch (Throwable e) {
            LogUtils.e(TAG, "loadHook error: " + e.getMessage());
        }
    }

    public interface FlashPicListener extends Listener {
    }
}
