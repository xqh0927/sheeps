package com.example.sheeps.game.ui.animations

import androidx.compose.animation.core.*
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 羊了个羊 游戏动画工具类 (GameAnimations)
 * 统一管理和复用游戏中棋盘震动、卡牌飞行、卡牌被锁抖动、高亮呼吸、点击按压反馈等所有动效。
 */
object GameAnimations {

    // ==========================================
    // 1. 基础动画参数配置 (Specs)
    // ==========================================

    /**
     * 卡牌飞行动画的插值 Specs (350ms, 缓入缓出)
     */
    val FlyTweenSpec = tween<Float>(
        durationMillis = 350,
        easing = FastOutSlowInEasing
    )

    /**
     * 抖动动画的插值 Specs (50ms, 线性)
     */
    val ShakeTweenSpec = tween<Float>(
        durationMillis = 50,
        easing = LinearEasing
    )

    /**
     * 棋盘震动动画的插值 Specs (45ms, 线性)
     */
    val BoardShakeTweenSpec = tween<Offset>(
        durationMillis = 45,
        easing = LinearEasing
    )

    /**
     * 呼吸灯发光/缩放动画的循环配置 (600ms, 双向往返)
     */
    val PulseRepeatableSpec = infiniteRepeatable<Float>(
        animation = tween(600, easing = FastOutSlowInEasing),
        repeatMode = RepeatMode.Reverse
    )

    /**
     * 卡牌点击缩小的弹簧配置 (有弹性，中等阻尼)
     */
    val PressSpringSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )

    // ==========================================
    // 2. 挂起式一阶动画执行器 (Suspend Animations)
    // ==========================================

    /**
     * 触发卡牌被锁定的单次水平抖动动画
     * 往复移动 1 次，最后归零。
     *
     * @param animatable 卡牌水平偏移的 Animatable 实例
     */
    suspend fun runTileShakeAnimation(animatable: Animatable<Float, *>) {
        repeat(1) {
            animatable.animateTo(-6f, ShakeTweenSpec)
            animatable.animateTo(6f, ShakeTweenSpec)
        }
        animatable.animateTo(0f, ShakeTweenSpec)
    }

    /**
     * 触发使用炸弹道具时的棋盘震动动画
     * 往复摇晃 2 次，最后归零复位。
     *
     * @param animatable 棋盘偏移量 Offset 的 Animatable 实例
     */
    suspend fun runBoardShakeAnimation(animatable: Animatable<Offset, *>) {
        repeat(2) {
            animatable.animateTo(Offset(-5f, -4f), BoardShakeTweenSpec)
            animatable.animateTo(Offset(5f, 4f), BoardShakeTweenSpec)
        }
        animatable.animateTo(Offset.Zero, BoardShakeTweenSpec)
    }

    /**
     * 执行卡牌向卡槽移入的飞行动画进度
     * 进度从 0f 动画到 1f。
     *
     * @param animatable 飞行进度 Float 的 Animatable 实例
     */
    suspend fun runTileFlyAnimation(animatable: Animatable<Float, *>) {
        animatable.animateTo(1f, FlyTweenSpec)
    }

    // ==========================================
    // 3. Composable 状态动画辅助函数 (State Animations)
    // ==========================================

    /**
     * 生成提示卡牌高亮时的呼吸缩放比例
     * 
     * @param isHighlighted 是否高亮
     */
    @Composable
    fun rememberPulseScale(isHighlighted: Boolean): State<Float> {
        if (!isHighlighted) return remember { mutableStateOf(1f) }
        
        val infiniteTransition = rememberInfiniteTransition(label = "pulse_scale_transition")
        return infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.08f,
            animationSpec = PulseRepeatableSpec,
            label = "pulseScale"
        )
    }

    /**
     * 生成提示卡牌高亮时的边框呼吸发光透明度
     *
     * @param isHighlighted 是否高亮
     */
    @Composable
    fun rememberHighlightBorderAlpha(isHighlighted: Boolean): State<Float> {
        if (!isHighlighted) return remember { mutableStateOf(0f) }

        val infiniteTransition = rememberInfiniteTransition(label = "highlight_border_transition")
        return infiniteTransition.animateFloat(
            initialValue = 0.5f,
            targetValue = 1f,
            animationSpec = PulseRepeatableSpec,
            label = "borderAlpha"
        )
    }

    /**
     * 生成卡牌按压时的弹性收缩缩放比例
     *
     * @param interactionSource 交互事件源
     * @param isBlocked 是否被压住（被压住时点击不触发缩小反馈）
     */
    @Composable
    fun rememberPressScale(
        interactionSource: InteractionSource,
        isBlocked: Boolean
    ): State<Float> {
        val isPressed by interactionSource.collectIsPressedAsState()
        return animateFloatAsState(
            targetValue = if (isPressed && !isBlocked) 0.88f else 1f,
            animationSpec = PressSpringSpec,
            label = "pressScale"
        )
    }
}
