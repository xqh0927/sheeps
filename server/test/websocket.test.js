/**
 * websocket.ts 回归测试 —— 重点覆盖 Bug 2 的修复路径：
 *   对手断线 → 我方（对手客户端）收到 GAME_OVER_DISCONNECT_WIN → 对局判负结束。
 *
 * 通过 mock 出 D1 的 game_commands 表（内存实现）与可控时钟（覆写 setInterval / Date.now），
 * 在无需真实 Cloudflare Worker 环境的情况下验证 handleDisconnect 的写入与轮询转发逻辑。
 *
 * 运行方式（见 README/QA 报告）：
 *   1) 编译源码： npx tsc src/websocket.ts src/types.ts --outDir .tmp-test --target ES2022 \
 *        --module commonjs --moduleResolution node --types @cloudflare/workers-types \
 *        --skipLibCheck --esModuleInterop --strict false
 *   2) 运行测试： node --test test/websocket.test.js
 *
 * 注意：本测试仅覆盖【服务端】行为（事件写入 + 轮询转发）。
 *       客户端 DuelCommandHandler 的守卫逻辑（targetPlayerId != state.playerId）属 Kotlin，
 *       在本环境无法编译，仅做静态审阅（见 QA 报告）。
 */
const { test } = require('node:test');
const assert = require('node:assert');
const { handleWebSocketSession } = require('../.tmp-test/websocket.js');

// ---------------------------------------------------------------------------
// 可控时钟：覆写全局 setInterval / clearInterval / Date.now，使其可在测试中手动驱动
// ---------------------------------------------------------------------------
let mockNow = 1_000_000;
let intervals = [];

function installClock() {
  mockNow = 1_000_000;
  intervals = [];
  global.Date.now = () => mockNow;
  global.setInterval = (cb) => {
    const id = { cb };
    intervals.push(id);
    return id;
  };
  global.clearInterval = (id) => {
    const i = intervals.indexOf(id);
    if (i >= 0) intervals.splice(i, 1);
  };
}

/** 驱动所有已注册的轮询回调各执行一次（await 其异步本体） */
async function tick() {
  for (const id of [...intervals]) {
    await id.cb();
  }
}

function advance(ms) {
  mockNow += ms;
}

const flush = () => new Promise((r) => setImmediate(r));

// ---------------------------------------------------------------------------
// Mock D1：内存版 game_commands 表，支持 INSERT / SELECT MAX(id) / SELECT 转发 / LIKE 重连查询 / DELETE
// ---------------------------------------------------------------------------
function makeDB() {
  const rows = [];
  let idCounter = 0;

  function runInsert(params) {
    // INSERT INTO game_commands (game_id, sender_id, command_data, created_at) VALUES (?, ?, ?, ?)
    const [game_id, sender_id, command_data, created_at] = params;
    const id = ++idCounter;
    rows.push({ id, game_id, sender_id, command_data, created_at });
    return { success: true, meta: { last_row_id: id } };
  }

  function query(sql, params) {
    if (sql.includes('MAX(id)')) {
      const game_id = params[0];
      const max = rows
        .filter((r) => r.game_id === game_id)
        .reduce((m, r) => Math.max(m, r.id), 0);
      return { results: [{ maxId: max || null }] };
    }
    if (sql.includes('DELETE')) {
      const threshold = params[0];
      const before = rows.length;
      for (let i = rows.length - 1; i >= 0; i--) {
        if (rows[i].created_at < threshold) rows.splice(i, 1);
      }
      return { success: true, changes: before - rows.length };
    }
    if (sql.includes('LIKE')) {
      // 重连查询：game_id, created_at >= ?, command_data LIKE '%PLAYER_RECONNECTED%'
      const game_id = params[0];
      const createdAtThreshold = params[1];
      return {
        results: rows.filter(
          (r) =>
            r.game_id === game_id &&
            r.sender_id === 'SYSTEM' &&
            r.created_at >= createdAtThreshold &&
            r.command_data.includes('PLAYER_RECONNECTED')
        ),
      };
    }
    // 转发查询：game_id, id > ?, sender_id != ?
    const game_id = params[0];
    const idThreshold = params[1];
    const senderId = params[2];
    const matched = rows
      .filter((r) => r.game_id === game_id && r.id > idThreshold && r.sender_id !== senderId)
      .sort((a, b) => a.id - b.id);
    return { results: matched };
  }

  return {
    rows,
    prepare(sql) {
      return {
        bind(...params) {
          return {
            run() {
              return runInsert(params);
            },
            all() {
              return query(sql, params);
            },
            first() {
              const res = query(sql, params);
              return res.results[0] || null;
            },
          };
        },
      };
    },
    // 测试辅助：模拟一次新会话写入的 PLAYER_RECONNECTED 事件
    insertReconnect(gameId, targetPlayerId, createdAt) {
      const id = ++idCounter;
      rows.push({
        id,
        game_id: gameId,
        sender_id: 'SYSTEM',
        command_data: JSON.stringify({
          gameId,
          seqId: 0,
          timestamp: createdAt,
          senderId: 'SYSTEM',
          type: 'SYSTEM_EVENT',
          payload: { systemMessage: 'PLAYER_RECONNECTED', targetPlayerId },
        }),
        created_at: createdAt,
      });
      return id;
    },
  };
}

// ---------------------------------------------------------------------------
// Mock WebSocket：记录 send 内容，可手动触发 close/error
// ---------------------------------------------------------------------------
function makeSocket() {
  const listeners = {};
  const sent = [];
  return {
    sent,
    addEventListener(type, handler) {
      (listeners[type] ||= []).push(handler);
    },
    send(data) {
      sent.push(data);
    },
    emit(type, event) {
      (listeners[type] || []).forEach((h) => h(event));
    },
  };
}

function parseAll(socket) {
  return socket.sent.map((s) => JSON.parse(s));
}

function findSystem(socket, systemMessage) {
  return parseAll(socket).find(
    (c) => c.payload && c.payload.systemMessage === systemMessage
  );
}

// ===========================================================================
// 测试用例
// ===========================================================================

test('Bug2-1: 玩家断线时 handleDisconnect 写入 PLAYER_DISCONNECTED 通知对手', async () => {
  installClock();
  const db = makeDB();
  const env = { DB: db };
  const socketA = makeSocket();

  await handleWebSocketSession(socketA, 'game1', 'A', env);
  socketA.emit('close', {});
  await flush();

  const disc = db.rows.find((r) => r.command_data.includes('PLAYER_DISCONNECTED'));
  assert.ok(disc, '应写入 PLAYER_DISCONNECTED 事件');
  const payload = JSON.parse(disc.command_data).payload;
  assert.strictEqual(payload.systemMessage, 'PLAYER_DISCONNECTED');
  assert.strictEqual(payload.targetPlayerId, 'A', 'targetPlayerId 应为断线方 A');
  assert.strictEqual(disc.sender_id, 'SYSTEM');
});

test('Bug2-2: 对手断线且超 15s 宽限期未重连 → 我方(对手)收到 GAME_OVER_DISCONNECT_WIN 判负', async () => {
  installClock();
  const db = makeDB();
  const env = { DB: db };
  const socketA = makeSocket(); // 断线方
  const socketB = makeSocket(); // 我方（应收到判负）

  await handleWebSocketSession(socketA, 'game1', 'A', env);
  await handleWebSocketSession(socketB, 'game1', 'B', env);

  // 对局进行一段时间后 A 断线
  advance(5000);
  socketA.emit('close', {});
  await flush();

  // 超过 15s 宽限期
  advance(16000);
  await tick();

  // 我方(B)应收到 GAME_OVER_DISCONNECT_WIN，且 targetPlayerId = 断线方 A
  const got = findSystem(socketB, 'GAME_OVER_DISCONNECT_WIN');
  assert.ok(got, '我方(B)应收到 GAME_OVER_DISCONNECT_WIN');
  assert.strictEqual(got.payload.targetPlayerId, 'A');

  // 数据库中也应存在该事件
  const go = db.rows.find((r) => r.command_data.includes('GAME_OVER_DISCONNECT_WIN'));
  assert.ok(go, '数据库应写入 GAME_OVER_DISCONNECT_WIN');
  assert.strictEqual(JSON.parse(go.command_data).payload.targetPlayerId, 'A');
});

test('Bug2-3: 断线方在宽限期内重连 → 不写入 GAME_OVER_DISCONNECT_WIN（重连取消判负）', async () => {
  installClock();
  const db = makeDB();
  const env = { DB: db };
  const socketA = makeSocket();

  await handleWebSocketSession(socketA, 'game1', 'A', env);
  advance(5000);
  socketA.emit('close', {});
  await flush();

  // 模拟 A 在宽限期内重连（新会话写入 PLAYER_RECONNECTED，时间戳 > disconnectTime）
  db.insertReconnect('game1', 'A', mockNow + 1000);

  advance(16000);
  await tick();

  const go = db.rows.find((r) => r.command_data.includes('GAME_OVER_DISCONNECT_WIN'));
  assert.strictEqual(go, undefined, '宽限期内重连后不应判负');
});

test('Bug2-4: 双方都断线 → 各自写入 GAME_OVER_DISCONNECT_WIN，且轮询不再向已断线方转发', async () => {
  installClock();
  const db = makeDB();
  const env = { DB: db };
  const socketA = makeSocket();
  const socketB = makeSocket();

  await handleWebSocketSession(socketA, 'game1', 'A', env);
  await handleWebSocketSession(socketB, 'game1', 'B', env);
  advance(5000);
  socketA.emit('close', {});
  socketB.emit('close', {});
  await flush();

  advance(16000);
  await tick();

  const goA = db.rows.find(
    (r) =>
      r.command_data.includes('GAME_OVER_DISCONNECT_WIN') &&
      JSON.parse(r.command_data).payload.targetPlayerId === 'A'
  );
  const goB = db.rows.find(
    (r) =>
      r.command_data.includes('GAME_OVER_DISCONNECT_WIN') &&
      JSON.parse(r.command_data).payload.targetPlayerId === 'B'
  );
  assert.ok(goA, 'A 断线应判 A 负');
  assert.ok(goB, 'B 断线应判 B 负');

  // 双方均已 closed，轮询处于“仅判负检查”分支，不再向 socket 转发指令
  assert.strictEqual(
    findSystem(socketA, 'GAME_OVER_DISCONNECT_WIN'),
    undefined,
    '已断线方 A 不应再收到转发'
  );
  assert.strictEqual(
    findSystem(socketB, 'GAME_OVER_DISCONNECT_WIN'),
    undefined,
    '已断线方 B 不应再收到转发'
  );
});

test('Bug2-5: GAME_OVER_DISCONNECT_WIN 的 targetPlayerId 指向断线方而非我方', async () => {
  // 校验事件语义正确：对手(B)收到事件后，客户端守卫用 targetPlayerId != state.playerId 判定，
  // 因此 targetPlayerId 必须是「断线方 A」，B 才不会误判自己获胜。
  installClock();
  const db = makeDB();
  const env = { DB: db };
  const socketA = makeSocket();
  const socketB = makeSocket();

  await handleWebSocketSession(socketA, 'game1', 'A', env);
  await handleWebSocketSession(socketB, 'game1', 'B', env);
  advance(5000);
  socketA.emit('close', {});
  await flush();
  advance(16000);
  await tick();

  const got = findSystem(socketB, 'GAME_OVER_DISCONNECT_WIN');
  assert.ok(got);
  assert.strictEqual(got.payload.targetPlayerId, 'A');
  assert.notStrictEqual(got.payload.targetPlayerId, 'B', 'targetPlayerId 不应是我方 B');
});
