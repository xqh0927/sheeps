const { before, after, test } = require('node:test');
const assert = require('node:assert/strict');
const { Miniflare } = require('miniflare');
const { buildSync } = require('esbuild');
const { readFileSync } = require('node:fs');
const { join } = require('node:path');
const { unstable_splitSqlQuery } = require('wrangler');
const { generateJWT } = require('../.tmp-test/crypto');
let worker, db;
before(async () => {
  worker = new Miniflare({ modules: true, compatibilityDate: '2026-06-27',
    script: buildSync({entryPoints:[join(__dirname,'../src/index.ts')],bundle:true,format:'esm',write:false}).outputFiles[0].text,
    d1Databases: ['DB'], kvNamespaces: ['SHEEPS_CACHE'], r2Buckets: ['AVATAR_BUCKET'] });
  db = await worker.getD1Database('DB');
  await db.batch(unstable_splitSqlQuery(readFileSync(join(__dirname,'../schema.sql'),'utf8')).map(s=>db.prepare(s)));
  for (const id of ['alice','bob']) await db.prepare('INSERT INTO users(id,username,created_at) VALUES(?,?,?)').bind(id,id,Date.now()).run();
});
after(async () => worker?.dispose());
async function request(id, method) {
  const token = await generateJWT({userId:id,type:'access',exp:Date.now()+60000});
  return worker.dispatchFetch('http://localhost/api/user/deletion-request',{method,headers:{Authorization:'Bearer '+token}});
}
test('players can request, check and withdraw their own account deletion', async () => {
  const post = await request('alice','POST'); assert.equal(post.status,200);
  const first=await post.json();assert.ok(first.requested_at>0);
  assert.deepEqual(await (await request('alice','POST')).json(),first);
  assert.deepEqual(await (await request('alice','GET')).json(),first);
  assert.equal((await (await request('bob','GET')).json()).requested_at,null);
  assert.equal((await (await request('alice','DELETE')).json()).requested_at,null);
  assert.equal((await (await request('alice','GET')).json()).requested_at,null);
  assert.equal(await db.prepare('SELECT count(*) AS n FROM users').first('n'),2);
});

test('completed deletion ends the existing session, including token renewal', async () => {
  const id = 'removed-player';
  await db.prepare('INSERT INTO users(id,username,created_at) VALUES(?,?,?)').bind(id,id,Date.now()).run();
  const access = await generateJWT({userId:id,type:'access',exp:Date.now()+60000});
  const refresh = await generateJWT({userId:id,type:'refresh',exp:Date.now()+60000});
  await db.prepare('DELETE FROM users WHERE id=?').bind(id).run();
  const history = await worker.dispatchFetch('http://localhost/api/user/points-history', {headers:{Authorization:'Bearer '+access}});
  assert.equal(history.status,401);
  const renewal = await worker.dispatchFetch('http://localhost/api/auth/refresh', {method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({refreshToken:refresh})});
  assert.equal(renewal.status,401);
});
