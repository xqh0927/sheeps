-- ============================================================
-- 清理已废弃的卡牌皮肤商品（SKIN_INK / SKIN_CYBER 等）
-- 用法：在项目根目录执行：
--   npx wrangler d1 execute my-app-db --remote --file=server/cleanup_deprecated_skins.sql
--
-- 或直接在 Cloudflare Dashboard → D1 → my-app-db 中粘贴执行
-- ============================================================

-- 1. 查看即将删除的记录（先确认再删）
SELECT id, item_type, name, points_price, stock
FROM shop_items
WHERE item_type IN ('CLASSIC', 'SKIN_INK', 'SKIN_CYBER', 'SKIN_KEAI', 'SKIN_DAIMENG');

-- 2. 删除废弃皮肤的商品记录
DELETE FROM shop_items
WHERE item_type IN ('CLASSIC', 'SKIN_INK', 'SKIN_CYBER', 'SKIN_KEAI', 'SKIN_DAIMENG');

-- 3. （可选）清理用户背包中可能存在的废弃皮肤
DELETE FROM user_items
WHERE item_type IN ('CLASSIC', 'SKIN_INK', 'SKIN_CYBER', 'SKIN_KEAI', 'SKIN_DAIMENG');
