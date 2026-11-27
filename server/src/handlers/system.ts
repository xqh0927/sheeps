import { Env } from '../types';
import { getCorsHeaders, getGameModeStatus } from '../helpers';
import { getDatabaseAppUpdate } from '../update';
import { publishedTextSql } from '../i18n';

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
     * GET /api/notice/list — 查询当前发布的多语言公告
     *
     * Query 参数（通过请求头 Accept-Language 解析）:
     *   @param {string} [lang] — 语言标识
     *
     * 响应: [{ id, title, content, type, created_at }]
     */
    if (path === '/api/notice/list' && request.method === 'GET') {
        const notices = await env.DB.prepare(`
            SELECT id, ${publishedTextSql('notice', 'title')}, ${publishedTextSql('notice', 'content')}, type, created_at
            FROM notice ORDER BY created_at DESC, id DESC
        `).bind(lang).all();
        return new Response(JSON.stringify(notices.results), { headers: { ...corsHeaders, 'Cache-Control': 'no-store' } });

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

        // Read the current release state on every check; the APK HEAD probe is cached by URL.
        const databaseUpdate = await getDatabaseAppUpdate(env, currentCode, lang);
        const gameModes = await getGameModeStatus(env);
        const responseData = JSON.stringify({ ...databaseUpdate, game_modes: gameModes });
        return new Response(responseData, { headers: { ...corsHeaders, 'Cache-Control': 'no-store' } });
    }

    return null;
}
