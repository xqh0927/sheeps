/*
 * QA 验收测试：4 项后台管理功能（方案 B 全量归一化 i18n）
 * 驱动 server/src 下【真实实现】，以 mock 的 D1 / KV / HTTP 环境验证核心逻辑。
 * 不使用部署；纯逻辑断言（符合任务书"用 Node 独立脚本 + 断言"的降级方案）。
 *
 * 运行：node --test test/qa_features.test.js
 */
const test = require('node:test');
const assert = require('node:assert/strict');

const {
  getI18nBatch, getI18n, resolveI18n,
} = require('../.tmp-qa/i18n.js');
const { getGameModeStatus } = require('../.tmp-qa/helpers.js');
const { getDatabaseAppUpdate } = require('../.tmp-qa/update.js');
const { handleGameRoutes } = require('../.tmp-qa/handlers/game.js');
const { handleAdminRoutes } = require('../.tmp-qa/handlers/admin.js');
const { generateJWT } = require('../.tmp-qa/crypto.js');

// ============ 测试环境工厂 ============

function buildEnv(overrides = {}) {
  const state = {
    config: {
      gamemode_stage: 'on',
      gamemode_endless: 'off',
      i18n_dual_write: 'on',
    },
    i18n_strings: [],
    app_version: [],
    users: [
      { id: 'u_1001', username: 'alice', phone: '13800001001', is_banned: 0 },
      { id: 'u_1002', username: 'bob', phone: '13900002002', is_banned: 0 },
    ],
    leaderboard: [],
    ...(overrides.state || {}),
  };

  const writes = []; // 捕获所有写操作 {sql, args}
  let lastRowId = 0;

  function resolveData(sql, args) {
    const s = sql.toLowerCase();
    // config 读
    if (s.includes('from config') && s.includes('value')) {
      const key = args[0];
      return state.config[key] !== undefined ? { value: state.config[key] } : null;
    }
    // i18n 批量读
    if (s.includes('from i18n_strings') && s.includes('str_key')) {
      const module = args[0], locale = args[1];
      return state.i18n_strings
        .filter((r) => r.module === module && r.locale === locale)
        .map((r) => ({ str_key: r.str_key, value: r.value }));
    }
    // app 更新读（status = 1 过滤）
    if (s.includes('from app_version') && s.includes('status = 1')) {
      const cur = args[0];
      return state.app_version
        .filter((r) => r.version_code > cur && r.status === 1)
        .sort((a, b) => b.version_code - a.version_code)
        .map((r) => ({
          version_code: r.version_code, version_name: r.version_name,
          apk_url: r.apk_url, download_url: r.download_url,
          update_log: r.update_log, status: r.status, is_force_update: r.is_force_update,
        }));
    }
    // requireAdmin 封禁检查
    if (s.includes('is_banned from users')) {
      return { is_banned: 0 };
    }
    // leaderboard 用户存在性检查（createLeaderboardRow）
    if (s.includes('id from users where id')) {
      const id = args[0];
      return state.users.find((u) => u.id === id) ? { id } : null;
    }
    // searchUsers
    if (s.includes('id, username, phone from users')) {
      const kw = String(args[0]).replace(/%/g, '');
      return state.users
        .filter((u) => u.phone.includes(kw) || u.username.includes(kw))
        .map((u) => ({ id: u.id, username: u.username, phone: u.phone }));
    }
    // updateAppVersion 前后读取
    if (s.includes('from app_version where version_code')) {
      const code = args[0];
      const row = state.app_version.find((r) => r.version_code === code);
      return row ? { ...row } : null;
    }
    // updateI18n 前读取
    if (s.includes('from i18n_strings where id')) {
      const id = args[0];
      const row = state.i18n_strings.find((r) => String(r.id) === String(id));
      return row ? { str_key: row.str_key, module: row.module, locale: row.locale, value: row.value } : null;
    }
    // leaderboard 计数
    if (s.includes('from leaderboard') && s.includes('count')) {
      return { c: state.leaderboard.length };
    }
    // leaderboard 列表
    if (s.includes('from leaderboard l join users')) {
      return state.leaderboard.map((r) => ({
        id: r.id, user_id: r.user_id, username: 'u', level_id: r.level_id,
        score: r.score, clear_time_ms: r.clear_time_ms, game_mode: r.game_mode, achieved_at: r.achieved_at,
      }));
    }
    return null;
  }

  const DB = {
    prepare(sql) {
      return {
        bind(...args) {
          return {
            async first() {
              const d = resolveData(sql, args);
              if (Array.isArray(d)) return d[0] || null;
              return d;
            },
            async all() {
              const d = resolveData(sql, args);
              if (Array.isArray(d)) return { results: d };
              return { results: d ? [d] : [] };
            },
            async run() {
              writes.push({ sql, args });
              return { success: true, lastRowId: ++lastRowId };
            },
          };
        },
      };
    },
    async batch(statements) {
      const out = [];
      for (const st of statements) out.push(await st.run());
      return out;
    },
  };

  const kvMap = new Map();
  const SHEEPS_CACHE = {
    async get(k) { return kvMap.has(k) ? kvMap.get(k) : null; },
    async put(k, v) { kvMap.set(k, v); },
    async delete(k) { kvMap.delete(k); },
  };

  const env = {
    DB,
    SHEEPS_CACHE,
    AVATAR_BUCKET: { put: async () => ({}) },
    R2_PUBLIC_URL: 'https://file.example.com',
    JWT_SECRET: 'qa_test_secret',
    AES_KEY_HEX: 'a1b2c3d4e5f6a7b8b9c0d1e2f3a4b5c6d7e8f9a0b1c2d3e4f5a6b7c8d9e0f1a2',
    ADMIN_WEB_ORIGIN: '*',
  };

  return { env, state, writes };
}

// 生成管理员 token（与 verifyJWT 使用同一默认密钥）
async function adminToken(role = 'super') {
  return generateJWT({ userId: 'admin_1', phone: '13800000000', role, type: 'access', exp: Date.now() + 3600_000 });
}

function adminReq(method, path, body) {
  const token = null; // 在调用处注入
  return {
    method,
    headers: {
      get: (h) => (h.toLowerCase() === 'authorization' ? `Bearer ${token}` : null),
    },
    json: async () => body,
    url: `https://example.com${path}`,
    _token: token,
  };
}

// 解析 INSERT 列名
function insertCols(sql) {
  const m = /INSERT INTO \w+ \(([^)]+)\) VALUES/i.exec(sql);
  if (!m) return [];
  return m[1].split(',').map((c) => c.trim().replace(/"/g, ''));
}
// 解析 UPDATE SET 列名
function updateCols(sql) {
  const m = /UPDATE \w+ SET ([^]+?) WHERE/i.exec(sql);
  if (!m) return [];
  return m[1].split(',').map((c) => c.split('=')[0].trim().replace(/"/g, ''));
}

// ============ 1. getGameModeStatus ============

test('getGameModeStatus: 默认 stage=on / endless=off / battle=off', async () => {
  const { env } = buildEnv();
  const s = await getGameModeStatus(env);
  assert.deepEqual(s, { stage: true, endless: false, battle: false });
});

test('getGameModeStatus: 覆盖 endless=on', async () => {
  const { env, state } = buildEnv();
  state.config.gamemode_endless = 'on';
  const s = await getGameModeStatus(env);
  assert.deepEqual(s, { stage: true, endless: true, battle: false });
});

test('getGameModeStatus: 覆盖 stage=off', async () => {
  const { env, state } = buildEnv();
  state.config.gamemode_stage = 'off';
  const s = await getGameModeStatus(env);
  assert.deepEqual(s, { stage: false, endless: false, battle: false });
});

// ============ 2. resolveI18n ============

test('resolveI18n: 命中返回 i18n 值', () => {
  const map = new Map([['shop_items.1.name', 'Apple']]);
  assert.equal(resolveI18n(map, 'shop_items.1.name', '苹果'), 'Apple');
});

test('resolveI18n: 缺失回退 fallback', () => {
  const map = new Map();
  assert.equal(resolveI18n(map, 'shop_items.1.name', '苹果'), '苹果');
});

test('resolveI18n: 空字符串值回退 fallback', () => {
  const map = new Map([['shop_items.1.name', '']]);
  assert.equal(resolveI18n(map, 'shop_items.1.name', '苹果'), '苹果');
});

test('resolveI18n: null 值回退 fallback', () => {
  const map = new Map([['shop_items.1.name', null]]);
  assert.equal(resolveI18n(map, 'shop_items.1.name', '苹果'), '苹果');
});

// ============ 3. getI18nBatch ============

test('getI18nBatch: zh 返回空 Map（走基列）', async () => {
  const { env, state } = buildEnv();
  state.i18n_strings.push({ str_key: 'shop_items.1.name', locale: 'zh', module: 'shop_items', value: '苹果' });
  const map = await getI18nBatch(env, 'shop_items', 'zh');
  assert.equal(map.size, 0);
});

test('getI18nBatch: en 返回命中 Map', async () => {
  const { env, state } = buildEnv();
  state.i18n_strings.push(
    { str_key: 'shop_items.1.name', locale: 'en', module: 'shop_items', value: 'Apple' },
    { str_key: 'shop_items.1.description', locale: 'en', module: 'shop_items', value: 'Desc' },
  );
  const map = await getI18nBatch(env, 'shop_items', 'en');
  assert.equal(map.get('shop_items.1.name'), 'Apple');
  assert.equal(map.get('shop_items.1.description'), 'Desc');
});

test('getI18nBatch: KV 缓存命中可直接返回（不查库）', async () => {
  const { env, state } = buildEnv();
  // 预置 KV 缓存，库为空 → 证明走缓存
  env.SHEEPS_CACHE.put('i18n_shop_items_en', JSON.stringify({ 'shop_items.9.name': 'CachedName' }));
  const map = await getI18nBatch(env, 'shop_items', 'en');
  assert.equal(map.get('shop_items.9.name'), 'CachedName');
  assert.equal(state.i18n_strings.length, 0); // 未查库
});

test('getI18nBatch: locale 后缀映射正确 (tw/ja/ko)', async () => {
  const { env, state } = buildEnv();
  state.i18n_strings.push(
    { str_key: 'task.t1.name', locale: 'tw', module: 'task', value: '繁體' },
    { str_key: 'task.t1.name', locale: 'ja', module: 'task', value: '日本語' },
    { str_key: 'task.t1.name', locale: 'ko', module: 'task', value: '한글' },
  );
  assert.equal((await getI18nBatch(env, 'task', 'tw')).get('task.t1.name'), '繁體');
  assert.equal((await getI18nBatch(env, 'task', 'ja')).get('task.t1.name'), '日本語');
  assert.equal((await getI18nBatch(env, 'task', 'ko')).get('task.t1.name'), '한글');
});

// ============ 4. getI18n ============

test('getI18n: zh 直接返回 fallback', async () => {
  const { env } = buildEnv();
  assert.equal(await getI18n(env, 'shop_items', 'shop_items.1.name', 'zh', '苹果'), '苹果');
});

test('getI18n: en 命中返回 i18n 值', async () => {
  const { env, state } = buildEnv();
  state.i18n_strings.push({ str_key: 'shop_items.1.name', locale: 'en', module: 'shop_items', value: 'Apple' });
  assert.equal(await getI18n(env, 'shop_items', 'shop_items.1.name', 'en', '苹果'), 'Apple');
});

test('getI18n: en 缺失回退 fallback', async () => {
  const { env } = buildEnv();
  assert.equal(await getI18n(env, 'shop_items', 'shop_items.1.name', 'en', '苹果'), '苹果');
});

// ============ 5. [REMOVED] dualWriteBaseColumn 宽列双写用例 ============
// 方案 B 已将多语言归一化到单表 i18n_strings，业务表宽列(name_en 等)已 DROP，
// src/i18n.ts 中的 dualWriteBaseColumn 已删除。下列旧双写用例引用已移除函数，
// 属陈旧测试，整体移除（不改动 src）。多语言真实落库验证见第 11 节。

// ============ 6. getDatabaseAppUpdate（status=1 过滤 + 下载链接 + i18n update_log）============

test('getDatabaseAppUpdate: 仅 status=1 参与；download_url 优先', async () => {
  const { env, state } = buildEnv();
  state.app_version.push(
    { version_code: 2, version_name: 'v2', apk_url: 'https://x/v2.apk', download_url: 'https://x/v2dl.apk', update_log: 'v2base', status: 1, is_force_update: 0 },
    { version_code: 3, version_name: 'v3', apk_url: 'https://x/v3.apk', download_url: null, update_log: 'v3', status: 0, is_force_update: 0 }, // 草稿不参与
  );
  const origFetch = globalThis.fetch;
  globalThis.fetch = async (url) => ({ status: url === 'https://x/v2dl.apk' ? 200 : 404 });
  try {
    const r = await getDatabaseAppUpdate(env, 1, 'zh');
    assert.equal(r.has_update, true);
    assert.equal(r.version_name, 'v2');
    assert.equal(r.apk_url, 'https://x/v2dl.apk'); // download_url 优先
    assert.equal(r.update_log, 'v2base'); // 无 i18n → 基列兜底
    assert.equal(r.force_update, false);
  } finally {
    globalThis.fetch = origFetch;
  }
});

test('getDatabaseAppUpdate: download_url 为空时回退 apk_url', async () => {
  const { env, state } = buildEnv();
  state.app_version.push(
    { version_code: 2, version_name: 'v2', apk_url: 'https://x/v2.apk', download_url: null, update_log: 'v2', status: 1, is_force_update: 1 },
  );
  const origFetch = globalThis.fetch;
  globalThis.fetch = async (url) => ({ status: url === 'https://x/v2.apk' ? 200 : 404 });
  try {
    const r = await getDatabaseAppUpdate(env, 1, 'zh');
    assert.equal(r.apk_url, 'https://x/v2.apk');
    assert.equal(r.force_update, true);
  } finally {
    globalThis.fetch = origFetch;
  }
});

test('getDatabaseAppUpdate: update_log 经 i18n 读取 (en)', async () => {
  const { env, state } = buildEnv();
  state.app_version.push(
    { version_code: 2, version_name: 'v2', apk_url: 'https://x/v2.apk', download_url: 'https://x/v2dl.apk', update_log: 'v2base', status: 1, is_force_update: 0 },
  );
  state.i18n_strings.push({ str_key: 'app_version.2.update_log', locale: 'en', module: 'app_version', value: 'v2 EN log' });
  const origFetch = globalThis.fetch;
  globalThis.fetch = async () => ({ status: 200 });
  try {
    const r = await getDatabaseAppUpdate(env, 1, 'en');
    assert.equal(r.update_log, 'v2 EN log');
  } finally {
    globalThis.fetch = origFetch;
  }
});

test('getDatabaseAppUpdate: 当前已是最新版本 → 无更新', async () => {
  const { env, state } = buildEnv();
  state.app_version.push({ version_code: 2, version_name: 'v2', apk_url: 'https://x/v2.apk', download_url: null, update_log: 'v2', status: 1, is_force_update: 0 });
  const r = await getDatabaseAppUpdate(env, 2, 'zh'); // currentCode=2，无更高版本
  assert.equal(r.has_update, false);
});

// ============ 7. 排行榜无尽网关（game.ts）============

test('game leaderboard 网关: game_mode=1 + endless=off → disabled 空榜', async () => {
  const { env } = buildEnv(); // 默认 endless=off
  const url = new URL('https://e.com/api/leaderboard?game_mode=1&level_id=1&type=history&page=1&limit=20');
  const req = { method: 'GET', headers: { get: () => null }, url: url.toString() };
  const res = await handleGameRoutes(req, env, url.pathname, url);
  const data = await res.json();
  assert.equal(data.success, true);
  assert.deepEqual(data.rankings, []);
  assert.equal(data.disabled, true);
});

test('game leaderboard 网关: game_mode=1 + endless=on → 返回真实榜', async () => {
  const { env, state } = buildEnv();
  state.config.gamemode_endless = 'on';
  state.leaderboard.push({ id: 1, user_id: 'u_1001', level_id: 0, score: 999, clear_time_ms: 100, game_mode: 1, achieved_at: Date.now() });
  const url = new URL('https://e.com/api/leaderboard?game_mode=1&level_id=0&type=history&page=1&limit=20');
  const req = { method: 'GET', headers: { get: () => null }, url: url.toString() };
  const res = await handleGameRoutes(req, env, url.pathname, url);
  const data = await res.json();
  assert.equal(data.disabled, undefined);
  assert.equal(data.rankings.length, 1);
  assert.equal(data.rankings[0].score, 999);
});

test('game leaderboard 网关: 关卡榜 game_mode=0 不受无尽开关影响', async () => {
  const { env, state } = buildEnv(); // endless=off
  state.leaderboard.push({ id: 2, user_id: 'u_1002', level_id: 5, score: 500, clear_time_ms: 200, game_mode: 0, achieved_at: Date.now() });
  const url = new URL('https://e.com/api/leaderboard?game_mode=0&level_id=5&type=history&page=1&limit=20');
  const req = { method: 'GET', headers: { get: () => null }, url: url.toString() };
  const res = await handleGameRoutes(req, env, url.pathname, url);
  const data = await res.json();
  assert.equal(data.disabled, undefined);
  assert.equal(data.rankings.length, 1);
});

test('game daily-popup 网关: game_mode=1 + endless=off → disabled', async () => {
  const { env } = buildEnv();
  const url = new URL('https://e.com/api/leaderboard/daily-popup?game_mode=1');
  const req = { method: 'GET', headers: { get: () => null }, url: url.toString() };
  const res = await handleGameRoutes(req, env, url.pathname, url);
  const data = await res.json();
  assert.equal(data.disabled, true);
  assert.deepEqual(data.top3, []);
});

// ============ 8. 管理端：App 版本 status→release_time ============

async function adminCall(env, method, path, body, role = 'super') {
  const token = await adminToken(role);
  const url = new URL(`https://example.com${path}`);
  const req = {
    method,
    headers: { get: (h) => (h.toLowerCase() === 'authorization' ? `Bearer ${token}` : null) },
    json: async () => body,
    url: url.toString(),
  };
  return handleAdminRoutes(req, env, url.pathname, url);
}

/** 等待 fire-and-forget 的 onCreated(seed) 微任务/宏任务完成（seed 不被 genericCreate await） */
function flush() {
  return new Promise((r) => setTimeout(r, 0));
}

test('admin createAppVersion: status=1 自动写 release_time', async () => {
  const { env, writes } = buildEnv();
  const res = await adminCall(env, 'POST', '/api/admin/app-versions', { version_code: 100, version_name: 'v100', status: 1 });
  assert.equal(res.status, 200);
  const ins = writes.find((w) => w.sql.includes('INSERT INTO app_version'));
  assert.ok(ins, '应有 app_version INSERT');
  const cols = insertCols(ins.sql);
  assert.ok(cols.includes('release_time'), `列应包含 release_time，实际: ${cols}`);
  const idx = cols.indexOf('release_time');
  assert.equal(typeof ins.args[idx], 'number');
});

test('admin createAppVersion: status=0 不写 release_time', async () => {
  const { env, writes } = buildEnv();
  await adminCall(env, 'POST', '/api/admin/app-versions', { version_code: 101, version_name: 'v101', status: 0 });
  const ins = writes.find((w) => w.sql.includes('INSERT INTO app_version'));
  const cols = insertCols(ins.sql);
  assert.ok(!cols.includes('release_time'), `status=0 不应含 release_time，实际: ${cols}`);
});

test('admin updateAppVersion: 0→1 自动写 release_time', async () => {
  const { env, state, writes } = buildEnv();
  state.app_version.push({ version_code: 2, version_name: 'v2', status: 0, release_time: null });
  const res = await adminCall(env, 'PUT', '/api/admin/app-versions/2', { status: 1 });
  assert.equal(res.status, 200);
  const upd = writes.find((w) => w.sql.includes('UPDATE app_version SET'));
  assert.ok(upd, '应有 app_version UPDATE');
  const cols = updateCols(upd.sql);
  assert.ok(cols.includes('release_time'), `SET 应包含 release_time，实际: ${cols}`);
});

test('admin updateAppVersion: 已发布(release_time 已存在) 不再覆盖 release_time', async () => {
  const { env, state, writes } = buildEnv();
  state.app_version.push({ version_code: 5, version_name: 'v5', status: 1, release_time: 123456 });
  await adminCall(env, 'PUT', '/api/admin/app-versions/5', { status: 1 });
  const upd = writes.find((w) => w.sql.includes('UPDATE app_version SET'));
  const cols = updateCols(upd.sql);
  assert.ok(!cols.includes('release_time'), `已发布不应重写 release_time，实际: ${cols}`);
});

// ============ 9. 管理端：排行榜列表网关（disabled）+ searchUsers ============

test('admin listLeaderboard: game_mode=1 + endless=off → disabled 空榜', async () => {
  const { env } = buildEnv();
  const res = await adminCall(env, 'GET', '/api/admin/leaderboard?game_mode=1');
  assert.equal(res.status, 200);
  const data = await res.json();
  assert.equal(data.success, true);
  assert.deepEqual(data.list, []);
  assert.equal(data.disabled, true);
});

test('admin searchUsers: 按手机号片段反查 id', async () => {
  const { env } = buildEnv();
  const res = await adminCall(env, 'GET', '/api/admin/users/search?keyword=1001');
  const data = await res.json();
  assert.equal(data.list.length, 1);
  assert.equal(data.list[0].id, 'u_1001');
});

test('admin searchUsers: 按昵称反查 id', async () => {
  const { env } = buildEnv();
  const res = await adminCall(env, 'GET', '/api/admin/users/search?keyword=bob');
  const data = await res.json();
  assert.equal(data.list[0].id, 'u_1002');
});

test('admin searchUsers: 支持 q 参数', async () => {
  const { env } = buildEnv();
  const res = await adminCall(env, 'GET', '/api/admin/users/search?q=alice');
  const data = await res.json();
  assert.equal(data.list[0].id, 'u_1001');
});

// ============ 10. 管理端：排行榜手动补录 + i18n 双写 ============

test('admin createLeaderboardRow: 校验用户存在并写入', async () => {
  const { env, writes } = buildEnv();
  const res = await adminCall(env, 'POST', '/api/admin/leaderboard', {
    user_id: 'u_1001', level_id: 1, score: 500, clear_time_ms: 1000, game_mode: 0,
  });
  assert.equal(res.status, 200);
  const ins = writes.find((w) => w.sql.includes('INSERT INTO leaderboard'));
  assert.ok(ins, '应写入 leaderboard');
  // 校验 user_id/level_id/score/clear_time_ms/game_mode/achieved_at
  assert.deepEqual(ins.args.slice(0, 5), ['u_1001', 1, 500, 1000, 0]);
  assert.equal(typeof ins.args[5], 'number'); // achieved_at
});

test('admin updateI18n: 仅更新 i18n_strings 的 value（方案 B 不再双写宽列）', async () => {
  const { env, state, writes } = buildEnv();
  state.i18n_strings.push({ id: 1, str_key: 'shop_items.1000.name', module: 'shop_items', locale: 'en', value: 'old' });
  const res = await adminCall(env, 'PUT', '/api/admin/i18n/1', { value: 'NewEN' });
  assert.equal(res.status, 200);
  // 方案 B：只改 i18n_strings，不再 UPDATE 业务宽列
  const dw = writes.find((w) => w.sql.includes('UPDATE shop_items'));
  assert.ok(!dw, '方案 B 不应再双写业务宽列');
  const upd = writes.find((w) => w.sql.includes('UPDATE i18n_strings SET'));
  assert.ok(upd, '应更新 i18n_strings');
  assert.match(upd.sql, /SET "value" = \?/);
  assert.equal(upd.args[0], 'NewEN');
  assert.equal(upd.args[upd.args.length - 1], '1');
});

test('admin createI18n: 写入 i18n_strings（方案 B 不再双写宽列）', async () => {
  const { env, writes } = buildEnv();
  const res = await adminCall(env, 'POST', '/api/admin/i18n', {
    str_key: 'shop_items.2000.description', module: 'shop_items', locale: 'tw', value: '描述',
  });
  assert.equal(res.status, 200);
  // 方案 B：只写 i18n_strings，不再 UPDATE 业务宽列
  const dw = writes.find((w) => w.sql.includes('UPDATE shop_items'));
  assert.ok(!dw, '方案 B 不应再双写业务宽列');
  const ins = writes.find((w) => w.sql.includes('INSERT INTO i18n_strings'));
  assert.ok(ins, '应写入 i18n_strings');
  const cols = insertCols(ins.sql);
  assert.ok(cols.includes('str_key') && cols.includes('locale') && cols.includes('value'),
    `列应包含 str_key/locale/value，实际: ${cols}`);
});

// ============ 11. 管理端：新建内容强制 5 语言 + 多语言真实落库（方案 B 增量）============

/** 过滤 i18n_strings 播种写（args = [str_key, locale, module, value, updated_at]） */
function i18nSeedWrites(writes) {
  return writes.filter((w) => w.sql.includes('INSERT OR IGNORE INTO i18n_strings'));
}
function i18nSeedMap(writes) {
  const m = {};
  for (const w of i18nSeedWrites(writes)) {
    const [strKey, locale, module, value] = w.args;
    m[`${strKey}.${locale}`] = value;
  }
  return m;
}
function hasSuffixCol(cols) {
  return cols.some((c) => c.endsWith('_en') || c.endsWith('_tw') || c.endsWith('_ja') || c.endsWith('_ko'));
}

test('admin createShopItem (5 语言): 多语言值真实落库到 i18n_strings（10 行）', async () => {
  const { env, writes } = buildEnv();
  const body = {
    name: '苹果', name_en: 'Apple', name_tw: '蘋果', name_ja: 'リンゴ', name_ko: '사과',
    description: '红红的果子', description_en: 'A red fruit', description_tw: '紅紅的果子', description_ja: '赤い果物', description_ko: '빨간 과일',
    item_type: 'PROP_FOOD', points_price: 100, stock: 50,
  };
  const res = await adminCall(env, 'POST', '/api/admin/shop-items', body);
  await flush();
  assert.equal(res.status, 200);

  const seeds = i18nSeedWrites(writes);
  assert.equal(seeds.length, 10, `2 字段 × 5 语言 = 10 行，实际 ${seeds.length}`);
  const m = i18nSeedMap(writes);
  assert.equal(m['shop_items.1.name.zh'], '苹果');
  assert.equal(m['shop_items.1.name.en'], 'Apple');
  assert.equal(m['shop_items.1.name.tw'], '蘋果');
  assert.equal(m['shop_items.1.name.ja'], 'リンゴ');
  assert.equal(m['shop_items.1.name.ko'], '사과');
  assert.equal(m['shop_items.1.description.zh'], '红红的果子');
  assert.equal(m['shop_items.1.description.en'], 'A red fruit');
  assert.equal(m['shop_items.1.description.tw'], '紅紅的果子');
  assert.equal(m['shop_items.1.description.ja'], '赤い果物');
  assert.equal(m['shop_items.1.description.ko'], '빨간 과일');

  // 业务表 INSERT 不应包含 locale 后缀列（宽列已 DROP），否则会引用不存在的列
  const ins = writes.find((w) => w.sql.includes('INSERT INTO shop_items'));
  assert.ok(ins, '应有 shop_items INSERT');
  const cols = insertCols(ins.sql);
  assert.ok(!hasSuffixCol(cols), `业务表 INSERT 不应含后缀列，实际: ${cols}`);
  assert.ok(cols.includes('name') && cols.includes('description'), `应包含基础列，实际: ${cols}`);
});

test('admin createShopItem (zh-only / Android 兼容): 仅 zh 落库，其余空占位、不报错', async () => {
  const { env, writes } = buildEnv();
  const body = {
    name: '苹果', description: '', item_type: 'PROP_FOOD', points_price: 100,
  };
  const res = await adminCall(env, 'POST', '/api/admin/shop-items', body);
  await flush();
  assert.equal(res.status, 200);

  const seeds = i18nSeedWrites(writes);
  assert.equal(seeds.length, 10, `旧调用方只发 zh，仍应播种 10 行空占位，实际 ${seeds.length}`);
  const m = i18nSeedMap(writes);
  assert.equal(m['shop_items.1.name.zh'], '苹果');
  assert.equal(m['shop_items.1.name.en'], '');
  assert.equal(m['shop_items.1.name.tw'], '');
  assert.equal(m['shop_items.1.name.ja'], '');
  assert.equal(m['shop_items.1.name.ko'], '');
  // description 基列为空 → 5 语言全空占位
  assert.equal(m['shop_items.1.description.zh'], '');
  assert.equal(m['shop_items.1.description.ko'], '');
});

test('admin createNotice (5 语言): title/content 真实落库', async () => {
  const { env, writes } = buildEnv();
  const body = {
    title: '维护公告', title_en: 'Maintenance', title_tw: '維護公告', title_ja: 'メンテ', title_ko: '점검',
    content: '系统维护中', content_en: 'Under maintenance', content_tw: '系統維護中', content_ja: 'メンテ中', content_ko: '점검 중',
    type: 'MAINTENANCE',
  };
  const res = await adminCall(env, 'POST', '/api/admin/notices', body);
  await flush();
  assert.equal(res.status, 200);
  const m = i18nSeedMap(writes);
  assert.equal(m['notice.1.title.zh'], '维护公告');
  assert.equal(m['notice.1.title.en'], 'Maintenance');
  assert.equal(m['notice.1.title.ko'], '점검');
  assert.equal(m['notice.1.content.zh'], '系统维护中');
  assert.equal(m['notice.1.content.ja'], 'メンテ中');
  const ins = writes.find((w) => w.sql.includes('INSERT INTO notice'));
  assert.ok(!hasSuffixCol(insertCols(ins.sql)), 'notice INSERT 不应含后缀列');
});

test('admin createAppVersion (5 语言 update_log): 真实落库', async () => {
  const { env, writes } = buildEnv();
  const body = {
    version_code: 200, version_name: 'v200', status: 0,
    update_log: '基础更新', update_log_en: 'Base update', update_log_tw: '基礎更新', update_log_ja: '基本更新', update_log_ko: '기본 업데이트',
  };
  const res = await adminCall(env, 'POST', '/api/admin/app-versions', body);
  await flush();
  assert.equal(res.status, 200);
  const seeds = i18nSeedWrites(writes).filter((w) => w.args[2] === 'app_version');
  assert.equal(seeds.length, 5, `update_log × 5 语言 = 5 行，实际 ${seeds.length}`);
  const m = i18nSeedMap(writes);
  assert.equal(m['app_version.200.update_log.zh'], '基础更新');
  assert.equal(m['app_version.200.update_log.en'], 'Base update');
  assert.equal(m['app_version.200.update_log.tw'], '基礎更新');
  assert.equal(m['app_version.200.update_log.ja'], '基本更新');
  assert.equal(m['app_version.200.update_log.ko'], '기본 업데이트');
  const ins = writes.find((w) => w.sql.includes('INSERT INTO app_version'));
  assert.ok(!hasSuffixCol(insertCols(ins.sql)), 'app_version INSERT 不应含后缀列');
});

test('admin createTask (5 语言): name/description 真实落库（onCreated 用 body.id 作 entityId）', async () => {
  const { env, writes } = buildEnv();
  const body = {
    id: 't_001',
    name: '每日任务', name_en: 'Daily', name_tw: '每日任務', name_ja: 'デイリー', name_ko: '데일리',
    description: '完成任务', description_en: 'Complete', description_tw: '完成任務', description_ja: '完了', description_ko: '완료',
    target_count: 5, points_reward: 10,
  };
  const res = await adminCall(env, 'POST', '/api/admin/tasks', body);
  await flush();
  assert.equal(res.status, 200);
  const m = i18nSeedMap(writes);
  assert.equal(m['task.t_001.name.zh'], '每日任务');
  assert.equal(m['task.t_001.name.en'], 'Daily');
  assert.equal(m['task.t_001.name.ko'], '데일리');
  assert.equal(m['task.t_001.description.zh'], '完成任务');
  assert.equal(m['task.t_001.description.ja'], '完了');
  const seeds = i18nSeedWrites(writes).filter((w) => w.args[2] === 'task');
  assert.equal(seeds.length, 10, `name+description 各 5 语言 = 10 行，实际 ${seeds.length}`);
  const ins = writes.find((w) => w.sql.includes('INSERT INTO task'));
  assert.ok(!hasSuffixCol(insertCols(ins.sql)), 'task INSERT 不应含后缀列');
});
