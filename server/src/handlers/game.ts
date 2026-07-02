import { Env } from '../types';
import { getCorsHeaders, getAuthenticatedUser, getCachedConfig } from '../helpers';
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

        // 仅在传了确定性种子时读缓存，随机种子每次都不一样无需缓存
        if (seedStr) {
            // 优先查内存缓存（最快）
            if (CACHE_STAGE_CONFIG.has(cacheKey))
                return new Response(CACHE_STAGE_CONFIG.get(cacheKey)!, { headers: corsHeaders });

            // 其次查 KV 持久化缓存（跨 Worker 实例共享，冷启动也能命中）
            const kvCached = await env.SHEEPS_CACHE.get(cacheKey);
            if (kvCached) {
                // 回填内存缓存
                CACHE_STAGE_CONFIG.set(cacheKey, kvCached);
                return new Response(kvCached, { headers: corsHeaders });
            }
        }

        // 获取用户 ID：优先使用认证用户，其次查询参数，最后默认 0
        let userId = 0;
        const authUser = await getAuthenticatedUser(request, env);
        if (authUser) {
          const parsed = parseInt(authUser.userId, 10);
          if (!isNaN(parsed)) userId = parsed;
        } else {
          const userIdStr = url.searchParams.get('userId');
          if (userIdStr) {
            const parsed = parseInt(userIdStr, 10);
            if (!isNaN(parsed)) userId = parsed;
          }
        }

        // 调用确定性必定可解的关卡生成算法
        const newLayout = generateSolvableLevel(userId, levelId, seed);
        const layoutJson = JSON.stringify(newLayout);
        if (seedStr) {
            // 同时写入内存缓存和 KV 缓存
            CACHE_STAGE_CONFIG.set(cacheKey, layoutJson);
            // 异步写入 KV，不阻塞响应（et.catch 防止 unhandled rejection）
            env.SHEEPS_CACHE.put(cacheKey, layoutJson, { expirationTtl: 86400 }).catch(() => {});
        }

        return new Response(layoutJson, { headers: corsHeaders });
    }

    // 2. 扣除积分以解锁新关卡接口
    if (path === '/api/level/unlock' && request.method === 'POST') {
        const authUser = await getAuthenticatedUser(request, env);
        if (!authUser) return new Response(JSON.stringify({ error: 'Unauthorized' }), { status: 401, headers: corsHeaders });

        const body: { level_id: number } = await request.json();
        
        // 从 KV 缓存读取解锁积分配置（避免 D1 查询）
        const configKey = `level_${body.level_id}_unlock_points`;
        const costStr = await getCachedConfig(env, configKey, body.level_id === 2 ? '50' : '200');
        const cost = parseInt(costStr, 10);
        
        // 批量执行数据库事务：查询是否已解锁、读取用户当前积分
        const [unlockCheckResult, userResult] = await env.DB.batch([
            env.DB.prepare('SELECT 1 FROM level_unlock WHERE user_id = ? AND level_id = ?').bind(authUser.userId, body.level_id),
            env.DB.prepare('SELECT points FROM users WHERE id = ?').bind(authUser.userId)
        ]);

        const userRow = userResult.results[0] as any;
        // 如果已经解锁过了，直接返回成功，避免重复扣分
        if (unlockCheckResult.results.length > 0) return new Response(JSON.stringify({ success: true, current_points: userRow?.points || 0 }), { headers: corsHeaders });

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

        // 从 KV 缓存读取签到奖励配置
        const rewardsStr = await getCachedConfig(env, 'sign_rewards', '20,20,30,30,40,50,100');
        const rewards = rewardsStr.split(',').map(Number);
        
        // 批量查询：当天签到情况、昨天连续签到天数、当前用户积分
        const [checkSignResult, lastSignResult, userResult] = await env.DB.batch([
            env.DB.prepare('SELECT 1 FROM sign_record WHERE user_id = ? AND sign_date = ?').bind(authUser.userId, chinaToday),
            env.DB.prepare('SELECT streak FROM sign_record WHERE user_id = ? AND sign_date = ?').bind(authUser.userId, chinaYesterday),
            env.DB.prepare('SELECT points FROM users WHERE id = ?').bind(authUser.userId)
        ]);

        if (checkSignResult.results[0]) return new Response(JSON.stringify({ error: 'Already signed in today' }), { status: 400, headers: corsHeaders });

        // 连续签到天数 + 1
        const newStreak = ((lastSignResult.results[0] as { streak: number } | undefined)?.streak || 0) + 1;
        // 连签 7 天循环奖励规则
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
        // KV 缓存 300 秒（排行榜数据无需秒级实时性）
        await env.SHEEPS_CACHE.put(cacheKey, responseData, { expirationTtl: 300 });
        return new Response(responseData, { headers: corsHeaders });
    }

    // 6. 每日弹窗（返回昨日积分榜前三名 + 当前用户昨日排名）
    if (path === '/api/leaderboard/daily-popup' && request.method === 'GET') {
        const authUser1 = await getAuthenticatedUser(request, env);
        const now1 = Date.now();
        const chinaToday1 = new Date(new Date(now1 + 8 * 3600000).toISOString().split('T')[0] + 'T00:00:00+08:00').getTime();
        const yesterdayStart1 = chinaToday1 - 24 * 3600000;
        const yesterdayEnd1 = chinaToday1 - 1;

        // top3 可跨用户缓存
        const top3CacheKey = 'daily_popup_top3_' + yesterdayStart1;
        let top3Raw = await env.SHEEPS_CACHE.get(top3CacheKey);
        let top3: Array<{ username: string; points: number }>;
        if (top3Raw) {
            top3 = JSON.parse(top3Raw);
        } else {
            const top3Result = await env.DB.prepare(
                'SELECT u.username, SUM(l.score) as points FROM leaderboard l JOIN users u ON l.user_id = u.id WHERE l.achieved_at >= ?1 AND l.achieved_at <= ?2 GROUP BY l.user_id ORDER BY points DESC LIMIT 3'
            ).bind(yesterdayStart1, yesterdayEnd1).all();
            top3 = top3Result.results as any;
            await env.SHEEPS_CACHE.put(top3CacheKey, JSON.stringify(top3), { expirationTtl: 600 });
        }

        // 当前用户昨日排名（按总积分 SUM，每个人不同，不缓存）
        let yesterdayRank = 0;
        if (authUser1) {
            const userTotal = await env.DB.prepare(
                'SELECT SUM(score) as points FROM leaderboard WHERE user_id = ? AND achieved_at >= ? AND achieved_at <= ?'
            ).bind(authUser1.userId, yesterdayStart1, yesterdayEnd1).first<{ points: number }>();
            const points = userTotal?.points ?? 0;
            if (points > 0) {
                const higher = await env.DB.prepare(
                    'SELECT COUNT(*) as cnt FROM (SELECT user_id, SUM(score) as points FROM leaderboard WHERE achieved_at >= ? AND achieved_at <= ? GROUP BY user_id HAVING points > ?)'
                ).bind(yesterdayStart1, yesterdayEnd1, points).first<{ cnt: number }>();
                yesterdayRank = (higher?.cnt ?? 0) + 1;
            }
        }

        return new Response(JSON.stringify({ success: true, top3, yesterdayRank }), { headers: corsHeaders });
    }
    return null;
}