package com.akhilflix.app.ui.components

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.akhilflix.app.data.model.PluginInfo

/**
 * One CloudStream plugin repo on Home: a horizontal row of its plugins.
 * Tapping a plugin shows what it is and offers its GitHub page and its
 * .cs3 download (opened in the browser, so it lands in Downloads and
 * can be installed in CloudStream from there).
 *
 * Honest limit: AkhilFlix can BROWSE a plugin repo, it cannot RUN its
 * plugins — those need CloudStream itself. The .cs3 download is the
 * bridge between the two apps.
 */
@Composable
fun PluginSection(
    repoName: String,
    plugins: List<PluginInfo>
) {
    if (plugins.isEmpty()) return
    var selected by remember { mutableStateOf<PluginInfo?>(null) }

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
                PluginCard(plugin, onClick = { selected = plugin })
            }
        }
        Spacer(Modifier.height(20.dp))
    }

    selected?.let { plugin ->
        PluginDetailDialog(plugin, onDismiss = { selected = null })
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

@Composable
private fun PluginDetailDialog(
    plugin: PluginInfo,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val authors = (plugin.authors ?: emptyList()).joinToString(", ")
    val types = (plugin.tvTypes ?: emptyList()).joinToString(", ")
    val details = buildList {
        plugin.description?.ifBlank { null }?.let { add(it) }
        add("Version: ${plugin.version}")
        if (authors.isNotBlank()) add("By: $authors")
        if (types.isNotBlank()) add("Type: $types")
    }.joinToString("\n\n")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(plugin.displayName()) },
        text = { Text(details) },
        confirmButton = {
            val cs3 = plugin.cs3Url?.ifBlank { null }
            if (cs3 != null) {
                TextButton(onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(cs3)))
                    onDismiss()
                }) { Text("Download .cs3") }
            }
        },
        dismissButton = {
            val repoUrl = plugin.repositoryUrl?.ifBlank { null }
            if (repoUrl != null) {
                TextButton(onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(repoUrl)))
                    onDismiss()
                }) { Text("GitHub") }
            }
            TextButton(onClick = onDismiss) { Text("Band karo") }
        }
    )
}
