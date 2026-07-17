package com.example.sheeps.game.ui

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
import com.example.sheeps.lib_base.base.collectWithLifecycle
import com.therouter.router.Route
import com.example.sheeps.lib_base.base.BaseActivity
import com.example.sheeps.game.state.EndlessViewEffect
import com.example.sheeps.game.state.EndlessViewIntent
import com.example.sheeps.game.ui.screens.EndlessScreen
import com.example.sheeps.game.viewmodel.EndlessViewModel
import com.example.sheeps.ui.theme.SheepsTheme
import com.hjq.toast.Toaster
import dagger.hilt.android.AndroidEntryPoint

/**
 * 无尽生存模式 Activity（路由路径 `/endless/play`）。
 *
 * 负责构建 Compose 无尽界面，并将 EndlessScreen 的用户意图转发为
 * [com.example.sheeps.game.state.EndlessViewIntent] 交给 [EndlessViewModel]；
 * 同时收集 [com.example.sheeps.game.state.EndlessViewEffect] 副作用
 * （Toast / 音效 / 振动预留 / 退出）。
 *
 * 生命周期职责：
 * - [initView]：在 `onCreate` 之后由 [BaseActivity] 调用，构建界面并发送
 *   Init 意图（非每日、seed=0）；无需要手动释放的资源。
 * - [initData]：收集副作用流；协程绑定 `lifecycleScope`，onDestroy 自动取消。
 *
 * 线程约束：UI 构建与意图转发均运行于主线程。
 */
@Route(path = "/endless/play")
@AndroidEntryPoint
class EndlessActivity : BaseActivity() {

    private val viewModel: EndlessViewModel by viewModels()

    /**
     * 初始化界面（由 [BaseActivity] 在 onCreate 之后调用）。
     * 职责：构建 Compose 内容，并发送 [com.example.sheeps.game.state.EndlessViewIntent.Init]
     * （非每日模式、seed=0）启动无尽对局。
     * ⚠️ 资源释放：仅做组合构建，无需手动释放；状态收集由 Compose 自动管理。
     */
    override fun initView(savedInstanceState: Bundle?) {
        setContent {
            SheepsTheme {
                // 在 Compose 中收集 UI 状态；collectAsState 自动绑定组合生命周期，无泄漏风险
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

    /**
     * 初始化数据（由 [BaseActivity] 在 initView 之后调用）。
     * 职责：在生命周期范围内收集 [com.example.sheeps.game.state.EndlessViewEffect]
     * 副作用（Toast / 音效 / 振动 / 退出游戏）。
     * ⚠️ 资源释放：收集协程绑定于 lifecycleScope，随 Activity 销毁自动取消，无泄漏。
     */
    override fun initData() {
        // 在生命周期范围内收集 ViewModel 发出的副作用
        viewModel.viewEffect.collectWithLifecycle(this) { effect ->
            when (effect) {
                is EndlessViewEffect.ShowToast -> Toaster.show(effect.message)
                is EndlessViewEffect.PlaySound -> { /* 播放音效（接入音效模块后实现） */ }
                is EndlessViewEffect.Vibrate -> { /* 设备振动（接入后实现） */ }
                is EndlessViewEffect.ExitGame -> finish()
            }
        }
    }

    override fun getOverrideThemeResId(): Int {
        return com.example.sheeps.ui.theme.ThemeManager.getThemeResId()
    }
}
