package com.akhilflix.app.ui.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.akhilflix.app.ui.viewmodel.HomeViewModel
import com.akhilflix.app.util.Constants

/**
 * Player router — one screen, every content type:
 *  - Video has a direct streamUrl (HLS/DASH/MP4/MKV/WebM) -> advanced ExoPlayer
 *  - Otherwise (YouTube video id) -> in-app YouTube embed
 */
@Composable
fun VideoPlayerScreen(
    viewModel: HomeViewModel,
    videoId: String,
    onBack: () -> Unit
) {
    val video = viewModel.videoById(videoId)

    // Direct link present -> universal ExoPlayer (PSPlayer-style).
    // Null-safe: streamUrl is null for YouTube catalog videos (Gson
    // leaves the field null when the JSON has no streamUrl), so a plain
    // isNotBlank() here crashed with NullPointerException on every tap.
    val streamUrl = video?.streamUrl
    if (video != null && !streamUrl.isNullOrBlank()) {
        ExoPlayerScreen(video = video, streamUrl = streamUrl, onBack = onBack)
        return
    }

    // No direct link -> YouTube embed player.
    YouTubePlayerScreen(videoId = videoId, onBack = onBack)
}

/**
 * In-app YouTube player using the IFrame embed (no API key needed,
 * works for public videos). Falls back to the YouTube app / browser.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun YouTubePlayerScreen(videoId: String, onBack: () -> Unit) {
    val context = LocalContext.current
    Column(Modifier.fillMaxSize()) {
        IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back") }
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.mediaPlaybackRequiresUserGesture = false
                    webViewClient = WebViewClient()
                    webChromeClient = WebChromeClient()
                    loadData(
                        """
                        <html><body style="margin:0;background:#000">
                        <iframe width="100%" height="100%"
                          src="https://www.youtube.com/embed/$videoId?autoplay=1&rel=0"
                          frameborder="0" allowfullscreen></iframe>
                        </body></html>
                        """.trimIndent(),
                        "text/html",
                        "utf-8"
                    )
                }
            },
            modifier = Modifier.fillMaxWidth().weight(1f)
        )
        Button(
            onClick = {
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(Constants.watchUrlFor(videoId))
                )
                context.startActivity(intent)
            },
            modifier = Modifier.padding(16.dp)
        ) {
            Text("Open in YouTube app")
        }
        Text(
            text = "Video ID: $videoId",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, bottom = 16.dp)
        )
    }
}
