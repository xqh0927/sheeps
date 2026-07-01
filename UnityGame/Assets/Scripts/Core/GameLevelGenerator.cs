using System;
using System.Collections.Generic;
using System.Linq;
using UnityGame.Data;

namespace UnityGame.Core
{
    /// <summary>
    /// 游戏关卡生成器
    /// 负责生成可解的关卡
    /// </summary>
    public class GameLevelGenerator
    {
        private static System.Random random = new System.Random();
        
        // 卡牌类型数量（可以根据需要增加）
        private const int TOTAL_TILE_TYPES = 12;
        
        /// <summary>
        /// 生成可解的本地关卡
        /// </summary>
        /// <param name="levelId">关卡ID</param>
        /// <returns>生成的卡牌列表</returns>
        public List<Tile> GenerateSolvableLevel(int levelId)
        {
            // 根据关卡难度调整参数
            int tileCount = GetTileCountForLevel(levelId);
            int layers = GetLayerCountForLevel(levelId);
            
            // 确保卡牌数量是3的倍数
            tileCount = (tileCount / 3) * 3;
            
            List<Tile> tiles = new List<Tile>();
            
            // 生成卡牌类型（每3张相同类型）
            List<int> tileTypes = GenerateTileTypes(tileCount);
            
            // 生成卡牌位置
            int tileIndex = 0;
            for (int z = 0; z < layers; z++)
            {
                int tilesInThisLayer = Math.Min(tileCount - tileIndex, GetTilesPerLayer(z, levelId));
                
                for (int i = 0; i < tilesInThisLayer && tileIndex < tileCount; i++)
                {
                    // 生成位置（确保有重叠）
                    float x = (float)random.NextDouble() * 4; // 0-4 范围
                    float y = (float)random.NextDouble() * 6; // 0-6 范围
                    
                    string id = $"tile_{levelId}_{tileIndex}";
                    Tile tile = new Tile(id, tileTypes[tileIndex], x, y, z);
                    tiles.Add(tile);
                    tileIndex++;
                }
            }
            
            // 打乱顺序（但保持坐标不变）
            ShuffleTiles(tiles);
            
            return tiles;
        }
        
        /// <summary>
        /// 根据关卡ID获取卡牌总数
        /// </summary>
        private int GetTileCountForLevel(int levelId)
        {
            // 第1关：21张（7组）
            // 第2关：30张（10组）
            // 第3关及以上：39张（13组）
            if (levelId <= 1) return 21;
            if (levelId <= 2) return 30;
            return 39;
        }
        
        /// <summary>
        /// 根据关卡ID获取层数
        /// </summary>
        private int GetLayerCountForLevel(int levelId)
        {
            // 第1关：2层
            // 第2关：3层
            // 第3关及以上：3-4层
            if (levelId <= 1) return 2;
            if (levelId <= 2) return 3;
            return 4;
        }
        
        /// <summary>
        /// 获取每层的卡牌数量
        /// </summary>
        private int GetTilesPerLayer(int layerIndex, int levelId)
        {
            // 底层放更多卡牌，上层放更少
            int totalTiles = GetTileCountForLevel(levelId);
            int layers = GetLayerCountForLevel(levelId);
            
            if (layerIndex == 0) return totalTiles / 2;  // 底层50%
            if (layerIndex == layers - 1) return totalTiles / 6;  // 顶层较少
            
            return totalTiles / (layers * 2);  // 中间层平均分配
        }
        
        /// <summary>
        /// 生成卡牌类型列表（每3张相同）
        /// </summary>
        private List<int> GenerateTileTypes(int tileCount)
        {
            List<int> types = new List<int>();
            int groups = tileCount / 3;
            
            for (int i = 0; i < groups; i++)
            {
                int type = i % TOTAL_TILE_TYPES;
                // 每组3张相同类型
                types.Add(type);
                types.Add(type);
                types.Add(type);
            }
            
            return types;
        }
        
        /// <summary>
        /// 打乱卡牌顺序（保持坐标不变）
        /// </summary>
        private void ShuffleTiles(List<Tile> tiles)
        {
            int n = tiles.Count;
            while (n > 1)
            {
                n--;
                int k = random.Next(n + 1);
                Tile temp = tiles[k];
                tiles[k] = tiles[n];
                tiles[n] = temp;
            }
        }
        
        /// <summary>
        /// 验证关卡是否可解（简单验证：确保有足够的匹配对）
        /// </summary>
        public bool ValidateLevel(List<Tile> tiles)
        {
            // 统计每种类型的数量
            Dictionary<int, int> typeCount = new Dictionary<int, int>();
            foreach (var tile in tiles)
            {
                if (!typeCount.ContainsKey(tile.type))
                    typeCount[tile.type] = 0;
                typeCount[tile.type]++;
            }
            
            // 每种类型的数量必须是3的倍数
            foreach (var count in typeCount.Values)
            {
                if (count % 3 != 0)
                    return false;
            }
            
            return true;
        }
    }
}
