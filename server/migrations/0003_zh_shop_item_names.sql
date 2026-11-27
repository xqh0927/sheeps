-- Existing shop IDs are deployment-specific. Match the product type and the complete legacy value.
WITH standard(item_type, old_name, new_name) AS (
    VALUES
        ('UNDO', '乾坤符 (Undo)', '乾坤符'),
        ('MOVEOUT', '缩地咒 (MoveOut)', '缩地咒'),
        ('SHUFFLE', '流沙契 (Shuffle)', '流沙契'),
        ('REVIVE', '还魂丹 (Revive)', '还魂丹'),
        ('HINT', '天眼符 (Hint)', '天眼符'),
        ('BOMB', '雷震子 (Bomb)', '雷震子'),
        ('JOKER', '太极牌 (Joker)', '太极牌'),
        ('DOUBLE_POINTS', '双倍符 (Double)', '双倍符')
)
UPDATE i18n_strings
SET value = standard.new_name, updated_at = unixepoch() * 1000
FROM shop_items AS item JOIN standard ON standard.item_type = item.item_type
WHERE i18n_strings.module = 'shop_items' AND i18n_strings.locale = 'zh'
  AND i18n_strings.str_key = 'shop_items.' || item.id || '.name'
  AND i18n_strings.value = standard.old_name;

WITH standard(item_type, old_name, new_name) AS (
    VALUES
        ('UNDO', '乾坤符 (Undo)', '乾坤符'),
        ('MOVEOUT', '缩地咒 (MoveOut)', '缩地咒'),
        ('SHUFFLE', '流沙契 (Shuffle)', '流沙契'),
        ('REVIVE', '还魂丹 (Revive)', '还魂丹'),
        ('HINT', '天眼符 (Hint)', '天眼符'),
        ('BOMB', '雷震子 (Bomb)', '雷震子'),
        ('JOKER', '太极牌 (Joker)', '太极牌'),
        ('DOUBLE_POINTS', '双倍符 (Double)', '双倍符')
)
UPDATE shop_items
SET name = standard.new_name
FROM standard
WHERE shop_items.item_type = standard.item_type AND shop_items.name = standard.old_name;
