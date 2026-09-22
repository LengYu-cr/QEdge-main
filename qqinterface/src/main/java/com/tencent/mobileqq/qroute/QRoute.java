package com.tencent.mobileqq.qroute;

import androidx.annotation.NonNull;

public class QRoute {

    @NonNull
    public static <T> T api(Class<T> cls) {
        try {
            return (T) cls.newInstance();
        } catch (Throwable e) {
            e.printStackTrace();
            throw new RuntimeException("Stub!");
        }
    }

    public static <T extends QRouteApi> T apiFromPlugin(Class<T> cls) {
        throw new RuntimeException("Stub!");
    }

    public static <T extends QRouteApi> T apiIPCSync(Class<T> cls) {
        throw new RuntimeException("Stub!");
    }
}
