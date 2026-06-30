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

    // 每日签到
    if (path === '/api/sign/today' && request.method === 'POST') {
        const authUser = await getAuthenticatedUser(request, env);
        if (!authUser) return new Response(JSON.stringify({ error: 'Unauthorized' }), { status: 401, headers: corsHeaders });

        const chinaToday = new Date(Date.now() + 8 * 3600000).toISOString().split('T')[0];
        const chinaYesterday = new Date(Date.now() + 8 * 3600000 - 24 * 3600000).toISOString().split('T')[0];

        const [checkSignResult, lastSignResult, configResult, userResult] = await env.DB.batch([
            env.DB.prepare('SELECT 1 FROM sign_record WHERE user_id = ? AND sign_date = ?').bind(authUser.userId, chinaToday),
            env.DB.prepare('SELECT streak FROM sign_record WHERE user_id = ? AND sign_date = ?').bind(authUser.userId, chinaYesterday),
            env.DB.prepare('SELECT value FROM config WHERE key = ?').bind('sign_rewards'),
            env.DB.prepare('SELECT points FROM users WHERE id = ?').bind(authUser.userId)
        ]);

        if (checkSignResult.results[0]) return new Response(JSON.stringify({ error: 'Already signed in today' }), { status: 400, headers: corsHeaders });

        const newStreak = ((lastSignResult.results[0] as { streak: number } | undefined)?.streak || 0) + 1;
        const rewardsConfigRow = configResult.results[0] as { value: string } | undefined;
        const rewards = rewardsConfigRow ? rewardsConfigRow.value.split(',').map(Number) : [20, 20, 30, 30, 40, 50, 100];
        const rewardPoints = rewards[Math.min(newStreak, 7) - 1] || 20;

        const newPoints = ((userResult.results[0] as { points: number } | undefined)?.points || 0) + rewardPoints;

        await env.DB.batch([
            env.DB.prepare('UPDATE users SET points = ? WHERE id = ?').bind(newPoints, authUser.userId),
            env.DB.prepare('INSERT INTO sign_record (user_id, sign_date, streak, points_rewarded, created_at) VALUES (?, ?, ?, ?, ?)').bind(authUser.userId, chinaToday, newStreak, rewardPoints, Date.now()),
            env.DB.prepare('INSERT INTO point_record (user_id, type, amount, source, remaining_points, created_at) VALUES (?, ?, ?, ?, ?, ?)').bind(authUser.userId, 'IN', rewardPoints, 'SIGN_IN', newPoints, Date.now()),
            env.DB.prepare('INSERT OR REPLACE INTO user_task (user_id, task_id, task_date, progress, is_completed, is_rewarded) VALUES (?, ?, ?, ?, ?, ?)').bind(authUser.userId, 'SIGN_IN_ONCE', chinaToday, 1, 1, 0)
        ]);

        return new Response(JSON.stringify({ success: true, streak: newStreak, reward_points: rewardPoints, current_points: newPoints }), { headers: corsHeaders });
    }
    // 排行榜查询
    if (path === '/api/leaderboard' && request.method === 'GET') {
        const levelIdStr = url.searchParams.get('level_id');
        if (!levelIdStr) return new Response(JSON.stringify({ error: 'Missing level_id' }), { status: 400, headers: corsHeaders });

        const levelId = parseInt(levelIdStr, 10);
        const type = url.searchParams.get('type') || 'history';
        const page = parseInt(url.searchParams.get('page') || '1', 10);
        const limit = parseInt(url.searchParams.get('limit') || '20', 10);

        const cacheKey = `leaderboard_${levelId}_${type}_${page}_${limit}`;
        const cached = await env.SHEEPS_CACHE.get(cacheKey);
        if (cached) return new Response(cached, { headers: corsHeaders });

        const offset = (page - 1) * limit;
        let timeFilter = 0;
        const now = Date.now();
        const chinaTodayStr = new Date(now + 8 * 3600000).toISOString().split('T')[0];
        const todayStart = new Date(chinaTodayStr + 'T00:00:00+08:00').getTime();

        if (type === 'daily') timeFilter = todayStart;
        else if (type === 'weekly') timeFilter = todayStart - ((new Date(now + 8 * 3600000).getDay() + 6) % 7) * 24 * 3600000;

        const results = await env.DB.prepare(
            `SELECT username, avatar, clear_time_ms, score, achieved_at FROM (SELECT u.username, u.avatar, l.clear_time_ms, l.score, l.achieved_at, ROW_NUMBER() OVER (PARTITION BY l.user_id ORDER BY l.score DESC, l.clear_time_ms ASC) as rn FROM leaderboard l JOIN users u ON l.user_id = u.id WHERE l.level_id = ? AND l.achieved_at >= ?) WHERE rn = 1 ORDER BY score DESC, clear_time_ms ASC LIMIT ? OFFSET ?`
        ).bind(levelId, timeFilter, limit, offset).all();

        const responseData = JSON.stringify({ success: true, rankings: results.results });
        await env.SHEEPS_CACHE.put(cacheKey, responseData, { expirationTtl: 120 });
        return new Response(responseData, { headers: corsHeaders });
    }
    return null;
}