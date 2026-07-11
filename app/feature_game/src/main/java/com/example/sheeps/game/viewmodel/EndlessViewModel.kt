package com.example.sheeps.game.viewmodel

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.example.sheeps.core.R
import com.example.sheeps.core.base.BaseMviViewModel
import com.example.sheeps.core.preference.UserPreferences
import com.example.sheeps.data.network.ApiService
import com.example.sheeps.game.state.*
import com.example.sheeps.game.viewmodel.delegates.EndlessScoreDelegate
import com.example.sheeps.game.viewmodel.engine.EndlessEngine
import com.example.sheeps.game.viewmodel.helpers.EndlessLevelGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 无尽生存模式（叠塔）ViewModel。
 *
 * 通过 [EndlessEngine] 纯函数推进逻辑，自身负责：
 * - 下落计时器（viewModelScope 内 while 循环，读 currentState.dropIntervalMs；冻结时暂停）
 * - 波次提速（每消除 10 张 或 存活 25s → wave++、dropIntervalMs *= 0.9、activeSuitCount 提升）
 * - Freeze（暂停下落 + 倒计时 UI）
 * - 死亡 → 提交分数 → 结算
 */
@HiltViewModel
class EndlessViewModel @Inject constructor(
    private val apiService: ApiService,
    private val prefs: UserPreferences,
    private val endlessGenerator: EndlessLevelGenerator,
    private val endlessScoreDelegate: EndlessScoreDelegate,
    @ApplicationContext private val context: Context
) : BaseMviViewModel<EndlessViewState, EndlessViewIntent, EndlessViewEffect>(EndlessViewState()) {

    companion object {
        private const val COL_COUNT = 6              // 棋盘列数（无尽叠塔）
        private const val FREEZE_DURATION_MS = 4000L
        private const val FREEZE_INITIAL = 3
        private const val WAVE_ELIM_STEP = 15       // 每消除 15 张升一波
        private const val WAVE_TIME_STEP_MS = 30_000L // 每存活 30s 升一波
        private const val MAX_SUIT = 12
    }

    /** 下落循环 Job（Restart/Init 时取消旧 Job，避免并发计时器） */
    private var spawnJob: Job? = null
    /** 累计存活毫秒（近似 = 各次下落间隔之和） */
    private var survivalMs: Long = 0
    /** 累计消除卡牌数（用于波次触发） */
    private var eliminatedCount: Int = 0

    override fun handleIntent(intent: EndlessViewIntent) {
        when (intent) {
            is EndlessViewIntent.Init -> handleInit(intent.isDaily, intent.seed)
            is EndlessViewIntent.ClickColumn -> handleClickColumn(intent.col, intent.tileId)
            is EndlessViewIntent.UseFreeze -> handleUseFreeze()
            is EndlessViewIntent.Pause -> handlePause()
            is EndlessViewIntent.Resume -> handleResume()
            is EndlessViewIntent.Restart -> handleRestart()
            is EndlessViewIntent.Leave -> setEffect(EndlessViewEffect.ExitGame)
            is EndlessViewIntent.DismissResult -> updateState { copy(showResult = false) }
        }
    }

    // ===== 生命周期 =====

    /**
     * 初始化无尽对局：重置生成器与计数器，构建初始 [EndlessViewState]，并启动下落循环。
     * 普通模式使用 `System.currentTimeMillis()` 作为种子，每日挑战复用传入 `seed` 保证公平竞速。
     * 运行于主线程（纯状态构建 + 启动协程）。
     */
    private fun handleInit(isDaily: Boolean, seed: Long) {
        val actualSeed = if (isDaily) seed else System.currentTimeMillis()
        endlessGenerator.reset(actualSeed)
        survivalMs = 0
        eliminatedCount = 0
        spawnJob?.cancel()
        updateState {
            EndlessViewState(
                status = EndlessStatus.PLAYING,
                currentSkin = prefs.getCurrentSkin(),
                bestScore = prefs.getEndlessBest(),
                isDaily = isDaily,
                seed = actualSeed,
                activeSuitCount = 3,
                wave = 1,
                dropIntervalMs = 3200, // 初始放缓至 3.2 秒
                freezeCount = FREEZE_INITIAL,
                columns = List(COL_COUNT) { emptyList() },
                maxSlot = 7,
                deathRow = 10,
                visibleLayers = 2
            )
        }
        startSpawnLoop()
    }

    private fun handleRestart() {
        handleInit(currentState.isDaily, currentState.seed)
    }

    private fun handlePause() {
        if (currentState.status == EndlessStatus.PLAYING) {
            spawnJob?.cancel()
            updateState { copy(status = EndlessStatus.PAUSED) }
        } else if (currentState.status == EndlessStatus.PAUSED) {
            handleResume()
        }
    }

    private fun handleResume() {
        if (currentState.status == EndlessStatus.PAUSED) {
            updateState { copy(status = EndlessStatus.PLAYING) }
            startSpawnLoop()
        }
    }

    // ===== 下落计时器 =====

    /**
     * 启动下落循环：取消旧 [spawnJob] 后在 [viewModelScope] 内 `while (PLAYING)` 循环。
     * 冻结期间以 200ms 轮询跳过实际下落；正常按 `dropIntervalMs` 间隔触发 [doSpawn]。
     * 旧 Job 先 cancel 防止并发计时器叠加（内存/逻辑隐患防护）。
     */
    private fun startSpawnLoop() {
        spawnJob?.cancel()
        spawnJob = viewModelScope.launch {
            while (currentState.status == EndlessStatus.PLAYING) {
                if (currentState.isFrozen) {
                    delay(200)
                    continue
                }
                delay(currentState.dropIntervalMs)
                if (currentState.status != EndlessStatus.PLAYING) break
                doSpawn()
            }
        }
    }

    /** 生成一行卡牌并压入棋盘（[EndlessEngine.pushRow]）。触顶则判定 death_line；否则累加存活时间并检测波次提速。运行于主线程。 */
    private fun doSpawn() {
        val row = endlessGenerator.nextRow(currentState.columns, currentState.activeSuitCount, COL_COUNT)
        val (newColumns, dead) = EndlessEngine.pushRow(currentState.columns, row, currentState.deathRow)
        survivalMs += currentState.dropIntervalMs
        if (dead) {
            updateState { copy(columns = newColumns) }
            handleDeath("death_line")
            return
        }
        updateState { copy(columns = newColumns) }
        maybeAdvanceWave()
    }

    // ===== 交互 =====

    /**
     * 处理点击某列卡牌：委托 [EndlessEngine.clickColumn] 取出卡牌飞入卡槽并即时三消。
     * 连击累加/归零、得分计算与死亡判定（death_line / slot_overflow）均在此完成。运行于主线程。
     */
    private fun handleClickColumn(col: Int, tileId: String) {
        if (currentState.status != EndlessStatus.PLAYING) return

        val result = EndlessEngine.clickColumn(
            currentState.columns, currentState.slotTiles, col, tileId, currentState.maxSlot
        )

        val (newScore, newCombo) = if (result.matched) {
            val combo = currentState.combo + 1
            val gain = (EndlessEngine.BASE_GAIN * EndlessEngine.comboMultiplier(combo)).toInt()
            (currentState.score + gain) to combo
        } else {
            currentState.score to 0
        }

        val dead = EndlessEngine.isDead(result.columns, result.slot, currentState.deathRow, currentState.maxSlot)
        if (dead != null) {
            if (result.matched) eliminatedCount += result.eliminatedIds.size
            updateState {
                copy(
                    columns = result.columns,
                    slotTiles = result.slot,
                    score = newScore,
                    combo = newCombo,
                    lastMatchedTileIds = result.eliminatedIds
                )
            }
            handleDeath(dead)
            return
        }

        if (result.matched) {
            eliminatedCount += result.eliminatedIds.size
            setEffect(EndlessViewEffect.PlaySound(SoundType.MATCH))
            maybeAdvanceWave()
        } else {
            setEffect(EndlessViewEffect.PlaySound(SoundType.CLICK))
        }

        updateState {
            copy(
                columns = result.columns,
                slotTiles = result.slot,
                score = newScore,
                combo = newCombo,
                lastMatchedTileIds = result.eliminatedIds
            )
        }
    }

    /**
     * 使用冻结道具：暂停下落并启动倒计时协程（[viewModelScope] 内 100ms 轮询更新 `freezeRemainingMs`）。
     * 倒计时结束自动解除冻结。运行于主线程。
     */
    private fun handleUseFreeze() {
        if (currentState.freezeCount <= 0 || currentState.isFrozen) return
        if (currentState.status != EndlessStatus.PLAYING) return
        updateState { copy(isFrozen = true, freezeRemainingMs = FREEZE_DURATION_MS, freezeCount = freezeCount - 1) }
        viewModelScope.launch {
            var remaining = FREEZE_DURATION_MS
            while (remaining > 0 && currentState.isFrozen) {
                delay(100)
                remaining -= 100
                updateState { copy(freezeRemainingMs = remaining.coerceAtLeast(0)) }
            }
            if (currentState.isFrozen) updateState { copy(isFrozen = false, freezeRemainingMs = 0) }
        }
    }

    // ===== 波次与死亡 =====

    private fun maybeAdvanceWave() {
        val targetByElim = eliminatedCount / WAVE_ELIM_STEP + 1
        val targetByTime = (survivalMs / WAVE_TIME_STEP_MS).toInt() + 1
        val target = maxOf(targetByElim, targetByTime)
        if (target > currentState.wave) advanceWave(target)
    }

    private fun advanceWave(target: Int) {
        val newDrop = (currentState.dropIntervalMs * 0.9).toLong().coerceAtLeast(300L)
        val newSuits = minOf(MAX_SUIT, 3 + (target - 1) / 2)
        updateState { copy(wave = target, dropIntervalMs = newDrop, activeSuitCount = newSuits) }
        val banner = context.getString(R.string.endless_wave_banner, target)
        setEffect(EndlessViewEffect.ShowToast(banner))
    }

    /**
     * 死亡结算：取消下落 Job，更新最高分与结算弹窗，播放失败音效与振动，并提交云端成绩。
     * @param reason 死因："death_line"（触顶）或 "slot_overflow"（卡槽溢出）
     */
    private fun handleDeath(reason: String) {
        spawnJob?.cancel()
        val prevBest = currentState.bestScore
        val isNewBest = currentState.score > prevBest
        // 本地最高分更新（delegate 内也会写，这里保证即时显示）
        if (isNewBest) prefs.setEndlessBest(currentState.score)
        updateState {
            copy(
                status = EndlessStatus.GAMEOVER,
                showResult = true,
                deathReason = reason,
                bestScore = maxOf(prevBest, currentState.score)
            )
        }
        setEffect(EndlessViewEffect.PlaySound(SoundType.LOSE))
        setEffect(EndlessViewEffect.Vibrate)

        endlessScoreDelegate.submitEndlessScore(
            scope = viewModelScope,
            finalScore = currentState.score,
            elapsedMs = survivalMs,
            isDaily = currentState.isDaily,
            getLocalizedString = ::loc,
            setEffect = { effect -> setEffect(effect) }
        )
    }

    /** 多语言取词：依据 UserPreferences 中保存的语言选择 */
    private fun loc(zh: String, en: String, tw: String, ja: String, ko: String): String =
        when (prefs.getLanguage()) {
            "en" -> en
            "tw" -> tw
            "ja" -> ja
            "ko" -> ko
            else -> zh
        }
}
