"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const helpers_1 = require("./helpers");
const websocket_1 = require("./websocket");
const auth_1 = require("./handlers/auth");
const match_1 = require("./handlers/match");
const user_1 = require("./handlers/user");
const shop_1 = require("./handlers/shop");
const task_1 = require("./handlers/task");
const game_1 = require("./handlers/game");
const system_1 = require("./handlers/system");
const zod_openapi_1 = require("@hono/zod-openapi");
const swagger_ui_1 = require("@hono/swagger-ui");
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
 * Cloudflare Workers 全局入口 fetch 事件处理器
 */
exports.default = {
    async fetch(request, env) {
        const corsHeaders = (0, helpers_1.getCorsHeaders)();
        // 1. 处理 OPTIONS 预检请求以允许跨域
        if (request.method === 'OPTIONS') {
            return new Response(null, { headers: corsHeaders });
        }
        const url = new URL(request.url);
        const path = url.pathname;
        const lang = (0, helpers_1.getLangSuffix)(request);
        // 2. 处理 WebSocket 联机对决协议升级路由
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
        // 3. 模块化 HTTP 路由精准分发
        try {
            let response = null;
            if (path.startsWith('/api/auth')) {
                response = await (0, auth_1.handleAuthRoutes)(request, env, path);
            }
            else if (path.startsWith('/api/match')) {
                response = await (0, match_1.handleMatchRoutes)(request, env, path, url);
            }
            else if (path.startsWith('/api/user')) {
                response = await (0, user_1.handleUserRoutes)(request, env, path);
            }
            else if (path.startsWith('/api/shop')) {
                response = await (0, shop_1.handleShopRoutes)(request, env, path, lang);
            }
            else if (path.startsWith('/api/task')) {
                response = await (0, task_1.handleTaskRoutes)(request, env, path, lang);
            }
            else if (path.startsWith('/api/admin') || path.startsWith('/api/app') || path.startsWith('/api/notice')) {
                response = await (0, system_1.handleSystemRoutes)(request, env, path, lang, url);
            }
            else if (path.startsWith('/api/level') || path.startsWith('/api/score') || path.startsWith('/api/sign') || path.startsWith('/api/leaderboard')) {
                response = await (0, game_1.handleGameRoutes)(request, env, path, url);
            }
            // 如果对应的业务路由匹配并成功响应则直接返回；否则抛出 404 Not Found
            return response || new Response(JSON.stringify({ error: 'Not Found' }), { status: 404, headers: corsHeaders });
        }
        catch (e) {
            // 捕获未知异常，防止无服务计算崩溃导致请求直接挂死
            return new Response(JSON.stringify({ error: e.message || 'Internal Server Error' }), { status: 500, headers: corsHeaders });
        }
    }
};
