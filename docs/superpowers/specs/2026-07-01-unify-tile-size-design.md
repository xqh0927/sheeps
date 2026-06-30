# 2026-07-01 棋盘与卡槽卡牌尺寸统一（48dp）设计方案

该设计文档旨在解决：
1. 棋盘中与卡槽中的卡牌大小不一致的问题，将两处卡牌物理渲染尺寸统一为 **`48.dp`**，以保持完美的视觉一致性。
2. 对齐物理遮挡公式：在卡牌尺寸变更为 `48.dp` 之后，将客户端运行期判定、本地生成求解器、服务端生成求解器的“10% 重叠遮挡公式”中的卡牌面积和阈值同步更新。

## 物理参数变化

- **卡牌物理尺寸**：从原本的 `52.dp`（棋盘）和 `40.dp`（卡槽）统一调整为 **`48.dp`**。
- **单张卡牌面积**：$48.0 \times 48.0 = 2304.0\text{ dp}^2$。
- **10% 遮挡判定面积阈值**：从 `270.4` 调整为 **`230.4`**。
- **逻辑间距**：保持 `spacing = 46` 不变。

## 涉及文件与修改点

### 1. [MODIFY] [TileView.kt](file:///c:/Users/15613/Documents/file/sheeps/app/feature_game/src/main/java/com/example/sheeps/game/ui/components/TileView.kt)

- 将 `TileView` 签名中的默认 `tileSize` 参数从 `52.dp` 更改为 **`48.dp`**。

### 2. [MODIFY] [GameBoard.kt](file:///c:/Users/15613/Documents/file/sheeps/app/feature_game/src/main/java/com/example/sheeps/game/ui/components/GameBoard.kt) 与 [DuelGameBoard.kt](file:///c:/Users/15613/Documents/file/sheeps/app/feature_game/src/main/java/com/example/sheeps/game/ui/components/DuelGameBoard.kt)

- 将内部常量 `tileSize` 从 `52` 更改为 **`48`**。

### 3. [MODIFY] [GameDock.kt](file:///c:/Users/15613/Documents/file/sheeps/app/feature_game/src/main/java/com/example/sheeps/game/ui/components/GameDock.kt)

- 将 `MovedOutTray`（置物架）和 `MatchingSlot`（卡槽）中的 `TileView` 的 `tileSize` 统一设置为 **`48.dp`**。
- 移除 `MatchingSlot` 中单个插槽容器的 `.clip(RoundedCornerShape(8.dp))`，以允许 `48.dp` 的卡牌完美溢出展示，不被截断。

### 4. [MODIFY] [DuelScreen.kt](file:///c:/Users/15613/Documents/file/sheeps/app/feature_game/src/main/java/com/example/sheeps/game/ui/screens/DuelScreen.kt)

- 将卡槽中 `TileView` 的 `tileSize` 从 `40.dp` 更改为 **`48.dp`**。
- 将 `DuelFlyingTilesLayer` 中 Box 的默认高度尺寸从 `52.dp` 统一调整为 **`48.dp`**，并在 scale 缩放公式中对齐：`val scale = 1f - (2f / 48f) * progress`（使飞行到卡槽最终正好是 `48.dp` 的 1:1 比例，无缩放差）。
- 同样移除插槽容器的 `.clip(RoundedCornerShape(8.dp))`。

### 5. [MODIFY] [GameScreen.kt](file:///c:/Users/15613/Documents/file/sheeps/app/feature_game/src/main/java/com/example/sheeps/game/ui/screens/GameScreen.kt)

- 将 `FlyingTilesLayer` 中 Box 的大小设为 `48.dp`，并同步微调 `scale` 映射：`val scale = 1f - (2f / 48f) * progress`（即从 `48.dp` 飞入 `48.dp` 卡槽，不发生缩放）。

### 6. [MODIFY] [GameEngine.kt](file:///c:/Users/15613/Documents/file/sheeps/app/core/src/main/java/com/example/sheeps/core/game/GameEngine.kt), [GameLevelGenerator.kt](file:///c:/Users/15613/Documents/file/sheeps/app/feature_game/src/main/java/com/example/sheeps/game/viewmodel/helpers/GameLevelGenerator.kt), [level.ts](file:///c:/Users/15613/Documents/file/sheeps/server/src/level.ts)

- 将所有包含 `52.0`（卡牌宽）和 `270.4`（10% 面积阈值）的计算表达式分别替换为 **`48.0`** 和 **`230.4`**。

## 验证计划

- 确认编译通过。
- 进入第 3 关及其他关卡，点击卡牌进行消除。
- 确认飞行动画起点和终点大小完全一致（均为 `48.dp`），飞行过程顺畅自然。
- 确认卡槽中的卡牌和棋盘上的卡牌大小完全相等，没有突兀感。
