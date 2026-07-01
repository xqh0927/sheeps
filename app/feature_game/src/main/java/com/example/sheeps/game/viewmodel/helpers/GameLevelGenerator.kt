package com.example.sheeps.game.viewmodel.helpers

import com.example.sheeps.data.model.Tile
import javax.inject.Inject
import kotlin.math.abs

/**
 * 本地关卡生成器
 * 用于在无网络情况下生成可解的关卡数据
 */
class GameLevelGenerator @Inject constructor() {

    /**
     * 生成一个保证有解的本地关卡
     * 采用反向生成算法：先生成布局，然后反向填充成对的卡牌类型
     */
    fun generateSolvableLevelLocal(levelId: Int): List<Tile> {
        // 根据关卡ID计算难度（卡牌种类数）
        val numTypes = if (levelId == 1) 3 else minOf(12, (3 + 3 * Math.log(levelId.toDouble())).toInt())

        val coordinates = mutableListOf<Point3D>()
        if (levelId == 1) {
            // 第一关采用固定简单的布局
            coordinates.addAll(listOf(
                Point3D(1.0f, 1.0f, 0), Point3D(2.0f, 1.0f, 0),
                Point3D(1.0f, 2.0f, 0), Point3D(2.0f, 2.0f, 0),
                Point3D(1.5f, 1.5f, 1), Point3D(2.5f, 1.5f, 1),
                Point3D(1.5f, 2.5f, 1), Point3D(2.5f, 2.5f, 1),
                Point3D(2.0f, 2.0f, 2)
            ))
        } else {
            // 后续关卡根据算法生成堆叠布局
            val maxCards = if (levelId == 2) 36 else 36 + (levelId - 2) * 12
            val possible = mutableListOf<Point3D>()
            val layersCount = if (levelId == 2) 4 else minOf(12, (12 - 8 / Math.sqrt((levelId - 1).toDouble())).toInt())

            val baseSize = 6 + levelId / 20
            for (z in 0 until layersCount) {
                val size = baseSize - z
                if (size < 2) break
                val offset = z * 0.5f
                for (r in 0 until size) {
                    for (c in 0 until size) {
                        possible.add(Point3D(c + offset + 1.0f, r + offset + 1.0f, z))
                    }
                }
            }

            // 洗牌随机化位置
            val rand = lcg(levelId * 1000L)
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
        val randAssign = lcg(levelId * 1000L + 100)

        // 判断卡牌遮挡关系的闭包，与运行时物理重叠规则完全对齐以保证可解性（重叠面积大于单张卡牌10%）
        val blocks = { a: Point3D, b: Point3D ->
            if (a.z <= b.z) {
                false
            } else {
                val dx = abs(a.x - b.x)
                val dy = abs(a.y - b.y)
                val ox = (48.0f - dx * 46.0f).coerceAtLeast(0f)
                val oy = (48.0f - dy * 46.0f).coerceAtLeast(0f)
                ox * oy > 230.4f
            }
        }

        // 反向分配卡牌类型，确保顶层总是存在可消除的组合
        while (unassigned.isNotEmpty()) {
            val exposed = unassigned.filter { node ->
                unassigned.none { other -> other != node && blocks(other.coord, node.coord) }
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

        // 关卡类型全局概率分布计算 (20% 盲盒, 40% 封印, 40% 正常)
        val randType = lcg(levelId * 1000L + 500)
        val typeRoll = randType()
        // 只有 levelId >= 3 才能生成盲盒关卡，概率为 20%
        val isBlindLevel = levelId >= 3 && typeRoll < 0.20
        // 关卡为 Level 2 及以上时，40% 的概率为封印关卡 (即 0.20 <= typeRoll < 0.60)
        val isSealedLevel = levelId >= 2 && typeRoll >= 0.20 && typeRoll < 0.60

        val randProps = lcg(levelId * 1000L + 200)
        val maxZ = coordinates.maxOfOrNull { it.z } ?: 0
        return nodes.map { node ->
            var isBlind = false
            var sealed = 0

            val r = randProps()
            if (isBlindLevel) {
                val blindProb = minOf(0.20, 0.10 + (levelId - 3) * 0.015)
                val limitZ = if (maxZ >= 4) (maxZ - 2) else (maxZ - 1)
                if (r < blindProb && node.coord.z < limitZ) {
                    isBlind = true
                }
            } else if (isSealedLevel) {
                if (r < 0.30) {
                    sealed = 1
                }
            }

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

    private data class Point3D(val x: Float, val y: Float, val z: Int)
    private data class LocalNode(val index: Int, val coord: Point3D, var assignedType: Int)

    /**
     * 简单的线性同余生成器，用于生成确定性的随机序列
     */
    private fun lcg(seed: Long): () -> Double {
        var s = seed
        return {
            s = (s * 1664525L + 1013904223L) % 4294967296L
            s.toDouble() / 4294967296.0
        }
    }
}
