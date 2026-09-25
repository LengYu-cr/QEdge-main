package me.lengyu.qedge.hook.item;

import android.view.View;

import java.lang.reflect.Field;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

import me.lengyu.qedge.hook.annotation.HookItemAnnotation;
import me.lengyu.qedge.hook.base.BaseApiHookItem;
import me.lengyu.qedge.utils.LogUtils;
import me.lengyu.qedge.utils.ModuleConfig;
import me.lengyu.qedge.utils.ReflectUtils;

@HookItemAnnotation(value = "全屏按钮0行显示", category = "item")
public class ForceFullScreenBtnShow extends BaseApiHookItem {

    public static final ForceFullScreenBtnShow INSTANCE = new ForceFullScreenBtnShow();
    private static final String TAG = "ForceFullScreenBtnShow";

    private static final String STATE_CLASS =
            "com.tencent.mobileqq.aio.input.fullscreen.entry.FullScreenBtnUIState$UpdateFullScreenBtnStatus";

    private static boolean isEnabled() {
        return ModuleConfig.INSTANCE.getBoolean("force_fullscreen_btn_show", false);
    }

    @Override
    public void loadHook() {
        try {
            Class<?> cls = ReflectUtils.hostClassLoader.loadClass(STATE_CLASS);

            XposedBridge.hookAllConstructors(cls, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!isEnabled()) return;
                    if (param.args == null || param.args.length < 2) return;
                    // 第一个参数 isShowBtn 强制为 true
                    param.args[0] = Boolean.TRUE;
                }
            });
        } catch (Throwable t) {
            LogUtils.e(TAG, "loadHook: " + t);
        }
    }
}