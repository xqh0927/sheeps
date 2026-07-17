package com.example.sheeps.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest

/**
 * 远程图片封装（Coil AsyncImage + 本地兜底 painter）。
 *
 * - 有 [url]：用 Coil 异步加载，加载中 / 加载失败均回退到 [fallbackResId]（默认皮肤本地图）。
 * - [url] 为空：直接显示 [fallbackResId]。
 *
 * 用于卡面 / 封面渲染，配合 [com.example.sheeps.core.game.TileIconProvider]
 * 的「URL 注册表 + 本地兜底」策略（决策 #3：shuang 为唯一本地兜底）。
 *
 * @param url        远程图片 URL（可空）
 * @param fallbackResId 加载失败 / 无 URL 时的本地 drawable 资源 ID
 * @param size       可选固定尺寸；不传则按父容器约束
 */
@Composable
fun RemoteImage(
    url: String?,
    fallbackResId: Int,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    size: Dp? = null
) {
    val resolved = if (size != null) modifier.size(size) else modifier
    if (url.isNullOrBlank()) {
        androidx.compose.foundation.Image(
            painter = painterResource(id = fallbackResId),
            contentDescription = contentDescription,
            modifier = resolved,
            contentScale = ContentScale.Fit
        )
    } else {
        val context = LocalContext.current
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(url)
                .crossfade(true)
                .diskCachePolicy(CachePolicy.ENABLED)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .networkCachePolicy(CachePolicy.ENABLED)
                .build(),
            contentDescription = contentDescription,
            modifier = resolved,
            contentScale = ContentScale.Fit,
            placeholder = painterResource(id = fallbackResId),
            error = painterResource(id = fallbackResId)
        )
    }
}
