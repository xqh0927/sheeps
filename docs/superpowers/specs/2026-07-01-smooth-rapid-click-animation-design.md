# 2026-07-01 快速点击卡牌流畅度与状态同步优化设计方案

该设计文档旨在解决高频/快速点击卡牌时发生的卡顿、状态不一致、或飞行动画重叠/滞后问题：
1. **即时状态更新**：改变现有的“等飞行动画完全结束（350ms）再触发 `onTileClick` 更新 ViewModel”的逻辑，重构为**点击后瞬间触发 `onTileClick` 改变棋盘与卡槽的状态**。
   - **效果**：卡槽中的剩余位置计数瞬间更新，后续快速点击的卡牌能瞬间瞄准正确的物理卡槽位置（例如 position 0, 1, 2...），解决重叠飞向同一卡槽的问题；同时下方的阻塞状态瞬间被刷新解开，允许玩家快速连续点击刚露出来的卡牌。
2. **插槽卡牌暂存遮罩**：由于数据层状态瞬间变更为已进入卡槽，为了避免卡牌在飞完之前就在卡槽中“穿帮”双重显示，我们在消除卡槽中利用 `flyingTileIds` 判断，将正在飞行中的插槽卡牌设置 `alpha = 0f` 隐藏。待 350ms 飞行动画完全落槽并移出 `flyingTileIds` 时，插槽卡牌平滑恢复为可见（`alpha = 1f`）。
3. **列表性能组件 key 绑定**：在 `GameBoard` 与 `DuelGameBoard` 的卡牌循环中引入 `key(tile.id)` 封装，并在 `MovedOutTray` 循环中引入 `key(tile.id)`，在飞行动画层引入 `key(fly.tileId)`。
   - **效果**：让 Compose 在列表结构增删时复用对应的 Composable 节点，彻底消除棋盘重构时全量测量的额外 CPU 耗时。
4. **棋盘坐标静态化记忆**：用 `remember` 锁定关卡初始加载时的 `minX`/`maxX`/`minY`/`maxY` 极值，避免因消牌导致边界变量动态重算，从而彻底消除棋盘晃动/移位的问题。

## 涉及文件与修改点

### 1. [MODIFY] [GameBoard.kt](file:///c:/Users/15613/Documents/file/sheeps/app/feature_game/src/main/java/com/example/sheeps/game/ui/components/GameBoard.kt) 和 [DuelGameBoard.kt](file:///c:/Users/15613/Documents/file/sheeps/app/feature_game/src/main/java/com/example/sheeps/game/ui/components/DuelGameBoard.kt)
- 已经在上一步骤中为 `GameBoard` 和 `DuelGameBoard` 实现了 `remember` 锁定以及 `key(tile.id)` 复用逻辑。

### 2. [MODIFY] [GameDock.kt](file:///c:/Users/15613/Documents/file/sheeps/app/feature_game/src/main/java/com/example/sheeps/game/ui/components/GameDock.kt)
- `GameDock` 与 `MatchingSlot` 增加传入参数 `flyingTileIds: Set<String>`。
- 在匹配插槽 `TileView` 渲染时，增加 `.alpha(if (flyingTileIds.contains(tile.id)) 0f else 1f)`。

### 3. [MODIFY] [GameScreen.kt](file:///c:/Users/15613/Documents/file/sheeps/app/feature_game/src/main/java/com/example/sheeps/game/ui/screens/GameScreen.kt)
- 修改 `onTileClick` 调用时机：执行飞行时，立刻执行 `onTileClick(tile)`，协程仅负责在 350ms 动画结束后将卡牌移出 `flyingTiles` 与 `flyingTileIds`。
- 将 `flyingTileIds` 作为参数传入 `GameDock(...)`。

### 4. [MODIFY] [DuelScreen.kt](file:///c:/Users/15613/Documents/file/sheeps/app/feature_game/src/main/java/com/example/sheeps/game/ui/screens/DuelScreen.kt)
- 同样优化 `onTileClick` 触发时机，瞬间更新对决状态。
- 为 `DuelMatchingSlot` 传入 `flyingTileIds` 并在 TileView 处增加飞行隐藏。

## 验证计划
- 确认编译通过。
- 打开游戏快速连击 3 张卡牌，确认飞行动画分别独立、按序飞向插槽的不同格子中，无任何错位或卡顿，过程极其丝滑。
