package com.example.sheeps.core.game

import com.example.sheeps.data.model.Tile
import com.example.sheeps.data.model.TileState
import kotlin.math.abs
import kotlin.math.floor

/**
 * 游戏棋盘引擎（无状态纯函数集合）。
 *
 * 负责麻将连连看类玩法中瓦片([com.example.sheeps.data.model.Tile])的遮挡判定与覆盖面积计算。
 * 算法职责仅用于实时 UI 高亮——决定哪些牌被上层压住而「不可点击」。
 *
 * 与 `server/src/level.ts` 中服务端生成棋盘时的遮挡逻辑保持概念对齐（客户端只读判定，
 * 不涉及关卡生成/难度公式，难度由 `server/src/difficulty.ts` 控制）。
 *
 * 线程约束：本类为纯 CPU 计算、无 I/O、无共享可变状态，可在主线程调用；
 * 但棋盘规模较大（数百张牌）时遍历开销较高，建议置于 [kotlinx.coroutines.Dispatchers.Default]
 * 以避免掉帧。所有公开方法均非 `synchronized`，调用方需保证传入的 `board` 在计算期间不变。
 */
object GameEngine {

    // 单张瓦片渲染基准尺寸（dp 等效），用作覆盖面积的归一化基准
    private const val TILE = 46f
    // 相邻瓦片在棋盘网格中的逻辑步长（与 TILE 相等表示瓦片紧贴排列）
    private const val SPACING = 46f

    // 遮挡判定阈值：被上层瓦片覆盖面积超过单牌面积的 1% 即视为「被遮挡」
    private const val COVERAGE_THRESHOLD = 0.01f

    /**
     * 判断指定瓦片是否被上层瓦片遮挡（不可点击）。
     *
     * @param tile  待判定的目标瓦片
     * @param board 当前棋盘全部瓦片
     * @return true 表示该牌已被遮挡（上层覆盖面积超过 [COVERAGE_THRESHOLD]）
     */
    fun isTileBlocked(tile: Tile, board: List<Tile>): Boolean {
        var covered = 0f
        for (o in board) {
            if (o.z <= tile.z) continue
            if (o.state != TileState.NORMAL && o.state != TileState.BLOCKED) continue
            covered += overlapArea(tile, o)
        }
        return covered > TILE * TILE * COVERAGE_THRESHOLD
    }

    /**
     * 返回当前直接压住指定瓦片的所有上层瓦片（用于 UI 提示「先消哪些牌」）。
     *
     * @param tile  目标瓦片
     * @param board 当前棋盘全部瓦片
     * @return 覆盖面积大于 0 且层级更高（z 更大）的瓦片列表
     */
    fun getBlockingTiles(tile: Tile, board: List<Tile>): List<Tile> {
        return board.filter { o ->
            o.z > tile.z &&
            (o.state == TileState.NORMAL || o.state == TileState.BLOCKED) &&
            overlapArea(tile, o) > 0
        }
    }

    /**
     * 批量重算整盘每张瓦片的遮挡状态，返回新的瓦片列表（不可变更新，不修改入参）。
     *
     * 对每张处于 [com.example.sheeps.data.model.TileState.NORMAL] 或
     * [com.example.sheeps.data.model.TileState.BLOCKED] 的牌计算覆盖面积，
     * 超过 [COVERAGE_THRESHOLD] 则标记为 [com.example.sheeps.data.model.TileState.BLOCKED]，
     * 其余（已入槽/已移出等）状态原样保留。
     *
     * @param board 当前棋盘全部瓦片
     * @return 状态已刷新的瓦片副本列表（长度与入参一致）
     */
    fun calculateBlockedStates(board: List<Tile>): List<Tile> {
        val grid = buildGrid(board)
        return board.map { tile ->
            if (tile.state != TileState.NORMAL && tile.state != TileState.BLOCKED) {
                tile.copy()
            } else {
                val covered = calculateCoveredArea(tile, grid)
                val blocked = covered > TILE * TILE * COVERAGE_THRESHOLD
                tile.copy(state = if (blocked) TileState.BLOCKED else TileState.NORMAL)
            }
        }
    }

    /**
     * 计算瓦片 `a` 被瓦片 `b` 在 XY 平面上的投影重叠面积（矩形相交近似）。
     *
     * 仅当 `b` 层级高于 `a`（z 更大）时才可能非零；重叠量由两者中心坐标差与
     * [SPACING]、[TILE] 共同决定（坐标以格子单位存储，乘 [SPACING] 还原为像素）。
     *
     * @return 重叠面积（像素平方），无重叠返回 0
     */
    private fun overlapArea(a: Tile, b: Tile): Float {
        if (b.z <= a.z) return 0f
        val ox = (TILE - abs(b.x - a.x) * SPACING).coerceAtLeast(0f)
        val oy = (TILE - abs(b.y - a.y) * SPACING).coerceAtLeast(0f)
        return ox * oy
    }

    /**
     * 将棋盘按 `(floor(x), floor(y))` 网格分桶，加速遮挡面积查询。
     *
     * 单桶内仅含坐标相近的瓦片，[calculateCoveredArea] 只需在目标格周围 ±2 范围内查找，
     * 避免对整盘做 O(n²) 遍历。
     */
    private fun buildGrid(board: List<Tile>): Map<Int, Map<Int, List<Tile>>> {
        val map = mutableMapOf<Int, MutableMap<Int, MutableList<Tile>>>()
        for (t in board) {
            val bx = floor(t.x).toInt()
            val by = floor(t.y).toInt()
            map.getOrPut(bx) { mutableMapOf() }
                .getOrPut(by) { mutableListOf() }
                .add(t)
        }
        return map
    }

    /**
     * 利用网格索引计算单张瓦片被上层覆盖的总面积。
     *
     * 仅遍历目标格 `tile` 周围 ±2 范围内的瓦片，跳过自身、层级不高于自身的牌，
     * 以及已入槽/已移出等非激活状态的牌，对其累加 [overlapArea]。
     *
     * @param tile 目标瓦片
     * @param grid [buildGrid] 生成的网格索引
     * @return 覆盖总面积（像素平方）
     */
    private fun calculateCoveredArea(
        tile: Tile,
        grid: Map<Int, Map<Int, List<Tile>>>
    ): Float {
        val bx = floor(tile.x).toInt()
        val by = floor(tile.y).toInt()
        var covered = 0f
        for (dx in -2..2) {
            val col = grid[bx + dx] ?: continue
            for (dy in -2..2) {
                val bucket = col[by + dy] ?: continue
                for (o in bucket) {
                    if (o.id == tile.id) continue
                    if (o.z <= tile.z) continue
                    if (o.state != TileState.NORMAL && o.state != TileState.BLOCKED) continue
                    covered += overlapArea(tile, o)
                }
            }
        }
        return covered
    }
}
