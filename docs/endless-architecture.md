# 无尽生存模式（叠塔）· 架构设计与任务分解

> 文档状态：v1.0（架构阶段，基于 `gdd-endless-survival-mode.md` v0.2）
> 范围：完整 GDD 玩法（概念 A 无尽井 + 多层可见 + 暂存区 + Freeze + 每日种子 + 波次提速）+ 端到端（客户端 + server + admin-console）
> 约束：UI 全部用 `MaterialTheme.colorScheme.*` 令牌；卡牌皮肤通过 `currentSkin` 动态跟随

---

## 1. 架构总览

```
app/ (Android · Kotlin · Compose · MVI)
├─ feature_game/.../game/endless/        ← 新增无尽子包（全部新文件）
│   ├─ state/EndlessMviContract.kt        State / Intent / Effect
│   ├─ viewmodel/EndlessViewModel.kt      @HiltViewModel
│   ├─ viewmodel/engine/EndlessEngine.kt  纯逻辑核心（重力/三消/死亡/计分）
│   ├─ viewmodel/helpers/EndlessLevelGenerator.kt   LCG 生成器（可解保证）
│   ├─ viewmodel/delegates/EndlessScoreDelegate.kt   game_mode=1 提交
│   ├─ ui/screens/EndlessScreen.kt
│   ├─ ui/components/EndlessWellBoard.kt  6 列井 + 下落动画 + 多层可见
│   ├─ ui/components/EndlessDock.kt       7 格卡槽（复用 TileView）
│   ├─ ui/components/EndlessHud.kt        分数/连击/波次/Freeze/Hold 按钮
│   ├─ ui/components/EndlessHoldSlot.kt   暂存区
│   ├─ ui/components/EndlessResultDialog.kt  结算/失败弹窗
│   └─ EndlessActivity.kt                 @Route("/endless/play")
├─ core/.../data/model/GameModels.kt      ScoreRequest 加 game_mode 字段
├─ core/src/main/res/values/strings.xml   + values-en/strings.xml   无尽文案
└─ feature_menu/.../ui/components/EndlessModeComponents.kt   入口"开始"接 EndlessActivity

server/ (TypeScript · Cloudflare D1)
├─ schema.sql                leaderboard 加 game_mode 列
└─ src/handlers/game.ts      /api/score/submit 与 /api/leaderboard 加 game_mode 路由
└─ src/handlers/admin.ts     getStats 加无尽指标

admin-console/ (React + TS)
├─ src/pages/ShopItems.tsx   ITEM_TYPES 加 'FREEZE'
├─ src/pages/Users.tsx       PROPS_DEFINITIONS 加 FREEZE
├─ src/pages/Dashboard.tsx   STAT_CARDS 加无尽指标
└─ src/api/admin.ts          Stats 类型加无尽字段
```

概念 A 重申：6 列竖井，卡牌自顶下落、自底堆积；玩家点列顶牌入 7 格卡槽，凑 3 同花消除；顶到死亡线或卡槽溢出即 Game Over。

---

## 2. 核心数据结构

### 2.1 复用与新增约定
- **Tile 模型直接复用**（不改）：`Tile(id, type, x, y, z, state, isBlind, sealedCount)`。
  - 无尽井用：`x` = 列索引（0..5，浮点但取整数列），`y` = 该牌在列中的**高度（自底 0 起）**，`z` = 0，`state` 用 `NORMAL`（场上）/ `IN_SLOT`（卡槽）/ `MOVED_OUT`（暂存）。
  - 下落动画层：逻辑上牌已"落定"到某列高度，UI 用 `animateOffset` 从 spawn 高度滑到目标高度做视觉下落。
- **TileState / GameEngine**：`calculateBlockedStates` 是 3D 叠堆遮挡，**无尽井不用**（列重力天然只有列顶可点）。`EndlessEngine` 自行判断可点击牌。

### 2.2 EndlessViewState（Kotlin，放 `state/EndlessMviContract.kt`）
```kotlin
data class EndlessViewState(
    val isLoading: Boolean = false,
    val status: EndlessStatus = EndlessStatus.READY,          // READY/PLAYING/PAUSED/GAMEOVER
    val currentSkin: String = "classic",
    // 棋盘：每列自底向上的牌栈
    val columns: List<List<Tile>> = List(6) { emptyList() },  // 6 列
    val deathRow: Int = 12,                                    // 列高达到此值 = 触顶
    // 卡槽
    val slotTiles: List<Tile> = emptyList(),
    val maxSlot: Int = 7,
    // 暂存区
    val holdTile: Tile? = null,
    val holdCooldownMs: Long = 0,
    // 计分
    val score: Int = 0,
    val combo: Int = 0,
    val wave: Int = 1,
    val bestScore: Int = 0,
    // 节奏
    val dropIntervalMs: Long = 1500,                          // 当前下落间隔（随波次缩短）
    val isFrozen: Boolean = false,
    val freezeRemainingMs: Long = 0,
    val freezeCount: Int = 0,
    // 生成器
    val activeSuitCount: Int = 3,                             // 当前同屏花色种类（主难度杠杆）
    val isDaily: Boolean = false,
    val seed: Long = 0,
    // 视觉
    val visibleLayers: Int = 2,                               // 每列透出的下方层数
    val lastMatchedTileIds: Set<String> = emptySet(),         // 消除高亮
    val showResult: Boolean = false,
    val deathReason: String = ""                             // "death_line" / "slot_overflow"
)

enum class EndlessStatus { READY, PLAYING, PAUSED, GAMEOVER }
```

### 2.3 EndlessViewIntent（sealed interface）
```kotlin
sealed interface EndlessViewIntent {
    data class Init(val isDaily: Boolean, val seed: Long = 0) : EndlessViewIntent
    data class ClickColumn(val col: Int) : EndlessViewIntent          // 点列顶牌
    object UseFreeze : EndlessViewIntent
    object UseHold : EndlessViewIntent                                // 暂存列顶牌 / 取回
    data class SwapColumns(val a: Int, val b: Int) : EndlessViewIntent // 列顶交换（v1.5，预留）
    object Pause : EndlessViewIntent
    object Resume : EndlessViewIntent
    object Restart : EndlessViewIntent
    object Leave : EndlessViewIntent
    object DismissResult : EndlessViewIntent
}
```

### 2.4 EndlessViewEffect（sealed interface）
```kotlin
sealed interface EndlessViewEffect {
    data class ShowToast(val message: String) : EndlessViewEffect
    data class PlaySound(val soundType: SoundType) : EndlessViewEffect  // 复用现有 SoundType
    object Vibrate : EndlessViewEffect
    object ExitGame : EndlessViewEffect
}
```

---

## 3. EndlessEngine（纯逻辑核心，`viewmodel/engine/EndlessEngine.kt`）

> 设计为 `object EndlessEngine`（或带注入的 class），所有方法 `pure`（输入 state 片段 → 输出新片段），便于 ViewModel 调用与（未来）测试。

### 3.1 方法签名
```kotlin
object EndlessEngine {
    /** 生成一个 tick：把下一张牌落入最短列（公平），返回新 columns */
    fun spawnTick(columns: List<List<Tile>>, nextTile: Tile, deathRow: Int): Pair<List<List<Tile>>, Boolean>
    /** 点击列 col 的顶牌 → 入槽；返回 (新 columns, 新 slot, 是否触发消除, 消除的 id 集合) */
    fun clickColumn(columns: List<List<Tile>>, slot: List<Tile>, col: Int, maxSlot: Int): ClickResult
    /** 在 slot 中检测并消除 3 同花，返回 (新 slot, 消除 ids, baseGain) */
    fun resolveMatch(slot: List<Tile>): MatchResult
    /** 连击倍率：1 + ln(combo)，combo>=1 */
    fun comboMultiplier(combo: Int): Double = 1.0 + kotlin.math.ln((combo).coerceAtLeast(1).toDouble())
    /** 死亡判定 */
    fun isDead(columns: List<List<Tile>>, slot: List<Tile>, deathRow: Int, maxSlot: Int): String?
    /** 暂存取/存 */
    fun toggleHold(holdTile: Tile?, col: List<Tile>, slot: List<Tile>): HoldResult
}
```

### 3.2 算法要点
- **spawnTick**：`nextTile` 落入 `columns` 中高度最小的一列（并列随机）；若该列高度 +1 >= deathRow → 返回 `dead=true`。
- **clickColumn**：仅列顶（最后一员）可点；取出后加入 `slot`；调用 `resolveMatch`。
- **resolveMatch**：`slot.groupBy { it.type }`，任意 type 数量 >=3 → 移除前 3 张（按 id），`baseGain = 100`，返回消除 ids。重复直到无可消（防连锁：一次点击只消一组，余下随后续点击再消，简化手感）。
- **计分**：`score += (baseGain * comboMultiplier(combo)).toInt()`；match 成功 `combo += 1`，否则（点击未触发消除）`combo = 0`。
- **死亡**：`columns.any { it.size >= deathRow }` → `"death_line"`；`slot.size >= maxSlot` → `"slot_overflow"`。
- **多层可见**：UI 层用 `visibleLayers` 渲染列顶往下第 2~3 张（半透明、不可点），逻辑层不影响可点击判定。
- **暂存 Hold**：点 Hold 时，把当前某列顶牌移入 `holdTile`（列顶牌由玩家选择？简化：Hold 作用于"最近点击列"或 UI 传入的列）。取回时 holdTile 进 slot，带 `holdCooldownMs`（如 3000ms）防滥用成第 8 槽。CD 由 ViewModel 计时。

### 3.3 节奏与难度（ViewModel 驱动）
- `dropIntervalMs` 初始 1500ms；每完成一波（每消除 12 张 或 每存活 30s，取先到）→ `wave++`，`dropIntervalMs = (dropIntervalMs * 0.9).toLong()`，发 `ShowToast("Wave $wave · Speed Up!")`。
- `activeSuitCount` 随 wave/score 提升：wave1=3，每 2 波 +1，上限 12（GDD §5）。
- `isFrozen` 时 ViewModel 暂停下落计时器 `freezeRemainingMs`（3~5s，取 4000）。

---

## 4. EndlessLevelGenerator（`viewmodel/helpers/EndlessLevelGenerator.kt`）

```kotlin
class EndlessLevelGenerator @Inject constructor() {
    /** 基于 seed 的 LCG，保证同种子同序列（每日挑战） */
    fun nextTile(state: EndlessGenState): Pair<Tile, EndlessGenState>
}
```
- **可解保证（核心）**：以 `batchSize = activeSuitCount * 3` 为一批，每批**恰好包含每种当前激活花色各 3 张**（打乱顺序）后逐个吐出。这样整条序列每种花色总数均为 3 的倍数 → 理论上全可消除，死亡只可能是玩家操作失误。
- **每日种子**：`isDaily` 时 `seed = YYYYMMDD` 转 long；普通模式 `seed = System.currentTimeMillis()`。LCG：`state = (state * 1103515245 + 12345) and 0x7FFFFFFF`。
- 输出 `Tile(id="endless_${counter}", type, x=spawnCol, y=0, z=0)`；`spawnCol` 由 ViewModel 决定（最短列），生成器只决定 `type`。

---

## 5. EndlessScoreDelegate（`viewmodel/delegates/EndlessScoreDelegate.kt`）

复用 `ScoreDelegate` 的 sha256 签名与提交范式，但组装带 `game_mode=1` 的 `ScoreRequest`：
```kotlin
fun submitEndlessScore(
    scope, finalScore, elapsedMs, isDaily,
    setEffect: (EndlessViewEffect) -> Unit
) {
    val userId = prefs.getUserId()
    val token = prefs.getToken()
    val auth = token?.let { "Bearer $it" }
    val levelId = 0                      // 无尽模式用 0 占位
    val sign = sha256("${userId}_${levelId}_${elapsedMs}_folklore")
    apiService.submitScore(auth, ScoreRequest(
        user_id = userId, level_id = levelId, score = finalScore,
        clear_time_ms = elapsedMs, sign = sign, game_mode = 1
    ))
}
```
- 同时本地存 `bestScore`（SharedPreferences `endless_best`）。
- 防作弊：与服务端签名校验一致（见 §7）。

---

## 6. 状态机与调用流程

```
READY ──Init──▶ PLAYING ──Pause/Resume──▶ PAUSED
   ▲                │  ▲
   │            死亡 │  │ 下落 tick / 点击 / Freeze / Hold
   │                ▼  │
   │             GAMEOVER ──DismissResult──▶ (退出/重开)
   └────────── Restart ───────────────┘
```
调用流：用户操作 → `EndlessViewIntent` → `EndlessViewModel.handleIntent` → 调 `EndlessEngine` 纯函数得到新片段 → `updateState { copy(...) }` → Compose 重渲染。下落由 ViewModel 内 `LaunchedEffect` 计时器（按 `dropIntervalMs`，冻结时暂停）触发 `spawnTick`。

---

## 7. 后端契约（server + 端到端）

### 7.1 ScoreRequest 扩展（client `GameModels.kt`）
```kotlin
@Serializable
data class ScoreRequest(
    val user_id: String,
    val level_id: Int,
    val score: Int,
    val clear_time_ms: Long,
    val sign: String,
    val game_mode: Int = 0          // 0=闯关/PvP, 1=无尽生存
)
```

### 7.2 server/schema.sql（约 174 行 CREATE TABLE leaderboard）
```sql
CREATE TABLE leaderboard (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  user_id INTEGER NOT NULL,
  level_id INTEGER NOT NULL,
  score INTEGER NOT NULL,
  clear_time_ms INTEGER NOT NULL,
  game_mode INTEGER NOT NULL DEFAULT 0,
  achieved_at INTEGER NOT NULL
);
CREATE INDEX idx_leaderboard_mode ON leaderboard(game_mode, level_id, score DESC);
```
> 若库已存在，用 `ALTER TABLE leaderboard ADD COLUMN game_mode INTEGER NOT NULL DEFAULT 0;`（在 schema 初始化脚本末尾追加，或单独迁移）。

### 7.3 server/src/handlers/game.ts
- **/api/score/submit（约 141 行）**：
  - 解析 `body.game_mode`（默认 0）。
  - 去重检查（约 156 行）`WHERE user_id=? AND level_id=? AND game_mode=?`。
  - 插入（约 173 行）`INSERT INTO leaderboard (user_id, level_id, score, clear_time_ms, game_mode, achieved_at) VALUES (?,?,?,?,?,?)`。
  - 防作弊：沿用现有 sign 校验（`sha256(userId_levelId_clearTime_folklore)`），`level_id` 对无尽传 0 不影响公式。
- **/api/leaderboard（约 262 行）**：查询加 `AND l.game_mode = ?` 过滤；客户端传 `game_mode` 作为 query 参数（新增 `@Query game_mode`，默认 0）。无尽榜 `level_id` 忽略（或传 0）。

### 7.4 server/src/handlers/admin.ts — getStats（约 277 行）
新增两个查询并并入返回：
```ts
const endlessToday = await env.DB.prepare(
  `SELECT COUNT(*) c FROM leaderboard WHERE game_mode = 1 AND achieved_at >= ?`
).bind(todayStart).first();
const endlessBest = await env.DB.prepare(
  `SELECT MAX(score) s FROM leaderboard WHERE game_mode = 1`
).first();
// 并入 stats 返回： endless_play_count = endlessToday.c, endless_max_score = endlessBest.s
```

### 7.5 admin-console（GDD §8 已给精确 diff，逐字采用）
- `ShopItems.tsx` `ITEM_TYPES` 加 `'FREEZE'`。
- `Users.tsx` `PROPS_DEFINITIONS` 加 `{ type: 'FREEZE', label: '冻结符 (Freeze)' }`。
- `Dashboard.tsx` `STAT_CARDS` 加 `{ key:'endless_play_count', label:'今日无尽挑战次数' }` 与 `{ key:'endless_max_score', label:'无尽模式历史最高分' }`。
- `src/api/admin.ts` `Stats` 类型加 `endless_play_count?: number; endless_max_score?: number;`。

---

## 8. 有序任务列表（实现波次）

- **Wave 1（纯逻辑，无 UI）**
  1. `EndlessMviContract.kt` — State/Intent/Effect
  2. `EndlessEngine.kt` — 重力/点击/三消/死亡/连击倍率/暂存
  3. `EndlessLevelGenerator.kt` — LCG + 批次可解 + 每日种子
  4. `core/.../GameModels.kt` — ScoreRequest 加 game_mode
- **Wave 2（ViewModel + 委派）**
  5. `EndlessScoreDelegate.kt`
  6. `EndlessViewModel.kt` — 计时下落、波次、Freeze、Hold、死亡→提交
- **Wave 3（UI）**
  7. `EndlessWellBoard.kt` — 6 列 + 下落动画 + 多层可见（复用 TileView）
  8. `EndlessDock.kt` — 7 槽（复用 TileView，参照 GameDock.MatchingSlot）
  9. `EndlessHud.kt` — 分数/连击/波次/Freeze/Hold
  10. `EndlessHoldSlot.kt`
  11. `EndlessResultDialog.kt`
  12. `EndlessScreen.kt` — 组装
- **Wave 4（接入）**
  13. `EndlessActivity.kt` — `@Route("/endless/play")`，extends BaseActivity，hilt 注入 VM
  14. `core/values/strings.xml` + `values-en/strings.xml` — endless_* 文案
  15. `EndlessModeComponents.kt` — "开始游戏" 按钮 `TheRouter.build("/endless/play").navigation()`
- **Wave 5（后端）**
  16. `server/schema.sql` — game_mode 列
  17. `server/src/handlers/game.ts` — submit + leaderboard 加 game_mode
  18. `server/src/handlers/admin.ts` — getStats 无尽指标
- **Wave 6（后台）**
  19. `admin-console` 三页面 + Stats 类型（按 §8 diff）

---

## 9. 共享约定
- **主题**：所有颜色用 `MaterialTheme.colorScheme.*`（primary/secondary/surface/surfaceVariant/surfaceContainer/outline/onSurface 等），禁止写死。
- **皮肤**：`EndlessScreen` 接收 `currentSkin`（来自 MenuViewState 或 VM 初始化传入），所有 `TileView`/`TileCardBase` 用 `skin = currentSkin`；切皮肤时自动刷新。
- **strings 命名**：`endless_*` 前缀（如 `endless_score`、`endless_combo`、`endless_wave`、`endless_freeze`、`endless_hold`、`endless_game_over`、`endless_death_line`、`endless_slot_overflow`、`endless_best`、`endless_start`、`endless_pause`、`endless_resume`、`endless_restart`）。
- **路由**：`@Route(path = "/endless/play")`，无需改 Manifest。
- **音效/振动**：复用 `SoundType`（CLICK/MATCH/WIN/LOSE）与 `EndlessViewEffect.PlaySound/Vibrate`。

## 10. 关键风险与决策
- **无 Gradle 编译验证**：本沙箱无法构建 Android，工程师须严格对齐现有 import/签名/Compose 用法；QA 做静态核对。
- **DuelActionDelegate 不复用**：其强类型绑定 `DuelViewState`，无尽用独立 `EndlessEngine`（逻辑更贴合列重力模型），仅复用其"groupBy type 找 3 同花"思路。
- **GameDock 不复用**：强类型绑 `GameViewState`；新建 `EndlessDock` 复用 `TileView` 渲染 7 槽（复制 MatchingSlot 的布局与动画，参数化 slotTiles/currentSkin）。
- **列交换（SwapColumns）**：GDD 定为 v1.5，本版先留 Intent/接口，UI 与逻辑可后置，不阻塞首版。
