package com.example.sheeps.core.multiplayer

import kotlin.math.ln

/**
 * 联机消消乐攻击力转换与操作流控计算器
 */
object AttackCalculator {
    /**
     * 非线性攻击力转换公式: Attack = Base * (1 + ln(Combo))
     * 对数增长曲线防止秒杀，鼓励连击
     * 
     * @param combo 当前连击数
     * @param base 基础攻击力
     * @return 经过连击加成后的最终攻击力
     */
    fun calculateAttackPower(combo: Int, base: Double = 10.0): Double {
        if (combo <= 1) return base
        // 使用自然对数函数 ln(combo) 使攻击力随连击数呈对数级增长，避免攻击力过快爆炸
        return base * (1.0 + ln(combo.toDouble()))
    }

    /**
     * 滑动窗口流控限流器，防止客户端通过外挂或修改器恶意高频刷包攻击
     */
    class RateLimiter(private val minIntervalMs: Long = 100) {
        private var lastActionTime: Long = 0L // 记录上一次合法操作的时间戳

        /**
         * 校验当前操作是否被允许
         * 
         * @param currentTimeMs 当前的操作时间戳
         * @return true 表示操作间隔达标允许执行；false 表示操作过于频繁，触发限流
         */
        @Synchronized // 保证并发环境下的原子性，避免重复记录或越权操作
        fun isActionAllowed(currentTimeMs: Long): Boolean {
            if (currentTimeMs - lastActionTime >= minIntervalMs) {
                lastActionTime = currentTimeMs // 更新上一次合法操作时间
                return true
            }
            return false
        }
    }
}
