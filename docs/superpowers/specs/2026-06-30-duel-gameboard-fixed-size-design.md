# 2026-06-30 对决模式 DuelGameBoard 固定大小设计方案

该设计文档旨在解决对决模式中，随着卡牌消除，棋盘包围盒收缩导致下方 UI 抖动/跳动的问题。我们通过固定 `DuelGameBoard` 的尺寸来保持 UI 的稳定性。

## 用户审查内容

- **目标尺寸**：固定宽度为 `340.dp`，固定高度为 `400.dp`，与普通模式 `GameBoard` 的尺寸规范完全保持一致。
- **排版对齐**：使用居中对齐（`Alignment.Center`），内部计算的卡片包围盒在 `Box` 内居中显示。
- **迷雾覆盖**：迷雾覆盖层（`FogEffectOverlay`）依然使用 `fillMaxSize()` 以填满这个固定的 `340.dp` x `400.dp` 容器。

## 涉及文件与修改点

### 1. [MODIFY] [DuelGameBoard.kt](file:///c:/Users/15613/Documents/file/sheeps/app/feature_game/src/main/java/com/example/sheeps/game/ui/components/DuelGameBoard.kt)

- 给 `DuelGameBoard` 新增参数 `modifier: Modifier = Modifier`。
- 将最外层容器 `BoxWithConstraints` 替换为 `Box`。
- 配置其默认 modifier：`.size(width = 340.dp, height = 400.dp)`。

## 验证计划

- 确认编译通过。
- 在对决模式中消除卡牌，观察下方消除槽及技能面板等组件位置，确保其不再随着卡牌消减而上下发生位置移动。
