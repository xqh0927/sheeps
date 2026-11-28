const { before, after, test } = require('node:test');
const assert = require('node:assert/strict');
const { readFileSync } = require('node:fs');
const path = require('node:path');
const { Miniflare } = require('miniflare');
const { unstable_splitSqlQuery: splitSql } = require('wrangler');
const { handleAuthRoutes } = require('../.tmp-test/handlers/auth');
const { handleUserRoutes } = require('../.tmp-test/handlers/user');

let runtime, db, kv;
before(async () => {
  runtime = new Miniflare({
    modules: true,
    script: 'export default {}',
    d1Databases: ['DB'],
    kvNamespaces: ['SHEEPS_CACHE'],
  });
  db = await runtime.getD1Database('DB');
  kv = await runtime.getKVNamespace('SHEEPS_CACHE');
  const schema = readFileSync(path.join(__dirname, '../schema.sql'), 'utf8');
  await db.batch(splitSql(schema).map(sql => db.prepare(sql)));
});
after(async () => runtime?.dispose());

const baseEnv = (extra = {}) => ({
  DB: db,
  SHEEPS_CACHE: kv,
  BIRD_API_KEY: 'bk_us1_test_key',
  EMAIL_SENDER: 'noreply@xqh.cc.cd',
  EMAIL_SENDER_NAME: '秘境消消乐',
  ...extra,
});

function authPost(route, body, env = baseEnv(), headers = {}) {
  const req = new Request('http://localhost' + route, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...headers },
    body: JSON.stringify(body),
  });
  return handleAuthRoutes(req, env, route);
}

function userPost(route, body, token, env = baseEnv()) {
  const req = new Request('http://localhost' + route, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Authorization: 'Bearer ' + token },
    body: JSON.stringify(body),
  });
  return handleUserRoutes(req, env, route);
}

test('unavailable Bird email config returns 503 without issuing a code', async () => {
  const res = await authPost('/api/auth/send-code', { email: 'no-config@example.com' }, baseEnv({ BIRD_API_KEY: '' }));
  assert.equal(res.status, 503);
  assert.equal(await db.prepare('SELECT count(*) AS n FROM email_verification_codes').first('n'), 0);
});

test('invalid email format is rejected with 400', async () => {
  const res = await authPost('/api/auth/send-code', { email: '13800000000' });
  assert.equal(res.status, 400);
});

test('Bird sends one verification email per cooldown, normalizes case, and omits code from response', async (t) => {
  let calls = 0;
  let sentCode = '';
  t.mock.method(globalThis, 'fetch', async (url, init) => {
    assert.equal(url, 'https://us1.platform.bird.com/v1/email/messages');
    assert.equal(init.headers.Authorization, 'Bearer bk_us1_test_key');
    const payload = JSON.parse(init.body);
    assert.equal(payload.from.email, 'noreply@xqh.cc.cd');
    assert.deepEqual(payload.to, ['player1@example.com']);
    const match = payload.text.match(/\b(\d{6})\b/);
    assert.ok(match, 'email body should contain 6-digit code');
    sentCode = match[1];
    calls++;
    return Response.json({ id: 'em_test_1' }, { status: 202 });
  });

  const pair = await Promise.all([
    authPost('/api/auth/send-code', { email: '  Player1@Example.COM ' }),
    authPost('/api/auth/send-code', { email: 'player1@example.com' }),
  ]);
  assert.deepEqual(pair.map(r => r.status).sort(), [200, 429]);
  assert.equal(calls, 1);
  const okBody = await pair.find(r => r.ok).json();
  assert.deepEqual(okBody, { success: true, code: '' });
  assert.match(sentCode, /^\d{6}$/);
});

test('failed Bird delivery rolls back reserved code and allows immediate retry', async (t) => {
  const email = 'retry@example.com';
  let attempt = 0;
  t.mock.method(globalThis, 'fetch', async () => {
    attempt++;
    return attempt === 1
      ? new Response('upstream error', { status: 500 })
      : Response.json({ id: 'em_test_2' }, { status: 202 });
  });

  const first = await authPost('/api/auth/send-code', { email });
  assert.equal(first.status, 502);
  assert.equal(await db.prepare('SELECT count(*) AS n FROM email_verification_codes WHERE email = ?').bind(email).first('n'), 0);

  const second = await authPost('/api/auth/send-code', { email });
  assert.equal(second.status, 200);
});

test('full email register, email/phone binding, and dual email/phone password login flow', async (t) => {
  let latestCode = '';
  t.mock.method(globalThis, 'fetch', async (_url, init) => {
    const payload = JSON.parse(init.body);
    latestCode = payload.text.match(/\b(\d{6})\b/)[1];
    return Response.json({ id: 'em_test_flow' }, { status: 202 });
  });

  // 1. Send code & register with email + password
  const sendRes = await authPost('/api/auth/send-code', { email: 'Hero@Example.com' });
  assert.equal(sendRes.status, 200);

  const regRes = await authPost('/api/auth/register', {
    email: 'hero@example.com',
    password: 'Password123',
    code: latestCode,
  });
  assert.equal(regRes.status, 200);
  const regData = await regRes.json();
  assert.equal(regData.success, true);
  assert.equal(regData.user.email, 'hero@example.com');
  assert.equal(regData.user.phone, null);

  // 2. Bind phone number without SMS
  const bindPhoneRes = await userPost('/api/user/bind-phone', { phone: '13812345678' }, regData.token);
  assert.equal(bindPhoneRes.status, 200);
  assert.deepEqual(await bindPhoneRes.json(), { success: true, phone: '13812345678' });

  // 3. Password login works with both email and bound phone
  const loginByEmail = await authPost('/api/auth/login-password', { account: 'HERO@example.com', password: 'Password123' });
  assert.equal(loginByEmail.status, 200);
  const emailLoginData = await loginByEmail.json();
  assert.equal(emailLoginData.user.id, regData.user.id);
  assert.equal(emailLoginData.user.phone, '13812345678');

  const loginByPhone = await authPost('/api/auth/login-password', { account: '13812345678', password: 'Password123' });
  assert.equal(loginByPhone.status, 200);
  assert.equal((await loginByPhone.json()).user.id, regData.user.id);

  // 4. Legacy phone-only user can bind email with verification code
  const legacyId = crypto.randomUUID();
  await db.prepare('INSERT INTO users (id, phone, username, created_at) VALUES (?, ?, ?, ?)')
    .bind(legacyId, '13900001111', '老玩家', Date.now()).run();
  const { generateJWT } = require('../.tmp-test/crypto');
  const legacyToken = await generateJWT({ userId: legacyId, phone: '13900001111', type: 'access', exp: Date.now() + 60000 });

  // Binding an already taken email returns 409
  await db.prepare('INSERT OR REPLACE INTO email_verification_codes(email, code, created_at) VALUES (?, ?, ?)')
    .bind('hero@example.com', '654321', Date.now()).run();
  const conflictRes = await userPost('/api/user/bind-email', { email: 'hero@example.com', code: '654321' }, legacyToken);
  assert.equal(conflictRes.status, 409);

  // Binding a fresh email succeeds
  await authPost('/api/auth/send-code', { email: 'legacy@example.com' });
  const bindEmailRes = await userPost('/api/user/bind-email', { email: 'legacy@example.com', code: latestCode }, legacyToken);
  assert.equal(bindEmailRes.status, 200);
  assert.deepEqual(await bindEmailRes.json(), { success: true, email: 'legacy@example.com' });

  // 5. Reset password via email verification code
  await db.prepare('INSERT OR REPLACE INTO email_verification_codes(email, code, created_at) VALUES (?, ?, ?)')
    .bind('hero@example.com', '112233', Date.now()).run();
  const resetRes = await authPost('/api/auth/reset-password', {
    email: 'hero@example.com',
    code: '112233',
    newPassword: 'NewPassword456',
  });
  assert.equal(resetRes.status, 200);

  const loginNewPw = await authPost('/api/auth/login-password', { account: '13812345678', password: 'NewPassword456' });
  assert.equal(loginNewPw.status, 200);
});

test('5 wrong verification codes invalidate the issued code', async () => {
  const email = 'bruteforce@example.com';
  await db.prepare('INSERT OR REPLACE INTO email_verification_codes(email, code, created_at) VALUES (?, ?, ?)')
    .bind(email, '998877', Date.now()).run();

  for (let i = 0; i < 4; i++) {
    const res = await authPost('/api/auth/login', { email, code: '000000' });
    assert.equal(res.status, 400);
  }
  // 5th wrong attempt invalidates and deletes the code
  const fifth = await authPost('/api/auth/login', { email, code: '000000' });
  assert.equal(fifth.status, 400);
  assert.match((await fifth.json()).error, /次数过多/);
  assert.equal(await db.prepare('SELECT count(*) AS n FROM email_verification_codes WHERE email = ?').bind(email).first('n'), 0);

  // Even the original valid code can no longer succeed
  const sixth = await authPost('/api/auth/login', { email, code: '998877' });
  assert.equal(sixth.status, 400);
});

test('5 wrong password login attempts lock the account with 429', async () => {
  const email = 'pwlock@example.com';
  await db.prepare('INSERT OR REPLACE INTO email_verification_codes(email, code, created_at) VALUES (?, ?, ?)')
    .bind(email, '123456', Date.now()).run();
  const reg = await authPost('/api/auth/register', { email, password: 'RightPassword1', code: '123456' });
  assert.equal(reg.status, 200);

  for (let i = 0; i < 5; i++) {
    const res = await authPost('/api/auth/login-password', { account: email, password: 'WrongPassword9' });
    assert.equal(res.status, 400);
  }
  const locked = await authPost('/api/auth/login-password', { account: email, password: 'RightPassword1' });
  assert.equal(locked.status, 429);
});

test('global daily email cap circuit-breaks sending across distinct emails and IPs', async (t) => {
  t.mock.method(globalThis, 'fetch', async () => Response.json({ id: 'em_cap' }, { status: 202 }));
  const envCap = baseEnv({ EMAIL_DAILY_LIMIT: '1' });
  // Previous tests already incremented the day bucket, so a limit of 1 must immediately trigger 429
  const blocked = await authPost('/api/auth/send-code', { email: 'cap-test@example.com' }, envCap, { 'CF-Connecting-IP': '198.51.100.77' });
  assert.equal(blocked.status, 429);
});
