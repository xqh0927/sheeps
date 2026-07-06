"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.handleUserRoutes = handleUserRoutes;
exports.handleAvatarProxy = handleAvatarProxy;
const helpers_1 = require("../helpers");
/**
 * 处理用户中心相关的 HTTP 路由请求
 * 包含端云协同的数据同步（本地 Room 脏数据增量覆盖并自动生成云端历史备份）、获取用户 Profile、查询积分流水历史、查询兑换记录历史、以及用户重命名。
 *
 * @param request 客户端 HTTP 请求对象
 * @param env 环境变量及 D1 数据库实例
 * @param path 请求的 URL 路径
 * @return 匹配路由时返回 Response 对象，未匹配时返回 null
 */
async function handleUserRoutes(request, env, path) {
    const corsHeaders = (0, helpers_1.getCorsHeaders)();
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
        const authUser = await (0, helpers_1.getAuthenticatedUser)(request, env);
        if (!authUser)
            return new Response(JSON.stringify({ error: 'Unauthorized' }), { status: 401, headers: corsHeaders });
        const body = await request.json();
        // 第一步：备份用户的旧数据，避免同步过程中由于异常导致数据彻底丢失
        const backupQueries = [
            env.DB.prepare('SELECT points FROM users WHERE id = ?').bind(authUser.userId),
            env.DB.prepare('SELECT level_id FROM level_unlock WHERE user_id = ?').bind(authUser.userId),
            env.DB.prepare('SELECT item_type, count FROM user_items WHERE user_id = ?').bind(authUser.userId)
        ];
        const [oldUserResult, oldLevelsResult, oldItemsResult] = await env.DB.batch(backupQueries);
        const oldUser = oldUserResult.results[0];
        const mutationStatements = [];
        if (oldUser) {
            const backupPayload = JSON.stringify({ points: oldUser.points, unlocked_levels: oldLevelsResult.results.map((r) => r.level_id), items: oldItemsResult.results });
            // 将备份写入日志，并自动清理 7 天前过期的历史备份，防 D1 爆库
            mutationStatements.push(env.DB.prepare('INSERT INTO backup_save_log (user_id, save_data, created_at) VALUES (?, ?, ?)').bind(authUser.userId, backupPayload, Date.now()), env.DB.prepare('DELETE FROM backup_save_log WHERE created_at < ?').bind(Date.now() - 7 * 86400000));
        }
        // 第二步：合并客户端传上来的积分数据（取本地和云端极大值），合并关卡解锁进度，以及叠加或更新背包道具
        if (body.points !== undefined && body.points > 0)
            mutationStatements.push(env.DB.prepare('UPDATE users SET points = MAX(points, ?) WHERE id = ?').bind(body.points, authUser.userId));
        if (body.unlocked_levels)
            for (const lvl of body.unlocked_levels)
                mutationStatements.push(env.DB.prepare('INSERT OR IGNORE INTO level_unlock (user_id, level_id, unlocked_at) VALUES (?, ?, ?)').bind(authUser.userId, lvl, Date.now()));
        if (body.items)
            for (const item of body.items)
                mutationStatements.push(env.DB.prepare('INSERT OR REPLACE INTO user_items (user_id, item_type, count) VALUES (?, ?, COALESCE((SELECT MAX(count, ?) FROM user_items WHERE user_id = ? AND item_type = ?), ?))').bind(authUser.userId, item.item_type, item.count, authUser.userId, item.item_type, item.count));
        if (mutationStatements.length > 0)
            await env.DB.batch(mutationStatements);
        // 第三步：返回用户最新同步完的最终权威云端数据，供端侧刷新本地 Room 状态
        const [updatedUserResult, updatedLevelsResult, updatedItemsResult] = await env.DB.batch([
            env.DB.prepare('SELECT id, phone, username, avatar, points FROM users WHERE id = ?').bind(authUser.userId),
            env.DB.prepare('SELECT level_id FROM level_unlock WHERE user_id = ?').bind(authUser.userId),
            env.DB.prepare('SELECT item_type, count FROM user_items WHERE user_id = ?').bind(authUser.userId)
        ]);
        return new Response(JSON.stringify({ success: true, user: updatedUserResult.results[0], unlocked_levels: updatedLevelsResult.results.map((r) => r.level_id), items: updatedItemsResult.results }), { headers: corsHeaders });
    }
    /**
     * GET /api/user/profile — 获取用户完整Profile
     *
     * 请求头:
     *   Authorization: Bearer <token>
     *
     * 响应: { success, user, unlocked_levels, items, today_signed, sign_streak, highest_level_cleared, avatarUrl }
     */
    if (path === '/api/user/profile' && request.method === 'GET') {
        const authUser = await (0, helpers_1.getAuthenticatedUser)(request, env);
        if (!authUser)
            return new Response(JSON.stringify({ error: 'Unauthorized' }), { status: 401, headers: corsHeaders });
        const chinaToday = new Date(Date.now() + 8 * 3600000).toISOString().split('T')[0];
        const [userResult, levelsResult, itemsResult, signedTodayResult, lastSignResult, highestClearedResult] = await env.DB.batch([
            env.DB.prepare('SELECT id, phone, username, avatar, points, avatar_url FROM users WHERE id = ?').bind(authUser.userId),
            env.DB.prepare('SELECT level_id FROM level_unlock WHERE user_id = ?').bind(authUser.userId),
            env.DB.prepare('SELECT item_type, count FROM user_items WHERE user_id = ?').bind(authUser.userId),
            env.DB.prepare('SELECT 1 FROM sign_record WHERE user_id = ? AND sign_date = ?').bind(authUser.userId, chinaToday),
            env.DB.prepare('SELECT streak FROM sign_record WHERE user_id = ? ORDER BY sign_date DESC LIMIT 1').bind(authUser.userId),
            env.DB.prepare('SELECT MAX(level_id) as highest FROM leaderboard WHERE user_id = ?').bind(authUser.userId)
        ]);
        const userData = userResult.results[0];
        return new Response(JSON.stringify({
            success: true, user: userData, unlocked_levels: levelsResult.results.map((r) => r.level_id), items: itemsResult.results,
            today_signed: signedTodayResult.results[0] !== null && signedTodayResult.results[0] !== undefined, sign_streak: lastSignResult.results[0]?.streak || 0, highest_level_cleared: highestClearedResult.results[0]?.highest || 0,
            avatarUrl: userData?.avatar_url || null
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
        const authUser = await (0, helpers_1.getAuthenticatedUser)(request, env);
        if (!authUser)
            return new Response(JSON.stringify({ error: 'Unauthorized' }), { status: 401, headers: corsHeaders });
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
        const authUser = await (0, helpers_1.getAuthenticatedUser)(request, env);
        if (!authUser)
            return new Response(JSON.stringify({ error: 'Unauthorized' }), { status: 401, headers: corsHeaders });
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
        const authUser = await (0, helpers_1.getAuthenticatedUser)(request, env);
        if (!authUser)
            return new Response(JSON.stringify({ error: 'Unauthorized' }), { status: 401, headers: corsHeaders });
        const body = await request.json();
        if (!body.new_username)
            return new Response(JSON.stringify({ error: 'Missing new_username' }), { status: 400, headers: corsHeaders });
        await env.DB.prepare('UPDATE users SET username = ? WHERE id = ?').bind(body.new_username, authUser.userId).run();
        return new Response(JSON.stringify({ success: true }), { headers: corsHeaders });
    }
    /**
     * POST /api/user/avatar — 上传用户头像（multipart/form-data）
     *
     * 请求头:
     *   Authorization: Bearer <token>
     *   Content-Type: multipart/form-data
     *
     * 请求体:
     *   @param {File} avatar — 用户头像图片文件（png/jpeg/webp，≤ 512KB）
     *
     * 响应: { success: true, avatarUrl: string }
     */
    if (path === '/api/user/avatar' && request.method === 'POST') {
        const authUser = await (0, helpers_1.getAuthenticatedUser)(request, env);
        if (!authUser)
            return new Response(JSON.stringify({ error: 'Unauthorized' }), { status: 401, headers: corsHeaders });
        // 从 multipart/form-data 中读取图片文件
        const contentType = request.headers.get('Content-Type') || '';
        if (!contentType.includes('multipart/form-data')) {
            return new Response(JSON.stringify({ error: 'Expected multipart/form-data' }), { status: 400, headers: corsHeaders });
        }
        const formData = await request.formData();
        const file = formData.get('avatar');
        if (!file) {
            return new Response(JSON.stringify({ error: 'Missing avatar file' }), { status: 400, headers: corsHeaders });
        }
        // 校验文件大小（≤ 512KB）
        if (file.size > 512 * 1024) {
            return new Response(JSON.stringify({ error: 'Avatar image too large (max 512KB)' }), { status: 400, headers: corsHeaders });
        }
        // 校验文件类型
        const validTypes = ['image/png', 'image/jpeg', 'image/webp'];
        if (!validTypes.includes(file.type)) {
            return new Response(JSON.stringify({ error: 'Invalid image type (allowed: png, jpeg, webp)' }), { status: 400, headers: corsHeaders });
        }
        // 删除该用户的旧头像文件（避免 R2 存储泄漏）
        const oldObjects = await env.AVATAR_BUCKET.list({ prefix: `avatars/${authUser.userId}_` });
        for (const oldObj of oldObjects.objects) {
            await env.AVATAR_BUCKET.delete(oldObj.key);
        }
        // 上传到 R2
        const timestamp = Date.now();
        const key = `avatars/${authUser.userId}_${timestamp}.png`;
        await env.AVATAR_BUCKET.put(key, file.stream(), {
            httpMetadata: { contentType: file.type || 'image/jpeg' },
            customMetadata: { userId: authUser.userId, uploadedAt: String(timestamp) }
        });
        // 存储 R2 公网直链到 D1（供 profile 等领域直接查询，跳过 Worker 代理）
        const r2PublicUrl = env.R2_PUBLIC_URL || new URL(request.url).origin;
        const avatarUrl = `${r2PublicUrl}/${key}`;
        await env.DB.prepare('UPDATE users SET avatar_url = ? WHERE id = ?')
            .bind(avatarUrl, authUser.userId).run();
        // 响应中返回完整公网 URL（Android 端直接用于 Coil 加载）
        return new Response(JSON.stringify({ success: true, avatarUrl }), { headers: corsHeaders });
    }
    return null;
}
/**
 * 处理头像代理请求 — GET /api/avatar/:userId
 * 从 R2 中查找用户最新头像并直接返回图片二进制数据
 *
 * @param request 客户端 HTTP 请求对象
 * @param env 环境变量及 R2 bucket 实例
 * @param path 请求的 URL 路径
 * @return 匹配时返回图片 Response，否则返回 null
 */
async function handleAvatarProxy(request, env, path) {
    const corsHeaders = (0, helpers_1.getCorsHeaders)();
    if (!path.startsWith('/api/avatar/') || request.method !== 'GET') {
        return null;
    }
    // 路径格式: /api/avatar/{userId}
    const userId = path.replace('/api/avatar/', '');
    if (!userId) {
        return new Response(JSON.stringify({ error: 'Missing userId' }), { status: 400, headers: corsHeaders });
    }
    // 查找 R2 中该用户的所有头像文件
    const objects = await env.AVATAR_BUCKET.list({ prefix: `avatars/${userId}_` });
    if (objects.objects.length === 0) {
        return new Response(JSON.stringify({ error: 'Avatar not found' }), { status: 404, headers: corsHeaders });
    }
    // 取最新的一个（按上传时间排序）
    const latestObject = objects.objects.sort((a, b) => (b.uploaded?.getTime() || 0) - (a.uploaded?.getTime() || 0))[0];
    const object = await env.AVATAR_BUCKET.get(latestObject.key);
    if (!object) {
        return new Response(JSON.stringify({ error: 'Avatar file not found' }), { status: 404, headers: corsHeaders });
    }
    const headers = new Headers();
    object.writeHttpMetadata(headers);
    headers.set('Cache-Control', 'public, max-age=86400'); // 缓存24小时
    headers.set('etag', object.httpEtag);
    // 合并 CORS 头
    for (const [key, value] of Object.entries(corsHeaders)) {
        headers.set(key, value);
    }
    return new Response(object.body, { headers });
}
