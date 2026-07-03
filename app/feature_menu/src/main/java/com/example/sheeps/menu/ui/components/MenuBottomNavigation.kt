package com.example.sheeps.menu.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MenuBottomNavigation(
    currentTab: String,
    onTabSelected: (String) -> Unit
) {
    val navItems = listOf(
        Triple("game", com.example.sheeps.core.R.string.tab_game, com.example.sheeps.core.R.drawable.ic_nav_game),
        Triple("shop", com.example.sheeps.core.R.string.tab_shop, com.example.sheeps.core.R.drawable.ic_nav_shop),
        Triple("me",   com.example.sheeps.core.R.string.tab_me, com.example.sheeps.core.R.drawable.ic_nav_profile)
    )

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        modifier = Modifier
            .border(width = 0.5.dp, color = MaterialTheme.colorScheme.outline)
            .shadow(elevation = 8.dp)
    ) {
        navItems.forEach { (tab, labelResId, icon) ->
            val label = stringResource(id = labelResId)
            val selected = currentTab == tab
            val iconScale by animateFloatAsState(
                targetValue = if (selected) 1.15f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "navIconScale_$tab"
            )
            NavigationBarItem(
                selected = selected,
                onClick  = { onTabSelected(tab) },
                icon = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            painter = painterResource(id = icon),
                            contentDescription = label,
                            modifier = Modifier.scale(iconScale)
                        )
                        if (selected) {
                            Spacer(Modifier.height(2.dp))
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.secondary)
                            )
                        }
                    }
                },
                label = { Text(label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, fontSize = 11.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor   = MaterialTheme.colorScheme.secondary,
                    selectedTextColor   = MaterialTheme.colorScheme.secondary,
                    indicatorColor      = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
