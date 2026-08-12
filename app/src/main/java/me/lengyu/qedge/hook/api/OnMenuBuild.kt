package me.lengyu.qedge.hook.api

import com.tencent.mobileqq.aio.msg.AIOMsgItem
import com.tencent.qqnt.aio.menu.ui.QQCustomMenuExpandableLayout
import com.tencent.qqnt.kernel.nativeinterface.MsgRecord
import me.lengyu.qedge.R
import me.lengyu.qedge.hook.annotation.HookItemAnnotation
import me.lengyu.qedge.hook.base.BaseApiHookItem
import me.lengyu.qedge.hook.base.Listener
import me.lengyu.qedge.plugin.bean.MsgData
import me.lengyu.qedge.utils.HostInfo
import me.lengyu.qedge.utils.QQCurrentEnv
import me.lengyu.qedge.utils.dexkit.DexKitTask
import me.lengyu.qedge.utils.hook.hookBefore
import me.lengyu.qedge.utils.hook.hookReplace
import me.lengyu.qedge.utils.hook.invokeOriginal
import me.lengyu.qedge.utils.LogUtils
import me.lengyu.qedge.utils.reflect.findMethod
import me.lengyu.qedge.utils.reflect.findMethodOrNull
import me.lengyu.qedge.utils.reflect.findMethods
import me.lengyu.qedge.utils.reflect.setObjectByType
import me.lengyu.qedge.utils.reflect.newInstanceWithArgs
import me.lengyu.qedge.utils.reflect.getObjectByType
import me.lengyu.qedge.utils.reflect.getObjectByTypeOrNull
import org.luckypray.dexkit.query.FindClass
import org.luckypray.dexkit.query.base.BaseFinder
import java.lang.reflect.Method

@HookItemAnnotation(value = "监听消息菜单", category = "api")
object OnMenuBuild : BaseApiHookItem<OnMenuBuild.MenuClickListener>(), DexKitTask {

    private const val PREFIX = "[QEdge]"
    private const val MENU_TYPE = "CopyMenuItem"
    private const val FALLBACK_LABEL = "功能"

    override fun loadHook() {
        val itemClass = requireClass(MENU_TYPE)
        val itemSuperClass = itemClass.superclass!!

        // ========== 公共部分：setMenu 插入菜单项 ==========
        QQCustomMenuExpandableLayout::class.java
            .findMethod {
                name = "setMenu"
            }.hookBefore(this) { param ->
                try {
                    val customMenu = param.args[0]
                    val items = customMenu.getObjectByTypeOrNull<MutableList<Any>>((customMenu as Any).javaClass.superclass)
                    if (items.isNullOrEmpty()) return@hookBefore

                    val aioMsgItem = items[0].getObjectByType<AIOMsgItem>(itemSuperClass)

                    val msgRecord = aioMsgItem.msgRecord
                    val msgType = msgRecord.msgType.toString()
                    forEachChecked { listener ->
                        if (listener.menuKey.isEmpty()) return@forEachChecked
                        if (!listener.isEnabled()) return@forEachChecked
                        if (items.any { isModuleItem(it, listener.menuKey) }) return@forEachChecked

                        val args = listener.menuKey.split(",")
                        if (args.size < 5) return@forEachChecked

                        val targetTypes = args.subList(4, args.size).filter { it.isNotEmpty() }
                        if (targetTypes.isNotEmpty() && !targetTypes.contains(msgType)) {
                            return@forEachChecked
                        }

                        addMenuItem(items, itemClass, listener.menuKey, aioMsgItem)
                    }
                } catch (e: Throwable) {
                    LogUtils.e("OnMenuBuild", "setMenu hook error: " + e.message)
                    LogUtils.e(e)
                }
            }

        // ========== TIM 兜底：找 CopyMenuItem 里的显示名 + icon + 点击方法 Hook（不硬编码方法名，按特征找） ==========
        // 即使 hookReplace View 创建方法没生效，这几个也能把 TIM 原生渲染的名字/icon 改对，且点击能执行我们的 listener
        if (HostInfo.isTIM) {
            // 显示名方法：返回 String，零参数（CopyMenuItem 里有两个，都 Hook 兜底）
            itemClass.findMethods {
                returnType = String::class.java
                paramTypes()
            }.forEach { m ->
                try {
                    m.hookReplace(this) { param ->
                        val raw = param.invokeOriginal() as? String
                        // 只有返回值刚好等于我们存的完整 menuKey 时才替换（过滤掉 e() 那种返回常量 "CopyMenuItem" 的）
                        if (raw == null || !raw.startsWith(PREFIX)) return@hookReplace raw
                        return@hookReplace resolveMenuName(raw)
                    }
                    // LogUtils.d("OnMenuBuild", "[TIM] hookDisplay 成功: ${m.name}()String")
                } catch (t: Throwable) {
                    LogUtils.e("OnMenuBuild", "[TIM] hookDisplay 方法失败: ${m.name} → ${t.message}")
                }
            }
            // icon 方法：返回 int，零参数（CopyMenuItem 里就一个）
            itemClass.findMethods {
                returnType = Int::class.javaPrimitiveType
                paramTypes()
            }.forEach { m ->
                try {
                    m.hookReplace(this) { param ->
                        // 判断 this 上存的 String field 是我们的 menuKey 才替换 icon
                        val label = (param.thisObject as Any).getObjectByTypeOrNull<String>()
                            ?: return@hookReplace param.invokeOriginal()
                        if (!label.startsWith(PREFIX)) return@hookReplace param.invokeOriginal()
                        return@hookReplace R.drawable.ic_launcher
                    }
                    // LogUtils.d("OnMenuBuild", "[TIM] hookIcon 成功: ${m.name}()I")
                } catch (t: Throwable) {
                    LogUtils.e("OnMenuBuild", "[TIM] hookIcon 方法失败: ${m.name} → ${t.message}")
                }
            }
            // 点击事件方法：MCP 反编译确认 CopyMenuItem 没有 implements View.OnClickListener，
            // 执行复制逻辑的是 h()V（零参数 void 方法），里面调 clipboard.setText()
            // 所以按 "零参数 void + 方法体内有 clipboard 调用" 的特征找。
            // 兜底：所有零参数 void 方法都 Hook 一下，判断 label 就拦截
            val allVoidNoArg = itemClass.findMethods { returnType = void; paramTypes() }
            // LogUtils.d("OnMenuBuild", "[TIM] 零参数 void 方法找到 ${allVoidNoArg.size} 个: ${allVoidNoArg.joinToString { it.name + "()" }}")
            allVoidNoArg.forEach { m ->
                try {
                    m.hookReplace(this) { param ->
                        val label = (param.thisObject as Any).getObjectByTypeOrNull<String>()
                            ?: return@hookReplace param.invokeOriginal()
                        if (!label.startsWith(PREFIX)) return@hookReplace param.invokeOriginal()
                        // 是我们的菜单项：不 invokeOriginal（防止走 h()V 的复制逻辑），直接执行我们的 listener
                        val aioMsgItem = runCatching {
                            (param.thisObject as Any).getObjectByType<AIOMsgItem>(itemSuperClass)
                        }.getOrNull() ?: return@hookReplace param.invokeOriginal()
                        val msgRecord = aioMsgItem.msgRecord
                        onMenuClick(label, msgRecord, null)
                        // LogUtils.d("OnMenuBuild", "[TIM] hookClick 触发: ${m.name}() → label=$label")
                    }
                    // LogUtils.d("OnMenuBuild", "[TIM] hookClick 成功: ${m.name}()V")
                } catch (t: Throwable) {
                    LogUtils.e("OnMenuBuild", "[TIM] hookClick 方法失败: ${m.name} → ${t.message}")
                }
            }
        }

        // ========== QQ 原逻辑（精确签名 + hookBefore set param.result）完全不动 ==========
        if (!HostInfo.isTIM) {
            loadHook_QQ(itemClass, itemSuperClass)
        } else {
            loadHook_TIM(itemClass, itemSuperClass)
        }
    }

    // =========================================================
    //                       QQ 版本（原逻辑不动）
    // =========================================================
    private fun loadHook_QQ(itemClass: Class<*>, itemSuperClass: Class<*>) {
        QQCustomMenuExpandableLayout::class.java.findMethod {
            returnType = android.view.View::class.java
            paramTypes(Int::class.java, itemClass, Boolean::class.java, FloatArray::class.java)
        }.hookBefore(this) { param ->
            try {
                val menuItem = param.args[1]
                val label = menuItem.getObjectByTypeOrNull<String>() ?: return@hookBefore

                if (label.startsWith(PREFIX)) {
                    val aioMsgItem = menuItem.getObjectByType<AIOMsgItem>(itemSuperClass)
                    val msgRecord = aioMsgItem.msgRecord

                    param.result = null

                    val view = param.thisObject as QQCustomMenuExpandableLayout
                    val context = QQCurrentEnv.getActivity() ?: QQCurrentEnv.getQQAppInterface().application
                    val labelText = resolveMenuName(label)
                    val (itemSize, iconSize, padding, textMarginTop) = calcViewSizes(context)

                    val layout = buildMenuLayout(context, itemSize, padding)
                    val icon = buildMenuIcon(context, iconSize)
                    val text = buildMenuText(context, labelText, textMarginTop)

                    layout.addView(icon)
                    layout.addView(text)
                    layout.setOnClickListener { onMenuClick(label, msgRecord, view) }

                    param.result = layout
                }
            } catch (e: Throwable) {
                LogUtils.e("OnMenuBuild", "View hook error: " + e.message)
                LogUtils.e(e)
            }
        }
    }

    // =========================================================
    //                       TIM 版本适配
    // =========================================================
    private fun loadHook_TIM(itemClass: Class<*>, itemSuperClass: Class<*>) {
        val menuLayoutClass = QQCustomMenuExpandableLayout::class.java
        val hookTargets = linkedSetOf<Method>()
        // 先试 QQ 的那套精确签名
        runCatching {
            menuLayoutClass.findMethodOrNull {
                returnType = android.view.View::class.java
                paramTypes(Int::class.java, itemClass, Boolean::class.java, FloatArray::class.java)
            }
        }.getOrNull()?.let { hookTargets.add(it) }
        // 扫所有返回 View 且参数里含有 ui/f 类型（或其父/子类）的方法
        for (m in menuLayoutClass.findMethods { returnType = android.view.View::class.java }) {
            val pts = m.parameterTypes
            if (pts.size < 3) continue
            val hasItemArg = pts.any {
                it == itemClass || itemClass.isAssignableFrom(it) || it.isAssignableFrom(itemClass) ||
                it == itemSuperClass || itemSuperClass.isAssignableFrom(it) ||
                (it.superclass?.let { sc -> sc == itemSuperClass || itemClass.isAssignableFrom(sc) } == true)
            }
            if (!hasItemArg) continue
            hookTargets.add(m)
        }
        if (hookTargets.isEmpty()) {
            hookTargets.addAll(menuLayoutClass.findMethods { returnType = android.view.View::class.java })
        }
        for (method in hookTargets) {
            try {
                method.hookReplace(this) { param ->
                    val menuItem = param.args.firstOrNull { arg ->
                        arg != null && (
                            itemClass.isInstance(arg) || itemSuperClass.isInstance(arg) ||
                            itemSuperClass.superclass?.let { sc -> sc.isInstance(arg) } == true
                        )
                    } ?: return@hookReplace param.invokeOriginal()
                    val label = menuItem.getObjectByTypeOrNull<String>()
                        ?: return@hookReplace param.invokeOriginal()
                    if (!label.startsWith(PREFIX)) return@hookReplace param.invokeOriginal()

                    val aioMsgItem = menuItem.getObjectByType<AIOMsgItem>(itemSuperClass)
                    val msgRecord = aioMsgItem.msgRecord
                    val context = QQCurrentEnv.getActivity() ?: QQCurrentEnv.getQQAppInterface().application
                    val labelText = resolveMenuName(label)
                    val (itemSize, iconSize, padding, textMarginTop) = calcViewSizes(context)

                    val layout = buildMenuLayout(context, itemSize, padding)
                    val icon = buildMenuIcon(context, iconSize)
                    val text = buildMenuText(context, labelText, textMarginTop)

                    layout.addView(icon)
                    layout.addView(text)
                    layout.setOnClickListener { onMenuClick(label, msgRecord, param.thisObject as QQCustomMenuExpandableLayout) }

                    return@hookReplace layout
                }
                // LogUtils.d("OnMenuBuild", "[TIM] hookView 成功: ${method.name}(${method.parameterTypes.joinToString(",") { it.simpleName }})${method.returnType.simpleName}")
            } catch (t: Throwable) {
                LogUtils.e("OnMenuBuild", "[TIM] hookView 方法失败: ${method.name} → ${t.message}")
            }
        }
    }

    // =========================================================
    //                       公共方法
    // =========================================================
    private fun resolveMenuName(label: String): String {
        val a = label.split(",")
        return a.getOrNull(2)?.takeIf { it.isNotEmpty() }
            ?: a.getOrNull(1)?.takeIf { it.isNotEmpty() }
            ?: FALLBACK_LABEL
    }

    private fun calcViewSizes(context: android.content.Context): Array<Int> {
        val density = context.resources.displayMetrics.density
        return arrayOf(
            (80 * density + 0.5f).toInt(),   // itemSize
            (20 * density + 0.5f).toInt(),   // iconSize
            (8 * density + 0.5f).toInt(),    // padding
            (4 * density + 0.5f).toInt()     // textMarginTop
        )
    }

    private fun buildMenuLayout(
        context: android.content.Context,
        itemSize: Int,
        padding: Int
    ): android.widget.LinearLayout {
        return android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            layoutParams = android.view.ViewGroup.LayoutParams(itemSize, itemSize)
            setPadding(padding, padding, padding, padding)
        }
    }

    private fun buildMenuIcon(
        context: android.content.Context,
        iconSize: Int
    ): android.widget.ImageView {
        return android.widget.ImageView(context).apply {
            setImageResource(R.drawable.ic_launcher)
            scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
            layoutParams = android.widget.LinearLayout.LayoutParams(iconSize, iconSize)
        }
    }

    private fun buildMenuText(
        context: android.content.Context,
        labelText: String,
        textMarginTop: Int
    ): android.widget.TextView {
        return android.widget.TextView(context).apply {
            text = labelText
            textSize = 12f
            setTextColor(android.graphics.Color.WHITE)
            gravity = android.view.Gravity.CENTER
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = textMarginTop
            }
        }
    }

    private fun onMenuClick(
        label: String,
        msgRecord: MsgRecord,
        expandable: QQCustomMenuExpandableLayout?
    ) {
        try {
            val matches = getListenerSet().filterIsInstance<MenuClickListener>()
                .filter { it.menuKey == label }
            if (matches.isNotEmpty()) matches[0].onClick(MsgData(msgRecord))
        } catch (t: Throwable) {
            LogUtils.e("OnMenuBuild", "onClick error: " + t.message)
        } finally {
            runCatching { expandable?.dismiss() }
        }
    }

    private fun addMenuItem(
        items: MutableList<Any>,
        itemClass: Class<*>,
        key: String,
        aioMsgItem: AIOMsgItem
    ) {
        try {
            val context = QQCurrentEnv.getActivity() ?: QQCurrentEnv.getQQAppInterface().application
            val newItem = itemClass.newInstanceWithArgs(context, aioMsgItem)
            newItem.setObjectByType(key)
            items.add(0, newItem)
        } catch (e: Throwable) {
            LogUtils.e("OnMenuBuild", "addMenuItem error: " + e.message)
            LogUtils.e(e)
        }
    }

    private fun isModuleItem(item: Any, key: String): Boolean {
        return runCatching { item.getObjectByTypeOrNull<String>() == key }.getOrDefault(false)
    }

    @JvmStatic
    fun addMenuListener(
        menuKey: String,
        callback: (MsgData, String) -> Unit
    ) = addMenuListenerInternal(menuKey, { true }, callback)

    @JvmStatic
    fun addMenuListener(
        menuKey: String,
        enabled: () -> Boolean,
        callback: (MsgData, String) -> Unit
    ) = addMenuListenerInternal(menuKey, enabled, callback)

    private fun addMenuListenerInternal(
        menuKey: String,
        enabled: () -> Boolean,
        callback: (MsgData, String) -> Unit
    ) {
        // 先移除同 menuKey 的老 listener，保证幂等
        getListenerSet().removeIf { l -> l.menuKey == menuKey }
        addListener(object : MenuClickListener {
            override val menuKey = menuKey
            override fun isEnabled(): Boolean = runCatching { enabled() }.getOrDefault(false)
            override fun onClick(msgData: MsgData) {
                callback(msgData, menuKey)
            }
        })
    }

    @JvmStatic
    fun removeMenuListenersForItem(predicate: (String) -> Boolean) {
        getListenerSet().removeIf { l -> predicate(l.menuKey) }
    }

    override fun getQueryMap(): Map<String, BaseFinder> = mapOf(
        MENU_TYPE to FindClass().apply {
            searchPackages("com.tencent.qqnt.aio.menu")
            matcher {
                usingStrings(MENU_TYPE)
            }
        }
    )

    interface MenuClickListener : Listener {
        val menuKey: String
        fun isEnabled(): Boolean = true
        fun onClick(msgData: MsgData)
    }
}
