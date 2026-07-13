import { Env } from './types';

/**
 * 处理 WebSocket 联机对战的会话逻辑
 * 核心原理：由于 Serverless 无状态，这里通过 D1 数据库轮询来实现双端消息同步
 */
export async function handleWebSocketSession(socket: WebSocket, gameId: string, playerId: string, env: Env) {
  const now = Date.now();

  // 1. 玩家上线：写入重连事件到 D1 数据库，通知对手
  try {
    await env.DB.prepare(
      'INSERT INTO game_commands (game_id, sender_id, command_data, created_at) VALUES (?, ?, ?, ?)'
    ).bind(
      gameId, 'SYSTEM',
      JSON.stringify({
        gameId, seqId: 0, timestamp: now, senderId: 'SYSTEM', type: 'SYSTEM_EVENT',
        payload: { systemMessage: 'PLAYER_RECONNECTED', targetPlayerId: playerId }
      }),
      now
    ).run();
  } catch (err) {
    console.error('写入重连事件失败:', err);
  }

  // 2. 初始化游标：获取当前数据库的最大指令 ID，防止读取历史旧消息
  let lastReadId = 0;
  try {
    const maxRow = await env.DB.prepare('SELECT MAX(id) as maxId FROM game_commands WHERE game_id = ?').bind(gameId).first<{ maxId: number | null }>();
    lastReadId = maxRow?.maxId || 0;
  } catch (err) {
    console.error('游标初始化失败:', err);
  }

  // 3. 开启轮询引擎 (250ms 间隔)
  let isClosed = false;
  // 断线时间戳：用于 15 秒宽限期内判定本方是否已重连，避免误判胜负
  let disconnectTime = 0;
  const GRACE_MS = 15000;
  const intervalId = setInterval(async () => {
    // 掉线宽限期内：仅做超时判负检查，不再向已关闭的 socket 推送指令
    if (isClosed) {
      if (Date.now() - disconnectTime >= GRACE_MS) {
        try {
          // 检查宽限期内本方是否已重连（PLAYER_RECONNECTED 事件的 targetPlayerId 指向本方）
          const rows = await env.DB.prepare(
            "SELECT command_data FROM game_commands WHERE game_id = ? AND sender_id = 'SYSTEM' AND created_at >= ? AND command_data LIKE ?"
          ).bind(gameId, disconnectTime, '%PLAYER_RECONNECTED%').all<{ command_data: string }>();
          let reconnected = false;
          for (const r of rows.results) {
            try {
              const cmd = JSON.parse(r.command_data);
              if (cmd?.payload?.targetPlayerId === playerId) { reconnected = true; break; }
            } catch { /* 解析失败忽略 */ }
          }
          if (!reconnected) {
            // 本方在宽限期内未重连：判本方负、对手胜，对手客户端收到后同步结束对局
            const t = Date.now();
            await env.DB.prepare(
              'INSERT INTO game_commands (game_id, sender_id, command_data, created_at) VALUES (?, ?, ?, ?)'
            ).bind(
              gameId, 'SYSTEM',
              JSON.stringify({
                gameId, seqId: 0, timestamp: t, senderId: 'SYSTEM', type: 'SYSTEM_EVENT',
                payload: { systemMessage: 'GAME_OVER_DISCONNECT_WIN', targetPlayerId: playerId }
              }),
              t
            ).run();
          }
        } catch (e) {
          console.error('掉线超时判负失败:', e);
        }
        clearInterval(intervalId);
      }
      return;
    }
    try {
      // 增量拉取对手发送的新指令
      const rows = await env.DB.prepare(
        'SELECT id, command_data FROM game_commands WHERE game_id = ? AND id > ? AND sender_id != ? ORDER BY id ASC'
      ).bind(gameId, lastReadId, playerId).all<{ id: number; command_data: string }>();

      // 推送给当前连接的客户端并更新游标
      for (const row of rows.results) {
        try {
          socket.send(row.command_data);
        } catch {
          // socket 已关闭：跳过推送，掉线判负由 handleDisconnect / 宽限期逻辑接管
        }
        lastReadId = Math.max(lastReadId, row.id);
      }

      // 清理过期的中继指令（1 小时前的对局必然已结束），避免 game_commands 表无限膨胀
      try {
        await env.DB.prepare('DELETE FROM game_commands WHERE created_at < ?').bind(Date.now() - 3600_000).run();
      } catch (e) {
        console.error('清理过期 game_commands 失败:', e);
      }
    } catch (err) {
      console.error('轮询指令出错:', err);
    }
  }, 250);

  // 4. 处理客户端发来的操作指令
  let lastActionTime = 0;
  socket.addEventListener('message', async (event) => {
    try {
      const text = typeof event.data === 'string' ? event.data : new TextDecoder().decode(event.data);
      const command = JSON.parse(text);
      const currentTime = Date.now();

      // 滑动窗口限流防刷 (100ms 内只能操作一次)
      if (currentTime - lastActionTime < 100) {
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
      await env.DB.prepare(
        'INSERT INTO game_commands (game_id, sender_id, command_data, created_at) VALUES (?, ?, ?, ?)'
      ).bind(gameId, playerId, JSON.stringify(command), currentTime).run();

    } catch (e) {
      console.error('WebSocket 消息处理错误', e);
    }
  });

  // 5. 优雅断线与掉线判负逻辑 (15秒宽限期)
  // 本方断开连接时：立即写入 PLAYER_DISCONNECTED 通知对手进入“等待重连”；
  // 15 秒宽限期内若本方未重连，则由轮询引擎写入 GAME_OVER_DISCONNECT_WIN 判本方负、对手胜，
  // 使对手客户端同步结束对局（见客户端 DuelCommandHandler.handleSystemEvent）。
  const handleDisconnect = async () => {
    if (isClosed) return;
    isClosed = true;
    disconnectTime = Date.now();
    try {
      await env.DB.prepare(
        'INSERT INTO game_commands (game_id, sender_id, command_data, created_at) VALUES (?, ?, ?, ?)'
      ).bind(
        gameId, 'SYSTEM',
        JSON.stringify({
          gameId, seqId: 0, timestamp: disconnectTime, senderId: 'SYSTEM', type: 'SYSTEM_EVENT',
          payload: { systemMessage: 'PLAYER_DISCONNECTED', targetPlayerId: playerId }
        }),
        disconnectTime
      ).run();
    } catch (err) {
      console.error('写入掉线事件失败:', err);
    }
  };
  socket.addEventListener('close', handleDisconnect);
  socket.addEventListener('error', handleDisconnect);
}