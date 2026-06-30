import { Env } from '../types';
import { getCorsHeaders, getAuthenticatedUser } from '../helpers';

export async function handleShopRoutes(request: Request, env: Env, path: string, lang: string): Promise<Response | null> {
    const corsHeaders = getCorsHeaders();

    if (path === '/api/shop/items' && request.method === 'GET') {
        const cacheKey = `shop_items_${lang}`;
        const cached = await env.SHEEPS_CACHE.get(cacheKey);
        if (cached) return new Response(cached, { headers: corsHeaders });

        const nameCol = lang ? `COALESCE(name_${lang}, name)` : 'name';
        const descCol = lang ? `COALESCE(description_${lang}, description)` : 'description';
        const items = await env.DB.prepare(`SELECT id, ${nameCol} as name, ${descCol} as description, image_url, item_type, points_price, stock FROM shop_items`).all();

        const jsonStr = JSON.stringify(items.results);
        await env.SHEEPS_CACHE.put(cacheKey, jsonStr, { expirationTtl: 600 });
        return new Response(jsonStr, { headers: corsHeaders });
    }

    if (path === '/api/shop/exchange' && request.method === 'POST') {
        const authUser = await getAuthenticatedUser(request, env);
        if (!authUser) return new Response(JSON.stringify({ error: 'Unauthorized' }), { status: 401, headers: corsHeaders });

        const body: { shop_item_id: number; count: number } = await request.json();
        if (body.shop_item_id === undefined || !body.count || body.count < 1) return new Response(JSON.stringify({ error: 'Invalid parameters' }), { status: 400, headers: corsHeaders });

        const [shopItemResult, userResult] = await env.DB.batch([
            env.DB.prepare('SELECT name, item_type, points_price, stock FROM shop_items WHERE id = ?').bind(body.shop_item_id),
            env.DB.prepare('SELECT points FROM users WHERE id = ?').bind(authUser.userId)
        ]);

        const shopItem = shopItemResult.results[0] as any;
        if (!shopItem || shopItem.stock < body.count) return new Response(JSON.stringify({ error: 'Item out of stock' }), { status: 400, headers: corsHeaders });

        const totalCost = shopItem.points_price * body.count;
        const userRow = userResult.results[0] as any;
        if (!userRow || userRow.points < totalCost) return new Response(JSON.stringify({ error: 'Insufficient points' }), { status: 400, headers: corsHeaders });

        const remainingPoints = userRow.points - totalCost;

        const writeResults = await env.DB.batch([
            env.DB.prepare('UPDATE users SET points = ? WHERE id = ?').bind(remainingPoints, authUser.userId),
            env.DB.prepare('UPDATE shop_items SET stock = stock - ? WHERE id = ?').bind(body.count, body.shop_item_id),
            env.DB.prepare('INSERT INTO user_items (user_id, item_type, count) VALUES (?, ?, ?) ON CONFLICT(user_id, item_type) DO UPDATE SET count = count + ?').bind(authUser.userId, shopItem.item_type, body.count, body.count),
            env.DB.prepare('INSERT INTO exchange_record (user_id, shop_item_id, item_type, count, points_cost, created_at) VALUES (?, ?, ?, ?, ?, ?)').bind(authUser.userId, body.shop_item_id, shopItem.item_type, body.count, totalCost, Date.now()),
            env.DB.prepare('INSERT INTO point_record (user_id, type, amount, source, remaining_points, created_at) VALUES (?, ?, ?, ?, ?, ?)').bind(authUser.userId, 'OUT', totalCost, `SHOP_REDEEM_${shopItem.item_type}`, remainingPoints, Date.now()),
            env.DB.prepare('SELECT count FROM user_items WHERE user_id = ? AND item_type = ?').bind(authUser.userId, shopItem.item_type)
        ]);

        for (const l of ['', 'en', 'tw', 'ja', 'ko']) await env.SHEEPS_CACHE.delete(`shop_items_${l}`);

        return new Response(JSON.stringify({ success: true, item_type: shopItem.item_type, new_count: (writeResults[5].results[0] as any)?.count || 0, remaining_points: remainingPoints }), { headers: corsHeaders });
    }

    return null;
}