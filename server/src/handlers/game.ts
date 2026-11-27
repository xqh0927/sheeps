/**
 * @module handlers/game
 * @fileoverview 游戏业务路由处理模块。
 *
 * 由 `handleGameRoutes` 统一分发，承载核心游戏闭环：
 * - 关卡布局获取与解锁（积分消耗）
 * - 通关成绩安全提交（含防作弊签名校验、自动补全游客用户、任务进度推进）
 * - 每日签到（连续签到奖励 + 签到任务自动领取）
 * - 多维度排行榜查询（历史/日/周，按人去重或全量）与每日弹窗
 *
 * 业务流转位置：位于玩家端鉴权之后，所有写操作均通过 `getAuthenticatedUser`
 * 解析 Bearer Token；涉及多步 DB 变更时使用 `env.DB.batch` 保证原子性。
 */

import { Env } from '../types';
import { getCorsHeaders, getAuthenticatedUser, getCachedConfig, getGameModeStatus, cacheControl, jsonError } from '../helpers';
import { generateSolvableLevel } from '../level';
import { readLevelTilesByLevels } from '../lib/level-tiles';
import { sha256 } from '../crypto';
import { refreshWinTasks } from './task';

/**
 * 关卡布局内存缓存（进程级，Worker 重启即失效）。
 * 首关共用固定布局键，其余关卡使用 `v4_{levelId}_{seed}`，仅缓存确定性种子生成的布局；
 * 作为 KV 之前的快速命中层，未命中时从 KV 回填。
 */
const CACHE_STAGE_CONFIG = new Map<number | string, string>();
const validRunId = (id: unknown): id is string => typeof id === 'string' && /^[a-zA-Z0-9-]{1,64}$/.test(id);

/** Both halves of a run use the same account/guest checks and initial guest unlocks. */
async function resolveStagePlayer(request: Request, env: Env, claimedId: string, hasRunId: boolean) {
    const authUser = await getAuthenticatedUser(request, env);
    if (request.headers.get('Authorization') && !authUser) return jsonError('Unauthorized', 401, env);
    if (hasRunId && !authUser && (typeof claimedId !== 'string' || !/^uuid-[a-f0-9-]{36}$/i.test(claimedId))) {
        return jsonError('Unauthorized', 401, env);
    }
    if (hasRunId && authUser && authUser.userId !== claimedId) return jsonError('Account changed', 409, env);

    const userId = authUser?.userId ?? claimedId;
    const mutations: D1PreparedStatement[] = [];
    if (!authUser && !await env.DB.prepare('SELECT 1 FROM users WHERE id = ?').bind(userId).first()) {
        const now = Date.now();
        mutations.push(env.DB.prepare('INSERT OR IGNORE INTO users (id, username, role, points, created_at) VALUES (?, ?, ?, ?, ?)')
            .bind(userId, `玩家_${userId.slice(-4)}`, 'user', 0, now));
        for (const level of [1, 2, 3]) {
            mutations.push(env.DB.prepare('INSERT OR IGNORE INTO level_unlock (user_id, level_id, unlocked_at) VALUES (?, ?, ?)')
                .bind(userId, level, now));
        }
    }
    return { userId, mutations };
}

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

    /**
     * GET /api/level — 获取关卡布局数据
     *
     * Query 参数:
     *   @param {number} id — 关卡编号（必填）
     *   @param {number} [seed] — 可选，随机种子（不传则服务端随机生成）
     *   @param {number} [userId] — 可选，游客用户ID
     *
     * 响应: Tile[] 瓦片布局数组
     */
    if (path === '/api/level' && request.method === 'GET') {
        const levelIdStr = url.searchParams.get('id');
        if (!levelIdStr) return new Response(JSON.stringify({ error: 'Missing level ID' }), { status: 400, headers: corsHeaders });
        const levelId = parseInt(levelIdStr, 10);
        if (isNaN(levelId)) return new Response(JSON.stringify({ error: 'Invalid level ID' }), { status: 400, headers: corsHeaders });

        // An operator may publish or withdraw a board after this seed has already been generated.
        const cacheHeaders = { 'Cache-Control': 'no-store', 'X-Stage-Source': 'generated', 'X-Stage-Version': 'generated-v5' };
        const published = (await readLevelTilesByLevels(env, [levelId])).get(levelId);
        if (published?.length) {
            const layout = JSON.stringify(published);
            return new Response(layout, { headers: {
                ...corsHeaders, ...cacheHeaders, 'X-Stage-Source': 'published', 'X-Stage-Version': await sha256(layout),
            } });
        }

        const seedStr = url.searchParams.get('seed');
        // 若客户端未传种子，则在后端随机生成一个，保证离线与在线的关卡同源随机性
        const seed = seedStr ? parseInt(seedStr, 10) : Math.floor(Math.random() * 1000000) + 1;
        const cacheKey = levelId === 1 ? 'v5_1' : `v4_${levelId}_${seed}`;

        // 仅在传了确定性种子时读缓存，随机种子每次都不一样无需缓存
        if (seedStr) {
            // 优先查内存缓存（最快）
            if (CACHE_STAGE_CONFIG.has(cacheKey))
                return new Response(CACHE_STAGE_CONFIG.get(cacheKey)!, { headers: { ...corsHeaders, ...cacheHeaders } });

            // 其次查 KV 持久化缓存（跨 Worker 实例共享，冷启动也能命中）
            const kvCached = await env.SHEEPS_CACHE.get(cacheKey);
            if (kvCached) {
                // 回填内存缓存
                CACHE_STAGE_CONFIG.set(cacheKey, kvCached);
                return new Response(kvCached, { headers: { ...corsHeaders, ...cacheHeaders } });
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

        return new Response(layoutJson, { headers: { ...corsHeaders, ...cacheHeaders } });
    }

    /**
     * POST /api/level/unlock — 消耗积分解锁关卡
     *
     * 请求头:
     *   Authorization: Bearer <token>
     *
     * 请求体 (JSON):
     *   @param {number} level_id — 目标关卡编号
     *
     * 响应: { success: true, current_points: <number> }
     */
    if (path === '/api/level/unlock' && request.method === 'POST') {
        const authUser = await getAuthenticatedUser(request, env);
        if (!authUser) return new Response(JSON.stringify({ error: 'Unauthorized' }), { status: 401, headers: corsHeaders });

        const body: { level_id: number } = await request.json();
        
        // 从 KV 缓存读取解锁积分配置（避免 D1 查询）
        const configKey = `level_${body.level_id}_unlock_points`;
        const costStr = await getCachedConfig(env, configKey, body.level_id === 2 ? '50' : '200');
        const cost = parseInt(costStr, 10);
        
        // 批量执行数据库事务（D1 batch 保证同批语句原子提交/回滚）：
        // 并行查询「是否已解锁」与「用户当前积分」，避免读-改-写竞态下重复扣分
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

    if (path === '/api/level/attempt' && request.method === 'POST') {
        const body: any = await request.json();
        if (!validRunId(body?.run_id) || !Number.isSafeInteger(body.level_id) || body.level_id <= 0 ||
            typeof body.layout_version !== 'string' || !/^[A-Za-z0-9._-]{1,128}$/.test(body.layout_version) ||
            !Number.isSafeInteger(body.started_at) || body.started_at <= 0) {
            return jsonError('Invalid stage attempt', 400, env);
        }
        const player = await resolveStagePlayer(request, env, body.user_id, true);
        if (player instanceof Response) return player;
        player.mutations.push(env.DB.prepare(
            'INSERT INTO stage_attempts (user_id, run_id, level_id, layout_version, started_at) VALUES (?, ?, ?, ?, ?) ON CONFLICT(user_id, run_id) DO NOTHING'
        ).bind(player.userId, body.run_id, body.level_id, body.layout_version, body.started_at));
        await env.DB.batch(player.mutations);
        return new Response(JSON.stringify({ success: true, run_id: body.run_id }), { headers: corsHeaders });
    }

    /**
     * POST /api/score/submit — 提交通关成绩（含防作弊签名校验）
     *
     * 请求体 (JSON):
     *   @param {string} user_id — 用户ID
     *   @param {number} level_id — 关卡编号
     *   @param {number} clear_time_ms — 通关耗时（毫秒）
     *   @param {number} score — 得分
     *   @param {string} sign — SHA256 防作弊签名
     *
     * 响应: { success }
     */
    const requireScoreReceipt = path === '/api/score/submit-once';
    if ((requireScoreReceipt || path === '/api/score/submit') && request.method === 'POST') {
        const body: any = await request.json();
        if (!body.sign) return new Response(JSON.stringify({ error: 'Missing parameters' }), { status: 400, headers: corsHeaders });

        // 无尽生存模式标记：0=闯关/PvP, 1=无尽；旧客户端不含该字段时默认 0（兼容）
        const gameMode = Number(body.game_mode) || 0;

        // 防作弊校验：使用加盐 SHA256 算法校验签名合法性。
        // 注意：`folklore` 是服务端/客户端约定的硬编码盐值（共享密钥），
        // 客户端须以相同拼接方式与盐值生成签名，缺失或不符即视为作弊请求(403)。
        const expectedSign = await sha256(`${body.user_id}_${body.level_id}_${body.clear_time_ms}_folklore`);
        if (body.sign !== expectedSign) return new Response(JSON.stringify({ error: 'Invalid signature' }), { status: 403, headers: corsHeaders });

        const now = Date.now();
        const runId = body.run_id;
        if ((requireScoreReceipt || runId !== undefined) && !validRunId(runId)) {
            return jsonError('Invalid run ID', 400, env);
        }
        const player = await resolveStagePlayer(request, env, body.user_id, Boolean(runId));
        if (player instanceof Response) return player;
        const { userId: resolvedUserId, mutations } = player;
        const achievedAt = runId && Number.isSafeInteger(body.completed_at) && body.completed_at > 0
            ? Math.min(now, body.completed_at) : now;
        const taskDate = new Date(achievedAt + 8 * 3600000).toISOString().split('T')[0];

        // Each transaction gets its own attempt ID. Retries find the old receipt and cannot write a second score.
        const attempt = runId ? crypto.randomUUID() : null;
        const gate = attempt ? ' WHERE EXISTS (SELECT 1 FROM stage_score_receipts WHERE attempt_id = ?)' : '';
        const gateArgs = attempt ? [attempt] : [];
        if (runId) {
            mutations.push(env.DB.prepare('INSERT OR IGNORE INTO stage_score_receipts (user_id, run_id, attempt_id) VALUES (?, ?, ?)')
                .bind(resolvedUserId, runId, attempt));
        }
        if (gameMode === 0 && (body.is_win === undefined || Number(body.is_win) === 1)) {
            mutations.push(env.DB.prepare(`INSERT OR IGNORE INTO level_unlock (user_id, level_id, unlocked_at) SELECT ?, ?, ?${gate}`)
                .bind(resolvedUserId, body.level_id + 1, achievedAt, ...gateArgs));
        }

        const itemsUsed = Number(body.items_used) || 0;
        const isWin = body.is_win !== undefined ? Number(body.is_win) : 1;

        // 将成绩录入排行榜表（包含 is_win 和 items_used）
        mutations.push(
            env.DB.prepare(`INSERT INTO leaderboard (user_id, level_id, score, clear_time_ms, game_mode, items_used, is_win, achieved_at) SELECT ?, ?, ?, ?, ?, ?, ?, ?${gate}`)
                .bind(resolvedUserId, body.level_id, body.score, body.clear_time_ms, gameMode, itemsUsed, isWin, achievedAt, ...gateArgs)
        );

        if (runId && gameMode === 0) {
            // Link the immediately preceding score insert inside this transaction. A replay skips both writes.
            mutations.push(env.DB.prepare(`INSERT INTO stage_run_scores (user_id, run_id, abandoned, score_id)
                SELECT ?, ?, ?, last_insert_rowid()${gate}`)
                .bind(resolvedUserId, runId, isWin === 0 && body.abandoned === true ? 1 : 0, ...gateArgs));
        }

        if (gameMode === 0 && isWin === 1) mutations.push(refreshWinTasks(env, resolvedUserId, taskDate));
        await env.DB.batch(mutations);

        return new Response(JSON.stringify({ success: true, run_id: runId }), { headers: corsHeaders });
    }

    /**
     * POST /api/sign/today — 每日签到领取积分
     *
     * 请求头:
     *   Authorization: Bearer <token>
     *
     * 响应: { success, streak, reward_points, current_points }
     */
    if (path === '/api/sign/today' && request.method === 'POST') {
        const authUser = await getAuthenticatedUser(request, env);
        if (!authUser) return new Response(JSON.stringify({ error: 'Unauthorized' }), { status: 401, headers: corsHeaders });

        const chinaToday = new Date(Date.now() + 8 * 3600000).toISOString().split('T')[0];
        const chinaYesterday = new Date(Date.now() + 8 * 3600000 - 24 * 3600000).toISOString().split('T')[0];

        // 从 KV 缓存读取签到奖励配置
        const rewardsStr = await getCachedConfig(env, 'sign_rewards', '20,20,30,30,40,50,100');
        const rewards = rewardsStr.split(',').map(Number);
        
        // 批量查询：当天签到情况、昨天连续签到天数、当前用户积分
        const [checkSignResult, lastSignResult, taskResult] = await env.DB.batch([
            env.DB.prepare('SELECT 1 FROM sign_record WHERE user_id = ? AND sign_date = ?').bind(authUser.userId, chinaToday),
            env.DB.prepare('SELECT streak FROM sign_record WHERE user_id = ? AND sign_date = ?').bind(authUser.userId, chinaYesterday),
            env.DB.prepare("SELECT points_reward FROM task WHERE id = 'SIGN_IN_ONCE'")
        ]);

        if (checkSignResult.results[0]) return new Response(JSON.stringify({ error: 'Already signed in today' }), { status: 400, headers: corsHeaders });

        // 连续签到天数 + 1
        const newStreak = ((lastSignResult.results[0] as { streak: number } | undefined)?.streak || 0) + 1;
        // 连签 7 天循环奖励规则
        const rewardPoints = rewards[Math.min(newStreak, 7) - 1] || 20;

        const task = taskResult.results[0] as {points_reward:number} | undefined;
        const taskReward = task?.points_reward ?? 0;
        const total = rewardPoints + taskReward;
        const user = authUser.userId;
        const now = Date.now();
        const ledger = (amount: number, source: string, laterCredit: number) =>
            env.DB.prepare(`INSERT INTO point_record (user_id, type, amount, source, remaining_points, created_at)
                SELECT id, 'IN', ?, ?, points - ?, ? FROM users WHERE id = ?`).bind(amount, source, laterCredit, now, user);
        const writes = [
            env.DB.prepare('INSERT INTO sign_record (user_id, sign_date, streak, points_rewarded, created_at) VALUES (?, ?, ?, ?, ?)')
                .bind(user, chinaToday, newStreak, rewardPoints, now),
            env.DB.prepare('UPDATE users SET points = points + ? WHERE id = ? RETURNING points').bind(total, user),
            ledger(rewardPoints, 'SIGN_IN', taskReward)
        ];
        if (task) {
            writes.push(ledger(taskReward, 'SIGN_IN_ONCE', 0),
                env.DB.prepare(`INSERT OR REPLACE INTO user_task (user_id, task_id, task_date, progress, is_completed, is_rewarded)
                    VALUES (?, 'SIGN_IN_ONCE', ?, 1, 1, 1)`).bind(user, chinaToday));
        }
        try {
            const result = await env.DB.batch(writes);
            const balance = (result[1].results[0] as {points:number}).points;
            return new Response(JSON.stringify({success:true, streak:newStreak, reward_points:total, current_points:balance}), {headers:corsHeaders});
        } catch (error) {
            if (await env.DB.prepare('SELECT 1 FROM sign_record WHERE user_id = ? AND sign_date = ?').bind(user, chinaToday).first()) {
                return new Response(JSON.stringify({error:'Already signed in today'}), {status:400, headers:corsHeaders});
            }
            throw error;
        }
    }

    /**
     * GET /api/leaderboard — 排行榜查询（分页、按时间维度筛选）
     *
     * Query 参数:
     *   @param {number} level_id — 关卡编号（必填）
     *   @param {string} [type="history"] — 排行类型：daily / weekly / history
     *   @param {number} [page=1] — 页码
     *   @param {number} [limit=20] — 每页条数
     *
     * 响应: { success, rankings: [{ username, avatar, clear_time_ms, score, achieved_at }] }
     */
    if (path === '/api/leaderboard' && request.method === 'GET') {
        const cacheHeaders = cacheControl(60);
        const levelIdStr = url.searchParams.get('level_id');
        if (!levelIdStr) return new Response(JSON.stringify({ error: 'Missing level_id' }), { status: 400, headers: corsHeaders });

        const levelId = parseInt(levelIdStr, 10);
        const type = url.searchParams.get('type') || 'history';
        const page = parseInt(url.searchParams.get('page') || '1', 10);
        const limit = parseInt(url.searchParams.get('limit') || '20', 10);
        // 无尽生存模式：0=闯关/PvP, 1=无尽；默认 0 兼容旧客户端
        const gameMode = parseInt(url.searchParams.get('game_mode') || '0', 10);

        // 方案 B·游戏模式网关：无尽模式关闭时返回空榜 + disabled 提示
        if (gameMode === 1) {
            const { endless } = await getGameModeStatus(env);
            if (!endless) {
                return new Response(JSON.stringify({ success: true, rankings: [], total: 0, disabled: true }), { headers: { ...corsHeaders, ...cacheHeaders } });
            }
        }

        // per_user=all 显示全部得分记录，默认 per_user=best 每人只取最高分
        const perUser = url.searchParams.get('per_user') || 'best';
        const showAll = perUser === 'all';

        const cacheKey = `leaderboard_${levelId}_${gameMode}_${type}_${perUser}_${page}_${limit}`;
        const cached = await env.SHEEPS_CACHE.get(cacheKey);
        if (cached) return new Response(cached, { headers: { ...corsHeaders, ...cacheHeaders } });

        const offset = (page - 1) * limit;
        let timeFilter = 0;
        const now = Date.now();
        const chinaTodayStr = new Date(now + 8 * 3600000).toISOString().split('T')[0];
        const todayStart = new Date(chinaTodayStr + 'T00:00:00+08:00').getTime();

        if (type === 'daily') timeFilter = todayStart;
        else if (type === 'weekly') timeFilter = todayStart - ((new Date(now + 8 * 3600000).getDay() + 6) % 7) * 24 * 3600000;

        const whereClause = 'l.level_id = ? AND l.game_mode = ? AND l.achieved_at >= ?' +
            (gameMode === 0 ? ' AND l.is_win = 1' : '');

        // 查询总数（用于分页）
        let totalSql: string;
        let totalBinds: any[];
        if (showAll) {
            totalSql = `SELECT COUNT(*) as c FROM leaderboard l WHERE ${whereClause}`;
            totalBinds = [levelId, gameMode, timeFilter];
        } else {
            totalSql = `SELECT COUNT(DISTINCT l.user_id) as c FROM leaderboard l JOIN users u ON l.user_id = u.id WHERE ${whereClause}`;
            totalBinds = [levelId, gameMode, timeFilter];
        }
        const totalRow = await env.DB.prepare(totalSql).bind(...totalBinds).first<{ c: number }>();
        const total = totalRow?.c || 0;

        let resultsSql: string;
        if (showAll) {
            // per_user=all：显示全部得分记录（用于无尽生存模式查看历史记录）
            resultsSql = `SELECT l.id, u.username, u.avatar_url AS avatar, l.clear_time_ms, l.score, l.achieved_at FROM leaderboard l JOIN users u ON l.user_id = u.id WHERE ${whereClause} ORDER BY l.score DESC, l.clear_time_ms ASC LIMIT ? OFFSET ?`;
        } else {
            // per_user=best：每人只取最高分（防刷榜）
            resultsSql = `SELECT username, avatar, clear_time_ms, score, achieved_at FROM (SELECT u.username, u.avatar_url AS avatar, l.clear_time_ms, l.score, l.achieved_at, ROW_NUMBER() OVER (PARTITION BY l.user_id ORDER BY l.score DESC, l.clear_time_ms ASC) as rn FROM leaderboard l JOIN users u ON l.user_id = u.id WHERE ${whereClause}) WHERE rn = 1 ORDER BY score DESC, clear_time_ms ASC LIMIT ? OFFSET ?`;
        }

        const results = await env.DB.prepare(resultsSql).bind(levelId, gameMode, timeFilter, limit, offset).all();

        const responseData = JSON.stringify({ success: true, rankings: results.results, total });
        // KV 缓存 60 秒（排行榜数据无需秒级实时性，per_user=all 缓存更短）
        const ttl = showAll ? 60 : 300;
        await env.SHEEPS_CACHE.put(cacheKey, responseData, { expirationTtl: ttl });
        return new Response(responseData, { headers: { ...corsHeaders, ...cacheHeaders } });
    }

    /**
     * GET /api/leaderboard/daily-popup — 每日弹窗（昨日积分榜TOP3 + 当前用户排名）
     *
     * 请求头:
     *   Authorization: Bearer <token> （可选，未登录返回 rank=0）
     *
     * 响应: { success, top3: [{ username, points }], yesterdayRank: <number> }
     */
    if (path === '/api/leaderboard/daily-popup' && request.method === 'GET') {
        // 无尽生存模式网关：仅当显式请求 game_mode=1 且模式关闭时返回 disabled
        const gmParam = url.searchParams.get('game_mode');
        if (gmParam === '1') {
            const { endless } = await getGameModeStatus(env);
            if (!endless) {
                return new Response(JSON.stringify({ success: true, top3: [], yesterdayRank: 0, disabled: true }), { headers: corsHeaders });
            }
        }

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

        // 当前用户昨日排名（按总积分 SUM，每个人不同）
        // P1-5 优化：原本每人每次弹窗都跑两条 leaderboard 大表聚合查询，叠加 KV 短缓存
        // （按 userId + date 为 key，TTL 60s）后可直接命中返回，降低峰值 DB 压力；
        // 缓存 miss 仍走原聚合查询，业务逻辑语义不变。需配合 idx_leaderboard_achieved 索引提速。
        let yesterdayRank = 0;
        if (authUser1) {
            const rankCacheKey = `daily_popup_rank_${authUser1.userId}_${yesterdayStart1}`;
            const cachedRankRaw = await env.SHEEPS_CACHE.get(rankCacheKey);
            if (cachedRankRaw !== null && cachedRankRaw !== undefined) {
                yesterdayRank = parseInt(cachedRankRaw, 10) || 0;
            } else {
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
                // 异步回填 KV（TTL 60s），不阻塞主流程；无成绩记为 0 也缓存，避免反复空查
                await env.SHEEPS_CACHE.put(rankCacheKey, String(yesterdayRank), { expirationTtl: 60 }).catch(() => {});
            }
        }

        return new Response(JSON.stringify({ success: true, top3, yesterdayRank }), { headers: corsHeaders });
    }
    return null;
}