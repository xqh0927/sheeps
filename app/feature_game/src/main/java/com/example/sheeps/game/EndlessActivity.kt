package com.example.sheeps.game

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.therouter.router.Route
import com.example.sheeps.core.base.BaseActivity
import com.example.sheeps.game.state.EndlessViewEffect
import com.example.sheeps.game.state.EndlessViewIntent
import com.example.sheeps.game.ui.screens.EndlessScreen
import com.example.sheeps.game.viewmodel.EndlessViewModel
import com.example.sheeps.theme.SheepsTheme
import com.hjq.toast.Toaster
import dagger.hilt.android.AndroidEntryPoint

@Route(path = "/endless/play")
@AndroidEntryPoint
class EndlessActivity : BaseActivity() {

    private val viewModel: EndlessViewModel by viewModels()

    override fun initView(savedInstanceState: Bundle?) {
        setContent {
            SheepsTheme {
                val state by viewModel.viewState.collectAsState()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    EndlessScreen(
                        state = state,
                        onIntent = { viewModel.sendIntent(it) }
                    )
                }
            }
        }

        viewModel.sendIntent(EndlessViewIntent.Init(isDaily = false, seed = 0))
    }

    override fun initData() {
        lifecycleScope.launchWhenStarted {
            viewModel.viewEffect.collect { effect ->
                when (effect) {
                    is EndlessViewEffect.ShowToast -> Toaster.show(effect.message)
                    is EndlessViewEffect.PlaySound -> { /* 播放音效（接入音效模块后实现） */ }
                    is EndlessViewEffect.Vibrate -> { /* 设备振动（接入后实现） */ }
                    is EndlessViewEffect.ExitGame -> finish()
                }
            }
        }
    }
}
