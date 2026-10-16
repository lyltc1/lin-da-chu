# 同步数据格式规范

大厨手记把 GitHub 仓库当作数据后端：菜谱以 JSON 文本存储、图片以文件存储，
App 通过 GitHub Contents API 读写。本文是 App 与数据仓库 `lyltc1/lin-da-chu-data` 之间的契约。

代码位置：`data/sync/SyncModels.kt`（编解码）、`data/sync/SyncManager.kt`（同步流程）。

## 仓库结构

```
lin-da-chu-data/
├── index.json                  菜谱索引，同步的唯一入口
├── recipes/<dishId>.json       单个菜谱完整内容（含食材与步骤）
├── images/<dishId>/<uuid>.jpg  封面与步骤图
└── menus/                      对外发布的菜单快照（M6 二维码点餐）
```

`images/` 下的相对路径与 App 私有目录 `filesDir/` 下的路径**完全一致**，
因此同步时只需按路径搬运文件，无需任何映射表。

## index.json

同步时的比对依据。App 读它来判断"哪条菜谱远端更新了"，从而避免全量下载。

```json
{
  "version": 1,
  "updatedAt": 1750000000000,
  "dishes": [
    {
      "id": "d-tomato-egg",
      "title": "番茄炒蛋",
      "updatedAt": 1750000000000,
      "deleted": false,
      "sha": "3b18e512..."
    }
  ]
}
```

| 字段 | 类型 | 说明 |
|---|---|---|
| `version` | Int | 格式版本，当前 `1` |
| `updatedAt` | Long | 索引最后写入时间（epoch millis） |
| `dishes[].id` | String | 菜谱 ID，同时是 `recipes/<id>.json` 的文件名 |
| `dishes[].title` | String | 菜名，冗余存储便于在仓库里直接浏览 |
| `dishes[].updatedAt` | Long | 该菜谱最后修改时间，冲突判定依据 |
| `dishes[].deleted` | Boolean | 墓碑标记 |
| `dishes[].sha` | String | `recipes/<id>.json` 的 blob sha，更新文件时必须带上 |

## recipes/<dishId>.json

```json
{
  "id": "d-tomato-egg",
  "title": "番茄炒蛋",
  "coverPath": "images/d-tomato-egg/a1b2.jpg",
  "categoryId": "c-home",
  "tags": "快手,家常,下饭",
  "difficulty": 1,
  "durationMin": 10,
  "servings": 2,
  "notes": "番茄去皮口感更细腻。",
  "rating": 5,
  "cookCount": 12,
  "createdAt": 1750000000000,
  "updatedAt": 1750000000000,
  "deletedAt": null,
  "ingredients": [
    { "name": "番茄", "amount": "2", "unit": "个", "group": "主料" }
  ],
  "steps": [
    { "text": "番茄切块", "images": ["images/d-tomato-egg/a1b2.jpg"], "timerSec": 180 }
  ]
}
```

字段约定：

- `tags` 存逗号分隔字符串（与数据库一致，避免 JSON 数组带来的额外解析歧义）
- `coverPath` / `images[]` 均为**相对路径**，形如 `images/<dishId>/<uuid>.jpg`
- `deletedAt` 为 `null` 表示未删除；非空即墓碑，同步时会传播删除
- `ingredients` 与 `steps` 用数组顺序表达排序，落库时按索引回填 `orderIndex`

## 同步流程

1. **读索引**：拉取 `index.json`，建立 `id → updatedAt/sha` 映射；不存在则视为空仓库
2. **拉远端**：对每个远端条目，若 `remote.updatedAt > local.updatedAt`，下载 `recipes/<id>.json` 覆盖本地，并补齐缺失图片
3. **推本地**：对每条本地数据，若远端不存在或 `local.updatedAt > remote.updatedAt`，先上传缺失图片，再写入 `recipes/<id>.json`（已存在则携带原 sha）
4. **回写索引**：合并两端结果，按 `updatedAt` 倒序写回 `index.json`，并记录本次同步时间

## 冲突策略

以 `updatedAt` 较新者为准（last-write-wins）。

- 删除优先于修改的比较同样走时间戳：墓碑的 `updatedAt` 若比远端修改时间新，删除生效；
  反之说明删除后又有人改过，保留修改。
- 个人单人多设备场景下真冲突概率极低，暂不提供交互式合并；
  若日后出现"两边都改了同一道菜"的实际困扰，再引入保留双版本的机制。

## 凭据

- 需要具有 `repo` 权限的 GitHub Personal Access Token
- 仅保存在本机 DataStore，**不会**写入备份 zip，换机不跟随

## 后续（M6）

`menus/` 用于存放对外发布的菜单快照：App 生成静态 JSON 交给 GitHub Pages 渲染，
朋友扫码后浏览的正是这份数据，订单再回传到仓库。与本文格式共用 `SyncRecipe`。
