package me.lengyu.qedge.hook.item

import me.lengyu.qedge.hook.annotation.HookItemAnnotation
import me.lengyu.qedge.hook.base.BaseSwitchHookItem
import me.lengyu.qedge.utils.HookUtils
import me.lengyu.qedge.utils.LogUtils
import me.lengyu.qedge.utils.ModuleConfig
import me.lengyu.qedge.utils.ReflectUtils
import de.robv.android.xposed.XposedHelpers

/**
 * @Author 冷雨
 * @Description 绕过 QQ 资料卡封禁账号拦截，强制显示被封禁用户的资料卡主页
 * 通过 Hook ProfileCardForbidAccountHelper 的判断方法，使其始终返回未封禁状态
 */
@HookItemAnnotation(
    value = "绕过资料卡封禁",
    category = "item",
    tag = "资料卡",
    desc = "强制显示被封禁用户的 QQ 资料卡主页，绕过封禁拦截弹窗"
)
object BypassProfileBan : BaseSwitchHookItem() {

    private const val TAG = "BypassProfileBan"
    private const val KEY_ENABLE = "bypass_profile_ban"

    // ProfileCardForbidAccountHelper 封禁判断工具类
    private const val FORBID_HELPER = "com.tencent.mobileqq.profilecard.utils.ProfileCardForbidAccountHelper"
    // ProfileCardInfo 资料卡数据容器
    private const val PROFILE_CARD_INFO = "com.tencent.mobileqq.profilecard.data.ProfileCardInfo"
    // FriendProfileCardActivity 资料卡页面，封禁字段在数据刷新时被置为 true，导致按钮/头像变暗
    private const val FRIEND_ACTIVITY = "com.tencent.mobileqq.profilecard.activity.FriendProfileCardActivity"

    private fun isEnabled() = ModuleConfig.getBoolean(KEY_ENABLE, false)

    override fun onInit(): Boolean = true

    override fun onHook() {
        val classLoader = ReflectUtils.hostClassLoader ?: return
        hookForbidHelper(classLoader)
        hookForbidSpecify(classLoader)
        hookForbidFieldReset(classLoader)
        hookNickProtect(classLoader)
    }

    // 记录每个 Activity 首个有效的本地昵称，用于回填 strNick / nameArray[0] / allInOne.nickname
    private val nicknameBackup = java.util.concurrent.ConcurrentHashMap<Any, String>()

    /**
     * 资料卡昵称空白原因：被封禁账号服务端卡片不下发 strNick（null），
     * updateNameArrayByCard 会无条件用 strNick 覆盖 nameArray[0]，
     * 而顶栏昵称实际取自 strNick / allInOne.nickname，三者都 null 就空白。
     * 这里把首次进入时本地关系链初始化好的昵称缓存下来，预填进 card.strNick，
     * 走和正常账号一致的填充链路；并在 after 阶段兜底回填 nameArray[0] 与 allInOne.nickname。
     */
    private fun hookNickProtect(classLoader: ClassLoader) {
        try {
            val activityClass = Class.forName(FRIEND_ACTIVITY, false, classLoader)
            for (m in activityClass.declaredMethods.filter { it.name == "updateNameArrayByCard" }) {
                HookUtils.hookBefore(m) { param ->
                    try {
                        val info = XposedHelpers.getObjectField(param.thisObject, "mProfileCardInfo")
                        val card = XposedHelpers.getObjectField(info, "card")
                        val nameArray = XposedHelpers.getObjectField(info, "nameArray") as? Array<*>
                        // 记录首个非空本地昵称（initNameArray 初始化出来的真实昵称）
                        val old = nameArray?.getOrNull(0) as? String
                        if (!old.isNullOrBlank()) {
                            nicknameBackup[param.thisObject] = old
                        }
                        // 关键在于让 strNick 非空，这样 QQ 原生链路才会把昵称铺到顶栏
                        val nick = nicknameBackup[param.thisObject]
                        if (!nick.isNullOrBlank()) {
                            val strNick = XposedHelpers.getObjectField(card, "strNick") as? String
                            if (strNick.isNullOrBlank()) {
                                XposedHelpers.setObjectField(card, "strNick", nick)
                            }
                        }
                    } catch (_: Throwable) {
                    }
                }
                HookUtils.hookAfter(m) { param ->
                    try {
                        val info = XposedHelpers.getObjectField(param.thisObject, "mProfileCardInfo")
                        val nick = nicknameBackup[param.thisObject]
                        if (nick.isNullOrBlank()) return@hookAfter
                        // nameArray[0] 兜底：避免被 strNick(null) 覆盖后空白
                        val nameArray = XposedHelpers.getObjectField(info, "nameArray") as? Array<*>
                        if (nameArray?.getOrNull(0)?.toString().isNullOrBlank() && nameArray is Array<*>) {
                            @Suppress("UNCHECKED_CAST")
                            (nameArray as Array<Any?>)[0] = nick
                        }
                        // allInOne.nickname 兜底：顶栏标题可能直接读它
                        val allInOne = XposedHelpers.getObjectField(info, "allInOne")
                        if (XposedHelpers.getObjectField(allInOne, "nickname").toString().isNullOrBlank()) {
                            XposedHelpers.setObjectField(allInOne, "nickname", nick)
                        }
                        // LogUtils.i(TAG, "昵称兜底生效: $nick")
                    } catch (_: Throwable) {
                    }
                }
            }
        } catch (e: Throwable) {
            LogUtils.e(TAG, "hookNickProtect error: ${e.message}")
        }
    }

    /**
     * 列表组件直接读取 Card/ContactCard 的 isForbidAccount 字段来决定按钮/头像是否置灰，
     * 该字段在 handleNotifyProfileCard 等数据刷新路径中被置为 true，直接读字段无法被 hook。
     * 因此在资料卡所有刷新出口前，把 mProfileCardInfo 下的 card / contactCard 的
     * isForbidAccount 重置为 false，确保组件渲染时读到的是未封禁状态。
     */
    private fun hookForbidFieldReset(classLoader: ClassLoader) {
        try {
            val activityClass = Class.forName(FRIEND_ACTIVITY, false, classLoader)
            // 资料卡数据刷新的所有入口，任意一个触发都会在 UI 读取字段前重置封禁状态
            val targets = arrayOf(
                "handleGetProfileCard",
                "handleNotifyProfileCard",
                "onCardUpdate",
                "updateCardInfo",
                "updateForbidState"
            )
            var hookedCount = 0
            for (name in targets) {
                val methods = activityClass.declaredMethods.filter { it.name == name }
                for (method in methods) {
                    HookUtils.hookBefore(method) { param ->
                        if (isEnabled()) resetForbidFields(param.thisObject)
                    }
                    hookedCount++
                }
            }
            // LogUtils.i(TAG, "已 Hook FriendProfileCardActivity 刷新出口，重置封禁字段, 方法数=" + hookedCount)
        } catch (e: Throwable) {
            LogUtils.e(TAG, "hookForbidFieldReset error: ${e.message}")
        }
    }

    /**
     * 重置 mProfileCardInfo 下 card / contactCard 的 isForbidAccount / forbidCode，
     * 使按钮与头像渲染时读到未封禁状态。字段不存在时静默跳过。
     */
    private fun resetForbidFields(activity: Any?) {
        try {
            if (activity == null) return
            val info = XposedHelpers.getObjectField(activity, "mProfileCardInfo") ?: return
            val card = XposedHelpers.getObjectField(info, "card")
            val contactCard = XposedHelpers.getObjectField(info, "contactCard")
            for (obj in listOf(card, contactCard)) {
                if (obj == null) continue
                try {
                    XposedHelpers.setBooleanField(obj, "isForbidAccount", false)
                } catch (_: Throwable) {
                }
                try {
                    XposedHelpers.setIntField(obj, "forbidCode", 0)
                } catch (_: Throwable) {
                }
            }
        } catch (_: Throwable) {
        }
    }

    /**
     * Hook isForbidByAnyType 方法，拦截所有封禁类型判断
     * 强制返回 false，绕过所有封禁检查
     */
    private fun hookForbidHelper(classLoader: ClassLoader) {
        try {
            val helperClass = Class.forName(FORBID_HELPER, false, classLoader)
            val infoClass = Class.forName(PROFILE_CARD_INFO, false, classLoader)
            val method = XposedHelpers.findMethodExact(helperClass, "isForbidByAnyType", infoClass)
            
            HookUtils.hookReplace(method) { param ->
                if (!isEnabled()) return@hookReplace HookUtils.invokeOriginalMethod(param)
                // 强制返回 false，表示非封禁账号
                false
            }
            // LogUtils.i(TAG, "已 Hook isForbidByAnyType")
        } catch (e: Throwable) {
            LogUtils.e(TAG, "hookForbidHelper error: ${e.message}")
        }
    }

    /**
     * Hook isForbidBySpecifyTypes 方法，拦截特定封禁码判断
     * 强制返回 false，绕过 201 等特定封禁码弹窗
     */
    private fun hookForbidSpecify(classLoader: ClassLoader) {
        try {
            val helperClass = Class.forName(FORBID_HELPER, false, classLoader)

            // isForbidBySpecifyTypes 的具体签名不固定（重载随版本变化），
            // 遍历同名方法，取返回 boolean 且参数数量匹配的（绕过具体参数类型差异）
            val method = helperClass.declaredMethods
                .filter { it.name == "isForbidBySpecifyTypes" && it.returnType == Boolean::class.javaPrimitiveType }
                .firstOrNull()
                ?: error("未找到 isForbidBySpecifyTypes")

            // LogUtils.i(TAG, "匹配到 isForbidBySpecifyTypes 签名: " + method.toGenericString())

            HookUtils.hookReplace(method) { param ->
                if (!isEnabled()) return@hookReplace HookUtils.invokeOriginalMethod(param)
                // 强制返回 false，不匹配任何封禁码
                false
            }
            // LogUtils.i(TAG, "已 Hook isForbidBySpecifyTypes")
        } catch (e: Throwable) {
            LogUtils.e(TAG, "hookForbidSpecify error: ${e.message}")
        }
    }
}