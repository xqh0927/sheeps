/**
 * 商品类型分区枚举。
 *
 * 对应真实 DB / Android 端的分区约定：
 * - 皮肤（SKIN_*）：7 个皮肤系列
 * - 道具（非 SKIN_*）：8 个道具类型
 *
 * 每个元素均为 { label, value } 形式，可直接作为 CrudPage 的 select options。
 * label 为管理后台展示用中文，value 为 DB/Android 端约定的 item_type 原始码。
 */

/** 皮肤分区类型（7 项）：label 为后台展示中文，value 为 DB/Android 约定的 item_type 原始码。 */
export const SKIN_ITEM_TYPES = [
  { label: '双倍', value: 'SKIN_SHUANG' },
  { label: '河南', value: 'SKIN_HENAN' },
  { label: '四川', value: 'SKIN_SICHUAN' },
  { label: '数码', value: 'SKIN_ELECTRONIC' },
  { label: '生活', value: 'SKIN_DAILY' },
  { label: '蔬菜', value: 'SKIN_VEGETABLE' },
  { label: '水果', value: 'SKIN_FRUIT' },
];

/** 道具分区类型（8 项）：非 SKIN_* 的常规道具，结构与 SKIN_ITEM_TYPES 对齐。 */
export const PROP_ITEM_TYPES = [
  { label: '撤销', value: 'UNDO' },
  { label: '移出', value: 'MOVEOUT' },
  { label: '洗牌', value: 'SHUFFLE' },
  { label: '复活', value: 'REVIVE' },
  { label: '提示', value: 'HINT' },
  { label: '炸弹', value: 'BOMB' },
  { label: '王牌', value: 'JOKER' },
  { label: '双倍积分', value: 'DOUBLE_POINTS' },
];
