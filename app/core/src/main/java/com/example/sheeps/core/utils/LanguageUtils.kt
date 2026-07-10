package com.example.sheeps.core.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.sheeps.core.R
import com.example.sheeps.data.model.ShopItem

/**
 * 商品展示名称 / 描述的本地化辅助。
 *
 * **多语言策略（后台接口驱动）**：
 * - 优先使用 [itemI18n] 参数传入的后台 /api/shop/items 多语言数据。
 * - 无后台数据时回退本地 R.string 硬编码（仅在游戏中或离线场景触发）。
 *
 * 调用方：
 * - 背包 / 历史等有 [ShopItem] 缓存的场景，应传入 itemI18n 以获得后台统一多语言。
 * - 游戏内对局（准备对话框 / 道具栏 / 带出选择等）传 null，走本地 R.string 兜底。
 */

@Composable
fun getLocalizedItemName(itemType: String, itemI18n: Map<String, String>? = null): String {
    // 优先：后台接口多语言
    itemI18n?.get(itemType.uppercase())?.let { return it }
    // 回退：本地 R.string
    return when (itemType.uppercase()) {
        "UNDO" -> stringResource(id = R.string.item_undo)
        "MOVEOUT" -> stringResource(id = R.string.item_moveout)
        "SHUFFLE" -> stringResource(id = R.string.item_shuffle)
        "REVIVE" -> stringResource(id = R.string.item_revive)
        "HINT" -> stringResource(id = R.string.item_hint)
        "BOMB" -> stringResource(id = R.string.item_bomb)
        "FREEZE" -> stringResource(id = R.string.item_freeze)
        "JOKER" -> stringResource(id = R.string.item_joker)
        "DOUBLE_POINTS" -> stringResource(id = R.string.item_double_points)
        "SKIN_SHUANG" -> stringResource(id = R.string.item_skin_shuang)
        else -> itemType
    }
}

@Composable
fun getLocalizedItemDesc(itemType: String, defaultDesc: String?, itemI18n: Map<String, String>? = null): String {
    // 优先：后台接口多语言
    itemI18n?.get(itemType.uppercase())?.let { return it }
    // 回退：本地 R.string
    return when (itemType.uppercase()) {
        "UNDO" -> stringResource(id = R.string.item_undo_desc)
        "MOVEOUT" -> stringResource(id = R.string.item_moveout_desc)
        "SHUFFLE" -> stringResource(id = R.string.item_shuffle_desc)
        "REVIVE" -> stringResource(id = R.string.item_revive_desc)
        "HINT" -> stringResource(id = R.string.item_hint_desc)
        "BOMB" -> stringResource(id = R.string.item_bomb_desc)
        "FREEZE" -> stringResource(id = R.string.item_freeze_desc)
        "JOKER" -> stringResource(id = R.string.item_joker_desc)
        "DOUBLE_POINTS" -> stringResource(id = R.string.item_double_points_desc)
        "SKIN_SHUANG" -> stringResource(id = R.string.item_skin_shuang_desc)
        else -> defaultDesc ?: stringResource(id = R.string.default_item_desc)
    }
}

/**
 * 商店商品显示名：优先使用服务端已本地化的 [ShopItem.name]。
 * 仅当默认皮肤 shuang 且服务端名为空时，回退本地 R.string.item_skin_shuang，保证离线必显。
 */
@Composable
fun getShopItemName(item: ShopItem): String {
    if (item.item_type.equals("SKIN_SHUANG", ignoreCase = true)) {
        val server = item.name
        if (!server.isNullOrBlank()) return server
        return stringResource(id = R.string.item_skin_shuang)
    }
    return item.name ?: item.item_type
}

/**
 * 商店商品显示描述：优先使用服务端已本地化的 [ShopItem.description]。
 * 仅当默认皮肤 shuang 且服务端描述为空时，回退本地 R.string.item_skin_shuang_desc。
 */
@Composable
fun getShopItemDesc(item: ShopItem): String {
    if (item.item_type.equals("SKIN_SHUANG", ignoreCase = true)) {
        val server = item.description
        if (!server.isNullOrBlank()) return server
        return stringResource(id = R.string.item_skin_shuang_desc)
    }
    return item.description ?: ""
}

@Composable
fun getLocalizedSource(source: String, itemI18n: Map<String, String>? = null): String {
    if (source.startsWith("UNLOCK_LEVEL_")) {
        val levelId = source.substringAfter("UNLOCK_LEVEL_")
        return stringResource(id = R.string.points_source_unlock_level, levelId)
    }
    if (source.startsWith("SHOP_REDEEM_")) {
        val itemType = source.substringAfter("SHOP_REDEEM_")
        return stringResource(
            id = R.string.points_source_shop_redeem,
            getLocalizedItemName(itemType, itemI18n)
        )
    }
    if (source.startsWith("DAILY_TASK_")) {
        val taskNameKey = source.substringAfter("DAILY_TASK_")
        return stringResource(
            id = R.string.points_source_daily_task,
            getLocalizedTaskName(taskNameKey)
        )
    }
    return when (source.uppercase()) {
        "FIRST_CLEAR" -> stringResource(id = R.string.points_source_first_clear)
        "SIGN_IN" -> stringResource(id = R.string.points_source_sign_in)
        "SIGN_IN_ONCE" -> stringResource(id = R.string.points_source_sign_in_once)
        "SET_PASSWORD" -> stringResource(id = R.string.points_source_set_password)
        else -> source
    }
}

@Composable
fun getLocalizedTaskName(taskName: String): String {
    return when (taskName.uppercase()) {
        "PLAY_3_GAMES", "PLAY_3" -> stringResource(id = R.string.task_play_3)
        "PLAY_5_GAMES", "PLAY_5" -> stringResource(id = R.string.task_play_5)
        "SIGN_IN" -> stringResource(id = R.string.task_sign_in)
        else -> taskName
    }
}
