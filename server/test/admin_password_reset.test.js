const { before, after, test } = require('node:test');
const assert = require('node:assert/strict');
const { readFileSync } = require('node:fs');
const { join } = require('node:path');
const { randomUUID } = require('node:crypto');
const { Miniflare } = require('miniflare');
const { unstable_splitSqlQuery } = require('wrangler');
const { handleAdminRoutes } = require('../.tmp-test/handlers/admin');
const { handleAuthRoutes } = require('../.tmp-test/handlers/auth');
const { hashPassword, verifyPassword } = require('../.tmp-test/auth-utils');

let runtime, env, oldHash, token;
const original = randomUUID();
const resetPassword = 'Qa' + randomUUID().slice(0, 12);
const phones = { super: '19900000011', operator: '19900000012', readonly: '19900000013', user: '19900000014' };

async function post(route, body, auth, bindings = env) {
  const url = new URL(route, 'http://localhost');
  const headers = {'Content-Type': 'application/json'};
  if (auth) headers.Authorization = 'Bearer ' + auth;
  const request = new Request(url, {method: 'POST', headers, body: JSON.stringify(body)});
  return url.pathname.startsWith('/api/admin/') ? handleAdminRoutes(request, bindings, url.pathname, url) : handleAuthRoutes(request, bindings, url.pathname);
}
before(async () => {
  runtime = new Miniflare({modules:true, script:'export default {}', d1Databases:['DB'], kvNamespaces:['SHEEPS_CACHE'], compatibilityDate:'2026-06-27'});
  env = {...await runtime.getBindings(), ACCOUNT_RESET_DEFAULT_PASSWORD: resetPassword};
  const sql = readFileSync(join(__dirname, '../schema.sql'), 'utf8');
  await env.DB.batch(unstable_splitSqlQuery(sql).map(s => env.DB.prepare(s)));
  oldHash = await hashPassword(original);
  for (const [role, phone] of Object.entries(phones)) {
    await env.DB.prepare('INSERT INTO users(id,phone,role,username,password_hash,created_at) VALUES(?,?,?,?,?,0)')
      .bind(role, phone, role, role, oldHash).run();
  }
  token = {};
  for (const role of ['super','operator','readonly']) {
    const response = await post('/api/admin/login',{phone:phones[role],password:original});
    assert.equal(response.status,200);
    token[role]=(await response.json()).token;
  }
});
after(async () => runtime?.dispose());

const reset = (target, auth, bindings) => post('/api/admin/users/' + target + '/reset-password', {}, auth, bindings);
const stored = id => env.DB.prepare('SELECT password_hash FROM users WHERE id=?').bind(id).first('password_hash');

test('only a currently active super may reset a password', async () => {
  assert.equal((await reset('user')).status,401);
  for (const role of ['operator','readonly']) assert.equal((await reset('user',token[role])).status,403);
  await env.DB.prepare("UPDATE users SET role='operator' WHERE id='super'").run();
  try { assert.equal((await reset('user',token.super)).status,403); }
  finally { await env.DB.prepare("UPDATE users SET role='super' WHERE id='super'").run(); }
  assert.equal(await stored('user'),oldHash);
});

test('configuration and target are checked before writing', async () => {
  assert.equal((await reset('user',token.super,{...env,ACCOUNT_RESET_DEFAULT_PASSWORD:undefined})).status,503);
  assert.equal((await reset('missing',token.super)).status,404);
  await env.DB.prepare("INSERT INTO users(id,username,created_at) VALUES('guest','guest',0)").run();
  assert.equal((await reset('guest',token.super)).status,404);
  assert.equal(await stored('user'),oldHash);
});

test('super resets players and all administrator roles; only the requested account changes', async () => {
  for (const role of ['user','readonly','operator','super']) {
    const response=await reset(role,token.super);
    assert.equal(response.status,200);
    assert.deepEqual(await response.json(),{success:true});
    const hash=await stored(role);
    assert.notEqual(hash,oldHash);
    assert.ok(await verifyPassword(resetPassword,hash));
    const login=role==='user'?'/api/auth/login-password':'/api/admin/login';
    assert.equal((await post(login,{phone:phones[role],password:original})).status,role === 'user' ? 400 : 401);
    assert.equal((await post(login,{phone:phones[role],password:resetPassword})).status,200);
    const audit=await env.DB.prepare("SELECT * FROM admin_audit_log WHERE action='RESET_USER_PASSWORD' AND target_id=?").bind(role).all();
    assert.equal(audit.results.length,1);
    assert.equal(audit.results[0].admin_id,'super');
    assert.ok(!JSON.stringify(audit.results).includes(resetPassword));
    assert.ok(!JSON.stringify(audit.results).includes(hash));
  }
});

test('new and legacy administrator accounts both have addressable IDs', async () => {
  const created=await post('/api/admin/accounts',{phone:'19900000016',role:'operator',password:original},token.super);
  assert.equal(created.status,200);
  const {id}=await created.json();
  assert.ok(id);
  assert.equal((await reset(id,token.super)).status,200);
  await env.DB.prepare("INSERT INTO users(phone,username,role,created_at) VALUES('19900000017','legacy','readonly',0)").run();
  const migration=readFileSync(join(__dirname,'../migrations/0009_admin_account_ids.sql'),'utf8');
  await env.DB.prepare(migration).run();
  const legacyId=await env.DB.prepare("SELECT id FROM users WHERE phone='19900000017'").first('id');
  assert.ok(legacyId);
  assert.equal((await env.DB.prepare(migration).run()).meta.changes,0);
  assert.equal((await reset(legacyId,token.super)).status,200);
  assert.equal((await post('/api/admin/login',{phone:'19900000017',password:resetPassword})).status,200);
});

test('failure to record the audit rolls back the password reset', async () => {
  const beforeHash=await stored('user');
  await env.DB.prepare("CREATE TRIGGER reject_reset_log BEFORE INSERT ON admin_audit_log WHEN NEW.action='RESET_USER_PASSWORD' BEGIN SELECT RAISE(ABORT,'audit unavailable'); END").run();
  try {
    await assert.rejects(reset('user',token.super),/audit unavailable/);
    assert.equal(await stored('user'),beforeHash);
  } finally {
    await env.DB.prepare('DROP TRIGGER reject_reset_log').run();
  }
});
