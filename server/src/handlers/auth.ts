import { Env } from '../types';
import { getCorsHeaders, getLangSuffix } from '../helpers';
import { generateJWT, verifyJWT } from '../crypto';
import { finishPlayerLogin, newPlayer } from '../lib/player-login';
import { hashPassword, verifyPassword } from '../auth-utils';
import {
    EmailServiceUnavailableError,
    checkEmailRateLimit,
    clearPasswordLoginFailures,
    consumeEmailCode,
    isPasswordLoginLocked,
    normalizeEmail,
    recordPasswordLoginFailure,
    sendVerificationEmail,
    verifyEmailCodeRecord,
} from '../lib/email-delivery';

/**
 * 处理所有与身份认证相关的 HTTP 路由请求
 *
 * 认证策略：
 * - 验证码登录 / 注册 / 重置密码统一采用「邮箱 + 6 位验证码」（Bird Email API）
 * - 密码登录支持「邮箱或手机号 + 密码」双标识登录，带 5 次失败 15 分钟锁定
 * - 登录 / 注册 / 密码登录统一通过 finishPlayerLogin 原子合并游客数据与签发双 Token
 */
export async function handleAuthRoutes(request: Request, env: Env, path: string): Promise<Response | null> {
    const corsHeaders = getCorsHeaders();

    /**
     * POST /api/auth/send-code — 发送邮箱验证码
     */
    if (path === '/api/auth/send-code' && request.method === 'POST') {
        const body: { email?: string } = await request.json();
        if (!body.email) {
            return Response.json({ error: '请填写邮箱' }, { status: 400, headers: corsHeaders });
        }

        const email = normalizeEmail(body.email);
        if (!email) {
            return Response.json({ error: '邮箱格式错误' }, { status: 400, headers: corsHeaders });
        }

        if (!env.BIRD_API_KEY?.trim() || !env.EMAIL_SENDER?.trim()) {
            return Response.json({ error: '邮件服务暂不可用' }, { status: 503, headers: corsHeaders });
        }

        const clientIp = request.headers.get('CF-Connecting-IP') || request.headers.get('X-Forwarded-For')?.split(',')[0]?.trim() || '';
        const allowed = await checkEmailRateLimit(env, email, clientIp);
        if (!allowed) {
            return Response.json({ error: '请稍后再获取验证码' }, { status: 429, headers: corsHeaders });
        }

        const issuedAt = Date.now();
        const code = String(100000 + crypto.getRandomValues(new Uint32Array(1))[0] % 900000);
        const reserved = await env.DB.prepare(`
            INSERT INTO email_verification_codes(email, code, created_at) VALUES(?, ?, ?)
            ON CONFLICT(email) DO UPDATE SET code = excluded.code, created_at = excluded.created_at
            WHERE email_verification_codes.created_at <= excluded.created_at - 60000
        `).bind(email, code, issuedAt).run();
        if (!reserved.meta.changes) {
            return Response.json({ error: '请稍后再获取验证码' }, { status: 429, headers: corsHeaders });
        }

        try {
            await sendVerificationEmail(env, email, code, getLangSuffix(request));
        } catch (err) {
            await env.DB.prepare('DELETE FROM email_verification_codes WHERE email = ? AND code = ? AND created_at = ?')
                .bind(email, code, issuedAt).run();
            if (err instanceof EmailServiceUnavailableError) {
                return Response.json({ error: '邮件服务暂不可用' }, { status: 503, headers: corsHeaders });
            }
            return Response.json({ error: '验证码发送失败，请重试' }, { status: 502, headers: corsHeaders });
        }

        return Response.json({ success: true, code: '' }, { headers: corsHeaders });
    }

    /**
     * POST /api/auth/login — 邮箱验证码登录/自动注册
     */
    if (path === '/api/auth/login' && request.method === 'POST') {
        const body: { email?: string; code?: string; device_uuid?: string } = await request.json();
        if (!body.email || !body.code) {
            return Response.json({ error: '请填写邮箱和验证码' }, { status: 400, headers: corsHeaders });
        }

        const email = normalizeEmail(body.email);
        if (!email) {
            return Response.json({ error: '邮箱格式错误' }, { status: 400, headers: corsHeaders });
        }

        const [codeResult, userResult] = await env.DB.batch([
            env.DB.prepare('SELECT code, created_at FROM email_verification_codes WHERE email = ?').bind(email),
            env.DB.prepare('SELECT id FROM users WHERE email = ?').bind(email)
        ]);

        const tokenRecord = codeResult.results[0] as { code: string; created_at: number } | undefined;
        const verified = await verifyEmailCodeRecord(env, email, body.code, tokenRecord);
        if (!verified.ok) {
            return Response.json({ error: verified.error }, { status: 400, headers: corsHeaders });
        }

        if (!await consumeEmailCode(env, email, body.code, verified.issuedAt)) {
            return Response.json({ error: '验证码已失效，请重新获取' }, { status: 400, headers: corsHeaders });
        }

        const existing = userResult.results[0] as { id: string } | undefined;
        const id = existing ? existing.id : crypto.randomUUID();
        const initial = existing ? [] : newPlayer(env.DB, id, email, null);
        return finishPlayerLogin(env, id, body.device_uuid, initial);
    }

    /**
     * POST /api/auth/refresh — 刷新双 Token
     */
    if (path === '/api/auth/refresh' && request.method === 'POST') {
        const body: { refreshToken?: string } = await request.json();
        if (!body.refreshToken) {
            return Response.json({ error: '缺少刷新令牌' }, { status: 400, headers: corsHeaders });
        }

        const payload = await verifyJWT(body.refreshToken);
        if (!payload || payload.type !== 'refresh') {
            return Response.json({ error: '登录凭证已过期，请重新登录' }, { status: 401, headers: corsHeaders });
        }

        const account = await env.DB.prepare('SELECT email, phone FROM users WHERE id = ?')
            .bind(payload.userId).first<{ email: string | null; phone: string | null }>();
        if (!account) {
            return Response.json({ error: '登录凭证已失效，请重新登录' }, { status: 401, headers: corsHeaders });
        }
        const newAccessToken = await generateJWT({ userId: payload.userId, email: account.email, phone: account.phone, type: 'access', exp: Date.now() + 7200000 });
        const newRefreshToken = await generateJWT({ userId: payload.userId, email: account.email, phone: account.phone, type: 'refresh', exp: Date.now() + 30 * 86400000 });

        return Response.json({ success: true, token: newAccessToken, refreshToken: newRefreshToken }, { headers: corsHeaders });
    }

    /**
     * POST /api/auth/register — 邮箱验证码 + 密码注册（支持合并游客数据）
     */
    if (path === '/api/auth/register' && request.method === 'POST') {
        const body: { email?: string; password?: string; code?: string; device_uuid?: string } = await request.json();
        if (!body.email || !body.password || !body.code) {
            return Response.json({ error: '请填写完整注册信息' }, { status: 400, headers: corsHeaders });
        }

        const email = normalizeEmail(body.email);
        if (!email) {
            return Response.json({ error: '邮箱格式错误' }, { status: 400, headers: corsHeaders });
        }

        const codeRecord = await env.DB.prepare('SELECT code, created_at FROM email_verification_codes WHERE email = ?')
            .bind(email).first<{ code: string; created_at: number }>();
        const verified = await verifyEmailCodeRecord(env, email, body.code, codeRecord);
        if (!verified.ok) {
            return Response.json({ error: verified.error }, { status: 400, headers: corsHeaders });
        }

        const existingUser = await env.DB.prepare('SELECT id FROM users WHERE email = ?')
            .bind(email).first<{ id: string }>();
        if (existingUser) {
            return Response.json({ error: '该邮箱已注册' }, { status: 400, headers: corsHeaders });
        }

        if (!await consumeEmailCode(env, email, body.code, verified.issuedAt)) {
            return Response.json({ error: '验证码已失效，请重新获取' }, { status: 400, headers: corsHeaders });
        }

        const pwHash = await hashPassword(body.password);
        const id = crypto.randomUUID();
        return finishPlayerLogin(env, id, body.device_uuid, newPlayer(env.DB, id, email, pwHash));
    }

    /**
     * POST /api/auth/login-password — 密码登录（支持邮箱或手机号 + 密码，合并游客数据）
     */
    if (path === '/api/auth/login-password' && request.method === 'POST') {
        const body: { account?: string; email?: string; phone?: string; password?: string; device_uuid?: string } = await request.json();
        const rawIdentifier = (body.account || body.email || body.phone || '').trim();
        if (!rawIdentifier || !body.password) {
            return Response.json({ error: '请填写账号和密码' }, { status: 400, headers: corsHeaders });
        }

        const isEmailLogin = rawIdentifier.includes('@');
        const lookupColumn = isEmailLogin ? 'email' : 'phone';
        const lookupValue = isEmailLogin ? normalizeEmail(rawIdentifier) : rawIdentifier;
        if (!lookupValue) {
            return Response.json({ error: '邮箱格式错误' }, { status: 400, headers: corsHeaders });
        }

        if (await isPasswordLoginLocked(env, lookupValue)) {
            return Response.json({ error: '密码错误次数过多，请 15 分钟后再试' }, { status: 429, headers: corsHeaders });
        }

        const user = await env.DB.prepare(`SELECT id, password_hash FROM users WHERE ${lookupColumn} = ?`)
            .bind(lookupValue).first<{ id: string; password_hash: string | null }>();
        if (!user) {
            return Response.json({ error: '用户不存在' }, { status: 400, headers: corsHeaders });
        }

        if (!user.password_hash) {
            return Response.json({ error: '尚未设置密码，请使用验证码登录' }, { status: 400, headers: corsHeaders });
        }

        const pwValid = await verifyPassword(body.password, user.password_hash);
        if (!pwValid) {
            await recordPasswordLoginFailure(env, lookupValue);
            return Response.json({ error: '密码错误' }, { status: 400, headers: corsHeaders });
        }
        await clearPasswordLoginFailures(env, lookupValue);

        return finishPlayerLogin(env, user.id, body.device_uuid);
    }

    /**
     * POST /api/auth/reset-password — 通过邮箱验证码重置密码
     */
    if (path === '/api/auth/reset-password' && request.method === 'POST') {
        const body: { email?: string; code?: string; newPassword?: string } = await request.json();
        if (!body.email || !body.code || !body.newPassword) {
            return Response.json({ error: '请填写完整信息' }, { status: 400, headers: corsHeaders });
        }

        const email = normalizeEmail(body.email);
        if (!email) {
            return Response.json({ error: '邮箱格式错误' }, { status: 400, headers: corsHeaders });
        }

        const codeRecord = await env.DB.prepare('SELECT code, created_at FROM email_verification_codes WHERE email = ?')
            .bind(email).first<{ code: string; created_at: number }>();
        const verified = await verifyEmailCodeRecord(env, email, body.code, codeRecord);
        if (!verified.ok) {
            return Response.json({ error: verified.error }, { status: 400, headers: corsHeaders });
        }

        const existingUser = await env.DB.prepare('SELECT id FROM users WHERE email = ?')
            .bind(email).first<{ id: string }>();
        if (!existingUser) {
            return Response.json({ error: '用户不存在' }, { status: 400, headers: corsHeaders });
        }

        if (!await consumeEmailCode(env, email, body.code, verified.issuedAt)) {
            return Response.json({ error: '验证码已失效，请重新获取' }, { status: 400, headers: corsHeaders });
        }

        const pwHash = await hashPassword(body.newPassword);
        await env.DB.prepare('UPDATE users SET password_hash = ? WHERE id = ?').bind(pwHash, existingUser.id).run();

        return Response.json({ success: true }, { headers: corsHeaders });
    }

    /**
     * GET /api/auth/check-password — 检查用户是否已设置密码
     */
    if (path === '/api/auth/check-password' && request.method === 'GET') {
        const authHeader = request.headers.get('Authorization');
        if (!authHeader || !authHeader.startsWith('Bearer ')) {
            return Response.json({ error: '未授权，请登录' }, { status: 401, headers: corsHeaders });
        }

        const token = authHeader.substring(7);
        const payload = await verifyJWT(token);
        if (!payload || payload.type !== 'access') {
            return Response.json({ error: '未授权，请登录' }, { status: 401, headers: corsHeaders });
        }

        const user = await env.DB.prepare('SELECT password_hash FROM users WHERE id = ?')
            .bind(payload.userId).first<{ password_hash: string | null }>();

        return Response.json({ hasPassword: !!(user?.password_hash) }, { headers: corsHeaders });
    }

    /**
     * POST /api/auth/set-password — 设置密码（奖励 50 积分）
     */
    if (path === '/api/auth/set-password' && request.method === 'POST') {
        const authHeader = request.headers.get('Authorization');
        if (!authHeader || !authHeader.startsWith('Bearer ')) {
            return Response.json({ error: '未授权，请登录' }, { status: 401, headers: corsHeaders });
        }

        const token = authHeader.substring(7);
        const payload = await verifyJWT(token);
        if (!payload || payload.type !== 'access') {
            return Response.json({ error: '未授权，请登录' }, { status: 401, headers: corsHeaders });
        }

        const body: { password?: string } = await request.json();
        if (!body.password) {
            return Response.json({ error: '请填写密码' }, { status: 400, headers: corsHeaders });
        }

        const user = await env.DB.prepare('SELECT password_hash, points FROM users WHERE id = ?')
            .bind(payload.userId).first<{ password_hash: string | null; points: number }>();
        if (user?.password_hash) {
            return Response.json({ error: '密码已设置' }, { status: 400, headers: corsHeaders });
        }

        const pwHash = await hashPassword(body.password);
        const newPoints = (user?.points || 0) + 50;

        await env.DB.batch([
            env.DB.prepare('UPDATE users SET password_hash = ?, points = ? WHERE id = ?').bind(pwHash, newPoints, payload.userId),
            env.DB.prepare('INSERT INTO point_record (user_id, type, amount, source, remaining_points, created_at) VALUES (?, ?, ?, ?, ?, ?)')
                .bind(payload.userId, 'IN', 50, 'SET_PASSWORD', newPoints, Date.now())
        ]);

        return Response.json({ success: true, reward_points: 50, current_points: newPoints }, { headers: corsHeaders });
    }

    return null;
}
