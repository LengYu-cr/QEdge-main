package me.lengyu.qedge.plugin.view

import android.app.Activity
import android.content.Intent
import android.view.View
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tencent.qqnt.aio.activity.AIODelegate
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import me.lengyu.qedge.activity.SettingActivity
import me.lengyu.qedge.coldrain.ColdRainCore
import me.lengyu.qedge.lifecycle.Parasitics
import me.lengyu.qedge.plugin.PluginManager
import me.lengyu.qedge.plugin.bean.PluginInfo
import me.lengyu.qedge.ui.pages.coldrain.ColdRainConfig
import me.lengyu.qedge.ui.core.compatibility.QEdgeBottomDialog
import me.lengyu.qedge.ui.core.theme.AccentGreen
import me.lengyu.qedge.ui.core.theme.Dimens
import me.lengyu.qedge.ui.core.theme.QEdgeTheme
import me.lengyu.qedge.utils.LogUtils
import me.lengyu.qedge.utils.ModuleConfig
import me.lengyu.qedge.utils.qq.QQCurrentEnv
import me.lengyu.qedge.utils.Toasts
import me.lengyu.qedge.utils.qq.FriendTool
import java.util.regex.Pattern

object ChatSettingLoader {

    private var hooked = false
    private var aioDelegate: AIODelegate? = null

    // 所有可选的聊天页入口匹配点
    val ENTRY_OPTIONS = mapOf(
        "more_features" to "更多功能",
        "chat_settings" to "聊天设置",
        "bubble" to "泡泡",
        "emoji" to "表情",
        "camera" to "相机",
        "album" to "相册",
        "voice" to "语音"
    )

    data class PluginContact(
        val chatType: Int = 0,
        val peerUid: String = "",
        val peerUin: String = "",
        val guild: String = "",
        val peerName: String = ""
    )

    val currentContact: PluginContact
        get() = aioDelegate?.let {
            parseToPluginContact(it.aioContact.toString())
        } ?: PluginContact()

    @JvmStatic
    fun loadHook() {
        if (hooked) return
        hooked = true

        try {
            XposedBridge.hookAllMethods(
                android.widget.ImageView::class.java,
                "onAttachedToWindow",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        try {
                            // 根据配置选择入口匹配点，避免与其他模块冲突
                            val entryMode = ModuleConfig.getString("chat_setting_entry", "more_features")
                            val targetText = ENTRY_OPTIONS[entryMode] ?: "更多功能"

                            val view = param.thisObject as android.widget.ImageView
                            val desc = view.contentDescription
                            if (desc != null && desc.toString().contains(targetText)) {
                                view.post {
                                    try {
                                        view.setOnLongClickListener {
                                            showMenuDialog(view)
                                            true
                                        }
                                            } catch (e: Throwable) {
                                        LogUtils.e("ChatSettingLoader", "setOnLongClickListener failed: " + e.message)
                                    }
                                }
                            }
                                } catch (e: Throwable) {
                            LogUtils.e("ChatSettingLoader", "afterHookedMethod failed: " + e.message)
                        }
                    }
                }
            )

            XposedBridge.hookAllMethods(
                AIODelegate::class.java,
                "show",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        try {
                            aioDelegate = param.thisObject as AIODelegate
                            notifyChatInterface()
                        } catch (e: Throwable) {
                            LogUtils.e("ChatSettingLoader", "hook AIODelegate.show failed: " + e.message)
                        }
                    }
                }
            )

            XposedBridge.hookAllMethods(
                AIODelegate::class.java,
                "hide",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        aioDelegate = null
                    }
                }
            )

        } catch (e: Throwable) {
            LogUtils.e("ChatSettingLoader", "loadHook failed: " + e.message)
        }
    }

    private fun notifyChatInterface() {
        try {
            val contact = currentContact
            if (contact.peerUin.isEmpty()) return
            PluginManager.plugins.filter { it.isRunning() }.forEach { plugin ->
                try {
                    plugin.getCompiler().getCallback().chatInterface(
                        contact.chatType,
                        contact.peerUin,
                        contact.peerName
                    )
                } catch (e: Throwable) {
                    LogUtils.e("ChatSettingLoader", "chatInterface callback failed: " + e.message)
                }
            }
        } catch (e: Throwable) {
            LogUtils.e("ChatSettingLoader", "notifyChatInterface failed: " + e.message)
        }
    }

    private fun parseToPluginContact(input: String): PluginContact {
        val regex = Pattern.compile("(\\w+)=([^,)]*)")
        val matcher = regex.matcher(input)
        val map = mutableMapOf<String, String>()
        while (matcher.find()) {
            val key = matcher.group(1) ?: continue
            val value = matcher.group(2)?.trim('\'') ?: ""
            map[key] = value
        }

        val chatType = try {
            map["chatType"]?.toInt() ?: 0
        } catch (e: NumberFormatException) {
            0
        }
        val peerUid = map["peerUid"] ?: ""
        val guild = map["guildId"] ?: ""
        val peerName = map["nick"] ?: ""

        val peerUin = if (chatType == 2) {
            peerUid
        } else {
            try {
                FriendTool.getUinFromUid(peerUid).ifEmpty {
                    ""
                }
            } catch (e: Throwable) {
                ""
            }
        }

        return PluginContact(
            chatType,
            peerUid,
            peerUin,
            guild,
            peerName
        )
    }

    private fun showMenuDialog(view: View) {
        // 优先使用真实 Activity Context，避免 view.context 是 ContextThemeWrapper 或已销毁的 Activity
        // （"显示完闪退"最常见根因：BadTokenException 在动画结束后 relayout 时被 WindowManager 抛出）
        val hostActivity = QQCurrentEnv.getActivity()
            ?: (view.context as? Activity)
        if (hostActivity == null || hostActivity.isFinishing || hostActivity.isDestroyed) {
            LogUtils.e("ChatSettingLoader", "skip showMenuDialog: host activity invalid")
            Toasts.toast("当前页面状态异常，无法打开菜单")
            return
        }
        Parasitics.ensureInitialized(hostActivity)
        runCatching { Parasitics.injectModuleResources(hostActivity.resources) }

        val menuItems = mutableListOf<PluginMenuItem>()

        for (plugin in PluginManager.plugins) {
            runCatching {
                if (plugin.isRunning()) {
                    val items = plugin.getCompiler().getMenuItems()
                    if (items.isNotEmpty()) {
                        menuItems.add(PluginMenuItem.Header(plugin.name, plugin.id))
                        for ((name, method) in items) {
                            menuItems.add(
                                PluginMenuItem.Action(
                                    name,
                                    plugin.id
                                ) {
                                    invokeMenuItem(plugin, method)
                                }
                            )
                        }
                    }
                }
            }.onFailure { e ->
                LogUtils.e("ChatSettingLoader", "build plugin menu failed for ${plugin.name}: ${e.message}")
            }
        }

        runCatching {
            val coldRain = ColdRainCore.getInstance()
            if (coldRain.isInitialized() && coldRain.isMasterEnabled()) {
                val contact = currentContact
                val isGroup = contact.chatType == 2
                val effectiveKey = if (isGroup) contact.peerUin else "friend_global"
                menuItems.add(PluginMenuItem.Header("冷雨Java", "cold_rain"))
                menuItems.add(
                    PluginMenuItem.Action(
                        if (coldRain.isGroupMasterEnabled(effectiveKey)) "关机" else "开机",
                        "cold_rain"
                    ) {
                        if (contact.peerUin.isEmpty()) {
                            Toasts.toast("获取聊天信息失败")
                            return@Action
                        }
                        val enabled = coldRain.isGroupMasterEnabled(effectiveKey)
                        coldRain.setGroupMasterEnabled(effectiveKey, !enabled)
                        Toasts.toast(if (enabled) (if (isGroup) "本群已关机" else "好友聊天已关机") else (if (isGroup) "本群已开机" else "好友聊天已开机"))
                    }
                )
                menuItems.add(
                    PluginMenuItem.Action(
                        "开关设置",
                        "cold_rain"
                    ) {
                        if (contact.peerUin.isEmpty()) {
                            Toasts.toast("获取聊天信息失败")
                            return@Action
                        }
                        showGroupSwitchDialog(hostActivity, effectiveKey, isGroup)
                    }
                )
            }
        }.onFailure { e ->
            LogUtils.e("ChatSettingLoader", "build coldrain menu failed: ${e.message}")
        }

        if (menuItems.isEmpty()) {
            startPluginActivity(hostActivity)
            return
        }

        runCatching {
            QEdgeBottomDialog(hostActivity) { dismiss ->
                PluginMenuContent(menuItems, dismiss, {
                    startPluginActivity(hostActivity)
                    dismiss()
                }) { pluginId ->
                    val plugin = PluginManager.plugins.find { it.id == pluginId }
                    if (plugin != null) {
                        Toasts.toast("正在重载: ${plugin.name}...")
                        dismiss()
                        try {
                            PluginManager.stopPlugin(plugin)
                            PluginManager.startPlugin(plugin)
                            Toasts.toast("${plugin.name} 重载成功")
                        } catch (e: Exception) {
                            Toasts.toast("重载失败: ${e.message}")
                        }
                    }
                }
            }.show()
        }.onFailure { e ->
            LogUtils.e("ChatSettingLoader", "showMenuDialog failed: ${e.message}")
            Toasts.toast("打开菜单失败: ${e.message}")
        }
    }

    private fun startPluginActivity(context: android.content.Context) {
        try {
            val activity = QQCurrentEnv.getActivity()
                ?: (context as? Activity)
            if (activity == null || activity.isFinishing || activity.isDestroyed) {
                LogUtils.e("ChatSettingLoader", "skip startPluginActivity: host activity invalid")
                Toasts.toast("当前页面状态异常")
                return
            }
            Parasitics.ensureInitialized(activity)
            runCatching { Parasitics.injectModuleResources(activity.resources) }
            val intent = Intent(activity, SettingActivity::class.java)
            // 直接跳转到 Java 脚本页面（HomeScreen），不受上次停留页面影响
            intent.putExtra("page", "plugin")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            intent.addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            activity.startActivity(intent)
        } catch (e: Throwable) {
            LogUtils.e("ChatSettingLoader", "startPluginActivity failed: " + e.message)
            Toasts.toast("跳转失败: ${e.message}")
        }
    }

    private fun invokeMenuItem(plugin: PluginInfo, methodName: String) {
        try {
            val contact = currentContact
            if (contact.peerUin.isEmpty() || contact.chatType < 0) {
                Toasts.toast("获取聊天信息失败")
                return
            }
            val callback = plugin.getCompiler().getCallback()
                ?: error("插件未注册Callback: ${plugin.name}")
            callback.invokeMenuItem(
                methodName,
                contact.chatType,
                contact.peerUin,
                contact.peerName
            )
        } catch (e: Throwable) {
            LogUtils.e("ChatSettingLoader", "invokeMenuItem ${plugin.name}#$methodName failed: ${e.message}")
            Toasts.toast("执行失败: ${e.message}")
        }
    }

    private fun showGroupSwitchDialog(context: android.content.Context, peerUin: String, isGroup: Boolean) {
        try {
            val coldRain = ColdRainCore.getInstance()
            if (!coldRain.isInitialized()) {
                Toasts.toast("冷雨Java未初始化")
                return
            }
            val activity = QQCurrentEnv.getActivity()
                ?: (context as? Activity)
            if (activity == null || activity.isFinishing || activity.isDestroyed) {
                LogUtils.e("ChatSettingLoader", "skip showGroupSwitchDialog: host activity invalid")
                Toasts.toast("当前页面状态异常")
                return
            }
            Parasitics.ensureInitialized(activity)
            runCatching { Parasitics.injectModuleResources(activity.resources) }
            ColdRainConfig.init(activity)

            val features = ColdRainConfig.allFeatures.filter {
                coldRain.isFeatureEnabled(it.key) &&
                !ColdRainConfig.isPersonalFeature(it.key) &&
                !(isGroup && false || !isGroup && coldRain.isGroupOnlyFeature(it.key))
            }

            runCatching {
                QEdgeBottomDialog(activity) { dismiss ->
                    GroupSwitchContent(
                        peerUin = peerUin,
                        isGroup = isGroup,
                        features = features,
                        onDismiss = dismiss
                    )
                }.show()
            }.onFailure { e ->
                LogUtils.e("ChatSettingLoader", "showGroupSwitchDialog dialog show failed: ${e.message}")
                Toasts.toast("打开失败: ${e.message}")
            }
        } catch (e: Throwable) {
            LogUtils.e("ChatSettingLoader", "showGroupSwitchDialog failed: ${e.message}")
            Toasts.toast("打开失败: ${e.message}")
        }
    }

    sealed class PluginMenuItem {
        data class Header(val name: String, val pluginId: String) : PluginMenuItem()
        data class Action(val name: String, val pluginId: String, val onClick: () -> Unit) : PluginMenuItem()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PluginMenuContent(
    menuItems: List<ChatSettingLoader.PluginMenuItem>,
    onDismiss: () -> Unit,
    onTitleLongClick: () -> Unit,
    onReloadPlugin: (String) -> Unit
) {
    val colors = QEdgeTheme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .padding(Dimens.PaddingLarge)
    ) {
        Text(
            "脚本菜单",
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .combinedClickable(
                    interactionSource = androidx.compose.foundation.interaction.MutableInteractionSource(),
                    indication = null,
                    onLongClick = onTitleLongClick,
                    onClick = {}
                )
                .padding(vertical = 4.dp),
            colors.textPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(Dimens.PaddingSmall))
        Text(
            "点击脚本名称可重载脚本",
            Modifier.fillMaxWidth(),
            colors.textSecondary,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))
        HorizontalDivider(color = colors.textSecondary.copy(0.1f))
        Spacer(Modifier.height(20.dp))
        if (menuItems.isEmpty()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 40.dp), Alignment.Center
            ) {
                Text("暂无运行中的脚本菜单", fontSize = 14.sp, color = colors.textSecondary)
            }
        } else {
            LazyColumn(
                Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Dimens.PaddingMedium)
            ) {
                // 显式 key：sealed class 混合列表时，自增索引会导致 diff 阶段
                // IndexOutOfBoundsException（LazyList 在动画结束下一次 recompose 时崩溃）
                items(
                    menuItems,
                    key = { item ->
                        when (item) {
                            is ChatSettingLoader.PluginMenuItem.Header ->
                                "header_${item.pluginId}_${item.name}"
                            is ChatSettingLoader.PluginMenuItem.Action ->
                                "action_${item.pluginId}_${item.name}"
                        }
                    }
                ) { item ->
                    when (item) {
                        is ChatSettingLoader.PluginMenuItem.Header -> PluginHeaderItem(item.name) {
                            runCatching { onReloadPlugin(item.pluginId) }.onFailure { e ->
                                LogUtils.e("ChatSettingLoader", "PluginHeaderItem onClick failed: ${e.message}")
                                Toasts.toast("重载失败: ${e.message}")
                            }
                        }

                        is ChatSettingLoader.PluginMenuItem.Action -> PluginActionItem(item.name) {
                            runCatching {
                                item.onClick()
                                onDismiss()
                            }.onFailure { e ->
                                LogUtils.e("ChatSettingLoader", "PluginActionItem onClick failed: ${e.message}")
                                Toasts.toast("执行失败: ${e.message}")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PluginHeaderItem(name: String, onClick: () -> Unit) {
    Text(
        name,
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(12.dp, 14.dp),
        AccentGreen,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun PluginActionItem(name: String, onClick: () -> Unit) {
    val colors = QEdgeTheme.colors
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.cardBackground)
            .clickable(onClick = onClick)
            .padding(Dimens.CardCornerRadius, 18.dp), Alignment.Center
    ) {
        Text(name, fontSize = 15.sp, color = colors.textPrimary)
    }
}

@Composable
private fun GroupSwitchContent(
    peerUin: String,
    isGroup: Boolean,
    features: List<ColdRainConfig.FeatureItem>,
    onDismiss: () -> Unit
) {
    val colors = QEdgeTheme.colors
    val coldRain = ColdRainCore.getInstance()
    val label = if (isGroup) "本群" else "好友"

    val switchStates = remember {
        mutableStateMapOf<String, Boolean>().also { map ->
            features.forEach { feature ->
                map[feature.key] = coldRain.isGroupFeatureEnabled(feature.key, peerUin)
            }
        }
    }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(Dimens.PaddingLarge)
    ) {
        Text(
            "功能开关设置",
            Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            colors.textPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(Dimens.PaddingSmall))
        Text(
            "共 ${features.size} 个功能",
            Modifier.fillMaxWidth(),
            colors.textSecondary,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))
        HorizontalDivider(color = colors.textSecondary.copy(0.1f))
        Spacer(Modifier.height(20.dp))

        if (features.isEmpty()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 40.dp), Alignment.Center
            ) {
                Text("暂无可设置的功能", fontSize = 14.sp, color = colors.textSecondary)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(features, key = { it.key }) { feature ->
                    val enabled = switchStates[feature.key] ?: false
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.cardBackground)
                            .padding(Dimens.CardCornerRadius, 12.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = feature.name,
                                color = colors.textPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = feature.description,
                                color = colors.textSecondary,
                                fontSize = 12.sp
                            )
                        }
                        Switch(
                            checked = enabled,
                            onCheckedChange = { checked ->
                                switchStates[feature.key] = checked
                                runCatching {
                                    coldRain.setGroupFeatureEnabled(feature.key, peerUin, checked)
                                }.onFailure { e ->
                                    LogUtils.e(
                                        "ChatSettingLoader",
                                        "setGroupFeatureEnabled ${feature.key} failed: ${e.message}"
                                    )
                                    Toasts.toast("设置失败: ${e.message}")
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}