package com.akhilflix.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.akhilflix.api.AkhilFlixPlugin

/**
 * THIN HOST SHELL. This Activity contains NO app features — it only
 * downloads the AkhilFlix plugin from the GitHub repo and shows it.
 * All screens, player, content and behaviour live in the plugin DEX.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { HostScreen() }
    }
}

private sealed interface HostState {
    data object Loading : HostState
    data class Ready(val plugin: AkhilFlixPlugin) : HostState
    data class Failed(val message: String) : HostState
}

@Composable
private fun HostScreen() {
    var state by remember { mutableStateOf<HostState>(HostState.Loading) }
    var retryCount by remember { mutableStateOf(0) }
    // LocalContext.current is a @Composable call — read it in the
    // composable body, not inside LaunchedEffect (not a @Composable scope).
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(retryCount) {
        state = HostState.Loading
        when (val result = PluginLoader.load(context)) {
            is PluginLoader.Result.Ready -> state = HostState.Ready(result.plugin)
            is PluginLoader.Result.Failed -> state = HostState.Failed(result.message)
        }
    }

    when (val s = state) {
        is HostState.Loading -> LoadingScreen()
        // NOTE: Compose does not allow try/catch around composable calls
        // ("Try catch is not supported around composable function
        // invocations"), so Content() is called directly. Plugin load
        // failures are caught in PluginLoader (Throwable) and shown as an
        // error screen instead of crashing.
        is HostState.Ready -> s.plugin.Content()
        is HostState.Failed -> ErrorScreen(
            message = s.message,
            onRetry = { retryCount++ }
        )
    }
}

@Composable
private fun LoadingScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "AkhilFlix",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(16.dp))
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Repo se app load ho rahi hai…",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ErrorScreen(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "AkhilFlix",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(16.dp))
        Text(text = message, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry) { Text("Retry") }
    }
}
