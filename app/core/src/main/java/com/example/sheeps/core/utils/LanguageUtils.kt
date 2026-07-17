package com.example.sheeps.core.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.sheeps.ui.R
import com.example.sheeps.data.model.ShopItem

/**
 * 商品展示名称 / 描述的本地化辅助。
 *
 * **多语言策略（后台接口驱动）**：
 * - 优先使用 [itemI18n] 参数传入的后台 /api/shop/items 多语言数据。
 * - 无后台数据时回退本地 com.example.sheeps.ui.R.string 硬编码（仅在游戏中或离线场景触发）。
 *
 * 调用方：
 * - 背包 / 历史等有 [ShopItem] 缓存的场景，应传入 itemI18n 以获得后台统一多语言。
 * - 游戏内对局（准备对话框 / 道具栏 / 带出选择等）传 null，走本地 com.example.sheeps.ui.R.string 兜底。
 */

/**
 * 获取商品展示名称（本地化）。
 * 优先使用后台多语言 [itemI18n]，回退本地 com.example.sheeps.ui.R.string 资源。
 * @param itemType 商品类型标识（如 "UNDO"）
 * @param itemI18n 后台接口多语言 Map（key 为大写类型），可空
 * @return 本地化后的商品名称
 * ⚠️ 线程约束：@MainThread（@Composable，在组合阶段调用，内部使用 stringResource）
 */
@Composable
fun getLocalizedItemName(itemType: String, itemI18n: Map<String, String>? = null): String {
    // 优先：后台接口多语言
    itemI18n?.get(itemType.uppercase())?.let { return it }
    // 回退：本地 com.example.sheeps.ui.R.string
    return when (itemType.uppercase()) {
        "UNDO" -> stringResource(id = com.example.sheeps.ui.R.string.item_undo)
        "MOVEOUT" -> stringResource(id = com.example.sheeps.ui.R.string.item_moveout)
        "SHUFFLE" -> stringResource(id = com.example.sheeps.ui.R.string.item_shuffle)
        "REVIVE" -> stringResource(id = com.example.sheeps.ui.R.string.item_revive)
        "HINT" -> stringResource(id = com.example.sheeps.ui.R.string.item_hint)
        "BOMB" -> stringResource(id = com.example.sheeps.ui.R.string.item_bomb)
        "JOKER" -> stringResource(id = com.example.sheeps.ui.R.string.item_joker)
        "DOUBLE_POINTS" -> stringResource(id = com.example.sheeps.ui.R.string.item_double_points)
        "SKIN_SHUANG" -> stringResource(id = com.example.sheeps.ui.R.string.item_skin_shuang)
        else -> itemType
    }
}

/**
 * 获取商品展示描述（本地化）。
 * 优先使用后台多语言 [itemI18n]，回退本地 com.example.sheeps.ui.R.string 资源；[defaultDesc] 作为最终兜底。
 * @param itemType 商品类型标识
 * @param defaultDesc 后台未提供描述时的兜底文本，可空
 * @param itemI18n 后台接口多语言 Map，可空
 * ⚠️ 线程约束：@MainThread（@Composable）
 */
@Composable
fun getLocalizedItemDesc(
    itemType: String,
    defaultDesc: String?,
    itemI18n: Map<String, String>? = null
): String {
    // 优先：后台接口多语言
    itemI18n?.get(itemType.uppercase())?.let { return it }
    // 回退：本地 com.example.sheeps.ui.R.string
    return when (itemType.uppercase()) {
        "UNDO" -> stringResource(id = com.example.sheeps.ui.R.string.item_undo_desc)
        "MOVEOUT" -> stringResource(id = com.example.sheeps.ui.R.string.item_moveout_desc)
        "SHUFFLE" -> stringResource(id = com.example.sheeps.ui.R.string.item_shuffle_desc)
        "REVIVE" -> stringResource(id = com.example.sheeps.ui.R.string.item_revive_desc)
        "HINT" -> stringResource(id = com.example.sheeps.ui.R.string.item_hint_desc)
        "BOMB" -> stringResource(id = com.example.sheeps.ui.R.string.item_bomb_desc)
        "JOKER" -> stringResource(id = com.example.sheeps.ui.R.string.item_joker_desc)
        "DOUBLE_POINTS" -> stringResource(id = com.example.sheeps.ui.R.string.item_double_points_desc)
        "SKIN_SHUANG" -> stringResource(id = com.example.sheeps.ui.R.string.item_skin_shuang_desc)
        else -> defaultDesc ?: stringResource(id = com.example.sheeps.ui.R.string.default_item_desc)
    }
}

/**
 * 商店商品显示名：优先使用服务端已本地化的 [ShopItem.name]。
 * 仅当默认皮肤 shuang 且服务端名为空时，回退本地 com.example.sheeps.ui.R.string.item_skin_shuang，保证离线必显。
 */
@Composable
fun getShopItemName(item: ShopItem): String {
    if (item.item_type.equals("SKIN_SHUANG", ignoreCase = true)) {
        val server = item.name
        if (!server.isNullOrBlank()) return server
        return stringResource(id = com.example.sheeps.ui.R.string.item_skin_shuang)
    }
    return item.name ?: item.item_type
}

/**
 * 商店商品显示描述：优先使用服务端已本地化的 [ShopItem.description]。
 * 仅当默认皮肤 shuang 且服务端描述为空时，回退本地 com.example.sheeps.ui.R.string.item_skin_shuang_desc。
 */
@Composable
fun getShopItemDesc(item: ShopItem): String {
    if (item.item_type.equals("SKIN_SHUANG", ignoreCase = true)) {
        val server = item.description
        if (!server.isNullOrBlank()) return server
        return stringResource(id = com.example.sheeps.ui.R.string.item_skin_shuang_desc)
    }
    return item.description ?: ""
}

/**
 * 积分来源描述本地化。按前缀（UNLOCK_LEVEL_/SHOP_REDEEM_/DAILY_TASK_）或固定枚举映射为可读文本。
 * @param source 积分来源原始标识
 * @param itemI18n 后台接口多语言 Map（用于 SHOP_REDEEM_ 场景的商品名），可空
 * ⚠️ 线程约束：@MainThread（@Composable）
 */
@Composable
fun getLocalizedSource(source: String, itemI18n: Map<String, String>? = null): String {
    if (source.startsWith("UNLOCK_LEVEL_")) {
        val levelId = source.substringAfter("UNLOCK_LEVEL_")
        return stringResource(id = com.example.sheeps.ui.R.string.points_source_unlock_level, levelId)
    }
    if (source.startsWith("SHOP_REDEEM_")) {
        val itemType = source.substringAfter("SHOP_REDEEM_")
        return stringResource(
            id = com.example.sheeps.ui.R.string.points_source_shop_redeem,
            getLocalizedItemName(itemType, itemI18n)
        )
    }
    if (source.startsWith("DAILY_TASK_")) {
        val taskNameKey = source.substringAfter("DAILY_TASK_")
        return stringResource(
            id = com.example.sheeps.ui.R.string.points_source_daily_task,
            getLocalizedTaskName(taskNameKey)
        )
    }
    return when (source.uppercase()) {
        "FIRST_CLEAR" -> stringResource(id = com.example.sheeps.ui.R.string.points_source_first_clear)
        "SIGN_IN" -> stringResource(id = com.example.sheeps.ui.R.string.points_source_sign_in)
        "SIGN_IN_ONCE" -> stringResource(id = com.example.sheeps.ui.R.string.points_source_sign_in_once)
        "SET_PASSWORD" -> stringResource(id = com.example.sheeps.ui.R.string.points_source_set_password)
        else -> source
    }
}

/**
 * 每日任务名本地化。将后台任务标识映射为本地 com.example.sheeps.ui.R.string 文本。
 * @param taskName 任务标识（如 "PLAY_3_GAMES"）
 * ⚠️ 线程约束：@MainThread（@Composable）
 */
@Composable
fun getLocalizedTaskName(taskName: String): String {
    return when (taskName.uppercase()) {
        "PLAY_3_GAMES", "PLAY_3", "WIN_3_GAMES", "WIN_3" -> stringResource(id = com.example.sheeps.ui.R.string.task_win_3)
        "PLAY_5_GAMES", "PLAY_5", "WIN_5_GAMES", "WIN_5" -> stringResource(id = com.example.sheeps.ui.R.string.task_win_5)
        "SIGN_IN" -> stringResource(id = com.example.sheeps.ui.R.string.task_sign_in)
        else -> taskName
    }
}
