package com.example.sheeps.menu.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
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
import com.example.sheeps.core.game.TileIconProvider
import com.example.sheeps.data.model.ShopItem
import com.example.sheeps.theme.CrimsonRed
import com.example.sheeps.ui.components.ItemAnimationIcon
import com.example.sheeps.core.R
import com.example.sheeps.core.utils.getLocalizedItemName
import com.example.sheeps.core.utils.getLocalizedItemDesc
import androidx.compose.ui.res.stringResource

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
            val isProvinceSkin = item.item_type.startsWith("SKIN_") && item.item_type != "SKIN_INK" && item.item_type != "SKIN_CYBER"
            if (isSkin && isProvinceSkin) {
                val context = LocalContext.current
                val iconRes = TileIconProvider.getIconResource(context, skinKey, 1)
                if (iconRes != 0) {
                    Image(
                        painter = painterResource(id = iconRes),
                        contentDescription = "Skin Preview",
                        modifier = Modifier.size(64.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier.size(64.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("?", fontSize = 24.sp, color = Color.Gray)
                    }
                }
            } else {
                ItemAnimationIcon(
                    itemType = item.item_type,
                    size = 64.dp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = getLocalizedItemName(item.item_type),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color.DarkGray
            )

            Text(
                text = getLocalizedItemDesc(item.item_type, item.description),
                fontSize = 11.sp,
                color = Color.Gray,
                minLines = 2,
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
                    text = if (isSkin && isUnlocked) stringResource(id = R.string.skin_unlocked) else stringResource(id = R.string.points_suffix, item.points_price),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = CrimsonRed
                )
                if (!isSkin) {
                    Text(
                        text = stringResource(id = R.string.stock_prefix, item.stock),
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
                        text = if (isCurrentlyApplied) stringResource(id = R.string.skin_applied) else stringResource(id = R.string.skin_apply),
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
                    Text(stringResource(id = R.string.btn_exchange), fontSize = 11.sp, color = Color.White)
                }
            }
        }
    }
}
