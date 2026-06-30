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

    var animateTrigger by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        animateTrigger = true
    }
    val scale by animateFloatAsState(
        targetValue = if (animateTrigger) 1f else 0.85f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "dialogScale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (animateTrigger) 1f else 0f,
        animationSpec = tween(300, easing = EaseOutQuad),
        label = "dialogAlpha"
    )

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
                    .fillMaxWidth(0.9f)
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        alpha = alpha
                    )
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
                    // Header text
                    Text(
                        text = if (isLocked) "关卡结界未解" else "整装破局（第 $levelId 关）",
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

                            Spacer(modifier = Modifier.height(4.dp))

                            val itemTypes = listOf(
                                "UNDO", "SHUFFLE", "MOVEOUT", "REVIVE", "HINT", "BOMB", "JOKER", "DOUBLE_POINTS"
                            )

                            val selectedTypesCount = remember(state.selectedCarryItems) {
                                state.selectedCarryItems.count { it.value > 0 }
                            }

                            Column(
                                modifier = Modifier
                                    .height(280.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                itemTypes.forEach { type ->
                                    val name = getLocalizedItemName(type)
                                    val stock =
                                        state.backpackItems.find { it.item_type == type }?.count
                                            ?: 0
                                    val selected = state.selectedCarryItems[type] ?: 0
                                    val isGray = selected == 0

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .background(Color(0xFFFCFAF6), RoundedCornerShape(8.dp))
                                            .border(
                                                0.5.dp,
                                                Color(0xFFE5DDD3),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .padding(8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            // Call new high definition Canvas vector animation icon
                                            ItemAnimationIcon(
                                                itemType = type,
                                                size = 40.dp,
                                                isGray = isGray
                                            )
                                            Column {
                                                Text(
                                                    text = name,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.DarkGray
                                                )
                                                Text(
                                                    stringResource(id = R.string.stock_prefix, stock),
                                                    fontSize = 11.sp,
                                                    color = Color.Gray
                                                )
                                            }
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            IconButton(
                                                onClick = { onUpdateItem(type, -1) },
                                                enabled = selected > 0,
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .background(
                                                        if (selected > 0) Color(0xFFFFF0EC) else Color(
                                                            0xFFF5F5F5
                                                        ), CircleShape
                                                    )
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.KeyboardArrowDown,
                                                    contentDescription = stringResource(id = R.string.prepare_desc_decrease),
                                                    tint = if (selected > 0) CrimsonRed else Color.Gray,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }

                                            // Number change spring scale feedback
                                            val textScale by animateFloatAsState(
                                                targetValue = if (selected > 0) 1.15f else 1f,
                                                animationSpec = spring(stiffness = Spring.StiffnessHigh),
                                                label = "textScale"
                                            )
                                            Text(
                                                text = selected.toString(),
                                                modifier = Modifier
                                                    .padding(horizontal = 10.dp)
                                                    .graphicsLayer(
                                                        scaleX = textScale,
                                                        scaleY = textScale
                                                    ),
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (selected > 0) CrimsonRed else Color.DarkGray
                                            )
                                            val canIncrease = selected < stock && (selected > 0 || selectedTypesCount < 5)
                                            IconButton(
                                                onClick = { onUpdateItem(type, 1) },
                                                enabled = canIncrease,
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .background(
                                                        if (canIncrease) Color(
                                                            0xFFFFF0EC
                                                        ) else Color(0xFFF5F5F5), CircleShape
                                                    )
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.KeyboardArrowUp,
                                                    contentDescription = stringResource(id = R.string.prepare_desc_increase),
                                                    tint = if (canIncrease) CrimsonRed else Color.Gray,
                                                    modifier = Modifier.size(18.dp)
                                                )
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
