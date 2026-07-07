import { Env } from '../types';
import { getCorsHeaders } from '../helpers';
import { getDatabaseAppUpdate } from '../update';

/**
 * 处理系统级业务相关的 HTTP 路由请求
 * 包含公告列表查询（多语言匹配）、客户端 App 版本更新检测。
 *
 * 注意：原 /api/admin/config（GET/POST）已迁移至 handlers/admin.ts 并加管理员鉴权，
 * 此处不再处理，避免无鉴权暴露配置写入口。
 *
 * @param request 客户端 HTTP 请求对象
 * @param env 环境变量及 D1 数据库实例
 * @param path 请求的 URL 路径
 * @param lang 客户端语言标识
 * @param url 完整的 URL 解析对象
 * @return 匹配路由时返回 Response 对象，未匹配时返回 null
 */
export async function handleSystemRoutes(request: Request, env: Env, path: string, lang: string, url: URL): Promise<Response | null> {
    const corsHeaders = getCorsHeaders();

    /**
     * GET /api/notice/list — 公告列表查询（支持多语言与KV缓存）
     *
     * Query 参数（通过请求头 Accept-Language 解析）:
     *   @param {string} [lang] — 语言标识
     *
     * 响应: [{ id, title, content, type, created_at }]
     */
    if (path === '/api/notice/list' && request.method === 'GET') {
        const cacheKey = `notices_${lang}_v2`;
        // 读取公告 KV 缓存，默认缓存 1 小时以降低 D1 读负荷
        const cached = await env.SHEEPS_CACHE.get(cacheKey);
        if (cached && cached.startsWith('[{')) return new Response(cached, { headers: corsHeaders });

        const titleCol = lang ? `COALESCE(title_${lang}, title)` : 'title';
        const contentCol = lang ? `COALESCE(content_${lang}, content)` : 'content';
        const notices = await env.DB.prepare(`SELECT id, ${titleCol} as title, ${contentCol} as content, type, created_at FROM notice ORDER BY created_at DESC`).all();

        const jsonStr = JSON.stringify(notices.results);
        await env.SHEEPS_CACHE.put(cacheKey, jsonStr, { expirationTtl: 3600 });
        return new Response(jsonStr, { headers: corsHeaders });
    }

    /**
     * GET /api/app/check-update — 客户端App版本更新检测
     *
     * Query 参数:
     *   @param {number} version_code — 当前客户端版本号
     *
     * 响应: { has_update, latest_version, download_url, update_info }
     */
    if (path === '/api/app/check-update' && request.method === 'GET') {
        const currentCodeStr = url.searchParams.get('version_code');
        const currentCode = currentCodeStr ? parseInt(currentCodeStr, 10) : 1;

        // KV 缓存避免频繁 D1 + HEAD 探测
        const cacheKey = `update_check_${currentCode}_${lang}`;
        const cached = await env.SHEEPS_CACHE.get(cacheKey);
        if (cached) return new Response(cached, { headers: corsHeaders });

        // 调用 update 模块进行 APK 的 HEAD 轻量级可用性探针检测与延迟缓存判定
        const databaseUpdate = await getDatabaseAppUpdate(env, currentCode, lang);
        const responseData = JSON.stringify(databaseUpdate);
        // 更新检测结果缓存 5 分钟（与 GitHub API 缓存对齐）
        await env.SHEEPS_CACHE.put(cacheKey, responseData, { expirationTtl: 300 });
        return new Response(responseData, { headers: corsHeaders });
    }

    return null;
}
