import { Env } from '../types';
import { getCorsHeaders, getAuthenticatedUser } from '../helpers';
import { generateSolvableLevel } from '../level';
import { sha256 } from '../crypto';

const CACHE_STAGE_CONFIG = new Map<number | string, string>();

/**
 * 处理所有与游戏业务相关的 HTTP 路由请求
 * 包括获取关卡布局数据、积分扣减解锁新关卡、通关成绩安全提交与校验、每日签到活动、以及多维度排行榜查询。
 * 
 * @param request 客户端 HTTP 请求对象
 * @param env 环境变量及 D1 数据库实例
 * @param path 请求的 URL 路径
 * @param url 完整的 URL 解析对象（包含 Query 参数）
 * @return 匹配路由时返回 Response 对象，未匹配时返回 null
 */
export async function handleGameRoutes(request: Request, env: Env, path: string, url: URL): Promise<Response | null> {
    const corsHeaders = getCorsHeaders();

    // 1. 获取关卡布局数据接口
    if (path === '/api/level' && request.method === 'GET') {
        const levelIdStr = url.searchParams.get('id');
        if (!levelIdStr) return new Response(JSON.stringify({ error: 'Missing level ID' }), { status: 400, headers: corsHeaders });
        const levelId = parseInt(levelIdStr, 10);
        if (isNaN(levelId)) return new Response(JSON.stringify({ error: 'Invalid level ID' }), { status: 400, headers: corsHeaders });

        const seedStr = url.searchParams.get('seed');
        // 若客户端未传种子，则在后端随机生成一个，保证离线与在线的关卡同源随机性
        const seed = seedStr ? parseInt(seedStr, 10) : Math.floor(Math.random() * 1000000) + 1;
        const cacheKey = `${levelId}_${seed}`;

        // 仅在传了确定性种子时读取内存缓存，加速金字塔卡牌布局获取
        if (seedStr && CACHE_STAGE_CONFIG.has(cacheKey)) return new Response(CACHE_STAGE_CONFIG.get(cacheKey)!, { headers: corsHeaders });

        // 调用确定性必定可解的关卡生成算法
        const newLayout = generateSolvableLevel(levelId, seed);
        const layoutJson = JSON.stringify(newLayout);
        if (seedStr) CACHE_STAGE_CONFIG.set(cacheKey, layoutJson);

        return new Response(layoutJson, { headers: corsHeaders });
    }

    // 2. 扣除积分以解锁新关卡接口
    if (path === '/api/level/unlock' && request.method === 'POST') {
        const authUser = await getAuthenticatedUser(request, env);
        if (!authUser) return new Response(JSON.stringify({ error: 'Unauthorized' }), { status: 401, headers: corsHeaders });

        const body: { level_id: number } = await request.json();
        const configKey = `level_${body.level_id}_unlock_points`;
        
        // 批量执行数据库事务：查询是否已解锁、读取后台配置解锁所需积分、读取用户当前积分
        const [unlockCheckResult, configResult, userResult] = await env.DB.batch([
            env.DB.prepare('SELECT 1 FROM level_unlock WHERE user_id = ? AND level_id = ?').bind(authUser.userId, body.level_id),
            env.DB.prepare('SELECT value FROM config WHERE key = ?').bind(configKey),
            env.DB.prepare('SELECT points FROM users WHERE id = ?').bind(authUser.userId)
        ]);

        const userRow = userResult.results[0] as any;
        // 如果已经解锁过了，直接返回成功，避免重复扣分
        if (unlockCheckResult.results.length > 0) return new Response(JSON.stringify({ success: true, current_points: userRow?.points || 0 }), { headers: corsHeaders });

        // 读取解锁配置开销，默认第 2 关扣 50 积分，其余扣 200 积分
        const cost = configResult.results[0] ? parseInt((configResult.results[0] as any).value, 10) : (body.level_id === 2 ? 50 : 200);
        if (!userRow || userRow.points < cost) return new Response(JSON.stringify({ error: 'Insufficient points' }), { status: 400, headers: corsHeaders });

        const newPoints = userRow.points - cost;
        // 事务写入：扣除积分、写入解锁记录、写入流水账明细
        await env.DB.batch([
            env.DB.prepare('UPDATE users SET points = ? WHERE id = ?').bind(newPoints, authUser.userId),
            env.DB.prepare('INSERT INTO level_unlock (user_id, level_id, unlocked_at) VALUES (?, ?, ?)').bind(authUser.userId, body.level_id, Date.now()),
            env.DB.prepare('INSERT INTO point_record (user_id, type, amount, source, remaining_points, created_at) VALUES (?, ?, ?, ?, ?, ?)').bind(authUser.userId, 'OUT', cost, `UNLOCK_LEVEL_${body.level_id}`, newPoints, Date.now())
        ]);

        return new Response(JSON.stringify({ success: true, current_points: newPoints }), { headers: corsHeaders });
    }

    // 3. 安全提交并校验通关成绩接口
    if (path === '/api/score/submit' && request.method === 'POST') {
        const body: any = await request.json();
        if (!body.sign) return new Response(JSON.stringify({ error: 'Missing parameters' }), { status: 400, headers: corsHeaders });

        // 防作弊校验：使用加盐 SHA256 算法校验签名合法性
        const expectedSign = await sha256(`${body.user_id}_${body.level_id}_${body.clear_time_ms}_folklore`);
        if (body.sign !== expectedSign) return new Response(JSON.stringify({ error: 'Invalid signature' }), { status: 403, headers: corsHeaders });

        const authUser = await getAuthenticatedUser(request, env);
        const resolvedUserId = authUser ? authUser.userId : body.user_id;

        const chinaToday = new Date(Date.now() + 8 * 3600000).toISOString().split('T')[0];
        // 批量查询：用户信息、此前该关卡的通关次数、当天的每日任务进度
        const [userResult, clearsResult, tasksResult] = await env.DB.batch([
            env.DB.prepare('SELECT points FROM users WHERE id = ?').bind(resolvedUserId),
            env.DB.prepare('SELECT COUNT(*) as count FROM leaderboard WHERE user_id = ? AND level_id = ?').bind(resolvedUserId, body.level_id),
            env.DB.prepare('SELECT ut.task_id, ut.progress, t.target_count, ut.is_completed FROM user_task ut JOIN task t ON ut.task_id = t.id WHERE ut.user_id = ? AND ut.task_date = ?').bind(resolvedUserId, chinaToday)
        ]);

        let userExists = userResult.results[0] as any;
        const mutations = [];

        // 如果用户在主表不存在（可能是游客网络异常直接结算），在此自动补全用户及解锁前 3 关
        if (!userExists) {
            mutations.push(env.DB.prepare('INSERT INTO users (id, username, points, created_at) VALUES (?, ?, 0, ?)').bind(resolvedUserId, `玩家_${resolvedUserId.slice(-4)}`, Date.now()));
            for (const lvl of [1, 2, 3]) mutations.push(env.DB.prepare('INSERT OR IGNORE INTO level_unlock (user_id, level_id, unlocked_at) VALUES (?, ?, ?)').bind(resolvedUserId, lvl, Date.now()));
            userExists = { points: 0 };
        }

        let firstClear = false, finalPoints = userExists.points;
        // 如果是该玩家的首次通关（无论离线还是在线），奖励 50 积分
        if (!(clearsResult.results[0] as any)?.count) {
            firstClear = true; finalPoints += 50;
            mutations.push(env.DB.prepare('UPDATE users SET points = ? WHERE id = ?').bind(finalPoints, resolvedUserId));
        }
        // 自动解锁下一关
        mutations.push(env.DB.prepare('INSERT OR IGNORE INTO level_unlock (user_id, level_id, unlocked_at) VALUES (?, ?, ?)').bind(resolvedUserId, body.level_id + 1, Date.now()));
        // 将成绩录入排行榜表
        mutations.push(env.DB.prepare('INSERT INTO leaderboard (user_id, level_id, score, clear_time_ms, achieved_at) VALUES (?, ?, ?, ?, ?)').bind(resolvedUserId, body.level_id, body.score, body.clear_time_ms, Date.now()));

        if (mutations.length > 0) await env.DB.batch(mutations);
        return new Response(JSON.stringify({ success: true, first_clear: firstClear, points_reward: firstClear ? 50 : 0 }), { headers: corsHeaders });
    }

    // 4. 每日签到接口
    if (path === '/api/sign/today' && request.method === 'POST') {
        const authUser = await getAuthenticatedUser(request, env);
        if (!authUser) return new Response(JSON.stringify({ error: 'Unauthorized' }), { status: 401, headers: corsHeaders });

        const chinaToday = new Date(Date.now() + 8 * 3600000).toISOString().split('T')[0];
        const chinaYesterday = new Date(Date.now() + 8 * 3600000 - 24 * 3600000).toISOString().split('T')[0];

        // 批量查询：当天签到情况、昨天连续签到天数、签到积分配置、当前用户积分
        const [checkSignResult, lastSignResult, configResult, userResult] = await env.DB.batch([
            env.DB.prepare('SELECT 1 FROM sign_record WHERE user_id = ? AND sign_date = ?').bind(authUser.userId, chinaToday),
            env.DB.prepare('SELECT streak FROM sign_record WHERE user_id = ? AND sign_date = ?').bind(authUser.userId, chinaYesterday),
            env.DB.prepare('SELECT value FROM config WHERE key = ?').bind('sign_rewards'),
            env.DB.prepare('SELECT points FROM users WHERE id = ?').bind(authUser.userId)
        ]);

        if (checkSignResult.results[0]) return new Response(JSON.stringify({ error: 'Already signed in today' }), { status: 400, headers: corsHeaders });

        // 连续签到天数 + 1
        const newStreak = ((lastSignResult.results[0] as { streak: number } | undefined)?.streak || 0) + 1;
        const rewardsConfigRow = configResult.results[0] as { value: string } | undefined;
        // 连签 7 天循环奖励规则，默认为：20、20、30、30、40、50、100
        const rewards = rewardsConfigRow ? rewardsConfigRow.value.split(',').map(Number) : [20, 20, 30, 30, 40, 50, 100];
        const rewardPoints = rewards[Math.min(newStreak, 7) - 1] || 20;

        const newPoints = ((userResult.results[0] as { points: number } | undefined)?.points || 0) + rewardPoints;

        // 签到事务原子入库
        await env.DB.batch([
            env.DB.prepare('UPDATE users SET points = ? WHERE id = ?').bind(newPoints, authUser.userId),
            env.DB.prepare('INSERT INTO sign_record (user_id, sign_date, streak, points_rewarded, created_at) VALUES (?, ?, ?, ?, ?)').bind(authUser.userId, chinaToday, newStreak, rewardPoints, Date.now()),
            env.DB.prepare('INSERT INTO point_record (user_id, type, amount, source, remaining_points, created_at) VALUES (?, ?, ?, ?, ?, ?)').bind(authUser.userId, 'IN', rewardPoints, 'SIGN_IN', newPoints, Date.now()),
            // 更新每日签到任务状态为已完成
            env.DB.prepare('INSERT OR REPLACE INTO user_task (user_id, task_id, task_date, progress, is_completed, is_rewarded) VALUES (?, ?, ?, ?, ?, ?)').bind(authUser.userId, 'SIGN_IN_ONCE', chinaToday, 1, 1, 0)
        ]);

        return new Response(JSON.stringify({ success: true, streak: newStreak, reward_points: rewardPoints, current_points: newPoints }), { headers: corsHeaders });
    }

    // 5. 排行榜列表查询接口
    if (path === '/api/leaderboard' && request.method === 'GET') {
        const levelIdStr = url.searchParams.get('level_id');
        if (!levelIdStr) return new Response(JSON.stringify({ error: 'Missing level_id' }), { status: 400, headers: corsHeaders });

        const levelId = parseInt(levelIdStr, 10);
        const type = url.searchParams.get('type') || 'history';
        const page = parseInt(url.searchParams.get('page') || '1', 10);
        const limit = parseInt(url.searchParams.get('limit') || '20', 10);

        const cacheKey = `leaderboard_${levelId}_${type}_${page}_${limit}`;
        // 读取 Cloudflare KV 缓存以优化并发响应率并降低 D1 读负荷
        const cached = await env.SHEEPS_CACHE.get(cacheKey);
        if (cached) return new Response(cached, { headers: corsHeaders });

        const offset = (page - 1) * limit;
        let timeFilter = 0;
        const now = Date.now();
        const chinaTodayStr = new Date(now + 8 * 3600000).toISOString().split('T')[0];
        const todayStart = new Date(chinaTodayStr + 'T00:00:00+08:00').getTime();

        if (type === 'daily') timeFilter = todayStart;
        else if (type === 'weekly') timeFilter = todayStart - ((new Date(now + 8 * 3600000).getDay() + 6) % 7) * 24 * 3600000;

        // 执行 SQL：按分数从高到低、通关耗时从短到长进行中国特色消消乐排行榜排序（限制每个用户只取个人的最高记录，防刷榜）
        const results = await env.DB.prepare(
            `SELECT username, avatar, clear_time_ms, score, achieved_at FROM (SELECT u.username, u.avatar, l.clear_time_ms, l.score, l.achieved_at, ROW_NUMBER() OVER (PARTITION BY l.user_id ORDER BY l.score DESC, l.clear_time_ms ASC) as rn FROM leaderboard l JOIN users u ON l.user_id = u.id WHERE l.level_id = ? AND l.achieved_at >= ?) WHERE rn = 1 ORDER BY score DESC, clear_time_ms ASC LIMIT ? OFFSET ?`
        ).bind(levelId, timeFilter, limit, offset).all();

        const responseData = JSON.stringify({ success: true, rankings: results.results });
        // KV 缓存 120 秒
        await env.SHEEPS_CACHE.put(cacheKey, responseData, { expirationTtl: 120 });
        return new Response(responseData, { headers: corsHeaders });
    }
    return null;
}