# QEdge Code Wiki

> 基于 **NT QQ / NT TIM** 的 Xposed 增强模块 + 在线脚本平台。模块化设计，所有功能开关独立可控；Hook 全部 O(1)，无遍历/无循环反射/无深度拷贝；配套 PHP 后台支持脚本上传、下载、反馈、赞助墙、用户中心。

---

## 目录

1. [项目整体架构](#1-项目整体架构)
2. [技术栈与依赖](#2-技术栈与依赖)
3. [模块职责](#3-模块职责)
4. [核心流程与初始化](#4-核心流程与初始化)
5. [Hook 系统](#5-hook-系统)
6. [DexKit 动态查找](#6-dexkit-动态查找)
7. [冷雨功能核心](#7-冷雨功能核心)
8. [在线脚本插件系统](#8-在线脚本插件系统)
9. [UI 层架构](#9-ui-层架构)
10. [基础设施层](#10-基础设施层)
11. [QQ 服务接口封装](#11-qq-服务接口封装)
12. [项目构建与运行](#12-项目构建与运行)
13. [第三方 APP Hook](#13-第三方-app-hook)
14. [PHP 后台架构](#14-php-后台架构)
15. [附录：工程约束与规范](#15-附录工程约束与规范)

---

## 1. 项目整体架构

### 1.1 分层架构

```
┌─────────────────────────────────────────────────────────────┐
│                     UI 层 (Jetpack Compose)                   │
│  MainActivity / HomeScreen / ColdRainScreen / FileManager   │
├─────────────────────────────────────────────────────────────┤
│                      功能业务层                               │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────┐   │
│  │ ColdRainCore │  │ PluginManager│  │ QQ/TIM 功能Hook  │   │
│  └──────────────┘  └──────────────┘  └──────────────────┘   │
├─────────────────────────────────────────────────────────────┤
│                     Hook 框架层                              │
│  XposedEntry -> MainHook -> HookRegistry -> BaseHookItem    │
├─────────────────────────────────────────────────────────────┤
│                   DexKit 动态查找层                          │
│  DexKitFinder / DexKitTask / DexKitCache                    │
├─────────────────────────────────────────────────────────────┤
│                     基础设施层                               │
│  ModuleConfig / LogUtils / HttpUtils / QQCurrentEnv / ...   │
├─────────────────────────────────────────────────────────────┤
│              QQ Stub 接口层 (qqinterface)                    │
│  IKernelService / IFriendsInfoService / MsgElement / ...    │
└─────────────────────────────────────────────────────────────┘
```

### 1.2 工程目录结构

```
QEdge/
├── app/                                     # 主模块（Xposed + UI）
│   └── src/main/java/me/lengyu/qedge/
│       ├── coldrain/                        # 冷雨机器人核心（21 功能）
│       │   ├── ColdRainCore.java / ColdRainFeature.java
│       │   └── features/                    # 各功能策略（At/群管/签到/天气/解析/...）
│       ├── hook/                            # Hook 系统
│       │   ├── XposedEntry.java / MainHook.java / UserData.java / HeartbeatManager.java
│       │   ├── annotation/  # HookItemAnnotation / HookCategory
│       │   ├── api/         # 事件分发（OnReceiveMsg/OnSendMsg/OnMenuBuild/...）
│       │   ├── base/        # 基类 + HookRegistry 注册中心
│       │   ├── item/        # 功能项（防撤回/闪照破解/保活/复读/...33 项）
│       │   ├── entry/       # QQPlusInject / QQSettingInject（设置入口劫持）
│       │   ├── kk/          # KK键盘
│       │   ├── kugou/       # 酷狗音乐
│       │   ├── aoruan/      # 傲软抠图
│       │   ├── ifly/        # com.iflytek.inputmethod
│       │   ├── deviceInfoX/ # com.liuzh.deviceinfo
│       │   ├── painlessword/# tech.xiangzi.painless
│       │   └── woodenletter/# com.One.WoodenLetter
│       ├── plugin/                        # 在线脚本引擎（Java(BeanShell) + JS(Rhino) 双引擎）
│       │   ├── PluginManager.java / PluginCompiler.java / PluginCallback.java
│       │   ├── JsRuntime.kt / JsConsole    # JS(Rhino) 运行时
│       │   ├── PluginInfo.java / PluginError.java / FixClassLoader.java
│       │   ├── api/PluginMethod.java       # 暴露给脚本的 180+ 宿主 API
│       │   ├── bean/                       # MsgData/PluginInfo/GroupInfo/MemberInfo/...
│       │   └── view/ChatSettingLoader.kt   # 聊天设置入口 BottomSheet
│       ├── ui/                            # Compose UI
│       │   ├── pages/home/                # 新首页（MainScreen 侧滑栏 + 卡片）
│       │   ├── pages/coldrain/            # 冷雨配置页
│       │   ├── pages/file/                # 文件管理/编辑器/音频/图片
│       │   ├── services/OnlinePluginService.kt  # 在线脚本 HTTP 服务
│       │   └── components/ + core/theme/  # 原子组件/对话框/主题
│       ├── lifecycle/                     # 寄生Activity（Parasitics/DynamicRegistry/CounterfeitFactory）
│       ├── utils/                         # 工具集（配置/日志/网络/反射/DexKit/proto/json/qq）
│       ├── activity/                      # SettingActivity / ThemeHelper
│       ├── common/ModuleScope.kt          # IO 协程调度
│       └── MainActivity.kt / LauncherActivity.kt
│
├── qqinterface/                            # QQ接口stub（compileOnly）
├── libs/libxposed/                         # Xposed API
├── QEdge后台/QEdge/                        # PHP后台
├── bsh/                                    # BeanShell解释器（java 插件）
├── build.gradle.kts
└── gradle/libs.versions.toml
```

---

## 2. 技术栈与依赖

| 项目 | 版本 | 用途 |
|------|------|------|
| compileSdk / targetSdk / minSdk | 37 / 37 / 29 (Android 10) | |
| AGP / Kotlin | 9.1.1 / 2.4.10 | |
| Compose BOM | 2026.06.01 | UI 框架 |
| DexKit | 2.2.0 | 动态查找混淆类/方法 |
| Protobuf JavaLite | 4.35.1 | 协议解析 |
| Rhino | 1.7.15 | JS 插件脚本引擎（ES6，解释执行） |
| Dalvik DX | 16.0.1 | BeanShell 动态编译 dex |
| Xposed API | 82 | Hook 框架（compileOnly） |

**NDK**：`abiFilters = ["arm64-v8a"]`（仅保留 64 位，32 位设备由 `XposedEntry` 拦截并提示）

**资源 ID 隔离**：`--package-id 0x69` 避免与宿主 QQ 资源冲突。

**R8 配置**：`android.enableR8.fullMode=true`，Proguard 保留 `-keep class me.lengyu.qedge.** { *; }` 和 `-dontobfuscate`，禁止 `-dontoptimize`。

---

## 3. 模块职责

### 3.1 app 模块

| 子包 | 职责 | 关键类 |
|------|------|--------|
| `hook/` | Xposed Hook 系统，宿主注入入口 | `XposedEntry`, `MainHook`, `HookRegistry` |
| `coldrain/` | 冷雨 QQ 机器人：消息处理、功能调度、群管 | `ColdRainCore`, `ColdRainFeature` |
| `plugin/` | Java(BeanShell) + JS(Rhino) 双引擎脚本加载/编译/执行 | `PluginManager`, `PluginCompiler`, `JsRuntime`, `PluginCallback` |
| `ui/` | Compose UI：新首页/在线脚本/冷雨配置/文件管理 | `home/MainScreen`, `HomeScreen`, `services/OnlinePluginService` |
| `utils/` | 基础设施：配置、日志、网络、反射、DexKit | `ModuleConfig`, `LogUtils`, `QQCurrentEnv` |
| `lifecycle/` | 寄生 Activity：在 QQ 进程中启动模块界面 | `Parasitics`, `DynamicActivityRegistry`, `CounterfeitActivityInfoFactory` |

### 3.2 qqinterface 模块

QQ/TIM 接口 Stub 层（compileOnly，不打包）：提供 NT 内核接口、消息数据类、QRoute 路由接口的签名，编译时类型检查，运行时通过宿主 ClassLoader 加载真实类。

---

## 4. 核心流程与初始化

### 4.1 Xposed 注入流程

```
XposedEntry.initZygote()           -- 检测 Hook 框架（LSPosed/EdXposed）
    |
XposedEntry.handleLoadPackage()    -- 按包名分流
    |
    ├─ QQ/TIM: hook BaseApplicationImpl.onCreate
    |       ├─ HostInfo.init() + Parasitics.initForStubActivity()
    |       ├─ DexKit 缓存有效? 是 -> MainHook.loadHook()
    |       │                  否 -> DexKitFinder.doFind() -> 显示查找弹窗
    |       └─ MainHook.loadHook():
    |           ├─ registerHookItems()        注册所有 BaseHookItem
    |           ├─ loadApiHook()              加载 API 类 Hook
    |           ├─ initSwitchHookItem()       初始化开关型 Hook
    |           ├─ hookAccountChange()        监听账号切换
    |           └─ Thread[Plugin-AutoLoad]:
    |               2s 延迟 -> loadPluginsIfNeeded() -> ColdRainCore.init()
    |
    ├─ KK 键盘      -> KKHook.loadHook()
    ├─ 酷狗音乐     -> KuGouHook.loadHook()（大字版 / 概念版分别入口）
    ├─ 傲软抠图     -> AoRuanHook.loadHook()
    ├─ 讯飞输入法   -> IFlyHook.loadHook()
    ├─ 设备信息X    -> DeviceInfoXHook.loadHook()
    ├─ 无痛单词     -> PainlessWordHook.loadHook()
    └─ 木函         -> WoodenLetterHook.loadHook()
```

### 4.2 DexKit 首次查找流程

1. Hook `SplashActivity.doOnCreate` 显示 Compose 查找进度弹窗
2. 收集所有 `DexKitTask`，过滤 `.filter { it.isApplicable() }`
3. `DexKitBridge.create(sourceDir)` 加载宿主 APK
4. 逐个执行 `getQueryMap()` -> FindClass / FindMethod
5. 结果存入 `DexKitCache.cacheMap`（key = `TaskTAG->QueryName`）
6. 保存缓存到文件 -> 提示完成 -> 杀进程重启

### 4.3 账号切换流程

1. Hook `QQAppInterface` 消息 Facade 初始化方法
2. 3s 延迟执行 `onAccountChanged()`：
   - `QQCurrentEnv.reset()` 清空缓存
   - `processDataForCurrent("init")` 重初始化 Hook 数据
   - `loadPluginsIfNeeded()` 重载插件
   - `HeartbeatManager.startHeartbeat()` 重启心跳

---

## 5. Hook 系统

### 5.1 入口类

#### [XposedEntry.java](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/hook/XposedEntry.java)

Xposed 模块入口，实现 `IXposedHookLoadPackage` + `IXposedHookZygoteInit`。

| 函数 | 说明 |
|------|------|
| `initZygote(StartupParam)` | 检测 Hook 框架类型，保存 modulePath |
| `handleLoadPackage(LoadPackageParam)` | 按包名分发：QQ/TIM -> 延迟到 Application.onCreate；第三方 APP -> 立即 Hook |
| `hookBaseApplicationOnCreate(classLoader)` | QQ/TIM 主入口，AtomicBoolean 保证一次性初始化 |
| `getModulePathFromClassLoader()` | modulePath 为 null 时从 dexElements 反向查找 .apk 路径 |

支持的宿主：`com.tencent.mobileqq`, `com.tencent.tim`, `im.weshine.keyboard`, `com.iflytek.inputmethod`, `com.kugou.android`, `com.apowersoft.backgrounderaser`, `com.liuzh.deviceinfo`, `tech.xiangzi.painless`, `com.One.WoodenLetter`

#### [MainHook.java](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/hook/MainHook.java)

Hook 加载调度中心。

| 函数 | 说明 |
|------|------|
| `registerHookItems()` | 静态注册 20+ BaseHookItem 到 HookRegistry |
| `loadHook()` | 主入口：检查封禁 -> 注册 -> 加载 API Hook -> 初始化开关 Hook -> 账号切换 -> 插件+冷雨 |
| `loadApiHook()` | 遍历所有 BaseApiHookItem，`isInTargetProcess()` 则 `loadHook()` |
| `initSwitchHookItem()` | 遍历所有 BaseSwitchHookItem，读取配置启用/禁用 |
| `hookAccountChange()` | 多策略监听账号切换（优先特定方法 -> 兜底构造函数） |
| `onAccountChanged()` | 重置 QQ 环境 -> 重初始化数据 -> 重载插件 -> 重启心跳 |

### 5.2 Hook 基类体系

```java
// 所有 Hook 项的根类
public abstract class BaseHookItem {
    protected boolean isEnable = true;
    public boolean isInTargetProcess();  // 根据 HookItemAnnotation 判断当前进程
    public boolean isEnable();
    public void setEnable(boolean enable);
}
```

继承链：

```
BaseHookItem
├── BaseApiHookItem<T extends Listener>     // 观察者模式，事件分发
│   ├── OnReceiveMsg / OnSendMsg            // 收发消息
│   ├── OnMenuBuild / OnPaiYiPai            // 菜单构建 / 拍一拍
│   ├── OnTroopJoin / OnTroopQuit           // 进退群
│   ├── OnTroopShutUp / OnQZonePush         // 禁言 / QZone 推送
│   ├── OnGetRKey                           // RKey 获取
│   └── FromServiceMsgDispatcher            // 服务消息分发
│
├── BaseSwitchHookItem                      // 开关型 Hook（UI 开关控制）
│   ├── FlashPicBypass / DownloadEmotion    // 闪照破解 / 资源下载
│   ├── TransparentAvatar / VideoToBubble   // 透明头像 / 视频转泡泡
│   ├── AntiPokeDelay / TimArkCardBypass    // 取消拍延迟 / TIM 卡片绕过
│   ├── KeepAliveHook / QZoneSchedule       // 保活 / 定时任务
│   ├── RepeatMsg / LevelBoost              // 复读 / 等级加速（自动加好友）
│   ├── PreventRecall / CopyArkMessage      // 防撤回 / 复制卡片
│   ├── LongClickSendCard / AutoLikeBack    // 长按发卡片 / 名片回赞
│   ├── RemoveLinkInfo / QZoneLikeTool      // 屏蔽链接卡片 / QZone 打卡·秒赞·日签等
│   ├── BypassProfileBan / RemoveQrCodeCheck // 绕过资料卡封禁 / 解除扫码限制
│   ├── SkipScanWaitTime / QLogRedirect     // 跳过扫码确认 / QLog 日志处理
│   ├── RemoveAds / RemoveRiskWebpageBlock  // 去横幅广告 / 解除风险网页拦截
│   ├── VoiceSpeed / ImageRatioOverride     // 语音倍速 / 篡改图片比例
│   ├── ImageSummary / EmotionAiTag         // 图片外显自定义 / 表情包 AI 标签
│   └── ...
│
└── BaseClickableHookItem                   // 可点击菜单项
    ├── QQPlusInject                        // QQ+ 注入
    └── QQSettingInject                     // 设置页注入
```

### 5.3 观察者模式（API Hook）

每个 `BaseApiHookItem` 内部维护 `List<T extends Listener>` 监听器：

```java
OnReceiveMsg.INSTANCE.registerListener(msgRecord -> { ... });
```

以 OnReceiveMsg 为例：`loadHook()` 通过 DexKit 三策略查找 `IKernelMsgService` 实现类 -> Hook `onRecvMsg(ArrayList<MsgRecord>)` after -> `notifyListeners(msgRecord)`。

### 5.4 HookRegistry 注册中心

| 方法 | 说明 |
|------|------|
| `register(BaseHookItem)` | 注册单个 Hook 项（按 Class 去重） |
| `getHookItems()` | 返回所有 Hook 项副本 |
| `getHookItemsByClass(Class<T>)` | 按类型过滤（如获取所有 BaseSwitchHookItem） |

### 5.5 核心功能深度解析

#### 5.5.1 防撤回 `PreventRecall`

**Hook 点**：`IQQNTWrapperSession$CppProxy.onMsfPush(cmd, buffer, pushExtraInfo)`

**核心思路**：拦截撤回推送，在协议层修改数据，使 QQ 内核收不到撤回通知，被撤回的消息依然可见。

**两条推送流**：

| 推送 | cmd | 处理方式 |
|------|-----|----------|
| **InfoSyncPush** | `trpc.msg.register_proxy.RegisterProxy.InfoSyncPush` | 从 syncInfoBody 中移除撤回消息条目，内核收不到撤回通知 |
| **MsgPush** | `trpc.msg.olpush.OlPushService.MsgPush` | 将撤回操作中的 msgSeq 改为非法值 `1`，撤回失效 |

**撤回识别**（原始字节操作，不依赖 ProtoBuf 序列化库）：

- 私聊撤回：`msgType=528` + `msgSubType=138`
- 群聊撤回：`msgType=732` + `msgSubType=17`

**撤回元数据存储**：`ConcurrentHashMap<String, RecallMeta>`，key = `peerUin/groupUin_msgSeq`，用于后续 UI 提示。

**已撤回提示**：Hook `AIOBubbleMsgItemVB.handleUIState`，在消息视图刷新时查找 `recallMetaMap` 匹配，添加蓝色（`#12B7F5`、14sp、居中）「消息已撤回」标签。

**线程模型**：`recallExecutor`（单线程守护线程池）处理所有 Proto 解析和字节操作，`Future.get(500ms)` 等待结果。

**AIO 刷新**：双重策略查找 RecyclerView（向上父链遍历深度 50 + Activity decorView 递归搜索），缓存 adapter 引用，撤回时主动 `notifyDataSetChanged`。

#### 5.5.2 复制卡片消息 `CopyArkMessage`

**Hook 点**：`AIOBubbleMsgItemVB.handleUIState`

**功能**：检测消息中的 `ArkElement`，在卡片消息上方添加橙红色「长按复制卡片」按钮（`#69F0AE`、13sp），长按将卡片 JSON 数据（`bytesData` 字段）复制到剪贴板。

**复用处理**：非卡片消息时主动移除旧按钮（`removeView`），处理 ListView/RecyclerView 视图复用场景。

**视图获取**：`e` 字段优先 + `getHostView()` 备选，与防撤回共用 `getVBView()` 策略。

**配置**：`copy_ark_message`，默认关闭，UI 位于「聊天功能」卡片中「防撤回」之后。

#### 5.5.3 长按发送按钮发卡片 `LongClickSendCard`

**功能**：开启后，聊天界面长按发送按钮，将输入框内容作为 Ark 卡片 JSON 发送；非 JSON 内容 Toast 提示「卡片格式错误」；发送成功后自动清空输入框。

**三级 Hook 策略**（策略 2 和 3 同时运行，互不干扰）：

| 策略 | Hook 点 | 说明 |
|------|---------|------|
| 策略 1 | `AIOSendMsgVBDelegate.bindViewAndData` after | 从 binding 字段提取 EditText 和可点击发送按钮 |
| 策略 2 | `AIODefaultInputViewBinder` / `GuildAioDefaultInputViewBinder.bindViewAndData` after | 字段遍历 + `e.e`（EditText）/ `e.l/f/g/h/i/j/k/m/n`（按钮）查找 |
| 策略 3 | `AIODelegate.show` after | 四次延时扫描视图树（300ms/800ms/1500ms/2500ms），从 decorView 递归查找 EditText + 同级/父级可点击按钮 |

**按钮标记**：`setTag("qedge_send_card_tag")` 防止重复设置长按监听器。

**配置**：`long_click_send_card`，默认关闭，UI 位于「聊天功能」卡片中「复制卡片消息」下方。

#### 5.5.4 聊天设置入口劫持 `ChatSettingLoader`

**功能**：将模块的脚本管理、冷雨配置以 BottomSheet 形式注入到聊天页面的 UI 入口。通过长按聊天页特定元素触发。

**7 个可选入口**（HomeScreen 卡片式 radio 单选，4 个一行，选中蓝色高亮加粗，需重启 QQ 生效）：

| 入口 key | 对应 UI 元素 |
|----------|-------------|
| `more_features` | 右下角加号 |
| `chat_settings` | 右上角三条杠 |
| `bubble` | 泡泡（消息拍摄按钮） |
| `emoji` | 表情（表情面板按钮） |
| `camera` | 相机按钮 |
| `album` | 相册（图片选择器按钮） |
| `voice` | 语音（语音面板按钮） |

**弹窗结构**：`LazyColumn` 列表，展示「脚本管理」列表（每个插件一条，含开关 + 长按菜单）和「冷雨配置」入口。

#### 5.5.5 等级加速 & QZone 定时任务 `QZoneSchedule`

**三重触发机制**（所有等级加速功能必须实现）：

1. **加载时触发**：`loadHook` 启动时检查
2. **开关切换触发**：UI 打开开关时立即检查
3. **00:00 每日触发**：主进程 Timer 定时任务

**每日去重**：`isDoneToday(tag)` / `markDoneToday(tag)` 幂等机制，确保同一天只执行一次。

| 功能 | 配置键 | 说明 |
|------|--------|------|
| 空间等级签到 | `qzone_daily_checkin_enabled` | QZone 打卡 |
| QQ 日签打卡 | `qq_daily_sign_enabled` | ti.qq.com 签到 |
| 大会员签到 | `qq_bigvip_checkin_enabled` | QQ 大会员中心 |
| 自动加好友 | `level_boost_enabled` | 等级加速加好友 |
| 空间浏览 | `space_browse_enabled` | 提取最多 15 条好友动态链接，带 cookie 以 800ms 间隔访问 |
| 定时说说 | `qzone_schedule_mood_enabled` | 用户自定义时间（HH:mm）+ 内容 |

#### 5.5.6 保活机制 `KeepAliveHook`

**三种保活策略**（独立开关）：

| 策略 | 配置键 | 说明 |
|------|--------|------|
| 像素悬浮窗 | `keep_alive_pixel` | 1x1 像素 WindowManager 悬浮窗 |
| 前台通知 | `keep_alive_foreground` | 常驻通知栏「QQ 正在后台运行」 |
| 后台通知 | `keep_alive_background` | 常驻通知栏「QEdge 保活服务运行中」 |

**关键约束**：必须使用主线程 Looper 操作 WindowManager；通知每 3 秒重新下发（QQ 主界面 `onResume` 会触发 `NotificationManager.cancelAll()`）。

#### 5.5.7 复读机 `RepeatMsg`

**Hook 点**：AIO 消息气泡视图（与防撤回/复制卡片共用 `getVBView()` 策略）。

**功能**：开启后，在单聊/群聊消息气泡旁注入「复读」入口，点击即可自动复读该条消息（图片/语音/视频等各类 Element 一并转发）。

**配置**：`repeat_msg`，默认关闭，UI 位于「聊天功能」卡片。

#### 5.5.8 绕过资料卡封禁 `BypassProfileBan`

**配置**：`bypass_profile_ban`，默认关闭，UI 位于「资料卡」卡片。

**功能**：强制显示被封禁用户的 QQ 资料卡主页，绕过封禁拦截弹窗；并修复被 ban 账号资料卡按钮/头像变暗、昵称不显示的问题。

**三个 Hook 出口**：

| Hook 点 | 处理 |
|---------|------|
| `ProfileCardForbidAccountHelper.isForbidByAnyType` | HookReplace 强制返回 `false`（匹配任意封禁码） |
| `ProfileCardForbidAccountHelper.isForbidBySpecifyTypes` | 遍历 `declaredMethods` 按方法名 + 返回 boolean 匹配签名（版本差异，绕过具体参数类型），强制返回 `false` |
| `FriendProfileCardActivity.updateForbidState` | HookBefore 重置 `ProfileCardInfo.card.isForbidAccount=false`，避免按钮/头像变暗；记录首个有效本地昵称，经 `updateNameArrayByCard` 兜底回填 `strNick` / `nameArray[0]` / `allInOne.nickname` |

字段重置同时清空 `card.forbidCode`，彻底规避服务端下发封禁标记导致的界面禁用态。

#### 5.5.9 解除扫码限制 `RemoveQrCodeCheck`

**配置**：`remove_qrcode_check`，默认关闭。

**功能**：解除长按识别或从相册扫描二维码时的风险校验。

**Hook 点**：`com.tencent.open.agent.QrAgentLoginManager` 的扫码风险检查方法。

**实现**：方法名混淆，通过反射遍历方法，按返回类型 void + 参数组合定位后，在 HookReplace 中通过 HookUtils 遍历参数改写**首个 boolean 参数**为 false，兼容 `(boolean,String,Bundle)` 与 `(QrAgentLoginManager,boolean,String,Bundle)` 两种签名。

#### 5.5.10 跳过扫码确认等待 `SkipScanWaitTime`

**配置**：`skip_scan_wait_time`，默认关闭。

**功能**：扫码登录确认页忽略倒计时，可直接点击「确认」按钮。

**Hook 点**：`com.tencent.biz.qrcode.activity.QRLoginAuthActivity.doOnCreate`。

**实现**：HookAfter 遍历视图查找 `QUIButton`（直接类型匹配，非反射），通过 `setEnabled(true)` + 重置类型（`setType`/清除倒计时相关状态）立即启用确认按钮，主线程 Handler 操作。

#### 5.5.11 QLog 日志处理 `QLogRedirect`

**配置**：`qlog_redirect_mode`（`off`/`mute`/`redirect`），UI 为弹窗三选一，开关控制启停。

**功能**：Hook `com.tencent.qphone.base.util.QLog` 的日志汇总点，三种模式：

| 模式 | 行为 |
|------|------|
| `off`（关闭） | 完全放行原日志，不影响 QQ |
| `mute`（纯拦截） | 丢弃 QQ 所有日志（logcat/beacon/文件），不写任何本地文件 |
| `redirect`（重定向） | 拦截并丢弃原日志，写入 `QEdge/log/QLog/yyyy-MM-dd_HH.log`（按小时分片） |

写入采用异步队列（`LinkedBlockingQueue` + 单线程消费者），保证 O(1) 不阻塞主线程。

#### 5.5.12 语音倍速 / 图片比例 / 图片外显 / 表情标签

| 功能 | 类 | 配置键 | 说明 |
|------|------|------|------|
| 语音消息倍速播放 | `VoiceSpeed` | `voice_speed_enable` / `voice_speed_value`（默认 1.5） | Hook 底层播放器 `setPlaySpeed`，强制固定倍速，UI 弹窗可自定义 |
| 篡改发送图片比例 | `ImageRatioOverride` | `image_ratio` / `image_ratio_width` / `image_ratio_height` | 对所有发送的图片元素（picElement）强制设置 picWidth/picHeight，UI 弹窗输入宽高 |
| 图片外显自定义 | `ImageSummary` | `image_summary` 相关 | 发送图片时将外显摘要改为随机文案或 HTTP 接口返回内容 |
| 表情包 AI 标签 | `EmotionAiTag` | `emotion_ai_tag` | 发送单图纯表情包（picType=1000）改为 2000 + picSubType=14，带上「AI表情」标签 |

#### 5.5.13 平台级功能（去广告 / 风控 / 会员 / 上报）

| 功能 | 类 | 配置键 | 说明 |
|------|------|------|------|
| 去页面内横幅广告 | `RemoveAds` | `remove_ads` | 拦截 QQ 主界面顶部横幅（LebaPluginBannerView）等广告数据源 |
| 解除风险网页拦截 | `RemoveRiskWebpageBlock` | `remove_risk_webpage` | 点消息链接时不再被 `c.pc.qq.com` 风险页拦截 |
| 解锁本地会员 | `ForceVip` | `force_vip` | 本地强制超级会员/VIP/SVIP，解锁自动语音转文字、表情收藏 500 上限、语音/文件上传限制（主页不显示） |
| 屏蔽 QQ秀/AI头像 | `DisableAIAvatar` | `disable_ai_avatar` | Hook 相关 boolean 判断方法强制返回 false |
| 禁用 QQ 修复补丁 | `AntiQfixPatch` | `anti_qfix_patch` | 拦截并禁用 QQ 的修复补丁机制 |
| 禁用 QQ 日志上报 | `AntiReport` | `anti_report` | 在最终 SSO 发送前拦截，禁用 QQ 日志/上报链路 |

---

## 6. DexKit 动态查找

### 6.1 设计目标

不同版本 QQ/TIM 类名/方法名混淆不同，DexKit 通过字符串特征（类中包含的方法名、字段字符串）定位目标。

### 6.2 核心组件

#### [DexKitTask.kt](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/utils/dexkit/DexKitTask.kt) 接口

```kotlin
interface DexKitTask {
    val TAG: String
    fun getQueryMap(): Map<String, BaseFinder>   // "name" -> FindClass/FindMethod
    fun isApplicable(): Boolean = true           // 宿主适用性（TIM-only 覆写返回 HostInfo.isTIM）
    fun requireClass(name: String): Class<*>     // 缓存获取 Class
    fun requireMethod(name: String): Method      // 缓存获取 Method
}
```

#### [DexKitFinder.kt](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/utils/dexkit/DexKitFinder.kt)

DexKit 查找执行器：IO 协程中按需加载 DexKit → 执行所有 `DexKitTask` 的 FindClass/FindMethod → 写入 `DexKitCache` → 保存缓存 → 加载 Hook。首次运行时由 `SplashActivity.doOnCreate` 显示 Compose 进度弹窗。

#### [DexKitManager.java](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/utils/dexkit/DexKitManager.java)

DexKit 原生库加载管理器：解决 Hook 运行在宿主寄生 ClassLoader 中找不到 `libdexkit.so` 的问题 —— 优先 `loadLibrary`，失败则按模块 APK native 库绝对路径 `System.load()` 回退，并缓存加载状态。

#### [DexKitCache.kt](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/utils/dexkit/DexKitCache.kt)

结果缓存（内存 Map + 磁盘 JSON 文件）。

| 方法 | 说明 |
|------|------|
| `initCache()` / `saveCache()` | 文件加载 / 写入 |
| `validateAllTasks()` | 校验缓存中每个条目是否仍有效 |
| `getDescriptor(key)` | 取 `TaskTAG->name` 的 descriptor 字符串 |
| `getClass(key)` / `getMethod(key)` | descriptor -> Class / Method |

缓存 Key 格式：`"OnReceiveMsg->msgService"` / `"TroopTool->getMemberInfo"`

---

## 7. 冷雨功能核心

### 7.1 架构

[ColdRainCore.java](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/coldrain/ColdRainCore.java) 采用**单例 + 策略模式**，维护 `Map<String, ColdRainFeature>` 功能策略表（20+ 功能），每个功能实现 `shouldHandle(msgData)` + `handle(msgData, core)` 接口。

功能包括：状态查询、菜单、问答、签到、群管、禁言检测、视频/音乐/图片解析、天气、整点报时、头衔、点赞、自动上管、头像菜单、进退群欢迎、黑白名单等。

### 7.2 消息处理流水线

```
收到消息 (OnReceiveMsg)
    |
handleMessage(msgRecord)
    ├─ MsgData(msgRecord) 解析消息结构
    ├─ [Q群管家 token 捕获] type=2 && userUin=2854196310 -> 解析 JSON 卡片 -> 保存 token
    ├─ 特殊命令「开机/关机」-> 仅管理员/自己 -> 设置群开关
    ├─ 特殊命令「开启/关闭XX功能」-> 管理员命令 -> 设置群功能开关
    ├─ 「菜单」命令 -> isMenuRestricted() 检查 -> MenuFeature
    └─ 遍历 features.entrySet()
        ├─ feature.shouldHandle(msgData)?
        ├─ canTriggerFeature(featureKey, msgData)?
        │   ├─ master_enabled?（总开关）
        │   ├─ menu_restricted?（仅管理员/自己）
        │   ├─ group_only?（群聊专属功能在私聊不可用）
        │   ├─ group_master_enabled?（单群开机）
        │   ├─ feature_enabled?（全局开关）
        │   └─ group_feature_enabled?（群独立开关）
        └─ feature.handle(msgData, this) -> break（匹配即停止）
```

### 7.3 权限层级

`isAdminOrSelf(msgData)`：任一满足即通过——自己发送 / uin==自己 / uin==主人 / uin 在全局管理员列表 / 群聊场景 uin 在群管理员列表。

### 7.4 配置管理

文件位置：`<QQCurrentEnv.getLocalPath()>/冷雨Java/config.json`，独立于 ModuleConfig，支持 `checkAndReloadIfModified()` 热加载。

| 配置键 | 说明 |
|--------|------|
| `master_enabled` / `master_uin` | 总开关 / 主人 QQ |
| `menu_restricted` / `menu_name` | 菜单限制 / 触发词 |
| `global_admins` / `group_admins_<群号>` | 管理员（逗号分隔） |
| `group_master_enabled_<群号>` / `group_<featureKey>_<群号>` | 单群开关 |
| `feature_<name>` | 功能全局开关（默认 true） |
| `reply_mode` | 回复模式：text/card/image/forward/markdown/reply/guanjia |
| `guanjia_token_<群号>` | Q群管家会话 token（自动获取） |

### 7.5 回复模式

| mode | 说明 |
|------|------|
| text / card / image | 纯文本 / Ark 卡片 / 文字转图片 |
| forward / markdown | 合并转发卡片 / Markdown 卡片 |
| reply | 有 msgId 则引用回复，否则纯文本 |
| guanjia | 群聊专用：通过 Q群管家问答机制间接回复（需群主/管理员权限） |

回复模板变量：`[at]` / `[qq]` / `[uin]` / `[qun]` / `[time]` / `[Name]`

### 7.6 Q群管家机制

```
发送回复请求
    ├─ 检查：群聊 + 群主/管理员 + pskey/skey 存在
    ├─ guanjiaAddQna() 添加临时问答 (question=随机串, answer=实际内容)
    │   └─ POST web.qun.qq.com 需 Headers: qname-service:976321:131072 + qname-space:Production
    │       Body 字段全部 escapeJson()（处理 "、\、\n、\r、\t）
    ├─ 有 token?
    │   ├─ 否 -> 艾特 Q群管家(2854196310) 发"Come on!" -> 等卡片 -> 保存 token
    │   └─ 是 -> guanjiaTriggerQna() -> 成功时清理临时问答
    │           会话过期(ec=70000) -> 重新艾特管家
    └─ 任何失败 -> 回退为纯文本发送
```

---

## 8. 在线脚本插件系统

> 双引擎架构：**Java（BeanShell）** 与 **JS（Rhino）** 插件并行，两者 API 能力对齐，均暴露同一套 `PluginMethod` 宿主接口。

### 8.1 架构

[PluginManager.java](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/plugin/PluginManager.java) 管理所有插件。

```
<ModuleDataPath>/plugin/
├── <插件目录名>/
│   ├── info.prop       # id, pluginName, versionCode, author, type
│   ├── desc.txt        # 描述文本
│   └── main.java       # Java(BeanShell) 源码 / main.js (Rhino)
```

`info.prop` 中的 **`type`** 字段区分语言：`js`/`javascript` → JS(Rhino)，缺省或其它 → `java`(BeanShell)。

### 8.2 语言类型（`PluginInfo`）

| 类型 | 主脚本 | 引擎 | 说明 |
|------|--------|------|------|
| `java`（默认） | `main.java` | BeanShell（`bsh/`） | 类 Java 语法，`PluginCompiler.start()` 编译执行 |
| `js` | `main.js` | Rhino（`JsRuntime`） | ES6，解释执行，Android 兼容 |

### 8.3 生命周期

| 方法 | 说明 |
|------|------|
| `loadAll()` | 扫描 plugin 目录，构建 `List<PluginInfo>` |
| `startPlugin(PluginInfo)` | 按 `isJs()` 分支：Java → `PluginCompiler.start()`；JS → `JsRuntime.start()` 执行 main.js |
| `stopPlugin(PluginInfo)` | 调用 `unLoadPlugin()` 后销毁对应运行时 |
| `reloadPlugin(PluginInfo)` | stop + start |
| `deletePlugin(PluginInfo)` | stop -> 移除列表 -> 递归删目录 |
| `setAutoLoad(plugin, isAuto)` | 增删 autoLoadList，持久化到 `AutoLoadList.json` |
| `startAutoLoadPlugins()` | 线程遍历未运行的自动加载（间隔 100ms） |
| `initAllPluginForCurrent()` | 全停 -> 全清 -> 重加载 -> 自动启动 |

### 8.4 JS（Rhino）运行时 [JsRuntime.kt](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/plugin/JsRuntime.kt)

- **线程模型**：Rhino 的 Context/Scriptable 不能跨线程共享 —— 每个 JS 插件独占一个单线程 Executor，`start` / 回调 / `loadJs` / `stop` 全部串行执行，天然线程安全。
- **解释执行**：`optimizationLevel = -1`（Android ART 无法运行 Rhino 生成的字节码）+ `languageVersion = VERSION_ES6`。
- **全局裸调**：`PluginMethod` 的全部 public 方法映射为 JS 全局函数（无需 `qe.` 前缀），通过隐藏的 `__api__` 对象（NativeJavaObject）按参数个数 + 运行时类型分派重载。
- **环境变量注入**：`context` / `myUin` / `pluginPath` / `pluginId` / `classLoader`。
- **console**：`console.log/error/warn/info` → 宿主 `LogUtils` + 插件目录 `log.txt`，`error` 额外弹 Toast。
- **loadJs(绝对路径)**：对齐 `loadJava`，把目标 JS 文件加载进同一作用域，`loadedFiles` 去重防循环。
- **约定回调**：脚本内定义同名顶层函数（`onMsg` / `joinGroup` / `quitGroup` / `shutUpGroup` / `getMsg` / `onPaiYiPai` / `unLoadPlugin` 等），由 [PluginCallback.java](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/plugin/PluginCallback.java) 回调触发；无需返回值的走 `callFunctionAsync`，需返回值的走 `callFunctionSync`。

### 8.5 BeanShell 脚本示例（java 型）

```java
log("脚本开始运行...");
qqToast(2, "Hello World!");

addItem("测试菜单", "onTestClick");

void onTestClick(int chatType, String peerUin, String peerName) {
    qqToast(2, "点击了菜单");
}

void unLoadPlugin() {
    qqToast(0, "脚本停止运行");
}
```

### 8.6 JS 脚本示例（js 型）

```javascript
console.log("JS 脚本开始运行...");
qqToast(2, "Hello World!");

function onMsg(msgData) {
    if (msgData.type == 2 && msgData.msg.indexOf("天气") >= 0) {
        sendGroupMsg(msgData.peerUin, "今天天气很好！");
    }
}

function unLoadPlugin() {
    console.log("JS 脚本停止运行");
}
```

---

## 9. UI 层架构

### 9.1 组件分层

```
ui/
├── components/
│   ├── atoms/          QEdgeCard / QEdgeSwitch / Buttons
│   ├── molecules/      QEdgeTopBar / TabItem / EmptyState / AnimatedComponents
│   └── dialogs/        ConfirmDialog / TextDialog / RawTextDialog / UpdateDialog / PluginMenuDialog / WelcomeDialog / RepeatMsgActionDialog / CenterDialogContainerNoButton / ...（含 File/Audio/Image 弹窗）
├── core/
│   ├── theme/          Color.kt (Light/Dark/OLED) / Dimens.kt / Theme.kt
│   └── compatibility/  XposedComposeDialog.kt (宿主内启动 Compose 弹窗基类)
├── services/
│   └── OnlinePluginService.kt              # 在线脚本 HTTP 接口封装
└── pages/
    ├── HomeScreen.kt                       # 寄生 QQ 的 4 Tab 配置页（模块首页/Java脚本/冷雨Java/文件管理）
    ├── PluginData.java                     # 插件 UI 数据模型
    ├── home/                               # 新首页（MainScreen 侧滑栏）
    │   ├── MainScreen.kt / HomeSideRail.kt / HomeContentPanel.kt
    │   ├── HomeDialogs.kt / HomeSupportDialog.kt
    │   └── HomeBatteryState.kt             # 电池状态指示器
    ├── coldrain/                           # 冷雨配置（ColdRainScreen / ColdRainConfig / ColdRainConfigSection）
    └── file/                               # 文件管理（FileListPanel / TextEditor / AudioPlayer / ImagePreview）
```

### 9.2 模块首页（`HomeScreen.kt`）

寄生于 QQ 的配置页顶部为 `QEdgeTopBar`，顶部下方支持 **顶栏下推面板**（四类卡片**互斥**，点同一按钮收起，展开/收起为下推式而非弹窗）：

| 面板值 | 卡片 | 触发按钮 |
|--------|------|----------|
| 0 | 无 | — |
| 1 | 用户信息 `UserInfoCard`（头像/昵称/QQ号/角色权限标签/签名/注册时间/版本） | 头像按钮（更新日志按钮左侧，仅 `selectedTab==0` 显示，圆形 40dp，加载失败/加载中显示 👤 占位，点击跳登录页 `https://v.yuafeng.cn/QEdge/user/`） |
| 2 | 更新日志 `UpdateLogCard`（API 拉取，纵向可滚动） | 更新日志按钮 |
| 3 | 赞助墙 `SponsorCard`（微信赞赏码图片居中） | 赞助按钮 |

头像 URL：`https://q.qlogo.cn/g?b=qq&nk=<uin>&s=100`（`q.qlogo.cn`）。首页功能区按卡片分组：QQ空间(`card_qzone`) → 聊天功能(`card_chat`) → 资料卡(`card_profile`) → 等级加速 → 保活。

### 9.3 主入口

[MainActivity.kt](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/MainActivity.kt) - 独立模块界面，`enableEdgeToEdge()` 沉浸式，启动时自动 `checkUpdate()`。

新首页 [home/MainScreen.kt](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/ui/pages/home/MainScreen.kt)：`HomeScaffold` 侧滑栏页面，主内容 `HomeContentPanel`（Hero：菜单按钮/版本/检查更新/Logo/渐变背景 + Overview：问候语/励志文案/更新状态/英文 footer），左侧 `HomeSideRail` 抽屉经 `drawerOffset/contentOffset/scrimAlpha` 动画控制开合、支持左缘滑动手势，内含文案、`HomeBatteryState` 电池指示器与 `LazyColumn` 操作按钮列表。

---

## 10. 基础设施层

### 10.1 配置存储（禁止 SharedPreferences）

#### [ModuleConfig.kt](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/utils/ModuleConfig.kt)

顶层配置入口（单例 object），底层委托 `JsonConfigUtils`。支持 Boolean/String/Int/Long 类型的 put/get/contains/remove。每个配置名对应一个 `.json` 文件。

冷雨使用独立 `config.json`，不经过 ModuleConfig，原因：多账号路径隔离 + 支持 mtime 热加载。

### 10.2 宿主信息

[HostInfo.kt](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/utils/HostInfo.kt) - 提供 `packageName`、`processName`、`versionCode`、`isQQ`/`isTIM`/`isInHostProcess`、`getHostContext()`、`getModuleDataPath()` 等。

### 10.3 QQ 当前环境

[QQCurrentEnv.java](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/utils/QQCurrentEnv.java)

| 方法 | 说明 |
|------|------|
| `getCurrentUin()` | 当前 QQ 号（多级兜底） |
| `getCookieUin()` | Cookie 格式 UIN，自动补零到 10 位，前缀 "o" |
| `getCurrentUid()` / `getCurrentName()` | 当前 Uid（NT 新标识）/ 昵称 |
| `getQQAppInterface()` | `QQAppInterface` 实例（多级兜底） |
| `getKernelMsgService()` | NT 内核消息服务 |
| `getActivity()` | 当前前台 Activity |
| `getCurrentDir()` / `getLocalPath()` / `getHostPath()` | 目录路径（自动 mkdirs） |
| `reset()` | 账号切换时清空全部缓存 |

### 10.4 日志 & 网络

[LogUtils.java](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/utils/LogUtils.java) - 双输出（Logcat + 磁盘文件 `<CurrentDir>/log/yyyy-MM-dd.log`），线程安全 `synchronized(LOCK)`，格式 `yyyy-MM-dd HH:mm:ss.SSS LEVEL TAG: message`。

HttpUtils.java - GET/POST 封装，支持自定义 Headers，用于 Q群管家 API、签到、空间接口、视频解析、更新检查等。

### 10.5 工具集子包（`utils/`）

| 子包/文件 | 职责 |
|-----------|------|
| `utils/dexkit/` | DexKit 查找与缓存：`DexKitFinder.kt`（执行查找）、`DexKitTask.kt`（任务接口）、`DexKitCache.kt`（结果缓存）、`DexKitManager.java`（native 库加载回退） |
| `utils/json/` | JSON 便捷：`JsonExt.java`（路径取值/`u_` UID 深度查找）、`MessageTool.java`（pbandk 消息反射读写）、`ProtoData.java`（JSON/Protobuf 编解码） |
| `utils/proto/` | 协议发送：`PacketHelper.java`（gzip/WUP/SSOEASY 发包转 JSON）、`packetListener.java` / `protoListener.java` |
| `utils/reflect/` | 反射工具：`ReflectUtils.java` / `ReflectDSL.kt` / `ReflectExtensions.kt` / `ReflectCache.kt` / `ClassUtils.kt` |
| `utils/hook/` | Hook 扩展：`HookExtensions.kt`（Kotlin DSL 包装 XposedBridge）、`HookStatusImpl.java` |
| `utils/qq/` | QQ 能力封装：`QQCurrentEnv.java` / `MsgTool.java` / `TroopTool.kt` / `FriendTool.java` / `CookieTool.java` / `QQServiceHelper.java` / `ExtraTool.java` |
| `ObjectStore.java` | 跨类共享对象/实例的内存存储 |
| `JarLoader.java` / `HybridClassLoader.java` | 宿主/插件 dex 加载 |
| `ModulePathHolder.java` | 模块 APK 路径持有 |
| `Toasts.java` | 统一 Toast 封装 |
| `HookUtils.java` | Hook 通用辅助 |

### 10.6 协程调度

[common/ModuleScope.kt](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/common/ModuleScope.kt) - 全局 IO/主线程协程作用域（`launchIO` 等），插件回调、网络与冷雨功能均在其上异步执行。

### 10.7 设置页与主题

[activity/SettingActivity.kt](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/activity/SettingActivity.kt) - 独立设置界面（QQ+ 入口 / 桌面图标启动时打开）；[activity/ThemeHelper.java](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/activity/ThemeHelper.java) - 主题切换辅助。

---

## 11. QQ 服务接口封装

### 11.1 QQServiceHelper

[QQServiceHelper.java](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/utils/qq/QQServiceHelper.java) - 统一 QQ 服务获取入口，优先直接调用。

| 方法 | 说明 |
|------|------|
| `getApi(Class<T>)` | QRouteApi 接口：优先 `QRoute.api()` -> 兜底反射 |
| `getRuntime(Class<T>)` | getApi 兜底 -> QQAppInterface 中查找匹配返回类型 |
| `getRuntimeService(Class<T>)` | `runtime.getRuntimeService(serviceClass, "")`（直接泛型调用，无反射） |

### 11.2 MsgTool

消息发送工具（`utils/qq/MsgTool.java`）：`sendMsg` / `sendReplyMsg` / `sendPic` / `sendVideo` / `sendPtt` / `sendCard` / `sendMarkDown` / `recallMsg`。

### 11.3 群/好友/Cookie 工具

| 工具 | 主要方法 |
|------|----------|
| TroopTool | `getGroupInfo()` / `getMemberInfo()` / `shutUp()` / `kickMember()` / `setAdmin()` / `setUniqueTitle()` |
| FriendTool | `getUidFromUin()` / `getFriendInfo()` / `likeProfile()` / `addFriend()` / `delFriend()` |
| CookieTool | `getSkey()` / `getPskey(domain)` / `getBkn(skey)` / `getFullCookie(domain)` |

---

## 12. 项目构建与运行

### 12.1 构建命令

```bash
./gradlew :app:assembleDebug              # Debug 构建
./gradlew :app:assembleRelease            # Release 构建（R8 优化 + 签名）
./gradlew installDebugAndRestartQQ        # 安装 Debug 并重启 QQ
```

自定义 Gradle 任务：`killQQ` / `openQQ` / `restartQQ` / `installDebugAndRestartQQ`。

签名配置：`storeFile` 指向 `app/qedge.jks`，口令与别名改从 `local.properties`（已 gitignore）读取 —— `qedge.storePassword` / `qedge.keyAlias` / `qedge.keyPassword`，不再硬编码进 `build.gradle.kts`；缺失时构建直接报错提示。

### 12.2 运行要求

| 项目 | 要求 |
|------|------|
| 模块版本 | 0.2.7（versionCode 27） |
| Android | 9.0 ~ 16 (API 28 ~ 37) |
| Xposed 框架 | LSPosed / LSPatch / FPA / 原子 / 无极（Zygisk 模式） |
| NT QQ | 8.9.58 ~ 9.3.xx |
| NT TIM | 3.9.0 ~ 4.1.0 |
| 作用域 | LSPosed 中勾选：QQ/TIM/KK 键盘/酷狗/傲软/讯飞输入法/设备信息X/无痛单词/木函 |

### 12.3 首次启动

1. 安装 APK -> LSPosed 勾选 QQ/TIM 作用域
2. 重启 QQ -> 首次弹出 DexKit 查找进度对话框
3. 查找完成 -> 点击确定杀进程 -> 再次打开 QQ -> 注入成功
4. QQ 首页下拉 -> QQ Plus -> QEdge 设置入口 / 或桌面 QEdge 图标进入

---

## 13. 第三方 APP Hook

### 13.1 KK 键盘 ([KKHook.java](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/hook/kk/KKHook.java))

解锁 SVIP/VIP（皮肤/字体/表情全可用），关闭所有广告，DexKit 查找退出拦截 + 全局兜底防崩溃。

### 13.2 酷狗音乐 ([KuGouHook.java](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/hook/kugou/KuGouHook.java))

大字版 Hook `KGTinkerApplication.onCreate`，概念版 Hook `KGApplication.onCreate`。跳过所有开屏广告（GdtSplashActivity / AdContainerActivity -> 直接跳 MediaActivity，gotoAd 方法返回跳过）。

### 13.3 傲软抠图 ([AoRuanHook.java](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/hook/aoruan/AoRuanHook.java))

Hook `VipManager` 的 `isVip()`/`isVipValid()`/`isExpire()`/`isVipValidOrBalance()` 全返回 true，`getDeadlineDate()` 改写为长有效期，抠图不限次数。

---

## 14. PHP 后台架构

### 14.1 环境与目录

- PHP 7.3+ / MySQL 5.7+ / MariaDB
- 目录：`admin/`（管理后台）、`user/`（用户中心）、`api/`（REST API，JSON 返回）、`online_plugin/`（脚本平台前端）、`update/`（版本检查）、`heartbeat/`（心跳）、`Secluded/`（第三方 QQ 平台登录，供电脑代挂复用出码/验证接口）

### 14.2 安全特性

| 特性 | 实现 |
|------|------|
| SQL 注入防护 | 全部用户输入 PDO 预处理 |
| XSS 防护 | `htmlspecialchars(ENT_QUOTES, 'UTF-8')` 输出转义 |
| Session 安全 | 私有存储路径 + 7 天有效期 + 滑动窗口续期 |
| 脚本上传校验 | ZIP 内部读取 `info.prop`（id/name/version/author） |
| 版本唯一约束 | 数据库 `idx_plugin_version(plugin_id, version_code)` 唯一索引 |

### 14.3 核心数据表

- `users` - 用户表（含 permission: user/admin）
- `plugins` - 脚本表（plugin_id, plugin_name, version_code, author, desc, download_count, review_status）
- `plugin_reviews` / `feedback` / `sponsors` / `sessions` - 评论/反馈/赞助/Session
- `hangup_users` - 电脑代挂表（`qq` PK、`mid`、`start_time`、`auto_online`、`last_fail`、`created_at`）

### 14.4 电脑代挂（QEdge/user/）

纯服务器端逻辑，复用主系统 `require.php` 的数据库连接与赞助判断（`users.is_sponsor` + `sponsor_users`），不占用手机端性能。文件均嵌于 `QEdge/user/`：

| 文件 | 职责 |
|------|------|
| `hangup.php` | 代挂页面：未登录自动 302 到 `login.php`；登录后经 session `login_qq` 取 QQ 号；二维码框初始 `display:none`，出码成功后 `inline-block`，登录/失败后隐藏 |
| `hangup_common.php` | 公共工具：`ensureHangupTable` 建表、`hangupOnline`/`hangupOffline` 上游上线/下线、`hangupSign` 签名 |
| `hangup_auto_online.php` | 每日 00:00 定时任务：拉起 `auto_online=1` 的账号上线，失败写 `last_fail` |
| `hangup_check.php` | 每 10 分钟定时任务：下线在线超 2 小时的账号（`start_time` 置 NULL 而非删记录，保留次日自动上线） |
| `isOnline.php?uin=` | 供模块/后台查询指定 QQ 是否在线（内部调用上游 `uin-list-get`） |

出码与登录复用 `Secluded/get_qrcode.php` 与 `verify_login.php`。非赞助用户触达时立即调用上游 `set-online online=false` 强制下线并提示「仅赞助用户可用」。

---

## 15. 附录：工程约束与规范

| 约束 | 说明 |
|------|------|
| **禁止 SharedPreferences** | 所有配置使用 JsonConfigUtils / ColdRainCore.config.json 集中式 JSON |
| **禁止 Build.CPU_ABI** | 必须使用 `Build.SUPPORTED_ABIS[0]` 并检查数组长度 |
| **禁止 MODE_MULTI_PROCESS** | 必须使用 MODE_PRIVATE |
| **禁止 -dontoptimize** | Proguard 中不得包含，必须开启 R8 优化 |
| **必须 R8 fullMode** | `android.enableR8.fullMode=true` |
| **必须 -dontobfuscate** | Xposed 模块不得混淆代码 |
| **必须 keep 模块代码** | `-keep class me.lengyu.qedge.** { *; }` |
| **DexKitTask 必须 isApplicable()** | 宿主专属功能覆写，任务列表必须 `.filter { it.isApplicable() }` |
| **HTTP/IO 必须异步** | 所有 Hook 回调 O(1)，耗时操作丢新 Thread |
| **定时任务仅主进程** | `HostInfo.processName == HostInfo.packageName` 判断 |
| **QQServiceHelper 直接泛型调用** | `getRuntimeService` 禁止反射，QRouteApi 必须用 `getApi()` |
| **多账号路径隔离** | 禁止硬编码 `/storage/emulated/0/`，使用 `QQCurrentEnv.getLocalPath()` |
| **菜单限制** | 触发冷雨功能 + 显示菜单都必须通过 `isMenuRestricted()` 检查 |
| **Q群管家** | 需群主/管理员 + 仅群聊(mtype=2) + 首次艾特 Q群管家(2854196310) |
| **LogUtils 异常日志** | 必须重定向到 `QEdge/log/yyyy-MM-dd.log`，禁止 `XposedBridge.log(throwable)` |
| **JS 插件** | Rhino 单线程 Executor、解释执行(opt=-1)+ES6；`console` 走 LogUtils；回调走 `callFunctionAsync/Sync` |
| **KeepAliveHook** | 必须主线程 Looper，通知每 3 秒重新下发（QQ onResume 会 cancelAll） |

### 等级加速三重触发

所有等级加速功能（空间签到/日签/大会员/自动加好友/空间浏览）必须三重触发：

1. **加载时触发**：loadHook 启动时检查
2. **开关切换触发**：UI 打开开关时立即检查
3. **00:00 每日触发**：Timer 定时任务

每日去重：`isDoneToday(tag)` / `markDoneToday(tag)` 幂等机制。