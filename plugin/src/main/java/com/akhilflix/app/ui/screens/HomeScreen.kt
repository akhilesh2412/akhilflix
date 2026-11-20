package com.akhilflix.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.akhilflix.app.data.model.AppAction
import com.akhilflix.app.data.model.Course
import com.akhilflix.app.ui.components.SectionRenderer
import com.akhilflix.app.ui.viewmodel.HomeViewModel

/**
 * Home = every section from AppConfig.homeSections, rendered in order,
 * plus the backend-driven action buttons from AppConfig.homeButtons,
 * plus rows from repos the user added himself from GitHub.
 * Pull-to-refresh reloads the backend config (new sections appear instantly).
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onCourseClick: (Course, String) -> Unit,
    onWatchClick: (Course) -> Unit,
    onActionClick: (AppAction) -> Unit,
    onReposClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var refreshing by remember { mutableStateOf(false) }
    val pullState = rememberPullRefreshState(
        refreshing = refreshing,
        onRefresh = {
            refreshing = true
            viewModel.refresh()
            refreshing = false
        }
    )

    Box(Modifier.pullRefresh(pullState).fillMaxSize()) {
        if (state.loading) {
            // Plain text, no animated spinner: CircularProgressIndicator
            // crashed on launch (Compose material3/animation version
            // mismatch), so the startup path deliberately uses no
            // animation at all.
            Text(
                text = "Loading courses...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.align(Alignment.Center)
            )
            return@Box
        }
        LazyColumn {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
                ) {
                    Text(
                        text = state.config.appName,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedButton(onClick = onReposClick) { Text("＋ Add Repo") }
                }
            }
            // Backend-driven action buttons (e.g. Join Telegram, Play Featured).
            if (state.config.homeButtons.isNotEmpty()) {
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                    ) {
                        items(state.config.homeButtons) { action ->
                            Button(onClick = { onActionClick(action) }) {
                                Text(action.label.ifBlank { action.type })
                            }
                        }
                    }
                }
            }
            items(state.config.homeSections) { section ->
                val courses = state.sections[section.id] ?: emptyList()
                SectionRenderer(
                    section = section,
                    courses = courses,
                    onCourseClick = onCourseClick,
                    onWatchClick = onWatchClick
                )
            }
            // Rows from repos the user added himself from GitHub.
            items(state.dynamicSections) { section ->
                val courses = state.sections[section.id] ?: emptyList()
                SectionRenderer(
                    section = section,
                    courses = courses,
                    onCourseClick = onCourseClick,
                    onWatchClick = onWatchClick
                )
            }
        }
        PullRefreshIndicator(
            refreshing = refreshing,
            state = pullState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}
