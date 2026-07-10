import { Env } from '../types';
import { getCorsHeaders } from '../helpers';
import { generateJWT, verifyJWT } from '../crypto';
import { hashPassword, verifyPassword } from '../auth-utils';

/**
 * 处理所有与身份认证相关的 HTTP 路由请求
 * 
 * @param request 客户端 HTTP 请求对象
 * @param env 环境变量及 D1 数据库实例
 * @param path 请求的 URL 路径
 * @return 匹配路由时返回 Response 对象，未匹配时返回 null
 */
export async function handleAuthRoutes(request: Request, env: Env, path: string): Promise<Response | null> {
    const corsHeaders = getCorsHeaders();

    /**
     * POST /api/auth/send-code — 发送验证码
     *
     * 请求体 (JSON):
     *   @param {string} phone — 手机号
     *
     * 响应: { success: true, code: "123456" }
     */
    if (path === '/api/auth/send-code' && request.method === 'POST') {
        const body: { phone: string } = await request.json();
        if (!body.phone) return new Response(JSON.stringify({ error: '请填写手机号' }), { status: 400, headers: corsHeaders });

        // 生成 6 位随机数字验证码
        const code = Math.floor(100000 + Math.random() * 900000).toString();
        // 将验证码写入 login_token 表，并记录当前毫秒级时间戳以实现过期校验
        await env.DB.prepare('INSERT OR REPLACE INTO login_token (phone, code, created_at) VALUES (?, ?, ?)').bind(body.phone, code, Date.now()).run();
        return new Response(JSON.stringify({ success: true, code }), { headers: corsHeaders });
    }

    /**
     * POST /api/auth/login — 验证码登录/注册
     *
     * 请求体 (JSON):
     *   @param {string} phone — 手机号
     *   @param {string} code — 6位验证码
     *   @param {string} [device_uuid] — 可选，游客设备ID（用于数据合并）
     *
     * 响应: { success, token, refreshToken, user, unlocked_levels, items, today_signed, sign_streak }
     */
    if (path === '/api/auth/login' && request.method === 'POST') {
        const body: { phone: string; code: string; device_uuid?: string } = await request.json();
        if (!body.phone || !body.code) return new Response(JSON.stringify({ error: '请填写手机号和验证码' }), { status: 400, headers: corsHeaders });

        const chinaToday = new Date(Date.now() + 8 * 3600000).toISOString().split('T')[0];

        // ─── 合并查询：一次 D1 batch 完成所有读操作 ───
        const [codeRecord, userRow, unlockedResult, itemsResult, signTodayRow, lastSignResult] = await env.DB.batch([
            env.DB.prepare('SELECT code, created_at FROM login_token WHERE phone = ?').bind(body.phone),
            env.DB.prepare('SELECT id, phone, username, avatar_url AS avatar, points, password_hash FROM users WHERE phone = ?').bind(body.phone),
            env.DB.prepare('SELECT level_id FROM level_unlock WHERE user_id = (SELECT id FROM users WHERE phone = ?)').bind(body.phone),
            env.DB.prepare('SELECT item_type, count FROM user_items WHERE user_id = (SELECT id FROM users WHERE phone = ?)').bind(body.phone),
            env.DB.prepare('SELECT 1 FROM sign_record WHERE user_id = (SELECT id FROM users WHERE phone = ?) AND sign_date = ?').bind(body.phone, chinaToday),
            env.DB.prepare('SELECT streak FROM sign_record WHERE user_id = (SELECT id FROM users WHERE phone = ?) ORDER BY sign_date DESC LIMIT 1').bind(body.phone)
        ]);

        const tokenRecord = codeRecord.results[0] as { code: string; created_at: number } | undefined;
        if (!tokenRecord || tokenRecord.code !== body.code)
            return new Response(JSON.stringify({ error: '验证码错误' }), { status: 400, headers: corsHeaders });
        if (Date.now() - tokenRecord.created_at > 5 * 60 * 1000)
            return new Response(JSON.stringify({ error: '验证码已过期' }), { status: 400, headers: corsHeaders });

        // 清除验证码（异步，不阻塞响应）
        env.DB.prepare('DELETE FROM login_token WHERE phone = ?').bind(body.phone).run().catch(() => {});

        let user = userRow.results[0] as any;
        // 准备响应数据（默认使用预查询结果）
        let finalLevels: number[] = unlockedResult.results.map((r: any) => r.level_id);
        let finalItems: any[] = itemsResult.results;
        let finalSigned = signTodayRow.results[0] !== null && signTodayRow.results[0] !== undefined;
        let finalStreak: number = (lastSignResult.results[0] as any)?.streak || 0;

        // 如果用户不存在，说明是新用户，自动执行注册逻辑
        if (!user) {
            const uuid = crypto.randomUUID();
            const defaultUsername = `国风玩家_${body.phone.slice(-4)}`;
            const insertQueries = [
                // 插入用户主表
                env.DB.prepare('INSERT INTO users (id, phone, username, role, points, created_at) VALUES (?, ?, ?, ?, ?, ?)').bind(uuid, body.phone, defaultUsername, 'user', 0, Date.now()),
                // 默认解锁第 1 关
                env.DB.prepare('INSERT INTO level_unlock (user_id, level_id, unlocked_at) VALUES (?, 1, ?)').bind(uuid, Date.now())
            ];
            // 新注册用户赠送初始道具：撤销、洗牌、置物架移出、复活（各 1 个）
            for (const item of ['UNDO', 'SHUFFLE', 'MOVEOUT', 'REVIVE']) {
                insertQueries.push(env.DB.prepare('INSERT INTO user_items (user_id, item_type, count) VALUES (?, ?, ?)').bind(uuid, item, 1));
            }
            await env.DB.batch(insertQueries);
            user = { id: uuid, phone: body.phone, username: defaultUsername, avatar: null, points: 0 };
            // 新用户响应数据：手动构建，避免额外查询
            finalLevels = [1];
            finalItems = [{ item_type: 'UNDO', count: 1 }, { item_type: 'SHUFFLE', count: 1 }, { item_type: 'MOVEOUT', count: 1 }, { item_type: 'REVIVE', count: 1 }];
            finalSigned = false;
            finalStreak = 0;
        }

        // 游客合并逻辑：如果登录时传入了设备的游客 device_uuid，且与当前注册用户的 id 不同，需要合并积分、关卡进度及道具
        if (body.device_uuid && body.device_uuid !== user.id) {
            const guestUser = await env.DB.prepare('SELECT points FROM users WHERE id = ?').bind(body.device_uuid).first<{ points: number }>();
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
                for (const row of guestLevelsResult.results as any[]) {
                    mergeMutations.push(env.DB.prepare('INSERT OR IGNORE INTO level_unlock (user_id, level_id, unlocked_at) VALUES (?, ?, ?)').bind(user.id, row.level_id, Date.now()));
                    if (!finalLevels.includes(row.level_id)) finalLevels.push(row.level_id);
                }
                // 道具叠加更新：合并游客道具到响应数据
                for (const row of guestItemsResult.results as any[]) {
                    mergeMutations.push(env.DB.prepare('INSERT INTO user_items (user_id, item_type, count) VALUES (?, ?, ?) ON CONFLICT(user_id, item_type) DO UPDATE SET count = count + ?').bind(user.id, row.item_type, row.count, row.count));
                    const existing = finalItems.find((i: any) => i.item_type === row.item_type);
                    if (existing) existing.count += row.count;
                    else finalItems.push({ item_type: row.item_type, count: row.count });
                }

                // 级联删除游客在各个业务表中的历史数据，防止外键冲突或脏数据残留
                const userTables = ['level_unlock', 'user_items', 'leaderboard', 'point_record', 'exchange_record', 'sign_record', 'user_task', 'backup_save_log'];
                for (const table of userTables) {
                    mergeMutations.push(env.DB.prepare(`DELETE FROM ${table} WHERE user_id = ?`).bind(body.device_uuid));
                }
                // backup_save_log 子表无 user_id 列，用 IN 子查询按 backup_id 级联删除
                mergeMutations.push(env.DB.prepare('DELETE FROM backup_unlocked_levels WHERE backup_id IN (SELECT id FROM backup_save_log WHERE user_id = ?)').bind(body.device_uuid));
                mergeMutations.push(env.DB.prepare('DELETE FROM backup_save_items WHERE backup_id IN (SELECT id FROM backup_save_log WHERE user_id = ?)').bind(body.device_uuid));
                mergeMutations.push(env.DB.prepare('DELETE FROM users WHERE id = ?').bind(body.device_uuid));
                await env.DB.batch(mergeMutations);
            }
        }

        // 生成 Access Token (过期时间 2 小时) 与 Refresh Token (过期时间 30 天)
        const token = await generateJWT({ userId: user.id, phone: user.phone, type: 'access', exp: Date.now() + 7200000 });
        const refreshToken = await generateJWT({ userId: user.id, phone: user.phone, type: 'refresh', exp: Date.now() + 30 * 86400000 });

        // 直接使用已合并查询的结果（无需额外 D1 调用）
        return new Response(JSON.stringify({
            success: true, token, refreshToken, user,
            unlocked_levels: finalLevels,
            items: finalItems,
            today_signed: finalSigned,
            sign_streak: finalStreak,
            hasPassword: !!(user?.password_hash)
        }), { headers: corsHeaders });
    }

    /**
     * POST /api/auth/refresh — 刷新Token（双Token静默续期）
     *
     * 请求体 (JSON):
     *   @param {string} refreshToken — 已有的 Refresh Token
     *
     * 响应: { success: true, token: "<new_access>", refreshToken: "<new_refresh>" }
     */
    if (path === '/api/auth/refresh' && request.method === 'POST') {
        const body: { refreshToken: string } = await request.json();
        if (!body.refreshToken) return new Response(JSON.stringify({ error: '缺少刷新令牌' }), { status: 400, headers: corsHeaders });

        // 验证 Refresh Token 合法性与过期时间
        const payload = await verifyJWT(body.refreshToken);
        if (!payload || payload.type !== 'refresh') return new Response(JSON.stringify({ error: '登录凭证已过期，请重新登录' }), { status: 401, headers: corsHeaders });

        // 重新签发一组新的双 Token
        const newAccessToken = await generateJWT({ userId: payload.userId, phone: payload.phone, type: 'access', exp: Date.now() + 7200000 });
        const newRefreshToken = await generateJWT({ userId: payload.userId, phone: payload.phone, type: 'refresh', exp: Date.now() + 30 * 86400000 });

        return new Response(JSON.stringify({ success: true, token: newAccessToken, refreshToken: newRefreshToken }), { headers: corsHeaders });
    }

    /**
     * POST /api/auth/register — 密码注册
     *
     * 请求体 (JSON):
     *   @param {string} phone — 手机号
     *   @param {string} password — 密码（6-20位）
     *   @param {string} code — 6位验证码
     *
     * 响应: { success: true, token, refreshToken, user, ... }
     */
    if (path === '/api/auth/register' && request.method === 'POST') {
        const body: { phone: string; password: string; code: string } = await request.json();
        if (!body.phone || !body.password || !body.code)
            return new Response(JSON.stringify({ error: '请填写完整注册信息' }), { status: 400, headers: corsHeaders });

        // 验证码校验
        const codeRecord = await env.DB.prepare('SELECT code, created_at FROM login_token WHERE phone = ?')
            .bind(body.phone).first<{ code: string; created_at: number }>();
        if (!codeRecord || codeRecord.code !== body.code)
            return new Response(JSON.stringify({ error: '验证码错误' }), { status: 400, headers: corsHeaders });
        if (Date.now() - codeRecord.created_at > 5 * 60 * 1000)
            return new Response(JSON.stringify({ error: '验证码已过期' }), { status: 400, headers: corsHeaders });

        // 检查手机号是否已注册
        const existingUser = await env.DB.prepare('SELECT id FROM users WHERE phone = ?')
            .bind(body.phone).first<{ id: string }>();
        if (existingUser)
            return new Response(JSON.stringify({ error: '该手机号已注册' }), { status: 400, headers: corsHeaders });

        // 清除验证码
        env.DB.prepare('DELETE FROM login_token WHERE phone = ?').bind(body.phone).run().catch(() => {});

        // 哈希密码
        const pwHash = await hashPassword(body.password);

        const chinaToday = new Date(Date.now() + 8 * 3600000).toISOString().split('T')[0];
        const uuid = crypto.randomUUID();
        const defaultUsername = `国风玩家_${body.phone.slice(-4)}`;

        const insertQueries = [
            env.DB.prepare('INSERT INTO users (id, phone, username, role, points, password_hash, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)')
                .bind(uuid, body.phone, defaultUsername, 'user', 0, pwHash, Date.now()),
            env.DB.prepare('INSERT INTO level_unlock (user_id, level_id, unlocked_at) VALUES (?, 1, ?)').bind(uuid, Date.now())
        ];
        for (const item of ['UNDO', 'SHUFFLE', 'MOVEOUT', 'REVIVE']) {
            insertQueries.push(env.DB.prepare('INSERT INTO user_items (user_id, item_type, count) VALUES (?, ?, ?)').bind(uuid, item, 1));
        }
        await env.DB.batch(insertQueries);

        const user = { id: uuid, phone: body.phone, username: defaultUsername, avatar: null, points: 0 };
        const token = await generateJWT({ userId: uuid, phone: body.phone, type: 'access', exp: Date.now() + 7200000 });
        const refreshToken = await generateJWT({ userId: uuid, phone: body.phone, type: 'refresh', exp: Date.now() + 30 * 86400000 });

        return new Response(JSON.stringify({
            success: true, token, refreshToken, user,
            unlocked_levels: [1],
            items: [{ item_type: 'UNDO', count: 1 }, { item_type: 'SHUFFLE', count: 1 }, { item_type: 'MOVEOUT', count: 1 }, { item_type: 'REVIVE', count: 1 }],
            today_signed: false, sign_streak: 0, hasPassword: true
        }), { headers: corsHeaders });
    }

    /**
     * POST /api/auth/login-password — 密码登录
     *
     * 请求体 (JSON):
     *   @param {string} phone — 手机号
     *   @param {string} password — 密码
     *
     * 响应: { success, token, refreshToken, user, unlocked_levels, items, today_signed, sign_streak, hasPassword }
     */
    if (path === '/api/auth/login-password' && request.method === 'POST') {
        const body: { phone: string; password: string } = await request.json();
        if (!body.phone || !body.password)
            return new Response(JSON.stringify({ error: '请填写手机号和密码' }), { status: 400, headers: corsHeaders });

        const chinaToday = new Date(Date.now() + 8 * 3600000).toISOString().split('T')[0];

        const [userRow, unlockedResult, itemsResult, signTodayRow, lastSignResult] = await env.DB.batch([
            env.DB.prepare('SELECT id, phone, username, avatar_url AS avatar, points, password_hash FROM users WHERE phone = ?').bind(body.phone),
            env.DB.prepare('SELECT level_id FROM level_unlock WHERE user_id = (SELECT id FROM users WHERE phone = ?)').bind(body.phone),
            env.DB.prepare('SELECT item_type, count FROM user_items WHERE user_id = (SELECT id FROM users WHERE phone = ?)').bind(body.phone),
            env.DB.prepare('SELECT 1 FROM sign_record WHERE user_id = (SELECT id FROM users WHERE phone = ?) AND sign_date = ?').bind(body.phone, chinaToday),
            env.DB.prepare('SELECT streak FROM sign_record WHERE user_id = (SELECT id FROM users WHERE phone = ?) ORDER BY sign_date DESC LIMIT 1').bind(body.phone)
        ]);

        const user = userRow.results[0] as any;
        if (!user)
            return new Response(JSON.stringify({ error: '用户不存在' }), { status: 400, headers: corsHeaders });

        // 验证密码
        if (!user.password_hash)
            return new Response(JSON.stringify({ error: '尚未设置密码，请使用验证码登录' }), { status: 400, headers: corsHeaders });

        const pwValid = await verifyPassword(body.password, user.password_hash);
        if (!pwValid)
            return new Response(JSON.stringify({ error: '密码错误' }), { status: 400, headers: corsHeaders });

        const token = await generateJWT({ userId: user.id, phone: user.phone, type: 'access', exp: Date.now() + 7200000 });
        const refreshToken = await generateJWT({ userId: user.id, phone: user.phone, type: 'refresh', exp: Date.now() + 30 * 86400000 });

        return new Response(JSON.stringify({
            success: true, token, refreshToken, user,
            unlocked_levels: unlockedResult.results.map((r: any) => r.level_id),
            items: itemsResult.results,
            today_signed: signTodayRow.results[0] !== null && signTodayRow.results[0] !== undefined,
            sign_streak: (lastSignResult.results[0] as any)?.streak || 0,
            hasPassword: true
        }), { headers: corsHeaders });
    }

    /**
     * POST /api/auth/reset-password — 重置密码
     *
     * 请求体 (JSON):
     *   @param {string} phone — 手机号
     *   @param {string} code — 6位验证码
     *   @param {string} newPassword — 新密码
     *
     * 响应: { success: true }
     */
    if (path === '/api/auth/reset-password' && request.method === 'POST') {
        const body: { phone: string; code: string; newPassword: string } = await request.json();
        if (!body.phone || !body.code || !body.newPassword)
            return new Response(JSON.stringify({ error: '请填写完整信息' }), { status: 400, headers: corsHeaders });

        // 验证码校验
        const codeRecord = await env.DB.prepare('SELECT code, created_at FROM login_token WHERE phone = ?')
            .bind(body.phone).first<{ code: string; created_at: number }>();
        if (!codeRecord || codeRecord.code !== body.code)
            return new Response(JSON.stringify({ error: '验证码错误' }), { status: 400, headers: corsHeaders });
        if (Date.now() - codeRecord.created_at > 5 * 60 * 1000)
            return new Response(JSON.stringify({ error: '验证码已过期' }), { status: 400, headers: corsHeaders });

        // 清除验证码
        env.DB.prepare('DELETE FROM login_token WHERE phone = ?').bind(body.phone).run().catch(() => {});

        const pwHash = await hashPassword(body.newPassword);
        await env.DB.prepare('UPDATE users SET password_hash = ? WHERE phone = ?').bind(pwHash, body.phone).run();

        return new Response(JSON.stringify({ success: true }), { headers: corsHeaders });
    }

    /**
     * GET /api/auth/check-password — 检查用户是否已设置密码
     *
     * 请求头:
     *   Authorization: Bearer <token>
     *
     * 响应: { hasPassword: boolean }
     */
    if (path === '/api/auth/check-password' && request.method === 'GET') {
        const authHeader = request.headers.get('Authorization');
        if (!authHeader || !authHeader.startsWith('Bearer '))
            return new Response(JSON.stringify({ error: '未授权，请登录' }), { status: 401, headers: corsHeaders });

        const token = authHeader.substring(7);
        const payload = await verifyJWT(token);
        if (!payload || payload.type !== 'access')
            return new Response(JSON.stringify({ error: '未授权，请登录' }), { status: 401, headers: corsHeaders });

        const user = await env.DB.prepare('SELECT password_hash FROM users WHERE id = ?')
            .bind(payload.userId).first<{ password_hash: string | null }>();

        return new Response(JSON.stringify({ hasPassword: !!(user?.password_hash) }), { headers: corsHeaders });
    }

    /**
     * POST /api/auth/set-password — 设置密码（奖励 50 积分）
     *
     * 请求头:
     *   Authorization: Bearer <token>
     *
     * 请求体 (JSON):
     *   @param {string} password — 新密码
     *
     * 响应: { success: true, reward_points: 50 }
     */
    if (path === '/api/auth/set-password' && request.method === 'POST') {
        const authHeader = request.headers.get('Authorization');
        if (!authHeader || !authHeader.startsWith('Bearer '))
            return new Response(JSON.stringify({ error: '未授权，请登录' }), { status: 401, headers: corsHeaders });

        const token = authHeader.substring(7);
        const payload = await verifyJWT(token);
        if (!payload || payload.type !== 'access')
            return new Response(JSON.stringify({ error: '未授权，请登录' }), { status: 401, headers: corsHeaders });

        const body: { password: string } = await request.json();
        if (!body.password)
            return new Response(JSON.stringify({ error: '请填写密码' }), { status: 400, headers: corsHeaders });

        // 检查是否已设置密码
        const user = await env.DB.prepare('SELECT password_hash, points FROM users WHERE id = ?')
            .bind(payload.userId).first<{ password_hash: string | null; points: number }>();
        if (user?.password_hash)
            return new Response(JSON.stringify({ error: '密码已设置' }), { status: 400, headers: corsHeaders });

        const pwHash = await hashPassword(body.password);
        const newPoints = (user?.points || 0) + 50;

        await env.DB.batch([
            env.DB.prepare('UPDATE users SET password_hash = ?, points = ? WHERE id = ?').bind(pwHash, newPoints, payload.userId),
            env.DB.prepare('INSERT INTO point_record (user_id, type, amount, source, remaining_points, created_at) VALUES (?, ?, ?, ?, ?, ?)')
                .bind(payload.userId, 'IN', 50, 'SET_PASSWORD', newPoints, Date.now())
        ]);

        return new Response(JSON.stringify({ success: true, reward_points: 50, current_points: newPoints }), { headers: corsHeaders });
    }

    return null;
}