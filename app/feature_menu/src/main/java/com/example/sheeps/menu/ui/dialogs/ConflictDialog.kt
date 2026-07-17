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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.sheeps.ui.R
import com.example.sheeps.menu.state.ConflictInfo

/**
 * 存档冲突解决对话框（基于 Material [AlertDialog]）。
 *
 * 当检测到本地存档与云端存档不一致时弹出，提供"本地存档"与"云端存档"两张卡片，
 * 用户必须二选一（[onChooseLocal] 或 [onChooseCloud]）以决定最终采用哪份存档。
 *
 * 触发来源：登录/数据同步流程检测到存档冲突时，由菜单 ViewModel 弹出。
 * 确认后：调用方将依据所选来源覆盖写入存档状态（回写 ViewModel / 数据层）；
 * 本对话框通过 [onChooseLocal]/[onChooseCloud] 回传用户选择，不自带提交逻辑。
 *
 * 线程约束：强制选择、禁止点外部或返回键关闭（见下方 onDismissRequest = {}）。
 * [onChooseLocal]/[onChooseCloud] 在主线程（UI 线程）回调。
 *
 * @param info 冲突双方的存档信息（[ConflictInfo]），含 localLevel/localPoints 与 cloudLevel/cloudPoints。
 * @param onChooseLocal 用户选择保留本地存档的回调。
 * @param onChooseCloud 用户选择保留云端存档的回调。
 */
@Composable
fun ConflictDialog(
    info: ConflictInfo,
    onChooseLocal: () -> Unit,
    onChooseCloud: () -> Unit
) {
    Dialog(onDismissRequest = {}, properties = DialogProperties()) {
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
                    color = MaterialTheme.colorScheme.onSurface
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
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = stringResource(id = R.string.save_points, info.localPoints),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // 选项 2：云端存档卡片
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 8.dp)
                            .clickable { onChooseCloud() },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = stringResource(id = R.string.save_points, info.cloudPoints),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
}
