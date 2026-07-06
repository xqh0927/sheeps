package com.example.sheeps.game.state

import com.example.sheeps.data.model.RankingEntry
import com.example.sheeps.data.model.Tile

/**
 * 游戏进行状态枚举。
 */
enum class GameStatus {
    /** 初始加载状态 */
    INIT,
    /** 处于游戏主菜单 */
    MENU,
    /** 正在对局中 */
    PLAYING,
    /** 闯关成功（胜利） */
    WON,
    /** 闯关失败 */
    LOST
}

/**
 * 游戏音效类型定义。
 */
enum class SoundType {
    /** 点击卡牌声 */
    CLICK,
    /** 封印解除声 */
    UNSEAL,
    /** 成功消除声 */
    MATCH,
    /** 胜利欢呼声 */
    WIN,
    /** 失败音效 */
    LOSE
}

/**
 * 游戏界面状态模型（MVI 中的 State）。
 * 包含了维持游戏运行所需的所有 UI 数据。
 */
/**
 * 棋盘边界信息，加载关卡时计算一次，保证缩放和居中基于正确的完整布局
 */
data class BoardBounds(
    val minX: Float = 0f,
    val maxX: Float = 0f,
    val minY: Float = 0f,
    val maxY: Float = 0f
)

data class GameViewState(
    /** 是否正在加载关卡数据 */
    val isLoading: Boolean = false,
    /** 隐私协议是否已接受 */
    val isPrivacyAccepted: Boolean = false,
    /** 当前用户的昵称 */
    val username: String = "",
    /** 用户已解锁的最高关卡 ID */
    val unlockedLevel: Int = 1,
    /** 当前正在进行的关卡 ID */
    val currentLevelId: Int = 1,
    /** 棋盘上所有的卡牌列表 */
    val boardTiles: List<Tile> = emptyList(),
    /** 棋盘卡牌边界（加载关卡时计算，避免 Compose remember 缓存时序问题） */
    val boardBounds: BoardBounds = BoardBounds(),
    /** 消除槽（七格槽位）中的卡牌列表 */
    val slotTiles: List<Tile> = emptyList(),
    /** 已使用“移出”道具暂时存放的卡牌列表 */
    val movedOutTiles: List<Tile> = emptyList(),
    
    // --- 道具剩余次数（由进入关卡前选择的携带量决定） ---
    val undoCount: Int = 0,
    val moveOutCount: Int = 0,
    val shuffleCount: Int = 0,
    val reviveCount: Int = 0,
    val hintCount: Int = 0,
    val bombCount: Int = 0,
    val jokerCount: Int = 0,
    val doublePointsCount: Int = 0,

    /** 当前是否激活了双倍积分倍率 */
    val isDoublePointsActive: Boolean = false,
    /** 当前处于提示状态的高亮卡牌 ID 集合 */
    val highlightedTileIds: Set<String> = emptySet(),
    /** 本次关卡的当前得分 */
    val score: Int = 0,
    /** 当前游戏运行状态 */
    val gameStatus: GameStatus = GameStatus.INIT,
    /** 排行榜数据 */
    val rankings: List<RankingEntry> = emptyList(),
    /** 当前使用的卡牌皮肤主题名称 */
    val currentSkin: String = "classic",
    /** 正在抖动的卡牌 ID 集合（用于提示遮挡） */
    val shakingTileIds: Set<String> = emptySet(),
    /** 是否展示携带道具选择弹窗 */
    val showCarrySelection: Boolean = false,
    /** 背包中每种道具的库存数量 */
    val backpackItemStocks: Map<String, Int> = emptyMap(),
    /** 临时选择准备携带的道具 */
    val tempCarryItems: Map<String, Int> = emptyMap()
)

/**
 * 游戏界面意图（MVI 中的 Intent）。
 * 描述了用户可以在游戏界面上发起的各种操作。
 */
sealed interface GameViewIntent {
    /** 同意隐私协议 */
    object AgreePrivacy : GameViewIntent
    /** 初始化用户信息 */
    object InitUser : GameViewIntent
    /** 修改昵称 */
    data class ChangeUsername(val newName: String) : GameViewIntent
    /** 加载指定关卡数据 */
    data class LoadLevel(val levelId: Int, val carryItemsJson: String? = null) : GameViewIntent
    /** 点击了棋盘上的某张卡牌 */
    data class ClickTile(val tile: Tile) : GameViewIntent
    /** 使用“撤销”道具 */
    object UseUndo : GameViewIntent
    /** 使用“移出”道具 */
    object UseMoveOut : GameViewIntent
    /** 使用“洗牌”道具 */
    object UseShuffle : GameViewIntent
    /** 触发“复活”逻辑 */
    object Revive : GameViewIntent
    /** 使用“提示”道具 */
    object UseHint : GameViewIntent
    /** 使用“炸弹”道具 */
    object UseBomb : GameViewIntent
    /** 使用“万能牌”道具 */
    object UseJoker : GameViewIntent
    /** 使用“双倍积分”道具 */
    object UseDoublePoints : GameViewIntent
    /** 加载关卡排行榜数据 */
    data class LoadLeaderboard(val levelId: Int) : GameViewIntent
    /** 重新开始当前关卡 */
    object RestartLevel : GameViewIntent
    /** 返回主菜单界面 */
    object GoBackToMenu : GameViewIntent
    /** 触发重新开始关卡流程（展示道具选择） */
    object TriggerRestartFlow : GameViewIntent
    /** 更新临时选择携带的道具数量 */
    data class UpdateTempCarryItem(val itemType: String, val change: Int) : GameViewIntent
    /** 确认重新开始关卡并扣除道具 */
    object ConfirmRestartWithCarry : GameViewIntent
    /** 取消携带道具选择，关闭弹窗 */
    object DismissCarrySelection : GameViewIntent
}

/**
 * 游戏界面副作用（MVI 中的 Effect）。
 * 描述了由 ViewModel 触发的、不需要持久化到状态流中的一次性事件。
 */
sealed interface GameViewEffect {
    /** 显示吐司提示 */
    data class ShowToast(val message: String) : GameViewEffect
    /** 播放指定音效 */
    data class PlaySound(val soundType: SoundType) : GameViewEffect
    /** 触发触觉反馈（振动） */
    object Vibrate : GameViewEffect
}
