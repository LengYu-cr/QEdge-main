package me.lengyu.qedge.hook.api;

import me.lengyu.qedge.common.ModuleScope;
import me.lengyu.qedge.hook.annotation.HookItemAnnotation;
import me.lengyu.qedge.hook.base.BaseApiHookItem;
import me.lengyu.qedge.hook.base.Listener;
import me.lengyu.qedge.utils.HookUtils;
import me.lengyu.qedge.utils.LogUtils;
import me.lengyu.qedge.utils.ReflectUtils;
import me.lengyu.qedge.plugin.bean.JoinData;
import me.lengyu.qedge.utils.qq.FriendTool;

/**
 * @Author 冷雨
 * @Description 监听群成员加入
 */
@HookItemAnnotation(value = "监听群成员加入", category = "api")
public class OnTroopJoin extends BaseApiHookItem<OnTroopJoin.TroopJoinListener> {

    public static final OnTroopJoin INSTANCE = new OnTroopJoin();

    public OnTroopJoin() {}

    @Override
    public void loadHook() {
        try {
                hookMSFServlet();
        } catch (Throwable e) {
            LogUtils.e("OnTroopJoin", "loadHook error: " + e.getMessage());
            LogUtils.e(e);
        }
    }

    /** 订阅 MSFServlet 拦截到的进群事件，异步完成 uid->uin 转换 */
    private void hookMSFServlet() {
        FromServiceMsgDispatcher.loadHook();
        FromServiceMsgDispatcher.registerJoinListener((troopUin, memberUid, adminUid, joinType) -> {
            handleJoin(troopUin, memberUid, adminUid, joinType);
        });
    }

    private void handleJoin(String troopUin, String memberUid, String adminUid, int joinType) {
        ModuleScope.launchIOJava("OnTroopJoin", () -> {
            try {
                String memberUin = (memberUid == null || memberUid.isEmpty()) ? "" : FriendTool.getUinFromUid(memberUid);
                if (memberUin.isEmpty()) {
                    // uid->uin 可能未就绪，重试几次
                    for (int i = 0; i < 3; i++) {
                        Thread.sleep(100);
                        memberUin = FriendTool.getUinFromUid(memberUid);
                        if (!memberUin.isEmpty()) break;
                    }
                }
                if (memberUin.isEmpty()) {
                    LogUtils.e("OnTroopJoin", "memberUin resolve failed: " + memberUid);
                    return;
                }
                String adminUin = (adminUid == null || adminUid.isEmpty()) ? null : FriendTool.getUinFromUid(adminUid);
                JoinData data = new JoinData(troopUin, memberUin, joinType, memberUid, adminUin, adminUid);
                // LogUtils.i("OnTroopJoin", "join data: " + data.toString());
                notifyListeners(data);
            } catch (Throwable e) {
                LogUtils.e("OnTroopJoin", "handleJoin error: " + e.getMessage());
                LogUtils.e(e);
            }
        });
    }

    public interface TroopJoinListener extends Listener {
        void onJoin(JoinData data);
    }

    public static void registerListener(TroopJoinListener listener) {
        INSTANCE.addListener(listener);
    }

    public static void unregisterListener(TroopJoinListener listener) {
        INSTANCE.removeListener(listener);
    }

    private void notifyListeners(JoinData data) {
        forEachChecked(listener -> listener.onJoin(data));
    }
}
