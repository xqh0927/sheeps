"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.handleSystemRoutes = handleSystemRoutes;
const helpers_1 = require("../helpers");
const update_1 = require("../update");
/**
 * 处理系统级业务相关的 HTTP 路由请求
 * 包含公告列表查询（多语言匹配）、管理员基础环境参数配置（读取与保存）、以及客户端 App 版本更新检测。
 *
 * @param request 客户端 HTTP 请求对象
 * @param env 环境变量及 D1 数据库实例
 * @param path 请求的 URL 路径
 * @param lang 客户端语言标识
 * @param url 完整的 URL 解析对象
 * @return 匹配路由时返回 Response 对象，未匹配时返回 null
 */
async function handleSystemRoutes(request, env, path, lang, url) {
    const corsHeaders = (0, helpers_1.getCorsHeaders)();
    // 1. 公告列表查询接口（支持多语言与 KV 缓存）
    if (path === '/api/notice/list' && request.method === 'GET') {
        const cacheKey = `notices_${lang}`;
        // 读取公告 KV 缓存，默认缓存 1 小时以降低 D1 读负荷
        const cached = await env.SHEEPS_CACHE.get(cacheKey);
        if (cached)
            return new Response(cached, { headers: corsHeaders });
        const titleCol = lang ? `COALESCE(title_${lang}, title)` : 'title';
        const contentCol = lang ? `COALESCE(content_${lang}, content)` : 'content';
        const notices = await env.DB.prepare(`SELECT id, ${titleCol} as title, ${contentCol} as content, type, created_at FROM notice ORDER BY created_at DESC`).all();
        const jsonStr = JSON.stringify(notices.results);
        await env.SHEEPS_CACHE.put(cacheKey, jsonStr, { expirationTtl: 3600 });
        return new Response(jsonStr, { headers: corsHeaders });
    }
    // 2. 获取管理员配置接口（常用于读取签到配置、解锁积分门槛等）
    if (path === '/api/admin/config' && request.method === 'GET') {
        const cached = await env.SHEEPS_CACHE.get('admin_config_list');
        if (cached)
            return new Response(cached, { headers: corsHeaders });
        const list = await env.DB.prepare('SELECT key, value FROM config').all();
        const jsonStr = JSON.stringify(list.results);
        await env.SHEEPS_CACHE.put('admin_config_list', jsonStr, { expirationTtl: 600 });
        return new Response(jsonStr, { headers: corsHeaders });
    }
    // 3. 写入/修改管理员配置接口
    if (path === '/api/admin/config' && request.method === 'POST') {
        const body = await request.json();
        if (!body.key || !body.value)
            return new Response(JSON.stringify({ error: 'Missing config key or value' }), { status: 400, headers: corsHeaders });
        // 保存配置到 config 表并主动删除旧缓存，保证后续读取能及时同步
        await env.DB.prepare('INSERT OR REPLACE INTO config (key, value) VALUES (?, ?)').bind(body.key, body.value).run();
        await env.SHEEPS_CACHE.delete('admin_config_list');
        return new Response(JSON.stringify({ success: true }), { headers: corsHeaders });
    }
    // 4. 客户端 App 版本更新检测接口
    if (path === '/api/app/check-update' && request.method === 'GET') {
        const currentCodeStr = url.searchParams.get('version_code');
        // 调用 update 模块进行 APK 的 HEAD 轻量级可用性探针检测与延迟缓存判定
        const databaseUpdate = await (0, update_1.getDatabaseAppUpdate)(env, currentCodeStr ? parseInt(currentCodeStr, 10) : 1);
        return new Response(JSON.stringify(databaseUpdate), { headers: corsHeaders });
    }
    return null;
}
