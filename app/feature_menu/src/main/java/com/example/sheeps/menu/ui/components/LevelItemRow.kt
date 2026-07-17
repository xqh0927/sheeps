package com.example.sheeps.menu.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.example.sheeps.ui.R
import com.example.sheeps.ui.theme.ShapeLarge
import com.example.sheeps.ui.theme.ShapeMedium
import com.example.sheeps.ui.theme.ShapeSmall
import com.example.sheeps.ui.theme.Text_Disabled_Dark

/**
 * 单个关卡条目（关卡列表行）。
 * 展示关卡编号、类型描述（普通/盲阵/封印/休息/地狱）、解锁/锁定状态，
 * 并提供「开始」与「排行榜」入口。
 *
 * @param levelId 关卡编号（从 1 开始）。
 * @param isUnlocked 是否已解锁；锁定态降低透明度并显示锁图标，点击不触发 [onStart]。
 * @param onStart 点击卡片（已解锁时）开始该关卡的回调。
 * @param onShowLeaderboard 点击排行榜图标的回调。
 *
 * 说明：
 * - 无状态（Stateless）组件：所有展示数据来自参数，按压缩放动画由
 *   [androidx.compose.animation.core.animateFloatAsState] + [androidx.compose.foundation.interaction.MutableInteractionSource]
 *   驱动，运行于**主线程**。
 * - 关卡类型描述通过 `remember(levelId)` 记忆化派生，避免每次重组重复计算。
 */
@Composable
fun LevelItemRow(
    levelId: Int,
    isUnlocked: Boolean,
    onStart: () -> Unit,
    onShowLeaderboard: () -> Unit
) {

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && isUnlocked) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "levelCardScale"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .graphicsLayer(alpha = if (isUnlocked) 1f else 0.55f)
            .clip(ShapeLarge)
            .background(
                if (isUnlocked) {
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceVariant,
                            MaterialTheme.colorScheme.surfaceContainer
                        )
                    )
                } else {
                    Brush.linearGradient(
                        colors = listOf(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.colorScheme.surfaceVariant)
                    )
                }
            )
            .border(
                width = if (isUnlocked) 1.dp else 0.5.dp,
                brush = if (isUnlocked) {
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                        )
                    )
                } else {
                    Brush.linearGradient(
                        colors = listOf(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    )
                },
                shape = ShapeLarge
            )
            .clickable(
                interactionSource = interactionSource,
                indication        = null,
                onClick           = onStart
            )
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            if (isUnlocked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.surfaceContainer,
                            shape = ShapeMedium
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isUnlocked) {
                        Text(
                            text       = "$levelId",
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            fontSize   = 18.sp,
                            color      = MaterialTheme.colorScheme.secondary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                Column {
                    Text(
                        text       = stringResource(id = R.string.level_number, levelId),
                        style      = MaterialTheme.typography.titleMedium,
                        fontFamily = FontFamily.Serif,
                        color      = if (isUnlocked) MaterialTheme.colorScheme.onBackground else Text_Disabled_Dark
                    )
                    val descRes = remember(levelId) {
                        val isRest = levelId >= 5 && levelId % 5 == 0
                        val isBlind = !isRest && levelId >= 3 && levelId % 3 == 0
                        val isSealed = !isRest && !isBlind && levelId >= 2 && levelId % 2 == 0

                        when {
                            levelId == 1 -> R.string.level_desc_1
                            levelId == 2 -> R.string.level_desc_2
                            levelId == 3 -> R.string.level_desc_3
                            isBlind -> R.string.level_desc_blind
                            isSealed -> R.string.level_desc_sealed
                            else -> R.string.level_desc_hell
                        }
                    }
                    Text(
                        text  = stringResource(id = descRes),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isUnlocked) MaterialTheme.colorScheme.onSurfaceVariant else Text_Disabled_Dark.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            if (isUnlocked) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onShowLeaderboard,
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                shape = CircleShape
                            )
                            .border(
                                0.5.dp,
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
                                CircleShape
                            )
                    ) {
                        Text(text = "🏆", fontSize = 14.sp)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(ShapeSmall)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                            .border(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), ShapeSmall)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text       = stringResource(id = R.string.btn_break_array),
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.SemiBold,
                            fontSize   = 13.sp,
                            color      = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
