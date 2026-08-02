package me.lengyu.qedge.utils.hook;

/**
 * Hook status detection, NO KOTLIN, NO ANDROIDX!
 */
public class HookStatusImpl {

    /**
     * To be changed by the hook
     */
    public static volatile boolean sZygoteHookMode = false;
    /**
     * To be changed by the hook
     */
    public static volatile String sZygoteHookProvider = null;
    /**
     * To be changed by the hook
     */
    public static volatile boolean sIsLsposedDexObfsEnabled = false;

    private HookStatusImpl() {
    }

}