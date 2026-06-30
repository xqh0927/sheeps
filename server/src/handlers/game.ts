import { Env } from '../types';
import { getCorsHeaders, getAuthenticatedUser } from '../helpers';
import { generateSolvableLevel } from '../level';
import { sha256 } from '../crypto';

const CACHE_STAGE_CONFIG = new Map<number | string, string>();

export async function handleGameRoutes(request: Request, env: Env, path: string, url: URL): Promise<Response | null> {
    const corsHeaders = getCorsHeaders();

    if (path === '/api/level' && request.method === 'GET') {
        const levelIdStr = url.searchParams.get('id');
        if (!levelIdStr) return new Response(JSON.stringify({ error: 'Missing level ID' }), { status: 400, headers: corsHeaders });
        const levelId = parseInt(levelIdStr, 10);
        if (isNaN(levelId)) return new Response(JSON.stringify({ error: 'Invalid level ID' }), { status: 400, headers: corsHeaders });

        const seedStr = url.searchParams.get('seed');
        const seed = seedStr ? parseInt(seedStr, 10) : Math.floor(Math.random() * 1000000) + 1;
        const cacheKey = `${levelId}_${seed}`;

        if (seedStr && CACHE_STAGE_CONFIG.has(cacheKey)) return new Response(CACHE_STAGE_CONFIG.get(cacheKey)!, { headers: corsHeaders });

        const newLayout = generateSolvableLevel(levelId, seed);
        const layoutJson = JSON.stringify(newLayout);
        if (seedStr) CACHE_STAGE_CONFIG.set(cacheKey, layoutJson);

        return new Response(layoutJson, { headers: corsHeaders });
    }

    if (path === '/api/level/unlock' && request.method === 'POST') {
        const authUser = await getAuthenticatedUser(request, env);
        if (!authUser) return new Response(JSON.stringify({ error: 'Unauthorized' }), { status: 401, headers: corsHeaders });

        const body: { level_id: number } = await request.json();
        const configKey = `level_${body.level_id}_unlock_points`;
        const [unlockCheckResult, configResult, userResult] = await env.DB.batch([
            env.DB.prepare('SELECT 1 FROM level_unlock WHERE user_id = ? AND level_id = ?').bind(authUser.userId, body.level_id),
            env.DB.prepare('SELECT value FROM config WHERE key = ?').bind(configKey),
            env.DB.prepare('SELECT points FROM users WHERE id = ?').bind(authUser.userId)
        ]);

        const userRow = userResult.results[0] as any;
        if (unlockCheckResult.results.length > 0) return new Response(JSON.stringify({ success: true, current_points: userRow?.points || 0 }), { headers: corsHeaders });

        const cost = configResult.results[0] ? parseInt((configResult.results[0] as any).value, 10) : (body.level_id === 2 ? 50 : 200);
        if (!userRow || userRow.points < cost) return new Response(JSON.stringify({ error: 'Insufficient points' }), { status: 400, headers: corsHeaders });

        const newPoints = userRow.points - cost;
        await env.DB.batch([
            env.DB.prepare('UPDATE users SET points = ? WHERE id = ?').bind(newPoints, authUser.userId),
            env.DB.prepare('INSERT INTO level_unlock (user_id, level_id, unlocked_at) VALUES (?, ?, ?)').bind(authUser.userId, body.level_id, Date.now()),
            env.DB.prepare('INSERT INTO point_record (user_id, type, amount, source, remaining_points, created_at) VALUES (?, ?, ?, ?, ?, ?)').bind(authUser.userId, 'OUT', cost, `UNLOCK_LEVEL_${body.level_id}`, newPoints, Date.now())
        ]);

        return new Response(JSON.stringify({ success: true, current_points: newPoints }), { headers: corsHeaders });
    }

    if (path === '/api/score/submit' && request.method === 'POST') {
        const body: any = await request.json();
        if (!body.sign) return new Response(JSON.stringify({ error: 'Missing parameters' }), { status: 400, headers: corsHeaders });

        const expectedSign = await sha256(`${body.user_id}_${body.level_id}_${body.clear_time_ms}_folklore`);
        if (body.sign !== expectedSign) return new Response(JSON.stringify({ error: 'Invalid signature' }), { status: 403, headers: corsHeaders });

        const authUser = await getAuthenticatedUser(request, env);
        const resolvedUserId = authUser ? authUser.userId : body.user_id;

        const chinaToday = new Date(Date.now() + 8 * 3600000).toISOString().split('T')[0];
        const [userResult, clearsResult, tasksResult] = await env.DB.batch([
            env.DB.prepare('SELECT points FROM users WHERE id = ?').bind(resolvedUserId),
            env.DB.prepare('SELECT COUNT(*) as count FROM leaderboard WHERE user_id = ? AND level_id = ?').bind(resolvedUserId, body.level_id),
            env.DB.prepare('SELECT ut.task_id, ut.progress, t.target_count, ut.is_completed FROM user_task ut JOIN task t ON ut.task_id = t.id WHERE ut.user_id = ? AND ut.task_date = ?').bind(resolvedUserId, chinaToday)
        ]);

        let userExists = userResult.results[0] as any;
        const mutations = [];

        if (!userExists) {
            mutations.push(env.DB.prepare('INSERT INTO users (id, username, points, created_at) VALUES (?, ?, 0, ?)').bind(resolvedUserId, `玩家_${resolvedUserId.slice(-4)}`, Date.now()));
            for (const lvl of [1, 2, 3]) mutations.push(env.DB.prepare('INSERT OR IGNORE INTO level_unlock (user_id, level_id, unlocked_at) VALUES (?, ?, ?)').bind(resolvedUserId, lvl, Date.now()));
            userExists = { points: 0 };
        }

        let firstClear = false, finalPoints = userExists.points;
        if (!(clearsResult.results[0] as any)?.count) {
            firstClear = true; finalPoints += 50;
            mutations.push(env.DB.prepare('UPDATE users SET points = ? WHERE id = ?').bind(finalPoints, resolvedUserId));
        }
        mutations.push(env.DB.prepare('INSERT OR IGNORE INTO level_unlock (user_id, level_id, unlocked_at) VALUES (?, ?, ?)').bind(resolvedUserId, body.level_id + 1, Date.now()));
        mutations.push(env.DB.prepare('INSERT INTO leaderboard (user_id, level_id, score, clear_time_ms, achieved_at) VALUES (?, ?, ?, ?, ?)').bind(resolvedUserId, body.level_id, body.score, body.clear_time_ms, Date.now()));

        if (mutations.length > 0) await env.DB.batch(mutations);
        return new Response(JSON.stringify({ success: true, first_clear: firstClear, points_reward: firstClear ? 50 : 0 }), { headers: corsHeaders });
    }

    // /api/sign/today, /api/leaderboard 等其他 game 路由以此类推... (为节省篇幅略，结构一致)

    return null;
}