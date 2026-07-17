package com.example.sheeps.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.sheeps.ui.R

/**
 * 道具图标封装（v2 决策 #2：移除 Canvas 程序化动画）。
 *
 * - 优先用 Coil 加载 [imageUrl]（来自 item_icons 真身 / shop_items.image_url）；
 * - [imageUrl] 缺失或加载失败：回退到静态占位 mipmap [R.mipmap.ic_launcher]（非动画）。
 *
 * @param itemType  道具类型（如 UNDO），用于 contentDescription 与默认占位
 * @param imageUrl  远程图标 URL（可空）
 * @param size      渲染尺寸
 * @param isGray    是否置灰（不可用时）
 */
@Composable
fun ItemIcon(
    itemType: String,
    imageUrl: String?,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    isGray: Boolean = false
) {
    val context = LocalContext.current
    val grayModifier = if (isGray) modifier.graphicsLayer { alpha = 0.45f } else modifier
    val request = ImageRequest.Builder(context)
        .data(if (imageUrl.isNullOrBlank()) R.mipmap.ic_launcher else imageUrl)
        .placeholder(R.mipmap.ic_launcher)
        .error(R.mipmap.ic_launcher)
        .crossfade(true)
        .build()
    AsyncImage(
        model = request,
        contentDescription = itemType,
        modifier = grayModifier.size(size),
        contentScale = androidx.compose.ui.layout.ContentScale.Fit
    )
}
