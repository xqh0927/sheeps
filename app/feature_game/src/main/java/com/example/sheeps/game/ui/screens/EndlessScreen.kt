package com.example.sheeps.game.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.sheeps.core.R
import com.example.sheeps.game.state.EndlessStatus
import com.example.sheeps.game.state.EndlessViewIntent
import com.example.sheeps.game.state.EndlessViewState
import com.example.sheeps.game.ui.components.*
import com.example.sheeps.ui.components.SheepsTopAppBar

/**
 * 无尽生存模式主界面。
 * 组装 HUD（[com.example.sheeps.game.ui.components.EndlessHud]）+ 6 列棋盘
 * （[com.example.sheeps.game.ui.components.EndlessWellBoard]）+ 卡槽
 * （[com.example.sheeps.game.ui.components.EndlessDock]）；当 [EndlessViewState.showResult]
 * 为 true 时弹出结算对话框（[com.example.sheeps.game.ui.components.EndlessResultDialog]）。
 *
 * effect（toast / sound / vibrate / exit）由 Activity 处理，Screen 不处理。
 *
 * 重组注意点：[state] 变化即触发重组；`Scaffold` 的 `paddingValues` 由
 * Compose 在状态栏/导航栏测量后提供，无需手动处理。
 *
 * 线程约束：Composable 运行于主线程；所有点击通过 [onIntent] 即时转发给
 * ViewModel，不在 Screen 内做耗时/IO 操作。
 * ⚠️ 内存隐患：回调 [onIntent] 由 Activity 持有，本 Screen 不捕获额外
 * Activity/Context 引用；结算弹窗仅在 `showResult` 时组合，关闭后随之销毁。
 *
 * @param state 当前无尽模式视图状态
 * @param onIntent 意图分发（直接转给 ViewModel.sendIntent）
 */
@Composable
fun EndlessScreen(
    state: EndlessViewState,
    onIntent: (EndlessViewIntent) -> Unit
) {
    val actionIcon = if (state.status == EndlessStatus.PAUSED) Icons.Default.PlayArrow else Icons.Default.Pause

    Scaffold(
        topBar = {
            SheepsTopAppBar(
                title = stringResource(id = R.string.home_endless_title),
                onBack = { onIntent(EndlessViewIntent.Leave) },
                showAction = true,
                actionIcon = actionIcon,
                onActionClick = { onIntent(EndlessViewIntent.Pause) }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            EndlessHud(
                score = state.score,
                combo = state.combo,
                wave = state.wave,
                bestScore = state.bestScore,
                isFrozen = state.isFrozen,
                freezeCount = state.freezeCount,
                onFreeze = { onIntent(EndlessViewIntent.UseFreeze) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            EndlessWellBoard(
                columns = state.columns,
                currentSkin = state.currentSkin,
                deathRow = state.deathRow,
                visibleLayers = state.visibleLayers,
                onColumnClick = { col, tileId -> onIntent(EndlessViewIntent.ClickColumn(col, tileId)) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            EndlessDock(
                slotTiles = state.slotTiles,
                currentSkin = state.currentSkin
            )
        }
    }

    if (state.showResult) {
        EndlessResultDialog(
            score = state.score,
            bestScore = state.bestScore,
            deathReason = state.deathReason,
            isNewBest = state.score > 0 && state.score >= state.bestScore,
            onRestart = { onIntent(EndlessViewIntent.Restart) },
            onLeave = { onIntent(EndlessViewIntent.Leave) }
        )
    }
}
