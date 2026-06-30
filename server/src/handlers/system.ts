import { Env } from '../types';
import { getCorsHeaders } from '../helpers';
import { getDatabaseAppUpdate } from '../update';

export async function handleSystemRoutes(request: Request, env: Env, path: string, lang: string, url: URL): Promise<Response | null> {
    const corsHeaders = getCorsHeaders();

    if (path === '/api/notice/list' && request.method === 'GET') {
        const cacheKey = `notices_${lang}`;
        const cached = await env.SHEEPS_CACHE.get(cacheKey);
        if (cached) return new Response(cached, { headers: corsHeaders });

        const titleCol = lang ? `COALESCE(title_${lang}, title)` : 'title';
        const contentCol = lang ? `COALESCE(content_${lang}, content)` : 'content';
        const notices = await env.DB.prepare(`SELECT id, ${titleCol} as title, ${contentCol} as content, type, created_at FROM notice ORDER BY created_at DESC`).all();

        const jsonStr = JSON.stringify(notices.results);
        await env.SHEEPS_CACHE.put(cacheKey, jsonStr, { expirationTtl: 3600 });
        return new Response(jsonStr, { headers: corsHeaders });
    }

    if (path === '/api/admin/config' && request.method === 'GET') {
        const cached = await env.SHEEPS_CACHE.get('admin_config_list');
        if (cached) return new Response(cached, { headers: corsHeaders });

        const list = await env.DB.prepare('SELECT key, value FROM config').all();
        const jsonStr = JSON.stringify(list.results);
        await env.SHEEPS_CACHE.put('admin_config_list', jsonStr, { expirationTtl: 600 });
        return new Response(jsonStr, { headers: corsHeaders });
    }

    if (path === '/api/admin/config' && request.method === 'POST') {
        const body: { key: string; value: string } = await request.json();
        if (!body.key || !body.value) return new Response(JSON.stringify({ error: 'Missing config key or value' }), { status: 400, headers: corsHeaders });

        await env.DB.prepare('INSERT OR REPLACE INTO config (key, value) VALUES (?, ?)').bind(body.key, body.value).run();
        await env.SHEEPS_CACHE.delete('admin_config_list');
        return new Response(JSON.stringify({ success: true }), { headers: corsHeaders });
    }

    if (path === '/api/app/check-update' && request.method === 'GET') {
        const currentCodeStr = url.searchParams.get('version_code');
        const databaseUpdate = await getDatabaseAppUpdate(env, currentCodeStr ? parseInt(currentCodeStr, 10) : 1);
        return new Response(JSON.stringify(databaseUpdate), { headers: corsHeaders });
    }

    return null;
}