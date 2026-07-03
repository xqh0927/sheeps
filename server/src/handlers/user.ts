import { Env } from '../types';
import { getCorsHeaders, getAuthenticatedUser } from '../helpers';

/**
 * 处理用户中心相关的 HTTP 路由请求
 * 包含端云协同的数据同步（本地 Room 脏数据增量覆盖并自动生成云端历史备份）、获取用户 Profile、查询积分流水历史、查询兑换记录历史、以及用户重命名。
 * 
 * @param request 客户端 HTTP 请求对象
 * @param env 环境变量及 D1 数据库实例
 * @param path 请求的 URL 路径
 * @return 匹配路由时返回 Response 对象，未匹配时返回 null
 */
export async function handleUserRoutes(request: Request, env: Env, path: string): Promise<Response | null> {
    const corsHeaders = getCorsHeaders();

    /**
     * POST /api/user/sync — 端云数据增量同步
     *
     * 请求头:
     *   Authorization: Bearer <token>
     *
     * 请求体 (JSON):
     *   @param {number} [points] — 客户端当前积分
     *   @param {number[]} [unlocked_levels] — 已解锁关卡ID列表
     *   @param {Array<{item_type: string, count: number}>} [items] — 道具背包
     *
     * 响应: { success, user, unlocked_levels, items }
     */
    if (path === '/api/user/sync' && request.method === 'POST') {
        const authUser = await getAuthenticatedUser(request, env);
        if (!authUser) return new Response(JSON.stringify({ error: 'Unauthorized' }), { status: 401, headers: corsHeaders });

        const body: any = await request.json();
        // 第一步：备份用户的旧数据，避免同步过程中由于异常导致数据彻底丢失
        const backupQueries = [
            env.DB.prepare('SELECT points FROM users WHERE id = ?').bind(authUser.userId),
            env.DB.prepare('SELECT level_id FROM level_unlock WHERE user_id = ?').bind(authUser.userId),
            env.DB.prepare('SELECT item_type, count FROM user_items WHERE user_id = ?').bind(authUser.userId)
        ];
        const [oldUserResult, oldLevelsResult, oldItemsResult] = await env.DB.batch(backupQueries);
        const oldUser = oldUserResult.results[0] as { points: number } | undefined;

        const mutationStatements = [];
        if (oldUser) {
            const backupPayload = JSON.stringify({ points: oldUser.points, unlocked_levels: oldLevelsResult.results.map((r: any) => r.level_id), items: oldItemsResult.results });
            // 将备份写入日志，并自动清理 7 天前过期的历史备份，防 D1 爆库
            mutationStatements.push(
                env.DB.prepare('INSERT INTO backup_save_log (user_id, save_data, created_at) VALUES (?, ?, ?)').bind(authUser.userId, backupPayload, Date.now()),
                env.DB.prepare('DELETE FROM backup_save_log WHERE created_at < ?').bind(Date.now() - 7 * 86400000)
            );
        }

        // 第二步：合并客户端传上来的积分数据（取本地和云端极大值），合并关卡解锁进度，以及叠加或更新背包道具
        if (body.points !== undefined && body.points > 0) mutationStatements.push(env.DB.prepare('UPDATE users SET points = MAX(points, ?) WHERE id = ?').bind(body.points, authUser.userId));
        if (body.unlocked_levels) for (const lvl of body.unlocked_levels) mutationStatements.push(env.DB.prepare('INSERT OR IGNORE INTO level_unlock (user_id, level_id, unlocked_at) VALUES (?, ?, ?)').bind(authUser.userId, lvl, Date.now()));
        if (body.items) for (const item of body.items) mutationStatements.push(env.DB.prepare('INSERT OR REPLACE INTO user_items (user_id, item_type, count) VALUES (?, ?, COALESCE((SELECT MAX(count, ?) FROM user_items WHERE user_id = ? AND item_type = ?), ?))').bind(authUser.userId, item.item_type, item.count, authUser.userId, item.item_type, item.count));

        if (mutationStatements.length > 0) await env.DB.batch(mutationStatements);

        // 第三步：返回用户最新同步完的最终权威云端数据，供端侧刷新本地 Room 状态
        const [updatedUserResult, updatedLevelsResult, updatedItemsResult] = await env.DB.batch([
            env.DB.prepare('SELECT id, phone, username, avatar, points FROM users WHERE id = ?').bind(authUser.userId),
            env.DB.prepare('SELECT level_id FROM level_unlock WHERE user_id = ?').bind(authUser.userId),
            env.DB.prepare('SELECT item_type, count FROM user_items WHERE user_id = ?').bind(authUser.userId)
        ]);

        return new Response(JSON.stringify({ success: true, user: updatedUserResult.results[0], unlocked_levels: updatedLevelsResult.results.map((r: any) => r.level_id), items: updatedItemsResult.results }), { headers: corsHeaders });
    }

    /**
     * GET /api/user/profile — 获取用户完整Profile
     *
     * 请求头:
     *   Authorization: Bearer <token>
     *
     * 响应: { success, user, unlocked_levels, items, today_signed, sign_streak, highest_level_cleared }
     */
    if (path === '/api/user/profile' && request.method === 'GET') {
        const authUser = await getAuthenticatedUser(request, env);
        if (!authUser) return new Response(JSON.stringify({ error: 'Unauthorized' }), { status: 401, headers: corsHeaders });

        const chinaToday = new Date(Date.now() + 8 * 3600000).toISOString().split('T')[0];
        const [userResult, levelsResult, itemsResult, signedTodayResult, lastSignResult, highestClearedResult] = await env.DB.batch([
            env.DB.prepare('SELECT id, phone, username, avatar, points FROM users WHERE id = ?').bind(authUser.userId),
            env.DB.prepare('SELECT level_id FROM level_unlock WHERE user_id = ?').bind(authUser.userId),
            env.DB.prepare('SELECT item_type, count FROM user_items WHERE user_id = ?').bind(authUser.userId),
            env.DB.prepare('SELECT 1 FROM sign_record WHERE user_id = ? AND sign_date = ?').bind(authUser.userId, chinaToday),
            env.DB.prepare('SELECT streak FROM sign_record WHERE user_id = ? ORDER BY sign_date DESC LIMIT 1').bind(authUser.userId),
            env.DB.prepare('SELECT MAX(level_id) as highest FROM leaderboard WHERE user_id = ?').bind(authUser.userId)
        ]);

        return new Response(JSON.stringify({
            success: true, user: userResult.results[0], unlocked_levels: levelsResult.results.map((r: any) => r.level_id), items: itemsResult.results,
            today_signed: signedTodayResult.results[0] !== null, sign_streak: (lastSignResult.results[0] as any)?.streak || 0, highest_level_cleared: (highestClearedResult.results[0] as any)?.highest || 0
        }), { headers: corsHeaders });
    }

    /**
     * GET /api/user/points-history — 查询积分收支明细
     *
     * 请求头:
     *   Authorization: Bearer <token>
     *
     * 响应: [{ type, amount, source, remaining_points, created_at }, ...] （最近50条）
     */
    if (path === '/api/user/points-history' && request.method === 'GET') {
        const authUser = await getAuthenticatedUser(request, env);
        if (!authUser) return new Response(JSON.stringify({ error: 'Unauthorized' }), { status: 401, headers: corsHeaders });
        const records = await env.DB.prepare('SELECT type, amount, source, remaining_points, created_at FROM point_record WHERE user_id = ? ORDER BY created_at DESC LIMIT 50').bind(authUser.userId).all();
        return new Response(JSON.stringify(records.results), { headers: corsHeaders });
    }

    /**
     * GET /api/user/exchange-history — 查询道具兑换历史
     *
     * 请求头:
     *   Authorization: Bearer <token>
     *
     * 响应: [{ id, shop_item_id, item_type, count, points_cost, created_at }, ...] （最近50条）
     */
    if (path === '/api/user/exchange-history' && request.method === 'GET') {
        const authUser = await getAuthenticatedUser(request, env);
        if (!authUser) return new Response(JSON.stringify({ error: 'Unauthorized' }), { status: 401, headers: corsHeaders });
        const records = await env.DB.prepare('SELECT id, shop_item_id, item_type, count, points_cost, created_at FROM exchange_record WHERE user_id = ? ORDER BY created_at DESC LIMIT 50').bind(authUser.userId).all();
        return new Response(JSON.stringify(records.results), { headers: corsHeaders });
    }

    /**
     * POST /api/user/rename — 修改用户昵称
     *
     * 请求体 (JSON):
     *   @param {string} id — 用户ID
     *   @param {string} new_username — 新昵称
     *
     * 响应: { success: true }
     */
    if (path === '/api/user/rename' && request.method === 'POST') {
        const body: { id: string; new_username: string } = await request.json();
        if (!body.id || !body.new_username) return new Response(JSON.stringify({ error: 'Missing parameters' }), { status: 400, headers: corsHeaders });
        await env.DB.prepare('UPDATE users SET username = ? WHERE id = ?').bind(body.new_username, body.id).run();
        return new Response(JSON.stringify({ success: true }), { headers: corsHeaders });
    }

    return null;
}