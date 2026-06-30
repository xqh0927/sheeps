# 2026-07-01 关卡金字塔叠层布局设计方案

该设计文档旨在将关卡生成器中的卡牌层级生成逻辑从原来的“周期性交替对齐”修改为“每一层向内收缩并偏移半个卡牌”的金字塔渐进式堆叠结构。

## 目标与修改要求

- **金字塔堆叠结构**：
  - 每一层 $z$ 的网格大小相比下一层减小 1 单元（即 $size_z = size_0 - z$）。
  - 每一层 $z$ 的偏移量累加 $0.5$（即 $offset_z = z \times 0.5f$）。
  - 达到“每一层往上都向内少半个（各边少 0.25，整层宽度/高度小 1），且完美卡入下层四个卡牌交界缝隙中”的视觉和逻辑布局。
- **边界控制**：若计算出的层级网格大小小于 2，则停止向上堆叠。

## 涉及文件与修改点

### 1. [MODIFY] [GameLevelGenerator.kt](file:///c:/Users/15613/Documents/file/sheeps/app/feature_game/src/main/java/com/example/sheeps/game/viewmodel/helpers/GameLevelGenerator.kt)

- 修改 `generateSolvableLevelLocal` 中动态生成卡牌坐标点的循环逻辑。
- 将：
  ```kotlin
  val size = maxOf(3, baseSize - z / 3)
  val offset = if (z % 2 == 0) 0.0f else 0.5f
  ```
  修改为：
  ```kotlin
  val size = baseSize - z
  if (size < 2) break
  val offset = z * 0.5f
  ```

## 验证计划

- 确认编译通过。
- 打开游戏关卡（如第 2 关或更高级别），观察卡牌布局。
- 确认各层级的卡牌呈现向心收缩的金字塔形状，且每一层卡牌均位于下一层卡牌的对角缝隙中心。
