package com.example.sheeps.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class TileState {
    NORMAL,
    BLOCKED,
    IN_SLOT,
    MOVED_OUT
}

@Serializable
data class Tile(
    val id: String,
    val type: Int,
    val x: Float,
    val y: Float,
    val z: Int,
    var state: TileState = TileState.NORMAL,
    val isBlind: Boolean = false,
    var sealedCount: Int = 0
)

@Serializable
data class UserInfo(
    val id: String,
    val phone: String? = null,
    val username: String,
    val avatar: String? = null,
    val points: Int
)

@Serializable
data class UserItem(
    val item_type: String, // UNDO, SHUFFLE, MOVEOUT, REVIVE, HINT, BOMB, JOKER, DOUBLE_POINTS
    val count: Int
)

@Serializable
data class ShopItem(
    val id: Int,
    val name: String,
    val description: String? = null,
    val image_url: String? = null,
    val item_type: String,
    val points_price: Int,
    val stock: Int
)

@Serializable
data class ExchangeRecord(
    val id: Int,
    val shop_item_id: Int,
    val item_type: String,
    val count: Int,
    val points_cost: Int,
    val created_at: Long
)

@Serializable
data class PointRecord(
    val id: Int = 0,
    val type: String, // IN, OUT
    val amount: Int,
    val source: String,
    val remaining_points: Int,
    val created_at: Long
)

@Serializable
data class DailyTask(
    val task_id: String,
    val name: String,
    val description: String,
    val progress: Int,
    val target_count: Int,
    val is_completed: Boolean,
    val is_rewarded: Boolean,
    val points_reward: Int
)

@Serializable
data class Notice(
    val id: Int,
    val title: String,
    val content: String,
    val type: String, // ACTIVITY, UPDATE, MAINTENANCE
    val created_at: Long
)

@Serializable
data class SendCodeRequest(
    val phone: String
)

@Serializable
data class SendCodeResponse(
    val success: Boolean,
    val code: String
)

@Serializable
data class LoginRequest(
    val phone: String,
    val code: String,
    val device_uuid: String? = null
)

@Serializable
data class LoginResponse(
    val success: Boolean,
    val token: String,
    val refreshToken: String? = null,
    val user: UserInfo,
    val unlocked_levels: List<Int> = emptyList(),
    val items: List<UserItem> = emptyList(),
    val today_signed: Boolean = false,
    val sign_streak: Int = 0
)

@Serializable
data class SyncRequest(
    val points: Int,
    val unlocked_levels: List<Int>,
    val items: List<UserItem>
)

@Serializable
data class SyncResponse(
    val success: Boolean,
    val user: UserInfo,
    val unlocked_levels: List<Int>,
    val items: List<UserItem>
)

@Serializable
data class UnlockLevelRequest(
    val level_id: Int
)

@Serializable
data class UnlockLevelResponse(
    val success: Boolean,
    val current_points: Int
)

@Serializable
data class ExchangeRequest(
    val shop_item_id: Int,
    val count: Int
)

@Serializable
data class ExchangeResponse(
    val success: Boolean,
    val item_type: String,
    val new_count: Int,
    val remaining_points: Int
)

@Serializable
data class SignResponse(
    val success: Boolean,
    val streak: Int,
    val reward_points: Int,
    val current_points: Int
)

@Serializable
data class TaskClaimRequest(
    val task_id: String
)

@Serializable
data class TaskClaimResponse(
    val success: Boolean,
    val current_points: Int
)

@Serializable
data class UserProfileResponse(
    val success: Boolean,
    val user: UserInfo,
    val unlocked_levels: List<Int>,
    val items: List<UserItem>,
    val today_signed: Boolean,
    val sign_streak: Int,
    val highest_level_cleared: Int
)

@Serializable
data class RegisterRequest(
    val id: String,
    val username: String
)

@Serializable
data class RenameRequest(
    val id: String,
    val new_username: String
)

@Serializable
data class ScoreRequest(
    val user_id: String,
    val level_id: Int,
    val score: Int,
    val clear_time_ms: Long,
    val sign: String
)

@Serializable
data class RankingEntry(
    val username: String,
    val avatar: String? = null,
    val clear_time_ms: Long,
    val score: Int,
    val achieved_at: Long
)

@Serializable
data class LeaderboardResponse(
    val success: Boolean,
    val rankings: List<RankingEntry> = emptyList()
)

@Serializable
data class GenericResponse(
    val success: Boolean
)

@Serializable
data class RefreshResponse(
    val success: Boolean,
    val token: String? = null,
    val refreshToken: String? = null,
    val error: String? = null
)

@Serializable
data class AppUpdateResponse(
    val has_update: Boolean = false,
    val version_name: String? = null,
    val apk_url: String? = null,
    val update_log: String? = null,
    val force_update: Boolean = false
)

@Serializable
data class MatchJoinRequest(
    val playerId: String
)

@Serializable
data class MatchStatusResponse(
    val status: String, // searching, matched, error
    val gameId: String? = null,
    val opponentId: String? = null
)
