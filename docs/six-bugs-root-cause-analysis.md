# 秘境消消乐 - 6 问题根因分析报告

> 日期：2026-07-07 | 版本：v1.0

---

## 问题 1：显示积分和排行榜积分不一致

### 根因
两套完全不同的积分计算公式，各自独立运转：

| 位置 | 显示/用途 | 公式 |
|------|-----------|------|
| `GameResultOverlay.kt:162`（结算页） | 玩家看到的分数 | `消除组数 × 100`（双倍时×200） |
| `ScoreDelegate.kt:44-52`（提交排行榜） | 排行榜实际分数 | `max(100, (1000 - 秒数×2 - 道具数×50)) × 难度系数 × 双倍` |

### 涉及文件
- 前端：`GameResultOverlay.kt`, `ScoreDelegate.kt`
- 后端：无（后端直接信任客户端提交的分数）

---

## 问题 2：闯关模式过关送金币 + 明细无记录

### 根因

**2a - 过关送金币**：
- 前端 `ScoreDelegate.kt:57-59`：`val pointsReward = if (currentProgress == null) 50 else 0`
- 后端 `game.ts:172-175`：首次通关 `finalPoints += 50; UPDATE users SET points = ?`

**2b - 明细无记录**：
- 后端 `POST /api/score/submit` 中执行了 `UPDATE users SET points` 但**未插入 `point_record` 表**

### 涉及文件
- 前端：`ScoreDelegate.kt`, `SyncRepository.kt`
- 后端：`game.ts`

---

## 问题 3：闯关模式缺少计时器显示

### 根因
`levelStartTime` 存在于 `GameViewModel` 内部但不暴露给 UI：
- `GameViewState` 无 `elapsedTime` 字段
- `GameStatusBar` 无时间显示
- `GameResultOverlay` 通关弹窗不显示用时
- 无 tick 机制驱动计时器刷新

### 涉及文件
- 前端：`GameMviContract.kt`, `GameViewModel.kt`, `GameStatusBar.kt`, `GameResultOverlay.kt`
- 后端：无

---

## 问题 4：重新开始时卡牌形状不变

### 根因
两层固化：
- 后端 `level.ts`：`shapeIndex = levelId * 1000 % 18`（固定）
- 前端 `GameLevelGenerator.kt`：布局洗牌使用 `levelId * 1000L` 固定种子

每次重新开始传入新 seed，但 seed 未用于形状/布局选择。

### 涉及文件
- 前端：`GameLevelGenerator.kt`
- 后端：`level.ts`

---

## 问题 5：积分榜单缺少积分构成说明

### 根因
排行榜 `LeaderboardActivity.kt` 只展示数字分数，无规则说明。

### 涉及文件
- 前端：`LeaderboardActivity.kt`
- 后端：无

---

## 问题 6：快速点击导致消除不了的牌

### 根因
`GameLogicDelegate.handleClickTile` 无防抖/锁保护：
1. 第 1 次点击 → 放入槽位 → 启动 360ms 延迟协程
2. 第 2 次点击 → 在协程执行前又修改了槽位
3. 360ms 后协程执行时，`state.boardTiles` 已被后续点击修改，消除判断可能出错

### 涉及文件
- 前端：`GameLogicDelegate.kt`
- 后端：无

---

## 汇总

| # | 问题 | 前端文件 | 后端文件 | 类型 |
|---|------|----------|----------|------|
| 1 | 积分不一致 | GameResultOverlay.kt | - | Bug Fix |
| 2 | 过关送金币 | ScoreDelegate.kt, SyncRepository.kt | game.ts | Bug Fix |
| 3 | 计时器显示 | GameMviContract.kt, GameViewModel.kt, GameStatusBar.kt, GameResultOverlay.kt | - | 新功能 |
| 4 | 形状固定 | GameLevelGenerator.kt | level.ts | Bug Fix |
| 5 | 规则说明 | LeaderboardActivity.kt | - | 新功能 |
| 6 | 快速点击 Bug | GameLogicDelegate.kt | - | Bug Fix |
