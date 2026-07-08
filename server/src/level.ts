import { Point3D, TileData } from './types';
import { calculateCardCount, isRestLevel } from './difficulty';

/**
 * 根据关卡 ID 获取关卡的基础难度系数
 * 
 * @param levelId 关卡 ID
 * @return 难度系数 (1-3)
 */
export function getDifficultyForLevel(levelId: number): number {
  if (levelId === 1) return 1;
  if (levelId === 2) return 2;
  return 3;
}

/**
 * 确定性线性同余随机数生成器 (LCG - Linear Congruential Generator)
 * 保证使用相同种子时，在前端本地与后端云端生成完全同源、确定性的随机序列。
 * 
 * @param seed 随机数种子
 * @returns 闭包随机数生成函数，每次调用返回 [0, 1) 之间的浮点数
 */
export function lcg(seed: number) {
  let s = seed;
  return function () {
    s = (s * 1664525 + 1013904223) % 4294967296;
    return s / 4294967296;
  };
}

/**
 * 封印门控解锁阈值：每累计消除 N 张正常牌（= 消除 1 组三连）即解锁 1 张封印牌。
 * 与 Android 端 GameViewState.sealedUnlockThreshold 同步，固定为 3（详见 sealed-unlock-mechanism-design.md §6）。
 */
export const SEAL_UNLOCK_THRESHOLD = 3;

/**
 * 生成均匀分布的封印牌
 */
function generateSealedUniformly(
  nodes: { index: number; coord: Point3D; assignedType: number }[],
  rand: () => number,
  sealRatio: number,
  maxLayer: number
): Map<number, number> {
  if (nodes.length === 0) return new Map();

  const maxZ = Math.max(...nodes.map((n) => n.coord.z));
  let eligible = nodes.filter((n) => n.coord.z <= maxZ * 0.7);
  if (eligible.length === 0) eligible = nodes;

  const totalSealed = Math.max(1, Math.floor(nodes.length * sealRatio));

  const shuffled = [...eligible];
  for (let i = shuffled.length - 1; i >= 0; i--) {
    const j = Math.floor(rand() * (i + 1));
    [shuffled[i], shuffled[j]] = [shuffled[j], shuffled[i]];
  }

  const result = new Map<number, number>();
  const chosen = shuffled.slice(0, totalSealed);
  for (const node of chosen) {
    result.set(node.index, randomSealLayer(rand, maxLayer));
  }

  return result;
}

function randomSealLayer(rand: () => number, maxLayer: number): number {
  if (maxLayer === 1) return 1;
  if (maxLayer === 2) return rand() < 0.65 ? 1 : 2;
  const r = rand();
  if (r < 0.50) return 1;
  if (r < 0.85) return 2;
  return 3;
}

/**
 * 生成保证必定有解（Solvable）的关卡卡牌三维布局数据
 * 算法原理：自底向上（Z轴）根据轮廓形状生成可用网格坐标，之后利用 LCG 反向模拟玩家消除步骤（反向从槽位拿牌放到棋盘上暴露位置）以填充花色。
 * 
 * @param userId  用户唯一标识（用于难度曲线千人千面）
 * @param levelId 关卡 ID
 * @param seed    LCG 随机数种子
 * @return 关卡所有卡牌的数据列表
 */
export function generateSolvableLevel(userId: number, levelId: number, seed: number): TileData[] {
  let coordinates: Point3D[] = [];

  // 第 1 关采用简单的新手引导固定布局
  if (levelId === 1) {
    coordinates = [
      { x: 1.0, y: 1.0, z: 0 }, { x: 2.0, y: 1.0, z: 0 },
      { x: 1.0, y: 2.0, z: 0 }, { x: 2.0, y: 2.0, z: 0 },
      { x: 1.5, y: 1.5, z: 1 }, { x: 2.5, y: 1.5, z: 1 },
      { x: 1.5, y: 2.5, z: 1 }, { x: 2.5, y: 2.5, z: 1 },
      { x: 2.0, y: 2.0, z: 2 },
      { x: 2.0, y: 1.0, z: 3 }, { x: 1.0, y: 2.0, z: 3 }, { x: 2.0, y: 2.0, z: 3 }
    ];
  } else {
    // 后续关卡根据算法生成堆叠布局
    // 与 Android 端对齐：maxCards 来自难度曲线，baseSize 按 levelId/2 增长
    const { cardCount: maxCards } = calculateCardCount(userId, levelId);
    const possible: Point3D[] = [];

    const layersCount = Math.min(12, Math.floor(12 - 8 / Math.sqrt(levelId - 1)));
    const baseSize = Math.min(20, 6 + Math.floor(levelId / 2));

    for (let z = 0; z < layersCount; z++) {
      const size = Math.max(3, baseSize - Math.floor(z / 3));
      const colSize = Math.min(6, size);
      const rowSize = Math.min(7, size);
      const offset = z % 2 === 0 ? 0 : 0.5;
      for (let r = 0; r < rowSize; r++) {
        for (let c = 0; c < colSize; c++) {
          // 第 1 关正方形 fallback，其余关卡用 shapeType 选择 18 种形状之一
          possible.push({ x: c + offset + 1.0, y: r + offset + 1.0, z });
        }
      }
    }

    // 引入种子和关卡影响布局，相同 levelId 不同 seed 产生不同排列
    const layoutSeed = seed * 31 + levelId * 1000;
    const rand = lcg(layoutSeed);
    for (let i = possible.length - 1; i > 0; i--) {
      const j = Math.floor(rand() * (i + 1));
      [possible[i], possible[j]] = [possible[j], possible[i]];
    }

    // 确保卡牌总数是 3 的倍数
    const count = Math.min(possible.length, maxCards) - (Math.min(possible.length, maxCards) % 3);
    coordinates = possible.slice(0, count);
  }

  coordinates.sort((a, b) => a.z - b.z);

  type Node = { index: number; coord: Point3D; assignedType: number };
  const nodes: Node[] = coordinates.map((coord, index) => ({ index, coord, assignedType: -1 }));
  const unassigned = new Set<Node>(nodes);

  const numTypes = levelId === 1 ? 3 : Math.min(12, 3 + Math.floor(3 * Math.log(levelId)));
  const randAssign = lcg(seed + 100);

  // 25% 覆盖面积遮挡算法：累计所有更高层卡牌的覆盖面积
  const overlapArea = (a: Point3D, b: Point3D) => {
    if (a.z <= b.z) return 0;
    const dx = Math.abs(a.x - b.x);
    const dy = Math.abs(a.y - b.y);
    const ox = Math.max(0, 48 - dx * 46);
    const oy = Math.max(0, 48 - dy * 46);
    return ox * oy;
  };

  // 反向分配卡牌类型，确保顶层总是存在可消除的组合
  while (unassigned.size > 0) {
    const exposed = [...unassigned].filter((node) => {
      const covered = [...unassigned]
        .filter((other) => other !== node && other.coord.z > node.coord.z)
        .reduce((sum, other) => sum + overlapArea(other.coord, node.coord), 0);
      return covered < 48 * 48 * 0.01;
    });

    if (exposed.length < 3) {
      // 剩余不足三张时强制分配
      const rem = [...unassigned];
      let k = 0;
      while (k + 2 < rem.length) {
        const t = Math.floor(randAssign() * numTypes) + 1;
        rem[k].assignedType = t;
        rem[k + 1].assignedType = t;
        rem[k + 2].assignedType = t;
        unassigned.delete(rem[k]);
        unassigned.delete(rem[k + 1]);
        unassigned.delete(rem[k + 2]);
        k += 3;
      }
      for (const node of unassigned) {
        node.assignedType = 1;
      }
      unassigned.clear();
      break;
    }

    const type = Math.floor(randAssign() * numTypes) + 1;
    const exposedMutable = [...exposed];
    for (let k = 0; k < 3; k++) {
      const idx = Math.floor(randAssign() * exposedMutable.length);
      const chosen = exposedMutable.splice(idx, 1)[0];
      chosen.assignedType = type;
      unassigned.delete(chosen);
    }
  }

  // ===== 固定关卡类型规则（替代随机，与 Android 端对齐） =====
  // 休息关（levelId % 5 == 0，levelId >= 5）：卡牌数量打八折
  // 盲盒关（levelId % 3 == 0，levelId >= 3）：底层卡牌部分变盲盒
  // 封印关（levelId % 2 == 0）：每张卡有概率带封印
  // 优先级: 休息 > 盲盒 > 封印 > 普通
  const isRest = isRestLevel(levelId);
  const isBlindLevel = !isRest && levelId >= 3 && levelId % 3 === 0;
  const isSealedLevel = !isRest && !isBlindLevel && levelId >= 2 && levelId % 2 === 0;

  const maxZ = Math.max(...nodes.map(n => n.coord.z));

  // ===== 坐标归一化：质心对齐模式 =====
  // 目的：确保 Android 客户端渲染时卡牌不会因坐标跨度过大而变得极小
  // 使用质心（center-of-mass）归一化而非包围盒归一化，
  // 保证圆环/圆等非对称形状的视觉重心精确落在棋盘中心，不会整体偏移
  // 注意：归一化仅在最终输出时执行，不影响 blocks() 遮挡判定和花色分配逻辑（它们使用原始坐标）
  const coordCount = coordinates.length;
  const centroidX = coordinates.reduce((s, c) => s + c.x, 0) / coordCount;
  const centroidY = coordinates.reduce((s, c) => s + c.y, 0) / coordCount;

  // 计算相对于质心的最大半径（用于等比缩放）
  let maxRadius = 0;
  for (const c of coordinates) {
    const dx = c.x - centroidX;
    const dy = c.y - centroidY;
    const r = Math.max(Math.abs(dx), Math.abs(dy));
    if (r > maxRadius) maxRadius = r;
  }
  maxRadius = Math.max(maxRadius, 0.1); // 防止除零

  // 目标参数：质心放在 (targetCenter, targetCenter)，最远卡牌距质心 targetRadius 单位
  const targetCenter = 5.5;   // 棋盘中心坐标（输出范围约 [0.5, 10.5]）
  const targetRadius = 4.8;   // 最大半径，留出边距避免裁切
  const normScale2 = targetRadius / maxRadius;

  // 盲盒关卡：均匀分布固定数量的盲盒牌
  let blindIndices: Set<number>;
  if (isBlindLevel) {
    const blindProb = Math.min(0.20, 0.10 + (levelId - 3) * 0.015);
    const limitZ = maxZ >= 4 ? (maxZ - 2) : (maxZ - 1);
    const eligible = nodes.filter((n) => n.coord.z < limitZ);
    const count = Math.max(1, Math.floor(eligible.length * blindProb));
    // Fisher–Yates 洗牌取前 count 个
    const indices = eligible.map((_, i) => i);
    const randShuffle = lcg(seed + 200);
    for (let i = indices.length - 1; i > 0; i--) {
      const j = Math.floor(randShuffle() * (i + 1));
      [indices[i], indices[j]] = [indices[j], indices[i]];
    }
    blindIndices = new Set(indices.slice(0, count).map(i => eligible[i].index));
  } else {
    blindIndices = new Set();
  }

  // 封印关卡：多层封印 + 聚簇分布
  const maxSealLayer = levelId <= 6 ? 1 : levelId <= 14 ? 2 : 3;
  const sealRatio = levelId <= 6 ? 0.30 : levelId <= 14 ? 0.35 : 0.40;
  const clusterCount = levelId <= 6 ? 2 : levelId <= 14 ? 3 : 4;

  const randProps = lcg(seed + 300);
  const sealedClusters = isSealedLevel
    ? generateSealedUniformly(nodes, () => randProps(), sealRatio, maxSealLayer)
    : new Map<number, number>();

  return nodes.map((node) => {
    const isBlind = blindIndices.has(node.index);
    const sealedCount = sealedClusters.get(node.index) ?? 0;

    return {
      id: `tile_${node.index}`,
      x: node.coord.x,
      y: node.coord.y,
      z: node.coord.z,
      type: node.assignedType,
      isBlind,
      sealedCount,
      sealUnlockThreshold: SEAL_UNLOCK_THRESHOLD
    };
  });
}
