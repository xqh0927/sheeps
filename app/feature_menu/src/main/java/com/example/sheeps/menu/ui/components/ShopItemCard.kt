package com.example.sheeps.menu.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sheeps.data.model.ShopItem
import com.example.sheeps.core.game.TileIconProvider
import com.example.sheeps.theme.CrimsonRed
import com.example.sheeps.ui.components.ItemAnimationIcon

@Composable
fun ShopItemCard(
    item: ShopItem,
    backpackCount: Int,
    currentSkin: String,
    onExchange: (Int) -> Unit,
    onApplySkin: (String) -> Unit
) {
    val isSkin = item.item_type.startsWith("SKIN_") || item.item_type == "CLASSIC"
    val isUnlocked = if (item.item_type == "CLASSIC") true else backpackCount >= 1
    
    val skinKey = remember(item.item_type) {
        if (item.item_type == "CLASSIC") {
            "classic"
        } else if (item.item_type.startsWith("SKIN_")) {
            item.item_type.removePrefix("SKIN_").lowercase()
        } else {
            "classic"
        }
    }
    val isCurrentlyApplied = isSkin && currentSkin == skinKey

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(0.5.dp, Color(0xFFE5DDD3)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isSkin) {
                // 皮肤预览：显示该皮肤的第一个图标
                val context = LocalContext.current
                val iconRes = TileIconProvider.getIconResource(context, skinKey, 1)
                Image(
                    painter = painterResource(id = iconRes),
                    contentDescription = "Skin Preview",
                    modifier = Modifier.size(64.dp)
                )
            } else {
                // 调用高清 Canvas 动画图标 (针对道具)

                ItemAnimationIcon(
                    itemType = item.item_type,
                    size = 64.dp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = item.name,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color.DarkGray
            )

            Text(
                text = item.description ?: "奇门法宝",
                fontSize = 11.sp,
                color = Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (isSkin && isUnlocked) "已解锁" else "${item.points_price} 积分",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = CrimsonRed
                )
                if (!isSkin) {
                    Text(
                        text = "存: ${item.stock}",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (isSkin && isUnlocked) {
                Button(
                    onClick = { onApplySkin(skinKey) },
                    enabled = !isCurrentlyApplied,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCurrentlyApplied) Color.Gray else CrimsonRed,
                        disabledContainerColor = Color(0xFFE0E0E0),
                        disabledContentColor = Color.Gray
                    ),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(30.dp)
                ) {
                    Text(
                        text = if (isCurrentlyApplied) "已启用" else "启用",
                        fontSize = 11.sp,
                        color = if (isCurrentlyApplied) Color.DarkGray else Color.White
                    )
                }
            } else {
                Button(
                    onClick = { onExchange(1) },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(30.dp)
                ) {
                    Text("兑换", fontSize = 11.sp, color = Color.White)
                }
            }
        }
    }
}
