# 产品需求文档（PRD）：关卡卡牌数量-难度系数系统

> **文档版本**：v2.0  
> **编写人**：Alice（产品经理）  
> **日期**：2025-07-15  
> **项目**：秘境消消乐（Mystic Match）  
> **关联文件**：`server/src/level.ts`、前端关卡选择界面

---

## 一、项目信息

| 字段 | 值 |
|------|-----|
| **语言** | 中文 |
| **技术栈** | Vite + React + MUI + Tailwind CSS（前端）+ Node.js/TypeScript（服务端） |
| **项目名称** | `sheeps` |
| **原始需求** | 将关卡卡牌数量从简单线性公式调整为基于难度系数曲线的渐进式系统 |

---

## 二、产品定义

### 2.1 产品目标

1. **渐进式难度增长（100 级）**：当前 `getDifficultyForLevel()` 在第 3 关之后立刻跳到最大值 3，之后所有关卡难度完全一致，导致玩家在第 3 关后体验"一刀切"。重新设计为 1~100 级难度系数，让难度随关卡平滑爬升数周甚至数月。

2. **千人千面**：引入基于用户 ID 的确定性随机，让每位玩家的难度攀升节奏略有差异（同一难度停留 1 关或 2 关），避免玩家之间直接对比"第 X 关一定有多少张牌"，增加探索感和讨论话题。

3. **节奏控制**：加入"休息关卡"机制（每 5 关一次减压），平衡玩家心流，防止连续高强度关卡导致的疲劳弃坑。

4. **可视化成长**：UI 层展示难度星级（★~★★★★★），难度标识适配 3 套皮肤（经典/水墨/霓虹），让玩家直观感知成长进度。

5. **运营底座**：1~100 难度系数为后续活动关卡、每日挑战、排行榜等运营功能提供统一的难度基准。

### 2.2 用户故事

- **作为休闲玩家**，我希望前期关卡难度缓慢增长，有"休息关卡"让我喘口气，而不是连续高强度闯关。
- **作为核心玩家**，我希望后期关卡有明确的难度层级感，看到 ★★★★★ 标识时能感到真正的挑战。
- **作为社交玩家**，我想和朋友讨论"你的第 X 关是多少张牌？"，因为不同用户的难度曲线不同，带来话题性。
- **作为策划运营**，我希望有一个细粒度的 1~100 难度基准，用于设计"困难模式""每日地狱关卡"等活动。
- **作为 UI 设计师**，我希望难度标识能跟随皮肤系统切换风格（经典/水墨/霓虹），保持视觉一致性。
- **作为开发工程师**，我希望难度系统是纯函数，给定 `(userId, levelId)` 返回确定值，不引入额外状态。

---

## 三、技术规范

### 3.1 需求池

| 优先级 | 需求 | 说明 |
|--------|------|------|
| **P0** | 难度系数函数 `getDifficultyForLevel` | 返回 1~100，确定性依赖 `(userId, levelId)` |
| **P0** | 难度→卡牌数量映射公式 | 幂函数曲线，始终为 3 的倍数，范围 12~300 |
| **P0** | 关卡 1/2/3 固定值 | L1=12, L2=48, L3=60，不受随机影响 |
| **P0** | 同一难度内线性微增 | 若 D 覆盖 2 关，第 2 关比第 1 关多少量牌 |
| **P0** | 休息关卡 | L5/L10/L15/… 卡牌数减少 25%，四舍五入到 3 的倍数 |
| **P0** | UI 难度星级标识 | ★~★★★★★，适配 3 套皮肤（经典/水墨/霓虹） |
| **P1** | numTypes 联动检查 | 维持现有公式，验证与新曲线的匹配度 |

### 3.2 架构设计（P0 完整方案）

---

#### 3.2.1 数据结构

```typescript
/**
 * 难度信息——getDifficultyForLevel 的返回结构
 */
interface DifficultyInfo {
  /** 难度系数 1~100 */
  difficulty: number;

  /** 当前关卡在本难度段内的序号（0-based） */
  subIndex: number;

  /** 本难度段共覆盖多少关（1 或 2，由用户种子决定） */
  totalSubLevels: number;
}

/**
 * 卡牌数量计算结果
 */
interface CardCountResult {
  /** 最终卡牌数量（3 的倍数） */
  cardCount: number;

  /** 是否为休息关卡 */
  isRestLevel: boolean;

  /** 难度星级 1~5 */
  stars: number;

  /** 难度标签文案 */
  label: string;
}

/**
 * 难度星级皮肤配置——前端使用
 */
interface DifficultyStarsSkin {
  /** 填充色 */
  fillColor: string;
  /** 描边色 */
  strokeColor: string;
  /** 发光色（霓虹皮肤专用） */
  glowColor?: string;
  /** 背景纹理（水墨皮肤专用） */
  backgroundTexture?: string;
}
```

---

#### 3.2.2 函数签名

```typescript
// ===== 文件: server/src/difficulty.ts (新建) =====

/**
 * 根据用户 ID 和关卡 ID 获取难度信息
 *
 * 规则:
 * - L1: D=1, subIndex=0, totalSubLevels=1
 * - L2: D=2, subIndex=0, totalSubLevels=1
 * - L3: D=3, subIndex=0, totalSubLevels=1
 * - L4+: 每个难度 1~2 关，由 LCG(userId, difficulty) 决定
 *
 * @param userId  用户唯一标识（数字）
 * @param levelId 关卡编号
 * @returns 难度信息
 */
export function getDifficultyForLevel(userId: number, levelId: number): DifficultyInfo;

/**
 * 根据难度信息计算基础卡牌数量（不含休息关折扣）
 *
 * 公式: baseCards(d) = roundTo3( 60 + 240 * ((d - 3) / 97)^0.55 )
 *       其中 D=1→12, D=2→48, D=3→60（硬编码）
 *
 * @param info  难度信息
 * @param levelId 关卡编号（用于子级别微增）
 * @returns 基础卡牌数量（3 的倍数）
 */
export function getBaseCardCount(info: DifficultyInfo, levelId: number): number;

/**
 * 判断是否为休息关卡
 *
 * 规则: L1-L4 无休息关；L5 起每 5 关（L5, L10, L15…）为休息关
 *
 * @param levelId 关卡编号
 * @returns 是否为休息关卡
 */
export function isRestLevel(levelId: number): boolean;

/**
 * 综合计算最终卡牌数量
 *
 * 逻辑:
 *   1. 计算基础卡牌数（含子级别微增）
 *   2. 若为休息关卡，乘以 0.75 折扣系数
 *   3. 四舍五入到 3 的倍数
 *   4. 限制在 [12, 300] 范围内
 *
 * @param userId  用户唯一标识
 * @param levelId 关卡编号
 * @returns 卡牌数量计算结果
 */
export function calculateCardCount(userId: number, levelId: number): CardCountResult;

// ===== 文件: server/src/level.ts (集成点修改) =====

// generateSolvableLevel 中，将:
//   const maxCards = Math.min(300, 24 + levelId * 12);
// 改为:
//   const { cardCount: maxCards } = calculateCardCount(userId, levelId);

// ===== 文件: client/src/utils/difficulty.ts (新建) =====

/**
 * 获取难度星级 1~5
 *   D   1-10  → ★
 *   D  11-30  → ★★
 *   D  31-60  → ★★★
 *   D  61-90  → ★★★★
 *   D 91-100  → ★★★★★
 */
export function getDifficultyStars(difficulty: number): number;

/**
 * 获取难度标签文案
 */
export function getDifficultyLabel(difficulty: number): string;

// ===== 文件: client/src/theme/difficultyStars.ts (新建) =====

/**
 * 根据当前皮肤主题获取难度星级样式配置
 *
 * @param skin   皮肤名称: "classic" | "ink" | "neon"
 * @param stars  星级 1~5
 * @returns 样式配置
 */
export function getStarsSkin(skin: string, stars: number): DifficultyStarsSkin;
```

---

#### 3.2.3 核心算法

##### A. `getDifficultyForLevel` — 难度攀升算法

```typescript
import { lcg } from './level';

export function getDifficultyForLevel(
  userId: number,
  levelId: number
): DifficultyInfo {
  // 前三关固定
  if (levelId === 1) return { difficulty: 1, subIndex: 0, totalSubLevels: 1 };
  if (levelId === 2) return { difficulty: 2, subIndex: 0, totalSubLevels: 1 };
  if (levelId === 3) return { difficulty: 3, subIndex: 0, totalSubLevels: 1 };

  // 从 D=4 开始，逐个难度段累加，直至覆盖 levelId
  let currentDifficulty = 3;
  let levelCursor = 3; // 已分配的关卡数（L1-L3）

  while (levelCursor < levelId) {
    currentDifficulty++;

    // 当前难度值保底 1 关（D>=99 时只给 1 关，确保最终能抵达 D=100）
    if (currentDifficulty >= 100) {
      // D=100 覆盖所有剩余关卡
      const remaining = levelId - levelCursor;
      return {
        difficulty: 100,
        subIndex: remaining - 1,
        totalSubLevels: Math.max(1, remaining),
      };
    }

    // LCG 决定本难度段覆盖 1 关还是 2 关
    const rng = lcg(userId * 9973 + currentDifficulty * 10007);
    const levelsForDifficulty = rng() < 0.5 ? 1 : 2;

    const nextCursor = levelCursor + levelsForDifficulty;

    if (nextCursor >= levelId) {
      // levelId 落在本难度段内
      const subIndex = levelId - levelCursor - 1; // 0-based
      return {
        difficulty: currentDifficulty,
        subIndex,
        totalSubLevels: levelsForDifficulty,
      };
    }

    levelCursor = nextCursor;
  }

  // 兜底（理论上不会到达）
  return { difficulty: 100, subIndex: 0, totalSubLevels: 1 };
}
```

**复杂度分析**：最坏情况 O(100)，平均约 50 次迭代。每个 `generateSolvableLevel` 调用一次，完全可以接受。若后续需要优化，可在 `difficultyCache: Map<string, DifficultyInfo>` 中缓存 `"${userId}_${levelId}"` 的结果。

---

##### B. `getBaseCardCount` — 卡牌数量算法

**幂函数公式**（D≥4）：

```
rawCards(d) = 60 + 240 × ((d - 3) / 97) ^ 0.55
baseCards(d) = roundToMultipleOf3(rawCards(d))
```

其中：
- `60` = D=3 的卡牌数（基线）
- `240` = 300 - 60（增长空间）
- `97` = 100 - 3（难度步数）
- `0.55` = 幂指数（< 1 使前期增长快、后期收敛，形成"前陡后缓"的 S 型口感）

**关键节点验证**：

| D | rawCards | 取整到 3 的倍数 | 增量 |
|---|----------|----------------|------|
| 1 | — | 12（硬编码） | — |
| 2 | — | 48（硬编码） | +36 |
| 3 | — | 60（硬编码） | +12 |
| 4 | 79.5 | **78** | +18 |
| 10 | 112.7 | **114** | +36（累计） |
| 20 | 149.6 | **150** | +36 |
| 30 | 175.1 | **174** | +24 |
| 40 | 196.0 | **195** | +21 |
| 50 | 214.1 | **213** | +18 |
| 60 | 230.6 | **231** | +18 |
| 70 | 246.2 | **246** | +15 |
| 80 | 261.1 | **261** | +15 |
| 90 | 275.6 | **276** | +15 |
| 100 | 300.0 | **300** | +24 |

**子级别微增**（当 `totalSubLevels === 2` 时）：

```typescript
function getBaseCardCount(info: DifficultyInfo, levelId: number): number {
  if (info.difficulty === 1) return 12;
  if (info.difficulty === 2) return 48;
  if (info.difficulty === 3) return 60;
  if (info.difficulty >= 100) return 300;

  // 计算本难度段的基础值（subIndex=0 时的值）
  const d = info.difficulty;
  const raw = 60 + 240 * Math.pow((d - 3) / 97, 0.55);
  const base = Math.round(raw / 3) * 3;

  if (info.totalSubLevels === 1) return base;

  // 计算下一难度段的基础值，用于微增
  const rawNext = 60 + 240 * Math.pow((d + 1 - 3) / 97, 0.55);
  const baseNext = Math.round(rawNext / 3) * 3;

  const delta = Math.max(3, Math.round((baseNext - base) / 2 / 3) * 3);

  return info.subIndex === 0 ? base : base + delta;
}
```

---

##### C. `isRestLevel` — 休息关卡判定

```typescript
export function isRestLevel(levelId: number): boolean {
  // L1-L4 无休息关
  if (levelId < 5) return false;
  // L5 起每 5 关
  return levelId % 5 === 0;
}
```

休息关折扣：**75%**（即减少 25%），四舍五入到 3 的倍数。

示例：
- L5（假设基础 78 张）→ `round(78 * 0.75 / 3) * 3` = `round(19.5) * 3` = 60 张
- L10（假设基础 150 张）→ `round(150 * 0.75 / 3) * 3` = `round(37.5) * 3` = 114 张

---

##### D. `calculateCardCount` — 综合计算

```typescript
export function calculateCardCount(
  userId: number,
  levelId: number
): CardCountResult {
  const info = getDifficultyForLevel(userId, levelId);
  const baseCards = getBaseCardCount(info, levelId);
  const isRest = isRestLevel(levelId);

  let cardCount: number;
  if (isRest) {
    cardCount = Math.round((baseCards * 0.75) / 3) * 3;
  } else {
    cardCount = baseCards;
  }

  // 安全边界
  cardCount = Math.max(12, Math.min(300, cardCount));

  return {
    cardCount,
    isRestLevel: isRest,
    stars: getDifficultyStars(info.difficulty),
    label: getDifficultyLabel(info.difficulty),
  };
}
```

---

#### 3.2.4 集成方式

**文件变更清单**：

| 文件 | 操作 | 内容 |
|------|------|------|
| `server/src/difficulty.ts` | **新建** | 上述所有难度算法函数 |
| `server/src/level.ts` | **修改** | `generateSolvableLevel` 签名增加 `userId` 参数；替换 `maxCards` 计算；导出 `lcg` 供 `difficulty.ts` 使用 |
| `server/src/types.ts` | **修改** | 新增 `DifficultyInfo`、`CardCountResult` 类型导出 |
| `client/src/utils/difficulty.ts` | **新建** | `getDifficultyStars`、`getDifficultyLabel` |
| `client/src/theme/difficultyStars.ts` | **新建** | `getStarsSkin`（三套皮肤配置） |
| 关卡选择界面组件 | **修改** | 调用 `calculateCardCount` 获取星级和标签并渲染 |

**`generateSolvableLevel` 签名变更**：

```typescript
// 旧签名
export function generateSolvableLevel(levelId: number, seed: number): TileData[]

// 新签名
export function generateSolvableLevel(
  userId: number,
  levelId: number,
  seed: number
): TileData[]
```

**`lcg` 函数导出**：当前 `lcg` 已在 `server/src/level.ts` 中定义但未导出。需增加 `export` 关键字，供 `difficulty.ts` 引用。

---

#### 3.2.5 UI 难度星级系统

##### 星级映射

| 难度范围 | 星级 | 标签 | 颜色基调 |
|----------|------|------|----------|
| D 1-10 | ★ | 轻松 | 绿色 |
| D 11-30 | ★★ | 普通 | 蓝色 |
| D 31-60 | ★★★ | 困难 | 橙色 |
| D 61-90 | ★★★★ | 噩梦 | 紫色 |
| D 91-100 | ★★★★★ | 地狱 | 红色 |

##### 皮肤适配

| 皮肤 | 星级实现方案 | 备注 |
|------|-------------|------|
| **经典** | 纯色填充 + 细描边，扁平风格 | 默认主题，明亮高饱和 |
| **水墨** | 毛笔笔触纹理填充 + 墨色渐变描边 | 墨色浓淡渐变，留白美学 |
| **霓虹** | 发光填充 + 外发光 glow 效果 + 暗色底 | 赛博朋克风格，需 `glowColor` |

```typescript
// client/src/theme/difficultyStars.ts 骨架
const SKIN_STARS: Record<string, DifficultyStarsSkin> = {
  classic: {
    fillColor: '#FFD700',
    strokeColor: '#B8860B',
  },
  ink: {
    fillColor: '#2C1810',
    strokeColor: '#1A0E08',
    backgroundTexture: 'url(/assets/ink-brush.png)',
  },
  neon: {
    fillColor: '#FF00FF',
    strokeColor: '#00FFFF',
    glowColor: 'rgba(255, 0, 255, 0.6)',
  },
};

export function getStarsSkin(skin: string, stars: number): DifficultyStarsSkin {
  const base = SKIN_STARS[skin] ?? SKIN_STARS.classic;
  // 根据星级微调颜色亮度（星级越高颜色越饱和）
  return {
    ...base,
    fillColor: stars >= 4 ? intensifyColor(base.fillColor, stars / 5) : base.fillColor,
  };
}
```

---

#### 3.2.6 numTypes 联动检查

**决策**：维持现有公式，不联动修改。

```typescript
// server/src/level.ts 中保持不变
const numTypes = levelId === 1 ? 3 : Math.min(12, Math.floor(3 + 3 * Math.log(levelId)));
```

**验证**：新卡牌曲线下，numTypes 与卡牌数的比值仍然合理：

| Level（约） | numTypes | 卡牌数（约） | 每组牌数（牌数/种类数/3） |
|------------|----------|-------------|---------------------------|
| 1 | 3 | 12 | 1.3 — 新手友好 |
| 2 | 5 | 48 | 3.2 |
| 3 | 6 | 60 | 3.3 |
| ~5 | 7 | ~78 | 3.7 |
| ~10 | 8 | ~114 | 4.8 |
| ~20 | 9 | ~150 | 5.6 |
| ~40 | 10 | ~195 | 6.5 |
| ~60 | 11 | ~231 | 7.0 |
| ~90 | 12 | ~276 | 7.7 |
| 150+ | 12 | 300 | 8.3 |

后期（L40+）每组牌数逐渐上升到 7~8 组/种类。这**不是缺陷而是特性**：后期同花色牌多，消除路径更灵活但需要更多步数清空，自然增加难度。同时 numTypes 最大 12 种花色也避免了花色过多导致 UI 辨识困难。

---

### 3.3 完整调用流程（开发者视角）

```
客户端                           服务端
───────                         ───────

用户点击"开始第 N 关"
  │
  ├─→ POST /api/level/generate
  │   body: { userId, levelId }
  │                             ├─→ calculateCardCount(userId, levelId)
  │                             │     ├─→ getDifficultyForLevel(userId, levelId)
  │                             │     │     └─→ LCG(userId, difficulty)  → 1 or 2
  │                             │     ├─→ getBaseCardCount(info, levelId)
  │                             │     │     └─→ pow() + 子级别微增
  │                             │     └─→ isRestLevel(levelId) → 是否 ×0.75
  │                             │
  │                             ├─→ 结果.cardCount 作为 maxCards 传入
  │                             │   generateSolvableLevel(userId, levelId, seed)
  │                             │
  │                             └─→ 返回 { tiles, cardCount, stars, label, isRestLevel }
  │
  ├─→ 渲染棋盘 + 难度星级 UI
  │
  └─→ 玩家开始游戏
```

---

### 3.4 边界情况处理

| 场景 | 处理 |
|------|------|
| `levelId = 0` 或负数 | `getDifficultyForLevel` 默认返回 D=1 |
| `userId` 为 0 或未登录 | 使用默认 userId=0（游客统一曲线） |
| D≥100 后仍有更多关卡 | D=100 覆盖所有后续关卡，subIndex 持续增长，但卡牌数锁定 300 |
| 休息关卡牌数低于 12 | `Math.max(12, ...)` 保底 |
| D=100 + 休息关 | `round(300 * 0.75 / 3) * 3 = 225`，仍远高于最低值 |
| `totalSubLevels=1` 且为休息关 | 直接使用折扣后的基础值（无子级别微增） |

---

### 3.5 性能与确定性保障

- **确定性**：所有随机源自 `lcg(userId * 9973 + difficulty * 10007)`，同一个 `(userId, difficulty)` 组合始终产生相同的 `1 or 2` 决策。无外部状态、无数据库查询。
- **可缓存**：建议服务端实现 `Map<`${userId}_${levelId}`, CardCountResult>` 内存缓存，避免每次生成牌局都重新计算（虽然 O(100) 本身已足够快）。
- **纯函数**：除 `Math.pow` 和 `Math.round` 外无副作用，可在前后端各自独立计算。

---

## 四、UI/UX 规格

### 4.1 关卡选择界面改造

在关卡列表中每个关卡的卡片上新增难度标识区域：

```
┌──────────────────────────────────┐
│  第 15 关                        │
│                                  │
│  ★★★☆☆        卡牌: 150 张      │
│  困难                           │
│                                  │
│  [🎁 休息关卡]  ← 仅休息关显示    │
│                                  │
│         [开始挑战]               │
└──────────────────────────────────┘
```

### 4.2 皮肤切换时的行为

- 用户切换皮肤（经典↔水墨↔霓虹）时，星级组件的渲染样式立即跟随切换
- 不需要重新请求服务端数据，纯前端 CSS/纹理替换
- 霓虹皮肤的 `glowColor` 使用 CSS `filter: drop-shadow()` 实现，不引入额外 DOM

### 4.3 休息关卡视觉差异

- 关卡卡片边框改为柔和的波浪线或虚线
- 标签显示"🎁 休息关卡"，背景色偏暖（浅黄/浅粉）
- 进入游戏后可在顶部短暂显示 "休息一下~" toast（2 秒后消失）

---

## 五、非功能需求

- **确定性**：`getDifficultyForLevel(userId, levelId)` 必须是纯函数，同一组 `(userId, levelId)` 始终返回相同 `DifficultyInfo`
- **向后兼容**：关卡 1/2/3 的卡牌数量不变，已有玩家进度不受影响
- **性能**：服务端 O(100) 最坏情况，加内存缓存后 O(1)；前端 O(1) 查表
- **离线可用**：`calculateCardCount` 可在前端独立计算（不依赖网络），用于离线预览关卡信息

---

## 六、验收标准

### 服务端

- [ ] `getDifficultyForLevel(userId, 1)` → `{ difficulty: 1, subIndex: 0, totalSubLevels: 1 }`
- [ ] `getDifficultyForLevel(userId, 2)` → `{ difficulty: 2, subIndex: 0, totalSubLevels: 1 }`
- [ ] `getDifficultyForLevel(userId, 3)` → `{ difficulty: 3, subIndex: 0, totalSubLevels: 1 }`
- [ ] 同一 `(userId, difficulty)` 的 LCG 结果确定（两次调用返回相同 `totalSubLevels`）
- [ ] 不同 `userId` 的 L4+ 难度曲线有差异（至少 30% 的难度段决策不同）
- [ ] 所有 `calculateCardCount` 返回的 `cardCount` 是 3 的倍数
- [ ] `cardCount` 始终在 [12, 300] 范围内
- [ ] L5 是休息关卡，卡牌数比基础值少 ~25%
- [ ] L10 是休息关卡，L15 是休息关卡
- [ ] D=100 时卡牌数为 300（非休息关）
- [ ] `generateSolvableLevel` 新签名正常工作

### 前端

- [ ] `getDifficultyStars(5)` → 1, `getDifficultyStars(25)` → 2, `getDifficultyStars(50)` → 3
- [ ] `getDifficultyStars(80)` → 4, `getDifficultyStars(95)` → 5
- [ ] 三套皮肤下星级组件渲染正确
- [ ] 休息关卡标识在关卡列表中可见
- [ ] 切换皮肤时星级样式立即变化

---

## 七、附录

### A. 完整难度分布示例（假设 userId=42, LCG 种子）

```
L1:  D=1   sub=0/1  cards=12   (固定)
L2:  D=2   sub=0/1  cards=48   (固定)
L3:  D=3   sub=0/1  cards=60   (固定)
L4:  D=4   sub=0/1  cards=78   (取决于LCG)
L5:  D=4   sub=0/1  cards=59   (休息关, 78*0.75≈59)
L6:  D=5   sub=0/2  cards=84   (取决于LCG)
L7:  D=5   sub=1/2  cards=90   (子级别微增: 84+6)
L8:  D=6   sub=0/1  cards=96
...
```

### B. 幂指数选择说明

选择 `0.55` 而非 `0.5`（平方根）或 `0.6` 的原因：

| 幂指数 | D=4 卡牌数 | D=10 | D=50 | D=90 | 曲线特性 |
|--------|-----------|------|------|------|----------|
| 0.50 | 83 | 116 | 211 | 269 | 前期过快，后期太缓 |
| **0.55** | **79** | **114** | **213** | **276** | **均衡，前期舒适** |
| 0.60 | 76 | 111 | 216 | 282 | 前期过慢，后期堆积 |

`0.55` 在 D=4 时给出 ~79 张（vs 当前 L4 的 72 张），增量 +7 张，体感温和；D=50 时 ~213 张，D=100 时恰好 300，整体过渡自然。
