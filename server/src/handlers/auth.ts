import { Env } from '../types';
import { getCorsHeaders } from '../helpers';
import { generateJWT, verifyJWT } from '../crypto';

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

    // 1. 发送登录验证码接口
    if (path === '/api/auth/send-code' && request.method === 'POST') {
        const body: { phone: string } = await request.json();
        if (!body.phone) return new Response(JSON.stringify({ error: 'Missing phone number' }), { status: 400, headers: corsHeaders });

        // 生成 6 位随机数字验证码
        const code = Math.floor(100000 + Math.random() * 900000).toString();
        // 将验证码写入 login_token 表，并记录当前毫秒级时间戳以实现过期校验
        await env.DB.prepare('INSERT OR REPLACE INTO login_token (phone, code, created_at) VALUES (?, ?, ?)').bind(body.phone, code, Date.now()).run();
        return new Response(JSON.stringify({ success: true, code }), { headers: corsHeaders });
    }

    // 2. 验证码登录/注册接口
    if (path === '/api/auth/login' && request.method === 'POST') {
        const body: { phone: string; code: string; device_uuid?: string } = await request.json();
        if (!body.phone || !body.code) return new Response(JSON.stringify({ error: 'Missing parameters' }), { status: 400, headers: corsHeaders });

        const chinaToday = new Date(Date.now() + 8 * 3600000).toISOString().split('T')[0];

        // ─── 合并查询：一次 D1 batch 完成所有读操作 ───
        const [codeRecord, userRow, unlockedResult, itemsResult, signTodayRow, lastSignResult] = await env.DB.batch([
            env.DB.prepare('SELECT code, created_at FROM login_token WHERE phone = ?').bind(body.phone),
            env.DB.prepare('SELECT id, phone, username, avatar, points FROM users WHERE phone = ?').bind(body.phone),
            env.DB.prepare('SELECT level_id FROM level_unlock WHERE user_id = (SELECT id FROM users WHERE phone = ?)').bind(body.phone),
            env.DB.prepare('SELECT item_type, count FROM user_items WHERE user_id = (SELECT id FROM users WHERE phone = ?)').bind(body.phone),
            env.DB.prepare('SELECT 1 FROM sign_record WHERE user_id = (SELECT id FROM users WHERE phone = ?) AND sign_date = ?').bind(body.phone, chinaToday),
            env.DB.prepare('SELECT streak FROM sign_record WHERE user_id = (SELECT id FROM users WHERE phone = ?) ORDER BY sign_date DESC LIMIT 1').bind(body.phone)
        ]);

        const tokenRecord = codeRecord.results[0] as { code: string; created_at: number } | undefined;
        if (!tokenRecord || tokenRecord.code !== body.code)
            return new Response(JSON.stringify({ error: 'Verification code incorrect' }), { status: 400, headers: corsHeaders });
        if (Date.now() - tokenRecord.created_at > 5 * 60 * 1000)
            return new Response(JSON.stringify({ error: 'Verification code expired' }), { status: 400, headers: corsHeaders });

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
                const tablesToDelete = ['level_unlock', 'user_items', 'leaderboard', 'point_record', 'exchange_record', 'sign_record', 'user_task', 'backup_save_log'];
                for (const table of tablesToDelete) mergeMutations.push(env.DB.prepare(`DELETE FROM ${table} WHERE user_id = ?`).bind(body.device_uuid));
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
            sign_streak: finalStreak
        }), { headers: corsHeaders });
    }

    // 3. 双 Token 静默刷新接口
    if (path === '/api/auth/refresh' && request.method === 'POST') {
        const body: { refreshToken: string } = await request.json();
        if (!body.refreshToken) return new Response(JSON.stringify({ error: 'Missing refresh token' }), { status: 400, headers: corsHeaders });

        // 验证 Refresh Token 合法性与过期时间
        const payload = await verifyJWT(body.refreshToken);
        if (!payload || payload.type !== 'refresh') return new Response(JSON.stringify({ error: 'Invalid or expired refresh token' }), { status: 401, headers: corsHeaders });

        // 重新签发一组新的双 Token
        const newAccessToken = await generateJWT({ userId: payload.userId, phone: payload.phone, type: 'access', exp: Date.now() + 7200000 });
        const newRefreshToken = await generateJWT({ userId: payload.userId, phone: payload.phone, type: 'refresh', exp: Date.now() + 30 * 86400000 });

        return new Response(JSON.stringify({ success: true, token: newAccessToken, refreshToken: newRefreshToken }), { headers: corsHeaders });
    }

    return null;
}