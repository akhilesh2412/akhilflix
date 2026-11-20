package com.akhilflix.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.akhilflix.app.ui.viewmodel.HomeViewModel

/**
 * "My Repos" — add any of the user's own CloudStream repos from GitHub.
 * Paste a repo link (github.com/owner/repo, a raw youtube_courses.json
 * link, or even the repo's CloudStream install URL) and the app fetches
 * its catalog; its courses then show up on Home, in Search and in the
 * Courses tab. Added repos are saved on the phone — no APK update needed
 * to add or remove one.
 */
@Composable
fun ReposScreen(
    viewModel: HomeViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var input by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back") }
            Text(
                text = "My Repos",
                style = MaterialTheme.typography.headlineSmall
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Apne CloudStream repo ka GitHub link paste karo — jaise github.com/Akhilesh2412/programming-courses. Us repo ke courses Home par aa jayenge.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            label = { Text("GitHub repo link") },
            placeholder = { Text("github.com/username/repo") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = {
                viewModel.addRepo(input)
                input = ""
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("＋ Add Repo") }
        state.repoStatus?.let { status ->
            Spacer(Modifier.height(12.dp))
            Text(
                text = status,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Added repos (${state.addedRepos.size})",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(8.dp))
        if (state.addedRepos.isEmpty()) {
            Text(
                text = "Abhi koi repo add nahi hai. Upar se apna pehla repo add karo.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.addedRepos) { repo ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = repo.name,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "${state.addedRepoCounts[repo.id] ?: 0} courses",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { viewModel.removeRepo(repo.id) }) {
                        Icon(Icons.Filled.Delete, "Remove ${repo.name}")
                    }
                }
            }
        }
    }
}
