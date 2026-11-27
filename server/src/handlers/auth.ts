import { Env } from '../types';
import { getCorsHeaders } from '../helpers';
import { generateJWT, verifyJWT } from '../crypto';
import { finishPlayerLogin, newPlayer } from '../lib/player-login';
import { hashPassword, verifyPassword } from '../auth-utils';

// Consume only the same issuance that was checked, including when a resend races verification.
async function consumeCode(env: Env, phone: string, code: string, issuedAt: number): Promise<boolean> {
    const result = await env.DB.prepare('DELETE FROM login_token WHERE phone=? AND code=? AND created_at=? AND created_at>=?')
        .bind(phone, code, issuedAt, Date.now() - 5 * 60 * 1000).run();
    return result.meta.changes === 1;
}

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
     * 响应: { success: true, code: "" }（保留旧版兼容字段）
     */
    if (path === '/api/auth/send-code' && request.method === 'POST') {
        const body: { phone: string } = await request.json();
        // 参数校验：手机号必填，缺失返回 400 Bad Request
        if (!body.phone) return new Response(JSON.stringify({ error: '请填写手机号' }), { status: 400, headers: corsHeaders });

        if (typeof body.phone !== 'string' || !/^1\d{10}$/.test(body.phone)) return Response.json({ error: '手机号格式错误' }, { status: 400, headers: corsHeaders });
        const gateway = env.SMS_DELIVERY;
        const endpoint = gateway ? 'https://sms.internal/verification' : env.SMS_DELIVERY_URL;
        if (!endpoint || (!gateway && (!env.SMS_DELIVERY_TOKEN || !endpoint.startsWith('https://')))) return Response.json({ error: '短信服务暂不可用' }, { status: 503, headers: corsHeaders });

        const issuedAt = Date.now();
        const code = String(100000 + crypto.getRandomValues(new Uint32Array(1))[0] % 900000);
        const reserved = await env.DB.prepare(`
            INSERT INTO login_token(phone, code, created_at) VALUES(?, ?, ?)
            ON CONFLICT(phone) DO UPDATE SET code=excluded.code, created_at=excluded.created_at
            WHERE login_token.created_at <= excluded.created_at - 60000
        `).bind(body.phone, code, issuedAt).run();
        if (!reserved.meta.changes) return Response.json({ error: '请稍后再获取验证码' }, { status: 429, headers: corsHeaders });
        try {
            const headers = new Headers({ 'Content-Type': 'application/json' });
            if (!gateway) headers.set('Authorization', 'Bearer ' + env.SMS_DELIVERY_TOKEN);
            const deliveryRequest = new Request(endpoint, {
                method: 'POST', headers, redirect: 'manual',
                body: JSON.stringify({ phone: body.phone, code }), signal: AbortSignal.timeout(8000)
            });
            const receipt = gateway ? await gateway.fetch(deliveryRequest) : await fetch(deliveryRequest);
            if (!receipt.ok || (await receipt.json<{ accepted?: boolean }>()).accepted !== true) {
                throw new Error('SMS delivery not accepted');
            }
        } catch {
            await env.DB.prepare('DELETE FROM login_token WHERE phone=? AND code=? AND created_at=?')
                .bind(body.phone, code, issuedAt).run();
            return Response.json({ error: '验证码发送失败，请重试' }, { status: 502, headers: corsHeaders });
        }
        // Keep the string field for previously published APKs that require it.
        return Response.json({ success: true, code: '' }, { headers: corsHeaders });
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
        // 参数校验：手机号与验证码必须同时存在，缺失返回 400
        if (!body.phone || !body.code) return new Response(JSON.stringify({ error: '请填写手机号和验证码' }), { status: 400, headers: corsHeaders });

        const tokenRecord = await env.DB.prepare('SELECT code, created_at FROM login_token WHERE phone=?').bind(body.phone)
            .first<{code:string;created_at:number}>();
        if (!tokenRecord || tokenRecord.code !== body.code)
            return new Response(JSON.stringify({ error: '验证码错误' }), { status: 400, headers: corsHeaders });
        if (Date.now() - tokenRecord.created_at > 5 * 60 * 1000)
            return new Response(JSON.stringify({ error: '验证码已过期' }), { status: 400, headers: corsHeaders });

        // 核销本次验证码，再执行账号操作
        if (!await consumeCode(env, body.phone, body.code, tokenRecord.created_at))
            return Response.json({ error: '验证码已失效，请重新获取' }, { status: 400, headers: corsHeaders });

        const existing = await env.DB.prepare('SELECT id FROM users WHERE phone=?').bind(body.phone).first<{id:string}>();
        const id = existing?.id ?? crypto.randomUUID();
        return finishPlayerLogin(env, id, body.device_uuid, existing ? [] : newPlayer(env.DB, id, body.phone, null));
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

        // 校验 Refresh Token：先 verifyJWT 验签并确认未过期（exp），再校验 type==='refresh'，否则返回 401（凭证失效需重新登录）
        const payload = await verifyJWT(body.refreshToken);
        if (!payload || payload.type !== 'refresh') return new Response(JSON.stringify({ error: '登录凭证已过期，请重新登录' }), { status: 401, headers: corsHeaders });

        // 重新签发一组新的双 Token
        const account = await env.DB.prepare('SELECT 1 FROM users WHERE id=?').bind(payload.userId).first();
        if (!account) return Response.json({ error: '登录凭证已失效，请重新登录' }, { status: 401, headers: corsHeaders });
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
        const body: { phone: string; password: string; code: string; device_uuid?: string } = await request.json();
        // 参数校验：手机号、密码、验证码三者必填，缺失返回 400
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
        if (!await consumeCode(env, body.phone, body.code, codeRecord.created_at))
            return Response.json({ error: '验证码已失效，请重新获取' }, { status: 400, headers: corsHeaders });

        // 密码安全：单向加盐哈希（hashPassword）后落库 password_hash，明文密码绝不在 DB 中持久化
        const pwHash = await hashPassword(body.password);

        const id = crypto.randomUUID();
        return finishPlayerLogin(env, id, body.device_uuid, newPlayer(env.DB, id, body.phone, pwHash));
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
        const body: { phone: string; password: string; device_uuid?: string } = await request.json();
        // 参数校验：手机号与密码必填，缺失返回 400
        if (!body.phone || !body.password)
            return new Response(JSON.stringify({ error: '请填写手机号和密码' }), { status: 400, headers: corsHeaders });

        const user = await env.DB.prepare('SELECT id,password_hash FROM users WHERE phone=?').bind(body.phone)
            .first<{id:string;password_hash:string|null}>();
        if (!user)
            return new Response(JSON.stringify({ error: '用户不存在' }), { status: 400, headers: corsHeaders });

        // 验证密码：verifyPassword 用同一盐值重算哈希并与存储值比对（恒定时间比较，防时序攻击）
        if (!user.password_hash)
            return new Response(JSON.stringify({ error: '尚未设置密码，请使用验证码登录' }), { status: 400, headers: corsHeaders });

        const pwValid = await verifyPassword(body.password, user.password_hash);
        if (!pwValid)
            return new Response(JSON.stringify({ error: '密码错误' }), { status: 400, headers: corsHeaders });

        return finishPlayerLogin(env, user.id, body.device_uuid);
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
        // 参数校验：手机号、验证码、新密码三者必填，缺失返回 400
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
        if (!await consumeCode(env, body.phone, body.code, codeRecord.created_at))
            return Response.json({ error: '验证码已失效，请重新获取' }, { status: 400, headers: corsHeaders });

        // 密码安全：单向加盐哈希后更新 users.password_hash（按手机号定位），明文密码不入 DB
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
        // 鉴权：校验 Authorization: Bearer <accessToken>，缺失或非法返回 401；随后 verifyJWT 验签并确认 type==='access'
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

        // 事务边界：设置密码 + 赠送 50 积分 + 写积分流水（source=SET_PASSWORD）合并单 batch 原子提交
        await env.DB.batch([
            env.DB.prepare('UPDATE users SET password_hash = ?, points = ? WHERE id = ?').bind(pwHash, newPoints, payload.userId),
            env.DB.prepare('INSERT INTO point_record (user_id, type, amount, source, remaining_points, created_at) VALUES (?, ?, ?, ?, ?, ?)')
                .bind(payload.userId, 'IN', 50, 'SET_PASSWORD', newPoints, Date.now())
        ]);

        return new Response(JSON.stringify({ success: true, reward_points: 50, current_points: newPoints }), { headers: corsHeaders });
    }

    return null;
}