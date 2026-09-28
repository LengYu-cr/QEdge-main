# QEdge 代码审查报告

> 审查对象：QEdge（Android Xposed 模块，宿主 NT QQ / TIM 及多个第三方 APP）
> 审查范围：`app/src/main/java/`（`me/lengyu/qedge/**`、`bsh/**`、`com/liquidglass/**`）+ 构建脚本 + 清单 + 资源规则
> 审查维度：安全性、性能与并发、健壮性与资源、架构与设计、文档一致性
> 方法：静态源码精读 + 全仓检索取证；每条结论均附 `文件:行号` 证据，已剔除无法证实的推测项
> 说明：本报告仅做分析，**未修改任何代码**

---

## 0. 摘要

### 0.1 结论

项目整体工程化程度不低——配置系统有去抖落盘与原子写、冷启动有 32 位设备拒绝与优雅降级、DexKit 有缓存与并发限流。但存在三类**结构性**问题：

1. **脚本/插件体系没有有效沙箱**，且插件代码放在可被本地篡改的外部存储、在线下载不校验完整性 —— 三者叠加构成完整的任意代码执行链。
2. **宿主崩溃风险链**：动态 Activity 代理、IPackageManager 代理、DexKit 缺失类等多处异常会穿透到宿主进程且无兜底。
3. **宣称与实现不符**：文档称「Hook 全部 O(1)、零卡顿」「必须 isApplicable 过滤」「同一套 PluginMethod 宿主接口」，实际均不成立（详见 §5）。

### 0.2 问题统计

| 维度 | 高 | 中 | 低 | 合计 |
|---|---|---|---|---|
| 安全性 | 5 | 6 | 3 | 14 |
| 性能与并发 | 3 | 12 | 9 | 24 |
| 健壮性与资源 | 6 | 21 | 3 | 30 |
| 架构与设计 | 9 | 12 | 7 | 28 |
| **合计** | **23** | **51** | **22** | **96** |

### 0.3 优先修复 Top 10（按风险收益排序）

| # | 问题 | 位置 | 级别 |
|---|---|---|---|
| 1 | 插件代码位于外部存储且可被本地篡改 → 植入任意代码 | `utils/HostInfo.kt:83-85`、`utils/qq/QQCurrentEnv.java:307-317` | 高 |
| 2 | 在线插件下载无签名/哈希校验即可解压并加载执行（供应链） | `ui/services/OnlinePluginService.kt:79-147` | 高 |
| 3 | BeanShell 插件无沙箱，任意 Java 脚本可任意代码执行 | `plugin/PluginCompiler.java:76` | 高 |
| 4 | Rhino JS 未设 ClassShutter、用 `initStandardObjects`，暴露 Java 包 | `plugin/JsRuntime.kt:71` | 高 |
| 5 | 插件 API 直接向脚本暴露 QQ 会话票据（skey/pskey/real_skey…）与反射/文件/网络原语 | `plugin/api/PluginMethod.java:255-289,601-665,863-914` | 高 |
| 6 | 全局未捕获异常处理器「吞异常、不转交原 handler、不杀进程」 | `hook/kk/KKHook.java:175-187` | 高 |
| 7 | `CounterfeitActivityInfoFactory` 抛未捕获异常穿透 IPackageManager 代理 → 崩宿主 | `lifecycle/CounterfeitActivityInfoFactory.java:16-38` | 高 |
| 8 | `Parasitics` 用 `!!` + `as` 强转，失败即崩宿主 | `lifecycle/Parasitics.kt:324` | 高 |
| 9 | 冷启动主线程同步做缓存读盘 + 反射校验 + Hook 注册 → 首帧卡顿 | `hook/XposedEntry.java:206-233`、`hook/MainHook.java:131-173` | 高 |
| 10 | 消息热路径 `listenerSet` 为裸 `HashSet`，并发遍历可能抛 CME 中断消息处理 | `hook/base/BaseApiHookItem.java:11-45` | 高 |

---

## 1. 安全性

> 威胁模型：模块运行于宿主进程内、持有 Xposed Hook 能力；「插件/脚本」允许用户加载 Java(BeanShell) 与 JS(Rhino)。因此**脚本沙箱是否有效**、**插件来源是否可信**是关键。

### 1.1 高危

#### S-H1　BeanShell 引擎未启用有效沙箱，脚本可获得任意代码执行
- 证据：`plugin/PluginCompiler.java:76` `interpreter = new Interpreter();`（未安装任何 SecurityGuard）；全仓检索 `SecurityGuard` / `setSecurityGuard` 在 `me/lengyu/qedge` 下**零命中**。
- 内置的 `bsh/security/MainSecurityGuard.java` 即便启用也仅禁止脚本操作 guard 自身，对文件/网络/反射一律放行。
- 影响：任意 `main.java` 在宿主进程内以完整权限执行——读写任意文件、任意网络请求、经 `classLoader` 加载 `com.tencent.*` 并反射调用 QQ 内部 API。
- 建议：为 `Interpreter` 安装白名单 `SecurityGuard`（默认全拒，仅放行 `PluginMethod` 受控 API）；或在文档中如实声明「插件拥有完整权限」。

#### S-H2　Rhino JS 未设 ClassShutter，暴露 Java 包对象
- 证据：`plugin/JsRuntime.kt:71` `val sc = ctx.initStandardObjects()`（非 `initSafeStandardObjects`）；全文件无 `setClassShutter`。
- 佐证：`plugin/JsRuntime.kt:148-154` 注入 `classLoader`；`:209-223` `importClass` 用宿主类加载器 `Class.forName`；`:272-305` `loadJs/loadJsLib` 可加载任意磁盘路径脚本。
- 影响：JS 插件可 `importClass("java.lang.Runtime")` 或访问 `Packages.com.tencent...`，等价于任意代码执行。
- 建议：改用 `initSafeStandardObjects()` + 注册 `ClassShutter` 白名单；`loadJs/loadJsLib` 限制在插件目录内并校验来源。

#### S-H3　插件 API 直接暴露 QQ 凭据与危险原语
- 证据：`plugin/api/PluginMethod.java`
  - 凭据：`:255` `getSkey()`、`:259` `getPskey(url)`、`:263` `getRealSkey()`、`:267` `getStweb()`、`:271` `getPt4Token(url)`、`:283` `getGroupRKey()`、`:287` `getFriendRKey()`（实现见 `utils/qq/CookieTool.java:40-128`）。
  - 反射：`:601` `callMethod`、`:605` `callStaticMethod`、`:616/620` `get/setFieldValue`、`:624` `newInstance`、`:635` `findClass`、`:645/652` `findMethod`、`:659` `findField`。
  - 文件：`:820` `deleteFile`、`:863` `readFile`、`:884` `writeFile`、`:900` `appendFile`。
  - 网络：`:390/404` `httpGet`、`:418/432` `httpPost`、`:446` `downloadFile`。
  - 动态代码：`:118` `loadJar`、`:129` `loadDex`；Hook：`:794/807` `hookAfter/hookBefore`。
- 上述方法被 `plugin/JsRuntime.kt:161-178` 与 `plugin/PluginCompiler.java:258-274` 全部注册为脚本可直接调用的全局函数。
- 影响：脚本无需「逃逸」即可拿到可劫持会话的票据，并具备任意反射/文件/网络能力——当前设计下最直接的敏感数据外泄面。
- 建议：凭据类 API 默认关闭或纳入显式授权；反射 API 加目标白名单；文件 API 限制根目录在插件目录内。

#### S-H4　在线插件下载无完整性/签名校验即可加载执行
- 证据：`ui/services/OnlinePluginService.kt`
  - `:22` `BASE_URL = "https://v.yuafeng.cn/QEdge"`；`:88` `URL("$PLUGIN_DOWNLOAD_URL?id=$pluginId")`。
  - `:99-117` 下载 → 落到 `File(QQCurrentEnv.getCurrentDir(), "plugin")` → `extractPluginZip(...)`；`:136` `ZipUtil.unzip(...)`。
  - `:144-147` 仅校验「目录非空」，**无签名/哈希/发布者校验**。
- 影响：服务端被入侵、DNS/网络劫持或响应被替换时，模块直接加载执行攻击者插件，且该插件拥有 S-H3 的全部能力。
- 建议：插件包强制作者私钥签名、模块内置公钥验签，验签失败拒绝解压与加载；至少提供签名哈希强校验。

#### S-H5　插件代码/脚本从外部存储加载，可被本地篡改
- 证据：`utils/HostInfo.kt:83-85` → `_moduleDataPath = "${externalDir.absolutePath}/QEdge/"`（`getExternalFilesDir(null)?.parentFile`）；`utils/qq/QQCurrentEnv.java:307-317` `getCurrentDir()` 返回该路径；`plugin/PluginManager.java:30-44` `pluginDir = new File(QQCurrentEnv.getCurrentDir(), "plugin")`。
- 脚本入口：`plugin/PluginCompiler.java:69`（`main.java`）、`plugin/JsRuntime.kt:60`（`main.js`）。
- 影响：在 Android 10 及以下或具备相应访问能力的场景，本地文件管理器/USB 可替换插件目录下的 `main.java`/`main.js`，即可在宿主进程内植入任意代码（结合 S-H1/S-H2/S-H3 取得完整权限）。
- 建议：可执行脚本与插件迁移到应用内部私有目录（`getFilesDir()`），并对加载脚本做哈希/签名校验；外部存储仅作只读展示。

### 1.2 中危

| 编号 | 问题 | 证据 | 建议 |
|---|---|---|---|
| S-M1 | `HttpUtils.download` 用服务器可控 `Content-Disposition.filename` 拼接落盘路径，未过滤 `../`，可路径穿越写出 | `utils/HttpUtils.java:84-90`、`:129` | 取 `File(name).getName()`，落盘前校验 `canonicalPath` 前缀 |
| S-M2 | 在线插件用服务器下发 `pluginName` 拼接解压目录，可 `../` 逃逸出 `plugin/`（随后被 `deleteDir`/`mkdirs`） | `ui/services/OnlinePluginService.kt:114-117`；`pluginName` 来源 `:60` | `pluginName` 白名单校验 + 目录规范化前缀校验 |
| S-M3 | 配置/会话令牌明文 JSON 落外部存储（`.dat` 仅后缀伪装） | `utils/ModuleConfig.kt:15-16`、`utils/JsonConfigUtils.java:341-346`、`coldrain/ColdRainCore.java:1049-1050` | 敏感项用 Keystore/加密存储；或迁内部私有目录 |
| S-M4 | 日志落盘外部存储且未脱敏（含 UIN、URL、异常堆栈） | `utils/LogUtils.java:77-81`、`:98-146` | 日志迁内部目录；UIN/URL/令牌脱敏；加开关与自动清理 |
| S-M5 | `allowBackup=true` 且备份/迁移规则为空，数据可被 ADB 备份/云备份带走 | `AndroidManifest.xml:11-21`、`res/xml/backup_rules.xml`、`res/xml/data_extraction_rules.xml` | 改 `allowBackup="false"` 或显式 `<exclude>` |
| S-M6 | 动态加载 Jar/Dex 且校验薄弱（`isValidJar` 仅查是否含 `.class`，无签名校验） | `plugin/api/PluginMethod.java:118,129`、`utils/JarLoader.java:65,77` | 禁止脚本直接加载任意路径；`isValidJar` 增加签名校验 |

### 1.3 低危

| 编号 | 问题 | 证据 | 建议 |
|---|---|---|---|
| S-L1 | `MainActivity` 导出且无权限保护（LAUNCHER 必需，但可被任意应用拉起并触发更新检查） | `AndroidManifest.xml:22-35`、`MainActivity.kt:75` | 保持导出，但可校验调用方或限定处理 `MODULE_SETTINGS` |
| S-L2 | 寄生/动态 Activity 注册可伪造 `ActivityInfo`，脚本可注册并拉起界面（UI 欺骗面） | `lifecycle/Parasitics.kt:80-205`、`lifecycle/CounterfeitActivityInfoFactory.java:16-38`、`plugin/api/PluginMethod.java:133,355-388` | 对脚本默认关闭 `registerActivity`；动态 Activity 类来源白名单 |
| S-L3 | 主动削弱宿主安全能力：关闭网页安全 OCR 检测、解除风险网页拦截 | `hook/item/DisableWebSecurityCheck.java:33-41`、`hook/item/RemoveRiskWebpageBlock.kt:36-58` | 默认关闭 + 二次确认，UI 明示风险 |

### 1.4 已具备防护 / 未发现问题（避免误报）

- **Zip Slip 已正确防护**：`utils/ZipUtil.java:74-79` 对压缩包条目做 `canonicalPath` 前缀校验；`ui/pages/file/FileManagerUtils.kt:230-237,376-380` 同样校验。
- **HTTPS 通道无证书校验缺陷**：`utils/HttpUtils.java` 全程默认 `HttpURLConnection`，无自定义 `TrustManager`/`HostnameVerifier`；URL 均为 `https://`；清单未声明 `usesCleartextTraffic`。
- **无硬编码 API Key/密钥/口令**：全项目检索 `api_key/apikey/secret/password/pwd` 无命中。
- **无命令执行面**：无 `Runtime.getRuntime().exec` / `ProcessBuilder`（注：仍可经 S-H3 的 `callStaticMethod` 间接达成）。
- **无 SQL 注入面**：未发现 `SQLiteDatabase`/`rawQuery`/`execSQL`。
- **模块未自建 WebView**：`addJavascriptInterface`/`setJavaScriptEnabled` 零命中。

---

## 2. 性能与并发

> 模块宣称「零卡顿、Hook 全部 O(1)」，经实证**均不成立**（见 §2.4）。

### 2.1 高危

#### P-H1　冷启动主线程同步执行缓存读盘 + 反射校验 + Hook 注册
- 证据：`hook/XposedEntry.java:206-233` 在 `BaseApplicationImpl.onCreate` 的 `afterHookedMethod`（冷启动主线程）内同步执行 `DexKitCache.initCache()` / `validateAllTasks()` / `MainHook.loadHook()`；`hook/MainHook.java:131-173` 同步执行 `registerHookItems()`、`FromServiceMsgDispatcher.loadHook()`、`hookAccountChange()`；`utils/dexkit/DexKitCache.kt:57-72`（`cacheFile.readText()`）、`:104-126`（逐任务反射取 `TAG`）。
- 影响：冷启动关键路径同步完成磁盘 IO + 反射遍历 + `Class.forName`/`getDeclaredMethod` 多次，直接延迟首帧。
- 建议：将缓存加载/校验/服务分发/账号切换整体投递到 `ModuleScope.launchHook()`；主线程仅保留 `initialized.compareAndSet` 与必要 Hook 点注册。

#### P-H2　`BaseApiHookItem.listenerSet` 为裸 `HashSet`，消息热路径遍历且懒初始化无同步
- 证据：`hook/base/BaseApiHookItem.java:11-45`（`listenerSet` 懒初始化、`forEachChecked` 遍历）；调用点 `hook/api/OnReceiveMsg.java:55-68`、`OnSendMsg.java:59-74`、`OnMenuBuild.kt:47-76`。
- 影响：插件加载/卸载的 `addListener/removeListener` 与消息回调遍历并发时可能抛 `ConcurrentModificationException`，中断整条消息处理。
- 建议：改 `CopyOnWriteArraySet`；懒初始化加双检锁或 `volatile`。

#### P-H3　`PluginManager` 公开静态可变集合无同步，与遍历并发
- 证据：`plugin/PluginManager.java:22-26`（`public static final List/Map`）、`:38-59` `loadAll()` 先 `clear()`、`:177-203` 自动加载线程遍历；读点 `plugin/view/ChatSettingLoader.kt:154,210+`。
- 影响：UI/回调线程 `filter` 遍历与 `loadAll().clear()` 并发 → CME 或读到空列表/半加载态。
- 建议：改 `CopyOnWriteArrayList`/`ConcurrentHashMap`，对外只暴露不可变快照。

### 2.2 中危

| 编号 | 问题 | 证据 | 建议 |
|---|---|---|---|
| P-M1 | HomeScreen 组合期 60+ 次 `ModuleConfig` 读取，每次触发 `synchronized` + `exists()` + `lastModified()` 磁盘 stat，构成首帧卡顿 | `ui/pages/HomeScreen.kt:202-325`；`utils/JsonConfigUtils.java:97-102,178-192` | `Dispatchers.IO` 一次性读整表后再构建 State；或为 stat 加 200~500ms 节流 |
| P-M2 | HomeScreen 组合期同步解码全屏背景大图 | `ui/pages/HomeScreen.kt:329-335`、`:135-173` | 先渲染主题色，后台解码完成再切图/降采样 |
| P-M3 | HomeScreen 每个开关回调新建裸线程（该文件 `Thread {` 约 120 处），而 `putXxx` 本身仅内存写 + 300ms 去抖落盘 | `ui/pages/HomeScreen.kt:655-713`；`utils/JsonConfigUtils.java:162-171` | 改 `ModuleScope.launchIOJava` 或直接主线程调用 |
| P-M4 | `ColdRainCore` 每条消息多次 `lastModified` stat + 回复链内**同步 HTTP**，运行于消息回调线程 | `coldrain/ColdRainCore.java:624-631,383-471,932,967+` | 入口一次性快照配置；网络调用异步投递 |
| P-M5 | `ChatSettingLoader`/`MediaPanelLoader` 每次 `onAttachedToWindow` 读配置（聊天流每个 ImageView 各一次 stat） | `plugin/view/ChatSettingLoader.kt:93-97`、`MediaPanelLoader.kt:118-122` | 配置变更时缓存到内存字段，回调读内存 |
| P-M6 | `DexKitCache.cacheMap` 为普通 `mutableMap`，启动/IO 协程/各 Hook 线程并发读写 | `utils/dexkit/DexKitCache.kt:13,23-40,63-66`；写入点 `DexKitFinder.kt:139-163` | 改 `ConcurrentHashMap` |
| P-M7 | LevelBoost 每个任务 fire-and-forget 裸线程 + `Thread.sleep` + `latch.await(15s)`，单 tick 最多并发 6 条 | `hook/item/LevelBoost.kt:312-475`（`:427-442` latch） | 统一走 `ModuleScope` 或专用串行调度器；协程替代 sleep/await |
| P-M8 | `HourlyChimeFeature` 静态非 daemon Timer 且永不 `cancel()` | `coldrain/features/HourlyChimeFeature.java:20-21,77-92` | `new Timer("QEdge-HourlyChime", true)`；关闭时 cancel |
| P-M9 | `HeartbeatManager` 非 daemon Timer + 10min 周期同步网络；`scheduleAtFixedRate` 超时会补跑堆积 | `hook/HeartbeatManager.java:57,73-117` | daemon 化；改 `schedule` 或跳过未完成任务 |
| P-M10 | `OnReceiveMsg` 缓存未命中时新建 `DexKitBridge` 全量扫描（注释自述 5~6s）+ `ClassLoader.packages` 强反射兜底 | `hook/api/OnReceiveMsg.java:149-177,208-238` | 统一走 IO 协程并复用单 bridge；兜底仅在非首启启用 |
| P-M11 | `MainHook.processDataForCurrent` 每个 item 每次重新 `getDeclaredMethod("initData")` | `hook/MainHook.java:297-320` | `Method` 提为 `static final` 或走 `ReflectCache` |
| P-M12 | `parseContact`/`parseToPluginContact` 每次调用都 `Pattern.compile`，位于会话 UI Hot 路径 | `plugin/view/ChatSettingLoader.kt:170-172`、`MediaPanelLoader.kt:173-176` | 正则提为对象级 `val` 常量 |

### 2.3 低危

| 编号 | 问题 | 证据 |
|---|---|---|
| P-L1 | `ModuleScope.postToMain` 每次新建 `Handler` | `common/ModuleScope.kt:80-83` |
| P-L2 | `KeepAliveHook` 每 3s 主线程自 `postDelayed` 刷新通知 | `hook/item/KeepAliveHook.kt:37,51-66` |
| P-L3 | `ReflectCache`/`ReflectUtils` 反射缓存无容量上限 | `utils/reflect/ReflectCache.kt:15-17`、`utils/ReflectUtils.java:92,141` |
| P-L4 | `ReflectUtils` 三个 `findMethod/findConstructor` 重载未走缓存 | `utils/ReflectUtils.java:96-111,145-187,189-227` |
| P-L5 | `MediaPanelContent.gifCache` 无上限（同文件 `videoFrameCache`/`thumbCache` 已是 LruCache） | `ui/pages/media/MediaPanelContent.kt:2456` |
| P-L6 | `ObjectStore` 文件流未 try-with-resources | `utils/ObjectStore.java:35-37,73-75` |
| P-L7 | `MainScreen.railActions` 每次组合重建列表 | `ui/pages/home/MainScreen.kt:35-63` |
| P-L8 | `MediaPanelContent` 权限轮询最长占用 10s | `ui/pages/media/MediaPanelContent.kt:2353-2374` |
| P-L9 | `RepeatMsg.loadCustomIcon` 首帧 stat + 两遍 `decodeFile`（已有缓存） | `hook/item/RepeatMsg.kt:64-88,169` |

### 2.4 对「零卡顿、Hook 全部 O(1)」的实证结论

- **「Hook 全部 O(1)」不成立**：`BaseApiHookItem.forEachChecked`（`hook/base/BaseApiHookItem.java:39-45`）对每条消息/每次菜单构建做 O(监听器数) 遍历；`FromServiceMsgDispatcher` 对每个推送包做同步 `ProtoData.fromBytes + toJSON`（`hook/api/FromServiceMsgDispatcher.java:90-173`）；`ColdRainCore.handleMessage` 每条消息做多次 stat 并遍历全部 features。
- **「零卡顿」不成立**：冷启动主线程存在同步缓存读盘/反射/Hook 注册（P-H1）；设置页首帧存在 60+ 次磁盘 stat（P-M1）与同步大图解码（P-M2）。
- **正向实践**：`common/ModuleScope.kt:27-34` 的 `Dispatchers.IO.limitedParallelism(2)` 限流调度器、`plugin/JsRuntime.kt:41-43` 每插件独占 daemon 单线程 Executor、`utils/dexkit/DexKitFinder.kt:106-129` 在 IO 协程复用单 bridge —— 这些已显著降低爆炸半径，可作为其余模块的改造范式。

---

## 3. 健壮性与资源

### 3.1 高危

#### R-H1　全局未捕获异常处理器「吞异常、不转交原 handler、不杀进程」
- 证据：`hook/kk/KKHook.java:175-187`
  ```java
  final Thread.UncaughtExceptionHandler defaultHandler =
      Thread.getDefaultUncaughtExceptionHandler();
  Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
      LogUtils.e("hookExit", "Uncaught exception in " + thread.getName() + ": " + throwable.getMessage());
      LogUtils.e(throwable);
  });
  ```
- `defaultHandler` 取出后从未被调用；处理器内既不转交原 handler、也不终止进程。异常被静默吞掉，进程可能留在不一致状态（脏内存/死锁/无响应）。
- 建议：日志后调用 `defaultHandler.uncaughtException(thread, throwable)` 交还系统语义，或显式 `Process.killProcess(myPid())`。

#### R-H2　`CounterfeitActivityInfoFactory` 抛未捕获异常穿透 IPackageManager 代理
- 证据：`lifecycle/CounterfeitActivityInfoFactory.java:16-38`——`:18` `getHostContext()` 未判空（null 则 `:26` NPE）；找不到候选 Activity 时 `:34` `throw new IllegalStateException(...)`，该异常沿 `lifecycle/Parasitics.kt:179-204` 的 `Proxy` 冒泡到宿主 `IPackageManager` 调用点。
- 影响：直接崩溃宿主。
- 建议：整个工厂方法 try/catch 包裹，失败返回 null 或回退 stub；代理侧对 null 兜底，禁止异常穿透 Binder 代理。

#### R-H3　`Parasitics` 用 `!!` + `as` 强转，失败即崩宿主
- 证据：`lifecycle/Parasitics.kt:324`
  ```kotlin
  Parasitics::class.java.classLoader!!.loadClass(className).newInstance() as Activity
  ```
- 影响：`classLoader` 为 null → NPE；实例非 `Activity` → `ClassCastException`；均未捕获。
- 建议：改 `as? Activity` 并降级。

#### R-H4　`Parasitics.invokeOriginal` 重抛 targetException 穿透代理
- 证据：`lifecycle/Parasitics.kt:206-212` `catch (e: InvocationTargetException) { throw e.targetException }`。
- 影响：被代理方法抛出的原始异常直接穿透代理层，宿主侧无捕获即崩溃。
- 建议：代理层统一捕获记录，必要时返回安全默认值。

#### R-H5　DexKit 缓存查不到即抛异常，无兜底
- 证据：`utils/dexkit/DexKitCache.kt:43-54`（`throw ClassNotFoundException(key)` / `throw NoSuchMethodException(key)`）、`utils/dexkit/DexKitTask.kt:14-20`（`requireClass/requireMethod`）、`utils/dexkit/DexKitFinder.kt:57-59`（`getDeclaredMethod("doOnCreate")` 于 `doFind()` 调用处无 try/catch）。
- 影响：QQ/TIM 版本变化导致类/方法缺失时，若调用方未捕获，直接崩溃宿主。
- 建议：提供返回 `null` 的 `findOrNull` 变体，调用点强制判空降级并记录。

#### R-H6　主 QQ 路径无全局崩溃兜底
- 证据：全仓唯一 `setDefaultUncaughtExceptionHandler` 在 `hook/kk/KKHook.java:175-187`（且语义错误，见 R-H1）；`hook/MainHook.java` / `hook/XposedEntry.java` 主 QQ 注入路径**无任何全局 handler**。
- 建议：主入口安装**保护性** handler：记录模块相关堆栈并**转交原始 handler**（不篡改崩溃语义）。

### 3.2 中危

| 编号 | 问题 | 证据 |
|---|---|---|
| R-M1 | 插件异常堆栈被永久丢弃（`printStackTrace` 被注释，`StringWriter` 恒为空） | `plugin/PluginError.java:39`（`// e.printStackTrace(pw);`） |
| R-M2 | Hook 回调异常无内层保护，直接穿透 XposedBridge | `utils/HookUtils.java:30-46`、`utils/hook/HookExtensions.kt:9-44` |
| R-M3 | 静默吞异常集中点（`catch (IOException ignored) {}` 等） | `utils/HttpUtils.java:44,145,218,257,294,332,369`；`hook/kk/KKHook.java:464,473,479,498`；`coldrain/features/AtFeature.java:229,245,259`；`utils/HybridClassLoader.java:69,83,90` |
| R-M4 | `ColdRainCore` 多处文件 IO 非 try-with-resources（异常路径句柄泄漏） | `coldrain/ColdRainCore.java:130-136,153-160,176-178,267-273,288-295,306-308` |
| R-M5 | `MusicMenuFeature` 音乐模式文件读写非 try-with-resources | `coldrain/features/MusicMenuFeature.java:604-618,620-631` |
| R-M6 | `HttpURLConnection` 未 `disconnect()`（连接泄漏） | `utils/HttpUtils.java:17-47,197-221,227-260,266-297,303-335,337-372` |
| R-M7 | `download` 递归重定向无深度上限（可 `StackOverflowError`） | `utils/HttpUtils.java:74` |
| R-M8 | `ReflectUtils` 缺判空（`assert` 在生产禁用后 NPE；`findMethod` 未判 `clazz==null`） | `utils/ReflectUtils.java:52,96-111` |
| R-M9 | `ColdRainCore.getConfigInt` 未判 `configData` 为 null | `coldrain/ColdRainCore.java:249-254` |
| R-M10 | `MsgTool` 下载失败返回 null 后直接构造 `File` | `utils/qq/MsgTool.java:381-393,400,414,428` |
| R-M11 | `MainHook` 构造器数组下标无长度检查 | `hook/MainHook.java:261` |
| R-M12 | Hook 初始化非原子（`initialized` 非 volatile，先判后置非原子）→ 可重复初始化 | `hook/MainHook.java:75,131-133`；`hook/kk/KKHook.java:35,37-45` |
| R-M13 | `initSwitchHookItem`/插件加载无 try/catch；`lastPluginLoadTime` 竞态 | `hook/MainHook.java:125,209-216,236-243` |
| R-M14 | `Parasitics.initForStubActivity` 失败静默且不置位 | `lifecycle/Parasitics.kt:79-117` |
| R-M15 | 静态持有 `ResourcesLoader` 不释放 | `lifecycle/Parasitics.kt:281-287` |
| R-M16 | `PluginManager`/`HookRegistry` 公开静态集合无同步 | `plugin/PluginManager.java:22-26`；`hook/base/HookRegistry.java:13-15` |
| R-M17 | `DexKitCache.cacheMap` 非线程安全 | `utils/dexkit/DexKitCache.kt:13` |
| R-M18 | `ColdRainCore.configData/features` 无同步（`initialized` 非 volatile） | `coldrain/ColdRainCore.java:63,99-114,150-165` |
| R-M19 | `MusicMenuFeature.searchMusicList` 静态 `HashMap` 在多 IO 线程并发读写 | `coldrain/features/MusicMenuFeature.java:29` |
| R-M20 | Timer 非 daemon + 任务内异常静默 | `coldrain/features/HourlyChimeFeature.java:80-91`；`hook/HeartbeatManager.java` |
| R-M21 | 日志/Toast 自身异常安全缺失（兜底路径的兜底仍可能抛异常） | `utils/LogUtils.java`、`utils/Toasts.java`、`plugin/JsRuntime.kt` |

### 3.3 低危

| 编号 | 问题 | 证据 |
|---|---|---|
| R-L1 | `PluginManager`/`ObjectStore` 文件写入非 try-with-resources | `plugin/PluginManager.java`、`utils/ObjectStore.java` |
| R-L2 | 多处非 volatile 可见性标志位 | `coldrain/features/HourlyChimeFeature.java:21`、`utils/qq/MsgTool.java:36,38-47`、`utils/HookUtils.java:16`、`utils/HeartbeatManager.java` |
| R-L3 | 其它 Kotlin `!!` 命中点待复核 | `hook/api/OnMenuBuild.kt:41`、`ui/core/compatibility/XposedComposeDialog.kt:100`、`ui/pages/FileManagerScreen.kt`（多处） |

### 3.4 经核验确认无问题（避免误报）

- `utils/ZipUtil.java`（Zip Slip 校验 + `ZipFile` 资源规范）
- `plugin/JsRuntime.kt`（Rhino `Context.enter/exit` 配对正确）
- `ui/pages/file/AudioPlayerScreen.kt:93-117`、`AudioPlayerDialog.kt:101-137`（`DisposableEffect` + `MediaPlayer.release()` 正确）
- `utils/qq/MsgTool.java:302-315,646-690,768-778`（`MediaPlayer`/`MediaMetadataRetriever`/`FileOutputStream` 已 finally 或 try-with-resources 释放）
- `utils/json/JsonConfigUtils.java`（内存缓存 + 去抖落盘 + 原子 rename 设计良好）
- `lifecycle/DynamicActivityRegistry.kt`（`ConcurrentHashMap`）
- `coldrain/features/StatusFeature.java:101,131`（`registerReceiver(null, ifilter)` 为 sticky 广播查询规范用法，非泄漏）
- `hook/XposedEntry.java`（`AtomicBoolean` 防重入 + 分阶段独立 try/catch 降级）
- `hook/api/OnReceiveMsg.java:100-239`（DexKit 缓存 → 硬编码 → 扫描三级回退）

---

## 4. 架构与设计

### 4.1 高危

| 编号 | 问题 | 证据 | 建议 |
|---|---|---|---|
| A-H1 | `MainHook.java` 上帝类：注册 45 个 HookItem + 业务编排 + 插件 UI CRUD（`getPluginList`/`deletePlugin`/`createPlugin`…）混于一身（431 行） | `hook/MainHook.java:77-123,131-207,332-430` | 拆为 `HookRegistrar` / `HookPipeline` / `PluginFacade` |
| A-H2 | `XposedEntry.java` 上帝类：8 个近乎复制粘贴的 `hookXxx(app)`（277-416 行），包名/Application 类名多处硬编码（418 行） | `hook/XposedEntry.java:37,72-79,81-132,165-204,277-416` | 抽 `ThirdPartyAppAdapter` 数据表 + 统一 `hookThirdParty(app)` 模板 |
| A-H3 | 两套互不复用的配置系统并行：`JsonConfigUtils`/`ModuleConfig` 与 `ColdRainCore` 自持 `config.dat` | `coldrain/ColdRainCore.java:55,65-84,509-530,562-580` | 冷雨配置切到 `ModuleConfig`；下拉/中文名映射抽常量表 |
| A-H4 | 45 个 HookItem 手工逐行注册，`@HookItemAnnotation` 注解体系定义了但未参与自动发现 | `hook/MainHook.java:77-123`（45 次 `HookRegistry.register`）；`hook/annotation/HookItemAnnotation.java` | 注解处理器或运行期扫包自动注册 |
| A-H5 | 脚本双引擎（BeanShell/Rhino）无统一抽象，`start`/`startJs`/`stop`/API 注入/回调注册全部双路分叉 | `plugin/PluginCompiler.java:57,110,129,167,258`；`plugin/JsRuntime.kt:162` | 定义 `ScriptEngine` 接口，两引擎各作实现 |
| A-H6 | 大量可变全局静态状态，`HostInfo.init()` 内 `runCatching` 静默失败、初始化顺序靠调用时机隐式保证 | `utils/HostInfo.kt`；`hook/base/HookRegistry.java:13-15`；`plugin/PluginManager.java:22-26` | `HostInfo` 失败路径显式上报；引入 `ModuleBootstrap` 显式初始化序列 |
| A-H7 | DexKit 加载方式不统一：`KKHook`/`KuGouHook` 直接 `System.loadLibrary("dexkit")`，绕过 `DexKitManager` 的绝对路径回退 | `hook/kk/KKHook.java:154,337`；`hook/kugou/KuGouHook.java:170`；对照 `utils/dexkit/DexKitManager.java:57` | 全部改用 `DexKitManager.ensureLibrary()` |
| A-H8 | 单 ABI 限制 `arm64-v8a`，叠加 32 位设备拒绝逻辑，适配面进一步收窄 | `app/build.gradle.kts:31`；`hook/XposedEntry.java:165-204` | 如需覆盖 32 位补齐 `armeabi-v7a` 的 so；否则在 README 明确声明 |
| A-H9 | 文档与代码在「注册方式 / 脚本接口 / isApplicable」三处实质性偏差 | 详见 §5 | 更新文档或补齐实现 |

### 4.2 中危

| 编号 | 问题 | 证据 | 建议 |
|---|---|---|---|
| A-M1 | 配置键反向依赖 UI：`ModuleConfig.` 全项目命中约 206 处 / 30 文件，`HomeScreen.kt` 独占约 126 处，UI 直接持有键字符串 + 默认值 | `ui/pages/HomeScreen.kt:202-244` | 建集中 `ConfigKeys` 常量 + 类型化访问器 |
| A-M2 | Hook 基类相互 `instanceof`，父类下探子类，形成环状耦合 | `hook/base/BaseApiHookItem.java:39-45`；`hook/base/BaseSwitchHookItem.java:44-49`；`hook/base/BaseClickableHookItem.java`（仅 10 行） | 以接口（`Enableable`/`ClickableConfig`）替代 `instanceof` |
| A-M3 | 配置键全为魔法字符串，无集中定义/类型安全/版本迁移；外部服务地址与 Q群管家 uin 硬编码 | `utils/ModuleConfig.kt`（全 `String key`）；`coldrain/ColdRainCore.java:2854196310,891-899,932` | 集中 `ConfigKeys` + `version`/`migrate()`；外部地址抽常量 |
| A-M4 | `isEnable`(字段)/`isEnabled()`/`isAvailable()`/`isApplicable()` 四套语义并存 | `hook/base/BaseHookItem.java:11,26-28`；`hook/base/BaseSwitchHookItem.java:12-13`；`hook/item/TimArkCardBypass.kt:32-36` | 统一三层契约：`isApplicable`(环境)/`isEnabled`(开关)/`isAvailable`(初始化态) |
| A-M5 | `HookRegistry` 静态可变集合，`register` 无同步 | `hook/base/HookRegistry.java:13-15,17-25` | 注册阶段单线程化或改 `CopyOnWriteArrayList` |
| A-M6 | `PluginMethod` 1139 行、约 180+ 扁平 public API；错误处理仅 log 后重抛；内嵌宿主分支 | `plugin/api/PluginMethod.java:519-535,1099-1105` | 按域拆 `MsgApi`/`FileApi`/`ReflectApi` 等门面；统一结果对象 |
| A-M7 | 插件生命周期/隔离/卸载/版本管理缺失（无版本号、无依赖校验） | `plugin/PluginManager.java`；`plugin/FixClassLoader.java:60` | 引入插件清单版本与隔离卸载状态机 |
| A-M8 | 第三方适配重复：`AoRuanHook.hookVip` 五段重复、`KuGouHook` 两组近似、300+ 行签名数组内联源码、`IFlyHook.hookNativeLoad` 死代码 | `hook/aoruan/AoRuanHook.java:33+`；`hook/kugou/KuGouHook.java:33-157,260,295,330,358`；`hook/ifly/IFlyHook.java:100` | 抽 `hookVipMethods(clazz, methods, value)`；签名数据移入资源；清理死代码 |
| A-M9 | 签名配置健壮性：`local.properties` 缺失即 `error()` 硬失败、keystore 路径硬编码、debug 复用 release 签名 | `app/build.gradle.kts:9-15,37,57` | keystore 路径可配置且可空回退；debug 用独立签名 |
| A-M10 | R8 策略：`-dontobfuscate` + 整包 `-keep`，缺 `-keepattributes Signature`（影响泛型反射） | `app/proguard-rules.pro` | 按需 `-keep`；补 `Signature` |
| A-M11 | 构建脚本硬编码宿主包名 | `app/build.gradle.kts:130` | 改为属性注入 |
| A-M12 | 重复代码块与超长方法集中 | `hook/item/LevelBoost.kt:211-321`；`coldrain/ColdRainCore.java:383-471,738-821,1144-1189` | 抽方法、消除重复 |

### 4.3 低危

| 编号 | 问题 | 证据 |
|---|---|---|
| A-L1 | `BaseHookItem` 无 `isApplicable`，文档描述不符（实际为 `isInTargetProcess`） | `hook/base/BaseHookItem.java:17-24`；`utils/dexkit/DexKitTask.kt:12` |
| A-L2 | `BaseSwitchHookItem` 空模板方法（`saveData()` 从未被调用）+ 基类直连 `ModuleConfig` | `hook/base/BaseSwitchHookItem.java:67-79` |
| A-L3 | `FixClassLoader` 无缓存、`PluginManager` 轮询线程无生命周期收口 | `plugin/FixClassLoader.java:60`；`plugin/PluginManager.java:177-188` |
| A-L4 | `HostInfo` 职责过重（包名/进程/数据路径/主题/Context 混一），主题判断重复、自身包名硬编码 | `utils/HostInfo.kt`（160 行） |
| A-L5 | 依赖版本风险：Rhino 1.7.15 较老（ES6 支持有限）、`dalvikDx` 16.0.1 属运行期 dex 生成的重依赖 | `gradle/libs.versions.toml` |
| A-L6 | 死代码、注释掉的日志、命名不一致（`isEnable` vs `isEnabled`） | `hook/ifly/IFlyHook.java:100`；`hook/base/BaseSwitchHookItem.java:70`；`hook/item/TimArkCardBypass.kt:89,121,204` |
| A-L7 | `HookCategory` 自身被标注 `@HookItemAnnotation`（注解误用） | `hook/annotation/HookCategory.java` |

### 4.4 关于 `JsonConfigUtils` 备份机制的两点澄清（避免误导后续工作）

- 备份文件名一致，**不是缺陷**：`flush()` 写 `getConfigFile(...).getName() + ".bak"` 即 `<name>.dat.bak`，`loadConfig` 读取的 `configName + ".dat.bak"` 经 `getConfigFile`（返回 `configName + ".dat"`）核实完全一致。
- 但备份写入**未走 temp+rename**（主文件是原子的），且写入的是**与主文件相同的 snapshot**，因此「从备份恢复」对「写入内容本身非法」的场景无保护。可作为改进项（级别：低）。

---

## 5. 文档与代码一致性（CodeWiki.md / README.md）

| 文档声明 | 代码事实 | 证据 |
|---|---|---|
| `registerHookItems()` 静态注册 45 个 HookItem | 数量属实，但注解体系未参与自动发现，注册完全手工 | `hook/MainHook.java:77-123`；`hook/annotation/HookItemAnnotation.java` |
| 脚本系统暴露「同一套 180+ PluginMethod 宿主接口」 | 实际 BeanShell 与 Rhino 两条独立反射注入路径分叉 | `plugin/PluginCompiler.java:57,110,129,258`；`plugin/JsRuntime.kt:162` |
| 「必须 `isApplicable` 过滤」 | `BaseHookItem` 无 `isApplicable`，仅有 `isInTargetProcess`；`isApplicable` 来源是 `DexKitTask` | `hook/base/BaseHookItem.java:17-24`；`hook/item/TimArkCardBypass.kt:38` |
| 「Hook 全部 O(1)、零卡顿」 | 不成立（见 §2.4） | `hook/base/BaseApiHookItem.java:39-45`；`hook/XposedEntry.java:206-233` |
| 分层：UI → 业务 → Hook → DexKit → 基础设施 | `MainHook` 反向承载 UI 插件接口；`ModuleConfig` 被 UI 直连约 126 处 | `hook/MainHook.java:332-430`；`ui/pages/HomeScreen.kt:202-325` |
| 禁 SharedPreferences / 禁 `Build.CPU_ABI` / 禁 `MODE_MULTI_PROCESS` | 未发现违反（配置走 JSON） | `utils/JsonConfigUtils.java` |

**结论**：文档描述的是「目标架构」，代码在 Hook 注册、脚本系统、配置系统三处存在实质回退。建议二选一——补齐实现，或按第 §4 的结论修订文档。

---

## 6. 修复优先级路线图

### 阶段一：止血（消除「可被利用 + 可崩宿主」）

1. **切断代码注入链**：插件/脚本迁移到内部私有目录（`getFilesDir()`），外部存储仅只读展示 —— S-H5、S-M3、S-M4。
2. **插件完整性校验**：在线下载与本地加载均强制签名/哈希验签 —— S-H4、S-M6。
3. **修复崩溃风险链**：`CounterfeitActivityInfoFactory` 全包裹、`Parasitics.kt:324` 改 `as?`、`invokeOriginal` 不重抛、DexKit 提供 `findOrNull` —— R-H2~R-H5。
4. **纠正全局异常策略**：`KKHook` 处理器转交原 handler；主 QQ 入口补保护性 handler —— R-H1、R-H6。

### 阶段二：收敛暴露面（脚本最小权限）

5. Rhino 改 `initSafeStandardObjects` + `ClassShutter`；BeanShell 装白名单 `SecurityGuard` —— S-H1、S-H2。
6. 凭据 API（`getRealSkey`/`getSkey`/`getPskey`…）默认关闭或需授权；反射/文件 API 加白名单与目录约束 —— S-H3。
7. 路径穿越修复：`HttpUtils.download` 文件名取 basename、`pluginName` 白名单 —— S-M1、S-M2。

### 阶段三：性能与稳定性

8. 冷启动重活移出主线程 —— P-H1；配置读取加 stat 节流 / HomeScreen 一次性读表 —— P-M1。
9. 并发容器替换裸集合 —— P-H2、P-H3、P-M6、R-M16~R-M19。
10. 文件句柄 try-with-resources、`HttpURLConnection.disconnect()`、Timer daemon 化 —— R-M4~R-M6、R-M20、P-M8、P-M9。
11. 恢复 `PluginError` 堆栈输出、消除静默 catch —— R-M1、R-M3。

### 阶段四：架构重构（中长期）

12. 拆 `MainHook`/`XposedEntry` 上帝类、`ThirdPartyAppAdapter` 数据表驱动 —— A-H1、A-H2。
13. 统一配置系统与 `ConfigKeys` 常量 —— A-H3、A-M1、A-M3。
14. 注解驱动 Hook 自动注册；`ScriptEngine` 抽象统一双引擎 —— A-H4、A-H5。
15. 统一 DexKit 加载入口；按需 R8 规则 —— A-H7、A-M10。
16. 修订 CodeWiki.md / README.md 中与实现不符的描述 —— §5。

---

## 附录：本次审查中已确认「无问题」的项

| 项 | 证据 | 说明 |
|---|---|---|
| Zip Slip 防护 | `utils/ZipUtil.java:74-79`、`ui/pages/file/FileManagerUtils.kt:230-237` | `canonicalPath` 前缀校验正确 |
| TLS 校验 | `utils/HttpUtils.java` | 默认 `HttpURLConnection`，无降级校验；URL 全为 HTTPS |
| 硬编码密钥 | 全项目检索 | 无 `api_key`/`secret`/`password` 命中 |
| 命令执行 | 全项目检索 | 无 `Runtime.exec`/`ProcessBuilder` |
| SQL 注入 | 全项目检索 | 无数据库操作 |
| WebView 注入 | 全项目检索 | 模块未自建 WebView |
| Rhino 上下文配对 | `plugin/JsRuntime.kt` | `Context.enter/exit` 成对 |
| Compose 播放器释放 | `AudioPlayerScreen.kt:93-117`、`AudioPlayerDialog.kt:101-137` | `DisposableEffect` + `release()` 正确 |
| 媒体资源释放 | `utils/qq/MsgTool.java:302-315,646-690,768-778` | finally / try-with-resources 正确 |
| 配置落盘原子性 | `utils/JsonConfigUtils.java`（主文件 temp+rename） | 设计良好（备份为非原子，见 §4.4） |
| DexKit 并发限流 | `common/ModuleScope.kt:27-34`、`utils/dexkit/DexKitFinder.kt:106-129` | `limitedParallelism(2)` + 单 bridge 复用 |
| XposedEntry 防重入 | `hook/XposedEntry.java` | `AtomicBoolean` + 分阶段独立降级 |
| 多级回退范式 | `hook/api/OnReceiveMsg.java:100-239` | 缓存 → 硬编码 → 扫描三级兜底 |

---

> 本报告基于静态源码分析，未进行动态运行验证。反射类 API 的实际可达范围受宿主 QQ/TIM 版本影响，但「脚本无有效沙箱」「插件代码可被本地篡改」等结论不依赖版本。