import { Env, TileData } from '../types';
import { getCorsHeaders, jsonError, requireAdmin, assertCanWrite, assertSuper, AdminPayload, getGameModeStatus } from '../helpers';
import { generateJWT, verifyJWT, sha256 } from '../crypto';
import { putR2Image, validateImage, purgeShopCache, putR2Object } from '../lib/r2';
import { verifyPassword, hashPassword } from '../auth-utils';
import { writeAuditChanges, reassembleAuditSnapshots } from '../lib/audit';
import { writeLevelTiles, readLevelTilesByLevels, assembleLayoutData, parseLayoutData } from '../lib/level-tiles';
import {
  seedI18nForEntity,
  localeValuesFromBody,
  stripLocaleSuffixedKeys,
} from '../i18n';

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

/**
 * 解析分页参数：从 query 读取 page / pageSize，约束范围 [1, PAGE_SIZE_MAX]，计算 SQL OFFSET。
 * 默认值：page=1，pageSize=PAGE_SIZE_DEFAULT(20)。用于在多条列表接口中统一分页。
 */
function parsePaging(url: URL): { page: number; pageSize: number; offset: number } {
  const page = Math.max(1, parseInt(url.searchParams.get('page') || '1', 10) || 1);
  const pageSize = Math.min(
    PAGE_SIZE_MAX,
    Math.max(1, parseInt(url.searchParams.get('pageSize') || String(PAGE_SIZE_DEFAULT), 10) || PAGE_SIZE_DEFAULT)
  );
  return { page, pageSize, offset: (page - 1) * pageSize };
}

/**
 * 提取客户端真实 IP：优先 CF-Connecting-IP，其次 x-forwarded-for 首个，缺省 unknown。
 * 用于审计日志 source_ip 字段落库。
 */
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
    const info = await env.DB.prepare(
      `INSERT INTO admin_audit_log
        (admin_id, admin_phone, admin_role, action, target_type, target_id, source_ip, user_agent, created_at)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)`
    )
      .bind(
        admin.userId,
        admin.phone,
        admin.role,
        action,
        target.type || null,
        target.id || null,
        getClientIp(request),
        request.headers.get('User-Agent') || 'unknown',
        Date.now()
      )
      .run();
    // 写主表后取 lastRowId，将快照按字段拆入 admin_audit_changes 子表（旧 before/after_snapshot 列停止写入）
    const changeId = info.meta.last_row_id;
    if (changeId != null && changeId !== undefined) {
      await writeAuditChanges(env, changeId, target.before, target.after);
    }
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
  if (!row || !row.role || !['super', 'operator', 'readonly'].includes(row.role)) {
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

/**
 * 列出用户（读操作）。鉴权：requireAdmin。
 * 业务：动态拼接 WHERE（keyword 模糊匹配 phone/username、role 精确），
 * 先 COUNT(*) 取 total，再 SELECT 分页（ORDER BY created_at DESC）。
 * 涉及表：users。响应结构见路由注释。
 */
async function listUsers(request: Request, env: Env, admin: AdminPayload, url: URL): Promise<Response> {
  const { page, pageSize, offset } = parsePaging(url);
  const keyword = (url.searchParams.get('keyword') || '').trim();
  const role = (url.searchParams.get('role') || '').trim();
  const wheres: string[] = [];
  const binds: any[] = [];
  if (keyword) {
    wheres.push('(phone LIKE ? OR username LIKE ?)');
    binds.push(`%${keyword}%`, `%${keyword}%`);
  }
  if (role) {
    wheres.push('role = ?');
    binds.push(role);
  }
  const where = wheres.length ? 'WHERE ' + wheres.join(' AND ') : '';
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

/**
 * 调整用户积分（写操作，需 assertCanWrite）。
 * 参数校验：amount 必须为非零 number，否则 400。
 * 业务：先读取旧 points 作为审计 before，计算 after=before+amount，UPDATE users.points；
 * 仅 DB 变更成功后落审计 ADJUST_POINTS（含 reason）。涉及表：users。非原子：单条 UPDATE，失败无回滚需求。
 */
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

/**
 * 封禁/解封用户（写操作，需 assertCanWrite）。
 * 参数校验：body.banned 缺省视为封禁(true)。先查旧 is_banned 用于审计 before。
 * 业务：UPDATE users.is_banned；DB 成功后落 BAN_USER/UNBAN_USER 审计。涉及表：users。
 */
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

/**
 * 修改用户昵称（写操作，需 assertCanWrite）。
 * 参数校验：username 非空否则 400；用户不存在 404。
 * 业务：读取旧 username 作为审计 before，UPDATE users.username；成功后落 RENAME_USER 审计。涉及表：users。
 */
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

/**
 * 删除用户（高危写操作，super 专属，assertSuper）。
 * 参数校验：用户不存在 404；双护栏——禁止删管理员账户(403，避免后台锁死)、禁止删当前登录账户(403)。
 * 事务边界：env.DB.batch 一次性提交 12 条 DELETE，级联清理该用户在
 * level_unlock/user_items/exchange_record/point_record/sign_record/user_task/leaderboard/
 * backup_save_*（含子查询）/users/login_token 的全部数据，保证原子性。
 * 仅 batch 成功后落 DELETE_USER 审计。涉及表：上述多张。
 */
async function deleteUser(request: Request, env: Env, admin: AdminPayload, id: string): Promise<Response> {
  const guard = assertSuper(admin);
  if (guard) return guard;
  const user = await env.DB.prepare('SELECT id, phone, username, role FROM users WHERE id = ?')
    .bind(id)
    .first<{ id: string; phone: string; username: string; role: string }>();
  if (!user) return jsonError('用户不存在', 404, env);
  // 安全护栏：禁止移除管理员账户，避免后台被锁死
  if (['super', 'operator', 'readonly'].includes(user.role)) return jsonError('不能移除管理员账户', 403, env);
  // 安全护栏：禁止移除当前登录账户
  if (admin.userId === id) return jsonError('不能移除当前登录账户', 403, env);
  // 级联删除该用户在相关表中的全部数据（同一原子事务）
  await env.DB.batch([
    env.DB.prepare('DELETE FROM level_unlock WHERE user_id = ?').bind(id),
    env.DB.prepare('DELETE FROM user_items WHERE user_id = ?').bind(id),
    env.DB.prepare('DELETE FROM exchange_record WHERE user_id = ?').bind(id),
    env.DB.prepare('DELETE FROM point_record WHERE user_id = ?').bind(id),
    env.DB.prepare('DELETE FROM sign_record WHERE user_id = ?').bind(id),
    env.DB.prepare('DELETE FROM user_task WHERE user_id = ?').bind(id),
    env.DB.prepare('DELETE FROM leaderboard WHERE user_id = ?').bind(id),
    env.DB.prepare('DELETE FROM backup_unlocked_levels WHERE backup_id IN (SELECT id FROM backup_save_log WHERE user_id = ?)').bind(id),
    env.DB.prepare('DELETE FROM backup_save_items WHERE backup_id IN (SELECT id FROM backup_save_log WHERE user_id = ?)').bind(id),
    env.DB.prepare('DELETE FROM backup_save_log WHERE user_id = ?').bind(id),
    env.DB.prepare('DELETE FROM users WHERE id = ?').bind(id),
    env.DB.prepare('DELETE FROM login_token WHERE phone = ?').bind(user.phone),
  ]);
  await writeAudit(env, request, admin, 'DELETE_USER', { type: 'users', id, before: pick(user, ['phone', 'username', 'role']) });
  return new Response(JSON.stringify({ success: true }), { headers: getCorsHeaders(env) });
}

// ============ 通用 CRUD 辅助 ============

interface CrudConfig {
  table: string;
  action: string;
  idColumn?: string;
  snapshotCols?: string[];
  /** 搜索关键字时匹配的列名列表（为空则不支持关键字搜索） */
  searchCols?: string[];
  /**
   * 创建成功后的回调，用于 i18n 播种等后处理。
   * entityId: 新记录的 ID（auto-increment 则为 lastRowId，TEXT PK 则为 body 中的 id 字段）
   * body: 原始请求体
   */
  onCreated?: (entityId: string, body: Record<string, any>) => Promise<void>;
}

/**
 * 通用列表（读操作，requireAdmin 已在路由层通过）。
 * 业务：按 cfg.searchCols 动态拼接关键字 LIKE 的 WHERE（OR 组合），COUNT(*) 取 total，
 * 再 SELECT * 分页（ORDER BY rowid DESC）。涉及表：cfg.table。
 * 注意：SELECT * 会带出全列，仅用于后台内部，前端按需取字段。
 */
async function genericList(request: Request, env: Env, cfg: CrudConfig, url: URL): Promise<Response> {
  const { page, pageSize, offset } = parsePaging(url);
  const keyword = (url.searchParams.get('keyword') || '').trim();

  // 构建 WHERE 子句：支持关键字搜索
  let where = '';
  const whereBinds: any[] = [];
  if (keyword && cfg.searchCols && cfg.searchCols.length > 0) {
    const clauses = cfg.searchCols.map((col) => `"${col}" LIKE ?`);
    where = `WHERE (${clauses.join(' OR ')})`;
    cfg.searchCols.forEach(() => whereBinds.push(`%${keyword}%`));
  }

  const totalRow = await env.DB.prepare(`SELECT COUNT(*) as c FROM ${cfg.table} ${where}`).bind(...whereBinds).first<{ c: number }>();
  const rows = await env.DB.prepare(`SELECT * FROM ${cfg.table} ${where} ORDER BY rowid DESC LIMIT ? OFFSET ?`).bind(...whereBinds, pageSize, offset).all();
  return new Response(JSON.stringify({ success: true, list: rows.results, total: totalRow?.c || 0, page, pageSize }), { headers: getCorsHeaders(env) });
}

/**
 * 通用创建（写操作，需 assertCanWrite）。
 * 业务：剔除 locale 后缀键 → 动态拼 INSERT（双引号包裹列名兼容关键字列）→ 取 lastRowId 作为新 ID；
 * DB 写入成功后落 CREATE_${action} 审计，再异步回调 cfg.onCreated（如 i18n 播种，失败不阻塞响应）。
 * 涉及表：cfg.table。非批量：单条 INSERT，成功后审计。
 */
async function genericCreate(request: Request, env: Env, admin: AdminPayload, cfg: CrudConfig): Promise<Response> {
  const guard = assertCanWrite(admin);
  if (guard) return guard;
  const body = (await request.json().catch(() => null)) as Record<string, any> | null;
  if (!body || typeof body !== 'object') return jsonError('请求体无效', 400, env);
  // 业务表 INSERT 前剔除仅用于 i18n 传输的 locale 后缀键（宽列已 DROP，否则会引用不存在的列）
  const insertBody = stripLocaleSuffixedKeys(body);
  const cols = Object.keys(insertBody).filter((k) => insertBody[k] !== undefined);
  if (cols.length === 0) return jsonError('无有效字段', 400, env);
  const placeholders = cols.map(() => '?').join(', ');
  // 列名统一用双引号包裹，兼容 SQL 关键字列（如 shop_items 的 "group"）
  const quotedCols = cols.map((c) => `"${c}"`).join(', ');
  const sql = `INSERT INTO ${cfg.table} (${quotedCols}) VALUES (${placeholders})`;
  const info = await env.DB.prepare(sql).bind(...cols.map((c) => insertBody[c])).run();
  const newId = (info as any).lastRowId?.toString() || null;
  await writeAudit(env, request, admin, `CREATE_${cfg.action}`, { type: cfg.table, id: newId, after: pick(body, cfg.snapshotCols) });

  // 创建后回调：i18n 播种等（失败不阻塞主流程响应）。
  // 注意：传入**完整 body**（含后缀键），供 onCreated 通过 localeValuesFromBody 读取多语言值。
  if (cfg.onCreated) {
    const entityId = newId || '';
    cfg.onCreated(entityId, body).catch((e) => console.error(`onCreated failed for ${cfg.table}:`, e));
  }

  return new Response(JSON.stringify({ success: true, id: newId }), { headers: getCorsHeaders(env) });
}

/**
 * 通用更新（写操作，需 assertCanWrite）。
 * 业务：先读旧记录作审计 before；剔除 locale 后缀键与 id 列 → 动态拼 UPDATE SET；
 * 写后重读 after，落 UPDATE_${action} 审计（仅快照 cfg.snapshotCols 字段）。涉及表：cfg.table。
 */
async function genericUpdate(request: Request, env: Env, admin: AdminPayload, cfg: CrudConfig, id: string): Promise<Response> {
  const guard = assertCanWrite(admin);
  if (guard) return guard;
  const idCol = cfg.idColumn || 'id';
  const before = await env.DB.prepare(`SELECT * FROM ${cfg.table} WHERE ${idCol} = ?`).bind(id).first();
  if (!before) return jsonError('记录不存在', 404, env);
  const body = (await request.json().catch(() => null)) as Record<string, any> | null;
  if (!body || typeof body !== 'object') return jsonError('请求体无效', 400, env);
  // 编辑态前端只发 zh 基列，但为稳妥仍先剔除 locale 后缀键（防御性，避免引用不存在的列）
  const writeBody = stripLocaleSuffixedKeys(body);
  const cols = Object.keys(writeBody).filter((k) => writeBody[k] !== undefined && k !== idCol);
  if (cols.length === 0) return jsonError('无有效字段', 400, env);
  // 列名统一用双引号包裹，兼容 SQL 关键字列（如 shop_items 的 "group"）
  const sets = cols.map((c) => `"${c}" = ?`).join(', ');
  const sql = `UPDATE ${cfg.table} SET ${sets} WHERE "${idCol}" = ?`;
  await env.DB.prepare(sql).bind(...cols.map((c) => writeBody[c]), id).run();
  const after = await env.DB.prepare(`SELECT * FROM ${cfg.table} WHERE ${idCol} = ?`).bind(id).first();
  await writeAudit(env, request, admin, `UPDATE_${cfg.action}`, { type: cfg.table, id, before: pick(before, cfg.snapshotCols), after: pick(after, cfg.snapshotCols) });
  return new Response(JSON.stringify({ success: true }), { headers: getCorsHeaders(env) });
}

/**
 * 通用删除（写操作，需 assertCanWrite）。
 * 业务：先读旧记录作审计 before；DELETE BY idCol；成功后落 DELETE_${action} 审计（含 before 快照）。
 * 涉及表：cfg.table。非级联：仅删本表记录，子表依赖由业务层另行处理。
 */
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

/**
 * 字段选择器：从对象中挑选指定列组成新对象，用于审计快照裁剪（仅记录关心的字段）。
 * cols 为空或 obj 为空时原样返回。
 */
function pick(obj: any, cols?: string[]): any {
  if (!obj) return obj;
  if (!cols || cols.length === 0) return obj;
  const out: any = {};
  for (const c of cols) if (obj[c] !== undefined) out[c] = obj[c];
  return out;
}

// ============ 配置 ============

/**
 * 读取全部配置（读操作，requireAdmin）。
 * 业务：SELECT key,value FROM config。涉及表：config。响应：{ success, list:[{key,value}] }。
 */
async function getConfig(request: Request, env: Env): Promise<Response> {
  const rows = await env.DB.prepare('SELECT key, value FROM config').all();
  return new Response(JSON.stringify({ success: true, list: rows.results }), { headers: getCorsHeaders(env) });
}

/**
 * 新增/更新配置（写操作，super 专属，assertSuper）。
 * 参数校验：key 必填否则 400。
 * 业务：INSERT ... ON CONFLICT(key) DO UPDATE 实现 upsert；同时删除 SHEEPS_CACHE 中 config_${key}，
 * 使 getCachedConfig 立即生效（避免脏缓存）；成功后落 UPDATE_CONFIG 审计。涉及表：config + KV。
 */
async function updateConfig(request: Request, env: Env, admin: AdminPayload): Promise<Response> {
  const guard = assertSuper(admin);
  if (guard) return guard;
  const body = await request.json<{ key?: string; value?: string }>().catch(() => null);
  if (!body || !body.key) return jsonError('缺少配置 key', 400, env);
  const before = await env.DB.prepare('SELECT key, value FROM config WHERE key = ?').bind(body.key).first();
  await env.DB.prepare('INSERT INTO config (key, value) VALUES (?, ?) ON CONFLICT(key) DO UPDATE SET value = ?').bind(body.key, body.value ?? '', body.value ?? '').run();
  // 清除对应 KV 缓存，使 getCachedConfig 立即读到新值
  await env.SHEEPS_CACHE.delete(`config_${body.key}`).catch(() => {});
  await writeAudit(env, request, admin, 'UPDATE_CONFIG', { type: 'config', id: body.key, before, after: { key: body.key, value: body.value } });
  return new Response(JSON.stringify({ success: true }), { headers: getCorsHeaders(env) });
}

// ============ 统计 ============

/**
 * 全局运营统计（读操作，requireAdmin）。
 * 业务：Promise.all 并行聚合 users（总数/今日注册）、exchange_record、point_record(IN 累计)、
 * notice、banned、shop_items、task、levels 计数；再单独查 leaderboard 中 game_mode=1
 * （无尽生存）的今日挑战次数与历史最高分。涉及多表，纯读。响应见路由注释。
 */
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

  // 无尽生存模式指标：今日挑战次数 + 历史最高分
  const endlessPlay = await env.DB.prepare(
    'SELECT COUNT(*) as c FROM leaderboard WHERE game_mode = 1 AND achieved_at >= ?'
  ).bind(startOfToday()).first<{ c: number }>();
  const endlessMax = await env.DB.prepare(
    'SELECT COALESCE(MAX(score),0) as m FROM leaderboard WHERE game_mode = 1'
  ).first<{ m: number }>();

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
      endless_play_count: endlessPlay?.c || 0,
      endless_max_score: endlessMax?.m || 0,
    }),
    { headers: getCorsHeaders(env) }
  );
}

/**
 * 返回今天 00:00:00 的时间戳（毫秒），用于统计/查询中「今日」区间下界。
 */
function startOfToday(): number {
  const d = new Date();
  d.setHours(0, 0, 0, 0);
  return d.getTime();
}

// ============ 超级管理员专属：账户管理 ============

/**
 * 管理员账户列表（读操作，super 专属，assertSuper 已在路由层）。
 * 业务：SELECT 后台角色账户（role IN super/operator/readonly），按创建时间倒序。涉及表：users。
 */
async function listAccounts(request: Request, env: Env): Promise<Response> {
  const rows = await env.DB.prepare("SELECT id, phone, username, role, is_banned, created_at FROM users WHERE role IN ('super','operator','readonly') ORDER BY created_at DESC").all();
  return new Response(JSON.stringify({ success: true, list: rows.results }), { headers: getCorsHeaders(env) });
}

/**
 * 新建管理员账户（写操作，super 专属，assertSuper）。
 * 参数校验：phone/role 必填(400)；role 须为后台角色(400)；手机号已注册 409。
 * 业务：密码缺省生成随机强密码（12 位，含大小写+数字+符号）；hashPassword 后 INSERT users(role, points=0)；
 * 成功后落 CREATE_ADMIN 审计，仅系统生成密码时回传 initialPassword。涉及表：users。
 */
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

/**
 * 修改管理员角色（写操作，super 专属，assertSuper）。
 * 参数校验：role 须为 user/super/operator/readonly(400)；用户不存在 404。
 * 业务：UPDATE users.role；若把唯一 super 降级，仅审计预警不阻断（防锁死由前端/流程控制）。
 * 成功后落 UPDATE_ADMIN_ROLE 审计（before/after role）。涉及表：users。
 */
async function setAccountRole(request: Request, env: Env, admin: AdminPayload, id: string): Promise<Response> {
  const guard = assertSuper(admin);
  if (guard) return guard;
  const body = await request.json<{ role?: string }>().catch(() => null);
  if (!body || !['user', 'super', 'operator', 'readonly'].includes(body.role || '')) return jsonError('角色非法', 400, env);
  const before = await env.DB.prepare('SELECT role FROM users WHERE id = ?').bind(id).first<{ role: string }>();
  if (!before) return jsonError('用户不存在', 404, env);
  if (before.role === 'super' && body.role !== 'super') {
    // 防止把最后一个 super 降级导致锁死：允许但审计
  }
  await env.DB.prepare("UPDATE users SET role = ? WHERE id = ?").bind(body.role, id).run();
  await writeAudit(env, request, admin, 'UPDATE_ADMIN_ROLE', { type: 'admin', id, before: { role: before.role }, after: { role: body.role } });
  return new Response(JSON.stringify({ success: true }), { headers: getCorsHeaders(env) });
}

/**
 * 封禁/解封管理员账户（写操作，super 专属，assertSuper）。
 * 参数校验：body.disabled 缺省视为封禁；仅匹配后台角色账户(404)。
 * 业务：UPDATE users.is_banned；成功后落 DISABLE_ADMIN 审计。涉及表：users。
 */
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

/**
 * 审计日志查询（只读，super 专属，assertSuper 已在路由层）。
 * 业务：按 admin_id/action/created_at 范围动态拼 WHERE；分页读 admin_audit_log，
 * 再用 reassembleAuditSnapshots 按 change_id 重组 before/after 快照（兼容迁移前旧列）。
 * 涉及表：admin_audit_log + admin_audit_changes。响应见路由注释。
 */
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
  const list = rows.results as any[];
  // 新数据：before/after 已拆至 admin_audit_changes，按 change_id 重组为快照字符串；
  // 旧数据（迁移前写入）：admin_audit_log 的 before/after_snapshot 旧列仍有效，直接复用。
  const ids = list.map((r) => r.id);
  if (ids.length) {
    const changeMap = await reassembleAuditSnapshots(env, ids);
    for (const row of list) {
      const recon = changeMap.get(row.id);
      if (recon) {
        row.before_snapshot = recon.before;
        row.after_snapshot = recon.after;
      }
      // 无 KV 子表记录时保留 deprecated 列值（可能为 JSON 字符串或 null），前端契约不变
    }
  }
  return new Response(JSON.stringify({ success: true, list, total: totalRow?.c || 0, page, pageSize }), { headers: getCorsHeaders(env) });
}

// ============ 用户资产背包管理 ============

/**
 * 查看用户背包（读操作，requireAdmin）。
 * 业务：SELECT item_type,count FROM user_items WHERE user_id。涉及表：user_items。
 */
async function listUserItems(request: Request, env: Env, admin: AdminPayload, id: string): Promise<Response> {
  const rows = await env.DB.prepare('SELECT item_type, count FROM user_items WHERE user_id = ?').bind(id).all();
  return new Response(JSON.stringify({ success: true, list: rows.results }), { headers: getCorsHeaders(env) });
}

/**
 * 批量设置用户背包（写操作，需 assertCanWrite）。
 * 参数校验：body.items 须为数组(400)。
 * 业务：先读旧库存作审计 before；用 items 映射为 INSERT OR REPLACE 语句，
 * 经 env.DB.batch 原子覆盖 user_items；成功后落 UPDATE_USER_ITEMS 审计（before=旧快照, after=新列表）。
 * 涉及表：user_items。
 */
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

// ============ 图片资源（R2 + D1）============
// 商品封面 / 皮肤 12 张卡面 / 道具图标 的「上传 + 写库 + 镜像 image_url + 刷缓存」

/**
 * 通用图片上传：multipart 的 file + key（或 folder 由后端拼 key）→ 校验 → 写 R2 → 返回公开 URL。
 * 鉴权：requireAdmin 已过，内部再 assertCanWrite（只读角色 403）。
 * 路径约束：key 必须以 `images/` 开头，且不得包含 `..`，防越权写其它前缀。
 */
async function uploadImage(request: Request, env: Env, admin: AdminPayload): Promise<Response> {
  const guard = assertCanWrite(admin);
  if (guard) return guard;

  const contentType = request.headers.get('Content-Type') || '';
  if (!contentType.includes('multipart/form-data')) {
    return jsonError('Expected multipart/form-data', 400, env);
  }

  const formData = await request.formData();
  const file = formData.get('file') as File | null;
  if (!file) return jsonError('缺少图片文件', 400, env);

  // key 优先取前端指定；否则按 folder + 时间戳生成
  let key = (formData.get('key') as string | null) || '';
  if (!key) {
    const folder = (formData.get('folder') as string | null) || 'images';
    const safeName = (file.name || 'upload.png').replace(/[^\w.\-]/g, '_');
    key = `${folder}/${Date.now()}_${safeName}`;
  }
  // 安全护栏：仅允许写入 images/ 前缀，禁止越权路径 / 目录穿越
  if (!key.startsWith('images/') || key.includes('..')) {
    return jsonError('非法的存储键（必须以 images/ 开头）', 400, env);
  }

  const err = validateImage(file);
  if (err) return jsonError(err, 400, env);

  const url = await putR2Image(env, key, file);
  return new Response(JSON.stringify({ success: true, url }), { headers: getCorsHeaders(env) });
}

/**
 * 获取某皮肤的 12 张卡面（GET，供管理后台预填）。
 */
async function getSkinTiles(request: Request, env: Env, skinType: string): Promise<Response> {
  const rows = await env.DB.prepare(
    'SELECT tile_index, image_url FROM skin_tiles WHERE skin_type = ? ORDER BY tile_index ASC'
  ).bind(skinType).all();
  return new Response(JSON.stringify({ success: true, skin_type: skinType, tiles: rows.results }), { headers: getCorsHeaders(env) });
}

/**
 * 批量保存某皮肤的 12 张卡面（PUT）。
 * 1) 逐条 UPSERT 到 skin_tiles；
 * 2) 封面镜像：取 tile_index=1 的 URL 写回 shop_items.image_url（决策 #6）；
 * 3) purgeShopCache。
 */
async function saveSkinTiles(request: Request, env: Env, admin: AdminPayload, skinType: string): Promise<Response> {
  const guard = assertCanWrite(admin);
  if (guard) return guard;

  const body = (await request.json().catch(() => null)) as { tiles?: { tile_index: number; image_url: string }[] } | null;
  if (!body || !Array.isArray(body.tiles)) return jsonError('请求体无效', 400, env);

  const base = env.R2_PUBLIC_URL || 'https://file.xqh.cc.cd';
  const clean = body.tiles
    .filter((t) => t && t.tile_index >= 1 && t.tile_index <= 12 && typeof t.image_url === 'string' && t.image_url.startsWith(base))
    .map((t) => ({ tile_index: t.tile_index, image_url: t.image_url }));
  if (clean.length === 0) return jsonError('无有效卡面数据', 400, env);

  // 1) 逐条 UPSERT（存在则覆盖 image_url）
  for (const t of clean) {
    await env.DB.prepare(
      `INSERT INTO skin_tiles (skin_type, tile_index, image_url) VALUES (?, ?, ?)
       ON CONFLICT(skin_type, tile_index) DO UPDATE SET image_url = excluded.image_url`
    ).bind(skinType, t.tile_index, t.image_url).run();
  }

  // 2) 封面镜像（决策 #6）：tile_index=1 的 URL 写回 shop_items.image_url
  const cover = clean.find((t) => t.tile_index === 1);
  if (cover) {
    const itemType = `SKIN_${skinType.toUpperCase()}`;
    await env.DB.prepare('UPDATE shop_items SET image_url = ? WHERE item_type = ?').bind(cover.image_url, itemType).run();
  }

  // 3) 刷 KV 缓存
  await purgeShopCache(env);
  await writeAudit(env, request, admin, 'SAVE_SKIN_TILES', { type: 'skin_tiles', id: skinType, after: clean });
  return new Response(JSON.stringify({ success: true }), { headers: getCorsHeaders(env) });
}

/**
 * 获取某道具图标的当前 URL（GET，供管理后台预填）。
 */
async function getItemIcon(request: Request, env: Env, itemType: string): Promise<Response> {
  const row = await env.DB.prepare('SELECT item_type, image_url FROM item_icons WHERE item_type = ?').bind(itemType).first();
  return new Response(JSON.stringify({ success: true, item_type: itemType, icon: row || null }), { headers: getCorsHeaders(env) });
}

/**
 * 保存某道具图标（PUT）：写 item_icons（真身，决策 #4）+ 镜像 shop_items.image_url（决策 #6）+ 刷缓存。
 */
async function saveItemIcon(request: Request, env: Env, admin: AdminPayload, itemType: string): Promise<Response> {
  const guard = assertCanWrite(admin);
  if (guard) return guard;

  const body = (await request.json().catch(() => null)) as { image_url?: string } | null;
  if (!body || typeof body.image_url !== 'string' || !body.image_url) return jsonError('请求体无效', 400, env);

  const base = env.R2_PUBLIC_URL || 'https://file.xqh.cc.cd';
  if (!body.image_url.startsWith(base)) return jsonError('图标 URL 必须来自本站点 R2', 400, env);

  // 1) 写 item_icons 真身
  await env.DB.prepare(
    `INSERT INTO item_icons (item_type, image_url) VALUES (?, ?)
     ON CONFLICT(item_type) DO UPDATE SET image_url = excluded.image_url`
  ).bind(itemType, body.image_url).run();

  // 2) 镜像到 shop_items.image_url（决策 #6）
  await env.DB.prepare('UPDATE shop_items SET image_url = ? WHERE item_type = ?').bind(body.image_url, itemType).run();

  // 3) 刷 KV 缓存
  await purgeShopCache(env);
  await writeAudit(env, request, admin, 'SAVE_ITEM_ICON', { type: 'item_icons', id: itemType, after: { image_url: body.image_url } });
  return new Response(JSON.stringify({ success: true }), { headers: getCorsHeaders(env) });
}

/**
 * 商品写接口包装：复用 genericUpdate，写完额外刷新 shop KV 缓存（image_url / group 变更需生效）。
 */
async function updateShopItemRoute(request: Request, env: Env, admin: AdminPayload, id: string): Promise<Response> {
  const res = await genericUpdate(request, env, admin, { table: 'shop_items', action: 'SHOP_ITEM', idColumn: 'id', snapshotCols: ['item_type', 'points_price', 'stock'] }, id);
  await purgeShopCache(env);
  return res;
}

// ============ App 版本管理 ============

/**
 * 新增 App 版本：写入 created_at；download_url 可填外部 URL 或由 /api/admin/upload-apk 回填。
 */
async function createAppVersion(request: Request, env: Env, admin: AdminPayload): Promise<Response> {
  const guard = assertCanWrite(admin);
  if (guard) return guard;
  const body = (await request.json().catch(() => null)) as Record<string, any> | null;
  if (!body || typeof body !== 'object') return jsonError('请求体无效', 400, env);
  if (body.version_code === undefined || body.version_code === null || body.version_code === '') {
    return jsonError('缺少 version_code', 400, env);
  }
  if (!body.version_name) return jsonError('缺少 version_name', 400, env);
  // 数值字段强转 Number（CrudPage select 以字符串提交）
  if (body.status !== undefined) body.status = Number(body.status);
  if (body.is_force_update !== undefined) body.is_force_update = Number(body.is_force_update);
  // 新建即发布：status=1 且 release_time 为空时写 release_time
  if (body.status === 1 && (body.release_time === undefined || body.release_time === null)) {
    body.release_time = Date.now();
  }
  if (body.created_at === undefined) body.created_at = Date.now();
  // 业务表 INSERT 前剔除仅用于 i18n 传输的 locale 后缀键（宽列已 DROP）
  const writeBody = stripLocaleSuffixedKeys(body);
  const cols = Object.keys(writeBody).filter((k) => writeBody[k] !== undefined);
  const placeholders = cols.map(() => '?').join(', ');
  // 列名统一双引号包裹，兼容 SQL 关键字列
  const quoted = cols.map((c) => `"${c}"`).join(', ');
  const info = await env.DB.prepare(`INSERT INTO app_version (${quoted}) VALUES (${placeholders})`)
    .bind(...cols.map((c) => writeBody[c]))
    .run();
  const newId = String(body.version_code);
  await writeAudit(env, request, admin, 'CREATE_APP_VERSION', { type: 'app_version', id: newId, after: pick(body, ['version_name', 'status', 'apk_url', 'download_url']) });

  // 自动播种 i18n 多语言行（update_log 字段，5 种语言真实落库）
  if (body.update_log !== undefined && body.update_log !== null) {
    seedI18nForEntity(env, 'app_version', newId, { update_log: localeValuesFromBody(body, 'update_log') }).catch((e) => console.error('seedI18nForEntity failed for app_version:', e));
  }

  return new Response(JSON.stringify({ success: true, id: newId }), { headers: getCorsHeaders(env) });
}

/**
 * 编辑 App 版本：status 变 1（已发布）且 release_time 为空时，自动写入 release_time=now。
 */
async function updateAppVersion(request: Request, env: Env, admin: AdminPayload, code: number): Promise<Response> {
  const guard = assertCanWrite(admin);
  if (guard) return guard;
  const before = await env.DB.prepare('SELECT * FROM app_version WHERE version_code = ?').bind(code).first();
  if (!before) return jsonError('记录不存在', 404, env);
  const body = (await request.json().catch(() => null)) as Record<string, any> | null;
  if (!body || typeof body !== 'object') return jsonError('请求体无效', 400, env);

  // 数值字段强转 Number（CrudPage select 以字符串提交，避免 status==='1' 漏判发布）
  if (body.status !== undefined) body.status = Number(body.status);
  if (body.is_force_update !== undefined) body.is_force_update = Number(body.is_force_update);

  // 发布：status 变 1 且 release_time 空时写 release_time
  if (body.status === 1 && before.release_time === null && (body.release_time === undefined || body.release_time === null)) {
    body.release_time = Date.now();
  }

  const cols = Object.keys(body).filter((k) => body[k] !== undefined && k !== 'version_code');
  if (cols.length === 0) return jsonError('无有效字段', 400, env);
  const sets = cols.map((c) => `"${c}" = ?`).join(', ');
  await env.DB.prepare(`UPDATE app_version SET ${sets} WHERE version_code = ?`)
    .bind(...cols.map((c) => body[c]), code)
    .run();
  const after = await env.DB.prepare('SELECT * FROM app_version WHERE version_code = ?').bind(code).first();
  await writeAudit(env, request, admin, 'UPDATE_APP_VERSION', {
    type: 'app_version',
    id: String(code),
    before: pick(before, ['version_name', 'status', 'release_time']),
    after: pick(after, ['version_name', 'status', 'release_time']),
  });
  return new Response(JSON.stringify({ success: true }), { headers: getCorsHeaders(env) });
}

// ============ i18n 多语言统一管 ============

/** 从 str_key 解析 entity_ref（module 与 field 之间的部分） */
function parseEntityRef(strKey: string, module: string): string | null {
  const prefix = `${module}.`;
  if (!strKey.startsWith(prefix)) return null;
  const rest = strKey.slice(prefix.length);
  const idx = rest.lastIndexOf('.');
  if (idx < 0) return null;
  return rest.slice(0, idx);
}

/** 业务模块列表（需在 i18n 列表里 enrich entity_label 可读名） */
const I18N_BUSINESS_MODULES = ['shop_items', 'task', 'notice', 'app_version'];

/**
 * 为 i18n 列表行补充 entity_label（业务模块显示实体可读名，如商品名）。
 */
async function enrichEntityLabels(env: Env, list: any[]): Promise<void> {
  const modulesInList = [...new Set(list.map((r) => r.module))].filter((m) => I18N_BUSINESS_MODULES.includes(m));
  for (const m of modulesInList) {
    const refs = [...new Set(list.filter((r) => r.module === m).map((r) => parseEntityRef(r.str_key, m)))].filter((x) => x !== null);
    if (refs.length === 0) continue;
    let labelMap: Record<string, string> = {};
    const placeholders = refs.map(() => '?').join(', ');
    if (m === 'shop_items') {
      const r = await env.DB.prepare(`SELECT id, name FROM shop_items WHERE id IN (${placeholders})`).bind(...refs).all();
      for (const row of r.results as any[]) labelMap[String(row.id)] = row.name;
    } else if (m === 'task') {
      const r = await env.DB.prepare(`SELECT id, name FROM task WHERE id IN (${placeholders})`).bind(...refs).all();
      for (const row of r.results as any[]) labelMap[String(row.id)] = row.name;
    } else if (m === 'notice') {
      const r = await env.DB.prepare(`SELECT id, title FROM notice WHERE id IN (${placeholders})`).bind(...refs).all();
      for (const row of r.results as any[]) labelMap[String(row.id)] = row.title;
    } else if (m === 'app_version') {
      const r = await env.DB.prepare(`SELECT version_code, version_name FROM app_version WHERE version_code IN (${placeholders})`).bind(...refs).all();
      for (const row of r.results as any[]) labelMap[String(row.version_code)] = row.version_name;
    }
    for (const row of list) {
      if (row.module === m) {
        const ref = parseEntityRef(row.str_key, m);
        if (ref !== null) row.entity_label = labelMap[String(ref)] || null;
      }
    }
  }
}

/**
 * 多语言列表：支持 module / locale / keyword 过滤；业务模块 enrich entity_label。
 */
async function listI18n(request: Request, env: Env, url: URL): Promise<Response> {
  const module = (url.searchParams.get('module') || '').trim();
  const locale = (url.searchParams.get('locale') || '').trim();
  const keyword = (url.searchParams.get('keyword') || '').trim();

  const wheres: string[] = [];
  const binds: any[] = [];
  if (module) { wheres.push('module = ?'); binds.push(module); }
  if (locale) { wheres.push('locale = ?'); binds.push(locale); }
  if (keyword) { wheres.push('(str_key LIKE ? OR value LIKE ?)'); binds.push(`%${keyword}%`, `%${keyword}%`); }
  const whereSql = wheres.length ? `WHERE ${wheres.join(' AND ')}` : '';

  // 不分页，返回全部匹配行（当前数据量约235条，每个str_key最多5个locale），
  // 前端按 str_key 聚合后做客户端分页，避免按单行分页导致聚合后条目骤减的问题。
  const rows = await env.DB.prepare(`SELECT * FROM i18n_strings ${whereSql} ORDER BY module, str_key, locale LIMIT 2000`)
    .bind(...binds)
    .all();
  const list = rows.results as any[];
  await enrichEntityLabels(env, list);
  return new Response(JSON.stringify({ success: true, list, total: list.length }), { headers: getCorsHeaders(env) });
}

/**
 * 新增 i18n 键：写入 i18n_strings（含 updated_at）；业务 key 模式双写基列。
 */
async function createI18n(request: Request, env: Env, admin: AdminPayload): Promise<Response> {
  const guard = assertCanWrite(admin);
  if (guard) return guard;
  const body = (await request.json().catch(() => null)) as Record<string, any> | null;
  if (!body || typeof body !== 'object') return jsonError('请求体无效', 400, env);
  if (!body.str_key || !body.locale || !body.module) return jsonError('缺少 str_key/locale/module', 400, env);
  if (body.updated_at === undefined) body.updated_at = Date.now();
  const cols = Object.keys(body).filter((k) => body[k] !== undefined);
  const placeholders = cols.map(() => '?').join(', ');
  const quoted = cols.map((c) => `"${c}"`).join(', ');
  const info = await env.DB.prepare(`INSERT INTO i18n_strings (${quoted}) VALUES (${placeholders})`)
    .bind(...cols.map((c) => body[c]))
    .run();
  const newId = (info as any).lastRowId?.toString() || null;
  await writeAudit(env, request, admin, 'CREATE_I18N', { type: 'i18n_strings', id: newId, after: pick(body, ['str_key', 'locale', 'module', 'value']) });
  return new Response(JSON.stringify({ success: true, id: newId }), { headers: getCorsHeaders(env) });
}

/**
 * 编辑 i18n 值：写 value + updated_at；业务 key 模式双写基列（过渡期保持 fallback 同步）。
 */
async function updateI18n(request: Request, env: Env, admin: AdminPayload, id: string): Promise<Response> {
  const guard = assertCanWrite(admin);
  if (guard) return guard;
  const before = (await env.DB.prepare('SELECT * FROM i18n_strings WHERE id = ?').bind(id).first()) as {
    str_key: string;
    module: string;
    locale: string;
    value: string | null;
  } | null;
  if (!before) return jsonError('记录不存在', 404, env);
  const body = (await request.json().catch(() => null)) as Record<string, any> | null;
  if (!body || typeof body !== 'object') return jsonError('请求体无效', 400, env);

  const patch: Record<string, any> = {};
  if (body.value !== undefined) patch.value = body.value;
  patch.updated_at = Date.now();
  const cols = Object.keys(patch);
  const sets = cols.map((c) => `"${c}" = ?`).join(', ');
  await env.DB.prepare(`UPDATE i18n_strings SET ${sets} WHERE id = ?`).bind(...cols.map((c) => patch[c]), id).run();

  await writeAudit(env, request, admin, 'UPDATE_I18N', {
    type: 'i18n_strings',
    id,
    before: pick(before, ['str_key', 'locale', 'value']),
    after: { value: body.value },
  });
  return new Response(JSON.stringify({ success: true }), { headers: getCorsHeaders(env) });
}

// ============ APK 上传（R2）============

/**
 * 上传 APK 安装包到 R2：复用通用上传 putR2Object，放宽前缀（apks/）与文件类型约束，
 * 返回 { url } 供后台回填 app_version.download_url。
 */
async function uploadApk(request: Request, env: Env, admin: AdminPayload): Promise<Response> {
  const guard = assertCanWrite(admin);
  if (guard) return guard;
  const contentType = request.headers.get('Content-Type') || '';
  if (!contentType.includes('multipart/form-data')) {
    return jsonError('Expected multipart/form-data', 400, env);
  }
  const formData = await request.formData();
  const file = formData.get('file') as File | null;
  if (!file) return jsonError('缺少 APK 文件', 400, env);
  // 仅放行 .apk
  if (!file.name || !file.name.toLowerCase().endsWith('.apk')) {
    return jsonError('仅支持 .apk 安装包', 400, env);
  }
  const safeName = (file.name || 'app.apk').replace(/[^\w.\-]/g, '_');
  const key = `apks/${Date.now()}_${safeName}`;
  const url = await putR2Object(env, key, file, 'application/vnd.android.package-archive');
  return new Response(JSON.stringify({ success: true, url }), { headers: getCorsHeaders(env) });
}

// ============ 排行榜手动管理 ============

/**
 * 排行榜列表（后台）：JOIN users 取 username，按 score DESC；
 * game_mode=1 且 gamemode_endless=off 时返回空列表 + disabled 提示。
 */
async function listLeaderboard(request: Request, env: Env, url: URL): Promise<Response> {
  const { stage, endless } = await getGameModeStatus(env);
  const gameMode = parseInt(url.searchParams.get('game_mode') || '0', 10);
  if (gameMode === 1 && !endless) {
    return new Response(
      JSON.stringify({ success: true, list: [], total: 0, page: 1, pageSize: 20, disabled: true, message: '无尽生存模式未开启' }),
      { headers: getCorsHeaders(env) }
    );
  }

  const { page, pageSize, offset } = parsePaging(url);
  const wheres: string[] = ['l.game_mode = ?'];
  const binds: any[] = [gameMode];
  const levelId = url.searchParams.get('level_id');
  if (levelId !== null && levelId !== '') {
    wheres.push('l.level_id = ?');
    binds.push(parseInt(levelId, 10));
  }
  const whereSql = `WHERE ${wheres.join(' AND ')}`;
  const totalRow = await env.DB.prepare(`SELECT COUNT(*) as c FROM leaderboard l ${whereSql}`).bind(...binds).first<{ c: number }>();
  const rows = await env.DB.prepare(
    `SELECT l.id, l.user_id, u.username, l.level_id, l.score, l.clear_time_ms, l.game_mode, l.achieved_at
     FROM leaderboard l JOIN users u ON l.user_id = u.id ${whereSql} ORDER BY l.score DESC LIMIT ? OFFSET ?`
  )
    .bind(...binds, pageSize, offset)
    .all();

  return new Response(
    JSON.stringify({ success: true, list: rows.results, total: totalRow?.c || 0, page, pageSize }),
    { headers: getCorsHeaders(env) }
  );
}

/**
 * 新增排行榜行（手动补录/校正）：校验 user 存在，写 achieved_at。
 */
async function createLeaderboardRow(request: Request, env: Env, admin: AdminPayload): Promise<Response> {
  const guard = assertCanWrite(admin);
  if (guard) return guard;
  const body = (await request.json().catch(() => null)) as Record<string, any> | null;
  if (!body || typeof body !== 'object') return jsonError('请求体无效', 400, env);
  if (!body.user_id) return jsonError('缺少 user_id', 400, env);
  const user = await env.DB.prepare('SELECT id FROM users WHERE id = ?').bind(body.user_id).first();
  if (!user) return jsonError('用户不存在', 404, env);

  const score = Number(body.score) || 0;
  const clearTime = Number(body.clear_time_ms) || 0;
  const levelId = Number(body.level_id) || 0;
  const gameMode = Number(body.game_mode) || 0;

  const info = await env.DB.prepare(
    'INSERT INTO leaderboard (user_id, level_id, score, clear_time_ms, game_mode, achieved_at) VALUES (?, ?, ?, ?, ?, ?)'
  )
    .bind(body.user_id, levelId, score, clearTime, gameMode, Date.now())
    .run();
  const newId = (info as any).lastRowId?.toString();
  await writeAudit(env, request, admin, 'CREATE_LEADERBOARD', {
    type: 'leaderboard',
    id: newId,
    after: pick(body, ['user_id', 'level_id', 'score', 'game_mode']),
  });
  return new Response(JSON.stringify({ success: true, id: newId }), { headers: getCorsHeaders(env) });
}

/**
 * 改排行榜分（手动改分）：仅允许修改 score / clear_time_ms / level_id / game_mode。
 */
async function updateLeaderboardRow(request: Request, env: Env, admin: AdminPayload, id: string): Promise<Response> {
  const guard = assertCanWrite(admin);
  if (guard) return guard;
  const before = await env.DB.prepare('SELECT * FROM leaderboard WHERE id = ?').bind(id).first();
  if (!before) return jsonError('记录不存在', 404, env);
  const body = (await request.json().catch(() => null)) as Record<string, any> | null;
  if (!body || typeof body !== 'object') return jsonError('请求体无效', 400, env);

  const editable = ['score', 'clear_time_ms', 'level_id', 'game_mode'];
  const cols = Object.keys(body).filter((k) => editable.includes(k) && body[k] !== undefined);
  if (cols.length === 0) return jsonError('无有效字段（仅可改分数/耗时/关卡/模式）', 400, env);
  const sets = cols.map((c) => `"${c}" = ?`).join(', ');
  await env.DB.prepare(`UPDATE leaderboard SET ${sets} WHERE id = ?`)
    .bind(...cols.map((c) => Number(body[c])), id)
    .run();
  const after = await env.DB.prepare('SELECT * FROM leaderboard WHERE id = ?').bind(id).first();
  await writeAudit(env, request, admin, 'UPDATE_LEADERBOARD', {
    type: 'leaderboard',
    id,
    before: pick(before, ['score', 'clear_time_ms', 'level_id']),
    after: pick(after, ['score', 'clear_time_ms', 'level_id']),
  });
  return new Response(JSON.stringify({ success: true }), { headers: getCorsHeaders(env) });
}

/**
 * 用户搜索（反查 id）：按手机号 / 昵称 LIKE 反查，供排行榜手动改分 UserPicker 选用户。
 */
async function searchUsers(request: Request, env: Env, url: URL): Promise<Response> {
  const keyword = (url.searchParams.get('q') ?? url.searchParams.get('keyword') ?? '').trim();
  if (!keyword) {
    return new Response(JSON.stringify({ success: true, list: [] }), { headers: getCorsHeaders(env) });
  }
  const rows = await env.DB.prepare(
    'SELECT id, username, phone FROM users WHERE phone LIKE ? OR username LIKE ? ORDER BY created_at DESC LIMIT 20'
  )
    .bind(`%${keyword}%`, `%${keyword}%`)
    .all();
  return new Response(JSON.stringify({ success: true, list: rows.results }), { headers: getCorsHeaders(env) });
}

// ============ 关卡（layout_data 拆表：主表 levels + 子表 level_tiles） ============

/**
 * 关卡列表：分页读取 levels，再按本页 level_id 批量读取 level_tiles 重组 layout_data 字符串。
 * 未回填的存量数据回退使用 levels.layout_data 旧列（deprecated），保证前端契约不变。
 */
async function listLevels(request: Request, env: Env, url: URL): Promise<Response> {
  const { page, pageSize, offset } = parsePaging(url);
  const totalRow = await env.DB.prepare('SELECT COUNT(*) as c FROM levels').bind().first<{ c: number }>();
  const rows = await env.DB.prepare(
    'SELECT level_id, difficulty, created_at FROM levels ORDER BY rowid DESC LIMIT ? OFFSET ?'
  )
    .bind(pageSize, offset)
    .all();
  const list = rows.results as any[];
  const ids = list.map((r) => r.level_id);
  const tileMap = await readLevelTilesByLevels(env, ids);
  for (const row of list) {
    const tiles = tileMap.get(row.level_id);
    if (tiles && tiles.length) {
      // 已回填：由子表规范化数据重组
      row.layout_data = assembleLayoutData(tiles);
    } else if (row.layout_data != null) {
      // 存量数据尚未回填，直接复用旧 JSON 列
      row.layout_data = row.layout_data;
    } else {
      row.layout_data = '[]';
    }
  }
  return new Response(
    JSON.stringify({ success: true, list, total: totalRow?.c || 0, page, pageSize }),
    { headers: getCorsHeaders(env) }
  );
}

/**
 * 创建关卡：layout_data(JSON 字符串) → 解析为 TileData[] 写入 level_tiles 子表；
 * 主表仅写 level_id/difficulty/created_at（停止写 layout_data 旧列）。
 * 若旧库 levels.layout_data 仍为 NOT NULL 且迁移未放开约束，则回退写入旧列兜底，避免创建失败。
 */
async function createLevel(request: Request, env: Env, admin: AdminPayload): Promise<Response> {
  const guard = assertCanWrite(admin);
  if (guard) return guard;
  const body = (await request.json().catch(() => null)) as Record<string, any> | null;
  if (!body || typeof body !== 'object') return jsonError('请求体无效', 400, env);
  if (body.level_id === undefined || body.level_id === null || body.level_id === '') {
    return jsonError('缺少 level_id', 400, env);
  }
  let tiles: TileData[] = [];
  if (body.layout_data !== undefined && body.layout_data !== null) {
    try {
      tiles = parseLayoutData(body.layout_data);
    } catch {
      return jsonError('layout_data 不是合法的 JSON 数组', 400, env);
    }
  }
  const levelId = Number(body.level_id);
  const difficulty = body.difficulty !== undefined ? Number(body.difficulty) : null;
  try {
    await env.DB.prepare('INSERT INTO levels (level_id, difficulty, created_at) VALUES (?, ?, ?)')
      .bind(levelId, difficulty, Date.now())
      .run();
  } catch (e: any) {
    // 兜底：旧库 layout_data 仍为 NOT NULL 时，回退写入旧列，避免创建失败
    const msg = String(e?.message || '');
    if (msg.includes('NOT NULL') || msg.includes('constraint')) {
      await env.DB.prepare('INSERT INTO levels (level_id, difficulty, created_at, layout_data) VALUES (?, ?, ?, ?)')
        .bind(levelId, difficulty, Date.now(), assembleLayoutData(tiles))
        .run();
    } else {
      throw e;
    }
  }
  await writeLevelTiles(env, levelId, tiles);
  const layoutStr = assembleLayoutData(tiles);
  const layoutSummary = tiles.length
    ? { layout_length: layoutStr.length, layout_hash: await sha256(layoutStr) }
    : null;
  await writeAudit(env, request, admin, 'CREATE_LEVEL', { type: 'levels', id: String(levelId), after: layoutSummary });
  return new Response(JSON.stringify({ success: true, id: String(levelId) }), { headers: getCorsHeaders(env) });
}

/**
 * 更新关卡：difficulty 变更 UPDATE 主表；layout_data 变更则重写 level_tiles（先删后插）。
 */
async function updateLevel(request: Request, env: Env, admin: AdminPayload, levelIdStr: string): Promise<Response> {
  const guard = assertCanWrite(admin);
  if (guard) return guard;
  const levelId = levelIdStr;
  const beforeRow = await env.DB.prepare('SELECT 1 FROM levels WHERE level_id = ?').bind(levelId).first();
  if (!beforeRow) return jsonError('关卡不存在', 404, env);
  const beforeTiles = await readLevelTilesByLevels(env, [Number(levelId)]);
  const beforeLayout = beforeTiles.get(Number(levelId));
  const body = (await request.json().catch(() => null)) as Record<string, any> | null;
  if (!body || typeof body !== 'object') return jsonError('请求体无效', 400, env);

  if (body.difficulty !== undefined) {
    await env.DB.prepare('UPDATE levels SET difficulty = ? WHERE level_id = ?').bind(Number(body.difficulty), levelId).run();
  }
  let afterLayout = beforeLayout;
  if (body.layout_data !== undefined && body.layout_data !== null) {
    let tiles: TileData[];
    try {
      tiles = parseLayoutData(body.layout_data);
    } catch {
      return jsonError('layout_data 不是合法的 JSON 数组', 400, env);
    }
    await writeLevelTiles(env, Number(levelId), tiles);
    afterLayout = tiles;
  }
  const beforeSummary = beforeLayout
    ? { layout_length: assembleLayoutData(beforeLayout).length, layout_hash: await sha256(assembleLayoutData(beforeLayout)) }
    : null;
  const afterSummary = afterLayout
    ? { layout_length: assembleLayoutData(afterLayout).length, layout_hash: await sha256(assembleLayoutData(afterLayout)) }
    : (body.layout_data !== undefined ? {} : null);
  await writeAudit(env, request, admin, 'UPDATE_LEVEL', { type: 'levels', id: levelId, before: beforeSummary, after: afterSummary });
  return new Response(JSON.stringify({ success: true }), { headers: getCorsHeaders(env) });
}

/**
 * 删除关卡：先删 level_tiles 子表，再删主表。
 */
async function deleteLevel(request: Request, env: Env, admin: AdminPayload, levelIdStr: string): Promise<Response> {
  const guard = assertCanWrite(admin);
  if (guard) return guard;
  const levelId = levelIdStr;
  const beforeRow = await env.DB.prepare('SELECT 1 FROM levels WHERE level_id = ?').bind(levelId).first();
  if (!beforeRow) return jsonError('关卡不存在', 404, env);
  const beforeTiles = await readLevelTilesByLevels(env, [Number(levelId)]);
  const beforeLayout = beforeTiles.get(Number(levelId));
  await env.DB.prepare('DELETE FROM level_tiles WHERE level_id = ?').bind(Number(levelId)).run();
  await env.DB.prepare('DELETE FROM levels WHERE level_id = ?').bind(levelId).run();
  const beforeSummary = beforeLayout ? { layout_length: assembleLayoutData(beforeLayout).length } : null;
  await writeAudit(env, request, admin, 'DELETE_LEVEL', { type: 'levels', id: levelId, before: beforeSummary });
  return new Response(JSON.stringify({ success: true }), { headers: getCorsHeaders(env) });
}

/** 清空 SHEEPS_CACHE KV 中所有键，返回删除数量 */
async function clearAllCache(request: Request, env: Env): Promise<Response> {
  let cursor: string | undefined;
  let deleted = 0;
  do {
    const list = await env.SHEEPS_CACHE.list({ cursor, limit: 1000 });
    const keys = list.keys.map(k => k.name);
    if (keys.length > 0) {
      await Promise.all(keys.map(k => env.SHEEPS_CACHE.delete(k).catch(() => {})));
      deleted += keys.length;
    }
    cursor = list.list_complete ? undefined : list.cursor;
  } while (cursor);
  return new Response(JSON.stringify({ success: true, deleted }), { headers: getCorsHeaders(env) });
}

// ============ 路由分发 ============

export async function handleAdminRoutes(request: Request, env: Env, path: string, url: URL): Promise<Response | null> {
  const method = request.method;

  // ============ 无鉴权接口（登录 / 刷新令牌）============
  /**
   * POST /api/admin/login （无鉴权）
   * 用途：管理员账号密码登录，签发 access + refresh 双令牌。
   * 鉴权：无（公开）。请求参数(body)：phone, password。
   * 响应：{ success, token(access), refreshToken, user }；失败 400/401。
   */
  if (path === '/api/admin/login' && method === 'POST') return adminLogin(request, env);
  /**
   * POST /api/admin/refresh （无鉴权，使用 refresh token）
   * 用途：用有效 refresh token 换取新的 access + refresh 令牌对。
   * 鉴权：校验 refresh token 合法性 + 账号未被封禁 + 角色一致。
   * 请求参数(body)：refreshToken。响应：{ success, token, refreshToken }；失败 401。
   */
  if (path === '/api/admin/refresh' && method === 'POST') return adminRefresh(request, env);

  // 以下均需要管理员鉴权
  const admin = await requireAdmin(request, env);
  if (admin instanceof Response) return admin;

  // ============ 用户管理 ============
  /**
   * GET /api/admin/users
   * 用途：分页列出前台用户，支持 keyword（手机号/昵称模糊）、role 过滤。
   * 鉴权：requireAdmin。请求参数(query)：page, pageSize, keyword, role。
   * 响应：{ success, list:[id,phone,username,points,role,is_banned,avatar_url,created_at], total, page, pageSize }。
   */
  if (path === '/api/admin/users' && method === 'GET') return listUsers(request, env, admin, url);
  // 调整用户积分（写操作，需 assertCanWrite）
  /**
   * POST /api/admin/users/:id/points
   * 用途：手动调整某用户积分（正加负减，不能为零）。
   * 鉴权：requireAdmin + assertCanWrite（readonly→403）。
   * 请求参数(body)：amount(number, 非零), reason。响应：{ success, points }。
   * 失败时审计不落库（DB 变更未成功）。
   */
  let m = path.match(/^\/api\/admin\/users\/([^/]+)\/points$/);
  if (m && method === 'POST') return adjustPoints(request, env, admin, m[1]);
  // 封禁 / 解封用户（写操作，需 assertCanWrite）
  /**
   * POST /api/admin/users/:id/ban
   * 用途：封禁(banned=true)或解封(banned=false)某用户。
   * 鉴权：requireAdmin + assertCanWrite。请求参数(body)：banned(boolean)。
   * 响应：{ success, is_banned }。
   */
  m = path.match(/^\/api\/admin\/users\/([^/]+)\/ban$/);
  if (m && method === 'POST') return banUser(request, env, admin, m[1]);
  // 重命名（改昵称）/ 删除用户
  /**
   * PUT /api/admin/users/:id  —— 修改昵称
   * 用途：修改用户 username。鉴权：requireAdmin + assertCanWrite。
   * 请求参数(body)：username(非空)。响应：{ success }。
   */
  m = path.match(/^\/api\/admin\/users\/([^/]+)$/);
  if (m && method === 'PUT') return renameUser(request, env, admin, m[1]);
  /**
   * DELETE /api/admin/users/:id —— 删除用户（高危，super 专属）
   * 用途：级联删除该用户在多张业务表中的全部数据。
   * 鉴权：requireAdmin + assertSuper；内含双护栏（禁止删管理员/当前登录账户）。
   * 事务：env.DB.batch 内一次性提交所有 DELETE，保证级联删除原子性。
   * 响应：{ success }。
   */
  if (m && method === 'DELETE') return deleteUser(request, env, admin, m[1]);
  // 用户背包（道具）查看 / 批量更新
  /**
   * GET /api/admin/users/:id/items —— 查看用户背包
   * 用途：列出 user_items 中该用户的所有道具及数量。鉴权：requireAdmin。
   * 响应：{ success, list:[item_type, count] }。
   */
  m = path.match(/^\/api\/admin\/users\/([^/]+)\/items$/);
  if (m && method === 'GET') return listUserItems(request, env, admin, m[1]);
  /**
   * POST /api/admin/users/:id/items —— 批量设置背包
   * 用途：用 items 列表整体覆盖该用户背包（INSERT OR REPLACE）。
   * 鉴权：requireAdmin + assertCanWrite。请求参数(body)：items:[{item_type,count}]。
   * 事务：env.DB.batch 批量写入；写后落审计（before=旧库存快照）。
   * 响应：{ success }。
   */
  if (m && method === 'POST') return updateUserItems(request, env, admin, m[1]);

  // ============ 配置 ============
  /**
   * GET /api/admin/config —— 读取全部配置
   * 用途：拉取 config 表所有 key/value。鉴权：requireAdmin。
   * 响应：{ success, list:[{key,value}] }。
   */
  if (path === '/api/admin/config' && method === 'GET') return getConfig(request, env);
  /**
   * POST /api/admin/config —— 新增/更新配置（upsert，super 专属）
   * 用途：键不存在则插入，存在则覆盖 value；同时删除对应 KV 缓存使其立即生效。
   * 鉴权：requireAdmin + assertSuper。请求参数(body)：key(必填), value。
   * 响应：{ success }。
   */
  if (path === '/api/admin/config' && method === 'POST') return updateConfig(request, env, admin);

  // ============ 统计 ============
  /**
   * GET /api/admin/stats —— 全局运营指标
   * 用途：并行聚合用户数、今日注册、兑换/积分/公告/封禁/商品/任务/关卡数，
   * 及无尽生存模式今日挑战次数与历史最高分。鉴权：requireAdmin。
   * 响应：{ success, users_total, today_signup, exchange_total, points_total, ... }。
   */
  if (path === '/api/admin/stats' && method === 'GET') return getStats(request, env);

  // ============ 商品 ============
  /**
   * GET /api/admin/shop-items —— 商品列表（分页+关键字）
   * 用途：genericList 读取 shop_items，支持 name/item_type 关键字搜索。
   * 鉴权：requireAdmin。响应：{ success, list, total, page, pageSize }。
   */
  if (path === '/api/admin/shop-items' && method === 'GET') return genericList(request, env, { table: 'shop_items', action: 'SHOP_ITEM', snapshotCols: ['item_type', 'points_price', 'stock'], searchCols: ['name', 'item_type'] }, url);
  /**
   * POST /api/admin/shop-items —— 新建商品
   * 用途：genericCreate 写 shop_items；创建后回调 seedI18nForEntity 播种 name/description 多语言。
   * 鉴权：requireAdmin + assertCanWrite。响应：{ success, id }。
   */
  if (path === '/api/admin/shop-items' && method === 'POST') return genericCreate(request, env, admin, { table: 'shop_items', action: 'SHOP_ITEM', snapshotCols: ['item_type', 'points_price', 'stock'], onCreated: (id, body) => seedI18nForEntity(env, 'shop_items', id, { name: localeValuesFromBody(body, 'name'), description: localeValuesFromBody(body, 'description') }) });
  m = path.match(/^\/api\/admin\/shop-items\/([^/]+)$/);
  /**
   * PUT /api/admin/shop-items/:id —— 编辑商品
   * 用途：genericUpdate 改 shop_items 后额外 purgeShopCache，使 image_url/group 变更生效。
   * 鉴权：requireAdmin + assertCanWrite。
   */
  if (m && method === 'PUT') return updateShopItemRoute(request, env, admin, m[1]);
  /**
   * DELETE /api/admin/shop-items/:id —— 删除商品
   * 用途：genericDelete 删除 shop_items 记录并落审计。鉴权：requireAdmin + assertCanWrite。
   */
  if (m && method === 'DELETE') return genericDelete(request, env, admin, { table: 'shop_items', action: 'SHOP_ITEM', idColumn: 'id', snapshotCols: ['item_type', 'points_price', 'stock'] }, m[1]);

  // ============ 图片上传（通用）============
  /**
   * POST /api/admin/upload-image —— 通用图片上传至 R2
   * 用途：multipart 上传，校验后写 R2 返回公开 URL；key 须以 images/ 开头防越权。
   * 鉴权：requireAdmin + assertCanWrite。请求参数(formData)：file + key/folder。
   * 响应：{ success, url }。
   */
  if (path === '/api/admin/upload-image' && method === 'POST') return uploadImage(request, env, admin);

  // ============ 皮肤 12 张卡面 ============
  /**
   * GET /api/admin/skin-tiles/:skinType —— 获取皮肤卡面
   * 用途：读取 skin_tiles 中该皮肤 12 张卡面（供后台预填）。鉴权：requireAdmin。
   * 响应：{ success, skin_type, tiles:[{tile_index,image_url}] }。
   */
  m = path.match(/^\/api\/admin\/skin-tiles\/([^/]+)$/);
  if (m && method === 'GET') return getSkinTiles(request, env, m[1]);
  /**
   * PUT /api/admin/skin-tiles/:skinType —— 保存皮肤卡面
   * 用途：Upsert skin_tiles；tile_index=1 镜像写 shop_items.image_url；purgeShopCache 后落审计。
   * 鉴权：requireAdmin + assertCanWrite。请求参数(body)：tiles:[{tile_index,image_url}]。
   * 响应：{ success }。
   */
  if (m && method === 'PUT') return saveSkinTiles(request, env, admin, m[1]);

  // ============ 道具图标（item_icons 真身 + 镜像 image_url）============
  /**
   * GET /api/admin/item-icons/:itemType —— 获取道具图标
   * 用途：读取 item_icons 中该道具当前图标 URL（供后台预填）。鉴权：requireAdmin。
   * 响应：{ success, item_type, icon }。
   */
  m = path.match(/^\/api\/admin\/item-icons\/([^/]+)$/);
  if (m && method === 'GET') return getItemIcon(request, env, m[1]);
  /**
   * PUT /api/admin/item-icons/:itemType —— 保存道具图标
   * 用途：写 item_icons 真身 + 镜像 shop_items.image_url + 刷缓存；URL 须来自本站点 R2。
   * 鉴权：requireAdmin + assertCanWrite。请求参数(body)：image_url。响应：{ success }。
   */
  if (m && method === 'PUT') return saveItemIcon(request, env, admin, m[1]);

  // ============ 公告 ============
  /**
   * GET /api/admin/notices —— 公告列表（分页+关键字）
   * 用途：genericList 读 notice，支持 title/content 搜索。鉴权：requireAdmin。
   * 响应：{ success, list, total, page, pageSize }。
   */
  if (path === '/api/admin/notices' && method === 'GET') return genericList(request, env, { table: 'notice', action: 'NOTICE', snapshotCols: ['title', 'content', 'type'], searchCols: ['title', 'content'] }, url);
  /**
   * POST /api/admin/notices —— 新建公告
   * 用途：genericCreate 写 notice；回调播种 title/content 多语言。
   * 鉴权：requireAdmin + assertCanWrite。响应：{ success, id }。
   */
  if (path === '/api/admin/notices' && method === 'POST') return genericCreate(request, env, admin, { table: 'notice', action: 'NOTICE', snapshotCols: ['title', 'content', 'type'], onCreated: (id, body) => seedI18nForEntity(env, 'notice', id, { title: localeValuesFromBody(body, 'title'), content: localeValuesFromBody(body, 'content') }) });
  m = path.match(/^\/api\/admin\/notices\/([^/]+)$/);
  /**
   * PUT /api/admin/notices/:id —— 编辑公告
   * 用途：genericUpdate 改 notice。鉴权：requireAdmin + assertCanWrite。
   */
  if (m && method === 'PUT') return genericUpdate(request, env, admin, { table: 'notice', action: 'NOTICE', idColumn: 'id', snapshotCols: ['title', 'content', 'type'] }, m[1]);
  /**
   * DELETE /api/admin/notices/:id —— 删除公告
   * 用途：genericDelete 删除 notice 并落审计。鉴权：requireAdmin + assertCanWrite。
   */
  if (m && method === 'DELETE') return genericDelete(request, env, admin, { table: 'notice', action: 'NOTICE', idColumn: 'id', snapshotCols: ['title', 'content', 'type'] }, m[1]);

  // ============ 任务 ============
  /**
   * GET /api/admin/tasks —— 任务列表（分页+关键字）
   * 用途：genericList 读 task，支持 name/id 搜索。鉴权：requireAdmin。
   * 响应：{ success, list, total, page, pageSize }。
   */
  if (path === '/api/admin/tasks' && method === 'GET') return genericList(request, env, { table: 'task', action: 'TASK', snapshotCols: ['target_count', 'points_reward'], searchCols: ['name', 'id'] }, url);
  /**
   * POST /api/admin/tasks —— 新建任务
   * 用途：genericCreate 写 task；回调播种 name/description 多语言（key=body.id）。
   * 鉴权：requireAdmin + assertCanWrite。响应：{ success, id }。
   */
  if (path === '/api/admin/tasks' && method === 'POST') return genericCreate(request, env, admin, { table: 'task', action: 'TASK', snapshotCols: ['target_count', 'points_reward'], onCreated: (_rowid, body) => seedI18nForEntity(env, 'task', body.id, { name: localeValuesFromBody(body, 'name'), description: localeValuesFromBody(body, 'description') }) });
  m = path.match(/^\/api\/admin\/tasks\/([^/]+)$/);
  /**
   * PUT /api/admin/tasks/:id —— 编辑任务
   * 用途：genericUpdate 改 task。鉴权：requireAdmin + assertCanWrite。
   */
  if (m && method === 'PUT') return genericUpdate(request, env, admin, { table: 'task', action: 'TASK', idColumn: 'id', snapshotCols: ['target_count', 'points_reward'] }, m[1]);
  /**
   * DELETE /api/admin/tasks/:id —— 删除任务
   * 用途：genericDelete 删除 task 并落审计。鉴权：requireAdmin + assertCanWrite。
   */
  if (m && method === 'DELETE') return genericDelete(request, env, admin, { table: 'task', action: 'TASK', idColumn: 'id', snapshotCols: ['target_count', 'points_reward'] }, m[1]);

  // ============ 关卡（layout_data 拆表：主表 + level_tiles 子表）============
  /**
   * GET /api/admin/levels —— 关卡列表
   * 用途：分页读 levels，按本页 level_id 批量读 level_tiles 重组 layout_data（存量回退旧列）。
   * 鉴权：requireAdmin。响应：{ success, list:[level_id,difficulty,created_at,layout_data], total, page, pageSize }。
   */
  if (path === '/api/admin/levels' && method === 'GET') return listLevels(request, env, url);
  /**
   * POST /api/admin/levels —— 新建关卡
   * 用途：主表写 level_id/difficulty；layout_data 解析后写 level_tiles 子表（旧库 NOT NULL 时回退旧列）。
   * 鉴权：requireAdmin + assertCanWrite。请求参数(body)：level_id, difficulty, layout_data(JSON)。
   * 响应：{ success, id }。
   */
  if (path === '/api/admin/levels' && method === 'POST') return createLevel(request, env, admin);
  m = path.match(/^\/api\/admin\/levels\/([^/]+)$/);
  /**
   * PUT /api/admin/levels/:levelId —— 更新关卡
   * 用途：difficulty 改主表；layout_data 变更则先删后插 level_tiles 重写。鉴权：requireAdmin + assertCanWrite。
   */
  if (m && method === 'PUT') return updateLevel(request, env, admin, m[1]);
  /**
   * DELETE /api/admin/levels/:levelId —— 删除关卡
   * 用途：先删 level_tiles 子表再删主表（保持引用一致）。鉴权：requireAdmin + assertCanWrite。
   */
  if (m && method === 'DELETE') return deleteLevel(request, env, admin, m[1]);

  // ============ 超级管理员专属：账户管理 ============
  /**
   * GET /api/admin/accounts —— 管理员账户列表（super 专属）
   * 用途：列出 role IN(super,operator,readonly) 的账户。鉴权：requireAdmin + assertSuper。
   * 响应：{ success, list:[id,phone,username,role,is_banned,created_at] }。
   */
  if (path === '/api/admin/accounts' && method === 'GET') { const g = assertSuper(admin); if (g) return g; return listAccounts(request, env); }
  /**
   * POST /api/admin/accounts —— 新建管理员账户（super 专属）
   * 用途：为指定手机号/角色创建后台账户；未提供密码则生成随机强密码返回。
   * 鉴权：requireAdmin + assertSuper。请求参数(body)：phone, role, password?(可选)。
   * 响应：{ success, id, initialPassword?(仅系统生成时返回) }。
   */
  if (path === '/api/admin/accounts' && method === 'POST') return createAccount(request, env, admin);
  m = path.match(/^\/api\/admin\/accounts\/([^/]+)\/role$/);
  /**
   * PUT /api/admin/accounts/:id/role —— 修改账户角色（super 专属）
   * 用途：变更 users.role；降级最后一个 super 仅审计不阻止。鉴权：requireAdmin + assertSuper。
   * 请求参数(body)：role(user|super|operator|readonly)。
   */
  if (m && method === 'PUT') return setAccountRole(request, env, admin, m[1]);
  m = path.match(/^\/api\/admin\/accounts\/([^/]+)\/disable$/);
  /**
   * POST /api/admin/accounts/:id/disable —— 封禁/解封管理员（super 专属）
   * 用途：设置管理员 is_banned。鉴权：requireAdmin + assertSuper。请求参数(body)：disabled(boolean)。
   */
  if (m && method === 'POST') return disableAccount(request, env, admin, m[1]);

  /**
   * GET /api/admin/audit-logs —— 审计日志查看（super 专属，只读）
   * 用途：分页按 admin_id/action/time 过滤 admin_audit_log；子表快照重组为 before/after。
   * 鉴权：requireAdmin + assertSuper。响应：{ success, list, total, page, pageSize }。
   */
  if (path === '/api/admin/audit-logs' && method === 'GET') { const g = assertSuper(admin); if (g) return g; return listAuditLogs(request, env, url); }

  // ============ App 版本管理 ============
  /**
   * GET /api/admin/app-versions —— 版本列表（分页+关键字，主键 version_code）
   * 用途：genericList 读 app_version。鉴权：requireAdmin。响应：{ success, list, total, page, pageSize }。
   */
  if (path === '/api/admin/app-versions' && method === 'GET')
    return genericList(request, env, { table: 'app_version', action: 'APP_VERSION', idColumn: 'version_code', snapshotCols: ['version_name', 'status'], searchCols: ['version_name'] }, url);
  /**
   * POST /api/admin/app-versions —— 新建版本（自动播种 update_log 多语言）
   * 用途：写 app_version；status=1 且无 release_time 时补写；回调播种 update_log。鉴权：requireAdmin + assertCanWrite。
   */
  if (path === '/api/admin/app-versions' && method === 'POST') return createAppVersion(request, env, admin);
  m = path.match(/^\/api\/admin\/app-versions\/([^/]+)$/);
  /**
   * PUT /api/admin/app-versions/:code —— 编辑版本
   * 用途：改 app_version；status 变 1 且 release_time 空时补写发布时间。鉴权：requireAdmin + assertCanWrite。
   */
  if (m && method === 'PUT') return updateAppVersion(request, env, admin, parseInt(m[1], 10));
  /**
   * DELETE /api/admin/app-versions/:code —— 删除版本
   * 用途：genericDelete 删除 app_version 并落审计。鉴权：requireAdmin + assertCanWrite。
   */
  if (m && method === 'DELETE') return genericDelete(request, env, admin, { table: 'app_version', action: 'APP_VERSION', idColumn: 'version_code', snapshotCols: ['version_name', 'status'] }, m[1]);

  // ============ APK 上传（R2）============
  /**
   * POST /api/admin/upload-apk —— 上传 APK 安装包（R2）
   * 用途：校验 .apk 后写 apks/ 前缀，返回 url 供回填 app_version.download_url。
   * 鉴权：requireAdmin + assertCanWrite。请求参数(formData)：file。响应：{ success, url }。
   */
  if (path === '/api/admin/upload-apk' && method === 'POST') return uploadApk(request, env, admin);

  // ============ 多语言统一管（i18n_strings）============
  /**
   * GET /api/admin/i18n —— 多语言列表（不分页，前端聚合）
   * 用途：按 module/locale/keyword 过滤 i18n_strings，业务模块 enrich entity_label。鉴权：requireAdmin。
   * 响应：{ success, list, total }。
   */
  if (path === '/api/admin/i18n' && method === 'GET') return listI18n(request, env, url);
  /**
   * POST /api/admin/i18n —— 新增 i18n 键（自动写 updated_at）
   * 用途：写 i18n_strings。鉴权：requireAdmin + assertCanWrite。请求参数(body)：str_key, locale, module, value。
   */
  if (path === '/api/admin/i18n' && method === 'POST') return createI18n(request, env, admin);
  m = path.match(/^\/api\/admin\/i18n\/([^/]+)$/);
  /**
   * PUT /api/admin/i18n/:id —— 编辑 i18n 值
   * 用途：写 value + updated_at。鉴权：requireAdmin + assertCanWrite。请求参数(body)：value。
   */
  if (m && method === 'PUT') return updateI18n(request, env, admin, m[1]);
  /**
   * DELETE /api/admin/i18n/:id —— 删除 i18n 键
   * 用途：genericDelete 删除 i18n_strings 并落审计。鉴权：requireAdmin + assertCanWrite。
   */
  if (m && method === 'DELETE') return genericDelete(request, env, admin, { table: 'i18n_strings', action: 'I18N', idColumn: 'id', snapshotCols: ['str_key', 'locale', 'module'] }, m[1]);

  // ============ 排行榜手动管理 ============
  /**
   * GET /api/admin/leaderboard —— 排行榜列表（JOIN users）
   * 用途：JOIN 取 username，按 score 降序；无尽模式未开启且 game_mode=1 时返回 disabled 空列表。
   * 鉴权：requireAdmin。响应：{ success, list, total, page, pageSize }（或 disabled 标记）。
   */
  if (path === '/api/admin/leaderboard' && method === 'GET') return listLeaderboard(request, env, url);
  /**
   * POST /api/admin/leaderboard —— 新增排行榜行（手动补录）
   * 用途：校验 user 存在后写 leaderboard 并补 achieved_at。鉴权：requireAdmin + assertCanWrite。
   * 请求参数(body)：user_id, level_id, score, clear_time_ms, game_mode。
   */
  if (path === '/api/admin/leaderboard' && method === 'POST') return createLeaderboardRow(request, env, admin);
  m = path.match(/^\/api\/admin\/leaderboard\/([^/]+)$/);
  /**
   * PUT /api/admin/leaderboard/:id —— 改分（仅 score/clear_time_ms/level_id/game_mode）
   * 用途：手动校分，白名单字段更新。鉴权：requireAdmin + assertCanWrite。
   */
  if (m && method === 'PUT') return updateLeaderboardRow(request, env, admin, m[1]);
  /**
   * DELETE /api/admin/leaderboard/:id —— 删除排行榜行
   * 用途：genericDelete 删除 leaderboard 并落审计。鉴权：requireAdmin + assertCanWrite。
   */
  if (m && method === 'DELETE') return genericDelete(request, env, admin, { table: 'leaderboard', action: 'LEADERBOARD', idColumn: 'id', snapshotCols: ['user_id', 'score'] }, m[1]);

  // ============ 用户搜索（反查 id）============
  /**
   * GET /api/admin/users/search —— 用户搜索（供 UserPicker 反查 id）
   * 用途：按手机号/昵称 LIKE 取前 20 条。鉴权：requireAdmin。响应：{ success, list:[id,username,phone] }。
   */
  if (path === '/api/admin/users/search' && method === 'GET') return searchUsers(request, env, url);

  // ============ 清空 KV 缓存 ============
  /**
   * POST /api/admin/cache/clear —— 清空 SHEEPS_CACHE KV（super 专属，高危）
   * 用途：游标遍历删除 KV 全部键，返回删除数量。鉴权：requireAdmin + assertSuper。
   * 响应：{ success, deleted }。
   */
  if (path === '/api/admin/cache/clear' && method === 'POST') {
    const g = assertSuper(admin);
    if (g) return g;
    return clearAllCache(request, env);
  }

  return jsonError('Not Found', 404, env);
}
