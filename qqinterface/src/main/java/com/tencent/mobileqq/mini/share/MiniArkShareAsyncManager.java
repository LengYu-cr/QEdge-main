package com.tencent.mobileqq.mini.share;

import android.os.Bundle;
import android.os.RemoteException;
import android.text.TextUtils;
import com.tencent.mobileqq.mini.api.IMiniCallback;

public class MiniArkShareAsyncManager {
    private static final String TAG = "MiniArkShareAsyncManage [miniappArkShare]";

    private MiniArkShareAsyncManager() {
    }

    public static void performUploadArkShareImage(String str, IMiniCallback iMiniCallback) {
        if (iMiniCallback != null) {
            try {
                iMiniCallback.onCallbackResult(false, new Bundle());
            } catch (Throwable th) {
                th.printStackTrace();
            }
        }
    }

    public static void performUploadQzoneMiddlePageScreenshotImage(String str, IMiniCallback iMiniCallback) {
        if (iMiniCallback != null) {
            try {
                iMiniCallback.onCallbackResult(false, new Bundle());
            } catch (Throwable th) {
                th.printStackTrace();
            }
        }
    }
}
