# 林大厨 · 开发规划

个人菜谱展示与制作流程记录工具（Android）。

## 1. 目标与三条动线

| 动线 | 场景 |
|---|---|
| 记 | 做菜时/做完后，快速记录食材与图文步骤 |
| 查 | 按分类浏览或搜索，边做边看 |
| 秀 | 只读展示模式，朋友挑选并点单 |

## 2. 功能清单

### MVP（M1–M5）

| 模块 | 内容 |
|---|---|
| 菜谱列表 | 网格卡片：封面、菜名、分类、耗时、难度、做过次数 |
| 分类索引 | 分类 + 标签双维度；自定义分类排序 |
| 搜索 | 菜名/食材/标签/步骤正文全字段；按时间与评分排序 |
| 菜谱详情 | 食材清单分组 + 步骤图文；步骤计时 |
| 编辑 | 新建/编辑/删除；步骤增删改与排序；相册选图与拍照；图片压缩 |
| 本地存储 | Room；图片存 App 私有目录 |
| 备份 | 导出/导入 zip（数据库 + 图片） |
| GitHub 同步 | 与私有数据仓库双向增量同步 |

### 二期

烹饪模式（常亮 + 分步全屏 + 音量键翻页）、长图分享、做菜记录与评分统计、桌面小组件、份量换算。

## 3. 技术架构

- **UI**：Jetpack Compose + Material3，单 Activity
- **数据**：Room（含 FTS5 虚表）、DataStore（偏好与同步元数据）
- **图片**：Coil 加载；系统相册/相机取图；统一压缩（最长边 ≤1600px，质量 80）
- **模式**：MVVM + 单向数据流（`UiState` / `Intent`）
- **导航**：先自研轻量返回栈，页面增多后引入 Navigation-Compose
- **同步**：OkHttp/Retrofit 调 GitHub Contents API（M5）

```
app/src/main/java/com/lyltc1/lindachu/
├── MainActivity.kt
├── data/
│   ├── local/       db / dao / entity
│   ├── repository/  DishRepository / SettingsRepository
│   ├── backup/      BackupManager（zip 导出导入）
│   └── sync/        GitHubSyncClient / SyncModels
├── domain/          model / usecase
├── ui/              home / detail / edit / search / order / components / theme
└── di/              依赖装配
```

### 已验证版本矩阵（本机构建通过）

| 项 | 版本 |
|---|---|
| Android Gradle Plugin | 9.4.0 |
| Kotlin | 2.2.10 |
| Gradle | 9.6.0 |
| compileSdk / targetSdk | 37 |
| minSdk | 26 |
| Compose BOM | 2026.02.01 |
| Room | 2.7.2 |
| Coil | 3.1.0 |
| DataStore | 1.1.4 |

## 4. 数据模型

### category
`id`(PK) · `name` · `sortOrder` · `colorHex`

### dish
`id`(PK, uuid) · `title` · `coverPath` · `categoryId` · `tags`(逗号分隔) · `difficulty`(1–5) · `durationMin` · `servings` · `notes` · `rating` · `cookCount` · `createdAt` · `updatedAt` · `deletedAt`(软删墓碑)

### ingredient
`id`(PK) · `dishId` · `name` · `amount` · `unit` · `groupName`(主料/辅料/调料) · `orderIndex`

### step
`id`(PK) · `dishId` · `orderIndex` · `text` · `imagePaths`(JSON 数组) · `timerSec`

### cook_log
`id`(PK) · `dishId` · `cookedAt` · `note` · `rating`

### dish_fts（FTS5 虚表）
`dishId`(UNINDEXED) · `title` · `content`（食材 + 标签 + 步骤文本聚合）

保存菜谱时同步维护 FTS 行，不使用数据库触发器，逻辑集中、易调试。

## 5. 搜索方案

- MVP：FTS5 `MATCH` 查询，结果按 `updatedAt` 降序
- 排序与筛选：时间、评分、耗时、做过次数；分类与标签组合过滤
- 输入去噪：忽略首尾空格；中文不做分词，依赖 FTS5 的 unicode61 分词器

## 6. GitHub 数据同步方案

### 仓库结构（`lyltc1/lin-da-chu-data`，private）

```
index.json              菜谱摘要索引（id / title / updatedAt / sha）
recipes/<dishId>.json   单个菜谱（食材 + 步骤 + 元信息）
images/<dishId>/        封面与步骤图
menus/                  对外发布的菜单快照
```

### 菜谱 JSON

```json
{
  "id": "d-9f2a1c",
  "title": "番茄炒蛋",
  "cover": "images/d-9f2a1c/cover.jpg",
  "category": "家常菜",
  "tags": ["快手", "素"],
  "difficulty": 1,
  "durationMin": 10,
  "servings": 2,
  "notes": "",
  "rating": 5,
  "cookCount": 3,
  "createdAt": 1710000000000,
  "updatedAt": 1710000000000,
  "deleted": false,
  "ingredients": [{ "name": "鸡蛋", "amount": "3", "unit": "个", "group": "主料" }],
  "steps": [{ "text": "蛋液加盐打散", "images": ["images/d-9f2a1c/s1.jpg"], "timerSec": null }]
}
```

### 同步流程

1. **拉取**：`GET index.json` → 与本地 `updatedAt` 比对 → 只下载变化的 `recipes/*.json` 与缺失图片 → 写回 Room
2. **推送**：本地 `updatedAt` 大于上次同步时间的条目 → `PUT` 菜谱 JSON（携带 `sha`）→ 上传新增图片 → 更新 `index.json`
3. **冲突**：`updatedAt` 较新者胜；删除走 `deleted: true` 墓碑并保留 90 天，避免误删扩散
4. **时机**：手动触发 + 进入/退出应用自动同步；图片上传仅在 Wi-Fi 下执行

### 认证与安全

- fine-grained Personal Access Token，仅授权 `lin-da-chu-data` 的 Contents 读写
- 令牌存 EncryptedSharedPreferences，不进 Git、不进日志
- 首次进入引导粘贴令牌，设置页可清除

## 7. 二维码点餐方案

### 发布流程

App 内"发布菜单" → 勾选要展示的菜 → 生成脱敏快照（菜名、封面缩略图、分类、标签、一句描述，**不含详细步骤**）→ 推送到公开仓库 `lyltc1/lin-da-chu-menu` → GitHub Pages 上线 → 生成二维码，内容为 `https://lyltc1.github.io/lin-da-chu-menu/?m=<menuId>`

### 静态菜单页

单页 HTML + JS，无框架：分类 tab、关键词搜索、卡片勾选、备注（忌口）、生成订单。

### 订单回传

| 路径 | 做法 | 代价 |
|---|---|---|
| A（MVP） | 生成订单文本，一键复制，朋友微信发回 | 零后端、零维护，需人工一步 |
| B（增强） | 轻量后端（腾讯云开发 CloudBase / Cloudflare Worker + D1），页面 POST 订单，App"订单"页拉取 | 需注册与部署，国内推荐 CloudBase |

先落 A 打通全链路，再按需上 B。

## 8. 里程碑与验收

| 阶段 | 内容 | 验收 |
|---|---|---|
| M0 | 环境、仓库、骨架 | ✅ `assembleDebug` 通过 |
| M1 | 数据层 + 种子数据 | 列表展示种子菜谱 |
| M2 | 列表 / 分类 / 详情 | 可浏览完整图文步骤 |
| M3 | 编辑端 | 新建菜谱并配图后详情可见 |
| M4 | 搜索与筛选 | 中文关键词命中食材与步骤 |
| M5 | GitHub 同步 + 备份 | 两机间数据一致 |
| M6 | 二维码点餐 | 扫码可选菜并回传订单 |
| M7 | 打磨与发版 | 签名 APK + Release |

## 9. 风险与对策

| 风险 | 对策 |
|---|---|
| 图片撑爆仓库与备份 | 统一压缩；删菜谱级联清理图片；缩略图单独尺寸 |
| 分区存储权限 | 一律用 SAF 选择器，不申请存储权限 |
| 误删数据 | 软删除 + 90 天墓碑 |
| 中文路径构建风险 | 已启用 `android.overridePathCheck`；若后续异常，迁至 ASCII 路径 |
| 同步冲突 | 按 `updatedAt` 覆盖，冲突项列出提示 |

## 10. 本地环境

| 项 | 值 |
|---|---|
| JDK | Temurin 21.0.12（`C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot`） |
| Android Studio | 2026.1.4（`D:\Android Studio`） |
| Android SDK | `D:\Android\Sdk`（platform android-37，build-tools 36.0.0） |
| 构建命令 | `.\gradlew.bat assembleDebug` |
