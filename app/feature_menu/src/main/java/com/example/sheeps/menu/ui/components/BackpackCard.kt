package com.example.sheeps.menu.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
import com.example.sheeps.menu.state.MenuViewState
import com.example.sheeps.theme.CrimsonRed
import com.example.sheeps.theme.Gold_Primary
import com.example.sheeps.ui.components.ItemAnimationIcon
import com.hjq.toast.Toaster

/**
 * 背包物品卡片组件
 * 横向展示用户拥有的道具及其数量，并支持点击交互使用或应用皮肤
 *
 * @param state 界面状态数据，包含背包物品列表
 * @param onApplySkin 应用/切换皮肤回调
 * @param onGoToPlay 前往游戏关卡回调
 */
@Composable
fun BackpackCard(
    state: MenuViewState,
    onApplySkin: (String) -> Unit,
    onGoToPlay: () -> Unit
) {
    if (!state.isLoggedIn || state.backpackItems.isEmpty()) return

    var selectedItem by remember { mutableStateOf<com.example.sheeps.data.model.UserItem?>(null) }

    if (selectedItem != null) {
        val item = selectedItem!!
        val isSkin = item.item_type.startsWith("SKIN_") || item.item_type == "CLASSIC"
        val skinKey = remember(item.item_type) {
            if (item.item_type == "CLASSIC") {
                "classic"
            } else if (item.item_type.startsWith("SKIN_")) {
                item.item_type.removePrefix("SKIN_").lowercase()
            } else {
                "classic"
            }
        }
        val isCurrentlyApplied = isSkin && state.currentSkin == skinKey

        BackpackItemDetailDialog(
            item = item,
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
                color = CrimsonRed,
                modifier = Modifier.padding(bottom = 12.dp),
                fontFamily = FontFamily.Serif
            )

            val magicItems = remember(state.backpackItems) {
                state.backpackItems.filter { !it.item_type.startsWith("SKIN_") && it.item_type != "CLASSIC" }
            }
            val skinItems = remember(state.backpackItems) {
                state.backpackItems.filter { it.item_type.startsWith("SKIN_") || it.item_type == "CLASSIC" }
            }

            if (magicItems.isNotEmpty()) {
                Text(
                    text = stringResource(id = R.string.backpack_magic_section),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Gold_Primary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    magicItems.forEach { item ->
                        BackpackItem(item = item, onClick = { selectedItem = item })
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
                    color = Gold_Primary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    skinItems.forEach { item ->
                        BackpackItem(item = item, onClick = { selectedItem = item })
                    }
                }
            }
        }
    }
}

/**
 * 单个背包物品条目
 */
@Composable
private fun BackpackItem(
    item: com.example.sheeps.data.model.UserItem,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val isSkin = item.item_type.startsWith("SKIN_") || item.item_type == "CLASSIC"
    val skinKey = remember(item.item_type) {
        if (item.item_type == "CLASSIC") {
            "classic"
        } else if (item.item_type.startsWith("SKIN_")) {
            item.item_type.removePrefix("SKIN_").lowercase()
        } else {
            "classic"
        }
    }

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
            val isProvinceSkin = item.item_type.startsWith("SKIN_") && item.item_type != "SKIN_INK" && item.item_type != "SKIN_CYBER"
            if (isSkin && isProvinceSkin) {
                val iconRes = TileIconProvider.getIconResource(context, skinKey, 1)
                if (iconRes != 0) {
                    Image(
                        painter = painterResource(id = iconRes),
                        contentDescription = "Skin Preview",
                        modifier = Modifier.size(36.dp)
                    )
                } else {
                    Box(modifier = Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                        Text("?", fontSize = 16.sp, color = Color.Gray)
                    }
                }
            } else {
                ItemAnimationIcon(
                    itemType = item.item_type,
                    size = 36.dp
                )
            }
        }

        Text(
            text = getLocalizedItemName(item.item_type),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = CrimsonRed,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = stringResource(id = R.string.backpack_stock, item.count),
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.DarkGray
        )
    }
}

/**
 * 背包物品详细信息与使用/应用弹窗
 */
@Composable
private fun BackpackItemDetailDialog(
    item: com.example.sheeps.data.model.UserItem,
    isCurrentSkin: Boolean,
    onDismiss: () -> Unit,
    onApplySkin: () -> Unit,
    onGoToPlay: () -> Unit
) {
    val context = LocalContext.current
    val isSkin = item.item_type.startsWith("SKIN_") || item.item_type == "CLASSIC"
    val skinKey = remember(item.item_type) {
        if (item.item_type == "CLASSIC") {
            "classic"
        } else if (item.item_type.startsWith("SKIN_")) {
            item.item_type.removePrefix("SKIN_").lowercase()
        } else {
            "classic"
        }
    }

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
                    val isProvinceSkin = item.item_type.startsWith("SKIN_") && item.item_type != "SKIN_INK" && item.item_type != "SKIN_CYBER"
                    if (isSkin && isProvinceSkin) {
                        val iconRes = TileIconProvider.getIconResource(context, skinKey, 1)
                        Image(
                            painter = painterResource(id = iconRes),
                            contentDescription = "Skin Preview",
                            modifier = Modifier.size(64.dp)
                        )
                    } else {
                        ItemAnimationIcon(
                            itemType = item.item_type,
                            size = 64.dp
                        )
                    }
                }

                // 道具/皮肤名称
                Text(
                    text = getLocalizedItemName(item.item_type),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = CrimsonRed,
                    fontFamily = FontFamily.Serif
                )

                Spacer(modifier = Modifier.height(4.dp))

                // 拥有数量
                Text(
                    text = stringResource(id = R.string.backpack_stock, item.count),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 功能描述
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = getLocalizedItemDesc(item.item_type, null),
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
                                contentColor = Color.White,
                                disabledContainerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                disabledContentColor = Color.Gray
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
                                contentColor = Color.White
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
