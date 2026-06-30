# 2026-07-01 盲盒牌数量与难度系数（关卡等级）挂钩设计方案

该设计文档旨在将盲盒卡牌在关卡中的分配比例（数量）与关卡难度系数（`levelId`）进行动态挂钩，以实现游戏难度阶梯式提升。

## 难度分配公式

对于启用了盲盒牌的关卡（满足 `levelId >= 3 && levelId % 3 == 0`）：
- **基础比例**：从 15% 开始。
- **递增公式**：随着 `levelId` 提升，盲盒比例以每 3 关提升 5% 的幅度增加，公式为：`blindProb = minOf(0.35, 0.10 + (levelId / 3) * 0.05)`。
- **具体关卡比例**：
  - **第 3 关**：$0.10 + 1 \times 0.05 = 15\%$
  - **第 6 关**：$0.10 + 2 \times 0.05 = 20\%$
  - **第 9 关**：$0.10 + 3 \times 0.05 = 25\%$
  - **第 12 关**：$0.10 + 4 \times 0.05 = 30\%$
  - **第 15 关及以后**：封顶于 $35\%$ 比例。

通过这一阶梯式递增，高级关卡的盲盒图案数量将显著多于低级关卡，挑战性更强，同时封顶在 35% 保证游戏不会因未知牌过多而完全失控。

## 涉及文件与修改点

### 1. [MODIFY] [GameLevelGenerator.kt](file:///c:/Users/15613/Documents/file/sheeps/app/feature_game/src/main/java/com/example/sheeps/game/viewmodel/helpers/GameLevelGenerator.kt)

- 修改随机属性分配的概率阈值：
  ```kotlin
  val blindProb = minOf(0.35, 0.10 + (levelId / 3) * 0.05)
  if (r < blindProb) {
      isBlind = true
  }
  ```

### 2. [MODIFY] [level.ts](file:///c:/Users/15613/Documents/file/sheeps/server/src/level.ts)

- 服务端的盲盒属性分配概率同步使用此公式，确保联机对决和本地生成的规则完全一致：
  ```typescript
  const blindProb = Math.min(0.35, 0.10 + Math.floor(levelId / 3) * 0.05);
  if (r < blindProb) {
    isBlind = true;
  }
  ```

## 验证计划

- 确认编译通过。
- 对比生成第 3 关与第 9 关，确认第 9 关生成的盲盒卡牌总数比例明显高于第 3 关（比例从 15% 增加至 25%）。
