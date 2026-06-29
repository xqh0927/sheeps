package com.example.sheeps.core.game

import com.example.sheeps.data.model.Tile
import com.example.sheeps.data.model.TileState
import kotlin.math.abs

object GameEngine {

    /**
     * Checks if a card (tile) is blocked/obscured by any other card at a higher z-layer.
     */
    fun isTileBlocked(tile: Tile, board: List<Tile>): Boolean {
        val W = 1.0f
        val H = 1.0f
        return board.any { other ->
            other.id != tile.id &&
            (other.state == TileState.NORMAL || other.state == TileState.BLOCKED) &&
            other.z > tile.z &&
            abs(other.x - tile.x) < W &&
            abs(other.y - tile.y) < H
        }
    }

    /**
     * Calculates the occlusion state of all cards on the board, updating TileState.BLOCKED/NORMAL.
     */
    fun calculateBlockedStates(board: List<Tile>): List<Tile> {
        return board.map { tile ->
            if (tile.state == TileState.NORMAL || tile.state == TileState.BLOCKED) {
                val blocked = isTileBlocked(tile, board)
                tile.copy(state = if (blocked) TileState.BLOCKED else TileState.NORMAL)
            } else {
                tile
            }
        }
    }
}
