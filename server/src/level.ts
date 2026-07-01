import { Point3D, TileData } from './types';

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
 * 生成保证必定有解（Solvable）的关卡卡牌三维布局数据
 * 算法原理：自底向上（Z轴）根据轮廓形状生成可用网格坐标，之后利用 LCG 反向模拟玩家消除步骤（反向从槽位拿牌放到棋盘上暴露位置）以填充花色。
 * 
 * @param levelId 关卡 ID
 * @param seed LCG 随机数种子
 * @return 关卡所有卡牌的数据列表
 */
export function generateSolvableLevel(levelId: number, seed: number): TileData[] {
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
    // 关卡卡牌总数随关卡 ID 严格递增关系：24 + levelId * 12
    const maxCards = 24 + levelId * 12;
    const possibleCoords: Point3D[] = [];

    // 关卡层数随关卡 ID 对数增加：L = 12 - 8 / sqrt(levelId - 1)，最大限制为 12 层
    const layersCount = Math.min(12, Math.floor(12 - 8 / Math.sqrt(levelId - 1)));

    // 棋盘基础网格大小，随关卡增加而变大
    const baseSize = 6 + Math.floor(levelId / 20);

    // 根据种子从 18 种不同的关卡异形轮廓中随机挑选一种进行图形网格过滤
    const shapeType = seed % 18;

    for (let z = 0; z < layersCount; z++) {
      // 网格尺寸逐层递减 1，偏移量逐层累加 0.5，形成向上收缩的金字塔堆叠
      const size = Math.max(3, baseSize - Math.floor(z / 3));
      const offset = (z % 2 === 0) ? 0 : 0.5;
      const center = (size - 1) / 2;

      for (let r = 0; r < size; r++) {
        for (let c = 0; c < size; c++) {
          let keep = true;

          // 相对于棋盘中心的归一化偏移量 (-1.0 至 1.0)
          const dx = center > 0 ? (c - center) / center : 0;
          const dy = center > 0 ? (r - center) / center : 0;
          const distMan = Math.abs(dx) + Math.abs(dy); // 曼哈顿距离
          const distEuclid = Math.sqrt(dx * dx + dy * dy); // 欧氏距离

          switch (shapeType) {
            case 0: // 正方形
              keep = true;
              break;
            case 1: // 金字塔/三角形
              const margin = z * 0.45;
              if (r < margin || r >= size - margin || c < margin || c >= size - margin) {
                keep = false;
              }
              break;
            case 2: // 十字形
              if (Math.abs(c - center) >= 1.2 && Math.abs(r - center) >= 1.2) {
                keep = false;
              }
              break;
            case 3: // 菱形
              if (distMan > 1.15) {
                keep = false;
              }
              break;
            case 4: // 圆环形
              if (distEuclid < 0.4 || distEuclid > 1.05) {
                keep = false;
              }
              break;
            case 5: // X字形
              if (Math.abs(Math.abs(dx) - Math.abs(dy)) > 0.28) {
                keep = false;
              }
              break;
            case 6: // 爱心形
              if (dx * dx + (dy - Math.abs(dx) * 0.6) * (dy - Math.abs(dx) * 0.6) > 0.95) {
                keep = false;
              }
              break;
            case 7: // 沙漏形
              if (Math.abs(dx) > Math.abs(dy) + 0.1) {
                keep = false;
              }
              break;
            case 8: // 星形
              if (Math.abs(dx) >= 0.22 && Math.abs(dy) >= 0.22 && Math.abs(Math.abs(dx) - Math.abs(dy)) >= 0.22) {
                keep = false;
              }
              break;
            case 9: // 空心正方形
              if (Math.abs(dx) <= 0.6 && Math.abs(dy) <= 0.6) {
                keep = false;
              }
              break;
            case 10: // 双圆环
              if (Math.abs(distEuclid - 0.75) > 0.25 && Math.abs(distEuclid - 0.3) > 0.2) {
                keep = false;
              }
              break;
            case 11: // 网格交错空心
              if ((r + c) % 2 !== 0) {
                keep = false;
              }
              break;
            case 12: // 箭头形
              if (dy < -0.7 || Math.abs(dx) > (dy + 1.0) * 0.8) {
                keep = false;
              }
              break;
            case 13: // 蝴蝶形
              if (Math.abs(dx) < Math.abs(dy) * 0.65) {
                keep = false;
              }
              break;
            case 14: // 同心圆
              if (distEuclid >= 0.35 && (distEuclid <= 0.68 || distEuclid >= 1.0)) {
                keep = false;
              }
              break;
            case 15: // 太极双鱼轮廓
              if (distEuclid >= 1.02 || (dy <= Math.sin(dx * Math.PI) * 0.45)) {
                keep = false;
              }
              break;
            case 16: // 梯级楼梯形
              if (r + c < size / 2 || r + c >= size * 1.5) {
                keep = false;
              }
              break;
            case 17: // 六边形
              if (Math.abs(dy) > 0.95 || Math.abs(dx) * 1.5 + Math.abs(dy) > 1.55) {
                keep = false;
              }
              break;
          }

          if (keep) {
            possibleCoords.push({
              x: c + offset + 1.0,
              y: r + offset + 1.0,
              z: z
            });
          }
        }
      }
    }

    // 洗牌坐标：使用确定性 LCG 打乱三维坐标列表
    let rand = lcg(seed);
    for (let i = possibleCoords.length - 1; i > 0; i--) {
      const j = Math.floor(rand() * (i + 1));
      const temp = possibleCoords[i];
      possibleCoords[i] = possibleCoords[j];
      possibleCoords[j] = temp;
    }

    // 强制截断坐标数量至 3 的整数倍，这是卡牌消除必定可解的前提
    const count = Math.min(possibleCoords.length, maxCards) - (Math.min(possibleCoords.length, maxCards) % 3);
    coordinates = possibleCoords.slice(0, count);
  }

  // 重新按高度排序，方便逻辑上自底向上叠放
  coordinates.sort((a, b) => a.z - b.z);

  const W = 52.0 / 46.0;
  const H = 52.0 / 46.0;

  // 关卡卡牌的花色种类 T 随关卡 ID 增长而增加：T = 3 + 3 * ln(levelId)，最多 12 种花色
  const numTypes = levelId === 1 ? 3 : Math.min(12, Math.floor(3 + 3 * Math.log(levelId)));

  interface Node {
    index: number;
    coord: Point3D;
    assignedType: number;
  }

  const nodes: Node[] = coordinates.map((c, idx) => ({
    index: idx,
    coord: c,
    assignedType: -1
  }));

  // 10% 物理重叠遮挡算法：用于判断 A 是否遮挡 B
  const blocks = (a: Point3D, b: Point3D) => {
    if (a.z <= b.z) {
      return false;
    }
    const dx = Math.abs(a.x - b.x);
    const dy = Math.abs(a.y - b.y);
    const ox = Math.max(0, 48.0 - dx * 46.0);
    const oy = Math.max(0, 48.0 - dy * 46.0);
    return ox * oy > 230.4; // 重合面积必须大于 10% 单张牌面积 (48 * 48 = 2304)
  };

  const unassigned = new Set<Node>(nodes);
  let randAssign = lcg(seed + 100);

  // 必定可解生成算法核心（反向还原法）：
  // 1. 在待涂色卡牌集合中，挑出当前未被任何未着色牌压住的最上层“暴露卡牌”。
  // 2. 随机在暴露卡牌中挑出 3 张牌，涂上同一种花色。
  // 3. 将这 3 张牌从待涂色集合中移除，它们下方被压住的卡牌随之“暴露”。
  // 4. 重复上述步骤，直至所有卡牌涂色完成。因为花色分配逻辑与消除步骤完全镜像对称，所以逆向消去必定有解。
  while (unassigned.size > 0) {
    const exposedNodes: Node[] = [];
    for (const node of unassigned) {
      let isCovered = false;
      for (const other of unassigned) {
        if (other !== node && blocks(other.coord, node.coord)) {
          isCovered = true;
          break;
        }
      }
      if (!isCovered) {
        exposedNodes.push(node);
      }
    }

    // 若暴露的可用卡牌少于 3 张，进入回退容错：强制将余下全部卡牌 3 个一组随意涂上相同花色
    if (exposedNodes.length < 3) {
      const rem = Array.from(unassigned);
      while (rem.length >= 3) {
        const type = Math.floor(randAssign() * numTypes) + 1;
        for (let k = 0; k < 3; k++) {
          const n = rem.pop()!;
          n.assignedType = type;
          unassigned.delete(n);
        }
      }
      for (const n of rem) {
        n.assignedType = 1;
        unassigned.delete(n);
      }
      break;
    }

    // 正常涂色：随机指定一种花色并赋予暴露出来的 3 张卡牌
    const type = Math.floor(randAssign() * numTypes) + 1;
    for (let k = 0; k < 3; k++) {
      const idx = Math.floor(randAssign() * exposedNodes.length);
      const chosen = exposedNodes.splice(idx, 1)[0];
      chosen.assignedType = type;
      unassigned.delete(chosen);
    }
  }

  // 基于 LCG 种子计算关卡附加类型：40% 正常关卡、40% 封印关卡、20% 盲盒关卡
  let randType = lcg(seed + 500);
  const typeRoll = randType();
  // 盲盒关卡只在第 3 关及以上启用，概率为 20%
  const isBlindLevel = levelId >= 3 && typeRoll < 0.20;
  // 封印关卡在第 2 关及以上启用，概率为 40% (即 0.20 <= typeRoll < 0.60)
  const isSealedLevel = levelId >= 2 && typeRoll >= 0.20 && typeRoll < 0.60;

  let randProps = lcg(seed + 200);
  const maxZ = Math.max(...nodes.map(n => n.coord.z));
  return nodes.map((node) => {
    let isBlind = false;
    let sealedCount = 0;

    const r = randProps();
    if (isBlindLevel) {
      const blindProb = Math.min(0.20, 0.10 + (levelId - 3) * 0.015);
      // 开局可读性保护：最表层 2~3 层卡牌禁止设为盲盒牌，保证前 3-5 步有可见消除入口
      const limitZ = maxZ >= 4 ? (maxZ - 2) : (maxZ - 1);
      if (r < blindProb && node.coord.z < limitZ) {
        isBlind = true;
      }
    } else if (isSealedLevel) {
      // 封印关卡中有 30% 概率给卡牌贴上解封符纸
      if (r < 0.30) {
        sealedCount = 1;
      }
    }

    return {
      id: `tile_${node.index}`,
      x: node.coord.x,
      y: node.coord.y,
      z: node.coord.z,
      type: node.assignedType,
      isBlind,
      sealedCount
    };
  });
}
