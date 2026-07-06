package com.example.sheeps.menu.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sheeps.core.R
import com.example.sheeps.core.game.TileIconProvider
import com.example.sheeps.core.utils.getLocalizedItemDesc
import com.example.sheeps.core.utils.getLocalizedItemName
import com.example.sheeps.data.model.ShopItem
import com.example.sheeps.ui.components.ItemAnimationIcon

@Composable
fun ShopItemCard(
    item: ShopItem,
    backpackCount: Int,
    currentSkin: String,
    userPoints: Int,
    onExchange: (Int) -> Unit,
    onApplySkin: (String) -> Unit
) {
    val isSkin = item.item_type.startsWith("SKIN_") || item.item_type == "CLASSIC"
    val isUnlocked =
        if (item.item_type == "CLASSIC") true else backpackCount >= 1

    val skinKey = remember(item.item_type) {
        if (item.item_type == "CLASSIC") {
            "classic"
        } else if (item.item_type.startsWith("SKIN_")) {
            item.item_type.removePrefix("SKIN_").lowercase()
        } else {
            "classic"
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
            // 所有皮肤类型（包括 CLASSIC、SKIN_INK、SKIN_CYBER、SKIN_KEAI、SKIN_DAIMENG 以及省份皮肤）
            // 都用第一张 tile 图片做封面预览
            val isAnySkin = isSkin
            if (isAnySkin) {
                val context = LocalContext.current
                val iconRes = TileIconProvider.getIconResource(context, skinKey, 1)
                if (iconRes != 0) {
                    Image(
                        painter = painterResource(id = iconRes),
                        contentDescription = "Skin Preview",
                        modifier = Modifier.size(64.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier.size(64.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "?",
                            fontSize = 24.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                ItemAnimationIcon(
                    itemType = item.item_type,
                    size = 64.dp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = getLocalizedItemName(item.item_type),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = getLocalizedItemDesc(item.item_type, item.description),
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
                    text = "确认兑换",
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
                        text = if (isSkin) "确定使用积分兑换此皮肤吗？" else "确定使用积分兑换此道具吗？",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = getLocalizedItemName(item.item_type),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // 数量选择器
                    if (isSkin) {
                        Text(
                            text = "数量: 1",
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
                            "单价:",
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
                            "总计:",
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
                            "当前余额:",
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
                    Text("确认兑换", color = MaterialTheme.colorScheme.onPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExchangeDialog = false }) {
                    Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }
}
