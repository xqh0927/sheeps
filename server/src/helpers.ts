import { Env } from './types';
import { verifyJWT } from './crypto';

/** 管理员角色枚举 */
export type AdminRole = 'super' | 'operator' | 'readonly';

/** 管理员 JWT 解析后的身份信息 */
export interface AdminPayload {
  userId: string;
  phone: string;
  role: AdminRole;
  type: 'access' | 'refresh';
  exp?: number;
}

/** 合法的管理员角色集合 */
const ADMIN_ROLES: AdminRole[] = ['super', 'operator', 'readonly'];

/**
 * 获取跨域资源共享 (CORS) 请求头
 * 优先使用 env.ADMIN_WEB_ORIGIN（管理后台 Pages 站点的精确源），否则回退到 '*'。
 * Allow-Methods 已扩展为 GET,POST,PUT,DELETE,OPTIONS 以支撑后台写操作与预检。
 *
 * @param env 可选，传入后可精确授权管理后台域名
 */
export function getCorsHeaders(env?: Env) {
  const origin = (env && env.ADMIN_WEB_ORIGIN) || '*';
  return {
    'Access-Control-Allow-Origin': origin,
    'Access-Control-Allow-Methods': 'GET, POST, PUT, DELETE, OPTIONS',
    'Access-Control-Allow-Headers': 'Content-Type, Authorization',
    'Content-Type': 'application/json'
  };
}

/**
 * 统一构造错误响应（带 CORS 头）
 * @param message 错误描述
 * @param status HTTP 状态码
 * @param env 可选，传入后 CORS 使用精确源
 */
export function jsonError(message: string, status: number, env?: Env): Response {
  return new Response(JSON.stringify({ error: message }), { status, headers: getCorsHeaders(env) });
}

/**
 * 鉴权中间件辅助函数：从请求头解析并验证 Access Token（玩家端）
 * 保留原有玩家鉴权逻辑，不被后台改造破坏。
 * @returns 验证通过返回用户身份信息，否则返回 null
 */
export async function getAuthenticatedUser(request: Request, env: Env): Promise<{ userId: string; phone: string } | null> {
  const authHeader = request.headers.get('Authorization');
  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return null; // 未携带 Token 或格式错误
  }

  const token = authHeader.substring(7); // 截取 'Bearer ' 之后的部分
  const payload = await verifyJWT(token);

  // 确保使用的是 access token 而不是 refresh token
  if (!payload || payload.type !== 'access') {
    return null;
  }
  return payload;
}

/**
 * 管理员鉴权：校验后台 access token
 * 三级校验：type==='access' && role IN (super,operator,readonly) && is_banned=0。
 * 封禁状态可能在 token 签发之后变化，因此查库二次确认。
 *
 * @returns 成功返回 AdminPayload，失败返回 401 Response（调用方用 instanceof Response 判断）
 */
export async function requireAdmin(request: Request, env: Env): Promise<AdminPayload | Response> {
  const authHeader = request.headers.get('Authorization');
  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return jsonError('未提供管理员凭证', 401, env);
  }

  const token = authHeader.substring(7).trim();
  const payload = await verifyJWT(token);

  if (!payload || payload.type !== 'access' || !ADMIN_ROLES.includes(payload.role)) {
    return jsonError('无效的管理员凭证', 401, env);
  }

  // 封禁状态可能晚于 token 签发发生变化，查库二次确认
  const row = await env.DB.prepare('SELECT is_banned FROM users WHERE id = ?').bind(payload.userId).first<{ is_banned: number }>();
  if (!row || row.is_banned === 1) {
    return jsonError('管理员账号已被封禁', 401, env);
  }

  return payload as AdminPayload;
}

/**
 * 写操作守卫：readonly 角色调用写接口 → 403
 * @returns 无权时返回 403 Response，否则返回 null
 */
export function assertCanWrite(payload: AdminPayload): Response | null {
  if (payload.role === 'readonly') {
    return jsonError('无写权限（当前为只读角色）', 403);
  }
  return null;
}

/**
 * 超级守卫：非 super 角色调用 super 专属接口 → 403
 * @returns 无权时返回 403 Response，否则返回 null
 */
export function assertSuper(payload: AdminPayload): Response | null {
  if (payload.role !== 'super') {
    return jsonError('仅超级管理员可执行此操作', 403);
  }
  return null;
}

/**
 * 国际化辅助：根据请求头的 Accept-Language 决定返回的内容语言后缀
 */
export function getLangSuffix(request: Request): string {
  const acceptLang = request.headers.get('Accept-Language') || 'zh';
  if (acceptLang.includes('en')) return 'en';
  if (acceptLang.includes('TW') || acceptLang.includes('rTW') || acceptLang.includes('tw') || acceptLang.includes('Hant')) return 'tw';
  if (acceptLang.includes('ja')) return 'ja';
  if (acceptLang.includes('ko')) return 'ko';
  return ''; // 默认简体中文无后缀
}

/**
 * 读取系统配置值（优先 KV 缓存，未命中查 D1 并回填缓存）
 * 用于签到奖励、解锁积分门槛等低频变更的配置项
 */
export async function getCachedConfig(env: Env, key: string, defaultValue: string): Promise<string> {
  const cacheKey = `config_${key}`;
  const cached = await env.SHEEPS_CACHE.get(cacheKey);
  if (cached !== null && cached !== undefined) return cached;

  const row = await env.DB.prepare('SELECT value FROM config WHERE key = ?').bind(key).first<{ value: string }>();
  const value = row?.value ?? defaultValue;
  // 异步写 KV，不阻塞主流程
  env.SHEEPS_CACHE.put(cacheKey, value, { expirationTtl: 600 }).catch(() => {});
  return value;
}

/**
 * 错误信息国际化翻译（占位实现，后续扩展多语言映射）
 */
export function translateErrorMessage(error: string, lang: string): string {
  // TODO: 后续实现多语言错误信息映射表
  return error;
}
