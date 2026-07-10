package com.example.sheeps.menu.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.sheeps.core.R
import com.example.sheeps.core.game.TileIconProvider
import com.example.sheeps.core.utils.getLocalizedItemName
import com.example.sheeps.core.utils.getLocalizedItemDesc
import com.example.sheeps.data.model.ShopItem
import com.example.sheeps.menu.state.MenuViewState
import com.example.sheeps.ui.components.ItemIcon
import com.example.sheeps.ui.components.RemoteImage
import com.hjq.toast.Toaster

/**
 * 背包物品卡片组件
 * 横向展示用户拥有的道具及其数量，并支持点击交互使用或应用皮肤
 *
 * @param state 界面状态数据，包含背包物品列表
 * @param shopItems 商城商品缓存（来自 ShopCache，已按请求语言本地化）。按 item_type 索引后，
 *                  用于为背包中仅有 [com.example.sheeps.data.model.UserItem.item_type]（无 name / image_url）
 *                  的物品补全本地化名称与远程图标，修复「显示原始 key」与「显示默认 app 图标」两个问题。
 * @param onApplySkin 应用/切换皮肤回调
 * @param onGoToPlay 前往游戏关卡回调
 */
@Composable
fun BackpackCard(
    state: MenuViewState,
    shopItems: List<ShopItem>,
    onApplySkin: (String) -> Unit,
    onGoToPlay: () -> Unit
) {
    if (!state.isLoggedIn || state.backpackItems.isEmpty()) return

    // 按 item_type 建立索引，便于从 ShopItem 缓存中查找本地化的 name / image_url / description
    val shopItemMap = remember(shopItems) { shopItems.associateBy { it.item_type } }
    // 构建后台多语言映射（道具名 / 道具描述），传入 getLocalizedItemName/Desc 优先使用
    val itemI18nName = remember(shopItems) { shopItems.associate { it.item_type.uppercase() to (it.name ?: it.item_type) } }
    val itemI18nDesc = remember(shopItems) { shopItems.associate { it.item_type.uppercase() to (it.description ?: "") } }

    var selectedItem by remember { mutableStateOf<com.example.sheeps.data.model.UserItem?>(null) }

    if (selectedItem != null) {
        val item = selectedItem!!
        val isSkin = item.item_type.startsWith("SKIN_")
        val skinKey = remember(item.item_type) {
            if (item.item_type.startsWith("SKIN_")) {
                item.item_type.removePrefix("SKIN_").lowercase()
            } else {
                ""
            }
        }
        val isCurrentlyApplied = isSkin && state.currentSkin == skinKey

        BackpackItemDetailDialog(
            item = item,
            shopItemMap = shopItemMap,
            itemI18nName = itemI18nName,
            itemI18nDesc = itemI18nDesc,
            isCurrentSkin = isCurrentlyApplied,
            onDismiss = { selectedItem = null },
            onApplySkin = { onApplySkin(skinKey) },
            onGoToPlay = onGoToPlay
        )
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(id = R.string.backpack_title),
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp),
                fontFamily = FontFamily.Serif
            )

            val magicItems = remember(state.backpackItems) {
                state.backpackItems.filter { !it.item_type.startsWith("SKIN_") }
            }
            val skinItems = remember(state.backpackItems) {
                state.backpackItems.filter { it.item_type.startsWith("SKIN_") }
            }

            if (magicItems.isNotEmpty()) {
                Text(
                    text = stringResource(id = R.string.backpack_magic_section),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    magicItems.forEach { item ->
                        BackpackItem(item = item, shopItemMap = shopItemMap, itemI18nName = itemI18nName, onClick = { selectedItem = item })
                    }
                }
            }

            if (magicItems.isNotEmpty() && skinItems.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
            }

            if (skinItems.isNotEmpty()) {
                Text(
                    text = stringResource(id = R.string.backpack_skins_section),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    skinItems.forEach { item ->
                        BackpackItem(item = item, shopItemMap = shopItemMap, itemI18nName = itemI18nName, onClick = { selectedItem = item })
                    }
                }
            }
        }
    }
}

/**
 * 单个背包物品条目
 *
 * @param shopItemMap 按 item_type 索引的商城商品缓存，用于补全 name 与 image_url
 */
@Composable
private fun BackpackItem(
    item: com.example.sheeps.data.model.UserItem,
    shopItemMap: Map<String, ShopItem>,
    itemI18nName: Map<String, String>,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val isSkin = item.item_type.startsWith("SKIN_")
    val skinKey = remember(item.item_type) {
        if (item.item_type.startsWith("SKIN_")) {
            item.item_type.removePrefix("SKIN_").lowercase()
        } else {
            ""
        }
    }
    // 从商城缓存中查找对应的 ShopItem，优先使用其本地化 name / image_url
    val shopItem = remember(item.item_type) { shopItemMap[item.item_type] }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() }
            .padding(8.dp)
            .width(84.dp)
    ) {
        // 图标区域
        Box(
            modifier = Modifier
                .size(40.dp)
                .padding(bottom = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isSkin) {
                RemoteImage(
                    url = TileIconProvider.getTileUrl(skinKey, 1),
                    fallbackResId = TileIconProvider.getFallbackResId(context, 1),
                    modifier = Modifier.size(36.dp),
                    contentDescription = "Skin Preview"
                )
            } else {
                // 优先使用 ShopItem 的远程图标，服务端未下发（UserItem.image_url 为 null）时回退
                ItemIcon(
                    itemType = item.item_type,
                    imageUrl = shopItem?.image_url ?: item.image_url,
                    size = 36.dp
                )
            }
        }

        // 名称：优先使用 ShopItem 本地化名称，取不到再回退到本地映射
        Text(
            text = shopItem?.name ?: getLocalizedItemName(item.item_type, itemI18nName),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = stringResource(id = R.string.backpack_stock, item.count),
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * 背包物品详细信息与使用/应用弹窗
 *
 * @param shopItemMap 按 item_type 索引的商城商品缓存，用于补全 name 与 description
 */
@Composable
private fun BackpackItemDetailDialog(
    item: com.example.sheeps.data.model.UserItem,
    shopItemMap: Map<String, ShopItem>,
    itemI18nName: Map<String, String>,
    itemI18nDesc: Map<String, String>,
    isCurrentSkin: Boolean,
    onDismiss: () -> Unit,
    onApplySkin: () -> Unit,
    onGoToPlay: () -> Unit
) {
    val context = LocalContext.current
    val isSkin = item.item_type.startsWith("SKIN_")
    val skinKey = remember(item.item_type) {
        if (item.item_type.startsWith("SKIN_")) {
            item.item_type.removePrefix("SKIN_").lowercase()
        } else {
            ""
        }
    }
    // 从商城缓存中查找对应的 ShopItem，优先使用其本地化 name / description
    val shopItem = remember(item.item_type) { shopItemMap[item.item_type] }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.5.dp, Color(0xFFCBAA6A)), // 黄金边框
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 图标展示 (更清晰的 64.dp)
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .padding(bottom = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSkin) {
                        RemoteImage(
                            url = TileIconProvider.getTileUrl(skinKey, 1),
                            fallbackResId = TileIconProvider.getFallbackResId(context, 1),
                            modifier = Modifier.size(64.dp),
                            contentDescription = "Skin Preview"
                        )
                    } else {
                        // 优先使用 ShopItem 的远程图标，服务端未下发（UserItem.image_url 为 null）时回退
                        ItemIcon(
                            itemType = item.item_type,
                            imageUrl = shopItem?.image_url ?: item.image_url,
                            size = 64.dp
                        )
                    }
                }

                // 道具/皮肤名称：优先使用 ShopItem 本地化名称，取不到再回退到本地映射
                Text(
                    text = shopItem?.name ?: getLocalizedItemName(item.item_type, itemI18nName),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = FontFamily.Serif
                )

                Spacer(modifier = Modifier.height(4.dp))

                // 拥有数量
                Text(
                    text = stringResource(id = R.string.backpack_stock, item.count),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 功能描述：优先使用 ShopItem 本地化描述，取不到再回退到本地映射
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = shopItem?.description ?: getLocalizedItemDesc(item.item_type, null, itemI18nDesc),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 操作按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 返回按钮
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = stringResource(id = R.string.btn_back), fontSize = 14.sp)
                    }

                    // 主要动作按钮（使用/应用）
                    if (isSkin) {
                        Button(
                            onClick = {
                                if (!isCurrentSkin) {
                                    onApplySkin()
                                    Toaster.show(context.getString(R.string.skin_apply_success))
                                }
                                onDismiss()
                            },
                            enabled = !isCurrentSkin,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF9E1F1F),
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                disabledContainerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Text(
                                text = if (isCurrentSkin) stringResource(id = R.string.skin_applied) else stringResource(id = R.string.skin_apply),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Button(
                            onClick = {
                                onGoToPlay()
                                onDismiss()
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF9E1F1F),
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Text(
                                text = stringResource(id = R.string.btn_go_to_play),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
