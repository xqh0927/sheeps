const assert = require('node:assert/strict');
const { before, after, test } = require('node:test');
const { readFileSync } = require('node:fs');
const { join } = require('node:path');
const { Miniflare } = require('miniflare');
const { unstable_splitSqlQuery: splitSql } = require('wrangler');
const { handleAdminRoutes } = require('../.tmp-test/handlers/admin');
const { hashPassword } = require('../.tmp-test/auth-utils');

let runtime, env, token;
async function adminRequest(method, route, body) {
  const url = new URL('/api/admin/' + route, 'http://localhost');
  const headers = { 'Content-Type': 'application/json' };
  if (token) headers.Authorization = 'Bearer ' + token;
  const request = new Request(url, { method, headers, body: JSON.stringify(body) });
  const response = await handleAdminRoutes(request, env, url.pathname, url);
  assert.equal(response.status, 200);
  return response.json();
}

const post = (route, body) => adminRequest('POST', route, body);

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

test('editing a task and its simplified Chinese translation publishes the same name and description', async () => {
  const id = 'QA.TASK.ZH';
  await post('tasks', { id, name: '原名', description: '原说明', name_en: 'English', target_count: 1, points_reward: 1 });
  await adminRequest('PUT', 'tasks/' + id, { name: '新名', description: '新说明' });
  for (const [field, value] of [['name', '新名'], ['description', '新说明']]) {
    const key = 'task.' + id + '.' + field;
    const { list } = await adminRequest('GET', 'i18n?keyword=' + encodeURIComponent(key));
    const zh = list.find(row => row.locale === 'zh');
    assert.equal(zh.value, value);
    await adminRequest('PUT', 'i18n/' + zh.id, { value: value + '二' });
    assert.equal(await env.DB.prepare('SELECT ' + field + ' FROM task WHERE id = ?').bind(id).first(field), value + '二');
  }
  assert.equal(await env.DB.prepare("SELECT value FROM i18n_strings WHERE str_key = ? AND locale = 'en'")
    .bind('task.' + id + '.name').first('value'), 'English');
});

test('translation value searches return every locale of the matched key', async () => {
  await post('tasks', { id: 'GROUP_KEY', name: '名称', name_en: 'unique-group-match', description: '说明', target_count: 1, points_reward: 1 });
  const { list } = await adminRequest('GET', 'i18n?keyword=unique-group-match');
  assert.deepEqual(list.map(r => r.locale).sort(), ['en', 'ja', 'ko', 'tw', 'zh']);
  assert.ok(list.every(row => row.str_key === 'task.GROUP_KEY.name'));
  const englishOnly = await adminRequest('GET', 'i18n?locale=en&keyword=unique-group-match');
  assert.deepEqual(englishOnly.list.map(row => row.locale), ['en']);
  const group = 'i18n?module=task&str_key=task.GROUP_KEY.name';
  assert.equal((await adminRequest('DELETE', group)).deleted, 5);
  assert.equal((await adminRequest('DELETE', group)).deleted, 0);
  assert.equal((await adminRequest('GET', 'i18n?keyword=task.GROUP_KEY.description')).list.length, 5);
});

test('automatic and supplied administrator passwords both provision a working operator account', async () => {
  for (const [phone, password] of [['19900009983', undefined], ['19900009984', 'local-explicit-secret']]) {
    const account = await post('accounts', { phone, password, role: 'operator' });
    assert.ok(account.id);
    if (password) assert.equal(account.initialPassword, undefined);
    else assert.ok(account.initialPassword);
    const login = await post('login', { phone, password: password ?? account.initialPassword });
    assert.equal(login.user.id, account.id);
    assert.equal(login.user.role, 'operator');
  }
});

test('historical task repair requires evidence of the last Chinese translation edit', async () => {
  const id = 'HISTORY_TASK';
  await post('tasks', { id, name: '原名称', description: '原说明', target_count: 1, points_reward: 5 });
  const { list } = await adminRequest('GET', 'i18n?keyword=task.HISTORY_TASK.name&locale=zh');
  await adminRequest('PUT', 'i18n/' + list[0].id, { value: '已确认的名称' });
  // Recreate a pre-fix one-sided edit and a second disagreement without audit evidence.
  await env.DB.prepare('UPDATE task SET name = ? WHERE id = ?').bind('原名称', id).run();
  await env.DB.prepare("UPDATE i18n_strings SET value = ? WHERE locale = 'zh' AND str_key = ?")
    .bind('未核实说明', 'task.HISTORY_TASK.description').run();
  const migration = readFileSync(join(__dirname, '../migrations/0014_task_zh_publication.sql'), 'utf8');
  for (let run = 0; run < 2; run++) {
    await env.DB.batch(splitSql(migration).map(sql => env.DB.prepare(sql)));
    const task = await env.DB.prepare('SELECT name, description FROM task WHERE id = ?').bind(id).first();
    assert.deepEqual(task, { name: '已确认的名称', description: '原说明' });
    assert.equal(await env.DB.prepare("SELECT value FROM i18n_strings WHERE locale = 'zh' AND str_key = ?")
      .bind('task.HISTORY_TASK.description').first('value'), '未核实说明');
  }
});
