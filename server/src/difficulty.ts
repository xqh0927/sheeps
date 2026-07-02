import { lcg } from './level';
import { DifficultyInfo, CardCountResult } from './types';

/**
 * 内存缓存：避免重复计算同一 (userId, levelId) 的难度信息。
 * 虽然 getDifficultyForLevel 最坏 O(100) 已足够快，但缓存可消除重复调用开销。
 */
const difficultyCache = new Map<string, DifficultyInfo>();

/**
 * 根据用户 ID 和关卡 ID 获取难度信息。
 *
 * 规则:
 * - L1: D=1, subIndex=0, totalSubLevels=1
 * - L2: D=2, subIndex=0, totalSubLevels=1
 * - L3: D=3, subIndex=0, totalSubLevels=1
 * - L4+: 每个难度段覆盖 1~2 关，由 LCG(userId*9973 + D*10007) 决定。
 *        遍历 D=4→100 直至覆盖目标 levelId。
 * - D>=100: 覆盖所有剩余关卡，卡牌数锁定 300。
 *
 * @param userId  用户唯一标识（数字）
 * @param levelId 关卡编号（从 1 开始）
 * @returns 难度信息
 */
export function getDifficultyForLevel(userId: number, levelId: number): DifficultyInfo {
  const cacheKey = `${userId}_${levelId}`;
  const cached = difficultyCache.get(cacheKey);
  if (cached) return cached;

  let result: DifficultyInfo;

  // 前三关固定
  if (levelId === 1) {
    result = { difficulty: 1, subIndex: 0, totalSubLevels: 1 };
  } else if (levelId === 2) {
    result = { difficulty: 2, subIndex: 0, totalSubLevels: 1 };
  } else if (levelId === 3) {
    result = { difficulty: 3, subIndex: 0, totalSubLevels: 1 };
  } else {
    // 从 D=4 开始，逐个难度段累加，直至覆盖 levelId
    let currentDifficulty = 3;
    let levelCursor = 3; // 已分配的关卡数（L1-L3）

    while (levelCursor < levelId) {
      currentDifficulty++;

      // D>=100 时覆盖所有剩余关卡
      if (currentDifficulty >= 100) {
        const remaining = levelId - levelCursor;
        result = {
          difficulty: 100,
          subIndex: remaining - 1,
          totalSubLevels: Math.max(1, remaining),
        };
        break;
      }

      // LCG 决定本难度段覆盖 1 关还是 2 关
      const rng = lcg(userId * 9973 + currentDifficulty * 10007);
      const levelsForDifficulty: 1 | 2 = rng() < 0.5 ? 1 : 2;

      const nextCursor = levelCursor + levelsForDifficulty;

      if (nextCursor >= levelId) {
        // levelId 落在本难度段内
        const subIndex = levelId - levelCursor - 1; // 0-based
        result = {
          difficulty: currentDifficulty,
          subIndex,
          totalSubLevels: levelsForDifficulty,
        };
        break;
      }

      levelCursor = nextCursor;
    }

    // 兜底（理论上不会到达，但保留以应对边界情况）
    if (!result!) {
      result = { difficulty: 100, subIndex: 0, totalSubLevels: 1 };
    }
  }

  difficultyCache.set(cacheKey, result);
  return result;
}

/**
 * 将数值四舍五入到最近的 3 的倍数。
 *
 * @param value 原始数值
 * @returns 最接近的 3 的倍数
 */
function roundTo3(value: number): number {
  return Math.round(value / 3) * 3;
}

/**
 * 根据难度信息计算基础卡牌数量（不含休息关折扣）。
 *
 * 公式（D≥4）:
 *   rawCards(d) = 60 + 240 × ((d - 3) / 97) ^ 0.55
 *   baseCards(d) = roundTo3(rawCards(d))
 *
 * 硬编码:
 *   D=1 → 12, D=2 → 48, D=3 → 60, D≥100 → 300
 *
 * 子级别微增（totalSubLevels === 2 时）:
 *   subIndex=0 返回 base; subIndex=1 返回 base+delta
 *   delta = max(3, round((baseNext - base) / 2 / 3) * 3)
 *
 * @param info     难度信息
 * @param _levelId 关卡编号（保留参数，兼容接口；子级别由 info.subIndex 处理）
 * @returns 基础卡牌数量（3 的倍数）
 */
export function getBaseCardCount(info: DifficultyInfo, _levelId: number): number {
  const d = info.difficulty;

  if (d === 1) return 12;
  if (d === 2) return 48;
  if (d === 3) return 60;
  if (d >= 100) return 300;

  // 计算本难度段的基础值（subIndex=0 时的值）
  const raw = 60 + 240 * Math.pow((d - 3) / 97, 0.55);
  const base = roundTo3(raw);

  if (info.totalSubLevels === 1) return base;

  // 计算下一难度段的基础值，用于微增
  const rawNext = 60 + 240 * Math.pow((d + 1 - 3) / 97, 0.55);
  const baseNext = roundTo3(rawNext);

  const delta = Math.max(3, Math.round((baseNext - base) / 2 / 3) * 3);

  return info.subIndex === 0 ? base : base + delta;
}

/**
 * 判断是否为休息关卡。
 *
 * 规则: L1-L4 无休息关；L5 起每 5 关（L5, L10, L15…）为休息关。
 *
 * @param levelId 关卡编号
 * @returns 是否为休息关卡
 */
export function isRestLevel(levelId: number): boolean {
  if (levelId < 5) return false;
  return levelId % 5 === 0;
}

/**
 * 根据难度系数获取星级（1~5）。
 *
 * 映射: floor(difficulty / 20) + 1，上限 5。
 *
 * @param difficulty 难度系数 1~100
 * @returns 星级 1~5
 */
function getStars(difficulty: number): number {
  return Math.min(5, Math.floor(difficulty / 20) + 1);
}

/**
 * 根据难度系数获取难度标签文案。
 *
 * @param difficulty 难度系数 1~100
 * @returns 标签文案
 */
function getLabel(difficulty: number): string {
  if (difficulty <= 10) return '轻松';
  if (difficulty <= 30) return '普通';
  if (difficulty <= 60) return '困难';
  if (difficulty <= 90) return '噩梦';
  return '地狱';
}

/**
 * 综合计算最终卡牌数量。
 *
 * 逻辑:
 *   1. 调用 getDifficultyForLevel 获取难度信息
 *   2. 调用 getBaseCardCount 计算基础卡牌数（含子级别微增）
 *   3. 若为休息关卡，乘以 0.80 折扣系数（减少 20%）
 *   4. 四舍五入到 3 的倍数
 *   5. 限制在 [12, 300] 范围内
 *
 * @param userId  用户唯一标识
 * @param levelId 关卡编号
 * @returns 卡牌数量计算结果
 */
export function calculateCardCount(userId: number, levelId: number): CardCountResult {
  const info = getDifficultyForLevel(userId, levelId);
  const baseCards = getBaseCardCount(info, levelId);
  const isRest = isRestLevel(levelId);

  let cardCount: number;
  if (isRest) {
    // 休息关折扣：80%（用户最终决策，非 PRD 中的 75%）
    cardCount = Math.round((baseCards * 0.80) / 3) * 3;
  } else {
    cardCount = baseCards;
  }

  // 安全边界：限制在 [12, 300]
  cardCount = Math.max(12, Math.min(300, cardCount));

  return {
    cardCount,
    isRestLevel: isRest,
    stars: getStars(info.difficulty),
    label: getLabel(info.difficulty),
  };
}
