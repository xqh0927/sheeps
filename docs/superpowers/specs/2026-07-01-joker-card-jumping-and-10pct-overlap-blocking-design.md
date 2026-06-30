# 2026-07-01 棋盘卡牌抖动修复与 10% 面积遮挡判断方案

该设计文档旨在解决：
1. 对决模式下，消除卡牌后其他卡牌依然存在轻微抖动/移位的问题。
2. 细化重叠遮挡规则：卡牌之间的重叠面积必须大于单张卡牌面积的 **10%**（即重叠面积 $> 270.4\text{ dp}^2$）时，才正式判定为“遮挡/锁定关系”。

## 涉及文件与修改点

### 1. [MODIFY] [DuelGameBoard.kt](file:///c:/Users/15613/Documents/file/sheeps/app/feature_game/src/main/java/com/example/sheeps/game/ui/components/DuelGameBoard.kt)

- **修复卡牌跳动**：将第 73 行的内层 `Box` 的尺寸从原先错误的 `boardWidth.dp` 与 `boardHeight.dp`（这会导致定位参照系偏离和单位重叠）更改为正确的物理卡牌内容大小 **`contentWidth.dp`** 与 **`contentHeight.dp`**（与 `GameBoard.kt` 的逻辑对齐）。
  - **效果**：使内层包裹卡牌的容器完全贴合卡牌，并由外层 `Box(Alignment.Center)` 居中。这能保证卡牌在整个游戏过程中处于绝对静止状态，不会因点击消牌产生任何跳动。

### 2. [MODIFY] [GameEngine.kt](file:///c:/Users/15613/Documents/file/sheeps/app/core/src/main/java/com/example/sheeps/core/game/GameEngine.kt)

- **实现 10% 面积重叠遮挡判定**：
  - 单张卡牌的尺寸为 $52.0 \times 52.0 = 2704.0\text{ dp}^2$，其 10% 面积为 `270.4f`。
  - 对于两张卡牌，计算它们在物理坐标下的重叠宽度 $ox = \max(0, 52.0 - dx \times 46.0)$ 和高度 $oy = \max(0, 52.0 - dy \times 46.0)$。
  - 重叠面积 $area = ox \times oy$。只有当 $area > 270.4\text{ f}$ 且上层卡牌高度更高时，才判定为遮挡。

### 3. [MODIFY] [GameLevelGenerator.kt](file:///c:/Users/15613/Documents/file/sheeps/app/feature_game/src/main/java/com/example/sheeps/game/viewmodel/helpers/GameLevelGenerator.kt)

- 本地客户端生成器中的 `blocks` 判定同步修改为上述“重叠面积 > 10%”的公式，确保生成的可解性路径符合同样的物理判定条件。

### 4. [MODIFY] [level.ts](file:///c:/Users/15613/Documents/file/sheeps/server/src/level.ts)

- 服务端生成算法中的 `blocks` 重叠规则同步修改为“重叠面积 > 270.4”公式，使三方逻辑绝对同步。

## 验证计划

- 确认编译通过。
- 点击消除卡牌，确认无论是闯关模式还是对战模式，其余卡牌均保持纹丝不动。
- 测试卡牌微小边缘重叠（重合面积低于 10%）的情况，下层卡牌应该高亮且可正常点击。
