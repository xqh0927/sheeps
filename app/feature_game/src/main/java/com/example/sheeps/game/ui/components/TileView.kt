package com.example.sheeps.game.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.sheeps.core.R
import com.example.sheeps.core.game.TileCardBase
import com.example.sheeps.core.game.TileIconProvider
import com.example.sheeps.data.model.Tile
import com.example.sheeps.data.model.TileState

import androidx.compose.ui.graphics.Color

/**
 * 秘境消消乐 · 卡牌组件（重构版）
 * 已经过优化，移除臃肿的 Canvas 绘图代码，支持 34 套省级行政区美食 WebP 图标
 */
@Composable
fun TileView(
    tile: Tile,
    onClick: () -> Unit,
    currentSkin: String = "classic",
    modifier: Modifier = Modifier,
    tileSize: Dp = 52.dp,
    isShaking: Boolean = false
) {
    val isBlocked = tile.state == TileState.BLOCKED
    val isBlind = tile.isBlind && isBlocked
    val isSealed = tile.sealedCount > 0

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // 按压弹性缩放动画
    val scale by animateFloatAsState(
        targetValue = if (isPressed && !isBlocked) 0.88f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "tileScale"
    )

    // Blocked shake feedback animation
    val shakeOffset = remember { Animatable(0f) }
    LaunchedEffect(isShaking) {
        if (isShaking) {
            repeat(4) {
                shakeOffset.animateTo(
                    targetValue = -6f,
                    animationSpec = tween(durationMillis = 50, easing = LinearEasing)
                )
                shakeOffset.animateTo(
                    targetValue = 6f,
                    animationSpec = tween(durationMillis = 50, easing = LinearEasing)
                )
            }
            shakeOffset.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 50, easing = LinearEasing)
            )
        }
    }

    val scaleByJiggle by animateFloatAsState(
        targetValue = if (isShaking) 1.1f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "jiggleScale"
    )

    TileCardBase(
        skin = currentSkin,
        modifier = modifier
            .offset(x = shakeOffset.value.dp)
            .graphicsLayer(
                scaleX = if (isShaking) scaleByJiggle else scale,
                scaleY = if (isShaking) scaleByJiggle else scale,
            )
            .size(tileSize)
            .border(
                width = if (isShaking) 2.dp else 0.dp,
                color = if (isShaking) Color.Red else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = true,
                onClick = onClick
            )
    ) {
        val context = LocalContext.current
        
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (isBlind) {
                // 神秘牌：显示背面图标
                Image(
                    painter = painterResource(id = R.drawable.tile_back),
                    contentDescription = "Blind Tile",
                    modifier = Modifier.size(tileSize * 0.7f)
                )
            } else {
                // 普通图标
                val iconResId = TileIconProvider.getIconResource(context, currentSkin, tile.type)
                Image(
                    painter = painterResource(id = iconResId),
                    contentDescription = "Tile Icon",
                    modifier = Modifier
                        .size(tileSize * 0.9f)
                        .alpha(if (isBlocked) 0.45f else 1f)
                )

                // 封印叠加层
                if (isSealed) {
                    Image(
                        painter = painterResource(id = R.drawable.tile_back),
                        contentDescription = "Sealed Overlay",
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(1f)
                    )
                }
            }
        }
    }
}
