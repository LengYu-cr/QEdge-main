package me.lengyu.qedge.hook.item

import me.lengyu.qedge.hook.annotation.HookItemAnnotation
import me.lengyu.qedge.hook.base.BaseSwitchHookItem
import me.lengyu.qedge.utils.HookUtils
import me.lengyu.qedge.utils.LogUtils
import me.lengyu.qedge.utils.ModuleConfig
import me.lengyu.qedge.utils.ReflectUtils
import me.lengyu.qedge.utils.dexkit.DexKitTask
import org.luckypray.dexkit.query.FindClass
import org.luckypray.dexkit.query.base.BaseFinder
import java.lang.reflect.Modifier

/**
 * @Author 冷雨
 * @Description QQ 收到别人发的安装包时，会自动把 xxx.apk 改名为 xxx.apk.1。
 * 这个 .1 是 QQ 的接收保护后缀，不是真实扩展名，但 QQ 多处按「最后一个点」取扩展名，
 * 于是把 .apk.1 当成未知文件：聊天页不显示 APK 图标，文件页也没有安装入口。
 *
 * 同一目录已经有同名文件时，QQ 落盘还会按「重名加序号」再插一段带空格的 (n)：
 * xxx.apk.1 已存在时，第二份会存成 xxx.apk (1).1，第三份 xxx.apk (2).1。
 * 这种名字的最后一个点是 (n) 前那个，扩展名被当成 1，同样不是安装包。
 *
 * 处理方式：在扩展名提取处把保护后缀归一掉，并把 (n) 挪到 .apk 之前：
 * - xxx.apk.1        -> xxx.apk
 * - xxx.apk (1).1     -> xxx(1).apk
 * - xxx.apk (1).1.rename -> xxx(1).apk.rename（保留 QQ 的改名保护）
 * 具体入口：
 * - 聊天页：`com.tencent.qqnt.aio.element.file.util` 包下的文件工具类，按扩展名判类型，
 *   归一化它的「文件名 -> 扩展名」入参后类型变为 5（APK）。该类被混淆（QQ 9.3.70 为 a），
 *   不硬编码类名，由 DexKit 按包名 + 类内字符串特征定位。
 * - 文件管理器：扩展名出口 IFileUtil.getExtension() -> Q()，归一化后 nFileType 变为 5（APK）。
 */
@HookItemAnnotation(
    value = "APK.1 识别为安装包",
    category = "item",
    tag = "APK.1 识别为安装包",
    desc = "QQ 收到 APK 时会自动改名为 xxx.apk.1（本地重名落盘为 xxx.apk (1).1），开启后仍按安装包显示并可直接安装",
    process = "Main"
)
object Apk1AsApk : BaseSwitchHookItem(), DexKitTask {

    private const val TAG = "Apk1AsApk"

    private const val KEY_ENABLE = "apk1_as_apk"
    private const val KEY_AIO_FILE_UTIL = "aio_file_util"

    private const val AIO_FILE_UTIL_PACKAGE = "com.tencent.qqnt.aio.element.file.util"

    /**
     * 类内字符串特征：QQ 的名称保护逻辑用这几个字面量拼 `.apk` / `.rename`，
     * 用于把目标类从包内其它类中区分出来（类名与方法名都被混淆，不能硬编码）。
     */
    private const val TOKEN_SUFFIX_APK = ".apk.rename"
    private const val TOKEN_SUFFIX_NUM = ".rename"

    private const val FILE_UTIL_IMPL_CLASS = "com.tencent.mobileqq.filemanager.api.impl.FileUtilImpl"
    private const val FILE_UTILS_CLASS = "com.tencent.mobileqq.utils.FileUtils"

    /**
     * 分组：1 = .apk 之前的主体，2 = 重名序号 (n)（可无、可多段，QQ 落盘时会在前面补一个空格），
     * 3 = QQ 的 .1 保护后缀，4 = .rename（可无）
     * 例：xxx.apk.1 / xxx.apk.12 / xxx.apk(1).1 / xxx.apk (1).1 / xxx.apk (2).1.rename
     */
    private val APK_PROTECTED_SUFFIX = Regex("(?i)^(.*)\\.apk((?:\\s*\\(\\d+\\))*)(\\.\\d+)(\\.rename)?$")

    private fun isEnabled() = ModuleConfig.getBoolean(KEY_ENABLE, false)

    override fun getQueryMap(): Map<String, BaseFinder> = mapOf(
        KEY_AIO_FILE_UTIL to FindClass().apply {
            searchPackages(AIO_FILE_UTIL_PACKAGE)
            matcher {
                usingStrings(TOKEN_SUFFIX_APK, TOKEN_SUFFIX_NUM, "[0-9]*")
            }
        }
    )

    /**
     * 钩子无条件安装，开关在回调里判断，改动即时生效。
     */
    override fun onHook() {
        hookAioFileUtil()
        hookGetExtension(FILE_UTIL_IMPL_CLASS)
        hookGetExtension(FILE_UTILS_CLASS)
    }

    /**
     * 聊天页类型来源：文件工具类里「文件名 -> 扩展名」的静态方法，其返回值再被用于查类型表。
     * 类名/方法名都被混淆，这里只按包名 + 字符串特征由 DexKit 定位类，再按签名取方法，
     * 定位失败只跳过，不回退任何硬编码类名。
     */
    private fun hookAioFileUtil() {
        val clazz = try {
            requireClass(KEY_AIO_FILE_UTIL)
        } catch (e: Throwable) {
            LogUtils.e(TAG, "aio file util class not resolved by DexKit: ${e.message}")
            return
        }
        val method = clazz.declaredMethods.firstOrNull {
            !it.isBridge &&
                Modifier.isStatic(it.modifiers) &&
                it.returnType == String::class.java &&
                it.parameterTypes.size == 1 &&
                it.parameterTypes[0] == String::class.java
        }
        if (method == null) {
            LogUtils.e(TAG, "aio file util extension method not found in ${clazz.name}")
            return
        }
        HookUtils.hookBefore(method) { param ->
            if (!isEnabled()) return@hookBefore
            val name = param.args[0] as? String ?: return@hookBefore
            param.args[0] = normalize(name)
        }
    }

    /**
     * 文件管理器取扩展名的两个稳定入口：FileUtilImpl.getExtension()（文件列表/下载都用它）
     * 与 FileUtils.getExtension()（其它业务也在用）。两者都是未被混淆的公开类，按类名 + 方法名即可。
     * 归一化后 QQFileManagerUtilImpl.Q() 返回 5，FileManagerEntity.nFileType 同步变为 5。
     */
    private fun hookGetExtension(className: String) {
        val clazz = ReflectUtils.findClassIfExists(className) ?: run {
            LogUtils.e(TAG, "$className not found")
            return
        }
        val method = clazz.declaredMethods.firstOrNull {
            it.name == "getExtension" &&
                it.returnType == String::class.java &&
                it.parameterTypes.size == 1 &&
                it.parameterTypes[0] == String::class.java
        }
        if (method == null) {
            LogUtils.e(TAG, "$className#getExtension not found")
            return
        }
        HookUtils.hookAfter(method) { param ->
            if (!isEnabled()) return@hookAfter
            val name = param.args[0] as? String ?: return@hookAfter
            val match = APK_PROTECTED_SUFFIX.matchEntire(name) ?: return@hookAfter
            // 与 QQ 自身逻辑保持一致：带 .rename 的仍返回 .apk.rename，保留改名保护
            param.result = if (match.groupValues[4].isEmpty()) ".apk" else ".apk.rename"
        }
    }

    private fun normalize(name: String): String {
        val match = APK_PROTECTED_SUFFIX.matchEntire(name) ?: return name
        val base = match.groupValues[1]
        // QQ 落盘的重名序号带前导空格（xxx.apk (1).1），归一化时去掉空格
        val index = match.groupValues[2].filterNot(Char::isWhitespace)
        val rename = match.groupValues[4]
        // xxx.apk.1     -> xxx.apk
        // xxx.apk (1).1 -> xxx(1).apk（把重名序号挪到 .apk 之前，否则装不上）
        return base + index + ".apk" + rename
    }
}
