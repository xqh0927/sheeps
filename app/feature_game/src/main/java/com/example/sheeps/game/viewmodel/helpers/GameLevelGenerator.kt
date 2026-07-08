package com.example.sheeps.game.viewmodel.helpers

import com.example.sheeps.core.game.SkinConstants
import com.example.sheeps.data.model.Tile
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * 本地关卡生成器
 * 用于在无网络情况下生成可解的关卡数据。
 *
 * 卡牌数量基于难度系数曲线（与服务端 server/src/difficulty.ts 完全对齐）：
 * - L1=12, L2=48, L3=60（固定）
 * - L4+ 使用幂函数公式 + 1~2 关随机 + 休息关 80% 折扣
 * - 本地模式 userId=0 作为难度种子
 * - @param seed LCG 随机种子，控制卡牌图案与布局，不同 seed 产生不同排列
 */
class GameLevelGenerator @Inject constructor() {

    /**
     * 生成一个保证有解的本地关卡
     * 采用反向生成算法：先生成布局，然后反向填充成对的卡牌类型
     * @param seed LCG 随机种子，默认使用 levelId 保持向后兼容
     */
    fun generateSolvableLevelLocal(levelId: Int, seed: Long = levelId * 1000L, userId: Int = 0): List<Tile> {
        // 根据关卡ID计算难度（卡牌种类数），受限于美术资源总量
        val numTypes = if (levelId == 1) 3 else minOf(
            SkinConstants.MAX_TILE_TYPES,
            (3 + 3 * Math.log(levelId.toDouble())).toInt()
        )

        // 使用难度系数系统计算卡牌总数
        val maxCards = calculateCardCount(userId, levelId)

        val coordinates = mutableListOf<Point3D>()
        if (levelId == 1) {
            // 第一关采用固定简单的布局（12张牌）
            coordinates.addAll(
                listOf(
                    Point3D(0.0f, 0.0f, 0),
                    Point3D(0.0f, 2.5f, 0),
                    Point3D(2.5f, 0.0f, 0),
                    Point3D(2.5f, 2.5f, 0),

                    Point3D(0.4f, 0.4f, 1),
                    Point3D(0.4f, 2.9f, 1),
                    Point3D(2.9f, 0.4f, 1),
                    Point3D(2.9f, 2.9f, 1),

                    Point3D(0.8f, 0.8f, 2),
                    Point3D(0.8f, 3.3f, 2),
                    Point3D(3.3f, 0.8f, 2),
                    Point3D(3.3f, 3.3f, 2)
                )
            )
        } else {
            // 后续关卡根据算法生成堆叠布局
            // 与后端 server/src/level.ts 完全对齐：maxCards 来自难度曲线、baseSize 按 levelId/2 增长
            val possible = mutableListOf<Point3D>()
            val layersCount = minOf(12, (12 - 8 / sqrt((levelId - 1).toDouble())).toInt())

            val baseSize = minOf(20, 6 + levelId / 2)
            for (z in 0 until layersCount) {
                val size = maxOf(3, baseSize - z / 3)
                val colSize = minOf(6, size)
                val rowSize = minOf(7, size)
                val offset = if (z % 2 == 0) 0f else 0.5f
                for (r in 0 until rowSize) {
                    for (c in 0 until colSize) {
                        // 本地 fallback：使用正方形 shape（与后端 shape=0 一致）
                        // 后端默认从 18 种异形中随机选一种，本地仅 fallback 时使用正方形保证可解
                        possible.add(Point3D(c + offset + 1.0f, r + offset + 1.0f, z))
                    }
                }
            }

            // 引入种子影响布局，相同 levelId 不同 seed 产生不同排列
            val layoutSeed = seed + levelId * 1000L
            val rand = lcg(layoutSeed)
            for (i in possible.indices.reversed()) {
                val j = (rand() * (i + 1)).toInt()
                val temp = possible[i]
                possible[i] = possible[j]
                possible[j] = temp
            }

            // 确保卡牌总数是3的倍数
            val count = minOf(possible.size, maxCards) - (minOf(possible.size, maxCards) % 3)
            coordinates.addAll(possible.take(count))
        }

        coordinates.sortBy { it.z }

        val nodes = coordinates.mapIndexed { index, coord ->
            LocalNode(index, coord, -1)
        }

        val unassigned = nodes.toMutableSet()
        val randAssign = lcg(seed + 100)

        // 25% 覆盖面积遮挡算法：累计所有更高层卡牌的覆盖面积
        val overlapArea = { a: Point3D, b: Point3D ->
            if (a.z <= b.z) {
                0f
            } else {
                val dx = abs(a.x - b.x)
                val dy = abs(a.y - b.y)
                val ox = (48.0f - dx * 46.0f).coerceAtLeast(0f)
                val oy = (48.0f - dy * 46.0f).coerceAtLeast(0f)
                ox * oy
            }
        }

        // 反向分配卡牌类型，确保顶层总是存在可消除的组合
        while (unassigned.isNotEmpty()) {
            // 覆盖面积累计：累计所有更高层卡牌的覆盖面积，超过 25% 即视为遮挡
            val exposed = unassigned.filter { node ->
                val covered = unassigned
                    .filter { other -> other != node && other.coord.z > node.coord.z }
                    .sumOf { other -> overlapArea(other.coord, node.coord).toDouble() }
                    .toFloat()
                covered < 48.0f * 48.0f * 0.01f
            }

            if (exposed.size < 3) {
                // 剩余不足三张时强制分配
                val rem = unassigned.toList()
                var k = 0
                while (k + 2 < rem.size) {
                    val t = (randAssign() * numTypes).toInt() + 1
                    rem[k].assignedType = t
                    rem[k + 1].assignedType = t
                    rem[k + 2].assignedType = t
                    unassigned.remove(rem[k])
                    unassigned.remove(rem[k + 1])
                    unassigned.remove(rem[k + 2])
                    k += 3
                }
                for (node in unassigned) {
                    node.assignedType = 1
                }
                unassigned.clear()
                break
            }

            val type = (randAssign() * numTypes).toInt() + 1
            val exposedMutable = exposed.toMutableList()
            for (k in 0 until 3) {
                val idx = (randAssign() * exposedMutable.size).toInt()
                val chosen = exposedMutable.removeAt(idx)
                chosen.assignedType = type
                unassigned.remove(chosen)
            }
        }

        // ===== 坐标归一化（blocks() 已用原始坐标跑完，此时坐标系可安全压缩）=====
        // 与后端 server/src/level.ts 完全对齐：质心居中到 (5.5, 5.5)
        val normCx: Float
        val normCy: Float
        val normScale: Float
        if (coordinates.isNotEmpty()) {
            val cnt = coordinates.size
            normCx = coordinates.sumOf { it.x.toDouble() }.toFloat() / cnt
            normCy = coordinates.sumOf { it.y.toDouble() }.toFloat() / cnt
            var maxR = 0f
            for (c in coordinates) {
                maxR = maxOf(maxR, maxOf(abs(c.x - normCx), abs(c.y - normCy)))
            }
            maxR = maxOf(maxR, 0.1f)
            normScale = 4.8f / maxR
        } else {
            normCx = 0f; normCy = 0f; normScale = 1.0f
        }

        // ===== 固定关卡类型规则（替代随机） =====
        // 休息关（levelId % 5 == 0，levelId >= 5）：卡牌数量打八折
        // 盲盒关（levelId % 3 == 0，levelId >= 3）：底层卡牌部分变盲盒
        // 封印关（levelId % 2 == 0）：每张卡有概率带封印
        // 优先级: 休息 > 盲盒 > 封印 > 普通
        val isRest = isRestLevel(levelId)
        val isBlindLevel = !isRest && levelId >= 3 && levelId % 3 == 0
        val isSealedLevel = !isRest && !isBlindLevel && levelId >= 2 && levelId % 2 == 0

        val maxZ = coordinates.maxOfOrNull { it.z } ?: 0
        // 盲盒关卡：均匀分布固定数量的盲盒牌
        val blindIndices = if (isBlindLevel) {
            val blindProb = minOf(0.20, 0.10 + (levelId - 3) * 0.015)
            val limitZ = if (maxZ >= 4) (maxZ - 2) else (maxZ - 1)
            val eligible = nodes.filter { it.coord.z < limitZ }
            val count = maxOf(1, (eligible.size * blindProb).toInt())
            // 对 eligible 名单洗牌后取前 count 个作为盲盒牌
            val indices = eligible.indices.toMutableList()
            val randShuffle = lcg(seed + 200)
            for (i in indices.indices.reversed()) {
                val j = (randShuffle() * (i + 1)).toInt()
                val tmp = indices[i]; indices[i] = indices[j]; indices[j] = tmp
            }
            indices.take(count).map { eligible[it].index }.toSet()
        } else {
            emptySet()
        }

        // 封印关卡：多层封印 + 聚簇分布
        val maxSealLayer = when {
            levelId <= 6 -> 1
            levelId <= 14 -> 2
            else -> 3
        }
        val sealRatio = when {
            levelId <= 6 -> 0.30
            levelId <= 14 -> 0.35
            else -> 0.40
        }
        val clusterCount = when {
            levelId <= 6 -> 2
            levelId <= 14 -> 3
            else -> 4
        }

        val randProps = lcg(seed + 300)
        val sealedClusters = if (isSealedLevel) {
            generateSealedUniformly(nodes, { randProps() }, sealRatio, maxSealLayer)
        } else {
            emptyMap()
        }

        return nodes.map { node ->
            val isBlind = node.index in blindIndices
            val sealed = sealedClusters[node.index] ?: 0

            Tile(
                id = "tile_${node.index}",
                x = node.coord.x,
                y = node.coord.y,
                z = node.coord.z,
                type = node.assignedType,
                isBlind = isBlind,
                sealedCount = sealed
            )
        }
    }

    /**
     * 生成均匀分布的封印牌
     *
     * @param nodes 所有卡牌节点
     * @param rand 随机数生成函数，返回 [0, 1)
     * @param sealRatio 封印卡牌占总卡牌的比例
     * @param maxLayer 最大封印层数
     * @return Map<nodeIndex, sealedCount>
     */
    private fun generateSealedUniformly(
        nodes: List<LocalNode>,
        rand: () -> Double,
        sealRatio: Double,
        maxLayer: Int
    ): Map<Int, Int> {
        if (nodes.isEmpty()) return emptyMap()

        val maxZ = nodes.maxOfOrNull { it.coord.z } ?: 0
        // 封印只生成在较低层（z <= 70% 最高层），保证玩家能优先看到
        val eligible = nodes.filter { it.coord.z <= maxZ * 0.7f }
            .takeIf { it.isNotEmpty() } ?: nodes.toList()

        val totalSealed = maxOf(1, (nodes.size * sealRatio).toInt())

        // 随机打乱备选节点
        val shuffled = eligible.toMutableList()
        for (i in shuffled.indices.reversed()) {
            val j = (rand() * (i + 1)).toInt()
            val tmp = shuffled[i]
            shuffled[i] = shuffled[j]
            shuffled[j] = tmp
        }

        val result = mutableMapOf<Int, Int>()
        for (node in shuffled.take(totalSealed)) {
            result[node.index] = randomSealLayer(rand, maxLayer)
        }

        return result
    }

    private fun randomSealLayer(rand: () -> Double, maxLayer: Int): Int = when (maxLayer) {
        1 -> 1
        2 -> if (rand() < 0.65) 1 else 2
        else -> when {
            rand() < 0.50 -> 1
            rand() < 0.85 -> 2
            else -> 3
        }
    }

    // ===== 难度系数系统（与服务端 server/src/difficulty.ts 完全对齐）=====

    /**
     * 根据用户 ID 和关卡 ID 获取难度信息。
     *
     * 规则:
     * - L1: D=1, subIndex=0, totalSubLevels=1
     * - L2: D=2, subIndex=0, totalSubLevels=1
     * - L3: D=3, subIndex=0, totalSubLevels=1
     * - L4+: 每个难度段覆盖 1~2 关，由 LCG(userId*9973 + D*10007) 决定
     */
    private fun getDifficultyForLevel(userId: Int, levelId: Int): DifficultyInfo {
        if (levelId == 1) return DifficultyInfo(1, 0, 1)
        if (levelId == 2) return DifficultyInfo(2, 0, 1)
        if (levelId == 3) return DifficultyInfo(3, 0, 1)

        var currentDifficulty = 3
        var levelCursor = 3

        while (levelCursor < levelId) {
            currentDifficulty++

            if (currentDifficulty >= 100) {
                val remaining = levelId - levelCursor
                return DifficultyInfo(100, remaining - 1, maxOf(1, remaining))
            }

            val rng = lcg(userId * 9973L + currentDifficulty * 10007L)
            val levelsForDifficulty = if (rng() < 0.5) 1 else 2

            val nextCursor = levelCursor + levelsForDifficulty
            if (nextCursor >= levelId) {
                val subIndex = levelId - levelCursor - 1
                return DifficultyInfo(currentDifficulty, subIndex, levelsForDifficulty)
            }

            levelCursor = nextCursor
        }

        return DifficultyInfo(100, 0, 1)
    }

    /**
     * 根据难度信息计算基础卡牌数量（不含休息关折扣）。
     *
     * 公式（D≥4）: rawCards(d) = 60 + 240 * ((d - 3) / 97) ^ 0.55
     */
    private fun getBaseCardCount(info: DifficultyInfo): Int {
        val d = info.difficulty

        if (d == 1) return 12
        if (d == 2) return 48
        if (d == 3) return 60
        if (d >= 100) return 300

        val raw = 60.0 + 240.0 * ((d - 3).toDouble() / 97.0).pow(0.55)
        val base = roundTo3(raw)

        if (info.totalSubLevels == 1) return base

        val rawNext = 60.0 + 240.0 * ((d + 1 - 3).toDouble() / 97.0).pow(0.55)
        val baseNext = roundTo3(rawNext)

        val delta = maxOf(3, ((baseNext - base).toDouble() / 2.0 / 3.0).let {
            Math.round(it).toInt()
        } * 3)

        return if (info.subIndex == 0) base else base + delta
    }

    /**
     * 判断是否为休息关卡。
     * L1-L4 无休息关；L5 起每 5 关（L5, L10, L15…）为休息关。
     */
    private fun isRestLevel(levelId: Int): Boolean {
        if (levelId < 5) return false
        return levelId % 5 == 0
    }

    /**
     * 综合计算最终卡牌数量。
     * 休息关折扣: 80%（减少 20%），四舍五入到 3 的倍数，限制在 [12, 300]。
     */
    private fun calculateCardCount(userId: Int, levelId: Int): Int {
        val info = getDifficultyForLevel(userId, levelId)
        val baseCards = getBaseCardCount(info)
        val isRest = isRestLevel(levelId)

        val cardCount = if (isRest) {
            roundTo3(baseCards.toDouble() * 0.80)
        } else {
            baseCards
        }

        return maxOf(12, minOf(300, cardCount))
    }

    /**
     * 四舍五入到最近的 3 的倍数。
     */
    private fun roundTo3(value: Double): Int {
        return (Math.round(value / 3.0) * 3).toInt()
    }

    // ===== 内部数据类 =====

    private data class Point3D(val x: Float, val y: Float, val z: Int)
    private class LocalNode(val index: Int, val coord: Point3D, var assignedType: Int)
    private data class DifficultyInfo(
        val difficulty: Int,
        val subIndex: Int,
        val totalSubLevels: Int
    )

    /**
     * 简单的线性同余生成器，用于生成确定性的随机序列。
     * 与服务端 server/src/level.ts 的 lcg 函数使用相同的乘数、加数和模数。
     */
    private fun lcg(seed: Long): () -> Double {
        var s = seed
        return {
            s = (s * 1664525L + 1013904223L) % 4294967296L
            s.toDouble() / 4294967296.0
        }
    }
}
