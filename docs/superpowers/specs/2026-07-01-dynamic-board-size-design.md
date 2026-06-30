# 2026-07-01 棋盘动态自适应固定尺寸设计方案

该设计文档旨在优化闯关和对战模式下的棋盘大小和排列，使其满足以下间距与对齐要求：
1. **左右间距**：棋盘的宽度保证左右各比屏幕边缘少 `16.dp`（即宽度为 `屏幕宽度 - 32.dp`），确保视觉排版统一且具有足够的左右留白。
2. **上下间距**：棋盘的高度自适应等于棋盘内部卡片区域的最高高度加 `24.dp`（即上下各留白 `12.dp`），并且在整个关卡游玩期间高度恒定不变。
3. **内容居中**：卡牌网格在棋盘容器内保持完美居中。

## 涉及文件与修改点

### 1. [MODIFY] [GameBoard.kt](file:///c:/Users/15613/Documents/file/sheeps/app/feature_game/src/main/java/com/example/sheeps/game/ui/components/GameBoard.kt)

- 导入 `androidx.compose.ui.platform.LocalConfiguration`。
- 修改 `GameBoard` 的尺寸计算：
  - 获取屏幕宽度：`val screenWidth = LocalConfiguration.current.screenWidthDp.dp`。
  - 计算棋盘高度：`val boardHeight = (contentHeight + 24).dp`。
  - 计算棋盘宽度：`val boardWidth = screenWidth - 32.dp`。
  - 设置 outer Box 的 `modifier = modifier.size(width = boardWidth, height = boardHeight)`，并通过 `contentAlignment = Alignment.Center` 实现卡片居中。

### 2. [MODIFY] [DuelGameBoard.kt](file:///c:/Users/15613/Documents/file/sheeps/app/feature_game/src/main/java/com/example/sheeps/game/ui/components/DuelGameBoard.kt)

- 导入 `androidx.compose.ui.platform.LocalConfiguration`。
- 修改 `DuelGameBoard` 的尺寸计算（与 `GameBoard` 保持一致）：
  - 计算宽度：`val boardWidth = screenWidth - 32.dp`。
  - 计算高度：`val boardHeight = (contentHeight + 24).dp`。
  - 设置 outer Box 的 `modifier = modifier.size(width = boardWidth, height = boardHeight)`。

## 验证计划

- 确认编译通过。
- 打开闯关模式和对决模式，确认棋盘的两侧与屏幕边缘正好相差 `16.dp`。
- 确认棋盘内部的卡牌在棋盘中水平和垂直方向完美居中，且上下预留了 `12.dp` 的呼吸空间。
