# QEdge Code Wiki

> 基于 **NT QQ / NT TIM** 的 Xposed 增强模块 + 在线脚本平台。模块化设计，所有功能开关独立可控；零卡顿设计原则：Hook 全部 O(1)，无遍历/无循环反射/无深度拷贝；配套 PHP 后台支持脚本上传、下载、反馈、赞助墙、用户中心。

---

## 目录

1. [项目整体架构](#1-项目整体架构)
2. [技术栈与依赖](#2-技术栈与依赖)
3. [模块职责详解](#3-模块职责详解)
4. [核心流程与初始化](#4-核心流程与初始化)
5. [关键类与函数说明](#5-关键类与函数说明)
6. [Hook 系统](#6-hook-系统)
7. [DexKit 动态查找机制](#7-dexkit-动态查找机制)
8. [冷雨功能核心](#8-冷雨功能核心)
9. [在线脚本插件系统](#9-在线脚本插件系统)
10. [UI 层架构（Jetpack Compose）](#10-ui-层架构jetpack-compose)
11. [基础设施层](#11-基础设施层)
12. [QQ 服务接口封装](#12-qq-服务接口封装)
13. [项目构建与运行](#13-项目构建与运行)
14. [第三方 APP Hook](#14-第三方-app-hook)
15. [PHP 后台架构](#15-php-后台架构)

---

## 1. 项目整体架构

### 1.1 分层架构图

```
┌─────────────────────────────────────────────────────────────┐
│                        UI 层 (Compose)                       │
│  MainActivity / HomeScreen / ColdRainScreen / FileManager   │
├─────────────────────────────────────────────────────────────┤
│                     功能业务层                                │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────┐   │
│  │ ColdRainCore │  │ PluginManager│  │ QQ/TIM 功能Hook  │   │
│  └──────────────┘  └──────────────┘  └──────────────────┘   │
├─────────────────────────────────────────────────────────────┤
│                     Hook 框架层                              │
│  XposedEntry → MainHook → HookRegistry → BaseHookItem       │
├─────────────────────────────────────────────────────────────┤
│                   DexKit 动态查找层                          │
│  DexKitFinder / DexKitTask / DexKitCache                    │
├─────────────────────────────────────────────────────────────┤
│                    基础设施层                                 │
│  ModuleConfig / LogUtils / HttpUtils / QQCurrentEnv / ...   │
├─────────────────────────────────────────────────────────────┤
│              QQ Stub 接口层 (qqinterface)                    │
│  IKernelService / IFriendsInfoService / MsgElement / ...    │
└─────────────────────────────────────────────────────────────┘
```

### 1.2 工程目录结构

```
QEdge/
├── app/                                    # 主模块（Xposed模块 + UI）
│   └── src/main/java/me/lengyu/qedge/
│       ├── activity/                       # Activity 入口
│       │   ├── SettingActivity.kt         # 寄生在QQ中的设置页
│       │   └── ThemeHelper.java           # 主题辅助
│       ├── coldrain/                       # 冷雨机器人核心
│       │   ├── ColdRainCore.java          # 核心调度器（单例）
│       │   ├── ColdRainFeature.java       # 功能接口
│       │   └── features/                  # 20+ 功能实现
│       ├── hook/                           # Hook 系统
│       │   ├── XposedEntry.java           # Xposed入口
│       │   ├── MainHook.java              # Hook主加载器
│       │   ├── HeartbeatManager.java      # 心跳管理
│       │   ├── annotation/                # Hook注解（分类/标记）
│       │   ├── api/                       # 通用API Hook（事件分发）
│       │   ├── base/                      # Hook基类 + 注册中心
│       │   ├── entry/                     # QQ菜单注入入口
│       │   ├── item/                      # QQ/TIM具体功能项
│       │   ├── kk/                        # KK键盘Hook
│       │   ├── kugou/                     # 酷狗音乐Hook
│       │   └── aoruan/                    # 傲软抠图Hook
│       ├── lifecycle/                      # 动态Activity注册
│       │   ├── Parasitics.kt              # 资源/寄生Activity
│       │   └── DynamicActivityRegistry.kt # 动态注册Activity
│       ├── plugin/                         # 在线脚本引擎
│       │   ├── PluginManager.java         # 插件生命周期管理
│       │   ├── PluginCompiler.java        # BeanShell编译执行
│       │   ├── PluginCallback.java        # 插件回调接口
│       │   ├── api/PluginMethod.java      # 插件暴露的API
│       │   ├── bean/                      # 插件数据Bean（MsgData等）
│       │   └── view/ChatSettingLoader.kt  # 聊天设置注入
│       ├── ui/                             # Compose UI
│       │   ├── components/                # 组件（atoms/molecules/dialogs）
│       │   ├── core/theme/                # 主题系统
│       │   ├── core/compatibility/        # Xposed兼容弹窗
│       │   └── pages/                     # 页面（Home/ColdRain/File）
│       ├── utils/                          # 工具集
│       │   ├── dexkit/                    # DexKit KT封装
│       │   ├── hook/                      # Hook扩展
│       │   ├── json/                      # JSON/Proto解析工具
│       │   ├── proto/                     # 协议包收发
│       │   ├── qq/                        # QQ专用工具
│       │   ├── reflect/                   # 反射工具
│       │   ├── ModuleConfig.kt            # 模块配置（JSON存储）
│       │   ├── HostInfo.kt                # 宿主信息
│       │   ├── QQCurrentEnv.java          # QQ当前环境
│       │   ├── LogUtils.java              # 日志工具
│       │   ├── HttpUtils.java             # 网络请求
│       │   ├── JsonConfigUtils.java       # JSON配置底层
│       │   └── ObjectStore.java           # 对象持久化
│       ├── LauncherActivity.kt            # 启动入口
│       └── MainActivity.kt                # 模块主界面
│
├── qqinterface/                            # QQ接口stub（compileOnly）
│   └── src/main/java/
│       ├── com/tencent/qqnt/              # NT内核接口
│       ├── com/tencent/mobileqq/          # QQ主程序接口
│       └── com/tencent/common/app/        # 通用应用接口
│
├── libs/libxposed/                         # Xposed API（本地库）
│
├── QEdge后台/QEdge/                        # PHP后台
│   ├── admin/                              # 管理员后台
│   ├── user/                               # 用户中心
│   ├── api/                                # REST API
│   ├── online_plugin/                      # 在线脚本平台
│   ├── update/                             # 更新检查
│   └── heartbeat/                          # 心跳接口
│
├── bsh/                                     # BeanShell解释器（内嵌源码）
│
├── build.gradle.kts                        # 根构建脚本
├── gradle/libs.versions.toml               # 版本目录
└── gradle.properties                       # Gradle配置（R8 fullMode）
```

---

## 2. 技术栈与依赖

### 2.1 版本信息

| 项目 | 版本 |
|------|------|
| namespace / applicationId | `me.lengyu.qedge` |
| versionCode / versionName | 17 / 0.1.7 |
| compileSdk / targetSdk | 37 |
| minSdk | 29 (Android 10) |
| AGP | 9.1.1 |
| Kotlin | 2.4.10 |
| Compose BOM | 2026.06.01 |

### 2.2 核心依赖

| 依赖 | 版本 | 用途 |
|------|------|------|
| DexKit | 2.2.0 | 动态查找混淆后的类/方法 |
| Protobuf JavaLite | 4.35.1 | 协议数据解析 |
| Dalvik DX | 16.0.1 | BeanShell动态编译dex |
| Xposed API | 82 | Xposed Hook框架接口（compileOnly） |
| AndroidX Core KTX | 1.19.0 | Kotlin扩展 |
| Compose Material3 | BOM管理 | UI组件库 |
| Activity Compose | 1.13.0 | Compose Activity集成 |
| qqinterface module | — | QQ/TIM接口stub（compileOnly） |

### 2.3 NDK 配置

```kotlin
ndk {
    abiFilters += listOf("arm64-v8a", "armeabi-v7a")
}
```

### 2.4 资源 ID 隔离

```kotlin
androidResources {
    additionalParameters += listOf("--allow-reserved-package-id", "--package-id", "0x69")
}
```
> 目的：模块资源与宿主QQ资源不冲突，实现寄生Activity资源访问。

### 2.5 R8 优化配置（gradle.properties）

```properties
android.enableR8.fullMode=true    # 启用R8全模式，激进代码移除
```

- Proguard 规则保留：`-dontobfuscate`（Xposed模块禁止混淆）
- Proguard 规则保留：`-keep class me.lengyu.qedge.** { *; }`（模块代码全保留）
- **禁止** `-dontoptimize`（开启R8优化，方法内联/死代码移除）

---

## 3. 模块职责详解

### 3.1 app 模块

Xposed 模块主体，包含以下子系统：

| 子包 | 职责 | 关键类 |
|------|------|--------|
| `hook/` | Xposed Hook 系统，所有宿主注入逻辑入口 | `XposedEntry`, `MainHook`, `HookRegistry` |
| `coldrain/` | 冷雨QQ机器人：消息处理、功能调度、群管 | `ColdRainCore`, `ColdRainFeature`, `*Feature` |
| `plugin/` | 在线脚本平台：BeanShell脚本加载/编译/执行 | `PluginManager`, `PluginCompiler` |
| `ui/` | Jetpack Compose UI：模块首页、冷雨配置、文件管理 | `HomeScreen`, `MainScreen`, `ColdRainScreen` |
| `utils/` | 基础设施：配置、日志、网络、反射、DexKit、QQ工具 | `ModuleConfig`, `LogUtils`, `QQCurrentEnv` |
| `lifecycle/` | 寄生Activity：在QQ进程中启动模块Activity | `Parasitics`, `DynamicActivityRegistry` |
| `activity/` | Activity实现类 | `SettingActivity`, `ThemeHelper` |

### 3.2 qqinterface 模块

QQ/TIM 接口 Stub 层（compileOnly，不打包进APK）：

- 提供 QQ NT 内核接口签名（`IKernelService`, `IFriendsInfoService`等）
- 提供消息数据类签名（`MsgElement`, `Contact`, `TroopInfo`等）
- 提供 QRoute 路由接口
- 作用：编译时类型检查，运行时通过宿主 ClassLoader 加载真实类

### 3.3 libs/libxposed 模块

本地 Xposed API 库（compileOnly）：
- `api/`：IXposedHookLoadPackage / IXposedHookZygoteInit 接口
- `service/`：Xposed 服务端接口

---

## 4. 核心流程与初始化

### 4.1 Xposed 注入全流程

```
Xposed Framework
    │
    ▼
XposedEntry.initZygote()           ──→ 检测Hook框架（LSPosed/EdXposed）
    │
    ▼
XposedEntry.handleLoadPackage()    ──→ 按包名分流
    │
    ├─ QQ/TIM (com.tencent.mobileqq/tim)
    │       │
    │       ▼
    │   hook BaseApplicationImpl.onCreate
    │       │
    │       ▼
    │   HostInfo.init(hostContext)        初始化宿主信息
    │   Parasitics.initForStubActivity()  注册寄生Activity
    │       │
    │       ├─ DexKit缓存有效？
    │       │   ├─ 是 → MainHook.loadHook()
    │       │   └─ 否 → DexKitFinder.doFind() → 显示查找弹窗
    │       │
    │       ▼
    │   MainHook.loadHook()
    │       ├─ registerHookItems()         注册所有BaseHookItem
    │       ├─ FromServiceMsgDispatcher    服务消息分发
    │       ├─ loadApiHook()               加载API类Hook
    │       ├─ initSwitchHookItem()        初始化开关型Hook
    │       ├─ hookAccountChange()         监听账号切换
    │       ├─ ChatSettingLoader           聊天设置注入
    │       └─ Thread[Plugin-AutoLoad]
    │           ├─ 2s延迟 → loadPluginsIfNeeded()  加载BeanShell插件
    │           └─ ColdRainCore.init()              冷雨机器人初始化
    │
    ├─ KK键盘 (im.weshine.keyboard)  → KKHook.loadHook()
    ├─ 酷狗大字版/概念版             → KuGouHook.loadHook(flavor)
    └─ 傲软抠图                      → AoRuanHook.loadHook()
```

### 4.2 DexKit 查找流程（首次启动/缓存失效）

1. Hook `SplashActivity.doOnCreate` 显示 Compose 查找进度弹窗
2. 收集所有 `DexKitTask`（含 `HookRegistry` 中 + `TroopTool` + `QZoneLikeTool`）
3. 过滤 `.filter { it.isApplicable() }`（TIM专属功能在QQ不执行，反之亦然）
4. 创建 `DexKitBridge.create(sourceDir)` 加载宿主 APK
5. 逐个执行任务的 `getQueryMap()` → FindClass / FindMethod
6. 结果存入 `DexKitCache.cacheMap`，key = `TaskTAG->QueryName`
7. 保存缓存到文件 → 提示"查找完成，点击确定退出QQ" → 杀进程重启

### 4.3 账号切换流程

1. Hook `QQAppInterface` 的消息Facade初始化方法（或构造函数）
2. 触发后 3s 延迟执行 `onAccountChanged()`：
   - `QQCurrentEnv.reset()` 清空 Uin/Uid/Nickname 缓存
   - 保存当前 Uin 到 `QEdge_Config_<Uin>` SP
   - `processDataForCurrent("init")` → 初始化所有可点击Hook项数据
   - `loadPluginsIfNeeded()` 重新加载当前账号插件
   - `HeartbeatManager.startHeartbeat()` 重启心跳

---

## 5. 关键类与函数说明

### 5.1 入口类

#### [XposedEntry.java](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/hook/XposedEntry.java)

Xposed 模块入口，实现 `IXposedHookLoadPackage` + `IXposedHookZygoteInit`。

| 函数 | 说明 |
|------|------|
| `initZygote(StartupParam)` | Zygote 阶段初始化，检测 Hook 框架类型（LSPosed/EdXposed/Dreamland），保存 modulePath |
| `handleLoadPackage(LoadPackageParam)` | 按包名分发：QQ/TIM → 延迟到 Application.onCreate；第三方APP → 立即Hook |
| `isNameSupported(packageName)` | 支持的宿主：QQ/TIM/KK键盘/酷狗/傲软（前缀匹配） |
| `hookBaseApplicationOnCreate(classLoader)` | QQ/TIM 主入口：延迟AtomicBoolean一次性初始化 |
| `getModulePathFromClassLoader()` | modulePath 为 null 时的兜底，从 ClassLoader 的 dexElements 中反向查找 .apk 路径 |

关键常量：
```java
String[] supportedPackages = {
    "com.tencent.mobileqq", "com.tencent.tim",
    "im.weshine.keyboard", "com.kugou.android", "com.apowersoft.backgrounderaser"
}
```

#### [MainHook.java](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/hook/MainHook.java)

Hook 加载调度中心。

| 函数 | 说明 |
|------|------|
| `registerHookItems()` | 静态注册 20+ BaseHookItem 到 HookRegistry（OnReceiveMsg/FlashPicBypass/KeepAlive/...） |
| `loadHook()` | 主加载入口：检查封禁状态 → 注册项 → 加载API Hook → 初始化开关Hook → Hook账号切换 → 线程加载插件+冷雨 |
| `loadApiHook()` | 遍历所有 BaseApiHookItem，若 `isInTargetProcess()` 则 `item.loadHook()` |
| `initSwitchHookItem()` | 遍历所有 BaseSwitchHookItem，调用 `item.init()` 读取配置启用/禁用 |
| `hookAccountChange()` | 监听账号切换（多策略：优先找特定方法→兜底构造函数） |
| `onAccountChanged()` | 账号变化时：重置QQ环境→写Uin缓存→重初始化数据→重载插件→重启心跳 |
| `processDataForCurrent(tag)` | 触发所有 BaseClickableHookItem 的 initData/saveData（反射调用protected方法） |

插件管理静态方法（UI调用）：`getPluginList()`, `setPluginRunning()`, `setPluginAutoLoad()`, `deletePlugin()`, `reloadPlugin()`, `createPlugin()`

---

### 5.2 Hook 基类体系

#### [BaseHookItem.java](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/hook/base/BaseHookItem.java)

所有 Hook 项的根类。

```java
public abstract class BaseHookItem {
    protected boolean isEnable = true;
    public boolean isInTargetProcess();   // 读HookItemAnnotation，判断当前进程是否匹配
    public boolean isEnable();
    public void setEnable(boolean enable);
}
```

#### HookItem 继承链

```
BaseHookItem
├── BaseApiHookItem<T extends Listener>       // API类Hook（事件分发型）
│   ├── OnReceiveMsg                          // 接收消息
│   ├── OnSendMsg                             // 发送消息
│   ├── OnMenuBuild                           // 菜单构建
│   ├── OnPaiYiPai                            // 拍一拍
│   ├── OnQZonePush                           // QZone推送
│   ├── OnTroopJoin / OnTroopQuit             // 进/退群
│   ├── OnTroopShutUp                         // 群禁言
│   ├── OnGetRKey                             // RKey获取
│   └── FromServiceMsgDispatcher              // 服务消息
│
├── BaseSwitchHookItem                        // 开关型Hook（UI开关控制）
│   ├── FlashPicBypass                        // 闪照破解
│   ├── DownloadEmotion                       // 资源下载
│   ├── TransparentAvatar                     // 透明头像
│   ├── VideoToBubble                         // 视频转泡泡
│   ├── AntiPokeDelay                         // 取消拍一拍延迟
│   ├── TimArkCardBypass                      // TIM卡片绕过
│   ├── AutoLikeBack                          // 名片回赞
│   ├── KeepAliveHook                         // 保活
│   ├── QZoneSchedule                         // 定时任务调度
│   └── RemoveLinkInfo                        // 屏蔽链接卡片
│
└── BaseClickableHookItem                     // 可点击菜单项
    ├── QQPlusInject                          // QQ+注入
    └── QQSettingInject                       // 设置页注入
```

#### [HookRegistry.java](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/hook/base/HookRegistry.java)

Hook 注册中心（静态 List + Set 去重）。

| 方法 | 说明 |
|------|------|
| `register(BaseHookItem)` | 注册单个Hook项（按Class去重） |
| `getHookItems()` | 返回所有Hook项副本 |
| `getHookItemsByClass(Class<T>)` | 按类型过滤（如获取所有BaseSwitchHookItem） |

---

## 6. Hook 系统

### 6.1 API Hook 机制（观察者模式）

每个 `BaseApiHookItem` 内部维护一个 `List<T extends Listener>` 监听器列表：

```
OnReceiveMsg.INSTANCE.registerListener(new ReceiveMsgListener() {
    void onReceive(Object msgRecord) { ... }
});
```

以 [OnReceiveMsg](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/hook/api/OnReceiveMsg.java) 为例：

1. `loadHook()` 查找 `IKernelMsgService` 实现类（DexKit缓存→DexKit查找→硬编码兜底三策略）
2. Hook `onRecvMsg(ArrayList<MsgRecord>)` after → 取第0条 → `notifyListeners(msgRecord)`
3. Hook `onAddSendMsg(MsgRecord)` after → 同样通知

### 6.2 Hook 工具封装

`HookUtils.hookAfter(method, callback)` - 统一的 after Hook 封装，避免重复样板代码。

---

## 7. DexKit 动态查找机制

### 7.1 设计目标

不同版本 QQ/TIM 类名/方法名混淆不同，DexKit 通过字符串特征（类中包含的方法名、字段字符串）定位目标。

### 7.2 核心组件

#### [DexKitTask.kt](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/utils/dexkit/DexKitTask.kt) 接口

```kotlin
interface DexKitTask {
    val TAG: String
    fun getQueryMap(): Map<String, BaseFinder>   // "name" -> FindClass/FindMethod
    fun isApplicable(): Boolean = true           // 宿主适用性（如TIM-only覆写返回HostInfo.isTIM）
    fun requireClass(name: String): Class<*>     // 缓存获取Class
    fun requireMethod(name: String): Method      // 缓存获取Method
}
```

#### [DexKitFinder.kt](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/utils/dexkit/DexKitFinder.kt) 对象

执行实际的 DexKit 查找。

关键流程：
1. 从 `HookRegistry` 过滤出 `DexKitTask` 实例 + 追加 `TroopTool` + `QZoneLikeTool`
2. 过滤 `.filter { it.isApplicable() }`
3. `DexKitBridge.create(sourceDir)` 打开宿主 dex
4. 遍历所有 task 的 queryMap：
   - `FindClass` → `b.findClass(query)` → `singleOrNull()` → 取 descriptor 存入 cache
   - `FindMethod` → 同理，无匹配时记录 Error 日志

#### [DexKitCache.kt](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/utils/dexkit/DexKitCache.kt)

结果缓存（内存 Map + 磁盘 JSON 文件）。

| 方法 | 说明 |
|------|------|
| `initCache()` | 从文件加载缓存到内存 |
| `validateAllTasks()` | 校验缓存中每个条目是否仍然有效（类是否包含期望方法） |
| `saveCache()` | 内存cache写入文件 |
| `getDescriptor(key)` | 取 `TaskTAG->name` 的descriptor字符串 |
| `getClass(key)` | descriptor → Class（DexClass.getInstance） |
| `getMethod(key)` | descriptor → Method |

缓存Key格式：`"OnReceiveMsg->msgService"` / `"TroopTool->getMemberInfo"`

---

## 8. 冷雨功能核心

### 8.1 架构设计

[ColdRainCore.java](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/coldrain/ColdRainCore.java) 是整个冷雨机器人的大脑，采用**单例 + 策略模式**：

```
ColdRainCore (单例)
├── Map<String, ColdRainFeature> features     // 20+ 功能策略
│   ├── "feature_status"       → StatusFeature
│   ├── "feature_menu"         → MenuFeature
│   ├── "feature_question"     → QuestionFeature
│   ├── "feature_signin"       → SignInFeature
│   ├── "feature_group_manager"→ GroupManagerFeature
│   ├── "feature_ban"          → BanDetectionFeature
│   ├── "feature_video_parse"  → VideoParseFeature
│   ├── "feature_image_menu"   → ImageMenuFeature
│   ├── "feature_music"        → MusicMenuFeature
│   ├── "feature_weather"      → WeatherFeature
│   ├── "feature_hourly"       → HourlyChimeFeature
│   ├── "feature_title"        → TitleFeature
│   ├── "feature_like"         → LikeFeature
│   ├── "feature_autoadmin"    → AutoAdminFeature
│   ├── "feature_at"           → AtFeature
│   ├── "feature_avatar_menu"  → AvatarMenuFeature
│   ├── "feature_welcome_join" → WelcomeJoinFeature
│   ├── "feature_welcome_quit" → WelcomeQuitFeature
│   └── "feature_black_white_list" → BlackWhiteListFeature
│
├── config.json               // 配置（集中式JSON，禁止SharedPreferences）
├── data/                     // 功能数据文件
└── OnReceiveMsg.Listener     // 消息入口
```

### 8.2 [ColdRainFeature.java](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/coldrain/ColdRainFeature.java) 接口

```java
public interface ColdRainFeature {
    boolean shouldHandle(MsgData msgData);   // 判断是否需要处理该消息
    void handle(MsgData msgData, ColdRainCore core);  // 执行处理
}
```

### 8.3 消息处理流水线

```
收到消息 (OnReceiveMsg)
    │
    ▼
handleMessage(msgRecord)
    ├─ MsgData(msgRecord) 解析消息结构
    │
    ├─ [Q群管家token捕获] type=2 && userUin=2854196310 && msg含"token"
    │   └─ handleGuanjiaTokenMessage() → 解析JSON卡片→保存token→触发问答
    │
    ├─ 特殊命令「开机/关机」→ 仅管理员/自己 → 设置 group_master_enabled_<群号>
    │
    ├─ 特殊命令「开启/关闭XX功能」→ 管理员命令 → 设置 group_feature_<key>_<群号>
    │
    ├─ 「菜单」命令 → isMenuRestricted() 检查 → MenuFeature.handle()
    │
    └─ 遍历 features.entrySet()
        ├─ feature.shouldHandle(msgData)?
        ├─ canTriggerFeature(featureKey, msgData)?
        │   ├─ master_enabled?
        │   ├─ menu_restricted?（仅管理员/自己）
        │   ├─ group_only?（群聊专属功能在私聊不可用）
        │   ├─ group_master_enabled?
        │   ├─ feature_enabled?（全局开关）
        │   └─ group_feature_enabled?（群独立开关）
        └─ feature.handle(msgData, this) → break（匹配即停止）
```

### 8.4 权限层级

```
isAdminOrSelf(msgData) 判定（任一满足即通过）
├── sendType == 1 (自己发送)
├── uin == getMyUin() (自己)
├── uin == master_uin (主人)
├── uin ∈ global_admins.split(",") (全局管理员)
└── 群聊场景：uin ∈ group_admins_<群号>.split(",") (群管理员)
```

### 8.5 配置管理

**文件位置**：`<QQCurrentEnv.getLocalPath()>/Android/media/<包名>/冷雨Java/config.json`

| 配置键 | 类型 | 说明 |
|--------|------|------|
| `master_enabled` | bool | 冷雨总开关（全局） |
| `menu_name` | string | 菜单名称，默认"菜单" |
| `menu_restricted` | bool | 菜单限制：仅主人/管理员/自己可触发 |
| `master_uin` | string | 主人QQ号 |
| `global_admins` | string | 全局管理员（逗号分隔） |
| `group_admins_<群号>` | string | 群管理员（逗号分隔） |
| `group_master_enabled_<群号>` | bool | 单群开机状态 |
| `group_<featureKey>_<群号>` | bool | 单群功能开关 |
| `feature_<name>` | bool | 功能全局开关（默认全true） |
| `reply_mode` | string | 回复模式：text/card/image/forward/markdown/reply/guanjia |
| `guanjia_token_<群号>` | string | Q群管家会话token（自动获取） |
| `menu_name` | string | 自定义菜单触发词 |

### 8.6 回复模式（6种）

| mode | 说明 |
|------|------|
| text / 文字 | 纯文本消息 `MsgTool.sendMsg()` |
| card / 卡片 | 构造Ark JSON卡片发送 |
| image / 图片 | 调用API生成文字转图后发图 |
| forward / 转发 | 构造"聊天记录"合并转发卡片 |
| markdown / MarkDown | 转换为Markdown卡片格式，文本变inline命令链接 |
| reply / 回复 | 有msgId则引用回复，否则纯文本 |
| guanjia / 管家 | **群聊专用**：通过Q群管家问答机制间接回复（需群主/管理员权限） |

回复模板变量替换：`[at]→艾特`, `[qq]→自己UIN`, `[uin]→发送者UIN`, `[qun]→群号`, `[time]→时间`, `[Name]→发送者昵称`

### 8.7 Q群管家机制（间接回复）

```
发送回复请求
    │
    ├─ 检查：群聊 + 群主/管理员权限 + pskey/skey存在
    │
    ├─ guanjiaAddQna()  添加临时问答 {question=随机串, answer=实际内容}
    │   └─ POST web.qun.qq.com/qunrobot/proxy/...
    │      Headers: qname-service:976321:131072 + qname-space:Production
    │      Body字段全部 escapeJson()（处理"、\、\n、\r、\t）
    │
    ├─ 有 token?
    │   ├─ 否 → 艾特 Q群管家(2854196310) 发"Come on!" → 等卡片 → 保存token
    │   └─ 是 → guanjiaTriggerQna(question, token)
    │           ├─ 成功 → guanjiaDeleteQna("1"/"2") 清理临时问答
    │           └─ 会话过期(ec=70000) → 重新艾特管家
    │
    └─ 任何失败 → 回退为纯文本发送
```

---

## 9. 在线脚本插件系统

### 9.1 架构概览

[PluginManager.java](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/plugin/PluginManager.java) 管理所有 BeanShell 脚本插件。

```
存储结构：
<ModuleDataPath>/plugin/
├── <插件目录名>/
│   ├── info.prop       # id, pluginName, versionCode, author
│   ├── desc.txt        # 描述文本
│   └── main.java       # BeanShell源码（类Java语法）
```

### 9.2 生命周期

| 方法 | 说明 |
|------|------|
| `loadAll()` | 扫描plugin目录，构建 `List<PluginInfo>`（恢复运行中状态） |
| `startPlugin(PluginInfo)` | `PluginCompiler.start()` → 编译执行main.java，保存Compiler到runningCompilers |
| `stopPlugin(PluginInfo)` | `PluginCompiler.stop(true)` → 调用 `unLoadPlugin()` → 移除runningCompilers |
| `reloadPlugin(PluginInfo)` | stop + start |
| `deletePlugin(PluginInfo)` | stop → 移除列表 → 递归删目录 |
| `setAutoLoad(plugin, isAuto)` | 增删 autoLoadList，持久化到 `AutoLoadList.json` |
| `startAutoLoadPlugins()` | 线程遍历：autoLoadList中且未运行的启动（间隔100ms） |
| `initAllPluginForCurrent()` | 全停 → 全清 → 重加载 → 自动启动 |
| `createExamplePlugin()` | 生成含 info.prop/desc.txt/main.java 的示例脚本 |

### 9.3 BeanShell 脚本示例

```java
log("脚本开始运行...");
qqToast(2, "Hello World!");

addItem("测试菜单", "onTestClick");

void onTestClick(int chatType, String peerUin, String peerName) {
    qqToast(2, "点击了菜单");
}

void unLoadPlugin() {
    qqToast(0, "脚本停止运行");
    log("脚本停止运行");
}
```

---

## 10. UI 层架构（Jetpack Compose）

### 10.1 组件分层

```
ui/
├── components/
│   ├── atoms/                      # 基础原子组件
│   │   ├── Buttons.kt             # ActionButton / 各种按钮
│   │   ├── QEdgeCard.kt           # 统一卡片容器
│   │   └── QEdgeSwitch.kt         # 自定义开关
│   ├── molecules/                  # 分子组件
│   │   ├── QEdgeTopBar.kt         # 顶部栏
│   │   ├── TabItem.kt             # Tab项
│   │   ├── EmptyState.kt          # 空状态
│   │   └── AnimatedComponents.kt  # 动画列表项
│   └── dialogs/                    # 弹窗
│       ├── CenterDialogContainerNoButton.kt
│       ├── ConfirmDialog.kt
│       ├── PluginMenuDialog.kt
│       ├── TextDialog.kt
│       ├── UpdateDialog.kt
│       └── WelcomeDialog.kt
│
├── core/
│   ├── theme/                      # 主题系统
│   │   ├── Color.kt               # 颜色定义（Light/Dark/OLED）
│   │   ├── Dimens.kt              # 尺寸常量
│   │   └── Theme.kt               # QEdgeTheme入口 + Material3动态色
│   └── compatibility/
│       └── XposedComposeDialog.kt # Xposed宿主内启动Compose弹窗的基类
│
└── pages/
    ├── home/                       # 新首页（MainScreen）
    │   ├── MainScreen.kt
    │   ├── HomeContentPanel.kt
    │   ├── HomeSideRail.kt
    │   └── HomeBatteryState.kt
    ├── HomeScreen.kt              # 旧版首页（Tab切换：模块/脚本/冷雨/文件）
    ├── coldrain/                   # 冷雨配置
    │   ├── ColdRainScreen.kt
    │   ├── ColdRainConfig.kt
    │   └── ColdRainConfigSection.kt
    ├── file/                       # 文件管理
    │   ├── FileManagerScreen.kt
    │   ├── FileListPanel.kt
    │   ├── TextEditorScreen.kt
    │   ├── AudioPlayerScreen.kt    # 含音频播放
    │   ├── ImagePreviewScreen.kt   # 含图片预览
    │   └── FileManagerUtils.kt
    ├── PluginData.java            # 插件UI数据类
    └── services/
        └── OnlinePluginService.kt # 在线脚本平台API
```

### 10.2 主Activity入口

[MainActivity.kt](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/MainActivity.kt)

- `enableEdgeToEdge()` + `WindowCompat.setDecorFitsSystemWindows(false)` 沉浸式
- 启动时自动 `checkUpdate(showToast=false)` 访问 `check.php` 检测新版本
- 提供：`QQ群/Telegram/用户后台/更新日志/检查更新/启动QQ` 入口

[HomeScreen.kt](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/ui/pages/HomeScreen.kt)（模块内设置页，寄生QQ中）

4个Tab：
1. **模块首页**：QQ空间（秒赞/秒评/定时说说）/ 聊天功能（闪照破解/资源下载/屏蔽链接/视频转泡泡/拍一拍/TIM卡片绕过）/ 资料卡（透明头像/名片回赞）/ 等级加速（4签到+自动加好友+空间浏览）/ 应用保活（像素/前台/后台通知）
2. **Java脚本**：本地插件列表 + 在线脚本市场（搜索/下载/上传）
3. **冷雨Java**：冷雨配置界面（`ColdRainScreen`）
4. **文件管理**：`FileManagerScreen`

---

## 11. 基础设施层

### 11.1 配置存储（严格禁止 SharedPreferences）

#### [ModuleConfig.kt](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/utils/ModuleConfig.kt)

顶层配置入口，单例 object，底层委托 `JsonConfigUtils`。

```kotlin
object ModuleConfig {
    private const val CONFIG_NAME = "config"
    private val configDir = "${HostInfo.getModuleDataPath()}data/"
    
    // 类型安全的 put/get：Boolean/String/Int/Long
    fun putBoolean(key: String, value: Boolean)
    fun getBoolean(key: String, defaultValue: Boolean): Boolean
    // ... 其他类型
    fun contains(key: String): Boolean
    fun remove(key: String)
}
```

#### [JsonConfigUtils.java](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/utils/JsonConfigUtils.java)

底层JSON文件读写：

- 每个配置名 = 一个 `.json` 文件（绝对路径 `absoluteDir/configName.json`）
- 操作：loadConfig（读文件→JSONObject）→ 修改 → saveConfig（写回文件）
- 支持类型：String/Int/Long/Double/Boolean + remove/clear/contains
- 提供 `getConfigMap()` 转为 `Map<String,Object>`

#### ColdRainCore 独立配置

冷雨使用自己的 `config.json`（`冷雨Java/config.json`），不经过 ModuleConfig，原因：
- 多账号隔离路径不同（基于 `QQCurrentEnv.getLocalPath()`）
- 支持 `checkAndReloadIfModified()`：每次读取前比较文件 mtime，发现外部修改自动 reload

### 11.2 宿主信息

[HostInfo.kt](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/utils/HostInfo.kt)

```kotlin
object HostInfo {
    const val PACKAGE_NAME_QQ = "com.tencent.mobileqq"
    const val PACKAGE_NAME_TIM = "com.tencent.tim"
    
    lateinit var packageName: String      // 宿主包名
    lateinit var processName: String      // 当前进程名
    var versionCode: Long = 0             // 宿主版本号
    var versionName: String = ""          // 宿主版本名
    
    val isQQ: Boolean      // packageName == QQ
    val isTIM: Boolean     // packageName == TIM
    val isInHostProcess: Boolean  // isQQ || isTIM
    
    fun init(context)       // PM获取包信息、计算moduleDataPath
    fun getHostContext(): Context?   // 宿主Context
    fun getModuleDataPath(): String  // 模块数据目录（Android/data/.../QEdge/）
}
```

### 11.3 QQ 当前环境

[QQCurrentEnv.java](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/utils/QQCurrentEnv.java)

| 方法/字段 | 说明 |
|-----------|------|
| `getCurrentUin()` | 当前登录QQ号（多级兜底：getCurrentAccountUin→getAccount→getAccountUin→currentUin字段） |
| `getCookieUin()` | Cookie格式UIN，自动补零到10位，前缀"o"（如 o0123456789） |
| `getCurrentUid()` | 当前Uid（NT架构新标识） |
| `getCurrentName()` | 当前昵称 |
| `getQQAppInterface()` | `QQAppInterface` 实例（MobileQQ.peekAppRuntime→wait兜底→BaseApplicationImpl兜底） |
| `getKernelMsgService()` | NT内核消息服务（IKernelService → msgService.getValue） |
| `getActivity()` | 当前前台Activity（反射ActivityThread.mActivities遍历） |
| `getCurrentDir()` | 模块数据目录路径（自动mkdirs） |
| `getLocalPath()` | 存储根路径 `Environment.getExternalStorageDirectory().getPath()+"/"` |
| `getHostPath()` | 宿主数据目录 `/Android/data/<包名>/` |
| `reset()` | 账号切换时清空全部缓存 |

### 11.4 日志系统

[LogUtils.java](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/utils/LogUtils.java)

- 双输出：`android.util.Log` + 写入磁盘文件
- 文件位置：`<CurrentDir>/log/yyyy-MM-dd.log`
- 线程安全：`synchronized(LOCK)` 包裹写文件
- 格式：`yyyy-MM-dd HH:mm:ss.SSS LEVEL TAG: message`
- Throwable：`StringWriter + PrintWriter` 捕获完整堆栈
- 日志级别：`d/i/w/e`，均支持 `(message)` 和 `(tag, message)` 双签名

### 11.5 网络工具

HttpUtils.java - HTTP 请求封装：

- `get(url)` / `get(url, headers)` - GET请求
- `post(url, body)` / `post(url, body, headers)` - POST请求（body=String）
- 用于：Q群管家API、等级加速签到、空间接口、视频解析、更新检查等

---

## 12. QQ 服务接口封装

### 12.1 QQServiceHelper

[QQServiceHelper.java](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/utils/qq/QQServiceHelper.java)

统一的 QQ 服务获取入口，**优先直接调用而非反射**：

| 方法 | 说明 |
|------|------|
| `<T> T getApi(Class<T>)` | **QRouteApi接口**：优先 `QRoute.api(apiClass)` → 兜底反射 |
| `<T> T getRuntime(Class<T>)` | getApi兜底 → QQAppInterface中查找匹配返回类型的方法 |
| `<T> T getHandler(Class<T>)` | getBusinessHandler(handlerClass.getName) → 兜底按返回类型查找 |
| `AppRuntime getRuntime()` | `BaseApplicationImpl.getApplication().getRuntime()` |
| `<T extends IRuntimeService> T getRuntimeService(serviceClass)` | `runtime.getRuntimeService(serviceClass, "")`（**直接泛型调用，无反射**） |

### 12.2 MsgTool

消息发送工具（`utils/qq/MsgTool.java`）：

| 方法 | 说明 |
|------|------|
| `sendMsg(Contact, text)` / `sendMsg(peerUin, type, text)` | 发送纯文本 |
| `sendReplyMsg(Contact, msgId, text)` | 引用回复 |
| `sendPic(peerUin, url, type)` | 发送图片（URL） |
| `sendVideo(peerUin, path, type, size, ...)` | 发送视频 |
| `sendPtt(peerUin, url, type)` | 发送语音（音乐解析功能用） |
| `sendCard(peerUin, cardJson, type)` | 发送Ark卡片 |
| `sendMarkDown(peerUin, mdText, type)` | 发送Markdown卡片 |
| `recallMsg(type, peerUin, msgId)` | 撤回消息 |
| `sendMusicCard(...)` / `sendMiniApp(...)` | 构造特殊卡片（ExtraTool中） |

### 12.3 TroopTool

群相关操作（`utils/qq/TroopTool.kt`，实现 DexKitTask）：

| 方法 | 说明 |
|------|------|
| `getGroupInfo(groupUin): TroopInfo` | 获取群信息（isTroopOwner / isTroopAdmin 判断） |
| `getMemberInfo(groupUin, uin): MemberInfo` | 获取群成员信息（uinName群名片） |
| `shutUp(groupUin, uin, timeSec)` | 群禁言 |
| `kickMember(groupUin, uin, refuseForever)` | 踢人 |
| `setAdmin(groupUin, uin, isAdmin)` | 设置/取消管理员 |
| `setUniqueTitle(groupUin, uin, title, expireTime)` | 设置专属头衔 |

### 12.4 FriendTool

好友操作（`utils/qq/FriendTool.java`）：

| 方法 | 说明 |
|------|------|
| `getUidFromUin(uin): String` | Uin → Uid 转换（NT需要） |
| `getFriendInfo(uid)` | 获取好友信息（昵称等） |
| `likeProfile(uid, count)` | 名片点赞 |
| `addFriend(uin, message)` | 添加好友（等级加速用） |
| `delFriend(uin, delType, notShieldTmpSession)` | 删除好友（IDelFriendService.delFriend） |

### 12.5 CookieTool

Cookie 管理（`utils/qq/CookieTool.java`）：

| 方法 | 说明 |
|------|------|
| `getSkey(): String?` | 获取 skey |
| `getPskey(domain): String?` | 获取指定域名的 pskey（如 qun.qq.com） |
| `getBkn(skey): long` | skey → bkn（g_tk）计算算法 |
| `getFullCookie(domain): String` | 组装完整 Cookie 字符串：`p_uin=o0xxx;uin=o0xxx;skey=xxx;p_skey=xxx` |

---

## 13. 项目构建与运行

### 13.1 构建命令

```bash
# Debug 构建
./gradlew :app:assembleDebug

# Release 构建（R8优化+资源压缩+签名）
./gradlew :app:assembleRelease

# 安装Debug并重启QQ（自定义Gradle任务）
./gradlew installDebugAndRestartQQ
```

### 13.2 自定义 Gradle 任务（app/build.gradle.kts）

| 任务名 | 说明 |
|--------|------|
| `killQQ` | `adb shell am force-stop com.tencent.mobileqq` |
| `openQQ` | `adb shell monkey -p com.tencent.mobileqq ... LAUNCHER` |
| `restartQQ` | 先 kill 再 open |
| `installDebugAndRestartQQ` | `:app:installDebug` → restartQQ |

### 13.3 运行要求

| 项目 | 要求 |
|------|------|
| Android 系统 | 9.0 ~ 16 (API 28 ~ 37) |
| Xposed 框架 | LSPosed / LSPatch / FPA / 原子 / 无极（Zygisk模式） |
| NT QQ | 8.9.58 ~ 9.3.xx（包名 com.tencent.mobileqq） |
| NT TIM | 3.9.0 ~ 4.1.0（包名 com.tencent.tim） |
| 作用域 | LSPosed中勾选：QQ/TIM/KK键盘/酷狗/傲软 |

### 13.4 首次启动流程

1. 安装 APK → LSPosed 中勾选 QQ / TIM 作用域
2. 重启 QQ → 首次启动弹出 DexKit 查找进度对话框
3. 等待所有方法查找完成 → 点击"确定"杀进程
4. 再次打开 QQ → 注入成功
5. QQ首页下拉 → QQ Plus → QEdge 设置入口进入设置页
6. 或从桌面 QEdge 图标进入独立模块界面

### 13.5 签名配置

Release/Debug 均使用硬编码签名：
```kotlin
storeFile = file("qedge.jks")
storePassword = "lengyu520."
keyAlias = "qedge_key"
keyPassword = "lengyu520."
```

---

## 14. 第三方 APP Hook

### 14.1 KK 键盘 (im.weshine.keyboard) - [KKHook.java](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/hook/kk/KKHook.java)

- 解锁 SVIP / VIP：皮肤、字体、表情全部可用
- 关闭所有广告
- DexKit 查找退出拦截逻辑 + 全局兜底防崩溃
- Hook Application.onCreate 后直接注入

### 14.2 酷狗音乐 (com.kugou.android.elder / .lite) - [KuGouHook.java](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/hook/kugou/KuGouHook.java)

**大字版** Hook `KGTinkerApplication.onCreate`
**概念版** Hook `KGApplication.onCreate`

- 跳过所有开屏广告：
  - GdtSplashActivity / AdContainerActivity → 直接跳 MediaActivity
  - 大字版/概念版 gotoAd 方法 → 返回跳过

### 14.3 傲软抠图 (com.apowersoft.backgrounderaser) - [AoRuanHook.java](file:///c:/Users/ASUS/AndroidStudioProjects/QEdge/app/src/main/java/me/lengyu/qedge/hook/aoruan/AoRuanHook.java)

- 解锁 VIP：Hook `VipManager` 的以下方法全返回 true：
  - `isVip()` / `isVipValid()` / `isExpire()` / `isVipValidOrBalance()`
- `getDeadlineDate()` 改写为长有效期
- 抠图不限次数

---

## 15. PHP 后台架构

### 15.1 环境要求

- PHP 7.3+ （全局 utf8mb4 字符集）
- MySQL 5.7+ / MariaDB
- 必需目录写入权限：`sessions/ cache/ upload/ backup/` (chmod 755)

### 15.2 目录结构

```
QEdge后台/QEdge/
├── index.php              # 首页：赞助墙 + 脚本列表
├── require.php            # 全局引入：DB连接 + Session + 字符集 + 安全过滤
├── install.php            # 首次访问：创建数据表
├── function.php           # 公共函数库
├── .htaccess              # URL重写 + 安全限制
├──
├── admin/                 # 管理后台（需admin权限）
│   ├── index.php          # 仪表盘
│   ├── plugins.php        # 脚本列表 + 编辑名称/介绍/作者 + 删除
│   ├── sponsor.php        # 赞助管理
│   ├── feedback.php       # 用户反馈处理 + 回复
│   ├── banned.php         # 封禁用户
│   ├── login.php          # 管理员登录
│   └── logout.php
├──
├── user/                  # 用户中心
│   ├── index.php          # 个人中心
│   ├── login.php          # 用户登录
│   ├── forgotpassword.php # 找回密码
│   ├── feedback.php       # 反馈提交 + 查看历史
│   ├── sponsor.php        # 赞助作者（微信/支付宝收款码）
│   ├── plugin_review.php  # 脚本评论
│   ├── motify_password.php# 修改密码
│   ├── header.php         # 公共头部（侧滑栏布局）
│   └── footer.php
├──
├── api/                   # REST API（模块端调用，JSON返回）
│   ├── captcha.php        # 验证码
│   ├── get_qq_info.php    # 获取QQ信息
│   ├── admin/             # 管理员API
│   │   ├── login.php
│   │   ├── plugin_edit.php / plugin_delete.php / plugin_review.php
│   │   ├── sponsor_action.php
│   │   ├── user_action.php
│   │   └── feedback_reply.php
│   └── user/              # 用户API
│       ├── login.php / forgot_password.php / change_password.php
│       ├── plugin_download.php / plugin_review.php
│       ├── feedback_submit.php
│       ├── zip_preview.php / zip_file_content.php
│       └── update_nickname.php / update_signature.php
├──
├── online_plugin/         # 在线脚本平台（前端页面）
│   ├── index.php          # 列表页
│   ├── list.php           # 列表数据
│   ├── details.php        # 详情页
│   ├── preview.php        # 预览脚本内容
│   └── download.php       # 下载ZIP
├──
├── update/                # 模块更新
│   ├── check.php          # 版本检查API（模块端MainActivity调用）
│   ├── changelog.php      # 更新日志API
│   └── index.php
├──
├── heartbeat/             # 模块心跳
│   └── index.php          # 模块定期上报保活
└── assets/                # 静态资源
    ├── common.js          # 公共JS
    └── style.css          # 公共CSS
```

### 15.3 安全特性

| 特性 | 实现 |
|------|------|
| SQL注入防护 | 全部用户输入使用 PDO 预处理 |
| XSS 防护 | 输出时 `htmlspecialchars(ENT_QUOTES, 'UTF-8')` 转义 |
| Session 安全 | 私有存储路径 + `gc_maxlifetime/cookie_lifetime` 7天 + 滑动窗口续期 |
| 脚本上传校验 | ZIP 内部读取 `info.prop`（id/name/version/author），避免 multipart 编码问题 |
| 版本唯一约束 | 数据库 `idx_plugin_version(plugin_id, version_code)` 唯一索引，同ID同版本无论作者都驳回 |
| 字符集 | require.php 全局 `SET NAMES utf8mb4` |

### 15.4 数据库（核心表推测）

- `users` - 用户表（含 permission 字段：user/admin）
- `plugins` - 脚本表（plugin_id, plugin_name, version_code, author, desc, upload_qq, download_count, review_status）
- `plugin_reviews` - 脚本评论表
- `feedback` - 反馈表（user_id, content, reply, status）
- `sponsors` - 赞助墙表
- `sessions` - Session存储表

---

## 附录 A：关键约束与工程规范

（来自项目实际约束，必须遵守）

| 约束 | 说明 |
|------|------|
| **禁止 SharedPreferences** | 所有配置使用 JsonConfigUtils / ColdRainCore.config.json 的集中式 JSON 文件 |
| **禁止 Build.CPU_ABI** | API 21 废弃，必须使用 `Build.SUPPORTED_ABIS[0]` 并检查数组长度 |
| **禁止 MODE_MULTI_PROCESS** | API 23 废弃，必须使用 MODE_PRIVATE |
| **禁止 -dontoptimize** | Proguard 中不得包含，必须开启 R8 优化 |
| **必须 R8 fullMode** | gradle.properties 中 `android.enableR8.fullMode=true` |
| **必须 -dontobfuscate** | Xposed 模块不得混淆代码 |
| **必须 keep 模块代码** | `-keep class me.lengyu.qedge.** { *; }` |
| **DexKitTask 必须 isApplicable()** | 宿主专属功能覆写返回 HostInfo.isTIM / isQQ，任务列表必须 `.filter { it.isApplicable() }` |
| **HTTP/IO 必须异步** | 所有 Hook 回调 O(1)，耗时操作丢新 Thread |
| **定时任务仅主进程** | 通过 `HostInfo.processName == HostInfo.packageName` 判断，避免子进程重复 |
| **QQServiceHelper.getRuntimeService** | 必须 `<T extends IRuntimeService>` 直接泛型调用，禁止反射 |
| **QRouteApi 接口必须 getApi()** | 如 IFriendsInfoService 必须用 `QQServiceHelper.getApi()` 而非 getRuntimeService |
| **多账号路径隔离** | 文件路径必须使用 `QQCurrentEnv.getLocalPath()/getHostPath()` 或 `Environment.getExternalStorageDirectory()`，禁止硬编码 `/storage/emulated/0/` |
| **菜单限制启用时** | 触发冷雨功能 (`ColdRainCore.java:427`) + 显示菜单 (`ColdRainCore.java:380`) 都必须通过 `isMenuRestricted()` 检查 |
| **Q群管家功能** | 需要群主/管理员权限 + 仅群聊(mtype=2) + 首次需艾特Q群管家(2854196310) |
| **LogUtils 异常日志** | 必须重定向到 `QEdge/log/yyyy-MM-dd.log`，禁止使用 `XposedBridge.log(throwable)` |
| **KeepAliveHook WindowManager** | 必须使用主线程 Looper，通知每3秒重新下发（QQ onResume 会 cancelAll） |

---

## 附录 B：等级加速三重触发机制

所有等级加速功能（空间签到/日签/大会员/自动加好友/空间浏览）必须三重触发保证执行：

1. **加载时触发**：loadHook 启动时检查是否已做
2. **开关切换触发**：用户在 UI 打开开关时立即检查
3. **00:00 每日触发**：Timer 定时任务，每天零点检查

每日去重：`isDoneToday(tag)` / `markDoneToday(tag)` 幂等机制，避免重复执行。

---

<p align="center">QEdge Code Wiki v1.0 — 基于项目源码自动生成</p>
