package com.example.sheeps.game.ui

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.sheeps.core.base.collectWithLifecycle
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.therouter.router.Route
import com.example.sheeps.core.base.BaseActivity
import com.example.sheeps.game.state.GameViewIntent
import com.example.sheeps.game.ui.screens.GameScreen
import com.example.sheeps.game.viewmodel.GameViewModel
import com.example.sheeps.theme.SheepsTheme
import com.hjq.toast.Toaster

/**
 * 单机游戏模式 Activity（路由路径 `/game/play`）。
 *
 * 负责接收外部参数（如关卡 ID、携带道具 JSON），初始化 Compose 游戏界面，
 * 并将 GameScreen 的用户操作转发为 [com.example.sheeps.game.state.GameViewIntent]
 * 交给 [GameViewModel]；同时处理与 UI 相关的副作用（Toast、音效预留、振动、路由跳转）。
 *
 * 生命周期职责：
 * - [initView]：在 `onCreate` 之后由 [BaseActivity] 调用，仅做界面组合构建，
 *   无需要手动释放的资源。
 * - [initData]：加载初始关卡并收集 [com.example.sheeps.game.state.GameViewEffect]；
 *   协程与状态收集均由 Compose / `lifecycleScope` 在销毁时自动取消。
 *
 * 线程约束：UI 构建与回调转发均运行于主线程；振动通过
 * `Context.getSystemService(VIBRATOR_SERVICE)` 局部获取，不持有静态引用。
 */
@Route(path = "/game/play")
@dagger.hilt.android.AndroidEntryPoint
class GameActivity : BaseActivity() {

    private val viewModel: GameViewModel by viewModels()
    private var levelId: Int = 1

    /**
     * 初始化界面（由 [BaseActivity] 在 onCreate 之后调用）。
     * 职责：读取 intent 中的关卡参数并构建 Compose 内容；将 GameScreen 的
     * 各类回调转发为 [com.example.sheeps.game.state.GameViewIntent] 交给 [viewModel]。
     * ⚠️ 资源释放：本方法仅做组合构建，无需手动释放的资源；
     * 协程与状态收集由 Compose / lifecycleScope 在销毁时自动取消。
     */
    override fun initView(savedInstanceState: Bundle?) {
        // 从 intent 中获取关卡 ID，默认为第 1 关
        levelId = intent.getIntExtra("levelId", 1)

        setContent {
            SheepsTheme {
                // 在 Compose 中收集 UI 状态；collectAsState 自动绑定组合生命周期，
                // 离开界面即停止收集，无泄漏风险
                val state by viewModel.viewState.collectAsState()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    GameScreen(
                        state = state,
                        onTileClick = { tile -> viewModel.sendIntent(GameViewIntent.ClickTile(tile)) },
                        onUseUndo = { viewModel.sendIntent(GameViewIntent.UseUndo) },
                        onUseMoveOut = { viewModel.sendIntent(GameViewIntent.UseMoveOut) },
                        onUseShuffle = { viewModel.sendIntent(GameViewIntent.UseShuffle) },
                        onRevive = {
                            if (state.currentLevelId > 3 && !state.isLoggedIn) {
                                com.therouter.TheRouter.build("/auth/login")
                                    .navigation(this@GameActivity)
                                com.hjq.toast.Toaster.show(getString(com.example.sheeps.core.R.string.toast_login_required_level))
                            } else {
                                viewModel.sendIntent(GameViewIntent.Revive)
                            }
                        },
                        onUseHint = { viewModel.sendIntent(GameViewIntent.UseHint) },
                        onUseBomb = { viewModel.sendIntent(GameViewIntent.UseBomb) },
                        onUseJoker = { viewModel.sendIntent(GameViewIntent.UseJoker) },
                        onUseDouble = { viewModel.sendIntent(GameViewIntent.UseDoublePoints) },
                        onRestart = {
                            if (state.currentLevelId > 3 && !state.isLoggedIn) {
                                com.therouter.TheRouter.build("/auth/login")
                                    .navigation(this@GameActivity)
                                com.hjq.toast.Toaster.show(getString(com.example.sheeps.core.R.string.toast_login_required_level))
                            } else {
                                viewModel.sendIntent(GameViewIntent.RestartLevel)
                            }
                        },
                        onBack = { finish() },
                        onNextLevel = {
                            val nextLvl = state.currentLevelId + 1
                            if (nextLvl > 3 && !state.isLoggedIn) {
                                com.therouter.TheRouter.build("/auth/login")
                                    .navigation(this@GameActivity)
                                com.hjq.toast.Toaster.show(getString(com.example.sheeps.core.R.string.toast_login_required_level))
                            } else {
                                viewModel.sendIntent(GameViewIntent.LoadLevel(nextLvl, null))
                            }
                        },
                        onShowLeaderboard = {
                            if (state.currentLevelId > 3 && !state.isLoggedIn) {
                                com.therouter.TheRouter.build("/auth/login")
                                    .navigation(this@GameActivity)
                                com.hjq.toast.Toaster.show(getString(com.example.sheeps.core.R.string.toast_login_required_level))
                            } else {
                                com.therouter.TheRouter.build("/leaderboard/show")
                                    .withInt("levelId", state.currentLevelId)
                                    .navigation()
                            }
                        },
                        onUpdateTempCarryItem = { type, change ->
                            viewModel.sendIntent(GameViewIntent.UpdateTempCarryItem(type, change))
                        },
                        onConfirmRestartWithCarry = {
                            viewModel.sendIntent(GameViewIntent.ConfirmRestartWithCarry)
                        },
                        onDismissCarrySelection = {
                            viewModel.sendIntent(GameViewIntent.DismissCarrySelection)
                        }
                    )
                }
            }
        }
    }

    /**
     * 初始化数据（由 [BaseActivity] 在 initView 之后调用）。
     * 职责：读取携带道具 JSON 并发送 LoadLevel 意图加载关卡；
     * 在生命周期范围内收集 [com.example.sheeps.game.state.GameViewEffect]
     * 副作用（Toast / 音效 / 振动）。
     * ⚠️ 资源释放：收集协程绑定于 lifecycleScope，onDestroy 时自动取消；
     * 振动通过 getSystemService 局部获取 Vibrator，不持有静态引用，无泄漏。
     */
    override fun initData() {
        val carryJson = intent.getStringExtra("carryItemsJson")
        // 发送初始加载关卡意图
        viewModel.sendIntent(GameViewIntent.LoadLevel(levelId, carryJson))

        // 在生命周期范围内收集 ViewModel 发出的副作用
        viewModel.viewEffect.collectWithLifecycle(this) { effect ->
            when (effect) {
                is com.example.sheeps.game.state.GameViewEffect.ShowToast -> {
                    Toaster.show(effect.message)
                }
                is com.example.sheeps.game.state.GameViewEffect.PlaySound -> {
                    // 预留：此处触发全局音效播放
                }
                is com.example.sheeps.game.state.GameViewEffect.Vibrate -> {
                    try {
                        val vibrator = getSystemService(android.content.Context.VIBRATOR_SERVICE) as android.os.Vibrator
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            vibrator.vibrate(android.os.VibrationEffect.createOneShot(100, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
                        } else {
                            @Suppress("DEPRECATION")
                            vibrator.vibrate(100)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.sendIntent(GameViewIntent.InitUser)
    }

    override fun getOverrideThemeResId(): Int {
        return com.example.sheeps.theme.ThemeManager.getThemeResId()
    }
}
