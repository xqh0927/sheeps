# 2026-07-01 飞行卡牌硬件加速动画优化方案

该设计文档旨在解决在关卡消牌时卡牌飞行动画卡顿掉帧的问题，利用 Jetpack Compose 的硬件加速绘图机制（`graphicsLayer`）进行深度渲染性能优化。

## 问题原因

- **状态读取时机错误**：目前 `FlyingTilesLayer` 直接在 Composable 的主体渲染域中读取了 `fly.progress.value` 动画进度值，导致在卡牌飞行的 350ms 过程中，整个动画渲染层在每一帧（每 16ms）都会触发全量重构（Recomposition）和重新布署（Relayout）。
- **坐标单位转换消耗**：每一帧均在主线程进行从 `px`（像素）到 `Dp` 的转换，增加了计算负荷。

## 解决方案与修改要求

- **使用 `graphicsLayer` 延期状态读取**：将 `progress.value` 的读取以及坐标的偏移计算全部移入 `graphicsLayer` Lambda 块内。
  - **原理**：使状态变化仅关联到 Draw（绘制）阶段。Compose 会在卡牌移动时跳过重组和测量，直接通过 GPU 进行硬件加速位移（`translationX`/`translationY`）与缩放（`scale`），大幅提升帧率至满帧（60/120 FPS）。
- **像素直传**：因为 `graphicsLayer.translationX` 直接接收像素单位，可以直接使用 `positionInRoot()` 获得的原始 `Float` 坐标做加减，无需通过密度（`density.toDp()`）进行单位变换，减少计算。

## 涉及文件与修改点

### 1. [MODIFY] [GameScreen.kt](file:///c:/Users/15613/Documents/file/sheeps/app/feature_game/src/main/java/com/example/sheeps/game/ui/screens/GameScreen.kt)

- 修改 `FlyingTilesLayer` 组件：
  - 将 `Modifier.offset(x = xDp, y = yDp).size(sizeDp)` 替换为 `.size(52.dp).graphicsLayer { ... }`。
  - 移除无用的 `density` 参数。

### 2. [MODIFY] [DuelScreen.kt](file:///c:/Users/15613/Documents/file/sheeps/app/feature_game/src/main/java/com/example/sheeps/game/ui/screens/DuelScreen.kt)

- 同样修改 `DuelFlyingTilesLayer` 结构，移除 `density` 变换，改为 `graphicsLayer` 硬件加速平移与缩放。

## 验证计划

- 确认编译通过。
- 点击卡牌进行消除，确认飞行动画在 350ms 内保持绝对流畅、不发生瞬间停顿与掉帧。
