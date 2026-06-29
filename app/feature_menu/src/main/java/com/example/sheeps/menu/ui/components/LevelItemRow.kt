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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sheeps.theme.*

@Composable
fun LevelItemRow(
    levelId: Int,
    isUnlocked: Boolean,
    onStart: () -> Unit
) {

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && isUnlocked) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "levelCardScale"
    )
    val infiniteTransition = rememberInfiniteTransition(label = "levelGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue  = 0.7f,
        animationSpec = infiniteRepeatable(
            animation  = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "levelGlowAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
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
                            Gold_Subtle.copy(alpha = glowAlpha),
                            Gold_Primary.copy(alpha = glowAlpha * 0.5f),
                            Gold_Subtle.copy(alpha = glowAlpha)
                        )
                    )
                } else {
                    Brush.linearGradient(
                        colors = listOf(MaterialTheme.colorScheme.outline, MaterialTheme.colorScheme.outline)
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
            // 左侧：关卡编号 + 难度
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 关卡编号圆标
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            if (isUnlocked) Crimson_PrimaryContainer.copy(alpha = 0.5f)
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
                            color      = Gold_Primary
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
                        text       = "第 $levelId 关",
                        style      = MaterialTheme.typography.titleMedium,
                        fontFamily = FontFamily.Serif,
                        color      = if (isUnlocked) Text_Primary_Dark else Text_Disabled_Dark
                    )
                    Text(
                        text  = when (levelId) {
                            1    -> "初稺门径 · 极其简单"
                            2    -> "略有小成 · 普通难度"
                            3    -> "降妖伏魔 · 略有挑战"
                            else -> "奇门生死局 · 包含封印/盲盒"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isUnlocked) Text_Secondary_Dark else Text_Disabled_Dark.copy(alpha = 0.6f)
                    )
                }
            }

            // 右侧：状态指示
            if (isUnlocked) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(ShapeSmall)
                        .background(Crimson_Primary.copy(alpha = 0.15f))
                        .border(0.5.dp, Crimson_Primary.copy(alpha = 0.3f), ShapeSmall)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text       = "破阵",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.SemiBold,
                        fontSize   = 13.sp,
                        color      = Gold_Primary
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = Gold_Primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            } else {
                Text(
                    text  = "封印中",
                    style = MaterialTheme.typography.labelSmall,
                    color = Text_Disabled_Dark
                )
            }
        }
    }
}
