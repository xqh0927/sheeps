package com.example.sheeps.core.game

import android.content.Context
import com.example.sheeps.ui.R
import com.example.sheeps.data.game.SkinConstants
import com.example.sheeps.data.model.ShopItem

/**
 * 卡牌皮肤 / 道具图标 资源注册表。
 *
 * v2 改为「URL 注册表 + 本地兜底」模式：
 *  - [setShopItems] 从商城列表注入数据，建立：
 *      · 皮肤渲染键(skinKey) → 12 张远程卡面 URL（来自 skin_tiles）
 *      · 道具 item_type → 远程图标 URL（来自 item_icons，镜像于 shop_items.image_url）
 *      · 皮肤按 group 分组数据
 *  - 渲染时优先返回远程 URL，由 Coil 加载；URL 缺失或加载失败，回退到默认皮肤
 *    [SkinConstants.DEFAULT_SKIN]（shuang）的本地 drawable `tile_shuang_{type}`。
 *
 * 单一数据源：所有图片 URL 来自后端，Android 不再写死非兜底皮肤的本地 drawable 映射。
 * 向后兼容：注册表为空（旧版 / 离线）时，所有皮肤回退默认皮肤本地图。
 */
object TileIconProvider {

    /** skinKey(小写渲染键) -> 12 张卡面 URL（index 0..11 对应 tile 1..12，缺失为 null） */
    private val skinTileUrls = mutableMapOf<String, List<String?>>()

    /** item_type(大写) -> 道具图标 URL */
    private val itemIconUrls = mutableMapOf<String, String?>()

    /** 分组数据：group(可空) -> 该组皮肤商品（按原始顺序） */
    private val groups = mutableMapOf<String?, MutableList<ShopItem>>()

    /** 最近一次注入的全部商品 */
    private var allItems: List<ShopItem> = emptyList()

    /**
     * 注入商城商品列表，重建注册表与分组数据。
     * 应在每次拉取/刷新 shop 列表后调用。
     */
    fun setShopItems(items: List<ShopItem>) {
        allItems = items
        skinTileUrls.clear()
        itemIconUrls.clear()
        groups.clear()
        for (item in items) {
            if (item.item_type.startsWith("SKIN_")) {
                val key = item.item_type.removePrefix("SKIN_").lowercase()
                skinTileUrls[key] = item.tiles ?: emptyList()
                groups.getOrPut(item.group) { mutableListOf() }.add(item)
            } else {
                itemIconUrls[item.item_type.uppercase()] = item.image_url
            }
        }
    }

    /**
     * 获取某皮肤某类型卡面的远程 URL（1-based tileIndex）。
     * @return URL 字符串；无远程数据返回 null（调用方应回退本地默认皮肤）。
     */
    fun getTileUrl(skinKey: String, tileIndex: Int): String? {
        val list = skinTileUrls[skinKey.lowercase()] ?: return null
        if (tileIndex !in 1..12) return null
        val idx = tileIndex - 1
        return if (idx < list.size) list[idx] else null
    }

    /**
     * 批量获取某皮肤全部 12 张卡面的远程 URL（用于切换皮肤后预加载，解决进对局首张加载慢）。
     * 内部按 1..12 顺序取 [getTileUrl]，过滤掉取不到的（null）。
     * @param skinKey 皮肤渲染键（任意大小写，内部统一小写）
     * @return 该皮肤全部有效卡面 URL 列表
     */
    fun getSkinTileUrls(skinKey: String): List<String> = (1..12).mapNotNull { getTileUrl(skinKey, it) }

    /**
     * 获取某道具图标的远程 URL。
     * @return URL 字符串；无远程数据返回 null（调用方应回退静态占位 drawable）。
     */
    fun getItemIconUrl(itemType: String): String? = itemIconUrls[itemType.uppercase()]

    /** 当前分组数据（key 为 group 值，可为 null 表示未分组） */
    fun getGroups(): Map<String?, List<ShopItem>> = groups

    /** 最近一次注入的全部商品 */
    fun getAllItems(): List<ShopItem> = allItems

    /**
     * 默认皮肤（[SkinConstants.DEFAULT_SKIN]，即 shuang）本地兜底图的静态映射表。
     *
     * 由 `app/core/src/main/res/drawable/tile_shuang_1..12.webp` 这些真实资源在编译期直接引用生成，
     * 彻底替代原先运行时 `Resources.getIdentifier(...)` 反射查找（反射有性能损耗且在新 Android 上易被
     * 资源混淆/裁剪影响）。[getFallbackResId] 仅做一次 Map 查表，O(1) 且无反射。
     *
     * 键为 tileIndex（1..12，与 [SkinConstants.MAX_TILE_TYPES] 对齐），值为对应的
     * `R.drawable.tile_shuang_{index}` 资源 ID。
     */
    private val fallbackMap: Map<Int, Int> = mapOf(
        1 to R.drawable.tile_shuang_1,
        2 to R.drawable.tile_shuang_2,
        3 to R.drawable.tile_shuang_3,
        4 to R.drawable.tile_shuang_4,
        5 to R.drawable.tile_shuang_5,
        6 to R.drawable.tile_shuang_6,
        7 to R.drawable.tile_shuang_7,
        8 to R.drawable.tile_shuang_8,
        9 to R.drawable.tile_shuang_9,
        10 to R.drawable.tile_shuang_10,
        11 to R.drawable.tile_shuang_11,
        12 to R.drawable.tile_shuang_12
    )

    /**
     * 回退到默认皮肤（[SkinConstants.DEFAULT_SKIN]，即 shuang）的本地 drawable。
     * 当远程 URL 缺失或加载失败时作为兜底，保证离线/弱网可用。
     *
     * 通过静态 [fallbackMap] 直接查表返回资源 ID，移除运行时反射。
     * 若 [tileIndex] 超出 [fallbackMap]（理论上不会，类型恒为 1..12），则回退到默认兜底图
     * [R.drawable.tile_shuang_1]，保证始终返回一个有效 drawable 而非 0（避免调用方
     * `painterResource(0)` 崩溃）。
     */
    fun getFallbackResId(context: Context, tileIndex: Int): Int {
        return fallbackMap[tileIndex] ?: R.drawable.tile_shuang_1
    }
}
