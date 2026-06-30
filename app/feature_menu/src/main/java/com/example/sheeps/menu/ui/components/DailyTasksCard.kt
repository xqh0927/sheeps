package com.example.sheeps.menu.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sheeps.core.R
import com.example.sheeps.core.utils.getLocalizedTaskName
import com.example.sheeps.menu.state.MenuViewState
import com.example.sheeps.theme.CrimsonRed

/**
 * 每日任务卡片组件
 * 展示用户的每日任务列表、进度以及领取奖励按钮
 *
 * @param state 界面状态数据，包含任务列表
 * @param onClaimTask 领取任务奖励的回调
 */
@Composable
fun DailyTasksCard(
    state: MenuViewState,
    onClaimTask: (String) -> Unit
) {
    if (!state.isLoggedIn) return

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(id = R.string.tasks_title),
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = CrimsonRed,
                modifier = Modifier.padding(bottom = 12.dp),
                fontFamily = FontFamily.Serif
            )

            if (state.dailyTasks.isEmpty()) {
                Text(
                    text = stringResource(id = R.string.tasks_empty),
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            } else {
                state.dailyTasks.forEach { task ->
                    TaskItem(
                        task = task,
                        onClaimTask = onClaimTask
                    )
                }
            }
        }
    }
}

/**
 * 单个任务条目组件
 */
@Composable
private fun TaskItem(
    task: com.example.sheeps.data.model.DailyTask,
    onClaimTask: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = getLocalizedTaskName(task.name),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${getLocalizedTaskName(task.description)} (${task.progress}/${task.target_count})",
                fontSize = 11.sp,
                color = Color.Gray
            )
        }
        
        if (task.is_rewarded) {
            Text(
                text = stringResource(id = R.string.task_rewarded),
                fontSize = 12.sp,
                color = Color.Gray
            )
        } else {
            Button(
                onClick = { onClaimTask(task.task_id) },
                enabled = task.is_completed,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CrimsonRed,
                    disabledContainerColor = Color.LightGray
                ),
                contentPadding = PaddingValues(horizontal = 8.dp),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.height(28.dp)
            ) {
                Text(
                    text = if (task.is_completed) 
                        stringResource(id = R.string.task_claim, task.points_reward) 
                    else 
                        stringResource(id = R.string.task_incomplete),
                    fontSize = 10.sp,
                    color = Color.White
                )
            }
        }
    }
}
