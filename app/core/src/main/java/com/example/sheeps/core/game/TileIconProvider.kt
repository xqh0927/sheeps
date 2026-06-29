package com.example.sheeps.core.game

import android.content.Context
import com.example.sheeps.core.R

/**
 * 负责根据皮肤和类型提供对应的图片资源 ID
 */
object TileIconProvider {

    fun getIconResource(context: Context, skin: String, type: Int): Int {
        val normalizedSkin = skin.lowercase()
        
        // 1. 处理 34 个省份皮肤 (SVG)
        if (SkinConstants.provinceIds.contains(normalizedSkin)) {
            val resName = "tile_${normalizedSkin}_$type"
            val resId = context.resources.getIdentifier(resName, "drawable", context.packageName)
            if (resId != 0) return resId
        }

        // 2. 处理经典、水墨、赛博等传统皮肤
        val resName = if (normalizedSkin == "classic" || normalizedSkin == "default") {
            "tile_$type"
        } else {
            "tile_${normalizedSkin}_$type"
        }

        val resId = context.resources.getIdentifier(resName, "drawable", context.packageName)
        if (resId != 0) return resId

        // 3. 默认回退到经典 WebP 图标
        val fallbackName = "tile_$type"
        return context.resources.getIdentifier(fallbackName, "drawable", context.packageName).let {
            if (it != 0) it else R.drawable.tile_1
        }
    }
}
