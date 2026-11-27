const { before, after, test } = require('node:test');
const assert = require('node:assert/strict');
const { randomUUID } = require('node:crypto');
const { readFileSync } = require('node:fs');
const { join } = require('node:path');
const { buildSync } = require('esbuild');
const { Miniflare } = require('miniflare');
const { unstable_splitSqlQuery: splitSql } = require('wrangler');
const { sha256 } = require('../.tmp-test/crypto');
let runtime, db, seq = 0;
const day = 86400000;
const chinaDate = ago => new Date(Date.now() + 8 * 3600000 - ago * day).toISOString().slice(0, 10);
before(async () => {
  const script = buildSync({entryPoints:[join(__dirname,'../src/index.ts')],bundle:true,format:'esm',write:false}).outputFiles[0].text;
  runtime = new Miniflare({compatibilityDate:'2026-06-27', workers:[
    {name:'api',modules:true,script,d1Databases:['DB'],kvNamespaces:['SHEEPS_CACHE'],serviceBindings:{SMS_DELIVERY:'sms'}},
    {name:'sms',modules:true,script:'export default {fetch() { return Response.json({accepted:true}); }}'}
  ]});
  db = await runtime.getD1Database('DB','api');
  await db.batch(splitSql(readFileSync(join(__dirname,'../schema.sql'),'utf8')).map(s=>db.prepare(s)));
});
after(async () => runtime?.dispose());
async function http(route, body, token) {
  const res = await runtime.dispatchFetch('http://localhost'+route,{method:body?'POST':'GET',
    headers:{'Content-Type':'application/json',...(token?{Authorization:'Bearer '+token}:{})},body:body?JSON.stringify(body):undefined});
  return {status:res.status,data:await res.json()};
}
async function ok(...args) {
  const r = await http(...args);
  assert.equal(r.status, 200, JSON.stringify(r.data));
  return r.data;
}
async function code(phone) {
  await db.prepare('INSERT OR REPLACE INTO login_token VALUES (?,?,?)').bind(phone,'543210',Date.now()).run();
  return '543210';
}
const phoneNumber = () => '1990000'+String(++seq).padStart(4,'0');
async function register(phone=phoneNumber(), guest) {
  return ok('/api/auth/register',{phone,password:'local-test-123',code:await code(phone),device_uuid:guest});
}
const profile = account=>ok('/api/user/profile',null,account.token);
test('workerd delivers a verification request to the configured service', async () => {
  const phone = phoneNumber();
  assert.equal((await ok('/api/auth/send-code',{phone})).success,true);
  assert.match((await db.prepare('SELECT code FROM login_token WHERE phone=?').bind(phone).first()).code,/^\d{6}$/);
});
for (const method of ['register','login-password','login']) {
  test(method+' preserves guest wins, legacy scores, starts and retry receipts', async () => {
    const guest = 'uuid-'+randomUUID(), run = randomUUID(), time = 30000, phone = phoneNumber();
    const body = {user_id:guest,level_id:1,clear_time_ms:time,score:60,is_win:1,run_id:run,completed_at:Date.now(),
      sign:await sha256([guest,1,time,'folklore'].join('_'))};
    await ok('/api/level/attempt',{user_id:guest,level_id:1,run_id:run,layout_version:'generated-v5',started_at:Date.now()-time});
    await ok('/api/score/submit-once',body);
    await ok('/api/score/submit',{...body,run_id:undefined});
    let account;
    if (method==='register') account=await register(phone,guest);
    else if (method==='login') account=await ok('/api/auth/login',{phone,device_uuid:guest,code:await code(phone)});
    else {
      await register(phone);
      account=await ok('/api/auth/login-password',{phone,password:'local-test-123',device_uuid:guest});
    }
    const id=account.user.id;
    assert.equal((await profile(account)).highest_level_cleared,1);
    assert.ok(account.unlocked_levels.includes(2));
    await ok('/api/score/submit-once',{...body,user_id:id,sign:await sha256([id,1,time,'folklore'].join('_'))},account.token);
    for(const [table,n] of [['leaderboard',2],['stage_attempts',1],['stage_run_scores',1],['stage_score_receipts',1]]) {
      assert.equal(await db.prepare('SELECT COUNT(*) AS n FROM '+table+' WHERE user_id=?').bind(id).first('n'),n,table);
    }
    assert.equal(await db.prepare('SELECT id FROM users WHERE id=?').bind(guest).first(),null);
  });
}
test('concurrent join and polling leave each player in exactly one reciprocal room', async () => {
  const ids=Array.from({length:9},()=>randomUUID());
  try {
    await ok('/api/match/join',{playerId:ids[0]});
    await Promise.all(ids.slice(1).map(playerId=>ok('/api/match/join',{playerId})));
    const poll = id=>ok('/api/match/status?playerId='+id);
    const states=await Promise.all(ids.map(poll));
    assert.equal(states.filter(r=>r.status==='waiting').length,1);
    for (let i=0;i<ids.length;i++) if (states[i].status==='matched') {
      const other=states[ids.indexOf(states[i].opponentId)];
      assert.equal(other.gameId,states[i].gameId); assert.equal(other.opponentId,ids[i]);
    }
  } finally { await Promise.all(ids.map(playerId=>ok('/api/match/leave',{playerId}))); }
});
test('permanent skins are charged once across distinct orders and cannot be bought in bulk', async () => {
  const account=await register(), id=account.user.id;
  await db.prepare('UPDATE users SET points=600 WHERE id=?').bind(id).run();
  const buy=(count,key)=>http('/api/shop/exchange',{shop_item_id:1000,count,operation_id:key},account.token);
  assert.equal((await buy(2,randomUUID())).status,400);
  const keys=[randomUUID(),randomUUID()];
  const sales=await Promise.all(keys.map(k=>buy(1,k)));
  assert.deepEqual(sales.map(r=>r.status).sort(),[200,409]);
  assert.equal(sales.find(r=>r.status===409).data.error,'Skin already owned');
  const replayKey=keys[sales.findIndex(r=>r.status===200)];
  assert.equal((await buy(1,replayKey)).status,200);
  const p=await profile(account);
  assert.equal(p.user.points,400); assert.equal(p.items.find(i=>i.item_type==='SKIN_HENAN').count,1);
  assert.equal((await ok('/api/user/exchange-history',null,account.token)).length,1);
});
test('streak reward wraps on day eight and all login surfaces reset after a missed day', async () => {
  const phone=phoneNumber(), account=await register(phone), id=account.user.id;
  await db.prepare('INSERT INTO sign_record VALUES(?,?,?,?,?)').bind(id,chinaDate(2),6,50,Date.now()-2*day).run();
  assert.equal((await profile(account)).sign_streak,0);
  assert.equal((await ok('/api/auth/login-password',{phone,password:'local-test-123'})).sign_streak,0);
  assert.equal((await ok('/api/auth/login',{phone,code:await code(phone)})).sign_streak,0);
  await db.prepare('INSERT INTO sign_record VALUES(?,?,?,?,?)').bind(id,chinaDate(1),7,100,Date.now()-day).run();
  assert.equal((await profile(account)).sign_streak,7);
  const signed=await ok('/api/sign/today',{},account.token);
  assert.equal(signed.streak,8);assert.equal(signed.reward_points,30);
});
test('a zero-reward task is claimed only once by two devices', async () => {
  const a=await register(), id=a.user.id;
  await db.prepare("INSERT INTO task VALUES ('WIN_TEST_ZERO','Zero','Zero',1,0)").run();
  await db.prepare("INSERT INTO user_task VALUES (?,'WIN_TEST_ZERO',?,1,1,0)").bind(id,chinaDate(0)).run();
  const claim=()=>http('/api/task/claim',{task_id:'WIN_TEST_ZERO'},a.token);
  assert.deepEqual((await Promise.all([claim(),claim()])).map(r=>r.status).sort(),[200,400]);
  assert.equal((await profile(a)).user.points,0);
  assert.deepEqual((await ok('/api/user/points-history',null,a.token)).map(r=>r.amount),[0]);
});
test('zero-score players listed in the daily popup have matching personal ranks, including ties', async () => {
  const players = [await register(), await register()];
  for (const account of players) {
    await db.prepare('INSERT INTO leaderboard(user_id,level_id,score,clear_time_ms,game_mode,is_win,achieved_at) VALUES(?,1,0,1000,0,0,?)')
      .bind(account.user.id, Date.now() - day).run();
  }
  for (const account of players) {
    const popup = await ok('/api/leaderboard/daily-popup', null, account.token);
    const position = popup.top3.findIndex(row => row.username === account.user.username) + 1;
    assert.ok(position > 0);
    assert.equal(popup.yesterdayRank, position);
  }
});

test('two logins merge existing guest balances, inventory and backups once', async () => {
  const phone = phoneNumber();
  const account = await register(phone);
  const guest = 'uuid-' + randomUUID();
  const target = account.user.id;
  await db.prepare('INSERT INTO users(id,username,points,created_at) VALUES(?,?,?,?)')
    .bind(guest, 'Legacy guest', 70, Date.now()).run();
  await db.prepare('INSERT INTO user_items VALUES(?,?,?)').bind(guest,'UNDO',2).run();
  const backup = await db.prepare('INSERT INTO backup_save_log(user_id,points,created_at) VALUES(?,?,?)')
    .bind(guest,70,Date.now()).run();
  await db.prepare('INSERT INTO backup_save_items VALUES(?,?,?)').bind(backup.meta.last_row_id,'UNDO',2).run();
  const login = () => ok('/api/auth/login-password', {phone,password:'local-test-123',device_uuid:guest});
  const results = await Promise.all([login(),login()]);
  assert.deepEqual(results.map(r=>r.user.points),[70,70]);
  assert.deepEqual(results.map(r=>r.items.find(i=>i.item_type==='UNDO').count),[3,3]);
  assert.equal(await db.prepare('SELECT user_id FROM backup_save_log WHERE id=?').bind(backup.meta.last_row_id).first('user_id'),target);
  assert.equal(await db.prepare('SELECT count FROM backup_save_items WHERE backup_id=?').bind(backup.meta.last_row_id).first('count'),2);
});
