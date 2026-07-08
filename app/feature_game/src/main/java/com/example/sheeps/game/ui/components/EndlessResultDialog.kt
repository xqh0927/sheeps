package com.example.sheeps.game.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.sheeps.core.R

/**
 * 无尽生存模式结算 / 失败弹窗。
 * 标题"游戏结束"，显示本局分数 / 最高分 / 死因文案，含"再来一局"与"返回"按钮。
 * 所有颜色走 MaterialTheme.colorScheme 令牌。
 *
 * @param isNewBest 是否刷新了历史最高分（用于展示"新纪录！"）
 */
@Composable
fun EndlessResultDialog(
    score: Int,
    bestScore: Int,
    deathReason: String,
    isNewBest: Boolean,
    onRestart: () -> Unit,
    onLeave: () -> Unit
) {
    val reasonText = when (deathReason) {
        "death_line" -> stringResource(R.string.endless_death_line)
        "slot_overflow" -> stringResource(R.string.endless_slot_overflow)
        else -> ""
    }

    AlertDialog(
        onDismissRequest = { },
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = stringResource(R.string.endless_game_over),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (reasonText.isNotEmpty()) {
                    Text(
                        text = reasonText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Text(
                    text = stringResource(R.string.endless_score, score),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.endless_best, bestScore),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (isNewBest) {
                    Text(
                        text = stringResource(R.string.endless_new_best),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onRestart,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(stringResource(R.string.endless_restart))
            }
        },
        dismissButton = {
            TextButton(onClick = onLeave) {
                Text(
                    text = stringResource(R.string.btn_back),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    )
}
