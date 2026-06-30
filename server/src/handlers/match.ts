import { Env } from '../types';
import { getCorsHeaders } from '../helpers';

/**
 * 处理多人实时对决匹配相关的 HTTP 路由请求
 * 包含玩家加入匹配队列、轮询匹配状态、自动超时清理（30秒上限）、以及中途离开队列。
 * 
 * @param request 客户端 HTTP 请求对象
 * @param env 环境变量及 D1 数据库实例
 * @param path 请求的 URL 路径
 * @param url 完整的 URL 解析对象（包含 Query 参数）
 * @return 匹配路由时返回 Response 对象，未匹配时返回 null
 */
export async function handleMatchRoutes(request: Request, env: Env, path: string, url: URL): Promise<Response | null> {
    const corsHeaders = getCorsHeaders();

    // 1. 玩家加入匹配队列接口
    if (path === '/api/match/join' && request.method === 'POST') {
        const body: { playerId: string } = await request.json();
        if (!body.playerId) return new Response(JSON.stringify({ error: 'Missing playerId' }), { status: 400, headers: corsHeaders });

        const playerId = body.playerId;
        const now = Date.now();
        // 清理当前玩家的历史匹配队列条目
        await env.DB.prepare('DELETE FROM matchmaking_queue WHERE player_id = ?').bind(playerId).run();
        // 清理超时的匹配条目 (超过 30 秒未成功配对的条目)
        await env.DB.prepare('DELETE FROM matchmaking_queue WHERE joined_at < ? AND matched_game_id IS NULL').bind(now - 30000).run();

        // 尝试寻找队列中的有效对手（入队时间在 30 秒内且未配对的其它玩家）
        const opponent = await env.DB.prepare('SELECT player_id FROM matchmaking_queue WHERE player_id != ? AND matched_game_id IS NULL AND joined_at >= ? ORDER BY joined_at ASC LIMIT 1').bind(playerId, now - 30000).first<{ player_id: string }>();

        if (opponent) {
            // 成功配对：生成全局唯一对局 GameId，LCG 同源关卡种子 gameSeed，并随机挑选 8~14 级作为对决关卡难度
            const gameId = `duel_${now}_${Math.random().toString(36).substring(2, 8)}`;
            const gameSeed = Math.floor(Math.random() * 1000000) + 1;
            const duelLevel = Math.floor(Math.random() * 7) + 8;

            // 原子地将对手的条目更新为配对成功，同时将当前玩家的条目也写为配对成功状态
            await env.DB.prepare('UPDATE matchmaking_queue SET matched_game_id = ?, matched_opponent = ?, game_seed = ?, duel_level = ? WHERE player_id = ?').bind(gameId, playerId, gameSeed, duelLevel, opponent.player_id).run();
            await env.DB.prepare('INSERT INTO matchmaking_queue (player_id, joined_at, matched_game_id, matched_opponent, game_seed, duel_level) VALUES (?, ?, ?, ?, ?, ?)').bind(playerId, now, gameId, opponent.player_id, gameSeed, duelLevel).run();

            return new Response(JSON.stringify({ status: 'matched', gameId, opponentId: opponent.player_id, duelLevel, gameSeed }), { headers: corsHeaders });
        } else {
            // 暂无对手，将自己置入等待队列
            await env.DB.prepare('INSERT INTO matchmaking_queue (player_id, joined_at, matched_game_id, matched_opponent, game_seed, duel_level) VALUES (?, ?, NULL, NULL, NULL, NULL)').bind(playerId, now).run();
            return new Response(JSON.stringify({ status: 'waiting', message: 'Waiting for opponent' }), { headers: corsHeaders });
        }
    }

    // 2. 轮询匹配状态接口
    if (path === '/api/match/status' && request.method === 'GET') {
        const playerId = url.searchParams.get('playerId');
        if (!playerId) return new Response(JSON.stringify({ error: 'Missing playerId' }), { status: 400, headers: corsHeaders });

        const entry = await env.DB.prepare('SELECT matched_game_id, matched_opponent, game_seed, duel_level, joined_at FROM matchmaking_queue WHERE player_id = ?').bind(playerId).first<any>();
        if (!entry) return new Response(JSON.stringify({ status: 'not_in_queue' }), { headers: corsHeaders });
        // 如果此前的玩家匹配逻辑已帮其完成了配对，直接返回配对成功信息
        if (entry.matched_game_id) return new Response(JSON.stringify({ status: 'matched', gameId: entry.matched_game_id, opponentId: entry.matched_opponent, duelLevel: entry.duel_level, gameSeed: entry.game_seed }), { headers: corsHeaders });

        // 否则自己作为轮询发起者，尝试拉取有无最新加入的其它等待玩家进行配对
        const now = Date.now();
        const opponent = await env.DB.prepare('SELECT player_id FROM matchmaking_queue WHERE player_id != ? AND matched_game_id IS NULL AND joined_at >= ? ORDER BY joined_at ASC LIMIT 1').bind(playerId, now - 30000).first<{ player_id: string }>();

        if (opponent) {
            const gameId = `duel_${now}_${Math.random().toString(36).substring(2, 8)}`;
            const gameSeed = Math.floor(Math.random() * 1000000) + 1;
            const duelLevel = Math.floor(Math.random() * 7) + 8;

            // 锁竞争防护：只有成功将对手状态修改成功的那个线程，才有权插入当前玩家的配对结果，避免并发导致两人各自连上不同对局
            const updateOpponentResult = await env.DB.prepare('UPDATE matchmaking_queue SET matched_game_id = ?, matched_opponent = ?, game_seed = ?, duel_level = ? WHERE player_id = ? AND matched_game_id IS NULL').bind(gameId, playerId, gameSeed, duelLevel, opponent.player_id).run();
            if (updateOpponentResult.meta.changes > 0) {
                await env.DB.prepare('UPDATE matchmaking_queue SET matched_game_id = ?, matched_opponent = ?, game_seed = ?, duel_level = ? WHERE player_id = ?').bind(gameId, opponent.player_id, gameSeed, duelLevel, playerId).run();
                return new Response(JSON.stringify({ status: 'matched', gameId, opponentId: opponent.player_id, duelLevel, gameSeed }), { headers: corsHeaders });
            }
        }

        // 30 秒匹配超时限制，超时自动移出队列并重置为未排队状态
        if (Date.now() - entry.joined_at > 30000) {
            await env.DB.prepare('DELETE FROM matchmaking_queue WHERE player_id = ?').bind(playerId).run();
            return new Response(JSON.stringify({ status: 'not_in_queue' }), { headers: corsHeaders });
        }

        return new Response(JSON.stringify({ status: 'waiting' }), { headers: corsHeaders });
    }

    // 3. 主动取消并离开匹配队列接口
    if (path === '/api/match/leave' && request.method === 'POST') {
        const body: { playerId: string } = await request.json();
        if (body.playerId) await env.DB.prepare('DELETE FROM matchmaking_queue WHERE player_id = ?').bind(body.playerId).run();
        return new Response(JSON.stringify({ status: 'left' }), { headers: corsHeaders });
    }

    return null;
}