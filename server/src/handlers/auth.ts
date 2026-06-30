import { Env } from '../types';
import { getCorsHeaders } from '../helpers';
import { generateJWT, verifyJWT } from '../crypto';

export async function handleAuthRoutes(request: Request, env: Env, path: string): Promise<Response | null> {
    const corsHeaders = getCorsHeaders();

    if (path === '/api/auth/send-code' && request.method === 'POST') {
        const body: { phone: string } = await request.json();
        if (!body.phone) return new Response(JSON.stringify({ error: 'Missing phone number' }), { status: 400, headers: corsHeaders });

        const code = Math.floor(100000 + Math.random() * 900000).toString();
        await env.DB.prepare('INSERT OR REPLACE INTO login_token (phone, code, created_at) VALUES (?, ?, ?)').bind(body.phone, code, Date.now()).run();
        return new Response(JSON.stringify({ success: true, code }), { headers: corsHeaders });
    }

    if (path === '/api/auth/login' && request.method === 'POST') {
        const body: { phone: string; code: string; device_uuid?: string } = await request.json();
        if (!body.phone || !body.code) return new Response(JSON.stringify({ error: 'Missing parameters' }), { status: 400, headers: corsHeaders });

        const codeRecord = await env.DB.prepare('SELECT code, created_at FROM login_token WHERE phone = ?').bind(body.phone).first<{ code: string; created_at: number }>();
        if (!codeRecord || codeRecord.code !== body.code) return new Response(JSON.stringify({ error: 'Verification code incorrect' }), { status: 400, headers: corsHeaders });
        if (Date.now() - codeRecord.created_at > 5 * 60 * 1000) return new Response(JSON.stringify({ error: 'Verification code expired' }), { status: 400, headers: corsHeaders });

        const [deleteTokenResult, userResult] = await env.DB.batch([
            env.DB.prepare('DELETE FROM login_token WHERE phone = ?').bind(body.phone),
            env.DB.prepare('SELECT id, phone, username, avatar, points FROM users WHERE phone = ?').bind(body.phone)
        ]);

        let user = userResult.results[0] as any;

        if (!user) {
            const uuid = crypto.randomUUID();
            const defaultUsername = `国风玩家_${body.phone.slice(-4)}`;
            const insertQueries = [
                env.DB.prepare('INSERT INTO users (id, phone, username, avatar, points, created_at) VALUES (?, ?, ?, ?, ?, ?)').bind(uuid, body.phone, defaultUsername, null, 0, Date.now()),
                env.DB.prepare('INSERT INTO level_unlock (user_id, level_id, unlocked_at) VALUES (?, 1, ?)').bind(uuid, Date.now())
            ];
            for (const item of ['UNDO', 'SHUFFLE', 'MOVEOUT', 'REVIVE']) {
                insertQueries.push(env.DB.prepare('INSERT INTO user_items (user_id, item_type, count) VALUES (?, ?, ?)').bind(uuid, item, 1));
            }
            await env.DB.batch(insertQueries);
            user = { id: uuid, phone: body.phone, username: defaultUsername, avatar: null, points: 0 };
        }

        if (body.device_uuid && body.device_uuid !== user.id) {
            const guestUser = await env.DB.prepare('SELECT points FROM users WHERE id = ?').bind(body.device_uuid).first<{ points: number }>();
            if (guestUser) {
                const [guestLevelsResult, guestItemsResult] = await env.DB.batch([
                    env.DB.prepare('SELECT level_id FROM level_unlock WHERE user_id = ?').bind(body.device_uuid),
                    env.DB.prepare('SELECT item_type, count FROM user_items WHERE user_id = ?').bind(body.device_uuid)
                ]);
                const mergedPoints = user.points + guestUser.points;
                user.points = mergedPoints;

                const mergeMutations = [env.DB.prepare('UPDATE users SET points = ? WHERE id = ?').bind(mergedPoints, user.id)];
                for (const row of guestLevelsResult.results as any[]) mergeMutations.push(env.DB.prepare('INSERT OR IGNORE INTO level_unlock (user_id, level_id, unlocked_at) VALUES (?, ?, ?)').bind(user.id, row.level_id, Date.now()));
                for (const row of guestItemsResult.results as any[]) mergeMutations.push(env.DB.prepare('INSERT INTO user_items (user_id, item_type, count) VALUES (?, ?, ?) ON CONFLICT(user_id, item_type) DO UPDATE SET count = count + ?').bind(user.id, row.item_type, row.count, row.count));

                const tablesToDelete = ['level_unlock', 'user_items', 'leaderboard', 'point_record', 'exchange_record', 'sign_record', 'user_task', 'backup_save_log'];
                for (const table of tablesToDelete) mergeMutations.push(env.DB.prepare(`DELETE FROM ${table} WHERE user_id = ?`).bind(body.device_uuid));
                mergeMutations.push(env.DB.prepare('DELETE FROM users WHERE id = ?').bind(body.device_uuid));
                await env.DB.batch(mergeMutations);
            }
        }

        const token = await generateJWT({ userId: user.id, phone: user.phone, type: 'access', exp: Date.now() + 7200000 });
        const refreshToken = await generateJWT({ userId: user.id, phone: user.phone, type: 'refresh', exp: Date.now() + 30 * 86400000 });

        const chinaToday = new Date(Date.now() + 8 * 3600000).toISOString().split('T')[0];
        const [unlockedResult, itemsResult, signTodayResult, lastSignResult] = await env.DB.batch([
            env.DB.prepare('SELECT level_id FROM level_unlock WHERE user_id = ?').bind(user.id),
            env.DB.prepare('SELECT item_type, count FROM user_items WHERE user_id = ?').bind(user.id),
            env.DB.prepare('SELECT 1 FROM sign_record WHERE user_id = ? AND sign_date = ?').bind(user.id, chinaToday),
            env.DB.prepare('SELECT streak FROM sign_record WHERE user_id = ? ORDER BY sign_date DESC LIMIT 1').bind(user.id)
        ]);

        return new Response(JSON.stringify({
            success: true, token, refreshToken, user,
            unlocked_levels: unlockedResult.results.map((r: any) => r.level_id),
            items: itemsResult.results,
            today_signed: signTodayResult.results[0] !== null,
            sign_streak: (lastSignResult.results[0] as any)?.streak || 0
        }), { headers: corsHeaders });
    }

    if (path === '/api/auth/refresh' && request.method === 'POST') {
        const body: { refreshToken: string } = await request.json();
        if (!body.refreshToken) return new Response(JSON.stringify({ error: 'Missing refresh token' }), { status: 400, headers: corsHeaders });

        const payload = await verifyJWT(body.refreshToken);
        if (!payload || payload.type !== 'refresh') return new Response(JSON.stringify({ error: 'Invalid or expired refresh token' }), { status: 401, headers: corsHeaders });

        const newAccessToken = await generateJWT({ userId: payload.userId, phone: payload.phone, type: 'access', exp: Date.now() + 7200000 });
        const newRefreshToken = await generateJWT({ userId: payload.userId, phone: payload.phone, type: 'refresh', exp: Date.now() + 30 * 86400000 });

        return new Response(JSON.stringify({ success: true, token: newAccessToken, refreshToken: newRefreshToken }), { headers: corsHeaders });
    }

    return null;
}