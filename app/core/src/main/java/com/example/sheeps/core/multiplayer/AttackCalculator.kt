package com.example.sheeps.core.multiplayer

import kotlin.math.ln

/**
 * 联机消消乐攻击力转换与操作流控计算器
 */
object AttackCalculator {
    /**
     * 非线性攻击力转换公式: Attack = Base * (1 + ln(Combo))
     * 对数增长曲线防止秒杀，鼓励连击
     */
    fun calculateAttackPower(combo: Int, base: Double = 10.0): Double {
        if (combo <= 1) return base
        return base * (1.0 + ln(combo.toDouble()))
    }

    /**
     * 滑动窗口流控限流器，防止客户端通过外挂或修改器恶意高频刷包攻击
     */
    class RateLimiter(private val minIntervalMs: Long = 100) {
        private var lastActionTime: Long = 0L

        /**
         * 校验当前操作是否被允许（必须距离上一次操作至少间隔 minIntervalMs）
         */
        @Synchronized
        fun isActionAllowed(currentTimeMs: Long): Boolean {
            if (currentTimeMs - lastActionTime >= minIntervalMs) {
                lastActionTime = currentTimeMs
                return true
            }
            return false
        }
    }
}
