package com.example.sheeps.game.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.sheeps.game.state.DuelViewState
import com.example.sheeps.game.state.GameStatus
import com.example.sheeps.ui.components.PrimaryButton
import androidx.compose.ui.res.stringResource
import com.example.sheeps.ui.R

/**
 * 对决模式结算对话框。
 *
 * 当 [DuelViewState.gameStatus] 为 [GameStatus.WON] / [GameStatus.LOST] 时，
 * 以 `Dialog` + 内部 `AlertDialog` 展示双方得分与“离开”按钮（[onLeave]）。
 *
 * 线程约束：纯 Composable，运行于主线程。
 * ⚠️ 内存隐患：弹窗由 `DuelScreen` 在终局时组合，离开后从组合树移除并释放。
 * ⚠️ 交互约束：`Dialog` 与 `AlertDialog` 的 `onDismissRequest` 均为 `{}`，
 * 即不可通过返回键/点击外部关闭，必须由用户点击“离开”按钮退出，避免误触丢失对局结果。
 *
 * @param state   当前对决视图状态，提供胜负状态与双方得分
 * @param onLeave 点击“离开”按钮时的回调
 */
@Composable
fun DuelResultDialog(
    state: DuelViewState,
    onLeave: () -> Unit
) {
    // 非终局状态（对局进行中）时不渲染结算弹窗，直接结束组合
    if (state.gameStatus != GameStatus.WON && state.gameStatus != GameStatus.LOST) return

    Dialog(
        // 不可取消：屏蔽系统返回/外部点击，强制用户点“离开”
        onDismissRequest = {},
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        AlertDialog(
            onDismissRequest = {},
            title = {
                Text(
                    text = if (state.gameStatus == GameStatus.WON) stringResource(id = R.string.duel_won_title) else stringResource(id = R.string.duel_lost_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = if (state.gameStatus == GameStatus.WON) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = stringResource(id = R.string.duel_your_score, state.score), style = MaterialTheme.typography.bodyLarge)
                    Text(text = stringResource(id = R.string.duel_opponent_score, state.opponentScore), style = MaterialTheme.typography.bodyLarge)
                }
            },
            confirmButton = {
                PrimaryButton(
                    text = stringResource(id = R.string.duel_btn_leave),
                    onClick = onLeave,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}
