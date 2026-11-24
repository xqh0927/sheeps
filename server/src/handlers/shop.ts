import { Env } from '../types';
import { getCorsHeaders, getAuthenticatedUser, cacheControl } from '../helpers';
import { getI18nBatch, resolveI18n } from '../i18n';

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
export async function handleShopRoutes(request: Request, env: Env, path: string, lang: string): Promise<Response | null> {
    const corsHeaders = getCorsHeaders();

    /**
     * GET /api/shop/items — 查询积分商城商品列表（支持多语言与KV缓存）
     *
     * Query 参数（通过请求头 Accept-Language 解析）:
     *   @param {string} [lang] — 语言标识（en / zh-CN / tw / ja / ko）
     *
     * 响应: [{ id, name, description, image_url, item_type, points_price, stock }]
     */
    if (path === '/api/shop/items' && request.method === 'GET') {
        const cacheHeaders = cacheControl(300);
        const cacheKey = `shop_items_${lang}`;
        // DB/缓存：优先读 KV（SHEEPS_CACHE）热点缓存，TTL 10 分钟，命中直接返回，降低 D1 读压力
        const cached = await env.SHEEPS_CACHE.get(cacheKey);
        if (cached) return new Response(cached, { headers: { ...corsHeaders, ...cacheHeaders } });

        // 方案 B：先取基列（zh 兜底），再用 i18n_strings 解析本地化文案
        // DB：查询 shop_items 商品表，过滤掉非兑换类（CLASSIC 及各类 SKIN_* 皮肤），仅返回可消耗积分兑换的法宝道具
        const items = await env.DB.prepare(
            `SELECT id, name, description, image_url, item_type, points_price, stock, "group"
             FROM shop_items
             WHERE item_type NOT IN ('CLASSIC', 'SKIN_INK', 'SKIN_CYBER', 'SKIN_KEAI', 'SKIN_DAIMENG')`
        ).all();

        // 为皮肤项批量装配 tiles[]（12 张卡面，按 tile_index 1..12 排序，缺失项为 null）
        const skinItems = items.results.filter(
            (i: any) => typeof i.item_type === 'string' && i.item_type.startsWith('SKIN_')
        );
        const tilesBySkin: Record<string, (string | null)[]> = {};
        if (skinItems.length > 0) {
            const skinTypes = skinItems.map((i: any) => i.item_type.replace('SKIN_', '').toLowerCase());
            const placeholders = skinTypes.map(() => '?').join(', ');
            const rows = await env.DB.prepare(
                `SELECT skin_type, tile_index, image_url FROM skin_tiles WHERE skin_type IN (${placeholders})`
            ).bind(...skinTypes).all();
            for (const r of rows.results as any[]) {
                const arr = (tilesBySkin[r.skin_type] = tilesBySkin[r.skin_type] || new Array(12).fill(null));
                if (r.tile_index >= 1 && r.tile_index <= 12) arr[r.tile_index - 1] = r.image_url;
            }
        }

        // 批量读取 shop_items 模块的本地化文案，命中优先、缺值回退基列
        const i18nMap = await getI18nBatch(env, 'shop_items', lang);
        const result = items.results.map((i: any) => {
            const name = resolveI18n(i18nMap, `shop_items.${i.id}.name`, i.name);
            const description = resolveI18n(i18nMap, `shop_items.${i.id}.description`, i.description);
            const base = {
                id: i.id,
                name,
                description,
                image_url: i.image_url,
                item_type: i.item_type,
                points_price: i.points_price,
                stock: i.stock,
                "group": i.group,
            };
            if (typeof i.item_type === 'string' && i.item_type.startsWith('SKIN_')) {
                const skinType = i.item_type.replace('SKIN_', '').toLowerCase();
                return { ...base, tiles: tilesBySkin[skinType] || [] };
            }
            return base;
        });

        const jsonStr = JSON.stringify(result);
        await env.SHEEPS_CACHE.put(cacheKey, jsonStr, { expirationTtl: 600 });
        return new Response(jsonStr, { headers: { ...corsHeaders, ...cacheHeaders } });
    }

    /**
     * POST /api/shop/exchange — 积分兑换道具（含库存扣减、背包写入、流水记录）
     *
     * 请求头:
     *   Authorization: Bearer <token>
     *
     * 请求体 (JSON):
     *   @param {number} shop_item_id — 商品ID
     *   @param {number} count — 兑换数量（≥1）
     *
     * 响应: { success, item_type, new_count, remaining_points }
     */
    if (path === '/api/shop/exchange' && request.method === 'POST') {
        const authUser = await getAuthenticatedUser(request, env);
        if (!authUser) return new Response(JSON.stringify({ error: 'Unauthorized' }), { status: 401, headers: corsHeaders });

        const body: { shop_item_id: number; count: number; operation_id?: string } = await request.json();
        if (!Number.isSafeInteger(body.shop_item_id) || !Number.isSafeInteger(body.count) || body.count < 1 ||
            (body.operation_id !== undefined && (typeof body.operation_id !== 'string' || !/^[a-zA-Z0-9-]{1,64}$/.test(body.operation_id)))) {
            return new Response(JSON.stringify({error: 'Invalid parameters'}), {status: 400, headers: corsHeaders});
        }

        const attempt = crypto.randomUUID();
        const operation = body.operation_id ?? attempt;
        const now = Date.now();
        // Only the transaction that inserts its unique attempt_id may apply this receipt.
        // Eligibility, balance and inventory are read inside the same D1 write transaction.
        const results = await env.DB.batch<Record<string, number | string>>([
            env.DB.prepare(`INSERT INTO shop_exchange_receipts
                (user_id, operation_id, attempt_id, shop_item_id, item_type, quantity, points_cost, remaining_points, new_count)
                SELECT u.id, ?1, ?2, s.id, s.item_type, ?3, s.points_price * ?3, u.points - s.points_price * ?3,
                       COALESCE(i.count, 0) + ?3
                FROM shop_items s JOIN users u ON u.id = ?4
                LEFT JOIN user_items i ON i.user_id = u.id AND i.item_type = s.item_type
                WHERE s.id = ?5 AND s.stock >= ?3 AND u.points >= s.points_price * ?3
                ON CONFLICT(user_id, operation_id) DO NOTHING`)
                .bind(operation, attempt, body.count, authUser.userId, body.shop_item_id),
            env.DB.prepare(`UPDATE users SET points = r.remaining_points FROM shop_exchange_receipts r
                WHERE r.attempt_id = ? AND users.id = r.user_id`).bind(attempt),
            env.DB.prepare(`UPDATE shop_items SET stock = stock - r.quantity FROM shop_exchange_receipts r
                WHERE r.attempt_id = ? AND shop_items.id = r.shop_item_id`).bind(attempt),
            env.DB.prepare(`INSERT INTO user_items (user_id, item_type, count)
                SELECT user_id, item_type, new_count FROM shop_exchange_receipts WHERE attempt_id = ?
                ON CONFLICT(user_id, item_type) DO UPDATE SET count = excluded.count`).bind(attempt),
            env.DB.prepare(`INSERT INTO exchange_record (user_id, shop_item_id, item_type, count, points_cost, created_at)
                SELECT user_id, shop_item_id, item_type, quantity, points_cost, ? FROM shop_exchange_receipts
                WHERE attempt_id = ?`).bind(now, attempt),
            env.DB.prepare(`INSERT INTO point_record (user_id, type, amount, source, remaining_points, created_at)
                SELECT user_id, 'OUT', points_cost, 'SHOP_REDEEM_' || item_type, remaining_points, ?
                FROM shop_exchange_receipts WHERE attempt_id = ?`).bind(now, attempt),
            env.DB.prepare(`SELECT shop_item_id, item_type, quantity, new_count, remaining_points FROM shop_exchange_receipts
                WHERE user_id = ? AND operation_id = ?`).bind(authUser.userId, operation),
            env.DB.prepare('SELECT stock FROM shop_items WHERE id = ?').bind(body.shop_item_id)
        ]);
        const receipt = results[6].results[0];
        if (!receipt) {
            const stock = Number(results[7].results[0]?.stock ?? 0);
            return new Response(JSON.stringify({error: stock < body.count ? 'Item out of stock' : 'Insufficient points'}),
                {status: 400, headers: corsHeaders});
        }
        if (receipt.shop_item_id !== body.shop_item_id || receipt.quantity !== body.count) {
            return new Response(JSON.stringify({error: 'Operation already used for a different order'}), {status: 409, headers: corsHeaders});
        }
        await Promise.all(['', 'en', 'tw', 'ja', 'ko'].map(l => env.SHEEPS_CACHE.delete(`shop_items_${l}`)))
            .catch(error => console.error('Failed to invalidate shop inventory cache', error));

        return new Response(JSON.stringify({success: true, item_type: receipt.item_type, new_count: receipt.new_count,
            remaining_points: receipt.remaining_points}), {headers: corsHeaders});
    }

    return null;
}