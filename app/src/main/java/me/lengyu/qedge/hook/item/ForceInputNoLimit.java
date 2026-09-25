package me.lengyu.qedge.hook.item;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import me.lengyu.qedge.hook.annotation.HookItemAnnotation;
import me.lengyu.qedge.hook.base.BaseApiHookItem;
import me.lengyu.qedge.utils.LogUtils;
import me.lengyu.qedge.utils.ModuleConfig;
import me.lengyu.qedge.utils.ReflectUtils;

@HookItemAnnotation(value = "解除输入框字数上限", category = "item")
public class ForceInputNoLimit extends BaseApiHookItem {

    public static final ForceInputNoLimit INSTANCE = new ForceInputNoLimit();
    private static final String TAG = "ForceInputNoLimit";

    /** 目标上限值，可按需调整 */
    private static final int MAX_LENGTH = 1000000;

    private static final String AIO_EDIT_TEXT =
            "com.tencent.mobileqq.aio.input.edit.AIOEditText";

    private static boolean isEnabled() {
        return ModuleConfig.INSTANCE.getBoolean("force_input_no_limit", false);
    }

    @Override
    public void loadHook() {
        if (!isEnabled()) return;
        hookUniqueIntField();
    }

    /**
     * 扫描 AIOEditText 里唯一的 static final int 字段并改值。
     * 该字段由资源读取赋值（非编译期常量），改字段值通常可生效。
     * 若出现多个候选则放弃，避免误伤其它 int 字段。
     */
    private void hookUniqueIntField() {
        try {
            Class<?> cls = ReflectUtils.hostClassLoader.loadClass(AIO_EDIT_TEXT);

            Field target = null;
            for (Field f : cls.getDeclaredFields()) {
                if (f.getType() != int.class) continue;
                if (f.isSynthetic()) continue;
                int mod = f.getModifiers();
                if (!Modifier.isStatic(mod) || !Modifier.isFinal(mod)) continue;

                if (target != null) {
                    // 出现第二个候选，唯一性破裂，放弃
                    LogUtils.e(TAG, "发现多个 static final int 字段，放弃改字段");
                    return;
                }
                target = f;
            }
            if (target == null) {
                LogUtils.e(TAG, "未找到唯一的 static final int 字段");
                return;
            }

            target.setAccessible(true);

            // 尝试去掉 final 修饰（Android 高版本可能不允许，忽略失败）
            try {
                Field modifiersField = Field.class.getDeclaredField("accessFlags");
                modifiersField.setAccessible(true);
                int flags = modifiersField.getInt(target);
                modifiersField.setInt(target, flags & ~Modifier.FINAL);
            } catch (Throwable ignore) {
                // 忽略
            }

            target.setInt(null, MAX_LENGTH);
        } catch (Throwable t) {
            LogUtils.e(TAG, "hookUniqueIntField: " + t);
        }
    }
}