package com.example.sheeps.core.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.sheeps.core.R

@Composable
fun getLocalizedItemName(itemType: String): String {
    return when (itemType.uppercase()) {
        "UNDO" -> stringResource(id = R.string.item_undo)
        "MOVEOUT" -> stringResource(id = R.string.item_moveout)
        "SHUFFLE" -> stringResource(id = R.string.item_shuffle)
        "REVIVE" -> stringResource(id = R.string.item_revive)
        "HINT" -> stringResource(id = R.string.item_hint)
        "BOMB" -> stringResource(id = R.string.item_bomb)
        "JOKER" -> stringResource(id = R.string.item_joker)
        "DOUBLE_POINTS" -> stringResource(id = R.string.item_double_points)
        "SKIN_INK" -> stringResource(id = R.string.item_skin_ink)
        "SKIN_CYBER" -> stringResource(id = R.string.item_skin_cyber)
        "SKIN_HENAN" -> stringResource(id = R.string.item_skin_henan)
        "SKIN_SICHUAN" -> stringResource(id = R.string.item_skin_sichuan)
        "CLASSIC" -> stringResource(id = R.string.item_skin_classic)
        else -> itemType
    }
}

@Composable
fun getLocalizedItemDesc(itemType: String, defaultDesc: String?): String {
    return when (itemType.uppercase()) {
        "UNDO" -> stringResource(id = R.string.item_undo_desc)
        "MOVEOUT" -> stringResource(id = R.string.item_moveout_desc)
        "SHUFFLE" -> stringResource(id = R.string.item_shuffle_desc)
        "REVIVE" -> stringResource(id = R.string.item_revive_desc)
        "HINT" -> stringResource(id = R.string.item_hint_desc)
        "BOMB" -> stringResource(id = R.string.item_bomb_desc)
        "JOKER" -> stringResource(id = R.string.item_joker_desc)
        "DOUBLE_POINTS" -> stringResource(id = R.string.item_double_points_desc)
        "SKIN_INK" -> stringResource(id = R.string.item_skin_ink_desc)
        "SKIN_CYBER" -> stringResource(id = R.string.item_skin_cyber_desc)
        "SKIN_HENAN" -> stringResource(id = R.string.item_skin_henan_desc)
        "SKIN_SICHUAN" -> stringResource(id = R.string.item_skin_sichuan_desc)
        "CLASSIC" -> stringResource(id = R.string.item_skin_classic_desc)
        else -> defaultDesc ?: stringResource(id = R.string.default_item_desc)
    }
}

@Composable
fun getLocalizedSource(source: String): String {
    if (source.startsWith("UNLOCK_LEVEL_")) {
        val levelId = source.substringAfter("UNLOCK_LEVEL_")
        return stringResource(id = R.string.points_source_unlock_level, levelId)
    }
    if (source.startsWith("SHOP_REDEEM_")) {
        val itemType = source.substringAfter("SHOP_REDEEM_")
        return stringResource(id = R.string.points_source_shop_redeem, getLocalizedItemName(itemType))
    }
    if (source.startsWith("DAILY_TASK_")) {
        val taskNameKey = source.substringAfter("DAILY_TASK_")
        return stringResource(id = R.string.points_source_daily_task, getLocalizedTaskName(taskNameKey))
    }
    return when (source.uppercase()) {
        "FIRST_CLEAR" -> stringResource(id = R.string.points_source_first_clear)
        "SIGN_IN" -> stringResource(id = R.string.points_source_sign_in)
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
