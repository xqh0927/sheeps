package com.example.sheeps.core.game

import com.example.sheeps.data.model.Tile
import com.example.sheeps.data.model.TileState
import kotlin.math.abs
import kotlin.math.floor

object GameEngine {

    private const val TILE = 48f
    private const val SPACING = 46f

    private const val COVERAGE_THRESHOLD = 0.01f

    fun isTileBlocked(tile: Tile, board: List<Tile>): Boolean {
        var covered = 0f
        for (o in board) {
            if (o.z <= tile.z) continue
            if (o.state != TileState.NORMAL && o.state != TileState.BLOCKED) continue
            covered += overlapArea(tile, o)
        }
        return covered > TILE * TILE * COVERAGE_THRESHOLD
    }

    fun getBlockingTiles(tile: Tile, board: List<Tile>): List<Tile> {
        return board.filter { o ->
            o.z > tile.z &&
            (o.state == TileState.NORMAL || o.state == TileState.BLOCKED) &&
            overlapArea(tile, o) > 0
        }
    }

    fun calculateBlockedStates(board: List<Tile>): List<Tile> {
        val grid = buildGrid(board)
        return board.map { tile ->
            if (tile.state != TileState.NORMAL && tile.state != TileState.BLOCKED) {
                tile
            } else {
                val covered = calculateCoveredArea(tile, grid)
                val blocked = covered > TILE * TILE * COVERAGE_THRESHOLD
                tile.copy(state = if (blocked) TileState.BLOCKED else TileState.NORMAL)
            }
        }
    }

    private fun overlapArea(a: Tile, b: Tile): Float {
        if (b.z <= a.z) return 0f
        val ox = (TILE - abs(b.x - a.x) * SPACING).coerceAtLeast(0f)
        val oy = (TILE - abs(b.y - a.y) * SPACING).coerceAtLeast(0f)
        return ox * oy
    }

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
