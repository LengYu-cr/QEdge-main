# 网页开发规范文档 · 纯白淡蓝主题

> 本文档用于指导前端开发，基于“纯白背景 + 淡蓝色调”视觉体系。
> **核心原则**：干净、清爽、无AI配色、无emoji、适配移动端+电脑端。


## 一、色彩系统

| 用途 | 色值 | 说明 |
|------|------|------|
| 页面背景 | `#ffffff` | 纯白主背景 |
| 卡片/区块背景 | `#ffffff` | 保持纯白，与背景统一 |
| 辅助背景（淡蓝） | `#f5faff` 或 `#f0f6fe` | 用于区块点缀、卡片hover |
| 主边框（淡蓝） | `#d9e8f7` 或 `#dce8f5` | 卡片、分割线 |
| 强调边框 | `#b8d0e8` 或 `#9bbde0` | hover状态、焦点 |
| 主标题文字 | `#1a2b3c` 或 `#1c3450` | 深色，保证对比度 |
| 正文文字 | `#1f3347` 或 `#1e2b3c` | 深灰蓝，易读 |
| 辅助文字 | `#3f5b77` | 次级信息 |
| 淡蓝点缀 | `#dceaff`、`#c7ddf5` | 标签、徽章、小面积强调 |
| 阴影 | `rgba(180, 200, 220, 0.15)` | 柔和投影，不抢眼 |


## 二、布局结构

### 2.1 整体容器
- 最大宽度：`1100px`
- 居中显示，左右留白
- 背景纯白，圆角 `20px` ~ `24px`
- 细边框 `1px solid #eaf0f8` + 柔和阴影

### 2.2 响应式断点

| 设备 | 断点 | 内边距调整 |
|------|------|-----------|
| 电脑端 | ≥ 681px | 容器 `padding: 2.5rem 2rem` |
| 平板/手机 | ≤ 680px | 容器 `padding: 1.6rem 1rem`，网格变为单列 |
| 小屏手机 | ≤ 420px | 进一步缩小内边距，字体适当调小 |

### 2.3 网格系统
- 电脑端：`grid-template-columns: 1fr 1fr`（两列）
- 移动端（≤680px）：`grid-template-columns: 1fr`（单列）
- 列间距：`1.8rem ~ 2rem`


## 三、组件规范

### 3.1 卡片（Card）
```css
.card {
  background: #ffffff;
  border-radius: 20px;
  padding: 1.6rem 1.6rem 1.8rem;
  border: 1px solid #e6eef9;
  box-shadow: 0 2px 8px rgba(190, 212, 235, 0.08);
}
.card:hover {
  border-color: #b8d0e8;
  background: #fbfdff;
}
```

3.2 标题

· 主标题：font-size: 2.2rem，font-weight: 550
· 副标题/小标题：font-size: 1.3rem，font-weight: 520
· 标题底部淡蓝下划线：border-bottom: 2px solid #dce8f5

3.3 徽章/标签（Badge）

```css
.badge {
  background: #e5f0fc;
  color: #1f4970;
  padding: 0.15rem 0.9rem;
  border-radius: 40px;
  border: 1px solid #c7ddf5;
}
```

3.4 列表

· 无默认项目符号，使用淡蓝圆点 ·（color: #4a7aab）
· 列表项间用淡蓝虚线分割：border-bottom: 1px dashed #e2edf9

3.5 分割线

```css
hr {
  border: 0;
  height: 1px;
  background: linear-gradient(90deg, #d6e5f5, #ffffff);
}
```

四、交互与状态

状态 样式变化
卡片 Hover 边框变深（#b8d0e8），背景微调（#fbfdff）
按钮/链接 Hover 背景变为淡蓝（#dceaff），边框加深
焦点状态 使用 #9bbde0 边框或外发光

五、字体与排版

· 字体栈：system-ui, -apple-system, 'Segoe UI', Roboto, 'Helvetica Neue', sans-serif
· 行高：1.6
· 字重：标题 500~550，正文 350~400
· 禁止使用 emoji，统一使用文字或符号（如 ·、–）

六、移动端适配要点

1. 断点 680px：网格由两列变为单列
2. 断点 420px：进一步缩小内边距（padding: 1rem 0.8rem）
3. 所有卡片、按钮、输入框在移动端保持可点按区域 ≥ 44px
4. 横向布局（如 flex 行）在移动端改为纵向排列
5. 文字大小不低于 14px（移动端）

七、禁止使用的样式

· ❌ 渐变色背景（纯白或淡蓝纯色）
· ❌ emoji（😊、✨、🚀 等）
· ❌ AI 风格配色（紫、粉、霓虹、高饱和）
· ❌ 复杂动画（仅允许极简过渡，如 hover 边框变化）
· ❌ 深色模式（仅支持浅色）

八、示例 HTML 结构

```html
<div class="doc-container">
  <!-- 头部 -->
  <header class="doc-header">
    <h1>页面标题</h1>
    <span class="sub">副标题或描述</span>
  </header>

  <!-- 网格卡片区 -->
  <div class="section-grid">
    <div class="card">
      <h3>卡片标题</h3>
      <p>卡片内容描述……</p>
    </div>
    <div class="card">
      <h3>卡片标题</h3>
      <ul>
        <li>列表项一</li>
        <li>列表项二</li>
      </ul>
    </div>
  </div>

  <!-- 布局演示 -->
  <div class="layout-demo">
    <span class="item">元素一</span>
    <span class="item primary">主要元素</span>
  </div>
</div>
```

九、色值速查表

名称 HEX
纯白 #ffffff
淡蓝辅助背景 #f5faff / #f0f6fe / #f8fcff
卡片边框 #e6eef9
主边框 #d9e8f7 / #dce8f5
强调边框 #b8d0e8 / #9bbde0
淡蓝填充 #dceaff / #e5f0fc
标题深色 #1a2b3c / #1c3450
正文深色 #1f3347 / #1e2b3c
辅助文字 #3f5b77
阴影 rgba(180, 200, 220, 0.15)


## 十一、数据库：plugins 表索引调整（版本重复冲突修复）

> 背景：`idx_plugin_version` 原来是 `UNIQUE(plugin_id, version_code)`，会导致：
> - 同一个脚本 ID 不能上传两个相同版本号（即使是不同作者、完全不同的内容也不行）
> - 作者自己想重新打包上传修复版（不改版本号）直接抛 1062 Duplicate entry
>
> 修复思路：**`could_id` 自增主键是全局唯一标识**，`plugin_id + version_code` 只用作快速查询的普通索引，不再唯一。
> 代码层面（`online_plugin/index.php`）已经做了幂等处理：
> - 同一作者再次上传同 `plugin_id + version_code` → 覆盖旧记录（更新 zip/名称/介绍，重走审核，旧 zip 自动删除）
> - 不同作者上传同 `plugin_id + version_code` → 作为两条独立 could_id 记录存在（完全不冲突）
> - 不同版本号（同一 plugin_id） → 正常 INSERT，版本历史列表页按 `ORDER BY could_id DESC` 展示即可

执行以下 ALTER 语句（**需要先处理线上已有的重复数据，否则 DROP UNIQUE → ADD INDEX 后数据不会丢，只是不再强约束唯一**，推荐先备份）：

```sql
-- 1) 先确认当前索引名和状态（正常是 idx_plugin_version，UNIQUE）
SHOW INDEX FROM plugins WHERE Key_name = 'idx_plugin_version';

-- 2) 删除 UNIQUE 约束（如果报索引不存在可以跳过，证明已经改过）
ALTER TABLE `plugins` DROP INDEX `idx_plugin_version`;

-- 3) 重新加为普通 INDEX（加速按 plugin_id+version_code 查询，不唯一）
ALTER TABLE `plugins` ADD INDEX `idx_plugin_version` (`plugin_id`, `version_code`);
```

执行完成后上传 `today_wife_memory-1.0` 多次应该不会再出现 1062 错误。

## 十二、数据库：feedback 反馈表安装

用户反馈功能（用户提交 → 管理员处理回复 → 用户查看结果）需在数据库中新建 `feedbacks` 表，执行以下 SQL：

```sql
CREATE TABLE IF NOT EXISTS `feedbacks` (
  `could_id`      BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '自增主键（与 plugins.could_id 风格统一）',
  `qq`            VARCHAR(32)     NOT NULL COLLATE utf8mb4_unicode_ci COMMENT '反馈用户QQ号（关联 users.qq，显式 COLLATE 避免 1267 排序规则冲突）',
  `user_nickname` VARCHAR(64)     NULL     DEFAULT NULL   COMMENT '冗余用户昵称，方便列表展示',
  `title`         VARCHAR(200)    NOT NULL                COMMENT '反馈标题',
  `content`       TEXT            NOT NULL                COMMENT '反馈正文内容',
  `attachment`    VARCHAR(500)    NULL     DEFAULT NULL   COMMENT '附件地址（预留字段，暂未启用上传）',
  `status`        TINYINT UNSIGNED NOT NULL DEFAULT 0     COMMENT '状态：0 待处理 / 1 处理中 / 2 已解决 / 3 已关闭',
  `reply`         TEXT            NULL     DEFAULT NULL   COMMENT '管理员处理回复',
  `reply_qq`      VARCHAR(32)     NULL     DEFAULT NULL   COLLATE utf8mb4_unicode_ci COMMENT '处理管理员的QQ（关联 users.qq review_permission=1，显式 COLLATE）',
  `reply_nickname`VARCHAR(64)     NULL     DEFAULT NULL   COMMENT '冗余管理员昵称，展示用',
  `reply_time`    DATETIME        NULL     DEFAULT NULL   COMMENT '最后一次处理/回复时间',
  `create_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '反馈提交时间',
  `update_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录更新时间',
  PRIMARY KEY (`could_id`),
  KEY `idx_qq_status` (`qq`, `status`),
  KEY `idx_status_create` (`status`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户反馈工单表';
```

字段说明：
- `could_id` 自增主键：命名与 `plugins` 表保持一致，前后端 API 用这个字段索引单条反馈
- `status` 状态流转：用户提交默认 0(待处理) → 管理员接手改成 1(处理中) → 管理员回复并 2(已解决) / 3(已关闭)
- `reply` / `reply_qq` / `reply_time`：管理员回复内容、处理人QQ、回复时间，用户在"我的反馈"里可以查看
- 两张索引：
  - `idx_qq_status`：用户端按 QQ 查"我自己的所有反馈"，按状态筛选更快
  - `idx_status_create`：后台按状态筛选反馈列表，按时间倒序

> **关于 Illegal mix of collations（1267 错误）的说明**：
> 如果 `users.qq` 字段与 `feedbacks.qq` / `feedbacks.reply_qq` 的排序规则不一致（一个是 utf8mb4_general_ci，一个是 utf8mb4_unicode_ci），
> 则 JOIN 或 WHERE qq=? 时会报 `SQLSTATE[HY000]: General error: 1267 Illegal mix of collations`。
>
> 代码中已对所有 qq 比较加了显式 COLLATE 做短期止血；建议长期做法是执行下面 ALTER 统一字段排序规则：
> ```sql
> ALTER TABLE `feedbacks`
>   MODIFY COLUMN `qq`       VARCHAR(32) NOT NULL COLLATE utf8mb4_unicode_ci COMMENT '反馈用户QQ号（关联 users.qq）',
>   MODIFY COLUMN `reply_qq` VARCHAR(32) NULL DEFAULT NULL COLLATE utf8mb4_unicode_ci COMMENT '处理管理员的QQ（关联 users.qq review_permission=1）';
> ```
> 如果 `users.qq` 本身不是 utf8mb4_unicode_ci，也同步对 `users` 表执行 MODIFY COLUMN qq ... COLLATE utf8mb4_unicode_ci 统一。

