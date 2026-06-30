# 2026-07-01 客户端与服务端遮挡逻辑全量对齐设计方案

该设计文档旨在解决将重叠遮挡规则修改为 `W=1.0` 后引起的“点击时发生物理穿透”（可以点击视觉上压在底层的卡牌）的问题，并实现客户端、本地生成器以及服务端生成器的重叠遮挡判定完美对齐。

## 问题原因

- **视觉穿透漏洞**：当卡牌偏移量为 1.0 时（例如相隔两层的卡牌在 X/Y 轴投影差值为 1.0），物理上其实存在 6dp 的小面积重叠。若将遮挡判定设为 `W=1.0`，则该卡牌被认为“不重叠”，玩家就可以隔空点击被压在下层的卡牌，产生“视觉穿透”的体验。
- **物理与求解不一致**：真正的物理重叠公式应为 `渲染宽度 / 逻辑步长 = 52.0 / 46.0`（约 `1.13`）。要保证游戏既不穿透，又保证 100% 可解，必须将**客户端游戏判定、本地客户端生成器判定、服务端生成器判定**全部统一对齐至该真实物理值 `52.0 / 46.0`。

## 涉及文件与修改点

### 1. [MODIFY] [GameEngine.kt](file:///c:/Users/15613/Documents/file/sheeps/app/core/src/main/java/com/example/sheeps/core/game/GameEngine.kt)

- 将 `W` 和 `H` 还原为物理真实的 **`52.0f / 46.0f`**（约 `1.13f`），防止出现点击穿透。

### 2. [MODIFY] [GameLevelGenerator.kt](file:///c:/Users/15613/Documents/file/sheeps/app/feature_game/src/main/java/com/example/sheeps/game/viewmodel/helpers/GameLevelGenerator.kt)

- 修改 `blocks` 闭包中的坐标重叠判定，将其对齐至相同的物理值：
  ```kotlin
  val blocks = { a: Point3D, b: Point3D ->
      a.z > b.z && abs(a.x - b.x) < 52.0f / 46.0f && abs(a.y - b.y) < 52.0f / 46.0f
  }
  ```

### 3. [MODIFY] [level.ts](file:///c:/Users/15613/Documents/file/sheeps/server/src/level.ts)

- 修改服务端生成算法中的 `W` 和 `H` 常量，使之与客户端严格同步：
  ```typescript
  const W = 52.0 / 46.0;
  const H = 52.0 / 46.0;
  ```

## 验证计划

- 确认编译通过。
- 打开闯关和对决模式，确认被压在下层的卡牌（包括相隔多层但有小面积重叠的卡牌）显示为正确的灰色变暗锁定状态，且不可被“穿透点击”。
- 验证经过重新对齐后的生成器生成的地图在整个游戏过程中完全能顺利解开。
