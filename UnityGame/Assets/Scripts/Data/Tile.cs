using UnityEngine;

namespace UnityGame.Data
{
    /// <summary>
    /// 卡牌数据模型
    /// </summary>
    [System.Serializable]
    public class Tile
    {
        public string id;           // 唯一标识
        public int type;           // 卡牌类型（图案）
        public float x;            // X坐标位置
        public float y;            // Y坐标位置
        public int z;              // Z轴层级
        public TileState state;    // 卡牌状态
        public int sealedCount;    // 封印次数（Android原生代码对应字段）
        public bool isBlind;       // 是否为盲盒牌（Android原生代码对应字段）

        public Tile(string id, int type, float x, float y, int z, TileState state = TileState.NORMAL, int sealedCount = 0, bool isBlind = false)
        {
            this.id = id;
            this.type = type;
            this.x = x;
            this.y = y;
            this.z = z;
            this.state = state;
            this.sealedCount = sealedCount;
            this.isBlind = isBlind;
        }

        /// <summary>
        /// 创建副本
        /// </summary>
        public Tile Copy()
        {
            return new Tile(id, type, x, y, z, state, sealedCount, isBlind);
        }

        public override bool Equals(object obj)
        {
            if (obj is Tile other)
                return this.id == other.id;
            return false;
        }

        public override int GetHashCode()
        {
            return id.GetHashCode();
        }

        public override string ToString()
        {
            return $"Tile(id={id}, type={type}, pos=({x},{y},{z}), state={state})";
        }
    }
}
