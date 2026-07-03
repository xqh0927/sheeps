"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.handleShopRoutes = handleShopRoutes;
const helpers_1 = require("../helpers");
/**
 * 处理积分商城相关的 HTTP 路由请求
 * 包含获取多语言商品列表（配合缓存）、以及使用积分兑换游戏法宝道具（支持事务原子性）。
 *
 * @param request 客户端 HTTP 请求对象
 * @param env 环境变量及 D1 数据库实例
 * @param path 请求的 URL 路径
 * @param lang 客户端语言标识（如 'en'、'zh-CN'）用于国际化商品显示
 * @return 匹配路由时返回 Response 对象，未匹配时返回 null
 */
async function handleShopRoutes(request, env, path, lang) {
    const corsHeaders = (0, helpers_1.getCorsHeaders)();
    // 1. 查询积分商城商品列表接口（支持多语言与 KV 缓存）
    if (path === '/api/shop/items' && request.method === 'GET') {
        const cacheKey = `shop_items_${lang}`;
        // 读取 Cloudflare KV 缓存，减少频繁读库开销（缓存时间 10 分钟）
        const cached = await env.SHEEPS_CACHE.get(cacheKey);
        if (cached)
            return new Response(cached, { headers: corsHeaders });
        // SQL 国际化回退机制：若传入的语言对应列不存在，回退至系统默认字段
        const nameCol = lang ? `COALESCE(name_${lang}, name)` : 'name';
        const descCol = lang ? `COALESCE(description_${lang}, description)` : 'description';
        const items = await env.DB.prepare(`SELECT id, ${nameCol} as name, ${descCol} as description, image_url, item_type, points_price, stock FROM shop_items`).all();
        const jsonStr = JSON.stringify(items.results);
        await env.SHEEPS_CACHE.put(cacheKey, jsonStr, { expirationTtl: 600 });
        return new Response(jsonStr, { headers: corsHeaders });
    }
    // 2. 积分兑换商品/道具接口
    if (path === '/api/shop/exchange' && request.method === 'POST') {
        const authUser = await (0, helpers_1.getAuthenticatedUser)(request, env);
        if (!authUser)
            return new Response(JSON.stringify({ error: 'Unauthorized' }), { status: 401, headers: corsHeaders });
        const body = await request.json();
        if (body.shop_item_id === undefined || !body.count || body.count < 1)
            return new Response(JSON.stringify({ error: 'Invalid parameters' }), { status: 400, headers: corsHeaders });
        // 批量读取商品信息和用户当前可用积分
        const [shopItemResult, userResult] = await env.DB.batch([
            env.DB.prepare('SELECT name, item_type, points_price, stock FROM shop_items WHERE id = ?').bind(body.shop_item_id),
            env.DB.prepare('SELECT points FROM users WHERE id = ?').bind(authUser.userId)
        ]);
        const shopItem = shopItemResult.results[0];
        if (!shopItem || shopItem.stock < body.count)
            return new Response(JSON.stringify({ error: 'Item out of stock' }), { status: 400, headers: corsHeaders });
        const totalCost = shopItem.points_price * body.count;
        const userRow = userResult.results[0];
        if (!userRow || userRow.points < totalCost)
            return new Response(JSON.stringify({ error: 'Insufficient points' }), { status: 400, headers: corsHeaders });
        const remainingPoints = userRow.points - totalCost;
        // 执行原子扣积分、减库存、添加用户背包道具（ON CONFLICT 时叠加数量）、记录兑换及积分消费明细
        const writeResults = await env.DB.batch([
            env.DB.prepare('UPDATE users SET points = ? WHERE id = ?').bind(remainingPoints, authUser.userId),
            env.DB.prepare('UPDATE shop_items SET stock = stock - ? WHERE id = ?').bind(body.count, body.shop_item_id),
            env.DB.prepare('INSERT INTO user_items (user_id, item_type, count) VALUES (?, ?, ?) ON CONFLICT(user_id, item_type) DO UPDATE SET count = count + ?').bind(authUser.userId, shopItem.item_type, body.count, body.count),
            env.DB.prepare('INSERT INTO exchange_record (user_id, shop_item_id, item_type, count, points_cost, created_at) VALUES (?, ?, ?, ?, ?, ?)').bind(authUser.userId, body.shop_item_id, shopItem.item_type, body.count, totalCost, Date.now()),
            env.DB.prepare('INSERT INTO point_record (user_id, type, amount, source, remaining_points, created_at) VALUES (?, ?, ?, ?, ?, ?)').bind(authUser.userId, 'OUT', totalCost, `SHOP_REDEEM_${shopItem.item_type}`, remainingPoints, Date.now()),
            env.DB.prepare('SELECT count FROM user_items WHERE user_id = ? AND item_type = ?').bind(authUser.userId, shopItem.item_type)
        ]);
        // 由于库存及数据发生变化，主动清除全部多语言商城的 KV 缓存，确保下一次查询数据最新
        for (const l of ['', 'en', 'tw', 'ja', 'ko'])
            await env.SHEEPS_CACHE.delete(`shop_items_${l}`);
        return new Response(JSON.stringify({ success: true, item_type: shopItem.item_type, new_count: writeResults[5].results[0]?.count || 0, remaining_points: remainingPoints }), { headers: corsHeaders });
    }
    return null;
}
