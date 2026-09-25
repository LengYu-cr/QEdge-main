package me.lengyu.qedge.hook.entry;

import android.app.Activity;
import android.content.Intent;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import me.lengyu.qedge.R;
import me.lengyu.qedge.activity.SettingActivity;
import me.lengyu.qedge.hook.annotation.HookItemAnnotation;
import me.lengyu.qedge.hook.base.BaseApiHookItem;
import me.lengyu.qedge.lifecycle.Parasitics;
import me.lengyu.qedge.utils.LogUtils;
import me.lengyu.qedge.utils.ReflectUtils;
/**
 * @Author 冷雨
 * @Description QQ加号入口
 */
@HookItemAnnotation(category = "entry", process = "All", value = "QQ加号入口")
public class QQPlusInject extends BaseApiHookItem {

    public static final QQPlusInject INSTANCE = new QQPlusInject();

    public static QQPlusInject getInstance() {
        return INSTANCE;
    }

    private static final String POP_CLS = "com.tencent.widget.PopupMenuDialog";

    @Override
    public void loadHook() {
        try {
            ClassLoader classLoader = ReflectUtils.hostClassLoader;
            if (classLoader == null) {
                classLoader = getClass().getClassLoader();
            }
            
            Class<?> PopupCls = XposedHelpers.findClass(POP_CLS, classLoader);
            final Class<?> MenuItemCls = XposedHelpers.findClass("com.tencent.widget.PopupMenuDialog$MenuItem", classLoader);
            final Class<?> listenerCls = XposedHelpers.findClass("com.tencent.widget.PopupMenuDialog$OnClickActionListener", classLoader);
            
            Method targetMethod = null;
            Method[] declaredMethods = PopupCls.getDeclaredMethods();
            int length = declaredMethods.length;
            int i = 0;
            while (true) {
                if (i >= length) {
                    break;
                }
                Method method = declaredMethods[i];
                if (!"conversationPlusBuild".equals(method.getName())) {
                    i++;
                } else {
                    targetMethod = method;
                    break;
                }
            }
            
            if (targetMethod == null) {
                return;
            }
            
            targetMethod.setAccessible(true);
            XposedBridge.hookMethod(targetMethod, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) {
                    try {
                        if (param.args != null && param.args.length >= 3) {
                            Activity activity = (Activity) param.args[0];
                            Parasitics.INSTANCE.injectModuleResources(activity.getResources());
                            
                            List<Object> menuList = (List<Object>) param.args[1];
                            Object originListener = param.args[2];
                            
                            Object qrItem = XposedHelpers.newInstance(MenuItemCls, 
                                new Object[]{Integer.valueOf(R.string.app_name), "QEdge", "QEdge", Integer.valueOf(R.mipmap.ic_launcher)});
                            menuList.add(0, qrItem);
                            
                            Object newListener = Proxy.newProxyInstance(
                                listenerCls.getClassLoader(), 
                                new Class[]{listenerCls}, 
                                new ListenerWrapper(originListener, activity)
                            );
                            param.args[2] = newListener;
                        }
                    } catch (Throwable e) {
                        LogUtils.e("QQPlusInject", "添加菜单失败: " + e.getMessage());
                    }
                }
            });
                        
        } catch (Throwable e) {
            LogUtils.e("QQPlusInject", "loadHook 失败: " + e.getMessage());
        }
    }
    
    private static class ListenerWrapper implements InvocationHandler {
        private Object originListener;
        private Activity activity;
        
        public ListenerWrapper(Object origin, Activity act) {
            this.originListener = origin;
            this.activity = act;
        }
        
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if ("onClickAction".equals(method.getName()) && args != null && args.length > 0) {
                Object menuItem = args[0];
                int menuId = XposedHelpers.getIntField(menuItem, "id");
                
                if (menuId == R.string.app_name) {
                    try {
                        Parasitics.INSTANCE.ensureInitialized(activity);
                        Intent intent = new Intent(activity, SettingActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        intent.addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                        activity.startActivity(intent);
                    } catch (Throwable e) {
                        LogUtils.e("QQPlusInject", "启动Activity失败: " + e.getMessage());
                    }
                    return null;
                }
            }
            if (originListener != null) {
                return method.invoke(originListener, args);
            }
            return null;
        }
    }
}