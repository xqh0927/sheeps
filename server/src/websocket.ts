import { Env } from './types';

const GRACE_MS = 15_000;
const INSERT_EVENT = 'INSERT INTO game_commands (game_id, sender_id, command_data, created_at)';
const EVENT_NAME = "json_extract(command_data, '$.payload.systemMessage')";
const EVENT_TARGET = "json_extract(command_data, '$.payload.targetPlayerId')";
// Android sends these events when play ends before a disconnect deadline.
const NORMAL_RESULTS = ['WIN', 'LOST', 'OPPONENT_QUIT'];
const RESULT_FOR_GAME = `FROM game_commands WHERE game_id = ? AND (
  (sender_id = 'SYSTEM' AND ${EVENT_NAME} = 'GAME_OVER_DISCONNECT_WIN') OR
  (json_extract(command_data, '$.type') = 'SYSTEM_EVENT' AND ${EVENT_NAME} IN ('WIN', 'LOST', 'OPPONENT_QUIT')))`;
const HAS_RESULT = `SELECT 1 ${RESULT_FOR_GAME}`;
const NEWER_CONNECTION = `SELECT 1 FROM game_commands WHERE game_id = ? AND sender_id = 'SYSTEM'
  AND id > ? AND ${EVENT_NAME} = 'PLAYER_RECONNECTED' AND ${EVENT_TARGET} = ?`;

type RelayRow = { id: number; sender_id: string; command_data: string; created_at: number };

function systemEvent(gameId: string, systemMessage: string, targetPlayerId: string, timestamp: number) {
  return JSON.stringify({
    gameId, seqId: 0, timestamp, senderId: 'SYSTEM', type: 'SYSTEM_EVENT',
    payload: { systemMessage, targetPlayerId },
  });
}

/** Each live socket relays D1 commands and observes its opponent's reconnect deadline. */
export async function handleWebSocketSession(
  socket: WebSocket, gameId: string, playerId: string, env: Env, ctx: ExecutionContext,
) {
  const now = Date.now();
  const joined = await env.DB.prepare(`${INSERT_EVENT} SELECT ?, 'SYSTEM', ?, ? WHERE NOT EXISTS (${HAS_RESULT})`)
    .bind(gameId, systemEvent(gameId, 'PLAYER_RECONNECTED', playerId, now), now, gameId).run();
  if (!joined.meta.changes) {
    const result = await env.DB.prepare(`SELECT command_data ${RESULT_FOR_GAME} AND sender_id != ? ORDER BY id LIMIT 1`)
      .bind(gameId, playerId).first<{ command_data: string }>();
    if (result) socket.send(result.command_data);
    return;
  }

  const connectionId = joined.meta.last_row_id;
  let lastReadId = connectionId;
  let stopped = false;
  let polling = false;
  let pending: { id: number; player: string; deadline: number } | undefined;

  function observeSystemEvent(row: RelayRow) {
    const cmd = JSON.parse(row.command_data);
    if (cmd?.type !== 'SYSTEM_EVENT' || !cmd.payload) return false;
    const { systemMessage, targetPlayerId } = cmd.payload;
    if (NORMAL_RESULTS.includes(systemMessage)) return true;
    if (row.sender_id !== 'SYSTEM') return false;
    if (systemMessage === 'GAME_OVER_DISCONNECT_WIN') return true;
    if (targetPlayerId !== playerId) {
      if (systemMessage === 'PLAYER_DISCONNECTED' && !pending) {
        pending = { id: row.id, player: targetPlayerId, deadline: row.created_at + GRACE_MS };
      } else if (systemMessage === 'PLAYER_RECONNECTED' && pending && pending.player === targetPlayerId
          && row.created_at <= pending.deadline) {
        pending = undefined;
      }
    }
    return false;
  }

  // A reconnecting observer may have missed the other player's disconnect.
  const previous = await env.DB.prepare(`SELECT id, sender_id, command_data, created_at FROM game_commands
    WHERE game_id = ? AND id < ? AND sender_id = 'SYSTEM' AND ${EVENT_TARGET} != ?
    ORDER BY id DESC LIMIT 1`).bind(gameId, connectionId, playerId).first<RelayRow>();
  if (previous && previous.created_at + GRACE_MS >= now) {
    observeSystemEvent(previous);
  }

  const intervalId = setInterval(async () => {
    if (polling || stopped) return;
    polling = true;
    try {
      const rows = await env.DB.prepare(
        'SELECT id, sender_id, command_data, created_at FROM game_commands WHERE game_id = ? AND id > ? AND sender_id != ? ORDER BY id ASC'
      ).bind(gameId, lastReadId, playerId).all<RelayRow>();
      for (const row of rows.results) {
        if (stopped) return;
        socket.send(row.command_data);
        lastReadId = row.id;
        if (observeSystemEvent(row)) {
          stopPolling();
          return;
        }
      }

      const t = Date.now();
      if (!stopped && pending && t >= pending.deadline) {
        // The write checks both conditions atomically, including a reconnect racing this poll.
        await env.DB.prepare(`${INSERT_EVENT} SELECT ?, 'SYSTEM', ?, ?
          WHERE NOT EXISTS (${HAS_RESULT}) AND NOT EXISTS (${NEWER_CONNECTION} AND created_at <= ?)`)
          .bind(gameId, systemEvent(gameId, 'GAME_OVER_DISCONNECT_WIN', pending.player, t), t,
            gameId, gameId, pending.id, pending.player, pending.deadline).run();
        pending = undefined;
      }
    } catch (err) {
      if (!stopped) console.error('轮询指令出错:', err);
    } finally {
      polling = false;
    }
  }, 250);

  function stopPolling() {
    stopped = true;
    clearInterval(intervalId);
  }

  // 处理客户端发来的操作指令
  let lastActionTime = 0;
  let resultPending = false;
  socket.addEventListener('message', async (event) => {
    if (stopped) return;
    try {
      const text = typeof event.data === 'string' ? event.data : new TextDecoder().decode(event.data);
      const command = JSON.parse(text);
      const currentTime = Date.now();

      const normalResult = command.type === 'SYSTEM_EVENT' && NORMAL_RESULTS.includes(command.payload?.systemMessage);
      // A terminal event may follow the last move immediately. Admit a single pending result per socket;
      // the conditional insert below arbitrates results across both players.
      if (normalResult && resultPending) return;
      if (!normalResult && currentTime - lastActionTime < 100) {
        socket.send(JSON.stringify({
          gameId, seqId: command.seqId, timestamp: currentTime, senderId: 'SYSTEM',
          type: 'SYSTEM_EVENT', payload: { systemMessage: 'RATE_LIMIT_EXCEEDED' }
        }));
        return;
      }
      lastActionTime = currentTime;

      // 核心数值防作弊计算 (服务器端覆盖客户端伤害)
      if (command.type === 'ATTACK') {
        const combo = command.payload.comboCount || 1;
        const attackPower = 10.0 * (1.0 + Math.log(combo)); // 非线性伤害计算
        command.payload.attackPower = attackPower;
      }

      // 写入指令到 D1，对手的轮询引擎会自动拉取
      const args = [gameId, playerId, JSON.stringify(command), currentTime];
      if (normalResult) {
        resultPending = true;
        const result = await env.DB.prepare(`${INSERT_EVENT} SELECT ?, ?, ?, ? WHERE NOT EXISTS (${HAS_RESULT})`)
          .bind(...args, gameId).run().catch(error => {
            resultPending = false;
            throw error;
          });
        if (result.meta.changes) stopPolling();
      } else {
        await env.DB.prepare(`${INSERT_EVENT} VALUES (?, ?, ?, ?)`).bind(...args).run();
      }

    } catch (e) {
      console.error('WebSocket 消息处理错误', e);
    }
  });

  const handleDisconnect = () => {
    if (stopped) return;
    stopPolling();
    const t = Date.now();
    // Finish the presence write even when the HTTP upgrade request is being released.
    // A delayed close from a superseded socket must not disconnect its replacement.
    ctx.waitUntil(env.DB.prepare(`${INSERT_EVENT} SELECT ?, 'SYSTEM', ?, ?
      WHERE NOT EXISTS (${NEWER_CONNECTION}) AND NOT EXISTS (${HAS_RESULT})`)
      .bind(gameId, systemEvent(gameId, 'PLAYER_DISCONNECTED', playerId, t), t,
        gameId, connectionId, playerId, gameId).run()
      .catch(err => console.error('写入掉线事件失败:', err)));
  };
  socket.addEventListener('close', handleDisconnect);
  socket.addEventListener('error', handleDisconnect);

  // Prune when a session opens instead of scanning the whole table on every relay poll.
  try {
    await env.DB.prepare('DELETE FROM game_commands WHERE created_at < ?').bind(now - 3600_000).run();
  } catch (err) {
    console.error('清理过期 game_commands 失败:', err);
  }
}
