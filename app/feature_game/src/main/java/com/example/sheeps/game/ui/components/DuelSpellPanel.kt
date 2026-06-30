package com.example.sheeps.game.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sheeps.game.state.DuelViewState
import com.example.sheeps.theme.Crimson_Primary
import com.example.sheeps.theme.Gold_Primary

/**
 * 对决法术/大招面板
 * 展示可用的恶搞技能，消耗能量释放
 */
@Composable
fun DuelSpellPanel(
    state: DuelViewState,
    onCastSpell: (String) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val spells = listOf(
            SpellInfo("迷雾", "FOG", 3, Color(0xFF8C7B70)),
            SpellInfo("禁魔", "SILENCE", 4, Color(0xFF7E57C2)),
            SpellInfo("乱序", "SHUFFLE", 5, Color(0xFF26A69A)),
            SpellInfo("锁槽", "SHRINK", 6, Crimson_Primary),
            SpellInfo("封魔", "SEAL_ALL", 10, Gold_Primary)
        )

        spells.forEach { spell ->
            SpellButton(
                info = spell,
                currentEnergy = state.currentEnergy,
                isUsed = state.usedSpells.contains(spell.key),
                isSilenced = state.isSilenced,
                onClick = { onCastSpell(spell.key) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

private data class SpellInfo(
    val name: String,
    val key: String,
    val cost: Int,
    val color: Color
)

@Composable
private fun SpellButton(
    info: SpellInfo,
    currentEnergy: Int,
    isUsed: Boolean,
    isSilenced: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isEnabled = currentEnergy >= info.cost && !isUsed && !isSilenced
    val alpha = if (isEnabled) 1.0f else 0.4f
    
    val buttonText = when {
        isUsed -> "已用"
        isSilenced -> "禁魔"
        else -> info.name
    }
    
    Button(
        onClick = onClick,
        enabled = isEnabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = info.color,
            disabledContainerColor = info.color.copy(alpha = 0.2f)
        ),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(vertical = 8.dp, horizontal = 2.dp),
        modifier = modifier.alpha(alpha)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = buttonText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isEnabled) Color.White else Color.Gray,
                maxLines = 1
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "${info.cost}能",
                fontSize = 9.sp,
                color = if (isEnabled) Color.White.copy(alpha = 0.8f) else Color.Gray,
                maxLines = 1
            )
        }
    }
}
