package com.example.sheeps.menu.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sheeps.core.R
import com.example.sheeps.core.game.TileIconProvider
import com.example.sheeps.core.utils.getShopItemDesc
import com.example.sheeps.core.utils.getShopItemName
import com.example.sheeps.data.model.ShopItem
import com.example.sheeps.ui.components.ItemIcon
import com.example.sheeps.ui.components.RemoteImage

/**
 * 商城商品卡片（道具 / 皮肤）。
 * 展示封面、名称、描述、价格/库存，并提供「兑换」或「应用皮肤」操作；兑换时弹出数量确认弹窗。
 *
 * @param item 商品数据 [com.example.sheeps.data.model.ShopItem]。
 * @param backpackCount 背包中该商品已拥有数量（用于判断是否已解锁皮肤）。
 * @param currentSkin 当前已应用的皮肤 key。
 * @param userPoints 用户当前积分（用于计算可兑换上限）。
 * @param onExchange 确认兑换回调，参数为兑换数量。
 * @param onApplySkin 应用/切换皮肤回调，参数为皮肤 key。
 *
 * 说明：
 * - 无状态（Stateless）组件，所有数据来自参数；本地 UI 状态（兑换弹窗显隐、数量）通过
 *   `remember { mutableStateOf(...) }` 持有，随组合销毁而释放。
 * - 弹窗（AlertDialog）内嵌于本 Composable，确认后通过 [onExchange]/[onApplySkin] 将意图上抛给 ViewModel。
 * - 封面图片通过 Coil 异步加载（RemoteImage），图片请求在 IO 线程执行，结果切回**主线程**提交组合。
 */
@Composable
fun ShopItemCard(
    item: ShopItem,
    backpackCount: Int,
    currentSkin: String,
    userPoints: Int,
    onExchange: (Int) -> Unit,
    onApplySkin: (String) -> Unit
) {
    val isSkin = item.item_type.startsWith("SKIN_")
    val isUnlocked = backpackCount >= 1

    val skinKey = remember(item.item_type) {
        if (item.item_type.startsWith("SKIN_")) {
            item.item_type.removePrefix("SKIN_").lowercase()
        } else {
            ""
        }
    }
    val isCurrentlyApplied = isSkin && currentSkin == skinKey

    var showExchangeDialog by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 所有皮肤类型（包括省份皮肤与特效皮肤系列）都用第一张 tile 图片做封面预览
            // v2：URL 优先（Coil），缺失/失败回退默认皮肤本地 drawable
            val isAnySkin = isSkin
            if (isAnySkin) {
                val context = LocalContext.current
                RemoteImage(
                    url = TileIconProvider.getTileUrl(skinKey, 1),
                    fallbackResId = TileIconProvider.getFallbackResId(context, 1),
                    modifier = Modifier.size(64.dp),
                    contentDescription = "Skin Preview"
                )
            } else {
                ItemIcon(
                    itemType = item.item_type,
                    imageUrl = item.image_url,
                    size = 64.dp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = getShopItemName(item),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = getShopItemDesc(item),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                minLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (isSkin && isUnlocked) stringResource(id = R.string.skin_unlocked) else stringResource(
                        id = R.string.points_suffix,
                        item.points_price
                    ),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                if (!isSkin) {
                    Text(
                        text = stringResource(id = R.string.stock_prefix, item.stock),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (isSkin && isUnlocked) {
                Button(
                    onClick = { onApplySkin(skinKey) },
                    enabled = !isCurrentlyApplied,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCurrentlyApplied) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary,
                        disabledContainerColor = MaterialTheme.colorScheme.outlineVariant,
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(30.dp)
                ) {
                    Text(
                        text = if (isCurrentlyApplied) stringResource(id = R.string.skin_applied) else stringResource(
                            id = R.string.skin_apply
                        ),
                        fontSize = 11.sp,
                        color = if (isCurrentlyApplied) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onPrimary
                    )
                }
            } else {
                Button(
                    onClick = { showExchangeDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(30.dp)
                ) {
                    Text(
                        stringResource(id = R.string.btn_exchange),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }

    // 兑换确认弹窗
    if (showExchangeDialog) {
        val maxAffordable =
            if (item.points_price > 0) userPoints / item.points_price else Int.MAX_VALUE
        val maxQuantity = if (isSkin) 1 else minOf(maxAffordable, item.stock)
        var quantity by remember { mutableIntStateOf(1) }

        AlertDialog(
            onDismissRequest = { showExchangeDialog = false },
            title = {
                Text(
                    text = stringResource(id = R.string.shop_exchange_title),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(
                            id = if (isSkin) R.string.shop_exchange_skin_desc else R.string.shop_exchange_item_desc
                        ),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = getShopItemName(item),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // 数量选择器
                    if (isSkin) {
                        Text(
                            text = stringResource(id = R.string.shop_quantity_label, 1),
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            IconButton(
                                onClick = { if (quantity > 1) quantity-- },
                                enabled = quantity > 1
                            ) {
                                Text(
                                    "−",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (quantity > 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            OutlinedTextField(
                                value = quantity.toString(),
                                onValueChange = { v ->
                                    v.toIntOrNull()?.let { n ->
                                        quantity = n.coerceIn(1, maxQuantity)
                                    }
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                textStyle = LocalContext.current.let { ctx ->
                                    androidx.compose.ui.text.TextStyle(
                                        textAlign = TextAlign.Center,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                modifier = Modifier.width(72.dp)
                            )

                            IconButton(
                                onClick = { if (quantity < maxQuantity) quantity++ },
                                enabled = quantity < maxQuantity
                            ) {
                                Text(
                                    "+",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (quantity < maxQuantity) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 费用汇总
                    val totalCost = item.points_price * quantity
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            stringResource(id = R.string.shop_unit_price_label),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            stringResource(id = R.string.points_suffix, item.points_price),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            stringResource(id = R.string.shop_total_label),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            stringResource(id = R.string.points_suffix, totalCost),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            stringResource(id = R.string.shop_balance_label),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            stringResource(id = R.string.points_suffix, userPoints),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onExchange(quantity)
                        showExchangeDialog = false
                    },
                    enabled = quantity > 0 && quantity <= maxQuantity,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(stringResource(id = R.string.shop_exchange_title), color = MaterialTheme.colorScheme.onPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExchangeDialog = false }) {
                    Text(stringResource(id = R.string.btn_cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }
}
