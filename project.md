# 🚀 AI 智能便利贴 Android 开发终极指南

## 0. 项目愿景
打造一个**本地优先 (Local-First)** 的待办管理工具，实现“手机 $\leftrightarrow$ 云端 $\leftrightarrow$ 墨水屏设备”的三端实时同步。
**核心体验：** 极速的本地响应 $\rightarrow$ 隐形的后台同步 $\rightarrow$ 极简的 AI 审美。

---

## 1. 核心技术栈 (2026 Tech Stack)
AI 必须严格遵守以下技术选型，禁止使用过时库：
- **语言**: Kotlin 2.0+ (Coroutines, Flow)
- **UI 框架**: Jetpack Compose + Material 3 (Expressive Design)
- **架构**: MVVM + Clean Architecture (逻辑分层，物理单模块)
- **依赖注入**: Hilt
- **本地存储**: 
    - **Room**: 业务数据 (Todo, Device, SyncQueue)
    - **DataStore**: 用户配置 (URL, API Key, SyncInterval)
- **网络层**: Retrofit + OkHttp + Kotlinx Serialization
- **后台任务**: WorkManager (用于周期性同步与离线补发)
- **小组件**: Jetpack Glance (响应式布局)
- **图片加载**: Coil

---

## 2. 视觉与交互规范 (Gemini/Grok Style)
**目标：** 摒弃传统 Android 界面，追求深色、发光、悬浮的现代感。

### 2.1 视觉 Token
- **底色**: `Deep Black` / `Dark Charcoal` (`#000000` 或 `#0B0B0B`)。
- **卡片**: `Floating Glass` (半透明深灰色，极细的亮色描边，大圆角 $24dp+$)。
- **点缀色 (Accent)**: 采用渐变色 (例如：`#4285F4` $\rightarrow$ `#9B51E0`)，用于关键按钮和激活状态。
- **发光效果 (Glow)**: 关键元素 (如待办完成勾选) 带有轻微的外部扩散阴影 (Bloom Effect)。
- **字体**: 标题使用高字重 (Semi-bold)，正文使用 `Noto Sans SC`，数字使用 `Inter` 或 `Sora`。

### 2.2 交互动效
- **响应**: 所有点击必须有 `Scale(0.98f)` 的缩放反馈。
- **转场**: 页面切换采用“淡入 + 纵向微移” (180ms)。
- **状态**: 待办完成时，文字划线伴随颜色由 `Accent` $\rightarrow$ `Gray` 的渐变。

---

## 3. 核心架构：本地优先同步 (Local-First Logic)
**必须实现方案 B：写本地 $\rightarrow$ 异步上行。**

### 3.1 数据状态流转
1. **写入**: `UI` $\rightarrow$ `ViewModel` $\rightarrow$ `Repository` $\rightarrow$ **`Room (状态: PENDING)`** $\rightarrow$ `UI 立即更新`。
2. **同步**: `SyncManager` 监测到 `PENDING` 数据 $\rightarrow$ 调用 `API` $\rightarrow$ 成功 $\rightarrow$ **`Room (状态: SYNCED)`**。
3. **失败**: API 报错 $\rightarrow$ **`Room (状态: FAILED)`** $\rightarrow$ 进入 `WorkManager` 指数退避重试队列。

### 3.2 自动同步触发器
- **实时触发**: 新增/编辑/删除/完成 待办时，立即触发一次同步。
- **定时触发**: 根据 `Settings.syncInterval` (1/10/30/60min) 定时拉取云端更新。
- **设备联动**: 每次待办列表更新成功后，**自动**调用一次 `/display/text` 或结构化接口，将最新待办推送到选中设备。

---

## 4. 功能模块分解

### 模块 A：设备与设置 (Infrastructure)
- **设置中心**: `platformUrl`, `apiKey`, `syncInterval` (DataStore 持久化)。
- **设备管理**: 拉取 `/open/v1/devices` $\rightarrow$ 选择设备 $\rightarrow$ 持久化 `deviceId`。
- **拦截器**: `OkHttp Interceptor` 动态注入 `X-API-Key` 和 `baseUrl`。

### 模块 B：待办管理 (Todo Core)
- **功能**: 完整的 CRUD + 状态筛选 (全部/未完成/已完成)。
- **字段**: `title`, `description`, `dueDate`, `dueTime`, `priority` (0-2), `status`。
- **体验**: 支持乐观更新，弱网下不显示加载圈，仅在顶部显示微小的“同步中”状态条。

### 模块 C：内容推送中心 (Hardware Push)
- **推送类型**: 文本、结构化文本、图片 (Multipart)。
- **页面逻辑**: `pageId` 强制限制 $1 \sim 5$。
- **清屏**: 单页删除或循环调用 $1 \sim 5$ 页执行全清。

### 模块 D：桌面小组件 (Glance Widget)
- **布局**: 响应式 (Responsive)，适配 $2 \times 2$ 到 $4 \times 4$。
- **快捷路径**: 
    - 点击 `+` $\rightarrow$ 直接跳转 `QuickCreateActivity`。
    - 点击条目 $\rightarrow$ 跳转至 App 内编辑页。
- **刷新**: 随本地 Room 数据变化实时触发 `Widget.update`。

---

## 5. API 接口契约 (必须严格对齐)

| 模块     | 方法     | 路径                                           | 关键参数                           |
| :------- | :------- | :--------------------------------------------- | :--------------------------------- |
| **设备** | `GET`    | `/open/v1/devices`                             | -                                  |
| **待办** | `GET`    | `/open/v1/todos`                               | `deviceId`, `status`               |
| **待办** | `POST`   | `/open/v1/todos`                               | `title`, `priority`, `deviceId`... |
| **待办** | `PUT`    | `/open/v1/todos/{id}`                          | 更新字段                           |
| **待办** | `PUT`    | `/open/v1/todos/{id}/complete`                 | 切换完成状态                       |
| **待办** | `DELETE` | `/open/v1/todos/{id}`                          | -                                  |
| **显示** | `POST`   | `/open/v1/devices/{id}/display/text`           | `text`, `pageId`                   |
| **显示** | `POST`   | `/open/v1/devices/{id}/display/image`          | `images[]` (Multipart), `dither`   |
| **显示** | `DELETE` | `/open/v1/devices/{id}/display/pages/{pageId}` | -                                  |

**全局约定**: 
- Header: `X-API-Key: <apiKey>`
- 成功判定: `code == 0`

---

## 6. 开发步骤 (Roadmap)

**AI 请按以下顺序分步执行，每步完成后请让我验收：**

1. **Phase 1: 基建** $\rightarrow$ 项目初始化 $\rightarrow$ Hilt 配置 $\rightarrow$ DataStore 设置中心 $\rightarrow$ 动态 OkHttp 拦截器。
2. **Phase 2: 本地存储** $\rightarrow$ Room Entity (Todo, Device, SyncQueue) $\rightarrow$ DAO 实现 $\rightarrow$ Repository 基础结构。
3. **Phase 3: 同步引擎** $\rightarrow$ 实现 `SyncManager` $\rightarrow$ 落地“方案 B” (PENDING $\rightarrow$ SYNCED) $\rightarrow$ WorkManager 周期任务。
4. **Phase 4: 待办 UI** $\rightarrow$ 落地 Gemini/Grok 风格 $\rightarrow$ 列表页 $\rightarrow$ 新建/编辑页 $\rightarrow$ 状态切换动效。
5. **Phase 5: 推送中心** $\rightarrow$ 实现三种推送接口 $\rightarrow$ 联动待办更新自动推送。
6. **Phase 6: 小组件** $\rightarrow$ Glance 响应式布局 $\rightarrow$ 快捷新建/编辑入口 $\rightarrow$ 刷新机制。
7. **Phase 7: 打磨** $\rightarrow$ 全局动效优化 $\rightarrow$ 弱网异常处理 $\rightarrow$ 最终验收。

---

## 7. 注意事项 (Critical Warnings)
- **禁止** 在 UI 线程进行任何数据库或网络操作。
- **禁止** 直接在 ViewModel 中调用 API，必须经过 `Repository` $\rightarrow$ `Room` $\rightarrow$ `SyncManager`。
- **图片处理**: 上传前必须检查 $\le 2\text{MB}$ 且 $\le 5$ 张。
- **容错**: 所有 API 调用必须包裹在 `try-catch` 中，并将错误状态写回 Room 的 `SyncQueue` 以便重试。