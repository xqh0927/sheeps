import { Env } from './types';
import { getCorsHeaders, getLangSuffix } from './helpers';
import { handleWebSocketSession } from './websocket';
import { handleAuthRoutes } from './handlers/auth';
import { handleMatchRoutes } from './handlers/match';
import { handleUserRoutes, handleAvatarProxy } from './handlers/user';
import { handleShopRoutes } from './handlers/shop';
import { handleTaskRoutes } from './handlers/task';
import { handleGameRoutes } from './handlers/game';
import { handleSystemRoutes } from './handlers/system';
import { decryptRequest, encryptResponse } from './middleware';
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
  schemaMigrated = true;
}

/**
 * Cloudflare Workers 全局入口 fetch 事件处理器
 */
export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    const corsHeaders = getCorsHeaders();

    // 0. 执行 D1 Schema 自动迁移（首次请求时触发）
    await migrateSchema(env);

    // 1. 处理 OPTIONS 预检请求以允许跨域
    if (request.method === 'OPTIONS') {
      return new Response(null, { headers: corsHeaders });
    }

    const url = new URL(request.url);
    const path = url.pathname;
    const lang = getLangSuffix(request);

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
       * 系统模块 — 覆盖接口:
       *   GET  /api/notice/list — 公告列表
       *   GET  /api/admin/config — 获取管理员配置
       *   POST /api/admin/config — 修改管理员配置
       *   GET  /api/app/check-update — App版本更新检测
       */
      } else if (path.startsWith('/api/admin') || path.startsWith('/api/app') || path.startsWith('/api/notice')) {
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