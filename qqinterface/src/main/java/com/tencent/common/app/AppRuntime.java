package com.tencent.common.app;


public class AppRuntime {

    public AppRuntime() {
    }

    public static AppRuntime getRuntime() {
        return new AppRuntime();
    }

    public String getCurrentAccountUin() {
        return "10000";
    }

}
