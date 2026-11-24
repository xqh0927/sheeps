package com.example.sheeps.menu.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sheeps.ui.R

@Composable
internal fun ChallengeHero(level: Int, onClick: () -> Unit) {
    val ink = Color(0xFF17274D)
    val action = stringResource(R.string.prepare_btn_start)
    var hasEntered by rememberSaveable { mutableStateOf(false) }
    val arrival = remember { Animatable(if (hasEntered) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!hasEntered) {
            hasEntered = true
            arrival.animateTo(1f, tween(650))
        }
    }
    val shape = RoundedCornerShape(27.dp)
    BoxWithConstraints(
        Modifier.fillMaxWidth().clip(shape).background(Color(0xFFE2E7FA))
            .clickable(role = Role.Button, onClickLabel = action, onClick = onClick)
    ) {
        val cardHeight = (maxWidth * .52f).coerceIn(176.dp, 284.dp)
        Image(
            painterResource(R.drawable.sheep_glass_art), contentDescription = null,
            modifier = Modifier.matchParentSize().graphicsLayer {
                scaleX = 1.025f - .025f * arrival.value
                scaleY = scaleX
            },
            contentScale = ContentScale.Crop
        )
        Column(
            Modifier.fillMaxWidth(.56f).heightIn(min = cardHeight)
                .padding(start = 19.dp, end = 2.dp, top = 18.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(stringResource(R.string.current_challenge), color = ink,
                style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(level.toString().padStart(2, '0'), color = ink,
                fontSize = 66.sp, lineHeight = 72.sp, fontWeight = FontWeight.SemiBold,
                letterSpacing = (-4).sp)
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.background(Color.White.copy(alpha = .76f), CircleShape)
                    .border(1.dp, Color.White.copy(alpha = .8f), CircleShape)
                    .padding(horizontal = 13.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(action, color = ink, fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f, fill = false))
                Spacer(Modifier.width(7.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = ink, modifier = Modifier.size(15.dp))
            }
        }
    }
}
