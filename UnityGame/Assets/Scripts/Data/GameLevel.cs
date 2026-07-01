using System.Collections.Generic;

namespace UnityGame.Data
{
    /// <summary>
    /// 游戏关卡数据
    /// </summary>
    [System.Serializable]
    public class GameLevel
    {
        public int levelId;
        public List<Tile> tiles;
        public int maxSlotSize = 7;
        
        public GameLevel(int levelId)
        {
            this.levelId = levelId;
            this.tiles = new List<Tile>();
        }
    }

    /// <summary>
    /// 槽位中的卡牌组
    /// </summary>
    [System.Serializable]
    public class SlotData
    {
        public List<Tile> tiles = new List<Tile>();
        public const int MAX_SIZE = 7;
        
        public bool IsFull() => tiles.Count >= MAX_SIZE;
        public bool CanAdd() => tiles.Count < MAX_SIZE;
        public void AddTile(Tile tile)
        {
            if (CanAdd())
            {
                tile.state = TileState.IN_SLOT;
                tiles.Add(tile);
            }
        }
        
        public bool RemoveTile(Tile tile)
        {
            // 按 id 查找，避免引用相等性问题
            int index = tiles.FindIndex(t => t.id == tile.id);
            if (index >= 0)
            {
                tiles.RemoveAt(index);
                return true;
            }
            return false;
        }
        
        public void Clear()
        {
            tiles.Clear();
        }
    }
}
