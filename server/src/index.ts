import { Env } from './types';
import { getCorsHeaders, getLangSuffix } from './helpers';
import { handleWebSocketSession } from './websocket';
import { handleAuthRoutes } from './handlers/auth';
import { handleMatchRoutes } from './handlers/match';
import { handleUserRoutes } from './handlers/user';
import { handleShopRoutes } from './handlers/shop';
import { handleTaskRoutes } from './handlers/task';
import { handleGameRoutes } from './handlers/game';
import { handleSystemRoutes } from './handlers/system';

import { OpenAPIHono } from '@hono/zod-openapi';
import { swaggerUI } from '@hono/swagger-ui';

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
 * Cloudflare Workers 全局入口 fetch 事件处理器
 */
export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    const corsHeaders = getCorsHeaders();

    // 1. 处理 OPTIONS 预检请求以允许跨域
    if (request.method === 'OPTIONS') {
      return new Response(null, { headers: corsHeaders });
    }

    const url = new URL(request.url);
    const path = url.pathname;
    const lang = getLangSuffix(request);

    // 2. 处理 WebSocket 联机对决协议升级路由
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

    // 3. 模块化 HTTP 路由精准分发
    try {
      let response: Response | null = null;

      if (path.startsWith('/api/auth')) {
        response = await handleAuthRoutes(request, env, path);
      } else if (path.startsWith('/api/match')) {
        response = await handleMatchRoutes(request, env, path, url);
      } else if (path.startsWith('/api/user')) {
        response = await handleUserRoutes(request, env, path);
      } else if (path.startsWith('/api/shop')) {
        response = await handleShopRoutes(request, env, path, lang);
      } else if (path.startsWith('/api/task')) {
        response = await handleTaskRoutes(request, env, path, lang);
      } else if (path.startsWith('/api/admin') || path.startsWith('/api/app') || path.startsWith('/api/notice')) {
        response = await handleSystemRoutes(request, env, path, lang, url);
      } else if (path.startsWith('/api/level') || path.startsWith('/api/score') || path.startsWith('/api/sign') || path.startsWith('/api/leaderboard')) {
        response = await handleGameRoutes(request, env, path, url);
      }

      // 如果对应的业务路由匹配并成功响应则直接返回；否则抛出 404 Not Found
      return response || new Response(JSON.stringify({ error: 'Not Found' }), { status: 404, headers: corsHeaders });

    } catch (e: any) {
      // 捕获未知异常，防止无服务计算崩溃导致请求直接挂死
      return new Response(JSON.stringify({ error: e.message || 'Internal Server Error' }), { status: 500, headers: corsHeaders });
    }
  }
};