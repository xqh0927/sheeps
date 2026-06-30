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
  const intervalId = setInterval(async () => {
    if (isClosed) {
      clearInterval(intervalId);
      return;
    }
    try {
      // 增量拉取对手发送的新指令
      const rows = await env.DB.prepare(
        'SELECT id, command_data FROM game_commands WHERE game_id = ? AND id > ? AND sender_id != ? ORDER BY id ASC'
      ).bind(gameId, lastReadId, playerId).all<{ id: number; command_data: string }>();

      // 推送给当前连接的客户端并更新游标
      for (const row of rows.results) {
        socket.send(row.command_data);
        lastReadId = Math.max(lastReadId, row.id);
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
  const handleDisconnect = async () => { /* 详细代码同前，此处主要负责清理定时器与判定掉线超时胜负 */ };
  socket.addEventListener('close', handleDisconnect);
  socket.addEventListener('error', handleDisconnect);
}