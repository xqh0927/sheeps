package com.example.sheeps.game.state

import com.example.sheeps.data.game.SkinConstants
import com.example.sheeps.data.model.Tile

/**
 * 无尽生存模式（叠塔）界面状态模型（MVI 中的 State）。
 * 概念 A：6 列竖井，卡牌自顶下落、自底堆积；点击列顶牌入 7 格卡槽，凑 3 同花消除。
 */
data class EndlessViewState(
    /** 是否正在加载 */
    val isLoading: Boolean = false,
    /** 运行态：READY / PLAYING / PAUSED / GAMEOVER */
    val status: EndlessStatus = EndlessStatus.READY,
    /** 当前卡牌皮肤（来自 UserPreferences，动态跟随） */
    val currentSkin: String = SkinConstants.DEFAULT_SKIN,
    /** 棋盘：每列自底向上的牌栈（index 0 = 列底） */
    val columns: List<List<Tile>> = List(6) { emptyList() },
    /** 列高达到此值 = 触顶死亡 */
    val deathRow: Int = 12,
    /** 卡槽中的卡牌 */
    val slotTiles: List<Tile> = emptyList(),
    /** 卡槽最大容量 */
    val maxSlot: Int = 7,
    /** 当前得分 */
    val score: Int = 0,
    /** 当前连击数（0 = 无连击） */
    val combo: Int = 0,
    /** 当前波次 */
    val wave: Int = 1,
    /** 历史最高分（本局开始前载入，用于对比是否新纪录） */
    val bestScore: Int = 0,
    /** 当前下落间隔（毫秒），随波次提速缩短 */
    val dropIntervalMs: Long = 1500,
    /** 是否处于冻结状态（暂停下落） */
    val isFrozen: Boolean = false,
    /** 冻结剩余毫秒 */
    val freezeRemainingMs: Long = 0,
    /** 可用冻结次数 */
    val freezeCount: Int = 0,
    /** 当前同屏花色种类（首要难度杠杆） */
    val activeSuitCount: Int = 3,
    /** 是否每日挑战（同种子） */
    val isDaily: Boolean = false,
    /** 本局种子 */
    val seed: Long = 0,
    /** 每列透出的下方可见层数 */
    val visibleLayers: Int = 2,
    /** 最近一次消除的卡牌 id（高亮用） */
    val lastMatchedTileIds: Set<String> = emptySet(),
    /** 是否展示结算弹窗 */
    val showResult: Boolean = false,
    /** 死因："death_line" / "slot_overflow" */
    val deathReason: String = ""
)

enum class EndlessStatus { READY, PLAYING, PAUSED, GAMEOVER }

/**
 * 无尽生存模式界面意图。
 */
sealed interface EndlessViewIntent {
    /** 初始化对局（每日挑战传种子，普通模式传 0 由 VM 取 System.currentTimeMillis） */
    data class Init(val isDaily: Boolean, val seed: Long = 0) : EndlessViewIntent
    /** 点击列中卡牌（col = 列索引 0..5, tileId = 被点击卡牌的唯一 id） */
    data class ClickColumn(val col: Int, val tileId: String) : EndlessViewIntent
    /** 使用冻结道具 */
    object UseFreeze : EndlessViewIntent
    /** 暂停 / 继续（切换） */
    object Pause : EndlessViewIntent
    /** 继续（显式） */
    object Resume : EndlessViewIntent
    /** 重新开始 */
    object Restart : EndlessViewIntent
    /** 离开（退出界面） */
    object Leave : EndlessViewIntent
    /** 关闭结算弹窗 */
    object DismissResult : EndlessViewIntent
}

/**
 * 无尽生存模式界面副作用（复用现有 SoundType）。
 */
sealed interface EndlessViewEffect {
    data class ShowToast(val message: String) : EndlessViewEffect
    data class PlaySound(val soundType: SoundType) : EndlessViewEffect
    object Vibrate : EndlessViewEffect
    object ExitGame : EndlessViewEffect
}
