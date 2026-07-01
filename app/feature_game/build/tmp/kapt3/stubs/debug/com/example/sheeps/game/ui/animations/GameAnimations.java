package com.example.sheeps.game.ui.animations;

import androidx.compose.animation.core.*;
import androidx.compose.foundation.interaction.InteractionSource;
import androidx.compose.runtime.*;
import androidx.compose.ui.unit.Dp;

/**
 * 羊了个羊 游戏动画工具类 (GameAnimations)
 * 统一管理和复用游戏中棋盘震动、卡牌飞行、卡牌被锁抖动、高亮呼吸、点击按压反馈等所有动效。
 */
@kotlin.Metadata(mv = {2, 2, 0}, k = 1, xi = 48, d1 = {"\u0000T\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\u0010\u0007\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001B\t\b\u0002\u00a2\u0006\u0004\b\u0002\u0010\u0003J \u0010\u0016\u001a\u00020\u00172\u0010\u0010\u0018\u001a\f\u0012\u0004\u0012\u00020\u0006\u0012\u0002\b\u00030\u0019H\u0086@\u00a2\u0006\u0002\u0010\u001aJ \u0010\u001b\u001a\u00020\u00172\u0010\u0010\u0018\u001a\f\u0012\u0004\u0012\u00020\f\u0012\u0002\b\u00030\u0019H\u0086@\u00a2\u0006\u0002\u0010\u001aJ \u0010\u001c\u001a\u00020\u00172\u0010\u0010\u0018\u001a\f\u0012\u0004\u0012\u00020\u0006\u0012\u0002\b\u00030\u0019H\u0086@\u00a2\u0006\u0002\u0010\u001aJ\u0016\u0010\u001d\u001a\b\u0012\u0004\u0012\u00020\u00060\u001e2\u0006\u0010\u001f\u001a\u00020 H\u0007J\u0016\u0010!\u001a\b\u0012\u0004\u0012\u00020\u00060\u001e2\u0006\u0010\u001f\u001a\u00020 H\u0007J\u001e\u0010\"\u001a\b\u0012\u0004\u0012\u00020\u00060\u001e2\u0006\u0010#\u001a\u00020$2\u0006\u0010%\u001a\u00020 H\u0007R\u0017\u0010\u0004\u001a\b\u0012\u0004\u0012\u00020\u00060\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0007\u0010\bR\u0017\u0010\t\u001a\b\u0012\u0004\u0012\u00020\u00060\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\n\u0010\bR\u0017\u0010\u000b\u001a\b\u0012\u0004\u0012\u00020\f0\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\r\u0010\bR\u0017\u0010\u000e\u001a\b\u0012\u0004\u0012\u00020\u00060\u000f\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0010\u0010\u0011R\u0017\u0010\u0012\u001a\b\u0012\u0004\u0012\u00020\u00060\u0013\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0014\u0010\u0015\u00a8\u0006&"}, d2 = {"Lcom/example/sheeps/game/ui/animations/GameAnimations;", "", "<init>", "()V", "FlyTweenSpec", "Landroidx/compose/animation/core/TweenSpec;", "", "getFlyTweenSpec", "()Landroidx/compose/animation/core/TweenSpec;", "ShakeTweenSpec", "getShakeTweenSpec", "BoardShakeTweenSpec", "Landroidx/compose/ui/geometry/Offset;", "getBoardShakeTweenSpec", "PulseRepeatableSpec", "Landroidx/compose/animation/core/InfiniteRepeatableSpec;", "getPulseRepeatableSpec", "()Landroidx/compose/animation/core/InfiniteRepeatableSpec;", "PressSpringSpec", "Landroidx/compose/animation/core/SpringSpec;", "getPressSpringSpec", "()Landroidx/compose/animation/core/SpringSpec;", "runTileShakeAnimation", "", "animatable", "Landroidx/compose/animation/core/Animatable;", "(Landroidx/compose/animation/core/Animatable;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "runBoardShakeAnimation", "runTileFlyAnimation", "rememberPulseScale", "Landroidx/compose/runtime/State;", "isHighlighted", "", "rememberHighlightBorderAlpha", "rememberPressScale", "interactionSource", "Landroidx/compose/foundation/interaction/InteractionSource;", "isBlocked", "feature_game_debug"})
public final class GameAnimations {
    
    /**
     * 卡牌飞行动画的插值 Specs (350ms, 缓入缓出)
     */
    @org.jetbrains.annotations.NotNull()
    private static final androidx.compose.animation.core.TweenSpec<java.lang.Float> FlyTweenSpec = null;
    
    /**
     * 抖动动画的插值 Specs (50ms, 线性)
     */
    @org.jetbrains.annotations.NotNull()
    private static final androidx.compose.animation.core.TweenSpec<java.lang.Float> ShakeTweenSpec = null;
    
    /**
     * 棋盘震动动画的插值 Specs (45ms, 线性)
     */
    @org.jetbrains.annotations.NotNull()
    private static final androidx.compose.animation.core.TweenSpec<androidx.compose.ui.geometry.Offset> BoardShakeTweenSpec = null;
    
    /**
     * 呼吸灯发光/缩放动画的循环配置 (600ms, 双向往返)
     */
    @org.jetbrains.annotations.NotNull()
    private static final androidx.compose.animation.core.InfiniteRepeatableSpec<java.lang.Float> PulseRepeatableSpec = null;
    
    /**
     * 卡牌点击缩小的弹簧配置 (有弹性，中等阻尼)
     */
    @org.jetbrains.annotations.NotNull()
    private static final androidx.compose.animation.core.SpringSpec<java.lang.Float> PressSpringSpec = null;
    @org.jetbrains.annotations.NotNull()
    public static final com.example.sheeps.game.ui.animations.GameAnimations INSTANCE = null;
    
    private GameAnimations() {
        super();
    }
    
    /**
     * 卡牌飞行动画的插值 Specs (350ms, 缓入缓出)
     */
    @org.jetbrains.annotations.NotNull()
    public final androidx.compose.animation.core.TweenSpec<java.lang.Float> getFlyTweenSpec() {
        return null;
    }
    
    /**
     * 抖动动画的插值 Specs (50ms, 线性)
     */
    @org.jetbrains.annotations.NotNull()
    public final androidx.compose.animation.core.TweenSpec<java.lang.Float> getShakeTweenSpec() {
        return null;
    }
    
    /**
     * 棋盘震动动画的插值 Specs (45ms, 线性)
     */
    @org.jetbrains.annotations.NotNull()
    public final androidx.compose.animation.core.TweenSpec<androidx.compose.ui.geometry.Offset> getBoardShakeTweenSpec() {
        return null;
    }
    
    /**
     * 呼吸灯发光/缩放动画的循环配置 (600ms, 双向往返)
     */
    @org.jetbrains.annotations.NotNull()
    public final androidx.compose.animation.core.InfiniteRepeatableSpec<java.lang.Float> getPulseRepeatableSpec() {
        return null;
    }
    
    /**
     * 卡牌点击缩小的弹簧配置 (有弹性，中等阻尼)
     */
    @org.jetbrains.annotations.NotNull()
    public final androidx.compose.animation.core.SpringSpec<java.lang.Float> getPressSpringSpec() {
        return null;
    }
    
    /**
     * 触发卡牌被锁定的单次水平抖动动画
     * 往复移动 1 次，最后归零。
     *
     * @param animatable 卡牌水平偏移的 Animatable 实例
     */
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object runTileShakeAnimation(@org.jetbrains.annotations.NotNull()
    androidx.compose.animation.core.Animatable<java.lang.Float, ?> animatable, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    /**
     * 触发使用炸弹道具时的棋盘震动动画
     * 往复摇晃 2 次，最后归零复位。
     *
     * @param animatable 棋盘偏移量 Offset 的 Animatable 实例
     */
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object runBoardShakeAnimation(@org.jetbrains.annotations.NotNull()
    androidx.compose.animation.core.Animatable<androidx.compose.ui.geometry.Offset, ?> animatable, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    /**
     * 执行卡牌向卡槽移入的飞行动画进度
     * 进度从 0f 动画到 1f。
     *
     * @param animatable 飞行进度 Float 的 Animatable 实例
     */
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object runTileFlyAnimation(@org.jetbrains.annotations.NotNull()
    androidx.compose.animation.core.Animatable<java.lang.Float, ?> animatable, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    /**
     * 生成提示卡牌高亮时的呼吸缩放比例
     *
     * @param isHighlighted 是否高亮
     */
    @androidx.compose.runtime.Composable()
    @org.jetbrains.annotations.NotNull()
    public final androidx.compose.runtime.State<java.lang.Float> rememberPulseScale(boolean isHighlighted) {
        return null;
    }
    
    /**
     * 生成提示卡牌高亮时的边框呼吸发光透明度
     *
     * @param isHighlighted 是否高亮
     */
    @androidx.compose.runtime.Composable()
    @org.jetbrains.annotations.NotNull()
    public final androidx.compose.runtime.State<java.lang.Float> rememberHighlightBorderAlpha(boolean isHighlighted) {
        return null;
    }
    
    /**
     * 生成卡牌按压时的弹性收缩缩放比例
     *
     * @param interactionSource 交互事件源
     * @param isBlocked 是否被压住（被压住时点击不触发缩小反馈）
     */
    @androidx.compose.runtime.Composable()
    @org.jetbrains.annotations.NotNull()
    public final androidx.compose.runtime.State<java.lang.Float> rememberPressScale(@org.jetbrains.annotations.NotNull()
    androidx.compose.foundation.interaction.InteractionSource interactionSource, boolean isBlocked) {
        return null;
    }
}