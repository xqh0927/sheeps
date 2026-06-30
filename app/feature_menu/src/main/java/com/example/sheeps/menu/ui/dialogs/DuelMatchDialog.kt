package com.example.sheeps.menu.ui.dialogs

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
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
import com.example.sheeps.menu.state.MenuViewState
import com.example.sheeps.theme.CrimsonRed
import com.example.sheeps.core.R
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.delay

@Composable
fun DuelMatchDialog(
    state: MenuViewState,
    onJoin: () -> Unit,
    onLeave: () -> Unit,
    onDismiss: () -> Unit
) {
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

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            if (state.matchStatus == "error") {
                Button(
                    onClick = { onJoin() },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(stringResource(id = R.string.dialog_match_re), color = Color.White)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.dialog_match_exit), color = Color.Gray)
            }
        },
        title = {
            Text(
                text = stringResource(id = R.string.home_match_title),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = CrimsonRed,
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
                        color = CrimsonRed,
                        startAngle = -90f,
                        sweepAngle = 180f,
                        useCenter = true,
                        size = size
                    )
                    drawCircle(
                        color = CrimsonRed,
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
                        color = CrimsonRed,
                        radius = r * 0.15f,
                        center = Offset(centerOffset.x, centerOffset.y + r / 2f)
                    )
                    drawCircle(
                        color = CrimsonRed,
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
                        "error" -> Color.Gray
                        else -> CrimsonRed.copy(alpha = 0.8f)
                    },
                    fontFamily = FontFamily.Serif
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(id = R.string.duel_match_cultivator, state.phone),
                    fontSize = 11.sp,
                    color = Color.Gray
                )

                if (state.matchStatus == "matched" && state.matchedGameId != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "对局号: ${state.matchedGameId.takeLast(10)}",
                        fontSize = 10.sp,
                        color = Color.Gray.copy(alpha = 0.6f)
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp)
    )
}
