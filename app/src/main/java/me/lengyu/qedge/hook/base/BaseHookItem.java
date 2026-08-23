package me.lengyu.qedge.hook.base;

import me.lengyu.qedge.hook.annotation.HookItemAnnotation;
import me.lengyu.qedge.utils.HostInfo;
/**
 * @Author 冷雨
 * @Description 基础钩子项
 */
public abstract class BaseHookItem {

    protected boolean isEnable = true;

    protected HookItemAnnotation getAnnotation() {
        return this.getClass().getAnnotation(HookItemAnnotation.class);
    }

    public boolean isInTargetProcess() {
        HookItemAnnotation annotation = getAnnotation();
        if (annotation == null) return false;
        String target = annotation.process();
        if (target == null || target.isEmpty() || "All".equals(target)) return true;
        String currentProcess = HostInfo.processName;
        return currentProcess.equals(HostInfo.packageName + target);
    }

    public boolean isEnable() {
        return isEnable;
    }

    public void setEnable(boolean enable) {
        isEnable = enable;
    }
}