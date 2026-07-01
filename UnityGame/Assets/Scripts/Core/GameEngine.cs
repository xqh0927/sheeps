using System.Collections.Generic;
using UnityGame.Data;

namespace UnityGame.Core
{
    /// <summary>
    /// 游戏核心引擎，处理卡牌间的遮挡逻辑
    /// 提供静态算法用于计算卡牌是否被压住、获取压住某张牌的所有卡牌、以及批量刷新棋盘卡牌状态
    /// </summary>
    public static class GameEngine
    {
        private const float TILE_SIZE = 48.0f;
        private const float TILE_SPACING = 46.0f;
        private const float BLOCK_THRESHOLD = 230.4f; // 重叠面积阈值（约半张牌的面积）

        /// <summary>
        /// 判定一张卡牌是否被其它处于更高层级（Z轴）且位置重叠的卡牌遮挡（锁定）
        /// </summary>
        /// <param name="tile">需要检查的卡牌</param>
        /// <param name="board">当前棋盘上所有存在的卡牌列表</param>
        /// <returns>如果被遮挡返回 true，否则返回 false</returns>
        public static bool IsTileBlocked(Tile tile, List<Tile> board)
        {
            foreach (var other in board)
            {
                // 跳过自身、非正常状态的牌、或者在当前牌下方(Z轴更小或相等)的牌
                if (other.id == tile.id ||
                    (other.state != TileState.NORMAL && other.state != TileState.BLOCKED) ||
                    other.z <= tile.z)
                {
                    continue;
                }

                // 计算重叠面积：根据卡牌坐标差 (dx, dy) 算出水平和垂直的重叠长度
                // 修复：使用 Mathf.Max 替代 Clamp，与 Android 原生代码保持一致
                // Android 原生代码：val ox = max(0f, TILE_SIZE - dx * TILE_SPACING)
                float dx = UnityEngine.Mathf.Abs(other.x - tile.x);
                float dy = UnityEngine.Mathf.Abs(other.y - tile.y);
                
                // 计算重叠面积（仅在重叠时为正数）
                float ox = UnityEngine.Mathf.Max(0, TILE_SIZE - dx * TILE_SPACING);
                float oy = UnityEngine.Mathf.Max(0, TILE_SIZE - dy * TILE_SPACING);
                
                // 只要重叠面积超过阈值，即判定为被遮挡
                if (ox * oy > BLOCK_THRESHOLD)
                {
                    return true;
                }
            }

            return false;
        }

        /// <summary>
        /// 获取所有遮挡住（压住）指定卡牌的卡牌列表
        /// </summary>
        /// <param name="tile">指定的卡牌</param>
        /// <param name="board">当前棋盘卡牌列表</param>
        /// <returns>正在遮挡该卡牌的卡牌集合</returns>
        public static List<Tile> GetBlockingTiles(Tile tile, List<Tile> board)
        {
            List<Tile> blockingTiles = new List<Tile>();

            foreach (var other in board)
            {
                // 过滤条件：排除自身、非正常状态的牌、或者在当前牌下方(Z轴更小)的牌
                if (other.id == tile.id ||
                    (other.state != TileState.NORMAL && other.state != TileState.BLOCKED) ||
                    other.z <= tile.z)
                {
                    continue;
                }

                // 重叠检测算法
                // 修复：使用 Mathf.Max 替代 Clamp，与 Android 原生代码保持一致
                float dx = UnityEngine.Mathf.Abs(other.x - tile.x);
                float dy = UnityEngine.Mathf.Abs(other.y - tile.y);
                
                // 计算重叠面积（仅在重叠时为正数）
                float ox = UnityEngine.Mathf.Max(0, TILE_SIZE - dx * TILE_SPACING);
                float oy = UnityEngine.Mathf.Max(0, TILE_SIZE - dy * TILE_SPACING);
                
                // 判定遮挡
                if (ox * oy > BLOCK_THRESHOLD)
                {
                    blockingTiles.Add(other);
                }
            }

            return blockingTiles;
        }

        /// <summary>
        /// 批量计算并更新棋盘上所有卡牌的遮挡状态（TileState.BLOCKED 或 TileState.NORMAL）
        /// 通常在玩家点击取走一张牌后调用，以刷新下方卡牌的可点击性
        /// </summary>
        /// <param name="board">原始卡牌列表</param>
        /// <returns>更新状态后的新卡牌列表副本</returns>
        public static List<Tile> CalculateBlockedStates(List<Tile> board)
        {
            List<Tile> newBoard = new List<Tile>();

            foreach (var tile in board)
            {
                // 只有处于 NORMAL 或 BLOCKED 状态的牌才需要重新判断状态
                if (tile.state == TileState.NORMAL || tile.state == TileState.BLOCKED)
                {
                    bool blocked = IsTileBlocked(tile, board);
                    // 如果当前牌被遮挡，状态标记为 BLOCKED，否则设为 NORMAL
                    newBoard.Add(new Tile(tile.id, tile.type, tile.x, tile.y, tile.z, 
                        blocked ? TileState.BLOCKED : TileState.NORMAL));
                }
                else
                {
                    // 已被消除或其它状态的牌保持原状
                    newBoard.Add(tile.Copy());
                }
            }

            return newBoard;
        }

        /// <summary>
        /// 获取所有可见的卡牌（NORMAL 或 BLOCKED 状态）
        /// </summary>
        public static List<Tile> GetVisibleTiles(List<Tile> board)
        {
            return board.FindAll(t => t.state == TileState.NORMAL || t.state == TileState.BLOCKED);
        }

        /// <summary>
        /// 获取所有可点击的卡牌（仅 NORMAL 状态）
        /// </summary>
        public static List<Tile> GetClickableTiles(List<Tile> board)
        {
            return board.FindAll(t => t.state == TileState.NORMAL);
        }
    }

    /// <summary>
    /// 扩展方法类
    /// </summary>
    public static class FloatExtensions
    {
        public static float Clamp(this float value, float min, float max)
        {
            if (value < min) return min;
            if (value > max) return max;
            return value;
        }
    }
}
