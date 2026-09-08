//
// Decompiled by Jadx - 590ms
//
package com.tencent.mobileqq.mini.helper;

import android.text.TextUtils;
import com.tencent.common.app.BaseApplicationImpl;
import com.tencent.mobileqq.mini.config.ad.MiniAdConfigBean;
import com.tencent.mobileqq.qmmkv.QMMKV;
import com.tencent.mobileqq.qroute.QRoute;
import com.tencent.mobileqq.unitedconfig_android.api.IUnitedConfigManager;
import com.tencent.qphone.base.util.QLog;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import mqq.app.AppRuntime;
import mqq.app.MobileQQ;

public class MiniAdExposureHelper {
    private static final String EXPO_TIMESTAMP_LIST_SPLIT_FLAG = ",";
    private static final String KEY_EXPO_TIMESTAMP_LIST = "mini_loading_ad_expo_timestamp_list_";
    public static final String TAG = "MiniAdExposureHelper";
    private static int sMaxCacheExpoTimeStampNum;

    public static boolean checkAdExpoFreqAvailable() {
        MiniAdConfigBean loadConfig = QRoute.api(IUnitedConfigManager.class).loadConfig("100226");
        if (loadConfig == null) {
            QLog.d(TAG, 1, "checkAdExpoFreqAvailable: config is null -> expo freq access");
            return true;
        }
        long miniLoadingAdCheckDuration1 = loadConfig.getMiniLoadingAdCheckDuration1();
        long miniLoadingAdCheckDuration2 = loadConfig.getMiniLoadingAdCheckDuration2();
        int miniLoadingAdCheckTime1 = loadConfig.getMiniLoadingAdCheckTime1();
        int miniLoadingAdCheckTime2 = loadConfig.getMiniLoadingAdCheckTime2();
        sMaxCacheExpoTimeStampNum = Math.max(miniLoadingAdCheckTime1, miniLoadingAdCheckTime2);
        AppRuntime peekAppRuntime = BaseApplicationImpl.getApplication().peekAppRuntime();
        List<Long> recentExpoTimeStampList = getRecentExpoTimeStampList(peekAppRuntime != null ? peekAppRuntime.getAccount() : "");
        return checkAdExpoFreqAvailable(miniLoadingAdCheckDuration1, miniLoadingAdCheckTime1, recentExpoTimeStampList) && checkAdExpoFreqAvailable(miniLoadingAdCheckDuration2, miniLoadingAdCheckTime2, recentExpoTimeStampList);
    }

    private static List<Long> getRecentExpoTimeStampList(String str) {
        String decodeString = QMMKV.from(MobileQQ.sMobileQQ, "vas_mmkv_configurations").decodeString(KEY_EXPO_TIMESTAMP_LIST + str, "");
        ArrayList arrayList = new ArrayList();
        for (String str2 : decodeString.split(EXPO_TIMESTAMP_LIST_SPLIT_FLAG)) {
            if (!TextUtils.isEmpty(str2)) {
                try {
                    arrayList.add(Long.valueOf(Long.parseLong(str2)));
                } catch (Throwable th) {
                    QLog.e(TAG, 1, "getRecentExpoTimeStampList error:", th);
                }
            }
        }
        return arrayList;
    }

    public static void updateRecentExpoTimeStampList() {
        if (sMaxCacheExpoTimeStampNum == 0) {
            QLog.d(TAG, 1, "updateRecentExpoTimeStampList return: cache num is 0");
            return;
        }
        AppRuntime peekAppRuntime = BaseApplicationImpl.getApplication().peekAppRuntime();
        String account = peekAppRuntime != null ? peekAppRuntime.getAccount() : "";
        List<Long> recentExpoTimeStampList = getRecentExpoTimeStampList(account);
        recentExpoTimeStampList.add(Long.valueOf(System.currentTimeMillis()));
        int size = recentExpoTimeStampList.size();
        int i = sMaxCacheExpoTimeStampNum;
        if (size > i) {
            recentExpoTimeStampList = recentExpoTimeStampList.subList(size - i, size);
        }
        StringBuilder sb = new StringBuilder();
        Iterator<Long> it = recentExpoTimeStampList.iterator();
        while (it.hasNext()) {
            sb.append(it.next().longValue());
            sb.append(EXPO_TIMESTAMP_LIST_SPLIT_FLAG);
        }
        QMMKV.from(MobileQQ.sMobileQQ, "vas_mmkv_configurations").encodeString(KEY_EXPO_TIMESTAMP_LIST + account, sb.toString());
        QLog.d(TAG, 1, "updateRecentExpoTimeStampList: " + sb.toString());
    }

    private static boolean checkAdExpoFreqAvailable(long j, int i, List<Long> list) {
        if (j != 0 && i != 0) {
            long currentTimeMillis = System.currentTimeMillis();
            int size = list.size();
            if (size < i) {
                QLog.d(TAG, 1, "checkAdExpoFreqAvailable: checkDuration: " + j + ",checkTime: " + i + ", cacheSize:" + size + " -> expo freq access");
                return true;
            }
            try {
                Iterator<Long> it = list.subList(size - i, size).iterator();
                while (it.hasNext()) {
                    if (currentTimeMillis - it.next().longValue() > j) {
                        QLog.d(TAG, 1, "checkAdExpoFreqAvailable: checkDuration: " + j + ",checkTime: " + i + ", cacheSize:" + size + " (curTimeStamp - timeStamp > checkDuration) -> expo freq access");
                        return true;
                    }
                }
                QLog.e(TAG, 1, "checkAdExpoFreqAvailable: checkDuration: " + j + ",checkTime: " + i + " -> expo freq limit");
                return false;
            } catch (Exception e) {
                QLog.e(TAG, 1, "checkAdExpoFreqAvailable: failed " + e);
                return false;
            }
        }
        QLog.d(TAG, 1, "checkAdExpoFreqAvailable: checkDuration: " + j + ",checkTime: " + i + "-> expo freq access");
        return true;
    }
}
