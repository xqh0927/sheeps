namespace UnityGame.Data
{
    /// <summary>
    /// 卡牌状态枚举
    /// </summary>
    public enum TileState
    {
        NORMAL = 0,      // 正常状态（可点击）
        BLOCKED = 1,     // 被遮挡状态
        IN_SLOT = 2,     // 已在槽位中
        MATCHED = 3,     // 已匹配消除
        MOVED_OUT = 4    // 已移出
    }
}
