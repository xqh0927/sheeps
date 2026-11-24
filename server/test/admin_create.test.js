const assert = require('node:assert/strict');
const { before, after, test } = require('node:test');
const { readFileSync } = require('node:fs');
const { join } = require('node:path');
const { Miniflare } = require('miniflare');
const { unstable_splitSqlQuery: splitSql } = require('wrangler');
const { handleAdminRoutes } = require('../.tmp-test/handlers/admin');
const { hashPassword } = require('../.tmp-test/auth-utils');

let runtime, env, token;
async function post(route, body) {
  const url = new URL('/api/admin/' + route, 'http://localhost');
  const headers = { 'Content-Type': 'application/json' };
  if (token) headers.Authorization = 'Bearer ' + token;
  const request = new Request(url, { method: 'POST', headers, body: JSON.stringify(body) });
  const response = await handleAdminRoutes(request, env, url.pathname, url);
  assert.equal(response.status, 200);
  return response.json();
}

before(async () => {
  runtime = new Miniflare({
    modules: true, script: 'export default {}', d1Databases: ['DB'],
    kvNamespaces: ['SHEEPS_CACHE'], compatibilityDate: '2026-06-27',
  });
  env = { DB: await runtime.getD1Database('DB'), SHEEPS_CACHE: await runtime.getKVNamespace('SHEEPS_CACHE') };
  const schema = readFileSync(join(__dirname, '../schema.sql'), 'utf8');
  await env.DB.batch(splitSql(schema).map(sql => env.DB.prepare(sql)));
  const password = 'local-test-password';
  await env.DB.prepare('INSERT INTO users(id, phone, username, role, password_hash, created_at) VALUES (?, ?, ?, ?, ?, ?)')
    .bind('fixture', '19900009988', 'Test admin', 'super', await hashPassword(password), Date.now()).run();
  token = (await post('login', { phone: '19900009988', password })).token;
});

after(async () => { await runtime?.dispose(); });

test('notice creation assigns a timestamp and uses the persisted ID for translations', async () => {
  const { id } = await post('notices', { type: 'ACTIVITY', title: '活动', title_en: 'Event', content: '公告' });
  const notice = await env.DB.prepare('SELECT id, created_at FROM notice WHERE id = ?').bind(id).first();
  assert.ok(notice.created_at > 0);
  const translated = await env.DB.prepare("SELECT value FROM i18n_strings WHERE str_key = ? AND locale = 'en'")
    .bind('notice.' + notice.id + '.title').first('value');
  assert.equal(translated, 'Event');
});

test('both generated and caller-provided entity IDs identify creation audit records', async () => {
  const shop = await post('shop-items', { name: '道具', name_en: 'Item', item_type: 'UNDO', points_price: 3 });
  const shopId = await env.DB.prepare("SELECT id FROM shop_items WHERE name = '道具'").first('id');
  assert.equal(shop.id, String(shopId));
  assert.equal(await env.DB.prepare("SELECT value FROM i18n_strings WHERE str_key = ? AND locale = 'en'")
    .bind('shop_items.' + shopId + '.name').first('value'), 'Item');
  const task = await post('tasks', { id: 'WIN_TEST', name: '闯关', description: '测试', target_count: 1, points_reward: 5 });
  assert.equal(task.id, 'WIN_TEST');
  for (const [action, id] of [['CREATE_SHOP_ITEM', shop.id], ['CREATE_TASK', task.id]]) {
    assert.equal(await env.DB.prepare('SELECT target_id FROM admin_audit_log WHERE action = ?')
      .bind(action).first('target_id'), id);
  }
});

test('simultaneous shop creations retain their own translation and audit IDs', async () => {
  const names = ['First product', 'Second product'];
  await Promise.all(names.map(async name => {
    const { id } = await post('shop-items', { name, name_en: name, item_type: 'HINT', points_price: 4 });
    assert.equal(await env.DB.prepare("SELECT value FROM i18n_strings WHERE str_key = ? AND locale = 'en'")
      .bind('shop_items.' + id + '.name').first('value'), name);
    assert.equal(await env.DB.prepare("SELECT COUNT(*) AS n FROM admin_audit_log WHERE action = 'CREATE_SHOP_ITEM' AND target_id = ?")
      .bind(id).first('n'), 1);
  }));
});

test('version creation supports a null legacy timestamp and the download URL from the form', async () => {
  const download = 'https://example.org/v2.apk';
  await post('app-versions', { version_code: 2, version_name: '2.0', download_url: download, update_log: '日志', created_at: null });
  const row = await env.DB.prepare('SELECT * FROM app_version WHERE version_code = 2').first();
  assert.ok(row.created_at > 0);
  assert.equal(row.apk_url, download);
  assert.equal(await env.DB.prepare("SELECT value FROM i18n_strings WHERE str_key = 'app_version.2.update_log' AND locale = 'zh'")
    .first('value'), '日志');
});

test('a failed translation write rolls back the new entity', async () => {
  await env.DB.prepare(`CREATE TRIGGER reject_test_translation BEFORE INSERT ON i18n_strings
    WHEN NEW.value = 'rollback-fixture' BEGIN SELECT RAISE(ABORT, 'fixture failure'); END`).run();
  await assert.rejects(post('shop-items', {
    name: 'rollback-fixture', item_type: 'HINT', points_price: 2,
  }), /fixture failure/);
  assert.equal(await env.DB.prepare("SELECT COUNT(*) FROM shop_items WHERE name = 'rollback-fixture'").first('COUNT(*)'), 0);
});
