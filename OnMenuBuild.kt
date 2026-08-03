package me.lengyu.qedge.hook.api

import com.tencent.mobileqq.aio.msg.AIOMsgItem
import com.tencent.qqnt.aio.menu.ui.QQCustomMenuExpandableLayout
import com.tencent.qqnt.kernel.nativeinterface.MsgRecord
import me.lengyu.qedge.R
import me.lengyu.qedge.hook.annotation.HookItemAnnotation
import me.lengyu.qedge.hook.base.BaseApiHookItem
import me.lengyu.qedge.hook.base.Listener
import me.lengyu.qedge.plugin.bean.MsgData
import me.lengyu.qedge.utils.QQCurrentEnv
import me.lengyu.qedge.utils.dexkit.DexKitTask
import me.lengyu.qedge.utils.hook.hookBefore
import me.lengyu.qedge.utils.LogUtils
import me.lengyu.qedge.utils.reflect.findMethod
import me.lengyu.qedge.utils.reflect.setObjectByType
import me.lengyu.qedge.utils.reflect.newInstanceWithArgs
import me.lengyu.qedge.utils.reflect.getObjectByType
import me.lengyu.qedge.utils.reflect.getObjectByTypeOrNull
import org.luckypray.dexkit.query.FindClass
import org.luckypray.dexkit.query.base.BaseQuery

@HookItemAnnotation(value = "监听消息菜单", category = "api")
object OnMenuBuild : BaseApiHookItem<OnMenuBuild.MenuClickListener>(), DexKitTask {

    private const val PREFIX = "[QEdge]"
    private const val MENU_TYPE = "CopyMenuItem"

    override fun loadHook() {
        val itemClass = requireClass(MENU_TYPE)
        val itemSuperClass = itemClass.superclass!!

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
                    e.printStackTrace()
                }
            }

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

                    val labelText = label.split(",")[2]
                    val itemSize = (80 * context.resources.displayMetrics.density + 0.5f).toInt()
                    val iconSize = (20 * context.resources.displayMetrics.density + 0.5f).toInt()
                    val padding = (8 * context.resources.displayMetrics.density + 0.5f).toInt()
                    val textMarginTop = (4 * context.resources.displayMetrics.density + 0.5f).toInt()

                    val layout = android.widget.LinearLayout(context).apply {
                        orientation = android.widget.LinearLayout.VERTICAL
                        gravity = android.view.Gravity.CENTER
                        layoutParams = android.view.ViewGroup.LayoutParams(itemSize, itemSize)
                        setPadding(padding, padding, padding, padding)
                    }

                    val icon = android.widget.ImageView(context).apply {
                        setImageResource(R.drawable.ic_launcher)
                        layoutParams = android.widget.LinearLayout.LayoutParams(iconSize, iconSize)
                    }

                    val textView = android.widget.TextView(context).apply {
                        text = labelText
                        textSize = 12f
                        setTextColor(android.graphics.Color.WHITE)
                        gravity = android.view.Gravity.CENTER
                        layoutParams = android.widget.LinearLayout.LayoutParams(
                            android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                        ).apply {
                            topMargin = textMarginTop
                        }
                    }

                    layout.addView(icon)
                    layout.addView(textView)

                    layout.setOnClickListener {
                        try {
                            listenerSet.single { it.menuKey == label }.onClick(MsgData(msgRecord))
                        } catch (t: Throwable) {
                            LogUtils.e("OnMenuBuild", "onClick error: " + t.message)
                        } finally {
                            try {
                                view.dismiss()
                            } catch (_: Throwable) {}
                        }
                    }

                    param.result = layout
                }
            } catch (e: Throwable) {
                LogUtils.e("OnMenuBuild", "View hook error: " + e.message)
                e.printStackTrace()
            }
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
            e.printStackTrace()
        }
    }

    private fun isModuleItem(item: Any, key: String): Boolean {
        return item.getObjectByTypeOrNull<String>() == key
    }

    @JvmStatic
    fun addMenuListener(menuKey: String, callback: (MsgData, String) -> Unit) {
        addListener(object : MenuClickListener {
            override val menuKey = menuKey
            override fun onClick(msgData: MsgData) {
                callback(msgData, menuKey)
            }
        })
    }

    @JvmStatic
    fun removeMenuListenersForItem(predicate: (String) -> Boolean) {
        listenerSet.removeIf { predicate(it.menuKey) }
    }

    override fun getQueryMap(): Map<String, BaseQuery> = mapOf(
        MENU_TYPE to FindClass().apply {
            searchPackages("com.tencent.qqnt.aio.menu")
            matcher {
                usingStrings(MENU_TYPE)
            }
        }
    )

    interface MenuClickListener : Listener {
        val menuKey: String
        fun onClick(msgData: MsgData)
    }
}