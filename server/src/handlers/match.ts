import { Env } from '../types';
import { getCorsHeaders } from '../helpers';

export async function handleMatchRoutes(request: Request, env: Env, path: string, url: URL): Promise<Response | null> {
    const corsHeaders = getCorsHeaders();

    if (path === '/api/match/join' && request.method === 'POST') {
        const body: { playerId: string } = await request.json();
        if (!body.playerId) return new Response(JSON.stringify({ error: 'Missing playerId' }), { status: 400, headers: corsHeaders });

        const playerId = body.playerId;
        const now = Date.now();
        await env.DB.prepare('DELETE FROM matchmaking_queue WHERE player_id = ?').bind(playerId).run();
        await env.DB.prepare('DELETE FROM matchmaking_queue WHERE joined_at < ? AND matched_game_id IS NULL').bind(now - 30000).run();

        const opponent = await env.DB.prepare('SELECT player_id FROM matchmaking_queue WHERE player_id != ? AND matched_game_id IS NULL AND joined_at >= ? ORDER BY joined_at ASC LIMIT 1').bind(playerId, now - 30000).first<{ player_id: string }>();

        if (opponent) {
            const gameId = `duel_${now}_${Math.random().toString(36).substring(2, 8)}`;
            const gameSeed = Math.floor(Math.random() * 1000000) + 1;
            const duelLevel = Math.floor(Math.random() * 7) + 8;

            await env.DB.prepare('UPDATE matchmaking_queue SET matched_game_id = ?, matched_opponent = ?, game_seed = ?, duel_level = ? WHERE player_id = ?').bind(gameId, playerId, gameSeed, duelLevel, opponent.player_id).run();
            await env.DB.prepare('INSERT INTO matchmaking_queue (player_id, joined_at, matched_game_id, matched_opponent, game_seed, duel_level) VALUES (?, ?, ?, ?, ?, ?)').bind(playerId, now, gameId, opponent.player_id, gameSeed, duelLevel).run();

            return new Response(JSON.stringify({ status: 'matched', gameId, opponentId: opponent.player_id, duelLevel, gameSeed }), { headers: corsHeaders });
        } else {
            await env.DB.prepare('INSERT INTO matchmaking_queue (player_id, joined_at, matched_game_id, matched_opponent, game_seed, duel_level) VALUES (?, ?, NULL, NULL, NULL, NULL)').bind(playerId, now).run();
            return new Response(JSON.stringify({ status: 'waiting', message: 'Waiting for opponent' }), { headers: corsHeaders });
        }
    }

    if (path === '/api/match/status' && request.method === 'GET') {
        const playerId = url.searchParams.get('playerId');
        if (!playerId) return new Response(JSON.stringify({ error: 'Missing playerId' }), { status: 400, headers: corsHeaders });

        const entry = await env.DB.prepare('SELECT matched_game_id, matched_opponent, game_seed, duel_level, joined_at FROM matchmaking_queue WHERE player_id = ?').bind(playerId).first<any>();
        if (!entry) return new Response(JSON.stringify({ status: 'not_in_queue' }), { headers: corsHeaders });
        if (entry.matched_game_id) return new Response(JSON.stringify({ status: 'matched', gameId: entry.matched_game_id, opponentId: entry.matched_opponent, duelLevel: entry.duel_level, gameSeed: entry.game_seed }), { headers: corsHeaders });

        const now = Date.now();
        const opponent = await env.DB.prepare('SELECT player_id FROM matchmaking_queue WHERE player_id != ? AND matched_game_id IS NULL AND joined_at >= ? ORDER BY joined_at ASC LIMIT 1').bind(playerId, now - 30000).first<{ player_id: string }>();

        if (opponent) {
            const gameId = `duel_${now}_${Math.random().toString(36).substring(2, 8)}`;
            const gameSeed = Math.floor(Math.random() * 1000000) + 1;
            const duelLevel = Math.floor(Math.random() * 7) + 8;

            const updateOpponentResult = await env.DB.prepare('UPDATE matchmaking_queue SET matched_game_id = ?, matched_opponent = ?, game_seed = ?, duel_level = ? WHERE player_id = ? AND matched_game_id IS NULL').bind(gameId, playerId, gameSeed, duelLevel, opponent.player_id).run();
            if (updateOpponentResult.meta.changes > 0) {
                await env.DB.prepare('UPDATE matchmaking_queue SET matched_game_id = ?, matched_opponent = ?, game_seed = ?, duel_level = ? WHERE player_id = ?').bind(gameId, opponent.player_id, gameSeed, duelLevel, playerId).run();
                return new Response(JSON.stringify({ status: 'matched', gameId, opponentId: opponent.player_id, duelLevel, gameSeed }), { headers: corsHeaders });
            }
        }

        if (Date.now() - entry.joined_at > 30000) {
            await env.DB.prepare('DELETE FROM matchmaking_queue WHERE player_id = ?').bind(playerId).run();
            return new Response(JSON.stringify({ status: 'not_in_queue' }), { headers: corsHeaders });
        }

        return new Response(JSON.stringify({ status: 'waiting' }), { headers: corsHeaders });
    }

    if (path === '/api/match/leave' && request.method === 'POST') {
        const body: { playerId: string } = await request.json();
        if (body.playerId) await env.DB.prepare('DELETE FROM matchmaking_queue WHERE player_id = ?').bind(body.playerId).run();
        return new Response(JSON.stringify({ status: 'left' }), { headers: corsHeaders });
    }

    return null;
}