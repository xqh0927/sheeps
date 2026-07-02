import { generateSolvableLevel, lcg } from '../src/level';
import { TileData } from '../src/types';

/**
 * 单元测试：验证基于难度系数曲线的关卡卡牌数系统
 * 固定使用 userId=0 进行测试（游客统一曲线）
 */

describe('关卡卡牌数测试（难度曲线系统）', () => {
  
  describe('maxCards 基于难度系数计算', () => {
    test('关卡 1 应使用固定布局（12张牌）', () => {
      const tiles = generateSolvableLevel(0, 1, 12345);
      expect(tiles.length).toBe(12);
    });

    test('关卡 2: 固定 48 张牌', () => {
      const tiles = generateSolvableLevel(0, 2, 12345);
      expect(tiles.length).toBe(48);
    });

    test('关卡 3: 固定 60 张牌', () => {
      const tiles = generateSolvableLevel(0, 3, 12345);
      expect(tiles.length).toBe(60);
    });

    test('关卡 100: 难度 D=100，卡牌数 300', () => {
      const tiles = generateSolvableLevel(0, 100, 12345);
      expect(tiles.length).toBe(300);
    });

    test('关卡 200: D=100 覆盖所有剩余关卡，仍为 300', () => {
      const tiles = generateSolvableLevel(0, 200, 12345);
      expect(tiles.length).toBe(300);
    });

    test('关卡 50: 卡牌数为 3 的倍数且在 [12, 300] 范围内', () => {
      const tiles = generateSolvableLevel(0, 50, 12345);
      expect(tiles.length % 3).toBe(0);
      expect(tiles.length).toBeGreaterThanOrEqual(12);
      expect(tiles.length).toBeLessThanOrEqual(300);
    });
  });

  describe('baseSize 足够支持高关卡', () => {
    test('关卡 100 的 baseSize 应能生成足够坐标', () => {
      const tiles = generateSolvableLevel(0, 100, 12345);
      expect(tiles.length).toBe(300);
    });

    test('关卡 200 的 baseSize 应能生成足够坐标', () => {
      const tiles = generateSolvableLevel(0, 200, 12345);
      expect(tiles.length).toBe(300);
    });
  });

  describe('坐标数量验证', () => {
    test('多种形状在高关卡下应生成足够卡牌', () => {
      for (let seed = 0; seed < 18; seed++) {
        const tiles = generateSolvableLevel(0, 100, seed);
        expect(tiles.length).toBeGreaterThanOrEqual(252);
        expect(tiles.length % 3).toBe(0);
      }
    });
  });

  describe('count 是 3 的倍数', () => {
    test('所有关卡的卡牌数必须是 3 的倍数', () => {
      const testLevels = [1, 2, 3, 10, 22, 23, 50, 100, 150, 200];
      for (const levelId of testLevels) {
        for (let seed = 0; seed < 10; seed++) {
          const tiles = generateSolvableLevel(0, levelId, seed);
          expect(tiles.length % 3).toBe(0);
        }
      }
    });
  });

  describe('算法正确性验证', () => {
    test('生成的卡牌应包含必要的字段', () => {
      const tiles = generateSolvableLevel(0, 100, 12345);
      expect(tiles.length).toBe(300);
      
      for (const tile of tiles) {
        expect(tile.id).toBeDefined();
        expect(tile.x).toBeDefined();
        expect(tile.y).toBeDefined();
        expect(tile.z).toBeDefined();
        expect(tile.type).toBeGreaterThanOrEqual(1);
        expect(typeof tile.isBlind).toBe('boolean');
        expect(typeof tile.sealedCount).toBe('number');
      }
    });

    test('卡牌类型数量应随关卡增加', () => {
      const tiles1 = generateSolvableLevel(0, 1, 12345);
      const tiles100 = generateSolvableLevel(0, 100, 12345);
      
      const types1 = new Set(tiles1.map(t => t.type));
      const types100 = new Set(tiles100.map(t => t.type));
      
      // 关卡 1 应有 3 种类型
      expect(types1.size).toBeLessThanOrEqual(3);
      // 关卡 100 应有更多类型（最多12种）
      expect(types100.size).toBeGreaterThan(types1.size);
    });
  });

  describe('边界情况验证', () => {
    test('关卡 1 的特殊固定布局不受修改影响', () => {
      const tiles = generateSolvableLevel(0, 1, 12345);
      // 关卡1应有12张牌（固定布局 + 新难度系统 L1=12）
      expect(tiles.length).toBe(12);
      // 验证坐标是否正确
      expect(tiles[0]).toEqual({ id: 'tile_0', x: expect.any(Number), y: expect.any(Number), z: 0, type: expect.any(Number), isBlind: false, sealedCount: 0 });
    });

    test('多次生成相同关卡应具有确定性', () => {
      const tiles1 = generateSolvableLevel(42, 23, 12345);
      const tiles2 = generateSolvableLevel(42, 23, 12345);
      
      expect(tiles1.length).toBe(tiles2.length);
      for (let i = 0; i < tiles1.length; i++) {
        expect(tiles1[i].x).toBe(tiles2[i].x);
        expect(tiles1[i].y).toBe(tiles2[i].y);
        expect(tiles1[i].z).toBe(tiles2[i].z);
        expect(tiles1[i].type).toBe(tiles2[i].type);
      }
    });
  });

  describe('layersCount 验证', () => {
    test('高关卡时 layersCount 应足够大以支持 300 张卡牌', () => {
      const tiles = generateSolvableLevel(0, 100, 12345);
      const maxZ = Math.max(...tiles.map(t => t.z));
      
      // 至少有 5 层（这样才能容纳300张牌）
      expect(maxZ).toBeGreaterThanOrEqual(4);
    });
  });
});

describe('LCG 随机数生成器测试', () => {
  test('LCG 应使用相同种子生成相同序列', () => {
    const rand1 = lcg(12345);
    const rand2 = lcg(12345);
    
    for (let i = 0; i < 100; i++) {
      expect(rand1()).toBe(rand2());
    }
  });

  test('LCG 应生成 [0, 1) 范围内的数', () => {
    const rand = lcg(12345);
    
    for (let i = 0; i < 100; i++) {
      const val = rand();
      expect(val).toBeGreaterThanOrEqual(0);
      expect(val).toBeLessThan(1);
    }
  });
});
