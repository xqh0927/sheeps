package com.example.sheeps.core.game

import android.content.Context
import com.example.sheeps.core.R

/**
 * 负责根据皮肤和类型提供对应的图片资源 ID
 *
 * 回退策略：
 * 1. 尝试皮肤专属资源（如 tile_classic_1、tile_province_beijing_1）
 * 2. 如果当前不是经典皮肤，回退到经典皮肤资源（tile_1、tile_2...）
 * 3. 如果仍然找不到，返回 0（UI 层应显示类型编号或占位符）
 */
object TileIconProvider {

    fun getIconResource(context: Context, skin: String, type: Int): Int {
        val normalizedSkin = skin.lowercase()

        // 1. 尝试皮肤专属资源
        val resName = buildResName(normalizedSkin, type)
        var resId = context.resources.getIdentifier(resName, "drawable", context.packageName)
        if (resId != 0) return resId

        // 2. 回退到经典皮肤（如果当前不是经典皮肤）
        if (normalizedSkin != "classic" && normalizedSkin != "default") {
            val classicResName = "tile_$type"
            resId = context.resources.getIdentifier(classicResName, "drawable", context.packageName)
            if (resId != 0) return resId
        }

        // 3. 所有回退均失败，返回 0
        // UI 层应处理 resId=0 的情况（显示类型编号或占位符）
        // 不再使用 R.drawable.tile_1 作为最终兜底，
        // 否则所有缺失的类型都会显示为类型 1 的图标
        return 0
    }

    private fun buildResName(skin: String, type: Int): String {
        return if (SkinConstants.provinceIds.contains(skin)) {
            "tile_${skin}_$type"
        } else if (skin == "classic" || skin == "default") {
            "tile_$type"
        } else {
            "tile_${skin}_$type"
        }
    }
}
