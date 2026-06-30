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
 * 负责渲染棋盘中的单张卡牌，处理视觉反馈、点击状态及状态叠加效果（封印、迷雾、锁定）。
 * * @param tile 当前卡牌的数据模型
 * @param onClick 点击事件回调
 * @param currentSkin 当前使用的皮肤主题
 * @param modifier 自定义修饰符
 * @param tileSize 卡牌的渲染尺寸（默认 52dp）
 * @param isShaking 是否处于被锁定且不可选中的“抖动”状态
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
    // 状态判定：根据卡牌属性判断是否处于迷雾（Blind）或被上方卡牌压制（Blocked）
    val isBlocked = tile.state == TileState.BLOCKED
    val isBlind = tile.isBlind && isBlocked
    val isSealed = tile.sealedCount > 0

    // 处理交互源：用于捕获按压状态以实现弹性缩放
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // 按压弹性缩放动画：点击时轻微缩小，增加反馈感
    val scale by animateFloatAsState(
        targetValue = if (isPressed && !isBlocked) 0.88f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "tileScale"
    )

    // 被阻止点击时的水平抖动动画：给玩家明确的“不可点击”视觉暗示
    val shakeOffset = remember { Animatable(0f) }
    LaunchedEffect(isShaking) {
        if (isShaking) {
            repeat(2) {
                shakeOffset.animateTo(-6f, tween(50, easing = LinearEasing))
                shakeOffset.animateTo(6f, tween(50, easing = LinearEasing))
            }
            shakeOffset.animateTo(0f, tween(50, easing = LinearEasing))
        }
    }

    // 抖动时的大小变化动画
    val scaleByJiggle by animateFloatAsState(
        targetValue = if (isShaking) 1.1f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "jiggleScale"
    )

    // 基础容器包装：处理皮肤边框和整体交互逻辑
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
                indication = null, // 移除系统默认水波纹，以自定义缩放为主
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
                // 情况 1：迷雾牌，显示牌背
                Image(
                    painter = painterResource(id = R.drawable.tile_back),
                    contentDescription = "Blind Tile",
                    modifier = Modifier.size(tileSize * 0.7f)
                )
            } else {
                // 情况 2：显示正面图标，若被压制则降低透明度
                val iconResId = TileIconProvider.getIconResource(context, currentSkin, tile.type)
                Image(
                    painter = painterResource(id = iconResId),
                    contentDescription = "Tile Icon",
                    modifier = Modifier
                        .size(tileSize * 0.9f)
                        .alpha(if (isBlocked) 0.45f else 1f)
                )

                // 叠加层：封印效果
                if (isSealed) {
                    Image(
                        painter = painterResource(id = R.drawable.tile_back),
                        contentDescription = "Sealed Overlay",
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(1f) // 可根据需求调整遮罩样式
                    )
                }
            }
        }
    }
}