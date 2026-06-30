package com.example.sheeps.menu.state

import com.example.sheeps.data.model.*

/**
 * 菜单主界面状态模型。
 * 包含了商城、个人中心、对战大厅及全局 App 状态所需的数据。
 */
data class MenuViewState(
    /** 是否正在执行全局加载（如刷新数据） */
    val isLoading: Boolean = false,
    /** 当前用户是否已登录 */
    val isLoggedIn: Boolean = false,
    /** 用户昵称 */
    val username: String = "",
    /** 用户绑定的手机号（UID） */
    val phone: String = "",
    /** 当前账户可用积分余额 */
    val points: Int = 0,
    /** 用户已解锁的最高关卡 ID */
    val unlockedLevel: Int = 1,
    /** 今日是否已完成签到 */
    val todaySigned: Boolean = false,
    /** 连续签到天数 */
    val signStreak: Int = 0,
    /** 闯关历史上清除过的最高关卡数 */
    val highestLevelCleared: Int = 0,
    
    // --- 核心列表数据 ---
    /** 用户背包内的道具列表 */
    val backpackItems: List<UserItem> = emptyList(),
    /** 商店可兑换的商品列表 */
    val shopItems: List<ShopItem> = emptyList(),
    /** 系统公告通知列表 */
    val notices: List<Notice> = emptyList(),
    /** 每日任务进度列表 */
    val dailyTasks: List<DailyTask> = emptyList(),
    
    // --- 历史日志 ---
    /** 积分变动记录 */
    val pointsHistory: List<PointRecord> = emptyList(),
    /** 道具兑换记录 */
    val exchangeHistory: List<ExchangeRecord> = emptyList(),
    
    // --- 交互状态 ---
    /** 当前准备进入关卡时选择携带的道具映射 (ItemType -> Count) */
    val selectedCarryItems: Map<String, Int> = emptyMap(),

    /** 当前网络连接监控状态 */
    val networkStatus: com.example.sheeps.core.utils.NetworkStatus = com.example.sheeps.core.utils.NetworkStatus.ONLINE,

    /** 当前应用生效的语言代码（如 "zh", "en"） */
    val language: String = "",

    /** 应用版本更新信息 */
    val appUpdateInfo: AppUpdateResponse? = null,
    /** 当前生效的卡牌皮肤 ID */
    val currentSkin: String = "classic",

    // --- 在线匹配状态 ---
    /** 匹配状态：none (空闲), searching (匹配中), matched (已匹配), error (异常) */
    val matchStatus: String = "none",
    /** 匹配成功后的房间 ID */
    val matchedGameId: String? = null,
    /** 匹配到的对手玩家 ID */
    val matchedOpponentId: String? = null,
    /** 对决所使用的关卡 ID */
    val duelLevel: Int = 2,
    /** 对决地图生成的随机种子 */
    val gameSeed: Int = 0
)

/**
 * 存档冲突详细信息。
 * 当本地离线进度与云端进度不一致时使用。
 */
data class ConflictInfo(
    val localPoints: Int,
    val localLevel: Int,
    val cloudPoints: Int,
    val cloudLevel: Int
)

/**
 * 菜单界面意图。
 */
sealed interface MenuViewIntent {
    /** 强制重新从服务器加载所有数据 */
    object LoadData : MenuViewIntent
    /** 请求发送短信验证码 */
    data class SendSmsCode(val phone: String) : MenuViewIntent
    /** 执行手机号+验证码登录 */
    data class LoginWithCode(val phone: String, val code: String) : MenuViewIntent
    /** 注销登录，清除本地缓存 */
    object Logout : MenuViewIntent
    /** 执行每日签到动作 */
    object SignIn : MenuViewIntent
    /** 在商店兑换指定数量的道具 */
    data class ExchangeShopItem(val shopItemId: Int, val count: Int) : MenuViewIntent
    /** 领取已完成任务的积分奖励 */
    data class ClaimTask(val taskId: String) : MenuViewIntent
    /** 使用积分强行解锁指定关卡 */
    data class UnlockLevelWithPoints(val levelId: Int) : MenuViewIntent
    /** 备战阶段更新携带道具的数量 */
    data class UpdateCarryItem(val itemType: String, val change: Int) : MenuViewIntent
    /** 清空当前选择的携带道具 */
    object ClearCarryItems : MenuViewIntent
    /** 正式启动游戏界面 */
    data class GoToGame(val levelId: Int, val carryItemsJson: String) : MenuViewIntent
    /** 解决存档冲突：选择保留本地还是覆盖云端 */
    data class ResolveConflict(val useLocal: Boolean) : MenuViewIntent
    /** 更改应用显示语言 */
    data class ChangeLanguage(val lang: String) : MenuViewIntent
    /** 忽略/关闭当前显示的更新提示 */
    object DismissUpdate : MenuViewIntent
    /** 切换当前全局使用的皮肤主题 */
    data class ChangeSkin(val skin: String) : MenuViewIntent
    
    // --- 联机匹配相关 ---
    /** 加入全球随机对战匹配队列 */
    data class JoinMatch(val playerId: String) : MenuViewIntent
    /** 退出匹配队列 */
    data class LeaveMatch(val playerId: String) : MenuViewIntent
    /** 重置匹配状态至初始 */
    object ResetMatchStatus : MenuViewIntent
}

/**
 * 菜单界面副作用。
 */
sealed interface MenuViewEffect {
    /** 显示提示消息 */
    data class ShowToast(val message: String) : MenuViewEffect
    /** 导航并跳转至游戏 Activity */
    data class NavigateToGame(val levelId: Int, val carryItemsJson: String) : MenuViewEffect
    /** 强制显示登录对话框 */
    object ShowLoginDialog : MenuViewEffect
    /** 显示存档冲突解决对话框 */
    data class ShowConflictDialog(
        val localPoints: Int,
        val localLevel: Int,
        val cloudPoints: Int,
        val cloudLevel: Int
    ) : MenuViewEffect
}
