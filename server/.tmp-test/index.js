"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.getDatabaseAppUpdate = exports.clearApkCache = exports.checkApkExists = exports.mapGitHubReleaseToUpdate = exports.isForceUpdateRelease = exports.findApkAsset = exports.parseReleaseVersionCode = void 0;
const helpers_1 = require("./helpers");
const crypto_1 = require("./crypto");
const websocket_1 = require("./websocket");
const auth_1 = require("./handlers/auth");
const match_1 = require("./handlers/match");
const user_1 = require("./handlers/user");
const shop_1 = require("./handlers/shop");
const task_1 = require("./handlers/task");
const game_1 = require("./handlers/game");
const system_1 = require("./handlers/system");
const admin_1 = require("./handlers/admin");
const middleware_1 = require("./middleware");
const legal_docs_1 = require("./legal-docs");
var update_1 = require("./update");
Object.defineProperty(exports, "parseReleaseVersionCode", { enumerable: true, get: function () { return update_1.parseReleaseVersionCode; } });
Object.defineProperty(exports, "findApkAsset", { enumerable: true, get: function () { return update_1.findApkAsset; } });
Object.defineProperty(exports, "isForceUpdateRelease", { enumerable: true, get: function () { return update_1.isForceUpdateRelease; } });
Object.defineProperty(exports, "mapGitHubReleaseToUpdate", { enumerable: true, get: function () { return update_1.mapGitHubReleaseToUpdate; } });
Object.defineProperty(exports, "checkApkExists", { enumerable: true, get: function () { return update_1.checkApkExists; } });
Object.defineProperty(exports, "clearApkCache", { enumerable: true, get: function () { return update_1.clearApkCache; } });
Object.defineProperty(exports, "getDatabaseAppUpdate", { enumerable: true, get: function () { return update_1.getDatabaseAppUpdate; } });
const zod_openapi_1 = require("@hono/zod-openapi");
const swagger_ui_1 = require("@hono/swagger-ui");
/** 标记 D1 schema 迁移是否已执行（Worker 实例级缓存） */
let schemaMigrated = false;
// 创建一个支持 OpenAPI 规范的 Hono 实例（备用框架）
const app = new zod_openapi_1.OpenAPIHono();
// 1. 挂载 Swagger UI 接口文档端点（预留支持，通过 /doc 可以查阅开放 API）
app.get('/doc', (0, swagger_ui_1.swaggerUI)({ url: '/doc/openapi.json' }));
// 2. 预留 Hono 框架路由分发示例
app.post('/api/auth/login', async (c) => {
    const env = c.env;
    const request = c.req.raw;
    const response = await (0, auth_1.handleAuthRoutes)(request, env, '/api/auth/login');
    return response || c.json({ error: 'Not found' }, 404);
});
/**
 * 执行 D1 数据库 schema 自动迁移
 * 幂等操作：若列已存在则忽略错误
 */
async function migrateSchema(env) {
    if (schemaMigrated)
        return;
    try {
        await env.DB.prepare('ALTER TABLE users ADD COLUMN password_hash TEXT').run();
    }
    catch (_) { /* 列已存在，忽略 */ }
    try {
        await env.DB.prepare('ALTER TABLE users ADD COLUMN avatar_url TEXT').run();
    }
    catch (_) { /* 列已存在，忽略 */ }
    // 管理后台：三级角色模型（取代 is_admin 二值）+ 封禁标记
    try {
        await env.DB.prepare("ALTER TABLE users ADD COLUMN role TEXT DEFAULT 'readonly'").run();
    }
    catch (_) { /* 列已存在，忽略 */ }
    try {
        await env.DB.prepare('ALTER TABLE users ADD COLUMN is_banned INTEGER DEFAULT 0').run();
    }
    catch (_) { /* 列已存在，忽略 */ }
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
      before_snapshot TEXT,
      after_snapshot  TEXT,
      source_ip       TEXT,
      user_agent      TEXT,
      created_at      INTEGER NOT NULL
    )`).run();
    }
    catch (_) { /* 表已存在，忽略 */ }
    try {
        await env.DB.prepare('CREATE INDEX IF NOT EXISTS idx_audit_admin ON admin_audit_log(admin_id)').run();
    }
    catch (_) { /* 索引已存在，忽略 */ }
    try {
        await env.DB.prepare('CREATE INDEX IF NOT EXISTS idx_audit_created ON admin_audit_log(created_at)').run();
    }
    catch (_) { /* 索引已存在，忽略 */ }
    schemaMigrated = true;
}
/**
 * Cloudflare Workers 全局入口 fetch 事件处理器
 */
exports.default = {
    async fetch(request, env) {
        // 0. 注入生产密钥（Worker Secrets）；开发环境保留 fallback 常量
        (0, crypto_1.configureSecrets)(env);
        const corsHeaders = (0, helpers_1.getCorsHeaders)(env);
        // 1. 执行 D1 Schema 自动迁移（首次请求时触发）
        await migrateSchema(env);
        // 1. 处理 OPTIONS 预检请求以允许跨域
        if (request.method === 'OPTIONS') {
            return new Response(null, { headers: corsHeaders });
        }
        const url = new URL(request.url);
        const path = url.pathname;
        const lang = (0, helpers_1.getLangSuffix)(request);
        // 1.2 处理静态法律文件路由
        if (request.method === 'GET') {
            if (path === '/privacy.html' || path === '/api/legal/privacy') {
                return new Response(legal_docs_1.privacyHtml, {
                    headers: {
                        'Content-Type': 'text/html; charset=utf-8',
                        'Access-Control-Allow-Origin': '*'
                    }
                });
            }
            if (path === '/agreement.html' || path === '/api/legal/agreement') {
                return new Response(legal_docs_1.agreementHtml, {
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
            if (!gameId || !playerId)
                return new Response('Missing gameId or playerId', { status: 400 });
            // 使用 Web API 创建双向 WebSocket 信道对
            const [client, server] = Object.values(new WebSocketPair());
            // 后端服务器接受连接，启动多人局内状态机监听
            server.accept();
            (0, websocket_1.handleWebSocketSession)(server, gameId, playerId, env);
            // 返回 101 Switching Protocols 升级应答，握手成功
            return new Response(null, { status: 101, webSocket: client, headers: corsHeaders });
        }
        // 3. 加密中间件：解密请求体（若请求包含 encrypted 字段）
        const ENCRYPTION_ENABLED = true;
        const { request: decryptedRequest, wasEncrypted } = ENCRYPTION_ENABLED
            ? await (0, middleware_1.decryptRequest)(request)
            : { request, wasEncrypted: false };
        // 4. 模块化 HTTP 路由精准分发
        try {
            let response = null;
            /**
             * 头像代理 — 覆盖接口:
             *   GET /api/avatar/:userId — 从 R2 读取用户头像并返回
             */
            if (path.startsWith('/api/avatar/') && request.method === 'GET') {
                response = await (0, user_1.handleAvatarProxy)(decryptedRequest, env, path);
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
            }
            else if (path.startsWith('/api/auth')) {
                response = await (0, auth_1.handleAuthRoutes)(decryptedRequest, env, path);
                /**
                 * 匹配模块 — 覆盖接口:
                 *   POST /api/match/join — 加入匹配队列
                 *   GET  /api/match/status — 轮询匹配状态
                 *   POST /api/match/leave — 离开匹配队列
                 */
            }
            else if (path.startsWith('/api/match')) {
                response = await (0, match_1.handleMatchRoutes)(decryptedRequest, env, path, url);
                /**
                 * 用户模块 — 覆盖接口:
                 *   POST /api/user/sync — 端云数据同步
                 *   GET  /api/user/profile — 获取用户Profile
                 *   GET  /api/user/points-history — 积分流水查询
                 *   GET  /api/user/exchange-history — 兑换记录查询
                 *   POST /api/user/rename — 修改昵称
                 *   POST /api/user/avatar — 上传头像
                 */
            }
            else if (path.startsWith('/api/user')) {
                response = await (0, user_1.handleUserRoutes)(decryptedRequest, env, path);
                /**
                 * 商城模块 — 覆盖接口:
                 *   GET  /api/shop/items — 商品列表
                 *   POST /api/shop/exchange — 积分兑换道具
                 */
            }
            else if (path.startsWith('/api/shop')) {
                response = await (0, shop_1.handleShopRoutes)(decryptedRequest, env, path, lang);
                /**
                 * 任务模块 — 覆盖接口:
                 *   GET  /api/task/daily — 每日任务列表
                 *   POST /api/task/claim — 领取任务奖励
                 */
            }
            else if (path.startsWith('/api/task')) {
                response = await (0, task_1.handleTaskRoutes)(decryptedRequest, env, path, lang);
                /**
                 * 管理后台模块 — 覆盖接口（见 handlers/admin.ts）：
                 *   POST /api/admin/login — 管理员登录（无鉴权）
                 *   POST /api/admin/refresh — 刷新访问令牌（无鉴权）
                 *   GET/POST/PUT/DELETE /api/admin/* — 其余后台接口（先过 requireAdmin 三级校验）
                 */
            }
            else if (path.startsWith('/api/admin')) {
                response = await (0, admin_1.handleAdminRoutes)(decryptedRequest, env, path, url);
                /**
                 * 系统模块 — 覆盖接口:
                 *   GET  /api/notice/list — 公告列表
                 *   GET  /api/app/check-update — App版本更新检测
                 */
            }
            else if (path.startsWith('/api/app') || path.startsWith('/api/notice')) {
                response = await (0, system_1.handleSystemRoutes)(decryptedRequest, env, path, lang, url);
                /**
                 * 游戏模块 — 覆盖接口:
                 *   GET  /api/level — 获取关卡布局
                 *   POST /api/level/unlock — 积分解锁关卡
                 *   POST /api/score/submit — 提交通关成绩
                 *   POST /api/sign/today — 每日签到
                 *   GET  /api/leaderboard — 排行榜查询
                 *   GET  /api/leaderboard/daily-popup — 每日弹窗
                 */
            }
            else if (path.startsWith('/api/level') || path.startsWith('/api/score') || path.startsWith('/api/sign') || path.startsWith('/api/leaderboard')) {
                response = await (0, game_1.handleGameRoutes)(decryptedRequest, env, path, url);
            }
            // 如果对应的业务路由匹配并成功响应则直接返回；否则抛出 404 Not Found
            let finalResponse = response || new Response(JSON.stringify({ error: 'Not Found' }), { status: 404, headers: corsHeaders });
            // JSON 错误国际化翻译
            if (finalResponse.headers.get('Content-Type')?.includes('application/json')) {
                try {
                    const responseClone = finalResponse.clone();
                    const jsonBody = await responseClone.json();
                    if (jsonBody && typeof jsonBody === 'object' && jsonBody.error) {
                        const originalError = jsonBody.error;
                        const translatedError = (0, helpers_1.translateErrorMessage)(originalError, lang);
                        if (translatedError !== originalError) {
                            jsonBody.error = translatedError;
                            finalResponse = new Response(JSON.stringify(jsonBody), {
                                status: finalResponse.status,
                                headers: finalResponse.headers
                            });
                        }
                    }
                }
                catch (_) {
                    // 忽略解析错误
                }
            }
            // 5. 加密中间件：若原始请求是加密的，则加密响应体
            if (wasEncrypted) {
                finalResponse = await (0, middleware_1.encryptResponse)(finalResponse);
            }
            return finalResponse;
        }
        catch (e) {
            // 捕获未知异常，防止无服务计算崩溃导致请求直接挂死
            return new Response(JSON.stringify({ error: e.message || 'Internal Server Error' }), { status: 500, headers: corsHeaders });
        }
    }
};
