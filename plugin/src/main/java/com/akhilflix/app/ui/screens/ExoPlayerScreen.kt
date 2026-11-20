package com.akhilflix.app.ui.screens

import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import com.akhilflix.app.data.model.Video

/**
 * Advanced universal player (PSPlayer-style): plays everything.
 * HLS (.m3u8), DASH (.mpd), progressive MP4/MKV/WebM — ExoPlayer
 * auto-detects the format from the URL. The built-in PlayerView controller
 * gives play/pause, seek, playback speed, quality selection and subtitles.
 *
 * `headers` (e.g. Referer / User-Agent from a plugin's ExtractorLink)
 * are sent with every stream request; empty for plain course videos.
 */
@Composable
fun ExoPlayerScreen(
    video: Video,
    streamUrl: String,
    onBack: () -> Unit,
    headers: Map<String, String> = emptyMap()
) {
    val context = LocalContext.current

    val exoPlayer = remember(streamUrl) {
        // Custom HTTP factory so plugin links can carry their headers
        // (Referer etc.). With no headers it behaves exactly like before.
        val httpFactory = DefaultHttpDataSource.Factory()
            .setDefaultRequestProperties(headers)
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(httpFactory))
            .build().apply {
            val mimeType = mimeTypeFor(streamUrl)
            val subtitleConfigs = buildList {
                // Null-safe: subtitleUrl is null when the JSON has no
                // such field (same Gson pitfall as streamUrl).
                val subtitleUrl = video.subtitleUrl
                if (!subtitleUrl.isNullOrBlank()) {
                    add(
                        MediaItem.SubtitleConfiguration.Builder(
                            Uri.parse(subtitleUrl)
                        )
                            .setMimeType(subtitleMimeTypeFor(subtitleUrl))
                            .setLanguage("en")
                            .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                            .build()
                    )
                }
            }
            val mediaItem = MediaItem.Builder()
                .setUri(streamUrl)
                .apply { if (mimeType != null) setMimeType(mimeType) }
                .setSubtitleConfigurations(subtitleConfigs)
                .build()
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose { exoPlayer.release() }
    }

    Column(Modifier.fillMaxSize()) {
        IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back") }

        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = true
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxWidth().weight(1f)
        )

        Text(
            text = video.title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 16.dp)
        )
        Text(
            text = "Format: ${formatLabelFor(streamUrl)}  •  ${video.durationLabel()}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 4.dp)
        )
        Button(
            onClick = {
                // Fallback: hand the raw stream to any external player (VLC/MX).
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(Uri.parse(streamUrl), "video/*")
                }
                context.startActivity(Intent.createChooser(intent, "Open with"))
            },
            modifier = Modifier.padding(16.dp)
        ) {
            Text("Open in external player (VLC / MX Player)")
        }
    }
}

/** Detect the container from the URL so ExoPlayer picks the right source. */
private fun mimeTypeFor(url: String): String? {
    val lower = url.substringBefore("?").lowercase()
    return when {
        lower.endsWith(".m3u8") -> MimeTypes.APPLICATION_M3U8
        lower.endsWith(".mpd") -> MimeTypes.APPLICATION_MPD
        lower.endsWith(".mp4") -> MimeTypes.VIDEO_MP4
        lower.endsWith(".webm") -> MimeTypes.VIDEO_WEBM
        lower.endsWith(".mkv") -> MimeTypes.VIDEO_MATROSKA
        else -> null // let ExoPlayer sniff the content
    }
}

private fun subtitleMimeTypeFor(url: String): String {
    val lower = url.substringBefore("?").lowercase()
    return when {
        lower.endsWith(".vtt") -> MimeTypes.TEXT_VTT
        lower.endsWith(".srt") -> MimeTypes.APPLICATION_SUBRIP
        else -> MimeTypes.TEXT_VTT
    }
}

private fun formatLabelFor(url: String): String {
    val lower = url.substringBefore("?").lowercase()
    return when {
        lower.endsWith(".m3u8") -> "HLS"
        lower.endsWith(".mpd") -> "DASH"
        lower.endsWith(".mp4") -> "MP4"
        lower.endsWith(".webm") -> "WebM"
        lower.endsWith(".mkv") -> "MKV"
        else -> "Auto"
    }
}
