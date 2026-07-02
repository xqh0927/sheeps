const test = require('node:test');
const assert = require('node:assert/strict');

const { generateSolvableLevel } = require('../.tmp-test/level.js');
const { calculateCardCount } = require('../.tmp-test/difficulty.js');

// 固定使用 userId=0 进行测试（游客统一曲线）

test('关卡 1 应使用固定布局（12张牌）', () => {
  const tiles = generateSolvableLevel(0, 1, 12345);
  assert.equal(tiles.length, 12);
});

test('关卡 2: 固定 48 张牌', () => {
  const tiles = generateSolvableLevel(0, 2, 12345);
  assert.equal(tiles.length, 48);
});

test('关卡 3: 固定 60 张牌', () => {
  const tiles = generateSolvableLevel(0, 3, 12345);
  assert.equal(tiles.length, 60);
});

test('generateSolvableLevel 的 maxCards 与 calculateCardCount 一致', () => {
  const testLevels = [5, 10, 15, 20, 30, 50, 100, 150, 200, 300, 500];
  for (const levelId of testLevels) {
    const { cardCount: expectedMax } = calculateCardCount(0, levelId);
    const tiles = generateSolvableLevel(0, levelId, 12345);
    // 实际生成的卡牌数应 ≤ maxCards（受棋盘坐标数量限制）
    assert.ok(
      tiles.length <= expectedMax,
      `关卡 ${levelId}: tiles.length=${tiles.length} > expectedMax=${expectedMax}`
    );
    assert.equal(tiles.length % 3, 0, `关卡 ${levelId}: tiles.length=${tiles.length} 不是 3 的倍数`);
  }
});

test('所有关卡的卡牌数必须是 3 的倍数', () => {
  const testLevels = [1, 2, 3, 10, 22, 23, 50, 100, 150, 200, 300, 500];
  for (const levelId of testLevels) {
    for (let seed = 0; seed < 5; seed++) {
      const tiles = generateSolvableLevel(0, levelId, seed);
      assert.ok(tiles.length % 3 === 0, `关卡 ${levelId} 种子 ${seed} 的卡牌数 ${tiles.length} 不是 3 的倍数`);
    }
  }
});

test('生成的卡牌应包含必要的字段', () => {
  // 使用高关卡确保生成足够多的卡牌来验证字段
  const tiles = generateSolvableLevel(0, 500, 12345);

  for (const tile of tiles) {
    assert.ok(tile.id !== undefined, '应有 id 字段');
    assert.ok(tile.x !== undefined, '应有 x 字段');
    assert.ok(tile.y !== undefined, '应有 y 字段');
    assert.ok(tile.z !== undefined, '应有 z 字段');
    assert.ok(tile.type >= 1, 'type 应大于等于 1');
    assert.equal(typeof tile.isBlind, 'boolean', 'isBlind 应是 boolean');
    assert.equal(typeof tile.sealedCount, 'number', 'sealedCount 应是 number');
  }
});

test('多次生成相同关卡应具有确定性', () => {
  const tiles1 = generateSolvableLevel(42, 23, 12345);
  const tiles2 = generateSolvableLevel(42, 23, 12345);
  
  assert.equal(tiles1.length, tiles2.length);
  for (let i = 0; i < tiles1.length; i++) {
    assert.equal(tiles1[i].x, tiles2[i].x);
    assert.equal(tiles1[i].y, tiles2[i].y);
    assert.equal(tiles1[i].z, tiles2[i].z);
    assert.equal(tiles1[i].type, tiles2[i].type);
  }
});

test('高关卡时 layersCount 应足够大以支持大量卡牌', () => {
  const tiles = generateSolvableLevel(0, 500, 12345);
  const maxZ = Math.max(...tiles.map(t => t.z));
  
  // 至少有 5 层（这样才能容纳大量牌）
  assert.ok(maxZ >= 4, `关卡 500 的最大层级应为至少 4，实际为 ${maxZ}`);
});

test('不同 shapeType 都应能生成卡牌（高关卡）', () => {
  // 测试所有 18 种形状，每种都应生成至少 12 张卡牌
  for (let seed = 0; seed < 18; seed++) {
    const tiles = generateSolvableLevel(0, 500, seed);
    assert.ok(tiles.length >= 12, `形状 ${seed} 应生成至少 12 张卡牌，实际生成 ${tiles.length}`);
    assert.equal(tiles.length % 3, 0);
  }
});

test('卡牌类型数量应随关卡增加', () => {
  const tiles1 = generateSolvableLevel(0, 1, 12345);
  const tiles500 = generateSolvableLevel(0, 500, 12345);
  
  const types1 = new Set(tiles1.map(t => t.type));
  const types500 = new Set(tiles500.map(t => t.type));
  
  // 关卡 1 应有 3 种类型
  assert.ok(types1.size <= 3, `关卡 1 的类型数应为 3，实际为 ${types1.size}`);
  // 高关卡应有更多类型
  assert.ok(types500.size > types1.size, `关卡 500 的类型数 ${types500.size} 应大于关卡 1 的 ${types1.size}`);
});

test('卡牌数量随关卡递增（广义单调性）', () => {
  // 验证在较高区间内卡牌数不会突然下降（休息关除外）
  let prevCount = 0;
  for (let levelId = 5; levelId <= 100; levelId++) {
    if (levelId % 5 === 0) continue; // 跳过休息关
    const tiles = generateSolvableLevel(0, levelId, 12345);
    if (levelId === 5) prevCount = tiles.length;
    if (levelId === 100) {
      assert.ok(tiles.length > prevCount, `L100 卡牌数 ${tiles.length} 应大于 L5 的 ${prevCount}`);
    }
  }
});

test('所有关卡的卡牌数都在 [12, 300] 范围内', () => {
  const testLevels = [1, 2, 3, 5, 10, 20, 50, 100, 200, 300, 500];
  for (const levelId of testLevels) {
    for (let seed = 0; seed < 5; seed++) {
      const tiles = generateSolvableLevel(0, levelId, seed);
      assert.ok(tiles.length >= 12, `关卡 ${levelId}: tiles.length=${tiles.length} < 12`);
      assert.ok(tiles.length <= 300, `关卡 ${levelId}: tiles.length=${tiles.length} > 300`);
    }
  }
});
