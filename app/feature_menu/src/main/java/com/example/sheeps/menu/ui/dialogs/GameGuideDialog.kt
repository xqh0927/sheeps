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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sheeps.menu.ui.components.CoinCanvas
import com.example.sheeps.menu.ui.components.CompassCanvas
import com.example.sheeps.menu.ui.components.GourdCanvas
import com.example.sheeps.menu.ui.components.StarCanvas
import com.example.sheeps.theme.CrimsonRed

@Composable
fun GameGuideDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("吾已领悟", color = Color.White)
            }
        },
        title = {
            Text(
                "秘境修仙指南",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = CrimsonRed,
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
                        title = "一、奇门法阵 (基本玩法)",
                        description = "点击上方重叠层叠的神秘卡牌，它们会落入下方 7 格的乾坤法宝槽。只要槽内集齐 3 张相同图案的卡牌，即可将其熔炼消除。若 7 个槽位被填满且无法消除，则法决失效，挑战失败。",
                        icon = { CompassCanvas() }
                    )
                }

                // 2. 道具说明
                item {
                    GuideSectionCard(
                        title = "二、仙法法宝 (三大道具)",
                        description = "移出法宝：将最左侧 3 张卡牌移出法宝槽并暂存，腾出空间。\n洗牌符咒：将场上所有未消除的卡牌彻底重新排列，化解死局。\n撤销仙法：撤回上一步的熔炼点击，退回上一张卡牌。",
                        icon = { GourdCanvas() }
                    )
                }

                // 3. 积分说明
                item {
                    GuideSectionCard(
                        title = "三、福禄修为 (积分规则)",
                        description = "每日签署福禄（签到）可稳定斩获积分。挑战秘境主线关卡，首次破阵通关亦可赢得海量积分。参与多人实时“天命对决”斩获胜绩，更是赢取大量积分与连胜荣耀的最佳途径！",
                        icon = { StarCanvas() }
                    )
                }

                // 4. 商城说明
                item {
                    GuideSectionCard(
                        title = "四、藏宝仙阁 (积分兑换)",
                        description = "修仙积攒的积分可在“商场”中兑换移出、洗牌、撤销等珍稀辅助法宝。更有酷炫的“赛博霓虹”与儒雅的“水墨写意”定制卡牌皮肤，任君兑换，以换取不同的悟道意境。",
                        icon = { CoinCanvas() }
                    )
                }

                // 5. 天命对决多人对局与法术诅咒
                item {
                    GuideSectionCard(
                        title = "五、天命对决 (多人法术竞技)",
                        description = "在天命对决模式中，每次熔炼消除卡牌将充盈能量（上限 10 点）。\n迷雾咒（消耗 3 能量）：令对手战场黑雾弥漫，需点击探照驱散（持续 6 秒）。\n锁槽咒（消耗 6 能量）：封锁对手消除槽至 6 格（持续 8 秒），槽满立即判负。\n封印咒（消耗 10 能量）：瞬间给对手所有当前可点击的暴露牌附加封印。",
                        icon = { StarCanvas() }
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp)
    )
}

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
                    .background(CrimsonRed.copy(alpha = 0.08f), CircleShape)
                    .border(0.5.dp, CrimsonRed.copy(alpha = 0.3f), CircleShape),
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
                    color = CrimsonRed,
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
