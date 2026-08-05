# QEdge Code Wiki

> 基于 NT QQ / NT TIM 的 Xposed 增强模块 + 在线脚本平台。
> 模块化设计，所有功能开关独立可控；零卡顿设计原则：Hook 全部 O(1)，无遍历 / 无循环反射 / 无深度拷贝；配套 PHP 后台支持脚本上传、下载、反馈、赞助墙、用户中心。

---

## 目录

- [1. 项目概览](#1-项目概览)
- [2. 整体架构](#2-整体架构)
- [3. 工程结构与模块划分](#3-工程结构与模块划分)
- [4. 核心模块详解](#4-核心模块详解)
  - [4.1 Hook 系统](#41-hook-系统)
  - [4.2 ColdRain 功能系统](#42-coldrain-功能系统)
  - [4.3 Plugin 脚本系统](#43-plugin-脚本系统)
  - [4.4 Utils 工具层](#44-utils-工具层)
  - [4.5 UI 层与生命周期注入](#45-ui-层与生命周期注入)
  - [4.6 BeanShell 脚本引擎](#46-beanshell-脚本引擎)
  - [4.7 qqinterface 桩模块](#47-qqinterface-桩模块)
  - [4.8 PHP 后台](#48-php-后台)
- [5. 关键类与函数索引](#5-关键类与函数索引)
- [6. 依赖关系](#6-依赖关系)
- [7. 项目运行方式](#7-项目运行方式)
- [8. 构建与部署](#8-构建与部署)

---

## 1. 项目概览

### 1.1 项目定位

QEdge 是一个面向 **NT QQ / NT TIM** 的 Xposed 增强模块，同时附带一个完整的在线脚本平台。它通过 Xposed 框架注入 QQ / TIM 进程，在不修改宿主 APK 的前提下扩展聊天增强、QQ 空间自动化、等级加速、群机器人等功能。除 QQ / TIM 外，模块还支持注入 KK 键盘、酷狗音乐、傲软抠图等第三方 APP 的 VIP 解锁 / 去广告。

### 1.2 技术栈

| 层 | 技术 |
|----|------|
| 客户端 | Kotlin + Java（JDK 17），Android 9 ~ 16，Jetpack Compose |
| Hook 框架 | Xposed API 82（LSPosed / LSPatch / FPA / 原子 / 无极 等） |
| 动态查找 | DexKit 2.2.0（运行时按特征扫描 DEX） |
| 反射 DSL | Kavaref 1.1.0 |
| 脚本引擎 | 内嵌 BeanShell 3.0.0-SNAPSHOT（Android/Dalvik 适配） |
| 动态类生成 | dalvik-dx 16.0.1 |
| 协议解析 | protobuf-java 4.35.1 + 自研 ProtoData wire-format 解析器 |
| 后台 | PHP 7.3 + MySQL 5.7+ / MariaDB（InnoDB + utf8mb4） |
| 构建 | AGP 9.1.1 + Kotlin 2.4.10 + Gradle（Kotlin DSL） |

### 1.3 支持版本

| 平台 | 推荐版本 | 包名 |
|------|----------|------|
| NT QQ | 8.9.58 - 9.3.xx | `com.tencent.mobileqq` |
| NT TIM | 3.9.0 - 4.1.0 | `com.tencent.tim` |
| KK 键盘 | 最新版 | `im.weshine.keyboard` |
| 酷狗音乐（普通 / 大字 / 概念） | 最新版 | `com.kugou.android` / `.elder` / `.lite` |
| 傲软抠图 | 最新版 | `com.apowersoft.backgrounderaser` |
| Android | 9.0 ~ 16 | — |
| Xposed 框架 | LSPosed / LSPatch / FPA / 原子 / 无极（Zygisk 模式）等 | — |

### 1.4 模块标识

- `applicationId` = `me.lengyu.qedge`
- `versionCode` = 15，`versionName` = "0.1.5"
- Xposed 入口：`me.lengyu.qedge.hook.XposedEntry`（声明于 `assets/xposed_init` 与 `resources/META-INF/xposed/`）
- 资源包 ID 固定 `0x69`（`--allow-reserved-package-id --package-id 0x69`），避免与宿主资源 ID 冲突

---

## 2. 整体架构

QEdge 是一个**多模块 Gradle 工程**，由三大部分组成：

```
QEdge/
├── app/                 ① Android Xposed 模块主体（Kotlin + Java）
├── qqinterface/         ② QQ 内部 API 编译期类型桩（compileOnly）
└── QEdge后台/QEdge/      ③ PHP + MySQL 在线脚本平台后台
```

三部分的关系：

- ① 依赖 ② 仅在编译期（`compileOnly`），② 的 `.class` 不会进入 ① 的 APK，运行时由宿主 QQ 进程提供真实类。
- ① 与 ③ 通过 HTTP 交互：① 内的 `HeartbeatManager` 周期上报心跳、`OnlinePluginService` 拉取 / 上传脚本、`MainActivity` 检查更新。
- ③ 是独立部署的 Web 服务，不依赖 ① 或 ②。

### 2.1 客户端分层架构

```
┌────────────────────────────────────────────────────────────────────┐
│                        Xposed 框架（宿主进程）                       │
├────────────────────────────────────────────────────────────────────┤
│  入口层     XposedEntry → MainHook → HookRegistry                  │
├────────────────────────────────────────────────────────────────────┤
│  Hook 层    hook/base（基类+注册表）  hook/api（事件监听）            │
│             hook/item（具体功能）    hook/entry（入口注入）          │
│             hook/kk / kugou / aoruan（第三方 APP）                  │
├────────────────────────────────────────────────────────────────────┤
│  业务层     ColdRainCore + 22 个 ColdRainFeature（QQ 机器人框架）   │
│             PluginManager + BeanShell 引擎（在线脚本运行时）         │
├────────────────────────────────────────────────────────────────────┤
│  UI 层      MainActivity / SettingActivity（Compose）              │
│             lifecycle（Parasitics + DynamicActivityRegistry）      │
├────────────────────────────────────────────────────────────────────┤
│  工具层     utils/dexkit | utils/reflect | utils/qq                │
│             utils/json | utils/proto | utils/hook                  │
└────────────────────────────────────────────────────────────────────┘
```

### 2.2 数据流向

```
Xposed 事件源
   ├── MSFServlet.onReceive ──► FromServiceMsgDispatcher ──┬──► OnPaiYiPai
   │                                                         ├──► OnTroopShutUp
   │                                                         ├──► OnQZonePush
   │                                                         └──► AutoLikeBack
   ├── MsgService.onRecvMsg / onAddSendMsg ──► OnReceiveMsg ──► ColdRainCore.handleMessage
   ├── IKernelMsgService.CppProxy.sendMsg ──► OnSendMsg ──► VideoToBubble
   ├── QQCustomMenuExpandableLayout.setMenu ──► OnMenuBuild ──► DownloadEmotion / 脚本菜单
   ├── PaiYiPaiHandler ──► AntiPokeDelay
   ├── MsgRecord.* ──► FlashPicBypass
   └── ArkConfigModel ──► TimArkCardBypass
```

---

## 3. 工程结构与模块划分

```
QEdge/
├── app/                                # Android Xposed 模块主体
│   ├── src/main/
│   │   ├── assets/xposed_init          # 声明 Xposed 入口类
│   │   ├── java/
│   │   │   ├── bsh/                    # 内嵌 BeanShell 脚本引擎
│   │   │   │   ├── Interpreter.java
│   │   │   │   ├── BshClassManager.java
│   │   │   │   ├── classpath/          # 脚本类加载器（含 Dex 适配）
│   │   │   │   ├── org/objectweb/asm/  # 内嵌 ASM（运行时生成字节码）
│   │   │   │   ├── security/           # 安全防护
│   │   │   │   └── BSH*.java           # AST 节点
│   │   │   └── me/lengyu/qedge/
│   │   │       ├── MainActivity.kt
│   │   │       ├── LauncherActivity.kt
│   │   │       ├── activity/           # SettingActivity / ThemeHelper
│   │   │       ├── coldrain/           # ColdRain 机器人框架
│   │   │       │   ├── ColdRainCore.java
│   │   │       │   ├── ColdRainFeature.java
│   │   │       │   └── features/       # 22 个业务 Feature
│   │   │       ├── common/             # ModuleScope
│   │   │       ├── hook/              # Hook 系统
│   │   │       │   ├── XposedEntry.java   # Xposed 入口
│   │   │       │   ├── MainHook.java      # Hook 编排
│   │   │       │   ├── HeartbeatManager.java
│   │   │       │   ├── annotation/        # @HookItemAnnotation
│   │   │       │   ├── base/              # 基类体系 + HookRegistry
│   │   │       │   ├── api/               # 底层事件监听
│   │   │       │   ├── entry/             # QQ 入口注入
│   │   │       │   ├── item/              # 具体功能项
│   │   │       │   ├── kk/                # KK 键盘
│   │   │       │   ├── kugou/             # 酷狗音乐
│   │   │       │   └── aoruan/            # 傲软抠图
│   │   │       ├── lifecycle/          # Activity 伪装 + 资源注入
│   │   │       ├── plugin/             # 在线脚本系统
│   │   │       │   ├── PluginManager.java
│   │   │       │   ├── PluginCompiler.java
│   │   │       │   ├── PluginCallback.java
│   │   │       │   ├── FixClassLoader.java
│   │   │       │   ├── api/PluginMethod.java   # 脚本可调用 API
│   │   │       │   ├── bean/            # 数据 Bean
│   │   │       │   └── view/           # ChatSettingLoader
│   │   │       ├── ui/                  # Compose UI
│   │   │       │   ├── pages/           # HomeScreen / FileManagerScreen / coldrain/
│   │   │       │   ├── components/      # atoms / molecules / dialogs
│   │   │       │   ├── core/            # theme / compatibility
│   │   │       │   └── services/        # OnlinePluginService
│   │   │       └── utils/              # 工具层
│   │   │           ├── dexkit/ | reflect/ | json/ | proto/ | qq/ | hook/
│   │   │           └── HostInfo.kt, ModuleConfig.kt, ...
│   │   ├── res/                        # 资源（图标 PNG/WebP 为主）
│   │   └── resources/META-INF/xposed/  # scope.list / xposed_init / xposed_module
│   └── build.gradle.kts
├── qqinterface/                        # QQ 内部 API 编译期桩
│   └── src/main/java/
│       ├── androidx/lifecycle/
│       ├── com/qq/jce/ | com/qq/taf/jce/   # JCE/WUP 协议
│       ├── com/tencent/
│       │   ├── common/app/ | mobileqq/app/  # QQ 应用核心
│       │   ├── aio/ | biz/ | mobileqq/      # AIO / 业务 / Activity
│       │   ├── qqnt/kernel/                 # NT 内核（含数百 nativeinterface 类）
│       │   └── widget/ | smtt/ | ...
│       └── mqq/ | oicq/ | tenpay/ ...
├── QEdge后台/QEdge/                     # PHP 后台
│   ├── require.php / function.php / install.php / index.php
│   ├── admin/         # 管理后台页面
│   ├── user/          # 用户中心页面
│   ├── api/           # HTTP API（admin/* user/*）
│   ├── online_plugin/ # 脚本上传 / 列表 / 下载 / 详情
│   ├── update/        # 版本检查 / 更新日志
│   ├── heartbeat/     # 心跳接收
│   └── assets/        # common.js / style.css
├── libs/libxposed/                    # 内嵌 libxposed 源码（参考）
├── settings.gradle.kts                 # rootProject.name = "QRoutine"，include(":app", ":qqinterface")
├── gradle/libs.versions.toml          # 版本目录
└── README.md
```

---

## 4. 核心模块详解

### 4.1 Hook 系统

Hook 系统是 QEdge 的执行核心，采用「**注解驱动 + 注册表管理 + 监听器分发**」的分层架构。

#### 4.1.1 基类体系（[hook/base/](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/hook/base)）

继承关系：

```
Listener (marker interface)
    ▲
BaseHookItem (abstract)                         # 根基类
    ├── BaseSwitchHookItem (abstract)            # 带开关 + UI
    │       └── BaseClickableHookItem<T>         # 带配置弹窗
    └── BaseApiHookItem<T extends Listener>      # API 监听型
```

| 类 | 职责 |
|----|------|
| [Listener.java](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/hook/base/Listener.java) | 所有监听器的空标记接口，用于泛型约束 |
| [BaseHookItem.java](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/hook/base/BaseHookItem.java) | 顶层抽象。`isEnable`、`getAnnotation()` 反射读注解、`isInTargetProcess()` 按 `@HookItemAnnotation.process` 字段判定进程生效范围（`"All"`/空 = 全进程，否则要求 `HostInfo.processName == packageName + target`） |
| [BaseSwitchHookItem.java](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/hook/base/BaseSwitchHookItem.java) | 带开关的 Hook 项。生命周期 `init()`：`onInit()` → 进程校验 → `initData()`（若是 Clickable）→ `onHook()`。提供 `getTag()/getDesc()/getCategory()` UI 元数据。`getPrefs()` 返回按当前 QQ 号隔离的 `QEdge_Config_{uin}`（`MODE_MULTI_PROCESS` 跨进程） |
| [BaseClickableHookItem.java](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/hook/base/BaseClickableHookItem.java) | 可点击弹窗配置项，抽象方法 `ConfigContent(Runnable onDismiss)` 由子类构建弹窗 |
| [BaseApiHookItem.java](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/hook/base/BaseApiHookItem.java) | API 监听基类。持有 `Set<T> listenerSet`；核心方法 `forEachChecked(Consumer<T>)` 遍历时自动跳过 `isEnable()==false` 的监听器，实现"开关动态控制监听器是否生效" |
| [HookRegistry.java](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/hook/base/HookRegistry.java) | 全局注册中心。`hookItems: List<BaseHookItem>` + `registeredClasses: Set<Class>` 去重；`getHookItemsByClass(Class<T>)` 按类型筛选供 `MainHook` 批量加载 |

#### 4.1.2 注解（[hook/annotation/](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/hook/annotation)）

- [HookCategory.java](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/hook/annotation/HookCategory.java)：分类常量 `CHAT / API / OTHER / ITEM`
- [HookItemAnnotation.java](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/hook/annotation/HookItemAnnotation.java)：`@Retention(RUNTIME) @Target(TYPE)`，属性 `value/tag/desc/category/process`

#### 4.1.3 工作流程

```
1. XposedEntry.initZygote()           → 保存 modulePath，探测 HookProvider（LSPosed/EdXposed/Dreamland）
2. XposedEntry.handleLoadPackage()    → 按包名分流
3. hookBaseApplicationOnCreate()      → Hook BaseApplicationImpl.onCreate after
4. HostInfo.init(ctx) + Parasitics.initForStubActivity(ctx)
5. DexKitCache.initCache() + validateAllTasks()
   ├─ 命中 → MainHook.loadHook()
   └─ 未命中 → DexKitFinder.doFind()（异步预扫 + 进度对话框 + 重启 QQ）
6. MainHook.loadHook():
   ├─ HeartbeatManager.isBanned() 检查（封禁则跳过）
   ├─ registerHookItems()        → HookRegistry 注册 19 个 Hook 项
   ├─ FromServiceMsgDispatcher.loadHook()  → MSF 包体监听
   ├─ loadApiHook()              → 遍历 BaseApiHookItem，目标进程内调 item.loadHook()
   ├─ initSwitchHookItem()       → 遍历 BaseSwitchHookItem，调 item.init()
   ├─ hookAccountChange()         → Hook QQAppInterface 账号切换，重置环境 + 加载插件 + 重启心跳
   ├─ ChatSettingLoader.loadHook() → 聊天页设置入口注入
   └─ (延迟 2s) loadPluginsIfNeeded() + ColdRainCore.init()
```

#### 4.1.4 API 层（[hook/api/](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/hook/api)）

| 类 | Hook 目标 | 监听器签名 |
|----|-----------|------------|
| [FromServiceMsgDispatcher.java](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/hook/api/FromServiceMsgDispatcher.java) | `MSFServlet.onReceive`，过滤 `trpc.msg.olpush.OlPushService.MsgPush` 命令字，wupBuffer → ProtoData → JSON 分发 | `DispatcherListener(cmd, json)` |
| [OnGetRKey.java](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/hook/api/OnGetRKey.java) | `FromServiceMsg.getWupBuffer`，命令字 `OidbSvcTrpcTcp.0x9067_202`，提取好友/群 RKey | 存入静态字段 `friendRkey / groupRkey` |
| [OnMenuBuild.kt](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/hook/api/OnMenuBuild.kt) | `QQCustomMenuExpandableLayout.setMenu` + 返回 View 的方法；QQ/TIM 分流实现 | `MenuClickListener.onClick(MsgData)` |
| [OnPaiYiPai.java](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/hook/api/OnPaiYiPai.java) | 订阅 FromServiceMsgDispatcher，`cmd1==732&&cmd2==20`（群）/ `528&&290`（私聊） | `onPai(peerUin, chatType, fromUin)` |
| [OnQZonePush.java](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/hook/api/OnQZonePush.java) | 三路并行：`QZonePushApiImpl.onHandlePushMsg`、`MessageMicro.mergeFrom`、MSFServlet 命令字 | `onPush(JSONObject)` |
| [OnReceiveMsg.java](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/hook/api/OnReceiveMsg.java) | `MsgService.onRecvMsg/onAddSendMsg`；三级类查找（DexKitCache → 实时 DexKit → 硬编码 → 反射扫包） | `onReceive(msgRecord)` |
| [OnSendMsg.java](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/hook/api/OnSendMsg.java) | `IKernelMsgService$CppProxy.sendMsg`（5 参），before | `onSend(Contact, elements)` |
| [OnTroopJoin.java](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/hook/api/OnTroopJoin.java) | QQ：`TroopMemberAddPushProcessor`；TIM：`TroopOnlinePushHandler.handleJoin` | `onJoin(troopUin, memberUin)` |
| [OnTroopQuit.java](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/hook/api/OnTroopQuit.java) | `TroopMemberInfoServiceImpl.deleteTroopMember` after | `onQuit(troopUin, memberUin)` |
| [OnTroopShutUp.java](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/hook/api/OnTroopShutUp.java) | 订阅 FromServiceMsgDispatcher，`msgHead.1==732&&.2==12` | `onShutUp(troopUin, memberUin, time, opUin)` |

#### 4.1.5 Entry 注入（[hook/entry/](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/hook/entry)）

- [QQPlusInject.java](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/hook/entry/QQPlusInject.java)：Hook `PopupMenuDialog.conversationPlusBuild`，在 "+" 菜单插入 QEdge 入口，点击启动 `SettingActivity`
- [QQSettingInject.kt](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/hook/entry/QQSettingInject.kt)：DexKit 查找 `SettingConfigProvider`，在 QQ 设置页 sections 列表 index=1 插入 QEdge 入口

#### 4.1.6 Item 功能项（[hook/item/](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/hook/item)）

| 类 | 功能 | 开关 SP Key |
|----|------|-------------|
| [FlashPicBypass.java](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/hook/item/FlashPicBypass.java) | 闪照破解：Hook `MsgRecord` 所有 public 方法，`subMsgType & ~8192` 清除闪照位 | `flash_pic_bypass` |
| [DownloadEmotion.kt](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/hook/item/DownloadEmotion.kt) | 图片/视频/语音长按下载，通过 `OnMenuBuild` 注册菜单项 `[QEdge],DownloadEmotion,保存,,2,7,32,6` | `download_emotion` |
| [AntiPokeDelay.java](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/hook/item/AntiPokeDelay.java) | 取消拍一拍时间限制：Hook `PaiYiPaiHandler` 返回 boolean 的方法，强制 `setResult(true)` | `anti_poke_delay` |
| [TransparentAvatar.kt](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/hook/item/TransparentAvatar.kt) | 透明头像：四路 Hook（PhotoCropActivity 路径捕获 / `BitmapFactory.decodeFile` / `ProfileCardUtil.F` 绕过尺寸校验 / `Bitmap.compress` 改 PNG 保留透明通道） | `transparent_avatar` |
| [VideoToBubble.kt](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/hook/item/VideoToBubble.kt) | 视频转泡泡消息：订阅 `OnSendMsg`，发送前替换视频元素为 `MsgTool.createBubbleVideoElement` | `video_to_bubble`（仅 QQ） |
| [TimArkCardBypass.kt](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/hook/item/TimArkCardBypass.kt) | TIM Ark 卡片白名单绕过：DexKit 找 `ArkConfigModel`，Hook `(String,String)→boolean` 强制返回 true | `tim_ark_card_bypass`（默认 true，仅 TIM） |
| [AutoLikeBack.kt](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/hook/item/AutoLikeBack.kt) | 名片自动回赞：识别 `type==203` 名片被赞推送，LRU 去重（上限 500），延迟 600+random(400)ms 调 `FriendTool.sendZan` | `profile_auto_like_back` |
| [QZoneLikeTool.kt](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/hook/item/QZoneLikeTool.kt) | QZone HTTP 工具集：`doLike / doComment / publishMood / qzoneClockIn / dailySign / bigVipClockIn`，依赖 `CookieTool`（skey/pskey/bkn） | （工具类，无开关） |
| [QZoneSchedule.kt](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/hook/item/QZoneSchedule.kt) | 定时任务调度器：凌晨 00:00 跑三签到，自定义 HH:mm 跑定时说说；三重去重（主进程 + AtomicBoolean + SP 日期标记） | `qzone_daily_checkin_enabled` 等 |

#### 4.1.7 第三方 APP Hook

| 文件 | 目标 APP | 策略 |
|------|----------|------|
| [KKHook.java](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/hook/kk/KKHook.java) | KK 键盘 | `isVip()→true`、`isAdFree()→true`、5 路防退出拦截（UncaughtException / System.exit / Process.killProcess / ActivityManager.forceStopPackage / DexKit 字符串查"do process kill"） |
| [KuGouHook.java](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/hook/kugou/KuGouHook.java) | 酷狗音乐（普通/大字/概念） | 跳过开屏广告（直接跳 MediaActivity）、签名伪造（替换 `PackageInfo.signatures`）、反 Hook 注入空实现、浏览器/检测关键词拦截 |
| [AoRuanHook.java](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/hook/aoruan/AoRuanHook.java) | 傲软抠图 | `VipManager.isVip/isVipValid/isExpire/isVipValidOrBalance→true`，`getDeadlineDate()→"2099.12.31"` |

#### 4.1.8 心跳与远程管控

[HeartbeatManager.java](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/hook/HeartbeatManager.java) 每 10 分钟（`HEARTBEAT_INTERVAL=600000`）向 `https://v.yuafeng.cn/QEdge/heartbeat/index.php` 上报 `{qq, nickname, qq_version, module_version}`（JSON → 16 进制字符串 POST）。响应三态：

- `code==403`：账号封禁，置 `isBanned=true`，主线程 Toast，`MainHook.loadHook()` 检查后跳过所有 Hook 加载
- `code==200`：正常，新用户返回 `initial_password` 触发欢迎弹窗
- `code==0`：有新版本，返回 `apk/version/update` 触发更新弹窗

---

### 4.2 ColdRain 功能系统

ColdRain 是一套基于 QQ NT 内核的**机器人功能框架**，采用「**核心 + 特性（Feature）**」分层架构。

#### 4.2.1 核心调度器 [ColdRainCore.java](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/coldrain/ColdRainCore.java)

- **单例**：双重检查锁定（DCL）+ `volatile instance`
- **初始化流程**（`init(Context)`）：
  1. `initConfigFile()`：在外置存储 `Android/media/<包名>/冷雨Java/` 创建目录 + `config.json` + `data/`
  2. `loadConfig()`：读 `config.json` 到 `JSONObject configData`，记录 `lastFileModified`
  3. `registerFeatures()`：硬编码注册 21 个 Feature 到 `Map<String, ColdRainFeature> features`
  4. `initDefaultConfig()`：写默认 `master_enabled=false`、`menu_name=菜单`，每个 feature 键默认 true
  5. `setupMessageListener()`：注册 `OnReceiveMsg` 监听器
  6. 注册 `WelcomeJoinFeature` / `WelcomeQuitFeature` / `BlackWhiteListFeature` 的事件监听器
  7. `HourlyChimeFeature.startTimer()` 启动整点报时定时器
- **配置热重载**：`checkAndReloadIfModified()` 比较 `configFile.lastModified()` 与缓存值，文件外部修改则自动重载
- **配置键命名约定**：
  - 全局功能开关：`feature_<name>`
  - 群功能开关：`group_feature_<name>_<groupUin>`
  - 群主控开关：`group_master_enabled_<groupUin>`
  - 全局管理员：`global_admins`；群管理员：`group_admins_<groupUin>`；主人：`master_uin`
- **消息分发**（`handleMessage`）：
  1. 总开关检查
  2. "开机/关机"指令（管理员才能操作）
  3. "开启/关闭+功能中文名"群功能开关指令
  4. 菜单指令（直接 `new MenuFeature().handle()`，不走 features Map）
  5. 遍历 features Map，`shouldHandle` + `canTriggerFeature` 权限校验，命中后 `handle` 并 `break`（单消息单处理）
- **权限分层** `canTriggerFeature`：
  ```
  isMasterEnabled 全局总开关
     ↓
  isMenuCommand（菜单类指令豁免群功能开关）
     ↓
  isFeatureEnabled 全局功能开关（feature_status 例外）
     ↓
  isPersonalFeature（feature_status / feature_group_manager / feature_at）
     ↓
  群消息：group_master_enabled + isGroupFeatureEnabled 双层校验
  ```
- **权限体系** `isAdminOrSelf(MsgData)`：自身发送 → 当前 QQ → master_uin → global_admins → group_admins
- **回复模式** `reply()`：支持 text/card/image/forward/markdown/reply/guanjia 七种模式，统一变量替换 `[at]/[qq]/[uin]/[qun]/[time]/[Name]/[pic=`，失败降级为纯文本

#### 4.2.2 Feature 接口 [ColdRainFeature.java](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/coldrain/ColdRainFeature.java)

```java
public interface ColdRainFeature {
    boolean shouldHandle(MsgData msgData);
    void handle(MsgData msgData, ColdRainCore core);
}
```

设计模式：**策略模式 + 责任链变体**，核心按 Map 注册顺序遍历，第一个 `shouldHandle==true` 即消费消息（break）。所有 Feature 的 `handle` 几乎都用 `new Thread().start()` 异步化网络 IO。

#### 4.2.3 Feature 清单（22 个）

| Feature | featureKey | 触发示例 | 说明 |
|---------|-----------|----------|------|
| [MenuFeature](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/coldrain/features/MenuFeature.java) | — | "菜单" | 装饰菜单展示已启用功能，核心直接 `new` 调用 |
| [StatusFeature](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/coldrain/features/StatusFeature.java) | `feature_status` | "运行状态" | 个人功能，聚合电池/内存/存储/版本/运行时长等信息 |
| [QuestionFeature](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/coldrain/features/QuestionFeature.java) | `feature_question` | "问答功能"/"添加本群精准问#问#答" | 精准+模糊问答，数据文件 `qa_group_<qun>.json` 等，匹配优先级：本群精准→全局精准→本群模糊→全局模糊 |
| [SignInFeature](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/coldrain/features/SignInFeature.java) | `feature_signin` | "签到"/"设置签到金币随机" | 日签到去重，随机/自定义/定值三种金币模式 |
| [GroupManagerFeature](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/coldrain/features/GroupManagerFeature.java) | `feature_group_manager` | "群管菜单"/"禁言@"/"踢@" | 个人功能，禁言/解禁/踢/踢黑/上管/下管/全体禁言等 |
| [BlackWhiteListFeature](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/coldrain/features/BlackWhiteListFeature.java) | `feature_black_white_list` | "黑白名单"/"拉黑<QQ>" | 静态注册 `OnTroopJoin`（黑名单自动踢）+ `OnTroopShutUp`（白名单自动解禁），提供 `canOperate()` 供其他 Feature 调用 |
| [WelcomeJoinFeature](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/coldrain/features/WelcomeJoinFeature.java) | `feature_welcome_join` | "进群欢迎"/"设置进群欢迎" | 注册 `OnTroopJoin` 监听，替换 `{qq}` 变量 |
| [WelcomeQuitFeature](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/coldrain/features/WelcomeQuitFeature.java) | `feature_welcome_quit` | "退群提示" | 注册 `OnTroopQuit` 监听，与 WelcomeJoin 共享"提示系统"指令 |
| [BanDetectionFeature](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/coldrain/features/BanDetectionFeature.java) | `feature_ban` | "违禁系统"/"添加违禁词" | 被动触发，每条群消息经过，命中违禁词自动禁言 |
| [QueryFeature](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/coldrain/features/QueryFeature.java) | `feature_query` | "查Q音<QQ>"/"查等级<QQ>" | 调 `vip.qq.com`/`qzone.qq.com` 等接口查 QQ 等级/达人/QID/空间 |
| [VideoParseFeature](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/coldrain/features/VideoParseFeature.java) | `feature_video_parse` | "视频解析"/含抖音/快手/B站/小红书链接 | 调 `api.yuafeng.cn` 解析视频/图集 |
| [ImageMenuFeature](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/coldrain/features/ImageMenuFeature.java) | `feature_image_menu` | "图片菜单"/"随机腹肌"/"原神系列" | 40+ 图片指令，部分即时拉取部分跟随重定向 |
| [VideoMenuFeature](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/coldrain/features/VideoMenuFeature.java) | `feature_video_menu` | "视频菜单"/"哔哩哔哩<关键词>" | 状态机，`searchCache` 缓存搜索结果，回复序号发送视频 |
| [MusicMenuFeature](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/coldrain/features/MusicMenuFeature.java) | `feature_music` | "音乐菜单"/"点歌"/"QQ点歌" | 多平台搜索（QQ/网易/酷狗/酷我/汽水），状态机选号，7 种发送模式 |
| [ImageFeature](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/coldrain/features/ImageFeature.java) | `feature_image` | "图片功能"/"抠头像<QQ>"/"阴影头像" | 从 gitee 下载 `图片.zip` 素材包，`Bitmap/Canvas/Matrix/PorterDuffXfermode` 实现头像贴图 |
| [WeatherFeature](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/coldrain/features/WeatherFeature.java) | `feature_weather` | "天气系统"/"详细天气#省#市" | 调 `wis.qq.com` 21 项生活指数 |
| [HourlyChimeFeature](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/coldrain/features/HourlyChimeFeature.java) | `feature_hourly` | "整点报时"/"切换文字报时" | 唯一基于定时器触发，每 60s 检查 `mm:ss==00:00` 时遍历所有启用群发送 |
| [TitleFeature](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/coldrain/features/TitleFeature.java) | `feature_title` | "我要头衔 <内容>"/"上头衔<QQ>" | 调 `ExtraTool.setMemberTitle`，自助申请检查违禁词 |
| [LikeFeature](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/coldrain/features/LikeFeature.java) | `feature_like` | "赞我点赞"/"点赞<QQ>" | 调 `FriendTool.sendZan(uin, 20)` |
| [AutoAdminFeature](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/coldrain/features/AutoAdminFeature.java) | `feature_autoadmin` | "自助上管"/"我要管理" | 自动 `TroopTool.setGroupAdmin` |
| [AtFeature](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/coldrain/features/AtFeature.java) | `feature_at` | "艾特处理"/"设置艾特回复" | 个人功能，4 种艾特行为：回复/禁言/提醒/管家禁言 |
| [AvatarMenuFeature](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/coldrain/features/AvatarMenuFeature.java) | `feature_avatar_menu` | "头像菜单"/"上传头像" | 需先发图片再回复指令，调 `ExtraTool.uploadAvatar/Cover` |

#### 4.2.4 协作关系

- `GroupManagerFeature` / `TitleFeature` 调用 `BlackWhiteListFeature.canOperate()` 检查白名单保护
- `WelcomeJoinFeature` ↔ `WelcomeQuitFeature` 共享"提示系统"指令
- `BlackWhiteListFeature` + `WelcomeJoinFeature` + `WelcomeQuitFeature` 均注册 `OnTroop*` 监听器独立响应事件
- 所有 Feature 通过 `handle(msgData, core)` 反向访问核心的配置/回复/权限

#### 4.2.5 插件化评估

**未使用任何插件化或反射机制**。21 个 Feature 通过 `features.put("feature_xxx", new XxxFeature())` 显式 new，编译期硬依赖。新增功能必须修改 `ColdRainCore.registerFeatures()` 源码并重新编译。

---

### 4.3 Plugin 脚本系统

Plugin 系统是一个完整的"在线脚本"运行时框架，基于 BeanShell 解释器在 QQ/TIM 宿主进程内动态执行用户编写的 `main.java` 脚本。

#### 4.3.1 核心组件

| 类 | 职责 |
|----|------|
| [PluginManager.java](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/plugin/PluginManager.java) | 全静态工具类，插件系统入口与编排者。`plugins: List<PluginInfo>` + `autoLoadList: List<String>`（持久化到 `data/AutoLoadList.json`）+ `runningCompilers: Map<String, PluginCompiler>` |
| [PluginCompiler.java](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/plugin/PluginCompiler.java) | 脚本运行容器（非传统编译器）。持有 `Interpreter`、`FixClassLoader`、`PluginMethod api`、`PluginCallback callback`、`menuItems/msgMenuItem` |
| [PluginCallback.java](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/plugin/PluginCallback.java) | 事件回调分发器，把 6 类 Hook 事件（消息收发/入退群/禁言/拍一拍）+ 聊天界面切换回调到脚本同名方法 |
| [FixClassLoader.java](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/plugin/FixClassLoader.java) | 聚合 ClassLoader，统一"宿主 QQ 类 + 模块类 + 动态 jar/dex 类"三类加载需求 |
| [PluginError.java](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/plugin/PluginError.java) | 错误处理，写 `error.log`（带时间戳+分隔线） |

#### 4.3.2 插件目录约定

```
plugin/<插件目录>/
├── info.prop        # 必需，Java Properties 格式（id / pluginName / versionCode / author）
├── desc.txt         # 可选，纯文本描述
├── main.java        # 必需，BeanShell 脚本入口
├── config/          # PluginMethod 的 putXxx/getXxx 默认存这里
├── log.txt          # 运行时日志
└── error.log       # PluginError 自动写入
```

`info.prop` 示例：

```properties
id=example_1700000000000
pluginName=示例脚本
versionCode=1.0
author=Developer
```

#### 4.3.3 生命周期

| 操作 | 方法 | 流程 |
|------|------|------|
| 加载 | `loadAll()` | 遍历 plugin/ 子目录 → `PluginInfo.fromDir(dir)` → 若 runningCompilers 已有同 ID 则保留运行态 → `loadAutoLoadConfig()` |
| 启动 | `startPlugin(plugin)` | `compiler.start()` → `runningCompilers.put(id, compiler)` |
| 停止 | `stopPlugin(plugin)` | `compiler.stop(true)` 调脚本 `unLoadPlugin()` → 注销 Activity + 反注册监听器 → `runningComparsers.remove(id)` |
| 卸载 | `deletePlugin(plugin)` | stop + 从列表移除 + 移除自动加载登记 + 递归删除目录 + 保存配置 |
| 自动加载 | `startAutoLoadPlugins()` | 新线程遍历 autoLoadList 中未运行插件，每个启动间隔 100ms |
| 热重载 | `reloadPlugin(plugin)` | stop + start |
| 全量重置 | `initAllPluginForCurrent()` | `stopAllPlugins() → plugins.clear() → loadAll() → startAutoLoadPlugins()` |

触发时机：`MainHook.loadHook()` 延迟 2s 调 `loadPluginsIfNeeded()`（10s 节流），账号切换时也会触发。

#### 4.3.4 启动流程（PluginCompiler.start）

```
1. 若已运行，先 stop(false)
2. info.updateFromDisk() 刷新元数据
3. 检查 main.java 存在
4. new Interpreter()
5. 注入上下文变量：context / myUin / classLoader / pluginPath / pluginId
6. interpreter.setClassLoader(FixClassLoader)   # 替换 BshClassManager.externalClassLoader
7. registerApiMethods(interpreter)                # PluginMethod 100+ API 注入为全局函数
8. info.setRunning(true)
9. interpreter.source("main.java")                # 解释执行脚本顶层代码
10. registerCallbacks()                           # PluginCallback 注册 6 类事件监听器 + OnMenuBuild 菜单
```

#### 4.3.5 PluginInfo 字段

| 字段 | 类型 | 来源 | 说明 |
|------|------|------|------|
| `id` | String (final) | info.prop `id` | 唯一 ID，不可变 |
| `name` | String | info.prop `pluginName` | 显示名 |
| `version` | String | info.prop `versionCode` | 版本号 |
| `author` | String | info.prop `author` | 作者 |
| `dirPath` | String (final) | 目录路径 | 插件目录 |
| `desc` | String | `desc.txt` | 描述 |
| `isRunning` | boolean | 运行时 | 是否运行中 |
| `compiler` | PluginCompiler | 构造时 new | 关联容器 |

#### 4.3.6 脚本 API（[PluginMethod.java](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/plugin/api/PluginMethod.java)）

通过反射把 `PluginMethod` 所有 public 方法（150+）包装为 `BshMethod` 注入到脚本全局命名空间。分类：

| 分类 | 代表方法 |
|------|----------|
| 日志/Toast | `log / toast / qqToast` |
| QQ 账号 | `getCurrentUin/Name/Uid / getPluginPath/Id/Name / setPluginEnabled` |
| 好友/群/成员 | `getAllFriend / isFriend / sendZan / getGroupList / shutUpAll / kickGroup / setGroupAdmin / setMemberTitle / getGroupMemberList` |
| 消息发送 | `sendMsg / sendPic / sendPtt / sendCard / sendVideo / sendFile / sendReplyMsg / recallMsg / sendBubbleVideo / sendPai` |
| 多平台音乐 | `qqsendTroopMusic / wysendTroopMusic / kgsendTroopMusic / ...sendFriendMusic` |
| Cookie/鉴权 | `getSkey / getPskey / getGTK / getBkn / getFriendRKey / getGroupRKey` |
| 配置存储 | `putString/getString / putInt/getInt / putBoolean/getBoolean / remove / contains / clear`（基于 JSON） |
| HTTP/文件 | `httpGet / httpPost / downloadFile / readFile / writeFile / listFiles` |
| 编码/加密 | `urlEncode/Decode / base64Encode/Decode / toJson / parseJson` |
| Protobuf | `pbDecode / pbEncode / pbToHex / pbFromHex` |
| Hook/反射 | `hookAfter / hookBefore / callMethod / findClass / findMethod / findField / newInstance` |
| 动态加载 | `loadJava / loadJar / loadDex` |
| Activity | `registerActivity / startQQActivity` |
| 线程 | `runOnUiThread / runOnBackgroundThread / sleep / isMainThread` |
| 菜单 | `addItem(name, callback) / addMenuItem(name, callback, msgTypes[])` |

#### 4.3.7 聊天页入口注入（[ChatSettingLoader.kt](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/plugin/view/ChatSettingLoader.kt)）

三处 Hook：
1. `ImageView.onAttachedToWindow`：检查 `contentDescription` 含"聊天设置"/"更多功能"，设置 `OnLongClickListener` → `showMenuDialog`
2. `AIODelegate.show`：保存当前 `aioDelegate`，调 `notifyChatInterface()` 回调脚本 `chatInterface(chatType, peerUin, peerName)`
3. `AIODelegate.hide`：清空引用

`showMenuDialog`：收集所有运行中插件的 `getMenuItems()`，每个插件一个 Header + Actions，点击 Header 重载该插件，点击 Action 调脚本同名方法。

#### 4.3.8 脚本回调机制

主程序 → 脚本的被动回调：

| 事件源 | 脚本回调方法 |
|--------|--------------|
| `OnReceiveMsg` | `onMsg(Object msgData)` |
| `OnSendMsg` | `getMsg(String content)` / `getSummary(String summary)`（同步拦截改写） |
| `OnTroopJoin` | `joinGroup(String troopUin, String memberUin)` |
| `OnTroopQuit` | `quitGroup(String troopUin, String memberUin)` |
| `OnTroopShutUp` | `shutUpGroup(String troopUin, String memberUin, Long time, String opUin)` |
| `OnPaiYiPai` | `onPaiYiPai(String peerUin, int chatType, String fromUin)` |
| `ChatSettingLoader` | `chatInterface(int chatType, String peerUin, String peerName)` |
| 卸载时 | `unLoadPlugin()` |
| 消息菜单点击 | `addMenuItem(name, callback)` 注册的同名方法，签名 `(Object msgData)` |
| 聊天面板菜单点击 | `addItem(name, callback)` 注册的方法，签名 `(int chatType, String peerUin, String peerName)` |

回调执行机制 `invokeMethodExists`：按方法名查找 → 按参数类型匹配 → `BshMethod.invoke`，方法不存在则安静跳过。所有回调在子线程 `Plugin-<pluginId>` 中执行。

#### 4.3.9 数据 Bean（[plugin/bean/](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/plugin/bean)）

| Bean | 用途 |
|------|------|
| [MsgData.java](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/plugin/bean/MsgData.java) | 消息数据，封装 `MsgRecord`，解析文本/图片/视频/语音/文件/回复/At 元素 |
| [FriendInfo.java](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/plugin/bean/FriendInfo.java) | 好友信息（uin/uid/name/remark） |
| [GroupInfo.java](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/plugin/bean/GroupInfo.java) | 群信息 |
| [MemberInfo.java](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/plugin/bean/MemberInfo.java) | 群成员信息 |
| [ForbidInfo.java](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/plugin/bean/ForbidInfo.java) | 禁言信息 |
| [BlackUser.java](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/plugin/bean/BlackUser.java) | 黑名单 |
| [JointGroup.java](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/plugin/bean/JointGroup.java) | 共同群 |
| [PluginInfo.java](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/plugin/bean/PluginInfo.java) | 插件元数据 |

---

### 4.4 Utils 工具层

#### 4.4.1 顶层工具

| 类 | 职责 |
|----|------|
| [HostInfo.kt](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/utils/HostInfo.kt) | 宿主环境信息单例。`packageName/processName/versionCode/versionName` + `isQQ/isTIM/isInHostProcess` + `getModuleDataPath()` 返回 `/Android/data/<宿主包>/QEdge/` |
| [ModuleConfig.kt](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/utils/ModuleConfig.kt) | 跨进程配置门面，封装 `JsonConfigUtils`，配置落地为 `data/config.json` |
| [JsonConfigUtils.java](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/utils/JsonConfigUtils.java) | 基于 `JSONObject` 的 JSON 文件 KV 存储，`loadConfig`/`saveConfig` 全量读写 |
| [LogUtils.java](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/utils/LogUtils.java) | 双通道日志（Logcat + XposedBridge.log），固定 TAG `[QEdge]` |
| [Toasts.java](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/utils/Toasts.java) | 主线程 Toast + QQ 风格 `QQToastUtil` |
| [HookUtils.java](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/utils/HookUtils.java) | Java 版 Xposed 封装（`HookCallback` 接口） |
| [HttpUtils.java](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/utils/HttpUtils.java) | 基于 `HttpURLConnection` 的同步 HTTP，`download` 手动处理 301/302/303/307/308 重定向 |
| [HybridClassLoader.java](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/utils/HybridClassLoader.java) | 自定义 ClassLoader，`isHostClass`/`isConflictingClass` 判断冲突类（androidx/kotlin/protobuf）直接抛异常强制走宿主版本 |
| [ReflectUtils.java](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/utils/ReflectUtils.java) | Java 版反射查找/调用，`injectClassLoader` 通过反射修改模块 ClassLoader 的 `parent` 字段注入 HybridClassLoader |
| [QQCurrentEnv.java](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/utils/QQCurrentEnv.java) | 缓存当前登录 QQ 账号（Uin/Uid/昵称/AppRuntime/KernelMsgService/Activity），`getQQAppInterface()` 通过 `MobileQQ.getMobileQQ().peekAppRuntime()` 获取 |
| [JarLoader.java](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/utils/JarLoader.java) | 动态加载 Jar/Dex，`loadJarToSystem` 反射 `PathClassLoader.addDexPath` 注入系统 ClassLoader |
| [ObjectStore.java](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/utils/ObjectStore.java) | 基于 `QQCurrentEnv.getCurrentDir()` 的轻量文件存储（List/String） |
| [ModulePathHolder.java](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/utils/ModulePathHolder.java) | 静态字段持有模块 APK 路径 |

#### 4.4.2 DexKit 子包（[utils/dexkit/](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/utils/dexkit)）

| 类 | 职责 |
|----|------|
| [DexKitCache.kt](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/utils/dexkit/DexKitCache.kt) | 持久化 DexKit 缓存。`cacheFile` 路径含 `hostVersionCode` + `moduleVersionCode`，宿主或模块升级时自动失效重查。`initCache()` / `saveCache()` JSON 序列化。`validateAllTasks()` 反射 HookRegistry 检查每个 DexKitTask 的 `${TAG}->${key}` 是否齐全 |
| [DexKitTask.kt](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/utils/dexkit/DexKitTask.kt) | 任务接口：`getQueryMap(): Map<String, BaseFinder>` + `requireClass(name)/requireMethod(name)` 反查缓存 |
| [DexKitFinder.kt](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/utils/dexkit/DexKitFinder.kt) | 查找调度器。Hook `SplashActivity.doOnCreate` 弹 Compose 进度对话框 → IO 协程遍历所有 DexKitTask 的 queryMap → `DexKitBridge.findClass/findMethod` → `DexKitCache.saveCache()` → `Process.killProcess` 重启 QQ |

**DexKit 完整生命周期**：

```
首次启动 / 版本升级
   │
   ▼
DexKitCache.initCache()  ← Hook 入口调用
   │ 读取 cacheFile（路径含 hostVersionCode + moduleVersionCode）
   ▼
validateAllTasks() 检查每个 "${TAG}->${key}" 是否齐全
   │
   ▼ 失败
DexKitFinder.doFind()
   ├─ System.loadLibrary("dexkit")
   ├─ MainHook.registerHookItems()
   ├─ Hook SplashActivity.doOnCreate after → 显示进度对话框
   └─ startFind() (IO 协程)
       ├─ 收集所有 DexKitTask + TroopTool + QZoneLikeTool
       ├─ DexKitBridge.create(sourceDir)
       ├─ 遍历 task.getQueryMap()
       ├─ DexKitCache.saveCache()
       └─ Process.killProcess  ← 重启 QQ 加载缓存
```

缓存键格式：`"${taskClassSimpleName}->${queryName}"`，如 `"TroopTool->setting"`。

#### 4.4.3 Reflect 子包（[utils/reflect/](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/utils/reflect)）

类型安全的反射 DSL，新代码倾向使用。

| 类 | 职责 |
|----|------|
| [ClassUtils.kt](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/utils/reflect/ClassUtils.kt) | 持有 `hostClassLoader` + `moduleClassLoader`，提供扩展属性 `String.clazz` / `String.toClass` |
| [ReflectCache.kt](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/utils/reflect/ReflectCache.kt) | `ConcurrentHashMap` 缓存 Method/Field/Constructor，哨兵对象 `NOT_FOUND` 区分"未查找"与"查找为 null" |
| [ReflectDSL.kt](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/utils/reflect/ReflectDSL.kt) | DSL 搜索器。`MethodSearcher` 可配置 `name/returnType/paramTypes/paramCount/isStatic/visibility`，多候选按继承深度打分选最优 |
| [ReflectExtensions.kt](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/utils/reflect/ReflectExtensions.kt) | 高层扩展：`Any.callMethod / Class.callStaticMethod / Any.getObject / setObject / Class.newInstanceWithArgs` |

DSL 示例：

```kotlin
val method = SomeClass::class.java.findMethod {
    name = "doSomething"
    returnType = void
    paramTypes(string, int)
    visibility = private
}
```

#### 4.4.4 JSON 子包（[utils/json/](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/utils/json)）

| 类 | 职责 |
|----|------|
| [JsonExt.java](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/utils/json/JsonExt.java) | `walk(json, vararg path)` 路径取值；`findUidDeep(obj)` 深度优先遍历找 `u_` 开头 UID |
| [MessageTool.java](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/utils/json/MessageTool.java) | 通过 DexKit 在 `pbandk` 包下查找 `Message` 与 `FieldDescriptor` 类，反射读写 protobuf 字段 |
| [ProtoData.java](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/utils/json/ProtoData.java) | **自研 protobuf wire-format 解析/序列化器**，不依赖 .proto 文件。`fromBytes/toBytes/fromJSON/toJSON`，`isLikelyString` 启发式判断字段是字符串还是嵌套 message。提供 `get/set/removeField` 等完整访问器，用于防撤回篡改 |

#### 4.4.5 Proto 子包（[utils/proto/](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/utils/proto)）

| 类 | 职责 |
|----|------|
| [PacketHelper.java](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/utils/proto/PacketHelper.java) | QQ 数据包核心。`sendRequest(serviceCmd, rawData, protoListener)` 通过 `SSOEasyServlet` + `ToServiceMsg` 发送，`BusinessObserver.onReceive` 异步回调；`sendPacket(cmd, JSONObject, packetListener)` 用 `ProtoData` 序列化 |
| [packetListener.java](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/utils/proto/packetListener.java) | 高层结果回调接口 `onResult(boolean success, JSONObject json)` |
| [protoListener.java](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/utils/proto/protoListener.java) | 底层 cmd 级回调 `onSuccess(cmd, json) / onFailure(cmd, error)` |

**数据包监听机制**：请求-响应配对模式，不是被动监听所有包。`ExtraTool.sendPacket/fetchPacket` 用 `CountDownLatch` 把异步回调转同步（30s 超时）。

#### 4.4.6 QQ 子包（[utils/qq/](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/utils/qq)）

| 类 | 职责 |
|----|------|
| [QQServiceHelper.java](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/utils/qq/QQServiceHelper.java) | 通过 QRoute 路由系统获取 API/Runtime/Handler，多重回退（QRoute.api → 反射 QQAppInterface） |
| [CookieTool.java](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/utils/qq/CookieTool.java) | 从 TicketManager 获取 skey/p_skey/real_skey/pt4token/stweb，`getBkn(key)` 标准 bkn 哈希 `5381 + (hash<<5) + char`，`getFriendRKey/getGroupRKey` 从 OnGetRKey 静态字段读 |
| [FriendTool.java](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/utils/qq/FriendTool.java) | `getAllFriend / isFriend / getUidFromUin / getUinFromUid / sendZan`（构造特定字节序列调 CardHandler） |
| [MsgTool.java](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/utils/qq/MsgTool.java) | 消息发送工具：`sendMsg/sendPic/sendPtt/sendCard/sendVideo/sendFile/sendMarkDown/sendReplyMsg/sendPai/recallMsg/addLocalJsonGrayTipMsg/createBubbleVideoElement`。`makeContact(peerUin, chatType)` 构造 Contact；`sendMsg` 解析 `[atUin=xxx]/[pic=url]` 标签 |
| [TroopTool.kt](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/utils/qq/TroopTool.kt) | 群组工具，DexKitTask 典范实现。用 `Proxy.newProxyInstance` + `CompletableFuture.get(5s)` 把 QQ NT 异步回调转同步（`getMemberInfo/getGroupMemberList/getForbidInfo`） |
| [ExtraTool.java](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/utils/qq/ExtraTool.java) | 综合协议工具（代码量最大）。头像/封面上传、音乐/小程序卡片发送、`sendPacket/fetchPacket` 同步包装、`addFriend/getGroupBlackList/getJointGroupList` 等通过 OidbSvc 协议 |

#### 4.4.7 Hook 子包（[utils/hook/](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/utils/hook)）

| 类 | 职责 |
|----|------|
| [HookExtensions.kt](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/utils/hook/HookExtensions.kt) | Kotlin lambda DSL：`Member.hookAfter/hookBefore/hookReplace(owner, block)`，`MethodHookParam.invokeOriginal()` |
| [HookStatusImpl.java](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/utils/hook/HookStatusImpl.java) | 纯 Java 静态字段（**NO KOTLIN, NO ANDROIDX** 注释），`sZygoteHookMode/sZygoteHookProvider/sIsLsposedDexObfsEnabled`，在 ClassLoader 注入早期被读取 |

#### 4.4.8 跨进程配置存储机制

```
业务代码
   │ ModuleConfig.putBoolean("feature_x", true)
   ▼
ModuleConfig.kt
   │ configDir = "${HostInfo.getModuleDataPath()}data/"
   │ = "/storage/emulated/0/Android/data/<宿主包>/QEdge/data/"
   ▼
JsonConfigUtils.java
   │ putValue → loadConfig → saveConfig
   │ 文件：data/config.json
   ▼
外部存储（Android/data/<宿主包>/QEdge/data/config.json）
   ▲
   │ 其他进程（如 QQ 主进程、模块配置进程）读取同一文件
```

跨进程特性：路径基于宿主包名，多进程共享同一 Android/data 目录；JSON 文本无文件锁，依赖"最后一次写入胜出"语义，适合低频配置场景。

#### 4.4.9 双轨制说明

| 功能 | 旧版（Java） | 新版（Kotlin） |
|------|-------------|---------------|
| ClassLoader 获取 | `ClassUtils.java` | `reflect/ClassUtils.kt` |
| 反射查找 | `ReflectUtils.java` | `reflect/ReflectDSL.kt + ReflectExtensions.kt` |
| DexKit 缓存 | `DexKitCache.java`（纯内存） | `dexkit/DexKitCache.kt`（持久化 JSON） |
| Hook | `HookUtils.java`（接口） | `hook/HookExtensions.kt`（lambda） |

新代码倾向用 Kotlin 子包，但 `QQCurrentEnv/MsgTool/ExtraTool/FriendTool/CookieTool/TroopTool` 仍引用旧版 `ReflectUtils.java`，两套体系长期并存。

---

### 4.5 UI 层与生命周期注入

#### 4.5.1 Activity 清单

| Activity | 作用 |
|----------|------|
| [MainActivity.kt](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/MainActivity.kt) | 模块自身启动入口（LAUNCHER），Compose 渲染 `MainScreen`，检查更新 + 更新日志弹窗 |
| [LauncherActivity.kt](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/LauncherActivity.kt) | NoDisplay 透明 Activity，用于无 UI 启动 |
| [SettingActivity.kt](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/activity/SettingActivity.kt) | 模块设置主页（在 QQ 进程内运行），Compose + `AnimatedContent` 侧滑切换 plugin/file_manager/cold_rain 三页 |

#### 4.5.2 SettingActivity 注入方式

`SettingActivity` 通过 `Parasitics + DynamicActivityRegistry` 在 QQ 宿主进程内运行：

1. `XposedEntry.handleLoadPackage()` 中调 `DynamicActivityRegistry.register(SettingActivity.class)`
2. `Parasitics.initForStubActivity(ctx)` 替换 `ActivityThread.mInstrumentation` 为 `ProxyInstrumentation`，Hook `IActivityManager.startActivity` 把目标 Activity 替换为 Stub `CameraPreviewActivity`，Hook `IPackageManager.getActivityInfo` 返回伪造的 `ActivityInfo`
3. 启动时通过 `Intent.putExtra(ACTIVITY_PROXY_INTENT, realIntent)` 携带真实 Intent
4. `Handler.mCallback` 拦截 `LAUNCH_ACTIVITY`（what=100/159）消息，`unwrapIntent` 还原真实 Intent
5. `ProxyInstrumentation.newActivity` 用 `moduleLoader.loadClass` 加载模块 Activity 类
6. `ProxyInstrumentation.callActivityOnCreate` 调 `injectModuleResources` 注入模块资源

#### 4.5.3 主题系统

[ThemeHelper.java](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/activity/ThemeHelper.java) + [ui/core/theme/](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/ui/core/theme)（Theme.kt / Color.kt / Dimens.kt）：

- Material 3 动态色（Light/Dark 双主题 + OLED 纯黑）
- `SettingActivity.setupTheme()` 读 `QEdge_Theme` SP，-1 跟随系统，0/1 强制亮/暗
- `SettingActivity` 通过反射 `MainHook.*` 方法操作插件列表（`getPluginList/setPluginRunning/setPluginAutoLoad/deletePlugin/reloadPlugin/createPlugin/processDataForCurrent`）

#### 4.5.4 Compose 页面结构

```
SettingActivity
  └─ QEdgeTheme
      └─ AnimatedContent (currentPage)
          ├─ "plugin"      → HomeScreen（插件列表 + 功能开关卡片分组）
          ├─ "file_manager" → FileManagerScreen（文件管理）
          └─ "cold_rain"   → ColdRainScreen（ColdRain 配置）
```

UI 组件按 Atomic Design 分层：
- [ui/components/atoms/](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/ui/components/atoms)：Buttons / QEdgeCard / QEdgeSwitch
- [ui/components/molecules/](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/ui/components/molecules)：QEdgeTopBar / TabItem / EmptyState / AnimatedComponents
- [ui/components/dialogs/](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/ui/components/dialogs)：UpdateDialog / WelcomeDialog / ConfirmDialog / TextDialog / PluginMenuDialog
- [ui/core/compatibility/](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/ui/core/compatibility)：`XposedComposeDialog`（在宿主进程弹 Compose 对话框）

#### 4.5.5 在线脚本服务 [OnlinePluginService.kt](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/ui/services/OnlinePluginService.kt)

- `BASE_URL = "https://v.yuafeng.cn/QEdge"`
- `fetchOnlinePlugins(search, callback)`：GET `/online_plugin/list.php?api=json&search=...`
- `downloadPlugin(pluginId, pluginName, callback)`：下载 ZIP 到 `temp/`，`extractPluginZip` 解压到 `plugin/<pluginName>/`（含 Zip Slip 防护 + 自动找根目录）
- `uploadPlugin(...)`：`zipDirectory` 打包后 multipart/form-data POST 到 `/online_plugin/index.php`

#### 4.5.6 生命周期与资源注入（[lifecycle/](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/lifecycle)）

| 类 | 职责 |
|----|------|
| [Parasitics.kt](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/lifecycle/Parasitics.kt) | 资源注入 + Activity 伪装。Stub Activity 为 `com.tencent.mobileqq.activity.photo.CameraPreviewActivity`。`injectModuleResources`：API 30+ 用 `ResourcesLoader`，低版本反射 `AssetManager.addAssetPath` |
| [DynamicActivityRegistry.kt](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/lifecycle/DynamicActivityRegistry.kt) | 动态 Activity 注册表（`registeredActivities: MutableMap<String, Class<out Activity>>`），供脚本 `registerActivity` 使用 |
| [CounterfeitActivityInfoFactory.java](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/lifecycle/CounterfeitActivityInfoFactory.java) | 伪造 `ActivityInfo`，让系统认为模块 Activity 是 QQ 已注册的 `CameraPreviewActivity`，绕过 manifest 校验 |

---

### 4.6 BeanShell 脚本引擎

项目内嵌完整 BeanShell 3.0.0-SNAPSHOT（[bsh/](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/bsh)），是 Plugin 系统的运行时核心。

#### 4.6.1 角色

1. 解析 `main.java` 的 Java 风格语法（含 BSH* AST 节点、Parser、ParserTreeConstants）
2. 在 `NameSpace` 中维护脚本变量与方法
3. 解释执行顶层语句与方法调用
4. 通过 `BshClassManager` 管理类加载（脚本定义 class、外部 jar、宿主类）
5. 把 `PluginMethod` 的 Java 方法包装成 `BshMethod` 注入到脚本全局命名空间

#### 4.6.2 Android/Dalvik 适配

- [BshScriptClassLoader.java](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/bsh/classpath/BshScriptClassLoader.java)：`addClass(name, byte[])` 把 BeanShell 生成的 Java 字节码用 `DexClassLoaderHelper.convertClassToDex` 转 DEX，再用 `InMemoryDexClassLoader` 加载，新 ClassLoader 挂到链头
- [DexClassLoaderHelper.java](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/bsh/util/DexClassLoaderHelper.java)：用 `com.android.dx` 的 `CfTranslator.translate` 把 `.class` 翻译为 DEX
- [BshClassManager.java](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/bsh/BshClassManager.java)：多级类加载架构（脚本生成类 > 脚本 classpath > 外部 ClassLoader（FixClassLoader）> 线程上下文 ClassLoader > Class.forName > 源文件类）
- [bsh/security/](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/bsh/security)：`MainSecurityGuard` + `SecurityGuard` 危险代码防护
- [bsh/org/objectweb/asm/](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/bsh/org/objectweb/asm)：内嵌 ASM 库，运行时生成类字节码

---

### 4.7 qqinterface 桩模块

[qqinterface/](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/qqinterface) 是**纯编译期类型桩模块**，不是运行时实际承载逻辑的库。

#### 4.7.1 定位判定

1. `qqinterface/build.gradle.kts` 依赖全部 `compileOnly`，无 `implementation`
2. `AndroidManifest.xml` 为空 `<manifest>` 标签
3. 源码中 `throw new RuntimeException("Stub !")` / `UnsupportedOperationException("only view.")`
4. 在 `app/build.gradle.kts` 第 107 行以 `compileOnly(project(":qqinterface"))` 引用

#### 4.7.2 Stub 设计模式

| 模式 | 示例 |
|------|------|
| 纯空壳类 | `LifecycleOwner`（空接口）、`AIOContact`、`ChatFragment`、`DrawerFrameViewGroup` |
| 抛异常 | `QQAppInterface.getApp()` → `throw new RuntimeException("Stub !")`；`BaseApplicationImpl.getApplication()` → `UnsupportedOperationException("only view.")` |
| 返回默认值 | `AppRuntime.getCurrentUin()` → `""`；`isLogin()` → `false` |
| 保留签名抽象类 | `JceStruct`（抽象 `readFrom/writeTo`）、`OidbWrapper`（抽象 `createToServiceMsg`）、`AppInterface`（抽象 `getApp/getAppid/getCurrentAccountUin`） |
| 完整 POJO | `com.tencent.qqnt.kernel.nativeinterface.Contact`（含字段/getter/setter/构造/toString），供 `:app` 本地构造实例传入 QQ 接口 |

#### 4.7.3 存在意义

1. **编译期类型解析**：让 `:app` 编译时能"看见"QQ 内部类的符号，避免 `cannot find symbol`
2. **强类型编码**：替代纯反射字符串，支持 IDE 自动补全与重构
3. **保留继承链**：让 `:app` 能定义 QQ 类的子类（如自定义 BusinessHandler）
4. **规避版权与体积**：纯桩无 QQ 真实业务代码，`compileOnly` 不进 APK

#### 4.7.4 关键包族

覆盖 QQ 几乎全部主要技术域，桩规模超过 200 个 Java 文件：

| 包族 | 作用域 |
|------|--------|
| `com.qq.jce.wup` / `com.qq.taf.jce` | JCE/WUP/TAF 协议序列化 |
| `com.tencent.common.app` / `com.tencent.mobileqq.app` | QQ 应用层基类（AppInterface / BaseApplicationImpl / QQAppInterface / BusinessHandler） |
| `com.tencent.mobileqq.msf.sdk` / `com.qphone.base.remote` | MSF 消息服务框架 |
| `com.tencent.mobileqq.pb` | protobuf-micro 字段体系 |
| `com.tencent.aio` / `biz` / `mobileqq.activity` | AIO 聊天 / 业务 / Activity |
| `com.tencent.mobileqq.troop` / `profilecard` | 群功能 / 资料卡 |
| `com.tencent.qqnt.kernel.api` / `nativeinterface` | NT 内核服务接口（含数百个数据/回调类） |
| `mqq.app` / `manager` / `observer` | mqq 框架（AppRuntime / MobileQQ / Servlet / MSFServlet / TicketManagerImpl） |
| `com.tencent.qroute` / `mvi` | QRoute 路由 / MVI 架构 |
| `oicq.wlogin_sdk` / `com.tenpay.sdk` | 登录 SDK / 支付 SDK |
| `com.tencent.smtt.sdk` | X5 WebView |

---

### 4.8 PHP 后台

[QEdge后台/QEdge/](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/QEdge后台/QEdge) 是独立部署的 PHP + MySQL Web 服务。

#### 4.8.1 数据库 Schema（[install.php](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/QEdge后台/QEdge/install.php)）

7 张核心表 + feedbacks 表，全部 InnoDB + utf8mb4_unicode_ci：

| 表 | 主键/唯一键 | 关键字段 | 说明 |
|----|------------|----------|------|
| `users` | `id` PK / `qq` UNIQUE | nickname / password / is_sponsor / is_banned / signature / module_version / qq_version / upload_permission / review_permission / register_time / last_login_time / last_login_ip | 用户表 |
| `plugins` | `id` PK / `plugin_id` UNIQUE | plugin_name / version_code / author_name / upload_qq / file_path / download_count / status(0待审核/1通过/2拒绝) / reject_reason / description / review_time / reviewer_qq | 在线插件表 |
| `email_codes` | `id` PK | email / code / type / expires_at / used | 邮箱验证码表 |
| `user_stats` | `id` PK / `stat_date` UNIQUE | new_users / active_users / online_plugins | 用户活跃统计表 |
| `sessions` | `id` PK / `session_id` UNIQUE | user_qq / user_type(user/admin) / ip_address / user_agent / created_at / last_activity | 会话表 |
| `banned_users` | `id` PK / `qq` UNIQUE | nickname | 黑名单表（支持未注册用户） |
| `sponsor_users` | `id` PK / `qq` UNIQUE | nickname / amount / note | 赞助用户表（支持未注册用户） |

`install.php` 还创建管理员账号（`password_hash(ADMIN_PASSWORD, PASSWORD_DEFAULT)`），创建 `logs/` 与 `uploads/plugins/` 目录，写 `install.lock` 防止重装。

#### 4.8.2 全局引入 [require.php](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/QEdge后台/QEdge/require.php)

- **错误处理**：`error_reporting(E_ALL)` + `display_errors=1` + `log_errors=1`，日志写 `logs/error.log`
- **字符集三层 UTF-8**：`default_charset=UTF-8` + `mb_internal_encoding/http_output/regex_encoding` + `iconv_set_encoding`；输入由 `function.php` 的 `decodeUtf8Input() + forceUtf8()` 兜底
- **时区**：`Asia/Shanghai`
- **数据库连接** `getDBConnection()`：PDO + `ERRMODE_EXCEPTION` + `FETCH_ASSOC` + `EMULATE_PREPARES=false`，DSN charset + 二次 `SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci` + `SET character_set_*` 三重兜底
- **会话**：
  - `SESSION_LIFETIME = 86400*7`（7 天）
  - `SESSION_SAVE_PATH = __DIR__/data/sessions`（本地私有目录，防系统 /tmp 定时清理，带 `.htaccess` Deny from all）
  - `session.gc_maxlifetime = SESSION_LIFETIME`，`gc_probability=1 / gc_divisor=100`（1% GC）
  - `cookie_lifetime = SESSION_LIFETIME`（关闭浏览器不过期）
  - PHP 7.3+ 用 `session_set_cookie_params(array)` 设置 `SameSite=Lax` + `httponly=true` + `secure=$isHttps`
- **硬编码配置**：数据库账密、管理员账密、SMTP 授权码（生产应改为环境变量）

#### 4.8.3 关键 API 端点

##### 在线脚本下载协议

| 端点 | 方法 | 请求 | 响应 |
|------|------|------|------|
| `/online_plugin/list.php?api=json` | GET | 可选 `search` | `{code:200, data:[{could_id, plugin_id, plugin_name, version_code, author_name, upload_qq, download_count, upload_time}]}` |
| `/online_plugin/download.php?id=<could_id>` | GET | `id` | `Content-Type: application/zip` + `Content-Disposition: attachment; filename="<name>_v<version>.zip"`，下载次数 +1 |
| `/online_plugin/index.php` | POST | multipart `pluginFile` + 表单 `qq` | `{code:200/400/500, message, data?}` |

##### 心跳协议 `/heartbeat/index.php`

- 方法：POST，Body 为 hex 字符串（`hex2bin` 解码为 JSON）
- 请求 JSON：`{qq, nickname, qq_version, module_version}`
- QQ 号正则校验 `^[1-9][0-9]{4,10}$`
- 查 `banned_users` 黑名单 → `code=403`
- 新用户：自动注册，生成 12 位随机密码 `encryptPassword`，响应 `code=200` + `data.initial_password`
- 老用户：`is_banned==1` → `code=403`；否则更新信息，响应 `code=200` + `data.{qq, nickname, is_sponsor, upload_permission}`
- 有新版本：响应 `code=0` + `data.{apk, version, update}`

##### 版本检查协议 `/update/check.php`

- 方法：GET，参数 `version` 和/或 `version_code`
- 响应：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "has_update": true,
    "latest_version": "0.1.3",
    "latest_version_code": 13,
    "update_log": "v0.1.3 (...)\n• ...\n• ...",
    "changelog": [{"version": "0.1.3", "date": "...", "items": [...]}],
    "download_url": "https://cdn.yuafeng.cn/ly/QEdge_0.1.3.apk"
  }
}
```

##### 用户中心 API（`/api/user/`）

| 端点 | 功能 |
|------|------|
| `login.php` | 用户登录（QQ + 密码，`password_verify`） |
| `forgot_password.php` | 忘记密码（发邮箱验证码） |
| `change_password.php` | 修改密码 |
| `update_nickname.php` | 更新昵称 |
| `update_signature.php` | 更新签名 |
| `feedback_submit.php` | 提交反馈 |
| `plugin_download.php` | 用户下载脚本 |
| `plugin_review.php` | 用户审核脚本（有 review_permission） |
| `zip_file_content.php` | 读取 ZIP 内部文件（零信任，直接解 ZIP 读 `info.prop`） |
| `zip_preview.php` | ZIP 预览 |

##### 管理员 API（`/api/admin/`）

| 端点 | 功能 |
|------|------|
| `login.php` | 管理员登录 |
| `plugin_review.php` | 脚本审核通过/拒绝 |
| `plugin_edit.php` | 编辑脚本名称/介绍/作者 |
| `plugin_delete.php` | 删除脚本 |
| `feedback_reply.php` | 回复用户反馈 |
| `sponsor_action.php` | 赞助管理 |
| `user_action.php` | 用户操作（封禁/解封/权限） |

#### 4.8.4 安全设计

| 维度 | 实现 |
|------|------|
| SQL 注入 | PDO 预处理 + `EMULATE_PREPARES=false` |
| XSS | `htmlspecialchars(ENT_QUOTES, 'UTF-8')` 转义输出；`sanitizeInput` |
| CSRF | `SameSite=Lax` Cookie；`CSRF_TOKEN_NAME` 常量 |
| 会话安全 | 本地私有 session 目录 + `.htaccess Deny`；`httponly=true` 防 JS 读取；7 天 + 滑动续期 |
| 密码哈希 | `password_hash(PASSWORD_DEFAULT)` + `password_verify`（管理员登录有明文比较的待修复点） |
| 验证码 | GD 扩展生成图形验证码（`api/captcha.php`） |
| 文件上传 | 零信任设计：服务端直接解 ZIP 读 `info.prop`，不信任客户端文本字段；同一 `plugin_id + version_code` 唯一索引兜底 |
| 编码兜底 | 三层 UTF-8：`forceUtf8` / `decodeUtf8Input` / `dbFixUtf8` |
| `.htaccess` | 各目录防护规则 |

---

## 5. 关键类与函数索引

### 5.1 入口与编排

| 类/方法 | 位置 | 说明 |
|---------|------|------|
| `XposedEntry` | [XposedEntry.java](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/hook/XposedEntry.java) | Xposed 入口，`initZygote` + `handleLoadPackage` 按包名分流 |
| `MainHook.loadHook()` | [MainHook.java](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/hook/MainHook.java) | Hook 编排核心：注册 → 加载 API Hook → 初始化开关项 → 账号切换监听 → 异步加载插件 + ColdRain |
| `MainHook.registerHookItems()` | 同上 | 注册 19 个 Hook 项到 HookRegistry |
| `MainHook.processDataForCurrent(tag)` | 同上 | 反射调所有 `BaseClickableHookItem` 的 `initData()`/`saveData()` |
| `MainHook.getPluginList()` | 同上 | 返回 `List<PluginData>` 供 UI |
| `HookRegistry.register(item)` | [HookRegistry.java](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/hook/base/HookRegistry.java) | 去重注册 Hook 项 |
| `HookRegistry.getHookItemsByClass(clazz)` | 同上 | 按类型筛选 Hook 项 |

### 5.2 Hook 基类

| 方法 | 说明 |
|------|------|
| `BaseHookItem.isInTargetProcess()` | 按注解 `process` 字段判定进程生效 |
| `BaseSwitchHookItem.init()` | 生命周期：`onInit()` → 进程校验 → `initData()` → `onHook()` |
| `BaseSwitchHookItem.getPrefs()` | 返回 `QEdge_Config_{uin}` 跨进程 SP |
| `BaseApiHookItem.forEachChecked(action)` | 遍历监听器自动跳过 `isEnable()==false` |
| `BaseApiHookItem.loadHook()` | 抽象，子类安装具体 Xposed Hook |

### 5.3 ColdRain 核心

| 方法 | 说明 |
|------|------|
| `ColdRainCore.getInstance()` | DCL 单例 |
| `ColdRainCore.init(ctx)` | 初始化流程（建目录 → 加载配置 → 注册 Feature → 注册监听器 → 启动定时器） |
| `ColdRainCore.handleMessage(msgRecord)` | 消息分发主流程 |
| `ColdRainCore.canTriggerFeature(featureKey, msgData)` | 权限分层校验 |
| `ColdRainCore.isAdminOrSelf(msgData)` | 管理员判定（自身→当前QQ→master→global_admins→group_admins） |
| `ColdRainCore.reply(msgData, text)` | 7 种回复模式分派 |
| `ColdRainCore.checkAndReloadIfModified()` | 配置热重载 |
| `ColdRainFeature.shouldHandle(msgData)` | 判定是否处理 |
| `ColdRainFeature.handle(msgData, core)` | 执行业务 |

### 5.4 Plugin 系统

| 方法 | 说明 |
|------|------|
| `PluginManager.loadAll()` | 扫描 plugin/ 目录加载所有插件 |
| `PluginManager.startPlugin(plugin)` | 启动插件 |
| `PluginManager.stopPlugin(plugin)` | 停止插件（调 `unLoadPlugin`） |
| `PluginManager.initAllPluginForCurrent()` | 全量重置 |
| `PluginCompiler.start()` | 启动 BeanShell 解释器 + 注入 API + 执行 main.java + 注册回调 |
| `PluginCompiler.stop(invokeCallback)` | 停止 + 注销 Activity + 反注册监听器 |
| `PluginCompiler.registerApiMethods(interpreter)` | 注入 PluginMethod 100+ 方法为全局函数 |
| `PluginCompiler.registerCallbacks()` | 注册 6 类事件监听器 + 菜单 |
| `PluginCallback.invokeMethodExists(name, paramTypes, args)` | 按名查找脚本方法并调用 |
| `FixClassLoader.addClassLoader(loader)` | 添加动态 jar/dex ClassLoader 到聚合链 |
| `PluginMethod.*` | 150+ 脚本可调用 API |

### 5.5 Utils 关键方法

| 方法 | 说明 |
|------|------|
| `HostInfo.init(context)` | 初始化宿主环境信息 |
| `HostInfo.getModuleDataPath()` | 返回 `/Android/data/<包名>/QEdge/` |
| `ModuleConfig.putBoolean/getString/...` | 跨进程配置读写 |
| `DexKitCache.initCache()` / `saveCache()` / `validateAllTasks()` | DexKit 缓存生命周期 |
| `DexKitFinder.doFind()` | 启动 DexKit 预扫 + 进度对话框 + 重启 QQ |
| `ReflectDSL.findMethod/findMethods/findField` | 类型安全反射查找 |
| `ProtoData.fromBytes/toBytes/fromJSON/toJSON` | 无 .proto 协议解析 |
| `PacketHelper.sendRequest/sendPacket` | QQ 数据包收发 |
| `CookieTool.getSkey/getPskey/getBkn/getGTK` | 登录态票据 |
| `MsgTool.sendMsg/sendPic/sendPtt/.../createBubbleVideoElement` | 消息发送 |
| `TroopTool.shutUp/kickGroup/setGroupAdmin/setGroupMemberTitle` | 群操作 |
| `ExtraTool.sendMusic/sendMiniApp/uploadAvatar/sendPacket/fetchPacket` | 综合协议工具 |
| `FriendTool.getAllFriend/getUidFromUin/sendZan` | 好友操作 |
| `HybridClassLoader.isHostClass/isConflictingClass` | 类冲突隔离 |

### 5.6 生命周期注入

| 方法 | 说明 |
|------|------|
| `Parasitics.initForStubActivity(ctx)` | 替换 Instrumentation + Hook IActivityManager/IPackageManager |
| `Parasitics.injectModuleResources(res)` | API 30+ 用 ResourcesLoader，低版本反射 addAssetPath |
| `DynamicActivityRegistry.register(clazz)` | 注册动态 Activity |
| `CounterfeitActivityInfoFactory.makeProxyActivityInfo(className, flags)` | 伪造 ActivityInfo |

---

## 6. 依赖关系

### 6.1 模块依赖

```
settings.gradle.kts: rootProject.name = "QRoutine", include(":app", ":qqinterface")

:app
  ├─ compileOnly(project(":qqinterface"))   # QQ 内部 API 桩（编译期，不进 APK）
  ├─ compileOnly(libs.xposed)               # Xposed API 82
  ├─ compileOnly(libs.androidx.savedstate / lifecycle.* / common.java8)
  ├─ implementation(libs.dexkit)            # DexKit 2.2.0
  ├─ implementation(libs.protobuf.java)     # protobuf 4.35.1
  ├─ implementation(libs.dalvik.dx)         # dalvik-dx 16.0.1
  ├─ implementation(libs.kavaref.core/extension/android)  # Kavaref 1.1.0
  ├─ implementation(libs.coil.compose)      # Coil 2.7.0
  ├─ implementation(platform(libs.androidx.compose.bom))  # Compose BOM 2026.06.01
  └─ implementation(libs.androidx.material3 / ui / activity.compose / core.ktx)

:qqinterface
  ├─ compileOnly(androidx.annotation:annotation:1.7.0)
  ├─ compileOnly(org.jetbrains:annotations:24.1.0)
  └─ compileOnly(org.jetbrains.kotlin:kotlin-stdlib:1.9.24)
```

### 6.2 客户端内部依赖层次

```
第 1 层（无 utils 内部依赖）:
  HostInfo, LogUtils, ModulePathHolder, HybridClassLoader, JsonConfigUtils,
  HttpUtils, ClassUtils.java/.kt, JarLoader, HookStatusImpl, packetListener,
  protoListener, JsonExt, ReflectCache.kt

第 2 层:
  ModuleConfig → HostInfo, JsonConfigUtils
  ReflectUtils → HybridClassLoader, LogUtils
  Toasts → HostInfo, QQCurrentEnv
  DexKitCache.kt → HostInfo, LogUtils, ReflectUtils
  ReflectDSL.kt → ReflectCache
  ReflectExtensions.kt → ReflectCache, XposedBridge
  MessageTool → DexKitBridge
  ProtoData → protobuf

第 3 层:
  QQCurrentEnv → HostInfo, ReflectUtils, LogUtils
  DexKitHelper → DexKitCache.java, ReflectUtils
  DexKitTask → DexKitCache.kt, ReflectUtils
  ObjectStore → QQCurrentEnv, LogUtils
  HookExtensions → BaseHookItem

第 4 层:
  QQServiceHelper → QQCurrentEnv, ReflectUtils, QRoute
  PacketHelper → ProtoData, LogUtils

第 5 层（业务工具）:
  CookieTool → QQCurrentEnv, ReflectUtils, QQServiceHelper
  FriendTool → QQServiceHelper, QQCurrentEnv, ReflectUtils
  MsgTool → QQCurrentEnv, ReflectUtils, HttpUtils, LogUtils, DexKitHelper, FriendTool, QQServiceHelper
  TroopTool → QQServiceHelper, QQCurrentEnv, ClassUtils, ReflectUtils, PacketHelper, DexKitTask, reflect.*, HostInfo
  ExtraTool → QQCurrentEnv, HostInfo, PacketHelper, HttpUtils, ProtoData, Toasts, LogUtils
  DexKitFinder → HookRegistry, HostInfo, LogUtils, reflect, hook.hookAfter, TroopTool, QZoneLikeTool, DexKitBridge, DexKitCache, ModuleScope
```

### 6.3 客户端 ↔ 后台 HTTP 交互

| 客户端组件 | 后台端点 | 频率 |
|------------|----------|------|
| `HeartbeatManager` | `/heartbeat/index.php` | 每 10 分钟 |
| `MainActivity.checkUpdate` | `/update/check.php?version_code=` | 启动时 + 手动 |
| `MainActivity.showUpdateLogDialog` | `/update/changelog.php` | 手动 |
| `OnlinePluginService.fetchOnlinePlugins` | `/online_plugin/list.php?api=json` | 进入在线脚本页 |
| `OnlinePluginService.downloadPlugin` | `/online_plugin/download.php?id=` | 用户下载 |
| `OnlinePluginService.uploadPlugin` | `/online_plugin/index.php` | 用户上传 |

---

## 7. 项目运行方式

### 7.1 运行时启动时序

```
1. 用户安装 QEdge APK，在 LSPosed 中勾选作用域（QQ/TIM/KK/酷狗/傲软）
2. QQ 进程启动 → Zygote → Xposed 框架加载 QEdge
3. XposedEntry.initZygote()            # 保存 modulePath，探测 HookProvider
4. XposedEntry.handleLoadPackage()     # 按包名分流
5. Hook BaseApplicationImpl.onCreate after:
   a. HostInfo.init(ctx)                # 读取宿主版本信息
   b. Parasitics.initForStubActivity(ctx)  # Activity 伪装 + 资源注入就绪
   c. DexKitCache.initCache() + validateAllTasks()
      ├─ 命中 → MainHook.loadHook()
      └─ 未命中 → DexKitFinder.doFind()（进度对话框 + 预扫 + 重启 QQ）
6. MainHook.loadHook():
   a. HeartbeatManager.isBanned() 检查
   b. registerHookItems()              # 注册 19 个 Hook 项
   c. FromServiceMsgDispatcher.loadHook()
   d. loadApiHook()                    # 安装各 API Hook
   e. initSwitchHookItem()            # 初始化开关项
   f. hookAccountChange()              # 监听账号切换
   g. ChatSettingLoader.loadHook()    # 聊天页设置入口
   h. (延迟 2s) loadPluginsIfNeeded() + ColdRainCore.init()
7. HeartbeatManager.getInstance().startHeartbeat()  # 启动心跳（10 分钟周期）
8. 用户在 QQ 中点 "+" 菜单或设置页 → 启动 SettingActivity（通过 Parasitics 伪装）
9. SettingActivity 通过反射 MainHook.* 操作插件列表
```

### 7.2 数据存储位置

| 数据 | 路径 | 说明 |
|------|------|------|
| 模块配置 | `/Android/data/<宿主包>/QEdge/data/config.json` | 跨进程 JSON 配置 |
| DexKit 缓存 | `/Android/data/<宿主包>/QEdge/global/dexkit/CacheMap_<host>_<module>` | 版本感知失效 |
| ColdRain 配置 | `/Android/media/<宿主包>/冷雨Java/config.json` | 机器人配置（支持热重载） |
| ColdRain 数据 | `/Android/media/<宿主包>/冷雨Java/data/` | 问答库等独立 JSON |
| 插件目录 | `/Android/data/<宿主包>/QEdge/plugin/<插件目录>/` | 每个插件一个子目录 |
| 插件自动加载列表 | `/Android/data/<宿主包>/QEdge/data/AutoLoadList.json` | 自动启动插件 ID 列表 |
| 下载媒体 | `/storage/emulated/0/Tencent/QEdge/{Pictures,Videos,PTT}/` | 图片/视频/语音下载 |
| 主题 SP | `QEdge_Theme` | 主题模式 + 当前页面 |
| 功能开关 SP | `QEdge_Config_<uin>` | 按 QQ 号隔离，`MODE_MULTI_PROCESS` |
| 心跳 SP | `QEdge_Heartbeat` | 当前 uin 缓存 |

### 7.3 支持的运行模式

- **LSPosed**：标准 Hook 模式，Dex 混淆启用（`sIsLsposedDexObfsEnabled=true`）
- **LSPatch / FPA / 原子 / 无极（Zygisk 模式）**：兼容支持
- **EdXposed / Dreamland（PineXposed）**：通过 TAG 前缀识别

---

## 8. 构建与部署

### 8.1 客户端构建

#### 环境要求

- AGP 9.1.1 + Kotlin 2.4.10 + Gradle（Kotlin DSL）
- JDK 17
- `compileSdk = 37`，`minSdk = 29`，`targetSdk = 37`
- ABI：`arm64-v8a` + `armeabi-v7a`

#### 构建命令

```bash
# Debug 构建
./gradlew :app:assembleDebug

# Release 构建（需 qedge.jks 签名，已配置在 build.gradle.kts）
./gradlew :app:assembleRelease
```

签名配置（`app/build.gradle.kts`）：

```kotlin
signingConfigs {
    create("release") {
        storeFile = file("qedge.jks")
        storePassword = "lengyu520."
        keyAlias = "qedge_key"
        keyPassword = "lengyu520."
    }
}
```

#### ADB 便捷任务

`app/build.gradle.kts` 定义了 QQ 相关 Gradle 任务：

| 任务 | 作用 |
|------|------|
| `killQQ` | `adb shell am force-stop com.tencent.mobileqq` |
| `openQQ` | `adb shell monkey -p com.tencent.mobileqq -c LAUNCHER 1` |
| `restartQQ` | 依赖 killQQ 后打开 QQ |
| `installDebugAndRestartQQ` | 依赖 `:app:installDebug`，完成后 restartQQ |

```bash
./gradlew installDebugAndRestartQQ   # 安装 Debug 版并重启 QQ
```

#### 资源与混淆

- 资源包 ID 固定 `0x69`（`--allow-reserved-package-id --package-id 0x69`）
- Release 启用 `isMinifyEnabled=true` + `isShrinkResources=true`
- ProGuard 规则见 [proguard-rules.pro](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/proguard-rules.pro)
- 排除 `/META-INF/{AL2.0,LGPL2.1}`

### 8.2 后台部署

#### 环境要求

- PHP 7.3+
- MySQL 5.7+ / MariaDB
- PDO / Session / GD 扩展

#### 部署步骤

1. 上传 `QEdge后台/QEdge/` 全部文件到网站根目录
2. 修改 `require.php` 中的数据库连接信息（`DataBase_HOST/USER/PASS/NAME`）、管理员账密（`ADMIN_USERNAME/PASSWORD/QQ`）、SMTP 配置（`SMTP_HOST/PORT/USER/PASS`）
3. 首次访问 `install.php` 创建数据表（自动建库 + 建表 + 创建管理员 + 写 `install.lock`）
4. 管理员默认账号：手动在 `users` 表把 `permission` 改成 `admin`，或用 `install.php` 创建的默认管理员
5. 目录权限：`sessions/ cache/ upload/ backup/ data/ logs/` 必须可写（`chmod 755` 或 `777`）

#### 目录结构

```
QEdge/
├── require.php              # 全局引入（DB + session + 字符集）
├── function.php             # 公共函数（sanitizeInput / encryptPassword / jsonResponse / logMessage / sendMail / generateRandomPassword / getClientIP / forceUtf8 / decodeUtf8Input）
├── install.php              # 安装脚本（建库建表 + 创建管理员 + install.lock）
├── index.php                # 首页（赞助墙 + 脚本列表）
├── admin/                   # 管理后台（dashboard/plugins/feedback/sponsor/banned）
├── user/                    # 用户中心（login/feedback/sponsor/plugin_review）
├── api/admin/               # 管理 API
├── api/user/                # 用户 API
├── api/captcha.php          # 图形验证码
├── api/get_qq_info.php      # QQ 昵称查询
├── online_plugin/           # 脚本上传/列表/下载/详情/预览
├── update/                  # 版本检查/更新日志
├── heartbeat/               # 心跳接收
├── assets/common.js         # 前端通用 JS
├── assets/style.css         # 前端样式
├── data/sessions/            # session 文件（.htaccess Deny）
├── logs/                    # 日志目录
└── uploads/plugins/         # 脚本 ZIP 上传目录
```

### 8.3 模块安装与作用域配置

1. 安装 QEdge APK 到设备
2. 在 LSPosed 管理器中启用 QEdge 模块
3. 勾选作用域：QQ / TIM（必需），KK 键盘 / 酷狗音乐 / 傲软抠图（可选）
4. 强制停止目标 APP 并重新启动
5. 首次启动会触发 DexKit 预扫（显示进度对话框），完成后自动重启 QQ 加载缓存
6. 在 QQ "+" 菜单或设置页找到 QEdge 入口进入 `SettingActivity`

### 8.4 脚本插件开发流程

1. 在 `SettingActivity` 点击"创建示例脚本"，或手动在 `plugin/` 目录下创建子目录
2. 编写 `info.prop`（id/pluginName/versionCode/author）、`desc.txt`、`main.java`
3. `main.java` 使用 BeanShell 语法，可直接调用 `PluginMethod` 暴露的 150+ API（如 `sendMsg / httpGet / hookBefore / loadJar`）
4. 实现回调方法（如 `onMsg / joinGroup / chatInterface / unLoadPlugin`）响应事件
5. 通过 `addItem / addMenuItem` 注册聊天菜单
6. 点击 `SettingActivity` 的"上传"按钮，`OnlinePluginService.uploadPlugin` 打包 ZIP 上传到后台审核
7. 审核通过后其他用户可在"在线脚本"页下载使用

---

## 附录：设计亮点

1. **注解驱动 + 注册表**：`@HookItemAnnotation` 声明元数据，`HookRegistry` 集中管理，`MainHook` 按类型批量加载，新增功能只需注册一行
2. **进程隔离**：`isInTargetProcess()` 基于注解 `process` 字段精准控制 Hook 生效进程
3. **监听器解耦**：`BaseApiHookItem.forEachChecked` 自动按 `BaseSwitchHookItem.isEnable()` 过滤，API 层与 Item 层彻底解耦，开关动态生效
4. **多版本兼容**：`OnReceiveMsg` 三级类查找（DexKitCache → DexKit 实时 → 硬编码 → 反射扫包）
5. **QQ/TIM 双适配**：`OnMenuBuild` 分 `loadHook_QQ` / `loadHook_TIM` 两套实现
6. **DexKit 预扫缓存**：`DexKitCache` 文件名带版本号实现版本感知失效，避免运行时卡顿
7. **跨进程 SP**：`MODE_MULTI_PROCESS` + 按 UIN 隔离的 `QEdge_Config_{uin}`
8. **幂等与去重**：`QZoneSchedule` 三重去重，`AutoLikeBack` LRU 去重，`HookRegistry` 类去重
9. **反检测策略**（酷狗）：签名伪造 + 反 Hook 注入空实现 + 浏览器/检测关键词拦截
10. **远程管控**：`HeartbeatManager` 实现封禁、首次欢迎、版本更新三态响应
11. **零信任上传**：服务端直接解 ZIP 读 `info.prop`，不信任客户端文本字段
12. **Activity 伪装**：替换 Instrumentation + 动态代理 IActivityManager/IPackageManager，让模块 Activity 在 QQ 宿主进程运行
13. **HybridClassLoader 类冲突隔离**：`isConflictingClass` 显式抛异常强制走宿主版本
14. **TroopTool 的 CompletableFuture + Proxy 模式**：把 QQ NT 异步回调接口转同步结果
15. **ProtoData 无 .proto 协议解析**：直接对 wire format 字节流操作，结合 `isLikelyString` 启发式判断
16. **BeanShell Android 适配**：`DexClassLoaderHelper` 把生成的字节码用 dx 转 DEX，让脚本 `class` 定义在 Android 上可加载
17. **FixClassLoader 聚合加载**：统一宿主类 + 模块类 + 动态 jar/dex 类三类加载需求

---

<p align="center">Made with Java/Kotlin by LengYu</p>
