package com.example.sheeps.menu.state

import com.example.sheeps.data.model.*

data class MenuViewState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val username: String = "",
    val phone: String = "",
    val points: Int = 0,
    val unlockedLevel: Int = 1,
    val todaySigned: Boolean = false,
    val signStreak: Int = 0,
    val highestLevelCleared: Int = 0,
    
    // Core Lists
    val backpackItems: List<UserItem> = emptyList(),
    val shopItems: List<ShopItem> = emptyList(),
    val notices: List<Notice> = emptyList(),
    val dailyTasks: List<DailyTask> = emptyList(),
    
    // History logs
    val pointsHistory: List<PointRecord> = emptyList(),
    val exchangeHistory: List<ExchangeRecord> = emptyList(),
    
    // Item carry select
    val selectedCarryItems: Map<String, Int> = emptyMap(), // item_type -> selected count

    // Network status
    val networkStatus: com.example.sheeps.core.utils.NetworkStatus = com.example.sheeps.core.utils.NetworkStatus.ONLINE,

    // Selected Language
    val language: String = "",

    // App Update
    val appUpdateInfo: AppUpdateResponse? = null,
    val currentSkin: String = "classic",

    // Matchmaking
    val matchStatus: String = "none", // none, searching, matched, error
    val matchedGameId: String? = null,
    val matchedOpponentId: String? = null
)

data class ConflictInfo(
    val localPoints: Int,
    val localLevel: Int,
    val cloudPoints: Int,
    val cloudLevel: Int
)

sealed interface MenuViewIntent {
    object LoadData : MenuViewIntent
    data class SendSmsCode(val phone: String) : MenuViewIntent
    data class LoginWithCode(val phone: String, val code: String) : MenuViewIntent
    object Logout : MenuViewIntent
    object SignIn : MenuViewIntent
    data class ExchangeShopItem(val shopItemId: Int, val count: Int) : MenuViewIntent
    data class ClaimTask(val taskId: String) : MenuViewIntent
    data class UnlockLevelWithPoints(val levelId: Int) : MenuViewIntent
    data class UpdateCarryItem(val itemType: String, val change: Int) : MenuViewIntent
    object ClearCarryItems : MenuViewIntent
    data class GoToGame(val levelId: Int, val carryItemsJson: String) : MenuViewIntent
    data class ResolveConflict(val useLocal: Boolean) : MenuViewIntent
    data class ChangeLanguage(val lang: String) : MenuViewIntent
    object DismissUpdate : MenuViewIntent
    data class ChangeSkin(val skin: String) : MenuViewIntent
    
    // Matchmaking
    data class JoinMatch(val playerId: String) : MenuViewIntent
    data class LeaveMatch(val playerId: String) : MenuViewIntent
    object ResetMatchStatus : MenuViewIntent
}

sealed interface MenuViewEffect {
    data class ShowToast(val message: String) : MenuViewEffect
    data class NavigateToGame(val levelId: Int, val carryItemsJson: String) : MenuViewEffect
    object ShowLoginDialog : MenuViewEffect
    data class ShowConflictDialog(
        val localPoints: Int,
        val localLevel: Int,
        val cloudPoints: Int,
        val cloudLevel: Int
    ) : MenuViewEffect
}
