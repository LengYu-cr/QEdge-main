package me.lengyu.qedge.hook.base;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * @Author 冷雨
 * @Description 基础API钩子项
 */
public abstract class BaseApiHookItem<T extends Listener> extends BaseHookItem {

    /**
     * 消息热路径：hook 回调线程遍历，Hook 加载线程注册监听。
     * 必须用 CopyOnWriteArraySet——裸 HashSet 在多线程遍历/修改时会抛 ConcurrentModificationException
     * 并中断当次消息处理；同时它也避免了两个线程同时懒初始化导致监听器丢失。
     * 遍历侧无锁且不额外分配，写入只发生在加载与极少解除时。
     */
    private final Set<T> listenerSet = new CopyOnWriteArraySet<>();
    private boolean hookLoaded = false;

    public boolean isHookLoaded() {
        return hookLoaded;
    }

    public void setHookLoaded(boolean loaded) {
        hookLoaded = loaded;
    }

    protected Set<T> getListenerSet() {
        return listenerSet;
    }

    public abstract void loadHook();

    public final void addListener(T listener) {
        getListenerSet().add(listener);
    }

    public final void removeListener(T listener) {
        getListenerSet().remove(listener);
    }

    protected void forEachChecked(java.util.function.Consumer<T> action) {
        for (T listener : getListenerSet()) {
            if (!(listener instanceof BaseSwitchHookItem) || ((BaseSwitchHookItem) listener).isEnable()) {
                action.accept(listener);
            }
        }
    }
}
