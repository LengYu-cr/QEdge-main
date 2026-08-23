package me.lengyu.qedge.hook.base;

import me.lengyu.qedge.hook.annotation.HookCategory;
import me.lengyu.qedge.hook.annotation.HookItemAnnotation;
import me.lengyu.qedge.utils.ModuleConfig;
/**
 * @Author 冷雨
 * @Description 基础开关钩子项
 */
public abstract class BaseSwitchHookItem extends BaseHookItem {

    protected boolean isAvailable = false;
    private boolean initialized = false;

    public String getTag() {
        HookItemAnnotation annotation = getAnnotation();
        return annotation != null ? annotation.tag() : "Unknown";
    }

    public String getDesc() {
        HookItemAnnotation annotation = getAnnotation();
        return annotation != null ? annotation.desc() : "";
    }

    public String getCategory() {
        HookItemAnnotation annotation = getAnnotation();
        return annotation != null ? annotation.category() : HookCategory.OTHER;
    }

    public boolean isAvailable() {
        return isAvailable;
    }

    public void init() {
        if (initialized) return;
        initialized = true;
        try {
            isAvailable = onInit();
        } catch (Throwable t) {
            isAvailable = false;
        }
        try {
            if (isAvailable && isInTargetProcess()) {
                if (this instanceof BaseClickableHookItem) {
                    try {
                        ((BaseClickableHookItem<?>) this).initData();
                    } catch (Throwable ignored) {
                    }
                }
                try {
                    onHook();
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable t) {
            isAvailable = false;
        }
    }

    protected boolean onInit() {
        return true;
    }

    protected void onHook() {
    }

    protected void initData() {
    }

    protected void saveData() {
    }

    protected boolean getBoolean(String key, boolean defaultValue) {
        return ModuleConfig.getBoolean(key, defaultValue);
    }

    protected void putBoolean(String key, boolean value) {
        ModuleConfig.putBoolean(key, value);
    }
}