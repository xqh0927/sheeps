package com.example.sheeps.game

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
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
 * 单机游戏模式 Activity。
 * 负责接收外部参数（如关卡ID、携带道具），初始化游戏界面，并处理与 UI 相关的副作用（如振动、音效、弹窗）。
 */
@Route(path = "/game/play")
@dagger.hilt.android.AndroidEntryPoint
class GameActivity : BaseActivity() {

    private val viewModel: GameViewModel by viewModels()
    private var levelId: Int = 1

    override fun initView(savedInstanceState: Bundle?) {
        // 从 intent 中获取关卡 ID，默认为第 1 关
        levelId = intent.getIntExtra("levelId", 1)

        setContent {
            SheepsTheme {
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
                        onRevive = { viewModel.sendIntent(GameViewIntent.Revive) },
                        onUseHint = { viewModel.sendIntent(GameViewIntent.UseHint) },
                        onUseBomb = { viewModel.sendIntent(GameViewIntent.UseBomb) },
                        onUseJoker = { viewModel.sendIntent(GameViewIntent.UseJoker) },
                        onUseDouble = { viewModel.sendIntent(GameViewIntent.UseDoublePoints) },
                        onRestart = { viewModel.sendIntent(GameViewIntent.RestartLevel) },
                        onBack = { finish() },
                        onNextLevel = {
                            viewModel.sendIntent(GameViewIntent.LoadLevel(state.currentLevelId + 1, null))
                        },
                        onShowLeaderboard = {
                            // 路由跳转至排行榜
                            com.therouter.TheRouter.build("/leaderboard/show")
                                .withInt("levelId", state.currentLevelId)
                                .navigation()
                        }
                    )
                }
            }
        }
    }

    override fun initData() {
        val carryJson = intent.getStringExtra("carryItemsJson")
        // 发送初始加载关卡意图
        viewModel.sendIntent(GameViewIntent.LoadLevel(levelId, carryJson))

        // 在生命周期范围内收集 ViewModel 发出的副作用
        lifecycleScope.launchWhenStarted {
            viewModel.viewEffect.collect { effect ->
                when (effect) {
                    is com.example.sheeps.game.state.GameViewEffect.ShowToast -> {
                        Toaster.show(effect.message)
                    }
                    is com.example.sheeps.game.state.GameViewEffect.PlaySound -> {
                        // 预留：此处触发全局音效播放
                    }
                    is com.example.sheeps.game.state.GameViewEffect.Vibrate -> {
                        // 预留：此处触发设备震动反馈
                    }
                }
            }
        }
    }
}
