# 闯关模式卡牌数量固定与图案随机刷新设计方案

## 1. 背景与问题分析
在目前版本的关卡生成逻辑中：
- 客户端在进入或重新开始闯关模式时，会生成一个基于时间的随机数作为 `seed`（种子）。
- 服务端（以及本地离线 fallback 逻辑）使用该随机 `seed` 去做两件事：
  1. 选择关卡形状图案（例如：`shapeType = seed % 18`），并对可能的卡牌网格坐标列表进行洗牌。
  2. 决定卡牌的花色（图案）分配、盲盒卡牌分布及封印卡牌分布。
- 因为不同形状能容纳的坐标上限不同，而如果所选形状能容纳的卡牌数小于难度曲线所计算的目标卡牌数时，最终卡牌数量就会发生截断。这导致同一关卡每次重新玩时，卡牌的总数量和排布形状经常会变动。

## 2. 设计目标
- **卡牌数量与形状固定**：对于任意给定的关卡 `levelId`，进入或重新开始游戏时，卡牌的总数量以及它们所组成的背景轮廓形状、具体坐标排布应当完全一致。
- **卡牌图案与道具属性随机**：在重复进入同一关卡时，卡牌代表的花色（图案类型）分配、盲盒与封印等卡牌属性应当保持随机刷新，保证游戏可玩性。

## 3. 技术方案
通过**分离布局种子（Layout Seed）与图案属性种子（Pattern/Property Seed）**来实现该需求：

### 3.1 服务端修改 (`server/src/level.ts`)
在 `generateSolvableLevel` 函数中：
- **布局生成阶段**：
  - 引入一个只与 `levelId` 关联的固定种子：`const layoutSeed = levelId * 1000;`。
  - 将 `shapeType` 计算修改为使用 `layoutSeed`：
    ```typescript
    const shapeType = layoutSeed % 18;
    ```
  - 将可能的坐标列表打乱（Fisher-Yates Shuffle）时使用 `layoutSeed`：
    ```typescript
    let rand = lcg(layoutSeed);
    ```
  - 这样，对于同一个 `levelId`，`possibleCoords` 的长度和截断后的 `coordinates` 序列（包括它们的三维坐标 `x, y, z`）在每次计算时都绝对固定。
- **花色与属性生成阶段**：
  - 涂色分配的随机生成器 `randAssign` 依然使用传入的随机 `seed`（如 `seed + 100`）。
  - 盲盒与封印属性的随机生成器 `randShuffle`、`randProps` 依然使用传入的随机 `seed`（如 `seed + 200`、`seed + 300`）。

### 3.2 客户端单机离线模式修改 (`GameLevelGenerator.kt`)
本地 `generateSolvableLevelLocal` 函数也使用相同的逻辑处理：
- **布局坐标生成与打乱**使用固定的布局种子：
  ```kotlin
  val layoutSeed = levelId * 1000L
  val rand = lcg(layoutSeed)
  ```
- **花色分配与属性生成**使用传入的随机 `seed`：
  ```kotlin
  val randAssign = lcg(seed + 100)
  ```

---

## 4. 验证计划

### 4.1 单元测试与接口验证
- 检查 `/api/level` 在多次使用不同 `seed` 调用同一个关卡 `id` 时，返回的卡牌数量（数组大小）以及每个 id 对应的坐标 `x, y, z` 是否保持完全相同。
- 检查返回的 `type`（花色）、`isBlind`、`sealedCount` 是否随 `seed` 变化而正常改变。

### 4.2 本地运行验证
- 编译并启动本地客户端，在网络连接情况下多次“重新闯关”，检查卡牌摆放形状及总数量是否无变化，而卡牌花色改变。
- 断开网络后在“单机模式”下进行同样测试，保证离线表现一致。
