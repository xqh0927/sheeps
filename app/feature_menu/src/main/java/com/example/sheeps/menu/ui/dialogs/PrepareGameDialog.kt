package com.example.sheeps.menu.ui.dialogs

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.sheeps.core.R
import com.example.sheeps.core.utils.getLocalizedItemName
import com.example.sheeps.menu.state.MenuViewState
import com.example.sheeps.ui.components.ItemIcon
import com.example.sheeps.core.game.TileIconProvider
import com.hjq.toast.Toaster

/**
 * 进入关卡前的准备对话框（自定义全屏遮罩 + [Card] 内容）。
 *
 * 展示关卡标题，并根据关卡规则提示特殊玩法（盲关/封印关/休息关）；
 * 关卡未解锁时显示解锁费用与"解锁"按钮（消耗积分），已解锁时可选择携带道具
 * （增减各种道具数量），最后点击"开始"进入游戏。
 *
 * 触发来源：首页/关卡选择（MenuScreen）点击某关卡进入。
 * 确认后：
 *  - 已解锁：由 [onConfirm] 回传 [levelId] 进入对局；
 *  - 道具增减：由 [onUpdateItem] 实时回写 ViewModel 的 `selectedCarryItems`；
 *  - 未解锁：由 [onUnlock] 回传 [levelId] 扣除积分并解锁（积分不足用 [Toaster] 提示）。
 *
 * 线程约束：所有 `state`（[MenuViewState]）读取与道具选择发生在主线程（UI 线程），
 * [onConfirm]/[onUpdateItem]/[onUnlock] 由上层负责切到 IO/网络线程提交。
 * [LocalContext] 仅用于 [Toaster] 提示，组合期间引用，Dialog 关闭后释放。
 *
 * ⚠️ 内存隐患：道具网格（8 种道具 × 多项状态）与图标列表在每次重组时由
 * `listOf(...)`/`chunked(4)` 重新创建，数据量小可接受；[ItemIcon] 加载的网络图片
 * 由图片加载库管理回收。本对话框无静态 Context 持有、无未取消协程，关闭即释放。
 *
 * @param levelId 目标关卡 ID（1 起）。
 * @param state 菜单视图状态（[MenuViewState]），含 unlockedLevel、points、selectedCarryItems、backpackItems 等。
 * @param onDismiss 关闭对话框的回调（返回/关闭按钮或点击遮罩触发）。
 * @param onConfirm 点击"开始"的回调，参数为 [levelId]，用于进入对局。
 * @param onUpdateItem 增减携带道具的回调，参数为 (道具类型, 变更数量 +1/-1)。
 * @param onUnlock 点击"解锁"的回调，参数为 [levelId]，用于扣除积分并解锁关卡。
 */
@Composable
fun PrepareGameDialog(
    levelId: Int,
    state: MenuViewState,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
    onUpdateItem: (String, Int) -> Unit,
    onUnlock: (Int) -> Unit
) {
    val context = LocalContext.current
    val isLocked = levelId > state.unlockedLevel
    val cost = if (levelId == 2) 50 else if (levelId == 3) 100 else 200

    val isRest = levelId >= 5 && levelId % 5 == 0
    val isBlind = !isRest && levelId >= 3 && levelId % 3 == 0
    val isSealed = !isRest && !isBlind && levelId >= 2 && levelId % 2 == 0

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
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
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.secondary,
                            Color(0xFFEDD9A3),
                            MaterialTheme.colorScheme.secondary
                        )
                    )
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth()
                ) {
                    // 对话框头部主标题文本
                    Text(
                        text = if (isLocked) stringResource(id = R.string.prepare_title_locked) else stringResource(
                            id = R.string.prepare_title_unlocked,
                            levelId
                        ),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 18.sp,
                        fontFamily = FontFamily.Serif,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    if (isLocked) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(
                                text = stringResource(
                                    id = R.string.prepare_locked_desc,
                                    cost,
                                    state.points
                                ),
                                fontSize = 13.sp,
                                lineHeight = 20.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = onDismiss) {
                                    Text(
                                        stringResource(id = R.string.dialog_prepare_back),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Button(
                                    onClick = {
                                        if (state.points >= cost) {
                                            onUnlock(levelId)
                                        } else {
                                            Toaster.show(context.getString(R.string.prepare_locked_insufficient))
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        stringResource(id = R.string.dialog_prepare_unlock),
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = stringResource(id = R.string.prepare_setup_desc),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 16.sp
                            )

                            if (isBlind) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                    border = BorderStroke(
                                        1.dp,
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = stringResource(id = R.string.prepare_blind_warning),
                                            color = MaterialTheme.colorScheme.primary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            lineHeight = 15.sp
                                        )
                                    }
                                }
                            }

                            if (isSealed) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                                    border = BorderStroke(
                                        1.dp,
                                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = stringResource(id = R.string.prepare_sealed_warning),
                                            color = MaterialTheme.colorScheme.secondary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            lineHeight = 15.sp
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            val itemTypes = listOf(
                                "UNDO",
                                "SHUFFLE",
                                "MOVEOUT",
                                "REVIVE",
                                "HINT",
                                "BOMB",
                                "JOKER",
                                "DOUBLE_POINTS"
                            )

                            val selectedTypesCount = remember(state.selectedCarryItems) {
                                state.selectedCarryItems.count { it.value > 0 }
                            }

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
                                            val stock =
                                                state.backpackItems.find { it.item_type == type }?.count
                                                    ?: 0
                                            val selected = state.selectedCarryItems[type] ?: 0
                                            val isGray = selected == 0

                                            Column(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .background(
                                                        MaterialTheme.colorScheme.surfaceVariant,
                                                        RoundedCornerShape(12.dp)
                                                    )
                                                    .border(
                                                        1.dp,
                                                        if (selected > 0) MaterialTheme.colorScheme.primary.copy(
                                                            alpha = 0.5f
                                                        ) else MaterialTheme.colorScheme.outlineVariant,
                                                        RoundedCornerShape(12.dp)
                                                    )
                                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                ItemIcon(
                                                    itemType = type,
                                                    imageUrl = TileIconProvider.getItemIconUrl(type),
                                                    size = 38.dp,
                                                    isGray = isGray
                                                )
                                                Text(
                                                    text = name,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1
                                                )
                                                Text(
                                                    stringResource(
                                                        id = R.string.stock_prefix,
                                                        stock
                                                    ),
                                                    fontSize = 9.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1
                                                )

                                                Spacer(modifier = Modifier.height(2.dp))

                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.Center,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(22.dp)
                                                            .clip(CircleShape)
                                                            .background(if (selected > 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.outlineVariant)
                                                            .clickable(enabled = selected > 0) {
                                                                onUpdateItem(
                                                                    type,
                                                                    -1
                                                                )
                                                            },
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.KeyboardArrowDown,
                                                            contentDescription = "减少",
                                                            tint = if (selected > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                    }

                                                    val textScale by animateFloatAsState(
                                                        targetValue = if (selected > 0) 1.15f else 1f,
                                                        animationSpec = spring(stiffness = Spring.StiffnessHigh),
                                                        label = "textScale"
                                                    )
                                                    Text(
                                                        text = selected.toString(),
                                                        modifier = Modifier
                                                            .padding(horizontal = 6.dp)
                                                            .graphicsLayer(
                                                                scaleX = textScale,
                                                                scaleY = textScale
                                                            ),
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (selected > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                    )

                                                    val canIncrease =
                                                        selected < stock && (selected > 0 || selectedTypesCount < 5)
                                                    Box(
                                                        modifier = Modifier
                                                            .size(22.dp)
                                                            .clip(CircleShape)
                                                            .background(if (canIncrease) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.outlineVariant)
                                                            .clickable(enabled = canIncrease) {
                                                                onUpdateItem(
                                                                    type,
                                                                    1
                                                                )
                                                            },
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.KeyboardArrowUp,
                                                            contentDescription = "增加",
                                                            tint = if (canIncrease) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                            modifier = Modifier.size(14.dp)
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

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(onClick = onDismiss) {
                                    Text(
                                        stringResource(id = R.string.dialog_prepare_close),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Button(
                                    onClick = { onConfirm(levelId) },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        stringResource(id = R.string.prepare_btn_start),
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
    }
}
