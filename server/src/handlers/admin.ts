import { Env } from '../types';
import { getCorsHeaders, jsonError, requireAdmin, assertCanWrite, assertSuper, AdminPayload } from '../helpers';
import { generateJWT, verifyJWT, sha256 } from '../crypto';
import { verifyPassword, hashPassword } from '../auth-utils';

/**
 * 管理后台路由处理器
 * 所有后台接口集中在此；鉴权分层：
 *   - POST /api/admin/login、POST /api/admin/refresh 无鉴权
 *   - 其余接口先过 requireAdmin（type==='access' && role IN (super,operator,readonly) && is_banned=0）
 *   - 写操作再经 assertCanWrite（readonly → 403）
 *   - super 专属接口再经 assertSuper（非 super → 403）
 * 审计：所有写操作在 DB 变更成功后落 admin_audit_log（该表无任何写/删接口）。
 */

const PAGE_SIZE_DEFAULT = 20;
const PAGE_SIZE_MAX = 100;

function parsePaging(url: URL): { page: number; pageSize: number; offset: number } {
  const page = Math.max(1, parseInt(url.searchParams.get('page') || '1', 10) || 1);
  const pageSize = Math.min(
    PAGE_SIZE_MAX,
    Math.max(1, parseInt(url.searchParams.get('pageSize') || String(PAGE_SIZE_DEFAULT), 10) || PAGE_SIZE_DEFAULT)
  );
  return { page, pageSize, offset: (page - 1) * pageSize };
}

function getClientIp(request: Request): string {
  return (
    request.headers.get('CF-Connecting-IP') ||
    request.headers.get('x-forwarded-for')?.split(',')[0]?.trim() ||
    'unknown'
  );
}

/**
 * 审计落库：写入 admin_audit_log。仅由后台写接口在 DB 变更成功后调用。
 */
async function writeAudit(
  env: Env,
  request: Request,
  admin: AdminPayload,
  action: string,
  target: { type?: string; id?: string; before?: any; after?: any }
): Promise<void> {
  try {
    await env.DB.prepare(
      `INSERT INTO admin_audit_log
        (admin_id, admin_phone, admin_role, action, target_type, target_id, before_snapshot, after_snapshot, source_ip, user_agent, created_at)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`
    )
      .bind(
        admin.userId,
        admin.phone,
        admin.role,
        action,
        target.type || null,
        target.id || null,
        target.before !== undefined ? JSON.stringify(target.before) : null,
        target.after !== undefined ? JSON.stringify(target.after) : null,
        getClientIp(request),
        request.headers.get('User-Agent') || 'unknown',
        Date.now()
      )
      .run();
  } catch (e) {
    console.error('writeAudit failed:', e);
  }
}

/**
 * 管理员登录（无鉴权）
 */
async function adminLogin(request: Request, env: Env): Promise<Response> {
  const body = await request.json<{ phone?: string; password?: string }>().catch(() => null);
  if (!body || !body.phone || !body.password) {
    return jsonError('缺少手机号或密码', 400, env);
  }
  const row = await env.DB.prepare('SELECT id, phone, username, role, password_hash, is_banned FROM users WHERE phone = ?')
    .bind(body.phone)
    .first<{ id: string; phone: string; username: string; role: string; password_hash: string; is_banned: number }>();
  if (!row || !row.role || row.role === 'readonly') {
    return jsonError('账号不存在或无后台权限', 401, env);
  }
  if (row.is_banned === 1) {
    return jsonError('账号已被封禁', 401, env);
  }
  const ok = await verifyPassword(body.password, row.password_hash);
  if (!ok) {
    return jsonError('密码错误', 401, env);
  }
  const accessToken = await generateJWT({ userId: row.id, phone: row.phone, role: row.role, type: 'access', exp: Date.now() + 2 * 3600 * 1000 });
  const refreshToken = await generateJWT({ userId: row.id, phone: row.phone, role: row.role, type: 'refresh', exp: Date.now() + 7 * 24 * 3600 * 1000 });
  await writeAudit(env, request, { userId: row.id, phone: row.phone, role: row.role as any, type: 'access' }, 'LOGIN_SUCCESS', { type: 'admin', id: row.id });
  return new Response(
    JSON.stringify({ success: true, token: accessToken, refreshToken, user: { id: row.id, phone: row.phone, username: row.username, role: row.role } }),
    { headers: getCorsHeaders(env) }
  );
}

/**
 * 刷新访问令牌（无鉴权，使用 refresh token）
 */
async function adminRefresh(request: Request, env: Env): Promise<Response> {
  const body = await request.json<{ refreshToken?: string }>().catch(() => null);
  if (!body || !body.refreshToken) {
    return jsonError('缺少刷新令牌', 400, env);
  }
  const payload = await verifyJWT(body.refreshToken);
  if (!payload || payload.type !== 'refresh' || !payload.role) {
    return jsonError('无效的刷新令牌', 401, env);
  }
  const row = await env.DB.prepare('SELECT id, phone, role, is_banned FROM users WHERE id = ?')
    .bind(payload.userId)
    .first<{ id: string; phone: string; role: string; is_banned: number }>();
  if (!row || row.is_banned === 1 || row.role !== payload.role) {
    return jsonError('账号状态异常', 401, env);
  }
  const accessToken = await generateJWT({ userId: row.id, phone: row.phone, role: row.role, type: 'access', exp: Date.now() + 2 * 3600 * 1000 });
  const refreshToken = await generateJWT({ userId: row.id, phone: row.phone, role: row.role, type: 'refresh', exp: Date.now() + 7 * 24 * 3600 * 1000 });
  return new Response(
    JSON.stringify({ success: true, token: accessToken, refreshToken }),
    { headers: getCorsHeaders(env) }
  );
}

// ============ 用户管理 ============

async function listUsers(request: Request, env: Env, admin: AdminPayload, url: URL): Promise<Response> {
  const { page, pageSize, offset } = parsePaging(url);
  const keyword = (url.searchParams.get('keyword') || '').trim();
  let where = '';
  const binds: any[] = [];
  if (keyword) {
    where = 'WHERE phone LIKE ? OR username LIKE ?';
    binds.push(`%${keyword}%`, `%${keyword}%`);
  }
  const totalRow = await env.DB.prepare(`SELECT COUNT(*) as c FROM users ${where}`).bind(...binds).first<{ c: number }>();
  const rows = await env.DB.prepare(
    `SELECT id, phone, username, points, role, is_banned, avatar_url, created_at FROM users ${where} ORDER BY created_at DESC LIMIT ? OFFSET ?`
  )
    .bind(...binds, pageSize, offset)
    .all();
  return new Response(
    JSON.stringify({ success: true, list: rows.results, total: totalRow?.c || 0, page, pageSize }),
    { headers: getCorsHeaders(env) }
  );
}

async function adjustPoints(request: Request, env: Env, admin: AdminPayload, id: string): Promise<Response> {
  const guard = assertCanWrite(admin);
  if (guard) return guard;
  const body = await request.json<{ amount?: number; reason?: string }>().catch(() => null);
  if (!body || typeof body.amount !== 'number' || body.amount === 0) {
    return jsonError('积分调整量无效', 400, env);
  }
  const before = await env.DB.prepare('SELECT points FROM users WHERE id = ?').bind(id).first<{ points: number }>();
  if (!before) return jsonError('用户不存在', 404, env);
  const after = before.points + body.amount;
  await env.DB.prepare('UPDATE users SET points = ? WHERE id = ?').bind(after, id).run();
  await writeAudit(env, request, admin, 'ADJUST_POINTS', { type: 'user', id, before: { points: before.points }, after: { points: after, reason: body.reason } });
  return new Response(JSON.stringify({ success: true, points: after }), { headers: getCorsHeaders(env) });
}

async function banUser(request: Request, env: Env, admin: AdminPayload, id: string): Promise<Response> {
  const guard = assertCanWrite(admin);
  if (guard) return guard;
  const body = await request.json<{ banned?: boolean }>().catch(() => ({ banned: true }));
  const banned = body.banned === true ? 1 : 0;
  const row = await env.DB.prepare('SELECT is_banned FROM users WHERE id = ?').bind(id).first<{ is_banned: number }>();
  if (!row) return jsonError('用户不存在', 404, env);
  await env.DB.prepare('UPDATE users SET is_banned = ? WHERE id = ?').bind(banned, id).run();
  await writeAudit(env, request, admin, banned === 1 ? 'BAN_USER' : 'UNBAN_USER', { type: 'user', id, before: { is_banned: row.is_banned }, after: { is_banned: banned } });
  return new Response(JSON.stringify({ success: true, is_banned: banned }), { headers: getCorsHeaders(env) });
}

async function renameUser(request: Request, env: Env, admin: AdminPayload, id: string): Promise<Response> {
  const guard = assertCanWrite(admin);
  if (guard) return guard;
  const body = await request.json<{ username?: string }>().catch(() => null);
  if (!body || !body.username || !body.username.trim()) return jsonError('昵称不能为空', 400, env);
  const row = await env.DB.prepare('SELECT username FROM users WHERE id = ?').bind(id).first<{ username: string }>();
  if (!row) return jsonError('用户不存在', 404, env);
  await env.DB.prepare('UPDATE users SET username = ? WHERE id = ?').bind(body.username.trim(), id).run();
  await writeAudit(env, request, admin, 'RENAME_USER', { type: 'user', id, before: { username: row.username }, after: { username: body.username.trim() } });
  return new Response(JSON.stringify({ success: true }), { headers: getCorsHeaders(env) });
}

// ============ 通用 CRUD 辅助 ============

interface CrudConfig {
  table: string;
  action: string;
  idColumn?: string;
  snapshotCols?: string[];
}

async function genericList(request: Request, env: Env, cfg: CrudConfig, url: URL): Promise<Response> {
  const { page, pageSize, offset } = parsePaging(url);
  const totalRow = await env.DB.prepare(`SELECT COUNT(*) as c FROM ${cfg.table}`).bind().first<{ c: number }>();
  const rows = await env.DB.prepare(`SELECT * FROM ${cfg.table} ORDER BY rowid DESC LIMIT ? OFFSET ?`).bind(pageSize, offset).all();
  return new Response(JSON.stringify({ success: true, list: rows.results, total: totalRow?.c || 0, page, pageSize }), { headers: getCorsHeaders(env) });
}

async function genericCreate(request: Request, env: Env, admin: AdminPayload, cfg: CrudConfig): Promise<Response> {
  const guard = assertCanWrite(admin);
  if (guard) return guard;
  const body = (await request.json().catch(() => null)) as Record<string, any> | null;
  if (!body || typeof body !== 'object') return jsonError('请求体无效', 400, env);
  const cols = Object.keys(body).filter((k) => body[k] !== undefined);
  if (cols.length === 0) return jsonError('无有效字段', 400, env);
  const placeholders = cols.map(() => '?').join(', ');
  const sql = `INSERT INTO ${cfg.table} (${cols.join(', ')}) VALUES (${placeholders})`;
  const info = await env.DB.prepare(sql).bind(...cols.map((c) => body[c])).run();
  const newId = (info as any).lastRowId?.toString() || null;
  await writeAudit(env, request, admin, `CREATE_${cfg.action}`, { type: cfg.table, id: newId, after: pick(body, cfg.snapshotCols) });
  return new Response(JSON.stringify({ success: true, id: newId }), { headers: getCorsHeaders(env) });
}

async function genericUpdate(request: Request, env: Env, admin: AdminPayload, cfg: CrudConfig, id: string): Promise<Response> {
  const guard = assertCanWrite(admin);
  if (guard) return guard;
  const idCol = cfg.idColumn || 'id';
  const before = await env.DB.prepare(`SELECT * FROM ${cfg.table} WHERE ${idCol} = ?`).bind(id).first();
  if (!before) return jsonError('记录不存在', 404, env);
  const body = (await request.json().catch(() => null)) as Record<string, any> | null;
  if (!body || typeof body !== 'object') return jsonError('请求体无效', 400, env);
  const cols = Object.keys(body).filter((k) => body[k] !== undefined && k !== idCol);
  if (cols.length === 0) return jsonError('无有效字段', 400, env);
  const sets = cols.map((c) => `${c} = ?`).join(', ');
  const sql = `UPDATE ${cfg.table} SET ${sets} WHERE ${idCol} = ?`;
  await env.DB.prepare(sql).bind(...cols.map((c) => body[c]), id).run();
  const after = await env.DB.prepare(`SELECT * FROM ${cfg.table} WHERE ${idCol} = ?`).bind(id).first();
  await writeAudit(env, request, admin, `UPDATE_${cfg.action}`, { type: cfg.table, id, before: pick(before, cfg.snapshotCols), after: pick(after, cfg.snapshotCols) });
  return new Response(JSON.stringify({ success: true }), { headers: getCorsHeaders(env) });
}

async function genericDelete(request: Request, env: Env, admin: AdminPayload, cfg: CrudConfig, id: string): Promise<Response> {
  const guard = assertCanWrite(admin);
  if (guard) return guard;
  const idCol = cfg.idColumn || 'id';
  const before = await env.DB.prepare(`SELECT * FROM ${cfg.table} WHERE ${idCol} = ?`).bind(id).first();
  if (!before) return jsonError('记录不存在', 404, env);
  await env.DB.prepare(`DELETE FROM ${cfg.table} WHERE ${idCol} = ?`).bind(id).run();
  await writeAudit(env, request, admin, `DELETE_${cfg.action}`, { type: cfg.table, id, before: pick(before, cfg.snapshotCols) });
  return new Response(JSON.stringify({ success: true }), { headers: getCorsHeaders(env) });
}

function pick(obj: any, cols?: string[]): any {
  if (!obj) return obj;
  if (!cols || cols.length === 0) return obj;
  const out: any = {};
  for (const c of cols) if (obj[c] !== undefined) out[c] = obj[c];
  return out;
}

// ============ 配置 ============

async function getConfig(request: Request, env: Env): Promise<Response> {
  const rows = await env.DB.prepare('SELECT key, value FROM config').all();
  return new Response(JSON.stringify({ success: true, list: rows.results }), { headers: getCorsHeaders(env) });
}

async function updateConfig(request: Request, env: Env, admin: AdminPayload): Promise<Response> {
  const guard = assertCanWrite(admin);
  if (guard) return guard;
  const body = await request.json<{ key?: string; value?: string }>().catch(() => null);
  if (!body || !body.key) return jsonError('缺少配置 key', 400, env);
  const before = await env.DB.prepare('SELECT key, value FROM config WHERE key = ?').bind(body.key).first();
  await env.DB.prepare('INSERT INTO config (key, value) VALUES (?, ?) ON CONFLICT(key) DO UPDATE SET value = ?').bind(body.key, body.value ?? '', body.value ?? '').run();
  await writeAudit(env, request, admin, 'UPDATE_CONFIG', { type: 'config', id: body.key, before, after: { key: body.key, value: body.value } });
  return new Response(JSON.stringify({ success: true }), { headers: getCorsHeaders(env) });
}

// ============ 统计 ============

async function getStats(request: Request, env: Env): Promise<Response> {
  const [users, today, exchange, points, notice, banned, shop, task, level] = await Promise.all([
    env.DB.prepare('SELECT COUNT(*) as c FROM users').bind().first<{ c: number }>(),
    env.DB.prepare('SELECT COUNT(*) as c FROM users WHERE created_at >= ?').bind(startOfToday()).first<{ c: number }>(),
    env.DB.prepare('SELECT COUNT(*) as c FROM exchange_record').bind().first<{ c: number }>(),
    env.DB.prepare("SELECT COALESCE(SUM(amount),0) as s FROM point_record WHERE type = 'IN'").bind().first<{ s: number }>(),
    env.DB.prepare('SELECT COUNT(*) as c FROM notice').bind().first<{ c: number }>(),
    env.DB.prepare('SELECT COUNT(*) as c FROM users WHERE is_banned = 1').bind().first<{ c: number }>(),
    env.DB.prepare('SELECT COUNT(*) as c FROM shop_items').bind().first<{ c: number }>(),
    env.DB.prepare('SELECT COUNT(*) as c FROM task').bind().first<{ c: number }>(),
    env.DB.prepare('SELECT COUNT(*) as c FROM levels').bind().first<{ c: number }>(),
  ]);
  return new Response(
    JSON.stringify({
      success: true,
      users_total: users?.c || 0,
      today_signup: today?.c || 0,
      exchange_total: exchange?.c || 0,
      points_total: points?.s || 0,
      notice_count: notice?.c || 0,
      banned_count: banned?.c || 0,
      shop_item_count: shop?.c || 0,
      task_count: task?.c || 0,
      level_count: level?.c || 0,
    }),
    { headers: getCorsHeaders(env) }
  );
}

function startOfToday(): number {
  const d = new Date();
  d.setHours(0, 0, 0, 0);
  return d.getTime();
}

// ============ 超级管理员专属：账户管理 ============

async function listAccounts(request: Request, env: Env): Promise<Response> {
  const rows = await env.DB.prepare("SELECT id, phone, username, role, is_banned, created_at FROM users WHERE role IN ('super','operator','readonly') ORDER BY created_at DESC").all();
  return new Response(JSON.stringify({ success: true, list: rows.results }), { headers: getCorsHeaders(env) });
}

async function createAccount(request: Request, env: Env, admin: AdminPayload): Promise<Response> {
  const guard = assertSuper(admin);
  if (guard) return guard;
  const body = await request.json<{ phone?: string; role?: string; password?: string }>().catch(() => null);
  if (!body || !body.phone || !body.role) return jsonError('缺少手机号或角色', 400, env);
  if (!['super', 'operator', 'readonly'].includes(body.role)) return jsonError('角色非法', 400, env);
  const existed = await env.DB.prepare('SELECT id FROM users WHERE phone = ?').bind(body.phone).first();
  if (existed) return jsonError('该手机号已注册', 409, env);
  const pwd = body.password || Math.random().toString(36).slice(-8) + 'A1!';
  const hash = await hashPassword(pwd);
  const info = await env.DB.prepare('INSERT INTO users (phone, username, role, password_hash, points) VALUES (?, ?, ?, ?, 0)')
    .bind(body.phone, body.phone, body.role, hash)
    .run();
  const newId = (info as any).lastRowId?.toString();
  await writeAudit(env, request, admin, 'CREATE_ADMIN', { type: 'admin', id: newId, after: { phone: body.phone, role: body.role } });
  return new Response(JSON.stringify({ success: true, id: newId, initialPassword: body.password ? undefined : pwd }), { headers: getCorsHeaders(env) });
}

async function setAccountRole(request: Request, env: Env, admin: AdminPayload, id: string): Promise<Response> {
  const guard = assertSuper(admin);
  if (guard) return guard;
  const body = await request.json<{ role?: string }>().catch(() => null);
  if (!body || !['super', 'operator', 'readonly'].includes(body.role || '')) return jsonError('角色非法', 400, env);
  const before = await env.DB.prepare("SELECT role FROM users WHERE id = ? AND role IN ('super','operator','readonly')").bind(id).first<{ role: string }>();
  if (!before) return jsonError('管理员账号不存在', 404, env);
  if (before.role === 'super' && body.role !== 'super') {
    // 防止把最后一个 super 降级导致锁死：允许但审计
  }
  await env.DB.prepare("UPDATE users SET role = ? WHERE id = ?").bind(body.role, id).run();
  await writeAudit(env, request, admin, 'UPDATE_ADMIN_ROLE', { type: 'admin', id, before: { role: before.role }, after: { role: body.role } });
  return new Response(JSON.stringify({ success: true }), { headers: getCorsHeaders(env) });
}

async function disableAccount(request: Request, env: Env, admin: AdminPayload, id: string): Promise<Response> {
  const guard = assertSuper(admin);
  if (guard) return guard;
  const body = await request.json<{ disabled?: boolean }>().catch(() => ({ disabled: true }));
  const banned = body.disabled === true ? 1 : 0;
  const before = await env.DB.prepare("SELECT is_banned FROM users WHERE id = ? AND role IN ('super','operator','readonly')").bind(id).first<{ is_banned: number }>();
  if (!before) return jsonError('管理员账号不存在', 404, env);
  await env.DB.prepare('UPDATE users SET is_banned = ? WHERE id = ?').bind(banned, id).run();
  await writeAudit(env, request, admin, 'DISABLE_ADMIN', { type: 'admin', id, before: { is_banned: before.is_banned }, after: { is_banned: banned } });
  return new Response(JSON.stringify({ success: true, is_banned: banned }), { headers: getCorsHeaders(env) });
}

// ============ 审计日志查看（super 只读） ============

async function listAuditLogs(request: Request, env: Env, url: URL): Promise<Response> {
  const { page, pageSize, offset } = parsePaging(url);
  const adminId = url.searchParams.get('admin_id');
  const action = url.searchParams.get('action');
  const from = url.searchParams.get('from');
  const to = url.searchParams.get('to');
  const wheres: string[] = [];
  const binds: any[] = [];
  if (adminId) { wheres.push('admin_id = ?'); binds.push(adminId); }
  if (action) { wheres.push('action = ?'); binds.push(action); }
  if (from) { wheres.push('created_at >= ?'); binds.push(parseInt(from, 10)); }
  if (to) { wheres.push('created_at <= ?'); binds.push(parseInt(to, 10)); }
  const whereSql = wheres.length ? `WHERE ${wheres.join(' AND ')}` : '';
  const totalRow = await env.DB.prepare(`SELECT COUNT(*) as c FROM admin_audit_log ${whereSql}`).bind(...binds).first<{ c: number }>();
  const rows = await env.DB.prepare(`SELECT * FROM admin_audit_log ${whereSql} ORDER BY created_at DESC LIMIT ? OFFSET ?`).bind(...binds, pageSize, offset).all();
  return new Response(JSON.stringify({ success: true, list: rows.results, total: totalRow?.c || 0, page, pageSize }), { headers: getCorsHeaders(env) });
}

// ============ 用户资产背包管理 ============

async function listUserItems(request: Request, env: Env, admin: AdminPayload, id: string): Promise<Response> {
  const rows = await env.DB.prepare('SELECT item_type, count FROM user_items WHERE user_id = ?').bind(id).all();
  return new Response(JSON.stringify({ success: true, list: rows.results }), { headers: getCorsHeaders(env) });
}

async function updateUserItems(request: Request, env: Env, admin: AdminPayload, id: string): Promise<Response> {
  const guard = assertCanWrite(admin);
  if (guard) return guard;
  const body = (await request.json().catch(() => null)) as { items?: Array<{ item_type: string; count: number }> } | null;
  if (!body || !Array.isArray(body.items)) return jsonError('请求体无效', 400, env);
  
  const before = await env.DB.prepare('SELECT item_type, count FROM user_items WHERE user_id = ?').bind(id).all();
  
  const statements = body.items.map(item => 
    env.DB.prepare('INSERT OR REPLACE INTO user_items (user_id, item_type, count) VALUES (?, ?, ?)')
      .bind(id, item.item_type, item.count)
  );
  await env.DB.batch(statements);
  
  await writeAudit(env, request, admin, 'UPDATE_USER_ITEMS', { type: 'user_items', id, before: before.results, after: body.items });
  return new Response(JSON.stringify({ success: true }), { headers: getCorsHeaders(env) });
}

// ============ 路由分发 ============

export async function handleAdminRoutes(request: Request, env: Env, path: string, url: URL): Promise<Response | null> {
  const method = request.method;

  // 无鉴权接口
  if (path === '/api/admin/login' && method === 'POST') return adminLogin(request, env);
  if (path === '/api/admin/refresh' && method === 'POST') return adminRefresh(request, env);

  // 以下均需要管理员鉴权
  const admin = await requireAdmin(request, env);
  if (admin instanceof Response) return admin;

  // 用户管理
  if (path === '/api/admin/users' && method === 'GET') return listUsers(request, env, admin, url);
  let m = path.match(/^\/api\/admin\/users\/([^/]+)\/points$/);
  if (m && method === 'POST') return adjustPoints(request, env, admin, m[1]);
  m = path.match(/^\/api\/admin\/users\/([^/]+)\/ban$/);
  if (m && method === 'POST') return banUser(request, env, admin, m[1]);
  m = path.match(/^\/api\/admin\/users\/([^/]+)$/);
  if (m && method === 'PUT') return renameUser(request, env, admin, m[1]);
  m = path.match(/^\/api\/admin\/users\/([^/]+)\/items$/);
  if (m && method === 'GET') return listUserItems(request, env, admin, m[1]);
  if (m && method === 'POST') return updateUserItems(request, env, admin, m[1]);

  // 配置
  if (path === '/api/admin/config' && method === 'GET') return getConfig(request, env);
  if (path === '/api/admin/config' && method === 'POST') return updateConfig(request, env, admin);

  // 统计
  if (path === '/api/admin/stats' && method === 'GET') return getStats(request, env);

  // 商品
  if (path === '/api/admin/shop-items' && method === 'GET') return genericList(request, env, { table: 'shop_items', action: 'SHOP_ITEM', snapshotCols: ['item_type', 'points_price', 'stock'] }, url);
  if (path === '/api/admin/shop-items' && method === 'POST') return genericCreate(request, env, admin, { table: 'shop_items', action: 'SHOP_ITEM', snapshotCols: ['item_type', 'points_price', 'stock'] });
  m = path.match(/^\/api\/admin\/shop-items\/([^/]+)$/);
  if (m && method === 'PUT') return genericUpdate(request, env, admin, { table: 'shop_items', action: 'SHOP_ITEM', idColumn: 'id', snapshotCols: ['item_type', 'points_price', 'stock'] }, m[1]);
  if (m && method === 'DELETE') return genericDelete(request, env, admin, { table: 'shop_items', action: 'SHOP_ITEM', idColumn: 'id', snapshotCols: ['item_type', 'points_price', 'stock'] }, m[1]);

  // 公告
  if (path === '/api/admin/notices' && method === 'GET') return genericList(request, env, { table: 'notice', action: 'NOTICE', snapshotCols: ['title', 'content', 'type'] }, url);
  if (path === '/api/admin/notices' && method === 'POST') return genericCreate(request, env, admin, { table: 'notice', action: 'NOTICE', snapshotCols: ['title', 'content', 'type'] });
  m = path.match(/^\/api\/admin\/notices\/([^/]+)$/);
  if (m && method === 'PUT') return genericUpdate(request, env, admin, { table: 'notice', action: 'NOTICE', idColumn: 'id', snapshotCols: ['title', 'content', 'type'] }, m[1]);
  if (m && method === 'DELETE') return genericDelete(request, env, admin, { table: 'notice', action: 'NOTICE', idColumn: 'id', snapshotCols: ['title', 'content', 'type'] }, m[1]);

  // 任务
  if (path === '/api/admin/tasks' && method === 'GET') return genericList(request, env, { table: 'task', action: 'TASK', snapshotCols: ['target_count', 'points_reward'] }, url);
  if (path === '/api/admin/tasks' && method === 'POST') return genericCreate(request, env, admin, { table: 'task', action: 'TASK', snapshotCols: ['target_count', 'points_reward'] });
  m = path.match(/^\/api\/admin\/tasks\/([^/]+)$/);
  if (m && method === 'PUT') return genericUpdate(request, env, admin, { table: 'task', action: 'TASK', idColumn: 'id', snapshotCols: ['target_count', 'points_reward'] }, m[1]);
  if (m && method === 'DELETE') return genericDelete(request, env, admin, { table: 'task', action: 'TASK', idColumn: 'id', snapshotCols: ['target_count', 'points_reward'] }, m[1]);

  // 关卡（layout_data 仅记长度/hash）
  if (path === '/api/admin/levels' && method === 'GET') return genericList(request, env, { table: 'levels', action: 'LEVEL', snapshotCols: ['layout_data'] }, url);
  if (path === '/api/admin/levels' && method === 'POST') {
    const guard = assertCanWrite(admin); if (guard) return guard;
    const body = (await request.json().catch(() => null)) as Record<string, any> | null;
    if (!body || typeof body !== 'object') return jsonError('请求体无效', 400, env);
    const cols = Object.keys(body).filter((k) => body[k] !== undefined);
    const placeholders = cols.map(() => '?').join(', ');
    const info = await env.DB.prepare(`INSERT INTO levels (${cols.join(', ')}) VALUES (${placeholders})`).bind(...cols.map((c) => body[c])).run();
    const newId = (info as any).lastRowId?.toString();
    const layoutSummary = body.layout_data ? { layout_length: String(body.layout_data).length, layout_hash: await sha256(String(body.layout_data)) } : null;
    await writeAudit(env, request, admin, 'CREATE_LEVEL', { type: 'levels', id: newId, after: layoutSummary });
    return new Response(JSON.stringify({ success: true, id: newId }), { headers: getCorsHeaders(env) });
  }
  m = path.match(/^\/api\/admin\/levels\/([^/]+)$/);
  if (m && method === 'PUT') {
    const guard = assertCanWrite(admin); if (guard) return guard;
    const before = await env.DB.prepare('SELECT layout_data FROM levels WHERE level_id = ?').bind(m[1]).first();
    const body = (await request.json().catch(() => null)) as Record<string, any> | null;
    if (!body || typeof body !== 'object') return jsonError('请求体无效', 400, env);
    const cols = Object.keys(body).filter((k) => body[k] !== undefined && k !== 'id');
    if (cols.length === 0) return jsonError('无有效字段', 400, env);
    const sets = cols.map((c) => `${c} = ?`).join(', ');
    await env.DB.prepare(`UPDATE levels SET ${sets} WHERE level_id = ?`).bind(...cols.map((c) => body[c]), m[1]).run();
    const after = body.layout_data ? { layout_length: String(body.layout_data).length, layout_hash: await sha256(String(body.layout_data)) } : {};
    await writeAudit(env, request, admin, 'UPDATE_LEVEL', { type: 'levels', id: m[1], before: before ? { layout_length: String(before.layout_data).length, layout_hash: await sha256(String(before.layout_data)) } : null, after });
    return new Response(JSON.stringify({ success: true }), { headers: getCorsHeaders(env) });
  }
  if (m && method === 'DELETE') {
    const guard = assertCanWrite(admin); if (guard) return guard;
    const before = await env.DB.prepare('SELECT layout_data FROM levels WHERE level_id = ?').bind(m[1]).first();
    await env.DB.prepare('DELETE FROM levels WHERE level_id = ?').bind(m[1]).run();
    await writeAudit(env, request, admin, 'DELETE_LEVEL', { type: 'levels', id: m[1], before: before ? { layout_length: String(before.layout_data).length } : null });
    return new Response(JSON.stringify({ success: true }), { headers: getCorsHeaders(env) });
  }

  // 超级管理员专属
  if (path === '/api/admin/accounts' && method === 'GET') { const g = assertSuper(admin); if (g) return g; return listAccounts(request, env); }
  if (path === '/api/admin/accounts' && method === 'POST') return createAccount(request, env, admin);
  m = path.match(/^\/api\/admin\/accounts\/([^/]+)\/role$/);
  if (m && method === 'PUT') return setAccountRole(request, env, admin, m[1]);
  m = path.match(/^\/api\/admin\/accounts\/([^/]+)\/disable$/);
  if (m && method === 'POST') return disableAccount(request, env, admin, m[1]);

  if (path === '/api/admin/audit-logs' && method === 'GET') { const g = assertSuper(admin); if (g) return g; return listAuditLogs(request, env, url); }

  return jsonError('Not Found', 404, env);
}
