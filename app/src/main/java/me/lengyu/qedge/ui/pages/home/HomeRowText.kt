package me.lengyu.qedge.ui.pages.home

/**
 * 首页文案注解：[title] 为行标题，[subtitle] 为静态副标题。
 * 副标题需要在运行时动态拼接时，调用处传 subtitle 覆盖注解里的静态文案。
 * [card] 标记所属卡片的 key，搜索关键词由注解自动聚合，无需另写关键词表；
 * [cardHeader] 标记该项是卡片标题行（其 title + subtitle 即卡片自身的可搜索文本）。
 */
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
internal annotation class RowText(
    val title: String,
    val subtitle: String = "",
    val card: String = "",
    val cardHeader: Boolean = false
)

/**
 * 首页全部功能行的文案：卡片标题行与功能行的标题、副标题都集中在这里，是展示与搜索的唯一来源。
 * 字段值即查找注解用的 key，调用处只引用这里的常量。
 */
internal object HomeRowText {

    // ==================== 电脑代挂 ====================

    @field:RowText(title = "电脑代挂", subtitle = "远端挂机 在线", card = "hangup", cardHeader = true)
    val CARD_HANGUP = "电脑代挂"

    // ==================== QQ空间 ====================

    @field:RowText(title = "QQ空间", subtitle = "自动点赞、自动评论", card = "card_qzone", cardHeader = true)
    val CARD_QZONE = "QQ空间"

    @field:RowText(title = "空间秒赞", subtitle = "收到好友动态自动点赞(确保在前台运行)", card = "card_qzone")
    val QZONE_AUTO_LIKE = "空间秒赞"

    @field:RowText(title = "空间秒评", subtitle = "自动评论好友动态，点击修改评论内容", card = "card_qzone")
    val QZONE_AUTO_COMMENT = "空间秒评"

    @field:RowText(title = "定时发说说 +0.5天", subtitle = "定时自动发布说说，点击配置", card = "card_qzone")
    val MOOD_SCHEDULE = "定时发说说 +0.5天"

    // ==================== 聊天功能 ====================

    @field:RowText(title = "聊天功能", subtitle = "闪照破解、视频转泡泡等", card = "card_chat", cardHeader = true)
    val CARD_CHAT = "聊天功能"

    @field:RowText(title = "闪照破解", subtitle = "闪照直接查看，无需长按", card = "card_chat")
    val FLASH_PIC_BYPASS = "闪照破解"

    @field:RowText(title = "表情/泡泡/视频/语音下载", subtitle = "保存至 Download/QQ/QEdge/，点击复制", card = "card_chat")
    val EMOTION_DOWNLOAD = "表情/泡泡/视频/语音下载"

    @field:RowText(title = "屏蔽链接信息卡片", subtitle = "收到链接时，自动屏蔽", card = "card_chat")
    val REMOVE_LINK_INFO = "屏蔽链接信息卡片"

    @field:RowText(title = "视频转泡泡消息", subtitle = "发送视频时，自动替换为泡泡", card = "card_chat")
    val VIDEO_TO_BUBBLE = "视频转泡泡消息"

    @field:RowText(title = "取消拍一拍时间限制", subtitle = "解除拍一拍时间限制", card = "card_chat")
    val ANTI_POKE_DELAY = "取消拍一拍时间限制"

    @field:RowText(title = "防撤回", subtitle = "拦截QQ消息撤回，已撤回的消息依然可见", card = "card_chat")
    val PREVENT_RECALL = "防撤回"

    @field:RowText(title = "复制卡片消息", subtitle = "在卡片上方显示长按复制按钮，长按复制JSON", card = "card_chat")
    val COPY_ARK_MESSAGE = "复制卡片消息"

    @field:RowText(title = "长按发送发卡片", subtitle = "长按发送按钮将输入框内JSON作为卡片消息发送", card = "card_chat")
    val LONG_CLICK_SEND_CARD = "长按发送发卡片"

    @field:RowText(title = "消息复读", subtitle = "点击复读，长按可复制链接、查看原始消息", card = "card_chat")
    val REPEAT_MSG = "消息复读"

    @field:RowText(title = "AI表情标签", subtitle = "发送纯表情包时自动带上AI表情标签", card = "card_chat")
    val EMOTION_AI_TAG = "AI表情标签"

    @field:RowText(title = "篡改发送图片比例", subtitle = "修改发送图片的宽高比例，点击配置", card = "card_chat")
    val IMAGE_RATIO = "篡改发送图片比例"

    @field:RowText(title = "语音消息倍速播放", subtitle = "修改语音消息播放倍速，点击修改", card = "card_chat")
    val VOICE_SPEED = "语音消息倍速播放"

    @field:RowText(title = "语音强制免提", subtitle = "语音消息强制扬声器播放，不走听筒", card = "card_chat")
    val FORCE_SPEAKER = "语音强制免提"

    @field:RowText(title = "图片外显自定义", subtitle = "自定义图片外显文案或接口，点击配置", card = "card_chat")
    val IMAGE_SUMMARY = "图片外显自定义"

    @field:RowText(title = "TIM卡片阻断绕过", subtitle = "解除低版本TIM对Ark卡片跳转的限制", card = "card_chat")
    val TIM_ARK_CARD_BYPASS = "TIM卡片阻断绕过"

    @field:RowText(title = "聊天页脚本菜单入口", subtitle = "长按聊天页对应按钮打开脚本菜单（重启QQ生效），点击更改", card = "card_chat")
    val CHAT_SCRIPT_ENTRY = "聊天页脚本菜单入口"

    @field:RowText(title = "综合面板（表情/语音/视频）", subtitle = "打开后长按聊天页对应按钮打开综合面板", card = "card_chat")
    val MEDIA_PANEL = "综合面板（表情/语音/视频）"

    @field:RowText(title = "综合面板入口", subtitle = "长按聊天页对应按钮打开综合面板，点击更改", card = "card_chat")
    val MEDIA_PANEL_ENTRY = "综合面板入口"

    @field:RowText(title = "解除输入框字数上限", subtitle = "解除聊天输入框字数限制，可输入超长文本（重启QQ生效）", card = "card_chat")
    val FORCE_INPUT_NO_LIMIT = "解除输入框字数上限"

    @field:RowText(title = "强制显示输入框全屏按钮", subtitle = "强制全屏输入按钮始终显示，不随行数隐藏", card = "card_chat")
    val FORCE_FULLSCREEN_BTN = "强制显示输入框全屏按钮"

    // ==================== 资料卡 ====================

    @field:RowText(title = "资料卡", subtitle = "上传透明头像等，名片回赞", card = "card_profile", cardHeader = true)
    val CARD_PROFILE = "资料卡"

    @field:RowText(title = "半透明头像上传", subtitle = "可上传(群)头像、名片等，不用则关", card = "card_profile")
    val TRANSPARENT_AVATAR = "半透明头像上传"

    @field:RowText(title = "名片自动回赞", subtitle = "收到名片点赞自动回赞", card = "card_profile")
    val PROFILE_AUTO_LIKE_BACK = "名片自动回赞"

    // ==================== 等级加速 ====================

    @field:RowText(title = "等级加速", subtitle = "00:00时自动空间打卡，qq日签打卡，大会员签到，自动加好友", card = "card_level", cardHeader = true)
    val CARD_LEVEL = "等级加速"

    @field:RowText(title = "空间等级签到", subtitle = "自动执行空间打卡 +0.5天", card = "card_level")
    val QZONE_CHECKIN = "空间等级签到"

    @field:RowText(title = "QQ 日签打卡", subtitle = "自动执行日签打卡 +0.5天", card = "card_level")
    val DAILY_SIGN = "QQ 日签打卡"

    @field:RowText(title = "大会员签到", subtitle = "自动执行（无需开通大会员） +0.5天", card = "card_level")
    val BIG_VIP_CHECKIN = "大会员签到"

    @field:RowText(title = "自动加好友", subtitle = "自动添加3个好友 +1.5天", card = "card_level")
    val AUTO_ADD_FRIEND = "自动加好友"

    @field:RowText(title = "空间浏览", subtitle = "浏览好友说说10条 +0.5天", card = "card_level")
    val SPACE_BROWSE = "空间浏览"

    // ==================== 应用保活 ====================

    @field:RowText(title = "应用保活", subtitle = "应用保活，保持进程可见，可能会高耗电", card = "card_keepalive", cardHeader = true)
    val CARD_KEEPALIVE = "应用保活"

    @field:RowText(title = "透明悬浮窗", subtitle = "1x1透明悬浮窗，保持进程可见", card = "card_keepalive")
    val KEEP_ALIVE_PIXEL = "透明悬浮窗"

    @field:RowText(title = "前台通知", subtitle = "高优先级常驻通知，最高保活优先级", card = "card_keepalive")
    val KEEP_ALIVE_FOREGROUND = "前台通知"

    @field:RowText(title = "后台通知", subtitle = "低优先级通知，轻量保活", card = "card_keepalive")
    val KEEP_ALIVE_BACKGROUND = "后台通知"

    // ==================== 基础配置 ====================

    @field:RowText(title = "基础配置", subtitle = "禁用QQ修复补丁、日志上报等系统级功能", card = "card_system", cardHeader = true)
    val CARD_SYSTEM = "基础配置"

    @field:RowText(title = "禁用QQ修复补丁", subtitle = "拦截并禁用QQ的修复补丁机制，从而更稳定地使用QEdge", card = "card_system")
    val ANTI_QFIX_PATCH = "禁用QQ修复补丁"

    @field:RowText(title = "禁用QQ日志上报", subtitle = "拦截SSO上报并禁用QQ日志上传，可防止模块报错数据一并上传被服务器检测", card = "card_system")
    val ANTI_REPORT = "禁用QQ日志上报"

    @field:RowText(
        title = "解锁本地会员",
        subtitle = "强制本地QQ超级会员/VIP/SVIP，目前可用于开启QQ自带的自动语音转文字、解除表情包收藏500的限制、解除语音发送时长限制、解除每日文件上传限制，其他的自己去测试。会员不会在主页显示。",
        card = "card_system"
    )
    val FORCE_VIP = "解锁本地会员"

    @field:RowText(title = "屏蔽QQ秀/AI头像", subtitle = "屏蔽QQ秀与AI头像相关显示", card = "card_system")
    val DISABLE_AI_AVATAR = "屏蔽QQ秀/AI头像"

    @field:RowText(title = "解除风险网页拦截", subtitle = "点击消息中链接时不再拦截风险网页", card = "card_system")
    val REMOVE_RISK_WEBPAGE = "解除风险网页拦截"

    @field:RowText(title = "拦截网页安全检测", subtitle = "阻止WebView截图上传识别，跳过网页安全OCR检测", card = "card_system")
    val DISABLE_WEB_SECURITY_CHECK = "拦截网页安全检测"

    @field:RowText(title = "浏览器JS接口放行", subtitle = "允许自定义域名使用JS调用QQ的mqq和内部接口，点击配置", card = "card_system")
    val WEB_JS_ALLOWLIST = "浏览器JS接口放行"

    @field:RowText(title = "拦截安全校验(谨慎开启)", subtitle = "屏蔽重打包检测、签名校验与APK版本读取（可能导致其他功能异常）", card = "card_system")
    val DISABLE_SEC_CHECK = "拦截安全校验(谨慎开启)"

    @field:RowText(title = "解除扫码限制", subtitle = "解除长按识别或从相册中扫描二维码时的风险检查", card = "card_system")
    val REMOVE_QRCODE_CHECK = "解除扫码限制"

    @field:RowText(title = "跳过扫码确认等待时间", subtitle = "忽略倒计时，扫码确认按钮可直接点击确认登录", card = "card_system")
    val SKIP_SCAN_WAIT = "跳过扫码确认等待时间"

    @field:RowText(title = "分屏允许扫码", subtitle = "分屏/小窗下也允许打开扫一扫，绕过QQ的多窗口限制", card = "card_system")
    val SPLIT_SCREEN_SCAN = "分屏允许扫码"

    @field:RowText(title = "绕过资料卡封禁", subtitle = "强制显示被封禁用户的 QQ 资料卡主页，绕过封禁拦截弹窗", card = "card_system")
    val BYPASS_PROFILE_BAN = "绕过资料卡封禁"

    @field:RowText(title = "去页面内横幅广告", subtitle = "清理QQ主界面顶部横幅广告等广告数据源", card = "card_system")
    val REMOVE_ADS = "去页面内横幅广告"

    @field:RowText(title = "QLog日志重定向/拦截", subtitle = "QQ日志拦截或重定向，点击选择模式", card = "card_system")
    val QLOG_REDIRECT = "QLog日志重定向/拦截"

    @field:RowText(title = "强制模块Toast", subtitle = "接管QQ原生Toast，改用模块样式弹出提示", card = "card_system")
    val FORCE_MODULE_TOAST = "强制模块Toast"

    @field:RowText(title = "自定义背景图", subtitle = "开启后跳转相册选图作为背景，未选图时使用默认明暗主题", card = "card_system")
    val BG_IMAGE = "自定义背景图"

    // ==================== 模块配置 ====================

    @field:RowText(title = "模块配置", subtitle = "DexKit 缓存状态检查与重建", card = "card_module_config", cardHeader = true)
    val CARD_MODULE_CONFIG = "模块配置"

    @field:RowText(title = "重建 DexKit 缓存", subtitle = "清除当前缓存并重新扫描宿主方法，缺失项会列在上方", card = "card_module_config")
    val REBUILD_DEXKIT_CACHE = "重建 DexKit 缓存"
}

/** 全部注解：key -> 注解，lazy 扫描 [HomeRowText] 的字段构建一次 */
private val homeRowAnnotations: List<RowText> by lazy {
    HomeRowText::class.java.declaredFields.mapNotNull { it.getAnnotation(RowText::class.java) }
}

/** 行文案索引：title -> 注解 */
private val homeRowTexts: Map<String, RowText> by lazy {
    homeRowAnnotations.associateBy { it.title }
}

/** 卡片索引：card key -> 该卡的注解（含卡片标题行） */
private val homeRowTextsByCard: Map<String, List<RowText>> by lazy {
    homeRowAnnotations.filter { it.card.isNotEmpty() }.groupBy { it.card }
}

/** 行标题：从注解读取，未登记时退回 key 本身 */
internal fun homeRowTitle(key: String): String = homeRowTexts[key]?.title ?: key

/** 行副标题：调用处传了 subtitle 则以调用处为准（动态副标题），否则用注解里的静态文案 */
internal fun homeRowSubtitle(key: String, subtitle: String? = null): String =
    subtitle ?: homeRowTexts[key]?.subtitle ?: ""

/** 卡片自身的可搜索文本：卡片标题 + 副标题（行过滤时命中则整卡所有行都显示） */
internal fun homeCardSearchText(key: String): String =
    homeRowTextsByCard[key].orEmpty()
        .firstOrNull { it.cardHeader }
        ?.let { "${it.title} ${it.subtitle}".trim() }
        .orEmpty()

/** 卡片内全部功能行标题（不含卡片标题行），用于判定整卡是否显示 */
internal fun homeCardRowTitles(key: String): List<String> =
    homeRowTextsByCard[key].orEmpty().filterNot { it.cardHeader }.map { it.title }

/** 含功能行的全部卡片 key */
internal fun homeCardKeys(): Set<String> = homeRowTextsByCard.keys