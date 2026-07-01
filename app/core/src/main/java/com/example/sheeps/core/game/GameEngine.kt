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
