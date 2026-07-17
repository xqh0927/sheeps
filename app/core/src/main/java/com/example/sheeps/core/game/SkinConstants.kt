package com.example.sheeps.core.game

/**
 * 皮肤系统常量配置。
 *
 * 方案 A（多语言动态下发）：非默认皮肤的「名称 / 描述 / 分组头」等本地化文案已移交给服务端
 * /api/shop/items 动态下发，客户端不再硬编码（原 ProvinceSkin / SpecialSkin / provinces /
 * SHOP_GROUPS / GROUP_OTHER 等本地配置已清除）。本文件仅保留渲染键与本地兜底所需的常量。
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
     * 本地 drawable 兜底：仅 "shuang"（萌趣竞技）在 app/core/src/main/res/drawable 下内置 12 张
     * 本地 drawable：tile_shuang_1..12.webp。这是**唯一**的本地兜底资源；electronic / daily /
     * vegetable / fruit / henan / sichuan 等皮肤均通过远程 URL 加载卡面，失败统一回退到 shuang
     * 本地 drawable，而非各自拥有本地兜底。
     *
     * 因此 DEFAULT_SKIN 必须为 "shuang"：它既是全局默认皮肤，也是所有远程皮肤的最终兜底。
     */
    const val DEFAULT_SKIN = "shuang"
}
