// ===========================================================================
// JSON 列拆表重构 —— 全面回归测试 (Module A/B/C/D)
//
// 运行：node --test test/json_split.test.js
// 依赖：Node 22+ 内置 node:sqlite（D1 替身）、编译后的 lib（server/.tmp-qa/compiled）
// 框架：node:test + node:assert（与现有测试约定一致，零新增依赖）
//
// D1 替身由 ./helpers/d1_adapter.cjs 提供：把 node:sqlite 包成 env.DB，
// 形态对齐 Cloudflare D1（prepare().bind().run()/.all()/.first()/batch()）。
// ===========================================================================

const { describe, it, beforeEach, afterEach } = require('node:test');
const assert = require('node:assert/strict');

const { createTestEnv } = require('./helpers/d1_adapter.cjs');
const { writeAuditChanges, reassembleAuditSnapshots } = require('../.tmp-qa/compiled/lib/audit.js');
const {
  writeLevelTiles,
  readLevelTilesByLevels,
  assembleLayoutData,
  parseLayoutData,
} = require('../.tmp-qa/compiled/lib/level-tiles.js');
const { backfillLevels, backfillBackups, backfillAudit } = require('../.tmp-qa/compiled/lib/migrate.js');

// ---------------------------------------------------------------------------
// 测试环境：每个用例独立内存库，互不影响
// ---------------------------------------------------------------------------
let env;
beforeEach(() => {
  env = createTestEnv();
});
afterEach(() => {
  env.close();
});

// ---------------------------------------------------------------------------
// 通用辅助
// ---------------------------------------------------------------------------
async function countRows(table, col, val) {
  const r = await env.DB.prepare(`SELECT COUNT(*) as c FROM ${table} WHERE ${col} = ?`).bind(val).first();
  return r.c;
}

// 在 admin_audit_log 插入一行（兼容 NOT NULL 约束），返回自增 changeId
async function seedAudit({ before, after }) {
  const info = await env.DB.prepare(
    `INSERT INTO admin_audit_log (admin_id, admin_phone, admin_role, action, created_at, before_snapshot, after_snapshot)
     VALUES (?,?,?,?,?,?,?)`
  )
    .bind('admin1', '13900000000', 'super', 'TEST_ACTION', 1700000000000, before ?? null, after ?? null)
    .run();
  return info.meta.last_row_id;
}

// 在 levels 插入一行（兼容 created_at NOT NULL），可选写入 layout_data 旧列
async function seedLevel(levelId, layoutData) {
  await env.DB.prepare('INSERT INTO levels (level_id, created_at, layout_data) VALUES (?,?,?)')
    .bind(levelId, 1700000000000, layoutData ?? null)
    .run();
}

// 在 backup_save_log 插入一行，save_data 为旧列 JSON 文本
async function seedBackup(id, saveData, points) {
  await env.DB.prepare('INSERT INTO backup_save_log (id, user_id, created_at, save_data, points) VALUES (?,?,?,?,?)')
    .bind(id, 'u' + id, 1700000000000, saveData ?? null, points ?? null)
    .run();
}

// 复刻前端 AuditLogs.tsx 的 safeParse（契约校验用，与源码逐字一致）
function safeParse(s) {
  if (!s) return null;
  try {
    return JSON.parse(s);
  } catch {
    return s;
  }
}

// ===========================================================================
// A. audit.ts —— 审计快照拆表 / 重组
// ===========================================================================
describe('A. audit.ts 审计快照拆表/重组', () => {
  it('A1. 写多字段(基本类型/数组/嵌套/值null/字段缺失)后重组深度相等', async () => {
    const changeId = await seedAudit({ before: null, after: null });
    const before = {
      name: 'old',
      count: 1,
      flag: true,
      nested: { a: [1, 2], b: { c: 'x' } },
      arr: [1, 'x', null],
      missing: null,
    };
    const after = {
      name: 'new',
      count: 2,
      flag: false,
      nested: { a: [1, 3], b: { c: 'y' } },
      arr: [2, null, 'y'],
      extra: 'added',
    };
    await writeAuditChanges(env, changeId, before, after);

    const map = await reassembleAuditSnapshots(env, [changeId]);
    const snap = map.get(changeId);
    assert.ok(snap, '应返回该 changeId 的快照');
    assert.equal(typeof snap.before, 'string');
    assert.equal(typeof snap.after, 'string');

    const b = JSON.parse(snap.before);
    const a = JSON.parse(snap.after);
    assert.deepStrictEqual(b, before, 'before 重组后应与原始对象深度相等');
    assert.deepStrictEqual(a, after, 'after 重组后应与原始对象深度相等');
    // 显式校验「值为 null」被正确还原为 null（而非缺失/字符串）
    assert.strictEqual(b.missing, null);
    assert.strictEqual(a.arr[1], null);
  });

  it('A2. SQL NULL 与 "null" 字符串严格可区分', async () => {
    const changeId = await seedAudit({ before: null, after: null });
    const before = { shared: 42, onlyBefore: 1, nullVal: null, strNull: 'null' };
    const after = { shared: 42, onlyAfter: 2, nullVal: null, strNull: 'null' };
    await writeAuditChanges(env, changeId, before, after);

    // —— 落库层面验证编码区分 ——
    const rowOnlyAfter = await env.DB.prepare(
      `SELECT old_val, new_val FROM admin_audit_changes WHERE change_id=? AND field='onlyAfter'`
    ).bind(changeId).first();
    assert.strictEqual(rowOnlyAfter.old_val, null, '字段仅存在于 after：old_val 应为 SQL NULL（表示 before 无此字段）');
    assert.strictEqual(rowOnlyAfter.new_val, '2');

    const rowNullVal = await env.DB.prepare(
      `SELECT old_val, new_val FROM admin_audit_changes WHERE change_id=? AND field='nullVal'`
    ).bind(changeId).first();
    assert.strictEqual(rowNullVal.old_val, 'null', '值为 null：应以 "null" 文本落库，区别于 SQL NULL');
    assert.strictEqual(rowNullVal.new_val, 'null');

    const rowStrNull = await env.DB.prepare(
      `SELECT old_val, new_val FROM admin_audit_changes WHERE change_id=? AND field='strNull'`
    ).bind(changeId).first();
    assert.strictEqual(rowStrNull.old_val, '"null"', '字面量字符串 "null" 应被 JSON.stringify 包成 "\\"null\\""');

    // —— 重组层面验证语义区分 ——
    const map = await reassembleAuditSnapshots(env, [changeId]);
    const b = JSON.parse(map.get(changeId).before);
    const a = JSON.parse(map.get(changeId).after);

    assert.strictEqual(b.shared, 42);
    assert.strictEqual(b.onlyBefore, 1);
    assert.strictEqual('onlyAfter' in b, false, 'before 对象不应含 onlyAfter（SQL NULL = 字段不存在）');
    assert.strictEqual(a.onlyAfter, 2);
    assert.strictEqual('onlyBefore' in a, false, 'after 对象不应含 onlyBefore');

    assert.strictEqual(b.nullVal, null, 'null 值重组后仍为 null');
    assert.strictEqual(a.nullVal, null);
    assert.strictEqual(b.strNull, 'null', '字面量字符串 "null" 重组后仍为字符串，不与 null 混淆');
    assert.strictEqual(a.strNull, 'null');
  });

  it('A3. writeAuditChanges 经 batch 一次性写入全部字段行', async () => {
    const changeId = await seedAudit({ before: null, after: null });
    await writeAuditChanges(env, changeId, { a: 1, b: 2, c: 3 }, { a: 1, b: 2, c: 3, d: 4 });
    const n = await countRows('admin_audit_changes', 'change_id', changeId);
    assert.equal(n, 4, '字段并集 {a,b,c,d} 应写入 4 行');
  });

  it('A3b. D1 适配层 batch：全部语句执行并汇总；单条失败向上传播', async () => {
    const st1 = env.DB.prepare('INSERT INTO levels (level_id, created_at) VALUES (?,?)').bind(9001, 1);
    const st2 = env.DB.prepare('INSERT INTO levels (level_id, created_at) VALUES (?,?)').bind(9002, 1);
    const res = await env.DB.batch([st1, st2]);
    assert.equal(res.results.length, 2);
    assert.equal(res.results[0].success, true);
    const c = await env.DB.prepare('SELECT COUNT(*) as c FROM levels WHERE level_id IN (9001,9002)').bind().first();
    assert.equal(c.c, 2);

    const bad = env.DB.prepare('INSERT INTO no_such_table (x) VALUES (1)').bind();
    const good = env.DB.prepare('INSERT INTO levels (level_id, created_at) VALUES (?,?)').bind(9003, 1);
    await assert.rejects(() => env.DB.batch([good, bad]), '含非法语句的 batch 应抛出错误（失败可见）');
  });

  it('A4. reassembleAuditSnapshots 仅返回有 KV 子表记录的 change_id，且值为字符串', async () => {
    const withKV = await seedAudit({ before: null, after: null });
    const withoutKV = await seedAudit({ before: null, after: null });
    await writeAuditChanges(env, withKV, { x: 1 }, { x: 2 });
    const map = await reassembleAuditSnapshots(env, [withKV, withoutKV]);
    assert.ok(map.has(withKV), '有 KV 记录的 change_id 应在 Map 中');
    assert.ok(!map.has(withoutKV), '无 KV 记录的 change_id 不应在 Map 中（调用方应回退 deprecated 列）');
    const snap = map.get(withKV);
    assert.equal(typeof snap.before, 'string');
    assert.equal(typeof snap.after, 'string');
  });
});

// ===========================================================================
// B. level-tiles.ts —— 关卡 tile 拆表 / 重组
// ===========================================================================
describe('B. level-tiles.ts 关卡 tile 拆表/重组', () => {
  const sampleTiles = (n) =>
    Array.from({ length: n }, (_, i) => ({
      id: 't' + i,
      x: i,
      y: i,
      z: 0,
      type: (i % 5) + 1,
      isBlind: i % 2 === 0,
      sealedCount: i,
      sealUnlockThreshold: 3, // 固定阈值，避免 undefined 在 JSON 序列化中被丢弃
    }));

  it('B1. parseLayoutData ↔ assembleLayoutData 互转幂等', () => {
    const tiles = sampleTiles(6);
    const round = parseLayoutData(assembleLayoutData(tiles));
    assert.deepStrictEqual(round, tiles, 'TileData[] → JSON → TileData[] 应完全一致');

    assert.deepStrictEqual(parseLayoutData(assembleLayoutData([])), [], '空数组应幂等');
    assert.deepStrictEqual(parseLayoutData(assembleLayoutData(undefined)), [], 'undefined 应降级为 []');
    assert.deepStrictEqual(parseLayoutData([]), [], '已为数组时原样返回');
  });

  it('B1b. parseLayoutData 对非法输入抛错', () => {
    // 非字符串/非数组：命中自定义错误
    assert.throws(() => parseLayoutData(123), /layout_data 格式非法/);
    // 非法 JSON 字符串：JSON.parse 抛 SyntaxError，由调用方 catch 后返回 400（功能等价）
    assert.throws(() => parseLayoutData('not json{'));
  });

  it('B2. writeLevelTiles 先删后插：连写两次同 level 行数= tile 数（无重复）', async () => {
    await seedLevel(10);
    const tiles = sampleTiles(5);
    await writeLevelTiles(env, 10, tiles);
    await writeLevelTiles(env, 10, tiles); // 第二次应幂等
    const n = await countRows('level_tiles', 'level_id', 10);
    assert.equal(n, 5, '连写两次后子表行数应等于 tile 数，无重复');
    // 校验 tile_index 连续 0..4
    const idxs = await env.DB.prepare('SELECT tile_index FROM level_tiles WHERE level_id=? ORDER BY tile_index').bind(10).all();
    assert.deepStrictEqual(idxs.results.map((r) => r.tile_index), [0, 1, 2, 3, 4]);
  });

  it('B2b. writeLevelTiles 空 tiles 应清空（删后不插）', async () => {
    await seedLevel(11);
    await writeLevelTiles(env, 11, sampleTiles(3));
    await writeLevelTiles(env, 11, []);
    const n = await countRows('level_tiles', 'level_id', 11);
    assert.equal(n, 0, '写入空数组后应删除该 level 全部 tile');
  });

  it('B3. readLevelTilesByLevels 批量读取多 level 并按 tile_index 升序聚合', async () => {
    await seedLevel(20);
    await seedLevel(21);
    const tilesA = sampleTiles(3);
    const tilesB = sampleTiles(2);
    await writeLevelTiles(env, 20, tilesA);
    await writeLevelTiles(env, 21, tilesB);

    const map = await readLevelTilesByLevels(env, [20, 21]);
    assert.equal(map.size, 2);
    assert.equal(map.get(20).length, 3);
    assert.equal(map.get(21).length, 2);

    // 字段映射正确性（is_blind 数字→布尔；sealUnlockThreshold 透传）
    const first = map.get(20)[0];
    assert.strictEqual(first.id, 't0');
    assert.strictEqual(first.isBlind, true);
    assert.strictEqual(first.sealUnlockThreshold, 3);

    // 顺序：按 tile_index 升序
    assert.deepStrictEqual(map.get(20).map((t) => t.id), ['t0', 't1', 't2']);

    // 整体往返一致
    assert.deepStrictEqual(map.get(20), tilesA);
    assert.deepStrictEqual(map.get(21), tilesB);
  });

  it('B3b. readLevelTilesByLevels 对 sealUnlockThreshold=NULL 还原为 undefined 不串字段', async () => {
    await seedLevel(22);
    const tiles = [{ id: 'z', x: 0, y: 0, z: 0, type: 1, isBlind: false, sealedCount: 0, sealUnlockThreshold: null }];
    await writeLevelTiles(env, 22, tiles);
    const map = await readLevelTilesByLevels(env, [22]);
    assert.strictEqual(map.get(22)[0].sealUnlockThreshold, undefined, 'NULL 阈值应还原为 undefined');
    assert.strictEqual(map.get(22)[0].isBlind, false);
  });
});

// ===========================================================================
// C. migrate.ts —— 存量回填幂等（旧 JSON 列 → 子表）
// ===========================================================================
describe('C. migrate.ts 存量回填幂等', () => {
  function seedLegacyData() {
    // levels：level 100 含 3 tile 的 layout_data；level 101 无 layout_data
    const layout100 = JSON.stringify([
      { id: 'a', x: 0, y: 0, z: 0, type: 1, isBlind: true, sealedCount: 2, sealUnlockThreshold: 3 },
      { id: 'b', x: 1, y: 1, z: 0, type: 2, isBlind: false, sealedCount: 1, sealUnlockThreshold: 3 },
      { id: 'c', x: 2, y: 2, z: 1, type: 3, isBlind: true, sealedCount: 0, sealUnlockThreshold: 3 },
    ]);
    return (async () => {
      await seedLevel(100, layout100);
      await seedLevel(101, null);

      // backup 200：含 unlocked/items/points；backup 201：空数组且 points 列 NULL（验证 UPDATE 提升）
      await seedBackup(
        200,
        JSON.stringify({ unlocked_levels: [5, 6], items: [{ item_type: 'UNDO', count: 3 }, { item_type: 'SHUFFLE', count: 1 }], points: 120 }),
        120
      );
      await seedBackup(
        201,
        JSON.stringify({ unlocked_levels: [], items: [], points: 50 }),
        null // points 列为 NULL，应由 save_data.points 回填
      );

      // audit 300：before/after 均有；301：仅 after（CREATE 类）；302：两者皆 NULL（应跳过）
      await seedAudit({ before: JSON.stringify({ title: 'old' }), after: JSON.stringify({ title: 'new' }) });
      await seedAudit({ before: null, after: JSON.stringify({ created: true }) });
      await seedAudit({ before: null, after: null });
    })();
  }

  it('C1. backfillLevels：从 layout_data 回填 level_tiles 且字段正确', async () => {
    await seedLegacyData();
    await backfillLevels(env);

    assert.equal(await countRows('level_tiles', 'level_id', 100), 3, 'level 100 应回填 3 个 tile');
    assert.equal(await countRows('level_tiles', 'level_id', 101), 0, 'level 101 无 layout_data 应跳过');

    const t0 = await env.DB.prepare('SELECT * FROM level_tiles WHERE level_id=? AND tile_index=0').bind(100).first();
    assert.strictEqual(t0.tile_id, 'a');
    assert.strictEqual(t0.is_blind, 1, 'isBlind=true 应映射为 1');
    assert.strictEqual(t0.seal_unlock_threshold, 3);
    const t1 = await env.DB.prepare('SELECT * FROM level_tiles WHERE level_id=? AND tile_index=1').bind(100).first();
    assert.strictEqual(t1.is_blind, 0, 'isBlind=false 应映射为 0');
  });

  it('C2. backfillBackups：unlocked/items 拆表 + points 提升', async () => {
    await seedLegacyData();
    await backfillBackups(env);

    assert.equal(await countRows('backup_unlocked_levels', 'backup_id', 200), 2, 'backup 200 应拆 2 个解锁关卡');
    assert.equal(await countRows('backup_unlocked_levels', 'backup_id', 201), 0, '空数组不应产生行');
    assert.equal(await countRows('backup_save_items', 'backup_id', 200), 2, 'backup 200 应拆 2 个道具');
    assert.equal(await countRows('backup_save_items', 'backup_id', 201), 0);

    const item = await env.DB.prepare("SELECT * FROM backup_save_items WHERE backup_id=? AND item_type='UNDO'").bind(200).first();
    assert.strictEqual(item.count, 3);

    // points 提升：backup 200 原 120（非 NULL，不被覆盖）；backup 201 原 NULL → 50
    const p200 = await env.DB.prepare('SELECT points FROM backup_save_log WHERE id=?').bind(200).first();
    const p201 = await env.DB.prepare('SELECT points FROM backup_save_log WHERE id=?').bind(201).first();
    assert.strictEqual(p200.points, 120);
    assert.strictEqual(p201.points, 50, 'points 列为 NULL 时应由 save_data.points 提升');
  });

  it('C3. backfillAudit：重组写入 admin_audit_changes，且 CREATE 类 before 重组为 "{}"', async () => {
    await seedLegacyData();
    await backfillAudit(env);

    // 找到三个 audit 的 id（按插入顺序 300,301,302）
    const ids = await env.DB.prepare('SELECT id FROM admin_audit_log ORDER BY id').bind().all();
    const [id300, id301, id302] = ids.results.map((r) => r.id);

    assert.equal(await countRows('admin_audit_changes', 'change_id', id300), 1, 'audit 300 单字段 title');
    assert.equal(await countRows('admin_audit_changes', 'change_id', id301), 1, 'audit 301（仅 after）单字段');
    assert.equal(await countRows('admin_audit_changes', 'change_id', id302), 0, 'audit 302（两者皆 NULL）应被跳过');

    const map = await reassembleAuditSnapshots(env, [id300, id301]);
    const b300 = JSON.parse(map.get(id300).before);
    const a300 = JSON.parse(map.get(id300).after);
    assert.deepStrictEqual(b300, { title: 'old' });
    assert.deepStrictEqual(a300, { title: 'new' });

    // CREATE 类（before 完全缺失）→ 重组为 "{}" 而非 null
    const a301 = JSON.parse(map.get(id301).after);
    assert.deepStrictEqual(a301, { created: true });
    assert.strictEqual(map.get(id301).before, '{}', '工程师报告的偏差：CREATE 类 before 重组为 "{}"');
  });

  it('C4. 回填幂等：再运行一次三个 backfill，子表行数不变（不重复插入）', async () => {
    await seedLegacyData();
    await backfillLevels(env);
    await backfillBackups(env);
    await backfillAudit(env);

    const before = {
      lt100: await countRows('level_tiles', 'level_id', 100),
      ul200: await countRows('backup_unlocked_levels', 'backup_id', 200),
      bi200: await countRows('backup_save_items', 'backup_id', 200),
      audit: await env.DB.prepare('SELECT COUNT(*) as c FROM admin_audit_changes').bind().first(),
    };

    // 再次运行
    await backfillLevels(env);
    await backfillBackups(env);
    await backfillAudit(env);

    assert.equal(await countRows('level_tiles', 'level_id', 100), before.lt100, 'level_tiles 不应重复');
    assert.equal(await countRows('backup_unlocked_levels', 'backup_id', 200), before.ul200, 'unlocked 不应重复');
    assert.equal(await countRows('backup_save_items', 'backup_id', 200), before.bi200, 'items 不应重复');
    const after = await env.DB.prepare('SELECT COUNT(*) as c FROM admin_audit_changes').bind().first();
    assert.equal(after.c, before.audit.c, 'audit_changes 不应重复');
  });

  it('C5. D1 语法兼容：backfill 在 node:sqlite(json_each/json_extract) 下真实执行成功', async () => {
    await seedLegacyData();
    // 若 json_each/json_extract 不兼容，backfill 会抛错（被内部 try/catch 吞掉并 console.error），
    // 此时子表无行，下面断言会失败 —— 即「不兼容则记录」。
    await backfillLevels(env);
    await backfillBackups(env);
    await backfillAudit(env);
    assert.ok((await countRows('level_tiles', 'level_id', 100)) === 3, 'json_each/json_extract 在 node:sqlite 下可用');
  });
});

// ===========================================================================
// D. handler 契约 / 前端兼容（前端零改动验证）
// ===========================================================================
describe('D. handler 契约 / 前端兼容', () => {
  it('D1. AuditLogs.safeParse 对 "{}" 与 null 均健壮，不报错', () => {
    assert.strictEqual(safeParse(null), null, 'null → null');
    assert.deepStrictEqual(safeParse('{}'), {}, '工程师报告的偏差 "{}" → 空对象（非 null），但不抛错');
    assert.deepStrictEqual(safeParse('{"x":1}'), { x: 1 });
    assert.strictEqual(safeParse('not json'), 'not json', '非法 JSON 兜底为原字符串，不抛错');
    // 模拟前端详情渲染：before="{}" 时整段 JSON.stringify 不抛错
    const detail = {
      before: safeParse('{}'),
      after: safeParse('{"a":1}'),
    };
    const rendered = JSON.stringify(detail);
    assert.ok(rendered.includes('"before":{}'), '渲染时 before 显示为 {}（前端无感知差异，展示正常）');
  });

  it('D1b. 真实 reassemble 输出经 safeParse 不抛错（含 CREATE 类 before="{}"）', async () => {
    const changeId = await seedAudit({ before: null, after: null });
    await writeAuditChanges(env, changeId, undefined, { created: true, name: 'lv' });
    const map = await reassembleAuditSnapshots(env, [changeId]);
    const snap = map.get(changeId);
    assert.strictEqual(snap.before, '{}');
    // 直接使用前端 safeParse 解析，断言不抛错且结果合理
    assert.doesNotThrow(() => safeParse(snap.before));
    assert.deepStrictEqual(safeParse(snap.before), {});
    assert.deepStrictEqual(safeParse(snap.after), { created: true, name: 'lv' });
  });

  it('D2. 关卡 GET 重组的 layout_data 为合法 JSON 字符串（admin.ts:965-970 契约）', async () => {
    await seedLevel(50);
    const tiles = [
      { id: 'p', x: 0, y: 0, z: 0, type: 1, isBlind: true, sealedCount: 1, sealUnlockThreshold: 3 },
      { id: 'q', x: 1, y: 1, z: 0, type: 2, isBlind: false, sealedCount: 0, sealUnlockThreshold: 3 },
    ];
    await writeLevelTiles(env, 50, tiles);

    // 复刻 admin.ts 关卡 GET 的重组逻辑
    const tileMap = await readLevelTilesByLevels(env, [50]);
    const layoutStr = assembleLayoutData(tileMap.get(50));
    assert.equal(typeof layoutStr, 'string', 'layout_data 必须是字符串契约');
    const parsed = JSON.parse(layoutStr); // 前端收到后解析
    assert.ok(Array.isArray(parsed));
    assert.deepStrictEqual(parsed, tiles, 'layout_data 还原后与原始 tiles 一致');
    assert.deepStrictEqual(parseLayoutData(layoutStr), tiles);
  });

  it('D2b. 无 tile 的关卡 layout_data 返回 "[]"（合法 JSON）', async () => {
    await seedLevel(51);
    const tileMap = await readLevelTilesByLevels(env, [51]);
    const layoutStr = assembleLayoutData(tileMap.get(51)); // undefined → []
    assert.equal(layoutStr, '[]');
    assert.deepStrictEqual(JSON.parse(layoutStr), []);
  });
});
