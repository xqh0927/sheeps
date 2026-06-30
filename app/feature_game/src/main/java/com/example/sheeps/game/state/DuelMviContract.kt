package com.example.sheeps.game.state

import com.example.sheeps.core.multiplayer.WebSocketManager
import com.example.sheeps.data.model.Tile

data class DuelViewState(
    val isLoading: Boolean = false,
    val gameId: String = "",
    val playerId: String = "",
    val opponentId: String = "",
    val connectionState: WebSocketManager.ConnectionState = WebSocketManager.ConnectionState.Disconnected,
    
    val boardTiles: List<Tile> = emptyList(),
    val slotTiles: List<Tile> = emptyList(),
    val movedOutTiles: List<Tile> = emptyList(),
    
    val score: Int = 0,
    val combo: Int = 0,
    val opponentProgress: Float = 0f, // 0.0 to 1.0 (cards remaining ratio)
    val opponentScore: Int = 0,
    
    val gameStatus: GameStatus = GameStatus.INIT,
    val incomingAttackMessage: String? = null,
    val winnerId: String? = null,
    
    // 新增恶搞与诅咒系统状态
    val currentEnergy: Int = 0,               // 玩家当前能量值（上限为 10）
    val isFogActive: Boolean = false,         // 是否处于被“起雾”致盲状态
    val maxSlotSize: Int = 7,                 // 最大可用卡槽数量（受击时缩减为 6）
    val activeSpellMessage: String? = null,   // 屏幕上方受击提示词（如：“对手对你使用了缩槽术！”）
    val totalTileCount: Int = 0,              // 棋盘初始卡牌总数 (用于精准计算进度)
    val opponentEliminatedCount: Int = 0,     // 对手已消除的卡牌数量
    val spellCountdownSeconds: Int = 0,       // 道具卡/诅咒倒计时
    val isSilenced: Boolean = false,          // 是否被禁魔/沉默
    val usedSpells: Set<String> = emptySet(),  // 局内已经使用过的大招
    val shakingTileIds: Set<String> = emptySet()
)

sealed interface DuelViewIntent {
    data class Init(val gameId: String, val playerId: String, val levelId: Int, val seed: Int) : DuelViewIntent
    data class ClickTile(val tile: Tile) : DuelViewIntent
    object Restart : DuelViewIntent
    object Leave : DuelViewIntent
    data class CastSpell(val spellType: String) : DuelViewIntent // 新增：玩家点击主动施法意图
}

sealed interface DuelViewEffect {
    data class ShowToast(val message: String) : DuelViewEffect
    data class PlaySound(val soundType: SoundType) : DuelViewEffect
    object Vibrate : DuelViewEffect
    object ExitGame : DuelViewEffect
}
