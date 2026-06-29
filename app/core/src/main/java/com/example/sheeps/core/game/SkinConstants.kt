package com.example.sheeps.core.game

/**
 * 皮肤系统常量配置
 */
object SkinConstants {

    data class ProvinceSkin(val id: String, val name: String)

    val provinces = listOf(
        ProvinceSkin("beijing", "北京"),
        ProvinceSkin("tianjin", "天津"),
        ProvinceSkin("hebei", "河北"),
        ProvinceSkin("shanxi", "山西"),
        ProvinceSkin("inner_mongolia", "内蒙古"),
        ProvinceSkin("liaoning", "辽宁"),
        ProvinceSkin("jilin", "吉林"),
        ProvinceSkin("heilongjiang", "黑龙江"),
        ProvinceSkin("shanghai", "上海"),
        ProvinceSkin("jiangsu", "江苏"),
        ProvinceSkin("zhejiang", "浙江"),
        ProvinceSkin("anhui", "安徽"),
        ProvinceSkin("fujian", "福建"),
        ProvinceSkin("jiangxi", "江西"),
        ProvinceSkin("shandong", "山东"),
        ProvinceSkin("henan", "河南"),
        ProvinceSkin("hubei", "湖北"),
        ProvinceSkin("hunan", "湖南"),
        ProvinceSkin("guangdong", "广东"),
        ProvinceSkin("guangxi", "广西"),
        ProvinceSkin("hainan", "海南"),
        ProvinceSkin("chongqing", "重庆"),
        ProvinceSkin("sichuan", "四川"),
        ProvinceSkin("guizhou", "贵州"),
        ProvinceSkin("yunnan", "云南"),
        ProvinceSkin("tibet", "西藏"),
        ProvinceSkin("shaanxi", "陕西"),
        ProvinceSkin("gansu", "甘肃"),
        ProvinceSkin("qinghai", "青海"),
        ProvinceSkin("ningxia", "宁夏"),
        ProvinceSkin("xinjiang", "新疆"),
        ProvinceSkin("hongkong", "香港"),
        ProvinceSkin("macau", "澳门"),
        ProvinceSkin("taiwan", "台湾")
    )
    
    val provinceIds = provinces.map { it.id }
}
