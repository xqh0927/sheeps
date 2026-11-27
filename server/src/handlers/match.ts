import { Env } from '../types';
import { getCorsHeaders } from '../helpers';

type QueueEntry = {
    matched_game_id: string | null;
    matched_opponent: string | null;
    duel_level: number | null;
    game_seed: number | null;
};

export async function handleMatchRoutes(request: Request, env: Env, path: string, url: URL): Promise<Response | null> {
    const join = path === '/api/match/join' && request.method === 'POST';
    const leave = path === '/api/match/leave' && request.method === 'POST';
    const poll = path === '/api/match/status' && request.method === 'GET';
    if (!join && !leave && !poll) return null;

    const headers = getCorsHeaders();
    const player = poll ? url.searchParams.get('playerId') : (await request.json<{playerId: string}>()).playerId;
    if (!player) {
        if (leave) return Response.json({status:'left'}, {headers});
        return Response.json({error:'Missing playerId'}, {status:400, headers});
    }

    const db = env.DB;
    if (leave) {
        await db.prepare(`DELETE FROM matchmaking_queue WHERE player_id=?1 OR matched_game_id IN (
            SELECT matched_game_id FROM matchmaking_queue WHERE player_id=?1)`).bind(player).run();
        return Response.json({status:'left'}, {headers});
    }

    const now = Date.now();
    const cutoff = now - 30000;
    const game = `duel_${now}_${crypto.randomUUID().replaceAll('-', '')}`;
    const statements = [db.prepare('DELETE FROM matchmaking_queue WHERE matched_game_id IS NULL AND joined_at < ?').bind(cutoff)];
    if (join) {
        // A retried join sees its existing room. A new search after a finished game (or an old
        // abandoned match) retires both previous entries before re-enqueuing this player.
        statements.push(db.prepare(`DELETE FROM matchmaking_queue WHERE matched_game_id IN (
            SELECT q.matched_game_id FROM matchmaking_queue q WHERE q.player_id=? AND
            (q.joined_at < ? OR EXISTS (SELECT 1 FROM game_commands c WHERE c.game_id=q.matched_game_id
              AND json_extract(c.command_data, '$.type')='SYSTEM_EVENT'
              AND json_extract(c.command_data, '$.payload.systemMessage') IN ('WIN','LOST','OPPONENT_QUIT','GAME_OVER_DISCONNECT_WIN')))
        )`).bind(player,cutoff),
        db.prepare('INSERT OR IGNORE INTO matchmaking_queue(player_id, joined_at) VALUES (?,?)').bind(player,now));
    }

    // Select the oldest waiting opponent and assign both halves inside one D1 transaction.
    // Polls may also pair waiting entries left by the previous Worker version.
    statements.push(
        db.prepare(`UPDATE matchmaking_queue SET matched_game_id=?1, matched_opponent=?2, game_seed=?3, duel_level=?4, joined_at=?5
            WHERE player_id=(SELECT player_id FROM matchmaking_queue WHERE player_id != ?2 AND matched_game_id IS NULL
                ORDER BY joined_at,player_id LIMIT 1)
            AND EXISTS (SELECT 1 FROM matchmaking_queue WHERE player_id=?2 AND matched_game_id IS NULL)`)
            .bind(game,player,1+Math.floor(Math.random()*1000000),8+Math.floor(Math.random()*7),now),
        db.prepare(`UPDATE matchmaking_queue AS self SET matched_game_id=other.matched_game_id, matched_opponent=other.player_id,
            game_seed=other.game_seed, duel_level=other.duel_level, joined_at=other.joined_at FROM matchmaking_queue AS other
            WHERE self.player_id=? AND other.matched_game_id=?`).bind(player,game),
        db.prepare('SELECT matched_game_id, matched_opponent, game_seed, duel_level FROM matchmaking_queue WHERE player_id=?').bind(player)
    );
    const results = await db.batch<QueueEntry>(statements);
    const row = results[results.length - 1].results[0];
    if (!row) return Response.json({status:'not_in_queue'}, {headers});
    if (!row.matched_game_id) return Response.json(join ? {status:'waiting', message:'Waiting for opponent'} : {status:'waiting'}, {headers});
    return Response.json({status:'matched', gameId:row.matched_game_id, opponentId:row.matched_opponent,
        duelLevel:row.duel_level, gameSeed:row.game_seed}, {headers});
}
