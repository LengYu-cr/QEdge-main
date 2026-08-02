package me.lengyu.qedge.hook.kugou;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Handler;
import android.os.Looper;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import me.lengyu.qedge.utils.ReflectUtils;
import me.lengyu.qedge.utils.LogUtils;
import me.lengyu.qedge.utils.HookUtils;
import me.lengyu.qedge.utils.HostInfo;
import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;

public class KuGouHook {

    private static boolean initialized = false;

    private static final byte[] KUGOU_ORIGINAL_SIGNATURE = {
        (byte)0x30, (byte)0x82, (byte)0x03, (byte)0xD4, (byte)0x06, (byte)0x09, (byte)0x2A, (byte)0x86,
        (byte)0x48, (byte)0x86, (byte)0xF7, (byte)0x0D, (byte)0x01, (byte)0x07, (byte)0x02, (byte)0xA0,
        (byte)0x82, (byte)0x03, (byte)0xC5, (byte)0x30, (byte)0x82, (byte)0x03, (byte)0xC1, (byte)0x02,
        (byte)0x01, (byte)0x01, (byte)0x31, (byte)0x0F, (byte)0x30, (byte)0x0D, (byte)0x06, (byte)0x09,
        (byte)0x60, (byte)0x86, (byte)0x48, (byte)0x01, (byte)0x65, (byte)0x03, (byte)0x04, (byte)0x02,
        (byte)0x01, (byte)0x05, (byte)0x00, (byte)0x30, (byte)0x0B, (byte)0x06, (byte)0x09, (byte)0x2A,
        (byte)0x86, (byte)0x48, (byte)0x86, (byte)0xF7, (byte)0x0D, (byte)0x01, (byte)0x07, (byte)0x01,
        (byte)0xA0, (byte)0x82, (byte)0x02, (byte)0x6D, (byte)0x30, (byte)0x82, (byte)0x02, (byte)0x69,
        (byte)0x30, (byte)0x82, (byte)0x01, (byte)0xD2, (byte)0xA0, (byte)0x03, (byte)0x02, (byte)0x01,
        (byte)0x02, (byte)0x02, (byte)0x04, (byte)0x4D, (byte)0x89, (byte)0xA1, (byte)0x09, (byte)0x30,
        (byte)0x0D, (byte)0x06, (byte)0x09, (byte)0x2A, (byte)0x86, (byte)0x48, (byte)0x86, (byte)0xF7,
        (byte)0x0D, (byte)0x01, (byte)0x01, (byte)0x05, (byte)0x05, (byte)0x00, (byte)0x30, (byte)0x78,
        (byte)0x31, (byte)0x0E, (byte)0x30, (byte)0x0C, (byte)0x06, (byte)0x03, (byte)0x55, (byte)0x04,
        (byte)0x06, (byte)0x13, (byte)0x05, (byte)0x43, (byte)0x68, (byte)0x69, (byte)0x6E, (byte)0x61,
        (byte)0x31, (byte)0x12, (byte)0x30, (byte)0x10, (byte)0x06, (byte)0x03, (byte)0x55, (byte)0x04,
        (byte)0x08, (byte)0x13, (byte)0x09, (byte)0x47, (byte)0x75, (byte)0x61, (byte)0x6E, (byte)0x67,
        (byte)0x44, (byte)0x6F, (byte)0x6E, (byte)0x67, (byte)0x31, (byte)0x12, (byte)0x30, (byte)0x10,
        (byte)0x06, (byte)0x03, (byte)0x55, (byte)0x04, (byte)0x07, (byte)0x13, (byte)0x09, (byte)0x47,
        (byte)0x75, (byte)0x61, (byte)0x6E, (byte)0x67, (byte)0x5A, (byte)0x68, (byte)0x6F, (byte)0x75,
        (byte)0x31, (byte)0x0E, (byte)0x30, (byte)0x0C, (byte)0x06, (byte)0x03, (byte)0x55, (byte)0x04,
        (byte)0x0A, (byte)0x13, (byte)0x05, (byte)0x4B, (byte)0x75, (byte)0x47, (byte)0x6F, (byte)0x75,
        (byte)0x31, (byte)0x17, (byte)0x30, (byte)0x15, (byte)0x06, (byte)0x03, (byte)0x55, (byte)0x04,
        (byte)0x0B, (byte)0x13, (byte)0x0E, (byte)0x4B, (byte)0x75, (byte)0x67, (byte)0x6F, (byte)0x75,
        (byte)0x20, (byte)0x4E, (byte)0x65, (byte)0x74, (byte)0x77, (byte)0x6F, (byte)0x72, (byte)0x6B,
        (byte)0x73, (byte)0x31, (byte)0x15, (byte)0x30, (byte)0x13, (byte)0x06, (byte)0x03, (byte)0x55,
        (byte)0x04, (byte)0x03, (byte)0x13, (byte)0x0C, (byte)0x4D, (byte)0x6F, (byte)0x62, (byte)0x69,
        (byte)0x6C, (byte)0x65, (byte)0x20, (byte)0x4B, (byte)0x75, (byte)0x67, (byte)0x6F, (byte)0x75,
        (byte)0x30, (byte)0x20, (byte)0x17, (byte)0x0D, (byte)0x31, (byte)0x31, (byte)0x30, (byte)0x33,
        (byte)0x32, (byte)0x33, (byte)0x30, (byte)0x37, (byte)0x32, (byte)0x38, (byte)0x30, (byte)0x39,
        (byte)0x5A, (byte)0x18, (byte)0x0F, (byte)0x32, (byte)0x31, (byte)0x31, (byte)0x31, (byte)0x30,
        (byte)0x32, (byte)0x32, (byte)0x37, (byte)0x30, (byte)0x37, (byte)0x32, (byte)0x38, (byte)0x30,
        (byte)0x39, (byte)0x5A, (byte)0x30, (byte)0x78, (byte)0x31, (byte)0x0E, (byte)0x30, (byte)0x0C,
        (byte)0x06, (byte)0x03, (byte)0x55, (byte)0x04, (byte)0x06, (byte)0x13, (byte)0x05, (byte)0x43,
        (byte)0x68, (byte)0x69, (byte)0x6E, (byte)0x61, (byte)0x31, (byte)0x12, (byte)0x30, (byte)0x10,
        (byte)0x06, (byte)0x03, (byte)0x55, (byte)0x04, (byte)0x08, (byte)0x13, (byte)0x09, (byte)0x47,
        (byte)0x75, (byte)0x61, (byte)0x6E, (byte)0x67, (byte)0x44, (byte)0x6F, (byte)0x6E, (byte)0x67,
        (byte)0x31, (byte)0x12, (byte)0x30, (byte)0x10, (byte)0x06, (byte)0x03, (byte)0x55, (byte)0x04,
        (byte)0x07, (byte)0x13, (byte)0x09, (byte)0x47, (byte)0x75, (byte)0x61, (byte)0x6E, (byte)0x67,
        (byte)0x5A, (byte)0x68, (byte)0x6F, (byte)0x75, (byte)0x31, (byte)0x0E, (byte)0x30, (byte)0x0C,
        (byte)0x06, (byte)0x03, (byte)0x55, (byte)0x04, (byte)0x0A, (byte)0x13, (byte)0x05, (byte)0x4B,
        (byte)0x75, (byte)0x47, (byte)0x6F, (byte)0x75, (byte)0x31, (byte)0x17, (byte)0x30, (byte)0x15,
        (byte)0x06, (byte)0x03, (byte)0x55, (byte)0x04, (byte)0x0B, (byte)0x13, (byte)0x0E, (byte)0x4B,
        (byte)0x75, (byte)0x67, (byte)0x6F, (byte)0x75, (byte)0x20, (byte)0x4E, (byte)0x65, (byte)0x74,
        (byte)0x77, (byte)0x6F, (byte)0x72, (byte)0x6B, (byte)0x73, (byte)0x31, (byte)0x15, (byte)0x30,
        (byte)0x13, (byte)0x06, (byte)0x03, (byte)0x55, (byte)0x04, (byte)0x03, (byte)0x13, (byte)0x0C,
        (byte)0x4D, (byte)0x6F, (byte)0x62, (byte)0x69, (byte)0x6C, (byte)0x65, (byte)0x20, (byte)0x4B,
        (byte)0x75, (byte)0x67, (byte)0x6F, (byte)0x75, (byte)0x30, (byte)0x81, (byte)0x9F, (byte)0x30,
        (byte)0x0D, (byte)0x06, (byte)0x09, (byte)0x2A, (byte)0x86, (byte)0x48, (byte)0x86, (byte)0xF7,
        (byte)0x0D, (byte)0x01, (byte)0x01, (byte)0x01, (byte)0x05, (byte)0x00, (byte)0x03, (byte)0x81,
        (byte)0x8D, (byte)0x00, (byte)0x30, (byte)0x81, (byte)0x89, (byte)0x02, (byte)0x81, (byte)0x81,
        (byte)0x00, (byte)0xCA, (byte)0xB2, (byte)0x7D, (byte)0x77, (byte)0x62, (byte)0x81, (byte)0x96,
        (byte)0xE0, (byte)0xF1, (byte)0x62, (byte)0x01, (byte)0xA8, (byte)0x25, (byte)0x54, (byte)0xDC,
        (byte)0x54, (byte)0x9D, (byte)0xF5, (byte)0xB7, (byte)0x1F, (byte)0x0B, (byte)0x5C, (byte)0x18,
        (byte)0x6C, (byte)0x2E, (byte)0x57, (byte)0x8F, (byte)0xAE, (byte)0xBD, (byte)0x13, (byte)0x72,
        (byte)0x46, (byte)0x43, (byte)0x95, (byte)0x37, (byte)0x7E, (byte)0x0C, (byte)0xEB, (byte)0x8E,
        (byte)0x59, (byte)0x4E, (byte)0xE8, (byte)0xDC, (byte)0xF6, (byte)0x9C, (byte)0x84, (byte)0x30,
        (byte)0xB1, (byte)0x92, (byte)0x61, (byte)0x90, (byte)0x9C, (byte)0x5F, (byte)0x5A, (byte)0x13,
        (byte)0x8E, (byte)0x65, (byte)0x59, (byte)0xFA, (byte)0xFC, (byte)0xFE, (byte)0x5D, (byte)0x40,
        (byte)0xE0, (byte)0x7F, (byte)0x68, (byte)0x72, (byte)0xBD, (byte)0x9B, (byte)0x33, (byte)0xF2,
        (byte)0x77, (byte)0xA9, (byte)0xFF, (byte)0xF7, (byte)0xD9, (byte)0xC5, (byte)0x6E, (byte)0xE3,
        (byte)0x71, (byte)0x55, (byte)0xB4, (byte)0xDC, (byte)0x84, (byte)0x93, (byte)0xCD, (byte)0xBF,
        (byte)0x7D, (byte)0x41, (byte)0x5D, (byte)0xD8, (byte)0xCF, (byte)0xB6, (byte)0xF9, (byte)0x7D,
        (byte)0xE9, (byte)0xD1, (byte)0x5A, (byte)0x1D, (byte)0x63, (byte)0x90, (byte)0xBF, (byte)0x37,
        (byte)0x14, (byte)0x00, (byte)0x7A, (byte)0x11, (byte)0xD0, (byte)0x18, (byte)0x74, (byte)0xE0,
        (byte)0x33, (byte)0xA4, (byte)0xFE, (byte)0x1C, (byte)0x86, (byte)0x61, (byte)0x71, (byte)0x40,
        (byte)0x29, (byte)0x40, (byte)0xF6, (byte)0x79, (byte)0xCE, (byte)0xBE, (byte)0xDA, (byte)0x79,
        (byte)0x4F, (byte)0x02, (byte)0x03, (byte)0x01, (byte)0x00, (byte)0x01, (byte)0x30, (byte)0x0D,
        (byte)0x06, (byte)0x09, (byte)0x2A, (byte)0x86, (byte)0x48, (byte)0x86, (byte)0xF7, (byte)0x0D,
        (byte)0x01, (byte)0x01, (byte)0x05, (byte)0x05, (byte)0x00, (byte)0x03, (byte)0x81, (byte)0x81,
        (byte)0x00, (byte)0x84, (byte)0x00, (byte)0x70, (byte)0xED, (byte)0xDF, (byte)0x43, (byte)0x27,
        (byte)0x97, (byte)0x2F, (byte)0xB7, (byte)0x68, (byte)0x89, (byte)0x28, (byte)0xCB, (byte)0x44,
        (byte)0xD9, (byte)0xBF, (byte)0xBD, (byte)0xB2, (byte)0x74, (byte)0x75, (byte)0xCC, (byte)0xAA,
        (byte)0xE1, (byte)0x6F, (byte)0xF2, (byte)0x34, (byte)0x59, (byte)0x24, (byte)0x41, (byte)0x9E,
        (byte)0x9E, (byte)0x6B, (byte)0xD2, (byte)0x9E, (byte)0xB8, (byte)0x7B, (byte)0x4F, (byte)0x09,
        (byte)0xF6, (byte)0xED, (byte)0xF8, (byte)0xB2, (byte)0xF2, (byte)0x81, (byte)0x54, (byte)0xDE,
        (byte)0x6E, (byte)0x63, (byte)0x7E, (byte)0xE8, (byte)0x8C, (byte)0x99, (byte)0x17, (byte)0xFF,
        (byte)0x06, (byte)0xB2, (byte)0x41, (byte)0x0D, (byte)0xAD, (byte)0x01, (byte)0x92, (byte)0xD1,
        (byte)0x0F, (byte)0x41, (byte)0x9F, (byte)0xCD, (byte)0x5E, (byte)0xD7, (byte)0xA7, (byte)0xA3,
        (byte)0x6B, (byte)0xE7, (byte)0x2F, (byte)0xD0, (byte)0x7C, (byte)0x57, (byte)0x3C, (byte)0x6F,
        (byte)0x50, (byte)0xFC, (byte)0x1D, (byte)0xD9, (byte)0x5E, (byte)0x86, (byte)0xD2, (byte)0xAE,
        (byte)0x0B, (byte)0x02, (byte)0x50, (byte)0x7A, (byte)0x08, (byte)0xFC, (byte)0xEE, (byte)0xE9,
        (byte)0xFF, (byte)0x2F, (byte)0x9E, (byte)0xE9, (byte)0x8F, (byte)0x12, (byte)0x3B, (byte)0x23,
        (byte)0x39, (byte)0x82, (byte)0x94, (byte)0x55, (byte)0xBB, (byte)0x0B, (byte)0x09, (byte)0x68,
        (byte)0x08, (byte)0xAC, (byte)0xBF, (byte)0x67, (byte)0x89, (byte)0xE3, (byte)0x0C, (byte)0xA6,
        (byte)0x14, (byte)0x25, (byte)0x42, (byte)0x9A, (byte)0xE9, (byte)0xBC, (byte)0x96, (byte)0x48,
        (byte)0x39, (byte)0x31, (byte)0x82, (byte)0x01, (byte)0x2B, (byte)0x30, (byte)0x82, (byte)0x01,
        (byte)0x27, (byte)0x02, (byte)0x01, (byte)0x01, (byte)0x30, (byte)0x81, (byte)0x80, (byte)0x30,
        (byte)0x78, (byte)0x31, (byte)0x0E, (byte)0x30, (byte)0x0C, (byte)0x06, (byte)0x03, (byte)0x55,
        (byte)0x04, (byte)0x06, (byte)0x13, (byte)0x05, (byte)0x43, (byte)0x68, (byte)0x69, (byte)0x6E,
        (byte)0x61, (byte)0x31, (byte)0x12, (byte)0x30, (byte)0x10, (byte)0x06, (byte)0x03, (byte)0x55,
        (byte)0x04, (byte)0x08, (byte)0x13, (byte)0x09, (byte)0x47, (byte)0x75, (byte)0x61, (byte)0x6E,
        (byte)0x67, (byte)0x44, (byte)0x6F, (byte)0x6E, (byte)0x67, (byte)0x31, (byte)0x12, (byte)0x30,
        (byte)0x10, (byte)0x06, (byte)0x03, (byte)0x55, (byte)0x04, (byte)0x07, (byte)0x13, (byte)0x09,
        (byte)0x47, (byte)0x75, (byte)0x61, (byte)0x6E, (byte)0x67, (byte)0x5A, (byte)0x68, (byte)0x6F,
        (byte)0x75, (byte)0x31, (byte)0x0E, (byte)0x30, (byte)0x0C, (byte)0x06, (byte)0x03, (byte)0x55,
        (byte)0x04, (byte)0x0A, (byte)0x13, (byte)0x05, (byte)0x4B, (byte)0x75, (byte)0x47, (byte)0x6F,
        (byte)0x75, (byte)0x31, (byte)0x17, (byte)0x30, (byte)0x15, (byte)0x06, (byte)0x03, (byte)0x55,
        (byte)0x04, (byte)0x0B, (byte)0x13, (byte)0x0E, (byte)0x4B, (byte)0x75, (byte)0x67, (byte)0x6F,
        (byte)0x75, (byte)0x20, (byte)0x4E, (byte)0x65, (byte)0x74, (byte)0x77, (byte)0x6F, (byte)0x72,
        (byte)0x6B, (byte)0x73, (byte)0x31, (byte)0x15, (byte)0x30, (byte)0x13, (byte)0x06, (byte)0x03,
        (byte)0x55, (byte)0x04, (byte)0x03, (byte)0x13, (byte)0x0C, (byte)0x4D, (byte)0x6F, (byte)0x62,
        (byte)0x69, (byte)0x6C, (byte)0x65, (byte)0x20, (byte)0x4B, (byte)0x75, (byte)0x67, (byte)0x6F,
        (byte)0x75, (byte)0x02, (byte)0x04, (byte)0x4D, (byte)0x89, (byte)0xA1, (byte)0x09, (byte)0x30,
        (byte)0x0D, (byte)0x06, (byte)0x09, (byte)0x60, (byte)0x86, (byte)0x48, (byte)0x01, (byte)0x65,
        (byte)0x03, (byte)0x04, (byte)0x02, (byte)0x01, (byte)0x05, (byte)0x00, (byte)0x30, (byte)0x0D,
        (byte)0x06, (byte)0x09, (byte)0x2A, (byte)0x86, (byte)0x48, (byte)0x86, (byte)0xF7, (byte)0x0D,
        (byte)0x01, (byte)0x01, (byte)0x01, (byte)0x05, (byte)0x00, (byte)0x04, (byte)0x81, (byte)0x80,
        (byte)0x2A, (byte)0x6D, (byte)0xE5, (byte)0xC4, (byte)0xE9, (byte)0x20, (byte)0x88, (byte)0x23,
        (byte)0x4B, (byte)0xEF, (byte)0xED, (byte)0xBB, (byte)0xBA, (byte)0x42, (byte)0x10, (byte)0x48,
        (byte)0x15, (byte)0xF9, (byte)0x80, (byte)0x80, (byte)0x3E, (byte)0x68, (byte)0x46, (byte)0x28,
        (byte)0xA1, (byte)0x77, (byte)0x8E, (byte)0xE1, (byte)0x7F, (byte)0x95, (byte)0x03, (byte)0x57,
        (byte)0x55, (byte)0x0E, (byte)0x9A, (byte)0x58, (byte)0xBE, (byte)0xE0, (byte)0x14, (byte)0x5E,
        (byte)0xC4, (byte)0x28, (byte)0x15, (byte)0xB5, (byte)0xDF, (byte)0x62, (byte)0x23, (byte)0xC8,
        (byte)0x2B, (byte)0x14, (byte)0xA4, (byte)0x26, (byte)0x7D, (byte)0xFB, (byte)0x97, (byte)0xB4,
        (byte)0x2D, (byte)0x46, (byte)0x44, (byte)0xFD, (byte)0xCF, (byte)0x91, (byte)0x4A, (byte)0xF4,
        (byte)0x22, (byte)0xFC, (byte)0x93, (byte)0x96, (byte)0x3A, (byte)0xFD, (byte)0xE2, (byte)0xA9,
        (byte)0x0D, (byte)0x7D, (byte)0x35, (byte)0xAB, (byte)0x2F, (byte)0x88, (byte)0x0C, (byte)0x79,
        (byte)0x2D, (byte)0x57, (byte)0xAF, (byte)0x19, (byte)0x01, (byte)0x5F, (byte)0xA2, (byte)0xDB,
        (byte)0x79, (byte)0x1C, (byte)0xE5, (byte)0x9E, (byte)0x05, (byte)0x7C, (byte)0xEE, (byte)0x5E,
        (byte)0xF6, (byte)0xCD, (byte)0x84, (byte)0xC2, (byte)0x90, (byte)0xD2, (byte)0x2A, (byte)0x92,
        (byte)0x08, (byte)0x94, (byte)0x05, (byte)0x1C, (byte)0x79, (byte)0xE9, (byte)0xD1, (byte)0xB8,
        (byte)0x1E, (byte)0xB6, (byte)0xE2, (byte)0x1D, (byte)0x8F, (byte)0x49, (byte)0x83, (byte)0xEF,
        (byte)0x6F, (byte)0x0F, (byte)0xFF, (byte)0x11, (byte)0x2C, (byte)0x0E, (byte)0xF3, (byte)0x78
    };

    public static void loadHook(String packageName) {
        if (initialized) return;
        initialized = true;

        try {
            String sourceDir = HostInfo.getHostContext().getApplicationInfo().sourceDir;
            if (sourceDir == null) {
                LogUtils.e("KuGouHook", "sourceDir is null");
                return;
            }

            System.loadLibrary("dexkit");
            DexKitBridge bridge = DexKitBridge.create(sourceDir);
            if (bridge == null) {
                LogUtils.e("KuGouHook", "DexKitBridge.create returned null");
                return;
            }

            try {
                if (packageName.equals("com.kugou.android.elder")) {
                    hookElder(bridge);
                } else if (packageName.equals("com.kugou.android.lite")) {
                    hookLite(bridge);
                }
                hookGdtSplashActivity();
                hookAdContainerActivity();
                hookAPSecuritySdk();
                hookPackageManagerSignatures();
                hookPackageManagerHacker();
                hookPackageInfoHookImpl();
                hookActivityStartActivity();
            } finally {
                bridge.close();
            }

        } catch (Throwable e) {
            LogUtils.e("KuGouHook", "loadHook error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void hookElder(DexKitBridge bridge) {
        try {
            FindMethod findMethod = new FindMethod();
            findMethod.matcher(new MethodMatcher()
                .usingStrings("splash gotoGdtSplashActivity")
            );

            List<MethodData> methods = bridge.findMethod(findMethod);
            if (methods.isEmpty()) {
                LogUtils.e("KuGouHook", "大字版: 未找到跳开屏广告的方法");
                return;
            }

            MethodData methodData = methods.get(0);

            Method gotoAdMethod = methodData.getMethodInstance(ReflectUtils.hostClassLoader);

            HookUtils.hookBefore(gotoAdMethod, param -> {
                try {
                    Activity act = (Activity) param.thisObject;
                    Intent intent = new Intent();
                    intent.setClassName(act, "com.kugou.android.app.MediaActivity");
                    act.startActivity(intent);
                    act.finish();
                    param.setResult(null);
                } catch (Throwable e) {
                    // LogUtils.e("KuGouHook", "大字版: gotoAd callback error: " + e.getMessage());
                }
            });

        } catch (Throwable e) {
            LogUtils.e("KuGouHook", "大字版: hookElder error: " + e.getMessage());
        }
    }

    private static void hookLite(DexKitBridge bridge) {
        try {
            FindMethod findMethod = new FindMethod();
            findMethod.matcher(new MethodMatcher()
                .usingStrings("BaseSplashActivity: goADContainerActivity")
            );

            List<MethodData> methods = bridge.findMethod(findMethod);
            if (methods.isEmpty()) {
                LogUtils.e("KuGouHook", "概念版: 未找到跳开屏广告的方法");
                return;
            }

            MethodData methodData = methods.get(0);

            Method gotoAdMethod = methodData.getMethodInstance(ReflectUtils.hostClassLoader);

            HookUtils.hookBefore(gotoAdMethod, param -> {
                try {
                    Activity act = (Activity) param.thisObject;
                    Intent intent = new Intent();
                    intent.setClassName(act, "com.kugou.android.app.MediaActivity");
                    act.startActivity(intent);
                    act.finish();
                    param.setResult(null);
                } catch (Throwable e) {
                    LogUtils.e("KuGouHook", "概念版: gotoAd callback error: " + e.getMessage());
                }
            });

        } catch (Throwable e) {
            LogUtils.e("KuGouHook", "概念版: hookLite error: " + e.getMessage());
        }
    }

    private static void hookGdtSplashActivity() {
        try {
            Class<?> gdtClass = ReflectUtils.hostClassLoader.loadClass("com.kugou.android.app.splash.GdtSplashActivity");

            HookUtils.hookAllMethods(gdtClass, "onResume", param -> {
                try {
                    Activity act = (Activity) param.thisObject;
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        try {
                            Intent intent = new Intent();
                            intent.setClassName(act, "com.kugou.android.app.MediaActivity");
                            act.startActivity(intent);
                            act.finish();
                        } catch (Throwable e) {
                            LogUtils.e("KuGouHook", "保险跳转失败: " + e.getMessage());
                        }
                    }, 10);
                } catch (Throwable e) {
                    LogUtils.e("KuGouHook", "hookGdtSplashActivity callback error: " + e.getMessage());
                }
            });

        } catch (ClassNotFoundException e) {
        } catch (Throwable e) {
            LogUtils.e("KuGouHook", "hookGdtSplashActivity error: " + e.getMessage());
        }
    }

    private static void hookAdContainerActivity() {
        try {
            Class<?> adClass = ReflectUtils.hostClassLoader.loadClass("com.kugou.android.app.splash.adcontainer.AdContainerActivity");

            HookUtils.hookAllMethods(adClass, "onResume", param -> {
                try {
                    Activity act = (Activity) param.thisObject;
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        try {
                            Intent intent = new Intent();
                            intent.setClassName(act, "com.kugou.android.app.MediaActivity");
                            act.startActivity(intent);
                            act.finish();
                        } catch (Throwable e) {
                            LogUtils.e("KuGouHook", "保险跳转失败: " + e.getMessage());
                        }
                    }, 10);
                } catch (Throwable e) {
                    LogUtils.e("KuGouHook", "hookAdContainerActivity callback error: " + e.getMessage());
                }
            });

        } catch (ClassNotFoundException e) {
        } catch (Throwable e) {
            LogUtils.e("KuGouHook", "hookAdContainerActivity error: " + e.getMessage());
        }
    }

    private static void hookAPSecuritySdk() {
        try {
            Class<?> apsClass = ReflectUtils.hostClassLoader.loadClass("com.alipay.apmobilesecuritysdk.face.APSecuritySdk");

            Method[] methods = apsClass.getDeclaredMethods();
            for (Method method : methods) {
                if (method.getName().equals("initToken")) {
                    HookUtils.hookBefore(method, param -> {
                        Object listener = null;
                        for (Object arg : param.args) {
                            if (arg != null && arg.getClass().getName().contains("InitResultListener")) {
                                listener = arg;
                                break;
                            }
                        }
                        if (listener != null) {
                            hookInitResultListener(listener.getClass());
                        }
                    });
                } else if (method.getName().equals("getTokenResult")) {
                    HookUtils.hookAfter(method, param -> {
                        Object tokenResult = param.getResult();
                        if (tokenResult != null) {
                            hookTokenResultGetters(tokenResult.getClass());
                        }
                    });
                }
            }

        } catch (ClassNotFoundException e) {
        } catch (Throwable e) {
            LogUtils.e("KuGouHook", "hookAPSecuritySdk error: " + e.getMessage());
        }
    }

    private static void hookInitResultListener(Class<?> listenerClass) {
        try {
            HookUtils.hookAllMethods(listenerClass, "onResult", param -> {
                Object tokenResult = param.args[0];
                if (tokenResult != null) {
                    hookTokenResultGetters(tokenResult.getClass());
                }
            });
        } catch (Throwable e) {
            LogUtils.e("KuGouHook", "hookInitResultListener error: " + e.getMessage());
        }
    }

    private static void hookTokenResultGetters(Class<?> tokenResultClass) {
        try {
            Method[] methods = tokenResultClass.getDeclaredMethods();
            for (Method method : methods) {
                String name = method.getName();
                if (name.startsWith("get")) {
                    HookUtils.hookAfter(method, param -> {
                        Class<?> returnType = method.getReturnType();
                        if (returnType == String.class) {
                            param.setResult("");
                        } else if (returnType == boolean.class || returnType == Boolean.class) {
                            param.setResult(true);
                        } else if (returnType == int.class || returnType == Integer.class) {
                            param.setResult(0);
                        } else if (returnType == long.class || returnType == Long.class) {
                            param.setResult(0L);
                        } else if (returnType == byte.class || returnType == Byte.class) {
                            param.setResult((byte) 0);
                        } else if (returnType == short.class || returnType == Short.class) {
                            param.setResult((short) 0);
                        } else if (returnType == float.class || returnType == Float.class) {
                            param.setResult(0.0f);
                        } else if (returnType == double.class || returnType == Double.class) {
                            param.setResult(0.0d);
                        } else if (returnType.isArray()) {
                            param.setResult(null);
                        } else if (returnType == Object.class) {
                            param.setResult(null);
                        }
                    });
                }
            }
        } catch (Throwable e) {
            LogUtils.e("KuGouHook", "hookTokenResultGetters error: " + e.getMessage());
        }
    }

    private static void hookPackageManagerSignatures() {
        try {
            Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
            Method currentActivityThread = activityThreadClass.getDeclaredMethod("currentActivityThread");
            currentActivityThread.setAccessible(true);
            Object activityThread = currentActivityThread.invoke(null);

            Method getPackageManager = activityThreadClass.getDeclaredMethod("getPackageManager");
            getPackageManager.setAccessible(true);
            Object packageManager = getPackageManager.invoke(activityThread);

            Class<?> packageManagerClass = packageManager.getClass();
            Method[] methods = packageManagerClass.getDeclaredMethods();
            for (Method method : methods) {
                if ("getPackageInfo".equals(method.getName())) {
                    Class<?>[] params = method.getParameterTypes();
                    if (params.length == 2 && params[0] == String.class && params[1] == int.class) {
                        HookUtils.hookAfter(method, param -> {
                            try {
                                Object result = param.getResult();
                                if (result instanceof PackageInfo) {
                                    PackageInfo info = (PackageInfo) result;
                                    String pkgName = (String) param.args[0];
                                    int flags = (int) param.args[1];

                                    if ((flags & PackageManager.GET_SIGNATURES) != 0 &&
                                        (pkgName.equals("com.kugou.android.lite") || pkgName.equals("com.kugou.android.elder"))) {
                                        info.signatures = new Signature[]{new Signature(KUGOU_ORIGINAL_SIGNATURE)};
                                    }
                                }
                            } catch (Throwable e) {
                                LogUtils.e("KuGouHook", "hookPackageManagerSignatures callback error: " + e.getMessage());
                            }
                        });
                    }
                }
            }

        } catch (Throwable e) {
            LogUtils.e("KuGouHook", "hookPackageManagerSignatures error: " + e.getMessage());
        }
    }

    private static Signature[] getOriginalSignatures(String packageName) {
        try {
            android.content.pm.ApplicationInfo appInfo = HostInfo.getHostContext().getPackageManager().getApplicationInfo(packageName, 0);
            if (appInfo == null || appInfo.sourceDir == null) {
                LogUtils.e("KuGouHook", "sourceDir is null");
                return null;
            }

            java.util.jar.JarFile jarFile = new java.util.jar.JarFile(appInfo.sourceDir);
            java.util.Enumeration<java.util.jar.JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                java.util.jar.JarEntry entry = entries.nextElement();
                if (entry.getName().endsWith(".RSA") || entry.getName().endsWith(".DSA")) {
                    java.io.InputStream is = jarFile.getInputStream(entry);
                    byte[] certBytes = new byte[is.available()];
                    is.read(certBytes);
                    is.close();
                    java.security.cert.CertificateFactory cf = java.security.cert.CertificateFactory.getInstance("X.509");
                    java.security.cert.X509Certificate x509Cert = (java.security.cert.X509Certificate) cf.generateCertificate(new java.io.ByteArrayInputStream(certBytes));
                    jarFile.close();
                    return new Signature[]{new Signature(x509Cert.getEncoded())};
                }
            }
            jarFile.close();
        } catch (PackageManager.NameNotFoundException e) {
            LogUtils.e("KuGouHook", "Package not found: " + packageName);
        } catch (Throwable e) {
            LogUtils.e("KuGouHook", "getOriginalSignatures error: " + e.getMessage());
        }
        return null;
    }

    private static void hookActivityStartActivity() {
        try {
            XposedHelpers.findAndHookMethod("android.app.Activity", ReflectUtils.hostClassLoader, "startActivity", Intent.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                    Intent intent = (Intent) param.args[0];
                    checkAndBlockBrowserIntent(intent, param);
                }
            });

            XposedHelpers.findAndHookMethod("android.app.Activity", ReflectUtils.hostClassLoader, "startActivity", Intent.class, android.os.Bundle.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                    Intent intent = (Intent) param.args[0];
                    checkAndBlockBrowserIntent(intent, param);
                }
            });

            XposedHelpers.findAndHookMethod("android.content.Context", ReflectUtils.hostClassLoader, "startActivity", Intent.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                    Intent intent = (Intent) param.args[0];
                    checkAndBlockBrowserIntent(intent, param);
                }
            });

            XposedHelpers.findAndHookMethod("android.content.Context", ReflectUtils.hostClassLoader, "startActivity", Intent.class, android.os.Bundle.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                    Intent intent = (Intent) param.args[0];
                    checkAndBlockBrowserIntent(intent, param);
                }
            });

            XposedHelpers.findAndHookMethod("android.content.ContextWrapper", ReflectUtils.hostClassLoader, "startActivity", Intent.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                    Intent intent = (Intent) param.args[0];
                    checkAndBlockBrowserIntent(intent, param);
                }
            });

            XposedHelpers.findAndHookMethod("android.content.ContextWrapper", ReflectUtils.hostClassLoader, "startActivity", Intent.class, android.os.Bundle.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                    Intent intent = (Intent) param.args[0];
                    checkAndBlockBrowserIntent(intent, param);
                }
            });

            XposedHelpers.findAndHookMethod("android.content.ContextWrapper", ReflectUtils.hostClassLoader, "startActivityForResult", Intent.class, int.class, android.os.Bundle.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                    Intent intent = (Intent) param.args[0];
                    checkAndBlockBrowserIntent(intent, param);
                }
            });

        } catch (Throwable e) {
            LogUtils.e("KuGouHook", "hookActivityStartActivity error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void checkAndBlockBrowserIntent(Intent intent, XC_MethodHook.MethodHookParam param) {
        if (intent == null) return;

        String action = intent.getAction();
        String dataString = intent.getDataString();
        String packageName = intent.getPackage();
        String componentName = intent.getComponent() != null ? intent.getComponent().getClassName() : "";

        if (dataString != null) {
        }

        boolean shouldBlock = false;

        if (action != null && action.equals("android.intent.action.VIEW")) {
            if (dataString != null && (dataString.startsWith("http://") || dataString.startsWith("https://"))) {
                shouldBlock = true;
            }
        }

        if (!shouldBlock && packageName != null) {
            if (packageName.contains("browser") || packageName.contains("chrome") || 
                packageName.contains("ucbrowser") || packageName.contains("qqbrowser") ||
                packageName.contains("sogou") || packageName.contains("baidu")) {
                shouldBlock = true;
            }
        }

        if (!shouldBlock && componentName != null) {
            if (componentName.contains("Browser") || componentName.contains("browser")) {
                shouldBlock = true;
            }
        }

        if (!shouldBlock && dataString != null) {
            String lowerData = dataString.toLowerCase();
            if (lowerData.contains("kugou") || lowerData.contains("crack") || 
                lowerData.contains("break") || lowerData.contains("pirate") ||
                lowerData.contains("hack") || lowerData.contains("modify") ||
                lowerData.contains("patch") || lowerData.contains("verify") ||
                lowerData.contains("unauthorized") || lowerData.contains("illegal") ||
                lowerData.contains("tamper") || lowerData.contains("detect")) {
                shouldBlock = true;
            }
        }

        if (shouldBlock) {
            param.setResult(null);
            param.setThrowable(new RuntimeException("Blocked by KuGouHook"));
        }
    }

    private static void hookPackageManagerHacker() {
        try {
            Class<?> hackerClass = ReflectUtils.hostClassLoader.loadClass("com.kugou.framework.hack.PackageManagerHacker");
            Method[] methods = hackerClass.getDeclaredMethods();
            for (Method method : methods) {
                if ("inject".equals(method.getName()) && method.getParameterTypes().length == 1) {
                    HookUtils.hookAfter(method, param -> {
                    });
                    break;
                }
            }
        } catch (ClassNotFoundException e) {
        } catch (Throwable e) {
            LogUtils.e("KuGouHook", "hookPackageManagerHacker error: " + e.getMessage());
        }
    }

    private static void hookPackageInfoHookImpl() {
        try {
            Class<?> hookImplClass = ReflectUtils.hostClassLoader.loadClass("com.kugou.framework.hack.PackageInfoHookImpl");
            Method[] methods = hookImplClass.getDeclaredMethods();
            for (Method method : methods) {
                if (method.getName().equals("hookPackageInfo") || 
                    method.getName().equals("a") || 
                    method.getName().equals("onHook")) {
                    HookUtils.hookAfter(method, param -> {
                    });
                }
            }
        } catch (ClassNotFoundException e) {
        } catch (Throwable e) {
            LogUtils.e("KuGouHook", "hookPackageInfoHookImpl error: " + e.getMessage());
        }
    }
}
