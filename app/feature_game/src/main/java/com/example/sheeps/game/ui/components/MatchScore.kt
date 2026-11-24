package com.example.sheeps.game.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.sheeps.ui.components.AnimatedCounter

/** A consistent number hierarchy for live and final scores in the three game modes. */
@Composable
internal fun MatchScore(label: String, value: Int, ink: Color, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        AnimatedCounter(
            count = value,
            style = MaterialTheme.typography.headlineLarge.copy(
                color = ink,
                fontWeight = FontWeight.Bold,
                fontFeatureSettings = "tnum",
                letterSpacing = (-1.5).sp
            )
        )
    }
}

/** One opaque reading surface keeps changing numbers clear against the playfield. */
@Composable
internal fun Scoreboard(content: @Composable RowScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}
