package com.example.sheeps.core.game

/**
 * 皮肤系统常量配置
 */
object SkinConstants {

    data class ProvinceSkin(val id: String, val name: String)

    val provinces = listOf(
        ProvinceSkin("henan", "河南"),
        ProvinceSkin("sichuan", "四川")
    )

    val provinceIds = provinces.map { it.id }
}
