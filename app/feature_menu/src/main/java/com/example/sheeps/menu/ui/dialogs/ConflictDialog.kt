package com.example.sheeps.menu.ui.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sheeps.core.R
import com.example.sheeps.menu.state.ConflictInfo

@Composable
fun ConflictDialog(
    info: ConflictInfo,
    onChooseLocal: () -> Unit,
    onChooseCloud: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {}, // 强制用户必须做出存档选择，不能直接点外部关闭
        title = {
            Text(
                text = stringResource(id = R.string.dialog_conflict_title),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        },
        text = {
            Column {
                Text(
                    text = stringResource(id = R.string.dialog_conflict_desc),
                    fontSize = 14.sp,
                    color = Color.DarkGray
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // 选项 1：本地存档卡片
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                            .clickable { onChooseLocal() },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F7F5)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = stringResource(id = R.string.save_local),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(id = R.string.save_level, info.localLevel),
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                            Text(
                                text = stringResource(id = R.string.save_points, info.localPoints),
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    // 选项 2：云端存档卡片
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 8.dp)
                            .clickable { onChooseCloud() },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F9F7)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = stringResource(id = R.string.save_cloud),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(id = R.string.save_level, info.cloudLevel),
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                            Text(
                                text = stringResource(id = R.string.save_points, info.cloudPoints),
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        shape = RoundedCornerShape(16.dp)
    )
}
