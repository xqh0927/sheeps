package com.example.sheeps.game

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.therouter.router.Route
import com.example.sheeps.core.base.BaseActivity
import com.example.sheeps.game.state.DuelViewEffect
import com.example.sheeps.game.state.DuelViewIntent
import com.example.sheeps.game.ui.screens.DuelScreen
import com.example.sheeps.game.viewmodel.DuelViewModel
import com.example.sheeps.theme.SheepsTheme
import com.hjq.toast.Toaster
import dagger.hilt.android.AndroidEntryPoint

@Route(path = "/game/duel")
@AndroidEntryPoint
class DuelActivity : BaseActivity() {

    private val viewModel: DuelViewModel by viewModels()

    override fun initView(savedInstanceState: Bundle?) {
        val gameId = intent.getStringExtra("gameId") ?: ""
        val playerId = intent.getStringExtra("playerId") ?: ""
        val levelId = intent.getIntExtra("levelId", 2)
        val seed = intent.getIntExtra("seed", 0)

        setContent {
            SheepsTheme {
                val state by viewModel.viewState.collectAsState()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DuelScreen(
                        state = state,
                        onTileClick = { tile -> viewModel.sendIntent(DuelViewIntent.ClickTile(tile)) },
                        onLeave = { 
                            viewModel.sendIntent(DuelViewIntent.Leave)
                        },
                        onRestart = { viewModel.sendIntent(DuelViewIntent.Restart) },
                        onCastSpell = { spellType -> viewModel.sendIntent(DuelViewIntent.CastSpell(spellType)) }
                    )
                }
            }
        }

        if (gameId.isNotEmpty() && playerId.isNotEmpty()) {
            viewModel.sendIntent(DuelViewIntent.Init(gameId, playerId, levelId, seed))
        }
    }

    override fun initData() {
        lifecycleScope.launchWhenStarted {
            viewModel.viewEffect.collect { effect ->
                when (effect) {
                    is DuelViewEffect.ShowToast -> Toaster.show(effect.message)
                    is DuelViewEffect.PlaySound -> { /* Play sound */ }
                    is DuelViewEffect.Vibrate -> { /* Vibrate */ }
                    is DuelViewEffect.ExitGame -> finish()
                }
            }
        }
    }
}
