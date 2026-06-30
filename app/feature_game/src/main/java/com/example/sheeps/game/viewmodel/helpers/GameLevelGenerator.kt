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
        val numTypes = if (levelId == 1) 3 else minOf(16, (3 + 3 * Math.log(levelId.toDouble())).toInt())

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
                val size = maxOf(3, baseSize - z / 3)
                val offset = if (z % 2 == 0) 0.0f else 0.5f
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

        // 判断卡牌遮挡关系的闭包
        val blocks = { a: Point3D, b: Point3D ->
            a.z > b.z && abs(a.x - b.x) < 1.0f && abs(a.y - b.y) < 1.0f
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

        val randProps = lcg(levelId * 1000L + 200)
        return nodes.map { node ->
            var isBlind = false
            var sealed = 0

            // 高级关卡增加特殊属性（盲盒、封印等）
            if (levelId >= 2) {
                val r = randProps()
                if (levelId % 10 == 0) {
                    if (r < 0.15) {
                        isBlind = true
                    } else if (r < 0.30) {
                        sealed = 1
                    }
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
