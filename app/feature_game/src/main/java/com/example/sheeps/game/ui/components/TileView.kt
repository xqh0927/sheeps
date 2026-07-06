package com.example.sheeps.game.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.apkfuns.logutils.LogUtils
import com.example.sheeps.core.R
import com.example.sheeps.core.game.TileCardBase
import com.example.sheeps.core.game.TileIconProvider
import com.example.sheeps.data.model.Tile
import com.example.sheeps.data.model.TileState
import com.example.sheeps.game.BuildConfig
import com.example.sheeps.game.ui.animations.GameAnimations

/**
 * 秘境消消乐 · 卡牌组件（重构版）
 * 负责渲染棋盘中的单张卡牌，处理视觉反馈、点击状态及状态叠加效果（封印、迷雾、锁定）。
 *
 * @param tile 当前卡牌的数据模型
 * @param onClick 点击事件回调
 * @param currentSkin 当前使用的皮肤主题
 * @param modifier 自定义修饰符
 * @param tileSize 卡牌的渲染尺寸（默认 52dp）
 * @param isShaking 是否处于被锁定且不可选中的“抖动”状态
 * @param isHighlighted 是否处于“提示”状态（金光闪烁、呼吸缩放）
 */
@Composable
fun TileView(
    tile: Tile,
    onClick: () -> Unit,
    currentSkin: String = "classic",
    modifier: Modifier = Modifier,
    tileSize: Dp = 48.dp,
    isShaking: Boolean = false,
    isHighlighted: Boolean = false
) {
    // 状态判定：根据卡牌属性判断是否处于迷雾（Blind）或被上方卡牌压制（Blocked）
    val isBlocked = tile.state == TileState.BLOCKED

    //不可点击遮罩透明度
    val mask = if (BuildConfig.DEBUG) 0f else 0.35f

    val isBlind =
        tile.isBlind && (tile.state == TileState.NORMAL || tile.state == TileState.BLOCKED)
    val isSealed = tile.sealedCount > 0

    // 处理交互源：用于捕获按压状态以实现弹性缩放
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // 提示符高亮与按压相关的动画，均提取自 GameAnimations 工具类
    val pulseScale by GameAnimations.rememberPulseScale(
        isHighlighted
    )
    val borderAlpha by GameAnimations.rememberHighlightBorderAlpha(
        isHighlighted
    )
    val scale by GameAnimations.rememberPressScale(
        interactionSource, isBlocked
    )

    // 被阻止点击时的水平抖动动画：给玩家明确的“不可点击”视觉暗示
    val shakeOffset = remember { Animatable(0f) }
    LaunchedEffect(isShaking) {
        if (isShaking) {
            GameAnimations.runTileShakeAnimation(shakeOffset)
        }
    }

    // 抖动时的大小变化动画
    val scaleByJiggle by animateFloatAsState(
        targetValue = if (isShaking) 1.05f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "jiggleScale"
    )

    // 基础容器包装：处理皮肤边框和整体交互逻辑
    TileCardBase(
        skin = currentSkin, modifier = modifier
            .offset(x = shakeOffset.value.dp)
            .graphicsLayer(
                scaleX = if (isShaking) scaleByJiggle else (scale * pulseScale),
                scaleY = if (isShaking) scaleByJiggle else (scale * pulseScale),
            )
            .size(tileSize)
            .border(
                width = if (isShaking) 2.dp else if (isHighlighted) 2.5.dp else 0.dp,
                color = if (isShaking) Color.Red else if (isHighlighted) MaterialTheme.colorScheme.secondary.copy(
                    alpha = borderAlpha
                ) else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(
                interactionSource = interactionSource, indication = null, // 移除系统默认水波纹，以自定义缩放为主
                enabled = true, onClick = onClick
            )
    ) {
        val context = LocalContext.current

        // Coil ImageLoader 用于渲染动画 WebP（灵动动画系列皮肤）
        val imageLoader = remember {
            ImageLoader.Builder(context)
                .components {
                    add(ImageDecoderDecoder.Factory())
                }
                .build()
        }

        Box(
            modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
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
                // 灵动动画系列皮肤（keai / daimeng）使用 Coil AsyncImage 渲染动画 WebP
                val isAnimatedSkin = currentSkin == "keai" || currentSkin == "daimeng"
                val iconResId = TileIconProvider.getIconResource(context, currentSkin, tile.type)
                if (iconResId != 0) {
                    if (isAnimatedSkin) {
                        AsyncImage(
                            model = ImageRequest.Builder(context).data(iconResId).build(),
                            contentDescription = "Tile Icon",
                            imageLoader = imageLoader,
                            modifier = Modifier
                                .size(tileSize * 0.9f)
                                .alpha(if (isBlocked) mask else 1f)
                        )
                    } else {
                        Image(
                            painter = painterResource(id = iconResId),
                            contentDescription = "Tile Icon",
                            modifier = Modifier
                                .size(tileSize * 0.9f)
                                .alpha(if (isBlocked) mask else 1f)
                        )
                    }
                } else {
                                      Box(
                        modifier = Modifier
                            .size(tileSize * 0.9f)
                            .alpha(if (isBlocked) mask else 1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tile.type.toString(),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // 临时显示 tile.id：NORMAL=红色，BLOCKED=蓝色，附带 z 层
                if (BuildConfig.DEBUG) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = tile.id.removePrefix("tile_"),
                            color = if (isBlocked) Color.Blue else Color.Red,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "z${tile.z}",
                            color = if (isBlocked) Color.Blue else Color.Red,
                            fontSize = 6.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // 叠加层：封印效果 (方案 A：半透明灰蓝色冰封网格 + 金色锁头图标 + 剩余解封次数)
                if (isSealed) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xBB2C3E50)) // 半透明灰蓝色冰封底色
                            .border(
                                1.5.dp,
                                Color(0xFFF1C40F).copy(alpha = 0.8f),
                                RoundedCornerShape(8.dp)
                            ), // 金色微光边框
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "🔒",
                                fontSize = 18.sp,
                                modifier = Modifier.graphicsLayer(scaleX = 1.1f, scaleY = 1.1f)
                            )
                            if (tile.sealedCount > 1) {
                                Text(
                                    text = "x${tile.sealedCount}",
                                    color = Color(0xFFF1C40F), // 金色字体
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 1.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}