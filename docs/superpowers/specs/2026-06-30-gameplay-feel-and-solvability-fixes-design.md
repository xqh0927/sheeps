# 2026-06-30 棋盘卡死、点击跳动及卡顿性能修复方案

该设计文档旨在解决：
1. 棋盘界面在点击卡牌时发生卡顿的问题。
2. 点击卡牌后，棋盘内其他卡牌会发生跳动/移位的问题。
3. 判定遮挡的逻辑范围过大，导致卡牌在逻辑上判定被锁住而使关卡无法顺利通关（无解）的问题。

## 涉及文件与修改点

### 1. [MODIFY] [GameEngine.kt](file:///c:/Users/15613/Documents/file/sheeps/app/core/src/main/java/com/example/sheeps/core/game/GameEngine.kt)

- **对齐遮挡逻辑范围**：将 `GameEngine` 中的判定宽度 `W` 与 `H` 从 `52.0f / 46.0f`（约 `1.13`）调整为 `1.0f`。
  - **背景**：关卡生成器（`GameLevelGenerator.kt`）的求解和反向验证算法中判定卡牌遮挡的判定范围是 `1.0f`，而运行时 `GameEngine` 采用了 `1.13f`。这就导致许多本被判定为可解的关卡，在实际游戏中因 `1.0f <= 距离 < 1.13f` 被锁定而变得无解。
  - **修复**：将重叠判定上限设为 `< 1.0f`，与关卡生成器严格对齐，保证生成的关卡 100% 具备可解性。同时减少无意义的微小面积压迫判定，提升玩家点击可玩性。

### 2. [MODIFY] [GameBoard.kt](file:///c:/Users/15613/Documents/file/sheeps/app/feature_game/src/main/java/com/example/sheeps/game/ui/components/GameBoard.kt)

- **消除绝对坐标跳动**：将卡牌包围盒 `minX`, `maxX`, `minY`, `maxY` 的计算对象由 `visibleTiles`（仅可见卡牌，会随消除而不断变动）更改为 `state.boardTiles`（全局固定所有卡牌）。
  - **效果**：卡牌的偏移量 `x = (tile.x - minX) * spacing` 的参照物 `minX`/`minY` 变得恒定不变。消除任意卡牌后，其余卡牌将保持原位不动。
  - **性能提升**：避免了每次消除引起全量卡牌布局大小变更及重度 Recomposition，彻底消除界面卡顿。

### 3. [MODIFY] [DuelGameBoard.kt](file:///c:/Users/15613/Documents/file/sheeps/app/feature_game/src/main/java/com/example/sheeps/game/ui/components/DuelGameBoard.kt)

- 同样将 `minX`, `maxX`, `minY`, `maxY` 的计算对象由 `visibleTiles` 更改为 `state.boardTiles`，并以此计算 `boardWidth` 和 `boardHeight`，消除对决模式下的抖动与卡顿。

## 验证计划

- 确认编译通过。
- 打开闯关和对决模式，消除卡牌，验证其余卡牌位置完全静止不动，且消除瞬间无任何掉帧卡顿。
- 验证卡牌边缘轻微重合时（如间隔为 1 个完整卡牌逻辑身位），下层卡牌可正常点击，消除了死局现象。
