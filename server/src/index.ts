/**
 * Cloudflare Workers 主入口与 HTTP 请求处理管线
 * ============================================
 * 本文件是 Worker 的全局入口，所有入站请求都经下方 `fetch` 处理器按如下顺序处理：
 *
 *   0. configureSecrets(env)        — 注入 Worker Secrets（生产）/ fallback 常量（开发）。
 *   1. CORS 响应头计算              — 准备跨域头（后续各分支复用同一份 `corsHeaders`）。
 *      · D1 自动迁移已移除：避免首请求超时，schema 改由 schema.sql 手工维护。
 *   2. OPTIONS 预检                 — 直接返回空体响应，提前结束跨域握手。
 *   3. 静态法律文件                 — /privacy.html、/agreement.html 等 GET 端点短路返回 HTML。
 *   4. WebSocket 升级               — `/api/ws` 建立双向信道，移交联机对决状态机。
 *   5. /doc Swagger 短路            — /doc 与 /doc/openapi.json 绕过加解密，直接交给 Hono `app` 渲染文档。
 *   6. 加密中间件（解密）          — 若请求含 encrypted 字段，先 `decryptRequest` 还原明文再路由。
 *   7. 模块化路由分发              — 按 path 前缀依次匹配 avatar/auth/match/user/shop/task/admin/system/game。
 *   8. 404 兜底                     — 未命中任何业务路由时返回 JSON `{ error: 'Not Found' }`。
 *   9. i18n 错误翻译                — 对 JSON 错误体按请求语言本地化（translateErrorMessage）。
 *  10. 加密响应                     — 若原始请求是加密的，则 `encryptResponse` 后再返回。
 *   X. 异常兜底                     — 任何未捕获异常统一返回 500，防止实例崩溃。
 *
 * 关键约束：除第 3/5 步的静态与文档端点外，所有业务请求必须先经过第 6 步解密，
 * 才能在第 7 步被各 handler 正确识别与分发。
 */
import { Env } from './types';
import { getCorsHeaders, getLangSuffix, translateErrorMessage } from './helpers';
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
import openapiSpec from './openapi.json';

// 创建一个支持 OpenAPI 规范的 Hono 实例（用于托管 Swagger UI 文档）
const app = new OpenAPIHono<{ Bindings: Env }>();

// 1. 挂载 Swagger UI 接口文档端点（通过 /doc 查阅开放 API）
app.get('/doc', swaggerUI({ url: '/doc/openapi.json' }));
// 2. 直接返回手写的 OpenAPI spec（未使用 app.openapi() 注册，故显式提供 JSON）
app.get('/doc/openapi.json', (c) => c.json(openapiSpec as Record<string, unknown>));

/**
 * Cloudflare Workers 全局入口 fetch 事件处理器
 */
export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    // 0. 注入生产密钥（Worker Secrets）；开发环境保留 fallback 常量
    configureSecrets(env);

    const corsHeaders = getCorsHeaders(env);

    // D1 schema 自动迁移已移除：当前 D1 已是最新 schema（所有历史迁移均已完成）。
    // 变更表结构请手动执行 SQL（见 schema.sql），勿在请求路径里跑迁移（会导致首请求超时）。
    // 处理 OPTIONS 预检请求以允许跨域
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

    // 2.1 Swagger 文档端点：绕过加解密中间件，直接交给 app 实例处理
    if (path === '/doc' || path === '/doc/openapi.json') {
      return app.fetch(request);
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
