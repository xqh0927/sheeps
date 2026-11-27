const { before, after, test } = require('node:test');
const assert = require('node:assert/strict');
const { once } = require('node:events');
const { execFileSync } = require('node:child_process');
const { mkdtempSync, rmSync } = require('node:fs');
const { tmpdir } = require('node:os');
const path = require('node:path');
const { setTimeout: delay } = require('node:timers/promises');
const { unstable_startWorker } = require('wrangler');

const root = path.resolve(__dirname, '..');
const wrangler = path.join(root, 'node_modules/wrangler/bin/wrangler.js');
let storage;
let worker;
let base;

function d1(...args) {
  return execFileSync(process.execPath, [wrangler, 'd1', 'execute', 'DB', '--local',
    '--persist-to', storage, ...args], { cwd: root, encoding: 'utf8' });
}

before(async () => {
  storage = mkdtempSync(path.join(tmpdir(), 'sheeps-duel-'));
  d1('--file', 'schema.sql');
  worker = await unstable_startWorker({
    config: path.join(root, 'wrangler.jsonc'),
    dev: {
      server: { hostname: '127.0.0.1', port: Number(process.env.DUEL_TEST_PORT || 0) },
      watch: false, inspector: false, persist: storage, logLevel: 'error',
    },
  });
  base = (await worker.url).origin;
});

after(async () => {
  try { await worker?.dispose(); }
  finally { if (storage) rmSync(storage, { recursive: true, force: true }); }
});

async function match(a, b) {
  const join = async playerId => {
    const response = await fetch(base + '/api/match/join', {
      method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ playerId }),
    });
    assert.equal(response.status, 200);
    return response.json();
  };
  assert.equal((await join(a)).status, 'waiting');
  const pair = await join(b);
  assert.equal(pair.opponentId, a);
  return pair.gameId;
}

async function connect(t, gameId, playerId) {
  const url = new URL('/api/ws', base);
  url.protocol = 'ws:';
  url.search = new URLSearchParams({ gameId, playerId });
  const socket = new WebSocket(url);
  t.after(async () => {
    if (socket.readyState === WebSocket.CLOSED) return;
    const closed = once(socket, 'close', { signal: AbortSignal.timeout(4000) });
    socket.close(1000);
    await closed;
  });
  const messages = [];
  socket.addEventListener('message', event => messages.push(JSON.parse(event.data)));
  await once(socket, 'open', { signal: AbortSignal.timeout(5000) });
  return { socket, messages };
}

async function waitForMessage(peer, name, target, timeout = 4000) {
  const deadline = Date.now() + timeout;
  while (Date.now() < deadline) {
    const message = peer.messages.find(cmd => cmd.payload?.systemMessage === name && cmd.payload.targetPlayerId === target);
    if (message) return message;
    await delay(30);
  }
  assert.fail('Timed out waiting for ' + name + ' for ' + target + '; received ' + JSON.stringify(peer.messages));
}

function storedCommands(gameId) {
  // gameId comes from the local matchmaking handler.
  assert.match(gameId, /^duel_[\w]+$/);
  const sql = `SELECT command_data FROM game_commands WHERE game_id='${gameId}' ORDER BY id`;
  return JSON.parse(d1('--command', sql, '--json'))[0].results.map(row => JSON.parse(row.command_data));
}

function countWins(gameId) {
  return storedCommands(gameId).filter(cmd => cmd.payload?.systemMessage === 'GAME_OVER_DISCONNECT_WIN').length;
}

test('remaining player receives one disconnect victory after 15 seconds', async t => {
  const gameId = await match('A', 'B');
  const b = await connect(t, gameId, 'B');
  const a = await connect(t, gameId, 'A');
  await waitForMessage(b, 'PLAYER_RECONNECTED', 'A');
  a.socket.close(1000);
  const disconnected = await waitForMessage(b, 'PLAYER_DISCONNECTED', 'A');
  const otherObserver = await connect(t, gameId, 'B');
  const win = await waitForMessage(b, 'GAME_OVER_DISCONNECT_WIN', 'A', 18500);
  assert.deepEqual(await waitForMessage(otherObserver, 'GAME_OVER_DISCONNECT_WIN', 'A'), win);
  assert.equal(win.senderId, 'SYSTEM');
  assert.equal(win.type, 'SYSTEM_EVENT');
  assert.ok(win.timestamp - disconnected.timestamp >= 15000);
  await delay(600);
  assert.equal(b.messages.filter(cmd => cmd.payload?.systemMessage === 'GAME_OVER_DISCONNECT_WIN').length, 1);
  const reopened = await connect(t, gameId, 'B');
  assert.deepEqual(await waitForMessage(reopened, 'GAME_OVER_DISCONNECT_WIN', 'A'), win);
  assert.equal(countWins(gameId), 1);
});

test('reconnecting within the grace period cancels the pending victory and keeps the relay usable', async t => {
  const gameId = await match('C', 'D');
  const d = await connect(t, gameId, 'D');
  const c = await connect(t, gameId, 'C');
  await waitForMessage(d, 'PLAYER_RECONNECTED', 'C');
  c.socket.close(1000);
  await waitForMessage(d, 'PLAYER_DISCONNECTED', 'C');
  d.messages.length = 0;
  const replacement = await connect(t, gameId, 'C');
  await waitForMessage(d, 'PLAYER_RECONNECTED', 'C');
  await delay(15500);
  const command = { gameId, senderId: 'C', type: 'ELIMINATE', seqId: 1, timestamp: Date.now(), payload: { comboCount: 2 } };
  const arrived = once(d.socket, 'message', { signal: AbortSignal.timeout(4000) });
  replacement.socket.send(JSON.stringify(command));
  assert.deepEqual(JSON.parse((await arrived)[0].data), command);
  assert.equal(countWins(gameId), 0);
});

test('closing an older socket after replacement keeps the player online', async t => {
  const gameId = await match('E', 'F');
  const f = await connect(t, gameId, 'F');
  const old = await connect(t, gameId, 'E');
  await waitForMessage(f, 'PLAYER_RECONNECTED', 'E');
  f.messages.length = 0;
  const current = await connect(t, gameId, 'E');
  await waitForMessage(f, 'PLAYER_RECONNECTED', 'E');
  old.socket.close(1000);
  await delay(700);
  assert.ok(!f.messages.some(cmd => cmd.payload?.systemMessage === 'PLAYER_DISCONNECTED'));
  current.socket.close(1000);
  await waitForMessage(f, 'PLAYER_DISCONNECTED', 'E');
});

test('both players closing releases the duel without awarding either one a win', async t => {
  const gameId = await match('G', 'H');
  const h = await connect(t, gameId, 'H');
  const g = await connect(t, gameId, 'G');
  await waitForMessage(h, 'PLAYER_RECONNECTED', 'G');
  g.socket.close(1000);
  await waitForMessage(h, 'PLAYER_DISCONNECTED', 'G');
  h.socket.close(1000);
  await delay(15750);
  assert.equal(countWins(gameId), 0);
});

test('closing after a normal victory preserves the completed result', async t => {
  const gameId = await match('I', 'J');
  const j = await connect(t, gameId, 'J');
  const i = await connect(t, gameId, 'I');
  await waitForMessage(j, 'PLAYER_RECONNECTED', 'I');
  i.socket.send(JSON.stringify({
    gameId, seqId: 1, timestamp: Date.now(), senderId: 'I', type: 'SYSTEM_EVENT', payload: { systemMessage: 'WIN' },
  }));
  await waitForMessage(j, 'WIN', undefined);
  i.socket.close(1000);
  await delay(600);
  assert.ok(!j.messages.some(cmd => cmd.payload?.systemMessage === 'PLAYER_DISCONNECTED'));
  assert.ok(!storedCommands(gameId).some(cmd => cmd.payload?.systemMessage === 'PLAYER_DISCONNECTED'));
});

for (const outcome of ['WIN', 'LOST']) {
  test(outcome + ' immediately after the last move settles the match exactly once', async t => {
    const sender = 'sender-' + outcome;
    const receiver = 'peer-' + outcome;
    const gameId = await match(sender, receiver);
    const peer = await connect(t, gameId, receiver);
    const player = await connect(t, gameId, sender);
    await waitForMessage(peer, 'PLAYER_RECONNECTED', sender);
    function send(seqId, type, payload) {
      player.socket.send(JSON.stringify({ gameId, seqId, timestamp: Date.now(), senderId: sender, type, payload }));
    }
    send(1, 'ELIMINATE', { tilesEliminated: ['one', 'two', 'three'] });
    send(2, 'ELIMINATE', { tilesEliminated: ['four', 'five', 'six'] });
    send(3, 'SYSTEM_EVENT', { systemMessage: outcome });
    send(4, 'SYSTEM_EVENT', { systemMessage: outcome });
    assert.equal((await waitForMessage(peer, outcome, undefined)).senderId, sender);
    const commands = storedCommands(gameId);
    assert.equal(commands.filter(c => c.type === 'ELIMINATE').length, 1);
    assert.equal(commands.filter(c => c.payload?.systemMessage === outcome).length, 1);
  });
}
