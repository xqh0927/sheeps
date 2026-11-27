const { before, after, test } = require('node:test');
const assert = require('node:assert/strict');
const { readFileSync } = require('node:fs');
const { join } = require('node:path');
const { buildSync } = require('esbuild');
const { Miniflare } = require('miniflare');
const { unstable_splitSqlQuery: splitSql } = require('wrangler');
const { generateJWT, sha256 } = require('../.tmp-test/crypto');
const { hashPassword } = require('../.tmp-test/auth-utils');

let runtime, db, token;
const unavailableApks = new Set();
before(async () => {
  const script = buildSync({ entryPoints: [join(__dirname, '../src/index.ts')], bundle: true, format: 'esm', write: false });
  runtime = new Miniflare({
    modules: true, script: script.outputFiles[0].text, compatibilityDate: '2026-06-27',
    d1Databases: ['DB'], kvNamespaces: ['SHEEPS_CACHE'], r2Buckets: ['AVATAR_BUCKET'],
    outboundService: req => new Response(null, {
      status: req.method === 'HEAD' && req.url.startsWith('https://apk.invalid/') && !unavailableApks.has(req.url) ? 200 : 404,
    }),
  });
  db = await runtime.getD1Database('DB');
  await db.batch(splitSql(readFileSync(join(__dirname, '../schema.sql'), 'utf8')).map(sql => db.prepare(sql)));
  const phone = '19900009988', password = 'temporary-test-pass';
  await db.prepare('INSERT INTO users(id, phone, username, role, password_hash, created_at) VALUES (?, ?, ?, ?, ?, ?)')
    .bind('publisher', phone, 'Publisher', 'super', await hashPassword(password), Date.now()).run();
  token = (await request('/api/admin/login', 'POST', { phone, password })).token;
});
after(async () => { await runtime?.dispose(); });

async function request(path, method = 'GET', body, language = 'zh-CN', expected = 200) {
  const headers = { 'Content-Type': 'application/json', 'Accept-Language': language };
  if (token && path.startsWith('/api/admin/')) headers.Authorization = 'Bearer ' + token;
  const res = await runtime.dispatchFetch('http://localhost' + path, {
    method, headers, body: body === undefined ? undefined : JSON.stringify(body),
  });
  const result = await res.json();
  assert.equal(res.status, expected, JSON.stringify(result));
  return result;
}

test('the tutorial skips the old seeded cache and gives priority to a published first level', async () => {
  const cache = await runtime.getKVNamespace('SHEEPS_CACHE');
  const path = '/api/level?id=1&seed=117';
  await cache.put('v4_1_117', '[]');
  const board = await request(path);
  assert.equal(board.length, 12);
  assert.deepEqual(await request('/api/level?id=1&seed=654&userId=42'), board);
  assert.ok(board.every(t => !t.isBlind && t.sealedCount === 0));
  const remaining = new Set(board);
  const exposed = () => [...remaining].filter(t => ![...remaining].some(upper =>
    upper.z > t.z && Math.abs(upper.x - t.x) < 1 && Math.abs(upper.y - t.y) < 1));
  const opening = exposed();
  assert.deepEqual(opening.map(t => t.type).sort(), [1, 1, 1, 2]);
  for (const type of [1, 2, 3, 1]) {
    const match = exposed().filter(t => t.type === type);
    assert.equal(match.length, 3);
    if (type === 2) assert.equal(match.filter(t => !opening.includes(t)).length, 2);
    match.forEach(t => remaining.delete(t));
  }
  assert.equal(remaining.size, 0);
  const custom = [1, 2, 3].map(x => ({
    id: 'published-' + x, x, y: 1, z: 0, type: 9, isBlind: false, sealedCount: 0,
  }));
  await request('/api/admin/levels', 'POST', { level_id: 1, difficulty: 1, layout_data: custom });
  assert.deepEqual(await request(path), custom);
  await request('/api/admin/levels/1', 'DELETE');
  assert.deepEqual(await request(path), board);
});

test('the same seeded request sees published and edited tiles and resumes generation after deletion', async () => {
  const path = '/api/level?id=42&seed=731';
  const generated = await request(path);
  const headers = async () => (await runtime.dispatchFetch('http://localhost' + path)).headers;
  assert.equal((await headers()).get('X-Stage-Source'), 'generated');
  assert.equal((await headers()).get('X-Stage-Version'), 'generated-v5');
  const make = length => Array.from({ length }, (_, i) => ({
    id: 'fixture-' + i, x: i, y: 0, z: 0, type: 1, isBlind: false, sealedCount: 0,
  }));
  await request('/api/admin/levels', 'POST', { level_id: 42, difficulty: 1, layout_data: JSON.stringify(make(3)) });
  assert.deepEqual(await request(path), make(3));
  const publishedHeaders = await headers();
  assert.equal(publishedHeaders.get('X-Stage-Source'), 'published');
  const version = publishedHeaders.get('X-Stage-Version');
  assert.match(version, /^[0-9a-f]{64}$/);
  await request('/api/admin/levels/42', 'PUT', { difficulty: 2, layout_data: make(6) });
  assert.deepEqual(await request(path), make(6));
  assert.notEqual((await headers()).get('X-Stage-Version'), version);
  assert.deepEqual(await request('/api/level?id=42'), make(6));
  await request('/api/admin/levels/42', 'PUT', { difficulty: 3, layout_data: '{}' }, 'zh-CN', 400);
  assert.deepEqual(await request(path), make(6));
  assert.equal(await db.prepare('SELECT difficulty FROM levels WHERE level_id=42').first('difficulty'), 2);
  await request('/api/admin/levels/42', 'DELETE');
  assert.equal((await headers()).get('X-Stage-Source'), 'generated');
  assert.deepEqual(await request(path), generated);
});

const opening = [
  { id: 'lower', x: 0, y: 0, z: 0, type: 4, isBlind: true, sealedCount: 0 },
  { id: 'upper', x: 0.99, y: 0, z: 1, type: 4, isBlind: false, sealedCount: 1 },
  { id: 'neighbor', x: 0, y: 0, z: 0, type: 4, isBlind: false, sealedCount: 1 },
];

for (const { reason, levelId, tiles, message } of [
  { reason: 'incomplete triples by type', levelId: 51,
    tiles: opening.map((tile, i) => ({ ...tile, type: i === 0 ? 2 : 4 })), message: /花色.*2.*3/ },
  { reason: 'no opening after accumulating two small overlaps from higher layers', levelId: 52,
    tiles: [opening[0], { ...opening[1], x: 0.994 }, { ...opening[2], x: -0.994, z: 2 }], message: /起手/ },
  { reason: 'the first same-layer touch target intercepts both unsealed tiles', levelId: 53,
    tiles: [opening[2], opening[0], { ...opening[1], x: 0, z: 0, sealedCount: 0 }], message: /起手/ },
  { reason: 'two earlier rectangles jointly intercept the entire tile', levelId: 54,
    tiles: [{ ...opening[2], y: -0.5 }, { ...opening[1], x: 0, y: 0.5, z: 0 }, opening[0]], message: /起手/ },
  { reason: 'a higher tile intercepts the only gap left by an earlier same-layer tile', levelId: 61,
    tiles: [{ ...opening[2], x: -1 / 128 }, opening[0], { ...opening[1], x: 127 / 128 }], message: /起手/ },
]) {
  test('invalid publication leaves the current level intact: ' + reason, async () => {
    const adminPath = '/api/admin/levels';
    const path = '/api/level?id=' + levelId + '&seed=731';
    const generated = await request(path);
    const invalid = { level_id: levelId, difficulty: 4, layout_data: tiles };
    const rejected = await request(adminPath, 'POST', invalid, 'zh-CN', 400);
    assert.match(rejected.error, message);
    assert.equal(await db.prepare('SELECT level_id FROM levels WHERE level_id=?').bind(levelId).first(), null);
    assert.deepEqual(await request(path), generated);

    // The blind lower tile is clickable at the Android one-percent boundary.
    // The sealed tile at the same height contributes no cover.
    await request(adminPath, 'POST', { level_id: levelId, difficulty: 1, layout_data: opening });
    const edited = await request(adminPath + '/' + levelId, 'PUT', invalid, 'zh-CN', 400);
    assert.match(edited.error, message);
    assert.equal(await db.prepare('SELECT difficulty FROM levels WHERE level_id=?').bind(levelId).first('difficulty'), 1);
    assert.deepEqual(await request(path), opening);
  });
}

test('a gap between earlier rectangles allows a touch away from the center', async () => {
  const tiles = [{ ...opening[2], y: -0.25 }, { ...opening[1], x: 0, y: 0.875, z: 0 }, opening[0]];
  await request('/api/admin/levels', 'POST', { level_id: 55, layout_data: tiles });
  assert.deepEqual(await request('/api/level?id=55'), tiles);
});

for (const [levelId, field, value] of [[56, 'z', 2147483648], [57, 'type', -2147483649],
  [58, 'sealedCount', -2147483649], [59, 'x', 1e39], [60, 'y', -1e39]]) {
  test('rejects a tile with a value outside Android numeric range: ' + field, async () => {
    const tiles = opening.map(tile => ({ ...tile, [field]: value }));
    const rejected = await request('/api/admin/levels', 'POST', { level_id: levelId, layout_data: tiles }, 'zh-CN', 400);
    assert.match(rejected.error, /Android.*范围/);
  });
}

test('legacy level JSON remains playable and can be replaced or cleared', async () => {
  await db.prepare("ALTER TABLE levels ADD COLUMN layout_data TEXT NOT NULL DEFAULT '[]'").run();
  const tiles = [{ id: 'original', x: 1.5, y: 0, z: 0, type: 2, isBlind: false, sealedCount: 0 }];
  const path = '/api/level?id=7&seed=731';
  const generated = await request(path);
  await db.prepare('INSERT INTO levels (level_id, difficulty, created_at, layout_data) VALUES (7, 1, 1, ?)')
    .bind(JSON.stringify(tiles)).run();
  assert.deepEqual(await request(path), tiles);
  const replacement = [0, 1, 2].map(x => ({ ...tiles[0], x, id: 'replacement-' + x }));
  await request('/api/admin/levels/7', 'PUT', { layout_data: replacement });
  assert.deepEqual(await request(path), replacement);
  await request('/api/admin/levels/7', 'PUT', { layout_data: [] });
  assert.deepEqual(await request(path), generated);
});

test('published notices reflect edits in both admin forms, including previously read locales', async () => {
  const { id } = await request('/api/admin/notices', 'POST', {
    type: 'NOTICE', title: '初稿', title_en: 'Draft', content: '正文', content_en: 'Body',
  });
  const notice = async lang => (await request('/api/notice/list', 'GET', undefined, lang)).find(n => String(n.id) === id);
  const rows = async () => (await request('/api/admin/i18n?module=notice')).list.filter(r => r.str_key === 'notice.' + id + '.title');
  const edit = async (lang, value) => request('/api/admin/i18n/' + (await rows()).find(r => r.locale === lang).id, 'PUT', { value });
  assert.equal((await notice('en')).title, 'Draft');
  assert.equal((await notice('zh-CN')).title, '初稿');
  await edit('en', 'Published');
  assert.equal((await notice('en')).title, 'Published');
  await edit('zh', '正式');
  assert.equal((await notice('zh-CN')).title, '正式');
  assert.equal((await request('/api/admin/notices')).list.find(n => String(n.id) === id).title, '正式');
  await request('/api/admin/notices/' + id, 'PUT', { title: '修订' });
  assert.equal((await rows()).find(r => r.locale === 'zh').value, '修订');
  await request('/api/admin/i18n/' + (await rows()).find(r => r.locale === 'en').id, 'DELETE');
  assert.equal((await notice('en')).title, '修订');
  await request('/api/admin/notices/' + id, 'DELETE');
  assert.equal(await notice('en'), undefined);
});

test('release, edit, withdrawal, republish and deletion reach the same client version immediately', async () => {
  const check = () => request('/api/app/check-update?version_code=390', 'GET', undefined, 'en');
  const url = '/api/admin/app-versions/400';
  await request('/api/admin/app-versions', 'POST', {
    version_code: 400, version_name: '4.0', download_url: 'https://apk.invalid/first', status: 0,
    update_log: 'Initial', update_log_en: 'First release',
  });
  assert.equal((await check()).has_update, false);
  await request(url, 'PUT', { status: 1 });
  assert.equal((await check()).update_log, 'First release');
  await request(url, 'PUT', { version_name: '4.0.1', download_url: 'https://apk.invalid/second', is_force_update: 1 });
  const edited = await check();
  assert.equal(edited.version_name, '4.0.1');
  assert.equal(edited.apk_url, 'https://apk.invalid/second');
  assert.equal(edited.force_update, true);
  await request(url, 'PUT', { status: 2 });
  assert.equal((await check()).has_update, false);
  await request(url, 'PUT', { status: 1 });
  assert.equal((await check()).has_update, true);
  await request(url, 'DELETE');
  assert.equal((await check()).has_update, false);
});

test('republishing a now-available APK at the same URL is visible immediately', async () => {
  const url = 'https://apk.invalid/reuploaded';
  const updates = () => request('/api/app/check-update?version_code=401');
  unavailableApks.add(url);
  await request('/api/admin/app-versions', 'POST', { version_code: 402, version_name: 'replacement', status: 1, download_url: url });
  assert.equal((await updates()).has_update, false);
  await request('/api/admin/app-versions/402', 'PUT', { status: 2 });
  unavailableApks.delete(url);
  await request('/api/admin/app-versions/402', 'PUT', { status: 1 });
  assert.equal((await updates()).apk_url, url);
  await request('/api/admin/app-versions/402', 'DELETE');
});

test('only pending requests appear in the filter; old confirmations preserve player data', async () => {
  const id = 'closing-player', phone = '19900001238', password = 'temporary-player';
  await db.prepare('INSERT INTO users(id, phone, username, role, password_hash, created_at) VALUES (?, ?, ?, ?, ?, ?)')
    .bind(id, phone, 'Closing player', 'user', await hashPassword(password), Date.now()).run();
  await db.prepare('INSERT INTO user_items (user_id, item_type, count) VALUES (?, ?, ?)').bind(id, 'UNDO', 7).run();
  const login = await request('/api/auth/login-password', 'POST', { phone, password });
  async function changeRequest(method) {
    const response = await runtime.dispatchFetch('http://localhost/api/user/deletion-request', {
      method, headers: { Authorization: 'Bearer ' + login.token },
    });
    assert.equal(response.status, 200);
    return response.json();
  }

  const bucket = await runtime.getR2Bucket('AVATAR_BUCKET');
  const ownKey = 'avatars/closing-player_123.png', neighbor = 'avatars/closing-player_other_123.png';
  await bucket.put(ownKey, 'fixture');
  await bucket.put(neighbor, 'neighbor');
  await db.prepare('UPDATE users SET avatar_url=? WHERE id=?').bind('https://external.invalid/' + neighbor, id).run();
  const first = (await changeRequest('POST')).requested_at;
  const pending = await request('/api/admin/users?deletion_pending=1');
  assert.equal(pending.total, 1);
  assert.equal(pending.list[0].id, id);
  assert.equal(pending.list[0].deletion_requested_at, first);
  assert.equal((await request('/api/admin/users?role=super&deletion_pending=1')).total, 0);

  const remove = stamp => '/api/admin/users/' + id + '?expected_requested_at=' + stamp;
  await changeRequest('DELETE');
  await request(remove(first), 'DELETE', undefined, 'zh-CN', 409);
  const second = (await changeRequest('POST')).requested_at;
  assert.notEqual(second, first);
  await request(remove(first), 'DELETE', undefined, 'zh-CN', 409);
  assert.equal(await db.prepare('SELECT count FROM user_items WHERE user_id=?').bind(id).first('count'), 7);
  assert.equal((await changeRequest('GET')).requested_at, second);
  assert.ok(await bucket.head(ownKey));
  await request(remove(second), 'DELETE');
  assert.equal(await db.prepare('SELECT id FROM users WHERE id=?').bind(id).first(), null);
  assert.equal(await db.prepare('SELECT user_id FROM user_items WHERE user_id=?').bind(id).first(), null);
  assert.equal((await request('/api/admin/users?deletion_pending=1')).total, 0);
  assert.equal(await bucket.head(ownKey), null);
  assert.ok(await bucket.head(neighbor));
  const snapshots = await db.prepare(`SELECT c.field FROM admin_audit_changes c JOIN admin_audit_log a ON a.id=c.change_id
    WHERE a.action='DELETE_USER' AND a.target_id=?`).bind(id).all();
  assert.deepEqual(snapshots.results.map(r => r.field), ['deletion_requested_at']);
  await db.prepare('INSERT INTO users(id,username,created_at) VALUES (?, ?, ?)').bind('manual', 'Manual', Date.now()).run();
  await request('/api/admin/users/manual', 'DELETE');
  assert.equal(await db.prepare("SELECT id FROM users WHERE id='manual'").first(), null);
});

test('legacy Chinese repair follows proven edits only and keeps unaudited text intact', async () => {
  const { id } = await request('/api/admin/notices', 'POST', { type: 'NOTICE', title: '原标题', content: '原正文' });
  const rows = (await request('/api/admin/i18n?module=notice&locale=zh')).list;
  const title = rows.find(row => row.str_key === 'notice.' + id + '.title');
  await request('/api/admin/i18n/' + title.id, 'PUT', { value: '改译标题' });
  await request('/api/admin/notices/' + id, 'PUT', { content: '改正正文' });
  // Recreate the old one-sided writes using genuine audit records.
  await db.prepare('UPDATE notice SET title=? WHERE id=?').bind('原标题', id).run();
  await db.prepare('UPDATE i18n_strings SET value=? WHERE str_key=? AND locale=?')
    .bind('原正文', 'notice.' + id + '.content', 'zh').run();
  const unknown = (await request('/api/admin/notices', 'POST', { type: 'NOTICE', title: '已知基列', content: '正文' })).id;
  await db.prepare('UPDATE i18n_strings SET value=? WHERE str_key=? AND locale=?')
    .bind('来源不明', 'notice.' + unknown + '.title', 'zh').run();

  const migration = readFileSync(join(__dirname, '../migrations/0005_notice_zh_publication.sql'), 'utf8');
  for (let i = 0; i < 2; i++) {
    await db.batch(splitSql(migration).map(sql => db.prepare(sql)));
    assert.equal(await db.prepare('SELECT title FROM notice WHERE id=?').bind(id).first('title'), '改译标题');
    assert.equal(await db.prepare("SELECT value FROM i18n_strings WHERE locale='zh' AND str_key=?")
      .bind('notice.' + id + '.content').first('value'), '改正正文');
    assert.equal(await db.prepare('SELECT title FROM notice WHERE id=?').bind(unknown).first('title'), '已知基列');
  }
});

test('R2 failures leave a durable job; the scheduled pass removes owned files only', async () => {
  const { completeAvatarCleanup } = require('../.tmp-test/lib/account-avatars');
  const bucket = await runtime.getR2Bucket('AVATAR_BUCKET');
  await db.prepare('INSERT INTO account_avatar_cleanup (user_id, deleted_at, revision) VALUES (?,?,?)').bind('retry', 500, 'fixture').run();
  await bucket.put('avatars/retry_300.png', 'old', { customMetadata: { userId: 'retry' } });
  await bucket.put('avatars/retry_301.png', 'other', { customMetadata: { userId: 'different' } });
  await bucket.put('avatars/retry_501.png', 'new');
  const busy = { DB: db, AVATAR_BUCKET: { list: opts => bucket.list(opts), delete: async () => { throw Error('R2 unavailable'); } } };
  assert.equal(await completeAvatarCleanup(busy, 'retry'), false);
  assert.ok(await db.prepare("SELECT last_attempt_at FROM account_avatar_cleanup WHERE user_id='retry'").first('last_attempt_at') > 0);
  await (await runtime.getWorker()).scheduled({ cron: '17 * * * *' });
  assert.equal(await bucket.head('avatars/retry_300.png'), null);
  assert.ok(await bucket.head('avatars/retry_301.png'));
  assert.ok(await bucket.head('avatars/retry_501.png'));
  assert.equal(await db.prepare("SELECT user_id FROM account_avatar_cleanup WHERE user_id='retry'").first(), null);
});

test('level health counts first starts, settled outcomes, tools and subsequent same-level starts in play order', async () => {
  const guest = 'uuid-' + crypto.randomUUID();
  const other = 'uuid-' + crypto.randomUUID();
  const level = 134;
  const start = (user, run, version, at, levelId = level) => request('/api/level/attempt', 'POST', {
    user_id: user, run_id: run, layout_version: version, level_id: levelId, started_at: at,
  });
  const score = async (user, run, win, tools, at, abandoned = false) => request('/api/score/submit-once', 'POST', {
    user_id: user, level_id: level, score: 15, clear_time_ms: 5, is_win: win, items_used: tools, abandoned,
    run_id: run, completed_at: at, sign: await sha256(user + '_' + level + '_5_folklore'),
  });
  const health = (filter = '') => request('/api/admin/levels/health?level_id=' + level + filter);
  // Scores and starts can arrive in reverse order. An old client only sends a score.
  await score(guest, 'legacy', 1, 1, 10);
  await score(guest, 'lost', 0, 2, 110);
  await Promise.all([score(guest, 'lost', 1, 0, 111), score(guest, 'lost', 0, 2, 110)]);
  await score(other, 'won', 1, 0, 120);
  await start(guest, 'retry', 'v2', 130);
  await Promise.all([start(guest, 'lost', 'v1', 100), start(guest, 'lost', 'v1', 100)]);
  await start(other, 'won', 'v1', 115);
  await start(other, 'overlap', 'v1', 180);
  await score(other, 'overlap', 0, 0, 200);
  await start(other, 'already-playing', 'v1', 190);
  await start(other, 'other-level', 'v1', 250, level + 1);
  // A later win after abandoning the first start is not a first win.
  await start(guest, 'later-win', 'v2', 150);
  await score(guest, 'later-win', 1, 1, 170);
  // Voluntarily restarting settles the run but does not create a failure or retry.
  await start(guest, 'restart', 'v2', 175);
  await score(guest, 'restart', 0, 0, 180, true);
  await start(guest, 'after-restart', 'v2', 185);
  const data = await health();
  assert.equal(data.total, 2);
  assert.deepEqual(data.list, [
    { level_id: level, layout_version: 'v1', attempts: 4, settled_attempts: 3, failed_attempts: 2,
      first_settled_attempts: 2, first_wins: 1, retried_failures: 1, item_attempts: 1,
      first_win_rate: 0.5, retry_rate: 0.5, item_use_rate: 1 / 3 },
    { level_id: level, layout_version: 'v2', attempts: 4, settled_attempts: 2, failed_attempts: 0,
      first_settled_attempts: 0, first_wins: 0, retried_failures: 0, item_attempts: 1,
      first_win_rate: null, retry_rate: null, item_use_rate: 0.5 },
  ]);
  const page = await health('&page=2&pageSize=1');
  assert.deepEqual(page.list, [data.list[1]]);
  assert.equal(page.total, 2);
  assert.deepEqual((await health('&layout_version=v1')).list, [data.list[0]]);
  await request('/api/admin/users/' + guest, 'DELETE');
  const remaining = (await health()).list;
  assert.equal(remaining.length, 1);
  assert.equal(remaining[0].attempts, 3);
  assert.equal(remaining[0].retried_failures, 0);
});

test('an unknown layout proves a retry without adding a health cohort', async () => {
  const user = 'uuid-' + crypto.randomUUID();
  const level = 136;
  const start = (run, version, at) => request('/api/level/attempt', 'POST', {
    user_id: user, run_id: run, level_id: level, layout_version: version, started_at: at,
  });
  await start('first', 'generated-v5', 100);
  await request('/api/score/submit-once', 'POST', {
    user_id: user, run_id: 'first', level_id: level, is_win: 0, clear_time_ms: 10, score: 0,
    completed_at: 110, sign: await sha256(user + '_' + level + '_10_folklore'),
  });
  await start('retried', 'unknown', 120);
  const path = '/api/admin/levels/health?level_id=' + level;
  const page = await request(path);
  assert.equal(page.total, 1);
  assert.deepEqual(page.list.map(row => [row.layout_version, row.attempts, row.retry_rate]), [['generated-v5', 1, 1]]);
  const missing = await request(path + '&layout_version=unknown');
  assert.equal(missing.total, 0);
  assert.deepEqual(missing.list, []);
});

test('attempts acknowledge only matching identities and authenticated admins can read level health', async () => {
  const guest = 'uuid-' + crypto.randomUUID();
  const body = { user_id: guest, run_id: 'start-id', level_id: 135, layout_version: 'generated-v5', started_at: 250 };
  assert.deepEqual(await request('/api/level/attempt', 'POST', body), { success: true, run_id: body.run_id });
  await request('/api/level/attempt', 'POST', { ...body, layout_version: '' }, 'zh-CN', 400);
  await request('/api/level/attempt', 'POST', { ...body, started_at: 0 }, 'zh-CN', 400);
  await request('/api/level/attempt', 'POST', { ...body, user_id: 'publisher' }, 'zh-CN', 401);
  async function withToken(route, access, payload) {
    const headers = { 'Content-Type': 'application/json' };
    if (access) headers.Authorization = 'Bearer ' + access;
    return runtime.dispatchFetch('http://localhost' + route, {
      method: payload ? 'POST' : 'GET', headers, body: payload && JSON.stringify(payload),
    });
  }
  const playerToken = await generateJWT({ userId: guest, type: 'access', exp: Date.now() + 60000 });
  assert.equal((await withToken('/api/level/attempt', playerToken, body)).status, 200);
  assert.equal((await withToken('/api/level/attempt', playerToken, { ...body, user_id:'publisher' })).status, 409);
  assert.equal((await withToken('/api/level/attempt', 'invalid', body)).status, 401);
  for (const credential of [playerToken, undefined]) {
    assert.equal((await withToken('/api/admin/levels/health', credential)).status, 401);
  }
  const row = (await request('/api/admin/levels/health?level_id=135')).list[0];
  assert.equal(row.attempts, 1);
  assert.equal(row.item_use_rate, null);
  await request('/api/admin/users/' + guest, 'DELETE');
  assert.equal((await withToken('/api/level/attempt', playerToken, body)).status, 401);
});
