package com.akhilflix.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.akhilflix.app.data.model.PluginInfo

/**
 * One CloudStream plugin repo on Home: a horizontal row of its plugins.
 * Tapping a plugin RUNS it — the .cs3 is loaded by PluginRunner and
 * its home page (categories + items) opens. Downloading the .cs3 or
 * opening GitHub is still available from the plugin's home screen.
 */
@Composable
fun PluginSection(
    repoName: String,
    plugins: List<PluginInfo>,
    onPluginClick: (PluginInfo) -> Unit
) {
    if (plugins.isEmpty()) return
    Column(Modifier.fillMaxWidth()) {
        Text(
            text = repoName,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(plugins) { plugin ->
                PluginCard(plugin, onClick = { onPluginClick(plugin) })
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun PluginCard(
    plugin: PluginInfo,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(140.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = plugin.iconUrlSized(),
                contentDescription = plugin.displayName(),
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(48.dp)
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = plugin.displayName(),
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            maxLines = 2
        )
        Text(
            text = "v${plugin.version}" +
                ((plugin.tvTypes ?: emptyList()).firstOrNull()?.let { " • $it" } ?: ""),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
