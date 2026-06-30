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
import { Env } from './types';
import { handleAuthRoutes } from './handlers/auth'; // 你的 handler 模块

// 创建一个支持 OpenAPI 的 Hono 实例
const app = new OpenAPIHono<{ Bindings: Env }>();

// 1. 挂载 Swagger UI 文档接口 (访问 /doc 即可直接点击测试)
app.get('/doc', swaggerUI({ url: '/doc/openapi.json' }));

// 2. 路由分发 (保持你现有的业务逻辑)
// Hono 允许你以更优雅的方式挂载路由
app.post('/api/auth/login', async (c) => {
  const env = c.env;
  const request = c.req.raw;
  // 调用你之前写好的 handler
  const response = await handleAuthRoutes(request, env, '/api/auth/login');
  return response || c.json({ error: 'Not found' }, 404);
});

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    const corsHeaders = getCorsHeaders();

    if (request.method === 'OPTIONS') {
      return new Response(null, { headers: corsHeaders });
    }

    const url = new URL(request.url);
    const path = url.pathname;
    const lang = getLangSuffix(request);

    // 1. WebSocket 联机对战升级路由
    if (path === '/api/ws' || request.headers.get('Upgrade') === 'websocket') {
      const gameId = url.searchParams.get('gameId');
      const playerId = url.searchParams.get('playerId');
      if (!gameId || !playerId) return new Response('Missing gameId or playerId', { status: 400 });

      const [client, server] = Object.values(new WebSocketPair());
      server.accept();
      handleWebSocketSession(server, gameId, playerId, env);
      return new Response(null, { status: 101, webSocket: client, headers: corsHeaders });
    }

    // 2. 模块化 HTTP 路由分发
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

      // 如果 handler 匹配并处理了路由，返回其结果；否则返回 404
      return response || new Response(JSON.stringify({ error: 'Not Found' }), { status: 404, headers: corsHeaders });

    } catch (e: any) {
      return new Response(JSON.stringify({ error: e.message || 'Internal Server Error' }), { status: 500, headers: corsHeaders });
    }
  }
};