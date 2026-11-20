package com.akhilflix.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.akhilflix.app.ui.viewmodel.PluginViewModel
import com.lagradost.cloudstream3.AnimeLoadResponse
import com.lagradost.cloudstream3.Episode
import com.lagradost.cloudstream3.LiveStreamLoadResponse
import com.lagradost.cloudstream3.MovieLoadResponse
import com.lagradost.cloudstream3.TvSeriesLoadResponse

/**
 * Detail page for one item from a running plugin: poster, plot, and
 * either a Play button (movie / live) or the episode list (series /
 * anime). Play resolves the stream link through the provider's
 * loadLinks() and hands it to the ExoPlayer.
 */
@Composable
fun PluginDetailScreen(
    viewModel: PluginViewModel,
    onStreamReady: () -> Unit,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    // A resolved stream link means: go to the player.
    LaunchedEffect(state.pendingStream) {
        if (state.pendingStream != null) onStreamReady()
    }

    val detail = state.detail
    Column(Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back") }
            Text(
                text = detail?.name ?: "Loading...",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.weight(1f),
                maxLines = 2
            )
        }
        if (state.detailLoading) {
            Text(
                text = "Loading...",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(16.dp)
            )
            return@Column
        }
        if (detail == null) {
            state.error?.let { error ->
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
            }
            return@Column
        }
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            item {
                AsyncImage(
                    model = detail.posterUrl,
                    contentDescription = detail.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
                Spacer(Modifier.height(12.dp))
                detail.plot?.ifBlank { null }?.let { plot ->
                    Text(text = plot, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                }
                (detail.tags ?: emptyList()).takeIf { it.isNotEmpty() }?.let { tags ->
                    Text(
                        text = tags.joinToString(", "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                }
            }
            when (detail) {
                is MovieLoadResponse -> {
                    item {
                        Button(
                            onClick = { viewModel.playData(detail.name, detail.dataUrl) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.PlayArrow, "Play")
                            Spacer(Modifier.width(8.dp))
                            Text("Play")
                        }
                    }
                }
                is LiveStreamLoadResponse -> {
                    item {
                        Button(
                            onClick = { viewModel.playData(detail.name, detail.dataUrl) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.PlayArrow, "Play")
                            Spacer(Modifier.width(8.dp))
                            Text("Play Live")
                        }
                    }
                }
                is TvSeriesLoadResponse -> {
                    item {
                        Text(
                            text = "Episodes (${detail.episodes.size})",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    items(detail.episodes) { episode ->
                        EpisodeRow(episode) {
                            viewModel.playData(
                                episode.name ?: detail.name,
                                episode.data
                            )
                        }
                    }
                }
                is AnimeLoadResponse -> {
                    item {
                        Text(
                            text = "Episodes (${detail.episodes.size})",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    items(detail.episodes) { episode ->
                        EpisodeRow(episode) {
                            viewModel.playData(
                                episode.name ?: detail.name,
                                episode.data
                            )
                        }
                    }
                }
                else -> {
                    // Other LoadResponse types (e.g. torrents) have no
                    // playable stream in this app.
                    item {
                        Text(
                            text = "Is type ko play nahi kar sakte",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            item {
                if (state.streamLoading) {
                    Text(
                        text = "Stream load ho raha hai...",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                state.streamError?.let { error ->
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun EpisodeRow(
    episode: Episode,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Text(
            text = buildString {
                episode.season?.let { append("S$it ") }
                episode.episode?.let { append("E$it ") }
            }.ifBlank { "Episode" },
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = episode.name ?: "",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
            maxLines = 2
        )
        Icon(Icons.Filled.PlayArrow, "Play episode")
    }
}
