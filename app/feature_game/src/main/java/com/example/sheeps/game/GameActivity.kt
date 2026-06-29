package com.example.sheeps.game

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.compose.foundation.layout.fillMaxSize
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

@Route(path = "/game/play")
@dagger.hilt.android.AndroidEntryPoint
class GameActivity : BaseActivity() {

    private val viewModel: GameViewModel by viewModels()
    private var levelId: Int = 1

    override fun initView(savedInstanceState: Bundle?) {
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
                        onBack = { finish() }
                    )
                }
            }
        }
    }

    override fun initData() {
        val carryJson = intent.getStringExtra("carryItemsJson")
        // Automatically request loading the level with carried items
        viewModel.sendIntent(GameViewIntent.LoadLevel(levelId, carryJson))

        // Collect side effects in lifecycleScope
        lifecycleScope.launchWhenStarted {
            viewModel.viewEffect.collect { effect ->
                when (effect) {
                    is com.example.sheeps.game.state.GameViewEffect.ShowToast -> {
                        Toaster.show(effect.message)
                    }
                    is com.example.sheeps.game.state.GameViewEffect.PlaySound -> {
                        // Play CASUAL matching sounds
                    }
                    is com.example.sheeps.game.state.GameViewEffect.Vibrate -> {
                        // Vibrate phone
                    }
                }
            }
        }
    }
}
