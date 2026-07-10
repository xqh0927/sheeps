import { Env } from './types';
import { getCorsHeaders, getLangSuffix, translateErrorMessage } from './helpers';
import { ensureI18nSeeded } from './i18n';
import { configureSecrets } from './crypto';
import { handleWebSocketSession } from './websocket';
import { handleAuthRoutes } from './handlers/auth';
import { handleMatchRoutes } from './handlers/match';
import { handleUserRoutes, handleAvatarProxy } from './handlers/user';
import { handleShopRoutes } from './handlers/shop';
import { handleTaskRoutes } from './handlers/task';
import { handleGameRoutes } from './handlers/game';
import { handleSystemRoutes } from './handlers/system';
import { handleAdminRoutes } from './handlers/admin';
import { backfillLevels, backfillBackups, backfillAudit } from './lib/migrate';
import { decryptRequest, encryptResponse } from './middleware';
import { privacyHtml, agreementHtml } from './legal-docs';
export {
  parseReleaseVersionCode,
  findApkAsset,
  isForceUpdateRelease,
  mapGitHubReleaseToUpdate,
  checkApkExists,
  clearApkCache,
  getDatabaseAppUpdate,
} from './update';

import { OpenAPIHono } from '@hono/zod-openapi';
import { swaggerUI } from '@hono/swagger-ui';

/** 标记 D1 schema 迁移是否已执行（Worker 实例级缓存） */
let schemaMigrated = false;

// 创建一个支持 OpenAPI 规范的 Hono 实例（备用框架）
const app = new OpenAPIHono<{ Bindings: Env }>();

// 1. 挂载 Swagger UI 接口文档端点（预留支持，通过 /doc 可以查阅开放 API）
app.get('/doc', swaggerUI({ url: '/doc/openapi.json' }));

// 2. 预留 Hono 框架路由分发示例
app.post('/api/auth/login', async (c) => {
  const env = c.env;
  const request = c.req.raw;
  const response = await handleAuthRoutes(request, env, '/api/auth/login');
  return response || c.json({ error: 'Not found' }, 404);
});

/**
 * 执行 D1 数据库 schema 自动迁移
 * 幂等操作：若列已存在则忽略错误
 */
async function migrateSchema(env: Env): Promise<void> {
  if (schemaMigrated) return;
  try {
    await env.DB.prepare('ALTER TABLE users ADD COLUMN password_hash TEXT').run();
  } catch (_) { /* 列已存在，忽略 */ }
  try {
    await env.DB.prepare('ALTER TABLE users ADD COLUMN avatar_url TEXT').run();
  } catch (_) { /* 列已存在，忽略 */ }
  // 管理后台：三级角色模型（取代 is_admin 二值）+ 封禁标记
  try {
    await env.DB.prepare("ALTER TABLE users ADD COLUMN role TEXT DEFAULT 'user'").run();
  } catch (_) { /* 列已存在，忽略 */ }
  try {
    await env.DB.prepare('ALTER TABLE users ADD COLUMN is_banned INTEGER DEFAULT 0').run();
  } catch (_) { /* 列已存在，忽略 */ }
  // 管理后台：审计日志表（不可改/删，仅由后台写接口 INSERT）
  try {
    await env.DB.prepare(`CREATE TABLE IF NOT EXISTS admin_audit_log (
      id              INTEGER PRIMARY KEY AUTOINCREMENT,
      admin_id        TEXT    NOT NULL,
      admin_phone     TEXT    NOT NULL,
      admin_role      TEXT    NOT NULL,
      action          TEXT    NOT NULL,
      target_type     TEXT,
      target_id       TEXT,
      source_ip       TEXT,
      user_agent      TEXT,
      created_at      INTEGER NOT NULL
    )`).run();
  } catch (_) { /* 表已存在，忽略 */ }
  try {
    await env.DB.prepare('CREATE INDEX IF NOT EXISTS idx_audit_admin ON admin_audit_log(admin_id)').run();
  } catch (_) { /* 索引已存在，忽略 */ }
  try {
    await env.DB.prepare('CREATE INDEX IF NOT EXISTS idx_audit_created ON admin_audit_log(created_at)').run();
  } catch (_) { /* 索引已存在，忽略 */ }

  // 图片资源 CDN（v2）：skin_tiles / item_icons 表 + shop_items."group" 列
  try {
    await env.DB.prepare(
      `CREATE TABLE IF NOT EXISTS skin_tiles (
        skin_type   TEXT    NOT NULL,
        tile_index  INTEGER NOT NULL,
        image_url   TEXT    NOT NULL,
        PRIMARY KEY (skin_type, tile_index)
      )`
    ).run();
  } catch (_) { /* 表已存在，忽略 */ }
  try {
    await env.DB.prepare('CREATE INDEX IF NOT EXISTS idx_skin_tiles_type ON skin_tiles(skin_type)').run();
  } catch (_) { /* 索引已存在，忽略 */ }
  try {
    await env.DB.prepare(
      `CREATE TABLE IF NOT EXISTS item_icons (
        item_type  TEXT    NOT NULL PRIMARY KEY,
        image_url  TEXT    NOT NULL
      )`
    ).run();
  } catch (_) { /* 表已存在，忽略 */ }
  try {
    // "group" 为 SQL 关键字，需用双引号包裹
    await env.DB.prepare('ALTER TABLE shop_items ADD COLUMN "group" TEXT').run();
  } catch (_) { /* 列已存在，忽略 */ }

  // ============ 方案 B：i18n 归一化底座 ============
  // 1) i18n_strings 单表（建表幂等）
  try {
    await env.DB.prepare(`CREATE TABLE IF NOT EXISTS i18n_strings (
      id         INTEGER PRIMARY KEY AUTOINCREMENT,
      str_key    TEXT    NOT NULL,
      locale     TEXT    NOT NULL DEFAULT 'zh',
      module     TEXT    NOT NULL DEFAULT 'system',
      category   TEXT,
      value      TEXT,
      updated_at INTEGER,
      UNIQUE(str_key, locale, module, category)
    )`).run();
  } catch (_) { /* 表已存在，忽略 */ }
  try {
    await env.DB.prepare('CREATE INDEX IF NOT EXISTS idx_i18n ON i18n_strings(module, category, locale)').run();
  } catch (_) { /* 索引已存在，忽略 */ }
  try {
    await env.DB.prepare('CREATE INDEX IF NOT EXISTS idx_i18n_key ON i18n_strings(str_key, locale)').run();
  } catch (_) { /* 索引已存在，忽略 */ }

  // 2) app_version 加列（download_url / status / release_time，幂等）
  try {
    await env.DB.prepare('ALTER TABLE app_version ADD COLUMN download_url TEXT').run();
  } catch (_) { /* 列已存在，忽略 */ }
  try {
    await env.DB.prepare('ALTER TABLE app_version ADD COLUMN status INTEGER DEFAULT 0').run();
  } catch (_) { /* 列已存在，忽略 */ }
  try {
    await env.DB.prepare('ALTER TABLE app_version ADD COLUMN release_time INTEGER').run();
  } catch (_) { /* 列已存在，忽略 */ }
  // 历史数据回填：将原 apk_url 同步到 download_url
  try {
    await env.DB.prepare('UPDATE app_version SET download_url = apk_url WHERE download_url IS NULL').run();
  } catch (_) { /* 无数据或列缺失，忽略 */ }

  // 3) config 种子：游戏模式开关（默认闯关开、无尽关）
  try {
    await env.DB.prepare("INSERT OR IGNORE INTO config (key, value) VALUES ('gamemode_stage', 'on')").run();
    await env.DB.prepare("INSERT OR IGNORE INTO config (key, value) VALUES ('gamemode_endless', 'off')").run();
    await env.DB.prepare("INSERT OR IGNORE INTO config (key, value) VALUES ('gamemode_battle', 'off')").run();
  } catch (_) { /* 键已存在，忽略 */ }

  // 3.5) shop_items 种子：冻结符（FREEZE），幂等插入
  try {
    await env.DB.prepare("INSERT OR IGNORE INTO shop_items (name, description, item_type, points_price, stock) VALUES ('冻结符 (Freeze)', '暂停下落4秒，获得操作窗口', 'FREEZE', 35, 30)").run();
  } catch (_) { /* 已存在，忽略 */ }

  // 4) ETL 兜底：若 i18n_strings 为空，触发一次宽列→i18n 迁移
  await ensureI18nSeeded(env);

  // ============ JSON 字符串列拆表迁移（T01~T04） ============
  // 1) 新建子表（幂等）
  try {
    await env.DB.prepare(`CREATE TABLE IF NOT EXISTS level_tiles (
      level_id INTEGER NOT NULL,
      tile_index INTEGER NOT NULL,
      tile_id TEXT NOT NULL,
      x INTEGER,
      y INTEGER,
      z INTEGER,
      "type" INTEGER,
      is_blind INTEGER DEFAULT 0,
      sealed_count INTEGER DEFAULT 0,
      seal_unlock_threshold INTEGER,
      PRIMARY KEY (level_id, tile_index),
      FOREIGN KEY(level_id) REFERENCES levels(level_id)
    )`).run();
  } catch (_) { /* 表已存在，忽略 */ }
  try { await env.DB.prepare('CREATE INDEX IF NOT EXISTS idx_level_tiles_level ON level_tiles(level_id)').run(); } catch (_) {}
  try {
    await env.DB.prepare(`CREATE TABLE IF NOT EXISTS backup_unlocked_levels (
      backup_id INTEGER NOT NULL,
      level_id INTEGER NOT NULL,
      PRIMARY KEY (backup_id, level_id),
      FOREIGN KEY(backup_id) REFERENCES backup_save_log(id)
    )`).run();
  } catch (_) {}
  try { await env.DB.prepare('CREATE INDEX IF NOT EXISTS idx_backup_unlock_backup ON backup_unlocked_levels(backup_id)').run(); } catch (_) {}
  try {
    await env.DB.prepare(`CREATE TABLE IF NOT EXISTS backup_save_items (
      backup_id INTEGER NOT NULL,
      item_type TEXT NOT NULL,
      count INTEGER NOT NULL DEFAULT 0,
      PRIMARY KEY (backup_id, item_type),
      FOREIGN KEY(backup_id) REFERENCES backup_save_log(id)
    )`).run();
  } catch (_) {}
  try { await env.DB.prepare('CREATE INDEX IF NOT EXISTS idx_backup_items_backup ON backup_save_items(backup_id)').run(); } catch (_) {}
  try {
    await env.DB.prepare(`CREATE TABLE IF NOT EXISTS admin_audit_changes (
      change_id INTEGER NOT NULL,
      field TEXT NOT NULL,
      old_val TEXT,
      new_val TEXT,
      PRIMARY KEY (change_id, field),
      FOREIGN KEY(change_id) REFERENCES admin_audit_log(id)
    )`).run();
  } catch (_) {}
  try { await env.DB.prepare('CREATE INDEX IF NOT EXISTS idx_audit_changes_change ON admin_audit_changes(change_id)').run(); } catch (_) {}

  // 2) backup_save_log 提升 points 标量列（幂等）
  try {
    await env.DB.prepare('ALTER TABLE backup_save_log ADD COLUMN points INTEGER').run();
  } catch (_) { /* 列已存在，忽略 */ }

  // 3) 彻底删除 deprecated JSON 列（不再保留），单条 ALTER + try/catch 保证幂等
  await dropDeprecatedColumn(env, 'levels', 'layout_data');
  await dropDeprecatedColumn(env, 'backup_save_log', 'save_data');
  await dropDeprecatedColumn(env, 'admin_audit_log', 'before_snapshot');
  await dropDeprecatedColumn(env, 'admin_audit_log', 'after_snapshot');

  // 4) users.avatar 合并到 avatar_url 后删除 avatar 列
  try {
    await env.DB.prepare(
      "UPDATE users SET avatar_url = avatar WHERE avatar IS NOT NULL AND (avatar_url IS NULL OR avatar_url = '')"
    ).run();
  } catch (_) { /* 无数据或列缺失，忽略 */ }
  await dropDeprecatedColumn(env, 'users', 'avatar');

  // 5) 补索引（高频轮询 / 定期清理），幂等
  try { await env.DB.prepare('CREATE INDEX IF NOT EXISTS idx_game_commands_game_id ON game_commands(game_id)').run(); } catch (_) {}
  try { await env.DB.prepare('CREATE INDEX IF NOT EXISTS idx_backup_created ON backup_save_log(created_at)').run(); } catch (_) {}

  // 6) 存量数据回填（幂等，失败不阻断）
  await backfillLevels(env);
  await backfillBackups(env);
  await backfillAudit(env);

  // 7) 清理宽列（D1 归一化）：移除各业务表的多语言宽列，统一由 i18n_strings 管理
  // shop_items 8 列
  await dropDeprecatedColumn(env, 'shop_items', 'name_en');
  await dropDeprecatedColumn(env, 'shop_items', 'name_tw');
  await dropDeprecatedColumn(env, 'shop_items', 'name_ja');
  await dropDeprecatedColumn(env, 'shop_items', 'name_ko');
  await dropDeprecatedColumn(env, 'shop_items', 'description_en');
  await dropDeprecatedColumn(env, 'shop_items', 'description_tw');
  await dropDeprecatedColumn(env, 'shop_items', 'description_ja');
  await dropDeprecatedColumn(env, 'shop_items', 'description_ko');
  // task 8 列
  await dropDeprecatedColumn(env, 'task', 'name_en');
  await dropDeprecatedColumn(env, 'task', 'name_tw');
  await dropDeprecatedColumn(env, 'task', 'name_ja');
  await dropDeprecatedColumn(env, 'task', 'name_ko');
  await dropDeprecatedColumn(env, 'task', 'description_en');
  await dropDeprecatedColumn(env, 'task', 'description_tw');
  await dropDeprecatedColumn(env, 'task', 'description_ja');
  await dropDeprecatedColumn(env, 'task', 'description_ko');
  // notice 8 列
  await dropDeprecatedColumn(env, 'notice', 'title_en');
  await dropDeprecatedColumn(env, 'notice', 'title_tw');
  await dropDeprecatedColumn(env, 'notice', 'title_ja');
  await dropDeprecatedColumn(env, 'notice', 'title_ko');
  await dropDeprecatedColumn(env, 'notice', 'content_en');
  await dropDeprecatedColumn(env, 'notice', 'content_tw');
  await dropDeprecatedColumn(env, 'notice', 'content_ja');
  await dropDeprecatedColumn(env, 'notice', 'content_ko');
  // app_version 4 列
  await dropDeprecatedColumn(env, 'app_version', 'update_log_en');
  await dropDeprecatedColumn(env, 'app_version', 'update_log_tw');
  await dropDeprecatedColumn(env, 'app_version', 'update_log_ja');
  await dropDeprecatedColumn(env, 'app_version', 'update_log_ko');

  schemaMigrated = true;
}

/**
 * 安全地删除某列（用于清理 deprecated / 合并的冗余列）。
 * 使用单条 ALTER TABLE DROP COLUMN，绝不塞进 env.DB.batch()（D1 的 batch 对 DDL 不可靠，
 * 且 RENAME 会自动把子表外键改成临时名、DROP 后悬空）。
 * 列不存在时静默忽略，保证迁移幂等。
 */
async function dropDeprecatedColumn(
  env: Env,
  table: string,
  column: string
): Promise<void> {
  try {
    await env.DB.prepare(`ALTER TABLE ${table} DROP COLUMN ${column}`).run();
  } catch (_) { /* 列已不存在，忽略 */ }
}

/**
 * Cloudflare Workers 全局入口 fetch 事件处理器
 */
export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    // 0. 注入生产密钥（Worker Secrets）；开发环境保留 fallback 常量
    configureSecrets(env);

    const corsHeaders = getCorsHeaders(env);

    // 1. 执行 D1 Schema 自动迁移（首次请求时触发）
    await migrateSchema(env);

    // 1. 处理 OPTIONS 预检请求以允许跨域
    if (request.method === 'OPTIONS') {
      return new Response(null, { headers: corsHeaders });
    }

    const url = new URL(request.url);
    const path = url.pathname;
    const lang = getLangSuffix(request);

    // 1.2 处理静态法律文件路由
    if (request.method === 'GET') {
      if (path === '/privacy.html' || path === '/api/legal/privacy') {
        return new Response(privacyHtml, {
          headers: {
            'Content-Type': 'text/html; charset=utf-8',
            'Access-Control-Allow-Origin': '*'
          }
        });
      }
      if (path === '/agreement.html' || path === '/api/legal/agreement') {
        return new Response(agreementHtml, {
          headers: {
            'Content-Type': 'text/html; charset=utf-8',
            'Access-Control-Allow-Origin': '*'
          }
        });
      }
    }

    /**
     * 2. WebSocket 联机对决协议升级路由
     *
     * 覆盖接口:
     *   GET /api/ws?gameId=&playerId= — WebSocket 升级，用于双人实时对决通信
     */
    if (path === '/api/ws' || request.headers.get('Upgrade') === 'websocket') {
      const gameId = url.searchParams.get('gameId');
      const playerId = url.searchParams.get('playerId');
      if (!gameId || !playerId) return new Response('Missing gameId or playerId', { status: 400 });

      // 使用 Web API 创建双向 WebSocket 信道对
      const [client, server] = Object.values(new WebSocketPair());
      // 后端服务器接受连接，启动多人局内状态机监听
      server.accept();
      handleWebSocketSession(server, gameId, playerId, env);
      // 返回 101 Switching Protocols 升级应答，握手成功
      return new Response(null, { status: 101, webSocket: client, headers: corsHeaders });
    }

    // 3. 加密中间件：解密请求体（若请求包含 encrypted 字段）
    const ENCRYPTION_ENABLED = true;
    const { request: decryptedRequest, wasEncrypted } = ENCRYPTION_ENABLED
      ? await decryptRequest(request)
      : { request, wasEncrypted: false };

    // 4. 模块化 HTTP 路由精准分发
    try {
      let response: Response | null = null;

      /**
       * 头像代理 — 覆盖接口:
       *   GET /api/avatar/:userId — 从 R2 读取用户头像并返回
       */
      if (path.startsWith('/api/avatar/') && request.method === 'GET') {
        response = await handleAvatarProxy(decryptedRequest, env, path);

        /**
         * 认证模块 — 覆盖接口:
         *   POST /api/auth/send-code — 发送验证码
         *   POST /api/auth/login — 验证码登录/注册
         *   POST /api/auth/login-password — 密码登录
         *   POST /api/auth/register — 注册
         *   POST /api/auth/reset-password — 重置密码
         *   GET  /api/auth/check-password — 检查是否已设密码
         *   POST /api/auth/set-password — 设置密码
         *   POST /api/auth/refresh — 刷新Token
         */
      } else if (path.startsWith('/api/auth')) {
        response = await handleAuthRoutes(decryptedRequest, env, path);

        /**
         * 匹配模块 — 覆盖接口:
         *   POST /api/match/join — 加入匹配队列
         *   GET  /api/match/status — 轮询匹配状态
         *   POST /api/match/leave — 离开匹配队列
         */
      } else if (path.startsWith('/api/match')) {
        response = await handleMatchRoutes(decryptedRequest, env, path, url);

        /**
         * 用户模块 — 覆盖接口:
         *   POST /api/user/sync — 端云数据同步
         *   GET  /api/user/profile — 获取用户Profile
         *   GET  /api/user/points-history — 积分流水查询
         *   GET  /api/user/exchange-history — 兑换记录查询
         *   POST /api/user/rename — 修改昵称
         *   POST /api/user/avatar — 上传头像
         */
      } else if (path.startsWith('/api/user')) {
        response = await handleUserRoutes(decryptedRequest, env, path);

        /**
         * 商城模块 — 覆盖接口:
         *   GET  /api/shop/items — 商品列表
         *   POST /api/shop/exchange — 积分兑换道具
         */
      } else if (path.startsWith('/api/shop')) {
        response = await handleShopRoutes(decryptedRequest, env, path, lang);

        /**
         * 任务模块 — 覆盖接口:
         *   GET  /api/task/daily — 每日任务列表
         *   POST /api/task/claim — 领取任务奖励
         */
      } else if (path.startsWith('/api/task')) {
        response = await handleTaskRoutes(decryptedRequest, env, path, lang);

        /**
         * 管理后台模块 — 覆盖接口（见 handlers/admin.ts）：
         *   POST /api/admin/login — 管理员登录（无鉴权）
         *   POST /api/admin/refresh — 刷新访问令牌（无鉴权）
         *   GET/POST/PUT/DELETE /api/admin/* — 其余后台接口（先过 requireAdmin 三级校验）
         */
      } else if (path.startsWith('/api/admin')) {
        response = await handleAdminRoutes(decryptedRequest, env, path, url);

        /**
         * 系统模块 — 覆盖接口:
         *   GET  /api/notice/list — 公告列表
         *   GET  /api/app/check-update — App版本更新检测
         */
      } else if (path.startsWith('/api/app') || path.startsWith('/api/notice')) {
        response = await handleSystemRoutes(decryptedRequest, env, path, lang, url);

        /**
         * 游戏模块 — 覆盖接口:
         *   GET  /api/level — 获取关卡布局
         *   POST /api/level/unlock — 积分解锁关卡
         *   POST /api/score/submit — 提交通关成绩
         *   POST /api/sign/today — 每日签到
         *   GET  /api/leaderboard — 排行榜查询
         *   GET  /api/leaderboard/daily-popup — 每日弹窗
         */
      } else if (path.startsWith('/api/level') || path.startsWith('/api/score') || path.startsWith('/api/sign') || path.startsWith('/api/leaderboard')) {
        response = await handleGameRoutes(decryptedRequest, env, path, url);
      }

      // 如果对应的业务路由匹配并成功响应则直接返回；否则抛出 404 Not Found
      let finalResponse = response || new Response(JSON.stringify({ error: 'Not Found' }), { status: 404, headers: corsHeaders });

      // JSON 错误国际化翻译
      if (finalResponse.headers.get('Content-Type')?.includes('application/json')) {
        try {
          const responseClone = finalResponse.clone();
          const jsonBody = await responseClone.json() as any;
          if (jsonBody && typeof jsonBody === 'object' && jsonBody.error) {
            const originalError = jsonBody.error;
            const translatedError = translateErrorMessage(originalError, lang);
            if (translatedError !== originalError) {
              jsonBody.error = translatedError;
              finalResponse = new Response(JSON.stringify(jsonBody), {
                status: finalResponse.status,
                headers: finalResponse.headers
              });
            }
          }
        } catch (_) {
          // 忽略解析错误
        }
      }

      // 5. 加密中间件：若原始请求是加密的，则加密响应体
      if (wasEncrypted) {
        finalResponse = await encryptResponse(finalResponse);
      }

      return finalResponse;

    } catch (e: any) {
      // 捕获未知异常，防止无服务计算崩溃导致请求直接挂死
      return new Response(JSON.stringify({ error: e.message || 'Internal Server Error' }), { status: 500, headers: corsHeaders });
    }
  }
};