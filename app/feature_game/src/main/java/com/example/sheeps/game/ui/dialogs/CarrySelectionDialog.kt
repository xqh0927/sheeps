package com.example.sheeps.game.ui.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.sheeps.ui.R
import com.example.sheeps.core.utils.getLocalizedItemName
import com.example.sheeps.game.state.GameViewState
import com.example.sheeps.ui.components.ItemIcon
import com.example.sheeps.core.game.TileIconProvider

/**
 * 重开携带道具选择弹窗（Compose `Dialog`）。
 *
 * 以全屏半透明遮罩 + 居中卡片呈现，列出 8 种道具（UNDO / SHUFFLE / MOVEOUT /
 * REVIVE / HINT / BOMB / JOKER / DOUBLE_POINTS），每种显示图标、本地化名称、背包库存
 * （[GameViewState.backpackItemStocks]）与已选数量（[GameViewState.tempCarryItems]），
 * 通过 +/- 调用 [onUpdateItem] 调整临时携带数量，[onConfirm] 确认开始。
 *
 * 交互细节：
 * - 遮罩层 `clickable` 触发 [onDismiss]；卡片层 `clickable` 通过空实现阻断点击冒泡，
 *   避免误关弹窗。
 * - `DialogProperties(usePlatformDefaultWidth = false)` 使弹窗可占满宽度（0.95f）。
 *
 * 线程约束：纯 Composable，运行于主线程；图标 URL 由 `TileIconProvider` 同步提供。
 * ⚠️ 内存隐患：本弹窗随 `state.showCarrySelection` 组合/销毁，无静态引用；
 * `MutableInteractionSource` 用 `remember` 创建，随重组复用、随销毁释放，无泄漏。
 *
 * @param state       当前游戏视图状态，提供背包库存与已选道具
 * @param onDismiss   点击遮罩或“取消”时关闭弹窗
 * @param onConfirm   点击“确认开始”时带当前选择提交
 * @param onUpdateItem 调整某道具的临时携带数量（type, delta）
 */
@Composable
fun CarrySelectionDialog(
    state: GameViewState,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onUpdateItem: (String, Int) -> Unit
) {
    val itemTypes = listOf(
        "UNDO", "SHUFFLE", "MOVEOUT", "REVIVE", "HINT", "BOMB", "JOKER", "DOUBLE_POINTS"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }) { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }) { /* Block click propagation */ },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(
                    2.dp,
                    Brush.linearGradient(listOf(MaterialTheme.colorScheme.secondary, Color(0xFFEDD9A3), MaterialTheme.colorScheme.secondary))
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(id = R.string.carry_dialog_title),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 18.sp,
                        fontFamily = FontFamily.Serif,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Text(
                        text = stringResource(id = R.string.carry_dialog_desc),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    val chunkedItems = itemTypes.chunked(4)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        chunkedItems.forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                rowItems.forEach { type ->
                                    val name = getLocalizedItemName(type)
                                    val stock = state.backpackItemStocks[type] ?: 0
                                    val selected = state.tempCarryItems[type] ?: 0
                                    val isGray = selected == 0

                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                                            .border(
                                                1.dp,
                                                if (selected > 0) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant,
                                                RoundedCornerShape(12.dp)
                                            )
                                            .padding(vertical = 8.dp, horizontal = 4.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        ItemIcon(
                                            itemType = type,
                                            imageUrl = TileIconProvider.getItemIconUrl(type),
                                            size = 36.dp,
                                            isGray = isGray
                                        )
                                        Text(
                                            text = name,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            text = stringResource(id = R.string.carry_stock_label, stock),
                                            fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            textAlign = TextAlign.Center
                                        )

                                        Spacer(modifier = Modifier.height(2.dp))

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .clip(CircleShape)
                                                    .background(if (selected > 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.outlineVariant)
                                                    .clickable(enabled = selected > 0) { onUpdateItem(type, -1) },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.KeyboardArrowDown,
                                                    contentDescription = "Decrease",
                                                    tint = if (selected > 0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                            }

                                            Text(
                                                text = selected.toString(),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )

                                            Box(
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .clip(CircleShape)
                                                    .background(if (selected < stock) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.outlineVariant)
                                                    .clickable(enabled = selected < stock) { onUpdateItem(type, 1) },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.KeyboardArrowUp,
                                                    contentDescription = "Increase",
                                                    tint = if (selected < stock) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                                if (rowItems.size < 4) {
                                    for (i in 0 until (4 - rowItems.size)) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text(stringResource(id = R.string.btn_cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Button(
                            onClick = onConfirm,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                stringResource(id = R.string.carry_confirm_start),
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
