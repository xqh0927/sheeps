package com.example.sheeps.menu.ui.dialogs

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.sheeps.menu.state.MenuViewState
import com.example.sheeps.ui.R
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.delay

/**
 * 天命对决匹配对话框（基于 Material [AlertDialog]）。
 *
 * 进入即自动加入匹配队列，展示旋转的太极动画与匹配状态文案（搜索中/成功/失败/排队）。
 * 匹配成功显示对手 ID 与对局 ID；失败时提供"重新匹配"按钮；关闭时自动离开队列。
 *
 * 触发来源：首页（MenuScreen）点击"天命对决"弹出。
 * 确认后：匹配状态由 [state]（[MenuViewState]）驱动，匹配成功后的对局进入
 * 由上层依据 [state.matchedGameId] 跳转，本对话框仅负责匹配生命周期展示与回传。
 *
 * 线程约束：状态展示与动画在主线程（UI 线程）。下方的 [LaunchedEffect]、`delay` 等
 * 均在组合作用域的协程中运行，随 Dialog 关闭自动取消，不泄漏。
 *
 * ⚠️ 内存隐患：下方的匹配省略号动画使用 `LaunchedEffect(state.matchStatus)` 内
 * `while (state.matchStatus == "searching") { delay(500); ... }` 轮询，该协程由组合作用域
 * 持有，Dialog 关闭即取消；**切勿**改为 `rememberCoroutineScope().launch` 的全局作用域，
 * 否则需手动在 onDispose 中取消，否则 Dialog 销毁后仍会持续轮询。同时 `onJoin`/`onLeave`
 * 由上层负责切到 IO/网络线程。
 *
 * @param state 菜单视图状态（[MenuViewState]），含 matchStatus、matchedOpponentId、matchedGameId、phone 等。
 * @param onJoin 进入时自动加入匹配队列的回调。
 * @param onLeave 对话框销毁时自动离开匹配队列的回调（防止空置对手）。
 * @param onDismiss 关闭对话框的回调（退出按钮或点击外部触发）。
 */
@Composable
fun DuelMatchDialog(
    state: MenuViewState,
    onJoin: () -> Unit,
    onLeave: () -> Unit,
    onDismiss: () -> Unit
) {
    val themePrimary = MaterialTheme.colorScheme.primary
    val infiniteTransition = rememberInfiniteTransition(label = "taichiSpin")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing)
        ),
        label = "taichiRotation"
    )

    var dotCount by remember { mutableIntStateOf(0) }

    // 启动对话框时立即自动加入匹配队列
    LaunchedEffect(Unit) {
        onJoin()
    }

    // 循环播放加载中的省略号动画
    LaunchedEffect(state.matchStatus) {
        while (state.matchStatus == "searching") {
            delay(500)
            dotCount = (dotCount + 1) % 4
        }
    }

    // 对话框销毁时自动离开匹配队列，防止空置对手
    DisposableEffect(Unit) {
        onDispose {
            onLeave()
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties()) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            if (state.matchStatus == "error") {
                Button(
                    onClick = { onJoin() },
                    colors = ButtonDefaults.buttonColors(containerColor = themePrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(stringResource(id = R.string.dialog_match_re), color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.dialog_match_exit), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        title = {
            Text(
                text = stringResource(id = R.string.home_match_title),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = themePrimary,
                fontFamily = FontFamily.Serif
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Canvas(
                    modifier = Modifier
                        .size(80.dp)
                        .graphicsLayer { rotationZ = rotation }
                ) {
                    val r = size.width / 2f
                    val centerOffset = center
                    drawCircle(color = Color.White, radius = r, center = centerOffset)
                    drawArc(
                        color = themePrimary,
                        startAngle = -90f,
                        sweepAngle = 180f,
                        useCenter = true,
                        size = size
                    )
                    drawCircle(
                        color = themePrimary,
                        radius = r / 2f,
                        center = Offset(centerOffset.x, centerOffset.y - r / 2f)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = r / 2f,
                        center = Offset(centerOffset.x, centerOffset.y + r / 2f)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = r * 0.15f,
                        center = Offset(centerOffset.x, centerOffset.y - r / 2f)
                    )
                    drawCircle(
                        color = themePrimary,
                        radius = r * 0.15f,
                        center = Offset(centerOffset.x, centerOffset.y + r / 2f)
                    )
                    drawCircle(
                        color = themePrimary,
                        radius = r,
                        center = centerOffset,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                val dots = ".".repeat(dotCount)
                Text(
                    text = when (state.matchStatus) {
                        "searching" -> stringResource(id = R.string.duel_match_searching, dots)
                        "matched" -> stringResource(id = R.string.duel_match_success, state.matchedOpponentId?.takeLast(4) ?: "???")
                        "error" -> stringResource(id = R.string.duel_match_error)
                        else -> stringResource(id = R.string.duel_match_pending)
                    },
                    fontSize = 14.sp,
                    color = when (state.matchStatus) {
                        "matched" -> Color(0xFF2E7D32)
                        "error" -> MaterialTheme.colorScheme.onSurfaceVariant
                        else -> themePrimary.copy(alpha = 0.8f)
                    },
                    fontFamily = FontFamily.Serif
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(id = R.string.duel_match_cultivator, state.phone),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (state.matchStatus == "matched" && state.matchedGameId != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(id = R.string.duel_match_id_label, state.matchedGameId.takeLast(10)),
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp)
    )
    }
}
