package com.example.sheeps.menu.ui.components

import android.os.Build
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.sheeps.ui.R

val LocalMenuBottomInset = staticCompositionLocalOf { 0.dp }

private val tabs = listOf(
    Triple("game", R.string.tab_game, R.drawable.ic_nav_game),
    Triple("shop", R.string.tab_shop, R.drawable.ic_nav_shop),
    Triple("me", R.string.tab_me, R.drawable.ic_nav_profile)
)

@Composable
internal fun MenuBottomNavigation(
    currentTab: String, onTabSelected: (String) -> Unit,
    pageLayer: GraphicsLayer, pageOrigin: Offset
) {
    val colors = MaterialTheme.colorScheme
    val light = colors.background.luminance() > .5f
    val canBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    var glassOrigin by remember { mutableStateOf(Offset.Zero) }
    val position by animateFloatAsState(
        tabs.indexOfFirst { it.first == currentTab }.coerceAtLeast(0).toFloat(),
        spring(dampingRatio = .88f, stiffness = Spring.StiffnessMediumLow), label = "selectedLens"
    )
    Box(Modifier.fillMaxWidth().background(Brush.verticalGradient(
        0f to Color.Transparent, .55f to colors.background.copy(alpha = .14f), 1f to colors.background
    )).navigationBarsPadding().padding(horizontal = 22.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center) {
        BoxWithConstraints(
            Modifier.widthIn(max = 334.dp).fillMaxWidth()
                .shadow(9.dp, CircleShape, spotColor = Color.Black.copy(alpha = .35f))
                .clip(CircleShape).onGloballyPositioned { glassOrigin = it.positionInRoot() }
        ) {
            // Only the sampled page is blurred; icons and labels remain in the foreground.
            if (canBlur) Box(Modifier.matchParentSize().blur(12.dp).drawBehind {
                val origin = pageOrigin - glassOrigin
                translate(origin.x, origin.y) { drawLayer(pageLayer) }
            })
            val material = colors.surface.copy(alpha = if (!canBlur) .97f else if (light) .80f else .85f)
            Box(Modifier.matchParentSize().background(material).border(1.dp,
                Brush.verticalGradient(listOf(Color.White.copy(alpha = if (light) .95f else .27f),
                    colors.outlineVariant.copy(alpha = .55f))), CircleShape))
            val cellWidth = (maxWidth - 10.dp) / tabs.size
            Box(Modifier.matchParentSize().padding(5.dp)) {
                Box(Modifier.offset(x = cellWidth * position).width(cellWidth).fillMaxHeight()
                    .background(colors.primary.copy(alpha = if (light) .10f else .19f), CircleShape))
            }
            Row(Modifier.fillMaxWidth().padding(5.dp).selectableGroup()) {
                tabs.forEach { (id, label, icon) ->
                    val selected = currentTab == id
                    val ink = if (selected) colors.primary else colors.onSurfaceVariant
                    Column(Modifier.weight(1f).heightIn(min = 48.dp).clip(CircleShape)
                        .selectable(selected, role = Role.Tab) { if (!selected) onTabSelected(id) }
                        .padding(vertical = 3.dp),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Icon(painterResource(icon), null, Modifier.size(21.dp), tint = ink)
                        Text(stringResource(label), color = ink, style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
                    }
                }
            }
        }
    }
}
