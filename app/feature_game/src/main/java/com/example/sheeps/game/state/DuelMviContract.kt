package com.example.sheeps.game.state

import com.example.sheeps.core.multiplayer.WebSocketManager
import com.example.sheeps.data.model.Tile

/**
 * 对决模式（多人联机）界面状态模型。
 */
data class DuelViewState(
    /** 是否正在加载对局资源 */
    val isLoading: Boolean = false,
    /** 当前对局房间 ID */
    val gameId: String = "",
    /** 本地玩家 ID */
    val playerId: String = "",
    /** 对手玩家 ID */
    val opponentId: String = "",
    /** 实时连接状态（WebSocket 状态） */
    val connectionState: WebSocketManager.ConnectionState = WebSocketManager.ConnectionState.Disconnected,
    
    /** 本地棋盘卡牌列表 */
    val boardTiles: List<Tile> = emptyList(),
    /** 本地消除槽卡牌列表 */
    val slotTiles: List<Tile> = emptyList(),
    /** 本地已移出暂存区的卡牌列表 */
    val movedOutTiles: List<Tile> = emptyList(),
    
    /** 本地玩家得分 */
    val score: Int = 0,
    /** 当前连续消除组合数（连击） */
    val combo: Int = 0,
    /** 对手完成进度的比例（0.0 到 1.0） */
    val opponentProgress: Float = 0f,
    /** 对手玩家得分 */
    val opponentScore: Int = 0,
    
    /** 对局运行状态 */
    val gameStatus: GameStatus = GameStatus.INIT,
    /** 正在受到的攻击提示文本 */
    val incomingAttackMessage: String? = null,
    /** 胜者玩家 ID（对局结束后赋值） */
    val winnerId: String? = null,
    
    // --- 对决恶搞与诅咒系统状态 ---
    /** 玩家当前能量值（上限 10，用于释放大招） */
    val currentEnergy: Int = 0,
    /** 当前是否被迷雾致盲（对方释放了迷雾咒） */
    val isFogActive: Boolean = false,
    /** 消除槽最大容量（对方释放缩槽咒时会从 7 变为 6） */
    val maxSlotSize: Int = 7,
    /** 正在生效的法术状态描述消息 */
    val activeSpellMessage: String? = null,
    /** 对局初始卡牌总数（用于精准进度计算） */
    val totalTileCount: Int = 0,
    /** 对手已累计消除的卡牌总数 */
    val opponentEliminatedCount: Int = 0,
    /** 诅咒/道具效果生效剩余秒数 */
    val spellCountdownSeconds: Int = 0,
    /** 是否处于“禁魔/沉默”状态，无法释放法术 */
    val isSilenced: Boolean = false,
    /** 本局内已使用过的大招 Key 集合（单局限次） */
    val usedSpells: Set<String> = emptySet(),
    /** 正在执行阻挡反馈动画的卡牌 ID 集合 */
    val shakingTileIds: Set<String> = emptySet()
)

/**
 * 对决模式界面意图。
 */
sealed interface DuelViewIntent {
    /** 初始化对局，开始连接 WebSocket 并加载相同种子的关卡 */
    data class Init(val gameId: String, val playerId: String, val levelId: Int, val seed: Int) : DuelViewIntent
    /** 点击棋盘卡牌 */
    data class ClickTile(val tile: Tile) : DuelViewIntent
    /** 重新开始对局 */
    object Restart : DuelViewIntent
    /** 离开房间并退出对局 */
    object Leave : DuelViewIntent
    /** 主动释放恶搞大招 */
    data class CastSpell(val spellType: String) : DuelViewIntent
}

/**
 * 对决模式界面副作用。
 */
sealed interface DuelViewEffect {
    /** 显示提示消息 */
    data class ShowToast(val message: String) : DuelViewEffect
    /** 播放音效 */
    data class PlaySound(val soundType: SoundType) : DuelViewEffect
    /** 设备振动 */
    object Vibrate : DuelViewEffect
    /** 退出游戏界面返回主页 */
    object ExitGame : DuelViewEffect
}
