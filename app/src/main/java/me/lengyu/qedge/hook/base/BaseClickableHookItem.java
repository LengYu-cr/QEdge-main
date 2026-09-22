package me.lengyu.qedge.hook.base;

/**
 * @Author 冷雨
 * @Description 基础可点击钩子项
 */
public abstract class BaseClickableHookItem<T> extends BaseSwitchHookItem {

    public abstract void ConfigContent(Runnable onDismiss);
}