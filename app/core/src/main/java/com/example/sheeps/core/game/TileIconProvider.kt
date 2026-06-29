package com.example.sheeps.core.game

import android.content.Context
import com.example.sheeps.core.R

/**
 * 负责根据皮肤和类型提供对应的图片资源 ID
 */
object TileIconProvider {

    private val provinceList = listOf(
        "beijing", "tianjin", "hebei", "shanxi", "inner_mongolia",
        "liaoning", "jilin", "heilongjiang", "shanghai", "jiangsu",
        "zhejiang", "anhui", "fujian", "jiangxi", "shandong",
        "henan", "hubei", "hunan", "guangdong", "guangxi",
        "hainan", "chongqing", "sichuan", "guizhou", "yunnan",
        "tibet", "shaanxi", "gansu", "qinghai", "ningxia",
        "xinjiang", "hongkong", "macau", "taiwan"
    )

    fun getIconResource(context: Context, skin: String, type: Int): Int {
        val normalizedSkin = skin.lowercase()
        
        // 处理经典皮肤
        if (normalizedSkin == "classic" || normalizedSkin == "default") {
            val resName = "tile_$type"
            return context.resources.getIdentifier(resName, "drawable", context.packageName).let {
                if (it != 0) it else R.drawable.tile_1
            }
        }

        // 处理 34 个省份皮肤
        if (provinceList.contains(normalizedSkin)) {
            val resName = "tile_${normalizedSkin}_$type"
            val resId = context.resources.getIdentifier(resName, "drawable", context.packageName)
            if (resId != 0) return resId
        }

        // 默认回退到经典皮肤
        val fallbackName = "tile_$type"
        return context.resources.getIdentifier(fallbackName, "drawable", context.packageName).let {
            if (it != 0) it else R.drawable.tile_1
        }
    }
}
