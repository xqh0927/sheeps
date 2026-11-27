package com.example.sheeps.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.sheeps.ui.R

@DrawableRes
internal fun bundledItemIcon(itemType: String): Int = when (itemType.uppercase()) {
    "UNDO" -> R.drawable.item_icon_undo
    "MOVEOUT" -> R.drawable.item_icon_moveout
    "SHUFFLE" -> R.drawable.item_icon_shuffle
    "REVIVE" -> R.drawable.item_icon_revive
    "HINT" -> R.drawable.item_icon_hint
    "BOMB" -> R.drawable.item_icon_bomb
    "JOKER" -> R.drawable.item_icon_joker
    "DOUBLE_POINTS" -> R.drawable.item_icon_double_points
    else -> R.drawable.ic_item_unknown
}

/** 后台图片可更新；首次加载、离线及缺图时使用对应道具的内置图标。 */
@Composable
fun ItemIcon(
    itemType: String,
    imageUrl: String?,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    isGray: Boolean = false
) {
    val localIcon = painterResource(bundledItemIcon(itemType))
    val grayModifier = if (isGray) modifier.graphicsLayer { alpha = 0.45f } else modifier
    AsyncImage(
        model = imageUrl?.takeIf { it.isNotBlank() },
        contentDescription = itemType,
        modifier = grayModifier.size(size),
        placeholder = localIcon,
        error = localIcon,
        fallback = localIcon,
        contentScale = ContentScale.Fit
    )
}
