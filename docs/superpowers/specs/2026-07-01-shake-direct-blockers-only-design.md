# 2026-07-01 遮挡抖动特效只抖动上一层设计方案

该设计文档旨在优化在点击锁定卡牌时，其上方遮挡卡牌的抖动特效判定，使其仅对直接压在最上方的第一层卡牌触发抖动，而不是将所有垂直投影方向上的卡牌全部进行抖动。

## 物理定义

- **多层遮挡**：当卡牌 A 在 $z=0$ 层，其上方被 B ($z=1$) 遮挡，B 又被 C ($z=2$) 遮挡。
- **只抖动上一层**：点击卡牌 A 时，只有直接接触并压在 A 上的最底层遮挡物（即 $z$ 轴最小的遮挡卡牌 B）应该发生抖动。C 作为间接遮挡卡牌，不发生抖动。

## 涉及文件与修改点

### 1. [MODIFY] [GameLogicDelegate.kt](file:///c:/Users/15613/Documents/file/sheeps/app/feature_game/src/main/java/com/example/sheeps/game/viewmodel/delegates/GameLogicDelegate.kt)

- 修改 `handleClickTile` 中当 `isBlocked` 为 `true` 时的逻辑。
- 过滤 `blockers` 列表，找出具有最小 `z` 轴坐标的卡牌集合（即直接覆盖卡牌的第一层），仅将这些卡牌的 ID 加入 `shakingTileIds` 状态。
  ```kotlin
  val blockers = getBlockingTiles(tile, state.boardTiles)
  val minZ = blockers.minOfOrNull { it.z }
  val directBlockers = blockers.filter { it.z == minZ }
  val blockerIds = directBlockers.map { it.id }.toSet()
  ```

### 2. [MODIFY] [DuelActionDelegate.kt](file:///c:/Users/15613/Documents/file/sheeps/app/feature_game/src/main/java/com/example/sheeps/game/viewmodel/delegates/DuelActionDelegate.kt)

- 对决模式中的 `handleClickTile` 遮挡处理做完全相同的修改，使闯关模式与对决模式表现一致。

## 验证计划

- 确认编译通过。
- 点击一个被多层卡牌层层压住的卡牌，确认发生抖动和发红效果的卡牌仅为直接覆盖在其上方的第一层卡牌，更上层的卡牌保持静止。
