# QEdge

> 基于 **NT QQ / NT TIM** 的 Xposed 增强模块 + 在线脚本平台。  
> 模块化设计，所有功能开关独立可控；零卡顿设计原则：Hook 全部 O(1)，无遍历/无循环反射/无深度拷贝；配套 PHP 后台支持脚本上传、下载、反馈、赞助墙、用户中心。

---

## ✨ 功能总览

### 💬 聊天增强（QQ + TIM）

| 功能 | QQ | TIM | 说明 |
|------|:--:|:---:|------|
| 消息图片/视频/语音一键下载 | ✅ | ✅ | 长按菜单项，统一保存到 `/storage/emulated/0/Tencent/QEdge/{Pictures,Videos,PTT}/` |
| 闪照无限制查看 | ✅ | ✅ | 绕过"查看5秒后销毁"限制，仍可正常保存原图 |
| 半透明头像上传 | ✅ | ✅ | 上传 PNG 透明头像，支持 200×200 自动裁圆 |
| 自己发送的视频 → 泡泡消息 | ✅ | ❌（TIM 不支持泡泡查看） | 开关打开后，相册或拍摄的视频消息自动转成泡泡消息 Element 发送 |
| TIM Ark 卡片跳转白名单绕过 | ❌ | ✅ | 通过 DexKit 扫描 ark 组件包下所有 boolean 方法，HookAfter 强制返回 true，跳过 TIM 对非白名单卡片的阻断 |
| 音乐卡片 / 小程序卡片发送 | ✅ | ✅ | ExtraTool.sendMusicCard / sendMiniApp，直接构造 Ark Proto 发送 |
| 自定义透明头像 | ✅ | ✅ | 无限制，支持 PNG 透明头像|

### 🛰️ QQ 空间系列

| 功能 | 说明 |
|------|------|
| **空间秒赞** | 好友发说说推送到达即点赞，黑名单 & 冷却控制 |
| **空间秒评** | 好友说说推送到达即评论，评论内容可自定义（弹窗输入） |
| **名片自动回赞** | 收到"资料卡赞 N 次"推送 → 回赞 N 次（上限 20 防刷） |
| **定时发说说** | 用户自定义 HH:mm + 文本内容，到点自动发布，支持多进程幂等去重（三保险：主进程 Timer + SP 日 key + 成功才写标记） |

### ⚡ 等级加速类（每日 **00:00** 自动执行）

三项签到全部走独立 HTTP 接口，同一分钟内串行调度、各自独立的跨进程幂等 key：

- **空间等级签到** 
- **QQ 日签打卡**
- **大会员签到**

### 🧩 在线脚本平台（模块端 + PHP 后台）

- 脚本 ZIP 上传自动校验 `info.prop` 字段（脚本名 / ID / 版本号 / 作者），`desc.txt` 作为介绍从 ZIP 内部读取
- 同一脚本 ID + 版本号**无论作者都驳回**（数据库 `idx_plugin_version(plugin_id, version_code)` 唯一索引兜底）
- 管理员后台 `admin/plugins.php` 可编辑脚本名称 / 介绍 / 作者
- 用户后台 `user/` 电脑端侧滑栏同款布局：脚本下载、个人信息、反馈、赞助作者
- 用户反馈系统：用户提交反馈 → 管理员回复处理结果 → 用户侧可见处理状态与回复
- 赞助墙：微信/支付宝收款码（赞助说明出现在首页赞助墙列表）

### 🎨 模块 UI

- Material 3 动态色（Light/Dark 双主题 + OLED 纯黑）
- 首页卡片分组：聊天增强 → QQ 空间 → 资料卡 → 等级加速
- 所有设置项点击即时写入，无需退出页面
- 侧滑抽屉：首页 / 在线脚本 / 关于 / 开发者设置

### 🎧 第三方 APP 增强（非 QQ / TIM 作用域同样生效）

| APP | 包名 | Hook 能力 |
|-----|------|-----------|
| **KK 键盘** | `im.weshine.keyboard` | 解锁 SVIP / VIP，皮肤 / 字体 / 表情全部可用；关闭所有广告；DexKit 找退出拦截逻辑加全局兜底防崩溃 |
| **酷狗音乐（大字版 + 概念版）** | `com.kugou.android.elder`<br>`com.kugou.android.lite` | ① **跳过所有开屏广告**：GdtSplashActivity / AdContainerActivity / 大字版&概念版 gotoAd 直接跳主界面 `MediaActivity`|
| **傲软抠图** | `com.apowersoft.backgrounderaser` | 解锁 VIP：`VipManager.isVip` / `isVipValid` / `isExpire` / `isVipValidOrBalance` 全部返回 true；`getDeadlineDate` 改写为长有效期；抠图不限次数 |

---

## 📦 技术特性

1. **NT QQ / NT TIM 原生**
   - 完全不做旧版 QQ 适配，只针对 NT 架构的接口和协议
   - DexKit 动态查找类/方法，应对不同版本 QQ / TIM 的混淆差异

2. **零卡顿**
   - 所有 Hook 回调时间复杂度 O(1)
   - 避免 List 遍历、深度拷贝、循环反射；耗时操作（HTTP/IO）一律丢新 Thread
   - 定时任务仅主进程启动 Timer（通过 `HostInfo.processName == HostInfo.packageName` 判定），从源头减少子进程重复工作
3. **安全 / 字符集**
   - 后台 `require.php` 全局 `utf8mb4`，session 本地私有路径，`gc_maxlifetime / cookie_lifetime` 7 天 + 滑动窗口续期
   - 用户输入统一 PDO 预处理，防止 SQL 注入；XSS 通过 `htmlspecialchars(ENT_QUOTES, 'UTF-8')` 转义
   - ZIP 上传直接读取内部 `info.prop` / `desc.txt`，避免 multipart 中文字段编码问题

---

## 📋 支持版本

| 平台 | 推荐版本 | 包名 |
|------|----------|------|
| NT QQ | 8.9.58 - 9.3.xx | `com.tencent.mobileqq` |
| NT TIM | 3.9.0 - 4.1.0（已适配卡片跳转绕过） | `com.tencent.tim` |
| KK 键盘（全版本） | 最新版即可 | `im.weshine.keyboard` |
| 酷狗音乐（普通 / 大字 / 概念 三版） | 最新版即可 | `com.kugou.android` / `.elder` / `.lite` |
| 傲软抠图（全版本） | 最新版即可 | `com.apowersoft.backgrounderaser` |
| Android | 9.0 ~ 16 | — |
| Xposed 框架 | LSPosed / LSPatch / FPA / 原子 / 无极（Zygisk 模式）等 | — |

---

### 后台（PHP 7.3 + MySQL 5.7+ / MariaDB）

1. 上传 `QEdge后台/QEdge/` 全部文件到网站根目录
2. 修改 `QEdge/require.php` 中的数据库连接信息
3. 首次访问 `install.php` 创建数据表
4. 管理员默认账号密码：手动在 `users` 表里把 `permission` 改成 `admin`
5. 目录权限：`sessions/ cache/ upload/ backup/` 必须写入权限（`chmod 755` 或 777）

---

## 🧭 目录结构

```
QEdge/
├── app/
│   └── src/main/java/me/lengyu/qedge/
│       ├── hook/
│       │   ├── base/           # BaseHookItem / BaseApiHookItem 基类
│       │   ├── api/            # 通用 Hook 能力（OnMenuBuild / OnSendMsg / OnPush…）
│       │   ├── item/           # QQ/TIM 具体功能项（每个功能一个文件）
│       │   │   ├── QZoneSchedule.kt      # 定时任务调度器（三签到 + 定时说说）
│       │   │   ├── QZoneLikeTool.kt      # 空间 HTTP 接口封装（秒赞/秒评/签到/发说说/日签/大会员）
│       │   │   ├── TimArkCardBypass.kt   # TIM Ark 卡片白名单绕过
│       │   │   ├── AutoLikeBack.kt       # 名片自动回赞
│       │   │   ├── VideoToBubble.kt      # 视频转泡泡消息
│       │   │   ├── DownloadEmotion.kt    # 图片/视频/语音长按下载
│       │   │   └── FlashPicBypass.java   # 闪照绕过
│       │   ├── kk/                        # KK 键盘（im.weshine.keyboard）VIP/去广告 Hook
│       │   │   └── KKHook.java
│       │   ├── kugou/                     # 酷狗音乐（普通/大字/概念 三版）开屏跳过 + 乐固签名绕过
│       │   │   └── KuGouHook.java
│       │   └── aoruan/                    # 傲软抠图（com.apowersoft.backgrounderaser）VIP 解锁
│       │       └── AoRuanHook.java
│       ├── plugin/                       # 在线脚本引擎
│       │   ├── bean/MsgData.java
│       │   └── api/PluginMethod.java
│       ├── ui/pages/HomeScreen.kt        # 模块首页（Jetpack Compose）
│       └── utils/
│           ├── ModuleConfig.kt           # 跨进程 JSON SP
│           ├── HostInfo.kt               # 进程名/包名
│           └── qq/
│               ├── CookieTool.java       # skey / pskey / bkn 读取
│               ├── ExtraTool.java        # 音乐卡片 / 小程序卡片 / 消息工具
│               ├── FriendTool.java       # 好友操作（点赞/发送消息）
│               └── QQCurrentEnv.java     # 当前登录 QQ 号/Uin
│
└── QEdge后台/QEdge/
    ├── index.php                    # 首页（赞助墙 + 脚本列表）
    ├── require.php                  # 全局引入（DB + 会话 + 字符集）
    ├── admin/                       # 管理后台（侧滑栏布局）
    │   ├── index.php
    │   ├── plugins.php              # 脚本列表 + 编辑/删除
    │   ├── sponsor.php              # 赞助管理
    │   └── feedback.php             # 用户反馈处理
    ├── user/                        # 用户中心（电脑端侧滑栏同款）
    │   ├── index.php
    │   ├── my_plugins.php
    │   ├── upload.php               # 上传脚本
    │   ├── feedback.php             # 我提交的反馈 + 新增
    │   ├── sponsor.php              # 赞助作者（微信收款码）
    │   └── header.php / footer.php  # 公共头尾
    └── online_plugin/index.php      # 上传脚本的 HTTP 接口（模块端调用）
```

---

## ⚠️ 免责声明

1. 本项目**仅供学习交流**，请勿用于商业用途或违反腾讯用户协议的行为
2. 作者不对使用本模块导致的账号封禁、财产损失等承担任何责任
3. 脚本内容由用户上传，平台仅提供存储空间，脚本内容责任归上传者本人

---

<p align="center">Made with Java by LengYu</p>
