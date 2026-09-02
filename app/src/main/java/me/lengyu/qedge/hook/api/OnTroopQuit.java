package me.lengyu.qedge.hook.api;

import me.lengyu.qedge.hook.annotation.HookItemAnnotation;
import me.lengyu.qedge.hook.base.BaseApiHookItem;
import me.lengyu.qedge.hook.base.Listener;
import me.lengyu.qedge.plugin.bean.QuitData;
import me.lengyu.qedge.utils.HookUtils;
import me.lengyu.qedge.utils.LogUtils;
/**
 * @Author 冷雨
 * @Description 监听群成员退出
 */
@HookItemAnnotation(value = "监听群成员退出", category = "api")
public class OnTroopQuit extends BaseApiHookItem<OnTroopQuit.TroopQuitListener> {

    public static final OnTroopQuit INSTANCE = new OnTroopQuit();

    public OnTroopQuit() {}

    @Override
    public void loadHook() {
        try {
            Class<?> serviceImplClass = Class.forName("com.tencent.mobileqq.troop.api.impl.TroopMemberInfoServiceImpl");
            java.lang.reflect.Method deleteMethod = serviceImplClass.getDeclaredMethod(
                    "deleteTroopMember",
                    String.class,
                    String.class,
                    boolean.class
            );
            if (deleteMethod != null) {
                HookUtils.hookAfter(deleteMethod, param -> {
                    try {
                        String troopUin = (String) param.args[0];
                        String memberUin = (String) param.args[1];
                        boolean updateHeadAndName = (Boolean) param.args[2];
                        // LogUtils.d("OnTroopQuit", "troopUin: " + troopUin + ", memberUin: " + memberUin + ", updateHeadAndName: " + updateHeadAndName);
                        QuitData data = new QuitData(troopUin, memberUin, updateHeadAndName);
                        LogUtils.i("OnTroopQuit", "quit data: " + data.toString());
                        notifyListeners(data);
                    } catch (Throwable e) {
                        LogUtils.e("OnTroopQuit", "callback error: " + e.getMessage());
                        LogUtils.e(e);
                    }
                });
            } else {
                LogUtils.e("OnTroopQuit", "deleteTroopMember method not found");
            }
        } catch (ClassNotFoundException e) {
            LogUtils.e("OnTroopQuit", "TroopMemberInfoServiceImpl not found");
            LogUtils.e(e);
        } catch (NoSuchMethodException e) {
            LogUtils.e("OnTroopQuit", "deleteTroopMember method not found");
            LogUtils.e(e);
        }
    }

    private void notifyListeners(QuitData data) {
        forEachChecked(listener -> listener.onQuit(data));
    }

    public interface TroopQuitListener extends Listener {
        void onQuit(QuitData data);
    }

    public static void registerListener(TroopQuitListener listener) {
        INSTANCE.addListener(listener);
    }

    public static void unregisterListener(TroopQuitListener listener) {
        INSTANCE.removeListener(listener);
    }
}
