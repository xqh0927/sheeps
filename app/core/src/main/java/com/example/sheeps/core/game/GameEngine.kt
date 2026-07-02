package com.example.sheeps.core.game

import com.example.sheeps.data.model.Tile
import com.example.sheeps.data.model.TileState
import kotlin.math.abs

/**
 * 游戏核心引擎，处理卡牌间的遮挡逻辑。
 * 提供静态算法用于计算卡牌是否被压住、获取压住某张牌的所有卡牌、以及批量刷新棋盘卡牌状态。
 */
object GameEngine {

    /**
     * 判定一张卡牌（Tile）是否被其它处于更高层级（Z轴）且位置重叠的卡牌遮挡（锁定）。
     *
     * @param tile 需要检查的卡牌
     * @param board 当前棋盘上所有存在的卡牌列表
     * @return 如果被遮挡返回 true，否则返回 false
     */
    fun isTileBlocked(tile: Tile, board: List<Tile>): Boolean {
        return board.any { other ->
            if (other.id == tile.id ||
                (other.state != TileState.NORMAL && other.state != TileState.BLOCKED) ||
                other.z <= tile.z
            ) {
                false
            } else {
                // 计算重叠面积：根据卡牌坐标差 (dx, dy) 算出水平和垂直的重叠长度
                val dx = abs(other.x - tile.x)
                val dy = abs(other.y - tile.y)
                // 这里假设标准卡牌大小及重叠规则
                val ox = (48.0f - dx * 46.0f).coerceAtLeast(0f)
                val oy = (48.0f - dy * 46.0f).coerceAtLeast(0f)
                // 只要重叠面积超过阈值 230.4 (即大约半张牌的面积)，即判定为被遮挡
                ox * oy > 230.4f
            }
        }
    }

    /**
     * 获取所有遮挡住（压住）指定卡牌的卡牌列表。
     *
     * @param tile 指定的卡牌
     * @param board 当前棋盘卡牌列表
     * @return 正在遮挡该卡牌的卡牌集合
     */
    fun getBlockingTiles(tile: Tile, board: List<Tile>): List<Tile> {
        return board.filter { other ->
            // 过滤条件：排除自身、非正常状态的牌、或者在当前牌下方(Z轴更小)的牌
            if (other.id == tile.id ||
                (other.state != TileState.NORMAL && other.state != TileState.BLOCKED) ||
                other.z <= tile.z
            ) {
                false
            } else {
                // 重叠检测算法同上
                val dx = abs(other.x - tile.x)
                val dy = abs(other.y - tile.y)
                // 计算重叠面积
                val ox = (48.0f - dx * 46.0f).coerceAtLeast(0f)
                val oy = (48.0f - dy * 46.0f).coerceAtLeast(0f)
                // 判定遮挡
                ox * oy > 230.4f
            }
        }
    }

    /**
     * 批量计算并更新棋盘上所有卡牌的遮挡状态（TileState.BLOCKED 或 TileState.NORMAL）。
     * 通常在玩家点击取走一张牌后调用，以刷新下方卡牌的可点击性。
     *
     * @param board 原始卡牌列表
     * @return 更新状态后的新卡牌列表副本
     */
    fun calculateBlockedStates(board: List<Tile>): List<Tile> {
        return board.map { tile ->
            // 只有处于 NORMAL 或 BLOCKED 状态的牌才需要重新判断状态
            if (tile.state == TileState.NORMAL || tile.state == TileState.BLOCKED) {
                val blocked = isTileBlocked(tile, board)
                // 如果当前牌被遮挡，状态标记为 BLOCKED，否则设为 NORMAL
                tile.copy(state = if (blocked) TileState.BLOCKED else TileState.NORMAL)
            } else {
                // 已被消除或其它状态的牌保持原状
                tile
            }
        }
    }
}


//package com.example.sheeps.core.game
//
//import com.example.sheeps.data.model.Tile
//import com.example.sheeps.data.model.TileState
//import kotlin.math.abs
//import kotlin.math.floor
//
///**
// * 游戏核心引擎：遮挡逻辑与空间查询处理
// *
// * 该引擎负责实时计算卡牌之间的层级遮挡关系，确保只有处于最上层且未被其他卡牌压盖的卡牌才能被交互。
// * 采用了“空间哈希网格”优化，将复杂遮挡查询的复杂度从 O(N^2) 降至近乎 O(N)。
// */
//object GameEngine {
//
//    // 卡牌的基础尺寸参数
//    private const val TILE = 48f
//    private const val SPACING = 46f
//
//    // 【碰撞判定容差】
//    // 遮挡判定阈值：单位为逻辑像素。
//    // 为了防止因浮点数精度导致的边界漏判或边缘“粘连”，设置此阈值。
//    // 只要重叠长度超过此值，即判定为物理上的遮挡。
//    private const val OVERLAP_MARGIN = 0.01f
//
//    // ---- 单点查询接口 ----
//
//    /**
//     * 判断指定的卡牌 [tile] 是否被当前棋盘 [board] 上的其他卡牌遮挡。
//     * @return true 表示被遮挡（不可点击），false 表示处于顶层（可点击）。
//     */
//    fun isTileBlocked(tile: Tile, board: List<Tile>): Boolean {
//        for (o in board) {
//            // 忽略层级低于或等于自身的卡牌，只查找压在上面的卡牌
//            if (o.z <= tile.z) continue
//            // 只考虑正常状态或已被锁定状态的卡牌，已消除的卡牌忽略
//            if (o.state != TileState.NORMAL && o.state != TileState.BLOCKED) continue
//            // 执行物理遮挡碰撞检测
//            if (blocking(tile, o)) return true
//        }
//        return false
//    }
//
//    /**
//     * 获取所有遮挡住目标卡牌 [tile] 的卡牌列表。
//     * 常用于调试或实现“显示遮挡来源”的辅助功能。
//     */
//    fun getBlockingTiles(tile: Tile, board: List<Tile>): List<Tile> {
//        val result = ArrayList<Tile>()
//        for (o in board) {
//            if (o.z <= tile.z) continue
//            if (o.state != TileState.NORMAL && o.state != TileState.BLOCKED) continue
//            if (blocking(tile, o)) result.add(o)
//        }
//        return result
//    }
//
//    // ---- 批量状态计算 ----
//
//    /**
//     * 全局批量计算棋盘上所有卡牌的遮挡状态。
//     * 在关卡初始化或执行消除操作后调用此方法，更新棋盘逻辑状态。
//     */
//    fun calculateBlockedStates(board: List<Tile>): List<Tile> {
//        // 构建空间网格，避免全量双重循环，极大提升性能
//        val grid = buildGrid(board)
//        return board.map { tile ->
//            // 已被消除的卡牌保持状态不变
//            if (tile.state != TileState.NORMAL && tile.state != TileState.BLOCKED) tile
//            else {
//                // 重新判定该卡牌状态
//                val blocked = isBlockedGrid(tile, grid)
//                tile.copy(state = if (blocked) TileState.BLOCKED else TileState.NORMAL)
//            }
//        }
//    }
//
//    // ---- 内部计算逻辑 ----
//
//    /**
//     * 核心碰撞判定函数 (AABB 碰撞检测)。
//     * 比较两张卡牌在逻辑空间下的重叠情况。
//     *
//     * 逻辑：如果两张牌在 X 轴和 Y 轴的重叠长度均超过了容差值 [OVERLAP_MARGIN]，
//     * 则判定为存在遮挡关系。
//     */
//    private fun blocking(a: Tile, b: Tile): Boolean {
//        if (b.z <= a.z) return false
//
//        // 计算重叠的宽度和高度
//        val ox = TILE - abs(b.x - a.x) * SPACING
//        val oy = TILE - abs(b.y - a.y) * SPACING
//
//        // 只有当两个维度都发生深度重叠时才返回 true
//        return ox > OVERLAP_MARGIN && oy > OVERLAP_MARGIN
//    }
//
//    /**
//     * 构建空间网格映射 (Spatial Hashing)。
//     * 将棋盘上的卡牌按照逻辑坐标 (x, y) 映射进一个二维的桶结构 (Map<X, Map<Y, List>>)。
//     * 这样在查找遮挡时，只需检索周围邻近的桶。
//     */
//    private fun buildGrid(board: List<Tile>): Map<Int, Map<Int, List<Tile>>> {
//        val map = mutableMapOf<Int, MutableMap<Int, MutableList<Tile>>>()
//        for (t in board) {
//            // 使用 floor 向下取整，确保负坐标和边缘坐标能正确映射到桶中
//            val bx = floor(t.x).toInt()
//            val by = floor(t.y).toInt()
//
//            map.getOrPut(bx) { mutableMapOf() }
//                .getOrPut(by) { mutableListOf() }
//                .add(t)
//        }
//        return map
//    }
//
//    /**
//     * 在空间网格中进行高效遮挡检索。
//     * 检索范围设定为目标桶周围的 5x5 网格区域，确保覆盖所有可能的物理交集对象。
//     */
//    private fun isBlockedGrid(
//        tile: Tile,
//        grid: Map<Int, Map<Int, List<Tile>>>
//    ): Boolean {
//        val bx = floor(tile.x).toInt()
//        val by = floor(tile.y).toInt()
//
//        // 在周围 5x5 的网格区间内遍历，避免遗漏边缘碰撞
//        for (dx in -2..2) {
//            val col = grid[bx + dx] ?: continue
//            for (dy in -2..2) {
//                val bucket = col[by + dy] ?: continue
//                for (o in bucket) {
//                    // 跳过自身、层级较低或已处理状态的卡牌
//                    if (o.id == tile.id) continue
//                    if (o.z <= tile.z) continue
//                    if (o.state != TileState.NORMAL && o.state != TileState.BLOCKED) continue
//
//                    // 进行最终的重叠判断
//                    if (blocking(tile, o)) return true
//                }
//            }
//        }
//        return false
//    }
//}