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
        // 卡牌渲染宽度与逻辑步长比例，用于重叠判定
        val W = 52.0f / 46.0f
        val H = 52.0f / 46.0f
        return board.any { other ->
            other.id != tile.id &&
            // 只有处于正常或锁定状态的卡牌会产生遮挡效果
            (other.state == TileState.NORMAL || other.state == TileState.BLOCKED) &&
            // 对方必须在上方
            other.z > tile.z &&
            // 检查 X, Y 轴是否有足够的重叠面积
            abs(other.x - tile.x) < W &&
            abs(other.y - tile.y) < H
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
        val W = 52.0f / 46.0f
        val H = 52.0f / 46.0f
        return board.filter { other ->
            other.id != tile.id &&
            (other.state == TileState.NORMAL || other.state == TileState.BLOCKED) &&
            other.z > tile.z &&
            abs(other.x - tile.x) < W &&
            abs(other.y - tile.y) < H
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
            if (tile.state == TileState.NORMAL || tile.state == TileState.BLOCKED) {
                val blocked = isTileBlocked(tile, board)
                tile.copy(state = if (blocked) TileState.BLOCKED else TileState.NORMAL)
            } else {
                tile
            }
        }
    }
}
