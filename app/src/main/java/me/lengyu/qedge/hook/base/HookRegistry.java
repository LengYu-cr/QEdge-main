package me.lengyu.qedge.hook.base;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HookRegistry {

    private static final List<BaseHookItem> hookItems = new ArrayList<>();
    private static final Set<Class<? extends BaseHookItem>> registeredClasses = new HashSet<>();

    public static void register(BaseHookItem item) {
        if (item == null) return;
        Class<? extends BaseHookItem> cls = item.getClass();
        if (registeredClasses.contains(cls)) {
            return;
        }
        registeredClasses.add(cls);
        hookItems.add(item);
    }

    public static List<BaseHookItem> getHookItems() {
        return new ArrayList<>(hookItems);
    }

    public static <T extends BaseHookItem> List<T> getHookItemsByClass(Class<T> clazz) {
        List<T> result = new ArrayList<>();
        for (BaseHookItem item : hookItems) {
            if (clazz.isInstance(item)) {
                result.add(clazz.cast(item));
            }
        }
        return result;
    }
}