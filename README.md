# 林大厨 · LinDaChu

个人菜谱展示与制作流程记录工具（Android）。用文字或图文记录一道菜怎么做，按分类索引、按关键词搜索，方便回看自己怎么做的，也方便给朋友展示、让朋友点餐。

## 技术栈

- Kotlin + Jetpack Compose + Material3（单 Activity）
- Room（本地数据库，FTS5 全文搜索）
- Hilt 依赖注入、Navigation-Compose 路由
- Coil 图片加载、CameraX 拍照、DataStore 偏好设置
- 架构：MVVM + 单向数据流（UiState / Intent），分层 `ui / viewmodel / repository / data / di`

## 仓库划分

| 仓库 | 可见性 | 内容 |
|---|---|---|
| `lyltc1/lin-da-chu` | public | 应用源码（本仓库） |
| `lyltc1/lin-da-chu-data` | private | 菜谱数据 JSON 与图片，App 通过 GitHub API 同步 |
| `lyltc1/lin-da-chu-menu` | public | 菜单静态页（GitHub Pages），二维码扫码点单 |

菜谱图片属于用户数据，不进代码仓库。

## 本地构建

1. Android Studio（含 Android SDK）打开本目录
2. `local.properties` 中配置 `sdk.dir=<你的 Android SDK 路径>`（该文件已被 git 忽略）
3. `./gradlew assembleDebug` 或直接在 IDE 中运行 `app`

## 路线图

- [x] M0 环境与仓库初始化
- [ ] M1 数据层：Room 实体 / DAO / Repository + 同步数据格式
- [ ] M2 浏览端：菜谱列表、分类索引、菜谱详情
- [ ] M3 编辑端：菜谱增删改、步骤图文、图片压缩与存储
- [ ] M4 搜索：全字段搜索 + 筛选排序
- [ ] M5 GitHub 数据同步与备份导出导入
- [ ] M6 二维码扫码点单
- [ ] M7 打磨与发版

详见 [docs/plan.md](docs/plan.md)。
