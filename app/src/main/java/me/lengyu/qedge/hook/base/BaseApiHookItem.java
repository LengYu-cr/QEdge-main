package me.lengyu.qedge.hook.base;

import java.util.HashSet;
import java.util.Set;

public abstract class BaseApiHookItem<T extends Listener> extends BaseHookItem {

    private Set<T> listenerSet;

    protected Set<T> getListenerSet() {
        if (listenerSet == null) {
            listenerSet = new HashSet<>();
        }
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
