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
    val winnerId: String? = null
)

sealed interface DuelViewIntent {
    data class Init(val gameId: String, val playerId: String) : DuelViewIntent
    data class ClickTile(val tile: Tile) : DuelViewIntent
    object Restart : DuelViewIntent
    object Leave : DuelViewIntent
}

sealed interface DuelViewEffect {
    data class ShowToast(val message: String) : DuelViewEffect
    data class PlaySound(val soundType: SoundType) : DuelViewEffect
    object Vibrate : DuelViewEffect
}
