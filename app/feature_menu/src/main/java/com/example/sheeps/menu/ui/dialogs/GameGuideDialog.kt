package com.example.sheeps.menu.ui.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.sheeps.menu.ui.components.CoinCanvas
import com.example.sheeps.menu.ui.components.CompassCanvas
import com.example.sheeps.menu.ui.components.GourdCanvas
import com.example.sheeps.menu.ui.components.StarCanvas
import com.example.sheeps.ui.R
import androidx.compose.ui.res.stringResource

/**
 * 玩法引导对话框（基于 Material [AlertDialog]）。
 *
 * 通过可滚动的 [LazyColumn] 分段展示游戏说明（玩法、道具、积分、商城、天命对决），
 * 每段由 [GuideSectionCard] 渲染并配以 Canvas 图标。点击"知道了"关闭（经由 [onDismiss]）。
 *
 * 触发来源：首页/菜单（MenuScreen）点击"玩法引导"按钮弹出；纯只读展示，不回写 ViewModel。
 *
 * 线程约束：所有文案通过 [stringResource] 取自资源文件，在组合（compose）阶段于
 * 主线程（UI 线程）解析；[onDismiss] 在主线程回调。
 *
 * @param onDismiss 关闭对话框的回调（确认按钮或点击外部触发）。
 */
@Composable
fun GameGuideDialog(onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        AlertDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(stringResource(id = R.string.dialog_guide_ok), color = Color.White)
                }
            },
            title = {
                Text(
                    text = stringResource(id = R.string.guide_title),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = FontFamily.Serif
                )
            },
            text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. 玩法说明
                item {
                    GuideSectionCard(
                        title = stringResource(id = R.string.guide_sec1_title),
                        description = stringResource(id = R.string.guide_sec1_desc),
                        icon = { CompassCanvas() }
                    )
                }

                // 2. 道具说明
                item {
                    GuideSectionCard(
                        title = stringResource(id = R.string.guide_sec2_title),
                        description = stringResource(id = R.string.guide_sec2_desc),
                        icon = { GourdCanvas() }
                    )
                }

                // 3. 积分说明
                item {
                    GuideSectionCard(
                        title = stringResource(id = R.string.guide_sec3_title),
                        description = stringResource(id = R.string.guide_sec3_desc),
                        icon = { StarCanvas() }
                    )
                }

                // 4. 商城说明
                item {
                    GuideSectionCard(
                        title = stringResource(id = R.string.guide_sec4_title),
                        description = stringResource(id = R.string.guide_sec4_desc),
                        icon = { CoinCanvas() }
                    )
                }

                // 5. 天命对决多人对局与法术诅咒
                item {
                    GuideSectionCard(
                        title = stringResource(id = R.string.guide_sec5_title),
                        description = stringResource(id = R.string.guide_sec5_desc),
                        icon = { StarCanvas() }
                    )
                }
            }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

/**
 * 引导对话框中的单个说明卡片（标题 + 描述 + 左侧图标）。
 *
 * @param title 卡片标题文本。
 * @param description 卡片描述文本。
 * @param icon 左侧图标 Composable 插槽（如 [CompassCanvas]、[StarCanvas] 等），在主线程组合。
 */
@Composable
fun GuideSectionCard(
    title: String,
    description: String,
    icon: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), CircleShape)
                    .border(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                icon()
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = FontFamily.Serif
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f)
                )
            }
        }
    }
}
