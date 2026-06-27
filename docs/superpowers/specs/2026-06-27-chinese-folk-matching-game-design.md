# 国风民俗消除小游戏（羊了个羊类似玩法）设计规格说明书

本文档详细记录了该游戏的系统架构、交互界面、合规设计、核心层叠算法及后端 Cloudflare Workers + D1 数据库设计。

---

## 1. 业务目标与合规要求

### 1.1 项目目标
开发一款国风民俗主题的层叠消除小游戏（类似“羊了个羊”），使用最新的 **Android (Kotlin + Jetpack Compose)** 原生技术开发，后端托管于 **Cloudflare Workers & D1 SQL 数据库**（免费版），具备高性能、平滑动画和云端排行榜功能。

### 1.2 中国区上架合规标准 (Privacy & Game Compliance)
1. **零敏感权限声明**：在 `AndroidManifest.xml` 中仅声明 `android.permission.INTERNET`（网络权限），不声明任何定位、设备标识（IMEI/IMSI）、存储读写等敏感权限，从源头上规避隐私合规风险。
2. **隐私协议合规弹窗**：应用首次启动时，必须展示合规隐私政策同意弹窗。在用户点击“同意”前，绝不初始化 Retrofit/网络模块，更不能发起任何 API 请求。若用户选择“拒绝”，则给出友好提示并安全退出应用。
3. **健康游戏忠告与 CADPA 适龄提示**：
   - 启动页 (Splash Screen) 展示中国健康游戏忠告：“抵制不良游戏，拒绝盗版游戏。注意自我保护，谨防受骗上当。适度游戏益脑，沉迷游戏伤身。合理安排时间，享受健康生活。”
   - 启动页及主界面显著位置挂载 **CADPA 适龄提示绿标（8+）**。

---

## 2. 客户端系统设计 (Android - Kotlin & Compose)

### 2.1 技术栈与架构 (MVI 模式)
应用采用严格的 **MVI (Model-View-Intent)** 架构：
*   **ViewState** (UI 状态)：表示页面的唯一可信状态源。是只读且不可变的（如当前棋盘卡片列表、消除槽卡片、各道具剩余数量、当前得分、游戏状态等）。
*   **ViewIntent** (用户意图)：所有用户操作和外部触发事件转化为 Intent 发送给 ViewModel（如 `LoadLevel`、`ClickTile`、`UseUndo`、`UseMoveOut`、`UseShuffle`、`AgreePrivacy` 等）。
*   **ViewEffect/SideEffect** (副作用)：处理一次性事件（如弹窗提示、震动反馈、播放消除音效、页面跳转等）。
*   **ViewModel**：接收 Intent，处理业务逻辑并产生新的 ViewState，通过 StateFlow 抛给 UI 层；对于副作用则通过 Channel/SharedFlow 发送。
*   **网络请求**：Retrofit 2 + OkHttp 3 + Kotlin Serialization。

### 2.2 视觉设计与国风配色
游戏采用“新国潮”民俗卡通风格：
*   **主色调 (Primary)**：宫墙红 (`#C82423`)
*   **辅助色 (Secondary)**：帝王金/黄 (`#E6A23C`)
*   **背景色 (Background)**：水墨灰与青瓦白 (`#F5F5F7` 至 `#E0E0E0` 的优雅微渐变)
*   **字体**：适配 Compose 默认的 Sans-serif 字体，配置合适的字重与间距，展现古风雅致。

### 2.3 资源管理与卡牌元素 (Folk Art Motifs)
*   **图片资源**：所有卡片图案和背景均从公开的免费高清晰度矢量/位图资源站获取，优先使用清晰的 SVG 或高分辨率透明 PNG 贴图（使用 `drawable-xxhdpi` 规格），保证在各种分辨率屏幕上都清晰、不模糊。
*   **卡牌图案**：设计以下 12 种极具传统文化特色的民俗图案：
    1. 🏮 **红灯笼** (Lantern)
    2. 🧧 **红包袋** (Red Envelope)
    3. 🏺 **青花瓷** (Porcelain)
    4. 🪭 **折扇** (Folding Fan)
    5. 🧶 **中国结** (Chinese Knot)
    6. 🪙 **铜钱** (Copper Coin)
    7. 🦁 **醒狮头** (Lion Dance)
    8. 🥮 **月饼** (Mooncake)
    9. 🍵 **紫砂壶** (Teapot)
    10. 🧨 **鞭炮** (Firecracker)
    11. 🐟 **锦鲤** (Koi Fish)
    12. 🍑 **仙桃** (Peach)

---

## 3. 核心叠层逻辑与消除算法

### 3.1 卡片层叠模型
每张卡片 $i$ 包含数据结构：
```kotlin
data class Tile(
    val id: String,          // 唯一ID
    val type: Int,           // 图案类型 (1..12)
    val x: Float,            // 平面中心X轴（支持半格重叠）
    val y: Float,            // 平面中心Y轴（支持半格重叠）
    val z: Int,              // 所在图层（Z-index，数值越大越在上层）
    var state: TileState     // 状态: NORMAL（普通）, BLOCKED（被遮挡）, IN_SLOT（已入槽）, MOVED_OUT（被道具移出）
)
```
* **被遮挡判定**：假设卡片宽度为 $W$，高度为 $H$。当且仅当存在另一张状态为 `NORMAL` 的卡片 $B$，满足 $|A.x - B.x| < W$ 且 $|A.y - B.y| < H$，且 $B.z > A.z$ 时，卡片 $A$ 被判定为 **BLOCKED**（置灰，不可点击）。
* **消除槽 (Slot)**：最大容量为 7 格。每当玩家点击未遮挡的卡片，卡片飞入槽中并重新计算桌面遮挡状态。
  - 排序逻辑：卡槽内卡片会自动按 `type` 归类合并。
  - 消除逻辑：当卡槽内出现 3 张相同 `type` 的卡片时，触发“消消乐”动画，将这 3 张牌移除。
  - 失败判定：若卡槽积满 7 张牌且无法消除，则触发游戏结束。

### 3.2 特殊卡牌机制
为了提高玩法丰富度，设计两种特殊卡牌：
1. **“盲盒/灯谜卡” (Blind Box Card)**：当其处于 `BLOCKED` 状态时，卡面呈现“金色问号 `?`”；一旦其上方卡片被移开、状态变为 `NORMAL` 时，卡面自动翻开并播放国风云雾翻转效果，显露真实的图案类型。
2. **“封印卡” (Sealed Card)**：卡面上贴着“封印符咒”。玩家第 1 次点击时，它不进入卡槽，而是播放符咒破碎动画并解除封印（状态变为普通卡片）；第 2 次点击时才进入卡槽。

### 3.3 “保证可解”逆向关卡生成算法
为避免纯随机导致死局，关卡生成采用**逆向解题（Backwards Play）模拟**：
1. 根据关卡模板，在三维空间中放置若干卡片空位坐标集合（总数必须是 3 的倍数）。
2. 从顶层开始，找出当前在投影平面上完全不受上方空位遮挡的“暴露空位”。
3. 随机选择 3 个“暴露空位”，分配给它们相同的 `type`（如 3 个折扇）。
4. 将这 3 个空位从待分配列表中移除（这会导致原本被它们遮挡的下层空位暴露出来）。
5. 重复步骤 2..4，直到所有空间位置都被分配了图案类型。
6. 最终将生成的坐标与图案列表上传到云端或作为本地初始化文件。此关卡在玩家消除时**必定 100% 有解**。

### 3.4 道具逻辑
* **移出 (Move Out)**：取出卡槽中最左侧（或前3个）卡片移出到桌面的辅助置物架上，待后续消除。
* **撤销 (Undo)**：用操作栈回退上一次点击，将卡片从卡槽拉回原位置，并恢复其封印或盲盒状态。
* **洗牌 (Shuffle)**：仅打乱棋盘上所有剩余卡片的图案类型 (`type`)，保留原位置和遮挡层级。

---

## 4. 后端系统设计 (Cloudflare Workers & D1 SQL Database)

### 4.1 D1 数据库结构

```sql
-- 用户信息表
CREATE TABLE users (
    id TEXT PRIMARY KEY,          -- 设备唯一UUID
    username TEXT NOT NULL,       -- 玩家昵称
    created_at INTEGER NOT NULL   -- 注册时间戳
);

-- 云端关卡数据表
CREATE TABLE levels (
    level_id INTEGER PRIMARY KEY, -- 关卡ID (1, 2, 3...)
    difficulty INTEGER NOT NULL,  -- 难度级别
    layout_data TEXT NOT NULL,    -- 关卡空位坐标与图案分布 JSON
    created_at INTEGER NOT NULL
);

-- 排行榜表
CREATE TABLE leaderboard (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id TEXT NOT NULL,
    level_id INTEGER NOT NULL,
    score INTEGER NOT NULL,
    clear_time_ms INTEGER NOT NULL, -- 通关所用耗时
    achieved_at INTEGER NOT NULL,
    FOREIGN KEY(user_id) REFERENCES users(id)
);
```

### 4.2 API 路由接口
1. `POST /api/register`
   - 请求：`{ "id": "uuid-xxx", "username": "大明玩家_888" }`
   - 返回：用户信息
2. `POST /api/user/rename`
   - 请求：`{ "id": "uuid-xxx", "new_username": "新昵称" }`
   - 返回：状态
3. `GET /api/level?id={level_id}`
   - 返回：包含卡片坐标和类型的关卡详情 JSON
4. `POST /api/score/submit`
   - 请求：`{ "user_id": "uuid-xxx", "level_id": 1, "score": 1000, "clear_time_ms": 45000, "sign": "hmac_signature" }`
   - 校验：后端通过设备ID、用时和分数做 Hash 校验，通过后写入排行榜。
5. `GET /api/leaderboard?level_id={level_id}&limit=50`
   - 返回：按照通关用时 `clear_time_ms` 升序排列的前 50 名玩家列表。

---

## 5. 验证与测试计划

### 5.1 自动化测试
* 核心层叠与遮挡计算单元测试。
* 逆向关卡生成可解性验证测试（模拟 AI 玩关卡，确保 100% 能够清空棋盘）。
* 后端 Worker 接口的路由与 D1 数据库查询单元测试。

### 5.2 兼容性与真机验证
* 启动隐私协议同意/拒绝分支验证。
* UI 适配各种屏幕比例的手机，包括刘海屏、全面屏（适配 Edge-to-Edge）。
* 卡槽动画在不同性能等级的模拟器/真机上的流畅度体验（保持 60 FPS）。
