# 2026-06-30 万能牌（太极牌）逻辑修复设计方案

该设计文档旨在解决万能牌没有效果的问题。我们重新设计了万能牌的激活条件与消除规则，以确保游戏的可消除性和正确的功能实现。

## 功能要求

- **使用条件**：卡槽中必须存在至少两张相同的卡牌。如果不存在，则提示无法使用。
- **幻化与消除**：万能牌幻化为卡槽中那两张相同卡牌的图案，并与其一起凑成 3 张进行消除。
- **可消除性保证**：从卡槽外部（优先置物架，其次棋盘）寻找一张相同图案的卡牌并一同标记为消除状态，从而保证游戏在万能牌使用后依然可以完全消除（剩余卡牌总数保持为 3 的倍数）。

## 涉及文件与修改点

### 1. [MODIFY] [GameToolDelegate.kt](file:///c:/Users/15613/Documents/file/sheeps/app/feature_game/src/main/java/com/example/sheeps/game/viewmodel/delegates/GameToolDelegate.kt)

- 导入 `kotlinx.coroutines.CoroutineScope` 和 `kotlinx.coroutines.launch`。
- 修改 `handleUseJoker` 方法签名，添加 `scope: CoroutineScope` 参数。
- 实现寻找卡槽内是否有相同两张牌的逻辑。如果没有，弹窗提示并返回。
- 如果有，选定该类型为目标类型，在外部（`movedOutTiles` / `boardTiles`）找到一张相同的牌并将其标记为已消除。
- 扣减万能牌数量，增加 100 分，执行 `processSlotMatch` 进行底层的胜利/失败与计分统计。

### 2. [MODIFY] [GameViewModel.kt](file:///c:/Users/15613/Documents/file/sheeps/app/feature_game/src/main/java/com/example/sheeps/game/viewmodel/GameViewModel.kt)

- 在调用 `handleUseJoker` 时传入 `viewModelScope` 作为首个参数。

## 验证计划

- 验证编译通过。
- 在游戏卡槽中有 2 张相同卡牌时，点击万能牌，验证其成功消除这 2 张牌，且分数增加 100。
- 在游戏卡槽中没有 2 张相同卡牌时，点击万能牌，验证弹出 Toast 提示。
