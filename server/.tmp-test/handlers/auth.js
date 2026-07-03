"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.handleAuthRoutes = handleAuthRoutes;
const helpers_1 = require("../helpers");
const crypto_1 = require("../crypto");
/**
 * 处理所有与身份认证相关的 HTTP 路由请求
 *
 * @param request 客户端 HTTP 请求对象
 * @param env 环境变量及 D1 数据库实例
 * @param path 请求的 URL 路径
 * @return 匹配路由时返回 Response 对象，未匹配时返回 null
 */
async function handleAuthRoutes(request, env, path) {
    const corsHeaders = (0, helpers_1.getCorsHeaders)();
    // 1. 发送登录验证码接口
    if (path === '/api/auth/send-code' && request.method === 'POST') {
        const body = await request.json();
        if (!body.phone)
            return new Response(JSON.stringify({ error: 'Missing phone number' }), { status: 400, headers: corsHeaders });
        // 生成 6 位随机数字验证码
        const code = Math.floor(100000 + Math.random() * 900000).toString();
        // 将验证码写入 login_token 表，并记录当前毫秒级时间戳以实现过期校验
        await env.DB.prepare('INSERT OR REPLACE INTO login_token (phone, code, created_at) VALUES (?, ?, ?)').bind(body.phone, code, Date.now()).run();
        return new Response(JSON.stringify({ success: true, code }), { headers: corsHeaders });
    }
    // 2. 验证码登录/注册接口
    if (path === '/api/auth/login' && request.method === 'POST') {
        const body = await request.json();
        if (!body.phone || !body.code)
            return new Response(JSON.stringify({ error: 'Missing parameters' }), { status: 400, headers: corsHeaders });
        // 获取该手机号的最新验证码记录
        const codeRecord = await env.DB.prepare('SELECT code, created_at FROM login_token WHERE phone = ?').bind(body.phone).first();
        if (!codeRecord || codeRecord.code !== body.code)
            return new Response(JSON.stringify({ error: 'Verification code incorrect' }), { status: 400, headers: corsHeaders });
        // 限制验证码 5 分钟内有效
        if (Date.now() - codeRecord.created_at > 5 * 60 * 1000)
            return new Response(JSON.stringify({ error: 'Verification code expired' }), { status: 400, headers: corsHeaders });
        // 验证通过，删除验证码，并查询用户信息
        const [deleteTokenResult, userResult] = await env.DB.batch([
            env.DB.prepare('DELETE FROM login_token WHERE phone = ?').bind(body.phone),
            env.DB.prepare('SELECT id, phone, username, avatar, points FROM users WHERE phone = ?').bind(body.phone)
        ]);
        let user = userResult.results[0];
        // 如果用户不存在，说明是新用户，自动执行注册逻辑
        if (!user) {
            const uuid = crypto.randomUUID();
            const defaultUsername = `国风玩家_${body.phone.slice(-4)}`;
            const insertQueries = [
                // 插入用户主表
                env.DB.prepare('INSERT INTO users (id, phone, username, avatar, points, created_at) VALUES (?, ?, ?, ?, ?, ?)').bind(uuid, body.phone, defaultUsername, null, 0, Date.now()),
                // 默认解锁第 1 关
                env.DB.prepare('INSERT INTO level_unlock (user_id, level_id, unlocked_at) VALUES (?, 1, ?)').bind(uuid, Date.now())
            ];
            // 新注册用户赠送初始道具：撤销、洗牌、置物架移出、复活（各 1 个）
            for (const item of ['UNDO', 'SHUFFLE', 'MOVEOUT', 'REVIVE']) {
                insertQueries.push(env.DB.prepare('INSERT INTO user_items (user_id, item_type, count) VALUES (?, ?, ?)').bind(uuid, item, 1));
            }
            await env.DB.batch(insertQueries);
            user = { id: uuid, phone: body.phone, username: defaultUsername, avatar: null, points: 0 };
        }
        // 游客合并逻辑：如果登录时传入了设备的游客 device_uuid，且与当前注册用户的 id 不同，需要合并积分、关卡进度及道具
        if (body.device_uuid && body.device_uuid !== user.id) {
            const guestUser = await env.DB.prepare('SELECT points FROM users WHERE id = ?').bind(body.device_uuid).first();
            if (guestUser) {
                // 读取游客的关卡解锁和道具记录
                const [guestLevelsResult, guestItemsResult] = await env.DB.batch([
                    env.DB.prepare('SELECT level_id FROM level_unlock WHERE user_id = ?').bind(body.device_uuid),
                    env.DB.prepare('SELECT item_type, count FROM user_items WHERE user_id = ?').bind(body.device_uuid)
                ]);
                const mergedPoints = user.points + guestUser.points;
                user.points = mergedPoints;
                const mergeMutations = [env.DB.prepare('UPDATE users SET points = ? WHERE id = ?').bind(mergedPoints, user.id)];
                // 合并关卡解锁记录（忽略重复的）
                for (const row of guestLevelsResult.results)
                    mergeMutations.push(env.DB.prepare('INSERT OR IGNORE INTO level_unlock (user_id, level_id, unlocked_at) VALUES (?, ?, ?)').bind(user.id, row.level_id, Date.now()));
                // 道具叠加更新
                for (const row of guestItemsResult.results)
                    mergeMutations.push(env.DB.prepare('INSERT INTO user_items (user_id, item_type, count) VALUES (?, ?, ?) ON CONFLICT(user_id, item_type) DO UPDATE SET count = count + ?').bind(user.id, row.item_type, row.count, row.count));
                // 级联删除游客在各个业务表中的历史数据，防止外键冲突或脏数据残留
                const tablesToDelete = ['level_unlock', 'user_items', 'leaderboard', 'point_record', 'exchange_record', 'sign_record', 'user_task', 'backup_save_log'];
                for (const table of tablesToDelete)
                    mergeMutations.push(env.DB.prepare(`DELETE FROM ${table} WHERE user_id = ?`).bind(body.device_uuid));
                mergeMutations.push(env.DB.prepare('DELETE FROM users WHERE id = ?').bind(body.device_uuid));
                await env.DB.batch(mergeMutations);
            }
        }
        // 生成 Access Token (过期时间 2 小时) 与 Refresh Token (过期时间 30 天)
        const token = await (0, crypto_1.generateJWT)({ userId: user.id, phone: user.phone, type: 'access', exp: Date.now() + 7200000 });
        const refreshToken = await (0, crypto_1.generateJWT)({ userId: user.id, phone: user.phone, type: 'refresh', exp: Date.now() + 30 * 86400000 });
        // 获取当天签到状况及连续签到次数
        const chinaToday = new Date(Date.now() + 8 * 3600000).toISOString().split('T')[0];
        const [unlockedResult, itemsResult, signTodayResult, lastSignResult] = await env.DB.batch([
            env.DB.prepare('SELECT level_id FROM level_unlock WHERE user_id = ?').bind(user.id),
            env.DB.prepare('SELECT item_type, count FROM user_items WHERE user_id = ?').bind(user.id),
            env.DB.prepare('SELECT 1 FROM sign_record WHERE user_id = ? AND sign_date = ?').bind(user.id, chinaToday),
            env.DB.prepare('SELECT streak FROM sign_record WHERE user_id = ? ORDER BY sign_date DESC LIMIT 1').bind(user.id)
        ]);
        return new Response(JSON.stringify({
            success: true, token, refreshToken, user,
            unlocked_levels: unlockedResult.results.map((r) => r.level_id),
            items: itemsResult.results,
            today_signed: signTodayResult.results[0] !== null,
            sign_streak: lastSignResult.results[0]?.streak || 0
        }), { headers: corsHeaders });
    }
    // 3. 双 Token 静默刷新接口
    if (path === '/api/auth/refresh' && request.method === 'POST') {
        const body = await request.json();
        if (!body.refreshToken)
            return new Response(JSON.stringify({ error: 'Missing refresh token' }), { status: 400, headers: corsHeaders });
        // 验证 Refresh Token 合法性与过期时间
        const payload = await (0, crypto_1.verifyJWT)(body.refreshToken);
        if (!payload || payload.type !== 'refresh')
            return new Response(JSON.stringify({ error: 'Invalid or expired refresh token' }), { status: 401, headers: corsHeaders });
        // 重新签发一组新的双 Token
        const newAccessToken = await (0, crypto_1.generateJWT)({ userId: payload.userId, phone: payload.phone, type: 'access', exp: Date.now() + 7200000 });
        const newRefreshToken = await (0, crypto_1.generateJWT)({ userId: payload.userId, phone: payload.phone, type: 'refresh', exp: Date.now() + 30 * 86400000 });
        return new Response(JSON.stringify({ success: true, token: newAccessToken, refreshToken: newRefreshToken }), { headers: corsHeaders });
    }
    return null;
}
