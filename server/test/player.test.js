const { before, after, test } = require('node:test');
const assert = require('node:assert/strict');
const { readFileSync } = require('node:fs');
const path = require('node:path');
const { Miniflare } = require('miniflare');
const { unstable_splitSqlQuery: splitSql } = require('wrangler');
const { generateJWT, sha256 } = require('../.tmp-test/crypto');
const { buildSync } = require('esbuild');

let runtime, env;
before(async () => {
  runtime = new Miniflare({
    modules: true, script: buildSync({entryPoints:[path.join(__dirname, '../src/index.ts')],
      bundle:true, format:'esm', write:false}).outputFiles[0].text,
    d1Databases: ['DB'], kvNamespaces: ['SHEEPS_CACHE'], r2Buckets: ['AVATAR_BUCKET'],
    compatibilityDate: '2026-06-27',
  });
  env = await runtime.getBindings();
  const schema = readFileSync(path.join(__dirname, '../schema.sql'), 'utf8');
  await env.DB.batch(splitSql(schema).map(sql => env.DB.prepare(sql)));
});
after(async () => runtime?.dispose());

async function player() {
  const id = crypto.randomUUID();
  await env.DB.prepare('INSERT INTO users (id, username, created_at) VALUES (?, ?, ?)').bind(id, 'test', Date.now()).run();
  const token = await generateJWT({userId: id, type:'access', exp: Date.now() + 60000});
  async function request(route, body, expectedStatus = 200) {
    const url = new URL(route, 'http://localhost');
    const res = await runtime.dispatchFetch(url.href, {
      method: body ? 'POST' : 'GET',
      headers: {Authorization: 'Bearer ' + token, 'Content-Type': 'application/json'},
      body: body ? JSON.stringify(body) : undefined,
    });
    assert.equal(res.status, expectedStatus);
    return res.json();
  }
  async function score(level, win, mode = 0, extra = {}) {
    const clearTime = 25000;
    const sign = await sha256([id, level, clearTime, 'folklore'].join('_'));
    return request(extra.run_id === undefined ? '/api/score/submit' : '/api/score/submit-once', { user_id:id, level_id:level, score:50,
      clear_time_ms:clearTime, game_mode:mode, is_win:win, sign, ...extra });
  }
  return { id, request, score };
}

test('profile counts only won stage levels', async () => {
  const p = await player();
  await p.score(1,1);
  await p.score(2,0);
  await p.score(8,1,1);
  assert.equal((await p.request('/api/user/profile')).highest_level_cleared, 1);
});

test('wins before the first daily task fetch count exactly once towards rewards', async () => {
  const p = await player();
  await p.score(1,1);
  await p.score(1,0);
  await p.score(1,1,1);
  const task = async () => (await p.request('/api/task/daily')).find(t=>t.task_id === 'WIN_3_GAMES');
  assert.equal((await task()).progress, 1);
  await Promise.all([p.score(1,1), p.score(1,1)]);
  assert.equal((await task()).is_completed, true);
  await p.request('/api/task/claim', {task_id:'WIN_3_GAMES'});
  await p.score(1,1);
  assert.equal((await task()).is_rewarded, true);
  assert.equal((await task()).progress, 3);
});

test('sign-in ledger records intermediate and final balances', async () => {
  const p = await player();
  await p.request('/api/sign/today', {});
  const records = await p.request('/api/user/points-history');
  assert.equal(records.find(r=>r.source==='SIGN_IN').remaining_points, 20);
  assert.equal(records.find(r=>r.source==='SIGN_IN_ONCE').remaining_points, 30);
});

test('avatar proxy serves actual R2 image type', async () => {
  const p = await player();
  await env.AVATAR_BUCKET.put('avatars/'+p.id+'_1.png', new Uint8Array([1,2,3]), {httpMetadata:{contentType:'image/png'}});
  const url = '/api/avatar/' + p.id;
  const result = await runtime.dispatchFetch('http://localhost' + url);
  assert.equal(result.headers.get('Content-Type'), 'image/png');
  assert.deepEqual([...new Uint8Array(await result.arrayBuffer())], [1,2,3]);
});

test('offline consumption deducts once from current stock across retries, concurrent sync and exchanges', async () => {
  const p = await player();
  await env.DB.prepare('INSERT INTO user_items VALUES (?, ?, ?)').bind(p.id, 'UNDO', 3).run();
  const op = {operation_id:crypto.randomUUID(), item_type:'UNDO', quantity:1};
  const sync = ops => p.request('/api/user/sync', {consumptions:ops});
  const stock = async () => (await p.request('/api/user/profile')).items.find(i=>i.item_type==='UNDO').count;
  const results = await Promise.all([sync([op]), sync([op])]);
  assert.equal(await stock(), 2);
  assert.deepEqual(results[0].acknowledged_consumptions, [op.operation_id]);
  await env.DB.prepare('UPDATE users SET points = 100 WHERE id = ?').bind(p.id).run();
  const item = await env.DB.prepare("SELECT id FROM shop_items WHERE item_type = 'UNDO'").first();
  const purchase = count => p.request('/api/shop/exchange', {shop_item_id:item.id, count});
  await Promise.all([purchase(2), sync([op])]);
  assert.equal(await stock(),4);
  const otherDevice = {...op, operation_id:crypto.randomUUID()};
  await Promise.all([purchase(1), sync([otherDevice])]);
  assert.equal(await stock(),4);
  const op2 = {...op,operation_id:crypto.randomUUID(),quantity:6};
  await sync([op2]);
  assert.equal(await stock(),0);
  const stale = await p.request('/api/user/sync', {items:[{item_type:'UNDO',count:4},{item_type:'HINT',count:1}]});
  assert.equal(stale.items.find(i=>i.item_type==='UNDO').count,0);
  assert.equal(stale.items.some(i=>i.item_type==='HINT'),false);
});

test('shop commits affordable orders at the current balance and replays an acknowledged receipt', async () => {
  const p = await player();
  await env.DB.prepare('UPDATE users SET points = 100 WHERE id = ?').bind(p.id).run();
  const item = await env.DB.prepare("SELECT id, stock FROM shop_items WHERE item_type = 'UNDO'").first();
  const exchange = (key, count = 1) => p.request('/api/shop/exchange', {shop_item_id:item.id, count, operation_id:key});
  const firstKey = crypto.randomUUID();
  const first = await exchange(firstKey);
  assert.equal(first.new_count, 1);
  // A completed request whose response the client could not retain is sent again.
  assert.deepEqual(await exchange(firstKey), first);
  const orders = await Promise.all([exchange(crypto.randomUUID()), exchange(crypto.randomUUID())]);
  assert.deepEqual(orders.map(r => r.remaining_points).sort((a,b) => a-b), [40,60]);
  assert.equal((await p.request('/api/user/profile')).user.points, 40);
  assert.equal((await p.request('/api/user/exchange-history')).length, 3);
  const current = await env.DB.prepare('SELECT stock FROM shop_items WHERE id = ?').bind(item.id).first();
  assert.equal(current.stock, item.stock - 3);
  assert.deepEqual(await exchange(firstKey), first);
});

test('shop rejects unavailable orders without recording a charge or consuming inventory', async () => {
  const p = await player();
  const item = await env.DB.prepare("SELECT id FROM shop_items WHERE item_type = 'HINT'").first();
  const request = {shop_item_id:item.id, count:1};
  await p.request('/api/shop/exchange', request, 400);
  await env.DB.prepare('UPDATE users SET points = 100 WHERE id = ?').bind(p.id).run();
  await env.DB.prepare('UPDATE shop_items SET stock = 0 WHERE id = ?').bind(item.id).run();
  await p.request('/api/shop/exchange', request, 400);
  assert.equal((await p.request('/api/user/exchange-history')).length,0);
  assert.equal((await p.request('/api/user/points-history')).length,0);
  assert.equal((await p.request('/api/user/profile')).user.points,100);
});

test('stage rankings count winning runs only while endless rankings count every run', async () => {
  const winner = await player(), other = await player();
  await winner.score(9, 1);
  await winner.score(9, 0);
  await other.score(9, 0);
  for (const type of ['daily', 'weekly', 'history']) {
    for (const perUser of ['best', 'all']) {
      const list = await winner.request(`/api/leaderboard?level_id=9&type=${type}&per_user=${perUser}`);
      assert.equal(list.total, 1);
      assert.equal(list.rankings.length, 1);
    }
  }
  await env.SHEEPS_CACHE.put('config_gamemode_endless', 'on');
  try {
    await winner.score(0, 0, 1);
    const endless = await winner.request('/api/leaderboard?level_id=0&game_mode=1');
    assert.equal(endless.total, 1);
    assert.equal(endless.rankings.length, 1);
  } finally {
    await env.SHEEPS_CACHE.delete('config_gamemode_endless');
  }
});

test('reconnected stages and a lost receipt yield three rankings, unlocks and one task reward', async () => {
  const p = await player();
  await env.DB.prepare('UPDATE users SET username = ? WHERE id = ?').bind('Offline winner', p.id).run();
  const runs = [1, 2, 3].map(() => crypto.randomUUID());
  const finished = Date.now() - 1000;
  const submit = i => p.score(i + 1, 1, 0, {run_id:runs[i], completed_at:finished});
  assert.equal((await submit(0)).run_id, runs[0]);
  await Promise.all([submit(0), submit(1), submit(2)]);
  const records = await env.DB.prepare('SELECT achieved_at FROM leaderboard WHERE user_id = ?').bind(p.id).all();
  assert.deepEqual(records.results.map(r => r.achieved_at), [finished, finished, finished]);
  for (const level of [1, 2, 3]) {
    const standings = await p.request('/api/leaderboard?type=history&per_user=all&limit=49&level_id=' + level);
    assert.equal(standings.rankings.filter(row => row.username === 'Offline winner').length, 1);
  }
  const profile = await p.request('/api/user/profile');
  assert.equal(profile.highest_level_cleared, 3);
  assert.ok(profile.unlocked_levels.includes(4));
  const tasks = () => p.request('/api/task/daily');
  assert.equal((await tasks()).find(t => t.task_id === 'WIN_3_GAMES').progress, 3);
  await p.request('/api/task/claim', {task_id:'WIN_3_GAMES'});
  await submit(2);
  await p.request('/api/task/claim', {task_id:'WIN_3_GAMES'}, 400);
  assert.equal((await p.request('/api/user/points-history')).length, 1);
});

test('auto-credited sign-in task uses its configured reward including zero', async () => {
  try {
    for (const amount of [25, 0]) {
      await env.DB.prepare("UPDATE task SET points_reward = ? WHERE id = 'SIGN_IN_ONCE'").bind(amount).run();
      const p = await player();
      const sign = await p.request('/api/sign/today', {});
      assert.equal(sign.reward_points, 20 + amount);
      assert.equal(sign.current_points, 20 + amount);
      await p.request('/api/sign/today', {}, 400);
      await p.request('/api/task/claim', {task_id:'SIGN_IN_ONCE'}, 400);
      assert.equal((await p.request('/api/user/profile')).user.points, 20 + amount);
      assert.equal((await p.request('/api/user/points-history')).find(r => r.source === 'SIGN_IN_ONCE').amount, amount);
    }
  } finally {
    await env.DB.prepare("UPDATE task SET points_reward = 10 WHERE id = 'SIGN_IN_ONCE'").run();
  }
});

test('an old client submitting after account deletion cannot recreate its player as a guest', async () => {
  const p = await player();
  await env.DB.prepare('DELETE FROM users WHERE id = ?').bind(p.id).run();
  const time = 25000;
  const sign = await sha256([p.id, 1, time, 'folklore'].join('_'));
  await p.request('/api/score/submit', {user_id:p.id, level_id:1, clear_time_ms:time, score:50, sign}, 401);
  assert.equal(await env.DB.prepare('SELECT id FROM users WHERE id = ?').bind(p.id).first(), null);
});

test('receipted endpoint rejects missing or empty run IDs without creating a score; legacy posts still work', async () => {
  const p = await player();
  const time = 17000;
  const sign = await sha256([p.id, 1, time, 'folklore'].join('_'));
  const score = {user_id:p.id, level_id:1, clear_time_ms:time, score:50, sign};
  await p.request('/api/score/submit-once', score, 400);
  await p.request('/api/score/submit-once', {...score, run_id:''}, 400);
  assert.equal((await env.DB.prepare('SELECT COUNT(*) AS n FROM leaderboard WHERE user_id = ?').bind(p.id).first()).n, 0);
  assert.deepEqual(await p.request('/api/score/submit', score), {success:true});
});
