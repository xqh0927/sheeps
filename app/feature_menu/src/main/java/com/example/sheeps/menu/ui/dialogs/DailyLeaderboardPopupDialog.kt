package com.example.sheeps.menu.ui.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
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
import com.example.sheeps.data.model.DailyPopupResponse

@Composable
fun DailyLeaderboardPopupDialog(
    data: DailyPopupResponse,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(stringResource(id = R.string.dialog_guide_ok), color = MaterialTheme.colorScheme.onPrimary)
            }
        },
        title = {
            Text(
                text = stringResource(id = R.string.daily_leaderboard_title),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.primary,
                fontFamily = FontFamily.Serif
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 循环展示前三甲的荣誉榜单
                data.top3.forEachIndexed { index, entry ->
                    val rankText = when (index) {
                        0 -> stringResource(id = R.string.daily_rank_first, entry.username, entry.points)
                        1 -> stringResource(id = R.string.daily_rank_second, entry.username, entry.points)
                        2 -> stringResource(id = R.string.daily_rank_third, entry.username, entry.points)
                        else -> ""
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = rankText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // 展示玩家个人昨日的最终天梯排名
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                        .border(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (data.yesterdayRank > 0) {
                            stringResource(id = R.string.daily_my_rank, data.yesterdayRank)
                        } else {
                            stringResource(id = R.string.daily_my_rank_unranked)
                        },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp)
    )
}
