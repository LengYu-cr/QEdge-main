package me.lengyu.qedge.hook.item

import android.app.Activity
import me.lengyu.qedge.hook.annotation.HookItemAnnotation
import me.lengyu.qedge.hook.base.BaseSwitchHookItem
import me.lengyu.qedge.utils.HookUtils
import me.lengyu.qedge.utils.HostInfo
import me.lengyu.qedge.utils.LogUtils
import me.lengyu.qedge.utils.ModuleConfig
import me.lengyu.qedge.utils.ReflectUtils
import me.lengyu.qedge.utils.dexkit.DexKitTask
import org.luckypray.dexkit.query.FindClass
import org.luckypray.dexkit.query.base.BaseFinder
import java.lang.reflect.Modifier

/**
 * @Author 冷雨
 * @Description 屏蔽聊天页贴边浮动的 Q宠，同时阻止双击宠物跳转 Adelie 主页。
 *
 * 贴边宠物有两条创建路径：
 * 1. 会话页 / 主界面：com.tencent.mobileqq.qqpet.TabPetController#createMainFrameQQPet
 *    内部 new PetDragFrameLayout 并挂到 tabLayoutRoot，再创建 PetFloatingView。
 *    PetDragFrameLayout 的唯一业务调用方就是该方法，方法名被混淆，按签名
 *    (Activity, int) -> void 定位，前置返回即可完全不创建宠物视图。
 * 2. 单聊页：com.tencent.mobileqq.qqpet.aio.c2c.AIOPetVB（类名被混淆）的静态方法
 *    doInitC2CPetView 负责 new C2CPetViewController、创建宠物 View 并 addView。
 *    按「静态 + 6 参数 + 首参为自身 + 次参为 FragmentActivity」定位。
 *
 * 两条路径都拦截后，聊天页不会再出现贴边宠物，也就没有可双击进入 Adelie 的入口。
 */
@HookItemAnnotation(
    value = "屏蔽聊天页 Q宠",
    category = "item",
    tag = "屏蔽聊天页 Q宠",
    desc = "聊天页不再显示贴边浮动的 Q宠，也不会再双击进入 Q宠主页",
    process = "Main"
)
object HideAioPet : BaseSwitchHookItem(), DexKitTask {

    private const val TAG = "HideAioPet"

    private const val KEY_ENABLE = "hide_aio_pet"

    private const val KEY_TAB_PET_CONTROLLER = "tab_pet_controller"
    private const val KEY_AIO_PET_VB = "aio_pet_vb"

    private const val QQ_PET_PACKAGE = "com.tencent.mobileqq.qqpet"
    private const val QQ_PET_AIO_PACKAGE = "com.tencent.mobileqq.qqpet.aio"

    /** TabPetController 类名未混淆，DexKit 未命中时用它兜底 */
    private const val TAB_PET_CONTROLLER_CLASS = "com.tencent.mobileqq.qqpet.TabPetController"

    /** 工程未依赖 androidx.fragment，按类名比较 */
    private const val FRAGMENT_ACTIVITY_CLASS = "androidx.fragment.app.FragmentActivity"

    /** Q宠从 QQ 9.3.30 开始回归，低版本不安装钩子。 */
    private const val MIN_MAJOR = 9
    private const val MIN_MINOR = 3
    private const val MIN_PATCH = 30

    private fun isEnabled() = ModuleConfig.getBoolean(KEY_ENABLE, false)

    /** 当前宿主是否为 QQ 9.3.30 及以上（UI 侧也复用该判断）。 */
    fun isSupported(): Boolean {
        if (!HostInfo.isQQ) return false
        val parts = HostInfo.versionName.split(".")
        val major = parts.getOrNull(0)?.toIntOrNull() ?: return false
        val minor = parts.getOrNull(1)?.toIntOrNull() ?: return false
        val patch = parts.getOrNull(2)?.filter { it.isDigit() }?.toIntOrNull() ?: return false
        return when {
            major != MIN_MAJOR -> major > MIN_MAJOR
            minor != MIN_MINOR -> minor > MIN_MINOR
            else -> patch >= MIN_PATCH
        }
    }

    /** 低版本没有该功能，不执行 DexKit 查询。 */
    override fun isApplicable(): Boolean = isSupported()

    override fun getQueryMap(): Map<String, BaseFinder> = mapOf(
        KEY_TAB_PET_CONTROLLER to FindClass().apply {
            searchPackages(QQ_PET_PACKAGE)
            matcher {
                usingStrings(
                    "TabPetController[Pet]",
                    "doCreateQQPetView: petView already exists, skip duplicate creation",
                    "addView tabLayoutRoot: "
                )
            }
        },
        KEY_AIO_PET_VB to FindClass().apply {
            searchPackages(QQ_PET_AIO_PACKAGE)
            matcher {
                usingStrings(
                    "AIOPetVB",
                    "doInitC2CPetView: fragment detached, skip",
                    "create pet view root width: "
                )
            }
        }
    )

    override fun onInit(): Boolean {
        if (!isSupported()) {
            LogUtils.i(TAG, "QQ ${HostInfo.versionName} 低于 9.3.30，跳过")
            return false
        }
        return true
    }

    /**
     * 钩子无条件安装，开关在回调里判断，改动即时生效。
     */
    override fun onHook() {
        hookTabPet()
        hookC2CPet()
    }

    /**
     * 会话页 / 主界面贴边宠物。
     * TabPetController 中唯一 (Activity, int) -> void 的方法就是 createMainFrameQQPet。
     */
    private fun hookTabPet() {
        val clazz = resolveTabPetController() ?: run {
            LogUtils.e(TAG, "TabPetController not found")
            return
        }
        val methods = clazz.declaredMethods.filter {
            !it.isSynthetic && !it.isBridge &&
                it.returnType == Void.TYPE &&
                it.parameterTypes.size == 2 &&
                it.parameterTypes[0] == Activity::class.java &&
                it.parameterTypes[1] == Int::class.javaPrimitiveType
        }
        if (methods.isEmpty()) {
            LogUtils.e(TAG, "createMainFrameQQPet not found")
            return
        }
        methods.forEach { method ->
            HookUtils.hookBefore(method) { param ->
                if (!isEnabled()) return@hookBefore
                param.result = null
            }
        }
    }

    /**
     * 单聊页贴边宠物：AIOPetVB#doInitC2CPetView 是创建入口，类名被混淆。
     */
    private fun hookC2CPet() {
        val clazz = try {
            requireClass(KEY_AIO_PET_VB)
        } catch (e: Throwable) {
            LogUtils.e(TAG, "DexKit resolve AIOPetVB error: ${e.message}")
            return
        }
        val methods = clazz.declaredMethods.filter {
            !it.isBridge &&
                Modifier.isStatic(it.modifiers) &&
                it.returnType == Void.TYPE &&
                it.parameterTypes.size == 6 &&
                it.parameterTypes[0] == clazz &&
                it.parameterTypes[1].name == FRAGMENT_ACTIVITY_CLASS
        }
        if (methods.isEmpty()) {
            LogUtils.e(TAG, "doInitC2CPetView not found")
            return
        }
        methods.forEach { method ->
            HookUtils.hookBefore(method) { param ->
                if (!isEnabled()) return@hookBefore
                param.result = null
            }
        }
    }

    private fun resolveTabPetController(): Class<*>? {
        return try {
            requireClass(KEY_TAB_PET_CONTROLLER)
        } catch (e: Throwable) {
            LogUtils.e(TAG, "DexKit resolve error: ${e.message}，回退类名")
            ReflectUtils.findClassIfExists(TAB_PET_CONTROLLER_CLASS)
        }
    }
}
