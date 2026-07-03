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
import com.example.sheeps.core.R
import com.example.sheeps.theme.ShapeLarge
import com.example.sheeps.theme.ShapeMedium
import com.example.sheeps.theme.ShapeSmall
import com.example.sheeps.theme.Text_Disabled_Dark

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
                    Text(
                        text  = when (levelId) {
                            1    -> stringResource(id = R.string.level_desc_1)
                            2    -> stringResource(id = R.string.level_desc_2)
                            3    -> stringResource(id = R.string.level_desc_3)
                            else -> stringResource(id = R.string.level_desc_other)
                        },
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
