package com.example.sheeps.core.game

import com.example.sheeps.core.game.GameEngine.OVERLAP_MARGIN
import com.example.sheeps.data.model.Tile
import com.example.sheeps.data.model.TileState
import kotlin.math.abs
import kotlin.math.floor

/**
 * 游戏核心引擎：遮挡逻辑与空间查询处理
 *
 * 该引擎负责实时计算卡牌之间的层级遮挡关系，确保只有处于最上层且未被其他卡牌压盖的卡牌才能被交互。
 * 采用了“空间哈希网格”优化，将复杂遮挡查询的复杂度从 O(N^2) 降至近乎 O(N)。
 */
object GameEngine {

    // 卡牌的基础尺寸参数
    private const val TILE = 48f
    private const val SPACING = 46f

    // 【碰撞判定容差】
    // 遮挡判定阈值：单位为逻辑像素。
    // 为了防止因浮点数精度导致的边界漏判或边缘“粘连”，设置此阈值。
    // 只要重叠长度超过此值，即判定为物理上的遮挡。
    private const val OVERLAP_MARGIN = 0.25f

    // ---- 单点查询接口 ----

    /**
     * 判断指定的卡牌 [tile] 是否被当前棋盘 [board] 上的其他卡牌遮挡。
     * @return true 表示被遮挡（不可点击），false 表示处于顶层（可点击）。
     */
    fun isTileBlocked(tile: Tile, board: List<Tile>): Boolean {
        for (o in board) {
            // 忽略层级低于或等于自身的卡牌，只查找压在上面的卡牌
            if (o.z <= tile.z) continue
            // 只考虑正常状态或已被锁定状态的卡牌，已消除的卡牌忽略
            if (o.state != TileState.NORMAL && o.state != TileState.BLOCKED) continue
            // 执行物理遮挡碰撞检测
            if (blocking(tile, o)) return true
        }
        return false
    }

    /**
     * 获取所有遮挡住目标卡牌 [tile] 的卡牌列表。
     * 常用于调试或实现“显示遮挡来源”的辅助功能。
     */
    fun getBlockingTiles(tile: Tile, board: List<Tile>): List<Tile> {
        val result = ArrayList<Tile>()
        for (o in board) {
            if (o.z <= tile.z) continue
            if (o.state != TileState.NORMAL && o.state != TileState.BLOCKED) continue
            if (blocking(tile, o)) result.add(o)
        }
        return result
    }

    // ---- 批量状态计算 ----

    /**
     * 全局批量计算棋盘上所有卡牌的遮挡状态。
     * 在关卡初始化或执行消除操作后调用此方法，更新棋盘逻辑状态。
     */
    fun calculateBlockedStates(board: List<Tile>): List<Tile> {
        // 构建空间网格，避免全量双重循环，极大提升性能
        val grid = buildGrid(board)
        return board.map { tile ->
            // 已被消除的卡牌保持状态不变
            if (tile.state != TileState.NORMAL && tile.state != TileState.BLOCKED) {
                tile
            } else {
                // 重新判定该卡牌状态
                val blocked = isBlockedGrid(tile, grid)
                tile.copy(state = if (blocked) TileState.BLOCKED else TileState.NORMAL)
            }
        }
    }

    // ---- 内部计算逻辑 ----

    /**
     * 核心碰撞判定函数 (AABB 碰撞检测)。
     * 比较两张卡牌在逻辑空间下的重叠情况。
     *
     * 逻辑：如果两张牌在 X 轴和 Y 轴的重叠长度均超过了容差值 [OVERLAP_MARGIN]，
     * 则判定为存在遮挡关系。
     */
    private fun blocking(a: Tile, b: Tile): Boolean {
        if (b.z <= a.z) return false

        // 计算重叠的宽度和高度
        val ox = TILE - abs(b.x - a.x) * SPACING
        val oy = TILE - abs(b.y - a.y) * SPACING

        // 只有当两个维度都发生深度重叠时才返回 true
        return ox > OVERLAP_MARGIN && oy > OVERLAP_MARGIN
    }

    /**
     * 构建空间网格映射 (Spatial Hashing)。
     * 将棋盘上的卡牌按照逻辑坐标 (x, y) 映射进一个二维的桶结构 (Map<X, Map<Y, List>>)。
     * 这样在查找遮挡时，只需检索周围邻近的桶。
     */
    private fun buildGrid(board: List<Tile>): Map<Int, Map<Int, List<Tile>>> {
        val map = mutableMapOf<Int, MutableMap<Int, MutableList<Tile>>>()
        for (t in board) {
            // 使用 floor 向下取整，确保负坐标和边缘坐标能正确映射到桶中
            val bx = floor(t.x).toInt()
            val by = floor(t.y).toInt()

            map.getOrPut(bx) { mutableMapOf() }
                .getOrPut(by) { mutableListOf() }
                .add(t)
        }
        return map
    }

    /**
     * 在空间网格中进行高效遮挡检索。
     * 检索范围设定为目标桶周围的 5x5 网格区域，确保覆盖所有可能的物理交集对象。
     */
    private fun isBlockedGrid(
        tile: Tile,
        grid: Map<Int, Map<Int, List<Tile>>>
    ): Boolean {
        val bx = floor(tile.x).toInt()
        val by = floor(tile.y).toInt()

        // 在周围 5x5 的网格区间内遍历，避免遗漏边缘碰撞
        for (dx in -2..2) {
            val col = grid[bx + dx] ?: continue
            for (dy in -2..2) {
                val bucket = col[by + dy] ?: continue
                for (o in bucket) {
                    // 跳过自身、层级较低或已处理状态的卡牌
                    if (o.id == tile.id) continue
                    if (o.z <= tile.z) continue
                    if (o.state != TileState.NORMAL && o.state != TileState.BLOCKED) continue

                    // 进行最终的重叠判断
                    if (blocking(tile, o)) return true
                }
            }
        }
        return false
    }
}