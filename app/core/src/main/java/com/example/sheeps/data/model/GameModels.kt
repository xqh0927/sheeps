package com.example.sheeps.data.model

import kotlinx.serialization.Serializable

/** 瓦片（麻将牌）的运行时状态，参与游戏引擎遮挡判定与消除流程。 */
@Serializable
enum class TileState {
    /** 正常可交互（未被遮挡） */
    NORMAL,
    /** 被上层瓦片遮挡，暂不可点击 */
    BLOCKED,
    /** 已被拖入底部消除槽 */
    IN_SLOT,
    /** 已成功消除移出棋盘 */
    MOVED_OUT
}

/**
 * 棋盘上的一张瓦片（客户端运行时模型，对应服务端 `level.ts` 生成的关卡布局元素）。
 *
 * @param id    瓦片唯一标识
 * @param type  瓦片种类（1..12，对应卡面图案）
 * @param x     棋盘网格 X 坐标（格子单位）
 * @param y     棋盘网格 Y 坐标（格子单位）
 * @param z     层级（越大越靠上，遮挡判定以此为准）
 * @param state 运行时状态，见 [TileState]
 * @param isBlind 是否为盲牌（隐藏图案）
 * @param sealedCount 封印计数（被符纸封印的层数）
 */
@Serializable
data class Tile(
    val id: String,
    val type: Int,
    val x: Float,
    val y: Float,
    val z: Int,
    var state: TileState = TileState.NORMAL,
    var isBlind: Boolean = false,
    var sealedCount: Int = 0
)

/** 用户基础信息（登录/同步/资料接口返回的轻量用户模型）。 */
@Serializable
data class UserInfo(
    val id: String,
    val phone: String? = null,
    val username: String,
    val avatar: String? = null,
    val points: Int
)

/**
 * 用户持有的单个道具（背包条目）。
 *
 * @param item_type 道具类型，如 UNDO / SHUFFLE / MOVEOUT / REVIVE / HINT / BOMB / JOKER / DOUBLE_POINTS
 * @param count 当前持有数量
 * @param image_url v2 图片服务器下发：道具图标远程 URL；缺失时 [com.example.sheeps.ui.components.ItemIcon] 回退本地占位
 */
@Serializable
data class UserItem(
    val item_type: String, // UNDO, SHUFFLE, MOVEOUT, REVIVE, HINT, BOMB, JOKER, DOUBLE_POINTS
    val count: Int,
    val image_url: String? = null // v2 图片服务器下发：道具图标远程 URL；缺失时 ItemIcon 回退本地占位
)

/**
 * 积分商城商品（皮肤 / 道具），由 `/api/shop/items` 下发。
 *
 * @param id 商品 ID
 * @param name 商品名称（多语言由服务端下发）
 * @param description 商品描述
 * @param image_url 封面（皮肤）或道具图标统一展示字段，镜像自 skin_tiles[1] 或 item_icons
 * @param item_type 商品类型标识；以 "SKIN_" 前缀表示皮肤，其余为道具
 * @param points_price 兑换所需积分
 * @param stock 库存
 * @param group v2 新增：主题系列分组（可空，如 地域系列/萌系系列/数码系列/生活系列）
 * @param tiles v2 新增：仅皮肤项含 12 张卡面 URL，index 0..11 对应 tile 1..12
 */
@Serializable
data class ShopItem(
    val id: Int,
    val name: String,
    val description: String? = null,
    val image_url: String? = null,        // 封面(皮肤) / 道具图标 —— 统一展示字段（镜像自 skin_tiles[1] 或 item_icons）
    val item_type: String,
    val points_price: Int,
    val stock: Int,
    val group: String? = null,            // v2 新增：主题系列分组（可空，如 地域系列/萌系系列/数码系列/生活系列）
    val tiles: List<String>? = null       // v2 新增：仅皮肤项 12 张卡面 URL，index 0..11 对应 tile 1..12
)

/** 道具兑换历史记录（积分商城兑换明细）。 */
@Serializable
data class ExchangeRecord(
    val id: Int = 0,
    val shop_item_id: Int,
    val item_type: String,
    val count: Int,
    val points_cost: Int,
    val created_at: Long
)

/**
 * 积分流水明细。
 *
 * @param type 收支类型：IN（收入）/ OUT（支出）
 * @param amount 变动积分
 * @param source 来源说明
 * @param remaining_points 变动后剩余积分
 */
@Serializable
data class PointRecord(
    val id: Int = 0,
    val type: String, // IN, OUT
    val amount: Int,
    val source: String,
    val remaining_points: Int,
    val created_at: Long
)

/** 每日任务（含完成进度与奖励状态），由 `/api/task/daily` 下发。 */
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

/**
 * 系统公告。
 *
 * @param type 公告类型：ACTIVITY（活动）/ UPDATE（更新）/ MAINTENANCE（维护）
 */
@Serializable
data class Notice(
    val id: Int,
    val title: String,
    val content: String,
    val type: String, // ACTIVITY, UPDATE, MAINTENANCE
    val created_at: Long
)

/** 发送验证码请求（手机号）。 */
@Serializable
data class SendCodeRequest(
    val phone: String
)

/** 发送验证码响应（调试模式下 [code] 直接返回验证码）。 */
@Serializable
data class SendCodeResponse(
    val success: Boolean,
    val code: String
)

/**
 * 手机号 + 验证码登录请求。
 *
 * @param device_uuid 设备唯一标识（可空），用于游客数据合并
 */
@Serializable
data class LoginRequest(
    val phone: String,
    val code: String,
    val device_uuid: String? = null
)

/** 登录响应：返回双 Token、用户信息、已解锁关卡、道具背包与签到状态。 */
@Serializable
data class LoginResponse(
    val success: Boolean,
    val token: String,
    val refreshToken: String? = null,
    val user: UserInfo,
    val unlocked_levels: List<Int> = emptyList(),
    val items: List<UserItem> = emptyList(),
    val today_signed: Boolean = false,
    val sign_streak: Int = 0,
    val hasPassword: Boolean = false
)

/** 端云数据同步请求体（积分 + 已解锁关卡 + 道具背包）。 */
@Serializable
data class SyncRequest(
    val points: Int,
    val unlocked_levels: List<Int>,
    val items: List<UserItem>
)

/** 端云数据同步响应（返回云端权威的用户/关卡/道具数据）。 */
@Serializable
data class SyncResponse(
    val success: Boolean,
    val user: UserInfo,
    val unlocked_levels: List<Int>,
    val items: List<UserItem>
)

/** 解锁关卡请求（目标关卡编号）。 */
@Serializable
data class UnlockLevelRequest(
    val level_id: Int
)

/** 解锁关卡响应（含当前剩余积分）。 */
@Serializable
data class UnlockLevelResponse(
    val success: Boolean,
    val current_points: Int
)

/** 积分兑换道具请求（商品 ID + 数量）。 */
@Serializable
data class ExchangeRequest(
    val shop_item_id: Int,
    val count: Int
)

/** 积分兑换道具响应（含兑换后数量与剩余积分）。 */
@Serializable
data class ExchangeResponse(
    val success: Boolean,
    val item_type: String,
    val new_count: Int,
    val remaining_points: Int
)

/** 每日签到响应（连签天数 / 本次奖励 / 当前总积分）。 */
@Serializable
data class SignResponse(
    val success: Boolean,
    val streak: Int,
    val reward_points: Int,
    val current_points: Int
)

/** 领取任务奖励请求（任务 ID）。 */
@Serializable
data class TaskClaimRequest(
    val task_id: String
)

/** 领取任务奖励响应（含当前总积分）。 */
@Serializable
data class TaskClaimResponse(
    val success: Boolean,
    val current_points: Int
)

/** 用户完整资料响应（资料 / 背包 / 签到进度 / 最高关卡 / 头像 URL），云端权威数据源。 */
@Serializable
data class UserProfileResponse(
    val success: Boolean,
    val user: UserInfo,
    val unlocked_levels: List<Int>,
    val items: List<UserItem>,
    val today_signed: Boolean,
    val sign_streak: Int,
    val highest_level_cleared: Int,
    val avatarUrl: String? = null
)

/** 手机号 + 密码 + 验证码注册请求。 */
@Serializable
data class RegisterAuthRequest(
    val phone: String,
    val password: String,
    val code: String
)

/** 手机号 + 密码登录请求。 */
@Serializable
data class PasswordLoginRequest(
    val phone: String,
    val password: String
)

/** 重置密码请求（手机号 + 验证码 + 新密码）。 */
@Serializable
data class ResetPasswordRequest(
    val phone: String,
    val code: String,
    val newPassword: String
)

/** 设置密码请求（登录后设置）。 */
@Serializable
data class SetPasswordRequest(
    val password: String
)

/** 是否已设置密码检查响应。 */
@Serializable
data class CheckPasswordResponse(
    val hasPassword: Boolean
)

/** 改名请求（新昵称）。 */
@Serializable
data class RenameRequest(
    val new_username: String
)

/**
 * 通关成绩上报请求（含防作弊签名）。
 *
 * @param user_id 用户 ID
 * @param level_id 关卡编号
 * @param score 本次得分
 * @param clear_time_ms 通关清盘用时（毫秒）
 * @param sign 防作弊签名（客户端与服务端一致算法，如 SHA-256(userId_level_time_salt)）
 * @param game_mode 0=闯关/PvP，1=无尽生存；默认 0 保持向后兼容
 */
@Serializable
data class ScoreRequest(
    val user_id: String,
    val level_id: Int,
    val score: Int,
    val clear_time_ms: Long,
    val sign: String,
    /** 0=闯关/PvP，1=无尽生存；默认 0 保持向后兼容 */
    val game_mode: Int = 0
)

/** 排行榜单条记录（用户名 / 头像 / 成绩 / 达成时间）。 */
@Serializable
data class RankingEntry(
    val username: String,
    val avatar: String? = null,
    val clear_time_ms: Long,
    val score: Int,
    val achieved_at: Long
)

/** 排行榜响应（可能包含排行榜总开关 [disabled]）。 */
@Serializable
data class LeaderboardResponse(
    val success: Boolean,
    val rankings: List<RankingEntry> = emptyList(),
    val total: Int = 0,
    val disabled: Boolean = false
)

/** 通用成功/失败响应（部分接口附 [avatarUrl]）。 */
@Serializable
data class GenericResponse(
    val success: Boolean,
    val avatarUrl: String? = null
)

/** 刷新 Token 响应（返回新的双 Token，失败含 [error]）。 */
@Serializable
data class RefreshResponse(
    val success: Boolean,
    val token: String? = null,
    val refreshToken: String? = null,
    val error: String? = null
)

/** 各玩法模式开关（闯关 / 无尽 / 对战）。 */
@Serializable
data class GameModes(
    val stage: Boolean = true,
    val endless: Boolean = false,
    val battle: Boolean = false
)

/** App 更新检查响应（是否有更新 / 版本名 / 下载地址 / 更新日志 / 是否强制 / 玩法开关）。 */
@Serializable
data class AppUpdateResponse(
    val has_update: Boolean = false,
    val version_name: String? = null,
    val apk_url: String? = null,
    val update_log: String? = null,
    val force_update: Boolean = false,
    val game_modes: GameModes? = null
)

/** 加入匹配队列请求（玩家 ID）。 */
@Serializable
data class MatchJoinRequest(
    val playerId: String
)

/**
 * 匹配状态响应。
 *
 * @param status 匹配状态：searching（搜索中）/ matched（已配对）/ error（异常）
 * @param gameId 对局 ID（matched 时存在）
 * @param opponentId 对手 ID
 * @param duelLevel 对决关卡
 * @param gameSeed 对局随机种子（两端一致以保证同盘）
 */
@Serializable
data class MatchStatusResponse(
    val status: String, // searching, matched, error
    val gameId: String? = null,
    val opponentId: String? = null,
    val duelLevel: Int? = null,
    val gameSeed: Int? = null
)

/** 每日弹窗响应（昨日积分榜前三 + 当前玩家昨日排名）。 */
@Serializable
data class DailyPopupResponse(
    val success: Boolean,
    val top3: List<Top3Entry> = emptyList(),
    val yesterdayRank: Int
)

/** 每日弹窗中的前三名条目。 */
@Serializable
data class Top3Entry(
    val username: String,
    val points: Int
)