package com.example.sheeps.core.game

import com.example.sheeps.core.R

/**
 * 皮肤系统常量配置
 */
object SkinConstants {

    /**
     * 每套皮肤支持的最大样式种类（与 drawable 资源数量对齐）
     */
    const val MAX_TILE_TYPES = 12

    /**
     * 默认卡牌皮肤键（渲染键，与商铺 item_type 的 "SKIN_" 前缀去除后小写形式一致）。
     * 修改全局默认皮肤只需改这一处。
     *
     * 可用取值（必须与 app/core/src/main/res/drawable 下 tile_<key>_* 资源前缀对应）：
     *  - 灵动动画系列(特效): "shuang" / "electronic" / "daily" / "vegetable" / "fruit"
     *  - 省份系列:           "henan" / "sichuan"
     * 说明：已配专属卡面（存在 tile_<key>_1~12.webp）的渲染键即以上几种；
     *       其余键若未提供对应 drawable 资源，将回退到默认卡面。
     */
    const val DEFAULT_SKIN = "shuang"

    data class ProvinceSkin(val id: String, val name: String)

    val provinces = listOf(
        ProvinceSkin("henan", "河南"),
        ProvinceSkin("sichuan", "四川")
    )

    val provinceIds = provinces.map { it.id }

    data class SpecialSkin(val id: String, val nameRes: Int, val descRes: Int, val price: Int)

    /** 特效皮肤系列：数码 / 日常 / 蔬菜 / 水果，每组 12 张卡面 */
    val specialSkins = listOf(
        SpecialSkin("electronic", R.string.item_skin_electronic, R.string.item_skin_electronic_desc, 300),
        SpecialSkin("daily", R.string.item_skin_daily, R.string.item_skin_daily_desc, 300),
        SpecialSkin("vegetable", R.string.item_skin_vegetable, R.string.item_skin_vegetable_desc, 300),
        SpecialSkin("fruit", R.string.item_skin_fruit, R.string.item_skin_fruit_desc, 300)
    )
    val specialSkinItemTypes = specialSkins.map { "SKIN_${it.id.uppercase()}" }
}
