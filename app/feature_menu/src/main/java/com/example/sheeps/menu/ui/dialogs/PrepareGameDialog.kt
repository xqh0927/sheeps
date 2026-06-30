package com.example.sheeps.menu.ui.dialogs

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.sheeps.menu.state.MenuViewState
import com.example.sheeps.theme.CrimsonRed
import com.example.sheeps.theme.GoldenBronze
import com.example.sheeps.ui.components.ItemAnimationIcon
import com.hjq.toast.Toaster
import com.example.sheeps.core.R
import com.example.sheeps.core.utils.getLocalizedItemName
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext

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

    val hasBlind = remember(levelId) {
        if (levelId >= 3) {
            // 模拟 LCG 算法判断关卡类型是否为盲盒关卡（概率 20%）
            var s = levelId * 1000L + 500L
            s = (s * 1664525L + 1013904223L) % 4294967296L
            val typeRoll = s.toDouble() / 4294967296.0
            typeRoll < 0.20
        } else false
    }

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
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDF9)),
                border = BorderStroke(
                    2.dp,
                    Brush.linearGradient(listOf(GoldenBronze, Color(0xFFEDD9A3), GoldenBronze))
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
                        text = if (isLocked) stringResource(id = R.string.prepare_title_locked) else stringResource(id = R.string.prepare_title_unlocked, levelId),
                        fontWeight = FontWeight.Bold,
                        color = CrimsonRed,
                        fontSize = 18.sp,
                        fontFamily = FontFamily.Serif,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    if (isLocked) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(
                                text = stringResource(id = R.string.prepare_locked_desc, cost, state.points),
                                fontSize = 13.sp,
                                lineHeight = 20.sp,
                                color = Color.DarkGray
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = onDismiss) {
                                    Text(stringResource(id = R.string.dialog_prepare_back), color = Color.Gray)
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
                                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(stringResource(id = R.string.dialog_prepare_unlock), color = Color.White)
                                }
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = stringResource(id = R.string.prepare_setup_desc),
                                fontSize = 12.sp,
                                color = Color.Gray,
                                lineHeight = 16.sp
                            )

                            if (hasBlind) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF0EC)),
                                    border = BorderStroke(1.dp, CrimsonRed.copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "⚠️ 秘境深处迷雾重重！本关已启用「盲盒牌」，卡牌下方图案不可见，难度大幅提升！",
                                            color = CrimsonRed,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            lineHeight = 15.sp
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            val itemTypes = listOf(
                                "UNDO", "SHUFFLE", "MOVEOUT", "REVIVE", "HINT", "BOMB", "JOKER", "DOUBLE_POINTS"
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
                                                    .background(Color(0xFFFCFAF6), RoundedCornerShape(12.dp))
                                                    .border(
                                                        1.dp,
                                                        if (selected > 0) CrimsonRed.copy(alpha = 0.5f) else Color(0xFFE5DDD3),
                                                        RoundedCornerShape(12.dp)
                                                    )
                                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                ItemAnimationIcon(
                                                    itemType = type,
                                                    size = 38.dp,
                                                    isGray = isGray
                                                )
                                                Text(
                                                    text = name,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.DarkGray,
                                                    maxLines = 1
                                                )
                                                Text(
                                                    stringResource(id = R.string.stock_prefix, stock),
                                                    fontSize = 9.sp,
                                                    color = Color.Gray,
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
                                                            .background(if (selected > 0) Color(0xFFFFF0EC) else Color(0xFFF5F5F5))
                                                            .clickable(enabled = selected > 0) { onUpdateItem(type, -1) },
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.KeyboardArrowDown,
                                                            contentDescription = stringResource(id = R.string.prepare_desc_decrease),
                                                            tint = if (selected > 0) CrimsonRed else Color.Gray,
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
                                                        color = if (selected > 0) CrimsonRed else Color.DarkGray
                                                    )

                                                    val canIncrease = selected < stock && (selected > 0 || selectedTypesCount < 5)
                                                    Box(
                                                        modifier = Modifier
                                                            .size(22.dp)
                                                            .clip(CircleShape)
                                                            .background(if (canIncrease) Color(0xFFFFF0EC) else Color(0xFFF5F5F5))
                                                            .clickable(enabled = canIncrease) { onUpdateItem(type, 1) },
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.KeyboardArrowUp,
                                                            contentDescription = stringResource(id = R.string.prepare_desc_increase),
                                                            tint = if (canIncrease) CrimsonRed else Color.Gray,
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
                                    Text(stringResource(id = R.string.dialog_prepare_close), color = Color.Gray)
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Button(
                                    onClick = { onConfirm(levelId) },
                                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        stringResource(id = R.string.prepare_btn_start),
                                        color = Color.White,
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
