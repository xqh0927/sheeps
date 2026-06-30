package com.example.sheeps.game.state

import com.example.sheeps.data.model.RankingEntry
import com.example.sheeps.data.model.Tile

enum class GameStatus {
    INIT,
    MENU,
    PLAYING,
    WON,
    LOST
}

enum class SoundType {
    CLICK,
    UNSEAL,
    MATCH,
    WIN,
    LOSE
}

data class GameViewState(
    val isLoading: Boolean = false,
    val isPrivacyAccepted: Boolean = false,
    val username: String = "",
    val unlockedLevel: Int = 1,
    val currentLevelId: Int = 1,
    val boardTiles: List<Tile> = emptyList(),
    val slotTiles: List<Tile> = emptyList(),
    val movedOutTiles: List<Tile> = emptyList(),
    
    // Core item limits (dynamically set from carry selection)
    val undoCount: Int = 0,
    val moveOutCount: Int = 0,
    val shuffleCount: Int = 0,
    val reviveCount: Int = 0,
    val hintCount: Int = 0,
    val bombCount: Int = 0,
    val jokerCount: Int = 0,
    val doublePointsCount: Int = 0,

    val isDoublePointsActive: Boolean = false,
    val highlightedTileIds: Set<String> = emptySet(),
    val score: Int = 0,
    val gameStatus: GameStatus = GameStatus.INIT,
    val rankings: List<RankingEntry> = emptyList(),
    val currentSkin: String = "classic",
    val shakingTileIds: Set<String> = emptySet()
)

sealed interface GameViewIntent {
    object AgreePrivacy : GameViewIntent
    object InitUser : GameViewIntent
    data class ChangeUsername(val newName: String) : GameViewIntent
    data class LoadLevel(val levelId: Int, val carryItemsJson: String? = null) : GameViewIntent
    data class ClickTile(val tile: Tile) : GameViewIntent
    object UseUndo : GameViewIntent
    object UseMoveOut : GameViewIntent
    object UseShuffle : GameViewIntent
    object Revive : GameViewIntent
    object UseHint : GameViewIntent
    object UseBomb : GameViewIntent
    object UseJoker : GameViewIntent
    object UseDoublePoints : GameViewIntent
    data class LoadLeaderboard(val levelId: Int) : GameViewIntent
    object RestartLevel : GameViewIntent
    object GoBackToMenu : GameViewIntent
}

sealed interface GameViewEffect {
    data class ShowToast(val message: String) : GameViewEffect
    data class PlaySound(val soundType: SoundType) : GameViewEffect
    object Vibrate : GameViewEffect
}
