package com.example.sheeps.menu.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.sheeps.core.utils.getLocalizedTaskName
import com.example.sheeps.menu.state.MenuViewState
import com.example.sheeps.ui.R

@Composable
fun DailyTasksCard(state: MenuViewState, onClaimTask: (String) -> Unit) {
    if (!state.isLoggedIn) return
    val colors = MaterialTheme.colorScheme
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = colors.surface) {
        Column(Modifier.padding(horizontal = 15.dp)) {
            if (state.dailyTasks.isEmpty()) {
                Text(stringResource(R.string.tasks_empty), Modifier.padding(vertical = 16.dp), color = colors.onSurfaceVariant)
            }
            state.dailyTasks.forEachIndexed { index, task ->
                if (index > 0) HorizontalDivider(color = colors.outlineVariant)
                Column(Modifier.padding(vertical = 13.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text(getLocalizedTaskName(task.name), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(getLocalizedTaskName(task.description), color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        }
                        if (task.is_completed && !task.is_rewarded) {
                            FilledTonalButton(onClick = { onClaimTask(task.task_id) }) {
                                Text(stringResource(R.string.task_claim, task.points_reward))
                            }
                        } else {
                            Text(stringResource(if (task.is_rewarded) R.string.task_rewarded else R.string.task_incomplete),
                                style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                        }
                    }
                    val fraction by animateFloatAsState(
                        (task.progress.toFloat() / task.target_count.coerceAtLeast(1)).coerceIn(0f, 1f), label = "taskProgress")
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        LinearProgressIndicator(progress = { fraction }, modifier = Modifier.weight(1f))
                        Text("${task.progress} / ${task.target_count}", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
