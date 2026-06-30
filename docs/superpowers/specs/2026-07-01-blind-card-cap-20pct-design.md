# 2026-07-01 盲盒牌数量封顶 20% 且阶梯微增设计方案

根据最新要求，优化盲盒卡牌的生成比例公式：
1. **封顶上限**：盲盒占比上限由原本的 35% 调整为 **20%**。
2. **微幅递增**：难度每次提升（即每隔 3 关，即 `levelId % 3 == 0`）时，盲盒牌比例在上一档的基础上递增 **1.5%**（介于 1% - 2% 之间）。
3. **计算公式**：
   - 起步档（第 3 关）：**10%** 比例。
   - 公式：`blindProb = minOf(0.20, 0.10 + (levelId / 3 - 1) * 0.015)`
   - 递增数据：
     - **第 3 关**：$10\%$ 比例
     - **第 6 关**：$11.5\%$ 比例（增加 1.5%）
     - **第 9 关**：$13.0\%$ 比例（增加 1.5%）
     - **第 12 关**：$14.5\%$ 比例（增加 1.5%）
     - **第 15 关**：$16.0\%$ 比例（增加 1.5%）
     - **第 18 关**：$17.5\%$ 比例（增加 1.5%）
     - **第 21 关**：$19.0\%$ 比例（增加 1.5%）
     - **第 24 关及以后**：封顶于 **$20.0\%$**（增加 1.0% 后达到上限）。

## 涉及文件与修改点

### 1. [MODIFY] [GameLevelGenerator.kt](file:///c:/Users/15613/Documents/file/sheeps/app/feature_game/src/main/java/com/example/sheeps/game/viewmodel/helpers/GameLevelGenerator.kt)

- 修改盲盒牌的生成公式：
  ```kotlin
  val blindProb = minOf(0.20, 0.10 + (levelId / 3 - 1) * 0.015)
  ```

### 2. [MODIFY] [level.ts](file:///c:/Users/15613/Documents/file/sheeps/server/src/level.ts)

- 修改服务端的盲盒牌分配公式，使两端一致：
  ```typescript
  const blindProb = Math.min(0.20, 0.10 + (Math.floor(levelId / 3) - 1) * 0.015);
  ```

## 验证计划

- 确认编译通过。
- 确认盲盒牌占比表现温和，高等级关卡中其生成量也严格控制在 20% 以下。
