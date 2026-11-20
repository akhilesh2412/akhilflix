package com.akhilflix.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.akhilflix.app.data.model.TabConfig

/** Bottom navigation built entirely from AppConfig.tabs. */
@Composable
fun AppBottomBar(
    tabs: List<TabConfig>,
    currentScreen: String,
    onTabClick: (TabConfig) -> Unit
) {
    NavigationBar {
        tabs.forEach { tab ->
            NavigationBarItem(
                selected = currentScreen == tab.screen,
                onClick = { onTabClick(tab) },
                icon = { Icon(iconFor(tab.icon), contentDescription = tab.title) },
                label = { Text(tab.title) }
            )
        }
    }
}

private fun iconFor(name: String): ImageVector = when (name.lowercase()) {
    "school" -> Icons.Filled.School
    "search" -> Icons.Filled.Search
    "bookmark" -> Icons.Filled.Bookmark
    "play" -> Icons.Filled.PlayArrow
    else -> Icons.Filled.Home
}
