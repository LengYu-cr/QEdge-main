//
// Decompiled by Jadx - 551ms
//
package com.tencent.mobileqq.mini.api.impl;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import com.tencent.component.network.downloader.Downloader;
import com.tencent.mobileqq.mini.ad.MiniLoadingAdCommonManager;
import com.tencent.mobileqq.mini.api.IMiniLoadingAdApi;
import com.tencent.mobileqq.mini.api.data.IMiniLoadingAdListener;
import com.tencent.mobileqq.mini.manager.MiniLoadingAdReportHelper;
import com.tencent.mobileqq.mini.reuse.MiniappDownloadUtil;
import com.tencent.qqmini.sdk.launcher.core.proxy.AdProxy;
import java.util.ArrayList;
import java.util.HashMap;
import org.json.JSONObject;

public class MiniLoadingAdApiImpl implements IMiniLoadingAdApi {
    public void downloadAd(String str, String str2, boolean z, Downloader.DownloadListener downloadListener, Downloader.DownloadMode downloadMode, JSONObject jSONObject) {
        MiniappDownloadUtil.getInstance().download(str, str2, z, downloadListener, downloadMode, jSONObject);
    }

    public void reportEvent(String str, Bundle bundle, String str2) {
        MiniLoadingAdReportHelper.INSTANCE.reportEvent(str, bundle, "", str2);
    }

    public void updateLoadingAdLayoutAndShow(Activity activity, int i, String str, String str2, String str3, String str4, String str5, String str6, long j, final IMiniLoadingAdListener iMiniLoadingAdListener) {
        if (iMiniLoadingAdListener == null) {
            return;
        }
        MiniLoadingAdCommonManager.INSTANCE.updateLoadingAdLayoutAndShow(activity, i, str, str2, str3, str4, str5, str6, j, new AdProxy.ILoadingAdListener() {
            public void getLoadingAdLayoutReady() {
                iMiniLoadingAdListener.getLoadingAdLayoutReady();
            }

            public void onAdClick(int i2) {
                iMiniLoadingAdListener.onAdClick(i2);
            }

            public void onAdDismiss(boolean z) {
                iMiniLoadingAdListener.onAdDismiss(z);
            }

            public void onAdShow(View view) {
                iMiniLoadingAdListener.onAdShow(view);
            }

            public void onDownloadAdEnd(String str7, long j2, String str8) {
                iMiniLoadingAdListener.onDownloadAdEnd(str7, j2, str8);
            }

            public void onPreloadAdReceive(int i2) {
                iMiniLoadingAdListener.onPreloadAdReceive(i2);
            }

            public void onSelectAdProcessDone(String str7, ArrayList<Long> arrayList, HashMap<String, String> hashMap) {
                iMiniLoadingAdListener.onSelectAdProcessDone(str7, arrayList, hashMap);
            }
        });
    }
}
