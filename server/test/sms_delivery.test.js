const { before, after, test } = require('node:test');
const assert = require('node:assert/strict');
const { Miniflare } = require('miniflare');
const { handleAuthRoutes } = require('../.tmp-test/handlers/auth');
let runtime, db;
before(async () => {
  runtime = new Miniflare({ modules: true, script: 'export default {}', d1Databases: ['DB'] });
  db = await runtime.getD1Database('DB');
  await db.prepare('CREATE TABLE login_token (phone TEXT PRIMARY KEY, code TEXT NOT NULL, created_at INTEGER NOT NULL)').run();
});
after(async () => runtime?.dispose());
function send(phone, sender, config = {}) {
  const path = '/api/auth/send-code';
  const req = new Request('http://localhost' + path, { method: 'POST', body: JSON.stringify({ phone }) });
  return handleAuthRoutes(req, { DB: db, SMS_DELIVERY: sender, ...config }, path);
}
test('an unavailable SMS service returns an error without issuing a code', async () => {
  const response = await send('19900000001');
  assert.equal(response.status, 503);
  assert.equal(await db.prepare('SELECT count(*) AS n FROM login_token').first('n'), 0);
});
test('one accepted delivery per cooldown and no verification secret in the client response', async () => {
  const phone = '19900000002';
  let calls = 0;
  const sender = { fetch: async (req) => {
    const message = await req.json();
    assert.equal(message.phone, phone);
    assert.match(message.code, /^\d{6}$/);
    calls++;
    return Response.json({ accepted: true });
  }};
  const pair = await Promise.all([send(phone, sender), send(phone, sender)]);
  assert.deepEqual(pair.map(r => r.status).sort(), [200, 429]);
  assert.equal(calls, 1);
  const body = await pair.find(r => r.ok).json();
  assert.equal(body.success, true);
  assert.equal(body.code || '', '');
});
test('rejected delivery drops its reserved code and allows a retry', async () => {
  const phone = '19900000003';
  const response = await send(phone, {fetch: async () => Response.json({ accepted: false })});
  assert.equal(response.status, 502);
  assert.equal(await db.prepare('SELECT count(*) AS n FROM login_token WHERE phone=?').bind(phone).first('n'), 0);
  assert.equal((await send(phone, {fetch: async () => Response.json({accepted:true})})).status, 200);
});

test('an HTTPS SMS endpoint can accept delivery without a provider SDK', async (t) => {
  const phone = '19900000004';
  const url = 'https://sms.example.test/messages';
  const post = t.mock.method(globalThis, 'fetch', async req => {
    assert.equal(req.url, url);
    assert.equal(req.headers.get('Authorization'), 'Bearer test-token');
    const message = await req.json();
    assert.equal(message.phone, phone);
    assert.match(message.code, /^\d{6}$/);
    return Response.json({ accepted: true });
  });
  assert.equal((await send(phone, undefined, {SMS_DELIVERY_URL: url})).status, 503);
  const response = await send(phone, undefined, {SMS_DELIVERY_URL: url, SMS_DELIVERY_TOKEN: 'test-token'});
  assert.deepEqual(await response.json(), { success: true, code: '' });
  assert.equal(post.mock.callCount(), 1);
});
