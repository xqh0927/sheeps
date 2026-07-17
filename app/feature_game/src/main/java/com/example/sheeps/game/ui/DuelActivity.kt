package com.example.sheeps.game.ui

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
import com.example.sheeps.lib_base.base.collectWithLifecycle
import com.therouter.router.Route
import com.example.sheeps.lib_base.base.BaseActivity
import com.example.sheeps.game.state.DuelViewEffect
import com.example.sheeps.game.state.DuelViewIntent
import com.example.sheeps.game.ui.screens.DuelScreen
import com.example.sheeps.game.viewmodel.DuelViewModel
import com.example.sheeps.ui.theme.SheepsTheme
import com.hjq.toast.Toaster
import dagger.hilt.android.AndroidEntryPoint

/**
 * 对决模式 Activity（路由路径 `/game/duel`）。
 *
 * 负责从 intent 读取对局参数（gameId / playerId / levelId / seed），构建
 * Compose 对决界面，并将 DuelScreen 的用户操作转发为
 * [com.example.sheeps.game.state.DuelViewIntent] 交给 [DuelViewModel]；
 * 同时收集 [com.example.sheeps.game.state.DuelViewEffect] 副作用
 * （Toast / 音效 / 振动预留 / 退出）。
 *
 * 生命周期职责：
 * - [initView]：在 `onCreate` 之后由 [BaseActivity] 调用，构建界面并发送
 *   Init 意图；无需要手动释放的资源。
 * - [initData]：收集副作用流；协程绑定 `lifecycleScope`，onDestroy 自动取消。
 *
 * 线程约束：UI 构建与回调转发均运行于主线程。
 */
@Route(path = "/game/duel")
@AndroidEntryPoint
class DuelActivity : BaseActivity() {

    private val viewModel: DuelViewModel by viewModels()

    /**
     * 初始化界面（由 [BaseActivity] 在 onCreate 之后调用）。
     * 职责：读取 intent 中的对局参数并构建 Compose 内容；当 gameId 与 playerId
     * 均非空时发送 [com.example.sheeps.game.state.DuelViewIntent.Init] 启动对局。
     * ⚠️ 资源释放：仅做组合构建，无需手动释放；状态收集由 Compose 自动管理。
     */
    override fun initView(savedInstanceState: Bundle?) {
        val gameId = intent.getStringExtra("gameId") ?: ""
        val playerId = intent.getStringExtra("playerId") ?: ""
        val levelId = intent.getIntExtra("levelId", 2)
        val seed = intent.getIntExtra("seed", 0)

        setContent {
            SheepsTheme {
                // 在 Compose 中收集 UI 状态；collectAsState 自动绑定组合生命周期，无泄漏风险
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

    /**
     * 初始化数据（由 [BaseActivity] 在 initView 之后调用）。
     * 职责：在生命周期范围内收集 [com.example.sheeps.game.state.DuelViewEffect]
     * 副作用（Toast / 音效 / 振动 / 退出游戏）。
     * ⚠️ 资源释放：收集协程绑定于 lifecycleScope，随 Activity 销毁自动取消，无泄漏。
     */
    override fun initData() {
        // 在生命周期范围内收集 ViewModel 发出的副作用
        viewModel.viewEffect.collectWithLifecycle(this) { effect ->
            when (effect) {
                is DuelViewEffect.ShowToast -> Toaster.show(effect.message)
                is DuelViewEffect.PlaySound -> { /* Play sound */ }
                is DuelViewEffect.Vibrate -> { /* Vibrate */ }
                is DuelViewEffect.ExitGame -> finish()
            }
        }
    }

    override fun getOverrideThemeResId(): Int {
        return com.example.sheeps.ui.theme.ThemeManager.getThemeResId()
    }
}
