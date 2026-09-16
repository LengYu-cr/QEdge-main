package me.lengyu.qedge.hook.woodenletter;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import me.lengyu.qedge.utils.HostInfo;
import me.lengyu.qedge.utils.HookUtils;
import me.lengyu.qedge.utils.LogUtils;
import me.lengyu.qedge.utils.ReflectUtils;
import me.lengyu.qedge.utils.dexkit.DexKitManager;
import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.enums.StringMatchType;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;

/**
 * 木函 (com.One.WoodenLetter) 会员解锁
 *
 * 反编译原型（类名/方法名均为单字母混淆，版本更新即变，全部用 DexKit 结构特征定位）：
 *
 *   class com.One.WoodenLetter.activitys.user.util.a {
 *       public final boolean k() { return j() && f() > 0; }   // 会员判定
 *       public final int f() { ... return s3.a.e("user_gid", 0); }  // 读会员等级
 *       public final boolean j() { ... }                       // 登录判定
 *   }
 *   class s3.a {
 *       int e(String key, int defValue) { ... }                // 通用存储读
 *   }
 *
 * DexKit 查找链：
 *   1. 引用字符串 "user_gid" 的 int 无参方法 = f()，其所在类即 util.a
 *   2. 同类 boolean 无参、方法体 invoke 了 f() 的方法 = k()（用 MethodMatcher.create(f) 精确锁定）
 *   3. R8 内联兜底：f() 被内联时 k() 自己会引用 "user_gid"
 *   4. 存储兜底：f()/k() 调用的 (String,int)->int 方法 = s3.a.e()，对 key="user_gid" 返回 3
 */
public class WoodenLetterHook {

    private static final String TAG = "WoodenLetterHook";
    private static final String KEY_USER_GID = "user_gid";
    /** user_gid = 3 对应付费/会员等级（按逆向结论） */
    private static final int PRO_GID = 3;
    private static boolean initialized = false;

    public static void loadHook() {
        if (initialized) return;
        initialized = true;

        try {
            String sourceDir = HostInfo.getHostContext().getApplicationInfo().sourceDir;
            if (sourceDir == null) {
                LogUtils.e(TAG, "sourceDir is null");
                return;
            }
            if (!DexKitManager.ensureLibrary()) {
                LogUtils.e(TAG, "DexKit library load failed");
                return;
            }

            DexKitBridge bridge = DexKitBridge.create(sourceDir);
            if (bridge == null) {
                LogUtils.e(TAG, "DexKitBridge.create returned null");
                return;
            }

            try {
                hookVip(bridge);
            } finally {
                bridge.close();
            }
        } catch (Throwable e) {
            LogUtils.e(TAG, "loadHook error: " + e.getMessage());
            LogUtils.e(e);
        }
    }

    private static void hookVip(DexKitBridge bridge) {
        ClassLoader cl = ReflectUtils.hostClassLoader;

        // 1. 全量搜引用 "user_gid" 的方法（一次扫描覆盖 f()、存储包装、内联后的 k()）
        List<MethodData> gidHits;
        try {
            FindMethod find = new FindMethod();
            find.matcher(new MethodMatcher().addUsingString(KEY_USER_GID));
            gidHits = bridge.findMethod(find);
        } catch (Throwable t) {
            LogUtils.e(TAG, "find user_gid methods error: " + t.getMessage());
            return;
        }
        if (gidHits == null || gidHits.isEmpty()) {
            LogUtils.e(TAG, "未找到引用 user_gid 的方法，宿主版本可能已变更");
            return;
        }

        // 2. 识别 f()：int 无参；记录内联场景候选 k()：boolean 无参
        MethodData fData = null;
        List<MethodData> inlinedKCandidates = new ArrayList<>();
        for (MethodData md : gidHits) {
            if (md.getParamCount() != 0) continue;
            String ret = md.getReturnTypeName();
            if ("int".equals(ret) && fData == null) {
                fData = md;
            } else if ("boolean".equals(ret)) {
                inlinedKCandidates.add(md);
            }
        }

        String utilClassName = fData != null ? fData.getClassName() : null;
        if (utilClassName == null && !inlinedKCandidates.isEmpty()) {
            // f() 已被内联，util 类退化为从 k() 候选取得
            utilClassName = inlinedKCandidates.get(0).getClassName();
        }
        if (utilClassName == null) {
            LogUtils.e(TAG, "无法定位会员工具类（f()/k() 均未命中）");
            return;
        }

        Method fMethod = null;
        if (fData != null) {
            try {
                fMethod = fData.getMethodInstance(cl);
            } catch (Throwable t) {
                LogUtils.e(TAG, "resolve f() instance failed: " + t.getMessage());
            }
        }

        // 3. 定位 k()：本类 boolean 无参，且方法体调用了 f()
        Set<String> hookedKDescriptors = new HashSet<>();
        boolean hookedK = hookKViaExactInvoke(bridge, cl, utilClassName, fMethod, hookedKDescriptors);

        // 3.1 精确描述符失败 → 结构匹配（调用任意 int 无参 + boolean 无参方法）
        if (!hookedK) {
            hookedK = hookKViaStructuralShape(bridge, cl, utilClassName, hookedKDescriptors);
        }

        // 3.2 R8 内联兜底：k() 自己引用了 "user_gid"
        if (!hookedK) {
            for (MethodData md : inlinedKCandidates) {
                if (utilClassName.equals(md.getClassName())
                        && tryHookKData(md, cl, hookedKDescriptors)) {
                    hookedK = true;
                }
            }
        }

        if (!hookedK) {
            LogUtils.e(TAG, "k() 定位失败，仅启用存储层兜底");
        }

        // 4. 存储层兜底：f()/k() 调用链里的 (String,int)->int 读方法
        hookStorageReader(fData, cl);
        for (MethodData md : inlinedKCandidates) {
            if (utilClassName.equals(md.getClassName())) {
                hookStorageReader(md, cl);
            }
        }
    }

    /**
     * 精确方案：k() 必须调用了 f() 这个具体方法（方法描述符完全一致）
     */
    private static boolean hookKViaExactInvoke(DexKitBridge bridge, ClassLoader cl,
                                                String utilClassName, Method fMethod,
                                                Set<String> hookedDescriptors) {
        if (fMethod == null) return false;
        try {
            FindMethod find = new FindMethod();
            find.matcher(new MethodMatcher()
                    .declaredClass(utilClassName, StringMatchType.Equals, false)
                    .returnType("boolean")
                    .paramCount(0)
                    .addInvoke(MethodMatcher.create(fMethod))
            );
            List<MethodData> hits = bridge.findMethod(find);
            boolean any = false;
            for (MethodData md : hits) {
                if (tryHookKData(md, cl, hookedDescriptors)) any = true;
            }
            return any;
        } catch (Throwable t) {
            LogUtils.e(TAG, "hookKViaExactInvoke error: " + t.getMessage());
            return false;
        }
    }

    /**
     * 结构方案：本类 boolean 无参方法，调用了一个 int 无参方法（f）和一个 boolean 无参方法（j）
     */
    private static boolean hookKViaStructuralShape(DexKitBridge bridge, ClassLoader cl,
                                                    String utilClassName,
                                                    Set<String> hookedDescriptors) {
        try {
            FindMethod find = new FindMethod();
            find.matcher(new MethodMatcher()
                    .declaredClass(utilClassName, StringMatchType.Equals, false)
                    .returnType("boolean")
                    .paramCount(0)
                    .addInvoke(new MethodMatcher().returnType("int").paramCount(0))
                    .addInvoke(new MethodMatcher().returnType("boolean").paramCount(0))
            );
            List<MethodData> hits = bridge.findMethod(find);
            boolean any = false;
            for (MethodData md : hits) {
                if (tryHookKData(md, cl, hookedDescriptors)) any = true;
            }
            return any;
        } catch (Throwable t) {
            LogUtils.e(TAG, "hookKViaStructuralShape error: " + t.getMessage());
            return false;
        }
    }

    /**
     * 安装 k() → true，按描述符去重
     */
    private static boolean tryHookKData(MethodData md, ClassLoader cl, Set<String> hookedDescriptors) {
        try {
            String descriptor = md.getDescriptor();
            if (!hookedDescriptors.add(descriptor)) return false;
            Method m = md.getMethodInstance(cl);
            if (m == null) return false;
            m.setAccessible(true);
            HookUtils.hookBefore(m, param -> param.setResult(true));
            return true;
        } catch (Throwable t) {
            LogUtils.e(TAG, "hook k() failed: " + t.getMessage());
            return false;
        }
    }

    /**
     * 存储层兜底：扫描 owner 的调用链，找 (String, int|Integer) -> int|Integer 的读方法，
     * 仅当 key 参数 == "user_gid" 时返回 PRO_GID，其他 key 原样放行。
     */
    private static void hookStorageReader(MethodData owner, ClassLoader cl) {
        if (owner == null) return;
        try {
            for (MethodData invoked : owner.getInvokes()) {
                if (invoked.getParamCount() != 2) continue;
                List<String> params = invoked.getParamTypeNames();
                if (params == null || params.size() != 2) continue;
                if (!"java.lang.String".equals(params.get(0))) continue;
                String p1 = params.get(1);
                String ret = invoked.getReturnTypeName();
                boolean intLikeP1 = "int".equals(p1) || "java.lang.Integer".equals(p1);
                boolean intLikeRet = "int".equals(ret) || "java.lang.Integer".equals(ret);
                if (!intLikeP1 || !intLikeRet) continue;

                Method storageRead;
                try {
                    storageRead = invoked.getMethodInstance(cl);
                } catch (Throwable t) {
                    continue;
                }
                if (storageRead == null) continue;
                storageRead.setAccessible(true);
                HookUtils.hookBefore(storageRead, param -> {
                    if (param.args != null && param.args.length >= 1
                            && KEY_USER_GID.equals(param.args[0])) {
                        param.setResult(PRO_GID);
                    }
                });
                
            }
        } catch (Throwable t) {
            LogUtils.e(TAG, "hookStorageReader error: " + t.getMessage());
        }
    }
}
